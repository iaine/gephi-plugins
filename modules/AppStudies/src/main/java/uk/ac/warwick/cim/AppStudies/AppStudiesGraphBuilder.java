package uk.ac.warwick.cim.AppStudies;

import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.NodeDraft;
import org.gephi.io.importer.api.Report;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Turns a single parsed AppStudies JSON document into draft nodes and edges in a
 * {@link ContainerLoader}, tagging every element with a time interval derived
 * from the app version so the Gephi Timeline can play the graph across versions.
 *
 * Two schemas are handled:
 *
 *  1. "flows" files, which carry a pre-built graph in {@code graph.nodes} /
 *     {@code graph.links}.
 *
 *  2. "listening" (and similar) files, which have no node/edge arrays. For these
 *     the graph is DERIVED from the {@code chain} array: every entry links a
 *     {@code module} to a processing {@code stage} (capture -&gt; dsp -&gt; features
 *     -&gt; inference -&gt; output), producing a module/stage bipartite-ish pipeline
 *     graph. This is the most faithful structure the data supports.
 *
 * The same method can be called once per file by the directory importer, all
 * writing into one shared container, so versions accumulate into one dynamic
 * graph.
 *
 * @author iain
 */
public final class AppStudiesGraphBuilder {

    private AppStudiesGraphBuilder() {
    }

    /**
     * Declare the (dynamic) columns this builder writes. Call ONCE per container,
     * before the first {@link #addDocument} call.
     *
     * The boolean 'true' on addNodeColumn makes the column dynamic, but here we
     * keep the descriptive columns static and use addInterval() on the drafts for
     * existence-over-time, which is what actually drives the Timeline.
     */
    public static void declareColumns(ContainerLoader container) {
        container.addNodeColumn("kind", String.class);     // "module" | "stage" | "node"
        container.addNodeColumn("vendor", String.class);
        container.addNodeColumn("version", String.class);  // human-readable, e.g. "43.7.3"
        container.addEdgeColumn("relation", String.class); // "module->stage" | "link"
    }

    /**
     * Parse one document into the container.
     *
     * @param container the shared container being filled
     * @param root      the parsed JSON object for one file
     * @param report    import report for warnings (may be null)
     */
    public static void addDocument(ContainerLoader container, JSONObject root, Report report) {
        String version = resolveVersion(root);
        // Record for the custom timeline panel's human-readable labels.
        SharedVersionIndex.INSTANCE.add(version);
        double t = AppStudiesVersionMapper.toDouble(version);
        // Element exists from this version onward. Double.POSITIVE_INFINITY means
        // "and forever after"; if you'd rather an element only live at the exact
        // version it appeared, use addInterval(t, t) instead.
        double start = t;
        double end = Double.POSITIVE_INFINITY;

        boolean built = false;

        // ---- Schema 1: pre-built graph (flows files) ----
        JSONObject graph = root.optJSONObject("graph");
        if (graph != null) {
            JSONArray nodes = graph.optJSONArray("nodes");
            JSONArray links = optLinks(graph);
            if (nodes != null && nodes.length() > 0) {
                buildFromExplicitGraph(container, nodes, links, version, start, end);
                built = true;
            }
        }

        // ---- Schema 2: derive graph from the chain (listening files) ----
        if (!built) {
            JSONArray chain = root.optJSONArray("chain");
            if (chain != null && chain.length() > 0) {
                buildFromChain(container, chain, version, start, end);
                built = true;
            }
        }

        if (!built && report != null) {
            report.logIssue(new org.gephi.io.importer.api.Issue(
                    "AppStudies: no graph or chain data found for version " + version,
                    org.gephi.io.importer.api.Issue.Level.WARNING));
        }
    }

    // -- Schema 1 ----------------------------------------------------------

