package edu.wisc.ssec.mcidasv.startupmanager;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
	public static final String[][] PREF_PANELS = {
		{Constants.PREF_LIST_GENERAL, "/edu/wisc/ssec/mcidasv/resources/icons/mcidasv-round32.png"},
		{Constants.PREF_LIST_VIEW, "/edu/wisc/ssec/mcidasv/resources/icons/tab-new32.png"},
		{Constants.PREF_LIST_TOOLBAR, "/edu/wisc/ssec/mcidasv/resources/icons/applications-accessories32.png"},
		{Constants.PREF_LIST_DATA_CHOOSERS, "/edu/wisc/ssec/mcidasv/resources/icons/preferences-desktop-remote-desktop32.png"},
		{Constants.PREF_LIST_ADDE_SERVERS, "/edu/wisc/ssec/mcidasv/resources/icons/applications-internet32.png"},
		{Constants.PREF_LIST_AVAILABLE_DISPLAYS, "/edu/wisc/ssec/mcidasv/resources/icons/video-display32.png"},
		{Constants.PREF_LIST_NAV_CONTROLS, "/edu/wisc/ssec/mcidasv/resources/icons/input-mouse32.png"},
		{Constants.PREF_LIST_FORMATS_DATA,"/edu/wisc/ssec/mcidasv/resources/icons/preferences-desktop-theme32.png"},
		{Constants.PREF_LIST_ADVANCED, "/edu/wisc/ssec/mcidasv/resources/icons/applications-internet32.png"},
	};	

	/** */
	public static final Object[][] RENDER_HINTS = {
		{RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON},
		{RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY},
		{RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON}
	};

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

	/** */
	private static final String WINDOWS_ID = "Windows";	

	/** */
	private boolean isUnixLike = false;

	/** */
	private boolean isWindows = false;

	/** */
	private Hashtable<String, Object> store = new Hashtable<String, Object>();

	/** */
	private String unixScriptPath = "runMcV";
	
	private String windowsScriptPath = "runMcV.bat";

	private final static Pattern RE_GET_HEAP_SIZE = 
		Pattern.compile("^HEAP_SIZE=(.+)$", Pattern.MULTILINE);

	private final static Pattern RE_GET_WIN_HEAP_SIZE = 
		Pattern.compile("^SET HEAP_SIZE=(.+)$", Pattern.MULTILINE);
	
	private final static Pattern RE_GET_INIT_HEAP = 
		Pattern.compile("^INIT_HEAP=(.+)$", Pattern.MULTILINE);

	private final static Pattern RE_GET_THREAD_STACK =
		Pattern.compile("^THREAD_STACK=(.+)$", Pattern.MULTILINE);

	private final static Pattern RE_GET_YOUNG_GEN = 
		Pattern.compile("^YOUNG_GEN=(.+)$", Pattern.MULTILINE);

	private final static Pattern RE_GET_COLLAB_MODE = 
		Pattern.compile("^COLLAB_MODE=(.+)$", Pattern.MULTILINE);

	private final static Pattern RE_GET_COLLAB_PORT = 
		Pattern.compile("^COLLAB_PORT=(.+)$", Pattern.MULTILINE);

	private final static Pattern RE_GET_ENABLE_DEBUG =
		Pattern.compile("^ENABLE_DEBUG=(.+)$", Pattern.MULTILINE);

	private final static Pattern RE_SET_HEAP_SIZE = 
		Pattern.compile("^HEAP_SIZE=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);

	private final static Pattern RE_SET_WIN_HEAP_SIZE = 
		Pattern.compile("^SET HEAP_SIZE=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);
	
	private final static Pattern RE_SET_INIT_HEAP = 
		Pattern.compile("^INIT_HEAP=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);

	private final static Pattern RE_SET_THREAD_STACK =
		Pattern.compile("^THREAD_STACK=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);

	private final static Pattern RE_SET_YOUNG_GEN = 
		Pattern.compile("^YOUNG_GEN=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);

	private final static Pattern RE_SET_COLLAB_MODE = 
		Pattern.compile("^COLLAB_MODE=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);

	private final static Pattern RE_SET_COLLAB_PORT = 
		Pattern.compile("^COLLAB_PORT=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);

	private final static Pattern RE_SET_ENABLE_DEBUG =
		Pattern.compile("^ENABLE_DEBUG=[a-zA-Z0-9-]{0,}$", Pattern.MULTILINE);	

	private final static String PREF_SM_HEAPSIZE = "java.vm.heapsize";
	private final static String PREF_SM_INITHEAP = "java.vm.initialheap";
	private final static String PREF_SM_THREAD = "java.vm.threadstack";
	private final static String PREF_SM_YOUNGGEN = "java.vm.younggen";
	private final static String PREF_SM_XMEM = "mcx.allocmem";
	private final static String PREF_SM_XDIR = "mcx.workingdir";
	private final static String PREF_SM_XSCHED = "mcx.enablescheduler";
	private final static String PREF_SM_XCASE = "mcx.invertcase";
	private final static String PREF_SM_COLLAB = "idv.collabmode";
	private final static String PREF_SM_COLLAB_PORT = "idv.collabport";
	private final static String PREF_SM_DEBUG = "idv.enabledebug";	

	private Hashtable<String, Object[]> dataTable = 
		new Hashtable<String, Object[]>();
	

	private void initTableData() {
		// not quite ready for this stuff yet
	}

	private Object[] getTableData(String id) {
		if (dataTable.containsKey(id))
			return dataTable.get(id);
		
		return null;
	}
		
	private JComponent getComponent(String id) {
		Object[] tmpRef = getTableData(id);
		if (tmpRef != null)
			return (JComponent)tmpRef[3];
		
		return null;
	}
	
	private Pattern getRunScriptParser(String id) {
		Object[] tmpRef = getTableData(id);
		if (tmpRef != null)
			return (Pattern)tmpRef[1];
		
		return null;
	}
	
	private Pattern getRunScriptSetter(String id) {
		Object[] tmpRef = getTableData(id);
		if (tmpRef != null)
			return (Pattern)tmpRef[2];
		
		return null;
	}
	
	private String getRunScriptVariable(String id) {
		Object[] tmpRef = getTableData(id);
		if (tmpRef != null)
			return (String)tmpRef[0];
		
		return null;
	}
	
	/**
	 * 
	 */
	public StartupManager() {
		try {
			determinePlatform();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
				
		if (isUnixLike == true)
			readUnixStartup(unixScriptPath);
		else
			readWindowsStartup(windowsScriptPath);

	}
	
	/**
	 * 
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
		list.setSelectedIndex(0);
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

	private JPanel getUnavailablePanel() {
		return new JPanel();
	}
	
	JTextField maxHeap;
	
	private JPanel getAdvancedPanel() {
    	List<Component> stuff = new ArrayList<Component>();
		
		String blank = "";
		String heapSize = getPref(PREF_SM_HEAPSIZE, blank);
    	
    	maxHeap = new JTextField(heapSize, 10);
		    	
    	JPanel javaPanel = GuiUtils.vbox(
        	GuiUtils.lLabel("Java VM:"),
        	GuiUtils.doLayout(new Component[] {
        		GuiUtils.rLabel("  Maximum Heap Size:"),
        		GuiUtils.left(maxHeap),
        	}, 2, GuiUtils.WT_N, GuiUtils.WT_N));		
    	     
       	stuff.add(javaPanel);
     
       	return GuiUtils.inset(GuiUtils.topLeft(GuiUtils.doLayout(stuff, 1, GuiUtils.WT_N, GuiUtils.WT_N)), 5);

	}
	
	/**
	 * Return the contents of a given file as a String.
	 * 
	 * @param file The file with the contents people want.
	 * 
	 * @return Null if no valid file, or the contents of said file.
	 */
	private String readFile(String file) {
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
	 * 
	 * @param file
	 */
	private void readUnixStartup(String file) {
		String contents = readFile(file);

		// this isn't so pretty... each one of these calls searches the 
		// entire string.
		Matcher heapSize = RE_GET_HEAP_SIZE.matcher(contents);
		Matcher threadStack = RE_GET_THREAD_STACK.matcher(contents);
		Matcher initHeap = RE_GET_INIT_HEAP.matcher(contents);
		Matcher youngGen = RE_GET_YOUNG_GEN.matcher(contents);
		Matcher collabMode = RE_GET_COLLAB_MODE.matcher(contents);
		Matcher collabPort = RE_GET_COLLAB_PORT.matcher(contents);
		Matcher enableDebug = RE_GET_ENABLE_DEBUG.matcher(contents);

		if (heapSize.find()) {
			System.err.println("heap size=" + heapSize.group(1));
			setPref(PREF_SM_HEAPSIZE, heapSize.group(1));
		}

		if (threadStack.find()) {
			System.err.println("thread stack=" + threadStack.group(1));
			setPref(PREF_SM_THREAD, threadStack.group(1));
		}

		if (initHeap.find()) {
			System.err.println("initial heap="+initHeap.group(1));
			setPref(PREF_SM_INITHEAP, initHeap.group(1));
		}

		if (youngGen.find()) {
			System.err.println("young generation=" + youngGen.group(1));
			setPref(PREF_SM_YOUNGGEN, youngGen.group(1));
		}

		if (collabMode.find()) {
			// this is a boolean and needs to look for collabPort as well
			// need to figure out if there is a default collabPort.
		} else {
			setPref(PREF_SM_COLLAB, false);
			setPref(PREF_SM_COLLAB_PORT, "");
		}

		if (enableDebug.find()) {
			System.err.println("debug enabled");
			setPref(PREF_SM_DEBUG, true);
		} else {
			setPref(PREF_SM_DEBUG, false);
		}
	}

	/**
	 * 
	 * @param file
	 */
	private void readWindowsStartup(String file) {
		String contents = readFile(file);
		
		Matcher heapSize = RE_GET_WIN_HEAP_SIZE.matcher(contents);
		
		if (heapSize.find() == true) {
			System.err.println("windows heap size=" + heapSize.group(1));
			setPref(PREF_SM_HEAPSIZE, heapSize.group(1));
		}
	}

	private Hashtable<String, String> collectPrefs() {
		//List<String> prefs = new ArrayList<String>();
		Hashtable<String, String> prefs = new Hashtable<String, String>();
		StringBuffer heapSizeFlag;
		
		if (isWindows == true)
			heapSizeFlag = new StringBuffer("SET HEAP_SIZE=");
		else
			heapSizeFlag = new StringBuffer("HEAP_SIZE=");
		
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

		String collabMode;
		String collabPort;

		if (getPref(PREF_SM_COLLAB, false) == true) {
			collabMode = "-server";
			collabPort = getPref(PREF_SM_COLLAB_PORT, blank);
		} else {
			collabMode = blank;
			collabPort = blank;
		}

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
		
		prefs.put(PREF_SM_HEAPSIZE, heapSizeFlag.toString());
		prefs.put(PREF_SM_INITHEAP, initHeapFlag.toString());
		prefs.put(PREF_SM_YOUNGGEN, youngGenFlag.toString());
		prefs.put(PREF_SM_THREAD, threadStackFlag.toString());
		prefs.put(PREF_SM_COLLAB, collabModeFlag.toString());
		prefs.put(PREF_SM_COLLAB_PORT, collabPortFlag.toString());
		prefs.put(PREF_SM_DEBUG, debugFlag.toString());

		return prefs;
	}

	/**
	 * 
	 * @param file
	 */
	private void writeUnixStartup(String file) {
		Hashtable<String, String> data = collectPrefs();
		String contents = readFile(file);

		Matcher matcher = RE_SET_HEAP_SIZE.matcher(contents);
		contents = matcher.replaceAll(data.get(PREF_SM_HEAPSIZE));

		matcher = RE_SET_INIT_HEAP.matcher(contents);
		contents = matcher.replaceAll(data.get(PREF_SM_INITHEAP));

		matcher = RE_SET_YOUNG_GEN.matcher(contents);
		contents = matcher.replaceAll(data.get(PREF_SM_YOUNGGEN));

		matcher = RE_SET_THREAD_STACK.matcher(contents);
		contents = matcher.replaceAll(data.get(PREF_SM_THREAD));

		matcher = RE_SET_COLLAB_MODE.matcher(contents);
		contents = matcher.replaceAll(data.get(PREF_SM_COLLAB));

		matcher = RE_SET_COLLAB_PORT.matcher(contents);
		contents = matcher.replaceAll(data.get(PREF_SM_COLLAB_PORT));

		matcher = RE_SET_ENABLE_DEBUG.matcher(contents);
		contents = matcher.replaceAll(data.get(PREF_SM_DEBUG));

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(contents);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * beep beep boop DOOP
	 * @param file
	 */
	private void writeWindowsStartup(String file) {
		Hashtable<String, String> data = collectPrefs();
		String contents = readFile(file);
		
		Matcher matcher = RE_SET_WIN_HEAP_SIZE.matcher(contents);
		contents = matcher.replaceAll(data.get(PREF_SM_HEAPSIZE));
		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(contents);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param id
	 * @param dflt
	 * 
	 * @return
	 */
	private boolean getPref(String id, boolean dflt) {
		if (store.containsKey(id))
			return (Boolean)store.get(id);
		return dflt;
	}
	
	/**
	 * 
	 * @param id
	 * @param dflt
	 * 
	 * @return
	 */
	private String getPref(String id, String dflt) {
		if (store.containsKey(id))
			return (String)store.get(id);
		return dflt;
	}
	
	/**
	 * 
	 * @param id
	 * @param value
	 */
	private void setPref(String id, Object value) {
		store.put(id, value);
	}
	
	/**
	 * 
	 */
	private void setWidgets() {

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
			setPref(PREF_SM_HEAPSIZE, maxHeap.getText());
			if (isUnixLike == true)
				writeUnixStartup(unixScriptPath);
			else
				writeWindowsStartup(windowsScriptPath);
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
