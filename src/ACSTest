
import acs.ACSScheduler;
import model.Process;
import model.SimulationConfig;
import model.SimulationResult;
import model.TimeSlice;
import model.SliceType;

import java.util.Arrays;

public class ACSTest {

    public static void main(String[] args) {

        // Test Case 1
        SimulationConfig config1 = new SimulationConfig(
            Arrays.asList(
                new Process("P1", 0, 4, 2),
                new Process("P2", 1, 7, 3),
                new Process("P3", 3, 2, 1)
            ),
            3, 0
        );

        test("Test Case 1", config1);

        // Test Case 2
        SimulationConfig config2 = new SimulationConfig(
            Arrays.asList(
                new Process("P1", 0, 5, 4),
                new Process("P2", 1, 6, 2),
                new Process("P3", 4, 4, 3)
            ),
            3, 1
        );

        test("Test Case 2", config2);
    }

    static void test(String name, SimulationConfig config) {

        ACSScheduler scheduler = new ACSScheduler();
        SimulationResult result = scheduler.run(config);

        System.out.println("\n===== " + name + " =====");

        System.out.println("Gantt Chart:");

        for (TimeSlice slice : result.gantt) {

            String processName =
                slice.type == SliceType.RUN
                ? slice.processId
                : slice.type.toString();

            System.out.println(
                processName + " [" +
                slice.start + " - " +
                slice.end + "]"
            );
        }

        System.out.println("\nProcess Metrics:");

        for (Process p : result.processes) {
            System.out.println(
                p.getId() +
                " | WT = " + p.getWaitingTime() +
                " | TAT = " + p.getTurnaroundTime() +
                " | RT = " + p.getResponseTime() +
                " | Remaining = " + p.getRemainingTime()
            );
        }

        System.out.println("\nAverage Waiting Time: "
            + result.metrics.avgWaiting);

        System.out.println("Average Turnaround Time: "
            + result.metrics.avgTurnaround);

        System.out.println("Average Response Time: "
            + result.metrics.avgResponse);
    }
}
