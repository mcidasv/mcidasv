/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */
package edu.wisc.ssec.mcidasv.util;

import java.io.*;
import java.lang.Math;
import java.lang.String;
import java.lang.*;
import java.rmi.RemoteException;

import visad.*;

public class Interpolation {

   static int icnt,jcnt;

   static double ang = Math.PI/2.0;

   static double[][] array = new double[20][20];
   static double fi;
   static double fj;
   static double aa;
   static double aaa;
   static double aer;
   static double ae1 = 0.0;
   static double ae2 = 0.0;

   static int    ijij = 0;

   public static double biquad(double[][] ain, int nx, int ny, double x, double y) {

      double MISSING = 9.9E31;
      double BADVAL = 1.0E20;
      double OutQuadInterp;

      double[] dm = new double[4];
      double[] a = new double[nx*ny];
      double dst;
      double dst2;
      double dst3;
      double y0 = MISSING;
      double y1 = MISSING;
      double y2 = MISSING;
      double y3 = MISSING;
      double y12 = MISSING;
      double y03 = MISSING;
      double y123 = MISSING;

      int inx;
      int iny;
      int nxny;
      int iz;
      int imx;
      int ibad;
      int ix,iy;
      int derive03;

      for (ix=0;ix<nx;ix++) {
         for (iy=0;iy<ny;iy++) {
            a[(ix*nx)+iy] = ain[ix][iy];
         }
      }

      if (x<0.0) x = x + 0.0000001;
      if (y<0.0) y = y + 0.0000001;
      if (x>(float)(nx-1)) x = x - 0.0000001;
      if (y>(float)(ny-1)) y = y - 0.0000001;
      inx = (int)(Math.floor(x));
      iny = (int)(Math.floor(y));
//      nxny = nx*(iny-2) + inx - 1;
      nxny = nx*(iny-1) + inx -1;
      dst = (x+1000.0)%1.0;
      dst2 = dst*dst;
      dst3 = dst*dst2;
      iny--;
      for (iz=0;iz<4;iz++) {
      y0 = MISSING;
      y1 = MISSING;
      y2 = MISSING;
      y3 = MISSING;
//      System.out.println("nxny="+nxny+" inx="+inx+" iny="+iny);
         ibad = 1;
//      System.out.println("iz="+iz);
         if ((iny<0)||(iny>=ny)) {
//      System.out.println("bad");
            ibad = -1;
         }
         else {
//      System.out.println("here");
            imx = inx;
//      System.out.println("imx="+imx);
            if ((imx>=0)&&(imx<nx)) y1 = a[nxny+1];
            imx++;
//      System.out.println("imx="+imx);
            if ((imx>=0)&&(imx<nx)) y2 = a[nxny+2];
//      System.out.println("y1="+y1+" y2="+y2);
            if ((y1>1.0e29)&&(y2>1.0e29)) {
               ibad = -1;
            } 
            else {
               imx = inx - 1;
//      System.out.println("imx="+imx);
               if ((imx>=0)&&(imx<nx)&&(a[nxny]<BADVAL)) y0 = a[nxny];
               imx = imx + 3;
//      System.out.println("imx="+imx);
               if ((imx>=0)&&(imx<nx)&&(a[nxny+3]<BADVAL)) y3 = a[nxny+3];
//      System.out.println("y0="+y0+" y3="+y3);
               if (y1>BADVAL) {
                  if ((y2>BADVAL)||(y3>BADVAL)) {
                     ibad = -1;
                  }
                  else {
                     y1 = y2 + y2 -y3;
                  }
               }
               if ((y2>BADVAL)&&(ibad==1)) {
                  if ((y0>BADVAL)||(y1>BADVAL)) {
                     ibad = -1;
                  }
                  else {
                     y2 = y1 + y1 -y0;
                  }
               }
               if (ibad==1) {
                  if (y0>BADVAL) y0 = y1 + y1 - y2;
                  if (y3>BADVAL) y3 = y2 + y2 - y1;
                  y12 = y1 - y2;
                  y03 = y0 - y3;
                  y123 = y12 + y12 + y12;
               }
            }
         }
         if (ibad==-1) {
            dm[iz] = MISSING;
         } 
         else {
//        System.out.println("dst="+dst+" dst2="+dst2+" dst3="+dst3);
//        System.out.println("y0="+y0+" y1="+y1+" y2="+y2+" y3="+y3);
//        System.out.println("y03="+y03+" y12="+y12+" y123="+y123);
            dm[iz] = (dst3*(y123-y03)) + (dst2*(y0+y03-y1-y123-y12)) + (dst*(y2-y0)) + y1 + y1;
         }
         iny++;
         nxny = nxny+nx;
      }
      ibad = 1;

//      System.out.println("dm0= "+dm[0]+" dm1= "+dm[1]+" dm2= "+dm[2]+" dm3= "+dm[3]);
      if ((dm[1]>BADVAL)&&(dm[2]>BADVAL)) {
         // bad 1 and 2 values -- cannot interpolate
         derive03 = -1;
      }
      else {
         if (dm[1]<BADVAL) {
            // 1 is good, check 2
            if (dm[2]<BADVAL) {
               // 2 is good, can derive 0 and 3, if necessary
               derive03 = 1;
            }
            else {
               // 2 is bad, check 0 to see if 2 can be estimated
               if (dm[0]<BADVAL) {
                  // 0 is good, estimate 2 and derive 3, if necessary
                  dm[2] = dm[1] + dm[1] - dm[0];
                  derive03 = 1;
               } 
               else {
                  // 0 is bad, cannot estimate 2 -- cannot interpolate
                  derive03 = -1;
               }
            }
         }
         else {
            // 1 is bad, but 2 is good, check 3 to see if 1 can be estimated
            if (dm[3]<BADVAL) {
               // 3 is good, estimate 1 and derive 0, if necessary
               dm[1] = dm[2] + dm[2] - dm[3];
               derive03 = 1;
            }
            else {
               // 3 is bad, cannot estimate 1 -- cannot interpolate
               derive03 = -1;
            } 
         }
      }
      if (derive03==1) {
         // values 1 and 2 are good, will derive 0 and 3, if necessary 
         if(dm[0]>BADVAL) dm[0] = dm[1] + dm[1] - dm[2];
         if(dm[3]>BADVAL) dm[3] = dm[2] + dm[2] - dm[1];
         dst = (y+1000.0)%1.0;
         dst2 = dst*dst;
         dst3 = dst*dst2;
         y12 = dm[1] - dm[2];
         y03 = dm[0] - dm[3];
         y123 = y12 + y12 + y12;
         OutQuadInterp = 0.25*(dst3*(y123-y03) + (dst2*(dm[0]+y03-dm[1]-y123-y12)) + (dst*(dm[2]-dm[0])) + dm[1] + dm[1]);
      }
      else {
         // cannot interpolate, return missing value
         OutQuadInterp = MISSING;        
      }

      return OutQuadInterp;
   }

