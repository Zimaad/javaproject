package com.smartinterview;

import com.smartinterview.navigation.NavigationManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

/**
 * SmartInterview — AI-powered mock interview platform.
 * Main JavaFX Application entry point.
 */
public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        // Load bundled fonts
        loadFonts();

        // Root container
        StackPane root = new StackPane();
        root.getStyleClass().add("root-container");

        scene = new Scene(root, 1280, 800);

        // Load global stylesheets
        scene.getStylesheets().addAll(
            getClass().getResource("css/theme.css").toExternalForm(),
            getClass().getResource("css/components.css").toExternalForm()
        );

        // Initialize navigation
        NavigationManager.getInstance().initialize(scene, root);

        // Configure stage
        stage.setTitle("SmartInterview — Practice. Perform. Improve.");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.centerOnScreen();

        // Navigate to landing page
        NavigationManager.getInstance().navigateTo("landing");

        stage.show();
    }

    /**
     * Font files bundled in resources/com/smartinterview/fonts.
     *
     * Note on weights: TrueType only allows four styles per family
     * (regular / bold / italic / bold-italic), so Medium, SemiBold and
     * ExtraBold each register as their OWN family — "Plus Jakarta Sans
     * SemiBold" and so on. The stylesheets therefore select those
     * weights by family name rather than by -fx-font-weight, which
     * would silently fall back to Regular.
     */
    private static final String[] FONT_FILES = {
        "PlusJakartaSans-Regular.ttf",
        "PlusJakartaSans-Medium.ttf",
        "PlusJakartaSans-SemiBold.ttf",
        "PlusJakartaSans-Bold.ttf",
        "PlusJakartaSans-ExtraBold.ttf",
        "JetBrainsMono-Regular.ttf",
        "JetBrainsMono-Medium.ttf",
        "JetBrainsMono-Bold.ttf",
    };

    private void loadFonts() {
        for (String file : FONT_FILES) {
            try (InputStream in = getClass().getResourceAsStream("fonts/" + file)) {
                if (in == null) {
                    System.err.println("Font missing from resources: " + file);
                    continue;
                }
                if (Font.loadFont(in, 14) == null) {
                    System.err.println("Font could not be parsed: " + file);
                }
            } catch (IOException e) {
                System.err.println("Font failed to load: " + file + " — " + e.getMessage());
            }
        }
    }

    public static Scene getScene() {
        return scene;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
