/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
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
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.LoadBundleDialog;
import ucar.unidata.idv.ui.WindowInfo;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PollingInfo;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.xml.XmlUtil;
import edu.wisc.ssec.mcidasv.probes.ReadoutProbe;
import edu.wisc.ssec.mcidasv.ui.McvComponentGroup;
import edu.wisc.ssec.mcidasv.ui.McvComponentHolder;
import edu.wisc.ssec.mcidasv.ui.UIManager;
import edu.wisc.ssec.mcidasv.util.CompGroups;

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

    /**
     * Macro used as a place holder for wherever the IDV decides to place 
     * extracted contents of a bundle. 
     */
    private static final String MACRO_ZIDVPATH = "%"+PROP_ZIDVPATH+"%";

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

    /**
     * Java requires this constructor. 
     */
    public PersistenceManager() {
        super(null);
    }

    /**
     * @see ucar.unidata.idv.IdvPersistenceManager#PersistenceManager(IntegratedDataViewer)
     */
    public PersistenceManager(IntegratedDataViewer idv) {
        super(idv);
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
//        System.err.println("getMergeBundledLayers="+mergeBundledLayers);
        return mergeBundledLayers;
    }

    private void setMergeBundledLayers(final boolean newValue) {
//        System.err.println("setMergeBundledLayers: old="+mergeBundledLayers+" new="+newValue);
        mergeBundledLayers = newValue;
    }

    @Override public boolean getSaveDataSources() {
        boolean result = false;
        if (!savingDefaultLayout)
            result = super.getSaveDataSources();
//        System.err.println("getSaveDataSources="+result+" savingDefaultLayout="+savingDefaultLayout);
        return result;
    }

    @Override public boolean getSaveDisplays() {
        boolean result = false;
        if (!savingDefaultLayout)
            result = super.getSaveDisplays();
//        System.err.println("getSaveDisplays="+result+" savingDefaultLayout="+savingDefaultLayout);
        return result;
    }

    @Override public boolean getSaveViewState() {
        boolean result = true;
        if (!savingDefaultLayout)
            result = super.getSaveViewState();
//        System.err.println("getSaveViewState="+result+" savingDefaultLayout="+savingDefaultLayout);
        return result;
    }

    @Override public boolean getSaveJython() {
        boolean result = false;
        if (!savingDefaultLayout)
            result = super.getSaveJython();
//        System.err.println("getSaveJython="+result+" savingDefaultLayout="+savingDefaultLayout);
        return result;
    }

    public void doSaveAsDefaultLayout() {
        String layoutFile = getResourceManager().getResources(IdvResourceManager.RSC_BUNDLES).getWritable();
        // do prop check here?
        File f = new File(layoutFile);
        if (f.exists()) {
            boolean result = GuiUtils.showYesNoDialog(null, "Saving a new default layout will overwrite your existing default layout. Do you wish to continue?", "Overwrite Confirmation");
            if (!result)
                return;
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

    @Override protected boolean addToBundle(Hashtable data, List dataSources,
            List displayControls, List viewManagers,
            String jython) 
    {
        // add in some extra versioning information
        StateManager stateManager = (StateManager)getIdv().getStateManager();
        if (data != null)
            data.put("mcvversion", stateManager.getVersionInfo());

        // remove ReadoutProbes from the list
        if (displayControls != null) {
            List<DisplayControl> newControls = new ArrayList<DisplayControl>();
            for (DisplayControl dc : (List<DisplayControl>)displayControls) {
                if (!(dc instanceof ReadoutProbe))
                    newControls.add(dc);
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

        if (sitePath != null)
            dirs.add(IOUtil.joinDir(sitePath, IdvObjectStore.DIR_BUNDLES));

        for (String top : dirs) {
            List<File> subdirs = 
                IOUtil.getDirectories(Collections.singletonList(top), true);
            for (File subdir : subdirs)
                loadBundlesInDirectory(allBundles, 
                    fileToCategories(top, subdir.getPath()), subdir);
        }
        return allBundles;
    }

    private void loadBundlesInDirectory(final List<SavedBundle> allBundles, 
        final List<String> categories, final File file) 
    {
        String[] localBundles = file.list();

        for (int i = 0; i < localBundles.length; i++) {
            String filename = IOUtil.joinDir(file.toString(), localBundles[i]);
            if (ArgumentManager.isBundle(filename))
                allBundles.add(new SavedBundle(filename,
                    IOUtil.stripExtension(localBundles[i]), categories, true));
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

        String name = ((label != null) ? label : IOUtil.getFileTail(xmlFile));

        boolean shouldMerge = getStore().get(PREF_OPEN_MERGE, true);

        boolean removeAll   = false;

        boolean limitNewWindows = false;

        boolean mergeLayers = false;
        setMergeBundledLayers(false);

        if (checkToRemove) {
            // ok[0] = did the user press cancel 
            boolean[] ok = getPreferenceManager().getDoRemoveBeforeOpening(name);

            if (!ok[0])
                return false;

            if (!ok[1] && !ok[2]) {
                removeAll = false;
                shouldMerge = false;
                mergeLayers = false;
            }
            if (!ok[1] && ok[2]) {
                removeAll = false;
                shouldMerge = true;
                mergeLayers = false;
            }
            if (ok[1] && !ok[2]) {
                removeAll = false;
                shouldMerge = false;
                mergeLayers = true;
            }
            if (ok[1] && ok[2]) {
                removeAll = true;
                shouldMerge = true;
                mergeLayers = false;
            }

//          System.err.println("ok[1]= "+ok[1]+" ok[2]="+ok[2]);
//          System.err.println("removeAll="+removeAll+" shouldMerge="+shouldMerge+" mergeLayers="+mergeLayers);

            setMergeBundledLayers(mergeLayers);

            if (removeAll) {
                // Remove the displays first because, if we remove the data 
                // some state can get cleared that might be accessed from a 
                // timeChanged on the unremoved displays
                getIdv().removeAllDisplays();
                // Then remove the data
                getIdv().removeAllDataSources();
            }

            if (ok.length == 4)
                limitNewWindows = ok[3];
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
            //            System.err.println ("file "  + xmlFile);
            if (ArgumentManager.isZippedBundle(xmlFile)) {
                //                System.err.println (" is zidv");
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
                    if ( !GuiUtils.showOkCancelDialog(null, "Zip file data",
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
            if (showDialog)
                loadDialog.showDialog();
        }

        if (xmlFile != null) {
            getStateManager().putProperty(PROP_BUNDLEPATH,
                                          IOUtil.getFileRoot(xmlFile));
        }

        getStateManager().putProperty(PROP_LOADINGXML, true);
        try {
            xml = applyPropertiesToBundle(xml);
            if (xml == null)
                return;

            Trace.call1("Decode.toObject");
            Object data = getIdv().getEncoderForRead().toObject(xml);
            Trace.call2("Decode.toObject");

            if (data != null) {
                Hashtable properties = new Hashtable();
                if (data instanceof Hashtable) {
                    Hashtable ht = (Hashtable) data;

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
            if (xmlFile != null)
                logException("Error loading bundle: " + xmlFile, exc);
            else
                logException("Error loading bundle", exc);

            inError = true;
        }

        if (!fromCollab)
            showNormalCursor();

        getStateManager().putProperty(PROP_BUNDLEPATH, "");
        getStateManager().putProperty(PROP_ZIDVPATH, "");
        getStateManager().putProperty(PROP_LOADINGXML, false);

        if (!inError && getIdv().getInteractiveMode() && xmlFile != null)
            getIdv().addToHistoryList(xmlFile);

        loadDialog.dispose();
        if (loadDialog.getShouldRemoveItems()) {
            List displayControls = loadDialog.getDisplayControls();
            for (int i = 0; i < displayControls.size(); i++) {
                try {
                    ((DisplayControl) displayControls.get(i)).doRemove();
                } catch (Exception exc) {
                    // Ignore the exception
                }
            }
            List dataSources = loadDialog.getDataSources();
            for (int i = 0; i < dataSources.size(); i++) {
                getIdv().removeDataSource((DataSource) dataSources.get(i));
            }
        }

        loadDialog.clear();
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
                if (!(comp instanceof IdvComponentGroup))
                    continue;

                IdvComponentGroup group = (IdvComponentGroup)comp;
                List<IdvComponentHolder> holders =
                    group.getDisplayComponents();

                for (IdvComponentHolder holder : holders) {
                    if (holder.getViewManagers() != null) {
//                        System.err.println("extracted: " + holder.getViewManagers().size());
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
            for (IdvComponentHolder holder : holders)
                group.addComponent(holder);

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
                if (!(comp instanceof IdvComponentGroup))
                    continue;

                IdvComponentGroup group = (IdvComponentGroup)comp;
                holders.addAll(CompGroups.getComponentHolders(group));
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
                if (!(comp instanceof IdvComponentGroup))
                    continue;

                List<IdvComponentHolder> holders = 
                    new ArrayList<IdvComponentHolder>(
                            ((IdvComponentGroup)comp).getDisplayComponents());

                for (IdvComponentHolder holder : holders) {
                    if (!CompGroups.isDynamicSkin(holder))
                        continue;
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
        if (zidvPath.length() == 0)
            return;

        for (DataSourceImpl d : ds) {
            boolean isBulk = isBulkDataSource(d);
            if (!isBulk)
                continue;

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
            if (value instanceof String)
                return Boolean.valueOf((String)value);
            else if (value instanceof Boolean)
                return (Boolean)value;
        }

        DataSourceDescriptor desc = d.getDescriptor();
        boolean localFiles = desc.getFileSelection();

        List filePaths = d.getDataPaths();
        List tempPaths = d.getTmpPaths();
        if (filePaths == null || filePaths.isEmpty())
            return false;

        if (tempPaths == null || tempPaths.isEmpty())
            return false;

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
        if (dataSources != null)
            fixBulkDataSources(dataSources);

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
        if (CompGroups.hasDynamicSkins(windows))
            mapDynamicSkins(windows);

        List<WindowInfo> newWindows;
        if (limitNewWindows && windows.size() > 1)
            newWindows = injectComponentGroups(windows);
        else
            newWindows = betterInject(windows);

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
        List<WindowInfo> windows = new ArrayList<WindowInfo>();
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
        List<ViewManager> vms = new ArrayList<ViewManager>();
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
            Hashtable<String, Object> persist = window.getPersistentComponents();
            Set<Map.Entry<String, Object>> blah = persist.entrySet();
            for (Map.Entry<String, Object> entry : blah) {
                if (!(entry.getValue() instanceof IdvComponentGroup))
                    continue;

                IdvComponentGroup group = (IdvComponentGroup)entry.getValue();
                if (CompGroups.hasNestedGroups(group))
                    entry.setValue(flattenGroup(group));
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
            CompGroups.getComponentHolders(nested);

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

        for (IdvComponentHolder holder : newHolders)
            if (CompGroups.isUIHolder(holder))
                newHolders.remove(holder);

        return newHolders;
    }

    /**
     * <p>Ensures that the lists corresponding to the ids in <code>ids</code>
     * actually exist in <code>table</code>, even if they are empty.</p>
     */
    // TODO: not a fan of this method.
    protected static void populateEssentialLists(final String[] ids, final Hashtable<String, Object> table) {
        for (String id : ids)
            if (table.get(id) == null)
                table.put(id, new ArrayList<Object>());
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
                if (holders == null || holders.isEmpty())
                    continue;

                List<IdvComponentHolder> newHolders = 
                    new ArrayList<IdvComponentHolder>();

                // filter out any holders that don't contain view managers
                for (IdvComponentHolder holder : holders)
                    if (!CompGroups.isUIHolder(holder))
                        newHolders.add(holder);

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

            if (vm instanceof MapViewManager)
                if (((MapViewManager)vm).getUseGlobeDisplay())
                    props.append(DYNSKIN_PROPS_GLOBE);

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
    
    private static final String DYNSKIN_TMPNAME = "Dynamic Skin Test";
    private static final String DYNSKIN_TAG_PANEL = "panel";
    private static final String DYNSKIN_TAG_VIEW = "idv.view";
    private static final String DYNSKIN_ATTR_ID = "id";
    private static final String DYNSKIN_ATTR_COLS = "cols";
    private static final String DYNSKIN_ATTR_PROPS = "properties";
    private static final String DYNSKIN_ATTR_CLASS = "class";
    private static final String DYNSKIN_ATTR_VIEWID = "viewid";
    private static final String DYNSKIN_PROPS_GLOBE = "useGlobeDisplay=true;initialMapResources=/auxdata/maps/globemaps.xml;";
    private static final String DYNSKIN_PROPS_GENERAL = "clickToFocus=true;showToolBars=true;shareViews=true;showControlLegend=true;initialSplitPaneLocation=0.2;legendOnLeft=false;size=300:400;shareGroup=view%versionuid%;";
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
}
