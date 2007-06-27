package edu.wisc.ssec.mcidasv.data;


/**
 * A class to hold a frame directory, number and time
 */
public class McIdasFrameDescriptor {


    /** Frame number */
     
    public int myNumber;

    /** McIdasX link */
    public String myRequest;

    /**
     * Default constructor for unpersistence; does nothing
     *
     */
    public McIdasFrameDescriptor() {}

    /**
     * Create a descriptor from another.
     *
     * @param that   other image descriptor
     *
     */
    public McIdasFrameDescriptor(McIdasFrameDescriptor that) {
        this.myNumber    = that.myNumber;
        this.myRequest    = that.myRequest;
    }

    /**
     * Create a frame descriptor from the frame number
     *
     * @param frameNumber 
     */
    public McIdasFrameDescriptor(int frameNumber, String request) {
        this.myNumber = frameNumber;
        this.myRequest = request;
    }


    /**
     * See if this McIdasFrameDescriptor is equal to the object in
     * question
     *
     * @param o   object in question
     * @return  true if <code>o</code> is an McIdasFrameDescriptor and
     *          they area equivalent
     */
    public boolean equals(Object o) {
        if ( !(o instanceof McIdasFrameDescriptor)) {
            return false;
        }
        McIdasFrameDescriptor that = (McIdasFrameDescriptor) o;
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
