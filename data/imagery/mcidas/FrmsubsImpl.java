package ucar.unidata.data.imagery;


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
   public static native int getfrm( int frame, int linsize, int elesize,
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
      return getshm();
    }

    public int getNumberOfFrames() {
      return getnumfrm();
    }

    public int getCurrentFrame() {
      return getcurfrm();
    }

    public int getFrameSize( int[] frame, int[] linsize, int[] elesize) {
      return getfrmsize(frame, linsize, elesize);
    }

    public int getDirtyFlag(int frame) {
      return getdirty(frame);
    }

    public int getFrameData(int frame, int linsize, int elesize,
                            byte[] img, int[] stretchtab, int[] colortab, int[] graphicstab) {
      return getfrm( frame, linsize, elesize, img, stretchtab, colortab, graphicstab);
    }

    public int getGraphicsSize(int frame, int[] npts, int[] blocks, int[] mask) {
      return getgrasize(frame, npts, blocks, mask);
    }

    public int getGraphics(int frame, int npts, int[] graphics ) {
      return getgra(frame, npts, graphics );
    }

    public int getFrameDirectory(int crfrm, int[] frmdir) {
      return getdir(crfrm, frmdir);
    }

    public int detachSharedMemory() {
      return detshm();
    }
}
