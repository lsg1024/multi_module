package com.msa.jewelry.local.imagesearch.service;

import com.msa.jewelry.local.classification.entity.Classification;
import com.msa.jewelry.local.imagesearch.service.ProductImageSearchService.ProductMetadataLookup;
import com.msa.jewelry.local.material.entity.Material;
import com.msa.jewelry.local.product.entity.Product;
import com.msa.jewelry.local.product.entity.ProductImage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductMetadataLookupAdapter 단위 테스트")
class ProductMetadataLookupAdapterTest {

    @Mock EntityManager em;

    @InjectMocks
    ProductMetadataLookupAdapter adapter;

    // ============================================================
    // findByIds
    // ============================================================
    @Nested
    @DisplayName("findByIds")
    class FindByIds {

        @Test
        @DisplayName("null 입력 → 즉시 빈 Map 반환, EntityManager 무호출")
        void null_입력() {
            Map<Long, ProductMetadataLookup.ProductMeta> result = adapter.findByIds(null);

            assertThat(result).isEmpty();
            verifyNoInteractions(em);
        }

        @Test
        @DisplayName("빈 컬렉션 → 즉시 빈 Map 반환, EntityManager 무호출")
        void 빈_컬렉션() {
            Map<Long, ProductMetadataLookup.ProductMeta> result = adapter.findByIds(Collections.emptyList());

            assertThat(result).isEmpty();
            verifyNoInteractions(em);
        }

        @Test
        @DisplayName("정상 — material/classification 정상 매핑, 메인 이미지 path 결합")
        void 정상() {
            // 상품 1: material/classification 있음
            Product p1 = mock(Product.class);
            given(p1.getProductId()).willReturn(1001L);
            given(p1.getProductName()).willReturn("P1");

            Material material = mock(Material.class);
            given(material.getMaterialId()).willReturn(10L);
            given(material.getMaterialName()).willReturn("14K");
            given(p1.getMaterial()).willReturn(material);

            Classification cls = mock(Classification.class);
            given(cls.getClassificationName()).willReturn("반지");
            given(p1.getClassification()).willReturn(cls);

            // 상품 2: material/classification 모두 null
            Product p2 = mock(Product.class);
            given(p2.getProductId()).willReturn(1002L);
            given(p2.getProductName()).willReturn("P2");
            given(p2.getMaterial()).willReturn(null);
            given(p2.getClassification()).willReturn(null);

            // 메인 이미지 ProductImage (p1 에 1개만 존재)
            ProductImage mainImg = mock(ProductImage.class);
            given(mainImg.getProduct()).willReturn(p1);
            given(mainImg.getImagePath()).willReturn("main1.jpg");

            // TypedQuery 두 개 별도 stubbing
            @SuppressWarnings("unchecked")
            TypedQuery<Product> productQuery = mock(TypedQuery.class);
            given(em.createQuery(anyString(), eq(Product.class))).willReturn(productQuery);
            given(productQuery.setParameter(eq("ids"), any())).willReturn(productQuery);
            given(productQuery.getResultList()).willReturn(List.of(p1, p2));

            @SuppressWarnings("unchecked")
            TypedQuery<ProductImage> imageQuery = mock(TypedQuery.class);
            given(em.createQuery(anyString(), eq(ProductImage.class))).willReturn(imageQuery);
            given(imageQuery.setParameter(eq("ids"), any())).willReturn(imageQuery);
            given(imageQuery.getResultStream()).willReturn(List.of(mainImg).stream());

            Map<Long, ProductMetadataLookup.ProductMeta> result = adapter.findByIds(List.of(1001L, 1002L));

            assertThat(result).hasSize(2);

            ProductMetadataLookup.ProductMeta m1 = result.get(1001L);
            assertThat(m1.productName()).isEqualTo("P1");
            assertThat(m1.materialId()).isEqualTo(10L);
            assertThat(m1.materialName()).isEqualTo("14K");
            assertThat(m1.classification()).isEqualTo("반지");
            assertThat(m1.mainImagePath()).isEqualTo("main1.jpg");
            assertThat(m1.colorId()).isNull();
            assertThat(m1.colorName()).isNull();

            ProductMetadataLookup.ProductMeta m2 = result.get(1002L);
            assertThat(m2.productName()).isEqualTo("P2");
            assertThat(m2.materialId()).isNull();
            assertThat(m2.materialName()).isNull();
            assertThat(m2.classification()).isNull();
            // p2 는 메인 이미지가 없음 → null
            assertThat(m2.mainImagePath()).isNull();
        }

        @Test
        @DisplayName("메인 이미지 없음 — mainImagePath 가 null 인 ProductMeta 반환")
        void 메인이미지_없음() {
            Product p = mock(Product.class);
            given(p.getProductId()).willReturn(1L);
            given(p.getProductName()).willReturn("only");
            given(p.getMaterial()).willReturn(null);
            given(p.getClassification()).willReturn(null);

            @SuppressWarnings("unchecked")
            TypedQuery<Product> productQuery = mock(TypedQuery.class);
            given(em.createQuery(anyString(), eq(Product.class))).willReturn(productQuery);
            given(productQuery.setParameter(eq("ids"), any())).willReturn(productQuery);
            given(productQuery.getResultList()).willReturn(List.of(p));

            @SuppressWarnings("unchecked")
            TypedQuery<ProductImage> imageQuery = mock(TypedQuery.class);
            given(em.createQuery(anyString(), eq(ProductImage.class))).willReturn(imageQuery);
            given(imageQuery.setParameter(eq("ids"), any())).willReturn(imageQuery);
            given(imageQuery.getResultStream()).willReturn(java.util.stream.Stream.empty());

            Map<Long, ProductMetadataLookup.ProductMeta> result = adapter.findByIds(List.of(1L));

            assertThat(result).hasSize(1);
            assertThat(result.get(1L).mainImagePath()).isNull();
        }

