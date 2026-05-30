package com.msa.jewelry.global.batch.product.other;

import com.msa.jewelry.local.classification.repository.ClassificationRepository;
import com.msa.jewelry.local.color.repository.ColorRepository;
import com.msa.jewelry.local.material.repository.MaterialRepository;
import com.msa.jewelry.local.set.repository.SetTypeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UpdateJobListener implements JobExecutionListener {

    private final ClassificationRepository classificationRepository;
    private final ColorRepository colorRepository;
    private final MaterialRepository materialRepository;
    private final SetTypeRepository setTypeRepository;

    public UpdateJobListener(ClassificationRepository classificationRepository,
                             ColorRepository colorRepository,
                             MaterialRepository materialRepository,
                             SetTypeRepository setTypeRepository) {
        this.classificationRepository = classificationRepository;
        this.colorRepository = colorRepository;
        this.materialRepository = materialRepository;
        this.setTypeRepository = setTypeRepository;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            return;
        }

        JobParameters params = jobExecution.getJobParameters();
        log.info("UpdateJobListener afterJob {}", params.getParameters());

        if (params.getParameters().containsKey("materialId")) {
            Long materialId = params.getLong("materialId");
            materialRepository.findById(materialId).ifPresent(materialRepository::delete);
        } else if (params.getParameters().containsKey("classificationId")) {
            Long classificationId = params.getLong("classificationId");
            classificationRepository.findById(classificationId).ifPresent(classificationRepository::delete);
        } else if (params.getParameters().containsKey("setTypeId")) {
            Long setTypeId = params.getLong("setTypeId");
            setTypeRepository.findById(setTypeId).ifPresent(setTypeRepository::delete);
        } else if (params.getParameters().containsKey("colorId")) {
            Long colorId = params.getLong("colorId");
            colorRepository.findById(colorId).ifPresent(colorRepository::delete);
        }
    }
}
