package com.msa.account.global.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.account.global.kafka.dto.GoldHarryLossUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaConsumer {

    private final JobLauncher jobLauncher;
    private final Job updateStoreGoldHarryLossJob;

    public KafkaConsumer(JobLauncher jobLauncher, Job updateStoreGoldHarryLossJob) {
        this.jobLauncher = jobLauncher;
        this.updateStoreGoldHarryLossJob = updateStoreGoldHarryLossJob;
    }
    @KafkaListener(topics = "goldHarryLoss.update", groupId = "goldHarry-group")
    public void handleGoldHarryLossUpdate(String message) {
        log.info("RAW message from kafka: {}", message);
        ObjectMapper om = new ObjectMapper();
        try {
            GoldHarryLossUpdatedEvent event = om.readValue(message, GoldHarryLossUpdatedEvent.class);
            log.info("Parsed event: id={}, loss={}", event.goldHarryId(), event.newGoldHarryLoss());

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("commonOptionId", event.goldHarryId())
                    .addString("updatedGoldHarryLoss", event.newGoldHarryLoss())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(updateStoreGoldHarryLossJob, jobParameters);
        } catch (Exception e) {
            log.error("Parse or batch launch failed", e);
        }
    }
}
