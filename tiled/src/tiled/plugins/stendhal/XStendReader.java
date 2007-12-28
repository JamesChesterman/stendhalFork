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

package tiled.plugins.stendhal;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.swing.filechooser.FileFilter;
import tiled.core.Map;
import tiled.plugins.MapReaderPlugin;

/**
 * Reads the compressed .xstend map file format.
 * 
 * @author mtotz
 */
public class XStendReader extends Reader implements MapReaderPlugin {
	/** reads the map. */
	public Map readMap(String filename) {
		try {
			return readMap(new FileInputStream(new File(filename)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** reads the map. */
	public Map readMap(InputStream inputStream) {
		return readMap(inputStream, true);
	}

	/** all filefilters .*/
	public FileFilter[] getFilters() {
		return new FileFilter[] { new FileFilter() {

			public boolean accept(File pathname) {
				return pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".xstend");
			}

			public String getDescription() {
				return "Stendhal Compressed Map Files (*.xstend)";
			}

		} };
	}

	/** returns the description. */
	public String getPluginDescription() {
		return "Mapreader for the Stendhal compressed map format";
	}
}
