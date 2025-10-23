package com.example.ddorang.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 설정
 *
 * @Async 어노테이션을 활성화하여 비동기 메서드 실행을 지원합니다.
 * VideoAnalysisService의 비동기 영상 분석에 필요
 */
@Configuration
@EnableAsync
public class AsyncConfig {

}