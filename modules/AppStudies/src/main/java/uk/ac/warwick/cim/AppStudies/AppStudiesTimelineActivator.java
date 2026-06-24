package uk.ac.warwick.cim.AppStudies;

import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.TimeFormat;
import org.gephi.timeline.api.TimelineController;
import org.openide.util.Lookup;

/**
 * Enables Gephi's built-in Timeline for the current workspace and configures it
 * for the version axis.
 *
 * The importer sets each element's existence interval, but the Timeline panel is
 * not shown until {@link TimelineController#setEnabled(boolean)} is called. Run
 * this AFTER the container has been processed into the workspace (i.e. after the
 * import finishes), e.g. from a menu action or a workspace listener.
 *
 * @author iain
 */
public final class AppStudiesTimelineActivator {

    private AppStudiesTimelineActivator() {
    }

    public static void enableForCurrentWorkspace() {
        GraphController gc = Lookup.getDefault().lookup(GraphController.class);
        GraphModel model = gc.getGraphModel();
        if (model == null) {
            return;
        }

        // Versions were mapped to doubles, so display the axis as a number.
        model.setTimeFormat(TimeFormat.DOUBLE);

        TimelineController timeline = Lookup.getDefault().lookup(TimelineController.class);
        if (timeline != null) {
            timeline.setEnabled(true);
            // Optionally pin the visible bounds to the data's min/max so the
            // playhead starts framed on the version range:
            // double min = model.getTimeBounds().getLow();
            // double max = model.getTimeBounds().getHigh();
            // timeline.setCustomBounds(min, max);
        }
    }
}
