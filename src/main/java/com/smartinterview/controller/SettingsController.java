package com.smartinterview.controller;

import com.smartinterview.model.UserSettings;
import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.AuthException;
import com.smartinterview.service.AuthService;
import com.smartinterview.ui.Dialogs;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public class SettingsController {

    @FXML private ComboBox<String> difficultyCombo;
    @FXML private ComboBox<String> durationCombo;
    @FXML private ToggleButton emailToggle;
    @FXML private ToggleButton reminderToggle;

    @FXML private HBox statusBox;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        difficultyCombo.getItems().addAll("Easy", "Medium", "Hard");
        durationCombo.getItems().addAll("15 minutes", "30 minutes", "45 minutes");

        UserSettings saved = AuthService.getInstance().getSettings();
        difficultyCombo.setValue(saved.getDefaultDifficulty());
        durationCombo.setValue(saved.getDefaultDuration());
        emailToggle.setSelected(saved.isEmailNotifications());
        reminderToggle.setSelected(saved.isPracticeReminders());

        // The toggle's label states what is true now, not what clicking does,
        // so its meaning does not rest on the selected colour alone.
        bindToggleLabel(emailToggle);
        bindToggleLabel(reminderToggle);
    }

    private void bindToggleLabel(ToggleButton toggle) {
        toggle.setText(toggle.isSelected() ? "Enabled" : "Disabled");
        toggle.selectedProperty().addListener(
            (obs, was, isNow) -> toggle.setText(isNow ? "Enabled" : "Disabled"));
    }

    @FXML
    private void onSave() {
        UserSettings s = new UserSettings();
        s.setDefaultDifficulty(difficultyCombo.getValue());
        s.setDefaultDuration(durationCombo.getValue());
        s.setEmailNotifications(emailToggle.isSelected());
        s.setPracticeReminders(reminderToggle.isSelected());
        try {
            AuthService.getInstance().saveSettings(s);
            showStatus("Preferences saved.");
        } catch (AuthException e) {
            showStatus(e.getMessage());
        }
    }

    @FXML
    private void onChangePassword() {
        Dialogs.changePassword().ifPresent(pair -> {
            try {
                AuthService.getInstance().changePassword(pair[0], pair[1]);
                showStatus("Password changed.");
            } catch (AuthException e) {
                showStatus(e.getMessage());
            }
        });
    }

    @FXML
    private void onDeleteAccount() {
        if (AuthService.getInstance().isAdmin()) {
            showStatus("The administrator account cannot be deleted.");
            return;
        }
        boolean confirmed = Dialogs.confirm("Delete account",
            "This permanently deletes your account, interview history and saved questions. It cannot be undone.",
            "Delete account");
        if (!confirmed) {
            return;
        }
        try {
            AuthService.getInstance().deleteCurrentAccount();
            NavigationManager.getInstance().navigateTo("landing");
        } catch (AuthException e) {
            showStatus(e.getMessage());
        }
    }

    /**
     * Inline confirmation rather than a modal Alert: an Alert opens its own
     * window, which does not inherit the app stylesheets and so renders in
     * the default JavaFX theme.
     */
    private void showStatus(String message) {
        statusLabel.setText(message);
        statusBox.setVisible(true);
        statusBox.setManaged(true);

        PauseTransition hide = new PauseTransition(Duration.seconds(4));
        hide.setOnFinished(e -> {
            statusBox.setVisible(false);
            statusBox.setManaged(false);
        });
        hide.play();
    }
}
