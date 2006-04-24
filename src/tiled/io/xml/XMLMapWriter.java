/*
 *  Tiled Map Editor, (c) 2004-2006
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Adam Turk <aturk@biggeruniverse.com>
 *  Bjorn Lindeijer <b.lindeijer@xs4all.nl>
 */

package tiled.io.xml;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Color;
import java.io.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.prefs.Preferences;
import java.util.zip.GZIPOutputStream;

import tiled.core.*;
import tiled.io.*;
import tiled.mapeditor.selection.SelectionLayer;
import tiled.util.*;

/**
 * @version $Id$
 */
public class XMLMapWriter implements MapWriter
{
    private static final int LAST_BYTE = 0x000000FF;

    /**
     * Saves a map to an XML file.
     *
     * @param filename the filename of the map file
     */
    public void writeMap(Map map, String filename) throws Exception {
        OutputStream os = new FileOutputStream(filename);

        if (filename.endsWith(".tmx.gz")) {
            os = new GZIPOutputStream(os);
        }

        Writer writer = new OutputStreamWriter(os);
        XMLWriter xmlWriter = new XMLWriter(writer);

        xmlWriter.startDocument();
        writeMap(map, xmlWriter, filename);
        xmlWriter.endDocument();

        writer.flush();

        if (os instanceof GZIPOutputStream) {
            ((GZIPOutputStream)os).finish();
        }
    }

    /**
     * Saves a tileset to an XML file.
     *
     * @param filename the filename of the tileset file
     */
    public void writeTileset(TileSet set, String filename) throws Exception {
        OutputStream os = new FileOutputStream(filename);
        Writer writer = new OutputStreamWriter(os);
        XMLWriter xmlWriter = new XMLWriter(writer);

        xmlWriter.startDocument();
        writeTileset(set, xmlWriter, filename);
        xmlWriter.endDocument();

        writer.flush();
    }


    public void writeMap(Map map, OutputStream out) throws Exception {
        Writer writer = new OutputStreamWriter(out);
        XMLWriter xmlWriter = new XMLWriter(writer);

        xmlWriter.startDocument();
        writeMap(map, xmlWriter, "/.");
        xmlWriter.endDocument();

        writer.flush();
    }

    public void writeTileset(TileSet set, OutputStream out) throws Exception {
        Writer writer = new OutputStreamWriter(out);
        XMLWriter xmlWriter = new XMLWriter(writer);

        xmlWriter.startDocument();
        writeTileset(set, xmlWriter, "/.");
        xmlWriter.endDocument();

        writer.flush();
    }

