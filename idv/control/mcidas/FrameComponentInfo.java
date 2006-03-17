package ucar.unidata.idv.control.mcidas;




/**
 * Class FrameComponentInfo Holds the state of
 * the frame components refreshed by the McIDASPoller
 */
public class FrameComponentInfo {


    /** Should we include image data */
    public boolean isImage = true;

    /** Should we include graphics data */
    public boolean isGraphics = true;


    /** Should we include color tables data */
    public boolean isColorTable = true;


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
        return this.isImage;
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
     * Get the isColorTable property.
     *
     * @return The isColorTable property.
     */
    public boolean getIsColorTable() {
        return this.isColorTable;
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
     * Set the isGraphics property.
     *
     * @param newValue The new vaue for the isGraphics property.
     */
    public void setIsGraphics(boolean newValue) {
        this.isGraphics = newValue;
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
