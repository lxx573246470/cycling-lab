# Cycling Lab Web 架构设计

> 版本：v0.1 · 2026-06-02
> 状态：草案（初版，待评审）
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
