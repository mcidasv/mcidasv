package ucar.unidata.data.imagery.mcidas;

import edu.wisc.ssec.mcidas.AREAnav;
import edu.wisc.ssec.mcidas.McIDASUtil;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import ucar.unidata.util.Misc;

public class FrameSubs {

    int myFrameNo;
    String myRequest;
    String ucRequest;
    String fileRequest;
    String dataRequest;
    String graphicsRequest;

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
        //this.graphicsRequest = this.myRequest + "N&text=";
        this.graphicsRequest = this.myRequest + "P&text=";
    }


    private DataInputStream getInputStream(String newRequest) {
        DataInputStream retStream = null;
        try {
            url = new URL(newRequest);
            urlc = url.openConnection();
            InputStream is = urlc.getInputStream();
            retStream = new DataInputStream( new BufferedInputStream(is));
        } catch (Exception e) {
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
            System.out.println("FrameSubs getInts: offset=" + offset + ":exception e=" + e);
            return numRead;
        }
        try {
            inputStream.close();
        } catch (Exception ee) {
        }
        McIDASUtil.flip(buf,0,numRead-1);
        return numRead;
    }


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
            System.out.println("FrameSubs getFrameData: e=" + e);
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
            System.out.println("FrameSubs getFrameData: e=" + e);
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
            System.out.println("FrameSubs getTable: e=" + e);
            return false;
        }
        McIDASUtil.flip(tbl,0,255);
        return true;
    }

 
    public void getColorTables(int frame, int[] stretchtab, 
                            int[] colortab) {
        stretchtab = stretchTable;
        colortab = colorTable;
    }


    public List getGraphics(int frame) {
        List graphics = new ArrayList();
        String segment = " "; 
        if (frame < 0) frame = getCurrentFrame();
        if (this.graphicsRequest == null) {
           getRequestStrings(); 
        }

        String newRequest = this.graphicsRequest + frame;
        //System.out.println(newRequest);
        inputStream = getInputStream(newRequest);
        String lineOut = null;

        int lineCount = 0;
        try {
            lineOut = inputStream.readLine();
            //System.out.println(lineOut);
            lineCount++;
            lineOut = inputStream.readLine();
            //System.out.println(lineOut);
            lineCount++;
        } catch (Exception e) {
            //System.out.println("FrameSubs getGraphics 1: exception=" + e);
            try {
                inputStream.close();
            } catch (Exception ee) {
            }
            return graphics;
        }
        String next;
        while (lineOut != null) {
            //if (lineCount < 10) System.out.println(lineOut);
            graphics.add(lineOut);
            try {
                lineOut = inputStream.readLine();
                lineCount++;
            } catch (Exception e) {
                //System.out.println("FrameSubs getGraphics 2: exception=" + e);
                try {
                    inputStream.close();
                } catch (Exception ee) {
                }
                return graphics;
            }
        }
        return graphics;
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
        if (frame < 0) frame = getCurrentFrame();
        if (this.fileRequest == null) {
            getRequestStrings();
        }

        String newRequest = this.fileRequest + fileName(frame);
        inputStream = getInputStream(newRequest);

        int dirLength = 65;
        int[] dir = getInts(inputStream, dirLength);

        int navLength;
        if (dir[64] == AREAnav.LALO) {
          navLength = 127;
        } else {
          navLength = 639;
        }
        int[] nav = getInts(inputStream, navLength);

        int auxLength = 0;
        int rows = 0;
        int cols = 0;
        int begLat= 0;
        int begLon =0;
        if (navLength == 128) {
            rows = nav[65];
            cols = nav[66];
            begLat = nav[78]/4;
            begLon = nav[79]/4;
            auxLength = begLon + (rows*cols);
        }
        int[] aux = getInts(inputStream,auxLength);

        int[] frmdir = new int[dirLength + navLength + auxLength];
        System.arraycopy(dir, 0, frmdir, 0, dirLength);
        System.arraycopy(nav, 0, frmdir, dirLength, navLength);
        if (auxLength > 0)
            System.arraycopy(aux, 0, frmdir, dirLength+navLength, auxLength);

        this.myFrmdir = frmdir;
      
        try {
            inputStream.close();
        } catch (Exception ee) {
            System.out.println("FrameSubs getFrameDir: Error closing  ee=" + ee);
        }
        return frmdir;
    }


    private int[] getInts(DataInputStream stream, int count) {
        int[] buf = new int[count];
        if (count < 1) return buf;

        int havebytes=0;
        int needbytes=count;

        try {
            while (havebytes<needbytes) {
                buf[havebytes] = inputStream.readInt();
                havebytes++;
            }
        } catch (Exception e) {
            System.out.println("FrameSubs getInts: e=" + e);
            return buf;
        }

        McIDASUtil.flip(buf,0,count-1);
        return buf;
    }
}
