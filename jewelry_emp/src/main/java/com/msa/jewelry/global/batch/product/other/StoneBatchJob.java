package com.msa.jewelry.global.batch.product.other;

import com.msa.jewelry.local.stone.dto.StoneDto;
import com.msa.jewelry.local.stone.dto.StoneWorkGradePolicyDto;
import com.msa.jewelry.local.stone.entity.Stone;
import com.msa.jewelry.local.stone.entity.StoneWorkGradePolicy;
import com.msa.jewelry.local.stone.repository.StoneRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        // 처리 정책:
        //   ① 같은 stoneName 의 Stone 이 없으면 → 새 Stone + 정책 전부 생성 (기존 흐름)
        //   ② 같은 stoneName 이 이미 있으면 → 누락된 grade 의 policy 만 add (backfill 용도)
        //   ③ 추가할 policy 가 0건이면 return null (chunk skip)
        return dto -> {
            // dto → 후보 policy 리스트 변환
            List<StoneWorkGradePolicy> candidatePolicies = buildPoliciesFromDto(dto);

            Stone existing = stoneRepository.findByStoneNameIgnoreCase(dto.getStoneName()).orElse(null);
            if (existing != null) {
                if (candidatePolicies.isEmpty()) {
                    return null;
                }
                // 이미 등록된 grade 는 건너뛰고 누락된 것만 추가
                Set<String> existingGrades = existing.getGradePolicies().stream()
                        .map(p -> p.getGrade().name())
                        .collect(Collectors.toCollection(HashSet::new));

                boolean changed = false;
                for (StoneWorkGradePolicy np : candidatePolicies) {
                    if (existingGrades.add(np.getGrade().name())) {
                        existing.addGradePolicy(np);
                        changed = true;
                    }
                }
                return changed ? existing : null;
            }

            // 신규 Stone — 기존 로직과 동일
            BigDecimal weight = parseWeight(dto.getStoneWeight());
            Integer purchasePrice = parsePurchasePrice(dto.getStonePurchasePrice());

            Stone stone = Stone.builder()
                    .stoneName(dto.getStoneName())
                    .stoneNote(Optional.ofNullable(dto.getStoneNote()).orElse(""))
                    .stoneWeight(weight)
                    .stonePurchasePrice(purchasePrice)
                    .gradePolicies(new ArrayList<>())
                    .build();

            candidatePolicies.forEach(stone::addGradePolicy);
            return stone;
        };
    }

    /** stoneWorkGradePolicyDto 배열을 StoneWorkGradePolicy 엔티티 후보로 변환 (stone 미연결 상태). */
    private static List<StoneWorkGradePolicy> buildPoliciesFromDto(StoneDto dto) {
        List<StoneWorkGradePolicy> result = new ArrayList<>();
        if (dto.getStoneWorkGradePolicyDto() == null) {
            return result;
        }
        for (StoneWorkGradePolicyDto p : dto.getStoneWorkGradePolicyDto()) {
            int labor = parseLaborCost(p.getLaborCost());
            result.add(StoneWorkGradePolicy.builder()
                    .grade(p.getGrade())
                    .laborCost(labor)
                    .build());
        }
        return result;
    }

    private static BigDecimal parseWeight(Object raw) {
        if (raw == null || raw.toString().isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(raw.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static Integer parsePurchasePrice(Object raw) {
        if (raw == null) return 0;
        if (raw instanceof Number) return ((Number) raw).intValue();
        if (raw.toString().isBlank()) return 0;
        try {
            return (int) Double.parseDouble(raw.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseLaborCost(Object raw) {
        if (raw == null) return 0;
        if (raw instanceof Number) return ((Number) raw).intValue();
        if (raw instanceof String s && !s.isBlank()) {
            try {
                return (int) Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
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