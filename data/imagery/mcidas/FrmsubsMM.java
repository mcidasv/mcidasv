package ucar.unidata.data.imagery.mcidas;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import edu.wisc.ssec.mcidas.McIDASUtil;

/**
 * Class FrmsubsMM contains the memory-mapped version
 * of routines in frmsubs.c
 */
public class FrmsubsMM {

    static MappedByteBuffer frmBuf;
    static IntBuffer uc;
    static int lastFrame = 0;
    static int blockSize = 256;

    /**
     * Constructor
     */
    public FrmsubsMM() throws Exception {
      getMemoryMappedUC();
    }

    public void getMemoryMappedUC() {
      //System.out.println("FrmsubsMM getMemoryMappedUC:");
      RandomAccessFile fRand;
      String fName = System.getProperty("MCVTEMP");
      //System.out.println("   MCVTEMP=" + fName);
      fName = fName.concat("MCIMEM");
      //System.out.println("   fName=" + fName);
      fName = fName.concat(System.getProperty("MCVNUM"));
      //System.out.println("   fName=" + fName);
      try {
        fRand = new RandomAccessFile(fName, "rw");
      } catch (Exception e) {
        System.out.println("FrmsubsMM getMemoryMappedUC: File not found " + fName);
        return;
      }

      long length=0;
      try {
        length = fRand.length();
      } catch (Exception e) {
        System.out.println("FrmsubsMM getMemoryMappedUC: length IOException");
      }
      //System.out.println("   length=" + length);

      FileChannel fChan;
      fChan = fRand.getChannel();

      try {
        frmBuf = fChan.map(FileChannel.MapMode.READ_WRITE, 0, length);
      } catch (Exception e) {
        System.out.println("FrmsubsMM getMemoryMappedUC: IO Exception");
        return;
      }

      uc = frmBuf.asIntBuffer();
      return;
    }

    public int getNumberOfFrames() {
      int numfrm[] = { 0 };
      if (uc == null) {
        try {
          getMemoryMappedUC();
        } catch (Exception e) {
            System.out.println("FrmsubsMM getNumberOfFrames: File not found");
            return -1;
        }
      }
      numfrm[0] = uc.get(13);
      McIDASUtil.flip(numfrm,0,0);
      return numfrm[0];
    }

    public int getCurrentFrame() {
      //System.out.println("FrmsubsMM getCurrentFrame:");
      int curfrm[] = { 0 };
      if (uc == null) {
        try {
          //System.out.println("   calling getMemoryMappedUC...");
          getMemoryMappedUC();
        } catch (Exception e) {
          System.out.println("FrmsubsMM getCurrentFrame: File not found");
          return -1;
        }
      }

      curfrm[0] = uc.get(51);
      McIDASUtil.flip(curfrm,0,0);
      return curfrm[0];
    }

    public int getFrameSize( int frame ) {
      int istat = 0;
      if (uc == null) {
        try {
          getMemoryMappedUC();
        } catch (Exception e) {
            System.out.println("FrmsubsMM getFrameSize: File not found");
            return -1;
        }
      }
      try {
        if (frame < 0) frame = getCurrentFrame();
      } catch (Exception e) {
          System.out.println("FrmsubsMM getFrameSize: File not found");
          return -1;
      }
        
      int size[] = { 0 };
      size[0] = uc.get(3000+frame);
      McIDASUtil.flip(size,0,0);
      return size[0];
    }

    public int getLineSize(int frame ) {
      int linsize = 0;
      int size = getFrameSize(frame);
      linsize = size%65536;
      return linsize;
    }

    public int getEleSize(int frame ) {
      int elesize = 0;
      int size = getFrameSize(frame);
      elesize = size/65536;
      return elesize;
    }

    public int getDirtyFlag(int frame) {
      int istat = -1;
      int dirty;
      if (uc == null) {
        try {
          getMemoryMappedUC();
        } catch (Exception e) {
            System.out.println("FrmsubsMM getDirtyFlag: File not found");
            return -1;
        }
      }
      if (frame < 0) frame = getCurrentFrame();
      int dindex = getFrameStart(frame)+5;
      int dflag[] = { 0 };
      dflag[0] = uc.get(dindex);
      McIDASUtil.flip(dflag,0,0);
      dirty = dflag[0];

      if (frame != lastFrame) dirty = -1;

      if (dirty != 0) {
        lastFrame = frame;
        try {
          uc = uc.put(dindex, 0);
        } catch (Exception e) {
          System.out.println("   frmBuf.put error");
          return dirty;
        }
      }
      return dirty;
    }

