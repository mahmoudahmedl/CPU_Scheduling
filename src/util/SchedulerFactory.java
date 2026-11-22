package util;

import model.Scheduler;


public class SchedulerFactory {
    public static final String SJF = "SJF";
    public static final String ACS = "ACS";

    public static Scheduler create(String key) {
        String className = SJF.equals(key) ? "sjf.PreemptiveSJFScheduler" : "acs.ACSScheduler";
        try {
            return (Scheduler) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            System.out.println("[Warning] " + className + " not found yet -> using DummyScheduler (FCFS).");
            return new DummyScheduler(key + " (dummy)");
        }
    }
}
