# 营养助手 Web 生产部署手册

> 适用版本：已完成「生产安全修复 + 手机号注册/重置密码」改造后的版本
> 编写日期：2026-09-24
> 默认目标：把用户 H5 网站部署到现有域名，Java 后端和 Python AI 只在服务器内网监听；MySQL、Redis 不暴露公网。
> 本文不部署微信小程序。管理后台作为可选站点。

---

## 0. 先说结论

### 0.1 能不能直接上线？

**可以上线（Web/H5 网站）。** 之前列出的上线阻断项已在本仓库代码中全部修复，按本文第 2 节以后部署即可公网访问。

实际构建与测试验证结果：

- Java 后端：`mvn test` 通过（26 个单元测试全绿），`mvn -DskipTests package` 通过。
- 管理后台：`npm run build` 通过。
- H5 网站：`npm run build:h5` 通过，产物中已无 `localhost:8088`。
- 小程序不作为本次目标（本次只上线网站）。

### 0.2 已修复的上线阻断项

| 编号 | 风险 | 现状 |
|---|---|---|
| 1 | H5 写死 `http://localhost:8088`，别人访问会请求访问者本机 | **已修复**：4 个文件改为同域相对路径 `BASE_URL = ''`，请求走当前站点 `/api` |
| 2 | 注册会写 `password_encrypted`，而建库 SQL 无该字段 | **已修复**：删除 `password_encrypted` 字段与写入逻辑，建库 SQL 已含 `phone` 字段 |
| 3 | `food_nutrition` 表无数据 | 仍需部署时导入 CSV，见第 5.3 节 |
| 4 | 启动自动创建 `test / 123456` | **已修复**：`DataInitializer` 加 `@Profile("dev")`，生产环境不再创建 |
| 5 | JWT、AES 有固定默认密钥 | **已修复**：移除默认值，未设置 `JWT_SECRET` / `AI_AES_KEY` 时启动失败 |
| 6 | `/api/attachment/**` 整体免鉴权（含上传/删除） | **已修复**：从白名单移除，上传、删除均需登录 |
| 7 | Python 回调接口未校验 API Key | **已修复**：`/api/rag/knowledge/callback`、`/api/chat/session/*` 回调校验 `FASTAPI_SECRET_KEY` |
| 8 | AI 配置、审核、知识库接口只校验登录 | **已修复**：`/api/ai/config`、`/api/audit/**`、`/api/rag/knowledge/**` 仅 ADMIN 可访问 |
| 9 | 用户密码可逆 AES 存储、管理端可解密查看 | **已修复**：只保留 BCrypt 哈希；管理端不再展示密码，改展示手机号 |
| 10 | Nginx、systemd、Dockerfile、Compose、CI 不在仓库 | Nginx/systemd 需按本文手工配置（Dockerfile/Compose/CI 仍不在仓库） |

### 0.3 账号体系变更（本次）

- 注册新增 **手机号** 字段，必须为合法 11 位中国大陆手机号（`^1[3-9]\d{9}$`）。
- **用户名唯一**，**手机号不唯一**（同一手机号可注册多个账号）。
- 用户密码只存 BCrypt 哈希，**不可逆**，管理端无法查看明文。
- 登录页新增 **「重置密码」** 入口：输入「用户名 + 注册手机号 + 新密码」，手机号与该用户名不匹配时提示「手机号错误」。

> 安全提示：重置密码仅以「用户名 + 手机号」校验身份，未接入短信验证码，属于“知道手机号即可重置”的弱校验。若面向真实用户开放，建议后续接入短信验证码（SMS OTP）加固。

---

## 1. 本次已完成的安全修复（无需你再改源码）

以下改动已经提交在代码里，部署时**不需要**再手动改源码或执行兼容 DDL，直接构建即可。

### 1.1 H5 API 地址已改为同域

这 4 个文件原本写死 `http://localhost:8088`，现已统一改为空字符串（同域相对路径）：

```text
nutrition-miniapp/src/api/index.ts
nutrition-miniapp/src/api/request.ts
nutrition-miniapp/src/api/add/add.ts
nutrition-miniapp/src/api/shouye/index.ts
```

```ts
// 请求 /api/... 会走当前网站域名，不依赖访问者本机
const BASE_URL = ''
```

因此 Nginx 必须把 `https://你的域名/api/**` 反代到 Java 后端（见第 10 节）。
若将来前后端分域，再改成 `const BASE_URL = 'https://api.example.com'`。

### 1.2 测试账号初始化已禁用

`DataInitializer` 已加 `@Profile("dev")`，生产环境（默认 profile）不会创建 `test / 123456`。
只有在显式 `--spring.profiles.active=dev` 时才会创建测试账号。

### 1.3 JWT 与 AES 已移除默认密钥

- `application.yml` 的 `jwt.secret` 改为 `${JWT_SECRET}`，无默认值。
- `AesUtil` 的 `ai.aes.key` 改为 `${ai.aes.key:}`，为空时启动直接抛异常。

**因此 `JWT_SECRET` 与 `AI_AES_KEY` 现在是必填项**，不设置后端起不来。生成示例：

```bash
openssl rand -base64 64   # JWT_SECRET
openssl rand -base64 48   # AI_AES_KEY
```

> `AI_AES_KEY` 用于加密 `ai_config` 表里的模型 API Key。若数据库里已有用旧密钥加密的 AI 配置，换密钥前需先迁移，否则历史密文解不开。

### 1.4 鉴权白名单与权限边界已收紧

- `/api/attachment/**` 已从白名单整体移除：上传、批量上传、删除、批量删除均需登录 JWT；上传接口以 JWT 用户为准，忽略请求体里的 `userId`，防止越权替他人上传。
  - **例外**：`GET /api/attachment/{id}/url` 仍公开只读，因为前端 `<image>` 标签直接引用它（无法携带 JWT）。它只返回 OSS 文件地址，不提供上传/删除能力。
  - 若将来 OSS 改为私有 Bucket，需要一并调整该接口与图片展示方案（改用签名 URL）。
- `/api/rag/knowledge/callback`、`/api/chat/session/create`、`/api/chat/session/flush`、`/api/chat/session/{id}/history` 免 JWT，但**控制器会校验 `Authorization: Bearer <FASTAPI_SECRET_KEY>`**，密钥来自 `fastapi.api-secret-key`（环境变量 `FASTAPI_SECRET_KEY`）。
- `/api/admin/**`、`/api/ai/config`（含子路径）、`/api/audit/**`、`/api/rag/knowledge/**` 仅 `ADMIN` 角色可访问，普通用户返回 403。

### 1.5 用户密码已改为不可逆存储

- 删除 `SysUser.passwordEncrypted` 字段、`sys_user.password_encrypted` 列引用和写入逻辑。
- 用户密码只保留 `password_hash`（BCrypt）。
- 管理端用户列表不再展示密码，改为展示手机号。

> 建库 SQL（`nutrition_db.sql`）已同步为只含 `password_hash`，**不要再执行任何 `ADD COLUMN password_encrypted` 的兼容 DDL**。

### 1.6 账号体系：手机号注册 + 重置密码

- `sys_user` 新增 `phone` 列（不唯一），注册必填，格式校验 `^1[3-9]\d{9}$`。
- 用户名仍唯一（`uk_username`）。
- 新增接口 `POST /api/auth/reset-password`：入参 `username`、`phone`、`newPassword`、`confirmPassword`；手机号与用户名不匹配时统一返回「手机号错误」。
- 登录页新增「重置密码」入口。

