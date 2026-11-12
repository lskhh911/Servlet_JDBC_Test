package com.example.web;

import com.example.dao.StudentDao;
import com.example.model.Student;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 学生 REST 接口：
 *   GET    /api/students       查询全部
 *   GET    /api/students/{id}  查询单个
 *   POST   /api/students       新增
 *   PUT    /api/students/{id}  修改
 *   DELETE /api/students/{id}  删除
 */
@WebServlet(name = "StudentServlet", urlPatterns = "/api/students/*")
public class StudentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();

    private final StudentDao studentDao = new StudentDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");

        try {
            String pathInfo = request.getPathInfo();

            if (isCollection(pathInfo)) {
                writeJson(response, HttpServletResponse.SC_OK, studentDao.findAll());
                return;
            }

            Long id = parseId(pathInfo);
            if (id == null) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "学生 id 不合法。");
                return;
            }

            Student student = studentDao.findById(id);
            if (student == null) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "学生不存在。");
                return;
            }

            writeJson(response, HttpServletResponse.SC_OK, student);
        } catch (SQLException e) {
            handleServerError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");

        if (!isCollection(request.getPathInfo())) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "接口不存在。");
            return;
        }

        Student student = readStudent(request, response);
        if (student == null) {
            return;
        }

        String error = validate(student);
        if (error != null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, error);
            return;
        }

        try {
            Student created = studentDao.insert(student);
            response.setHeader("Location",
                    request.getContextPath() + request.getServletPath() + "/" + created.getId());
            writeJson(response, HttpServletResponse.SC_CREATED, created);
        } catch (SQLException e) {
            handleServerError(response, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");

        Long id = parseId(request.getPathInfo());
        if (id == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "请提供合法的学生 id。");
            return;
        }

        Student student = readStudent(request, response);
        if (student == null) {
            return;
        }

        student.setId(id);

        String error = validate(student);
        if (error != null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, error);
            return;
        }

        try {
            if (!studentDao.update(student)) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "学生不存在。");
                return;
            }
            writeJson(response, HttpServletResponse.SC_OK, student);
        } catch (SQLException e) {
            handleServerError(response, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");

        Long id = parseId(request.getPathInfo());
        if (id == null) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "请提供合法的学生 id。");
            return;
        }

        try {
            if (!studentDao.delete(id)) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "学生不存在。");
                return;
            }
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (SQLException e) {
            handleServerError(response, e);
        }
    }

    private boolean isCollection(String pathInfo) {
        return pathInfo == null || "/".equals(pathInfo);
    }

    /**
     * 只接受 /{数字}，避免把 /1/extra 之类的路径误当成合法 id。
     */
    private Long parseId(String pathInfo) {
        if (pathInfo == null || !pathInfo.matches("/\\d+")) {
            return null;
        }

        try {
            long id = Long.parseLong(pathInfo.substring(1));
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Student readStudent(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            char[] buffer = new char[1024];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                body.append(buffer, 0, length);
            }
        }

        if (body.length() == 0) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "请求体不能为空。");
            return null;
        }

        try {
            Student student = GSON.fromJson(body.toString(), Student.class);
            if (student == null) {
                writeError(response, HttpServletResponse.SC_BAD_REQUEST, "JSON 内容不合法。");
                return null;
            }
            return student;
        } catch (JsonSyntaxException e) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "JSON 格式错误。");
            return null;
        }
    }

    private String validate(Student student) {
        if (student.getName() == null || student.getName().trim().isEmpty()) {
            return "姓名不能为空。";
        }

        String name = student.getName().trim();
        if (name.length() > 50) {
            return "姓名不能超过 50 个字符。";
        }
        student.setName(name);

        if (student.getGender() != 0 && student.getGender() != 1) {
            return "性别只能是 0（女）或 1（男）。";
        }

        if (student.getGrade() < 1 || student.getGrade() > 6) {
            return "年级只能是 1 到 6。";
        }

        if (student.getScore() < 0 || student.getScore() > 100) {
            return "成绩只能是 0 到 100。";
        }

        return null;
    }

    private void writeJson(HttpServletResponse response, int status, Object data)
            throws IOException {

        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        GSON.toJson(data, response.getWriter());
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {

        Map<String, String> body = new HashMap<>();
        body.put("message", message);
        writeJson(response, status, body);
    }

    private void handleServerError(HttpServletResponse response, SQLException exception)
            throws IOException {

        log("数据库操作失败", exception);
        writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "服务器处理失败，请检查数据库连接或稍后重试。");
    }
}
