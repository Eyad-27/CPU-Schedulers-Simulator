import java.util.*;

public class Process {
    private String processName;
    private int arrivalTime;
    private int burstTime;
    private int priority;
    // Quantum field used for AG scheduler variants
    private int quantum;
    private List<Integer> quantumHistory;

    // Runtime state
    private int remainingTime;
    private int waitingTime;
    private int startTime = -1; // first time the process gets CPU
    private int completionTime = -1; // when the process finishes

    public Process(String processName, int arrivalTime, int burstTime, int priority) {
        this(processName, arrivalTime, burstTime, priority, 0);
    }

    public Process(String processName, int arrivalTime, int burstTime, int priority, int quantum) {
        this.processName = processName;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.quantum = quantum;
        this.remainingTime = burstTime;
        this.waitingTime = 0;
        this.quantumHistory = new ArrayList<>();
        this.quantumHistory.add(this.quantum);
    }

    // Getters and setters
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    public int getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(int arrivalTime) { this.arrivalTime = arrivalTime; }

    public int getBurstTime() { return burstTime; }
    public void setBurstTime(int burstTime) {
        this.burstTime = burstTime;
        // keep remaining in sync if needed
        if (this.remainingTime > burstTime) {
            this.remainingTime = burstTime;
        }
    }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public List<Integer> getQuantumHistory() {
        return quantumHistory;
    }
    public void setQuantumHistory(List<Integer> quantumHistory) {
        this.quantumHistory = quantumHistory;
    }

    public int getQuantum() { return quantum; }
    public void setQuantum(int quantum) {
        this.quantum = quantum;
        this.quantumHistory.add(quantum);
    }

    public int getRemainingTime() { return remainingTime; }
    public void setRemainingTime(int remainingTime) { this.remainingTime = remainingTime; }

    public int getWaitingTime() { return waitingTime; }
    public void setWaitingTime(int waitingTime) { this.waitingTime = waitingTime; }

    public int getStartTime() { return startTime; }
    public void setStartTime(int startTime) { this.startTime = startTime; }

    public int getCompletionTime() { return completionTime; }
    public void setCompletionTime(int completionTime) { this.completionTime = completionTime; }

    // Utility methods
    public boolean isCompleted() {
        return remainingTime <= 0;
    }

    public void consumeCpu(int amount) {
        if (amount < 0) return;
        if (startTime == -1) {
            // start time will be set by scheduler when scheduled
        }
        remainingTime = Math.max(0, remainingTime - amount);
    }

    public void incrementWaiting(int amount) {
        if (amount > 0) waitingTime += amount;
    }

    public int getTurnaroundTime() {
        if (completionTime < 0) return -1;
        return completionTime - arrivalTime;
    }

    @Override
    public String toString() {
        return String.format("%s(arr=%d, burst=%d, prio=%d)", processName, arrivalTime, burstTime, priority);
    }
}

