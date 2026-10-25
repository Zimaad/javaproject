package com.smartinterview.controller;

import com.smartinterview.ai.AiClient;
import com.smartinterview.model.Interview;
import com.smartinterview.model.UserSettings;
import com.smartinterview.service.InterviewService;
import com.smartinterview.service.InterviewSession;
import javafx.concurrent.Task;
import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import java.util.ArrayList;
import java.util.List;

public class InterviewConfigController implements NavigationManager.DataReceiver {

    @FXML private Label roleTitleLabel;
    
    // Toggles
    @FXML private ToggleButton btnEasy, btnMedium, btnHard;
    @FXML private CheckBox chkMcq, chkCoding, chkHr;
    @FXML private ToggleButton btn15m, btn30m, btn45m;
    @FXML private ToggleButton btn5q, btn10q, btn15q;
    
    // Summary
    @FXML private Label summaryRole;
    @FXML private Label summaryRounds;
    @FXML private Label summaryDifficulty;
    @FXML private Label summaryDuration;
    @FXML private Label summaryQuestions;
    @FXML private Label errorLabel;
    @FXML private Button btnStart;
    @FXML private HBox errorBox;

    private ToggleGroup difficultyGroup;
    private ToggleGroup durationGroup;
    private ToggleGroup questionsGroup;
    
    private Interview currentInterview;

    @FXML
    public void initialize() {

        // Setup toggle groups
        difficultyGroup = new ToggleGroup();
        btnEasy.setToggleGroup(difficultyGroup);
        btnMedium.setToggleGroup(difficultyGroup);
        btnHard.setToggleGroup(difficultyGroup);

        durationGroup = new ToggleGroup();
        btn15m.setToggleGroup(durationGroup);
        btn30m.setToggleGroup(durationGroup);
        btn45m.setToggleGroup(durationGroup);

        questionsGroup = new ToggleGroup();
        btn5q.setToggleGroup(questionsGroup);
        btn10q.setToggleGroup(questionsGroup);
        btn15q.setToggleGroup(questionsGroup);
        
        onUpdateSummary();
    }

    @Override
    public void receiveData(Object data) {
        if (data instanceof Interview) {
            currentInterview = (Interview) data;
            roleTitleLabel.setText(currentInterview.getRoleName());
            summaryRole.setText(currentInterview.getRoleName());
            
            // The user's saved preferences win over the role's own defaults.
            UserSettings prefs = AuthService.getInstance().getSettings();
            String difficulty = prefs.getDefaultDifficulty();
            if (difficulty.equals("Hard")) btnHard.setSelected(true);
            else if (difficulty.equals("Easy")) btnEasy.setSelected(true);
            else btnMedium.setSelected(true);

            if (prefs.getDefaultDuration().startsWith("15")) btn15m.setSelected(true);
            else if (prefs.getDefaultDuration().startsWith("45")) btn45m.setSelected(true);
            else btn30m.setSelected(true);

            onUpdateSummary();
        }
    }

    @FXML
    private void onUpdateSummary() {
        // Validate toggles (prevent unselecting all)
        if (difficultyGroup.getSelectedToggle() == null) btnMedium.setSelected(true);
        if (durationGroup.getSelectedToggle() == null) btn30m.setSelected(true);
        if (questionsGroup.getSelectedToggle() == null) btn10q.setSelected(true);

        // Update Difficulty
        ToggleButton diff = (ToggleButton) difficultyGroup.getSelectedToggle();
        summaryDifficulty.setText(diff.getText());
        summaryDifficulty.getStyleClass().removeAll("text-success", "text-warning", "text-danger");
        summaryDifficulty.getStyleClass().add(switch (diff.getText()) {
            case "Easy" -> "text-success";
            case "Medium" -> "text-warning";
            default -> "text-danger";
        });

        // Update Duration & Questions
        summaryDuration.setText(((ToggleButton) durationGroup.getSelectedToggle()).getText());
        summaryQuestions.setText(((ToggleButton) questionsGroup.getSelectedToggle()).getText());

        // Update Rounds
        List<String> rounds = new ArrayList<>();
        if (chkMcq.isSelected()) rounds.add("MCQ");
        if (chkCoding.isSelected()) rounds.add("Coding");
        if (chkHr.isSelected()) rounds.add("HR");
        
        if (rounds.isEmpty()) {
            summaryRounds.setText("None selected");
            summaryRounds.getStyleClass().removeAll("text-accent");
            summaryRounds.getStyleClass().add("text-danger");
        } else {
            summaryRounds.setText(String.join(" → ", rounds));
            summaryRounds.getStyleClass().removeAll("text-danger");
            summaryRounds.getStyleClass().add("text-accent");
        }
        
        errorBox.setVisible(false);
        errorBox.setManaged(false);
    }