### 1.7 第二轮生产加固（本轮已完成）

| 项目 | 现状 | 是否需你操作 |
|---|---|---|
| Swagger / OpenAPI 公网匿名访问 | **已关闭**：默认 `SWAGGER_ENABLED=false`，且过滤器把 `/swagger-ui/**`、`/v3/api-docs/**` 纳入 ADMIN 鉴权，非管理员一律 401/403 | 无需 |
| CORS 任意来源 | **已收紧**：去掉 `allowedOriginPatterns("*")`，改为读取 `CORS_ALLOWED_ORIGINS` 白名单，默认不开放跨域 | 可选配置 |
| 敏感内容写日志 | **已脱敏**：日志级别默认 `INFO`；聊天消息、prompt、搜索词、审核文本、食物描述等改为只记录**长度** | 无需 |
| OSS 默认 PublicRead | **代码已支持私有模式**：`OSS_PRIVATE_BUCKET=true` 时不再设公开读，改为后端生成短期签名 URL | **需要**（见 1.8） |
| 管理端登录限流 / 短信验证码 | **本轮未改**（按需求跳过） | 后续按需 |
| 前端依赖漏洞 | **已修复一批**：小程序 52→43，管理后台 5→2；剩余项只能靠破坏性大版本升级（vite 8 / uni-app 工具链重版） | 见 1.9 |
| Python 依赖未锁定 | **已锁定**：新增 `nutrition-AI/requirements.lock.txt`（143 个包精确版本） | 无需 |

### 1.8 OSS 私有 Bucket：需要你在阿里云控制台做的改动

**需要**。代码侧已支持，但要真正生效必须同时完成控制台操作：

1. 登录阿里云控制台 → OSS → 选择 Bucket（当前 `nutrition-file-202607`）。
2. 「读写权限」从「公共读」改为「**私有**」。
3. 在服务器 `/etc/nutrition/server.env` 增加：

```dotenv
OSS_PRIVATE_BUCKET=true
OSS_SIGNED_URL_EXPIRE_SECONDS=3600
```

4. 重启后端：`sudo systemctl restart nutrition-server`。

说明：

- 改为私有后，**已存在的旧对象**在 Bucket 设为私有后立即不可匿名访问，无需逐个改 ACL。
- 后端会在返回头像、动态图片、附件 URL 时自动生成有效期 1 小时的签名 URL。
- 微信内容审核需要可访问的图片地址，代码已自动改用签名 URL，无需额外配置。
- **若暂时不做控制台改动**，保持 `OSS_PRIVATE_BUCKET=false` 即可，系统行为与现在完全一致（图片公开可访问）。

### 1.9 前端依赖漏洞现状

已执行 `npm audit fix` 并回归构建通过：

| 项目 | 修复前 | 修复后 | 剩余说明 |
|---|---|---|---|
| `nutrition-miniapp` | 52（9 low / 29 moderate / 14 high） | **43（9 low / 25 moderate / 9 high）** | 剩余全部位于 uni-app 构建工具链（`@dcloudio/*`、`jimp`、`jest`、`vite`），**不进入部署产物** |
| `nutrition-admin` | 5（2 moderate / 3 high） | **2（1 moderate / 1 high）** | 剩余为 `esbuild`/`vite`，修复需升到 vite 8（破坏性） |

重要结论：已核验 H5 构建产物（31 个 JS，共 1.29 MB）中**不含** `intlify`、`vue-i18n`、`jimp`、`jest`、`adm-zip` 等受影响包，说明剩余漏洞属于**构建期工具链**，不会随网站发布给终端用户。

如需彻底清零，需要整体升级 uni-app / vite 大版本，属于破坏性变更，建议单独排期并完整回归后再做。

---

## 2. 推荐部署拓扑

本文默认使用一个主域名：

| 组件 | 地址 | 监听位置 |
|---|---|---|
| 用户 H5 网站 | `https://www.example.com` | Nginx 443 |
| Java API | `https://www.example.com/api/**` | Nginx 反代到 `127.0.0.1:8088` |
| Python AI | 仅服务器内部 | `127.0.0.1:8004` |
| MySQL | 仅服务器内部 | `127.0.0.1:3306` |
| Redis | 仅服务器内部 | `127.0.0.1:6379` |
| 管理后台（可选） | `https://admin.example.com` | Nginx 443 |

将文中占位符替换成你的实际值：

```text
WEB_DOMAIN=www.example.com
ADMIN_DOMAIN=admin.example.com
SERVER_IP=203.0.113.10
APP_USER=nutrition
APP_HOME=/opt/nutrition-web
```

下面命令以 Ubuntu 22.04/24.04、root 或 sudo 权限、MySQL 8/Redis 7 为例。不同发行版只需替换包管理命令。

---

## 3. 服务器基础环境

### 3.1 安装运行组件

```bash
sudo apt update
sudo apt install -y \
  openjdk-17-jdk \
  nginx mysql-server redis-server \
  python3 python3-venv python3-pip \
  curl unzip ca-certificates apache2-utils
```

生产服务器不再安装 Maven 和 Node.js。Java JAR、H5 和管理后台都在本地或 CI 构建，服务器只运行构建产物。

本地构建机需要：

- JDK 17。
- Maven 3.8 或更高版本。
- Node.js 20 LTS 和 npm。

检查：

```bash
java -version
python3 --version
mysql --version
redis-server --version
nginx -v
```

### 3.2 创建应用用户和目录

```bash
sudo useradd --system --create-home --shell /usr/sbin/nologin nutrition || true
sudo mkdir -p /opt/nutrition-web /etc/nutrition /var/log/nutrition /var/log/nutrition/heapdumps
sudo chown -R nutrition:nutrition /opt/nutrition-web /var/log/nutrition
sudo chmod 750 /opt/nutrition-web /var/log/nutrition
```

建议目录结构：

```text
/opt/nutrition-web/
├── app/                 # 本地构建后上传的 nutrition-server.jar
├── src/
│   └── nutrition-AI/    # Python AI 源码；虚拟环境在服务器创建
├── www/                 # 本地构建后上传的 H5 静态文件
└── admin/               # 本地构建后上传的管理端静态文件
```

---

## 4. 本地构建和上传部署产物

生产服务器不拉取 Java 源码，也不安装 Maven、Node.js。所有可构建组件先在本地或 CI 完成构建，再上传运行产物。

### 4.1 本地构建 Java JAR

在本地项目根目录执行：

```bash
cd nutrition-server
mvn clean package
```

`mvn clean package` 默认会执行测试。构建成功后产物为：

```text
nutrition-server/target/nutrition-server-1.0.0.jar
```

这个 JAR 是 Spring Boot 可执行包，包含 Java 代码、Mapper XML、资源文件和第三方依赖，并内置 Web 容器。生产服务器不需要单独安装 Tomcat。

如果 CI 已独立执行测试，本地快速构建也可以使用：

```bash
mvn clean package -DskipTests
```

### 4.2 本地构建前端

H5：

```bash
cd nutrition-miniapp
npm ci
npm run build:h5
```

产物目录：

```text
nutrition-miniapp/dist/build/h5
```

管理后台（回到项目根目录后）：

```bash
cd nutrition-admin
npm ci
npm run build
```

产物目录：

```text
nutrition-admin/dist
```

