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

import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.LatLonProjection;

/**
 * An extension of <tt>LatLonProjection</tt> that adds properties for 
 * the center point and dimensions of the projection box.  Appropriate
 * getters and setters are added so they will be picked up by the
 * <tt>ProjectionImpl</tt> and thereby editable by the user.
 * 
 * @author McIDAS-V Dev Team, UW SSEC
 * @version $Id$
 */
public class McIDASVLatLonProjection extends LatLonProjection {

	private static final long serialVersionUID = -4939597425144220140L;

	public McIDASVLatLonProjection() {
		this("");
	}

	public McIDASVLatLonProjection(String name) {
		this(name, new ProjectionRect(-180, -90, 180, 90));
	}

	public McIDASVLatLonProjection(String name, ProjectionRect mapArea) {
        addParameter("grid_mapping_name", "McVLatLon");
        this.name = name;
        defaultMapArea = mapArea;
	}
	
    /**
     * Get the class name
     * @return class name
     */
    public String getClassName() {
        return "McVLatLon";
    }


    /**
     * Get the label to be used in the gui for this type of projection
     *
     * @return Type label
     */
    public String getProjectionTypeLabel() {
        return "McV Lat/Lon";
    }
	
	/**
	 * Set the center of the projection box X coord.
	 * @param x
	 */
	public void setCenterX(double x) {
		defaultMapArea.x = x - defaultMapArea.width/2;
	}
	
	/**
	 * Set the center of the projection box Y coord.
	 * @param y
	 */
	public void setCenterY(double y) {
		defaultMapArea.y = y - defaultMapArea.height/2;
	}
	
	/**
	 * Set the overall width of the projection box.
	 * @param w
	 */
	public void setLonWidth(double w) {
		defaultMapArea.width = w;
	}
	
	/**
	 * Set the overall height of the projection box
	 * @param h
	 */
	public void setLatHeight(double h) {
		defaultMapArea.height = h;
	}
	
	public double getCenterX() {
		return defaultMapArea.x + defaultMapArea.width/2;
	}
	
	public double getCenterY() {
		return defaultMapArea.y + defaultMapArea.height/2;
	}

	public double getLonWidth() {
		return defaultMapArea.width;
	}
	
	public double getLatHeight() {
		return defaultMapArea.height;
	}
	
    /**
     * Make the default display projection
     * @return Default display projection
     */
    protected ProjectionImpl makeDefaultProjection() {
        return new McIDASVLatLonProjection("World",
                                    new ProjectionRect(-180., -180., 180.,
                                        180.));
    }
	
}
