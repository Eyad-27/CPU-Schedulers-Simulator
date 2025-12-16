import java.util.*;

public class RoundRobinScheduler implements Scheduler {
    private final int timeQuantum;

    public RoundRobinScheduler(int timeQuantum) {
        this.timeQuantum = timeQuantum;
    }

    @Override
    public ScheduleResult schedule(Process[] inputProcesses, int contextSwitch) {
        // Defensive copy and sort by arrival time
        List<Process> processes = new ArrayList<>();
        for (Process p : inputProcesses) {
            Process copy = new Process(p.getProcessName(), p.getArrivalTime(), p.getBurstTime(), p.getPriority(), p.getQuantum());
            processes.add(copy);
        }
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));

        Queue<Process> readyQueue = new ArrayDeque<>();
        ScheduleResult result = new ScheduleResult();

        int time = 0;
        int idx = 0; // index into sorted processes for arrivals

        // Bring in any processes that arrive at time 0
        while (idx < processes.size() && processes.get(idx).getArrivalTime() <= time) {
            readyQueue.add(processes.get(idx++));
        }

        Process current = null;
        int sliceRemaining = 0;

        while (!readyQueue.isEmpty() || idx < processes.size() || (current != null && !current.isCompleted())) {
            // If no current, fetch next from ready
            if (current == null || sliceRemaining == 0 || current.isCompleted()) {
                // If we were running a process and need to switch, apply context switch (except first dispatch)
                if (current != null && !current.isCompleted()) {
                    readyQueue.add(current); // put it back at end
                }
                // peek next process
                Process next = readyQueue.peek();
                if (next != null) {
                    // apply context switch if this isn't the first ever dispatch or if switching between processes
                    if (result.executionOrder.size() > 0) {
                        // Simulate context switch per tick:
                        // - advance time one by one
                        // - increment waiting for all ready processes (including the next to run)
                        // - bring in arrivals that occur during the switch so they can accrue waiting for remaining CS ticks
                        for (int cs = 0; cs < contextSwitch; cs++) {
                            time += 1;
                            for (Process rp : readyQueue) {
                                rp.incrementWaiting(1);
                            }
                            while (idx < processes.size() && processes.get(idx).getArrivalTime() <= time) {
                                readyQueue.add(processes.get(idx++));
                            }
                        }
                    }
                    current = readyQueue.poll();
                    sliceRemaining = timeQuantum;
                    if (current.getStartTime() == -1) current.setStartTime(time);
                    // record dispatch into execution order timeline (names only for tests)
                    result.executionOrder.add(current.getProcessName());
                } else {
                    // No ready process; jump time to next arrival to avoid idle loops
                    if (idx < processes.size()) {
                        int jump = processes.get(idx).getArrivalTime() - time;
                        time += jump;
                        // bring arrivals
                        while (idx < processes.size() && processes.get(idx).getArrivalTime() <= time) {
                            readyQueue.add(processes.get(idx++));
                        }
                        continue; // try to select again
                    } else {
                        break; // everything done
                    }
                }
            }

            // Run the current process for 1 time unit or until completion or quantum expires
            int runFor = Math.min(1, sliceRemaining);
            current.consumeCpu(runFor);
            sliceRemaining -= runFor;
            time += runFor;

            // Update waiting time for all processes in ready queue
            for (Process rp : readyQueue) {
                rp.incrementWaiting(runFor);
            }

            // Bring in any processes that arrive at this time
            while (idx < processes.size() && processes.get(idx).getArrivalTime() <= time) {
                readyQueue.add(processes.get(idx++));
            }

            // If completed, set completion and reset current to force picking next
            if (current.isCompleted()) {
                current.setCompletionTime(time);
                // push metrics
                result.waitingTimes.put(current.getProcessName(), current.getWaitingTime());
                result.turnaroundTimes.put(current.getProcessName(), current.getTurnaroundTime());
                current = null;
                sliceRemaining = 0;
            } else if (sliceRemaining == 0) {
                // time slice expired; will cause context switch next iteration
            }
        }

        // compute averages
        double totalWait = 0, totalTurn = 0;
        for (Process p : processes) {
            Integer w = result.waitingTimes.get(p.getProcessName());
            Integer t = result.turnaroundTimes.get(p.getProcessName());
            if (w == null) {
                // if process never completed (shouldn't happen), estimate
                w = p.getWaitingTime();
            }
            if (t == null) {
                t = p.getTurnaroundTime();
            }
            totalWait += w;
            totalTurn += t;
        }
        result.averageWaiting = processes.isEmpty() ? 0 : totalWait / processes.size();
        result.averageTurnaround = processes.isEmpty() ? 0 : totalTurn / processes.size();

        return result;
    }
}
