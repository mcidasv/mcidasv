package edu.wisc.ssec.mcidasv.chooser;


/**
 * Class ServerDescriptor Holds the state of
 * server components from servers.xml
 */
public class ServerDescriptor {

    /** Is the server active */
    private boolean isActive;

    /** Server name or IP address */
    private String serverName;

    /** Group name */
    private String groupName;

    /** Data type */
    private String dataType;

    /**
     * Constructor
     *
     *
     */
    public ServerDescriptor(String type, String name, String group, String active) {
        this.serverName = name;
        this.groupName = group;
        this.dataType = type;
        this.isActive = active.equals("true");
    }

    /**
     * Get the isActive property.
     *
     * @return The isActive property.
     */
    public boolean getIsActive() {
        return this.isActive;
    }

    /**
     * Get the data type
     *
     * @return The dataType property.
     */
    public String getDataType() {
        return this.dataType;
    }


    /**
     * Does this server contain data of specified type
     *
     * @return true or false.
     */
    public boolean isDataType(String type) {
        return this.dataType.equals(type);
    }

    /**
     * Get the serverName property.
     *
     * @return The serverName property.
     */
    public String getServerName() {
        return this.serverName;
    }


    /**
     * Get the groupName property.
     *
     * @return The groupName property.
     */
    public String getGroupName() {
        return this.groupName;
    }


    /**
     * Set the isActive property.
     *
     * @param newValue The new vaue for the isActive property.
     */
    public void setIsActive(boolean newValue) {
        this.isActive = newValue;
    }

    /**
     * Set the dataType property.
     *
     * @param newValue The new vaue for the dataType property.
     */
    public void setDataType(String newValue) {
        this.dataType = newValue;
    }

    /**
     * Set the serverName property.
     *
     * @param newValue The new vaue for the serverName property.
     */
    public void setServerName(String newValue) {
        this.serverName = newValue;
    }


    /**
     * Set the groupName property.
     *
     * @param newValue The new vaue for the groupName property.
     */
    public void setGroupName(String newValue) {
        this.groupName = newValue;
    }


    /**
     * Get a String representation of this object
     * @return a string representation
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(this.serverName);
        buf.append("/");
        buf.append(this.groupName);
        return buf.toString();
    }
}
