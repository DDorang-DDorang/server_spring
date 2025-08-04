package com.example.ddorang.team.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

// DEPRECATED: Redis 기반 초대 시스템으로 변경됨 - 이 엔티티는 더 이상 사용되지 않음
@Entity
@Table(name = "team_invite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Deprecated
public class TeamInvite {

    @Id
    @GeneratedValue
    @Column(name = "invite_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "invite_code", unique = true, nullable = false, length = 32)
    private String inviteCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "current_uses", nullable = false)
    @Builder.Default
    private Integer currentUses = 0;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return isActive && !isExpired() && (maxUses == null || currentUses < maxUses);
    }
}