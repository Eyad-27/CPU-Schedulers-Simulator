import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("CPU Schedulers Simulator");

        boolean running = true;
        while (running) {
            System.out.println("\nSelect Mode:");
            System.out.println("  1) Automated Test Cases");
            System.out.println("  2) Manual Input");
            System.out.println("  3) Exit");
            int modeChoice = readInt(scanner, "Your choice: ");

            switch (modeChoice) {
                case 1:
                    TestExecutor.displayTestMenu(scanner);
                    break;
                case 2:
                    runManualFlow(scanner);
                    break;
                case 3:
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }

        scanner.close();
        System.out.println("Goodbye!");
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

    private static void runManualFlow(Scanner scanner) {
        int n = readInt(scanner, "Enter number of processes: ");
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

        System.out.println("\nSelect Scheduler:");
        System.out.println("  1) Round Robin");
        System.out.println("  2) SJF (Preemptive) - stub");
        System.out.println("  3) Priority (Preemptive) - stub");
        System.out.println("  4) AG Scheduler - stub");
        int choice = readInt(scanner, "Your choice: ");

        ScheduleResult result = null;
        switch (choice) {
            case 1: {
                int rrQuantum = readInt(scanner, "Enter Round Robin Time Quantum: ");
                Scheduler rr = new RoundRobinScheduler(rrQuantum);
                result = rr.schedule(processes, contextSwitch);
                break;
            }
            case 2:
                System.out.println("SJF Scheduler is not implemented yet (assigned to Mohamed Tarek).");
                break;
            case 3:
                Scheduler ps = new Main().new PriorityScheduler();
                result = ps.schedule(processes, contextSwitch);
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

    public class PriorityScheduler implements Scheduler {

        private static final int AGING_THRESHOLD = 5; // fixes starvation

        public PriorityScheduler() {}

        @Override
        public ScheduleResult schedule(Process[] processes, int contextSwitch) {

            ScheduleResult result = new ScheduleResult();

            int n = processes.length;
            int completed = 0;
            int currentTime = 0;
            String lastProcess = null;

            // Runtime helpers
            Map<Process, Integer> effectivePriority = new HashMap<>();
            Map<Process, Integer> waitingStart = new HashMap<>();

            for (Process p : processes) {
                effectivePriority.put(p, p.getPriority());
                waitingStart.put(p, -1);
            }

            while (completed < n) {

                // ===== Aging calculation =====
                for (Process p : processes) {
                    if (!p.isCompleted() && p.getArrivalTime() <= currentTime) {
                        if (waitingStart.get(p) != -1) {
                            int waited = currentTime - waitingStart.get(p);
                            effectivePriority.put(
                                    p,
                                    p.getPriority() - (waited / AGING_THRESHOLD)
                            );
                        } else {
                            effectivePriority.put(p, p.getPriority());
                        }
                    }
                }

                // ===== Select highest priority process (lowest value) =====
                Process current = null;
                for (Process p : processes) {
                    if (p.getArrivalTime() <= currentTime && !p.isCompleted()) {
                        if (current == null ||
                                effectivePriority.get(p) < effectivePriority.get(current)) {
                            current = p;
                        }
                    }
                }

                // ===== CPU idle =====
                if (current == null) {
                    currentTime++;
                    continue;
                }

                // ===== Context switching =====
                if (lastProcess != null &&
                        !lastProcess.equals(current.getProcessName())) {
                    currentTime += contextSwitch;
                }

                // ===== Execution order =====
                if (lastProcess == null ||
                        !lastProcess.equals(current.getProcessName())) {
                    result.executionOrder.add(current.getProcessName());
                }

                // ===== First CPU access =====
                if (current.getStartTime() == -1) {
                    current.setStartTime(currentTime);
                }

                // ===== Run for 1 time unit =====
                waitingStart.put(current, -1);
                current.consumeCpu(1);
                currentTime++;

                // ===== Update waiting time for others =====
                for (Process p : processes) {
                    if (p != current &&
                            !p.isCompleted() &&
                            p.getArrivalTime() <= currentTime) {
                        if (waitingStart.get(p) == -1) {
                            waitingStart.put(p, currentTime);
                        }
                    }
                }

                // ===== Completion =====
                if (current.isCompleted()) {
                    current.setCompletionTime(currentTime);
                    completed++;
                }

                lastProcess = current.getProcessName();
            }

            // ===== Final metrics =====
            double totalWaiting = 0;
            double totalTurnaround = 0;

            for (Process p : processes) {
                int turnaround = p.getCompletionTime() - p.getArrivalTime();
                int waiting = turnaround - p.getBurstTime();

                p.setWaitingTime(waiting);

                result.waitingTimes.put(p.getProcessName(), waiting);
                result.turnaroundTimes.put(p.getProcessName(), turnaround);

                totalWaiting += waiting;
                totalTurnaround += turnaround;
            }

            result.averageWaiting = totalWaiting / n;
            result.averageTurnaround = totalTurnaround / n;

            return result;
        }
    }


}