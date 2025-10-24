/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.mcidasv;

import java.awt.Color;
import java.awt.Dimension;

import ucar.unidata.idv.IdvConstants;
import ucar.unidata.util.PatternFileFilter;

/**
 * Application wide constants.
 */
public interface Constants extends IdvConstants {

    /** Path to a skin that creates a window with an empty comp group. */
    String BLANK_COMP_GROUP =
        "/edu/wisc/ssec/mcidasv/resources/skins/window/comptest.xml";

    /** Name of a thing that contains the data choosers and field selector. */
    String DATASELECTOR_NAME = "McIDAS-V - Data Explorer";

    /**
     * A thing that contains one or more of the things named
     * {@code PANEL_NAME}. One of these can be either in a tab
     * or in it's own window.
     */
    String DISPLAY_NAME = "Display";

    /** Name of a thing that contains the display/layer controls. */
    String DISPLAYCONTROLLER_NAME = "Display Controller";

    /** Macro for the build date. */
    String MACRO_BUILDDATE = "%BUILDDATE%";

    /** Macro for the copyright year in the about HTML file. */
    String MACRO_COPYRIGHT_YEAR = "%COPYRIGHT_YEAR%";

    /** Macro for the IDV version in the about HTML file. */
    String MACRO_IDV_VERSION = "%IDVVERSION%";

    /** Macro for the version in the about HTML file. */
    String MACRO_VERSION = "%MCVERSION%";

    /** Macro for the VisAD version in the about HTML file. */
    String MACRO_VISAD_VERSION = "%VISADVERSION%";

    /** Macro for granule count (only applies for swath data). */
    String MACRO_GRANULE_COUNT = "%granulecount%";

    /** Default size for GUI elements. */
    int ELEMENT_WIDTH = 90;
    int GAP_RELATED = 6;
    int GAP_UNRELATED = (GAP_RELATED * 2);
    int ELEMENT_DOUBLE_WIDTH = ELEMENT_WIDTH * 2;
    int ELEMENT_ONEHALF_WIDTH = (int)Math.round(ELEMENT_WIDTH * 1.5);
    int ELEMENT_HALF_WIDTH = Math.round(ELEMENT_WIDTH / 2);
    int ELEMENT_DOUBLEDOUBLE_WIDTH = (ELEMENT_DOUBLE_WIDTH * 2) + ELEMENT_WIDTH + (GAP_RELATED * 3) + 24;
    
    /**
     * Common line styles used in various UI controls
     */
    
    String [] lineStyles = new String[] { "_____", "_ _ _", ".....", "_._._" };

    /** Icon locations for buttons. */
    String ICON_APPLY_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/action_go.gif";
    String ICON_ACCEPT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/accept.png";
    String ICON_CANCEL_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/stop-loads16.png";
    String ICON_EXCLAMATION_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/exclamation.png";
    String ICON_INFORMATION_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/information.png";
    String ICON_ERROR_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/error.png";
    String ICON_HELP_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/show-help16.png";
    String ICON_ADD_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/add.png";
    String ICON_DELETE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/delete.png";
    String ICON_CONNECT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/connect.png";
    String ICON_DISCONNECT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/disconnect.png";
    String ICON_UNDO_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/arrow_undo.png";
    String ICON_REDO_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/arrow_redo.png";
    String ICON_REFRESH_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/view-refresh16.png";
    String ICON_OPEN_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/document-open16.png";
    String ICON_SAVE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/save-as-fave-bundle16.png";
    String ICON_SAVEAS_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/document-save16.png";
    String ICON_PREFERENCES_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/preferences-system16.png";
    String ICON_NEWWINDOW_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/application_add.png";
    String ICON_NEWTAB_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/tab_add.png";
    String ICON_NEXT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/resultset_next.png";
    String ICON_PREVIOUS_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/resultset_previous.png";
    String ICON_RANDOM_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/view-refresh16.png";
    String ICON_HELPTIPS_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/dialog-information16.png";
    String ICON_CONSOLE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/utilities-system-monitor16.png";
    String ICON_CHECKVERSION_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/preferences-desktop-multimedia16.png";
    String ICON_FORUMS_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/report_go.png";
    String ICON_SUPPORT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/email_go.png";
    String ICON_DATAEXPLORER_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/mcidasv-round16.png";
    String ICON_LOCALDATA_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/show-data16.png";
    String ICON_COLORTABLE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/x-office-presentation16.png";
    String ICON_LAYOUTEDIT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/accessories-text-editor16.png";
    String ICON_RANGEANDBEARING_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/network-wireless16.png";
    String ICON_LOCATION_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/start-here16.png";
    String ICON_BACKGROUND_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/background-image16.png";
    String ICON_USERSGUIDE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/book_open.png";
    String ICON_GETTINGSTARTED_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/book_next.png";
    String ICON_NOTE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/note.png";
    String ICON_MCIDASV_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/mcidasv-round16.png";
    String ICON_MCIDASV_DEFAULT = "/edu/wisc/ssec/mcidasv/resources/icons/prefs/mcidasv-default-logo.png";
    
