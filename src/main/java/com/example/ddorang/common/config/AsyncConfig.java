package com.example.ddorang.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 및 스케줄링 설정
 *
 * @Async 어노테이션을 활성화하여 비동기 메서드 실행을 지원합니다.
 * @Scheduled 어노테이션을 활성화하여 주기적 작업 실행을 지원합니다.
 * VideoAnalysisService의 비동기 영상 분석에 필요
 *
 * 스레드 풀 설정:
 * - Core Pool: 5개 (기본 유지 스레드)
 * - Max Pool: 20개 (최대 스레드)
 * - Queue Capacity: 100개 (대기열)
 */
@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 스레드 수 (항상 유지)
        executor.setCorePoolSize(5);

        // 최대 스레드 수
        executor.setMaxPoolSize(20);

        // 큐 용량 (대기 작업 수)
        executor.setQueueCapacity(100);

        // 스레드 이름 접두사 (로그 추적 용이)
        executor.setThreadNamePrefix("async-video-");

        // 큐가 가득 찬 경우 처리 정책: 호출한 스레드에서 직접 실행
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 스레드 풀 초기화
        executor.initialize();

        log.info("비동기 스레드 풀 설정 완료: core={}, max={}, queue={}",
            executor.getCorePoolSize(),
            executor.getMaxPoolSize(),
            executor.getQueueCapacity());

        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("비동기 작업 실패: method={}, error={}",
                method.getName(), ex.getMessage(), ex);
        };
    }
}