   public static FlatField biquad(FlatField fromFld, FlatField toFld) throws VisADException, RemoteException {
     Gridded2DSet fromSet = (Gridded2DSet) fromFld.getDomainSet();
     int[] lens = fromSet.getLengths();
     double[][] fromVals = fromFld.getValues(false);
     Set toSet = toFld.getDomainSet();

     
     float[][] coords = transform(toSet, fromSet);
     coords = ((GriddedSet)toSet).valueToGrid(coords);
   
     double[] newVals = biquad(fromVals, lens[1], lens[0], Set.floatToDouble(coords));
     FlatField newFld = new FlatField((FunctionType) toFld.getType(), toSet);
     newFld.setSamples(new double[][] {newVals});

     return newFld;
   }

   public static double[] biquad(double[][] a, int nx, int ny, double[][] xy) {
     int numPts = xy[0].length;
     double[] interpVals = new double[numPts];
     for (int k=0; k<numPts; k++) {
       interpVals[k] = biquad(a, nx, ny, xy[0][k], xy[1][k]);
     }
     return interpVals;
   }

   public static double bilinear(double[][] a, int nx, int ny, double x, double y) {

      double MISSING = 9.9E31;
      double OutBiInterp;
      double dx;
      double dy;
      double vy1;
      double vy2;

      int nx1;
      int nx2;
      int ny1;
      int ny2;

      if (((x<=0.0)||(x>=(float)(nx)))||((y<=0.0)||(y>=(float)(ny)))) {
         OutBiInterp = MISSING;
      }
      else {
//         nx1 = Math.max((int)(Math.floor(x-1)),0);
//         nx2 = nx1 + 1;
//         ny1 = Math.max((int)(Math.floor(y-1)),0);
//         ny2 = ny1 + 1;
//         dx = x - (float)(nx1) - 1;   // added -1
//         dy = y - (float)(ny1) - 1;   // added -1
         nx1 = Math.max((int)(Math.floor(x)),0);
         nx2 = nx1 + 1;
         ny1 = Math.max((int)(Math.floor(y)),0);
         ny2 = ny1 + 1;
         dx = x - (float)(nx1);   // added -1
         dy = y - (float)(ny1);   // added -1
//         System.out.println("a11="+a[nx1][ny1]+" a21="+a[nx2][ny1]);
//         System.out.println("a22="+a[nx2][ny2]+" a12="+a[nx2][ny1]);
         vy1 = a[nx1][ny1] + dx*(a[nx2][ny1] - a[nx1][ny1]);
         vy2 = a[nx1][ny2] + dx*(a[nx2][ny2] - a[nx1][ny2]);
//         System.out.println("vy1="+vy1+" vy2="+vy2);
         OutBiInterp = vy1 + dy*(vy2 - vy1);
      }

      return OutBiInterp;
   }


