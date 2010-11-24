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
import edu.wisc.ssec.mcidasv.data.TimeSelection;
import edu.wisc.ssec.mcidasv.data.adde.sgp4.*;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import visad.Data;
import visad.DataReference;
import visad.Text;
import visad.VisADException;
import visad.VisADException;

import visad.data.mcidas.AddeTextAdapter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.net.URL;
import java.net.URLConnection;

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

    private SGP4SatData data = new SGP4SatData();

    public static double pi = SGP4unit.pi;

    private Hashtable selectionProps;

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
        String key = AddeTleChooser.TLE_SERVER_NAME_KEY;
        if (properties.containsKey(key)) {
            Object server = properties.get(key);
            key = AddeTleChooser.TLE_GROUP_NAME_KEY;
            Object group = properties.get(key);
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
                for (int i=0; i<cards.length; i++) {
                    tleCards.add(cards[i]);
                    int indx = cards[i].indexOf(" ");
                    if (indx < 0) {
                        choices.add(cards[i]);
                    }
                }
            }
        } else {
            try {
                key = AddeTleChooser.URL_NAME_KEY;
                String urlStr = (String)(properties.get(key));
                URL url = new URL(urlStr);
                URLConnection urlCon = url.openConnection();
                InputStreamReader isr = new InputStreamReader(urlCon.getInputStream());
                BufferedReader tleReader = new BufferedReader(isr);
                String nextLine = null;
                int tleCount = 0;
                while ((nextLine = tleReader.readLine()) != null) {
                    if (nextLine.length() < 50) {
                        choices.add(nextLine);
                    }
                }
            } catch (Exception e) {
                System.out.println("\ne=" + e + "\n");
            }
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

/* code example in 
   /home/gad/src/JSatTrak/JSatTrak-4.1-src/JSatTrak/src/name/gano/astro/propogators/sgp4_cssi/SGP4utils.java
*/

    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {

        System.out.println("\ngetDataInner:");
        System.out.println("    dataChoice=" + dataChoice);
        System.out.println("    category=" + category);
        System.out.println("    dataSelection=" + dataSelection + "\n");

        final double deg2rad = pi / 180.0;         //   0.0174532925199433
        final double xpdotp = 1440.0 / (2.0 * pi);  // 229.1831180523293

        double sec, tumin;
        int year = 0;
        int mon, day, hr, minute;//, nexp, ibexp;

        boolean gotit = false;
        int index = -1;
        String choiceName = dataChoice.getName();
        List tleComps = new ArrayList();
        while(!gotit) {
            index++;
            String name = ((String)tleCards.get(index)).trim();
            if (name.equals(choiceName)) {
                data.name = name; 
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
                gotit= true;
            }
            if (index+3 > tleCards.size()) gotit = true;
        }
        if (gotit == false) return null;


        SGP4unit.Gravconsttype gravconsttype = SGP4unit.Gravconsttype.wgs72;
        double[] temp = SGP4unit.getgravconst(gravconsttype);
        tumin = temp[0];

        // ---- find no, ndot, nddot ----
        data.no = data.no / xpdotp; //* rad/min
        data.nddot = data.nddot * Math.pow(10.0, data.nexp);
        data.bstar = data.bstar * Math.pow(10.0, data.ibexp);

        // ---- convert to sgp4 units ----
        data.a = Math.pow(data.no * tumin, (-2.0 / 3.0));
        data.ndot = data.ndot / (xpdotp * 1440.0);  //* ? * minperday
        data.nddot = data.nddot / (xpdotp * 1440.0 * 1440);

        // ---- find standard orbital elements ----
        data.inclo = data.inclo * deg2rad;
        data.nodeo = data.nodeo * deg2rad;
        data.argpo = data.argpo * deg2rad;
        data.mo = data.mo * deg2rad;

        data.alta = data.a * (1.0 + data.ecco) - 1.0;
        data.altp = data.a * (1.0 - data.ecco) - 1.0;

        // ----------------------------------------------------------------
        // find sgp4epoch time of element set
        // remember that sgp4 uses units of days from 0 jan 1950 (sgp4epoch)
        // and minutes from the epoch (time)
        // ----------------------------------------------------------------

        // ---------------- temp fix for years from 1957-2056 -------------------
        // --------- correct fix will occur when year is 4-digit in tle ---------
        if(data.epochyr < 57)
        {
            year = data.epochyr + 2000;
        }
        else
        {
            year = data.epochyr + 1900;
        }

        SGP4SatData satData = new SGP4SatData();

        // computes the m/d/hr/min/sec from year and epoch days
        SGP4utils.MDHMS mdhms = SGP4utils.days2mdhms(year, data.epochdays);
        mon = mdhms.mon;
        day = mdhms.day;
        hr = mdhms.hr;
        minute = mdhms.minute;
        sec = mdhms.sec;
        System.out.println("\nyear=" + year + " day=" + data.epochdays);
        System.out.println("    mon=" + mon + " day=" + day);
        System.out.println("    hr=" + hr + " minute=" + minute + " sec=" + sec);
        // computes the jd from  m/d/...
        data.jdsatepoch = SGP4utils.jday(year, mon, day, hr, minute, sec);
        System.out.println("epochdays=" + data.epochdays);
        System.out.println("jdsatepoch=" + data.jdsatepoch);

        // ---------------- initialize the orbit at sgp4epoch -------------------
        char opsmode = SGP4utils.OPSMODE_IMPROVED; // OPSMODE_IMPROVED
        boolean result = SGP4unit.sgp4init(gravconsttype, opsmode, data.satnum,
                data.jdsatepoch - 2433281.5, data.bstar,
                data.ecco, data.argpo, data.inclo, data.mo, data.no,
                data.nodeo, data);
        System.out.println("\nsgp4init result=" + result + "\n");

        this.selectionProps = dataSelection.getProperties();
        Enumeration propEnum = this.selectionProps.keys();
        for (int i = 0; propEnum.hasMoreElements(); i++) {
            String key = propEnum.nextElement().toString();
            String val = (String)this.selectionProps.get(key);
            System.out.println("key=" + key + " val=" + val);
        }

        return null;
    }

    private int decodeCard1(String card) {
/*
        System.out.println("\ndecodeCard1:");
        System.out.println("    card=" + card);
        System.out.println("    length=" + card.length());
*/
        int satId = 0;
        int launchYear = 0;
        int intCode = 0;
        int yyyy = 0;
        double ddd = 1.0;
        double firstDev = 1.0;
        double secondDev = 1.0;
        double bStar = 1.0;
        int ephemerisType = 0;
        int elementNumber = 0;

        int ret = 0;
        System.out.println(card);
        int ck1 = checksum(card.substring(0, 68));
        String str = card.substring(0, 1);
        if (str.equals("1")) {
            satId = getInt(2, 7, card);
            System.out.println("    satId = " + satId);
            data.satnum = satId;
            ++ret;

            data.classification = card.substring(7, 8);
            data.intldesg = card.substring(9, 17);
            int yy = getInt(18, 20, card);
            data.epochyr = yy;
            ++ret;

            ddd = getDouble(20, 32, card);
            System.out.println("    ddd = " + ddd);
            data.epochdays = ddd;
            ++ret;

            firstDev = getDouble(33, 43, card);
            System.out.println("    firstDev = " + firstDev);
            data.ndot = firstDev;
            ++ret;

            if((card.substring(44, 52)).equals("        "))
            {
                data.nddot = 0;
                data.nexp = 0;
            }
            else
            {
                data.nddot = getDouble(44, 50, card) / 1.0E5;
                data.nexp = getInt(50, 52, card);
            }
            System.out.println("    nddot=" + data.nddot);
            System.out.println("    nexp=" + data.nexp);

            data.bstar = getDouble(53, 59, card) / 1.0E5;
            data.ibexp = getInt(59, 61, card);
            System.out.println("    bstar=" + data.bstar);
            System.out.println("    ibexp=" + data.ibexp);

            try {
                ephemerisType = getInt(62, 63, card);
                System.out.println("    ephemerisType = " + ephemerisType);
                data.numb = ephemerisType;
                ++ret;

                elementNumber = getInt(64, 68, card);
                System.out.println("    elementNumber = " + elementNumber);
                data.elnum = elementNumber;
                ++ret;
            } catch (Exception e) {
                System.out.println("Warning: Error Reading numb or elnum from TLE line 1 sat#:" + data.satnum);
            }

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
        double inclination = 1.0;
        double rightAscension = 1.0;
        double eccentricity = 1.0;
        double argOfPerigee = 1.0;
        double meanAnomaly = 1.0;
        double meanMotion = 1.0;
        int revolutionNumber = 0;

        int ret = 0;
        System.out.println("\n" + card);
        int ck1 = checksum(card.substring(0, 68));
        String str = card.substring(0, 1);
        if (str.equals("2")) {
            int nsat = getInt(2, 7, card);
            System.out.println("    nsat = " + nsat + " data.satnum=" + data.satnum);
            if (nsat != data.satnum) {
                System.out.println("Warning TLE line 2 Sat Num doesn't match line1 for sat: " + data.name);
            } else {
                inclination = getDouble(8, 16, card);
                data.inclo = inclination;
                System.out.println("    inclo = " + data.inclo);
                ++ret;

                rightAscension = getDouble(17, 25, card);
                data.nodeo = rightAscension;
                System.out.println("    nodeo = " + data.nodeo);
                ++ret;

                eccentricity = getDouble(26, 33, card) / 1.0E7;
                data.ecco = eccentricity;
                System.out.println("    ecco = " + data.ecco);
                ++ret;

                argOfPerigee = getDouble(34, 42, card);
                data.argpo = argOfPerigee;
                System.out.println("    argpo = " + data.argpo);
                ++ret;

                meanAnomaly = getDouble(43, 51, card);
                data.mo = meanAnomaly;
                System.out.println("    mo = " + data.mo);
                ++ret;

                meanMotion = getDouble(52, 63, card);
                data.no = meanMotion;
                System.out.println("    no = " + data.no);
                ++ret;

                try {
                    revolutionNumber = getInt(63, 68, card);
                    data.revnum = revolutionNumber;
                    System.out.println("    revnum = " + data.revnum);
                    ++ret;
                } catch (Exception e) {
                    System.out.println("Warning: Error Reading revnum from TLE line 2 sat#:" + data.satnum + "\n" + e.toString());
                    data.revnum = -1;
                }

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

    protected void initDataSelectionComponents(
                   List<DataSelectionComponent> components, final DataChoice dataChoice) {

        IntegratedDataViewer idv = getDataContext().getIdv();
        idv.showWaitCursor();
        try {
            TimeSelection timeSelection = new TimeSelection(this);
            components.add(timeSelection);
        } catch (Exception e) {
            System.out.println("problem creating TimeSelection e=" + e);
        }
        idv.showNormalCursor();
    }
}