    String ICON_DEFAULTLAYOUT_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/key.png";
    String ICON_DEFAULTLAYOUTADD_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/key_add.png";
    String ICON_DEFAULTLAYOUTDELETE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/key_delete.png";
    
    String ICON_REMOVE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/edit-cut16.png";
    String ICON_REMOVELAYERS_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/remove-layers16.png";
    String ICON_REMOVEDATA_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/remove-data16.png";
    String ICON_REMOVELAYERSDATA_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/remove-layers-data16.png";
    
    String ICON_FAVORITE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/fave-bundle16.png";
    String ICON_FAVORITESAVE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/save-as-fave-bundle16.png";
    String ICON_FAVORITEMANAGE_SMALL = "/edu/wisc/ssec/mcidasv/resources/icons/buttons/manage-favorite16.png";

    String ICON_CANCEL = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/stop-load22.png";
    String ICON_HELP = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/show-help22.png";
    String ICON_REFRESH = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/view-refresh22.png";
    String ICON_UPDATE = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/system-software-update22.png";
    String ICON_OPEN = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/document-open22.png";
    String ICON_SAVE = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/document-save22.png";
    
    /**
     * Java OS descriptor for the Max OSX operating system. This should be
     * constant for any machine running java on OSX.
     */
    String OS_OSX = "Mac OS X";

    /**
     * Name of thing that contains the actual VisAD display, the animation
     * control, view and projection menus, and the toolbar.
     */
    String PANEL_NAME = "Panel";

    /** For the "do not show again" option when dialog is shown warning users of this state */
    String PREF_RELATIVE_TIME_BUNDLE = "Bundle contains relative time ADDE requests";

    /** Server preference manager. */
    String PREF_LIST_ADDE_SERVERS = "ADDE Servers";

    /** Server state preference ID prefix. Holds the last server/group used. */
    String PREF_SERVERSTATE = "idv.chooser.adde.serverstate";

    /**
     * The server/group to use if there is no value associated with
     * {@link #PREF_SERVERSTATE}.
     */
    String[] DEFAULT_SERVERSTATE = new String[] { "adde.ucar.edu", "RTIMAGES" };

    /** Advanced preferences for IDV, Java, and McIDAS-X. */
    String PREF_LIST_ADVANCED = "Advanced";

    /** Advanced preferences for IDV, Java, and McIDAS-X. */
    String PREF_LIST_LOCAL_ADDE = "Local Data";

    /** Preferences for which display types to allow. */
    String PREF_LIST_AVAILABLE_DISPLAYS = "Available Displays";

    /** Preferences for which data choosers should show up. */
    String PREF_LIST_DATA_CHOOSERS = "Data Sources";

    /** Name of panel containing preferences related to data formatting. */
    String PREF_LIST_FORMATS_DATA = "Formats & Data";

    /** Name of panel that holds the "general" sorts of user preferences. */
    String PREF_LIST_GENERAL = "General";

    /** Panel name for the different nav control scheme preferences. */
    String PREF_LIST_NAV_CONTROLS = "Navigation Controls";

    /** Pref for image chooser to include system servers. */
    String PREF_SYSTEMSERVERSIMG =
        "mcidasv.chooser.adde.image.servers.system";

    /** Preferences for configuring what appears in the toolbar. */
    String PREF_LIST_TOOLBAR = "Toolbar Options";

    /** Name of different preferences for configuring how tabs/windows look. */
    String PREF_LIST_VIEW = "Display Window";

