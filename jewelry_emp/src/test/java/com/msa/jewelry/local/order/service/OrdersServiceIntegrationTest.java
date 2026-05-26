package com.msa.jewelry.local.order.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.jewelry.local.factory.service.FactoryService;
import com.msa.jewelry.local.order.repository.OrdersRepository;
import com.msa.jewelry.local.product.service.ProductService;
import com.msa.jewelry.local.store.service.StoreService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * OrdersService 통합 테스트 — 핵심 시나리오 슬라이스.
 *
 * <p>외부 모듈 의존성(StoreService/FactoryService/ProductService) 은 @MockBean,
 * Repository 와 트랜잭션 경계는 실제 H2 로 검증.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.bootstrap.enabled=false",
        "eureka.client.enabled=false",
        "spring.batch.job.enabled=false",
        "spring.flyway.enabled=false"
})
@DisplayName("OrdersService 통합 테스트")
class OrdersServiceIntegrationTest {

    @Autowired OrdersService ordersService;
    @Autowired OrdersRepository ordersRepository;

    @PersistenceContext EntityManager em;

    @MockBean JwtUtil jwtUtil;
    @MockBean StoreService storeService;
    @MockBean FactoryService factoryService;
    @MockBean ProductService productService;

    @BeforeEach
    void stubTokens() {
        given(jwtUtil.getTenantId(anyString())).willReturn("tenant-int");
        given(jwtUtil.getNickname(anyString())).willReturn("integration-tester");
        given(jwtUtil.getRole(anyString())).willReturn("USER");
    }

    @Test
    @DisplayName("빈 DB 에서 주문 목록 조회 — 결과 비어있어도 컨텍스트 로드 성공")
    void 빈DB_컨텍스트_로드() {
        assertThat(ordersRepository.count()).isZero();
    }
}
