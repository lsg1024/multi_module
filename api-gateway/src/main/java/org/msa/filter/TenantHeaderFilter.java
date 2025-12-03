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
            String reqId = exchange.getRequest().getId();
            String explicitTenant = exchange.getRequest().getHeaders().getFirst(c.getHeader());

            // [DEBUG] 진입 로그: 헤더 값 확인
            log.info("[TenantFilter] Enter. ReqID: {}, HeaderKey: {}, Value: {}", reqId, c.getHeader(), explicitTenant);

            String tenant;
            // [Fix] 기존 코드의 NPE 위험 수정: explicitTenant가 null일 경우 isEmpty() 호출 시 에러 발생함
            if (explicitTenant != null && !explicitTenant.isEmpty()) {
                tenant = explicitTenant;
            } else {
                tenant = null;
            }

            if (tenant != null) {
                // [DEBUG] 성공 로그
                log.info("[TenantFilter] Success. TenantID: {}. ReqID: {}", tenant, reqId);

                exchange.getAttributes().put(TENANT_ATTRIBUTE_KEY, tenant);

                ServerHttpRequest req = exchange.getRequest().mutate()
                        .headers(h -> h.set(c.getHeader(), tenant))
                        .build();

                return chain.filter(exchange.mutate().request(req).build());
            } else {
                // [DEBUG] 실패 로그
                log.warn("[TenantFilter] Fail: Missing TenantID. ReqID: {}", reqId);

                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }, c.getOrder());
    }
}