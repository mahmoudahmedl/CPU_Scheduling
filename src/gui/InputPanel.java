package gui;

import io.InputParser;
import model.*;
import model.Process;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Process table + Q + CS fields. Member 1 owns this file. */
public class InputPanel extends JPanel {
    private final DefaultTableModel model =
            new DefaultTableModel(new Object[]{"Process", "Arrival", "Burst", "Priority"}, 0);
    private final JTable table = new JTable(model);
    private final JSpinner quantum = new JSpinner(new SpinnerNumberModel(3, 1, 1000, 1));
    private final JSpinner contextSwitch = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));

    public InputPanel() {
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createTitledBorder("Input"));

        // default rows = Test Case 1 from the assignment
        model.addRow(new Object[]{"P1", 0, 4, 2});
        model.addRow(new Object[]{"P2", 1, 7, 3});
        model.addRow(new Object[]{"P3", 3, 2, 1});

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(420, 120));
        add(sp, BorderLayout.CENTER);

        JButton add = new JButton("Add row");
        add.addActionListener(e -> model.addRow(new Object[]{"P" + (model.getRowCount() + 1), 0, 1, 1}));
        JButton remove = new JButton("Remove row");
        remove.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) model.removeRow(r);
        });
        JButton load = new JButton("Load file...");
        load.addActionListener(e -> loadFile());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        buttons.add(add);
        buttons.add(remove);
        buttons.add(load);
        buttons.add(new JLabel("  Quantum (Q):"));
        buttons.add(quantum);
        buttons.add(new JLabel("Context switch (CS):"));
        buttons.add(contextSwitch);
        add(buttons, BorderLayout.SOUTH);
    }

    /** Reads the table. Throws IllegalArgumentException with a clear message when something is wrong. */
    public SimulationConfig buildConfig() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        List<Process> list = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            String id = String.valueOf(model.getValueAt(i, 0)).trim();
            try {
                int arrival = Integer.parseInt(String.valueOf(model.getValueAt(i, 1)).trim());
                int burst = Integer.parseInt(String.valueOf(model.getValueAt(i, 2)).trim());
                int priority = Integer.parseInt(String.valueOf(model.getValueAt(i, 3)).trim());
                list.add(new Process(id, arrival, burst, priority));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Row " + (i + 1) + ": arrival, burst and priority must be whole numbers.");
            }
        }
        InputParser.validateProcesses(list);
        return new SimulationConfig(list, (Integer) quantum.getValue(), (Integer) contextSwitch.getValue());
    }

    private void loadFile() {
        JFileChooser fc = new JFileChooser(new File("data"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            List<Process> list = InputParser.readFromFile(fc.getSelectedFile().getPath());
            model.setRowCount(0);
            for (Process p : list)
                model.addRow(new Object[]{p.getId(), p.getArrivalTime(), p.getBurstTime(), p.getInitialPriority()});
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot load file", JOptionPane.ERROR_MESSAGE);
        }
    }
}
