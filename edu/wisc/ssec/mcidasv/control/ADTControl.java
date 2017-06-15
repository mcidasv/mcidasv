/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2017
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

package edu.wisc.ssec.mcidasv.control;

import static ucar.unidata.util.GuiUtils.hbox;
import static ucar.unidata.util.GuiUtils.filler;
import static ucar.unidata.util.GuiUtils.left;
import static ucar.unidata.util.GuiUtils.topLeft;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arr;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.adt.Data;
import edu.wisc.ssec.mcidasv.adt.Env;
import edu.wisc.ssec.mcidasv.adt.Functions;
import edu.wisc.ssec.mcidasv.adt.History;
import edu.wisc.ssec.mcidasv.adt.Main;
import edu.wisc.ssec.mcidasv.adt.ReadIRImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.imagery.AddeImageDataSource;
import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.ImageDataSource;
import ucar.unidata.idv.DisplayInfo;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.unidata.xml.XmlObjectStore;
import ucar.visad.Util;
import ucar.visad.display.Animation;
import ucar.visad.display.PointProbe;
import ucar.visad.quantities.AirTemperature;
import visad.CommonUnit;
import visad.DateTime;
import visad.DisplayEvent;
import visad.FieldImpl;
import visad.FlatField;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.Set;
import visad.VisADException;
import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;
import visad.util.DataUtility;
import edu.wisc.ssec.mcidas.AreaDirectory;

/**
 * Advanced Dvorak Technique Display Control
 * Algorithm developed at UW Madison/CIMSS to objectively determine tropical
 * cyclone intensity from geostationary satellite infrared imagery.
 * 
 * @author Tim Olander
 */

public class ADTControl extends DisplayControlImpl {
    
    private static final Logger logger = LoggerFactory.getLogger(ADTControl.class);
    
    // Tooltip strings for the various UI buttons and inputs
    private static final String TOOLTIP_LAND_FLAG_ON = "Apply ADT Land Interaction Rule";
    private static final String TOOLTIP_LAND_FLAG_OFF = "Do Not Apply ADT Land Interaction Rule";
    private static final String TOOLTIP_MSLP_FROM_DVORAK = "Utilize Dvorak Technique to Derive MSLP";
    private static final String TOOLTIP_MSLP_FROM_CKZ = "Utilize Coutney/Knaff/Zehr Wind Speed/Presssure Technique";
    
    public static final String[] SCENE_TYPES = {
        "Eye", "Pinhole Eye", "Large Eye", "CDO", "Embedded Center",
        "Irregular CDO", "Curved Band", "Shear"
    };
    
    private static final String[] FORECAST_TYPES = {
        "ATCF", "DISC", "PACWARN", "GENERIC", "RMSC ICAO", "RMSC WTIO",
        "TCWC AXAU", "BEST", "HURDAT"
    };
    
    /** _more_ */
    private LatLonWidget latLonWidget;

    /** the probe */
    private PointProbe probe;
    
    /** _more_ */
    private LatLonPoint probeLocation;
    
    /** _more_ */
    private DataChoice choice;
    
    /** _more_ */
    private static boolean running = false;
    
    private static boolean runFullADTAnalysis = false;
    private static boolean GUIFileOverrideTF = false;
    
    private static boolean GUIOverrideSceneTF;
    
    private static boolean GUIRunAutoTF;
    private static boolean GUIOverrideTF;
    private static boolean GUIATCFOutputTF;
    private static boolean GUIInitStrengthTF;
    private static boolean GUILandFlagTF;

    // Default Java boolean value is false - need to initialize if we want true
    private static boolean GUIUseCKZTF = true;
    private static boolean GUIVmax1or10TF = true;

    private static boolean GUICommentAddTF;
    private static boolean GUIDeleteTF;
    private static boolean GUIATCFRecordOutputTF;
    private static boolean GUIPMWActivateTF;
    
    // need to determine or provide option
    private static int GUIDomainID;
    
    // need to initialize pulldown menu
    private static int GUIForecastType = 0;
    
    private static int GUIMWJulianDate;
    private static int GUIMWHHMMSSTime;
    private static int GUIStartDate;
    private static int GUIStartTime;
    private static int GUIEndDate;
    private static int GUIEndTime;
    private static int GUIHistoryListFormat;
    
    private static double GUIRawTValue;
    private static double GUIMWScore;
    private static double GUICKZGaleRadius;
    private static double GUICKZPenv;
    private static double GUIRMWSize;
    private static double GUIUserLatitude;
    private static double GUIUserLongitude;
    
    private static String GUIForecastFileName;
    private static String GUIATCFStormID;
    private static String GUIATCFSiteID;
    private static String GUIHistoryFileName;
    private static String GUIHistoryFileListingName;
    private static String GUICommentString;

    /** _more_ */
    private JButton adtBtn;
    
    private JButton forecastBtn;
    
    private JButton PMWFileBtn;
    
    private JRadioButton manButton;
    
    /** _more_ */
    private JComboBox<String> forecastTypeBox;

    private JFrame resultFrame;
    private JTextArea resultArea;
    private JFrame historyFrame;
    private JTextArea historyArea;
    
    private JLabel selectedHistoryFile;
    
    private JFileChooser historyFileSaveChooser;
    
    private JFrame overrideSceneFrame;
    private JLabel overrideSceneCurrentValueLabel;
    private JComboBox<String> overrideSceneTypeBox;
    
    private JLabel historyLabel;
    
    private static String HistoryListOutput;
    
    private static final String SCENE_TYPE_PREFIX = "Current Scene Type: ";
    
    /**
     * 
     */
    public ADTControl()  {
        super();
    }
    
    public boolean init(DataChoice choice) throws VisADException,
                        RemoteException {
        logger.info("ADTControl constructor begin...");
        
        if (!super.init(choice)) {
            return false;
        }
        this.choice = choice;

        probe = new PointProbe(new RealTuple(RealTupleType.SpatialEarth3DTuple,
                                new double[] { 0, 0, 0 }));

        probe.setManipulable(true);
        probe.setVisible(false);
        probe.setAutoSize(true);

        probe.setPointSize(getDisplayScale());
        addDisplayable(probe, FLAG_COLOR);
        
        /* obtain initial ADT environmental parameters */
        getADTenvParameters();
        
        /* setup window contents in Controls Window */
        setContents(setupMainWindow());
        
        // TJJ Jun 2017
        // We want to initialize probe to display center if in Manual mode
        NavigatedDisplay d = getNavigatedDisplay();
        if (manButton.isSelected()) {
            if (d != null) {
                EarthLocation el = d.getCenterPoint();
                logger.debug("Initializing probe location to: " + el.getLatitude() + ", " + el.getLongitude());
                probeLocation = el.getLatLonPoint();
                probe.setVisible(true);
            }
        }
        updateProbeLocation();
        return true;

    }

