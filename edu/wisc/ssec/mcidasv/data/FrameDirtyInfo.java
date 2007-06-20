package edu.wisc.ssec.mcidasv.data;


/**
 * Class FrameDirtyInfo Holds the state of
 * the frame components from McIdas-X
 */
public class FrameDirtyInfo {


    /** Dirty status of each component */
    public boolean dirtyImage = false;
    public boolean dirtyGraphics = false;
    public boolean dirtyColorTable = false;


    /**
     * Constructor
     *
     *
     */
    public FrameDirtyInfo() {}



    /**
     * Copy constructor
     *
     * @param that The FrameDirtyInfo to copy
     *
     */
    public FrameDirtyInfo(FrameDirtyInfo that) {
        this.dirtyImage      = that.dirtyImage;
        this.dirtyGraphics   = that.dirtyGraphics;
        this.dirtyColorTable = that.dirtyColorTable;
    }



    /**
     * Constructor
     */
    public FrameDirtyInfo( boolean isDirtyImage, boolean isDirtyGraphics, boolean isDirtyColorTable) {
        this.dirtyImage      = isDirtyImage;
        this.dirtyGraphics   = isDirtyGraphics;
        this.dirtyColorTable = isDirtyColorTable;
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
        StringBuffer buf = new StringBuffer();
        buf.append("dirtyImage? ");
        buf.append(this.dirtyImage);
        buf.append(", dirtyGraphics? ");
        buf.append(this.dirtyGraphics);
        buf.append(", dirtyColorTable? ");
        buf.append(this.dirtyColorTable);
        return buf.toString();
    }

}
