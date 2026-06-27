/*
 * Licensed under the GNU General Public License Version 3.
 */
package org.yale.cs.graphics.gephi.imagepreview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.preview.api.Item;
import org.gephi.preview.plugin.items.NodeItem;
import org.junit.Test;

/**
 * Unit tests for {@link NodeImageItemBuilder}.
 * <p>
 * These exercise the builder against a real in-memory {@link GraphModel}
 * (created via {@code GraphModel.Factory.newInstance()}), so no running Gephi
 * instance is required. This is the code that had to change most during the
 * 0.8.x to 0.9.x migration, so it is the most valuable thing to lock down.
 */
public class NodeImageItemBuilderTest {

    private static final String IMAGE_COLUMN = "image";

    private GraphModel newModel() {
        return GraphModel.Factory.newInstance();
    }

    @Test
    public void noImageColumn_returnsEmpty() {
        GraphModel model = newModel();
        Graph graph = model.getGraph();
        Node n = model.factory().newNode("n0");
        graph.addNode(n);

        Item[] items = new NodeImageItemBuilder().getItems(graph);

        assertNotNull(items);
        assertEquals("No 'image' column means nothing to render", 0, items.length);
    }

    @Test
    public void onlyNodesWithImageAttribute_produceItems() {
        GraphModel model = newModel();
        model.getNodeTable().addColumn(IMAGE_COLUMN, String.class);
        Graph graph = model.getGraph();

        Node withImage = model.factory().newNode("withImage");
        withImage.setAttribute(IMAGE_COLUMN, "node.png");

        Node withoutImage = model.factory().newNode("withoutImage");
        // no image attribute set

        Node blankImage = model.factory().newNode("blankImage");
        blankImage.setAttribute(IMAGE_COLUMN, "   "); // whitespace-only is ignored

        graph.addNode(withImage);
        graph.addNode(withoutImage);
        graph.addNode(blankImage);

        Item[] items = new NodeImageItemBuilder().getItems(graph);

        assertEquals("Only the node with a real image name yields an item", 1, items.length);
        assertTrue(items[0] instanceof ImageItem);
        assertEquals("node.png", items[0].getSource());
    }

    @Test
    public void builtItem_copiesGeometryAndFlipsY() {
        GraphModel model = newModel();
        model.getNodeTable().addColumn(IMAGE_COLUMN, String.class);
        Graph graph = model.getGraph();

        Node n = model.factory().newNode("n0");
        n.setAttribute(IMAGE_COLUMN, "node.png");
        n.setX(5f);
        n.setY(7f);
        n.setSize(10f);
        graph.addNode(n);

        Item[] items = new NodeImageItemBuilder().getItems(graph);
        assertEquals(1, items.length);
        Item item = items[0];

        // The builder copies x as-is and flips the sign of y (Gephi's preview
        // pipeline uses an inverted y axis relative to the graph model).
        assertEquals(5f, (Float) item.getData(NodeItem.X), 0.0001f);
        assertEquals(-7f, (Float) item.getData(NodeItem.Y), 0.0001f);

        // Size is doubled to match the diameter used elsewhere in the renderer.
        assertEquals(20f, (Float) item.getData(NodeItem.SIZE), 0.0001f);

        assertNotNull("A colour should always be attached", item.getData(NodeItem.COLOR));
    }

    @Test
    public void getType_isImage() {
        assertSame(ImageItem.IMAGE, new NodeImageItemBuilder().getType());
    }
}
