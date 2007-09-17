package edu.wisc.ssec.mcidasv.data.hydra;

import visad.CoordinateSystem;
import visad.GridCoordinateSystem;
import visad.VisADException;
import visad.RealTupleType;
import visad.Linear2DSet;
import visad.Gridded2DSet;
import visad.Linear1DSet;
import visad.Unit;
import visad.Set;


public class LongitudeLatitudeCoordinateSystem extends CoordinateSystem {

   float offset_0;
   float offset_1;

   Linear2DSet domainSet;
   Linear2DSet subSet;
   Gridded2DSet gset;

   public LongitudeLatitudeCoordinateSystem(Linear2DSet domainSet, Gridded2DSet gset ) throws VisADException {
     super(RealTupleType.SpatialEarth2DTuple, null);
     this.gset = gset;
     Linear1DSet set = domainSet.getLinear1DComponent(0);
     offset_0 = (float)set.getFirst();
     set = domainSet.getLinear1DComponent(1);
     offset_1 = (float)set.getFirst();
     int[] lengths = domainSet.getLengths();
     int[] gset_lengths = gset.getLengths();
     subSet = new Linear2DSet(0.0, gset_lengths[0], lengths[0],
                              0.0, gset_lengths[1], lengths[1]);
   }

   public float[][] toReference(float[][] values) throws VisADException {
     for (int k=0; k<values[0].length; k++) {
       values[0][k] -= offset_0;
       values[1][k] -= offset_1;
     }
     float[][] coords = subSet.gridToValue(values);
     coords = gset.gridToValue(coords);
     return coords;
   }

   public float[][] fromReference(float[][] values) throws VisADException {
     float[][] grid_vals = gset.valueToGrid(values);
     float[][] coords = subSet.valueToGrid(grid_vals);
     for (int k=0; k<coords[0].length; k++) {
       coords[0][k] += offset_0;
       coords[1][k] += offset_1;
     }
     return coords;
   }

   public double[][] toReference(double[][] values) throws VisADException {
     for (int k=0; k<values[0].length; k++) {
       values[0][k] -= offset_0;
       values[1][k] -= offset_1;
     }

     float[][] coords = subSet.gridToValue(Set.doubleToFloat(values));
     coords = gset.gridToValue(coords);
     return Set.floatToDouble(coords);
   }

   public double[][] fromReference(double[][] values) throws VisADException {
     float[][] grid_vals = gset.valueToGrid(Set.doubleToFloat(values));
     float[][] coords = subSet.valueToGrid(grid_vals);

     for (int k=0; k<coords[0].length; k++) {
       coords[0][k] += offset_0;
       coords[1][k] += offset_1;
     }
     return Set.floatToDouble(coords);
   }

   public boolean equals(Object cs) {
     return (cs instanceof LongitudeLatitudeCoordinateSystem);
   }
}