    private Container setupMainWindow() {

        /* add Lat/Lon position display text areas */  
        latLonWidget = new LatLonWidget(GuiUtils.makeActionListener(this,
                        "latLonWidgetChanged", null));
        JPanel latlonPanel = hbox(Misc.newList(latLonWidget));

        /* add Manual or Automated storm centering buttons */

        manButton = new JRadioButton("Manual");
        manButton.setActionCommand("Manual");
        manButton.setSelected(true);
        manButton.setToolTipText("Manually Select Storm Center In Image");
        JRadioButton autoButton = new JRadioButton("Automated");
        autoButton.setActionCommand("Automated");
        autoButton.setSelected(false);
        autoButton.setToolTipText("Select Forecast File For First Guess Below");
        ButtonGroup automangroup = new ButtonGroup();
        automangroup.add(manButton);
        automangroup.add(autoButton);
        
        /* add forecast file file selector button and file type menu */
        JLabel autoStormSelectLabel = new JLabel("AUTOMATED STORM SELECTION");
        JLabel forecastSelectLabel = new JLabel("Selected Forecast File:");
    
        JLabel forecastLabel = new JLabel("No forecast file selected yet");
        
        manButton.addActionListener(ae -> {
            // enable the manual lat/lon text boxes 
            latLonWidget.getLonField().setEnabled(true);
            latLonWidget.getLatField().setEnabled(true);
            autoStormSelectLabel.setEnabled(false);
            forecastSelectLabel.setEnabled(false);
            forecastBtn.setEnabled(false);
            forecastTypeBox.setEnabled(false);
            GUIRunAutoTF = false;
        });
        
        autoButton.addActionListener(ae -> {
            // disable the manual lat/lon text boxes when in auto mode
            latLonWidget.getLonField().setEnabled(false);
            latLonWidget.getLatField().setEnabled(false);
            autoStormSelectLabel.setEnabled(true);
            forecastSelectLabel.setEnabled(true);
            forecastBtn.setEnabled(true);
            forecastTypeBox.setEnabled(true);
            GUIRunAutoTF = true;
            System.out.printf("running automated ADT!!!\n");
        });

        forecastBtn = new JButton("Select Forecast File");
        forecastBtn.setPreferredSize(new Dimension(200,30));
        forecastBtn.addActionListener(fbtn -> {
            GUIForecastFileName = selectForecastFile();
            logger.trace("forecast file name=%s\n", GUIForecastFileName);
            forecastLabel.setText(
               GUIForecastFileName.substring(GUIForecastFileName.lastIndexOf(File.separatorChar) + 1)
            );
        });

        forecastTypeBox = new JComboBox<>(FORECAST_TYPES);
        forecastTypeBox.setSelectedIndex(GUIForecastType);
        forecastTypeBox.setPreferredSize(new Dimension(150,20));
        forecastTypeBox.addActionListener(ame -> {
            GUIForecastType = forecastTypeBox.getSelectedIndex();
            logger.trace("forecast file type=%d\n", GUIForecastType);
        });
        
        forecastTypeBox.setToolTipText("Select Forecast File type.");
        autoStormSelectLabel.setEnabled(false);
        forecastSelectLabel.setEnabled(false);
        forecastBtn.setEnabled(false);
        forecastTypeBox.setEnabled(false);

        /* define default history file text field message */
        selectedHistoryFile = new JLabel("No history file selected yet");

        /* add history file selection button */
        JButton historyBtn = new JButton("Select History File");
        historyBtn.setPreferredSize(new Dimension(200, 30));
        historyBtn.addActionListener(hbtn -> {
            GUIHistoryFileName = selectHistoryFile();
            logger.debug("history file name={}", GUIHistoryFileName);
            runFullADTAnalysis = true;
            selectedHistoryFile.setText(
               GUIHistoryFileName.substring(GUIHistoryFileName.lastIndexOf(File.separatorChar) + 1)
            );
        });

        /* add main ADT analysis start button */
        adtBtn = new JButton("Run ADT Analysis");
        adtBtn.setPreferredSize(new Dimension(250, 50));
        adtBtn.addActionListener(ae -> runADTmain());
        
        /* add history file list/edit button */
        JButton listBtn = new JButton("List/Edit History File");
        listBtn.setPreferredSize(new Dimension(250, 50));
        listBtn.addActionListener(ae -> {
            logger.debug("listing history file name={}", GUIHistoryFileName);
            try {
                listHistoryFile();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(null, 
                    "Your selection does not appear to be a valid ADT History File.");
            }
        });
    
        // TJJ Jan 2017
        // We'll keep the Manual vs. Automated PMW radio button group around
        // in case code to support automated is added later. For now, only
        // manual works in this version, so we'll just set the state of the
        // buttons but not show them.
        
        JRadioButton PMWManButton = new JRadioButton("Manual");
        PMWManButton.setActionCommand("Man");
        PMWManButton.setSelected(true);
        PMWManButton.setEnabled(true);
        
        JRadioButton PMWAutoButton = new JRadioButton("Automated");
        PMWAutoButton.setActionCommand("Auto");
        PMWAutoButton.setSelected(false);
        PMWAutoButton.setEnabled(false);
        
        /* PMW Manual options */
        JLabel pmwManDateLabel = new JLabel("Date:");
        JLabel pmwManTimeLabel = new JLabel("Time:");
        JLabel pmwManScoreLabel = new JLabel("Score:");
        JTextField pmwManDateTextField = new JTextField("1900JAN01", 8);
        pmwManDateTextField.addActionListener(ae -> {
            /* read PMW overpass date */
            JTextField src = (JTextField)ae.getSource();
            GUIMWJulianDate =
                Functions.cmonth2julian(src.getText());
            GUIMWScore = -99.0;
        });
        JTextField pmwManTimeTextField = new JTextField("000000",6);
        pmwManTimeTextField.addActionListener(ae -> {
            /* read PMW overpass time */
            JTextField src = (JTextField)ae.getSource();
            GUIMWHHMMSSTime = Integer.valueOf(src.getText());
            GUIMWScore = -99.0;
        });
        JTextField pmwManScoreTextField = new JTextField("-99.0",4);
        pmwManScoreTextField.addActionListener(ae -> {
            /* read PMW overpass score */
            JTextField src = (JTextField)ae.getSource();
            GUIMWScore = Double.valueOf(src.getText());
        });
        pmwManDateTextField.setEnabled(false);
        pmwManTimeTextField.setEnabled(false);
        pmwManScoreTextField.setEnabled(false);
        pmwManDateLabel.setEnabled(false);
        pmwManTimeLabel.setEnabled(false);
        pmwManScoreLabel.setEnabled(false);
    
        ButtonGroup pmwgroup = new ButtonGroup();
        pmwgroup.add(PMWAutoButton);
        pmwgroup.add(PMWManButton);
        PMWAutoButton.addActionListener(ae -> {
            /* enter file name */
            // Automated - file entry
            PMWFileBtn.setEnabled(true);
            pmwManDateTextField.setEnabled(false);
            pmwManTimeTextField.setEnabled(false);
            pmwManScoreTextField.setEnabled(false);
            pmwManDateLabel.setEnabled(false);
            pmwManTimeLabel.setEnabled(false);
            pmwManScoreLabel.setEnabled(false);
        });
        PMWManButton.addActionListener(ae -> {
            /* enter date/time and score manually */
            // Maunal entry
            PMWFileBtn.setEnabled(false);
            pmwManDateTextField.setEnabled(true);
            pmwManTimeTextField.setEnabled(true);
            pmwManScoreTextField.setEnabled(true);
            pmwManDateLabel.setEnabled(true);
            pmwManTimeLabel.setEnabled(true);
            pmwManScoreLabel.setEnabled(true);
        });
        
        /* Add PMW Analysis option buttons and entry fields */
        JCheckBox PMWActivateButton = new JCheckBox("Activate");
        PMWActivateButton.setActionCommand("PMW");
        PMWActivateButton.setSelected(false);
        PMWActivateButton.setEnabled(true);
        PMWActivateButton.addActionListener(ae -> {
            // if on, turn off and vice versa
            GUIPMWActivateTF = !GUIPMWActivateTF;
            PMWManButton.setEnabled(GUIPMWActivateTF);
            PMWManButton.setSelected(GUIPMWActivateTF);
            pmwManDateTextField.setEnabled(GUIPMWActivateTF);
            pmwManTimeTextField.setEnabled(GUIPMWActivateTF);
            pmwManScoreTextField.setEnabled(GUIPMWActivateTF);
            pmwManDateLabel.setEnabled(GUIPMWActivateTF);
            pmwManTimeLabel.setEnabled(GUIPMWActivateTF);
            pmwManScoreLabel.setEnabled(GUIPMWActivateTF);
            PMWActivateButton.setSelected(GUIPMWActivateTF);
        });
        
        /* add CKZ option buttons and entry fields */
        JLabel ckzPenvLabel = new JLabel("Penv:");
        ckzPenvLabel.setEnabled(false);
        
        JLabel ckz34radiusLabel = new JLabel("34kt Radius:");
        ckz34radiusLabel.setEnabled(false);
        
        JTextField ckzPenvTextField = new JTextField("1012", 5);
        ckzPenvTextField.addActionListener(ae -> {
            JTextField src = (JTextField)ae.getSource();
            GUICKZPenv = Integer.valueOf(src.getText());
        });
        JTextField ckz34radiusTextField = new JTextField("300", 5);
        ckz34radiusTextField.addActionListener(ae -> {
            JTextField src = (JTextField)ae.getSource();
            GUICKZGaleRadius = Integer.valueOf(src.getText());
        });
        ckzPenvTextField.setEnabled(false);
        ckz34radiusTextField.setEnabled(false);
    
        JRadioButton mslpDvorakButton = new JRadioButton("Dvorak");
        mslpDvorakButton.setActionCommand("Dvorak");
        mslpDvorakButton.setSelected(true);
        mslpDvorakButton.setToolTipText(TOOLTIP_MSLP_FROM_DVORAK);
        JRadioButton mslpCKZButton = new JRadioButton("CKZ");
        mslpCKZButton.setActionCommand("CKZ");
        mslpCKZButton.setSelected(false);
        mslpCKZButton.setToolTipText(TOOLTIP_MSLP_FROM_CKZ);
        ButtonGroup mslpgroup = new ButtonGroup();
        mslpgroup.add(manButton);
        mslpgroup.add(autoButton);
        mslpDvorakButton.addActionListener(ae -> {
            // Dvorak
            ckzPenvTextField.setEnabled(false);
            ckz34radiusTextField.setEnabled(false);
            ckzPenvLabel.setEnabled(false);
            ckz34radiusLabel.setEnabled(false);
            mslpDvorakButton.setSelected(true);
            mslpCKZButton.setSelected(false);
            GUIUseCKZTF = false;
        });
        mslpCKZButton.addActionListener(ae -> {
            // CKZ
            ckzPenvTextField.setEnabled(true);
            ckz34radiusTextField.setEnabled(true);
            ckzPenvLabel.setEnabled(true);
            ckz34radiusLabel.setEnabled(true);
            mslpDvorakButton.setSelected(false);
            mslpCKZButton.setSelected(true);
            GUIUseCKZTF = true;
        });
        
        /* various other keyword options */
        /* Initial classification entry -- RAWT */
        JLabel RawTLabel = new JLabel("Raw T:");
        JTextField RawTTextField = new JTextField("1.0", 4);
        RawTTextField.addActionListener(ae -> {
            JTextField src = (JTextField)ae.getSource();
            GUIRawTValue = Double.valueOf(src.getText());
            GUIInitStrengthTF = GUIRawTValue >= 1.0;
        });
        
        /* Radius of Max Wind entry -- RMW */
        JLabel RMWLabel = new JLabel("RMW:");
        JTextField RMWTextField = new JTextField("-99",4);
        RMWTextField.addActionListener(ae -> {
            JTextField src = (JTextField)ae.getSource();
            GUIRMWSize = Double.valueOf(src.getText());
        });
        
        /* Override option */
        JButton sceneOverrideButton = new JButton("Override Scene Type");
        JLabel OverrideLabel = new JLabel(SCENE_TYPE_PREFIX + SCENE_TYPES[Env.OverrideSceneTypeIndex]);
        sceneOverrideButton.addActionListener(ae -> {
            overrideSceneFrame.setVisible(true);
        });
        
//        JRadioButton OverrideYESButton = new JRadioButton("YES");
//        OverrideYESButton.setActionCommand("Yes");
//        OverrideYESButton.setSelected(false);
//        JRadioButton OverrideNOButton = new JRadioButton("NO");
//        OverrideNOButton.setActionCommand("No");
//        OverrideNOButton.setSelected(true);
//        ButtonGroup overridegroup = new ButtonGroup();
//        overridegroup.add(OverrideNOButton);
//        overridegroup.add(OverrideYESButton);
//
//        OverrideYESButton.addActionListener(ae -> {
//            GUIOverrideTF = true;
//            GUIOverrideSceneTF = true;
//            OverrideNOButton.setSelected(false);
//            OverrideYESButton.setSelected(true);
//        });
//
//        OverrideNOButton.addActionListener(ae -> {
//            GUIOverrideTF = false;
//            GUIOverrideSceneTF = false;
//            OverrideNOButton.setSelected(true);
//            OverrideYESButton.setSelected(false);
//        });
        
        /* ATCF Analysis Output Checkbox */
        
        JLabel ATCFOutputLabel = new JLabel("ATCF Output:");
        JCheckBox ATCFOutputButton = new JCheckBox("Activate");
        ATCFOutputButton.setActionCommand("ATCF");
        ATCFOutputButton.setSelected(false);
        ATCFOutputButton.setEnabled(true);
    
        JLabel ATCFEntryStormLabel = new JLabel("Storm ID:");
        JTextField ATCFEntryStormTextField = new JTextField("XXX",8);
        JLabel ATCFEntrySiteLabel = new JLabel("Site ID:");
        JTextField ATCFEntrySiteTextField = new JTextField("XXXX",8);
        ATCFEntryStormLabel.setEnabled(false);
        ATCFEntryStormTextField.setEnabled(false);
        ATCFEntrySiteLabel.setEnabled(false);
        ATCFEntrySiteTextField.setEnabled(false);
        ATCFEntryStormTextField.addActionListener(ae -> {
            JTextField src = (JTextField)ae.getSource();
            GUIATCFStormID = src.getText();
        });
        ATCFEntrySiteTextField.addActionListener(ae -> {
            JTextField src = (JTextField)ae.getSource();
            GUIATCFSiteID = src.getText();
        });
        
        ATCFOutputButton.addActionListener(ae -> {
            // if on, turn off and vice versa
            GUIATCFRecordOutputTF = !GUIATCFRecordOutputTF;
            ATCFEntryStormLabel.setEnabled(GUIATCFRecordOutputTF);
            ATCFEntryStormTextField.setEnabled(GUIATCFRecordOutputTF);
            ATCFEntrySiteLabel.setEnabled(GUIATCFRecordOutputTF);
            ATCFEntrySiteTextField.setEnabled(GUIATCFRecordOutputTF);
            ATCFOutputButton.setSelected(GUIATCFRecordOutputTF);
        });
        
        /* Land Flag button -- LAND */
        JLabel LandFlagLabel = new JLabel("Land Flag:");
        JRadioButton LandONButton = new JRadioButton("ON");
        LandONButton.setActionCommand("On");
        LandONButton.setSelected(true);
        LandONButton.setToolTipText(TOOLTIP_LAND_FLAG_ON);
        JRadioButton LandOFFButton = new JRadioButton("OFF");
        LandOFFButton.setActionCommand("Off");
        LandOFFButton.setSelected(false);
        LandOFFButton.setToolTipText(TOOLTIP_LAND_FLAG_OFF);
        ButtonGroup landgroup = new ButtonGroup();
        landgroup.add(LandONButton);
        landgroup.add(LandOFFButton);
        LandONButton.addActionListener(ae -> {
            // LAND=YES
            LandONButton.setSelected(true);
            LandOFFButton.setSelected(false);
            GUILandFlagTF = true;
        });
        LandOFFButton.addActionListener(ae -> {
            // LAND=NO
            LandONButton.setSelected(false);
            LandOFFButton.setSelected(true);
            GUILandFlagTF = false;
        });
        
        /*  Wind Speed Vmax output button -- VOUT */
        JLabel VOutLabel = new JLabel("VMax:");
        JRadioButton V1MinButton = new JRadioButton("One-minute");
        V1MinButton.setActionCommand("One");
        V1MinButton.setSelected(true);
        JRadioButton V10MinButton = new JRadioButton("Ten-minute");
        V10MinButton.setActionCommand("Ten");
        V10MinButton.setSelected(false);
        ButtonGroup voutgroup = new ButtonGroup();
        voutgroup.add(V1MinButton);
        voutgroup.add(V10MinButton);
        V1MinButton.addActionListener(ae -> {
            // 1-minute winds
            V1MinButton.setSelected(true);
            V10MinButton.setSelected(false);
            GUIVmax1or10TF = true;
        });
        V10MinButton.addActionListener(ae -> {
            // 10-minute winds
            V1MinButton.setSelected(false);
            V10MinButton.setSelected(true);
            GUIVmax1or10TF = false;
        });
        
        JCheckBox GUIFileOverrideButton = new JCheckBox("GUI File Override");
        GUIFileOverrideButton.setActionCommand("Override");
        GUIFileOverrideButton.setSelected(false);
        GUIFileOverrideButton.setEnabled(true);
        GUIFileOverrideButton.addActionListener(ae -> {
            if (GUIFileOverrideTF) {
                GUIFileOverrideTF = false;
                getADTenvParameters();
            } else {
                GUIFileOverrideTF = true;
            }
            GUIFileOverrideButton.setSelected(GUIFileOverrideTF);
        });
        JLabel blankfield = new JLabel("");

        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent widgets =
            GuiUtils.formLayout(
                arr(left(hbox(arr(new JLabel("Storm Center Selection:"), manButton, autoButton), 5)),
                    filler(),
                    left(hbox(arr(new JLabel("MANUAL STORM SELECTION")), 10)),
                    filler(),
                    left(hbox(arr(filler(30,1),latlonPanel))), filler(),
                    left(hbox(arr(autoStormSelectLabel), 10)), filler(),
                    left(hbox(arr(filler(30,1),forecastBtn, forecastTypeBox,
                        forecastSelectLabel, forecastLabel), 5)), filler(),
                    left(hbox(arr(blankfield))),
                    filler(1,10),
                    left(hbox(arr(new JLabel("HISTORY FILE INFORMATION")), 10)),filler(),
                    left(hbox(arr(filler(30, 1), historyBtn, new JLabel
                        ("Selected History File: "), selectedHistoryFile), 5)),
                    filler(),
                    left(hbox(arr(blankfield))),
                    filler(1,10),
                    left(hbox(arr(new JLabel("PMW ANALYSIS")), 10)),filler(),
                    left(hbox(arr(filler(30,1),PMWActivateButton,
                        pmwManDateLabel, pmwManDateTextField, pmwManTimeLabel,
                        pmwManTimeTextField, pmwManScoreLabel, pmwManScoreTextField), 5)), filler(),
                    left(hbox(arr(blankfield))),
                    filler(1,10),
                    left(hbox(arr(new JLabel("MISCELLANEOUS OPTIONS")), 10)), filler(),
                    left(hbox(arr(filler(30, 1),new JLabel("MSLP Conversion Method:"), mslpDvorakButton, mslpCKZButton, ckzPenvLabel, ckzPenvTextField, ckz34radiusLabel,ckz34radiusTextField), 5)),filler(),
                    left(hbox(arr(filler(30, 1), sceneOverrideButton, OverrideLabel), 5)), filler(),
                    left(hbox(arr(filler(30, 1),LandFlagLabel,LandONButton, LandOFFButton, filler(20,1), VOutLabel, V1MinButton, V10MinButton, filler(20,1),RawTLabel,RawTTextField, RMWLabel, RMWTextField), 5)),filler(),
                    left(hbox(arr(filler(30, 1),ATCFOutputLabel, ATCFOutputButton,ATCFEntryStormLabel,ATCFEntryStormTextField, ATCFEntrySiteLabel,ATCFEntrySiteTextField), 5)),filler(),
                    left(hbox(arr(filler(80, 1), adtBtn, listBtn, GUIFileOverrideButton), 20)), filler()));
                    
        JPanel controls = topLeft(widgets);

        /* set up ADT Bulletin display area */
        resultArea = new JTextArea();
        JScrollPane resultScroller = new JScrollPane(resultArea);
        resultScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        resultScroller.setPreferredSize(new Dimension(400, 200));

        Font c = new Font("Courier", Font.BOLD, 12);
        
        resultFrame = new JFrame("ADT Results");
        resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        resultFrame.add(resultArea,BorderLayout.CENTER);
        resultFrame.setPreferredSize(new Dimension(400, 600));
        resultFrame.setFont(c);

        /* set up ADT History File display area */
        historyFrame = new JFrame("ADT History File Listing");
        Container historyContainer = historyFrame.getContentPane();
        historyFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel historyTextPanel = new JPanel();
        FlowLayout historyListLayout = new FlowLayout();
        historyTextPanel.setLayout(historyListLayout);
        historyListLayout.setAlignment(FlowLayout.CENTER);
           
        historyArea = new JTextArea(50,150);
        historyArea.setEditable(false);
        JScrollPane historyScroller = new JScrollPane(historyArea);
        historyScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        historyScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        historyScroller.setPreferredSize(new Dimension(1200, 400));
        historyArea.setFont(c);

        JPanel historyLabelPanel = new JPanel();
        FlowLayout HistoryLabelLayout = new FlowLayout();
        historyLabelPanel.setLayout(HistoryLabelLayout);
        HistoryLabelLayout.setAlignment(FlowLayout.CENTER);
        historyLabel = new JLabel("No History File Selected");
        historyLabel.setPreferredSize(new Dimension(800,20));
        historyLabel.setFont(c);
        
        /* history file Editing Date Selection window */
        JFrame historyDateFrame = new JFrame("History File Editor");
        Container historyDateContainer = historyDateFrame.getContentPane();
        historyDateFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel historyDatePanel = new JPanel();
        FlowLayout DateStartEndLayout = new FlowLayout();
        historyDatePanel.setLayout(DateStartEndLayout);
        DateStartEndLayout.setAlignment(FlowLayout.CENTER);
        JLabel historyDateStartLabel = new JLabel("Start:");
        JLabel historyDateStartDateLabel = new JLabel("Date");
        JTextField historyDateStartDateTextField = new JTextField("0000XXX00", 10);
        JLabel historyDateStartTimeLabel = new JLabel("Time");
        JTextField historyDateStartTimeTextField = new JTextField("-1", 8);
        JLabel historyDateEndLabel = new JLabel("End");
        JLabel historyDateEndDateLabel = new JLabel("Date");
        JTextField historyDateEndDateTextField = new JTextField("0000XXX00", 10);
        JLabel historyDateEndTimeLabel = new JLabel("Time");
        JTextField historyDateEndTimeTextField = new JTextField("-1", 8);
        
        JPanel historyButtonPanel = new JPanel();
        FlowLayout HistoryButtonLayout = new FlowLayout();
        historyButtonPanel.setLayout(HistoryButtonLayout);
        HistoryButtonLayout.setAlignment(FlowLayout.CENTER);
        JButton historyEditBtn = new JButton("Edit History File");
        historyEditBtn.setPreferredSize(new Dimension(200,20));
        historyEditBtn.addActionListener(ae -> {
            historyDateFrame.pack();
            historyDateFrame.setVisible(true);
            logger.debug("editing history file name={}", GUIHistoryFileName);
        });
        JButton historySaveListingBtn = new JButton("Output Listing");
        historySaveListingBtn.setPreferredSize(new Dimension(200,20));
        historySaveListingBtn.addActionListener(ae -> {
            GUIHistoryFileListingName = selectHistoryFileOutput();
            logger.debug("saving history listing file name={}", GUIHistoryFileListingName);
            GUIHistoryListFormat = -1;
        });
        JButton historyWriteATCFBtn = new JButton("Write ATCF");
        historyWriteATCFBtn.setPreferredSize(new Dimension(200,20));
        historyWriteATCFBtn.addActionListener(ae -> {
            GUIATCFOutputTF = true;
            GUIHistoryListFormat = 0;
            logger.debug("calling ATCFFileOutput");
            ATCFFileOutput(0);
        });
        historyLabelPanel.add(historyLabel);
        historyLabelPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        historyTextPanel.add(historyScroller);
        historyTextPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        historyButtonPanel.add(historyEditBtn);
        historyButtonPanel.add(historySaveListingBtn);
        historyButtonPanel.add(historyWriteATCFBtn);
        historyButtonPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        historyContainer.add(historyLabelPanel,BorderLayout.NORTH);
        historyContainer.add(historyTextPanel,BorderLayout.CENTER);
        historyContainer.add(historyButtonPanel,BorderLayout.SOUTH);
        
        historyDateStartDateTextField.addActionListener(ae -> {
            JTextField textField = (JTextField)ae.getSource();
            GUIStartDate = Functions.cmonth2julian(textField.getText());
        });
        historyDateStartTimeTextField.addActionListener(ae -> {
            JTextField textField = (JTextField)ae.getSource();
            GUIStartTime = Integer.valueOf(textField.getText());
        });
        historyDateEndDateTextField.addActionListener(ae -> {
            JTextField textField = (JTextField)ae.getSource();
            GUIEndDate = Functions.cmonth2julian(textField.getText());
        });
        historyDateEndTimeTextField.addActionListener(ae -> {
            JTextField textField = (JTextField)ae.getSource();
            GUIEndTime = Integer.valueOf(textField.getText());
        });
        
        JPanel historyDateButtonPanel = new JPanel();
        FlowLayout DateButtonLayout = new FlowLayout();
        historyDateButtonPanel.setLayout(DateButtonLayout);
        DateButtonLayout.setAlignment(FlowLayout.CENTER);
        JRadioButton historyEditDeleteButton = new JRadioButton("Delete Records");
        historyEditDeleteButton.setActionCommand("Delete");
        historyEditDeleteButton.setSelected(false);
        JRadioButton historyEditAddCommentButton = new JRadioButton("Add Comment");
        historyEditAddCommentButton.setActionCommand("Comment");
        historyEditAddCommentButton.setSelected(false);
        ButtonGroup editgroup = new ButtonGroup();
        editgroup.add(historyEditDeleteButton);
        editgroup.add(historyEditAddCommentButton);
        JLabel historyEditAddCommentLabel = new JLabel("Comment:");
        JTextField historyEditAddCommentTextField = new JTextField("no comment entered", 25);
        historyEditAddCommentTextField.setEnabled(false);
        
        historyEditDeleteButton.addActionListener(ae -> {
            // history Edit - Delete
            historyEditDeleteButton.setSelected(true);
            historyEditAddCommentButton.setSelected(false);
            historyEditAddCommentLabel.setEnabled(false);
            historyEditAddCommentTextField.setEnabled(false);
            GUICommentAddTF = false;
            GUIDeleteTF = true;
        });
        
        historyEditAddCommentButton.addActionListener(ae -> {
            // history Edit - Add Comment
            historyEditDeleteButton.setSelected(false);
            historyEditAddCommentButton.setSelected(true);
            historyEditAddCommentLabel.setEnabled(true);
            historyEditAddCommentTextField.setEnabled(true);
            GUICommentAddTF = true;
            GUIDeleteTF = false;
        });
        historyEditAddCommentTextField.addActionListener(ae -> {
            JTextField src = (JTextField)ae.getSource();
            GUICommentString = src.getText();
        });
        JPanel historyEditInputPanel = new JPanel();
        FlowLayout EditInputButtonLayout = new FlowLayout();
        historyEditInputPanel.setLayout(EditInputButtonLayout);
        EditInputButtonLayout.setAlignment(FlowLayout.CENTER);
        JButton historyEditApplyButton = new JButton("Apply Edits");
        historyEditApplyButton.setPreferredSize(new Dimension(150,20));
        historyEditApplyButton.addActionListener(ae -> modifyHistoryFile());
        JButton historyEditCancelButton = new JButton("Cancel");
        historyEditCancelButton.setPreferredSize(new Dimension(150,20));
        historyEditCancelButton.addActionListener(ae -> historyDateFrame.dispose());
        historyDatePanel.add(historyDateStartLabel);
        historyDatePanel.add(historyDateStartDateLabel);
        historyDatePanel.add(historyDateStartDateTextField);
        historyDatePanel.add(historyDateStartTimeLabel);
        historyDatePanel.add(historyDateStartTimeTextField);
        historyDatePanel.add(historyDateEndLabel);
        historyDatePanel.add(historyDateEndDateLabel);
        historyDatePanel.add(historyDateEndDateTextField);
        historyDatePanel.add(historyDateEndTimeLabel);
        historyDatePanel.add(historyDateEndTimeTextField);
        historyDatePanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        historyDateButtonPanel.add(historyEditDeleteButton);
        historyDateButtonPanel.add(historyEditAddCommentButton);
        historyDateButtonPanel.add(historyEditAddCommentLabel);
        historyDateButtonPanel.add(historyEditAddCommentTextField);
        historyEditInputPanel.add(historyEditApplyButton);
        historyEditInputPanel.add(historyEditCancelButton);
        historyDateContainer.add(historyDatePanel, BorderLayout.NORTH);
        historyDateContainer.add(historyDateButtonPanel, BorderLayout.CENTER);
        historyDateContainer.add(historyEditInputPanel, BorderLayout.SOUTH);

        /* set up Scene Type Override Window display window */
        overrideSceneFrame = new JFrame("Override Scene Type");
        overrideSceneFrame.setSize(new Dimension(400, 300));
        Container overrideSceneContainer = overrideSceneFrame.getContentPane();
        overrideSceneFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel overrideSceneCurrentPanel = new JPanel();
        FlowLayout OverrideSceneCurrentLayout = new FlowLayout();
        overrideSceneCurrentPanel.setLayout(OverrideSceneCurrentLayout);
        OverrideSceneCurrentLayout.setAlignment(FlowLayout.CENTER);
        JLabel overrideSceneCurrentLabel = new JLabel("Current Scene Type:");
        overrideSceneCurrentValueLabel = new JLabel(SCENE_TYPES[Env.OverrideSceneTypeIndex]);
        JPanel overrideSceneSelectPanel = new JPanel();
        FlowLayout OverrideSceneSelectLayout = new FlowLayout();
        overrideSceneCurrentPanel.setLayout(OverrideSceneSelectLayout);
        OverrideSceneSelectLayout.setAlignment(FlowLayout.CENTER);
        JLabel overrideSceneSelectLabel = new JLabel("Select New Scene Type:");
        overrideSceneTypeBox = new JComboBox<>(SCENE_TYPES);
        overrideSceneTypeBox.setSelectedIndex(Env.OverrideSceneTypeIndex);
        overrideSceneTypeBox.setPreferredSize(new Dimension(150,20));
        // overrideSceneTypeBox.addActionListener(ame -> Env.OverrideSceneTypeIndex = overrideSceneTypeBox.getSelectedIndex());
        JPanel overrideSceneButtonPanel = new JPanel();
        FlowLayout OverrideSceneButtonLayout = new FlowLayout();
        overrideSceneButtonPanel.setLayout(OverrideSceneButtonLayout);
        OverrideSceneButtonLayout.setAlignment(FlowLayout.CENTER);
        JButton overrideSceneAcceptButton = new JButton("Accept New Scene");
        overrideSceneAcceptButton.setPreferredSize(new Dimension(190,20));
        overrideSceneAcceptButton.addActionListener(ae -> {
            // accept new scene selection
            overrideSceneFrame.setVisible(false);
            Env.OverrideSceneTypeIndex = overrideSceneTypeBox.getSelectedIndex();
            OverrideLabel.setText(SCENE_TYPE_PREFIX + SCENE_TYPES[Env.OverrideSceneTypeIndex]);
            overrideSceneCurrentValueLabel.setText(SCENE_TYPES[Env.OverrideSceneTypeIndex]);
            // runADTmain();
        });
        JButton overrideSceneCancelButton = new JButton("Keep Current Scene");
        overrideSceneCancelButton.setPreferredSize(new Dimension(190,20));
        overrideSceneCancelButton.addActionListener(ae -> {
            overrideSceneFrame.setVisible(false);
            // runADTmain();
        });
        overrideSceneCurrentPanel.add(overrideSceneCurrentLabel);
        overrideSceneCurrentPanel.add(overrideSceneCurrentValueLabel);
        overrideSceneSelectPanel.add(overrideSceneSelectLabel);
        overrideSceneSelectPanel.add(overrideSceneTypeBox);
        overrideSceneButtonPanel.add(overrideSceneAcceptButton);
        overrideSceneButtonPanel.add(overrideSceneCancelButton);
        overrideSceneContainer.add(overrideSceneCurrentPanel, BorderLayout.NORTH);
        overrideSceneContainer.add(overrideSceneSelectPanel, BorderLayout.CENTER);
        overrideSceneContainer.add(overrideSceneButtonPanel, BorderLayout.SOUTH);

        return controls;
    }

