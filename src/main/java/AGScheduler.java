// TODO by Yassin: Implement Part A - The flow (FCFS -> Priority -> SJF).
// TODO by Pedro: Implement Part B - The Quantum updates and History printing.
// Hints for scenarios:
// 1) Used all quantum: move to end of queue; recompute quantum based on history.
// 2) Unused quantum: transfer remaining quantum to next algorithm phase; may get priority boost.
// 3) Completed: record completion and finalize metrics; history should log transitions.

import java.util.*;

public class AGScheduler implements Scheduler {

    public AGScheduler() {

    }

    @Override
    public ScheduleResult schedule(Process[] processes, int contextSwitch) {
        // TODO: Implement AG scheduling logic building on FCFS -> Priority -> SJF phases
        // TODO: Keep per-process quantum updated and maintain an execution history output

        ScheduleResult result = new ScheduleResult();
        List<Process> processList = new ArrayList<>();
        List<Process> readyQueue = new ArrayList<>();
        int left = processes.length;

        for(Process p : processes) {
            processList.add(new Process(p.getProcessName(), p.getArrivalTime(), p.getBurstTime(), p.getPriority(), p.getQuantum()));
        }

        int currTime = 0;
        Process currProcess = null;

        while(!finished(processList)) {

            left -= addArrivals(processList, readyQueue, currTime);
            if(readyQueue.isEmpty() && left != 0) {
                currTime++;
                continue;
            }


            // =================== FCFS ===================
            if (currProcess == null) {
                int firstArrived = getFirstArrived(currProcess, readyQueue);
                currProcess = readyQueue.remove(firstArrived);
                result.executionOrder.add(currProcess.getProcessName());
            }

            int fcfs = (int) Math.ceil(currProcess.getQuantum() * 0.25);
            for (int i = 0; i < fcfs; i++) {
                if (currProcess.getRemainingTime() != 0) currTime++;
                currProcess.consumeCpu(1);
                left -= addArrivals(processList, readyQueue, currTime);
            }

            if (currProcess.getRemainingTime() == 0) {
                completeProcess(currProcess, readyQueue, currTime);
                currProcess = null;
                continue;
            }


            // =================== Priority ===================
            // ensure readyQueue has candidates before calling helper
            if (readyQueue.isEmpty()) {
                // nothing available for priority phase; continue main loop
                continue;
            }
            int highPriority = getHighestPriority(currProcess, readyQueue);

            int Q = currProcess.getQuantum();
            // Scenario 1
            Process next;
            if (highPriority > -1)
                next = readyQueue.get(highPriority);
            else {
                next = currProcess;
            }
            if (currProcess.getRemainingTime() != 0 &&  next != currProcess) {
                int consumedQuantum = (int) Math.ceil((double) (currProcess.getQuantum() - fcfs) * 0.5);
                int newQuantum = currProcess.getQuantum() + consumedQuantum;
                currProcess.setQuantum(newQuantum);
                readyQueue.add(currProcess);
                currProcess = readyQueue.remove(highPriority);
                result.executionOrder.add(currProcess.getProcessName());
                continue;
            }

            int priorityQuantum = (int) Math.ceil(currProcess.getQuantum() * 0.25);
            for (int i = 0; i < priorityQuantum; i++) {
                if (currProcess.getRemainingTime() != 0) currTime++;
                currProcess.consumeCpu(1);
                left -= addArrivals(processList, readyQueue, currTime);
            }
            if (currProcess.getRemainingTime() == 0) {
                completeProcess(currProcess, readyQueue, currTime);
                currProcess = null;
                continue;
            }


            // =================== SJF ===================
            // ensure readyQueue has candidates before calling helper
            if (readyQueue.isEmpty()) {
                // nothing available for priority phase; continue main loop
                continue;
            }
            int shortest = getShortestJob(currProcess, readyQueue);

            if (shortest > -1) {
                next = readyQueue.get(shortest);
            }
            else {
                next = currProcess;
            }

            // Scenario 2
            if (currProcess.getRemainingTime() != 0 && next != currProcess) {
                int consumedQuantum = (int) Math.ceil((double) (currProcess.getQuantum() - fcfs - priorityQuantum));
                int newQuantum = currProcess.getQuantum() + consumedQuantum;
                currProcess.setQuantum(newQuantum);
                readyQueue.add(currProcess);
                currProcess = readyQueue.remove(shortest);
                result.executionOrder.add(currProcess.getProcessName());
                continue;
            }



            int sjf = Q - fcfs - priorityQuantum; // 50%
            for (int i = 0; i < sjf; i++) {
                if (currProcess.getRemainingTime() != 0) currTime++;
                currProcess.consumeCpu(1);
                left -= addArrivals(processList, readyQueue, currTime);
            }
            if (currProcess.getRemainingTime() == 0) {
                completeProcess(currProcess, readyQueue, currTime);
                currProcess = null;
                continue;
            }


            if (readyQueue.isEmpty()) {
                // nothing available for priority phase; continue main loop
                continue;
            }

            next = readyQueue.get(0);
            if (currProcess.getRemainingTime() != 0 && next != currProcess) {
//                int consumedQuantum = currProcess.getQuantum() - priorityQuantum - sjf;
                int newQuantum = currProcess.getQuantum() + 2;
                currProcess.setQuantum(newQuantum);
                readyQueue.add(currProcess);
                int firstArrived = getFirstArrived(currProcess, readyQueue);
                currProcess = readyQueue.remove(firstArrived);
                result.executionOrder.add(currProcess.getProcessName());
            }

        }

        /* ===== FINAL STATS ===== */
//        double tw = 0, tt = 0;
//        for (Process p : processList) {
//            int tat = p.getCompletionTime() - p.getArrivalTime();
//            int wt = tat - p.getBurstTime();
//            result.waitingTimes.put(p.getProcessName(), wt);
//            result.turnaroundTimes.put(p.getProcessName(), tat);
//            result.quantumHistory.put(p.getProcessName(), p.getQuantumHistory());
//            tw += wt;
//            tt += tat;
//        }
//
//        result.averageWaiting = tw / processList.size();
//        result.averageTurnaround = tt / processList.size();

        double tw = 0, tt = 0;
        for (Process p : processList) {
            int finish = p.getCompletionTime();
            int arrival = p.getArrivalTime();
            int burst = p.getBurstTime();

            int tat = finish - arrival;
            int wt = tat - burst;

            result.waitingTimes.put(p.getProcessName(), wt);
            result.turnaroundTimes.put(p.getProcessName(), tat);
            result.quantumHistory.put(p.getProcessName(), p.getQuantumHistory());

            tw += wt;
            tt += tat;
        }

        result.averageWaiting = tw / processList.size();
        result.averageTurnaround = tt / processList.size();


        return result;
    }



