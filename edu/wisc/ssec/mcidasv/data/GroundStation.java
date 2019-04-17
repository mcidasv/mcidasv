/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2018
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

import java.awt.Font;

import edu.wisc.ssec.mcidasv.data.hydra.CurveDrawer;
import ucar.visad.display.TextDisplayable;
import visad.georef.EarthLocationTuple;

/**
 * 
 * Container for everything needed to maintain state and draw a satellite "ground station".
 * For our purposes we draw the range ring and label the center point. Stuff like Fonts
 * and Colors are determined by the instantiated VisAD objects (CurveDrawer and TextDisplayable).
 * @author tommyj
 *
 */

public class GroundStation
{

    public static final int DEFAULT_ANTENNA_ANGLE = 5;
    
	private String name = null;
	private EarthLocationTuple elt = null;
	private int antennaAngle = DEFAULT_ANTENNA_ANGLE;
	// The station label
	private TextDisplayable td = null;
	// The ground station range ring
	private CurveDrawer cd = null;

    /**
	 * We'll try to get by with only one way to initialize these.
	 */
    
	public GroundStation(String name, EarthLocationTuple location) {
	    this.name = name;
	    this.elt = location;
	}

    /**
     * @return the label
     */
    public String getName() {
        return name;
    }

    /**
     * @return the EarthLocationTuple
     */
    public EarthLocationTuple getElt() {
        return elt;
    }

    /**
     * @param name the label to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param elt the EarthLocationTuple to set
     */
    public void setElt(EarthLocationTuple elt) {
        this.elt = elt;
    }

    /**
     * @return the antenna angle
     */
    public int getAntennaAngle() {
        return antennaAngle;
    }

    /**
     * @param antennaAngle the antenna angle to set
     */
    public void setAntennaAngle(int antennaAngle) {
        this.antennaAngle = antennaAngle;
    }

    /**
     * @return the CurveDrawer
     */
    public CurveDrawer getCd() {
        return cd;
    }

    /**
     * @param cd the CurveDrawer to set
     */
    public void setCd(CurveDrawer cd) {
        this.cd = cd;
    }

    /**
     * @return the TextDisplayable
     */
    public TextDisplayable getTd() {
        return td;
    }

    /**
     * @param td the TextDisplayable to set
     */
    public void setTd(TextDisplayable td) {
        this.td = td;
    }

    /** 
     * Override to only show name since used in combo boxes
     * @see java.lang.Object#toString()
     * 
     */
    @Override
    public String toString() {
        return name;
    }

}
