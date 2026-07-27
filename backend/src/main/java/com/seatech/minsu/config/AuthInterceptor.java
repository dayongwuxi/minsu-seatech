package com.seatech.minsu.config;

import com.seatech.minsu.common.BusinessException;
import com.seatech.minsu.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 按路径前缀区分端校验 JWT：
 *   /api/admin/** → 管理端 token；其余 /api/** 需登录的接口 → 会员端 token。
 * 放行名单在 WebConfig 中配置。
 */
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final String audience;

    public AuthInterceptor(JwtUtil jwtUtil, String audience) {
        this.jwtUtil = jwtUtil;
        this.audience = audience;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
        Long userId = token == null ? null : jwtUtil.parseUserId(token, audience);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        AuthContext.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
