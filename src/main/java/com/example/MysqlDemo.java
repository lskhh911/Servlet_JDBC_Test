package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MysqlDemo {

    // 连接 URL：把主机、端口、数据库名换成你自己的
    // 额外参数说明：
    //   useSSL=false                  本地/内网连接通常不开 SSL
    //   allowPublicKeyRetrieval=true  MySQL 8 的 caching_sha2_password 认证需要
    //   serverTimezone=Asia/Shanghai  避免时区报错
    //   characterEncoding=utf8        避免中文乱码
    private static final String JDBC_URL =
            "jdbc:mysql://100.114.239.18:3306/learnjdbc"
                    + "?useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=Asia/Shanghai"
                    + "&characterEncoding=utf8";

    private static final String JDBC_USER = "learn";
    private static final String JDBC_PASSWORD = "learnpassword";

    public static void main(String[] args) {

        // JDBC 4.0 之后驱动会自动加载，这里显式加载可以更早暴露“缺驱动”的问题
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("找不到 MySQL 驱动，请确认 mysql-connector-j 已在 classpath 中");
            e.printStackTrace();
            return;
        }

        // try-with-resources：代码块结束时自动 conn.close()，不会忘记关闭
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {

            System.out.println("连接成功！URL = " + conn.getMetaData().getURL());
            System.out.println("当前数据库 = " + conn.getCatalog());

            // 查询 students 表：只取男生（gender = 1）
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT id, grade, name, gender FROM students WHERE gender=0")) {

                System.out.println("id\tgrade\tname\tgender");

                // ResultSet 内部指向第一行之前，next() 返回 true 表示还有下一行
                while (rs.next()) {
                    long id = rs.getLong(1);      // 注意：索引从 1 开始
                    long grade = rs.getLong(2);
                    String name = rs.getString(3);
                    int gender = rs.getInt(4);

                    System.out.println(id + "\t" + grade + "\t" + name + "\t" + gender);
                }
            }

        } catch (SQLException e) {
            System.err.println("数据库操作失败：" + e.getMessage());
            e.printStackTrace();
        }

        // 到这里连接已经自动关闭
    }
}