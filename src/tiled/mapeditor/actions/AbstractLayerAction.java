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

package tiled.mapeditor.actions;

import java.awt.event.ActionEvent;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.Icon;

import tiled.core.Map;
import tiled.mapeditor.MapEditor;
import tiled.mapeditor.undo.MapLayerStateEdit;

/**
 * Provides a common abstract class for actions that modify the layer
 * configuration. It makes sure the undo/redo information is properly
 * maintained.
 *
 * @version $Id$
 */
public abstract class AbstractLayerAction extends AbstractAction
{
    protected final MapEditor editor;

    protected AbstractLayerAction(MapEditor editor,
                                  String name, String description)
    {
        super(name);
        putValue(SHORT_DESCRIPTION, description);
        putValue(ACTION_COMMAND_KEY, name);
        this.editor = editor;
    }

    protected AbstractLayerAction(MapEditor editor,
                                  String name, String description, Icon icon)
    {
        this(editor, name, description);
        putValue(SMALL_ICON, icon);
    }

    /**
     * Wraps {@link #doPerformAction} in order to capture the layer vector
     * before and after the action is performed.
     */
    public final void actionPerformed(ActionEvent e) {
        // Capture the layers before the operation is executed.
        Map map = editor.getCurrentMap();
        Vector layersBefore = new Vector(map.getLayerVector());

        doPerformAction();

        // Capture the layers after the operation is executed and create the
        // layer state edit instance.
        Vector layersAfter = new Vector(map.getLayerVector());
        MapLayerStateEdit mapLayerStateEdit =
                new MapLayerStateEdit(map, layersBefore, layersAfter,
                                      e.getActionCommand());
        editor.getUndoSupport().postEdit(mapLayerStateEdit);
    }

    /**
     * Actually performs the action that modifies the layer configuration.
     */
    public abstract void doPerformAction();
}