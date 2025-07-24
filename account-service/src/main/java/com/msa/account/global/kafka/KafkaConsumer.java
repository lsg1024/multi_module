package com.msa.account.global.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.account.global.kafka.dto.GoldHarryDeletedEvent;
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
    private final Job deleteGoldHarryJob;

    public KafkaConsumer(JobLauncher jobLauncher, Job updateStoreGoldHarryLossJob, Job deleteGoldHarryJob) {
        this.jobLauncher = jobLauncher;
        this.updateStoreGoldHarryLossJob = updateStoreGoldHarryLossJob;
        this.deleteGoldHarryJob = deleteGoldHarryJob;
    }
    @KafkaListener(topics = "goldHarryLoss.update", groupId = "goldHarry-group", concurrency = "3")
    public void handleGoldHarryLossUpdate(String message) {
        ObjectMapper om = new ObjectMapper();
        try {
            GoldHarryLossUpdatedEvent event = om.readValue(message, GoldHarryLossUpdatedEvent.class);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tenantId", event.tenantId())
                    .addLong("goldHarryId", event.goldHarryId())
                    .addString("updatedGoldHarryLoss", event.newGoldHarryLoss())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(updateStoreGoldHarryLossJob, jobParameters);
        } catch (Exception e) {
            log.error("Parse or batch launch failed", e);
            throw new IllegalArgumentException(e);
        }
    }

    @KafkaListener(topics = "goldHarry.deleted", groupId = "goldHarry-group", concurrency = "3")
    public void handleGoldHarryDelete(String message) {
        ObjectMapper oj = new ObjectMapper();
        try {
            GoldHarryDeletedEvent event = oj.readValue(message, GoldHarryDeletedEvent.class);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tenantId", event.tenantId())
                    .addString("goldHarryId", event.goldHarryId())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(deleteGoldHarryJob, jobParameters);
        } catch (Exception e) {
            log.error("Parse or batch launch failed", e);
            throw new IllegalArgumentException(e);
        }
    }
}
