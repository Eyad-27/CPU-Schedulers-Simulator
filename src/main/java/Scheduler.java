public interface Scheduler {
    /**
     * Execute scheduling on the provided processes.
     *
     * @param processes array of processes to schedule
     * @param contextSwitch the fixed context switch delay applied when switching between processes
     * @return a ScheduleResult containing per-process metrics and execution order
     */
    ScheduleResult schedule(Process[] processes, int contextSwitch);
}

/**
 * Simple DTO for returning scheduling results.
 */
class ScheduleResult {
    public java.util.List<String> executionOrder = new java.util.ArrayList<>();
    public java.util.Map<String, Integer> waitingTimes = new java.util.HashMap<>();
    public java.util.Map<String, Integer> turnaroundTimes = new java.util.HashMap<>();
    public double averageWaiting;
    public double averageTurnaround;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Execution Order: ").append(executionOrder).append('\n');
        sb.append("Waiting Times: ").append(waitingTimes).append('\n');
        sb.append("Turnaround Times: ").append(turnaroundTimes).append('\n');
        sb.append(String.format("Averages -> Waiting: %.2f, Turnaround: %.2f", averageWaiting, averageTurnaround));
        return sb.toString();
    }
}

