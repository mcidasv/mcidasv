package edu.wisc.ssec.mcidasv.data;

import edu.wisc.ssec.mcidas.AREAnav;
import edu.wisc.ssec.mcidas.McIDASUtil;

import java.awt.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Date;

/**
 * Class FrameDirectory holds information obtained
 * from frame directory files, FRAMEn.p, from McIdax-X
 */
public class FrameDirectory {

    private FrameSubs fsi;
    private String myRequest;

    /** time of data in frame */
    private Date nominalTime;

    /** number of frames */
    private int numberOfFrames;

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

    /**
     * Constructor
     *
     *
     */
    public FrameDirectory(String request) {
      fsi = new FrameSubs(request);
      myRequest = request;
      this.numberOfFrames = fsi.getNumberOfFrames();
      if (this.numberOfFrames < 0)
         System.out.println("FrameDirectory: Error in numberOfFrames");
    }



    /**
     * Copy constructor
     *
     * @param that The FrameDirectory to copy
     *
     */
    public FrameDirectory(FrameDirectory that) {
        this.sensorName       = that.sensorName;
        this.sensorNumber    = that.sensorNumber;
        this.cyd  = that.cyd;
        this.hms  = that.hms;
        this.nominalTime = new Date(1000*McIDASUtil.mcDayTimeToSecs(that.cyd,that.hms));
        this.band  = that.band;
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
        if (this.sensorNumber != -1)
          this.sensorName = getName(directory[0]);
        else
          this.sensorName = " ";
        this.cyd = directory[1];
        this.hms = directory[2];
        this.nominalTime = new Date(1000*McIDASUtil.mcDayTimeToSecs(cyd,hms));
        this.band = directory[3];
        this.uLLine = directory[4];
        this.uLEle = directory[5];
        this.lineMag = directory[19];
        this.eleMag = directory[20];
/*
        System.out.println("   cyd=" + cyd);
        System.out.println("   hms=" + hms);
        System.out.println("   band=" + band);
        System.out.println("   uLLine=" + uLLine);
        System.out.println("   uLEle=" + uLEle);
        System.out.println("   lineMag=" + lineMag);
        System.out.println("   eleMag=" + eleMag);
*/
        if (this.lineMag < 0) this.lineMag = 1;
        if (this.eleMag < 0) this.eleMag = 1;
        this.lineRes = directory[10];
        this.eleRes = directory[11];
        McIDASUtil.flip(directory,64,64);
        //System.out.println("nav type = " + new Integer(0).toHexString(directory[64]));
        int navLength;
        if (directory[64] == AREAnav.LALO) {
          navLength = 128;
        } else {
          navLength = 640;
        }
        nav = new int[navLength];
        for (int i=0; i<navLength; i++)
          this.nav[i] = directory[64+i];
        int auxLength = 0;
        int rows = 0;
        int cols = 0;
        int begLat= 0;
        int begLon =0;
        if (navLength == 128) {
          rows = this.nav[65];
          cols = this.nav[66];
          begLat = this.nav[78]/4;
          begLon = this.nav[79]/4;
          auxLength = begLon + (rows*cols);
          aux = new int[auxLength];
        }
        else {
          aux = new int[1];
        }
        int numPoints = rows * cols;
        for (int i=0; i<numPoints; i++) {
          this.aux[i+begLat] = directory[64+navLength+i];
        }
        if (auxLength > 0) {
          for (int i=0; i<numPoints; i++) {
            this.aux[i+begLon] = directory[64+navLength+numPoints+i];
          }
        }
        //System.out.println("navLength=" + navLength + " auxLength=" + auxLength); 
    }

    /**
     * getName - get text name of sensor source from ~mcidas/data/SATANNOT
     *
     */

     public String getName(int num) {
       String name = "";
       FileInputStream fis;
       //byte[] bline={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
       byte[] bline = new byte[31];
       int off=0;
       int ret = 0;
       int sensor=0;

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

    public int getFrameDirectory(int frame, int[] frmdir) throws Exception {
      int istat=0;
      if (fsi.myDir != frame) {
        frmdir = fsi.getFrameDir(frame);
        if (istat < 0) 
           System.out.println("FrameDirectory: Error in getFrameDirectory");
      }
      return istat;
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
}
