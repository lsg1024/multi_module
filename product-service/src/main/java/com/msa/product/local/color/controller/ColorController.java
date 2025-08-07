package com.msa.product.local.color.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.product.local.color.dto.ColorDto;
import com.msa.product.local.color.service.ColorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ColorController {

    private final ColorService colorService;

    public ColorController(ColorService colorService) {
        this.colorService = colorService;
    }

    @PostMapping("/colors")
    public ResponseEntity<ApiResponse<String>> createColor(
            @Valid @RequestBody ColorDto colorDto) {

        colorService.saveColor(colorDto);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    /**
     * 테스트용 생성 코드 단일 save 분리호출
     */
    @PostMapping("/colorss")
    public ResponseEntity<ApiResponse<String>> createColors(
            @Valid @RequestBody List<ColorDto> colorDtos) {

        colorDtos.forEach(colorService::saveColor);
        return ResponseEntity.ok(ApiResponse.success("일괄 생성 완료"));
    }

    @GetMapping("/colors/{id}")
    public ResponseEntity<ApiResponse<ColorDto.ResponseSingle>> getColor(
            @PathVariable(name = "id") Long colorId) {

        ColorDto.ResponseSingle color = colorService.getColor(colorId);
        return ResponseEntity.ok(ApiResponse.success(color));
    }

    @GetMapping("/colors")
    public ResponseEntity<ApiResponse<List<ColorDto.ResponseSingle>>> getColors(
            @RequestParam(name = "name", required = false) String colorName) {

        return ResponseEntity.ok(ApiResponse.success(colorService.getColors(colorName)));
    }

    @PatchMapping("/colors/{id}")
    public ResponseEntity<ApiResponse<String>> updateColor(
            @PathVariable(name = "id") Long colorId,
            @Valid @RequestBody ColorDto colorDto) {

        colorService.updateColor(colorId, colorDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @DeleteMapping("/colors/{id}")
    public ResponseEntity<ApiResponse<String>> deleteColor(
            @AccessToken String accessToken,
            @PathVariable(name = "id") Long colorId) {

        colorService.deleteColor(accessToken, colorId);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }

    @GetMapping("/api/color/{id}")
    public ResponseEntity<ApiResponse<String>> getColorInfo(@PathVariable Long id) {
        String colorName = colorService.getColorName(id);
        return ResponseEntity.ok(ApiResponse.success(colorName));
    }
}
