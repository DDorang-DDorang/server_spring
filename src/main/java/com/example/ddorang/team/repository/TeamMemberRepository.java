package com.example.ddorang.team.repository;

import com.example.ddorang.auth.entity.User;
import com.example.ddorang.team.entity.Team;
import com.example.ddorang.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    
    @Query("SELECT tm FROM TeamMember tm JOIN FETCH tm.user u WHERE tm.team = :team ORDER BY tm.joinedAt ASC")
    List<TeamMember> findByTeamOrderByJoinedAtAsc(@Param("team") Team team);
    
    List<TeamMember> findByUserOrderByJoinedAtDesc(User user);
    
    Optional<TeamMember> findByTeamAndUser(Team team, User user);
    
    boolean existsByTeamAndUser(Team team, User user);
    
    @Query("SELECT tm FROM TeamMember tm WHERE tm.team = :team AND tm.role = 'OWNER'")
    List<TeamMember> findTeamOwners(Team team);
    
    @Query("SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team = :team")
    long countByTeam(Team team);
}