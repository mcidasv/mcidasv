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

package edu.wisc.ssec.mcidasv.chooser;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.w3c.dom.Element;

import ucar.unidata.data.DataSource;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PollingInfo;
import ucar.unidata.xml.XmlUtil;

import edu.wisc.ssec.mcidasv.ArgumentManager;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

/**
 * A class for choosing files that can be polled.
 *
 * @author IDV development team
 */
public class PollingFileChooser extends FileChooser {

    /** Any initial file system path to start with */
    public static final String ATTR_DIRECTORY = "directory";

    /** Polling interval */
    public static final String ATTR_INTERVAL = "interval";

    /** The title attribute */
    public static final String ATTR_TITLE = "title";

    /** polling info */
    private PollingInfo pollingInfo;
    
    /** file path widget accessible to everyone */
    private JTextField filePathWidget;
    
    /** file pattern widget accessible to everyone */
    private JTextField filePatternWidget;
    
    /** Keep track of what was selected and update status accordingly */
    boolean isDirectory = false;
    int directoryCount = 0;
    int fileCount = 0;
    
    /**
     * Create the PollingFileChooser, passing in the manager and the xml element
     * from choosers.xml
     *
     * @param mgr The manager
     * @param root The xml root
     *
     */
    public PollingFileChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        
        Element chooserNode = getXmlNode();
        
