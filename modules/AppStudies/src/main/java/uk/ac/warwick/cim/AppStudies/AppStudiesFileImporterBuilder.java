/*
 * Class to load the JSON/JSONL files that come from the AppStudies toolkit. 
 */
package uk.ac.warwick.cim.AppStudies;

import org.gephi.io.importer.api.FileType;
import org.gephi.io.importer.spi.FileImporter;
import org.gephi.io.importer.spi.FileImporterBuilder;
import org.openide.filesystems.FileObject;

/**
 *
 * @author iain
 */
public class AppStudiesFileImporterBuilder implements FileImporterBuilder{
    
    public static final String IDENTIFIER1 = "json";
    public static final String IDENTIFIER2 = "jsonl";
    @Override
    public FileImporter buildImporter() {
        return new AppStudiesFileImporter();
    }

    @Override
    public FileType[] getFileTypes() {
        FileType ft = new FileType(".json", "AppStudies json");
        return new FileType[]{ft};
    }

    @Override
    public boolean isMatchingImporter(FileObject fileObject) {
        return fileObject.getExt().equalsIgnoreCase(IDENTIFIER1) || fileObject.getExt().equalsIgnoreCase(IDENTIFIER2);
    }

    @Override
    public String getName() {
        return IDENTIFIER1 + IDENTIFIER2;
    }
    
}
