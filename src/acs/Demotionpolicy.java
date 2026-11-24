package acs;

import model.Process;
import model.Tier;

public class DemotionPolicy {

    public static boolean shouldDemote(Process p, int usedInSlice, int quantum) {
        if (p == null) {
            return false;
        }
        return usedInSlice >= quantum && p.getRemainingTime() > 0;
    }

    public static void demote(Process p, int now, Tier2Queue t2) {
        if (p == null || t2 == null) {
            return;
        }
        p.setTier(Tier.TIER2);
        p.setBatch(true);
        p.setWaitStartTime(now);
        t2.add(p, now);
    }
}
