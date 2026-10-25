package com.smartinterview.controller;

import com.smartinterview.ai.AiClient;
import com.smartinterview.model.Question;
import com.smartinterview.ui.Dialogs;
import com.smartinterview.ui.QuestionEditorDialog;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.util.Duration;
import java.sql.SQLException;
import com.smartinterview.service.QuestionService;
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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class AdminQuestionController {

    private static final String ANY_CATEGORY = "All categories";
    private static final String ANY_TYPE = "All types";

    @FXML private TableView<Question> questionTable;
    @FXML private TableColumn<Question, String> colId;
    @FXML private TableColumn<Question, String> colType;
    @FXML private TableColumn<Question, String> colCategory;
    @FXML private TableColumn<Question, String> colDiff;
    @FXML private TableColumn<Question, String> colText;
    @FXML private TableColumn<Question, Void> colAction;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> typeCombo;

    private List<Question> allQuestions;

    @FXML
    public void initialize() {
        allQuestions = QuestionService.getInstance().getAll();

        setupTable();
        buildFilters();
        applyFilters();

        searchField.textProperty().addListener((obs, old, now) -> applyFilters());
        categoryCombo.valueProperty().addListener((obs, old, now) -> applyFilters());
        typeCombo.valueProperty().addListener((obs, old, now) -> applyFilters());
    }

    private void setupTable() {
        colId.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getId()));
        colType.setCellValueFactory(cell ->
            new SimpleStringProperty(cell.getValue().getType().toUpperCase(Locale.ROOT)));
        colCategory.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategory()));
        colDiff.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDifficulty()));
        colText.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getQuestionText()));

        // Difficulty reads as a badge, matching how it appears everywhere else.
        colDiff.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String difficulty, boolean empty) {
                super.updateItem(difficulty, empty);
                if (empty || difficulty == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(difficulty);
                badge.getStyleClass().addAll("badge", switch (difficulty) {
                    case "Easy" -> "badge-easy";
                    case "Medium" -> "badge-medium";
                    default -> "badge-hard";
                });
                setGraphic(badge);
            }
        });

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox pane = new HBox(4, btnEdit, btnDelete);

            {
                // Icon-only row actions, each with a tooltip naming it.
                btnEdit.getStyleClass().addAll("btn-icon", "btn-ghost");
                btnEdit.setGraphic(new FontIcon("mdi2p-pencil-outline"));
                btnEdit.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btnEdit.setTooltip(new Tooltip("Edit question"));
                btnEdit.setOnAction(e -> onEditQuestion(getTableView().getItems().get(getIndex())));

                btnDelete.getStyleClass().addAll("btn-icon", "btn-danger-quiet");
                btnDelete.setGraphic(new FontIcon("mdi2d-delete-outline"));
                btnDelete.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btnDelete.setTooltip(new Tooltip("Delete question"));
                btnDelete.setOnAction(e -> onDeleteQuestion(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void buildFilters() {
        Set<String> categories = new TreeSet<>();
        Set<String> types = new TreeSet<>();
        for (Question q : allQuestions) {
            if (q.getCategory() != null) categories.add(q.getCategory());
            if (q.getType() != null) types.add(q.getType().toUpperCase(Locale.ROOT));
        }

        categoryCombo.setItems(FXCollections.observableArrayList(categories));
        categoryCombo.getItems().add(0, ANY_CATEGORY);
        categoryCombo.setValue(ANY_CATEGORY);

        typeCombo.setItems(FXCollections.observableArrayList(types));
        typeCombo.getItems().add(0, ANY_TYPE);
        typeCombo.setValue(ANY_TYPE);
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String category = categoryCombo.getValue();
        String type = typeCombo.getValue();

        questionTable.getItems().setAll(allQuestions.stream()
            .filter(q -> query.isEmpty()
                || (q.getQuestionText() != null
                    && q.getQuestionText().toLowerCase(Locale.ROOT).contains(query)))
            .filter(q -> category == null || ANY_CATEGORY.equals(category) || category.equals(q.getCategory()))
            .filter(q -> type == null || ANY_TYPE.equals(type)
                || (q.getType() != null && type.equalsIgnoreCase(q.getType())))
            .toList());
    }

    @FXML
    private void onAddQuestion() {
        QuestionEditorDialog.show(null).ifPresent(this::persist);
    }

    private void onEditQuestion(Question q) {
        QuestionEditorDialog.show(q).ifPresent(this::persist);
    }

    private void persist(Question q) {
        try {
            QuestionService.getInstance().save(q);
            reload();
        } catch (SQLException e) {
            showMessage("Could not save the question", e.getMessage());
        }
    }

    private void onDeleteQuestion(Question q) {
        if (!Dialogs.confirm("Delete question", "Delete this question from the bank? It cannot be undone.", "Delete")) {
            return;
        }
        try {
            QuestionService.getInstance().delete(q.getId());
            reload();
        } catch (SQLException e) {
            showMessage("Could not delete the question", e.getMessage());
        }
    }

    @FXML
    private void onGenerateQuestions() {
        if (!AiClient.getInstance().isConfigured()) {
            showMessage("AI is not configured", "Add an API key to config.properties and restart the app.");
            return;
        }
        Dialogs.generateQuestions().ifPresent(req -> {
            showMessage("Generating…", "Asking the AI for " + req.count() + " " + req.type().toUpperCase(Locale.ROOT)
                + " questions about " + req.topic() + ".");
            Task<List<Question>> task = new Task<>() {
                @Override
                protected List<Question> call() throws Exception {
                    return QuestionService.getInstance().generate(
                        req.type(), "software", List.of(req.topic()), req.difficulty(), req.count());
                }
            };
            task.setOnSucceeded(e -> {
                reload();
                showMessage("Added " + task.getValue().size() + " questions", "They are now in the bank.");
            });
            task.setOnFailed(e -> {
                reload();
                showMessage("Generation failed", task.getException().getMessage());
            });
            Thread worker = new Thread(task, "generate-questions");
            worker.setDaemon(true);
            worker.start();
        });
    }

    /** Re-reads the bank after a change so the table and filters reflect it. */
    private void reload() {
        allQuestions = QuestionService.getInstance().getAll();
        buildFilters();
        applyFilters();
    }

    /** Reported in the table's own area; a bare modal Alert would open an unstyled window. */
    private void showMessage(String title, String body) {
        questionTable.setPlaceholder(buildNotice(title, body));
        if (!questionTable.getItems().isEmpty()) {
            questionTable.getItems().clear();
            PauseTransition restore = new PauseTransition(Duration.seconds(4));
            restore.setOnFinished(e -> applyFilters());
            restore.play();
        }
    }

    private javafx.scene.Node buildNotice(String heading, String text) {
        FontIcon icon = new FontIcon("mdi2i-information-outline");
        icon.getStyleClass().addAll("icon-2xl", "icon-subtle");

        Label title = new Label(heading);
        title.getStyleClass().add("empty-title");

        Label body = new Label(text);
        body.getStyleClass().add("empty-text");
        body.setWrapText(true);

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(12, icon, title, body);
        box.getStyleClass().add("empty-state");
        box.setAlignment(javafx.geometry.Pos.CENTER);
        return box;
    }
}
