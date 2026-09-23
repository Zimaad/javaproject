package com.smartinterview.navigation;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton navigation manager that handles all screen transitions.
 * Loads FXML views and swaps them in the main container with fade transitions.
 */
public class NavigationManager {

    private static NavigationManager instance;
    private Scene scene;
    private StackPane rootContainer;
    private String currentView;
    private final Map<String, String> viewMap = new HashMap<>();
    private final Map<String, String> cssMap = new HashMap<>();

    private NavigationManager() {
        // Map view names to FXML paths
        viewMap.put("landing", "/com/smartinterview/view/landing.fxml");
        viewMap.put("login", "/com/smartinterview/view/login.fxml");
        viewMap.put("register", "/com/smartinterview/view/register.fxml");
        viewMap.put("dashboard", "/com/smartinterview/view/dashboard.fxml");
        viewMap.put("interview-select", "/com/smartinterview/view/interview-select.fxml");
        viewMap.put("interview-config", "/com/smartinterview/view/interview-config.fxml");
        viewMap.put("mcq-round", "/com/smartinterview/view/mcq-round.fxml");
        viewMap.put("coding-round", "/com/smartinterview/view/coding-round.fxml");
        viewMap.put("hr-round", "/com/smartinterview/view/hr-round.fxml");
        viewMap.put("evaluation", "/com/smartinterview/view/evaluation.fxml");
        viewMap.put("performance-report", "/com/smartinterview/view/performance-report.fxml");
        viewMap.put("interview-history", "/com/smartinterview/view/interview-history.fxml");
        viewMap.put("question-bank", "/com/smartinterview/view/question-bank.fxml");
        viewMap.put("profile", "/com/smartinterview/view/profile.fxml");
        viewMap.put("settings", "/com/smartinterview/view/settings.fxml");
        viewMap.put("admin-dashboard", "/com/smartinterview/view/admin-dashboard.fxml");
        viewMap.put("admin-questions", "/com/smartinterview/view/admin-questions.fxml");

        // Map views to their specific CSS
        cssMap.put("landing", "/com/smartinterview/css/landing.css");
        cssMap.put("login", "/com/smartinterview/css/auth.css");
        cssMap.put("register", "/com/smartinterview/css/auth.css");
        cssMap.put("dashboard", "/com/smartinterview/css/dashboard.css");
        cssMap.put("interview-select", "/com/smartinterview/css/interview.css");
        cssMap.put("interview-config", "/com/smartinterview/css/interview.css");
        cssMap.put("mcq-round", "/com/smartinterview/css/interview.css");
        cssMap.put("coding-round", "/com/smartinterview/css/interview.css");
        cssMap.put("hr-round", "/com/smartinterview/css/interview.css");
        cssMap.put("evaluation", "/com/smartinterview/css/interview.css");
        cssMap.put("performance-report", "/com/smartinterview/css/dashboard.css");
        cssMap.put("interview-history", "/com/smartinterview/css/dashboard.css");
        cssMap.put("question-bank", "/com/smartinterview/css/dashboard.css");
        cssMap.put("profile", "/com/smartinterview/css/dashboard.css");
        cssMap.put("settings", "/com/smartinterview/css/dashboard.css");
        cssMap.put("admin-dashboard", "/com/smartinterview/css/admin.css");
        cssMap.put("admin-questions", "/com/smartinterview/css/admin.css");
    }

    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    public void initialize(Scene scene, StackPane rootContainer) {
        this.scene = scene;
        this.rootContainer = rootContainer;
    }

    /**
     * Navigate to a named view with a fade transition.
     */
    public void navigateTo(String viewName) {
        navigateTo(viewName, null);
    }

    /**
     * Navigate to a named view, passing optional data to the controller.
     */
    public void navigateTo(String viewName, Object data) {
        String fxmlPath = viewMap.get(viewName);
        if (fxmlPath == null) {
            System.err.println("Unknown view: " + viewName);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Pass data to controller if it supports it
            Object controller = loader.getController();
            if (controller instanceof DataReceiver && data != null) {
                ((DataReceiver) controller).receiveData(data);
            }

            // Load view-specific CSS
            String cssPath = cssMap.get(viewName);
            if (cssPath != null) {
                String cssUrl = getClass().getResource(cssPath).toExternalForm();
                if (!scene.getStylesheets().contains(cssUrl)) {
                    scene.getStylesheets().add(cssUrl);
                }
            }

            // Fade transition
            if (!rootContainer.getChildren().isEmpty()) {
                Parent oldView = (Parent) rootContainer.getChildren().get(0);
                FadeTransition fadeOut = new FadeTransition(Duration.millis(150), oldView);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    rootContainer.getChildren().clear();
                    view.setOpacity(0);
                    rootContainer.getChildren().add(view);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(200), view);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();
            } else {
                view.setOpacity(0);
                rootContainer.getChildren().add(view);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), view);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            }

            currentView = viewName;

        } catch (IOException e) {
            System.err.println("Failed to load view: " + viewName);
            e.printStackTrace();
        }
    }

    public String getCurrentView() {
        return currentView;
    }

    /**
     * Interface for controllers that can receive navigation data.
     */
    public interface DataReceiver {
        void receiveData(Object data);
    }
}
