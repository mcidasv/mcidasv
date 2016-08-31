/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2016
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
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
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

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
import ucar.visad.Util;
import ucar.visad.display.Animation;
import ucar.visad.display.CompositeDisplayable;
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
 * 
 * Description of what this class provides...
 * 
 * @author Tim Olander
 *
 */

@SuppressWarnings({"unused", "static-access", "unchecked", "rawtypes"})

public class ADTControl extends DisplayControlImpl {
    
    private static final Logger logger = LoggerFactory.getLogger(ADTControl.class);

    /** _more_ */
    private LatLonWidget latLonWidget;

    /** the probe */
    private PointProbe probe;
    
    /** _more_ */
    private LatLonPoint probeLocation;
    
    /** _more_ */
    private CompositeDisplayable holder;
    
    /** _more_ */
    private DataChoice choice;
    
    /** _more_ */
    private static boolean runOnClick = false;
    
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
    private static boolean GUIUseCKZTF;
    private static boolean GUIVmax1or10TF;
    private static boolean GUICommentAddTF;
    private static boolean GUIDeleteTF;
    private static boolean GUIATCFRecordOutputTF;
    private static boolean GUIPMWActivateTF;
    
    private static int GUIDomainID;           // need to determine or provide option
    private static int GUIForecastType = 0;   // need to initialize pulldown menu
    private static int GUIMWJulianDate;
    private static int GUIMWHHMMSSTime;
    private static int GUIStartDate;
    private static int GUIStartTime;
    private static int GUIEndDate;
    private static int GUIEndTime;
    private static int GUIHistoryListFormat;
    private static int GUIOverrideSceneType;
    
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
    private static String GUIATCFFileOutputName;
    private static String GUICommentString;

    /** _more_ */
    private JButton adtBtn;
    private JButton historyBtn;
    private JButton forecastBtn;
    private JButton listBtn;
    private JButton PMWFileBtn;
    
    /** _more_ */
    private JComboBox forecastTypeBox;
    
    /** _more_ */
    private JRadioButton mslpDvorakButton;
    private JRadioButton mslpCKZButton;
    private JRadioButton OverrideYESButton;
    private JRadioButton OverrideNOButton;
    private JRadioButton LandONButton;
    private JRadioButton LandOFFButton;
    private JRadioButton V1MinButton;
    private JRadioButton V10MinButton;
    private JRadioButton PMWAutoButton;
    private JRadioButton PMWManButton;
    private JButton overrideSceneAcceptButton;
    private JButton overrideSceneCancelButton;

    private int overrideSceneTypeIndex = 0;

    private JFrame resultFrame;
    private JTextArea resultArea;
    private JFrame historyFrame;
    private JTextArea historyArea;
    private JFrame historyFileFrame;
    private JFileChooser historyFileChooser;
    private JTextField historyTextField;
    private JButton historySaveListingBtn;
    private JButton historyEditBtn;
    private JButton historyWriteATCFBtn;
    private JFileChooser historyFileSaveChooser;
    private JTextField automanTextField;
    private JTextField forecastTextField;
    private JFileChooser forecastFileChooser;
    private JTextField ckzPenvTextField;
    private JTextField ckz34radiusTextField;
    private JTextField RawTTextField;
    private JTextField RMWTextField;
    private JTextField pmwManDateTextField;
    private JTextField pmwManTimeTextField;
    private JTextField pmwManScoreTextField;
    private JLabel pmwManDateLabel;
    private JLabel pmwManTimeLabel;
    private JLabel pmwManScoreLabel;
    private JFileChooser PMWFileChooser;
    private Container historyDateContainer; 
    private JFrame historyDateFrame;
    private JPanel historyDatePanel;
    private JPanel historyDateStartPanel;
    private JPanel historyDateEndPanel;
    private JLabel historyDateStartLabel;
    private JLabel historyDateEndLabel;
    private JLabel historyDateStartDateLabel;
    private JLabel historyDateStartTimeLabel;
    private JLabel historyDateEndDateLabel;
    private JLabel historyDateEndTimeLabel;
    private JPanel historyDateButtonPanel;
    private JRadioButton historyEditDeleteButton;
    private JRadioButton historyEditAddCommentButton;
    private JTextField historyEditAddCommentTextField;
    private JLabel historyEditAddCommentLabel;
    private JPanel historyEditInputPanel;
    private JButton historyEditApplyButton;
    private JButton historyEditCancelButton;
    private Container historyContainer;
    private JPanel historyLabelPanel;
    private JPanel historyTextPanel;
    private JPanel historyButtonPanel;
    private JFrame overrideSceneFrame;
    private Container overrideSceneContainer;
    private JPanel overrideSceneCurrentPanel;
    private JLabel overrideSceneCurrentLabel;
    private JPanel overrideSceneSelectPanel;
    private JLabel overrideSceneSelectLabel;
    private JTextField overrideSceneCurrentTextField;
    private JComboBox overrideSceneTypeBox;
    private JPanel overrideSceneButtonPanel;
    private JLabel ATCFOutputLabel;
    private JLabel ATCFEntryStormLabel;
    private JLabel ATCFEntrySiteLabel;
    private JTextField ATCFEntryStormTextField;
    private JTextField ATCFEntrySiteTextField;
    private JCheckBox ATCFOutputButton;
    private JCheckBox PMWActivateButton;
    private JCheckBox GUIFileOverrideButton;

    private JTextField historyDateStartDateTextField;
    private JTextField historyDateStartTimeTextField;
    private JTextField historyDateEndDateTextField;
    private JTextField historyDateEndTimeTextField;
    
    private JLabel autoStormSelectLabel;
    private JLabel forecastSelectLabel;
    private JLabel historyLabel;
    private JLabel ckzPenvLabel;
    private JLabel ckz34radiusLabel;
    
    private static String HistoryListOutput;
        
    private static String GUIPMWFileName;
    private String[] SceneTypeStrings = { "Eye", "Pinhole Eye","Large Eye","CDO","Embedded Center",
                                          "Irregular CDO","Curved Band","Shear"};

    
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

        probe.setManipulable(false);
        probe.setVisible(false);
        probe.setAutoSize(true);

        probe.setPointSize(getDisplayScale());
        addDisplayable(probe, FLAG_COLOR);
        
        /* obtain initial ADT environmental parameters */
        getADTenvParameters();
        
