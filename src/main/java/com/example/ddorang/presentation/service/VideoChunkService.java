package com.example.ddorang.presentation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 비디오 파일 청킹 및 업로드 서비스
 * 큰 비디오 파일을 50MB 단위로 분할하여 FastAPI로 전송
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoChunkService {

    private static final long CHUNK_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MILLIS = 2_000L;
    private static final long MAX_RETRY_DELAY_MILLIS = 10_000L;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${fastapi.base-url:http://localhost:8000}")
    private String fastApiUrl;

    /**
     * 비디오 파일을 청크로 분할하고 FastAPI로 업로드
     *
     * @param videoFile 업로드할 비디오 파일
     * @param metadata FastAPI에 전송할 메타데이터 (target_time 등)
     * @return FastAPI job_id
     */
    public String uploadVideoInChunks(File videoFile, Map<String, Object> metadata) {
        return uploadVideoInChunks(videoFile, metadata, null);
    }

    /**
     * 비디오 파일을 청크로 분할하고 FastAPI로 업로드 (video_path 반환용)
     *
     * @param videoFile 업로드할 비디오 파일
     * @param metadata FastAPI에 전송할 메타데이터 (target_time 등)
     * @param videoPathMap video_path를 저장할 Map (null 가능)
     * @return FastAPI job_id
     */
    public String uploadVideoInChunks(File videoFile, Map<String, Object> metadata, Map<String, String> videoPathMap) {
        log.debug("DEBUG: VideoChunkService.uploadVideoInChunks() 메서드 진입");
        log.info("📦 청크 업로드 시작: {} ({}MB)",
            videoFile.getName(),
            videoFile.length() / (1024 * 1024));
        log.debug("DEBUG: fastApiUrl: {}", fastApiUrl);
        log.debug("DEBUG: metadata: {}", metadata);

        List<File> chunks = new ArrayList<>();

        try {
            // 1. 파일을 청크로 분할
            chunks = splitIntoChunks(videoFile);
            log.info("✂️ 파일 분할 완료: {} → {}개 청크", videoFile.getName(), chunks.size());

            // 2. 청크를 FastAPI로 업로드
            String originalFilename = extractFilenameWithoutExtension(videoFile.getName());
            String fastApiJobId = uploadChunks(chunks, originalFilename, metadata, videoPathMap);

            log.info("청크 업로드 완료: job_id={}", fastApiJobId);

            return fastApiJobId;

        } catch (IOException e) {
            log.error("파일 분할 실패: {}", videoFile.getName(), e);
            throw new RuntimeException("비디오 파일 분할 중 오류 발생: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("청크 업로드 실패: {}", videoFile.getName(), e);
            throw new RuntimeException("청크 업로드 중 오류 발생: " + e.getMessage(), e);
        } finally {
            // 3. 임시 청크 파일 정리 (성공/실패 관계없이 반드시 실행)
            cleanupChunks(chunks);
        }
    }

    /**
     * 비디오 파일을 50MB 청크로 분할
     * 메모리 효율을 위해 스트림 방식으로 읽기
     */
    private List<File> splitIntoChunks(File videoFile) throws IOException {
        List<File> chunks = new ArrayList<>();

        long fileSize = videoFile.length();
        int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);

        log.debug("파일 크기: {}MB, 예상 청크 수: {}", fileSize / (1024 * 1024), totalChunks);

        // 원본 파일 확장자 추출
        String fileExtension = getFileExtension(videoFile.getName());

        try (FileInputStream fis = new FileInputStream(videoFile);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            byte[] buffer = new byte[(int) CHUNK_SIZE];
            int chunkIndex = 0;

            while (true) {
                // CHUNK_SIZE만큼 읽기 (마지막 청크는 작을 수 있음)
                int bytesRead = 0;
                int totalBytesRead = 0;

                // 정확히 CHUNK_SIZE 또는 파일 끝까지 읽기
                while (totalBytesRead < CHUNK_SIZE) {
                    bytesRead = bis.read(buffer, totalBytesRead, (int)(CHUNK_SIZE - totalBytesRead));
                    if (bytesRead == -1) {
                        break; // 파일 끝
                    }
                    totalBytesRead += bytesRead;
                }

                if (totalBytesRead == 0) {
                    break; // 더 이상 읽을 데이터 없음
                }

                // 임시 청크 파일 생성 (원본 확장자 유지)
                Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
                String chunkFileName = String.format("%s_chunk_%d%s",
                    extractFilenameWithoutExtension(videoFile.getName()),
                    chunkIndex,
                    fileExtension);
                Path chunkPath = tempDir.resolve(chunkFileName);

                // 청크 데이터를 임시 파일에 쓰기
                try (FileOutputStream fos = new FileOutputStream(chunkPath.toFile())) {
                    fos.write(buffer, 0, totalBytesRead);
                }

                File chunkFile = chunkPath.toFile();
                chunks.add(chunkFile);

                log.debug("청크 생성: {} ({}MB)", chunkFileName, totalBytesRead / (1024 * 1024));

                chunkIndex++;

                if (bytesRead == -1) {
                    break; // 파일 끝 도달
                }
            }
        }

        return chunks;
    }

    /**
     * 청크를 FastAPI /analysis 엔드포인트로 순차 업로드
     */
    private String uploadChunks(List<File> chunks, String originalFilename, Map<String, Object> metadata)
            throws Exception {
        return uploadChunks(chunks, originalFilename, metadata, null);
    }

    /**
     * 청크를 FastAPI /analysis 엔드포인트로 순차 업로드 (video_path 반환용)
     */
    private String uploadChunks(List<File> chunks, String originalFilename, Map<String, Object> metadata, Map<String, String> videoPathMap)
            throws Exception {

        int totalChunks = chunks.size();
        String fastApiJobId = null;

        for (int i = 0; i < totalChunks; i++) {
            File chunk = chunks.get(i);

            log.info("청크 업로드 중: {}/{} ({}MB)",
                i + 1,
                totalChunks,
                chunk.length() / (1024 * 1024));

            // 멀티파트 요청 구성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("video", new FileSystemResource(chunk));
            body.add("metadata", objectMapper.writeValueAsString(metadata));
            body.add("chunk_index", i);
            body.add("total_chunks", totalChunks);
            body.add("original_filename", originalFilename);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // FastAPI 호출
            ResponseEntity<Map<String, Object>> response = sendChunkWithRetry(requestEntity, i, totalChunks);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException(
                    String.format("청크 업로드 실패: %d/%d - HTTP %s",
                        i + 1, totalChunks, response.getStatusCode()));
            }

            // 응답 본문 로깅
            log.info("청크 {}/{} 응답: {}", i + 1, totalChunks, response.getBody());

            // job_id 받기 (첫 번째 청크 또는 마지막 청크에서 올 수 있음)
            if (response.getBody().containsKey("job_id")) {
                String receivedJobId = (String) response.getBody().get("job_id");
                if (receivedJobId != null && !receivedJobId.isEmpty()) {
                    fastApiJobId = receivedJobId;
                    log.info("FastAPI job_id 할당: {}", fastApiJobId);
                }
            }
            
            // 마지막 청크에서 save_path 또는 video_path를 받을 수 있음
            if (i == totalChunks - 1) {
                String videoPath = null;
                
                // save_path 우선 확인 (FastAPI가 실제로 반환하는 필드)
                if (response.getBody().containsKey("save_path")) {
                    String savePath = (String) response.getBody().get("save_path");
                    if (savePath != null && !savePath.isEmpty()) {
                        // 절대 경로를 상대 경로로 변환
                        videoPath = convertToRelativePath(savePath);
                        log.info("📹 FastAPI에서 save_path 수신: {} → {}", savePath, videoPath);
                    }
                }
                // video_path가 있으면 사용 (fallback)
                else if (response.getBody().containsKey("video_path")) {
                    videoPath = (String) response.getBody().get("video_path");
                    if (videoPath != null && !videoPath.isEmpty()) {
                        log.info("📹 FastAPI에서 video_path 수신: {}", videoPath);
                    }
                }
                
                // video_path를 Map에 저장 (상위로 전달)
                if (videoPath != null && !videoPath.isEmpty() && videoPathMap != null) {
                    videoPathMap.put("video_path", videoPath);
                }
            }

            log.debug("✓ 청크 {}/{} 업로드 완료", i + 1, totalChunks);
        }

        // 모든 청크 업로드 완료 후 job_id 확인
        if (fastApiJobId == null) {
            throw new RuntimeException("모든 청크 업로드 완료했지만 FastAPI가 job_id를 반환하지 않음");
        }

        return fastApiJobId;
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map<String, Object>> sendChunkWithRetry(
        HttpEntity<MultiValueMap<String, Object>> requestEntity,
        int chunkIndex,
        int totalChunks
    ) {
        int attempt = 0;
        long backoff = INITIAL_RETRY_DELAY_MILLIS;

        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.exchange(
                    fastApiUrl + "/analysis",
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
                );
            } catch (ResourceAccessException ex) {
                attempt++;
                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    throw new RuntimeException(
                        String.format(
                            "청크 업로드 실패 (재시도 %d회 초과): 청크 %d/%d",
                            MAX_RETRY_ATTEMPTS,
                            chunkIndex + 1,
                            totalChunks
                        ),
                        ex
                    );
                }

                long sleepMillis = Math.min(backoff, MAX_RETRY_DELAY_MILLIS);
                log.warn(
                    "네트워크 오류로 청크 업로드 재시도 예정: 청크 {}/{} (시도 {}/{}) - {}: {}",
                    chunkIndex + 1,
                    totalChunks,
                    attempt,
                    MAX_RETRY_ATTEMPTS,
                    ex.getClass().getSimpleName(),
                    ex.getMessage()
                );

                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("청크 업로드 재시도 대기 중 인터럽트 발생", interruptedException);
                }

                backoff = Math.min(backoff * 2, MAX_RETRY_DELAY_MILLIS);
            }
        }

        throw new IllegalStateException("청크 업로드 재시도 로직에서 도달할 수 없는 위치입니다.");
    }

    /**
     * 임시 청크 파일 삭제
     */
    private void cleanupChunks(List<File> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        log.debug("🧹 임시 청크 파일 정리 시작: {}개", chunks.size());

        int deletedCount = 0;
        for (File chunk : chunks) {
            try {
                if (chunk.exists() && chunk.delete()) {
                    deletedCount++;
                }
            } catch (Exception e) {
                log.warn("청크 삭제 실패 (무시됨): {}", chunk.getName(), e);
            }
        }

        log.debug("✓ 청크 파일 정리 완료: {}/{}개 삭제", deletedCount, chunks.size());
    }

    /**
     * 파일명에서 확장자 제거
     * 예: "my_video.mp4" → "my_video"
     */
    private String extractFilenameWithoutExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        }

        return filename;
    }

    /**
     * 파일명에서 확장자 추출 (소문자로 통일)
     * 예: "my_video.MP4" → ".mp4"
     * 예: "my_video.mp4" → ".mp4"
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex).toLowerCase();
        }

        return "";
    }

    /**
     * 절대 경로를 상대 경로로 변환
     * 예: "C:/uploads/stored_videos/123.mp4" → "stored_videos/123.mp4"
     * 예: "/app/uploads/stored_videos/123.mp4" → "stored_videos/123.mp4"
     */
    private String convertToRelativePath(String absolutePath) {
        if (absolutePath == null || absolutePath.isEmpty()) {
            return absolutePath;
        }
        
        // Windows와 Linux 경로 모두 처리
        String path = absolutePath.replace("\\", "/");
        
        // stored_videos 또는 videos 디렉터리가 포함된 경우, 그 이후 부분만 추출
        int storedVideosIndex = path.indexOf("stored_videos");
        if (storedVideosIndex >= 0) {
            return path.substring(storedVideosIndex);
        }

        int videosIndex = path.indexOf("videos");
        if (videosIndex >= 0) {
            return path.substring(videosIndex);
        }

        // 위 디렉터리가 없더라도 파일명만이라도 추출해 반환
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < path.length() - 1) {
            String filenameOnly = path.substring(lastSlashIndex + 1);
            if (!filenameOnly.isEmpty()) {
                return filenameOnly;
            }
        }
        
        // 상대 경로로 변환할 수 없는 경우 원본 반환
        return absolutePath;
    }

}
