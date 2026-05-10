package com.msa.jewelry.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA / QueryDSL 통합 설정.
 *
 * <p>모놀리스로 통합하면서 4개 서비스에 흩어져 있던 JPA 설정을 단일화.
 * 모든 모듈의 엔티티가 단일 EntityManager 를 공유하므로 단일 트랜잭션
 * 안에서 여러 모듈의 데이터를 일관성 있게 변경할 수 있다.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.msa.jewelry")
public class JpaConfig {

    @PersistenceContext
    private EntityManager em;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(em);
    }
}
