package ucar.unidata.data.imagery.mcidas;




/**
 * Class FrameComponentInfo Holds the state of
 * the frame components refreshed by the McIDASPoller
 */
public class FrameComponentInfo {


    public boolean needToRestart = false;

    /** Should we include image data */
    private boolean isImage = true;

    /** Should we include graphics data */
    private boolean isGraphics = true;


    /** Should we include color tables data */
    private boolean isColorTable = true;

    public boolean dirtyImage = false;
    public boolean dirtyGraphics = false;
    public boolean dirtyColorTable = false;


    /**
     * Constructor
     *
     *
     */
    public FrameComponentInfo() {}



    /**
     * Copy constructor
     *
     * @param that The FrameComponentInfo to copy
     *
     */
    public FrameComponentInfo(FrameComponentInfo that) {
        this.isImage         = that.isImage;
        this.isGraphics      = that.isGraphics;
        this.isColorTable    = that.isColorTable;
        this.dirtyImage      = that.dirtyImage;
        this.dirtyGraphics   = that.dirtyGraphics;
        this.dirtyColorTable = that.dirtyColorTable;
    }



    /**
     * Constructor
     *
     * @param isImage The isImage parameter
     * @param isGraphics The isGraphics parameter
     * @param isColorTable The isColorTable parameter
     *
     */
    public FrameComponentInfo(boolean isImage, boolean isGraphics, boolean isColorTable,
           boolean isDirtyImage, boolean isDirtyGraphics, boolean isDirtyColorTable) {
        this.isImage = isImage;
        this.isGraphics = isGraphics;
        this.isColorTable = isColorTable;
        this.dirtyImage      = isDirtyImage;
        this.dirtyGraphics   = isDirtyGraphics;
        this.dirtyColorTable = isDirtyColorTable;
    }



    /**
     * Get the isImage property.
     *
     * @return The isImage property.
     */
    public boolean getIsImage() {
        return this.isImage;
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
     * Get the isGraphics property.
     *
     * @return The isGraphics property.
     */
    public boolean getIsGraphics() {
        return this.isGraphics;
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
     * Get the isColorTable property.
     *
     * @return The isColorTable property.
     */
    public boolean getIsColorTable() {
        return this.isColorTable;
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
     * Set the isImage property.
     *
     * @param newValue The new vaue for the isImage property.
     */
    public void setIsImage(boolean newValue) {
        this.isImage = newValue;
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
     * Set the isGraphics property.
     *
     * @param newValue The new vaue for the isGraphics property.
     */
    public void setIsGraphics(boolean newValue) {
        this.isGraphics = newValue;
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
     * Set the isColorTable property.
     *
     * @param newValue The new vaue for the isColorTable property.
     */
    public void setIsColorTable(boolean newValue) {
        this.isColorTable = newValue;
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
        buf.append("isImage?: ");
        buf.append(this.isImage);
        buf.append(", isGraphics?: ");
        buf.append(this.isGraphics);
        buf.append(", isColorTable?: ");
        buf.append(this.isColorTable);
        return buf.toString();
    }

}
