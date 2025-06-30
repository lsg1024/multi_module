package com.msa.auth.filter;

import com.msa.auth.user.UserDto;
import com.msacommon.global.jwt.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;

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

        // 2) 토큰 유효성 검사
        String user_id = jwtUtil.getId(token);
        String owner = jwtUtil.getOwner(token);
        String nickname = jwtUtil.getNickname(token);
        String role = jwtUtil.getRole(token);

        UserDto.UserInfo userInfo = new UserDto.UserInfo(
                user_id,
                owner,
                nickname,
                role
        );


        Collection<? extends GrantedAuthority> authorities = userInfo.getAuthorities();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userInfo, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
