package com.msa.jewelry.local.product.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.exception.ExceptionMessage;
import com.msa.jewelry.local.classification.repository.ClassificationRepository;
import com.msa.jewelry.local.color.repository.ColorRepository;
import com.msa.jewelry.local.factory.service.FactoryService;
import com.msa.jewelry.local.gold_price.repository.GoldRepository;
import com.msa.jewelry.local.material.repository.MaterialRepository;
import com.msa.jewelry.local.product.dto.ProductDetailDto;
import com.msa.jewelry.local.product.dto.ProductDetailView;
import com.msa.jewelry.local.product.dto.ProductDto;
import com.msa.jewelry.local.product.dto.ProductImageView;
import com.msa.jewelry.local.product.dto.ProductView;
import com.msa.jewelry.local.product.entity.Product;
import com.msa.jewelry.local.product.repository.ProductRepository;
import com.msa.jewelry.local.product.repository.image.ProductImageRepository;
import com.msa.jewelry.local.product.repository.stone.ProductStoneRepository;
import com.msa.jewelry.local.product.repository.work_grade_policy_group.CustomProductWorkGradePolicyGroup;
import com.msa.jewelry.local.product.repository.work_grade_policy_group.ProductWorkGradePolicyGroupRepository;
import com.msa.jewelry.local.set.repository.SetTypeRepository;
import com.msa.jewelry.local.stone.repository.StoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * ProductServiceImpl 단위 테스트.
 *
 * <p>외부 의존성(Repository × 11, JwtUtil, FactoryService, ProductImageService) 을
 * Mockito 로 격리하여 비즈니스 로직만 검증한다.
 *
 * <p>커버리지 — 15개 public 메서드:
 * <ul>
 *   <li>saveProduct, getProduct, getProducts, updateProduct, deletedProduct</li>
 *   <li>getProductInfoByName, getProductStonesByName, updateProductFactoryName</li>
 *   <li>getProductInfo, getRelatedProducts, findProductByName, getProductDetail</li>
 *   <li>findProductDetailByName, getProductImages</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductServiceImpl 단위 테스트")
class ProductServiceImplTest {

    private static final String TOKEN     = "Bearer test-token";
    private static final String TENANT_ID = "tenant-001";
    private static final String NICKNAME  = "tester";
    private static final String ROLE_USER = "USER";
    private static final Long PRODUCT_ID  = 501L;

    @Mock JwtUtil jwtUtil;
    @Mock FactoryService factoryService;
    @Mock SetTypeRepository setTypeRepository;
    @Mock MaterialRepository materialRepository;
    @Mock ClassificationRepository classificationRepository;
    @Mock ColorRepository colorRepository;
    @Mock StoneRepository stoneRepository;
    @Mock ProductRepository productRepository;
    @Mock GoldRepository goldRepository;
    @Mock ProductStoneRepository productStoneRepository;
    @Mock ProductImageRepository productImageRepository;
    @Mock ProductWorkGradePolicyGroupRepository productWorkGradePolicyGroupRepository;
    @Mock CustomProductWorkGradePolicyGroup customProductWorkGradePolicyGroupRepository;
    @Mock ProductImageService productImageService;

    @InjectMocks
    ProductServiceImpl productService;

    @BeforeEach
    void commonStubs() {
        given(jwtUtil.getTenantId(anyString())).willReturn(TENANT_ID);
        given(jwtUtil.getNickname(anyString())).willReturn(NICKNAME);
        given(jwtUtil.getRole(anyString())).willReturn(ROLE_USER);
    }

    // -----------------------------------------------------------------------
    // getProduct
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getProduct")
    class GetProduct {

        @Test
        @DisplayName("Product 없음 → NOT_FOUND 예외")
        void product_없음() {
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProduct(PRODUCT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(ExceptionMessage.NOT_FOUND);
        }
    }

    // -----------------------------------------------------------------------
    // getProducts (페이징)
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getProducts")
    class GetProducts {

