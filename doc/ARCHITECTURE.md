# Cycling Lab Web 架构设计

> 版本：v0.3 · 2026-06-05
> 状态：M0 已落地（后端 jar + 前端 dist 部署到 19000 反代,见 SERVER_INSTALLATION.md）;M1 详细设计（档案 + 训练课库）进入评审
> 适用范围：将现有 `cycling-lab`（以 Markdown + 本地脚本为主的个人骑行训练实验室）改造为多用户 Web 应用。

---

## 1. 背景与现状

`cycling-lab` 目前是一个基于文件系统的个人训练管理仓库：

- `profile/`：骑手长期档案（年龄、身高、体重、最大心率、FTP、设备、目标）。
- `plans/library/`：可复用训练课模板（endurance / recovery / intervals / testing / strength / outdoor）。
- `plans/<year>/week-NN/`：周计划与每日具体课表（Markdown 表格 + 引用模板）。
- `workouts/zwo/`：可导入 Zwift / TrainingPeaks 等平台的 `.zwo` 课程文件。
- `training/<year>/week-NN/fit/`：原始 FIT 训练记录。
- `training/<year>/week-NN/notes/`：基于 `scripts/analyze_fit.py` 生成的训练分析笔记。
- `training/<year>/week-NN/screenshots/`：码表 / App 截图。
- `review/<year>/week-NN-review.md`：周复盘。
- `skills/`：Codex 技能说明，FIT 分析逻辑沉淀在 `.codex/skills/cycling-fit-analysis/scripts/analyze_fit.py`。

### 1.1 现存痛点

1. **缺少可视化**：FIT 分析产物是静态 Markdown 表格，无法做趋势、对比和交互。
2. **检索/统计困难**：周训练量、心率漂移、踏频占比等指标散落在多个 md 文件中。
3. **计划与执行脱节**：周计划里"实际记录"列是手填文字，无法回链到具体的训练数据。
4. **单人单端**：纯文件系统形式，无法在手机 / 异地访问。
5. **无法支持他人**：要分享模板、给队员排课都很麻烦。

### 1.2 改造目标

把 `cycling-lab` 改造为「**个人起底、可平滑扩展为多用户**」的 Web 应用，复用现有 FIT 分析与 ZWO 课程文件能力，新增可视化、AI 辅助与跨端访问。

---

## 2. 目标与非目标

### 2.1 目标（In Scope）

- G1. 现有能力 Web 化：档案、训练课库、周计划、训练记录、复盘、ZWO 课程文件的增删改查与展示。
- G2. FIT 上传 + 自动分析：上传 `.fit` 后自动解析并入库，生成可交互图表（心率 / 功率 / 踏频曲线、区间分布、10 分钟分段等）。
- G3. ZWO 课程文件生成器：通过表单配置热身 / 主训练 / 间歇结构，预览并下载 `.zwo`。
- G4. AI 辅助：基于骑手档案 + 近期训练数据，生成 / 调整周计划；对 FIT 数据给出文字解读。
- G5. 多用户基础：账号、登录、用户隔离、权限分级（v0.1 至少支持「普通用户 / 管理员」）。

### 2.2 非目标（Out of Scope，v0.1 不做）

- 实时心率 / 功率流式接入（ANT+ / BLE）。
- 与 Strava、Garmin Connect、Wahoo 等三方平台的双向同步（v0.1 阶段只考虑 FIT 导入）。
- 移动端原生 App（v0.1 仅 Web，PWA 可选）。
- 社交、Feed、好友体系。

---

## 3. 技术栈选型

### 3.1 后端

| 维度 | 选型 | 说明 |
| --- | --- | --- |
| 语言 / 运行时 | **Java 21（LTS）+ Spring Boot 3.3+** | 长期支持，虚拟线程（Virtual Threads）适合 I/O 密集型场景（FIT 解析、对象存储）。 |
| Web 框架 | Spring Web MVC（初期）/ 后续可平滑切 WebFlux | 团队熟悉度高、调试简单；虚拟线程下 MVC 已能覆盖大多数并发场景。 |
| 持久层 | **Spring Data JPA + Hibernate 6** + **PostgreSQL 16** | 结构化数据；JSONB 字段保存 FIT 元数据。 |
| 迁移 | **Flyway** | SQL 迁移，纳入版本控制。 |
| 缓存 | **Redis 7** | 会话 / 热点缓存 / 排行榜类聚合查询。 |
| 安全 | **Spring Security 6 + JWT（Access + Refresh）** | 无状态，适配前后端分离。 |
| 对象存储 | **MinIO**（S3 兼容 API） | 存 `.fit` 原文、`.zwo`、截图、md 笔记。 |
| 异步任务 | **Spring Modulith @AsyncTask / Spring Batch（轻量）** | FIT 解析、图表渲染、AI 摘要均可异步。 |
| 可观测性 | **Micrometer + OpenTelemetry** + Grafana / Prometheus | 后续接入。 |
| 鉴权 | 自有账号体系（v0.1） | v0.2 可考虑 OIDC。 |
| AI 集成 | **Spring AI** | 统一抽象多家 LLM Provider（OpenAI / Anthropic / 国产模型），便于切换。 |

### 3.2 前端

| 维度 | 选型 | 说明 |
| --- | --- | --- |
| 构建 | **Vite 5+** | 快、HMR 好。 |
| 框架 | **React 19 + TypeScript 5** | 生态最广，匹配「技术栈新一些」的要求。 |
| 路由 | **TanStack Router**（或 React Router 7） | 类型安全路由。 |
| 数据层 | **TanStack Query v5** + **Zod** | 服务端状态、请求 / 响应 schema 校验。 |
| 表单 | **React Hook Form + Zod** | 计划编辑器、ZWO 生成器。 |
| UI 组件 | **shadcn/ui + Tailwind CSS 4** | 现代、轻量、可复制魔改。 |
| 图表 | **Recharts**（轻量）/ **Apache ECharts**（复杂可视化） | 训练曲线用 Recharts，仪表盘用 ECharts。 |
| 状态 | **Zustand**（轻量全局态） + 组件局部 state | 避免 Redux 复杂度。 |
| 国际化 | **react-i18next** | 现有文档多为中文，UI 准备 i18n。 |
| 富文本 / Markdown | **MDXEditor / TipTap** | 训练笔记、复盘编辑。 |
| 测试 | **Vitest + Testing Library + Playwright** | 单元 + E2E。 |

### 3.3 基础设施

- **容器化**：Docker + Docker Compose（本地一键起 `postgres / redis / minio / backend / frontend`）。
- **CI/CD**：Jenkins 为主，GitHub Actions 可作为轻量备选；流水线跑后端测试、前端测试、镜像构建、制品归档与环境部署。
- **反向代理**：Traefik 或 Nginx。
- **LLM Provider**：通过 Spring AI 配置，**默认不绑定供应商**；本地可对接 Ollama。

---

## 4. 总体架构

### 4.1 逻辑视图