管理后台是可选站点；如果不部署管理后台，可以跳过管理后台的构建、上传和 Nginx 配置。

构建前确认 H5 的 API 地址仍为同域相对路径，即第 1.1 节所述的 `BASE_URL = ''`。

### 4.3 上传产物

先在服务器创建临时上传目录：

```bash
ssh nutrition@SERVER_IP \
  "mkdir -p /tmp/nutrition-deploy/app /tmp/nutrition-deploy/www /tmp/nutrition-deploy/admin /opt/nutrition-web/app /opt/nutrition-web/www /opt/nutrition-web/admin /opt/nutrition-web/src/nutrition-AI"
```

上传 Java JAR：

```bash
scp nutrition-server/target/nutrition-server-1.0.0.jar \
  nutrition@SERVER_IP:/tmp/nutrition-deploy/app/
```

上传 H5 和管理后台静态产物：

```bash
scp -r nutrition-miniapp/dist/build/h5/. \
  nutrition@SERVER_IP:/tmp/nutrition-deploy/www/

scp -r nutrition-admin/dist/. \
  nutrition@SERVER_IP:/tmp/nutrition-deploy/admin/
```

上传 Python 源码。Windows 本地建议使用 Git Bash、WSL 或具备 `rsync` 的环境：

```bash
rsync -av --delete \
  --exclude '.env' \
  --exclude '.venv/' \
  --exclude 'logs/' \
  --exclude 'tmp/' \
  --exclude '__pycache__/' \
  --exclude '*.pyc' \
  nutrition-AI/ \
  nutrition@SERVER_IP:/opt/nutrition-web/src/nutrition-AI/
```

不要上传：

- `nutrition-AI/.env`
- `nutrition-AI/.venv`
- `nutrition-AI/logs`
- `nutrition-AI/tmp`
- `node_modules`
- Java 源码、`target` 中间文件
- 包含生产密钥的本地配置文件

### 4.4 安装产物并调整权限

在服务器执行：

```bash
sudo install -o nutrition -g nutrition -m 640 \
  /tmp/nutrition-deploy/app/nutrition-server-1.0.0.jar \
  /opt/nutrition-web/app/nutrition-server.jar

sudo cp -a /tmp/nutrition-deploy/www/. /opt/nutrition-web/www/
sudo cp -a /tmp/nutrition-deploy/admin/. /opt/nutrition-web/admin/

sudo chown -R www-data:www-data /opt/nutrition-web/www /opt/nutrition-web/admin
sudo chown -R nutrition:nutrition /opt/nutrition-web/src/nutrition-AI
sudo find /opt/nutrition-web/src/nutrition-AI -type d -exec chmod 750 {} \;
sudo find /opt/nutrition-web/src/nutrition-AI -type f -exec chmod 640 {} \;
```

检查最终目录：

```bash
ls -lh /opt/nutrition-web/app/nutrition-server.jar
ls -ld /opt/nutrition-web/www /opt/nutrition-web/admin
ls -ld /opt/nutrition-web/src/nutrition-AI
```

---

## 5. MySQL 初始化

### 5.1 创建数据库和应用账号

不要使用 `root` 运行后端。下面密码请替换成高强度随机值：

```bash
sudo mysql
```

在 MySQL 中执行：

```sql
CREATE DATABASE IF NOT EXISTS nutrition_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'nutrition_app'@'127.0.0.1'
  IDENTIFIED BY '替换成MySQL应用密码';

ALTER USER 'nutrition_app'@'127.0.0.1'
  IDENTIFIED BY '替换成MySQL应用密码';

GRANT ALL PRIVILEGES ON nutrition_db.* TO 'nutrition_app'@'127.0.0.1';
FLUSH PRIVILEGES;
EXIT;
```

### 5.2 导入表结构

项目 SQL 文件开头有 4 行非 SQL 文本（`数据库表设计`、空行、`创建数据库`、`create database nutrition_db;`），且没有 `USE nutrition_db`。导入前先把这 4 行删掉，再导入已创建的数据库：

```bash
# 从已上传的 JAR 中提取建表 SQL
sudo unzip -p /opt/nutrition-web/app/nutrition-server.jar \
  BOOT-INF/classes/db/nutrition_db.sql \
  | sed -e '1,/^create database nutrition_db;/d' \
  | mysql -h 127.0.0.1 -u nutrition_app -p nutrition_db
```

校验 `sys_user` 已包含 `phone` 列、且没有 `password_encrypted` 列（应为空结果）：

```bash
mysql -h 127.0.0.1 -u nutrition_app -p nutrition_db -e "SHOW COLUMNS FROM sys_user;"
```

> **不要**再执行任何 `ADD COLUMN password_encrypted` 的兼容 DDL。当前代码只写 `password_hash`，建库 SQL 也只含 `password_hash` 与 `phone`。

如果是从**旧库升级**（已有 `password_encrypted` 列的历史库），执行一次清理即可：

```sql
ALTER TABLE sys_user DROP COLUMN password_encrypted;
```

### 5.3 导入食物营养 CSV

当前数据库 SQL 只建表，不导入 CSV。必须导入，否则食物搜索、分类和营养缓存为空。

CSV：

```text
JAR 内路径：BOOT-INF/classes/foodData/food_nutrition.csv
```

MySQL 8 默认可能禁止客户端 `LOAD DATA LOCAL`。先确认：

```sql
SHOW VARIABLES LIKE 'local_infile';
```

如果值为 `OFF`，先在 MySQL 配置中启用并重启 MySQL；也可以使用支持导入的 MySQL 客户端/DataGrip 从 CSV 导入。

命令行导入示例：

```bash
sudo unzip -p /opt/nutrition-web/app/nutrition-server.jar \
  BOOT-INF/classes/foodData/food_nutrition.csv \
  > /tmp/food_nutrition.csv

mysql --local-infile=1 -h 127.0.0.1 -u nutrition_app -p nutrition_db <<'SQL'
LOAD DATA LOCAL INFILE '/tmp/food_nutrition.csv'
INTO TABLE food_nutrition
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(food_name, food_category, edible_part, calorie, protein, fat, carbohydrate);
SQL

rm -f /tmp/food_nutrition.csv
```

校验：

```bash
mysql -h 127.0.0.1 -u nutrition_app -p nutrition_db -e \
  "SELECT COUNT(*) AS food_count FROM food_nutrition;"
```

### 5.4 创建管理员账号

管理员密码必须是 BCrypt。可用 `htpasswd` 生成：

```bash
ADMIN_PASSWORD='替换成管理员强密码'
ADMIN_HASH="$(htpasswd -bnBC 12 '' "$ADMIN_PASSWORD" | tr -d ':\n')"

mysql -h 127.0.0.1 -u nutrition_app -p nutrition_db -e "
INSERT INTO sys_admin (username, password, nickname)
VALUES ('admin', '${ADMIN_HASH}', '系统管理员')
ON DUPLICATE KEY UPDATE password = VALUES(password), nickname = VALUES(nickname);
"
```

不要使用 `123456` 作为管理员密码。

---

## 6. Redis 配置

编辑 `/etc/redis/redis.conf`，确保：

```conf
bind 127.0.0.1 ::1
protected-mode yes
requirepass 替换成Redis强密码
```

重启：

```bash
sudo systemctl enable --now redis-server
sudo systemctl restart redis-server
redis-cli -h 127.0.0.1 -a '替换成Redis强密码' ping
```

