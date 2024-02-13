/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
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

import java.awt.Color;
import java.awt.Font;

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
    
    private String name;
    private EarthLocationTuple elt;
    private int antennaAngle;
    private Color color = Color.MAGENTA;
    private double altitude;
    private Font font;
    private int lineWidth;
    private int lineStyle;
    private boolean globeDisplay;
    
    public GroundStation() {
        // for bundles
    }
    
    public GroundStation(String name, EarthLocationTuple location) {
        this(name, location, DEFAULT_ANTENNA_ANGLE);
    }
    
    /**
     * Constructor with additional antenna angle parameter.
     */

    public GroundStation(String name, EarthLocationTuple location, int angle) {
        this.name = name;
        this.elt = location;
        this.antennaAngle = angle;
    }

    /**
     * Constructor with additional antenna angle and altitude parameters.
     */

    public GroundStation(String name, EarthLocationTuple location, int angle, double altitude) {
        this.name = name;
        this.elt = location;
        this.antennaAngle = angle;
        this.altitude = altitude;
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

    public Color getColor() {
        return color;
    }

    public void setColor(Color newColor) {
        color = newColor;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double newAltitude) {
        altitude = newAltitude;
    }

    public void setFont(Font newFont) {
        font = newFont;
    }
    
    public Font getFont() {
        return font;
    }
    
    public void setGlobeDisplay(boolean newValue) {
        globeDisplay = newValue;
    }
    
    public boolean getGlobeDisplay() {
        return globeDisplay;
    }
    
    public void setLineStyle(int newStyle) {
        lineStyle = newStyle;
    }
    
    public int getLineStyle() {
        return lineStyle;
    }
    
    public void setLineWidth(int newWidth) {
        lineWidth = newWidth;
    }
    
    public int getLineWidth() {
        return lineWidth;
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
