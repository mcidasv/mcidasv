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

package edu.wisc.ssec.mcidasv.chooser.adde;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import org.w3c.dom.Element;

import ucar.unidata.data.AddeUtil;
import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.point.AddePointDataSource;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.ui.symbol.StationModelManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.station.StationLocationMap;
import ucar.visad.UtcDate;

import visad.DateTime;

import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaDirectoryList;
import edu.wisc.ssec.mcidas.AreaFileException;
import edu.wisc.ssec.mcidas.McIDASException;
import edu.wisc.ssec.mcidas.McIDASUtil;
import edu.wisc.ssec.mcidas.adde.AddePointDataReader;
import edu.wisc.ssec.mcidas.adde.DataSetInfo;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;


/**
 * Selection widget for ADDE point data
 *
 * @author MetApps Development Team
 * @version $Revision$ $Date$
 */
public class AddePointDataChooser extends AddeChooser {

    /**
     * Property for the dataset name key.
     * @see #getDatasetName()
     */
    public static String DATASET_NAME_KEY = "name";

    /** Property for the data type. */
    public static String DATA_TYPE = "ADDE.POINT";

    /** Are we currently reading times */
    private Object readTimesTask;
    
    /** box for the relative time */
    private JComboBox relTimeIncBox;

    /** the relative time increment */
    private float relativeTimeIncrement = 1;

    /** list of levels */
    //TODO: These should be in the field selector... no reason to have them in the chooser
    private static String[] levels = {
        "SFC", "1000", "925", "850", "700", "500", "400", "300", "250", "200",
        "150", "100", "70", "50", "30", "20", "10"
    };
    
    //TODO: Generalize the following lists somehow--persistence?

    /** list of descriptors to use */
    protected String[] descriptorsAllow = new String[] {};
    
    /** list of descriptors to ignore */
    protected String[] descriptorsDeny = new String[] {};
    
    /** list of descriptors that have upper air data */
    protected String[] descriptorsUpper = new String[] {};
    
    protected boolean firstTime = true;
    protected boolean retry = true;
        
    /**
     * Create a chooser for Adde POINT data
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddePointDataChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
                
        addDescComp(addSourceButton);

        descriptorsAllow = new String[] { };
        
        descriptorsDeny = new String[] {
        		"PTSRCS", "PROFHOURLY", "PROF6MIN", "LIGHTNING"
        };
        
        descriptorsUpper = new String[] {
        		"UPPERMAND", "UPPERSIG"
        };

    }
    
    /**
     *  Generate a list of image descriptors for the descriptor list.
     */
    protected void readDescriptors() {
        try {
            StringBuffer buff   = getGroupUrl(REQ_DATASETINFO, getGroup());
            buff.append("&type=" + getDataType());
            DataSetInfo  dsinfo = new DataSetInfo(buff.toString());
            descriptorTable = dsinfo.getDescriptionTable();
                        
            // Remove "special" POINT types that we know about... generalize this
            Hashtable descriptorsAllowHash = new Hashtable();
            for (String descriptor : descriptorsAllow) {
            	descriptorsAllowHash.put(descriptor, descriptor);
            }
            Hashtable descriptorsDenyHash = new Hashtable();
            for (String descriptor : descriptorsDeny) {
            	descriptorsDenyHash.put(descriptor, descriptor);
            }
            for (Enumeration e = descriptorTable.keys(); e.hasMoreElements();) {
            	Object key = e.nextElement();
                String str = (String) descriptorTable.get(key);
                if (descriptorsAllowHash.size() > 0 && !descriptorsAllowHash.contains((String)str)) {
                	descriptorTable.remove(key);
                }
                if (descriptorsDenyHash.contains((String)str)) {
                	descriptorTable.remove(key);
                }
            }
            
            String[]    names       = new String[descriptorTable.size()];
            Enumeration enumeration = descriptorTable.keys();
            for (int i = 0; enumeration.hasMoreElements(); i++) {
                names[i] = enumeration.nextElement().toString();
            }
            Arrays.sort(names);
            setDescriptors(names);
            setState(STATE_CONNECTED);
        } catch (Exception e) {
            handleConnectionError(e);
        }
    }
    
