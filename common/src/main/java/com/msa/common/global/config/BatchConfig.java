package com.msa.common.global.config;

import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Profile("dev")
@Configuration
public class BatchConfig extends DefaultBatchConfiguration {

    private final DataSource metaDataSource;
    private final PlatformTransactionManager metaTransactionManager;

    // MetaDBConfig에서 등록한 Bean들을 주입받습니다.
    public BatchConfig(
            @Qualifier("metaDataSource") DataSource metaDataSource,
            @Qualifier("metaTransactionManager") PlatformTransactionManager metaTransactionManager) {
        this.metaDataSource = metaDataSource;
        this.metaTransactionManager = metaTransactionManager;
    }

    @Override
    protected DataSource getDataSource() {
        // Spring Batch가 메타데이터(BATCH_JOB_INSTANCE 등) 관리에 사용할 DB를 지정
        return metaDataSource;
    }

    @Override
    protected PlatformTransactionManager getTransactionManager() {
        // Spring Batch가 메타데이터 관리에 사용할 트랜잭션 매니저 지정
        return metaTransactionManager;
    }
}