/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2015
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
package edu.wisc.ssec.mcidasv.chooser;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeListener;

import java.io.IOException;

import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.w3c.dom.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bushe.swing.event.annotation.EventTopicSubscriber;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import edu.wisc.ssec.mcidasv.util.pathwatcher.DirectoryWatchService;
import edu.wisc.ssec.mcidasv.util.pathwatcher.SimpleDirectoryWatchService;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Position;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.TextColor;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;

/**
 * {@code FileChooser} is another {@literal "UI nicety"} extension. The main
 * difference is that this class allows {@code choosers.xml} to specify a
 * boolean attribute, {@code "selectdatasourceid"}. If disabled or not present,
 * a {@code FileChooser} will behave exactly like a standard 
 * {@link FileChooser}.
 * 
 * <p>If the attribute is present and enabled, the {@code FileChooser}'s 
 * data source type will automatically select the 
 * {@link ucar.unidata.data.DataSource} corresponding to the chooser's 
 * {@code "datasourceid"} attribute.
 */
public class FileChooser extends ucar.unidata.idv.chooser.FileChooser implements Constants {

    private static final Logger logger =
        LoggerFactory.getLogger(FileChooser.class);

    /** 
     * Chooser attribute that controls selecting the default data source.
     * @see #selectDefaultDataSource
     */
    public static final String ATTR_SELECT_DSID = "selectdatasourceid";

    /** Default data source ID for this chooser. Defaults to {@code null}. */
    private final String defaultDataSourceId;

    /** 
     * Whether or not to select the data source corresponding to 
     * {@link #defaultDataSourceId} within the {@link JComboBox} returned by
     * {@link #getDataSourcesComponent()}. Defaults to {@code false}.
     */
    private final boolean selectDefaultDataSource;

    /**
     * If there is a default data source ID, get the combo box display value
     */
    private String defaultDataSourceName;
    
    /** Different subclasses can use the combobox of data source ids */
    private JComboBox sourceComboBox;
    
    /**
     * Get a handle on the actual file chooser
     */
    protected JFileChooser fileChooser;

    /**
     * The panels that might need to be enabled/disabled
     */
    protected JPanel topPanel = new JPanel();
    protected JPanel centerPanel = new JPanel();
    protected JPanel bottomPanel = new JPanel();
    
    /**
     * Boolean to tell if the load was initiated from the load button
     * (as opposed to typing in a filename... we need to capture that)
     */
    protected Boolean buttonPressed = false;
    
    /**
     * Get a handle on the IDV
     */
    protected IntegratedDataViewer idv = getIdv();

    /** Directory monitoring service. May be {@code null}. */
    protected DirectoryWatchService watchService;

    /** This is mostly used to preemptively null-out the listener. */
    protected DirectoryWatchService.OnFileChangeListener watchListener;


    /**
     * Creates a {@code FileChooser} and bubbles up {@code mgr} and 
     * {@code root} to {@link FileChooser}.
     * 
     * @param mgr Global IDV chooser manager.
     * @param root XML representing this chooser.
     */
    public FileChooser(final IdvChooserManager mgr, final Element root) {
        super(mgr, root);

        AnnotationProcessor.process(this);

        String id = XmlUtil.getAttribute(root, ATTR_DATASOURCEID, (String)null);
        defaultDataSourceId = (id != null) ? id.toLowerCase() : id;

        selectDefaultDataSource =
            XmlUtil.getAttribute(root, ATTR_SELECT_DSID, false);
        
    }
    
    /**
     * Label for {@link #getDataSourcesComponent()} selector.
     *
     * @return {@code String} to use as the label for data type selector.
     */
    protected String getDataSourcesLabel() {
        return "Data Type:";
    }