    private void runADTmain() {
        if (!running) {
            running = true;
            adtBtn.setEnabled(false);
            adtBtn.setText("Running");
            Misc.run(() -> {
                runADT();
                ExitADT();
            });
        }
    }
        
    private void runADT() {
        Main StormADT = new Main();
        String ADTRunOutput;
        String ErrorMessage;
        
        if (GUIFileOverrideTF) {
            String GUIOverrideFilePath = System.getenv("ODTHOME");
            if (GUIOverrideFilePath == null) {
                GUIOverrideFilePath = System.getenv("HOME");
            }
            String GUIOverrideFile = GUIOverrideFilePath + "/runadt.nogui.inputs.txt";
            /* GUIFileOverrideCheckBoxToggle();  change toggle back to OFF */
            int RetVal = ReadGUIOverrideInputFile(GUIOverrideFile);
            if (RetVal == -1) {
                ErrorMessage = String.format("Error reading GUI override file %s\n",GUIOverrideFile);
                System.out.printf(ErrorMessage);
                userMessage(ErrorMessage);
                ExitADT();
                return;
            }
        }
        
        loadADTenvParameters();
        
        boolean RunAuto = Env.AutoTF;

        /* set storm position either through automated storm selection or by manual choice */
        GetImageDateTime();
        int ReturnVal = StormADT.GetInitialPosition();  // should set up to throw exception instead of return value
        if (ReturnVal < 0) {
            ErrorMessage = "Error obtaining initial position... exiting ADT\n";
            System.out.printf(ErrorMessage);
            userMessage(ErrorMessage);
            ExitADT();
        } else {
            if (RunAuto) {
                try {
                    float CenterLatitude = (float)Env.SelectedLatitude;
                    float CenterLongitude =  (float)Env.SelectedLongitude;
                    /* System.out.printf("pre-ARCHER latitude=%f longitude=%f\n",CenterLatitude,CenterLongitude); */
                    GetImageData(CenterLatitude, CenterLongitude);
                } catch (Exception exception) {
                    ErrorMessage = "Error reading IR data pre-ARCHER\n";
                    System.out.printf(ErrorMessage);
                    userMessage(ErrorMessage);
                    ExitADT();
                    return;
                }
                StormADT.GetARCHERPosition();
            } else {
                if (probeLocation == null) {
                    ErrorMessage = "Please select storm center location manually and try again";
                    System.out.printf(ErrorMessage);
                    userMessage(ErrorMessage);
                    ExitADT();
                    return;
                }
            }
            
            try {
                float CenterLatitude = (float) Env.SelectedLatitude;
                float CenterLongitude =  (float) Env.SelectedLongitude;
                /* System.out.printf("latitude=%f longitude=%f domain=%d\n",CenterLatitude,CenterLongitude,DomainID); */
                GetImageData(CenterLatitude, CenterLongitude);
            } catch (Exception e) {
                // e.printStackTrace();
                ErrorMessage = "Error reading IR data in getimagedata()\n";
                logger.error(ErrorMessage.trim(), e);
                userMessage(ErrorMessage);
                ExitADT();
                return;
            }
            
            try {
                logger.debug("RUNNING ADT ANALYSIS");
                ADTRunOutput = StormADT.RunADTAnalysis(runFullADTAnalysis,GUIHistoryFileName);
            } catch (IOException exception) {
                ErrorMessage = "Error with call to StormADT.RunADT()\n";
                System.out.printf(ErrorMessage);
                userMessage(ErrorMessage);
                ExitADT();
                return;
            }
            if (GUIOverrideSceneTF) {
                /* System.out.printf("Overriding scene type!!!  Scene value=%d\n",InitialSceneTypeValue); */
                overrideSceneCurrentValueLabel.setText(SCENE_TYPES[Env.OverrideSceneTypeIndex]);
                overrideSceneFrame.pack();
                overrideSceneFrame.setVisible(true);
                ExitADT();
            } else {
                logger.debug("done running ADT");
    
                resultArea.setText(ADTRunOutput);
                resultFrame.pack();
                resultFrame.setVisible(true);
 
                if (GUIHistoryFileName != null) {
                    try {
                        // int[] InsertRecs = History.InsertHistoryRecord(runFullADTAnalysis,GUIHistoryFileName);
                        /* System.out.printf("*** Modified=%d InsertOverwriteFlag=%d***\n",InsertRecs[0],InsertRecs[1]); */
                        int NumRecs = History.WriteHistoryFile(GUIHistoryFileName);
                        ErrorMessage = String.format("Number of records written to history file: %d\n", NumRecs);
                    } catch (IOException exception) {
                        ErrorMessage = String.format("Error writing history file %s\n",GUIHistoryFileName);
                    }
                    System.out.printf(ErrorMessage);
                    userMessage(ErrorMessage);
                }
                if (GUIATCFRecordOutputTF) {
                    ATCFFileOutput(-1);
                }
                ExitADT();
            }
        }
    }
    
