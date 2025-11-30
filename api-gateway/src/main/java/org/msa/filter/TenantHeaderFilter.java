package org.msa.filter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

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

            String tenant;
            String explicitTenant = exchange.getRequest().getHeaders().getFirst(c.getHeader());
            if (!explicitTenant.isEmpty()) {
                tenant = explicitTenant;
            } else {
                tenant = null;
            }

            if (tenant != null) {
                exchange.getAttributes().put(TENANT_ATTRIBUTE_KEY, tenant);

                ServerHttpRequest req = exchange.getRequest().mutate()
                        .headers(h -> h.set(c.getHeader(), tenant))
                        .build();

                return chain.filter(exchange.mutate().request(req).build());
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }, c.getOrder());
    }
}