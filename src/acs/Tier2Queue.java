package acs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import model.Process;
import model.ProcessView;
import model.Tier;

public class Tier2Queue {

    private final List<Process> queue;
    private final AgingPolicy aging;

    public Tier2Queue(AgingPolicy aging) {
        this.queue = new ArrayList<>();
        this.aging = aging != null ? aging : new AgingPolicy(4);
    }

    public void add(Process p, int now) {
        if (p == null) {
            return;
        }
        p.setTier(Tier.TIER2);
        p.setBatch(true);
        if (p.getWaitStartTime() < 0) {
            p.setWaitStartTime(now);
        }
        p.setCurrentPriority(aging.effectivePriority(p, now));
        queue.add(p);
    }

    public Process pickNext(int now) {
        if (queue.isEmpty()) {
            return null;
        }
        aging.applyAgingToAll(queue, now);

        Comparator<Process> comparator = Comparator
            .comparingInt((Process p) -> aging.effectivePriority(p, now))
            .thenComparingInt(Process::getWaitStartTime)
            .thenComparing(Process::getId);

        Process best = queue.stream().min(comparator).orElse(null);
        if (best != null) {
            queue.remove(best);
        }
        return best;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public List<Process> getProcesses() {
        return new ArrayList<>(queue);
    }

    public List<ProcessView> snapshotView(int now) {
        aging.applyAgingToAll(queue, now);

        List<Process> sorted = new ArrayList<>(queue);
        sorted.sort(
            Comparator.comparingInt((Process p) -> aging.effectivePriority(p, now))
                .thenComparingInt(Process::getWaitStartTime)
                .thenComparing(Process::getId)
        );

        List<ProcessView> views = new ArrayList<>();
        for (Process p : sorted) {
            views.add(ProcessView.of(p));
        }
        return views;
    }
}