    /**
     * Overridden so that McIDAS-V can attempt auto-selecting the default data
     * source type.
     */
    @Override
    protected JComboBox getDataSourcesComponent() {
        sourceComboBox = getDataSourcesComponent(true);
        if (selectDefaultDataSource && defaultDataSourceId != null) {
            Map<String, Integer> ids = comboBoxContents(sourceComboBox);
            if (ids.containsKey(defaultDataSourceId)) {
                sourceComboBox.setSelectedIndex(ids.get(defaultDataSourceId));
                defaultDataSourceName = sourceComboBox.getSelectedItem().toString();
                sourceComboBox.setVisible(false);
            }
        }
        return sourceComboBox;
    }

    /**
     * Maps data source IDs to their index within {@code box}. This method is 
     * only applicable to {@link JComboBox}es created for {@link FileChooser}s.
     * 
     * @param box Combo box containing relevant data source IDs and indices. 
     * 
     * @return A mapping of data source IDs to their offset within {@code box}.
     */
    private static Map<String, Integer> comboBoxContents(final JComboBox box) {
        assert box != null;
        Map<String, Integer> map = new HashMap<String, Integer>();
        for (int i = 0; i < box.getItemCount(); i++) {
            Object o = box.getItemAt(i);
            if (!(o instanceof TwoFacedObject))
                continue;
            TwoFacedObject tfo = (TwoFacedObject)o;
            map.put(TwoFacedObject.getIdString(tfo), i);
        }
        return map;
    }
    
    /**
     * If the dataSources combo box is non-null then
     * return the data source id the user selected.
     * Else, return null
     *
     * @return Data source id
     */
    protected String getDataSourceId() {
        return getDataSourceId(sourceComboBox);
    }
    
    /**
     * Get the accessory component
     *
     * @return the component
     */
    protected JComponent getAccessory() {
        return GuiUtils.left(
            GuiUtils.inset(
                FileManager.makeDirectoryHistoryComponent(
                    fileChooser, false), new Insets(13, 0, 0, 0)));
    }

    /**
     * Override the base class method to catch the do load
     */
    public void doLoadInThread() {
        selectFiles(fileChooser.getSelectedFiles(),
                    fileChooser.getCurrentDirectory());
    }

    /**
     * Override the base class method to catch the do update
     */
    public void doUpdate() {
        fileChooser.rescanCurrentDirectory();
    }
    
    /**
     * Allow multiple file selection.  Override if necessary.
     *
     * @return Always returns {@code true}.
     */
    protected boolean getAllowMultiple() {
        return true;
    }
    
    /**
     * Set whether the user has made a selection that contains data.
     *
     * @param have   true to set the haveData property.  Enables the
     *               loading button
     */
    public void setHaveData(boolean have) {
        super.setHaveData(have);
        updateStatus();
    }
    
    /**
     * Set the status message appropriately
     */
    protected void updateStatus() {
        super.updateStatus();
        if(!getHaveData()) {
            if (getAllowMultiple())
                setStatus("Select one or more files");
            else
                setStatus("Select a file"); 
        }
    }
        
    /**
     * Get the top components for the chooser
     *
     * @param comps  the top component
     */
    protected void getTopComponents(List comps) {
        Element chooserNode = getXmlNode();

        // Force ATTR_DSCOMP to be false before calling super.getTopComponents
        // We call getDataSourcesComponent later on
        boolean dscomp = XmlUtil.getAttribute(chooserNode, ATTR_DSCOMP, true);
        XmlUtil.setAttributes(chooserNode, new String[] { ATTR_DSCOMP, "false" });
        super.getTopComponents(comps);
        if (dscomp) XmlUtil.setAttributes(chooserNode, new String[] { ATTR_DSCOMP, "true" });
    }
    
    /**
     * Get the top panel for the chooser
     * @return the top panel
     */
    protected JPanel getTopPanel() {
        List topComps  = new ArrayList();
        getTopComponents(topComps);
        if (topComps.size() == 0) return null;
        JPanel topPanel = GuiUtils.left(GuiUtils.doLayout(topComps, 0, GuiUtils.WT_N, GuiUtils.WT_N));
        topPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        
        return McVGuiUtils.makeLabeledComponent("Options:", topPanel);
    }
    
