package gui;

import model.*;
import model.Process;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Locale;

/** Process table + averages + CPU utilization. Member 1 owns this file. */
public class ResultsPanel extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Process", "Arrival", "Burst", "Finish", "Turnaround", "Waiting", "Response"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JLabel summary = new JLabel(" ");

    public ResultsPanel() {
        setLayout(new BorderLayout(6, 6));
        add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        summary.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(summary, BorderLayout.SOUTH);
    }

    public void setResult(SimulationResult r) {
        model.setRowCount(0);
        for (Process p : r.processes) {
            model.addRow(new Object[]{p.getId(), p.getArrivalTime(), p.getBurstTime(), p.getFinishTime(),
                    p.getTurnaroundTime(), p.getWaitingTime(), p.getResponseTime()});
        }
        Metrics m = r.metrics;
        summary.setText(String.format(Locale.US,
                "<html><b>%s</b><br>Average Waiting Time: %.2f &nbsp;|&nbsp; Average Turnaround Time: %.2f "
                        + "&nbsp;|&nbsp; Average Response Time: %.2f &nbsp;|&nbsp; CPU Utilization: %.2f%%</html>",
                r.schedulerName, m.avgWaiting, m.avgTurnaround, m.avgResponse, m.cpuUtilization));
    }
}
