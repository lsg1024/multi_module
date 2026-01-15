package com.msa.account.local.saleLog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.account.global.kafka.KafkaConsumer;
import com.msa.account.global.kafka.service.KafkaService;
import com.msa.account.local.transaction_history.domain.entity.SaleLog;
import com.msa.account.local.transaction_history.repository.SaleLogRepository;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.AuditorHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerTest {

    @Mock private KafkaService kafkaService;
    @Mock private SaleLogRepository saleLogRepository;
    @Mock private JobLauncher jobLauncher;
    @Mock private Job saleLogRebalanceJob;
    @Mock private Acknowledgment ack;
    @Mock private Job updateStoreGoldHarryLossJob;
    @Mock private Job deleteGoldHarryJob;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private KafkaConsumer kafkaConsumer;

    @Test
    @DisplayName("[Batch Skip] 가장 최신 거래(미래 로그 없음)라면 배치를 실행하지 않는다")
    void skipBatchTest() throws Exception {
        // given
        // 테스트용 KafkaConsumer 수동 주입 (InjectMocks가 생성자 파라미터가 많을 때 꼬일 수 있음)
        kafkaConsumer = new KafkaConsumer(objectMapper, jobLauncher, updateStoreGoldHarryLossJob, deleteGoldHarryJob, saleLogRebalanceJob, kafkaService, saleLogRepository);

        String jsonMessage = "{\"tenantId\":\"test\", \"id\":1, \"saleDate\":\"2026-01-14T10:00:00\"}";

        // Mock: 서비스가 저장 후 반환할 SaleLog (ID: 100, Date: 10:00)
        SaleLog savedLog = mock(SaleLog.class);
        when(savedLog.getId()).thenReturn(100L);
        when(savedLog.getSaleDate()).thenReturn(LocalDateTime.of(2026, 1, 14, 10, 0));

        when(kafkaService.updateCurrentBalance(any())).thenReturn(savedLog);

        // Mock: 내 뒤에 미래 로그가 있는가? -> FALSE (내가 최신임)
        when(saleLogRepository.existsFutureLog(eq(1L), any(), eq(100L))).thenReturn(false);

        try (MockedStatic<TenantContext> tenantMock = mockStatic(TenantContext.class);
             MockedStatic<AuditorHolder> auditorMock = mockStatic(AuditorHolder.class)) {

            // when
            kafkaConsumer.handleUpdateCurrentBalance(jsonMessage, ack);

            // then
            // 1. 서비스 로직은 실행되어야 함
            verify(kafkaService, times(1)).updateCurrentBalance(any());

            // 2. [핵심] 미래 로그가 없으므로 배치는 실행되지 않아야 함
            verify(jobLauncher, never()).run(any(), any());

            verify(ack, times(1)).acknowledge();
        }
    }

    @Test
    @DisplayName("[Batch Trigger] 과거 날짜 데이터가 삽입되었다면(미래 로그 존재) 배치를 실행한다")
    void triggerBatch_PastDate() throws Exception {
        // given
        kafkaConsumer = new KafkaConsumer(objectMapper, jobLauncher, updateStoreGoldHarryLossJob, deleteGoldHarryJob, saleLogRebalanceJob, kafkaService, saleLogRepository);

        String jsonMessage = "{\"tenantId\":\"test\", \"id\":1, \"saleDate\":\"2026-01-01T10:00:00\"}"; // 과거 날짜

        // Mock: 저장된 로그 (ID: 50, Date: 1월 1일)
        SaleLog savedLog = mock(SaleLog.class);
        when(savedLog.getId()).thenReturn(50L);
        when(savedLog.getSaleDate()).thenReturn(LocalDateTime.of(2026, 1, 1, 10, 0));

        when(kafkaService.updateCurrentBalance(any())).thenReturn(savedLog);

        // Mock: 내 뒤에 미래 로그가 있는가? -> TRUE (이미 1월 14일 데이터가 있다고 가정)
        when(saleLogRepository.existsFutureLog(eq(1L), any(), eq(50L))).thenReturn(true);

        try (MockedStatic<TenantContext> tenantMock = mockStatic(TenantContext.class);
             MockedStatic<AuditorHolder> auditorMock = mockStatic(AuditorHolder.class)) {

            // when
            kafkaConsumer.handleUpdateCurrentBalance(jsonMessage, ack);

            // then
            // [핵심] 배치가 실행되어야 함
            verify(jobLauncher, times(1)).run(eq(saleLogRebalanceJob), any(JobParameters.class));
            verify(ack, times(1)).acknowledge();
        }
    }
}