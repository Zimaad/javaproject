package com.smartinterview.controller;

import com.smartinterview.model.Question;
import com.smartinterview.service.QuestionService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class QuestionBankController {

    private static final String ANY_CATEGORY = "All categories";
    private static final String ANY_DIFFICULTY = "All difficulties";
    private static final String ANY_TYPE = "All types";

    @FXML private VBox questionsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> difficultyCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private Label resultCountLabel;

    private List<Question> allQuestions;

    @FXML
    public void initialize() {
        allQuestions = QuestionService.getInstance().getAll();

        buildFilters();
        applyFilters();

        // Filtering happens as the user types / picks, with no Apply button.
        searchField.textProperty().addListener((obs, old, now) -> applyFilters());
        categoryCombo.valueProperty().addListener((obs, old, now) -> applyFilters());
        difficultyCombo.valueProperty().addListener((obs, old, now) -> applyFilters());
        typeCombo.valueProperty().addListener((obs, old, now) -> applyFilters());
    }

    /** Populates the filter dropdowns from the data itself. */
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

        difficultyCombo.setItems(FXCollections.observableArrayList(
            ANY_DIFFICULTY, "Easy", "Medium", "Hard"));
        difficultyCombo.setValue(ANY_DIFFICULTY);

        typeCombo.setItems(FXCollections.observableArrayList(types));
        typeCombo.getItems().add(0, ANY_TYPE);
        typeCombo.setValue(ANY_TYPE);
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String category = categoryCombo.getValue();
        String difficulty = difficultyCombo.getValue();
        String type = typeCombo.getValue();

        questionsContainer.getChildren().clear();
        int shown = 0;

        for (Question q : allQuestions) {
            if (!query.isEmpty()
                && (q.getQuestionText() == null
                    || !q.getQuestionText().toLowerCase(Locale.ROOT).contains(query))) {
                continue;
            }
            if (category != null && !ANY_CATEGORY.equals(category) && !category.equals(q.getCategory())) {
                continue;
            }
            if (difficulty != null && !ANY_DIFFICULTY.equals(difficulty) && !difficulty.equals(q.getDifficulty())) {
                continue;
            }
            if (type != null && !ANY_TYPE.equals(type)
                && (q.getType() == null || !type.equalsIgnoreCase(q.getType()))) {
                continue;
            }

            questionsContainer.getChildren().add(createQuestionCard(q));
            shown++;
        }

        if (shown == 0) {
            questionsContainer.getChildren().add(createEmptyState());
        }
        resultCountLabel.setText(shown + (shown == 1 ? " question" : " questions"));
    }

    private VBox createQuestionCard(Question q) {
        VBox card = new VBox(14);
        card.getStyleClass().add("question-card");

        // ---- Header: type, category, difficulty, bookmark ----
        Label typeBadge = new Label(q.getType() == null ? "—" : q.getType().toUpperCase(Locale.ROOT));
        typeBadge.getStyleClass().addAll("badge", "badge-category");

        Label categoryBadge = new Label(q.getCategory());
        categoryBadge.getStyleClass().add("badge");

        Label difficultyBadge = new Label(q.getDifficulty());
        difficultyBadge.getStyleClass().addAll("badge", difficultyClass(q.getDifficulty()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Icon-only control, so it carries a tooltip for its meaning.
        Button bookmark = new Button();
        bookmark.getStyleClass().add("bookmark-btn");
        bookmark.setGraphic(new FontIcon("mdi2s-star-outline"));
        bookmark.setTooltip(new Tooltip("Save this question"));
        bookmark.setOnAction(e -> toggleBookmark(bookmark, q));
        if (q.isBookmarked()) {
            showBookmarked(bookmark, true);
        }

        HBox header = new HBox(8, typeBadge, categoryBadge, difficultyBadge, spacer, bookmark);
        header.setAlignment(Pos.CENTER_LEFT);

        // ---- Question ----
        Label questionText = new Label(q.getQuestionText());
        questionText.getStyleClass().add("qbank-question");
        questionText.setWrapText(true);

        // ---- Answer panel, collapsed by default ----
        String answer = q.getExplanation() != null ? q.getExplanation() : q.getSampleAnswer();
        card.getChildren().addAll(header, questionText);

        if (answer == null || answer.isBlank()) {
            return card;
        }

        Label answerLabel = new Label(q.getExplanation() != null ? "EXPLANATION" : "SAMPLE ANSWER");
        answerLabel.getStyleClass().add("qbank-answer-label");

        Label answerText = new Label(answer);
        answerText.getStyleClass().add("qbank-answer");
        answerText.setWrapText(true);

        VBox answerPanel = new VBox(answerLabel, answerText);
        answerPanel.getStyleClass().add("qbank-answer-panel");
        answerPanel.setVisible(false);
        answerPanel.setManaged(false);

        FontIcon chevron = new FontIcon("mdi2c-chevron-down");
        chevron.getStyleClass().add("icon-xs");

        Button toggle = new Button("Show answer");
        toggle.getStyleClass().addAll("btn-ghost", "btn-small");
        toggle.setGraphic(chevron);
        toggle.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        toggle.setOnAction(e -> {
            boolean showing = !answerPanel.isVisible();
            answerPanel.setVisible(showing);
            answerPanel.setManaged(showing);
            toggle.setText(showing ? "Hide answer" : "Show answer");
            chevron.setIconLiteral(showing ? "mdi2c-chevron-up" : "mdi2c-chevron-down");
        });

        HBox toggleRow = new HBox(toggle);
        toggleRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(toggleRow, answerPanel);
        return card;
    }

    private void toggleBookmark(Button bookmark, Question q) {
        boolean turnOn = !bookmark.getStyleClass().contains("bookmark-btn-on");
        q.setBookmarked(turnOn);
        QuestionService.getInstance().setBookmarked(q.getId(), turnOn);
        showBookmarked(bookmark, turnOn);
    }

    private void showBookmarked(Button bookmark, boolean on) {
        FontIcon icon = (FontIcon) bookmark.getGraphic();
        bookmark.getStyleClass().remove("bookmark-btn-on");
        if (on) {
            bookmark.getStyleClass().add("bookmark-btn-on");
            icon.setIconLiteral("mdi2s-star");
            bookmark.setTooltip(new Tooltip("Saved — click to remove"));
        } else {
            icon.setIconLiteral("mdi2s-star-outline");
            bookmark.setTooltip(new Tooltip("Save this question"));
        }
    }

    private VBox createEmptyState() {
        FontIcon icon = new FontIcon("mdi2m-magnify");
        icon.getStyleClass().addAll("icon-2xl", "icon-subtle");

        Label title = new Label("No matching questions");
        title.getStyleClass().add("empty-title");

        Label hint = new Label("Try a different search term or clear the filters.");
        hint.getStyleClass().add("empty-text");

        Button clear = new Button("Clear filters");
        clear.getStyleClass().addAll("btn-secondary", "btn-small");
        clear.setOnAction(e -> {
            searchField.clear();
            categoryCombo.setValue(ANY_CATEGORY);
            difficultyCombo.setValue(ANY_DIFFICULTY);
            typeCombo.setValue(ANY_TYPE);
        });

        VBox empty = new VBox(icon, title, hint, clear);
        empty.getStyleClass().add("empty-state");
        empty.setAlignment(Pos.CENTER);
        return empty;
    }

    private static String difficultyClass(String difficulty) {
        if ("Easy".equals(difficulty)) return "badge-easy";
        if ("Medium".equals(difficulty)) return "badge-medium";
        return "badge-hard";
    }
}
