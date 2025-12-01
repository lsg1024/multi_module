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
        private boolean require = true;
        private boolean verifyDevice = true;
        private boolean verifyForwardedFor = true;
        private boolean verifyTenant = true;
        private String userIdHeader = "X-User-ID";
        private int order = -1;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            String reqId = exchange.getRequest().getId();

            // [DEBUG] 1. 필터 진입
            log.info("[AuthFilter] Enter. ReqID: {}, Path: {}, Require: {}", reqId, path, config.isRequire());

            if (!config.isRequire()) {
                log.info("[AuthFilter] Pass (Auth not required). ReqID: {}", reqId);
                return chain.filter(exchange);
            }

            String bearer = exchange.getRequest().getHeaders().getFirst("Authorization");

            // [DEBUG] 2. 헤더 존재 확인
            if (bearer == null || !bearer.startsWith("Bearer ")) {
                log.error("[AuthFilter] Fail: No Bearer Token. ReqID: {}", reqId);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = bearer.substring(7);

            try {
                // [DEBUG] 3. 만료 체크 시작
                // log.debug("[AuthFilter] Checking Expiration. ReqID: {}", reqId); // 너무 많으면 주석 처리
                jwtUtil.isExpired(token);
                // [DEBUG] 4. 만료 체크 통과
            } catch (ExpiredJwtException e) {
                log.error("[AuthFilter] Fail: Token Expired. ReqID: {}, Err: {}", reqId, e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            } catch (Exception e) {
                // [중요] JwtUtil 내부의 다른 에러(Redis 연결 등)를 잡기 위함
                log.error("[AuthFilter] Fail: Unexpected Error in JwtUtil. ReqID: {}", reqId, e);
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR); // 500 처리
                return exchange.getResponse().setComplete();
            }

            // Tenant 검증
            if (config.isVerifyTenant()) {
                try {
                    String tokenTenantId = jwtUtil.getTenantId(token);
                    String requestTenantId = exchange.getAttribute(TenantHeaderFilter.TENANT_ATTRIBUTE_KEY);

                    if (requestTenantId == null) {
                        requestTenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
                    }

                    // log.debug("[AuthFilter] Tenant Check. Token: {}, Req: {}", tokenTenantId, requestTenantId);

                    if (tokenTenantId != null && !tokenTenantId.equals(requestTenantId)) {
                        log.warn("[AuthFilter] Fail: Tenant Mismatch! ReqID: {}, TokenT: {}, ReqT: {}", reqId, tokenTenantId, requestTenantId);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                } catch (Exception e) {
                    log.error("[AuthFilter] Error during Tenant Check. ReqID: {}", reqId, e);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }

            // Device 검증
            if (config.isVerifyDevice()) {
                try {
                    String device = jwtUtil.getDevice(token);
                    String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");

                    if (userAgent == null || !device.equals(userAgent)) {
                        log.warn("[AuthFilter] Fail: Device Mismatch. ReqID: {}", reqId);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                } catch (Exception e) {
                    log.error("[AuthFilter] Error during Device Check. ReqID: {}", reqId, e);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }

            // IP 검증
            if (config.isVerifyForwardedFor()) {
                try {
                    String forward = jwtUtil.getForward(token);
                    String fwdHeader = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
                    String clientIp = (fwdHeader != null) ? fwdHeader.split(",")[0].trim() : "";

                    if (!forward.equals(clientIp)) {
                        log.warn("[AuthFilter] Fail: IP Mismatch. ReqID: {}, TokenIP: {}, RealIP: {}", reqId, forward, clientIp);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                } catch (Exception e) {
                    log.error("[AuthFilter] Error during IP Check. ReqID: {}", reqId, e);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }

            try {
                String userId = jwtUtil.getId(token);
                // [DEBUG] 5. 최종 성공 및 헤더 주입
                log.info("[AuthFilter] Success. ReqID: {}, UserID: {}. Forwarding to Service.", reqId, userId);

                ServerWebExchange mutated = exchange.mutate().request(r -> r.headers(h -> h.set(config.getUserIdHeader(), userId))).build();
                return chain.filter(mutated);
            } catch (Exception e) {
                log.error("[AuthFilter] Fail: Error extracting UserID. ReqID: {}", reqId, e);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

        }, config.getOrder());
    }
}