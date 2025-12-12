package com.msa.order.global.feign_client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.api.ApiResponse;
import com.msa.order.global.feign_client.dto.AssistantStoneDto;
import com.msa.order.global.feign_client.dto.ProductDetailDto;
import com.msa.order.global.feign_client.dto.ProductImageDto;
import feign.FeignException;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ProductFeignClient create(Throwable cause) {
        return new ProductFeignClient() {

            @Override
            public ResponseEntity<ApiResponse<String>> getClassificationInfo(Map<String, Object> headers, Long classificationId) {
                log.error("[Fallback] getClassificationInfo ID: {}, Error: {}", classificationId, cause.getMessage());
                return resolveError(cause, "분류 정보를 가져올 수 없습니다.");
            }

            @Override
            public ResponseEntity<ApiResponse<AssistantStoneDto.Response>> getAssistantStoneInfo(Map<String, Object> headers, Long assistantStoneId) {
                log.error("[Fallback] getAssistantStoneInfo ID: {}, Error: {}", assistantStoneId, cause.getMessage());
                return resolveError(cause, "보조석 정보를 가져올 수 없습니다.");
            }

            @Override
            public ResponseEntity<ApiResponse<String>> getColorInfo(Map<String, Object> headers, Long colorId) {
                log.error("[Fallback] getColorInfo ID: {}, Error: {}", colorId, cause.getMessage());
                return resolveError(cause, "색상 정보를 가져올 수 없습니다.");
            }

            @Override
            public ResponseEntity<ApiResponse<String>> getMaterialInfo(Map<String, Object> headers, Long materialId) {
                log.error("[Fallback] getMaterialInfo ID: {}, Error: {}", materialId, cause.getMessage());
                return resolveError(cause, "재질 정보를 가져올 수 없습니다.");
            }

            @Override
            public ResponseEntity<ApiResponse<ProductDetailDto>> getProductInfo(Map<String, Object> headers, Long productId, String grade) {
                log.error("[Fallback] getProductInfo ID: {}, Grade: {}, Error: {}", productId, grade, cause.getMessage());
                return resolveError(cause, "제품 상세 정보를 가져올 수 없습니다.");
            }

            @Override
            public ResponseEntity<ApiResponse<Map<Long, ProductImageDto>>> getProductImages(Map<String, Object> headers, List<Long> productIds) {
                log.error("[Fallback] getProductImages IDs: {}, Error: {}", productIds, cause.getMessage());
                return resolveError(cause, "제품 이미지를 가져올 수 없습니다.");
            }

            @Override
            public ResponseEntity<ApiResponse<String>> getSetTypeName(Map<String, Object> headers, Long setTypeId) {
                log.error("[Fallback] getSetTypeName ID: {}, Error: {}", setTypeId, cause.getMessage());
                return resolveError(cause, "세트 정보를 가져올 수 없습니다.");
            }

            @Override
            public ResponseEntity<ApiResponse<Boolean>> getExistStoneId(Map<String, Object> headers, Long stoneId) {
                log.error("[Fallback] getExistStoneId ID: {}, Error: {}", stoneId, cause.getMessage());
                return resolveError(cause, "스톤 존재 여부를 확인할 수 없습니다.");
            }
        };
    }

    private <T> ResponseEntity<ApiResponse<T>> resolveError(Throwable cause, String defaultMessage) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        String errorMessage = defaultMessage;

        if (cause instanceof FeignException feignException) {

            int feignStatus = feignException.status();
            if (feignStatus > 0) {
                status = HttpStatus.valueOf(feignStatus);
            }

            String content = feignException.contentUTF8();
            if (content != null && !content.isEmpty()) {
                try {
                    ApiResponse<?> errorResponse = objectMapper.readValue(content, ApiResponse.class);
                    if (errorResponse.getMessage() != null) {
                        errorMessage = errorResponse.getMessage();
                    }
                } catch (Exception e) {
                    log.warn("[Fallback] JSON Parsing Failed. Content: {}", content);
                }
            }
        }

        return ResponseEntity.status(status)
                .body(ApiResponse.error(errorMessage));
    }
}