package ucar.unidata.data.imagery.mcidas;

import edu.wisc.ssec.mcidas.McIDASUtil;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import ucar.unidata.util.Misc;

public class FrameSubs {

    int myFrameNo;
    String myRequest;
    String ucRequest;
    String fileRequest;
    String dataRequest;

    private URL url;
    private URLConnection urlc;
    private DataInputStream inputStream;

    static int lastFrame = 0;
    static int blockSize = 256;

    int width = 0;
    int height = 0;
    int[] stretchTable = new int[256];
    int[] colorTable = new int[256];
    int[] graphicsTable = new int[256];

    public int myFrmdir[];
    public byte myImg[];
    public int myDir=0;

    /**
     * Constructor
     */
    public FrameSubs(String request) {
      this.myRequest = request;
      this.myFrameNo = 0;
      getRequestStrings();
    }

    private void getRequestStrings() {
        this.ucRequest = this.myRequest + "W&text=";
        this.fileRequest = this.myRequest + "F&text=";
        this.dataRequest = this.myRequest + "D&text=";
    }


    private DataInputStream getInputStream(String newRequest) {
        DataInputStream retStream = null;
        try {
            url = new URL(newRequest);
            urlc = url.openConnection();
            InputStream is = urlc.getInputStream();
            retStream = new DataInputStream( new BufferedInputStream(is));
        } catch (Exception e) {
            System.out.println("FrameSubs getInputStream: exception e=" + e);
        }
        return retStream;
    }


    private int getInts(int offset, int[] buf) {
        int numRead = 0;
        if (this.ucRequest == null) {
            getRequestStrings();
        }

        String newRequest = this.ucRequest + offset;
        inputStream = getInputStream(newRequest);
        try {
            int avail = 0;
            int count = 0;
            while (avail < buf.length) {
                avail = inputStream.available()/4;
                count++;
                if (count > 100) return numRead;
            }
            for (int i=0; i<buf.length; i++) {
                buf[i] = inputStream.readInt();
                numRead++;
            }
        }
        catch (Exception e)
       {
            System.out.println("FrameSubs getInts offset=" + offset + ":exception e=" + e);
            return numRead;
        }
        try {
            inputStream.close();
        } catch (Exception ee) {
        }
        McIDASUtil.flip(buf,0,numRead-1);
        return numRead;
    }

/*
    private int getBytes(int offset, byte[] buf) {
        int numRead = 0;
        if (this.ucRequest == null) {
           getRequestStrings();
        }

        String newRequest = this.ucRequest + offset + " " + buf.length;
        inputStream = getInputStream(newRequest);
         
        try {
            int avail = 0;
            int count = 0;
            while (avail < buf.length) {
                avail = inputStream.available();
                count++;
                if (count > 100) return numRead;
            }
            numRead = inputStream.read(buf, 0, buf.length);
        }
        catch (Exception e)
        {
            System.out.println("FrameSubs getBytes offset=" + offset + ":exception e=" + e);
        }
        try {
            inputStream.close();
        } catch (Exception ee) {
        }
        return numRead;
    }
*/


    public int getNumberOfFrames() {
        int[] buf = { 0 };
        if (getInts(13, buf) < 1) return -1;
        return buf[0];
    }

    public int getCurrentFrame() {
        int[] buf = { 0 };
        if (getInts(51, buf) < 1) return -1;
        return buf[0];
    }


    public int getLineSize(int frame ) {
        if (height == 0) {
           if (getFrameData(frame) < 0) height = -1;
        }
        return height;
    }

    public int getEleSize(int frame ) {
        if (width == 0) {
            if (getFrameData(frame) < 0) width = -1;
        }
        return width;
    }


    private int getFrameData(int frame) {
        int ret = -1;
        if (frame < 0) frame = getCurrentFrame();
        if (this.dataRequest == null) {
           getRequestStrings();
        }

        String newRequest = this.dataRequest + frame;
        inputStream = getInputStream(newRequest);

        try {
            int[] intBuf = new int[2];
            intBuf[0] = inputStream.readInt();
            intBuf[1] = inputStream.readInt();
            McIDASUtil.flip(intBuf,0,1);
            width = intBuf[0];
            height = intBuf[1];
        } catch (Exception e) {
            System.out.println("getFrameData e=" + e);
            return ret;
        }

        if (!getTable(inputStream, stretchTable)) return ret;
        if (!getTable(inputStream, colorTable)) return ret;
        if (!getTable(inputStream, graphicsTable)) return ret;

        int havebytes=0;
        int needbytes=width*height;
        byte[] img = new byte[needbytes];
        int count = 0;
        int num = 0;
        int indx = 0;
        try {
            while (needbytes > 0) {
                num = inputStream.read(img, indx, needbytes);
                indx += num;
                havebytes += num;
                needbytes -= num;
                count++;
                Misc.sleep(10);
                if (count > 100) return ret;
            }
        } catch (Exception e) {
            System.out.println("FrameSubs getFrameData e=" + e);
            return ret;
        } 
        this.myImg = img;
        ret = 0;
        return ret;
    }


