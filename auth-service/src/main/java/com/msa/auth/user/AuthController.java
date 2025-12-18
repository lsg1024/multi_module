package com.msa.auth.user;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.tenant.TenantContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
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
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("refreshToken")) {
                refreshToken = cookie.getValue();
            }
        }

        String[] tokens = refreshTokenService.reissueRefreshToken(refreshToken, ACCESS_TTL, REFRESH_TTL);

        String tenantId = TenantContext.getTenant();
        response.setHeader("Authorization", "Bearer " + tokens[0]);
        ResponseCookie cookie = createCookie("refreshToken", tokens[1], REFRESH_TTL, tenantId);
        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private ResponseCookie createCookie(String key, String value, Long TTL, String tenantId) {
        return ResponseCookie.from(key, value)
                .domain(tenantId + "." + COOKIE_URL)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(TTL / 1000)
                .build();
    }

}
