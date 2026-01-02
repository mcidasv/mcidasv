/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.mcidasv.chooser;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.pathwatcher.DirectoryWatchService;
import edu.wisc.ssec.mcidasv.util.pathwatcher.OnFileChangeListener;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventTopicSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.idv.chooser.IdvChooser;

import static edu.wisc.ssec.mcidasv.McIDASV.getStaticMcv;
import static ucar.unidata.idv.chooser.IdvChooser.PREF_DEFAULTDIR;

/**
 * An extension of JFileChooser to handle Two-Line Element (TLE)
 * files, for plotting satellite orbit tracks.
 * 
 * @author Gail Dengel and Tommy Jasmin
 *
 */
public class TLEFileChooser extends JFileChooser implements AncestorListener, PropertyChangeListener {
    
    private static final String ID = "tlefilechooser";
    
    /**
     * auto-generated default value
     */
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(TLEFileChooser.class);

    /* the enclosing orbit track chooser */
    private PolarOrbitTrackChooser potc = null;

    /** This is mostly used to preemptively null-out the listener. */
    protected OnFileChangeListener watchListener;
    
    /** 
     * Value is controlled via {@link #ancestorAdded(AncestorEvent)} and
     * {@link #ancestorRemoved(AncestorEvent)}
     */
    private boolean trulyVisible;
    
    /**
     * Create the file chooser
     *
     * @param chooser {@code PolarOrbitTrackChooser} to which this {@code TLEFileChooser} belongs.
     * @param directory Initial directory.
     * @param filename Initial filename within {@code directory}.
     */
    public TLEFileChooser(PolarOrbitTrackChooser chooser, String directory, String filename) {
        super(directory);
        AnnotationProcessor.process(this);
        potc = chooser;

        logger.debug("TLEFileChooser constructor...");
        setControlButtonsAreShown(false);
        setMultiSelectionEnabled(false);
        FileFilter filter = new FileNameExtensionFilter("TLE files", "txt");
        addChoosableFileFilter(filter);
        setAcceptAllFileFilterUsed(false);
        setFileFilter(filter);
        addPropertyChangeListener(this);
        addAncestorListener(this);
        
        File tmpFile = new File(directory + File.separatorChar + filename);
//        logger.trace("tmpFile='{}' exists='{}'", tmpFile, tmpFile.exists());
        setSelectedFile(null);
        setSelectedFile(tmpFile);
//        final JList list = McVGuiUtils.getDescendantOfType(JList.class, this, "Enabled", true);
//        list.requestFocus();
    }

    @Override public void setSelectedFile(File file) {
        // i REALLY don't know how to explain this one...but don't remove the
        // following if-else stuff. at least on OSX, it has *something* to do with
        // whether or not the UI actually shows the file selection.
        // what is somewhat weird is that commenting out the current if-else
        // and doing something like:
        // if (file != null) {
        //     boolean weird = file.exists();
        // }
        // does *NOT* work--but maybe HotSpot is optimizing away the unused code, right?
        // wrong! the following also does not work:
        // if (file != null && file.exists()) {
        //    logger.trace("exists!");
        // }
        // i will note that calls to this method appear to be happening on threads
        // other than the EDT...but using SwingUtilities.invokeLater and
        // SwingUtilities.invokeAndWait have not worked so far (and I've tried
        // the obvious places in the code, including POTC.doMakeContents()).
        if (file != null) {
            logger.trace("setting file='{}' exists={}", file, file.exists());
        } else {
            logger.trace("setting file='{}' exists=NULL", file);
        }
        super.setSelectedFile(file);
    }

    /**
     * Approve the selection
     */
    @Override public void approveSelection() {
        super.approveSelection();
        potc.doLoad();
    }

    public void setPotc(PolarOrbitTrackChooser potc) {
        this.potc = potc;
    }

    public PolarOrbitTrackChooser getPotc() {
        return potc;
    }

