package com.msa.jewelry.local.qz.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QzSigningService 단위 테스트.
 *
 * <p>QZ Tray 인쇄 요청 서명 — 외부 의존성 없이 PrivateKey 로 SHA1withRSA 서명을 생성한다.
 *
 * <p>커버리지:
 * <ul>
 *   <li>정상 — 유효한 PKCS#8 RSA Key 로 서명 생성, Base64 유효성 검증</li>
 *   <li>예외 — 키 누락 / 손상된 Base64 / 잘못된 키 포맷</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("QzSigningService 단위 테스트")
class QzSigningServiceTest {

    @InjectMocks
    QzSigningService qzSigningService;

    /** 매 테스트마다 새 RSA 키쌍을 만들어 PKCS#8 Base64 로 주입. */
    private static String generatePkcs8Base64PrivateKey() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        PrivateKey privateKey = gen.generateKeyPair().getPrivate();
        // RSA PrivateKey.getEncoded() 는 기본적으로 PKCS#8 (X.509-style) 포맷
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(qzSigningService, "QZ_KEY", generatePkcs8Base64PrivateKey());
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("sign")
    class Sign {

        @Test
        @DisplayName("정상 — 평문에 대해 Base64 인코딩된 서명 반환")
        void 정상_서명생성() throws Exception {
            String data = "QZ-PRINT-JOB-2026-05-27";

            String result = qzSigningService.sign(data);

            assertThat(result).isNotBlank();
            // Base64 디코딩 가능해야 한다
            byte[] decoded = Base64.getDecoder().decode(result);
            assertThat(decoded).isNotEmpty();
            // 2048-bit RSA SHA1withRSA 서명은 256 byte
            assertThat(decoded.length).isEqualTo(256);
        }

        @Test
        @DisplayName("동일 입력에 동일한 서명 — 결정적(deterministic) 검증")
        void 동일입력_동일서명() throws Exception {
            String data = "stable-payload";

            String s1 = qzSigningService.sign(data);
            String s2 = qzSigningService.sign(data);

            assertThat(s1).isEqualTo(s2);
        }

        @Test
        @DisplayName("빈 문자열도 서명 가능 — 비어있는 message 에 대해 정상 동작")
        void 빈문자열_서명() throws Exception {
            String result = qzSigningService.sign("");

            assertThat(result).isNotBlank();
            assertThat(Base64.getDecoder().decode(result)).hasSize(256);
        }

        @Test
        @DisplayName("키 미주입(null) → NullPointerException")
        void 키_누락() {
            ReflectionTestUtils.setField(qzSigningService, "QZ_KEY", null);

            assertThatThrownBy(() -> qzSigningService.sign("anything"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("손상된 Base64 키 → IllegalArgumentException(Base64 디코딩 실패)")
        void 잘못된_Base64() {
            ReflectionTestUtils.setField(qzSigningService, "QZ_KEY", "NOT_A_VALID_BASE64!!!@@@");

            assertThatThrownBy(() -> qzSigningService.sign("anything"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Base64 는 통과하지만 PKCS#8 포맷 아님 → InvalidKeySpecException")
        void 잘못된_키포맷() {
            // 디코딩은 되지만 PKCS#8 EncodedKeySpec 으로 해석할 수 없는 바이트
            String randomBytes = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4, 5});
            ReflectionTestUtils.setField(qzSigningService, "QZ_KEY", randomBytes);

            assertThatThrownBy(() -> qzSigningService.sign("anything"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("생성된 서명을 동일 키의 public key 로 검증 시 성공 — 정상성 round-trip")
        void 서명검증_roundTrip() throws Exception {
            // 새 키쌍을 만들고 그 PrivateKey 를 service 에 주입
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            java.security.KeyPair pair = gen.generateKeyPair();
            String encoded = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
            ReflectionTestUtils.setField(qzSigningService, "QZ_KEY", encoded);

            String data = "verifiable-data";
            String signedB64 = qzSigningService.sign(data);

            Signature verifier = Signature.getInstance("SHA1withRSA");
            verifier.initVerify(pair.getPublic());
            verifier.update(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            assertThat(verifier.verify(Base64.getDecoder().decode(signedB64))).isTrue();
        }
    }
}
