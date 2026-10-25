package com.smartinterview.service;

import com.smartinterview.model.InterviewResult;
import com.smartinterview.model.QuestionReview;
import com.smartinterview.service.PdfCanvas.Rgb;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Builds the full interview report as a PDF: overall result, then a detailed
 * review of every round with the candidate's answers and model solutions.
 */
public final class ReportPdf {

    private ReportPdf() {}

    public static void write(InterviewResult r, Path file) throws IOException {
        try (PdfCanvas pdf = new PdfCanvas("SmartInterview  |  " + r.getRoleName() + "  |  " + r.getDate())) {
            summary(pdf, r);

            List<QuestionReview> reviews = r.getReviews() == null ? List.of() : r.getReviews();
            if (reviews.isEmpty()) {
                pdf.heading("Question review");
                pdf.muted("A question-by-question review is not available for this interview, because it was "
                        + "taken before detailed reports were added.");
            } else {
                revisitList(pdf, reviews);
                for (String round : List.of("MCQ", "Coding", "HR")) {
                    List<QuestionReview> inRound = reviews.stream().filter(q -> q.round().equals(round)).toList();
                    if (inRound.isEmpty()) continue;
                    pdf.pageBreak();
                    switch (round) {
                        case "MCQ" -> mcqRound(pdf, r, inRound);
                        case "Coding" -> codingRound(pdf, r, inRound);
                        default -> hrRound(pdf, r, inRound);
                    }
                }
            }
            pdf.save(file);
        }
    }

    // ------------------------------------------------------------ summary

