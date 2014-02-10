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

package edu.wisc.ssec.mcidasv.ui;

import java.awt.BorderLayout;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import com.toedter.calendar.JCalendar;
import com.toedter.calendar.JDateChooser;

import ucar.unidata.util.GuiUtils;

/**
 * This class is just a backport of the IDV's old {@code DateTimePicker}.
 */
public class JCalendarPicker extends JPanel {

    /** Default time zone */
    private static TimeZone defaultTimeZone;

    /** Date chooser */
    private JDateChooser dateChooser;

    /** SpinnerDateModel */
    private SpinnerDateModel timeModel;

    /** JCalendar */
    private JCalendar jc;

    /**
     * Default constructor. Builds a {@code JCalendarPicker} with a {@code null}
     * initial date and includes the {@literal "hour picker"}.
     */
    public JCalendarPicker() {
        this(null, true);
    }

    /**
     * Creates a {@code JCalendarPicker} with a {@code null} initial date.
     *
     * @param includeHours Whether or not to include an hour picker.
     */
    public JCalendarPicker(boolean includeHours) {
        this(null, includeHours);
    }

    /**
     * Create a {@code JCalendarPicker} with the initial date.
     *
     * @param date Initial date. {@code null} is allowed.
     */
    public JCalendarPicker(Date date) {
        this(date, true);
    }

    /**
     * Create a {@code JCalendarPicker} with the initial date.
     *
     * @param date Initial date. {@code null} is allowed.
     * @param includeHours {@code true} to have an hour picker.
     */
    public JCalendarPicker(Date date, boolean includeHours) {
        jc = new JCalendar();
        Calendar calendar = getCalendar(null);
        jc.getDayChooser().setCalendar(calendar);
        jc.setCalendar(calendar);

        dateChooser = new JDateChooser(jc, new Date(), null, null);
        setLayout(new BorderLayout());

        // Create a date spinner that controls the hours
        timeModel = new SpinnerDateModel(calendar.getTime(), null, null,
            Calendar.HOUR_OF_DAY);
        JSpinner spinner = new JSpinner(timeModel);
        JSpinner.DateEditor editor =
            new JSpinner.DateEditor(spinner, "HH:mm");
        spinner.setEditor(editor);
        JComponent timeComp;
        String timeZoneLabel = ' ' + TimeZone.getDefault().getID();
        if (includeHours) {
            timeComp = GuiUtils.hbox(spinner, new JLabel(timeZoneLabel), 5);
        } else {
            timeComp = new JLabel(timeZoneLabel);
        }
        add(BorderLayout.CENTER, GuiUtils.hbox(dateChooser, timeComp));
        if (date != null) {
            setDate(date);
        }
    }

    /**
     * Set the default time zone for all instances.
     *
     * @param timeZone Default time zone. {@code null} is allowed, but be aware
     * that it will result in calls to {@link #getDefaultTimeZone()} returning
     * {@literal "GMT"}.
     */
    public static void setDefaultTimeZone(TimeZone timeZone) {
        defaultTimeZone = timeZone;
    }

    /**
     * Get the default time zone. If one has not been set, this method will
     * use {@literal "GMT"}.
     *
     * @return Default time zone.
     */
    public static TimeZone getDefaultTimeZone() {
        if (defaultTimeZone == null) {
            defaultTimeZone = TimeZone.getTimeZone("GMT");
        }
        return defaultTimeZone;
    }

    /**
     * Get the Date that has been set
     *
     * @return the date
     */
    public Date getDate() {
        Date d = dateChooser.getDate();
        Calendar c0 = getCalendar(d);
        Calendar c1 = new GregorianCalendar(c0.get(Calendar.YEAR),
            c0.get(Calendar.MONTH),
            c0.get(Calendar.DAY_OF_MONTH));

        if (timeModel != null) {
            Date time = timeModel.getDate();
            Calendar timeCal = getCalendar(time);
            c1.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            c1.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            c1.set(Calendar.SECOND, timeCal.get(Calendar.SECOND));
        }
        return c1.getTime();
    }

    /**
     * Get the calendar for this object
     *
     * @param d the date
     *
     * @return the associated calendar
     */
    private Calendar getCalendar(Date d) {
        Calendar calendar = new GregorianCalendar();
        if (d != null) {
            calendar.setTime(d);
        }
        return calendar;
    }

    /**
     * Set the Date.
     *
     * @param d the new Date
     */
    public void setDate(Date d) {
        Calendar c = getCalendar(d);
        dateChooser.setDate(c.getTime());
        timeModel.setValue(d);
    }
}
