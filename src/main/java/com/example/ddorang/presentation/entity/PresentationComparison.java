package com.example.ddorang.presentation.entity;

import com.example.ddorang.auth.entity.User;
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "presentation_comparison")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PresentationComparison {

    @Id @GeneratedValue
    @Column(name = "comparison_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presentation_id_1", nullable = false)
    private Presentation presentation1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presentation_id_2", nullable = false)
    private Presentation presentation2;

    @Column(name = "comparison_data", columnDefinition = "JSON")
    private String comparisonData;

    @Column(name = "comparison_summary", columnDefinition = "TEXT")
    private String comparisonSummary;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
