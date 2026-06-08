# 服务器软件安装清单

> 本文档记录本机所安装的全部软件、位置、配置、启动命令、端口等信息,作为运维速查表。
>
> ⚠️ **本文档含明文账号密码(Jenkins 登录等),仅个人本地使用,勿提交到公开仓库 / 截图外发。**
>
> **最后更新**: 2026-06-03

---

## 1. 系统基础信息

| 项目 | 值 |
|---|---|
| 操作系统 | Ubuntu 24.04.4 LTS (Noble Numbat) |
| 内核 | Linux 6.8.0-63-generic |
| 主机名 | `cycling-lab`(`hostnamectl set-hostname`,`/etc/hosts` 加 `127.0.0.1 cycling-lab`;阿里云实例 ID 仍是 `iZ2vc1piav73owa3jajpf0Z`,不可改) |
| 云厂商 | 阿里云 ECS |
| 公网 IP | `8.137.52.31` |
| 内网 IP | `172.19.38.126` |
| 防火墙 (UFW) | inactive(系统层未开启,**实际由阿里云安全组管控**) |

### 用户账号

| 用户 | 家目录 | 用途 |
|---|---|---|
| `root` | `/root` | 系统管理员 |
| `admin` | `/home/admin` | 阿里云默认管理用户 |
| `ai` | `/home/ai` | Claude Code 运行用户(已 sudo 提权过) |

---

## 2. 软件安装清单

### 2.1 Jenkins(CI/CD 自动化服务器)

| 项目 | 内容 |
|---|---|
| **版本** | 2.555.2 |
| **可执行文件** | `/usr/bin/jenkins` |
| **WAR 包** | `/usr/share/java/jenkins.war` |
| **数据目录 (JENKINS_HOME)** | `/var/lib/jenkins` |
| **缓存/Web 根目录** | `/var/cache/jenkins/war` |
| **systemd unit** | `/usr/lib/systemd/system/jenkins.service` |
| **systemd drop-in** | `/etc/systemd/system/jenkins.service.d/override.conf`(设 `JENKINS_LISTEN_ADDRESS=127.0.0.1` + `JENKINS_PREFIX=/jks`) |
| **环境变量配置** | `/etc/default/jenkins`(仅 SysV 兼容用,systemd 下被 drop-in 覆盖) |
| **APT 源** | `/etc/apt/sources.list.d/jenkins.list` |
| **GPG 密钥** | `/usr/share/keyrings/jenkins.gpg` |
| **监听端口** | `127.0.0.1:8080`(只绑本机,**不再公网直连**);公网经 nginx `19000/jks` 反代 |
| **运行用户** | `jenkins:jenkins` |
| **依赖** | Java 21+(本机使用 OpenJDK 21) |
| **访问地址** | http://8.137.52.31:19000/jks/(走 nginx 反代,需阿里云安全组放行 19000) |
| **初始管理员密码** | `/var/lib/jenkins/secrets/initialAdminPassword`(仅首次解锁用,解锁后已建账号) |
| **正式登录凭据** | `lxx` / `0ooo00o00o`(同步备份于 `/root/.cycling-lab-secrets` 的 `JENKINS_USER` / `JENKINS_PASSWORD`) |

**启动/管理命令**
```bash
systemctl start   jenkins          # 启动
systemctl stop    jenkins          # 停止
systemctl restart jenkins          # 重启
systemctl status  jenkins          # 查看状态
systemctl enable  jenkins          # 开机自启(已配)
journalctl -u jenkins -f           # 实时日志
journalctl -u jenkins -n 100       # 最近 100 行日志
cat /var/lib/jenkins/secrets/initialAdminPassword   # 取初始密码
```

**安装来源**: 官方 APT 源 `https://pkg.jenkins.io/debian-stable`

---

### 2.2 Java(OpenJDK)

本机同时安装了 Java 17 与 Java 21,**默认 java 指向 Java 21**(因 Jenkins 要求)。

| 版本 | 安装路径 | 说明 |
|---|---|---|
| OpenJDK **21**.0.11 | `/usr/lib/jvm/java-21-openjdk-amd64` | **默认**,Jenkins 使用 |
| OpenJDK **17**.0.19 | `/usr/lib/jvm/java-17-openjdk-amd64` | 保留备用 |

