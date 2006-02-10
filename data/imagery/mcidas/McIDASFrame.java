package ucar.unidata.data.imagery.mcidas;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;

import ucar.unidata.util.Misc;


/**
 * Class to hold McIDAS-X frame datasets
 */
public class McIDASFrame {

    /** source frame number */
    int myNumber;

    /** frame descriptor */
    McIDASXFrameDescriptor myDescriptor;


    /** frame data */
    protected byte[]         myImg;       /* image pixel values */
    protected float[][]      myEnhTable;  /* enhancement table  */
    protected int[]          myGraphics;  /* graphics           */
    protected int[][]        myGraLocs;   /* graphics locations */
    protected FrameDirectory myFrameDir;  /* frame directory    */

    /** frame dimensions */
    protected int lines;
    protected int elems;

    /** magnification factors */
    protected int lMag;
    protected int eMag;

/* ???
    private FrmsubsImpl fsi = new FrmsubsImpl();
*/
    private static FrmsubsMM fsi;
    static {
      try {
        fsi = new FrmsubsMM();
        fsi.getMemoryMappedUC();
      } catch (Exception e) { }
    }

    private int status;
    private int success = 0;
 
    /**
     *  Empty constructor for XML encoding
     */
    public McIDASFrame() {}


    /**
     * Construct a new McIDASFrame from the given frame number
     *
     * @param frameNumber       frame number
     */
    public McIDASFrame(int frameNumber) {
      if (frameNumber < 1) {
        frameNumber = fsi.getCurrentFrame();
        if (frameNumber < 0) {
          System.out.println("McIDASFrame: unable to get current frame number");
        }
      }
      this.myNumber = frameNumber;
    }


    /** Get frame data */
    protected int getFrameData(boolean infoData, boolean infoEnh) {
 
     int frm = this.myNumber;

     FrameDirectory fd = this.myFrameDir;
     if (fd == null) {
       getFrameDirectory(frm);
       fd = this.myFrameDir;
     }

     int linsize = 0;
     int elesize = 0;
     int lineMag = fd.lineMag;
     int eleMag = fd.eleMag;

     linsize = fsi.getLineSize(frm)/lineMag;
     elesize = fsi.getEleSize(frm)/eleMag;

     if (status<0) {
       System.out.println("McIDASFrame: unable to get frame size myNumber=" + this.myNumber);
       return status;
     }

     this.lines = linsize;
     this.elems = elesize;
     this.lMag = lineMag;
     this.eMag = eleMag;
     //System.out.println("current frame: "+this.myNumber+"\n"+"linsize: "+this.lines+"\n"+"elesize: "+this.elems);

     int imgSize = 1;
     if (infoData)
       imgSize = this.lines*this.elems;
     byte[] img = new byte[imgSize];

     int enhSize = 1;
     if (infoEnh)
       enhSize = 256;
     int[] stab = new int[enhSize];
     int[] ctab = new int[enhSize];
     int[] gtab = new int[enhSize];

     //System.out.println("myNumber=" + this.myNumber + " lines=" + this.lines + " elems=" + this.elems);
     status = fsi.getFrameData(infoData, infoEnh, this.myNumber,
                               img, stab, ctab, gtab);
     if (status<0) {
       System.out.println("McIDASFrame: unable to get frame data");
       return status;
     }
     if (infoData)
       this.myImg = img;

     if (infoEnh) {
       //System.out.println("McIDASFrame: getFrameData  making enhTable");
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
       //System.out.println("McIDASFrame: done");
     }

     return success;
  }


    /** Get graphics data */
  protected int getGraphicsData() {

    status = 0;
    int npts=0;
    try {
      npts = fsi.getGraphicsSize(this.myNumber);
    } catch (Exception e) {
        status = -1;
        System.out.println("McIDASFrame: File not found");
    }
    if (status<0) {
      System.out.println("McIDASFrame: unable to get graphics size");
      return status;
    }

    int[] gra = new int[npts];

    try {
      status = fsi.getGraphics(this.myNumber, npts, gra);
    } catch (Exception e) {
        System.out.println("McIDASFrame: File not found");
    }
    if (status<0) {
      System.out.println("McIDASFrame: unable to get graphics data");
      return status;
    }

    int[] color_pts = new int[npts];
    int[][] loc_pts = new int[2][npts];
    int loc,lin;
    int gpts = 0;
    int elems = this.elems * this.eMag;

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
    this.myGraphics = color_pts;
    this.myGraLocs = loc_pts;

    //System.out.println("McIDASFrame: getGraphicsData done");
    return success;
  }


  /** Get frame directory */
  protected int getFrameDirectory(int frm) {
    int istat = 0;

    if (frm != fsi.myDir) {
      istat = fsi.getFrameDirectory(frm);
      if (istat < 0) {
        System.out.println("McIDASFrame getFrameDirectory: can't read directory frame=" + frm);
        return istat;
      }
    }
      
    int[] frmdir = fsi.myFrmdir;

    FrameDirectory fd = new FrameDirectory(frmdir);
    this.myFrameDir = fd;

    istat = 0;
    return istat;
  }
}
