package com.smartinterview.ui;

import com.smartinterview.App;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;

import java.util.Optional;

/**
 * Modal dialogs that carry the app's stylesheets. A bare JavaFX Dialog opens
 * its own window that would otherwise render in the default theme.
 */
public final class Dialogs {

    private Dialogs() {}

    /** Applies the app theme and owner window to any dialog. */
    public static void style(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().addAll(App.getScene().getStylesheets());
        if (!pane.getStyleClass().contains("root-container")) {
            pane.getStyleClass().add("root-container");
        }
        dialog.initOwner(App.getPrimaryStage());
    }

    public static boolean confirm(String title, String message, String confirmLabel) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        ButtonType yes = new ButtonType(confirmLabel, javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(yes, ButtonType.CANCEL);
        Label text = new Label(message);
        text.setWrapText(true);
        text.setMaxWidth(380);
        dialog.getDialogPane().setContent(text);
        style(dialog);
        return dialog.showAndWait().filter(b -> b == yes).isPresent();
    }

    /** What to generate with the AI. */
    public record GenerateRequest(String type, String topic, String difficulty, int count) {
    }

    public static Optional<GenerateRequest> generateQuestions() {
        Dialog<GenerateRequest> dialog = new Dialog<>();
        dialog.setTitle("Generate questions with AI");

        javafx.scene.control.ComboBox<String> type = new javafx.scene.control.ComboBox<>();
        type.getItems().addAll("mcq", "coding", "hr");
        type.setValue("mcq");
        javafx.scene.control.TextField topic = new javafx.scene.control.TextField();
        topic.setPromptText("e.g. Java Collections, SQL joins");
        javafx.scene.control.ComboBox<String> difficulty = new javafx.scene.control.ComboBox<>();
        difficulty.getItems().addAll("Easy", "Medium", "Hard");
        difficulty.setValue("Medium");
        javafx.scene.control.ComboBox<Integer> count = new javafx.scene.control.ComboBox<>();
        count.getItems().addAll(3, 5, 10);
        count.setValue(5);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.addRow(0, new Label("Type"), type);
        grid.addRow(1, new Label("Topic"), topic);
        grid.addRow(2, new Label("Difficulty"), difficulty);
        grid.addRow(3, new Label("How many"), count);
        Label error = new Label();
        grid.add(error, 0, 4, 2, 1);

        ButtonType go = new ButtonType("Generate", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(go, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().lookupButton(go).addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (topic.getText().isBlank()) {
                error.setText("Enter a topic.");
                ev.consume();
            }
        });
        dialog.setResultConverter(b -> b == go
            ? new GenerateRequest(type.getValue(), topic.getText().trim(), difficulty.getValue(), count.getValue())
            : null);
        style(dialog);
        return dialog.showAndWait();
    }

    /** Asks for the current and new password. Returns {current, new} or empty if cancelled. */
    public static Optional<String[]> changePassword() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Change password");

        PasswordField current = new PasswordField();
        PasswordField next = new PasswordField();
        PasswordField confirm = new PasswordField();
        next.setPromptText("At least 8 characters");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.addRow(0, new Label("Current password"), current);
        grid.addRow(1, new Label("New password"), next);
        grid.addRow(2, new Label("Confirm new password"), confirm);
        Label error = new Label();
        error.setWrapText(true);
        grid.add(error, 0, 3, 2, 1);

        ButtonType save = new ButtonType("Change password", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(grid);

        // Keep the dialog open and explain, rather than closing on a mismatch.
        dialog.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (!next.getText().equals(confirm.getText())) {
                error.setText("The new passwords do not match.");
                ev.consume();
            }
        });
        dialog.setResultConverter(b -> b == save ? new String[] {current.getText(), next.getText()} : null);
        style(dialog);
        return dialog.showAndWait();
    }
}
