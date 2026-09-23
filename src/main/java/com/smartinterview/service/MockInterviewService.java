package com.smartinterview.service;

import com.smartinterview.model.Interview;
import com.smartinterview.model.Question;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Mock interview service — provides dummy questions and interview data.
 * Replace with real API calls later.
 */
public class MockInterviewService {

    private static MockInterviewService instance;

    private MockInterviewService() {}

    public static MockInterviewService getInstance() {
        if (instance == null) {
            instance = new MockInterviewService();
        }
        return instance;
    }

    /**
     * Returns available interview roles.
     */
    public List<Interview> getAvailableRoles() {
        List<Interview> roles = new ArrayList<>();

        Interview java = new Interview("INT001", "Java Developer",
            "Test your Java fundamentals, OOP knowledge, and problem-solving ability.",
            "Medium", 30, 15, Arrays.asList("Java", "OOP", "Collections", "JDBC", "Multithreading"));
        java.setRoleIcon("fab-java");
        roles.add(java);

        Interview swe = new Interview("INT002", "Software Engineer",
            "Comprehensive assessment of CS fundamentals, algorithms, and system design skills.",
            "Hard", 45, 20, Arrays.asList("DSA", "Algorithms", "DBMS", "OS", "System Design"));
        swe.setRoleIcon("fas-laptop-code");
        roles.add(swe);

        Interview data = new Interview("INT003", "Data Analyst",
            "Evaluate your data analysis, SQL proficiency, and statistical reasoning skills.",
            "Medium", 30, 15, Arrays.asList("SQL", "Python", "Statistics", "Data Analysis"));
        data.setRoleIcon("fas-chart-bar");
        roles.add(data);

        return roles;
    }

    /**
     * Returns MCQ questions for a given role.
     */
    public List<Question> getMcqQuestions(String roleId) {
        List<Question> questions = new ArrayList<>();

        questions.add(new Question("Q001", "Java", "Medium",
            "Which of the following is NOT a feature of Java?",
            Arrays.asList("Object-Oriented", "Platform Independent", "Pointer Support", "Multithreaded"),
            2, "Java does not support explicit pointers for security and simplicity reasons."));

        questions.add(new Question("Q002", "Java", "Easy",
            "What is the default value of an int variable in Java?",
            Arrays.asList("null", "0", "1", "undefined"),
            1, "Primitive types in Java have default values. For int, it is 0."));

        questions.add(new Question("Q003", "OOP", "Medium",
            "Which OOP concept is achieved by using method overloading and method overriding?",
            Arrays.asList("Encapsulation", "Polymorphism", "Abstraction", "Inheritance"),
            1, "Polymorphism allows methods to take many forms through overloading (compile-time) and overriding (runtime)."));

        questions.add(new Question("Q004", "Collections", "Hard",
            "What is the time complexity of put() operation in a HashMap?",
            Arrays.asList("O(1)", "O(log n)", "O(n)", "O(1) amortized, O(n) worst case"),
            3, "HashMap put is O(1) amortized, but can degrade to O(n) with hash collisions."));

        questions.add(new Question("Q005", "JDBC", "Medium",
            "Which interface is used to execute parameterized SQL queries in JDBC?",
            Arrays.asList("Statement", "PreparedStatement", "CallableStatement", "ResultSet"),
            1, "PreparedStatement is used for parameterized queries, offering SQL injection protection."));

        questions.add(new Question("Q006", "Java", "Easy",
            "Which keyword is used to prevent a class from being inherited?",
            Arrays.asList("static", "abstract", "final", "private"),
            2, "The 'final' keyword prevents class inheritance when applied to a class declaration."));

        questions.add(new Question("Q007", "Multithreading", "Hard",
            "What happens when two threads access a synchronized method simultaneously?",
            Arrays.asList(
                "Both execute simultaneously",
                "One waits until the other finishes",
                "A compilation error occurs",
                "A DeadlockException is thrown"
            ),
            1, "Synchronized methods allow only one thread at a time. The other thread must wait for the lock to be released."));

        questions.add(new Question("Q008", "OOP", "Medium",
            "Which of the following represents an IS-A relationship?",
            Arrays.asList("Composition", "Aggregation", "Inheritance", "Association"),
            2, "Inheritance represents an IS-A relationship (e.g., Dog IS-A Animal)."));

        questions.add(new Question("Q009", "Collections", "Medium",
            "Which collection does NOT allow duplicate elements?",
            Arrays.asList("ArrayList", "LinkedList", "HashSet", "Vector"),
            2, "HashSet implements the Set interface which by definition does not allow duplicates."));

        questions.add(new Question("Q010", "Java", "Hard",
            "What is the purpose of the 'volatile' keyword in Java?",
            Arrays.asList(
                "Makes a variable constant",
                "Ensures visibility of changes across threads",
                "Prevents garbage collection",
                "Enables method overriding"
            ),
            1, "volatile ensures that changes to a variable are immediately visible to all threads."));

        return questions;
    }

