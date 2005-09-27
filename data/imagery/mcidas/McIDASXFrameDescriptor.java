package ucar.unidata.data.imagery;


import visad.DateTime;
import visad.VisADException;

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

    private FrmsubsImpl fsi = new FrmsubsImpl();

    /** FrameDirectory */
    FrameDirectory myDirectory;

    /** Frame number */
    int myNumber;

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
        myNumber = frameNumber;
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
        myDirectory = directory;
        myNumber    = frameNumber;
        setTimeFromDirectory();
    }

    /**
     * Use the frame number to create the frame directory
     */
    private void processNumber() {
        int[] frmdir = new int[704];
        fsi.getFrameDirectory(myNumber, frmdir);
        myDirectory = new FrameDirectory(frmdir);
        setTimeFromDirectory();
    }


    /**
     * Set the time from the frame directory.
     */
    private void setTimeFromDirectory() {
        if (myDirectory != null) {
            try {
                myTime = new DateTime(myDirectory.getNominalTime());
            } catch (VisADException ve) {
                myTime = null;
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
        return Misc.equals(that.myDirectory, myDirectory)
               && (myNumber == that.myNumber);
    }


    /**
     * Get the FrameDirectory
     *
     * @return  this objects image metadata
     */
    public FrameDirectory getDirectory() {
        return myDirectory;
    }

    /**
     * Get the DateTime
     */
    public DateTime getDateTime() {
        return myTime;
    }


    /**
     * Get a String representation of this object
     * @return a string representation
     */
    public String toString() {
//        return myDirectory.toString();
        return myTime.toString();
    }
}
