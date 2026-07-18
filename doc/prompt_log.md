# Prompt 日志 · PersonalWorkbench

> 对应评分项 **C(1) Prompt 日志（10%）**  
> 约定：每条含 **原始 Prompt**、**AI 原始回复（代码块）**、**对应功能 / 文件**。  
> 工具：Cursor Agent · 来源：本机历史对话 · 时间：2026-07-16 ～ 2026-07-19

---

## 目录

| # | 主题 |
|---|---|
| 1 | 工作台三页落地（项目 / 任务 / 日程） |
| 2 | 日期反序列化 Bug（Jackson） |
| 3 | 日程改为周 / 月日历视图 |
| 4 | 操作日志 vs 任务附件（先评估） |
| 5 | 任务与项目合并入口评估 |
| 6 | 纠正错误的 `panels/` 拆分 |
| 7 | 项目空间大标题（非筛选条） |
| 8 | 任务四状态改为 Tab |
| 9 | 任务页视觉打磨 + 去掉卡片状态下拉 |
| 10 | 独立任务 / 绑定项目检索 |
| 11 | AI 智能规划项目任务 |
| 12 | 会议记录 + AI 纪要整理 |
| 13 | 登录后看不到会议菜单（权限） |
| 14 | 任务附件状态 |
| 15 | 附件列表弹窗 + kkFileView |
| 16 | 前置任务（先修）评估 |
| 17 | 首页「今日重点」改版 |
| 18 | 首页视觉增强（桌面仪器） |
| 19 | 首页加时钟 |
| 20 | 首页上方精修 |
| 21 | 登录页文案与配色 |
| 22 | 登录页办公桌背景 |
| 23 | 登录页突显核心功能 |
| 24 | 导航改为悬浮 Dock |
| 25 | 统一 Ant Design Vue 图标 |
| 26 | 整体配色分析 |
| 27 | 前端响应式组件评估 |
| 28 | ECharts 进度可视化评估 |
| 29 | 每周重复日程不显示 Bug |
| 30 | 后端模块体检与清理 |
| 31 | 根 README 编写 |

---

## 1. 工作台三页落地

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-16 |
| 对应功能 | 项目管理、任务看板、日程议程 |
| 对应文件 | `vue-module/src/apis/workbench/{Project,Task,Event}Api.js`<br>`vue-module/src/views/workbench/{Project,Task,Event}Page.vue`<br>`vue-module/src/styles/workbench.css` |

### Prompt（原始）

```text
根据后端接口设计与构建页面 @springboot-module/workbench-module
页面放在 @vue-module/src/views/workbench
调用后端接口 api 放在 @vue-module/src/apis/workbench
根据已经安装的 skill 设计
/design-taste-frontend /frontend-design
```

### AI 返回（原始）

```text
已按后端接口与 MosesFrame 设计体系落地工作台三页。

Design Read：个人生产力工作台（项目 / 任务 / 日程），沿用冷青 MosesFrame，偏向 Linear 风格看板与议程，不是落地页。
Dials：VARIANCE 6 / MOTION 4 / DENSITY 5

### 新增文件
| 类型 | 路径 |
|------|------|
| API | vue-module/src/apis/workbench/{Project,Task,Event}Api.js |
| 页面 | vue-module/src/views/workbench/{Project,Task,Event}Page.vue |
| 样式 | vue-module/src/styles/workbench.css（已接入 main.css） |

### 页面能力
- 项目管理 (/project)：列表 + KPI、状态标签、归档/恢复、CRUD（v-permission）
- 任务看板 (/task)：四列看板（待办/进行中/已完成/已取消）、优先级筛选、跨列移动、关联项目
- 日程议程 (/event)：按日分组、即将到来/全部/已过去、全天与每周重复、关联任务

### 使用前确认
1. 已执行 doc/sql/wb_permission.sql
2. 后端 8090 已启动
3. 重新登录以刷新菜单（路由由 router_name 自动匹配，无需改 routers.js）
```

---

## 2. 日期反序列化 Bug（Jackson）

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-16 |
| 对应功能 | 日程 / 任务时间字段提交 |
| 对应文件 | `springboot-module/start-module/src/main/resources/application.yml`（`spring.jackson`） |

### Prompt（原始）

