package util;

import model.*;
import model.Process;
import java.util.*;

public class DummyScheduler implements Scheduler {
    private final String name;

    public DummyScheduler(String name) { this.name = name; }

    @Override public String getName() { return name; }

    @Override
    public SimulationResult run(SimulationConfig cfg) {
        SimulationResult r = new SimulationResult();
        r.schedulerName = name;
        r.config = cfg;
        r.processes = cfg.deepCopyProcesses();
        List<Process> order = new ArrayList<>(r.processes);
        order.sort(Comparator.comparingInt(Process::getArrivalTime));

        int now = 0;
        for (int i = 0; i < order.size(); i++) {
            Process p = order.get(i);
            if (now < p.getArrivalTime()) {
                r.gantt.add(new TimeSlice(SliceType.IDLE, null, now, p.getArrivalTime()));
                now = p.getArrivalTime();
            }
            List<ProcessView> waiting = new ArrayList<>();
            for (int j = i + 1; j < order.size(); j++)
                if (order.get(j).getArrivalTime() <= now) waiting.add(ProcessView.of(order.get(j)));
            r.snapshots.add(new TimeStepSnapshot(now, p.getId(),
                    new ArrayList<>(Collections.singletonList(ProcessView.of(p))), waiting, new ArrayList<>()));

            p.setStartTime(now);
            r.gantt.add(new TimeSlice(SliceType.RUN, p.getId(), now, now + p.getBurstTime()));
            now += p.getBurstTime();
            p.execute(p.getBurstTime());
            p.setFinishTime(now);
        }
        r.metrics = MetricsCalculator.compute(r.processes, r.gantt);
        return r;
    }
}
