package com.smartinterview.model;

import java.util.List;
import java.util.Map;

/**
 * Represents the result of a completed interview.
 */
public class InterviewResult {
    private String interviewId;
    private String roleName;
    private double overallScore;
    private Map<String, Double> roundScores; // "MCQ" -> 85.0, "Coding" -> 78.0, "HR" -> 80.0
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendations;
    private String date;
    private int timeTakenMinutes;

    public InterviewResult() {}

    // Getters and Setters
    public String getInterviewId() { return interviewId; }
    public void setInterviewId(String id) { this.interviewId = id; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String name) { this.roleName = name; }

    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double score) { this.overallScore = score; }

    public Map<String, Double> getRoundScores() { return roundScores; }
    public void setRoundScores(Map<String, Double> scores) { this.roundScores = scores; }

    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }

    public List<String> getWeaknesses() { return weaknesses; }
    public void setWeaknesses(List<String> weaknesses) { this.weaknesses = weaknesses; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recs) { this.recommendations = recs; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getTimeTakenMinutes() { return timeTakenMinutes; }
    public void setTimeTakenMinutes(int mins) { this.timeTakenMinutes = mins; }
}
