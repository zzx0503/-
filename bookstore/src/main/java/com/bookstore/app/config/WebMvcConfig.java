package com.bookstore.app.config;

import com.bookstore.interceptor.AdminInterceptor;
import com.bookstore.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
            .addPathPatterns("/api/**", "/admin-api/**")
            .excludePathPatterns(
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/refresh",
                "/error", "/doc.html", "/v3/api-docs/**", "/webjars/**", "/actuator/**",
                "/internal/agent-tools/**"
            );
        registry.addInterceptor(adminInterceptor)
            .addPathPatterns("/admin-api/**")
            .excludePathPatterns("/admin-api/auth/login");
    }
}
