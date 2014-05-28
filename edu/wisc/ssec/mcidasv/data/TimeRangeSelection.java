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

package edu.wisc.ssec.mcidasv.data;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import edu.wisc.ssec.mcidasv.Constants;

import edu.wisc.ssec.mcidasv.data.dateChooser.*;

import java.awt.Dimension;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;

import javax.swing.*;

import name.gano.astro.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSourceImpl;

import visad.VisADException;

public class TimeRangeSelection extends DataSelectionComponent implements Constants {

      private static final Logger logger = LoggerFactory.getLogger(TimeRangeSelection.class);

      private JTextField beginTimeFld;
      private JTextField endTimeFld;
      private String defaultBegTime = "00:00:00";
      private String defaultEndTime = "23:59:59";
      private Date defaultDay = null;

      private JDateChooser begDay = null;
      private JDateChooser endDay = null;

      public static final String PROP_BEGTIME = "BeginTime";
      public static final String PROP_ENDTIME = "EndTime";
      protected static final String PROP_BTIME = "BTime";
      protected static final String PROP_ETIME = "ETime";
      protected static final String PROP_YEAR = "Year";
      protected static final String PROP_MONTH = "Month";
      protected static final String PROP_DAY = "Day";
      protected static final String PROP_HOURS = "Hours";
      protected static final String PROP_MINS = "Mins";
      protected static final String PROP_SECS = "Secs";

      private JPanel timeRangeComp = new JPanel();

      public TimeRangeSelection(DataSourceImpl dataSource) 
              throws VisADException, RemoteException {
          super("Time Range");
      }