        pollingInfo = (PollingInfo) idv.getPreference(PREF_POLLINGINFO + "." + getId());
        if (pollingInfo == null) {
            pollingInfo = new PollingInfo();
            pollingInfo.setMode(PollingInfo.MODE_COUNT);
            pollingInfo.setName("Directory");
            pollingInfo.setFilePattern(getAttribute(ATTR_FILEPATTERN, ""));
            pollingInfo.setFilePaths(Misc.newList(getAttribute(ATTR_DIRECTORY, "")));
            pollingInfo.setIsActive(XmlUtil.getAttribute(chooserNode, ATTR_POLLON, true));

            pollingInfo.setInterval((long) (XmlUtil.getAttribute(chooserNode, ATTR_INTERVAL, 5.0) * 60 * 1000));
            int fileCount = 1;
            String s = XmlUtil.getAttribute(chooserNode, ATTR_FILECOUNT, "1");
            s = s.trim();
            if (s.equals("all")) {
                fileCount = Integer.MAX_VALUE;
            } else {
                fileCount = new Integer(s).intValue();
            }
            pollingInfo.setFileCount(fileCount);
        }
        filePathWidget = pollingInfo.getFilePathWidget();
        filePatternWidget = pollingInfo.getPatternWidget();

    }
    
    /**
     * An extension of JFileChooser
     *
     * @author IDV Development Team
     * @version $Revision$
     */
    public class MyDirectoryChooser extends MyFileChooser {

        /**
         * Create the file chooser
         *
         * @param path   the initial path
         */
        public MyDirectoryChooser(String path) {
            super(path);
            setMultiSelectionEnabled(getAllowMultiple());
            setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        }
        
        /**
         * Set the selected directory
         *
         * @param selectedDirectory  the selected directory
         */
        public void setCurrentDirectory(File selectedDirectory) {
            super.setCurrentDirectory(selectedDirectory);
            setSelectedFiles(null);
        }
        
        /**
         * Set the selected files
         *
         * @param selectedFiles  the selected files
         */
        public void setSelectedFiles(File[] selectedFiles) {
            fileCount=0;
            directoryCount=0;
            if (selectedFiles == null || selectedFiles.length == 0) {
                isDirectory = true;
                if (filePathWidget!=null) {
                    filePathWidget.setText(this.getCurrentDirectory().getAbsolutePath());
                }
            }
            else {
                isDirectory = false;
                for (File selectedFile : selectedFiles) {
                    if (selectedFile.isFile()) fileCount++;
                    if (selectedFile.isDirectory()) {
                        directoryCount++;
                        if (directoryCount==1 && filePathWidget!=null) {
                            filePathWidget.setText(selectedFile.getAbsolutePath());
                        }
                    }
                }
            }
            super.setSelectedFiles(selectedFiles);
            
            // Disable load button if we arrived here by typing a directory or file name
            if (directoryCount > 0 ||
                    directoryCount == 0 && fileCount == 0 && !isDirectory) {
                setHaveData(false);
            }
            else {
                setHaveData(true);
            }

            updateStatus();
        }
    
    }

    /**
     * Make the file chooser
     *
     * @param path   the initial path
     *
     * @return  the file chooser
     */
    protected JFileChooser doMakeDirectoryChooser(String path) {
        return new MyDirectoryChooser(path);
    }

    /**
     * Override the base class method to catch the do load
     * This directly handles loading directories and passes off files to selectFiles() and selectFilesInner()
     */
    public void doLoadInThread() {
        Element chooserNode = getXmlNode();

        idv.getStateManager().writePreference(PREF_POLLINGINFO + "." + getId(), pollingInfo);
        idv.getStateManager().writePreference(PREF_DEFAULTDIR + getId(), pollingInfo.getFile());
        
//        userMessage("doLoadInThread: fileCount: " + fileCount + ", directoryCount: " + directoryCount + ", isDirectory: " + isDirectory + ", getHaveData: " + getHaveData() + ", buttonPressed: " + buttonPressed);

        // If a user types in a directory (on Windows) do not try to load that directory.
        // If the load button was pressed, go for it!
        if (fileCount == 0 && !buttonPressed) return;
        
        // If this is file(s) only, use FileChooser.doLoadInThread()
        if (fileCount > 0) {
            super.doLoadInThread();
            return;
        }
        
        Hashtable properties = new Hashtable();
        if ( !pollingInfo.applyProperties()) {
            return;
        }
        
        String title = basename(pollingInfo.getFile());
        title += "/" + ((JTextField)pollingInfo.getPatternWidget()).getText();
        pollingInfo.setName(title);
        properties.put(DataSource.PROP_TITLE, title);
        properties.put(DataSource.PROP_POLLINFO, pollingInfo.cloneMe());

        String dataSourceId;
        if (XmlUtil.hasAttribute(chooserNode, ATTR_DATASOURCEID)) {
            dataSourceId = XmlUtil.getAttribute(chooserNode, ATTR_DATASOURCEID);
        } else {
            dataSourceId = getDataSourceId();
        }
        
        makeDataSource(pollingInfo.getFiles(), dataSourceId, properties);
    }

    /**
     * Handle the selection of the set of files
     * Copy from IDV FileChooser, add ability to name and poll
     */
    protected boolean selectFilesInner(File[] files, File directory)
            throws Exception {
        Element chooserNode = getXmlNode();
        
        if ((files == null) || (files.length == 0)) {
            userMessage("Please select a file");
            return false;
        }
        FileManager.addToHistory(files[0]);
        List    selectedFiles      = new ArrayList();
        String  fileNotExistsError = "";
        boolean didXidv            = false;

        for (int i = 0; i < files.length; i++) {
            if ( !files[i].exists()) {
                fileNotExistsError += "File does not exist: " + files[i] + "\n";
            } else {
                String filename = files[i].toString();
                //Check for the bundle or jnlp file
                if (((ArgumentManager)idv.getArgsManager()).isBundle(filename)
                        || idv.getArgsManager().isJnlpFile(filename)) {
                    didXidv = idv.handleAction(filename, null);
                } else {
                    selectedFiles.add(filename);
                }
            }
        }

        if (didXidv) {
            closeChooser();
            return true;
        }

        if (selectedFiles.size() == 0) {
            return false;
        }

        if (fileNotExistsError.length() > 0) {
            userMessage(fileNotExistsError);
            return false;
        }

        Object definingObject = selectedFiles;
        String title = selectedFiles.size() + " files";
        if (selectedFiles.size() == 1) {
            definingObject = selectedFiles.get(0);
            title = basename(definingObject.toString());
        }
        
        String dataSourceId;
        if (XmlUtil.hasAttribute(chooserNode, ATTR_DATASOURCEID)) {
            dataSourceId = XmlUtil.getAttribute(chooserNode, ATTR_DATASOURCEID);
        } else {
            dataSourceId = getDataSourceId();
        }
        
        Hashtable   properties  = new Hashtable();
        
        // TODO: I disabled file polling on purpose:
        // The control for this is in the Directory options and is grayed out
        //  when selecting single files.  Is this something people want?
        PollingInfo newPollingInfo = new PollingInfo(false);
        String pattern = getFilePattern();
        if ((pattern != null) && (pattern.length() > 0)) {
            newPollingInfo.setFilePattern(pattern);
        }
        newPollingInfo.setName(title);
        properties.put(DataSource.PROP_TITLE, title);
        properties.put(DataSource.PROP_POLLINFO, newPollingInfo);

        // explicitly denote whether or not this was a "bulk load". these data
        // sources require a little extra attention when being unpersisted.
        properties.put("bulk.load", (selectedFiles.size() > 1));
        return makeDataSource(definingObject, dataSourceId, properties);
    }
    
    /**
     * Emulate basename()
     */
    private String basename(String path) {
        if (path.lastIndexOf('/') > 0)
            path = path.substring(path.lastIndexOf('/'));
        else if (path.lastIndexOf('\\') > 0)
            path = path.substring(path.lastIndexOf('\\'));
        if (path.length() > 1)
            path = path.substring(1);
        return path;
    }
    
    /**
     * Get the tooltip for the load button
     *
     * @return The tooltip for the load button
     */
    protected String getLoadToolTip() {
        return "Load the file(s) or directory";
    }

    /**
     * Override the base class method to catch the do update.
     */
    @Override public void doUpdate() {
        fileChooser.rescanCurrentDirectory();
    }

    /**
     * Process PollingInfo GUI components based on their label and properties
     * Turn it into a nicely-formatted labeled panel
     */
    private JPanel processPollingOption(JLabel label, JPanel panel) {
        String string = label.getText().trim();
        
        // File Pattern
        if (string.equals("File Pattern:")) {
            Component panel1 = panel.getComponent(0);
            if (panel1 instanceof JPanel) {
                Component[] comps = ((JPanel)panel1).getComponents();
                if (comps.length == 2) {
                    List newComps1 = new ArrayList();
                    List newComps2 = new ArrayList();
                    if (comps[0] instanceof JPanel) {
                        Component[] comps2 = ((JPanel)comps[0]).getComponents();
                        if (comps2.length==1 &&
                                comps2[0] instanceof JPanel)
                            comps2=((JPanel)comps2[0]).getComponents();
                        if (comps2.length == 2) {
                            if (comps2[0] instanceof JTextField) {
                                McVGuiUtils.setComponentWidth((JTextField) comps2[0], McVGuiUtils.Width.SINGLE);
                            }
                            newComps1.add(comps2[0]);
                            newComps2.add(comps2[1]);
                        }
                    }
                    newComps1.add(comps[1]);
                    panel = GuiUtils.vbox(
                            GuiUtils.left(GuiUtils.hbox(newComps1)),
                            GuiUtils.left(GuiUtils.hbox(newComps2))
                    );
                }
            }
        }
        
        // Files
        if (string.equals("Files:")) {
            Component panel1 = panel.getComponent(0);
            if (panel1 instanceof JPanel) {
                Component[] comps = ((JPanel)panel1).getComponents();
                if (comps.length == 6) {
                    List newComps1 = new ArrayList();
                    List newComps2 = new ArrayList();
                    if (comps[3] instanceof JRadioButton) {
                        String text = ((JRadioButton) comps[3]).getText().trim();
                        if (text.equals("All files in last:")) text="All files in last";
                        ((JRadioButton) comps[3]).setText(text);
                    }
                    if (comps[4] instanceof JTextField) {
                        McVGuiUtils.setComponentWidth((JTextField) comps[4], McVGuiUtils.Width.HALF);
                    }
                    if (comps[5] instanceof JLabel) {
                        String text = ((JLabel) comps[5]).getText().trim();
                        ((JLabel) comps[5]).setText(text);
                    }
                    newComps1.add(comps[0]);
                    newComps1.add(comps[1]);
                    newComps2.add(comps[3]);
                    newComps2.add(comps[4]);
                    newComps2.add(comps[5]);
                    panel = GuiUtils.vbox(
                            GuiUtils.left(GuiUtils.hbox(newComps1)),
                            GuiUtils.left(GuiUtils.hbox(newComps2))
                    );
                }
            }
        }
        
        // Polling
        if (string.equals("Polling:")) {
            Component panel1 = panel.getComponent(0);
            if (panel1 instanceof JPanel) {
                Component[] comps = ((JPanel)panel1).getComponents();
                if (comps.length == 4) {
                    List newComps = new ArrayList();
                    if (comps[0] instanceof JCheckBox) {
                        ((JCheckBox) comps[0]).setText("");
                    }
                    if (comps[1] instanceof JLabel) {
                        String text = ((JLabel) comps[1]).getText().trim();
                        if (text.equals("Check every:")) text="Refresh every";
                        ((JLabel) comps[1]).setText(text);
                    }
                    if (comps[2] instanceof JTextField) {
                        McVGuiUtils.setComponentWidth((JTextField) comps[2], McVGuiUtils.Width.HALF);
                    }
                    if (comps[3] instanceof JLabel) {
                        String text = ((JLabel) comps[3]).getText().trim();
                        ((JLabel) comps[3]).setText(text);
                    }
                    newComps.add(comps[0]);
                    newComps.add(comps[1]);
                    newComps.add(comps[2]);
                    newComps.add(comps[3]);
                    string="";
                    panel = GuiUtils.left(GuiUtils.hbox(newComps));
                }
            }
        }
        
        return McVGuiUtils.makeLabeledComponent(string, panel);
    }
    
    /**
     * Turn PollingInfo options into a nicely-formatted panel
     */
    private JPanel processPollingOptions(List comps) {
        List newComps = new ArrayList();
        newComps = new ArrayList();
        if (comps.size() == 4) {
//          newComps.add(comps.get(0));
            
            // Put Recent and Pattern panels next to each other and make them bordered
            Component[] labelPanel1 = ((JPanel)comps.get(2)).getComponents();
            Component[] labelPanel2 = ((JPanel)comps.get(1)).getComponents();
            if (labelPanel1[1] instanceof JPanel && labelPanel2[1] instanceof JPanel) {
                JPanel recentPanel = (JPanel)labelPanel1[1];
                JPanel patternPanel = (JPanel)labelPanel2[1];
                recentPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Recent Files"));
                patternPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("File Pattern"));
                
                // Make the container panel
                JPanel filePanel = new JPanel();
                
                GroupLayout layout = new GroupLayout(filePanel);
                filePanel.setLayout(layout);
                layout.setHorizontalGroup(
                    layout.createParallelGroup(LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(recentPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(GAP_RELATED)
                        .addComponent(patternPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                        )
                );
                layout.setVerticalGroup(
                    layout.createParallelGroup(LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(TRAILING)
                            .addComponent(recentPanel, LEADING, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(patternPanel, LEADING, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                            )
                );

                newComps.add(McVGuiUtils.makeLabeledComponent("Directory:", filePanel));
            }
            else {
                newComps.add(comps.get(1));
                newComps.add(comps.get(2));
            }
            newComps.add(comps.get(3));
        }
        else {
            newComps = comps;
        }
        return GuiUtils.top(GuiUtils.vbox(newComps));
    }
    
    /**
     * Set the status message appropriately
     */
    protected void updateStatus() {
        super.updateStatus();
        String selectedReference = "the selected data";
        
        if(!getHaveData()) {
            setStatus("Select zero, one, or multiple files");
            GuiUtils.enableTree(bottomPanel, false);
            return;
        }
        
        if (isDirectory) {
            selectedReference = "all files in this directory";
        }
        else {
            if (fileCount > 0) {
                if (fileCount > 1) selectedReference = "the selected files";
                else selectedReference = "the selected file";
            }
            if (directoryCount > 0) {
                selectedReference = "the selected directory";
            }
        }
        GuiUtils.enableTree(bottomPanel, isDirectory || directoryCount > 0);
        setStatus("Press \"" + CMD_LOAD + "\" to load " + selectedReference, "buttons");
    }
        
    /**
     * Get the top panel for the chooser
     * @return the top panel
     */
//    protected JPanel getTopPanel() {
//      return McVGuiUtils.makeLabeledComponent("Source Name:", pollingInfo.getNameWidget());
//    }
    
    /**
     * Get the center panel for the chooser
     * @return the center panel
     */
    protected JPanel getCenterPanel() {
        fileChooser = doMakeDirectoryChooser(path);
        fileChooser.setPreferredSize(new Dimension(300, 300));
        fileChooser.setMultiSelectionEnabled(getAllowMultiple());

        JPanel centerPanel;
        JComponent accessory = getAccessory();
        if (accessory == null) {
            centerPanel = GuiUtils.center(fileChooser);
        } else {
            centerPanel = GuiUtils.centerRight(fileChooser, GuiUtils.top(accessory));
        }
        centerPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        return McVGuiUtils.makeLabeledComponent("Files:", centerPanel);
    }
    
    /**
     * Get the bottom panel for the chooser
     * @return the bottom panel
     */
    protected JPanel getBottomPanel() {

        // Pull apart the PollingInfo components and rearrange them
        // Don't want to override PollingInfo because it isn't something the user sees
        // Arranged like: Label, Panel; Label, Panel; Label, Panel; etc...
        List comps = new ArrayList();
        List newComps = new ArrayList();
        pollingInfo.getPropertyComponents(comps, false, pollingInfo.getFileCount()>0);
        for (int i=0; i<comps.size()-1; i++) {
            JComponent compLabel = (JComponent)comps.get(i);
            if (compLabel instanceof JLabel) {
                i++;
                JComponent compPanel = (JComponent)comps.get(i);
                if (compPanel instanceof JPanel) {
                    newComps.add(processPollingOption((JLabel)compLabel, (JPanel)compPanel));
                }
            }
        }
        
        JPanel pollingPanel = processPollingOptions(newComps);
        return pollingPanel;
    }

}

