-- 补跑：migrate_project_into_task.sql 最后一步字段名写错（应为 remark）
-- 前面 1～8 步已成功时，只执行本文件即可。

UPDATE `sys_permission`
SET `remark` = '任务与项目树（项目为根任务，阶段/步骤为子任务）'
WHERE `permission_id` = 320;
