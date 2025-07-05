package com.msa.auth.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

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
}