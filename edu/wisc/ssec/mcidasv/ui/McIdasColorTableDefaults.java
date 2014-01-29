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


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.StringTokenizer;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;

import visad.*;

import java.awt.Color;

import java.lang.Integer;

import java.util.ArrayList;

import java.util.List;

/**
 * A class to provide color tables suitable for data displays.
 * Uses some code by Ugo Taddei. All methods are static.
 *
 * @version $Id$
 */
public class McIdasColorTableDefaults {

    /** The name of the &quot;aod&quot; color table */
    public static final String NAME_AOD = "AOD";

    /** The name of the &quot;cot&quot; color table */
    public static final String NAME_COT = "COT";

    /**
     * Create a ColorTable and add it to the given list
     *
     * @param name The CT name
     * @param category Its category
     * @param table The actual data
     * @return The color table
     */
    public static ColorTable createColorTable(String name, String category,
            float[][] table) {
        return createColorTable(new ArrayList(), name, category, table);
    }


    /**
     * _more_
     *
     * @param l _more_
     * @param name _more_
     * @param category _more_
     * @param table _more_
     *
     * @return _more_
     */
    public static ColorTable createColorTable(ArrayList l, String name,
            String category, float[][] table) {
        return createColorTable(l, name, category, table, false);
    }

    /**
     * Create a ColorTable and add it to the given list
     *
     * @param l List to add the ColorTable to
     * @param name The CT name
     * @param category Its category
     * @param table The actual data
     * @param tableFlipped If true then the table data is not in row major order
     * @return The color table
     */
    public static ColorTable createColorTable(ArrayList l, String name,
            String category, float[][] table, boolean tableFlipped) {

        // ensure all R G and B values in the table are in 0.0 to 1.0 range
        for (int i = 0; i < table.length; i++) {
            for (int j = 0; j < table[i].length; j++) {
                if (table[i][j] < 0.0) {
                    //System.out.println(" bad value "+table [i][j] );
                    table[i][j] = 0.0f;
                } else if (table[i][j] > 1.0) {
                    //System.out.println(" bad value "+table [i][j]+" for table "
                    // +name +"  comp "+i+"  pt "+j);
                    table[i][j] = 1.0f;
                }
            }
        }

        ColorTable colorTable = new ColorTable(name.toUpperCase(), name,
                                    category, table, tableFlipped);
        l.add(colorTable);
        return colorTable;
    }

    /**
     * Read in and process a hydra color table
     *
     * @param name File name
     * @return The processed CT data
     *
     * @throws IllegalArgumentException
     */
    public static final float[][] makeTableFromASCII(String name)
            throws IllegalArgumentException {
        float colorTable[][];
        try {
            InputStream is = IOUtil.getInputStream(name);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is));
            String lineOut = reader.readLine();
            StringTokenizer tok = null;
            List reds = new ArrayList();
            List greens = new ArrayList();
            List blues = new ArrayList();
            while (lineOut != null) {
                if (!lineOut.startsWith("!")) {
                    tok = new StringTokenizer(lineOut," ");
                    reds.add(tok.nextToken());
                    greens.add(tok.nextToken());
                    blues.add(tok.nextToken());
                }
                lineOut = reader.readLine();
            }
            if (StringUtil.isDigits((String)reds.get(0))) {
                colorTable = processInts(reds, greens, blues);
            } else {
                colorTable = processFloats(reds, greens, blues);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.toString());
        }
        return colorTable;
    }


    /**
     * Convert strings of integers to scaled colors.
     *
     * @param reds Array containing red strings.
     * @param greens Array containing green strings.
     * @param blues Array containing blue strings.
     *
     * @return Color table of scaled floats.
     */
    static private float[][] processInts(List reds, List greens, List blues) {
        List colors = new ArrayList();
        int red = 0;
        int green = 0;
        int blue = 0;
        int num = reds.size();
        float colorTable[][] = new float[3][num];
        for (int i=0; i<num; i++) {
            red = new Integer((String)reds.get(i)).intValue();
            green = new Integer((String)greens.get(i)).intValue();
            blue = new Integer((String)blues.get(i)).intValue();
            colors.add(new Color(red, green, blue));
        }
        colorTable = toArray(colors);
        return colorTable;
    }


    /**
     * Convert strings of floats to scaled colors.
     *
     * @param reds List containing red strings.
     * @param greens List containing green strings.
     * @param blues List containing blue strings.
     *
     * @return Color table of scaled floats.
     */
    static private float[][] processFloats(List reds, List greens, List blues) {
        int num = reds.size();
        float colorTable[][] = new float[3][num];
        for (int i=0; i<num; i++) {
            colorTable[0][i] = new Float((String)reds.get(i)).floatValue();
            colorTable[1][i] = new Float((String)greens.get(i)).floatValue();
            colorTable[2][i] = new Float((String)blues.get(i)).floatValue();
        }
        return colorTable;
    }

    /**
     * Utility to convert list of colors to float array
     *
     * @param colors colors
     *
     * @return color array
     */
    private static float[][] toArray(List colors) {
        float colorTable[][] = new float[3][colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            Color c = (Color) colors.get(i);
            colorTable[0][i] = ((float) c.getRed()) / 255.f;
            colorTable[1][i] = ((float) c.getGreen()) / 255.f;
            colorTable[2][i] = ((float) c.getBlue()) / 255.f;
/*
            System.out.println(colorTable[0][i] + " " +
                               colorTable[1][i] + " " +
                               colorTable[2][i]);
*/
        }
        return colorTable;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param cat _more_
     * @param colors _more_
     *
     * @return _more_
     */
    private static ColorTable makeColorTable(String name, String cat,
                                             ArrayList colors) {
        ColorTable ct = new ColorTable(name, cat, null);
        ct.setTable(colors);
        return ct;
    }
}
