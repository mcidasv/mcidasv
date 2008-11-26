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
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import ucar.unidata.ui.Help;
import ucar.unidata.util.GuiUtils;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVTextField;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;

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
         * Sets the amount of available memory--the args parser knows this
         * 
         * @param megabytes Memory in megabytes
         */
        public void setAvailableMemory(final int megabytes) {
        	availableMemory = megabytes;
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

        JPanel outerPanel = new JPanel();
        
        JPanel startupPanel = new JPanel();
        startupPanel.setBorder(BorderFactory.createTitledBorder("Startup Options"));

        // Build the memory panel
        JPanel heapPanel = McVGuiUtils.makeLabeledComponent(heapSize.getLabel()+":", heapSize.getComponent());
                
        // Build the 3D panel
        JCheckBox use3dCheckBox = (JCheckBox)use3d.getComponent();
        use3dCheckBox.setText(use3d.getLabel());
        JCheckBox joglCheckBox = (JCheckBox)jogl.getComponent();
        joglCheckBox.setText(jogl.getLabel());
        JPanel j3dPanel = McVGuiUtils.makeLabeledComponent("3D:",
        		McVGuiUtils.topBottom(use3dCheckBox, joglCheckBox, null));

        // Build the bundle panel
        JScrollPane startupBundleTree = (JScrollPane)startupBundle.getComponent();
        McVGuiUtils.setComponentWidth(startupBundleTree, Width.DOUBLE);
        JCheckBox defaultBundleCheckBox = (JCheckBox)defaultBundle.getComponent();
        defaultBundleCheckBox.setText(defaultBundle.getLabel());
        JPanel bundlePanel = McVGuiUtils.makeLabeledComponent(startupBundle.getLabel()+":",
        		McVGuiUtils.topBottom(startupBundleTree, defaultBundleCheckBox, McVGuiUtils.Prefer.TOP));

        org.jdesktop.layout.GroupLayout panelLayout = new org.jdesktop.layout.GroupLayout(startupPanel);
        startupPanel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
        	panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
        		.add(heapPanel)
        		.add(j3dPanel)
        		.add(bundlePanel)
        );
        panelLayout.setVerticalGroup(
        	panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(panelLayout.createSequentialGroup()
                .add(heapPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
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
    protected void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        byte[] buf = new byte[1024];
        int length;

        while ((length = in.read(buf)) > 0)
            out.write(buf, 0, length);

        in.close();
        out.close();
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

//    private static class OptionMaster {
    private enum OptionMaster {
        /** The lone OptionMaster instance. */
        INSTANCE;

        // TODO(jon): write CollectionHelpers.zip() and CollectionHelpers.zipWith()
        public final Object[][] blahblah = {
            { "HEAP_SIZE", "Memory", "512m", OptionType.MEMORY, OptionPlatform.ALL, OptionVisibility.VISIBLE },
            { "JOGL_TOGL", "Enable JOGL", "1", OptionType.BOOLEAN, OptionPlatform.UNIXLIKE, OptionVisibility.VISIBLE },
            { "USE_3DSTUFF", "Enable 3D controls", "1", OptionType.BOOLEAN, OptionPlatform.ALL, OptionVisibility.VISIBLE },
            { "DEFAULT_LAYOUT", "Load default layout", "1", OptionType.BOOLEAN, OptionPlatform.ALL, OptionVisibility.VISIBLE },
            { "STARTUP_BUNDLE", "Load bundle", "", OptionType.DIRTREE, OptionPlatform.ALL, OptionVisibility.VISIBLE },
            /**
             * TODO: DAVEP: TomW's windows machine needs SET D3DREND= to work properly.
             * Not sure why, but it shouldn't hurt other users.  Investigate after Alpha10
             */
            { "D3DREND", "  Use Direct3D:", "", OptionType.TEXT, OptionPlatform.WINDOWS, OptionVisibility.HIDDEN },
        };

        /**
         * {@link Option}s can be either platform-specific or applicable to all
         * platforms. Options that are platform-specific still appear in the 
         * UI, but their component is not enabled.
         */
        public enum OptionPlatform { ALL, UNIXLIKE, WINDOWS };

        /**
         * The different types of {@link Option}s.
         * @see TextOption
         * @see BooleanOption
         * @see MemoryOption
         */
        public enum OptionType { TEXT, BOOLEAN, MEMORY, DIRTREE };

        /** 
         * Different ways that an {@link Option} might be displayed.
         */
        public enum OptionVisibility { VISIBLE, HIDDEN };

        /** Maps an option ID to the corresponding object. */
        private final Map<String, Option> optionMap;

        OptionMaster() {
            normalizeUserDirectory();
            optionMap = buildOptions(blahblah);
//            readStartup();
        }

        /**
         * Creates the specified options and returns a mapping of the option ID
         * to the actual {@link Option} object.
         * 
         * @param options An array specifying the {@code Option}s to be built.
         * 
         * @return Mapping of ID to {@code Option}.
         * 
         * @throws AssertionError if the option array contained an entry that
         * this method cannot build.
         */
        private Map<String, Option> buildOptions(final Object[][] options) {
            // TODO(jon): seriously, get that zip stuff working! this array 
            // stuff is BAD.
            Map<String, Option> optMap = new HashMap<String, Option>();

            for (Object[] arrayOption : options) {
                String id = (String)arrayOption[0];
                String label = (String)arrayOption[1];
                String defaultValue = (String)arrayOption[2];
                OptionType type = (OptionType)arrayOption[3];
                OptionPlatform platform = (OptionPlatform)arrayOption[4];
                OptionVisibility visibility = (OptionVisibility)arrayOption[5];

                Option newOption;
                switch (type) {
                    case TEXT:
                        newOption = new TextOption(id, label, defaultValue, 
                            platform, visibility);
                        break;
                    case BOOLEAN:
                        newOption = new BooleanOption(id, label, defaultValue, 
                            platform, visibility);
                        break;
                    case MEMORY:
                        newOption = new MemoryOption(id, label, defaultValue, 
                            platform, visibility);
                        break;
                    case DIRTREE:
                        newOption = new DirectoryOption(id, label, defaultValue, platform, visibility);
                        break;
                    default:
                         throw new AssertionError(type + 
                             " is not known to OptionMaster.buildOptions()");
                }
                optMap.put(id, newOption);
            }
            return optMap;
        }

        /**
         * Converts a {@link Platform} to its corresponding 
         * {@link OptionPlatform} type.
         * 
         * @return The current platform as a {@code OptionPlatform} type.
         * 
         * @throws AssertionError if {@link StartupManager#getPlatform()} 
         * returned something that this method cannot convert.
         */
        // a lame-o hack :(
        protected OptionPlatform convertToOptionPlatform() {
            Platform platform = StartupManager.INSTANCE.getPlatform();
            switch (platform) {
                case WINDOWS: return OptionPlatform.WINDOWS;
                case UNIXLIKE: return OptionPlatform.UNIXLIKE;
                default: 
                    throw new AssertionError("Unknown platform: " + platform);
            }
        }

        /**
         * Returns the {@link Option} mapped to {@code id}.
         * 
         * @param id The ID whose associated {@code Option} is to be returned.
         * 
         * @return Either the {@code Option} associated with {@code id}, or 
         * {@code null} if there was no association.
         */
        public Option getOption(final String id) {
            return optionMap.get(id);
        }

        // TODO(jon): getAllOptions and optionsBy* really need some work.
        // I want to eventually do something like:
        // Collection<Option> = getOpts().byPlatform(WINDOWS, ALL).byType(BOOLEAN).byVis(HIDDEN)
        public Collection<Option> getAllOptions() {
            return Collections.unmodifiableCollection(optionMap.values());
        }

        public Collection<Option> optionsByPlatform(
            final Set<OptionPlatform> platforms) 
        {
            if (platforms == null)
                throw new NullPointerException();

            Collection<Option> allOptions = getAllOptions();
            Collection<Option> filteredOptions = 
                new ArrayList<Option>(allOptions.size());
            for (Option option : allOptions)
                if (platforms.contains(option.getOptionPlatform()))
                    filteredOptions.add(option);
//          return Collections.unmodifiableCollection(filteredOptions);
            return filteredOptions;
        }

        public Collection<Option> optionsByType(final Set<OptionType> types) {
            if (types == null)
                throw new NullPointerException();

            Collection<Option> allOptions = getAllOptions();
            Collection<Option> filteredOptions = 
                new ArrayList<Option>(allOptions.size());
            for (Option option : allOptions)
                if (types.contains(option.getOptionType()))
                    filteredOptions.add(option);
//          return Collections.unmodifiableCollection(filteredOptions);
            return filteredOptions;
        }

        public Collection<Option> optionsByVisibility(
            final Set<OptionVisibility> visibilities) 
        {
            if (visibilities == null)
                throw new NullPointerException();

            Collection<Option> allOptions = getAllOptions();
            Collection<Option> filteredOptions = 
                new ArrayList<Option>(allOptions.size());
            for (Option option : allOptions)
                if (visibilities.contains(option.getOptionVisibility()))
                    filteredOptions.add(option);
//            return Collections.unmodifiableCollection(filteredOptions);
            return filteredOptions;
        }

        private void normalizeUserDirectory() {
            Platform platform = StartupManager.INSTANCE.getPlatform();
            File dir = new File(platform.getUserDirectory());
            File prefs = new File(platform.getUserPrefs());

            if (!dir.exists())
                dir.mkdir();

            if (!prefs.exists()) {
                try {
                    File defaultPrefs = new File(platform.getDefaultPrefs());
                    StartupManager.INSTANCE.copy(defaultPrefs, prefs);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void readStartup() {
            String contents;
            String line;

            File script = 
                new File(StartupManager.INSTANCE.getPlatform().getUserPrefs());
            if (script.getPath().length() == 0)
                return;

            try {
                BufferedReader br = new BufferedReader(new FileReader(script));
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#"))
                        continue;

                    contents = new String(line);
                    String[] chunks = contents.replace("SET ", "").split("=");
                    if (chunks.length == 2) {
                        Option option = getOption(chunks[0]);
                        if (option != null)
                            option.fromPrefsFormat(line);
                    }
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        protected void writeStartup() {
            File script = 
                new File(StartupManager.INSTANCE.getPlatform().getUserPrefs());
            if (script.getPath().length() == 0)
                return;

            // TODO(jon): use filters when you've made 'em less stupid
            String newLine = 
                StartupManager.INSTANCE.getPlatform().getNewLine();
            OptionPlatform currentPlatform = convertToOptionPlatform();
            StringBuilder contents = new StringBuilder();
            for (Object[] arrayOption : blahblah) {
                Option option = getOption((String)arrayOption[0]);
                OptionPlatform platform = option.getOptionPlatform();
                if (platform == OptionPlatform.ALL || platform == currentPlatform)
                    contents.append(option.toPrefsFormat() + newLine);
            }

            try {
                BufferedWriter out = 
                    new BufferedWriter(new FileWriter(script));
                out.write(contents.toString());
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static abstract class Option {
        /**
         * A unique identifier for an option. Should be the same as the 
         * startup variable name found in the startup preference file.
         */
        private final String optionId;

        /** 
         * Brief description of the option. It will appear as the option's 
         * label in the GUI.
         */
        private final String label;

        /** @see OptionMaster.OptionType */
        private final OptionMaster.OptionType optionType;

        /** @see OptionMaster.OptionPlatform */
        private final OptionMaster.OptionPlatform optionPlatform;

        /** @see OptionMaster.OptionVisibility */
        private final OptionMaster.OptionVisibility optionVisibility;

        /**
         * Creates an option that can hold a specified sort of data and that
         * applies to a given platform.
         * 
         * @param id ID used to refer to this option.
         * @param label Text that'll be used as the GUI label for this option
         * @param optionType Type of data this option will represent.
         * @param optionPlatform Platform(s) where this option is applicable.
         * @param optionVisibility Visibility behavior of this option.
         */
        public Option(final String id, final String label, 
            final OptionMaster.OptionType optionType, 
            final OptionMaster.OptionPlatform optionPlatform, 
            final OptionMaster.OptionVisibility optionVisibility) 
        {
            this.optionId = id;
            this.label = label;
            this.optionType = optionType;
            this.optionPlatform = optionPlatform;
            this.optionVisibility = optionVisibility;
        }

        /**
         * Determines if the option applies to the current platform.
         * 
         * @return {@code true} if this option is applicable, {@code false} 
         * otherwise.
         */
        protected boolean onValidPlatform() {
            OptionMaster.OptionPlatform platform = getOptionPlatform();
            if (platform == OptionMaster.OptionPlatform.ALL)
                return true;
            if (platform == OptionMaster.INSTANCE.convertToOptionPlatform())
                return true;
            return false;
        }

        /**
         * Tests the specified string to see if it's valid for the current 
         * platform. Currently strings that contain &quot;SET &quot; 
         * [ note the space ] are considered to be Windows-only, while strings 
         * lacking &quot;SET &quot; are considered Unix-like.
         * 
         * @param text The string to test.
         * 
         * @return Whether or not the string is valid.
         */
        private boolean isValidPrefFormat(final String text) {
            assert text != null;
            boolean hasSet = text.contains("SET ");
            boolean isWin = (StartupManager.INSTANCE.getPlatform() == StartupManager.Platform.WINDOWS);
            return (isWin == hasSet);
        }

        /**
         * Returns the type of option that this option is.
         * 
         * @return The option type.
         * 
         * @see OptionMaster.OptionType
         */
        public OptionMaster.OptionType getOptionType() {
            return optionType;
        }

        /**
         * Returns the platform(s) to which this option applies.
         * 
         * @return The option's platform.
         * 
         * @see OptionMaster.OptionPlatform
         */
        public OptionMaster.OptionPlatform getOptionPlatform() {
            return optionPlatform;
        }

        
        public OptionMaster.OptionVisibility getOptionVisibility() {
            return optionVisibility;
        }

        /**
         * Returns the ID used when referring to this option.
         * 
         * @return The option's ID.
         */
        public String getOptionId() {
            return optionId;
        }

        /**
         * Returns a brief description of this option. Mostly useful for 
         * providing a GUI label.
         * 
         * @return The option's label.
         */
        public String getLabel() {
            return label;
        }

        public void fromPrefsFormat(final String text) {
            if (!isValidPrefFormat(text))
                throw new IllegalArgumentException("Incorrect syntax for this platform: " + text);

            String copy = new String(text);
            if (StartupManager.INSTANCE.getPlatform() == StartupManager.Platform.WINDOWS)
                copy = copy.replace("SET ", "");

            String[] chunks = copy.split("=");
            if (chunks.length == 2 && chunks[0].equals(optionId))
                setValue(chunks[1]);
            else
                setValue("");
        }

        public String toPrefsFormat() {
            StringBuilder str = new StringBuilder(optionId);
            if (StartupManager.INSTANCE.getPlatform() == 
                StartupManager.Platform.WINDOWS) 
            {
                str.insert(0, "SET ");
            }
            return str.append("=").append(getValue()).toString();
        }

        /**
         * Returns the GUI component that represents the option. 
         * {@link BooleanOption}s are represented by a {@link JCheckBox}, while
         * {@link TextOption}s appear as a {@link JTextField}.
         * 
         * @return The GUI representation of this option.
         */
        public abstract JComponent getComponent();

        /**
         * Returns the value of the option. Note that {@link BooleanOption}s
         * return either "0" or "1".
         * 
         * @return The current value of the option.
         */
        public abstract String getValue();

        /**
         * Forces the value of the option to the data specified. Note that 
         * {@link BooleanOption}s accept either "0", or "1".
         * 
         * @param value New value to use.
         */
        public abstract void setValue(final String value);

        /**
         * Friendly string representation of the option.
         * 
         * @return String containing relevant info about the option.
         * 
         * @see TextOption#toString()
         * @see BooleanOption#toString()
         */
        public abstract String toString();
    }

    private static class DirectoryOption extends Option {
        private String value = "";
        public DirectoryOption(final String id, final String label, final String defaultValue, final OptionMaster.OptionPlatform optionPlatform, final OptionMaster.OptionVisibility optionVisibility) {
            super(id, label, OptionMaster.OptionType.DIRTREE, optionPlatform, optionVisibility);
            setValue(defaultValue);
        }

        // it would be good to add helpful tooltip messages, like:
        // Directory: <path>
        // Zipped IDV Bundle: <path>
        // McIDAS-V Bundle: <path>
        // maybe icons too?
        // needs to filter for known bundle types
        private static void exploreDirectory(final String directory, final DefaultMutableTreeNode parent) {
            for (File f : new File(directory).listFiles()) {
                DefaultMutableTreeNode current = new DefaultMutableTreeNode(f.getName());
                if (f.isDirectory()) {
                    parent.add(current);
                    exploreDirectory(f.getPath(), current);
                } else {
                    parent.add(current);
                }
            }
        }


        public JComponent getComponent() {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Managed Bundles");
            final JTree tree = new JTree(root);
            tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(final TreeSelectionEvent e) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
                    System.err.println("Selected="+node);
                    setValue(node.toString());
                }
            });
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            JScrollPane scroller = new JScrollPane(tree);
            exploreDirectory(StartupManager.INSTANCE.getPlatform().getUserBundles(), root);
            tree.expandRow(0);
            scroller.setPreferredSize(new Dimension(300,300));
            return scroller;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String newValue) {
            value = newValue;
        }

        public String toString() {
            return String.format("[DirectoryOption@%x: optionId=%s, value=%s]", hashCode(), getOptionId(), getValue());
        }
    }

    private static class TextOption extends Option {
        private String value = "";

        public TextOption(final String id, final String label, 
            final String defaultValue, 
            final OptionMaster.OptionPlatform optionPlatform,
            final OptionMaster.OptionVisibility optionVisibility) 
        {
            super(id, label, OptionMaster.OptionType.TEXT, optionPlatform, optionVisibility);
            setValue(defaultValue);
        }

        public JComponent getComponent() {
            final JTextField tf = new JTextField(getValue(), 10);
            tf.addKeyListener(new KeyAdapter() {
                public void keyReleased(final KeyEvent e) {
                    setValue(tf.getText());
                }
            });
            if (!onValidPlatform()) {
                tf.setEnabled(false);
            }
            return tf;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String newValue) {
            value = newValue;
        }

        public String toString() {
            return String.format("[TextOption@%x: optionId=%s, value=%s]", 
                hashCode(), getOptionId(), getValue());
        }
    }

    private static class MemoryOption extends Option implements ActionListener {
        public enum Prefix {
//            NONE("", "bytes"),
//            KILO("K", "kilobytes"),
            MEGA("M", "megabytes"),
            GIGA("G", "gigabytes"),
            TERA("T", "terabytes"),
            PERCENT("P", "percent");

            private final String javaChar;
            private final String name;

            private Prefix(final String javaChar, final String name) {
                this.javaChar = javaChar;
                this.name = name;
            }

            public String getJavaChar() {
                return javaChar.toUpperCase();
            }

            public String getName() {
                return name;
            }

            public String getJavaFormat(final String value) {
                long longVal = Long.parseLong(value);
                return longVal + javaChar;
            }

            @Override public String toString() {
                return name;
            }
        }

        private enum State { 
            VALID(Color.BLACK, Color.WHITE),
            WARN(Color.BLACK, new Color(255, 255, 204)),
            ERROR(Color.WHITE, Color.PINK);

            private final Color foreground;
            private final Color background;

            private State(final Color foreground, final Color background) {
                this.foreground = foreground;
                this.background = background;
            }
            
            public Color getForeground() { return foreground; }
            public Color getBackground() { return background; }
        }

        private final static Prefix[] PREFIXES = { Prefix.MEGA, Prefix.GIGA, Prefix.TERA };
        private Prefix currentPrefix = Prefix.MEGA;

        private static final Pattern MEMSTRING = 
            Pattern.compile("^(\\d+)([M|G|T|P]?)$", Pattern.CASE_INSENSITIVE);

        private final String defaultPrefValue;
        private String value = "512M"; // bootstrap

        private JRadioButton jrbSlider = new JRadioButton();
        private JRadioButton jrbNumber = new JRadioButton();
        private ButtonGroup jtbBg = GuiUtils.buttonGroup(jrbSlider, jrbNumber);
        
        private JPanel sliderPanel = new JPanel();
        private JLabel sliderLabel = new JLabel();
        private JSlider slider = new JSlider();
        
        private JPanel textPanel = new JPanel();
        private McVTextField text = new McVTextField();
        private JComboBox memVals = new JComboBox(PREFIXES);
        private String initTextValue = value;
        private Prefix initPrefixValue = currentPrefix;
        
        private int minSliderValue = 10;
        private int maxSliderValue = 80;
        private int initSliderValue = minSliderValue;
        
        private int maxmem = getMaximumHeapSize();

        private State currentState = State.VALID;
        
        private boolean doneInit = false;

        public MemoryOption(final String id, final String label, 
            final String defaultValue,
            final OptionMaster.OptionPlatform optionPlatform,
            final OptionMaster.OptionVisibility optionVisibility) 
        {
            super(id, label, OptionMaster.OptionType.MEMORY, optionPlatform, optionVisibility);
            try {
                setValue(defaultValue);
            } catch (IllegalArgumentException e) {
                setValue(value);
            }
            text.setAllow(new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});
            defaultPrefValue = defaultValue;
            jrbSlider.setActionCommand("slider");
            jrbSlider.addActionListener(this);
            jrbNumber.setActionCommand("number");
            jrbNumber.addActionListener(this);
            sliderPanel.setEnabled(false);
            textPanel.setEnabled(false);
        }

        private String[] getNames(final Prefix[] arr) {
            assert arr != null;
            String[] newArr = new String[arr.length];
            for (int i = 0; i < arr.length; i++)
                newArr[i] = arr[i].getName();
            return newArr;
        }

        private void setState(final State newState) {
            assert newState != null : newState;
            currentState = newState;
            text.setForeground(currentState.getForeground());
            text.setBackground(currentState.getBackground());
        }

        private boolean isValid() {
            return currentState == State.VALID;
        }
        
        private boolean isSlider() {
        	return currentPrefix.equals(Prefix.PERCENT);
        }
        
        public void actionPerformed(ActionEvent e) {
        	if (e.getActionCommand().equals("slider")) {
        		GuiUtils.enableTree(sliderPanel, true);
        		GuiUtils.enableTree(textPanel, false);
        	}
        	else {
        		GuiUtils.enableTree(sliderPanel, false);
        		GuiUtils.enableTree(textPanel, true);
        	}
        }
        
        public ChangeListener percentListener = new ChangeListener() {
        	public void stateChanged(ChangeEvent evt) {
        		if (!sliderPanel.isEnabled()) return;
        		int value = ((JSlider)evt.getSource()).getValue();
        		setValue(value + Prefix.PERCENT.getJavaChar());
        		text.setText("" + Math.round(value / 100.0 * maxmem));
        	}
        };
        
        private void handleNewValue(final JTextField field, final JComboBox box) {
        	if (!textPanel.isEnabled()) return;
            assert field != null;
            assert box != null;
            
            try {
                String newValue = field.getText();
                String huh = ((Prefix)box.getSelectedItem()).getJavaFormat(newValue);

                if (!isValid())
                    setState(State.VALID);

                setValue(huh);
            } catch (IllegalArgumentException e) {
                setState(State.ERROR);
                text.setToolTipText("This value must be an integer greater than zero.");
            }
        }

        public JComponent getComponent() {
        	JPanel topPanel = GuiUtils.hbox(jrbSlider, getSliderComponent());
            JPanel bottomPanel = GuiUtils.hbox(jrbNumber, getTextComponent());
            if (isSlider()) {
        		GuiUtils.enableTree(sliderPanel, true);
        		GuiUtils.enableTree(textPanel, false);
            }
            else {
        		GuiUtils.enableTree(sliderPanel, false);
        		GuiUtils.enableTree(textPanel, true);
            }
            doneInit = true;
            return McVGuiUtils.topBottom(topPanel, bottomPanel, null);
        }
        
        public JComponent getSliderComponent() {
        	sliderLabel = new JLabel("Use "+initSliderValue+"% ");
        	JLabel postLabel = new JLabel(" of available memory (" + maxmem +"mb)");
            JComponent[] sliderComps = GuiUtils.makeSliderPopup(minSliderValue, maxSliderValue+1, initSliderValue, percentListener);
            slider = (JSlider) sliderComps[1];
            slider.setMinorTickSpacing(5);
            slider.setMajorTickSpacing(10);
            slider.setSnapToTicks(true);
            slider.setExtent(1);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            sliderComps[0].setToolTipText("Set maximum memory by percent");
            sliderPanel = GuiUtils.hbox(sliderLabel, sliderComps[0], postLabel);
            return sliderPanel;
        }

        public JComponent getTextComponent() {
            text.setText(initTextValue);
            text.addKeyListener(new KeyAdapter() {
                public void keyReleased(final KeyEvent e) {
                    handleNewValue(text, memVals);
                }
            });
            memVals.setSelectedItem(initPrefixValue);
            memVals.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    handleNewValue(text, memVals);
                }
            });
            McVGuiUtils.setComponentWidth(text, McVGuiUtils.Width.ONEHALF);
            McVGuiUtils.setComponentWidth(memVals, McVGuiUtils.Width.ONEHALF);
            textPanel = GuiUtils.hbox(text, memVals);
            return textPanel;
        }

        public String toString() {
            return String.format(
                "[MemoryOption@%x: value=%s, currentPrefix=%s]", 
                hashCode(), value, currentPrefix);
        }

        public String getValue() {
            if (!isValid())
                return defaultPrefValue;
            return currentPrefix.getJavaFormat(value);
        }

        // overridden so that any illegal vals coming *out of* a runMcV.prefs
        // can be replaced with a legal val.
        @Override public void fromPrefsFormat(final String prefText) {
            try {
                super.fromPrefsFormat(prefText);
            } catch (IllegalArgumentException e) {
                setValue("512M");
            }
        }

        public void setValue(final String newValue) {
            Matcher m = MEMSTRING.matcher(newValue);
            if (!m.matches())
                throw new IllegalArgumentException("Badly formatted memory string: "+newValue);

            String quantity = m.group(1);
            String prefix = m.group(2);

            int intVal = Integer.parseInt(quantity);
            if (intVal <= 0)
                throw new IllegalArgumentException("Memory cannot be less than or equal to zero: "+newValue);
            if (prefix.length() == 0)
                prefix = "M";

            for (Prefix tmp : PREFIXES) {
                if (prefix.toUpperCase().equals(tmp.getJavaChar())) {
                    value = quantity;
                    currentPrefix = tmp;
                    
                    // Work around all the default settings going on
                    initSliderValue = minSliderValue;
                    initPrefixValue = currentPrefix;
                    initTextValue = value;
                    
                    if (maxmem>0) {
                    	int multiplier = 1;
                    	if (currentPrefix.equals(Prefix.GIGA)) multiplier=1024;
                    	else if (currentPrefix.equals(Prefix.TERA)) multiplier=1024 * 1024;
                    	initSliderValue = (int)Math.round(Integer.parseInt(value) * 100.0 * multiplier / maxmem);
                    	initSliderValue = Math.max(Math.min(initSliderValue, maxSliderValue), minSliderValue);
                    	slider.setValue(initSliderValue);
                    	sliderLabel.setText("Use "+initSliderValue+"% ");
                    }
                    if (!doneInit) jrbNumber.setSelected(true);
                    return;
                }
            }
            
            if (prefix.toUpperCase().equals(Prefix.PERCENT.getJavaChar())) {
            	value = quantity;
            	currentPrefix = Prefix.PERCENT;
            	
                // Work around all the default settings going on
            	initSliderValue = Integer.parseInt(value);
            	initPrefixValue = Prefix.MEGA;
        		initTextValue = "" + (int)Math.round(initSliderValue * maxmem / 100.0);
        		
            	sliderLabel.setText("Use "+value+"% ");
            	if (maxmem>0) {
            		memVals.setSelectedItem(initPrefixValue);
            		text.setText(initTextValue);
            	}
            	if (!doneInit) jrbSlider.setSelected(true);
            	return;
            }

            throw new IllegalArgumentException("Could not find matching memory prefix for \""+prefix+"\" in string: "+newValue);
        }
    }

    private static class BooleanOption extends Option {
        private String value = "0";

        public BooleanOption(final String id, final String label, 
            final String defaultValue, 
            final OptionMaster.OptionPlatform optionPlatform,
            final OptionMaster.OptionVisibility optionVisibility) 
        {
            super(id, label, OptionMaster.OptionType.BOOLEAN, optionPlatform, optionVisibility);
            setValue(defaultValue);
        }

        public JComponent getComponent() {
            final JCheckBox cb = new JCheckBox();
            cb.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    setValue(cb.isSelected() ? "1" : "0");
                }
            });
            boolean booleanValue = false;
            if (value.equals("1"))
                booleanValue = true;
            cb.setSelected(booleanValue);
            if (!onValidPlatform()) {
                cb.setEnabled(false);
            }
            return cb;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String newValue) {
            if (newValue.equals("0") || newValue.equals("1"))
                value = newValue;
        }

        public String toString() {
            return String.format("[BooleanOption@%x: optionId=%s, value=%s]", 
                hashCode(), getOptionId(), getValue());
        }
    }
    
    public static int getMaximumHeapSize() {
    	int sysmem = StartupManager.INSTANCE.getPlatform().getAvailableMemory();
    	if (System.getProperty("os.arch").indexOf("64") < 0) return Constants.MAX_MEMORY_32BIT;
    	return sysmem;
    }

    public static String getUserPath(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-userpath") && (i+1) < args.length)
                return args[i+1];
        }
        return null;
    }
    
    public static int getAvailableMemory(String[] args) {
    	int sysmem = 0;
    	for (int i = 0; i < args.length; i++) {
    		if (args[i].indexOf("-D"+Constants.PROP_SYSMEM+"=") >= 0) {
    			try {
    				sysmem = Integer.parseInt(args[i].substring(Constants.PROP_SYSMEM.length()+3));
    				return sysmem;
    			}
    			catch (NumberFormatException e) {
    				return 0;
    			}
    		}
    	}
    	return sysmem;
    }

    public static void main(String[] args) {
        StartupManager sm = StartupManager.INSTANCE;

        String userPath = getUserPath(args);
        if (userPath != null)
            sm.getPlatform().setUserDirectory(getUserPath(args));
        
       	sm.getPlatform().setAvailableMemory(getAvailableMemory(args));

        sm.createDisplay();
    }
}