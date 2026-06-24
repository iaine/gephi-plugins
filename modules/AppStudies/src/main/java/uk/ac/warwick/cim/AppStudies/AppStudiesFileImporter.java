/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.ac.warwick.cim.AppStudies;

import java.io.LineNumberReader;
import java.io.Reader;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.ImportUtils;
import org.gephi.io.importer.api.NodeDraft;
import org.gephi.io.importer.api.Report;
import org.gephi.io.importer.spi.FileImporter;
import org.json.*;

/**
 *
 * @author iain
 */
public class AppStudiesFileImporter implements FileImporter {

    private Reader reader;
    private ContainerLoader container;
    private Report report;
    

    @Override
    public void setReader(Reader reader) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean execute(ContainerLoader cl) {

        this.report = new Report();
        LineNumberReader lineReader = ImportUtils.getTextReader(reader);
        try {
            //Set container as undirected
            container.setEdgeDefault(EdgeDirectionDefault.DIRECTED);

            //Import
            importData(lineReader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }
    
    private void importData(LineNumberReader reader) throws Exception {
        String line = reader.readLine();
        
        ContainerLoader containerLoader = new ContainerLoader();
        
        // Create columns
        containerLoader.getNodeTable().addColumn("start", Double.class);
        containerLoader.getNodeTable().addColumn("end", Double.class);
        containerLoader.getNodeTable().addColumn("version", String.class);
        
        
        while ((line = reader.readLine()) != null) {
            /**
             * Major @todo: fix the different imports if they exist? 
             */
            JSONObject jsonObj = new JSONObject(line);
            
            
            double t = AppStudiesVersionMapper.toDouble(ext.version);
            
            //check nodes using data
            int node1Index = (Integer.valueOf(str[0].trim()));
            int node2Index = (Integer.valueOf(str[1].trim()));
            
            //let's create a Node now
            NodeDraft node1;
            if (container.nodeExists(String.valueOf(node1Index))) {
                node1 = container.getNode(String.valueOf(node1Index));
            } else {
                node1 = container.factory().newNodeDraft(String.valueOf(node1Index));
                node1.setValue("start", t);
                node1.setValue("end", t);
                node1.setValue("version", t);
                //Don't forget to add the node
                container.addNode(node1);
            }

            NodeDraft node2;
            if (container.nodeExists(String.valueOf(node2Index))) {
                node2 = container.getNode(String.valueOf(node2Index));
            } else {
                node2 = container.factory().newNodeDraft(String.valueOf(node2Index));
                node2.setValue("start", t);
                node2.setValue("end", t);
                node2.setValue("version", t);
                //Don't forget to add the node
                container.addNode(node2);
            }
            
            //Create edge
            EdgeDraft edgeDraft = container.factory().newEdgeDraft();
            edgeDraft.setSource(node1);
            edgeDraft.setTarget(node2);
            //ignore weight for now
            //edgeDraft.setWeight(weight);
            container.addEdge(edgeDraft);
        }
        
        
        
        
        
    }

    @Override
    public ContainerLoader getContainer() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Report getReport() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}
