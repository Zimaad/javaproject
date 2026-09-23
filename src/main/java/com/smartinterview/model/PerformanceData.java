package com.smartinterview.model;

/**
 * Represents a single data point for performance charts.
 */
public class PerformanceData {
    private String label; // e.g. "Interview 1"
    private double score;
    private String date;

    public PerformanceData() {}

    public PerformanceData(String label, double score, String date) {
        this.label = label;
        this.score = score;
        this.date = date;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
