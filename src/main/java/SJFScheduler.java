import java.util.*;

public class SJFScheduler implements Scheduler {

    @Override
    public ScheduleResult schedule(Process[] processes, int contextSwitch) {
        // 1. Initialize result and process objects
        ScheduleResult result = new ScheduleResult();
        List<Process> processList = new ArrayList<>();
        for (Process p : processes) {
            processList.add(new Process(p.getProcessName(), p.getArrivalTime(), p.getBurstTime(), p.getPriority(), p.getQuantum()));
        }

        // 2. Setup Queue: Shortest Remaining Time First, then Earliest Arrival
        PriorityQueue<Process> readyQueue = new PriorityQueue<>((a, b) -> {
            if (a.getRemainingTime() != b.getRemainingTime()) {
                return Integer.compare(a.getRemainingTime(), b.getRemainingTime());
            }
            return Integer.compare(a.getArrivalTime(), b.getArrivalTime());
        });

        int currentTime = 0;
        int completedCount = 0;
        int n = processList.size();
        Process currentProcess = null;
        String lastProcessName = null; 

        while (completedCount < n) {
            //Check for Arrivals 
            for (Process p : processList) {
                if (p.getArrivalTime() == currentTime && !readyQueue.contains(p) && p.getRemainingTime() > 0 && p != currentProcess) {
                    readyQueue.add(p);
                }
            }

            //Preemption Check 
            // If we have a current process, check if someone in the queue is shorter
            if (currentProcess != null && !readyQueue.isEmpty()) {
                Process bestInQueue = readyQueue.peek();
                if (bestInQueue.getRemainingTime() < currentProcess.getRemainingTime()) {
                    readyQueue.add(currentProcess);
                    currentProcess = null; 
                }
            }

            //Selection & Context Switch 
            if (currentProcess == null && !readyQueue.isEmpty()) {
                Process selected = readyQueue.poll();

                // Check if this is a context switch (different from last process, and not the first run)
                if (lastProcessName != null && !selected.getProcessName().equals(lastProcessName)) {
                    for (int i = 0; i < contextSwitch; i++) {
                        currentTime++;
                        for (Process p : processList) {
                            if (p.getArrivalTime() == currentTime && !readyQueue.contains(p) && p.getRemainingTime() > 0 && p != selected) {
                                readyQueue.add(p);
                            }
                        }
                    }
                }

                currentProcess = selected;
                
                // Record Start Time (if first time running)
                if (currentProcess.getStartTime() == -1) {
                    currentProcess.setStartTime(currentTime);
                }
                
                // Log Execution Order (only if it changes)
                if (lastProcessName == null || !currentProcess.getProcessName().equals(lastProcessName)) {
                    result.executionOrder.add(currentProcess.getProcessName());
                    lastProcessName = currentProcess.getProcessName();
                }
            }

            //Execution 
            if (currentProcess != null) {
                currentProcess.consumeCpu(1);
                currentTime++;

                if (currentProcess.isCompleted()) {
                    currentProcess.setCompletionTime(currentTime);

                    int turnaround = currentProcess.getCompletionTime() - currentProcess.getArrivalTime();
                    int waiting = turnaround - currentProcess.getBurstTime();

                    result.waitingTimes.put(currentProcess.getProcessName(), waiting);
                    result.turnaroundTimes.put(currentProcess.getProcessName(), turnaround);

                    completedCount++;
                    currentProcess = null; // Process done, CPU becomes free

                }
            } else {
                currentTime++;   // CPU is idle
            }
        }

        // 3. Calculate Averages
        double totalWait = 0, totalTurn = 0;
        for (Integer w : result.waitingTimes.values()){    
        totalWait += w;
        }
        for (Integer t : result.turnaroundTimes.values()) {
            totalTurn += t;
        }

        result.averageWaiting = n == 0 ? 0 : totalWait / n;
        result.averageTurnaround = n == 0 ? 0 : totalTurn / n;

        return result;
    }
}