        /* setup window contents in Controls Window */
        setContents(setupMainWindow());
        updateProbeLocation();
        return true;

    }

    public Container setupMainWindow() {

        /* add Lat/Lon position display text areas */  
        latLonWidget = new LatLonWidget(GuiUtils.makeActionListener(this,
                        "latLonWidgetChanged", null));
        JPanel latlonPanel = GuiUtils.hbox(Misc.newList(latLonWidget));

        /* add Manual or Automated storm centering buttons */
        automanTextField = new JTextField("Select Storm Center In Image",25);
        JRadioButton manButton = new JRadioButton("Manual");
        manButton.setActionCommand("Manual");
        manButton.setSelected(true);
        JRadioButton autoButton = new JRadioButton("Automated");
        autoButton.setActionCommand("Automated");
        autoButton.setSelected(false);
        ButtonGroup automangroup = new ButtonGroup();
        automangroup.add(manButton);
        automangroup.add(autoButton);
        manButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                updateAutoManText(1);
                GUIRunAutoTF = false;
            }
        });
        autoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                updateAutoManText(2);
                GUIRunAutoTF = true;
                System.out.printf("running automated ADT!!!\n");
            }
        });
        /* add forecast file file selector button and file type menu */ 
        autoStormSelectLabel = new JLabel("AUTOMATED STORM SELECTION");
        forecastTextField = new JTextField("No forecast file selected yet",40);
        
        forecastBtn = new JButton("Select Forecast File");
        forecastBtn.setPreferredSize(new Dimension(200,30));
        forecastBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent fbtn) {
                GUIForecastFileName = selectForecastFile(); 
                System.out.printf("forecast file name=%s\n",GUIForecastFileName);
                updateForecastTextField();
            }
        });
        String[] forecastTypeStrings = { "ATCF", "DISC","PACWARN","GENERIC","RMSC ICAO",
                                         "RMSC WTIO","TCWC AXAU","(empty)","BEST","HURDAT"};
        forecastTypeBox = new JComboBox(forecastTypeStrings);
        forecastTypeBox.setSelectedIndex(GUIForecastType);
        forecastTypeBox.setPreferredSize(new Dimension(150,20));
        forecastTypeBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ame) {
                    GUIForecastType = forecastTypeBox.getSelectedIndex();
                    System.out.printf("forecast file type=%d\n",GUIForecastType);
                }
        });
        forecastSelectLabel = new JLabel("Selected Forecast File:");
        forecastTypeBox.setToolTipText("Select Forecast File type.");
        autoStormSelectLabel.setEnabled(false);
        forecastSelectLabel.setEnabled(false);
        forecastTextField.setEnabled(false);
        forecastBtn.setEnabled(false);
        forecastTypeBox.setEnabled(false);

        /* define default history file text field message */
        historyTextField = new JTextField("No history file selected yet",40);

        /* add history file selection button */
        historyBtn = new JButton("Select History File");
        historyBtn.setPreferredSize(new Dimension(200,30));
        historyBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent hbtn) {
                GUIHistoryFileName = selectHistoryFile(); 
                System.out.printf("history file name=%s\n",GUIHistoryFileName);
                runFullADTAnalysis = true;
                updateHistoryTextField();
            }
        });

        /* add main ADT analysis start button */
        adtBtn = new JButton("Run ADT Analysis");
        adtBtn.setPreferredSize(new Dimension(250,50));
//        adtBtn.setBackground(Color.BLUE);
//        adtBtn.setForeground(Color.WHITE);
        adtBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                runADTmain();
            }
        });
        
        /* add history file list/edit button */
        listBtn = new JButton("List/Edit History File");
        listBtn.setPreferredSize(new Dimension(250,50));
