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

import edu.wisc.ssec.mcidas.AREAnav;
import edu.wisc.ssec.mcidas.McIDASUtil;

import java.io.*;
import java.util.Date;

/**
 * Class FrameDirectory holds information obtained
 * from frame directory files, FRAMEn.p, from McIdas-X
 */
public class FrameDirectory {

    /** time of data in frame */
    private Date nominalTime;

    /** Sensor source name */
    private String sensorName;

    /** Sensor source number */
    private int sensorNumber;

    /** Year and Julian day, ccyyddd */
    private int cyd;

    /** Time, hhmmss */
    private int hms;

    /** Band number */
    private int band;

    /** Upper-left corner satellite coordinates */
    private int uLLine;
    private int uLEle;

    /** Magnification factors */
    private int lineMag;
    private int eleMag;

    /** Resolution factors */
    private int lineRes;
    private int eleRes;

    /** Navigation block */
    private int[] nav;

    /** Navigation block */
    private int[] aux;
    
    /** GRAF navigation type */
    private int AREAnavGRAF = 1196572998;
    
    /**
     * Constructor
     */
    public FrameDirectory() { }
    
    /**
     * Copy constructor
     *
     * @param that The FrameDirectory to copy
     *
     */
    public FrameDirectory(FrameDirectory that) {
        this.sensorName = that.sensorName;
        this.sensorNumber = that.sensorNumber;
        this.cyd = that.cyd;
        this.hms = that.hms;
        this.nominalTime = new Date(1000*McIDASUtil.mcDayTimeToSecs(that.cyd,that.hms));
        this.band = that.band;
        this.uLLine = that.uLLine;
        this.uLEle = that.uLEle;
        this.lineMag = that.lineMag;
        this.eleMag = that.eleMag;
        this.lineRes = that.lineRes;
        this.eleRes = that.eleRes;
        this.nav = that.nav;
        this.aux = that.aux;
    }

    /**
     * Constructor
     *
     * @param directory frame directory from McIdax-X
     *
     */
    public FrameDirectory(int[] directory) {
        //System.out.println("FrameDirectory constructor:");
        this.sensorNumber = directory[0];
//        if (this.sensorNumber != -1)
//          this.sensorName = getName(directory[0]);
//        else
//          this.sensorName = "";
        this.sensorName = "";
        this.cyd = directory[1];
        this.hms = directory[2];
        this.nominalTime = new Date(1000*McIDASUtil.mcDayTimeToSecs(cyd,hms));
        this.band = directory[3];
        this.uLLine = directory[4];
        this.uLEle = directory[5];
        this.lineRes = directory[10];
        this.eleRes = directory[11];
        this.lineMag = directory[19];
        this.eleMag = directory[20];

//        if (this.lineMag < 0) this.lineMag = 1;
//        if (this.eleMag < 0) this.eleMag = 1;     
//        this.lineMag=1;
//        this.eleMag=1;
        
/*
        System.out.println("  cyd=" + cyd);
        System.out.println("  hms=" + hms);
        System.out.println("  band=" + band);
        System.out.println("  uLLine=" + uLLine);
        System.out.println("  uLEle=" + uLEle);
        System.out.println("  lineMag=" + lineMag);
        System.out.println("  eleMag=" + eleMag);
        System.out.println("  lineRes=" + lineRes);
        System.out.println("  eleRes=" + eleRes);
*/
//        System.out.println("Navigation type " + directory[64] + ": " + navIntToString(directory[64]));
               
        int navLength;
        if (directory[64] == AREAnav.LALO) navLength = 128;
        else navLength = 640;
        this.nav = new int[navLength];
        System.arraycopy(directory, 64, this.nav, 0, navLength);
        
        if (this.nav[0] == this.AREAnavGRAF)
        	this.nav = transformGRAFIntoRECT(this.nav);
        
        int auxLength = 0;
        int rows = 0;
        int cols = 0;
        int begLat = 0;
        int begLon = 0;
        if (this.nav[0] == AREAnav.LALO) {
          rows = this.nav[65];
          cols = this.nav[66];
          begLat = this.nav[78]/4;
          begLon = this.nav[79]/4;
          auxLength = begLon + (rows*cols);
          this.aux = new int[auxLength];
        }
        else {
          this.aux = new int[1];
        }
        int numPoints = rows * cols;
        System.arraycopy(directory, 64+navLength, this.aux, begLat, numPoints);
        if (auxLength > 0) {
        	System.arraycopy(directory, 64+navLength+numPoints, this.aux, begLon, numPoints);
        }
//        System.out.println("FrameDirectory navLength: " + navLength + ", auxLength: " + auxLength); 
    }

