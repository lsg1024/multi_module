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
                // 테넌트 불일치 → refreshToken 쿠키 삭제 후 거부
                Cookie expiredCookie = new Cookie("refreshToken", null);
                expiredCookie.setDomain(COOKIE_URL);
                expiredCookie.setMaxAge(0);
                expiredCookie.setPath("/");
                expiredCookie.setHttpOnly(true);
                response.addCookie(expiredCookie);
                return ResponseEntity.status(401).body(ApiResponse.error("Tenant mismatch"));
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
