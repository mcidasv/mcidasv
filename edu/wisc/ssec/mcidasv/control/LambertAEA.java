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

package edu.wisc.ssec.mcidasv.control;

import visad.georef.MapProjection;
import visad.data.hdfeos.LambertAzimuthalEqualArea;
import visad.RealTupleType;
import visad.CoordinateSystem;
import visad.Data;
import visad.SI;
import visad.Unit;
import java.awt.geom.Rectangle2D;
import visad.VisADException;
import java.rmi.RemoteException;


public class LambertAEA extends MapProjection {

   CoordinateSystem cs;
   Rectangle2D rect;
   float earthRadius = 6367470; //- meters

   public LambertAEA(float[][] corners, float lonCenter, float latCenter) throws VisADException {
      super(RealTupleType.SpatialEarth2DTuple, new Unit[] {SI.meter, SI.meter});

      cs = new LambertAzimuthalEqualArea(RealTupleType.SpatialEarth2DTuple, earthRadius,
                   lonCenter*Data.DEGREES_TO_RADIANS, latCenter*Data.DEGREES_TO_RADIANS,
                         0,0);

     float[][] xy = cs.fromReference(corners);

     float min_x = Float.MAX_VALUE;
     float min_y = Float.MAX_VALUE;
     float max_x = -Float.MAX_VALUE;
     float max_y = -Float.MAX_VALUE;

     for (int k=0; k<xy[0].length;k++) {
       if (xy[0][k] < min_x) min_x = xy[0][k];
       if (xy[1][k] < min_y) min_y = xy[1][k];
       if (xy[0][k] > max_x) max_x = xy[0][k];
       if (xy[1][k] > max_y) max_y = xy[1][k];
     }

     float del_x = max_x - min_x;
     float del_y = max_y - min_y;

     boolean forceSquareMapArea = true;
     if (forceSquareMapArea) {
       if (del_x < del_y) {
          del_x = del_y;
       }
       else if (del_y < del_x) {
          del_y = del_x;
       }
     }

     rect = new Rectangle2D.Float(-del_x/2, -del_y/2, del_x, del_y);
   }

   public LambertAEA(float[][] corners) throws VisADException {
     super(RealTupleType.SpatialEarth2DTuple, new Unit[] {SI.meter, SI.meter});

     boolean spanGM = false;
     float lonA = corners[0][0];
     float lonB = corners[0][1];
     float lonC = corners[0][2];
     float lonD = corners[0][3];
     float latA = corners[1][0];
     float latB = corners[1][1];
     float latC = corners[1][2];
     float latD = corners[1][3];

     float diffAD = lonA - lonD;
     //float diffBC = lonB - lonC;

     if (Math.abs(diffAD) > 180) spanGM = true;
     //if (Math.abs(diffBC) > 180) spanGM = true;

     float lonCenter;

     if (spanGM) {
        float[] vals = MultiSpectralControl.minmax(new float[] {lonA, lonD});
        float wLon = vals[1];
        float eLon = vals[0];
        float del = 360f - wLon + eLon;
        lonCenter = wLon + del/2;
        if (lonCenter > 360) lonCenter -= 360f;
     }
     else {
        float[] vals = MultiSpectralControl.minmax(new float[] {lonA, lonD});
        float minLon = vals[0];
        float maxLon = vals[1];
        lonCenter = minLon + (maxLon - minLon)/2;
     }

     float[] vals = MultiSpectralControl.minmax(corners[1]);
     float minLat = vals[0];
     float maxLat = vals[1];
     float latCenter = minLat + (maxLat - minLat)/2;

     cs = new LambertAzimuthalEqualArea(RealTupleType.SpatialEarth2DTuple, earthRadius,
                   lonCenter*Data.DEGREES_TO_RADIANS, latCenter*Data.DEGREES_TO_RADIANS,
                         0,0);

     float[][] xy = cs.fromReference(corners);

     float min_x = Float.MAX_VALUE;
     float min_y = Float.MAX_VALUE;
     float max_x = Float.MIN_VALUE;
     float max_y = Float.MIN_VALUE;

     for (int k=0; k<xy[0].length;k++) {
       if (xy[0][k] < min_x) min_x = xy[0][k];
       if (xy[1][k] < min_y) min_y = xy[1][k];
       if (xy[0][k] > max_x) max_x = xy[0][k];
       if (xy[1][k] > max_y) max_y = xy[1][k];
     }

     float del_x = max_x - min_x;
     float del_y = max_y - min_y;

     boolean forceSquareMapArea = true;
     if (forceSquareMapArea) {
       if (del_x < del_y) {
          del_x = del_y;
       }
       else if (del_y < del_x) {
          del_y = del_x;
       }
     }

     min_x = -del_x/2;
     min_y = -del_y/2;

     rect = new Rectangle2D.Float(min_x, min_y, del_x, del_y);
   }

