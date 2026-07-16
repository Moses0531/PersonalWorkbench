DROP TABLE IF EXISTS `wb_project`;
CREATE TABLE `wb_project` (
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
                              INDEX `idx_wb_project_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
                              INDEX `idx_wb_project_user_order`(`user_id` ASC, `display_order` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作台-项目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for wb_task（任务）
-- project_id 为空：独立任务（如写本周工作总结）
-- project_id 非空：项目内任务（如教务系统-设计数据库）
-- 看板三列由 status 统计得出，不单独建列表
-- ----------------------------
DROP TABLE IF EXISTS `wb_task`;
CREATE TABLE `wb_task` (
                           `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
                           `user_id` bigint NOT NULL COMMENT '所属用户ID（逻辑关联 sys_user.user_id）',
                           `project_id` bigint NULL DEFAULT NULL COMMENT '所属项目ID（空=独立任务；逻辑关联 wb_project.project_id）',
                           `parent_task_id` bigint NULL DEFAULT NULL COMMENT '父任务ID（逻辑关联 wb_task.task_id，可选子步骤）',
                           `plan_batch_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AI规划批次号（同批落板/撤销）',
                           `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
                           `description` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '描述',
                           `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '状态/看板列（0-待办，1-进行中，2-已完成，3-已取消）',
                           `priority` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '1' COMMENT '优先级（0-低，1-中，2-高）',
                           `due_time` datetime NULL DEFAULT NULL COMMENT '截止时间',
                           `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '标签（逗号分隔）',
                           `display_order` int NOT NULL DEFAULT 0 COMMENT '同状态下显示顺序（越小越靠前）',
                           `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注说明',
                           `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           PRIMARY KEY (`task_id`) USING BTREE,
                           INDEX `idx_wb_task_user_due`(`user_id` ASC, `due_time` ASC) USING BTREE,
                           INDEX `idx_wb_task_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
                           INDEX `idx_wb_task_project_status_order`(`project_id` ASC, `status` ASC, `display_order` ASC) USING BTREE,
                           INDEX `idx_wb_task_plan_batch`(`user_id` ASC, `plan_batch_id` ASC) USING BTREE,
                           INDEX `idx_wb_task_parent`(`parent_task_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作台-任务表（独立任务或项目子任务）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for wb_event（日程）
-- repeat_type=1 且 repeat_weekdays 含 1：每周一固定（如开周会）
-- ----------------------------
DROP TABLE IF EXISTS `wb_event`;
CREATE TABLE `wb_event` (
                            `event_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日程ID',
                            `user_id` bigint NOT NULL COMMENT '所属用户ID（逻辑关联 sys_user.user_id）',
                            `task_id` bigint NULL DEFAULT NULL COMMENT '关联任务ID（可选；逻辑关联 wb_task.task_id）',
                            `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
                            `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '地点',
                            `start_time` datetime NOT NULL COMMENT '开始时间（重复日程表示当日时段起点）',
                            `end_time` datetime NOT NULL COMMENT '结束时间（重复日程表示当日时段终点）',
                            `is_all_day` tinyint NOT NULL DEFAULT 0 COMMENT '是否全天（0-否，1-是）',
                            `repeat_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '重复类型（0-不重复，1-每周）',
                            `repeat_weekdays` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '每周重复的星期（1=周一…7=周日，逗号分隔，如1或1,3,5）',
                            `display_order` int NOT NULL DEFAULT 0 COMMENT '同日显示顺序（越小越靠前）',
                            `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注说明',
                            `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`event_id`) USING BTREE,
                            INDEX `idx_wb_event_user_range`(`user_id` ASC, `start_time` ASC, `end_time` ASC) USING BTREE,
                            INDEX `idx_wb_event_user_repeat`(`user_id` ASC, `repeat_type` ASC) USING BTREE,
                            INDEX `idx_wb_event_task`(`task_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作台-日程表' ROW_FORMAT = Dynamic;
