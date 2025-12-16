import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Loads and parses test case JSON files.
 */
public class TestCaseLoader {

    /**
     * Represents a single test case.
     */
    public static class TestCase {
        public String name;
        public int contextSwitch;
        public int rrQuantum;
        public int agingInterval; // for AG scheduler
        public Process[] processes;
        public Map<String, ExpectedOutput> expectedOutputs; // keyed by scheduler type (SJF, RR, Priority, AG)

        public TestCase() {
            this.expectedOutputs = new HashMap<>();
        }
    }

    /**
     * Represents expected output for a single scheduler run.
     */
    public static class ExpectedOutput {
        public List<String> executionOrder;
        public Map<String, ProcessResult> processResults;
        public double averageWaitingTime;
        public double averageTurnaroundTime;

        public ExpectedOutput() {
            this.executionOrder = new ArrayList<>();
            this.processResults = new HashMap<>();
        }
    }

    /**
     * Represents a single process's expected result.
     */
    public static class ProcessResult {
        public String name;
        public int waitingTime;
        public int turnaroundTime;
        public List<Integer> quantumHistory; // for AG scheduler

        public ProcessResult() {
            this.quantumHistory = new ArrayList<>();
        }
    }

    /**
     * Load a single test case from a JSON file.
     */
    public static TestCase loadTestCase(String filePath) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        JSONObject json = new JSONObject(content);

        TestCase tc = new TestCase();

        // Try to parse as array (AG tests) or single object (Other tests)
        JSONObject testObj = json;
        if (json.has("length") && json.length() == 0) {
            // Empty array, skip
            return null;
        }

        // Handle both single test case and wrapped in array
        if (json.has("input") && !json.has("name")) {
            testObj = json;
        } else if (json.has("input")) {
            testObj = json;
        }

        // Get basic name if available
        if (testObj.has("name")) {
            tc.name = testObj.getString("name");
        } else {
            tc.name = new File(filePath).getName();
        }

        // Parse input
        JSONObject inputObj = testObj.getJSONObject("input");
        tc.contextSwitch = inputObj.optInt("contextSwitch", 1);
        tc.rrQuantum = inputObj.optInt("rrQuantum", 2);
        tc.agingInterval = inputObj.optInt("agingInterval", 5);

        // Parse processes
        JSONArray processesArray = inputObj.getJSONArray("processes");
        tc.processes = new Process[processesArray.length()];
        for (int i = 0; i < processesArray.length(); i++) {
            JSONObject pObj = processesArray.getJSONObject(i);
            String name = pObj.getString("name");
            int arrival = pObj.getInt("arrival");
            int burst = pObj.getInt("burst");
            int priority = pObj.getInt("priority");
            int quantum = pObj.optInt("quantum", 0);
            tc.processes[i] = new Process(name, arrival, burst, priority, quantum);
        }

        // Parse expected outputs
        if (testObj.has("expectedOutput")) {
            JSONObject expectedObj = testObj.getJSONObject("expectedOutput");

            // Check if it's a multi-scheduler test (like test_1.json) or single-scheduler test (like AG_test1.json)
            if (expectedObj.has("SJF") || expectedObj.has("RR") || expectedObj.has("Priority")) {
                // Multi-scheduler format
                if (expectedObj.has("SJF")) {
                    tc.expectedOutputs.put("SJF", parseExpectedOutput(expectedObj.getJSONObject("SJF")));
                }
                if (expectedObj.has("RR")) {
                    tc.expectedOutputs.put("RR", parseExpectedOutput(expectedObj.getJSONObject("RR")));
                }
                if (expectedObj.has("Priority")) {
                    tc.expectedOutputs.put("Priority", parseExpectedOutput(expectedObj.getJSONObject("Priority")));
                }
                if (expectedObj.has("AG")) {
                    tc.expectedOutputs.put("AG", parseExpectedOutput(expectedObj.getJSONObject("AG")));
                }
            } else {
                // Single-scheduler format (AG tests)
                tc.expectedOutputs.put("AG", parseExpectedOutput(expectedObj));
            }
        }

        return tc;
    }

    /**
     * Parse expected output from JSON.
     */
    private static ExpectedOutput parseExpectedOutput(JSONObject obj) {
        ExpectedOutput output = new ExpectedOutput();

        // Parse execution order
        if (obj.has("executionOrder")) {
            JSONArray execArray = obj.getJSONArray("executionOrder");
            for (int i = 0; i < execArray.length(); i++) {
                output.executionOrder.add(execArray.getString(i));
            }
        }

        // Parse process results
        if (obj.has("processResults")) {
            JSONArray resultsArray = obj.getJSONArray("processResults");
            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject pResult = resultsArray.getJSONObject(i);
                ProcessResult pr = new ProcessResult();
                pr.name = pResult.getString("name");
                pr.waitingTime = pResult.getInt("waitingTime");
                pr.turnaroundTime = pResult.getInt("turnaroundTime");

                // Parse quantum history if present
                if (pResult.has("quantumHistory")) {
                    JSONArray qHistArray = pResult.getJSONArray("quantumHistory");
                    for (int j = 0; j < qHistArray.length(); j++) {
                        pr.quantumHistory.add(qHistArray.getInt(j));
                    }
                }

                output.processResults.put(pr.name, pr);
            }
        }

        // Parse averages
        output.averageWaitingTime = obj.optDouble("averageWaitingTime", 0.0);
        output.averageTurnaroundTime = obj.optDouble("averageTurnaroundTime", 0.0);

        return output;
    }

    /**
     * Load all test cases from a directory.
     */
    public static List<TestCase> loadAllTestCases(String dirPath) throws Exception {
        List<TestCase> testCases = new ArrayList<>();
        File dir = new File(dirPath);

        if (!dir.isDirectory()) {
            System.err.println("Error: " + dirPath + " is not a directory.");
            return testCases;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    TestCase tc = loadTestCase(file.getAbsolutePath());
                    if (tc != null) {
                        testCases.add(tc);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading test case " + file.getName() + ": " + e.getMessage());
                }
            }
        }

        return testCases;
    }

    /**
     * Load test cases for a specific scheduler category.
     */
    public static List<TestCase> loadTestCasesByCategory(String category) throws Exception {
        String baseDir = "test_cases";
        String categoryDir;

        switch (category.toLowerCase()) {
            case "ag":
                categoryDir = baseDir + File.separator + "AG";
                break;
            case "other":
            case "other_schedulers":
                categoryDir = baseDir + File.separator + "Other_Schedulers";
                break;
            default:
                System.err.println("Unknown category: " + category);
                return new ArrayList<>();
        }

        return loadAllTestCases(categoryDir);
    }
}

