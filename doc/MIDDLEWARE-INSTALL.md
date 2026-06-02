# Cycling Lab 中间件安装清单

> 用途：交给服务器上的 Claude（运维 Agent），按本清单完成中间件安装与初始化。
> 文档版本：v0.1 · 2026-06-02
> 配套架构文档：`doc/ARCHITECTURE.md`

---

## 0. 任务边界

请在本服务器上完成以下工作，**不要触碰本仓库代码**，不要做无关的安装：

1. 安装本项目运行所需的全部中间件与运行时。
2. 完成基础初始化（建库、建桶、建服务账号、防火墙/端口放行）。
3. 把每个组件的健康检查跑通并把结果回报给我。
4. **不要**自动把后端 jar 或前端 dist 部署上去——M0 之后会有单独的部署步骤。
5. **不要**安装 / 启用本清单外的工具（k8s、监控、CI runner 等），如确有必要先报告。

---

## 1. 目标服务器（已知信息）

| 项 | 值 |
| --- | --- |
| 操作系统 | **Ubuntu 24.04.4 LTS (Noble Numbat)** |
| 内核 | Linux 6.8.0-63-generic |
| 架构 | x86_64（amd64） |
| 主机名 | `iZ2vc1piav73owa3jajpf0Z` |
| 云厂商 | 阿里云 ECS |
| 公网 IP | `8.137.52.31` |
| 内网 IP | `172.19.38.126` |
| 防火墙 | **本机 UFW `inactive`，由阿里云安全组管控** —— 不要在本机启用 UFW，避免和安全组规则冲突。 |
| 监听策略 | Postgres / Redis / MinIO 默认**只绑内网 IP `172.19.38.126`**，不绑 0.0.0.0。公网访问走 Nginx 80/443 入口。 |
| 用户 | `root` 或有 `sudo` 权限的账户（全部命令在 `root` 下跑） |

安装前请先确认机器身份无误，并回报：

```bash
cat /etc/os-release | head -4
uname -a
whoami
hostname
ip -4 addr show | grep -E 'inet '
ss -tlnp | grep -E ':(5432|6379|9000|9001|8080|5173|80|443) ' || echo "ports free"
ufw status 2>/dev/null || echo "ufw not installed"
df -h /var
free -h
```

把以上输出回报给我。

---

## 2. 总览：要装什么

| 组件 | 版本 | 用途 | 安装方式 | 是否必需 |
| --- | --- | --- | --- | --- |
| Docker Engine | 24+ | 容器运行时 | 官方 apt 源 | **必需** |
| Docker Compose Plugin | v2 (`docker compose`) | 编排 | 跟随 Docker | **必需** |
| PostgreSQL | 16 | 主数据库 | 官方 apt 源 | **必需** |
| Redis | 7 | 缓存 / 会话 | 官方 apt 源 | **必需** |
| MinIO Server | latest | 对象存储（FIT/ZWO/截图/md 笔记） | 官方二进制 | **必需** |
| MinIO Client (mc) | latest | 桶管理 | 官方二进制 | **必需** |
| Nginx | 1.24+ | 反向代理 + 前端静态托管 | 官方 apt 源 | **必需**（生产暴露 80/443 用） |
| OpenJDK | 21 (LTS) | 后端运行（仅在裸机跑时需要） | 官方 apt / temurin | **可选** |
| Node.js | 22 LTS | 前端构建（仅在裸机构建时需要） | NodeSource | **可选** |
| Prometheus | latest | 指标采集 | Docker | v0.1 不必装 |
| Grafana | latest | 指标可视化 | Docker | v0.1 不必装 |
| LLM Provider | — | Spring AI 后端 | 外部 API / Ollama | **可选**：v0.1 不接 AI 也可启动 |

> 推荐路径：**只用 Docker 跑中间件（Postgres / Redis / MinIO），Nginx 走 apt**。后端 / 前端先别跑，部署阶段再上。

---

## 3. 通用前置

