/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
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

package edu.wisc.ssec.mcidasv.data;

import java.util.List;

import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.idv.IdvProjectionManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.view.geoloc.ProjectionManager;

@SuppressWarnings("unchecked")
public class McIDASVProjectionManager extends IdvProjectionManager {

	/**
	 * Register the <tt>McIDASVLatLonProjection</tt> and pass to the super.
	 * @param idv
	 */
	public McIDASVProjectionManager(IntegratedDataViewer idv) {
		super(idv);
		List projections = ProjectionManager.getDefaultProjections();
		projections.add(0, "edu.wisc.ssec.mcidasv.data.McIDASVLatLonProjection");
	}

    /**
     * Make the default display projection a <tt>McIDASVLatLonProjection</tt>
     * @return Default display projection
     * @see edu.wisc.ssec.mcidasv.data.McIDASVProjectionManager
     */
	@Override
    protected ProjectionImpl makeDefaultProjection() {
        return new McIDASVLatLonProjection("World",
                                    new ProjectionRect(-180., -180., 180.,
                                        180.));
    }
	
}
