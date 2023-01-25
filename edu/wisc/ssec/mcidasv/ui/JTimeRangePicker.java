/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2023
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

package edu.wisc.ssec.mcidasv.ui;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import java.awt.Dimension;
import java.util.Calendar;

import javax.swing.GroupLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import name.gano.astro.time.Time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JTimeRangePicker extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(JTimeRangePicker.class);

    private JTextField beginTimeFld;
    private JTextField endTimeFld;
    private String defaultBegTime = "00:00:00";
    private String defaultEndTime = "23:59:59";

    public static final String PROP_BEGTIME = "BeginTime";
    public static final String PROP_ENDTIME = "EndTime";
    protected static final String PROP_BTIME = "BTime";
    protected static final String PROP_ETIME = "ETime";
    protected static final String PROP_YEAR = "Year";
    protected static final String PROP_MONTH = "Month";
    protected static final String PROP_DAY = "Day";
    protected static final String PROP_HOURS = "Hours";
    protected static final String PROP_MINS = "Mins";

    // TJJ use this to seed time objects with year/month/day
    Calendar cal = Calendar.getInstance();

    public JTimeRangePicker() {
        doMakeContents();
    }

    protected JComponent doMakeContents() {

        logger.debug("creating the JTimeRangePicker panel...");

        JLabel begTimeLab = new JLabel("Beg Time:");
        JLabel endTimeLab = new JLabel("End Time:");
        beginTimeFld = new JTextField(defaultBegTime, 8);
        beginTimeFld.setMaximumSize(new Dimension(80, 40));
        beginTimeFld.setToolTipText("HH:MM:SS");
        endTimeFld = new JTextField(defaultEndTime, 8);
        endTimeFld.setMaximumSize(new Dimension(80, 40));
        endTimeFld.setToolTipText("HH:MM:SS");

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout
                .createParallelGroup(LEADING)

                .addGroup(
                        layout.createSequentialGroup()
                            .addComponent(begTimeLab)
                            .addComponent(beginTimeFld))

                .addGroup(
                        layout.createSequentialGroup()
                            .addComponent(endTimeLab)
                            .addComponent(endTimeFld)));

        layout.setVerticalGroup(layout.createParallelGroup(LEADING).addGroup(
                layout.createSequentialGroup()

                        .addGroup(
                                layout.createParallelGroup(BASELINE)
                                    .addComponent(begTimeLab)
                                    .addComponent(beginTimeFld))
                        .addPreferredGap(RELATED)
                        .addPreferredGap(UNRELATED)

                        .addGroup(
                                layout.createParallelGroup(BASELINE)
                                    .addComponent(endTimeLab)
                                    .addComponent(endTimeFld)).addPreferredGap(RELATED)));

        return this;
    }

    public JComponent getTimeRangeComponent() {
        return this;
    }

    /**
     * Validate start time field
     * @return true if ok
     */
    
    public boolean begTimeOk() {
        String begTime = beginTimeFld.getText();
        String[] timeStrings = begTime.split(":");
        int num = timeStrings.length;
        if (num != 3)
            return false;
        int hours = -1;
        int mins = -1;
        int seconds = -1;
        try {
            hours = Integer.parseInt(timeStrings[0]);
            mins = Integer.parseInt(timeStrings[1]);
            seconds = Integer.parseInt(timeStrings[2]);
        } catch (NumberFormatException nfe) {
            return false;
        }
        if ((hours < 0) || (hours > 23))
            return false;
        if ((mins < 0) || (mins > 59))
            return false;
        if ((seconds < 0) || (seconds > 59))
            return false;
        
        return true;
    }

    /**
     * Validate end time field
     * @return true if ok
     */
    
    public boolean endTimeOk() {
        String endTime = endTimeFld.getText();
        String[] timeStrings = endTime.split(":");
        int num = timeStrings.length;
        if (num != 3)
            return false;
        int hours = -1;
        int mins = -1;
        int seconds = -1;
        try {
            hours = Integer.parseInt(timeStrings[0]);
            mins = Integer.parseInt(timeStrings[1]);
            seconds = Integer.parseInt(timeStrings[2]);
        } catch (NumberFormatException nfe) {
            return false;
        }
        if ((hours < 0) || (hours > 23))
            return false;
        if ((mins < 0) || (mins > 59))
            return false;
        if ((seconds < 0) || (seconds > 59))
            return false;
        
        return true;
    }

    /**
     * Make sure the end date/time exceeds the beginning date/time.
     * 
     * @return true if condition met, false otherwise
     * 
     */

    public boolean timeRangeOk() {

        if (! begTimeOk())
            return false;
        if (! endTimeOk())
            return false;

        int hours = 0;
        int mins = 0;
        int seconds = 0;

        String begTime = beginTimeFld.getText();
        String[] timeStrings = begTime.split(":");
        int num = timeStrings.length;
        if (num > 0)
            hours = (new Integer(timeStrings[0])).intValue();
        if (num > 1)
            mins = (new Integer(timeStrings[1])).intValue();
        if (num > 2)
            seconds = (new Integer(timeStrings[2])).intValue();
        
        // Year, Month, and Day are arbitrary, just need to match eTime vals
        Time bTime = new Time(2017, 2, 2, hours, mins, seconds);

        String endTime = endTimeFld.getText();
        timeStrings = endTime.split(":");
        num = timeStrings.length;
        if (num > 0)
            hours = (new Integer(timeStrings[0])).intValue();
        if (num > 1)
            mins = (new Integer(timeStrings[1])).intValue();
        if (num > 2)
            seconds = (new Integer(timeStrings[2])).intValue();
        
        // Year, Month, and Day are arbitrary, just need to match bTime vals
        Time eTime = new Time(2017, 2, 2, hours, mins, seconds);

        if (eTime.getJulianDate() < bTime.getJulianDate()) {
            return false;
        }
        return true;
    }

    public String getBegTimeStr() {
        return beginTimeFld.getText();
    }

    public String getEndTimeStr() {
        return endTimeFld.getText();
    }

}
