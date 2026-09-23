package com.smartinterview.controller;

import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.MockAuthService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.regex.Pattern;

public class RegisterController {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField collegeField;
    @FXML private ComboBox<String> degreeCombo;
    @FXML private ComboBox<String> yearCombo;
    @FXML private HBox errorBox;
    @FXML private Label errorLabel;

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

        // Clearing on edit keeps a stale error from sitting under a field
        // the user has already corrected.
        List<TextInputControl> watched =
            List.of(nameField, emailField, passwordField, confirmPasswordField);
        watched.forEach(f -> f.textProperty().addListener((obs, old, now) -> clearErrors()));
    }

    @FXML
    private void onRegister() {
        String name = text(nameField);
        String email = text(emailField);
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (name.isEmpty()) {
            fail("Enter your full name.", nameField);
            return;
        }
        if (email.isEmpty()) {
            fail("Enter your email address.", emailField);
            return;
        }
        if (!EMAIL.matcher(email).matches()) {
            fail("That does not look like an email address.", emailField);
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            fail("Use at least " + MIN_PASSWORD_LENGTH + " characters for your password.", passwordField);
            return;
        }
        if (!password.equals(confirm)) {
            fail("The two passwords do not match.", confirmPasswordField);
            return;
        }

        clearErrors();
        MockAuthService.getInstance().register(
            name, email, password, text(collegeField), degreeCombo.getValue(), yearCombo.getValue());
        NavigationManager.getInstance().navigateTo("login");
    }

    @FXML
    private void onBackToLogin() {
        NavigationManager.getInstance().navigateTo("login");
    }

    /** Marks the offending field, explains the problem and focuses it. */
    private void fail(String message, Control field) {
        errorLabel.setText(message);
        errorBox.setVisible(true);
        errorBox.setManaged(true);
        if (!field.getStyleClass().contains("field-error")) {
            field.getStyleClass().add("field-error");
        }
        field.requestFocus();
    }

    private void clearErrors() {
        errorBox.setVisible(false);
        errorBox.setManaged(false);
        List.of(nameField, emailField, passwordField, confirmPasswordField, collegeField)
            .forEach(f -> f.getStyleClass().remove("field-error"));
    }

    private static String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