```text
JSON parse error: Cannot deserialize value of type `java.util.Date` from String "2026-07-06 09:00:00":
not a valid representation (error: Failed to parse Date value '2026-07-06 09:00:00':
Cannot parse date "2026-07-06 09:00:00": while it seems to fit format 'yyyy-MM-dd'T'HH:mm:ss.SSSX',
parsing fails (leniency? null))
```

### AI 返回（原始）

```text
对，本来就该在 JSON / Jackson 层解决。

请求体已经是 JSON，问题不是“没用 JSON”，而是 Jackson 默认只认 ISO
（2026-07-06T09:00:00.000+08:00），前端发的是 2026-07-06 09:00:00，所以反序列化 java.util.Date 失败。

已在 application.yml 加上：

spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    serialization:
      write-dates-as-timestamps: false

重启后端后再保存日程即可。任务页的 dueTime 也会一起生效。
```

---

## 3. 日程改为周 / 月日历视图

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-16 |
| 对应功能 | 日程周视图 / 月视图 |
| 对应文件 | `vue-module/src/views/workbench/EventPage.vue` |

### Prompt（原始）

```text
这里应该显示日历组件，这样更直观更清晰
然后用户可以选择周查看和月查看，这样设计应该更加合理，你认为呢
/redesign-existing-projects
```

### AI 返回（原始）

```text
同意，这样更合理。

日程本质是时间轴上的空间分布，列表只能纵向扫，周/月格子能一眼看出疏密、冲突和空白。
原先用「即将到来 / 已过去」过滤，其实是在用筛选弥补缺少日历导航的问题。

已在 EventPage.vue 落地改造（沿用现有 Vue 3 + dayjs + workbench token，未引入新日历库）：

1. 主区域改为日历：月视图默认，可切周视图
2. 周期导航：上一周/月 · 今天 · 下一周/月
3. 右侧日详情：点选日期查看当日条目，支持编辑/删除
4. 快捷新建：点选日期后「添加」，或双击格子预填当天时间
5. 搜索保留；原先的时间范围 tabs 由日历导航替代

交互提示：点击格子选中 · 点击日程条打开编辑 · 双击格子新建。
```

---

## 4. 操作日志 vs 任务附件（先评估）

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-17 |
| 对应功能 | 产品决策：附件优先于全量审计日志 |
| 对应文件 | 后续落地：`wb_task.attachments` JSON / OSS 上传相关接口与 `TaskPage.vue` |

### Prompt（原始）

```text
思考是否增加以下两个点
1. workbench 模块引入操作日志，方便用户知道做了什么，避免工作重复
2. 增加文件功能，部分项目/任务所需文件或待处理文件可以一并上传，完成任务后相关文件可以保留存档
```

### AI 返回（原始节选）

```text
结论先说：附件值得做，且优先级高于操作日志；操作日志建议做成「实体时间线」而不是系统审计日志。

### 1. 操作日志 — 建议「轻量做」
| 方案 | 价值 | 问题 |
|------|------|------|
| 系统级操作日志（全量 CRUD 审计） | 可追溯 | 噪声大、个人场景用处有限 |
| 实体活动流（任务/项目上的关键变更） | 直接服务「防重复」 | 实现量可控 |

暂不建议：全库 AOP 审计表、独立「操作日志」管理页。

### 2. 文件功能 — 建议做
- 任务附件可复用已有阿里云 OSS
- 任务完成后不删附件
- 先做任务附件；项目级资料夹可二期

### 优先级
现在核心 CRUD 已可用
  ↓
① 任务附件（高价值）
  ↓
② 任务/项目活动流（轻量）
  ↓
③ 系统级审计日志（可长期不做）
```

---

## 5. 任务与项目合并入口评估

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-17 |
| 对应功能 | 事务页 Tab：任务 / 项目 |
| 对应文件 | `vue-module/src/views/workbench/TaskPage.vue` |

### Prompt（原始）

```text
任务和项目是否可以合并到一个页面，通过 tab 切换以提高效率？
是否认同这样的设计
```

### AI 返回（原始节选）

