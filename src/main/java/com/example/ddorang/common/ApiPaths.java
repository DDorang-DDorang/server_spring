package com.example.ddorang.common;

/* 경로 상수화 */
public interface ApiPaths {
    /* 인증 관련
    /* 공통 루트 */
    String ROOT = "/api";

    /* Auth(로컬 이메일/비밀번호) */
    String AUTH = ROOT + "/auth";

    /* OAuth(Google) */
    String OAUTH = ROOT + "/oauth2";

    /* Refresh Token*/
    String TOKEN_REFRESH = "/token/refresh";

    /* Logout */
    String TOKEN_LOGOUT = "/token/logout";
}
