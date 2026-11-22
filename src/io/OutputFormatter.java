package io;

import model.*;
import model.Process;
import java.util.*;

/** Prints the per-time-step tier matrix, the process table, averages and the Gantt chart. Always Locale.US. */
public class OutputFormatter {

    // ---------------- tier matrix (Requirement 2) ----------------

    public static String snapshotsToText(SimulationResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "%-6s | %-8s | %s%n", "Time", "Process", "Tier"));
        sb.append(repeat('-', 100)).append('\n');
        for (TimeStepSnapshot s : r.snapshots) sb.append(snapshotToText(s, isSJF(r)));
        return sb.toString();
    }

    /** One matrix row (used by the GUI step viewer too). */
    public static String snapshotToText(TimeStepSnapshot s, boolean sjf) {
        StringBuilder sb = new StringBuilder();
        String run = s.runningProcessId == null ? "-" : s.runningProcessId;
        if (sjf) {
            sb.append(String.format(Locale.US, "%-6d | %-8s | %s%n", s.time, run, tierToString("Ready queue", s.tier1)));
        } else {
            sb.append(String.format(Locale.US, "%-6d | %-8s | %s%n", s.time, run, tierToString("Tier 1", s.tier1)));
            sb.append(String.format(Locale.US, "%-6s | %-8s | %s%n", "", "", tierToString("Tier 2", s.tier2)));
        }
        for (String note : s.notes)
            sb.append(String.format(Locale.US, "%-6s | %-8s | %s%n", "", "", note));
        return sb.toString();
    }

    private static String tierToString(String label, List<ProcessView> v) {
        if (v == null || v.isEmpty()) return label + ": [Empty]";
        StringBuilder sb = new StringBuilder(label + ": [");
        for (int i = 0; i < v.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(v.get(i));
        }
        return sb.append("]").toString();
    }

    private static boolean isSJF(SimulationResult r) {
        return r.schedulerName != null && r.schedulerName.toUpperCase().contains("SJF");
    }

    // ---------------- process table + averages ----------------

    public static String processTableToText(SimulationResult r) {
        List<Process> ps = sortedById(r.processes);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "%-8s | %-8s | %-7s | %-10s | %-7s | %-8s%n",
                "Process", "Arrival", "Finish", "Turnaround", "Waiting", "Response"));
        sb.append(repeat('-', 66)).append('\n');
        for (Process p : ps) {
            sb.append(String.format(Locale.US, "%-8s | %-8d | %-7d | %-10d | %-7d | %-8d%n",
                    p.getId(), p.getArrivalTime(), p.getFinishTime(),
                    p.getTurnaroundTime(), p.getWaitingTime(), p.getResponseTime()));
        }
        sb.append('\n');
        Metrics m = r.metrics;
        sb.append(averageLine("Average Turnaround Time", ps, 't', m.avgTurnaround));
        sb.append(averageLine("Average Waiting Time", ps, 'w', m.avgWaiting));
        sb.append(averageLine("Average Response Time", ps, 'r', m.avgResponse));
        sb.append(String.format(Locale.US, "CPU Utilization: %.2f%% (total time = %d)%n", m.cpuUtilization, m.totalTime));
        return sb.toString();
    }

    /** Builds a line like: Average Waiting Time: (5 + 5 + 3)/3 = 4.33 time units */
    private static String averageLine(String label, List<Process> ps, char kind, double avg) {
        StringBuilder sum = new StringBuilder();
        for (int i = 0; i < ps.size(); i++) {
            if (i > 0) sum.append(" + ");
            Process p = ps.get(i);
            sum.append(kind == 't' ? p.getTurnaroundTime() : kind == 'w' ? p.getWaitingTime() : p.getResponseTime());
        }
        return String.format(Locale.US, "%s: (%s)/%d = %.2f time units%n", label, sum, ps.size(), avg);
    }

    // ---------------- Gantt ----------------

    public static String ganttToText(SimulationResult r) {
        StringBuilder sb = new StringBuilder("|");
        for (TimeSlice s : r.gantt) {
            String name = s.type == SliceType.RUN ? s.processId : s.type == SliceType.CONTEXT_SWITCH ? "CS" : "IDLE";
            sb.append(' ').append(name).append(' ').append(s.start).append('-').append(s.end).append(" |");
        }
        return sb.toString();
    }

    // ---------------- everything ----------------

    public static String allToText(SimulationResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("==== ").append(r.schedulerName).append(" ====\n");
        if (r.config != null)
            sb.append("Q = ").append(r.config.quantum).append(", CS = ").append(r.config.contextSwitch).append("\n\n");
        sb.append("Gantt chart:\n").append(ganttToText(r)).append("\n\n");
        sb.append("Tier matrix per time step:\n").append(snapshotsToText(r)).append('\n');
        sb.append(processTableToText(r));
        return sb.toString();
    }

    public static void printAll(SimulationResult r) {
        System.out.println(allToText(r));
    }

    /** Small table comparing several results (used by "run both"). */
    public static String comparisonToText(List<SimulationResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "%-34s | %-8s | %-10s | %-8s | %-8s%n",
                "Scheduler", "Avg WT", "Avg TAT", "Avg RT", "CPU %"));
        sb.append(repeat('-', 80)).append('\n');
        for (SimulationResult r : results) {
            sb.append(String.format(Locale.US, "%-34s | %-8.2f | %-10.2f | %-8.2f | %-8.2f%n",
                    r.schedulerName, r.metrics.avgWaiting, r.metrics.avgTurnaround,
                    r.metrics.avgResponse, r.metrics.cpuUtilization));
        }
        return sb.toString();
    }

    // ---------------- helpers ----------------

    private static List<Process> sortedById(List<Process> in) {
        List<Process> out = new ArrayList<>(in);
        out.sort((a, b) -> {
            String x = a.getId(), y = b.getId();
            if (x.length() != y.length()) return Integer.compare(x.length(), y.length());
            return x.compareTo(y);
        });
        return out;
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}
