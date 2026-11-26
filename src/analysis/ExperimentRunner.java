package analysis;

import java.util.ArrayList;
import java.util.List;

import model.Metrics;
import model.Scheduler;
import model.SimulationConfig;
import model.SimulationResult;


public class ExperimentRunner {

    /** Vary Q, keep CS fixed (taken from base). */
    public static List<ExperimentPoint> sweepQuantum(Scheduler s, SimulationConfig base, int[] qs) {
        List<ExperimentPoint> points = new ArrayList<>();
        for (int q : qs) {
            points.add(runOnce(s, base, q, base.contextSwitch));
        }
        return points;
    }

    /** Vary CS, keep Q fixed (taken from base). */
    public static List<ExperimentPoint> sweepContextSwitch(Scheduler s, SimulationConfig base, int[] css) {
        List<ExperimentPoint> points = new ArrayList<>();
        for (int cs : css) {
            points.add(runOnce(s, base, base.quantum, cs));
        }
        return points;
    }

    /** Every Q x CS combination. Order: for each Q, all CS values. */
    public static List<ExperimentPoint> sweepGrid(Scheduler s, SimulationConfig base, int[] qs, int[] css) {
        List<ExperimentPoint> points = new ArrayList<>();
        for (int q : qs) {
            for (int cs : css) {
                points.add(runOnce(s, base, q, cs));
            }
        }
        return points;
    }

    /** One independent run: fresh config + fresh process copies, then collect the metrics. */
    private static ExperimentPoint runOnce(Scheduler s, SimulationConfig base, int q, int cs) {
        SimulationConfig cfg = new SimulationConfig(base.deepCopyProcesses(), q, cs);
        cfg.agingInterval = base.agingInterval;   // the constructor does not copy it

        SimulationResult result = s.run(cfg);
        Metrics m = result.metrics;

        return new ExperimentPoint(q, cs, m.avgWaiting, m.avgTurnaround, m.avgResponse, m.cpuUtilization);
    }
}