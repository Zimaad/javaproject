package com.smartinterview.controller;

import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.MockAuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.regex.Pattern;

public class LoginController {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private HBox errorBox;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        // Clear the error as soon as the user starts fixing the input,
        // rather than leaving a stale message on screen.
        emailField.textProperty().addListener((obs, old, now) -> clearError());
        passwordField.textProperty().addListener((obs, old, now) -> clearError());
    }

    @FXML
    private void onLogin() {
        String email = text(emailField);
        String password = text(passwordField);

        if (email.isEmpty()) {
            fail("Enter the email address you signed up with.", emailField);
            return;
        }
        if (!EMAIL.matcher(email).matches()) {
            fail("That does not look like an email address.", emailField);
            return;
        }
        if (password.isEmpty()) {
            fail("Enter your password.", passwordField);
            return;
        }

        clearError();
        MockAuthService.getInstance().login(email, password);
        NavigationManager.getInstance().navigateTo("dashboard");
    }

    @FXML
    private void onRegister() {
        NavigationManager.getInstance().navigateTo("register");
    }

    @FXML
    private void onForgotPassword() {
        // Reported inline: a modal Alert opens its own window, which does
        // not inherit the app stylesheets.
        show("Password reset is not wired up in this demo build.", false);
    }

    @FXML
    private void onAdminLogin() {
        MockAuthService.getInstance().loginAsAdmin("admin@smartinterview.com", "admin");
        NavigationManager.getInstance().navigateTo("admin-dashboard");
    }

    /** Marks the offending field, explains the problem and focuses it. */
    private void fail(String message, javafx.scene.control.Control field) {
        show(message, true);
        if (!field.getStyleClass().contains("field-error")) {
            field.getStyleClass().add("field-error");
        }
        field.requestFocus();
    }

    private void show(String message, boolean isError) {
        errorLabel.setText(message);
        errorBox.getStyleClass().removeAll("alert-danger", "alert-info");
        errorBox.getStyleClass().add(isError ? "alert-danger" : "alert-info");
        errorBox.setVisible(true);
        errorBox.setManaged(true);
    }

    private void clearError() {
        errorBox.setVisible(false);
        errorBox.setManaged(false);
        emailField.getStyleClass().remove("field-error");
        passwordField.getStyleClass().remove("field-error");
    }

    private static String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}