    /** Preference ID for limiting # of new windows when loading bundles. */
    String PREF_OPEN_LIMIT_WIN = "mcv.open.limitwin";

    /** Name of the version check user preference. */
    String PREF_VERSION_CHECK = "mcidasv.doversioncheck";

    /** Name of the pre-release check user preference. */
    String PREF_PRERELEASE_CHECK = "mcidasv.doprereleasecheck";

    /** Name of the {@literal "remove all data warning"} preference. */
    String PREF_CONFIRM_REMOVE_DATA =
        "mcv.warn.remove.data.all";

    /** Name of the {@literal "remove all layers warning"} preference. */
    String PREF_CONFIRM_REMOVE_LAYERS =
        "mcv.warn.remove.layers.all";

    /** Name of the {@literal "remove everything warning"} preference. */
    String PREF_CONFIRM_REMOVE_BOTH =
        "mcv.warn.remove.everything";

    /**
     * Preference for controlling the automated saving of the default layout.
     */
    String PREF_AUTO_SAVE_DEFAULT_LAYOUT = "mcidasv.defaultlayout.autosave";

    String PREF_SAVE_DASHBOARD_VIZ = "mcidasv.dashboard.savevisibility";

    /** Preference for saving image preview default. */
    String PREF_IMAGE_PREVIEW = "mcidasv.chooser.adde.preview";
    
    /** Used to alert user they are modifying default color table */
    String PREF_MODIFY_DEFAULT_COLOR_TABLE = "mcidasv.default.colortable.modify";

    /** Whether or not to show the McIDAS-V {@literal "system"} bundles. */
    String PREF_SHOW_SYSTEM_BUNDLES = "mcidasv.showsystembundles";

    String PREF_NUM_IMAGE_PRESET_IMGCHOOSER = "mcidasv.numentries.imgchooser";

    String PREF_NUM_IMAGE_PRESET_RADARCHOOSER = "mcidasv.numentries.rdrchooser";


    /**
     * Show large or small icons. If PREF_TBM_ICONS is disabled, this pref
     * has no meaning.
     */
    String PREF_TBM_SIZE = "tbm.icon.size";

    /** Property name for for the path to about dialog template. */
    String PROP_ABOUTTEXT = "mcidasv.about.text";

    /** Path to the main McIDAS-V icon. */
    String PROP_APP_ICON = "mcidasv.window.icon";

    /** When was visad.jar built? */
    String PROP_VISAD_DATE = "visad.build.date";

    /** What version of VisAD lives within visad.jar? */
    String PROP_VISAD_REVISION = "visad.build.revision";

    /** Was there a problem determing VisAD's version? */
    String PROP_VISAD_PARSE_FAIL = "visad.build.parsefail";

    /** What exactly broke the version extraction? */
    String PROP_VISAD_ORIGINAL = "visad.build.contents";

    /** When was McIDAS-V built? */
    String PROP_BUILD_DATE = "mcidasv.build.date";

    /** Property name for the copyright year. */
    String PROP_COPYRIGHT_YEAR = "mcidasv.copyright.year";

    /** Property name for the McIDAS-V homepage URL. */
    String PROP_HOMEPAGE = "mcidasv.homepage";

    /** Specifies use of {@code edu.wisc.ssec.mcidasv.ui.TabbedUIManager}. */
    String PROP_TABBED_UI = "mcidasv.tabbedDisplay";

    /** Property name for the major version number. */
    String PROP_VERSION_MAJOR = "mcidasv.version.major";

    /** Property name for the minor version number. */
    String PROP_VERSION_MINOR = "mcidasv.version.minor";

    /** Property name for the version release number. */
    String PROP_VERSION_RELEASE = "mcidasv.version.release";

    /** Property name for the path to version file. */
    String PROP_VERSIONFILE = "mcidasv.version.file";

    /** Property that determines whether the view panel should pop up. */
    String PROP_VP_SHOWPOPUP =
        "idv.ui.viewpanel.showpopup";

    /** Property for whether view panel categories will be shown. */
    String PROP_VP_SHOWCATS =
        "idv.ui.viewpanel.showcategories";

    /** Typo was found in IDV code. */
    String PROP_VP_CATOPEN = "viewpanel.catgegory.open";

    /**
     * Property used to restore the size and position of the dashboard upon
     * start.
     */
    String PROP_DASHBOARD_BOUNDS = "mcidasv.dashboard.bounds";

