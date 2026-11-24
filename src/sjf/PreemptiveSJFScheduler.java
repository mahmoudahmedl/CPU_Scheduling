package sjf;

import java.util.ArrayList;
import java.util.List;
import model.*;
import model.Process;
import util.MetricsCalculator;

public class PreemptiveSJFScheduler implements Scheduler {

    @Override
    public String getName() {
        return "Preemptive SJF";
    }

    @Override
    public SimulationResult run(SimulationConfig cfg) {
        SimulationResult r = new SimulationResult();
        r.schedulerName = getName();
        r.config = cfg;
        r.gantt = new ArrayList<>();
        r.snapshots = new ArrayList<>();
        r.processes = cfg.deepCopyProcesses();
        List<Process> all = r.processes;

        List<Process> ready = new ArrayList<>();
        Process last = null;
        int now = 0;
        int finished = 0;

        while (finished < all.size()) {
            admitArrivals(now, all, ready);

            if (ready.isEmpty()) {
                int next = pickNextArrival(all, now);
                addSlice(r, SliceType.IDLE, null, now, next);
                now = next;
                last = null;
                continue;
            }

            Process current = pickShortest(ready);

            if (last != null && current != last && cfg.contextSwitch > 0) {
                addSlice(r, SliceType.CONTEXT_SWITCH, null, now, now + cfg.contextSwitch);
                now = now + cfg.contextSwitch;
            }

            recordSnapshot(now, current, ready, r);

            if (!current.hasStarted()) {
                current.setStartTime(now);
            }

            current.execute(1);
            addSlice(r, SliceType.RUN, current.getId(), now, now + 1);
            now++;
            last = current;

            if (current.getRemainingTime() == 0) {
                current.setFinishTime(now);
                finished++;
            }
        }

        MetricsCalculator.finalizeProcessTimes(all);
        r.metrics = MetricsCalculator.compute(all, r.gantt);
        return r;
    }

    private void admitArrivals(int now, List<Process> all, List<Process> ready) {
        ready.clear();
        for (Process p : all) {
            if (p.getArrivalTime() <= now && p.getRemainingTime() > 0) {
                ready.add(p);
            }
        }
    }

    private Process pickShortest(List<Process> ready) {
        Process best = null;
        for (Process p : ready) {
            if (best == null) {
                best = p;
            } else if (p.getRemainingTime() < best.getRemainingTime()) {
                best = p;
            } else if (p.getRemainingTime() == best.getRemainingTime()) {
                if (p.getArrivalTime() < best.getArrivalTime()) {
                    best = p;
                } else if (p.getArrivalTime() == best.getArrivalTime()) {
                    if (p.getId().compareTo(best.getId()) < 0) {
                        best = p;
                    }
                }
            }
        }
        return best;
    }

    private int pickNextArrival(List<Process> all, int now) {
        int nextArrival = Integer.MAX_VALUE;
        for (Process p : all) {
            if (p.getArrivalTime() > now && p.getRemainingTime() > 0) {
                nextArrival = Math.min(nextArrival, p.getArrivalTime());
            }
        }
        return nextArrival;
    }

    private void addSlice(SimulationResult r, SliceType type, String id, int start, int end) {
        if (!r.gantt.isEmpty() && type == SliceType.RUN) {
            TimeSlice prev = r.gantt.get(r.gantt.size() - 1);
            if (prev.type == SliceType.RUN && prev.processId.equals(id) && prev.end == start) {
                prev.end = end;
                return;
            }
        }
        r.gantt.add(new TimeSlice(type, id, start, end));
    }

    private void recordSnapshot(int now, Process running, List<Process> ready, SimulationResult r) {
        List<ProcessView> t1 = new ArrayList<>();
        for (Process p : ready) {
            t1.add(ProcessView.of(p));
        }
        r.snapshots.add(new TimeStepSnapshot(now, running.getId(), t1, new ArrayList<>(), new ArrayList<>()));
    }
}