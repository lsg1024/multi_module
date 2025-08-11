package com.msa.order.local.domain.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StoreDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        private Long storeId;
        private String storeName;
        private String grade;

        @Builder
        public Request(Long storeId, String storeName, String grade) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.grade = grade;
        }
    }
}
