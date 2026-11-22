package gui;

import model.*;
import model.Process;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** PLACEHOLDER written by Member 1 so the project compiles. Member 2 owns this file and will replace it. */
public class GanttPanel extends JPanel {
    private static final int UNIT = 32, X0 = 20, Y0 = 30, H = 50;
    private List<TimeSlice> slices = new ArrayList<>();

    public void setSlices(List<TimeSlice> s) {
        slices = s == null ? new ArrayList<>() : s;
        int end = 0;
        for (TimeSlice t : slices) end = Math.max(end, t.end);
        setPreferredSize(new Dimension(X0 * 2 + end * UNIT, 120));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (TimeSlice s : slices) {
            int x = X0 + s.start * UNIT, w = (s.end - s.start) * UNIT;
            Color c = s.type == SliceType.CONTEXT_SWITCH ? Color.GRAY
                    : s.type == SliceType.IDLE ? Color.WHITE
                    : Color.getHSBColor((Math.abs(String.valueOf(s.processId).hashCode()) % 100) / 100f, 0.35f, 0.95f);
            g.setColor(c);
            g.fillRect(x, Y0, w, H);
            g.setColor(Color.BLACK);
            g.drawRect(x, Y0, w, H);
            String label = s.type == SliceType.RUN ? s.processId : s.type == SliceType.CONTEXT_SWITCH ? "CS" : "idle";
            g.drawString(label, x + 4, Y0 + H / 2 + 4);
            g.drawString(String.valueOf(s.start), x - 3, Y0 + H + 16);
            g.drawString(String.valueOf(s.end), x + w - 3, Y0 + H + 16);
        }
    }
}
