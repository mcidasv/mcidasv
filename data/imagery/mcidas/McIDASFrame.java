package ucar.unidata.data.imagery.mcidas;

import edu.wisc.ssec.mcidas.McIDASUtil;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.Class;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import ucar.unidata.util.Misc;


/**
 * Class to hold McIDAS-X frame datasets
 */
public class McIDASFrame {

    int testFlag = 0;

    /** source frame number */
    private int myNumber;
    private String myRequest;

    /** frame descriptor */
    private McIDASFrameDescriptor myDescriptor;

    private FrameSubs myFs;


    /** frame data */
    private byte[]         myImg;       /* image pixel values */
    private float[][]      myEnhTable;  /* enhancement table  */
    private int[]          myGraphics;  /* graphics           */
    private int[][]        myGraLocs;   /* graphics locations */
    private FrameDirectory myFrameDir;  /* frame directory    */

    /** frame dimensions */
    private int myLines;
    private int myElems;

    /** magnification factors */
    private int lMag;
    private int eMag;

    private int status;
    private int success = 0;

    private DataInputStream inputStream;
 
    /**
     *  Empty constructor for XML encoding
     */
    public McIDASFrame() {}

//    public McIDASFrame(int i) {}

    /**
     * Construct a new McIDASFrame from the given frame number
     *
     * @param frameNumber       frame number
     */
    public McIDASFrame(int frameNumber, String request) {
        FrameSubs fs = new FrameSubs(request);
        this.myFs = fs;
        this.myElems = fs.getEleSize(frameNumber);
        this.myLines = fs.getLineSize(frameNumber);
        int[] stab = fs.stretchTable;
        int[] ctab = fs.colorTable;
        this.myEnhTable = getEnhTable(stab, ctab);
        this.myRequest = request;
        this.myNumber = frameNumber;
        this.myImg = fs.getImg(frameNumber);
        int[] dir = fs.getFrameDir(frameNumber);
        this.myFrameDir = new FrameDirectory(dir);
    }


    private float[][] getEnhTable(int[]stab, int[] ctab) {
        float[][] enhTable = new float[3][256];
        for (int i=1; i<18; i++) {
            enhTable[0][i] = (float)((ctab[i]/0x10000)&0xff);
            enhTable[1][i] = (float)((ctab[i]/0x100)&0xff);
            enhTable[2][i] = (float)(ctab[i]&0xff);
            //System.out.println(i + ": red=" + enhTable[0][i] + " green=" + enhTable[1][i]
            //                     + " blue=" + enhTable[2][i]);
        }
        for (int i=18; i<256; i++) {
            enhTable[0][i] = (float)((ctab[stab[i]]/0x10000)&0xff);
            enhTable[1][i] = (float)((ctab[stab[i]]/0x100)&0xff);
            enhTable[2][i] = (float)(ctab[stab[i]]&0xff);
            //System.out.println(i + ": red=" + enhTable[0][i] + " green=" + enhTable[1][i]
            //                     + " blue=" + enhTable[2][i]);
        }
        for (int i=0; i<256; i++) {
            enhTable[0][i] /= 0xff;
            enhTable[1][i] /= 0xff;
            enhTable[2][i] /= 0xff;
        }
        return enhTable;
    }

        
    /** Get current frame number */
    public int getCurrentFrame() {
        return this.myFs.getCurrentFrame();
    }


    /** Get frame number */
    public int getFrameNumber() {
        return this.myNumber;
    }

    /** Get frame data */
    public byte[] getFrameData() {
    return this.myImg;
  }


    /** Get frame data */
    public int getLineSize() {
    return this.myLines;
  }


    /** Get frame data */
    public int getElementSize() {
    return this.myElems;
  }


    /** Get Enhancement Table */
    public float[][] getEnhancementTable() {
    return this.myEnhTable;
  }


    /** Get Frame Directory */
    public FrameDirectory getFrameDirectory() {
    return this.myFrameDir;
  }


    /** Get graphics data */
  protected int getGraphicsData() {

    status = 0;
    int npts=0;
    try {
//      npts = fsi.getGraphicsSize(this.myNumber);
    } catch (Exception e) {
      status = -1;
      System.out.println("McIDASFrame: unable to detemine graphics size");
      return status;
    }

    int[] gra = new int[npts];
    int[] color_pts = new int[npts];
    int[][] loc_pts = new int[2][npts];

    if (npts > 0) {
      try {
//        status = fsi.getGraphics(this.myNumber, npts, gra);
        //System.out.println("McIDASFrame getGraphicsData: getGraphics status=" + status);
      } catch (Exception e) {
          System.out.println("McIDASFrame: Graphics not read");
      }
      if (status < 0) {
          return status;
      }

      int loc,lin;
      int gpts = 0;
      int elems = this.myElems * this.eMag;

      for (int i=0; i<npts; i++) {
        loc = gra[i]/0x100;
        if (elems == 0) {
          System.out.print("McIDASFrame: getGraphicsData elems=" + elems);
          return -1;
        }
        lin = (loc-1)/elems;
        lin /= this.lMag;
        if (lin >= 12) {
          loc_pts[0][gpts] = lin;
          loc_pts[1][gpts] = (loc-1) % elems;
          loc_pts[1][gpts] /=  this.eMag;
          color_pts[gpts] = gra[i]&0x000000ff;
          gpts++;
        }
      }
    }
    this.myGraphics = color_pts;
    this.myGraLocs = loc_pts;

    //System.out.println("McIDASFrame: getGraphicsData done");
    return success;
  }
}
