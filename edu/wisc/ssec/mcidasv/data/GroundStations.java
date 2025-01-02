/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.mcidasv.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import visad.VisADException;
import visad.georef.EarthLocationTuple;

public class GroundStations {
    private static final Logger logger =
        LoggerFactory.getLogger(GroundStations.class);
    
    private static final String card00 = "KMSN,SSEC,43.1398578,-89.3375136,270.4";

    // taken from http://www.gano.name/shawn/JSatTrak/data/groundstations/
    public static String groundStationDB = "/edu/wisc/ssec/mcidasv/resources/orbittrack_groundstations_db.csv";
    
    // shouldn't expose the List implementation to the public, but changing it
    // to List<...> namedLocs = new ArrayList<>() will break existing bundles :(
    private ArrayList<GroundStation> namedLocs = new ArrayList<>();

    /**
     * No-arg constructor for empty list which gets populated on-the-fly later.
     */
    
    public GroundStations() {
    }
    
    public GroundStations(String topCard) {
        // read data files for Ground Stations
        URL url = GroundStations.class.getResource(groundStationDB);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String nextLine = topCard;
            if (topCard == null) {
                nextLine = card00;
            }
            
            while (nextLine != null) {
                // split line into parts
                String[] elements = nextLine.split(",");
        
                if (elements.length == 5) // if the row is formatted correctly
                {
                    double dLat = Double.parseDouble(elements[2]);
                    double dLon = Double.parseDouble(elements[3]);
                    double dAlt = Double.parseDouble(elements[4]);
                    
                    EarthLocationTuple elt = new EarthLocationTuple(dLat, dLon, dAlt);
                    namedLocs.add(new GroundStation(elements[1], elt));
                }
                nextLine = in.readLine();
            } // while there are more lines to read
        } catch (IOException e) {
            logger.error("Problem reading ground stations, missing file or invalid file format", e);
        } catch (VisADException e) {
            logger.error("Problem creating earth location", e);
        }
    }
    
    public ArrayList<GroundStation> getGroundStations() {
        return namedLocs;
    }

}
