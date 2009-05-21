/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv;

import java.io.File;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.StringUtil;

/**
 * @author McIdas-V Team
 * @version $Id$
 */
public class ResourceManager extends IdvResourceManager {

    /** Points to the image parameters defaults */
    public static final XmlIdvResource RSC_IMAGEPARAMETERS =
        new XmlIdvResource("idv.resource.imageparameters",
                           "Image Parameters Defaults", "imageparameters\\.xml$");

    public static final IdvResource RSC_SITESERVERS =
        new XmlIdvResource("mcv.resource.siteservers", 
            "Site-specific Servers", "siteservers\\.xml$");

    public static final IdvResource RSC_NEW_USERSERVERS =
        new XmlIdvResource("mcv.resource.newuserservers", 
            "New style user servers", "persistedservers\\.xml$");

    public static final IdvResource RSC_OLD_USERSERVERS =
        new XmlIdvResource("mcv.resource.olduserservers", 
            "Old style user servers", "addeservers\\.xml$");

	public ResourceManager(IntegratedDataViewer idv) {		
		super(idv);
		checkMoveOutdatedDefaultBundle();
	}

	/**
	 * Adds support for McIdas-V macros.
	 * 
	 * @see ucar.unidata.idv.IdvResourceManager#getResourcePath(java.lang.String)
	 */
	public String getResourcePath(String path) {
		String retPath = path;
		if (path.contains(Constants.MACRO_VERSION)) {
			retPath = StringUtil.replace(
				path, 
				Constants.MACRO_VERSION, 
				((StateManager) getStateManager()).getMcIdasVersion()
			);
		} else {
			retPath = super.getResourcePath(path);
		}
		return retPath;
	}
	
	/**
	 * Look for existing "default.mcv" and "default.xidv" bundles in root userpath
	 * If they exist, move them to the "bundles" directory, preferring "default.mcv"
	 */
	private void checkMoveOutdatedDefaultBundle() {
		String userDirectory = getIdv().getObjectStore().getUserDirectory().toString();
		
		File defaultDir;
		File defaultNew;
		File defaultIdv;
		File defaultMcv;
		
		String os = System.getProperty("os.name");
		if (os == null)
			throw new RuntimeException();
		if (os.startsWith("Windows")) {
			defaultDir = new File(userDirectory + "\\bundles\\General");
			defaultNew = new File(defaultDir.toString() + "\\Default.mcv");
			defaultIdv = new File(userDirectory + "\\default.xidv");
			defaultMcv = new File(userDirectory + "\\default.mcv");
		}
		else {
			defaultDir = new File(userDirectory + "/bundles/General");
			defaultNew = new File(defaultDir.toString() + "/Default.mcv");
			defaultIdv = new File(userDirectory + "/default.xidv");
			defaultMcv = new File(userDirectory + "/default.mcv");
		}
		
		// If no Alpha default bundles exist, bail quickly
		if (!defaultIdv.exists() && !defaultMcv.exists()) return;
		
		// If the destination directory does not exist, create it.
		if (!defaultDir.exists()) {
			if (!defaultDir.mkdirs()) {
				System.err.println("Cannot create directory " + defaultDir.toString() + " for default bundle");
				return;
			}
		}
		
		// If the destination already exists, print lame error message and bail.
		// This whole check should only happen with Alphas so no biggie right?
		if (defaultNew.exists()) {
			System.err.println("Cannot copy current default bundle... " + defaultNew.toString() + " already exists");
			return;
		}
		
		// If only default.xidv exists, try to rename it.
		// If both exist, delete the default.xidv file.  It was being ignored anyway.
		if (defaultIdv.exists()) {
			if (defaultMcv.exists()) {
				defaultIdv.delete();
			}
			else {
				if (!defaultIdv.renameTo(defaultNew)) {
					System.out.println("Cannot copy current default bundle... error renaming " + defaultIdv.toString());
				}
			}
		}
		
		// If only default.mcv exists, try to rename it.
		if (defaultMcv.exists()) {
			if (!defaultMcv.renameTo(defaultNew)) {
				System.out.println("Cannot copy current default bundle... error renaming " + defaultMcv.toString());
			}
		}
				
	}
	
}
