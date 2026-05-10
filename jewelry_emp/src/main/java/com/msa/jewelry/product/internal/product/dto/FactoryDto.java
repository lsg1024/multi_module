package com.msa.jewelry.product.internal.product.dto;

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
        private String factoryHarry;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseBatch {
        private Long factoryId;
        private String factoryName;
    }
}
