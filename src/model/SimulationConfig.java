package model;

import java.util.ArrayList;
import java.util.List;

/** Everything a scheduler needs: processes, quantum, context-switch cost, aging interval. */
public class SimulationConfig {
    public List<Process> processes;
    public int quantum;
    public int contextSwitch;
    public int agingInterval = 4;   // priority improves by floor(wait / agingInterval)

    public SimulationConfig(List<Process> processes, int quantum, int contextSwitch) {
        this.processes = processes;
        this.quantum = quantum;
        this.contextSwitch = contextSwitch;
    }

    /** Schedulers must work on this copy and never touch the original list. */
    public List<Process> deepCopyProcesses() {
        List<Process> copy = new ArrayList<>();
        for (Process p : processes) copy.add(p.copy());
        return copy;
    }
}
