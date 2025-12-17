import java.util.*;

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
