package com.moses.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultConfig {
    private Integer code; //编码：1成功，0为失败
    private String msg; //错误信息
    private Object data; //数据
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
        ResultConfig result = new ResultConfig();
        result.msg = msg;
        result.code = 0;
        return result;
    }
}