**当前默认入口**
```
/usr/bin/java   →  /etc/alternatives/java  →  /usr/lib/jvm/java-21-openjdk-amd64/bin/java
/usr/bin/javac  →  /etc/alternatives/javac →  /usr/lib/jvm/java-21-openjdk-amd64/bin/javac
```

**版本切换命令**
```bash
# 交互式切换默认 java
update-alternatives --config java
update-alternatives --config javac

# 查看当前指向
update-alternatives --display java
java -version
```

**APT 包名**: `openjdk-17-jdk-headless`、`openjdk-21-jdk-headless`

---

### 2.3 Node.js + npm

| 项目 | 内容 |
|---|---|
| Node 版本 | v22.22.2 |
| npm 版本 | 10.9.7 |
| node 路径 | `/usr/bin/node` |
| npm 路径 | `/usr/bin/npm` |
| 全局包目录 | `/usr/lib/node_modules` |

**已装的全局 npm 包**
| 包名 | 版本 | 用途 |
|---|---|---|
| `@anthropic-ai/claude-code` | 2.1.160 | Claude Code CLI |
| `corepack` | 0.34.6 | Node 包管理器代理(yarn/pnpm) |
| `npm` | 10.9.7 | npm 自身 |

**常用命令**
```bash
node -v
npm -v
npm list -g --depth=0       # 看全局包
npm install -g <pkg>        # 装全局包
claude                      # 启动 Claude Code CLI
```

---

### 2.4 Python

| 项目 | 内容 |
|---|---|
| 版本 | Python 3.12.3 |
| 路径 | `/usr/bin/python3` |
| pip 版本 | 24.0 |
| pip 路径 | `/usr/bin/pip3` (也可用 `pip`) |
| pip 配置 | `/root/.pydistutils.cfg` |

**注意**: Ubuntu 24.04 默认未提供 `python` 软链接,只有 `python3`。如需要可:
```bash
apt-get install -y python-is-python3
```

---

### 2.5 Git

| 项目 | 内容 |
|---|---|
| 版本 | 2.43.0 |
| 路径 | `/usr/bin/git` |

---

### 2.6 基础工具(系统自带 / apt 装的)

| 工具 | 路径 | 说明 |
|---|---|---|
| `vim` | `/usr/bin/vim` | 文本编辑器 |
| `curl` | `/usr/bin/curl` | HTTP 客户端 |
| `wget` | `/usr/bin/wget` | 文件下载 |
| `tar` | `/usr/bin/tar` | 归档 |
| `jq` | `/usr/bin/jq` | JSON 处理 |
| `htop` | `/usr/bin/htop` | 进程监控 |
| `make` | `/usr/bin/make` | 构建工具 |
| `gcc` / `g++` | `/usr/bin/gcc` / `/usr/bin/g++` | C/C++ 编译器 |

---

### 2.7 Docker Engine + Compose Plugin(容器运行时)

| 项目 | 内容 |
|---|---|
| **版本** | Docker CE 29.5.2;docker-compose-plugin 5.1.4 |
| **可执行文件** | `/usr/bin/docker`(`/usr/bin/dockerd`、`/usr/bin/docker-proxy`) |
| **Compose 插件路径** | `/usr/libexec/docker/cli-plugins/docker-compose` |
| **数据目录** | `/var/lib/docker` |
| **systemd unit** | `/lib/systemd/system/docker.service`(socket 触发:`docker.socket`) |
| **APT 源** | `https://mirrors.aliyun.com/docker-ce/linux/ubuntu noble stable`(`/etc/apt/sources.list.d/docker.list`) |
| **GPG 密钥** | `/etc/apt/keyrings/docker.gpg` |
| **监听端口** | 无(daemon 本地 unix socket);容器端口由 `docker-proxy` 转发 |
| **运行用户** | `root`(daemon);容器内自定义 |
| **依赖** | `containerd.io`、`docker-buildx-plugin` |

