package com.msa.jewelry.config;

/**
 * REMOVED — 통합 시 중복 Bean 충돌로 인해 비활성화.
 *
 * com.msa.common.global.redis.config.RedisConfig 가 이미 RedisTemplate Bean 을 정의하고
 * jewelry_emp 의 ComponentScan 이 com.msa.common 을 포함하므로 자동으로 활용된다.
 * 본 클래스는 placeholder — 마이그레이션 완료 후 파일 자체 삭제 권장.
 */
class RedisConfig_PlaceholderRemoved {
}
