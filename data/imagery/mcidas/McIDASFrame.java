package ucar.unidata.data.imagery;


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

    private FrmsubsImpl fsi = new FrmsubsImpl();
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
      if (frameNumber == -1)
        frameNumber = fsi.getCurrentFrame();
      myNumber = frameNumber;
    }

    /** Attach shared memory for given frame */
    protected int attachSharedMemory() {
      return fsi.getSharedMemory();
    }

    /** Get frame data */
    protected int getFrameData() {

     int[] frm_a  = new int[] {0};
     int[] linsize_a = new int[] {0};
     int[] elesize_a = new int[] {0};

     frm_a[0] = myNumber;
    
     status = fsi.getFrameSize(frm_a, linsize_a, elesize_a);
     if (status<0)
       return status;

     lines = linsize_a[0];
     elems = elesize_a[0];
     // System.out.println("current frame: "+myNumber+"\n"+"linsize: "+lines+"\n"+"elesize: "+elems);

     byte[] img =  new byte[(lines)*elems];
     int[] stab = new int[256];
     int[] ctab = new int[256];
     int[] gtab = new int[256];

     status = fsi.getFrameData(myNumber, lines, elems, img, stab, ctab, gtab);
     if (status<0)
       return status;
     myImg = img;

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
     myEnhTable = enhTable;

     return success;
  }


    /** Get graphics data */
  protected int getGraphicsData() {

    int[] npts_a = new int[] {0};
    int[] nblocks_a = new int[] {0};
    int[] mask_a = new int[] {0};

    status = fsi.getGraphicsSize(myNumber, npts_a, nblocks_a, mask_a);
    if (status<0)
      return status;

    int npts = npts_a[0];
    int nblocks = nblocks_a[0];
    int mask = mask_a[0];

    int[] gra = new int[npts];

    status = fsi.getGraphics(myNumber, npts, gra);
    if (status<0)
      return status;

    int[] color_pts = new int[npts];
    int[][] loc_pts = new int[2][npts];
    int loc,lin;
    int gpts = 0;
    for (int i=0; i<npts; i++) {
      loc = gra[i]/0x100;
      lin = (loc-1)/elems;
      if (lin >= 12) {
        loc_pts[0][gpts] = lin;
        loc_pts[1][gpts] = (loc-1) % elems;
        color_pts[gpts] = gra[i]&0x000000ff;
        gpts++;
      }
    }
    myGraphics = color_pts;
    myGraLocs = loc_pts;

    return success;
  }


  /** Get frame directory */
  protected int getFrameDirectory() {
    int dret;
    int[] frmdir = new int[704];

    status = fsi.getFrameDirectory(myNumber, frmdir);
    if (status<0)
      return status;

    FrameDirectory fd = new FrameDirectory(frmdir);
    myFrameDir = fd;
    return success;
  }
}
