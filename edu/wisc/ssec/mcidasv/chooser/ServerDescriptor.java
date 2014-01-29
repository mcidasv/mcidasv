/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

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
