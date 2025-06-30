package com.msacommon.global.config;

import com.msacommon.global.jwt.AccessTokenArgumentResolver;
import com.msacommon.global.tenant.TenantInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final TenantInterceptor ti;

    public WebMvcConfig(TenantInterceptor ti) {
        this.ti = ti;
    }

    @Override
    public void addInterceptors(InterceptorRegistry reg) {
        reg.addInterceptor(ti).order(Ordered.HIGHEST_PRECEDENCE).addPathPatterns("/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AccessTokenArgumentResolver());
    }

}
