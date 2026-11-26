package analysis;

import java.util.Locale;


public class ExperimentPoint {

    // Inputs
    public int quantum;
    public int contextSwitch;

    // Outputs
    public double avgWaiting;
    public double avgTurnaround;
    public double avgResponse;
    public double cpuUtilization;   // percent (0-100)

    public ExperimentPoint(int quantum, int contextSwitch,
                           double avgWaiting, double avgTurnaround,
                           double avgResponse, double cpuUtilization) {
        this.quantum = quantum;
        this.contextSwitch = contextSwitch;
        this.avgWaiting = avgWaiting;
        this.avgTurnaround = avgTurnaround;
        this.avgResponse = avgResponse;
        this.cpuUtilization = cpuUtilization;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "Q=%d, CS=%d, avgWaiting=%.2f, avgTurnaround=%.2f, avgResponse=%.2f, cpuUtil=%.2f%%",
                quantum, contextSwitch, avgWaiting, avgTurnaround, avgResponse, cpuUtilization);
    }
}