Redis 不要开放公网端口。

---

## 7. 服务器安装运行依赖

### 7.1 校验 Java 部署产物

确认 JAR 已由本地或 CI 上传，并检查文件权限和 Spring Boot 启动入口：

```bash
ls -lh /opt/nutrition-web/app/nutrition-server.jar
sudo -u nutrition test -r /opt/nutrition-web/app/nutrition-server.jar
unzip -p /opt/nutrition-web/app/nutrition-server.jar META-INF/MANIFEST.MF \
  | grep -E 'Main-Class|Start-Class'
```

服务器不需要 Java 源码，也不需要执行 `mvn package`。只要服务器 JDK 版本不低于本地编译时使用的版本，通常建议两边都使用 JDK 17。

### 7.2 Python AI 服务

Python 虚拟环境必须在目标 Linux 服务器创建，不能上传 Windows 本地 `.venv`：

```bash
cd /opt/nutrition-web/src/nutrition-AI

python3 -m venv .venv
./.venv/bin/python -m pip install --upgrade pip
# 生产环境使用锁文件，保证与开发环境版本一致、可复现
./.venv/bin/pip install -r requirements.lock.txt
```

Python 版本建议 3.11 或 3.12。

> `requirements.txt` 只声明宽松范围（`>=`），`requirements.lock.txt` 锁定精确版本（143 个包）。
> 生产部署请用 `requirements.lock.txt`；升级依赖后在虚拟环境中重新执行
> `pip freeze > requirements.lock.txt` 再回归测试。

### 7.3 校验前端静态产物

H5 和管理后台已经由第 4 节上传到 Nginx 根目录，服务器不再执行 `npm ci` 或 `npm run build`：

```bash
test -f /opt/nutrition-web/www/index.html
test -f /opt/nutrition-web/admin/index.html
sudo -u www-data test -r /opt/nutrition-web/www/index.html
sudo -u www-data test -r /opt/nutrition-web/admin/index.html
```

未部署管理后台时，跳过最后两条 `admin` 检查。

---

## 8. 生产环境变量

### 8.1 `/etc/nutrition/server.env`

```dotenv
SERVER_ADDRESS=127.0.0.1
SERVER_PORT=8088

SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/nutrition_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=nutrition_app
DB_PASSWORD=替换成MySQL应用密码

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=替换成Redis强密码

JWT_SECRET=替换成openssl-rand-base64-64生成值
AI_AES_KEY=替换成openssl-rand-base64-48生成值

OSS_ACCESS_KEY_ID=替换成阿里云OSS AccessKeyId
OSS_ACCESS_KEY_SECRET=替换成阿里云OSS AccessKeySecret

WX_MINI_APPID=替换成微信AppID
WX_MINI_APPSECRET=替换成微信AppSecret
WX_AUDIT_VERSION=1

FASTAPI_BASE_URL=http://127.0.0.1:8004
FASTAPI_SECRET_KEY=替换成Java与Python共用的长随机串

# 日志级别（生产用 INFO；排障时临时改 DEBUG，注意 DEBUG 可能输出更多细节）
LOGGING_LEVEL_COM_NUTRITION=INFO
# MyBatis 默认走 SLF4J，只有把 com.nutrition.mapper 调到 DEBUG 才会输出 SQL
MYBATIS_LOG_IMPL=org.apache.ibatis.logging.slf4j.Slf4jImpl

# Swagger/OpenAPI 开关，生产保持 false（默认即 false）
SWAGGER_ENABLED=false

# 跨域白名单，逗号分隔；同域部署留空即可（默认留空）
# 仅当管理后台使用独立子域名时才需要填，例如：
# CORS_ALLOWED_ORIGINS=https://www.example.com,https://admin.example.com
CORS_ALLOWED_ORIGINS=

# OSS 私有 Bucket 模式；开启前必须先到阿里云把 Bucket 权限改为「私有」
OSS_PRIVATE_BUCKET=false
OSS_SIGNED_URL_EXPIRE_SECONDS=3600
```

说明：

- `SERVER_ADDRESS=127.0.0.1` 让 Java 只接受本机 Nginx 转发，不直接暴露 8088。
- `SPRING_DATASOURCE_URL` 可覆盖 `application.yml` 中写死的本地地址。
- **`JWT_SECRET`、`AI_AES_KEY` 为必填项**：代码已移除默认密钥，缺失时后端启动会直接失败（这是预期行为，避免使用可预测密钥）。
- `JWT_SECRET` 建议 ≥ 64 字节随机串（`openssl rand -base64 64`）。
- `AI_AES_KEY` 对应 `AesUtil` 的 `ai.aes.key`，用于加解密 `ai_config.api_key`。若数据库已有用旧密钥加密的 AI 配置，换密钥前需先迁移，否则历史密文解不开。
- `FASTAPI_SECRET_KEY` 必须与 Python 的 `API_SECRET_KEY` 完全一致。
- 如果不使用微信内容安全审核，可以先保持为空；但图片审核相关功能可能失败。
- `MYBATIS_LOG_IMPL` 默认使用 `org.apache.ibatis.logging.slf4j.Slf4jImpl`，SQL 日志受日志级别控制；生产环境不要改成 `org.apache.ibatis.logging.stdout.StdOutImpl`。

### 8.2 `/etc/nutrition/ai.env`

```dotenv
SERVER_HOST=127.0.0.1
SERVER_PORT=8004

API_SECRET_KEY=替换成Java与Python共用的长随机串

REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=替换成Redis强密码
REDIS_DB=0

VECTOR_ENDPOINT=替换成DashVector Endpoint
VECTOR_API_KEY=替换成DashVector API Key
VECTOR_COLLECTION_NAME=food_nutrition_knowledge

DASHSCOPE_API_KEY=替换成DashScope API Key
EMBEDDING_MODEL=text-embedding-v4

JAVA_BASE_URL=http://127.0.0.1:8088/api
JAVA_RAG_CALLBACK_PATH=/rag/knowledge/callback

LOG_LEVEL=INFO
```

### 8.3 文件权限

```bash
sudo chown root:nutrition /etc/nutrition/server.env /etc/nutrition/ai.env
sudo chmod 640 /etc/nutrition/server.env /etc/nutrition/ai.env
```

不要把这两个文件放进 Git、前端产物或公开下载目录。

---

## 9. systemd 服务

### 9.1 Java 后端

创建 `/etc/systemd/system/nutrition-server.service`：

```ini
[Unit]
Description=Nutrition Java API
After=network.target mysql.service redis-server.service
Wants=mysql.service redis-server.service

[Service]
Type=simple
User=nutrition
Group=nutrition
WorkingDirectory=/opt/nutrition-web/app
EnvironmentFile=/etc/nutrition/server.env
ExecStart=/usr/bin/java \
  -Xms256m -Xmx768m \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/nutrition/heapdumps \
  -XX:ErrorFile=/var/log/nutrition/hs_err_pid%p.log \
  -jar /opt/nutrition-web/app/nutrition-server.jar
Restart=always
RestartSec=5
SuccessExitStatus=143
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
```

### 9.2 Python AI 服务

创建 `/etc/systemd/system/nutrition-ai.service`：

