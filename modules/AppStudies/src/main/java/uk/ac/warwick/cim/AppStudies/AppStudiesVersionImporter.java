/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.ac.warwick.cim.AppStudies;

/**
 *
 * @author iain
 */
public class AppStudiesVersionTimelineImporter implements Importer {

    private ContainerLoader container;

    // ✅ Add configurable path
    private String inputPath;

    public void setInputPath(String path) {
        this.inputPath = path;
    }

    @Override
    public void execute(ContainerLoader container) {
        this.container = container;

        System.out.println("Loading graph from: " + inputPath);

        // 🔁 Use your real loader here
        ExternalGraphSource source = ExternalGraphSource.loadFromPath(inputPath);

        // (same logic as before...)
    }

    @Override
    public boolean cancel() {
        return true;
    }
}
``