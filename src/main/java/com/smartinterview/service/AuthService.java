package com.smartinterview.service;

import com.smartinterview.db.Database;
import com.smartinterview.db.PasswordHasher;
import com.smartinterview.model.User;
import com.smartinterview.model.UserSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Accounts, sessions and per-user settings, backed by the local database.
 */
public class AuthService {

    private static final AuthService INSTANCE = new AuthService();
    private static final int MIN_PASSWORD_LENGTH = 8;

    private User currentUser;

    private AuthService() {
    }

    public static AuthService getInstance() {
        return INSTANCE;
    }

    /** Signs in with email and password; the same message is used for either being wrong. */
    public User login(String email, String password) throws AuthException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email == null ? "" : email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordHasher.verify(password == null ? "" : password,
                        rs.getString("password_hash"))) {
                    currentUser = read(rs);
                    return currentUser;
                }
            }
        } catch (SQLException e) {
            throw storageError(e);
        }
        throw new AuthException("Incorrect email or password.");
    }

    public User register(String fullName, String email, String password,
                         String college, String degree, String year) throws AuthException {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new AuthException("Use at least " + MIN_PASSWORD_LENGTH + " characters for your password.");
        }
        String id = "USR-" + UUID.randomUUID().toString().substring(0, 8);
        String sql = """
            INSERT INTO users (id, full_name, email, password_hash, college, degree, year, role, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'student', ?)""";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, fullName);
            ps.setString(3, email.trim());
            ps.setString(4, PasswordHasher.hash(password));
            ps.setString(5, college == null ? "" : college);
            ps.setString(6, degree == null ? "" : degree);
            ps.setString(7, year == null ? "" : year);
            ps.setLong(8, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new AuthException("An account with that email already exists.");
            }
            throw storageError(e);
        }
        return new User(id, fullName, email.trim(), college, degree, year, "student");
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAdmin() {
        return currentUser != null && "admin".equalsIgnoreCase(currentUser.getRole());
    }

    public void logout() {
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Persists edits to the signed-in user's profile. */
    public void updateProfile(String fullName, String college, String degree, String year) throws AuthException {
        requireUser();
        String sql = "UPDATE users SET full_name = ?, college = ?, degree = ?, year = ? WHERE id = ?";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, college == null ? "" : college);
            ps.setString(3, degree == null ? "" : degree);
            ps.setString(4, year == null ? "" : year);
            ps.setString(5, currentUser.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw storageError(e);
        }
        currentUser.setFullName(fullName);
        currentUser.setCollege(college);
        currentUser.setDegree(degree);
        currentUser.setYear(year);
    }

    public void changePassword(String currentPassword, String newPassword) throws AuthException {
        requireUser();
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new AuthException("Use at least " + MIN_PASSWORD_LENGTH + " characters for the new password.");
        }
        try (Connection c = Database.connect()) {
            String stored;
            try (PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM users WHERE id = ?")) {
                ps.setString(1, currentUser.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    stored = rs.next() ? rs.getString(1) : null;
                }
            }
            if (!PasswordHasher.verify(currentPassword == null ? "" : currentPassword, stored)) {
                throw new AuthException("Your current password is incorrect.");
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
                ps.setString(1, PasswordHasher.hash(newPassword));
                ps.setString(2, currentUser.getId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw storageError(e);
        }
    }

    /** Deletes the signed-in account and, via cascade, its interviews, bookmarks and settings. */
    public void deleteCurrentAccount() throws AuthException {
        requireUser();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setString(1, currentUser.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw storageError(e);
        }
        currentUser = null;
    }

    // ------------------------------------------------------------ settings

    public UserSettings getSettings() {
        UserSettings settings = new UserSettings();
        if (currentUser == null) {
            return settings;
        }
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM settings WHERE user_id = ?")) {
            ps.setString(1, currentUser.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    settings.setDefaultDifficulty(rs.getString("default_difficulty"));
                    settings.setDefaultDuration(rs.getString("default_duration"));
                    settings.setEmailNotifications(rs.getInt("email_notifications") == 1);
                    settings.setPracticeReminders(rs.getInt("practice_reminders") == 1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not load settings: " + e.getMessage());
        }
        return settings;
    }

    public void saveSettings(UserSettings s) throws AuthException {
        requireUser();
        String sql = """
            INSERT INTO settings (user_id, default_difficulty, default_duration, email_notifications, practice_reminders)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(user_id) DO UPDATE SET
                default_difficulty = excluded.default_difficulty,
                default_duration = excluded.default_duration,
                email_notifications = excluded.email_notifications,
                practice_reminders = excluded.practice_reminders""";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, currentUser.getId());
            ps.setString(2, s.getDefaultDifficulty());
            ps.setString(3, s.getDefaultDuration());
            ps.setInt(4, s.isEmailNotifications() ? 1 : 0);
            ps.setInt(5, s.isPracticeReminders() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw storageError(e);
        }
    }

    // ------------------------------------------------------------ helpers

    private void requireUser() throws AuthException {
        if (currentUser == null) {
            throw new AuthException("You are not signed in.");
        }
    }

    private static User read(ResultSet rs) throws SQLException {
        return new User(rs.getString("id"), rs.getString("full_name"), rs.getString("email"),
                rs.getString("college"), rs.getString("degree"), rs.getString("year"), rs.getString("role"));
    }

    private static AuthException storageError(SQLException e) {
        e.printStackTrace();
        return new AuthException("The local database could not be read or written: " + e.getMessage());
    }
}
