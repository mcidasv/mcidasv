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

   public LambertAEA(Rectangle2D ll_rect) throws VisADException {
     super(RealTupleType.SpatialEarth2DTuple, new Unit[] {SI.meter, SI.meter});

     float minLon = (float) ll_rect.getX();
     float minLat = (float) ll_rect.getY();
     float del_lon = (float) ll_rect.getWidth();
     float del_lat = (float) ll_rect.getHeight();
     float maxLon = minLon + del_lon;
     float maxLat = minLat + del_lat;


     float earthRadius = 6367470; //- meters

     float lonDiff = maxLon - minLon;
     float lonCenter = minLon + (maxLon - minLon)/2;
     if (lonDiff > 180f) {
       lonCenter += 180f;
     }
     float latCenter = minLat + (maxLat - minLat)/2;

     cs = new LambertAzimuthalEqualArea(getReference(), earthRadius,
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
 
     if (del_x < del_y) del_x = del_y;
     if (del_y < del_x) del_y = del_x;
  
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
     return false;
   }

}
