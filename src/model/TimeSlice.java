package model;

/** One block on the Gantt chart: [start, end) */
public class TimeSlice {
    public SliceType type;
    public String processId;   // null for IDLE (for CONTEXT_SWITCH you may put the process being loaded)
    public int start;
    public int end;

    public TimeSlice(SliceType type, String processId, int start, int end) {
        this.type = type;
        this.processId = processId;
        this.start = start;
        this.end = end;
    }
}