```
┌──────────────────────────────────────────────────────────┐
│                      Browser (React SPA)                 │
│   计划编辑 / 训练记录 / FIT 可视化 / ZWO 生成 / AI 助手   │
└────────────────────────────┬─────────────────────────────┘
                             │ HTTPS (REST + JSON, JWT)
┌────────────────────────────▼─────────────────────────────┐
│                  Spring Boot API Gateway                 │
│  (Auth, Rate Limit, Request Logging, OpenAPI 文档)        │
└──────┬──────────────┬──────────────┬──────────────┬──────┘
       │              │              │              │
┌──────▼──────┐ ┌─────▼──────┐ ┌─────▼──────┐ ┌─────▼──────┐
│  profile /  │ │  plans /   │ │  training  │ │    ai      │
│  library /  │ │  weekly /  │ │  fit /     │ │  service   │
│  workout    │ │  daily     │ │  notes /   │ │ (Spring AI)│
│   service   │ │  service   │ │  review    │ │            │
└──────┬──────┘ └─────┬──────┘ └─────┬──────┘ └─────┬──────┘
       │              │              │              │
       └──────────────┴──────┬───────┴──────────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
        ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐
        │PostgreSQL │  │   Redis   │  │   MinIO   │
        │  (主数据) │  │ (缓存/会话)│  │(文件对象) │
        └───────────┘  └───────────┘  └───────────┘
```

### 4.2 部署视图

```
┌────────────── docker-compose (dev) ───────────────┐
│                                                  │
│  postgres   redis   minio   backend   frontend   │
│  :5432      :6379   :9000   :8080     :5173      │
│                                                  │
│  (prod) k8s：Deployment + HPA + Ingress         │
└──────────────────────────────────────────────────┘
```

### 4.3 数据流：FIT 上传与分析（核心链路）

```
1. 用户在 Web 上传 .fit
        │
2. POST /api/v1/trainings/files (multipart)
   - 后端先写 MinIO: trainings/<user>/<year>/week-NN/fit/<hash>.fit
   - DB 记录 training_file (status=PENDING)
        │
3. 异步任务 (AsyncTask)
   - 从 MinIO 下载 .fit
   - 调用 FIT 解析器（端口自 .codex/skills/cycling-fit-analysis）
   - 计算：平均功率 / NP / IF / TSS / 心率区间 / 功率区间 / 踏频区间 / 10 分钟分段
   - 落库 training_session + training_record_sample(降采样)
        │
4. 更新 status=READY，触发通知（WebSocket / SSE）
        │
5. 前端重新拉取 /api/v1/trainings/{id}/analysis 渲染图表
```

---

## 5. 模块划分

后端按业务域切分（包结构 + Spring Modulith 风格）：

```
com.cyclinglab
├── platform        # 通用底座
│   ├── auth        # 登录、Token、RBAC
│   ├── user        # 用户、偏好
│   ├── tenant      # 多租户隔离（v0.1 同源多用户即可，v0.2 走 schema 隔离）
│   └── common      # 通用工具、异常、Web 配置
├── profile         # 骑手档案、阈值、设备
├── library         # 训练课库（可复用模板）
├── plan            # 周计划、阶段计划、每日计划
├── workout         # ZWO 课程文件生成、解析、导入
├── training        # FIT 记录、分析、笔记
├── review          # 周复盘 / 阶段复盘
├── ai              # Spring AI 调用、Prompt 模板、对话历史
└── storage         # MinIO 抽象、文件元数据
```

前端按 `apps/web` 单仓 + 功能路由划分：

```
src/
├── app/                  # 路由、Provider、Layout
├── features/
│   ├── auth/
│   ├── profile/
│   ├── plan/
│   ├── library/
│   ├── workout/          # ZWO 生成器
│   ├── training/         # FIT 上传 / 图表 / 笔记
│   ├── review/
│   └── ai/               # AI 助手对话
├── components/           # 通用组件
├── lib/                  # api 客户端、工具
├── hooks/
└── stores/
```

---

## 6. 数据模型（核心实体）

> 详细 ER 见 `doc/erd.svg`（后续输出）。此处列出关键实体与字段。

### 6.1 用户与权限

- `user(id, email, display_name, password_hash, role, status, created_at, updated_at)`
- `role`: `ADMIN` / `USER`（v0.1）。
- 所有业务表带 `user_id`（多用户隔离基线），未来加 `tenant_id` 时无需大改。

### 6.2 骑手档案

`rider_profile(user_id, height_cm, weight_kg, max_hr, resting_hr, threshold_hr, ftp, cadence_range, bikes JSONB, power_meter, hr_strap, head_unit, goals JSONB, updated_at)`

一对一关联 `user`。

### 6.3 训练课库

- `workout_template(id, user_id, name, category, intensity, tags[], description_md, structure_json, source)`  
  `category ∈ {endurance, recovery, intervals, testing, strength, outdoor}`  
  `structure_json` 描述训练结构（热身 / 主训练 / 间歇段），是 ZWO 生成器的输入。
- `workout_template_version(id, template_id, version, structure_json, created_at)`：版本化，方便回溯。

### 6.4 周计划

- `weekly_plan(id, user_id, iso_year, iso_week, title, goal_md, created_at)`。
- `daily_plan(id, weekly_plan_id, date, weekday, target_text, template_id NULL, actual_training_id NULL, notes_md, status)`。
  `status ∈ {PLANNED, DONE, PARTIAL, SKIPPED, RESCHEDULED}`。

### 6.5 训练记录

- `training_file(id, user_id, year, iso_week, original_filename, object_key, size_bytes, sha256, sport_type, recorded_at, status)`。
- `training_session(id, file_id, user_id, started_at, duration_sec, distance_m, energy_kj, avg_hr, max_hr, avg_power, max_power, np, if, tss, avg_cadence, max_cadence, hr_drift, hr_zone_distribution JSONB, power_zone_distribution JSONB, cadence_zone_distribution JSONB, ten_min_segments JSONB, notes_md)`。
- `training_record_sample(id, session_id, t_offset_sec, hr, power, cadence, speed, altitude, lat, lon)`：降采样后的曲线点（默认 1Hz；超长记录可降为 5s / 10s）。

### 6.6 ZWO 课程文件

- `workout_file(id, user_id, name, sport_type, tags[], xml, source_template_id, created_at)`。

### 6.7 复盘

- `review(id, user_id, scope, scope_id, period_start, period_end, content_md, metrics JSONB, created_at, updated_at)`。  
  `scope ∈ {WEEK, PHASE}`。

### 6.8 AI

- `ai_conversation(id, user_id, title, model, created_at)`。
- `ai_message(id, conversation_id, role, content_md, tokens_in, tokens_out, created_at)`。
- `ai_prompt_template(id, key, version, content_md, model, params JSONB)`：管理 prompt 版本。

### 6.9 对象存储 Key 约定

```
cycling-lab/
├── <user_id>/
│   ├── fit/<year>/week-NN/<sha256>.fit
│   ├── zwo/<year>/<name>.zwo
│   ├── screenshots/<year>/week-NN/<name>.png
│   └── notes/<year>/week-NN/<name>.md
└── shared/
    └── workout-templates/  # 平台官方模板（v0.2）
```

---

## 7. 关键模块设计

### 7.1 认证与多用户隔离

