package com.smartinterview.controller;

import com.smartinterview.model.InterviewResult;
import com.smartinterview.model.PerformanceData;
import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.AuthService;
import com.smartinterview.service.InterviewService;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;

import java.util.List;

public class DashboardController {

    @FXML
    private Label greetingLabel;
    @FXML
    private Label interviewsCount;
    @FXML
    private Label avgScore;
    @FXML
    private Label bestScore;
    @FXML
    private Label questionsCount;
    @FXML
    private LineChart<String, Number> performanceChart;
    @FXML
    private TableView<InterviewResult> recentTable;
    @FXML
    private TableColumn<InterviewResult, String> colRole;
    @FXML
    private TableColumn<InterviewResult, String> colScore;
    @FXML
    private TableColumn<InterviewResult, String> colDate;
    @FXML
    private TableColumn<InterviewResult, String> colDuration;

    private final InterviewService dataService = InterviewService.getInstance();

    @FXML
    public void initialize() {
        // Update greeting
        String userName = "there";
        if (AuthService.getInstance().getCurrentUser() != null) {
            String fullName = AuthService.getInstance().getCurrentUser().getFullName();
            userName = fullName.split(" ")[0];
        }

        int hour = java.time.LocalTime.now().getHour();
        String greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
        greetingLabel.setText(greeting + ", " + userName);

        // Stats
        interviewsCount.setText(String.valueOf(dataService.getInterviewsCompleted()));
        avgScore.setText((int) dataService.getAverageScore() + "%");
        bestScore.setText((int) dataService.getBestScore() + "%");
        questionsCount.setText(String.valueOf(dataService.getQuestionsAnswered()));

        // Performance chart
        loadPerformanceChart();

        // Recent interviews table
        loadRecentInterviews();
    }

    private void loadPerformanceChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Score");

        List<PerformanceData> data = dataService.getPerformanceHistory(6);
        for (PerformanceData pd : data) {
            series.getData().add(new XYChart.Data<>(pd.getLabel(), pd.getScore()));
        }

        performanceChart.getData().clear();
        performanceChart.getData().add(series);
        performanceChart.setLegendVisible(false);
    }

    private void loadRecentInterviews() {
        colRole.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRoleName()));
        colScore.setCellValueFactory(cell -> new SimpleStringProperty((int) cell.getValue().getOverallScore() + "%"));
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDate()));
        colDuration
                .setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTimeTakenMinutes() + " min"));

        recentTable.getItems().addAll(dataService.getRecent(5));
        if (recentTable.getItems().isEmpty()) {
            recentTable.setPlaceholder(new Label("No interviews yet — start one from the Interview tab."));
        }
    }

    // Navigation handlers
    @FXML
    private void onNavDashboard() {
        NavigationManager.getInstance().navigateTo("dashboard");
    }

    @FXML
    private void onNavInterview() {
        NavigationManager.getInstance().navigateTo("interview-select");
    }

    @FXML
    private void onNavQuestionBank() {
        NavigationManager.getInstance().navigateTo("question-bank");
    }

    @FXML
    private void onNavPerformance() {
        NavigationManager.getInstance().navigateTo("performance-report");
    }

    @FXML
    private void onNavHistory() {
        NavigationManager.getInstance().navigateTo("interview-history");
    }

    @FXML
    private void onNavProfile() {
        NavigationManager.getInstance().navigateTo("profile");
    }

    @FXML
    private void onNavSettings() {
        NavigationManager.getInstance().navigateTo("settings");
    }

    @FXML
    private void onLogout() {
        AuthService.getInstance().logout();
        NavigationManager.getInstance().navigateTo("landing");
    }
}
