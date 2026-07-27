package com.seatech.minsu.common;

/** 业务错误码约定：4xx 客户端问题，5xx 服务端问题 */
public final class ResultCode {

    public static final int SUCCESS = 0;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int SERVER_ERROR = 500;

    private ResultCode() {
    }
}
