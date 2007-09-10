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
