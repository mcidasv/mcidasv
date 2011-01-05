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
import ucar.unidata.util.GuiUtils;

import visad.VisADException;

public class TimeRangeSelection extends DataSelectionComponent implements Constants {

      private static final Logger logger = LoggerFactory.getLogger(TimeRangeSelection.class);

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

      protected static final String PROP_BEGTIME = "BeginTime";
      protected static final String PROP_ENDTIME = "EndTime";
      protected static final String PROP_BTIME = "BTime";
      protected static final String PROP_ETIME = "ETime";
      protected static final String PROP_YEAR = "Year";
      protected static final String PROP_MONTH = "Month";
      protected static final String PROP_DAY = "Day";
      protected static final String PROP_HOURS = "Hours";
      protected static final String PROP_MINS = "Mins";
      protected static final String PROP_SECS = "Secs";

      private JPanel timeRangeComp = new JPanel();
      private JComboBox locationComboBox;

      public TimeRangeSelection(DataSourceImpl dataSource) 
              throws VisADException, RemoteException {
          super("Time Range");
          this.dataSource = dataSource;
      }

    protected JComponent doMakeContents() {

         Insets  dfltGridSpacing = new Insets(4, 0, 4, 0);
         String  dfltLblSpacing  = " ";
         Calendar cal = Calendar.getInstance();
         SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy");
         defaultDay = cal.getTime();
         List allComps = new ArrayList();
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
         timeRangeComp = GuiUtils.top(dateTimePanel);
         return timeRangeComp;
    }

    protected JComponent getTimeRangeComponent() {
        return timeRangeComp;
    }
 
    @Override public void applyToDataSelection(DataSelection dataSelection) {
        //System.out.println("applyToDataSelection: dataSelection=" + dataSelection);

        if (dataSelection == null) {
            dataSelection = new DataSelection(true);
        }

        Date beg = begDay.getDate();
        JCalendar cal = begDay.getJCalendar();
        JDayChooser dayChooser = cal.getDayChooser();
        int day = dayChooser.getDay();
        JMonthChooser monthChooser = cal.getMonthChooser();
        int month = monthChooser.getMonth() + 1;
        JYearChooser yearChooser = cal.getYearChooser();
        int year = yearChooser.getYear();

        String begTime = beginTimeFld.getText();
        String[] timeStrings = begTime.split(":");
        int hours = (new Integer(timeStrings[0])).intValue();
        int mins = (new Integer(timeStrings[1])).intValue();
        double secs = (new Double(timeStrings[2] + ".0")).doubleValue();

        Time bTime = new Time(year, month, day, hours, mins, secs);
        double dVal = bTime.getJulianDate();
        Double bigD = new Double(dVal);
        String begTimeStr = bigD.toString();
        dataSelection.putProperty(PROP_BEGTIME, bTime.getDateTimeStr());

        Integer intVal = new Integer(year);
        dataSelection.putProperty(PROP_YEAR, intVal.toString());
        intVal = new Integer(month);
        dataSelection.putProperty(PROP_MONTH, intVal.toString());
        intVal = new Integer(day);
        dataSelection.putProperty(PROP_DAY, intVal.toString());
        intVal = new Integer(hours);
        dataSelection.putProperty(PROP_HOURS, intVal.toString());
        intVal = new Integer(mins);
        dataSelection.putProperty(PROP_MINS, intVal.toString());
        Double doubleVal = new Double(secs);
        dataSelection.putProperty(PROP_SECS, doubleVal.toString());

        Date end = endDay.getDate();
        cal = endDay.getJCalendar();
        dayChooser = cal.getDayChooser();
        day = dayChooser.getDay();
        monthChooser = cal.getMonthChooser();
        month = monthChooser.getMonth() + 1;
        yearChooser = cal.getYearChooser();
        year = yearChooser.getYear();

        String endTime = endTimeFld.getText();
        timeStrings = endTime.split(":");
        hours = (new Integer(timeStrings[0])).intValue();
        mins = (new Integer(timeStrings[1])).intValue();
        secs = (new Double(timeStrings[2] + ".0")).doubleValue();

        Time eTime = new Time(year, month, day, hours, mins, secs);
        dVal = eTime.getJulianDate();
        bigD = new Double(dVal);
        String endTimeStr = bigD.toString();

        dataSelection.putProperty(PROP_ENDTIME, eTime.getDateTimeStr());
        dataSelection.putProperty(PROP_BTIME, begTimeStr);
        dataSelection.putProperty(PROP_ETIME, endTimeStr);
    }
}
