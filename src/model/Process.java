package model;

public class Process {
    private final String id;
    private final int arrivalTime;
    private final int burstTime;
    private final int initialPriority;

    private int remainingTime;
    private int currentPriority;
    private Tier tier = Tier.NONE;
    private int startTime = -1;       // -1 = never ran yet
    private int finishTime = -1;
    private int waitStartTime = -1;   // time it entered Tier 2 (used by aging)
    private boolean batch = false;    // true after demotion to Tier 2
    private int waitingTime;
    private int turnaroundTime;
    private int responseTime;

    public Process(String id, int arrivalTime, int burstTime, int initialPriority) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.initialPriority = initialPriority;
        reset();
    }

    public void reset() {
        remainingTime = burstTime;
        currentPriority = initialPriority;
        tier = Tier.NONE;
        startTime = -1;
        finishTime = -1;
        waitStartTime = -1;
        batch = false;
        waitingTime = 0;
        turnaroundTime = 0;
        responseTime = 0;
    }

    /** Fresh copy (input data only, no runtime data). */
    public Process copy() {
        return new Process(id, arrivalTime, burstTime, initialPriority);
    }

    /** Run this process for the given number of time units. */
    public void execute(int units) {
        remainingTime -= units;
        if (remainingTime < 0) remainingTime = 0;
    }

    public boolean isFinished() { return remainingTime == 0; }
    public boolean hasStarted() { return startTime != -1; }

    public String getId() { return id; }
    public int getArrivalTime() { return arrivalTime; }
    public int getBurstTime() { return burstTime; }
    public int getInitialPriority() { return initialPriority; }
    public int getRemainingTime() { return remainingTime; }
    public int getCurrentPriority() { return currentPriority; }
    public Tier getTier() { return tier; }
    public int getStartTime() { return startTime; }
    public int getFinishTime() { return finishTime; }
    public int getWaitStartTime() { return waitStartTime; }
    public boolean isBatch() { return batch; }
    public int getWaitingTime() { return waitingTime; }
    public int getTurnaroundTime() { return turnaroundTime; }
    public int getResponseTime() { return responseTime; }

    public void setRemainingTime(int v) { this.remainingTime = v; }
    public void setCurrentPriority(int v) { this.currentPriority = v; }
    public void setTier(Tier t) { this.tier = t; }
    public void setStartTime(int v) { this.startTime = v; }
    public void setFinishTime(int v) { this.finishTime = v; }
    public void setWaitStartTime(int v) { this.waitStartTime = v; }
    public void setBatch(boolean b) { this.batch = b; }
    public void setWaitingTime(int v) { this.waitingTime = v; }
    public void setTurnaroundTime(int v) { this.turnaroundTime = v; }
    public void setResponseTime(int v) { this.responseTime = v; }

    @Override
    public String toString() { return id; }
}
