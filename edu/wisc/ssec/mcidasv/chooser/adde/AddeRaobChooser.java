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
package edu.wisc.ssec.mcidasv.chooser.adde;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.w3c.dom.Element;

import edu.wisc.ssec.mcidas.McIDASUtil;
import edu.wisc.ssec.mcidas.adde.AddePointDataReader;
import edu.wisc.ssec.mcidas.adde.DataSetInfo;

import visad.DateTime;

import ucar.unidata.data.sounding.RaobDataSet;
import ucar.unidata.data.sounding.SoundingOb;
import ucar.unidata.data.sounding.SoundingStation;
import ucar.unidata.gis.mcidasmap.McidasMap;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.metdata.Station;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.view.CompositeRenderer;
import ucar.unidata.view.station.StationLocationMap;

import edu.wisc.ssec.mcidasv.data.adde.AddeSoundingAdapter;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;

/**
 * A chooser class for selecting Raob data.
 * Mostly just a wrapper around a
 *  {@link ucar.unidata.view.sounding.SoundingSelector}
 * that does most of the work
 *
 * @author IDV development team
 * @version $Revision$Date: 2011/03/24 16:06:32 $
 */


public class AddeRaobChooser extends AddePointDataChooser {
    
    /** Property for the data type. */
    public static String DATA_TYPE = "RAOB";
    
    /** Significant level objects corresponding to mandatory level objects */
    private Hashtable descriptorTable2 = new Hashtable();
    private JComboBox descriptorComboBox2 = new JComboBox();
    protected String[] descriptorNames2;
    private String LABEL_SELECT2 = " -- Optional Significant Levels -- ";
    private JCheckBox showAll = new JCheckBox("Show all sources");
    private Object readSatelliteTask;

    /** This flag keeps track of observed/satellite soundings */
    private boolean satelliteSounding = false;
    
    /** Selector for times when pointing to satellite data (required field) */
    private JLabel satelliteTimeLabel = McVGuiUtils.makeLabelRight("");
    private JPanel satelliteTimePanel;
    private JButton satelliteTimeButton;
    private JComboBox satelliteTimeComboBox;
    private JTextField satellitePixelTextField;
    private String satelliteTime = "";
    private String satellitePixel = "1";
    private List satelliteTimes = new ArrayList();
    
    /** We need to be able to enable/disable this based on sounding type */
    private JCheckBox mainHoursCbx;
    
    /** This is a virtual timestamp that tracks if the threaded adde connection should be aborted or not */
    private int connectionStep = 0;

    /** handle on the station update task */
    private Object readStationTask;
    
    /** list of times */
    private JList timesList;

    /** list of observations */
    private JList obsList;

    /** selected observations */
    private Vector selectedObs = new Vector();
    
    /** sounding adapter used by this selector */
    AddeSoundingAdapter soundingAdapter;
    
    /** flag for 0 and 12z only */
    private boolean showMainHoursOnly = true;

    /**
     * Construct a <code>RaobChooser</code> using the manager
     * and the root XML that defines this object.
     *
     * @param mgr  <code>IdvChooserManager</code> that controls this chooser.
     * @param root root element of the XML that defines this object
     */
    public AddeRaobChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        
        setSelectString(" -- Select Mandatory Levels -- ");
        
