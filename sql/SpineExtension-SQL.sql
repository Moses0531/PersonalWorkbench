DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
                            `user_id`         bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                            `account`         varchar(50) NOT NULL COMMENT '登录账号（手机/邮箱/用户名）',
                            `password`        varchar(100) NOT NULL COMMENT '密码(BCrypt)',
                            `status`          char(1) DEFAULT '0' COMMENT '状态（0-正常，1-封禁）',
                            `role_id`         bigint NOT NULL COMMENT '角色ID',
                            `username`        varchar(30) NOT NULL COMMENT '用户昵称',
                            `phone`           varchar(11) DEFAULT NULL COMMENT '手机号',
                            `email`           varchar(50) DEFAULT NULL COMMENT '邮箱',
                            `real_name`       varchar(100) DEFAULT '' COMMENT '真实姓名',
                            `sex`             char(1) DEFAULT '0' COMMENT '性别',
                            `avatar`          varchar(255) DEFAULT 'https://fangqianmin.oss-cn-hangzhou.aliyuncs.com/DefaultAva.png' COMMENT '头像',
                            `birthday`        datetime DEFAULT NULL COMMENT '生日',
                            `last_login_time` datetime DEFAULT NULL COMMENT '最近登录时间',
                            `create_time`     datetime DEFAULT CURRENT_TIMESTAMP,
                            `update_time`     datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            PRIMARY KEY (`user_id`),
                            UNIQUE KEY `uk_account` (`account`),
                            UNIQUE KEY `uk_phone` (`phone`),
                            UNIQUE KEY `uk_email` (`email`)
) COMMENT='系统用户表';

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
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色表';

INSERT INTO `sys_role` (`role_id`, `role_name`, `role_code`, `level`, `description`) VALUES
                                                                                     (0, '超级管理员', 'ROOT', 0, '平台最高权限'),
                                                                                     (1, '管理员', 'ADMIN', 1, '系统运维权限'),
                                                                                     (2, '普通用户', 'USER', 2, '普通业务用户');

