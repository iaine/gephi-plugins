/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.austgate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import org.gephi.preview.plugin.items.EdgeItem;
import org.gephi.preview.api.CanvasSize;
import org.gephi.preview.api.Item;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.api.RenderTarget;
import org.gephi.preview.spi.ItemBuilder;
import org.gephi.preview.spi.Renderer;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * 
 * Based on the Mutual Edges example. This plugin doesn't have to visually 
 * render anything. 
 * 
 * This example shows how a renderer can customize the output of another
 * renderer in the <code>preProcess()</code> phase. Renderers are executed
 * sequentially using the <code>position = 150</code> parameter set to the
 * <code>@ServiceProvider</code> annotation. When no position is set the
 * renderer is executed after all default renderers (nodes, edges, labels and
 * arrows). The default position for the edge renderer is 100 and 200 for the
 * arrow renderer. We know that the arrow renderer will use whatever color is
 * set for the edge so for this example we set a position equal to 150 to be
 * executed after the edge renderer but before the arrow renderer.
 * <p>
 * This example doesn't do anything in the <code>render()</code> method but
 * modifies the color of the edge item.
 * <p>
 * The renderer defines two new properties. Each property should have a unique
 * name so it's a good practice to set the property name as a public constant.
 * Note that the <code>MUTUALEGDE_HIGHLIGHT_COLOR</code> property depends on the
 * <code>MUTUALEGDE_HIGHLIGHT</code> property. Dependencies work only with
 * boolean properties and model the need to enable/disable features. Indeed, no
 * need to customize the highlight color if the feature is disabled by the user.
 * @author iain
 */
@ServiceProvider(service = Renderer.class, position = 450)
public class SonificationRenderer implements Renderer {
    
    //Custom properties
    public static final String SONIFICATION = "sonification.type";
    public static final String SONIFICATION_TYPE = "sonification.type";
    //Default values
    protected boolean defaultSonification = false;
    protected String sonifyType = "edge";
    
    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(SonificationRenderer.class, "SonificationRenderer.name");
    }

    @Override
    public void preProcess(PreviewModel previewModel) {
        PreviewProperties properties = previewModel.getProperties();
        
        if (properties.getBooleanValue(SONIFICATION)) {
            //@todo: set an option for sonifying nodes or edges
            this.sonifyEdges(previewModel);
        }
    }
    
    private void sonifyNodes (PreviewModel previewModel) {
        
        Item[] nodeItems = previewModel.getItems(Item.NODE);
        
        for (Item item: nodeItems) {
            //makes assumption that we've set sound
            String sound = item.getData("sound");
            //makes assumption that weight has been set. 
            String duration = item.getData("weight");
            
            this.sonify(sound, duration);
        }
    }
    
    private void sonifyEdges (PreviewModel previewModel) {
        Item[] edgeItems = previewModel.getItems(Item.EDGE);
        
        for (Item item: edgeItems) {
            //makes assumption that we've set sound
            //String sound = item.getData("sound");
            String sound = "edge.ck";
            //makes assumption that weight has been set. 
            //String duration = (item.getData("weight")) ?
            //        item.getData("weight").toString(): "1.0" ;
            String duration = "0.5";
            System.out.println(item.toString());
            
            this.sonify(sound, duration);
        }
    }
    
    private void sonify (String sound, String duration) {
        try {
 
            ProcessBuilder builder = new ProcessBuilder().command(
                    "/usr/local/bin/chuck ", sound, ":", duration);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                if (line == null) { break; }
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void render(Item item, RenderTarget rt, PreviewProperties pp) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void postProcess(PreviewModel pm, RenderTarget rt, PreviewProperties pp) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    //@todo: add in changing between nodes and edges
    public PreviewProperty[] getProperties() {
        return new PreviewProperty[]{
            PreviewProperty.createProperty(this, SONIFICATION, Boolean.class,
            "Sonification",
            "Sonify edges or nodes", 
            PreviewProperty.CATEGORY_EDGES).setValue(defaultSonification)
        }; 
    }

    @Override
    public boolean isRendererForitem(Item item, PreviewProperties pp) {
        return false; 
    }

    @Override
    public boolean needsItemBuilder(ItemBuilder ib, PreviewProperties pp) {
        return false;
    }

    @Override
    public CanvasSize getCanvasSize(Item item, PreviewProperties pp) {
        return new CanvasSize();
    }
    
}

