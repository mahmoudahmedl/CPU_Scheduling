import io.InputParser;
import io.OutputFormatter;
import gui.SimulatorFrame;
import model.*;
import model.Process;
import util.SchedulerFactory;

import javax.swing.SwingUtilities;
import java.util.*;

/** Console menu. Member 1 owns this file. */
public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println();
            System.out.println("=== CPU Scheduling Simulator ===");
            System.out.println("1) Preemptive SJF");
            System.out.println("2) Adaptive Container Scheduler (ACS)");
            System.out.println("3) Run both and compare");
            System.out.println("4) Open GUI");
            System.out.println("0) Exit");
            System.out.print("Choice: ");
            String c = sc.nextLine().trim();
            try {
                switch (c) {
                    case "1": runOne(sc, SchedulerFactory.SJF); break;
                    case "2": runOne(sc, SchedulerFactory.ACS); break;
                    case "3": runBoth(sc); break;
                    case "4":
                        SwingUtilities.invokeLater(() -> new SimulatorFrame().setVisible(true));
                        System.out.println("GUI opened. (Close the window to end the program.)");
                        break;
                    case "0": return;
                    default: System.out.println("Invalid choice.");
                }
            } catch (java.io.IOException e) {
                System.out.println("Error: cannot read file (" + e.getMessage() + ")");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void runOne(Scanner sc, String key) throws Exception {
        SimulationConfig cfg = askConfig(sc);
        Scheduler s = SchedulerFactory.create(key);
        OutputFormatter.printAll(s.run(cfg));
    }

    private static void runBoth(Scanner sc) throws Exception {
        SimulationConfig cfg = askConfig(sc);
        List<SimulationResult> results = new ArrayList<>();
        for (String key : new String[]{SchedulerFactory.SJF, SchedulerFactory.ACS}) {
            SimulationResult r = SchedulerFactory.create(key).run(cfg);
            OutputFormatter.printAll(r);
            results.add(r);
        }
        System.out.println("==== Comparison ====");
        System.out.println(OutputFormatter.comparisonToText(results));
    }

    /** Asks: file or manual input, then Q and CS (for files). */
    private static SimulationConfig askConfig(Scanner sc) throws Exception {
        System.out.print("Read processes from a file? (y/n): ");
        String a = sc.nextLine().trim().toLowerCase();
        if (a.startsWith("y")) {
            String path = InputParser.readLine(sc, "File path: ");
            List<Process> list = InputParser.readFromFile(path);
            int q = InputParser.readInt(sc, "Time quantum (Q): ", 1);
            int cs = InputParser.readInt(sc, "Context switch time (CS): ", 0);
            return new SimulationConfig(list, q, cs);
        }
        return InputParser.readFromConsole(sc);
    }
}
