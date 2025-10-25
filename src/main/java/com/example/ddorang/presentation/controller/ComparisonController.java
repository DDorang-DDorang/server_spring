package com.example.ddorang.presentation.controller;

import com.example.ddorang.presentation.dto.ComparisonResponseDto;
import com.example.ddorang.presentation.service.ComparisonService;
import com.example.ddorang.auth.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/presentations")
@RequiredArgsConstructor
@Slf4j
public class ComparisonController {
    
    private final ComparisonService comparisonService;
    
    /**
     * 두 발표 비교
     */
    @PostMapping("/{presentationId}/compare-with/{otherPresentationId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ComparisonResponseDto> comparePresentations(
            @PathVariable UUID presentationId,
            @PathVariable UUID otherPresentationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.info("발표 비교 요청 - 사용자: {}, 발표1: {}, 발표2: {}", 
                userDetails.getUser().getUserId(), presentationId, otherPresentationId);
        
        ComparisonResponseDto comparison = comparisonService.comparePresentations(
                userDetails.getUser().getUserId(), presentationId, otherPresentationId);
        
        return ResponseEntity.ok(comparison);
    }
    
    /**
     * 사용자의 모든 비교 기록 조회
     */
    @GetMapping("/comparisons")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ComparisonResponseDto>> getUserComparisons(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.info("사용자 비교 기록 조회 - 사용자: {}", userDetails.getUser().getUserId());
        
        List<ComparisonResponseDto> comparisons = comparisonService.getUserComparisons(userDetails.getUser().getUserId());
        
        return ResponseEntity.ok(comparisons);
    }
    
    /**
     * 특정 발표와 관련된 모든 비교 기록 조회
     */
    @GetMapping("/{presentationId}/comparisons")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ComparisonResponseDto>> getComparisonsInvolving(
            @PathVariable UUID presentationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.info("발표 관련 비교 기록 조회 - 사용자: {}, 발표: {}", 
                userDetails.getUser().getUserId(), presentationId);
        
        List<ComparisonResponseDto> comparisons = comparisonService.getComparisonsInvolving(
                userDetails.getUser().getUserId(), presentationId);
        
        return ResponseEntity.ok(comparisons);
    }
}