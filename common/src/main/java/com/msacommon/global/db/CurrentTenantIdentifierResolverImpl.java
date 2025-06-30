package com.msacommon.global.db;

import com.msacommon.global.tenant.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getTenant();
        return (tenant != null && !tenant.isEmpty()) ? tenant : "public";
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}