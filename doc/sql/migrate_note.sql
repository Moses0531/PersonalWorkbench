-- =============================================================================
-- 笔记模块：note 表 + 权限种子（存量库升级）
-- 文件：doc/sql/migrate_note.sql
-- 说明：
--   1. 全量 dump remo_assistant.sql / remo_assistant(s).sql 已包含 note 与权限 340–346
--   2. 本脚本仅用于升级「尚未有笔记模块」的旧库；新库直接导入全量 dump 即可
--   3. 执行前请备份；本脚本幂等：权限用 INSERT IGNORE；表已存在则跳过 CREATE
-- =============================================================================

SET NAMES utf8mb4;

-- 1) 笔记表
CREATE TABLE IF NOT EXISTS `note` (
  `note_id` bigint NOT NULL AUTO_INCREMENT COMMENT '笔记ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标题（碎片可空）',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '正文（纯文本/Markdown）',
  `note_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'scrap' COMMENT '类型：scrap-碎片 / digest-整理稿',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '0-活跃，1-已归档',
  `source_note_ids` json NULL COMMENT '整理稿来源碎片 ID 列表',
  `linked_task_id` bigint NULL DEFAULT NULL COMMENT '生成待办后挂接的顶层清单 ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`note_id`) USING BTREE,
  INDEX `idx_note_user_update`(`user_id` ASC, `update_time` DESC) USING BTREE,
  INDEX `idx_note_user_type_status`(`user_id` ASC, `note_type` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '业务-笔记' ROW_FORMAT = DYNAMIC;

-- 2) 权限（340–346）
INSERT IGNORE INTO `sys_permission`
(`permission_id`, `name`, `parent_id`, `type`, `status`, `code`, `icon`, `remark`, `router_name`, `display_order`, `component_path`, `is_display`, `create_time`, `update_time`)
VALUES
(340, '笔记', 0, 'M', '0', 'note', '', '零散想法，整理后可落成待办', 'NotePage', 3, '/note', 0, NOW(), NOW()),
(341, '新增笔记', 340, 'F', '0', 'note:add', '', NULL, NULL, NULL, NULL, 0, NOW(), NOW()),
(342, '删除笔记', 340, 'F', '0', 'note:remove', '', NULL, NULL, NULL, NULL, 0, NOW(), NOW()),
(343, '编辑笔记', 340, 'F', '0', 'note:modify', '', NULL, NULL, NULL, NULL, 0, NOW(), NOW()),
(344, '查看笔记', 340, 'F', '0', 'note:query', '', NULL, NULL, NULL, NULL, 0, NOW(), NOW()),
(345, 'AI笔记整合', 340, 'F', '0', 'ai:note:consolidate', '', '多条碎片整合为整理稿', NULL, NULL, NULL, 0, NOW(), NOW()),
(346, 'AI 笔记生成待办', 340, 'F', '0', 'ai:note:to-tasks', '', '整理稿拆解为待办树并落板', NULL, NULL, NULL, 0, NOW(), NOW());

-- 若曾执行旧版 CasualNote 种子，纠正菜单名与路由
UPDATE `sys_permission`
SET `name` = '笔记',
    `router_name` = 'NotePage',
    `component_path` = '/note',
    `remark` = '零散想法，整理后可落成待办',
    `update_time` = NOW()
WHERE `permission_id` = 340;

-- 3) 角色授权（0 超管 / 1 管理员 / 2 普通用户，对齐会议）
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.role_id, p.permission_id
FROM (
  SELECT 0 AS role_id UNION ALL SELECT 1 UNION ALL SELECT 2
) r
CROSS JOIN (
  SELECT 340 AS permission_id UNION ALL SELECT 341 UNION ALL SELECT 342
  UNION ALL SELECT 343 UNION ALL SELECT 344 UNION ALL SELECT 345 UNION ALL SELECT 346
) p;
