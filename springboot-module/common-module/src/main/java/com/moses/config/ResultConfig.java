package com.moses.config;

import com.moses.exception.ErrorCode;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResultConfig {
    /** 1 成功，0 失败。调用方用 errorCode 区分失败原因。 */
    private Integer code;
    private String msg;
    private Object data;
    /** 失败时的稳定编码，成功时为空 */
    private String errorCode;

    public static ResultConfig success() {
        ResultConfig result = new ResultConfig();
        result.code = 1;
        result.msg = "success";
        return result;
    }

    public static ResultConfig success(Object object) {
        ResultConfig result = new ResultConfig();
        result.data = object;
        result.code = 1;
        result.msg = "success";
        return result;
    }

    public static ResultConfig error(String msg) {
        return error(ErrorCode.BAD_REQUEST, msg);
    }

    public static ResultConfig error(ErrorCode errorCode, String msg) {
        ResultConfig result = new ResultConfig();
        result.code = 0;
        result.msg = msg;
        result.errorCode = (errorCode == null ? ErrorCode.BAD_REQUEST : errorCode).name();
        return result;
    }
}
