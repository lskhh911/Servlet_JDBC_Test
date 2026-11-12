-- 和当前项目配套的数据库结构。
-- 如果 learnjdbc 和 students 表已经存在，可以只执行 CREATE TABLE 部分。
CREATE DATABASE IF NOT EXISTS learnjdbc
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE learnjdbc;

CREATE TABLE IF NOT EXISTS students (
  id     BIGINT       NOT NULL AUTO_INCREMENT,
  name   VARCHAR(50)  NOT NULL,
  gender TINYINT(1)   NOT NULL COMMENT '1=男，0=女',
  grade  INT          NOT NULL COMMENT '1 到 6 年级',
  score  INT          NOT NULL COMMENT '0 到 100 分',
  PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
