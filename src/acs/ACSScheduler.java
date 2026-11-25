package acs;

import model.*;
import model.Process;
import util.MetricsCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptive Container Scheduler.
 *
 * Tier 1: Round Robin (Tier1Queue). A process that uses a full quantum and
 *         still has work left is demoted (DemotionPolicy) to Tier 2.
 * Tier 2: Non-preemptive priority (Tier2Queue).
 * Tier 3: Aging (AgingPolicy) - the longer a process waits in Tier 2,
 *         the better (smaller) its priority number becomes.
 */
public class ACSScheduler implements Scheduler {

    private Tier1Queue tier1;
    private Tier2Queue tier2;
    private List<Process> all;
    private SimulationResult result;
    private int quantum;
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

        int agingInterval = cfg.agingInterval > 0 ? cfg.agingInterval : 4;
        tier1 = new Tier1Queue();
        tier2 = new Tier2Queue(new AgingPolicy(agingInterval));
        all = cfg.deepCopyProcesses();
        result = new SimulationResult();
        result.schedulerName = getName();
        result.config = cfg;
        result.processes = all;
        result.gantt = new ArrayList<>();
        result.snapshots = new ArrayList<>();
        quantum = cfg.quantum;
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
                    // Arrivals at this exact time enter Tier 1 before the
                    // just-demoted process enters Tier 2.
                    admitArrivals(now);
                    DemotionPolicy.demote(selected, now, tier2);
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
        // Tier2Queue applies aging, picks the best priority and removes it.
        return tier2.pickNext(now);
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

        return DemotionPolicy.shouldDemote(p, used, quantum)
                ? SliceOutcome.DEMOTED
                : SliceOutcome.FINISHED;
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

    /**
     * The running process was already removed from its queue, so it is put
     * back at the front of its tier's view (it has the best priority anyway).
     */
    private void recordSnapshot(Process running, String note) {
        List<ProcessView> t1 = new ArrayList<>();
        List<ProcessView> t2 = new ArrayList<>();

        if (running.getTier() == Tier.TIER1) {
            t1.add(ProcessView.of(running));
        }
        t1.addAll(tier1.snapshotView());

        if (running.getTier() == Tier.TIER2) {
            t2.add(ProcessView.of(running));
        }
        t2.addAll(tier2.snapshotView(now));

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