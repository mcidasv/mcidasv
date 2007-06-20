package edu.wisc.ssec.mcidasv.data;


import java.util.ArrayList;
import java.util.List;

import ucar.unidata.util.Misc;


/**
 * Class to hold information about a list of McIdas-X frame datasets
 */
public class McDataset {

    /** name of this dataset */
    String myName;

    /** list of frame numbers */
    List myDescriptors;

    String myRequest;

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
     * @param name              what is your name?
     * @param imageDescriptors  a List of decriptors to locate
     *                          the images.  These could be filenames,
     *                          URLS (ADDE or otherwise)
     */
    public McDataset(String name, List frameDescriptors) {
        if (frameDescriptors != null) {
          for (int i=0; i<frameDescriptors.size(); i++) {
            McIdasFrameDescriptor fd = (McIdasFrameDescriptor) frameDescriptors.get(i);
            myRequest = fd.myRequest;
            frameNumbers.add(new Integer(fd.myNumber));
          }
        }
        else
          frameNumbers.add(new Integer(-1));
    }

    /**
     * Get the name of this dataset.
     *
     * @return the dataset name
     */
    public String getDatasetName() {
        return this.myName;
    }


    public String getRequest() {
        return this.myRequest;
    }

    /**
     * Set the name of this dataset.
     *
     * @param name   name for this dataset
     */
    public void setDatasetName(String name) {
        this.myName = name;
    }

    /**
     * Get the descriptors (locations) of the frames
     * in this dataset.
     *
     * @return list of frame descriptors
     */
    public List getFrameDescriptors() {
        return this.myDescriptors;
    }


    /**
     * Set the descriptors (locations) of the frames
     * in this dataset.
     *
     * @param d  the list of descriptors
     */
    public void setFrameDescriptors(List d) {
        this.myDescriptors = d;
    }

    /**
     * Get the number of frames in the dataset.
     *
     * @return number of descriptors
     */
    public int getNumFrames() {
        return this.myDescriptors.size();
    }

    /**
     * Return a String representation of this object.
     *
     * @return  a String representation of this object
     */
    public String toString() {
        return this.myName;
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
        return (this == that)
               || (Misc.equals(that.myName, this.myName)
                   && Misc.equals(that.myDescriptors, this.myDescriptors));
    }

    /**
     * Get the hashcode for this object
     *
     * @return  the hash code
     */
    public int hashCode() {
        int hashCode = this.myName.hashCode();
        hashCode ^= this.myDescriptors.hashCode();
        return hashCode;
    }
}