    private boolean finished(List<Process> all) {
        for (Process p : all)
            if (!p.isCompleted())
                return false;
        return true;
    }
    private int addArrivals(List<Process> all, List<Process> readyQueue, int time) {
        int count = 0;
        for (Process p : all)
            if (p.getArrivalTime() == time && !readyQueue.contains(p) && !p.isCompleted()) {
                readyQueue.add(p);
                count++;
            }
        return count;
    }

    private int getFirstArrived(Process cur, List <Process> q) {
        int min = Integer.MAX_VALUE;
        int minIdx = -1;
        for (int i = 0; i < q.size(); i++) {
            if(q.get(i).getArrivalTime() < min) {
                min = q.get(i).getArrivalTime();
                minIdx = i;
            }
        }
        return minIdx;
    }

    private int getHighestPriority(Process cur, List<Process> q) {
        int min = cur.getPriority();
//        if (cur.getRemainingTime() == 0) {
//            min = Integer.MAX_VALUE;
//        }
        int minIdx = -1;
        for (int i = 0; i < q.size(); i++) {
            if(q.get(i).getPriority() < min) {
                min = q.get(i).getPriority();
                minIdx = i;
            }
        }
        return minIdx;
    }

    private int getShortestJob(Process cur, List<Process> q) {
        int min = cur.getRemainingTime();
        int minIdx = -1;
//        if (min == 0) {
//            min = Integer.MAX_VALUE;
//        }
        for (int i = 0; i < q.size(); i++) {
            if(q.get(i).getRemainingTime() < min) {
                min = q.get(i).getRemainingTime();
                minIdx = i;
            }
        }
        return minIdx;
    }
    //
    private void completeProcess(Process currProcess, List<Process> q, int time) {
        currProcess.setRemainingTime(0);
        currProcess.setQuantum(0);
        currProcess.setCompletionTime(time);
        // remove from ready queue if present to avoid future accesses
        if (q != null) q.remove(currProcess);
    }
}