```text
有条件认同：合并导航入口、用 Tab 切换，能减少跳转；但不建议做成「同一套列表里硬塞两种实体」。

更认同的形态：
工作台
  └─ 事务（或「任务」）
       ├─ Tab「任务」→ 现有看板
       └─ Tab「项目」→ 现有列表

结论：
- 认同「合并到一页 + Tab」
- 不认同「交互与数据模型也合并成一种」
- 日程（EventPage）建议仍独立
```

---

## 6. 纠正错误的 `panels/` 拆分

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-17 |
| 对应功能 | 前端目录结构规范 |
| 对应文件 | 删除 `vue-module/src/views/workbench/panels/**`，逻辑收回 `TaskPage.vue` |

### Prompt（原始）

```text
@vue-module/src/views/workbench/panels 这是什么玩意
为什么新建了文件 看不懂整个项目的结构吗
```

### AI 返回（原始）

```text
说得对，panels/ 不该出现，是上次合并「任务 + 项目」时硬拆出来的，和本项目约定不一致。

上次落地「事务」单页 Tab 时，把原来的 TaskPage / ProjectPage 拆成：
TaskPage.vue          ← 只剩 Tab 壳
panels/
  TaskKanbanPanel.vue ← 原任务看板
  ProjectListPanel.vue← 原项目列表

项目实际结构：
views/workbench/     ← 路由页：*Page.vue
components/          ← 跨页面复用
没有 panels/ 这一层。

结论：新建 panels/ 是结构误判。要的话直接改成：看板 + 项目列表并回 TaskPage.vue，删掉整个 panels/。
```

---

## 7. 项目空间大标题（非筛选条）

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-17 |
| 对应功能 | 进入项目空间的信息架构 |
| 对应文件 | `vue-module/src/views/workbench/TaskPage.vue` |

### Prompt（原始）

```text
优化这个页面 @vue-module/src/views/workbench/TaskPage.vue
进入项目空间应该要有个大标题是这个项目
而不是在任务这里加筛选显示
```

### AI 返回（原始）

```text
已把「项目筛选」改成真正的项目空间体验：

1. 点项目名进入空间后，页头大标题就是项目名，副标题用项目描述
2. 去掉筛选横幅和工具栏里的「所属项目」下拉
3. 空间内隐藏任务/项目 Tab，改为「返回项目」
4. 统计、空态、新建任务都按当前项目收窄；卡片上也不再重复显示项目名

从项目列表点项目名即可进入；要回到列表用左上角「返回项目」。
```

---

## 8. 任务四状态改为 Tab

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-17 |
| 对应功能 | 任务列表按状态浏览 |
| 对应文件 | `vue-module/src/views/workbench/TaskPage.vue` |

### Prompt（原始）

```text
做成 4 个 tab 建 比这样清晰 你到底会不会设计网页
/redesign-existing-projects
```

### AI 返回（原始）

```text
四列看板已经改成状态 Tab 了。

改动
- 待办 / 进行中 / 已完成 / 已取消 四个 Tab，带数量角标
- 下方只显示当前状态的任务列表（单列，标题可读性更好）
- 新建任务默认落到当前 Tab 的状态
- 状态切换、编辑、删除逻辑不变

刷新页面看一下；窄屏下 Tab 可横向滑动。
```

---

## 9. 任务页视觉打磨 + 去掉卡片状态下拉

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 任务页 UI 精修 |
| 对应文件 | `vue-module/src/views/workbench/TaskPage.vue`、相关 workbench 样式 |

### Prompt（原始）

```text
这个删掉
仔细打磨一下这个页面的设计
要求加强视觉效果
/redesign-existing-projects
```

### AI 返回（原始）

```text
已完成两件事：

1. 删掉任务卡片上的「待办」状态下拉
   状态仍可通过顶部状态 Tab 筛选，以及编辑弹窗里修改。

2. 加强整页视觉（沿用现有 Ant Design + CSS 变量体系）
   - 氛围：更强的光晕 blob + 细颗粒纹理
   - 统计卡 / 面板：玻璃质感、色条、悬浮抬升
   - 状态 Tab：色点区分 + 激活态阴影
   - 任务卡：左侧状态色条、入场动画、hover 抬升与更深阴影
   - 按钮 / 搜索：更明确的 hover / focus 反馈

改状态请用「编辑任务」里的状态字段。
```

---

