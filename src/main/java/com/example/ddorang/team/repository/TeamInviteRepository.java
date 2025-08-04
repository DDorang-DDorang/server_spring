package com.example.ddorang.team.repository;

import com.example.ddorang.team.entity.Team;
import com.example.ddorang.team.entity.TeamInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// DEPRECATED: Redis 기반 초대 시스템으로 변경됨 - 이 Repository는 더 이상 사용되지 않음
@Repository
@Deprecated
public interface TeamInviteRepository extends JpaRepository<TeamInvite, UUID> {
    
    Optional<TeamInvite> findByInviteCodeAndIsActiveTrue(String inviteCode);
    
    List<TeamInvite> findByTeamAndIsActiveTrueOrderByCreatedAtDesc(Team team);
    
    @Query("SELECT ti FROM TeamInvite ti WHERE ti.team = :team AND ti.isActive = true AND ti.expiresAt > :now")
    List<TeamInvite> findActiveInvitesByTeam(Team team, LocalDateTime now);
    
    @Query("DELETE FROM TeamInvite ti WHERE ti.expiresAt < :now")
    void deleteExpiredInvites(LocalDateTime now);
}