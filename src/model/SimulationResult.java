package model;

import java.util.ArrayList;
import java.util.List;

/** Everything a scheduler returns from run(). */
public class SimulationResult {
    public String schedulerName;
    public SimulationConfig config;
    public List<Process> processes = new ArrayList<>();          // the finished copies
    public List<TimeSlice> gantt = new ArrayList<>();
    public List<TimeStepSnapshot> snapshots = new ArrayList<>();
    public Metrics metrics;
}
