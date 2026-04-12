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
        
        Item[] items = new AudioItem[graph.getNodeCount()];
        
        int i = 0;
        try {
            for (Node n : graph.getNodes()) {
                AudioItem audioItem = new AudioItem((String)n.getAttribute("audio"));
                audioItem.setData(NodeItem.X, n.x());
                audioItem.setData(NodeItem.Y, -n.y());
                audioItem.setData(NodeItem.Z, n.z());
                audioItem.setData(NodeItem.SIZE, n.size() * 2f);
                audioItem.setData(NodeItem.COLOR, new Color((int) (n.r() * 255),
                        (int) (n.g() * 255),
                        (int) (n.b() * 255),
                        (int) (n.alpha() * 255)));
                items[i++] = audioItem;
            }
        } catch (Exception e){
            items = new AudioItem[0];
            logger.log(Level.SEVERE,null,e);
        } 
        return items;
        
    }
    
    @Override
    public String getType() {
        return AudioItem.AUDIO;
    }
    
}
