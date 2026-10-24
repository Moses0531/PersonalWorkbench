/*
 Navicat Premium Dump SQL

 Source Server         : 本机mysql
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : personal_workbench

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 19/07/2026 00:26:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
-- 允许显式写入 AUTO_INCREMENT 列的 0（ROOT.role_id 必须为 0）
SET SESSION sql_mode = CONCAT(@@SESSION.sql_mode, ',NO_AUTO_VALUE_ON_ZERO');

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色-权限关联表' ROW_FORMAT = DYNAMIC;

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
INSERT INTO `role_permission` VALUES (100, 0, 300);
INSERT INTO `role_permission` VALUES (106, 0, 320);
INSERT INTO `role_permission` VALUES (107, 0, 321);
INSERT INTO `role_permission` VALUES (108, 0, 322);
INSERT INTO `role_permission` VALUES (109, 0, 323);
INSERT INTO `role_permission` VALUES (110, 0, 324);
INSERT INTO `role_permission` VALUES (111, 0, 325);
INSERT INTO `role_permission` VALUES (112, 0, 326);
INSERT INTO `role_permission` VALUES (113, 0, 327);
INSERT INTO `role_permission` VALUES (114, 0, 328);
INSERT INTO `role_permission` VALUES (145, 0, 329);
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
INSERT INTO `role_permission` VALUES (115, 1, 300);
INSERT INTO `role_permission` VALUES (121, 1, 320);
INSERT INTO `role_permission` VALUES (122, 1, 321);
INSERT INTO `role_permission` VALUES (123, 1, 322);
INSERT INTO `role_permission` VALUES (124, 1, 323);
INSERT INTO `role_permission` VALUES (125, 1, 324);
INSERT INTO `role_permission` VALUES (126, 1, 325);
INSERT INTO `role_permission` VALUES (127, 1, 326);
INSERT INTO `role_permission` VALUES (128, 1, 327);
INSERT INTO `role_permission` VALUES (129, 1, 328);
INSERT INTO `role_permission` VALUES (147, 1, 329);
INSERT INTO `role_permission` VALUES (38, 2, 1);
INSERT INTO `role_permission` VALUES (39, 2, 100);
INSERT INTO `role_permission` VALUES (40, 2, 103);
INSERT INTO `role_permission` VALUES (48, 2, 255);
INSERT INTO `role_permission` VALUES (50, 2, 256);
INSERT INTO `role_permission` VALUES (130, 2, 300);
INSERT INTO `role_permission` VALUES (136, 2, 320);
INSERT INTO `role_permission` VALUES (137, 2, 321);
INSERT INTO `role_permission` VALUES (138, 2, 322);
INSERT INTO `role_permission` VALUES (139, 2, 323);
INSERT INTO `role_permission` VALUES (140, 2, 324);
INSERT INTO `role_permission` VALUES (141, 2, 325);
INSERT INTO `role_permission` VALUES (142, 2, 326);
INSERT INTO `role_permission` VALUES (143, 2, 327);
INSERT INTO `role_permission` VALUES (144, 2, 328);
INSERT INTO `role_permission` VALUES (149, 2, 329);
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '权限表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_permission
-- ----------------------------
INSERT INTO `sys_permission` VALUES (1, '首页', 0, 'M', '0', 'home:query', '', '', 'DashboardPage', 0, '/dashboard', 0, '2026-07-12 23:59:09', '2026-07-17 01:06:32');
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
INSERT INTO `sys_permission` VALUES (255, '桌面助手', 0, 'M', '0', 'ai:chat', '', '拆解待办、规划推进', 'AiChatPage', 0, NULL, 0, '2026-07-15 01:50:49', '2026-07-15 01:50:49');
INSERT INTO `sys_permission` VALUES (256, '查看个人信息', 100, 'F', '0', 'profile:query', '', '', '', 0, NULL, 0, '2026-07-16 00:06:08', '2026-07-16 00:06:32');
INSERT INTO `sys_permission` VALUES (320, '事务', 0, 'M', '0', 'task', '', '待办看板：清单、阶段与待办同屏推进', 'TaskPage', 1, '/task', 0, '2026-07-17 00:05:53', '2026-07-17 00:07:45');
INSERT INTO `sys_permission` VALUES (321, '新增待办', 320, 'F', '0', 'task:add', '', NULL, NULL, NULL, NULL, 0, '2026-07-17 00:05:53', '2026-07-17 00:05:53');
INSERT INTO `sys_permission` VALUES (322, '删除待办', 320, 'F', '0', 'task:remove', '', NULL, NULL, NULL, NULL, 0, '2026-07-17 00:05:53', '2026-07-17 00:05:53');
INSERT INTO `sys_permission` VALUES (323, '编辑待办', 320, 'F', '0', 'task:modify', '', NULL, NULL, NULL, NULL, 0, '2026-07-17 00:05:53', '2026-07-17 00:05:53');
INSERT INTO `sys_permission` VALUES (324, '查看待办', 320, 'F', '0', 'task:query', '', NULL, NULL, NULL, NULL, 0, '2026-07-17 00:05:53', '2026-07-17 00:05:53');
INSERT INTO `sys_permission` VALUES (325, '新增项目', 320, 'F', '0', 'project:add', '', NULL, NULL, NULL, NULL, 0, '2026-07-17 00:05:53', '2026-07-17 00:05:53');
INSERT INTO `sys_permission` VALUES (326, '删除项目', 320, 'F', '0', 'project:remove', '', NULL, NULL, NULL, NULL, 0, '2026-07-17 00:05:53', '2026-07-17 00:05:53');
INSERT INTO `sys_permission` VALUES (327, '编辑项目', 320, 'F', '0', 'project:modify', '', NULL, NULL, NULL, NULL, 0, '2026-07-17 00:05:53', '2026-07-17 00:05:53');
INSERT INTO `sys_permission` VALUES (328, '查看项目', 320, 'F', '0', 'project:query', '', NULL, NULL, NULL, NULL, 0, '2026-07-17 00:05:53', '2026-07-17 00:05:53');
INSERT INTO `sys_permission` VALUES (329, 'AI智能规划', 320, 'F', '0', 'ai:plan', '', '待办清单智能规划预览与落板', NULL, NULL, NULL, 0, '2026-07-18 00:51:20', '2026-07-18 00:51:20');

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role（约定：ROOT=0，ADMIN=1，USER=2，后续自定义角色自增从 3 起）
-- ----------------------------
INSERT INTO `sys_role` VALUES (0, '超级管理员', 'ROOT', 0, '平台最高权限', '0', '2026-07-12 18:03:25', '2026-07-17 01:00:40');
INSERT INTO `sys_role` VALUES (1, '管理员', 'ADMIN', 1, '系统运维权限', '0', '2026-07-12 18:03:25', '2026-07-13 23:37:24');
INSERT INTO `sys_role` VALUES (2, '普通用户', 'USER', 2, '普通业务用户', '0', '2026-07-12 18:03:25', '2026-07-12 18:03:58');


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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (2, 'user', '$2a$10$7kkUrobqVd2rW56QbgZguOZiq12OgzwDpj/nIiu67WnryPc4bnTkW', '0', 2, '用户95626163', '14585252396', NULL, '', '0', 'https://fangqianmin.oss-cn-hangzhou.aliyuncs.com/DefaultAva.png', NULL, '2026-07-18 22:08:12', '2026-07-13 23:03:14', '2026-07-18 22:08:12');
INSERT INTO `sys_user` VALUES (3, 'admin', '$2b$12$kvyLSAyBVTlCz4GNP3XBI.19PoQXFQVNDX/rKp220FJOfSUJhwwAm', '0', 1, 'ADMIN', '14525253636', 'admin@admin.com', '', '0', 'https://fangqianmin.oss-cn-hangzhou.aliyuncs.com/DefaultAva.png', NULL, '2026-07-15 02:44:28', '2026-07-13 23:52:59', '2026-07-15 02:44:28');
INSERT INTO `sys_user` VALUES (4, 'root', '$2a$10$TOqMXc7qBBtvQA5OJ5VADOMvYwcK8xSWvmiWpedidXNyz.lp807TS', '0', 0, 'ROOT', NULL, 'root@root.com', '超级管理员', '0', 'https://fangqianmin.oss-cn-hangzhou.aliyuncs.com/2026/07/69b5e539-179c-4a7c-b51d-469e49769d65.png', '2025-10-14 00:00:00', '2026-07-18 22:21:53', '2026-07-12 23:59:09', '2026-07-18 22:21:53');

-- ----------------------------
-- Table structure for project
-- ----------------------------
DROP TABLE IF EXISTS `project`;
CREATE TABLE `project`  (
  `project_id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID（逻辑关联 sys_user.user_id）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '项目描述',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '状态（0-进行中，1-已归档）',
  `display_order` int NOT NULL DEFAULT 0 COMMENT '显示顺序（越小越靠前）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注说明',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`project_id`) USING BTREE,
  INDEX `idx_project_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_project_user_order`(`user_id` ASC, `display_order` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '业务-项目表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of project
-- ----------------------------
INSERT INTO `project` VALUES (1, 2, '开发福建大学教务系统', '给福建大学开发一个完整的教务系统', '0', 0, '', '2026-07-17 00:31:39', '2026-07-17 00:51:33');

-- ----------------------------
-- Table structure for task
-- ----------------------------
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task`  (
  `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID（逻辑关联 sys_user.user_id）',
  `project_id` bigint NULL DEFAULT NULL COMMENT '所属项目ID（空=独立任务；逻辑关联 project.project_id）',
  `parent_task_id` bigint NULL DEFAULT NULL COMMENT '父任务ID（逻辑关联 task.task_id，可选子步骤）',
  `plan_batch_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AI规划批次号（同批落板/撤销）',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '描述',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '状态/看板列（0-待办，1-进行中，2-已完成，3-已取消）',
  `priority` int NOT NULL DEFAULT 0 COMMENT '优先级（整数，用户自填）',
  `due_time` datetime NULL DEFAULT NULL COMMENT '截止时间',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '标签（逗号分隔）',
  `display_order` int NOT NULL DEFAULT 0 COMMENT '同状态下显示顺序（越小越靠前）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注说明',
  `attachments` json NULL COMMENT '附件列表（JSON 数组，挂本任务；OSS；元素含 id/name/url/size/mime/createTime/status；status：0-未处理，1-已处理，2-处理完成）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`task_id`) USING BTREE,
  INDEX `idx_task_user_due`(`user_id` ASC, `due_time` ASC) USING BTREE,
  INDEX `idx_task_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_task_project_status_order`(`project_id` ASC, `status` ASC, `display_order` ASC) USING BTREE,
  INDEX `idx_task_plan_batch`(`user_id` ASC, `plan_batch_id` ASC) USING BTREE,
  INDEX `idx_task_parent`(`parent_task_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '业务-任务表（独立任务或项目子任务）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of task
-- ----------------------------
INSERT INTO `task` VALUES (1, 2, 1, NULL, NULL, '与甲方沟通明确项目所需功能', '', '2', 0, '2026-07-20 10:00:37', '', 0, '', NULL, '2026-07-17 00:38:53', '2026-07-17 00:48:36');
INSERT INTO `task` VALUES (2, 2, NULL, NULL, NULL, '完成周工作总结', '', '0', 0, '2026-07-19 08:00:32', '', 0, '', '[{\"id\": \"5ed2518b654c440e9294eade894eaa07\", \"url\": \"https://fangqianmin.oss-cn-hangzhou.aliyuncs.com/2026/07/29c4d38c-adb7-429f-81ba-2b95fff4e723.sql\", \"mime\": \"application/octet-stream\", \"name\": \"datdabase.sql\", \"size\": 5584, \"createTime\": \"2026-07-17 23:34:12\"}, {\"id\": \"c0108f6e2c5a43c7b22f4a241ebb1f35\", \"url\": \"https://fangqianmin.oss-cn-hangzhou.aliyuncs.com/2026/07/715171ff-4cf7-4eb0-bdeb-3a32b8cdc91e.sql\", \"mime\": \"application/octet-stream\", \"name\": \"_localhost-2026_05_18_23_17_31-dump.sql\", \"size\": 10338, \"createTime\": \"2026-07-18 00:26:16\"}]', '2026-07-17 00:52:46', '2026-07-18 00:26:17');
INSERT INTO `task` VALUES (28, 2, 1, NULL, '2ea4899466bb454394cdb8f0b2e32297', '核心功能开发', '完成教务系统必备的登录与成绩查询模块，确保主干流程可用。', '0', 0, '2026-07-25 00:00:00', '', 0, 'AI规划·阶段', NULL, '2026-07-18 01:03:49', '2026-07-18 01:03:49');
INSERT INTO `task` VALUES (29, 2, 1, 28, '2ea4899466bb454394cdb8f0b2e32297', '登录模块开发', '实现学生/教师身份认证，包括账号密码验证与session管理。', '0', 1, '2026-07-22 00:00:00', '', 0, 'AI规划·步骤', '[{\"id\": \"568dfb2fea254ef0bc5131d2c46b2fed\", \"url\": \"https://fangqianmin.oss-cn-hangzhou.aliyuncs.com/2026/07/524e6f85-7096-4125-9b4f-a2b454c6b478.sql\", \"mime\": \"application/octet-stream\", \"name\": \"_localhost-2026_05_18_23_17_31-dump.sql\", \"size\": 10338, \"status\": \"2\", \"createTime\": \"2026-07-18 20:48:00\"}]', '2026-07-18 01:03:49', '2026-07-18 20:48:11');
INSERT INTO `task` VALUES (30, 2, 1, 28, '2ea4899466bb454394cdb8f0b2e32297', '成绩查询模块开发', '实现按学期/课程查询成绩，并显示成绩列表数据。', '0', 1, '2026-07-25 00:00:00', '', 1, 'AI规划·步骤', NULL, '2026-07-18 01:03:49', '2026-07-18 01:03:49');
INSERT INTO `task` VALUES (31, 2, 1, NULL, '2ea4899466bb454394cdb8f0b2e32297', '极简集成与部署', '合并代码、快速联调并上线到测试环境，确保可演示。', '0', 0, '2026-08-01 00:00:00', '', 1, 'AI规划·阶段', NULL, '2026-07-18 01:03:49', '2026-07-18 01:03:49');
INSERT INTO `task` VALUES (32, 2, 1, 31, '2ea4899466bb454394cdb8f0b2e32297', '前后端联调与修复', '将前后端对接，修复核心流程的阻塞问题。', '0', 1, '2026-07-29 00:00:00', '', 0, 'AI规划·步骤', NULL, '2026-07-18 01:03:49', '2026-07-18 01:03:49');
INSERT INTO `task` VALUES (33, 2, 1, 31, '2ea4899466bb454394cdb8f0b2e32297', '部署与验收', '部署到服务器，完成冒烟测试并交付演示。', '0', 1, '2026-08-01 00:00:00', '', 1, 'AI规划·步骤', NULL, '2026-07-18 01:03:49', '2026-07-18 01:03:49');

SET FOREIGN_KEY_CHECKS = 1;
