package gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import model.ProcessView;
import model.TimeStepSnapshot;

public class SnapshotPanel extends JPanel {

    private List<TimeStepSnapshot> snapshots = new ArrayList<>();
    private int index = 0;

    private final JLabel header = new JLabel(" ");
    private final JTextArea body = new JTextArea();
    private final JLabel counter = new JLabel("0 / 0");
    private final JButton prev = new JButton("Prev");
    private final JButton next = new JButton("Next");

    public SnapshotPanel() {
        setLayout(new BorderLayout(8, 8));

        header.setFont(new Font("SansSerif", Font.BOLD, 16));
        body.setEditable(false);
        body.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        controls.add(prev);
        controls.add(counter);
        controls.add(next);

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(body), BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);

        prev.addActionListener(e -> go(index - 1));
        next.addActionListener(e -> go(index + 1));

        refresh();
    }

    public void setSnapshots(List<TimeStepSnapshot> list) {
        snapshots = (list == null) ? new ArrayList<>() : new ArrayList<>(list);
        index = 0;
        refresh();
    }

    private void go(int i) {
        if (i < 0 || i >= snapshots.size()) {
            return;
        }
        index = i;
        refresh();
    }

    private void refresh() {
        if (snapshots.isEmpty()) {
            header.setText(" No data");
            body.setText("");
            counter.setText("0 / 0");
            prev.setEnabled(false);
            next.setEnabled(false);
            return;
        }

        TimeStepSnapshot s = snapshots.get(index);
        header.setText(" Time: " + s.time + "    Running: "
                       + (s.runningProcessId == null ? "-" : s.runningProcessId));

        StringBuilder sb = new StringBuilder();
        sb.append("Tier 1: ").append(format(s.tier1)).append("\n");
        sb.append("Tier 2: ").append(format(s.tier2)).append("\n");
        if (s.notes != null && !s.notes.isEmpty()) {
            sb.append("\n");
            for (String n : s.notes) {
                sb.append(n).append("\n");
            }
        }
        body.setText(sb.toString());
        body.setCaretPosition(0);

        counter.setText((index + 1) + " / " + snapshots.size());
        prev.setEnabled(index > 0);
        next.setEnabled(index < snapshots.size() - 1);
    }

    private String format(List<ProcessView> list) {
        if (list == null || list.isEmpty()) {
            return "[Empty]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(list.get(i));
        }
        return sb.append("]").toString();
    }
}