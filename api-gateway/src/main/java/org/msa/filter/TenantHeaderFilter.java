package org.msa.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class TenantHeaderFilter extends AbstractGatewayFilterFactory<TenantHeaderFilter.Config> {

    public static final String TENANT_ATTRIBUTE_KEY = "TENANT_ID";

    public TenantHeaderFilter() { super(Config.class); }

    @Getter @Setter
    public static class Config {
        private String header = "X-Tenant-ID";
        private boolean deriveFromHost = true;
        private int order = -4;
    }

    @Override
    public GatewayFilter apply(Config c) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            String tenant = null;

            log.info("TenantHeaderFilter start");
            if (c.isDeriveFromHost()) {
                String host = exchange.getRequest().getHeaders().getFirst("Host");
                log.info("TenantHeaderFilter host = {}", host);
                if (StringUtils.hasText(host)) {
                    if (host.contains(":")) {
                        host = host.split(":")[0];
                    }
                    log.info("TenantHeaderFilter StringUtils.hasText(host) = {}", host);
                    String[] parts = host.split("\\.");
                    if (parts.length >= 1) {
                        for (String part : parts) {
                            log.info("TenantHeaderFilter parts data = {}", part);
                        }
                        tenant = parts[0];
                        log.info("TenantHeaderFilter parts.length >= 1 = {}", tenant);
                    } else {
                        log.debug("Host does not contain subdomain: {}", host);
                    }
                }
            }

            if (!StringUtils.hasText(tenant)) {
                String explicitTenant = exchange.getRequest().getHeaders().getFirst(c.getHeader());
                log.info("TenantHeaderFilter explicitTenant = {}", explicitTenant);
                if (StringUtils.hasText(explicitTenant)) {
                    tenant = explicitTenant;
                }
            }

            if (StringUtils.hasText(tenant)) {
                exchange.getAttributes().put(TENANT_ATTRIBUTE_KEY, tenant);
                String finalTenant = tenant;
                log.info("TenantHeaderFilter TENANT_ATTRIBUTE_KEY = {}", tenant);
                ServerHttpRequest req = exchange.getRequest().mutate()
                        .headers(h -> h.set(c.getHeader(), finalTenant))
                        .build();

                log.info("TenantHeaderFilter finalTenant = {}", finalTenant);
                return chain.filter(exchange.mutate().request(req).build());
            } else {
                log.warn("Missing Tenant Information. Access Denied.");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }, c.getOrder());
    }
}