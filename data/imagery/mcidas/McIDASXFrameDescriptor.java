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
public class McIDASXFrameDescriptor {

/* ???
    private FrmsubsImpl fsi = new FrmsubsImpl();
*/
    //private FrmsubsMM fsi = new FrmsubsMM();
    private static FrmsubsMM fsi;
    static {
      try {
        fsi = new FrmsubsMM();
        fsi.getMemoryMappedUC();
      } catch (Exception e) { }
    }

    /** FrameDirectory */
    FrameDirectory myDirectory;

    /** Frame number */
    public int myNumber;

    /** time of the frame */
    DateTime myTime = null;

    /**
     * Default constructor for unpersistence; does nothing
     *
     */
    public McIDASXFrameDescriptor() {}

    /**
     * Create a descriptor from another.
     *
     * @param that   other image descriptor
     *
     */
    public McIDASXFrameDescriptor(McIDASXFrameDescriptor that) {
        this.myDirectory = that.myDirectory;
        this.myNumber    = that.myNumber;
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
    public McIDASXFrameDescriptor(int frameNumber) {
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
    public McIDASXFrameDescriptor(FrameDirectory directory, int frameNumber) {
        this.myDirectory = directory;
        this.myNumber    = frameNumber;
        setTimeFromDirectory();
    }

    /**
     * Use the frame number to create the frame directory
     */
    private void processNumber() {
       if (this.myNumber != fsi.myDir) {
         int status = fsi.getFrameDirectory(this.myNumber);
         if (status < 0) {
            System.out.println("McIDASXFrameDescriptor: can't processNumber");
            return;
         }
       }
       int[] frmdir = fsi.myFrmdir;
        this.myDirectory = new FrameDirectory(frmdir);
        setTimeFromDirectory();
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
     * See if this McIDASXFrameDescriptor is equal to the object in
     * question
     *
     * @param o   object in question
     * @return  true if <code>o</code> is an McIDASXFrameDescriptor and
     *          they area equivalent
     */
    public boolean equals(Object o) {
        if ( !(o instanceof McIDASXFrameDescriptor)) {
            return false;
        }
        McIDASXFrameDescriptor that = (McIDASXFrameDescriptor) o;
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
     * Get the DateTime
     */
    public DateTime getDateTime() {
        return this.myTime;
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
          buf.append(this.myTime.toString());
        }
        return buf.toString();
    }
}
