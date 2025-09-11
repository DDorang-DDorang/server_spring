package com.example.ddorang.presentation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class FastApiService {

    @Value("${fastapi.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public FastApiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().build();
    }

    /**
     * FastAPI에 비디오 파일을 전송하여 음성 분석 수행 (WebClient 사용)
     */
    public Map<String, Object> analyzeVideo(MultipartFile videoFile) throws IOException {
        log.info("FastAPI 음성 분석 요청 시작: {}, 파일 크기: {} bytes",
                videoFile.getOriginalFilename(), videoFile.getSize());

        String url = fastApiBaseUrl + "/stt";
        File tempFile = null;

        try {
            // 1. MultipartFile을 임시 파일로 저장
            String originalFilename = videoFile.getOriginalFilename();
            tempFile = File.createTempFile("upload-", ".mp4");
            videoFile.transferTo(tempFile);

            // 2. WebClient로 multipart/form-data 전송 (필드명 video로 변경, metadata 추가)
            MultiValueMap<String, Object> multipartData = new LinkedMultiValueMap<>();
            multipartData.add("video", new FileSystemResource(tempFile));
            multipartData.add("metadata", "{}"); // 빈 JSON 객체로 metadata 추가
            
            String responseBody = webClient.mutate()
                    .baseUrl(fastApiBaseUrl)
                    .build()
                    .post()
                    .uri("/stt")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(multipartData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("FastAPI 응답: {}", responseBody);

            // 3. 응답 파싱
            return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

        } catch (Exception e) {
            log.error("FastAPI 통신 오류 발생", e);
            return createMockAnalysisResult(videoFile.getOriginalFilename());
        } finally {
            // 임시 파일 삭제
            if (tempFile != null && tempFile.exists()) {
                if (tempFile.delete()) {
                    log.info("임시 파일 삭제 성공: {}", tempFile.getAbsolutePath());
                } else {
                    log.warn("임시 파일 삭제 실패: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * FastAPI에 대본 최적화 요청 전송
     */
    public Map<String, Object> optimizeScript(String script, Integer goalTimeSeconds, Integer currentDurationSeconds) {
        log.info("FastAPI 대본 최적화 요청 시작: 목표시간={}초, 현재시간={}초", 
                goalTimeSeconds, currentDurationSeconds);

        try {
            // 요청 데이터 구성
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("script", script);
            requestData.put("goal_time_seconds", goalTimeSeconds);
            requestData.put("current_duration_seconds", currentDurationSeconds);

            // WebClient로 POST 요청 전송
            String responseBody = webClient.mutate()
                    .baseUrl(fastApiBaseUrl)
                    .build()
                    .post()
                    .uri("/optimize-script")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("FastAPI 대본 최적화 응답: {}", responseBody);

            // 응답 파싱
            return objectMapper.readValue(responseBody, new TypeReference<>() {});

        } catch (Exception e) {
            log.error("FastAPI 대본 최적화 통신 오류 발생", e);
            return createMockOptimizeResult(script, goalTimeSeconds);
        }
    }

    /**
     * FastAPI 서버 연결 실패 시 사용할 목 대본 최적화 결과
     */
    private Map<String, Object> createMockOptimizeResult(String originalScript, Integer goalTimeSeconds) {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("optimized_script", originalScript + "\n\n[목 데이터] 대본이 " + goalTimeSeconds + "초에 맞게 최적화되었습니다. (실제 최적화는 FastAPI 서버 연결 후 가능)");
        mockResult.put("optimization_notes", "FastAPI 서버(" + fastApiBaseUrl + ")에 연결하여 실제 LLM 기반 대본 최적화를 이용하세요.");
        mockResult.put("estimated_duration_seconds", goalTimeSeconds);
        log.info("목 대본 최적화 결과 생성 완료: 목표시간 {}초", goalTimeSeconds);
        return mockResult;
    }

    /**
     * FastAPI 서버 연결 실패 시 사용할 목 분석 결과
     */
    private Map<String, Object> createMockAnalysisResult(String filename) {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("intensity_grade", "보통");
        mockResult.put("intensity_db", 65.5f);
        mockResult.put("intensity_text", "음성 강도가 적절합니다. (목 데이터 - FastAPI 서버 연결 필요)");
        mockResult.put("pitch_grade", "좋음");
        mockResult.put("pitch_avg", 150.2f);
        mockResult.put("pitch_text", "피치 변화가 자연스럽습니다. (목 데이터 - FastAPI 서버 연결 필요)");
        mockResult.put("wpm_grade", "보통");
        mockResult.put("wpm_avg", 120.5f);
        mockResult.put("wpm_comment", "말하기 속도가 적당합니다. (목 데이터 - FastAPI 서버 연결 필요)");
        mockResult.put("transcription", "안녕하세요. 이것은 테스트용 목 데이터입니다. 실제 음성 인식 결과는 FastAPI 서버(" + fastApiBaseUrl + ") 연결 후 확인하실 수 있습니다.");
        mockResult.put("pronunciation_score", 0.75f);
        mockResult.put("duration_seconds", 120); // Mock 데이터로 2분(120초) 설정
        log.info("목 분석 결과 생성 완료: {}", filename);
        return mockResult;
    }

    /**
     * 영상 분석 결과에서 실제 영상 길이 추출
     */
    public Integer extractDurationFromAnalysis(Map<String, Object> analysisResult) {
        if (analysisResult == null) {
            return null;
        }
        
        Object duration = analysisResult.get("duration_seconds");
        if (duration instanceof Integer) {
            return (Integer) duration;
        } else if (duration instanceof Number) {
            return ((Number) duration).intValue();
        }
        
        log.warn("분석 결과에서 duration_seconds를 찾을 수 없습니다: {}", analysisResult.keySet());
        return null;
    }
}
