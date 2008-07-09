/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv;

import ucar.unidata.idv.IdvConstants;
import ucar.unidata.util.PatternFileFilter;

/**
 * Application wide constants.
 * @version $Id$
 */
public interface Constants extends IdvConstants {

	/** Path to a skin that creates a window with an empty comp group. */
	public static final String BLANK_COMP_GROUP = 
		"/edu/wisc/ssec/mcidasv/resources/skins/window/comptest.xml";

	/**
	 * The name of a thing that contains the data choosers and
	 * field selector
	 */
	public static final String DATASELECTOR_NAME = "Data Explorer";

	/**
	 * A thing that contains one or more of the things named
	 * <tt>PANEL_NAME</tt>. One of these can be either in a tab
	 * or in it's own window.
	 */
	public static final String DISPLAY_NAME = "Display";

	/**
	 * The name of a thing that contains the display/layer controls
	 */
	public static final String DISPLAYCONTROLLER_NAME = "Display Controller";
	/** Macro for the build date. */
	public static String MACRO_BUILDDATE = "%BUILDDATE%";
	/** Macro for the copyright year in the about HTML file. */
	public static String MACRO_COPYRIGHT_YEAR = "%COPYRIGHT_YEAR%";
	/** Macro for the IDV version in the about HTML file. */
	public static String MACRO_IDV_VERSION = "%IDVVERSION%";
	/** Macro for the version in the about HTML file. */
	public static String MACRO_VERSION = "%MCVERSION%";

	/** 
	 * Java OS descriptor for the Max OSX operating system. This should be 
	 * constant for any machine running java on OSX.
	 */
	public static final String OS_OSX = "Mac OS X";
	/**
	 * The name of thing that contains the actual VisAD display,
	 * the animation control, view and projection menus, and the
	 * toolbar.
	 */
	public static final String PANEL_NAME = "Panel";

	/** Gail's server preference manager. */
	public static final String PREF_LIST_ADDE_SERVERS = "ADDE Servers";

	/** Advanced prefs for IDV, Java, and McIDAS-X. */
	public static final String PREF_LIST_ADVANCED = "Advanced";

	/** Prefs for which display types to allow. */
	public static final String PREF_LIST_AVAILABLE_DISPLAYS = "Available Displays";

	/** Prefs for which data choosers should show up. */
	public static final String PREF_LIST_DATA_CHOOSERS = "Data Sources";

	/** Name of panel containing prefs related to data formatting. */
	public static final String PREF_LIST_FORMATS_DATA = "Formats & Data";

	/** Name of the panel that holds the "general" sorts of user prefs. */
	public static final String PREF_LIST_GENERAL = "General";

	/** Panel name for the different nav control scheme prefs. */
	public static final String PREF_LIST_NAV_CONTROLS = "Navigation Controls";

	/** Prefs for configuring what appears in the toolbar. */
	public static final String PREF_LIST_TOOLBAR = "Toolbar Options";

	/** Name of the different prefs for configuring how tabs/windows look. */
	public static final String PREF_LIST_VIEW = "Display Window";

	/** Pref ID for limiting # of new windows when loading bundles. */
	public static final String PREF_OPEN_LIMIT_WIN = "mcv.open.limitwin";

	/** The name of the version check user preference. */
	public static final String PREF_VERSION_CHECK = "mcidasv.doversioncheck";

	/** 
	 * Show large or small icons. If PREF_TBM_ICONS is disabled, this pref
	 * has no meaning.
	 */
	public static final String PREF_TBM_SIZE = "tbm.icon.size";

	/** Property name for for the path to about dialog template. */
	public static String PROP_ABOUTTEXT = "mcidasv.about.text";

	/** Path to the main McIDAS-V icon. */
	public static final String PROP_APP_ICON = "mcidasv.window.icon";

	/** When was McIDAS-V built? */
	public static String PROP_BUILD_DATE = "mcidasv.build.date";

	/** Property name for the copyright year. */
	public static String PROP_COPYRIGHT_YEAR = "mcidasv.copyright.year";

	/** Property name for the McIdas-V homepage URL. */
	public static String PROP_HOMEPAGE = "mcidasv.homepage";

	/** Specifies use of {@link edu.wisc.ssec.mcidasv.ui.TabbedUIManager}. */
	public static final String PROP_TABBED_UI = "mcidasv.tabbedDisplay";

	/** Property name for the major version number. */
	public static String PROP_VERSION_MAJOR = "mcidasv.version.major";

	/** Property name for the minor version number. */
	public static String PROP_VERSION_MINOR = "mcidasv.version.minor";

	/** Property name for the version release number. */
	public static String PROP_VERSION_RELEASE = "mcidasv.version.release";

	/** Property name for the path to version file. */
	public static String PROP_VERSIONFILE = "mcidasv.version.file";

	/** Property that determines whether the view panel should pop up. */
	public static final String PROP_VP_SHOWPOPUP = 
		"idv.ui.viewpanel.showpopup";

	/** Property for whether view panel categories will be shown. */
	public static final String PROP_VP_SHOWCATS = 
		"idv.ui.viewpanel.showcategories";

	/** typo was found in IDV code. */
	public static final String PROP_VP_CATOPEN = "viewpanel.catgegory.open";

	/** Application property file name. */
	public static final String PROPERTIES_FILE = 
		"/edu/wisc/ssec/mcidasv/resources/mcidasv.properties";

	/** McIDAS-V webpage base url */
	public static final String HOMEPAGE_URL = "http://www.ssec.wisc.edu/mcidas/software/v";

	/** Location of latest version file under base url */
	public static final String VERSION_URL = "stable/version.txt";

	public static final String SCRUB_STRINGS_FILE = "/edu/wisc/ssec/mcidasv/resources/scrubstrings.xml";

    /** Where to look for javahelp */
    public static final String DEFAULT_DOCPATH = "/docs/userguide";
        
    /** File suffix for bundle files */
    public static final String SUFFIX_MCV = ".mcv";

    /** File suffix for compressed bundle files */
    public static final String SUFFIX_MCVZ = ".mcvz";

    /** File filter used for bundle files */
    public static final PatternFileFilter FILTER_MCV =
        new PatternFileFilter("(.+\\.mcv$)", "McIDAS-V Bundles (*.mcv)", SUFFIX_MCV);
    
    /** File filter used for bundle files */
    public static final PatternFileFilter FILTER_MCVZ =
        new PatternFileFilter("(.+\\.mcvz$)", "Zipped McIDAS-V Bundles (*.mcvz)", SUFFIX_MCVZ);

    /** File filter used for bundle files */
    public static final PatternFileFilter FILTER_MCVMCVZ =
        new PatternFileFilter("(.+\\.mcv$|.+\\.mcvz$)", "All McIDAS-V Bundles (*.mcv,*.mcvz)", SUFFIX_MCV);
    
    /** Default port for local ADDE servers */
    public static final String LOCAL_ADDE_PORT = "8112";
    
    public static final String PROP_CHAN = "selectedchannel";

}