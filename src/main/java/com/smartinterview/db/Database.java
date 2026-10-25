package com.smartinterview.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * Local SQLite database. The file lives in ./data next to where the app is
 * launched, and the schema is created on first run.
 */
public final class Database {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path DB_FILE = Path.of("data", "smartinterview.db");
    private static boolean initialised;

    private Database() {}

    /** Opens a connection. SQLite connections are cheap, so callers open one per operation. */
    public static Connection connect() throws SQLException {
        init();
        return open();
    }

    private static Connection open() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE.toString().replace('\\', '/'));
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
        return c;
    }

    private static synchronized void init() throws SQLException {
        if (initialised) {
            return;
        }
        try {
            Files.createDirectories(DB_FILE.getParent());
            Class.forName("org.sqlite.JDBC");
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create the data directory", e);
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver is missing", e);
        }

        try (Connection c = open(); Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    full_name TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                    password_hash TEXT NOT NULL,
                    college TEXT,
                    degree TEXT,
                    year TEXT,
                    role TEXT NOT NULL DEFAULT 'student',
                    created_at INTEGER NOT NULL
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS questions (
                    id TEXT PRIMARY KEY,
                    type TEXT NOT NULL,
                    category TEXT NOT NULL,
                    difficulty TEXT NOT NULL,
                    text TEXT NOT NULL,
                    options_json TEXT,
                    correct_index INTEGER NOT NULL DEFAULT 0,
                    code_template TEXT,
                    expected_output TEXT,
                    sample_answer TEXT,
                    explanation TEXT,
                    source TEXT NOT NULL DEFAULT 'seed'
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS interviews (
                    id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    role_name TEXT NOT NULL,
                    overall_score REAL NOT NULL,
                    time_taken_min INTEGER NOT NULL,
                    taken_at INTEGER NOT NULL,
                    questions_answered INTEGER NOT NULL DEFAULT 0,
                    round_scores_json TEXT,
                    strengths_json TEXT,
                    weaknesses_json TEXT,
                    recommendations_json TEXT
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS bookmarks (
                    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    question_id TEXT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
                    PRIMARY KEY (user_id, question_id)
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    user_id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                    default_difficulty TEXT NOT NULL DEFAULT 'Medium',
                    default_duration TEXT NOT NULL DEFAULT '30 minutes',
                    email_notifications INTEGER NOT NULL DEFAULT 1,
                    practice_reminders INTEGER NOT NULL DEFAULT 0
                )""");
            addColumnIfMissing(s, "interviews", "details_json", "TEXT");
        }

        Seeder.seedIfEmpty(Database::open);
        initialised = true;
    }


    /** Upgrades databases made before a column existed. */
    private static void addColumnIfMissing(Statement s, String table, String column, String type) throws SQLException {
        try (java.sql.ResultSet rs = s.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return;
                }
            }
        }
        s.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
    }

    public static <T> T fromJson(String json, Class<T> type, T fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return JSON.readValue(json, type);
        } catch (IOException e) {
            return fallback;
        }
    }
    // ------------------------------------------------------------ JSON helpers

    public static String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<String> stringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JSON.readValue(json, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            return List.of();
        }
    }

    public static Map<String, Double> doubleMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return JSON.readValue(json, new TypeReference<java.util.LinkedHashMap<String, Double>>() {});
        } catch (IOException e) {
            return Map.of();
        }
    }
}
