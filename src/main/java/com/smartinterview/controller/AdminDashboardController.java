package com.smartinterview.controller;

import com.smartinterview.model.ActivityEntry;
import com.smartinterview.service.InterviewService;
import com.smartinterview.service.QuestionService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Map;

public class AdminDashboardController {

    @FXML
    private Label totalUsers;
    @FXML
    private Label totalInterviews;
    @FXML
    private Label totalQuestions;
    @FXML
    private Label avgScore;
    @FXML
    private BarChart<String, Number> activityChart;
    @FXML
    private VBox activityList;

    @FXML
    public void initialize() {
        InterviewService interviews = InterviewService.getInstance();
        totalUsers.setText(String.valueOf(interviews.getTotalStudents()));
        totalInterviews.setText(String.valueOf(interviews.getTotalInterviews()));
        totalQuestions.setText(String.valueOf(QuestionService.getInstance().count()));
        avgScore.setText(interviews.getGlobalAverageScore() + "%");

        loadChart(interviews.getInterviewsPerDay());
        buildActivityFeed(interviews.getRecentActivity(6));
    }

    private void loadChart(Map<String, Integer> perDay) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Interviews");
        perDay.forEach((day, count) -> series.getData().add(new XYChart.Data<>(day, count)));
        activityChart.getData().add(series);
    }

    /**
     * Builds the feed in code so each row gets a real icon tile instead of
     * a coloured dot, and so the last row can drop its divider.
     */
    private void buildActivityFeed(List<ActivityEntry> entries) {
        activityList.getChildren().clear();

        if (entries.isEmpty()) {
            Label none = new Label("No activity yet.");
            none.getStyleClass().add("activity-time");
            activityList.getChildren().add(none);
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            ActivityEntry a = entries.get(i);
            boolean interview = a.kind().equals("interview");

            FontIcon icon = new FontIcon(interview ? "mdi2c-check-circle-outline" : "mdi2a-account-outline");
            icon.getStyleClass().addAll("icon-sm", "icon-muted");
            StackPane tile = new StackPane(icon);
            tile.getStyleClass().addAll("icon-tile", "icon-tile-sm", interview ? "tile-success" : "tile-info");

            Label text = new Label(a.text());
            text.getStyleClass().add("activity-text");
            text.setWrapText(true);

            String when = InterviewService.timeAgo(a.at());
            Label time = new Label(a.detail().isEmpty() ? when : when + " · " + a.detail());
            time.getStyleClass().add("activity-time");

            VBox copy = new VBox(2, text, time);
            HBox.setHgrow(copy, Priority.ALWAYS);

            HBox row = new HBox(12, tile, copy);
            row.getStyleClass().add("activity-item");
            row.setAlignment(Pos.CENTER_LEFT);
            if (i == entries.size() - 1) {
                row.getStyleClass().add("activity-item-last");
            }

            activityList.getChildren().add(row);
        }
    }
}
