# Prompt 日志 · RemoAssistant

---

## 目录

| # | 主题 |
|---|---|
| 1 | 方案 A：项目与任务数据模型统一 |

---

## 1. 方案 A：项目与任务数据模型统一

| 项 | 内容 |
|---|---|
| 日期 | 2026-09-05 |
| 对应功能 | 项目并入任务树（project / phase / task） |
| 对应文件 | `doc/sql/migrate_project_into_task.sql`<br>`Task` / `/task/*`<br>`vue-module/.../TaskPage.vue`<br>`doc/api接口文档.md` |

### Prompt（原始）

```text
方案 A：将项目与任务统一为同一任务树数据模型（nodeType=project/phase/task），
废弃独立 wb_project / /project/*，存量库用增量迁移脚本，全量 dump 保持不变。
```

### AI 返回（原始）

```text
已按方案 A 统一数据模型：

1. 模型 — wb_task 以 nodeType 区分 project（根）/ phase / task；archived 仅用于 project；
   父子关系用 parentTaskId 构成树。
2. 迁移 — 存量库仅执行 doc/sql/migrate_project_into_task.sql；
   personal_workbench.sql 与 personal_workbench(s).sql 全量 dump 不改动。
3. 接口 — 废弃 /project/*；项目 CRUD 与附件审查走 /task/*（含 listProjectAttachments 的
   projectTaskId）；AI 规划 preview/apply 使用 body.projectTaskId（兼容旧 projectId）。
4. 前端 — TaskPage 单模块；?projectId= 语义为项目根 taskId；权限统一为 task:*。
```

---
