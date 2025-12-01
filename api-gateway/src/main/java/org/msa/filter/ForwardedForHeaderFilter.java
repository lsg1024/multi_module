package org.msa.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Slf4j
@Component
public class ForwardedForHeaderFilter extends AbstractGatewayFilterFactory<ForwardedForHeaderFilter.Config> {

    public ForwardedForHeaderFilter() {
        super(Config.class);
    }

    @Getter
    @Setter
    public static class Config {
        private String header = "X-Forwarded-For";
        private int order = -3;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            String reqId = exchange.getRequest().getId(); // 요청 추적용 ID

            // 1. 원본 데이터 조회
            String originalHeader = exchange.getRequest().getHeaders().getFirst(config.getHeader());
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            String remoteIp = (remoteAddress != null && remoteAddress.getAddress() != null)
                    ? remoteAddress.getAddress().getHostAddress()
                    : "unknown";

            // [DEBUG] 원본 상태 로깅
            log.info("[IPFilter] Enter. ReqID: {}, OriginalHeader: {}, RemoteIP: {}", reqId, originalHeader, remoteIp);

            String clientIp;

            // 2. IP 결정 로직
            if (originalHeader != null && !originalHeader.isEmpty()) {
                // 헤더가 있으면 첫 번째 IP 사용
                clientIp = originalHeader.split(",")[0].trim();
                // log.debug("[IPFilter] Strategy: Header. ReqID: {}", reqId);
            } else {
                // 헤더가 없으면 Remote Address 사용
                clientIp = remoteIp;
                log.info("[IPFilter] Strategy: RemoteAddr (No Header). ReqID: {}", reqId);
            }

            // [DEBUG] 최종 결정된 IP 로깅
            log.info("[IPFilter] Final Client IP: {}. ReqID: {}", clientIp, reqId);

            // 3. 헤더 덮어쓰기 및 체인 전달
            ServerHttpRequest req = exchange.getRequest().mutate()
                    .headers(h -> h.set(config.getHeader(), clientIp))
                    .build();

            return chain.filter(exchange.mutate().request(req).build());
        }, config.getOrder());
    }
}