package model;

public interface Scheduler {
    String getName();
    SimulationResult run(SimulationConfig config);
}
