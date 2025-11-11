package com.msa.common.global.redis.service;

import com.msa.common.global.redis.enum_type.RedisEventStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisEventService {

    private final long eventTTL = 600;
    private static final String KEY_PREFIX = "idempotency:";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisEventService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void createNewEventId(String tenantId, String eventId) {
        String key = KEY_PREFIX + tenantId + ":" + eventId;
        redisTemplate.opsForValue().set(key, eventId, eventTTL, TimeUnit.SECONDS);
    }

    public RedisEventStatus checkAndSetProcessing(String tenantId, String eventId) {
        String key = KEY_PREFIX + tenantId + ":" + eventId;

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, STATUS_PROCESSING, eventTTL, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(success)) {
            return RedisEventStatus.NEW_REQUEST;
        }

        Object status = redisTemplate.opsForValue().get(key);

        if (STATUS_PROCESSING.equals(status)) {
            return RedisEventStatus.PROCESSING;
        }

        return RedisEventStatus.COMPLETED;
    }

    public void setCompleted(String tenantId, String eventId, Object response) {
        String key = KEY_PREFIX + tenantId + ":" + eventId;
        redisTemplate.opsForValue().set(key, response, eventTTL, TimeUnit.SECONDS);
    }


    public void deleteEventId(String tenantId, String eventId) {
        String key = KEY_PREFIX + tenantId + ":" + eventId;
        redisTemplate.delete(key);
    }

}
