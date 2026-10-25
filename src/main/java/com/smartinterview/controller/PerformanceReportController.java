package com.smartinterview.controller;

import com.smartinterview.App;
import com.smartinterview.model.InterviewResult;
import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.ReportPdf;
import com.smartinterview.service.InterviewService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PerformanceReportController implements NavigationManager.DataReceiver {

    @FXML private Label reportSubtitle;
    @FXML private Label overallScoreLabel;
    @FXML private StackPane scoreCircle;
    @FXML private FontIcon verdictIcon;
    @FXML private Label verdictLabel;

    @FXML private VBox roundsContainer;
    @FXML private VBox strengthsContainer;
    @FXML private VBox weaknessesContainer;
    @FXML private VBox recommendationsContainer;

    private InterviewResult shown;

    @FXML
    public void initialize() {
        // Fall back to the most recent interview when opened from the nav
        // rather than from a just-finished round.
        InterviewResult latest = InterviewService.getInstance().getLatest();
        if (latest != null) {
            populateReport(latest);
        } else {
            showEmpty();
        }
    }

    private void showEmpty() {
        reportSubtitle.setText("No interviews yet — finish one to see your report here.");
        overallScoreLabel.setText("—");
        verdictLabel.setText("No data");
        roundsContainer.getChildren().clear();
        fillFeedback(strengthsContainer, null, "", "");
        fillFeedback(weaknessesContainer, null, "", "");
        fillFeedback(recommendationsContainer, null, "", "");
    }

    @Override
    public void receiveData(Object data) {
        if (data instanceof InterviewResult result) {
            populateReport(result);
        }
    }

    private void populateReport(InterviewResult result) {
        shown = result;
        int score = (int) result.getOverallScore();

        reportSubtitle.setText(result.getRoleName() + " · completed " + result.getDate());
        overallScoreLabel.setText(score + "%");
        applyScoreBand(score);

        buildRoundBreakdown(result.getRoundScores());

        // Each feedback row pairs an icon with its text, so the category is
        // still legible if the colour is not perceived.
        fillFeedback(strengthsContainer, result.getStrengths(), "mdi2c-check-circle-outline", "icon-success");
        fillFeedback(weaknessesContainer, result.getWeaknesses(), "mdi2a-alert-circle-outline", "icon-danger");
        fillFeedback(recommendationsContainer, result.getRecommendations(), "mdi2a-arrow-right", "icon-accent");
    }

    /** Colours the score ring and sets the plain-language verdict. */
    private void applyScoreBand(int score) {
        scoreCircle.getStyleClass().removeAll(
            "score-circle-success", "score-circle-warning", "score-circle-danger");
        verdictIcon.getStyleClass().removeAll("icon-success", "icon-warning", "icon-danger");

        String band;
        String icon;
        String verdict;
        if (score >= 75) {
            band = "score-circle-success";
            icon = "icon-success";
            verdict = "Strong result";
        } else if (score >= 50) {
            band = "score-circle-warning";
            icon = "icon-warning";
            verdict = "Room to improve";
        } else {
            band = "score-circle-danger";
            icon = "icon-danger";
            verdict = "Needs practice";
        }

        scoreCircle.getStyleClass().add(band);
        verdictIcon.getStyleClass().add(icon);
        verdictIcon.setIconLiteral(score >= 75 ? "mdi2t-thumb-up-outline" : "mdi2t-target");
        verdictLabel.setText(verdict);
    }

    private void buildRoundBreakdown(Map<String, Double> roundScores) {
        roundsContainer.getChildren().clear();
        for (Map.Entry<String, Double> entry : roundScores.entrySet()) {
            double value = entry.getValue();

            Label name = new Label(entry.getKey());
            name.getStyleClass().add("round-name");

            ProgressBar bar = new ProgressBar(value / 100.0);
            bar.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(bar, Priority.ALWAYS);
            // The bar's colour tracks the same bands as the overall ring.
            bar.getStyleClass().add(
                value >= 75 ? "progress-success" : value >= 50 ? "progress-warning" : "progress-danger");

            Label percent = new Label((int) value + "%");
            percent.getStyleClass().add("round-percent");

            HBox row = new HBox(12, name, bar, percent);
            row.getStyleClass().add("round-score");
            row.setAlignment(Pos.CENTER_LEFT);
            roundsContainer.getChildren().add(row);
        }
    }

    private void fillFeedback(VBox container, List<String> items, String iconLiteral, String iconClass) {
        container.getChildren().clear();

        if (items == null || items.isEmpty()) {
            Label none = new Label("Nothing recorded for this interview.");
            none.getStyleClass().add("text-small");
            container.getChildren().add(none);
            return;
        }

        for (String item : items) {
            FontIcon icon = new FontIcon(iconLiteral);
            icon.getStyleClass().addAll("icon-sm", iconClass);

            Label text = new Label(item);
            text.getStyleClass().add("feedback-text");
            text.setWrapText(true);
            HBox.setHgrow(text, Priority.ALWAYS);

            HBox row = new HBox(10, icon, text);
            row.getStyleClass().add("feedback-row");
            row.setAlignment(Pos.TOP_LEFT);
            container.getChildren().add(row);
        }
    }

    @FXML
    private void onExport() {
        if (shown == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export report");
        chooser.setInitialFileName("Interview-Report-" + shown.getRoleName().replace(' ', '-') + "-"
            + shown.getDate().replace(", ", "-").replace(' ', '-') + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF document", "*.pdf"));
        File file = chooser.showSaveDialog(App.getPrimaryStage());
        if (file == null) {
            return;
        }
        try {
            ReportPdf.write(shown, file.toPath());
            reportSubtitle.setText(shown.getRoleName() + " · completed " + shown.getDate()
                + "  ·  saved " + file.getName());
        } catch (IOException e) {
            reportSubtitle.setText("Export failed: " + e.getMessage());
        }
    }

    @FXML
    private void onNavDashboard() {
        NavigationManager.getInstance().navigateTo("dashboard");
    }
}
