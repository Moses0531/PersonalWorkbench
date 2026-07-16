-- ----------------------------
-- Table structure for role_permission
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission`  (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                    `role_id` bigint NOT NULL COMMENT '角色ID',
                                    `permission_id` bigint NOT NULL COMMENT '权限ID',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    UNIQUE INDEX `uk_role_permission`(`role_id` ASC, `permission_id` ASC) USING BTREE,
                                    INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
                                    INDEX `idx_permission_id`(`permission_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 52 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色-权限关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role_permission
-- ----------------------------
INSERT INTO `role_permission` VALUES (1, 0, 1);
INSERT INTO `role_permission` VALUES (2, 0, 100);
INSERT INTO `role_permission` VALUES (3, 0, 103);
INSERT INTO `role_permission` VALUES (4, 0, 200);
INSERT INTO `role_permission` VALUES (5, 0, 210);
INSERT INTO `role_permission` VALUES (6, 0, 211);
INSERT INTO `role_permission` VALUES (7, 0, 212);
INSERT INTO `role_permission` VALUES (8, 0, 213);
INSERT INTO `role_permission` VALUES (9, 0, 214);
INSERT INTO `role_permission` VALUES (10, 0, 220);
INSERT INTO `role_permission` VALUES (11, 0, 221);
INSERT INTO `role_permission` VALUES (12, 0, 222);
INSERT INTO `role_permission` VALUES (13, 0, 223);
INSERT INTO `role_permission` VALUES (14, 0, 224);
INSERT INTO `role_permission` VALUES (15, 0, 230);
INSERT INTO `role_permission` VALUES (16, 0, 231);
INSERT INTO `role_permission` VALUES (17, 0, 232);
INSERT INTO `role_permission` VALUES (18, 0, 233);
INSERT INTO `role_permission` VALUES (19, 0, 234);
INSERT INTO `role_permission` VALUES (20, 0, 240);
INSERT INTO `role_permission` VALUES (21, 0, 241);
INSERT INTO `role_permission` VALUES (22, 0, 242);
INSERT INTO `role_permission` VALUES (23, 0, 243);
INSERT INTO `role_permission` VALUES (24, 0, 244);
INSERT INTO `role_permission` VALUES (25, 0, 250);
INSERT INTO `role_permission` VALUES (26, 0, 251);
INSERT INTO `role_permission` VALUES (27, 0, 252);
INSERT INTO `role_permission` VALUES (28, 0, 253);
INSERT INTO `role_permission` VALUES (45, 0, 254);
INSERT INTO `role_permission` VALUES (47, 0, 255);
INSERT INTO `role_permission` VALUES (29, 1, 1);
INSERT INTO `role_permission` VALUES (30, 1, 100);
INSERT INTO `role_permission` VALUES (31, 1, 103);
INSERT INTO `role_permission` VALUES (32, 1, 200);
INSERT INTO `role_permission` VALUES (33, 1, 210);
INSERT INTO `role_permission` VALUES (34, 1, 211);
INSERT INTO `role_permission` VALUES (35, 1, 212);
INSERT INTO `role_permission` VALUES (36, 1, 213);
INSERT INTO `role_permission` VALUES (37, 1, 214);
INSERT INTO `role_permission` VALUES (44, 1, 220);
INSERT INTO `role_permission` VALUES (43, 1, 224);
INSERT INTO `role_permission` VALUES (41, 1, 240);
INSERT INTO `role_permission` VALUES (42, 1, 244);
INSERT INTO `role_permission` VALUES (46, 1, 255);
INSERT INTO `role_permission` VALUES (51, 1, 256);
INSERT INTO `role_permission` VALUES (38, 2, 1);
INSERT INTO `role_permission` VALUES (39, 2, 100);
INSERT INTO `role_permission` VALUES (40, 2, 103);
INSERT INTO `role_permission` VALUES (48, 2, 255);
INSERT INTO `role_permission` VALUES (50, 2, 256);
INSERT INTO `role_permission` VALUES (49, 5, 256);

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`  (
                                   `permission_id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
                                   `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
                                   `parent_id` bigint NULL DEFAULT 0 COMMENT '父权限ID（0表示根节点）',
                                   `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'M' COMMENT '类型（D-目录，M-菜单，F-功能）',
                                   `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态（0-正常，1-停用）',
                                   `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限标识码',
                                   `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '图标URL',
                                   `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
                                   `router_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '页面路由',
                                   `display_order` int NULL DEFAULT NULL COMMENT '排序',
                                   `component_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路径',
                                   `is_display` int NULL DEFAULT 0 COMMENT '是否显示在导航栏',
                                   `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   PRIMARY KEY (`permission_id`) USING BTREE,
                                   INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
                                   INDEX `idx_type`(`type` ASC) USING BTREE,
                                   INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 257 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_permission
-- ----------------------------
INSERT INTO `sys_permission` VALUES (1, '首页', 0, 'M', '0', 'home:query', '', NULL, 'DashboardPage', 0, '/dashboard', 0, '2026-07-12 23:59:09', '2026-07-13 22:16:56');
INSERT INTO `sys_permission` VALUES (100, '个人信息', 0, 'M', '0', 'profile', '', '个人信息页', 'ProfilePage', 1, '/profile', 1, '2026-07-12 23:59:09', '2026-07-13 22:16:56');
INSERT INTO `sys_permission` VALUES (103, '编辑个人信息', 100, 'F', '0', 'profile:modify', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (200, '系统管理目录', 0, 'D', '0', 'system', '', '系统管理根目录', NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (210, '用户管理列表', 200, 'M', '0', 'user', '', '普通用户管理', 'UserManagementPage', 0, '/user-management', 0, '2026-07-12 23:59:09', '2026-07-14 00:09:12');
INSERT INTO `sys_permission` VALUES (211, '新增用户', 210, 'F', '0', 'user:add', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (212, '删除用户', 210, 'F', '0', 'user:remove', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (213, '编辑用户', 210, 'F', '0', 'user:modify', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (214, '查看用户', 210, 'F', '0', 'user:query', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (220, '角色管理列表', 200, 'M', '0', 'role', '', '角色配置', 'RoleManagementPage', NULL, '/role-management', 0, '2026-07-12 23:59:09', '2026-07-13 22:16:56');
INSERT INTO `sys_permission` VALUES (221, '新增角色', 220, 'F', '0', 'role:add', '', '', '', 0, NULL, 0, '2026-07-12 23:59:09', '2026-07-15 02:14:43');
INSERT INTO `sys_permission` VALUES (222, '删除角色', 220, 'F', '0', 'role:remove', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (223, '编辑角色', 220, 'F', '0', 'role:modify', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (224, '查看角色', 220, 'F', '0', 'role:query', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (230, '权限管理列表', 200, 'M', '0', 'permission', '', '权限配置', 'PermissionManagementPage', NULL, '/permission-management', 0, '2026-07-12 23:59:09', '2026-07-13 22:16:56');
INSERT INTO `sys_permission` VALUES (231, '新增权限', 230, 'F', '0', 'permission:add', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (232, '删除权限', 230, 'F', '0', 'permission:remove', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (233, '编辑权限', 230, 'F', '0', 'permission:modify', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (234, '查看权限', 230, 'F', '0', 'permission:query', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (240, '管理员信息列表', 200, 'M', '0', 'admin', '', '超级管理员可访问', 'AdminManagementPage', NULL, '/admin-management', 0, '2026-07-12 23:59:09', '2026-07-13 22:16:56');
INSERT INTO `sys_permission` VALUES (241, '新增管理员', 240, 'F', '0', 'admin:add', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (242, '删除管理员', 240, 'F', '0', 'admin:remove', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (243, '编辑管理员', 240, 'F', '0', 'admin:modify', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (244, '查看管理员', 240, 'F', '0', 'admin:query', '', NULL, NULL, NULL, NULL, 0, '2026-07-12 23:59:09', '2026-07-12 23:59:09');
INSERT INTO `sys_permission` VALUES (255, 'AI对话', 0, 'M', '0', 'ai:chat', '', '', 'AiChatPage', 0, NULL, 0, '2026-07-15 01:50:49', '2026-07-15 01:50:49');
INSERT INTO `sys_permission` VALUES (256, '查看个人信息', 100, 'F', '0', 'profile:query', '', '', '', 0, NULL, 0, '2026-07-16 00:06:08', '2026-07-16 00:06:32');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
                             `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                             `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
                             `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码',
                             `level` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '角色层级',
                             `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '角色描述',
                             `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态（0-正常，1-停用）',
                             `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                             PRIMARY KEY (`role_id`) USING BTREE,
                             UNIQUE INDEX `uk_role_code`(`role_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '管理员', 'ADMIN', 1, '系统运维权限', '0', '2026-07-12 18:03:25', '2026-07-13 23:37:24');
