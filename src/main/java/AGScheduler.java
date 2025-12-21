//// TODO by Yassin: Implement Part A - The flow (FCFS -> Priority -> SJF).
//// TODO by Pedro: Implement Part B - The Quantum updates and History printing.
//// Hints for scenarios:
//// 1) Used all quantum: move to end of queue; recompute quantum based on history.
//// 2) Unused quantum: transfer remaining quantum to next algorithm phase; may get priority boost.
//// 3) Completed: record completion and finalize metrics; history should log transitions.

import java.util.*;

public class AGScheduler implements Scheduler {

    public AGScheduler() {
    }

    @Override
    public ScheduleResult schedule(Process[] processes, int contextSwitch) {
        ScheduleResult result = new ScheduleResult();
        List<Process> processList = new ArrayList<>();
        LinkedList<Process> readyQueue = new LinkedList<>();

        // Create process copies
        for(Process p : processes) {
            processList.add(new Process(p.getProcessName(), p.getArrivalTime(),
                    p.getBurstTime(), p.getPriority(), p.getQuantum()));
        }

        int currTime = 0;
        Process currProcess = null;

        while(!allCompleted(processList)) {
            // Add arriving processes to ready queue
            addArrivals(processList, readyQueue, currTime);

            // If no ready process and no current process, advance time
            if(readyQueue.isEmpty() && currProcess == null) {
                currTime++;
                continue;
            }

            // Select new process if none is currently running
            if (currProcess == null) {
                if(readyQueue.isEmpty()) {
                    currTime++;
                    continue;
                }
                currProcess = readyQueue.removeFirst(); // FCFS - take from front
                result.executionOrder.add(currProcess.getProcessName());
            }

            int Q = currProcess.getQuantum();
            int fcfsTime = (int) Math.ceil(Q * 0.25);
            int priorityTime = (int) Math.ceil(Q * 0.25);
            int sjfTime = Q - fcfsTime - priorityTime;

            // =================== FCFS Phase (25%) ===================
            for (int i = 0; i < fcfsTime && currProcess.getRemainingTime() > 0; i++) {
                currProcess.consumeCpu(1);
                currTime++;
                addArrivals(processList, readyQueue, currTime);
            }

            if (currProcess.getRemainingTime() == 0) {
                finishProcess(currProcess, currTime);
                currProcess = null;
                continue;
            }

            // =================== Priority Phase (25%) ===================
            // Check for higher priority preemption ONLY at the start of priority phase
            int higherPriorityIdx = getHighestPriority(currProcess, readyQueue);

            if (higherPriorityIdx != -1) {
                // Preempted by higher priority process before priority phase starts
                int usedTime = fcfsTime;
                int remainingQuantum = Q - usedTime;
                int quantumIncrease = (int) Math.ceil(remainingQuantum / 2.0);
                currProcess.setQuantum(Q + quantumIncrease);
                readyQueue.addLast(currProcess);

                currProcess = readyQueue.remove(higherPriorityIdx);
                result.executionOrder.add(currProcess.getProcessName());
                continue;
            }

            // Execute priority phase fully
            for (int i = 0; i < priorityTime && currProcess.getRemainingTime() > 0; i++) {
                currProcess.consumeCpu(1);
                currTime++;
                addArrivals(processList, readyQueue, currTime);
            }

            if (currProcess.getRemainingTime() == 0) {
                finishProcess(currProcess, currTime);
                currProcess = null;
                continue;
            }

            // =================== SJF Phase (50%) ===================
            // Check for shorter job preemption ONLY at the start of SJF phase
            int shorterJobIdx = getShortestJob(currProcess, readyQueue);

            if (shorterJobIdx != -1) {
                // Preempted by shorter job before SJF phase starts
                int remainingSJF = sjfTime;
                currProcess.setQuantum(Q + remainingSJF);
                readyQueue.addLast(currProcess);

                currProcess = readyQueue.remove(shorterJobIdx);
                result.executionOrder.add(currProcess.getProcessName());
                continue;
            }

            // Execute SJF phase fully
            for (int i = 0; i < sjfTime && currProcess.getRemainingTime() > 0; i++) {
                currProcess.consumeCpu(1);
                currTime++;
                addArrivals(processList, readyQueue, currTime);
            }

            if (currProcess.getRemainingTime() == 0) {
                finishProcess(currProcess, currTime);
                currProcess = null;
                continue;
            }

            // =================== Quantum Exhausted ===================
            // Process used all quantum without completing
            currProcess.setQuantum(Q + 2);
            readyQueue.addLast(currProcess);
            currProcess = null;
        }

        // Calculate final statistics
        double totalWait = 0, totalTurnaround = 0;
        for (Process p : processList) {
            int turnaround = p.getCompletionTime() - p.getArrivalTime();
            int waiting = turnaround - p.getBurstTime();

            result.waitingTimes.put(p.getProcessName(), waiting);
            result.turnaroundTimes.put(p.getProcessName(), turnaround);
            result.quantumHistory.put(p.getProcessName(), p.getQuantumHistory());

            totalWait += waiting;
            totalTurnaround += turnaround;
        }

        result.averageWaiting = totalWait / processList.size();
        result.averageTurnaround = totalTurnaround / processList.size();

        return result;
    }

    private boolean allCompleted(List<Process> processes) {
        for (Process p : processes) {
            if (!p.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    private void addArrivals(List<Process> all, LinkedList<Process> ready, int time) {
        for (Process p : all) {
            if (p.getArrivalTime() == time && !p.isCompleted()) {
                // Only add if not already in ready queue and not currently running
                if (!ready.contains(p)) {
                    ready.addLast(p);
                }
            }
        }
    }

    private int getHighestPriority(Process current, LinkedList<Process> ready) {
        int minPriority = current.getPriority();
        int minIdx = -1;

        // Find process with highest priority (lowest number)
        // If multiple have same priority, take the first (FCFS)
        for (int i = 0; i < ready.size(); i++) {
            if (ready.get(i).getPriority() < minPriority) {
                minPriority = ready.get(i).getPriority();
                minIdx = i;
            }
        }
        return minIdx;
    }

    private int getShortestJob(Process current, LinkedList<Process> ready) {
        int minRemaining = current.getRemainingTime();
        int minIdx = -1;

        // Find process with shortest remaining time
        // If multiple have same time, take the first (FCFS)
        for (int i = 0; i < ready.size(); i++) {
            if (ready.get(i).getRemainingTime() < minRemaining) {
                minRemaining = ready.get(i).getRemainingTime();
                minIdx = i;
            }
        }
        return minIdx;
    }

    private void finishProcess(Process p, int time) {
        p.setCompletionTime(time);
        p.setQuantum(0);
    }
}