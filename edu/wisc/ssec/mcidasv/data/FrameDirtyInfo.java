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


/**
 * Class FrameDirtyInfo Holds the state of
 * the frame components from McIdas-X
 */
public class FrameDirtyInfo {
	
	/** Which frame number is this for */
	public int frameNumber = 0;

    /** Dirty status of each component */
    public boolean dirtyImage = false;
    public boolean dirtyGraphics = false;
    public boolean dirtyColorTable = false;

    /**
     * Constructor
     */
    public FrameDirtyInfo() {}

    /**
     * Copy constructor
     *
     * @param that The FrameDirtyInfo to copy
     *
     */
    public FrameDirtyInfo(FrameDirtyInfo that) {
    	this.frameNumber     = that.frameNumber;
        this.dirtyImage      = that.dirtyImage;
        this.dirtyGraphics   = that.dirtyGraphics;
        this.dirtyColorTable = that.dirtyColorTable;
    }

    /**
     * Constructor
     */
    public FrameDirtyInfo( int frameNumber, boolean isDirtyImage, boolean isDirtyGraphics, boolean isDirtyColorTable) {
    	this.frameNumber     = frameNumber;
    	this.dirtyImage      = isDirtyImage;
        this.dirtyGraphics   = isDirtyGraphics;
        this.dirtyColorTable = isDirtyColorTable;
    }

    /**
     * Get the frameNumber property.
     *
     * @return The frameNumber property.
     */
    public int getFrameNumber() {
        return this.frameNumber;
    }
    
    /**
     * Get the dirtyImage property.
     *
     * @return The dirtyImage property.
     */
    public boolean getDirtyImage() {
        return this.dirtyImage;
    }

    /**
     * Get the dirtyGraphics property.
     *
     * @return The dirtyGraphics property.
     */
    public boolean getDirtyGraphics() {
        return this.dirtyGraphics;
    }


    /**
     * Get the dirtyColorTable property.
     *
     * @return The dirtyColorTable property.
     */
    public boolean getDirtyColorTable() {
        return this.dirtyColorTable;
    }

    /**
     * Set the frameNumber property.
     *
     * @param newValue The new vaue for the frameNumber property.
     */
    public void setFrameNumber(int newValue) {
        this.frameNumber = newValue;
    }
    
    /**
     * Set the dirtyImage property.
     *
     * @param newValue The new vaue for the dirtyImage property.
     */
    public void setDirtyImage(boolean newValue) {
        this.dirtyImage = newValue;
    }

    /**
     * Set the dirtyGraphics property.
     *
     * @param newValue The new vaue for the dirtyGraphics property.
     */
    public void setDirtyGraphics(boolean newValue) {
        this.dirtyGraphics = newValue;
    }

    /**
     * Set the dirtyColorTable property.
     *
     * @param newValue The new vaue for the dirtyColorTable property.
     */
    public void setDirtyColorTable(boolean newValue) {
        this.dirtyColorTable = newValue;
    }

    /**
     * Get a String representation of this object
     * @return a string representation
     */
    public String toString() {
    	boolean clean = true;
        StringBuffer buf = new StringBuffer();
        buf.append("frame ");
        buf.append(this.frameNumber);
        buf.append(": ");
        if (this.dirtyImage) {
        	if (clean) buf.append("dirty ");
        	else buf.append (", ");
        	clean = false;
        	buf.append("image");
        }
        if (this.dirtyGraphics) {
        	if (clean) buf.append("dirty ");
        	else buf.append (", ");
        	clean = false;
        	buf.append("graphics");
        }
        if (this.dirtyColorTable) {
        	if (clean) buf.append("dirty ");
        	else buf.append (", ");
        	clean = false;
        	buf.append("colortable");
        }
        if (clean) buf.append("clean");
        return buf.toString();
    }

}
