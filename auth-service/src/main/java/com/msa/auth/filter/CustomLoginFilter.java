package com.msa.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.auth.redis.RedisRefreshTokenService;
import com.msa.auth.user.UserDto;
import com.msa.auth.user.UserServerClient;
import com.msacommon.global.api.ApiResponse;
import com.msacommon.global.jwt.JwtUtil;
import feign.FeignException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;

@Slf4j
public class CustomLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final long accessTtl;
    private final long refreshTtl;
    private final JwtUtil jwtUtil;
    private final RedisRefreshTokenService redisRefreshTokenService;

    private final UserServerClient userServerClient;

    public CustomLoginFilter(AuthenticationManager authenticationManager, long accessTtl, long refreshTtl, JwtUtil jwtUtil, RedisRefreshTokenService redisRefreshTokenService, UserServerClient userServerClient) {
        super.setAuthenticationManager(authenticationManager);
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
        this.jwtUtil = jwtUtil;
        this.redisRefreshTokenService = redisRefreshTokenService;
        this.userServerClient = userServerClient;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        log.info("attemptAuthentication start");

        UserDto.Login loginDto;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ServletInputStream inputStream = request.getInputStream();
            String body = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            loginDto = objectMapper.readValue(body, UserDto.Login.class);

            log.info("attemptAuthentication Body {}", loginDto.getUser_id());

            ApiResponse<UserDto.UserInfo> result;

            try {
                result = userServerClient.getLoginCheck(loginDto).getBody();
            } catch (FeignException.BadRequest e) {
                // 로그인 정보 오류로 간주 → 401로 매핑
                throw new BadCredentialsException("아이디/비밀번호가 일치하지 않습니다.", e);
            } catch (FeignException e) {
                // 기타 Feign 에러(5xx 등)는 서비스 장애로 간주
                throw new AuthenticationServiceException("인증 서버 장애가 발생했습니다.", e);
            }

            UserDto.UserInfo userinfo = result.getData();
            Collection<? extends GrantedAuthority> authorities = userinfo.getAuthorities();

            return new UsernamePasswordAuthenticationToken(userinfo, null, authorities);
        } catch (IOException e) {
            throw new RuntimeException("Json 형식 오류");
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) {

        UserDto.UserInfo userInfo = (UserDto.UserInfo) authentication.getPrincipal();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        // 토큰 생성
        String accessToken = jwtUtil.createJwt("access", userInfo.getUser_id(), userInfo.getOwner(), userInfo.getNickname(), role, accessTtl);
        String refreshToken = jwtUtil.createJwt("refresh", userInfo.getUser_id(), userInfo.getOwner(), userInfo.getNickname(), role, refreshTtl);

        // 리프레시 토큰 DB 저장
        redisRefreshTokenService.createNewToken(userInfo.getOwner(), userInfo.getNickname(), refreshToken);

        // 응답 헤더 및 쿠키 설정
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.setHeader("Authorization", "Bearer " + accessToken);
        response.addCookie(createCookie("refreshToken", refreshToken, refreshTtl));
        response.setStatus(HttpStatus.OK.value());
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {

        HttpStatus errorStatus;
        if (failed instanceof BadCredentialsException) {
            errorStatus = HttpStatus.UNAUTHORIZED;          // 401
        } else if (failed instanceof AuthenticationServiceException) {
            errorStatus = HttpStatus.INTERNAL_SERVER_ERROR; // 500
        } else {
            errorStatus = HttpStatus.FORBIDDEN;
        }

        response.setStatus(errorStatus.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> error = ApiResponse.error(failed.getMessage());
        String body = new ObjectMapper().writeValueAsString(error);
        response.getWriter().write(body);
    }

    private Cookie createCookie(String key, String value, Long TTL) {
            Cookie cookie = new Cookie(key, value);
            cookie.setMaxAge(TTL.intValue());
            cookie.setPath("/");
            cookie.setHttpOnly(true);
        return cookie;
    }

}