    private void ExitADT() {
        running = false;
        adtBtn.setEnabled(true);
        adtBtn.setText("Run Analysis");
    }
    
    /*
     * Override for additional local cleanup
     * (non-Javadoc)
     * @see ucar.unidata.idv.control.DisplayControlImpl#doRemove()
     */

    @Override
    public void doRemove() throws RemoteException, VisADException {
        super.doRemove();
        if (resultFrame != null) resultFrame.dispose();
        if (historyFrame != null) historyFrame.dispose();
    }

    private void listHistoryFile() {
        HistoryListOutput = null;
        
        History CurrentHistory = new History();
        
        // Make sure a valid History File has been selected. At startup, value will be null
        if (GUIHistoryFileName == null) {
            JOptionPane.showMessageDialog(null, 
            "Please first select a valid ADT History File.");
            return;
        }
        
        try {
            logger.debug("trying to read history file {}", GUIHistoryFileName);
            CurrentHistory.ReadHistoryFile(GUIHistoryFileName);
        } catch (IOException exception) {
            String ErrorMessage = String.format("History file %s is not found",GUIHistoryFileName);
            logger.warn(ErrorMessage);
            userMessage(ErrorMessage);
            return;
        }
        
        logger.debug("Number of history records: ", History.HistoryNumberOfRecords());
        
        HistoryListOutput = History.ListHistory(0, -1, "CIMS", "99X");
        historyLabel.setText(GUIHistoryFileName);
        historyArea.setText(HistoryListOutput);
        historyFrame.pack();
        historyFrame.setVisible(true);
        
    }
    