> **不要安装 / 启用 `ufw`**——本服务器安全组由阿里云管控，本机启用 UFW 会与安全组规则打架，徒增排障成本。

```bash
set -euo pipefail
apt-get update
apt-get install -y --no-install-recommends \
  ca-certificates curl gnupg lsb-release jq \
  apt-transport-https software-properties-common
timedatectl set-timezone Asia/Shanghai || true
```

> 非 Ubuntu（CentOS / RHEL / 阿里云 Linux）分支：
> ```bash
> yum install -y epel-release yum-utils
> # Docker / Postgres / Redis / Nginx 走对应 yum 源
> ```

---

## 4. 逐项安装

### 4.1 Docker Engine + Compose

```bash
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  > /etc/apt/sources.list.d/docker.list
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker
docker --version
docker compose version
```

如果 `docker` 提示权限：

```bash
usermod -aG docker $SUDO_USER || true
```

### 4.2 PostgreSQL 16

```bash
curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc | gpg --dearmor -o /etc/apt/keyrings/pgdg.gpg
echo "deb [signed-by=/etc/apt/keyrings/pgdg.gpg] http://apt.postgresql.org/pub/repos/apt $(. /etc/os-release && echo "$VERSION_CODENAME")-pgdg main" \
  > /etc/apt/sources.list.d/pgdg.list
apt-get update
apt-get install -y postgresql-16 postgresql-client-16
systemctl enable --now postgresql
sudo -u postgres psql -c "SELECT version();"
```

**建库 + 建用户**（密码**必须改**成自己的强密码并回报给我）：

```bash
PG_PASS="CHANGE_ME_PG_PASS_$(openssl rand -hex 12)"
sudo -u postgres psql <<SQL
CREATE USER cycling_lab WITH PASSWORD '$PG_PASS';
CREATE DATABASE cycling_lab OWNER cycling_lab;
GRANT ALL PRIVILEGES ON DATABASE cycling_lab TO cycling_lab;
SQL
echo "PG user=cycling_lab  password=$PG_PASS" > /root/.cycling-lab-secrets
chmod 600 /root/.cycling-lab-secrets
```

**监听地址**：编辑 `/etc/postgresql/16/main/postgresql.conf`，把 `listen_addresses` 改为：

```
listen_addresses = '172.19.38.126'
```

> 只绑内网 IP，不绑 0.0.0.0，也不绑 127.0.0.1（需要从本机其他进程连），远程走 Nginx / SSH 隧道。如果需要 `psql` 从本机连，请 `listen_addresses = '127.0.0.1,172.19.38.126'`。

`pg_hba.conf` 默认仅本机 socket 即可，不开远程 IP 白名单。然后：

```bash
systemctl reload postgresql
ss -tlnp | grep 5432
```

> 如确实需要远程管理 PG，再加：`host all cycling_lab 0.0.0.0/0 scram-sha-256`（**慎用全网开放**，建议限制 IP 段）。

### 4.3 Redis 7

```bash
curl -fsSL https://packages.redis.io/gpg | gpg --dearmor -o /etc/apt/keyrings/redis-archive-keyring.gpg
echo "deb [signed-by=/etc/apt/keyrings/redis-archive-keyring.gpg] https://packages.redis.io/deb $(. /etc/os-release && echo "$VERSION_CODENAME") main" \
  > /etc/apt/sources.list.d/redis.list
apt-get update
apt-get install -y redis
systemctl enable --now redis-server
redis-cli ping
```

**设置密码 + 只绑内网**（推荐）：

```bash
REDIS_PASS="CHANGE_ME_REDIS_$(openssl rand -hex 12)"
# 只绑内网 IP，关闭保护模式（已有密码的前提下）
sed -i "s/^# requirepass .*/requirepass $REDIS_PASS/" /etc/redis/redis.conf
sed -i "s/^bind 127.0.0.1 .*/bind 172.19.38.126/" /etc/redis/redis.conf
sed -i "s/^protected-mode yes/protected-mode no/" /etc/redis/redis.conf
systemctl restart redis-server
echo "REDIS_PASSWORD=$REDIS_PASS" >> /root/.cycling-lab-secrets
redis-cli -a "$REDIS_PASS" -h 172.19.38.126 ping
```

