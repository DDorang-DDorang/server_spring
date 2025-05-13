package com.example.ddo_auth.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "refreshToken", timeToLive = 604800)
public class RefreshToken {

    @Id
    private String email;
    private String token;

//    //refresh token 갱신 메서드 (탈취 가능성 있을 때)
//    public void updateToken(String newToken) {
//        this.token = newToken;
//    }
}
