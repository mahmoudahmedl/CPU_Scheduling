package acs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import model.Process;
import model.Tier;

public class AgingPolicy {

    private final int agingInterval;
    private final int minPriority;

    public AgingPolicy(int agingInterval) {
        this(agingInterval, 1);
    }

    public AgingPolicy(int agingInterval, int minPriority) {
        this.agingInterval = agingInterval > 0 ? agingInterval : 4;
        this.minPriority = minPriority;
    }

    public int getAgingInterval() {
        return agingInterval;
    }

    public int effectivePriority(Process p, int now) {
        int waitTime = Math.max(0, now - p.getWaitStartTime());
        int boost = waitTime / agingInterval;
        int effective = p.getInitialPriority() - boost;
        return Math.max(minPriority, effective);
    }

    public void applyAgingToAll(Collection<Process> ps, int now) {
        if (ps == null) {
            return;
        }
        for (Process p : ps) {
            if (p != null && p.getTier() == Tier.TIER2 && !p.isFinished()) {
                p.setCurrentPriority(effectivePriority(p, now));
            }
        }
    }

    public List<String> describeAging(Collection<Process> ps, int now) {
        List<String> notes = new ArrayList<>();
        if (ps == null) {
            return notes;
        }
        for (Process p : ps) {
            if (p != null && p.getTier() == Tier.TIER2 && !p.isFinished()) {
                int start = p.getWaitStartTime();
                int waited = Math.max(0, now - start);
                int boost = waited / agingInterval;
                if (boost > 0) {
                    notes.add(String.format(
                        "%s: Waited from T=%d to T=%d (%d units). Aging: floor(%d/%d) = %d",
                        p.getId(), start, now, waited, waited, agingInterval, boost
                    ));
                }
            }
        }
        return notes;
    }
}
