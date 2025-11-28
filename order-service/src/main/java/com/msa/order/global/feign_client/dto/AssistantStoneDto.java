package com.msa.order.global.feign_client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AssistantStoneDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long assistantStoneId;
        private String assistantStoneName;
        private String assistantStoneNote;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String assistantName;
        private String assistantNote;
    }

}