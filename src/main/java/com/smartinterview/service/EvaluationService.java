package com.smartinterview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartinterview.ai.AiClient;
import com.smartinterview.ai.AiException;
import com.smartinterview.model.InterviewResult;
import com.smartinterview.model.Question;
import com.smartinterview.model.QuestionReview;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Scores a finished interview and builds the per-question review behind the PDF report.
 * Multiple-choice is marked locally; coding and behavioural answers are graded by the AI,
 * which also writes model solutions. If the AI is unavailable those rounds are left out of
 * the score and the report says so, rather than inventing a number.
 */
public class EvaluationService {

    /** Called as each stage of evaluation begins (1-4) on the worker thread. */
    public interface Progress {
        void stageStarted(int stage);
    }

    /** Result of an AI code review from the coding round's Run button. */
    public record CodeReview(int passed, int total, List<String> cases, String summary) {
    }

    private static final EvaluationService INSTANCE = new EvaluationService();

    private EvaluationService() {
    }

    public static EvaluationService getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------ whole interview

    public InterviewResult evaluate(InterviewSession s, Progress progress) {
        Map<String, Double> roundScores = new LinkedHashMap<>();
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        List<String> feedbackForSummary = new ArrayList<>();
        List<QuestionReview> reviews = new ArrayList<>();
        int answered = 0;

        // 1. Multiple choice, marked locally.
        progress.stageStarted(1);
        Map<String, int[]> byTopic = new TreeMap<>(); // topic -> {correct, total}
        if (!s.getMcqQuestions().isEmpty()) {
            int correct = 0;
            for (int i = 0; i < s.getMcqQuestions().size(); i++) {
                Question q = s.getMcqQuestions().get(i);
                Integer chosen = s.getMcqAnswers().get(i);
                boolean right = chosen != null && chosen == q.getCorrectOptionIndex();
                if (chosen != null) answered++;
                if (right) correct++;
                int[] t = byTopic.computeIfAbsent(q.getCategory(), k -> new int[2]);
                t[1]++;
                if (right) t[0]++;
                reviews.add(mcqReview(i, q, chosen, right));
            }
            double score = 100.0 * correct / s.getMcqQuestions().size();
            roundScores.put("MCQ", round(score));
            feedbackForSummary.add("MCQ: " + correct + "/" + s.getMcqQuestions().size() + " correct. Per topic: "
                    + describeTopics(byTopic));
            for (Map.Entry<String, int[]> e : byTopic.entrySet()) {
                double pct = 100.0 * e.getValue()[0] / e.getValue()[1];
                if (pct >= 75) strengths.add("Solid grasp of " + e.getKey() + " (" + e.getValue()[0] + "/" + e.getValue()[1] + " correct)");
                else if (pct < 50) weaknesses.add(e.getKey() + " needs work (" + e.getValue()[0] + "/" + e.getValue()[1] + " correct)");
            }
        }

        // 2. Coding, graded by the AI.
        progress.stageStarted(2);
        if (!s.getCodingQuestions().isEmpty()) {
            try {
                RoundGrade grade = gradeCoding(s, reviews);
                answered += grade.answered;
                roundScores.put("Coding", round(grade.score));
                feedbackForSummary.add("Coding: " + grade.summary);
            } catch (AiException e) {
                recommendations.add("The coding round could not be graded: " + e.getMessage());
                addUngraded(s.getCodingQuestions(), "Coding", s.getCodeAnswers(), s.getCodeLanguage(), reviews);
            }
        }

        // 3. Behavioural answers, graded by the AI.
        progress.stageStarted(3);
        if (!s.getHrQuestions().isEmpty()) {
            try {
                RoundGrade grade = gradeHr(s, reviews);
                answered += grade.answered;
                roundScores.put("HR", round(grade.score));
                feedbackForSummary.add("HR: " + grade.summary);
            } catch (AiException e) {
                recommendations.add("The HR round could not be graded: " + e.getMessage());
                addUngraded(s.getHrQuestions(), "HR", s.getHrAnswers(), null, reviews);
            }
        }

        // 4. Written feedback.
        progress.stageStarted(4);
        boolean aiFeedback = false;
        if (AiClient.getInstance().isConfigured() && !roundScores.isEmpty()) {
            try {
                writeFeedback(s, roundScores, feedbackForSummary, strengths, weaknesses, recommendations);
                aiFeedback = true;
            } catch (AiException e) {
                recommendations.add("Personalised AI feedback was unavailable: " + e.getMessage());
            }
        }
        if (!aiFeedback) {
            addLocalRecommendations(byTopic, recommendations);
        }
        if (!AiClient.getInstance().isConfigured()
                && (!s.getCodingQuestions().isEmpty() || !s.getHrQuestions().isEmpty())) {
            recommendations.add("Add an AI API key so coding and HR answers can be graded and model solutions written.");
        }
        recommendations.addAll(s.getNotes());

        InterviewResult result = new InterviewResult();
        result.setInterviewId(InterviewService.newInterviewId());
        result.setRoleName(s.getInterview().getRoleName());
        result.setDate(InterviewService.today());
        result.setTimeTakenMinutes(s.elapsedMinutes());
        result.setQuestionsAnswered(answered);
        result.setRoundScores(roundScores);
        result.setOverallScore(roundScores.isEmpty() ? 0
                : round(roundScores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0)));
        result.setStrengths(strengths);
        result.setWeaknesses(weaknesses);
        result.setRecommendations(recommendations);
        result.setReviews(reviews);

