package com.moses.exception;

/**
 * 返回给调用方的稳定错误编码。文案可以改，编码不要改。
 * 同时声明对应的 HTTP 状态码，由全局异常处理器统一映射。
 */
public enum ErrorCode {

    UNAUTHORIZED(401),
    FORBIDDEN(403),
    USER_NOT_FOUND(404),
    USER_BANNED(403),
    NOT_FOUND(404),
    CONFLICT(409),
    BAD_REQUEST(400),
    SYSTEM_ERROR(500);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    /** 对应的 HTTP 状态码 */
    public int httpStatus() {
        return httpStatus;
    }
}
