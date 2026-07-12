package com.moses.user.entity;

import lombok.Data;

@Data
public class Register {

    /** 手机号（与邮箱二选一） */
    private String phone;

    /** 邮箱（与手机号二选一） */
    private String email;

    /** 密码 */
    private String password;

    /** 确认密码 */
    private String confirmPassword;

    /** 验证码明文 */
    private String captchaCode;

    /** 验证码 token */
    private String captchaToken;

    /** 验证码时间戳 */
    private Long captchaTimestamp;

    /** 系统生成的登录账号（注册成功后返回） */
    private String account;

    /** 用户ID（注册成功后返回） */
    private Long userId;
}
