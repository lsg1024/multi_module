package com.msa.order.local.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class FactoryDto {
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private Long factoryId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long factoryId;
        private String factoryName;
    }
}
