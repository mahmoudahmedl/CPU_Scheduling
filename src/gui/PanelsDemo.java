package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import model.SimulationResult;
import sjf.PreemptiveSJFScheduler;
import testcases.TestCases;

public class PanelsDemo {

    public static void main(String[] args) {
        SimulationResult r = new PreemptiveSJFScheduler().run(TestCases.testCase2());

        SwingUtilities.invokeLater(() -> {
            GanttPanel gantt = new GanttPanel();
            SnapshotPanel snaps = new SnapshotPanel();
            gantt.setSlices(r.gantt);
            snaps.setSnapshots(r.snapshots);

            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, gantt, snaps);
            split.setResizeWeight(0.4);

            JFrame f = new JFrame("Panels demo - " + r.schedulerName);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setLayout(new BorderLayout());
            f.add(split, BorderLayout.CENTER);
            f.setPreferredSize(new Dimension(800, 520));
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}