//        listBtn.setBackground(Color.GREEN);
//        listBtn.setForeground(Color.WHITE);
        listBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                System.out.printf("listing history file name=%s\n",GUIHistoryFileName);
                listHistoryFile();
            }
        });
        
        /* Add PMW Analysis option buttons and entry fields */
        PMWActivateButton = new JCheckBox("Activate");
        PMWActivateButton.setActionCommand("PMW");
        PMWActivateButton.setSelected(false);
        PMWActivateButton.setEnabled(true);
        PMWActivateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                PMWActivateCheckBoxToggle();
            }
        });
        PMWAutoButton = new JRadioButton("Automated");
        PMWAutoButton.setActionCommand("Auto");
        PMWAutoButton.setSelected(false);
        PMWAutoButton.setEnabled(false);
        PMWManButton = new JRadioButton("Manual");
        PMWManButton.setActionCommand("Man");
        PMWManButton.setSelected(false);
        PMWManButton.setEnabled(false);
        ButtonGroup pmwgroup = new ButtonGroup();
        pmwgroup.add(PMWAutoButton);
        pmwgroup.add(PMWManButton);
        PMWAutoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                /* enter file name */
                updatePMWFields(1);
            }
        });
        PMWManButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                /* enter date/time and score manually */
                updatePMWFields(2);
            }
        });
        /* PMW Automated option */
        PMWFileBtn = new JButton("Select PMW Input File");
        PMWFileBtn.setPreferredSize(new Dimension(200,30));
        PMWFileBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent hbtn) {
                GUIPMWFileName = selectPMWFile(); 
                System.out.printf("PMW file name=%s\n",GUIPMWFileName);
            }
        });
        PMWFileBtn.setEnabled(false);
        
        /* PMW Manual options */
        pmwManDateLabel = new JLabel("Date:");
        pmwManTimeLabel = new JLabel("Time:");
        pmwManScoreLabel = new JLabel("Score:");
        pmwManDateTextField = new JTextField("1900JAN01",8);
        pmwManDateTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                /* read PMW overpass date */
                GUIMWJulianDate = readPMWManualDateField();
                GUIMWScore = -99.0;
            }
        });
        pmwManTimeTextField = new JTextField("000000",6);
        pmwManTimeTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                /* read PMW overpass time */
                GUIMWHHMMSSTime = readPMWManualTimeField();
                GUIMWScore = -99.0;
            }
        });
        pmwManScoreTextField = new JTextField("-99.0",4);
        pmwManScoreTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                /* read PMW overpass score */
                GUIMWScore = readPMWManualScoreField();
            }
        });
        pmwManDateTextField.setEnabled(false);
        pmwManTimeTextField.setEnabled(false);
        pmwManScoreTextField.setEnabled(false);
        pmwManDateLabel.setEnabled(false);
        pmwManTimeLabel.setEnabled(false);
        pmwManScoreLabel.setEnabled(false);
        
        /* add CKZ option buttons and entry fields */
        mslpDvorakButton = new JRadioButton("Dvorak");
        mslpDvorakButton.setActionCommand("Dvorak");
        mslpDvorakButton.setSelected(true);
        mslpCKZButton = new JRadioButton("CKZ");
        mslpCKZButton.setActionCommand("CKZ");
        mslpCKZButton.setSelected(false);
        ButtonGroup mslpgroup = new ButtonGroup();
        mslpgroup.add(manButton);
        mslpgroup.add(autoButton);
        mslpDvorakButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                updateMSLPFields(1);
                GUIUseCKZTF = false;
            }
        });
        mslpCKZButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                updateMSLPFields(2);
                GUIUseCKZTF = true;
            }
        });
        ckzPenvLabel = new JLabel("Penv:");
        ckz34radiusLabel = new JLabel("34kt Radius:");
        ckzPenvTextField = new JTextField("1012",5);
        ckzPenvTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GUICKZPenv = readCKZTextFields(1);
            }
        });
        ckz34radiusTextField = new JTextField("300",5);
        ckz34radiusTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GUICKZGaleRadius = readCKZTextFields(2);
            }
        });
        ckzPenvLabel.setEnabled(false);
        ckz34radiusLabel.setEnabled(false);
        ckzPenvTextField.setEnabled(false);
        ckz34radiusTextField.setEnabled(false);
        
        /* various other keyword options */
        /* Initial classification entry -- RAWT */
        JLabel RawTLabel = new JLabel("Raw T:");
        RawTTextField = new JTextField("1.0",4);
        RawTTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GUIRawTValue = readRawTTextFields();
                if(GUIRawTValue>=1.0) {
                    GUIInitStrengthTF = true;
                } else {
                    GUIInitStrengthTF = false;
                }
            }
        });
        
           /* Radius of Max Wind entry -- RMW */
        JLabel RMWLabel = new JLabel("RMW:");
        RMWTextField = new JTextField("-99",4);
        RMWTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GUIRMWSize = readRMWTextFields();
            }
        });
        
        /* Override option */
        JLabel OverrideLabel = new JLabel("Manual Scene Override:");
        OverrideYESButton = new JRadioButton("YES");
        OverrideYESButton.setActionCommand("Yes");
        OverrideYESButton.setSelected(false);
        OverrideNOButton = new JRadioButton("NO");
        OverrideNOButton.setActionCommand("No");
        OverrideNOButton.setSelected(true);
        ButtonGroup overridegroup = new ButtonGroup();
        overridegroup.add(OverrideNOButton);
        overridegroup.add(OverrideYESButton);

        OverrideYESButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GUIOverrideTF = true;
                GUIOverrideSceneTF = true;
                   OverrideNOButton.setSelected(false);
                OverrideYESButton.setSelected(true);
            }
        });

        OverrideNOButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GUIOverrideTF = false;
                GUIOverrideSceneTF = false;
                OverrideNOButton.setSelected(true);
                OverrideYESButton.setSelected(false);
            }
        });
        
        /* ATCF Analysis Output Checkbox */
        JLabel ATCFOutputLabel = new JLabel("ATCF Output:");
        ATCFOutputButton = new JCheckBox("Activate");
        ATCFOutputButton.setActionCommand("ATCF");
        ATCFOutputButton.setSelected(false);
        ATCFOutputButton.setEnabled(true);
        ATCFOutputButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                ATCFRecordOutputCheckBoxToggle();
            }
        });
        ATCFEntryStormLabel = new JLabel("Storm ID:");
        ATCFEntryStormTextField = new JTextField("XXX",8);
        ATCFEntrySiteLabel = new JLabel("Site ID:");
        ATCFEntrySiteTextField = new JTextField("XXXX",8);
        ATCFEntryStormLabel.setEnabled(false);
        ATCFEntryStormTextField.setEnabled(false);
        ATCFEntrySiteLabel.setEnabled(false);
        ATCFEntrySiteTextField.setEnabled(false);
        ATCFEntryStormTextField.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent ae) {
                GUIATCFStormID = readATCFStringField(1);
            }
        });
        ATCFEntrySiteTextField.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent ae) {
                GUIATCFSiteID = readATCFStringField(2);
            }
        });
        
        /* Land Flag button -- LAND */
        JLabel LandFlagLabel = new JLabel("Land Flag:");
        LandONButton = new JRadioButton("ON");
        LandONButton.setActionCommand("On");
        LandONButton.setSelected(true);
        LandOFFButton = new JRadioButton("OFF");
        LandOFFButton.setActionCommand("Off");
        LandOFFButton.setSelected(false);
        ButtonGroup landgroup = new ButtonGroup();
        landgroup.add(LandONButton);
        landgroup.add(LandOFFButton);
        LandONButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                updateLandButton(1);
                GUILandFlagTF = true;
            }
        });
        LandOFFButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                updateLandButton(2);
                GUILandFlagTF = false;
            }
        });
        
        /*  Wind Speed Vmax output button -- VOUT */
        JLabel VOutLabel = new JLabel("VMax:");
        V1MinButton = new JRadioButton("One-minute");
        V1MinButton.setActionCommand("One");
        V1MinButton.setSelected(true);
        V10MinButton = new JRadioButton("Ten-minute");
        V10MinButton.setActionCommand("Ten");
        V10MinButton.setSelected(true);
        ButtonGroup voutgroup = new ButtonGroup();
        voutgroup.add(V1MinButton);
        voutgroup.add(V10MinButton);
        V1MinButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                updateVOutButton(1);
                GUIVmax1or10TF = true;
            }
        });
        V10MinButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                updateVOutButton(2);
                GUIVmax1or10TF = false;
            }
        });
        
        GUIFileOverrideButton = new JCheckBox("GUI File Override");
        GUIFileOverrideButton.setActionCommand("Override");
        GUIFileOverrideButton.setSelected(false);
        GUIFileOverrideButton.setEnabled(true);
        GUIFileOverrideButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GUIFileOverrideCheckBoxToggle();
            }
        });
        JLabel blankfield = new JLabel("");

        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent widgets = GuiUtils.formLayout(new Component[] {
                                GuiUtils.left(GuiUtils.hbox(new Component[] {new JLabel("Storm Center Selection:"), 
                                               manButton, autoButton, automanTextField }, 5)),GuiUtils.filler(),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {new JLabel("MANUAL STORM SELECTION")}, 10)),GuiUtils.filler(),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {GuiUtils.filler(30,1),latlonPanel})),GuiUtils.filler(),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {autoStormSelectLabel}, 10)),GuiUtils.filler(),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {GuiUtils.filler(30,1),forecastBtn, forecastTypeBox, forecastSelectLabel, forecastTextField },5)),GuiUtils.filler(),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {blankfield})),GuiUtils.filler(1,10),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {new JLabel("HISTORY FILE INFORMATION")}, 10)),GuiUtils.filler(),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {GuiUtils.filler(30,1),historyBtn ,new JLabel("Selected History File:"), historyTextField },5)),GuiUtils.filler(),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {blankfield})),GuiUtils.filler(1,10),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {new JLabel("PMW ANALYSIS")}, 10)),GuiUtils.filler(),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {GuiUtils.filler(30,1),PMWActivateButton,PMWManButton,
                                           pmwManDateLabel,pmwManDateTextField,pmwManTimeLabel,pmwManTimeTextField,
                                           pmwManScoreLabel,pmwManScoreTextField,PMWAutoButton, PMWFileBtn }, 5)),GuiUtils.filler(),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {blankfield})),GuiUtils.filler(1,10),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {new JLabel("MISCELLANEOUS OPTIONS")}, 10)),GuiUtils.filler(),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {GuiUtils.filler(30,1),new JLabel("MSLP Conversion Method:"), 
                                               mslpDvorakButton, mslpCKZButton, ckzPenvLabel, ckzPenvTextField, 
                                            ckz34radiusLabel,ckz34radiusTextField }, 5)),GuiUtils.filler(),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {GuiUtils.filler(30,1),OverrideLabel,OverrideNOButton,OverrideYESButton }, 5)),GuiUtils.filler(),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {GuiUtils.filler(30,1),LandFlagLabel,LandONButton, LandOFFButton, GuiUtils.filler(20,1),
                                             VOutLabel, V1MinButton, V10MinButton, GuiUtils.filler(20,1),RawTLabel,RawTTextField, RMWLabel, RMWTextField }, 5)),GuiUtils.filler(),
                                  GuiUtils.left(GuiUtils.hbox(new Component[] {GuiUtils.filler(30,1),ATCFOutputLabel,ATCFOutputButton,ATCFEntryStormLabel,ATCFEntryStormTextField,ATCFEntrySiteLabel,ATCFEntrySiteTextField}, 5)),GuiUtils.filler(),
                                GuiUtils.left(GuiUtils.hbox(new Component[] {GuiUtils.filler(100,1),adtBtn,listBtn,GUIFileOverrideButton},50)),GuiUtils.filler()
                                });      
        JPanel controls = GuiUtils.topLeft(widgets);

        /* set up ADT Bulletin display area */
        resultArea = new JTextArea();
        JScrollPane resultScroller = new JScrollPane(resultArea);
        resultScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        resultScroller.setPreferredSize(new Dimension(400, 200));

        Font c = new Font("Courier", Font.BOLD, 12);
        Font m = new Font("Monospaced", Font.PLAIN, 12);
        
        resultFrame = new JFrame("ADT Results");
        resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        resultFrame.add(resultArea,BorderLayout.CENTER);
        resultFrame.setPreferredSize(new Dimension(400, 600));
//        resultFrame.setBackground(Color.WHITE);
//        resultFrame.setForeground(Color.BLACK);
        resultFrame.setFont(c);

        /* set up ADT History File display area */
        historyFrame = new JFrame("ADT History File Listing");
        historyContainer = historyFrame.getContentPane();
        historyFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        historyTextPanel = new JPanel();
        FlowLayout historyListLayout = new FlowLayout();
        historyTextPanel.setLayout(historyListLayout);
        historyListLayout.setAlignment(FlowLayout.CENTER);
           
        historyArea = new JTextArea(50,150);
        historyArea.setEditable(false);
        JScrollPane historyScroller = new JScrollPane(historyArea);
        historyScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        historyScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        historyScroller.setPreferredSize(new Dimension(1200, 400));