    public int getFrameStart(int frame) {
      int start[] = { 0 };
      start[0] = uc.get(2000+frame);
      McIDASUtil.flip(start,0,0);
      return start[0]/4;
    }

    public int getFrameData(boolean infoData, boolean infoEnh, 
                            int frame,
                            byte[] img, int[] stretchtab, 
                            int[] colortab, int[] graphicstab) {
      int istat = -1;
      int dirty = -1;
      if (uc == null) {
        try {
          getMemoryMappedUC();
        } catch (Exception e) {
            System.out.println("FrmsubsMM getFrameData: File not found");
            return -1;
        }
      }
      try {
        if (frame < 0) frame = getCurrentFrame();
      } catch (Exception e) {
          System.out.println("FrmsubsMM getFrameData: File not found");
          return -1;
      }

      int [] frmdir = new int[64];
      getFrameDirectory( frame, frmdir);
      int lMag = frmdir[19];
      int eMag = frmdir[20];
      if (lMag < 0) lMag = 1;
      if (eMag < 0) eMag = 1;

      int dindex = getFrameStart(frame);

      int length = 256;
      if (infoEnh) {
        int cindex = dindex + 10;
        for (int i=0; i<length; i++) {
          stretchtab[i] = uc.get(cindex + i);
          colortab[i] = uc.get(cindex+length + i);
          graphicstab[i] = uc.get(cindex+(2*length) + i);
        }
        McIDASUtil.flip(stretchtab,0,length-1);
        McIDASUtil.flip(colortab,0,length-1);
        McIDASUtil.flip(graphicstab,0,length-1);
      }

      if (infoData) {
        dindex *= 4;
        dindex += 4*((length*3)+11);
        int cap = frmBuf.capacity();
        int linsize = getLineSize(frame);
        int elesize = getEleSize(frame);
        int ixx = linsize*elesize;
        if ((dindex+ixx) > cap) ixx = cap - dindex;
        int ioff = 0;
        int indx = 0;
        for (int i=12; i<linsize; i+=lMag) {
          for (int j=0; j<elesize; j+=eMag) {
            ioff = i*elesize + j;
            try {
              img[indx] = frmBuf.get(dindex+ioff);
              indx++;
            } catch (IndexOutOfBoundsException e) {
              System.out.println("  frmBuf=" + frmBuf);
              System.out.println("  dindex=" + dindex);
              System.out.println("  linsize=" + linsize + " elesize=" + elesize);
              System.out.println("  *** IndexOutOfBoundsException thrown ***");
            }
          }
        }
      }
      istat = 0;

      return istat;
    }

    public int getGraphicsSize(int frame) {
      if (uc == null) {
        try {
          getMemoryMappedUC();
        } catch (Exception e) {
            System.out.println("FrmsubsMM getGraphicsSize: File not found");
            return -1;
        }
      }
      try {
        if (frame < 0) frame = getCurrentFrame();
      } catch (Exception e) {
          System.out.println("FrmsubsMM getGraphicsSize: File not found");
          return -1;
      }
      int graphicsFrame[] = { 0 };
      graphicsFrame[0] = uc.get(10000 + frame);
      McIDASUtil.flip(graphicsFrame,0,0);

      int npts = 0;

      String graphicsFile = System.getProperty("MCVTEMP");
      graphicsFile = graphicsFile.concat("MCGMEM");
      graphicsFile = graphicsFile.concat(System.getProperty("MCVNUM"));
      //System.out.println("FrmsubsMM getGraphicsSize: graphicsFile=" + graphicsFile);

      RandomAccessFile fRand;
      try {
        fRand = new RandomAccessFile(graphicsFile, "r");
      } catch (Exception e) {
        System.out.println("FrmsubsMM getGraphicsSize: File not found " + graphicsFile);
        return npts;
      }

      long length=0;
      try {
        length = fRand.length();
      } catch (Exception e) {
        System.out.println("FrmsubsMM getGraphicsSize: IO Exception");
        return npts;
      }
      //System.out.println("FrmsubsMM getGraphicsSize: length=" + length);

      FileChannel fChan;
      fChan = fRand.getChannel();

      MappedByteBuffer graBuf;
      try {
        graBuf = fChan.map(FileChannel.MapMode.READ_ONLY, 0, length);
      } catch (Exception e) {
        System.out.println("FrmsubsMM getGraphicsSize: IO Exception");
        return npts;
      }

      IntBuffer gb  = graBuf.asIntBuffer();

      int jpt[] = { 0 };
      //System.out.println("   graphicsFrame[0]=" + graphicsFrame[0]);
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

      //System.out.println("FrmsubsMM getGraphicsSize: npts=" + npts);
      return npts;
    }

