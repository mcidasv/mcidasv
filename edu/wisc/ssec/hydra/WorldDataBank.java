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

package edu.wisc.ssec.hydra;

import java.net.URL;
import java.io.*;
import java.util.StringTokenizer;


public class WorldDataBank extends SimpleBoundaryAdapter {

    BufferedReader reader = null;

    public WorldDataBank(URL url) throws Exception {
        super(url);
    }

    protected void openSource() throws IOException {
        Object obj = new Object();
        InputStream ios = url.openStream();
        reader = new BufferedReader(new InputStreamReader(ios));
    }

    protected void readHeader() throws EOFException, IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new EOFException();
        }
        StringTokenizer strTok = new StringTokenizer(line);
        String[] tokens = new String[strTok.countTokens()];
        int tokCnt = 0;
        while (strTok.hasMoreElements()) {
            tokens[tokCnt++] = strTok.nextToken();
        }

        numPtsPolygon = Integer.valueOf(tokens[5]);

        return;
    }

    protected void readPolygonPoints() throws EOFException, IOException {
        float[][] latlon = new float[2][numPtsPolygon];

        for (int t = 0; t < numPtsPolygon; t++) {
            String line = reader.readLine();
            StringTokenizer strTok = new StringTokenizer(line);
            String[] tokens = new String[strTok.countTokens()];
            int tokCnt = 0;
            while (strTok.hasMoreElements()) {
                tokens[tokCnt++] = strTok.nextToken();
            }
            latlon[0][t] = Float.valueOf(tokens[0]);
            latlon[1][t] = Float.valueOf(tokens[1]);
        }

        polygons.add(latlon);

        return;
    }

    protected boolean skip() {
        return false;
    }

    protected void cleanup() throws IOException {
        reader.close();
    }

}
