package org.msa.filter;

import com.msacommon.global.jwt.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthorizationHeaderFilter implements GlobalFilter, Ordered {
    private final JwtUtil jwtUtil;

    public AuthorizationHeaderFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        if (path.startsWith("/eureka") || path.startsWith("/actuator/**") || path.startsWith("/auth/reissue") || path.startsWith("/auth/login") || path.startsWith("/users/login") || path.startsWith("/users/signup")) {
            return chain.filter(exchange);
        }

        String bearer = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (bearer == null || !bearer.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = bearer.substring(7); // "Bearer " 제거

        try {
            jwtUtil.isExpired(token);
        } catch (ExpiredJwtException e) {
            log.error("JWT 검증 실패: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String device = jwtUtil.getDevice(token);
        String deviceHeader = exchange.getRequest().getHeaders().getFirst("User-Agent");

        log.info("Device device {} deviceHeader {}", device, deviceHeader);

        if (!device.equals(deviceHeader)) {
            log.error("Device 불일치");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String forward = jwtUtil.getForward(token);
        String forwardHeader = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        String clientIp = forwardHeader.split(",")[0].trim();

        log.info("Forward forward {} forwardHeader {}", forward, clientIp);

        if (!forward.equals(clientIp)) {
            log.error("X-Forwarded-For 불일치");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String userId = jwtUtil.getId(token);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(builder -> builder.header("X-User-ID", userId))
                .build();

        return chain.filter(mutatedExchange);

    }

    @Override
    public int getOrder() {
        return -1;
    }

}