    /**
     * Get the bottom panel for the chooser
     * @return the bottom panel
     */
    protected JPanel getBottomPanel() {
        return null;
    }
        
    /**
     * Get the center panel for the chooser
     * @return the center panel
     */
    protected JPanel getCenterPanel() {
        Element chooserNode = getXmlNode();

        fileChooser = doMakeFileChooser(getPath());
        fileChooser.setPreferredSize(new Dimension(300, 300));
        fileChooser.setMultiSelectionEnabled(getAllowMultiple());

        fileChooser.addPropertyChangeListener(
            JFileChooser.DIRECTORY_CHANGED_PROPERTY,
            createPropertyListener()
        );
        
        List filters = new ArrayList();
        String filterString = XmlUtil.getAttribute(chooserNode, ATTR_FILTERS, (String) null);

        filters.addAll(getDataManager().getFileFilters());
        if (filterString != null) {
            filters.addAll(PatternFileFilter.createFilters(filterString));
        }

        if ( !filters.isEmpty()) {
            for (int i = 0; i < filters.size(); i++) {
                fileChooser.addChoosableFileFilter((FileFilter) filters.get(i));
            }
            fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
        }

        JPanel centerPanel;
        JComponent accessory = getAccessory();
        if (accessory == null) {
            centerPanel = GuiUtils.center(fileChooser);
        } else {
            centerPanel = GuiUtils.centerRight(fileChooser, GuiUtils.top(accessory));
        }
        centerPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        setHaveData(false);
        return McVGuiUtils.makeLabeledComponent("Files:", centerPanel);
    }

