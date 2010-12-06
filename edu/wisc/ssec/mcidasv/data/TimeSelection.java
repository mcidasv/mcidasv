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

package edu.wisc.ssec.mcidasv.data;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.data.adde.sgp4.Time;
import edu.wisc.ssec.mcidasv.data.dateChooser.*;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;

import visad.VisADException;

public class TimeSelection extends DataSelectionComponent implements Constants {

      private static final Logger logger = LoggerFactory.getLogger(TimeSelection.class);

      private DataSourceImpl dataSource;

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
      private JPanel beginTime;
      private JPanel beginDate;
      private JPanel endTime;
      private JPanel endDate;
      private JTextField beginTimeFld;
      private JTextField endTimeFld;
      private String defaultBegTime = "00:00:00";
      private String defaultEndTime = "23:59:59";
      private Date defaultDay = null;

      private JDateChooser begDay = null;
      private JDateChooser endDay = null;

      private List stations;
      private List lats;
      private List lons;
      private List alts;

      /** earth coordinates */
      protected static final String PROP_LOC = "Location";
      protected static final String PROP_LAT = "Latitude";
      protected static final String PROP_LON = "Longitude";
      protected static final String PROP_ALT = "Altitude";
      protected static final String PROP_BEGTIME = "BeginTime";
      protected static final String PROP_ENDTIME = "EndTime";
      protected static final String PROP_BTIME = "BTime";
      protected static final String PROP_ETIME = "ETime";

      private JComboBox locationComboBox;
/*
      private static final String[] stations = {"Madison", "Gilmore Creek", "Wallops Island"};
      private static final double[] lats = {43.13, 64.97, 37.50};
      private static final double[] lons = {89.35, 147.40, 75.40};
*/

      /** Input for lat/lon center point */
      protected LatLonWidget latLonWidget = new LatLonWidget();

      private JTextField latFld;
      private JTextField lonFld;
      private JTextField altitudeFld = new JTextField(" ", 5);

      public TimeSelection(DataSourceImpl dataSource) 
              throws VisADException, RemoteException {
          super("TLE");
          this.dataSource = dataSource;
      }

    protected JComponent doMakeContents() {
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
         Calendar cal = Calendar.getInstance();
         SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy");
         defaultDay = cal.getTime();
         List allComps = new ArrayList();
         allComps.add(new JLabel(" "));
         allComps.add(locationPanel);
         allComps.add(latLonAltPanel);
         allComps.add(new JLabel(" "));
         allComps.add(GuiUtils.lLabel("Begin:"));
         beginTimeFld = new JTextField(defaultBegTime, 10);
         beginTime = GuiUtils.doLayout(new Component[] {
                            new JLabel("            Time: "),
                            beginTimeFld }, 2,
                            GuiUtils.WT_N, GuiUtils.WT_N);
         allComps.add(beginTime);
         begDay = new JDateChooser(defaultDay);
         beginDate = GuiUtils.doLayout(new Component[] {
                            new JLabel("            Date: "),
                            begDay }, 2,
                            GuiUtils.WT_N, GuiUtils.WT_N);
         allComps.add(beginDate);
         allComps.add(GuiUtils.lLabel("End:"));
         endTimeFld = new JTextField(defaultEndTime, 10);
         endTime = GuiUtils.doLayout(new Component[] {
                            new JLabel("     "),
                            new JLabel("          Time: "),
                            endTimeFld }, 3,
                            GuiUtils.WT_N, GuiUtils.WT_N);
         allComps.add(endTime);
         endDay = new JDateChooser(defaultDay);
         endDate = GuiUtils.doLayout(new Component[] {
                            new JLabel("     "),
                            new JLabel("          Date: "),
                            endDay }, 3,
                            GuiUtils.WT_N, GuiUtils.WT_N);
         allComps.add(endDate);

         GuiUtils.tmpInsets = GRID_INSETS;
         JPanel dateTimePanel = GuiUtils.doLayout(allComps, 1, GuiUtils.WT_NY,
                                GuiUtils.WT_N);
         return GuiUtils.top(dateTimePanel);
    }

