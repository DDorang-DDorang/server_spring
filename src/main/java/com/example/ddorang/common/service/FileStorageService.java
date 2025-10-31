package com.example.ddorang.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    @Value("${app.upload.video.dir:uploads/videos}")
    private String videoUploadDir;
    
    @Value("${app.upload.thumbnail.dir:uploads/thumbnails}")
    private String thumbnailUploadDir;
    
    // 비디오 파일 저장
    public FileInfo storeVideoFile(MultipartFile file, String userId, Long projectId) {
        try {
            // 파일 유효성 검사
            validateVideoFile(file);
            
            // 저장 디렉토리 생성
            Path uploadPath = createUploadDirectory(videoUploadDir, userId, projectId);
            
            // 고유한 파일명 생성
            String originalFileName = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFileName);
            String storedFileName = generateUniqueFileName(fileExtension);
            
            // 파일 저장
            Path targetLocation = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("비디오 파일 저장 완료: {}", targetLocation.toString());
            
            return FileInfo.builder()
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .filePath(targetLocation.toString())
                    .relativePath(getRelativePath(targetLocation))
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .build();
                    
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", e.getMessage());
            throw new RuntimeException("파일 저장에 실패했습니다: " + e.getMessage());
        }
    }
    
    // 썸네일 파일 저장
    public FileInfo storeThumbnailFile(byte[] thumbnailData, String userId, Long projectId, String originalVideoFileName) {
        try {
            // 저장 디렉토리 생성
            Path uploadPath = createUploadDirectory(thumbnailUploadDir, userId, projectId);
            
            // 썸네일 파일명 생성
            String storedFileName = generateThumbnailFileName(originalVideoFileName);
            
            // 파일 저장
            Path targetLocation = uploadPath.resolve(storedFileName);
            Files.write(targetLocation, thumbnailData);
            
            log.info("썸네일 파일 저장 완료: {}", targetLocation.toString());
            
            return FileInfo.builder()
                    .originalFileName(storedFileName)
                    .storedFileName(storedFileName)
                    .filePath(targetLocation.toString())
                    .relativePath(getRelativePath(targetLocation))
                    .fileSize((long) thumbnailData.length)
                    .contentType("image/jpeg")
                    .build();
                    
        } catch (IOException e) {
            log.error("썸네일 저장 실패: {}", e.getMessage());
            throw new RuntimeException("썸네일 저장에 실패했습니다: " + e.getMessage());
        }
    }
    
    // 파일 삭제
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            boolean deleted = Files.deleteIfExists(path);
            
            if (deleted) {
                log.info("파일 삭제 완료: {}", filePath);
            } else {
                log.warn("삭제할 파일이 존재하지 않습니다: {}", filePath);
            }
            
            return deleted;
            
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", e.getMessage());
            return false;
        }
    }
    
    // 디렉토리 생성
    private Path createUploadDirectory(String baseDir, String userId, Long projectId) throws IOException {
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path uploadPath = Paths.get(baseDir, userId, projectId.toString(), dateDir);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.debug("디렉토리 생성: {}", uploadPath.toString());
        }
        
        return uploadPath;
    }
    
    // 비디오 파일 유효성 검사
    private void validateVideoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("파일이 비어있습니다.");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !isVideoFile(contentType)) {
            throw new RuntimeException("지원하지 않는 파일 형식입니다. 비디오 파일만 업로드 가능합니다.");
        }
        
        // 파일 크기 제한 (500MB)
        long maxFileSize = 500 * 1024 * 1024; // 500MB
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("파일 크기가 너무 큽니다. 최대 500MB까지 업로드 가능합니다.");
        }
    }
    
    // 비디오 파일 여부 확인
    private boolean isVideoFile(String contentType) {
        return contentType.startsWith("video/") ||
               contentType.equals("application/octet-stream"); // Blob 업로드 시
    }
    
    // 파일 확장자 추출
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return ".mp4"; // 기본 확장자
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }
    
    // 고유한 파일명 생성
    private String generateUniqueFileName(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s%s", timestamp, uuid, extension);
    }
    
    // 썸네일 파일명 생성
    private String generateThumbnailFileName(String originalVideoFileName) {
        String baseName = originalVideoFileName.substring(0, originalVideoFileName.lastIndexOf('.'));
        return baseName + "_thumbnail.jpg";
    }
    
    // 상대 경로 생성
    private String getRelativePath(Path absolutePath) {
        Path basePath = Paths.get(uploadDir);
        return basePath.relativize(absolutePath).toString().replace("\\", "/");
    }
    
    // 파일 정보 클래스
    public static class FileInfo {
        public final String originalFileName;
        public final String storedFileName;
        public final String filePath;
        public final String relativePath;
        public final Long fileSize;
        public final String contentType;
        
        private FileInfo(Builder builder) {
            this.originalFileName = builder.originalFileName;
            this.storedFileName = builder.storedFileName;
            this.filePath = builder.filePath;
            this.relativePath = builder.relativePath;
            this.fileSize = builder.fileSize;
            this.contentType = builder.contentType;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String originalFileName;
            private String storedFileName;
            private String filePath;
            private String relativePath;
            private Long fileSize;
            private String contentType;
            
            public Builder originalFileName(String originalFileName) {
                this.originalFileName = originalFileName;
                return this;
            }
            
            public Builder storedFileName(String storedFileName) {
                this.storedFileName = storedFileName;
                return this;
            }
            
            public Builder filePath(String filePath) {
                this.filePath = filePath;
                return this;
            }
            
            public Builder relativePath(String relativePath) {
                this.relativePath = relativePath;
                return this;
            }
            
            public Builder fileSize(Long fileSize) {
                this.fileSize = fileSize;
                return this;
            }
            
            public Builder contentType(String contentType) {
                this.contentType = contentType;
                return this;
            }
            
            public FileInfo build() {
                return new FileInfo(this);
            }
        }
    }
} 