-- 权限表
CREATE TABLE `sys_permission` (
                              `permission_id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
                              `name` varchar(50) NOT NULL COMMENT '名称',
                              `parent_id` bigint DEFAULT '0' COMMENT '父权限ID（0表示根节点）',
                              `type` char(1) DEFAULT 'M' COMMENT '类型（D-目录，M-菜单，F-功能）',
                              `status` char(1) DEFAULT '0' COMMENT '状态（0-正常，1-停用）',
                              `code` varchar(255) DEFAULT NULL COMMENT '权限标识码',
                              `icon` varchar(255) DEFAULT '' COMMENT '图标URL',
                              `remark` varchar(500) DEFAULT '' COMMENT '备注',
                              `router_name` varchar(255) DEFAULT NULL COMMENT '组件名称（如 DashboardPage，对应 Vue 文件名）',
                              `display_order` int DEFAULT NULL COMMENT '排序',
                              `component_path` varchar(255) DEFAULT NULL COMMENT '前端路由路径（如 /dashboard）',
                              `is_display` int DEFAULT '0' COMMENT '是否显示在导航栏',
                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              PRIMARY KEY (`permission_id`),
                              KEY `idx_parent_id` (`parent_id`),
                              KEY `idx_type` (`type`),
                              KEY `idx_status` (`status`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';

CREATE TABLE `role_permission` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `role_id` bigint NOT NULL COMMENT '角色ID',
                                   `permission_id` bigint NOT NULL COMMENT '权限ID',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
                                   KEY `idx_role_id` (`role_id`),
                                   KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-权限关联表';

-- 权限数据
INSERT INTO `sys_permission` (`permission_id`, `name`, `parent_id`, `type`, `status`, `code`, `icon`, `remark`, `router_name`, `display_order`, `component_path`, `is_display`) VALUES
                                                                                                                                                                            (1, '首页', 0, 'M', '0', 'home:query', '', NULL, 'DashboardPage', 0, '/dashboard', 0),
                                                                                                                                                                            (100, '个人信息', 0, 'M', '0', 'profile', '', '个人信息页', 'ProfilePage', 1, '/profile', 1),
                                                                                                                                                                            (103, '编辑个人信息', 100, 'F', '0', 'profile:modify', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (200, '系统管理目录', 0, 'D', '0', 'system', '', '系统管理根目录', NULL, NULL, NULL, 0),
                                                                                                                                                                            (210, '用户管理列表', 200, 'M', '0', 'user', '', '普通用户管理', 'UserManagementPage', NULL, '/user-management', 0),
                                                                                                                                                                            (211, '新增用户', 210, 'F', '0', 'user:add', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (212, '删除用户', 210, 'F', '0', 'user:remove', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (213, '编辑用户', 210, 'F', '0', 'user:modify', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (214, '查看用户', 210, 'F', '0', 'user:query', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (220, '角色管理列表', 200, 'M', '0', 'role', '', '角色配置', 'RoleManagementPage', NULL, '/role-management', 0),
                                                                                                                                                                            (221, '新增角色', 220, 'F', '0', 'role:add', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (222, '删除角色', 220, 'F', '0', 'role:remove', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (223, '编辑角色', 220, 'F', '0', 'role:modify', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (224, '查看角色', 220, 'F', '0', 'role:query', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (230, '权限管理列表', 200, 'M', '0', 'permission', '', '权限配置', 'PermissionManagementPage', NULL, '/permission-management', 0),
                                                                                                                                                                            (231, '新增权限', 230, 'F', '0', 'permission:add', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (232, '删除权限', 230, 'F', '0', 'permission:remove', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (233, '编辑权限', 230, 'F', '0', 'permission:modify', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (234, '查看权限', 230, 'F', '0', 'permission:query', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (240, '管理员信息列表', 200, 'M', '0', 'admin', '', '超级管理员可访问', 'AdminManagementPage', NULL, '/admin-management', 0),
                                                                                                                                                                            (241, '新增管理员', 240, 'F', '0', 'admin:add', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (242, '删除管理员', 240, 'F', '0', 'admin:remove', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (243, '编辑管理员', 240, 'F', '0', 'admin:modify', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (244, '查看管理员', 240, 'F', '0', 'admin:query', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (250, '角色权限管理', 200, 'M', '0', 'rolePermission', '', '角色与权限绑定', 'RolePermissionPage', NULL, '/role-permission', 0),
                                                                                                                                                                            (251, '查看角色权限', 250, 'F', '0', 'rolePermission:query', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (252, '新增角色权限', 250, 'F', '0', 'rolePermission:add', '', NULL, NULL, NULL, NULL, 0),
                                                                                                                                                                            (253, '移除角色权限', 250, 'F', '0', 'rolePermission:remove', '', NULL, NULL, NULL, NULL, 0);

-- 角色权限关联：ROOT 拥有全部权限
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
                                                               (0, 1), (0, 100), (0, 103),
                                                               (0, 200), (0, 210), (0, 211), (0, 212), (0, 213), (0, 214),
                                                               (0, 220), (0, 221), (0, 222), (0, 223), (0, 224),
                                                               (0, 230), (0, 231), (0, 232), (0, 233), (0, 234),
                                                               (0, 240), (0, 241), (0, 242), (0, 243), (0, 244),
                                                               (0, 250), (0, 251), (0, 252), (0, 253);

-- 角色权限关联：ADMIN 管理普通用户 + 个人信息
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
                                                               (1, 1), (1, 100), (1, 103),
                                                               (1, 200), (1, 210), (1, 211), (1, 212), (1, 213), (1, 214);

-- 角色权限关联：USER 首页 + 个人信息
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
                                                               (2, 1), (2, 100), (2, 103);

-- 初始化 ROOT 用户（账号 root，密码 root123）
INSERT INTO `sys_user` (`account`, `password`, `status`, `role_id`, `username`, `avatar`) VALUES
    ('root', '$2b$10$ygdWz1SFbxvtm5YiAnY2S..Hl44JLU6j9oNCEcM9uCXInMxtheWEy', '0', 0, '超级管理员', 'https://fangqianmin.oss-cn-hangzhou.aliyuncs.com/DefaultAva.png');

-- 已有库迁移：order 为 MySQL 保留字，改名为 display_order
-- ALTER TABLE `sys_permission` CHANGE COLUMN `order` `display_order` int DEFAULT NULL COMMENT '排序';