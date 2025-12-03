package com.msa.product.global.batch;

import com.msa.product.local.stone.stone.dto.StoneDto;
import com.msa.product.local.stone.stone.dto.StoneWorkGradePolicyDto;
import com.msa.product.local.stone.stone.entity.Stone;
import com.msa.product.local.stone.stone.entity.StoneWorkGradePolicy;
import com.msa.product.local.stone.stone.repository.StoneRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

@Configuration
public class StoneBatchJob {

    @Bean
    @StepScope
    public JsonItemReader<StoneDto> stoneJsonReader(
            @Value("#{jobParameters['filePath']}") String filePath) {

        return new JsonItemReaderBuilder<StoneDto>()
                .name("stoneJsonReader")
                .jsonObjectReader(new JacksonJsonObjectReader<>(StoneDto.class))
                .resource(new FileSystemResource(filePath))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<StoneDto, Stone> stoneProcessor(StoneRepository stoneRepository) {
        return dto -> {
            if (stoneRepository.existsByStoneName(dto.getStoneName())) {
                return null;
            }

            BigDecimal weight = BigDecimal.ZERO;
            if (dto.getStoneWeight() != null && !dto.getStoneWeight().toString().isBlank()) {
                try {
                    weight = new BigDecimal(dto.getStoneWeight().toString());
                } catch (NumberFormatException e) {
                    weight = BigDecimal.ZERO;
                }
            }

            Integer purchasePrice = 0;
            if (dto.getStonePurchasePrice() != null) {
                if (dto.getStonePurchasePrice() instanceof Number) {
                    purchasePrice = ((Number) dto.getStonePurchasePrice()).intValue();
                } else if (!dto.getStonePurchasePrice().toString().isBlank()){
                    try {
                        purchasePrice = (int) Double.parseDouble(dto.getStonePurchasePrice().toString());
                    } catch (NumberFormatException e) {
                        purchasePrice = 0;
                    }
                }
            }

            Stone stone = Stone.builder()
                    .stoneName(dto.getStoneName())
                    .stoneNote(Optional.ofNullable(dto.getStoneNote()).orElse(""))
                    .stoneWeight(weight)
                    .stonePurchasePrice(purchasePrice)
                    .gradePolicies(new ArrayList<>())
                    .build();

            // 4. gradePolicies 변환
            if (dto.getStoneWorkGradePolicyDto() != null) {
                for (StoneWorkGradePolicyDto p : dto.getStoneWorkGradePolicyDto()) {

                    int labor = 0;
                    Object costObj = p.getLaborCost();

                    if (costObj != null) {
                        if (costObj instanceof Number) {
                            labor = ((Number) costObj).intValue();
                        } else if (costObj instanceof String && !((String) costObj).isBlank()) {
                            try {
                                labor = (int) Double.parseDouble((String) costObj);
                            } catch (NumberFormatException e) {
                                labor = 0;
                            }
                        }
                    }

                    stone.addGradePolicy(
                            StoneWorkGradePolicy.builder()
                                    .grade(p.getGrade())
                                    .laborCost(labor)
                                    .build()
                    );
                }
            }
            return stone;
        };
    }

    @Bean
    @StepScope
    public ItemWriter<Stone> stoneWriter(StoneRepository stoneRepository) {
        return stoneRepository::saveAll;
    }

    @Bean
    public Step stoneInsertStep(
            JobRepository jobRepository,
            PlatformTransactionManager tx,
            JsonItemReader<StoneDto> reader,
            ItemProcessor<StoneDto, Stone> processor,
            ItemWriter<Stone> writer
    ) {
        return new StepBuilder("stoneInsertStep", jobRepository)
                .<StoneDto, Stone>chunk(100, tx)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job stoneInsertJob(JobRepository jobRepository, Step stoneInsertStep) {
        return new JobBuilder("stoneInsertJob", jobRepository)
                .start(stoneInsertStep)
                .build();
    }
}