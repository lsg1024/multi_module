package com.msa.auth.config;

import com.msa.auth.filter.CustomLoginFilter;
import com.msa.auth.filter.CustomLogoutFilter;
import com.msa.auth.filter.JwtFilter;
import com.msa.auth.redis.RedisRefreshTokenService;
import com.msa.auth.user.UserFeignClient;
import com.msa.common.global.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${jwt.access_ttl}")
    private Long ACCESS_TTL;

    @Value("${jwt.refresh_ttl}")
    private Long REFRESH_TTL;

    @Value("${cookie_url}")
    private String COOKIE_URL;

    private final JwtUtil jwtUtil;
    private final RedisRefreshTokenService redisService;
    private final UserFeignClient userFeignClient;
    private final AuthenticationConfiguration authConfig;

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 1) 세션, CSRF 비활성화
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        // 2) 인증·재발급 경로만 허용, 그 외 보호
        http
                .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/login", "/reissue").permitAll()
                .anyRequest().authenticated()
        );

        // 3) 에러 페이지 핸들링
        http
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, exDenied) ->
                                res.sendError(HttpServletResponse.SC_FORBIDDEN)));

        // 4) 커스텀 로그인 필터 — /auth/login
        CustomLoginFilter loginFilter = new CustomLoginFilter(
                authenticationManager(), ACCESS_TTL, REFRESH_TTL, COOKIE_URL, jwtUtil, redisService, userFeignClient
        );
        loginFilter.setFilterProcessesUrl("/login");

        http
                .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

        // 5) JWT 검사 필터 — 로그인 이후 모든 요청에서
        http
                .addFilterBefore(new JwtFilter(jwtUtil), CustomLoginFilter.class);

        // 6) 커스텀 로그아웃 필터 - /auth/logout
        CustomLogoutFilter logoutFilter = new CustomLogoutFilter(COOKIE_URL, jwtUtil, redisService);
        http
                .addFilterBefore(logoutFilter, LogoutFilter.class);

        return http.build();
    }
}