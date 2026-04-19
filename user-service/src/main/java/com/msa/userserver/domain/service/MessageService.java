package com.msa.userserver.domain.service;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.domain.dto.MessageDto;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.tenant.TenantContext;
import com.msa.userserver.domain.entity.MessageHistory;
import com.msa.userserver.domain.entity.SensConfig;
import com.msa.userserver.domain.feign.AccountClient;
import com.msa.userserver.domain.respository.MessageHistoryRepository;
import com.msa.userserver.domain.respository.SensConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SMS 전송 서비스.
 *
 * *테넌트별 Naver SENS 설정 관리(저장·조회·삭제)와 SMS 전송 및 전송 이력 관리를 담당한다.
 *
 * *주요 동작:
 *
 *   - SENS 설정 관리 — 테넌트ID 기준으로 {@link SensConfig}를 upsert 처리한다.
 *   - SMS 전송 — Feign 클라이언트({@link com.msa.userserver.domain.feign.AccountClient})로
 *       account-service에서 매장 전화번호를 일괄 조회한 뒤,
 *       각 매장에 대해 {@link NaverSensApi#sendSms}를 호출하고 성공/실패 결과를 수집한다.
 *   - 전송 이력 관리 — 전송 성공·실패 여부와 관계없이 모든 전송 시도를
 *       {@link com.msa.userserver.domain.entity.MessageHistory}로 저장한다.
 * 
 *
 * *의존성:
 *
 *   - {@link com.msa.userserver.domain.respository.SensConfigRepository}
 *   - {@link com.msa.userserver.domain.respository.MessageHistoryRepository}
 *   - {@link com.msa.userserver.domain.feign.AccountClient} — Feign으로 Store 전화번호 조회
 *   - {@link NaverSensApi} — SENS API 실제 호출
 *   - {@link com.msa.common.global.jwt.JwtUtil} — 액세스 토큰에서 tenantId·nickname 추출
 * 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final SensConfigRepository sensConfigRepository;
    private final MessageHistoryRepository messageHistoryRepository;
    private final AccountClient accountClient;
    private final NaverSensApi naverSensApi;
    private final JwtUtil jwtUtil;

    // SENS 설정 저장/수정
    @Transactional
    public MessageDto.SensConfigResponse saveSensConfig(String accessToken, MessageDto.SensConfigRequest request) {
        String tenantId = jwtUtil.getTenantId(accessToken);

        SensConfig config = sensConfigRepository.findByTenantId(tenantId)
                .map(existing -> {
                    existing.update(request.getAccessKey(), request.getSecretKey(),
                            request.getServiceId(), request.getSenderPhone());
                    return existing;
                })
                .orElseGet(() -> sensConfigRepository.save(
                        SensConfig.builder()
                                .tenantId(tenantId)
                                .accessKey(request.getAccessKey())
                                .secretKey(request.getSecretKey())
                                .serviceId(request.getServiceId())
                                .senderPhone(request.getSenderPhone())
                                .build()
                ));

        return MessageDto.SensConfigResponse.builder()
                .id(config.getId())
                .accessKey(config.getAccessKey())
                .serviceId(config.getServiceId())
                .senderPhone(config.getSenderPhone())
                .enabled(config.isEnabled())
                .build();
    }

    // SENS 설정 조회
    @Transactional(readOnly = true)
    public MessageDto.SensConfigResponse getSensConfig(String accessToken) {
        String tenantId = jwtUtil.getTenantId(accessToken);

        SensConfig config = sensConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("SENS 설정이 존재하지 않습니다."));

        return MessageDto.SensConfigResponse.builder()
                .id(config.getId())
                .accessKey(config.getAccessKey())
                .serviceId(config.getServiceId())
                .senderPhone(config.getSenderPhone())
                .enabled(config.isEnabled())
                .build();
    }

    // SENS 설정 삭제
    @Transactional
    public void deleteSensConfig(String accessToken) {
        String tenantId = jwtUtil.getTenantId(accessToken);

        SensConfig config = sensConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("SENS 설정이 존재하지 않습니다."));

        sensConfigRepository.delete(config);
    }

    // SMS 전송
    @Transactional
    public List<MessageDto.SendResult> sendMessage(String accessToken, MessageDto.SendRequest request) {
        String tenantId = jwtUtil.getTenantId(accessToken);
        String nickname = jwtUtil.getNickname(accessToken);

        SensConfig config = sensConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("SENS 설정이 존재하지 않습니다. 먼저 설정을 완료해주세요."));

        if (!config.isEnabled()) {
            throw new IllegalArgumentException("SENS 서비스가 비활성화 상태입니다.");
        }

        // Feign으로 Store 전화번호 조회
        Map<String, Object> headers = new HashMap<>();
        headers.put("X-Tenant-ID", TenantContext.getTenant());
        ResponseEntity<ApiResponse<List<MessageDto.StorePhoneInfo>>> response =
                accountClient.getStorePhones(headers, request.getStoreIds());

        if (response.getBody() == null || !response.getBody().isSuccess() || response.getBody().getData() == null) {
            throw new IllegalArgumentException("거래처 정보를 가져올 수 없습니다.");
        }

        List<MessageDto.StorePhoneInfo> storePhones = response.getBody().getData();
        List<MessageDto.SendResult> results = new ArrayList<>();

        for (MessageDto.StorePhoneInfo store : storePhones) {
            String phone = store.getStorePhoneNumber();

            if (phone == null || phone.isBlank()) {
                results.add(MessageDto.SendResult.builder()
                        .storeName(store.getStoreName())
                        .phone("")
                        .status("FAILED")
                        .errorMessage("전화번호가 등록되어 있지 않습니다.")
                        .build());

                saveHistory(tenantId, "", store.getStoreName(), request.getContent(),
                        "FAILED", "전화번호 미등록", null, nickname);
                continue;
            }

            // 전화번호에서 하이픈 제거
            String cleanPhone = phone.replaceAll("-", "");

            try {
                MessageDto.NaverSmsResponse smsResponse = naverSensApi.sendSms(config, cleanPhone, request.getContent());

                String requestId = smsResponse != null ? smsResponse.getRequestId() : null;

                results.add(MessageDto.SendResult.builder()
                        .storeName(store.getStoreName())
                        .phone(phone)
                        .status("SUCCESS")
                        .build());

                saveHistory(tenantId, phone, store.getStoreName(), request.getContent(),
                        "SUCCESS", null, requestId, nickname);

            } catch (Exception e) {
                log.error("SMS 전송 실패 - Store: {}, Phone: {}, Error: {}",
                        store.getStoreName(), phone, e.getMessage());

                results.add(MessageDto.SendResult.builder()
                        .storeName(store.getStoreName())
                        .phone(phone)
                        .status("FAILED")
                        .errorMessage(e.getMessage())
                        .build());

                saveHistory(tenantId, phone, store.getStoreName(), request.getContent(),
                        "FAILED", e.getMessage(), null, nickname);
            }
        }

        return results;
    }

    // 전송 이력 조회
    @Transactional(readOnly = true)
    public Page<MessageDto.HistoryResponse> getHistory(String accessToken, Pageable pageable) {
        String tenantId = jwtUtil.getTenantId(accessToken);

        return messageHistoryRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(h -> MessageDto.HistoryResponse.builder()
                        .id(h.getId())
                        .receiverPhone(h.getReceiverPhone())
                        .receiverName(h.getReceiverName())
                        .content(h.getContent())
                        .status(h.getStatus())
                        .errorMessage(h.getErrorMessage())
                        .sentBy(h.getSentBy())
                        .createdAt(h.getCreatedAt())
                        .build());
    }

    private void saveHistory(String tenantId, String phone, String storeName,
                             String content, String status, String errorMessage,
                             String requestId, String sentBy) {
        messageHistoryRepository.save(
                MessageHistory.builder()
                        .tenantId(tenantId)
                        .receiverPhone(phone)
                        .receiverName(storeName)
                        .content(content)
                        .status(status)
                        .errorMessage(errorMessage)
                        .naverRequestId(requestId)
                        .sentBy(sentBy)
                        .build()
        );
    }
}
