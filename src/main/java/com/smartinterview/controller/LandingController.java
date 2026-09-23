package com.smartinterview.controller;

import com.smartinterview.navigation.NavigationManager;
import javafx.fxml.FXML;

public class LandingController {

    @FXML
    private void onStartInterview() {
        NavigationManager.getInstance().navigateTo("login");
    }

    @FXML
    private void onExplore() {
        NavigationManager.getInstance().navigateTo("login");
    }

    @FXML
    private void onLogin() {
        NavigationManager.getInstance().navigateTo("login");
    }
}
