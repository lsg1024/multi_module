package com.msa.jewelry.local.product.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.jewelry.local.factory.service.FactoryService;
import com.msa.jewelry.local.product.repository.ProductRepository;
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
@DisplayName("ProductService 통합 테스트")
class ProductServiceImplIntegrationTest {

    @Autowired ProductService productService;
    @Autowired ProductRepository productRepository;

    @PersistenceContext EntityManager em;

    @MockBean JwtUtil jwtUtil;
    @MockBean StoreService storeService;
    @MockBean FactoryService factoryService;

    @BeforeEach
    void stubTokens() {
        given(jwtUtil.getTenantId(anyString())).willReturn("tenant-int");
        given(jwtUtil.getNickname(anyString())).willReturn("integration-tester");
        given(jwtUtil.getRole(anyString())).willReturn("USER");
    }

    @Test
    @DisplayName("빈 DB 에서 상품 카운트 — 0 & 컨텍스트 로드 성공")
    void 빈DB_컨텍스트() {
        assertThat(productRepository.count()).isZero();
    }
}
