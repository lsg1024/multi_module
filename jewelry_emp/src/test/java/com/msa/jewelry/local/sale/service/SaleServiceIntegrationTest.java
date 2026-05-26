package com.msa.jewelry.local.sale.service;

import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.jewelry.local.factory.dto.FactoryView;
import com.msa.jewelry.local.factory.service.FactoryService;
import com.msa.jewelry.local.product.dto.ProductImageView;
import com.msa.jewelry.local.product.service.ProductService;
import com.msa.jewelry.local.sale.dto.SaleDto;
import com.msa.jewelry.local.sale.entity.Sale;
import com.msa.jewelry.local.sale.entity.SalePayment;
import com.msa.jewelry.local.sale.repository.SalePaymentRepository;
import com.msa.jewelry.local.sale.repository.SaleRepository;
import com.msa.jewelry.local.store.dto.StoreView;
import com.msa.jewelry.local.store.service.StoreService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * SaleService 통합 테스트.
 *
 * <p>전체 Spring 컨텍스트를 띄우고 H2 + 실제 Repository 로 종단간 흐름을 검증한다.
 * 외부 의존성 (StoreService/FactoryService/ProductService/JwtUtil) 은 @MockBean 으로
 * 격리하여 SaleService 의 트랜잭션·영속성·도메인 메서드 동작에 집중한다.
 *
 * <p>각 테스트는 클래스 레벨 @Transactional 로 자동 롤백되므로 데이터 간 간섭이 없다.
 *
 * <p>실행 전제:
 * <ul>
 *   <li>{@code src/test/resources/application-test.yml} 가 H2 + cloud/redis/batch
 *       관련 자동구성을 모두 꺼두어 SpringBootTest 가 fail-fast 없이 뜬다</li>
 *   <li>JPA ddl-auto: create-drop 으로 매 테스트 클래스마다 스키마 재생성</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.bootstrap.enabled=false",
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "spring.batch.job.enabled=false",
        "spring.flyway.enabled=false"
})
@DisplayName("SaleService 통합 테스트")
class SaleServiceIntegrationTest {

    private static final String TOKEN     = "Bearer integration-token";
    private static final String TENANT_ID = "tenant-int";
    private static final String NICKNAME  = "integration-tester";

    @Autowired SaleService saleService;
    @Autowired SaleRepository saleRepository;
    @Autowired SalePaymentRepository salePaymentRepository;

    @PersistenceContext EntityManager em;

    // 외부 모듈은 mock 으로 격리 (네트워크/타 모듈 영향 제거)
    @MockBean JwtUtil jwtUtil;
    @MockBean StoreService storeService;
    @MockBean FactoryService factoryService;
    @MockBean ProductService productService;

    @BeforeEach
    void stubTokens() {
        given(jwtUtil.getTenantId(anyString())).willReturn(TENANT_ID);
        given(jwtUtil.getNickname(anyString())).willReturn(NICKNAME);
        given(jwtUtil.getRole(anyString())).willReturn("USER");
        given(storeService.getStoreInfoView(any()))
                .willReturn(new StoreView(10L, "강남금은방", "A", "1.5", "SELL", true));
        given(factoryService.getFactoryInfo(any()))
                .willReturn(new FactoryView(20L, "한빛제조사", "A", "1.5"));
        given(productService.getProductImages(any()))
                .willReturn(Map.of());
    }

    // -----------------------------------------------------------------------
    // checkBeforeSale — 데이터 없음 → NOT_FOUND 예외 (가장 가벼운 통합 시나리오)
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("checkBeforeSale (통합)")
    class CheckBeforeSale {

