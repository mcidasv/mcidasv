/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2015
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

import static java.text.DateFormat.getDateInstance;
import static javax.swing.UIManager.getColor;

import com.toedter.calendar.DateUtil;
import com.toedter.calendar.IDateEditor;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.MaskFormatter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * This class is just a {@link com.toedter.calendar.JTextFieldDateEditor} that
 * allows the user to enter either the day within (current) year or a
 * McIDAS-X style {@literal "julian day"} ({@code YYYYDDD} or {@code YYDDD}),
 * in addition to the formatting allowed by {@code JTextFieldDateEditor}.
 */
public class JCalendarDateEditor extends JFormattedTextField implements
    IDateEditor, CaretListener, FocusListener, ActionListener
{
    /** Match day of year. */
    private static final Pattern dayOnly = Pattern.compile("\\d{1,3}");

    /** Match {@code YYYYDDD}. */
    private static final Pattern yearDay = Pattern.compile("\\d{7}");

    /** Match {@code YYDDD} dates. */
    private static final Pattern badYearDay = Pattern.compile("\\d{5}");

    private static final Logger logger =
        LoggerFactory.getLogger(JCalendarDateEditor.class);

    protected Date date;

    protected SimpleDateFormat dateFormatter;

    /** Parse {@code DDD} dates (even if they are one or two digits). */
    private final SimpleDateFormat dayOfYear;

    /** Parse {@code YYYYDDD} dates. */
    private final SimpleDateFormat yearAndDay;

    /** Parse {@code YYDDD} dates. */
    private final SimpleDateFormat badYearAndDay;

    protected MaskFormatter maskFormatter;

    protected String datePattern;

    protected String maskPattern;

    protected char placeholder;

    protected Color darkGreen;

    protected DateUtil dateUtil;

    private boolean isMaskVisible;

    private boolean ignoreDatePatternChange;

    private int hours;

    private int minutes;

    private int seconds;

    private int millis;

    private Calendar calendar;

    public JCalendarDateEditor() {
        this(false, null, null, ' ');
    }

    public JCalendarDateEditor(String datePattern, String maskPattern,
                               char placeholder)
    {
        this(true, datePattern, maskPattern, placeholder);
    }

    public JCalendarDateEditor(boolean showMask, String datePattern,
                               String maskPattern, char placeholder)
    {
        dateFormatter = (SimpleDateFormat)getDateInstance(DateFormat.MEDIUM);
        dayOfYear = new SimpleDateFormat("DDD");
        yearAndDay = new SimpleDateFormat("yyyyDDD");
        badYearAndDay = new SimpleDateFormat("yyDDD");
        dateFormatter.setLenient(false);
        dayOfYear.setLenient(false);
        yearAndDay.setLenient(false);

        setDateFormatString(datePattern);
        if (datePattern != null) {
            ignoreDatePatternChange = true;
        }

        this.placeholder = placeholder;

        if (maskPattern == null) {
            this.maskPattern = createMaskFromDatePattern(this.datePattern);
        } else {
            this.maskPattern = maskPattern;
        }

        setToolTipText(this.datePattern);
        setMaskVisible(showMask);

        addCaretListener(this);
        addFocusListener(this);
        addActionListener(this);
        darkGreen = new Color(0, 150, 0);

        calendar = Calendar.getInstance();

        dateUtil = new DateUtil();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.toedter.calendar.IDateEditor#getDate()
     */
    @Override public Date getDate() {
        try {
            calendar.setTime(dateFormatter.parse(getText()));
            calendar.set(Calendar.HOUR_OF_DAY, hours);
            calendar.set(Calendar.MINUTE, minutes);
            calendar.set(Calendar.SECOND, seconds);
            calendar.set(Calendar.MILLISECOND, millis);
            date = calendar.getTime();
        } catch (ParseException e) {
            date = null;
        }
        return date;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.toedter.calendar.IDateEditor#setDate(java.util.Date)
     */
    @Override public void setDate(Date date) {
        setDate(date, true);
    }

    /**
     * Sets the date.
     *
     * @param date the date
     * @param firePropertyChange true, if the date property should be fired.
     */
    protected void setDate(Date date, boolean firePropertyChange) {
        Date oldDate = this.date;
        this.date = date;

        if (date == null) {
            setText("");
        } else {
            calendar.setTime(date);
            hours = calendar.get(Calendar.HOUR_OF_DAY);
            minutes = calendar.get(Calendar.MINUTE);
            seconds = calendar.get(Calendar.SECOND);
            millis = calendar.get(Calendar.MILLISECOND);

            String formattedDate = dateFormatter.format(date);
            try {
                setText(formattedDate);
            } catch (RuntimeException e) {
                logger.debug("could not set text: {}", e);
            }
        }
        if ((date != null) && dateUtil.checkDate(date)) {
            setForeground(Color.BLACK);
        }

        if (firePropertyChange) {
            firePropertyChange("date", oldDate, date);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.toedter.calendar.IDateEditor#setDateFormatString(java.lang.String)
     */
    @Override public void setDateFormatString(String dateFormatString) {
        if (!ignoreDatePatternChange) {
            try {
                dateFormatter.applyPattern(dateFormatString);
            } catch (RuntimeException e) {
                dateFormatter =
                    (SimpleDateFormat)getDateInstance(DateFormat.MEDIUM);
                dateFormatter.setLenient(false);
            }
            this.datePattern = dateFormatter.toPattern();
            setToolTipText(this.datePattern);
            setDate(date, false);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.toedter.calendar.IDateEditor#getDateFormatString()
     */
    @Override public String getDateFormatString() {
        return datePattern;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.toedter.calendar.IDateEditor#getUiComponent()
     */
    @Override public JComponent getUiComponent() {
        return this;
    }

    private Date attemptParsing(String text) {
        Date result = null;
        try {
            if (dayOnly.matcher(text).matches()) {
                String full = LocalDate.now().getYear() + text;
                result = yearAndDay.parse(full);
            } else if (yearDay.matcher(text).matches()) {
                result = yearAndDay.parse(text);
            } else if (badYearDay.matcher(text).matches()) {
                result = badYearAndDay.parse(text);
            } else {
                result = dateFormatter.parse(text);
            }
        } catch (Exception e) {
            logger.trace("failed to parse '{}'", text);
        }
        return result;
    }

    /**
     * After any user input, the value of the textfield is proofed. Depending on
     * being a valid date, the value is colored green or red.
     *
     * @param event Caret event.
     */
    @Override public void caretUpdate(CaretEvent event) {
        String text = getText().trim();
        String emptyMask = maskPattern.replace('#', placeholder);

        if (text.isEmpty() || text.equals(emptyMask)) {
            setForeground(Color.BLACK);
            return;
        }

        Date parsed = attemptParsing(this.getText());
        if ((parsed != null) && dateUtil.checkDate(parsed)) {
            this.setForeground(this.darkGreen);
        } else {
            this.setForeground(Color.RED);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
     */
    @Override public void focusLost(FocusEvent focusEvent) {
        checkText();
    }

    private void checkText() {
        Date parsedDate = attemptParsing(this.getText());
        if (parsedDate != null) {
            this.setDate(parsedDate, true);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
     */
    @Override public void focusGained(FocusEvent e) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.Component#setLocale(java.util.Locale)
     */
    @Override public void setLocale(Locale locale) {
        if (!locale.equals(getLocale()) || ignoreDatePatternChange) {
            super.setLocale(locale);
            dateFormatter =
                (SimpleDateFormat) getDateInstance(DateFormat.MEDIUM, locale);
            setToolTipText(dateFormatter.toPattern());
            setDate(date, false);
            doLayout();
        }
    }

    /**
     * Creates a mask from a date pattern. This is a very simple (and
     * incomplete) implementation thet works only with numbers. A date pattern
     * of {@literal "MM/dd/yy"} will result in the mask "##/##/##". Probably
     * you want to override this method if it does not fit your needs.
     *
     * @param datePattern Date pattern.
     * @return the mask
     */
    public String createMaskFromDatePattern(String datePattern) {
        String symbols = "GyMdkHmsSEDFwWahKzZ";
        StringBuilder maskBuffer = new StringBuilder(datePattern.length() * 2);
        for (int i = 0; i < datePattern.length(); i++) {
            char ch = datePattern.charAt(i);
            boolean symbolFound = false;
            for (int n = 0; n < symbols.length(); n++) {
                if (symbols.charAt(n) == ch) {
                    maskBuffer.append('#');
                    symbolFound = true;
                    break;
                }
            }
            if (!symbolFound) {
                maskBuffer.append(ch);
            }
        }
        return maskBuffer.toString();
    }

    /**
     * Returns {@code true}, if the mask is visible.
     *
     * @return {@code true}, if the mask is visible.
     */
    public boolean isMaskVisible() {
        return isMaskVisible;
    }

    /**
     * Sets the mask visible.
     *
     * @param isMaskVisible Whether or not the mask should be visible.
     */
    public void setMaskVisible(boolean isMaskVisible) {
        this.isMaskVisible = isMaskVisible;
        if (isMaskVisible) {
            if (maskFormatter == null) {
                try {
                    String mask = createMaskFromDatePattern(datePattern);
                    maskFormatter = new MaskFormatter(mask);
                    maskFormatter.setPlaceholderCharacter(this.placeholder);
                    maskFormatter.install(this);
                } catch (ParseException e) {
                    logger.debug("parsing error: {}", e);
                }
            }
        }
    }

    /**
     * Returns the preferred size. If a date pattern is set, it is the size the
     * date pattern would take.
     */
    @Override public Dimension getPreferredSize() {
        return (datePattern != null)
               ? new JTextField(datePattern).getPreferredSize()
               : super.getPreferredSize();
    }

    /**
     * Validates the typed date and sets it (only if it is valid).
     */
    @Override public void actionPerformed(ActionEvent e) {
        checkText();
    }

    /**
     * Enables and disabled the compoment. It also fixes the background bug
     * 4991597 and sets the background explicitely to a
     * TextField.inactiveBackground.
     */
    @Override public void setEnabled(boolean b) {
        super.setEnabled(b);
        if (!b) {
            super.setBackground(getColor("TextField.inactiveBackground"));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.toedter.calendar.IDateEditor#getMaxSelectableDate()
     */
    @Override public Date getMaxSelectableDate() {
        return dateUtil.getMaxSelectableDate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.toedter.calendar.IDateEditor#getMinSelectableDate()
     */
    @Override public Date getMinSelectableDate() {
        return dateUtil.getMinSelectableDate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.toedter.calendar.IDateEditor#setMaxSelectableDate(java.util.Date)
     */
    @Override public void setMaxSelectableDate(Date max) {
        dateUtil.setMaxSelectableDate(max);
        checkText();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.toedter.calendar.IDateEditor#setMinSelectableDate(java.util.Date)
     */
    @Override public void setMinSelectableDate(Date min) {
        dateUtil.setMinSelectableDate(min);
        checkText();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.toedter.calendar.IDateEditor#setSelectableDateRange(java.util.Date,java.util.Date)
     */
    @Override public void setSelectableDateRange(Date min, Date max) {
        dateUtil.setSelectableDateRange(min, max);
        checkText();
    }
}