    /**
     * Load in an ADDE point data set based on the
     * <code>PropertyChangeEvent<code>.
     *
     */
    public void doLoadInThread() {
        showWaitCursor();
        try {
//            StationModel selectedStationModel = getSelectedStationModel();
            String       source               = getRequestUrl();
                        
            // make properties Hashtable to hand the station name
            // to the AddeProfilerDataSource
            Hashtable ht = new Hashtable();
            getDataSourceProperties(ht);
//            ht.put(AddePointDataSource.PROP_STATIONMODELNAME,
//                   selectedStationModel.getName());
            ht.put(DATASET_NAME_KEY, getDescriptor());
            ht.put(DATA_NAME_KEY, getDataName());
            if (source.indexOf(AddeUtil.RELATIVE_TIME) >= 0) {
                ht.put(AddeUtil.NUM_RELATIVE_TIMES, getRelativeTimeIndices());
                ht.put(AddeUtil.RELATIVE_TIME_INCREMENT,
                       new Float(getRelativeTimeIncrement()));
            }

//            System.out.println("makeDataSource: source=" + source);
//            System.out.println("    DATA_TYPE=" + DATA_TYPE);
//            System.out.println("    ht=" + ht);
            makeDataSource(source, DATA_TYPE, ht);
            saveServerState();
        } catch (Exception excp) {
            logException("Unable to open ADDE point dataset", excp);
        }
        showNormalCursor();
    }
    
