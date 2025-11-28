package org.msa.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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

            // 1. 요청 ID 추적 (없으면 생성)
            String requestId = request.getHeaders().getFirst("X-Request-Id");
            // 변조된 요청 객체 생성 (헤더 추가)
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Request-Id", requestId)
                    .build();

            // 2. 시작 시간 기록
            long startTime = System.currentTimeMillis();

            log.info("[{}] Request: {} {}", tenant, mutatedRequest.getMethod(), mutatedRequest.getURI());

            // 3. 필터 체인 실행
            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                    .then(Mono.fromRunnable(() -> {
                        ServerHttpResponse response = exchange.getResponse();
                        HttpStatusCode status = response.getStatusCode();
                        long duration = System.currentTimeMillis() - startTime;

                        if (status.isError()) {
                            log.error("[{}] Response FAILED: {} | Status: {} | Time: {}ms",
                                    tenant, mutatedRequest.getURI(), status, duration);
                        } else {
                            // 정상일 경우 Info 레벨
                            log.info("[{}] Response OK: {} | Status: {} | Time: {}ms",
                                    tenant, mutatedRequest.getURI(), status, duration);
                        }
                    }));
        }, -1); // 순서를 가장 앞(-1)으로 유지
    }
}