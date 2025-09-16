package com.msa.product.local.stone.assistantStone.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AssistantStoneDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long assistantId;
        private String assistantName;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String assistantName;
        private String assistantNote;
    }

}
