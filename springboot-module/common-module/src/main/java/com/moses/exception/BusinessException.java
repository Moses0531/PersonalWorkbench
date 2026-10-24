package com.moses.exception;

/**
 * 可直接返回给调用方的业务错误。未指定编码时按参数错误处理。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(String message) {
        this(ErrorCode.BAD_REQUEST, message);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode == null ? ErrorCode.BAD_REQUEST : errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