### 4.4 MinIO Server + mc

官方推荐用 docker 跑（先生成密码再启动）：

```bash
MINIO_PASS="CHANGE_ME_MINIO_$(openssl rand -hex 12)"
echo "MINIO_ROOT_USER=minioadmin"  > /root/.cycling-lab-secrets
echo "MINIO_ROOT_PASSWORD=$MINIO_PASS" >> /root/.cycling-lab-secrets
chmod 600 /root/.cycling-lab-secrets
mkdir -p /var/lib/minio
docker run -d --name minio --restart=always \
  -p 172.19.38.126:9000:9000 \
  -p 172.19.38.126:9001:9001 \
  -v /var/lib/minio:/data \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD="$MINIO_PASS" \
  minio/minio:latest server /data --console-address ":9001"
sleep 3
docker logs minio --tail 20
```

> 上面把容器端口绑到内网 IP `172.19.38.126`，公网无法直接访问。Console (`9001`) 也只在内网开，需要管理时用 SSH 隧道：`ssh -L 19001:127.0.0.1:9001 root@8.137.52.31` 然后浏览器开 `http://127.0.0.1:19001`。

**安装 mc 并建桶**（mc 在本机即可，连接走内网 IP）：

```bash
curl -fsSL https://dl.min.io/client/mc/release/linux-amd64/mc -o /usr/local/bin/mc
chmod +x /usr/local/bin/mc
mc alias set local http://172.19.38.126:9000 minioadmin "$MINIO_PASS"
mc mb -p local/cycling-lab || true
mc anonymous set download local/cycling-lab || true
mc ls local/
```

### 4.5 Nginx

```bash
apt-get install -y nginx
systemctl enable --now nginx
nginx -v
```

占位 vhost，等部署阶段再写 upstream：

```bash
cat > /etc/nginx/sites-available/cycling-lab <<'EOF'
server {
    listen 80 default_server;
    server_name _;
    root /var/www/cycling-lab;
    index index.html;
    client_max_body_size 50m;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
EOF
ln -sf /etc/nginx/sites-available/cycling-lab /etc/nginx/sites-enabled/cycling-lab
rm -f /etc/nginx/sites-enabled/default
mkdir -p /var/www/cycling-lab
echo '<h1>cycling-lab placeholder</h1>' > /var/www/cycling-lab/index.html
nginx -t && systemctl reload nginx
```

### 4.6 （可选）OpenJDK 21 + Maven

仅在你要**裸机跑后端**时安装。Docker 跑后端可跳过。

```bash
apt-get install -y wget
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo "$VERSION_CODENAME") main" \
  > /etc/apt/sources.list.d/adoptium.list
apt-get update
apt-get install -y temurin-21-jdk
java -version
apt-get install -y maven
mvn -v
```

### 4.7 （可选）Node.js 22

仅在你要**裸机构建前端**时安装。

```bash
curl -fsSL https://deb.nodesource.com/gpg-key/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg
echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_22.x nodistro main" \
  > /etc/apt/sources.list.d/nodesource.list
apt-get update
apt-get install -y nodejs
node -v
npm -v
```

### 4.8 （可选）LLM Provider

v0.1 不接 AI 也可启动。如果要接，**二选一**：

- **A. 用云端 API（OpenAI / Anthropic / 国产模型 OpenAI 兼容端点）**：
  - 不在本机装任何东西，只需准备一个 API key。
  - 把端点 base url + key 写入 `/root/.cycling-lab-secrets` 附录。
- **B. 用本地 Ollama**：

  ```bash
  curl -fsSL https://ollama.com/install.sh | sh
  ollama serve &
  ollama pull qwen2.5:7b-instruct   # 或其他模型
  ```

