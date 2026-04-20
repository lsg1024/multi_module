package com.msa.order.global.feign_client;

import com.msa.common.global.api.ApiResponse;
import com.msa.order.global.feign_client.dto.AssistantStoneDto;
import com.msa.order.global.feign_client.dto.ProductDetailDto;
import com.msa.order.global.feign_client.dto.ProductImageDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "product", path = "/api", fallbackFactory = ProductFeignClientFallbackFactory.class)
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

    @GetMapping("/color/name")
    ResponseEntity<ApiResponse<Long>> getColorIdByName(
            @RequestHeader Map<String, Object> headers,
            @RequestParam("name") String name
    );

    @GetMapping("/material/{materialId}")
    ResponseEntity<ApiResponse<String>> getMaterialInfo(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("materialId") Long materialId
    );

    @GetMapping("/material/name")
    ResponseEntity<ApiResponse<Long>> getMaterialIdByName(
            @RequestHeader Map<String, Object> headers,
            @RequestParam("name") String name
    );

    @GetMapping("/product/name")
    ResponseEntity<ApiResponse<ProductDetailDto>> getProductInfoByName(
            @RequestHeader Map<String, Object> headers,
            @RequestParam("name") String productName
    );

    @PatchMapping("/product/{productId}/factory-name")
    ResponseEntity<ApiResponse<String>> updateProductFactoryName(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("productId") Long productId,
            @RequestBody Map<String, String> body
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

    @GetMapping("/product/stones")
    ResponseEntity<ApiResponse<List<ProductDetailDto.StoneInfo>>> getProductStonesByName(
            @RequestHeader Map<String, Object> headers,
            @RequestParam("name") String productName
    );

    @GetMapping("/stone/{stoneId}")
    ResponseEntity<ApiResponse<Boolean>> getExistStoneId(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("stoneId") Long stoneId
    );
}