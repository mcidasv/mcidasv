/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2012
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

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class GroundStations
{
    private static final String card00 = "KMSN,SSEC,43.1398578,-89.3375136,270.4";
    private int gsCount = 0; // count of stations loaded
    public static String groundStationDB = "data/groundstations/groundstations_db.csv";

    private List stations = new ArrayList();
    private List latitudes = new ArrayList();
    private List longitudes = new ArrayList();
    private List altitudes = new ArrayList();

    public GroundStations(String topCard)
    {
        // read data files for Ground Stations
        try
        {
            BufferedReader gsReader = null; // initalization of reader 
            
            //see if local file exists, if not stream from web
            
            // read local file
            if( new File(groundStationDB).exists())
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
               stations.add(" ");
               latitudes.add(" ");
               longitudes.add(" ");
               altitudes.add(" ");
            }
            
            while (nextLine != null)
            {
                // split line into parts
                String[] elements = nextLine.split(",");
                
                if (elements.length == 5) // if the row is formatted correctly
                {
                    String network = elements[0];
                    String stationName = elements[1];
                    stations.add(stationName);
                    String stationLat = elements[2];
                    latitudes.add(stationLat);
                    String stationLon = elements[3];
                    longitudes.add(stationLon);
                    String stationAlt = elements[4];
                    altitudes.add(stationAlt);

//                    System.out.println("" + gsCount + " : " + stationName + ", " + stationLat+ ", " + stationLon + ", " + stationAlt);
                    gsCount++;
                }
                nextLine = gsReader.readLine();
            }// while there are more lines to read
            gsReader.close();
        }
        catch (Exception e)
        {
             System.out.println("ERROR IN GROUND STATION READING POSSIBLE FILE FORMAT OR MISSING FILES:");
        }
    } // constructor

    public int getGroundStationCount() {
        return gsCount;
    }

    public List getStations() {
        return stations;
    }

    public List getLatitudes() {
        return latitudes;
    }

    public List getLongitudes() {
        return longitudes;
    }

    public List getAltitudes() {
        return altitudes;
    }
}
