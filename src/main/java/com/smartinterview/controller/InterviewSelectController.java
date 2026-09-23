package com.smartinterview.controller;

import com.smartinterview.model.Interview;
import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.MockInterviewService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class InterviewSelectController {

    @FXML private FlowPane rolesFlowPane;

    @FXML
    public void initialize() {
        loadRoles();
    }

    private void loadRoles() {
        List<Interview> roles = MockInterviewService.getInstance().getAvailableRoles();
        for (Interview role : roles) {
            rolesFlowPane.getChildren().add(createRoleCard(role));
        }
    }

    private VBox createRoleCard(Interview role) {
        VBox card = new VBox();
        card.getStyleClass().add("interview-card");

        // ---- Header: icon tile + title ----
        FontIcon icon = new FontIcon(iconFor(role.getRoleName()));
        icon.getStyleClass().addAll("icon-xl", "icon-accent");

        StackPane tile = new StackPane(icon);
        tile.getStyleClass().addAll("icon-tile", "icon-tile-lg", "tile-accent");

        Label title = new Label(role.getRoleName());
        title.getStyleClass().add("role-title");
        title.setWrapText(true);

        HBox header = new HBox(14, tile, title);
        header.setAlignment(Pos.CENTER_LEFT);

        // ---- Description ----
        Label description = new Label(role.getRoleDescription());
        description.getStyleClass().add("role-description");
        description.setWrapText(true);
        description.setMinHeight(54);

        // ---- Topic chips ----
        FlowPane topics = new FlowPane();
        topics.getStyleClass().add("topics-flow");
        List<String> topicList = role.getTopics();
        for (int i = 0; i < Math.min(3, topicList.size()); i++) {
            Label chip = new Label(topicList.get(i));
            chip.getStyleClass().add("chip");
            topics.getChildren().add(chip);
        }
        if (topicList.size() > 3) {
            Label more = new Label("+" + (topicList.size() - 3) + " more");
            more.getStyleClass().addAll("chip", "chip-accent");
            topics.getChildren().add(more);
        }

        // ---- Meta row ----
        HBox meta = new HBox(20,
            createMetaBox("DIFFICULTY", role.getDifficulty()),
            createMetaBox("DURATION", role.getDurationMinutes() + " min"),
            createMetaBox("QUESTIONS", String.valueOf(role.getQuestionCount()))
        );
        meta.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // ---- Action ----
        Button start = new Button("Configure interview");
        start.getStyleClass().add("btn-secondary");
        start.setGraphic(new FontIcon("mdi2a-arrow-right"));
        start.setContentDisplay(ContentDisplay.RIGHT);
        start.setMaxWidth(Double.MAX_VALUE);
        start.setOnAction(e -> onSelectRole(role));

        // The whole card is the click target; the button states the action.
        card.setOnMouseClicked(e -> onSelectRole(role));

        card.getChildren().addAll(header, description, topics, meta, spacer, start);
        return card;
    }

    /** Picks a role-appropriate icon from the MaterialDesign2 set. */
    private static String iconFor(String roleName) {
        String name = roleName == null ? "" : roleName.toLowerCase();
        if (name.contains("java")) return "mdi2c-coffee-outline";
        if (name.contains("data")) return "mdi2c-chart-box-outline";
        if (name.contains("web") || name.contains("frontend")) return "mdi2c-code-braces";
        if (name.contains("database") || name.contains("sql")) return "mdi2d-database-outline";
        if (name.contains("test") || name.contains("qa")) return "mdi2c-check-circle-outline";
        return "mdi2b-briefcase-outline";
    }

    private VBox createMetaBox(String label, String value) {
        Label caption = new Label(label);
        caption.getStyleClass().add("meta-label");

        Label figure = new Label(value);
        figure.getStyleClass().add("meta-value");

        // Difficulty gets a semantic colour; the label beside it keeps the
        // meaning readable without relying on that colour.
        switch (value) {
            case "Easy" -> figure.getStyleClass().add("text-success");
            case "Medium" -> figure.getStyleClass().add("text-warning");
            case "Hard" -> figure.getStyleClass().add("text-danger");
            default -> { }
        }

        VBox box = new VBox(2, caption, figure);
        box.getStyleClass().add("role-meta");
        return box;
    }

    private void onSelectRole(Interview role) {
        NavigationManager.getInstance().navigateTo("interview-config", role);
    }
}