```ini
[Unit]
Description=Nutrition Python AI
After=network.target redis-server.service
Wants=redis-server.service

[Service]
Type=simple
User=nutrition
Group=nutrition
WorkingDirectory=/opt/nutrition-web/src/nutrition-AI
EnvironmentFile=/etc/nutrition/ai.env
Environment=PYTHONUNBUFFERED=1
ExecStart=/opt/nutrition-web/src/nutrition-AI/.venv/bin/uvicorn main:app --host 127.0.0.1 --port 8004 --workers 1
Restart=always
RestartSec=5
SuccessExitStatus=143
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
```

Python 服务建议单 worker，因为当前应用有会话 TTL 后台扫描逻辑，多 worker 可能重复执行。

启动：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now nutrition-ai nutrition-server
sudo systemctl status nutrition-ai --no-pager
sudo systemctl status nutrition-server --no-pager
```

查看日志：

```bash
journalctl -u nutrition-ai -f
journalctl -u nutrition-server -f
```

---

## 10. Nginx 和 HTTPS

### 10.1 用户 H5 网站

创建 `/etc/nginx/sites-available/nutrition-web`：

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name www.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name www.example.com;

    # 使用你手动上传的证书；文件名和目录按实际情况替换
    ssl_certificate /etc/nginx/ssl/www.example.com/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/www.example.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;

    client_max_body_size 12m;

    root /opt/nutrition-web/www;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8088;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # AI SSE / 长请求
        proxy_buffering off;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|svg|webp|ico|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public, max-age=604800, immutable";
        try_files $uri =404;
    }

    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options SAMEORIGIN always;
    add_header Referrer-Policy strict-origin-when-cross-origin always;
}
```

启用：

```bash
sudo ln -sfn /etc/nginx/sites-available/nutrition-web /etc/nginx/sites-enabled/nutrition-web
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

### 10.2 管理后台（可选）

创建 `/etc/nginx/sites-available/nutrition-admin`：

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name admin.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name admin.example.com;

    # 使用你手动上传的证书；文件名和目录按实际情况替换
    ssl_certificate /etc/nginx/ssl/admin.example.com/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/admin.example.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;

    client_max_body_size 12m;

    root /opt/nutrition-web/admin;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8088;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }

    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options SAMEORIGIN always;
}
```

启用：

```bash
sudo ln -sfn /etc/nginx/sites-available/nutrition-admin /etc/nginx/sites-enabled/nutrition-admin
sudo nginx -t
sudo systemctl reload nginx
```

### 10.3 手动上传证书和到期更新

本文默认不使用 Certbot。证书由证书签发方提供后，手动上传到服务器。下面以 `www.example.com` 为例：

```bash
sudo mkdir -p /etc/nginx/ssl/www.example.com

# 将证书文件上传到临时目录后复制到这里；以下文件名必须与 Nginx 配置一致
sudo cp /path/to/fullchain.pem /etc/nginx/ssl/www.example.com/fullchain.pem
sudo cp /path/to/privkey.pem   /etc/nginx/ssl/www.example.com/privkey.pem

sudo chown -R root:root /etc/nginx/ssl
sudo find /etc/nginx/ssl -type d -exec chmod 700 {} \;
sudo find /etc/nginx/ssl -type f -name '*.pem' -exec chmod 600 {} \;
```

检查证书和私钥匹配、有效期及域名：

```bash
sudo openssl x509 -in /etc/nginx/ssl/www.example.com/fullchain.pem -noout -subject -issuer -dates
sudo openssl x509 -in /etc/nginx/ssl/www.example.com/fullchain.pem -noout -ext subjectAltName

# 两条命令输出的 SHA256 必须一致
sudo openssl x509 -in /etc/nginx/ssl/www.example.com/fullchain.pem -pubkey -noout \
  | openssl pkey -pubin -outform der | sha256sum
sudo openssl pkey -in /etc/nginx/ssl/www.example.com/privkey.pem -pubout -outform der | sha256sum
```

证书更新后先校验再平滑重载：

```bash
sudo nginx -t
sudo systemctl reload nginx
curl -Iv https://www.example.com
```

手动证书没有自动续期能力，必须设置到期检查或监控告警。至少每天检查一次是否进入 30 天续期窗口：

```bash
sudo openssl x509 -checkend 2592000 -noout \
  -in /etc/nginx/ssl/www.example.com/fullchain.pem \
  || logger -t nutrition-cert "www.example.com certificate expires within 30 days"
```

生产环境不要长期只提供 HTTP。微信、浏览器安全和登录令牌都不应在明文 HTTP 上传输。

---

## 11. 防火墙和端口检查

只开放：

- SSH
- 80
- 443

不要开放：

- 3306 MySQL
- 6379 Redis
- 8004 Python AI
- 8088 Java API

UFW 示例：

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw deny 3306/tcp
sudo ufw deny 6379/tcp
sudo ufw deny 8004/tcp
sudo ufw deny 8088/tcp
sudo ufw enable
sudo ufw status verbose
```

检查监听：

```bash
ss -lntp
```

理想结果：

```text
127.0.0.1:3306
127.0.0.1:6379
127.0.0.1:8004
127.0.0.1:8088
0.0.0.0:80
0.0.0.0:443
```

---

## 12. 首次启动与业务初始化

### 12.1 服务健康检查

```bash
systemctl is-active mysql
systemctl is-active redis-server
systemctl is-active nutrition-ai
systemctl is-active nutrition-server
systemctl is-active nginx
```

后端接口：

```bash
curl -i http://127.0.0.1:8088/api/food/search?keyword=鸡蛋
```

Python：

```bash
curl -i http://127.0.0.1:8004/health
```

公网：

```bash
curl -I https://www.example.com
curl -I https://www.example.com/api/food/search?keyword=鸡蛋
```

### 12.2 初始化 AI 模型配置

当前系统没有默认启用的大模型配置。至少需要在管理后台新增并启用一条 `ai_config`，否则 AI 对话不可用。

建议字段：

```text
model_name: qwen-plus
model_type: dashscope
api_url: https://dashscope.aliyuncs.com/compatible-mode/v1
api_key: 你的 DashScope API Key
temperature: 0.7
max_tokens: 800
is_enabled: 1
```

保存后重启 Java 服务，让 `AiConfigCacheRunner` 把启用配置写入 Redis：

```bash
sudo systemctl restart nutrition-server
sudo systemctl status nutrition-server --no-pager
```

### 12.3 验证网站功能

至少手工验证：

- 打开 `https://www.example.com`
- 注册新用户：验证码正常显示，手机号填非法格式会被前端和后端同时拒绝
- 用同一手机号注册第二个账号应成功（手机号不唯一），用已存在用户名注册应失败（用户名唯一）
- 登录成功
- 重置密码：输入正确用户名 + 错误手机号应提示「手机号错误」；输入正确用户名 + 正确手机号可改密并用新密码登录
- 食物搜索能返回数据
- 添加饮食记录
- 图片上传与展示
- AI 对话与健康分析
- 刷新页面后登录状态正常
- 浏览器开发者工具中没有 `localhost:8088` 或跨域错误
- 用普通用户 Token 调用 `GET /api/ai/config/list` 应返回 403

---

## 13. 备份、日志、故障排查和更新

### 13.1 备份策略

建议每天 03:30 自动备份 MySQL、Redis 和 `/etc/nutrition` 配置，默认保留 7 天，并将备份同步到另一台机器或对象存储。只放在同一块系统盘上的备份不算有效备份。

#### 13.1.1 创建备份账号配置

创建仅 root 可读的 MySQL 客户端配置：

