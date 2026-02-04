package com.msa.account.local.store.repository;

import com.msa.account.global.domain.entity.CommonOption;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.entity.OptionLevel;
import com.msa.account.local.store.domain.entity.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class StoreRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StoreRepository storeRepository;

    private Store savedStore;

    @BeforeEach
    void setUp() {
        GoldHarry goldHarry = GoldHarry.builder()
                .goldHarryLoss(new BigDecimal("1.10"))
                .build();
        entityManager.persist(goldHarry);

        CommonOption commonOption = CommonOption.builder()
                .optionLevel(OptionLevel.ONE)
                .goldHarry(goldHarry)
                .goldHarryLoss("1.10")
                .build();
        entityManager.persist(commonOption);

        Store store = Store.builder()
                .storeName("테스트매장")
                .storeOwnerName("홍길동")
                .storePhoneNumber("010-1234-5678")
                .storeNote("테스트 메모")
                .commonOption(commonOption)
                .build();

        savedStore = entityManager.persistAndFlush(store);
        entityManager.clear();
    }

    @Test
    @DisplayName("판매처 정보 조회 성공")
    void findByStoreInfo_success() {
        // given
        Long storeId = savedStore.getStoreId();

        // when
        Optional<Store> result = storeRepository.findByStoreInfo(storeId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStoreName()).isEqualTo("테스트매장");
        assertThat(result.get().getStoreOwnerName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("판매처명 존재 여부 확인 - 존재함")
    void existsByStoreName_exists() {
        // given
        String storeName = "테스트매장";

        // when
        boolean exists = storeRepository.existsByStoreName(storeName);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("판매처명 존재 여부 확인 - 존재하지 않음")
    void existsByStoreName_notExists() {
        // given
        String storeName = "없는매장";

        // when
        boolean exists = storeRepository.existsByStoreName(storeName);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("모든 옵션과 함께 판매처 조회")
    void findWithAllOptionsById_success() {
        // given
        Long storeId = savedStore.getStoreId();

        // when
        Optional<Store> result = storeRepository.findWithAllOptionsById(storeId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStoreName()).isEqualTo("테스트매장");
        assertThat(result.get().getCommonOption()).isNotNull();
    }

    @Test
    @DisplayName("판매처 등급 조회")
    void findByCommonOptionOptionLevel_success() {
        // given
        Long storeId = savedStore.getStoreId();

        // when
        OptionLevel result = storeRepository.findByCommonOptionOptionLevel(storeId);

        // then
        assertThat(result).isEqualTo(OptionLevel.ONE);
    }

    @Test
    @DisplayName("비관적 락으로 판매처 조회")
    void findByIdWithLock_success() {
        // given
        Long storeId = savedStore.getStoreId();

        // when
        Optional<Store> result = storeRepository.findByIdWithLock(storeId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStoreName()).isEqualTo("테스트매장");
    }
}
