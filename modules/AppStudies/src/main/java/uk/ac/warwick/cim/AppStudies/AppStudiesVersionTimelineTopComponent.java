package uk.ac.warwick.cim.AppStudies;

import java.awt.BorderLayout;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

/**
 * Step 6 - dockable window hosting {@link AppStudiesVersionTimelinePanel}.
 *
 * The class name, file name and constructor name now all agree (the original had
 * three different names, so it would not compile). The panel reads the shared
 * version index, so opening this window after an import shows the version ticks.
 *
 * @author iain
 */
@TopComponent.Description(
        preferredID = "AppStudiesVersionTimelineTopComponent",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Window", id = "uk.ac.warwick.cim.AppStudies.AppStudiesVersionTimelineTopComponent")
@ActionReference(path = "Menu/Window", position = 545)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_AppStudiesVersionTimelineAction",
        preferredID = "AppStudiesVersionTimelineTopComponent"
)
@Messages({
    "CTL_AppStudiesVersionTimelineAction=AppStudies Version Timeline",
    "CTL_AppStudiesVersionTimelineTopComponent=Version Timeline"
})
public final class AppStudiesVersionTimelineTopComponent extends TopComponent {

    private final AppStudiesVersionTimelinePanel panel;

    public AppStudiesVersionTimelineTopComponent() {
        setLayout(new BorderLayout());
        setName(Bundle.CTL_AppStudiesVersionTimelineTopComponent());

        panel = new AppStudiesVersionTimelinePanel(SharedVersionIndex.INSTANCE.get());
        add(panel, BorderLayout.CENTER);
    }

    @Override
    protected void componentShowing() {
        super.componentShowing();
        // Pick up versions from the latest import each time it is shown.
        panel.refresh();
    }
}
