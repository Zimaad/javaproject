package com.smartinterview.service;

import com.smartinterview.model.Interview;
import com.smartinterview.model.Question;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything about the interview in progress: the questions that were picked
 * and the answers given so far. The round screens fill it in; the evaluation
 * screen reads it.
 */
public class InterviewSession {

    private static InterviewSession current;

    private final Interview interview;
    private final long startedAt = System.currentTimeMillis();

    private List<Question> mcqQuestions = new ArrayList<>();
    private List<Question> codingQuestions = new ArrayList<>();
    private List<Question> hrQuestions = new ArrayList<>();

    private Map<Integer, Integer> mcqAnswers = new HashMap<>();
    private Map<Integer, String> codeAnswers = new HashMap<>();
    private String codeLanguage = "Java";
    private Map<Integer, String> hrAnswers = new HashMap<>();

    /** Problems found while preparing the interview, shown to the user in the report. */
    private final List<String> notes = new ArrayList<>();

    public InterviewSession(Interview interview) {
        this.interview = interview;
    }

    public static InterviewSession current() {
        return current;
    }

    public static void start(InterviewSession session) {
        current = session;
    }

    public static void clear() {
        current = null;
    }

    public Interview getInterview() { return interview; }
    public long getStartedAt() { return startedAt; }

    public int elapsedMinutes() {
        return (int) Math.max(1, Math.round((System.currentTimeMillis() - startedAt) / 60000.0));
    }

    public List<Question> getMcqQuestions() { return mcqQuestions; }
    public void setMcqQuestions(List<Question> q) { this.mcqQuestions = q; }

    public List<Question> getCodingQuestions() { return codingQuestions; }
    public void setCodingQuestions(List<Question> q) { this.codingQuestions = q; }

    public List<Question> getHrQuestions() { return hrQuestions; }
    public void setHrQuestions(List<Question> q) { this.hrQuestions = q; }

    public Map<Integer, Integer> getMcqAnswers() { return mcqAnswers; }
    public void setMcqAnswers(Map<Integer, Integer> a) { this.mcqAnswers = new HashMap<>(a); }

    public Map<Integer, String> getCodeAnswers() { return codeAnswers; }
    public void setCodeAnswers(Map<Integer, String> a) { this.codeAnswers = new HashMap<>(a); }

    public String getCodeLanguage() { return codeLanguage; }
    public void setCodeLanguage(String language) { this.codeLanguage = language; }

    public Map<Integer, String> getHrAnswers() { return hrAnswers; }
    public void setHrAnswers(Map<Integer, String> a) { this.hrAnswers = new HashMap<>(a); }

    public List<String> getNotes() { return notes; }
}
