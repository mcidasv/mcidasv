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
        String url = "adde://" + server + "/textdata?&PORT=112&COMPRESS=gzip&USER=GAD&PROJ=6999&GROUP=" + group + "&DESCR=" + filename;
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
            //name = name.toLowerCase().trim();
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
                index++;
                String card = (String)tleCards.get(index);
                int ncomps = decodeCard1(card);
                System.out.println("ncomps=" + ncomps);
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
        int satID = 0;
        int launchYear = 0;
        int intCode = 0;
        int yyddd = 0;
        double dayFraction = 1.0;
        double firstDev = 1.0;
        double secondDev = 1.0;
        double bStar = 1.0;
        int ephemerisType = 0;
        int elementNumber = 0;

        int ret = 0;

        if (card.length() < 67) {
            return ret;
        } else {
            System.out.println("\n" + card);
            int indx = card.indexOf("U");
            String val = card.substring(2, indx);
            satID = (new Integer(val)).intValue();
            System.out.println("    satID=" + satID);
            ++ret;

            ++indx;
            card = advCard(indx, card);
            int tempInt = getInt(5, card);
            launchYear = tempInt/1000;
            intCode = tempInt - launchYear*1000;
            System.out.println("    launchYear=" + launchYear + " intCode=" + intCode);
            ++ret;

            card = advCard(6, card);
            yyddd = getInt(5, card);
            System.out.println("    yyddd=" + yyddd);
            ++ret;

            card = advCard(5, card);
            dayFraction = getDouble(9, card);
            System.out.println("    dayFraction=" + dayFraction);
            ++ret;

            card = advCard(9, card);
            firstDev = getDouble(9, card);
            System.out.println("    firstDev=" + firstDev);
            ++ret;

            card = advCard(9, card);
            secondDev = getDoubleExp(5, 7, card);
            System.out.println("    secondDev=" + secondDev);
            ++ret;

            card = advCard(7, card);
            bStar = getDoubleExp(5, 7, card);
            System.out.println("    bStar=" + bStar);
            ++ret;

            card = advCard(7, card);
            ephemerisType = getInt(1, card);
            System.out.println("    ephemerisType=" + ephemerisType);
            ++ret;

            card = advCard(1, card);
            indx = card.length();
            if (indx > 4) indx = 4;
            elementNumber = getInt(indx, card);
            System.out.println("    elementNumber=" + elementNumber);
            ++ret;
        }
        return ret;
    }

    private String advCard(int index, String card) {
         return (card.substring(index)).trim();
    }

    private int getInt(int index, String card) {
        String val = card.substring(0, index);
        return (new Integer(val)).intValue();
    }

    private double getDoubleExp(int index1, int index2, String card) {
        double val = getDouble(index1, card);
        String str = card.substring(index1, index2);
        int exp = new Integer(str).intValue();
        return val *= Math.pow(10, exp);
    }

    private double getDouble(int index, String card) {
        String val = card.substring(0, index);
        return (new Double(val)).doubleValue();
    }
}
