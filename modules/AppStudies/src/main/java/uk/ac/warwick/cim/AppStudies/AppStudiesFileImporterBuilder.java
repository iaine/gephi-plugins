package uk.ac.warwick.cim.AppStudies;

import org.gephi.io.importer.api.FileType;
import org.gephi.io.importer.spi.FileImporter;
import org.gephi.io.importer.spi.FileImporterBuilder;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 * Builder that registers the AppStudies importer with Gephi and tells it which
 * file extensions to offer it for.
 *
 * The {@link ServiceProvider} annotation is essential: without it Gephi never
 * discovers the importer and it will not appear in File &gt; Import.
 *
 * @author iain
 */
@ServiceProvider(service = FileImporterBuilder.class)
public class AppStudiesFileImporterBuilder implements FileImporterBuilder {

    public static final String IDENTIFIER = "appstudies-json";

    private static final String EXT_JSON = "json";
    private static final String EXT_JSONL = "jsonl";

    @Override
    public FileImporter buildImporter() {
        return new AppStudiesFileImporter();
    }

    @Override
    public FileType[] getFileTypes() {
        return new FileType[]{
            new FileType(".json", "AppStudies JSON"),
            new FileType(".jsonl", "AppStudies JSONL")
        };
    }

    @Override
    public boolean isMatchingImporter(FileObject fileObject) {
        String ext = fileObject.getExt();
        return EXT_JSON.equalsIgnoreCase(ext) || EXT_JSONL.equalsIgnoreCase(ext);
    }

    @Override
    public String getName() {
        return IDENTIFIER;
    }
}
