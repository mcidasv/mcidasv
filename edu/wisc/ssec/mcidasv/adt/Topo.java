/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
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

package edu.wisc.ssec.mcidasv.adt;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Topo {

    public Topo() {
    }

    public static int ReadTopoFile(String topofile, double inputlat, double inputlon)
            throws IOException {

        int num_lon_elev = 5760;
        int num_lat_elev = 2880;
        double first_lon_elev = 0.0;
        double first_lat_elev = 90.0;
        double last_lon_elev = 360.0;
        double last_lat_elev = -90.0;

        double ax = inputlat;
        double bx = inputlon; /* to make mcidas compliant */
        System.out.printf("TOPO: lat: %f  lon: %f\n", ax, bx);

        InputStream filestream = Topo.class.getResourceAsStream(topofile);
        DataInputStream dis = new DataInputStream(filestream);

        double del_lon_elev = (last_lon_elev - first_lon_elev) / num_lon_elev;
        double del_lat_elev = (first_lat_elev - last_lat_elev) / num_lat_elev;
        System.out.printf("TOPO: dlat: %f  dlon: %f\n", del_lon_elev, del_lat_elev);

        int ay = (int) ((90.0 - ax) / del_lat_elev);
        if (bx < 0.0)
            bx = bx + 360.0;
        int by = (int) (bx / del_lon_elev);
        System.out.printf("TOPO: lat: %d  lon: %d \n", ay, by);
        long position = (long) (2 * ((ay * ((double) num_lon_elev)) + by));
        System.out.printf("TOPO: position=%d\n", position);
        // filestream.seek(position+1);
        dis.skip(position);
        System.err.println("After skip, about to read val...");

        int i = dis.readShort();
        System.err.println("After read, val: " + i);

        int ichar = (i == 0) ? 2 : 1;
        System.err.println("After read, returning: " + ichar);
        System.out.printf("TOPO: position=%d Value=%d landflag=%d \n ", position, i, ichar);
        filestream.close();

        return ichar;
    }

}
