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

package edu.wisc.ssec.mcidasv.startupmanager;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.LayoutStyle;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import ucar.unidata.ui.Help;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.StringUtil;

import edu.wisc.ssec.mcidasv.ArgumentManager;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.startupmanager.options.BooleanOption;
import edu.wisc.ssec.mcidasv.startupmanager.options.DirectoryOption;
import edu.wisc.ssec.mcidasv.startupmanager.options.LoggerLevelOption;
import edu.wisc.ssec.mcidasv.startupmanager.options.MemoryOption;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster;
import edu.wisc.ssec.mcidasv.startupmanager.options.TextOption;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

/**
 * Manages the McIDAS-V startup options in a context that is completely free
 * from the traditional IDV/McIDAS-V overhead.
 */
public class StartupManager implements edu.wisc.ssec.mcidasv.Constants {
    
    // TODO(jon): replace
    public static final String[][] PREF_PANELS = {
        { Constants.PREF_LIST_GENERAL, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/mcidasv-round32.png" },
        { Constants.PREF_LIST_VIEW, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/tab-new32.png" },
        { Constants.PREF_LIST_TOOLBAR, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/application-x-executable32.png" },
        { Constants.PREF_LIST_DATA_CHOOSERS, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/preferences-desktop-remote-desktop32.png" },
        { Constants.PREF_LIST_ADDE_SERVERS, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/applications-internet32.png" },
        { Constants.PREF_LIST_AVAILABLE_DISPLAYS, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/video-display32.png" },
        { Constants.PREF_LIST_NAV_CONTROLS, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/input-mouse32.png" },
        { Constants.PREF_LIST_FORMATS_DATA,"/edu/wisc/ssec/mcidasv/resources/icons/prefs/preferences-desktop-theme32.png" },
        { Constants.PREF_LIST_ADVANCED, "/edu/wisc/ssec/mcidasv/resources/icons/prefs/applications-internet32.png" },
    };
    
    // TODO(jon): replace
    public static final Object[][] RENDER_HINTS = {
        { RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON },
        { RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY },
        { RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON },
    };
    
    /** usage message */
    public static final String USAGE_MESSAGE =
        "Usage: runMcV-Prefs <args>";
    
    /** Path to the McIDAS-V help set within {@literal mcv_userguide.jar}. */
    private static final String HELP_PATH = "/docs/userguide";
    
    /** ID of the startup prefs help page. */
    private static final String HELP_TARGET = "idv.tools.preferences.advancedpreferences";
    
    /** The type of platform as reported by {@link #determinePlatform()}. */
    private final Platform platform = determinePlatform();
    
    /** Cached copy of the application rendering hints. */
    public static final RenderingHints HINTS = getRenderingHints();
    
    /** Contains the list of the different preference panels. */
    private final JList panelList = new JList(new DefaultListModel());
    
    /** Panel containing the startup options. */
    private JPanel ADVANCED_PANEL;
    
    /**
     * Panel to use for all other preference panels while running startup 
     * manager.
     */
    private JPanel BAD_CHOICE_PANEL;
    
    /** Contains the various buttons (Apply, Ok, Help, Cancel). */
    private JPanel COMMAND_ROW_PANEL;
    
    private static StartupManager instance;
    
    private StartupManager() {
        
    }
    
    public static StartupManager getInstance() {
        if (instance == null) {
            instance = new StartupManager();
        }
        return instance;
    }
    
    /**
     * Creates and returns the rendering hints for the GUI. 
     * Built from {@link #RENDER_HINTS}
     * 
     * @return Hints to use when displaying the GUI.
     */
    public static RenderingHints getRenderingHints() {
        RenderingHints hints = new RenderingHints(null);
        for (int i = 0; i < RENDER_HINTS.length; i++)
            hints.put(RENDER_HINTS[i][0], RENDER_HINTS[i][1]);
        return hints;
    }
    
    /**
     * Figures out the type of platform. Queries the &quot;os.name&quot; 
     * system property to determine the platform type.
     * 
     * @return {@link Platform#UNIXLIKE} or {@link Platform#WINDOWS}.
     */
    private Platform determinePlatform() {
        String os = System.getProperty("os.name");
        if (os == null) {
            throw new RuntimeException();
        }
        if (os.startsWith("Windows")) {
            return Platform.WINDOWS;
        } else {
            return Platform.UNIXLIKE;
        }
    }
    
    /** 
     * Returns either {@link Platform#UNIXLIKE} or 
     * {@link Platform#WINDOWS}.
     * 
     * @return The platform as determined by {@link #determinePlatform()}.
     */
    public Platform getPlatform() {
        return platform;
    }
    
    /**
     * Saves the changes to the preferences and quits. Unlike the other button
     * handling methods, this one is public. This was done so that the advanced
     * preferences (within McIDAS-V) can force an update to the startup prefs.
     */
    public void handleApply() {
        OptionMaster.getInstance().writeStartup();
    }
    
    /**
     * Saves the preference changes.
     */
    protected void handleOk() {
        OptionMaster.getInstance().writeStartup();
        System.exit(0);
    }
    
    /** 
     * Shows the startup preferences help page.
     */
    protected void handleHelp() {
        Help.setTopDir(HELP_PATH);
        Help.getDefaultHelp().gotoTarget(HELP_TARGET);
    }
    
    /**
     * Simply quits the program.
     */
    protected void handleCancel() {
        System.exit(0);
    }
    
    /**
     * Returns the preferences panel that corresponds with the user's 
     * {@code JList} selection.
     * 
     * <p>In the context of the startup manager, this means that any 
     * {@code JList} selection <i>other than</i> {@literal "Advanced"} will
     * return the results of {@link #getUnavailablePanel()}. Otherwise the
     * results of {@link #getAdvancedPanel(boolean)} will be returned.
     * 
     * @return Either the advanced preferences panel or an 
     * {@literal "unavailable"}, depending upon the user's selection.
     */
    private Container getSelectedPanel() {
        ListModel listModel = panelList.getModel();
        int index = panelList.getSelectedIndex();
        if (index == -1) {
            return getAdvancedPanel(true);
        }
        String key = ((JLabel)listModel.getElementAt(index)).getText();
        if (!Constants.PREF_LIST_ADVANCED.equals(key)) {
            return getUnavailablePanel();
        }
        return getAdvancedPanel(true);
    }
    
    /**
     * Creates and returns a dummy panel.
     * 
     * @return Panel containing only a note about 
     * &quot;options unavailable.&quot;
     */
    private JPanel buildUnavailablePanel() {
        JPanel panel = new JPanel();
        panel.add(new JLabel("These options are unavailable in this context"));
        return panel;
    }
    
    /**
     * Creates and returns the advanced preferences panel.
     * 
     * @return Panel with all of the various startup options.
     */
    private JPanel buildAdvancedPanel() {
        OptionMaster optMaster = OptionMaster.getInstance();
        MemoryOption heapSize = optMaster.getMemoryOption("HEAP_SIZE");
        BooleanOption jogl = optMaster.getBooleanOption("JOGL_TOGL");
        BooleanOption use3d = optMaster.getBooleanOption("USE_3DSTUFF");
        BooleanOption defaultBundle = optMaster.getBooleanOption("DEFAULT_LAYOUT");
        BooleanOption useDirect3d = optMaster.getBooleanOption("D3DREND");
        BooleanOption useCmsCollector = optMaster.getBooleanOption("USE_CMSGC");
        BooleanOption useNpot = optMaster.getBooleanOption("USE_NPOT");
        BooleanOption useGeometryByRef = optMaster.getBooleanOption("USE_GEOBYREF");
        BooleanOption useImageByRef = optMaster.getBooleanOption("USE_IMAGEBYREF");
        DirectoryOption startupBundle = optMaster.getDirectoryOption("STARTUP_BUNDLE");
        TextOption jvmArgs = optMaster.getTextOption("JVM_OPTIONS");
        LoggerLevelOption logLevel = optMaster.getLoggerLevelOption("LOG_LEVEL");
        
        JPanel startupPanel = new JPanel();
        startupPanel.setBorder(BorderFactory.createTitledBorder("Startup Options"));
        
        // Build the memory panel
        JPanel heapPanel = McVGuiUtils.makeLabeledComponent(heapSize.getLabel()+':', heapSize.getComponent());
        
        // Build the 3D panel
        JCheckBox use3dCheckBox = use3d.getComponent();
        use3dCheckBox.setText(use3d.getLabel());
        final JCheckBox joglCheckBox = jogl.getComponent();
        joglCheckBox.setText(jogl.getLabel());
        final JCheckBox direct3dBox = useDirect3d.getComponent();
        direct3dBox.setText(useDirect3d.getLabel());
        
        JPanel internalPanel = McVGuiUtils.topCenterBottom(use3dCheckBox, joglCheckBox, direct3dBox);
        JPanel j3dPanel = McVGuiUtils.makeLabeledComponent("3D:", internalPanel);
        
        // Build the bundle panel
        JPanel startupBundlePanel = startupBundle.getComponent();
        JCheckBox defaultBundleCheckBox = defaultBundle.getComponent();
        defaultBundleCheckBox.setText(defaultBundle.getLabel());
        JPanel bundlePanel = McVGuiUtils.makeLabeledComponent(startupBundle.getLabel()+":",
            McVGuiUtils.topBottom(startupBundlePanel, defaultBundleCheckBox, McVGuiUtils.Prefer.TOP));
            
        JCheckBox useCmsCollectorCheckBox = useCmsCollector.getComponent();
        useCmsCollectorCheckBox.setText(useCmsCollector.getLabel());
        
        JCheckBox useGeometryByRefCheckBox = useGeometryByRef.getComponent();
        useGeometryByRefCheckBox.setText(useGeometryByRef.getLabel());
        
        JCheckBox useImageByRefCheckBox = useImageByRef.getComponent();
        useImageByRefCheckBox.setText(useImageByRef.getLabel());
        
        JCheckBox useNpotCheckBox = useNpot.getComponent();
        useNpotCheckBox.setText(useNpot.getLabel());
        
        JComboBox logLevelComboBox = logLevel.getComponent();

        JPanel logLevelPanel = McVGuiUtils.makeLabeledComponent(logLevel.getLabel()+":", logLevelComboBox);
        
        JPanel miscPanel = McVGuiUtils.makeLabeledComponent("Misc:", useCmsCollectorCheckBox);

        JTextField jvmArgsField = jvmArgs.getComponent();
        JPanel jvmPanel = McVGuiUtils.makeLabeledComponent("Java Flags:", jvmArgsField);

        Component[] visadComponents = new Component[] {
                useGeometryByRefCheckBox,
                useImageByRefCheckBox,
                useNpotCheckBox
        };
        
        JPanel visadPanel = McVGuiUtils.makeLabeledComponent("VisAD:", McVGuiUtils.vertical(visadComponents));
        
        GroupLayout panelLayout = new GroupLayout(startupPanel);
        startupPanel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(heapPanel)
                .addComponent(j3dPanel)
                .addComponent(bundlePanel)
                .addComponent(visadPanel)
                .addComponent(logLevelPanel)
                .addComponent(miscPanel)
                .addComponent(jvmPanel)
        );
        panelLayout.setVerticalGroup(
            panelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(panelLayout.createSequentialGroup()
                .addComponent(heapPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(bundlePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(j3dPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(visadPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(logLevelPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(miscPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jvmPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            )
        );
        return startupPanel;
    }
    
    /**
     * Builds and returns a {@link JPanel} containing the various buttons that
     * control the startup manager. These buttons offer identical 
     * functionality to those built by the IDV's preference manager code.
     * 
     * @return A {@code JPanel} containing the following types of buttons:
     * {@link ApplyButton}, {@link OkButton}, {@link HelpButton}, 
     * and {@link CancelButton}.
     * 
     * @see GuiUtils#makeApplyOkHelpCancelButtons(ActionListener)
     */
    private JPanel buildCommandRow() {
        JPanel panel = new JPanel(new FlowLayout());
        // Apply doesn't really mean anything in standalone mode...
//        panel.add(new ApplyButton());
        panel.add(new OkButton());
        panel.add(new HelpButton());
        panel.add(new CancelButton());
        panel = McVGuiUtils.makePrettyButtons(panel);
        return panel;
    }
    
    /**
     * Returns the advanced preferences panel. Differs from the 
     * {@link #buildAdvancedPanel()} in that a panel isn't created, unless
     * {@code forceBuild} is {@code true}.
     * 
     * @param forceBuild Always rebuilds the advanced panel if {@code true}.
     * 
     * @return Panel containing the startup options.
     */
    public JPanel getAdvancedPanel(final boolean forceBuild) {
        if (forceBuild || ADVANCED_PANEL == null) {
            OptionMaster.getInstance().readStartup();
            ADVANCED_PANEL = buildAdvancedPanel();
        }
        return ADVANCED_PANEL;
    }
    
    public JPanel getUnavailablePanel() {
        if (BAD_CHOICE_PANEL == null) {
            BAD_CHOICE_PANEL = buildUnavailablePanel();
        }
        return BAD_CHOICE_PANEL;
    }
    
    /**
     * Returns a panel containing the Apply/Ok/Help/Cancel buttons.
     * 
     * @return Panel containing the the command row.
     */
    public JPanel getCommandRow() {
        if (COMMAND_ROW_PANEL == null) {
            COMMAND_ROW_PANEL = buildCommandRow();
        }
        return COMMAND_ROW_PANEL;
    }
    
    /**
     * Build and display the startup manager window.
     */
    protected void createDisplay() {
        DefaultListModel listModel = (DefaultListModel)panelList.getModel();
        
        for (int i = 0; i < PREF_PANELS.length; i++) {
            ImageIcon icon = new ImageIcon(getClass().getResource(PREF_PANELS[i][1]));
            JLabel label = new JLabel(PREF_PANELS[i][0], icon, SwingConstants.LEADING);
            listModel.addElement(label);
        }
        
        JScrollPane scroller = new JScrollPane(panelList);
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.0);
        splitPane.setLeftComponent(scroller);
        scroller.setMinimumSize(new Dimension(166, 319));
        
        panelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panelList.setSelectedIndex(PREF_PANELS.length - 1);
        panelList.setVisibleRowCount(PREF_PANELS.length);
        panelList.setCellRenderer(new IconCellRenderer());
        
        panelList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    splitPane.setRightComponent(getSelectedPanel());
                }
            }
        });
        
        splitPane.setRightComponent(getSelectedPanel());
        
        JFrame frame = new JFrame("User Preferences");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(splitPane);
        frame.getContentPane().add(getCommandRow(), BorderLayout.PAGE_END);
        
        frame.pack();
        frame.setVisible(true);
    }
    
    /**
     * Copies a file.
     * 
     * @param src The file to copy.
     * @param dst The path to the copy of {@code src}.
     * 
     * @throws IOException If there was a problem while attempting to copy.
     */
    public void copy(final File src, final File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        
        byte[] buf = new byte[1024];
        int length;
        
        while ((length = in.read(buf)) > 0) {
            out.write(buf, 0, length);
        }
        in.close();
        out.close();
    }
    
    public static class TreeCellRenderer extends DefaultTreeCellRenderer {
        @Override public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded,
                leaf, row, hasFocus);
                
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
            
            File f = (File)node.getUserObject();
            String path = f.getPath();
            
            if (f.isDirectory()) {
                setToolTipText("Bundle Directory: " + path);
            } else if (ArgumentManager.isZippedBundle(path)) {
                setToolTipText("Zipped Bundle: " + path);
            } else if (ArgumentManager.isXmlBundle(path)) {
                setToolTipText("XML Bundle: " + path);
            } else {
                setToolTipText("Unknown file type: " + path);
            }
            setText(f.getName().replace(f.getParent(), ""));
            return this;
        }
    }
    
    public static class IconCellRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(JList list, 
            Object value, int index, boolean isSelected, boolean cellHasFocus) 
        {
            super.getListCellRendererComponent(list, value, index, isSelected, 
                cellHasFocus);
                
            if (value instanceof JLabel) {
                setText(((JLabel)value).getText());
                setIcon(((JLabel)value).getIcon());
            }
            
            return this;
        }
        
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D)g;
            g2d.setRenderingHints(StartupManager.HINTS);
            super.paintComponent(g2d);
        }
    }
    
    private static abstract class CommandButton extends JButton 
        implements ActionListener 
    {
        public CommandButton(final String label) {
            super(label);
            McVGuiUtils.setComponentWidth(this);
            addActionListener(this);
        }
        
        @Override public void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D)g;
            g2d.setRenderingHints(StartupManager.HINTS);
            super.paintComponent(g2d);
        }
        
        abstract public void actionPerformed(final ActionEvent e);
    }
    
