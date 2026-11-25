package gui;

import io.OutputFormatter;
import model.*;
import util.SchedulerFactory;

import javax.swing.*;
import java.awt.*;

/** Main GUI window. Member 1 owns this file. */
public class SimulatorFrame extends JFrame {
    private final InputPanel inputPanel = new InputPanel();
    private final GanttPanel ganttPanel = new GanttPanel();          // Member 2
    private final SnapshotPanel snapshotPanel = new SnapshotPanel(); // Member 2
    private final ResultsPanel resultsPanel = new ResultsPanel();    // Member 1
    private final JTextArea textArea = new JTextArea();
    private final JComboBox<String> schedulerBox =
            new JComboBox<>(new String[]{"Preemptive SJF", "Adaptive Container Scheduler (ACS)"});

    public SimulatorFrame() {
        super("CPU Scheduling Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(6, 6));

        JButton run = new JButton("Run");
        run.addActionListener(e -> runSimulation());
        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.add(inputPanel, BorderLayout.CENTER);
        JPanel runBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        runBar.add(new JLabel("Scheduler:"));
        runBar.add(schedulerBox);
        runBar.add(run);
        top.add(runBar, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Gantt", new JScrollPane(ganttPanel));
        tabs.addTab("Steps (tier matrix)", snapshotPanel);
        tabs.addTab("Results", resultsPanel);
        tabs.addTab("Text output", new JScrollPane(textArea));
        add(tabs, BorderLayout.CENTER);

        setSize(900, 650);
        setLocationRelativeTo(null);
    }

    private void runSimulation() {
        try {
            SimulationConfig cfg = inputPanel.buildConfig();
            String key = schedulerBox.getSelectedIndex() == 0 ? SchedulerFactory.SJF : SchedulerFactory.ACS;
            Scheduler s = SchedulerFactory.create(key);
            showResult(s.run(cfg));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Sends the result to all tabs. */
    public void showResult(SimulationResult r) {
        ganttPanel.setSlices(r.gantt);
        snapshotPanel.setSJFMode(r.schedulerName != null && r.schedulerName.toUpperCase().contains("SJF"));
        snapshotPanel.setSnapshots(r.snapshots);
        resultsPanel.setResult(r);
        textArea.setText(OutputFormatter.allToText(r));
        textArea.setCaretPosition(0);
    }
}
