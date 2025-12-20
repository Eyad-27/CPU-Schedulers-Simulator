import java.util.*;

public class PriorityScheduler implements Scheduler {

    private int agingInterval = 5; 

    public void setAgingInterval(int interval) {
        this.agingInterval = interval;
    }

    private static class ProcessState {
        Process p;
        int currentPriority;
        int ageTimer = 0;
        int remainingBurst;

        ProcessState(Process p) {
            this.p = p;
            this.currentPriority = p.getPriority();
            this.remainingBurst = p.getBurstTime();
        }
    }

    @Override
    public ScheduleResult schedule(Process[] processes, int contextSwitch) {
        ScheduleResult result = new ScheduleResult();
        int n = processes.length;
        if (n == 0) return result;

        int completed = 0;
        int currentTime = 0;
        String lastProcessName = null;

        List<ProcessState> allStates = new ArrayList<>();
        for (Process p : processes) {
            allStates.add(new ProcessState(p));
        }

        ProcessState current = null;

        while (completed < n) {
            // 1. Find the best process currently available in the ready pool
            ProcessState best = findBestProcess(allStates, currentTime);

            if (best != null) {
                // Preemption/Selection Logic:
                // Switch if: 1. CPU is idle, OR 2. Best is different from current AND strictly better
                if (current == null || (best != current && isBetter(best, current))) {
                    
                    // Trigger Context Switch if switching to a DIFFERENT process
                    if (lastProcessName != null && !best.p.getProcessName().equals(lastProcessName)) {
                        for (int i = 0; i < contextSwitch; i++) {
                            currentTime++;
                            // Everyone waiting ages during context switch
                            applyAging(allStates, null, currentTime);
                            // Important: Re-check best after aging tick
                            best = findBestProcess(allStates, currentTime);
                        }
                    }

                    current = best;
                    // Reset age timer only when it actually starts/resumes on CPU
                    current.ageTimer = 0; 

                    if (result.executionOrder.isEmpty() || 
                        !result.executionOrder.get(result.executionOrder.size() - 1).equals(current.p.getProcessName())) {
                        result.executionOrder.add(current.p.getProcessName());
                    }
                }
            }

            // 2. Execution Tick
            if (current != null) {
                if (current.p.getStartTime() == -1) {
                    current.p.setStartTime(currentTime);
                }

                current.remainingBurst--;
                currentTime++;

                // Age everyone except the one currently running
                applyAging(allStates, current, currentTime);

                if (current.remainingBurst <= 0) {
                    current.p.setCompletionTime(currentTime);
                    completed++;
                    lastProcessName = current.p.getProcessName();
                    // Mark as completed in the Process object logic if it has such a method
                    // In this context, we'll rely on remainingBurst for findBestProcess
                    current = null; 
                } else {
                    lastProcessName = current.p.getProcessName();
                }
            } else {
                // CPU Idle
                currentTime++;
                applyAging(allStates, null, currentTime);
            }
        }

        return finalizeMetrics(processes, result);
    }

    private ProcessState findBestProcess(List<ProcessState> states, int time) {
        ProcessState best = null;
        for (ProcessState ps : states) {
            if (ps.p.getArrivalTime() <= time && ps.remainingBurst > 0) {
                if (best == null || isBetter(ps, best)) {
                    best = ps;
                }
            }
        }
        return best;
    }

    /**
     * Comparison logic to match Priority test cases:
     * 1. Lower Priority Value is better.
     * 2. Tie-breaker: Earlier Arrival Time is better.
     * 3. Second Tie-breaker: Original Priority (if needed).
     */
    private boolean isBetter(ProcessState a, ProcessState b) {
        if (a.currentPriority != b.currentPriority) {
            return a.currentPriority < b.currentPriority;
        }
        if (a.p.getArrivalTime() != b.p.getArrivalTime()) {
            return a.p.getArrivalTime() < b.p.getArrivalTime();
        }
        return a.p.getPriority() < b.p.getPriority();
    }

    private void applyAging(List<ProcessState> states, ProcessState runningNow, int time) {
        for (ProcessState ps : states) {
            // A process ages if it has arrived, is not finished, and is NOT on the CPU
            if (ps.p.getArrivalTime() < time && ps.remainingBurst > 0 && ps != runningNow) {
                ps.ageTimer++;
                if (ps.ageTimer >= agingInterval) {
                    // Priority improves (decreases) by 1, floor at 1
                    ps.currentPriority = Math.max(1, ps.currentPriority - 1);
                    ps.ageTimer = 0;
                }
            }
        }
    }

    private ScheduleResult finalizeMetrics(Process[] processes, ScheduleResult result) {
        double totalWT = 0, totalTAT = 0;
        int n = processes.length;

        for (Process p : processes) {
            int tat = p.getCompletionTime() - p.getArrivalTime();
            int wt = tat - p.getBurstTime();

            // Store results in maps (adjusting for ScheduleResult structure)
            result.waitingTimes.put(p.getProcessName(), wt);
            result.turnaroundTimes.put(p.getProcessName(), tat);

            totalWT += wt;
            totalTAT += tat;
        }

        // Rounded to match JSON expected outputs if necessary
        result.averageWaiting = n == 0 ? 0 : Math.round((totalWT / n) * 100.0) / 100.0;
        result.averageTurnaround = n == 0 ? 0 : Math.round((totalTAT / n) * 100.0) / 100.0;
        return result;
    }
}
