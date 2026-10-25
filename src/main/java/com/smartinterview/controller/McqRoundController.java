package com.smartinterview.controller;

import com.smartinterview.model.Interview;
import com.smartinterview.model.Question;
import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.InterviewSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class McqRoundController implements NavigationManager.DataReceiver {

    @FXML private Label roleLabel;
    @FXML private Label timerLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    
    @FXML private FlowPane qNavPane;
    @FXML private Label qNumberLabel;
    @FXML private Label categoryBadge;
    @FXML private Label difficultyBadge;
    @FXML private Label questionTextLabel;
    @FXML private VBox optionsContainer;
    
    @FXML private Button btnPrev;
    @FXML private Button btnNext;

    private Interview currentInterview;
    private List<Question> questions;
    private int currentIndex = 0;
    private Map<Integer, Integer> answers = new HashMap<>(); // Question Index -> Option Index
    private Map<Integer, Button> navButtons = new HashMap<>();
    
    private Timeline timer;
    private int timeRemainingSeconds = 15 * 60; // 15 mins default

    @FXML
    public void initialize() {
        // Will be configured when data is received
    }

    @Override
    public void receiveData(Object data) {
        if (data instanceof Interview) {
            currentInterview = (Interview) data;
            roleLabel.setText(currentInterview.getRoleName() + " - MCQ Round");
            
            // Adjust time per questions (e.g. 1.5 mins per question)
            timeRemainingSeconds = currentInterview.getQuestionCount() * 90;
            
            startTimer();
            loadQuestions();
        }
    }
    
    private void startTimer() {
        if (timer != null) timer.stop();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeRemainingSeconds--;
            int mins = timeRemainingSeconds / 60;
            int secs = timeRemainingSeconds % 60;
            timerLabel.setText(String.format("%02d:%02d", mins, secs));
            if (timeRemainingSeconds <= 60) {
                if (!timerLabel.getStyleClass().contains("timer-display-critical")) {
                    timerLabel.getStyleClass().add("timer-display-critical");
                }
            }
            if (timeRemainingSeconds <= 0) {
                timer.stop();
                submitRound();
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void loadQuestions() {
        // The session already holds the questions picked when the interview started.
        questions = InterviewSession.current().getMcqQuestions();

        buildNavigator();
        displayQuestion(0);
    }
    
    private void buildNavigator() {
        qNavPane.getChildren().clear();
        navButtons.clear();
        
        for (int i = 0; i < questions.size(); i++) {
            Button btn = new Button(String.valueOf(i + 1));
            btn.getStyleClass().add("q-nav-btn");
            final int idx = i;
            btn.setOnAction(e -> displayQuestion(idx));
            
            navButtons.put(i, btn);
            qNavPane.getChildren().add(btn);
        }
    }

    private void displayQuestion(int index) {
        if (index < 0 || index >= questions.size()) return;
        currentIndex = index;
        
        Question q = questions.get(index);
        
        qNumberLabel.setText("Question " + (index + 1));
        categoryBadge.setText(q.getCategory());
        difficultyBadge.setText(q.getDifficulty());
        
        // Update difficulty badge color
        difficultyBadge.getStyleClass().removeAll("badge-easy", "badge-medium", "badge-hard");
        if (q.getDifficulty().equals("Easy")) difficultyBadge.getStyleClass().add("badge-easy");
        else if (q.getDifficulty().equals("Medium")) difficultyBadge.getStyleClass().add("badge-medium");
        else difficultyBadge.getStyleClass().add("badge-hard");
        
        questionTextLabel.setText(q.getQuestionText());
        
        buildOptions(q, index);
        
        // Update Progress
        progressBar.setProgress((double) (index + 1) / questions.size());
        progressLabel.setText((index + 1) + "/" + questions.size());
        
        // Update Navigation Buttons
        btnPrev.setDisable(index == 0);
        boolean lastQuestion = index == questions.size() - 1;
        btnNext.setText(lastQuestion ? "Submit round" : "Next question");
        btnNext.getStyleClass().setAll("button", lastQuestion ? "btn-success" : "btn-primary");
        if (btnNext.getGraphic() instanceof org.kordamp.ikonli.javafx.FontIcon arrow) {
            arrow.setIconLiteral(lastQuestion ? "mdi2c-check" : "mdi2a-arrow-right");
        }
        
        updateNavigatorHighlight();
    }
    
    private void buildOptions(Question q, int qIndex) {
        optionsContainer.getChildren().clear();
        
        String[] labels = {"A", "B", "C", "D"};
        Integer selectedOpt = answers.get(qIndex);
        
        for (int i = 0; i < q.getOptions().size(); i++) {
            HBox optionBox = new HBox(12);
            optionBox.getStyleClass().add("mcq-option");
            if (selectedOpt != null && selectedOpt == i) {
                optionBox.getStyleClass().add("mcq-option-selected");
            }
            
            Label lblKey = new Label(labels[i]);
            lblKey.getStyleClass().add("option-key");
            
            Label lblVal = new Label(q.getOptions().get(i));
            lblVal.getStyleClass().add("option-text");
            lblVal.setWrapText(true);
            
            optionBox.getChildren().addAll(lblKey, lblVal);
            
            final int optIdx = i;
            Runnable select = () -> {
                answers.put(qIndex, optIdx);
                buildOptions(q, qIndex);   // re-render to show the selection
                updateNavigatorHighlight();
            };

            optionBox.setOnMouseClicked(e -> select.run());

            // An option is a real control, so it must be reachable and
            // selectable from the keyboard, not by mouse only.
            optionBox.setFocusTraversable(true);
            optionBox.setOnKeyPressed(ev -> {
                if (ev.getCode() == javafx.scene.input.KeyCode.SPACE
                    || ev.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    select.run();
                    ev.consume();
                }
            });
            
            optionsContainer.getChildren().add(optionBox);
        }
    }
    
    private void updateNavigatorHighlight() {
        for (int i = 0; i < questions.size(); i++) {
            Button btn = navButtons.get(i);
            btn.getStyleClass().removeAll("q-nav-btn-active", "q-nav-btn-answered");
            
            if (i == currentIndex) {
                btn.getStyleClass().add("q-nav-btn-active");
            } else if (answers.containsKey(i)) {
                btn.getStyleClass().add("q-nav-btn-answered");
            }
        }
    }

    @FXML
    private void onPrevQuestion() {
        displayQuestion(currentIndex - 1);
    }

    @FXML
    private void onNextQuestion() {
        if (currentIndex == questions.size() - 1) {
            submitRound();
        } else {
            displayQuestion(currentIndex + 1);
        }
    }
    
    private void submitRound() {
        if (timer != null) timer.stop();
        InterviewSession.current().setMcqAnswers(answers);

        // Find next round
        List<String> rounds = currentInterview.getRounds();
        int currentRoundIdx = rounds.indexOf("MCQ");
        
        if (currentRoundIdx != -1 && currentRoundIdx < rounds.size() - 1) {
            String nextRound = rounds.get(currentRoundIdx + 1);
            if (nextRound.equals("Coding")) {
                NavigationManager.getInstance().navigateTo("coding-round", currentInterview);
            } else if (nextRound.equals("HR")) {
                NavigationManager.getInstance().navigateTo("hr-round", currentInterview);
            }
        } else {
            // This was the last round
            NavigationManager.getInstance().navigateTo("evaluation", currentInterview);
        }
    }
}
