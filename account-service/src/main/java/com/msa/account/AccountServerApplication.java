package com.msa.account;

import com.msacommon.global.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@EnableCaching
@EnableJpaAuditing
@EnableDiscoveryClient
@SpringBootApplication(exclude = FlywayAutoConfiguration.class, scanBasePackages = {
        "com.msa.account", "com.msacommon.global"
})
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class AccountServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountServerApplication.class, args);
    }

    @Bean
    public AuditorAware<String> auditorProvider(JwtUtil jwtUtil) {
        return () -> {
            HttpServletRequest request =
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

            String bearerToken = request.getHeader("Authorization");

            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                String token = bearerToken.substring(7);

                try {
                    String id = jwtUtil.getId(token);
                    return Optional.ofNullable(id);
                } catch (Exception e) {
                    return Optional.of("anonymous");
                }
            }

            return Optional.of("anonymous");
        };
    }

}