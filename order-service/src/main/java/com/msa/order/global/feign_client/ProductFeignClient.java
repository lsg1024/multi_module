package com.msa.order.global.feign_client;

import com.msa.common.global.api.ApiResponse;
import com.msa.order.global.feign_client.dto.AssistantStoneDto;
import com.msa.order.global.feign_client.dto.ProductDetailDto;
import com.msa.order.global.feign_client.dto.ProductImageDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "product", path = "/product/api")
public interface ProductFeignClient {

    @GetMapping("/classification/{classificationId}")
    ResponseEntity<ApiResponse<String>> getClassificationInfo(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("classificationId") Long classificationId
    );

    @GetMapping("/assistant_stone/{assistantStoneId}")
    ResponseEntity<ApiResponse<AssistantStoneDto.Response>> getAssistantStoneInfo(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("assistantStoneId") Long assistantStoneId
    );

    @GetMapping("/color/{colorId}")
    ResponseEntity<ApiResponse<String>> getColorInfo(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("colorId") Long colorId
    );

    @GetMapping("/material/{materialId}")
    ResponseEntity<ApiResponse<String>> getMaterialInfo(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("materialId") Long materialId
    );

    @GetMapping("/product/{productId}/{grade}")
    ResponseEntity<ApiResponse<ProductDetailDto>> getProductInfo(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("productId") Long productId,
            @PathVariable("grade") String grade
    );

    @GetMapping("/products/images")
    ResponseEntity<ApiResponse<Map<Long, ProductImageDto>>> getProductImages(
            @RequestHeader Map<String, Object> headers,
            @RequestParam("ids") List<Long> productIds
    );

    @GetMapping("/set-type/{setTypeId}")
    ResponseEntity<ApiResponse<String>> getSetTypeName(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("setTypeId") Long setTypeId
    );

    @GetMapping("/stone/{stoneId}")
    ResponseEntity<ApiResponse<Boolean>> getExistStoneId(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("stoneId") Long stoneId
    );
}