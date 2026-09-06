-- =============================================================================
-- 撤销业务表 wb_ 前缀（存量库升级）
-- 文件：doc/sql/migrate_drop_wb_prefix.sql
-- 说明：
--   1. 执行前请备份；目标库建议已改为 remo_assistant（或先迁库再跑本脚本）
--   2. 若表已是无前缀名称，对应 RENAME 会失败，可跳过已完成的语句
--   3. event 为 MySQL 保留字，必须带反引号
-- =============================================================================

SET NAMES utf8mb4;

RENAME TABLE `wb_task` TO `task`;
RENAME TABLE `wb_event` TO `event`;
RENAME TABLE `wb_meeting` TO `meeting`;

-- 若已执行过 migrate_note.sql 且表名为 wb_note：
-- RENAME TABLE `wb_note` TO `note`;

-- 若库中仍有未合并的旧项目表：
-- RENAME TABLE `wb_project` TO `project`;
