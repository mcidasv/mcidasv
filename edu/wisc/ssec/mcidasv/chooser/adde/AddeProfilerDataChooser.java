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

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import org.w3c.dom.Element;

import ucar.unidata.data.AddeUtil;
import ucar.unidata.data.profiler.AddeProfilerDataSource;
//import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.station.StationLocationMap;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.visad.UtcDate;

import visad.DateTime;

import edu.wisc.ssec.mcidas.McIDASUtil;
import edu.wisc.ssec.mcidas.adde.AddePointDataReader;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;

/**
 * Selection widget for specifing data sources of
 * NOAA National Profiler Network data.
 * For selecting Profiler data source; user selects ADDE server,
 * profiler station(s),
 * and choice of data interval such as hourly or 6 minute.
 *
 * Metadata about the station (lat, lon. elevation)
 * and about the request is made available by "get" methods.
 *
 * @author Unidata IDV Development Team
 * @version $Revision$
 */
public class AddeProfilerDataChooser extends AddePointDataChooser {
	
    /** collection of station tables */
    private XmlResourceCollection stationResources;

    /**
     * ctor
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeProfilerDataChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
                    
        descriptorsAllow = new String[] {
        	"PROFHOURLY", "PROF6MIN"
        };
        
        descriptorsDeny = new String[] { };
        
    }
    
    /**
     * Tell the AddeChooser our name
     *
     * @return  The name
     */
    public String getDataName() {
        return "Profiler Data";
    }

    /**
     * Get the descriptor widget label.
     *
     * @return  label for the descriptor  widget
     */
    public String getDescriptorLabel() { 
        return "Profiler Type"; 
    }
        
    /**
     * Overwrite base class method to create the station map
     * with the appropriate properties.
     *
     * @return The new station map
     */
    protected StationLocationMap createStationMap() {
        return new StationLocationMap(true, (String) null, StationLocationMap.TEMPLATE_NAME) {
            public void setDeclutter(boolean declutter) {
                super.setDeclutter(declutter);
                updateStatus();
            }
        };
    }

    /**
     * Initialize  the  station map
     *
     * @param stationMap The station map to initialize
     */
    protected void initStationMap(StationLocationMap stationMap) {
        super.initStationMap(stationMap);

        // get station information from the xml file listed
        if (stationResources == null) {
            List resources =
                Misc.newList(
                    "/ucar/unidata/idv/resources/stations/profilerstns.xml");
            stationResources = new XmlResourceCollection("", resources);
        }

        // create an object to hold the station info
        NamedStationTable stationTable =
            NamedStationTable.createStationTable(stationResources.getRoot(0));


        List listOfTables =
            NamedStationTable.createStationTables(stationResources);
        if (listOfTables.size() > 0) {
            NamedStationTable profStations =
                (NamedStationTable) listOfTables.get(0);
            // Take this out if we only want to init stations 
            // when we connect to the server. 
            //   each "value" is a full Station object, not the name string
            stationMap.setStations(new ArrayList(profStations.values()));
        } else {
            //What to do if there are no stations
        }
    }

    /**
     * Get any extra key=value pairs that are appended to all requests.
     *
     * @param buff The buffer to append onto
     */
    protected void appendMiscKeyValues(StringBuffer buff) {
        appendKeyValue(buff, PROP_POS, getDoRelativeTimes() ? "ALL" : "0");
        super.appendMiscKeyValues(buff);
    }

    /**
     * Override this to determine how to select sample
     */
    protected void appendTimesRequest(StringBuffer buf) {
    	appendKeyValue(buf, PROP_SELECT, "'IDA BLMM'");
    }
    
    /**
     * Do we have times selected. Either we are doing absolute
     * times and there are some selected in the list. Or we
     * are doing relative times and we have done a connect to the
     * server
     *
     * @return Do we have times
     */
    public boolean timesOk() {
        if (usingStations() && (stationMap.getStations().size() > 0)
                && (getSelectedStations().size() == 0)) {
        	return false;
        }
    	return super.timesOk();
    }
    
    /**
     * Get the selection event from the profiler data chooser
     * and process it, creating a ADDE.PROFILER data source
     *
     */
    public void doLoadInThread() {
        showWaitCursor();
        try {
            List selectedStations = getSelectedStations();

            // make properties Hashtable to hand some 
            //  data selection metadata
            // to the AddeProfilerDataSource where it helps process 
            // the data from the server into data format this IDV
            // needs for this request.

            Hashtable profilersourceHT = new Hashtable();

            profilersourceHT.put(AddeProfilerDataSource.PROFILER_INT,
            		getRelBoxString());
            profilersourceHT.put(AddeProfilerDataSource.PROFILER_DATAINT,
            		getDescriptor());
            profilersourceHT.put(AddeProfilerDataSource.PROFILER_SERVER,
            		getServer());
            profilersourceHT.put(AddeProfilerDataSource.PROFILER_TIMES,
            		getDayTimeSelectString());
            profilersourceHT.put(AddeUtil.NUM_RELATIVE_TIMES,
            		getRelativeTimeIndices());
            profilersourceHT.put(AddeUtil.RELATIVE_TIME_INCREMENT,
            		new Float(getRelativeTimeIncrement()));
            profilersourceHT.put(AddeUtil.MISC_KEYWORDS,
            		getMiscKeywords());


            //System.out.println("   pc time list "+getSelectedTimes());
            //System.out.println
            // ("   pc data display interval "+getSelectedDataInterval());
            //System.out.println
            // ("  pc data source interval  "+getDataSourceInterval());

            // hard-coded "ADDE.PROFILER" is in idv/resources/datasource.xml,
            // which tells IDV to use code 
            // ucar.unidata.data.profiler.AddeProfilerDataSource
            makeDataSource(selectedStations, "ADDE.PROFILER",
                           profilersourceHT);
            saveServerState();
        } catch (Exception excp) {
            logException("Unable to open Profiler dataset", excp);
        }
        showNormalCursor();
    }
        
    /**
     * Get the extra time widget, but built in a different way.
     * Designed to be put into a GroupLayout
     */
    protected JComponent getExtraTimeComponent() {
    	TwoFacedObject[] intervals = { 
    			new TwoFacedObject(AddeProfilerDataSource.PROFILER_6MIN, .2f),
    			new TwoFacedObject(AddeProfilerDataSource.PROFILER_12MIN, .1f),
    			new TwoFacedObject(AddeProfilerDataSource.PROFILER_30MIN, .5f),
    			new TwoFacedObject(AddeProfilerDataSource.PROFILER_1HR, 1f)
    	};

        GuiUtils.setListData(relTimeIncBox, intervals);
        if (relTimeIncBox.getItemCount()>=4) relTimeIncBox.setSelectedIndex(3);
        
        return McVGuiUtils.makeLabeledComponent(relTimeIncLabel, relTimeIncBox);
    }
        
    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    public JComponent doMakeContents() {      
    	JPanel myPanel = new JPanel();
    	    	    	
        JLabel stationLabel = McVGuiUtils.makeLabelRight("Stations:");
        addServerComp(stationLabel);

        JComponent stationPanel = getStationMap();
        registerStatusComp("stations", stationPanel);
        addServerComp(stationPanel);
        
        JLabel timesLabel = McVGuiUtils.makeLabelRight("Times:");
        addDescComp(timesLabel);
        
        JPanel timesPanel = makeTimesPanel();
        timesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        addDescComp(timesPanel);
        
        enableWidgets();
        updateStatus();

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
                    .add(timesPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED))
        );
        
        setInnerPanel(myPanel);
        return super.doMakeContents(true);
    }

}

