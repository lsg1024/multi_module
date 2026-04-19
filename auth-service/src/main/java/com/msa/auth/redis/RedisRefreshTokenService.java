package com.msa.auth.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 refresh token 저장/조회/삭제 서비스.
 *
 * *Redis 키 형식: {@code refreshToken:{tenantId}:{forward}:{userAgent}:{nickname}}
 *
 * *주요 책임:
 *
 *   - 신규 토큰 저장 — TTL({@code jwt.refresh_ttl} 초) 적용
 *   - 토큰 갱신 — 기존 키에 새 값 덮어쓰기 ({@link #createNewToken} 재사용)
 *   - 토큰 존재 여부 확인
 *   - 토큰 삭제 — cross-tenant 감지 시 또는 로그아웃 시 호출
 * 
 *
 * *의존성: {@link org.springframework.data.redis.core.RedisTemplate}
 */
@Slf4j
@Service
public class RedisRefreshTokenService {

    @Value("${jwt.refresh_ttl}")
    private String REFRESH_TTL;
    private static final String keyPrefix = "refreshToken:";
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisRefreshTokenService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void createNewToken(String tenantId, String forward, String userAgent, String nickname, String token) {
        log.info("createNewToken");
        String key = keyPrefix + tenantId + ":" + forward + ":" + userAgent + ":" + nickname;
        redisTemplate.opsForValue().set(key, token, Long.parseLong(REFRESH_TTL), TimeUnit.SECONDS);
    }

    public void updateNewToken(String tenantId, String forward, String device, String nickname, String token) {
        log.info("updateNewToken");
        createNewToken(tenantId, forward, device, nickname, token);
    }

    public boolean existsToken(String tenantId, String nickname) {
        String key = keyPrefix + tenantId + ":" + nickname;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    public void deleteToken(String tenantId, String nickname) {
        String key = keyPrefix + tenantId + ":" + nickname;
        redisTemplate.delete(key);
    }

    public void deleteByKey(String key) {
        log.info("deleteByKey: {}", key);
        redisTemplate.delete(key);
    }
}