INSERT INTO `sys_role` VALUES (2, '普通用户', 'USER', 2, '普通业务用户', '0', '2026-07-12 18:03:25', '2026-07-12 18:03:58');
INSERT INTO `sys_role` VALUES (5, '超级管理员', 'ROOT', 0, '平台最高权限', '0', '2026-07-12 18:03:25', '2026-07-13 23:37:27');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
                             `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                             `account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '系统账号',
                             `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码(BCrypt)',
                             `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态（0-正常，1-封禁）',
                             `role_id` bigint NOT NULL COMMENT '角色ID',
                             `username` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户昵称',
                             `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
                             `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
                             `real_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '真实姓名',
                             `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '性别',
                             `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'https://fangqianmin.oss-cn-hangzhou.aliyuncs.com/DefaultAva.png' COMMENT '头像',
                             `birthday` datetime NULL DEFAULT NULL COMMENT '生日',
                             `last_login_time` datetime NULL DEFAULT NULL COMMENT '最近登录时间',
                             `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
                             `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             PRIMARY KEY (`user_id`) USING BTREE,
                             UNIQUE INDEX `uk_account`(`account` ASC) USING BTREE,
                             UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE,
                             UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (2, '95626163', '$2a$10$7kkUrobqVd2rW56QbgZguOZiq12OgzwDpj/nIiu67WnryPc4bnTkW', '0', 2, '用户95626163', '14585252396', NULL, '', '0', 'https://fangqianmin.oss-cn-hangzhou.aliyuncs.com/DefaultAva.png', NULL, '2026-07-14 00:24:45', '2026-07-13 23:03:14', '2026-07-15 02:14:59');
INSERT INTO `sys_user` VALUES (3, 'admin', '$2b$12$kvyLSAyBVTlCz4GNP3XBI.19PoQXFQVNDX/rKp220FJOfSUJhwwAm', '0', 1, 'ADMIN', '14525253636', 'admin@admin.com', '', '0', 'https://fangqianmin.oss-cn-hangzhou.aliyuncs.com/DefaultAva.png', NULL, '2026-07-15 02:44:28', '2026-07-13 23:52:59', '2026-07-15 02:44:28');
INSERT INTO `sys_user` VALUES (4, 'root', '$2a$10$TOqMXc7qBBtvQA5OJ5VADOMvYwcK8xSWvmiWpedidXNyz.lp807TS', '0', 0, 'ROOT', NULL, NULL, '超级管理员', '0', 'https://fangqianmin.oss-cn-hangzhou.aliyuncs.com/2026/07/69b5e539-179c-4a7c-b51d-469e49769d65.png', '2025-10-14 08:00:00', '2026-07-16 00:03:02', '2026-07-12 23:59:09', '2026-07-16 00:03:02');

SET FOREIGN_KEY_CHECKS = 1;