        try {
            InterviewService.getInstance().saveResult(result);
        } catch (SQLException e) {
            result.getRecommendations().add("This result could not be saved to your history: " + e.getMessage());
        }
        return result;
    }

    // ------------------------------------------------------------ MCQ

    private static QuestionReview mcqReview(int index, Question q, Integer chosen, boolean right) {
        String status = chosen == null ? "skipped" : right ? "correct" : "incorrect";
        String answer = chosen == null || q.getOptions() == null || chosen >= q.getOptions().size()
                ? null : q.getOptions().get(chosen);
        return new QuestionReview("MCQ", index + 1, q.getCategory(), q.getDifficulty(), q.getQuestionText(),
                q.getOptions(), chosen, q.getCorrectOptionIndex(), answer, right ? 100.0 : 0.0, status,
                null, q.getExplanation(), null, null, null, null, null, null);
    }

    // ------------------------------------------------------------ coding

    private record RoundGrade(double score, int answered, String summary) {
    }

    private RoundGrade gradeCoding(InterviewSession s, List<QuestionReview> reviews) throws AiException {
        List<Question> qs = s.getCodingQuestions();
        String language = s.getCodeLanguage() == null ? "Java" : s.getCodeLanguage();
        StringBuilder prompt = new StringBuilder();
        int answered = 0;
        for (int i = 0; i < qs.size(); i++) {
            String code = s.getCodeAnswers().getOrDefault(i, "");
            boolean attempted = isAttempt(code, qs.get(i).getCodeTemplate());
            if (attempted) answered++;
            prompt.append("### Problem ").append(i).append("\n").append(qs.get(i).getQuestionText())
                    .append("\n\n### Submission ").append(i).append(" (").append(language).append(")\n")
                    .append(attempted ? code : "(no attempt: the starter template was left unchanged)")
                    .append("\n\n");
        }

        String system = "You are a strict but fair technical interviewer grading coding solutions, and an expert "
                + language + " engineer writing model answers. For each submission, reason about correctness "
                + "against the examples and edge cases, time and space complexity, and code quality. Do not run "
                + "the code; trace it. An empty or unchanged template scores 0. A solution that is correct but "
                + "inefficient loses points for efficiency only.";
        String user = prompt + "Return {\"results\": [{\"problem\": index, \"score\": 0-100, "
                + "\"feedback\": \"3-4 sentences: what is right, specific bugs or missed edge cases\", "
                + "\"bestSolution\": \"complete, correct, optimal " + language + " code with short comments\", "
                + "\"approach\": [\"4-6 short steps explaining how to reach that solution\"], "
                + "\"timeComplexity\": \"e.g. O(n) with a one-line reason\", "
                + "\"spaceComplexity\": \"e.g. O(n) with a one-line reason\", "
                + "\"pitfalls\": [\"2-3 common mistakes on this problem\"]}], "
                + "\"summary\": \"two sentences on the candidate's coding overall\"}. "
                + "One result per problem, including problems with no attempt.";

        JsonNode root;
        try {
            root = AiClient.getInstance().completeJson(system, user, 6000);
        } catch (AiException e) {
            if (answered == 0) {
                // Nothing to grade, so the score is known without the AI; it only supplied the model answers.
                addUngraded(qs, "Coding", s.getCodeAnswers(), language, reviews);
                return new RoundGrade(0, 0, "No solutions were submitted.");
            }
            throw e;
        }

        double total = 0;
        for (int i = 0; i < qs.size(); i++) {
            Question q = qs.get(i);
            String code = s.getCodeAnswers().getOrDefault(i, "");
            boolean attempted = isAttempt(code, q.getCodeTemplate());
            JsonNode r = resultFor(root.path("results"), i);
            // Skipped work scores 0 whatever the model says.
            double score = attempted ? clamp(r.path("score").asDouble(0)) : 0;
            total += score;
            reviews.add(new QuestionReview("Coding", i + 1, q.getCategory(), q.getDifficulty(),
                    q.getQuestionText(), null, null, null, attempted ? code : null, score,
                    attempted ? "graded" : "skipped", text(r, "feedback"), null, text(r, "bestSolution"),
                    language, texts(r.path("approach")), text(r, "timeComplexity"), text(r, "spaceComplexity"),
                    texts(r.path("pitfalls"))));
        }
        return new RoundGrade(total / qs.size(), answered, root.path("summary").asText(""));
    }

    /** Lets the candidate sanity-check their code before submitting. Blocks; run off the UI thread. */
    public CodeReview reviewCode(Question q, String code, String language) throws AiException {
        String system = "You are a code judge. You cannot execute code, so trace it by hand against the "
                + "problem's examples and at least two edge cases of your own. Be honest: if the code does "
                + "not compile or is wrong, say so.";
        String user = "Language: " + language + "\n\nProblem:\n" + q.getQuestionText() + "\n\nSubmission:\n" + code
                + "\n\nReturn {\"compiles\": boolean, \"cases\": [{\"name\": short description, \"passed\": boolean, "
                + "\"note\": one short sentence}], \"summary\": one or two sentences of advice}. Use 3 to 5 cases.";
        JsonNode root = AiClient.getInstance().completeJson(system, user, 1500);

        List<String> lines = new ArrayList<>();
        int passed = 0;
        int total = 0;
        if (!root.path("compiles").asBoolean(true)) {
            lines.add("Compile check: likely to FAIL to compile");
        }
        for (JsonNode c : root.path("cases")) {
            total++;
            boolean ok = c.path("passed").asBoolean(false);
            if (ok) passed++;
            lines.add("Test " + total + ": " + (ok ? "Passed" : "Failed") + " — " + c.path("name").asText("")
                    + (c.path("note").asText("").isEmpty() ? "" : " (" + c.path("note").asText() + ")"));
        }
        return new CodeReview(passed, total, lines, root.path("summary").asText(""));
    }

    // ------------------------------------------------------------ HR

    private RoundGrade gradeHr(InterviewSession s, List<QuestionReview> reviews) throws AiException {
        List<Question> qs = s.getHrQuestions();
        StringBuilder prompt = new StringBuilder();
        int answered = 0;
        for (int i = 0; i < qs.size(); i++) {
            String answer = s.getHrAnswers().getOrDefault(i, "").strip();
            if (!answer.isEmpty()) answered++;
            prompt.append("### Question ").append(i).append("\n").append(qs.get(i).getQuestionText())
                    .append("\n### Answer ").append(i).append("\n")
                    .append(answer.isEmpty() ? "(no answer given)" : answer).append("\n\n");
        }

        String system = "You are an experienced HR interviewer grading a student's answers. Judge structure "
                + "(for example STAR), specificity, ownership, relevance to the question and communication. "
                + "Generic or very short answers score low. A missing answer scores 0.";
        String user = prompt + "Return {\"results\": [{\"question\": index, \"score\": 0-100, "
                + "\"feedback\": \"3 sentences on this specific answer\", "
                + "\"modelAnswer\": \"a strong first-person answer of 120-180 words\", "
                + "\"structure\": [\"3-5 short points on how that answer is built, e.g. Situation: ...\"], "
                + "\"missing\": [\"2-3 things the candidate's answer lacked\"]}], "
                + "\"summary\": \"two sentences on the candidate's communication overall\"}. "
                + "One result per question, including unanswered ones.";

        JsonNode root;
        try {
            root = AiClient.getInstance().completeJson(system, user, 5000);
        } catch (AiException e) {
            if (answered == 0) {
                addUngraded(qs, "HR", s.getHrAnswers(), null, reviews);
                return new RoundGrade(0, 0, "No answers were given.");
            }
            throw e;
        }

        double total = 0;
        for (int i = 0; i < qs.size(); i++) {
            Question q = qs.get(i);
            String answer = s.getHrAnswers().getOrDefault(i, "").strip();
            JsonNode r = resultFor(root.path("results"), i);
            double score = answer.isEmpty() ? 0 : clamp(r.path("score").asDouble(0));
            total += score;
            String model = text(r, "modelAnswer");
            reviews.add(new QuestionReview("HR", i + 1, q.getCategory(), q.getDifficulty(), q.getQuestionText(),
                    null, null, null, answer.isEmpty() ? null : answer, score,
                    answer.isEmpty() ? "skipped" : "graded", text(r, "feedback"), q.getSampleAnswer(),
                    model, null, texts(r.path("structure")), null, null, texts(r.path("missing"))));
        }
        return new RoundGrade(total / qs.size(), answered, root.path("summary").asText(""));
    }

    /** Reviews for answers that could not be AI-graded: the answer is kept, with no score. */
    private static void addUngraded(List<Question> qs, String round, Map<Integer, String> answers,
                                    String language, List<QuestionReview> reviews) {
        reviews.removeIf(r -> r.round().equals(round));
        for (int i = 0; i < qs.size(); i++) {
            Question q = qs.get(i);
            String answer = answers.getOrDefault(i, "");
            boolean attempted = round.equals("Coding") ? isAttempt(answer, q.getCodeTemplate()) : !answer.isBlank();
            reviews.add(new QuestionReview(round, i + 1, q.getCategory(), q.getDifficulty(), q.getQuestionText(),
                    null, null, null, attempted ? answer : null, attempted ? null : 0.0,
                    attempted ? "ungraded" : "skipped", null, q.getSampleAnswer(), null, language,
                    null, null, null, null));
        }
    }

    // ------------------------------------------------------------ feedback

    private void writeFeedback(InterviewSession s, Map<String, Double> roundScores, List<String> findings,
                               List<String> strengths, List<String> weaknesses,
                               List<String> recommendations) throws AiException {
        String system = "You are a career coach giving a student feedback after a mock "
                + s.getInterview().getRoleName() + " interview. Be specific to the evidence provided, "
                + "encouraging but honest. Never mention scores you were not given.";
        String user = "Round scores: " + roundScores + "\nFindings:\n- " + String.join("\n- ", findings)
                + "\n\nReturn {\"strengths\": [2-4 short strings], \"weaknesses\": [2-4 short strings], "
                + "\"recommendations\": [3-4 concrete practice actions]}.";
        JsonNode root = AiClient.getInstance().completeJson(system, user, 1200);

        // The AI's view replaces the locally derived topic lines, which it was already given.
        List<String> aiStrengths = texts(root.path("strengths"));
        List<String> aiWeaknesses = texts(root.path("weaknesses"));
        List<String> aiRecs = texts(root.path("recommendations"));
        if (aiStrengths.isEmpty() && aiWeaknesses.isEmpty() && aiRecs.isEmpty()) {
            throw new AiException("The AI returned no feedback.");
        }
        strengths.clear();
        strengths.addAll(aiStrengths);
        weaknesses.clear();
        weaknesses.addAll(aiWeaknesses);
        recommendations.addAll(0, aiRecs);
    }

    private static void addLocalRecommendations(Map<String, int[]> byTopic, List<String> recommendations) {
        for (Map.Entry<String, int[]> e : byTopic.entrySet()) {
            if (100.0 * e.getValue()[0] / e.getValue()[1] < 60) {
                recommendations.add("Revisit " + e.getKey() + " and retry its questions in the Question Bank.");
            }
        }
        if (recommendations.isEmpty() && !byTopic.isEmpty()) {
            recommendations.add("Try a harder difficulty to keep improving.");
        }
    }

    // ------------------------------------------------------------ helpers

    private static JsonNode resultFor(JsonNode results, int index) {
        for (JsonNode r : results) {
            int id = r.has("problem") ? r.path("problem").asInt(-1) : r.path("question").asInt(-1);
            if (id == index) {
                return r;
            }
        }
        return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    private static String text(JsonNode node, String field) {
        String v = node.path(field).asText("").strip();
        return v.isEmpty() ? null : v;
    }

    private static boolean isAttempt(String code, String template) {
        if (code == null || code.isBlank()) return false;
        return template == null || !code.strip().equals(template.strip());
    }

    private static List<String> texts(JsonNode array) {
        List<String> out = new ArrayList<>();
        array.forEach(n -> {
            String t = n.asText("").strip();
            if (!t.isEmpty()) out.add(t);
        });
        return out;
    }

    private static String describeTopics(Map<String, int[]> byTopic) {
        List<String> parts = new ArrayList<>();
        byTopic.forEach((k, v) -> parts.add(k + " " + v[0] + "/" + v[1]));
        return String.join(", ", parts);
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