## 10. 独立任务 / 绑定项目检索

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 任务归属筛选 |
| 对应文件 | `vue-module/src/views/workbench/TaskPage.vue` |

### Prompt（原始）

```text
这里能不能再加一个检索
一个是独立的任务 一个是绑定项目的任务
```

### AI 返回（原始）

```text
已在任务工具栏加上「任务归属」筛选，位置在优先级右侧：

- 独立任务：未关联项目（projectId 为空）
- 绑定项目：已关联项目
- 可清空，回到全部

进入某个项目空间后会自动隐藏该筛选项（该空间内任务都属于该项目）。刷新页面即可看到。
```

---

## 11. AI 智能规划项目任务

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 按项目主题拆解阶段/任务，预览后落库 |
| 对应文件 | `springboot-module/ai-module/.../AiPlanService.java`<br>`vue-module/src/apis/AiApi.js`<br>`vue-module/src/views/workbench/TaskPage.vue` |

### Prompt（原始）

```text
思考是否可以引入 AI，根据项目主题和描述智能规划项目任务阶段功能？
```

### AI 返回（原始节选 · 方案）

```text
做成 Tool 更好——但规划本身不该是 Tool，落库才是。

| 职责 | 形态 |
|------|------|
| 根据 name/description 生成阶段计划 | LLM 结构化输出（JSON） |
| 读项目 / 批量写任务 / 按 plan_batch 撤销 | @Tool |

推荐落地：
- MVP：项目页「AI 规划」按钮 + 结构化规划 Service + 预览确认写入
- Tool 适合承载工作台能力；规划用 LLM 结构化输出
```

### AI 返回（原始 · 落地增强）

```text
已加上项目截止时间（必填），并按剩余天数改拆解节奏：

| 剩余 | 节奏 |
|------|------|
| ≤14 天 | 2～3 阶段，极简必做（两周冲刺） |
| 15～45 天 | 3～4 阶段，均衡 |
| >45 天 | 4～6 阶段，可细分（约两月） |

阶段/步骤会带建议 dueTime，落板时写入任务截止日。
```

---

## 12. 会议记录 + AI 纪要整理

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 会议 CRUD、材料上传、AI 摘要 |
| 对应文件 | `doc/sql/wb_meeting.sql`、`wb_meeting_permission.sql`<br>`WbMeetingController`、`AiMeetingSummaryService`<br>`MeetingPage.vue`、`MeetingApi.js` |

### Prompt（原始）

```text
这个项目是否可以增加会议历史记录功能
例如星期一开的产品研讨会，然后可以把文件上传这个平台
例如会议上讨论 12345 个，然后 AI 根据文件整理好框架
```

### AI 返回（原始）

```text
会议功能已经写完了。

### 已落地内容
| 层 | 内容 |
|---|---|
| SQL | doc/sql/wb_meeting.sql、wb_meeting_permission.sql；全量脚本已同步 |
| 后端 | WbMeetingController（CRUD + 上传）、AiMeetingSummaryService（一键整理） |
| 前端 | MeetingPage.vue、MeetingApi.js、菜单图标「会议」 |

### 使用流程
新建会议（标题 + 时间）→ 上传材料 → 点「AI 整理」→ 概要写入记录。
侧栏「会议记录」→ /meeting（需执行 SQL 后重新登录）。
```

---

## 13. 登录后看不到会议菜单（权限）

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 会议菜单 RBAC 授权 |
| 对应文件 | `doc/sql/wb_meeting_permission.sql`<br>`SysPermissionMapper.xml`<br>`role_permission` 表 |

### Prompt（原始）

```text
为什么登录以后看不到会议纪要这块
```

### AI 返回（原始节选）

```text
sys_permission 有数据没问题；侧栏要显示，还得看你的角色有没有挂上这些权限。

菜单查询：sys_user → role_permission → sys_permission

而 wb_meeting_permission.sql 只给了角色 0 / 1 / 2，库里的超级管理员是 role_id = 5（没有 0），
所以 ROOT 账号执行完脚本也看不到。

补授权示例：
INSERT IGNORE INTO role_permission (role_id, permission_id)
VALUES (5, 330), (5, 331), (5, 332), (5, 333), (5, 334), (5, 335);

然后重新登录（菜单在登录时写入，改库不会自动刷新）。
```

