-- 文案对齐：任务 → 待办；菜单与权限展示名更贴近 RemoAssistant
-- 可重复执行；不改 code / 路由，仅改 name、remark

UPDATE `sys_permission`
SET `name` = '日程',
    `remark` = '今天的安排，可挂到待办'
WHERE `permission_id` = 310 AND `code` = 'event';

UPDATE `sys_permission`
SET `name` = '桌面助手',
    `remark` = '拆解待办、梳理笔记'
WHERE `permission_id` = 255 AND `code` = 'ai:chat';

UPDATE `sys_permission`
SET `remark` = '待办看板：清单、阶段与待办同屏推进'
WHERE `permission_id` = 320 AND `code` = 'task';

UPDATE `sys_permission` SET `name` = '新增待办' WHERE `permission_id` = 321 AND `code` = 'task:add';
UPDATE `sys_permission` SET `name` = '删除待办' WHERE `permission_id` = 322 AND `code` = 'task:remove';
UPDATE `sys_permission` SET `name` = '编辑待办' WHERE `permission_id` = 323 AND `code` = 'task:modify';
UPDATE `sys_permission` SET `name` = '查看待办' WHERE `permission_id` = 324 AND `code` = 'task:query';

UPDATE `sys_permission`
SET `remark` = '待办清单智能规划预览与落板'
WHERE `permission_id` = 329 AND `code` = 'ai:plan';

UPDATE `sys_permission`
SET `name` = 'AI 笔记生成待办',
    `remark` = '整理稿拆解为待办树并落板'
WHERE `code` = 'ai:note:to-tasks';
