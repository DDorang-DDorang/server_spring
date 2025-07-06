package com.example.ddorang.presentation.service;

import com.example.ddorang.auth.entity.User;
import com.example.ddorang.auth.repository.UserRepository;
import com.example.ddorang.presentation.dto.CommentCreateRequest;
import com.example.ddorang.presentation.dto.CommentResponse;
import com.example.ddorang.presentation.dto.CommentUpdateRequest;
import com.example.ddorang.presentation.entity.Comment;
import com.example.ddorang.presentation.entity.Presentation;
import com.example.ddorang.presentation.repository.CommentRepository;
import com.example.ddorang.presentation.repository.PresentationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final PresentationRepository presentationRepository;
    private final UserRepository userRepository;
    
    // 댓글 생성
    @Transactional
    public CommentResponse createComment(UUID presentationId, UUID userId, CommentCreateRequest request) {
        log.info("댓글 생성: 프레젠테이션 {}, 사용자 {}", presentationId, userId);
        
        // 프레젠테이션 존재 확인
        Presentation presentation = presentationRepository.findById(presentationId)
                .orElseThrow(() -> new RuntimeException("프레젠테이션을 찾을 수 없습니다."));
        
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 부모 댓글 확인 (대댓글인 경우)
        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("부모 댓글을 찾을 수 없습니다."));
            
            // 부모 댓글이 같은 프레젠테이션에 속하는지 확인
            if (!parentComment.getPresentation().getId().equals(presentationId)) {
                throw new RuntimeException("부모 댓글이 해당 프레젠테이션에 속하지 않습니다.");
            }
        }
        
        Comment comment = Comment.builder()
                .presentation(presentation)
                .user(user)
                .content(request.getContent())
                .timestamp(request.getTimestamp())
                .parentComment(parentComment)
                .createdAt(LocalDateTime.now())
                .build();
        
        Comment savedComment = commentRepository.save(comment);
        log.info("댓글 생성 완료: {}", savedComment.getId());
        
        return CommentResponse.fromWithoutReplies(savedComment);
    }
    
    // 댓글 수정
    @Transactional
    public CommentResponse updateComment(UUID commentId, UUID userId, CommentUpdateRequest request) {
        log.info("댓글 수정: {}, 사용자 {}", commentId, userId);
        
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        
        // 댓글 작성자 확인
        if (!comment.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("댓글을 수정할 권한이 없습니다.");
        }
        
        comment.setContent(request.getContent());
        comment.setUpdatedAt(LocalDateTime.now());
        
        Comment updatedComment = commentRepository.save(comment);
        log.info("댓글 수정 완료: {}", updatedComment.getId());
        
        return CommentResponse.fromWithoutReplies(updatedComment);
    }
    
    // 댓글 삭제
    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        log.info("댓글 삭제: {}, 사용자 {}", commentId, userId);
        
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        
        // 댓글 작성자 확인
        if (!comment.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("댓글을 삭제할 권한이 없습니다.");
        }
        
        commentRepository.delete(comment);
        log.info("댓글 삭제 완료: {}", commentId);
    }
    
    // 프레젠테이션의 댓글 목록 조회 (시간순)
    public List<CommentResponse> getCommentsByPresentationId(UUID presentationId, String sortBy) {
        log.info("프레젠테이션 {} 댓글 목록 조회, 정렬: {}", presentationId, sortBy);
        
        List<Comment> comments;
        if ("createdAt".equals(sortBy)) {
            comments = commentRepository.findByPresentationIdOrderByCreatedAt(presentationId);
        } else {
            comments = commentRepository.findByPresentationIdOrderByTimestamp(presentationId);
        }
        
        return comments.stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }
    
    // 특정 댓글의 대댓글 조회
    public List<CommentResponse> getRepliesByCommentId(UUID commentId) {
        log.info("댓글 {} 대댓글 조회", commentId);
        
        List<Comment> replies = commentRepository.findRepliesByParentCommentId(commentId);
        
        return replies.stream()
                .map(CommentResponse::fromWithoutReplies)
                .collect(Collectors.toList());
    }
    
    // 사용자의 댓글 목록 조회
    public List<CommentResponse> getCommentsByUserId(UUID userId) {
        log.info("사용자 {} 댓글 목록 조회", userId);
        
        List<Comment> comments = commentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        return comments.stream()
                .map(CommentResponse::fromWithoutReplies)
                .collect(Collectors.toList());
    }
    
    // 댓글 검색
    public List<CommentResponse> searchComments(UUID presentationId, String keyword) {
        log.info("프레젠테이션 {} 댓글 검색: {}", presentationId, keyword);
        
        List<Comment> comments = commentRepository.searchCommentsByKeyword(presentationId, keyword);
        
        return comments.stream()
                .map(CommentResponse::fromWithoutReplies)
                .collect(Collectors.toList());
    }
    
    // 댓글 통계 - 프레젠테이션별 전체 댓글 수
    public long getCommentCountByPresentationId(UUID presentationId) {
        log.info("프레젠테이션 {} 댓글 수 조회", presentationId);
        return commentRepository.countByPresentationId(presentationId);
    }
    
    // 댓글 통계 - 대댓글 수
    public long getReplyCountByCommentId(UUID commentId) {
        log.info("댓글 {} 대댓글 수 조회", commentId);
        return commentRepository.countRepliesByParentCommentId(commentId);
    }
    
    // 댓글 작성자 확인
    public boolean isCommentOwner(UUID commentId, UUID userId) {
        return commentRepository.existsByIdAndUserId(commentId, userId);
    }
}