---

## 14. 任务附件状态

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 附件：未处理 / 处理中 / 处理完成 |
| 对应文件 | `doc/sql/wb_task_attachment_status.sql`<br>`attachments[].status`（JSON）<br>`TaskPage.vue` |

### Prompt（原始）

```text
考虑是否给任务文件添加状态，例如处理完成、未处理、处理中？
```

### AI 返回（原始）

```text
说得对。按你们现有习惯，已有库变更应新建增量脚本；wb.sql 只维护全量建表定义。

已补：doc/sql/wb_task_attachment_status.sql

- 已有库执行该文件即可同步列注释
- 无新物理列：status 写在 attachments JSON 里
- wb.sql 里的注释更新可以保留，方便新库直接建对
```

---

## 15. 附件列表弹窗 + kkFileView

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 多附件管理：预览 / 下载 / 移除 |
| 对应文件 | `vue-module/src/views/workbench/TaskPage.vue`<br>`vue-module/.env.development`（`VITE_KK_FILE_VIEW_BASE`） |

### Prompt（原始）

```text
这边点击完直接一个弹窗 然后是文件列表 一个项目可能不止一个文件
然后有预览使用 kkFileView 下载 移除 这样
这个设计会不会更好
```

### AI 返回（原始）

```text
会更好，尤其是你已经预期「一个任务多个附件 + 预览 / 下载 / 移除」这套操作时。

已改完。卡片点「N 附件」会打开列表弹窗，支持多文件与预览 / 下载 / 移除。

改动要点
- 去掉气泡，改为附件列表弹窗（标题带任务名）
- 预览：走 kkFileView（新标签页）
- 下载：打开原 OSS 链接
- 移除：二次确认；项目已归档时隐藏移除

配置：vue-module/.env.development
VITE_KK_FILE_VIEW_BASE=http://127.0.0.1:8012
```

---

## 16. 前置任务（先修）评估

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 任务依赖（评估未强制落地硬门禁） |
| 对应文件 | 相关字段参考：`wb_task.parent_task_id`（阶段树，≠ 前置依赖） |

### Prompt（原始）

```text
是否考虑在带项目的任务下增加前置任务
类似大学课程逻辑 部分课程有先修课程 先评估
/redesign-existing-projects
```

### AI 返回（原始节选）

```text
评估结论：值得做，但建议做成「项目内软阻塞提示」，不要做成课程那种硬门禁；
且不要和现有的 parent_task_id 混用。

parent_task_id 是「阶段 → 步骤」树（组合关系），不是先后约束。
更贴近 Jira 的 Blocks / is blocked by：提示 + 可选软限制。

不适合一上来就做满的原因：个人场景硬门禁性价比低；与 parent_task_id 易混；UI/防环成本不低。
```

---

## 17. 首页「今日重点」改版

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 工作台首页信息架构 |
| 对应文件 | `vue-module/src/views/workbench/DashboardPage.vue` |

### Prompt（原始）

```text
根据项目主题完善首页设计，不要堆砌过多数据
例如可以优先显示三天内要结束的任务，显示今天的日程安排
display 的数据要展示重点，不然用户体验感很差
/redesign-existing-projects
```

### AI 返回（原始）

```text
首页已按工作台主题重做：从「欢迎墙」改成「今日重点」。

改了什么
- 问候区压缩成一行摘要（逾期 / 三天内截止 / 今日日程）
- 三天内截止：未完成且截止日 ≤ 今天+2 天（含逾期），按紧急度排序，最多 6 条
- 今日日程：当天安排 + 命中今天的每周重复，最多 6 条
- 去掉装饰性 SVG 和数据墙；空状态 / 骨架屏 / 跳转事务与日程
```

---

## 18. 首页视觉增强（桌面仪器）

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | Dashboard 视觉层次 |
| 对应文件 | `vue-module/src/views/workbench/DashboardPage.vue` |

### Prompt（原始）

```text
优化首页的样式设计
/frontend-design /redesign-existing-projects
需要增强视觉效果 要有部分图案 与核心要素
```

### AI 返回（原始）

