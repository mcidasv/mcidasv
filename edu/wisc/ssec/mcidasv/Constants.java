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

package edu.wisc.ssec.mcidasv;

import java.awt.Color;

import ucar.unidata.idv.IdvConstants;
import ucar.unidata.util.PatternFileFilter;

/**
 * Application wide constants.
 */
public interface Constants extends IdvConstants {

	/** Path to a skin that creates a window with an empty comp group. */
	public static final String BLANK_COMP_GROUP = 
		"/edu/wisc/ssec/mcidasv/resources/skins/window/comptest.xml";

	/**
	 * The name of a thing that contains the data choosers and
	 * field selector
	 */
	public static final String DATASELECTOR_NAME = "McIDAS-V - Data Explorer";

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

	/** Macro for the VisAD version in the about HTML file. */
	public static String MACRO_VISAD_VERSION = "%VISADVERSION%";

    /** Default size for GUI elements */
	public static final int ELEMENT_WIDTH = 90;
	public static final int GAP_RELATED = 6;
	public static final int GAP_UNRELATED = (GAP_RELATED * 2);
	public static final int ELEMENT_DOUBLE_WIDTH = ELEMENT_WIDTH * 2;
	public static final int ELEMENT_ONEHALF_WIDTH = (int)Math.round(ELEMENT_WIDTH * 1.5);
	public static final int ELEMENT_HALF_WIDTH = Math.round(ELEMENT_WIDTH / 2);
	public static final int ELEMENT_DOUBLEDOUBLE_WIDTH = (ELEMENT_DOUBLE_WIDTH * 2) + ELEMENT_WIDTH + (GAP_RELATED * 3) + 24;
	
	/** Icon locations for buttons */
	public static final String ICON_APPLY_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/action_go.gif";
	public static final String ICON_ACCEPT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/accept.png";
	public static final String ICON_CANCEL_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/stop-loads16.png";
	public static final String ICON_EXCLAMATION_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/exclamation.png";
	public static final String ICON_INFORMATION_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/information.png";
	public static final String ICON_ERROR_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/error.png";
	public static final String ICON_HELP_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/show-help16.png";
	public static final String ICON_ADD_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/add.png";
	public static final String ICON_DELETE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/delete.png";
	public static final String ICON_CONNECT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/connect.png";
	public static final String ICON_DISCONNECT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/disconnect.png";
	public static final String ICON_UNDO_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/arrow_undo.png";
	public static final String ICON_REDO_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/arrow_redo.png";
	public static final String ICON_REFRESH_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/view-refresh16.png";
	public static final String ICON_OPEN_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/document-open16.png";
	public static final String ICON_SAVE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/save-as-fave-bundle16.png";
	public static final String ICON_SAVEAS_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/document-save16.png";
	public static final String ICON_PREFERENCES_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/preferences-system16.png";
	public static final String ICON_NEWWINDOW_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/application_add.png";
	public static final String ICON_NEWTAB_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/tab_add.png";
	public static final String ICON_NEXT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/resultset_next.png";
	public static final String ICON_PREVIOUS_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/resultset_previous.png";
	public static final String ICON_RANDOM_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/view-refresh16.png";
	public static final String ICON_HELPTIPS_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/dialog-information16.png";
	public static final String ICON_CONSOLE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/utilities-system-monitor16.png";
	public static final String ICON_CHECKVERSION_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/preferences-desktop-multimedia16.png";
	public static final String ICON_FORUMS_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/report_go.png";
	public static final String ICON_SUPPORT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/email_go.png";
	public static final String ICON_DATAEXPLORER_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/mcidasv-round16.png";
	public static final String ICON_LOCALDATA_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/show-data16.png";
	public static final String ICON_COLORTABLE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/x-office-presentation16.png";
	public static final String ICON_LAYOUTEDIT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/accessories-text-editor16.png";
	public static final String ICON_RANGEANDBEARING_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/network-wireless16.png";
	public static final String ICON_LOCATION_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/start-here16.png";
	public static final String ICON_BACKGROUND_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/background-image16.png";
	public static final String ICON_USERSGUIDE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/book_open.png";
	public static final String ICON_GETTINGSTARTED_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/book_next.png";
	public static final String ICON_NOTE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/note.png";
	public static final String ICON_MCIDASV_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/mcidasv-round16.png";
	
	public static final String ICON_DEFAULTLAYOUT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/key.png";
	public static final String ICON_DEFAULTLAYOUTADD_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/key_add.png";
	public static final String ICON_DEFAULTLAYOUTDELETE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/key_delete.png";
	
	public static final String ICON_REMOVE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/edit-cut16.png";
	public static final String ICON_REMOVELAYERS_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/remove-layers16.png";
	public static final String ICON_REMOVEDATA_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/remove-data16.png";
	public static final String ICON_REMOVELAYERSDATA_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/remove-layers-data16.png";
	
