package com.smartinterview.controller;

import com.smartinterview.service.MockDataService;
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

public class AdminDashboardController {

    /** One row of the activity feed: icon, tint, description, timestamp. */
    private record Activity(String icon, String tint, String text, String time) {
    }

    private static final List<Activity> RECENT_ACTIVITY = List.of(
            new Activity("mdi2c-check-circle-outline", "tile-success", "Zimaad Azhari completed Java Developer mock",
                    "2 min ago · scored 85%"),
            new Activity("mdi2a-account-outline", "tile-info", "New student Priya signed up", "15 min ago"),
            new Activity("mdi2c-comment-question-outline", "tile-accent", "Rahul submitted a feedback report",
                    "1 hour ago"),
            new Activity("mdi2c-check-circle-outline", "tile-success", "Ananya completed Software Engineer mock",
                    "2 hours ago · scored 92%"));

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
        MockDataService ds = MockDataService.getInstance();
        totalUsers.setText(String.valueOf(ds.getTotalUsers()));
        totalInterviews.setText(String.valueOf(ds.getTotalInterviews()));
        totalQuestions.setText(String.valueOf(ds.getQuestionsInBank()));
        avgScore.setText(ds.getGlobalAverageScore() + "%");

        loadChart();
        buildActivityFeed();
    }

    private void loadChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Interviews");
        series.getData().add(new XYChart.Data<>("Mon", 120));
        series.getData().add(new XYChart.Data<>("Tue", 150));
        series.getData().add(new XYChart.Data<>("Wed", 140));
        series.getData().add(new XYChart.Data<>("Thu", 180));
        series.getData().add(new XYChart.Data<>("Fri", 160));
        series.getData().add(new XYChart.Data<>("Sat", 210));
        series.getData().add(new XYChart.Data<>("Sun", 190));

        activityChart.getData().add(series);
    }

    /**
     * Builds the feed in code so each row gets a real icon tile instead of
     * a coloured dot, and so the last row can drop its divider.
     */
    private void buildActivityFeed() {
        activityList.getChildren().clear();

        for (int i = 0; i < RECENT_ACTIVITY.size(); i++) {
            Activity a = RECENT_ACTIVITY.get(i);

            FontIcon icon = new FontIcon(a.icon());
            icon.getStyleClass().addAll("icon-sm", "icon-muted");
            StackPane tile = new StackPane(icon);
            tile.getStyleClass().addAll("icon-tile", "icon-tile-sm", a.tint());

            Label text = new Label(a.text());
            text.getStyleClass().add("activity-text");
            text.setWrapText(true);

            Label time = new Label(a.time());
            time.getStyleClass().add("activity-time");

            VBox copy = new VBox(2, text, time);
            HBox.setHgrow(copy, Priority.ALWAYS);

            HBox row = new HBox(12, tile, copy);
            row.getStyleClass().add("activity-item");
            row.setAlignment(Pos.CENTER_LEFT);
            if (i == RECENT_ACTIVITY.size() - 1) {
                row.getStyleClass().add("activity-item-last");
            }

            activityList.getChildren().add(row);
        }
    }
}
