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

/**
 * API Gateway JWT 검증 필터.
 *
 * *Spring Cloud Gateway의 {@link AbstractGatewayFilterFactory}를 확장하여
 * 인바운드 요청의 Authorization 헤더에 포함된 JWT 액세스 토큰을 검증한다.
 *
 * *검증 체인:
 *
 *   - 토큰 만료 확인 — {@code ExpiredJwtException} 발생 시 401 반환
 *   - 테넌트 일치 확인 — 토큰 내 tenantId 와 요청 헤더의 {@code X-Tenant-ID} 비교
 *   - 디바이스 일치 확인 — 토큰 내 device 와 {@code User-Agent} 헤더 비교
 *   - IP 일치 확인 — 토큰 내 forward IP 와 {@code X-Forwarded-For} 헤더 비교
 *   - 모든 검증 통과 시 {@code X-User-ID} 헤더를 추가한 뒤 다음 필터로 전달
 * 
 *
 * *의존성: {@link com.msa.common.global.jwt.JwtUtil} (토큰 파싱 및 클레임 추출),
 * {@link TenantHeaderFilter} (테넌트 속성 공유)
 */
@Slf4j
@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
    private final JwtUtil jwtUtil;

    public AuthorizationHeaderFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    /**
     * 필터 동작을 제어하는 설정 클래스.
     *
     *
     *   - {@code require} — false 로 설정하면 인증을 건너뜀 (공개 경로용)
     *   - {@code verifyDevice} — User-Agent 기반 디바이스 검증 활성화 여부
     *   - {@code verifyForwardedFor} — X-Forwarded-For IP 검증 활성화 여부
     *   - {@code verifyTenant} — 테넌트 ID 일치 검증 활성화 여부
     *   - {@code userIdHeader} — 검증 성공 후 하위 서비스에 전달할 사용자 ID 헤더명
     *   - {@code order} — {@link org.springframework.cloud.gateway.filter.OrderedGatewayFilter} 실행 순서
     * 
     */
    @Getter @Setter
    public static class Config {
        private boolean require = true;
        private boolean verifyDevice = true;
        private boolean verifyForwardedFor = true;
        private boolean verifyTenant = true;
        private String userIdHeader = "X-User-ID";
        private int order = -1;
    }

    /**
     * Gateway 필터를 생성하여 JWT 검증 체인을 구성한다.
     *
     * *처리 흐름:
     *
     *   - {@code config.require == false} 이면 즉시 통과
     *   - Authorization Bearer 헤더 존재 여부 확인 → 없으면 401
     *   - {@link com.msa.common.global.jwt.JwtUtil#isExpired} 호출로 만료 확인 → 만료 시 401, 내부 오류 시 500
     *   - 테넌트 검증: 토큰 tenantId vs {@code X-Tenant-ID} 헤더 불일치 시 401
     *   - 디바이스 검증: 토큰 device vs {@code User-Agent} 불일치 시 401
     *   - IP 검증: 토큰 forward IP vs {@code X-Forwarded-For} 첫 번째 IP 불일치 시 401
     *   - 모든 검증 통과 시 userId를 {@code X-User-ID} 헤더에 추가하고 체인 계속 실행
     * 
     *
     * @param config 필터 설정 (검증 항목 활성화 플래그, 헤더명, 실행 순서)
     * @return 설정된 {@link org.springframework.cloud.gateway.filter.OrderedGatewayFilter}
     */
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

                     log.debug("[AuthFilter] Tenant Check. Token: {}, Req: {}", tokenTenantId, requestTenantId);
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