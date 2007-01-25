package ucar.unidata.data.imagery.mcidas;


import visad.DateTime;
import visad.VisADException;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
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
    FrameDirectory myDirectory;

    /** Frame number */
     
    public int myNumber;

    /** time of the frame */
    private DateTime myTime = null;

    /** spectral band */
    private int myBand = 0;

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
        this.myDirectory = that.myDirectory;
        this.myNumber    = that.myNumber;
        this.myBand    = that.myBand;
        this.myRequest    = that.myRequest;
        try {
            this.myTime = new DateTime(that.myTime);
        } catch (Exception exc) {
            this.myTime = that.myTime;
        }
    }

    /**
     * Create a frame descriptor from the frame number
     *
     * @param int frameNumber
     */
    public McIDASFrameDescriptor(int frameNumber) {
        this.myNumber = frameNumber;
        processNumber();
    }

    /**
     * Create a frame descriptor from the number and FrameDirectory
     *
     * @param FrameDirectory   frame directory
     * @param int              frame number
     *
     */
    public McIDASFrameDescriptor(FrameDirectory directory, int frameNumber, String link) {
        this.myDirectory = directory;
        this.myNumber    = frameNumber;
        this.myBand    = directory.getBand();
        this.myRequest = link;
        setTimeFromDirectory();
    }

    /**
     * Use the frame number to create the frame directory
     */
    private void processNumber() {
/*
       if (this.myNumber != fsi.myDir) {
         int status = fsi.getFrameDirectory(this.myNumber);
         if (status < 0) {
            System.out.println("McIDASFrameDescriptor: can't processNumber");
            return;
         }
       }
       int[] frmdir = fsi.myFrmdir;
        this.myDirectory = new FrameDirectory(frmdir);
        setTimeFromDirectory();
        this.myBand = this.myDirectory.getBand();
*/
    }


    /**
     * Set the time from the frame directory.
     */
    private void setTimeFromDirectory() {
        if (this.myDirectory != null) {
            try {
                this.myTime = new DateTime(this.myDirectory.getNominalTime());
            } catch (VisADException ve) {
                this.myTime = null;
            }
        }
    }


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
        return Misc.equals(that.myDirectory, this.myDirectory)
               && (this.myNumber == that.myNumber);
    }


    /**
     * Get the FrameDirectory
     *
     * @return  this objects image metadata
     */
    public FrameDirectory getDirectory() {
        return this.myDirectory;
    }


    /**
     * Get the spectral band of this image
     *
     * @return  this objects image metadata
     */
    public int getBand() {
        return this.myBand;
    }

    /**
     * Get the DateTime
     */
    public DateTime getDateTime() {
        return this.myTime;
    }

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
//        return this.myDirectory.toString();
//        return myTime.toString();
        StringBuffer buf = new StringBuffer();
        //System.out.println("myNumber=" + this.myNumber + " myTime=" + this.myTime);

        if (this.myNumber > 0) {
          buf.append(this.myNumber + " ");
          FrameDirectory dir = this.myDirectory;
          int sensorNumber = dir.getSensorNumber();
          String name = dir.getName(sensorNumber);
          buf.append(name + " " + this.myBand + " ");
          buf.append(this.myTime.toString());
          //buf.append(this.myRequest);
        }

        return buf.toString();
    }
}
