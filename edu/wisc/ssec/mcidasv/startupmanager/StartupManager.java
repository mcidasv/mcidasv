package edu.wisc.ssec.mcidasv.startupmanager;

import edu.wisc.ssec.mcidasv.ui.persistbox.*;
 
import java.awt.*;

import java.awt.event.*;

import javax.swing.*;

import javax.swing.table.*;

import java.util.*;

/**
 * The <code>StartupManager</code> class implements a relatively simple Swing
 * GUI utility that allows users to easily manipulate their McIDAS-V or IDV
 * installations. Rather than have to write execution aliases and so on, the
 * user can simply edit the current settings, click save, and start either
 * McIDAS-V or IDV as they normally would.
 * 
 * Forthcoming features: 
 * - save off preferences to profiles
 * - be able to download these profiles from remote servers (perhaps pinging to see if any updates are needed?).
 * - replacing hardcoded strings with something read from a config xml file (easy multi-lingual support) or maybe a properties file?
 * 
 * TODO: offload some stuff into separate threads
 * TODO: for OS X either pick a slightly larger window size or turn off resizing
 *       to fix that resizer box issue.
 * 
 * @author Jonathan Beavers, SSEC
 */
public class StartupManager {

	private Hashtable actionToComponent = new Hashtable();
	
	// exception messages
	/** Exception message for dealing with bad ListItem types. */
	protected static final String EMSG_BAD_ITEM_TYPE = "";
	
	/** 
	 * Exception message for dealing with attempting to find an index for an
	 * unknown column name. 
	 */
	protected static final String EMSG_BAD_COLUMN_NAME = "";
	
	/** Exception message for dealing with null ListItems. */
	protected static final String EMSG_NULL_LIST_ITEM = "";
	// end exception messages
	
	// platform identification strings	
	/** */
	private final String WINDOWS_ID = "Windows";
	
	/** */
	private final String MACOSX_ID = "Mac OS X";	
	// end platform ids.
	
	// handy gui pointers
	/** Reference to command row of buttons along the bottom of the panel */
	private CommandRow commandRow;
	
	/** Holder of the tabs! */
	private JTabbedPane tabbedPane;
	
	/** Main frame */
	private JFrame frame;
	
	/** A friendly reference to the current instance. */
	private StartupManager manager = this;
	// end handy gui pointers
	
	/**
	 * Contains all of the tab types and related info for the tabbed pane of
	 * the startup manager.
	 */
	private Object[][] tabs = {
			{new JavaOptions(this), "Java VM", "1"},
			{new PluginOptions(this), "IDV Plugins", "2"},
			{new BundleOptions(this), "IDV Bundles", "3"}, 
			{new BatchOptions(this), "\"Batch Processing\"", "4"},
			{new NetworkingOptions(this), "\"Networking\"", "5"},
			{new MiscOptions(this), "Miscellaneous", "6"},
			{new McidasXOptions(this), "McIDAS X Options", "7"},
	};

	/** Whether or not determinePlatform() found Windows */
	protected boolean isWindows = false;
	
	/** Whether or not determinePlatform() found OS X */
	protected boolean isMac = false;
	
	/** Whether or not determinePlatform() found Unix */
	protected boolean isUnix = false;
	
	/**
	 * Initialize the startup manager, which largely consists of attempting
	 * to figure out which platform we're using (for the time being).
	 */
	public StartupManager() {
		try {
			determinePlatform();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Polls the various panels and so on to figure out what options need to
	 * be written to the script that starts McV/IDV.
	 * 
	 * @param path The path of the file that will be created/overwritten.
	 */
	public void writeScript(String path) {
		// each panel should have a method like getFlags() that returns a 
		// string with each flag and any parameters. 
		// java options must be placed differently than McV/IDV options.
		String flags = new String("");
		for (int i = 0; i < tabs.length; i++) {
			flags += ((OptionPanel)tabs[i][0]).getFlags();
		}
		System.out.println(flags);
	}
	
	/**
	 * Initializes the tabbed display panel and all of the child widgets, and
	 * then displays everything to the screen. 
	 */
	public void createDisplay() {
		createMainPanel();
		
		frame = new JFrame("Startup Manager");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		frame.getContentPane().add(commandRow.getPanel(), BorderLayout.SOUTH);
		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * Creates the tabs and adds them to the "main" panel. The tabs are 
	 * basically auto-created based upon the data in the "tabs" field. This 
	 * field must follow the following format:
	 * {new <TabPanel>(), "Visible Tab Name", "Keyboard Shortcut"}
	 */
	private void createMainPanel() {
		commandRow = new CommandRow(this);
		
		tabbedPane = 
			new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		//tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		
		// go on and create the required tabs
		for (int i = 0; i < tabs.length; i++) {
			JPanel temp = ((OptionPanel)tabs[i][0]).createPanel();
			String tabName = (String)tabs[i][1];
			String shortcut = (String)tabs[i][2];			
			tabbedPane.addTab(tabName, null, temp, shortcut);
		}		
	}
	
	/**
	 * Attempt to identify the OS that we're currently running on. Currently
	 * I only identify "Unix", OS X, and Windows. This method examines the Java
	 * os.name property, and if it finds either Windows or OS X, it'll set 
	 * either isWindows or isMac respectively. If neither Windows or OS X match,
	 * I set isUnix to true. 
	 * 
	 * This behavior is stupid.
	 */
	private void determinePlatform() {
		String os = System.getProperty("os.name");
		
		if (os == null)
			throw new RuntimeException();
		
		if (os.startsWith(WINDOWS_ID))
			isWindows = true;
		else if (os.startsWith(MACOSX_ID))
			isMac = false;
		else
			isUnix = false;
	}
	
	/**
	 * TODO: a pretty huge set of functionality.
	 */
	public void saveState() {
		System.out.println("todo!");
	}
	
	/**
	 * Loop through all of the tabs and call 
	 * <code>toggleAdvancedOptions</code> for each of them. 
	 */
	public void toggleAdvancedOptions() {
		for (int i = 0; i < tabs.length; i++)
			((OptionPanel)tabs[i][0]).toggleAdvancedOptions();
	}
	
	/**
	 * 
	 * 
	 * @param args things and stuff
	 */	
	public static void main(String[] args) {
		// default to Metal
		String laf = UIManager.getCrossPlatformLookAndFeelClassName();
		try {
			UIManager.setLookAndFeel(laf);
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();			
		}

		StartupManager mngr = new StartupManager();
		mngr.createDisplay();
	}
}