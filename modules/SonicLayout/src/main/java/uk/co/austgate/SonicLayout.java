/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.austgate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.LineUnavailableException;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.Edge;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutProperty;
import org.openide.util.Exceptions;
import org.gephi.statistics.plugin.Modularity;

import uk.co.austgate.ToneGenerator;
import uk.co.austgate.ToneAlgorithms;


/**
 * Implements a simple sonic layout. 
 * 
 * @author iain
 */
public class SonicLayout implements Layout {

    //Architecture
    private final LayoutBuilder builder;
    private GraphModel graphModel;
    //Flags
    private boolean executing = false;
    //Properties
    private boolean sonifyNodes = false;
    private boolean sonifyModularity = false;
    private String sonifyEdgePath = "/Users/";
    private String sonifySounds = "Enter the source : sound ; ";
    
    private HashMap<String, String> setTones = new HashMap<String, String>();
    
    private ToneGenerator toneGenerator;
    
    private ToneAlgorithms toneAlgorithms;
    
    private PlayWav playWav = new PlayWav();

    public SonicLayout(SonicLayoutBuilder builder) {
        this.builder = (LayoutBuilder) builder;
    }

    @Override
    public void resetPropertiesValues() {
        sonifyNodes = false;
        sonifyModularity = false;
        sonifySounds = "modularity.ck";
        sonifyEdgePath = "edge.ck";
        setTones.clear();
    }

    @Override
    public void initAlgo() {
        executing = true;
    }

    @Override
    public void goAlgo() {
        toneGenerator = new ToneGenerator();
        toneAlgorithms = new ToneAlgorithms();
        Graph graph = graphModel.getGraphVisible();
        graph.readLock();
        
        setSonificationSounds();
        
        if (sonifyNodes) {
            Node[] nodes = graph.getNodes().toArray();

            for (int i = 0; i < nodes.length; i++) {
                Node node = nodes[i];
                if (sonifyModularity) {
                    //let's get the modularity score here.
                    Modularity mod = new Modularity();
                    double modularityScore = mod.getModularity();
                    System.out.print(modularityScore);
                    Integer modularity = (Integer) node.getAttribute(Modularity.MODULARITY_CLASS);
                    System.out.println(modularity);
                    try {
                    Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        System.out.println(" interrupt " + ie.toString());
                    }
                }
            }
            executing=false;
        } else {
            Edge[] edges = graph.getEdges().toArray();
            
            //track the internal state. If unpause and not renew, carry on. 
            int track_i=0;
            if (executing) {

            for (int i = track_i; i < edges.length; i++) {
                    track_i = i;
                    Edge edge = edges[i];

                    try {
                        this.sonifyData(64.25, edge);
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {

                    }
                    /*PlayChuck playChuck = new PlayChuck();
                    if (!setTones.isEmpty()) {
                        String sound = setTones.get(edge.getSource().getLabel());

                        switch (sound.split(".")[1]) {
                            case "ck":
                                playChuck.playSound();
                                break;
                            case "wav":
                                playWav.playSound(sonifyEdgePath + "/" + sound);
                                break;
                            default:
                                System.out.println("Unsupported format " + sound);

                        }
                    }*/
                }
            }//executing
        }

        graph.readUnlock();
        executing = false;
    }
    
    /**
     * sonifyData.
     * 
     * This function calls the sonification code. 
     * 
     * The function takes the basic tone and the current edge from the graph. 
     * It finds the distance and the number of blocks to create additional tones
     * if required. 
     * 
     * It runs in a separate thread or Gephi locks while it completes. 
     * @param freq
     * @param edge 
     */
    private void sonifyData (Double freq, Edge edge) {

        Executors.newSingleThreadExecutor().execute(new Runnable() {

            @Override 
            public void run() {
                 try {

                    Integer dur = findAttribute("dist", edge);
                    Integer blocks = findAttribute("num_blocks", edge);
                    if (blocks > 0) {
                        Double cent = toneAlgorithms.createCent(freq, blocks);
                        toneGenerator.generateTone(dur + cent, blocks, dur);
                    } else {
                        toneGenerator.generateTone(freq, dur);
                    }

                    
                } catch (InterruptedException | LineUnavailableException ex) {
                    Exceptions.printStackTrace(ex);
                }
                
            }
        });
    }
    
    /**
     * Function to find an integer attribute in the graph
     * 
     * @param attr
     * @param edge
     * @return 
     */
    private Integer findAttribute(String attr, Edge edge) {
        return (Integer) edge.getAttribute(attr);
    }

    @Override
    public void endAlgo() {
        executing = false;
    }

    @Override
    public boolean canAlgo() {
        return executing;
    }

    @Override
    public LayoutProperty[] getProperties() {
        List<LayoutProperty> properties = new ArrayList<LayoutProperty>();
        final String GRIDLAYOUT = "Sonic Layout";

        try {
            properties.add(LayoutProperty.createProperty(
                    this, boolean.class,
                    "Sonify Nodes",
                    GRIDLAYOUT,
                    "If selected, sonify nodes or edges",
                    "getSonify", "setSonify"));
            properties.add(LayoutProperty.createProperty(
                    this, boolean.class,
                    "Sonify Modularity",
                    GRIDLAYOUT,
                    "If selected, sonify modularity",
                    "getModularity", "setModularity"));
            properties.add(LayoutProperty.createProperty(
                    this, String.class,
                    "Set the path for sounds",
                    GRIDLAYOUT,
                    "Enter the directory where sounds are",
                    "getSoundPath", "setSoundPath"));  
            properties.add(LayoutProperty.createProperty(
                    this, HashMap.class,
                    "Sound Lay",
                    GRIDLAYOUT,
                    "Enter the Sounds in name:sound form. Must be a ChucK or Wav file.",
                    "getSound", "setSoundMap"));             
        } catch (Exception e) {
            e.printStackTrace();
        }

        return properties.toArray(new LayoutProperty[0]);
    }

    @Override
    public LayoutBuilder getBuilder() {
        return builder;
    }

    @Override
    public void setGraphModel(GraphModel gm) {
        this.graphModel = gm;
    }
    
    private HashMap<String, String> setSonificationSounds () {
        setTones.clear();
        if (sonifySounds.contains(";")) {
            String[] patches = sonifySounds.split(";");
            for (String patch: patches) {
                String[] ps = patch.split(":");
                setTones.put(ps[0], ps[1]);
            }
        }
        return setTones;
    }
    
    /* Properties for the Interface */
    public void setSoundPath (String sonifyEdgePath) {
        this.sonifyEdgePath = sonifyEdgePath;
    }
    
    public String getSoundPath () {
        return sonifyEdgePath;
    }
    
    public void setSound (String sonifySounds) {
        this.sonifySounds = sonifySounds;      
    }
 
    public void setSoundMap (HashMap<String, String> sonifySound) {
        //@todo check this method
        this.setTones.putAll(sonifySound);     
    }
    public String getSound () {
        return sonifySounds;
    }

    public void setModularity (boolean sonifyModularity) {
        this.sonifyModularity = sonifyModularity;
    }
    
    public boolean getModularity () {
        return sonifyModularity;
    }
    
    public void setSonify (boolean sonifyNodes) {
        this.sonifyNodes = sonifyNodes;
    }
    
    public boolean getSonify () {
        return sonifyNodes;
    }
}