   public LambertAEA(Rectangle2D ll_rect) throws VisADException {
     this(ll_rect, true);
   }

   public LambertAEA(Rectangle2D ll_rect, boolean forceSquareMapArea) throws VisADException {
     super(RealTupleType.SpatialEarth2DTuple, new Unit[] {SI.meter, SI.meter});

     float minLon = (float) ll_rect.getX();
     float minLat = (float) ll_rect.getY();
     float del_lon = (float) ll_rect.getWidth();
     float del_lat = (float) ll_rect.getHeight();
     float maxLon = minLon + del_lon;
     float maxLat = minLat + del_lat;

     float lonDiff = maxLon - minLon;
     float lonCenter = minLon + (maxLon - minLon)/2;
     if (lonDiff > 180f) {
       lonCenter += 180f;
     }
     float latCenter = minLat + (maxLat - minLat)/2;

     cs = new LambertAzimuthalEqualArea(RealTupleType.SpatialEarth2DTuple, earthRadius,
                   lonCenter*Data.DEGREES_TO_RADIANS, latCenter*Data.DEGREES_TO_RADIANS,
                         0,0);

     float[][] xy = cs.fromReference(new float[][] {{minLon,maxLon,minLon,maxLon}, 
                                                    {minLat,minLat,maxLat,maxLat}});


     float min_x = Float.MAX_VALUE;
     float min_y = Float.MAX_VALUE;
     float max_x = Float.MIN_VALUE;
     float max_y = Float.MIN_VALUE;

     for (int k=0; k<xy[0].length;k++) {
       if (xy[0][k] < min_x) min_x = xy[0][k];
       if (xy[1][k] < min_y) min_y = xy[1][k];
       if (xy[0][k] > max_x) max_x = xy[0][k];
       if (xy[1][k] > max_y) max_y = xy[1][k];
     }

     float del_x = max_x - min_x;
     float del_y = max_y - min_y;
 
     if (forceSquareMapArea) {
       if (del_x < del_y) {
         del_x = del_y;
       }
       else if (del_y < del_x) {
         del_y = del_x;
       }
     }

     min_x = -del_x/2;
     min_y = -del_y/2;
  
     rect = new Rectangle2D.Float(min_x, min_y, del_x, del_y);
   }

   public Rectangle2D getDefaultMapArea() {
     return rect;
   }
     
   public float[][] toReference(float[][] values) throws VisADException {
     return cs.toReference(values);
   }

   public float[][] fromReference(float[][] values) throws VisADException {
     return cs.fromReference(values);
   }

   public double[][] toReference(double[][] values) throws VisADException {
     return cs.toReference(values);
   }

   public double[][] fromReference(double[][] values) throws VisADException {
     return cs.fromReference(values);
   }

   public boolean equals(Object cs) {
     if ( cs instanceof LambertAEA ) {
        LambertAEA that = (LambertAEA) cs;
        if ( (this.cs.equals(that.cs)) && this.getDefaultMapArea().equals(that.getDefaultMapArea())) {
           return true;
        }
     }
     return false;
   }

}
