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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.w3c.dom.Element;

import ucar.unidata.data.sounding.RaobDataSet;
import ucar.unidata.data.sounding.SoundingAdapter;
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
import visad.DateTime;
import edu.wisc.ssec.mcidasv.data.AddeSoundingAdapter;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

/**
 * A chooser class for selecting Raob data.
 * Mostly just a wrapper around a
 *  {@link ucar.unidata.view.sounding.SoundingSelector}
 * that does most of the work
 *
 * @author IDV development team
 * @version $Revision$Date: 2008/10/30 19:38:24 $
 */


public class AddeRaobChooser extends AddePointDataChooser {
	
    /** Property for the data type. */
    public static String DATA_TYPE = "RAOB";
	
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
    SoundingAdapter soundingAdapter;
    
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
        
        descriptorsAllow = new String[] {
        		"UPPERMAND", "UPPERSIG"
        };

        descriptorsDeny = new String[] { };

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
        return "Sounding Type"; 
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
        return getGroup() + "/UPPERMAND";
    }

    /**
     * Get the sig level dataset name.
     *
     * @return sig level dataset name
     */
    private String getSigLevelDataset() {
        return getGroup() + "/UPPERSIG";
    }
    
    /**
     * We don't require the user to select a descriptor, so always return true
     */
    protected boolean haveDescriptorSelected() {
    	return true;
    }
    
    /**
     * Method to call if the server changed.
     */
    protected void connectToServer() {
        clearStations();
        super.connectToServer();
        setAvailableStations(false);
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
    protected void readTimes() { }
    
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
                    SoundingOb newObs = new SoundingOb(station, dt);
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
            SoundingOb newObs = new SoundingOb(station,
                                    (DateTime) selectedTimes.get(i));
            if (selectedObs.contains(newObs)) {
                selectedObs.remove(newObs);
            }
        }
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
    protected void setSoundingAdapter(SoundingAdapter newAdapter) {
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
        return (DateTime) timesList.getSelectedValue();
    }

    /**
     * Get the selected time.
     *
     * @return the time selected in the list
     */
    public List getSelectedTimes() {
        return Misc.toList(timesList.getSelectedValues());
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
                    Object[] t = timesList.getSelectedValues();
                    newTimes(Misc.toList(t));
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
     *  Update the station map with available stations.
     */
    private void setAvailableStations(final boolean forceNewAdapter) {
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
            return;
        }
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
    			AddeSoundingAdapter newAdapter =
    				new AddeSoundingAdapter(getServer(),
    						getMandatoryDataset(),
    						getSigLevelDataset(),
    						showMainHoursOnly);
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
    	JCheckBox mainHoursCbx = new JCheckBox("00 & 12Z only", showMainHoursOnly);
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
        
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(timesPanel);
        timesPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
//                .addContainerGap()
                .add(selectPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(GAP_RELATED)
                .add(selectedPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                )
//                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
//                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, selectedPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, selectPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    )
//                .addContainerGap())
        );
        
        JComponent temp = super.makeTimesPanel();
        temp.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        McVGuiUtils.setMatchHeight(timesPanel, temp);

        return timesPanel;
    }
    
    /**
     * Make the UI for this selector.
     * 
     * @return The gui
     */   
    public JComponent doMakeContents() {
    	JPanel myPanel = new JPanel();
    	
    	JLabel descriptorLabelStatic = McVGuiUtils.makeLabelRight("Soundings:");
    	JLabel descriptorString = new JLabel("Upper air mandatory and significant levels");
    	
        JLabel stationLabel = McVGuiUtils.makeLabelRight("Stations:");
        addServerComp(stationLabel);

        JComponent stationPanel = getStationMap();
        registerStatusComp("stations", stationPanel);
        addServerComp(stationPanel);
        
        JLabel timesLabel = McVGuiUtils.makeLabelRight("");
        addServerComp(timesLabel);
        
        JPanel timesPanel = makeTimesPanel();
//        timesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        addServerComp(timesPanel);
        
        enableWidgets();
        updateStatus();

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(descriptorLabelStatic)
                        .add(GAP_RELATED)
                        .add(descriptorString))
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
                    .add(descriptorLabelStatic)
                    .add(descriptorString))
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