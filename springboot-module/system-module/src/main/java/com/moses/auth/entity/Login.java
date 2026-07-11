package com.moses.auth.entity;

import lombok.Data;

@Data
public class Login {

    /** 登录标识：账号、手机号或邮箱 */
    private String account;

    /** 密码 */
    private String password;

    /** 验证码明文 */
    private String captchaCode;

    /** 验证码 token */
    private String captchaToken;

    /** 验证码时间戳 */
    private Long captchaTimestamp;

    /** 登录令牌（登录成功后返回） */
    private String token;

    /** 用户ID（登录成功后返回） */
    private Long userId;

    /** 用户名（登录成功后返回） */
    private String username;
}