**启动/管理命令**
\`\`\`bash
systemctl start   docker
systemctl status  docker
journalctl -u docker -f
docker ps -a
docker compose version
\`\`\`

**说明**: v0.1 仅用于跑 MinIO;后端 jar / 前端 dist 暂未容器化,直接由 apt 包(JDK 21 / Node 22)裸机跑(M0 部署阶段决定)。

---

### 2.8 PostgreSQL 16(主数据库)

| 项目 | 内容 |
|---|---|
| **版本** | 16.14(`postgresql-16 16.14-1.pgdg24.04+1`) |
| **可执行文件** | `/usr/lib/postgresql/16/bin/postgres`、`/usr/bin/psql` |
| **数据目录** | `/var/lib/postgresql/16/main` |
| **配置目录** | `/etc/postgresql/16/main/`(`postgresql.conf`、`pg_hba.conf`、`pg_ident.conf`) |
| **APT 源** | `https://mirrors.aliyun.com/postgresql/repos/apt noble-pgdg main`(`/etc/apt/sources.list.d/pgdg.list`) |
| **GPG 密钥** | `/etc/apt/keyrings/pgdg.gpg` |
| **systemd unit** | `/lib/systemd/system/postgresql.service`(实际由 `postgresql@16-main.service` 实例承载) |
| **监听端口** | `5432/TCP`,**只绑内网 IP** `172.19.38.126`(不绑 0.0.0.0) |
| **公网反代** | TCP 四层 → `25432`(nginx `stream` 模块,见 §2.12 / `stream.d/middleware.conf`) |
| **运行用户** | `postgres:postgres` |
| **应用库 / 用户** | `cycling_lab` / `cycling_lab`(密码见 `/root/.cycling-lab-secrets`) |

**关键配置(`postgresql.conf`)**
```
listen_addresses = '172.19.38.126'   # 不绑公网,不绑 0.0.0.0
port = 5432
data_directory = '/var/lib/postgresql/16/main'
unix_socket_directories = '/var/run/postgresql'
```

**`pg_hba.conf` 允许的来源**
- `local` 全量 `peer`(本机 socket 同名 OS 用户)
- `host 127.0.0.1/32`、`172.19.38.126/32`、`::1/128` 走 `scram-sha-256`
- **不开放** `0.0.0.0/0` 远程白名单

**启动/管理命令**
\`\`\`bash
systemctl status postgresql
systemctl reload  postgresql        # 改 postgresql.conf / pg_hba.conf 后
sudo -u postgres psql
PGPASSWORD=$(grep ^PG_PASS= /root/.cycling-lab-secrets | cut -d= -f2) \
  psql -h 172.19.38.126 -U cycling_lab -d cycling_lab
\`\`\`

**说明**: 阿里云安全组**不放行** 5432(原始端口),公网 `nc 8.137.52.31 5432` 应 `closed`;本地电脑走公网 `8.137.52.31:25432` 经 nginx stream 代理(本地端 `psql -h 8.137.52.31 -p 25432 -U cycling_lab -d cycling_lab`)。

---

### 2.9 Redis 7(缓存 / 会话)

| 项目 | 内容 |
|---|---|
| **版本** | 7.0.15(`redis 5:7.0.15-1ubuntu0.24.04.4`) |
| **可执行文件** | `/usr/bin/redis-server`、`/usr/bin/redis-cli` |
| **数据目录** | `/var/lib/redis/`(`dump.rdb`) |
| **配置目录** | `/etc/redis/redis.conf`(`/etc/redis/redis.conf.bak` 为初次安装备份) |
| **APT 源** | `https://mirrors.aliyun.com/redis/deb noble main`(`/etc/apt/sources.list.d/redis.list`) |
| **GPG 密钥** | `/etc/apt/keyrings/redis-archive-keyring.gpg` |
| **systemd unit** | `/lib/systemd/system/redis-server.service` |
| **监听端口** | `6379/TCP`,**只绑内网 IP** `172.19.38.126` |
| **公网反代** | TCP 四层 → `26379`(nginx `stream` 模块,见 §2.12 / `stream.d/middleware.conf`) |
| **运行用户** | `redis:redis` |
| **认证** | `requirepass` 已设置,值见 `/root/.cycling-lab-secrets` 的 `REDIS_PASSWORD` |
| **`protected-mode`** | `no`(配合 requirepass 使用) |

**关键配置(`/etc/redis/redis.conf`)**
```
bind 172.19.38.126           # 不绑公网
protected-mode no
port 6379
daemonize yes
dir /var/lib/redis
logfile /var/log/redis/redis-server.log
requirepass <REDIS_PASSWORD> # 见 secrets
```