---

## 5. 阿里云安全组（替代本机 UFW）

> **不要在本机启用 UFW**。本服务器安全由阿里云安全组管控。请在阿里云控制台 → ECS → 本实例 → 安全组里配置入方向规则。

### 5.1 需要放行的入方向规则

| 端口 | 协议 | 源 | 用途 |
| --- | --- | --- | --- |
| 22 | TCP | `0.0.0.0/0`（生产建议限定运维出口 IP） | SSH 运维 |
| 80 | TCP | `0.0.0.0/0` | HTTP（公网入口） |
| 443 | TCP | `0.0.0.0/0` | HTTPS（后续上 TLS 用） |

**不放行**（按本机监听策略已只绑内网，安全组也不开）：

- 5432 / 6379 / 9000 / 9001 / 8080：仅内网访问，公网不开。

### 5.2 操作步骤（让另一个 Claude 去做）

```bash
# 本机读不到安全组规则，只能用阿里云 OpenAPI 操作。
# 如果目标机器上已配置了 aliyun CLI 且有对应 RAM AccessKey：
aliyun ecs DescribeSecurityGroupAttribute --SecurityGroupId <sg-id> \
  --RegionId cn-hangzhou --Direction ingress
```

> 如果 RAM AccessKey / OpenAPI 不便走控制台，请**在阿里云控制台手工配置**入方向规则。完成后请在控制台截图或导出 JSON 规则回贴给我。

### 5.3 验证安全组生效

```bash
# 从你本地机器（或任意外部机器）测试：
curl -sI --max-time 5 http://8.137.52.31/ | head -1        # 应返回 200
nc -zv 8.137.52.31 5432 -w 3   2>&1 | tail -1             # 应 timeout / refused（被安全组挡）
nc -zv 8.137.52.31 6379 -w 3   2>&1 | tail -1             # 同上
nc -zv 8.137.52.31 9000 -w 3   2>&1 | tail -1             # 同上
nc -zv 8.137.52.31 9001 -w 3   2>&1 | tail -1             # 同上
```

> 如果 5432 等端口在公网**能连上**，说明安全组没配好，必须修。

---

## 6. 端口与凭据总览

| 服务 | 监听端口 | 监听地址 | 公网是否放行 | 默认凭据 / 备注 |
| --- | --- | --- | --- | --- |
| Nginx | 80, 443 | `0.0.0.0` | ✅ 阿里云安全组放行 | 前端 + 反代 `/api` |
| Backend (后续) | 8080 | `127.0.0.1` | ❌ 仅经 Nginx 暴露 | — |
| PostgreSQL | 5432 | `172.19.38.126` | ❌ | `cycling_lab` / `<生成的强密码>` |
| Redis | 6379 | `172.19.38.126` | ❌ | `<生成的强密码>` |
| MinIO API | 9000 | `172.19.38.126` | ❌ | `minioadmin` / `<生成的强密码>` |
| MinIO Console | 9001 | `172.19.38.126` | ❌ | 同上 |

**MinIO 桶**：`cycling-lab`（建好即可，目录约定见架构文档 §6.9）。

---

## 7. 验收清单

安装完成后请逐项执行并把结果贴回来：

```bash
echo "== docker =="
docker --version && docker compose version

echo "== postgres =="
sudo -u postgres psql -c "SELECT version();" -tA
PGPASSWORD=$(grep -oP '^PG_PASS=\K.*' /root/.cycling-lab-secrets || true) \
  psql -h 172.19.38.126 -U cycling_lab -d cycling_lab \
  -c "SELECT current_database(), current_user, inet_server_addr();" 2>&1

echo "== redis =="
REDIS_PASS=$(grep -oP '^REDIS_PASSWORD=\K.*' /root/.cycling-lab-secrets)
redis-cli -a "$REDIS_PASS" -h 172.19.38.126 ping

echo "== minio =="
docker ps --filter name=minio --format '{{.Names}}\t{{.Status}}'
mc admin info local

echo "== nginx =="
nginx -t && systemctl is-active nginx
curl -sI http://127.0.0.1/ | head -1

echo "== ports (内网 IP) =="
ss -tlnp | grep -E ':(80|443|5432|6379|9000|9001) '

echo "== ports (公网能否访问敏感端口，必须失败) =="
for p in 5432 6379 9000 9001 8080; do
  echo -n "8.137.52.31:$p -> "
  timeout 3 bash -c "</dev/tcp/8.137.52.31/$p" 2>&1 && echo "OPEN (bad)" || echo "closed (good)"
done
```

