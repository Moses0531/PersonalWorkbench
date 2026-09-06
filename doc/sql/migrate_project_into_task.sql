-- =============================================================================
-- 方案 A：项目合并进任务树（可直接在现有库执行）
-- 文件：doc/sql/migrate_project_into_task.sql
-- 说明：
--   1. 不要改 remo_assistant.sql / remo_assistant(s).sql，只用本脚本升级已有库
--   2. 执行前请备份数据库
--   3. 适用前提：库中仍有 project，且 task 仍有 project_id 列、尚无 node_type
--   4. 本脚本只应执行一次；重复执行会因列已存在而失败
-- =============================================================================

SET NAMES utf8mb4;

-- 1) 扩展 task（含迁移临时列）
ALTER TABLE `task`
  ADD COLUMN `node_type` varchar(16) NOT NULL DEFAULT 'task'
    COMMENT '节点类型：project / phase / task' AFTER `user_id`,
  ADD COLUMN `archived` char(1) NOT NULL DEFAULT '0'
    COMMENT '是否归档（仅 project 有意义：0-否，1-是）' AFTER `node_type`,
  ADD COLUMN `_mig_old_project_id` bigint NULL DEFAULT NULL
    COMMENT '迁移临时列' AFTER `archived`;

-- 2) 旧项目 → 项目根任务（临时列记下旧 project_id）
INSERT INTO `task` (
  `user_id`, `node_type`, `archived`, `_mig_old_project_id`, `parent_task_id`, `plan_batch_id`,
  `title`, `description`, `status`, `priority`, `due_time`, `tags`,
  `display_order`, `remark`, `attachments`, `create_time`, `update_time`
)
SELECT
  p.`user_id`,
  'project',
  IF(p.`status` = '1', '1', '0'),
  p.`project_id`,
  NULL,
  NULL,
  p.`name`,
  IFNULL(p.`description`, ''),
  '0',
  0,
  NULL,
  '',
  IFNULL(p.`display_order`, 0),
  IFNULL(p.`remark`, ''),
  NULL,
  IFNULL(p.`create_time`, NOW()),
  IFNULL(p.`update_time`, NOW())
FROM `project` p;

-- 3) 原顶级项目任务挂到新项目根；识别阶段 node_type
UPDATE `task` t
INNER JOIN `task` root
  ON root.`node_type` = 'project'
 AND root.`_mig_old_project_id` = t.`project_id`
SET
  t.`parent_task_id` = IF(t.`parent_task_id` IS NULL, root.`task_id`, t.`parent_task_id`),
  t.`node_type` = CASE
    WHEN IFNULL(t.`remark`, '') LIKE '%阶段%' THEN 'phase'
    ELSE 'task'
  END
WHERE t.`project_id` IS NOT NULL
  AND t.`node_type` <> 'project';

-- 4) 去掉临时列与 project_id，重建索引
ALTER TABLE `task`
  DROP INDEX `idx_task_project_status_order`,
  DROP COLUMN `project_id`,
  DROP COLUMN `_mig_old_project_id`,
  ADD INDEX `idx_task_user_node_archived`(`user_id` ASC, `node_type` ASC, `archived` ASC);

-- 5) 删项目表与 project:* 权限（325–328）
DROP TABLE IF EXISTS `project`;

DELETE FROM `role_permission` WHERE `permission_id` IN (325, 326, 327, 328);
DELETE FROM `sys_permission` WHERE `permission_id` IN (325, 326, 327, 328);

UPDATE `sys_permission`
SET `remark` = '任务与项目树（项目为根任务，阶段/步骤为子任务）'
WHERE `permission_id` = 320;
