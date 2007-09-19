package edu.wisc.ssec.mcidasv;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.wisc.ssec.mcidasv.startupmanager.StartupManager.IconCellRenderer;

import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.PreferenceManager;

/**
 * This class is responsible for the preference dialog and
 * managing general preference state.
 * A  set of {@link ucar.unidata.xml.PreferenceManager}-s are added
 * into the dialog. This class then constructs a tabbed pane
 * window, one pane for each PreferenceManager.
 * On  the user's Ok or Apply the dialog will
 * have each PreferenceManager apply its preferences.
 *
 * @author IDV development team
 */
public class McIdasPreferenceManager extends IdvPreferenceManager 
implements ListSelectionListener {

	private Hashtable<String, Container> prefMap = 
		new Hashtable<String, Container>();

	private Hashtable<String, URL> iconMap = new Hashtable<String, URL>();
	
	private Dimension preferredSize;
	
	private List<PreferenceManager> managers = 
		new ArrayList<PreferenceManager>();
	
	private List<Object> dataList = new ArrayList<Object>();
	
	private GridBagConstraints cons;
	private JList labelList;
	private DefaultListModel listModel;
	private JScrollPane listScrollPane;
	private JSplitPane splitPane;
	
	private JPanel paneHolder;
	private JPanel pane;
	
    public McIdasPreferenceManager(IntegratedDataViewer idv) {
        super(idv);
        init();
        loadIcons();
    }

    // this is straight up UGLY
    private void initPane() {
    	System.err.printf("initPane()\n");
    	
    	listModel = new DefaultListModel();
    	labelList = new JList(listModel);
    	labelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		labelList.setCellRenderer(new IconCellRenderer());
		labelList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() == false) {
					System.err.println("before: " + listScrollPane.getPreferredSize() + " " + splitPane.getSize());
					splitPane.setRightComponent(getSelectedPanel());
					System.err.println("after: " + listScrollPane.getPreferredSize() + " " + splitPane.getSize());
				}
			}
		});
		listScrollPane = new JScrollPane(labelList);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		
		splitPane.setResizeWeight(0.0);
		splitPane.setLeftComponent(listScrollPane);
		System.err.println("Current: " + listScrollPane.getSize());
		System.err.println("Preferred: " + listScrollPane.getPreferredSize());
		
		// need something more reliable than MAGICAL DIMENSIONS.
		listScrollPane.setMinimumSize(new Dimension(161, 275));
		pane = new JPanel(new GridBagLayout());
		pane.add(splitPane);
		paneHolder.add(pane, BorderLayout.WEST);
    }
    
    // will eventually need to preempt IDV so that our JList will appear.
    public void add(String tabLabel, String description, PreferenceManager listener, Container panel, Object data) {    	
    	if (prefMap.containsKey(tabLabel) == true)
    		return;
    	
    	managers.add(listener);
    	dataList.add(data);
    	
    	System.err.printf("Label: %s, Desc: %s\n", tabLabel, description);
    	
    	prefMap.put(tabLabel, panel);
     	if (pane == null)
     		initPane();
     	
     	JLabel label = new JLabel();
     	label.setText(tabLabel);
     	label.setIcon(new ImageIcon(iconMap.get(tabLabel)));
     	listModel.addElement(label);
     	
     	// this SUCKS!
     	labelList.setSelectedIndex(0);
     	splitPane.setRightComponent(prefMap.get("General"));     	
	}

    /**
     * Apply the preferences
     *
     * @return ok
     */
    public boolean apply() {
        try {
            for (int i = 0; i < managers.size(); i++) {
                PreferenceManager manager =
                    (PreferenceManager) managers.get(i);
                manager.applyPreference(getStore(), dataList.get(i));
            }
            getStore().save();
            return true;
        } catch (Exception exc) {
            LogUtil.logException("Error applying preferences", exc);
            return false;
        }
    }    
    
    /**
     * Select a list item and its corresponding panel that both live within the 
     * preference window JList.
     * 
     * @param labelName The "name" of the JLabel within the JList.
     */
    public void selectListItem(String labelName) {
    	show();
    	toFront();
    	
    	if (pane == null)
    		return;
    	
    	for (int i = 0; i < listModel.getSize(); i++) {
    		String labelText = ((JLabel)listModel.get(i)).getText();
    		if (StringUtil.stringMatch(labelText, labelName)) {
    			labelList.setSelectedIndex(i);
    			return;
    		}
    	}
    }
    
    /**
     * 
     * @param tabNameToShow
     */
    public void showTab(String tabNameToShow) {
    	selectListItem(tabNameToShow);
    }
    
    // TODO: remove?
    public void show() {
    	System.err.printf("show()\n");
    	super.show();
    }

    // TODO: remove?
    public void close() {
    	System.err.printf("close()\n");
    	super.close();
    }
    
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) {
			System.err.println("before: " + listScrollPane.getPreferredSize() + " " + splitPane.getSize());
			splitPane.setRightComponent(getSelectedPanel());
			System.err.println("after: " + listScrollPane.getPreferredSize() + " " + splitPane.getSize());
		}
	}
    
	private Container getSelectedPanel() {
		String key = ((JLabel)listModel.getElementAt(labelList.getSelectedIndex())).getText();
		System.err.printf("Selected label: %s (%d)\n", key, labelList.getSelectedIndex());
		return prefMap.get(key);
	}
    
    private void loadIcons() {
    	String label = "General";
    	String icon = "/edu/wisc/ssec/mcidasv/resources/icons/range-bearing32.png";
    	URL tmp = getClass().getResource(icon);
    	iconMap.put(label, tmp);
    	
    	label = "Formats & Data";
    	iconMap.put(label, tmp);
    	
    	label = "View";
    	iconMap.put(label, tmp);
    	
    	label = "Navigation";
    	iconMap.put(label, tmp);
    	
    	label = "Toolbar";
    	iconMap.put(label, tmp);
    	
    	label = "Available Choosers";
    	iconMap.put(label, tmp);
    	
    	label = "Available Displays";
    	iconMap.put(label, tmp);
    	
    	label = "ADDE Servers";
    	iconMap.put(label, tmp);
    	
    }
    
    public void init() {
    	System.err.printf("init()\n");
    	paneHolder = new JPanel(new BorderLayout());
        Component buttons = GuiUtils.makeApplyOkHelpCancelButtons(this);
        contents = GuiUtils.centerBottom(paneHolder, buttons);    	
    }

    /**
     * Init the preference gui
     */
    protected void initPreferences() {
        System.err.printf("initPreferences()\n");
    	super.initPreferences();
        ServerPreferenceManager mspm = new ServerPreferenceManager(getIdv());
        mspm.addServerPreferences(this);
    }

	public class IconCellRenderer extends DefaultListCellRenderer {
		
		/**
		 * Extends the default list cell renderer to use icons in addition to
		 * the typical text.
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
		 * I wear some pretty fancy pants, so you'd better believe that I'm
		 * going to enable fancy-pants text antialiasing.
		 * 
		 * @param g The graphics object that we'll use as a base.
		 */
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D)g;
			
			g2d.setRenderingHints(getRenderingHints());
			
			super.paintComponent(g2d);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static RenderingHints getRenderingHints() {
		RenderingHints hints = new RenderingHints(null);
		for (int i = 0; i < RENDER_HINTS.length; i++)
			hints.put(RENDER_HINTS[i][0], RENDER_HINTS[i][1]);
		return hints;
	}
	
	/** Desired rendering hints with their desired values. */
	public static final Object[][] RENDER_HINTS = {
		{RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON},
		{RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY},
		{RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON}
	};
    
}

