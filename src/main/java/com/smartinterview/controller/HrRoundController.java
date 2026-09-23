package com.smartinterview.controller;

import com.smartinterview.model.Interview;
import com.smartinterview.model.Question;
import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.MockInterviewService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HrRoundController implements NavigationManager.DataReceiver {

    @FXML private Label roundProgressLabel;
    @FXML private Label roleLabel;
    @FXML private Label timerLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    
    @FXML private Label categoryBadge;
    @FXML private Label qNumberLabel;
    @FXML private Label questionTextLabel;
    @FXML private TextArea answerArea;
    @FXML private Label charCountLabel;
    
    @FXML private Button btnPrev;
    @FXML private Button btnNext;

    private Interview currentInterview;
    private List<Question> questions;
    private int currentIndex = 0;
    private Map<Integer, String> answers = new HashMap<>();
    
    private Timeline timer;
    private int timeRemainingSeconds = 15 * 60; // 15 mins default

    @FXML
    public void initialize() {
        answerArea.textProperty().addListener((obs, oldText, newText) -> {
            answers.put(currentIndex, newText);
            charCountLabel.setText(newText.length() + " / 2000");
            // Warn as the limit approaches, not once it has been passed.
            boolean nearLimit = newText.length() > 1800;
            if (nearLimit && !charCountLabel.getStyleClass().contains("char-count-warning")) {
                charCountLabel.getStyleClass().add("char-count-warning");
            } else if (!nearLimit) {
                charCountLabel.getStyleClass().remove("char-count-warning");
            }
        });
    }

    @Override
    public void receiveData(Object data) {
        if (data instanceof Interview) {
            currentInterview = (Interview) data;
            roleLabel.setText(currentInterview.getRoleName());
            
            // Set round progress text
            List<String> rounds = currentInterview.getRounds();
            int currentRoundIdx = rounds.indexOf("HR");
            roundProgressLabel.setText(
                ("Round " + (currentRoundIdx + 1) + " of " + rounds.size() + " - HR & BEHAVIOURAL").toUpperCase());
            
            timeRemainingSeconds = currentInterview.getQuestionCount() * 120; // 2 mins per hr question
            
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
        questions = MockInterviewService.getInstance().getHrQuestions("INT001");
        
        // Truncate to match user config (assuming HR gets fewer questions naturally)
        int targetCount = Math.min(questions.size(), Math.max(3, currentInterview.getQuestionCount() / 3));
        if (questions.size() > targetCount) {
            questions = questions.subList(0, targetCount);
        }
        
        displayQuestion(0);
    }
    
    private void displayQuestion(int index) {
        if (index < 0 || index >= questions.size()) return;
        currentIndex = index;
        
        Question q = questions.get(index);
        
        categoryBadge.setText(q.getCategory());
        qNumberLabel.setText((index + 1) + "/" + questions.size());
        questionTextLabel.setText(q.getQuestionText());
        
        if (answers.containsKey(index)) {
            answerArea.setText(answers.get(index));
        } else {
            answerArea.setText("");
        }
        
        // Update Progress
        progressBar.setProgress((double) (index + 1) / questions.size());
        progressLabel.setText((index + 1) + "/" + questions.size());
        
        // Update Navigation
        btnPrev.setDisable(index == 0);
        
        if (index == questions.size() - 1) {
            btnNext.setText("Submit Interview");
            btnNext.getStyleClass().setAll("button", "btn-success");
        } else {
            btnNext.setText("Next question");
            btnNext.getStyleClass().setAll("button", "btn-primary");
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
        // Since HR is typically last, we go to evaluation
        NavigationManager.getInstance().navigateTo("evaluation", currentInterview);
    }
}
