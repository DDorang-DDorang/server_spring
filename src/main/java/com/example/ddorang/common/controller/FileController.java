package com.example.ddorang.common.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
@Slf4j
public class FileController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // 메인 비디오 파일 제공 엔드포인트
    @GetMapping("/videos/**")
    public ResponseEntity<Resource> getVideoFile(HttpServletRequest request) {
        try {
            // URL에서 /api/files/videos/ 이후 경로 추출
            String requestURL = request.getRequestURL().toString();
            String contextPath = request.getContextPath();
            String servletPath = request.getServletPath();
            String pathInfo = request.getPathInfo();
            
            // /api/files/videos/ 이후의 전체 경로 추출
            String fullPath = request.getRequestURI();
            String videoPath = fullPath.substring(fullPath.indexOf("/api/files/videos/") + "/api/files/videos/".length());
            
            // videos/videos/ 중복 제거 (기존 잘못된 URL 호환성)
            if (videoPath.startsWith("videos/")) {
                videoPath = videoPath.substring("videos/".length());
            }
            
            log.info("비디오 파일 요청 경로: {}", videoPath);
            
            // 파일 경로 구성 (uploadDir + videos + videoPath)
            Path filePath = Paths.get(uploadDir).resolve("videos").resolve(videoPath).normalize();
            
            log.info("비디오 파일 절대 경로: {}", filePath);
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("파일을 찾을 수 없거나 읽을 수 없습니다: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            // 파일명 추출
            String filename = filePath.getFileName().toString();
            
            // 파일 타입 결정
            String contentType = "application/octet-stream";
            String lowerFilename = filename.toLowerCase();
            
            if (lowerFilename.endsWith(".mp4")) {
                contentType = "video/mp4";
            } else if (lowerFilename.endsWith(".webm")) {
                contentType = "video/webm";
            } else if (lowerFilename.endsWith(".avi")) {
                contentType = "video/x-msvideo";
            } else if (lowerFilename.endsWith(".mov")) {
                contentType = "video/quicktime";
            }
            
            log.info("비디오 파일 제공 성공: {} (타입: {})", filePath, contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000") // 1년 캐시
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*") // CORS 헤더 추가
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("파일 제공 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // 헬스체크용 엔드포인트
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("File service is running. Upload directory: " + uploadDir);
    }
} 