package com.msa.account.global.batch;

import com.msa.common.global.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TenantAwareJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String tenantId = jobExecution.getJobParameters().getString("tenantId");

        if (tenantId != null) {
            log.info("Batch Job Start: Setting TenantContext to [{}]", tenantId);
            TenantContext.setTenant(tenantId);
        } else {
            log.warn("Batch Job Start: No tenantId found in JobParameters");
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("Batch Job End: Clearing TenantContext");
        TenantContext.clear();
    }
}
