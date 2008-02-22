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

package edu.wisc.ssec.mcidasv.startupmanager;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ucar.unidata.util.GuiUtils;

import edu.wisc.ssec.mcidasv.Constants;

public class StartupManager implements ListSelectionListener, ActionListener {

	/** */
	public final static String PREF_SM_HEAPSIZE = "java.vm.heapsize";

	/** Whether or not we should attempt to mess with JOGL. */
	public final static String PREF_SM_JOGL = "java.jogl.togl";

	/** */
	public final static String PREF_SM_INITHEAP = "java.vm.initialheap";

	/** */
	public final static String PREF_SM_THREAD = "java.vm.threadstack";

	/** */
	public final static String PREF_SM_YOUNGGEN = "java.vm.younggen";

	/** */
	public final static String PREF_SM_XMEM = "mcx.allocmem";

	/** */
	public final static String PREF_SM_XDIR = "mcx.workingdir";

	/** */
	public final static String PREF_SM_XSCHED = "mcx.enablescheduler";

	/** */
	public final static String PREF_SM_XCASE = "mcx.invertcase";

	/** */
	public final static String PREF_SM_COLLAB = "idv.collabmode";

	/** */
	public final static String PREF_SM_COLLAB_PORT = "idv.collabport";

	/** */
	public final static String PREF_SM_DEBUG = "idv.enabledebug";

	public final static String PREF_SM_3D = "idv.disable3d";

