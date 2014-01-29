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
import java.io.FileReader;
import java.io.InputStreamReader;

import java.net.URL;
import java.net.URLConnection;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import visad.georef.EarthLocationTuple;

public class GroundStations
{
	private static final Logger logger = LoggerFactory.getLogger(GroundStations.class);
	private static final String card00 = "KMSN,SSEC,43.1398578,-89.3375136,270.4";
    public static String groundStationDB = "data/groundstations/groundstations_db.csv";
    private HashMap<String, EarthLocationTuple> namedLocs = new HashMap<String, EarthLocationTuple>();

    /**
	 * No-arg constructor for empty list which gets populated on-the-fly later.
	 */
    
	public GroundStations() {
	}

	public GroundStations(String topCard)
    {
        // read data files for Ground Stations
        try {
            BufferedReader gsReader = null; // initialization of reader 
            
            //see if local file exists, if not stream from web
            
            // read local file
            if (new File(groundStationDB).exists())
            {
                File gsFile = new File(groundStationDB);
                FileReader gsFileReader = new FileReader(gsFile);
                gsReader = new BufferedReader(gsFileReader); // from local file
            }
            else
            {
                // read from web
                URL url = new URL("http://www.gano.name/shawn/JSatTrak/" + groundStationDB);
                URLConnection c = url.openConnection();
                InputStreamReader isr = new InputStreamReader(c.getInputStream());
                gsReader = new BufferedReader(isr); // from the web
            }

            String nextLine = topCard;
            if (topCard == null) {
               nextLine = card00;
            }
            
            while (nextLine != null)
            {
                // split line into parts
                String[] elements = nextLine.split(",");
                
                if (elements.length == 5) // if the row is formatted correctly
                {
                    Double dLat = new Double(elements[2]);
                    Double dLon = new Double(elements[3]);
                    Double dAlt = new Double(elements[4]);
                    
                    EarthLocationTuple elt = new EarthLocationTuple(dLat, dLon, dAlt);
                    namedLocs.put(elements[1], elt);
                }
                nextLine = gsReader.readLine();
            } // while there are more lines to read

            gsReader.close();
        }
        catch (Exception e)
        {
        	e.printStackTrace();
            logger.error("ERROR: Problem reading ground stations, missing file or invalid file format");
        }
    } // constructor
    
    public HashMap getGroundStations() {
    	return namedLocs;
    }

}
