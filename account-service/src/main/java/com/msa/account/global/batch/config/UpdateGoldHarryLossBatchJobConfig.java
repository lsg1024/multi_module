package com.msa.account.global.batch.config;


import com.msa.account.global.domain.entity.CommonOption;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.exception.NotFoundException;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class UpdateGoldHarryLossBatchJobConfig {

    private final EntityManagerFactory emf;
    private final GoldHarryRepository goldHarryRepository;

    @Bean
    public Job updateStoreGoldHarryLossJob(JobRepository jobRepository, Step updateGoldHarryLossStep) {
        return new JobBuilder("updateGoldHarryLossJob", jobRepository)
                .start(updateGoldHarryLossStep)
                .build();
    }

    @Bean
    public Step updateGoldHarryLossStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager,
                                        JpaPagingItemReader<CommonOption> reader,
                                        ItemProcessor<CommonOption, CommonOption> processor,
                                        JpaItemWriter<CommonOption> writer) {
        return new StepBuilder("updateGoldHarryLossStep", jobRepository)
                .<CommonOption, CommonOption>chunk(100, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<CommonOption> commonOptionReader(
            @Value("#{jobParameters['goldHarryId']}") Long goldHarryId) {

        JpaPagingItemReader<CommonOption> reader = new JpaPagingItemReader<>();
        reader.setEntityManagerFactory(emf);
        reader.setQueryString("SELECT c FROM CommonOption c WHERE c.goldHarry.goldHarryId = :goldHarryId");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("goldHarryId", goldHarryId);
        reader.setParameterValues(parameters);
        reader.setPageSize(100);

        return reader;
    }
    @Bean
    @StepScope
    public ItemProcessor<CommonOption, CommonOption> updateGoldHarryLossProcessor(
            @Value("#{jobParameters['updatedGoldHarryLoss']}") String updatedGoldHarryLoss,
            @Value("#{jobParameters['goldHarryId']}") Long goldHarryId) {

        return option -> {
            if (!option.getGoldHarry().getGoldHarryId().equals(goldHarryId)) {
                GoldHarry newGoldHarry = goldHarryRepository.findById(goldHarryId)
                        .orElseThrow(() -> new NotFoundException("GoldHarry not found"));

                option.updateGoldHarry(newGoldHarry);
            } else {
                option.updateGoldHarryLoss(updatedGoldHarryLoss);
            }

            return option;
        };
    }

    @Bean
    public JpaItemWriter<CommonOption> commonOptionWriter() {
        JpaItemWriter<CommonOption> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);
        return writer;
    }
}