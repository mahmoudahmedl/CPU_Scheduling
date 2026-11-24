package acs;

import model.*;
import model.Process;
import util.MetricsCalculator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Adaptive Container Scheduler.
 *
 * Tier 1 is Round Robin. A process that consumes a complete quantum and still
 * has work remaining is demoted to Tier 2. Tier 2 is non-preemptive priority.
 *
 * NOTE: Tier 2 and aging are implemented privately here as an integration
 * fallback because Member 4's Tier2Queue/AgingPolicy classes are not present
 * in this project ZIP yet. They can be extracted/replaced when Member 4
 * delivers those files; no existing project file needs to be changed.
 */
public class ACSScheduler implements Scheduler {

    private Tier1Queue tier1;
    private final List<Process> tier2 = new ArrayList<>();
    private List<Process> all;
    private SimulationResult result;
    private int quantum;
    private int agingInterval;
    private int now;
    private Process lastProcess;

    @Override
    public String getName() {
        return "Adaptive Container Scheduler (ACS)";
    }

    @Override
    public SimulationResult run(SimulationConfig cfg) {
        if (cfg == null) {
            throw new IllegalArgumentException("SimulationConfig cannot be null.");
        }
        if (cfg.quantum <= 0) {
            throw new IllegalArgumentException("Time quantum must be greater than zero.");
        }

        tier1 = new Tier1Queue();
        tier2.clear();
        all = cfg.deepCopyProcesses();
        result = new SimulationResult();
        result.schedulerName = getName();
        result.config = cfg;
        result.processes = all;
        result.gantt = new ArrayList<>();
        result.snapshots = new ArrayList<>();
        quantum = cfg.quantum;
        agingInterval = cfg.agingInterval > 0 ? cfg.agingInterval : 4;
        now = 0;
        lastProcess = null;

        int finished = 0;
        while (finished < all.size()) {
            admitArrivals(now);

            if (tier1.isEmpty() && tier2.isEmpty()) {
                int next = nextArrivalTime();
                if (next == Integer.MAX_VALUE) {
                    break;
                }
                addSlice(SliceType.IDLE, null, now, next);
                now = next;
                lastProcess = null;
                continue;
            }

            Process selected = selectNext();
            if (selected == null) {
                continue;
            }

            if (lastProcess != null && lastProcess != selected && cfg.contextSwitch > 0) {
                addSlice(SliceType.CONTEXT_SWITCH, null, now, now + cfg.contextSwitch);
                now += cfg.contextSwitch;
                admitArrivals(now);
            }

            if (selected.getTier() == Tier.TIER1) {
                SliceOutcome outcome = runTier1Slice(selected);
                if (outcome == SliceOutcome.FINISHED) {
                    finished++;
                } else {
                    // Arrivals at this exact time are admitted before the
                    // just-demoted process enters Tier 2.
                    admitArrivals(now);
                    demote(selected);
                }
            } else {
                runTier2Burst(selected);
                finished++;
            }
            lastProcess = selected;
        }

        result.metrics = MetricsCalculator.compute(all, result.gantt);
        return result;
    }

    /** Admit every not-yet-admitted process whose arrival time has passed. */
    private void admitArrivals(int time) {
        for (Process p : all) {
            if (p.getTier() == Tier.NONE
                    && p.getRemainingTime() > 0
                    && p.getArrivalTime() <= time) {
                p.setTier(Tier.TIER1);
                tier1.enqueue(p);
            }
        }
    }

    /** Tier 1 always has priority over Tier 2. */
    private Process selectNext() {
        if (!tier1.isEmpty()) {
            return tier1.dequeue();
        }
        if (tier2.isEmpty()) {
            return null;
        }
        updateAging();
        tier2.sort(Comparator
                .comparingInt(Process::getCurrentPriority)
                .thenComparingInt(Process::getWaitStartTime)
                .thenComparing(Process::getId));
        return tier2.get(0);
    }

    private SliceOutcome runTier1Slice(Process p) {
        recordSnapshot(p, "Tier 1 Round Robin");
        if (!p.hasStarted()) {
            p.setStartTime(now);
        }

        int used = Math.min(quantum, p.getRemainingTime());
        int start = now;
        p.execute(used);
        now += used;
        addSlice(SliceType.RUN, p.getId(), start, now);

        if (p.isFinished()) {
            p.setFinishTime(now);
            p.setTier(Tier.NONE);
            return SliceOutcome.FINISHED;
        }

        return used == quantum ? SliceOutcome.DEMOTED : SliceOutcome.FINISHED;
    }

    /** Tier 2 is non-preemptive: once selected, run until completion. */
    private void runTier2Burst(Process p) {
        recordSnapshot(p, "Tier 2 non-preemptive priority");
        if (!p.hasStarted()) {
            p.setStartTime(now);
        }

        int start = now;
        int duration = p.getRemainingTime();
        p.execute(duration);
        now += duration;
        addSlice(SliceType.RUN, p.getId(), start, now);
        p.setFinishTime(now);
        p.setTier(Tier.NONE);
        tier2.remove(p);
    }

    private void demote(Process p) {
        p.setTier(Tier.TIER2);
        p.setBatch(true);
        p.setWaitStartTime(now);
        p.setCurrentPriority(p.getInitialPriority());
        tier2.add(p);
    }

    private void updateAging() {
        for (Process p : tier2) {
            int waited = Math.max(0, now - p.getWaitStartTime());
            int improvement = waited / agingInterval;
            // Smaller numeric priority means higher priority; keep it >= 1.
            p.setCurrentPriority(Math.max(1, p.getInitialPriority() - improvement));
        }
    }

    private int nextArrivalTime() {
        int next = Integer.MAX_VALUE;
        for (Process p : all) {
            if (p.getTier() == Tier.NONE && p.getRemainingTime() > 0
                    && p.getArrivalTime() > now) {
                next = Math.min(next, p.getArrivalTime());
            }
        }
        return next;
    }

    private void recordSnapshot(Process running, String note) {
        List<ProcessView> t1 = tier1.snapshotView();
        List<ProcessView> t2 = new ArrayList<>();
        updateAging();
        tier2.sort(Comparator
                .comparingInt(Process::getCurrentPriority)
                .thenComparingInt(Process::getWaitStartTime)
                .thenComparing(Process::getId));
        for (Process p : tier2) {
            t2.add(ProcessView.of(p));
        }
        List<String> notes = new ArrayList<>();
        notes.add(note);
        result.snapshots.add(new TimeStepSnapshot(now, running.getId(), t1, t2, notes));
    }

    private void addSlice(SliceType type, String id, int start, int end) {
        if (end <= start) {
            return;
        }
        if (type == SliceType.RUN && !result.gantt.isEmpty()) {
            TimeSlice previous = result.gantt.get(result.gantt.size() - 1);
            if (previous.type == SliceType.RUN
                    && previous.processId != null
                    && previous.processId.equals(id)
                    && previous.end == start) {
                previous.end = end;
                return;
            }
        }
        result.gantt.add(new TimeSlice(type, id, start, end));
    }
}
