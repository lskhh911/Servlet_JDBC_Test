# 学生信息管理（Servlet + JDBC）

一个用于 JavaWeb 入门练习的完整小项目：

```text
浏览器（HTML + CSS + JavaScript）
        │  fetch JSON
        ▼
StudentServlet（REST 接口）
        │
        ▼
StudentDao（JDBC + PreparedStatement）
        │
        ▼
MySQL：learnjdbc.students
```

页面支持查询、按姓名/年级搜索、新增、编辑和删除，所有修改都会写入数据库。

## 项目结构

```text
src/main/java/com/example/
├── dao/StudentDao.java                 JDBC 数据访问
├── model/Student.java                  学生实体
├── util/DBUtil.java                    数据库连接工具
└── web/
    ├── StudentServlet.java             /api/students REST 接口
    └── CharacterEncodingFilter.java    UTF-8 编码过滤器

src/main/resources/db.properties        数据库连接配置
src/main/webapp/
├── index.html                          页面结构
├── css/style.css                       页面样式
├── js/app.js                           页面交互与接口请求
└── WEB-INF/web.xml                     Web 应用配置
sql/schema.sql                          建表 SQL
```

## 环境要求

- JDK 8 或更高版本
- Maven 3.6+
- **Tomcat 9.x**

> 注意：本项目使用 `javax.servlet`，对应 Tomcat 9。Tomcat 10 及以上改成了
> `jakarta.servlet`，不修改代码直接部署会找不到 Servlet。

## 数据库配置

数据库连接在 `src/main/resources/db.properties`：

```properties
db.url=jdbc:mysql://100.114.239.18:3306/learnjdbc?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
db.username=learn
db.password=learnpassword
```

也可以使用环境变量覆盖，避免把密码提交到代码仓库：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

建表语句在 `sql/schema.sql`。当前代码使用的表结构：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT | 自增主键 |
| name | VARCHAR(50) | 姓名 |
| gender | TINYINT(1) | 1=男，0=女 |
| grade | INT | 1 到 6 年级 |
| score | INT | 0 到 100 分 |

## 一键启动（Windows）

项目根目录提供了启动脚本：

- 双击 `start.bat`
- 启动后访问 `http://localhost:8080/my-app/`
- 双击 `stop.bat` 停止服务

也可以在 PowerShell 中手动执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\start.ps1
powershell -ExecutionPolicy Bypass -File .\stop.ps1
```

第一次运行时会自动完成以下工作：

1. 使用 Maven 构建 `target/my-app.war`
2. 把 Tomcat 9 下载到 `%LOCALAPPDATA%\my-app-tomcat`
3. 部署 WAR 并启动 Tomcat（端口 8080）

> 页面必须通过 Tomcat 访问，不能直接双击 `index.html`。
> 因为前端需要请求 `/api/students`，直接打开文件时没有后端可用。

## 手动打包和部署

在项目根目录执行：

```bash
mvn clean package
```

生成 `target/my-app.war`，把它复制到 Tomcat 的 `webapps` 目录：

```text
apache-tomcat-9.x/webapps/my-app.war
```

启动 Tomcat，然后访问：

```text
http://localhost:8080/my-app/
```

### IDEA 中运行

1. Run → Edit Configurations → 添加 **Tomcat Server → Local**。
2. 选择本机 Tomcat 9 的安装目录。
3. 在 Deployment 选项卡中添加 `my-app:war exploded`。
4. Application context 填 `/my-app`，启动后访问 `http://localhost:8080/my-app/`。

## REST 接口

| 方法 | 地址 | 作用 | 成功状态码 |
| --- | --- | --- | --- |
| GET | `/api/students` | 查询全部学生 | 200 |
| GET | `/api/students/{id}` | 查询单个学生 | 200 |
| POST | `/api/students` | 新增学生 | 201 |
| PUT | `/api/students/{id}` | 修改学生 | 200 |
| DELETE | `/api/students/{id}` | 删除学生 | 204 |

新增或修改时请求体格式：

```json
{
  "name": "张三",
  "gender": 1,
  "grade": 3,
  "score": 88
}
```

校验失败返回 `400`：

```json
{
  "message": "成绩只能是 0 到 100。"
}
```

## 学习提示

- 前端用 `fetch` 调用接口，路径是相对路径 `api/students`，所以部署在 `/my-app` 或根路径都能工作。
- DAO 中所有 SQL 都使用 `PreparedStatement`，避免拼接字符串造成 SQL 注入。
- 每次请求通过 `DriverManager` 新建连接，适合入门练习；真实项目通常会使用连接池（如 HikariCP）。
- 当前数据库密码以明文写在配置文件中，仅供本地练习；生产环境应使用环境变量或配置中心。
- 原来的 `MysqlDemo.java` 仍然保留，可以单独运行，用来复习最基础的 JDBC 查询。
