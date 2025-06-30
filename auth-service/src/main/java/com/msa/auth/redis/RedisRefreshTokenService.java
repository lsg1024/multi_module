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
    private long REFRESH_TTL;
    private static final String keyPrefix = "refreshToken:";
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisRefreshTokenService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void createNewToken(String owner, String nickname, String token) {
        log.info("createNewToken");
        String key = keyPrefix + owner + ":" + nickname;
        redisTemplate.opsForValue().set(key, token, REFRESH_TTL, TimeUnit.SECONDS);
    }

    public void updateNewToken(String owner, String nickname, String token) {
        log.info("updateNewToken");
        createNewToken(owner, nickname, token);
    }

    public boolean existsToken(String owner, String nickname) {
        String key = keyPrefix + owner + ":" + nickname;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    public void deleteToken(String owner, String nickname) {
        String key = keyPrefix + owner + ":" + nickname;
        redisTemplate.delete(key);
    }
}