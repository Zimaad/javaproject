package com.smartinterview.model;

import java.util.List;

/**
 * Represents an interview configuration and metadata.
 */
public class Interview {
    private String id;
    private String roleName;
    private String roleDescription;
    private String roleIcon;
    private String difficulty;
    private int durationMinutes;
    private int questionCount;
    private List<String> topics;
    private List<String> rounds;
    private String status; // "pending", "in-progress", "completed"
    private double score;
    private String date;

    public Interview() {}

    public Interview(String id, String roleName, String roleDescription, String difficulty,
                     int durationMinutes, int questionCount, List<String> topics) {
        this.id = id;
        this.roleName = roleName;
        this.roleDescription = roleDescription;
        this.difficulty = difficulty;
        this.durationMinutes = durationMinutes;
        this.questionCount = questionCount;
        this.topics = topics;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getRoleDescription() { return roleDescription; }
    public void setRoleDescription(String desc) { this.roleDescription = desc; }

    public String getRoleIcon() { return roleIcon; }
    public void setRoleIcon(String roleIcon) { this.roleIcon = roleIcon; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int mins) { this.durationMinutes = mins; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int count) { this.questionCount = count; }

    public List<String> getTopics() { return topics; }
    public void setTopics(List<String> topics) { this.topics = topics; }

    public List<String> getRounds() { return rounds; }
    public void setRounds(List<String> rounds) { this.rounds = rounds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
