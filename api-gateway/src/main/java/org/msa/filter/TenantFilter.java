package org.msa.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class TenantFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String host = exchange.getRequest().getHeaders().getFirst(("Host"));
        String tenantId = host != null ? host.split("\\.")[0] : null;

        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .headers(h -> h.add("X-Tenant-ID", tenantId))
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        log.info("tenantId = {}", tenantId);

        return chain.filter(mutatedExchange);
    }

    // 필터 순서 -> 작은 순으로 먼저 실행
    @Override
    public int getOrder() {
        return -1;
    }
}
