import java.util.*;

public class PriorityScheduler implements Scheduler {
    private final int agingInterval;

    public PriorityScheduler(int agingInterval) {
        this.agingInterval = agingInterval;
    }

    @Override
    public ScheduleResult schedule(Process[] inputProcesses, int contextSwitch) {
        // Create defensive copies of processes
        List<Process> processes = new ArrayList<>();
        Map<String, Integer> lastAgingTime = new HashMap<>();

        for (Process p : inputProcesses) {
            Process copy = new Process(p.getProcessName(), p.getArrivalTime(),
                    p.getBurstTime(), p.getPriority(), p.getQuantum());
            processes.add(copy);
            lastAgingTime.put(copy.getProcessName(), copy.getArrivalTime());
        }

        ScheduleResult result = new ScheduleResult();

        int time = 0;
        int finished = 0;
        Process current = null;

        // sort by arrival to make arrivals predictable
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));

        // helper comparator: lower numeric priority means higher priority
        Comparator<Process> prioCmp = Comparator
                .comparingInt((Process p) -> p.getPriority())
                .thenComparingInt(Process::getArrivalTime)
                .thenComparing(Process::getProcessName);

        // previous running process name to record executionOrder only on switches
        String prevRunning = null;

        while (finished < processes.size()) {

            // apply aging for waiting processes
            for (Process p : processes) {
                if (p.getRemainingTime() > 0 && p.getArrivalTime() <= time) {
                    int lastAge = lastAgingTime.get(p.getProcessName());
                    while (agingInterval > 0 && (time - lastAge) >= agingInterval) {
                        if (p.getPriority() > 1) {
                            p.setPriority(p.getPriority() - 1);
                        }
                        lastAge += agingInterval;
                        lastAgingTime.put(p.getProcessName(), lastAge);
                    }
                }
            }

            // build ready list
            List<Process> ready = new ArrayList<>();
            for (Process p : processes) {
                if (p.getArrivalTime() <= time && p.getRemainingTime() > 0) {
                    ready.add(p);
                }
            }

            if (ready.isEmpty()) {
                time++;
                continue;
            }

            // pick highest priority (lowest numeric)
            ready.sort(prioCmp);
            Process next = ready.get(0);

            // if switching, account for context switch time (simulate aging during it)
            if (current != null && !current.getProcessName().equals(next.getProcessName())) {

                // Record the process we INTEND to switch to
                if (prevRunning == null || !prevRunning.equals(next.getProcessName())) {
                    result.executionOrder.add(next.getProcessName());
                    prevRunning = next.getProcessName();
                }

                for (int i = 0; i < contextSwitch; i++) {
                    time++;
                    // aging during context switch - all waiting processes can age
                    for (Process p : processes) {
                        if (p.getRemainingTime() > 0 && p.getArrivalTime() <= time
                                && !p.getProcessName().equals(next.getProcessName())) {
                            int lastAge = lastAgingTime.get(p.getProcessName());
                            while (agingInterval > 0 && (time - lastAge) >= agingInterval) {
                                if (p.getPriority() > 1) {
                                    p.setPriority(p.getPriority() - 1);
                                }
                                lastAge += agingInterval;
                                lastAgingTime.put(p.getProcessName(), lastAge);
                            }
                        }
                    }
                }

                // re-evaluate after context switch - if priority changed, do another CS
                ready.clear();
                for (Process p : processes) {
                    if (p.getArrivalTime() <= time && p.getRemainingTime() > 0) {
                        ready.add(p);
                    }
                }
                if (!ready.isEmpty()) {
                    ready.sort(prioCmp);
                    Process reevaluated = ready.get(0);

                    // If the highest priority changed during CS, need ANOTHER context switch
                    if (!reevaluated.getProcessName().equals(next.getProcessName())) {

                        // Record the NEW process we're switching to
                        if (!prevRunning.equals(reevaluated.getProcessName())) {
                            result.executionOrder.add(reevaluated.getProcessName());
                            prevRunning = reevaluated.getProcessName();
                        }

                        for (int i = 0; i < contextSwitch; i++) {
                            time++;
                            // aging during the second context switch
                            for (Process p : processes) {
                                if (p.getRemainingTime() > 0 && p.getArrivalTime() <= time
                                        && !p.getProcessName().equals(reevaluated.getProcessName())) {
                                    int lastAge = lastAgingTime.get(p.getProcessName());
                                    while (agingInterval > 0 && (time - lastAge) >= agingInterval) {
                                        if (p.getPriority() > 1) {
                                            p.setPriority(p.getPriority() - 1);
                                        }
                                        lastAge += agingInterval;
                                        lastAgingTime.put(p.getProcessName(), lastAge);
                                    }
                                }
                            }
                        }
                    }
                    next = reevaluated;
                }
            } else {
                // Not switching - record if needed
                if (prevRunning == null || !prevRunning.equals(next.getProcessName())) {
                    result.executionOrder.add(next.getProcessName());
                    prevRunning = next.getProcessName();
                }
            }

            // run one time unit (preemptive, so check each tick)
            current = next;
            if (current.getStartTime() == -1) {
                current.setStartTime(time);
            }
            current.setRemainingTime(current.getRemainingTime() - 1);
            // mark that it was just run (reset its last aging time)
            lastAgingTime.put(current.getProcessName(), time + 1);
            time++;

            if (current.getRemainingTime() == 0) {
                current.setCompletionTime(time);
                finished++;
                // reset prevRunning so next scheduling records new start
                prevRunning = null;
            }
        }

        // calculate times
        double totalWT = 0;
        double totalTAT = 0;

        for (Process p : processes) {
            int tat = p.getCompletionTime() - p.getArrivalTime();
            int wt = tat - p.getBurstTime();

            result.turnaroundTimes.put(p.getProcessName(), tat);
            result.waitingTimes.put(p.getProcessName(), wt);

            totalWT += wt;
            totalTAT += tat;
        }

        result.averageWaiting = totalWT / processes.size();
        result.averageTurnaround = totalTAT / processes.size();

        return result;
    }
}