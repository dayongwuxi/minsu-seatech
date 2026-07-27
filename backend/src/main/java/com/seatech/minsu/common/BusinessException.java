package com.seatech.minsu.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(ResultCode.BAD_REQUEST, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