    /**
     * Add the interval selector to the component.
     * @return superclass component with extra stuff
     */
    protected JPanel makeTimesPanel() {
    	JPanel newPanel = new JPanel();
    	
    	JPanel timesPanel = super.makeTimesPanel(false,true);
    	
    	JPanel customPanel = (JPanel)getCustomTimeComponent();
    	    	
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(newPanel);
        newPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(timesPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(layout.createSequentialGroup()
                .add(customPanel)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(timesPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(customPanel))
        );

    	return newPanel;
    }
    
    /**
     * Get the extra time widget, but built in a different way.
     * Designed to be put into a GroupLayout
     */
    protected JComponent getCustomTimeComponent() {
    	TwoFacedObject[] intervals = { 
    			new TwoFacedObject("30 minute", .5f),
    			new TwoFacedObject("Hourly", 1f),
    			new TwoFacedObject("Three hourly", 3f),
    			new TwoFacedObject("Six hourly", 6f),
    			new TwoFacedObject("12 hourly", 12f),
    			new TwoFacedObject("24 hourly", 24f)
    	};

        relTimeIncBox = new JComboBox();
        GuiUtils.setListData(relTimeIncBox, intervals);
        relTimeIncBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JComboBox box = (JComboBox) ae.getSource();
                if (GuiUtils.anySelected(box)) {
                    setRelativeTimeIncrement(getRelBoxValue());
                }
            }
        });
        McVGuiUtils.setComponentSize(relTimeIncBox, Width.ONEHALF);
        addServerComp(relTimeIncBox);
        relTimeIncBox.setToolTipText("Set the increment between relative times");
        
        JPanel newPanel = new JPanel();
        
        JLabel relTimeIncLabel = new JLabel("Interval:");
                
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(newPanel);
        newPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(relTimeIncLabel)
                .add(GAP_RELATED)
                .add(relTimeIncBox))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                .add(relTimeIncLabel)
                .add(relTimeIncBox))
        );

        return newPanel;
    }
        
    /**
     * Get the value from the relative increment box
     *
     * @return the selected value or a default
     */
    protected float getRelBoxValue() {
        float value = relativeTimeIncrement;
        if (relTimeIncBox != null) {
            Object o = relTimeIncBox.getSelectedItem();
            if (o != null) {
                String val = TwoFacedObject.getIdString(o);
                value = (float) Misc.parseNumber(val);
            }
        }
        return value;
    }
    
    /**
     * Get the string from the relative increment box
     *
     * @return the selected string or a default
     */
    public String getRelBoxString() {
    	String value = "";
    	if (relTimeIncBox != null) {
            Object o = relTimeIncBox.getSelectedItem();
            if (o != null) {
                value = TwoFacedObject.getIdString(o);
            }
    	}
        return value;
    }

    /**
     * Get the request URL
     *
     * @return  the request URL
     */
    public String getRequestUrl() {
        StringBuffer request = getGroupUrl(REQ_POINTDATA, getGroup());
        appendKeyValue(request, PROP_USER, getLastAddedUser());
        appendKeyValue(request, PROP_PROJ, getLastAddedProj());
        appendKeyValue(request, PROP_DESCR, getDescriptor());
        appendRequestSelectClause(request);
        appendKeyValue(request, PROP_NUM, "ALL");
        appendKeyValue(request, PROP_POS, getDoRelativeTimes() ? "ALL" : "0");
        return request.toString();
    }
        
    /**
     * Get the select clause for the adde request specific to this
     * type of data.
     *
     * @param buf The buffer to append to
     */
    protected void appendRequestSelectClause(StringBuffer buf) {
    	StringBuffer selectValue = new StringBuffer();
    	selectValue.append("'");
    	selectValue.append(getDayTimeSelectString());
    	if (getDescriptor().equalsIgnoreCase("SFCHOURLY")) {
    		selectValue.append(";type 0");
    	}
    	selectValue.append(";");
    	selectValue.append(AddeUtil.LATLON_BOX);
    	selectValue.append("'");
    	if (isUpperAir()){
    		selectValue.append(AddeUtil.LEVEL);
    		selectValue.append(";");
    	}
    	appendKeyValue(buf, PROP_SELECT, selectValue.toString());
    }
    
    /**
     * Check if we are ready to read times
     *
     * @return  true if times can be read
     */
    protected boolean canReadTimes() {
        return haveDescriptorSelected();
    }
        
    /**
     * Enable or disable the GUI widgets based on what has been
     * selected.
     */
    protected void enableWidgets() {
        boolean descriptorState = ((getState() == STATE_CONNECTED)
                                   && canReadTimes());

        for (int i = 0; i < compsThatNeedDescriptor.size(); i++) {
            JComponent comp = (JComponent) compsThatNeedDescriptor.get(i);
            GuiUtils.enableTree(comp, descriptorState);
        }

        boolean timesOk = timesOk();
        
        // Require times to be selected
        GuiUtils.enableTree(addSourceButton, descriptorState && timesOk);

        checkTimesLists();
        
        enableAbsoluteTimesList(getDoAbsoluteTimes() && descriptorState);

        getRelativeTimesChooser().setEnabled( !getDoAbsoluteTimes()
                && descriptorState);

        revalidate();
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
        if (getDoAbsoluteTimes() && !haveTimeSelected()) {
            return false;
        }
        return true;
    }
    
    /**
     * Return true if selected descriptor is for upper air
     */
    protected boolean isUpperAir() {
    	String descriptor = getDescriptor();
    	for (String descriptorUpper : descriptorsUpper) {
    		if (descriptor.equals(descriptorUpper)) return true;
    	}
    	return false;
    }
    
    /**
     * Update the widget with the latest data.
     *
     * @throws Exception On badness
     */
    public void handleUpdate() throws Exception {
        if (getState() != STATE_CONNECTED) {
            //If not connected then update the server list
            updateServerList();
        } else {
            //If we are already connected then update the rest of the chooser
            descriptorChanged();
        }
        updateStatus();
    }
        
    /**
     * Get the request string for times particular to this chooser
     *
     * @return request string
     */
    protected String getTimesRequest() {
        StringBuffer buf = getGroupUrl(REQ_POINTDATA, getGroup());
        appendKeyValue(buf, PROP_USER, getLastAddedUser());
        appendKeyValue(buf, PROP_PROJ, getLastAddedProj());
        appendKeyValue(buf, PROP_DESCR, getDescriptor());
        appendTimesRequest(buf);
        if (getDoAbsoluteTimes()) {
            appendKeyValue(buf, PROP_NUM, "ALL");
        }
        appendKeyValue(buf, PROP_PARAM, "DAY TIME");
        return buf.toString();
    }
    
    /**
     * Override this to determine how to select sample
     */
    protected void appendTimesRequest(StringBuffer buf) {
        if (!isUpperAir()) {
            appendKeyValue(buf, PROP_POS, "0");
        	appendKeyValue(buf, PROP_SELECT, "'LAT 39.5 40.5;LON 104.5 105.5'");
        }
        else {
            appendKeyValue(buf, PROP_POS, "ALL");
        }
    }
    
    /**
     * This allows derived classes to provide their own name for labeling, etc.
     *
     * @return  the dataset name
     */
    public String getDataName() {
        return "Point Data";
    }
    
    /**
     *  Read the set of image times available for the current server/group/type
     *  This method is a wrapper, setting the wait cursor and wrapping the
     *  call to {@link #readTimesInner()}; in a try/catch block
     */
    protected void readTimes() {
        clearTimesList();
        if ( !canReadTimes()) {
            return;
        }
        Misc.run(new Runnable() {
            public void run() {
                updateStatus();
                showWaitCursor();
                try {
                    readTimesInner();
                } catch (Exception e) {
                    handleConnectionError(e);
                }
                showNormalCursor();
                updateStatus();
            }
        });
    }

    /**
     * _more_
     */
    public void doCancel() {
        readTimesTask = null;
        setState(STATE_UNCONNECTED);
        super.doCancel();
    }

    /** locking mutex */
    private Object MUTEX = new Object();
    
    /**
     * Set the list of dates/times based on the image selection
     *
     */
    protected void readTimesInner() {
        SortedSet uniqueTimes = Collections.synchronizedSortedSet(new TreeSet());

        readTimesTask = startTask();
        updateStatus();
        Object task = readTimesTask;
        try {
            AddePointDataReader apr = new AddePointDataReader(getTimesRequest());
            //Make sure no other loads are  occurred
            boolean ok = stopTaskAndIsOk(task);
            if ( !Misc.equals(readTimesTask, task) || !ok) {
                return;
            }
            readTimesTask = null;

            synchronized (MUTEX) {
                int[][]  data  = apr.getData();
                String[] units = apr.getUnits();
                if ( !units[0].equals("CYD") || !units[1].equals("HMS")) {
                    throw new Exception("can't handle date/time units");
                }
                int numObs = data[0].length;
                //System.out.println("found " + numObs + " obs");
                // loop through and find all the unique times
                for (int i = 0; i < numObs; i++) {
                    try {
                        DateTime dt =
                            new DateTime(McIDASUtil.mcDayTimeToSecs(data[0][i],
                                data[1][i]));
                        uniqueTimes.add(dt);
                    } catch (Exception e) {}
                }
                //System.out.println(
                //      "found " + uniqueTimes.size() + " unique times");
                if (getDoAbsoluteTimes()) {
                    if ( !uniqueTimes.isEmpty()) {
                    	setAbsoluteTimes(new ArrayList(uniqueTimes));
                    	getTimesList().setSelectionMode(
                    			ListSelectionModel.SINGLE_INTERVAL_SELECTION);
                    }

                    //   Select the last n hours 
                    int selectedIndex = getAbsoluteTimes().size() - 1;
                    int firstIndex = Math.max(0, selectedIndex
                    		- getDefaultRelativeTimeIndex());
                    if (selectedIndex >= 0)
                    	setSelectedAbsoluteTime(selectedIndex, firstIndex);
                }
            }
            setState(STATE_CONNECTED);
        } catch (Exception excp) {
            stopTask(task);
            readTimesTask = null;
            handleConnectionError(excp);
            if (retry == false) return;
            try {
                handleUpdate();
            } catch (Exception e) {}

        }
    }
        
    /**
     * Show the given error to the user. If it was an Adde exception
     * that was a bad server error then print out a nice message.
     *
     * @param excp The exception
     */
    protected void handleConnectionError(Exception e) {
        if (e != null && e.getMessage() != null) {
            String msg = e.getMessage().toLowerCase();
        	if (msg != null && msg.indexOf("must be same schema type")>=0) {
        		retry = false;
        		return;
        	}
        }
        super.handleConnectionError(e);
    }

    protected int getNumTimesToSelect() {
        return 5;
    }
    
    /**
     * Get the default selected index for the relative times list.
     *
     * @return default index
     */
    protected int getDefaultRelativeTimeIndex() {
        return 4;
    }

    /**
     * Are there any times selected.
     *
     * @return Any times selected.
     */
    protected boolean haveTimeSelected() {
        return !getDoAbsoluteTimes() || getHaveAbsoluteTimesSelected();
    }

    /**
     * Create the date time selection string for the "select" clause
     * of the ADDE URL.
     *
     * @return the select day and time strings
     */
    protected String getDayTimeSelectString() {
        StringBuffer buf = new StringBuffer();
        if (getDoAbsoluteTimes()) {
            List times = getSelectedAbsoluteTimes();

            // no time selection is permitted as a valid choice -
            // will then use all times today by default.
            if (times.size() == 0) {
                return "";
            }

            //check for the "no times available" message
            if (times.get(0) instanceof String) {
                return "";
            }

            buf.append("time ");
            for (int i = 0; i < times.size(); i++) {
                DateTime dt = (DateTime) times.get(i);
                buf.append(UtcDate.getHMS(dt));
                if (i != times.size() - 1) {
                    buf.append(",");
                }
            }
        } else {
            buf.append(AddeUtil.RELATIVE_TIME);
        }
        return buf.toString();
    }

