package io;

import model.*;
import model.Process;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class InputParser {


    public static List<Process> readFromFile(String path) throws IOException {
        List<Process> list = new ArrayList<>();
        int lineNo = 0;
        for (String line : Files.readAllLines(Paths.get(path))) {
            lineNo++;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] t = line.split("\\s+");
            if (t.length != 4)
                throw new IllegalArgumentException("Line " + lineNo + ": expected 4 values (id arrival burst priority)");
            try {
                list.add(new Process(t[0], Integer.parseInt(t[1]), Integer.parseInt(t[2]), Integer.parseInt(t[3])));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Line " + lineNo + ": arrival, burst and priority must be integers");
            }
        }
        validateProcesses(list);
        return list;
    }

    public static SimulationConfig readFromConsole(Scanner sc) {
        int n = readInt(sc, "Number of processes: ", 1);
        List<Process> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            System.out.println("--- Process " + i + " ---");
            String id = readLine(sc, "Name (e.g. P" + i + "): ");
            int arrival = readInt(sc, "Arrival time: ", 0);
            int burst = readInt(sc, "Burst time: ", 1);
            int priority = readInt(sc, "Initial priority (lower number = higher priority): ", 0);
            list.add(new Process(id, arrival, burst, priority));
        }
        validateProcesses(list);
        int q = readInt(sc, "Time quantum (Q): ", 1);
        int cs = readInt(sc, "Context switch time (CS): ", 0);
        return new SimulationConfig(list, q, cs);
    }

    /** Checks: not empty, unique ids, arrival >= 0, burst > 0, priority >= 0. */
    public static void validateProcesses(List<Process> list) {
        if (list.isEmpty()) throw new IllegalArgumentException("No processes were given.");
        Set<String> ids = new HashSet<>();
        for (Process p : list) {
            if (p.getId().isEmpty()) throw new IllegalArgumentException("Process name cannot be empty.");
            if (!ids.add(p.getId())) throw new IllegalArgumentException("Duplicate process name: " + p.getId());
            if (p.getArrivalTime() < 0) throw new IllegalArgumentException(p.getId() + ": arrival must be >= 0");
            if (p.getBurstTime() <= 0) throw new IllegalArgumentException(p.getId() + ": burst must be > 0");
            if (p.getInitialPriority() < 0) throw new IllegalArgumentException(p.getId() + ": priority must be >= 0");
        }
    }

    // ---------- console helpers (public: Main uses them too) ----------

    public static String readLine(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("Please type something.");
        }
    }

    public static int readInt(Scanner sc, String prompt, int min) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v >= min) return v;
                System.out.println("Value must be >= " + min + ".");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }
}