    /* 
     * TODO: FrameDirectory.getName() is not used right now... keep the code here just in case.
     * If you do decide you need it, read SATANNOT over the bridge, not locally...
     */
/*
     public String getName(int num) {
       String name = "";
       FileInputStream fis;
       //byte[] bline={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
       byte[] bline = new byte[31];
       int off=0;
       int ret = 0;
       int sensor=0;

       System.out.println("Fix this: SATANNOT should not be read locally");
       try {
         fis  = new FileInputStream("/home/mcidas/data/SATANNOT");
       } catch(Exception e) {
           System.out.println("FrameDirectory: Can't find SATANNOT");
           return name;
       }
       int counter=0;
       int sensor1=0;
       int sensor2=0;
       int sensor3=0;
       while (ret != -1) {
         try {
           int ptr=0;
	   int next=0;
           for (int i=0;i<19; i++) { 
             next = fis.read();
             bline[ptr] = (byte)next;
             ptr++;
           }
           name = new String(bline, 0, ptr);
           for (int i=20;i<30; i++) {
             off = fis.read();
           }
           sensor1 = fis.read()-48;
           sensor2 = fis.read()-48;
           sensor3 = fis.read()-48;
           sensor = 0;
           if (sensor1 >= 0) {
             sensor = sensor1;
           }
           if (sensor2 >= 0) {
             sensor *= 10;
             sensor += sensor2;
           }
           if (sensor3 >= 0) {
             sensor *= 10;
             sensor += sensor3;
           }
           for (int i=32; i<80; i++)
             off = fis.read();
         } catch(Exception e) {
           System.out.println("FrameDirectory: Can't read SATANNOT");
           try {
               fis.close();
           } catch (Exception ee) {
           }
           return name;
         }
         if (sensor == num) ret =-1;
         counter++;
         if (counter>200) ret=-1;
       }
       try {
           fis.close();
       } catch (Exception e) {
       }
       return name;
     }
*/

    /**
     * Get the nominalTime.
     *
     * @return The nominalTime.
     */
    public Date getNominalTime() {
        return this.nominalTime;
    }

    /**
     * Get the sensorName.
     *
     * @return The sensorName.
     */
    public String getSensorName() {
        return this.sensorName;
    }

    /**
     * Get the sensorNumber.
     *
     * @return The sensorNumber.
     */
    public int getSensorNumber() {
        return this.sensorNumber;
    }

    /**
     * Get cyd.
     *
     * @return cyd.
     */
    public int getCyd() {
        return this.cyd;
    }

    /**
     * Get hms.
     *
     * @return hms.
     */
    public int getHms() {
        return this.hms;
    }

    /**
     * Get band.
     *
     * @return band.
     */
    public int getBand() {
        return this.band;
    }

    /**
     * Set sensorName.
     *
     * @param newName The new vaue for sensorName.
     */
    public void setSensorName(String newName) {
        this.sensorName = newName;
    }

    /**
     * Set cyd.
     *
     * @param newCyd The new vaue for cyd.
     */
    public void setCyd(int newCyd) {
        this.cyd = newCyd;
    }

    /**
     * Set hms.
     *
     * @param newHms The new vaue for hms.
     */
    public void setHms(int newHms) {
        this.hms = newHms;
    }

    /**
     * Set band.
     *
     * @param newBand The new vaue for band.
     */
    public void setBand(int newBand) {
        this.band = newBand;
    }

    /**
     * Get a String representation of this object
     * @return a string representation
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(this.sensorName + " ");
        buf.append(this.sensorNumber + " ");
        buf.append(this.cyd + " ");
        buf.append(this.hms + " ");
        buf.append(this.band);
        return buf.toString();
    }

    public int[] getFrameNav() {
        return nav;
    }

    public int[] getFrameAux() {
        return aux;
    }

    public int getLineRes() {
        return lineRes;
    }

    public int getEleRes() {
        return eleRes;
    }

    public int getULLine() {
        return uLLine;
    }

    public int getULEle() {
        return uLEle;
    }
    
    /**
     * Print the nav type
     */
    private String navIntToString(int navInt) {
        int int1 = navInt/0x1000000&0xff;
        int int2 = navInt/0x10000&0xff;
        int int3 = navInt/0x100&0xff;
        int int4 = navInt&0xff;
        String char1 = new Character((char)int1).toString();
        String char2 = new Character((char)int2).toString();
        String char3 = new Character((char)int3).toString();
        String char4 = new Character((char)int4).toString();
        String returnString = char1 + char2 + char3 + char4;
        return returnString;
    }
    
    /**
     * Since GRAF is not a real data projection, try to munge it into RECT for VisAD
     */
    private int[] transformGRAFIntoRECT(int[] nav) {
    	if (nav[0] != this.AREAnavGRAF) return nav;
    	int[] RECT = nav;
    	int minLat = RECT[21];
    	int maxLat = RECT[22];
    	int minLon = RECT[23];
    	int maxLon = RECT[24];
    	int minY = RECT[25];
    	int maxY = RECT[26];
    	int minX = RECT[27];
    	int maxX = RECT[28];
    	int centerLat = Math.round((maxLat - minLat) / 2) + minLat;
    	int centerLon = Math.round((maxLon - minLon) / 2) + minLon;
    	int rangeLat = maxLat - minLat;
    	int rangeLon = maxLon - minLon;
    	int centerY = Math.round((maxY - minY) / 2) + minY;
    	int centerX = Math.round((maxX - minX) / 2) + minX;
    	int rangeY = maxY - minY;
    	int rangeX = maxX - minX;
    	RECT[0] = AREAnav.RECT;
    	RECT[1] = centerY - minY;
    	RECT[2] = centerLat;
    	RECT[3] = centerX - minX;
    	RECT[4] = centerLon;
    	RECT[5] = Math.round(rangeLat / rangeY);
    	RECT[6] = Math.round(rangeLon / rangeX);
    	// Earth constants (eccentricity and radius)
    	RECT[7] = 6378388;
    	RECT[8] = 81992;
    	for (int i=9; i<24; i++) RECT[i] = 0;
    	return RECT;
    }
}
