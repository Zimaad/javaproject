package com.smartinterview.controller;

import com.smartinterview.ai.AiClient;
import com.smartinterview.model.Interview;
import com.smartinterview.model.Question;
import com.smartinterview.navigation.NavigationManager;
import com.smartinterview.service.EvaluationService;
import com.smartinterview.service.InterviewSession;
import javafx.concurrent.Task;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodingRoundController implements NavigationManager.DataReceiver {

    @FXML private Label roundProgressLabel;
    @FXML private Label roleLabel;
    @FXML private Label timerLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    
    @FXML private Label difficultyBadge;
    @FXML private Label categoryBadge;
    @FXML private Label problemTitle;
    @FXML private Label problemText;
    
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Button btnSubmit;
    @FXML private Button btnRun;
    
    @FXML private ComboBox<String> languageCombo;
    @FXML private TextArea codeArea;
    
    @FXML private VBox consolePanel;
    @FXML private Label consoleOutput;

    private Interview currentInterview;
    private List<Question> questions;
    private int currentIndex = 0;
    private Map<Integer, String> codeAnswers = new HashMap<>();
    
    private Timeline timer;
    private int timeRemainingSeconds = 45 * 60; // 45 mins default

    @FXML
    public void initialize() {
        languageCombo.getItems().addAll("Java", "Python", "C++", "JavaScript");
        languageCombo.getSelectionModel().select("Java");
        
        // Save code on typing
        codeArea.textProperty().addListener((obs, oldVal, newVal) -> {
            codeAnswers.put(currentIndex, newVal);
        });
    }

    @Override
    public void receiveData(Object data) {
        if (data instanceof Interview) {
            currentInterview = (Interview) data;
            roleLabel.setText(currentInterview.getRoleName());
            
            // Set round progress text
            List<String> rounds = currentInterview.getRounds();
            int currentRoundIdx = rounds.indexOf("Coding");
            roundProgressLabel.setText(
                ("Round " + (currentRoundIdx + 1) + " of " + rounds.size() + " - CODING CHALLENGE").toUpperCase());
            
            timeRemainingSeconds = currentInterview.getDurationMinutes() * 60; // Allocate total duration to coding if it's the main part
            
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
        questions = InterviewSession.current().getCodingQuestions();
        displayQuestion(0);
    }
    
    private void displayQuestion(int index) {
        if (index < 0 || index >= questions.size()) return;
        currentIndex = index;
        
        Question q = questions.get(index);
        
        categoryBadge.setText(q.getCategory());
        difficultyBadge.setText(q.getDifficulty());
        
        difficultyBadge.getStyleClass().removeAll("badge-easy", "badge-medium", "badge-hard");
        if (q.getDifficulty().equals("Easy")) difficultyBadge.getStyleClass().add("badge-easy");
        else if (q.getDifficulty().equals("Medium")) difficultyBadge.getStyleClass().add("badge-medium");
        else difficultyBadge.getStyleClass().add("badge-hard");
        
        // Parse Title and Text
        String rawText = q.getQuestionText();
        int firstNewLine = rawText.indexOf('\n');
        if (firstNewLine > 0) {
            problemTitle.setText(rawText.substring(0, firstNewLine).trim());
            problemText.setText(rawText.substring(firstNewLine).trim());
        } else {
            problemTitle.setText("Coding Challenge " + (index + 1));
            problemText.setText(rawText);
        }
        
        // Restore code or use template
        if (codeAnswers.containsKey(index)) {
            codeArea.setText(codeAnswers.get(index));
        } else {
            codeArea.setText(q.getCodeTemplate());
        }
        
        // Update Progress
        progressBar.setProgress((double) (index + 1) / questions.size());
        progressLabel.setText((index + 1) + "/" + questions.size());
        
        // Update Navigation
        btnPrev.setDisable(index == 0);
        
        if (index == questions.size() - 1) {
            btnNext.setDisable(true);
            btnSubmit.setText("Submit Round");
        } else {
            btnNext.setDisable(false);
            btnSubmit.setText("Submit & Next");
        }
        
        consolePanel.setVisible(false);
        consolePanel.setManaged(false);
    }
    
    @FXML
    private void onRunCode() {
        consolePanel.setVisible(true);
        consolePanel.setManaged(true);

        String code = codeArea.getText() == null ? "" : codeArea.getText();
        if (code.isBlank()) {
            consoleOutput.setText("Write some code first, then press Run.");
            return;
        }
        if (!AiClient.getInstance().isConfigured()) {
            consoleOutput.setText("Run needs an AI API key, because code is reviewed by the AI rather than executed.\n"
                + "Add a key to config.properties and restart the app.");
            return;
        }

        consoleOutput.setText("Reviewing your solution against example and edge cases…");
        btnRun.setDisable(true);
        Question q = questions.get(currentIndex);
        String language = languageCombo.getValue();

        Task<EvaluationService.CodeReview> review = new Task<>() {
            @Override
            protected EvaluationService.CodeReview call() throws Exception {
                return EvaluationService.getInstance().reviewCode(q, code, language);
            }
        };
        review.setOnSucceeded(e -> {
            EvaluationService.CodeReview r = review.getValue();
            StringBuilder out = new StringBuilder("AI review (the code is traced, not executed)\n\n");
            r.cases().forEach(line -> out.append(line).append('\n'));
            out.append('\n').append(r.passed()).append('/').append(r.total()).append(" cases passed");
            if (!r.summary().isBlank()) {
                out.append("\n\n").append(r.summary());
            }
            consoleOutput.setText(out.toString());
            btnRun.setDisable(false);
        });
        review.setOnFailed(e -> {
            consoleOutput.setText("Review failed: " + review.getException().getMessage());
            btnRun.setDisable(false);
        });
        Thread worker = new Thread(review, "code-review");
        worker.setDaemon(true);
        worker.start();
    }
    
    @FXML
    private void onCloseConsole() {
        consolePanel.setVisible(false);
        consolePanel.setManaged(false);
    }

    @FXML
    private void onPrevQuestion() {
        displayQuestion(currentIndex - 1);
    }

    @FXML
    private void onNextQuestion() {
        displayQuestion(currentIndex + 1);
    }
    
    @FXML
    private void onSubmitSolution() {
        if (currentIndex == questions.size() - 1) {
            submitRound();
        } else {
            onNextQuestion();
        }
    }
    
    private void submitRound() {
        if (timer != null) timer.stop();
        InterviewSession.current().setCodeAnswers(codeAnswers);
        InterviewSession.current().setCodeLanguage(languageCombo.getValue());

        List<String> rounds = currentInterview.getRounds();
        int currentRoundIdx = rounds.indexOf("Coding");
        
        if (currentRoundIdx != -1 && currentRoundIdx < rounds.size() - 1) {
            String nextRound = rounds.get(currentRoundIdx + 1);
            if (nextRound.equals("HR")) {
                NavigationManager.getInstance().navigateTo("hr-round", currentInterview);
            }
        } else {
            NavigationManager.getInstance().navigateTo("evaluation", currentInterview);
        }
    }
}
