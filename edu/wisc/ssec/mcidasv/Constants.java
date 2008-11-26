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

import java.awt.Color;

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

    /** Default size for GUI elements */
	public static final int ELEMENT_WIDTH = 90;
	public static final int GAP_RELATED = 6;
	public static final int GAP_UNRELATED = (GAP_RELATED * 2);
	public static final int ELEMENT_DOUBLE_WIDTH = ELEMENT_WIDTH * 2;
	public static final int ELEMENT_ONEHALF_WIDTH = (int)Math.round(ELEMENT_WIDTH * 1.5);
	public static final int ELEMENT_HALF_WIDTH = Math.round(ELEMENT_WIDTH / 2);
	public static final int ELEMENT_DOUBLEDOUBLE_WIDTH = (ELEMENT_DOUBLE_WIDTH * 2) + ELEMENT_WIDTH + (GAP_RELATED * 3) + 24;
	
	/** Icon locations for buttons */
	public static final String ICON_ACCEPT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/accept.png";
	public static final String ICON_CANCEL_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/cancel.png";
	public static final String ICON_EXCLAMATION_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/exclamation.png";
	public static final String ICON_INFORMATION_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/information.png";
	public static final String ICON_HELP_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/help.png";
	public static final String ICON_ADD_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/add.png";
	public static final String ICON_DELETE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/delete.png";
	public static final String ICON_CONNECT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/connect.png";
	public static final String ICON_DISCONNECT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/disconnect.png";
	public static final String ICON_UNDO_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/arrow_undo.png";
	public static final String ICON_REDO_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/arrow_redo.png";
	public static final String ICON_REFRESH_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/arrow_refresh.png";

	public static final String ICON_CANCEL = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/stop-load22.png";
	public static final String ICON_HELP = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/show-help22.png";
	public static final String ICON_REFRESH = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/view-refresh22.png";
		
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

        /** server state preference. Holds the last server/group used */
        public static final String PREF_SERVERSTATE =
            "idv.chooser.adde.serverstate";

	/** Advanced prefs for IDV, Java, and McIDAS-X. */
	public static final String PREF_LIST_ADVANCED = "Advanced";
	
	/** Advanced prefs for IDV, Java, and McIDAS-X. */
	public static final String PREF_LIST_LOCAL_ADDE = "Local Data";

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

        /** Pref for image chooser to include system servers */
        public static final String PREF_SYSTEMSERVERSIMG =
                "mcidasv.chooser.adde.image.servers.system";

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
    
    /** Preference to store what ADDE servers to show */
    public static final String PROP_SERVERS = "idv.serverstoshow";

    /** Preference to store whether to show all ADDE servers */
    public static final String PROP_SERVERS_ALL = "idv.serverstoshow.all";
    
    /** Name to store the total system memory */
    public static final String PROP_SYSMEM = "idv.sysmem";
    
    /** Maximum amount of memory 32bit JREs can address, in megabytes */
    public static final int MAX_MEMORY_32BIT = 1536;
    
    /** A particular shade of blue we are using for branding */
    public static final Color MCV_BLUE = new Color(96, 176, 224);
    public static final Color MCV_BLUE_DARK = new Color(0, 96, 255);

}
