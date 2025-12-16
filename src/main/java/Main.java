import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("CPU Schedulers Simulator");

        // Inputs
        int n = readInt(scanner, "Enter number of processes: ");
        int rrQuantum = readInt(scanner, "Enter Round Robin Time Quantum: ");
        int contextSwitch = readInt(scanner, "Enter Context Switching delay: ");

        Process[] processes = new Process[n];
        for (int i = 0; i < n; i++) {
            System.out.println("Process #" + (i + 1));
            System.out.print("  Name: ");
            String name = scanner.next();
            int arrival = readInt(scanner, "  Arrival Time: ");
            int burst = readInt(scanner, "  Burst Time: ");
            int priority = readInt(scanner, "  Priority: ");
            processes[i] = new Process(name, arrival, burst, priority);
        }

        // Menu
        System.out.println("\nSelect Mode:");
        System.out.println("  1) Manual Input");
        System.out.println("  2) Automated Test Cases");
        int modeChoice = readInt(scanner, "Your choice: ");

        if (modeChoice == 2) {
            TestExecutor.displayTestMenu(scanner);
            scanner.close();
            return;
        }

        System.out.println("\nSelect Scheduler:");
        System.out.println("  1) Round Robin");
        System.out.println("  2) SJF (Preemptive) - stub");
        System.out.println("  3) Priority (Preemptive) - stub");
        System.out.println("  4) AG Scheduler - stub");
        int choice = readInt(scanner, "Your choice: ");

        ScheduleResult result = null;
        switch (choice) {
            case 1:
                Scheduler rr = new RoundRobinScheduler(rrQuantum);
                result = rr.schedule(processes, contextSwitch);
                break;
            case 2:
                System.out.println("SJF Scheduler is not implemented yet (assigned to Mohamed Tarek).");
                break;
            case 3:
                System.out.println("Priority Scheduler is not implemented yet (assigned to Mohamed Waleed).");
                break;
            case 4:
                System.out.println("AG Scheduler is not implemented yet (assigned to Yassin and Pedro).");
                break;
            default:
                System.out.println("Invalid choice.");
        }

        if (result != null) {
            printResult(result);
        }

        scanner.close();
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.next());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private static void printResult(ScheduleResult result) {
        System.out.println("\n==== Scheduling Result ====");
        System.out.println("Execution Order (dispatch times): ");
        for (String e : result.executionOrder) {
            System.out.println("  " + e);
        }
        System.out.println("\nWaiting Times:");
        for (Map.Entry<String, Integer> e : result.waitingTimes.entrySet()) {
            System.out.println("  " + e.getKey() + ": " + e.getValue());
        }
        System.out.println("\nTurnaround Times:");
        for (Map.Entry<String, Integer> e : result.turnaroundTimes.entrySet()) {
            System.out.println("  " + e.getKey() + ": " + e.getValue());
        }
        System.out.printf("\nAverages -> Waiting: %.2f, Turnaround: %.2f\n", result.averageWaiting, result.averageTurnaround);
    }
}