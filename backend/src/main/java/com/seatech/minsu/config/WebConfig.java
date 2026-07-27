package com.seatech.minsu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;

    @Value("${minsu.upload.dir}")
    private String uploadDir;

    public WebConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 管理端：除登录/验证码外全部需要 admin token
        registry.addInterceptor(new AuthInterceptor(jwtUtil, JwtUtil.AUD_ADMIN))
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login", "/api/admin/captcha");
        // 会员端：仅登录后才可用的接口
        registry.addInterceptor(new AuthInterceptor(jwtUtil, JwtUtil.AUD_MEMBER))
                .addPathPatterns(
                        "/api/user/**",
                        "/api/bookings/**",
                        "/api/payments/**",
                        "/api/favorites/**",
                        "/api/reviews/**",
                        "/api/feedbacks/**",
                        "/api/chat/**",
                        "/api/upload"
                )
                // 免登录：支付配置与渠道回调（/api/promotions/active 本就不在拦截名单）
                .excludePathPatterns("/api/payments/config", "/api/payments/stripe/webhook",
                        "/api/payments/linktrust/callback");
    }
}
