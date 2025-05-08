package com.example.ddo_auth.repository;

import com.example.ddo_auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // 이메일 중복 체크
    boolean existsByEmail(String email);

    // 로그인용으로 유저 찾기
    Optional<User> findByEmail(String email);
}