        @Test
        @DisplayName("Product 쿼리 결과 빈 리스트 — 빈 Map 반환")
        void Product_쿼리_빈결과() {
            @SuppressWarnings("unchecked")
            TypedQuery<Product> productQuery = mock(TypedQuery.class);
            given(em.createQuery(anyString(), eq(Product.class))).willReturn(productQuery);
            given(productQuery.setParameter(eq("ids"), any())).willReturn(productQuery);
            given(productQuery.getResultList()).willReturn(Collections.emptyList());

            @SuppressWarnings("unchecked")
            TypedQuery<ProductImage> imageQuery = mock(TypedQuery.class);
            given(em.createQuery(anyString(), eq(ProductImage.class))).willReturn(imageQuery);
            given(imageQuery.setParameter(eq("ids"), any())).willReturn(imageQuery);
            given(imageQuery.getResultStream()).willReturn(java.util.stream.Stream.empty());

            Map<Long, ProductMetadataLookup.ProductMeta> result = adapter.findByIds(List.of(99L));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("EntityManager 가 PersistenceException 던지면 그대로 전파")
        void EM_예외() {
            willThrow(new PersistenceException("DB down"))
                    .given(em).createQuery(anyString(), eq(Product.class));

            assertThatThrownBy(() -> adapter.findByIds(List.of(1L)))
                    .isInstanceOf(PersistenceException.class);
        }
    }

    // ============================================================
    // findImagePaths
    // ============================================================
    @Nested
    @DisplayName("findImagePaths")
    class FindImagePaths {

        @Test
        @DisplayName("null 입력 → 빈 Map, EntityManager 무호출")
        void null_입력() {
            assertThat(adapter.findImagePaths(null)).isEmpty();
            verifyNoInteractions(em);
        }

        @Test
        @DisplayName("빈 컬렉션 → 빈 Map, EntityManager 무호출")
        void 빈_컬렉션() {
            assertThat(adapter.findImagePaths(Collections.emptyList())).isEmpty();
            verifyNoInteractions(em);
        }

        @Test
        @DisplayName("정상 — imageId 단위로 path 매핑")
        void 정상() {
            ProductImage i1 = mock(ProductImage.class);
            given(i1.getImageId()).willReturn(5001L);
            given(i1.getImagePath()).willReturn("a.jpg");
            ProductImage i2 = mock(ProductImage.class);
            given(i2.getImageId()).willReturn(5002L);
            given(i2.getImagePath()).willReturn("b.jpg");

            @SuppressWarnings("unchecked")
            TypedQuery<ProductImage> query = mock(TypedQuery.class);
            given(em.createQuery(anyString(), eq(ProductImage.class))).willReturn(query);
            given(query.setParameter(eq("ids"), any())).willReturn(query);
            given(query.getResultStream()).willReturn(List.of(i1, i2).stream());

            Map<Long, String> result = adapter.findImagePaths(List.of(5001L, 5002L));

            assertThat(result)
                    .containsEntry(5001L, "a.jpg")
                    .containsEntry(5002L, "b.jpg")
                    .hasSize(2);
            verify(query).setParameter(eq("ids"), any());
        }

        @Test
        @DisplayName("쿼리 결과 빈 stream → 빈 Map")
        void 빈_결과() {
            @SuppressWarnings("unchecked")
            TypedQuery<ProductImage> query = mock(TypedQuery.class);
            given(em.createQuery(anyString(), eq(ProductImage.class))).willReturn(query);
            given(query.setParameter(eq("ids"), any())).willReturn(query);
            given(query.getResultStream()).willReturn(java.util.stream.Stream.empty());

            assertThat(adapter.findImagePaths(List.of(1L, 2L))).isEmpty();
        }

        @Test
        @DisplayName("동일 imageId 중복 시 — merge function (a, b) -> a 로 첫 값 유지")
        void 중복_imageId_방어() {
            ProductImage i1 = mock(ProductImage.class);
            given(i1.getImageId()).willReturn(5001L);
            given(i1.getImagePath()).willReturn("first.jpg");
            ProductImage i2 = mock(ProductImage.class);
            given(i2.getImageId()).willReturn(5001L);
            given(i2.getImagePath()).willReturn("second.jpg");

            @SuppressWarnings("unchecked")
            TypedQuery<ProductImage> query = mock(TypedQuery.class);
            given(em.createQuery(anyString(), eq(ProductImage.class))).willReturn(query);
            given(query.setParameter(eq("ids"), any())).willReturn(query);
            given(query.getResultStream()).willReturn(List.of(i1, i2).stream());

            Map<Long, String> result = adapter.findImagePaths(List.of(5001L));

            assertThat(result).containsEntry(5001L, "first.jpg").hasSize(1);
        }

        @Test
        @DisplayName("쿼리 실행 실패 — PersistenceException 전파")
        void 쿼리_실패() {
            @SuppressWarnings("unchecked")
            TypedQuery<ProductImage> query = mock(TypedQuery.class);
            given(em.createQuery(anyString(), eq(ProductImage.class))).willReturn(query);
            given(query.setParameter(eq("ids"), any())).willReturn(query);
            given(query.getResultStream()).willThrow(new PersistenceException("query fail"));

            assertThatThrownBy(() -> adapter.findImagePaths(List.of(1L)))
                    .isInstanceOf(PersistenceException.class);
        }
    }
}
