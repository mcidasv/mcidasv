/*
 * $Id$
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.wisc.ssec.mcidasv.chooser.adde;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.w3c.dom.Element;

import ucar.unidata.data.imagery.BandInfo;
import ucar.unidata.data.imagery.ImageDataSource;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.TwoFacedObject;

import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaDirectoryList;
import edu.wisc.ssec.mcidas.AreaFileException;
import edu.wisc.ssec.mcidas.McIDASUtil;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;

/**
 * Widget to select NEXRAD radar images from a remote ADDE server
 * Displays a list of the descriptors (names) of the radar datasets
 * available for a particular ADDE group on the remote server.
 *
 * @author Don Murray
 */
public class AddeRadarChooser extends AddeImageChooser {

    /** Use to list the stations */
    protected static final String VALUE_LIST = "list";

    /** This is the list of properties that are used in the advanced gui */
    private static final String[] RADAR_PROPS = { PROP_UNIT };

    /** This is the list of labels used for the advanced gui */
    private static final String[] RADAR_LABELS = { "Data Type:" };

    /** Am I currently reading the stations */
    private boolean readingStations = false;

    private Object readStationTask;

    /** station table */
    private List nexradStations;



    /**
     * Construct an Adde image selection widget displaying information
     * for the specified dataset located on the specified server.
     *
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeRadarChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        this.nexradStations =
            getIdv().getResourceManager().findLocationsByType("radar");
    }

    /**
     * get the adde server grup type to use
     *
     * @return group type
     */
    protected String getGroupType() {
        return AddeServer.TYPE_RADAR;
    }

    /**
     * Should we show the advanced properties component in a separate panel
     *
     * @return false
     */
    public boolean showAdvancedInTab() {
        return false;
    }

    /**
     * Overwrite base class method to return the correct name
     * (used for labeling, etc.)
     *
     * @return  data name specific to this selector
     */
    public String getDataName() {
        return "Radar Data";
    }

