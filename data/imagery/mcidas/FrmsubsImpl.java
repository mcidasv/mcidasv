package ucar.unidata.data.imagery.mcidas;


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
   public static native int getshm();

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


   //- get the frame directory
   public static native int getdir(int crfrm, int[] frmdir);


   //- detach shared memory
   public static native int detshm();


    /**
     * Constructor
     */
    public FrmsubsImpl() {
    }

    public int getSharedMemory() {
      //System.out.println("getshm");
      return this.getshm();
    }

    public int getNumberOfFrames() {
      //System.out.println("getnumfrm");
      return this.getnumfrm();
    }

    public int getCurrentFrame() {
      //System.out.println("getcurfrm");
      return this.getcurfrm();
    }

    public int getFrameSize( int[] frame, int[] linsize, int[] elesize) {
      //System.out.println("getfrmsize");
      return this.getfrmsize(frame, linsize, elesize);
    }

    public int getDirtyFlag(int frame) {
      //System.out.println("getdirty");
      return this.getdirty(frame);
    }

    public int getFrameData(boolean infoData, boolean infoEnh, int frame, int linsize, int elesize,
                            byte[] img, int[] stretchtab, int[] colortab, int[] graphicstab) {
      int frm = 0;
      if (infoData) frm=1;
      int enh = 0;
      if (infoEnh) enh=1;
      return this.getfrm( frm, enh, frame, linsize, elesize, img, stretchtab, colortab, graphicstab);
    }

    public int getGraphicsSize(int frame, int[] npts, int[] blocks, int[] mask) {
      //System.out.println("  getgrasize frame=" + frame);
      if (this.getgrasize(frame, npts, blocks, mask) < 0) {
         System.out.println("FrmsubsImpl: getGraphicsSize  return code error");
         return -1;
      }
      //return this.getgrasize(frame, npts, blocks, mask);
      return 0;
    }

    public int getGraphics(int frame, int npts, int[] graphics ) {
      //System.out.println("getgra");
      return this.getgra(frame, npts, graphics );
    }

    public int getFrameDirectory(int crfrm, int[] frmdir) {
      //System.out.println("FrmsubsImpl: getdir crfrm=" + crfrm);
      return this.getdir(crfrm, frmdir);
    }

    public int detachSharedMemory() {
      //System.out.println("detshm");
      return this.detshm();
    }
}
