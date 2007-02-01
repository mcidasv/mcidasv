package ucar.unidata.data.imagery.mcidas;

import edu.wisc.ssec.mcidas.McIDASUtil;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ucar.unidata.util.Misc;


/**
 * Class to hold McIDAS-X frame datasets
 */
public class McXFrame {

    /** source frame number */
    public int myNumber;
    public static String myRequest;
    public URL myURL;
    public DataInputStream myDis;

    /** frame data from McCommandLineChooser */
    public byte[]         myImg;       /* image pixel values */
    public float[][]      myEnhTable;  /* enhancement table  */
    public int[]          myGraphics;  /* graphics           */
    public int[][]        myGraLocs;   /* graphics locations */
    public int[]          myDir;       /* directory block    */
    public FrameDirectory myFrameDir;  /* frame directory    */
    public int[]          myNav;       /* navigation block   */
    public int[]          myAux;       /* auxilliary block   */

    /** frame dimensions */
    public int lines;
    public int elems;

    /** magnification factors */
    protected int lMag;
    protected int eMag;
    protected Date dateTime;

    private int status;
    private int success = 0;
 
    /**
     *  Empty constructor for XML encoding
     */
    public McXFrame() {}


    /**
     * Construct a new McXFrame from the given frame number
     *
     * @param frameNumber       frame number
     */
    public McXFrame(String request, int frameNumber, int height, int width) {
        if (frameNumber < 1) {
            System.out.println("McXFrame: unable to get current frame number");
        }

        this.myRequest = request;
        this.myNumber = frameNumber;
        this.lines = height;
        this.elems = width;
    }


    /** Get frame data */
    protected int getFrameData(boolean infoData, boolean infoEnh) {
 
     int frm = this.myNumber;
     String getFrameDataRequest = myRequest +"I&text=" + frm;
     //System.out.println("getFrameDataRequest = " + getFrameDataRequest);

     FrameDirectory fd = this.myFrameDir;
     if (fd == null) {
       getFrameDirectory(frm);
       fd = this.myFrameDir;
     }

     int lineMag = fd.lineMag;
     int eleMag = fd.eleMag;
     dateTime = fd.getNominalTime();

     if (status<0) {
       System.out.println("McXFrame: unable to get frame size myNumber=" + this.myNumber);
       return status;
     }

     this.lMag = lineMag;
     this.eMag = eleMag;

     int imgSize = 1;
     if (infoData)
       imgSize = this.lines*this.elems;
     byte[] img = new byte[imgSize];
/*
     int enhSize = 1;
     if (infoEnh)
       enhSize = 256;
     int[] stab = new int[enhSize];
     int[] ctab = new int[enhSize];
     int[] gtab = new int[enhSize];

     //System.out.println("myNumber=" + this.myNumber + " lines=" + this.lines + " elems=" + this.elems);
     //status = fsi.getFrameData(infoData, infoEnh, this.myNumber,
     //                          img, stab, ctab, gtab);
     if (status<0) {
       System.out.println("McXFrame: unable to get frame data");
       return status;
     }
     if (infoData)
       this.myImg = img;

     if (infoEnh) {
       //System.out.println("McXFrame: getFrameData  making enhTable");
       float[][] enhTable = new float[3][256];
       for (int i=1; i<18; i++) {
         enhTable[0][i] = (float)((ctab[i]/0x10000)&0xff);
         enhTable[1][i] = (float)((ctab[i]/0x100)&0xff);
         enhTable[2][i] = (float)(ctab[i]&0xff);
         // System.out.println(i + ": red=" + enhTable[0][i] + " green=" + enhTable[1][i]
         //                      + " blue=" + enhTable[2][i]);
       }
       for (int i=18; i<256; i++) {
         enhTable[0][i] = (float)((ctab[stab[i]]/0x10000)&0xff);
         enhTable[1][i] = (float)((ctab[stab[i]]/0x100)&0xff);
         enhTable[2][i] = (float)(ctab[stab[i]]&0xff);
         // System.out.println(i + ": red=" + enhTable[0][i] + " green=" + enhTable[1][i]
         //                      + " blue=" + enhTable[2][i]);
       }
       for (int i=0; i<256; i++) {
         enhTable[0][i] /= 0xff;
         enhTable[1][i] /= 0xff;
         enhTable[2][i] /= 0xff;
       }
       this.myEnhTable = enhTable;
       //System.out.println("McXFrame: done");
     }
*/

     return success;
  }


    /** Get graphics data */
  protected int getGraphicsData() {

    status = 0;
    int npts=0;
    try {
      //npts = fsi.getGraphicsSize(this.myNumber);
    } catch (Exception e) {
      status = -1;
      System.out.println("McXFrame: unable to detemine graphics size");
      return status;
    }

    int[] gra = new int[npts];
    int[] color_pts = new int[npts];
    int[][] loc_pts = new int[2][npts];

    if (npts > 0) {
      try {
        //status = fsi.getGraphics(this.myNumber, npts, gra);
        //System.out.println("McXFrame getGraphicsData: getGraphics status=" + status);
      } catch (Exception e) {
          System.out.println("McXFrame: Graphics not read");
      }
      if (status < 0) {
          return status;
      }

      int loc,lin;
      int gpts = 0;
      int elems = this.elems * this.eMag;

      for (int i=0; i<npts; i++) {
        loc = gra[i]/0x100;
        if (elems == 0) {
          System.out.print("McXFrame: getGraphicsData elems=" + elems);
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

    //System.out.println("McXFrame: getGraphicsData done");
    return success;
  }


  /** Get frame directory */
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
    this.myFrameDir = fd;

    return istat;
  }
}