```bash
sudo tee /etc/nutrition/mysql-backup.cnf >/dev/null <<'EOF'
[client]
host=127.0.0.1
protocol=tcp
user=nutrition_app
password=替换成MySQL应用密码
EOF

sudo chown root:root /etc/nutrition/mysql-backup.cnf
sudo chmod 600 /etc/nutrition/mysql-backup.cnf
```

创建备份变量文件：

```bash
sudo tee /etc/nutrition/backup.env >/dev/null <<'EOF'
BACKUP_DIR=/var/backups/nutrition
RETENTION_DAYS=7
MYSQL_CONFIG=/etc/nutrition/mysql-backup.cnf
REDIS_PASSWORD='替换成Redis强密码'
EOF

sudo chown root:root /etc/nutrition/backup.env
sudo chmod 600 /etc/nutrition/backup.env
```

#### 13.1.2 备份脚本和自动清理

创建 `/usr/local/sbin/nutrition-backup.sh`：

```bash
#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

source /etc/nutrition/backup.env

BACKUP_DIR="${BACKUP_DIR:-/var/backups/nutrition}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
MYSQL_CONFIG="${MYSQL_CONFIG:-/etc/nutrition/mysql-backup.cnf}"
STAMP="$(date +%F_%H%M%S)"

install -d -m 750 "$BACKUP_DIR"

cleanup() {
    rm -f "$BACKUP_DIR"/*.tmp
}
trap cleanup EXIT

# MySQL 一致性逻辑备份
mysqldump --defaults-extra-file="$MYSQL_CONFIG" \
  --single-transaction --routines --triggers --events \
  nutrition_db \
  | gzip -9 > "$BACKUP_DIR/mysql_${STAMP}.sql.gz.tmp"
mv "$BACKUP_DIR/mysql_${STAMP}.sql.gz.tmp" \
   "$BACKUP_DIR/mysql_${STAMP}.sql.gz"

# Redis 一致性 RDB 快照；Redis 必须开启 AOF 用于进程崩溃恢复
redis-cli -h 127.0.0.1 -a "$REDIS_PASSWORD" --no-auth-warning \
  --rdb "$BACKUP_DIR/redis_${STAMP}.rdb.tmp"
gzip -9 "$BACKUP_DIR/redis_${STAMP}.rdb.tmp"
mv "$BACKUP_DIR/redis_${STAMP}.rdb.tmp.gz" \
   "$BACKUP_DIR/redis_${STAMP}.rdb.gz"

# 环境变量、Redis 客户端配置和备份配置
tar -C / -czf "$BACKUP_DIR/config_${STAMP}.tar.gz.tmp" \
  etc/nutrition
mv "$BACKUP_DIR/config_${STAMP}.tar.gz.tmp" \
   "$BACKUP_DIR/config_${STAMP}.tar.gz"

# 清理超过保留期的数据库、Redis 和配置备份
find "$BACKUP_DIR" -maxdepth 1 -type f \
  \( -name 'mysql_*.sql.gz' -o -name 'redis_*.rdb.gz' -o -name 'config_*.tar.gz' \) \
  -mtime +"$RETENTION_DAYS" -delete

# 清理超过 7 天的 Java 堆转储，避免再次占满磁盘
find /var/log/nutrition/heapdumps -maxdepth 1 -type f \
  -name '*.hprof' -mtime +7 -delete
```

设置权限并手工执行一次：

```bash
sudo chown root:root /usr/local/sbin/nutrition-backup.sh
sudo chmod 700 /usr/local/sbin/nutrition-backup.sh
sudo /usr/local/sbin/nutrition-backup.sh
sudo ls -lh /var/backups/nutrition
```

`config_*.tar.gz` 包含环境变量和备份密码，属于敏感文件。同步到异地或对象存储前必须先加密，并使用仅备份账号可读的 Bucket/路径。OSS Bucket 另外开启版本控制或生命周期保护；可回滚的 jar 和前端静态文件由 CI 或发布流程单独归档。

#### 13.1.3 定时执行

创建 `/etc/systemd/system/nutrition-backup.service`：

```ini
[Unit]
Description=Nutrition MySQL, Redis and config backup
After=mysql.service redis-server.service
Wants=mysql.service redis-server.service

[Service]
Type=oneshot
ExecStart=/usr/local/sbin/nutrition-backup.sh
```

创建 `/etc/systemd/system/nutrition-backup.timer`：

```ini
[Unit]
Description=Run Nutrition backup daily

[Timer]
OnCalendar=*-*-* 03:30:00
Persistent=true
RandomizedDelaySec=15m

[Install]
WantedBy=timers.target
```

启用并检查：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now nutrition-backup.timer
systemctl list-timers nutrition-backup.timer --no-pager
journalctl -u nutrition-backup.service -n 100 --no-pager
```

#### 13.1.4 Redis 持久化配置

Redis 保存了会话上下文、点赞/评论计数和 JWT 黑名单，不是可以完全忽略的纯缓存。编辑 `/etc/redis/redis.conf`，在现有配置中确保：

```conf
appendonly yes
appendfsync everysec
appendfilename "appendonly.aof"
appenddirname "appendonlydir"
```

重启并检查：

```bash
sudo systemctl restart redis-server
redis-cli -h 127.0.0.1 -a '替换成Redis强密码' --no-auth-warning CONFIG GET appendonly
redis-cli -h 127.0.0.1 -a '替换成Redis强密码' --no-auth-warning CONFIG GET appendfsync
```

`appendfsync everysec` 在性能和数据安全之间折中，极端故障时最多可能丢失约 1 秒写入；每日 RDB 备份用于恢复更早的数据。点赞/评论计数每小时回写 MySQL，因此 Redis 恢复后可能出现最多约 1 小时的计数回退，恢复后应观察并校正。

#### 13.1.5 恢复验证

MySQL 恢复前先停止写入，并在临时数据库验证备份可导入：

```bash
gunzip -c /var/backups/nutrition/mysql_YYYY-MM-DD_HHMMSS.sql.gz \
  | mysql --defaults-extra-file=/etc/nutrition/mysql-backup.cnf nutrition_db
```

Redis 使用 RDB 备份恢复时，先停止 Redis，保留现场，再将备份放回数据目录：

```bash
sudo systemctl stop redis-server
sudo cp -a /var/lib/redis/dump.rdb /var/lib/redis/dump.rdb.before-restore
gunzip -c /var/backups/nutrition/redis_YYYY-MM-DD_HHMMSS.rdb.gz \
  | sudo tee /var/lib/redis/dump.rdb >/dev/null
sudo chown redis:redis /var/lib/redis/dump.rdb
```

因为 Redis 7 开启 AOF 后会优先从 AOF 恢复，使用 RDB 恢复前必须临时将 `/var/lib/redis/appendonlydir` 移走，并在恢复验证后重新启用 AOF：

```bash
sudo mv /var/lib/redis/appendonlydir /var/lib/redis/appendonlydir.before-restore

# 临时关闭 AOF，让 Redis 本次从 RDB 恢复
sudo sed -E -i 's/^[[:space:]]*appendonly[[:space:]]+yes/appendonly no/' /etc/redis/redis.conf
sudo systemctl start redis-server
redis-cli -h 127.0.0.1 -a '替换成Redis强密码' --no-auth-warning PING
redis-cli -h 127.0.0.1 -a '替换成Redis强密码' --no-auth-warning DBSIZE