    private void modifyHistoryFile() {

        if (GUIDeleteTF) {
            // delete records
            int DeleteReturn[] = History.DeleteHistoryRecords(runFullADTAnalysis,GUIHistoryFileName);
            logger.debug("deleted {} records... modified {} records", DeleteReturn[1],DeleteReturn[0]);
        } else if( GUICommentAddTF) {
            // 
            int CommentAddReturn = History.CommentHistoryRecords(GUICommentString);
            logger.debug("added comment to {} records",CommentAddReturn);
        } else {
            // invalid selection
            logger.warn("entered invalid selection!");
        }
        
        try {
            int HistoryFileRecords = History.WriteHistoryFile(GUIHistoryFileName);
            if (HistoryFileRecords >= 0) {
                logger.debug("wrote {} records to '{}'", HistoryFileRecords, GUIHistoryFileName);
            }
        } catch (IOException exception) {
            String ErrorMessage = String.format("error updating history file %s",GUIHistoryFileName);
            System.out.printf(ErrorMessage);
            userMessage(ErrorMessage);
        }
    }
        
    private String selectHistoryFile() {
        
        String fileNameReturn = null;
        
        JFrame historyFileFrame = new JFrame();
        JFileChooser historyFileChooser = new JFileChooser();
        String historyPath = System.getenv("ODTHISTORY");
        if (historyPath == null) {
            historyPath = getLastPath("mcv.adt.lasthistorypath", System.getProperty("user.home"));
        }
        historyFileChooser.setCurrentDirectory(new File(historyPath));
        historyFileChooser.setDialogTitle("Select ADT History File");
        int returnVal = historyFileChooser.showOpenDialog(historyFileFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = historyFileChooser.getSelectedFile();
            fileNameReturn = file.getAbsolutePath();
            setLastPath("mcv.adt.lasthistorypath", file.getPath());
        }
        return fileNameReturn;
    }
    
