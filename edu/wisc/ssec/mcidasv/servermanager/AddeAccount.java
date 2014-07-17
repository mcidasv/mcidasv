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

package edu.wisc.ssec.mcidasv.servermanager;

/**
 * Simplistic representation of ADDE accounting information. This is an
 * immutable class.
 */
public class AddeAccount {

    /** Username to hand off to the server. */
    private final String username;

    /** Project number (currently not limited to a numeric value). */
    private final String project;

    /** 
     * Builds a new ADDE account object.
     * 
     * @param user Username to store. Cannot be {@code null}.
     * @param proj Project number to store. Cannot be {@code null}.
     * 
     * @throws NullPointerException if {@code user} or {@code proj} is
     * {@code null}.
     */
    public AddeAccount(final String user, final String proj) {
        if (user == null) {
            throw new NullPointerException("user cannot be null");
        }
        if (proj == null) {
            throw new NullPointerException("proj cannot be null");
        }
        username = user;
        project = proj;
    }

    /**
     * Get the username associated with this account.
     * 
     * @return {@link #username}
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the project number associated with this account.
     * 
     * @return {@link #project}
     */
    public String getProject() {
        return project;
    }

    /**
     * Determines whether or not a given object is equivalent to this ADDE
     * account. Currently the username and project number <b>are</b> case
     * sensitive, though this is likely to change.
     * 
     * @param obj Object to test against.
     * 
     * @return Whether or not {@code obj} is equivalent to this ADDE account.
     */
    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AddeAccount)) {
            return false;
        }
        AddeAccount other = (AddeAccount) obj;
        if (!project.equals(other.project)) {
            return false;
        }
        if (!username.equals(other.username)) {
            return false;
        }
        return true;
    }

    /**
     * Computes the hashcode of this ADDE account using the hashcodes of 
     * {@link #username} and {@link #project}.
     * 
     * @return A hash code value for this object.
     */
    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + project.hashCode();
        result = prime * result + username.hashCode();
        return result;
    }

    /**
     * Returns a string representation of this account. The formatting of
     * this string is subject to change, but currently looks like:<br/>
     * <pre>[AddeAccount@HASHCODE: username=..., project=...]</pre>
     * 
     * @return {@link String} representation of this ADDE account.
     */
    public String toString() {
        return String.format("[AddeAccount@%x: username=%s, project=%s]", hashCode(), username, project);
    }

    /**
     * Returns a {@literal "human-friendly"} representation of this accounting
     * object. Currently looks like {@code USER / PROJ}.
     * 
     * @return Friendly accounting detail {@code String}.
     */
    public String friendlyString() {
        return username+" / "+project;
    }
}
