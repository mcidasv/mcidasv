/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
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

package edu.wisc.ssec.mcidasv.chooser.adde;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import edu.wisc.ssec.mcidas.McIDASUtil;
import edu.wisc.ssec.mcidas.adde.AddePointDataReader;
import edu.wisc.ssec.mcidas.adde.DataSetInfo;
import edu.wisc.ssec.mcidasv.ui.JCalendarDateEditor;
import edu.wisc.ssec.mcidasv.ui.JCalendarPicker;
import edu.wisc.ssec.mcidasv.ui.JTimeRangePicker;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;

import ucar.unidata.data.DataSelection;
import ucar.unidata.data.AddeUtil;
import ucar.unidata.data.point.AddePointDataSource;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.adde.AddeServer;
import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.ui.symbol.StationModelManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.visad.UtcDate;

import visad.DateTime;
import visad.VisADException;

/**
 * Selection widget for ADDE point data
 * 
 * @version $Revision$ $Date$
 */

public class AddePointDataChooser extends AddeChooser {

    private static final long serialVersionUID = 1L;

    /** Logging object. Use it! */
    private static final Logger logger = LoggerFactory.getLogger(AddePointDataChooser.class);

    /**
     * Property for the dataset name key.
     * @see edu.wisc.ssec.mcidasv.chooser.adde.AddeChooser#getDataSetName()
     */
    public static String DATASET_NAME_KEY = "name";

    /** Property for the data type. */
    public static String DATA_TYPE = "ADDE.POINT.V";

    /** Are we currently reading times */
    private Object readTimesTask;
    
    /** box and label for the relative time */
    protected JLabel relTimeIncLabel;
    protected JComboBox relTimeIncBox;

    /** the relative time increment */
    private float relativeTimeIncrement = 1.0f;

    /** Date will default to current */
    DateTime dt = null;

    /** archive day button and label */
    protected JLabel archiveDayLabel;
    protected JButton archiveDayBtn;

    /** archive date formatter */
    private SimpleDateFormat archiveDayFormatter;

    /** station model manager */
    private StationModelManager stationModelManager;
    
    /** allowed descriptor prefix */
    protected String descriptorsAllowPrefix = "";
        
    protected boolean firstTime = true;
    protected boolean retry = true;
    
    /** we reset the retry flag any time remote server changes */
    protected String previousServer = "";

    /** Possibly ask for times a second time if the first sampling doesn't get any */
    private boolean gotObs = false;
    protected boolean tryWithoutSampling = false;

    /** Julian Date formatter */
    private static SimpleDateFormat jdFormat = new SimpleDateFormat("yyyyDDD");
        
    /**
     * Create a chooser for ADDE POINT data
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */

    public AddePointDataChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
                                
        this.stationModelManager = getIdv().getStationModelManager();

