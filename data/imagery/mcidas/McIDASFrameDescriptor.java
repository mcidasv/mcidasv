package ucar.unidata.data.imagery.mcidas;

import edu.wisc.ssec.mcidas.McIDASUtil;

import visad.DateTime;
import visad.VisADException;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import ucar.unidata.data.DataSource;

import ucar.unidata.xml.XmlDelegate;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlPersistable;
import ucar.unidata.util.Misc;

import org.w3c.dom.Element;


/**
 * A class to hold a frame directory, number and time
 */
public class McIDASFrameDescriptor {


    /** FrameDirectory */
//    FrameDirectory myDirectory;

    /** Frame number */
     
    public int myNumber;

    /** time of the frame */
//    private DateTime myTime = null;

    /** spectral band */
//    private int myBand = 0;

    /** McX link */
    public String myRequest;

    /**
     * Default constructor for unpersistence; does nothing
     *
     */
    public McIDASFrameDescriptor() {}

    /**
     * Create a descriptor from another.
     *
     * @param that   other image descriptor
     *
     */
    public McIDASFrameDescriptor(McIDASFrameDescriptor that) {
//        this.myDirectory = that.myDirectory;
        this.myNumber    = that.myNumber;
//        this.myBand    = that.myBand;
        this.myRequest    = that.myRequest;
/*
        try {
            this.myTime = new DateTime(that.myTime);
        } catch (Exception exc) {
            this.myTime = that.myTime;
        }
*/
    }

    /**
     * Create a frame descriptor from the frame number
     *
     * @param int frameNumber
     */
    public McIDASFrameDescriptor(int frameNumber, String request) {
        this.myNumber = frameNumber;
        this.myRequest = request;
//        processNumber();
    }

    /**
     * Create a frame descriptor from the number and FrameDirectory
     *
     * @param FrameDirectory   frame directory
     * @param int              frame number
     *
     */
/*
    public McIDASFrameDescriptor(FrameDirectory directory, int frameNumber, String link) {
        this.myDirectory = directory;
        this.myNumber    = frameNumber;
        this.myBand    = directory.getBand();
        this.myRequest = link;
        setTimeFromDirectory();
    }
*/

    /**
     * Use the frame number to create the frame directory
     */
/*
    private void processNumber() {

        int status = getFrameDirectory(this.myNumber);
        if (status < 0) {
            System.out.println("McIDASFrameDescriptor: can't processNumber");
            return;
        }
        setTimeFromDirectory();
        this.myBand = this.myDirectory.getBand();

    }
*/

  /** Get frame directory */
/*
    protected int getFrameDirectory(int frm) {
        int istat = 0;
        String frameDirRequest = myRequest +"B&text=" + frm;
        //System.out.println("frameDirRequest = " + frameDirRequest);

        URL dirUrl;
        URLConnection dirUrlc;
        DataInputStream dis = null;
        try
        {
          dirUrl = new URL(frameDirRequest);
          dirUrlc = dirUrl.openConnection();
          InputStream is = dirUrlc.getInputStream();
          dis = new DataInputStream( new BufferedInputStream(is));
        }
        catch (Exception e)
        {
          System.out.println("getFrameDirectory create DataInputStream exception e=" + e);
          return istat;
        }
        int dirLength = 64;
        int navLength = 640;
        int[] frmdir = new int[dirLength+navLength];
        try
        {
           int len = 0;
           int count = 0;

           while (len < dirLength+navLength) {
               try {
                   len = dis.available()/4;
               } catch (Exception e) {
                   System.out.println("getFrameDir: I/O error getting file size");
                   return istat;
               }
               //System.out.println("    len=" + len); 
               count++;
               if (count > 100)  return istat;
           }
           //System.out.println("frameNumber=" + frm + " len=" + len);
           if (len < dirLength+navLength) return istat;
           for (int i=0; i<len; i++)  {
               frmdir[i] = dis.readInt();
           }
        }
        catch (Exception e)
        {
            System.out.println("getFrameDir exception e=" + e);
        }
        istat = 1;

        McIDASUtil.flip(frmdir,0,frmdir.length-1);
        FrameDirectory fd = new FrameDirectory(frmdir);
        this.myDirectory = fd;

        return istat;
    }
*/


    /**
     * Set the time from the frame directory.
     */
/*
    private void setTimeFromDirectory() {
        if (this.myDirectory != null) {
            try {
                this.myTime = new DateTime(this.myDirectory.getNominalTime());
            } catch (VisADException ve) {
                this.myTime = null;
            }
        }
    }
*/

    /**
     * See if this McIDASFrameDescriptor is equal to the object in
     * question
     *
     * @param o   object in question
     * @return  true if <code>o</code> is an McIDASFrameDescriptor and
     *          they area equivalent
     */
    public boolean equals(Object o) {
        if ( !(o instanceof McIDASFrameDescriptor)) {
            return false;
        }
        McIDASFrameDescriptor that = (McIDASFrameDescriptor) o;
        return (this.myNumber == that.myNumber);
//        return Misc.equals(that.myDirectory, this.myDirectory)
//               && (this.myNumber == that.myNumber);
    }


    /**
     * Get the FrameDirectory
     *
     * @return  this objects image metadata
     */
/*
    public FrameDirectory getDirectory() {
        return this.myDirectory;
    }
*/


    /**
     * Get the spectral band of this image
     *
     * @return  this objects image metadata
     */
/*
    public int getBand() {
        return this.myBand;
    }
*/

    /**
     * Get the DateTime
     */
/*
    public DateTime getDateTime() {
        return this.myTime;
    }
*/

    /**
     * Get the Frame Number
     */
    public Integer getFrameNumber() {
        Integer frameInt = new Integer(this.myNumber);
        return frameInt;
    }

    /** 
     * Get link
     */
    public String getRequest() {
       return this.myRequest;
    }
 
    /**
     * Get a String representation of this object
     * @return a string representation
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        //System.out.println("myNumber=" + this.myNumber + " myTime=" + this.myTime);

        if (this.myNumber > 0) {
          buf.append("Frame " + this.myNumber);
/*
          FrameDirectory dir = this.myDirectory;
          int sensorNumber = dir.getSensorNumber();
          String name = dir.getName(sensorNumber);
          buf.append(name + " " + this.myBand + " ");
          buf.append(this.myTime.toString());
          buf.append(this.myRequest);
*/
        }

        return buf.toString();
    }
}
