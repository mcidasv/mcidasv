package ucar.unidata.data.imagery.mcidas;


/**
 * A class to hold a frame directory, number and time
 */
public class McIDASFrameDescriptor {


    /** Frame number */
     
    public int myNumber;

    /** McX link */
    public String myRequest;

    /**
     * Default constructor for unpersistence; does nothing
     *
     */
    public McIDASFrameDescriptor() {}

    /**
     * Create a descriptor from another.
     *
     * @param that   other image descriptor
     *
     */
    public McIDASFrameDescriptor(McIDASFrameDescriptor that) {
        this.myNumber    = that.myNumber;
        this.myRequest    = that.myRequest;
    }

    /**
     * Create a frame descriptor from the frame number
     *
     * @param int frameNumber
     */
    public McIDASFrameDescriptor(int frameNumber, String request) {
        this.myNumber = frameNumber;
        this.myRequest = request;
    }


    /**
     * See if this McIDASFrameDescriptor is equal to the object in
     * question
     *
     * @param o   object in question
     * @return  true if <code>o</code> is an McIDASFrameDescriptor and
     *          they area equivalent
     */
    public boolean equals(Object o) {
        if ( !(o instanceof McIDASFrameDescriptor)) {
            return false;
        }
        McIDASFrameDescriptor that = (McIDASFrameDescriptor) o;
        return (this.myNumber == that.myNumber);
    }


    /**
     * Get the Frame Number
     */
    public Integer getFrameNumber() {
        Integer frameInt = new Integer(this.myNumber);
        return frameInt;
    }

    /** 
     * Get link
     */
    public String getRequest() {
       return this.myRequest;
    }
 
    /**
     * Get a String representation of this object
     * @return a string representation
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        if (this.myNumber > 0) {
          buf.append("Frame " + this.myNumber);
        }

        return buf.toString();
    }
}
