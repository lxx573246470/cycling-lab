# cycling-lab

个人骑行训练实验室，沉淀骑手档案、周训练计划、FIT 文件、训练笔记、截图、复盘和可复用训练文件。
当前仓库包含两个并存的世界：

- **legacy**：根目录下的 `profile/`、`plans/`、`training/`、`review/`、`workouts/`、`fitting/` 等 markdown 与 FIT 文件。
- **web（M0 起步）**：`backend/`（Spring Boot）+ `frontend/`（React）+ `docker-compose.yml`。

架构设计见 [`doc/ARCHITECTURE.md`](doc/ARCHITECTURE.md)。

## 目录结构

```text
cycling-lab/
├── backend/                 # Spring Boot 3.3 + Java 21（M0）
├── frontend/                # React 19 + Vite + TS（M0）
├── doc/                     # 架构设计等文档
├── docker-compose.yml       # 一键起 postgres / redis / minio / backend / frontend
├── profile/                 # 骑手长期档案（legacy）
├── plans/                   # 可复用训练课库、周计划和阶段计划（legacy）
├── training/                # 每周训练数据、FIT、截图和训练笔记（legacy）
├── review/                  # 周复盘、阶段复盘（legacy）
├── workouts/                # 可导入骑行平台的训练文件（legacy）
├── fitting/                 # 自行车 fitting 资料（legacy）
├── scripts/                 # 本项目辅助脚本
└── skills/                  # Codex 项目技能说明
```

## Web 项目快速开始（M0 基线）

### 前置依赖

- **JDK 21**（推荐路径：`/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`）
- **Maven 3.9+**
- **Node.js 22+** 与 **npm 10+**
- **Docker + Docker Compose**（仅 docker 模式需要）

### 方式 A：本地直跑（不依赖 Docker）

启动后端：

```bash
cd backend
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
  PATH=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin:$PATH \
  mvn spring-boot:run
```

> 启动前需先准备好 `postgres`、`redis`、`minio`。可以单独用 `docker compose up -d postgres redis minio` 起依赖再跑后端。

启动前端：

```bash
cd frontend
npm install
npm run dev
```

- 前端：<http://localhost:5173>
- 后端：<http://localhost:8080>
- OpenAPI / Swagger UI：<http://localhost:8080/swagger-ui.html>
- MinIO 控制台：<http://localhost:9001>（minioadmin / minioadmin）

### 方式 B：Docker Compose 一键起

```bash
docker compose up --build
```

启动后访问 <http://localhost:5173>。默认账号可在 UI 上点 "注册" 创建；M0 不内置种子用户。

### 默认端口

| 服务 | 端口 | 凭据 |
| --- | --- | --- |
| Frontend (nginx) | 5173 | — |
| Backend (Spring) | 8080 | — |
| PostgreSQL | 5432 | `cycling_lab` / `cycling_lab` |
| Redis | 6379 | — |
| MinIO API | 9000 | `minioadmin` / `minioadmin` |
| MinIO Console | 9001 | `minioadmin` / `minioadmin` |

### 测试

后端：

```bash
cd backend
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home \
  PATH=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin:$PATH \
  mvn test
```

前端：

```bash
cd frontend
npm run test
```

## M0 已完成能力

- 后端：Spring Boot 3.3 + JPA + Flyway + Security + JWT（access + refresh）。
- 实体：`UserEntity`，`role = {USER, ADMIN}`，`status = {ACTIVE, DISABLED}`。
- 接口：
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
  - `GET  /api/v1/auth/me`（需 Bearer）
- 前端：登录/注册页 + 受保护 Dashboard 骨架 + TanStack Router + Query + Zustand 持久化 + 自动 token 刷新。
- Docker Compose：postgres + redis + minio + backend + frontend。

## 工作流（legacy）

1. 在 `profile/rider-profile.md` 维护身高、体重、最大心率、FTP、设备和训练目标。
2. 在 `plans/library/` 维护可复用训练课模板。
3. 在 `plans/2026/week-21/weekly-plan.md` 写本周训练安排，每日计划引用 `plans/library/` 中的模板。
4. 如果 AI 生成了新的训练课，先新增到 `plans/library/` 对应分类，再在周计划中引用。
5. 将 FIT 文件放入 `training/2026/week-21/fit/`。
6. 使用 `scripts/analyze_fit.py` 生成训练分析笔记到 `training/2026/week-21/notes/`。
7. 将关键截图放入 `training/2026/week-21/screenshots/`。
8. 在 `review/2026/week-21-review.md` 完成本周复盘。

## 计划复用（legacy）

`plans/library/` 是训练课模板库，按训练类型分类：

- `endurance/`: Z2、有氧基础、长距离耐力。
- `recovery/`: 恢复骑、休息日、低压力活动。
- `intervals/`: 甜区、阈值、VO2max、冲刺等间歇。
- `testing/`: FTP、心率阈值、基准测试和测试准备。
- `strength/`: 力量、灵活性、核心训练。

周计划不复制完整训练课内容，只引用模板。例如：

```markdown
| 周二 | [Z2 有氧基础骑](../../library/endurance/z2-base-ride.md) | 有氧容量 | training/2026/week-21/notes/ |  |
```

## FIT 分析（legacy）

本仓库已经加入项目技能说明，见 `skills/cycling-lab/SKILL.md`。如果本机已安装 Codex 的 `cycling-fit-analysis` skill，可直接运行：

```bash
python3 scripts/analyze_fit.py training/2026/week-21/fit/activity.fit \
  --age 31 \
  --height-cm 176 \
  --weight-kg 70 \
  --out-dir training/2026/week-21/notes
```

## 后续里程碑

见 `doc/ARCHITECTURE.md` 第 14 节：

- **M1** 档案 + 课库 + 周计划 CRUD
- **M2** ZWO 课程文件生成器
- **M3** FIT 上传 + 自动分析 + 图表
- **M4** 复盘 + 笔记
- **M5** AI 辅助（计划生成 / 训练解读）
- **M6** 多用户完善
- **M7** 从 legacy md/fit 仓库批量导入
