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
