package com.example.ddorang.presentation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 비디오 압축 서비스
 * FFmpeg를 사용하여 영상을 480p로 압축
 */
@Service
@Slf4j
public class VideoCompressionService {

    /**
     * 비디오를 480p로 압축 (원본이 480p 이상일 때만)
     *
     * @param inputFile 원본 비디오 파일
     * @return 압축된 비디오 파일 (압축 불필요 시 null)
     */
    public File compressTo480p(File inputFile) {
        log.info("🎬 영상 압축 확인 시작: {} ({}MB)",
            inputFile.getName(),
            inputFile.length() / (1024 * 1024));

        File outputFile = null;
        long startTime = System.currentTimeMillis();

        try {
            // 1. 원본 영상 해상도 확인
            int originalHeight = getVideoHeight(inputFile);
            log.info("원본 영상 해상도 높이: {}p", originalHeight);

            // 2. 480p 이하면 압축 건너뛰기
            if (originalHeight <= 480) {
                log.info("✅ 원본 해상도가 480p 이하이므로 압축 건너뜀 ({}p)", originalHeight);
                return null; // 압축 불필요
            }

            log.info("📐 원본 해상도 {}p → 480p로 압축 시작", originalHeight);

            // 3. 출력 파일 경로 생성 (임시 디렉토리)
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            String outputFileName = "compressed_" + System.currentTimeMillis() + "_" + inputFile.getName();
            outputFile = tempDir.resolve(outputFileName).toFile();

            log.info("압축 파일 경로: {}", outputFile.getAbsolutePath());

            // 4. FFmpeg 명령어 구성 (480p)
            ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-i", inputFile.getAbsolutePath(),  // 입력 파일
                "-vf", "scale=-2:480",              // 480p 해상도 (너비 자동 계산, 2의 배수)
                "-c:v", "libx264",                  // H.264 코덱
                "-crf", "30",                       // 품질 (28→30, 더 빠름)
                "-preset", "fast",                  // 인코딩 속도 (fast/medium/slow)
                "-c:a", "aac",                      // 오디오 AAC 코덱
                "-b:a", "128k",                     // 오디오 비트레이트
                "-y",                               // 덮어쓰기 자동 승인
                outputFile.getAbsolutePath()        // 출력 파일
            );

            processBuilder.redirectErrorStream(true);

            log.debug("FFmpeg 명령어: {}", String.join(" ", processBuilder.command()));

            // 3. FFmpeg 실행
            Process process = processBuilder.start();

            // 4. FFmpeg 출력 로그 읽기 (진행률 표시용)
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // FFmpeg 진행률 로그 (선택적으로 출력)
                    if (line.contains("time=") || line.contains("frame=")) {
                        log.debug("FFmpeg: {}", line);
                    }
                }
            }

            // 5. 프로세스 완료 대기
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg 압축 실패 - exit code: " + exitCode);
            }

            // 6. 압축 결과 확인
            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new RuntimeException("압축된 파일이 생성되지 않았습니다");
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            long originalSize = inputFile.length();
            long compressedSize = outputFile.length();
            double compressionRatio = (1 - (double) compressedSize / originalSize) * 100;

            log.info("✅ 영상 압축 완료: {}MB → {}MB ({}% 절약, {}초 소요)",
                originalSize / (1024 * 1024),
                compressedSize / (1024 * 1024),
                String.format("%.1f", compressionRatio),
                elapsedTime / 1000);

            return outputFile;

        } catch (Exception e) {
            log.error("❌ 영상 압축 실패: {}", inputFile.getName(), e);

            // 실패 시 압축된 파일 삭제
            if (outputFile != null && outputFile.exists()) {
                outputFile.delete();
            }

            throw new RuntimeException("영상 압축 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 압축된 임시 파일 삭제
     *
     * @param compressedFile 삭제할 압축 파일
     */
    public void deleteCompressedFile(File compressedFile) {
        if (compressedFile == null || !compressedFile.exists()) {
            return;
        }

        try {
            if (compressedFile.delete()) {
                log.debug("압축 파일 삭제 성공: {}", compressedFile.getName());
            } else {
                log.warn("압축 파일 삭제 실패: {}", compressedFile.getName());
            }
        } catch (Exception e) {
            log.warn("압축 파일 삭제 중 오류: {}", compressedFile.getName(), e);
        }
    }

    /**
     * 영상의 높이(해상도) 확인
     *
     * @param videoFile 비디오 파일
     * @return 영상 높이 (픽셀)
     */
    private int getVideoHeight(File videoFile) {
        try {
            // ffprobe로 영상 높이 추출
            ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=height",
                "-of", "default=noprint_wrappers=1:nokey=1",
                videoFile.getAbsolutePath()
            );

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String heightStr = reader.readLine();
                if (heightStr != null && !heightStr.trim().isEmpty()) {
                    return Integer.parseInt(heightStr.trim());
                }
            }

            process.waitFor();

            log.warn("영상 높이를 확인할 수 없음, 기본값 720p 반환");
            return 720; // 기본값 (압축 진행)

        } catch (Exception e) {
            log.warn("영상 높이 확인 실패, 기본값 720p 반환: {}", e.getMessage());
            return 720; // 기본값 (압축 진행)
        }
    }

    /**
     * FFmpeg 설치 여부 확인
     *
     * @return FFmpeg 설치 여부
     */
    public boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.error("FFmpeg 확인 실패 - FFmpeg가 설치되지 않았을 수 있습니다", e);
            return false;
        }
    }
}