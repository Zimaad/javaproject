package com.smartinterview.db;

import com.smartinterview.ai.AiConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

/** Fills a brand-new database with the starter question bank and an administrator. */
final class Seeder {

    @FunctionalInterface
    interface ConnectionSource {
        Connection open() throws SQLException;
    }

    static final String ADMIN_EMAIL = "admin@smartinterview.com";

    private Seeder() {}

    static void seedIfEmpty(ConnectionSource source) throws SQLException {
        try (Connection c = source.open()) {
            if (count(c, "questions") == 0) {
                seedQuestions(c);
            }
            if (count(c, "users") == 0) {
                seedAdmin(c);
            }
        }
    }

    private static int count(Connection c, String table) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void seedAdmin(Connection c) throws SQLException {
        String password = AiConfig.get("admin.password", "admin123");
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO users (id, full_name, email, password_hash, college, degree, year, role, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'admin', ?)""")) {
            ps.setString(1, "ADM-" + UUID.randomUUID().toString().substring(0, 8));
            ps.setString(2, "Administrator");
            ps.setString(3, ADMIN_EMAIL);
            ps.setString(4, PasswordHasher.hash(password));
            ps.setString(5, "");
            ps.setString(6, "");
            ps.setString(7, "");
            ps.setLong(8, System.currentTimeMillis());
            ps.executeUpdate();
        }
        System.out.println("Created administrator account " + ADMIN_EMAIL
                + " - change its password under Settings after signing in.");
    }

    private static void seedQuestions(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO questions (id, type, category, difficulty, text, options_json, correct_index,
                                       code_template, expected_output, sample_answer, explanation, source)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'seed')""")) {

            // ---- MCQ ----
            mcq(ps, "Q001", "Java", "Medium", "Which of the following is NOT a feature of Java?",
                List.of("Object-Oriented", "Platform Independent", "Pointer Support", "Multithreaded"), 2,
                "Java does not support explicit pointers for security and simplicity reasons.");
            mcq(ps, "Q002", "Java", "Easy", "What is the default value of an int variable in Java?",
                List.of("null", "0", "1", "undefined"), 1,
                "Primitive types in Java have default values. For int, it is 0.");
            mcq(ps, "Q003", "OOP", "Medium",
                "Which OOP concept is achieved by using method overloading and method overriding?",
                List.of("Encapsulation", "Polymorphism", "Abstraction", "Inheritance"), 1,
                "Polymorphism allows methods to take many forms through overloading (compile-time) and overriding (runtime).");
            mcq(ps, "Q004", "Collections", "Hard", "What is the time complexity of put() operation in a HashMap?",
                List.of("O(1)", "O(log n)", "O(n)", "O(1) amortized, O(n) worst case"), 3,
                "HashMap put is O(1) amortized, but can degrade to O(n) with hash collisions.");
            mcq(ps, "Q005", "JDBC", "Medium", "Which interface is used to execute parameterized SQL queries in JDBC?",
                List.of("Statement", "PreparedStatement", "CallableStatement", "ResultSet"), 1,
                "PreparedStatement is used for parameterized queries, offering SQL injection protection.");
            mcq(ps, "Q006", "Java", "Easy", "Which keyword is used to prevent a class from being inherited?",
                List.of("static", "abstract", "final", "private"), 2,
                "The 'final' keyword prevents class inheritance when applied to a class declaration.");
            mcq(ps, "Q007", "Multithreading", "Hard",
                "What happens when two threads access a synchronized method simultaneously?",
                List.of("Both execute simultaneously", "One waits until the other finishes",
                        "A compilation error occurs", "A DeadlockException is thrown"), 1,
                "Synchronized methods allow only one thread at a time. The other thread must wait for the lock to be released.");
            mcq(ps, "Q008", "OOP", "Medium", "Which of the following represents an IS-A relationship?",
                List.of("Composition", "Aggregation", "Inheritance", "Association"), 2,
                "Inheritance represents an IS-A relationship (e.g., Dog IS-A Animal).");
            mcq(ps, "Q009", "Collections", "Medium", "Which collection does NOT allow duplicate elements?",
                List.of("ArrayList", "LinkedList", "HashSet", "Vector"), 2,
                "HashSet implements the Set interface which by definition does not allow duplicates.");
            mcq(ps, "Q010", "Java", "Hard", "What is the purpose of the 'volatile' keyword in Java?",
                List.of("Makes a variable constant", "Ensures visibility of changes across threads",
                        "Prevents garbage collection", "Enables method overriding"), 1,
                "volatile ensures that changes to a variable are immediately visible to all threads.");
            mcq(ps, "QB011", "DSA", "Hard", "What is the time complexity of searching in a balanced BST?",
                List.of("O(1)", "O(log n)", "O(n)", "O(n log n)"), 1,
                "A balanced BST maintains O(log n) height, making search operations O(log n).");
            mcq(ps, "QB012", "DBMS", "Medium", "Which normal form eliminates transitive dependencies?",
                List.of("1NF", "2NF", "3NF", "BCNF"), 2,
                "Third Normal Form (3NF) eliminates transitive dependencies.");
            mcq(ps, "QB013", "OS", "Medium",
                "Which scheduling algorithm is optimal for minimizing average waiting time?",
                List.of("FCFS", "SJF", "Round Robin", "Priority"), 1,
                "Shortest Job First (SJF) is proven optimal for minimizing average waiting time.");
            mcq(ps, "QB014", "SQL", "Easy", "Which SQL clause is used to filter groups?",
                List.of("WHERE", "HAVING", "GROUP BY", "ORDER BY"), 1,
                "HAVING is used to filter groups after GROUP BY, while WHERE filters individual rows.");
            mcq(ps, "QB015", "Java", "Medium", "What is the difference between == and .equals() in Java?",
                List.of("No difference", "== checks reference, .equals() checks value",
                        "== checks value, .equals() checks reference", "Both check values only"), 1,
                "== compares object references (memory addresses), while .equals() compares object content/values.");
            mcq(ps, "QB016", "DSA", "Easy", "Which data structure uses LIFO ordering?",
                List.of("Queue", "Stack", "Heap", "Linked list"), 1,
                "A stack removes the most recently added element first (Last In, First Out).");
            mcq(ps, "QB017", "Algorithms", "Medium", "What is the worst-case time complexity of quicksort?",
                List.of("O(n)", "O(n log n)", "O(n^2)", "O(log n)"), 2,
                "Quicksort degrades to O(n^2) when pivots are consistently the smallest or largest element.");
            mcq(ps, "QB018", "Algorithms", "Hard", "Which algorithm finds shortest paths from one source in a graph with non-negative weights?",
                List.of("Kruskal's", "Dijkstra's", "Prim's", "Topological sort"), 1,
                "Dijkstra's algorithm computes single-source shortest paths when edge weights are non-negative.");
            mcq(ps, "QB019", "System Design", "Medium", "What is the main purpose of a load balancer?",
                List.of("Encrypt traffic", "Distribute requests across servers", "Store session data", "Compile code"), 1,
                "A load balancer spreads incoming requests over several servers to improve availability and throughput.");
            mcq(ps, "QB020", "Python", "Easy", "Which of these is an immutable type in Python?",
                List.of("list", "dict", "set", "tuple"), 3,
                "Tuples cannot be modified after creation, unlike lists, dicts and sets.");
            mcq(ps, "QB021", "Python", "Medium", "What does len(set([1, 2, 2, 3])) return?",
                List.of("4", "3", "2", "1"), 1,
                "A set drops duplicates, leaving {1, 2, 3}, which has length 3.");
            mcq(ps, "QB022", "Statistics", "Easy", "Which measure of central tendency is least affected by outliers?",
                List.of("Mean", "Median", "Range", "Variance"), 1,
                "The median depends on the middle value, so extreme values barely move it.");
            mcq(ps, "QB023", "Statistics", "Medium", "A p-value of 0.03 at a 5% significance level means you should:",
                List.of("Fail to reject the null hypothesis", "Reject the null hypothesis",
                        "Accept the alternative with certainty", "Collect no more data"), 1,
                "0.03 < 0.05, so the result is statistically significant and the null hypothesis is rejected.");
            mcq(ps, "QB024", "Data Analysis", "Medium", "Which chart best shows the distribution of a single numeric variable?",
                List.of("Pie chart", "Histogram", "Line chart", "Scatter plot"), 1,
                "A histogram bins values and shows how often each range occurs.");
            mcq(ps, "QB025", "SQL", "Medium", "Which JOIN returns all rows from the left table even without a match on the right?",
                List.of("INNER JOIN", "LEFT JOIN", "CROSS JOIN", "SELF JOIN"), 1,
                "LEFT JOIN keeps every left-table row, filling unmatched right-side columns with NULL.");
            mcq(ps, "QB026", "SQL", "Hard", "Which function assigns a rank to rows within a partition?",
                List.of("COUNT()", "ROW_NUMBER()", "SUM()", "COALESCE()"), 1,
                "ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...) numbers rows within each partition.");

            // ---- Coding ----
            coding(ps, "CQ001", "Arrays", "Medium", """
                Two Sum

                Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.

                You may assume that each input would have exactly one solution, and you may not use the same element twice.

                Example 1:
                Input: nums = [2, 7, 11, 15], target = 9
                Output: [0, 1]
                Explanation: Because nums[0] + nums[1] == 9, we return [0, 1].

                Example 2:
                Input: nums = [3, 2, 4], target = 6
                Output: [1, 2]

                Constraints:
                • 2 <= nums.length <= 10^4
                • -10^9 <= nums[i] <= 10^9
                • Only one valid answer exists.""", """
                import java.util.*;

                public class Solution {
                    public int[] twoSum(int[] nums, int target) {
                        // Write your solution here
                       \s
                    }
                }""", "[0, 1]");
            coding(ps, "CQ002", "Strings", "Easy", """
                Reverse String

                Write a function that reverses a string. The input string is given as an array of characters.

                You must do this by modifying the input array in-place with O(1) extra memory.

                Example 1:
                Input: s = ['h','e','l','l','o']
                Output: ['o','l','l','e','h']

                Example 2:
                Input: s = ['H','a','n','n','a','h']
                Output: ['h','a','n','n','a','H']""", """
                public class Solution {
                    public void reverseString(char[] s) {
                        // Write your solution here
                       \s
                    }
                }""", null);
            coding(ps, "CQ003", "Strings", "Medium", """
                Valid Parentheses

                Given a string s containing just the characters '(', ')', '{', '}', '[' and ']', determine if the input string is valid.

                A string is valid if open brackets are closed by the same type of bracket, and in the correct order.

                Example 1:
                Input: s = "()[]{}"
                Output: true

                Example 2:
                Input: s = "(]"
                Output: false""", """
                import java.util.*;

                public class Solution {
                    public boolean isValid(String s) {
                        // Write your solution here
                       \s
                    }
                }""", "true");

            // ---- HR ----
            hr(ps, "HQ001", "Behavioral", "Medium",
                "Tell me about a time when you had to work with a difficult team member. How did you handle it?",
                "Use the STAR method: Situation, Task, Action, Result. Focus on your conflict resolution skills.");
            hr(ps, "HQ002", "Behavioral", "Easy",
                "Why are you interested in this role? What motivates you to pursue a career in software development?",
                "Focus on genuine passion, specific aspects of the role, and how it aligns with your career goals.");
            hr(ps, "HQ003", "Situational", "Hard",
                "Describe a project where you faced a significant technical challenge. How did you approach and solve it?",
                "Describe the problem, your debugging/research process, the solution, and what you learned.");
            hr(ps, "HQ004", "Behavioral", "Medium",
                "Tell me about a time you missed a deadline or made a mistake. What did you do afterwards?",
                "Own the mistake, explain how you limited the impact, and describe what you changed to prevent a repeat.");
            hr(ps, "HQ005", "Situational", "Medium",
                "You disagree with a teammate's technical approach. How would you handle it?",
                "Show that you listen first, back opinions with evidence, and commit to the team's decision.");

            ps.executeBatch();
        }
    }

    private static void mcq(PreparedStatement ps, String id, String category, String difficulty, String text,
                            List<String> options, int correct, String explanation) throws SQLException {
        ps.setString(1, id);
        ps.setString(2, "mcq");
        ps.setString(3, category);
        ps.setString(4, difficulty);
        ps.setString(5, text);
        ps.setString(6, Database.toJson(options));
        ps.setInt(7, correct);
        ps.setString(8, null);
        ps.setString(9, null);
        ps.setString(10, null);
        ps.setString(11, explanation);
        ps.addBatch();
    }

    private static void coding(PreparedStatement ps, String id, String category, String difficulty, String text,
                               String template, String expected) throws SQLException {
        ps.setString(1, id);
        ps.setString(2, "coding");
        ps.setString(3, category);
        ps.setString(4, difficulty);
        ps.setString(5, text.strip());
        ps.setString(6, null);
        ps.setInt(7, 0);
        ps.setString(8, template.strip());
        ps.setString(9, expected);
        ps.setString(10, null);
        ps.setString(11, null);
        ps.addBatch();
    }

    private static void hr(PreparedStatement ps, String id, String category, String difficulty, String text,
                           String sample) throws SQLException {
        ps.setString(1, id);
        ps.setString(2, "hr");
        ps.setString(3, category);
        ps.setString(4, difficulty);
        ps.setString(5, text);
        ps.setString(6, null);
        ps.setInt(7, 0);
        ps.setString(8, null);
        ps.setString(9, null);
        ps.setString(10, sample);
        ps.setString(11, null);
        ps.addBatch();
    }
}
