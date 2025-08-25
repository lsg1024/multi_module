package com.msa.order.local.order.dto;

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

        public Response(Long storeId, String storeName) {
            this.storeId = storeId;
            this.storeName = storeName;
        }

        public Response(Long storeId, String storeName, String grade) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.grade = grade;
        }
    }
}
