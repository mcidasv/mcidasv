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


import ucar.unidata.idv.*;

import ucar.unidata.ui.ChooserList;
import ucar.unidata.ui.ChooserPanel;

import java.awt.*;
import java.awt.event.*;

import java.util.Vector;

import java.beans.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;

/**
 *
 * @author Unidata IDV Development Team
 * @version $Revision$
 */
public abstract class FrameChooser extends ChooserPanel {

    /** Property for new data selection */
    public static String NEW_SELECTION = "FrameChooser.NEW_SELECTION";

    /** Have connected */
    protected static final int STATE_CONNECTED = 2;

    /** flag for ignoring combobox changes */
    protected boolean ignoreStateChangedEvents = false;

    /**
     * Public keys for frame numbers, request, and data name.
     */
    public final static String FRAME_NUMBERS_KEY = "frame numbers";
    public final static String DATA_NAME_KEY = "data name";
    public final static String REQUEST_HOST = "host";
    public final static String REQUEST_PORT = "port";
    public final static String REQUEST_KEY = "key";

    /** Used to synchronize access to widgets (eg: disabling, setting state, etc). */
    protected Object WIDGET_MUTEX = new Object();

    /** frames list */
    private ChooserList framesList;

    /** Keep track of when are are doing a frame loop */
    private boolean doLoop = false;

    /** Frame loop radio button */
    private JRadioButton loopRB;

    /** Refresh current frame radio button */
    private JRadioButton curRB;

    /**
     * Create me.
     */
    public FrameChooser() {}

    /**
     * Handle when the user presses the update button
     *
     * @throws Exception _more_
     */
    public void handleUpdate() throws Exception {}

    /**
     * Handle when the user presses the update button
     */
    public void handleUpdateFromThread() {
        showWaitCursor();
        try {
            handleUpdate();
        } catch (Exception exc) {
        }
        showNormalCursor();
    }

    /**
     * Update the selector. Call handleUpdate in a thread
     */
    public final void doUpdate() {
        Misc.run(this, "handleUpdateFromThread");
    }

    /**
     * Handle the event
     *
     * @param ae The event
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        super.actionPerformed(ae);
    }

    /**
     * Disable/enable any components that depend on the server.
     * Try to update the status labelwith what we know here.
     */
    protected void updateStatus() {
       setHaveData(getGoodToGo());
    }

    /**
     * Are there any times in the times list.
     *
     * @return Do we have any times at all.
     */
    protected boolean haveAnyTimes() {
        return framesList.getModel().getSize() > 0;
    }

    /**
     * Are there more than one times in the times list.
     *
     * @return Do we have a series.
     */
    protected boolean haveASeries() {
        return !getTimesList().getSelectedValuesList().isEmpty();
    }

    /**
     * Create (if needed) and return the list that shows frames.
     *
     * @return The frames list.
     */
    public ChooserList getTimesList() {
        if (framesList == null) {
            framesList = new ChooserList();
            framesList.setVisibleRowCount(getTimesListSize());
            framesList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    updateStatus();
                }
            });
        }
        return framesList;
    }

    /**
     * Get the size of the times list
     *
     * @return the times list size
     */
    protected int getTimesListSize() {
        return 6;
    }

    /**
     * Clear all times in the times list.
     */
    protected void clearFramesList() {
        getTimesList().setListData(new Vector());
    }

    /**
     *  Do what needs to be done to read in the times.  Subclasses
     *  need to implement this.
     */
    protected abstract void readFrames();

    /**
     * Are we all set to load data.
     *
     * @return All set to load.
     */
    protected boolean getGoodToGo() {
        if ( !haveFrameSelected()) {
            return false;
        }
        return true;
    }

    /**
     * Create the current frame / frame loop selector
     *
     * @return  the image list panel
     */
    protected JPanel makeFramesPanel() {

        getTimesList().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if ( !getDoFrameLoop()) {
                    return;
                }
            }
        });

        ChangeListener listener = new ChangeListener() {
            public void stateChanged(ChangeEvent ae) {
                if (loopRB.isSelected() == getDoFrameLoop()) {
                    return;
                }
                doLoop = loopRB.isSelected();
                if (doLoop && !haveAnyTimes()) {
                    readFrames();
                } else {
                    updateStatus();
                }
                enableWidgets();
            }
        };

        loopRB = new JRadioButton("Select frames", getDoFrameLoop());
        loopRB.addChangeListener(listener);
        curRB = new JRadioButton("Refresh current frame", !getDoFrameLoop());
        curRB.addChangeListener(listener);
        GuiUtils.buttonGroup(loopRB, curRB);
        JPanel panel = GuiUtils.doLayout(new Component[] {
            curRB, loopRB, 
            new JLabel(" "),getTimesList().getScroller() 
        }, 2, GuiUtils.WT_N, GuiUtils.WT_NY);
        return GuiUtils.wrap(panel);
    }

    /**
     * Are there any frames selected.
     *
     * @return Any frames selected.
     */
    protected boolean haveFrameSelected() {
        return !getDoFrameLoop() || getTimesList().haveDataSelected();
    }

    /**
     * Do we do a frame loop or refresh current frame
     *
     * @return Do we do frame loop
     */
    protected boolean getDoFrameLoop() {
        return doLoop;
    }

    /**
     * Set whether we do a frame loop or refresh current frame
     *
     * @param yesorno true to do frame loop
     */
    protected void setDoFrameLoop(boolean yesorno) {
        doLoop = yesorno;
        // Should this be in 
        if (curRB != null) {
            curRB.setSelected(yesorno);
        }
    }

    /**
     * Did the user select current frame?
     *
     * @return Should we load current frame
     */
    protected boolean getDoCurrentFrame() {
        return !getDoFrameLoop();
    }

    /**
     * Enable or disable the GUI widgets based on what has been
     * selected.
     */
    protected void enableWidgets() {
        getTimesList().setEnabled(getDoFrameLoop());
    }
}
