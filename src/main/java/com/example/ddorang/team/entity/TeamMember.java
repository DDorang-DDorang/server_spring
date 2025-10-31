package com.example.ddorang.team.entity;

import com.example.ddorang.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "team_member",
       uniqueConstraints = {
           @UniqueConstraint(
               name = "uk_team_member_team_user", 
               columnNames = {"team_id", "user_id"}
           )
       },
       indexes = {
           @Index(name = "idx_team_member_team_id", columnList = "team_id"),
           @Index(name = "idx_team_member_user_id", columnList = "user_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMember {

    @Id
    @GeneratedValue
    @Column(name = "team_member_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.MEMBER;

    public enum Role {
        OWNER, MEMBER
    }

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;
}