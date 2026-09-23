package com.smartinterview.service;

import com.smartinterview.model.InterviewResult;
import com.smartinterview.model.PerformanceData;
import com.smartinterview.model.Question;

import java.util.*;

/**
 * Mock data service — provides dashboard stats, history, and question bank data.
 * Replace with real data layer later.
 */
public class MockDataService {

    private static MockDataService instance;

    private MockDataService() {}

    public static MockDataService getInstance() {
        if (instance == null) {
            instance = new MockDataService();
        }
        return instance;
    }

    // ========== Dashboard Stats ==========

    public int getInterviewsCompleted() { return 12; }
    public double getAverageScore() { return 84.0; }
    public double getBestScore() { return 91.0; }
    public int getQuestionsAnswered() { return 127; }

    // ========== Performance Chart Data ==========

    public List<PerformanceData> getPerformanceHistory() {
        return Arrays.asList(
            new PerformanceData("Interview 1", 68, "2026-07-15"),
            new PerformanceData("Interview 2", 74, "2026-07-28"),
            new PerformanceData("Interview 3", 72, "2026-08-05"),
            new PerformanceData("Interview 4", 81, "2026-08-18"),
            new PerformanceData("Interview 5", 86, "2026-09-01"),
            new PerformanceData("Interview 6", 91, "2026-09-12")
        );
    }

    // ========== Recent Interviews ==========

    public List<InterviewResult> getRecentInterviews() {
        List<InterviewResult> results = new ArrayList<>();

        InterviewResult r1 = new InterviewResult();
        r1.setInterviewId("INT001");
        r1.setRoleName("Java Developer");
        r1.setOverallScore(91);
        r1.setDate("Sep 12, 2026");
        r1.setTimeTakenMinutes(28);
        r1.setRoundScores(Map.of("MCQ", 95.0, "Coding", 88.0, "HR", 90.0));
        r1.setStrengths(Arrays.asList("Strong OOP concepts", "Clean code structure", "Good communication"));
        r1.setWeaknesses(Arrays.asList("Multithreading edge cases", "Complex SQL joins"));
        r1.setRecommendations(Arrays.asList(
            "Practice concurrent programming patterns",
            "Study advanced SQL query optimization",
            "Review design patterns (Strategy, Observer)"
        ));
        results.add(r1);

        InterviewResult r2 = new InterviewResult();
        r2.setInterviewId("INT002");
        r2.setRoleName("Software Engineer");
        r2.setOverallScore(84);
        r2.setDate("Sep 01, 2026");
        r2.setTimeTakenMinutes(42);
        r2.setRoundScores(Map.of("MCQ", 82.0, "Coding", 80.0, "HR", 90.0));
        r2.setStrengths(Arrays.asList("Good algorithmic thinking", "Clear explanations"));
        r2.setWeaknesses(Arrays.asList("Time complexity analysis", "System design depth"));
        r2.setRecommendations(Arrays.asList(
            "Practice dynamic programming problems",
            "Study system design case studies"
        ));
        results.add(r2);

        InterviewResult r3 = new InterviewResult();
        r3.setInterviewId("INT003");
        r3.setRoleName("Data Analyst");
        r3.setOverallScore(76);
        r3.setDate("Aug 18, 2026");
        r3.setTimeTakenMinutes(25);
        r3.setRoundScores(Map.of("MCQ", 80.0, "Coding", 70.0, "HR", 78.0));
        r3.setStrengths(Arrays.asList("Strong SQL basics", "Good statistical intuition"));
        r3.setWeaknesses(Arrays.asList("Advanced aggregations", "Python pandas operations"));
        r3.setRecommendations(Arrays.asList(
            "Practice complex SQL window functions",
            "Work on Python data manipulation projects"
        ));
        results.add(r3);

        return results;
    }

    // ========== Interview History (extended) ==========

