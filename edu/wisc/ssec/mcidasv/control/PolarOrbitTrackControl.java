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

package edu.wisc.ssec.mcidasv.control;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import visad.Data;
import visad.DateTime;
import visad.FieldImpl;
import visad.FlatField;
import visad.MathType;
import visad.Tuple;
import visad.ScalarMap;
import visad.ScalarType;
import visad.VisADException;
import visad.georef.LatLonTuple;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.imagery.BandInfo;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.control.ColorTableWidget;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.ui.XmlTree;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Range;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import edu.wisc.ssec.mcidasv.PersistenceManager;
import edu.wisc.ssec.mcidasv.data.ComboDataChoice;
import edu.wisc.ssec.mcidasv.data.GroundStations;

/**
 * {@link ucar.unidata.idv.control.PolarOrbitTrackControl} with some McIDAS-V
 * specific extensions. Namely parameter sets and support for inverted 
 * parameter defaults.
 */
public class PolarOrbitTrackControl extends ucar.unidata.idv.control.DisplayControlImpl {
//public class PolarOrbitTrackControl extends ucar.unidata.idv.control.TrackControl {

    private static final Logger logger = LoggerFactory.getLogger(PolarOrbitTrackControl.class);

    /** The spacing used in the grid layout */
    protected static final int GRID_SPACING = 3;

    /** Used by derived classes when they do a GuiUtils.doLayout */
    protected static final Insets GRID_INSETS = new Insets(GRID_SPACING,
                                                    GRID_SPACING,
                                                    GRID_SPACING,
                                                    GRID_SPACING);
    private double latitude;
    private double longitude;
    private double altitude;
    private JPanel locationPanel;
    private JPanel latLonAltPanel;

    private List stations;
    private List lats;
    private List lons;
    private List alts;

    private JComboBox locationComboBox;

    /** Input for lat/lon center point */
    protected LatLonWidget latLonWidget = new LatLonWidget();

    private JTextField latFld;
    private JTextField lonFld;
    private JTextField altitudeFld = new JTextField(" ", 5);

    public PolarOrbitTrackControl() {
        super();
        logger.trace("created new tlecontrol={}", Integer.toHexString(hashCode()));
        System.out.println("PolarOrbitTrackControl:");
    }

    @Override public boolean init(DataChoice dataChoice) 
        throws VisADException, RemoteException 
    {
        //System.out.println("init: dataChoice=" + dataChoice);
        boolean result = super.init((DataChoice)this.getDataChoices().get(0));
        return result;
    }

    @Override public Data getData(DataInstance dataInstance) {
        Data data = null;
        try {
            data = super.getData(dataInstance);
            List<DateTime> dts = new ArrayList();
            List lats = new ArrayList();
            List lons = new ArrayList();
            if (data instanceof Tuple) {
                Data[] dataArr = ((Tuple)data).getComponents();
                System.out.println("dataArr[0]=" + dataArr[0]);
                for (int i=0; i<dataArr.length; i++) {
                    Tuple t = (Tuple)dataArr[i];
                    Data[] tupleComps = t.getComponents();
                    System.out.println("tupleComps[0]=" + tupleComps[0]);
                    dts.add((DateTime)tupleComps[0]);
                    LatLonTuple llt = (LatLonTuple)tupleComps[1];
                    lats.add(llt.getLatitude().getValue());
                    lons.add(llt.getLongitude().getValue());
                    System.out.println("    DateTime=" + dts.get(i) + " Lat=" + lats.get(i) + " Lon=" + lons.get(i));
                }
            }
        } catch (Exception e) {
            System.out.println("getData e=" + e);
        }
        return data;
    }

    /**
     * Called by doMakeWindow in DisplayControlImpl, which then calls its
     * doMakeMainButtonPanel(), which makes more buttons.
     *
     * @return container of contents
     */
    public Container doMakeContents() {
        GroundStations gs = new GroundStations();
        int gsCount = gs.getGroundStationCount();
        String[] stats = new String[gsCount];
        stations = gs.getStations();
        for (int i=0; i<gsCount; i++) {
            stats[i] = (String)stations.get(i);
        }
        lats = gs.getLatitudes();
        lons = gs.getLongitudes();
        alts = gs.getAltitudes();

        locationComboBox = new JComboBox(stats);
        locationComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int indx = locationComboBox.getSelectedIndex();
                String str = (String)(lats.get(indx));
                Double d = new Double(str);
                double dVal = d.doubleValue();
                latLonWidget.setLat(dVal);
                str = (String)(lons.get(indx));
                d = new Double(str);
                dVal = d.doubleValue() * -1;
                latLonWidget.setLon(dVal);
                str = (String)(alts.get(indx));
                altitudeFld.setText(str);
            }
        });

        String str = (String)(lats.get(0));
        Double d = new Double(str);
        double dVal = d.doubleValue();
        latLonWidget.setLat(dVal);
        str = (String)(lons.get(0));
        d = new Double(str);
        dVal = d.doubleValue() * -1;
        latLonWidget.setLon(dVal);
        str = (String)(alts.get(0));
        altitudeFld = new JTextField(str, 5);
        latFld = latLonWidget.getLatField();
        lonFld = latLonWidget.getLonField();
        FocusListener latLonFocusChange = new FocusListener() {
            public void focusGained(FocusEvent fe) {
                latFld.setCaretPosition(latFld.getText().length());
                lonFld.setCaretPosition(lonFld.getText().length());
            }
            public void focusLost(FocusEvent fe) {
                setLatitude();
                setLongitude();
                setAltitude();
            }
        };
        locationPanel = GuiUtils.doLayout(new Component[] {
                           new JLabel("Ground Station: "),
                           locationComboBox }, 2,
                           GuiUtils.WT_N, GuiUtils.WT_N);
        latLonAltPanel = GuiUtils.doLayout(new Component[] {
                           latLonWidget,
                           new JLabel(" Altitude: "),
                           altitudeFld }, 3,
                           GuiUtils.WT_N, GuiUtils.WT_N);
        Insets  dfltGridSpacing = new Insets(4, 0, 4, 0);
        String  dfltLblSpacing  = " ";
        List allComps = new ArrayList();
        allComps.add(new JLabel(" "));
        allComps.add(locationPanel);
        allComps.add(latLonAltPanel);
        GuiUtils.tmpInsets = GRID_INSETS;
        JPanel dateTimePanel = GuiUtils.doLayout(allComps, 1, GuiUtils.WT_NY,
                               GuiUtils.WT_N);
        Data data = getData(getDataInstance());
        return GuiUtils.top(dateTimePanel);
    }

    private void setLatitude() {
        System.out.println("\nPolarOrbitTrackControl setLatitude:");
        this.latitude = latLonWidget.getLat();
    }

    private void setLongitude() {
        System.out.println("\nPolarOrbitTrackControl setLongitude:");
        this.longitude = latLonWidget.getLon();
    }

    private void setAltitude() {
        System.out.println("\nPolarOrbitTrackControl setAltitude:");
        String str = altitudeFld.getText();
        Double d = new Double(str);
        this.altitude = d.doubleValue();
    }
}
