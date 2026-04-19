package com.msa.product.local.product.repository;

import com.msa.product.local.classification.entity.Classification;
import com.msa.product.local.material.entity.Material;
import com.msa.product.local.product.entity.Product;
import com.msa.product.local.set.entity.SetType;
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
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Product savedProduct;
    private SetType setType;
    private Material material;
    private Classification classification;

    @BeforeEach
    void setUp() {
        setType = SetType.builder()
                .setTypeName("목걸이")
                .setTypeNote("테스트")
                .build();
        entityManager.persist(setType);

        material = Material.builder()
                .materialName("14K")
                .materialGoldPurityPercent(new BigDecimal("58.5"))
                .build();
        entityManager.persist(material);

        classification = Classification.builder()
                .classificationName("귀금속")
                .classificationNote("테스트")
                .build();
        entityManager.persist(classification);

        Product product = Product.builder()
                .factoryId(1L)
                .factoryName("테스트공장")
                .productFactoryName("공장상품명")
                .productName("테스트상품")
                .standardWeight(new BigDecimal("10.5"))
                .productNote("테스트 메모")
                .build();

        product.setSetType(setType);
        product.setMaterial(material);
        product.setClassification(classification);

        savedProduct = entityManager.persistAndFlush(product);
        entityManager.clear();
    }

    @Test
    @DisplayName("상품명 존재 여부 확인 - 존재함")
    void existsByProductName_exists() {
        // given
        String productName = "테스트상품";

        // when
        boolean exists = productRepository.existsByProductName(productName);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("상품명 존재 여부 확인 - 존재하지 않음")
    void existsByProductName_notExists() {
        // given
        String productName = "없는상품";

        // when
        boolean exists = productRepository.existsByProductName(productName);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("상품명 중복 확인 - 다른 상품ID")
    void existsByProductNameAndProductIdNot_different() {
        // given
        String productName = "테스트상품";
        Long differentId = 999L;

        // when
        boolean exists = productRepository.existsByProductNameAndProductIdNot(productName, differentId);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("상품명 중복 확인 - 동일 상품ID")
    void existsByProductNameAndProductIdNot_same() {
        // given
        String productName = "테스트상품";
        Long sameId = savedProduct.getProductId();

        // when
        boolean exists = productRepository.existsByProductNameAndProductIdNot(productName, sameId);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("모든 옵션과 함께 상품 조회")
    void findWithAllOptionsById_success() {
        // given
        Long productId = savedProduct.getProductId();

        // when
        Optional<Product> result = productRepository.findWithAllOptionsById(productId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getProductName()).isEqualTo("테스트상품");
        assertThat(result.get().getSetType().getSetTypeName()).isEqualTo("목걸이");
        assertThat(result.get().getMaterial().getMaterialName()).isEqualTo("14K");
        assertThat(result.get().getClassification().getClassificationName()).isEqualTo("귀금속");
    }

    @Test
    @DisplayName("상품명으로 상품 조회")
    void findByProductName_success() {
        // given
        String productName = "테스트상품";

        // when
        Optional<Product> result = productRepository.findByProductName(productName);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getFactoryName()).isEqualTo("테스트공장");
    }

    @Test
    @DisplayName("존재하지 않는 상품명으로 조회")
    void findByProductName_notFound() {
        // given
        String productName = "없는상품";

        // when
        Optional<Product> result = productRepository.findByProductName(productName);

        // then
        assertThat(result).isEmpty();
    }
}
