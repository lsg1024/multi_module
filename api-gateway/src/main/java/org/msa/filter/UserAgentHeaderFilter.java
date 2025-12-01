package org.msa.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserAgentHeaderFilter extends AbstractGatewayFilterFactory<UserAgentHeaderFilter.Config> {
    public UserAgentHeaderFilter() { super(Config.class); }

    @Getter
    @Setter
    public static class Config {
        private String header = "User-Agent";
        private int order = -2;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            String reqId = exchange.getRequest().getId();
            String userAgent = exchange.getRequest().getHeaders().getFirst(config.getHeader());

            // [DEBUG] 진입 로그
            log.info("[UserAgentFilter] Enter. ReqID: {}, Original User-Agent: {}", reqId, userAgent);

            String finalUserAgent;

            // [Safety] 헤더가 없을 경우 NPE 방지 및 기본값 설정
            if (userAgent != null && !userAgent.isEmpty()) {
                finalUserAgent = userAgent;
            } else {
                log.warn("[UserAgentFilter] Warning: Missing User-Agent header. Setting to 'Unknown'. ReqID: {}", reqId);
                finalUserAgent = "Unknown";
            }

            // 헤더 재설정 (Downstream 서비스로 전달)
            ServerHttpRequest req = exchange.getRequest().mutate()
                    .headers(h -> h.set(config.getHeader(), finalUserAgent))
                    .build();

            log.info("[UserAgentFilter] Success. Final User-Agent: {}. ReqID: {}", finalUserAgent, reqId);

            return chain.filter(exchange.mutate().request(req).build());
        }, config.getOrder());
    }
}