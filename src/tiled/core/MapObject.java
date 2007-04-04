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

package tiled.core;

import java.awt.Rectangle;
import java.util.Properties;

/**
 * @version $Id$
 */
public class MapObject
{
    private Sprite sprite;
    private Properties properties;

    protected float mapX, mapY;
    protected Rectangle bounds;
    protected boolean bVisible = true;
    protected String name, source, type;
    protected int width, height;

    public MapObject() {
        bounds = new Rectangle();
        properties = new Properties();
        name = "Object";
    }

    public void setX(int x) {
        mapX = x;
    }

    public void setY(int y) {
        mapY = y;
    }
    
    public void setName(String s) {
        name = s;
    }
    
    public void setSource(String s) {
        source = s;
    }

    public void setType(String s) {
        type = s;
    }

    public void setSprite(Sprite s) {
        sprite = s;
    }
    
    public void setWidth(int w) {
        width = w;
    }
    
    public void setHeight(int h) {
        height = h;
    }
    
    public void setProperties(Properties p) {
        properties = p;
    }

    public void translate(int x, int y) {
        mapX += x;
        mapY += y;
    }

    public int getX() {
        return (int) mapX;
    }

    public int getY() {
        return (int) mapY;
    }
    
    public String getName() {
        return name;
    }
    
    public String getSource() {
        return source;
    }

    public String getType() {
        return type;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }

    public Properties getProperties() {
        return properties;
    }

    public String toString() {
        return type + " (" + mapX + "," + mapY + ")";
    }
}
