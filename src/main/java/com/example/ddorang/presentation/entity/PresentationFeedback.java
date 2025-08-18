package com.example.ddorang.presentation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "presentation_feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationFeedback {

    @Id
    @GeneratedValue
    @Column(name = "feedback_id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presentation_id", nullable = false)
    private Presentation presentation;

    // 자주 사용된 단어들 (JSON 형태로 저장)
    @Column(name = "frequent_words", columnDefinition = "TEXT")
    private String frequentWords;

    // 어색한 문장들 (JSON 형태로 저장)
    @Column(name = "awkward_sentences", columnDefinition = "TEXT")
    private String awkwardSentences;

    // 난이도 문제들 (JSON 형태로 저장)
    @Column(name = "difficulty_issues", columnDefinition = "TEXT")
    private String difficultyIssues;

    // 예측된 질문들 (JSON 형태로 저장)
    @Column(name = "predicted_questions", columnDefinition = "TEXT")
    private String predictedQuestions;
} 