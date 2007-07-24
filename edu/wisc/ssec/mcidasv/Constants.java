package edu.wisc.ssec.mcidasv;

/**
 * Application wide constants.
 * @version $Id$
 */
public interface Constants {

	/** Application property file name. */
	public static final String PROPERTIES_FILE = "/edu/wisc/ssec/mcidasv/resources/mcidasv.properties";
	
	/** Specifies use of the {@link edu.wisc.ssec.mcidasv.ui.TabbedUIManager}. */
	public static final String PROP_TABBED_UI = "mcidasv.tabbedDisplay";
	
	/** Property name for for the path to about dialog template. */
	public static String PROP_ABOUTTEXT = "mcidasv.about.text";
	
	/** Property name for the path to version file. */
	public static String PROP_VERSIONFILE = "mcidasv.version.file";
	/** Property name for the major version number. */
	public static String PROP_VERSION_MAJOR = "mcidasv.version.major";
	/** Property name for the minor version number. */
	public static String PROP_VERSION_MINOR = "mcidasv.version.minor";
	/** Property name for the version release number. */
	public static String PROP_VERSION_RELEASE = "mcidasv.version.release";
	
	/** Macro for the version in the about HTML file. */
	public static String MACRO_VERSION = "%MCVERSION%";
	
	
}