```text
首页视觉已按「桌面仪器」方向升级，数据逻辑未改，主要变化在 DashboardPage.vue。

图案氛围：背景叠加网格、斜纹、流线节点、几何同心环、品牌六边形与颗粒
核心要素：新增「逾期 / 三日内 / 今日程」三格指标条；日签强化双轨道与角标
动效与适配：流线虚线、光晕漂移、卡片错落入场；prefers-reduced-motion 已处理
```

---

## 19. 首页加时钟

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 首页 Hero 饱满度 |
| 对应文件 | `vue-module/src/views/workbench/DashboardPage.vue` |

### Prompt（原始）

```text
这个页面放一个时钟 显得更加饱满 有设计
/redesign-existing-projects
```

### AI 返回（原始）

```text
已在首页右侧加上与日签同风格的玻璃线框时钟。

改动要点
- Hero 右侧改为「时钟 + 日签」并排：模拟表盘 + HH:mm:ss 数字读数
- 每秒刷新，秒针与冒号会动，构图更饱满
- 材质/圆角/青调阴影与现有日签一致，窄屏会自动换行
```

---

## 20. 首页上方精修

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | Dashboard hero 精修 |
| 对应文件 | `vue-module/src/views/workbench/DashboardPage.vue` |

### Prompt（原始）

```text
在现有项目的基础上让首页上方的设计更精致一点
```

### AI 返回（原始）

```text
首页上方已按现有青绿玻璃语言做了保留式精修，主要改动在 DashboardPage.vue 的 hero 区：

1. 品牌行收束 — 品牌与日期时间并排成 masthead
2. 问候更突出 — 标题略放大、姓名渐变更干净
3. 指标条统一 — 统一玻璃卡片 + 左侧色轨
4. 日签卡更细 — 光泽层、内嵌时间读数、悬停抬升更克制
5. 分区更清楚 — hero 底部加细分割线
```

---

## 21. 登录页文案与配色

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 登录 / 注册体验 |
| 对应文件 | `vue-module/src/views/system/user/AuthPage.vue` |

### Prompt（原始）

```text
@vue-module/src/views/system/user/AuthPage.vue
优化一下这个页面，背景图不要改，补充点文字信息，可以优化配色等
/redesign-existing-projects
```

### AI 返回（原始）

```text
按你的三点改完了：

1. 文案 — 去掉「权限与账号」这类管理端表述；表单里只保留面向使用者的短句
2. 背景 — 左侧大段文案整块删掉，遮罩改轻，插画区域基本露出来
3. 配色 — 统一用项目里的 --auth-accent（#0891b2）等变量，和顶栏 / 内容区同系
```

---

## 22. 登录页办公桌背景

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | Auth 页氛围与办公元素 |
| 对应文件 | `AuthPage.vue`、`BackgroundAuthPage.png` / `auth-desk-scene.png` 等静态资源 |

### Prompt（原始）

```text
@AuthPage.vue
/redesign-existing-projects 继续优化登录页的背景样式
可从常见的办公工具如电脑、办公桌、咖啡杯等入手，也可以结合其余工作内容构思
要求现代化 有设计感
```

### AI 返回（原始）

```text
登录页背景已按「办公桌工作台」方向升级，主要变化：

1. 接入插画 — 启用 BackgroundAuthPage.png（显示器 / 键盘 / 咖啡杯等）
2. 桌面层次 — 窗光、桌面纹理、细网格、grain
3. 浮动道具 — SVG 咖啡杯、笔记本、笔，以及日程便签
4. 笔记本外框 — 产品示意嵌进 laptop bezel
5. 响应式 — 窄屏隐藏道具，减弱插画，避免干扰登录
```

### 补充 Prompt（同主题迭代）

```text
页面右边后面有空白
继续优化设计：减少大量文字背景描述，以图案为核心，把办公元素突显
拉高对比度，与项目内页主题配色对应
/frontend-design /redesign-existing-projects
```

### AI 返回（原始节选）

```text
已把左侧做成完整「办公桌工作台」场景：
1. 真实桌面氛围图 — /auth-desk-scene.png
2. 立体桌面 — CSS 透视台面 + 桌沿 + 高光
3. 笔记本强化 — 铰链、键盘区、入场 settle 动画
4. 桌面物件 — 咖啡杯、笔记本、笔、鼠标、绿植围在电脑两侧
```

