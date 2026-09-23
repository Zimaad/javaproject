package com.smartinterview.controller;

import com.smartinterview.model.InterviewResult;
import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.MockDataService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class InterviewHistoryController {

    private static final String ANY_ROLE = "All roles";

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private ComboBox<String> periodCombo;

    @FXML private TableView<InterviewResult> historyTable;
    @FXML private TableColumn<InterviewResult, String> colDate;
    @FXML private TableColumn<InterviewResult, String> colRole;
    @FXML private TableColumn<InterviewResult, String> colScore;
    @FXML private TableColumn<InterviewResult, String> colDuration;
    @FXML private TableColumn<InterviewResult, Void> colAction;

    private List<InterviewResult> allResults;

    @FXML
    public void initialize() {
        allResults = MockDataService.getInstance().getInterviewHistory();

        setupTable();
        buildFilters();
        applyFilters();

        searchField.textProperty().addListener((obs, old, now) -> applyFilters());
        roleCombo.valueProperty().addListener((obs, old, now) -> applyFilters());
    }

    private void setupTable() {
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDate()));
        colRole.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRoleName()));
        colScore.setCellValueFactory(cell ->
            new SimpleStringProperty(String.valueOf((int) cell.getValue().getOverallScore())));
        colDuration.setCellValueFactory(cell ->
            new SimpleStringProperty(cell.getValue().getTimeTakenMinutes() + " min"));

        // Score renders as a banded chip so a row's outcome is readable at a
        // glance, with the number itself always present.
        colScore.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                    return;
                }
                int score = Integer.parseInt(value);
                Label chip = new Label(score + "%");
                chip.getStyleClass().addAll("chip",
                    score >= 75 ? "chip-success" : score >= 50 ? "chip-warning" : "chip-danger");
                setGraphic(chip);
            }
        });

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("View report");

            {
                btn.getStyleClass().addAll("btn-ghost", "btn-small");
                btn.setGraphic(new FontIcon("mdi2a-arrow-right"));
                btn.setContentDisplay(ContentDisplay.RIGHT);
                btn.setOnAction(e -> {
                    InterviewResult result = getTableView().getItems().get(getIndex());
                    NavigationManager.getInstance().navigateTo("performance-report", result);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void buildFilters() {
        Set<String> roles = new TreeSet<>();
        for (InterviewResult r : allResults) {
            if (r.getRoleName() != null) roles.add(r.getRoleName());
        }

        roleCombo.setItems(FXCollections.observableArrayList(roles));
        roleCombo.getItems().add(0, ANY_ROLE);
        roleCombo.setValue(ANY_ROLE);

        // Only one period of mock data exists, so the control states that
        // rather than offering ranges that would all return the same rows.
        periodCombo.setItems(FXCollections.observableArrayList("All time"));
        periodCombo.setValue("All time");
        periodCombo.setDisable(true);
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String role = roleCombo.getValue();

        historyTable.getItems().setAll(allResults.stream()
            .filter(r -> query.isEmpty()
                || (r.getRoleName() != null
                    && r.getRoleName().toLowerCase(Locale.ROOT).contains(query)))
            .filter(r -> role == null || ANY_ROLE.equals(role) || role.equals(r.getRoleName()))
            .toList());
    }
}
