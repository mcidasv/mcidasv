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

//
// GrabLineRendererJ3D.java
//

package edu.wisc.ssec.mcidasv.data.hydra;

import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Vector;

import visad.CommonUnit;
import visad.Data;
import visad.DataDisplayLink;
import visad.DataReference;
import visad.Display;
import visad.DisplayImpl;
import visad.DisplayRealType;
import visad.DisplayTupleType;
import visad.Gridded1DSet;
import visad.MathType;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.ScalarMap;
import visad.ShadowRealType;
import visad.ShadowType;
import visad.Unit;
import visad.VisADException;
import visad.VisADRay;

/**
 Grab and drag lines parallel to a coordinate axis.  For simple
 2D graphs, not yet generalized for 3D displays.  For a 
 vertical line, map Real to Display.XAxis, and assign 
 ConstantMap for Display.YAxis.  Vice-a-versa for a horizontal
 line.
 */

public class GrabLineRendererJ3D extends visad.java3d.DirectManipulationRendererJ3D {

  private float[][] spatialValues = null;

  private int closeIndex = -1;

  private float offsetx = 0.0f, offsety = 0.0f, offsetz = 0.0f;
  private int offset_count = 0;
  private static final int OFFSET_COUNT_INIT = 30;

  private transient DataDisplayLink link = null;
  private transient DataReference ref = null;
  private transient MathType type = null;
  private transient ShadowType shadow = null;

  private float point_x, point_y, point_z;
  private float line_x, line_y, line_z;

  private float[] f = new float[1];
  private float[] d = new float[1];

  private String notRealType = "not RealType";
  private String whyNotDirect = null;

  private boolean pickCrawlToCursor = true;

  private int[] axisToComponent = {-1, -1, -1};
  private ScalarMap[] directMap = {null, null, null};

  private DisplayImpl display = null;
  private DisplayTupleType tuple = null;
  private boolean stop = false;

  private Gridded1DSet domainSet = null;
  private int last_idx = -1;
  private float[][] samples = null;

  private int mouseModifiersMask  = 0;
  private int mouseModifiersValue = 0;

  public GrabLineRendererJ3D() {
    this(null);
  }

