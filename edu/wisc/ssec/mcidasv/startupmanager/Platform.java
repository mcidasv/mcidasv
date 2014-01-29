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
package edu.wisc.ssec.mcidasv.startupmanager;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster;

public enum Platform {
    /** Instance of unix-specific platform information. */
    UNIXLIKE("/", "runMcV.prefs", "\n"),
    
    /** Instance of windows-specific platform information. */
    WINDOWS("\\", "runMcV-Prefs.bat", "\r\n");
    
    /** Path to the user's {@literal "userpath"} directory. */
    private String userDirectory;
    
    /** The path to the user's copy of the startup preferences. */
    private String userPrefs;
    
    /** Path to the preference file that ships with McIDAS-V. */
    private final String defaultPrefs;
    
    /** Holds the platform's representation of a new line. */
    private final String newLine;
    
    /** Directory delimiter for the current platform. */
    private final String pathSeparator;
    
    /** Total amount of memory avilable in megabytes */
    private int availableMemory = 0;
    
    /**
     * Initializes the platform-specific paths to the different files 
     * required by the startup manager.
     * 
     * @param pathSeparator Character that delimits directories. On Windows
     * this will be {@literal \\}, while on Unix-style systems, it will be
     * {@literal /}.
     * @param defaultPrefs The path to the preferences file that ships with
     * McIDAS-V.
     * @param newLine Character(s!) that represent a new line for this 
     * platform.
     * 
     * @throws NullPointerException if either {@code pathSeparator} or 
     * {@code defaultPrefs} are null.
     * 
     * @throws IllegalArgumentException if either {@code pathSeparator} or
     * {@code defaultPrefs} are an empty string.
     */
    Platform(final String pathSeparator, final String defaultPrefs, 
        final String newLine) 
    {
        if (pathSeparator == null || defaultPrefs == null) {
            throw new NullPointerException("");
        }
        if (pathSeparator.isEmpty() || defaultPrefs.isEmpty()) {
            throw new IllegalArgumentException("");
        }
        
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Mac OS X")) {
            this.userDirectory = System.getProperty("user.home") + pathSeparator + "Documents" + pathSeparator + Constants.USER_DIRECTORY_NAME;
        } else {
            this.userDirectory = System.getProperty("user.home") + pathSeparator + Constants.USER_DIRECTORY_NAME;
        }
        this.userPrefs = userDirectory + pathSeparator + defaultPrefs;
        this.defaultPrefs = defaultPrefs;
        this.newLine = newLine;
        this.pathSeparator = pathSeparator;
    }
    
    /**
     * Sets the path to the user's {@literal "userpath"} directory explicitly.
     * If the specified path does not yet exist, this method will first
     * attempt to create it. The method will then attempt to verify whether or
     * not McIDAS-V can use the path.
     * 
     * @param path New path. Cannot be {@code null}, but does not have to exist
     * prior to running this method. Be aware that this method will attempt to
     * create {@code path} if it does not already exist.
     *
     * @throws IllegalArgumentException if {@code path} is not a
     * directory, or if it not both readable and writable.
     */
    public void setUserDirectory(final String path) throws IllegalArgumentException {
        File tmp = new File(path);
        if (!tmp.exists()) {
            tmp.mkdir();
        }

        // TODO(jon): or would tmp.isFile() suffice?
        if (tmp.exists() && !tmp.isDirectory()) {
            throw new IllegalArgumentException("'"+path+"' is not a directory.");
        }

        boolean canRead = tmp.canRead();
        boolean canWrite = tmp.canWrite();

        if (!canRead && !canWrite) {
            throw new IllegalArgumentException("'"+path+"' must be both readable and writable by McIDAS-V.");
        } else if (!canRead) {
            throw new IllegalArgumentException("'"+path+"' must be readable by McIDAS-V.");
        } else if (!canWrite) {
            throw new IllegalArgumentException("'"+path+"' must be writable by McIDAS-V.");
        }

        userDirectory = path;
        userPrefs = userDirectory + pathSeparator + defaultPrefs;
    }
    
    /**
     * Sets the amount of available memory. {@code megabytes} must be 
     * greater than or equal to zero.
     * 
     * @param megabytes Memory in megabytes
     * 
     * @throws NullPointerException if {@code megabytes} is {@code null}.
     * @throws IllegalArgumentException if {@code megabytes} is less than
     * zero or does not represent an integer.
     * 
     * @see StartupManager#getArgs
     */
    public void setAvailableMemory(String megabytes) {
        if (megabytes == null) {
            throw new NullPointerException("Available memory cannot be null");
        }
        if (megabytes.isEmpty()) {
            megabytes = "0";
        }
        
        try {
            int test = Integer.parseInt(megabytes);
            if (test < 0) {
                throw new IllegalArgumentException("Available memory must be a non-negative integer, not \""+megabytes+"\"");
            }
            availableMemory = test;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Could not convert \""+megabytes+"\" to a non-negative integer");
        }
    }
    
    /**
     * Returns the path to the user's {@literal "userpath"} directory.
     * 
     * @return Path to the user's directory.
     */
    public String getUserDirectory() {
        return userDirectory;
    }
    
    /**
     * Returns the path to a file in the user's {@literal "userpath"} directory.
     * 
     * @param filename Filename within the {@code userpath}. Cannot be 
     * {@code null}, but does not need to be a filename that already exists 
     * within the {@code userpath}.
     * 
     * @return Path to a file in the user's directory. <b>Note:</b> the file 
     * may not yet exist.
     */
    public String getUserFile(String filename) {
        return getUserDirectory() + pathSeparator + filename;
    }
    
    public String getUserBundles() {
        return getUserDirectory() + pathSeparator + "bundles";
    }
    
    /**
     * Returns the amount of available memory in megabytes
     * 
     * @return Available memory in megabytes
     */
    public int getAvailableMemory() {
        return availableMemory;
    }
    
    /**
     * Returns the path of user's copy of the startup preferences.
     * 
     * @return Path to the user's startup preferences file.
     */
    public String getUserPrefs() {
        return userPrefs;
    }
    
    /**
     * Returns the path of the startup preferences included in the McIDAS-V
     * distribution. Mostly useful for normalizing the user 
     * directory.
     * 
     * @return Path to the default startup preferences.
     * 
     * @see OptionMaster#normalizeUserDirectory()
     */
    public String getDefaultPrefs() {
        return defaultPrefs;
    }
    
    /**
     * Returns the platform's notion of a new line.
     * 
     * @return Unix-like: {@literal \n}; Windows: {@literal \r\n}.
     */
    public String getNewLine() {
        return newLine;
    }
    
    /**
     * Returns a brief summary of the platform specific file locations. 
     * Please note that the format and contents are subject to change.
     * 
     * @return String that looks like 
     * {@code [Platform@HASHCODE: defaultPrefs=..., userDirectory=..., 
     * userPrefs=...]}
     */
    @Override public String toString() {
        return String.format(
            "[Platform@%x: defaultPrefs=%s, userDirectory=%s, userPrefs=%s]",
            hashCode(), defaultPrefs, userDirectory, userPrefs);
    }
}