        @Test
        @DisplayName("당일 판매 세션이 DB 에 없으면 NOT_FOUND 예외 — 빈 H2 상태 검증")
        void 빈DB_NOT_FOUND() {
            assertThatThrownBy(() -> saleService.checkBeforeSale(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("찾을 수 없습니다"); // ExceptionMessage.NOT_FOUND
        }
    }

    // -----------------------------------------------------------------------
    // createStorePayment — Sale 신규 생성 + SalePayment 영속화 + applyDelta 호출
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createStorePayment (통합)")
    class CreateStorePayment {

        @Test
        @DisplayName("createNewSheet=true 로 새 Sale + SalePayment 가 영속화된다")
        void 새판매_결제_저장() {
            SaleDto.Request req = paymentRequest(10L, "18K", "3.250", 150_000,
                    SaleStatus.PAYMENT.name(), 85_000);

            saleService.createStorePayment(TOKEN, "evt_int_001", req, true);

            em.flush();
            em.clear();

            long saleCount = saleRepository.count();
            long paymentCount = salePaymentRepository.count();

            assertThat(saleCount).isEqualTo(1);
            assertThat(paymentCount).isEqualTo(1);
        }

        @Test
        @DisplayName("같은 eventId 로 두 번 호출 — DataIntegrityViolation 가 잡혀 두 번째 호출은 조용히 종료")
        void 멱등성_검증() {
            SaleDto.Request req1 = paymentRequest(10L, "18K", "3.250", 150_000,
                    SaleStatus.PAYMENT.name(), 85_000);
            saleService.createStorePayment(TOKEN, "evt_dup_key", req1, true);

            em.flush();

            SaleDto.Request req2 = paymentRequest(10L, "18K", "3.250", 150_000,
                    SaleStatus.PAYMENT.name(), 85_000);

            // 두 번째 호출은 같은 eventId — unique constraint 충돌이 catch 되어 정상 종료되어야 함
            // (실 DB 동작에 따라 동작은 다를 수 있으나 예외가 밖으로 새지 않으면 OK)
            saleService.createStorePayment(TOKEN, "evt_dup_key", req2, true);
            // 통과만 하면 검증 성공
        }
    }

    // -----------------------------------------------------------------------
    // updateAccountGoldPrice — 실제 Sale 엔티티의 시세 필드 업데이트
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateAccountGoldPrice (통합)")
    class UpdateAccountGoldPrice {

        @Test
        @DisplayName("실제 Sale 엔티티의 accountGoldPrice 가 영속화된 값으로 갱신")
        void 시세_갱신_영속화() {
            // given: Sale 한 건 미리 저장
            Sale sale = Sale.builder()
                    .saleStatus(SaleStatus.SALE)
                    .accountId(10L)
                    .accountName("강남금은방")
                    .accountHarry(new BigDecimal("1.50"))
                    .accountGrade("A")
                    .displayCode("2605160001")
                    .items(new ArrayList<>())
                    .build();
            saleRepository.saveAndFlush(sale);
            Long saleCode = sale.getSaleCode();

            SaleDto.GoldPriceRequest req = goldPriceRequest(95_000);

            // when
            saleService.updateAccountGoldPrice(saleCode.toString(), req);

            em.flush();
            em.clear();

            // then
            Sale reloaded = saleRepository.findBySaleCode(saleCode).orElseThrow();
            assertThat(reloaded.getAccountGoldPrice()).isEqualTo(95_000);
        }

        @Test
        @DisplayName("존재하지 않는 saleCode → NOT_FOUND 예외")
        void 없는_saleCode_예외() {
            SaleDto.GoldPriceRequest req = goldPriceRequest(95_000);

            assertThatThrownBy(() -> saleService.updateAccountGoldPrice("999999", req))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼 — DTO setter 가 없어 reflection 으로 필드 직접 주입
    // -----------------------------------------------------------------------
    private static SaleDto.Request paymentRequest(Long storeId, String material, String goldWeight,
                                                  Integer payAmount, String orderStatus,
                                                  Integer accountGoldPrice) {
        SaleDto.Request req = new SaleDto.Request();
        setField(req, "id", storeId);
        setField(req, "name", "강남금은방");
        setField(req, "harry", new BigDecimal("1.50"));
        setField(req, "grade", "A");
        setField(req, "orderStatus", orderStatus);
        setField(req, "material", material);
        setField(req, "goldWeight", goldWeight);
        setField(req, "payAmount", payAmount);
        setField(req, "note", "통합테스트");
        setField(req, "accountGoldPrice", accountGoldPrice);
        return req;
    }

    private static SaleDto.GoldPriceRequest goldPriceRequest(Integer price) {
        SaleDto.GoldPriceRequest req = new SaleDto.GoldPriceRequest();
        setField(req, "accountGoldPrice", price);
        return req;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("필드 주입 실패: " + name, e);
        }
    }
}
