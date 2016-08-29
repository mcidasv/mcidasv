package edu.wisc.ssec.mcidasv.control.adt;

import java.io.*;
import java.lang.String;

@SuppressWarnings("unused")

public class ADT_Topo {

   public ADT_Topo() {
   }

   public static int ReadTopoFile(String topofile, double inputlat, double inputlon) throws IOException {

      int    num_lon_elev=5760;
      int    num_lat_elev=2880;
      double first_lon_elev=0.0;
      double first_lat_elev=90.0;
      double last_lon_elev=360.0;
      double last_lat_elev=-90.0;

      // XXX TJJ just to keep moving, we'll fix this later
      boolean test = true;
      if (test) return 2;
      
      int topoflag = 0;
      double ax = inputlat;
      double bx = inputlon;  /* to make mcidas compliant */
      System.out.printf("TOPO: lat: %f  lon: %f\n",ax,bx);
      // File file = new File(topofile);
      // RandomAccessFile filestream = new RandomAccessFile(file,"r");
      InputStream filestream = ADT_Topo.class.getResourceAsStream(topofile);
      DataInputStream dis = new DataInputStream(filestream);
      
      // RandomAccessFile filestream = new RandomAccessFile(file,"r");
      // filestream.seek(0);

      double del_lon_elev = (last_lon_elev-first_lon_elev)/num_lon_elev;
      double del_lat_elev = (first_lat_elev-last_lat_elev)/num_lat_elev;
      System.out.printf("TOPO: dlat: %f  dlon: %f\n",del_lon_elev,del_lat_elev);
     
      int ay = (int)((90.0-ax)/del_lat_elev);
      if(bx<0.0) bx = bx+360.0;
      int by = (int)(bx/del_lon_elev);
      System.out.printf("TOPO: lat: %d  lon: %d \n",ay,by);
      long position = (long)(2*((ay*((double)num_lon_elev))+by));
      System.out.printf("TOPO: position=%d\n",position);
      // filestream.seek(position+1);
      dis.skip(position);
      System.err.println("After skip, about to read val...");

      int i = dis.readShort();
      System.err.println("After read, val: " + i);
      /** int i = filestream.readUnsignedByte(); */
      int ichar = (i==0) ? 2 : 1;
      System.err.println("After read, returning: " + ichar);
      System.out.printf("TOPO: position=%d Value=%d landflag=%d \n ",position,i,ichar);
      filestream.close();

      return ichar; 
   }

}
