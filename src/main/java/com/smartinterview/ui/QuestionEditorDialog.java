package com.smartinterview.ui;

import com.smartinterview.model.Question;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Add / edit form for a single question. Fields shown depend on the question type. */
public final class QuestionEditorDialog {

    private QuestionEditorDialog() {}

    /** Shows the form, prefilled when {@code existing} is non-null. Returns the edited question if saved. */
    public static Optional<Question> show(Question existing) {
        boolean editing = existing != null;
        Dialog<Question> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Edit question" : "Add question");

        ComboBox<String> type = new ComboBox<>();
        type.getItems().addAll("mcq", "coding", "hr");
        type.setValue(editing ? existing.getType() : "mcq");
        type.setDisable(editing);

        TextField category = new TextField(editing ? existing.getCategory() : "");
        category.setPromptText("e.g. Java, DSA, Behavioral");
        ComboBox<String> difficulty = new ComboBox<>();
        difficulty.getItems().addAll("Easy", "Medium", "Hard");
        difficulty.setValue(editing ? existing.getDifficulty() : "Medium");

        TextArea text = new TextArea(editing ? existing.getQuestionText() : "");
        text.setPrefRowCount(5);
        text.setWrapText(true);

        // MCQ
        TextField[] options = new TextField[4];
        ComboBox<String> correct = new ComboBox<>();
        correct.getItems().addAll("A", "B", "C", "D");
        correct.setValue("A");
        VBox mcqBox = new VBox(8);
        for (int i = 0; i < 4; i++) {
            options[i] = new TextField();
            options[i].setPromptText("Option " + (char) ('A' + i));
            if (editing && existing.getOptions() != null && i < existing.getOptions().size()) {
                options[i].setText(existing.getOptions().get(i));
            }
            mcqBox.getChildren().add(options[i]);
        }
        if (editing && "mcq".equals(existing.getType())) {
            correct.setValue(String.valueOf((char) ('A' + existing.getCorrectOptionIndex())));
        }
        mcqBox.getChildren().add(new HBoxLabel("Correct answer", correct));

        TextArea explanation = new TextArea(editing && existing.getExplanation() != null ? existing.getExplanation() : "");
        explanation.setPrefRowCount(2);
        explanation.setWrapText(true);
        explanation.setPromptText("Explanation shown with the answer");

        // Coding
        TextArea template = new TextArea(editing && existing.getCodeTemplate() != null ? existing.getCodeTemplate() : "");
        template.setPrefRowCount(5);
        template.setPromptText("Starter code shown in the editor");

        // HR
        TextArea sample = new TextArea(editing && existing.getSampleAnswer() != null ? existing.getSampleAnswer() : "");
        sample.setPrefRowCount(3);
        sample.setWrapText(true);
        sample.setPromptText("What a strong answer covers");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(4));
        grid.setPrefWidth(560);
        grid.addRow(0, new Label("Type"), type);
        grid.addRow(1, new Label("Category"), category);
        grid.addRow(2, new Label("Difficulty"), difficulty);
        grid.addRow(3, new Label("Question"), text);
        Label optionsLabel = new Label("Options");
        Label explanationLabel = new Label("Explanation");
        Label templateLabel = new Label("Code template");
        Label sampleLabel = new Label("Sample answer");
        grid.addRow(4, optionsLabel, mcqBox);
        grid.addRow(5, explanationLabel, explanation);
        grid.addRow(6, templateLabel, template);
        grid.addRow(7, sampleLabel, sample);
        Label error = new Label();
        error.setWrapText(true);
        grid.add(error, 0, 8, 2, 1);

        // Only the fields that apply to the chosen type take up space.
        var isMcq = Bindings.equal(type.valueProperty(), "mcq");
        var isCoding = Bindings.equal(type.valueProperty(), "coding");
        var isHr = Bindings.equal(type.valueProperty(), "hr");
        for (var n : List.of(optionsLabel, mcqBox, explanationLabel, explanation)) {
            n.visibleProperty().bind(isMcq);
            n.managedProperty().bind(isMcq);
        }
        for (var n : List.of(templateLabel, template)) {
            n.visibleProperty().bind(isCoding);
            n.managedProperty().bind(isCoding);
        }
        for (var n : List.of(sampleLabel, sample)) {
            n.visibleProperty().bind(isHr);
            n.managedProperty().bind(isHr);
        }

        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String problem = validate(type.getValue(), category, text, options);
            if (problem != null) {
                error.setText(problem);
                ev.consume();
            }
        });

        dialog.setResultConverter(button -> {
            if (button != save) {
                return null;
            }
            Question q = editing ? existing : new Question();
            q.setType(type.getValue());
            q.setCategory(category.getText().trim());
            q.setDifficulty(difficulty.getValue());
            q.setQuestionText(text.getText().trim());
            switch (type.getValue()) {
                case "mcq" -> {
                    List<String> opts = new ArrayList<>();
                    for (TextField f : options) opts.add(f.getText().trim());
                    q.setOptions(opts);
                    q.setCorrectOptionIndex(correct.getValue().charAt(0) - 'A');
                    q.setExplanation(explanation.getText().trim());
                }
                case "coding" -> q.setCodeTemplate(template.getText());
                default -> q.setSampleAnswer(sample.getText().trim());
            }
            return q;
        });

        Dialogs.style(dialog);
        return dialog.showAndWait();
    }

    private static String validate(String type, TextField category, TextArea text, TextField[] options) {
        if (category.getText().isBlank()) return "Enter a category.";
        if (text.getText().isBlank()) return "Enter the question text.";
        if ("mcq".equals(type)) {
            for (TextField f : options) {
                if (f.getText().isBlank()) return "Fill in all four options.";
            }
        }
        return null;
    }

    /** A label beside a control, for one row inside the MCQ box. */
    private static final class HBoxLabel extends javafx.scene.layout.HBox {
        HBoxLabel(String label, javafx.scene.Node control) {
            super(10, new Label(label), control);
            setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }
    }
}
