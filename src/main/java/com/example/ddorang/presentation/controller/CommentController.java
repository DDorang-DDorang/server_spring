package com.example.ddorang.presentation.controller;

import com.example.ddorang.common.ApiPaths;
import com.example.ddorang.presentation.dto.CommentCreateRequest;
import com.example.ddorang.presentation.dto.CommentResponse;
import com.example.ddorang.presentation.dto.CommentUpdateRequest;
import com.example.ddorang.presentation.service.CommentService;
import com.example.ddorang.auth.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.ROOT)
@RequiredArgsConstructor
@Slf4j
public class CommentController {
    
    private final CommentService commentService;
    
    // 댓글 생성
    @PostMapping("/presentations/{presentationId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID presentationId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.info("댓글 생성 요청 - 프레젠테이션: {}, 사용자: {}", presentationId, userDetails.getUser().getUserId());
        
        try {
            CommentResponse response = commentService.createComment(
                    presentationId, 
                    userDetails.getUser().getUserId(), 
                    request
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("댓글 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 댓글 수정
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.info("댓글 수정 요청 - ID: {}, 사용자: {}", commentId, userDetails.getUser().getUserId());
        
        try {
            CommentResponse response = commentService.updateComment(
                    commentId, 
                    userDetails.getUser().getUserId(), 
                    request
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("댓글 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.info("댓글 삭제 요청 - ID: {}, 사용자: {}", commentId, userDetails.getUser().getUserId());
        
        try {
            commentService.deleteComment(commentId, userDetails.getUser().getUserId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("댓글 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 프레젠테이션의 댓글 목록 조회
    @GetMapping("/presentations/{presentationId}/comments")
    public ResponseEntity<List<CommentResponse>> getCommentsByPresentation(
            @PathVariable UUID presentationId,
            @RequestParam(defaultValue = "timestamp") String sortBy) {
        
        log.info("댓글 목록 조회 요청 - 프레젠테이션: {}, 정렬: {}", presentationId, sortBy);
        
        try {
            List<CommentResponse> response = commentService.getCommentsByPresentationId(presentationId, sortBy);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("댓글 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 특정 댓글의 대댓글 조회
    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<List<CommentResponse>> getRepliesByComment(
            @PathVariable UUID commentId) {
        
        log.info("대댓글 조회 요청 - 댓글: {}", commentId);
        
        try {
            List<CommentResponse> response = commentService.getRepliesByCommentId(commentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("대댓글 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 사용자의 댓글 목록 조회
    @GetMapping("/users/{userId}/comments")
    public ResponseEntity<List<CommentResponse>> getCommentsByUser(
            @PathVariable UUID userId) {
        
        log.info("사용자 댓글 목록 조회 요청 - 사용자: {}", userId);
        
        try {
            List<CommentResponse> response = commentService.getCommentsByUserId(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("사용자 댓글 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 댓글 검색
    @GetMapping("/presentations/{presentationId}/comments/search")
    public ResponseEntity<List<CommentResponse>> searchComments(
            @PathVariable UUID presentationId,
            @RequestParam String keyword) {
        
        log.info("댓글 검색 요청 - 프레젠테이션: {}, 키워드: {}", presentationId, keyword);
        
        try {
            List<CommentResponse> response = commentService.searchComments(presentationId, keyword);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("댓글 검색 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 댓글 통계 - 프레젠테이션별 전체 댓글 수
    @GetMapping("/presentations/{presentationId}/comments/count")
    public ResponseEntity<CommentCountResponse> getCommentCount(
            @PathVariable UUID presentationId) {
        
        log.info("댓글 수 조회 요청 - 프레젠테이션: {}", presentationId);
        
        try {
            long count = commentService.getCommentCountByPresentationId(presentationId);
            CommentCountResponse response = new CommentCountResponse(count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("댓글 수 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 대댓글 수 조회
    @GetMapping("/comments/{commentId}/replies/count")
    public ResponseEntity<CommentCountResponse> getReplyCount(
            @PathVariable UUID commentId) {
        
        log.info("대댓글 수 조회 요청 - 댓글: {}", commentId);
        
        try {
            long count = commentService.getReplyCountByCommentId(commentId);
            CommentCountResponse response = new CommentCountResponse(count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("대댓글 수 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 댓글 소유자 확인
    @GetMapping("/comments/{commentId}/ownership")
    public ResponseEntity<CommentOwnershipResponse> checkCommentOwnership(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.info("댓글 소유자 확인 요청 - 댓글: {}, 사용자: {}", commentId, userDetails.getUser().getUserId());
        
        try {
            boolean isOwner = commentService.isCommentOwner(commentId, userDetails.getUser().getUserId());
            CommentOwnershipResponse response = new CommentOwnershipResponse(isOwner);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("댓글 소유자 확인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 응답 클래스들
    public static class CommentCountResponse {
        private long count;
        
        public CommentCountResponse(long count) {
            this.count = count;
        }
        
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }
    
    public static class CommentOwnershipResponse {
        private boolean isOwner;
        
        public CommentOwnershipResponse(boolean isOwner) {
            this.isOwner = isOwner;
        }
        
        public boolean isOwner() { return isOwner; }
        public void setOwner(boolean owner) { isOwner = owner; }
    }
}