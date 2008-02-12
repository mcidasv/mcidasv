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

package edu.wisc.ssec.mcidasv.data;


import java.util.ArrayList;
import java.util.List;

import ucar.unidata.util.Misc;


/**
 * Class to hold information about a list of McIdas-X frame datasets
 */
public class McDataset {

	/** dataset name */
    String myName;
    
	/** list of frames */
    List myXFrames;

    /** list of frame numbers */
    public List frameNumbers = new ArrayList();

    /**
     *  Empty constructor for XML encoding
     */
    public McDataset() {}

    /**
     * Construct a new McDataset with the specified name
     * and a list of image descriptors.
     *
     * @param name			what is your name?
     * @param frames		a List of frames to load.
     */
    public McDataset(String name, List xFrameList) {
    	myName = name;
    	if (xFrameList != null) {
    		for (int i=0; i<xFrameList.size(); i++) {
    			McIdasFrame xFrame = (McIdasFrame) xFrameList.get(i);
    			frameNumbers.add(new Integer(xFrame.getFrameNumber()));
    		}
        }
        else {
        	frameNumbers.add(new Integer(-1));
        }
    }

    /**
     * Check to see if this McDataset is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof McDataset)) {
            return false;
        }
        McDataset that = (McDataset) o;
        return (this == that) || Misc.equals(that.myXFrames, this.myXFrames);
    }

}
