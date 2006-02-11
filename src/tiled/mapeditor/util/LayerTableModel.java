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

package tiled.mapeditor.util;

import javax.swing.table.AbstractTableModel;

import tiled.core.MapLayer;
import tiled.core.MultilayerPlane;

/**
 * @version $Id$
 */
public class LayerTableModel extends AbstractTableModel
{
    private MultilayerPlane map;
    private static final String[] columnNames = {
            "Locked", "Show", "Layer name"
    };

    public LayerTableModel(MultilayerPlane map) {
        this.map = map;
    }

    public void setMap(MultilayerPlane map) {
        this.map = map;
        fireTableDataChanged();
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public int getRowCount() {
        if (map != null) {
            int totalLayers = map.getTotalLayers();
            /*
            for (int j = 0; j < map.getTotalLayers(); j++) {
                if (map.getLayer(j).getClass() == SelectionLayer.class) {
                    if (TiledConfiguration.keyHasValue("tiled.layer.showselection", "1")) {
                        totalLayers++;
                    }
                } else {
                    totalLayers++;
                }
            }
            */
            return totalLayers;
        } else {
            return 0;
        }
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public Class getColumnClass(int col) {
        switch (col) {
            case 0: return Boolean.class;
            case 1: return Boolean.class;
            case 2: return String.class;
        }
        return null;
    }

    public Object getValueAt(int row, int col) {
    	MapLayer layer = map.getLayer(getRowCount() - row - 1);
        
        if (layer != null) {
            if (col == 0) {
                return Boolean.valueOf(layer.getLocked() || !layer.isVisible());
            } else if (col == 1) {
                return Boolean.valueOf(layer.isVisible());
            } else if (col == 2) {
                return layer.getName();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean isCellEditable(int row, int col) {
    	MapLayer layer = map.getLayer(getRowCount() - row - 1);

        return !(col == 0 && layer != null && !layer.isVisible());
    }

    public void setValueAt(Object value, int row, int col) {
        MapLayer layer = map.getLayer(getRowCount() - row - 1);
        if (layer != null) {
            if (col == 0) {
                Boolean bool = (Boolean)value;
                layer.setLocked(bool.booleanValue());
            } else if (col == 1) {
                Boolean bool = (Boolean)value;
                layer.setVisible(bool.booleanValue());
            } else if (col == 2) {
                layer.setName(value.toString());
            }
            fireTableCellUpdated(row, col);
        }
    }
}