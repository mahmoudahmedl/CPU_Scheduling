package model;

import java.util.ArrayList;
import java.util.List;

/** One row of the "demotion matrix": the state of both tiers at one time step. */
public class TimeStepSnapshot {
    public int time;
    public String runningProcessId;       // null when CPU is idle / context switching
    public List<ProcessView> tier1;
    public List<ProcessView> tier2;
    public List<String> notes;            //  aging explanations, "Context switch saves P1 and loads P2"

    public TimeStepSnapshot(int time, String runningProcessId,
                            List<ProcessView> tier1, List<ProcessView> tier2, List<String> notes) {
        this.time = time;
        this.runningProcessId = runningProcessId;
        this.tier1 = tier1 == null ? new ArrayList<>() : tier1;
        this.tier2 = tier2 == null ? new ArrayList<>() : tier2;
        this.notes = notes == null ? new ArrayList<>() : notes;
    }
}
