package com.example.dao;

import com.example.model.Student;
import com.example.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生表的数据访问对象，只负责和数据库打交道。
 * SQL 全部使用 PreparedStatement，避免 SQL 注入。
 */
public class StudentDao {

    public List<Student> findAll() throws SQLException {
        String sql = "SELECT id, name, gender, grade, score FROM students ORDER BY id";
        List<Student> students = new ArrayList<>();

        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                students.add(mapRow(resultSet));
            }
        }
        return students;
    }

    public Student findById(long id) throws SQLException {
        String sql = "SELECT id, name, gender, grade, score FROM students WHERE id = ?";

        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        }
    }

    public Student insert(Student student) throws SQLException {
        String sql = "INSERT INTO students (name, gender, grade, score) VALUES (?, ?, ?, ?)";

        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, student.getName());
            statement.setBoolean(2, student.getGender() == 1);
            statement.setInt(3, student.getGrade());
            statement.setInt(4, student.getScore());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("新增学生成功，但没有拿到自增主键");
                }
                student.setId(keys.getLong(1));
            }
        }
        return student;
    }

    public boolean update(Student student) throws SQLException {
        String sql = "UPDATE students SET name = ?, gender = ?, grade = ?, score = ? WHERE id = ?";

        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, student.getName());
            statement.setBoolean(2, student.getGender() == 1);
            statement.setInt(3, student.getGrade());
            statement.setInt(4, student.getScore());
            statement.setLong(5, student.getId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM students WHERE id = ?";

        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private Student mapRow(ResultSet resultSet) throws SQLException {
        return new Student(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getBoolean("gender") ? 1 : 0,
                resultSet.getInt("grade"),
                resultSet.getInt("score")
        );
    }
}
