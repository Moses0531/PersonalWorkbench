package com.moses.rabc.constant;

/**
 * 系统内置角色编码（对应 sys_role.role_code）。
 * 业务代码按角色 code 解析角色，禁止硬编码角色 ID（ID 属于种子数据，可能随环境变化）。
 */
public final class RoleCode {

    /** 超级管理员（平台最高权限） */
    public static final String ROOT = "ROOT";

    /** 管理员（系统运维权限） */
    public static final String ADMIN = "ADMIN";

    /** 普通用户（注册默认角色） */
    public static final String USER = "USER";

    private RoleCode() {
    }
}