//        historyArea.setBackground(Color.WHITE);
//        historyArea.setForeground(Color.DARK_GRAY);
        historyArea.setFont(c);

        historyLabelPanel = new JPanel();
        FlowLayout HistoryLabelLayout = new FlowLayout();
        historyLabelPanel.setLayout(HistoryLabelLayout);
        HistoryLabelLayout.setAlignment(FlowLayout.CENTER);
        historyLabel = new JLabel("No History File Selected");
        historyLabel.setPreferredSize(new Dimension(800,20));
//        historyLabel.setBackground(Color.WHITE);
//        historyLabel.setForeground(Color.BLACK);
        historyLabel.setFont(c);
        
        historyButtonPanel = new JPanel();
        FlowLayout HistoryButtonLayout = new FlowLayout();
        historyButtonPanel.setLayout(HistoryButtonLayout);
        HistoryButtonLayout.setAlignment(FlowLayout.CENTER);
        historyEditBtn = new JButton("Edit History File");
        historyEditBtn.setPreferredSize(new Dimension(200,20));
        historyEditBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                editHistoryFileWindow(); 
                System.out.printf("editing history file name=%s\n",GUIHistoryFileName);
            }
        });
        historySaveListingBtn = new JButton("Output Listing");
        historySaveListingBtn.setPreferredSize(new Dimension(200,20));
        historySaveListingBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GUIHistoryFileListingName = selectHistoryFileOutput(); 
                System.out.printf("saving history listing file name=%s\n",GUIHistoryFileListingName);
                GUIHistoryListFormat = -1;
            }
        });
        historyWriteATCFBtn = new JButton("Write ATCF");
        historyWriteATCFBtn.setPreferredSize(new Dimension(200,20));
        historyWriteATCFBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GUIATCFOutputTF = true;
                GUIHistoryListFormat = 0;
                System.out.printf("calling ATCFFileOutput\n");
                ATCFFileOutput(0);
            }
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
               
        /* history file Editing Date Selection window */
        historyDateFrame = new JFrame("History File Editor");
        historyDateContainer = historyDateFrame.getContentPane();
        historyDateFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        historyDatePanel = new JPanel();
        FlowLayout DateStartEndLayout = new FlowLayout();
        historyDatePanel.setLayout(DateStartEndLayout);
        DateStartEndLayout.setAlignment(FlowLayout.CENTER);
        historyDateStartLabel = new JLabel("Start:");
        historyDateStartDateLabel = new JLabel("Date");
        historyDateStartDateTextField = new JTextField("0000XXX00",10);
        historyDateStartTimeLabel = new JLabel("Time");
        historyDateStartTimeTextField = new JTextField("-1",8);
        historyDateEndLabel = new JLabel("End");
        historyDateEndDateLabel = new JLabel("Date");
        historyDateEndDateTextField = new JTextField("0000XXX00",10);
        historyDateEndTimeLabel = new JLabel("Time");
        historyDateEndTimeTextField = new JTextField("-1",8);
        historyDateStartDateTextField.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent ae) {
                GUIStartDate = readDateTextStringField(1);
            }
        });
        historyDateStartTimeTextField.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent ae) {
                GUIStartTime = readDateTextStringField(2);
            }
        });
        historyDateEndDateTextField.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent ae) {
                GUIEndDate = readDateTextStringField(3);
            }
        });
        historyDateEndTimeTextField.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent ae) {
                GUIEndTime = readDateTextStringField(4);
            }
        });
        historyDateButtonPanel = new JPanel();
        FlowLayout DateButtonLayout = new FlowLayout();
        historyDateButtonPanel.setLayout(DateButtonLayout);
        DateButtonLayout.setAlignment(FlowLayout.CENTER);
        historyEditDeleteButton = new JRadioButton("Delete Records");
        historyEditDeleteButton.setActionCommand("Delete");
        historyEditDeleteButton.setSelected(false);
        historyEditAddCommentButton = new JRadioButton("Add Comment");
        historyEditAddCommentButton.setActionCommand("Comment");
        historyEditAddCommentButton.setSelected(false);
        ButtonGroup editgroup = new ButtonGroup();
        editgroup.add(historyEditDeleteButton);
        editgroup.add(historyEditAddCommentButton);
        historyEditAddCommentLabel = new JLabel("Comment:");
        historyEditAddCommentTextField = new JTextField("no comment entered",25);
        historyEditAddCommentTextField.setEnabled(false);
        historyEditDeleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                updateHistoryEditButton(1);
            }
        });
        historyEditAddCommentButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                updateHistoryEditButton(2);
            }
        });
        historyEditAddCommentTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GUICommentString = readCommentTextField();
            }
        });
        historyEditInputPanel = new JPanel();
        FlowLayout EditInputButtonLayout = new FlowLayout();
        historyEditInputPanel.setLayout(EditInputButtonLayout);
        EditInputButtonLayout.setAlignment(FlowLayout.CENTER);
        historyEditApplyButton = new JButton("Apply Edits");
        historyEditApplyButton.setPreferredSize(new Dimension(150,20));
        historyEditApplyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                modifyHistoryFile();
            }
        });
        historyEditCancelButton = new JButton("Cancel");
        historyEditCancelButton.setPreferredSize(new Dimension(150,20));
        historyEditCancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                historyDateFrame.dispose();
            }
        });
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
        overrideSceneContainer = overrideSceneFrame.getContentPane();
        overrideSceneFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        overrideSceneCurrentPanel = new JPanel();
        FlowLayout OverrideSceneCurrentLayout = new FlowLayout();
        overrideSceneCurrentPanel.setLayout(OverrideSceneCurrentLayout);
        OverrideSceneCurrentLayout.setAlignment(FlowLayout.CENTER);
        overrideSceneCurrentLabel = new JLabel("Current Scene Type:");
        overrideSceneCurrentTextField = new JTextField("N/A",10);
        overrideSceneSelectPanel = new JPanel();
        FlowLayout OverrideSceneSelectLayout = new FlowLayout();
        overrideSceneCurrentPanel.setLayout(OverrideSceneSelectLayout);
        OverrideSceneSelectLayout.setAlignment(FlowLayout.CENTER);
        overrideSceneSelectLabel = new JLabel("Select New Scene Type:");
        overrideSceneTypeBox = new JComboBox(SceneTypeStrings);
        overrideSceneTypeBox.setSelectedIndex(overrideSceneTypeIndex);
        overrideSceneTypeBox.setPreferredSize(new Dimension(150,20));
        overrideSceneTypeBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ame) {
                overrideSceneTypeIndex = overrideSceneTypeBox.getSelectedIndex();
            }
        });
        overrideSceneButtonPanel = new JPanel();
        FlowLayout OverrideSceneButtonLayout = new FlowLayout();
        overrideSceneButtonPanel.setLayout(OverrideSceneButtonLayout);
        OverrideSceneButtonLayout.setAlignment(FlowLayout.CENTER);
        overrideSceneAcceptButton = new JButton("Accept New Scene");
        overrideSceneAcceptButton.setPreferredSize(new Dimension(190,20));
        overrideSceneAcceptButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                // accept new scene selection
                overrideSceneFrame.dispose();
                /** GUIOverrideSceneTF=false; */
                GUIOverrideSceneType = overrideSceneTypeIndex;
                runADTmain();
            }
        });
        overrideSceneCancelButton = new JButton("Keep Current Scene");
        overrideSceneCancelButton.setPreferredSize(new Dimension(190,20));
        overrideSceneCancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                overrideSceneFrame.dispose();
                /** GUIOverrideSceneTF=false; */
                GUIOverrideSceneType = Env.OverrideSceneType;
                runADTmain();
            }
        });
        overrideSceneCurrentPanel.add(overrideSceneCurrentLabel);
        overrideSceneCurrentPanel.add(overrideSceneCurrentTextField);
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
        if (running) {
            return;
        }
        running = true;
        adtBtn.setEnabled(false);
        adtBtn.setText("Running");

        Misc.run(new Runnable() {
            public void run() {
                runADT();
                ExitADT();
            }
        });

    }
        
    private void runADT() {
        Main StormADT = new Main();
        String HistoryListOutput = null;
        String ADTRunOutput = null;
        String ErrorMessage = null;
        History CurrentHistory = new History();
        
        if(GUIFileOverrideTF) {
            String GUIOverrideFilePath = System.getenv("ODTHOME");
            if(GUIOverrideFilePath==null) {
                GUIOverrideFilePath = System.getenv("HOME");
            }
            String GUIOverrideFile = GUIOverrideFilePath + "/runadt.nogui.inputs.txt";
            /* GUIFileOverrideCheckBoxToggle();  change toggle back to OFF */
            int RetVal = ReadGUIOverrideInputFile(GUIOverrideFile);
            if(RetVal==-1) {
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
        if(ReturnVal<0) {
            ErrorMessage = String.format("Error obtaining initial position... exiting ADT\n");
            System.out.printf(ErrorMessage);
            userMessage(ErrorMessage);
            ExitADT();
            return;
        } else {
            if(RunAuto) {
                try {
                    float CenterLatitude = (float) Env.SelectedLatitude;
                    float CenterLongitude =  (float) Env.SelectedLongitude;
                    /** System.out.printf("pre-ARCHER latitude=%f longitude=%f\n",CenterLatitude,CenterLongitude); */
                    GetImageData(CenterLatitude, CenterLongitude);
                }
                catch(Exception exception)
                {
                    ErrorMessage = String.format("Error reading IR data pre-ARCHER\n");
                    System.out.printf(ErrorMessage);
                    userMessage(ErrorMessage);
                    ExitADT();
                    return;
                }
                StormADT.GetARCHERPosition();
            } else {
                if(probeLocation == null) {
                    ErrorMessage = String.format("Please select storm center location manually and try again");
                    System.out.printf(ErrorMessage);
                    userMessage(ErrorMessage);
                    ExitADT();
                    return;
                }
            }
        
            try {
                float CenterLatitude = (float) Env.SelectedLatitude;
                float CenterLongitude =  (float) Env.SelectedLongitude;
                int DomainID = Env.DomainID;
                /** System.out.printf("latitude=%f longitude=%f domain=%d\n",CenterLatitude,CenterLongitude,DomainID); */
                GetImageData(CenterLatitude, CenterLongitude);
            }
            catch(Exception e)
            {
                // e.printStackTrace();
                logger.error("Error reading IR data in getimagedata(): ", e);
                ErrorMessage = String.format("Error reading IR data in getimagedata()\n");
//                System.out.printf(ErrorMessage);
                userMessage(ErrorMessage);
                ExitADT();
                return;
            }
            try {
                System.out.printf("RUNNING ADT ANALYSIS\n");
                ADTRunOutput = StormADT.RunADTAnalysis(runFullADTAnalysis,GUIHistoryFileName);
                GUIOverrideSceneType = -99;  // is this needed here anymore?
            }
            catch(IOException exception)
            {
                ErrorMessage = String.format("Error with call to StormADT.RunADT()\n");
                System.out.printf(ErrorMessage);
                userMessage(ErrorMessage);
                ExitADT();
                return;
            }
            int InitialSceneTypeValue = Env.OverrideSceneType;
            if((GUIOverrideSceneTF)&&(InitialSceneTypeValue>=0)) {
                /** System.out.printf("Overriding scene type!!!  Scene value=%d\n",InitialSceneTypeValue); */
                overrideSceneCurrentTextField.setText(SceneTypeStrings[InitialSceneTypeValue]);
                overrideSceneFrame.pack();
                overrideSceneFrame.setVisible(true);
                ExitADT();
            } else {
                System.out.printf("done running ADT\n");
    
                resultArea.setText(ADTRunOutput);
                resultFrame.pack();
                resultFrame.setVisible(true);        
 
                if(GUIHistoryFileName!=null) {
                    try {
                        int[] InsertRecs = History.InsertHistoryRecord(runFullADTAnalysis,GUIHistoryFileName);
                        /** System.out.printf("*** Modified=%d InsertOverwriteFlag=%d***\n",InsertRecs[0],InsertRecs[1]); */
                        int NumRecs = History.WriteHistoryFile(GUIHistoryFileName);
                        ErrorMessage = String.format("Number of records writen to history file=%d\n",NumRecs);
                    }
                    catch (IOException exception) 
                    {
                        ErrorMessage = String.format("Error writing history file %s\n",GUIHistoryFileName);
                    }
                    System.out.printf(ErrorMessage);
                    userMessage(ErrorMessage);
                }
                if(GUIATCFRecordOutputTF) {
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
    
    private void listHistoryFile() {
        HistoryListOutput = null;
        
        History CurrentHistory = new History();
        
        try {
            System.out.printf("trying to read history file %s\n",GUIHistoryFileName);
            CurrentHistory.ReadHistoryFile(GUIHistoryFileName);
        }
        catch(IOException exception)
        {
            String ErrorMessage = String.format("History file %s is not found",GUIHistoryFileName);
            System.out.printf(ErrorMessage);
            userMessage(ErrorMessage);
            return;
        }
        
        int NumRecs = CurrentHistory.HistoryNumberOfRecords();
        /** System.out.printf("number of records=%d\n",NumRecs); */
        
        HistoryListOutput = CurrentHistory.ListHistory(0,-1,"CIMS","99X");
        historyLabel.setText(GUIHistoryFileName);
        historyArea.setText(HistoryListOutput);
        historyFrame.pack();
        historyFrame.setVisible(true);
        
    }
    
    private void modifyHistoryFile() {
        
        History CurrentHistory = new History();

        if(GUIDeleteTF) {
            // delete records
            int DeleteReturn[] = CurrentHistory.DeleteHistoryRecords(runFullADTAnalysis,GUIHistoryFileName);
            System.out.printf("deleted %d records... modified %d records\n", DeleteReturn[1],DeleteReturn[0]);
        } else if(GUICommentAddTF) {
            // 
            int CommentAddReturn = CurrentHistory.CommentHistoryRecords(GUICommentString);
            System.out.printf("added comment to %d records\n",CommentAddReturn);
        } else {
            // invalid selection
        }
        try {
            int HistoryFileRecords = CurrentHistory.WriteHistoryFile(GUIHistoryFileName);
        }
        catch(IOException exception) {
            String ErrorMessage = String.format("error updating history file %s",GUIHistoryFileName);
            System.out.printf(ErrorMessage);
            userMessage(ErrorMessage);
        }
    }
        
    private String selectHistoryFile() {
        
        String fileNameReturn = null;
        String HistoryPath = null;
        
        JFrame historyFileFrame = new JFrame();
        historyFileChooser = new JFileChooser();
        HistoryPath = System.getenv("ODTHISTORY");
        if(HistoryPath==null) {
            HistoryPath = System.getenv("HOME");
        }
        historyFileChooser.setCurrentDirectory(new File(HistoryPath));
        historyFileChooser.setDialogTitle("Select ADT History File");
        int returnVal = historyFileChooser.showOpenDialog(historyFileFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = historyFileChooser.getSelectedFile();
            fileNameReturn = String.format(file.getAbsolutePath());
        } else {
        }

        return fileNameReturn;
    }
    
    private String selectHistoryFileOutput() {
        
        String fileNameReturn = null;
        File saveFile = null;
        String HistoryPath = null;
        String ErrorMessage = null;
        
        JFrame historyFileSaveFrame = new JFrame();
        historyFileSaveChooser = new JFileChooser();
        HistoryPath = System.getenv("ODTHISTORY");
        if(HistoryPath==null) {
            HistoryPath = System.getenv("HOME");
        }
        historyFileSaveChooser.setCurrentDirectory(new File(HistoryPath));
        /** historyFileSaveChooser.setSelectedFile(new File(GUIHistoryFileName + ".txt")); */
        historyFileSaveChooser.setDialogTitle("Select ADT History Listing File");
        int returnVal = historyFileSaveChooser.showSaveDialog(historyFileSaveChooser);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            saveFile = historyFileSaveChooser.getSelectedFile();
            try(FileWriter outFile = new FileWriter(saveFile)) {
                outFile.write(HistoryListOutput);
                outFile.close();
                /** outFile.flush(); */
                ErrorMessage = String.format("success writing history file output file %s\n",saveFile.toString());
            } 
            catch(IOException ex) {
                ex.printStackTrace();
                ErrorMessage = String.format("error writing history file output file %s\n",saveFile.toString());
            }
            System.out.printf(ErrorMessage);
            userMessage(ErrorMessage);
        } else if (returnVal == JFileChooser.CANCEL_OPTION) {
            // User has pressed cancel button
        }
            
        return saveFile.toString();

    }
    
    private void ATCFFileOutput(int outputstyle) {
        File saveFile = null;
        String TempFileName = null;
        String ATCFOutputFileName = null;
        String ATCFOutputFilePath = null;
        String ATCFFileOutput = null;
        String ATCFMessage = null;
        String HistoryPath = null;
        boolean writefileTF = false;
        
        if(outputstyle==0) {
        	// output entire history file in ATCF 
            JFrame historyFileSaveFrame = new JFrame();
            historyFileSaveChooser = new JFileChooser();
            HistoryPath = System.getenv("ODTHISTORY");
            if(HistoryPath==null) {
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
            System.out.printf("saving ATCF history listing file name=%s writeTF=%b\n",saveFile.toString(),writefileTF);
        } else {
            // call routine to generate ATCF file name for single analysis record
            System.out.printf("stormID=%s siteID=%s\n",GUIATCFStormID,GUIATCFSiteID);
            ATCFOutputFileName = Functions.adt_atcffilename(GUIATCFStormID,GUIATCFSiteID);
            System.out.printf("atcf output name=%s*\n", ATCFOutputFileName);
            ATCFOutputFilePath = System.getenv("ODTOUTPUT");
            if(ATCFOutputFilePath==null) {
                ATCFOutputFilePath = System.getenv("HOME");
            }
            System.out.printf("atcf output path=%s*\n", ATCFOutputFilePath);
            saveFile = new File(ATCFOutputFilePath + "/" + ATCFOutputFileName);
            System.out.printf("atcf output name=%s*\n", saveFile.toString());
        	writefileTF = true;
        }
        // call routine to output file
        ATCFFileOutput = History.ListHistory(outputstyle,GUIHistoryListFormat,GUIATCFSiteID,GUIATCFStormID);
        if(writefileTF) {
            try(FileWriter outFile = new FileWriter(saveFile)) {
                outFile.write(ATCFFileOutput);
                outFile.close();
                /* outFile.flush(); */
                ATCFMessage = String.format("Success writing ATCF file %s",saveFile);
            } 
            catch(IOException ex) {
                ex.printStackTrace();
                ATCFMessage = String.format("Error writing ATCF file %s",saveFile);
            }
            System.out.printf(ATCFMessage);
            userMessage(ATCFMessage);
        }
    }
    
    private void updateHistoryTextField() {
        
        historyTextField.setText(GUIHistoryFileName);
        
    }

    private void editHistoryFileWindow() {
        historyDateFrame.pack();
        historyDateFrame.setVisible(true);
    }
    
    private String selectForecastFile() {
        
        String fileNameReturn = null;
        String ForecastPath = null;
        
        System.out.printf("in selectforecastfile\n");
        JFrame forecastFileFrame = new JFrame();
        forecastFileChooser = new JFileChooser();
        ForecastPath = System.getenv("ODTAUTO");
        if(ForecastPath==null) {
            ForecastPath = System.getenv("HOME");
        }
        System.out.printf("forecast path=%s\n",ForecastPath);
        forecastFileChooser.setCurrentDirectory(new File(ForecastPath));
        forecastFileChooser.setDialogTitle("Select ADT History File");
        int returnVal = forecastFileChooser.showOpenDialog(forecastFileFrame);
        System.out.printf("returnVal=%d\n",returnVal);
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = forecastFileChooser.getSelectedFile();
            fileNameReturn = String.format(file.getAbsolutePath());
        } else {
            System.out.printf("error with file chooser\n");
        }

        return fileNameReturn;
    }
    
    private void updateForecastTextField() {
        
        forecastTextField.setText(GUIForecastFileName);
        
    }
    
    private void updateAutoManText(int intval) {
        if(intval==1) {
            automanTextField.setText("Select Storm Center In Image");
            autoStormSelectLabel.setEnabled(false);
            forecastSelectLabel.setEnabled(false);
            forecastTextField.setEnabled(false);
            forecastBtn.setEnabled(false);
            forecastTypeBox.setEnabled(false);
        } else {
            automanTextField.setText("Select Forecast File For First Guess Below");
            autoStormSelectLabel.setEnabled(true);
            forecastSelectLabel.setEnabled(true);
            forecastTextField.setEnabled(true);
            forecastBtn.setEnabled(true);
            forecastTypeBox.setEnabled(true);
        }
    }
    
    private void updateMSLPFields(int intval) {
        if(intval==1) {
            // Dvorak
            ckzPenvTextField.setEnabled(false);
            ckz34radiusTextField.setEnabled(false);
            ckzPenvLabel.setEnabled(false);
            ckz34radiusLabel.setEnabled(false);
            mslpDvorakButton.setSelected(true);
            mslpCKZButton.setSelected(false);
        } else {
            // CKZ
            ckzPenvTextField.setEnabled(true);
            ckz34radiusTextField.setEnabled(true);
            ckzPenvLabel.setEnabled(true);
            ckz34radiusLabel.setEnabled(true);
            mslpDvorakButton.setSelected(false);
            mslpCKZButton.setSelected(true);
        }
        
    }
    
    private int readCKZTextFields(int intval) {
        int ReturnValue;
        
        if(intval==1) {
            // Penv
            ReturnValue = Integer.valueOf(ckzPenvTextField.getText()).intValue();
        } else {
            // R34
            ReturnValue = Integer.valueOf(ckz34radiusTextField.getText()).intValue();
        }
        
        return ReturnValue;
        
    }
    
    private void updateLandButton(int intval) {
        if(intval==1) {
            // LAND=YES
            LandONButton.setSelected(true);
            LandOFFButton.setSelected(false);
        } else {
            // LAND=NO
            LandONButton.setSelected(false);
            LandOFFButton.setSelected(true);
        }
        
    }
    
    private void updateVOutButton(int intval) {
        if(intval==1) {
            // 1-minute winds
            V1MinButton.setSelected(true);
            V10MinButton.setSelected(false);
        } else {
            // 10-minute winds
            V1MinButton.setSelected(false);
            V10MinButton.setSelected(true);
        }
        
    }
    
    private double readRawTTextFields() {
        double ReturnValue;
        
        ReturnValue = Double.valueOf(RawTTextField.getText()).doubleValue();
        
        return ReturnValue;
    }
    
    private double readRMWTextFields() {
        double ReturnValue;
        
        ReturnValue = Double.valueOf(RMWTextField.getText()).doubleValue();
        
        return ReturnValue;
    }
    
    private void updatePMWFields(int intval) {
        if(intval==1) {
            // Automated - file entry
            PMWFileBtn.setEnabled(true);
            pmwManDateTextField.setEnabled(false);
            pmwManTimeTextField.setEnabled(false);
            pmwManScoreTextField.setEnabled(false);
            pmwManDateLabel.setEnabled(false);
            pmwManTimeLabel.setEnabled(false);
            pmwManScoreLabel.setEnabled(false);
        } else {
            // Maunal entry
            PMWFileBtn.setEnabled(false);
            pmwManDateTextField.setEnabled(true);
            pmwManTimeTextField.setEnabled(true);
            pmwManScoreTextField.setEnabled(true);
            pmwManDateLabel.setEnabled(true);
            pmwManTimeLabel.setEnabled(true);
            pmwManScoreLabel.setEnabled(true);
        }
    }
    
    private int readPMWManualDateField() {
        String returnString = null;
        int returnValue;
        
        returnString = pmwManDateTextField.getText();
        returnValue = Functions.cmonth2julian(returnString);
        
        return returnValue;
    }
    
    private int readPMWManualTimeField() {
        int returnValue;
        
        returnValue = Integer.valueOf(pmwManTimeTextField.getText()).intValue();

        return returnValue;
    }
    
    private double readPMWManualScoreField() {
        double returnValue;
        
        returnValue = Double.valueOf(pmwManScoreTextField.getText()).doubleValue();
        
        return returnValue;
    }
    
    private String selectPMWFile() {
        
        String fileNameReturn = null;
        String DataPath = null;
        
        JFrame PMWFileFrame = new JFrame();
        PMWFileChooser = new JFileChooser();
        // PMWFileChooser.setCurrentDirectory(new File("/home/tlo/Development/mcidasv/edu/wisc/ssec/mcidasv/adt/main"));
        DataPath = System.getenv("ODTPMW");
        if(DataPath==null) {
            DataPath = System.getenv("HOME");
        }
        PMWFileChooser.setCurrentDirectory(new File(DataPath));
        PMWFileChooser.setDialogTitle("Select PMW History File");
        int returnVal = PMWFileChooser.showOpenDialog(PMWFileFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = PMWFileChooser.getSelectedFile();
            fileNameReturn = String.format(file.getAbsolutePath());
        } else {
        }

        return fileNameReturn;
    }
    
    private int readDateTextStringField(int intval) {
        int returnValue;
        String returnString = null;
        
        if(intval==1) {
            returnString = historyDateStartDateTextField.getText();
            returnValue = Functions.cmonth2julian(returnString);
        } else if (intval==2) {
            returnValue = Integer.valueOf(historyDateStartTimeTextField.getText()).intValue();
        } else if (intval==3) {
            returnString = historyDateEndDateTextField.getText();
            returnValue = Functions.cmonth2julian(returnString);
        } else {
            returnValue = Integer.valueOf(historyDateEndTimeTextField.getText()).intValue();
        }
        return returnValue;
    }
    
    private void updateHistoryEditButton(int intval) {
        if(intval==1) {
            // history Edit - Delete
            historyEditDeleteButton.setSelected(true);
            historyEditAddCommentButton.setSelected(false);
            historyEditAddCommentLabel.setEnabled(false);
            historyEditAddCommentTextField.setEnabled(false);
            GUICommentAddTF = false;
            GUIDeleteTF = true;
        } else {
            // history Edit - Add Comment
            historyEditDeleteButton.setSelected(false);
            historyEditAddCommentButton.setSelected(true);
            historyEditAddCommentLabel.setEnabled(true);
            historyEditAddCommentTextField.setEnabled(true);
            GUICommentAddTF = true;
            GUIDeleteTF = false;
        }
    }
    
    private String readCommentTextField() {
        String stringReturn = null;
        
        stringReturn = historyDateStartDateTextField.getText();
        
        return stringReturn;
    }
    
    private String readATCFStringField(int intval) {
        String stringReturn = null;
        
        if(intval==1) {
            stringReturn = ATCFEntryStormTextField.getText();
        } else {
            stringReturn = ATCFEntrySiteTextField.getText();
        }
        
        return stringReturn;
        
    }
    
    private void GUIFileOverrideCheckBoxToggle() {
        
        if(GUIFileOverrideTF) {
            GUIFileOverrideTF = false;
            getADTenvParameters();
        } else {
            GUIFileOverrideTF = true;
        }
        GUIFileOverrideButton.setSelected(GUIFileOverrideTF);

    }
    
    private void PMWActivateCheckBoxToggle() {
        if(GUIPMWActivateTF) {
            // if on, turn off
            GUIPMWActivateTF = false;
            PMWManButton.setEnabled(false);
            PMWManButton.setSelected(false);
            pmwManDateTextField.setEnabled(false);
            pmwManTimeTextField.setEnabled(false);
            pmwManScoreTextField.setEnabled(false);
            pmwManDateLabel.setEnabled(false);
            pmwManTimeLabel.setEnabled(false);
            pmwManScoreLabel.setEnabled(false);
        } else {
            // if off, turn on
            GUIPMWActivateTF = true;
            PMWManButton.setEnabled(true);
            PMWManButton.setSelected(true);
            pmwManDateTextField.setEnabled(true);
            pmwManTimeTextField.setEnabled(true);
            pmwManScoreTextField.setEnabled(true);
            pmwManDateLabel.setEnabled(true);
            pmwManTimeLabel.setEnabled(true);
            pmwManScoreLabel.setEnabled(true);
        }
        PMWActivateButton.setSelected(GUIPMWActivateTF);

    }
    
    private void ATCFRecordOutputCheckBoxToggle() {
        if(GUIATCFRecordOutputTF) {
            // if on, turn off
            GUIATCFRecordOutputTF = false;
            ATCFEntryStormLabel.setEnabled(false);
            ATCFEntryStormTextField.setEnabled(false);
            ATCFEntrySiteLabel.setEnabled(false);
            ATCFEntrySiteTextField.setEnabled(false);
        } else {
            // if off, turn on
            GUIATCFRecordOutputTF = true;
            ATCFEntryStormLabel.setEnabled(true);
            ATCFEntryStormTextField.setEnabled(true);
            ATCFEntrySiteLabel.setEnabled(true);
            ATCFEntrySiteTextField.setEnabled(true);
        }
        ATCFOutputButton.setSelected(GUIATCFRecordOutputTF);
    }
    
    private void getADTenvParameters() {
        Env GlobalVariables = new Env();
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
        GUIOverrideSceneType = Env.OverrideSceneType;
    }

    private void loadADTenvParameters() {
        /* Env GlobalVariables = new Env(); */

        System.out.printf("setting env parameters\n");
        
        /* send ADT Environmental parameters to Env prior to running ADT */
        /* boolean values */
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

        /* integer values */
        Env.DomainID = GUIDomainID;
        Env.ForecastFileType = GUIForecastType;
        Env.MWJulianDate = GUIMWJulianDate;
        Env.MWHHMMSSTime = GUIMWHHMMSSTime;
        Env.StartJulianDate = GUIStartDate;
        Env.StartHHMMSSTime = GUIStartTime;
        Env.EndJulianDate = GUIEndDate;
        Env.EndHHMMSSTime = GUIEndTime;
        Env.HistoryListFormat = GUIHistoryListFormat;
        /* double values */
        Env.InitRawTValue = GUIRawTValue;
        Env.MWScore = GUIMWScore;
        Env.CKZGaleRadius = GUICKZGaleRadius;
        Env.CKZPenv = GUICKZPenv;
        Env.RMWSize = GUIRMWSize;
        Env.SelectedLatitude = GUIUserLatitude;
        Env.SelectedLongitude = GUIUserLongitude;
        
        System.out.printf("load forecast file name=%s\n",GUIForecastFileName);
        Env.ForecastFileName = GUIForecastFileName;   // needed?
        Env.ASCIIOutputFileName = GUIHistoryFileListingName;   // needed?
        Env.StormIDString = GUIATCFStormID;
        Env.ATCFSourceAgcyIDString = GUIATCFSiteID;
        Env.OverrideSceneType = GUIOverrideSceneType;
        
    }
    
    public static int ReadGUIOverrideInputFile(String GUIOverrideFile) {
        
        System.out.printf("opening file %s\n",GUIOverrideFile);
        
        File GUIDataFile = new File(GUIOverrideFile);
        String delims = "[ ]+";
        String line = null;
        int retval = 1;
        
        GUIOverrideTF = false;
        GUIOverrideSceneTF = false;
        GUIOverrideSceneType = -99;
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
                if((line = GUIFile.nextLine()).isEmpty()){
                } else {
                    String[] tokens = line.split(delims);
                    String IDstring = tokens[0];
                    String RecValue = tokens[1];
                    /** System.out.printf("scanning IDstring=%s\n",IDstring); */
                    switch(IDstring) {
                        case "ATCFOutputTF" : GUIATCFOutputTF = Boolean.valueOf(RecValue); break;
                        case "ATCFRecordOutputTF" : GUIATCFRecordOutputTF = Boolean.valueOf(RecValue); break;
                        case "InitStrengthTF" : GUIInitStrengthTF = Boolean.valueOf(RecValue); break;
                        case "LandFlagTF" : GUILandFlagTF = Boolean.valueOf(RecValue); break;
                        case "UseCKZTF" : GUIUseCKZTF = Boolean.valueOf(RecValue); break;
                        case "Vmax1or10TF" : GUIVmax1or10TF = Boolean.valueOf(RecValue); break;
                        case "UsePMWTF" : GUIPMWActivateTF = Boolean.valueOf(RecValue); break;
                        case "ForecastType" : GUIForecastType = Integer.valueOf(RecValue).intValue(); break;
                        case "MWJulianDate" : GUIMWJulianDate = Integer.valueOf(RecValue).intValue(); break;
                        case "MWHHMMSSTime" : GUIMWHHMMSSTime = Integer.valueOf(RecValue).intValue(); break;
                        case "HistoryListFormat" : GUIHistoryListFormat = Integer.valueOf(RecValue).intValue(); break;
                        case "RawTValue" : GUIRawTValue = Double.valueOf(RecValue).doubleValue(); break;
                        case "MWScore" : GUIMWScore = Double.valueOf(RecValue).doubleValue(); break;
                        case "CKZGaleRadius" : GUICKZGaleRadius = Double.valueOf(RecValue).doubleValue(); break;
                        case "CKZPenv" : GUICKZPenv = Double.valueOf(RecValue).doubleValue(); break;
                        case "RMWSize" : GUIRMWSize = Double.valueOf(RecValue).doubleValue(); break;
                        case "HistoryFileName" : GUIHistoryFileName = RecValue; break;
                        case "ForecastFileName" : GUIForecastFileName = RecValue; break;
                        case "HistoryFileListingName" : GUIHistoryFileListingName = RecValue; break;
                        case "ATCFStormID" : GUIATCFStormID = RecValue; break;
                        case "ATCFSiteID" : GUIATCFSiteID = RecValue; break;
                        default : break;
                    }
                }
            }
            GUIFile.close();
        } catch (IOException ex) {
            retval = -1;;
        }
        return retval;
    }
        public void latLonWidgetChanged() {
            System.out.printf("latlonwidgetchanged called\n");
                try {
                        System.err.println("latlon widget changed");
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
                if (!canHandleEvents()) {
                        return;
                }
                int id = event.getId();
                String idstring = event.toString();
                InputEvent inputEvent = event.getInputEvent();

                /* System.out.printf("event ID=%d %s\n",id,idstring); */
                try {
                        if (id == DisplayEvent.MOUSE_PRESSED_LEFT) {
                            System.out.printf("Manual Position Selection\n");
                            probeLocation = toEarth(event).getLatLonPoint();
                            updateProbeLocation();
                        } else {
                            return;
                        }
                } catch (Exception e) {
                        logException("Error selecting position with mouse", e);
                }

        }

        private void updateProbeLocation() {
                try {
                        if (probeLocation == null) {
                                return;
                        }
                        double lon = probeLocation.getLongitude().getValue(
                                        CommonUnit.degree);
                        double lat = probeLocation.getLatitude().getValue(CommonUnit.degree);
                        probe.setPosition(new RealTuple(RealTupleType.SpatialEarth3DTuple,
                                        new double[] { lon, lat, 0 }));

                        probe.setVisible(true);

                        GUIUserLatitude = lat;    // added TLO
                        GUIUserLongitude = lon;    // added TLO
                        System.out.printf("set lat/lon from probe at lat=%f lon=%f\n",GUIUserLatitude,GUIUserLongitude);
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
         * @param value
         *            The new value for ProbeLocation
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
        
        protected FlatField getFlatField(FieldImpl data) throws VisADException,
        RemoteException {
                FlatField ff = null;
                if (GridUtil.isSequence(data)) {
                    ff = (FlatField) data.getSample(0);
                } else {
                    ff = (FlatField) data;
                }
                return ff;
        }

        public EarthLocation toEarth(DisplayEvent event) throws VisADException,
        RemoteException {
            NavigatedDisplay d = getNavigatedDisplay();
            return (d == null) ? null : d.getEarthLocation(toBox(event));
        }

        public void GetImageDateTime() {
            
            RealTuple timeTuple = null;
            Real tt = null;
            DateTime dat = null;
            
            List infos = getDisplayInfos();
            DataInstance de = getDataInstance();
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
                
            }
            
            double curdate = dat.getValue();
            System.out.printf("curdate=%f\n",curdate);

            Date datevalue = new Date((long)curdate*1000);
            
            SimpleDateFormat dateformat = new SimpleDateFormat("yyyyDDD");
            SimpleDateFormat timeformat = new SimpleDateFormat("HHmmss");
            dateformat.setTimeZone(TimeZone.getTimeZone("GMT"));
            timeformat.setTimeZone(TimeZone.getTimeZone("GMT"));

            String JulianDate = dateformat.format(datevalue);
            String HHMMSSTime = timeformat.format(datevalue);
            int ImageDateInt = Integer.valueOf(JulianDate.toString()).intValue();
            int ImageTimeInt = Integer.valueOf(HHMMSSTime.toString()).intValue();
            /** System.out.printf("image date = %d  image time=%d\n",ImageDateInt,ImageTimeInt); */
            
            Data.IRData_JulianDate = ImageDateInt;
            Data.IRData_HHMMSSTime = ImageTimeInt;
            
            System.out.printf("IMAGE DATE=%d  TIME=%d\n", Data.IRData_JulianDate, Data.IRData_HHMMSSTime);
        }
        
        public void GetImageData(float CenterLatitude, float CenterLongitude) {
            
            System.err.println("creating ReadIRImage()...");
            
            ReadIRImage IRImage = new ReadIRImage();

            FlatField ffield = null;
            int SatelliteID = 0;
            int channel = 0;
            
            List sources = new ArrayList();

            System.err.printf("into getimagedata\n");
            boolean isTemp = false;
            choice.getDataSources(sources);
            try {
                    List infos = getDisplayInfos();
                    DataInstance de = getDataInstance();
                    DisplayInfo displayInfo = (DisplayInfo) infos.get(0);

                    Animation anime = displayInfo.getViewManager().getAnimation();
                    Set timeSet = anime.getSet();
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
                            String name = ffield.getSample(0).getType().prettyString();
                    }

            } catch (VisADException e) {
                    logException("Handling data", e);
                    return;
            } catch (RemoteException f) {
            }
        
            String shortName = choice.getName();

            Env.UserDefineDomain = 0; // automated
            String sidName = Functions.adt_sattypes(SatelliteID);
           
            System.err.printf("SatelliteID=%d\n", SatelliteID);
            
            try
            {
                ReadIRImage.ReadIRDataFile(ffield, CenterLatitude, CenterLongitude,
                            SatelliteID, channel, isTemp);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                System.err.printf("error in ReadIRImage");
            }
            
        }
        
}
