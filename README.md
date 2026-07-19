# PersonalWorkbench · 个人工作台

## 1 项目概述

### 1.1 项目介绍

基于 Spring Boot 3.5 + Vue 3 的个人效率工作台，涵盖任务管理、日程安排、项目追踪、会议记录与 DeepSeek AI 助手。

数据按用户隔离，不做多人协作。系统管理（RBAC）与工作台功能并存：普通用户聚焦工作台，管理员保留用户 / 角色 / 权限运维能力。

### 1.2 技术栈

#### 后端

| 技术 | 版本 | 说明 |
| --- | --- | --- |
| Java | 17 | — |
| Spring Boot | 3.5.13 | Web、Data Redis、Test |
| MyBatis-Plus | 3.5.14 | ORM，Mapper 扫描 `com.moses.**.mapper` |
| Sa-Token | 1.45.0 | Token 鉴权（Redis 持久化），权限码校验 |
| MySQL | 8.3.0 | 数据持久化 |
| Redis | — | 会话 Token / AI 对话记忆，Lettuce 连接池 |
| Spring AI | 1.1.8 | DeepSeek 模型接入（`deepseek-chat`） |
| Knife4j | 4.5.0 | OpenAPI 文档 UI（`/doc.html`） |
| springdoc | 2.8.13 | OpenAPI 3 文档引擎 |
| Kaptcha | 2.3.2 | 图形验证码 |
| 阿里云 OSS | 3.17.4 | 头像 / 任务附件存储（可选） |

Maven 多模块结构：

```
springboot-module/
├── start-module        # 启动入口 RunApplication（端口 8090）
├── common-module       # 通用配置、工具类、统一响应信封（ResultConfig）、全局异常处理
├── system-module       # 认证、用户、角色、权限（RBAC）
├── workbench-module    # 任务 / 日程 / 项目 / 会议
└── ai-module           # AI 对话（SSE 流式）、智能规划、会议摘要
```

#### 前端

| 技术 | 版本 | 说明 |
| --- | --- | --- |
| Vue 3 | 3.5.38 | Composition API |
| Vite | 8.0.16 | 开发服务器 + 构建 |
| Vue Router | 5.1.0 | 静态路由 + 后端菜单动态路由 |
| Pinia | 3.0.4 | 状态管理 |
| Ant Design Vue | 4.2.6 | UI 组件库 |
| Axios | 1.18.1 | HTTP 请求，`/api` 代理到后端 |
| ECharts | 6.1.0 | 图表（`vue-echarts`） |
| wangEditor | 5.1.23 | 富文本编辑器 |
| dayjs | 1.11.21 | 日期处理 |
| pnpm | — | 包管理（Node `^22.18.0` 或 `>=24.12.0`） |

## 2 功能概览

| 模块 | 核心能力 |
| --- | --- |
| 认证与个人中心 | 登录 / 注册（图形验证码可选）、资料维护、密码修改、头像上传 |
| 系统管理（RBAC） | 普通用户 / 管理员分表管理；角色 CRUD；权限（菜单+功能码）CRUD；角色-权限绑定；登录后菜单按角色动态下发 |
| 工作台 · 任务 | 任务 CRUD，按项目分组，按状态筛选（待办/进行中/已完成/已取消）；支持优先级、截止日、标签；任务附件上传（OSS）及处理状态追踪 |
| 工作台 · 日程 | 日程 CRUD，支持全天事件、起止时间、地点、周重复规则、可选关联任务 |
| 工作台 · 项目 | 项目 CRUD，作为任务的归属容器；支持进行中/已归档状态 |
| 工作台 · 会议 | 会议 CRUD，记录标题/时间/地点/参会人；上传会议材料（OSS），由 AI 自动整理摘要 |
| AI 对话 | DeepSeek 流式对话（SSE），多会话管理（创建/切换/删除），Redis 持久化记忆 |
| AI 智能规划 | 输入目标 → LLM 拆解步骤预览 → 确认后批量落为任务 |
| AI 会议整理 | 上传会议材料后，AI 自动生成结构化摘要并写回会议记录 |

