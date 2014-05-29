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

package edu.wisc.ssec.mcidasv.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import jsattrak.objects.SatelliteTleSGP4;
import jsattrak.utilities.TLE;
import name.gano.astro.propogators.sgp4_cssi.SGP4SatData;
import name.gano.astro.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.StringUtil;

import visad.Data;
import visad.Text;
import visad.Tuple;
import visad.VisADException;
import visad.georef.LatLonTuple;

import edu.wisc.ssec.mcidas.adde.AddeTextReader;
import edu.wisc.ssec.mcidasv.chooser.PolarOrbitTrackChooser;
import edu.wisc.ssec.mcidasv.util.XmlUtil;

/**
 * Class for Two-Line-Element data sources, to plot orbit tracks
 * on McIDAS-V display window.
 *
 * @author Gail Dengel and Tommy Jasmin
 * @version $Revision$
 */

public class PolarOrbitTrackDataSource extends DataSourceImpl {

    private static final Logger logger = LoggerFactory.getLogger(PolarOrbitTrackDataSource.class);

    private ArrayList<String> tleCards = new ArrayList<String>();
    private ArrayList<String> choices = new ArrayList<String>();

    private SGP4SatData data = new SGP4SatData();
    private TLE tle;

    private Hashtable selectionProps;

    /** time step between data points */
    private int dTime = 1;

    private SatelliteTleSGP4 prop = null;
    private double julDate0 = 0.0;
    private double julDate1 = 0.0;
    
    TimeRangeSelection trs = null;

    /**
     * Default bean constructor for persistence; does nothing.
     */
    public PolarOrbitTrackDataSource() {}

    /**
     * Create a new PolarOrbitTrackDataSource
     *
     * @param descriptor    descriptor for this source
     * @param filename      ADDE URL
     * @param properties    extra properties for this source
     *
     */
    public PolarOrbitTrackDataSource(DataSourceDescriptor descriptor,
                              String filename, Hashtable properties)
           throws VisADException {
        super(descriptor, filename, null, properties);

        tleCards = new ArrayList<String>();
        choices = new ArrayList<String>();
        
        // we dealing with a local file?
        if (properties.containsKey(PolarOrbitTrackChooser.LOCAL_FILE_KEY)) {
            File f = (File) (properties.get(PolarOrbitTrackChooser.LOCAL_FILE_KEY));
            logger.debug("Local file: " + f.getName());
            URI uri = f.toURI();
            properties.put(PolarOrbitTrackChooser.URL_NAME_KEY, uri.toString());
        }
        
        // not a file, must be URL or ADDE request
        String key = PolarOrbitTrackChooser.TLE_SERVER_NAME_KEY;
        if (properties.containsKey(key)) {
        	logger.debug("ADDE request...");
            Object server = properties.get(key);
            key = PolarOrbitTrackChooser.TLE_GROUP_NAME_KEY;
            Object group = properties.get(key);
            key = PolarOrbitTrackChooser.TLE_USER_ID_KEY;
            Object user = properties.get(key);
            key = PolarOrbitTrackChooser.TLE_PROJECT_NUMBER_KEY;
            Object proj = properties.get(key);
            key = PolarOrbitTrackChooser.DATASET_NAME_KEY;
            Object descr = properties.get(key);
            String url = "adde://" + server + "/textdata?&PORT=112&COMPRESS=gzip&USER=" + user + "&PROJ=" + proj + "&GROUP=" + group + "&DESCR=" + descr;
            AddeTextReader reader = new AddeTextReader(url);
            List lines = null;
            if ("OK".equals(reader.getStatus())) {
                lines = reader.getLinesOfText();
            }
            if (lines == null) {
                notTLE();
                return;
            } else {
                String[] cards = StringUtil.listToStringArray(lines);
                for (int i = 0; i < cards.length; i++) {
                    String str = cards[i];
                    if (str.length() > 0) {
                        tleCards.add(XmlUtil.stripNonValidXMLCharacters(cards[i]));
                        int indx = cards[i].indexOf(" ");
                        if (indx < 0) {
                            choices.add(XmlUtil.stripNonValidXMLCharacters(cards[i]));
                        }
                    }
                }
            }
        } else {
            try {
                key = PolarOrbitTrackChooser.URL_NAME_KEY;
                String urlStr = (String)(properties.get(key));
                logger.debug("URL request: " + urlStr);
                URL url = new URL(urlStr);
                URLConnection urlCon = url.openConnection();
                InputStreamReader isr = new InputStreamReader(urlCon.getInputStream());
                BufferedReader tleReader = new BufferedReader(isr);
                String nextLine = null;
                while ((nextLine = tleReader.readLine()) != null) {
                    if (nextLine.length() > 0) {
                        tleCards.add(XmlUtil.stripNonValidXMLCharacters(nextLine));
                        if (nextLine.length() < 50) {
                            choices.add(XmlUtil.stripNonValidXMLCharacters(nextLine));
                        }
                    }
                }
            } catch (Exception e) {
                notTLE();
                return;
            }
        }
        checkFirstEntry();
    }

