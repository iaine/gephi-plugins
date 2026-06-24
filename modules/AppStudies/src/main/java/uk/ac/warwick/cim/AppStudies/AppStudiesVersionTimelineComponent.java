/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.ac.warwick.cim.AppStudies;

import java.awt.BorderLayout;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

@TopComponent.Description(
    preferredID = "VersionTimelineTopComponent",
    persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
    mode = "output",   // where it appears
    openAtStartup = true
)
@Messages({
    "CTL_VersionTimelineTopComponent=Version Timeline",
})
public class AppStudiesVersionTimelineTopComponent extends TopComponent {

    public VersionTimelineTopComponent() {
        setLayout(new BorderLayout());

        // ✅ Your custom panel goes here
        add(new VersionTimelinePanel(SharedVersionIndex.INSTANCE), BorderLayout.CENTER);

        setName(Bundle.CTL_VersionTimelineTopComponent());
    }
}