    public List<InterviewResult> getInterviewHistory() {
        List<InterviewResult> history = new ArrayList<>(getRecentInterviews());

        InterviewResult r4 = new InterviewResult();
        r4.setInterviewId("INT004");
        r4.setRoleName("Java Developer");
        r4.setOverallScore(81);
        r4.setDate("Aug 05, 2026");
        r4.setTimeTakenMinutes(30);
        history.add(r4);

        InterviewResult r5 = new InterviewResult();
        r5.setInterviewId("INT005");
        r5.setRoleName("Software Engineer");
        r5.setOverallScore(74);
        r5.setDate("Jul 28, 2026");
        r5.setTimeTakenMinutes(44);
        history.add(r5);

        InterviewResult r6 = new InterviewResult();
        r6.setInterviewId("INT006");
        r6.setRoleName("Data Analyst");
        r6.setOverallScore(68);
        r6.setDate("Jul 15, 2026");
        r6.setTimeTakenMinutes(22);
        history.add(r6);

        return history;
    }

    // ========== Mock Evaluation Result ==========

    public InterviewResult generateMockEvaluation(String roleName) {
        InterviewResult result = new InterviewResult();
        result.setInterviewId("INT-NEW-" + System.currentTimeMillis());
        result.setRoleName(roleName);
        result.setOverallScore(85);
        result.setDate("Sep 20, 2026");
        result.setTimeTakenMinutes(28);
        result.setRoundScores(Map.of("MCQ", 88.0, "Coding", 82.0, "HR", 85.0));
        result.setStrengths(Arrays.asList(
            "Strong understanding of core concepts",
            "Good problem-solving approach",
            "Clear and structured communication"
        ));
        result.setWeaknesses(Arrays.asList(
            "Could improve time management",
            "Edge case handling in code"
        ));
        result.setRecommendations(Arrays.asList(
            "Practice timed coding challenges",
            "Review boundary condition testing",
            "Study behavioral interview frameworks (STAR method)"
        ));
        return result;
    }

    // ========== Question Bank ==========

    public List<Question> getQuestionBank() {
        List<Question> bank = new ArrayList<>();
        bank.addAll(MockInterviewService.getInstance().getMcqQuestions("INT001"));
        bank.addAll(MockInterviewService.getInstance().getCodingQuestions("INT001"));
        bank.addAll(MockInterviewService.getInstance().getHrQuestions("INT001"));

        // Add more varied questions
        bank.add(new Question("QB011", "DSA", "Hard",
            "What is the time complexity of searching in a balanced BST?",
            Arrays.asList("O(1)", "O(log n)", "O(n)", "O(n log n)"),
            1, "A balanced BST maintains O(log n) height, making search operations O(log n)."));

        bank.add(new Question("QB012", "DBMS", "Medium",
            "Which normal form eliminates transitive dependencies?",
            Arrays.asList("1NF", "2NF", "3NF", "BCNF"),
            2, "Third Normal Form (3NF) eliminates transitive dependencies."));

        bank.add(new Question("QB013", "OS", "Medium",
            "Which scheduling algorithm is optimal for minimizing average waiting time?",
            Arrays.asList("FCFS", "SJF", "Round Robin", "Priority"),
            1, "Shortest Job First (SJF) is proven optimal for minimizing average waiting time."));

        bank.add(new Question("QB014", "SQL", "Easy",
            "Which SQL clause is used to filter groups?",
            Arrays.asList("WHERE", "HAVING", "GROUP BY", "ORDER BY"),
            1, "HAVING is used to filter groups after GROUP BY, while WHERE filters individual rows."));

        bank.add(new Question("QB015", "Java", "Medium",
            "What is the difference between == and .equals() in Java?",
            Arrays.asList(
                "No difference",
                "== checks reference, .equals() checks value",
                "== checks value, .equals() checks reference",
                "Both check values only"
            ),
            1, "== compares object references (memory addresses), while .equals() compares object content/values."));

        return bank;
    }

    // ========== Admin Stats ==========

    public int getTotalUsers() { return 248; }
    public int getTotalInterviews() { return 1547; }
    public int getQuestionsInBank() { return 350; }
    public double getGlobalAverageScore() { return 72.5; }
}
