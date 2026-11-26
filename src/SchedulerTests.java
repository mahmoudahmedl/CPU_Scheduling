import acs.*;
import io.InputParser;
import io.OutputFormatter;
import model.*;
import model.Process;
import sjf.PreemptiveSJFScheduler;
import util.MetricsCalculator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Self-checking tests for the whole project (SJF, ACS, Aging, Demotion, IO, Metrics).
 * It decides PASS / FAIL by itself. Put this file in src/ (default package, next to Main.java) and run it.
 *
 * Sections:
 *   1. SJF known answers          (hand-calculated, tie rule: earlier arrival, then smaller id)
 *   2. ACS known answers          (Test Case 1 and 2 from the assignment PDF + hand-calculated cases)
 *   3. Component tests            (AgingPolicy, Tier2Queue, DemotionPolicy, Tier1Queue)
 *   4. Metrics / IO / edge cases
 *   5. Random stress test         (hundreds of random cases, generic invariants for both schedulers)
 */
public class SchedulerTests {

    static int pass = 0, fail = 0;
    static final List<String> failures = new ArrayList<>();
    static final List<String> notes = new ArrayList<>();
    static boolean verbose = true;

    // ------------------------------------------------------------------ main

    public static void main(String[] args) throws Exception {
        // watchdog: a scheduler that loops forever must not freeze the tests
        Thread dog = new Thread(() -> {
            try { Thread.sleep(120_000); } catch (InterruptedException e) { return; }
            System.out.println("\nTIMEOUT: tests ran for more than 120 seconds. A scheduler probably has an infinite loop.");
            System.exit(2);
        });
        dog.setDaemon(true);
        dog.start();

        section("1. SJF known answers");
        safely("section 1", SchedulerTests::sjfKnownAnswers);
        section("2. ACS known answers (PDF Test Case 1 and 2 + hand-calculated)");
        safely("section 2", SchedulerTests::acsKnownAnswers);
        section("3. Component tests (Aging, Tier2Queue, Demotion, Tier1Queue)");
        safely("section 3", SchedulerTests::componentTests);
        section("4. Metrics, IO and edge cases");
        safely("section 4", SchedulerTests::metricsIoAndEdgeTests);
        section("5. Random stress test (invariants on 600 random cases x 2 schedulers)");
        verbose = false;
        safely("section 5", () -> randomStress(600, 2026L));
        verbose = true;

        System.out.println();
        System.out.println("==================================================");
        for (String n : notes) System.out.println("NOTE: " + n);
        if (!notes.isEmpty()) System.out.println("--------------------------------------------------");
        System.out.println("PASSED: " + pass + "    FAILED: " + fail);
        if (fail > 0) {
            System.out.println("\nFailures:");
            for (String f : failures) System.out.println("  - " + f);
        }
        System.out.println(fail == 0 ? "RESULT: ALL TESTS PASSED" : "RESULT: SOME TESTS FAILED");
        System.exit(fail == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ 1. SJF

    static void sjfKnownAnswers() {
        Scheduler s = new PreemptiveSJFScheduler();

        known("SJF A (your screenshot)", s, cfg(3, 0, new int[][]{{0, 6, 2}, {1, 4, 1}, {3, 2, 1}}),
                "P1 0-1|P2 1-5|P3 5-7|P1 7-12",
                new int[][]{{12, 12, 6, 0}, {5, 4, 0, 0}, {7, 4, 2, 2}}, 8.0 / 3, 20.0 / 3, Double.NaN);

        known("SJF B (classic SRTF example)", s, cfg(3, 0, new int[][]{{0, 8, 1}, {1, 4, 1}, {2, 9, 1}, {3, 5, 1}}),
                "P1 0-1|P2 1-5|P4 5-10|P1 10-17|P3 17-26",
                new int[][]{{17, 17, 9, 0}, {5, 4, 0, 0}, {26, 24, 15, 15}, {10, 7, 2, 2}}, 6.5, 13.0, Double.NaN);

        known("SJF C (idle gaps)", s, cfg(3, 0, new int[][]{{2, 3, 1}, {10, 2, 1}}),
                "IDLE 0-2|P1 2-5|IDLE 5-10|P2 10-12",
                new int[][]{{5, 3, 0, 0}, {12, 2, 0, 0}}, 0.0, 2.5, 100.0 * 5 / 12);

        known("SJF D (context switch = 1)", s, cfg(3, 1, new int[][]{{0, 6, 1}, {1, 2, 1}}),
                "P1 0-1|CS 1-2|P2 2-4|CS 4-5|P1 5-10",
                new int[][]{{10, 10, 4, 0}, {4, 3, 1, 1}}, 2.5, 6.5, 80.0);

        known("SJF E (all arrive at 0, ties by id)", s, cfg(3, 0, new int[][]{{0, 3, 1}, {0, 3, 1}, {0, 1, 1}}),
                "P3 0-1|P1 1-4|P2 4-7",
                new int[][]{{4, 4, 1, 1}, {7, 7, 4, 4}, {1, 1, 0, 0}}, 5.0 / 3, 4.0, 100.0);

        known("SJF F (chain of preemptions)", s, cfg(3, 0, new int[][]{{0, 7, 1}, {2, 4, 1}, {4, 1, 1}, {5, 4, 1}}),
                "P1 0-2|P2 2-4|P3 4-5|P2 5-7|P4 7-11|P1 11-16",
                new int[][]{{16, 16, 9, 0}, {7, 5, 1, 0}, {5, 1, 0, 0}, {11, 6, 2, 2}}, 3.0, 7.0, 100.0);

        known("SJF G (Initial Priority is ignored)", s, cfg(3, 0, new int[][]{{0, 5, 9}, {0, 2, 9}, {0, 3, 1}}),
                "P2 0-2|P3 2-5|P1 5-10",
                new int[][]{{10, 10, 5, 5}, {2, 2, 0, 0}, {5, 5, 2, 2}}, 7.0 / 3, 17.0 / 3, 100.0);
    }

    // ------------------------------------------------------------------ 2. ACS

    static void acsKnownAnswers() {
        Scheduler s = new ACSScheduler();

        known("ACS PDF Test Case 1 (Q=3, CS=0)", s, cfg(3, 0, new int[][]{{0, 4, 2}, {1, 7, 3}, {3, 2, 1}}),
                "P1 0-3|P2 3-6|P3 6-8|P1 8-9|P2 9-13",
                new int[][]{{9, 9, 5, 0}, {13, 12, 5, 2}, {8, 5, 3, 3}}, 13.0 / 3, 26.0 / 3, 100.0);

        known("ACS PDF Test Case 2 (Q=3, CS=1)", s, cfg(3, 1, new int[][]{{0, 5, 4}, {1, 6, 2}, {4, 4, 3}}),
                "P1 0-3|CS 3-4|P2 4-7|CS 7-8|P3 8-11|CS 11-12|P2 12-15|CS 15-16|P1 16-18|CS 18-19|P3 19-20",
                new int[][]{{18, 18, 13, 0}, {15, 14, 8, 3}, {20, 16, 12, 4}}, 11.0, 16.0, 75.0);

        known("ACS A (all jobs fit in one quantum: no demotion)", s, cfg(3, 0, new int[][]{{0, 2, 3}, {1, 3, 2}, {2, 1, 1}}),
                "P1 0-2|P2 2-5|P3 5-6",
                new int[][]{{2, 2, 0, 0}, {5, 4, 1, 1}, {6, 4, 3, 3}}, 4.0 / 3, 10.0 / 3, 100.0);

        known("ACS B (single long job)", s, cfg(4, 0, new int[][]{{0, 10, 1}}),
                "P1 0-10", new int[][]{{10, 10, 0, 0}}, 0.0, 10.0, 100.0);

        known("ACS C (idle at start)", s, cfg(3, 0, new int[][]{{5, 2, 1}}),
                "IDLE 0-5|P1 5-7", new int[][]{{7, 2, 0, 0}}, 0.0, 2.0, 100.0 * 2 / 7);

        known("ACS D (Tier 2 is non-preemptive, new job waits)", s, cfg(2, 0, new int[][]{{0, 6, 2}, {3, 1, 5}}),
                "P1 0-6|P2 6-7",
                new int[][]{{6, 6, 0, 0}, {7, 4, 3, 3}}, 1.5, 5.0, 100.0);

        known("ACS E (Tier 1 always beats Tier 2)", s, cfg(2, 0, new int[][]{{0, 4, 1}, {1, 2, 1}, {2, 2, 1}}),
                "P1 0-2|P2 2-4|P3 4-6|P1 6-8",
                new int[][]{{8, 8, 4, 0}, {4, 3, 1, 1}, {6, 4, 2, 2}}, 7.0 / 3, 5.0, 100.0);

        known("ACS F (aging: P1 beats P3 although its priority number is worse)", s,
                cfg(2, 0, new int[][]{{0, 3, 5}, {0, 8, 1}, {0, 4, 4}}),
                "P1 0-2|P2 2-4|P3 4-6|P2 6-12|P1 12-13|P3 13-15",
                new int[][]{{13, 13, 10, 0}, {12, 12, 4, 2}, {15, 15, 11, 4}}, 25.0 / 3, 40.0 / 3, 100.0);

        // batch flag
        SimulationResult r = new ACSScheduler().run(cfg(3, 0, new int[][]{{0, 4, 2}, {1, 7, 3}, {3, 2, 1}}));
        check("ACS demotion flags: P1 batch, P2 batch, P3 interactive",
                byId(r, "P1").isBatch() && byId(r, "P2").isBatch() && !byId(r, "P3").isBatch());
    }

    // ------------------------------------------------------------------ 3. components

    static void componentTests() {
        AgingPolicy ag = new AgingPolicy(4);
        Process p1 = new Process("P1", 0, 5, 4); p1.setWaitStartTime(3);
        Process p2 = new Process("P2", 1, 6, 2); p2.setWaitStartTime(7);
        Process p3 = new Process("P3", 4, 4, 3); p3.setWaitStartTime(11);
        checkEq("Aging: P1 at T=11 (waited 8) -> priority 2", ag.effectivePriority(p1, 11), 2);
        checkEq("Aging: P2 at T=11 (waited 4) -> priority 1", ag.effectivePriority(p2, 11), 1);
        checkEq("Aging: P3 at T=11 (waited 0) -> priority 3", ag.effectivePriority(p3, 11), 3);
        checkEq("Aging: P1 at T=15 (waited 12) -> priority 1", ag.effectivePriority(p1, 15), 1);
        checkEq("Aging: P3 at T=15 (waited 4) -> priority 2", ag.effectivePriority(p3, 15), 2);
        checkEq("Aging: waited 3 units -> no boost yet", ag.effectivePriority(p1, 6), 4);
        Process old = new Process("P9", 0, 5, 2); old.setWaitStartTime(0);
        checkEq("Aging: never goes below the minimum priority (1)", ag.effectivePriority(old, 400), 1);

        // Tier2Queue order from the PDF's Test Case 2
        Tier2Queue q = new Tier2Queue(new AgingPolicy(4));
        Process a = new Process("P1", 0, 5, 4), b = new Process("P2", 1, 6, 2), c = new Process("P3", 4, 4, 3);
        DemotionPolicy.demote(a, 3, q);
        DemotionPolicy.demote(b, 7, q);
        DemotionPolicy.demote(c, 11, q);
        checkEq("Tier2Queue size after 3 demotions", q.size(), 3);
        checkEq("Tier2Queue.pickNext(11) = P2 (PDF)", q.pickNext(11).getId(), "P2");
        checkEq("Tier2Queue.pickNext(15) = P1 (PDF)", q.pickNext(15).getId(), "P1");
        checkEq("Tier2Queue.pickNext(18) = P3", q.pickNext(18).getId(), "P3");
        check("Tier2Queue empty after 3 picks", q.isEmpty() && q.pickNext(20) == null);

        // tie-break: same effective priority -> earlier waitStart first
        Tier2Queue q2 = new Tier2Queue(new AgingPolicy(4));
        Process x = new Process("P2", 0, 4, 2), y = new Process("P1", 0, 4, 2);
        DemotionPolicy.demote(x, 2, q2);
        DemotionPolicy.demote(y, 5, q2);
        checkEq("Tier2Queue tie-break: earlier wait start goes first", q2.pickNext(5).getId(), "P2");

        // Demotion rules
        Process d = new Process("D", 0, 6, 1);
        d.execute(3);
        check("DemotionPolicy: used full quantum and not finished -> demote", DemotionPolicy.shouldDemote(d, 3, 3));
        check("DemotionPolicy: used less than quantum -> no demote", !DemotionPolicy.shouldDemote(d, 2, 3));
        d.execute(3);
        check("DemotionPolicy: finished exactly at quantum end -> no demote", !DemotionPolicy.shouldDemote(d, 3, 3));
        Process e = new Process("E", 0, 6, 1);
        Tier2Queue q3 = new Tier2Queue(new AgingPolicy(4));
        DemotionPolicy.demote(e, 9, q3);
        check("DemotionPolicy.demote sets tier2 + batch + waitStart",
                e.getTier() == Tier.TIER2 && e.isBatch() && e.getWaitStartTime() == 9 && q3.size() == 1);

        // Tier1Queue is FIFO
        Tier1Queue t1 = new Tier1Queue();
        t1.enqueue(new Process("A", 0, 1, 1));
        t1.enqueue(new Process("B", 0, 1, 1));
        checkEq("Tier1Queue FIFO: first out is A", t1.dequeue().getId(), "A");
        checkEq("Tier1Queue size after one dequeue", t1.size(), 1);
        checkEq("Tier1Queue snapshotView shows B", t1.snapshotView().get(0).id, "B");

        // Priority 0 (note only, not counted)
        Tier2Queue q4 = new Tier2Queue(new AgingPolicy(4));
        Process z0 = new Process("Z0", 0, 4, 0), z1 = new Process("Z1", 0, 4, 1);
        DemotionPolicy.demote(z1, 4, q4);
        DemotionPolicy.demote(z0, 5, q4);
        if (!q4.pickNext(5).getId().equals("Z0"))
            notes.add("A process with initial priority 0 is treated as priority 1 by AgingPolicy (minimum is 1), "
                    + "so it does NOT beat a priority-1 process. Fix: make the input parser require priority >= 1.");
    }

    // ------------------------------------------------------------------ 4. metrics / io / edge

    static void metricsIoAndEdgeTests() throws Exception {
        // Metrics throws when the scheduler forgot start/finish
        boolean threw = false;
        try {
            List<Process> ps = new ArrayList<>();
            ps.add(new Process("P1", 0, 3, 1));
            MetricsCalculator.finalizeProcessTimes(ps);
        } catch (IllegalStateException ex) {
            threw = true;
        }
        check("MetricsCalculator: missing start/finish -> clear exception", threw);

        // Input parser
        Path dir = Files.createTempDirectory("m1tests");
        Path ok = dir.resolve("ok.txt");
        Files.write(ok, Arrays.asList("# comment", "", "P1 0 4 2", "P2 1 7 3"));
        List<Process> read = InputParser.readFromFile(ok.toString());
        check("InputParser: reads 2 processes, skips comments/blank lines",
                read.size() == 2 && read.get(1).getBurstTime() == 7 && read.get(1).getInitialPriority() == 3);
        expectThrows("InputParser: wrong number of values", () -> {
            Path f = dir.resolve("bad1.txt"); Files.write(f, Arrays.asList("P1 0 4"));
            InputParser.readFromFile(f.toString());
        });
        expectThrows("InputParser: letters instead of numbers", () -> {
            Path f = dir.resolve("bad2.txt"); Files.write(f, Arrays.asList("P1 0 four 2"));
            InputParser.readFromFile(f.toString());
        });
        expectThrows("InputParser: duplicate process names", () -> {
            Path f = dir.resolve("bad3.txt"); Files.write(f, Arrays.asList("P1 0 4 2", "P1 1 4 2"));
            InputParser.readFromFile(f.toString());
        });
        expectThrows("InputParser: burst = 0", () -> {
            Path f = dir.resolve("bad4.txt"); Files.write(f, Arrays.asList("P1 0 0 2"));
            InputParser.readFromFile(f.toString());
        });
        expectThrows("InputParser: negative arrival", () -> {
            Path f = dir.resolve("bad5.txt"); Files.write(f, Arrays.asList("P1 -1 3 2"));
            InputParser.readFromFile(f.toString());
        });
        expectThrows("InputParser: empty file", () -> {
            Path f = dir.resolve("bad6.txt"); Files.write(f, Arrays.asList("# nothing"));
            InputParser.readFromFile(f.toString());
        });

        // Output formatter
        SimulationResult acs = new ACSScheduler().run(cfg(3, 0, new int[][]{{0, 4, 2}, {1, 7, 3}, {3, 2, 1}}));
        String t = OutputFormatter.allToText(acs);
        check("Output (ACS): has Tier 1 / Tier 2 matrix lines", t.contains("Tier 1: [") && t.contains("Tier 2: ["));
        check("Output (ACS): prints average formulas like the PDF",
                t.contains("(9 + 12 + 5)/3 = 8.67") && t.contains("(5 + 5 + 3)/3 = 4.33"));
        check("Output (ACS): no NaN / Infinity", !t.contains("NaN") && !t.contains("Infinity"));
        SimulationResult sjf = new PreemptiveSJFScheduler().run(cfg(3, 0, new int[][]{{0, 6, 2}, {1, 4, 1}}));
        String ts = OutputFormatter.allToText(sjf);
        check("Output (SJF): uses 'Ready queue' and no Tier 2 line",
                ts.contains("Ready queue: [") && !ts.contains("Tier 2: ["));
        check("Output: comparison table lists both schedulers",
                OutputFormatter.comparisonToText(Arrays.asList(sjf, acs)).contains("Adaptive Container Scheduler"));

        // Scheduler edge cases
        SimulationResult one = new PreemptiveSJFScheduler().run(cfg(3, 2, new int[][]{{0, 1, 1}}));
        check("SJF: single process of 1 unit", ganttOf(one).equals("P1 0-1") && one.metrics.cpuUtilization == 100.0);
        SimulationResult bigQ = new ACSScheduler().run(cfg(1000, 0, new int[][]{{0, 5, 1}, {0, 3, 1}}));
        check("ACS: huge quantum behaves like FCFS (no demotion)",
                ganttOf(bigQ).equals("P1 0-5|P2 5-8") && !byId(bigQ, "P1").isBatch());
        SimulationResult q1 = new ACSScheduler().run(cfg(1, 0, new int[][]{{0, 3, 1}, {0, 3, 1}}));
        check("ACS: quantum = 1 still finishes everything", allFinished(q1));
        List<Process> many = new ArrayList<>();
        for (int i = 1; i <= 25; i++) many.add(new Process("P" + i, 0, 1 + i % 4, 1 + i % 3));
        SimulationResult big = new ACSScheduler().run(new SimulationConfig(many, 2, 1));
        invariants("ACS 25 simultaneous arrivals", big);
        SimulationResult big2 = new PreemptiveSJFScheduler().run(new SimulationConfig(many, 2, 1));
        invariants("SJF 25 simultaneous arrivals", big2);

        expectThrows("ACS: quantum 0 is rejected", () -> new ACSScheduler().run(cfg(0, 0, new int[][]{{0, 3, 1}})));

        SimulationResult emptySjf = new PreemptiveSJFScheduler().run(new SimulationConfig(new ArrayList<>(), 3, 0));
        SimulationResult emptyAcs = new ACSScheduler().run(new SimulationConfig(new ArrayList<>(), 3, 0));
        check("Empty process list does not crash (SJF and ACS)", emptySjf.metrics != null && emptyAcs.metrics != null);

        // scheduler object reuse must not keep old state
        ACSScheduler reused = new ACSScheduler();
        reused.run(cfg(2, 1, new int[][]{{0, 9, 1}, {1, 4, 2}}));
        SimulationConfig second = cfg(3, 0, new int[][]{{0, 4, 2}, {1, 7, 3}, {3, 2, 1}});
        check("ACS: same object used twice gives the same result as a fresh one",
                ganttOf(reused.run(second)).equals(ganttOf(new ACSScheduler().run(second))));
        PreemptiveSJFScheduler reusedS = new PreemptiveSJFScheduler();
        reusedS.run(cfg(2, 1, new int[][]{{0, 9, 1}, {1, 4, 2}}));
        check("SJF: same object used twice gives the same result as a fresh one",
                ganttOf(reusedS.run(second)).equals(ganttOf(new PreemptiveSJFScheduler().run(second))));
    }

    // ------------------------------------------------------------------ 5. random stress

    static int staleAfterCs = 0;
    static String staleExample = null;

    static void randomStress(int n, long seed) {
        Random rnd = new Random(seed);
        for (int i = 0; i < n; i++) {
            int np = 1 + rnd.nextInt(7);
            int[][] procs = new int[np][3];
            int maxArr = rnd.nextBoolean() ? 6 : 25;
            for (int k = 0; k < np; k++) {
                procs[k][0] = rnd.nextInt(maxArr);
                procs[k][1] = 1 + rnd.nextInt(9);
                procs[k][2] = 1 + rnd.nextInt(5);
            }
            int q = 1 + rnd.nextInt(6);
            int cs = rnd.nextInt(4);
            SimulationConfig c = cfg(q, cs, procs);
            String label = "case#" + i + " " + describe(c);

            for (int which = 0; which < 2; which++) {
                Scheduler s = which == 0 ? new PreemptiveSJFScheduler() : new ACSScheduler();
                String name = which == 0 ? "SJF" : "ACS";
                SimulationResult r;
                try {
                    r = s.run(c);
                } catch (Throwable ex) {
                    check(name + " " + label + " threw " + ex, false);
                    continue;
                }
                invariants(name + " " + label, r);
                // input config must stay untouched
                boolean untouched = true;
                for (Process p : c.processes)
                    if (p.getStartTime() != -1 || p.getFinishTime() != -1 || p.getRemainingTime() != p.getBurstTime()) untouched = false;
                check(name + " " + label + ": original input not modified", untouched);
                if (which == 0) sjfRule(name + " " + label, r);
                else acsRules(name + " " + label, r);
            }
        }
        if (staleAfterCs > 0)
            notes.add("SJF (CS > 0): in " + staleAfterCs + " random cases a SHORTER process arrived during a context switch, "
                    + "but the already-chosen process still ran its next time unit (the choice is not re-checked after the CS). "
                    + "Example: " + staleExample + ". The PDF does not say what should happen, so confirm the rule with your TA.");
    }

    // ------------------------------------------------------------------ generic invariants

    static void invariants(String label, SimulationResult r) {
        SimulationConfig c = r.config;
        List<Process> ps = r.processes;
        List<TimeSlice> g = r.gantt;

        check(label + ": every process is in the result", ps.size() == c.processes.size());

        // Gantt contiguous
        boolean contiguous = !g.isEmpty() && g.get(0).start == 0;
        for (int i = 0; i < g.size(); i++) {
            if (g.get(i).end <= g.get(i).start) contiguous = false;
            if (i > 0 && g.get(i).start != g.get(i - 1).end) contiguous = false;
        }
        if (ps.isEmpty()) contiguous = true;
        check(label + ": Gantt is contiguous from 0 (no gaps, no overlaps, no empty slices)", contiguous);

        // run time == burst, nothing before arrival, start/finish match gantt
        for (Process p : ps) {
            int run = 0, first = -1, last = -1;
            boolean beforeArrival = false;
            for (TimeSlice s : g) {
                if (s.type == SliceType.RUN && p.getId().equals(s.processId)) {
                    run += s.end - s.start;
                    if (first == -1) first = s.start;
                    last = s.end;
                    if (s.start < p.getArrivalTime()) beforeArrival = true;
                }
            }
            check(label + ": " + p.getId() + " runs exactly its burst time", run == p.getBurstTime());
            check(label + ": " + p.getId() + " never runs before it arrives", !beforeArrival);
            check(label + ": " + p.getId() + " startTime = first RUN start", p.getStartTime() == first);
            check(label + ": " + p.getId() + " finishTime = last RUN end", p.getFinishTime() == last);
            check(label + ": " + p.getId() + " turnaround = finish - arrival",
                    p.getTurnaroundTime() == p.getFinishTime() - p.getArrivalTime());
            check(label + ": " + p.getId() + " waiting = turnaround - burst",
                    p.getWaitingTime() == p.getTurnaroundTime() - p.getBurstTime());
            check(label + ": " + p.getId() + " response = start - arrival",
                    p.getResponseTime() == p.getStartTime() - p.getArrivalTime());
            check(label + ": " + p.getId() + " waiting/response are not negative",
                    p.getWaitingTime() >= 0 && p.getResponseTime() >= 0);
        }

        // metrics
        if (!ps.isEmpty() && r.metrics != null) {
            double w = 0, t = 0, rt = 0;
            for (Process p : ps) { w += p.getWaitingTime(); t += p.getTurnaroundTime(); rt += p.getResponseTime(); }
            int busy = 0, total = 0;
            for (TimeSlice s : g) { if (s.type == SliceType.RUN) busy += s.end - s.start; total = Math.max(total, s.end); }
            check(label + ": metrics averages match the per-process values",
                    near(r.metrics.avgWaiting, w / ps.size()) && near(r.metrics.avgTurnaround, t / ps.size())
                            && near(r.metrics.avgResponse, rt / ps.size()));
            check(label + ": CPU utilization = busy / total", near(r.metrics.cpuUtilization, 100.0 * busy / total));
            check(label + ": CPU utilization between 0 and 100", r.metrics.cpuUtilization > 0 && r.metrics.cpuUtilization <= 100.0001);
        }

        // context switches
        for (int i = 0; i < g.size(); i++) {
            TimeSlice s = g.get(i);
            if (s.type == SliceType.CONTEXT_SWITCH) {
                check(label + ": CS slice length = configured CS", c.contextSwitch > 0 && s.end - s.start == c.contextSwitch);
                boolean between = i > 0 && i < g.size() - 1 && g.get(i - 1).type == SliceType.RUN && g.get(i + 1).type == SliceType.RUN
                        && !g.get(i - 1).processId.equals(g.get(i + 1).processId);
                check(label + ": every CS is between two RUN slices of different processes", between);
            }
            if (i > 0 && c.contextSwitch > 0 && s.type == SliceType.RUN && g.get(i - 1).type == SliceType.RUN) {
                check(label + ": switching process without a context switch at t=" + s.start,
                        g.get(i - 1).processId.equals(s.processId));
            }
            if (s.type == SliceType.IDLE) {
                boolean arrivedUnfinished = false, someoneArrivesAtEnd = false;
                for (Process p : ps) {
                    if (p.getArrivalTime() <= s.start && p.getFinishTime() > s.start) arrivedUnfinished = true;
                    if (p.getArrivalTime() == s.end) someoneArrivesAtEnd = true;
                }
                check(label + ": CPU is idle only when nobody is waiting (t=" + s.start + ")", !arrivedUnfinished);
                check(label + ": idle ends exactly at the next arrival (t=" + s.end + ")", someoneArrivesAtEnd);
            }
        }

        // snapshots
        boolean ordered = true;
        for (int i = 1; i < r.snapshots.size(); i++)
            if (r.snapshots.get(i).time < r.snapshots.get(i - 1).time) ordered = false;
        check(label + ": snapshots exist and are in time order", (ps.isEmpty() || !r.snapshots.isEmpty()) && ordered);
        boolean snapRunningOk = true;
        for (TimeStepSnapshot sn : r.snapshots) {
            boolean found = false;
            for (TimeSlice s : g)
                if (s.type == SliceType.RUN && sn.runningProcessId != null && sn.runningProcessId.equals(s.processId)
                        && sn.time >= s.start && sn.time < s.end) found = true;
            if (!found) snapRunningOk = false;
        }
        check(label + ": each snapshot's running process is really running at that time", snapRunningOk);
    }

    /** SJF: at every time unit the running process has the smallest remaining time. */
    static void sjfRule(String label, SimulationResult r) {
        SimulationConfig c = r.config;
        Map<String, Integer> done = new HashMap<>();
        for (Process p : r.processes) done.put(p.getId(), 0);
        boolean strictOk = true;
        String firstBad = "";
        for (int i = 0; i < r.gantt.size(); i++) {
            TimeSlice s = r.gantt.get(i);
            if (s.type != SliceType.RUN) continue;
            boolean afterCs = i > 0 && r.gantt.get(i - 1).type == SliceType.CONTEXT_SWITCH;
            for (int t = s.start; t < s.end; t++) {
                Process run = byId(r, s.processId);
                int remRun = run.getBurstTime() - done.get(run.getId());
                boolean shorterExists = false;
                for (Process q : r.processes) {
                    if (q == run || q.getArrivalTime() > t) continue;
                    int remQ = q.getBurstTime() - done.get(q.getId());
                    if (remQ > 0 && remQ < remRun) shorterExists = true;
                }
                if (shorterExists) {
                    if (afterCs && t == s.start) {
                        staleAfterCs++;
                        if (staleExample == null) staleExample = label;
                    } else {
                        strictOk = false;
                        if (firstBad.isEmpty()) firstBad = " (first problem at t=" + t + ")";
                    }
                }
                done.put(run.getId(), done.get(run.getId()) + 1);
            }
        }
        check(label + ": always runs the shortest remaining time" + firstBad, strictOk);
    }

    /** ACS: Tier 1 structure, demotion flag, non-preemptive Tier 2, Tier 1 beats Tier 2. */
    static void acsRules(String label, SimulationResult r) {
        int q = r.config.quantum;
        for (Process p : r.processes) {
            List<TimeSlice> rs = runsOf(r.gantt, p.getId());
            boolean shouldBeBatch = p.getBurstTime() > q;
            check(label + ": " + p.getId() + " batch flag = (burst > Q)", p.isBatch() == shouldBeBatch);
            if (!shouldBeBatch) {
                check(label + ": " + p.getId() + " (fits in Q) runs in ONE slice", rs.size() == 1 && rs.get(0).end - rs.get(0).start == p.getBurstTime());
            } else {
                boolean shape;
                int tier2Start, decision;
                if (rs.size() == 1) {
                    shape = rs.get(0).end - rs.get(0).start == p.getBurstTime();
                    tier2Start = rs.get(0).start + q;
                    decision = tier2Start;
                } else {
                    shape = rs.size() == 2 && rs.get(0).end - rs.get(0).start == q
                            && rs.get(1).end - rs.get(1).start == p.getBurstTime() - q;
                    tier2Start = rs.size() > 1 ? rs.get(1).start : -1;
                    decision = tier2Start;
                    if (rs.size() > 1) {
                        int idx = r.gantt.indexOf(rs.get(1));
                        if (idx > 0 && r.gantt.get(idx - 1).type == SliceType.CONTEXT_SWITCH)
                            decision = r.gantt.get(idx - 1).start;
                    }
                }
                check(label + ": " + p.getId() + " = one Tier 1 quantum, then ONE non-preemptive Tier 2 burst", shape);
                if (shape) {
                    boolean tier1First = true;
                    for (Process other : r.processes) {
                        if (other == p) continue;
                        if (other.getArrivalTime() <= decision && other.getStartTime() >= tier2Start) tier1First = false;
                    }
                    check(label + ": Tier 2 burst of " + p.getId() + " starts only when no new job waits in Tier 1", tier1First);
                }
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    static SimulationConfig cfg(int q, int cs, int[][] procs) {
        List<Process> list = new ArrayList<>();
        for (int i = 0; i < procs.length; i++)
            list.add(new Process("P" + (i + 1), procs[i][0], procs[i][1], procs[i][2]));
        return new SimulationConfig(list, q, cs);
    }

    static String describe(SimulationConfig c) {
        StringBuilder sb = new StringBuilder("Q=" + c.quantum + " CS=" + c.contextSwitch + " [");
        for (Process p : c.processes)
            sb.append(p.getId()).append("(a=").append(p.getArrivalTime()).append(",b=").append(p.getBurstTime())
                    .append(",p=").append(p.getInitialPriority()).append(") ");
        return sb.toString().trim() + "]";
    }

    static void known(String name, Scheduler s, SimulationConfig c, String gantt, int[][] perProc,
                      double avgWT, double avgTAT, double util) {
        SimulationResult r;
        try {
            r = s.run(c);
        } catch (Throwable ex) {
            check(name + ": threw " + ex, false);
            return;
        }
        checkEq(name + ": Gantt chart", ganttOf(r), gantt);
        for (int i = 0; i < perProc.length; i++) {
            Process p = byId(r, "P" + (i + 1));
            boolean ok = p != null && p.getFinishTime() == perProc[i][0] && p.getTurnaroundTime() == perProc[i][1]
                    && p.getWaitingTime() == perProc[i][2] && p.getResponseTime() == perProc[i][3];
            String got = p == null ? "missing" : "finish=" + p.getFinishTime() + " TAT=" + p.getTurnaroundTime()
                    + " WT=" + p.getWaitingTime() + " RT=" + p.getResponseTime();
            checkEq(name + ": P" + (i + 1) + " (finish/turnaround/waiting/response)", got,
                    "finish=" + perProc[i][0] + " TAT=" + perProc[i][1] + " WT=" + perProc[i][2] + " RT=" + perProc[i][3]);
        }
        check(name + ": average waiting = " + fmt(avgWT) + " (got " + fmt(r.metrics.avgWaiting) + ")", near(r.metrics.avgWaiting, avgWT));
        check(name + ": average turnaround = " + fmt(avgTAT) + " (got " + fmt(r.metrics.avgTurnaround) + ")", near(r.metrics.avgTurnaround, avgTAT));
        if (!Double.isNaN(util))
            check(name + ": CPU utilization = " + fmt(util) + "% (got " + fmt(r.metrics.cpuUtilization) + "%)", near(r.metrics.cpuUtilization, util));
        invariants(name + " [invariants]", r);
    }

    static String ganttOf(SimulationResult r) {
        StringBuilder sb = new StringBuilder();
        for (TimeSlice s : r.gantt) {
            if (sb.length() > 0) sb.append('|');
            String n = s.type == SliceType.RUN ? s.processId : s.type == SliceType.CONTEXT_SWITCH ? "CS" : "IDLE";
            sb.append(n).append(' ').append(s.start).append('-').append(s.end);
        }
        return sb.toString();
    }

    static Process byId(SimulationResult r, String id) {
        for (Process p : r.processes) if (p.getId().equals(id)) return p;
        return null;
    }

    static List<TimeSlice> runsOf(List<TimeSlice> g, String id) {
        List<TimeSlice> out = new ArrayList<>();
        for (TimeSlice s : g) if (s.type == SliceType.RUN && id.equals(s.processId)) out.add(s);
        return out;
    }

    static boolean allFinished(SimulationResult r) {
        for (Process p : r.processes) if (p.getFinishTime() < 0 || p.getRemainingTime() != 0) return false;
        return true;
    }

    static boolean near(double a, double b) { return Math.abs(a - b) < 0.005; }
    static String fmt(double d) { return String.format(Locale.US, "%.2f", d); }

    interface Thrower { void run() throws Exception; }

    /** Runs a whole section; if it crashes, that is recorded as a FAIL instead of stopping the tests. */
    static void safely(String name, Thrower t) {
        try {
            t.run();
        } catch (Throwable ex) {
            StackTraceElement[] st = ex.getStackTrace();
            String where = st.length > 0 ? " at " + st[0] : "";
            record(false, name + " CRASHED with " + ex + where);
        }
    }

    static void expectThrows(String what, Thrower t) {
        boolean threw = false;
        try { t.run(); } catch (Exception ex) { threw = true; }
        check(what + " -> rejected with an error", threw);
    }

    static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    static void checkEq(String what, Object actual, Object expected) {
        boolean ok = Objects.equals(actual, expected);
        record(ok, ok ? what : what + "   expected: " + expected + "   actual: " + actual);
    }

    static void check(String what, boolean ok) { record(ok, what); }

    static void record(boolean ok, String message) {
        if (ok) {
            pass++;
            if (verbose) System.out.println("PASS  " + message);
        } else {
            fail++;
            failures.add(message);
            System.out.println("FAIL  " + message);
        }
    }
}