    private static void summary(PdfCanvas pdf, InterviewResult r) throws IOException {
        pdf.title(r.getRoleName() + " — Interview Report");
        pdf.muted(r.getDate() + "   |   " + r.getTimeTakenMinutes() + " min   |   "
                + r.getQuestionsAnswered() + " questions answered");
        pdf.space(8);

        int score = (int) Math.round(r.getOverallScore());
        pdf.bigScore(score + "%", verdict(score), colourFor(score));
        pdf.space(6);

        if (r.getRoundScores() != null && !r.getRoundScores().isEmpty()) {
            pdf.heading("Round scores");
            r.getRoundScores().forEach((name, value) -> {
                try {
                    pdf.bar(roundTitle(name), value, colourFor(value));
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        }

        section(pdf, "Strengths", r.getStrengths(), "•");
        section(pdf, "Areas to improve", r.getWeaknesses(), "•");
        section(pdf, "Recommended next steps", r.getRecommendations(), "→");
    }

    private static void section(PdfCanvas pdf, String title, List<String> items, String marker) throws IOException {
        if (items == null || items.isEmpty()) return;
        pdf.heading(title);
        for (String item : items) {
            pdf.bullet(marker, item);
        }
    }

    /** Every question that was wrong, skipped or scored under 60%, so the reader knows where to look. */
    private static void revisitList(PdfCanvas pdf, List<QuestionReview> reviews) throws IOException {
        List<QuestionReview> weak = reviews.stream().filter(q -> needsWork(q)).toList();
        pdf.heading("Questions to revisit");
        if (weak.isEmpty()) {
            pdf.body("Nothing to revisit — every question was answered well.");
            return;
        }
        pdf.muted("The detailed sections that follow include the full solution for each of these.");
        for (QuestionReview q : weak) {
            String what = switch (q.status()) {
                case "incorrect" -> "answered incorrectly";
                case "skipped" -> "not answered";
                case "ungraded" -> "could not be graded";
                default -> "scored " + Math.round(q.score()) + "%";
            };
            pdf.bullet(roundTitle(q.round()) + " Q" + q.number() + "  (" + q.category() + ", " + q.difficulty()
                    + ")  —  " + what);
        }
    }

    private static boolean needsWork(QuestionReview q) {
        return switch (q.status()) {
            case "incorrect", "skipped", "ungraded" -> true;
            case "graded" -> q.score() != null && q.score() < 60;
            default -> false;
        };
    }

    // ------------------------------------------------------------ MCQ

    private static void mcqRound(PdfCanvas pdf, InterviewResult r, List<QuestionReview> qs) throws IOException {
        pdf.title("Round: Multiple choice");
        long correct = qs.stream().filter(q -> q.status().equals("correct")).count();
        pdf.muted(correct + " of " + qs.size() + " correct   |   " + roundScore(r, "MCQ"));
        pdf.space(4);

        for (QuestionReview q : qs) {
            pdf.rule();
            pdf.subheading("Question " + q.number() + "   (" + q.category() + ", " + q.difficulty() + ")");
            pdf.badge(q.status(), statusColour(q.status()));
            pdf.body(q.question());
            pdf.space(2);

            List<String> options = q.options() == null ? List.of() : q.options();
            for (int i = 0; i < options.size(); i++) {
                boolean isCorrect = q.correctIndex() != null && q.correctIndex() == i;
                boolean isChosen = q.chosenIndex() != null && q.chosenIndex() == i;
                String note = isCorrect && isChosen ? "   (your answer — correct)"
                        : isCorrect ? "   (correct answer)"
                        : isChosen ? "   (your answer)" : "";
                String line = (char) ('A' + i) + ".  " + options.get(i) + note;
                if (isCorrect) {
                    pdf.body(line, PdfCanvas.SUCCESS, true);
                } else if (isChosen) {
                    pdf.body(line, PdfCanvas.DANGER, true);
                } else {
                    pdf.body(line);
                }
            }
            if (q.chosenIndex() == null) {
                pdf.muted("You did not answer this question.");
            }
            if (q.explanation() != null && !q.explanation().isBlank()) {
                pdf.label("Why this is the answer");
                pdf.body(q.explanation());
            }
        }
    }

    // ------------------------------------------------------------ coding

    private static void codingRound(PdfCanvas pdf, InterviewResult r, List<QuestionReview> qs) throws IOException {
        pdf.title("Round: Coding");
        pdf.muted(roundScore(r, "Coding"));
        pdf.space(4);

        for (QuestionReview q : qs) {
            pdf.rule();
            pdf.subheading("Problem " + q.number() + "   (" + q.category() + ", " + q.difficulty() + ")");
            pdf.badge(scoreBadge(q), statusColour(q.status(), q.score()));

            pdf.label("Problem");
            pdf.body(q.question());

            pdf.label("Your solution" + (q.solutionLanguage() == null ? "" : " (" + q.solutionLanguage() + ")"));
            if (q.userAnswer() == null) {
                pdf.muted("No solution was submitted.");
            } else {
                pdf.code(q.userAnswer());
            }

            if (q.feedback() != null) {
                pdf.label("Feedback on your solution");
                pdf.body(q.feedback());
            }

            if (q.bestSolution() != null) {
                pdf.label("Best solution" + (q.solutionLanguage() == null ? "" : " (" + q.solutionLanguage() + ")"));
                pdf.code(stripFences(q.bestSolution()));
            } else {
                pdf.muted("A model solution is unavailable: it needs a working AI connection when the interview is graded.");
            }

            numbered(pdf, "How to get there", q.breakdown());
            if (q.timeComplexity() != null || q.spaceComplexity() != null) {
                pdf.label("Complexity");
                if (q.timeComplexity() != null) pdf.bullet("Time: " + q.timeComplexity());
                if (q.spaceComplexity() != null) pdf.bullet("Space: " + q.spaceComplexity());
            }
            bullets(pdf, "Common mistakes", q.pitfalls());
        }
    }

    // ------------------------------------------------------------ HR

    private static void hrRound(PdfCanvas pdf, InterviewResult r, List<QuestionReview> qs) throws IOException {
        pdf.title("Round: HR & behavioural");
        pdf.muted(roundScore(r, "HR"));
        pdf.space(4);

        for (QuestionReview q : qs) {
            pdf.rule();
            pdf.subheading("Question " + q.number() + "   (" + q.category() + ", " + q.difficulty() + ")");
            pdf.badge(scoreBadge(q), statusColour(q.status(), q.score()));
            pdf.body(q.question());

            pdf.label("Your answer");
            if (q.userAnswer() == null) {
                pdf.muted("No answer was given.");
            } else {
                pdf.body(q.userAnswer());
            }

            if (q.feedback() != null) {
                pdf.label("Feedback");
                pdf.body(q.feedback());
            }
            bullets(pdf, "What your answer was missing", q.pitfalls());

            if (q.bestSolution() != null) {
                pdf.label("What a strong answer looks like");
                pdf.body(q.bestSolution());
            }
            bullets(pdf, "How that answer is built", q.breakdown());

            if (q.explanation() != null && !q.explanation().isBlank()) {
                pdf.label("Interviewer's guidance");
                pdf.body(q.explanation());
            }
        }
    }

    // ------------------------------------------------------------ helpers

    private static void numbered(PdfCanvas pdf, String title, List<String> items) throws IOException {
        if (items == null || items.isEmpty()) return;
        pdf.label(title);
        for (int i = 0; i < items.size(); i++) {
            pdf.bullet((i + 1) + ".", items.get(i));
        }
    }

    private static void bullets(PdfCanvas pdf, String title, List<String> items) throws IOException {
        if (items == null || items.isEmpty()) return;
        pdf.label(title);
        for (String item : items) {
            pdf.bullet(item);
        }
    }

    private static String scoreBadge(QuestionReview q) {
        return switch (q.status()) {
            case "skipped" -> "Not attempted — 0%";
            case "ungraded" -> "Could not be graded";
            default -> "Score " + Math.round(q.score()) + "%";
        };
    }

    private static Rgb statusColour(String status) {
        return switch (status) {
            case "correct" -> PdfCanvas.SUCCESS;
            case "incorrect" -> PdfCanvas.DANGER;
            default -> PdfCanvas.MUTED;
        };
    }

    private static Rgb statusColour(String status, Double score) {
        if (score == null || status.equals("ungraded")) return PdfCanvas.MUTED;
        return colourFor(score);
    }

    private static Rgb colourFor(double score) {
        return score >= 75 ? PdfCanvas.SUCCESS : score >= 50 ? PdfCanvas.WARNING : PdfCanvas.DANGER;
    }

    private static String verdict(int score) {
        return score >= 75 ? "Strong result" : score >= 50 ? "Room to improve" : "Needs practice";
    }

    private static String roundTitle(String round) {
        return switch (round) {
            case "MCQ" -> "MCQ";
            case "HR" -> "HR";
            default -> round;
        };
    }

    private static String roundScore(InterviewResult r, String round) {
        Double v = r.getRoundScores() == null ? null : r.getRoundScores().get(round);
        return v == null ? "Round score unavailable" : "Round score: " + Math.round(v) + "%";
    }

    /** Models sometimes wrap code in markdown fences even when asked not to. */
    private static String stripFences(String code) {
        String s = code.strip();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            s = nl < 0 ? "" : s.substring(nl + 1);
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
        }
        return s.isBlank() ? code : s.strip();
    }
}
