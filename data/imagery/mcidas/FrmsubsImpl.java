package ucar.unidata.data.imagery.mcidas;

import java.io.*;
import edu.wisc.ssec.mcidas.McIDASUtil;

/**
 * Class FrmsubsImpl contains wrappers for the native
 * language routines in frmsubs.c
 */
public class FrmsubsImpl {

   static {
     System.loadLibrary("frmsubsimpl");
   }


   //- wrappers for shared memory subroutines
   //----------------------------------------------------------------------------

   //- attach shared memory
   public static native int getshm(int val);

   //- get number of frames
   public static native int getnumfrm();

   //- get current frame number
   public static native int getcurfrm();

   //- get current frame, dimensions
   public static native int getfrmsize( int[] frame, int[] linsize, int[] elesize);

   //- get dirty flag
   public static native int getdirty(int frame);


   //- get the frame
   public static native int getfrm( int frm, int enh, int frame, int linsize, int elesize,
                                    byte[] img, int[] stretchtab, int[] colortab, int[] graphicstab);


   //- get graphics info
   public static native int getgrasize( int frame, int[] npts, int[] blocks, int[] mask);


   //- get the graphics
   public static native int getgra(int frame, int npts, int[] graphics );


   //- detach shared memory
   public static native int detshm();


    /**
     * Constructor
     */
    public FrmsubsImpl() {
      getSharedMemory();
    }

    public int getSharedMemory() {
      int val = new Integer(System.getProperty("MCENV_POSUC")).intValue();
      int istat = getshm(val);
      return istat;
    }

    public int getNumberOfFrames() {
      //System.out.println("getnumfrm");
      return getnumfrm();
    }

    public int getCurrentFrame() {
      //System.out.println("getcurfrm");
      return getcurfrm();
    }

    public int getFrameSize( int[] frame, int[] linsize, int[] elesize) {
      //System.out.println("getfrmsize");
      return getfrmsize(frame, linsize, elesize);
    }

    public int getDirtyFlag(int frame) {
      //System.out.println("getdirty");
      return getdirty(frame);
    }

    public int getFrameData(boolean infoData, boolean infoEnh, int frame, int linsize, int elesize,
                            byte[] img, int[] stretchtab, int[] colortab, int[] graphicstab) {
      int frm = 0;
      if (infoData) frm=1;
      int enh = 0;
      if (infoEnh) enh=1;
      return getfrm( frm, enh, frame, linsize, elesize, img, stretchtab, colortab, graphicstab);
    }

    public int getGraphicsSize(int frame, int[] npts, int[] blocks, int[] mask) {
      //System.out.println("  getgrasize frame=" + frame);
      if (getgrasize(frame, npts, blocks, mask) < 0) {
         System.out.println("FrmsubsImpl: getGraphicsSize  return code error");
         return -1;
      }
      //return getgrasize(frame, npts, blocks, mask);
      return 0;
    }

    public int getGraphics(int frame, int npts, int[] graphics ) {
      //System.out.println("getgra");
      return getgra(frame, npts, graphics );
    }

    public String fileName(int frame) {
      int frm=frame;
      if (frm < 1) frm=getCurrentFrame();

      String mcpath = System.getProperty("MCPATH");
      int sindx = mcpath.lastIndexOf(":") + 1;
      String sub = mcpath.substring(sindx);
      Integer frmInt = new Integer(frm);
      String fn = sub.concat("/Frame" + frmInt.toString() + ".0");
      
      return fn;
    }
      
    public int getFrameDirectory(int crfrm, int[] frmdir) {
      int ret=-1;

      String fName = fileName(crfrm);

      DataInputStream dis;
      try {
        dis = new DataInputStream (
          new BufferedInputStream(new FileInputStream(fName))
        );
      } catch (Exception e) {
        System.out.println("Can't open " + fName);
        return ret;
      }

      for (int i=0; i<frmdir.length; i++) {
        try {
          frmdir[i] = dis.readInt();
        } catch(Exception e) {
          System.out.println("Can't read " + fName);
          return ret;
        }
      }
      McIDASUtil.flip(frmdir,0,frmdir.length-1);

      ret = 0;
      return ret;
    }

    public int detachSharedMemory() {
      //System.out.println("detshm");
      return detshm();
    }
}
