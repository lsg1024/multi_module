package org.msa.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
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
            String userAgent = exchange.getRequest().getHeaders().getFirst(config.getHeader());
            var req = exchange.getRequest().mutate()
                    .headers(h -> h.set(config.getHeader(), userAgent))
                    .build();
            return chain.filter(exchange.mutate().request(req).build());
        }, config.getOrder());
    }
}
