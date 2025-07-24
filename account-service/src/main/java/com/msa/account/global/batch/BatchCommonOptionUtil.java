package com.msa.account.global.batch;

import com.msa.account.global.domain.entity.CommonOption;
import com.msa.account.global.domain.entity.GoldHarry;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Component
public class BatchCommonOptionUtil {

    public JdbcPagingItemReader<CommonOption> createReader(
            @Value("#{jobParameters['tenantId']}") String tenantId,
            @Value("#{jobParameters['goldHarryId']}") Long goldHarryId,
            @Qualifier("defaultDataSource") DataSource dataSource) {
        JdbcPagingItemReader<CommonOption> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setPageSize(100);

        //CommonOption Builder
        reader.setRowMapper((rs, rowNum) -> {
            GoldHarry goldHarry = GoldHarry.builder()
                    .goldHarryId(rs.getLong("GOLD_HARRY_ID"))
                    .build();

            return new CommonOption(
                    rs.getLong("COMMON_OPTION_ID"),
                    goldHarry,
                    rs.getString("GOLD_HARRY_LOSS")
            );
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