    protected JComponent doMakeContents() {

        logger.debug("creating the TimeRangeSelection panel...");
    	JLabel begLab = new JLabel("  Begin");
        JLabel endLab = new JLabel("  End");
        JLabel begTimeLab = new JLabel("      Time:");
        JLabel endTimeLab = new JLabel("      Time:");
        JLabel begDateLab = new JLabel("      Date:");
        JLabel endDateLab = new JLabel("      Date:");
        beginTimeFld = new JTextField(defaultBegTime, 8);
        beginTimeFld.setMaximumSize(new Dimension(80, 40));
        endTimeFld = new JTextField(defaultEndTime, 8);
        endTimeFld.setMaximumSize(new Dimension(80, 40));
        Calendar cal = Calendar.getInstance();
        defaultDay = cal.getTime();
        begDay = new JDateChooser(defaultDay);
        begDay.setMaximumSize(new Dimension(140, 20));
        endDay = new JDateChooser(defaultDay);
        endDay.setMaximumSize(new Dimension(140, 20));
        
        // make them listen to each other to maintain range validity (beg <= end) and vice versa
        begDay.getJCalendar().getDayChooser().setName(JDayChooser.BEG_DAY);
        endDay.getJCalendar().getDayChooser().setName(JDayChooser.END_DAY);
        begDay.getJCalendar().getDayChooser().addPropertyChangeListener("day", endDay);
        endDay.getJCalendar().getDayChooser().addPropertyChangeListener("day", begDay);

        GroupLayout layout = new GroupLayout(timeRangeComp);
        timeRangeComp.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
                .addComponent(begLab)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(begTimeLab)
                    .addGap(GAP_RELATED)
                    .addComponent(beginTimeFld))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(begDateLab)
                    .addGap(GAP_RELATED)
                    .addComponent(begDay))
                .addComponent(endLab)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(endTimeLab)
                    .addGap(GAP_RELATED)
                    .addComponent(endTimeFld))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(endDateLab)
                    .addGap(GAP_RELATED)
                    .addComponent(endDay))
         );

        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(begLab)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(begTimeLab)
                    .addComponent(beginTimeFld))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(begDateLab)
                    .addComponent(begDay))
                .addPreferredGap(UNRELATED)
                .addComponent(endLab)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(endTimeLab)
                    .addComponent(endTimeFld))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(endDateLab)
                    .addComponent(endDay)))
         );

         return timeRangeComp;
    }

    protected JComponent getTimeRangeComponent() {
        return timeRangeComp;
    }

    public boolean begTimeOk() {
        String begTime = beginTimeFld.getText();
        String[] timeStrings = begTime.split(":");
        int num = timeStrings.length;
        if (num != 3) return false;
        int hours = -1;
        int mins = -1;
        int secs = -1; 
        try {
        	hours = Integer.parseInt(timeStrings[0]);
        	mins = Integer.parseInt(timeStrings[1]);
        	secs = Integer.parseInt(timeStrings[2]);
        } catch (NumberFormatException nfe) {
        	return false;
        }
        if ((hours < 0) || (hours > 23)) return false;
        if ((mins < 0) || (mins > 59)) return false;
        if ((secs < 0) || (secs > 59)) return false;
        return true;
    }
    
    public boolean endTimeOk() {
        String endTime = endTimeFld.getText();
        String[] timeStrings = endTime.split(":");
        int num = timeStrings.length;
        if (num != 3) return false;
        int hours = -1;
        int mins = -1;
        int secs = -1;
        try {
        	hours = Integer.parseInt(timeStrings[0]);
        	mins = Integer.parseInt(timeStrings[1]);
        	secs = Integer.parseInt(timeStrings[2]);
        } catch (NumberFormatException nfe) {
        	return false;
        }
        if ((hours < 0) || (hours > 23)) return false;
        if ((mins < 0) || (mins > 59)) return false;
        if ((secs < 0) || (secs > 59)) return false;
        return true;
    }
    
    public boolean timeRangeOk() {
    	
    	logger.info("timeRangeOk...");
    	if (! begTimeOk()) return false;
    	if (! endTimeOk()) return false;
    	
        JCalendar cal = begDay.getJCalendar();
        JDayChooser dayChooser = cal.getDayChooser();
        int day = dayChooser.getDay();
        JMonthChooser monthChooser = cal.getMonthChooser();
        int month = monthChooser.getMonth() + 1;
        JYearChooser yearChooser = cal.getYearChooser();
        int year = yearChooser.getYear();

        int hours = 0;
        int mins = 0;
        double secs = 0.0;
        String begTime = beginTimeFld.getText();
        String[] timeStrings = begTime.split(":");
        int num = timeStrings.length;
        if (num > 0)
            hours = (new Integer(timeStrings[0])).intValue();
        if (num > 1)
            mins = (new Integer(timeStrings[1])).intValue();
        if (num > 2)
            secs = (new Integer(timeStrings[2])).intValue();

        Time bTime = new Time(year, month, day, hours, mins, secs);

        cal = endDay.getJCalendar();
        dayChooser = cal.getDayChooser();
        day = dayChooser.getDay();
        monthChooser = cal.getMonthChooser();
        month = monthChooser.getMonth() + 1;
        yearChooser = cal.getYearChooser();
        year = yearChooser.getYear();

        String endTime = endTimeFld.getText();
        timeStrings = endTime.split(":");
        num = timeStrings.length;
        if (num > 0)
            hours = (new Integer(timeStrings[0])).intValue();
        if (num > 1)
            mins = (new Integer(timeStrings[1])).intValue();
        if (num > 2)
            secs = (new Integer(timeStrings[2])).intValue();

        Time eTime = new Time(year, month, day, hours, mins, secs);
        
        if (eTime.getJulianDate() < bTime.getJulianDate()) {
        	return false;
        }
        return true;
    }
    
    @Override public void applyToDataSelection(DataSelection dataSelection) {

    	logger.info("applyToDataSelection...");
    	
    	if (! begTimeOk()) return;
    	if (! endTimeOk()) return;
    	
        if (dataSelection == null) {
            dataSelection = new DataSelection(true);
        }

        JCalendar cal = begDay.getJCalendar();
        JDayChooser dayChooser = cal.getDayChooser();
        int day = dayChooser.getDay();
        JMonthChooser monthChooser = cal.getMonthChooser();
        int month = monthChooser.getMonth() + 1;
        JYearChooser yearChooser = cal.getYearChooser();
        int year = yearChooser.getYear();

        int hours = 0;
        int mins = 0;
        double secs = 0.0;
        String begTime = beginTimeFld.getText();
        String[] timeStrings = begTime.split(":");
        int num = timeStrings.length;
        if (num > 0)
            hours = (new Integer(timeStrings[0])).intValue();
        if (num > 1)
            mins = (new Integer(timeStrings[1])).intValue();
        if (num > 2)
            secs = (new Integer(timeStrings[2])).intValue();
        if ((hours < 0) || (hours > 23)) hours = 0;
        if ((mins < 0) || (mins > 59)) mins = 0;
        if ((secs < 0.0) || (secs > 59.0)) secs = 0.0;

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

        cal = endDay.getJCalendar();
        dayChooser = cal.getDayChooser();
        day = dayChooser.getDay();
        monthChooser = cal.getMonthChooser();
        month = monthChooser.getMonth() + 1;
        yearChooser = cal.getYearChooser();
        year = yearChooser.getYear();

        String endTime = endTimeFld.getText();
        timeStrings = endTime.split(":");
        num = timeStrings.length;
        if (num > 0)
            hours = (new Integer(timeStrings[0])).intValue();
        if (num > 1)
            mins = (new Integer(timeStrings[1])).intValue();
        if (num > 2)
            secs = (new Integer(timeStrings[2])).intValue();
        if ((hours < 0) || (hours > 23)) hours = 0;
        if ((mins < 0) || (mins > 59)) mins = 0;
        if ((secs < 0.0) || (secs > 59.0)) secs = 0.0;

        Time eTime = new Time(year, month, day, hours, mins, secs);
        dVal = eTime.getJulianDate();
        bigD = new Double(dVal);
        String endTimeStr = bigD.toString();

        dataSelection.putProperty(PROP_ENDTIME, eTime.getDateTimeStr());
        dataSelection.putProperty(PROP_BTIME, begTimeStr);
        dataSelection.putProperty(PROP_ETIME, endTimeStr);
    }
}
