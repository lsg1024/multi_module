package com.msa.order.local.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StoreDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        private Long storeId;
        private String storeName;
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private Long storeId;
        private String storeName;
        private String grade;
        private String storeHarry;

        @Builder
        public Response(Long storeId, String storeName, String grade, String storeHarry) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.grade = grade;
            this.storeHarry = storeHarry;
        }

    }
}
