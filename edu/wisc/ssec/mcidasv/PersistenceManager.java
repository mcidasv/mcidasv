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

package edu.wisc.ssec.mcidasv;

import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.python.core.PyObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.IdvManager;
import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.SavedBundle;
import ucar.unidata.idv.ServerUrlRemapper;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.IdvXmlUi;
import ucar.unidata.idv.ui.LoadBundleDialog;
import ucar.unidata.idv.ui.WindowInfo;
import ucar.unidata.ui.ComponentGroup;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PollingInfo;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlResourceCollection;

import edu.wisc.ssec.mcidasv.control.ImagePlanViewControl;
import edu.wisc.ssec.mcidasv.probes.ReadoutProbe;
import edu.wisc.ssec.mcidasv.ui.McvComponentGroup;
import edu.wisc.ssec.mcidasv.ui.McvComponentHolder;
import edu.wisc.ssec.mcidasv.ui.UIManager;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.XPathUtils;
import edu.wisc.ssec.mcidasv.util.XmlUtil;

/**
 * <p>McIDAS-V has 99 problems, and bundles are several of 'em. Since the UI of
 * alpha 10 relies upon component groups and McIDAS-V needs to support IDV and
 * bundles prior to alpha 10, we must add facilities for coping with bundles
 * that may not contain component groups. Here's a list of the issues and how
 * they are resolved:</p> 
 * 
 * <p><ul>
 * <li>Bundles prior to alpha 9 use the <code>TabbedUIManager</code>. Each tab
 * is, internally, an IDV window. This is reflected in the contents of bundles,
 * so the IDV wants to create a new window for each tab upon loading. Alpha 10
 * allows the user to force bundles to only create one window. This work is
 * done in {@link #injectComponentGroups(List)}.</li>
 * 
 * <li>The IDV allows users to save bundles that contain <i>both</i> 
 * {@link ucar.unidata.idv.ViewManager}s with component groups and without! 
 * This is actually only a problem when limiting the windows; 
 * <code>injectComponentGroups</code> has to wrap ViewManagers without 
 * component groups in dynamic skins. These ViewManagers must be removed 
 * from the bundle's internal list of ViewManagers, as they don't exist until
 * the dynamic skin is built. <i>Do not simply clear the list!</i> The 
 * ViewManagers within component groups must appear in it, otherwise the IDV
 * does not add them to the {@link ucar.unidata.idv.VMManager}. If limiting 
 * windows is off, everything will be caught properly by the unpersisting 
 * facilities in {@link edu.wisc.ssec.mcidasv.ui.UIManager}.</li>
 * 
 * @see IdvPersistenceManager
 * @see UIManager
 */
public class PersistenceManager extends IdvPersistenceManager {

    /** Key used to access a bundle's McIDAS-V in-depth versioning info section. */
    public static final String ID_MCV_VERSION = "mcvversion";

    private static final Logger logger = LoggerFactory.getLogger(PersistenceManager.class);

    /**
     * Macro used as a place holder for wherever the IDV decides to place 
     * extracted contents of a bundle. 
     */
    public static final String MACRO_ZIDVPATH = '%'+PROP_ZIDVPATH+'%';

    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(IdvManager.class.getName());

    /** Is the bundle being saved a layout bundle? */
    private boolean savingDefaultLayout = false;

    /** Stores the last active ViewManager from <i>before</i> a bundle load. */
    private ViewManager lastBeforeBundle = null;

    /** 
     * Whether or not the user wants to attempt merging bundled layers into
     * current displays.
     */
    private boolean mergeBundledLayers = false;

    /** Whether or not a bundle is actively loading. */
    private boolean bundleLoading = false;

    /** Cache the parameter sets XML */
    private XmlResourceCollection parameterSets;
    private static Document parameterSetsDocument;
    private static Element parameterSetsRoot;
    private static final String TAG_FOLDER = "folder";
    private static final String TAG_DEFAULT = "default";
    private static final String ATTR_NAME = "name";

    /** Use radio buttons to control state saving */
    private JRadioButton layoutOnlyRadio;
    private JRadioButton layoutSourcesRadio;
    private JRadioButton layoutSourcesDataRadio;
    
    /**
     * Java requires this constructor. 
     */
    public PersistenceManager() {
        this(null);
    }

    /**
     * @see ucar.unidata.idv.IdvPersistenceManager#IdvPersistenceManager(IntegratedDataViewer)
     */
    public PersistenceManager(IntegratedDataViewer idv) {
        super(idv);
           
    	//TODO: Saved for future development
/**
        layoutOnlyRadio = new JRadioButton("Layout only");
        layoutOnlyRadio.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                saveJythonBox.setSelectedIndex(0);
                saveJython = false;
            	makeDataRelativeCbx.setSelected(false);
            	makeDataRelative = false;
            	saveDataSourcesCbx.setSelected(false);
            	saveDataSources = false;
            	saveDataCbx.setSelected(false);
            	saveData = false;
            }
        });

        layoutSourcesRadio = new JRadioButton("Layout & Data Sources");
        layoutSourcesRadio.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                saveJythonBox.setSelectedIndex(1);
                saveJython = true;
            	makeDataRelativeCbx.setSelected(false);
            	makeDataRelative = false;
            	saveDataSourcesCbx.setSelected(true);
            	saveDataSources = true;
            	saveDataCbx.setSelected(false);
            	saveData = false;
            }
        });
        
        layoutSourcesDataRadio = new JRadioButton("Layout, Data Sources & Data");
        layoutSourcesRadio.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                saveJythonBox.setSelectedIndex(1);
                saveJython = true;
            	makeDataRelativeCbx.setSelected(false);
            	makeDataRelative = false;
            	saveDataSourcesCbx.setSelected(true);
            	saveDataSources = true;
            	saveDataCbx.setSelected(true);
            	saveData = true;
            }
        });
        //Group the radio buttons.
        layoutSourcesRadio.setSelected(true);
        ButtonGroup group = new ButtonGroup();
        group.add(layoutOnlyRadio);
        group.add(layoutSourcesRadio);
        group.add(layoutSourcesDataRadio);
*/
        
    }

    /**
     * Returns the last active {@link ViewManager} from <i>before</i> loading
     * the most recent bundle.
     * 
     * @return Either the ViewManager or {@code null} if there was no previous
     * ViewManager (such as loading a default bundle/layout).
     */
    public ViewManager getLastViewManager() {
        return lastBeforeBundle;
    }

    /**
     * Returns whether or not a bundle is currently being loaded.
     * 
     * @return Either {@code true} if {@code instantiateFromBundle} is doing 
     * what it needs to do, or {@code false}.
     * 
     * @see #instantiateFromBundle(Hashtable, boolean, LoadBundleDialog, boolean, Hashtable, boolean, boolean, boolean)
     */
    public boolean isBundleLoading() {
        return bundleLoading;
    }

    public boolean getMergeBundledLayers() {
        logger.trace("mergeBundledLayers={}", mergeBundledLayers);
        return mergeBundledLayers;
    }

    private void setMergeBundledLayers(final boolean newValue) {
        logger.trace("old={} new={}", mergeBundledLayers, newValue);
        mergeBundledLayers = newValue;
    }

    @Override public boolean getSaveDataSources() {
        boolean result = false;
        if (!savingDefaultLayout) {
            result = super.getSaveDataSources();
        }
        logger.trace("getSaveDataSources={} savingDefaultLayout={}", result, savingDefaultLayout);
        return result;
    }

    @Override public boolean getSaveDisplays() {
        boolean result = false;
        if (!savingDefaultLayout) {
            result = super.getSaveDisplays();
        }
        logger.trace("getSaveDisplays={} savingDefaultLayout={}", result, savingDefaultLayout);
        return result;
    }

    @Override public boolean getSaveViewState() {
        boolean result = true;
        if (!savingDefaultLayout) {
            result = super.getSaveViewState();
        }
        logger.trace("getSaveViewState={} savingDefaultLayout={}", result, savingDefaultLayout);
        return result;
    }

    @Override public boolean getSaveJython() {
        boolean result = false;
        if (!savingDefaultLayout) {
            result = super.getSaveJython();
        }
        logger.trace("getSaveJython={} savingDefaultLayout={}", result, savingDefaultLayout);
        return result;
    }