    /**
     * Returns the path that corresponds to the given McIDAS-V property ID.
     *
     * @param id ID used to store user's last selected path.
     * @param defaultPath Path to use if {@code id} has not been set.
     *
     * @return Either the {@code String} representation of the last selected
     * path, or {@code defaultPath}.
     */
    private String getLastPath(String id, String defaultPath) {
        McIDASV mcv = (McIDASV)getIdv();
        String path = defaultPath;
        if (mcv != null) {
            path = mcv.getObjectStore().get(id, defaultPath);
        }
        return path;
    }
    
    /**
     * Sets the value of the given McIDAS-V property ID to the specified path.
     *
     * @param id ID to store.
     * @param path Path to associate with {@code id}.
     */
    private void setLastPath(String id, String path) {
        String okayPath = (path != null) ? path : "";
        McIDASV mcv = (McIDASV)getIdv();
        if (mcv != null) {
            XmlObjectStore store = mcv.getObjectStore();
            store.put(id, okayPath);
            store.saveIfNeeded();
        }
    }
    
    private String selectHistoryFileOutput() {
        
        File saveFile = null;
        String HistoryPath;
        String ErrorMessage;
        
        historyFileSaveChooser = new JFileChooser();
        HistoryPath = System.getenv("ODTHISTORY");
        if (HistoryPath == null) {
            HistoryPath = System.getenv("HOME");
        }
        historyFileSaveChooser.setCurrentDirectory(new File(HistoryPath));
        /* historyFileSaveChooser.setSelectedFile(new File(GUIHistoryFileName + ".txt")); */
        historyFileSaveChooser.setDialogTitle("Select ADT History Listing File");
        int returnVal = historyFileSaveChooser.showSaveDialog(historyFileSaveChooser);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            saveFile = historyFileSaveChooser.getSelectedFile();
            try (FileWriter outFile = new FileWriter(saveFile)) {
                outFile.write(HistoryListOutput);
                outFile.close();
                /* outFile.flush(); */
                ErrorMessage = String.format("success writing history file output file %s\n",saveFile.toString());
            } catch (IOException ex) {
                ex.printStackTrace();
                ErrorMessage = String.format("error writing history file output file %s\n",saveFile.toString());
            }
            System.out.printf(ErrorMessage);
            userMessage(ErrorMessage);
        }
        //else if (returnVal == JFileChooser.CANCEL_OPTION) {
        //    // User has pressed cancel button
        //}
            
