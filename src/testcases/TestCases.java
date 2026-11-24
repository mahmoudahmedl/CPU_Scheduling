package testcases;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import model.Process;
import model.SimulationConfig;

public class TestCases {

    // Test Case 1 : Q = 3, CS = 0
    public static SimulationConfig testCase1() {
        List<Process> ps = new ArrayList<>(Arrays.asList(
            new Process("P1", 0, 4, 2),
            new Process("P2", 1, 7, 3),
            new Process("P3", 3, 2, 1)
        ));
        return new SimulationConfig(ps, 3, 0);
    }

    // Test Case 2 : Q = 3, CS = 1
    public static SimulationConfig testCase2() {
        List<Process> ps = new ArrayList<>(Arrays.asList(
            new Process("P1", 0, 5, 4),
            new Process("P2", 1, 6, 2),
            new Process("P3", 4, 4, 3)
        ));
        return new SimulationConfig(ps, 3, 1);
    }

    // Test Case 3: idle gaps (CPU is empty between processes), CS = 1
    public static SimulationConfig testCase3() {
        List<Process> ps = new ArrayList<>(Arrays.asList(
            new Process("P1", 0, 2, 1),
            new Process("P2", 5, 3, 2),
            new Process("P3", 6, 1, 3)
        ));
        return new SimulationConfig(ps, 2, 1);
    }

    // Test Case 4: ties (same arrival and same burst), CS = 0
    // checks the tie-break: earlier arrival, then lower id
    public static SimulationConfig testCase4() {
        List<Process> ps = new ArrayList<>(Arrays.asList(
            new Process("P1", 0, 3, 2),
            new Process("P2", 0, 3, 1),
            new Process("P3", 1, 3, 3)
        ));
        return new SimulationConfig(ps, 2, 0);
    }

    // Test Case 5: many arrivals with preemption, CS = 0
    public static SimulationConfig testCase5() {
        List<Process> ps = new ArrayList<>(Arrays.asList(
            new Process("P1", 0, 8, 3),
            new Process("P2", 1, 4, 1),
            new Process("P3", 2, 2, 2),
            new Process("P4", 3, 1, 4)
        ));
        return new SimulationConfig(ps, 4, 0);
    }

    // Test Case 6: big arrival gaps + larger CS (CS = 2)
    public static SimulationConfig testCase6() {
        List<Process> ps = new ArrayList<>(Arrays.asList(
            new Process("P1", 0, 3, 2),
            new Process("P2", 10, 6, 1),
            new Process("P3", 11, 2, 3),
            new Process("P4", 20, 4, 2)
        ));
        return new SimulationConfig(ps, 3, 2);
    }

    // All cases, used by TestRunner and by M5's experiments
    public static List<SimulationConfig> all() {
        List<SimulationConfig> list = new ArrayList<>();
        list.add(testCase1());
        list.add(testCase2());
        list.add(testCase3());
        list.add(testCase4());
        list.add(testCase5());
        list.add(testCase6());
        return list;
    }
}