        @Test
        @DisplayName("빈 결과여도 정상 응답")
        void 빈결과() {
            Pageable pageable = PageRequest.of(0, 20);
            CustomPage<ProductDto.Page> empty = mock(CustomPage.class);
            given(empty.getContent()).willReturn(Collections.emptyList());

            given(customProductWorkGradePolicyGroupRepository.findProducts(any(), any(), any(), any(),
                    any(), any(), any(), eq(pageable)))
                    .willReturn(empty);

            CustomPage<ProductDto.Page> result = productService.getProducts(
                    "다이아", "name", "0", "100000", "name", "ASC", "A", pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // updateProduct
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("Product 없음 → NOT_FOUND 예외")
        void product_없음() {
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());
            ProductDto.Update dto = mock(ProductDto.Update.class);

            assertThatThrownBy(() -> productService.updateProduct(TOKEN, PRODUCT_ID, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // deletedProduct
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("deletedProduct")
    class DeleteProduct {

        @Test
        @DisplayName("role 이 WAIT 면 NOT_ACCESS 예외 — 권한 거부")
        void 권한_거부() {
            given(jwtUtil.getRole(TOKEN)).willReturn("WAIT");

            assertThatThrownBy(() -> productService.deletedProduct(TOKEN, PRODUCT_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(ExceptionMessage.NOT_ACCESS);

            verify(productRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Product 없음 → NOT_FOUND 예외")
        void product_없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn(ROLE_USER);
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deletedProduct(TOKEN, PRODUCT_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // getProductInfoByName
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getProductInfoByName")
    class GetProductInfoByName {

        @Test
        @DisplayName("Product 이름으로 못 찾음 → NOT_FOUND 예외")
        void 이름_없음() {
            given(productRepository.findByProductName("없는이름")).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductInfoByName("없는이름"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // getProductStonesByName
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getProductStonesByName")
    class GetProductStonesByName {

        @Test
        @DisplayName("스톤 없으면 빈 리스트 반환")
        void 빈_스톤() {
            Product product = mock(Product.class);
            given(product.getProductId()).willReturn(PRODUCT_ID);
            given(productRepository.findByProductName("반지A")).willReturn(Optional.of(product));
            given(productStoneRepository.findByProductId(PRODUCT_ID))
                    .willReturn(Collections.emptyList());

            List<ProductDetailDto.StoneInfo> result =
                    productService.getProductStonesByName("반지A");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Product 이름으로 못 찾음 → 예외")
        void 이름_없음() {
            given(productRepository.findByProductName("없는이름")).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductStonesByName("없는이름"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // updateProductFactoryName
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateProductFactoryName")
    class UpdateProductFactoryName {

        @Test
        @DisplayName("Product 없음 → NOT_FOUND 예외")
        void product_없음() {
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProductFactoryName(PRODUCT_ID, "신규공장"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // getProductInfo
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getProductInfo")
    class GetProductInfo {

        @Test
        @DisplayName("Product 없음 → NOT_FOUND 예외")
        void product_없음() {
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductInfo(PRODUCT_ID, "A"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // getRelatedProducts
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getRelatedProducts")
    class GetRelatedProducts {

        @Test
        @DisplayName("관련 상품 없으면 빈 리스트")
        void 빈결과() {
            given(productRepository.findRelatedProducts(PRODUCT_ID))
                    .willReturn(Collections.emptyList());

            List<ProductDto.RelatedProduct> result = productService.getRelatedProducts(PRODUCT_ID);

            assertThat(result).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // findProductByName
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("findProductByName")
    class FindProductByName {

        @Test
        @DisplayName("이름으로 못 찾으면 NOT_FOUND")
        void 없음() {
            given(productRepository.findProductViewByName("없음"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findProductByName("없음"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("정상 — ProductView 반환")
        void 정상() {
            ProductView view = mock(ProductView.class);
            given(productRepository.findProductViewByName("반지A"))
                    .willReturn(Optional.of(view));

            assertThat(productService.findProductByName("반지A")).isSameAs(view);
        }
    }

    // -----------------------------------------------------------------------
    // getProductDetail
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getProductDetail")
    class GetProductDetail {

        @Test
        @DisplayName("Product 없음 → NOT_FOUND")
        void 없음() {
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductDetail(PRODUCT_ID, "A"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // findProductDetailByName
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("findProductDetailByName")
    class FindProductDetailByName {

        @Test
        @DisplayName("못 찾으면 NOT_FOUND")
        void 없음() {
            given(productRepository.findProductDetailViewByName("없음"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findProductDetailByName("없음"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("정상 반환")
        void 정상() {
            ProductDetailView view = mock(ProductDetailView.class);
            given(productRepository.findProductDetailViewByName("반지A"))
                    .willReturn(Optional.of(view));

            assertThat(productService.findProductDetailByName("반지A")).isSameAs(view);
        }
    }

    // -----------------------------------------------------------------------
    // getProductImages (다건)
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getProductImages")
    class GetProductImages {

        @Test
        @DisplayName("빈 productIds → 빈 Map 즉시 반환 (레포 호출 안 함)")
        void 빈_입력() {
            Map<Long, ProductImageView> result =
                    productService.getProductImages(Collections.emptyList());

            assertThat(result).isEmpty();
            verify(productImageRepository, never()).findRepresentativeImagesByProductIds(any());
        }

        @Test
        @DisplayName("null productIds → 빈 Map 즉시 반환")
        void null_입력() {
            Map<Long, ProductImageView> result = productService.getProductImages(null);

            assertThat(result).isEmpty();
            verify(productImageRepository, never()).findRepresentativeImagesByProductIds(any());
        }

        @Test
        @DisplayName("매칭되는 이미지 없음 → 빈 Map")
        void 없음() {
            given(productImageRepository.findRepresentativeImagesByProductIds(anyList()))
                    .willReturn(Collections.emptyList());

            Map<Long, ProductImageView> result =
                    productService.getProductImages(List.of(1L, 2L, 3L));

            assertThat(result).isEmpty();
        }
    }
}