**启动/管理命令**
\`\`\`bash
systemctl status redis-server
systemctl restart redis-server
REDIS_PASS=$(grep ^REDIS_PASSWORD= /root/.cycling-lab-secrets | cut -d= -f2)
redis-cli -a "$REDIS_PASS" -h 172.19.38.126 ping
redis-cli -a "$REDIS_PASS" -h 172.19.38.126 INFO server | head
\`\`\`

**说明**: 阿里云安全组**不放行** 6379(原始端口),公网 `nc 8.137.52.31 6379` 应 `closed`;本地电脑走公网 `8.137.52.31:26379`(`redis-cli -h 8.137.52.31 -p 26379 -a $REDIS_PASSWORD PING`)。

---

### 2.10 MinIO Server(对象存储,Docker 容器)

| 项目 | 内容 |
|---|---|
| **版本** | `RELEASE.2025-09-07T16-13-09Z`(`go1.24.6`,镜像 `minio/minio:latest`) |
| **容器名** | `minio`(`docker run -d --name minio --restart=always`) |
| **数据卷** | 宿主机 `/var/lib/minio` → 容器 `/data`(单盘) |
| **镜像** | `minio/minio:latest` |
| **监听端口** | API `9000`、Console `9001`,**只绑内网 IP** `172.19.38.126`(经 `docker-proxy` 转发) |
| **公网反代** | HTTP 七层 → `19000/minio/`(API)、`19000/minio-console/`(Web UI),见 §2.12 / `sites-enabled/jenkins` |
| **根用户** | `minioadmin` / `CHANGE_ME_MINIO_8cc79d3ffd72cf8bb25ef6c5`(**明文也写在这里方便 vibe coding 引用**;`MINIO_ROOT_PASSWORD` 同步在 `/root/.cycling-lab-secrets`) |
| **启动参数** | `minio server /data --console-address ":9001"` |
| **桶** | `cycling-lab`(`mb -p`,已开启 `anonymous download`) |
| **重启策略** | `always`(随 Docker 守护进程) |

**启动/管理命令**
\`\`\`bash
docker ps --filter name=minio
docker logs minio --tail 50
docker restart minio
mc admin info local                  # 需先 mc alias set local ...
\`\`\`

**说明**: 阿里云安全组**不放行** 9000/9001,管理走 SSH 隧道(如 `ssh -L 19001:127.0.0.1:9001 root@8.137.52.31` 后浏览器开 `http://127.0.0.1:19001`)。

---

### 2.11 MinIO Client `mc`(桶 / 对象管理 CLI)

| 项目 | 内容 |
|---|---|
| **版本** | `RELEASE.2025-07-21T05-28-08Z`(`go1.24.5`) |
| **可执行文件** | `/usr/local/bin/mc` |
| **安装方式** | 官方二进制,从 `https://dl.minio.org.cn/client/mc/release/linux-amd64/mc` 下载(`dl.min.io` 国内访问慢) |
| **配置文件** | `~/.mc/config.json` |
| **当前 alias** | `local` → `http://172.19.38.126:9000` |
| **桶** | `cycling-lab`(`anonymous download` 已开) |

