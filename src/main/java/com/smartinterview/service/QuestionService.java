package com.smartinterview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartinterview.ai.AiClient;
import com.smartinterview.ai.AiException;
import com.smartinterview.db.Database;
import com.smartinterview.model.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * The question bank: storage, bookmarks, picking questions for an interview,
 * and growing the bank with AI-written questions.
 */
public class QuestionService {

    private static final QuestionService INSTANCE = new QuestionService();
    private static final List<String> DIFFICULTIES = List.of("Easy", "Medium", "Hard");

    private QuestionService() {
    }

    public static QuestionService getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------ queries

    /** Every question, with bookmark flags set for the signed-in user. */
    public List<Question> getAll() {
        Set<String> saved = bookmarkedIds();
        List<Question> all = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM questions ORDER BY type, category, id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Question q = read(rs);
                q.setBookmarked(saved.contains(q.getId()));
                all.add(q);
            }
        } catch (SQLException e) {
            System.err.println("Could not load questions: " + e.getMessage());
        }
        return all;
    }

    public int count() {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM questions");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------ admin edits

    /** Inserts the question, or updates it when it already has an id. */
    public Question save(Question q) throws SQLException {
        boolean isNew = q.getId() == null || q.getId().isBlank();
        if (isNew) {
            q.setId("U-" + UUID.randomUUID().toString().substring(0, 8));
        }
        String sql = """
            INSERT INTO questions (id, type, category, difficulty, text, options_json, correct_index,
                                   code_template, expected_output, sample_answer, explanation, source)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                type = excluded.type, category = excluded.category, difficulty = excluded.difficulty,
                text = excluded.text, options_json = excluded.options_json,
                correct_index = excluded.correct_index, code_template = excluded.code_template,
                expected_output = excluded.expected_output, sample_answer = excluded.sample_answer,
                explanation = excluded.explanation""";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, q, "admin");
            ps.executeUpdate();
        }
        return q;
    }

    public void delete(String id) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM questions WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------ bookmarks

    public void setBookmarked(String questionId, boolean on) {
        if (!AuthService.getInstance().isLoggedIn()) {
            return;
        }
        String sql = on
                ? "INSERT OR IGNORE INTO bookmarks (user_id, question_id) VALUES (?, ?)"
                : "DELETE FROM bookmarks WHERE user_id = ? AND question_id = ?";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, AuthService.getInstance().getCurrentUser().getId());
            ps.setString(2, questionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Could not save bookmark: " + e.getMessage());
        }
    }

    private Set<String> bookmarkedIds() {
        Set<String> ids = new HashSet<>();
        if (!AuthService.getInstance().isLoggedIn()) {
            return ids;
        }
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT question_id FROM bookmarks WHERE user_id = ?")) {
            ps.setString(1, AuthService.getInstance().getCurrentUser().getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not load bookmarks: " + e.getMessage());
        }
        return ids;
    }

    // ------------------------------------------------------------ interview picking

    /**
     * Picks {@code count} questions of one type for an interview. MCQs are limited to the role's topics.
     * When the bank has too few and an AI key is configured, new questions are generated and kept.
     * Problems are appended to {@code notes} rather than thrown, so the interview can still start.
     */
    public List<Question> pick(String type, String roleName, List<String> topics, String difficulty,
                               int count, List<String> notes) {
        List<Question> pool = new ArrayList<>(matching(type, "mcq".equals(type) ? topics : List.of()));

        // Prefer the requested difficulty, then the nearest ones.
        Collections.shuffle(pool);
        pool.sort((a, b) -> Integer.compare(distance(a.getDifficulty(), difficulty),
                distance(b.getDifficulty(), difficulty)));
        List<Question> chosen = new ArrayList<>(pool.subList(0, Math.min(count, pool.size())));

        // Ask the AI for more when the bank cannot supply enough at the requested difficulty.
        long exact = chosen.stream().filter(q -> difficulty.equalsIgnoreCase(q.getDifficulty())).count();
        int missing = count - (int) exact;
        if (missing > 0 && AiClient.getInstance().isConfigured()) {
            try {
                List<Question> fresh = generate(type, roleName, topics, difficulty, missing);
                // Fresh questions at the right difficulty replace the furthest-off picks.
                chosen.sort((a, b) -> Integer.compare(distance(b.getDifficulty(), difficulty),
                        distance(a.getDifficulty(), difficulty)));
                for (Question q : fresh) {
                    if (chosen.size() >= count) {
                        chosen.remove(0);
                    }
                    chosen.add(q);
                }
            } catch (AiException e) {
                notes.add("Could not generate new " + type.toUpperCase(Locale.ROOT) + " questions: " + e.getMessage());
            }
        } else if (chosen.size() < count && !AiClient.getInstance().isConfigured()) {
            notes.add("Only " + chosen.size() + " " + type.toUpperCase(Locale.ROOT)
                    + " questions are in the bank. Add an AI key to generate more.");
        }

        Collections.shuffle(chosen);
        return chosen;
    }

    private List<Question> matching(String type, Collection<String> categories) {
        List<Question> result = new ArrayList<>();
        for (Question q : getAll()) {
            if (!type.equalsIgnoreCase(q.getType())) {
                continue;
            }
            if (!categories.isEmpty() && categories.stream().noneMatch(c -> c.equalsIgnoreCase(q.getCategory()))) {
                continue;
            }
            result.add(q);
        }
        return result;
    }

    private static int distance(String have, String want) {
        int a = DIFFICULTIES.indexOf(have);
        int b = DIFFICULTIES.indexOf(want);
        return a < 0 || b < 0 ? 3 : Math.abs(a - b);
    }

    // ------------------------------------------------------------ AI generation

    /** Asks the AI for new questions, stores the valid ones and returns them. Blocks; run off the UI thread. */
    public List<Question> generate(String type, String roleName, List<String> topics, String difficulty,
                                   int count) throws AiException {
        String topicList = topics == null || topics.isEmpty() ? "general computer science" : String.join(", ", topics);
        Set<String> known = new HashSet<>();
        List<String> sample = new ArrayList<>();
        for (Question q : getAll()) {
            known.add(normalise(q.getQuestionText()));
            if (type.equalsIgnoreCase(q.getType()) && sample.size() < 25) {
                sample.add(firstLine(q.getQuestionText()));
            }
        }

        String system = "You write interview questions for a mock interview platform used by computer science "
                + "students preparing for " + roleName + " roles. Questions must be accurate, unambiguous and "
                + "original. Never repeat or paraphrase the questions you are told to avoid.";
        String shape = switch (type) {
            case "mcq" -> "Each item: {\"category\": one of [" + topicList + "], \"difficulty\": \"" + difficulty
                    + "\", \"question\": string, \"options\": exactly 4 strings with no letter prefixes, "
                    + "\"correctIndex\": 0-3, \"explanation\": one or two sentences}. Exactly one option is correct; "
                    + "never use \"all of the above\" or \"none of the above\".";
            case "coding" -> "Each item: {\"category\": a short topic such as Arrays or Strings, \"difficulty\": \""
                    + difficulty + "\", \"title\": short problem name, \"description\": full problem statement with at "
                    + "least two worked examples (Input/Output) and constraints, \"template\": a Java class named "
                    + "Solution containing the method signature and a '// Write your solution here' comment, "
                    + "\"expectedOutput\": the output for the first example}.";
            default -> "Each item: {\"category\": \"Behavioral\" or \"Situational\", \"difficulty\": \"" + difficulty
                    + "\", \"question\": string, \"sampleAnswer\": two sentences describing what a strong answer covers}.";
        };
        String user = "Write " + count + " " + difficulty + " " + typeName(type) + " questions about: " + topicList
                + ".\n" + shape + "\nReturn {\"questions\": [ ... ]}.\n\nDo not repeat these existing questions:\n- "
                + String.join("\n- ", sample);

        JsonNode root = AiClient.getInstance().completeJson(system, user, type.equals("coding") ? 4000 : 3000);
        JsonNode items = root.has("questions") ? root.get("questions") : root;

        List<Question> created = new ArrayList<>();
        for (JsonNode item : items) {
            Question q = parse(type, item, difficulty);
            if (q == null || !known.add(normalise(q.getQuestionText()))) {
                continue;
            }
            try {
                saveGenerated(q);
                created.add(q);
            } catch (SQLException e) {
                System.err.println("Could not store generated question: " + e.getMessage());
            }
        }
        if (created.isEmpty()) {
            throw new AiException("The AI did not return any usable questions.");
        }
        return created;
    }

    private static Question parse(String type, JsonNode n, String fallbackDifficulty) {
        String difficulty = n.path("difficulty").asText(fallbackDifficulty);
        if (!DIFFICULTIES.contains(difficulty)) {
            difficulty = fallbackDifficulty;
        }
        Question q = new Question();
        q.setType(type);
        q.setCategory(n.path("category").asText("General"));
        q.setDifficulty(difficulty);

        switch (type) {
            case "mcq" -> {
                List<String> options = new ArrayList<>();
                n.path("options").forEach(o -> options.add(o.asText().strip()));
                int correct = n.path("correctIndex").asInt(-1);
                String text = n.path("question").asText("").strip();
                if (text.isEmpty() || options.size() != 4 || options.stream().anyMatch(String::isBlank)
                        || correct < 0 || correct > 3) {
                    return null;
                }
                q.setQuestionText(text);
                q.setOptions(options);
                q.setCorrectOptionIndex(correct);
                q.setExplanation(n.path("explanation").asText(""));
            }
            case "coding" -> {
                String title = n.path("title").asText("").strip();
                String description = n.path("description").asText("").strip();
                String template = n.path("template").asText("").strip();
                if (title.isEmpty() || description.isEmpty() || template.isEmpty()) {
                    return null;
                }
                q.setQuestionText(title + "\n\n" + description);
                q.setCodeTemplate(template);
                q.setExpectedOutput(n.path("expectedOutput").asText(null));
            }
            default -> {
                String text = n.path("question").asText("").strip();
                if (text.isEmpty()) {
                    return null;
                }
                q.setQuestionText(text);
                q.setSampleAnswer(n.path("sampleAnswer").asText(""));
            }
        }
        return q;
    }

    private void saveGenerated(Question q) throws SQLException {
        q.setId("AI-" + UUID.randomUUID().toString().substring(0, 8));
        String sql = """
            INSERT INTO questions (id, type, category, difficulty, text, options_json, correct_index,
                                   code_template, expected_output, sample_answer, explanation, source)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, q, "ai");
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------ mapping

    private static void bind(PreparedStatement ps, Question q, String source) throws SQLException {
        ps.setString(1, q.getId());
        ps.setString(2, q.getType());
        ps.setString(3, q.getCategory());
        ps.setString(4, q.getDifficulty());
        ps.setString(5, q.getQuestionText());
        ps.setString(6, q.getOptions() == null ? null : Database.toJson(q.getOptions()));
        ps.setInt(7, q.getCorrectOptionIndex());
        ps.setString(8, q.getCodeTemplate());
        ps.setString(9, q.getExpectedOutput());
        ps.setString(10, q.getSampleAnswer());
        ps.setString(11, q.getExplanation());
        ps.setString(12, source);
    }

    private static Question read(ResultSet rs) throws SQLException {
        Question q = new Question();
        q.setId(rs.getString("id"));
        q.setType(rs.getString("type"));
        q.setCategory(rs.getString("category"));
        q.setDifficulty(rs.getString("difficulty"));
        q.setQuestionText(rs.getString("text"));
        String options = rs.getString("options_json");
        q.setOptions(options == null ? null : Database.stringList(options));
        q.setCorrectOptionIndex(rs.getInt("correct_index"));
        q.setCodeTemplate(rs.getString("code_template"));
        q.setExpectedOutput(rs.getString("expected_output"));
        q.setSampleAnswer(rs.getString("sample_answer"));
        q.setExplanation(rs.getString("explanation"));
        return q;
    }

    private static String typeName(String type) {
        return switch (type) {
            case "mcq" -> "multiple-choice";
            case "coding" -> "coding";
            default -> "HR and behavioural";
        };
    }

    private static String normalise(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    private static String firstLine(String text) {
        int nl = text.indexOf('\n');
        String line = nl > 0 ? text.substring(0, nl) : text;
        return line.length() > 140 ? line.substring(0, 140) : line;
    }
}
