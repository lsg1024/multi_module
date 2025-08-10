package org.msa.filter;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class TenantHeaderFilter extends AbstractGatewayFilterFactory<TenantHeaderFilter.Config> {

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
            String host = exchange.getRequest().getHeaders().getFirst(HttpHeaders.HOST);
            String hostname = host != null ? host.split(":")[0] : "";
            String tenant;
            if (c.isDeriveFromHost() && hostname.contains(".")) {
                tenant = hostname.split("\\.")[0];
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