# 数据确认无误后重新启用 AOF
redis-cli -h 127.0.0.1 -a '替换成Redis强密码' --no-auth-warning CONFIG SET appendonly yes
redis-cli -h 127.0.0.1 -a '替换成Redis强密码' --no-auth-warning CONFIG REWRITE
sudo systemctl restart redis-server
```

恢复流程必须至少在测试环境演练一次。正式恢复前还应确认备份时间、Redis 与 MySQL 的计数差异，以及 OSS 文件和数据库记录是否匹配。

### 13.2 日志容量和系统调优

#### 13.2.1 限制 journald 容量

Java 和 Python 的 systemd 标准输出都会进入 journald。创建 `/etc/systemd/journald.conf.d/nutrition.conf`：

```ini
[Journal]
Storage=persistent
Compress=yes
SystemMaxUse=512M
SystemKeepFree=1G
SystemMaxFileSize=64M
MaxRetentionSec=30day
RuntimeMaxUse=128M
```

应用配置并清理超限日志：

```bash
sudo systemctl restart systemd-journald
journalctl --disk-usage
sudo journalctl --vacuum-size=512M
sudo journalctl --vacuum-time=30d
journalctl --disk-usage
```

不要为 journald 配置 `logrotate`。journald 使用自己的容量和保留策略；`logrotate` 只适用于普通日志文件。

#### 13.2.2 Nginx 日志轮转

Ubuntu 的 Nginx 包通常已提供 `/etc/logrotate.d/nginx`。先检查：

```bash
sudo logrotate -d /etc/logrotate.d/nginx
```

如果该文件不存在，再创建 `/etc/logrotate.d/nginx`：

```conf
/var/log/nginx/*.log {
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    create 0640 www-data adm
    sharedscripts
    postrotate
        [ -f /run/nginx.pid ] && kill -USR1 $(cat /run/nginx.pid)
    endscript
}
```

#### 13.2.3 应用日志

- Java：统一输出到 stdout，由 journald 收集；MyBatis 使用 SLF4J，禁止生产环境配置 `org.apache.ibatis.logging.stdout.StdOutImpl`。只有临时把 `com.nutrition.mapper` 调成 `DEBUG` 时才输出 SQL。
- Python：Loguru 同时写 stderr 和 `nutrition-AI/logs/app_YYYY-MM-DD.log`，文件按 10 MB 轮转并保留 30 天。
- Nginx：使用系统 `logrotate`，不要直接删除正在写入的日志文件。

查看日志：

```bash
journalctl -u nutrition-server -n 200 --no-pager
journalctl -u nutrition-ai -n 200 --no-pager
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
sudo tail -f /opt/nutrition-web/src/nutrition-AI/logs/app_$(date +%F).log
```

### 13.3 故障排查

#### 13.3.1 服务启动失败快速定位

先获取状态和本次启动日志：

```bash
sudo systemctl status nutrition-server --no-pager
sudo systemctl status nutrition-ai --no-pager
sudo journalctl -u nutrition-server -b --no-pager -n 300
sudo journalctl -u nutrition-ai -b --no-pager -n 300
```

检查 systemd 文件、环境文件和监听端口：

```bash
sudo systemd-analyze verify /etc/systemd/system/nutrition-server.service
sudo systemd-analyze verify /etc/systemd/system/nutrition-ai.service
sudo stat -c '%U:%G %a %n' /etc/nutrition/server.env /etc/nutrition/ai.env
sudo systemctl show nutrition-server -p EnvironmentFiles
sudo systemctl show nutrition-ai -p EnvironmentFiles
ss -lntp | grep -E ':(3306|6379|8004|8088|80|443)\b'
```

按报错关键词分流：

| 日志关键词 | 优先检查 |
|---|---|
| `Could not resolve placeholder`、`Failed to bind properties` | `EnvironmentFile` 路径、权限、变量名、空值或格式 |
| `Access denied`、`Communications link failure` | MySQL 服务、账号、密码、`127.0.0.1:3306` 连通性 |
| `Unable to connect to Redis`、`RedisConnectionFailureException` | Redis 服务、密码、`127.0.0.1:6379` 连通性 |
| `Address already in use` | 端口是否被其他进程占用 |
| `Failed to load ApplicationContext` | 继续向下查看第一个 `Caused by`，不要只看最后一行 |

依赖连通性检查：

```bash
mysqladmin --defaults-extra-file=/etc/nutrition/mysql-backup.cnf ping
redis-cli -h 127.0.0.1 -a '替换成Redis强密码' --no-auth-warning PING
curl -i http://127.0.0.1:8004/health
curl -i http://127.0.0.1:8088/api/food/search?keyword=鸡蛋
```

#### 13.3.2 Java OOM 和线程排查

服务已配置 `-XX:+HeapDumpOnOutOfMemoryError`，OOM 时堆转储写入 `/var/log/nutrition/heapdumps`。先确认是否真的发生 OOM：

```bash
journalctl -u nutrition-server --since '24 hours ago' --no-pager \
  | grep -Ei 'OutOfMemoryError|Java heap space|GC overhead limit|Killed process'
sudo ls -lh /var/log/nutrition/heapdumps
```

获取 Java PID 并查看内存、GC 和线程：

```bash
PID="$(systemctl show -p MainPID --value nutrition-server)"
echo "$PID"

sudo jcmd "$PID" VM.version
sudo jcmd "$PID" GC.heap_info
sudo jcmd "$PID" Thread.print \
  > "/tmp/nutrition-threads-$(date +%F_%H%M%S).txt"

# 查看存活对象占用排名，可能短暂停顿，低峰期执行
sudo jmap -histo:live "$PID" | head -n 50
```

如果仍需人工堆转储：

```bash
sudo jmap -dump:live,format=b,file=/var/log/nutrition/heapdumps/java-$(date +%F_%H%M%S).hprof "$PID"
```

堆转储通常很大，分析后及时删除。排查方向包括：`-Xmx768m` 是否过小、接口是否一次加载过多数据、缓存是否无界、线程是否阻塞，以及 OOM 前后请求量是否突增。

#### 13.3.3 Python 内存和阻塞排查

先确认进程 RSS 是否持续增长，以及是否存在多个 worker：

```bash
PID="$(systemctl show -p MainPID --value nutrition-ai)"
ps -o pid,ppid,rss,vsz,%mem,etime,cmd -p "$PID"
pmap -x "$PID" | tail -n 1
```

如已安装 `py-spy`，可在低峰期查看 Python 栈：

```bash
sudo py-spy top --pid "$PID"
sudo py-spy dump --pid "$PID" > "/tmp/nutrition-ai-stack-$(date +%F_%H%M%S).txt"
```

没有 `py-spy` 时，不要在生产虚拟环境中临时升级大量依赖。可以在维护窗口安装或在测试环境复现。重点检查：

- RSS 是否随请求持续上涨且不回落。
- `uvicorn` 是否误启用了多个 worker。
- 会话 TTL 扫描是否持续报错。
- 是否加载了大模型对象、向量库对象或未释放的临时数据。
- `logs/app_YYYY-MM-DD.log` 是否有重复重试、异常循环或大量请求内容。

#### 13.3.4 快速恢复

- 环境变量问题：修正 `/etc/nutrition/*.env` 后执行 `sudo systemctl restart <服务名>`。
- MySQL 不可用：先恢复 MySQL，再启动 `nutrition-server`。
- Redis 不可用：先恢复 Redis；Redis 数据可能影响会话、计数和 Token 黑名单。
- 新版本启动失败：先回滚 jar 或前端静态文件，再保留日志和堆转储分析。

### 13.4 更新发布

在本地或 CI 构建新版本：

```bash
cd nutrition-server
mvn clean package

cd ../nutrition-miniapp
npm ci
npm run build:h5

cd ../nutrition-admin
npm ci
npm run build
```

按第 4.3 节上传 JAR、H5、管理后台和 Python 源码，然后在服务器执行：

```bash
# 更新已有部署时，先保留当前可回滚版本
BACKUP_STAMP="$(date +%F_%H%M%S)"
sudo cp -a /opt/nutrition-web/app/nutrition-server.jar \
  "/opt/nutrition-web/app/nutrition-server.jar.bak.${BACKUP_STAMP}"
sudo cp -a /opt/nutrition-web/www \
  "/opt/nutrition-web/www.bak.${BACKUP_STAMP}"
sudo cp -a /opt/nutrition-web/admin \
  "/opt/nutrition-web/admin.bak.${BACKUP_STAMP}"

# 安装新产物
sudo install -o nutrition -g nutrition -m 640 \
  /tmp/nutrition-deploy/app/nutrition-server-1.0.0.jar \
  /opt/nutrition-web/app/nutrition-server.jar
sudo cp -a /tmp/nutrition-deploy/www/. /opt/nutrition-web/www/
sudo cp -a /tmp/nutrition-deploy/admin/. /opt/nutrition-web/admin/
sudo chown -R www-data:www-data /opt/nutrition-web/www /opt/nutrition-web/admin

# 如果 Python 依赖有变化，重新安装锁文件
cd /opt/nutrition-web/src/nutrition-AI
./.venv/bin/pip install -r requirements.lock.txt

sudo systemctl restart nutrition-server nutrition-ai
sudo nginx -t
sudo systemctl reload nginx
```

发布后检查：

```bash
systemctl is-active nutrition-server nutrition-ai nginx
journalctl -u nutrition-server -n 100 --no-pager
journalctl -u nutrition-ai -n 100 --no-pager
curl -I https://www.example.com
curl -I https://www.example.com/api/food/search?keyword=鸡蛋
```

### 13.5 回滚

发布前保留：

```text
/opt/nutrition-web/app/nutrition-server.jar.bak.YYYY-MM-DD_HHMMSS
/opt/nutrition-web/www.bak.YYYY-MM-DD_HHMMSS/
/opt/nutrition-web/admin.bak.YYYY-MM-DD_HHMMSS/
```

回滚时恢复对应的 JAR 和前端目录，再执行：

```bash
sudo systemctl restart nutrition-server
sudo nginx -t
sudo systemctl reload nginx
```

---

## 14. 上线最终检查表

### 安全

- [ ] 未设置 `--spring.profiles.active=dev`（确保不创建 `test / 123456`）
- [ ] `JWT_SECRET`、`AI_AES_KEY`、`FASTAPI_SECRET_KEY` 均为随机值
- [ ] MySQL、Redis 未暴露公网
- [ ] Java 8088 和 Python 8004 只监听 127.0.0.1
- [ ] `/api/attachment/**` 上传/删除需登录，`{id}/url` 仍公开只读（已修复，回归验证一次）
- [ ] 回调接口已校验 API Key（已修复，回归验证一次）
- [ ] AI 配置、审核、知识库接口普通用户返回 403（已修复，回归验证一次）
- [ ] `sys_user` 无 `password_encrypted` 列，密码仅存 BCrypt
- [ ] Swagger/OpenAPI 已关闭（`SWAGGER_ENABLED=false`），访问 `/v3/api-docs` 返回 401/403
- [ ] `CORS_ALLOWED_ORIGINS` 只填真实域名（同域部署留空）
- [ ] 日志级别为 `INFO`，日志中不出现聊天原文/prompt
- [ ] MyBatis 未使用 `StdOutImpl`，默认 INFO 下不会把 SQL 和参数直接写入 journald
- [ ] OSS 若开启私有模式，已同步在阿里云把 Bucket 权限改为「私有」
- [ ] OSS 文件访问权限已确认
- [ ] 手动上传的 HTTPS 证书有效，已配置到期检查和人工更新流程

### 功能

- [ ] H5 中不存在 `localhost:8088`
- [ ] `sys_user` 已包含 `phone` 列
- [ ] `food_nutrition` 已导入 CSV 数据
- [ ] 管理员账号使用强密码
- [ ] 至少一条 `ai_config` 已启用
- [ ] Redis 缓存预热成功
- [ ] 注册（含手机号校验）、登录、重置密码、搜索、记录、上传、AI 对话均实测
- [ ] 重置密码：手机号不匹配时提示「手机号错误」
- [ ] 头像/动态图片能正常显示（若开启 OSS 私有模式，验证签名 URL 生效）
- [ ] 手机浏览器和桌面浏览器均能访问

### 运维

- [ ] systemd 开机自启
- [ ] Nginx 配置通过 `nginx -t`
- [ ] 生产服务器未依赖 Java 源码、Maven 或 Node.js 构建
- [ ] JAR、H5 和管理后台产物均由当前发布版本在本地或 CI 构建
- [ ] Python 虚拟环境在 Linux 服务器创建，未上传 Windows `.venv`
- [ ] `journald` 已限制 `SystemMaxUse`、`SystemKeepFree` 和 `MaxRetentionSec`
- [ ] Nginx 的 `/etc/logrotate.d/nginx` 已确认可正常轮转
- [ ] `nutrition-backup.timer` 已启用，MySQL、Redis 和配置备份成功生成
- [ ] 超过 7 天的 MySQL、Redis 和配置备份会自动清理
- [ ] Redis 已开启 AOF（`appendonly yes`、`appendfsync everysec`）
- [ ] 已完成至少一次 MySQL 和 Redis 恢复演练
- [ ] Java OOM 堆转储目录已创建，`jcmd`/`jmap`/`jstack` 可用
- [ ] 有 jar 和前端回滚包
- [ ] Python 依赖用 `requirements.lock.txt` 安装
- [ ] 发布命令和责任人已确定

---

## 15. 最小可用发布顺序

代码修复已完成，按这个顺序部署即可：

1. 在本地或 CI 构建 Java JAR、H5 和管理后台，上传 JAR、前端 `dist`、Python 源码（第 4 节）。
2. 从已上传 JAR 提取建库 SQL 和食物 CSV，完成数据库初始化（第 5 节）。
3. 创建强密码管理员（第 5.4 节）。
4. 写入 `/etc/nutrition/server.env` 与 `ai.env`，其中 `JWT_SECRET`、`AI_AES_KEY`、`FASTAPI_SECRET_KEY` 必须为随机值（第 8 节）。
5. 在服务器安装 Python 依赖，校验 JAR 和前端静态产物，然后启动 Java + Python（第 7、9 节）。**注意：未设置 `JWT_SECRET` / `AI_AES_KEY` 时后端会启动失败，这是预期行为。**
6. 配置 Nginx/HTTPS 和防火墙（第 10、11 节）。
7. 配置 journald 容量限制、Nginx 日志轮转、Redis AOF 和每日备份 timer（第 13 节）。
8. 配置并启用 AI 模型（第 12.2 节）。
9. 完成注册（手机号）、登录、重置密码、搜索、上传和 AI 实测后再公开发布（第 12.3 节）。
