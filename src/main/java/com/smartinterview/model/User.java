package com.smartinterview.model;

/**
 * Represents a user (student or admin) in the system.
 */
public class User {
    private String id;
    private String fullName;
    private String email;
    private String college;
    private String degree;
    private String year;
    private String role; // "student" or "admin"
    private String avatarUrl;

    public User() {}

    public User(String id, String fullName, String email, String college, String degree, String year, String role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.college = college;
        this.degree = degree;
        this.year = year;
        this.role = role;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
