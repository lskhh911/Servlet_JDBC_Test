package com.example.model;

/**
 * 学生实体，字段与数据库 students 表对应。
 */
public class Student {

    private long id;
    private String name;
    private int gender; // 1 = 男，0 = 女
    private int grade;
    private int score;

    public Student() {
    }

    public Student(long id, String name, int gender, int grade, int score) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.grade = grade;
        this.score = score;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