**常用命令**
\`\`\`bash
mc --version
mc ls local/
mc ls local/cycling-lab/
mc cp <local-file> local/cycling-lab/<key>
mc anonymous set download local/cycling-lab
mc admin info local
\`\`\`

**说明**: 后续 fit/zwo 截图/md 笔记会按 `architecture.md §6.9` 约定的目录结构上传;前端取公开读地址时,桶内对象可匿名下载。

---

### 2.12 Nginx(HTTP 反代 + 静态托管)

| 项目 | 内容 |
|---|---|
| **版本** | 1.24.0(`nginx 1.24.0-2ubuntu7.9`,Ubuntu noble 默认源) |
| **可执行文件** | `/usr/sbin/nginx` |
| **主配置** | `/etc/nginx/nginx.conf`(`include /etc/nginx/sites-enabled/*` + `stream { include /etc/nginx/stream.d/*.conf; }`) |
| **项目 vhost** | `/etc/nginx/sites-available/cycling-lab`(`sites-enabled/cycling-lab` 软链) + `/etc/nginx/sites-available/jenkins`(`sites-enabled/jenkins` 软链) |
| **TCP 反代配置** | `/etc/nginx/stream.d/middleware.conf`(`25432`→PG,`26379`→Redis) |
| **动态模块** | `libnginx-mod-stream`(apt 装,`/etc/nginx/modules-enabled/50-mod-stream.conf`) |
| **静态根目录** | `/var/www/cycling-lab/`(当前为 `<h1>cycling-lab placeholder</h1>` 占位) |
| **systemd unit** | `/lib/systemd/system/nginx.service` |
| **APT 源** | Ubuntu noble 默认源 + `libnginx-mod-stream`(主仓库自带) |
| **监听端口** | HTTP:`80/TCP`(cycling-lab 占位站) + `19000/TCP`(`/jks/`、`/minio/`、`/minio-console/`);TCP(stream):`25432`(PG)+ `26379`(Redis);均绑 `0.0.0.0` + `[::]`,经阿里云安全组公网放行 |
| **运行用户** | `www-data:www-data`(master 为 `root`) |
| **客户端体积上限** | HTTP 站点 50m;MinIO API 路径不限(`client_max_body_size 0`) |

**当前 vhost(`/etc/nginx/sites-available/cycling-lab`)**
```nginx
server {
    listen 80 default_server;
    server_name _;
    root /var/www/cycling-lab;
    index index.html;
    client_max_body_size 50m;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;     # M0 后端起后再确认 upstream
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
```

**启动/管理命令**
\`\`\`bash
systemctl status nginx
nginx -t                  # 改完配置必跑
systemctl reload nginx
curl -sI http://127.0.0.1/
\`\`\`

**说明**: 443/TLS 在上 HTTPS 时再加 `listen 443 ssl;` 与证书。当前 `default_server` 占用 80,已 `rm /etc/nginx/sites-enabled/default`。**注意**:`proxy_pass http://127.0.0.1:8080` 现阶段后端未起,只是占位配置;同时 `8080` 已被 Jenkins 占用(见 §2.1 与 §4),M0 部署后端时需先解决 8080 冲突。

---

## 3. 阿里云相关组件(系统预装,勿动)

| 服务 | 路径 | 用途 |
|---|---|---|
| `aegis.service` | `/usr/local/aegis` | 阿里云**云安全中心**客户端 |
| `aliyun.service` | — | 阿里云助手(运维通道) |
| `cloudmonitor.service` | `/usr/local/cloudmonitor` | 阿里云**云监控**插件 |
| `ecs_mq.service` | — | ECS 消息队列服务 |
| 安装脚本 | `/usr/local/agent_install-1.16.sh` | 监控代理安装脚本 |

⚠️ **以上服务由阿里云控制台管理,正常情况不要手动停止或卸载**,否则云监控、安全中心会异常。

---

## 4. 当前监听端口一览

| 端口 | 协议 | 进程 | 绑定地址 | 公网 | 说明 |
|---|---|---|---|---|---|
| **22** | TCP | `sshd` | `0.0.0.0` + `[::]` | ✅ 阿里云放行 | SSH 远程登录 |
| **53** | TCP | `systemd-resolve` | `127.0.0.54` 等本地 | ❌ | 本机 DNS 解析,无外网影响 |
| **80** | TCP | `nginx` | `0.0.0.0` + `[::]` | ✅ 阿里云放行 | Nginx 入口;`/api/*` 反代 127.0.0.1:8080,其余走 `/var/www/cycling-lab` |
| **5432** | TCP | `postgres` | `172.19.38.126` | ❌ 安全组挡 | PostgreSQL 16,**只内网**(公网走 `25432` nginx stream 反代) |
| **6379** | TCP | `redis-server` | `172.19.38.126` | ❌ 安全组挡 | Redis 7(`requirepass` 已设,**只内网**;公网走 `26379` nginx stream 反代) |
| **9000** | TCP | `docker-proxy` | `172.19.38.126` | ❌ 安全组挡 | MinIO API(**只内网**;公网走 `19000/minio/` nginx 反代) |
| **9001** | TCP | `docker-proxy` | `172.19.38.126` | ❌ 安全组挡 | MinIO Console(**只内网**;公网走 `19000/minio-console/` nginx 反代) |
| **8080** | TCP | `java` (Jenkins) | `127.0.0.1` | ❌ 不再公网 | Jenkins,**仅本机**(经 nginx `19000/jks` 反代) |
| **19000** | TCP | `nginx` | `0.0.0.0` + `[::]` | ⏳ 待阿里云放行 | 反代总入口:HTTP 路径 `/jks/`(Jenkins)、`/minio/`(S3 API)、`/minio-console/`(Web UI) |
| **25432** | TCP | `nginx` (stream) | `0.0.0.0` + `[::]` | ⏳ 待阿里云放行 | PostgreSQL TCP 四层反代 → `172.19.38.126:5432` |
| **26379** | TCP | `nginx` (stream) | `0.0.0.0` + `[::]` | ⏳ 待阿里云放行 | Redis TCP 四层反代 → `172.19.38.126:6379` |

**实时查看监听端口**
```bash
ss -ltnp                   # 监听中的 TCP 端口
ss -lunp                   # 监听中的 UDP 端口
ss -tnp                    # 已建立的 TCP 连接
```

---

## 5. systemd 服务速查

```bash
# 列出所有已启用的服务(开机自启)
systemctl list-unit-files --state=enabled --type=service

# 列出所有正在运行的服务
systemctl list-units --type=service --state=running

# 通用服务管理
systemctl status   <服务名>
systemctl start    <服务名>
systemctl stop     <服务名>
systemctl restart  <服务名>
systemctl enable   <服务名>     # 设为开机自启
systemctl disable  <服务名>     # 取消开机自启

# 日志
journalctl -u <服务名>            # 历史日志
journalctl -u <服务名> -f         # 实时跟随
journalctl -u <服务名> -n 200     # 最近 200 行
journalctl -u <服务名> --since "1 hour ago"
```

---

## 6. 重要路径汇总

| 路径 | 内容 |
|---|---|
| `/data/doc/` | **本文档所在目录** |
| `/var/lib/jenkins/` | Jenkins 数据(JENKINS_HOME) |
| `/var/lib/postgresql/16/main/` | PostgreSQL 16 数据目录 |
| `/var/lib/redis/` | Redis 持久化(`dump.rdb`) |
| `/var/lib/minio/` | MinIO 对象存储数据(`/data` 挂载点) |
| `/var/lib/docker/` | Docker 镜像 / 容器 / 卷根 |
| `/var/www/cycling-lab/` | Nginx 静态资源根(当前为占位) |
| `/etc/postgresql/16/main/` | PostgreSQL 配置(`postgresql.conf` / `pg_hba.conf`) |
| `/etc/redis/` | Redis 配置(`redis.conf` + `.bak`) |
| `/etc/nginx/` | Nginx 配置根(`sites-available/cycling-lab`) |
| `/usr/local/bin/mc` | MinIO Client 二进制 |
| `/root/.cycling-lab-secrets` | **中间件凭据集中文件**(0600,root only) |
| `/usr/lib/jvm/` | Java 多版本根目录 |
| `/etc/apt/sources.list.d/` | APT 第三方源(`docker.list`、`pgdg.list`、`redis.list`、`nodesource.sources`、`jenkins.list`) |
| `/etc/apt/keyrings/` | APT GPG 密钥(`docker.gpg`、`pgdg.gpg`、`redis-archive-keyring.gpg`、…) |
| `/usr/lib/systemd/system/` | systemd 服务单元 |
| `/etc/default/` | 各服务默认环境变量 |
| `/var/log/` | 系统日志 |

---

## 7. 安装历史 / 变更记录

| 日期 | 操作 | 备注 |
|---|---|---|
| 2026-06-02 | 安装 Jenkins 2.555.2 | 通过官方 APT 源 |
| 2026-06-02 | 安装 OpenJDK 21 | 因 Jenkins 2.555 要求 Java 21+,Java 17 启动失败,补装 |
| 2026-06-02 | 启动 jenkins.service | 端口 8080,设置开机自启 |
| 2026-06-02 | 建立本文档 | `/data/doc/SERVER_INSTALLATION.md` |
| 2026-06-02 | 安装 Docker CE 29.5.2 + docker-compose-plugin 5.1.4 | 阿里云镜像 `mirrors.aliyun.com/docker-ce`,systemd 托管 |
| 2026-06-02 | 安装 PostgreSQL 16.14 | 阿里云镜像 `mirrors.aliyun.com/postgresql`,建库 `cycling_lab` + 用户 `cycling_lab`,密码写入 `/root/.cycling-lab-secrets` |
| 2026-06-02 | 配置 PostgreSQL 监听 | `listen_addresses = '172.19.38.126'`,`pg_hba.conf` 仅 127.0.0.1/32 + 172.19.38.126/32,不开远程白名单 |
| 2026-06-02 | 安装 Redis 7.0.15 | 阿里云镜像 `mirrors.aliyun.com/redis`,`bind 172.19.38.126` + `requirepass` |
| 2026-06-02 | 启动 MinIO 容器(`minio/minio:latest`) | 数据卷 `/var/lib/minio` → `/data`,端口 9000/9001 只绑内网,`--restart=always` |
| 2026-06-02 | 安装 MinIO Client `mc` RELEASE.2025-07-21 | 国内镜像 `dl.minio.org.cn`,配置 alias `local` → `http://172.19.38.126:9000` |
| 2026-06-02 | 创建 MinIO 桶 `cycling-lab` | `mc mb -p` + `mc anonymous set download` |
| 2026-06-02 | 安装 Nginx 1.24.0 | Ubuntu noble 默认源,占位 vhost `/etc/nginx/sites-available/cycling-lab`,删除 default site |
| 2026-06-02 | 补齐 `/root/.cycling-lab-secrets` | 集中保存 PG/Redis/MinIO 凭据(0600,root only) |
| 2026-06-03 | 配置变更:Jenkins 收口到 127.0.0.1 | `systemctl edit` 加 drop-in 设 `JENKINS_LISTEN_ADDRESS=127.0.0.1` + `JENKINS_PREFIX=/jks`,`ss` 确认 8080 仅 `[::ffff:127.0.0.1]:8080` |
| 2026-06-03 | 新增 nginx vhost `jenkins` | 新建 `/etc/nginx/sites-available/jenkins`,`listen 19000`,`location /jks/` 透传 `http://127.0.0.1:8080`,带 `X-Forwarded-Prefix=/jks` + WebSocket 头 |
| 2026-06-03 | 验证 `19000/jks` 反代 | `curl /jks/login` 返回 200,`Set-Cookie: Path=/jks` 确认 Jenkins 识别前缀;公网 19000 需阿里云安全组放行 |
| 2026-06-03 | 登记 Jenkins 登录凭据 | `JENKINS_USER` / `JENKINS_PASSWORD` 追加到 `/root/.cycling-lab-secrets`(0600,root only);**为方便 vibe coding 时下载引用,本文档也直接写明文** |
| 2026-06-03 | 改主机名 | `hostnamectl set-hostname cycling-lab` + `/etc/hosts` 加 `127.0.0.1 cycling-lab`;阿里云实例 ID 不可改 |
| 2026-06-03 | nginx 装 `libnginx-mod-stream` | 默认 `nginx` 包不带 stream 模块,apt 装动态模块并启用(`/etc/nginx/modules-enabled/50-mod-stream.conf`) |
| 2026-06-03 | nginx 加 19000 下 MinIO 路径反代 | `sites-enabled/jenkins` 加 `location ^~ /minio/`(`172.19.38.126:9000`)+ `/minio-console/`(`172.19.38.126:9001`),带 WebSocket 头,MinIO API 关 buffer 放大文件 |
| 2026-06-03 | nginx stream 四层反代 PG/Redis | 新建 `/etc/nginx/stream.d/middleware.conf`,`25432→172.19.38.126:5432`、`26379→172.19.38.126:6379`;`nginx.conf` 顶层加 `stream { include /etc/nginx/stream.d/*.conf; }` |
| 2026-06-03 | 验证 4 个新代理 | MinIO Console SPA 返回 200 + `mc cp/cat/rm` round-trip 通;PG 经 `25432` 用 `psql` 查 `version()` 成功;Redis 经 `26379` PING/SET/GET/DEL 全通 |

---

## 8. 常用速查命令

```bash
# === 系统资源 ===
df -h                      # 磁盘空间
free -h                    # 内存
htop                       # 进程/CPU 监控(q 退出)
uptime                     # 负载

# === 网络 ===
ip a                       # 网卡 IP
ss -ltnp                   # 监听端口
curl ifconfig.me           # 查公网 IP

# === 包管理 ===
apt update                 # 刷新源
apt list --installed       # 已装包
apt-get install <pkg>      # 安装
apt-get remove <pkg>       # 卸载(保留配置)
apt-get purge  <pkg>       # 彻底卸载

# === 用户切换 ===
su - admin
su - ai
sudo -i                    # 提权到 root
```

---

> 📌 **维护建议**
> - 后续每次新装/卸载软件,记得回来更新本文档对应章节与「安装历史」表。
> - 阿里云相关组件(aegis/aliyun/cloudmonitor/ecs_mq)不要随意改动。
> - 重要服务变更前,先备份 `/var/lib/jenkins`、`/etc/` 下相关配置。