    /**
     * Returns coding questions for a given role.
     */
    public List<Question> getCodingQuestions(String roleId) {
        List<Question> questions = new ArrayList<>();

        Question q1 = new Question();
        q1.setId("CQ001");
        q1.setType("coding");
        q1.setCategory("Arrays");
        q1.setDifficulty("Medium");
        q1.setQuestionText(
            "Two Sum\n\n" +
            "Given an array of integers nums and an integer target, return indices of the two numbers " +
            "such that they add up to target.\n\n" +
            "You may assume that each input would have exactly one solution, and you may not use the " +
            "same element twice.\n\n" +
            "Example 1:\n" +
            "Input: nums = [2, 7, 11, 15], target = 9\n" +
            "Output: [0, 1]\n" +
            "Explanation: Because nums[0] + nums[1] == 9, we return [0, 1].\n\n" +
            "Example 2:\n" +
            "Input: nums = [3, 2, 4], target = 6\n" +
            "Output: [1, 2]\n\n" +
            "Constraints:\n" +
            "• 2 <= nums.length <= 10^4\n" +
            "• -10^9 <= nums[i] <= 10^9\n" +
            "• Only one valid answer exists."
        );
        q1.setCodeTemplate(
            "import java.util.*;\n\n" +
            "public class Solution {\n" +
            "    public int[] twoSum(int[] nums, int target) {\n" +
            "        // Write your solution here\n" +
            "        \n" +
            "    }\n" +
            "}"
        );
        q1.setExpectedOutput("[0, 1]");
        questions.add(q1);

        Question q2 = new Question();
        q2.setId("CQ002");
        q2.setType("coding");
        q2.setCategory("Strings");
        q2.setDifficulty("Easy");
        q2.setQuestionText(
            "Reverse String\n\n" +
            "Write a function that reverses a string. The input string is given as an array of characters.\n\n" +
            "You must do this by modifying the input array in-place with O(1) extra memory.\n\n" +
            "Example 1:\n" +
            "Input: s = ['h','e','l','l','o']\n" +
            "Output: ['o','l','l','e','h']\n\n" +
            "Example 2:\n" +
            "Input: s = ['H','a','n','n','a','h']\n" +
            "Output: ['h','a','n','n','a','H']"
        );
        q2.setCodeTemplate(
            "public class Solution {\n" +
            "    public void reverseString(char[] s) {\n" +
            "        // Write your solution here\n" +
            "        \n" +
            "    }\n" +
            "}"
        );
        questions.add(q2);

        return questions;
    }

    /**
     * Returns HR/Behavioral questions.
     */
    public List<Question> getHrQuestions(String roleId) {
        List<Question> questions = new ArrayList<>();

        Question q1 = new Question();
        q1.setId("HQ001");
        q1.setType("hr");
        q1.setCategory("Behavioral");
        q1.setDifficulty("Medium");
        q1.setQuestionText("Tell me about a time when you had to work with a difficult team member. How did you handle it?");
        q1.setSampleAnswer("Use the STAR method: Situation, Task, Action, Result. Focus on your conflict resolution skills.");
        questions.add(q1);

        Question q2 = new Question();
        q2.setId("HQ002");
        q2.setType("hr");
        q2.setCategory("Behavioral");
        q2.setDifficulty("Easy");
        q2.setQuestionText("Why are you interested in this role? What motivates you to pursue a career in software development?");
        q2.setSampleAnswer("Focus on genuine passion, specific aspects of the role, and how it aligns with your career goals.");
        questions.add(q2);

        Question q3 = new Question();
        q3.setId("HQ003");
        q3.setType("hr");
        q3.setCategory("Situational");
        q3.setDifficulty("Hard");
        q3.setQuestionText("Describe a project where you faced a significant technical challenge. How did you approach and solve it?");
        q3.setSampleAnswer("Describe the problem, your debugging/research process, the solution, and what you learned.");
        questions.add(q3);

        return questions;
    }
}
