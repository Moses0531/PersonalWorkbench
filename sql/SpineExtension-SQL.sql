create database `spine_extension`;
DROP TABLE IF EXISTS `sys_account`;
CREATE TABLE `sys_account` (
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