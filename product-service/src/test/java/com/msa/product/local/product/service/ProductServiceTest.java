package com.msa.product.local.product.service;

import com.msa.product.local.product.dto.ProductDto;
import com.msa.product.local.product.dto.ProductImageDto;
import com.msa.product.local.product.dto.ProductStoneDto;
import com.msa.product.local.product.dto.ProductWorkGradePolicyGroupDto;
import com.msa.product.local.product.repository.CustomProductWorkGradePolicyGroupRepository;
import com.msa.product.local.product.repository.ProductImageRepository;
import com.msa.product.local.product.repository.ProductRepository;
import com.msa.product.local.product.repository.ProductStoneRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductStoneRepository productStoneRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private CustomProductWorkGradePolicyGroupRepository customProductWorkGradePolicyGroupRepository;

    @Nested
    @DisplayName("상품 조회")
    class GetProduct {

        @Test
        @DisplayName("성공")
        void getProduct_success() {
            // given
            Long productId = 1L;
            ProductDto.Detail mockDetail = ProductDto.Detail.builder()
                    .productId("1")
                    .factoryId(1L)
                    .factoryName("테스트공장")
                    .productName("테스트상품")
                    .build();

            List<ProductStoneDto.Response> stones = Collections.emptyList();
            List<ProductImageDto.Response> images = Collections.emptyList();
            List<ProductWorkGradePolicyGroupDto.Response> groups = Collections.emptyList();

            given(productRepository.findByProductId(productId)).willReturn(mockDetail);
            given(productStoneRepository.findProductStones(productId)).willReturn(stones);
            given(productImageRepository.findImagesByProductId(productId)).willReturn(images);
            given(customProductWorkGradePolicyGroupRepository.findByWorkGradePolicyGroupByProductIdOrderById(productId))
                    .willReturn(groups);

            // when
            ProductDto.Detail result = productService.getProduct(productId);

            // then
            assertThat(result.getProductName()).isEqualTo("테스트상품");
            assertThat(result.getFactoryName()).isEqualTo("테스트공장");
            verify(productRepository).findByProductId(productId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 상품")
        void getProduct_notFound() {
            // given
            Long productId = 999L;
            given(productRepository.findByProductId(productId)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> productService.getProduct(productId))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("상품명 중복 확인")
    class CheckDuplicateProductName {

        @Test
        @DisplayName("중복된 상품명 존재")
        void existsByProductName_true() {
            // given
            String productName = "테스트상품";
            given(productRepository.existsByProductName(productName)).willReturn(true);

            // when
            boolean result = productRepository.existsByProductName(productName);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("중복된 상품명 없음")
        void existsByProductName_false() {
            // given
            String productName = "새상품";
            given(productRepository.existsByProductName(productName)).willReturn(false);

            // when
            boolean result = productRepository.existsByProductName(productName);

            // then
            assertThat(result).isFalse();
        }
    }
}
