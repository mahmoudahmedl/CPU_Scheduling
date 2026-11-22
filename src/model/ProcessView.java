package model;

/* Read-only picture of a process inside a queue (used by the tier matrix). */
public class ProcessView {
    public String id;
    public int remaining;
    public int priority;

    public ProcessView(String id, int remaining, int priority) {
        this.id = id;
        this.remaining = remaining;
        this.priority = priority;
    }

    public static ProcessView of(Process p) {
        return new ProcessView(p.getId(), p.getRemainingTime(), p.getCurrentPriority());
    }

    @Override
    public String toString() {
        return id + " (remaining: " + remaining + ", Priority: " + priority + ")";
    }
}
