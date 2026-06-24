/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.ac.warwick.cim.AppStudies;

import javax.swing.JPanel;
import org.gephi.io.importer.spi.ImporterUI;
/**
 *
 * @author iain
 */
public class AppStudiesVersionTimelineUIController implements ImporterUI {
    
    private AppStudiesVersionTimelineImporterUI panel;
    private AppStudiesVersionTimelineImporter importer;

    @Override
    public void setup(Importer importer) {
        this.importer = (AppStudiesVersionTimelineImporter) importer;

        if (panel == null) {
            panel = new AppStudiesVersionTimelineImporterUI();
        }
    }

    @Override
    public void unsetup(boolean update) {
        if (update) {
            importer.setInputPath(panel.getPath());
        }
        panel = null;
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    public String getDisplayName() {
        return "Version Timeline Import Settings";
    }

    @Override
    public boolean isUIForImporter(Importer importer) {
        return importer instanceof AppStudiesVersionTimelineImporter;
    }
}
