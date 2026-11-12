package com.example.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC 连接工具。
 *
 * 默认读取 classpath 下的 db.properties；
 * 也可以通过环境变量 DB_URL / DB_USERNAME / DB_PASSWORD 覆盖，方便换环境。
 */
public final class DBUtil {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = DBUtil.class.getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (input == null) {
                throw new IllegalStateException("找不到 db.properties");
            }

            PROPERTIES.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        String url = resolve("DB_URL", "db.url");
        String username = resolve("DB_USERNAME", "db.username");
        String password = resolve("DB_PASSWORD", "db.password");
        return DriverManager.getConnection(url, username, password);
    }

    private static String resolve(String environmentName, String propertyName) {
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.trim().isEmpty()) {
            return environmentValue;
        }
        return PROPERTIES.getProperty(propertyName);
    }
}
