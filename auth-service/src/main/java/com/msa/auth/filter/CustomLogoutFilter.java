package com.msa.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.auth.redis.RedisRefreshTokenService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

public class CustomLogoutFilter extends GenericFilterBean {

    private final String cookieUrl;
    private final JwtUtil jwtUtil;
    private final RedisRefreshTokenService redisRefreshTokenService;

    public CustomLogoutFilter(String cookieUrl, JwtUtil jwtUtil, RedisRefreshTokenService redisRefreshTokenService) {
        this.cookieUrl = cookieUrl;
        this.jwtUtil = jwtUtil;
        this.redisRefreshTokenService = redisRefreshTokenService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        String requestUri = request.getRequestURI();

        if (!requestUri.equals("/logout")) {
            filterChain.doFilter(request, response);
            return;
        }
        String requestMethod = request.getMethod();
        if (!requestMethod.equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("refreshToken")) {
                refreshToken = cookie.getValue();
            }
        }

        if (refreshToken == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String category = jwtUtil.getCategory(refreshToken);
        if (!category.equals("refresh")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String owner = jwtUtil.getTenantId(refreshToken);
        String nickname = jwtUtil.getNickname(refreshToken);
        boolean isExist = redisRefreshTokenService.existsToken(owner, nickname);
        if (!isExist) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        redisRefreshTokenService.deleteToken(owner, nickname);

        Cookie cookie = createCookie("refreshToken", refreshToken, 0L);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(
                ApiResponse.success("로그아웃 성공")
        );
        response.addCookie(cookie);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(jsonResponse);
    }

    private Cookie createCookie(String key, String value, Long TTL) {
        Cookie cookie = new Cookie(key, value);
        cookie.setDomain(cookieUrl);
        cookie.setMaxAge(TTL.intValue());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }
}