        descriptorComboBox2.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ( !ignoreDescriptorChange
                        && (e.getStateChange() == e.SELECTED)) {
                    descriptorChanged(false);
                }
            }
        });
        descriptorComboBox2.setEnabled(false);
        
        showAll.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (getState() == STATE_CONNECTED) {
                    doConnect();
                }
            }
        });
        
        satelliteTimeComboBox = new JComboBox();
        satelliteTimeComboBox.setEditable(true);
        satelliteTimeComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange()==e.DESELECTED) return;
                satelliteTime = satelliteTimeComboBox.getSelectedItem().toString();
                Misc.run(new Runnable() {
                    public void run() {
                        setAvailableStations(true);
                    }
                });
            }
        });

        satelliteTimeButton = McVGuiUtils.makeImageButton(ICON_UPDATE, "Request list of available times from server");
        satelliteTimeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sampleTimes();
            }
        });

        satellitePixelTextField = new JTextField(satellitePixel);
        satellitePixelTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                satellitePixel = satellitePixelTextField.getText().replace('-', ' ');
                Misc.run(new Runnable() {
                    public void run() {
                        setAvailableStations(true);
                    }
                });
            }
        });
    }
    
    /**
     * Tell the AddeChooser our name
     *
     * @return  The name
     */
    public String getDataName() {
        return "Sounding Data";
    }

    /**
     * Get the descriptor widget label.
     *
     * @return  label for the descriptor  widget
     */
    public String getDescriptorLabel() { 
        return "Soundings"; 
    }
    
    /**
     * get default display to create
     *
     * @return default display
     */
    protected String getDefaultDisplayType() {
        return "raob_skewt";
    }
    
    /**
     * Get the mandatory dataset name.
     *
     * @return mandatory dataset name
     */
    private String getMandatoryDataset() {
        if (getDescriptor() == null) return null;
        return getGroup() + "/" + getDescriptor();
    }

    /**
     * Get the sig level dataset name.
     *
     * @return sig level dataset name
     */
    private String getSigLevelDataset() {
        if (getDescriptor2() == null) return getMandatoryDataset();
        return getGroup() + "/" + getDescriptor2();
    }
    
    /**
     * Add a listener to the given combobox that will set the
     * state to unconnected.
     *
     * @param box The box to listen to.
     */
    protected void clearOnChange(final JComboBox box) {
        box.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ( !ignoreStateChangedEvents) {
                    setState(STATE_UNCONNECTED);
                    GuiUtils.setListData(descriptorComboBox, new Vector());
                    GuiUtils.setListData(descriptorComboBox2, new Vector());
                }
            }
        });
    }
    
    /**
     * Reset the descriptor stuff.
     */
    protected void resetDescriptorBox() {
        ignoreDescriptorChange = true;
        descriptorComboBox.setSelectedItem(LABEL_SELECT);
        if (descriptorComboBox2 != null) {
            descriptorComboBox2.setSelectedItem(LABEL_SELECT2);
            descriptorComboBox2.setEnabled(false);
        }
        ignoreDescriptorChange = false;
    }
    
    /**
     * Initialize the descriptor list from a list of names.
     *
     * @param names2 List of names.
     */
    protected void setDescriptors2(String[] names2) {
        synchronized (WIDGET_MUTEX) {
            ignoreDescriptorChange = true;
            descriptorComboBox2.removeAllItems();
            descriptorComboBox2.addItem(LABEL_SELECT2);
            descriptorNames2 = names2;
            if ((names2 == null) || (names2.length == 0)) {
                ignoreDescriptorChange = false;
                return;
            }
            for (int j = 0; j < names2.length; j++) {
                descriptorComboBox2.addItem(names2[j]);
            }
            ignoreDescriptorChange = false;
        }
    }
    
    /**
     * Get the selected descriptor.
     *
     * @return  the currently selected descriptor.
     */
    protected String getDescriptor2() {
        if (descriptorTable2 == null) {
            return null;
        }
        String selection = (String) descriptorComboBox2.getSelectedItem();
        if (selection == null) {
            return null;
        }
        if (selection.equals(LABEL_SELECT2)) {
            return null;
        }
        if (!selection.contains(nameSeparator)) {
            return (String)descriptorTable2.get(selection);
        }
        else {
            String[] toks = selection.split(nameSeparator);
            String key = toks[1].trim();
            return (String)descriptorTable2.get(key);
        }
    }

    /**
     * Method to call if the server changed.
     */
    protected void connectToServer() {
        clearStations();
        setDescriptors2(null);
        super.connectToServer();
        setAvailableStations(true);
    }
    
    /**
     * Do we have times selected.
     * @return Do we have times
     */
    public boolean timesOk() {
        return haveTimeSelected();
    }

    /**
     * Are there any times selected.
     *
     * @return Any times selected.
     */
    protected boolean haveTimeSelected() {
        if (selectedObs!=null) {
            if (selectedObs.size() > 0) return true;
        }
        return false;
    }
    
    /**
     * Do nothing for read times...
     * doUpdateInner handles all of this with an AddeSoundingAdapter
     */
    public void readTimes() { }
    
    /**
     * Wrapper for sampleTimesInner
     * Starts in a new thread and handles UI updating
     */
    private void sampleTimes() {
        readSatelliteTask = startTask();
        enableWidgets();
        Misc.run(new Runnable() {
            public void run() {
                sampleTimesInner();
                if(stopTaskAndIsOk(readSatelliteTask)) {
                    readSatelliteTask = null;
                    GuiUtils.setListData(satelliteTimeComboBox, satelliteTimes);
                    revalidate();
                } else {
                    //User pressed cancel
                    setState(STATE_UNCONNECTED);
                }
            }
        });
        updateStatus();
    }
    
    /**
     * Different way of reading times... for satellite soundings, do the following:
     * PTLIST GROUP/DESCRIPTOR.Z SEL='ROW X; COL Y' PAR=TIME
     *   where Z starts at 0 (expect an error), then goes to 1 and increases monotonically in outer loop until error
     *     and X starts at 1 and increases monotonically in middle loop until error
     *       and Y starts at 1 and increases by 25000 or so in inner loop until error
     * This samples times across the dataset
     */
    private void sampleTimesInner() {
        if (getDescriptor()==null) return;
        showWaitCursor();
        int posMax = 9999;
        int rowMax = 9999;
        int colMax = 999999;
        int colSkip = 24000;
        int consecutiveFailures = 0;
        Map<String, String> acctInfo = getAccountingInfo();
        String user = acctInfo.get("user");
        String proj = acctInfo.get("proj");
        String appendUserProj = "";
        if (!(user.equals("") || proj.equals("")))
            appendUserProj += "&user=" + user + "&proj=" + proj;
        satelliteTimes = new ArrayList();
        for (int pos = 0; pos < posMax; pos++) {
            for (int row=1; row<rowMax; row++) {
                for (int col=1; col<colMax; col+=colSkip) {
                    
                    String[] paramString = new String[] {
                            "group", getGroup(), "descr", getDescriptor(), "param", "DAY TIME", "num", "1",
                            "pos", Integer.toString(pos),
                            "select", "'ROW " + row + "; COL " + col + "'"
                        };
                        String request = Misc.makeUrl("adde", getServer(), "/point", paramString);
                        request += appendUserProj;
                        try {
                            AddePointDataReader dataReader = new AddePointDataReader(request);
                            int[][] data = dataReader.getData();
                            if (data[0].length == 0) throw new Exception();
                            for (int i = 0; i < data[0].length; i++) {
                                int day = data[0][i];
                                int time = data[1][i];
                                DateTime dt = new DateTime(McIDASUtil.mcDayTimeToSecs(day, time));
                                String timeString = dt.timeString().substring(0,5);
                                if (satelliteTimes.indexOf(timeString) < 0) {
                                    satelliteTimes.add(timeString);
                                }                               
                            }
                            // Reset consecutive failure count when you get good data
                            consecutiveFailures=0;
                        }
                        catch (Exception e) {
                                                                    
                            // We are at the beginning of a position
                            // Log a failure and increment the position
                            if (col==1 && row==1) {
                                row=rowMax;
                                consecutiveFailures++;
                                // If we have failed a few times in a row, bail completely
                                if (consecutiveFailures > 2) {
                                    pos=posMax;
                                }
                            }
                            
                            // If we failed at the first column, increment the position
                            if (col==1) row=rowMax;

                            // We have an exception, increment the row
                            col = colMax;
                            
                        }
                }
            }
        }
        
        Collections.sort(satelliteTimes);
        showNormalCursor();
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
            descriptorTable2 = new Hashtable();
            
            if (!showAll.isSelected()) {
                // Filter out anything not Upper Air Mandatory or Significant
                for (Enumeration enumeration = descriptorTable.keys(); enumeration.hasMoreElements();) {
                    Object key = enumeration.nextElement();
                    String keyString = key.toString();
                    String descriptorString = descriptorTable.get(key).toString();
                    if (keyString.toUpperCase().indexOf("MAND") >= 0 || descriptorString.indexOf("MAND") >= 0) {
                        continue;
                    }
                    if (keyString.toUpperCase().indexOf("SIG") >= 0 || descriptorString.indexOf("SIG") >= 0) {
                        descriptorTable2.put(key, descriptorTable.get(key));
                        descriptorTable.remove(key);
                        continue;
                    }
                    if (keyString.toUpperCase().indexOf("UPPER AIR") >= 0 ||
                            descriptorString.indexOf("UPPER") >= 0 ||
                            descriptorString.indexOf("UPPR") >= 0) {
                        descriptorTable2.put(key, descriptorTable.get(key));
                        continue;
                    }
                    if (keyString.toUpperCase().indexOf("SOUNDER") >= 0 ||
                            descriptorString.indexOf("SND") >= 0 ||
                            descriptorString.indexOf("SNDR") >= 0) {
                        descriptorTable2.put(key, descriptorTable.get(key));
                        continue;
                    }
                    if (keyString.toUpperCase().indexOf("GRET") >= 0 || descriptorString.indexOf("GRET") >= 0) {
                        descriptorTable2.put(key, descriptorTable.get(key));
                        continue;
                    }
                    if (keyString.toUpperCase().indexOf("SRET") >= 0 || descriptorString.indexOf("SRET") >= 0) {
                        descriptorTable2.put(key, descriptorTable.get(key));
                        continue;
                    }
                    descriptorTable.remove(key);
                }
            }
            else {
                // We have been told to Show All... put all descriptors into both categories
                for (Enumeration enumeration = descriptorTable.keys(); enumeration.hasMoreElements();) {
                    Object key = enumeration.nextElement();
                    descriptorTable2.put(key, descriptorTable.get(key));
                }
            }
            
            String[]    names       = new String[descriptorTable.size()];            
            Enumeration enumeration = descriptorTable.keys();
            for (int i = 0; enumeration.hasMoreElements(); i++) {
                Object thisElement = enumeration.nextElement();
                if (!isLocalServer())
                    names[i] = descriptorTable.get(thisElement).toString() + nameSeparator + thisElement.toString();
                else
                    names[i] = thisElement.toString();
            }
            Arrays.sort(names);
            setDescriptors(names);
            
            String[]    names2       = new String[descriptorTable2.size()];
            Enumeration enumeration2 = descriptorTable2.keys();
            for (int i = 0; enumeration2.hasMoreElements(); i++) {
                Object thisElement2 = enumeration2.nextElement();
                if (!isLocalServer())
                    names2[i] = descriptorTable2.get(thisElement2).toString() + nameSeparator + thisElement2.toString();
                else
                    names2[i] = nameSeparator + thisElement2.toString();
            }
            Arrays.sort(names2);
            setDescriptors2(names2);
            
            setState(STATE_CONNECTED);
        } catch (Exception e) {
            handleConnectionError(e);
        }
    }
    
    /**
     * See if we are pointing to observed or satellite soundings
     */
    private void checkSetObsSat() {
        System.out.println("checkSetObsSat: init");
        if (getServer() == null || getGroup() == null || getDescriptor() == null) return;
        System.out.println("checkSetObsSat: start");
        satelliteSounding = false;
        showWaitCursor();
        Map<String, String> acctInfo = getAccountingInfo();
        System.out.println("got acct info");
        String user = acctInfo.get("user");
        String proj = acctInfo.get("proj");
        String[] paramString = new String[] {
            "group", getGroup(), "descr", getDescriptor(), "param", "ZS", "num", "1", "pos", "all"
        };
        String request = Misc.makeUrl("adde", getServer(), "/point", paramString);
        if (!(user.equals("") || proj.equals("")))
            request += "&user=" + user + "&proj=" + proj;
        System.out.println("Making request: " + request);
        try {
            AddePointDataReader dataReader = new AddePointDataReader(request);
        }
        catch (Exception e) {
            if (e.getMessage().indexOf("Accounting data") >= 0) handleConnectionError(e);
            else satelliteSounding = true;
        }
        
        showNormalCursor();
        System.out.println("checkSetObsSat: done: " + satelliteSounding);
    }
    
    /**
     * Override clearStations to clear times as well
     */
    protected void clearStations() {
        super.clearStations();
        clearTimes();
    }
    
    /**
     * Remove all times from the user lists
     */
    protected void clearTimes() {
        if (obsList!=null) obsList.setListData(new Vector());
        if (timesList!=null) timesList.setListData(new Vector());
    }
    
    /**
     * Update labels, etc.
     */
    protected void updateStatus() {
        super.updateStatus();
        if (getState() != STATE_CONNECTED) {
            resetDescriptorBox();
            clearStations();
        }
        else {
            if (getDescriptor() == null) {
                if (descriptorComboBox2 != null) {
                    descriptorComboBox2.setSelectedItem(LABEL_SELECT2);
                }
                clearStations();
                setStatus("Select mandatory levels dataset");
                return;
            }
        }
        if (readSatelliteTask!=null) {
            if(taskOk(readSatelliteTask)) {
                setStatus("Reading sounding info from server");
            } else {
                readSatelliteTask  = null;
                setState(STATE_UNCONNECTED);
            }
        }
        if (readStationTask!=null) {
            if(taskOk(readStationTask)) {
                setStatus("Reading available stations from server");
            } else {
                readStationTask  = null;
                setState(STATE_UNCONNECTED);
            }
        }
        enableWidgets();
    }
    
    /**
     * Overwrite base class method to create the station map
     * with the appropriate properties.
     *
     * @return The new station map
     */
    protected StationLocationMap createStationMap() {
        return new StationLocationMap(true) {
            public void setDeclutter(boolean declutter) {
                super.setDeclutter(declutter);
                updateStatus();
            }
        };
    }
    
    /**
     * Initialize the stations
     *
     * @param stationMap The station map
     */
    protected void initStationMap(StationLocationMap stationMap) {
        CompositeRenderer renderer = new CompositeRenderer();
        renderer.addRenderer(new McidasMap("/auxdata/maps/OUTLSUPW"));
        renderer.addRenderer(new McidasMap("/auxdata/maps/OUTLSUPU"));
        renderer.setColor(MAP_COLOR);
        stationMap.setMapRenderer(renderer);

        stationMap.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent pe) {
                if (pe.getPropertyName().equals(
                        StationLocationMap.SELECTED_PROPERTY)) {
                    stationSelected((Station) pe.getNewValue());
                } else if (pe.getPropertyName().equals(
                        StationLocationMap.UNSELECTED_PROPERTY)) {
                    stationUnselected((Station) pe.getNewValue());
                } else if (pe.getPropertyName().equals(
                    StationLocationMap.ALL_UNSELECTED_PROPERTY)) {
                    unselectAll();
                }
            }
        });

    }

    /**
     * Handle a station selection
     *
     * @param station  selected station
     */
    private void stationSelected(Station station) {
        List selectedTimes = getSelectedTimes();
        if ((selectedTimes == null) || (selectedTimes.size() < 1)) {
            return;
        }
        for (int i = 0; i < selectedTimes.size(); i++) {
            DateTime dt = (DateTime) selectedTimes.get(i);
            List times =
                soundingAdapter.getSoundingTimes((SoundingStation) station);
            if ((times != null) && (times.size() > 0)) {
                if (times.contains(dt)) {
                    SoundingOb newObs = new SoundingOb((SoundingStation)station, dt);
                    if ( !selectedObs.contains(newObs)) {
                        selectedObs.add(newObs);
                    }
                }
            }
        }
        obsList.setListData(selectedObs);
        updateStatus();
    }

    /**
     * Unselect a station
     *
     * @param station  station to unselect
     */
    private void stationUnselected(Station station) {
        List selectedTimes = getSelectedTimes();
        if ((selectedTimes == null) || (selectedTimes.size() < 1)) {
            return;
        }
        for (int i = 0; i < selectedTimes.size(); i++) {
            SoundingOb newObs = new SoundingOb((SoundingStation)station,
                                    (DateTime) selectedTimes.get(i));
            if (selectedObs.contains(newObs)) {
                selectedObs.remove(newObs);
            }
        }
        obsList.setListData(selectedObs);
        updateStatus();
    }
    
    /**
     * Unselect all station
     */
    private void unselectAll() {
        List selectedTimes = getSelectedTimes();
        if ((selectedTimes == null) || (selectedTimes.size() < 1)) {
            return;
        }
        selectedObs.removeAllElements();
        obsList.setListData(selectedObs);
        updateStatus();
    }

    /**
     *  This looks in the selectedList of SoundingOb-s for all stations
     *  that are selected for the current time. It creates and returns
     *  a list of the Station-s held by these current SoundingOb-s
     *
     * @return list of currently selected stations
     */
    // Question: why does this care about current time?
    //           more than one time can be selected...
    private List getCurrentSelectedStations() {
        List     current     = new ArrayList();
//        DateTime currentTime = getSelectedTime();
        for (int i = 0; i < selectedObs.size(); i++) {
            SoundingOb ob = (SoundingOb) selectedObs.get(i);
//            if (ob.getTimestamp().equals(currentTime)) {
                current.add(ob.getStation());
//            }
        }
        return current;
    }
    
    /**
     * Get the current list of stations that are selected
     */
    private void setStations() {
        stationMap.setStations(soundingAdapter.getStations(),
                getCurrentSelectedStations(), stationMap.getDeclutter());
        stationMap.redraw();
    }

    /**
     * Set the SoundingAdapter used by this selector
     *
     * @param newAdapter   new adapter
     */
    protected void setSoundingAdapter(AddeSoundingAdapter newAdapter) {
        soundingAdapter = newAdapter;
        selectedObs.removeAllElements();
        obsList.setListData(selectedObs);
        setStations();
        setTimesListData(null);
        updateStatus();
    }

    /**
     * Set the data in the times list
     *
     * @param selected  a list of times that should be selected
     */
    private void setTimesListData(List selected) {
        if (soundingAdapter==null) return;
        DateTime[] times = soundingAdapter.getSoundingTimes();
        if (times != null) {
            timesList.setListData(times);
            if ((selected != null) && (selected.size() > 0)) {
                ListModel lm      = timesList.getModel();
                int[]     indices = new int[times.length];
                int       l       = 0;
                for (int i = 0; i < lm.getSize(); i++) {
                    if (selected.contains(lm.getElementAt(i))) {
                        indices[l++] = i;
                    }
                }
                if (l > 0) {
                    int[] selectedIndices = new int[l];
                    System.arraycopy(indices, 0, selectedIndices, 0, l);
                    timesList.setSelectedIndices(selectedIndices);
                    timesList.ensureIndexIsVisible(selectedIndices[l - 1]);
                } else {
                    timesList.setSelectedValue(times[times.length - 1], true);
                }
            } else if (times.length > 0) {
                timesList.setSelectedValue(times[times.length - 1], true);
            }
        } else {
            LogUtil.userMessage("No data available");
        }
    }

    /**
     * Get the selected time.
     *
     * @return the time selected in the list
     */
    public DateTime getSelectedTime() {
        return (DateTime)timesList.getSelectedValue();
    }

    /**
     * Get the selected time.
     *
     * @return the time selected in the list
     */
    public List getSelectedTimes() {
        return timesList.getSelectedValuesList();
    }
    
    /**
     * Create the list of times.
     *
     * @return List of times
     */
    private JList createTimesList() {
        timesList = new JList();
        timesList.setSelectionMode(
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        timesList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if ( !timesList.isSelectionEmpty()
                        && !e.getValueIsAdjusting()) {
                    newTimes(timesList.getSelectedValuesList());
                }
            }
        });
        return timesList;
    }
    
    /**
     * Set the new times
     *
     * @param times new times to use
     */
    private void newTimes(List times) {
        if (stationMap == null) return;
        List current = stationMap.getSelectedStations();
        if ((current == null) || (current.size() < 1)) {
            return;
        }
        selectedObs.removeAllElements();
        for (int i = 0; i < times.size(); i++) {
            DateTime dt = (DateTime) times.get(i);
            for (int j = 0; j < current.size(); j++) {
                SoundingStation ss      = (SoundingStation) current.get(j);
                List            ssTimes =
                    soundingAdapter.getSoundingTimes(ss);
                if ((ssTimes != null) && (times.size() > 0)) {
                    if (ssTimes.contains(dt)) {
                        SoundingOb newObs = new SoundingOb(ss, dt);
                        if ( !selectedObs.contains(newObs)) {
                            selectedObs.add(newObs);
                        }
                    }
                }
            }
        }
        obsList.setListData(selectedObs);
        updateStatus();
    }

    /**
     * Get the selected soundings
     *
     * @return List of selected soundings
     */
    public List getSelectedSoundings() {
        return selectedObs;
    }
    
    /**
     * Handle the selection of an ob
     *
     * @param event  MouseEvent for selection
     */
    private void obsListClicked(MouseEvent event) {
        if ( !SwingUtilities.isRightMouseButton(event)) {
            return;
        }
        int index = obsList.locationToIndex(new Point(event.getX(),
                        event.getY()));
        if ((index < 0) || (index >= selectedObs.size())) {
            return;
        }

        final SoundingOb obs   = (SoundingOb) selectedObs.get(index);

        JPopupMenu       popup = new JPopupMenu();
        JMenuItem        mi;

        mi = new JMenuItem("Remove " + obs);
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedObs.remove(obs);
                obsList.setListData(selectedObs);
                updateStatus();
                stationMap.setSelectedStations(getCurrentSelectedStations());
            }
        });

        popup.add(mi);

        mi = new JMenuItem("Remove all");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedObs.removeAllElements();
                obsList.setListData(selectedObs);
                updateStatus();
                stationMap.setSelectedStations(getCurrentSelectedStations());
            }
        });

        popup.add(mi);

        popup.show(obsList, event.getX(), event.getY());
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
     * Enable or disable the GUI widgets based on what has been
     * selected.
     */
    protected void enableWidgets() {
        super.enableWidgets();
        boolean readingTask = (readSatelliteTask!=null || readStationTask!=null);
        if (mainHoursCbx != null) mainHoursCbx.setVisible(!satelliteSounding);
        if (descriptorComboBox2 != null) {
            if (satelliteSounding) setDescriptors2(null);
            descriptorComboBox2.setVisible(!satelliteSounding);
            descriptorComboBox2.setEnabled(!readingTask &&
                    descriptorComboBox.getSelectedIndex() > 0);
        }
        if (satelliteTimePanel!=null) {
            satelliteTimePanel.setVisible(satelliteSounding);
            GuiUtils.enableTree(satelliteTimePanel, !readingTask);
            if (satelliteSounding)
                satelliteTimeLabel.setText("Time:");
            else
                satelliteTimeLabel.setText("");
        }
        if (showAll!=null) showAll.setEnabled(!readingTask);
    }
    
    /**
     * Respond to a change in the descriptor list.
     */
    protected void descriptorChanged() {
        descriptorChanged(true);
    }
    
    /**
     * Respond to a change in the descriptor list.
     */
    protected void descriptorChanged(final boolean checkObsSat) {
        satelliteSounding = false;
        readSatelliteTask = startTask();
        enableWidgets();
        Misc.run(new Runnable() {
            public void run() {
                if (checkObsSat) checkSetObsSat();
                setAvailableStations(true);
                updateStatus();
                if(stopTaskAndIsOk(readSatelliteTask)) {
                    readSatelliteTask = null;
                    updateStatus();
                    revalidate();
                } else {
                    //User pressed cancel
                    setState(STATE_UNCONNECTED);
                }
            }
        });
        updateStatus();
    }

    /**
     *  Update the station map with available stations.
     */
    private void setAvailableStations(final boolean forceNewAdapter) {
        if (getMandatoryDataset() == null) {
            updateStatus();
            return;
        }
        showWaitCursor();
        readStationTask = startTask();
        clearSelectedStations();
        updateStatus();
        doUpdateInner(forceNewAdapter);
        if(stopTaskAndIsOk(readStationTask)) {
            readStationTask = null;
            updateStatus();
            revalidate();
        } else {
            //User pressed cancel
            setState(STATE_UNCONNECTED);
        }
        showNormalCursor();
    }

    
    /**
     * Really update station map.
     *
     * @param forceNewAdapter If true then create a new adapter.
     *                        Else, tell the existing one to update.
     */
    private void doUpdateInner(final boolean forceNewAdapter) {
        try {
            if (forceNewAdapter || soundingAdapter == null) {
                AddeSoundingAdapter newAdapter;
                if (!satelliteSounding) {
                    newAdapter = new AddeSoundingAdapter(getServer(),
                        getMandatoryDataset(),
                        getSigLevelDataset(),
                        showMainHoursOnly,
                        this);
                }
                else {
                    newAdapter = new AddeSoundingAdapter(getServer(),
                        getMandatoryDataset(),
                        getSigLevelDataset(),
                        satelliteTime,
                        satellitePixel,
                        this);
                }
                soundingAdapter = null;
                setSoundingAdapter(newAdapter);
            } else {
                List times = getSelectedTimes();
                soundingAdapter.update();
                setStations();
                setTimesListData(times);
            }
        } catch (Exception exc) {
            LogUtil.logException("Updating sounding data", exc);
        }
    }
    
    /**
     * Load the data source in a thread
     */
    public void doLoadInThread() {
        List soundings = getSelectedSoundings();
        if (soundings.size() == 0) {
            userMessage("Please select one or more soundings.");
            return;
        }
        Hashtable ht = new Hashtable();
        getDataSourceProperties(ht);

        makeDataSource(new RaobDataSet(soundingAdapter, soundings), DATA_TYPE, ht);
        saveServerState();
    }
    
    /**
     * Add the times selector to the component.
     * @return superclass component with extra stuff
     */
    protected JPanel makeTimesPanel() {
        
        // Make the 0 & 12 checkbox
        mainHoursCbx = new JCheckBox("00 & 12Z only", showMainHoursOnly);
        mainHoursCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                showMainHoursOnly = ((JCheckBox) ev.getSource()).isSelected();
                Misc.run(new Runnable() {
                    public void run() {
                        setAvailableStations(true);
                    }
                });
            }
        });

        // Make the select panel
        JScrollPane availablePanel = new JScrollPane(createTimesList());
        availablePanel.setPreferredSize(new Dimension(175, 50));
        JPanel selectPanel = GuiUtils.centerBottom(availablePanel, mainHoursCbx);
        selectPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Available"));
        
        // Make the selected panel
        obsList = new JList();
        obsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                obsListClicked(e);
            }
        });
        JScrollPane selectedPanel = new JScrollPane(obsList);
        selectedPanel.setPreferredSize(new Dimension(175, 50));
        selectedPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected"));
        
        // Make the container panel
        JPanel timesPanel = new JPanel();
        selectPanel.setBackground(timesPanel.getBackground());
        selectedPanel.setBackground(timesPanel.getBackground());
        
        GroupLayout layout = new GroupLayout(timesPanel);
        timesPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
