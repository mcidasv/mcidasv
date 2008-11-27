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

import edu.wisc.ssec.mcidasv.ArgumentManager;
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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ucar.unidata.ui.Help;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.StringUtil;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.startupmanager.options.BooleanOption;
import edu.wisc.ssec.mcidasv.startupmanager.options.DirectoryOption;
import edu.wisc.ssec.mcidasv.startupmanager.options.MemoryOption;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster;
import edu.wisc.ssec.mcidasv.startupmanager.options.SliderOption;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

// using an enum to enforce singleton-ness is a hack, but it's been pretty 
// effective. OptionMaster is used in a similar way. The remaining enums are 
// used in the more traditional fashion.
public enum StartupManager implements edu.wisc.ssec.mcidasv.Constants {
    /** Lone instance of the startup manager. */
    INSTANCE;

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

    public enum Platform {
        /** Instance of unix-specific platform information. */
        UNIXLIKE("/", "runMcV.prefs", "\n"),

        /** Instance of windows-specific platform information. */
        WINDOWS("\\", "runMcV-Prefs.bat", "\r\n");

        /** Path to the user's {@literal ".mcidasv"} directory. */
        private String userDirectory;

        /** The path to the user's copy of the startup preferences. */
        private String userPrefs;

        /** Path to the preference file that ships with McIDAS-V. */
        private final String defaultPrefs;

        /** Holds the platform's representation of a new line. */
        private final String newLine;

        /** Directory delimiter for the current platform. */
        private final String pathSeparator;
        
        /** Total amount of memory avilable in megabytes */
        private int availableMemory = 0;

        /**
         * Initializes the platform-specific paths to the different files 
         * required by the startup manager.
         * 
         * @param pathSeparator Character that delimits directories. On Windows
         * this will be {@literal \\}, while on Unix-style systems, it will be
         * {@literal /}.
         * @param defaultPrefs The path to the preferences file that ships with
         * McIDAS-V.
         * @param newLine Character(s!) that represent a new line for this 
         * platform.
         * 
         * @throws NullPointerException if either {@code pathSeparator} or 
         * {@code defaultPrefs} are null.
         * 
         * @throws IllegalArgumentException if either {@code pathSeparator} or
         * {@code defaultPrefs} are an empty string.
         */
        Platform(final String pathSeparator, final String defaultPrefs, 
            final String newLine) 
        {
            if (pathSeparator == null || defaultPrefs == null)
                throw new NullPointerException("");
            if (pathSeparator.length() == 0 || defaultPrefs.length() == 0)
                throw new IllegalArgumentException("");

            this.userDirectory = System.getProperty("user.home") + pathSeparator + ".mcidasv";
            this.userPrefs = userDirectory + pathSeparator + defaultPrefs;
            this.defaultPrefs = defaultPrefs;
            this.newLine = newLine;
            this.pathSeparator = pathSeparator;
        }

        /**
         * Sets the path to the user's .mcidasv directory explicitly.
         * 
         * @param path New path.
         */
        public void setUserDirectory(final String path) {
            userDirectory = path;
            userPrefs = userDirectory + pathSeparator + defaultPrefs;
        }

        /**
         * Sets the amount of available memory. {@code megabytes} must be 
         * greater than or equal to zero.
         * 
         * @param megabytes Memory in megabytes
         * 
         * @throws NullPointerException if {@code megabytes} is {@code null}.
         * @throws IllegalArgumentException if {@code megabytes} is less than
         * zero or does not represent an integer.
         * 
         * @see StartupManager#getArgs(String[], Properties)
         */
        public void setAvailableMemory(final String megabytes) {
            if (megabytes == null)
                throw new NullPointerException("Available memory cannot be null");

            try {
                int test = Integer.parseInt(megabytes);
                if (test < 0) 
                    throw new IllegalArgumentException("Available memory must be a non-negative integer, not \""+megabytes+"\"");

                availableMemory = test;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Could not convert \""+megabytes+"\" to a non-negative integer");
            }
        }

        /**
         * Returns the path to the user's {@literal ".mcidasv"} directory.
         * 
         * @return Path to the user's directory.
         */
        public String getUserDirectory() {
            return userDirectory;
        }

        public String getUserBundles() {
            return getUserDirectory()+pathSeparator+"bundles";
        }
        