	/** */
	public static final String[][] PREF_PANELS = {
		{Constants.PREF_LIST_GENERAL, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/mcidasv-round32.png"},
		{Constants.PREF_LIST_VIEW, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/tab-new32.png"},
		{Constants.PREF_LIST_TOOLBAR, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/application-x-executable32.png"},
		{Constants.PREF_LIST_DATA_CHOOSERS, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/preferences-desktop-remote-desktop32.png"},
		{Constants.PREF_LIST_ADDE_SERVERS, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/applications-internet32.png"},
		{Constants.PREF_LIST_AVAILABLE_DISPLAYS, "/edu/wisc/ssec/mcidasv/resources/prefs/icons/video-display32.png"},
		{Constants.PREF_LIST_NAV_CONTROLS, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/input-mouse32.png"},
		{Constants.PREF_LIST_FORMATS_DATA,"/edu/wisc/ssec/mcidasv/resources/icons/prefs/preferences-desktop-theme32.png"},
		{Constants.PREF_LIST_ADVANCED, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/applications-internet32.png"},
	};

	/** */
	public static final Object[][] RENDER_HINTS = {
		{RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON},
		{RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY},
		{RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON}
	};

	/** String tried against the <tt>os.name</tt> property. */
	public static final String WINDOWS_ID = "Windows";

	public final static Pattern RE_GET_UNIX_3D = 
		Pattern.compile("^DISABLE_3DSTUFF=(.+)$", Pattern.MULTILINE);

	public final static Pattern RE_GET_WIN_3D = 
		Pattern.compile("^SET DISABLE_3DSTUFF=(.+)$", Pattern.MULTILINE);

	/** */
	public final static Pattern RE_GET_UNIX_HEAP_SIZE = 
		Pattern.compile("^HEAP_SIZE=(.+)$", Pattern.MULTILINE);

	/** */
	public final static Pattern RE_GET_WIN_HEAP_SIZE = 
		Pattern.compile("^SET HEAP_SIZE=(.+)$", Pattern.MULTILINE);
	
	/** 
	 * Regular expression that allows us to read the JOGL toggle variable in
	 * the unix-style startup script. 
	 */
	public final static Pattern RE_GET_UNIX_JOGL = 
		Pattern.compile("^JOGL_TOGL=(.+)$", Pattern.MULTILINE);

	/** 
	 * Regular expression that'll read the JOGL switch variable for windows.
	 */
	public final static Pattern RE_GET_WIN_JOGL = 
		Pattern.compile("^SET JOGL_TOGL=(.+)$", Pattern.MULTILINE);
	
	/** */	
	public final static Pattern RE_GET_UNIX_INIT_HEAP = 
		Pattern.compile("^INIT_HEAP=(.+)$", Pattern.MULTILINE);

	/** */
	public final static Pattern RE_GET_UNIX_THREAD_STACK =
		Pattern.compile("^THREAD_STACK=(.+)$", Pattern.MULTILINE);

	/** */
	public final static Pattern RE_GET_UNIX_YOUNG_GEN = 
		Pattern.compile("^YOUNG_GEN=(.+)$", Pattern.MULTILINE);

	/** */
	public final static Pattern RE_GET_UNIX_COLLAB_MODE = 
		Pattern.compile("^COLLAB_MODE=(.+)$", Pattern.MULTILINE);

	/** */
	public final static Pattern RE_GET_UNIX_COLLAB_PORT = 
		Pattern.compile("^COLLAB_PORT=(.+)$", Pattern.MULTILINE);

	/** */
	public final static Pattern RE_GET_UNIX_ENABLE_DEBUG =
		Pattern.compile("^ENABLE_DEBUG=(.+)$", Pattern.MULTILINE);

	public final static Pattern RE_SET_UNIX_3D = 
		Pattern.compile("^DISABLE_3DSTUFF=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);
	
	public final static Pattern RE_SET_WIN_3D = 
		Pattern.compile("^SET DISABLE_3DSTUFF=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);
	
	/** */
	public final static Pattern RE_SET_UNIX_HEAP_SIZE = 
		Pattern.compile("^HEAP_SIZE=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);

	/** */
	public final static Pattern RE_SET_WIN_HEAP_SIZE = 
		Pattern.compile("^SET HEAP_SIZE=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);

	/** Replace any lines that match this regexp with user input. */
	public final static Pattern RE_SET_UNIX_JOGL = 
		Pattern.compile("^JOGL_TOGL=[0-9]$", Pattern.MULTILINE);
	
	public final static Pattern RE_SET_WIN_JOGL = 
		Pattern.compile("^SET JOGL_TOGL=[0-9]$", Pattern.MULTILINE);
	
	/** */	
	public final static Pattern RE_SET_UNIX_INIT_HEAP = 
		Pattern.compile("^INIT_HEAP=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);

	/** */
	public final static Pattern RE_SET_UNIX_THREAD_STACK =
		Pattern.compile("^THREAD_STACK=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);

	/** */
	public final static Pattern RE_SET_UNIX_YOUNG_GEN = 
		Pattern.compile("^YOUNG_GEN=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);

	/** */
	public final static Pattern RE_SET_UNIX_COLLAB_MODE = 
		Pattern.compile("^COLLAB_MODE=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);

	/** */
	public final static Pattern RE_SET_UNIX_COLLAB_PORT = 
		Pattern.compile("^COLLAB_PORT=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);

	/** */
	public final static Pattern RE_SET_UNIX_ENABLE_DEBUG =
		Pattern.compile("^ENABLE_DEBUG=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);	

	/** */
	public static Hashtable<String, Pattern> windowsGetters =
		new Hashtable<String, Pattern>();

	/** */
	public static Hashtable<String, Pattern> windowsSetters =
		new Hashtable<String, Pattern>();
	
	/** */	
	public static Hashtable<String, Pattern> unixGetters =
		new Hashtable<String, Pattern>();
	
	/** */	
	public static Hashtable<String, Pattern> unixSetters = 
		new Hashtable<String, Pattern>();
	
	// TODO: comments
	static {
		windowsGetters.put(PREF_SM_HEAPSIZE, RE_GET_WIN_HEAP_SIZE);
		windowsGetters.put(PREF_SM_JOGL, RE_GET_WIN_JOGL);
		windowsGetters.put(PREF_SM_3D, RE_GET_WIN_3D);
		/*windowsGetters.put(PREF_SM_INITHEAP, RE_GET_WIN_INIT_HEAP);
		windowsGetters.put(PREF_SM_THREAD, RE_GET_WIN_THREAD_STACK);
		windowsGetters.put(PREF_SM_YOUNGGEN, RE_GET_WIN_YOUNG_GEN);
		windowsGetters.put(PREF_SM_XMEM, null);
		windowsGetters.put(PREF_SM_XDIR, null);
		windowsGetters.put(PREF_SM_XSCHED, null);
		windowsGetters.put(PREF_SM_XCASE, null);
		windowsGetters.put(PREF_SM_COLLAB, RE_GET_WIN_COLLAB_MODE);
		windowsGetters.put(PREF_SM_COLLAB_PORT, RE_GET_WIN_COLLAB_PORT);
		windowsGetters.put(PREF_SM_DEBUG, RE_GET_WIN_ENABLE_DEBUG);*/
		
		windowsSetters.put(PREF_SM_HEAPSIZE, RE_SET_WIN_HEAP_SIZE);
		windowsSetters.put(PREF_SM_JOGL, RE_SET_WIN_JOGL);
		windowsSetters.put(PREF_SM_3D, RE_SET_WIN_3D);
		
		unixGetters.put(PREF_SM_HEAPSIZE, RE_GET_UNIX_HEAP_SIZE);
		unixGetters.put(PREF_SM_JOGL, RE_GET_UNIX_JOGL);
		unixGetters.put(PREF_SM_3D, RE_GET_UNIX_3D);
		
		unixSetters.put(PREF_SM_HEAPSIZE, RE_SET_UNIX_HEAP_SIZE);
		unixSetters.put(PREF_SM_JOGL, RE_SET_UNIX_JOGL);
		unixSetters.put(PREF_SM_3D, RE_SET_UNIX_3D);
	}

	/** The name of the unix-style run script. */
	public final static String UNIX_SCRIPT_PATH = "runMcV";
	
	/** The name of the windows run script. */
	public final static String WINDOWS_SCRIPT_PATH = "runMcV.bat";	

	/** */
	private JSplitPane splitPane;

	/** */
	private JList list;

	/** */
	private DefaultListModel listModel;

	/** */
	private JScrollPane listScrollPane;

	/** */
	private JFrame frame;

	/** Contains the user input for the maximum JVM heap size. */
	private JTextField maxHeap;	

	/** User input for whether or not JOGL should be enabled. */
	private JCheckBox joglToggle;
	
	private JCheckBox disable3d;
	
	/** Is this a Unix-style platform? */
	private boolean isUnixLike = false;

	/** Is this a Windows platform? */
	private boolean isWindows = false;

	/** */
	private Hashtable<String, Object> store = new Hashtable<String, Object>();	
	
	/**
	 * 
	 */
	public StartupManager() {
		try {
			determinePlatform();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		
		Hashtable<String, Pattern> getters;
		String path;
		
		if (isUnixLike == true) {
			getters = unixGetters;
			path = UNIX_SCRIPT_PATH;
		} else {
			getters = windowsGetters;
			path = WINDOWS_SCRIPT_PATH;
		}

		readStartup(path, getters);
	}
	
	/**
	 * Queries the "os.name" property and tries to match against known platform
	 * strings. Currently this method will simply set one of <tt>isWindows</tt>
	 * or <tt>isUnixLike</tt> depending on whether or not Windows was found.
	 */
	private void determinePlatform() {
		String os = System.getProperty("os.name");
		
		if (os == null)
			throw new RuntimeException();
		
		if (os.startsWith(WINDOWS_ID))
			isWindows = true;
		else
			isUnixLike = true;
	}	
	
	/**
	 * Lays out the various components to create the main startup manager 
	 * frame. 
	 */
	public void createDisplay() {
		JPanel commandRow = createCommandRow();
		createListGUI();
		
		frame = new JFrame("User Preferences");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, 
				getSelectedPanel());
		splitPane.setResizeWeight(0);
		
		frame.getContentPane().add(splitPane);
		frame.getContentPane().add(commandRow, BorderLayout.PAGE_END);
		
		frame.pack();
		frame.setSize(600, 400);
		//frame.setResizable(false);
		frame.setVisible(true);
	}
	
	/**
	 * 
	 * 
	 * @return
	 */
	private JPanel createCommandRow() {
		JPanel row = new JPanel(new FlowLayout());
		
		JButton apply = new ApplyButton("Apply");		
		JButton ok = new OkButton("Ok");		
		JButton help = new HelpButton("Help");
		JButton cancel = new CancelButton("Cancel");
	
		row.add(apply);
		row.add(ok);
		row.add(help);
		row.add(cancel);
		
		return row;
	}
	
	/**
	 * 
	 * @param e
	 */
	public void actionPerformed(ActionEvent e) {
	}
	
	/**
	 * 
	 */
	private void createListGUI() {				
		listModel = new DefaultListModel();
		
    	for (int i = 0; i < PREF_PANELS.length; i++) {
    		JLabel label = new JLabel();

    		label.setText(PREF_PANELS[i][0]);
    		label.setIcon(new ImageIcon(getClass().getResource(PREF_PANELS[i][1])));
    		
    		listModel.addElement(label);
    	}
		
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(PREF_PANELS.length-1);
		list.addListSelectionListener(this);
		list.setVisibleRowCount(PREF_PANELS.length);
		list.setCellRenderer(new IconCellRenderer());
		listScrollPane = new JScrollPane(list);
	}	

	/**
	 * Handle
	 * 
	 * @param e
	 */
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) {
			splitPane.setRightComponent(getSelectedPanel());
		}
	}	
	
	/**
	 * Returns the container the corresponds to the currently selected label in
	 * the JList.
	 * 
	 * @return The current container.
	 */
	private Container getSelectedPanel() {
		String key = ((JLabel)listModel.getElementAt(list.getSelectedIndex())).getText();
		JPanel panel;
	
		if (key.equals(Constants.PREF_LIST_ADVANCED) == false)
			panel = getUnavailablePanel();
		else
			panel = getAdvancedPanel();

		return panel;
	}

	/**
	 * 
	 * 
	 * @return
	 */
	private JPanel getUnavailablePanel() {
		JPanel panel = new JPanel();
		
		panel.add(new JLabel("These options are unavailable in this context."));
		
		return panel;
	}
	
	/**
	 * 
	 * 
	 * @return
	 */
	private JPanel getAdvancedPanel() {
    	List<Component> guiComponents = new ArrayList<Component>();

    	String blank = "";
    	String heapSize = getPref(PREF_SM_HEAPSIZE, blank);
    	boolean joglEnabled = true;
    	if (getPref(PREF_SM_JOGL, "0").equals("0"))
    		joglEnabled = false;

    	maxHeap = new JTextField(heapSize, 10);
    	joglToggle = new JCheckBox();
    	joglToggle.setSelected(joglEnabled);

    	disable3d = new JCheckBox();
    	disable3d.setSelected(getPref(PREF_SM_3D, false));

    	JPanel javaPanel = GuiUtils.vbox(
        	GuiUtils.lLabel("Startup Options:"),
        	GuiUtils.doLayout(new Component[] {
        		GuiUtils.rLabel("  Maximum Heap Size:"),
        		GuiUtils.left(maxHeap),
        		GuiUtils.rLabel("  Enable JOGL:"),
        		GuiUtils.left(joglToggle),
        		GuiUtils.rLabel("  Disable 3D:"),
        		GuiUtils.left(disable3d),
        	}, 2, GuiUtils.WT_N, GuiUtils.WT_N));

    	guiComponents.add(javaPanel);

    	return GuiUtils.inset(GuiUtils.topLeft(GuiUtils.doLayout(guiComponents, 1, GuiUtils.WT_N, GuiUtils.WT_N)), 5);
	}

	/**
	 * Return the contents of a given file as a String.
	 * 
	 * @param file The file with the contents people want.
	 * 
	 * @return Null if no valid file, or the contents of said file.
	 */
	public static String readFile(String file) {
		StringBuffer contents = new StringBuffer();
		String line;

		File script = new File(file);
		if (script.getPath().length() == 0)
			return contents.toString();

		try {
			BufferedReader br = new BufferedReader(new FileReader(script));

			while ((line = br.readLine()) != null)
				contents.append(line + "\n");

			br.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return contents.toString();
	}

	/**
	 * Read a given startup script using the provided set of "preferences" and
	 * the regular expressions that discover their corresponding values.
	 * 
	 * @param file The file to parse.
	 * @param getters Keys and Patterns used to understand the contents of <tt>file</tt>.
	 */
	public void readStartup(String file, Hashtable<String, Pattern> getters) {
		String contents = readFile(file);

		Enumeration<String> keys = getters.keys();
		while (keys.hasMoreElements()) {
			String pref = keys.nextElement();
			Pattern regexp = getters.get(pref);

			Matcher m = regexp.matcher(contents);
			if (m.find() == true)
				setPref(pref, m.group(1));
		}
	}

	/**
	 * Writes to a given startup script.
	 * 
	 * @param file The script to which we apply our startup changes!
	 * @param setters The patterns used to set the values within the script.
	 */
	public void writeStartup(String file, Hashtable<String, Pattern> setters) {
		Hashtable<String, String> data = collectPrefs();
		String contents = readFile(file);

		Enumeration<String> keys = setters.keys();
		while (keys.hasMoreElements()) {
			String pref = keys.nextElement();
			Pattern regexp = setters.get(pref);

			Matcher m = regexp.matcher(contents);
			contents = m.replaceAll(data.get(pref));
		}

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(contents);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Polls the various startup option widgets for their values.
	 * 
	 * @return A table of prefs and the values that the user has set.
	 */
	private Hashtable<String, String> collectPrefs() {
		Hashtable<String, String> prefs = new Hashtable<String, String>();
		StringBuffer heapSizeFlag;
		StringBuffer joglFlag;
		StringBuffer threeDFlag;
		
		// TODO: make less stupid
		if (isWindows == true) {
			heapSizeFlag = new StringBuffer("SET HEAP_SIZE=");
			joglFlag = new StringBuffer("SET JOGL_TOGL=");
			threeDFlag = new StringBuffer("SET DISABLE_3DSTUFF=");
		}
		else {
			heapSizeFlag = new StringBuffer("HEAP_SIZE=");
			joglFlag = new StringBuffer("JOGL_TOGL=");
			threeDFlag = new StringBuffer("DISABLE_3DSTUFF=");
		}
		
		StringBuffer initHeapFlag = new StringBuffer("INIT_HEAP=");		
		StringBuffer youngGenFlag = new StringBuffer("YOUNG_GEN=");
		StringBuffer threadStackFlag = new StringBuffer("THREAD_STACK=");
		StringBuffer collabModeFlag = new StringBuffer("COLLAB_MODE=");
		StringBuffer collabPortFlag = new StringBuffer("COLLAB_PORT=");
		StringBuffer debugFlag = new StringBuffer("ENABLE_DEBUG=");

		String blank = "";
		String heapSize = getPref(PREF_SM_HEAPSIZE, blank);
		String initHeap = getPref(PREF_SM_INITHEAP, blank);
		String youngGen = getPref(PREF_SM_YOUNGGEN, blank);
		String threadStack = getPref(PREF_SM_THREAD, blank);
		String joglVal = getPref(PREF_SM_JOGL, "0");
		String threeDVal = ((Boolean)getPref(PREF_SM_3D, false)).toString();

		String collabMode;
		String collabPort;

		if (getPref(PREF_SM_COLLAB, false) == true) {
			collabMode = "-server";
			collabPort = getPref(PREF_SM_COLLAB_PORT, blank);
		} else {
			collabMode = blank;
			collabPort = blank;
		}

		joglFlag.append(joglVal);
		threeDFlag.append(threeDVal);
		
		if (heapSize.length() != 0)
			heapSizeFlag.append(heapSize);

		if (initHeap.length() != 0)
			initHeapFlag.append("-Xms" + initHeap);

		if (youngGen.length() != 0)
			youngGenFlag.append("-XX:NewSize=" + youngGen);

		if (threadStack.length() != 0)
			threadStackFlag.append("-XX:ThreadStackSize" + threadStack);

		if (collabMode.length() != 0)
			collabModeFlag.append(collabMode);

		if (collabPort.length() != 0)
			collabPortFlag.append(collabPort);

		System.err.println(heapSizeFlag);
		System.err.println(threeDFlag);
		
		prefs.put(PREF_SM_HEAPSIZE, heapSizeFlag.toString());
		prefs.put(PREF_SM_JOGL, joglFlag.toString());
		prefs.put(PREF_SM_3D, threeDFlag.toString());
		prefs.put(PREF_SM_INITHEAP, initHeapFlag.toString());
		prefs.put(PREF_SM_YOUNGGEN, youngGenFlag.toString());
		prefs.put(PREF_SM_THREAD, threadStackFlag.toString());
		prefs.put(PREF_SM_COLLAB, collabModeFlag.toString());
		prefs.put(PREF_SM_COLLAB_PORT, collabPortFlag.toString());
		prefs.put(PREF_SM_DEBUG, debugFlag.toString());

		return prefs;
	}
	
	/**
	 * Return the value of a given preference. If the preference hasn't been
	 * created properly, return the given default value.
	 * 
	 * @param id The ID of the preference.
	 * @param dflt If no preference, return this value.
	 * 
	 * @return The value of the preference.
	 */
	private boolean getPref(String id, boolean dflt) {
		if (store.containsKey(id))
			return (Boolean)store.get(id);
		return dflt;
	}
	
	/**
	 * Return the value of a given preference. If the preference hasn't been
	 * created properly, return the given default value.
	 * 
	 * @param id The ID of the preference.
	 * @param dflt If no preference, return this value.
	 * 
	 * @return The value of the preference.
	 */
	private String getPref(String id, String dflt) {
		if (store.containsKey(id))
			return (String)store.get(id);
		return dflt;
	}
		
	/**
	 * Set the value of the given preference to whatever your heart desires.
	 * 
	 * @param id The preference we are trying to set.
	 * @param value The new value of the preference.
	 */
	private void setPref(String id, Object value) {
		if (value.equals("true") || value.equals("false"))
			value = new Boolean((String)value);

		store.put(id, value);
	}
	
	/**
	 * 
	 * 
	 * @return
	 */
	public static RenderingHints getRenderingHints() {
		RenderingHints hints = new RenderingHints(null);
		
		for (int i = 0; i < RENDER_HINTS.length; i++)
			hints.put(RENDER_HINTS[i][0], RENDER_HINTS[i][1]);
		
		return hints;
	}
	
	/**
	 * 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String laf = UIManager.getCrossPlatformLookAndFeelClassName();
		
		try {
			UIManager.setLookAndFeel(laf);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		StartupManager mngr = new StartupManager();
		mngr.createDisplay();
	}
	
	/**
	 * 
	 * 
	 */
	public class IconCellRenderer extends DefaultListCellRenderer {
		
		/**
		 * 
		 * 
		 * @param list
		 * @param value
		 * @param index
		 * @param isSelected
		 * @param cellHasFocus
		 * 
		 * @return
		 */
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {

			super.getListCellRendererComponent(list, value, index, isSelected, 
					cellHasFocus);

			if (value instanceof JLabel) {
				setText(((JLabel)value).getText());
				setIcon(((JLabel)value).getIcon());
			}

			return this;
		}

		/**
		 * 
		 * 
		 * @param g
		 */
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D)g;
			
			g2d.setRenderingHints(getRenderingHints());
			
			super.paintComponent(g2d);
		}
	}
	
	/**
	 * 
	 * 
	 * @author McIDAS-V Developers
	 */
	private abstract class CommandButton extends JButton implements ActionListener {
		
		/**
		 * 
		 * @param label
		 */
		public CommandButton(String label) {
			super(label);
			this.addActionListener(this);
		}
		
		/**
		 * 
		 */
		abstract public void processEvent();
		
		/**
		 * 
		 * @param e
		 */
		public void actionPerformed(ActionEvent e) {
			processEvent();
		}
		
		/**
		 * 
		 * 
		 * @param g
		 */
		public void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D)g;
			
			g2d.setRenderingHints(getRenderingHints());
			
			super.paintComponent(g2d);
		}
		
	}
	
	private class ApplyButton extends CommandButton {
		public ApplyButton(String label) {
			super(label);
		}
		
		// save and quit
		public void processEvent() {
			System.out.println("apply");
			
			String joglthing = "0";
			if (joglToggle.isSelected() == true)
				joglthing = "1";
			
			setPref(PREF_SM_HEAPSIZE, maxHeap.getText());
			setPref(PREF_SM_JOGL, joglthing);
			setPref(PREF_SM_3D, disable3d.isSelected());

			Hashtable<String, Pattern> setters;
			String path;
			
			if (isUnixLike == true) {
				setters = unixSetters;
				path = UNIX_SCRIPT_PATH;
			} else {
				setters = windowsSetters;
				path = WINDOWS_SCRIPT_PATH;
			}
			
			writeStartup(path, setters);
		}
				
	}
	
	private class OkButton extends CommandButton {
		public OkButton(String label) {
			super(label);
		}

		// save
		public void processEvent() {
			System.out.println("ok");
		}
	}
	
	private class HelpButton extends CommandButton {
		public HelpButton(String label) {
			super(label);
		}
		
		// ??
		public void processEvent() {
			System.out.println("help");
		}
	}
	
	private class CancelButton extends CommandButton { 
		public CancelButton(String label) {
			super(label);
		}
		
		// quit
		public void processEvent() {
			System.exit(0);
		}
	}
}
