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
import javax.swing.SwingConstants;

import org.w3c.dom.Element;

import ucar.unidata.data.AddeUtil;
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
import ucar.visad.UtcDate;

import visad.DateTime;

import edu.wisc.ssec.mcidas.AreaFileException;
import edu.wisc.ssec.mcidas.McIDASUtil;
import edu.wisc.ssec.mcidas.adde.AddePointDataReader;
import edu.wisc.ssec.mcidas.adde.DataSetInfo;


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

    /** UI widget for selecting station models */
//    protected JComboBox stationModelBox;
    
    /** box for the relative time */
    private JComboBox relTimeIncBox;

    /** box for the relative time */
    private JComponent relTimeIncComp;

    /** the relative time increment */
    private float relativeTimeIncrement = 1.f;

    /** list of levels */
    //TODO: These should be in the field selector... no reason to have them in the chooser
    private static String[] levels = {
        "SFC", "1000", "925", "850", "700", "500", "400", "300", "250", "200",
        "150", "100", "70", "50", "30", "20", "10"
    };

    /** flag for selecting 00 and 12Z data only */
    private boolean zeroAndTwelveZOnly = true;
    
    /** flag for using levels for upper air data */
    private boolean doUpperAir = false;

    protected boolean firstTime = true;
    protected boolean retry = true;
    
    /** Panels that can be shown or hidden based on Upper Air selections */
    JComponent customPanel;
    JComponent customPanelUpperAir;
    
    /** Accounting information */
    protected static String user = "idv";
    protected static String proj = "0";

    /**
     * Create a chooser for Adde POINT data
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddePointDataChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
                
        addServerComp(addSourceButton);

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
     * Add the 00 & 12Z checkbox to the component.
     * @return superclass component with extra stuff
     */
    protected JPanel makeTimesPanel() {
    	JPanel newPanel = new JPanel();
    	
    	JPanel timesPanel = super.makeTimesPanel(false,true);
    	
    	customPanel = getCustomTimeComponent();
    	customPanel.setPreferredSize(new Dimension(customPanel.getPreferredSize().width, 24));
    	customPanel.setMinimumSize(new Dimension(customPanel.getMinimumSize().width, 24));
    	customPanel.setMaximumSize(new Dimension(customPanel.getMaximumSize().width, 24));
    	
    	customPanelUpperAir = getCustomTimeComponentUpperAir();
    	customPanelUpperAir.setPreferredSize(new Dimension(customPanel.getPreferredSize().width, 24));
    	customPanelUpperAir.setMinimumSize(new Dimension(customPanel.getMinimumSize().width, 24));
    	customPanelUpperAir.setMaximumSize(new Dimension(customPanel.getMaximumSize().width, 24));
    	customPanelUpperAir.setVisible(false);
    	
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(newPanel);
        newPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(timesPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(layout.createSequentialGroup()
                .add(customPanel)
                .add(customPanelUpperAir)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(timesPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(customPanel)
                .add(customPanelUpperAir))
        );

    	return newPanel;
    }
    
    /**
     * Get the extra time widget, but built in a different way.
     * Designed to be put into a GroupLayout
     */
    protected JComponent getCustomTimeComponent() {
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JComboBox box = (JComboBox) ae.getSource();
                if (GuiUtils.anySelected(box)) {
                    setRelativeTimeIncrement(getRelBoxValue());
                }
            }
        };
        String[] nums = new String[] {
            ".5", "1", "3", "6", "12", "24"
        };
        float[]  vals = new float[] {
            .5f, 1f, 3f, 6f, 12f, 24f
        };
        List l = new ArrayList();
        for (int i = 0; i < nums.length; i++) {
            l.add(new TwoFacedObject(nums[i], new Float(vals[i])));
        }
        relTimeIncBox = GuiUtils.getEditableBox(l, new Float(relativeTimeIncrement));
        relTimeIncBox.addActionListener(listener);
        relTimeIncBox.setToolTipText("Set the increment between most recent times");
        relTimeIncBox.setPreferredSize(new Dimension(ELEMENT_HALF_WIDTH, 24));
        relTimeIncBox.setMinimumSize(new Dimension(ELEMENT_HALF_WIDTH, 24));
        relTimeIncBox.setMaximumSize(new Dimension(ELEMENT_HALF_WIDTH, 24));
        
        JCheckBox timeSubset = GuiUtils.makeCheckbox("00 & 12Z only", this, "zeroAndTwelveZOnly");
        
        JPanel newPanel = new JPanel();
        
        JLabel beforeLabel = new JLabel("Use relative time increments of");
        JLabel afterLabel = new JLabel("hour(s)");
        
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(newPanel);
        newPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(beforeLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(relTimeIncBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(afterLabel)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                .add(beforeLabel)
                .add(relTimeIncBox)
                .add(afterLabel))
        );

        return newPanel;
    }
    
    /**
     * Get the extra time widget, but built in a different way.
     * Designed to be put into a GroupLayout
     */
    protected JComponent getCustomTimeComponentUpperAir() {
        JCheckBox timeSubset = GuiUtils.makeCheckbox("00 & 12Z only", this, "zeroAndTwelveZOnly");
        return timeSubset;
    }
    
    /**
     * Get the value from the relative increment box
     *
     * @return the seleted value or a default
     */
    private float getRelBoxValue() {
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
     * Get whether we should show 00 and 12Z times only.
     * @return true if only 00 and 12Z obs
     */
    public boolean getZeroAndTwelveZOnly() {
        return zeroAndTwelveZOnly;
    }

    /**
     * Set whether we should show 00 and 12Z times only.
     * @param value true if only 00 and 12Z obs
     */
    public void setZeroAndTwelveZOnly(boolean value) {
        zeroAndTwelveZOnly = value;
        if (getDoAbsoluteTimes()) {
            readTimes();
        }
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
    	if (doUpperAir){
    		selectValue.append(AddeUtil.LEVEL);
    		selectValue.append(";");
    	}
    	appendKeyValue(buf, PROP_SELECT, selectValue.toString());
    }
    
    /**
     * Handle when the user presses the connect button
     *
     * @throws Exception On badness
     */
    public void handleConnect() throws Exception {
//        System.err.println("AddeImageChooser: enter handleConnect");
        setState(STATE_CONNECTING);
//        System.err.println("AddeImageChooser: after setState");
        connectToServer();
//        System.err.println("AddeImageChooser: after connectToServer");
        updateStatus();
//        System.err.println("AddeImageChooser: leaving after updateStatus. status=" + getState());
    }
    
    /**
     * Update the widget with the latest data.
     *
     * @throws Exception On badness
     */
    @Override public void handleUpdate() throws Exception {
        if (getState() != STATE_CONNECTED) {
            //If not connected then connect.
//            handleConnect();
            updateServerList();
        } else {
            //If we are already connected  then update the rest of the chooser
            descriptorChanged();
        }
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
        // this is hokey, but take a smattering of stations.  
        //buf.append("&select='ID KDEN'");
        if (doUpperAir && getZeroAndTwelveZOnly()) {
            appendKeyValue(buf, PROP_SELECT, "'TIME 00,12'");
        }
        else {
        	appendKeyValue(buf, PROP_SELECT, "'LAT 39.5 40.5;LON 104.5 105.5'");
        }
        appendKeyValue(buf, PROP_POS, "0");  // set to 0 for now
        if (getDoAbsoluteTimes()) {
            appendKeyValue(buf, PROP_NUM, "ALL");
        }
        appendKeyValue(buf, PROP_PARAM, "DAY TIME");
        return buf.toString();
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
     * Set the list of available times.
     */
    protected void readTimes() {
        clearTimesList();
        SortedSet uniqueTimes =
            Collections.synchronizedSortedSet(new TreeSet());
        if (getDescriptor() == null) return;
//DAVEP
//        setState(STATE_CONNECTING);
        try {
            AddePointDataReader apr =
                new AddePointDataReader(getTimesRequest());
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
            setState(STATE_CONNECTED);
            //System.out.println(
            //      "found " + uniqueTimes.size() + " unique times");
        } catch (Exception excp) {        	
            //System.out.println("I am here excp=" + excp);
            handleConnectionError(excp);
            if (retry == false) return;
            try {
                handleUpdate();
            } catch (Exception e) {
            }
        }
        if (getDoAbsoluteTimes()) {
            if ( !uniqueTimes.isEmpty()) {
                setAbsoluteTimes(new ArrayList(uniqueTimes));
            }
            int selectedIndex = getAbsoluteTimes().size() - 1;
            setSelectedAbsoluteTime(selectedIndex);
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
        return 1;
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
            buf.append("time ");
            List times = getSelectedAbsoluteTimes();
            for (int i = 0; i < times.size(); i++) {
                DateTime dt = (DateTime) times.get(i);
                buf.append(UtcDate.getHMS(dt));
                if (i != times.size() - 1) {
                    buf.append(",");
                }
            }
        } else {
            buf.append(getRelativeTimeId());
        }
        return buf.toString();
    }

    /**
     * Get the identifier for relative time.  Subclasses can override.
     * @return the identifier
     */
    protected String getRelativeTimeId() {
        return AddeUtil.RELATIVE_TIME;
    }

    /**
     * Get the name of the dataset.
     *
     * @return descriptive name of the dataset.
     */
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
    	if (doUpperAir) {
            return getZeroAndTwelveZOnly() ? 12 : 3;
    	}
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
        enableWidgets();
    }
    
    /**
     * Enable or disable the GUI widgets based on what has been
     * selected.
     */
    protected void enableWidgets() {
        super.enableWidgets();
        if (relTimeIncComp != null) {
            GuiUtils.enableTree(relTimeIncComp, getDoRelativeTimes());
        }

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
     * Get the name of the dataset.
     *
     * @return descriptive name of the dataset.
     */
    public String getDatasetName() {
    	return getSelectedDescriptor();
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
    	
        descriptorComboBox.setMinimumSize(new Dimension(ELEMENT_DOUBLE_DOUBLE_WIDTH, 24));
        descriptorComboBox.setMaximumSize(new Dimension(ELEMENT_DOUBLE_DOUBLE_WIDTH, 24));
        descriptorComboBox.setPreferredSize(new Dimension(ELEMENT_DOUBLE_DOUBLE_WIDTH, 24));
    	        
        JLabel timesLabel = new JLabel("Times:");
        timesLabel.setMinimumSize(new Dimension(ELEMENT_WIDTH, 24));
        timesLabel.setMaximumSize(new Dimension(ELEMENT_WIDTH, 24));
        timesLabel.setPreferredSize(new Dimension(ELEMENT_WIDTH, 24));
        timesLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        timesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        addServerComp(timesLabel);
        
        JPanel timesPanel = makeTimesPanel();
        timesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        addServerComp(timesPanel);
                
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

}
