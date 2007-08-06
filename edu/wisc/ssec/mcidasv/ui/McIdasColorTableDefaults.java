package edu.wisc.ssec.mcidasv.ui;


import edu.wisc.ssec.mcidas.EnhancementTable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.StringTokenizer;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.ui.colortable.ColorTableDefaults;

import ucar.unidata.util.ColorTable;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Resource;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;



import visad.*;

import java.awt.Color;


import java.io.IOException;

import java.lang.Integer;

import java.net.URL;

import java.util.ArrayList;
import java.util.Hashtable;

import java.util.List;
import java.util.Vector;


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
        return makeTableFromASCII(name, false);
    }

    /**
     * Read in and process a hydra color table
     *
     * @param name File name
     * @param fromAuxdata Is this file from the auxdata dir
     * @return The processed CT data
     *
     * @throws IllegalArgumentException When bad things happen
     */
    public static final float[][] makeTableFromASCII(String name,
            boolean fromAuxdata)
            throws IllegalArgumentException {
        float colorTable[][] = new float[3][256];
        try {
/*
            if (fromAuxdata) {
                URL enhancement = Resource.getURL("/auxdata/ui/colortables/"
                                      + name);
                et = new EnhancementTable(enhancement);
            } else {
                URL url = IOUtil.getURL(name, McIdasColorTableDefaults.class);
                et = new EnhancementTable(url);
            }
*/
            InputStream is = IOUtil.getInputStream(name);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is));
            String lineOut = reader.readLine();
            StringTokenizer tok = null;
            Float fColor;
            int red = 0;
            int green = 0;
            int blue = 0;
            List colors = new ArrayList();
            while (lineOut != null) {
                if (!lineOut.startsWith("!")) {
                    tok = new StringTokenizer(lineOut," ");
                    red = new Integer(tok.nextToken()).intValue();
                    green = new Integer(tok.nextToken()).intValue();
                    blue = new Integer(tok.nextToken()).intValue();
                    colors.add(new Color(red, green, blue));
                }
                lineOut = reader.readLine();
            }
            colorTable = toArray(colors);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.toString());
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
        }
        return colorTable;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param cat _more_
     * @param file _more_
     * @param lines _more_
     * @param delimiter _more_
     *
     * @return _more_
     */
/*
    public static List makeRgbColorTables(String name, String cat,
                                          String file, List lines,
                                          String delimiter) {
        ArrayList colors = new ArrayList();
        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            String line = (String) lines.get(lineIdx);
            line = stripComments(line);
            if (line.startsWith("ncolors") || line.startsWith("ntsc")) {
                continue;
            }
            if ((line.length() == 0) || line.startsWith("#")) {
                continue;
            }

            List toks = StringUtil.split(line, delimiter, true, true);
            try {
                colors.add(new Color(toInt(toks.get(0)), toInt(toks.get(1)),
                                     toInt(toks.get(2))));
            } catch (NumberFormatException nfe) {
                throw new IllegalStateException("Bad number format in line:"
                        + line + " from file:" + file);
            } catch (IllegalArgumentException iae) {
                throw new IllegalStateException("Bad color value in line:"
                        + line + " from file:" + file);
            }
        }
        if ((colors != null) && (colors.size() > 0)) {
            return Misc.newList(makeColorTable(name, cat, colors));
        }
        return null;
    }
*/

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
/*
    private static int toInt(Object o) {
        String s = o.toString().trim();
        return (int) (new Double(s).doubleValue());
    }
*/

    /**
     * _more_
     *
     *
     * @param name _more_
     * @param cat _more_
     * @param file _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
/*
    public static List makeRgbColorTables(String name, String cat,
                                          String file)
            throws IOException {
        List   tables   = new ArrayList();
        String contents = IOUtil.readContents(file);
        if (contents.indexOf("ncolors=") >= 0) {
            return makeNclRgbColorTables(name, cat, file, contents);
        }


        List      lines     = StringUtil.split(contents, "\n", true, true);
        ArrayList colors    = null;


        String    delimiter = null;
        for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            String line = (String) lines.get(lineIdx);

            line = stripComments(line);
            if ((line.length() == 0) || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("*")) {
                if (colors != null) {
                    tables.add(makeColorTable(name, cat, colors));
                    //                    break;
                }
                colors = new ArrayList();
                name   = line.substring(1);
                int index = name.indexOf("#");
                if (index >= 0) {
                    name = name.substring(0, index).trim();
                }
                continue;
            }
            if (delimiter == null) {
                if (line.indexOf(",") >= 0) {
                    delimiter = ",";
                } else {
                    delimiter = " ";
                }
            }
            List toks = StringUtil.split(line, delimiter, true, true);
            //Handle simple rgb

            if (toks.size() == 3) {
                return makeRgbColorTables(name, cat, file, lines, delimiter);
            }
            if (toks.size() != 8) {
                throw new IllegalArgumentException(
                    "Incorrect number of tokens in:" + file + " Read:"
                    + line);
            }

            int idx = 0;
            try {
                int from       = toInt(toks.get(idx++));
                int to         = toInt(toks.get(idx++));
                int fromRed    = toInt(toks.get(idx++));
                int toRed      = toInt(toks.get(idx++));
                int redWidth   = toRed - fromRed + 1;
                int fromGreen  = toInt(toks.get(idx++));
                int toGreen    = toInt(toks.get(idx++));
                int greenWidth = toGreen - fromGreen + 1;
                int fromBlue   = toInt(toks.get(idx++));
                int toBlue     = toInt(toks.get(idx));
                int blueWidth  = toBlue - fromBlue + 1;

                int width      = to - from + 1;
                for (int i = 0; i < width; i++) {
                    double percent = i / (double) width;
                    Color c = new Color((int) (fromRed + redWidth * percent),
                                        (int) (fromGreen
                                            + greenWidth
                                              * percent), (int) (fromBlue
                                                  + blueWidth * percent));

                    colors.add(c);
                }
            } catch (NumberFormatException nfe) {
                throw new IllegalStateException("Bad number format in line:"
                        + line + " from file:" + file);
            }
        }
        if ((colors != null) && (colors.size() > 0)) {
            tables.add(makeColorTable(name, cat, colors));
        }
        return tables;
    }
*/

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