    public byte[] getImg(int frame) {
        return this.myImg;
    }


    private int getIntVal(byte[] buf, int offset) {
        int[] intBuf = new int[1];
        int value = 0;
        for (int i=0; i< 4; i++) {
            int shift = (4 - 1 -i) * 8;
            value += (buf[i+offset] & 0x000000FF) << shift;
        }
        intBuf[0] = value;
        McIDASUtil.flip(intBuf,0,0);
        return intBuf[0];
    }


    private boolean getTable(DataInputStream inputStream, int[] tbl) {
        try {
            for (int i=0; i<256; i++) {
                tbl[i] = inputStream.readInt();
            }
        } catch (Exception e) {
            System.out.println("getTable e=" + e);
            return false;
        }
        McIDASUtil.flip(tbl,0,255);
        return true;
    }

 
    public void getColorTables(int frame, int[] stretchtab, 
                            int[] colortab) {
        stretchtab = stretchTable;
        colortab = colorTable;
/*
        for (int i=0; i<256; i++) {
            System.out.println(i + ": " + stretchtab[i] + "    " + colortab[i]);
        }
*/
    }


    public int getGraphicsSize(int frame) {
/*
      if (uc == null) {
        try {
          getUC();
        } catch (Exception e) {
            System.out.println("FrameSubs getGraphicsSize: Unable to access UC");
            return -1;
        }
      }
      try {
        if (frame < 0) frame = getCurrentFrame();
      } catch (Exception e) {
          System.out.println("FrameSubs getGraphicsSize: Unable to get current frame number");
          return -1;
      }
      int graphicsFrame[] = { 0 };
      graphicsFrame[0] = uc.get(10000 + frame);
      McIDASUtil.flip(graphicsFrame,0,0);

      int npts = 0;

      String graphicsFile = System.getProperty("MCVTEMP");
      graphicsFile = graphicsFile.concat("MCGMEM");
      graphicsFile = graphicsFile.concat(System.getProperty("MCVNUM"));
      //System.out.println("FrameSubs getGraphicsSize: graphicsFile=" + graphicsFile);

      RandomAccessFile fRand;
      try {
        fRand = new RandomAccessFile(graphicsFile, "r");
      } catch (Exception e) {
        System.out.println("FrameSubs getGraphicsSize: File not found " + graphicsFile);
        return npts;
      }

      long length=0;
      try {
        length = fRand.length();
      } catch (Exception e) {
        System.out.println("FrameSubs getGraphicsSize: IO Exception");
        return npts;
      }
      //System.out.println("FrameSubs getGraphicsSize: length=" + length);

      FileChannel fChan;
      fChan = fRand.getChannel();

      MappedByteBuffer graBuf;
      try {
        graBuf = fChan.map(FileChannel.MapMode.READ_ONLY, 0, length);
      } catch (Exception e) {
        System.out.println("FrameSubs getGraphicsSize: IO Exception");
        return npts;
      }

      IntBuffer gb  = graBuf.asIntBuffer();

      int jpt[] = { 0 };
      //System.out.println("   graphicsFrame=" + graphicsFrame[0]);
      jpt[0] = uc.get(8000 + graphicsFrame[0]);
      McIDASUtil.flip(jpt,0,0);
      int j = jpt[0];
      while (j != -1) {
        //System.out.println("   j=" + j);
        j *= blockSize;
        jpt[0] = gb.get(j);
        McIDASUtil.flip(jpt,0,0);
        //System.out.println("   NumberOfPoints=" + jpt[0]);
        npts += jpt[0];
        jpt[0] = gb.get(j+blockSize-1);
        McIDASUtil.flip(jpt,0,0);
        //System.out.println("   NextBlock=" + jpt[0]);
        //System.out.println(" ");
        j = jpt[0];
      }

      //System.out.println("FrameSubs getGraphicsSize: npts=" + npts);
*/
       int npts = 0;
      return npts;
    }