    private void checkFirstEntry() {
        if (tleCards.isEmpty()) {
            notTLE();
            return;
        }
        String card = (String) tleCards.get(1);
        decodeCard1(card);
    }

    private int checksum(String str) {
        int sum = 0;
        byte[] bites = str.getBytes();
        for (int i = 0; i < bites.length; i++) {
            int val = (int) bites[i];
            if ((val > 47) && (val < 58)) {
                sum += val - 48;
            } else if (val == 45) {
                ++sum;
            }
        }
        return sum % 10;
    }

    private int decodeCard1(String card) {

    	logger.debug("Decoding card: " + card);
        int satId = 0;
        double ddd = 1.0;
        double firstDev = 1.0;
        int ephemerisType = 0;
        int elementNumber = 0;

        int ret = 0;
        if (card.length() < 69) {
            notTLE();
            return -1;
        }
        int ck1 = checksum(card.substring(0, 68));
        String str = card.substring(0, 1);
        if (str.equals("1")) {
            satId = getInt(2, 7, card);
            //System.out.println("    satId = " + satId);
            data.satnum = satId;
            ++ret;

            data.classification = card.substring(7, 8);
            data.intldesg = card.substring(9, 17);
            int yy = getInt(18, 20, card);
            data.epochyr = yy;
            ++ret;

            ddd = getDouble(20, 32, card);
            //System.out.println("    ddd = " + ddd);
            data.epochdays = ddd;
            ++ret;

            firstDev = getDouble(33, 43, card);
            //System.out.println("    firstDev = " + firstDev);
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
            //System.out.println("    nddot=" + data.nddot);
            //System.out.println("    nexp=" + data.nexp);

            data.bstar = getDouble(53, 59, card) / 1.0E5;
            data.ibexp = getInt(59, 61, card);
            //System.out.println("    bstar=" + data.bstar);
            //System.out.println("    ibexp=" + data.ibexp);

            try {
                ephemerisType = getInt(62, 63, card);
                //System.out.println("    ephemerisType = " + ephemerisType);
                data.numb = ephemerisType;
                ++ret;

                elementNumber = getInt(64, 68, card);
                //System.out.println("    elementNumber = " + elementNumber);
                data.elnum = elementNumber;
                ++ret;
            } catch (Exception e) {
                logger.error("Warning: Error Reading numb or elnum from TLE line 1 sat#:" + data.satnum);
            }

            int check = card.codePointAt(68) - 48;
            if (check != ck1) {
                notTLE();
//                logger.error("***** Failed checksum *****");
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
        //System.out.println("\n" + card);
        if (card.length() < 69) {
            notTLE();
            return -1;
        }
        int ck1 = checksum(card.substring(0, 68));
        String str = card.substring(0, 1);
        if (str.equals("2")) {
            int nsat = getInt(2, 7, card);
            //System.out.println("    nsat = " + nsat + " data.satnum=" + data.satnum);
            if (nsat != data.satnum) {
                logger.error("Warning TLE line 2 Sat Num doesn't match line1 for sat: " + data.name);
            } else {
                inclination = getDouble(8, 16, card);
                data.inclo = inclination;
                //System.out.println("    inclo = " + data.inclo);
                ++ret;

                rightAscension = getDouble(17, 25, card);
                data.nodeo = rightAscension;
                //System.out.println("    nodeo = " + data.nodeo);
                ++ret;

                eccentricity = getDouble(26, 33, card) / 1.0E7;
                data.ecco = eccentricity;
                //System.out.println("    ecco = " + data.ecco);
                ++ret;

                argOfPerigee = getDouble(34, 42, card);
                data.argpo = argOfPerigee;
                //System.out.println("    argpo = " + data.argpo);
                ++ret;

                meanAnomaly = getDouble(43, 51, card);
                data.mo = meanAnomaly;
                //System.out.println("    mo = " + data.mo);
                ++ret;

                meanMotion = getDouble(52, 63, card);
                data.no = meanMotion;
                //System.out.println("    no = " + data.no);
                ++ret;

                try {
                    revolutionNumber = getInt(63, 68, card);
                    data.revnum = revolutionNumber;
                    //System.out.println("    revnum = " + data.revnum);
                    ++ret;
                } catch (Exception e) {
                    logger.error("Warning: Error Reading revnum from TLE line 2 sat#:" + data.satnum + "\n" + e.toString());
                    data.revnum = -1;
                }

                int check = card.codePointAt(68) - 48;
                if (check != ck1) {
                    notTLE();
//                    logger.error("***** Failed checksum *****");
                    ret = -1;
                }
            }
        }
        return ret;
    }

    /**
     * Make the data choices associated with this source.
     */
    protected void doMakeDataChoices() {
        String category = "TLE";
        for (int i = 0; i < choices.size(); i++) {
            String name  = ((String) choices.get(i)).trim();
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

        boolean gotit = false;
        int index = -1;
        String choiceName = dataChoice.getName();
        String tleLine1 = "";
        String tleLine2 = "";

        while (!gotit) {
            index++;
            String name = ((String) tleCards.get(index)).trim();
            if (name.equals(choiceName)) {
                data.name = name; 
/*
                System.out.println("\n" + tleCards.get(index));
                System.out.println(tleCards.get(index+1));
                System.out.println(tleCards.get(index+2) + "\n");
*/
                index++;
                String card = (String) tleCards.get(index);
                tleLine1 = card;
                int ncomps = decodeCard1(card);
                if (ncomps < 0) return null;
                index++;
                card = (String) tleCards.get(index);
                tleLine2 = card;
                ncomps += decodeCard2(card);
                gotit= true;
            }
            if (index+3 > tleCards.size()) gotit = true;
        }
        if (gotit == false) return null;

        this.selectionProps = dataSelection.getProperties();
/*
        Enumeration propEnum = this.selectionProps.keys();
        for (int i = 0; propEnum.hasMoreElements(); i++) {
            String key = propEnum.nextElement().toString();
            String val = (String)this.selectionProps.get(key);
            System.out.println("key=" + key + " val=" + val);
        }
*/
        tle = new TLE(choiceName, tleLine1, tleLine2);

        String endStr = (String) this.selectionProps.get("ETime");
        Double dEnd = new Double(endStr);
        double endJulianDate = dEnd.doubleValue();
        julDate1 = endJulianDate;

		try {
			prop = new SatelliteTleSGP4(tle.getSatName(), tle.getLine1(),
					tle.getLine2());
			prop.setShowGroundTrack(false);
		} catch (Exception e) {
			logger.error("Error Creating SGP4 Satellite");
			e.printStackTrace();
			System.exit(1);
		}

        Time time = new Time(
                        (new Integer((String)this.selectionProps.get("Year"))).intValue(),
                        (new Integer((String)this.selectionProps.get("Month"))).intValue(),
                        (new Integer((String)this.selectionProps.get("Day"))).intValue(),
                        (new Integer((String)this.selectionProps.get("Hours"))).intValue(),
                        (new Integer((String)this.selectionProps.get("Mins"))).intValue(),
                        (new Double((String)this.selectionProps.get("Secs"))).doubleValue());
        double julianDate = time.getJulianDate();
        julDate0 = julianDate;
        Vector v = new Vector();

        while (julianDate <= julDate1) {
            // prop to the desired time
            prop.propogate2JulDate(julianDate);

            // get the lat/long/altitude [radians, radians, meters]
            double[] lla = prop.getLLA();
            double lat = lla[0] * 180.0 / Math.PI;
            double lon = lla[1] * 180.0 / Math.PI;

/*
             System.out.println(time.getDateTimeStr() + " Lat: " + lat
                                                      + " Lon: " + lon
                                                      + " Alt: " + alt);
 */
            Tuple data = new Tuple(new Data[] { new Text(time.getDateTimeStr()),
                                                new LatLonTuple(
                                                    lat,
                                                    lon
                                                )}
                         );
            v.add(data);
            time.add(Time.MINUTE, dTime);
            julianDate = time.getJulianDate();
        }

        return new Tuple((Data[]) v.toArray(new Data[v.size()]), false);
    }

    private double getDouble(int beg, int end, String card) {
        String str = card.substring(beg, end);
        str = str.trim();
        return (new Double(str)).doubleValue();
    }

    public int getDTime() {
        return dTime;
    }

    private int getInt(int beg, int end,  String card) {
        String str = card.substring(beg, end);
        str = str.trim();
        // TODO(jon): remove check and parseInteger upon switch to java 7+
        String javaVersion = System.getProperty("java.version");
        int tmp;
        if (javaVersion.startsWith("1.6")) {
            tmp = parseInteger(str);
        } else {
            tmp = Integer.valueOf(str);
        }
        return tmp;
    }

    /**
     * choices needs to persist to support bundles
     * @return the choices
     */

    public ArrayList<String> getChoices() {
        return choices;
    }

    /**
     * choices needs to persist to support bundles
     * @param choices the choices to set
     */

    public void setChoices(ArrayList<String> choices) {
        this.choices = choices;
    }

    /**
     * tleCards needs to persist to support bundles
     * @return the tleCards
     */

    public ArrayList<String> getTleCards() {
        return tleCards;
    }

    /**
     * tleCards needs to persist to support bundles
     * @param tleCards the tleCards to set
     */

    public void setTleCards(ArrayList<String> tleCards) {
        this.tleCards = tleCards;
    }

    /**
     * @return the trs
     */
    public TimeRangeSelection getTrs() {
        return trs;
    }

    public double getNearestAltToGroundStation(double gsLat, double gsLon) {
        double retAlt = 0.0;
        Time time = new Time(
            (new Integer((String)this.selectionProps.get("Year"))).intValue(),
            (new Integer((String)this.selectionProps.get("Month"))).intValue(),
            (new Integer((String)this.selectionProps.get("Day"))).intValue(),
            (new Integer((String)this.selectionProps.get("Hours"))).intValue(),
            (new Integer((String)this.selectionProps.get("Mins"))).intValue(),
            (new Double((String)this.selectionProps.get("Secs"))).doubleValue());

        double minDist = 999999.99;
        double julianDate = julDate0;

        while (julianDate <= julDate1) {
            // prop to the desired time
            prop.propogate2JulDate(julianDate);

            // get the lat/long/altitude [radians, radians, meters]
            double[] lla = prop.getLLA();
            double lat = lla[0] * 180.0 / Math.PI;
            double lon = lla[1] * 180.0 / Math.PI;
            double alt = lla[2];
            //System.out.println("    " + time.getDateTimeStr() + ": lat=" + lat + " lon=" + lon + " alt=" + alt);

            double latDiff = (gsLat - lat) * (gsLat - lat);
            double lonDiff = (gsLon - lon) * (gsLon - lon);
            double dist = Math.sqrt(latDiff+lonDiff);
            if (dist < minDist) {
                minDist = dist;
                retAlt = alt;
            }
            time.add(Time.MINUTE, dTime);
            julianDate = time.getJulianDate();
        }

        return retAlt;
    }

    public void initAfterCreation() {
    }

    protected void initDataSelectionComponents(
        List<DataSelectionComponent> components, final DataChoice dataChoice) {
/*
        System.out.println("\ninitDataSelectionComponents:");
        System.out.println("    components=" + components);
        System.out.println("    dataChoice=" + dataChoice);
        System.out.println("        categories=" + dataChoice.getCategories());
        System.out.println("        displayCategory=" + dataChoice.getDisplayCategory());
*/
        clearTimes();
        IntegratedDataViewer idv = getDataContext().getIdv();
        idv.showWaitCursor();
        try {
            trs = new TimeRangeSelection(this);
            components.add(trs);
        } catch (Exception e) {
            logger.error("problem creating TimeRangeSelection e=" + e);
        }
        idv.showNormalCursor();
    }

    private void notTLE() {
        tleCards = new ArrayList<String>();
        choices = new ArrayList<String>();
        setInError(true, "\nSource does not contain TLE data");
    }

    public void setDTime(int val) {
        dTime = val;
    }

    /**
     * Show the dialog
     *
     * @param initTabName What tab should we show. May be null.
     * @param modal Is dialog modal
     *
     * @return success
     */

    public boolean showPropertiesDialog(String initTabName, boolean modal) {
        //System.out.println("\n\nshowPropertiesDialog:");
        boolean ret = super.showPropertiesDialog(initTabName, modal);
        return ret;
    }

    // taken and slightly simplified version of GNU Classpath's Integer.parseInt:
    // http://cvs.savannah.gnu.org/viewvc/classpath/java/lang/Integer.java?root=classpath&view=markup
    // TODO(jon): remove upon switch to java 7+
    private static int parseInteger(String str) throws NumberFormatException {
        int radix = 10;
        if (str == null) {
            throw new NumberFormatException();
        }

        int len = str.length();

        if (len == 0) {
            throw new NumberFormatException("string length is null");
        }

        int index = 0;
        boolean isNeg = false;
        int ch = str.charAt(index);
        if (ch == '-') {
            if (len == 1) {
                throw new NumberFormatException("pure '-'");
            }
            isNeg = true;
            ch = str.charAt(++index);
        } else if (ch == '+') {
            if (len == 1) {
                throw new NumberFormatException("pure '+'");
            }
            ch = str.charAt(++index);
        }

        if (index == len) {
            throw new NumberFormatException("non terminated number: " + str);
        }

        int max = Integer.MAX_VALUE / radix;
        // We can't directly write `max = (MAX_VALUE + 1) / radix'.
        // So instead we fake it.
        if (isNeg && ((Integer.MAX_VALUE % radix) == (radix - 1))) {
            ++max;
        }

        int val = 0;
        while (index < len) {
            if ((val < 0) || (val > max)) {
                throw new NumberFormatException("number overflow (pos=" + index + ") : " + str);
            }
            ch = Character.digit(str.charAt(index++), radix);
            val = val * radix + ch;
            if ((ch < 0) || (((val < 0) && (!isNeg || (val != Integer.MIN_VALUE))))) {
                throw new NumberFormatException("invalid character at position " + index + " in " + str);
            }
        }
        return isNeg ? -val : val;
    }
}

