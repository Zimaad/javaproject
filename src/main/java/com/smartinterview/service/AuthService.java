package com.smartinterview.service;

import com.smartinterview.model.User;

/**
 * Mock authentication service.
 * Always returns success with a dummy user.
 * Replace with real auth (REST API / DB) later.
 */
public class MockAuthService {

    private static MockAuthService instance;
    private User currentUser;

    private MockAuthService() {
    }

    public static MockAuthService getInstance() {
        if (instance == null) {
            instance = new MockAuthService();
        }
        return instance;
    }

    /**
     * Mock login — always succeeds.
     */
    public User login(String email, String password) {
        currentUser = new User(
                "USR001",
                "Zimaad Azhari",
                email != null && !email.isEmpty() ? email : "Zimaad @university.edu",
                "MIT Institute of Technology",
                "B.Tech Computer Science",
                "3rd Year",
                "student");
        return currentUser;
    }

    /**
     * Mock admin login.
     */
    public User loginAsAdmin(String email, String password) {
        currentUser = new User(
                "ADM001",
                "Dr. Priya Patel",
                "admin@smartinterview.com",
                "MIT Institute of Technology",
                "Professor - CS Department",
                "",
                "admin");
        return currentUser;
    }

    /**
     * Mock registration — always succeeds.
     */
    public User register(String fullName, String email, String password,
            String college, String degree, String year) {
        currentUser = new User("USR002", fullName, email, college, degree, year, "student");
        return currentUser;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