        relTimeIncLabel = new JLabel(" Interval:");
        relTimeIncBox = new JComboBox();
        relTimeIncBox.setToolTipText("Set the increment between relative times");
        relTimeIncBox.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent ae) {
                JComboBox box = (JComboBox) ae.getSource();
                if (GuiUtils.anySelected(box)) {
                    setRelativeTimeIncrement(getRelBoxValue());
                }
            }
        });
        McVGuiUtils.setComponentWidth(relTimeIncBox, Width.ONEHALF);
        
        descriptorsAllowPrefix = "";
        
        try {
            dt = new DateTime();
        } catch (VisADException vade) {
            vade.printStackTrace();
        }

        archiveDayBtn = new JButton("Set Day");
        archiveDayBtn.addActionListener(e -> getArchiveDay());
        archiveDayBtn.setToolTipText("Select a specific day");

        // Initialize time range to full day
        archiveBegTime = "00:00:00";
        archiveEndTime = "23:59:59";

        archiveDayLabel = new JLabel("Select day:");
        archiveDayFormatter = new SimpleDateFormat(UtcDate.IYD_FORMAT);
    }
    
    /**
     * Do server connection stuff... override this with type-specific methods
     */
    @Override protected void readFromServer() {
        if (archiveDayLabel != null) {
            archiveDayLabel.setText("Select day:");
        }
        super.readFromServer();
    }

    /**
     *  Generate a list of image descriptors for the descriptor list.
     */
    @Override protected void readDescriptors() {
        try {

            String currentServer = this.getServer();
            logger.info("Current server: " + currentServer);
            logger.info("Previous server: " + previousServer);
            if (! currentServer.equals("previousServer")) {
                // Reset retry flag to force archive servers to throw up date picker
                logger.info("Resetting RETRY flag");
                retry = true;
            }
            previousServer = currentServer;

            StringBuffer buff = getGroupUrl(REQ_DATASETINFO, getGroup());
            buff.append("&type=").append(getDataType());
            DataSetInfo dsinfo = new DataSetInfo(buff.toString());
            descriptorTable = dsinfo.getDescriptionTable();
            
            // Only show descriptorsAllowPrefix if set
            for (Enumeration e = descriptorTable.keys(); e.hasMoreElements();) {
                Object key = e.nextElement();
                String str = (String)descriptorTable.get(key);
                if (!descriptorsAllowPrefix.isEmpty() && str.indexOf(descriptorsAllowPrefix) != 0) {
                    descriptorTable.remove(key);
                }
            }
            
            String[] names = new String[descriptorTable.size()];
            Enumeration enumeration = descriptorTable.keys();
            for (int i = 0; enumeration.hasMoreElements(); i++) {
                Object thisElement = enumeration.nextElement();
                if (!isLocalServer()) {
                    names[i] = descriptorTable.get(thisElement).toString() + nameSeparator + thisElement.toString();
                } else {
                    names[i] = thisElement.toString();
                }
            }
            Arrays.sort(names);
            setDescriptors(names);
            setState(STATE_CONNECTED);
        } catch (Exception e) {
            handleConnectionError(e);
        }
    }

    /**
     * Load in an ADDE point data set based on the {@code PropertyChangeEvent}.
     */
    @Override public void doLoadInThread() {
        showWaitCursor();
        try {
            StationModel selectedStationModel = getSelectedStationModel();
            String source = getRequestUrl();

            // make properties Hashtable to hand the station name
            // to the AddeProfilerDataSource
            Hashtable ht = new Hashtable();
            getDataSourceProperties(ht);
            ht.put(DataSelection.PROP_CHOOSERTIMEMATCHING, getDoTimeDrivers());
            ht.put(AddePointDataSource.PROP_STATIONMODELNAME,
                   selectedStationModel.getName());
            ht.put(DATASET_NAME_KEY, getDescriptor());
            ht.put(DATA_NAME_KEY, getDataName());
            if (source.contains(AddeUtil.RELATIVE_TIME)) {
                ht.put(AddeUtil.NUM_RELATIVE_TIMES, getRelativeTimeIndices());
                ht.put(AddeUtil.RELATIVE_TIME_INCREMENT, getRelativeTimeIncrement());
            }

            if (getDoAbsoluteTimes()) {
              ht.put(AddeUtil.ABSOLUTE_TIMES, getSelectedAbsoluteTimes());
            }

            makeDataSource(source, DATA_TYPE, ht);
            saveServerState();
        } catch (Exception excp) {
            logException("Unable to open ADDE point dataset", excp);
        }
        showNormalCursor();
        // uncheck the check box every time click the add source button
        drivercbx.setSelected(false);
        enableTimeWidgets();
        setDoTimeDrivers(false);
    }
        
    /**
     * Show the archive dialog. This method is not meant to be called but is
     * public by reason of implementation (or insanity).
     */
    public void getArchiveDay() {

        final JDialog dialog = GuiUtils.createDialog("Set Day and Time Range", true);
        final JCalendarPicker picker = new JCalendarPicker(false);
        final JTimeRangePicker trp = new JTimeRangePicker();

        if (archiveDay != null) {
            if (archiveDayFormatter == null) {
                archiveDayFormatter = new SimpleDateFormat(UtcDate.YMD_FORMAT);
            }
            Date d = null;
            try {
                d = archiveDayFormatter.parse(archiveDay);
                picker.setDate(d);
            } catch (Exception e) {
                logException("parsing archive day " + archiveDay, e);
            }
        }

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_OK)) {

                    // bad time range, throw up error window
                    if (! trp.timeRangeOk()) {
                        String msg = "Time range is invalid.\n" +
                                     "Please provide valid hours, minutes and\n" +
                                     "seconds, with End Time > Start Time.";
                        Object[] params = { msg };
                        JOptionPane.showMessageDialog(null, params, "Invalid Time Range", JOptionPane.OK_OPTION);
                        return;
                    } else {
                        archiveBegTime = trp.getBegTimeStr();
                        archiveEndTime = trp.getEndTimeStr();
                    }
                    try {
                        String pickerDate = picker.getUserSelectedDay();
                        LocalDate ld = LocalDate.parse(pickerDate);
                        dt = new DateTime(ld.toDate());
                        archiveDay = jdFormat.format(ld.toDate());
                        archiveDayBtn.setText(pickerDate);
                    } catch (Exception e) {
                    }

                    setDoAbsoluteTimes(true);
                    clearTimesList();
                    descriptorChanged();
                }
                dialog.dispose();
            }
        };

        final JCalendarDateEditor dateEditor =
            (JCalendarDateEditor)picker.getDateChooser().getDateEditor();
        dateEditor.getUiComponent().addKeyListener(new KeyListener() {
            @Override public void keyTyped(KeyEvent e) { }

            @Override public void keyPressed(KeyEvent e) { }

            @Override public void keyReleased(KeyEvent e) {
                if (!Color.RED.equals(dateEditor.getForeground())) {
                    KeyStroke stroke =
                        getKeyStroke(e.getKeyCode(), e.getModifiers());
                    if (stroke.getKeyCode() == KeyEvent.VK_ENTER) {
                        try {
                            String pickerDate = picker.getUserSelectedDay();
                            LocalDate ld = LocalDate.parse(pickerDate);
                            dt = new DateTime(ld.toDate());
                            archiveDay = jdFormat.format(ld.toDate());
                            archiveDayBtn.setText(pickerDate);
                        } catch (Exception ex) {
                            // nothing to do
                        }
                        setDoAbsoluteTimes(true);
                        descriptorChanged();
                        dialog.dispose();
                    }
                }
            }
        });

        JPanel buttons = GuiUtils.makeButtons(listener, new String[] {
                GuiUtils.CMD_OK, GuiUtils.CMD_CANCEL });

        JPanel dateTimePanel = new JPanel(new FlowLayout());
        dateTimePanel.add(picker);
        dateTimePanel.add(trp);

        JComponent contents = GuiUtils.topCenterBottom(GuiUtils.inset(GuiUtils
                .lLabel("Please select a day and optional time range for this dataset:"), 10), GuiUtils
                .inset(dateTimePanel, 10), buttons);
        Point p = new Point(200, 200);
        if (archiveDayBtn != null) {
            try {
                p = archiveDayBtn.getLocationOnScreen();
            } catch (IllegalComponentStateException ice) {
            }
        }
        dialog.setLocation(p);
        dialog.getContentPane().add(contents);
        dialog.pack();
        dialog.setVisible(true);
    }

    /**
     * Get the selected station model.
     *
     * @return StationModel to use: defined by defaultModels list in ctor
     */
    public StationModel getSelectedStationModel() {
        StationModel returnModel = null;
        if (isUpperAir()) {
            returnModel = this.stationModelManager.getStationModel("Observations>Upper Air");
        } else if (isSynoptic()) {
            returnModel = this.stationModelManager.getStationModel("Observations>SYNOP");
        } else {
            returnModel = this.stationModelManager.getStationModel("Observations>METAR");
        }
        return returnModel;
    }
    
    /**
     * Add the interval selector to the component.
     * @return superclass component with extra stuff
     */
    @Override protected JPanel makeTimesPanel() {
        JComponent extra1 = getExtraTimeComponentRelative();
        GuiUtils.enableTree(extra1, false);
        JComponent extra2 = getExtraTimeComponentAbsolute();
        JPanel timesPanel = super.makeTimesPanel(extra1, extra2);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(archiveDayBtn);
        underTimelistPanel.add(BorderLayout.CENTER, buttonPanel);
        return timesPanel;
    }
    
    /**
     * Get the extra time widget, but built in a different way.
     * Designed to be put into a GroupLayout
     *
     * @return Extra time widget
     */
    protected JComponent getExtraTimeComponentRelative() {
        TwoFacedObject[] intervals = { 
            new TwoFacedObject("30 minute", .5f),
            new TwoFacedObject("Hourly", 1f),
            new TwoFacedObject("Three hourly", 3f),
            new TwoFacedObject("Six hourly", 6f),
            new TwoFacedObject("12 hourly", 12f),
            new TwoFacedObject("24 hourly", 24f)
        };

        GuiUtils.setListData(relTimeIncBox, intervals);
        if (relTimeIncBox.getItemCount()>=2) relTimeIncBox.setSelectedIndex(1);
        
        return McVGuiUtils.makeLabeledComponent(relTimeIncLabel, relTimeIncBox, McVGuiUtils.Position.LEFT);
    }

    /**
     * Overridden in McIDAS-V to get a nicer set of interval combo box options.
     *
     * @return {@code JPanel} containing a label and the interval combo box.
     */
    @Override protected JComponent getExtraRelativeTimeComponent() {
        return getExtraTimeComponentRelative();
    }

    /**
     * Get the time popup widget
     * 
     * @return a widget for selecing the day
     */
    protected JComponent getExtraTimeComponentAbsolute() {
        return null;
//      return McVGuiUtils.makeLabeledComponent(archiveDayLabel, archiveDayBtn);
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
     * @return the request URL
     */
    public String getRequestUrl() {
        StringBuffer request = getGroupUrl(REQ_POINTDATA, getGroup());
        appendKeyValue(request, PROP_USER, getLastAddedUser());
        appendKeyValue(request, PROP_PROJ, getLastAddedProj());
        appendKeyValue(request, PROP_DESCR, getDescriptor());
        appendRequestSelectClause(request);
        appendKeyValue(request, PROP_NUM, "ALL");
        // TJJ Dec 2020 - looks like POS = 0 limits requests to current day, so
        // we should always use ALL, and let the rel/abs filters dictate what is
        // delivered?
        // appendKeyValue(request, PROP_POS, getDoRelativeTimes() ? "ALL" : "0");
        appendKeyValue(request, PROP_POS, "ALL");
        logger.info("Request URL: " + request.toString());
        return request.toString();
    }
        
    /**
     * Get the select clause for the ADDE request specific to this
     * type of data.
     *
     * @param buf The buffer to append to
     */

    protected void appendRequestSelectClause(StringBuffer buf) {
        StringBuilder selectValue = new StringBuilder(1024);
        selectValue.append('\'');
        selectValue.append(getDayTimeSelectString());
        // TODO: why is SFCHOURLY explicit here?  better way to do it?
        if ("SFCHOURLY".equalsIgnoreCase(getDescriptor())) {
            selectValue.append(";TYPE 0");
        }
        selectValue.append(';');

        if (isUpperAir()){
            selectValue.append(AddeUtil.LEVEL);
            selectValue.append(';');
        }
        // TJJ - This must be a placeholder macro for the URL
        selectValue.append(AddeUtil.LATLON_BOX);
        selectValue.append('\'');
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
    @Override protected void enableWidgets() {
        boolean descriptorState = ((getState() == STATE_CONNECTED)
                                   && canReadTimes());

        for (int i = 0; i < compsThatNeedDescriptor.size(); i++) {
            JComponent comp = (JComponent) compsThatNeedDescriptor.get(i);
            GuiUtils.enableTree(comp, descriptorState);
        }

        boolean timesOk = timesOk();
        
        // Require times to be selected
        GuiUtils.enableTree(loadButton, descriptorState && timesOk);

        checkTimesLists();
        
        enableAbsoluteTimesList(getDoAbsoluteTimes() && descriptorState);

        getRelativeTimesChooser().setEnabled( !getDoAbsoluteTimes()
                && descriptorState);

        if (drivercbx != null) {
//            logger.trace("set drivercbx={}", anyTimeDrivers() && descriptorState);
            drivercbx.setEnabled(anyTimeDrivers() && descriptorState);
        }

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
     * Return {@code true} if selected descriptor is for SYNOPTIC data.
     *
     * @return {@code true} iff {@link edu.wisc.ssec.mcidasv.chooser.adde.AddePointDataChooser#getDescriptor()}
     * is {@literal "SYNOP"}.
     */
    protected boolean isSynoptic() {
        return "SYNOP".equals(getDescriptor());
    }
    
    /**
     * Return {@code true} if selected descriptor is for upper air.
     *
     * @return {@code true} iff {@link edu.wisc.ssec.mcidasv.chooser.adde.AddePointDataChooser#getDescriptor()}
     * is {@literal "UPPERMAND"}.
     */
    protected boolean isUpperAir() {
        return "UPPERMAND".equals(getDescriptor());

    }
    
    /**
     * Return {@code true} if selected descriptor is for profiler.
     *
     * @return {@code true} iff {@link edu.wisc.ssec.mcidasv.chooser.adde.AddePointDataChooser#getDescriptor()}
     * is {@literal "PROF"}.
     */
    protected boolean isProfiler() {
        return "PROF".equals(getDescriptor());
    }
        
    /**
     * Update the widget with the latest data.
     *
     * @throws Exception On badness
     */
    @Override public void handleUpdate() throws Exception {
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
        // TJJ - I don't know what is up with these hardcoded CONUS-like
        // bounds, but finding it's best if I just leave them in
        if (! isUpperAir() && ! tryWithoutSampling) {
            appendKeyValue(buf, PROP_POS, "ALL");
            appendKeyValue(buf, PROP_SELECT, "'DAY " + getJulianDay() +
                    ";TIME " + archiveBegTime + " " + archiveEndTime +
                    ";LAT 38 42;LON 70 75'");
        }
        else {
            appendKeyValue(buf, PROP_SELECT, "'DAY " + getJulianDay() +
                    ";TIME " + archiveBegTime + " " + archiveEndTime +
                    ";LAT 38 42;LON 70 75'");
            appendKeyValue(buf, PROP_POS, "ALL");
        }
        if (getDoAbsoluteTimes()) {
            appendKeyValue(buf, PROP_NUM, "ALL");
        }

        appendKeyValue(buf, PROP_PARAM, "DAY TIME");
        return buf.toString();
    }
    
    /**
     * Get the current, or archive, if selected, Julian day as a String
     *
     * @return the julian day as a string (yyyyDDD)
     */

    private String getJulianDay() {
        return UtcDate.formatUtcDate(dt, "yyyyDDD");
    }

    /**
     * This allows derived classes to provide their own name for labeling, etc.
     *
     * @return  the dataset name
     */
    @Override public String getDataName() {
        return "Point Data";
    }
    
    /**
     * _more_
     */
    @Override public void doCancel() {
        readTimesTask = null;
        setState(STATE_UNCONNECTED);
        super.doCancel();
    }

    /** locking mutex */
    private final Object MUTEX = new Object();
    
    /**
     *  Read the set of image times available for the current server/group/type
     *  This method is a wrapper, setting the wait cursor and wrapping the
     *  call to {@link #readTimesInner()}; in a try/catch block
     */
    @Override public void readTimes() {
        clearTimesList();
        if (!canReadTimes()) {
            return;
        }
        Misc.run(new Runnable() {
            @Override public void run() {
                updateStatus();
                showWaitCursor();
                try {
                    gotObs = false;
                    tryWithoutSampling = false;
                    readTimesInner();
                    // Try again, this time not sampling by LAT/LON
                    if (haveDescriptorSelected() && !gotObs) {
                        tryWithoutSampling = true;
                        readTimesInner();
                    }
                } catch (Exception e) {
                    handleConnectionError(e);
                }
                showNormalCursor();
                updateStatus();
            }
        });
    }

    /**
     * Set the list of dates/times based on the image selection
     */
    protected void readTimesInner() {
        SortedSet<DateTime> uniqueTimes = Collections.synchronizedSortedSet(new TreeSet<DateTime>());

        readTimesTask = startTask();
        updateStatus();
        Object task = readTimesTask;
        try {
            AddePointDataReader apr = new AddePointDataReader(getTimesRequest());
            //Make sure no other loads are  occurred
            boolean ok = stopTaskAndIsOk(task);
            if (!Misc.equals(readTimesTask, task) || !ok) {
                return;
            }
            readTimesTask = null;

            synchronized (MUTEX) {
                int[][] data = apr.getData();
                String[] units = apr.getUnits();
                if ( !"CYD".equals(units[0]) || !"HMS".equals(units[1])) {
                    throw new Exception("can't handle date/time units");
                }
                int numObs = data[0].length;
                //System.out.println("found " + numObs + " obs");
                // loop through and find all the unique times
                try {
                    for (int i = 0; i < numObs; i++) {
                        DateTime dt =
                            new DateTime(McIDASUtil.mcDayTimeToSecs(data[0][i],
                                data[1][i]));
                        uniqueTimes.add(dt);
                    }
                } catch (Exception e) {
                    logger.error("problem building list of unique times", e);
                }
                //System.out.println(
                //      "found " + uniqueTimes.size() + " unique times");
                if (getDoAbsoluteTimes()) {
                    if (!uniqueTimes.isEmpty()) {
                        setAbsoluteTimes(new ArrayList<DateTime>(uniqueTimes));
                        getTimesList().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
                    }

                    //   Select the last n hours 
                    int selectedIndex = getAbsoluteTimes().size() - 1;
                    int firstIndex = Math.max(0, selectedIndex
                            - getDefaultRelativeTimeIndex());
                    if (selectedIndex >= 0)
                        setSelectedAbsoluteTime(selectedIndex, firstIndex);
                }
                if (numObs>0) {
                    gotObs = true;
                }
            }
            setState(STATE_CONNECTED);
        } catch (Exception excp) {
            stopTask(task);
            readTimesTask = null;
            handleConnectionError(excp);
            if (! retry) {
                return;
            }
            try {
                handleUpdate();
            } catch (Exception e) {
                logger.error("problem handling update", e);
            }
        }
    }

    /**
     * Show the given error to the user. If it was an Adde exception
     * that was a bad server error then print out a nice message.
     *
     * @param e The exception
     */

    @Override protected void handleConnectionError(Exception e) {
        if (retry) {
            // initialize Archive Date to PREVIOUS DAY (archive server)
            LocalDate ld = LocalDate.now();
            ld = ld.minusDays(1);
            archiveDay = jdFormat.format(ld.toDate());
            logger.info("setting archiveDay to: " + archiveDay);
            getArchiveDay();
            retry = false;
            return;
        }
        super.handleConnectionError(e);
    }

    /**
     * Are there any times selected.
     *
     * @return Any times selected.
     */
    @Override protected boolean haveTimeSelected() {
        return !getDoAbsoluteTimes() || getHaveAbsoluteTimesSelected();
    }

    /**
     * Create the date time selection string for the "select" clause
     * of the ADDE URL.
     *
     * @return the select day and time strings
     */
    protected String getDayTimeSelectString() {
        StringBuilder buf = new StringBuilder(1024);
        if (getDoAbsoluteTimes()) {
            List times = getSelectedAbsoluteTimes();

            // no time selection is permitted as a valid choice -
            // will then use all times today by default.
            if (times.isEmpty()) {
                return "";
            }

            //check for the "no times available" message
            if (times.get(0) instanceof String) {
                return "";
            }

            if (archiveDay != null) {
                logger.trace("archiveDay: {}", archiveDay);
                try {
                    buf.append("DAY ").append(archiveDay).append(';');
                } catch (Exception e) {
                    logger.error("archiveDay parse error", e);
                }
            }
            else {
                // TJJ - I don't think this will happen any more, but if I'm wrong,
                // set the value to current day and pass that in the SELECT clause
                logger.trace("archiveDay is null!");
                LocalDate ld = LocalDate.now();
                try {
                    dt = new DateTime(ld.toDate());
                } catch (VisADException e) {
                    e.printStackTrace();
                }
                archiveDay = jdFormat.format(ld.toDate());
                logger.trace("Setting archiveDay to: " + archiveDay);
                try {
                    buf.append("DAY ").append(archiveDay).append(';');
                } catch (Exception e) {
                    logger.error("archiveDay parse error", e);
                }
            }

            buf.append("TIME ");
            for (int i = 0; i < times.size(); i++) {
                DateTime dt = (DateTime) times.get(i);
                buf.append(UtcDate.getHMS(dt));
                if (i != times.size() - 1) {
                    buf.append(',');
                }
            }
        } else {
            buf.append(AddeUtil.RELATIVE_TIME);
        }
        return buf.toString();
    }

    /**
     * Get the data type for this chooser
     *
     * @return  the type
     */
    @Override public String getDataType() {
        return "POINT";
    }

    /**
     * Get the increment between times for relative time requests
     *
     * @return time increment (hours)
     */
    @Override public float getRelativeTimeIncrement() {
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
            relTimeIncBox.setSelectedItem(relativeTimeIncrement);
        }
    }

    /**
     * Update labels, enable widgets, etc.
     */
    @Override protected void updateStatus() {
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
     * @return  label for the descriptor widget
     */
    @Override public String getDescriptorLabel() {
        return "Point Type"; 
    }
    
    /**
     * get the ADDE server group type to use
     *
     * @return group type
     */
    @Override protected String getGroupType() {
        return AddeServer.TYPE_POINT;
    }
    
    /**
     * Make the UI for this selector.
     * 
     * @return The gui
     */   
    @Override public JComponent doMakeContents() {
        JPanel myPanel = new JPanel();
        
        McVGuiUtils.setComponentWidth(descriptorComboBox, 584);
                
        JLabel stationLabel = McVGuiUtils.makeLabelRight("Station:");
        addServerComp(stationLabel);
        
        JLabel timesLabel = McVGuiUtils.makeLabelRight("Times:");
        addDescComp(timesLabel);
        
        JPanel timesPanel = makeTimesPanel();
        timesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        addDescComp(timesPanel);
                
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
                        .addComponent(timesLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(descriptorLabel)
                    .addComponent(descriptorComboBox))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(timesLabel)
                    .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        
        setInnerPanel(myPanel);
        return super.doMakeContents();
    }

    public JComponent doMakeContents(boolean doesOverride) {
        if (doesOverride) {
            return super.doMakeContents();
        } else {
            return doMakeContents();
        }
    }
}
