/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
 *
 * All Rights Reserved
 *
 * McIDAS-V is built on Unidata's IDV, as well as SSEC's VisAD and HYDRA
 * projects. Parts of McIDAS-V source code are based on IDV, VisAD, and
 * HYDRA source code.
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.hydra;

import java.text.DecimalFormat;
import javax.swing.JLabel;

/**
 * @author rink
 */
public class InfoLabel {
    float lat;
    float lon;
    float value;
    String dateTime;
    String description;

    JLabel label = new JLabel();

    double[] vals1 = new double[]{Double.NaN};
    double[] values = vals1;

    DecimalFormat numFmt = new DecimalFormat();

    DecimalFormat latlonFmt = new DecimalFormat();

    public InfoLabel() {
        latlonFmt.setMaximumFractionDigits(2);
        latlonFmt.setMinimumFractionDigits(2);
        numFmt.setMaximumFractionDigits(2);
        numFmt.setMinimumFractionDigits(2);
    }

    public void setNumberFormat(DecimalFormat numFmt) {
        this.numFmt = numFmt;
    }

    public void setLatLon(float lat, float lon) {
        this.lat = lat;
        this.lon = lon;
        makeLabel();
    }

    public void setLatLonAndValue(float lat, float lon, float value) {
        this.lat = lat;
        this.lon = lon;
        vals1[0] = value;
        values = vals1;
        makeLabel();
    }

    public void setValue(double value) {
        vals1[0] = value;
        values = vals1;
        makeLabel();
    }

    public void setValues(double[] vals) {
        values = vals;
        makeLabel();
    }

    public void setLatLonAndValues(float lat, float lon, double[] vals) {
        this.lat = lat;
        this.lon = lon;
        values = vals;
        makeLabel();
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
        makeLabel();
    }

    public void setDescription(String desc) {
        this.description = desc;
        makeLabel();
    }

    public void setDescAndDateTime(String desc, String dateTime) {
        this.description = desc;
        this.dateTime = dateTime;
        makeLabel();
    }

    public JLabel getJLabel() {
        return label;
    }

    private void makeLabel() {
        String txt = "Lon: " + latlonFmt.format(lon) + "  Lat: " + latlonFmt.format(lat);
        txt = txt + "  Val: ";
        for (int k = 0; k < values.length; k++) {
            txt = txt + numFmt.format(values[k]) + ", ";
        }
        if (description != null) {
            txt = txt + " " + description;
        }
        if (dateTime != null) {
            txt = txt + " " + dateTime;
        }
        label.setText(txt);
    }
}
