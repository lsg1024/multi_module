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
public class ForwardedForHeaderFilter extends AbstractGatewayFilterFactory<ForwardedForHeaderFilter.Config> {

    public ForwardedForHeaderFilter() { super(Config.class); }

    @Getter
    @Setter
    public static class Config {
        private String header = "X-Forwarded-For";
        private int order = -3;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            String forwardedFor = exchange.getRequest().getHeaders().getFirst(config.getHeader());
            exchange.getRequest().getRemoteAddress();

            String clientIp = (forwardedFor != null)
                    ? forwardedFor.split(",")[0].trim()
                    : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();

            ServerHttpRequest req = exchange.getRequest().mutate()
                    .headers(h -> h.set(config.getHeader(), clientIp))
                    .build();

            return chain.filter(exchange.mutate().request(req).build());
        }, config.getOrder());
    }

}