   public static float[][] transform(Set fromSet, Set toSet) throws VisADException, RemoteException {
     CoordinateSystem fromCS = fromSet.getCoordinateSystem();
     CoordinateSystem toCS = toSet.getCoordinateSystem();
     Unit[] fromUnits = fromSet.getSetUnits();
     Unit[] toUnits = toSet.getSetUnits();

     int[] wedge = fromSet.getWedge();
     float[][] values = fromSet.indexToValue(wedge);

     values = CoordinateSystem.transformCoordinates(
                     ((SetType) fromSet.getType()).getDomain(), fromCS, fromUnits, null,
                     ((SetType) toSet.getType()).getDomain(), toCS, toUnits, null,
                     values);

     return values;

     //values = ((GriddedSet)toSet).valueToGrid(values);
   }

   public static void main(String[] args) throws IOException {

      for (icnt=0;icnt<20;icnt++) {
         for (jcnt=0;jcnt<20;jcnt++) {
            array[icnt][jcnt] = Math.sin(ang*((float)(icnt)/19.0)) + Math.sin(ang*((float)(jcnt)/19.0));
//            System.out.println("icnt="+icnt+" jcnt="+jcnt+" Array: "+array[icnt][jcnt]);
         }
      }

      for (icnt=5;icnt<=189;icnt=icnt+19) {
         for (jcnt=9;jcnt<=189;jcnt=jcnt+18) {
            fi = (double)(icnt) * 0.1;
            fj = (double)(jcnt) * 0.1;
            aa = (fi * fi) + (fj + fj);
            aa = Math.sin(ang*((fi)/19.0)) + Math.sin(ang*((fj)/19.0));
            aaa = bilinear(array,20,20,fi,fj);
            aer = (aa - aaa)/aa;
            ae1 = ae1 + Math.abs(aer);
            System.out.println("Line: "+icnt+" "+jcnt+" "+aa+" "+aaa+" "+aer);
            aaa = biquad(array,20,20,fi,fj);
            aer = (aa - aaa)/aa;
            ae2 = ae2 + Math.abs(aer);
            System.out.println("Quad: "+icnt+" "+jcnt+" "+aa+" "+aaa+" "+aer);
            ijij++;
         }
      }
      ae1 = ae1/(float)(ijij);   
      ae2 = ae2/(float)(ijij);   
      System.out.println("ae1,ae2="+ae1+" "+ae2);

   }
}
