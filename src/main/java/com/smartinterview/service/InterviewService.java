package com.smartinterview.service;

import com.smartinterview.db.Database;
import com.smartinterview.model.ActivityEntry;
import com.smartinterview.model.Interview;
import com.smartinterview.model.InterviewResult;
import com.smartinterview.model.PerformanceData;
import com.smartinterview.model.QuestionReview;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Interview roles, building a session, and storing and reading finished interviews.
 */
public class InterviewService {

    private static final InterviewService INSTANCE = new InterviewService();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);

    private InterviewService() {
    }

    public static InterviewService getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------ roles & sessions

    public List<Interview> getAvailableRoles() {
        List<Interview> roles = new ArrayList<>();

        Interview java = new Interview("INT001", "Java Developer",
                "Test your Java fundamentals, OOP knowledge, and problem-solving ability.",
                "Medium", 30, 15, Arrays.asList("Java", "OOP", "Collections", "JDBC", "Multithreading"));
        java.setRoleIcon("fab-java");
        roles.add(java);

        Interview swe = new Interview("INT002", "Software Engineer",
                "Comprehensive assessment of CS fundamentals, algorithms, and system design skills.",
                "Hard", 45, 20, Arrays.asList("DSA", "Algorithms", "DBMS", "OS", "System Design"));
        swe.setRoleIcon("fas-laptop-code");
        roles.add(swe);

        Interview data = new Interview("INT003", "Data Analyst",
                "Evaluate your data analysis, SQL proficiency, and statistical reasoning skills.",
                "Medium", 30, 15, Arrays.asList("SQL", "Python", "Statistics", "Data Analysis"));
        data.setRoleIcon("fas-chart-bar");
        roles.add(data);

        return roles;
    }

    /**
     * Picks the questions for every selected round. Blocks (it may call the AI), so run it off the UI thread.
     */
    public InterviewSession createSession(Interview interview) {
        InterviewSession session = new InterviewSession(interview);
        QuestionService questions = QuestionService.getInstance();
        List<String> rounds = interview.getRounds();
        int count = interview.getQuestionCount();
        String difficulty = interview.getDifficulty();

        if (rounds.contains("MCQ")) {
            session.setMcqQuestions(questions.pick("mcq", interview.getRoleName(), interview.getTopics(),
                    difficulty, count, session.getNotes()));
        }
        if (rounds.contains("Coding")) {
            session.setCodingQuestions(questions.pick("coding", interview.getRoleName(), interview.getTopics(),
                    difficulty, count >= 15 ? 3 : 2, session.getNotes()));
        }
        if (rounds.contains("HR")) {
            session.setHrQuestions(questions.pick("hr", interview.getRoleName(), interview.getTopics(),
                    difficulty, Math.max(3, count / 3), session.getNotes()));
        }
        return session;
    }

    // ------------------------------------------------------------ storing results

    public void saveResult(InterviewResult r) throws SQLException {
        String sql = """
            INSERT INTO interviews (id, user_id, role_name, overall_score, time_taken_min, taken_at,
                                    questions_answered, round_scores_json, strengths_json, weaknesses_json,
                                    recommendations_json, details_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getInterviewId());
            ps.setString(2, AuthService.getInstance().getCurrentUser().getId());
            ps.setString(3, r.getRoleName());
            ps.setDouble(4, r.getOverallScore());
            ps.setInt(5, r.getTimeTakenMinutes());
            ps.setLong(6, System.currentTimeMillis());
            ps.setInt(7, r.getQuestionsAnswered());
            ps.setString(8, Database.toJson(r.getRoundScores()));
            ps.setString(9, Database.toJson(r.getStrengths()));
            ps.setString(10, Database.toJson(r.getWeaknesses()));
            ps.setString(11, Database.toJson(r.getRecommendations()));
            ps.setString(12, Database.toJson(r.getReviews()));
            ps.executeUpdate();
        }
    }

    public static String newInterviewId() {
        return "INT-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // ------------------------------------------------------------ student reads

    /** All of the signed-in user's interviews, newest first. */
    public List<InterviewResult> getHistory() {
        return queryResults("SELECT * FROM interviews WHERE user_id = ? ORDER BY taken_at DESC", -1);
    }

    public List<InterviewResult> getRecent(int limit) {
        return queryResults("SELECT * FROM interviews WHERE user_id = ? ORDER BY taken_at DESC LIMIT ?", limit);
    }

    public InterviewResult getLatest() {
        List<InterviewResult> recent = getRecent(1);
        return recent.isEmpty() ? null : recent.get(0);
    }

    public int getInterviewsCompleted() {
        return (int) scalar("SELECT COUNT(*) FROM interviews WHERE user_id = ?", userId());
    }

    public double getAverageScore() {
        return scalar("SELECT COALESCE(AVG(overall_score), 0) FROM interviews WHERE user_id = ?", userId());
    }

    public double getBestScore() {
        return scalar("SELECT COALESCE(MAX(overall_score), 0) FROM interviews WHERE user_id = ?", userId());
    }

    public int getQuestionsAnswered() {
        return (int) scalar("SELECT COALESCE(SUM(questions_answered), 0) FROM interviews WHERE user_id = ?", userId());
    }

    /** The last {@code limit} interviews, oldest first, for the trend chart. */
    public List<PerformanceData> getPerformanceHistory(int limit) {
        List<InterviewResult> recent = new ArrayList<>(getRecent(limit));
        Collections.reverse(recent);
        List<PerformanceData> points = new ArrayList<>();
        int total = getInterviewsCompleted();
        int first = total - recent.size() + 1;
        for (int i = 0; i < recent.size(); i++) {
            InterviewResult r = recent.get(i);
            points.add(new PerformanceData("Interview " + (first + i), r.getOverallScore(), r.getDate()));
        }
        return points;
    }

    // ------------------------------------------------------------ admin reads

    public int getTotalStudents() {
        return (int) scalar("SELECT COUNT(*) FROM users WHERE role = 'student'");
    }

    public int getTotalInterviews() {
        return (int) scalar("SELECT COUNT(*) FROM interviews");
    }

    public double getGlobalAverageScore() {
        return Math.round(scalar("SELECT COALESCE(AVG(overall_score), 0) FROM interviews") * 10) / 10.0;
    }

    /** Interviews taken on each of the last seven days, oldest first, keyed by weekday name. */
    public Map<String, Integer> getInterviewsPerDay() {
        Map<String, Integer> perDay = new LinkedHashMap<>();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        for (int i = 6; i >= 0; i--) {
            perDay.put(today.minusDays(i).getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), 0);
        }
        long since = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT taken_at FROM interviews WHERE taken_at >= ?")) {
            ps.setLong(1, since);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String day = Instant.ofEpochMilli(rs.getLong(1)).atZone(zone).getDayOfWeek()
                            .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    perDay.merge(day, 1, Integer::sum);
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not load activity: " + e.getMessage());
        }
        return perDay;
    }

    /** Latest completed interviews and sign-ups, newest first. */
    public List<ActivityEntry> getRecentActivity(int limit) {
        List<ActivityEntry> entries = new ArrayList<>();
        try (Connection c = Database.connect()) {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT u.full_name, i.role_name, i.overall_score, i.taken_at
                    FROM interviews i JOIN users u ON u.id = i.user_id
                    ORDER BY i.taken_at DESC LIMIT ?""")) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(new ActivityEntry("interview",
                                rs.getString(1) + " completed " + rs.getString(2) + " mock",
                                "scored " + (int) rs.getDouble(3) + "%", rs.getLong(4)));
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT full_name, created_at FROM users WHERE role = 'student' ORDER BY created_at DESC LIMIT ?")) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(new ActivityEntry("signup", "New student " + rs.getString(1) + " signed up",
                                "", rs.getLong(2)));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not load activity: " + e.getMessage());
        }
        entries.sort((a, b) -> Long.compare(b.at(), a.at()));
        return entries.subList(0, Math.min(limit, entries.size()));
    }

    public static String timeAgo(long millis) {
        long minutes = Math.max(0, (System.currentTimeMillis() - millis) / 60000);
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
        long days = hours / 24;
        return days + (days == 1 ? " day ago" : " days ago");
    }

    // ------------------------------------------------------------ helpers

    private static String userId() {
        return AuthService.getInstance().isLoggedIn() ? AuthService.getInstance().getCurrentUser().getId() : "";
    }

    private static double scalar(String sql, String... params) {
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("Query failed: " + e.getMessage());
            return 0;
        }
    }

    private List<InterviewResult> queryResults(String sql, int limit) {
        List<InterviewResult> results = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId());
            if (limit >= 0) {
                ps.setInt(2, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InterviewResult r = new InterviewResult();
                    r.setInterviewId(rs.getString("id"));
                    r.setRoleName(rs.getString("role_name"));
                    r.setOverallScore(rs.getDouble("overall_score"));
                    r.setTimeTakenMinutes(rs.getInt("time_taken_min"));
                    r.setDate(DATE.format(Instant.ofEpochMilli(rs.getLong("taken_at"))
                            .atZone(ZoneId.systemDefault())));
                    r.setQuestionsAnswered(rs.getInt("questions_answered"));
                    r.setRoundScores(Database.doubleMap(rs.getString("round_scores_json")));
                    r.setStrengths(Database.stringList(rs.getString("strengths_json")));
                    r.setWeaknesses(Database.stringList(rs.getString("weaknesses_json")));
                    r.setRecommendations(Database.stringList(rs.getString("recommendations_json")));
                    r.setReviews(java.util.Arrays.asList(Database.fromJson(rs.getString("details_json"),
                            QuestionReview[].class, new QuestionReview[0])));
                    results.add(r);
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not load interviews: " + e.getMessage());
        }
        return results;
    }

    /** Formats "now" the way results show their date. */
    public static String today() {
        return DATE.format(LocalDate.now());
    }
}