- **登录**：`POST /api/v1/auth/login {email, password}` → 返回 `accessToken(15min) + refreshToken(7d)`。
- **刷新**：`POST /api/v1/auth/refresh`（refresh token 走 Redis 黑名单）。
- **请求鉴权**：Spring Security Filter 解析 JWT，写入 `SecurityContext(userId, role)`。
- **数据隔离**：基线在 SQL 层面强制 `WHERE user_id = :currentUserId`。抽象出 `OwnedEntity` 接口 + Hibernate `@Filter`，防漏。
- **管理员**：v0.1 仅超管可见全局统计与用户管理页。

### 7.2 训练课库 + 周计划

- 计划编辑：表格化 UI 拖拽，列：`日期 / 模板 / 目标 / 备注 / 状态`，支持按模板一键填充。
- 计划生成流程：选择周起始日 → 选目标（如恢复周 / 阈值周） → AI 草拟 → 用户确认 → 落库。
- 复用：周计划只存 `template_id` 引用，模板更新时弹出版本升级提示（不静默改历史数据）。

### 7.3 FIT 上传 + 分析

- **解析库**：Java 端用 [`jfit`](https://github.com/jpaw/jfit) 或自写 parser（FIT 是公开协议）；优先评估 `com.garmin:fit-sdk`（官方 Java SDK）。  
  解析逻辑与现有 `analyze_fit.py` 对齐，保证指标一致。
- **指标计算**：
  - 平均 / 最高 / NP（30s 滚动平均的四阶幂均值）。
  - IF = NP / FTP；TSS = (duration_sec * NP * IF) / (FTP * 3600) * 100。
  - 心率漂移：第二小时平均 HR - 第一小时平均 HR（同功率区间内）。
  - 区间分布：心率按最大心率 / 阈值心率；功率按 FTP。
- **降采样**：长骑行记录入库时按"等距 1Hz"存，超 4 小时记录降为 5s 一档，原始数据仅在 MinIO 中按需回放。
- **进度反馈**：解析走异步任务，前端用 SSE (`/api/v1/trainings/{id}/events`) 推送状态（PENDING / PARSING / READY / FAILED）。

### 7.4 ZWO 课程文件生成器

- **输入**：`workout_template.structure_json`（结构化树）：
  ```json
  {
    "blocks": [
      {"type": "warmup", "durationSec": 720, "powerLow": 0.45, "powerHigh": 0.62},
      {"type": "steady", "durationSec": 1080, "power": 0.65},
      {"type": "intervals", "repeats": 2, "on": {"durationSec": 480, "power": 0.88}, "off": {"durationSec": 240, "power": 0.55}}
      {"type": "cooldown", "durationSec": 420, "powerLow": 0.55, "powerHigh": 0.40}
    ]
  }
  ```
- **生成**：服务端 `WorkoutFileGenerator` 输出标准 `.zwo`（与现有 `workouts/zwo/*.zwo` 100% 兼容）。  
  XML 序列化用 Jackson + 自定义 `ZwoSerializer`，保证字段顺序、命名空间、注释稳定。
- **预览**：前端用 `<WorkoutChart />` 渲染功率曲线（Recharts），所见即所得。
- **导入**：保留 ZWO 文件也支持上传解析 → 自动转 `workout_template`。

### 7.5 AI 辅助（Spring AI）

- **能力 1：周计划生成**
  - 输入：`rider_profile` + 最近 N 周 `training_session` 摘要 + 用户自然语言诉求（"本周想练甜区"）。
  - Prompt 模板：`ai/prompt/plan-suggestion.st`（Mustache），注入 profile、近期量、FTP、心率漂移趋势。
  - 输出：结构化 JSON（每日 `template_id` 或新模板建议 + 备注），后端校验后落 `weekly_plan`。
  - 用户在 UI 上确认 / 调整 / 拒绝；不让 AI 直接写库。
- **能力 2：训练解读**
  - 输入：单次 `training_session` + 计算好的指标 JSON。
  - Prompt 模板：`ai/prompt/training-interpretation.st`。
  - 输出：Markdown 解读（亮点、风险、建议），与训练笔记并列展示。
- **能力 3：对话式问答**（v0.2）
  - 维护 `ai_conversation` / `ai_message`，支持多轮上下文。
- **可观测性**：记录 `tokens_in / tokens_out / model / latency_ms`，支持按用户限流。

### 7.6 对象存储（MinIO）

- 通过 `StorageService` 抽象，不让业务感知 MinIO；预留 S3 兼容切换。
- 关键能力：
  - 分片上传（前端直传 MinIO，签发 STS 短期 token，降低后端带宽压力）。
  - 预签名下载链接（截图 / FIT 原文 / 生成的 ZWO）。
  - 服务端加密（SSE-KMS）。
- 备份：MinIO 内置 erasure coding + 跨桶复制（v0.2）。

### 7.7 可视化

- **训练详情页**：
  - 顶部：心率 + 功率 + 踏频 三轴曲线（Recharts / ECharts）。
  - 区间分布：心率 / 功率 / 踏频 三张饼图。
  - 10 分钟功率表 + 配速 / 速度曲线。
- **仪表盘**：
  - 周量趋势（柱状图）、强度分布（堆叠）、HR 漂移趋势、踏频占比。
  - 当前 FTP / TSS 周累计。
- **AI 面板**：右侧抽屉，承接"解读本次训练 / 调整下周计划"。

---

## 8. API 设计原则

- 全部 `/api/v1` 前缀，OpenAPI 3.1 文档由 Springdoc 自动生成。
- 资源命名用复数 + 名词：`/trainings`, `/plans`, `/workout-templates`。
- 列表接口统一支持 `?page&size&sort=&q=`。
- 错误响应统一格式：
  ```json
  { "code": "VALIDATION_FAILED", "message": "...", "traceId": "..." }
  ```
- 写操作返回 `201 Created` + 资源地址；幂等写支持 `Idempotency-Key` 头。
- 时间字段统一 ISO-8601 UTC；前端按用户时区展示。

---

## 9. 前端架构要点

- **路由**：`/`, `/login`, `/dashboard`, `/plans`, `/library`, `/trainings`, `/trainings/:id`, `/workouts/builder`, `/review`, `/settings`, `/ai`。
- **API 客户端**：`lib/api.ts` 用 `fetch` + 拦截器统一注入 token、错误处理、traceId。
- **数据获取**：服务端状态全部走 TanStack Query；表单提交用 React Hook Form。
- **权限**：基于角色的菜单 / 路由守卫（`RequireRole`）。
- **主题**：明暗双主题（Tailwind CSS variables），色板参考现有 markdown 风格。
- **Markdown 编辑器**：复盘、训练笔记、模板描述都走同一编辑器（TipTap + markdown 双向转换）。
- **错误 / 加载**：统一 `<EmptyState />`、`<ErrorState />`、`<Skeleton />`。

---

## 10. 与现有 markdown 仓库的迁移策略

**原则：Web 端为唯一真实源，markdown 作为导出 / 备份，不双向同步。**

- **一次性导入脚本**（`tools/import-from-md`）：
  - 解析 `profile/rider-profile.md` → `rider_profile`。
  - 解析 `plans/library/**/*.md` → `workout_template`。
  - 解析 `plans/<year>/week-NN/weekly-plan.md` → `weekly_plan` + `daily_plan`，解析 `2026-05-21-*.md` 单日描述入 `notes_md`。
  - `workouts/zwo/*.zwo` 入 `workout_file`。
  - `training/<year>/week-NN/fit/*.fit` 入 `training_file` + 走异步解析任务生成 `training_session`。
  - `training/<year>/week-NN/notes/*.md` 入 `training_session.notes_md`（如有，保留原 md 作为对象存储备份）。
  - `review/<year>/week-NN-review.md` 入 `review`。
- **导出**：每个核心资源提供 `?format=md` 或 `GET /export/...zip`，导回原目录结构。
- **兼容期**：v0.1 期间保留原 `plans/`、`training/` 目录只读，便于回滚。v0.2 起标记 archive。

---

## 11. 安全

- 密码：Argon2id 哈希（Spring Security 默认 bcrypt 也可，至少 cost=12）。
- JWT：HS256，密钥在 Secret Manager；Refresh token Redis 黑名单。
- 跨域：按部署域名白名单。
- 速率限制：Bucket4j + Redis，登录、AI 接口限流更严。
- 上传校验：魔数 / 扩展名 / MIME / 大小（FIT ≤ 50MB，截图 ≤ 10MB）。
- 审计日志：登录、计划变更、AI 调用全部留痕。
- OWASP：依赖检查（`mvn org.owasp:dependency-check-maven`），前端 `npm audit` CI 阻塞。

---

## 12. 性能与可扩展性

- 读多写少，RESTful + Redis 缓存周聚合、TSS 累计、HR 漂移等指标。
- 写多发生在训练上传（异步任务），用虚拟线程跑解析，避免阻塞 Tomcat 线程。
- 大文件 FIT：前端 Web Worker 解析 + 仅上传关键采样点 + 原文件（v0.2 优化）。
- 分页：游标分页（`?cursor=...`）替代 offset，避免深翻页。
- 多用户扩展：水平扩展 backend，postgres 走只读副本承载聚合查询。

---

## 13. 测试策略

- 后端：JUnit 5 + Testcontainers（postgres / minio / redis）。  
  FIT 解析需有 golden file 测试（对每个指标固定预期）。
- 前端：Vitest 单元 + Playwright 关键路径（登录、上传 FIT、生成 ZWO、调用 AI）。
- 契约测试：Spring Cloud Contract 或 Pact，前后端解耦并行。

---

## 14. CI/CD 设计

### 14.1 Jenkins 定位

Jenkins 作为主 CI/CD 编排工具，负责从代码提交到测试、构建、镜像发布、部署和回滚的完整链路。GitHub Actions 可保留为轻量校验入口，例如 PR 上只跑格式检查和快速单元测试，正式构建与部署交给 Jenkins。

### 14.2 流水线阶段

建议在仓库根目录维护 `Jenkinsfile`，采用声明式 Pipeline：

1. **Checkout**：拉取代码，记录 commit、branch、build number。
2. **Backend Verify**：执行 `mvn verify`，包含单元测试、集成测试、Flyway migration 校验。
3. **Frontend Verify**：执行 `npm ci`、`npm run lint`、`npm run test`、`npm run build`。
4. **E2E Smoke**：用 Docker Compose 启动依赖服务，跑 Playwright 关键路径。
5. **Docker Build**：构建 `cycling-lab-backend`、`cycling-lab-web` 镜像。
6. **Security Scan**：运行 Maven dependency check、npm audit、镜像漏洞扫描（Trivy）。
7. **Publish Artifact**：推送镜像到私有 Registry，归档 OpenAPI 文档、测试报告、前端构建产物。
8. **Deploy Dev**：自动部署到个人开发环境。
9. **Deploy Prod**：手动确认后部署生产环境。
10. **Rollback**：保留最近 N 个镜像版本，支持按 tag 回滚。

### 14.3 分支与触发规则

- `feature/*`：只跑快速校验（后端单元测试、前端 lint/test）。
- `main`：跑完整 CI，成功后自动部署 dev。
- `release/*` 或手动参数化构建：构建版本镜像，人工确认后部署 prod。
- 定时任务：每日夜间跑完整回归、依赖漏洞扫描和备份校验。

### 14.4 凭据与环境

- Jenkins Credentials 管理数据库密码、JWT 密钥、MinIO/S3 密钥、LLM API Key、镜像仓库账号。
- 环境变量按 `dev / prod` 分组，禁止把密钥写入仓库。
- 数据库迁移由应用启动或流水线显式执行 Flyway，生产环境迁移前先备份。
- Docker Compose 适合个人服务器部署；后续需要更强隔离时再切 Kubernetes。

### 14.5 制品版本

- 后端镜像：`registry/cycling-lab-backend:<git-sha>` 和 `:<version>`。
- 前端镜像：`registry/cycling-lab-web:<git-sha>` 和 `:<version>`。
- ZWO / FIT 分析相关脚本版本与应用镜像绑定，避免同一 FIT 文件在不同环境算出不一致指标。

---

## 15. 里程碑（建议）

| 阶段 | 内容 | 验收标准 |
| --- | --- | --- |
| **M0：基线（1 周）** | 仓库脚手架：Spring Boot + React + Docker Compose + Flyway 初始表 + 登录 + 角色 + Jenkinsfile | 能本地起、登录成功，Jenkins 可跑通基础流水线 |
| **M1：档案 + 课库 + 周计划** | profile / library / weekly-plan CRUD | 三类资源可增删改查并展示 |
| **M2：ZWO 生成器** | 表单 + 预览 + 下载 | 生成的 ZWO 可被 Zwift 正常识别 |
| **M3：FIT 上传 + 分析** | 上传 → 异步解析 → 落库 → 图表 | 现有 4 个 fit 文件能完整入库并可视化 |
| **M4：复盘 + 笔记** | review / notes 编辑器 | 复盘可写、可看趋势 |
| **M5：AI 辅助** | 计划生成 + 训练解读 | 两个 prompt 模板可用，输出可被用户采纳 |
| **M6：CI/CD 完善** | Jenkins 完整流水线、镜像仓库、dev/prod 发布、回滚 | main 自动发布 dev，prod 可手动确认发布并回滚 |
| **M7：多用户完善** | 用户管理、权限、审计 | 可创建新用户并验证数据隔离 |
| **M8：迁移工具** | 从现有 md / fit 仓库批量导入 | 一键导入历史数据且无丢失 |

---

## 15.1 M1 详细设计：骑手档案 + 训练课库

> 范围:覆盖 §15 表中 **M1 = 档案 + 课库 + 周计划** 里的「档案」和「课库」两部分;周计划 (`daily_plan` / `weekly_plan`) 依赖课库落定后再展开,作为 M1 第二轮单独成节。
>
> M1 在 M0 已有的登录、用户隔离、Flyway 迁移、JWT 之上做增量,不破坏现有 `/api/v1/auth/*` 接口。

### 15.1.1 目标与非目标

**In Scope(M1 必须完成)**

- 每个用户维护 **一份** 骑手档案(`rider_profile`),`user_id` 1:1 关联,带版本化字段与派生区间。
- 每个用户维护自己的 **训练课库**(`workout_template`),支持 6 大分类(`endurance / recovery / intervals / outdoor / testing / strength`),CRUD + 软删 + 标签搜索。
- 训练课结构以 `structure_json`(blocks 树)形式落库,与 §7.4 的 ZWO 生成器共用同一份 schema,这是 M2 的输入契约。
- 提供 **派生指标** API(HR 区间、功率区间),前端展示区间的依据直接走 API,不要前端再算。
- 一份 **从现有 markdown 仓库导入** 的小工具(初版):扫描 `profile/rider-profile.md` 与 `plans/library/**/*.md`,把 frontmatter 解析成实体落库;M7 的批量导入会复用同一份解析器。

**Out of Scope(M1 不做)**

- 周计划(weekly_plan / daily_plan)编辑 UI 与 AI 草拟 — M1 第二轮。
- 平台官方共享模板(§6.9 的 `shared/workout-templates`)— v0.2。
- ZWO 文件生成与下载 — M2。
- 训练课库版本回滚的「可视化 diff」— 暂只保留历史,UI 简化。
- 多用户权限细分(目前档案/课库都按 `user_id` 隔离,共享只读靠 v0.2 的 `is_official` 字段,本轮不加)。
- 移动端原生 / PWA 离线编辑 — v0.2+。

### 15.1.2 数据模型细化

> 字段命名沿用 §6.2 / §6.3,这里只补「约束、默认值、枚举」和「未在 §6 提到的边角字段」。

#### 骑手档案 `rider_profile`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `user_id` | UUID | PK, FK → `user.id`, 1:1 | 主键 |
| `display_name` | varchar(64) | NOT NULL | 冗余于 `user.display_name`,便于档案导出/分享 |
| `height_cm` | smallint | 100 ≤ x ≤ 230 | 厘米 |
| `weight_kg` | numeric(5,1) | 30 ≤ x ≤ 200 | 公斤,1 位小数 |
| `max_hr` | smallint | 100 ≤ x ≤ 230 | bpm,派生 HR 区间的依据 |
| `resting_hr` | smallint | 30 ≤ x ≤ 120 | bpm,可选 |
| `threshold_hr` | smallint | 必须 < `max_hr` | 乳酸阈心率,可选;若空则按 `max_hr * 0.9` 估算 |
| `ftp` | smallint | 50 ≤ x ≤ 600 | watts,派生功率区间的依据 |
| `cadence_low` / `cadence_high` | smallint | 40 ≤ x ≤ 130, `low < high` | 常用踏频区间 rpm |
| `bikes` | JSONB | `[{name, type, mileage_km, notes}]` | 自行车列表,type ∈ {road, gravel, tt, mtb, indoor} |
| `power_meter` | varchar(128) | NULL | 功率计型号 |
| `hr_strap` | varchar(128) | NULL | 心率带型号 |
| `head_unit` | varchar(128) | NULL | 码表型号 |
| `goals` | JSONB | `{short_term, mid_term, long_term}` | 训练目标,Markdown 短文,各 ≤ 1KB |
| `preferences` | JSONB | `{weekly_days, session_minutes, intensity_bias, recovery_notes}` | 训练偏好(每周可训天数、单次可用时间、强度偏好、伤病/恢复说明) |
| `is_public` | boolean | DEFAULT false | 是否对其他用户可见(预留,M1 永远 false) |
| `created_at` | timestamptz | NOT NULL | |
| `updated_at` | timestamptz | NOT NULL,自动触发器 | |

**派生(不落库,只走 API)**

- HR 区间(7 区 Coggan):基于 `max_hr`(或 `threshold_hr` 修正)。
- 功率区间(7 区 Coggan):基于 `ftp`。
- 踏频建议区间:直接读 `cadence_low` / `cadence_high`。
- BMI:由 `weight_kg / (height_cm/100)^2` 实时算,仅展示。

#### 训练课模板 `workout_template`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | UUID | PK | |
| `user_id` | UUID | NOT NULL, FK → `user.id`,索引 | 隔离 |
| `name` | varchar(128) | NOT NULL,`(user_id, name)` 唯一 | 同名允许不同版本号(见下) |
| `category` | varchar(16) | NOT NULL,枚举见下 | |
| `intensity` | varchar(32) | NULL | 自由标签:`Z1` / `Z2` / `sweet-spot` / `threshold` / `VO2` / `none` 等 |
| `tags` | text[] | DEFAULT '{}' | 自由标签,如 `["outdoor", "fueling"]` |
| `description_md` | text | NULL | 长说明,M1 允许空 |
| `structure_json` | JSONB | NOT NULL | 见 §7.4 块结构,顶层 `blocks: [...]` |
| `source` | varchar(16) | `MANUAL` / `IMPORT` / `AI_SUGGESTED` | 用于追溯来源 |
| `is_archived` | boolean | DEFAULT false | 软删标志,前台不展示;M1 不做硬删 |
| `current_version` | int | NOT NULL DEFAULT 1 | 引用 `workout_template_version.version` |
| `created_at` / `updated_at` | timestamptz | NOT NULL | |

**版本表 `workout_template_version`**(每次「保存」不直接覆盖,而是写一条新 version)

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID | PK |
| `template_id` | UUID | FK → `workout_template.id` |
| `version` | int | 与 `template_id` 一起唯一,从 1 起 |
| `name` / `category` / `intensity` / `tags` / `description_md` / `structure_json` | (同上) | 当时快照 |
| `change_note` | varchar(256) | NULL,可选「为什么改」 |
| `created_at` | timestamptz | |
| `created_by` | UUID | FK → `user.id`(冗余,但方便审计) |

`workout_template.current_version` 永远指向最新 version;历史 `daily_plan.template_id + version` 想查当时内容也能 join 出来(M1 不暴露 UI,数据可查)。

**`category` 枚举**(与 `plans/library/` 子目录一一对应,前端下拉框直接用)

```
endurance  → endurance      (有氧 / Z2 / 长距离)
recovery   → recovery       (恢复骑 / 休息日)
intervals  → intervals      (甜区 / 阈值 / VO2 / 冲刺)
outdoor    → outdoor        (周末路骑 / 长距离 / 爬坡)
testing    → testing        (FTP 测试 / 心率阈值 / 基准)
strength   → strength       (力量 / 灵活性 / 核心 / fitting 纠正)
```

**`structure_json` schema(M1 必填,服务端强校验)**

```json
{
  "schemaVersion": 1,
  "totalDurationSec": 2700,
  "blocks": [
    { "type": "warmup",   "durationSec": 720,  "powerLow": 0.45, "powerHigh": 0.62 },
    { "type": "steady",   "durationSec": 1080, "power": 0.65 },
    { "type": "intervals","repeats": 2,
      "on":  { "durationSec": 480, "power": 0.88 },
      "off": { "durationSec": 240, "power": 0.55 } },
    { "type": "cooldown", "durationSec": 420,  "powerLow": 0.55, "powerHigh": 0.40 }
  ],
  "notes": "短文本,M2 ZWO 生成器不会用,M3 之后才进 FIT 笔记"
}
```

- `type ∈ {warmup, steady, intervals, cooldown, rest}`;`rest` 表示纯休息/无功率的课(休息日、拉伸)。
- 功率统一以 **FTP 系数** 表达(0~1.5),M1 不存绝对 watts,渲染与 ZWO 都按系数。
- `totalDurationSec` 服务端按 blocks 重算,失配返回 422。
- `schemaVersion` 现在锁 1,后续 M2/M3 改了 schema,老数据用 `migrateStructure(v1 → v2)` 转换。

#### 数据库迁移

- 一次性 Flyway 文件:
  - `V2__rider_profile.sql`
  - `V3__workout_template.sql`
  - `V4__workout_template_version.sql`
- 加 `rider_profile`、`workout_template`、`workout_template_version` 三张表。
- 给 `workout_template(user_id, category)`、`workout_template USING GIN(tags)` 加索引。
- 不在 V2/V3 里塞种子数据,种子来自导入工具(见 §15.1.5)。

### 15.1.3 API 设计

> 全部 `/api/v1` 前缀;所有 P0 接口要求登录(`@PreAuthorize("isAuthenticated()")`),admin 走 `/api/v1/admin/*`(M1 暂不实现)。
> 所有读写接口强制 `userId` 过滤,repository 用 `@Filter` / 显式条件,而不是靠 service 层手动塞。

#### 骑手档案

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/profile` | 获取当前用户档案;不存在时返回 200 + 空对象,前端用「未填写」提示 |
| `PUT` | `/api/v1/profile` | 全量 upsert;body 走 §15.1.2 字段;返回更新后的档案 |
| `PATCH` | `/api/v1/profile` | 部分更新,字段为空表示不改 |
| `GET` | `/api/v1/profile/derived-zones` | 返回 HR / 功率 / 踏频 三组区间;HR / 功率用 Coggan 7 区 |
| `GET` | `/api/v1/profile/export` | 返回 `profile + derived-zones + goals` 的 JSON dump(便于备份) |

派生区间 API 返回样例(只截关键段):

```json
{
  "hrZones": [
    { "zone": 1, "name": "Active Recovery", "low": 0.50, "high": 0.60, "bpmLow": 102, "bpmHigh": 122 },
    { "zone": 2, "name": "Endurance",       "low": 0.60, "high": 0.70, "bpmLow": 122, "bpmHigh": 142 },
    { "zone": 3, "name": "Tempo",           "low": 0.70, "high": 0.80, "bpmLow": 142, "bpmHigh": 162 },
    { "zone": 4, "name": "Threshold",       "low": 0.80, "high": 0.90, "bpmLow": 162, "bpmHigh": 183 },
    { "zone": 5, "name": "VO2",             "low": 0.90, "high": 1.00, "bpmLow": 183, "bpmHigh": 203 },
    { "zone": 6, "name": "Anaerobic",       "low": 1.00, "high": 1.20, "bpmLow": 203, "bpmHigh": 244 },
    { "zone": 7, "name": "Neuromuscular",   "low": 1.20, "high": 1.50, "bpmLow": 244, "bpmHigh": 305 }
  ],
  "powerZones": [
    { "zone": 1, "name": "Active Recovery", "low": 0.00, "high": 0.55, "wattsLow": 0,   "wattsHigh": 110 },
    { "zone": 2, "name": "Endurance",       "low": 0.56, "high": 0.75, "wattsLow": 112, "wattsHigh": 150 },
    ...
    { "zone": 7, "name": "Neuromuscular",   "low": 1.50, "high": 3.00, "wattsLow": 300, "wattsHigh": 600 }
  ],
  "cadenceRange": { "low": 80, "high": 90 },
  "ftp": 200, "maxHr": 203,
  "computedAt": "2026-06-05T07:25:00Z"
}
```

#### 训练课库

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/library/templates` | 列表;支持 `?category=&q=&tag=&archived=false&page=&size=&sort=updatedAt,desc`;`q` 命中 name/description |
| `GET` | `/api/v1/library/templates/{id}` | 详情,带 `currentVersion` 的内容 |
| `POST` | `/api/v1/library/templates` | 新建;body 必填 `name` + `category` + `structure_json`;返回带 id |
| `PUT` | `/api/v1/library/templates/{id}` | 全量替换;**写一条新 version** 而不是覆盖;`change_note` 可选 |
| `PATCH` | `/api/v1/library/templates/{id}` | 改 name / intensity / tags / description / archived,**不改** structure |
| `DELETE` | `/api/v1/library/templates/{id}` | 软删(`is_archived = true`),不真删;二次调用恢复?M1 不做 |
| `GET` | `/api/v1/library/templates/{id}/versions` | 列出所有历史 version(简表,无 structure) |
| `GET` | `/api/v1/library/templates/{id}/versions/{v}` | 取指定 version 的完整快照 |
| `GET` | `/api/v1/library/categories` | 返回 6 个分类的固定列表(下拉框 source of truth) |
| `POST` | `/api/v1/library/templates/{id}/duplicate` | 复制一份到当前 user;`name` 默认 `<原名> (副本)`,可改 |

> 复制接口很关键:周计划里想"基于甜区间歇微调"时,先 duplicate 再改,避免污染原模板。

**DTO 关键约束**

- `name` 长度 1~128,前后去空格。
- `category` 必须命中 6 分类枚举。
- `intensity` 是自由标签,前端下拉框内置 Z1~Z5 + sweet-spot/threshold/VO2,允许自定义文本(存啥就显示啥)。
- `tags` 数组元素去重、转小写、最长 32 字符、最多 16 个。
- `structure_json` 用 `WorkoutStructureSchema` 走 `jakarta.validation` 校验:blocks 非空、type 合法、`power*` ∈ [0, 1.5]、`durationSec` ≥ 0、`repeats` ≥ 1。

#### 错误返回

- 档案字段不合法 → `400 VALIDATION_FAILED`,`details[]` 给字段路径。
- structure_json 校验失败 → `422 UNPROCESSABLE_STRUCTURE`,`details[].pointer` 用 JSON Pointer(例如 `/blocks/2/on/power`)。
- 同名冲突 → `409 DUPLICATE_NAME`,`details[].conflictWith = existingId`。

#### 鉴权

- 档案接口:owner 读写,admin 只读(M1 admin 读也走 M7 的 user 上下文;本轮 admin 暂不能读写他人档案)。
- 课库接口:owner 全权;`is_official` 模板 v0.2 再说,本轮无官方模板,等于 owner only。
- Hibernate filter `tenantFilter`(§5 `platform.tenant` 包)在所有 `rider_profile` / `workout_template` 查询上自动叠加 `user_id = :currentUserId`,integration test 覆盖「用 A 用户的 token 读 B 用户的 id 必须 404」。

### 15.1.4 业务规则与校验

**档案**

- `ftp` 改了之后,所有 `workout_template.structure_json` 的功率系数渲染时 **不会** 自动变(系数是相对值,跨 FTP 仍有效),但派生功率区间的 `wattsLow / wattsHigh` 会变 → 前端档案页提示「FTP 调整后,所有课程预览的功率瓦数会同步刷新」。
- `max_hr` 改了同理,HR 区间刷新。
- `cadence_low / cadence_high` 不参与派生计算,只展示。
- `display_name` 改名不会同步到 `user.display_name`,要改 `user` 走 `PATCH /api/v1/users/me`(M6 加,M1 暂缺)。
- 档案删除:**M1 不做**。要重新填写就 PUT 覆盖;v0.2 再加 `DELETE /api/v1/profile`(同时软删该 user 全部业务数据)。

**训练课**

- 同一 user 下 `name` 唯一;重名 PUT 时返回 409,提示「你想用现存的 XX 吗?」,UI 提供「改个名」按钮。
- 删除已引用的课(`daily_plan.template_id = X`):**M1 静默失败**,DELETE 返回 409 `IN_USE`,提示「N 个日计划正在引用,先取消引用再删」。同时支持「改成 archived=true」,前台不再展示,但引用仍能 join 出原内容。
- 改 structure(走 PUT):不破坏历史 `daily_plan` 引用;读取时 `daily_plan` 拿 `template_id + version`,前端默认展示「当前最新版」,但标注「这是 v3,日计划创建于 v1」,用户可点切回 v1 看历史快照。
- structure_json 中 power 系数硬上限 **1.5**(=150% FTP),超过 → 422 拒绝;下限 0,等于 0 表示「无功率约束」(休息日)。
- `repeats` × 单段时长超过 4 小时 → 警告但不拒绝(长课)。

### 15.1.5 与现有 markdown 仓库的兼容

> 项目根目录的 `profile/rider-profile.md` 和 `plans/library/**/*.md` 是 M1 之前的「事实标准」。M1 要做的不是「全量迁移」(M8),而是给一个 **M1 阶段就地可用的导入器**,让新用户开账号后能一键把现有 md 变成可编辑的数据库记录。

**导入范围**

- 档案:`profile/rider-profile.md` → 1 条 `rider_profile`。
- 课库:`plans/library/**/*.md` → 1 条 `workout_template` / 文件;按 frontmatter 的 `category` 决定归属,缺 category 落到「uncategorized」并标 warning。

**解析器**

- 新模块 `com.cyclinglab.platform.importer`(M1 仅做 md 解析,不进 delivery,放 `library.importer`)。
- 复用现有 frontmatter 解析(项目用标准 YAML frontmatter,SnakeYAML 即可,不引新依赖)。
- `structure_json` 来源:**M1 阶段的 md 模板只有 markdown 表格,没有机器可读结构**;导入器做「尽力推断」:
  - 「阶段 / 时间 / 强度」表头 → 拆出 warmup / steady / cooldown 块。
  - 「间歇 / repeats」表头 → 拆出 intervals 块。
  - 推断不出的部分(比如「Z2」这种自由标签)写到 `description_md` 里,`structure_json` 给一个占位 `[{type: steady, durationSec: ?, power: 0.65}]`,**`totalDurationSec` 留 0,前端在编辑页标红「需补全」**。
- 导入完成返回 `ImportReport { riderProfile, templates: [{id, status, warnings: []}] }`,前端展示报告让用户确认。

**再导出(M1 不做,M3 之后再说)**

- 暂不提供「库 → md」导出。M1 阶段仓库里的 md 视为只读种子,改的话去 UI 改库。
- 真要回看原 md,UI 上加一个「查看原始 markdown」按钮,从 MinIO/本地取原文件渲染(放 v0.2)。

**双向同步**

- M1 **不**做库 → md 同步。如果用户两边都改,会被覆盖。建议:导入后仓库里那批 md 标 `archived: true`(放进 `profile/.archived/` 目录,不入 git),或者直接 `git rm`;M8 迁移工具会出整体方案。

### 15.1.6 前端设计

> 前端栈沿用 M0:React 19 + Vite + TanStack Router/Query + Zustand + RHF + Zod。本节列页面 / 路由 / 关键交互,不画 UI。

**新增路由**

```
/profile                   → 档案主页(查看 + 编辑)
/profile/zones             → 区间可视化(HR / 功率 / 踏频三组)
/library                   → 课库列表(筛选 + 搜索)
/library/new               → 新建课模板
/library/:id               → 课模板详情 / 编辑
/library/:id/versions      → 版本历史(简化列表)
/library/:id/versions/:v   → 单版本快照
```

**档案页 `/profile`**

- 顶部三张卡:基本信息(身高 / 体重 / BMI)、能力指标(FTP / max_hr / 阈值心率 / 静息心率)、训练偏好(每周可训天数 / 单次可用时间)。
- 主体表单,分 4 个折叠组:基本信息、设备与数据源、训练目标、训练偏好与限制。RHF + Zod,沿用 §9 的 `lib/api` 客户端。
- 顶部「区间预览」按钮 → 跳 `/profile/zones`(或者抽屉展示)。
- 保存:`PUT /api/v1/profile`;乐观更新,失败回滚。
- 空状态:首次进入是「未填写」,CTA「开始填写」直接展开基本信息组。

**区间页 `/profile/zones`**

- 三栏:HR 区间(7 行 + 折线图,bpm 范围) / 功率区间(7 行 + 折线图,watts 范围) / 踏频范围(单行卡)。
- 表格 + 简单条形图(Recharts)。无编辑,纯展示。
- 顶部 badge:`基于 max_hr=203, ftp=200, 2026-06-05 计算`,提示数据来源。

**课库列表 `/library`**

- 左侧 sidebar:6 个分类(每个带计数),+ 「全部」+ 「已归档」入口(v0.2)。
- 顶部:`+ 新建`按钮、搜索框、标签 chip 筛选(多选)、`category` 下拉。
- 列表:卡片视图,每张卡显示「name + 强度标签 + 主训练时长 + 标签 chips + updated_at」;hover 显示「查看 / 编辑 / 复制 / 归档」操作。
- 排序:默认 `updatedAt, desc`;M1 不做自定义排序。
- 列表请求走 TanStack Query,缓存 30s。

**课模板编辑 `/library/new` 与 `/library/:id`**

- 顶部:面包屑(`课库 / 甜区间歇`)。
- 主体:左侧表单(name / category / intensity / tags / description),右侧 **可视化编辑器**:
  - 块列表(可拖拽排序,可加块),每个块卡片显示「type + 持续时间 + 功率区间 + repeats」。
  - 块类型:`warmup` / `steady` / `intervals` / `cooldown` / `rest`。
  - 编辑器实时算出 `totalDurationSec` 并显示在顶部。
  - 底部一条「功率曲线预览」(Recharts),按 FTP=200 把 blocks 折线画出来;FTP 改 / 拉块 / 改系数,曲线实时变。
- 保存:`POST`(新建)或 `PUT`(更新,写新 version + change_note 必填);保存后跳详情。
- 删除:detail 页右上角「归档」按钮,二次确认后 `DELETE`。

**版本历史 `/library/:id/versions`**

- 时间倒序列表:version 号 + change_note + 创建时间。
- 点行跳 `/library/:id/versions/:v`,只读展示该 version 的快照,顶部 badge「v3 · 2026-06-05」,提示「这是历史版本,不可编辑」。

**Dashboard 集成**

- 现有 Dashboard 加两个 widget:
  - 「档案状态」:如果 `rider_profile` 缺关键字段(FTP / max_hr),显示「完善档案」CTA;否则展示「FTP 200 / max_hr 203 / BMI 22.6」+ 「查看 →」按钮。
  - 「课库概览」:6 个分类的课数柱状图,缺哪类给「该类暂无模板,新建一个?」按钮。

### 15.1.7 验收标准

> 「可演示 = OK」原则,每条都对应一次手工 + 一次自动测试。

| # | 验收点 | 手工 | 自动化 |
| --- | --- | --- | --- |
| A1 | 新用户注册后,无档案时 `/profile` 显示「未填写」CTA;填写并保存,刷新后字段保留 | ✅ | integration:空 → 填 → GET 回来字段一致 |
| A2 | 档案字段越界(身高 300cm)→ 400 + 字段路径;FTP 600 不允许 | ✅ | unit:boundary cases |
| A3 | `/profile/derived-zones` 返回 7 区 HR + 7 区功率,bpm / watts 区间正确(对 4 组典型 FTP / max_hr 组合做断言) | ✅ | unit:zone 计算器 |
| A4 | 新建课:`Z2 1h` 用结构化 blocks 提交 → 列表立刻可见,detail 页功率曲线预览正确 | ✅ | e2e:Playwright 1 条 |
| A5 | 课 structure 功率系数 1.6 → 422,响应 `details[0].pointer=/blocks/0/power` | ✅ | unit:schema 校验 |
| A6 | 改课(走 PUT)→ `workout_template_version` 新增 1 条,`workout_template.current_version` 自增;`/versions` 列表能看到 | ✅ | integration:version 表写入 |
| A7 | 同 user 重名 → 409;不同 user 允许同名 | ✅ | integration |
| A8 | 复制课 → 新 id,新 name(`(副本)`),blocks 一致 | ✅ | unit:duplicate service |
| A9 | A 用户的 token 用 B 用户的 template id → 404(不是 403,避免泄露存在性) | ✅ | integration:多用户隔离 |
| A10 | 「归档」课 → 列表不展示;再查详情 200,`is_archived=true`;DELETE 已归档的课 → 409 IDEMPOTENT(可选) | ✅ | integration |
| A11 | 导入 `profile/rider-profile.md` + 5 个 `plans/library/**/*.md` → 档案 1 条 + 课 5 条;无 category 的课进 uncategorized 且 `ImportReport.warnings` 非空 | ✅ | integration:md 解析器 |
| A12 | 课库 widget 计数与列表查询一致 | ✅ | e2e |
| A13 | 现有 M0 接口(`/api/v1/auth/*`)+ Spring Boot 启动 + Flyway 迁移 + `/actuator/health` 不回归 | ✅ | smoke + 现有 test 全绿 |

### 15.1.8 风险与待办

| 项 | 风险 / 待办 | 处置 |
| --- | --- | --- |
| structure_json 推断质量 | md → 结构化块不能 100% 还原,导入后用户要补全 | ImportReport 显式标 warnings;UI 给出「待补全」高亮 |
| FTP 改了,旧课的「上次执行」功率显示不再准确 | 历史 `training_session` 按当时功率存,不受影响;UI 上加「当时 FTP=200,现在 210」的提示 | 不在 M1,M3 训练记录做 |
| 多用户下重名课体验 | 自己的列表里只有一个,跨用户不混 | 当前已用 `user_id` 隔离,UX 自然,无需额外处理 |
| Markdown 仓库后续被改 | 与库不同步 | M1 阶段不双向同步,文档明示;M8 迁移工具再做单向同步 |
| Hibernate filter 在 native query 上失效 | 多用户隔离漏数据 | integration test 加 A/B 用户互查断言;code review 拦 native query |
| 课库内容随时间膨胀 | 列表性能 | 索引已加(`category`、`tags` GIN);分页默认 20,上限 100 |
| 周计划(M1 第二轮) | 这节只覆盖档案 + 课库;daily_plan / weekly_plan 留到下一轮 | 单独 PR,依赖本节 `workout_template.id` 落定 |
| admin 后台 | 平台级操作(改官方模板、查用户档案) | v0.2,本轮不开 admin 路由 |

---

## 16. 风险与对策

| 风险 | 影响 | 对策 |
| --- | --- | --- |
| FIT 协议复杂，解析不准 | 指标失真、用户不信任 | 与 `analyze_fit.py` 对齐指标，并维护 golden test 集 |
| AI 输出不可控 / 幻觉 | 给出错误训练建议 | 强制结构化输出 + 服务端校验；UI 必须二次确认；prompt 模板版本化 |
| LLM 成本不可控 | 账单失控 | 按用户限流 / 配额；先做轻量解读，避免长上下文 |
| 数据迁移丢失 | 用户历史数据不完整 | 先做 dry-run 报告；保留 md 备份；提供导出 |
| 单用户 → 多用户隔离 bug | 别人能看到你的数据 | Hibernate filter + 集成测试覆盖所有查询；code review 重点关注 |
| MinIO 运维 | 文件丢失 | 启用 versioning + 跨桶复制；定期演练恢复 |
| Jenkins 凭据或发布配置泄露 | 生产密钥泄露、环境被误操作 | 使用 Jenkins Credentials、最小权限账号、部署环境审批、构建日志脱敏 |
| 流水线过慢 | 开发反馈变差 | 分层流水线：feature 分支跑快速校验，main/release 跑完整回归 |

---

## 17. 开放问题（待确认）

1. **租户模型**：v0.1 走「同库 + user_id 隔离」够用；是否需要在 M0 直接支持 schema 级别隔离？
2. **AI 模型选择**：默认接哪家？是否需要国内模型兜底（合规 / 成本）？
3. **FIT 解析库**：用 Garmin 官方 Java SDK 还是社区库？是否要做 polyfill 兼容老设备记录？
4. **PWA / 移动端**：v0.1 是否同时支持 PWA 安装 + 离线查看历史？
5. **导出格式**：除 md 外，是否需要导出 PDF 复盘 / 教练分享包？
6. **国际化**：UI 是否先做中英双语？
7. **Jenkins 部署目标**：个人服务器用 Docker Compose 发布，还是直接面向 Kubernetes？
8. **镜像仓库**：使用 Docker Hub、GitHub Container Registry，还是自建 Harbor？

---

## 18. 变更记录

| 日期 | 版本 | 变更 |
| --- | --- | --- |
| 2026-06-02 | v0.1 | 初版草案（背景、目标、技术栈、模块、数据模型、关键设计、迁移、里程碑、风险） |
| 2026-06-02 | v0.2 | 增加 Jenkins CI/CD 设计、流水线阶段、发布策略、凭据与风险控制 |
| 2026-06-05 | v0.3 | 增加 §15.1 **M1 详细设计**(档案 + 训练课库):目标/数据模型细化/API/业务规则/md 导入/前端设计/验收标准/风险;沿用 §6.2 §6.3 §7.4 的字段与 `structure_json` 契约;周计划(M1 第二轮)留作下一节 |
