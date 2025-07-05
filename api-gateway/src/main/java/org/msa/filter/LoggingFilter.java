package org.msa.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingFilter extends AbstractGatewayFilterFactory<Object> {

    public LoggingFilter() {
        super(Object.class);
    }
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            log.info("Request: {} {}", request.getMethod(), request.getURI());

            return chain.filter(exchange)
                    .doOnSuccess(aVoid -> log.info("Response OK: {}", request.getURI()))
                    .doOnError(ex -> log.error("Error: {}", ex.getMessage()));
        };
    }

}