**至少满足**：

- [ ] `docker compose version` 输出 `v2.x`
- [ ] Postgres 16 起来；`psql ... -h 172.19.38.126` 用 `cycling_lab` 账号能连 `cycling_lab` 库；`inet_server_addr()` 返回 `172.19.38.126`
- [ ] Redis `PONG` 通过（`-h 172.19.38.126`）
- [ ] MinIO 容器 `running`；`mc ls local/` 能看到 `cycling-lab` 桶
- [ ] Nginx 起来，`curl http://127.0.0.1/` 返回 200
- [ ] `ss` 看到的 5432 / 6379 / 9000 / 9001 监听地址**是** `172.19.38.126`，不是 `0.0.0.0`
- [ ] **公网 `8.137.52.31:5432/6379/9000/9001/8080` 必须连不上**（安全组挡掉），只 80/443 通
- [ ] 阿里云安全组入方向规则截图或 JSON 已回贴

---

## 8. 我想从你这里拿到的东西（回执）

请把以下内容**贴回对话**给我：

1. 全部 `ss -tlnp` 输出。
2. `docker ps` 输出（应有 minio 一行）。
3. `/root/.cycling-lab-secrets` 内容（**脱敏后**——password 只回显末 4 位即可，例如 `xxxx...a3F2`）。
4. `curl -sI http://127.0.0.1/` 的状态行。
5. 公网端口探测结果（5432/6379/9000/9001/8080 应全部 `closed`）。
6. 阿里云安全组入方向规则（控制台截图或 OpenAPI 返回的 JSON 均可）。
7. 任何安装失败 / 端口被占 / 缺权限的情况。

---

## 9. 已知坑

- **apt 锁**：`apt` 报 `Could not get lock /var/lib/dpkg/lock-frontend` 时，先 `ps -ef | grep -E 'apt|dpkg' | grep -v grep`，确认无人在跑后再重试，不要 `kill` 别人的 apt。
- **Postgres 监听地址**：本任务用 `listen_addresses = '172.19.38.126'`，从本机 `psql` 也要用 `-h 172.19.38.126`，不要用 socket（默认是 socket，但本任务以 TCP 验收）。
- **Postgres 远程连接**：必须同时改 `postgresql.conf` 的 `listen_addresses` 和 `pg_hba.conf`，并 `systemctl reload`（不是 restart）。本任务只绑内网，**不开**远程白名单。
- **MinIO 密码**：环境变量里若含 `!`、`$` 等 shell 特殊字符，必须单引号包起来，且 `mc alias set` 的密码用相同写法。
- **Docker Hub 限速**：若 `docker pull` 超时，改用 `registry.cn-hangzhou.aliyuncs.com/library/postgres:16-alpine` 等国内镜像，并在 `docker run` 时替换。
- **阿里云安全组生效有 1-2 分钟延迟**：配完规则后等一会儿再 `nc` 验证。
- **时区**：Postgres、Redis、Nginx 全部以 UTC 处理业务时间；展示由前端按用户时区渲染。如果跑批/日志需要看本地时间，机器时区可设 `Asia/Shanghai`。
- **不要**安装 PostgREST、Supabase、Hasura、Keycloak 等本项目没用的组件。
- **不要**在 `8.137.52.31` 上做 `0.0.0.0:PORT` 的对外监听（即便本机有安全组），一律绑内网 IP / 127.0.0.1。
