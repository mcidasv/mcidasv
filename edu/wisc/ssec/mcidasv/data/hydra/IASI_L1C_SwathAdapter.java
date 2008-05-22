package edu.wisc.ssec.mcidasv.data.hydra;

import visad.Data;
import visad.FlatField;
import visad.Set;
import visad.CoordinateSystem;
import visad.RealType;
import visad.RealTupleType;
import visad.SetType;
import visad.Linear2DSet;
import visad.Unit;
import visad.FunctionType;
import visad.VisADException;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.HashMap;


public class IASI_L1C_SwathAdapter extends SwathAdapter {

   public IASI_L1C_SwathAdapter() {
   }

   public IASI_L1C_SwathAdapter(MultiDimensionReader reader, HashMap metadata) {
     super(reader, metadata);
   }

   protected void setLengths() {
     int len = getTrackLength();
     setTrackLength(len *= 2);
     len = getXTrackLength();
     setXTrackLength( len /= 2);
   }

   public FlatField getData(Object subset) throws Exception {
     Set domainSet = makeDomain(subset);

     HashMap new_subset = (HashMap) ((HashMap)subset).clone();
     new_subset.putAll((HashMap)subset);

     double[] coords = (double[]) new_subset.get(SwathAdapter.track_name);
     double[] new_coords = new double[] {0.0, coords[1]/2, 1.0};

     new_subset.put(SwathAdapter.track_name, new_coords);
     new_coords = new double[] {0.0, 119.0, 1.0};
     new_subset.put(SwathAdapter.xtrack_name, new_coords);

     return makeFlatField(domainSet, new_subset);
   }
}