    public int getGraphics(int frame, int npts, int[] graphics ) throws Exception {
      int istat = 0;
      int ptcount = 0;
      //System.out.println("FrmsubsMM getGraphics:  npts=" + npts);

      if (uc == null) { 
        try {
          getMemoryMappedUC();
        } catch (Exception e) {
            System.out.println("FrmsubsMM getGraphics: File not found");
            return -1;
        }
      }
      try {
        if (frame < 0) frame = getCurrentFrame();
      } catch (Exception e) { 
          System.out.println("FrmsubsMM getGraphics: File not found");
          return -1;
      }

      for (int i=0; i<npts; i++)
        graphics[i]=0;

      String graphicsFile = System.getProperty("MCVTEMP");
      graphicsFile = graphicsFile.concat("MCGMEM");
      graphicsFile = graphicsFile.concat(System.getProperty("MCVNUM"));
      //System.out.println("graphicsFile=" + graphicsFile);

      RandomAccessFile fRand;
      try {
        fRand = new RandomAccessFile(graphicsFile, "r");
      } catch (Exception e) {
        System.out.println("FrmsubsMM getGraphicsSize: File not found " + graphicsFile);
        return npts;
      }

      long length=0;
      try {
        length = fRand.length();
      } catch (Exception e) {
        System.out.println("FrmsubsMM getGraphicsSize: IO Exception");
        return npts;
      }

      FileChannel fChan;
      fChan = fRand.getChannel();

      MappedByteBuffer graBuf;
      try {
        graBuf = fChan.map(FileChannel.MapMode.READ_ONLY, 0, length);
      } catch (Exception e) {
        System.out.println("FrmsubsMM getGraphicsSize: IO Exception");
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
      //System.out.println("FrmsubsMM getGraphics: ptcount=" + ptcount);
      if (ptcount > npts) istat = -1;
      if (ptcount > 0)
        McIDASUtil.flip(graphics,0,ptcount-1);

      return istat;
    }

    public String fileName(int frame) {
      int frm=frame;
      if (frm < 1) frm=getCurrentFrame();

      String sub = System.getProperty("MCVPATH");
      Integer frmInt = new Integer(frm);
      String fn = sub.concat("Frame" + frmInt.toString() + ".0");
      //System.out.println("FrmsubsMM fileName:  fn=" + fn);
      
      return fn;
    }
      
    public int getFrameDirectory(int crfrm, int[] frmdir) {
      int ret=-1;

      if (crfrm < 0) crfrm=getCurrentFrame();
      //System.out.println("FrmsubsMM getFrameDirectory: crfrm=" + crfrm);

      String fName = fileName(crfrm);
      //System.out.println("FrmsubsMM getFrameDirectory: fName=" + fName);

      DataInputStream dis;
      try {
        dis = new DataInputStream (
          new BufferedInputStream(new FileInputStream(fName))
        );
      } catch (Exception e) {
        System.out.println("FrmsubsMM getFrameDirectory: Can't open " + fName);
        return ret;
      }

      for (int i=0; i<frmdir.length; i++) {
        try {
          frmdir[i] = dis.readInt();
        } catch(Exception e) {
          System.out.println("FrmsubsMM getFrameDirectory: Can't read " + fName);
          return ret;
        }
      }
      McIDASUtil.flip(frmdir,0,frmdir.length-1);

      ret = 0;
      return ret;
    }
}