    public void doSaveAsDefaultLayout() {
        String layoutFile = getResourceManager().getResources(IdvResourceManager.RSC_BUNDLES).getWritable();
        // do prop check here?
        File f = new File(layoutFile);
        if (f.exists()) {
            boolean result = GuiUtils.showYesNoDialog(null, "Saving a new default layout will overwrite your existing default layout. Do you wish to continue?", "Overwrite Confirmation");
            if (!result) {
                return;
            }
        }

        savingDefaultLayout = true;
        try {
            String xml = getBundleXml(true, true);
            if (xml != null) {
                IOUtil.writeFile(layoutFile, xml);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            savingDefaultLayout = false;
        }
    }

    @Override public JPanel getFileAccessory() {
        // Always save displays and data sources
        saveDisplaysCbx.setSelected(true);
        saveDisplays = true;
        saveViewStateCbx.setSelected(true);
        saveViewState = true;
        saveDataSourcesCbx.setSelected(true);
        saveDataSources = true;

        return GuiUtils.top(
            GuiUtils.vbox(
                Misc.newList(
                    GuiUtils.inset(new JLabel("Bundle save options:"),
                                   new Insets(0, 5, 5, 0)),
                                   saveJythonBox,
                                   makeDataRelativeCbx)));
    }
    
    /**
     * Have the user select an xidv filename and
     * write the current application state to it.
     * This also sets the current file name and
     * adds the file to the history list.
     */
    public void doSaveAs() {
        String filename =
            FileManager.getWriteFile(getArgsManager().getBundleFileFilters(),
                                     "mcvz", getFileAccessory());
        if (filename == null) {
            return;
        }
        setCurrentFileName(filename);

        boolean prevMakeDataEditable = makeDataEditable;
        makeDataEditable = makeDataEditableCbx.isSelected();

        boolean prevMakeDataRelative = makeDataRelative;
        makeDataRelative = makeDataRelativeCbx.isSelected();
        if (doSave(filename)) {
            getPublishManager().publishContent(filename, null, publishCbx);
            getIdv().addToHistoryList(filename);
        }
        makeDataEditable = prevMakeDataEditable;
        makeDataRelative = prevMakeDataRelative;

    }

    /**
     * Overridden so that McIDAS-V can: 
     * <ul>
     * <li>add better versioning information to bundles</li>
     * <li>remove {@link edu.wisc.ssec.mcidasv.probes.ReadoutProbe ReadoutProbes} from the {@code displayControls} that are getting persisted.</li>
     * <li>disallow saving multi-banded ADDE data sources until we have fix!</li>
     * </ul>
     */
    @Override protected boolean addToBundle(Hashtable data, List dataSources,
            List displayControls, List viewManagers,
            String jython) 
    {
        logger.trace("hacking bundle output!");
        // add in some extra versioning information
        StateManager stateManager = (StateManager)getIdv().getStateManager();
        if (data != null) {
            data.put(ID_MCV_VERSION, stateManager.getVersionInfo());
        }
        logger.trace("hacking displayControls={}", displayControls);
        logger.trace("hacking dataSources={}", dataSources);
        // remove ReadoutProbes from the list and possibly save off multibanded
        // ADDE data sources
        if (displayControls != null) {
//            Set<DataSourceImpl> observed = new HashSet<DataSourceImpl>();
            Map<DataSourceImpl, List<DataChoice>> observed = new LinkedHashMap<DataSourceImpl, List<DataChoice>>();
            List<DisplayControl> newControls = new ArrayList<DisplayControl>();
            for (DisplayControl dc : (List<DisplayControl>)displayControls) {
                if (dc instanceof ReadoutProbe) {
                    logger.trace("skipping readoutprobe!");
                    continue;
                } else if (dc instanceof ImagePlanViewControl) {
                    ImagePlanViewControl imageControl = (ImagePlanViewControl)dc;
                    List<DataSourceImpl> tmp = (List<DataSourceImpl>)imageControl.getDataSources();
                    for (DataSourceImpl src : tmp) {
                        if (observed.containsKey(src)) {
                            observed.get(src).addAll(src.getDataChoices());
                            logger.trace("already seen src={} new selection={}", src);
                        } else {
                            logger.trace("haven't seen src={}", src);
                            List<DataChoice> selected = new ArrayList<DataChoice>(imageControl.getDataChoices());
                            observed.put(src, selected);
                        }
                    }
                    logger.trace("found an image control: {} datasrcs={} datachoices={}", new Object[] { imageControl, imageControl.getDataSources(), imageControl.getDataChoices() });
                    newControls.add(dc);
                } else {
                    logger.trace("found some kinda thing: {}", dc.getClass().getName());
                    newControls.add(dc);
                }
            }
            for (Map.Entry<DataSourceImpl, List<DataChoice>> entry : observed.entrySet()) {
                logger.trace("multibanded src={} choices={}", entry.getKey(), entry.getValue());
            }
            displayControls = newControls;
        }

        return super.addToBundle(data, dataSources, displayControls, viewManagers, jython);
    }

    @Override public List getLocalBundles() {
        List<SavedBundle> allBundles = new ArrayList<SavedBundle>();
        List<String> dirs = new ArrayList<String>();
        String sitePath = getResourceManager().getSitePath();

        Collections.addAll(dirs, getStore().getLocalBundlesDir());

        if (sitePath != null) {
            dirs.add(IOUtil.joinDir(sitePath, IdvObjectStore.DIR_BUNDLES));
        }

        for (String top : dirs) {
            List<File> subdirs = 
                IOUtil.getDirectories(Collections.singletonList(top), true);
            for (File subdir : subdirs) {
                loadBundlesInDirectory(allBundles, 
                    fileToCategories(top, subdir.getPath()), subdir);
            }
        }
        return allBundles;
    }

    protected void loadBundlesInDirectory(List<SavedBundle> allBundles,
            List categories, File file) {
        String[] localBundles = file.list();

        for (int i = 0; i < localBundles.length; i++) {
            String filename = IOUtil.joinDir(file.toString(), localBundles[i]);
            if (ArgumentManager.isBundle(filename)) {
                allBundles.add(new SavedBundle(filename,
                    IOUtil.stripExtension(localBundles[i]), categories, true));
            }
        }
    }

    /**
     * <p>
     * Overridden so that McIDAS-V can redirect to the version of this method
     * that supports limiting the number of new windows.
     * </p>
     * 
     * @see #decodeXml(String, boolean, String, String, boolean, boolean,
     *      Hashtable, boolean, boolean, boolean)
     */
    @Override public void decodeXml(String xml, final boolean fromCollab,
        String xmlFile, final String label, final boolean showDialog,
        final boolean shouldMerge, final Hashtable bundleProperties,
        final boolean removeAll, final boolean letUserChangeData) 
    {
        decodeXml(xml, fromCollab, xmlFile, label, showDialog, shouldMerge,
            bundleProperties, removeAll, letUserChangeData, false);
    }

    /**
     * <p>
     * Hijacks control of the IDV's bundle loading facilities. Due to the way
     * versions of McIDAS-V prior to alpha 10 handled tabs, the user will end
     * up with a new window for each tab in the bundle. McIDAS-V alpha 10 has
     * the ability to only create one new window and have everything else go
     * into that window's tabs.
     * </p>
     * 
     * @see IdvPersistenceManager#decodeXmlFile(String, String, boolean, boolean, Hashtable)
     * @see #decodeXml(String, boolean, String, String, boolean, boolean, Hashtable,
     *      boolean, boolean, boolean)
     */
    @Override public boolean decodeXmlFile(String xmlFile, String label,
                                 boolean checkToRemove,
                                 boolean letUserChangeData,
                                 Hashtable bundleProperties) {

        logger.trace("loading bundle: '{}'", xmlFile);
        if (xmlFile.isEmpty()) {
            logger.warn("attempted to open a filename that is zero characters long");
            return false;
        }
        
        String name = ((label != null) ? label : IOUtil.getFileTail(xmlFile));

        boolean shouldMerge = getStore().get(PREF_OPEN_MERGE, true);

        boolean removeAll   = false;

        boolean limitNewWindows = false;

        boolean mergeLayers = false;
        setMergeBundledLayers(false);

        if (checkToRemove) {
            // ok[0] = did the user press cancel 
            boolean[] ok = getPreferenceManager().getDoRemoveBeforeOpening(name);

            if (!ok[0]) {
                return false;
            }

            if (!ok[1] && !ok[2]) { // create new [opt=0]
                removeAll = false;
                shouldMerge = false;
                mergeLayers = false;
            }
            if (!ok[1] && ok[2]) { // add new tabs [opt=2]
                removeAll = false;
                shouldMerge = true;
                mergeLayers = false;
            }
            if (ok[1] && !ok[2]) { // merge with active [opt=1]
                removeAll = false;
                shouldMerge = false;
                mergeLayers = true;
            }
            if (ok[1] && ok[2]) { // replace session [opt=3]
                removeAll = true;
                shouldMerge = true;
                mergeLayers = false;
            }

            logger.trace("removeAll={} shouldMerge={} mergeLayers={}", new Object[] { removeAll, shouldMerge, mergeLayers });

            setMergeBundledLayers(mergeLayers);

            if (removeAll) {
                // Remove the displays first because, if we remove the data 
                // some state can get cleared that might be accessed from a 
                // timeChanged on the unremoved displays
                getIdv().removeAllDisplays();
                // Then remove the data
                getIdv().removeAllDataSources();
            }

            if (ok.length == 4) {
                limitNewWindows = ok[3];
            }
        }

        // the UI manager may need to know which ViewManager was active *before*
        // we loaded the bundle.
        lastBeforeBundle = getVMManager().getLastActiveViewManager();

        ArgumentManager argsManager = (ArgumentManager)getArgsManager();

        boolean isZidv = ArgumentManager.isZippedBundle(xmlFile);

        if (!isZidv && !ArgumentManager.isXmlBundle(xmlFile)) {
            //If we cannot tell what it is then try to open it as a zidv file
            try {
                ZipInputStream zin = 
                    new ZipInputStream(IOUtil.getInputStream(xmlFile));
                isZidv = (zin.getNextEntry() != null);
            } catch (Exception e) {}
        }

        String bundleContents = null;
        try {
            //Is this a zip file
            logger.trace("bundle file={} isZipped={}", xmlFile, ArgumentManager.isZippedBundle(xmlFile));
            if (ArgumentManager.isZippedBundle(xmlFile)) {
                boolean ask   = getStore().get(PREF_ZIDV_ASK, true);
                boolean toTmp = getStore().get(PREF_ZIDV_SAVETOTMP, true);
                String  dir   = getStore().get(PREF_ZIDV_DIRECTORY, "");
                if (ask || ((dir.length() == 0) && !toTmp)) {

                    JCheckBox askCbx = 
                        new JCheckBox("Don't show this again", !ask);

                    JRadioButton tmpBtn =
                        new JRadioButton("Write to temporary directory", toTmp);

                    JRadioButton dirBtn = 
                        new JRadioButton("Write to:", !toTmp);

                    GuiUtils.buttonGroup(tmpBtn, dirBtn);
                    JTextField dirFld = new JTextField(dir, 30);
                    JComponent dirComp = GuiUtils.centerRight(
                                            dirFld,
                                            GuiUtils.makeFileBrowseButton(
                                                dirFld, true, null));

                    JComponent contents =
                        GuiUtils
                            .vbox(GuiUtils
                                .inset(new JLabel("Where should the data files be written to?"),
                                        5), tmpBtn,
                                        GuiUtils.hbox(dirBtn, dirComp),
                                            GuiUtils
                                                .inset(askCbx,
                                                    new Insets(5, 0, 0, 0)));

                    contents = GuiUtils.inset(contents, 5);
                    if (!GuiUtils.showOkCancelDialog(null, "Zip file data",
                            contents, null)) {
                        return false;
                    }

                    ask = !askCbx.isSelected();

                    toTmp = tmpBtn.isSelected();

                    dir = dirFld.getText().toString().trim();

                    getStore().put(PREF_ZIDV_ASK, ask);
                    getStore().put(PREF_ZIDV_SAVETOTMP, toTmp);
                    getStore().put(PREF_ZIDV_DIRECTORY, dir);
                    getStore().save();
                }

                String tmpDir = dir;
                if (toTmp) {
                    tmpDir = getIdv().getObjectStore().getUserTmpDirectory();
                    tmpDir = IOUtil.joinDir(tmpDir, Misc.getUniqueId());
                }
                IOUtil.makeDir(tmpDir);

                getStateManager().putProperty(PROP_ZIDVPATH, tmpDir);
                ZipInputStream zin =
                    new ZipInputStream(IOUtil.getInputStream(xmlFile));
                ZipEntry ze = null;

                while ((ze = zin.getNextEntry()) != null) {
                    String entryName = ze.getName();

                    if (ArgumentManager.isXmlBundle(entryName.toLowerCase())) {
                        bundleContents = new String(IOUtil.readBytes(zin,
                                null, false));
                    } else {
//                        String xmlPath = IOUtil.joinDir(tmpDir, entryName);
                        if (IOUtil.writeTo(zin, new FileOutputStream(IOUtil.joinDir(tmpDir, entryName))) < 0) {
                            return false;
                        }
                    }
                }
            } else {
                Trace.call1("Decode.readContents");
                bundleContents = IOUtil.readContents(xmlFile);
                Trace.call2("Decode.readContents");
            }

            // TODO: this can probably go one day. I altered the prefix of the
            // comp group classes. Old: "McIDASV...", new: "Mcv..."
            // just gotta be sure to fix the references in the bundles.
            // only people using the nightly build will be affected.
            if (bundleContents != null) {
                bundleContents = StringUtil.substitute(bundleContents, 
                    OLD_COMP_STUFF, NEW_COMP_STUFF);
                bundleContents = StringUtil.substitute(bundleContents, 
                    OLD_SOURCE_MACRO, NEW_SOURCE_MACRO);
            }
            
            
            Trace.call1("Decode.decodeXml");
            decodeXml(bundleContents, false, xmlFile, name, true,
                      shouldMerge, bundleProperties, removeAll,
                      letUserChangeData, limitNewWindows);
            Trace.call2("Decode.decodeXml");
            return true;
        } catch (Throwable exc) {
            if (contents == null) {
                logException("Unable to load bundle:" + xmlFile, exc);
            } else {
                logException("Unable to evaluate bundle:" + xmlFile, exc);
            }
            return false;
        }
    }

    // replace "old" references in a bundle's XML to the "new" classes.
    private static final String OLD_COMP_STUFF = "McIDASVComp";
    private static final String NEW_COMP_STUFF = "McvComp";

    private static final String OLD_SOURCE_MACRO = "%fulldatasourcename%";
    private static final String NEW_SOURCE_MACRO = "%datasourcename%";

    /**
     * <p>Overridden so that McIDAS-V can redirect to the version of this 
     * method that supports limiting the number of new windows.</p>
     * 
     * @see #decodeXmlInner(String, boolean, String, String, boolean, boolean, Hashtable, boolean, boolean, boolean)
     */
    @Override protected synchronized void decodeXmlInner(String xml,
                                                         boolean fromCollab, 
                                                         String xmlFile, 
                                                         String label, 
                                                         boolean showDialog, 
                                                         boolean shouldMerge, 
                                                         Hashtable bundleProperties,
                                                         boolean didRemoveAll, 
                                                         boolean changeData) {

        decodeXmlInner(xml, fromCollab, xmlFile, label, showDialog, 
                      shouldMerge, bundleProperties, didRemoveAll, changeData, 
                      false);

    }

    /**
     * <p>
     * Overridden so that McIDAS-V can redirect to the version of this method
     * that supports limiting the number of new windows.
     * </p>
     * 
     * @see #instantiateFromBundle(Hashtable, boolean, LoadBundleDialog,
     *      boolean, Hashtable, boolean, boolean, boolean)
     */
    @Override protected void instantiateFromBundle(Hashtable ht,
        boolean fromCollab, LoadBundleDialog loadDialog, boolean shouldMerge,
        Hashtable bundleProperties, boolean didRemoveAll,
        boolean letUserChangeData) throws Exception 
    {
        instantiateFromBundle(ht, fromCollab, loadDialog, shouldMerge,
            bundleProperties, didRemoveAll, letUserChangeData, false);
    }

    /**
     * <p>
     * Hijacks the second part of the IDV bundle loading pipeline so that
     * McIDAS-V can limit the number of new windows.
     * </p>
     * 
     * @see IdvPersistenceManager#decodeXml(String, boolean,
     *      String, String, boolean, boolean, Hashtable, boolean, boolean)
     * @see #decodeXmlInner(String, boolean, String, String, boolean, boolean,
     *      Hashtable, boolean, boolean, boolean)
     */
    public void decodeXml(final String xml, final boolean fromCollab,
        final String xmlFile, final String label, final boolean showDialog,
        final boolean shouldMerge, final Hashtable bundleProperties,
        final boolean removeAll, final boolean letUserChangeData,
        final boolean limitWindows) 
    {

        if (!getStateManager().getShouldLoadBundlesSynchronously()) {
            Runnable runnable = new Runnable() {

                public void run() {
                    decodeXmlInner(xml, fromCollab, xmlFile, label,
                        showDialog, shouldMerge, bundleProperties, removeAll,
                        letUserChangeData, limitWindows);
                }
            };
            Misc.run(runnable);
        } else {
            decodeXmlInner(xml, fromCollab, xmlFile, label, showDialog,
                shouldMerge, bundleProperties, removeAll, letUserChangeData,
                limitWindows);
        }
    }
    
    /**
     * <p>Hijacks the third part of the bundle loading pipeline.</p>
     * 
     * @see IdvPersistenceManager#decodeXmlInner(String, boolean, String, String, boolean, boolean, Hashtable, boolean, boolean)
     * @see #instantiateFromBundle(Hashtable, boolean, LoadBundleDialog, boolean, Hashtable, boolean, boolean, boolean)
     */
    protected synchronized void decodeXmlInner(String xml, boolean fromCollab, 
                                               String xmlFile, String label,
                                               boolean showDialog, 
                                               boolean shouldMerge, 
                                               Hashtable bundleProperties, 
                                               boolean didRemoveAll, 
                                               boolean letUserChangeData, 
                                               boolean limitNewWindows) {
                                               
        LoadBundleDialog loadDialog = new LoadBundleDialog(this, label);
        
        boolean inError = false;
        
        if ( !fromCollab) {
            showWaitCursor();
            if (showDialog) {
                loadDialog.showDialog();
            }
        }
        
        if (xmlFile != null) {
            getStateManager().putProperty(PROP_BUNDLEPATH,
                                          IOUtil.getFileRoot(xmlFile));
        }
        
        getStateManager().putProperty(PROP_LOADINGXML, true);
        XmlEncoder xmlEncoder = null;
        Hashtable<String, String> versions = null;
        try {
            xml = applyPropertiesToBundle(xml);
            if (xml == null) {
                return;
            }
            
//            checkForBadMaps(xmlFile);
            // perform any URL remapping that might be needed
            ServerUrlRemapper remapper = new ServerUrlRemapper(getIdv());
            Element bundleRoot = remapper.remapUrlsInBundle(xml);
            if (bundleRoot == null) {
                return;
            }
            
            remapper = null;

            xmlEncoder = getIdv().getEncoderForRead();
            Trace.call1("Decode.toObject");
            Object data = xmlEncoder.toObject(bundleRoot);
            Trace.call2("Decode.toObject");
            
            if (data != null) {
                Hashtable properties = new Hashtable();
                if (data instanceof Hashtable) {
                    Hashtable ht = (Hashtable) data;

                    versions = (Hashtable<String, String>)ht.get(ID_MCV_VERSION);

                    instantiateFromBundle(ht, fromCollab, loadDialog,
                                          shouldMerge, bundleProperties,
                                          didRemoveAll, letUserChangeData, 
                                          limitNewWindows);
                                          
                } else if (data instanceof DisplayControl) {
                    ((DisplayControl) data).initAfterUnPersistence(getIdv(),
                                                                   properties);
                    loadDialog.addDisplayControl((DisplayControl) data);
                } else if (data instanceof DataSource) {
                    getIdv().getDataManager().addDataSource((DataSource)data);
                } else if (data instanceof ColorTable) {
                    getColorTableManager().doImport(data, true);
                } else {
                    LogUtil.userErrorMessage(log_,
                                             "Decoding xml. Unknown object type:"
                                             + data.getClass().getName());
                }
                
                if ( !fromCollab && getIdv().haveCollabManager()) {
                    getCollabManager().write(getCollabManager().MSG_BUNDLE,
                                             xml);
                }
            }
        } catch (Throwable exc) {
            if (xmlFile != null) {
                logException("Error loading bundle: " + xmlFile, exc);
            } else {
                logException("Error loading bundle", exc);
            }
            
            inError = true;
        }
        
        if (!fromCollab) {
            showNormalCursor();
        }
        
        getStateManager().putProperty(PROP_BUNDLEPATH, "");
        getStateManager().putProperty(PROP_ZIDVPATH, "");
        getStateManager().putProperty(PROP_LOADINGXML, false);

        boolean generatedExceptions = false;
        if (xmlEncoder != null && xmlEncoder.getExceptions() != null) {
            generatedExceptions = !xmlEncoder.getExceptions().isEmpty();
        }

        if (generatedExceptions && getIdv().getInteractiveMode() && versions != null) {
            String versionFromBundle = versions.get("mcv.version.general");
            if (versionFromBundle != null) {
                String currentVersion = ((StateManager)getIdv().getStateManager()).getMcIdasVersion();
                int result = StateManager.compareVersions(currentVersion, versionFromBundle);
                if (result > 0) {
                    // bundle from a amazing futuristic version of mcv
                    logger.warn("Bundle is from a newer version of McIDAS-V; please consider upgrading McIDAS-V to avoid any compatibility issues.");
                } else if (result < 0) {
                    // bundle is from a stone age version of mcv
                    logger.warn("Bundle is from an older version of McIDAS-V");
                } else {
                    // bundle is from "this" version of mcv
                }
            } else {
                // bundle may have been generated by the idv or a VERY old mcv.
                logger.warn("Bundle may have been generated by the IDV or a very early version of McIDAS-V.");
            }
        }
        xmlEncoder = null;

        if (!inError && getIdv().getInteractiveMode() && xmlFile != null) {
            getIdv().addToHistoryList(xmlFile);
        }

        loadDialog.dispose();
        if (loadDialog.getShouldRemoveItems()) {
            List displayControls = loadDialog.getDisplayControls();
            for (int i = 0; i < displayControls.size(); i++) {
                try {
                    ((DisplayControl) displayControls.get(i)).doRemove();
                } catch (Exception exc) {
                    logger.warn("unexpected exception={}", exc);
                }
            }
            List dataSources = loadDialog.getDataSources();
            for (int i = 0; i < dataSources.size(); i++) {
                getIdv().removeDataSource((DataSource) dataSources.get(i));
            }
        }
        
        loadDialog.clear();
    }
    
    // initial pass at trying to fix bundles with resources mcv hasn't heard of
    private void checkForBadMaps(final String bundlePath) {
        String xpath = "//property[@name=\"InitialMap\"]/string|//property[@name=\"MapStates\"]//property[@name=\"Source\"]/string";
        for (Node node : XPathUtils.nodes(bundlePath, xpath)) {
            String mapPath = node.getTextContent();
            if (mapPath.contains("_dir/")) { // hahaha this needs some work
                List<String> toks = StringUtil.split(mapPath, "_dir/");
                if (toks.size() == 2) {
                    String plugin = toks.get(0).replace("/", "");
                    logger.trace("plugin: {} map: {}", plugin, mapPath);
                }
            } else {
                logger.trace("normal map: {}", mapPath);
            }
        }
    }

    /**
     * <p>
     * Builds a list of an incoming bundle's
     * {@link ucar.unidata.idv.ViewManager}s that are part of a component
     * group.
     * </p>
     * 
     * <p>
     * The reason for only being interested in component groups is because any
     * windows <i>not</i> using component groups will be made into a dynamic
     * skin. The associated ViewManagers do not technically exist until the
     * skin has been &quot;built&quot;, so there's nothing to do. These
     * ViewManagers must also be removed from the bundle's list of
     * ViewManagers.
     * </p>
     * 
     * <p>
     * However, any ViewManagers associated with component groups still need to
     * appear in the bundle's ViewManager list, and that's where this method
     * comes into play!
     * </p>
     * 
     * @param windows WindowInfos to be searched.
     * 
     * @return List of ViewManagers inside any component groups.
     */
    protected static List<ViewManager> extractCompGroupVMs(
        final List<WindowInfo> windows) 
    {

        List<ViewManager> newList = new ArrayList<ViewManager>();

        for (WindowInfo window : windows) {
            Collection<Object> comps =
                window.getPersistentComponents().values();

            for (Object comp : comps) {
                if (!(comp instanceof IdvComponentGroup)) {
                    continue;
                }

                IdvComponentGroup group = (IdvComponentGroup)comp;
                List<IdvComponentHolder> holders =
                    group.getDisplayComponents();

                for (IdvComponentHolder holder : holders) {
                    if (holder.getViewManagers() != null) {
                        logger.trace("extracted: {}", holder.getViewManagers().size());
                        newList.addAll(holder.getViewManagers());
                    }
                }
            }
        }
        return newList;
    }

    /**
     * <p>Does the work in fixing the collisions described in the
     * <code>instantiateFromBundle</code> javadoc. Basically just queries the
     * {@link ucar.unidata.idv.VMManager} for each 
     * {@link ucar.unidata.idv.ViewManager}. If a match is found, a new ID is
     * generated and associated with the ViewManager, its 
     * {@link ucar.unidata.idv.ViewDescriptor}, and any associated 
     * {@link ucar.unidata.idv.DisplayControl}s.</p>
     * 
     * @param vms ViewManagers in the incoming bundle.
     * 
     * @see #instantiateFromBundle(Hashtable, boolean, LoadBundleDialog, boolean, Hashtable, boolean, boolean, boolean)
     */
    protected void reverseCollisions(final List<ViewManager> vms) {
        for (ViewManager vm : vms) {
            ViewDescriptor vd = vm.getViewDescriptor();
            ViewManager current = getVMManager().findViewManager(vd);
            if (current != null) {
                ViewDescriptor oldVd = current.getViewDescriptor();
                String oldId = oldVd.getName();
                String newId = "view_" + Misc.getUniqueId();

                oldVd.setName(newId);
                current.setUniqueId(newId);

                List<DisplayControlImpl> controls = current.getControls();
                for (DisplayControlImpl control : controls) {
                    control.resetViewManager(oldId, newId);
                }
            }
        }
    }

    /**
     * <p>Builds a single window with a single component group. The group 
     * contains component holders that correspond to each window or component
     * holder stored in the incoming bundle.</p>
     * 
     * @param windows The bundle's list of 
     *                {@link ucar.unidata.idv.ui.WindowInfo}s.
     * 
     * @return List of WindowInfos that contains only one element/window.
     * 
     * @throws Exception Bubble up any exceptions from 
     *                   <code>makeImpromptuSkin</code>.
     */
    protected List<WindowInfo> injectComponentGroups(
        final List<WindowInfo> windows) throws Exception {

        McvComponentGroup group = 
            new McvComponentGroup(getIdv(), "Group");

        group.setLayout(McvComponentGroup.LAYOUT_TABS);

        Hashtable<String, McvComponentGroup> persist = 
            new Hashtable<String, McvComponentGroup>();

        for (WindowInfo window : windows) {
            List<IdvComponentHolder> holders = buildHolders(window);
            for (IdvComponentHolder holder : holders)
                group.addComponent(holder);
        }

        persist.put("comp1", group);

        // build a new window that contains our component group.
        WindowInfo limitedWindow = new WindowInfo();
        limitedWindow.setPersistentComponents(persist);
        limitedWindow.setSkinPath(Constants.BLANK_COMP_GROUP);
        limitedWindow.setIsAMainWindow(true);
        limitedWindow.setTitle("Super Test");
        limitedWindow.setViewManagers(new ArrayList<ViewManager>());
        limitedWindow.setBounds(windows.get(0).getBounds());

        // make a new list so that we can populate the list of windows with 
        // our single window.
        List<WindowInfo> newWindow = new ArrayList<WindowInfo>();
        newWindow.add(limitedWindow);
        return newWindow;
    }

    /**
     * <p>
     * Builds an altered copy of <code>windows</code> that preserves the
     * number of windows while ensuring all displays are inside component
     * holders.
     * </p>
     * 
     * @throws Exception Bubble up dynamic skin exceptions.
     * 
     * @see #injectComponentGroups(List)
     */
    // TODO: better name!!
    protected List<WindowInfo> betterInject(final List<WindowInfo> windows)
        throws Exception 
    {

        List<WindowInfo> newList = new ArrayList<WindowInfo>();

        for (WindowInfo window : windows) {
            McvComponentGroup group = new McvComponentGroup(getIdv(), "Group");

            group.setLayout(McvComponentGroup.LAYOUT_TABS);

            Hashtable<String, McvComponentGroup> persist =
                new Hashtable<String, McvComponentGroup>();

            List<IdvComponentHolder> holders = buildHolders(window);
            for (IdvComponentHolder holder : holders) {
                group.addComponent(holder);
            }

            persist.put("comp1", group);
            WindowInfo newWindow = new WindowInfo();
            newWindow.setPersistentComponents(persist);
            newWindow.setSkinPath(Constants.BLANK_COMP_GROUP);
            newWindow.setIsAMainWindow(window.getIsAMainWindow());
            newWindow.setViewManagers(new ArrayList<ViewManager>());
            newWindow.setBounds(window.getBounds());

            newList.add(newWindow);
        }
        return newList;
    }

    /**
     * <p>Builds a list of component holders with all of <code>window</code>'s
     * displays.</p>
     * 
     * @throws Exception Bubble up any problems creating a dynamic skin.
     */
    // TODO: refactor
    protected List<IdvComponentHolder> buildHolders(final WindowInfo window) 
        throws Exception {

        List<IdvComponentHolder> holders = 
            new ArrayList<IdvComponentHolder>();

        if (!window.getPersistentComponents().isEmpty()) {
            Collection<Object> comps = 
                window.getPersistentComponents().values();

            for (Object comp : comps) {
                if (!(comp instanceof IdvComponentGroup)) {
                    continue;
                }

                IdvComponentGroup group = (IdvComponentGroup)comp;
                holders.addAll(McVGuiUtils.getComponentHolders(group));
            }
        } else {
            holders.add(makeDynSkin(window));
        }

        return holders;
    }

    /**
     * <p>Builds a list of any dynamic skins in the bundle and adds them to the
     * UIMananger's &quot;cache&quot; of encountered ViewManagers.</p>
     * 
     * @param windows The bundle's windows.
     * 
     * @return Any dynamic skins in <code>windows</code>.
     */
    public List<ViewManager> mapDynamicSkins(final List<WindowInfo> windows) {
        List<ViewManager> vms = new ArrayList<ViewManager>();
        for (WindowInfo window : windows) {
            Collection<Object> comps = 
                window.getPersistentComponents().values();

            for (Object comp : comps) {
                if (!(comp instanceof IdvComponentGroup)) {
                    continue;
                }

                List<IdvComponentHolder> holders = 
                    new ArrayList<IdvComponentHolder>(
                            ((IdvComponentGroup)comp).getDisplayComponents());

                for (IdvComponentHolder holder : holders) {
                    if (!McVGuiUtils.isDynamicSkin(holder)) {
                        continue;
                    }
                    List<ViewManager> tmpvms = holder.getViewManagers();
                    for (ViewManager vm : tmpvms) {
                        vms.add(vm);
                        UIManager.savedViewManagers.put(
                            vm.getViewDescriptor().getName(), vm);
                    }
                    holder.setViewManagers(new ArrayList<ViewManager>());
                }
            }
        }
        return vms;
    }

    /**
     * Attempts to reconcile McIDAS-V's ability to easily load all files in a
     * directory with the way the IDV expects file data sources to behave upon
     * unpersistence.
     * 
     * <p>The problem is twofold: the paths referenced in the data source's 
     * {@code Sources} may not exist, and the <i>persistence</i> code combines
     * each individual file into a blob.
     * 
     * <p>The current solution is to note that the data source's 
     * {@link PollingInfo} is used by {@link ucar.unidata.data.FilesDataSource#initWithPollingInfo}
     * to replace the contents of the data source's file paths. Simply 
     * overwrite {@code PollingInfo#filePaths} with the path to the blob.
     * 
     * @param ds {@code List} of {@link DataSourceImpl}s to inspect and/or fix.
     * Cannot be {@code null}.
     * 
     * @see #isBulkDataSource(DataSourceImpl)
     */
    private void fixBulkDataSources(final List<DataSourceImpl> ds) {
        String zidvPath = getStateManager().getProperty(PROP_ZIDVPATH, "");

        // bail out if the macro replacement cannot work
        if (zidvPath.length() == 0) {
            return;
        }

        for (DataSourceImpl d : ds) {
            boolean isBulk = isBulkDataSource(d);
            if (!isBulk) {
                continue;
            }

            // err... now do the macro sub and replace the contents of 
            // data paths with the singular element in temp paths?
            List<String> tempPaths = new ArrayList<String>(d.getTmpPaths());
            String tempPath = tempPaths.get(0);
            tempPath = tempPath.replace(MACRO_ZIDVPATH, zidvPath);
            tempPaths.set(0, tempPath);
            PollingInfo p = d.getPollingInfo();
            p.setFilePaths(tempPaths);
        }
    }

    /**
     * Attempts to determine whether or not a given {@link DataSourceImpl} is
     * the result of a McIDAS-V {@literal "bulk load"}.
     * 
     * @param d {@code DataSourceImpl} to check. Cannot be {@code null}.
     * 
     * @return {@code true} if the {@code DataSourceImpl} matched the criteria.
     */
    private boolean isBulkDataSource(final DataSourceImpl d) {
        Hashtable properties = d.getProperties();
        if (properties.containsKey("bulk.load")) {
            // woohoo! no need to do the guesswork.
            Object value = properties.get("bulk.load");
            if (value instanceof String) {
                return Boolean.valueOf((String)value);
            } else if (value instanceof Boolean) {
                return (Boolean)value;
            }
        }

        DataSourceDescriptor desc = d.getDescriptor();
        boolean localFiles = desc.getFileSelection();

        List filePaths = d.getDataPaths();
        List tempPaths = d.getTmpPaths();
        if (filePaths == null || filePaths.isEmpty()) {
            return false;
        }

        if (tempPaths == null || tempPaths.isEmpty()) {
            return false;
        }

        // the least-involved heuristic i've found is:
        // localFiles == true
        // tempPaths.size() == 1 && filePaths.size() >= 2
        // and then we have a bulk load...
        // if those checks don't suffice, you can also look for the "prop.pollinfo" key
        // if the PollingInfo object has a filePaths list, with one element whose last directory matches 
        // the data source "name" (then you are probably good).
        if ((localFiles == true) && ((tempPaths.size() == 1) && (filePaths.size() >= 2))) {
            return true;
        }

        // end of line
        return false;
    }

    /**
     * <p>Overridden so that McIDAS-V can preempt the IDV's bundle loading. 
     * There will be problems if any of the incoming 
     * {@link ucar.unidata.idv.ViewManager}s share an ID with an existing 
     * ViewManager. While this case may seem unlikely, it can be triggered 
     * when loading a bundle and then reloading. The problem is that the 
     * ViewManagers are the same, and if the previous ViewManagers were not 
     * removed, the IDV doesn't know what to do.</p>
     * 
     * <p>Assigning the incoming ViewManagers a new ID, <i>and associating its
     * {@link ucar.unidata.idv.ViewDescriptor}s and 
     * {@link ucar.unidata.idv.DisplayControl}s</i> with the new ID fixes this
     * problem.</p>
     * 
     * <p>McIDAS-V also allows the user to limit the number of new windows the
     * bundle may create. If enabled, one new window will be created, and any
     * additional windows will become tabs (component holders) inside the new
     * window.</p>
     * 
     * <p>McIDAS-V also prefers the bundles being loaded to be in a 
     * semi-regular regular state. For example, say you have bundle containing
     * only data. The bundle will probably not contain lists of WindowInfos or
     * ViewManagers. Perhaps the bundle contains nested component groups as 
     * well! McIDAS-V will alter the unpersisted bundle state (<i>not the 
     * actual bundle file</i>) to make it fit into the expected idiom. Mostly
     * this just entails wrapping things in component groups and holders while
     * &quot;flattening&quot; any nested component groups.</p>
     * 
     * @param ht Holds unpersisted objects.
     * 
     * @param fromCollab Did the bundle come from the collab stuff?
     * 
     * @param loadDialog Show the bundle loading dialog?
     * 
     * @param shouldMerge Merge bundle contents into an existing window?
     * 
     * @param bundleProperties If non-null, use the set of time indices for 
     *                         data sources?
     * 
     * @param didRemoveAll Remove all data and displays?
     * 
     * @param letUserChangeData Allow changes to the data path?
     * 
     * @param limitNewWindows Only create one new window?
     * 
     * @see IdvPersistenceManager#instantiateFromBundle(Hashtable, boolean, LoadBundleDialog, boolean, Hashtable, boolean, boolean)
     */
    // TODO: check the accuracy of the bundleProperties javadoc above
    protected void instantiateFromBundle(Hashtable ht, 
                                         boolean fromCollab,
                                         LoadBundleDialog loadDialog,
                                         boolean shouldMerge,
                                         Hashtable bundleProperties,
                                         boolean didRemoveAll,
                                         boolean letUserChangeData,
                                         boolean limitNewWindows) 
            throws Exception {

        // hacky way of allowing other classes to determine whether or not
        // a bundle is loading
        bundleLoading = true;

        // every bundle should have lists corresponding to these ids
        final String[] important = { 
            ID_VIEWMANAGERS, ID_DISPLAYCONTROLS, ID_WINDOWS,
        };
        populateEssentialLists(important, ht);

        List<ViewManager> vms = (List)ht.get(ID_VIEWMANAGERS);
        List<DisplayControlImpl> controls = (List)ht.get(ID_DISPLAYCONTROLS);
        List<WindowInfo> windows = (List)ht.get(ID_WINDOWS);

        List<DataSourceImpl> dataSources = (List)ht.get("datasources");
        if (dataSources != null) {
            fixBulkDataSources(dataSources);
        }

        // older hydra bundles may contain ReadoutProbes in the list of
        // display controls. these are not needed, so they get removed.
//        controls = removeReadoutProbes(controls);
        ht.put(ID_DISPLAYCONTROLS, controls);

        if (vms.isEmpty() && windows.isEmpty() && !controls.isEmpty()) {
            List<ViewManager> fudged = generateViewManagers(controls);
            List<WindowInfo> buh = wrapViewManagers(fudged);

            windows.addAll(buh);
            vms.addAll(fudged);
        }

        // make sure that the list of windows contains no nested comp groups
        flattenWindows(windows);

        // remove any component holders that don't contain displays
        windows = removeUIHolders(windows);

        // generate new IDs for any collisions--typically happens if the same
        // bundle is loaded without removing the previously loaded VMs.
        reverseCollisions(vms);

        // if the incoming bundle has dynamic skins, we've gotta be sure to
        // remove their ViewManagers from the bundle's list of ViewManagers!
        // remember, because they are dynamic skins, the ViewManagers should
        // not exist until the skin is built.
        if (McVGuiUtils.hasDynamicSkins(windows)) {
            mapDynamicSkins(windows);
        }

        List<WindowInfo> newWindows;
        if (limitNewWindows && windows.size() > 1) {
            newWindows = injectComponentGroups(windows);
        } else {
            newWindows = betterInject(windows);
        }

//          if (limitNewWindows && windows.size() > 1) {
//              // make a single new window with a single component group. 
//              // the group's holders will correspond to each window in the 
//              // bundle.
//              List<WindowInfo> newWindows = injectComponentGroups(windows);
//              ht.put(ID_WINDOWS, newWindows);
//
//              // if there are any component groups in the bundle, we must 
//              // take care that their VMs appear in this list. VMs wrapped 
//              // in dynamic skins don't "exist" at this point, so they do 
//              // not need to be in this list.
//              ht.put(ID_VIEWMANAGERS, extractCompGroupVMs(newWindows));
//          }

        ht.put(ID_WINDOWS, newWindows);

        ht.put(ID_VIEWMANAGERS, extractCompGroupVMs(newWindows));

        // hand our modified bundle information off to the IDV
        super.instantiateFromBundle(ht, fromCollab, loadDialog, shouldMerge, 
                                    bundleProperties, didRemoveAll, 
                                    letUserChangeData);

        // no longer needed; the bundle is done loading.
        UIManager.savedViewManagers.clear();
        bundleLoading = false;
    }

//    private List<DisplayControlImpl> removeReadoutProbes(final List<DisplayControlImpl> controls) {
//        List<DisplayControlImpl> filtered = new ArrayList<DisplayControlImpl>();
//        for (DisplayControlImpl dc : controls) {
//            if (dc instanceof ReadoutProbe) {
//                try {
//                    dc.doRemove();
//                } catch (Exception e) {
//                    LogUtil.logException("Problem removing redundant readout probe", e);
//                }
//            } else if (dc != null) {
//                filtered.add(dc);
//            }
//        }
//        return filtered;
//    }

    private List<WindowInfo> wrapViewManagers(final List<ViewManager> vms) {
        List<WindowInfo> windows = new ArrayList<WindowInfo>(vms.size());
        for (ViewManager vm : vms) {
            WindowInfo window = new WindowInfo();
            window.setIsAMainWindow(true);
            window.setSkinPath("/ucar/unidata/idv/resources/skins/skin.xml");
            window.setTitle("asdf");
            List<ViewManager> vmList = new ArrayList<ViewManager>();
            vmList.add(vm);
            window.setViewManagers(vmList);
            window.setBounds(new Rectangle(200, 200, 200, 200));
            windows.add(window);
        }
        return windows;
    }

    private List<ViewManager> generateViewManagers(final List<DisplayControlImpl> controls) {
        List<ViewManager> vms = new ArrayList<ViewManager>(controls.size());
        for (DisplayControlImpl control : controls) {
            ViewManager vm = getVMManager().findOrCreateViewManager(control.getDefaultViewDescriptor(), "");
            vms.add(vm);
        }
        return vms;
    }

    /**
     * <p>Alters <code>windows</code> so that no windows in the bundle contain
     * nested component groups.</p>
     */
    protected void flattenWindows(final List<WindowInfo> windows) {
        for (WindowInfo window : windows) {
            Map<String, Object> persist = window.getPersistentComponents();
            Set<Map.Entry<String, Object>> blah = persist.entrySet();
            for (Map.Entry<String, Object> entry : blah) {
                if (!(entry.getValue() instanceof IdvComponentGroup)) {
                    continue;
                }

                IdvComponentGroup group = (IdvComponentGroup)entry.getValue();
                if (McVGuiUtils.hasNestedGroups(group)) {
                    entry.setValue(flattenGroup(group));
                }
            }
        }
    }

    /**
     * @return An altered version of <code>nested</code> that contains no 
     *         nested component groups.
     */
    protected IdvComponentGroup flattenGroup(final IdvComponentGroup nested) {
        IdvComponentGroup flat = 
            new IdvComponentGroup(getIdv(), nested.getName());

        flat.setLayout(nested.getLayout());
        flat.setShowHeader(nested.getShowHeader());
        flat.setUniqueId(nested.getUniqueId());

        List<IdvComponentHolder> holders = 
            McVGuiUtils.getComponentHolders(nested);

        for (IdvComponentHolder holder : holders) {
            flat.addComponent(holder);
            holder.setParent(flat);
        }

        return flat;
    }

    /**
     * @return An altered <code>group</code> containing only component holders
     *         with displays.
     */
    protected static List<IdvComponentHolder> removeUIHolders(final IdvComponentGroup group) {
        List<IdvComponentHolder> newHolders = 
            new ArrayList<IdvComponentHolder>(group.getDisplayComponents());

        for (IdvComponentHolder holder : newHolders) {
            if (McVGuiUtils.isUIHolder(holder)) {
                newHolders.remove(holder);
            }
        }

        return newHolders;
    }

    /**
     * <p>Ensures that the lists corresponding to the ids in <code>ids</code>
     * actually exist in <code>table</code>, even if they are empty.</p>
     */
    // TODO: not a fan of this method.
    protected static void populateEssentialLists(final String[] ids, final Hashtable<String, Object> table) {
        for (String id : ids) {
            if (table.get(id) == null) {
                table.put(id, new ArrayList<Object>());
            }
        }
    }

    /**
     * <p>Returns an altered copy of <code>windows</code> containing only 
     * component holders that have displays.</p>
     * 
     * <p>The IDV allows users to embed HTML controls or things like the 
     * dashboard into component holders. This ability, while powerful, could
     * make for a confusing UI.</p>
     */
    protected static List<WindowInfo> removeUIHolders(
        final List<WindowInfo> windows) {

        List<WindowInfo> newList = new ArrayList<WindowInfo>();
        for (WindowInfo window : windows) {
            // TODO: ought to write a WindowInfo cloning method
            WindowInfo newWin = new WindowInfo();
            newWin.setViewManagers(window.getViewManagers());
            newWin.setSkinPath(window.getSkinPath());
            newWin.setIsAMainWindow(window.getIsAMainWindow());
            newWin.setBounds(window.getBounds());
            newWin.setTitle(window.getTitle());

            Hashtable<String, IdvComponentGroup> persist = 
                new Hashtable<String, IdvComponentGroup>(
                    window.getPersistentComponents()); 

            for (Map.Entry<String, IdvComponentGroup> e : persist.entrySet()) {

                IdvComponentGroup g = e.getValue();

                List<IdvComponentHolder> holders = g.getDisplayComponents();
                if (holders == null || holders.isEmpty()) {
                    continue;
                }

                List<IdvComponentHolder> newHolders = 
                    new ArrayList<IdvComponentHolder>();

                // filter out any holders that don't contain view managers
                for (IdvComponentHolder holder : holders) {
                    if (!McVGuiUtils.isUIHolder(holder)) {
                        newHolders.add(holder);
                    }
                }

                g.setDisplayComponents(newHolders);
            }

            newWin.setPersistentComponents(persist);
            newList.add(newWin);
        }
        return newList;
    }

    /**
     * <p>Uses the {@link ucar.unidata.idv.ViewManager}s in <code>info</code> 
     * to build a dynamic skin.</p>
     * 
     * @param info Window that needs to become a dynamic skin.
     * 
     * @return A {@link edu.wisc.ssec.mcidasv.ui.McvComponentHolder} containing 
     *         the ViewManagers inside <code>info</code>.
     * 
     * @throws Exception Bubble up any XML problems.
     */
    public McvComponentHolder makeDynSkin(final WindowInfo info) throws Exception {
        Document doc = XmlUtil.getDocument(SIMPLE_SKIN_TEMPLATE);
        Element root = doc.getDocumentElement();

        Element panel = XmlUtil.findElement(root, DYNSKIN_TAG_PANEL,
                                            DYNSKIN_ATTR_ID, DYNSKIN_ID_VALUE);

        List<ViewManager> vms = info.getViewManagers();

        panel.setAttribute(DYNSKIN_ATTR_COLS, Integer.toString(vms.size()));

        for (ViewManager vm : vms) {

            Element view = doc.createElement(DYNSKIN_TAG_VIEW);

            view.setAttribute(DYNSKIN_ATTR_CLASS, vm.getClass().getName());
            view.setAttribute(DYNSKIN_ATTR_VIEWID, vm.getUniqueId());

            StringBuffer props = new StringBuffer(DYNSKIN_PROPS_GENERAL);

            if (vm instanceof MapViewManager) {
                if (((MapViewManager)vm).getUseGlobeDisplay()) {
                    props.append(DYNSKIN_PROPS_GLOBE);
                }
            }

            view.setAttribute(DYNSKIN_ATTR_PROPS, props.toString());

            panel.appendChild(view);

            UIManager.savedViewManagers.put(vm.getViewDescriptor().getName(), vm);
        }

        McvComponentHolder holder = 
            new McvComponentHolder(getIdv(), XmlUtil.toString(root));

        holder.setType(McvComponentHolder.TYPE_DYNAMIC_SKIN);
        holder.setName(DYNSKIN_TMPNAME);
        holder.doMakeContents();
        return holder;
    }

    public static IdvWindow buildDynamicSkin(int width, int height, int rows, int cols, List<PyObject> panelTypes) throws Exception {
        Document doc = XmlUtil.getDocument(SIMPLE_SKIN_TEMPLATE);
        Element root = doc.getDocumentElement();
        Element panel = XmlUtil.findElement(root, DYNSKIN_TAG_PANEL, DYNSKIN_ATTR_ID, DYNSKIN_ID_VALUE);
        panel.setAttribute(DYNSKIN_ATTR_ROWS, Integer.toString(rows));
        panel.setAttribute(DYNSKIN_ATTR_COLS, Integer.toString(cols));
        Element view = doc.createElement(DYNSKIN_TAG_VIEW);
        for (PyObject panelType : panelTypes) {
            String panelTypeRepr = panelType.__repr__().toString();
            Element node = doc.createElement(IdvUIManager.COMP_VIEW);
            StringBuilder props = new StringBuilder(DYNSKIN_PROPS_GENERAL);
            props.append("size=").append(width).append(':').append(height).append(';');
//            logger.trace("window props: {}", props);
            if ("MAP".equals(panelTypeRepr)) {
                node.setAttribute(IdvXmlUi.ATTR_CLASS, "ucar.unidata.idv.MapViewManager");
            } else if ("GLOBE".equals(panelTypeRepr)) {
                node.setAttribute(IdvXmlUi.ATTR_CLASS, "ucar.unidata.idv.MapViewManager");
                props.append(DYNSKIN_PROPS_GLOBE);
            } else if ("TRANSECT".equals(panelTypeRepr)) {
                node.setAttribute(IdvXmlUi.ATTR_CLASS, "ucar.unidata.idv.TransectViewManager");
            } else if ("MAP2D".equals(panelTypeRepr)) {
                node.setAttribute(IdvXmlUi.ATTR_CLASS, "ucar.unidata.idv.MapViewManager");
                props.append("use3D=false;");
            }
            view.setAttribute(DYNSKIN_ATTR_PROPS, props.toString());
            view.appendChild(node);
        }
        panel.appendChild(view);
        UIManager uiManager = (UIManager)McIDASV.getStaticMcv().getIdvUIManager();
        Element skinRoot = XmlUtil.getRoot(Constants.BLANK_COMP_GROUP, PersistenceManager.class);
        IdvWindow window = uiManager.createNewWindow(null, false, "McIDAS-V", Constants.BLANK_COMP_GROUP, skinRoot, false, null);
        ComponentGroup group = window.getComponentGroups().get(0);
        McvComponentHolder holder = new McvComponentHolder(McIDASV.getStaticMcv(), XmlUtil.toString(root));
        holder.setType(McvComponentHolder.TYPE_DYNAMIC_SKIN);
        holder.setName(DYNSKIN_TMPNAME);
        group.addComponent(holder);
        return window;
    }

    private static final String DYNSKIN_TMPNAME = "McIDAS-V buildWindow";
    private static final String DYNSKIN_TAG_PANEL = "panel";
    private static final String DYNSKIN_TAG_VIEW = "idv.view";
    private static final String DYNSKIN_ATTR_ID = "id";
    private static final String DYNSKIN_ATTR_COLS = "cols";
    private static final String DYNSKIN_ATTR_ROWS = "rows";
    private static final String DYNSKIN_ATTR_PROPS = "properties";
    private static final String DYNSKIN_ATTR_CLASS = "class";
    private static final String DYNSKIN_ATTR_VIEWID = "viewid";
    private static final String DYNSKIN_PROPS_GLOBE = "useGlobeDisplay=true;initialMapResources=/edu/wisc/ssec/mcidasv/resources/maps.xml;";
    private static final String DYNSKIN_PROPS_GENERAL = "clickToFocus=true;showToolBars=true;shareViews=true;showControlLegend=true;initialSplitPaneLocation=0.2;legendOnLeft=false;showEarthNavPanel=false;showControlLegend=false;shareGroup=view%versionuid%;";
    private static final String DYNSKIN_ID_VALUE = "mcv.content";

    /** XML template for generating dynamic skins. */
    private static final String SIMPLE_SKIN_TEMPLATE = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<skin embedded=\"true\">\n" +
        "  <ui>\n" +
        "    <panel layout=\"border\" bgcolor=\"red\">\n" +
        "      <idv.menubar place=\"North\"/>\n" +
        "      <panel layout=\"border\" place=\"Center\">\n" +
        "        <panel layout=\"flow\" place=\"North\">\n" +
        "          <idv.toolbar id=\"idv.toolbar\" place=\"West\"/>\n" +
        "          <panel id=\"idv.favoritesbar\" place=\"North\"/>\n" +
        "        </panel>\n" +
        "        <panel embeddednode=\"true\" id=\"mcv.content\" layout=\"grid\" place=\"Center\">\n" +
        "        </panel>" +
        "      </panel>\n" +
        "      <component idref=\"bottom_bar\"/>\n" +
        "    </panel>\n" +
        "  </ui>\n" +
        "  <styles>\n" +
        "    <style class=\"iconbtn\" space=\"2\" mouse_enter=\"ui.setText(idv.messagelabel,prop:tooltip);ui.setBorder(this,etched);\" mouse_exit=\"ui.setText(idv.messagelabel,);ui.setBorder(this,button);\"/>\n" +
        "    <style class=\"textbtn\" space=\"2\" mouse_enter=\"ui.setText(idv.messagelabel,prop:tooltip)\" mouse_exit=\"ui.setText(idv.messagelabel,)\"/>\n" +
        "  </styles>\n" +
        "  <components>\n" +
        "    <idv.statusbar place=\"South\" id=\"bottom_bar\"/>\n" +
        "  </components>\n" +
        "  <properties>\n" +
        "    <property name=\"icon.wait.wait\" value=\"/ucar/unidata/idv/images/wait.gif\"/>\n" +
        "  </properties>\n" +
        "</skin>\n";
    
    
    
    /**
     * Write the parameter sets
     */
    public void writeParameterSets() {
    	if (parameterSets != null) {
    		
    		//DAVEP: why is our write failing?
    		if (!parameterSets.hasWritableResource()) {
    			System.err.println("Oops--lost writable resource");
    		}
    		
    		try {
    			parameterSets.writeWritable();
    		} catch (IOException exc) {
            	LogUtil.logException("Error writing " + parameterSets.getDescription(), exc);
    		}
    		
    		parameterSets.setWritableDocument(parameterSetsDocument, parameterSetsRoot);
    	}
    }
    
    /**
     * Get the node representing the parameterType
     * 
     * @param parameterType What type of parameter set
     *
     * @return Element representing parameterType node
     */
    private Element getParameterTypeNode(String parameterType) {
    	if (parameterSets == null) {
    		parameterSets = getIdv().getResourceManager().getXmlResources(ResourceManager.RSC_PARAMETERSETS);
            if (parameterSets.hasWritableResource()) {
                parameterSetsDocument = parameterSets.getWritableDocument("<parametersets></parametersets>");
                parameterSetsRoot = parameterSets.getWritableRoot("<parametersets></parametersets>");
            }
            else {
            	System.err.println("No writable resource found");
            	return null;
            }
        }

    	Element parameterTypeNode = null;
    	try {
    		List<Element> rootTypes = XmlUtil.findChildren(parameterSetsRoot, parameterType);
    		if (rootTypes.size() == 0) {
    			parameterTypeNode = parameterSetsDocument.createElement(parameterType);
    			parameterSetsRoot.appendChild(parameterTypeNode);
    			System.out.println("Created new " + parameterType + " node");
    			writeParameterSets();
    		}
    		else if (rootTypes.size() == 1) {
    			parameterTypeNode = rootTypes.get(0);
    			System.out.println("Found existing " + parameterType + " node");
    		}
    	} catch (Exception exc) {
    		LogUtil.logException("Error loading " + parameterSets.getDescription(), exc);
    	}
    	return parameterTypeNode;
    }

    /**
     * Get a list of all of the categories for the given parameterType
     *
     * @param parameterType What type of parameter set
     *
     * @return List of (String) categories
     */
    public List<String> getAllParameterSetCategories(String parameterType) {
    	List<String> allCategories = new ArrayList<String>();
    	try {
    		Element rootType = getParameterTypeNode(parameterType);
    		if (rootType!=null) {
    			allCategories =
    				XmlUtil.findDescendantNamesWithSeparator(rootType, TAG_FOLDER, CATEGORY_SEPARATOR);
    		}
        } catch (Exception exc) {
        	LogUtil.logException("Error loading " + parameterSets.getDescription(), exc);
        }

        return allCategories;
    }
    

    /**
     * Get the list of {@link ParameterSet}s that are writable
     *
     * @param parameterType The type of parameter set
     *
     * @return List of writable parameter sets
     */
    public List<ParameterSet> getAllParameterSets(String parameterType) {
    	List<ParameterSet> allParameterSets = new ArrayList<ParameterSet>();
        try {
    		Element rootType = getParameterTypeNode(parameterType);
    		if (rootType!=null) {
    	    	List<String> defaults =
    	    		XmlUtil.findDescendantNamesWithSeparator(rootType, TAG_DEFAULT, CATEGORY_SEPARATOR);
    			
    			for (final String aDefault : defaults) {
    				Element anElement = XmlUtil.getElementAtNamedPath(rootType, stringToCategories(aDefault));
    				List<String> defaultParts = stringToCategories(aDefault);
    				int lastIndex = defaultParts.size() - 1;
    				String defaultName = defaultParts.get(lastIndex);
    				defaultParts.remove(lastIndex);
    				String folderName = StringUtil.join(CATEGORY_SEPARATOR, defaultParts);
    				ParameterSet newSet = new ParameterSet(defaultName, folderName, parameterType, anElement);
    				allParameterSets.add(newSet);
    			}

        	}
        } catch (Exception exc) {
        	LogUtil.logException("Error loading " + ResourceManager.RSC_PARAMETERSETS.getDescription(), exc);
        }
        
        return allParameterSets;
    }


    /**
     * Add the directory
     *
     * @param parameterType The type of parameter set
     * @param category The category (really a ">" delimited string)
     * @return true if the create was successful. False if there already is a category with that name
     */
    public boolean addParameterSetCategory(String parameterType, String category) {
    	System.out.println("addParameterSetCategory: " + category);
		Element rootType = getParameterTypeNode(parameterType);
    	XmlUtil.makeElementAtNamedPath(rootType, stringToCategories(category), TAG_FOLDER);
    	writeParameterSets();
        return true;
    }
    
    /**
     * Delete the given parameter set
     *
     * @param parameterType The type of parameter set
     * @param set Parameter set to delete.
     */
    public void deleteParameterSet(String parameterType, ParameterSet set) {
    	Element parameterElement = set.getElement();
    	Node parentNode = parameterElement.getParentNode();
    	parentNode.removeChild((Node)parameterElement);
    	writeParameterSets();
    }


    /**
     * Delete the directory and all of its contents
     * that the given category represents.
     *
     * @param parameterType The type of parameter set
     * @param category The category (really a ">" delimited string)
     */
    public void deleteParameterSetCategory(String parameterType, String category) {
		Element rootType = getParameterTypeNode(parameterType);
    	Element parameterSetElement = XmlUtil.getElementAtNamedPath(rootType, stringToCategories(category));
    	Node parentNode = parameterSetElement.getParentNode();
    	parentNode.removeChild((Node)parameterSetElement);
    	writeParameterSets();
    }


    /**
     * Rename the parameter set
     *
     * @param parameterType The type of parameter set
     * @param set The parameter set
     */
    public void renameParameterSet(String parameterType, ParameterSet set) {
    	String name = set.getName();
    	Element parameterElement = set.getElement();
//        while (true) {
            name = GuiUtils.getInput("Enter a new name", "Name: ", name);
            if (name == null) {
                return;
            }
			name = StringUtil.replaceList(name.trim(),
					new String[] { "<", ">", "/", "\\", "\"" },
					new String[] { "_", "_", "_", "_",  "_"  }
			);
            if (name.length() == 0) {
                return;
            }
//        }
    	parameterElement.setAttribute("name", name);
    	writeParameterSets();
    }
    
    /**
     * Move the bundle to the given category area
     *
     * @param parameterType The type of parameter set
     * @param set The parameter set
     * @param categories Where to move to
     */
    public void moveParameterSet(String parameterType, ParameterSet set, List categories) {
		Element rootType = getParameterTypeNode(parameterType);
    	Element parameterElement = set.getElement();
    	Node parentNode = parameterElement.getParentNode();
    	parentNode.removeChild((Node)parameterElement);
    	Node newParentNode = XmlUtil.getElementAtNamedPath(rootType, categories);
    	newParentNode.appendChild(parameterElement);
    	writeParameterSets();
    }

    /**
     * Move the bundle category
     *
     * @param parameterType The type of parameter set
     * @param fromCategories The category to move
     * @param toCategories Where to move to
     */
    public void moveParameterSetCategory(String parameterType, List fromCategories, List toCategories) {
		Element rootType = getParameterTypeNode(parameterType);
    	Element parameterSetElementFrom = XmlUtil.getElementAtNamedPath(rootType, fromCategories);
    	Node parentNode = parameterSetElementFrom.getParentNode();
    	parentNode.removeChild((Node)parameterSetElementFrom);
    	Node parentNodeTo = (Node)XmlUtil.getElementAtNamedPath(rootType, toCategories);
    	parentNodeTo.appendChild(parameterSetElementFrom);
    	writeParameterSets();
    }

    /**
     * Show the Save Parameter Set dialog
     */
    public boolean saveParameterSet(String parameterType, Hashtable parameterValues) {

    	try {
    		String title = "Save Parameter Set";

    		// Create the category dropdown
    		List<String> categories = getAllParameterSetCategories(parameterType);
    		final JComboBox catBox = new JComboBox();
    		catBox.setToolTipText(
    				"<html>Categories can be entered manually. <br>Use '>' as the category delimiter. e.g.:<br>General > Subcategory</html>");
    		catBox.setEditable(true);
    		McVGuiUtils.setComponentWidth(catBox, McVGuiUtils.ELEMENT_DOUBLE_WIDTH);
    		GuiUtils.setListData(catBox, categories);

    		// Create the default name dropdown
    		final JComboBox nameBox = new JComboBox();
    		nameBox.setEditable(true);
    		List tails = new ArrayList();

    		List<ParameterSet> pSets = getAllParameterSets(parameterType);
    		for (int i = 0; i < pSets.size(); i++) {
    			ParameterSet pSet = pSets.get(i);
    			tails.add(new TwoFacedObject(pSet.getName(), pSet));
    		}
    		java.util.Collections.sort(tails);

    		tails.add(0, new TwoFacedObject("", null));
    		GuiUtils.setListData(nameBox, tails);
    		nameBox.addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent ae) {
    				Object selected = nameBox.getSelectedItem();
    				if ( !(selected instanceof TwoFacedObject)) {
    					return;
    				}
    				TwoFacedObject tfo = (TwoFacedObject) selected;
    				List cats = ((ParameterSet) tfo.getId()).getCategories();
    				//    			if ((cats.size() > 0) && !catSelected) {
    				if ((cats.size() > 0)) {
    					catBox.setSelectedItem(
    							StringUtil.join(CATEGORY_SEPARATOR, cats));
    				}
    			}
    		});

