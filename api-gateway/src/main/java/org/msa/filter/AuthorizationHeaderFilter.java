package org.msa.filter;

import com.msacommon.global.jwt.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
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

//        String path = exchange.getRequest().getURI().getPath();
//
//        if (path.startsWith("/eureka") || path.startsWith("/actuator") || path.startsWith("/auth/reissue") || path.startsWith("/auth/login")) {
//            return chain.filter(exchange);
//        }
//
//        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
//
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            return exchange.getResponse().setComplete();
//        }
//
//        String token = authHeader.substring(7); // "Bearer " 제거
//
//        try {
//            if (jwtUtil.isExpired(token)) {
//                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//                return exchange.getResponse().setComplete();
//            }
//
//            String userId = jwtUtil.getUserId(token);
//
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(builder -> builder.header("X-User-ID", "test"))
                    .build();
//
//            return chain.filter(mutatedExchange);
//        } catch (Exception e) {
//            log.error("JWT 검증 실패: {}", e.getMessage());
//            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            return exchange.getResponse().setComplete();
//        }

        return chain.filter(mutatedExchange);

    }

    @Override
    public int getOrder() {
        return -4;
    }
}
