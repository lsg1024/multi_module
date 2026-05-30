package com.msa.jewelry.local.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.domain.dto.MessageDto;
import com.msa.jewelry.local.user.entity.SensConfig;
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

@Slf4j
@Component
public class NaverSensApi {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