---

## 23. 登录页突显核心功能

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | Auth 页产品示意（待办 / 日程 / 看板） |
| 对应文件 | `vue-module/src/views/system/user/AuthPage.vue`<br>`vue-module/index.html`（字体） |

### Prompt（原始）

```text
太单调了这样
/redesign-existing-projects 别人不知道你核心功能啊 先构思清楚再设计
```

### AI 返回（原始）

```text
登录页已按计划改完，遮住右侧表单后仍能读出「待办 / 日程 / 看板」。

改动要点
- AuthPage.vue：左右一体构图——左侧品牌 + 价值主张 + CSS 产品示意台，右侧登录
- 示意台展示今日待办、本周日程、迷你看板；带错落入场与日程扫光
- CTA 改为「进入工作台」
- index.html：引入 DM Sans + Noto Sans SC

登录/注册/验证码逻辑未改。
```

---

## 24. 导航改为悬浮 Dock

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 全局导航交互 |
| 对应文件 | 布局组件（侧栏 / 顶栏 / 命令面板相关 Vue 文件） |

### Prompt（原始）

```text
导航栏是否可以采取更加独特快捷的形式
/redesign-existing-projects
```

### AI 返回（原始）

```text
可以，已经改成更独特、更快的形态。

改动
1. 悬浮胶囊 Dock — 左侧圆角玻璃胶囊，不贴边、不全高占栏；纯图标直达
2. 命令优先 — 顶栏居中 Ctrl/⌘ K 跳转条；面板增加「最近」访问
3. 更省空间 — 内容区只让出约 80px；顶栏去掉重复的 Workbench 标题

菜单树、权限、路由、退出、头像逻辑未动。
```

---

## 25. 统一 Ant Design Vue 图标

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 全站功能性图标统一 |
| 对应文件 | `vue-module/package.json`（`@ant-design/icons-vue`）<br>`menuIcons.js` 及各业务页 |

### Prompt（原始）

```text
把所有的图标都改为 antdvue 的图标
pnpm install --save @ant-design/icons-vue
```

### AI 返回（原始）

```text
全部功能性图标已换成 @ant-design/icons-vue。

已完成：
- 依赖：@ant-design/icons-vue（已在 package.json）
- 共享组件：导航、列表、行操作、编辑器、menuIcons.js
- 业务页：任务、日程、RBAC 管理、AI 对话、错误页、个人资料

保留原 SVG（非 UI 图标）：BrandMarkView、装饰插画、CSS 噪点纹理
```

---

## 26. 整体配色分析

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 设计系统色板对齐 |
| 对应文件 | CSS 变量（如 `--auth-accent`、`--color-surface-*`）、`AuthPage.vue` |

### Prompt（原始）

```text
分析一下这个项目的整体配色是什么
```

### AI 返回（原始节选）

```text
登录页色值本身已经和内部基本一致（#0891b2 / #0e7490 / #22d3ee），
要「呼应」主要不是换色相，而是把分区逻辑对齐：顶栏深青 = 品牌氛围，内容区浅青白 = 表单面。

推荐：浅色入场，对齐内部三区
- 背景氛围：#0c4a5e → #0e7490 → #0891b2，淡出到 #f0f7fb
- 表单卡片：近白半透明，边框 rgba(34,184,207,0.14~0.24)
- 主按钮：135deg #22d3ee → #0891b2
- 文字：#0c2a42 / #3d6478 / #6a8fa3

一句话：保持冷青主色不动；登录页做成「顶栏深青氛围 + 内容区浅青白表单」。
```

---

## 27. 前端响应式组件评估

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 响应式基础设施（评估） |
| 对应文件 | 建议落地：`composables/useBreakpoint.js`、`variables.css` 断点 |

### Prompt（原始）

```text
是否考虑前端增加响应式组件设计
在每个页面调用，这样不要一直重复写
```

### AI 返回（原始节选）

```text
建议做，但别做成「每个页面包一层的响应式组件」。

更合适的拆法：
1. 断点规范：统一 sm/md/lg/xl
2. useBreakpoint：isMobile / isTablet / breakpoint
3. 布局积木（可选）：PageShell、列表筛选栏
4. 页面专属样式仍留在页面

不推荐：<ResponsiveWrapper> 包所有页面。
值得做：统一断点 + useBreakpoint + 少量高频布局组件。
```

