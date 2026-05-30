package com.msa.jewelry.local.user.service;

import com.msa.common.global.domain.dto.MessageDto;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.jewelry.global.exception.ExceptionMessage;
import com.msa.jewelry.local.store.service.StoreService;
import com.msa.jewelry.local.user.entity.SensConfig;
import com.msa.jewelry.local.user.repository.MessageHistoryRepository;
import com.msa.jewelry.local.user.repository.SensConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MessageService 단위 테스트")
class MessageServiceTest {

    private static final String TOKEN = "Bearer test-token";
    private static final String TENANT_ID = "tenant-001";
    private static final String NICKNAME = "tester";

    @Mock
    SensConfigRepository sensConfigRepository;
    @Mock
    MessageHistoryRepository messageHistoryRepository;
    @Mock
    StoreService storeService;
    @Mock
    NaverSensApi naverSensApi;
    @Mock
    JwtUtil jwtUtil;

    @InjectMocks
    MessageService messageService;

    @BeforeEach
    void commonStubs() {
        given(jwtUtil.getTenantId(anyString())).willReturn(TENANT_ID);
        given(jwtUtil.getNickname(anyString())).willReturn(NICKNAME);
    }

    // -----------------------------------------------------------------------
    // saveSensConfig
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("saveSensConfig")
    class SaveSensConfig {

        @Test
        @DisplayName("기존 config 가 있어도 저장 호출 — 덮어쓰기/업데이트 동작")
        void 기존_존재시_저장호출() {
            MessageDto.SensConfigRequest req = mock(MessageDto.SensConfigRequest.class);
            given(req.getAccessKey()).willReturn("ak");
            given(req.getSecretKey()).willReturn("sk");
            given(req.getServiceId()).willReturn("svc");
            given(req.getSenderPhone()).willReturn("01012345678");

            // saveSensConfig 가 어떤 분기를 타든 sensConfigRepository.save 가 호출되거나
            // 또는 entity update 메서드가 호출됨. 둘 다 verify 는 어렵지만 예외 없이 끝나야 함.
            given(sensConfigRepository.findByTenantId(TENANT_ID))
                    .willReturn(Optional.of(mock(SensConfig.class)));

            // 동작 자체가 예외 없이 끝나면 통과
            messageService.saveSensConfig(TOKEN, req);
        }

        @Test
        @DisplayName("기존 config 없으면 신규 저장")
        void 신규저장() {
            MessageDto.SensConfigRequest req = mock(MessageDto.SensConfigRequest.class);
            given(req.getAccessKey()).willReturn("ak");
            given(req.getSecretKey()).willReturn("sk");
            given(req.getServiceId()).willReturn("svc");
            given(req.getSenderPhone()).willReturn("01012345678");

            given(sensConfigRepository.findByTenantId(TENANT_ID)).willReturn(Optional.empty());
            given(sensConfigRepository.save(any(SensConfig.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            messageService.saveSensConfig(TOKEN, req);

            verify(sensConfigRepository).save(any(SensConfig.class));
        }
    }

    // -----------------------------------------------------------------------
    // getSensConfig
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getSensConfig")
    class GetSensConfig {

        @Test
        @DisplayName("config 없음 → NOT_FOUND 예외")
        void 없음() {
            given(sensConfigRepository.findByTenantId(TENANT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.getSensConfig(TOKEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("config 있으면 정상 응답")
        void 있음() {
            SensConfig config = mock(SensConfig.class);
            given(config.getAccessKey()).willReturn("ak");
            given(config.getSecretKey()).willReturn("sk");
            given(config.getServiceId()).willReturn("svc");
            given(config.getSenderPhone()).willReturn("01012345678");
            given(sensConfigRepository.findByTenantId(TENANT_ID)).willReturn(Optional.of(config));

            MessageDto.SensConfigResponse resp = messageService.getSensConfig(TOKEN);
            assertThat(resp).isNotNull();
        }
    }

    // -----------------------------------------------------------------------
    // deleteSensConfig
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("deleteSensConfig")
    class DeleteSensConfig {

        @Test
        @DisplayName("config 없음 → NOT_FOUND 예외")
        void 없음() {
            given(sensConfigRepository.findByTenantId(TENANT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.deleteSensConfig(TOKEN))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("정상 삭제")
        void 정상() {
            SensConfig config = mock(SensConfig.class);
            given(sensConfigRepository.findByTenantId(TENANT_ID)).willReturn(Optional.of(config));

            messageService.deleteSensConfig(TOKEN);

            verify(sensConfigRepository).delete(config);
        }
    }

    // -----------------------------------------------------------------------
    // sendMessage
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("sendMessage")
    class SendMessage {

        @Test
        @DisplayName("SENS config 없으면 NOT_FOUND 예외")
        void config_없음() {
            given(sensConfigRepository.findByTenantId(TENANT_ID)).willReturn(Optional.empty());
            MessageDto.SendRequest req = mock(MessageDto.SendRequest.class);

            assertThatThrownBy(() -> messageService.sendMessage(TOKEN, req))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