//                List<Process> all = new ArrayList<>();
//                Queue<Process> ready = new LinkedList<>();
//
//                for (Process p : processes) {
//                    p.setRemainingTime(p.getBurstTime());
//                    all.add(p);
//                }
//
//                int time = 0;
//                Process last = null;
//
//                while (!finished(all)) {
//
//                    addArrivals(all, ready, time);
//
//                    if (ready.isEmpty()) {
//                        time++;
//                        continue;
//                    }
//
//                    Process cur = ready.poll();
//
//                    /* ===== CONTEXT SWITCH ===== */
//                    if (last != null && last != cur) {
//                        time += contextSwitch;
//                        addArrivals(all, ready, time);
//                    }
//
//                    /* ===== DISPATCH ===== */
//                    result.executionOrder.add(cur.getProcessName());
//                    if (cur.getStartTime() == -1)
//                        cur.setStartTime(time);
//
//                    int Q = cur.getQuantum();
//                    cur.addToQuantumHistory(Q);
//
//                    int fcfs = (int) Math.ceil(Q * 0.25);
//                    int priority = (int) Math.ceil(Q * 0.25);
//                    int sjf = Q - fcfs - priority;
//
//                    /* ================= FCFS ================= */
//                    for (int i = 0; i < fcfs; i++) {
//                        cur.consumeCpu(1);
//                        time++;
//                        addArrivals(all, ready, time);
//
//                        if (cur.isCompleted()) {
//                            finish(cur, time);
//                            last = cur;
//                            break;
//                        }
//                    }
//                    if (cur.isCompleted()) continue;
//
//                    /* ================= PRIORITY ================= */
//                    boolean preempted = false;
//                    for (int i = 0; i < priority; i++) {
//
//                        cur.consumeCpu(1);
//                        time++;
//                        addArrivals(all, ready, time);
//
//                        if (cur.isCompleted()) {
//                            finish(cur, time);
//                            preempted = true;
//                            break;
//                        }
//
//                        if (higherPriorityExists(cur, ready)) {
//                            int remaining = Q - (fcfs + i + 1);
//                            int inc = (int) Math.ceil(remaining / 2.0);
//                            cur.setQuantum(Q + inc);
//                            ready.add(cur);
//                            preempted = true;
//                            break;
//                        }
//                    }
//                    if (preempted) {
//                        last = cur;
//                        continue;
//                    }
//
//                    /* ================= SJF ================= */
//                    // Check for preemption at start of SJF phase
//                    if (shorterJobExists(cur, ready)) {
//                        cur.setQuantum(Q + sjf);
//                        ready.add(cur);
//                        last = cur;
//                        continue;
//                    }
//
//                    for (int i = 0; i < sjf; i++) {
//
//                        cur.consumeCpu(1);
//                        time++;
//                        addArrivals(all, ready, time);
//
//                        if (cur.isCompleted()) {
//                            finish(cur, time);
//                            preempted = true;
//                            break;
//                        }
//
//                        if (shorterJobExists(cur, ready)) {
//                            int remaining = sjf - i - 1;
//                            cur.setQuantum(Q + remaining);
//                            ready.add(cur);
//                            preempted = true;
//                            break;
//                        }
//                    }
//                    if (preempted) {
//                        last = cur;
//                        continue;
//                    }
//
//                    /* ================= QUANTUM FINISHED ================= */
//                    if (!cur.isCompleted()) {
//                        cur.setQuantum(Q + 2);
//                        ready.add(cur);
//                    } else {
//                        finish(cur, time);
//                    }
//
//                    last = cur;
//                }
//
//                /* ===== FINAL STATS ===== */
//                double tw = 0, tt = 0;
//                for (Process p : all) {
//                    int tat = p.getCompletionTime() - p.getArrivalTime();
//                    int wt = tat - p.getBurstTime();
//                    result.waitingTimes.put(p.getProcessName(), wt);
//                    result.turnaroundTimes.put(p.getProcessName(), tat);
//                    result.quantumHistory.put(p.getProcessName(), p.getQuantumHistory());
//                    tw += wt;
//                    tt += tat;
//                }
//
//                result.averageWaiting = tw / all.size();
//                result.averageTurnaround = tt / all.size();

//        return result;

//    }



/* ================= HELPERS ================= */
//
//    private void finish(Process p, int time) {
//        p.setCompletionTime(time);
//        p.setQuantum(0);
//        p.addToQuantumHistory(0);
//    }
//
//    private void addArrivals(List<Process> all, Queue<Process> q, int time) {
//        for (Process p : all)
//            if (p.getArrivalTime() == time && !q.contains(p) && !p.isCompleted())
//                q.add(p);
//    }
//
//    private boolean finished(List<Process> all) {
//        for (Process p : all)
//            if (!p.isCompleted())
//                return false;
//        return true;
//    }
//
//    private boolean higherPriorityExists(Process cur, Queue<Process> q) {
//        for (Process p : q)
//            if (p.getPriority() < cur.getPriority())
//                return true;
//        return false;
//    }
//
//    private boolean shorterJobExists(Process cur, Queue<Process> q) {
//        for (Process p : q)
//            if (p.getRemainingTime() < cur.getRemainingTime())
//                return true;
//        return false;
//    }