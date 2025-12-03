package com.msa.product.global.batch.other;

import com.msa.product.global.kafka.KafkaProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UpdateJobListener implements JobExecutionListener {

    private final KafkaProducer kafkaProducer;

    public UpdateJobListener(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {


            JobParameters params = jobExecution.getJobParameters();
            String tenantId = params.getString("tenantId");

            log.info("UpdateJobListener afterJob {}", params.getParameters());

            if (params.getParameters().containsKey("materialId")) {
                Long materialId = params.getLong("materialId");
                kafkaProducer.sendMaterialDelete(tenantId, materialId);
            } else if (params.getParameters().containsKey("classificationId")) {
                Long classificationId = params.getLong("classificationId");
                kafkaProducer.sendClassificationDelete(tenantId, classificationId);
            } else if (params.getParameters().containsKey("setTypeId")) {
                Long setTypeId = params.getLong("setTypeId");
                kafkaProducer.sendSetTypeDelete(tenantId, setTypeId);
            } else if (params.getParameters().containsKey("colorId")) {
                Long colorId = params.getLong("colorId");
                kafkaProducer.sendColorDelete(tenantId, colorId);
            }
        }
    }
}