    @Override public void propertyChange(PropertyChangeEvent pce) {
        String propName = pce.getPropertyName();
        if (propName.equals(SELECTED_FILE_CHANGED_PROPERTY)) {
            // tell the chooser we have a file to load
            handleFileChanged();
        } else if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(propName)) {
            String newPath = pce.getNewValue().toString();
            handleChangeWatchService(newPath);
        }
    }

    protected void handleFileChanged() {
        if (potc != null) {
            File f = getSelectedFile();
            if ((f != null) && accept(f) && potc.localMode()) {
                if (!f.isDirectory()) {
                    // update last visited directory here
                    String potcId = PREF_DEFAULTDIR + potc.getId();
                    String potcFileId = PREF_DEFAULTDIR + potc.getId() + ".file";
                    String dir = getSelectedFile().getParent();
                    String file = getSelectedFile().getName();
                    potc.getIdv().getStateManager().writePreference(
                        potcId, dir
                    );
                    potc.getIdv().getStateManager().writePreference(
                        potcFileId, file
                    );
                    potc.enableLoadFromFile(true);
                }
            } else {
                potc.enableLoadFromFile(false);
            }
        } else {
            logger.warn("null potc, must be set by caller before use.");
        }
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
        String id = PREF_DEFAULTDIR + ID;
        potc.getIdv().getStateManager().writePreference(id, newPath);
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
     * <p>If there is a nonexistent path in the preferences file, 
     * {@link FileChooser#findValidParent(String)} will be used.</p>
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
        String prop = PREF_DEFAULTDIR + ID;
        String tempPath = (String)potc.getIdv().getPreference(prop);
        if ((tempPath == null)) {
            tempPath = defaultValue;
        } else if (!Files.exists(Paths.get(tempPath))) {
            tempPath = FileChooser.findValidParent(tempPath);
        }
        return tempPath;
    }
    
    /**
     * Respond to path changes in the {@code JFileChooser}.
     *
     * <p>This method will disable monitoring of the previous path and then
     * enable monitoring of {@code newPath}.</p>
     *
     * @param newPath New path to begin watching.
     */
    public void handleChangeWatchService(final String newPath) {
        DirectoryWatchService watchService = 
            ((McIDASV)potc.getIdv()).getWatchService();
            
        if ((watchService != null) && (watchListener != null)) {
            logger.trace("now watching '{}'", newPath);
            
            setPath(newPath);
            
            handleStopWatchService(Constants.EVENT_FILECHOOSER_STOP,
                                   "changed directory");
            
            handleStartWatchService(Constants.EVENT_FILECHOOSER_START,
                                    "new directory");
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
        McIDASV mcv = (McIDASV)potc.getIdv();
        boolean offscreen = mcv.getArgsManager().getIsOffScreen();
        boolean initDone = mcv.getHaveInitialized();
        String watchPath = getPath();
        if (isTrulyVisible() && !offscreen && initDone) {
            try {
                watchListener = createWatcher();
                mcv.watchDirectory(watchPath, "*", watchListener);
                logger.trace("watching '{}' pattern: '{}' (reason: '{}')", 
                             watchPath, "*", reason);
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
    @EventTopicSubscriber(topic= Constants.EVENT_FILECHOOSER_STOP)
    public void handleStopWatchService(final String topic,
                                       final Object reason)
    {
        logger.trace("stopping service (reason: '{}')", reason);
        
        DirectoryWatchService service = getStaticMcv().getWatchService();
        service.unregister(watchListener);
        
        service = null;
        watchListener = null;
    }
    
    /**
     * Creates a directory monitoring
     * {@link edu.wisc.ssec.mcidasv.util.pathwatcher.Service service}.
     *
     * @return Directory monitor that will respond to changes.
     */
    private OnFileChangeListener createWatcher() {
        watchListener = new OnFileChangeListener() {
            
            /** {@inheritDoc} */
            @Override public void onFileCreate(String filePath) {
                DirectoryWatchService service = 
                    getStaticMcv().getWatchService();
                if (service.isRunning()) {
                    SwingUtilities.invokeLater(() -> rescanCurrentDirectory());
                }
            }
            
            /** {@inheritDoc} */
            @Override public void onFileModify(String filePath) {
                DirectoryWatchService service = 
                    getStaticMcv().getWatchService();
                if (service.isRunning()) {
                    SwingUtilities.invokeLater(() -> rescanCurrentDirectory());
                }
            }
            
            /** {@inheritDoc} */
            @Override public void onFileDelete(String filePath) {
                refreshIfNeeded(filePath);
            }
            
            /** {@inheritDoc} */
            @Override public void onWatchInvalidation(String filePath) {
                refreshIfNeeded(filePath);
            }
        };
        return watchListener;
    }

    /**
     * Used to handle the {@link OnFileChangeListener#onFileDelete(String)} and
     * {@link OnFileChangeListener#onWatchInvalidation(String)} events.
     * 
     * @param filePath Path of interest. Cannot be {@code null}.
     */
    private void refreshIfNeeded(String filePath) {
        DirectoryWatchService service = getStaticMcv().getWatchService();
        if (service.isRunning()) {
            setPath(FileChooser.findValidParent(filePath));
            SwingUtilities.invokeLater(() -> {
                setCurrentDirectory(new File(getPath()));
                rescanCurrentDirectory();
            });
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override public void ancestorAdded(AncestorEvent ancestorEvent) {
        // keep the calls to setTrulyVisible as the first step. that way 
        // isTrulyVisible should work as expected.
        setTrulyVisible(true);
        
        handleStartWatchService(Constants.EVENT_FILECHOOSER_START, 
                                "chooser is visible");
        SwingUtilities.invokeLater(this::rescanCurrentDirectory);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override public void ancestorRemoved(AncestorEvent ancestorEvent) {
        // keep the calls to setTrulyVisible as the first step. that way 
        // isTrulyVisible should work as expected.
        setTrulyVisible(false);
        
        handleStopWatchService(Constants.EVENT_FILECHOOSER_STOP, 
                               "chooser is not visible");
        
    }
    
    /**
     * Not implemented.
     * 
     * @param ancestorEvent Ignored.
     */
    @Override public void ancestorMoved(AncestorEvent ancestorEvent) {}
    
    /**
     * Determine if this file chooser is actually visible to the user.
     * 
     * @return Whether or not this component has been made visible.
     */
    public boolean isTrulyVisible() {
        return trulyVisible;
    }
    
    /**
     * Set whether or not this file chooser is actually visible to the user.
     * 
     * @param value {@code true} means visible.
     */
    private void setTrulyVisible(boolean value) {
        trulyVisible = value;
    }
    
}
