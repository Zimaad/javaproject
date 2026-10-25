package com.smartinterview.model;

/** A user's saved preferences. */
public class UserSettings {
    private String defaultDifficulty = "Medium";
    private String defaultDuration = "30 minutes";
    private boolean emailNotifications = true;
    private boolean practiceReminders = false;

    public String getDefaultDifficulty() { return defaultDifficulty; }
    public void setDefaultDifficulty(String v) { this.defaultDifficulty = v; }

    public String getDefaultDuration() { return defaultDuration; }
    public void setDefaultDuration(String v) { this.defaultDuration = v; }

    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean v) { this.emailNotifications = v; }

    public boolean isPracticeReminders() { return practiceReminders; }
    public void setPracticeReminders(boolean v) { this.practiceReminders = v; }
}
