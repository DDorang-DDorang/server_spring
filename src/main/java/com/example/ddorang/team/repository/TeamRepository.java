package com.example.ddorang.team.repository;

import com.example.ddorang.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    
    @Query("SELECT DISTINCT t FROM Team t JOIN FETCH t.members tm JOIN FETCH tm.user WHERE tm.user.userId = :userId ORDER BY t.createdAt DESC")
    List<Team> findTeamsByUserId(UUID userId);
    
    @Query("SELECT tm FROM TeamMember tm JOIN FETCH tm.team t JOIN FETCH tm.user u WHERE tm.user.userId = :userId ORDER BY t.createdAt DESC")
    List<com.example.ddorang.team.entity.TeamMember> findTeamMembersByUserId(UUID userId);
    
    List<Team> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);
}