    private static void writeMap(Map map, XMLWriter w, String wp) throws IOException {
        try {
            w.startElement("map");

            w.writeAttribute("version", "0.99a");

            switch (map.getOrientation()) {
                case Map.MDO_ORTHO:
                    w.writeAttribute("orientation", "orthogonal"); break;
                case Map.MDO_ISO:
                    w.writeAttribute("orientation", "isometric"); break;
                case Map.MDO_OBLIQUE:
                    w.writeAttribute("orientation", "oblique"); break;
                case Map.MDO_HEX:
                    w.writeAttribute("orientation", "hexagonal"); break;
                case Map.MDO_SHIFTED:
                    w.writeAttribute("orientation", "shifted"); break;
            }

            w.writeAttribute("width", map.getWidth());
            w.writeAttribute("height", map.getHeight());
            w.writeAttribute("tilewidth", map.getTileWidth());
            w.writeAttribute("tileheight", map.getTileHeight());

            Properties props = map.getProperties();
            for (Enumeration keys = props.keys(); keys.hasMoreElements();) {
                String key = (String)keys.nextElement();
                w.startElement("property");
                w.writeAttribute("name", key);
                w.writeAttribute("value", props.getProperty(key));
                w.endElement();
            }

            int firstgid = 1;
            Iterator itr = map.getTilesets().iterator();
            while (itr.hasNext()) {
                TileSet tileset = (TileSet)itr.next();
                tileset.setFirstGid(firstgid);
                writeTilesetReference(tileset, w, wp);
                firstgid += tileset.getMaxTileId() + 1;
            }

            Iterator ml = map.getLayers();
            while (ml.hasNext()) {
                MapLayer layer = (MapLayer)ml.next();
                writeMapLayer(layer, w);
            }

            w.endElement();
        } catch (XMLWriterException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes a reference to an external tileset into a XML document.  In the
     * degenerate case where the tileset is not stored in an external file,
     * writes the contents of the tileset instead.
     */
    private static void writeTilesetReference(TileSet set, XMLWriter w, String wp)
        throws IOException {

        try {
            String source = set.getSource();

            if (source == null) {
                writeTileset(set, w, wp);
            } else {
                w.startElement("tileset");
                try {
                    w.writeAttribute("firstgid", set.getFirstGid());
                    w.writeAttribute("source", source.substring(
                                source.lastIndexOf(File.separatorChar) + 1));
                    if (set.getBaseDir() != null) {
                        w.writeAttribute("basedir", set.getBaseDir());
                    }
                } finally {
                    w.endElement();
                }
            }
        } catch (XMLWriterException e) {
            e.printStackTrace();
        }
    }

    private static void writeTileset(TileSet set, XMLWriter w, String wp)
        throws IOException {

        try {
            String tilebmpFile = set.getTilebmpFile();
            String name = set.getName();

            w.startElement("tileset");

            if (name != null) {
                w.writeAttribute("name", name);
            }

            w.writeAttribute("firstgid", set.getFirstGid());

            if (tilebmpFile != null) {
                w.writeAttribute("tilewidth", set.getTileWidth());
                w.writeAttribute("tileheight", set.getTileHeight());

                int tileSpacing = set.getTileSpacing();
                if (tileSpacing != 0) {
                    w.writeAttribute("spacing", tileSpacing);
                }
            }

            if (set.getBaseDir() != null) {
                w.writeAttribute("basedir", set.getBaseDir());
            }

            if (tilebmpFile != null) {
                w.startElement("image");
                w.writeAttribute("source", getRelativePath(wp, tilebmpFile));

                Color trans = set.getTransparentColor();
                if (trans != null) {
                    w.writeAttribute("trans", Integer.toHexString(
                                trans.getRGB()).substring(2));
                }
                w.endElement();
            } else {
                // Embedded tileset
                Preferences prefs = TiledConfiguration.node("saving");

                boolean embedImages = prefs.getBoolean("embedImages", true);
                boolean tileSetImages = prefs.getBoolean("tileSetImages", false);

                if (tileSetImages) {
                    Enumeration ids = set.getImageIds();
                    while (ids.hasMoreElements()) {
                        String id = (String)ids.nextElement();
                        w.startElement("image");
                        w.writeAttribute("format", "png");
                        w.writeAttribute("id", id);
                        w.startElement("data");
                        w.writeAttribute("encoding", "base64");
                        w.writeCDATA(new String(Base64.encode(
                                        ImageHelper.imageToPNG(
                                            set.getImageById(Integer.parseInt(id))))));
                        w.endElement();
                        w.endElement();
                    }
                } else if (!embedImages) {
                    String imgSource =
                            prefs.get("tileImagePrefix", "tile") + "set.png";

                    w.startElement("image");
                    w.writeAttribute("source", imgSource);

                    String tilesetFilename = wp.substring(0,
                            wp.lastIndexOf(File.separatorChar) + 1) + imgSource;
                    FileOutputStream fw = new FileOutputStream(new File(
                                tilesetFilename));
                    //byte[] data = ImageHelper.imageToPNG(setImage);
                    //fw.write(data, 0, data.length);
                    w.endElement();

                    fw.close();
                }

                // Check to see if there is a need to write tile elements
                Iterator tileIterator = set.iterator();
                boolean needWrite = !set.isOneForOne();

                if (embedImages) {
                    needWrite = true;
                } else {
                    while (tileIterator.hasNext()) {
                        Tile tile = (Tile)tileIterator.next();
                        if (!tile.getProperties().isEmpty()) {
                            needWrite = true;
                            break;
                            // As long as one has properties, they all
                            // need to be written.
                            // TODO: This shouldn't be necessary
                        }
                    }
                }

                if (needWrite) {
                    tileIterator = set.iterator();
                    while (tileIterator.hasNext()) {
                        Tile tile = (Tile)tileIterator.next();
                        // todo: move this check back into the iterator?
                        if (tile != null) {
                            writeTile(tile, w);
                        }
                    }
                }
            }
            w.endElement();
        } catch (XMLWriterException e) {
            e.printStackTrace();
        }
    }

    private static void writeObjectGroup(ObjectGroup o, XMLWriter w)
        throws IOException
    {
        Iterator itr = o.getObjects();
        while (itr.hasNext()) {
            writeObject((MapObject)itr.next(), o, w);
        }
    }

    /**
     * Writes this layer to an XMLWriter. This should be done <b>after</b> the
     * first global ids for the tilesets are determined, in order for the right
     * gids to be written to the layer data.
     */
    private static void writeMapLayer(MapLayer l, XMLWriter w) throws IOException {
        try {
            Preferences prefs = TiledConfiguration.node("saving");
            boolean encodeLayerData =
                    prefs.getBoolean("encodeLayerData", true);
            boolean compressLayerData =
                    prefs.getBoolean("layerCompression", true) &&
                            encodeLayerData;

            Rectangle bounds = new Rectangle();
            l.getBounds(bounds);

            if (l.getClass() == SelectionLayer.class) {
                w.startElement("selection");
            } else if(l instanceof ObjectGroup){
                w.startElement("objectgroup");
            } else {
                w.startElement("layer");
            }

            w.writeAttribute("name", l.getName());
            w.writeAttribute("width", bounds.width);
            w.writeAttribute("height", bounds.height);
            if (bounds.x != 0) {
                w.writeAttribute("xoffset", bounds.x);
            }
            if (bounds.y != 0) {
                w.writeAttribute("yoffset", bounds.y);
            }

            if (!l.isVisible()) {
                w.writeAttribute("visible", "0");
            }
            if (l.getOpacity() < 1.0f) {
                w.writeAttribute("opacity", l.getOpacity());
            }

            Properties props = l.getProperties();
            for (Enumeration keys = props.keys(); keys.hasMoreElements();) {
                String key = (String)keys.nextElement();
                w.startElement("property");
                w.writeAttribute("name", key);
                w.writeAttribute("value", props.getProperty(key));
                w.endElement();
            }

            if (l instanceof ObjectGroup){
                writeObjectGroup((ObjectGroup)l, w);
            } else {
                w.startElement("data");
                if (encodeLayerData) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    OutputStream out;

                    w.writeAttribute("encoding", "base64");

                    if (compressLayerData) {
                        w.writeAttribute("compression", "gzip");
                        out = new GZIPOutputStream(baos);
                    } else {
                        out = baos;
                    }

                    for (int y = 0; y < l.getHeight(); y++) {
                        for (int x = 0; x < l.getWidth(); x++) {
                            Tile tile = ((TileLayer)l).getTileAt(x, y);
                            int gid = 0;

                            if (tile != null) {
                                gid = tile.getGid();
                            }

                            out.write(gid       & LAST_BYTE);
                            out.write(gid >> 8  & LAST_BYTE);
                            out.write(gid >> 16 & LAST_BYTE);
                            out.write(gid >> 24 & LAST_BYTE);
                        }
                    }

                    if (compressLayerData) {
                        ((GZIPOutputStream)out).finish();
                    }

                    w.writeCDATA(new String(Base64.encode(baos.toByteArray())));
                } else {
                    for (int y = 0; y < l.getHeight(); y++) {
                        for (int x = 0; x < l.getWidth(); x++) {
                            Tile tile = ((TileLayer)l).getTileAt(x, y);
                            int gid = 0;

                            if (tile != null) {
                                gid = tile.getGid();
                            }

                            w.startElement("tile");
                            w.writeAttribute("gid", gid);
                            w.endElement();
                        }
                    }
                }
                w.endElement();
            }
            w.endElement();
        } catch (XMLWriterException e) {
            e.printStackTrace();
        }
    }

    private static void writeTile(Tile tile, XMLWriter w) throws IOException {
        try {
            w.startElement("tile");

            int tileId = tile.getId();

            w.writeAttribute("id", tileId);

            //if (groundHeight != getHeight()) {
            //    w.writeAttribute("groundheight", "" + groundHeight);
            //}

            Properties props = tile.getProperties();
            for (Enumeration keys = props.keys(); keys.hasMoreElements();) {
                String key = (String)keys.nextElement();
                w.startElement("property");
                w.writeAttribute("name", key);
                w.writeAttribute("value", props.getProperty(key));
                w.endElement();
            }

            Image tileImage = tile.getImage();

            Preferences prefs = TiledConfiguration.node("saving");

            boolean embedImages = prefs.getBoolean("embedImages", true);
            boolean tileSetImages = prefs.getBoolean("tileSetImages", false);

            // Write encoded data
            if (tileImage != null && !tileSetImages) {
                if (embedImages && !tileSetImages) {
                    w.startElement("image");
                    w.writeAttribute("format", "png");
                    w.startElement("data");
                    w.writeAttribute("encoding", "base64");
                    w.writeCDATA(new String(Base64.encode(
                                    ImageHelper.imageToPNG(tileImage))));
                    w.endElement();
                    w.endElement();
                } else if (tileSetImages) {
                    w.startElement("image");
                    w.writeAttribute("id", tile.getImageId());
                    w.endElement();
                } else {
                    String prefix = prefs.get("tileImagePrefix", "tile");
                    String filename = prefs.get("maplocation", "") +
                        prefix + tileId + ".png";
                    w.startElement("image");
                    w.writeAttribute("source", prefix + tileId + ".png");
                    FileOutputStream fw = new FileOutputStream(
                            new File(filename));
                    byte[] data = ImageHelper.imageToPNG(tileImage);
                    fw.write(data, 0, data.length);
                    fw.close();
                    w.endElement();
                }
            }

            if (tile instanceof AnimatedTile) {
                writeAnimation(((AnimatedTile)tile).getSprite(), w);
            }

            w.endElement();
        } catch (XMLWriterException e) {
            e.printStackTrace();
        }
    }

    private static void writeAnimation(Sprite s, XMLWriter w) throws IOException {
        try {
            w.startElement("animation");
            for (int k = 0; k < s.getTotalKeys(); k++) {
                Sprite.KeyFrame key = s.getKey(k);
                w.startElement("keyframe");
                w.writeAttribute("name", key.getName());
                for (int it = 0; it < key.getTotalFrames(); it++) {
                    Tile stile = key.getFrame(it);
                    w.startElement("tile");
                    w.writeAttribute("gid", stile.getGid());
                    w.endElement();
                }
                w.endElement();
            }
            w.endElement();
        } catch (XMLWriterException e) {
            e.printStackTrace();
        }
    }

    private static void writeObject(MapObject m, ObjectGroup o, XMLWriter w)
        throws IOException
    {
        try {
            Rectangle b = new Rectangle();
            o.getBounds(b);
            w.startElement("object");
            w.writeAttribute("x", m.getX() + b.x);
            w.writeAttribute("y", m.getY() + b.y);
            w.writeAttribute("type", m.getType());
            if (m.getSource() != null) {
                w.writeAttribute("source", m.getSource());
            }

            Properties props = m.getProperties();
            for (Enumeration keys = props.keys(); keys.hasMoreElements();) {
                String key = (String) keys.nextElement();
                w.startElement("property");
                w.writeAttribute("name", key);
                w.writeAttribute("value", props.getProperty(key));
                w.endElement();
            }

            w.endElement();
        } catch (XMLWriterException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the relative path from one file to the other. The function
     * expects absolute paths, relative paths will be converted to absolute
     * using the working directory.
     *
     * @param from the path of the origin file
     * @param to   the path of the destination file
     * @return     the relative path from origin to destination
     */
    public static String getRelativePath(String from, String to) {
        // Make the two paths absolute and unique
        try {
            from = new File(from).getCanonicalPath();
            to = new File(to).getCanonicalPath();
        } catch (IOException e) {
        }

        File fromFile = new File(from);
        File toFile = new File(to);
        Vector fromParents = new Vector();
        Vector toParents = new Vector();

        // Iterate to find both parent lists
        while (fromFile != null) {
            fromParents.add(0, fromFile.getName());
            fromFile = fromFile.getParentFile();
        }
        while (toFile != null) {
            toParents.add(0, toFile.getName());
            toFile = toFile.getParentFile();
        }

        // Iterate while parents are the same
        int shared = 0;
        int maxShared = Math.min(fromParents.size(), toParents.size());
        for (shared = 0; shared < maxShared; shared++) {
            String fromParent = (String)fromParents.get(shared);
            String toParent = (String)toParents.get(shared);
            if (!fromParent.equals(toParent)) {
                break;
            }
        }

        // Append .. for each remaining parent in fromParents
        StringBuffer relPathBuf = new StringBuffer();
        for (int i = shared; i < fromParents.size() - 1; i++) {
            relPathBuf.append(".." + File.separator);
        }

        // Add the remaining part in toParents
        for (int i = shared; i < toParents.size() - 1; i++) {
            relPathBuf.append(toParents.get(i) + File.separator);
        }
        relPathBuf.append(new File(to).getName());
        String relPath = relPathBuf.toString();

        // Turn around the slashes when path is relative
        try {
            String absPath = new File(relPath).getCanonicalPath();

            if (!absPath.equals(relPath)) {
                // Path is not absolute, turn slashes around
                // Assumes: \ does not occur in filenames
                relPath = relPath.replace('\\', '/');
            }
        } catch (IOException e) {
        }

        return relPath;
    }

    /**
     * @see tiled.io.MapReader#getFilter()
     */
    public String getFilter() throws Exception {
        return "*.tmx,*.tsx,*.tmx.gz";
    }

    public String getPluginPackage() {
        return "Tiled internal TMX reader/writer";
    }

    public String getDescription() {
        return
            "The core Tiled TMX format writer\n" +
            "\n" +
            "Tiled Map Editor, (c) 2005\n" +
            "Adam Turk\n" +
            "Bjorn Lindeijer";
    }

    public String getName() {
        return "Default Tiled XML (TMX) map writer";
    }

    public boolean accept(File pathname) {
        try {
            String path = pathname.getCanonicalPath();
            if (path.endsWith(".tmx") || path.endsWith(".tsx") || path.endsWith(".tmx.gz")) {
                return true;
            }
        } catch (IOException e) {}
        return false;
    }

    public void setLogger(PluginLogger logger) {
    }
}
