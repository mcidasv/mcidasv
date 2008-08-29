/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv;

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
}
