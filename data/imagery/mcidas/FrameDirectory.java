package ucar.unidata.data.imagery.mcidas;

import edu.wisc.ssec.mcidas.McIDASUtil;
import java.awt.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Date;

/**
 * Class FrameDirectory holds information obtained
 * from frame directory files, FRAMEn.p, from McIDAS-X
 */
public class FrameDirectory {

/* ???
    private FrmsubsImpl fsi = new FrmsubsImpl();
*/
    //private FrmsubsMM fsi = new FrmsubsMM();
    private FrmsubsMM fsi;

    /** time of data in frame */
   private Date nominalTime;

    /** number of frames */
    public int numberOfFrames;

    /** Sensor source name */
    private String sensorName;

    /** Sensor source number */
    public int sensorNumber;

    /** Year and Julian day, ccyyddd */
    public int cyd;

    /** Time, hhmmss */
    public int hms;

    /** Band number */
    public int band;

    /** Upper-left corner satellite coordinates */
    public int uLLine;
    public int uLEle;


    /** Magnification factors */
    public int lineMag;
    public int eleMag;

    /** Resolution factors */
    public int lineRes;
    public int eleRes;

    /** Navigation block */
    public int[] nav = new int[640];

    /**
     * Constructor
     *
     *
     */
    public FrameDirectory() throws Exception {
      fsi = new FrmsubsMM();
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
    }



    /**
     * Constructor
     *
     * @param directory frame directory from McIDAS-X
     *
     */
    public FrameDirectory(int[] directory) {
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
        this.lineRes = directory[10];
        this.eleRes = directory[11];
        for (int i=0; i<640; i++)
          this.nav[i] = directory[64+i];
        McIDASUtil.flip(this.nav,0,0);
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
             if (next != 32) {
               bline[ptr] = (byte)next;
               ptr++;
             }
           }
           name = new String(bline, 0, ptr);
           for (int i=20;i<30; i++) {
             off = fis.read();
           }
           sensor1 = fis.read()-48;
           sensor2 = fis.read()-48;
           sensor3 = fis.read()-48;
           sensor = 0;
           if (sensor1 >= 0)
             sensor = sensor1*100;
           if (sensor2 >= 0)
             sensor += sensor2*10;
           if (sensor3 >= 0)
             sensor += sensor3;
           for (int i=32; i<80; i++)
             off = fis.read();
         } catch(Exception e) {
           System.out.println("FrameDirectory: Can't read SATANNOT");
           return name;
         }
         if (sensor == num) ret =-1;
         counter++;
         if (counter>200) ret=-1;
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

    public int getFrameDirectory(int frm, int[] frmdir) throws Exception {
      int istat = fsi.getFrameDirectory(frm, frmdir);
      if (istat < 0) 
         System.out.println("FrameDirectory: Error in getFrameDirectory");
      return istat;
    }    

}
