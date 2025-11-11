package com.msa.common.global.redis.enum_type;

public enum RedisEventStatus {
    NEW_REQUEST, // 최초 요청 (처리 시작)
    PROCESSING,  // 처리 중 (중복 요청)
    COMPLETED    // 처리 완료 (중복 요청)
}
