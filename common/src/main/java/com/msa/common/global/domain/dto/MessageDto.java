package com.msa.common.global.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class MessageDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensConfigRequest {
        @NotBlank(message = "Access Key는 필수입니다.")
        private String accessKey;

        @NotBlank(message = "Secret Key는 필수입니다.")
        private String secretKey;

        @NotBlank(message = "Service ID는 필수입니다.")
        private String serviceId;

        @NotBlank(message = "발신번호는 필수입니다.")
        private String senderPhone;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensConfigResponse {
        private Long id;
        private String accessKey;
        private String serviceId;
        private String senderPhone;
        private boolean enabled;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRequest {
        @NotEmpty(message = "전송 대상이 필요합니다.")
        private List<Long> storeIds;

        @NotBlank(message = "메시지 내용은 필수입니다.")
        private String content;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendResult {
        private String storeName;
        private String phone;
        private String status;
        private String errorMessage;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryResponse {
        private Long id;
        private String receiverPhone;
        private String receiverName;
        private String content;
        private String status;
        private String errorMessage;
        private String sentBy;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorePhoneInfo {
        private Long storeId;
        private String storeName;
        private String storePhoneNumber;
    }

    // Naver SENS API request/response
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NaverSmsRequest {
        private String type;
        private String contentType;
        private String countryCode;
        private String from;
        private String content;
        private List<NaverSmsMessage> messages;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NaverSmsMessage {
        private String to;
        private String content;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NaverSmsResponse {
        private String requestId;
        private String requestTime;
        private String statusCode;
        private String statusName;
    }
}
