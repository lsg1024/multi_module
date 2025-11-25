package org.msa.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
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
        return new OrderedGatewayFilter((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            String tenant = exchange.getAttributeOrDefault(TenantHeaderFilter.TENANT_ATTRIBUTE_KEY, "unknown");

            log.info("[{}] Request: {} {}", tenant, request.getMethod(), request.getURI());

            return chain.filter(exchange)
                    .doOnSuccess(aVoid ->
                            log.info("[{}] Response OK: {}", tenant, request.getURI())
                    )
                    .doOnError(ex ->
                            log.error("[{}] Error: {}", tenant, ex.getMessage())
                    );
        }, -1);
    }
}
