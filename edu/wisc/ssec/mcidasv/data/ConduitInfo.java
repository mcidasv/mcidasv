package edu.wisc.ssec.mcidasv.data;


/**
 * Class ConduitInfo Holds the state of
 * the McIdas-X bridge
 */
public class ConduitInfo {


    /** Update status of each component */
    private boolean updateImage = false;
    private boolean updateGraphics = false;
    private boolean updateColorTable = false;

    /** Conduit protocol info */
    private String hostString = " ";
    private String portString = "8080";
    private String keyString = "00000000000000000000000000000000";

    private String request= "http://" + hostString  + ":" + portString + "/?sessionkey=" + keyString +
                    "&version=2&frame=0&x=0&y=0&type=";

    /**
     * Constructor
     *
     *
     */
    public ConduitInfo() {}



    /**
     * Copy constructor
     *
     * @param that The ConduitInfo to copy
     *
     */
    public ConduitInfo(ConduitInfo that) {
        this.hostString = that.hostString;
        this.portString = that.portString;
        this.keyString  = that.keyString;
    }



    /**
     * Constructor
     */
    public ConduitInfo(String host, String port, String key) {
        this.hostString = host;
        this.portString = port;
        this.keyString  = key;
    }


    /**
     * Get the hostString property.
     *
     * @return The hostString property.
     */
    public String getHostString() {
        return this.hostString;
    }


    /**
     * Get the portString property.
     *
     * @return The portString property.
     */
    public String getPortString() {
        return this.portString;
    }


    /**
     * Get the keyString property.
     *
     * @return The keyString property.
     */
    public String getKeyString() {
        return this.keyString;
    }


    /**
     * Set the hostString property.
     *
     * @param newValue The new vaue for the hostString property.
     */
    public void setHostString(String newValue) {
        this.hostString = newValue;
        setRequest();
    }


    /**
     * Set the portString property.
     *
     * @param newValue The new vaue for the portString property.
     */
    public void setPortString(String newValue) {
        this.portString = newValue;
        setRequest();
    }


    /**
     * Set the keyString property.
     *
     * @param newValue The new vaue for the keyString property.
     */
    public void setKeyString(String newValue) {
        this.keyString = newValue;
        setRequest();
    }

    /**
     * Set the request property.
     *
     * @param newValue The new vaue for the request property.
     */
    public void setRequest() {
        this.request = "http://" + hostString  + ":" + portString + "/?sessionkey=" + keyString +
                    "&version=2&frame=0&x=0&y=0&type=";
    }

    /**
     * Get the request property.
     *
     * @return The request property. 
     */
    public String getRequest() {
        return this.request;
    }


    /**
     * Get a String representation of this object
     * @return a string representation
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("hostString: ");
        buf.append(this.hostString);
        buf.append("  portString: ");
        buf.append(this.portString);
        buf.append("  keyString: ");
        buf.append(this.keyString);
        return buf.toString();
    }

}