//                .addContainerGap()
                .addComponent(selectPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(GAP_RELATED)
                .addComponent(selectedPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                )
//                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
//                .addContainerGap()
                .addGroup(layout.createParallelGroup(TRAILING)
                    .addComponent(selectedPanel, LEADING, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(selectPanel, LEADING, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                    )
//                .addContainerGap())
        );
        
        JComponent temp = super.makeTimesPanel();
        temp.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        McVGuiUtils.setComponentHeight(timesPanel, temp);

        return timesPanel;
    }
    
    /**
     * Make the UI for this selector.
     * 
     * @return The gui
     */   
    public JComponent doMakeContents() {
        JPanel myPanel = new JPanel();
        
        McVGuiUtils.setComponentWidth(descriptorComboBox, Width.DOUBLEDOUBLE);
        McVGuiUtils.setComponentWidth(descriptorComboBox2, descriptorComboBox);
        McVGuiUtils.setComponentWidth(satelliteTimeComboBox, Width.DOUBLE);
        McVGuiUtils.setComponentWidth(satellitePixelTextField, Width.DOUBLE);
        
        satelliteTimePanel = McVGuiUtils.sideBySide(
                McVGuiUtils.sideBySide(satelliteTimeComboBox, satelliteTimeButton),
                McVGuiUtils.makeLabeledComponent("IDN:", satellitePixelTextField)
                );
        satelliteTimePanel.setVisible(false);

        JPanel extraPanel = McVGuiUtils.sideBySide(
                GuiUtils.left(McVGuiUtils.sideBySide(descriptorComboBox2, satelliteTimePanel, 0)),
                GuiUtils.right(showAll));
        
//      McVGuiUtils.setComponentWidth(extraPanel, descriptorComboBox);
        
        JLabel stationLabel = McVGuiUtils.makeLabelRight("Stations:");
        addServerComp(stationLabel);

        JComponent stationPanel = getStationMap();
        registerStatusComp("stations", stationPanel);
//        addServerComp(stationPanel);
        addDescComp(stationPanel);
        
        JLabel timesLabel = McVGuiUtils.makeLabelRight("");
//        addServerComp(timesLabel);
        addDescComp(timesLabel);
        
        JPanel timesPanel = makeTimesPanel();
//        timesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
//        addServerComp(timesPanel);
        addDescComp(timesPanel);
        
        enableWidgets();
        updateStatus();

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
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(satelliteTimeLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(extraPanel))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(stationLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(stationPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(timesLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(descriptorLabel)
                    .addComponent(descriptorComboBox))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(satelliteTimeLabel)
                    .addComponent(extraPanel))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(stationLabel)
                    .addComponent(stationPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(timesLabel)
                    .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(RELATED))
        );

        
        
        
        
        setInnerPanel(myPanel);
        return super.doMakeContents(true);
    }
    
}
