package com.msa.jewelry.local.product.service;

import com.msa.common.global.tenant.TenantContext;
import com.msa.jewelry.global.exception.ExceptionMessage;
import com.msa.jewelry.local.imagesearch.event.ProductImageDeletedEvent;
import com.msa.jewelry.local.imagesearch.event.ProductImageUploadedEvent;
import com.msa.jewelry.local.product.dto.ProductImageDto;
import com.msa.jewelry.local.product.entity.Product;
import com.msa.jewelry.local.product.entity.ProductImage;
import com.msa.jewelry.local.product.repository.ProductRepository;
import com.msa.jewelry.local.product.repository.image.ProductImageRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductImageService 단위 테스트")
class ProductImageServiceTest {

    private static final String TENANT     = "tenant-001";
    private static final Long   PRODUCT_ID = 501L;
    private static final Long   IMAGE_ID   = 9001L;

    @Mock ProductRepository productRepository;
    @Mock ProductImageRepository productImageRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks
    ProductImageService productImageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // @Value 필드는 @InjectMocks 가 채워주지 않음 — 임시 경로로 직접 주입.
        ReflectionTestUtils.setField(productImageService, "baseUploadPath", tempDir.toString());
        TenantContext.setTenant(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // -----------------------------------------------------------------------
    // bulkUploadByFileName
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("bulkUploadByFileName")
    class BulkUploadByFileName {

        @Test
        @DisplayName("빈 리스트 — 어떤 호출도 발생하지 않음")
        void 빈_리스트() {
            productImageService.bulkUploadByFileName(Collections.emptyList());

            verifyNoInteractions(productRepository, productImageRepository, eventPublisher);
        }

        @Test
        @DisplayName("originalFilename 이 null 인 파일은 skip")
        void 파일명_없으면_skip() {
            MultipartFile file = mock(MultipartFile.class);
            given(file.getOriginalFilename()).willReturn(null);

            productImageService.bulkUploadByFileName(List.of(file));

            verifyNoInteractions(productRepository, productImageRepository, eventPublisher);
        }

        @Test
        @DisplayName("상품 미존재 시 skip 로그만 — 저장 없음")
        void 상품_없음_skip() {
            MultipartFile file = new MockMultipartFile(
                    "file", "미존재상품.jpg", "image/jpeg", smallPngBytes());
            given(productRepository.findByProductNameIgnoreCase("미존재상품"))
                    .willReturn(Optional.empty());

            productImageService.bulkUploadByFileName(List.of(file));

            verify(productImageRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("파일 저장 중 IOException 발생해도 다음 파일은 계속 처리 (catch + log)")
        void IOException_무시_다음_계속() {
            // 첫 파일 — bytes 0짜리 빈 파일로 Thumbnails 가 IOException
            MultipartFile badFile = new MockMultipartFile(
                    "f", "반지.jpg", "image/jpeg", new byte[0]);
            Product product1 = stubProduct();

            given(productRepository.findByProductNameIgnoreCase("반지"))
                    .willReturn(Optional.of(product1));

            // 예외가 밖으로 새어 나가지 않아야 함
            productImageService.bulkUploadByFileName(List.of(badFile));

            // 저장은 실패했지만 메서드는 정상 종료
            verify(productImageRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // uploadProductImage
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("uploadProductImage")
    class UploadProductImage {

        @Test
        @DisplayName("Product 없음 → IllegalArgumentException(NOT_FOUND)")
        void product_없음() {
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

            MultipartFile file = mock(MultipartFile.class);
            assertThatThrownBy(() -> productImageService.uploadProductImage(PRODUCT_ID, file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("기존 이미지 있으면 기존 이미지 삭제 후 신규 이미지 저장 + DeletedEvent 발행")
        void 기존_이미지_삭제_후_신규() {
            Product product = stubProduct();
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

            ProductImage old = mock(ProductImage.class);
            given(old.getImageId()).willReturn(IMAGE_ID);
            given(old.getImagePath()).willReturn("/products/501/old.jpg");
            given(productImageRepository.findByProduct(product)).willReturn(new ArrayList<>(List.of(old)));

            MultipartFile newFile = new MockMultipartFile(
                    "img", "new.jpg", "image/jpeg", smallPngBytes());

            productImageService.uploadProductImage(PRODUCT_ID, newFile);

            // 기존 이미지 삭제
            verify(productImageRepository).delete(old);
            // DeletedEvent 발행
            verify(eventPublisher).publishEvent(any(ProductImageDeletedEvent.class));
            // 신규 이미지 저장
            verify(productImageRepository).save(any(ProductImage.class));
            // UploadedEvent 발행
            verify(eventPublisher).publishEvent(any(ProductImageUploadedEvent.class));
        }

        @Test
        @DisplayName("기존 이미지 없으면 신규 이미지만 저장 — DeletedEvent 발행 안 됨")
        void 기존_이미지_없음() {
            Product product = stubProduct();
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
            given(productImageRepository.findByProduct(product)).willReturn(Collections.emptyList());

            MultipartFile newFile = new MockMultipartFile(
                    "img", "new.jpg", "image/jpeg", smallPngBytes());

            productImageService.uploadProductImage(PRODUCT_ID, newFile);

            verify(productImageRepository).save(any(ProductImage.class));
            verify(eventPublisher, never()).publishEvent(any(ProductImageDeletedEvent.class));
            verify(eventPublisher).publishEvent(any(ProductImageUploadedEvent.class));
        }

        @Test
        @DisplayName("MultipartFile 의 getInputStream 이 IOException → RuntimeException 으로 래핑되어 던져짐")
        void IOException_래핑() {
            Product product = stubProduct();
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
            given(productImageRepository.findByProduct(product)).willReturn(Collections.emptyList());

            MultipartFile bad = mock(MultipartFile.class);
            given(bad.getOriginalFilename()).willReturn("bad.jpg");
            try {
                given(bad.getInputStream()).willThrow(new IOException("io fail"));
            } catch (IOException ignored) {
                // mock setup, not thrown here
            }

            assertThatThrownBy(() -> productImageService.uploadProductImage(PRODUCT_ID, bad))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("이미지 저장 실패");
        }
    }

    // -----------------------------------------------------------------------
    // addProductImages
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("addProductImages")
    class AddProductImages {

        @Test
        @DisplayName("Product 없음 → NOT_FOUND")
        void product_없음() {
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productImageService.addProductImages(PRODUCT_ID,
                    List.of(mock(MultipartFile.class))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("빈 파일/ null 파일은 skip")
        void 빈파일_skip() {
            Product product = stubProduct();
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

            MultipartFile empty = mock(MultipartFile.class);
            given(empty.isEmpty()).willReturn(true);

            // null 1건 + empty 1건 — 둘 다 저장 안 됨
            List<MultipartFile> images = new ArrayList<>();
            images.add(null);
            images.add(empty);

            productImageService.addProductImages(PRODUCT_ID, images);

            verify(productImageRepository, never()).save(any());
        }

        @Test
        @DisplayName("정상 — 여러 파일이 모두 저장되고 각 파일마다 UploadedEvent 발행")
        void 여러파일_정상() {
            Product product = stubProduct();
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

            MultipartFile f1 = new MockMultipartFile("f", "a.jpg", "image/jpeg", smallPngBytes());
            MultipartFile f2 = new MockMultipartFile("f", "b.png", "image/png", smallPngBytes());

            productImageService.addProductImages(PRODUCT_ID, List.of(f1, f2));

            verify(productImageRepository, times(2)).save(any(ProductImage.class));
            verify(eventPublisher, times(2)).publishEvent(any(ProductImageUploadedEvent.class));
        }
    }

    // -----------------------------------------------------------------------
    // getProductImageList
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getProductImageList")
    class GetProductImageList {

        @Test
        @DisplayName("Product 없음 → NOT_FOUND")
        void product_없음() {
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productImageService.getProductImageList(PRODUCT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("이미지 없음 — 빈 리스트 반환")
        void 빈_결과() {
            Product product = stubProduct();
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
            given(productImageRepository.findByProduct(product)).willReturn(Collections.emptyList());

            assertThat(productImageService.getProductImageList(PRODUCT_ID)).isEmpty();
        }

        @Test
        @DisplayName("정상 — Response DTO 매핑")
        void 정상_매핑() {
            Product product = stubProduct();
            given(productRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

            ProductImage img = mock(ProductImage.class);
            given(img.getImageId()).willReturn(IMAGE_ID);
            given(img.getImagePath()).willReturn("/products/501/a.jpg");
            given(img.getImageName()).willReturn("a.jpg");
            given(img.getImageOriginName()).willReturn("내사진.jpg");
            given(img.getImageMain()).willReturn(true);
            given(productImageRepository.findByProduct(product)).willReturn(List.of(img));

            List<ProductImageDto.Response> result = productImageService.getProductImageList(PRODUCT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getImageId()).isEqualTo(IMAGE_ID.toString());
            assertThat(result.get(0).getImagePath()).isEqualTo("/products/501/a.jpg");
            assertThat(result.get(0).getImageOriginName()).isEqualTo("내사진.jpg");
            assertThat(result.get(0).getImageMain()).isTrue();
        }
    }

    // -----------------------------------------------------------------------
    // deleteImage
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("deleteImage")
    class DeleteImage {

        @Test
        @DisplayName("이미지 없음 → IllegalArgumentException ('이미지를 찾을 수 없습니다.' 메시지)")
        void 없음() {
            given(productImageRepository.findById(IMAGE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productImageService.deleteImage(IMAGE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미지를 찾을 수 없습니다")
                    .hasMessageContaining(IMAGE_ID.toString());
        }

        @Test
        @DisplayName("정상 — physicalFile 삭제 + repo.delete + DeletedEvent 발행")
        void 정상() {
            ProductImage image = mock(ProductImage.class);
            given(image.getImagePath()).willReturn("/products/501/a.jpg");
            given(productImageRepository.findById(IMAGE_ID)).willReturn(Optional.of(image));

            productImageService.deleteImage(IMAGE_ID);

            verify(productImageRepository).delete(image);
            verify(eventPublisher).publishEvent(any(ProductImageDeletedEvent.class));
        }
    }

    // -----------------------------------------------------------------------
    // getImagesByProductIds
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getImagesByProductIds")
    class GetImagesByProductIds {

        @Test
        @DisplayName("null 입력 — 빈 Map, repo 호출 없음")
        void null_입력() {
            Map<Long, ProductImageDto.ProductImageResponse> result =
                    productImageService.getImagesByProductIds(null);

            assertThat(result).isEmpty();
            verify(productImageRepository, never()).findMainImagesByProductIds(any());
        }

        @Test
        @DisplayName("빈 리스트 — 빈 Map, repo 호출 없음")
        void 빈_리스트() {
            assertThat(productImageService.getImagesByProductIds(Collections.emptyList())).isEmpty();
            verify(productImageRepository, never()).findMainImagesByProductIds(any());
        }

        @Test
        @DisplayName("정상 — repo 결과 그대로 위임")
        void 위임() {
            ProductImageDto.ProductImageResponse single = mock(ProductImageDto.ProductImageResponse.class);
            Map<Long, ProductImageDto.ProductImageResponse> expected = Map.of(PRODUCT_ID, single);

            given(productImageRepository.findMainImagesByProductIds(List.of(PRODUCT_ID)))
                    .willReturn(expected);

            Map<Long, ProductImageDto.ProductImageResponse> result =
                    productImageService.getImagesByProductIds(List.of(PRODUCT_ID));

            assertThat(result).containsEntry(PRODUCT_ID, single);
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------
    private static Product stubProduct() {
        Product product = mock(Product.class);
        given(product.getProductId()).willReturn(PRODUCT_ID);
        given(product.getProductImages()).willReturn(new ArrayList<>());
        return product;
    }
    private static byte[] smallPngBytes() {
        return new byte[]{
                (byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n',
                0, 0, 0, 13, 'I', 'H', 'D', 'R',
                0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0,
                0x1f, 0x15, (byte)0xc4, (byte)0x89,
                0, 0, 0, 13, 'I', 'D', 'A', 'T',
                0x78, (byte)0x9c, 0x62, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01,
                0x0d, 0x0a, 0x2d, (byte)0xb4,
                0, 0, 0, 0, 'I', 'E', 'N', 'D', (byte)0xae, 0x42, 0x60, (byte)0x82
        };
    }
}