//    /**
//     * Get the name of the dataset.
//     *
//     * @return descriptive name of the dataset.
//     */
//    public String getDatasetName() {
//        return dataTypes.getSelectedItem().toString();
//    }

    /**
     * Get the data type for this chooser
     *
     * @return  the type
     */
    public String getDataType() {
        return "POINT";
    }

    /**
     * Get the increment between times for relative time requests
     *
     * @return time increment (hours)
     */
    public float getRelativeTimeIncrement() {
        return relativeTimeIncrement;
    }

    /**
     * Set the increment between times for relative time requests
     *
     * @param increment time increment (hours)
     */
    public void setRelativeTimeIncrement(float increment) {
        relativeTimeIncrement = increment;
        if (relTimeIncBox != null) {
            relTimeIncBox.setSelectedItem(new Float(relativeTimeIncrement));
        }
    }

    /**
     * Update labels, enable widgets, etc.
     */
    protected void updateStatus() {
        super.updateStatus();
        if (readTimesTask != null) {
            if (taskOk(readTimesTask)) {
                setStatus("Reading available times from server");
            }
        } else if (getDoAbsoluteTimes() && !haveTimeSelected()) {
            setStatus(MSG_TIMES);
        }
        enableWidgets();
    }

                
    /**
     * Get the descriptor widget label.
     *
     * @return  label for the descriptor  widget
     */
    public String getDescriptorLabel() { 
        return "Point Type"; 
    }
    
    /**
     * get the adde server grup type to use
     *
     * @return group type
     */
    @Override
    protected String getGroupType() {
        return AddeServer.TYPE_POINT;
    }
    
    /**
     * Make the UI for this selector.
     * 
     * @return The gui
     */   
    public JComponent doMakeContents() {
    	JPanel myPanel = new JPanel();
    	
    	McVGuiUtils.setComponentSize(descriptorComboBox, Width.DOUBLEDOUBLE);
    	        
        JLabel stationLabel = McVGuiUtils.makeLabelRight("Station:");
        addServerComp(stationLabel);
    	
        JLabel timesLabel = McVGuiUtils.makeLabelRight("Times:");
        addDescComp(timesLabel);
        
        JPanel timesPanel = makeTimesPanel();
        timesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        addDescComp(timesPanel);
                
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
                    .add(timesLabel)
                    .add(timesPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        
        setInnerPanel(myPanel);
        return super.doMakeContents();
    }

    public JComponent doMakeContents(boolean doesOverride) {
    	if (doesOverride)
    		return super.doMakeContents();
    	else
    		return doMakeContents();
    }
    
}
