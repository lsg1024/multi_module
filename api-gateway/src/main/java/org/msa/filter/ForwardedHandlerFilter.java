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
public class ForwardedHandlerFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

        String clientIp = (forwardedFor != null)
                ? forwardedFor.split(",")[0].trim()
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();

        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .headers(httpHeaders -> httpHeaders.add("X-Forwarded-For", clientIp))
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return -3;
    }
}