---

## 28. ECharts 进度可视化评估

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-19 |
| 对应功能 | 项目进度百分比 / 状态分布图（评估，按要求未直接改代码） |
| 对应文件 | 推荐落点：`DashboardPage.vue`、`TaskPage.vue` 项目空间；依赖 `echarts` / `vue-echarts` |

### Prompt（原始）

```text
思考一下项目那些地方适合加入更有视觉效果的图标
使用 echart 组件来展示（pnpm 方式）
比如说项目进度显示百分比什么的
你先评估一下 不要执行代码
```

### AI 返回（原始节选）

```text
结论：项目里还没有 echarts / vue-echarts。最适合上 ECharts 的是：
首页 Dashboard 和任务页的项目空间（完成率）。

P0：
1. Dashboard — 环形图：任务状态占比；柱状图：今/明/后三日截止量
2. TaskPage · 项目空间 — 环形进度：完成数 / (总数 − 已取消)

数据可用现有 pageTasksApi + 前端聚合，不必先做新 API。
```

---

## 29. 每周重复日程不显示 Bug

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-19 |
| 对应功能 | 周重复日程在日历格子展开 |
| 对应文件 | `vue-module/src/views/workbench/EventPage.vue` |

### Prompt（原始）

```text
修复 bug
如果是每周的话 下面的星期一应该都显示啊
```

### AI 返回（原始）

```text
已修好。

原因：日程页只按 startTime 的日期往格子里塞，没按「每周」规则展开。
首页「今日程」本来有展开逻辑，日历页没有。

改动：在 EventPage.vue 里加上与首页一致的判断——repeatType=1 时，
从开始日之后的每个匹配星期几都会显示。例如「周工作例会」设在周一，
6、13、20、27 号都会出现。
```

---

## 30. 后端模块体检与清理

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 死代码清理、API 归属 |
| 对应文件 | Controllers；`vue-module/src/apis/{AiApi,TaskApi}.js` |

### Prompt（原始）

```text
检查项目后端模块，明确以下几个事项
1. 所有的 controller 是不是都有接口文档的注解
2. 有没有写出来但是没有用到的方法
3. 有没有没有用到的文件
4. 有无逻辑不合理的地方
```

### AI 返回（原始）

```text
前端已对齐，结论如下：

本来就已对齐（无需再删）
- UserProfileApi 只有 uploadAvatarApi
- CaptchaApi 只有 fetchCaptchaBase64Api

本次清理
- 删除未使用的重复接口 chatWithAiApi（与流式 chatWithAiStream 同打 /ai/chat）
- 将 planPreviewApi / planApplyApi 从 TaskApi 挪到 AiApi，TaskPage 改为从 AiApi 引入
```

---

## 31. 根 README 编写

| 项 | 内容 |
|---|---|
| 日期 | 2026-07-18 |
| 对应功能 | 项目文档（评分 C(2) 配套） |
| 对应文件 | `README.md`、`doc/api.md` / `doc/api接口文档.md` |

### Prompt（原始）

```text
认真阅读项目全部内容
给项目写一个 README.md
包含项目简介、技术栈、本地运行指南
```

### AI 返回（原始）

```text
已在仓库根目录写好 README.md，主要内容包括：

1. 项目简介 — 个人工作台定位、数据隔离与功能模块一览
2. 技术栈 — Spring Boot 3 / Sa-Token / MyBatis-Plus / Redis / Spring AI
           与 Vue 3 / Vite / Ant Design Vue / Pinia 及模块划分
3. 本地运行指南 — 环境依赖、SQL 执行顺序、配置与启动命令、首次使用说明

并补充了目录结构与简要开发约定。
```

---

## 附：提交说明

1. 本文件路径：`doc/prompt_log.md`
2. 每条均满足：Prompt + AI 原始输出（代码块）+ 功能/文件标注
3. 若老师要求截屏：可对 Cursor 历史会话补充截图，与本文一并提交
4. 未收录的对话多为「任务卡住 / skill 安装 / 重复微调」等，不影响功能对照审查；需要可再续编