    private static void buildFromExplicitGraph(ContainerLoader container,
                                               JSONArray nodes, JSONArray links,
                                               String version, double start, double end) {
        for (int i = 0; i < nodes.length(); i++) {
            JSONObject n = nodes.optJSONObject(i);
            if (n == null) {
                continue;
            }
            // id may be "id" or "name" depending on toolkit output
            String id = n.optString("id", n.optString("name", String.valueOf(i)));
            NodeDraft node = getOrCreateNode(container, id, start, end);
            node.setValue("kind", "node");
            node.setValue("version", version);
            if (n.has("label")) {
                node.setLabel(n.optString("label", id));
            }
        }

        if (links != null) {
            for (int i = 0; i < links.length(); i++) {
                JSONObject l = links.optJSONObject(i);
                if (l == null) {
                    continue;
                }
                String src = l.optString("source", null);
                String tgt = l.optString("target", null);
                if (src == null || tgt == null) {
                    continue;
                }
                NodeDraft s = getOrCreateNode(container, src, start, end);
                NodeDraft d = getOrCreateNode(container, tgt, start, end);
                addEdge(container, s, d, "link", start, end);
            }
        }
    }

    // -- Schema 2 ----------------------------------------------------------

    private static void buildFromChain(ContainerLoader container, JSONArray chain,
                                       String version, double start, double end) {
        for (int i = 0; i < chain.length(); i++) {
            JSONObject c = chain.optJSONObject(i);
            if (c == null) {
                continue;
            }
            String module = c.optString("module", null);
            String stage = c.optString("stage", null);
            if (module == null || stage == null) {
                continue;
            }
            String vendor = c.optString("vendor", "unknown");

            // Stage node (id namespaced so it cannot collide with a module path)
            String stageId = "stage:" + stage;
            NodeDraft stageNode = getOrCreateNode(container, stageId, start, end);
            stageNode.setLabel(stage);
            stageNode.setValue("kind", "stage");
            stageNode.setValue("version", version);

            // Module node
            String moduleId = "module:" + module;
            NodeDraft moduleNode = getOrCreateNode(container, moduleId, start, end);
            moduleNode.setLabel(shortName(module));
            moduleNode.setValue("kind", "module");
            moduleNode.setValue("vendor", vendor);
            moduleNode.setValue("version", version);

            addEdge(container, moduleNode, stageNode, "module->stage", start, end);
        }
    }

    // -- helpers -----------------------------------------------------------

    private static NodeDraft getOrCreateNode(ContainerLoader container, String id,
                                             double start, double end) {
        NodeDraft node;
        if (container.nodeExists(id)) {
            node = container.getNode(id);
        } else {
            node = container.factory().newNodeDraft(id);
            container.addNode(node);
        }
        // addInterval is idempotent enough for our use: re-adding the same span is
        // harmless, and a node seen in several files gets the union of its spans.
        node.addInterval(start, end);
        return node;
    }

    private static void addEdge(ContainerLoader container, NodeDraft s, NodeDraft d,
                                String relation, double start, double end) {
        // Reuse an existing edge between the same endpoints if present
        if (container.edgeExists(s.getId(), d.getId())) {
            EdgeDraft existing = container.getEdge(s.getId(), d.getId());
            existing.addInterval(start, end);
            return;
        }
        EdgeDraft edge = container.factory().newEdgeDraft();
        edge.setSource(s);
        edge.setTarget(d);
        edge.setValue("relation", relation);
        edge.addInterval(start, end);
        container.addEdge(edge);
    }

    /** Accept either "links" or "edges" as the array name. */
    private static JSONArray optLinks(JSONObject graph) {
        JSONArray a = graph.optJSONArray("links");
        return a != null ? a : graph.optJSONArray("edges");
    }

    /** Prefer app.version, fall back to toolkit_version, then "0". */
    private static String resolveVersion(JSONObject root) {
        JSONObject app = root.optJSONObject("app");
        if (app != null) {
            String v = app.optString("version", null);
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return root.optString("toolkit_version", "0");
    }

    /** "lib/arm64-v8a/libfoo.so" -> "libfoo.so" for readable labels. */
    private static String shortName(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}
