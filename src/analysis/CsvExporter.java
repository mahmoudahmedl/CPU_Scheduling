package analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

public class CsvExporter {

    private static final String HEADER =
            "quantum,contextSwitch,avgWaiting,avgTurnaround,avgResponse,cpuUtilization";

    public static void export(List<ExperimentPoint> pts, String path) {
        if (pts == null) {
            throw new IllegalArgumentException("pts cannot be null");
        }
        Path file = Paths.get(path);
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);   // create results/ if missing
            }
            try (BufferedWriter w = Files.newBufferedWriter(file)) {
                w.write(HEADER);
                w.newLine();
                for (ExperimentPoint p : pts) {
                    w.write(toCsvRow(p));
                    w.newLine();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write CSV file: " + path, e);
        }
    }

    private static String toCsvRow(ExperimentPoint p) {
        return String.format(Locale.US, "%d,%d,%.4f,%.4f,%.4f,%.4f",
                p.quantum, p.contextSwitch,
                p.avgWaiting, p.avgTurnaround, p.avgResponse, p.cpuUtilization);
    }
}