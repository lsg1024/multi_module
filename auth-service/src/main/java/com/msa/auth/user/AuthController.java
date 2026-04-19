package com.msa.auth.user;

import com.msa.common.global.api.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 REST 컨트롤러.
 *
 * *현재 제공 엔드포인트:
 *
 *   - {@code POST /reissue} — 만료된 액세스 토큰을 재발급하는 엔드포인트.
 *       HttpOnly 쿠키에 저장된 refresh token을 읽어 검증하며,
 *       cross-tenant 재발급 시도(요청의 {@code X-Tenant-ID}와 토큰 내 tenantId 불일치)를
 *       감지하여 Redis 토큰 삭제 및 쿠키 만료 처리 후 401을 반환한다.
 * 
 *
 * *의존성: {@link RefreshTokenService} (토큰 재발급 로직),
 * {@code jwt.access_ttl} / {@code jwt.refresh_ttl} / {@code cookie_url} 프로퍼티
 */
@Slf4j
@RestController
public class AuthController {

    @Value("${jwt.access_ttl}")
    private Long ACCESS_TTL;

    @Value("${jwt.refresh_ttl}")
    private Long REFRESH_TTL;

    @Value("${cookie_url}")
    private String COOKIE_URL;

    private final RefreshTokenService refreshTokenService;

    public AuthController(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * 액세스 토큰 재발급 엔드포인트.
     *
     * *처리 흐름:
     *
     *   - 요청 쿠키에서 {@code refreshToken} 값을 추출한다.
     *   - 쿠키가 없으면 401을 반환한다.
     *   - 요청 헤더의 {@code X-Tenant-ID}와 refresh token 내 tenantId를 비교하여
     *       불일치 시 cross-tenant 공격으로 간주하고,
     *       Redis에서 해당 refresh token을 삭제한 뒤 쿠키를 만료 처리하고 401을 반환한다.
     *   - 검증 통과 시 새 액세스 토큰을 {@code Authorization} 응답 헤더에,
     *       새 refresh token을 HttpOnly 쿠키에 설정하고 200을 반환한다.
     * 
     *
     * @param request  쿠키 및 X-Tenant-ID 헤더를 포함한 HTTP 요청
     * @param response 새 토큰 및 만료 쿠키를 설정할 HTTP 응답
     * @return 성공 또는 오류 {@link com.msa.common.global.api.ApiResponse}
     */
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<Void>> reissueToken(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refreshToken")) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Refresh token not found"));
        }

        // 요청 헤더의 X-Tenant-ID와 refreshToken의 tenantId 비교
        String requestTenantId = request.getHeader("X-Tenant-ID");
        if (requestTenantId != null && !requestTenantId.isBlank()) {
            String tokenTenantId = refreshTokenService.getTenantIdFromToken(refreshToken);
            if (tokenTenantId != null && !tokenTenantId.equals(requestTenantId)) {
                log.warn("Cross-tenant reissue 시도 감지 - 요청: {}, 토큰: {}", requestTenantId, tokenTenantId);

                // Redis에서 refresh token 삭제 (서버 측 완전 무효화)
                refreshTokenService.deleteTokenByRefreshToken(refreshToken);

                // refreshToken 쿠키 삭제
                Cookie expiredCookie = new Cookie("refreshToken", null);
                expiredCookie.setDomain(COOKIE_URL);
                expiredCookie.setMaxAge(0);
                expiredCookie.setPath("/");
                expiredCookie.setHttpOnly(true);
                response.addCookie(expiredCookie);

                // Secure 속성 없이도 한번 더 삭제 시도 (브라우저 호환성)
                Cookie expiredCookieNoSecure = new Cookie("refreshToken", null);
                expiredCookieNoSecure.setMaxAge(0);
                expiredCookieNoSecure.setPath("/");
                expiredCookieNoSecure.setHttpOnly(true);
                response.addCookie(expiredCookieNoSecure);

                return ResponseEntity.status(401).body(ApiResponse.error("Tenant mismatch: session invalidated"));
            }
        }

        String[] tokens = refreshTokenService.reissueRefreshToken(refreshToken, ACCESS_TTL, REFRESH_TTL);

        response.setHeader("Authorization", "Bearer " + tokens[0]);
        response.addCookie(createCookie(tokens[1], REFRESH_TTL));

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Cookie createCookie(String value, Long TTL) {
        Cookie cookie = new Cookie("refreshToken", value);
        cookie.setDomain(COOKIE_URL);
        cookie.setMaxAge((int) (TTL / 1000));
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }

}
