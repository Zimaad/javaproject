package com.smartinterview.controller;

import com.smartinterview.model.Interview;
import com.smartinterview.model.InterviewResult;
import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.EvaluationService;
import com.smartinterview.service.InterviewSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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
            startEvaluation();
        }
    }

    private void startEvaluation() {
        reset(step1, icon1, "1");
        reset(step2, icon2, "2");
        reset(step3, icon3, "3");
        reset(step4, icon4, "4");

        btnViewReport.setVisible(false);
        btnViewReport.setManaged(false);
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);

        InterviewSession session = InterviewSession.current();
        StackPane[] icons = {icon1, icon2, icon3, icon4};
        HBox[] steps = {step1, step2, step3, step4};

        Task<InterviewResult> grading = new Task<>() {
            @Override
            protected InterviewResult call() {
                // Each stage is reported from this worker thread as it genuinely begins.
                return EvaluationService.getInstance().evaluate(session, stage -> Platform.runLater(() -> {
                    if (stage > 1) markDone(steps[stage - 2], icons[stage - 2]);
                    markActive(steps[stage - 1], icons[stage - 1]);
                }));
            }
        };
        grading.setOnSucceeded(e -> {
            markDone(step4, icon4);
            finalResult = grading.getValue();
            InterviewSession.clear();
            showReportButton();
        });
        grading.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            progressIndicator.setManaged(false);
            stepLabel(step4).setText("Evaluation failed: " + grading.getException().getMessage());
        });

        Thread worker = new Thread(grading, "evaluate-interview");
        worker.setDaemon(true);
        worker.start();
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
    }

    @FXML
    private void onViewReport() {
        NavigationManager.getInstance().navigateTo("performance-report", finalResult);
    }
}
