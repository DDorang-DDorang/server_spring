package com.example.ddorang.presentation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "presentation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Presentation {

    @Id
    @GeneratedValue
    @Column(name = "presentation_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "script", columnDefinition = "TEXT")
    private String script;

    @Column(name = "video_url", columnDefinition = "TEXT")
    private String videoUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "goal_time")
    private Integer goalTime;

    @OneToMany(mappedBy = "presentation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
}
