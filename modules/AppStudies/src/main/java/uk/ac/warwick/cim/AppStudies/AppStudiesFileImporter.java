package uk.ac.warwick.cim.AppStudies;

import java.io.BufferedReader;
import java.io.Reader;
import org.gephi.graph.api.TimeRepresentation;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.Report;
import org.gephi.io.importer.spi.FileImporter;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.ProgressTicket;
import org.json.JSONObject;

/**
 * Imports a single AppStudies .json or .jsonl file into a Gephi container.
 *
 * Time handling (the bit that makes the Timeline work):
 *  - the container's TimeRepresentation is set to INTERVAL;
 *  - every node/edge is given an existence interval [version, +inf);
 *  - version is a double produced by {@link AppStudiesVersionMapper}.
 *
 * Because Gephi opens one Reader per file, this importer handles ONE file. To
 * load a whole directory across versions, see the directory importer (which
 * reuses {@link AppStudiesGraphBuilder}).
 *
 * @author iain
 */
public class AppStudiesFileImporter implements FileImporter, LongTask {

    private Reader reader;
    private ContainerLoader container;
    private Report report;
    private ProgressTicket progressTicket;
    private boolean cancelled = false;

    @Override
    public void setReader(Reader reader) {
        this.reader = reader;
    }

    @Override
    public boolean execute(ContainerLoader loader) {
        this.container = loader;
        this.report = new Report();

        // Configure the container for a DIRECTED, dynamic (interval) graph.
        container.setEdgeDefault(EdgeDirectionDefault.DIRECTED);
        container.setTimeRepresentation(TimeRepresentation.INTERVAL);

        AppStudiesGraphBuilder.declareColumns(container);

        try (BufferedReader br = new BufferedReader(reader)) {
            // Read the whole stream so we can support BOTH:
            //  - a single pretty-printed JSON object spanning many lines, and
            //  - JSONL: one compact JSON object per line.
            StringBuilder all = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if (cancelled) {
                    return false;
                }
                all.append(line).append('\n');
            }

            String content = all.toString().trim();
            if (content.isEmpty()) {
                report.logIssue(new org.gephi.io.importer.api.Issue(
                        "AppStudies: empty file",
                        org.gephi.io.importer.api.Issue.Level.WARNING));
                return true;
            }

            if (looksLikeSingleObject(content)) {
                AppStudiesGraphBuilder.addDocument(container, new JSONObject(content), report);
            } else {
                // JSONL: parse line by line
                for (String l : content.split("\\R")) {
                    if (cancelled) {
                        return false;
                    }
                    String trimmed = l.trim();
                    if (!trimmed.isEmpty()) {
                        AppStudiesGraphBuilder.addDocument(container, new JSONObject(trimmed), report);
                    }
                }
            }
        } catch (Exception e) {
            report.logIssue(new org.gephi.io.importer.api.Issue(
                    "AppStudies import failed: " + e.getMessage(),
                    org.gephi.io.importer.api.Issue.Level.SEVERE));
            return false;
        }

        return !cancelled;
    }

    /**
     * A single JSON object that happens to contain newlines (pretty-printed)
     * versus genuine JSONL. Heuristic: if the trimmed content starts with '{' and
     * ends with '}' it is one object; JSONL files have multiple top-level objects
     * so the last char of the first line is typically '}' too, but the whole file
     * does not parse as one object. We try single-object first and fall back.
     */
    private boolean looksLikeSingleObject(String content) {
        if (!(content.startsWith("{") && content.endsWith("}"))) {
            return false;
        }
        try {
            new JSONObject(content);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    @Override
    public Report getReport() {
        return report;
    }

    @Override
    public boolean cancel() {
        cancelled = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progressTicket = progressTicket;
    }
}
