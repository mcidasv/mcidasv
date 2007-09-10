package edu.wisc.ssec.mcidasv.control;


/**
 * Class FrameComponentInfo Holds the state of
 * the frame components from McIDAS-X
 */
public class FrameComponentInfo {

    /** Should we include image data */
    private boolean isImage = true;

    /** Should we include graphics data */
    private boolean isGraphics = true;

    /** Should we include color tables data */
    private boolean isColorTable = true;
    
    /** Should we include the annotation line */
    private boolean isAnnotation = true;
    
    /** Should we reset the projection when the data is refreshed */
    private boolean resetProjection = true;
    
    /** Should we fake the date to preserve frame order */
    private boolean fakeDateTime = false;

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
        this.isAnnotation    = that.isAnnotation;
        this.resetProjection = that.resetProjection;
        this.fakeDateTime    = that.fakeDateTime;
    }

    /**
     * Constructor
     *
     * @param isImage The isImage parameter
     * @param isGraphics The isGraphics parameter
     * @param isColorTable The isColorTable parameter
     *
     */
    public FrameComponentInfo(boolean isImage,
    						  boolean isGraphics,
    						  boolean isColorTable,
    						  boolean isAnnotation,
    						  boolean resetProjection,
    						  boolean fakeDateTime) {
        this.isImage = isImage;
        this.isGraphics = isGraphics;
        this.isColorTable = isColorTable;
        this.isAnnotation = isAnnotation;
        this.resetProjection = resetProjection;
        this.fakeDateTime = fakeDateTime;
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
     * Get the isAnnotation property.
     *
     * @return The isAnnotation property.
     */
    public boolean getIsAnnotation() {
        return this.isAnnotation;
    }
    
    /**
     * Get the resetProjection property.
     *
     * @return The resetProjection property.
     */
    public boolean getResetProjection() {
        return this.resetProjection;
    }

    /**
     * Get the fakeDateTime property.
     *
     * @return The fakeDateTime property.
     */
    public boolean getFakeDateTime() {
        return this.fakeDateTime;
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
     * Set the isAnnotation property.
     *
     * @param newValue The new vaue for the isAnnotation property.
     */
    public void setIsAnnotation(boolean newValue) {
        this.isAnnotation = newValue;
    }
    
    /**
     * Set the resetProjection property.
     *
     * @param newValue The new vaue for the resetProjection property.
     */
    public void setResetProjection(boolean newValue) {
        this.resetProjection = newValue;
    }
    
    /**
     * Set the fakeDateTime property.
     *
     * @param newValue The new vaue for the fakeDateTime property.
     */
    public void setFakeDateTime(boolean newValue) {
        this.fakeDateTime = newValue;
    }
    
    /**
     * Get a String representation of this object
     * @return a string representation
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("isImage: ");
        buf.append(this.isImage);
        buf.append(", isGraphics: ");
        buf.append(this.isGraphics);
        buf.append(", isColorTable: ");
        buf.append(this.isColorTable);
        buf.append(", isAnnotation: ");
        buf.append(this.isAnnotation);
        buf.append(", resetProjection: ");
        buf.append(this.resetProjection);
        buf.append(", fakeDateTime: ");
        buf.append(this.fakeDateTime);
        return buf.toString();
    }

}
