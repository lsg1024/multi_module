package com.msa.userserver.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.domain.dto.MessageDto;
import com.msa.userserver.domain.entity.SensConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

/**
 * Naver SENS SMS API 클라이언트 컴포넌트.
 *
 * *Naver Cloud Platform의 Simple Easy Notification Service(SENS) API를 호출하여
 * SMS 메시지를 전송한다.
 *
 * *주요 동작:
 *
 *   - HMAC-SHA256 서명 생성 — {@code POST} 메서드, API URL, 타임스탬프, 액세스 키를
 *       개행 문자로 연결한 메시지를 secretKey로 서명하고 Base64로 인코딩한다.
 *   - RestTemplate API 호출 — {@code x-ncp-apigw-timestamp},
 *       {@code x-ncp-iam-access-key}, {@code x-ncp-apigw-signature-v2} 헤더를 설정하고
 *       {@code https://sens.apigw.ntruss.com/sms/v2/services/{serviceId}/messages} 에 POST 요청
 * 
 *
 * *의존성: {@link SensConfig} (테넌트별 API 자격증명),
 * {@link com.fasterxml.jackson.databind.ObjectMapper} (요청 직렬화),
 * {@link org.springframework.web.client.RestTemplate}
 */
@Slf4j
@Component
public class NaverSensApi {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 지정된 수신자에게 SMS를 전송한다.
     *
     * *처리 흐름:
     *
     *   - 현재 시각(밀리초)을 타임스탬프로 사용하여 HMAC-SHA256 서명을 생성한다.
     *   - 인증 헤더({@code x-ncp-apigw-timestamp}, {@code x-ncp-iam-access-key},
     *       {@code x-ncp-apigw-signature-v2})를 설정한다.
     *   - {@link com.msa.common.global.domain.dto.MessageDto.NaverSmsRequest}를 JSON으로
     *       직렬화하여 SENS API에 POST 요청을 전송한다.
     *   - API 호출 실패 시 {@link org.springframework.web.client.RestClientException}을
     *       로깅 후 재전파한다.
     * 
     *
     * @param config  테넌트별 SENS 자격증명
     * @param to      수신자 전화번호 (하이픈 없는 숫자 문자열)
     * @param content SMS 본문 내용
     * @return SENS API 응답 ({@code requestId} 포함), 실패 시 예외 발생
     */
    public MessageDto.NaverSmsResponse sendSms(SensConfig config, String to, String content)
            throws JsonProcessingException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException {

        Long time = System.currentTimeMillis();
        String signature = makeSignature(config, time);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-ncp-apigw-timestamp", time.toString());
        headers.set("x-ncp-iam-access-key", config.getAccessKey());
        headers.set("x-ncp-apigw-signature-v2", signature);

        MessageDto.NaverSmsRequest request = MessageDto.NaverSmsRequest.builder()
                .type("SMS")
                .contentType("COMM")
                .countryCode("82")
                .from(config.getSenderPhone())
                .content(content)
                .messages(List.of(
                        MessageDto.NaverSmsMessage.builder()
                                .to(to)
                                .content(content)
                                .build()
                ))
                .build();

        String body = objectMapper.writeValueAsString(request);
        HttpEntity<String> httpBody = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        String apiUrl = "https://sens.apigw.ntruss.com/sms/v2/services/" + config.getServiceId() + "/messages";

        try {
            return restTemplate.postForObject(new URI(apiUrl), httpBody, MessageDto.NaverSmsResponse.class);
        } catch (RestClientException e) {
            log.error("Naver SENS API 호출 실패: {}", e.getMessage());
            throw e;
        }
    }

    private String makeSignature(SensConfig config, Long time)
            throws NoSuchAlgorithmException, InvalidKeyException {

        String url = "/sms/v2/services/" + config.getServiceId() + "/messages";

        String message = "POST" + " " + url + "\n" + time.toString() + "\n" + config.getAccessKey();

        SecretKeySpec signingKey = new SecretKeySpec(
                config.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);

        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(rawHmac);
    }
}
