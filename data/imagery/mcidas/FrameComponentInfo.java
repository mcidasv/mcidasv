package ucar.unidata.data.imagery;




/**
 * Class FrameComponentInfo Holds the state of
 * the frame components refreshed by the McIDASPoller
 */
public class FrameComponentInfo {



    /** Should we include image data */
    private boolean isImage = true;

    /** Should we include graphics data */
    private boolean isGraphics = true;


    /** Should we include color tables data */
    private boolean isColorTable = true;


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
        this.isImage       = that.isImage;
        this.isGraphics    = that.isGraphics;
        this.isColorTable  = that.isColorTable;
    }



    /**
     * Constructor
     *
     * @param isImage The isImage parameter
     * @param isGraphics The isGraphics parameter
     * @param isColorTable The isColorTable parameter
     *
     */
    public FrameComponentInfo(boolean isImage, boolean isGraphics, boolean isColorTable) {
        this.isImage = isImage;
        this.isGraphics = isGraphics;
        this.isColorTable = isColorTable;
    }



    /**
     * Get the isImage property.
     *
     * @return The isImage property.
     */
    public boolean getIsImage() {
        return isImage;
    }

    /**
     * Get the isGraphics property.
     *
     * @return The isGraphics property.
     */
    public boolean getIsGraphics() {
        return isGraphics;
    }

    /**
     * Get the isColorTable property.
     *
     * @return The isColorTable property.
     */
    public boolean getIsColorTable() {
        return isColorTable;
    }

    /**
     * Set the isImage property.
     *
     * @param newValue The new vaue for the isImage property.
     */
    public void setIsImage(boolean newValue) {
        isImage = newValue;
    }

    /**
     * Set the isGraphics property.
     *
     * @param newValue The new vaue for the isGraphics property.
     */
    public void setIsGraphics(boolean newValue) {
        isGraphics = newValue;
    }

    /**
     * Set the isColorTable property.
     *
     * @param newValue The new vaue for the isColorTable property.
     */
    public void setIsColorTable(boolean newValue) {
        isColorTable = newValue;
    }

    /**
     * Get a String representation of this object
     * @return a string representation
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("isImage?: ");
        buf.append(isImage);
        buf.append(", isGraphics?: ");
        buf.append(isGraphics);
        buf.append(", isColorTable?: ");
        buf.append(isColorTable);
        return buf.toString();
    }

}
