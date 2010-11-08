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
import edu.wisc.ssec.mcidasv.data.dateChooser.JDateChooser;

import java.awt.Component;
import java.awt.Insets;
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

      public static final String DATE_FORMAT_NOW = "yyyyMMdd";

      public TimeSelection(DataSourceImpl dataSource) 
              throws VisADException, RemoteException {
          super("Time");
      }

/* code for JDateChooser from
   /home/gad/src/com/toedter/calendar/JDateChooser.java
*/
      protected JComponent doMakeContents() {
          Insets  dfltGridSpacing = new Insets(4, 0, 4, 0);
          String  dfltLblSpacing  = " ";
          Calendar cal = Calendar.getInstance();
          SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy");
          defaultDay = cal.getTime();
          List allComps = new ArrayList();
          allComps.add(new JLabel(" "));
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

          allComps.add(new JLabel(" "));
          allComps.add(new JLabel(" "));
          allComps.add(GuiUtils.lLabel("End:"));
          endTimeFld = new JTextField(defaultEndTime, 10);
          endTime = GuiUtils.doLayout(new Component[] {
                             new JLabel("     "),
                             new JLabel("            Time: "),
                             endTimeFld }, 3,
                             GuiUtils.WT_N, GuiUtils.WT_N);
          allComps.add(endTime);
          endDay = new JDateChooser(defaultDay);
          endDate = GuiUtils.doLayout(new Component[] {
                             new JLabel("     "),
                             new JLabel("            Date: "),
                             endDay }, 3,
                             GuiUtils.WT_N, GuiUtils.WT_N);
          allComps.add(endDate);


          GuiUtils.tmpInsets = GRID_INSETS;
          JPanel dateTimePanel = GuiUtils.doLayout(allComps, 1, GuiUtils.WT_NY,
                                  GuiUtils.WT_N);
          return GuiUtils.top(dateTimePanel);
      }

      @Override public void applyToDataSelection(DataSelection dataSelection) {
         System.out.println("\napplyToDataSelection: dataSelection=" + dataSelection);
         if (dataSelection == null) {
             dataSelection = new DataSelection(true);
         }
         Date beg = begDay.getDate();
         System.out.println("    beg=" + beg);
         System.out.println("    beg: month" + beg.getMonth() + " day=" + beg.getDay() + " year=" + beg.getYear());
         Date end = endDay.getDate();
         System.out.println("    end=" + end);
         System.out.println("    end: month" + end.getMonth() + " day=" + end.getDay() + " year=" + end.getYear());
      }
}
