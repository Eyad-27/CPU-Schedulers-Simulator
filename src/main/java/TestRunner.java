import java.util.*;

/**
 * Runs test cases and validates results against expected outputs.
 */
public class TestRunner {

    /**
     * Result of running a test case.
     */
    public static class TestResult {
        public String testName;
        public String schedulerType;
        public boolean passed;
        public List<String> failureReasons;
        public ScheduleResult actualResult;
        public TestCaseLoader.ExpectedOutput expectedOutput;

        public TestResult(String testName, String schedulerType) {
            this.testName = testName;
            this.schedulerType = schedulerType;
            this.passed = true;
            this.failureReasons = new ArrayList<>();
        }
    }

    /**
     * Run a single test case with a specific scheduler.
     */
    public static TestResult runTest(TestCaseLoader.TestCase testCase, String schedulerType, Scheduler scheduler) {
        TestResult result = new TestResult(testCase.name, schedulerType);

        try {
            // Make a defensive copy of processes
            Process[] processCopy = new Process[testCase.processes.length];
            for (int i = 0; i < testCase.processes.length; i++) {
                Process p = testCase.processes[i];
                processCopy[i] = new Process(
                    p.getProcessName(),
                    p.getArrivalTime(),
                    p.getBurstTime(),
                    p.getPriority(),
                    p.getQuantum()
                );
            }

            // Execute scheduler
            ScheduleResult actual = scheduler.schedule(processCopy, testCase.contextSwitch);
            result.actualResult = actual;

            // Get expected output
            TestCaseLoader.ExpectedOutput expected = testCase.expectedOutputs.get(schedulerType);
            if (expected == null) {
                result.passed = false;
                result.failureReasons.add("No expected output for scheduler type: " + schedulerType);
                return result;
            }
            result.expectedOutput = expected;

            // Validate execution order
            if (!actual.executionOrder.equals(expected.executionOrder)) {
                result.passed = false;
                result.failureReasons.add(
                    "Execution order mismatch.\n" +
                    "  Expected: " + expected.executionOrder + "\n" +
                    "  Got:      " + actual.executionOrder
                );
            }

            // Validate waiting times
            for (String processName : expected.processResults.keySet()) {
                TestCaseLoader.ProcessResult expectedProc = expected.processResults.get(processName);
                Integer actualWaiting = actual.waitingTimes.get(processName);

                if (actualWaiting == null) {
                    result.passed = false;
                    result.failureReasons.add("Process " + processName + " not found in results");
                } else if (actualWaiting != expectedProc.waitingTime) {
                    result.passed = false;
                    result.failureReasons.add(
                        "Waiting time mismatch for " + processName + ".\n" +
                        "  Expected: " + expectedProc.waitingTime + "\n" +
                        "  Got:      " + actualWaiting
                    );
                }
            }

            // Validate turnaround times
            for (String processName : expected.processResults.keySet()) {
                TestCaseLoader.ProcessResult expectedProc = expected.processResults.get(processName);
                Integer actualTurnaround = actual.turnaroundTimes.get(processName);

                if (actualTurnaround == null) {
                    result.passed = false;
                    result.failureReasons.add("Process " + processName + " not found in results");
                } else if (actualTurnaround != expectedProc.turnaroundTime) {
                    result.passed = false;
                    result.failureReasons.add(
                        "Turnaround time mismatch for " + processName + ".\n" +
                        "  Expected: " + expectedProc.turnaroundTime + "\n" +
                        "  Got:      " + actualTurnaround
                    );
                }
            }

            // Validate averages with small tolerance for floating point
            double tolerance = 0.1;
            if (Math.abs(actual.averageWaiting - expected.averageWaitingTime) > tolerance) {
                result.passed = false;
                result.failureReasons.add(
                    "Average waiting time mismatch.\n" +
                    "  Expected: " + expected.averageWaitingTime + "\n" +
                    "  Got:      " + actual.averageWaiting
                );
            }

            if (Math.abs(actual.averageTurnaround - expected.averageTurnaroundTime) > tolerance) {
                result.passed = false;
                result.failureReasons.add(
                    "Average turnaround time mismatch.\n" +
                    "  Expected: " + expected.averageTurnaroundTime + "\n" +
                    "  Got:      " + actual.averageTurnaround
                );
            }

        } catch (Exception e) {
            result.passed = false;
            result.failureReasons.add("Exception during execution: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Run all tests in a test case file for all applicable schedulers.
     */
    public static List<TestResult> runAllTests(TestCaseLoader.TestCase testCase) {
        List<TestResult> results = new ArrayList<>();

        // Try to run each scheduler type that has expected output
        for (String schedulerType : testCase.expectedOutputs.keySet()) {
            Scheduler scheduler = null;

            try {
                switch (schedulerType) {
                    case "RR":
                        scheduler = new RoundRobinScheduler(testCase.rrQuantum);
                        break;
                    case "SJF":
                        scheduler = new SJFScheduler();
                        break;
                    case "Priority":
                        scheduler = new PriorityScheduler(testCase.agingInterval);
                        break;
                    case "AG":
                        scheduler = new AGScheduler();
                        break;
                    default:
                        continue;
                }

                if (scheduler != null) {
                    TestResult result = runTest(testCase, schedulerType, scheduler);
                    results.add(result);
                }
            } catch (Exception e) {
                TestResult result = new TestResult(testCase.name, schedulerType);
                result.passed = false;
                result.failureReasons.add("Failed to create scheduler: " + e.getMessage());
                results.add(result);
            }
        }

        return results;
    }

    /**
     * Print test results in a formatted way.
     */
    public static void printTestResults(List<TestResult> results) {
        if (results.isEmpty()) {
            System.out.println("No tests to run.");
            return;
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST RESULTS");
        System.out.println("=".repeat(80));

        int passed = 0;
        int failed = 0;

        for (TestResult result : results) {
            if (result.passed) {
                System.out.println("\n✓ PASS: " + result.testName + " [" + result.schedulerType + "]");
                passed++;
            } else {
                System.out.println("\n✗ FAIL: " + result.testName + " [" + result.schedulerType + "]");
                for (String reason : result.failureReasons) {
                    System.out.println("    " + reason);
                }
                failed++;
            }
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("Summary: %d Passed, %d Failed (Total: %d)%n", passed, failed, results.size());
        System.out.println("=".repeat(80));
    }

    /**
     * Print a single test result with detailed output.
     */
    public static void printTestResultDetailed(TestResult result) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST: " + result.testName + " [" + result.schedulerType + "]");
        System.out.println("=".repeat(80));

        if (result.passed) {
            System.out.println("Status: ✓ PASSED");
        } else {
            System.out.println("Status: ✗ FAILED");
            System.out.println("\nFailure Details:");
            for (String reason : result.failureReasons) {
                System.out.println("  - " + reason);
            }
        }

        if (result.actualResult != null) {
            System.out.println("\nActual Output:");
            System.out.println("  Execution Order: " + result.actualResult.executionOrder);
            System.out.println("  Waiting Times: " + result.actualResult.waitingTimes);
            System.out.println("  Turnaround Times: " + result.actualResult.turnaroundTimes);
            System.out.printf("  Average Waiting: %.2f, Average Turnaround: %.2f%n",
                result.actualResult.averageWaiting, result.actualResult.averageTurnaround);
        }

        if (result.expectedOutput != null) {
            System.out.println("\nExpected Output:");
            System.out.println("  Execution Order: " + result.expectedOutput.executionOrder);
            System.out.printf("  Average Waiting: %.2f, Average Turnaround: %.2f%n",
                result.expectedOutput.averageWaitingTime, result.expectedOutput.averageTurnaroundTime);
        }

        System.out.println("=".repeat(80));
    }
}

