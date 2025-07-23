package com.msa.account.global.batch;

import com.msa.account.global.domain.entity.CommonOption;
import com.msa.account.global.domain.entity.GoldHarry;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DeleteGoldHarryBatchJob {

    @Bean
    public Job deleteGoldHarryJob(JobRepository jobRepository, Step deleteGoldHarryStep) {
        return new JobBuilder("deleteGoldHarryJob", jobRepository)
                .start(deleteGoldHarryStep)
                .build();
    }

    @Bean
    public Step deleteGoldHarryStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    JdbcPagingItemReader<GoldHarry> reader,
                                    ItemProcessor<GoldHarry, GoldHarry> processor,
                                    JdbcBatchItemWriter<GoldHarry> writer) {
        return new StepBuilder("deleteGoldHarryStep", jobRepository)
                .chunk(100, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<GoldHarry> goldHarryReader(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Value("#{jobParameters['goldHarryId']}") String goldHarryId,
            @Qualifier("defaultDataSource") DataSource dataSource) {

        JdbcPagingItemReader<GoldHarry> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setPageSize(100);

        // 헤리값을 삭제하고 기본 값으로 전부 변경하는 코드 추가 차라리 업데이트 코드에 추가를할까?
        reader.setRowMapper((rs, rowNum) -> {
            GoldHarry goldHarry = GoldHarry.builder()
                    .goldHarryId(rs.getLong("GOLD_HARRY_ID"))
                    .build();

//            return CommonOption.builder()
//                    .commonOptionId(rs.getString("COMMON_OPTION_ID"))
//                    .goldHarryLoss(rs.getString("GOLD_HARRY_LOSS"))
//                    .goldHarry(goldHarry)
//                    .build();
        });

        MySqlPagingQueryProvider provider = new MySqlPagingQueryProvider();
        provider.setSelectClause("SELECT co.*");
        provider.setFromClause(tenantId + ".COMMON_OPTION co " +
                "JOIN " + tenantId + ".GOLD_HARRY gh ON gh.GOLD_HARRY_ID = co.GOLD_HARRY_ID");
        provider.setWhereClause("gh.GOLD_HARRY_ID = :goldHarryId");

        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("COMMON_OPTION_ID", Order.ASCENDING);
        provider.setSortKeys(sortKeys);

        reader.setQueryProvider(provider);

        // 파라미터
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("goldHarryId", goldHarryId);
        reader.setParameterValues(parameterValues);

        return reader;
    }
}
