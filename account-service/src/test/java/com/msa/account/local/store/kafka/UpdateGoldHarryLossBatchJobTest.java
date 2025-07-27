package com.msa.account.local.store.kafka;

import com.msa.account.global.batch.UpdateGoldHarryLossBatchJobConfig;
import com.msa.account.global.domain.entity.CommonOption;
import com.msa.account.global.domain.entity.GoldHarry;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa.account.global.domain.repository.CommonOptionRepository;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest()
@SpringBatchTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application.yml")
@Import(UpdateGoldHarryLossBatchJobConfig.class)
public class UpdateGoldHarryLossBatchJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private GoldHarryRepository goldHarryRepository;

    @Autowired
    private CommonOptionRepository commonOptionRepository;

    private GoldHarry goldHarry1;
    private GoldHarry goldHarry2;

    @BeforeEach
    void setup() {
        // 테스트 데이터 초기화
        goldHarry1 = goldHarryRepository.save(new GoldHarry(new BigDecimal("1.11")));
        goldHarry2 = goldHarryRepository.save(new GoldHarry(new BigDecimal("2.22")));

        CommonOption option1 = CommonOption.builder()
                .goldHarry(goldHarry1)
                .goldHarryLoss("1.11")
                .optionLevel(null)
                .optionTradeType(null)
                .build();

        CommonOption option2 = CommonOption.builder()
                .goldHarry(goldHarry1)
                .goldHarryLoss("1.11")
                .optionLevel(null)
                .optionTradeType(null)
                .build();

        commonOptionRepository.saveAll(List.of(option1, option2));
    }

    @Test
    void goldHarryLoss_업데이트_배치_성공() throws Exception {
        // given
        String newLossValue = "9.99";
        goldHarry1.updateLoss(String.valueOf(new BigDecimal(newLossValue)));
        goldHarryRepository.save(goldHarry1);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("goldHarryId", goldHarry1.getGoldHarryId())
                .addString("updatedGoldHarryLoss", newLossValue)
                .addLong("timestamp", System.currentTimeMillis()) // 중복 방지
                .toJobParameters();

        // when
        JobExecution execution = jobLauncherTestUtils.getJobLauncher()
                .run(jobLauncherTestUtils.getJob(), jobParameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<CommonOption> updated = commonOptionRepository.findAll();
        assertThat(updated).allMatch(opt -> opt.getGoldHarryLoss().equals(newLossValue));
    }
}