      @Override public void applyToDataSelection(DataSelection dataSelection) {
         if (dataSelection == null) {
             dataSelection = new DataSelection(true);
         }

         String loc = (String)(locationComboBox.getSelectedItem());
         dataSelection.putProperty(PROP_LOC, loc);

         String lat = latFld.getText();
         dataSelection.putProperty(PROP_LAT, lat);

         String lon = lonFld.getText();
         dataSelection.putProperty(PROP_LON, lon);

         String alt = altitudeFld.getText();
         dataSelection.putProperty(PROP_ALT, alt);

         Date beg = begDay.getDate();
         JCalendar cal = begDay.getJCalendar();
         JDayChooser dayChooser = cal.getDayChooser();
         int day = dayChooser.getDay();
         JMonthChooser monthChooser = cal.getMonthChooser();
         int month = monthChooser.getMonth() + 1;
         JYearChooser yearChooser = cal.getYearChooser();
         int year = yearChooser.getYear();
         //System.out.println("    beg=" + beg);
         //System.out.println("    beg: month=" + month + " day=" + day + " year=" + year);

         String begTime = beginTimeFld.getText();
         String[] timeStrings = begTime.split(":");
         int hours = (new Integer(timeStrings[0])).intValue();
         int mins = (new Integer(timeStrings[1])).intValue();
         double secs = (new Double(timeStrings[2] + ".0")).doubleValue();
         //System.out.println("begTime=" + begTime);
         //System.out.println("    hours=" + hours + " minutes=" + mins + " seconds=" + secs);

         Time bTime = new Time(year, month, day, hours, mins, secs);
         double dVal = bTime.getJulianDate();
         Double bigD = new Double(dVal);
         String begTimeStr = bigD.toString();
         //System.out.println("bTime=" + bTime.getDateTimeStr());
         dataSelection.putProperty(PROP_BEGTIME, bTime.getDateTimeStr());

         Date end = endDay.getDate();
         //System.out.println("    end=" + end);
         cal = endDay.getJCalendar();
         dayChooser = cal.getDayChooser();
         day = dayChooser.getDay();
         monthChooser = cal.getMonthChooser();
         month = monthChooser.getMonth() + 1;
         yearChooser = cal.getYearChooser();
         year = yearChooser.getYear();
         //System.out.println("    end: month=" + month + " day=" + day + " year=" + year);

         String endTime = endTimeFld.getText();
         timeStrings = endTime.split(":");
         hours = (new Integer(timeStrings[0])).intValue();
         mins = (new Integer(timeStrings[1])).intValue();
         secs = (new Double(timeStrings[2] + ".0")).doubleValue();
         //System.out.println("endTime=" + endTime);
         //System.out.println("    year=" + year + " month=" + month + " day=" + day);
         //System.out.println("    hours=" + hours + " minutes=" + mins + " seconds=" + secs);

         Time eTime = new Time(year, month, day, hours, mins, secs);
         dVal = eTime.getJulianDate();
         bigD = new Double(dVal);
         String endTimeStr = bigD.toString();
         //System.out.println("begTimeStr=" + begTimeStr + " endTimeStr=" + endTimeStr);
         //System.out.println("eTime=" + eTime.getDateTimeStr());

         dataSelection.putProperty(PROP_ENDTIME, eTime.getDateTimeStr());
         dataSelection.putProperty(PROP_BTIME, begTimeStr);
         dataSelection.putProperty(PROP_ETIME, endTimeStr);
      }

    private void setLatitude() {
        this.latitude = latLonWidget.getLat();
    }

    public void setLatitude(double val) {
        latLonWidget.setLat(val);
        this.latitude = val;
    }

    private void setLongitude() {
        this.longitude = latLonWidget.getLon();
    }

    private void setAltitude() {
        String str = altitudeFld.getText();
        Double d = new Double(str);
        this.altitude = d.doubleValue();
    }

    public void setLongitude(double val) {
        latLonWidget.setLon(val);
        this.longitude = val;
    }
}
