package testcases;

import java.util.List;
import java.util.Map;

import model.Process;
import model.Scheduler;
import model.SimulationConfig;
import model.SimulationResult;
import sjf.PreemptiveSJFScheduler;

public class TestRunner {
    public static boolean verify(SimulationResult r, int caseNo) {
        Map<String, int[]> expected = ExpectedResults.expectedFor(caseNo);
        boolean ok = true;

        for (Process p : r.processes) {
            int[] exp = expected.get(p.getId());   
            if (exp == null) {
                System.out.println("  " + p.getId() + ": no expected values");
                ok = false;
                continue;
            }
            boolean same = p.getFinishTime() == exp[0]
                        && p.getTurnaroundTime() == exp[1]
                        && p.getWaitingTime() == exp[2];
            if (!same) {
                ok = false;
                System.out.println("  " + p.getId()
                    + " expected (finish, turnaround, waiting) = ("
                    + exp[0] + ", " + exp[1] + ", " + exp[2] + ")"
                    + " but got ("
                    + p.getFinishTime() + ", " + p.getTurnaroundTime() + ", "
                    + p.getWaitingTime() + ")");
            }
        }

        System.out.println("Case " + caseNo + " [" + r.schedulerName + "]: "
                           + (ok ? "PASS" : "FAIL"));
        return ok;
    }

    public static void main(String[] args) {
        Scheduler sjf = new PreemptiveSJFScheduler();

        List<SimulationConfig> cases = TestCases.all();
        int passed = 0;

        for (int i = 0; i < cases.size(); i++) {
            int caseNo = i + 1;
            SimulationResult r = sjf.run(cases.get(i));
            if (verify(r, caseNo)) {
                passed++;
            }
        }

        System.out.println();
        System.out.println(passed + " / " + cases.size() + " passed");
    }
}