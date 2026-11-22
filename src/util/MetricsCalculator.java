package util;

import model.*;
import model.Process;
import java.util.List;

public class MetricsCalculator {

    /** turnaround = finish - arrival ; waiting = turnaround - burst ; response = start - arrival */
    public static void finalizeProcessTimes(List<Process> ps) {
        for (Process p : ps) {
            if (p.getFinishTime() < 0 || p.getStartTime() < 0) {
                throw new IllegalStateException("Process " + p.getId()
                        + " has no startTime/finishTime. The scheduler must set them.");
            }
            p.setTurnaroundTime(p.getFinishTime() - p.getArrivalTime());
            p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());
            p.setResponseTime(p.getStartTime() - p.getArrivalTime());
        }
    }

    /** Fills per-process times and returns the averages + CPU utilization. */
    public static Metrics compute(List<Process> ps, List<TimeSlice> gantt) {
        finalizeProcessTimes(ps);
        double w = 0, t = 0, r = 0;
        for (Process p : ps) {
            w += p.getWaitingTime();
            t += p.getTurnaroundTime();
            r += p.getResponseTime();
        }
        int busy = 0, total = 0;
        for (TimeSlice s : gantt) {
            if (s.type == SliceType.RUN) busy += s.end - s.start;
            total = Math.max(total, s.end);
        }
        int n = ps.size();
        Metrics m = new Metrics();
        m.avgWaiting = n == 0 ? 0 : w / n;
        m.avgTurnaround = n == 0 ? 0 : t / n;
        m.avgResponse = n == 0 ? 0 : r / n;
        m.totalTime = total;
        m.cpuUtilization = total == 0 ? 0 : 100.0 * busy / total;
        return m;
    }
}
