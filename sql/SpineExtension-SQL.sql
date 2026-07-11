create database `spine_extension`;
DROP TABLE IF EXISTS `sys_auth`;
CREATE TABLE `sys_auth` (
                               `account_id` bigint NOT NULL AUTO_INCREMENT COMMENT '账号ID',
                               `account` varchar(30) NOT NULL COMMENT '登录账号（手机号/邮箱/用户名）',
                               `password` varchar(100) NOT NULL COMMENT '密码(BCrypt加密)',
                               `status` char(1) DEFAULT '0' COMMENT '状态（0-正常，1-封禁）',
                               `last_login_time` datetime DEFAULT NULL COMMENT '最近登录时间',
                               `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               PRIMARY KEY (`account_id`),
                               UNIQUE KEY `uk_account` (`account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统账号表';

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
                            `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                            `account_id` bigint NOT NULL COMMENT '关联的账号ID',
                            `role_id` bigint NOT NULL COMMENT '角色ID',
                            `username` varchar(30) NOT NULL COMMENT '用户昵称',
                            `email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
                            `real_name` varchar(100) DEFAULT '' COMMENT '真实姓名',
                            `phone` varchar(11) DEFAULT '' COMMENT '手机号码',
                            `sex` char(1) DEFAULT '0' COMMENT '用户性别 0-男 1-女',
                            `avatar` varchar(255) DEFAULT '' COMMENT '头像地址',
                            `birthday` datetime DEFAULT NULL COMMENT '生日',
                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`user_id`),
                            KEY `idx_account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';

DROP TABLE IF EXISTS `role_permission`;
DROP TABLE IF EXISTS `sys_permission`;
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
                        `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                        `role_name` varchar(50) NOT NULL COMMENT '角色名称',
                        `role_code` varchar(50) NOT NULL COMMENT '角色编码',
                        `level` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '角色层级',
                        `description` varchar(200) DEFAULT '' COMMENT '角色描述',
                        `status` char(1) DEFAULT '0' COMMENT '状态（0-正常，1-停用）',
                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (`role_id`),
                        UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色表';

INSERT INTO `sys_role` (`role_id`, `role_name`, `role_code`, `level`, `description`) VALUES
                                                                                     (1, '超级管理员', 'ROOT', 0, '平台最高权限'),
                                                                                     (2, '管理员', 'ADMIN', 1, '系统运维权限'),
                                                                                     (3, '普通用户', 'USER', 2, '普通业务用户');

-- 权限表
CREATE TABLE `sys_permission` (
                              `permission_id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
                              `name` varchar(50) NOT NULL COMMENT '名称',
                              `parent_id` bigint DEFAULT '0' COMMENT '父权限ID（0表示根节点）',
                              `type` char(1) DEFAULT 'M' COMMENT '类型（D-目录，M-菜单，F-功能）',
                              `status` char(1) DEFAULT '0' COMMENT '状态（0-正常，1-停用）',
                              `code` varchar(20) DEFAULT NULL COMMENT '权限标识码',
                              `icon` varchar(255) DEFAULT '' COMMENT '图标URL',
                              `remark` varchar(500) DEFAULT '' COMMENT '备注',
                              `router_name` varchar(255) DEFAULT NULL COMMENT '页面路由',
                              `order` int DEFAULT NULL COMMENT '排序',
                              `component_path` varchar(255) DEFAULT NULL COMMENT '路径',
                              `is_display` int DEFAULT '0' COMMENT '是否显示在导航栏',
                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              PRIMARY KEY (`permission_id`),
                              KEY `idx_parent_id` (`parent_id`),
                              KEY `idx_type` (`type`),
                              KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2031 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';

CREATE TABLE `role_permission` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `role_id` bigint NOT NULL COMMENT '角色ID',
                                   `permission_id` bigint NOT NULL COMMENT '权限ID',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
                                   KEY `idx_role_id` (`role_id`),
                                   KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-权限关联表';