    private static class ApplyButton extends CommandButton {
        public ApplyButton() {
            super("Apply");
        }
        public void actionPerformed(final ActionEvent e) {
            StartupManager.getInstance().handleApply();
        }
    }
    
    private static class OkButton extends CommandButton {
        public OkButton() {
            super("OK");
        }
        public void actionPerformed(final ActionEvent e) {
            StartupManager.getInstance().handleOk();
        }
    }
    
    private static class HelpButton extends CommandButton {
        public HelpButton() {
            super("Help");
        }
        public void actionPerformed(final ActionEvent e) {
            StartupManager.getInstance().handleHelp();
        }
    }
    
    private static class CancelButton extends CommandButton {
        public CancelButton() {
            super("Cancel");
        }
        public void actionPerformed(final ActionEvent e) {
            StartupManager.getInstance().handleCancel();
        }
    }
    
    public static Properties getDefaultProperties() {
        Properties props = new Properties();
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Mac OS X")) {
            props.setProperty("userpath", String.format("%s%s%s%s%s", System.getProperty("user.home"), File.separator, "Documents", File.separator, Constants.USER_DIRECTORY_NAME));
        } else {
            props.setProperty("userpath", String.format("%s%s%s", System.getProperty("user.home"), File.separator, Constants.USER_DIRECTORY_NAME));
        }
        props.setProperty(Constants.PROP_SYSMEM, "0");
        return props;
    }
    
    /**
     * Extract any command-line properties and their corresponding values.
     * 
     * <p>May print out usage information if a badly formatted 
     * {@literal "property=value"} pair is encountered, or when an unknown 
     * argument is found (depending on value of the {@code ignoreUnknown} 
     * parameter). 
     * 
     * <p><b>NOTE:</b> {@code null} is not a permitted value for any parameter.
     * 
     * @param ignoreUnknown Whether or not to handle unknown arguments.
     * @param fromStartupManager Whether or not this call originated from 
     * {@code startupmanager.jar}.
     * @param args Array containing command-line arguments.
     * @param defaults Default parameter values.
     * 
     * @return Command-line arguments as a collection of property identifiers
     * and values.
     */
    public static Properties getArgs(final boolean ignoreUnknown, 
        final boolean fromStartupManager, final String[] args, 
        final Properties defaults) 
    {
        Properties props = new Properties(defaults);
        for (int i = 0; i < args.length; i++) {
            
            // handle property definitions
            if (args[i].startsWith("-D")) {
                List<String> l = StringUtil.split(args[i].substring(2), "=");
                if (l.size() == 2) {
                    props.setProperty(l.get(0), l.get(1));
                } else {
                    usage("Invalid property:" + args[i]);
                }
            }
            
            // handle userpath changes
            else if (ARG_USERPATH.equals(args[i]) && (i+1) < args.length) {
                props.setProperty("userpath", args[++i]);
            }
            
            // handle help requests
            else if (ARG_HELP.equals(args[i]) && (fromStartupManager)) {
                System.err.println(USAGE_MESSAGE);
                System.err.println(getUsageMessage());
                System.exit(1);
            }
            
            // bail out for unknown args, unless we don't care!
            else if (!ignoreUnknown){
                usage("Unknown argument: " + args[i]);
            }
        }
        return props;
    }
    
    public static int getMaximumHeapSize() {
        int sysmem = StartupManager.getInstance().getPlatform().getAvailableMemory();
        if (sysmem > Constants.MAX_MEMORY_32BIT &&
                System.getProperty("os.arch").indexOf("64") < 0) {
            return Constants.MAX_MEMORY_32BIT;
        }
        return sysmem;
    }
    
    /**
     * Print out the command line usage message and exit. Taken entirely from
     * {@link ucar.unidata.idv.ArgsManager}.
     * 
     * @param err The usage message
     */
    private static void usage(final String err) {
        String msg = USAGE_MESSAGE;
        msg = msg + "\n" + getUsageMessage();
        LogUtil.userErrorMessage(err + "\n" + msg);
        System.exit(1);
    }
    
    /**
     * Return the command line usage message.
     * 
     * @return The usage message
     */
    protected static String getUsageMessage() {
        return "\t"+ARG_HELP+"  (this message)\n"+
               "\t"+ARG_USERPATH+"  <user directory to use>\n"+
               "\t-Dpropertyname=value  (Define the property value)\n";
    }
    
    /**
     * Applies the command line arguments to the startup preferences. This function
     * is mostly useful because it allows us to supply an arbitrary {@code args} array,
     * link in {@link edu.wisc.ssec.mcidasv.McIDASV#main(String[])}.
     * 
     * @param ignoreUnknown If {@code true} ignore any parameters that do not 
     * apply to the startup manager. If {@code false} the non-applicable 
     * parameters should signify an error.
     * @param fromStartupManager Whether or not this call originated from the 
     * startup manager (rather than preferences).
     * @param args Incoming command line arguments. Cannot be {@code null}.
     * 
     * @throws NullPointerException if {@code args} is null.
     * 
     * @see #getArgs(boolean, boolean, String[], Properties)
     */
    public static void applyArgs(final boolean ignoreUnknown, final boolean fromStartupManager, final String[] args) throws IllegalArgumentException {
        if (args == null) {
            throw new NullPointerException("Arguments list cannot be null");
        }
        StartupManager sm = StartupManager.getInstance();
        Platform platform = sm.getPlatform();
        
        Properties props = getArgs(ignoreUnknown, fromStartupManager, args, getDefaultProperties());
        platform.setUserDirectory(props.getProperty("userpath"));
        platform.setAvailableMemory(props.getProperty(Constants.PROP_SYSMEM));
    }
    
    public static void main(String[] args) {
        applyArgs(false, true, args);
        StartupManager.getInstance().createDisplay();
    }
}
