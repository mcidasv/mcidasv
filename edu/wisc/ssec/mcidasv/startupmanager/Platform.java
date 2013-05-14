package edu.wisc.ssec.mcidasv.startupmanager;

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
     * Sets the path to the user's userpath directory explicitly.
     * 
     * @param path New path.
     */
    public void setUserDirectory(final String path) {
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
     * @see StartupManager#getArgs(String[], Properties)
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
     * Returns the path to a file in the user's {@literal "userpath"} directory
     * 
     * @param filename
     * @return Path to a file in the user's directory.
     */
    public String getUserFile(String filename) {
        return getUserDirectory()+pathSeparator+filename;
    }
    
    public String getUserBundles() {
        return getUserDirectory()+pathSeparator+"bundles";
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
