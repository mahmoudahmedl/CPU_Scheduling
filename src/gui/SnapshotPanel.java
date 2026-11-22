package gui;

import io.OutputFormatter;
import model.*;
import model.Process;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** PLACEHOLDER written by Member 1 so the project compiles. Member 2 owns this file and will replace it. */
public class SnapshotPanel extends JPanel {
    private List<TimeStepSnapshot> snaps = new ArrayList<>();
    private boolean sjf = false;
    private int index = 0;
    private final JTextArea area = new JTextArea();
    private final JLabel label = new JLabel(" ");

    public SnapshotPanel() {
        setLayout(new BorderLayout(6, 6));
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        add(new JScrollPane(area), BorderLayout.CENTER);
        JButton prev = new JButton("< Prev");
        JButton next = new JButton("Next >");
        prev.addActionListener(e -> show(index - 1));
        next.addActionListener(e -> show(index + 1));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bar.add(prev);
        bar.add(label);
        bar.add(next);
        add(bar, BorderLayout.SOUTH);
    }

    public void setSnapshots(List<TimeStepSnapshot> s) {
        snaps = s == null ? new ArrayList<>() : s;
        show(0);
    }

    public void setSJFMode(boolean sjf) { this.sjf = sjf; }

    private void show(int i) {
        if (snaps.isEmpty()) { area.setText(""); label.setText("No steps"); return; }
        index = Math.max(0, Math.min(i, snaps.size() - 1));
        area.setText(OutputFormatter.snapshotToText(snaps.get(index), sjf));
        label.setText("Step " + (index + 1) + " / " + snaps.size());
    }
}
