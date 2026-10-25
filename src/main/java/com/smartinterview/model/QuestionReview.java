package com.smartinterview.model;

import java.util.List;

/**
 * Everything about one question in a finished interview: what was asked, what the
 * candidate answered, how it was marked, and the model solution. Stored with the
 * interview so the full report can be rebuilt later.
 *
 * @param round        "MCQ", "Coding" or "HR"
 * @param status       MCQ: correct / incorrect / skipped. Coding and HR: graded / skipped / ungraded
 * @param score        0-100, or null when the answer could not be graded
 * @param explanation  MCQ: why the right option is right
 * @param bestSolution Coding: model code. HR: model answer. Null when unavailable.
 * @param breakdown    Coding: approach, step by step. HR: how a strong answer is structured.
 * @param pitfalls     Coding: common mistakes. HR: what the candidate's answer was missing.
 */
public record QuestionReview(
        String round,
        int number,
        String category,
        String difficulty,
        String question,
        List<String> options,
        Integer chosenIndex,
        Integer correctIndex,
        String userAnswer,
        Double score,
        String status,
        String feedback,
        String explanation,
        String bestSolution,
        String solutionLanguage,
        List<String> breakdown,
        String timeComplexity,
        String spaceComplexity,
        List<String> pitfalls) {
}
