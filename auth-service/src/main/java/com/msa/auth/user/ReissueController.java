package com.msa.auth.user;

import com.msacommon.global.api.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ReissueController {

    @Value("${jwt.access_ttl}")
    private Long ACCESS_TTL;

    @Value("${jwt.refresh_ttl}")
    private Long REFRESH_TTL;

    private final ReissueService reissueService;

    public ReissueController(ReissueService reissueService) {
        this.reissueService = reissueService;
    }

    @GetMapping("/test/hello")
    public String hello(@RequestHeader("X-Tenant-ID") String tenantId, HttpServletResponse response) {
        log.info("hello log {}", tenantId);
        response.addHeader("X-Tenant-ID", tenantId);
        return "Auth-Server";
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<Void>> reissueToken(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = null;

        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("refreshToken")) {
                refreshToken = cookie.getValue();
                log.info("cookie refreshToken {}", refreshToken);
            }
        }

        String[] tokens = reissueService.reissueRefreshToken(refreshToken, ACCESS_TTL, REFRESH_TTL);

        response.setHeader("Authorization", "Bearer " + tokens[0]);
        response.addCookie(createCookie("refreshToken", tokens[1], REFRESH_TTL));

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Cookie createCookie(String key, String value, Long TTL) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(TTL.intValue());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }

}