    /** Property used to store and retrieve color selection history. */
    String PROP_RECENT_COLORS = "mcidasv.colorchooser.recentcolors";

    /**
     * Property used to store and retrieve the {@literal "0-360"} checkbox
     * value.
     */
    String PROP_HYDRA_360 = "mcidasv.hydra.multispectral.use360";
    
    /**
     * Property indicating how many source granules made up a swath data
     * source.
     */
    String PROP_GRANULE_COUNT = "mcidasv.swath.granulecount";

    /** Application property file name. */
    String PROPERTIES_FILE =
        "/edu/wisc/ssec/mcidasv/resources/mcidasv.properties";

    /** McIDAS-V base URL. */
    String HOMEPAGE_URL = "https://www.ssec.wisc.edu/mcidas/software/v";

    /** Location of latest version file under base URL. */
    String VERSION_URL = "stable/version.txt";
    String VERSION_HANDLER_URL = "stable/version.php";

    /** Location of latest pre-release directory under base URL. */
    String PRERELEASE_URL = "prerelease/";

    /** Location of latest notice file under base URL. */
    String NOTICE_URL = "stable/notice.txt";

    /**
     * {@literal "Scrub strings"} are simple string substitutions for things
     * like labels.
     */
    String SCRUB_STRINGS_FILE =
        "/edu/wisc/ssec/mcidasv/resources/scrubstrings.xml";

    /** Where to look for javahelp. */
    String DEFAULT_DOCPATH = "/docs/userguide";
        
    /** File suffix for bundle files. */
    String SUFFIX_MCV = ".mcv";

    /** File suffix for compressed bundle files. */
    String SUFFIX_MCVZ = ".mcvz";

    /** File filter used for bundle files. */
    PatternFileFilter FILTER_MCV =
        new PatternFileFilter("(.+\\.mcv$)",
            "McIDAS-V Bundles (*.mcv)", SUFFIX_MCV);
    
    /** File filter used for bundle files. */
    PatternFileFilter FILTER_MCVZ =
        new PatternFileFilter("(.+\\.mcvz$)", "McIDAS-V Zipped Data Bundles (*.mcvz)", SUFFIX_MCVZ);

    /** File filter used for bundle files. */
    PatternFileFilter FILTER_MCVMCVZ =
        new PatternFileFilter("(.+\\.mcv$|.+\\.mcvz$)", "All McIDAS-V Bundles (*.mcv,*.mcvz)", SUFFIX_MCV);
    
    /** Default port for local ADDE servers. */
    String LOCAL_ADDE_PORT = "8112";
    
    String PROP_CHAN = "selectedchannel";
    
    /** Preference to store what ADDE servers to show. */
    String PROP_SERVERS = "idv.serverstoshow";

    /** Preference to store whether to show all ADDE servers. */
    String PROP_SERVERS_ALL = "idv.serverstoshow.all";
    
    /** Name to store the total system memory. */
    String PROP_SYSMEM = "idv.sysmem";
    
    /** Default map z-level */
    double DEFAULT_MAP_Z_LEVEL = -0.99d;

    /** Maximum amount of memory 32bit JREs can address, in megabytes. */
    int MAX_MEMORY_32BIT = 1536;
    
    /** A particular shade of blue we are using for branding. */
    Color MCV_BLUE = new Color(96, 176, 224);
    Color MCV_BLUE_DARK = new Color(0, 96, 255);

    /** default color picker size */
    Dimension DEFAULT_COLOR_PICKER_SIZE = new Dimension(24, 20);

    /** Identifier for the {@literal "monitor panel"} window component. */
    String COMP_MONITORPANEL = "mcv.monitorpanel";
    
    /** Default user directory name */
    String USER_DIRECTORY_NAME = "McIDAS-V";

    /** Tooltip for adaptive resolution menu items. */
    String TOOLTIP_PROGRESSIVE_RESOLUTION = "This feature can be turned on or off in the \"Display Window\" section of the User Preferences.";

    /** EventBus topic for signaling that directory monitors may begin. */
    String EVENT_FILECHOOSER_START = "FileChooser.StartWatchService";

    /** EventBus topic for signaling that directory monitors should stop. */
    String EVENT_FILECHOOSER_STOP = "FileChooser.StopWatchService";
}
