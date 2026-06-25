/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.ac.warwick.cim.sonic;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import org.gephi.preview.api.Item;
import org.gephi.preview.spi.ItemBuilder;
import org.gephi.preview.plugin.items.NodeItem;
import org.openide.util.lookup.ServiceProvider;

/**
 * Implementation of Sonic Bridge from Indian Sound Cultures
 * 
 * @author iain
 */
@ServiceProvider(service=ItemBuilder.class)
public class sonicBridge implements ItemBuilder {
    
    
    private static final Logger logger = Logger.getLogger(sonicBridge.class.getName());
    

    @Override
    public Item[] getItems(Graph graph) {
        
        java.util.List<AudioItem> built = new java.util.ArrayList<>();

        try {
            for (Node n : graph.getNodes()) {
                Object attr = n.getAttribute("audio");
                // Only build items for nodes that actually link to audio.
                if (attr == null || attr.toString().trim().isEmpty()) {
                    continue;
                }
                AudioItem audioItem = new AudioItem(attr.toString());
                audioItem.setData(NodeItem.X, n.x());
                audioItem.setData(NodeItem.Y, -n.y());
                audioItem.setData(NodeItem.Z, n.z());
                audioItem.setData(NodeItem.SIZE, n.size() * 2f);
                audioItem.setData(NodeItem.COLOR, new Color((int) (n.r() * 255),
                        (int) (n.g() * 255),
                        (int) (n.b() * 255),
                        (int) (n.alpha() * 255)));
                built.add(audioItem);
            }
        } catch (Exception e){
            built.clear();
            logger.log(Level.SEVERE,null,e);
        } 
        return built.toArray(new AudioItem[0]);
        
    }
    
    @Override
    public String getType() {
        return AudioItem.AUDIO;
    }
    
}