    /**
     * Creates a {@link PropertyChangeListener} that listens for
     * {@link JFileChooser#DIRECTORY_CHANGED_PROPERTY}.
     *
     * <p>This is used to disable directory monitoring in directories not
     * being looked at, as well as enabling monitoring of the directory the
     * user has chosen.</p>
     *
     * @return {@code PropertyChangeListener} that listens for
     * {@code JFileChooser} directory changes.
     */
    protected PropertyChangeListener createPropertyListener() {
        return evt -> {
            logger.trace("prop change: evt={}", evt);
            String name = evt.getPropertyName();
            if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(name)) {
                String newPath = evt.getNewValue().toString();
                logger.trace("old: '{}', new: '{}'", getPath(), newPath);
                handleChangeWatchService(newPath);
            }
        };
    }

    /**
     * Change the path that the file chooser is presenting to the user.
     *
     * <p>This value will be written to the user's preferences so that the user
     * can pick up where they left off after restarting McIDAS-V.</p>
     *
     * @param newPath Path to set.
     */
    public void setPath(String newPath) {
        String id = PREF_DEFAULTDIR + getId();
        idv.getStateManager().writePreference(id, newPath);
    }

    /**
     * See the javadoc for {@link #getPath(String)}.
     *
     * <p>The difference between the two is that this method passes the value
     * of {@code System.getProperty("user.home")} to {@link #getPath(String)}
     * as the default value.</p>
     *
     * @return Path to use for the chooser.
     */
    public String getPath() {
        return getPath(System.getProperty("user.home"));
    }

    /**
     * Get the path the {@link JFileChooser} should be using.
     *
     * <p>If the path in the user's preferences is {@code null}
     * (or does not exist), {@code defaultValue} will be returned.</p>
     *
     * @param defaultValue Default path to use if there is a {@literal "bad"}
     *                     path in the user's preferences.
     *                     Cannot be {@code null}.
     *
     * @return Path to use for the chooser.
     *
     * @throws NullPointerException if {@code defaultValue} is {@code null}.
     */
    public String getPath(final String defaultValue) {
        Objects.requireNonNull(defaultValue, "Default value may not be null");
        String tempPath = (String)idv.getPreference(PREF_DEFAULTDIR + getId());
        if ((tempPath == null) || !Paths.get(tempPath).toFile().exists()) {
            tempPath = defaultValue;
        }
        return tempPath;
    }

    /**
     * Respond to path changes in the {@code JFileChooser}.
     *
     * <p>This method will disable monitoring of the previous path and then
     * enable monitoring of {@code newPath}.</p>
     *
     * @param newPath
     */
    public void handleChangeWatchService(final String newPath) {
        if (watchService != null && watchListener != null) {
            logger.trace("now watching '{}'", newPath);

            setPath(newPath);

            handleStopWatchService(
                Constants.EVENT_FILECHOOSER_STOP,
                "changed directory"
            );

            handleStartWatchService(
                Constants.EVENT_FILECHOOSER_START,
                "new directory"
            );
        }
    }

    /**
     * Begin monitoring the directory returned by {@link #getPath()} for
     * changes.
     *
     * @param topic Artifact from {@code EventBus} annotation. Not used.
     * @param reason Optional {@literal "Reason"} for starting.
     *               Helpful for logging.
     */
    @EventTopicSubscriber(topic=Constants.EVENT_FILECHOOSER_START)
    public void handleStartWatchService(final String topic,
                                        final Object reason)
    {
        boolean offscreen = getIdv().getArgsManager().getIsOffScreen();
        boolean initDone = getIdv().getHaveInitialized();
        String watchPath = getPath();
        if (!offscreen && initDone) {
            try {
                watchService = createWatcher(watchPath, getFilePattern());
                watchService.start();
                logger.trace("watching '{}' (reason: '{}')", watchPath, reason);
            } catch (IOException e) {
                logger.error("error creating watch service", e);
            }
        }
    }

    /**
     * Disable directory monitoring (if it was enabled in the first place).
     *
     * @param topic Artifact from {@code EventBus} annotation. Not used.
     * @param reason Optional {@literal "Reason"} for starting.
     *               Helpful for logging.
     */
    @EventTopicSubscriber(topic=Constants.EVENT_FILECHOOSER_STOP)
    public void handleStopWatchService(final String topic,
                                       final Object reason)
    {
        if (watchService != null) {
            logger.trace("stopping service (reason: '{}')", reason);
            watchService.stop();
            logger.trace("should be good to go!");
            watchService = null;
            watchListener = null;
        }
    }

    /**
     * Creates a directory monitoring
     * {@link edu.wisc.ssec.mcidasv.util.pathwatcher.Service Service} for
     * the given {@code path} and files matching {@code glob}.
     *
     * @param path Path to monitor. Cannot be {@code null}.
     * @param glob Unix-style {@literal "glob"} filter. If {@code null} or
     *             empty, the service will use {@literal "*"}.
     *
     * @return Directory monitor that responds to changes in files matching
     * {@code glob} in {@code path}.
     *
     * @throws NullPointerException if {@code path} is {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    protected DirectoryWatchService createWatcher(final String path,
                                                  final String glob)
        throws IOException
    {
        DirectoryWatchService service = new SimpleDirectoryWatchService();
        watchListener = new DirectoryWatchService.OnFileChangeListener() {
            @Override public void onFileCreate(String filePath) {
                logger.trace("file created: '{}'", filePath);
                if (fileChooser != null && service.isRunning()) {
                    SwingUtilities.invokeLater(() -> doUpdate());
                }
            }

            @Override public void onFileModify(String filePath) {
                logger.trace("file modified: '{}'", filePath);
                if (fileChooser != null && service.isRunning()) {
                    SwingUtilities.invokeLater(() -> doUpdate());
                }
            }

            @Override public void onFileDelete(String filePath) {
                logger.trace("file deleted: '{}'", filePath);
                if (fileChooser != null && service.isRunning()) {
                    SwingUtilities.invokeLater(() -> doUpdate());
                }
            }
        };
        service.register(watchListener, path, glob);
        return service;
    }

    private JLabel statusLabel = new JLabel("Status");

    @Override
    public void setStatus(String statusString, String foo) {
        if (statusString == null)
            statusString = "";
        statusLabel.setText(statusString);
    }
        
    /**
     * Create a more McIDAS-V-like GUI layout
     */
    protected JComponent doMakeContents() {
        // Run super.doMakeContents()
        // It does some initialization on private components that we can't get at
        JComponent parentContents = super.doMakeContents();
        Element chooserNode = getXmlNode();

        String pathFromXml =
            XmlUtil.getAttribute(chooserNode, ATTR_PATH, (String)null);
        if (pathFromXml != null && Paths.get(pathFromXml).toFile().exists()) {
            setPath(pathFromXml);
        }

        JComponent typeComponent = new JPanel();
        if (XmlUtil.getAttribute(chooserNode, ATTR_DSCOMP, true)) {
            typeComponent = getDataSourcesComponent();
        }
        if (defaultDataSourceName != null) {
            typeComponent = new JLabel(defaultDataSourceName);
            McVGuiUtils.setLabelBold((JLabel)typeComponent, true);
            McVGuiUtils.setComponentHeight(typeComponent, new JComboBox());
        }
                        
        // Create the different panels... extending classes can override these
        topPanel = getTopPanel();
        centerPanel = getCenterPanel();
        bottomPanel = getBottomPanel();
        
        JPanel innerPanel = centerPanel;
        if (topPanel!=null && bottomPanel!=null)
            innerPanel = McVGuiUtils.topCenterBottom(topPanel, centerPanel, bottomPanel);
        else if (topPanel!=null) 
            innerPanel = McVGuiUtils.topBottom(topPanel, centerPanel, McVGuiUtils.Prefer.BOTTOM);
        else if (bottomPanel!=null)
            innerPanel = McVGuiUtils.topBottom(centerPanel, bottomPanel, McVGuiUtils.Prefer.TOP);
        
        // Start building the whole thing here
        JPanel outerPanel = new JPanel();

        JLabel typeLabel = McVGuiUtils.makeLabelRight(getDataSourcesLabel());
                
        JLabel statusLabelLabel = McVGuiUtils.makeLabelRight("");
                
        McVGuiUtils.setLabelPosition(statusLabel, Position.RIGHT);
        McVGuiUtils.setComponentColor(statusLabel, TextColor.STATUS);
        
        JButton helpButton = McVGuiUtils.makeImageButton(ICON_HELP, "Show help");
        helpButton.setActionCommand(GuiUtils.CMD_HELP);
        helpButton.addActionListener(this);
        
        JButton refreshButton = McVGuiUtils.makeImageButton(ICON_REFRESH, "Refresh");
        refreshButton.setActionCommand(GuiUtils.CMD_UPDATE);
        refreshButton.addActionListener(this);
        
        McVGuiUtils.setButtonImage(loadButton, ICON_ACCEPT_SMALL);
        McVGuiUtils.setComponentWidth(loadButton, Width.DOUBLE);
        
        // This is how we know if the action was initiated by a button press
        loadButton.addActionListener(new ActionListener() {
                   public void actionPerformed(ActionEvent e) {
                       buttonPressed = true;
                       Misc.runInABit(1000, new Runnable() {
                           public void run() {
                               buttonPressed = false;
                           }
                       });
                   }
              }
        );

        GroupLayout layout = new GroupLayout(outerPanel);
        outerPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(helpButton)
                        .addGap(GAP_RELATED)
                        .addComponent(refreshButton)
                        .addPreferredGap(RELATED)
                        .addComponent(loadButton))
                        .addGroup(LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(LEADING)
                            .addComponent(innerPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(typeLabel)
                                .addGap(GAP_RELATED)
                                .addComponent(typeComponent))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(statusLabelLabel)
                                .addGap(GAP_RELATED)
                                .addComponent(statusLabel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(typeLabel)
                    .addComponent(typeComponent))
                .addPreferredGap(UNRELATED)
                .addComponent(innerPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(UNRELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(statusLabelLabel)
                    .addComponent(statusLabel))
                .addPreferredGap(UNRELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(loadButton)
                    .addComponent(refreshButton)
                    .addComponent(helpButton))
                .addContainerGap())
        );
    
        return outerPanel;

    }
    
}
