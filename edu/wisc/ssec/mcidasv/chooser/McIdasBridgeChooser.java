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

package edu.wisc.ssec.mcidasv.chooser;


import edu.wisc.ssec.mcidasv.*;

import edu.wisc.ssec.mcidasv.data.McDataset;
import edu.wisc.ssec.mcidasv.data.McIdasXInfo;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.util.List;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;

import org.w3c.dom.Element;

import ucar.unidata.idv.*;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.util.PreferenceList;

import ucar.unidata.xml.XmlResourceCollection;


public class McIdasBridgeChooser extends IdvChooser {

    private McIdasXChooser mcIdasXChooser;

    /**
     * Create the chooser with the given manager and xml
     *
     * @param mgr The manager
     * @param root The xml
     *
     */
    public McIdasBridgeChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    /**
     * Handle the update event. Just pass it through to the mcIdasXChooser
     */
    public void doUpdate() {
        mcIdasXChooser.doUpdate();
    }

    /**
     * Make the GUI
     *
     * @return The GUI
     */
    protected JComponent doMakeContents() {
        mcIdasXChooser = doMakeMcIdasXChooser();
        initChooserPanel(mcIdasXChooser);
        mcIdasXChooser.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals(
                        McIdasXChooser.NEW_SELECTION)) {
                    loadMcIdasDataSet(e);
                }

            }
        });
        return mcIdasXChooser.getContents();
    }

    /**
     * Make the chooser. This is a hook so a derived class
     * can make its own chooser
     *
     * @return The {@link edu.wisc.ssec.mcidasv.chooser.McIdasXChooser} to pass
     * to the mcIdasXChooser.
     */
    protected McIdasXChooser doMakeMcIdasXChooser() {
        return new McIdasXChooser(this, getPreferenceList(McIdasChooser.PREF_FRAMEDESCLIST)) {
            public void doCancel() {
                closeChooser();
            }
        };
    }


    /**
     * Get the xml resource collection that defines the frame default xml
     *
     * @return Frame defaults resources
     */
    protected XmlResourceCollection getFrameDefaults() {
        return getIdv().getResourceManager().getXmlResources(
            McIDASV.RSC_FRAMEDEFAULTS);
    }

    /**
     * User said go, we go. Simply get the list of frames
     * from the McIdasXChooser and create the McIdas-X
     * DataSource
     *
     * @param e The event
     */
    protected void loadMcIdasDataSet(PropertyChangeEvent e) {
        //System.out.println("edu/wisc/ssec/mcidasv/chooser/McIdasBridgeChooser: loadMcIdasDataSet");
        McIdasXChooser mxc = (McIdasXChooser) e.getSource();
        McDataset mds = new McDataset(mxc.getDatasetName(), (List) e.getNewValue());
        // make properties Hashtable 
        Hashtable ht = new Hashtable();
        ht.put(mxc.FRAME_NUMBERS_KEY, mds.frameNumbers);
        //System.out.println("frameNumbers=" + mds.frameNumbers);
        if (mds.frameNumbers.size() > 1) {
           //System.out.println("  Frame Sequence");
           ht.put(mxc.DATA_NAME_KEY,"Frame Sequence");
        } else {
           //System.out.println("  Frame");
           ht.put(mxc.DATA_NAME_KEY,"Frame");
        }
        ht.put(mxc.REQUEST_HOST, mxc.getHost());
        ht.put(mxc.REQUEST_PORT, mxc.getPort());
        ht.put(mxc.REQUEST_KEY, mxc.getKey());
        //System.out.println("    ht:  " + ht);
        makeDataSource("", "MCIDASX", ht);
    }
}
