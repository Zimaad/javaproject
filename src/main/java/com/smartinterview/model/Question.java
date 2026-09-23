package com.smartinterview.model;

import java.util.List;

/**
 * Represents a question (MCQ, Coding, or HR type).
 */
public class Question {
    private String id;
    private String type; // "mcq", "coding", "hr"
    private String category; // "Java", "DSA", "SQL", "HR", etc.
    private String difficulty; // "Easy", "Medium", "Hard"
    private String questionText;
    private List<String> options; // For MCQ
    private int correctOptionIndex; // For MCQ (0-based)
    private String codeTemplate; // For coding questions
    private String expectedOutput; // For coding questions
    private String sampleAnswer; // For HR questions
    private String explanation;
    private boolean bookmarked;

    public Question() {}

    // MCQ constructor
    public Question(String id, String category, String difficulty, String questionText,
                    List<String> options, int correctOptionIndex, String explanation) {
        this.id = id;
        this.type = "mcq";
        this.category = category;
        this.difficulty = difficulty;
        this.questionText = questionText;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.explanation = explanation;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String text) { this.questionText = text; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public int getCorrectOptionIndex() { return correctOptionIndex; }
    public void setCorrectOptionIndex(int idx) { this.correctOptionIndex = idx; }

    public String getCodeTemplate() { return codeTemplate; }
    public void setCodeTemplate(String template) { this.codeTemplate = template; }

    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String output) { this.expectedOutput = output; }

    public String getSampleAnswer() { return sampleAnswer; }
    public void setSampleAnswer(String answer) { this.sampleAnswer = answer; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public boolean isBookmarked() { return bookmarked; }
    public void setBookmarked(boolean bookmarked) { this.bookmarked = bookmarked; }
}