    @FXML
    private void onStartInterview() {
        List<String> rounds = new ArrayList<>();
        if (chkMcq.isSelected()) rounds.add("MCQ");
        if (chkCoding.isSelected()) rounds.add("Coding");
        if (chkHr.isSelected()) rounds.add("HR");

        if (rounds.isEmpty()) {
            errorLabel.setText("Select at least one round to include.");
            errorBox.setVisible(true);
            errorBox.setManaged(true);
            return;
        }

        if (currentInterview == null) {
            return;
        }
        currentInterview.setRounds(rounds);
        currentInterview.setDifficulty(((ToggleButton) difficultyGroup.getSelectedToggle()).getText());
        currentInterview.setDurationMinutes(Integer.parseInt(((ToggleButton) durationGroup.getSelectedToggle()).getText().split(" ")[0]));
        currentInterview.setQuestionCount(Integer.parseInt(((ToggleButton) questionsGroup.getSelectedToggle()).getText()));

        // Picking questions can involve an AI call, so it runs off the UI thread.
        btnStart.setDisable(true);
        btnStart.setText(AiClient.getInstance().isConfigured() ? "Preparing your questions…" : "Starting…");

        Task<InterviewSession> prepare = new Task<>() {
            @Override
            protected InterviewSession call() {
                return InterviewService.getInstance().createSession(currentInterview);
            }
        };
        prepare.setOnSucceeded(e -> {
            InterviewSession session = prepare.getValue();
            if (!hasQuestions(session)) {
                failStart("No questions are available for the selected rounds.");
                return;
            }
            InterviewSession.start(session);
            String first = rounds.get(0);
            NavigationManager.getInstance().navigateTo(
                first.equals("MCQ") ? "mcq-round" : first.equals("Coding") ? "coding-round" : "hr-round",
                currentInterview);
        });
        prepare.setOnFailed(e -> failStart("Could not prepare the interview: " + prepare.getException().getMessage()));

        Thread worker = new Thread(prepare, "prepare-interview");
        worker.setDaemon(true);
        worker.start();
    }

    private boolean hasQuestions(InterviewSession s) {
        List<String> rounds = currentInterview.getRounds();
        return (!rounds.contains("MCQ") || !s.getMcqQuestions().isEmpty())
            && (!rounds.contains("Coding") || !s.getCodingQuestions().isEmpty())
            && (!rounds.contains("HR") || !s.getHrQuestions().isEmpty());
    }

    private void failStart(String message) {
        btnStart.setDisable(false);
        btnStart.setText("Start interview");
        errorLabel.setText(message);
        errorBox.setVisible(true);
        errorBox.setManaged(true);
    }

    // Sidebar
    @FXML private void onNavDashboard() { NavigationManager.getInstance().navigateTo("dashboard"); }
    @FXML private void onNavInterview() { NavigationManager.getInstance().navigateTo("interview-select"); }
    @FXML private void onNavQuestionBank() { NavigationManager.getInstance().navigateTo("question-bank"); }
    @FXML private void onNavPerformance() { NavigationManager.getInstance().navigateTo("performance-report"); }
    @FXML private void onNavHistory() { NavigationManager.getInstance().navigateTo("interview-history"); }
    @FXML private void onNavProfile() { NavigationManager.getInstance().navigateTo("profile"); }
    @FXML private void onNavSettings() { NavigationManager.getInstance().navigateTo("settings"); }
    @FXML private void onLogout() {
        AuthService.getInstance().logout();
        NavigationManager.getInstance().navigateTo("landing");
    }
}
