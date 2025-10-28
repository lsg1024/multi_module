package com.msa.common.global.config;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.AuditorHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Slf4j
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider(JwtUtil jwtUtil) {
        return () -> {

            Optional<String> auditorFromHolder = AuditorHolder.getAuditor();
            if (auditorFromHolder.isPresent()) {
                return auditorFromHolder;
            }

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return Optional.of("anonymous");
            }

            HttpServletRequest request = attributes.getRequest();
            String bearerToken = request.getHeader("Authorization");

            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                String token = bearerToken.substring(7);
                try {
                    return Optional.ofNullable(jwtUtil.getId(token));
                } catch (Exception e) {
                    return Optional.of("anonymous");
                }
            }

            return Optional.of("anonymous");
        };
    }
}