        /**
         * Returns the amount of available memory in megabytes
         * 
         * @return Available memory in megabytes
         */
        public int getAvailableMemory() {
        	return availableMemory;
        }

        /**
         * Returns the path of user's copy of the startup preferences.
         * 
         * @return Path to the user's startup preferences file.
         */
        public String getUserPrefs() {
            return userPrefs;
        }

        /**
         * Returns the path of the startup preferences included in the McIDAS-V
         * distribution. Mostly useful for normalizing the user 
         * directory.
         * 
         * @return Path to the default startup preferences.
         * 
         * @see OptionMaster#normalizeUserDirectory()
         */
        public String getDefaultPrefs() {
            return defaultPrefs;
        }

        /**
         * Returns the platform's notion of a new line.
         * 
         * @return Unix-like: {@literal \n}; Windows: {@literal \r\n}.
         */
        public String getNewLine() {
            return newLine;
        }

        /**
         * Returns a brief summary of the platform specific file locations. 
         * Please note that the format and contents are subject to change.
         * 
         * @return String that looks like 
         * {@code [Platform@HASHCODE: defaultPrefs=..., userDirectory=..., 
         * userPrefs=...]}
         */
        @Override public String toString() {
            return String.format(
                "[Platform@%x: defaultPrefs=%s, userDirectory=%s, userPrefs=%s]",
                hashCode(), defaultPrefs, userDirectory, userPrefs);
        }
    }

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
        if (os == null)
            throw new RuntimeException();

        if (os.startsWith("Windows"))
            return Platform.WINDOWS;
        else
            return Platform.UNIXLIKE;
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
        OptionMaster.INSTANCE.writeStartup();
    }

    /**
     * Saves the preference changes.
     */
    protected void handleOk() {
        OptionMaster.INSTANCE.writeStartup();
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
     * 
     * @return
     */
    private Container getSelectedPanel() {
        ListModel listModel = panelList.getModel();
        int index = panelList.getSelectedIndex();
        if (index == -1)
            return getAdvancedPanel(true);

        String key = ((JLabel)listModel.getElementAt(index)).getText();
        if (!key.equals(Constants.PREF_LIST_ADVANCED))
            return getUnavailablePanel();

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
        OptionMaster optMaster = OptionMaster.INSTANCE;
        MemoryOption heapSize = (MemoryOption)optMaster.getOption("HEAP_SIZE");
        BooleanOption jogl = (BooleanOption)optMaster.getOption("JOGL_TOGL");
        BooleanOption use3d = (BooleanOption)optMaster.getOption("USE_3DSTUFF");
        BooleanOption defaultBundle = (BooleanOption)optMaster.getOption("DEFAULT_LAYOUT");
        DirectoryOption startupBundle = (DirectoryOption)optMaster.getOption("STARTUP_BUNDLE");
//        SliderOption sliderTest = (SliderOption)optMaster.getOption("SLIDER_TEST");

        JPanel outerPanel = new JPanel();
        
        JPanel startupPanel = new JPanel();
        startupPanel.setBorder(BorderFactory.createTitledBorder("Startup Options"));

        // Build the memory panel
        JPanel heapPanel = McVGuiUtils.makeLabeledComponent(heapSize.getLabel()+":", heapSize.getComponent());

//        JPanel testPanel = McVGuiUtils.makeLabeledComponent(sliderTest.getLabel()+":", sliderTest.getComponent());

        // Build the 3D panel
        JCheckBox use3dCheckBox = (JCheckBox)use3d.getComponent();
        use3dCheckBox.setText(use3d.getLabel());
        JCheckBox joglCheckBox = (JCheckBox)jogl.getComponent();
        joglCheckBox.setText(jogl.getLabel());
        JPanel j3dPanel = McVGuiUtils.makeLabeledComponent("3D:",
            McVGuiUtils.topBottom(use3dCheckBox, joglCheckBox, null));

        // Build the bundle panel
        JScrollPane startupBundleTree = (JScrollPane)startupBundle.getComponent();
        JCheckBox defaultBundleCheckBox = (JCheckBox)defaultBundle.getComponent();
        defaultBundleCheckBox.setText(defaultBundle.getLabel());
        JPanel bundlePanel = McVGuiUtils.makeLabeledComponent(startupBundle.getLabel()+":",
            McVGuiUtils.topBottom(startupBundleTree, defaultBundleCheckBox, McVGuiUtils.Prefer.TOP));

        org.jdesktop.layout.GroupLayout panelLayout = new org.jdesktop.layout.GroupLayout(startupPanel);
        startupPanel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(heapPanel)
//                .add(testPanel)
                .add(j3dPanel)
                .add(bundlePanel)
        );
        panelLayout.setVerticalGroup(
            panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panelLayout.createSequentialGroup()
                .add(heapPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
//                .add(testPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
//                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(j3dPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(bundlePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(outerPanel);
        outerPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(startupPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(startupPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        
        return outerPanel;
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
            OptionMaster.INSTANCE.readStartup();
            ADVANCED_PANEL = buildAdvancedPanel();
        }
        return ADVANCED_PANEL;
    }

    public JPanel getUnavailablePanel() {
        if (BAD_CHOICE_PANEL == null)
            BAD_CHOICE_PANEL = buildUnavailablePanel();
        return BAD_CHOICE_PANEL;
    }

    /**
     * Returns a panel containing the Apply/Ok/Help/Cancel buttons.
     * 
     * @return Panel containing the the command row.
     */
    public JPanel getCommandRow() {
        if (COMMAND_ROW_PANEL == null)
            COMMAND_ROW_PANEL = buildCommandRow();
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
                if (!e.getValueIsAdjusting())
                    splitPane.setRightComponent(getSelectedPanel());
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

        while ((length = in.read(buf)) > 0)
            out.write(buf, 0, length);

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

            if (f.isDirectory())
                setToolTipText("Bundle Directory: " + path);
            else if (ArgumentManager.isZippedBundle(path))
                setToolTipText("Zipped Bundle: " + path);
            else if (ArgumentManager.isXmlBundle(path))
                setToolTipText("XML Bundle: " + path);
            else
                setToolTipText("Unknown file type: " + path);

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
            StartupManager.INSTANCE.handleApply();
        }
    }

    private static class OkButton extends CommandButton {
        public OkButton() {
            super("OK");
        }
        public void actionPerformed(final ActionEvent e) {
            StartupManager.INSTANCE.handleOk();
        }
    }

    private static class HelpButton extends CommandButton {
        public HelpButton() {
            super("Help");
        }
        public void actionPerformed(final ActionEvent e) {
            StartupManager.INSTANCE.handleHelp();
        }
    }

    private static class CancelButton extends CommandButton {
        public CancelButton() {
            super("Cancel");
        }
        public void actionPerformed(final ActionEvent e) {
            StartupManager.INSTANCE.handleCancel();
        }
    }

    public static Properties getDefaultProperties() {
        Properties props = new Properties();
        props.setProperty("userpath", String.format("%s%s.mcidasv", System.getProperty("user.home"), File.separator));
        props.setProperty(Constants.PROP_SYSMEM, "0");
        return props;
    }

    public static Properties getArgs(final String[] args, final Properties defaults) {
        Properties props = new Properties(defaults);
        for (int i = 0; i < args.length; i++) {

            // handle property definitions
            if (args[i].startsWith("-D")) {
                List<String> l = StringUtil.split(args[i].substring(2), "=");
                if (l.size() == 2)
                    props.setProperty(l.get(0), l.get(1));
                else
                    usage("Invalid property:" + args[i]);
            }

            // handle userpath changes
            else if (args[i].equals(ARG_USERPATH) && (i+1) < args.length) {
                props.setProperty("userpath", args[i+1]);
            }

            // handle help requests
            else if (args[i].equals(ARG_HELP)) {
                System.err.println(USAGE_MESSAGE);
                System.err.println(getUsageMessage());
                System.exit(1);
            }

            // bail out for unknown args!
            else {
                usage("Unknown argument: " + args[i]);
            }
        }
        return props;
    }

    public static int getMaximumHeapSize() {
        int sysmem = StartupManager.INSTANCE.getPlatform().getAvailableMemory();
        if (System.getProperty("os.arch").indexOf("64") < 0) return Constants.MAX_MEMORY_32BIT;
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

    public static void main(String[] args) {
        StartupManager sm = StartupManager.INSTANCE;
        Platform platform = sm.getPlatform();

        Properties props = getArgs(args, getDefaultProperties());
        platform.setUserDirectory(props.getProperty("userpath"));
        platform.setAvailableMemory(props.getProperty(Constants.PROP_SYSMEM));

        sm.createDisplay();
    }
}
