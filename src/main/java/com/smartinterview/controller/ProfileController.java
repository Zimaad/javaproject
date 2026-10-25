package com.smartinterview.controller;

import com.smartinterview.model.User;
import com.smartinterview.service.AuthException;
import com.smartinterview.service.AuthService;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public class ProfileController {

    @FXML private Label avatarInitials;
    @FXML private Label profileName;
    @FXML private Label profileRole;
    @FXML private Label summaryEmail;
    @FXML private Label summaryCollege;
    @FXML private Label emailValue;

    @FXML private TextField nameField;
    @FXML private TextField collegeField;
    @FXML private ComboBox<String> degreeCombo;
    @FXML private ComboBox<String> yearCombo;

    @FXML private HBox statusBox;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        degreeCombo.getItems().addAll(
            "B.Tech Computer Science",
            "B.Tech Information Technology",
            "B.E. Computer Science",
            "BCA",
            "MCA",
            "M.Tech Computer Science",
            "B.Sc Computer Science",
            "Other"
        );
        yearCombo.getItems().addAll("1st Year", "2nd Year", "3rd Year", "4th Year", "Graduate");

        populateFromUser();
    }

    /** Fills every field and the summary rail from the signed-in user. */
    private void populateFromUser() {
        User user = AuthService.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        profileName.setText(user.getFullName());
        profileRole.setText("admin".equalsIgnoreCase(user.getRole()) ? "Administrator" : "Student");
        avatarInitials.setText(initialsOf(user.getFullName()));

        emailValue.setText(blankToDash(user.getEmail()));
        summaryEmail.setText(blankToDash(user.getEmail()));
        summaryCollege.setText(blankToDash(user.getCollege()));

        nameField.setText(user.getFullName());
        collegeField.setText(user.getCollege());

        if (user.getDegree() != null) degreeCombo.setValue(user.getDegree());
        if (user.getYear() != null) yearCombo.setValue(user.getYear());
    }

    @FXML
    private void onSave() {
        User user = AuthService.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            // Validation failure is reported next to the work, and focus
            // moves to the field that needs fixing.
            nameField.getStyleClass().add("field-error");
            showStatus("Full name cannot be empty.", false);
            nameField.requestFocus();
            return;
        }
        nameField.getStyleClass().remove("field-error");

        try {
            AuthService.getInstance().updateProfile(
                name, collegeField.getText(), degreeCombo.getValue(), yearCombo.getValue());
        } catch (AuthException e) {
            showStatus(e.getMessage(), false);
            return;
        }

        populateFromUser();
        showStatus("Profile updated.", true);
    }

    @FXML
    private void onReset() {
        nameField.getStyleClass().remove("field-error");
        populateFromUser();
        hideStatus();
    }

    /**
     * Inline confirmation rather than a modal Alert: an Alert opens its own
     * window, which does not inherit the app stylesheets and so renders in
     * the default JavaFX theme.
     */
    private void showStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusBox.getStyleClass().removeAll("alert-success", "alert-danger");
        statusBox.getStyleClass().add(success ? "alert-success" : "alert-danger");
        statusBox.setVisible(true);
        statusBox.setManaged(true);

        if (success) {
            PauseTransition hide = new PauseTransition(Duration.seconds(4));
            hide.setOnFinished(e -> hideStatus());
            hide.play();
        }
    }

    private void hideStatus() {
        statusBox.setVisible(false);
        statusBox.setManaged(false);
    }

    private static String initialsOf(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        String initials = parts[0].substring(0, 1);
        if (parts.length > 1) {
            initials += parts[parts.length - 1].substring(0, 1);
        }
        return initials.toUpperCase();
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
