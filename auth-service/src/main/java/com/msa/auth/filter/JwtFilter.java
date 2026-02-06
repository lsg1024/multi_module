package com.msa.auth.filter;

import com.msa.common.global.domain.dto.UserDto;
import com.msa.common.global.jwt.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1) 헤더에서 토큰 추출
        String bearer = request.getHeader("Authorization");
        String token = null;
        if (bearer != null && bearer.startsWith("Bearer ")) {
            token = bearer.substring(7);
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtUtil.isExpired(token);
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String category = jwtUtil.getCategory(token);

        if (!category.equals("access")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String device = jwtUtil.getDevice(token);
        String H_userAgent = request.getHeader("User-Agent");

        if (H_userAgent == null || !H_userAgent.equals(device)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String forward = jwtUtil.getForward(token);
        String forwardHeader = request.getHeader("X-Forwarded-For");
        String H_forwarded = (forwardHeader != null) ? forwardHeader.split(",")[0].trim() : request.getRemoteAddr();

        if (!H_forwarded.equals(forward)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String user_id = jwtUtil.getId(token);
        String tenantId = jwtUtil.getTenantId(token);
        String nickname = jwtUtil.getNickname(token);
        String role = jwtUtil.getRole(token);
        Long storeId = jwtUtil.getStoreId(token);

        UserDto.UserInfo userInfo = UserDto.UserInfo.builder()
                .userId(user_id)
                .tenantId(tenantId)
                .nickname(nickname)
                .role(role)
                .storeId(storeId)
                .build();

        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(userInfo.getRole()));
        Authentication authentication = new UsernamePasswordAuthenticationToken(userInfo, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