## 3 本地运行

### 3.1 环境要求

- JDK 17+
- Maven 3.8+
- Node.js（`^22.18.0` 或 `>=24.12.0`） + [pnpm](https://pnpm.io/)
- MySQL 8.x（本地实例）
- Redis（默认 `localhost:6379`，无密码）

### 3.2 初始化数据库

创建数据库：

```sql
CREATE DATABASE personal_workbench DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

在库中按执行SQL文件：
一个为数据+结构，一个为仅结构
### 3.3 配置后端

编辑 `springboot-module/start-module/src/main/resources/application.yml`，按本机环境修改密码即可。  
**本地与服务器 JDBC 连接参数已对齐**（时区 / `allowPublicKeyRetrieval` / `useSSL`）；Docker 只覆盖 host（`mysql`）与密码（`MYSQL_ROOT_PASSWORD`）。

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/personal_workbench?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: 你的密码
  data:
    redis:
      host: localhost
      port: 6379
```

**AI / DeepSeek（必填）：** 必须设置环境变量 `DEEPSEEK_KEY`。未配置时 Spring AI 会在启动阶段直接失败，后端无法启动。

**OSS 附件上传（可选）：** 设置环境变量 `OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`，并确认 `aliyun.oss.*` 配置与桶一致。

### 3.3.1 本地 vs 服务器：允许不同的只有这些

| 项 | 本地开发 | 服务器 Docker |
| --- | --- | --- |
| MySQL host | `localhost` | `mysql`（compose 服务名） |
| Redis host | `localhost` | `redis` |
| DB 密码 | `application.yml` / `SPRING_DATASOURCE_PASSWORD` | `deploy/.env` → `MYSQL_ROOT_PASSWORD` |
| 前端 API | Vite 代理 `/api` → `8090` | Nginx `/api/` → `backend:8090`（须 `underscores_in_headers on`，否则丢弃 `rbac_token`） |
| 附件预览 | 本机 kkFileView `http://127.0.0.1:8012` | Nginx `/preview` → `kkfileview:8012`（同域，勿再指向本机 8012） |
| JDBC 其它参数 | **相同** | **相同** |
| Token 头名 | `rbac_token` | `rbac_token`（与本地一致） |

### 3.4 启动后端

```bash
cd springboot-module
mvn spring-boot:run -pl start-module
```

或在 IDE 中运行 `com.moses.RunApplication`。

- 服务地址：`http://localhost:8090`
- API 文档：`http://localhost:8090/doc.html`

### 3.5 启动前端

```bash
cd vue-module
pnpm install
pnpm dev
```

Vite 已将 `/api` 代理到 `http://localhost:8090`（`/api` 前缀自动去除）。浏览器访问 `http://localhost:5173`。

附件在线预览依赖 [kkFileView](https://kkfileview.keking.cn/)：本地在 `vue-module/.env.development` 配置 `VITE_KK_FILE_VIEW_BASE=http://127.0.0.1:8012`，并本机启动 kkFileView；服务器由 Compose 拉起 `pw-kkfileview`，前端生产构建写死同域 `/preview`。

生产构建：

```bash
pnpm build
```

产物在 `vue-module/dist`。

### 3.6 首次使用

- 注册已开启（`app.auth.register-enabled: true`），可直接注册普通用户（`USER` 角色）体验工作台。
- 种子账号见 `PersonalWorkbench-SQL.sql` 中的 `sys_user` 记录：`admin`（管理员）、`root`（超级管理员）。密码以库中实际 BCrypt 值为准；若无法登录，走注册或库中重置。
- 登录后菜单由后端权限动态下发：普通用户可见首页、工作台（日程 / 事务 / 项目）、个人中心、AI；系统管理菜单仅管理员可见。

## 4 生产部署（阿里云 ECS + Docker + GitHub Actions）

推送到 `master` 后，GitHub Actions 将代码 **scp 到 ECS**（避免服务器直连 GitHub 失败），再 SSH 执行 `docker compose up -d --build`。浏览器访问 `http://ECS公网IP`（或已解析的域名）。

### 4.1 阿里云一次性准备

1. ECS 已安装 Docker Engine 与 Compose 插件，并绑定公网 IP。
2. 安全组入方向：放行 **TCP 80**（站点）、**TCP 22**（SSH）；不要对公网开放 3306 / 6379 / 8090。
3. 克隆仓库到服务器（示例路径 `/opt/PersonalWorkbench`）：

```bash
sudo mkdir -p /opt && sudo git clone https://github.com/Moses0531/PersonalWorkbench.git /opt/PersonalWorkbench
cd /opt/PersonalWorkbench/deploy
cp .env.example .env
# 编辑 .env：必须设置 MYSQL_ROOT_PASSWORD、DEEPSEEK_KEY；OSS_* 按需
nano .env
docker compose --env-file .env up -d --build
```

4. 浏览器打开 `http://公网IP` 验证。若使用域名，在阿里云 DNS 添加 A 记录指向该 IP。

### 4.2 GitHub Secrets（自动部署）

在仓库 **Settings → Secrets and variables → Actions** 中配置：

| Secret | 说明 |
| --- | --- |
| `SSH_HOST` | ECS 公网 IP 或域名 |
| `SSH_USER` | SSH 用户名（如 `root`） |
| `SSH_PRIVATE_KEY` | 对应私钥全文 |
| `SSH_PORT` | 可选，默认 `22` |
| `DEPLOY_PATH` | 服务器仓库路径，如 `/opt/PersonalWorkbench` |
| `DEEPSEEK_KEY` | **必填**：写入服务器 `.env` 并注入后端容器 |
| `MYSQL_ROOT_PASSWORD` | 建议填写，与服务器 `.env` 中数据库密码一致 |
| `OSS_ACCESS_KEY_ID` | **头像/附件上传必填**：写入服务器 `.env` 并注入后端容器 |
| `OSS_ACCESS_KEY_SECRET` | **头像/附件上传必填**：与上一行成对配置 |

配置完成后，日常只需本地：

```bash
git push origin master
```

即可触发 [Deploy](.github/workflows/deploy.yml) 自动上传代码并重建容器；PR / push 还会跑 [CI](.github/workflows/ci.yml) 编译检查。无需再登录服务器拷代码。

## 5 项目结构

```
PersonalWorkbench/
├── deploy/                 # Docker Compose 部署（nginx / backend / mysql / redis / kkFileView）
├── .github/workflows/      # CI + 自动部署
├── doc/                    # SQL 脚本
├── springboot-module/      # 后端 Maven 多模块
│   ├── start-module/       # 启动入口 + application.yml
│   ├── common-module/      # 通用配置与工具
│   ├── system-module/      # RBAC 认证授权
│   ├── workbench-module/   # 工作台业务
│   └── ai-module/          # AI 能力
└── vue-module/             # 前端 Vue 3 项目
    ├── src/
    │   ├── apis/           # API 封装
    │   ├── components/     # 通用组件
    │   ├── router/         # 路由（静态 + 动态）
    │   ├── stores/         # Pinia 状态
    │   ├── utils/          # 工具函数（含 kkFileView 预览）
    │   └── views/          # 页面组件
    └── vite.config.js      # Vite 配置 + /api 代理
```

## 6 开发约定

- 接口统一使用 `ResultConfig` 信封返回；业务数据通过 `StpUtil.getLoginIdAsLong()` 做用户隔离，禁止仅凭资源 ID 越权访问。
- 前端页面通过 `views/**/*.vue` 与菜单权限中的 `router_name` / `component_path` 动态挂载，新增页面时保持组件名与权限种子一致。
- 鉴权 Token 通过请求头 `rbac_token: Bearer <token>` 传递（与 Sa-Token 配置一致）。
- 数据库无物理外键，通过索引 + 应用层逻辑保证数据一致性；统一使用物理删除（无 `is_deleted` 字段）。