  public GrabLineRendererJ3D(Gridded1DSet domainSet) {
    super();
    this.domainSet = domainSet;
    try {
      if (domainSet != null) samples = domainSet.getSamples();
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  public String getWhyNotDirect() {
    return whyNotDirect;
  }

  public synchronized void setSpatialValues(float[][] spatial_values) {
    spatialValues = spatial_values;
  }

  public void checkDirect() throws VisADException, RemoteException {
    setIsDirectManipulation(false);

    display = getDisplay();
    link = getLinks()[0];
    ref = link.getDataReference();
    shadow = link.getShadow().getAdaptedShadowType();
    type = link.getType();
    if (!(type instanceof RealType)) {
      whyNotDirect = notRealType;
      return;
    }

    tuple = ((ShadowRealType) shadow).getDisplaySpatialTuple();

    //-ShadowRealType[] components = shadow.getRealComponents();
    ShadowRealType[] components = {(ShadowRealType)shadow};

    for (int i=0; i<components.length; i++) {
      Enumeration maps = components[i].getSelectedMapVector().elements();
      while (maps.hasMoreElements()) {
        ScalarMap map = (ScalarMap) maps.nextElement();
        DisplayRealType dreal = map.getDisplayScalar();
        DisplayTupleType tuple = dreal.getTuple();
        if (tuple != null &&
            (tuple.equals(Display.DisplaySpatialCartesianTuple) ||
             (tuple.getCoordinateSystem() != null &&
              tuple.getCoordinateSystem().getReference().equals(
              Display.DisplaySpatialCartesianTuple)))) {
          int index = dreal.getTupleIndex();
          axisToComponent[index] = i;
          directMap[index] = map;
        }
      } // end while (maps.hasMoreElements())
    }

    setIsDirectManipulation(true);
  }


  public synchronized float checkClose(double[] origin, double[] direction) 
  {
    int mouseModifiers = getLastMouseModifiers();
    if ((mouseModifiers & mouseModifiersMask) != mouseModifiersValue) {
      return Float.MAX_VALUE;
    }

    float distance = Float.MAX_VALUE;
    if (display == null) return distance;
    if (spatialValues == null) return distance;
    float o_x = (float) origin[0];
    float o_y = (float) origin[1];
    float o_z = (float) origin[2];
    float d_x = (float) direction[0];
    float d_y = (float) direction[1];
    float d_z = (float) direction[2];
/*
System.out.println("origin = " + o_x + " " + o_y + " " + o_z);
System.out.println("direction = " + d_x + " " + d_y + " " + d_z);
*/

    for (int i=0; i<spatialValues[0].length; i++) {
      float x = spatialValues[0][i] - o_x;
      float y = spatialValues[1][i] - o_y;
      float z = spatialValues[2][i] - o_z;
      float dot = x * d_x + y * d_y + z * d_z;
      x = x - dot * d_x;
      y = y - dot * d_y;
      z = z - dot * d_z;
      float d = (float) Math.sqrt(x * x + y * y + z * z);
      if (d < distance) {
        distance = d;
        closeIndex = i;
        offsetx = x;
        offsety = y;
        offsetz = z;
      }
/*
System.out.println("spatialValues["+i+"] = " + spatialValues[0][i] + " " +
spatialValues[1][i] + " " + spatialValues[2][i] + " d = " + d);
*/
    }

    float dist1D = Float.MAX_VALUE;
    if (axisToComponent[0] != -1) dist1D = offsetx;
    if (axisToComponent[1] != -1) dist1D = offsety;
    if (axisToComponent[2] != -1) dist1D = offsetz;
    return Math.abs(dist1D);
  }

  public synchronized void drag_direct(VisADRay ray, boolean first,
                                       int mouseModifiers) {
    if (display == null) return;
    
    // disable printing of the cursor info string
    getDisplayRenderer().setCursorStringOn(false);
    
    // System.out.println("drag_direct " + first + " " + type);
    if (spatialValues == null || ref == null || shadow == null ||
        link == null) return;

    if (first) {
      stop = false;
    }
    else {
      if (stop) return;
    }

    float o_x = (float) ray.position[0];
    float o_y = (float) ray.position[1];
    float o_z = (float) ray.position[2];
    float d_x = (float) ray.vector[0];
    float d_y = (float) ray.vector[1];
    float d_z = (float) ray.vector[2];

    if (pickCrawlToCursor) {
      if (first) {
        offset_count = OFFSET_COUNT_INIT;
      }
      else {
        if (offset_count > 0) offset_count--;
      }
      if (offset_count > 0) {
        float mult = ((float) offset_count) / ((float) OFFSET_COUNT_INIT);
        o_x += mult * offsetx;
        o_y += mult * offsety;
        o_z += mult * offsetz;
      }
    }

    if (first) {
      point_x = spatialValues[0][closeIndex];
      point_y = spatialValues[1][closeIndex];
      point_z = spatialValues[2][closeIndex];
      int lineAxis = -1;
      for (int i=0; i<3; i++) {
        if (getAxisToComponent(i) >= 0) {
          lineAxis = i;
        }
      }
      line_x = (lineAxis == 0) ? 1.0f : 0.0f;
      line_y = (lineAxis == 1) ? 1.0f : 0.0f;
      line_z = (lineAxis == 2) ? 1.0f : 0.0f;
    }
    float[] x = new float[3];

    // find closest point on line to ray
    // logic from vis5d/cursor.c
    // line o_, d_ to line point_, line_
    float ld = d_x * line_x + d_y * line_y + d_z * line_z;
    float od = o_x * d_x + o_y * d_y + o_z * d_z;
    float pd = point_x * d_x + point_y * d_y + point_z * d_z;
    float ol = o_x * line_x + o_y * line_y + o_z * line_z;
    float pl = point_x * line_x + point_y * line_y + point_z * line_z;
    if (ld * ld == 1.0f) return;
    float t = ((pl - ol) - (ld * (pd - od))) / (ld * ld - 1.0f);
    // x is closest point
    x[0] = point_x + t * line_x;
    x[1] = point_y + t * line_y;
    x[2] = point_z + t * line_z;

    try {
      float[] xx = {x[0], x[1], x[2]};
      if (tuple != null) {
        /*- TDR ??
        float[][] cursor = {{x[0]}, {x[1]}, {x[2]}};
        float[][] new_cursor =
          tuple.getCoordinateSystem().fromReference(cursor);
        x[0] = new_cursor[0][0];
        x[1] = new_cursor[1][0];
        x[2] = new_cursor[2][0];
        */
      }
      Data newData = null;
      Data data;
      try {
        data = link.getData();
      } catch (RemoteException re) {
        if (visad.collab.CollabUtil.isDisconnectException(re)) {
          getDisplay().connectionFailed(this, link);
          removeLink(link);
          link = null;
          return;
        }
        throw re;
      }
      int ii = -1;
      RealType rtype = null;
      if (type instanceof RealType) {
        if (domainSet == null) addPoint(xx);
        for (int i=0; i<3; i++) {
          if (getAxisToComponent(i) >= 0) {
            ii = i;
            f[0] = x[i];
            d = getDirectMap(i).inverseScaleValues(f);
            // RealType rtype = (RealType) data.getType();
            rtype = (RealType) type;
            newData = new Real(rtype, (double) d[0], rtype.getDefaultUnit(), null);
            break;
          }
        }
        if (domainSet != null) {
          int[] idx = domainSet.valueToIndex(new float[][] {d});
          if (idx[0] != last_idx && idx[0] >= 0) {
            newData = new Real(rtype, (double)samples[0][idx[0]], rtype.getDefaultUnit(), null);

            // create location string
            Vector<String> vect = new Vector<String>();
            //-Real r = new Real(rtype, d[0]);
            Real r = new Real(rtype, samples[0][idx[0]]);
            Unit overrideUnit = getDirectMap(ii).getOverrideUnit();
            Unit rtunit = rtype.getDefaultUnit();
            // units not part of Time string
            if (overrideUnit != null && !overrideUnit.equals(rtunit) &&
                (!Unit.canConvert(rtunit, CommonUnit.secondsSinceTheEpoch) ||
                 rtunit.getAbsoluteUnit().equals(rtunit))) {
              double dval =  overrideUnit.toThis((double) d[0], rtunit);
              r = new Real(rtype, dval, overrideUnit);
            }
            String valueString = r.toValueString();
            vect.addElement(rtype.getName() + " = " + valueString);
            getDisplayRenderer().setCursorStringVector(vect);

            ref.setData(newData);
            link.clearData();
            last_idx = idx[0];
          }
        }
        else {
          ref.setData(newData);
          link.clearData();
        }
      }
      else if (type instanceof RealTupleType) {
        addPoint(xx);
        int n = ((RealTuple) data).getDimension();
        Real[] reals = new Real[n];
        Vector<String> vect = new Vector<String>();
        for (int i=0; i<3; i++) {
          int j = getAxisToComponent(i);
          if (j >= 0) {
            f[0] = x[i];
            d = getDirectMap(i).inverseScaleValues(f);
            Real c = (Real) ((RealTuple) data).getComponent(j);
            rtype = (RealType) c.getType();
            reals[j] = new Real(rtype, (double) d[0], rtype.getDefaultUnit(), null);
          }
        }
        getDisplayRenderer().setCursorStringVector(vect);
        for (int j=0; j<n; j++) {
          if (reals[j] == null) {
            reals[j] = (Real) ((RealTuple) data).getComponent(j);
          }
        }
        newData = new RealTuple((RealTupleType) type, reals,
                                ((RealTuple) data).getCoordinateSystem());
        //ref.setData(newData);
        //link.clearData();

        if (domainSet != null) {
          int[] idx = domainSet.valueToIndex(new float[][] {d});
          if (idx[0] != last_idx && idx[0] >= 0) {
            newData = new Real(rtype, (double)samples[0][idx[0]], rtype.getDefaultUnit(), null);
                                                                                                                  
            // create location string
            vect = new Vector<String>();
            //-Real r = new Real(rtype, d[0]);
            Real r = new Real(rtype, samples[0][idx[0]]);
            Unit overrideUnit = getDirectMap(ii).getOverrideUnit();
            Unit rtunit = rtype.getDefaultUnit();
            // units not part of Time string
            if (overrideUnit != null && !overrideUnit.equals(rtunit) &&
                (!Unit.canConvert(rtunit, CommonUnit.secondsSinceTheEpoch) ||
                 rtunit.getAbsoluteUnit().equals(rtunit))) {
              double dval =  overrideUnit.toThis((double) d[0], rtunit);
              r = new Real(rtype, dval, overrideUnit);
            }
            String valueString = r.toValueString();
            vect.addElement(rtype.getName() + " = " + valueString);
            getDisplayRenderer().setCursorStringVector(vect);
            
            ref.setData(newData);
            link.clearData();
            last_idx = idx[0];
          }
        }
        else {
          ref.setData(newData);
          link.clearData();
        }

      }

   }
   catch (VisADException e) {
     System.out.println("drag_direct " + e);
     e.printStackTrace();
   }
   catch (RemoteException e) {
     System.out.println("drag_direct " + e);
     e.printStackTrace();
   }

  }

  private int getAxisToComponent(int i) {
    return axisToComponent[i];
  }

  private ScalarMap getDirectMap(int i) {
    return directMap[i];
  }

  public void stop_direct() {
    stop = true;
  }
}
