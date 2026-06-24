/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.ac.warwick.cim.AppStudies;

import javax.swing.*;
import java.awt.*;

public class AppStudiesVersionTimelinePanel extends JPanel {

    private final AppStudiesVersionIndex versionIndex;

    public AppStudiesVersionTimelinePanel(AppStudiesVersionIndex index) {
        this.versionIndex = index;
        setPreferredSize(new Dimension(600, 80));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = getWidth();
        int height = getHeight();

        Double[] values = versionIndex.getValues().toArray(new Double[0]);

        if (values.length == 0) return;

        double min = values[0];
        double max = values[values.length - 1];

        for (double value : values) {

            int x = (int) ((value - min) / (max - min) * width);

            // Draw tick
            g.drawLine(x, height / 2, x, height / 2 + 10);

            // Draw version label instead of number
            String label = versionIndex.getLabel(value);
            g.drawString(label, x - 10, height / 2 + 25);
        }
    }
}
