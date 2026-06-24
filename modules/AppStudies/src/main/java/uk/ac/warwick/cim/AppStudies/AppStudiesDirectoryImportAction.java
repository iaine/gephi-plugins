package uk.ac.warwick.cim.AppStudies;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.TimeRepresentation;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ContainerFactory;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.AppendProcessor;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.io.processor.spi.Processor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.json.JSONObject;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.WindowManager;

/**
 * Step 5 - directory import.
 *
 * Lets the user pick a directory, collects every .json / .jsonl file in it,
 * sorts them by app version (via {@link AppStudiesVersionMapper}) and loads them
 * ALL into a single dynamic graph in one workspace. Because each element is
 * stamped with a version interval, the result is one graph that the Gephi
 * Timeline can play across versions.
 *
 * Registered under File menu. Single-module plugins can register actions like
 * this with the NetBeans @ActionRegistration annotations.
 *
 * @author iain
 */
@ActionID(category = "File", id = "uk.ac.warwick.cim.AppStudies.AppStudiesDirectoryImportAction")
@ActionRegistration(displayName = "#CTL_AppStudiesDirectoryImport")
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1550)
})
@Messages("CTL_AppStudiesDirectoryImport=Import AppStudies Directory...")
public final class AppStudiesDirectoryImportAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select a directory of AppStudies JSON files");

        int result = chooser.showOpenDialog(WindowManager.getDefault().getMainWindow());
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File dir = chooser.getSelectedFile();

        // Toggle: if a graph already exists, ask whether to append to it or start
        // a fresh one. With no existing workspace there is nothing to append to,
        // so we go straight to a new graph.
        boolean append = false;
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        if (pc.getCurrentProject() != null && pc.getCurrentWorkspace() != null) {
            NotifyDescriptor d = new NotifyDescriptor.Confirmation(
                    "Append these files to the current graph?\n\n"
                    + "Yes  - merge into the existing workspace (keeps current nodes)\n"
                    + "No   - create a new workspace for this import",
                    "AppStudies Directory Import",
                    NotifyDescriptor.YES_NO_OPTION);
            Object answer = DialogDisplayer.getDefault().notify(d);
            append = (answer == NotifyDescriptor.YES_OPTION);
        }

        importDirectory(dir, append);
    }

    /**
     * Backwards-compatible entry point: imports into a fresh workspace.
     */
    public static void importDirectory(File dir) {
        importDirectory(dir, false);
    }

    /**
     * Core logic, separated from the Swing event so it can be unit-tested or
     * called from a configurable path elsewhere.
     *
     * @param dir    directory of .json / .jsonl files
     * @param append true to merge into the current workspace (AppendProcessor),
     *               false to create a new workspace (DefaultProcessor)
     */
    public static void importDirectory(File dir, boolean append) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }

        // 1. Collect candidate files
        File[] found = dir.listFiles((d, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".json") || lower.endsWith(".jsonl");
        });
        if (found == null || found.length == 0) {
            return;
        }

        // 2. Sort by version so the dynamic graph builds in chronological order.
        //    We peek each file's version cheaply; files that can't be read sort last.
        List<File> files = new ArrayList<>();
        for (File f : found) {
            files.add(f);
        }
        files.sort(Comparator.comparingDouble(AppStudiesDirectoryImportAction::peekVersionValue));

        // 3. Choose workspace: append to current, or create a fresh one.
        //    Only clear the version legend when starting fresh.
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        if (pc.getCurrentProject() == null) {
            pc.newProject();
        }

        Workspace workspace;
        if (append && pc.getCurrentWorkspace() != null) {
            workspace = pc.getCurrentWorkspace();
        } else {
            SharedVersionIndex.INSTANCE.clear();
            workspace = pc.newWorkspace(pc.getCurrentProject());
            pc.openWorkspace(workspace);
        }

        ContainerFactory factory = Lookup.getDefault().lookup(ContainerFactory.class);
        Container container = factory.newContainer();
        ContainerLoader loader = container.getLoader();
        loader.setEdgeDefault(EdgeDirectionDefault.DIRECTED);
        loader.setTimeRepresentation(TimeRepresentation.INTERVAL);

        AppStudiesGraphBuilder.declareColumns(loader);

        // 4. Parse every file into the same container
        for (File f : files) {
            try {
                String content = readAll(f).trim();
                if (content.isEmpty()) {
                    continue;
                }
                if (content.startsWith("{") && content.endsWith("}") && isSingleObject(content)) {
                    AppStudiesGraphBuilder.addDocument(loader, new JSONObject(content), container.getReport());
                } else {
                    for (String line : content.split("\\R")) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            AppStudiesGraphBuilder.addDocument(loader, new JSONObject(trimmed), container.getReport());
                        }
                    }
                }
            } catch (Exception ex) {
                // Skip a bad file but keep going with the rest.
                container.getReport().logIssue(new org.gephi.io.importer.api.Issue(
                        "AppStudies: failed to read " + f.getName() + ": " + ex.getMessage(),
                        org.gephi.io.importer.api.Issue.Level.WARNING));
            }
        }

        // 5. Process the single container into the workspace.
        //    DefaultProcessor = replace into a clean workspace;
        //    AppendProcessor  = merge into the existing graph.
        container.closeLoader();
        ImportController importController = Lookup.getDefault().lookup(ImportController.class);
        Processor processor = append ? new AppendProcessor() : new DefaultProcessor();
        importController.process(container, processor, workspace);

        // 6. Turn the version axis into a live Timeline
        AppStudiesTimelineActivator.enableForCurrentWorkspace();
    }

    // --- helpers ----------------------------------------------------------

    /** Cheaply read a file's version for sorting; returns +inf if unreadable. */
    private static double peekVersionValue(File f) {
        try {
            String content = readAll(f).trim();
            if (content.isEmpty()) {
                return Double.POSITIVE_INFINITY;
            }
            // Parse the whole thing as one object, or fall back to the first
            // JSONL line — either way we just need the top-level version field.
            JSONObject root;
            if (content.startsWith("{") && isSingleObject(content)) {
                root = new JSONObject(content);
            } else {
                root = new JSONObject(content.split("\\R", 2)[0].trim());
            }
            JSONObject app = root.optJSONObject("app");
            String v = (app != null) ? app.optString("version", null) : null;
            if (v == null || v.isEmpty()) {
                v = root.optString("toolkit_version", "0");
            }
            return AppStudiesVersionMapper.toDouble(v);
        } catch (Exception ex) {
            return Double.POSITIVE_INFINITY;
        }
    }

    private static boolean isSingleObject(String content) {
        try {
            new JSONObject(content);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static String readAll(File f) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (Reader r = new FileReader(f)) {
            char[] buf = new char[8192];
            int n;
            while ((n = r.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
        }
        return sb.toString();
    }
}
