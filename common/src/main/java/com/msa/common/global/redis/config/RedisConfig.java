package com.msa.common.global.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

//    @Value("${redis.password}")
//    private String password;
//
//    private final RedisClusterProperties redisClusterProperties;
//
//    public RedisConfig(RedisClusterProperties redisClusterProperties) {
//        this.redisClusterProperties = redisClusterProperties;
//    }
//
//    @Bean
//    public RedissonClient redissonClient() {
//        Config config = new Config();
//        // Redis 클러스터 설정
//        String[] nodeAddresses = redisClusterProperties.getNodes().stream()
//                .map(node -> "redis://" + node.replace(",", ""))
//                .toArray(String[]::new);
//
//        config.useClusterServers()
//                .setPassword(password)
//                .addNodeAddress(nodeAddresses)
//                .setScanInterval(2000)  // 클러스터 스캔 주기 설정 (밀리초)
//                .setConnectTimeout(5000)  // 연결 시간 제한 (밀리초)
//                .setIdleConnectionTimeout(10000);  // 유휴 연결 시간 제한 (밀리초)
//
//        config.setLockWatchdogTimeout(30000L);
//
//        return Redisson.create(config);
//    }
//
//    @Bean
//    public RedisCacheManager redisCacheManager(RedisConnectionFactory cf) {
//        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
//                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
//                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
//                .entryTtl(Duration.ofMinutes(3L)); // 캐시 수명 설정
//
//        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
//        cacheConfigurations.put("sLC", redisCacheConfiguration.entryTtl(Duration.ofMinutes(3L))); // 3분
//        cacheConfigurations.put("lLC", redisCacheConfiguration.entryTtl(Duration.ofMinutes(10L))); // 10분
//        cacheConfigurations.put("vLLC", redisCacheConfiguration.entryTtl(Duration.ofDays(3L))); // 3일
//
//        return RedisCacheManager.RedisCacheManagerBuilder
//                .fromConnectionFactory(cf)
//                .cacheDefaults(redisCacheConfiguration)
//                .withInitialCacheConfigurations(cacheConfigurations)
//                .build();
//    }

//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedissonClient redissonClient) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//        template.setConnectionFactory(new RedissonConnectionFactory(redissonClient));
//        return template;
//    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());


        return redisTemplate;
    }
}
