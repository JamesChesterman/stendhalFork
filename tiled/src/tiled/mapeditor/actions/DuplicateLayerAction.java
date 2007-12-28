/*
 *  Tiled Map Editor, (c) 2004
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Adam Turk <aturk@biggeruniverse.com>
 *  Bjorn Lindeijer <b.lindeijer@xs4all.nl>
 *  
 *  modified for Stendhal, an Arianne powered RPG 
 *  (http://arianne.sf.net)
 *
 *  Matthias Totz <mtotz@users.sourceforge.net>
 */

package tiled.mapeditor.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import tiled.core.MapLayer;
import tiled.mapeditor.MapEditor;

/**
 * Duplicates the current layer.
 * 
 * @author mtotz
 */
public class DuplicateLayerAction extends AbstractAction {
	private static final long serialVersionUID = -3670111021863463422L;

	private MapEditor mapEditor;

	public DuplicateLayerAction(MapEditor mapEditor) {
		super("Duplicate Layer");
		putValue(SHORT_DESCRIPTION, "Duplicate current layer");
		putValue(SMALL_ICON, MapEditor.loadIcon("resources/gimp-duplicate-16.png"));
		this.mapEditor = mapEditor;
	}

	public void actionPerformed(ActionEvent e) {
		if (mapEditor.currentLayer >= 0) {
			try {
				MapLayer clone = (MapLayer) mapEditor.getCurrentLayer().clone();
				clone.setName(clone.getName() + " copy");
				mapEditor.currentMap.addLayer(clone);
			} catch (CloneNotSupportedException ex) {
				ex.printStackTrace();
			}
			mapEditor.setCurrentLayer(mapEditor.currentMap.getTotalLayers() - 1);
		}
	}
}
