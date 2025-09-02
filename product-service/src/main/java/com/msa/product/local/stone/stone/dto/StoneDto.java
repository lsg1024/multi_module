package com.msa.product.local.stone.stone.dto;

import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StoneDto {

    public static final String NO_MESSAGE = "필수 입력값 입니다.";

    @NotBlank(message = NO_MESSAGE)
    private String stoneName;
    private String stoneNote;
    private String stoneWeight;
    private Integer stonePurchasePrice;
    @Valid
    private List<StoneWorkGradePolicyDto> stoneWorkGradePolicyDto;

    @Getter
    @NoArgsConstructor
    public static class ResponseSingle {
        private String stoneId;
        private String stoneName;
        private String stoneNote;
        private String stoneWeight;
        private Integer stonePurchasePrice;
        private List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDto;

        @Builder
        @QueryProjection
        public ResponseSingle(String stoneId, String stoneName, String stoneNote, String stoneWeight, Integer stonePurchasePrice, List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDto) {
            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.stoneNote = stoneNote;
            this.stoneWeight = stoneWeight;
            this.stonePurchasePrice = stonePurchasePrice;
            this.stoneWorkGradePolicyDto = stoneWorkGradePolicyDto;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class PageDto {
        private String stoneId;
        private String stoneName;
        private String stoneNote;
        private String stoneWeight;
        private Integer stonePurchasePrice;
        private List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDto;

        @Builder
        @QueryProjection
        public PageDto(String stoneId, String stoneName, String stoneNote, String stoneWeight, Integer stonePurchasePrice, List<StoneWorkGradePolicyDto.Response> stoneWorkGradePolicyDto) {            this.stoneId = stoneId;
            this.stoneName = stoneName;
            this.stoneNote = stoneNote;
            this.stoneWeight = stoneWeight;
            this.stonePurchasePrice = stonePurchasePrice;
            this.stoneWorkGradePolicyDto = stoneWorkGradePolicyDto;
        }
    }
}