    @Override public String getDataType() {
        return "RADAR";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescriptorLabel() {
        return "Product";
    }

    /**
     * Get the size of the image list
     *
     * @return the image list size
     */
    protected int getImageListSize() {
        return 6;
    }
    
    /**
     * Get a description of the currently selected dataset
     *
     * @return the data set description.
     */
    public String getDatasetName() {
        return getSelectedStation() + " (" + super.getDatasetName() + ")";
    }

    /**
     * Method to call if the server changed.
     */
    protected void connectToServer() {
        clearStations();
        super.connectToServer();
        setAvailableStations();
    }

    /**
     * Check if we are ready to read times
     *
     * @return  true if times can be read
     */
    protected boolean canReadTimes() {
        return super.canReadTimes() && (getSelectedStation() != null);
    }

    /**
     * Get the advanced property names
     *
     * @return array of advanced properties
     */
    protected String[] getAdvancedProps() {
        return RADAR_PROPS;
    }

    /**
     * Get the labels for the advanced properties
     *
     * @return array of labels
     */
    protected String[] getAdvancedLabels() {
        return RADAR_LABELS;
    }

    /**
     * Update labels, etc.
     */
    protected void updateStatus() {
        super.updateStatus();
        if (getState() != STATE_CONNECTED) {
            clearStations();
        }
        if (readStationTask!=null) {
            if(taskOk(readStationTask)) {
                setStatus("Reading available stations from server");
            } else {
                readStationTask  = null;
                setState(STATE_UNCONNECTED);
            }
        }
    }

    /**
     * A new station was selected. Update the gui.
     *
     * @param stations List of selected stations
     */
    protected void newSelectedStations(List stations) {
        super.newSelectedStations(stations);
        descriptorChanged();
    }

    /**
     *  Generate a list of radar ids for the id list.
     */
    private void setAvailableStations() {
        readStationTask = startTask();
        clearSelectedStations();
        updateStatus();
        List stations = readStations();
        if(stopTaskAndIsOk(readStationTask)) {
            readStationTask = null;
            if (stations != null) {
                getStationMap().setStations(stations);
            } else {
                clearStations();
            }
            updateStatus();
            revalidate();
        } else {
            //User pressed cancel
            setState(STATE_UNCONNECTED);
            return;
        }
    }

    /**
     * Generate a list of radar ids for the id list.
     *
     * @return  list of station IDs
     */
    private List readStations() {
        ArrayList stations = new ArrayList();
        try {
            if ((descriptorNames == null) || (descriptorNames.length == 0)) {
                return stations;
            }
            StringBuffer buff        = getGroupUrl(REQ_IMAGEDIR, getGroup());
            String       descrForIds = descriptorNames[0];
            // try to use base reflectivity if it's available.
            for (int i = 0; i < descriptorNames.length; i++) {
                if ((descriptorNames[i] != null)
                        && descriptorNames[i].toLowerCase().startsWith(
                            "base")) {
                    descrForIds = descriptorNames[i];
                    break;
                }
            }
            appendKeyValue(buff, PROP_DESCR,
                           getDescriptorFromSelection(descrForIds));
            appendKeyValue(buff, PROP_ID, VALUE_LIST);
            Hashtable         seen    = new Hashtable();
            AreaDirectoryList dirList =
                new AreaDirectoryList(buff.toString());
            for (Iterator it = dirList.getDirs().iterator(); it.hasNext(); ) {
                AreaDirectory ad = (AreaDirectory) it.next();
                String stationId =
                    McIDASUtil.intBitsToString(ad.getValue(20)).trim();
                //Check for uniqueness
                if (seen.get(stationId) != null) {
                    continue;
                }
                seen.put(stationId, stationId);
                //System.err.println ("id:" + stationId);
                Object station = findStation(stationId);
                if (station != null) {
                    stations.add(station);
                }
            }
        } catch (AreaFileException e) {
            String msg = e.getMessage();
            if (msg.toLowerCase().indexOf(
                    "no images meet the selection criteria") >= 0) {
                LogUtil.userErrorMessage(
                    "No stations could be found on the server");
            } else {
                handleConnectionError(e);
            }
            stations = new ArrayList();
            setState(STATE_UNCONNECTED);
        }
        return stations;
    }

    /**
     * Find the station for the given ID
     *
     * @param stationId  the station ID
     *
     * @return  the station or null if not found
     */
    private Object findStation(String stationId) {
        for (int i = 0; i < nexradStations.size(); i++) {
            NamedStationTable table =
                (NamedStationTable) nexradStations.get(i);
            Object station = table.get(stationId);
            if (station != null) {
                return station;
            }
        }
        return null;
    }

    public void doCancel() {
        readStationTask = null;
        super.doCancel();
    }

    /**
     * Create the appropriate request string for the image.
     *
     * @param ad  <code>AreaDirectory</code> for the image in question.
     * @param doTimes  true if this is for absolute times, false for relative
     * @param cnt  image count (position in dataset)
     *
     * @return  the ADDE request URL
     */
    protected String makeRequestString(AreaDirectory ad, boolean doTimes,
                                       int cnt) {

        StringBuffer buf = getGroupUrl(REQ_IMAGEDATA, getGroup());
        buf.append(makeDateTimeString(ad, cnt, doTimes));
        String[] props = { PROP_DESCR, PROP_ID, PROP_UNIT, PROP_SPAC, PROP_MAG, 
                           PROP_SIZE, PROP_USER, PROP_PROJ };
        buf.append(makeProps(props, ad));
        return buf.toString();
    }

    /**
     * Get the list of properties for the base URL
     * @return list of properties
     */
    protected String[] getBaseUrlProps() {
        return new String[] { PROP_DESCR, PROP_ID, PROP_UNIT, PROP_SPAC,
                              PROP_BAND, PROP_USER, PROP_PROJ, };
    }

    /**
     * Overwrite the base class method to return the default property value
     * for PROP_ID.
     *
     * @param prop The property
     * @param ad The area directory
     * @param forDisplay Is this to show the end user in the gui.
     *
     * @return The value of the property
     */
    protected String getDefaultPropValue(String prop, AreaDirectory ad,
                                         boolean forDisplay) {
        if (prop.equals(PROP_ID)) {
            return getSelectedStation();
        }
        return super.getDefaultPropValue(prop, ad, forDisplay);
    }

    /**
     * Get a description of the properties
     *
     * @return  a description
     */
    protected String getPropertiesDescription() {
        StringBuffer buf = new StringBuffer();
        if (unitComboBox != null) {
            buf.append(getAdvancedLabels()[0]);
            buf.append(" ");
            buf.append(unitComboBox.getSelectedItem().toString());
        }
        return buf.toString();
    }

    /**
     * get properties
     *
     * @param ht properties
     */
    protected void getDataSourceProperties(Hashtable ht) {
    	unitComboBox.setSelectedItem(ALLUNITS);
        super.getDataSourceProperties(ht);
        ht.put(ImageDataSource.PROP_IMAGETYPE, ImageDataSource.TYPE_RADAR);
    }
        
    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    public JComponent doMakeContents() {      
    	JPanel myPanel = new JPanel();
    	        
        JLabel stationLabel = McVGuiUtils.makeLabelRight("Station:");
        addServerComp(stationLabel);

        JComponent stationPanel = getStationMap();
        registerStatusComp("stations", stationPanel);
        addServerComp(stationPanel);
        
        JLabel timesLabel = McVGuiUtils.makeLabelRight("Times:");
        addServerComp(timesLabel);
        
        JPanel timesPanel = makeTimesPanel(false);
        timesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        addServerComp(timesPanel);
    	
        // We need to create this but never show it... AddeImageChooser requires it to be instantiated
        unitComboBox = new JComboBox();
        
        enableWidgets();

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(descriptorLabel)
                        .add(GAP_RELATED)
                        .add(descriptorComboBox))
                    .add(layout.createSequentialGroup()
                        .add(stationLabel)
                        .add(GAP_RELATED)
                        .add(stationPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .add(timesLabel)
                        .add(GAP_RELATED)
                        .add(timesPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(descriptorLabel)
                    .add(descriptorComboBox))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(stationLabel)
                    .add(stationPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(timesLabel)
                    .add(timesPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        
        setInnerPanel(myPanel);
        return super.doMakeContents(true);
    }
    
}