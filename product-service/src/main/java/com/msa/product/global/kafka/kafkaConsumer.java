package com.msa.product.global.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.tenant.TenantContext;
import com.msa.product.global.exception.KafkaProcessingException;
import com.msa.product.global.kafka.dto.ClassificationEvent;
import com.msa.product.global.kafka.dto.ColorEvent;
import com.msa.product.global.kafka.dto.MaterialEvent;
import com.msa.product.global.kafka.dto.SetTypeEvent;
import com.msa.product.local.classification.entity.Classification;
import com.msa.product.local.classification.repository.ClassificationRepository;
import com.msa.product.local.color.entity.Color;
import com.msa.product.local.color.repository.ColorRepository;
import com.msa.product.local.material.entity.Material;
import com.msa.product.local.material.repository.MaterialRepository;
import com.msa.product.local.set.entity.SetType;
import com.msa.product.local.set.repository.SetTypeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class kafkaConsumer {

    private final ObjectMapper objectMapper;
    private final JobLauncher jobLauncher;
    private final SetTypeRepository setTypeRepository;
    private final MaterialRepository materialRepository;
    private final ClassificationRepository classificationRepository;
    private final ColorRepository colorRepository;
    private final Job classificationUpdateJob;
    private final Job materialUpdateJob;
    private final Job setTypeUpdateJob;
    private final Job colorUpdateJob;

    public kafkaConsumer(ObjectMapper objectMapper, JobLauncher jobLauncher,
                         SetTypeRepository setTypeRepository, MaterialRepository materialRepository,
                         ClassificationRepository classificationRepository,
                         ColorRepository colorRepository,
                         @Qualifier("updateClassificationJob") Job classificationUpdateJob,
                         @Qualifier("updateMaterialUpdateJob") Job materialUpdateJob,
                         @Qualifier("updateSetTypeUpdateJob") Job setTypeUpdateJob,
                         @Qualifier("updateColorJob") Job colorUpdateJob) {
        this.objectMapper = objectMapper;
        this.jobLauncher = jobLauncher;
        this.setTypeRepository = setTypeRepository;
        this.materialRepository = materialRepository;
        this.classificationRepository = classificationRepository;
        this.colorRepository = colorRepository;
        this.classificationUpdateJob = classificationUpdateJob;
        this.materialUpdateJob = materialUpdateJob;
        this.setTypeUpdateJob = setTypeUpdateJob;
        this.colorUpdateJob = colorUpdateJob;
    }

    @KafkaListener(topics = "classification.update", groupId = "classification-group", concurrency = "3")
    @RetryableTopic(attempts = "2", backoff = @Backoff(delay = 1000, maxDelay = 5000, random = true), include = KafkaProcessingException.class)
    public void handleClassificationUpdate(String message) {
        try {

            ClassificationEvent event = objectMapper.readValue(message, ClassificationEvent.class);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tenantId", event.tenantId())
                    .addLong("classificationId", event.classificationId())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(classificationUpdateJob, jobParameters);
        } catch (Exception e) {
            throw new KafkaProcessingException("classification.update 처리 중 실패 " + e);
        }
    }

    @KafkaListener(topics = "classification.delete", groupId = "classification-group")
    public void handleClassificationDelete(String message) {
        try {
            ClassificationEvent event = objectMapper.readValue(message, ClassificationEvent.class);
            TenantContext.setTenant(event.tenantId());

            Classification classification = classificationRepository.findById(event.classificationId())
                    .orElseThrow(() -> new IllegalArgumentException("소재 없음"));

            classificationRepository.delete(classification);

        } catch (Exception e) {
            log.error("분류 삭제 실패: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "material.update", groupId = "material-group", concurrency = "3")
    @RetryableTopic(attempts = "2", backoff = @Backoff(delay = 1000, maxDelay = 5000, random = true), include = KafkaProcessingException.class)
    public void handleMaterialUpdate(String message) {
        try {
            MaterialEvent event = objectMapper.readValue(message, MaterialEvent.class);
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tenantId", event.tenantId())
                    .addLong("materialId", event.materialId())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(materialUpdateJob, jobParameters);
        } catch (Exception e) {
            throw new KafkaProcessingException("handleMaterialUpdate 오류 " + e);
        }
    }

    @KafkaListener(topics = "material.delete", groupId = "material-group")
    public void handleMaterialDelete(String message) {
        try {
            MaterialEvent event = objectMapper.readValue(message, MaterialEvent.class);
            TenantContext.setTenant(event.tenantId());

            Material material = materialRepository.findById(event.materialId())
                    .orElseThrow(() -> new IllegalArgumentException("소재 없음"));

            materialRepository.delete(material);

        } catch (Exception e) {
            log.error("재질 삭제 실패: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "set-type.update", groupId = "set-type-group", concurrency = "3")
    @RetryableTopic(attempts = "2", backoff = @Backoff(delay = 1000, maxDelay = 5000, random = true), include = KafkaProcessingException.class)
    public void handleSetTypeUpdate(String message) {
        try {
            SetTypeEvent event = objectMapper.readValue(message, SetTypeEvent.class);
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tenantId", event.tenantId())
                    .addLong("setTypeId", event.setTypeId())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(setTypeUpdateJob, jobParameters);
        } catch (Exception e) {
            throw new KafkaProcessingException("handleSetTypeUpdate 오류 " + e);
        }
    }

    @KafkaListener(topics = "set-type.delete", groupId = "set-type-group")
    public void handleSetTypeDelete(String message) {
        try {
            SetTypeEvent event = objectMapper.readValue(message, SetTypeEvent.class);
            TenantContext.setTenant(event.tenantId());

            SetType setType = setTypeRepository.findById(event.setTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("소재 없음"));

            setTypeRepository.delete(setType);

        } catch (Exception e) {
            log.error("세트 타입 삭제 실패: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "color.update", groupId = "color-group", concurrency = "3")
    @RetryableTopic(attempts = "2", backoff = @Backoff(delay = 1000, maxDelay = 5000, random = true), include = KafkaProcessingException.class)
    public void handleColorUpdate(String message) {
        try {
            ColorEvent event = objectMapper.readValue(message, ColorEvent.class);
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("tenantId", event.tenantId())
                    .addLong("colorId", event.colorId())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(setTypeUpdateJob, jobParameters);
        } catch (Exception e) {
            throw new KafkaProcessingException("handleColorUpdate 오류 " + e);
        }
    }

    @KafkaListener(topics = "color.delete", groupId = "color-group")
    public void handleColorDelete(String message) {
        try {
            ColorEvent event = objectMapper.readValue(message, ColorEvent.class);
            TenantContext.setTenant(event.tenantId());

            Color color = colorRepository.findById(event.colorId())
                    .orElseThrow(() -> new IllegalArgumentException("소재 없음"));

            colorRepository.delete(color);

        } catch (Exception e) {
            log.error("색상 타입 삭제 실패: {}", e.getMessage(), e);
        }
    }

}
