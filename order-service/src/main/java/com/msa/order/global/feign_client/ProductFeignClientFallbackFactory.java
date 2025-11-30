package com.msa.order.global.feign_client;

import com.msa.common.global.api.ApiResponse;
import com.msa.order.global.feign_client.dto.AssistantStoneDto;
import com.msa.order.global.feign_client.dto.ProductDetailDto;
import com.msa.order.global.feign_client.dto.ProductImageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ProductFeignClientFallbackFactory implements FallbackFactory<ProductFeignClient> {

    @Override
    public ProductFeignClient create(Throwable cause) {
        return new ProductFeignClient() {

            @Override
            public ResponseEntity<ApiResponse<String>> getClassificationInfo(Map<String, Object> headers, Long classificationId) {
                log.error("Product Feign Fallback [getClassificationInfo] classificationId: {}, Error: {}", classificationId, cause.getMessage());
                return serviceUnavailable("분류 정보를 가져올 수 없습니다.");
            }

            @Override
            public ResponseEntity<ApiResponse<AssistantStoneDto.Response>> getAssistantStoneInfo(Map<String, Object> headers, Long assistantStoneId) {
                log.error("Product Feign Fallback [getAssistantStoneInfo] assistantStoneId: {}, Error: {}", assistantStoneId, cause.getMessage());
                return serviceUnavailable("보조석 정보를 가져올 수 없습니다.");
            }

            @Override
            public ResponseEntity<ApiResponse<String>> getColorInfo(Map<String, Object> headers, Long colorId) {
                log.error("Product Feign Fallback [getColorInfo] colorId: {}, Error: {}", colorId, cause.getMessage());
                return serviceUnavailable("색상 정보를 가져올 수 없습니다.");
            }

            @Override
            public ResponseEntity<ApiResponse<String>> getMaterialInfo(Map<String, Object> headers, Long materialId) {
                log.error("Product Feign Fallback [getMaterialInfo] materialId: {}, Error: {}", materialId, cause.getMessage());
                return serviceUnavailable("재질 정보를 가져올 수 없습니다.");
            }

            @Override
            public ResponseEntity<ApiResponse<ProductDetailDto>> getProductInfo(Map<String, Object> headers, Long productId, String grade) {
                log.error("Product Feign Fallback [getProductInfo] productId: {}, grade: {}, Error: {}", productId, grade, cause.getMessage());
                return serviceUnavailable("제품 상세 정보를 가져올 수 없습니다.");
            }

            @Override
            public ResponseEntity<ApiResponse<Map<Long, ProductImageDto>>> getProductImages(Map<String, Object> headers, List<Long> productIds) {
                log.error("Product Feign Fallback [getProductImages] productIds: {}, Error: {}", productIds, cause.getMessage());
                return serviceUnavailable("제품 이미지를 가져올 수 없습니다.");
            }

            @Override
            public ResponseEntity<ApiResponse<String>> getSetTypeName(Map<String, Object> headers, Long setTypeId) {
                log.error("Product Feign Fallback [getSetTypeName] setTypeId: {}, Error: {}", setTypeId, cause.getMessage());
                return serviceUnavailable("세트 정보를 가져올 수 없습니다.");
            }

            @Override
            public ResponseEntity<ApiResponse<Boolean>> getExistStoneId(Map<String, Object> headers, Long stoneId) {
                log.error("Product Feign Fallback [getExistStoneId] stoneId: {}, Error: {}", stoneId, cause.getMessage());
                // Boolean 타입이라도 null이나 false를 리턴하기보다 503 에러를 리턴하여 호출자가 흐름을 제어하게 하는 것이 안전합니다.
                return serviceUnavailable("스톤 존재 여부를 확인할 수 없습니다.");
            }

            // 공통 에러 응답 생성 헬퍼 메서드
            private <T> ResponseEntity<ApiResponse<T>> serviceUnavailable(String message) {
                return ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ApiResponse.error(message));
            }
        };
    }
}