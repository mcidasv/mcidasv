/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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

package edu.wisc.ssec.mcidasv.chooser.adde;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.w3c.dom.Element;

import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.AddeImageInfo;
import ucar.unidata.data.imagery.BandInfo;
import ucar.unidata.data.imagery.ImageDataSource;
import ucar.unidata.data.imagery.ImageDataset;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.idv.chooser.adde.AddeServer.Group;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.ui.ChooserList;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlNodeList;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;
import ucar.visad.UtcDate;
import visad.DateTime;
import visad.Gridded1DSet;
import visad.VisADException;
import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaDirectoryList;
import edu.wisc.ssec.mcidas.McIDASException;
import edu.wisc.ssec.mcidas.adde.AddeSatBands;
import edu.wisc.ssec.mcidas.adde.AddeURL;
import edu.wisc.ssec.mcidas.adde.DataSetInfo;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.servermanager.EntryTransforms;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.servermanager.EntryStore;
import edu.wisc.ssec.mcidasv.util.CollectionHelpers;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Position;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;


/**
 * Widget to select images from a remote ADDE server
 * Displays a list of the descriptors (names) of the image datasets
 * available for a particular ADDE group on the remote server.
 *
 * @author Don Murray
 */
public class AddeTleChooser extends AddeChooser implements Constants {

    /** My servers */
//    private List tleServers;

    /** text type */
//    private static final String IMAGE_TYPE = "image";
    private static final String TLE_TYPE = "text";

    /**
     * Property for the tle server name key.
     * @see #getServer()
     */
    public static String TLE_SERVER_NAME_KEY = "tle_server";

    /**
     * Property for the tle group name key.
     * @see #getGroup()
     */
    public static String TLE_GROUP_NAME_KEY = "tle_group";

    /**
     * Property for the tle server name key.
     * @see #getServer()
     */
//    public static String SATELLITE_SERVER_NAME_KEY = "satellite_server";

    /**
     * Property for the tle group name key.
     * @see #getGroup()
     */
//    public static String SATELLITE_GROUP_NAME_KEY = "satellite_group";

//    private JComboBox imageServerSelector;
//    protected JComboBox imageGroupSelector;

    /**
     * Construct an Adde image selection widget
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeTleChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        showServers();        
    }
    
	/**
	 * Return the data source ID.  Used by extending classes.
	 */
    @Override
	protected String getDataSourceId() {
		return "ADDE.TLE.V";
	}
    
    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    @Override
    public JComponent doMakeContents() {
        JPanel myPanel = new JPanel();
/*
        imageServerSelector = getImageServerSelector(AddeServer.TYPE_IMAGE);
        JLabel serverLabelInner = new JLabel("Image Server:");
        McVGuiUtils.setLabelPosition(serverLabelInner, Position.RIGHT);
        JPanel serverLabel = GuiUtils.leftRight(parameterButton, serverLabelInner);
        McVGuiUtils.setComponentWidth(serverLabel);
        JLabel groupLabel = McVGuiUtils.makeLabelRight("Dataset:");

        McVGuiUtils.setComponentWidth(imageServerSelector, Width.DOUBLE);
        McVGuiUtils.setComponentWidth(imageGroupSelector, Width.DOUBLE);
        addDescComp(serverLabel);
        addDescComp(imageServerSelector);
        addDescComp(groupLabel);
        addDescComp(imageGroupSelector);
*/
        XmlObjectStore store = getIdv().getStore();

        GroupLayout layout = new GroupLayout(myPanel);
        myPanel.setLayout(layout);

        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(descriptorLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(descriptorComboBox))
/*
                                .add(layout.createSequentialGroup()
                                    .add(serverLabel)
                                    .add(GAP_RELATED)
                                    .add(imageServerSelector)
                                    .add(GAP_RELATED)
                                    .add(groupLabel)
                                    .add(imageGroupSelector)
                                )
*/
                )
            )
        );

        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(descriptorLabel)
                    .addComponent(descriptorComboBox))
/*
			.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(serverLabel)
    			    .add(imageServerSelector)
                            .add(GAP_RELATED)
                            .add(groupLabel)
                            .add(imageGroupSelector)
                        )
*/
            )
        );

        setInnerPanel(myPanel);
        return super.doMakeContents();
    }

    /**
     * Update labels, enable widgets, etc.
     */
    protected void updateStatus() {
        super.updateStatus();
        enableWidgets();
    }

    /**
     * Get the state
     *
     * @return The state
     */