    		JPanel panel = McVGuiUtils.sideBySide(
    				McVGuiUtils.makeLabeledComponent("Category:", catBox),
    				McVGuiUtils.makeLabeledComponent("Name:", nameBox)
    		);

    		String name = "";
    		String category = "";
    		while (true) {
    			if ( !GuiUtils.askOkCancel(title, panel)) {
    				return false;
    			}
    			name = StringUtil.replaceList(nameBox.getSelectedItem().toString().trim(),
    					new String[] { "<", ">", "/", "\\", "\"" },
    					new String[] { "_", "_", "_", "_",  "_"  }
    			);
    			if (name.length() == 0) {
    				LogUtil.userMessage("Please enter a name");
    				continue;
    			}
    			category = StringUtil.replaceList(catBox.getSelectedItem().toString().trim(),
    					new String[] { "/", "\\", "\"" },
    					new String[] { "_", "_",  "_"  }
    			);
    			if (category.length() == 0) {
    				LogUtil.userMessage("Please enter a category");
    				continue;
    			}
    			break;
    		}

    		// Create a new element from the hashtable
    		Element rootType = getParameterTypeNode(parameterType);
    		Element parameterElement = parameterSetsDocument.createElement(TAG_DEFAULT);
    	    for (Enumeration e = parameterValues.keys(); e.hasMoreElements(); ) {
    	    	Object nextKey = e.nextElement();
    	    	String attribute = (String)nextKey;
    	    	String value = (String)parameterValues.get(nextKey);
    	    	parameterElement.setAttribute(attribute, value);
    	    }

    		// Set the name to the one we entered
    		parameterElement.setAttribute(ATTR_NAME, name);

    		Element categoryNode = XmlUtil.makeElementAtNamedPath(rootType, stringToCategories(category), TAG_FOLDER);
//    		Element categoryNode = XmlUtil.getElementAtNamedPath(rootType, stringToCategories(category));

    		categoryNode.appendChild(parameterElement);    	
    		writeParameterSets();
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    		return false;
    	}
    	
    	return true;
    }

}
