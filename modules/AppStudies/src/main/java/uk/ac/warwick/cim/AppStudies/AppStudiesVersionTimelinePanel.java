package uk.ac.warwick.cim.AppStudies;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * Step 6 - custom version timeline strip.
 *
 * Draws one tick per imported version along a horizontal axis, labelled with the
 * human-readable version string (e.g. "43.7.3") rather than the raw mapped
 * double the Gephi engine actually uses. It reads from {@link AppStudiesVersionIndex}
 * which the importer populates via {@link SharedVersionIndex}.
 *
 * This complements Gephi's built-in Timeline (which does the actual graph
 * filtering); this strip is a readable legend of where the versions sit.
 *
 * @author iain
 */
public class AppStudiesVersionTimelinePanel extends JPanel {

    private final AppStudiesVersionIndex versionIndex;

    public AppStudiesVersionTimelinePanel(AppStudiesVersionIndex index) {
        this.versionIndex = index;
        setPreferredSize(new Dimension(600, 80));
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int axisY = height / 2;

        if (versionIndex == null || versionIndex.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.drawString("No versions imported yet", 12, axisY);
            return;
        }

        Double[] values = versionIndex.getValues().toArray(new Double[0]);

        double min = values[0];
        double max = values[values.length - 1];
        double span = (max - min);

        // Baseline
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawLine(20, axisY, width - 20, axisY);

        FontMetrics fm = g2.getFontMetrics(getFont().deriveFont(Font.PLAIN, 11f));

        for (double value : values) {
            // Map value to x, leaving 20px margins; handle the single-version case.
            int x;
            if (span == 0) {
                x = width / 2;
            } else {
                x = 20 + (int) ((value - min) / span * (width - 40));
            }

            // Tick
            g2.setColor(new Color(0x33, 0x66, 0x99));
            g2.fillOval(x - 3, axisY - 3, 6, 6);
            g2.drawLine(x, axisY, x, axisY + 8);

            // Human-readable label, centred under the tick
            String label = versionIndex.getLabel(value);
            int labelWidth = fm.stringWidth(label);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(label, x - labelWidth / 2, axisY + 22);
        }
    }

    /** Call after an import so the strip repaints with the new versions. */
    public void refresh() {
        revalidate();
        repaint();
    }
}