/*
    protected int getState() {
        int ret = STATE_UNCONNECTED;
        if (haveDescriptorSelected()) {
            ret = STATE_CONNECTED;
        }
        return ret;
    }
*/

    /* Get the server selector
     * @return The server selector
     */
/*
    private JComboBox getImageServerSelector(String type) {
        this.tleServers = getManagedServers(type);
        final JComboBox newServerSelector = new JComboBox(new Vector(tleServers));
        imageGroupSelector = getImageGroupSelector(newServerSelector, type);
        final String groupType = type;
        newServerSelector.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ( !ignoreStateChangedEvents) {
                    setImageGroups(newServerSelector, imageGroupSelector, groupType);
                }
            }
        });
        return newServerSelector;
    }

    private List<AddeServer> getManagedServers(final String type) {
        EntryStore entryStore = ((McIDASV)getIdv()).getServerManager();
        return arrList(entryStore.getIdvStyleEntries(type));
    }
*/

    /**
     * Get the group selector
     * @return The group selector
     */
/*
    private JComboBox getImageGroupSelector(JComboBox serverSel, String type) {
        JComboBox newGroupSelector = new JComboBox();
        setImageGroups(serverSel, newGroupSelector, type);
        return newGroupSelector;
    }
*/

    /**
     * Get the selected AddeServer
     *
     * @return the server or null
     */
/*
    private AddeServer getImageServer(JComboBox serverSel) {
        Object selected = serverSel.getSelectedItem();
        if ((selected != null) && (selected instanceof AddeServer)) {
            return (AddeServer) selected;
        }
        return null;
    }
*/

    /**
     * Get the selected group
     *
     * @return the group name
     */
/*
    private AddeServer.Group getImageGroup(JComboBox groupSel) {
        Object selected = groupSel.getSelectedItem();
        if ((selected != null) && (selected instanceof AddeServer.Group)) {
            return (AddeServer.Group) selected;
        }
        return null;
    }
*/

    /**
     * Set the group list
     */
/*
    protected void setImageGroups(JComboBox serverSel, JComboBox groupSel, String type) {
        EntryStore servManager = ((McIDASV)getIdv()).getServerManager();
        AddeServer server = getImageServer(serverSel);
        if (server != null) {
            List<Group> groups = CollectionHelpers.arrList();
            EntryType selType = EntryTransforms.strToEntryType("image");
            groups.addAll(servManager.getIdvStyleRemoteGroups(server.getName(), selType));
            //List groups   = server.getGroupsWithType(type);
            GuiUtils.setListData(groupSel, groups);
            groupSel.setSelectedItem(0);
        } else {
            GuiUtils.setListData(groupSel, new Vector());
        }
    }
*/

    /**
     * Get the data type ID
     *
     * @return  the data type
     */

    public String getDataType() {
        //return "IMAGE";
        return "TEXT";
    }

    /**
      * get the adde server grup type to use
      *
      * @return group type
      */
    @Override
    protected String getGroupType() {
            //return AddeServer.TYPE_IMAGE;
            return TLE_TYPE;
    }

    /**
     * User said go, we go. 
     * Create the ADDE.TLE.V DataSource
     * 
     */
    public void doLoadInThread() {
        String obj = "TLE";
        Hashtable ht = new Hashtable();
        getDataSourceProperties(ht);
        makeDataSource(obj, getDataSourceId(), ht);
        saveServerState();
    }

    /**
     * Get the DataSource properties
     * 
     * @param ht
     *            Hashtable of properties
     */
    protected void getDataSourceProperties(Hashtable ht) {
        super.getDataSourceProperties(ht);
        ht.put(DATASET_NAME_KEY, getDatasetName());
        ht.put(TLE_SERVER_NAME_KEY, getServer());
        ht.put(TLE_GROUP_NAME_KEY, getGroup());
//        ht.put(SATELLITE_SERVER_NAME_KEY, getImageServer(imageServerSelector));
//        ht.put(SATELLITE_GROUP_NAME_KEY, getImageGroup(imageGroupSelector));
    }

    private String getDatasetName() {
        return "TLE";
    }
}
