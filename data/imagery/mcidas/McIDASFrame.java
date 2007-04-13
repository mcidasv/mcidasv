package ucar.unidata.data.imagery.mcidas;

import java.util.List;

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
    private List      myGraphics;  /* graphics           */
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

    /**
     *  Empty constructor for XML encoding
     */
    public McIDASFrame() {}


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
        this.myGraphics = fs.getGraphics(frameNumber);
        int[] dir = fs.getFrameDir(frameNumber);
        this.myFrameDir = new FrameDirectory(dir);
    }


    private float[][] getEnhTable(int[]stab, int[] ctab) {
        float[][] enhTable = new float[3][256];
        for (int i=1; i<18; i++) {
            enhTable[0][i] = (float)((ctab[i]/0x10000)&0xff);
            enhTable[1][i] = (float)((ctab[i]/0x100)&0xff);
            enhTable[2][i] = (float)(ctab[i]&0xff);
        }
        for (int i=18; i<256; i++) {
            enhTable[0][i] = (float)((ctab[stab[i]]/0x10000)&0xff);
            enhTable[1][i] = (float)((ctab[stab[i]]/0x100)&0xff);
            enhTable[2][i] = (float)(ctab[stab[i]]&0xff);
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
  protected List getGraphicsData() {
    return this.myGraphics;
  }
}
