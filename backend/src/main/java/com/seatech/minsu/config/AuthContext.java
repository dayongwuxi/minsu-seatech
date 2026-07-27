package com.seatech.minsu.config;

/** 保存当前请求登录者 id（会员或管理员，按拦截器写入） */
public final class AuthContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    public static void set(Long userId) {
        CURRENT.set(userId);
    }

    public static Long get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    private AuthContext() {
    }
}