    public int getGraphics(int frame, int npts, int[] graphics ) throws Exception {
/*
      int istat = 0;
      int ptcount = 0;

      if (uc == null) { 
        try {
          getUC();
        } catch (Exception e) {
            System.out.println("FrameSubs getGraphics: Unable to access UC");
            return -1;
        }
      }
      try {
        if (frame < 0) frame = getCurrentFrame();
      } catch (Exception e) { 
          System.out.println("FrameSubs getGraphics: Unable to get current frame number");
          return -1;
      }

      for (int i=0; i<npts; i++)
        graphics[i]=0;

      String graphicsFile = System.getProperty("MCVTEMP");
      graphicsFile = graphicsFile.concat("MCGMEM");
      graphicsFile = graphicsFile.concat(System.getProperty("MCVNUM"));
      //System.out.println("FrameSubs getGraphics: graphicsFile=" + graphicsFile);

      RandomAccessFile fRand;
      try {
        fRand = new RandomAccessFile(graphicsFile, "r");
      } catch (Exception e) {
        System.out.println("FrameSubs getGraphics: File not found " + graphicsFile);
        return npts;
      }

      long length=0;
      try {
        length = fRand.length();
      } catch (Exception e) {
        System.out.println("FrameSubs getGraphicsSize: IO Exception");
        return npts;
      }

      FileChannel fChan;
      fChan = fRand.getChannel();

      MappedByteBuffer graBuf;
      try {
        graBuf = fChan.map(FileChannel.MapMode.READ_ONLY, 0, length);
      } catch (Exception e) {
        System.out.println("FrameSubs getGraphicsSize: IO Exception");
        return npts;
      }

      IntBuffer gb  = graBuf.asIntBuffer();

      int graphicsFrame[] = { 0 };
      graphicsFrame[0] = uc.get(10000 + frame);
      McIDASUtil.flip(graphicsFrame,0,0);
      //System.out.println("  graphicsFrame=" + graphicsFrame[0]); 
      int jpt[] = { 0 };
      jpt[0] = uc.get(8000 + graphicsFrame[0]);
      McIDASUtil.flip(jpt,0,0);
      int j = jpt[0];
      while (j != -1) {
        //System.out.println("  j=" + j);
        int ipt[] = { 0 };
        j *= blockSize;
        ipt[0] = gb.get(j);
        McIDASUtil.flip(ipt,0,0);
        //System.out.println("   NumberOfPoints=" + ipt[0]);
        for (int i=1; i!=ipt[0]; i++) {
          graphics[ptcount] = gb.get(j+i);
          ptcount ++;
        }
        jpt[0] = gb.get(j+blockSize-1);
        McIDASUtil.flip(jpt,0,0);
        j = jpt[0];
        //System.out.println("   NextBlock=" + j);
      }
      //System.out.println("FrameSubs getGraphics: ptcount=" + ptcount);
      if (ptcount > npts) istat = -1;
      if (ptcount > 0)
        McIDASUtil.flip(graphics,0,ptcount-1);
*/
      int istat = 0;
      return istat;
    }

    public String fileName(int frame) {
      int frm=frame;
      if (frm < 1) frm=getCurrentFrame();

      Integer frmInt = new Integer(frm);
      String fn = "Frame" + frmInt.toString() + ".0";
      
      return fn;
    }


    public DataInputStream makeDataInputStream(int crfrm) {
      DataInputStream ret = null;

      if (crfrm < 0) crfrm=getCurrentFrame();

      String fName = fileName(crfrm);

      FileInputStream fis;
      try {
        fis = new FileInputStream(fName);
      } catch (Exception e) {
        System.out.println("FrameSubs makeDataInputStream:  file not found " + fName);
        return ret;
      }

      ret = new DataInputStream( new BufferedInputStream(fis));
      return ret;
    } 

  
    public int[] getFrameDir(int frame) {
        //System.out.println("FrameSubs getFrameDir:");
        if (frame < 0) frame = getCurrentFrame();
        if (this.fileRequest == null) {
            getRequestStrings();
        }

        String newRequest = this.fileRequest + fileName(frame);
        //System.out.println("   newRequest=" + newRequest);
        inputStream = getInputStream(newRequest);

        int dirLength = 64;
        int navLength = 640;
        int[] frmdir = new int[dirLength+navLength];

        int havebytes=0;
        int needbytes=dirLength+navLength;

        try {
            while (havebytes<needbytes) {
                frmdir[havebytes] = inputStream.readInt();
                havebytes++;
            }
        } catch (Exception e) {
            System.out.println("FrameSubs getFrameDir e=" + e);
            return frmdir;
        } 

        //System.out.println("   Flipping...");
        McIDASUtil.flip(frmdir,0,frmdir.length-1);
        //System.out.println("   ...Flipped");
        this.myFrmdir = frmdir;
      
        try {
            //System.out.println("   Closing inputStream...");
            inputStream.close();
            //System.out.println("   ...Closed");
        } catch (Exception ee) {
            System.out.println("   Error closing  ee=" + ee);
        }
        return frmdir;
    }
}