        String saveFilePath = null;
        if (saveFile != null) {
            saveFilePath = saveFile.toString();
        }
        return saveFilePath;

    }
    
    private void ATCFFileOutput(int outputstyle) {
        File saveFile = null;
        String ATCFOutputFileName;
        String ATCFOutputFilePath;
        String ATCFFileOutput;
        String ATCFMessage;
        String HistoryPath;
        boolean writefileTF = false;
        
        if (outputstyle == 0) {
            // output entire history file in ATCF
            historyFileSaveChooser = new JFileChooser();
            HistoryPath = System.getenv("ODTHISTORY");
            if (HistoryPath == null) {
                HistoryPath = System.getenv("HOME");
            }
            historyFileSaveChooser.setCurrentDirectory(new File(HistoryPath));
            historyFileSaveChooser.setDialogTitle("Select ATCF History Listing File");
            int returnVal = historyFileSaveChooser.showSaveDialog(historyFileSaveChooser);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                saveFile = historyFileSaveChooser.getSelectedFile();
                writefileTF = true;
            } else if (returnVal == JFileChooser.CANCEL_OPTION) {
                // User has pressed cancel button
                writefileTF = false;
            }
            logger.debug("saving ATCF history listing file name={} writeTF={}", saveFile, writefileTF);
        } else {
            // call routine to generate ATCF file name for single analysis record
            logger.debug("stormID={} siteID={}", GUIATCFStormID, GUIATCFSiteID);
            ATCFOutputFileName = Functions.adt_atcffilename(GUIATCFStormID,GUIATCFSiteID);
            logger.debug("atcf output name={}*", ATCFOutputFileName);
            ATCFOutputFilePath = System.getenv("ODTOUTPUT");
            if (ATCFOutputFilePath == null) {
                ATCFOutputFilePath = System.getenv("HOME");
            }
            logger.debug("atcf output path={}*", ATCFOutputFilePath);
            saveFile = new File(ATCFOutputFilePath + "/" + ATCFOutputFileName);
            logger.debug("atcf output name={}*", saveFile.toString());
            writefileTF = true;
        }
        // call routine to output file
        ATCFFileOutput = History.ListHistory(outputstyle,GUIHistoryListFormat,GUIATCFSiteID,GUIATCFStormID);
        if (writefileTF) {
            try (FileWriter outFile = new FileWriter(saveFile)) {
                outFile.write(ATCFFileOutput);
                outFile.close();
                /* outFile.flush(); */
                ATCFMessage = String.format("Success writing ATCF file %s",saveFile);
            } catch (IOException ex) {
                ex.printStackTrace();
                ATCFMessage = String.format("Error writing ATCF file %s",saveFile);
            }
            System.out.printf(ATCFMessage);
            userMessage(ATCFMessage);
        }
    }
    
    private String selectForecastFile() {
        
        String fileNameReturn = null;
        
        logger.debug("in selectForecastFile");
        JFrame forecastFileFrame = new JFrame();
        JFileChooser forecastFileChooser = new JFileChooser();
        String forecastPath = System.getenv("ODTAUTO");
        if (forecastPath == null) {
            forecastPath = getLastPath("mcv.adt.lastforecastpath", System.getProperty("user.home"));
        }
        logger.debug("forecast path={}", forecastPath);
        forecastFileChooser.setCurrentDirectory(new File(forecastPath));
        forecastFileChooser.setDialogTitle("Select ADT Forecast File");
        int returnVal = forecastFileChooser.showOpenDialog(forecastFileFrame);
        logger.debug("retVal={}", returnVal);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = forecastFileChooser.getSelectedFile();
            fileNameReturn = file.getAbsolutePath();
            setLastPath("mcv.adt.lastforecastpath", file.getPath());
        } else {
            logger.error("error with file chooser");
        }
        return fileNameReturn;
    }
    
    private void getADTenvParameters() {
        History.InitCurrent(true);
        GUIHistoryFileName = null;

        /* load initial ADT Environmental parameters */
        GUIDeleteTF = Env.DeleteTF;
        GUIRunAutoTF = Env.AutoTF;
        GUIOverrideSceneTF = Env.OverSceneTF;
        GUIOverrideTF = Env.OverTF;
        GUIATCFOutputTF = Env.ATCFOutputTF;
        GUIATCFRecordOutputTF = Env.ATCFRecordOutputTF;
        GUIInitStrengthTF = Env.InitStrengthTF;
        GUILandFlagTF = Env.LandFlagTF;
        GUIUseCKZTF = Env.UseCKZTF;
        GUIVmax1or10TF = Env.Vmax1or10TF;
        GUICommentAddTF = Env.CommentAddTF;
        GUIPMWActivateTF = Env.UsePMWTF;
        
        /* integer values */
        GUIDomainID = Env.DomainID;
        GUIForecastType = Env.ForecastFileType;
        GUIMWJulianDate = Env.MWJulianDate;
        GUIMWHHMMSSTime = Env.MWHHMMSSTime;
        GUIStartDate = Env.StartJulianDate;
        GUIStartTime = Env.StartHHMMSSTime;
        GUIEndDate = Env.EndJulianDate;
        GUIEndTime = Env.EndHHMMSSTime;
        GUIHistoryListFormat = Env.HistoryListFormat;
        /* double values */
        GUIRawTValue = Env.InitRawTValue;
        GUIMWScore = Env.MWScore;
        GUICKZGaleRadius = Env.CKZGaleRadius;
        GUICKZPenv = Env.CKZPenv;
        GUIRMWSize = Env.RMWSize;
        GUIUserLatitude = Env.SelectedLatitude;
        GUIUserLongitude = Env.SelectedLongitude;
        
        GUIForecastFileName = Env.ForecastFileName;            // needed?
        GUIHistoryFileListingName = Env.ASCIIOutputFileName;   // needed?
        GUIATCFStormID = Env.StormIDString;
        GUIATCFSiteID = Env.ATCFSourceAgcyIDString;

    }

    private void loadADTenvParameters() {
        /* Env GlobalVariables = new Env(); */
        
        logger.debug("setting env parameters");
        
        // send ADT Environmental parameters to Env prior to running ADT
        // boolean values
        Env.DeleteTF = GUIDeleteTF;
        Env.AutoTF = GUIRunAutoTF;
        Env.OverTF = GUIOverrideTF;
        Env.ATCFOutputTF = GUIATCFOutputTF;
        Env.ATCFRecordOutputTF = GUIATCFRecordOutputTF;
        Env.InitStrengthTF = GUIInitStrengthTF;
        Env.LandFlagTF = GUILandFlagTF;
        Env.UseCKZTF = GUIUseCKZTF;
        Env.Vmax1or10TF = GUIVmax1or10TF;
        Env.CommentAddTF = GUICommentAddTF;
        Env.OverSceneTF = GUIOverrideSceneTF;
        Env.UsePMWTF = GUIPMWActivateTF;

        // integer values
        Env.DomainID = GUIDomainID;
        Env.ForecastFileType = GUIForecastType;
        Env.MWJulianDate = GUIMWJulianDate;
        Env.MWHHMMSSTime = GUIMWHHMMSSTime;
        Env.StartJulianDate = GUIStartDate;
        Env.StartHHMMSSTime = GUIStartTime;
        Env.EndJulianDate = GUIEndDate;
        Env.EndHHMMSSTime = GUIEndTime;
        Env.HistoryListFormat = GUIHistoryListFormat;
        // double values
        Env.InitRawTValue = GUIRawTValue;
        Env.MWScore = GUIMWScore;
        Env.CKZGaleRadius = GUICKZGaleRadius;
        Env.CKZPenv = GUICKZPenv;
        Env.RMWSize = GUIRMWSize;
        Env.SelectedLatitude = GUIUserLatitude;
        Env.SelectedLongitude = GUIUserLongitude;
        
        logger.debug("load forecast file name={}", GUIForecastFileName);
        Env.ForecastFileName = GUIForecastFileName;   // needed?
        Env.ASCIIOutputFileName = GUIHistoryFileListingName;   // needed?
        Env.StormIDString = GUIATCFStormID;
        Env.ATCFSourceAgcyIDString = GUIATCFSiteID;
        
    }
    
    private static int ReadGUIOverrideInputFile(String GUIOverrideFile) {
        
        logger.debug("opening file '{}'", GUIOverrideFile);
        
        File GUIDataFile = new File(GUIOverrideFile);
        String delims = "[ ]+";
        String line;
        int retval = 1;
        
        GUIOverrideTF = false;
        GUIOverrideSceneTF = false;
        GUICommentString = null;
        GUIRunAutoTF = true;
        GUIDeleteTF = false;
        GUICommentAddTF = false;
        GUIStartDate = 1900001;
        GUIStartTime = 000000;
        GUIEndDate = 1900001;
        GUIEndTime = 000000;
        GUIUserLatitude = -99.5;
        GUIUserLongitude = -999.5;
        GUIDomainID = 0;
        runFullADTAnalysis = true;
        
        try {
            Scanner GUIFile = new Scanner(GUIDataFile);
            while (GUIFile.hasNextLine()) {
                if ((line = GUIFile.nextLine()).isEmpty()){
                    break;
                } else {
                    String[] tokens = line.split(delims);
                    String IDstring = tokens[0];
                    String RecValue = tokens[1];
                    /* System.out.printf("scanning IDstring=%s\n",IDstring); */
                    switch (IDstring) {
                        case "ATCFOutputTF":
                            GUIATCFOutputTF = Boolean.valueOf(RecValue);
                            break;
                        case "ATCFRecordOutputTF":
                            GUIATCFRecordOutputTF = Boolean.valueOf(RecValue);
                            break;
                        case "InitStrengthTF":
                            GUIInitStrengthTF = Boolean.valueOf(RecValue);
                            break;
                        case "LandFlagTF":
                            GUILandFlagTF = Boolean.valueOf(RecValue);
                            break;
                        case "UseCKZTF":
                            GUIUseCKZTF = Boolean.valueOf(RecValue);
                            break;
                        case "Vmax1or10TF":
                            GUIVmax1or10TF = Boolean.valueOf(RecValue);
                            break;
                        case "UsePMWTF":
                            GUIPMWActivateTF = Boolean.valueOf(RecValue);
                            break;
                        case "ForecastType":
                            GUIForecastType = Integer.valueOf(RecValue);
                            break;
                        case "MWJulianDate":
                            GUIMWJulianDate = Integer.valueOf(RecValue);
                            break;
                        case "MWHHMMSSTime":
                            GUIMWHHMMSSTime = Integer.valueOf(RecValue);
                            break;
                        case "HistoryListFormat":
                            GUIHistoryListFormat = Integer.valueOf(RecValue);
                            break;
                        case "RawTValue":
                            GUIRawTValue = Double.valueOf(RecValue);
                            break;
                        case "MWScore":
                            GUIMWScore = Double.valueOf(RecValue);
                            break;
                        case "CKZGaleRadius":
                            GUICKZGaleRadius = Double.valueOf(RecValue);
                            break;
                        case "CKZPenv":
                            GUICKZPenv = Double.valueOf(RecValue);
                            break;
                        case "RMWSize":
                            GUIRMWSize = Double.valueOf(RecValue);
                            break;
                        case "HistoryFileName":
                            GUIHistoryFileName = RecValue;
                            break;
                        case "ForecastFileName":
                            GUIForecastFileName = RecValue;
                            break;
                        case "HistoryFileListingName":
                            GUIHistoryFileListingName = RecValue;
                            break;
                        case "ATCFStormID":
                            GUIATCFStormID = RecValue;
                            break;
                        case "ATCFSiteID":
                            GUIATCFSiteID = RecValue;
                            break;
                        default:
                            break;
                    }
                }
            }
            GUIFile.close();
        } catch (IOException ex) {
            retval = -1;
        }
        return retval;
    }
    
    public void latLonWidgetChanged() {
        logger.debug("latlonwidgetchanged called");
        try {
            logger.debug("latlon widget changed");
            String message = latLonWidget.isValidValues();
            if (message != null) {
                    userMessage(message);
                    return;
            }
            probeLocation = ucar.visad.Util.makeEarthLocation(
                            latLonWidget.getLat(), latLonWidget.getLon()).getLatLonPoint();
        } catch (Exception e) {
            logException("Handling LatLonWidget changed", e);
        }
    }
    
    protected boolean shouldAddDisplayListener() {
        return true;
    }
    
    protected boolean shouldAddControlListener() {
        return true;
    }

    protected boolean canHandleEvents() {
        if (!getHaveInitialized() || (getMakeWindow() && !getWindowVisible())) {
            return false;
        }
        return isGuiShown();
    }
    
    public void handleDisplayChanged(DisplayEvent event) {
        super.handleDisplayChanged(event);
        if (canHandleEvents()) {
            int id = event.getId();
            // String idstring = event.toString();
            // InputEvent inputEvent = event.getInputEvent();
            // System.out.printf("event ID=%d %s\n",id,idstring);
            try {
                if (id == DisplayEvent.MOUSE_PRESSED_LEFT) {
                    logger.debug("Manual Position Selection");
                    probeLocation = toEarth(event).getLatLonPoint();
                    updateProbeLocation();
                }
            } catch (Exception e) {
                logException("Error selecting position with mouse", e);
            }
        }
    }
    
    private void updateProbeLocation() {
        try {
            if (probeLocation == null) {
                return;
            }
            double lon = probeLocation.getLongitude().getValue(CommonUnit.degree);
            double lat = probeLocation.getLatitude().getValue(CommonUnit.degree);
            probe.setPosition(
                new RealTuple(RealTupleType.SpatialEarth3DTuple, new double[] { lon, lat, 0 }));
            probe.setVisible(true);
            
            GUIUserLatitude = lat;    // added TLO
            GUIUserLongitude = lon;    // added TLO
            logger.debug("set lat/lon from probe at lat={} lon={}", GUIUserLatitude, GUIUserLongitude);
            if (latLonWidget != null) {
                latLonWidget.setLat(getDisplayConventions().formatLatLon(
                                probeLocation.getLatitude().getValue(CommonUnit.degree)));
                latLonWidget.setLon(getDisplayConventions().formatLatLon(
                                probeLocation.getLongitude().getValue(CommonUnit.degree)));
            }
        } catch (Exception e) {
            logException("Handling probe changed", e);
        }
    }

    /**
     * Set the ProbeLocation property.
     *
     * @param value New value for ProbeLocation.
     */
    public void setProbeLocation(LatLonPoint value) {
        probeLocation = value;
    }

    /**
     * Get the ProbeLocation property.
     *
     * @return The ProbeLocation
     */
    public LatLonPoint getProbeLocation() {
            return probeLocation;
    }
        
    protected FlatField getFlatField(FieldImpl data)
        throws VisADException, RemoteException
    {
        FlatField ff;
        if (GridUtil.isSequence(data)) {
            ff = (FlatField)data.getSample(0);
        } else {
            ff = (FlatField)data;
        }
        return ff;
    }

    public EarthLocation toEarth(DisplayEvent event)
        throws VisADException, RemoteException
    {
        NavigatedDisplay d = getNavigatedDisplay();
        return (d == null) ? null : d.getEarthLocation(toBox(event));
    }

    private void GetImageDateTime() {
        
        RealTuple timeTuple;
        Real tt;
        DateTime dat;
        
        List infos = getDisplayInfos();
        DisplayInfo displayInfo = (DisplayInfo) infos.get(0);
        
        try {
            Animation anime = displayInfo.getViewManager().getAnimation();
            Set timeSet = anime.getSet();
            int pos = anime.getCurrent();
            
            timeTuple = DataUtility.getSample(timeSet, pos);
            tt = (Real) timeTuple.getComponent(0);
            dat = new DateTime(tt);
        } catch (VisADException e) {
            logException("Handling data", e);
            return;
        } catch (RemoteException f) {
            logger.warn("Something went wrong!", f);
            return;
        }
        
        double curdate = dat.getValue();
        logger.debug("curdate={}",curdate);
        
        Date datevalue = new Date((long)curdate*1000);
        
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyyDDD");
        SimpleDateFormat timeformat = new SimpleDateFormat("HHmmss");
        dateformat.setTimeZone(TimeZone.getTimeZone("GMT"));
        timeformat.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        String JulianDate = dateformat.format(datevalue);
        String HHMMSSTime = timeformat.format(datevalue);
        int ImageDateInt = Integer.valueOf(JulianDate);
        int ImageTimeInt = Integer.valueOf(HHMMSSTime);
        // System.out.printf("image date = %d  image time=%d\n",ImageDateInt,ImageTimeInt); */
        
        Data.IRData_JulianDate = ImageDateInt;
        Data.IRData_HHMMSSTime = ImageTimeInt;
        
        logger.debug("IMAGE DATE={} TIME={}", Data.IRData_JulianDate, Data.IRData_HHMMSSTime);
    }
    
    private void GetImageData(float CenterLatitude, float CenterLongitude) {
        logger.debug("creating ReadIRImage()...");
        
        // ReadIRImage IRImage = new ReadIRImage();
        
        FlatField ffield;
        int SatelliteID;
        int channel;
        
        List sources = new ArrayList();
        
        logger.debug("entering getimagedata");
        boolean isTemp = false;
        choice.getDataSources(sources);
        try {
            List infos = getDisplayInfos();
            DataInstance de = getDataInstance();
            DisplayInfo displayInfo = (DisplayInfo) infos.get(0);
                
            Animation anime = displayInfo.getViewManager().getAnimation();
            // Set timeSet = anime.getSet();
            int pos = anime.getCurrent();
            ffield = DataUtil.getFlatField(de.getData());
            DataSourceImpl dsi = (DataSourceImpl) sources.get(0);
            
            if (dsi instanceof AddeImageDataSource) {
                ImageDataSource dds = (ImageDataSource) sources.get(0);
                List imageLists = dds.getImageList();
                
                AddeImageDescriptor aid = (AddeImageDescriptor) imageLists.get(pos);
                AreaDirectory ad = aid.getDirectory();
                SatelliteID = ad.getSensorID();
                int[] bands = ad.getBands();
                channel = bands[0];
                
                isTemp = Util.isCompatible(ffield, AirTemperature.getRealType());
            } else {
                channel = 4;
                SatelliteID = 70;
                // String name = ffield.getSample(0).getType().prettyString();
            }
        } catch (VisADException e) {
            logException("Handling data", e);
            return;
        } catch (RemoteException f) {
            logger.warn("Something went wrong!", f);
            return;
        }
            
        // String shortName = choice.getName();
        
        Env.UserDefineDomain = 0; // automated
        // String sidName = Functions.adt_sattypes(SatelliteID);
        
        logger.debug("SatelliteID={}", SatelliteID);
        
        try {
            ReadIRImage.ReadIRDataFile(ffield,
                                       CenterLatitude,
                                       CenterLongitude,
                                       SatelliteID,
                                       channel,
                                       isTemp);
        }
        catch (Exception ex) {
            logger.error("ReadIRImage failed", ex);
        }
    }
}
