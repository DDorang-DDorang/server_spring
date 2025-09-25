package com.example.ddorang.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("잘못된 인수: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_ARGUMENT", e.getMessage()));
    }

    @ExceptionHandler({SecurityException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleSecurityException(Exception e) {
        log.error("권한 없음: {} (예외 타입: {})", e.getMessage(), e.getClass().getName(), e);
        return ResponseEntity.status(403)
                .body(ErrorResponse.of("ACCESS_DENIED", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        log.error("상태 오류: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_STATE", e.getMessage()));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedOperationException(UnsupportedOperationException e) {
        log.error("지원하지 않는 작업: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("UNSUPPORTED_OPERATION", e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(Exception e) {
        log.error("유효성 검증 실패: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("VALIDATION_FAILED", "입력값이 올바르지 않습니다."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("런타임 오류: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("RUNTIME_ERROR", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("예상치 못한 오류: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
    }
}