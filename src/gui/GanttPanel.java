package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import model.SliceType;
import model.TimeSlice;

public class GanttPanel extends JPanel {

    private final Canvas canvas = new Canvas();

    public GanttPanel() {
        setLayout(new BorderLayout());
        add(new JScrollPane(canvas), BorderLayout.CENTER);
    }

    public void setSlices(List<TimeSlice> slices) {
        canvas.setSlices(slices);
    }

    private static class Canvas extends JPanel {

        private static final int UNIT = 36;
        private static final int LEFT = 20;
        private static final int TOP = 30;
        private static final int BAR_H = 50;

        private static final Color[] PALETTE = {
    new Color(0x1E88E5),  
    new Color(0x8E24AA),   
    new Color(0xFDD835),  
    new Color(0xE53935),  
    new Color(0x43A047),   
    new Color(0xFB8C00),   
    new Color(0x00ACC1),   
    new Color(0xEC407A),  
    new Color(0x6D4C41)    
};

        private List<TimeSlice> slices = new ArrayList<>();
        private final Map<String, Color> colors = new HashMap<>();

        Canvas() {
            setBackground(Color.WHITE);
        }

        void setSlices(List<TimeSlice> s) {
            slices = (s == null) ? new ArrayList<>() : new ArrayList<>(s);
            colors.clear();
            for (TimeSlice t : slices) {
                if (t.type == SliceType.RUN && !colors.containsKey(t.processId)) {
                    colors.put(t.processId, PALETTE[colors.size() % PALETTE.length]);
                }
            }
            int total = slices.isEmpty() ? 0 : slices.get(slices.size() - 1).end;
            setPreferredSize(new Dimension(LEFT * 2 + total * UNIT, TOP + BAR_H + 60));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();

            for (TimeSlice t : slices) {
                int x = LEFT + t.start * UNIT;
                int w = (t.end - t.start) * UNIT;

                Color fill;
                String label;
                if (t.type == SliceType.RUN) {
                    fill = colors.get(t.processId);
                    label = t.processId;
                } else if (t.type == SliceType.CONTEXT_SWITCH) {
                    fill = new Color(0xBBBBBB);
                    label = "CS";
                } else {
                    fill = new Color(0xEEEEEE);
                    label = "IDLE";
                }

                g2.setColor(fill);
                g2.fillRect(x, TOP, w, BAR_H);
                g2.setColor(Color.DARK_GRAY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(x, TOP, w, BAR_H);

                g2.setColor(t.type == SliceType.RUN ? Color.WHITE : Color.BLACK);
                int tx = x + (w - fm.stringWidth(label)) / 2;
                int ty = TOP + (BAR_H + fm.getAscent()) / 2 - 2;
                if (fm.stringWidth(label) <= w) {
                    g2.drawString(label, tx, ty);
                }

                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(t.start), x - fm.stringWidth(String.valueOf(t.start)) / 2,
                              TOP + BAR_H + 18);
            }

            if (!slices.isEmpty()) {
                int end = slices.get(slices.size() - 1).end;
                int x = LEFT + end * UNIT;
                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(end), x - fm.stringWidth(String.valueOf(end)) / 2,
                              TOP + BAR_H + 18);
            }
        }
    }
}