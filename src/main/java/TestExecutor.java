import java.util.*;
import java.io.File;

/**
 * Handles test case execution and menu for running automated tests.
 */
public class TestExecutor {

    /**
     * Display the test execution menu and handle user choices.
     */
    public static void displayTestMenu(Scanner scanner) {
        boolean continueMenu = true;

        while (continueMenu) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("AUTOMATED TEST EXECUTION");
            System.out.println("=".repeat(80));
            System.out.println("1) Run ALL test cases (Other Schedulers)");
            System.out.println("2) Run specific test case (Other Schedulers)");
            System.out.println("3) Run ALL test cases (AG Scheduler)");
            System.out.println("4) Run specific test case (AG Scheduler)");
            System.out.println("5) Back to main menu");
            System.out.print("\nYour choice: ");

            int choice;
            try {
                choice = Integer.parseInt(scanner.next());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            switch (choice) {
                case 1:
                    runAllOtherSchedulerTests();
                    break;
                case 2:
                    runSpecificOtherSchedulerTest(scanner);
                    break;
                case 3:
                    runAllAGTests();
                    break;
                case 4:
                    runSpecificAGTest(scanner);
                    break;
                case 5:
                    continueMenu = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Run all Other Schedulers test cases.
     */
    private static void runAllOtherSchedulerTests() {
        try {
            System.out.println("\nLoading test cases from test_cases/Other_Schedulers/...");
            List<TestCaseLoader.TestCase> testCases = TestCaseLoader.loadTestCasesByCategory("other");

            if (testCases.isEmpty()) {
                System.out.println("No test cases found in test_cases/Other_Schedulers/");
                return;
            }

            System.out.println("Loaded " + testCases.size() + " test cases.");
            List<TestRunner.TestResult> allResults = new ArrayList<>();

            for (TestCaseLoader.TestCase testCase : testCases) {
                System.out.println("\nRunning: " + testCase.name);
                List<TestRunner.TestResult> results = TestRunner.runAllTests(testCase);
                allResults.addAll(results);
            }

            // Print detailed results only
            for (TestRunner.TestResult result : allResults) {
                TestRunner.printTestResultDetailed(result);
            }
            // Then print the summary line at the end
            printSummary(allResults);

        } catch (Exception e) {
            System.err.println("Error running tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Run a specific Other Schedulers test case.
     */
    private static void runSpecificOtherSchedulerTest(Scanner scanner) {
        try {
            System.out.println("\nAvailable test cases:");
            List<TestCaseLoader.TestCase> testCases = TestCaseLoader.loadTestCasesByCategory("other");

            if (testCases.isEmpty()) {
                System.out.println("No test cases found in test_cases/Other_Schedulers/");
                return;
            }

            for (int i = 0; i < testCases.size(); i++) {
                System.out.println((i + 1) + ") " + testCases.get(i).name);
            }

            System.out.print("\nSelect test case (1-" + testCases.size() + "): ");
            int testChoice;
            try {
                testChoice = Integer.parseInt(scanner.next());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
                return;
            }

            if (testChoice < 1 || testChoice > testCases.size()) {
                System.out.println("Invalid choice.");
                return;
            }

            TestCaseLoader.TestCase selectedTest = testCases.get(testChoice - 1);
            System.out.println("\nRunning: " + selectedTest.name);
            List<TestRunner.TestResult> results = TestRunner.runAllTests(selectedTest);

            // Print detailed results only
            for (TestRunner.TestResult result : results) {
                TestRunner.printTestResultDetailed(result);
            }
            // Then print the summary line at the end
            printSummary(results);

        } catch (Exception e) {
            System.err.println("Error running test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Run all AG Scheduler test cases.
     */
    private static void runAllAGTests() {
        try {
            System.out.println("\nLoading test cases from test_cases/AG/...");
            List<TestCaseLoader.TestCase> testCases = TestCaseLoader.loadTestCasesByCategory("ag");

            if (testCases.isEmpty()) {
                System.out.println("No test cases found in test_cases/AG/");
                return;
            }

            System.out.println("Loaded " + testCases.size() + " test cases.");
            List<TestRunner.TestResult> allResults = new ArrayList<>();

            for (TestCaseLoader.TestCase testCase : testCases) {
                System.out.println("\nRunning: " + testCase.name);
                List<TestRunner.TestResult> results = TestRunner.runAllTests(testCase);
                allResults.addAll(results);
            }

            // Print detailed results only
            for (TestRunner.TestResult result : allResults) {
                TestRunner.printTestResultDetailed(result);
            }
            // Then print the summary line at the end
            printSummary(allResults);

        } catch (Exception e) {
            System.err.println("Error running tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Run a specific AG Scheduler test case.
     */
    private static void runSpecificAGTest(Scanner scanner) {
        try {
            System.out.println("\nAvailable test cases:");
            List<TestCaseLoader.TestCase> testCases = TestCaseLoader.loadTestCasesByCategory("ag");

            if (testCases.isEmpty()) {
                System.out.println("No test cases found in test_cases/AG/");
                return;
            }

            for (int i = 0; i < testCases.size(); i++) {
                System.out.println((i + 1) + ") " + testCases.get(i).name);
            }

            System.out.print("\nSelect test case (1-" + testCases.size() + "): ");
            int testChoice;
            try {
                testChoice = Integer.parseInt(scanner.next());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
                return;
            }

            if (testChoice < 1 || testChoice > testCases.size()) {
                System.out.println("Invalid choice.");
                return;
            }

            TestCaseLoader.TestCase selectedTest = testCases.get(testChoice - 1);
            System.out.println("\nRunning: " + selectedTest.name);
            List<TestRunner.TestResult> results = TestRunner.runAllTests(selectedTest);

            // Print detailed results only
            for (TestRunner.TestResult result : results) {
                TestRunner.printTestResultDetailed(result);
            }
            // Then print the summary line at the end
            printSummary(results);

        } catch (Exception e) {
            System.err.println("Error running test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Print a compact summary line at the very end.
     */
    private static void printSummary(List<TestRunner.TestResult> results) {
        int passed = 0;
        int failed = 0;
        for (TestRunner.TestResult r : results) {
            if (r.passed) passed++; else failed++;
        }
        System.out.println("\n" + "=".repeat(80));
        System.out.printf("Summary: %d Passed, %d Failed (Total: %d)%n", passed, failed, results.size());
        System.out.println("=".repeat(80));
    }
}
