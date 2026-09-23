package com.smartinterview.controller;

import com.smartinterview.model.Interview;
import com.smartinterview.model.InterviewResult;
import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.MockDataService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class EvaluationController implements NavigationManager.DataReceiver {

    @FXML private HBox step1, step2, step3, step4;
    @FXML private StackPane icon1, icon2, icon3, icon4;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button btnViewReport;

    private Interview currentInterview;
    private InterviewResult finalResult;

    @FXML
    public void initialize() {
        // Nothing to do until the finished interview arrives.
    }

    @Override
    public void receiveData(Object data) {
        if (data instanceof Interview interview) {
            currentInterview = interview;
            startSimulation();
        }
    }

    private void startSimulation() {
        reset(step1, icon1, "1");
        reset(step2, icon2, "2");
        reset(step3, icon3, "3");
        reset(step4, icon4, "4");

        btnViewReport.setVisible(false);
        btnViewReport.setManaged(false);
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO,          e -> markActive(step1, icon1)),
            new KeyFrame(Duration.seconds(1.0),  e -> { markDone(step1, icon1); markActive(step2, icon2); }),
            new KeyFrame(Duration.seconds(2.5),  e -> { markDone(step2, icon2); markActive(step3, icon3); }),
            new KeyFrame(Duration.seconds(3.5),  e -> { markDone(step3, icon3); markActive(step4, icon4); }),
            new KeyFrame(Duration.seconds(4.5),  e -> { markDone(step4, icon4); showReportButton(); })
        );
        timeline.play();
    }

    /** Pending: the step shows its number, greyed out. */
    private void reset(HBox row, StackPane marker, String number) {
        row.getStyleClass().remove("eval-step-active");
        marker.getStyleClass().setAll("step-icon", "step-icon-pending");
        marker.getChildren().setAll(new Label(number));
        stepLabel(row).getStyleClass().setAll("step-text", "step-text-pending");
    }

    /** Running: the row gets a wash and the marker a spinner icon. */
    private void markActive(HBox row, StackPane marker) {
        row.getStyleClass().add("eval-step-active");
        marker.getStyleClass().setAll("step-icon", "step-icon-active");
        marker.getChildren().setAll(icon("mdi2r-refresh", "icon-accent"));
        stepLabel(row).getStyleClass().setAll("step-text", "step-text-active");
    }

    /** Done: a check mark, so progress is not signalled by colour alone. */
    private void markDone(HBox row, StackPane marker) {
        row.getStyleClass().remove("eval-step-active");
        marker.getStyleClass().setAll("step-icon", "step-icon-done");
        marker.getChildren().setAll(icon("mdi2c-check", "icon-success"));
        stepLabel(row).getStyleClass().setAll("step-text", "step-text-done");
    }

    private static FontIcon icon(String literal, String colourClass) {
        FontIcon fi = new FontIcon(literal);
        fi.getStyleClass().addAll("icon-sm", colourClass);
        return fi;
    }

    /** The descriptive label is the second child of each step row. */
    private static Label stepLabel(HBox row) {
        return (Label) row.getChildren().get(1);
    }

    private void showReportButton() {
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        btnViewReport.setVisible(true);
        btnViewReport.setManaged(true);

        finalResult = MockDataService.getInstance()
            .generateMockEvaluation(currentInterview.getRoleName());
    }

    @FXML
    private void onViewReport() {
        NavigationManager.getInstance().navigateTo("performance-report", finalResult);
    }
}
