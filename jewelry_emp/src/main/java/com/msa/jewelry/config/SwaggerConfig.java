package com.msa.jewelry.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI(Swagger) 설정.
 *
 * <p>2026-05 P6 추가. springdoc-openapi v2.x 기반.
 *
 * <ul>
 *   <li>접속 URL: {@code /swagger-ui/index.html}</li>
 *   <li>JSON 스펙: {@code /v3/api-docs}</li>
 * </ul>
 *
 * <p>인증은 api-gateway / auth-service 가 외부에서 발급한 Bearer JWT 를 그대로 받는 구조이므로,
 * Swagger UI 에서도 상단의 "Authorize" 버튼으로 토큰을 한 번 입력하면 모든 API 호출에 자동 부착된다.
 * (jewelry_emp 자체는 토큰을 검증하지 않으며, 검증은 운영 환경에서 api-gateway 가 수행.)
 */
@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI jewelryEmpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("jewelry_emp API")
                        .description("MSA 4개 서비스(order/account/product/user)를 통합한 모놀로식 백엔드 API. " +
                                "외부 호출은 api-gateway 를 거쳐 들어오며, 모든 보호된 API 는 Bearer JWT 가 필요합니다.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Jewelry Emp Backend Team"))
                        .license(new License()
                                .name("Internal Use Only")))
                .servers(List.of(
                        new Server().url("http://localhost:8023").description("Local"),
                        new Server().url("/").description("Same-host (gateway-routed)")
                ))
                // Bearer JWT — Swagger UI 의 "Authorize" 버튼으로 토큰을 한 번 입력하면 모든 호출에 자동 부착
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("api-gateway / auth-service 가 발급한 JWT 액세스 토큰. " +
                                                "포맷: `Bearer <token>` — Swagger UI 에서는 'Bearer' 접두사 없이 토큰만 입력하면 됩니다.")));
    }
}