	public static final String ICON_FAVORITE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/fave-bundle16.png";
	public static final String ICON_FAVORITESAVE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/save-as-fave-bundle16.png";
	public static final String ICON_FAVORITEMANAGE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/manage-favorite16.png";

	public static final String ICON_CANCEL = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/stop-load22.png";
	public static final String ICON_HELP = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/show-help22.png";
	public static final String ICON_REFRESH = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/view-refresh22.png";
	public static final String ICON_UPDATE = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/system-software-update22.png";
	public static final String ICON_OPEN = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/document-open22.png";
	public static final String ICON_SAVE = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/document-save22.png";
	
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

    /** Server state preference ID prefix. Holds the last server/group used. */
    public static final String PREF_SERVERSTATE = "idv.chooser.adde.serverstate";

    /** The server/group to use if there is no value associated with {@link #PREF_SERVERSTATE}. */
    public static final String[] DEFAULT_SERVERSTATE = new String[] { "adde.ucar.edu", "RTIMAGES" };

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

	/** The name of the prerelease check user preference. */
	public static final String PREF_PRERELEASE_CHECK = "mcidasv.doprereleasecheck";

	/** Name of the {@literal "remove all data warning"} preference. */
	public static final String PREF_CONFIRM_REMOVE_DATA = 
	    "mcv.warn.remove.data.all";

	/** Name of the {@literal "remove all layers warning"} preference. */
	public static final String PREF_CONFIRM_REMOVE_LAYERS = 
	    "mcv.warn.remove.layers.all";

	/** Name of the {@literal "remove everything warning"} preference. */
	public static final String PREF_CONFIRM_REMOVE_BOTH = 
	    "mcv.warn.remove.everything";

    /** Preference for controlling the automated saving of the default layout. */
    public static final String PREF_AUTO_SAVE_DEFAULT_LAYOUT = "mcidasv.defaultlayout.autosave";

    public static final String PREF_SAVE_DASHBOARD_VIZ = "mcidasv.dashboard.savevisibility";

    /** Preference for saving image preview default */
    public static final String PREF_IMAGE_PREVIEW = "mcidasv.chooser.adde.preview";
    
	/** 
	 * Show large or small icons. If PREF_TBM_ICONS is disabled, this pref
	 * has no meaning.
	 */
	public static final String PREF_TBM_SIZE = "tbm.icon.size";

	/** Property name for for the path to about dialog template. */
	public static String PROP_ABOUTTEXT = "mcidasv.about.text";

	/** Path to the main McIDAS-V icon. */
	public static final String PROP_APP_ICON = "mcidasv.window.icon";

	/** WHen was visad.jar built? */
	public static String PROP_VISAD_DATE = "visad.build.date";

	/** What version of VisAD lives within visad.jar? */
	public static String PROP_VISAD_REVISION = "visad.build.revision";

	/** Was there a problem determing VisAD's version? */
	public static String PROP_VISAD_PARSE_FAIL = "visad.build.parsefail";

	/** What exactly broke the version extraction? */
	public static String PROP_VISAD_ORIGINAL = "visad.build.contents";

	/** When was McIDAS-V built? */
	public static String PROP_BUILD_DATE = "mcidasv.build.date";

	/** Property name for the copyright year. */
	public static String PROP_COPYRIGHT_YEAR = "mcidasv.copyright.year";

	/** Property name for the McIdas-V homepage URL. */
	public static String PROP_HOMEPAGE = "mcidasv.homepage";

	/** Specifies use of {@code edu.wisc.ssec.mcidasv.ui.TabbedUIManager}. */
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

	/** Property used to restore the size and position of the dashboard upon start. */
	public static final String PROP_DASHBOARD_BOUNDS = "mcidasv.dashboard.bounds";

	/** Application property file name. */
	public static final String PROPERTIES_FILE = 
		"/edu/wisc/ssec/mcidasv/resources/mcidasv.properties";

	/** McIDAS-V webpage base url */
	public static final String HOMEPAGE_URL = "http://www.ssec.wisc.edu/mcidas/software/v";

	/** Location of latest version file under base url */
	public static final String VERSION_URL = "stable/version.txt";
	public static final String VERSION_HANDLER_URL = "stable/version.php";

	/** Location of latest prerelease directory under base url */
	public static final String PRERELEASE_URL = "prerelease/";

	/** Location of latest notice file under base url */
	public static final String NOTICE_URL = "stable/notice.txt";

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
        new PatternFileFilter("(.+\\.mcvz$)", "McIDAS-V Zipped Data Bundles (*.mcvz)", SUFFIX_MCVZ);

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

    /** Identifier for the {@literal "monitor panel"} window component. */
    public static final String COMP_MONITORPANEL = "mcv.monitorpanel";
    
    /** Default user directory name */
    public static final String USER_DIRECTORY_NAME = "McIDAS-V";
}
