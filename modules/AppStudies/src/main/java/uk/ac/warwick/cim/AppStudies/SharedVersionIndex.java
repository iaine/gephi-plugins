package uk.ac.warwick.cim.AppStudies;

/**
 * Process-wide singleton holding the set of versions seen during the most recent
 * import. The importer / directory action populate it; the custom timeline panel
 * ({@link AppStudiesVersionTimelinePanel}) reads it to draw human-readable
 * version labels (e.g. "43.7.3") instead of the raw mapped doubles.
 *
 * This is deliberately simple shared state rather than a Lookup service because
 * it is purely a UI convenience and has no behaviour to mock or swap.
 *
 * @author iain
 */
public enum SharedVersionIndex {

    INSTANCE;

    private final AppStudiesVersionIndex index = new AppStudiesVersionIndex();

    public AppStudiesVersionIndex get() {
        return index;
    }

    /** Record a version string (no-op if null/empty). */
    public void add(String version) {
        if (version != null && !version.isEmpty()) {
            index.add(version);
        }
    }

    /** Clear before a fresh import so stale versions are not shown. */
    public void clear() {
        index.clear();
    }
}
