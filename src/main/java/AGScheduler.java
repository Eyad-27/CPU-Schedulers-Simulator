// TODO by Yassin: Implement Part A - The flow (FCFS -> Priority -> SJF).
// TODO by Pedro: Implement Part B - The Quantum updates and History printing.
// Hints for scenarios:
// 1) Used all quantum: move to end of queue; recompute quantum based on history.
// 2) Unused quantum: transfer remaining quantum to next algorithm phase; may get priority boost.
// 3) Completed: record completion and finalize metrics; history should log transitions.

public class AGScheduler implements Scheduler {

    public AGScheduler() {}

    @Override
    public ScheduleResult schedule(Process[] processes, int contextSwitch) {
        // TODO: Implement AG scheduling logic building on FCFS -> Priority -> SJF phases
        // TODO: Keep per-process quantum updated and maintain an execution history output
        return new ScheduleResult();
    }
}

