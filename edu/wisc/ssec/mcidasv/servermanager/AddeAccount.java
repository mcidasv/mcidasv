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
        if (user == null)
            throw new NullPointerException();
        if (proj == null)
            throw new NullPointerException();

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
     * @param o Object to test against.
     * 
     * @return Whether or not {@code o} is equivalent to this ADDE account.
     * 
     * @see {@link String#equals(Object)}.
     */
    @Override public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o == this)
            return true;
        if (!(o instanceof AddeAccount))
            return false;

        AddeAccount a = (AddeAccount)o;
        return a.getUsername().equals(username) && a.getProject().equals(project);
    }

    /**
     * Computes the hashcode of this ADDE account using the hashcodes of 
     * {@link #username} and {@link #project}.
     * 
     * @return A hash code value for this object.
     * 
     * @see {@link String#hashCode()}.
     */
    @Override public int hashCode() {
        int result = 17;
        result = 31 * result + username.hashCode();
        result = 31 * result + project.hashCode();
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
}