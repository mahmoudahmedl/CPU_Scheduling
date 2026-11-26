package analysis;

import acs.ACSScheduler;
import model.Scheduler;
import model.SimulationConfig;
import testcases.TestCases;

public class RunnerCheck {
    public static void main(String[] args) {
        SimulationConfig base = TestCases.testCase2();
        Scheduler acs = new ACSScheduler();

        for (ExperimentPoint p : ExperimentRunner.sweepQuantum(acs, base, new int[]{2, 3, 4})) {
            System.out.println(p);
        }
        System.out.println("base Q=" + base.quantum + ", CS=" + base.contextSwitch);
    }
}