package org.msa.filter;

import com.msa.common.global.jwt.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Slf4j
@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
    private final JwtUtil jwtUtil;

    public AuthorizationHeaderFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Getter @Setter
    public static class Config {
        private boolean require = true;              // false면 인증 스킵 (공개 라우트)
        private boolean verifyDevice = true;         // UA 바인딩 검증
        private boolean verifyForwardedFor = true;   // X-Forwarded-For 바인딩 검증
        private String userIdHeader = "X-User-ID";
        private int order = -1;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            if (!config.isRequire()) return chain.filter(exchange);

            String bearer = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (bearer == null || !bearer.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = bearer.substring(7);

            try {
                jwtUtil.isExpired(token);
            } catch (ExpiredJwtException e) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            if (config.isVerifyDevice()) {
                String device = jwtUtil.getDevice(token);
                String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");

                if (userAgent == null || !device.equals(userAgent)) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }

            if (config.isVerifyForwardedFor()) {
                String forward = jwtUtil.getForward(token);
                String fwdHeader = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
                String clientIp = (fwdHeader != null) ? fwdHeader.split(",")[0].trim() : "";

                if (!forward .equals(clientIp)) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }

            String userId = jwtUtil.getId(token);
            ServerWebExchange mutated = exchange.mutate().request(r -> r.headers(h -> h.set(config.getUserIdHeader(), userId))).build();
            return chain.filter(mutated);

        }, config.getOrder());
    }
}
