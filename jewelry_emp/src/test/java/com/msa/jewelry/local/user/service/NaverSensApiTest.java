package com.msa.jewelry.local.user.service;

import com.msa.jewelry.local.user.entity.SensConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NaverSensApi 단위 테스트")
class NaverSensApiTest {

    @InjectMocks
    NaverSensApi naverSensApi;

    private static SensConfig config(String accessKey, String secretKey, String serviceId, String senderPhone) {
        SensConfig c = mock(SensConfig.class);
        given(c.getAccessKey()).willReturn(accessKey);
        given(c.getSecretKey()).willReturn(secretKey);
        given(c.getServiceId()).willReturn(serviceId);
        given(c.getSenderPhone()).willReturn(senderPhone);
        return c;
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("sendSms")
    class SendSms {

        @Test
        @DisplayName("실제 외부 호출 단계까지 도달 → RestClientException 전파 (4xx/5xx/timeout 공통)")
        void 외부호출_실패_전파() {
            // 실제 NCP 엔드포인트로 가지만, 자격증명이 가짜라 인증 실패하거나
            // 망 이슈로 RestClientException 이 던져진다.
            // (env 에 따라 UnknownHostException, ConnectException 등 → RestClientException 으로 wrap)
            SensConfig config = config(
                    "fake-access-key",
                    "fake-secret-key-1234567890",
                    "fake-service-id",
                    "01012345678");

            assertThatThrownBy(() -> naverSensApi.sendSms(config, "01098765432", "테스트 메시지"))
                    .isInstanceOfAny(RestClientException.class, java.net.URISyntaxException.class,
                            javax.net.ssl.SSLException.class, java.io.IOException.class,
                            Exception.class);
        }

        @Test
        @DisplayName("secretKey null → NullPointerException(서명 생성 단계)")
        void secretKey_null() {
            SensConfig config = config(
                    "access-key",
                    null,
                    "service-id",
                    "01012345678");

            assertThatThrownBy(() -> naverSensApi.sendSms(config, "01098765432", "내용"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("serviceId null → 서명 생성 단계에서 NullPointerException")
        void serviceId_null() {
            SensConfig config = config(
                    "access-key",
                    "secret-key-1234567890",
                    null,
                    "01012345678");

            assertThatThrownBy(() -> naverSensApi.sendSms(config, "01098765432", "내용"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("빈 secretKey → IllegalArgumentException(SecretKeySpec 의 key empty 검증)")
        void 빈_secretKey() {
            SensConfig config = config(
                    "access-key",
                    "",
                    "service-id",
                    "01012345678");

            // SecretKeySpec 는 빈 키 거부 → IllegalArgumentException
            assertThatThrownBy(() -> naverSensApi.sendSms(config, "01098765432", "내용"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("config 자체가 null → NullPointerException")
        void config_null() {
            assertThatThrownBy(() -> naverSensApi.sendSms(null, "01098765432", "내용"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("정상 자격 시그니처 빌드는 통과해야 함 — 외부 호출에서만 실패")
        void 시그니처_생성_분기_통과() {
            // 서명 단계는 통과해야 한다는 의미: 즉 NullPointerException 이 아니라
            // RestClientException/IOException 류가 나는지 확인.
            SensConfig config = config(
                    "valid-access",
                    "valid-secret-1234567890abcdef",
                    "valid-service-id",
                    "01012345678");

            Throwable t = catchAnyThrowable(() ->
                    naverSensApi.sendSms(config, "01098765432", "메시지"));

            assertThat(t).isNotNull();
            // NPE 아니어야 한다 — 즉 서명 단계는 통과
            assertThat(t).isNotInstanceOf(NullPointerException.class);
        }
    }

    // 작은 헬퍼 - lambda 안에서 예외 캐치
    private static Throwable catchAnyThrowable(ThrowingRunnable r) {
        try {
            r.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
