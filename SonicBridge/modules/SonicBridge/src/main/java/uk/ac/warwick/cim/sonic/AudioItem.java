/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.ac.warwick.cim.sonic;


import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.gephi.preview.api.G2DTarget;
import org.gephi.preview.plugin.items.AbstractItem;
import processing.core.PConstants;
import processing.core.PImage;

/**
 * A Item that represents a image within the file system.
 * <p>
 * Each instance of <code>AudioItem</code> represents a single Audio file. When
 * the file is loaded (using one of the methods) the image data is cached in
 * memory, allowing for faster rendering in the future.
 * <p>
 * In addition to the cached images, the item contains all the properties of
 * {@link NodeItem}.
 *
 * @author Iain
 */
public class AudioItem extends AbstractItem {

    /**
     * The identifier of this item.
     */
    public static final String AUDIO = "Audio";

    /**
     * The data key for accessing the Processing image file within the cache.
     */
    public static final String PROCESSING_DATA = "Processing_Data";

    /**
     * The data key for accessing the iText PDF image file within the cache.
     */
    public static final String PDF_DATA = "PDF_Data";

    private static final Logger logger = Logger.getLogger(AudioItem.class.getName());

    /**
     *
     * @param source The image filename
     */
    public AudioItem(String source) {
        super(source, AUDIO);
    }

    /**
     * Prepares this ImageItem to be rendered by the Processing target. Loading
     * from the cache, if available. If the method is forced to load a image
     * from the hard drive, it store it in the cache.
     *
     * @param location_name The name of the folder to load images from.
     * @param target The properties of the current rendering.
     * @return A Processing image corresponding to this item.
     */
    public PImage renderProcessing(File location_name, G2DTarget target) {
        PImage image = (PImage) data.get(PROCESSING_DATA);
        if (image == null) {
            if (source instanceof String) {
                File full_file = new File(location_name, (String) source);
                try {
                    //if(target.getApplet() != null)
                    //    image = target.getApplet().loadImage(full_file.getCanonicalPath());
                    // else 
                    //based on http://processing.org/discourse/beta/num_1234546778.html
                    //http://forum.processing.org/topic/converting-bufferedimage-to-pimage
                    BufferedImage im_plane;

                    im_plane = ImageIO.read(full_file);
                    image = new PImage(im_plane.getWidth(), im_plane.getHeight(), PConstants.ARGB);
                    im_plane.getRGB(0, 0, image.width, image.height, image.pixels, 0, image.width);
                    image.updatePixels();

                } catch (java.io.IOException e) {
                    logger.log(Level.SEVERE, "Unable to load image: " + full_file, e);
                }
            }

            //If we can't render the image
            if (image == null) {
                logger.log(Level.WARNING, "Unable to load image: {0}", source);
                return null;
            }

            data.put(PROCESSING_DATA, image);
        }
        return image;
    }

    /**
     * @param location_name The name of the folder to load images from.
     * @return The attribute to be added to the SVG file to represent this
     * image.
     */
    public String renderSVG(File location_name) {
        return new File(location_name, (String) source).getAbsolutePath();
    }

    /**
     * Prepares this ImageItem to be rendered by the PDF target. Loading from
     * the cache, if available. If the method is forced to load a image from the
     * hard drive, it stores it in the cache.
     *
     * @param location_name The name of the folder to load images from.
     * @return A iText PDF Image corresponding to this item.
     */
    public com.itextpdf.text.Image renderPDF(File location_name) {
        com.itextpdf.text.Image image = (com.itextpdf.text.Image) data.get(PDF_DATA);
        if (image == null) {
            if (source instanceof String) {
                File full_file = new File(location_name,(String) source);

                try {
                    image = Image.getInstance(full_file.getCanonicalPath());
                } catch (BadElementException ex) {
                    logger.log(Level.SEVERE, "Unable to load image: " + full_file, ex);
                } catch (MalformedURLException ex) {
                    logger.log(Level.SEVERE, "Unable to load image: " + full_file, ex);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Unable to load image: " + full_file, ex);
                }

            }
            //If we can't render the image, fallback to the defult render
            if (image == null) {
                logger.log(Level.WARNING, "Unable to load image: {0}", source);
                return null;
            }

            data.put(PDF_DATA, image);
        }

        return image;
    }
}
