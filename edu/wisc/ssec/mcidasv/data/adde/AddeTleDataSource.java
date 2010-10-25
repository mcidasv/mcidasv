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

package edu.wisc.ssec.mcidasv.data.adde;

import edu.wisc.ssec.mcidas.adde.AddeTextReader;
import edu.wisc.ssec.mcidasv.chooser.adde.AddeTleChooser;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import visad.Data;
import visad.DataReference;
import visad.Text;
import visad.VisADException;
import visad.VisADException;

import visad.data.mcidas.AddeTextAdapter;

import java.io.FileInputStream;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;



/**
 * Class for data sources of ADDE text data.  These may be generic
 * files or weather bulletins
 *
 * @author IDV development team
 * @version $Revision$
 */

public class AddeTleDataSource extends DataSourceImpl {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            AddeTleDataSource.class.getName());

    private List tleCards = new ArrayList();
    private List choices = new ArrayList();

    private int satId = 0;
    private int launchYear = 0;
    private int intCode = 0;
    private int yyddd = 0;
    private double dayFraction = 1.0;
    private double firstDev = 1.0;
    private double secondDev = 1.0;
    private double bStar = 1.0;
    private int ephemerisType = 0;
    private int elementNumber = 0;

    private double inclination = 1.0;
    private double rightAscension = 1.0;
    private double eccentricity = 1.0;
    private double argOfPerigee = 1.0;
    private double meanAnomaly = 1.0;
    private double meanMotion = 1.0;
    private int revolutionNumber = 0;

    /**
     * Default bean constructor for persistence; does nothing.
     */
    public AddeTleDataSource() {}

    /**
     * Create a new AddeTleDataSource
     *
     * @param descriptor    descriptor for this source
     * @param filename      ADDE URL
     * @param properties    extra properties for this source
     *
     */
    public AddeTleDataSource(DataSourceDescriptor descriptor,
                              String filename, Hashtable properties) {
        super(descriptor, filename, "Text data source", properties);
/*
        System.out.println("\nAddeTleDataSource:");
        System.out.println("    descriptor=" + descriptor);
        System.out.println("    filename=" + filename);
*/
        //System.out.println("properties:");
        String key = AddeTleChooser.TLE_SERVER_NAME_KEY;
        Object server = properties.get(key);
        //System.out.println("    " + key + "=" + server);
        key = AddeTleChooser.TLE_GROUP_NAME_KEY;
        Object group = properties.get(key);
        //System.out.println("    " + key + "=" + group);
/*
        key = AddeTleChooser.SATELLITE_SERVER_NAME_KEY;
        Object val = properties.get(key);
        System.out.println("    " + key + "=" + val);
        key = AddeTleChooser.SATELLITE_GROUP_NAME_KEY;
        val = properties.get(key);
        System.out.println("    " + key + "=" + val);
*/
        //String url = "adde://noaaport.ssec.wisc.edu/textdata?&PORT=112&COMPRESS=gzip&USER=GAD&PROJ=6999&GROUP=POESNAV&DESCR=TLE";
        key = AddeTleChooser.TLE_USER_ID_KEY;
        Object user = properties.get(key);
        key = AddeTleChooser.TLE_PROJECT_NUMBER_KEY;
        Object proj = properties.get(key);
        String url = "adde://" + server + "/textdata?&PORT=112&COMPRESS=gzip&USER=" + user + "&PROJ=" + proj + "&GROUP=" + group + "&DESCR=" + filename;
        System.out.println("\n" + url + "\n");
        AddeTextReader reader = new AddeTextReader(url);
        List lines = null;
        if ("OK".equals(reader.getStatus())) {
            lines = reader.getLinesOfText();
        }
        if (lines == null) {
            System.out.println("\nproblem reading TLE file");
        } else {
            String[] cards = StringUtil.listToStringArray(lines);
            //System.out.println("\n");
            for (int i=0; i<cards.length; i++) {
                tleCards.add(cards[i]);
                //System.out.println(cards[i]);
                int indx = cards[i].indexOf(" ");
                if (indx < 0) {
                    choices.add(cards[i]);
                }
            }
            //System.out.println("\n");
        }
    }



    /**
     * Make the data choices assoicated with this source.
     */
    protected void doMakeDataChoices() {
        String category = "unknown";
        for (int i=0; i<choices.size(); i++) {
            String name  = ((String)choices.get(i)).trim();
            addDataChoice(
                new DirectDataChoice(
                    this, name, name, name,
                    DataCategory.parseCategories(category, false)));
        }
    }

    /**
     * Actually get the data identified by the given DataChoce. The default is
     * to call the getDataInner that does not take the requestProperties. This
     * allows other, non unidata.data DataSource-s (that follow the old API)
     * to work.
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The visad.Text object
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
/*
        System.out.println("\ngetDataInner:");
        System.out.println("    dataChoice=" + dataChoice);
        System.out.println("    category=" + category);
        System.out.println("    dataSelection=" + dataSelection + "\n");
*/

        boolean gotit = false;
        int index = -1;
        String choiceName = dataChoice.getName();
        List tleComps = new ArrayList();
        while(!gotit) {
            index++;
            String name = ((String)tleCards.get(index)).trim();
            if (name.equals(choiceName)) {
 
                System.out.println("\n" + tleCards.get(index));
                System.out.println(tleCards.get(index+1));
                System.out.println(tleCards.get(index+2) + "\n");

                index++;
                String card = (String)tleCards.get(index);
                int ncomps = decodeCard1(card);
                if (ncomps < 0) return null;
                index++;
                card = (String)tleCards.get(index);
                ncomps += decodeCard2(card);
                if (ncomps < 0) return null;
                System.out.println("\nncomps=" + ncomps);
                gotit= true;
            }
            if (index+3 > tleCards.size()) gotit = true;
        }
        return null;
    }

    private int decodeCard1(String card) {
/*
        System.out.println("\ndecodeCard1:");
        System.out.println("    card=" + card);
        System.out.println("    length=" + card.length());
*/
        int ret = 0;
        System.out.println(card);
        int ck1 = checksum(card.substring(0, 68));
        String str = card.substring(0, 1);
        if (str.equals("1")) {
            satId = getInt(2, 7, card);
            System.out.println("    satId = " + satId);
            ++ret;

            launchYear = getInt(9, 11, card);
            System.out.println("    launchYear = " + launchYear);
            ++ret;

            intCode = getInt(11, 14, card);
            System.out.println("    intCode = " + intCode);
            ++ret;

            yyddd = getInt(18, 23, card);
            System.out.println("    yyddd = " + yyddd);
            ++ret;

            dayFraction = getDouble(23, 32, card);
            System.out.println("    dayFraction = " + dayFraction);
            ++ret;

            firstDev = getDouble(33, 43, card);
            System.out.println("    firstDev = " + firstDev);
            ++ret;

            str = card.substring(44,50);
            str += "E";
            str += card.substring(50,52);
            secondDev = getDouble(0, str.length(), str);
            System.out.println("    secondDev = " + secondDev);
            ++ret;

            str = card.substring(53,59);
            str += "E";
            str += card.substring(59,61);
            bStar = getDouble(0, str.length(), str);
            System.out.println("    bStar = " + bStar);
            ++ret;

            ephemerisType = getInt(62, 63, card);
            System.out.println("    ephemerisType = " + ephemerisType);
            ++ret;

            elementNumber = getInt(64, 68, card);
            System.out.println("    elementNumber = " + elementNumber);
            ++ret;

            int check = card.codePointAt(68) - 48;
            if (check != ck1) {
                System.out.println("***** Failed checksum *****");
                ret = -1;
            }
        }
        return ret;
    }

    private int decodeCard2(String card) {
/*
        System.out.println("\ndecodeCard2:");
        System.out.println("    card=" + card);
        System.out.println("    length=" + card.length());
*/
        int ret = 0;
        System.out.println("\n" + card);
        int ck1 = checksum(card.substring(0, 68));
        String str = card.substring(0, 1);
        if (str.equals("2")) {
            int nsat = getInt(2, 7, card);
            System.out.println("    nsat = " + nsat);
            if (nsat == satId) {
                inclination = getDouble(8, 16, card);
                System.out.println("    inclination = " + inclination);
                ++ret;

                rightAscension = getDouble(17, 25, card);
                System.out.println("    rightAscension = " + rightAscension);
                ++ret;

                eccentricity = getDouble(26, 33, card);
                System.out.println("    eccentricity = " + eccentricity);
                ++ret;

                argOfPerigee = getDouble(34, 42, card);
                System.out.println("    argOfPerigee = " + argOfPerigee);
                ++ret;

                meanAnomaly = getDouble(43, 51, card);
                System.out.println("    meanAnomaly = " + meanAnomaly);
                ++ret;

                meanMotion = getDouble(52, 63, card);
                System.out.println("    meanMotion = " + meanMotion);
                ++ret;

                revolutionNumber = getInt(63, 68, card);
                System.out.println("    revolutionNumber = " + revolutionNumber);
                ++ret;

                int check = card.codePointAt(68) - 48;
                if (check != ck1) {
                    System.out.println("***** Failed checksum *****");
                    ret = -1;
                }
            }
        }
        return ret;
    }

    private int getInt(int beg, int end,  String card) {
        String str = card.substring(beg, end);
        str = str.trim();
        return (new Integer(str)).intValue();
    }

    private double getDouble(int beg, int end, String card) {
        String str = card.substring(beg, end);
        str = str.trim();
        return (new Double(str)).doubleValue();
    }

    private int checksum(String str) {
        int sum = 0;
        byte[] bites = str.getBytes();
        for (int i=0; i<bites.length; i++) {
            int val = (int)bites[i];
            if ((val > 47) && (val < 58)) {
                sum += val - 48;
            } else if (val == 45) {
                ++sum;
            }
        }
        return sum % 10;
    }
}
