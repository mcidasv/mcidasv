//
// ShadowType.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2019 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

package visad;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.ArrayList;
import visad.data.DataCacheManager;

import visad.util.HersheyFont;

/**
 * The ShadowType hierarchy shadows the MathType hierarchy, within a
 * DataDisplayLink.
 * <P>
 */
public abstract class ShadowType extends Object implements java.io.Serializable {

  /** possible values for LevelOfDifficulty */
  public static final int NOTHING_MAPPED = 6;
  public static final int SIMPLE_TUPLE = 5;
  public static final int SIMPLE_ANIMATE_FIELD = 4;
  public static final int SIMPLE_FIELD = 3;
  public static final int NESTED = 2;
  public static final int LEGAL = 1;

  /**  Image ByReference flags **/
  public static final String PROP_IMAGE_BY_REF = "visad.java3d.imageByRef";

  /** Property to create a texture for a single value color filled contour */
  public static final String PROP_CONTOURFILL_SINGLE_VALUE_AS_TEXTURE = "visad.contourFillSingleValueAsTexture";
  public static final boolean byReference;
  public static final boolean yUp;
  static {
    byReference = Boolean.parseBoolean(System.getProperty(PROP_IMAGE_BY_REF, "false"));
    if (byReference) {
      yUp = true;
    } else {
      yUp = false;
    }
    //System.err.println("IMAGE_BY_REF:" + byReference);
  }


  /** basic information about this ShadowType */
  MathType Type; // MathType being shadowed
  transient DataDisplayLink Link;
  transient DisplayImpl display;
  transient private Data data; // from Link.getData()
  private ShadowType Parent;

  /** information calculated by constructors */
  /**
   * count of occurences of DisplayRealType-s ShadowScalarType: set for mappings
   * to DisplayRealType-s ShadowTupleType (incl ShadowRealTupleType): set to sum
   * for ShadowScalarType & ShadowRealTupleType components ShadowRealTupleType:
   * add contribution from Reference
   */
  int[] DisplayIndices;
  /**
   * ValueIndices is like DisplayIndices, but distinguishes different
   * ScalarMap-s of non-Single DisplayRealTypes
   */
  int[] ValueIndices;
  /**
   * MultipleSpatialDisplayScalar is true if any RealType component is mapped to
   * multiple spatial DisplayRealType-s
   */
  boolean MultipleSpatialDisplayScalar;
  /**
   * MultipleDisplayScalar is true if any RealType component is mapped to
   * multiple DisplayRealType-s, or if any RealTupleType component and its
   * Reference are both mapped
   */
  boolean MultipleDisplayScalar;
  /**
   * MappedDisplayScalar is true if any RealType component is mapped to any
   * DisplayRealType-s, including via a RealTupleType.Reference
   */
  boolean MappedDisplayScalar;

  /** information calculated by checkIndices & testIndices */
  boolean isTerminal;
  int LevelOfDifficulty;
  boolean isTextureMap;
  boolean curvedTexture;
  boolean isTexture3D;
  boolean isLinearContour3D;
  boolean adjustProjectionSeam;

  /**
   * Dtype and Rtype used only with ShadowSetType and Flat ShadowFunctionType
   */
  int Dtype; // Domain Type: D0, D1, D2, D3, D4 or Dbad
  int Rtype; // Range Type: R0, R1, R2, R3, R4 or Rbad
  /** possible values for Dtype */
  static final int D0 = 0; // (Unmapped)*
  static final int D1 = 1; // allSpatial + (SpatialOffset, IsoContour, Flow,
  // Text, Shape, ShapeScale, Color, Alpha, Range,
  // Unmapped)*
  static final int D2 = 2; // (SpatialOffset, Spatial, Color, Alpha,
  // Range, Unmapped)*
  static final int D3 = 3; // (Color, Alpha, Range, Unmapped)*
  static final int D4 = 4; // (Animation, Value)*
  static final int Dbad = 5;
  /** possible values for Rtype */
  static final int R0 = 0; // (Unmapped)*
  static final int R1 = 1; // (Color, Alpha, Range, Unmapped)*
  static final int R2 = 2; // (Spatial, SpatialOffset, Color, Alpha,
  // Range, Unmapped)*
  static final int R3 = 3; // (IsoContour, Flow, Text, Shape, ShapeScale Color,
  // Alpha, Range, Unmapped)*
  static final int R4 = 4; // (Spatial, SpatialOffset, IsoContour, Flow,
  // Text, Shape, ShapeScale, Color, Alpha, Range,
  // Unmapped)*
  static final int Rbad = 5;

  /** spatial DisplayTupleType at terminal nodes */
  DisplayTupleType spatialTuple = null;
  /** number of spatial DisplayRealType components at terminal nodes */
  int spatialDimension;
  /** flags for any IsoContour or Flow at terminal nodes */
  boolean anyContour;
  boolean anyFlow;
  boolean anyShape;
  boolean anyText;

  /** streamline flags */
  boolean streamline1;
  boolean streamline2;
  float streamlineDensity1;
  float streamlineDensity2;
  float arrowScale1;
  float arrowScale2;
  float stepFactor1;
  float stepFactor2;
  float packingFactor1;
  float packingFactor2;
  float cntrWeight1;
  float cntrWeight2;
  int n_pass1;
  int n_pass2;
  float reduction1;
  float reduction2;
  // ---------------------

  /** trajectory flags */
  public boolean trajectory1 = false;
  public boolean trajectory2 = false;
  protected ArrayList<FlowInfo> flowInfoList = new ArrayList<FlowInfo>();
  public boolean cacheTrajFlow1 = false;
  public boolean cacheTrajFlow2 = false;

  /** makeContour, manifoldDimension == 2 */
  int[] cnt = { 0 };
  ProjectionControl p_cntrl = null;
  ContourControl c_cntrl = null;
  // -------------------------------------

  /**
   * makeContour, manifoldDimension == 3. Needed for case of missing final
   * spatial coords
   */
  float[][] spatial_offset_values = null;

  /**
   * used by getComponents to record RealTupleTypes with coordinate transforms
   */
  int[] refToComponent;
  ShadowRealTupleType[] componentWithRef;
  int[] componentIndex;

  public ShadowType(MathType type, DataDisplayLink link, ShadowType parent)
      throws VisADException, RemoteException {
    Type = type;
    Link = link;
    display = link.getDisplay();
    Parent = parent;
    data = link.getData();
    DisplayIndices = zeroIndices(display.getDisplayScalarCount());
    ValueIndices = zeroIndices(display.getValueArrayLength());
    isTerminal = false;
    isTextureMap = false;
    curvedTexture = false;
    isTexture3D = false;
    isLinearContour3D = false;
    adjustProjectionSeam = true;
    LevelOfDifficulty = NOTHING_MAPPED;
    MultipleSpatialDisplayScalar = false;
    MultipleDisplayScalar = false;
    MappedDisplayScalar = false;
    p_cntrl = display.getProjectionControl();
  }

  public DataDisplayLink getLink() {
    return Link;
  }

  public int getLevelOfDifficulty() {
    return LevelOfDifficulty;
  }

  public boolean getIsTerminal() {
    return isTerminal;
  }

  public boolean getIsTextureMap() {
    return isTextureMap;
  }

  public boolean getCurvedTexture() {
    return curvedTexture;
  }

  public boolean getIsTexture3D() {
    return isTexture3D;
  }

  public boolean getIsLinearContour3D() {
    return isLinearContour3D;
  }

  public int[] getRefToComponent() {
    return refToComponent;
  }

  public ShadowRealTupleType[] getComponentWithRef() {
    return componentWithRef;
  }

  public int[] getComponentIndex() {
    return componentIndex;
  }

  public boolean getAdjustProjectionSeam() {
    return adjustProjectionSeam;
  }

  public ShadowRealType[] getComponents(ShadowType type, boolean doRef)
      throws VisADException {
    if (type == null)
      return null;
    if (doRef) {
      refToComponent = null;
      componentWithRef = null;
      componentIndex = null;
    }
    ShadowRealType[] reals;
    if (type instanceof ShadowRealType) {
      ShadowRealType[] r = { (ShadowRealType) type };
      return r;
    } else if (type instanceof ShadowRealTupleType) {
      int n = ((ShadowRealTupleType) type).getDimension();
      reals = new ShadowRealType[n];
      for (int i = 0; i < n; i++) {
        reals[i] = (ShadowRealType) ((ShadowRealTupleType) type)
            .getComponent(i);
      }
      if (doRef) {
        ShadowRealTupleType ref = ((ShadowRealTupleType) type).getReference();
        if (ref != null && ref.getMappedDisplayScalar()) {
          refToComponent = new int[1];
          componentWithRef = new ShadowRealTupleType[1];
          componentIndex = new int[1];
          refToComponent[0] = 0;
          componentWithRef[0] = (ShadowRealTupleType) type;
          componentIndex[0] = 0;
        }
      }
    } else if (type instanceof ShadowTupleType) {
      int m = ((ShadowTupleType) type).getDimension();
      int n = 0;
      int nref = 0;
      for (int i = 0; i < m; i++) {
        ShadowType component = ((ShadowTupleType) type).getComponent(i);
        if (component instanceof ShadowRealType) {
          n++;
        } else if (component instanceof ShadowRealTupleType) {
          n += getComponents(component, false).length;
          if (doRef) {
            ShadowRealTupleType ref = ((ShadowRealTupleType) component)
                .getReference();
            if (ref != null && ref.getMappedDisplayScalar())
              nref++;
          }
        }
      }
      reals = new ShadowRealType[n];
      int j = 0;
      if (nref == 0)
        doRef = false;
      if (doRef) {
        refToComponent = new int[nref];
        componentWithRef = new ShadowRealTupleType[nref];
        componentIndex = new int[nref];
      }
      int rj = 0;
      for (int i = 0; i < m; i++) {
        ShadowType component = ((ShadowTupleType) type).getComponent(i);
        if (component instanceof ShadowRealType
            || component instanceof ShadowRealTupleType) {
          /*
           * WLH 17 April 99 ShadowRealType[] r = getComponents(component,
           * false); for (int k=0; k<r.length; k++) { reals[j] = r[k]; j++; }
           */
          if (doRef && component instanceof ShadowRealTupleType) {
            ShadowRealTupleType ref = ((ShadowRealTupleType) component)
                .getReference();
            if (ref != null && ref.getMappedDisplayScalar()) {
              refToComponent[rj] = j;
              componentWithRef[rj] = (ShadowRealTupleType) component;
              componentIndex[rj] = i;
              rj++;
            }
          }
          ShadowRealType[] r = getComponents(component, false);
          for (int k = 0; k < r.length; k++) {
            reals[j] = r[k];
            j++;
          }
        }
      }
    } else {
      reals = null;
    }
    return reals;
  }

  public Data getData() {
    return data;
  }

  public ShadowType getAdaptedShadowType() {
    return this;
  }

  /**
   * create a zero'd array of indices (for each RealType or each
   * DisplayRealType)
   */
  static int[] zeroIndices(int length) {
    int[] local_indices = new int[length];
    for (int i = 0; i < length; i++) {
      local_indices[i] = 0;
    }
    return local_indices;
  }

  /** copy an array of indices (for each RealType or each DisplayRealType) */
  static int[] copyIndices(int[] indices) {
    int[] local_indices = new int[indices.length];
    for (int i = 0; i < indices.length; i++) {
      local_indices[i] = indices[i];
    }
    return local_indices;
  }

  /** add arrays of indices (for each RealType or each DisplayRealType) */
  static int[] addIndices(int[] indices, int[] indices2) throws VisADException {
    if (indices.length != indices2.length) {
      throw new DisplayException("ShadowType.addIndices: lengths don't match");
    }
    int[] local_indices = new int[indices.length];
    for (int i = 0; i < indices.length; i++) {
      local_indices[i] = indices[i] + indices2[i];
    }
    return local_indices;
  }

  public boolean getAnyContour() {
    return anyContour;
  }

  public boolean getAnyFlow() {
    return anyFlow;
  }

  public boolean getAnyShape() {
    return anyShape;
  }

  public boolean getAnyText() {
    return anyText;
  }

  /**
   * test for display_indices in (Spatial, SpatialOffset, Color, Alpha,
   * Animation, Range, Value, Flow, Text, Unmapped)
   */
  boolean checkNested(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      DisplayTupleType tuple = real.getTuple();
      if (tuple != null
          && (tuple.equals(Display.DisplaySpatialCartesianTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplaySpatialCartesianTuple))))
        continue; // Spatial
      // SpatialOffset
      if (Display.DisplaySpatialOffsetTuple.equals(tuple))
        continue;
      if (tuple != null
          && (tuple.equals(Display.DisplayRGBTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayRGBTuple))))
        continue; // Color
      if (tuple != null
          && (tuple.equals(Display.DisplayFlow1Tuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayFlow1Tuple))))
        continue; // Flow1
      if (tuple != null
          && (tuple.equals(Display.DisplayFlow2Tuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayFlow2Tuple))))
        continue; // Flow2
      if (real.equals(Display.RGB) || real.equals(Display.RGBA)
          || real.equals(Display.HSV) || real.equals(Display.CMY))
        continue; // more Color
      if (real.equals(Display.Alpha) || real.equals(Display.Animation)
          || real.equals(Display.SelectValue)
          || real.equals(Display.SelectRange) || real.equals(Display.Shape)
          || real.equals(Display.ShapeScale) || real.equals(Display.Text))
        continue;
      return false;
    }
    return true;
  }

  /**
   * test for display_indices in (Spatial, SpatialOffset, IsoContour, Color,
   * Alpha, Flow, Text, Range, Unmapped)
   */
  boolean checkR4(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      DisplayTupleType tuple = real.getTuple();
      if (tuple != null
          && (tuple.equals(Display.DisplaySpatialCartesianTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplaySpatialCartesianTuple))))
        continue; // Spatial
      // SpatialOffset
      if (Display.DisplaySpatialOffsetTuple.equals(tuple))
        continue;
      if (tuple != null
          && (tuple.equals(Display.DisplayRGBTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayRGBTuple))))
        continue; // Color
      if (tuple != null
          && (tuple.equals(Display.DisplayFlow1Tuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayFlow1Tuple))))
        continue; // Flow1
      if (tuple != null
          && (tuple.equals(Display.DisplayFlow2Tuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayFlow2Tuple))))
        continue; // Flow2
      if (real.equals(Display.RGB) || real.equals(Display.RGBA)
          || real.equals(Display.HSV) || real.equals(Display.CMY))
        continue; // more Color
      if (real.equals(Display.Alpha) || real.equals(Display.SelectRange)
          || real.equals(Display.Shape) || real.equals(Display.ShapeScale)
          || real.equals(Display.Text) || real.equals(Display.IsoContour))
        continue;
      return false;
    }
    return true;
  }

  /**
   * test for display_indices in (IsoContour, Color, Alpha, Flow, Text, Range,
   * Unmapped)
   */
  boolean checkR3(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      DisplayTupleType tuple = real.getTuple();
      if (tuple != null
          && (tuple.equals(Display.DisplayRGBTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayRGBTuple))))
        continue; // Color
      if (tuple != null
          && (tuple.equals(Display.DisplayFlow1Tuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayFlow1Tuple))))
        continue; // Flow1
      if (tuple != null
          && (tuple.equals(Display.DisplayFlow2Tuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayFlow2Tuple))))
        continue; // Flow2
      if (real.equals(Display.RGB) || real.equals(Display.RGBA)
          || real.equals(Display.HSV) || real.equals(Display.CMY))
        continue; // more Color
      if (real.equals(Display.Alpha) || real.equals(Display.SelectRange)
          || real.equals(Display.Shape) || real.equals(Display.ShapeScale)
          || real.equals(Display.Text) || real.equals(Display.IsoContour))
        continue;
      return false;
    }
    return true;
  }

  /**
   * test for display_indices in (Color, Alpha, Range, Flow, Shape, ShapeScale,
   * Text, Unmapped)
   */
  boolean checkR1D3(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      DisplayTupleType tuple = real.getTuple();
      if (tuple != null
          && (tuple.equals(Display.DisplayRGBTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayRGBTuple))))
        continue; // Color
      if (tuple != null
          && (tuple.equals(Display.DisplayFlow1Tuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayFlow1Tuple))))
        continue; // Flow1
      if (tuple != null
          && (tuple.equals(Display.DisplayFlow2Tuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayFlow2Tuple))))
        continue; // Flow2
      if (real.equals(Display.RGB) || real.equals(Display.RGBA)
          || real.equals(Display.HSV) || real.equals(Display.CMY))
        continue; // more Color
      if (real.equals(Display.Alpha) || real.equals(Display.Shape)
          || real.equals(Display.ShapeScale) || real.equals(Display.Text)
          || real.equals(Display.SelectRange))
        continue;
      return false;
    }
    return true;
  }

  /** test for display_indices in (Color, Range, Unmapped) */
  boolean checkColorRange(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      DisplayTupleType tuple = real.getTuple();
      if (tuple != null
          && (tuple.equals(Display.DisplayRGBTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayRGBTuple))))
        continue; // Color
      if (real.equals(Display.RGB) || real.equals(Display.HSV)
          || real.equals(Display.CMY))
        continue; // more Color
      if (real.equals(Display.SelectRange))
        continue;
      return false;
    }
    return true;
  }

  /** test for display_indices in (Color, Alpha, Range, Unmapped) */
  boolean checkColorAlphaRange(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      DisplayTupleType tuple = real.getTuple();
      if (tuple != null
          && (tuple.equals(Display.DisplayRGBTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayRGBTuple))))
        continue; // Color
      if (real.equals(Display.RGB) || real.equals(Display.RGBA)
          || real.equals(Display.Alpha) || real.equals(Display.HSV)
          || real.equals(Display.CMY))
        continue; // more Color
      if (real.equals(Display.SelectRange))
        continue;
      return false;
    }
    return true;
  }

  /** test for display_indices in (IsoContour, Color, Alpha, Range, Unmapped) */
  boolean checkContourColorAlphaRange(int[] display_indices)
      throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      DisplayTupleType tuple = real.getTuple();
      if (real.equals(Display.IsoContour))
        continue;
      if (tuple != null
          && (tuple.equals(Display.DisplayRGBTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayRGBTuple))))
        continue; // Color
      if (real.equals(Display.RGB) || real.equals(Display.RGBA)
          || real.equals(Display.Alpha) || real.equals(Display.HSV)
          || real.equals(Display.CMY))
        continue; // more Color
      if (real.equals(Display.SelectRange))
        continue;
      return false;
    }
    return true;
  }

  /**
   * test for display_indices in (Spatial, SpatialOffset, Color, Alpha, Range,
   * Flow, Shape, ShapeScale, Text, Unmapped)
   */
  boolean checkR2D2(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      DisplayTupleType tuple = real.getTuple();
      if (tuple != null
          && (tuple.equals(Display.DisplaySpatialCartesianTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplaySpatialCartesianTuple))))
        continue; // Spatial
      // SpatialOffset
      if (Display.DisplaySpatialOffsetTuple.equals(tuple))
        continue;
      if (tuple != null
          && (tuple.equals(Display.DisplayRGBTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayRGBTuple))))
        continue; // Color
      if (tuple != null
          && (tuple.equals(Display.DisplayFlow1Tuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayFlow1Tuple))))
        continue; // Flow1
      if (tuple != null
          && (tuple.equals(Display.DisplayFlow2Tuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayFlow2Tuple))))
        continue; // Flow2
      if (real.equals(Display.RGB) || real.equals(Display.RGBA)
          || real.equals(Display.HSV) || real.equals(Display.CMY))
        continue; // more Color
      if (real.equals(Display.Alpha) || real.equals(Display.Shape)
          || real.equals(Display.ShapeScale) || real.equals(Display.Text)
          || real.equals(Display.SelectRange))
        continue;
      return false;
    }
    return true;
  }

  /**
   * test for display_indices in (Spatial, SpatialOffset, Color, Range,
   * Unmapped)
   */
  /*
   * WLH 16 July 2000 boolean checkSpatialColorRange(int[] display_indices)
   * throws RemoteException { for (int i=0; i<display_indices.length; i++) { if
   * (display_indices[i] == 0) continue; DisplayRealType real =
   * (DisplayRealType) display.getDisplayScalar(i); DisplayTupleType tuple =
   * real.getTuple(); if (tuple != null &&
   * (tuple.equals(Display.DisplaySpatialCartesianTuple) ||
   * (tuple.getCoordinateSystem() != null &&
   * tuple.getCoordinateSystem().getReference().equals(
   * Display.DisplaySpatialCartesianTuple)))) continue; // Spatial //
   * SpatialOffset if (Display.DisplaySpatialOffsetTuple.equals(tuple))
   * continue; if (tuple != null && (tuple.equals(Display.DisplayRGBTuple) ||
   * (tuple.getCoordinateSystem() != null &&
   * tuple.getCoordinateSystem().getReference().equals(
   * Display.DisplayRGBTuple)))) continue; // Color if (real.equals(Display.RGB)
   * || real.equals(Display.HSV) || real.equals(Display.CMY)) continue; // more
   * Color if (real.equals(Display.SelectRange)) continue; return false; }
   * return true; }
   */

  /**
   * test for display_indices in (Spatial, SpatialOffset, Color, Alpha, Range,
   * Unmapped)
   */
  boolean checkSpatialOffsetColorAlphaRange(int[] display_indices)
      throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      DisplayTupleType tuple = real.getTuple();
      if (tuple != null
          && (tuple.equals(Display.DisplaySpatialCartesianTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplaySpatialCartesianTuple))))
        continue; // Spatial
      // SpatialOffset
      if (Display.DisplaySpatialOffsetTuple.equals(tuple))
        continue;
      if (tuple != null
          && (tuple.equals(Display.DisplayRGBTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayRGBTuple))))
        continue; // Color
      if (real.equals(Display.RGB) || real.equals(Display.RGBA)
          || real.equals(Display.Alpha) || real.equals(Display.HSV)
          || real.equals(Display.CMY))
        continue; // more Color
      if (real.equals(Display.SelectRange))
        continue;
      return false;
    }
    return true;
  }

  /**
   * test for display_indices in (Spatial, Color, Alpha, Range, Unmapped)
   */
  boolean checkSpatialColorAlphaRange(int[] display_indices)
      throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      DisplayTupleType tuple = real.getTuple();
      if (tuple != null
          && (tuple.equals(Display.DisplaySpatialCartesianTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplaySpatialCartesianTuple))))
        continue; // Spatial
      if (tuple != null
          && (tuple.equals(Display.DisplayRGBTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayRGBTuple))))
        continue; // Color
      if (real.equals(Display.RGB) || real.equals(Display.RGBA)
          || real.equals(Display.Alpha) || real.equals(Display.HSV)
          || real.equals(Display.CMY))
        continue; // more Color
      if (real.equals(Display.SelectRange))
        continue;
      return false;
    }
    return true;
  }

  /** test for display_indices in (Spatial, Range, Unmapped) */
  boolean checkSpatialRange(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      DisplayTupleType tuple = real.getTuple();
      if (tuple != null
          && (tuple.equals(Display.DisplaySpatialCartesianTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplaySpatialCartesianTuple))))
        continue; // Spatial
      if (real.equals(Display.SelectRange))
        continue;
      return false;
    }
    return true;
  }

  /** test for any Animation or Value in display_indices */
  int checkAnimationOrValue(int[] display_indices) throws RemoteException {
    int count = 0;
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      if (real.equals(Display.Animation) || real.equals(Display.SelectValue))
        count++;
    }
    return count;
  }

  /** test for any SelectRange in display_indices */
  boolean anyRange(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      if (real.equals(Display.SelectRange))
        return true;
    }
    return false;
  }

  /** test for any IsoContour in display_indices */
  boolean checkContour(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      if (real.equals(Display.IsoContour))
        return true;
    }
    return false;
  }

  /** test for any Flow in display_indices */
  boolean checkFlow(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      DisplayTupleType tuple = real.getTuple();
      if (tuple != null
          && (tuple.equals(Display.DisplayFlow1Tuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayFlow1Tuple))))
        return true; // Flow1
      if (tuple != null
          && (tuple.equals(Display.DisplayFlow2Tuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayFlow2Tuple))))
        return true; // Flow2
    }
    return false;
  }

  /** test for any Shape in display_indices */
  boolean checkShape(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      if (real.equals(Display.Shape))
        return true;
    }
    return false;
  }

  /** test for any Text in display_indices */
  boolean checkText(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      /*
       * System.out.println("checkText: display_indices[" + i + "] = " +
       * display_indices[i] + " real = " + ((DisplayRealType)
       * display.getDisplayScalar(i)).getName());
       */
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      /*
       * System.out.println("checkText: real = " + real.getName());
       */
      if (real.equals(Display.Text)) {
        return true;
      }
    }
    return false;
  }

  boolean checkAdjustProjectionSeam() throws RemoteException {
    float[] default_values = getLink().getDefaultValues();
    return default_values[display
        .getDisplayScalarIndex(Display.AdjustProjectionSeam)] > 0.5f;
  }

  /** test for display_indices in (Color, Unmapped) */
  boolean checkColor(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      DisplayTupleType tuple = real.getTuple();
      if (tuple != null
          && (tuple.equals(Display.DisplayRGBTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayRGBTuple))))
        continue; // Color
      if (real.equals(Display.RGB) || real.equals(Display.RGBA)
          || real.equals(Display.HSV) || real.equals(Display.CMY))
        continue; // more Color
      return false;
    }
    return true;
  }

  /** test for display_indices in (Color, Alpha, Unmapped) */
  boolean checkColorOrAlpha(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      DisplayTupleType tuple = real.getTuple();
      if (tuple != null
          && (tuple.equals(Display.DisplayRGBTuple) || (tuple
              .getCoordinateSystem() != null && tuple.getCoordinateSystem()
              .getReference().equals(Display.DisplayRGBTuple))))
        continue; // Color
      if (real.equals(Display.RGB) || real.equals(Display.RGBA)
          || real.equals(Display.HSV) || real.equals(Display.CMY))
        continue; // more Color
      if (real.equals(Display.Alpha))
        continue;
      return false;
    }
    return true;
  }

  /** test for any non-zero display_indices */
  boolean checkAny(int[] display_indices) throws RemoteException {
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] > 0)
        return true;
    }
    return false;
  }

  /** applied at terminal nodes */
  int testIndices(int[] indices, int[] display_indices, int levelOfDifficulty)
      throws VisADException, RemoteException {
    // can apply ScalarMap-s as scan down MathType tree
    // make sure some DisplayRealType is mapped

    // test whether any RealType-s occur more than once
    for (int i = 0; i < indices.length; i++) {
      if (indices[i] > 1) {
        ScalarType real = display.getScalar(i);
        throw new BadMappingException("RealType " + real.getName()
            + " occurs more than once: " + "ShadowType.testIndices");
      }
    }

    // test whether any DisplayRealType occurs at least once;
    // test whether any Single DisplayRealType occurs more than once
    for (int i = 0; i < display_indices.length; i++) {
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      if (display_indices[i] > 0)
        isTerminal = true;
      if (display_indices[i] > 1 && real.isSingle()) {
        throw new BadMappingException("Single " + "DisplayRealType "
            + real.getName() + " occurs more than once: "
            + "ShadowType.testIndices");
      }
    }

    // test whether DisplayRealType-s from multiple
    // spatial DisplayTupleType-s occur
    spatialTuple = null;
    spatialDimension = 0;
    for (int i = 0; i < display_indices.length; i++) {
      if (display_indices[i] > 0) {
        DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
        DisplayTupleType rtuple = real.getTuple();
        if (rtuple != null) {
          if (rtuple.equals(Display.DisplaySpatialCartesianTuple)
              || (rtuple.getCoordinateSystem() != null && rtuple
                  .getCoordinateSystem().getReference().equals(
                      Display.DisplaySpatialCartesianTuple))) {
            if (spatialTuple != null && !spatialTuple.equals(rtuple)) {
              throw new BadMappingException("DisplayRealType-s occur from "
                  + "multiple spatial DisplayTupleType-s: "
                  + "ShadowType.testIndices");
            }
            spatialTuple = rtuple;
            spatialDimension++;
          }
        }
      }
    }

    if (isTerminal) {
      if (levelOfDifficulty == LEGAL) {
        LevelOfDifficulty = LEGAL;
      } else {
        LevelOfDifficulty = NESTED;
      }
    } else {
      // this is not illegal but also not a terminal node
      // (no DisplayRealType-s mapped)
      LevelOfDifficulty = NOTHING_MAPPED;
    }
    /*
     * System.out.println("testIndices: LevelOfDifficulty = " +
     * LevelOfDifficulty + " isTerminal = " + isTerminal + " Type = " +
     * Type.prettyString());
     */
    return LevelOfDifficulty;
  }

  /*
   * DEPRECATE THIS, no longer needed because SetTypes, Flat FieldTypes and Flat
   * TupleTypes are terminals: this defines the default logic for ShadowTextType
   * and ShadowMissingType - which may occur as a Field Range and are treated as
   * unmapped
   */
  /**
   * scans ShadowType tree to determine display feasibility and precompute
   * useful information for Data transform; indices & display_indices are counts
   * (at leaves) of numbers of occurrences of RealTypes and DisplayRealTypes;
   * isTransform flags for (Animation, Range, Value) re-transform;
   * levelOfDifficulty passed down and up call chain
   */
  public int checkIndices(int[] indices, int[] display_indices,
      int[] value_indices, boolean[] isTransform, int levelOfDifficulty)
      throws VisADException, RemoteException {

    adjustProjectionSeam = checkAdjustProjectionSeam();
    LevelOfDifficulty = testIndices(indices, display_indices, levelOfDifficulty);
    return LevelOfDifficulty;
  }

  public DisplayImpl getDisplay() {
    return display;
  }

  public MathType getType() {
    return Type;
  }

  public boolean getMultipleDisplayScalar() {
    return MultipleDisplayScalar;
  }

  public boolean getMultipleSpatialDisplayScalar() {
    return MultipleSpatialDisplayScalar;
  }

  public boolean getMappedDisplayScalar() {
    return MappedDisplayScalar;
  }

  public int[] getDisplayIndices() {
    int[] ii = new int[DisplayIndices.length];
    for (int i = 0; i < DisplayIndices.length; i++)
      ii[i] = DisplayIndices[i];
    return ii;
  }

  public int[] getValueIndices() {
    int[] ii = new int[ValueIndices.length];
    for (int i = 0; i < ValueIndices.length; i++)
      ii[i] = ValueIndices[i];
    return ii;
  }
  
  public ArrayList<FlowInfo> getFlowInfo() {
      return this.flowInfoList;
  }

  /**
   * return true if DisplayIndices include multiple Animation, SelectValue and
   * SelectRange
   */
  boolean testTransform() {
    int count = 0;
    for (int i = 0; i < DisplayIndices.length; i++) {
      if (DisplayIndices[i] == 0)
        continue;
      DisplayRealType real = (DisplayRealType) display.getDisplayScalar(i);
      if (real.equals(Display.Animation) || real.equals(Display.SelectValue)
          || real.equals(Display.SelectRange)) {
        count++;
        if (count > 1)
          return true;
      }
    }
    return false;
  }

  /**
   * mark Control-s as needing re-Transform; default for ShadowTextType and
   * ShadowMissingType
   */
  void markTransform(boolean[] isTransform) {
  }

  /**
   * helpers for doTransform; they are in ShadowType because they are
   * independent of graphics library
   */

  /** map values to display_values according to ScalarMap-s in reals */
  public static void mapValues(float[][] display_values, double[][] values,
      ShadowRealType[] reals) throws VisADException {
    int n = values.length;
    if (n != reals.length) {
      throw new DisplayException("lengths don't match " + n + " != "
          + reals.length + ": " + "ShadowType.mapValues");
    }
    for (int i = 0; i < n; i++) {
      Enumeration maps = reals[i].getSelectedMapVector().elements();
      while (maps.hasMoreElements()) {
        ScalarMap map = (ScalarMap) maps.nextElement();
        int value_index = map.getValueIndex();
        /*
         * double[] range = map.getRange(); System.out.println(map.getScalar() +
         * " -> " + map.getDisplayScalar() + " : " + range[0] + " " + range[1] +
         * "  value_index = " + value_index);
         */
        // MEM
        display_values[value_index] = map.scaleValues(values[i]);
        /*
         * int m = values[i].length; for (int j=0; j<m; j++)
         * System.out.println("values["+i+"]["+j+"] = " + values[i][j] +
         * " display_values["+value_index+"]["+j+"] = " +
         * display_values[value_index][j]);
         */
        /*
         * int total = 0; int missing = 0; total =
         * display_values[value_index].length; for (int j=0;
         * j<display_values[value_index].length; j++) { if
         * (display_values[value_index][j] != display_values[value_index][j]) {
         * missing++; } } System.out.println("  total = " + total +
         * " missing = " + missing);
         */
      }
    }
  }

  /** map values into display_values according to ScalarMap-s in reals */
  public static void mapValues(float[][] display_values, float[][] values,
      ShadowRealType[] reals) throws VisADException {
    mapValues(display_values, values, reals, true);
  }

  /**
   * Map values into display_values according to ScalarMap-s in reals
   * 
   * @param display_values
   *          return display values
   * @param values
   *          data values
   * @param reals
   *          the ShadowRealTypes corresponding to the Scalar in maps
   * @param copy
   *          if false, scale values in place if reals[index] has only one
   *          mapping. Use true if values represent a getSamples(false) or
   *          getFloats(false)
   */
  public static void mapValues(float[][] display_values, float[][] values,
      ShadowRealType[] reals, boolean copy) throws VisADException {
    int n = values.length;
    if (n != reals.length) {
      throw new DisplayException("lengths don't match: ShadowType.mapValues");
    }
    for (int i = 0; i < n; i++) {
      boolean doCopy = copy;
      int size = reals[i].getSelectedMapVector().size();
      if (!copy && size > 1)
        doCopy = true;
      Enumeration maps = reals[i].getSelectedMapVector().elements();
      while (maps.hasMoreElements()) {
        ScalarMap map = (ScalarMap) maps.nextElement();
        int value_index = map.getValueIndex();
        /*
         * double[] range = map.getRange(); System.out.println(map.getScalar() +
         * " -> " + map.getDisplayScalar() + " : " + range[0] + " " + range[1] +
         * "  value_index = " + value_index);
         */
        // MEM
        display_values[value_index] = map.scaleValues(values[i], doCopy);
        /*
         * int m = values[i].length; for (int j=0; j<m; j++)
         * System.out.println("values["+i+"]["+j+"] = " + values[i][j] +
         * " display_values["+value_index+"]["+j+"] = " +
         * display_values[value_index][j]);
         */
        /*
         * int total = 0; int missing = 0; total =
         * display_values[value_index].length; for (int j=0;
         * j<display_values[value_index].length; j++) { if
         * (display_values[value_index][j] != display_values[value_index][j])
         * missing++; } System.out.println("  total = " + total + " missing = "
         * + missing);
         */
      }
    }
  }

  /* CTR: 13 Oct 1998 - BEGIN CHANGES */
  public static VisADGeometryArray makePointGeometry(float[][] spatial_values,
      byte[][] color_values) throws VisADException {
    return makePointGeometry(spatial_values, color_values, false);
  }

  public static VisADGeometryArray makePointGeometry(float[][] spatial_values,
      byte[][] color_values, boolean compress) throws VisADException {
    if (spatial_values == null) {
      throw new DisplayException("bad spatial_values: "
          + "ShadowType.makePointGeometry: bad");
    }
    VisADPointArray array = new VisADPointArray();

    if (compress) {
      // redimension arrays to eliminate Float.NaN values
      int len = spatial_values.length;
      int clen;
      if (color_values == null)
        clen = 0;
      else
        clen = color_values.length;
      float[] f = spatial_values[0];
      int flen = f.length;
      int nan = 0;
      for (int i = 0; i < flen; i++)
        if (f[i] != f[i])
          nan++;
      if (nan > 0) {
        float[][] new_s_values = new float[len][flen - nan];
        byte[][] new_c_values = color_values;
        if (clen > 0)
          new_c_values = new byte[clen][flen - nan];
        int c = 0;
        for (int i = 0; i < flen; i++) {
          if (f[i] == f[i]) {
            for (int j = 0; j < len; j++) {
              new_s_values[j][c] = spatial_values[j][i];
            }
            for (int j = 0; j < clen; j++) {
              new_c_values[j][c] = color_values[j][i];
            }
            c++;
          }
        }
        spatial_values = new_s_values;
        color_values = new_c_values;
      }
    }

    // set coordinates and colors
    // MEM
    SampledSet.setGeometryArray(array, spatial_values, 4, color_values);
    return array;
  }

  /* CTR: 13 Oct 1998 - END CHANGES */

  /**
   * collect and transform Shape DisplayRealType values from display_values;
   * offset by spatial_values, colored by color_values and selected by
   * range_select
   */
  public VisADGeometryArray[] assembleShape(float[][] display_values,
      int valueArrayLength, int[] valueToMap, Vector MapVector,
      int[] valueToScalar, DisplayImpl display, float[] default_values,
      int[] inherited_values, float[][] spatial_values, byte[][] color_values,
      boolean[][] range_select, int index, ShadowType shadow_api)
      throws VisADException, RemoteException {

    if (spatial_values[0] == null)
      return null;
    int total_length = 0;
    Vector array_vector = new Vector();
    float x = spatial_values[0][0];
    float y = spatial_values[1][0];
    float z = spatial_values[2][0];
    byte r = 0;
    byte g = 0;
    byte b = 0;
    byte a = 0;
    int color_length = 0;
    if (color_values != null) {
      color_length = color_values.length;
      r = color_values[0][0];
      g = color_values[1][0];
      b = color_values[2][0];
      if (color_length > 3)
        a = color_values[3][0];
    }

    float[] scales = null;
    for (int i = 0; i < valueArrayLength; i++) {
      if (display_values[i] != null) {
        int displayScalarIndex = valueToScalar[i];
        DisplayRealType real = display.getDisplayScalar(displayScalarIndex);
        if (real.equals(Display.ShapeScale)) {
          if (index < 0) {
            scales = display_values[i];
            display_values[i] = null; // MEM_WLH 27 March 99
          } else {
            if (display_values[i].length == 1) {
              scales = new float[] { display_values[i][0] };
            } else {
              scales = new float[] { display_values[i][index] };
            }
          }
        }
      }
    }
    if (scales == null) {
      int default_index = display.getDisplayScalarIndex(Display.ShapeScale);
      float default_scale = default_values[default_index];
      scales = new float[] { default_scale };
    }

    float[] values = null;
    ShapeControl control = null;
    for (int j = 0; j < valueArrayLength; j++) {
      if (display_values[j] != null) {
        int displayScalarIndex = valueToScalar[j];
        DisplayRealType real = display.getDisplayScalar(displayScalarIndex);

        if (real.equals(Display.Shape)) {
          if (index < 0) {
            values = display_values[j];
            display_values[j] = null; // MEM_WLH 27 March 99
          } else {
            if (display_values[j].length == 1) {
              values = new float[] { display_values[j][0] };
            } else {
              values = new float[] { display_values[j][index] };
            }
          }
          control = (ShapeControl) ((ScalarMap) MapVector
              .elementAt(valueToMap[j])).getControl();
          if (values == null || control == null)
            continue;

          // make len maximum of lengths of color_values,
          // spatial_values & scales
          int len = values.length;
          if (color_values != null) {
            if (color_values[0].length > len)
              len = color_values[0].length;
          }
          if (spatial_values[0].length > len)
            len = spatial_values[0].length;
          if (scales.length > len)
            len = scales.length;
          // expand values if necessary
          if (values.length < len) {
            float[] new_values = new float[len];
            for (int i = 0; i < len; i++)
              new_values[i] = values[0];
            values = new_values;
          }

          // WLH 31 May 2000
          float cscale = control.getScale();

          VisADGeometryArray[] arrays = control.getShapes(values);
          for (int i = 0; i < arrays.length; i++) {
            if (range_select[0] != null) {
              if (range_select[0].length == 1) {
                if (!range_select[0][0])
                  arrays[i] = null;
              } else {
                if (!range_select[0][i])
                  arrays[i] = null;
              }
            }
            VisADGeometryArray array = arrays[i];
            if (array != null) {
              if (spatial_values[0].length > 1) {
                x = spatial_values[0][i];
                y = spatial_values[1][i];
                z = spatial_values[2][i];
              }
              int npts = array.coordinates.length / 3;
              // offset shape location by spatial values
              float scale = (scales.length == 1) ? scales[0] : scales[i];

              // WLH 31 May 2000
              scale *= cscale;

              for (int k = 0; k < array.coordinates.length; k += 3) {
                array.coordinates[k] = x + scale * array.coordinates[k];
                array.coordinates[k + 1] = y + scale * array.coordinates[k + 1];
                array.coordinates[k + 2] = z + scale * array.coordinates[k + 2];
              }

              if (array.colors == null && color_values != null) {
                array.colors = new byte[color_length * npts];
                if (color_values[0].length > 1) {
                  r = color_values[0][i];
                  g = color_values[1][i];
                  b = color_values[2][i];
                  if (color_length > 3)
                    a = color_values[3][i];
                }
                for (int k = 0; k < array.colors.length; k += color_length) {
                  array.colors[k] = r;
                  array.colors[k + 1] = g;
                  array.colors[k + 2] = b;
                  if (color_length > 3)
                    array.colors[k + 3] = a;
                }
              }

            }
          } // end for (int i=0; i<arrays.length; i++)
          total_length += arrays.length;
          array_vector.addElement(arrays);
        } // end if (real.equals(Display.Shape))
      } // end if (display_values[i] != null)
    } // end for (int j=0; j<valueArrayLength; j++)

    if (total_length == 0)
      return null;
    VisADGeometryArray[] total_arrays = new VisADGeometryArray[total_length];
    Enumeration arrayses = array_vector.elements();
    int k = 0;
    while (arrayses.hasMoreElements()) {
      VisADGeometryArray[] arrays = (VisADGeometryArray[]) arrayses
          .nextElement();
      System.arraycopy(arrays, 0, total_arrays, k, arrays.length);
      k += arrays.length;
    }

    // WLH 30 May 2002
    DataRenderer renderer = getLink().getRenderer();
    if (getAdjustProjectionSeam()) {
      for (int i = 0; i < total_length; i++) {
        if (total_arrays[i] != null) {
          total_arrays[i] = total_arrays[i].adjustLongitudeBulk(renderer);
        }
      }
    }

    return total_arrays;
  }

  /**
   * collect and transform spatial DisplayRealType values from display_values;
   * add spatial offset DisplayRealType values; adjust flow1_values and
   * flow2_values for any coordinate transform; if needed, return a spatial Set
   * from spatial_values, with the same topology as domain_set (or an
   * appropriate Irregular topology); domain_set = null and allSpatial = false
   * if not called from ShadowFunctionType
   */
  public Set assembleSpatial(float[][] spatial_values,
      float[][] display_values, int valueArrayLength, int[] valueToScalar,
      DisplayImpl display, float[] default_values, int[] inherited_values,
      Set domain_set, boolean allSpatial, boolean set_for_shape,
      int[] spatialDimensions, boolean[][] range_select,
      float[][] flow1_values, float[][] flow2_values, float[] flowScale,
      boolean[] swap, DataRenderer renderer, ShadowType shadow_api)
      throws VisADException, RemoteException {
    DisplayTupleType spatial_tuple = null;
    // number of spatial samples, default is 1
    int len = 1;
    // number of non-inherited spatial dimensions
    int spatialDimension = 0;
    // temporary holder for non-inherited tuple_index values
    int[] tuple_indices = new int[3];
    spatialDimensions[0] = 0; // spatialDomainDimension
    spatialDimensions[1] = 0; // spatialManifoldDimension
    // holder for SpatialOffset values
    float[][] offset_values = new float[3][];
    boolean[] offset_copy = { false, false, false };

    // spatial map RealType Units
    Unit[] spatial_units = new Unit[] { null, null, null };
    // spatial map getRange() results for flow adjustment
    double[] ranges = new double[] { Double.NaN, Double.NaN, Double.NaN };
    // some helpers for computing ranges for flow adjustment
    int[] valueToMap = display.getValueToMap();
    Vector MapVector = display.getMapVector();

    // indexed by tuple_index
    int[] spatial_value_indices = { -1, -1, -1 };

    for (int i = 0; i < valueArrayLength; i++) {
      if (display_values[i] != null) {
        int displayScalarIndex = valueToScalar[i];
        DisplayRealType real = display.getDisplayScalar(displayScalarIndex);
        DisplayTupleType tuple = real.getTuple();

        if (tuple != null
            && (tuple.equals(Display.DisplaySpatialCartesianTuple) || (tuple
                .getCoordinateSystem() != null && tuple.getCoordinateSystem()
                .getReference().equals(Display.DisplaySpatialCartesianTuple)))) {
          if (spatial_tuple != null && !spatial_tuple.equals(tuple)) {
            throw new DisplayException("multiple spatial display tuples: "
                + "ShadowType.assembleSpatial");
          }
          spatial_tuple = tuple;
          int tuple_index = real.getTupleIndex();
          spatial_value_indices[tuple_index] = i;
          spatial_values[tuple_index] = display_values[i];
          len = Math.max(len, display_values[i].length);
          display_values[i] = null; // MEM_WLH 27 March 99
          spatialDimensions[0]++; // spatialDomainDimension
          if (inherited_values[i] == 0) {
            // don't count inherited spatial dimensions
            tuple_indices[spatialDimension] = tuple_index;
            spatialDimension++; // # non-inherited spatial dimensions
          }
          ScalarMap map = (ScalarMap) MapVector.elementAt(valueToMap[i]);
          double[] map_range = map.getRange();
          ranges[tuple_index] = map_range[1] - map_range[0];
          spatial_units[tuple_index] = ((RealType) map.getScalar())
              .getDefaultUnit();
        }
      } // end if (display_values[i] != null)
    } // end for (int i=0; i<valueArrayLength; i++)
    if (spatial_tuple == null) {
      spatial_tuple = Display.DisplaySpatialCartesianTuple;
    }

    if (spatialDimension == 0) {
      // len = 1 in this case
      spatialDimensions[1] = 0; // spatialManifoldDimension
    } else if (domain_set == null) {
      spatialDimensions[1] = spatialDimension; // spatialManifoldDimension
    } else if (!allSpatial) {
      spatialDimensions[1] = spatialDimension; // spatialManifoldDimension
      if (set_for_shape) {
        // cannot inherit Set topology from Field domain, so
        // construct IrregularSet topology of appropriate dimension
        RealType[] reals = new RealType[spatialDimension];
        float[][] samples = new float[spatialDimension][];
        for (int i = 0; i < spatialDimension; i++) {
          reals[i] = RealType.Generic;
          samples[i] = spatial_values[tuple_indices[i]];
        }
        RealTupleType tuple_type = new RealTupleType(reals);
        // MEM
        try {
          switch (spatialDimension) {
          case 1:
            domain_set = new Irregular1DSet(tuple_type, samples, null, null,
                null, false);
            break;
          case 2:
            domain_set = new Irregular2DSet(tuple_type, samples, null, null,
                null, null, false);
            break;
          case 3:
            domain_set = new Irregular3DSet(tuple_type, samples, null, null,
                null, null, false);
            break;
          }
        } catch (VisADException e) {
          domain_set = null;
        }
        // System.out.println("IrregularSet done");
      } else { // !set_for_shape
        domain_set = null;
      }
    } else { // spatialDimension > 0 && allSpatial && domain_set != null
      // spatialManifoldDimension
      spatialDimensions[1] = domain_set.getManifoldDimension();
    }

    //
    // need a spatial Set for shape (e.g., contour)
    // or spatialManifoldDimension < 3
    // NOTE - 3-D volume rendering may eventually need a spatial Set
    //
    boolean set_needed = domain_set != null
        && (set_for_shape || spatialDimensions[1] < 3);

    boolean[] missing_checked = { false, false, false };
    for (int i = 0; i < 3; i++) {
      if (spatial_values[i] == null) {
        // fill any null spatial value arrays with default values
        // MEM
        spatial_values[i] = new float[len];
        int default_index = display
            .getDisplayScalarIndex(((DisplayRealType) spatial_tuple
                .getComponent(i)));
        float default_value = default_values[default_index];
        java.util.Arrays.fill(spatial_values[i], default_value);
        missing_checked[i] = true;
      } else if (spatial_values[i].length == 1) {
        // check solitary spatial value array for missing
        float v = spatial_values[i][0];
        missing_checked[i] = true;
        if (v != v || Float.isInfinite(v)) {
          // missing with length = 1, so nothing to render
          range_select[0] = new boolean[1];
          range_select[0][0] = false;
          return null;
        }
        if (len > 1) {
          // expand solitary spatial value array
          // MEM
          spatial_values[i] = new float[len];
          for (int j = 0; j < len; j++)
            spatial_values[i][j] = v;
        }
      }
    } // end for (int i=0; i<3; i++)

    // first equalize lengths of flow*_values and spatial_values
    boolean anyFlow = false;
    int[] flen = { 0, 0 };
    float[][][] ff_values = { flow1_values, flow2_values };
    for (int k = 0; k < 2; k++) {
      for (int i = 0; i < 3; i++) {
        if (ff_values[k][i] != null) {
          anyFlow = true;
          flen[k] = Math.max(flen[k], ff_values[k][i].length);
        }
      }
    }
    len = Math.max(len, Math.max(flen[0], flen[1]));
    fillOut(spatial_values, len);
    if (flen[0] > 0)
      fillOut(flow1_values, len);
    if (flen[1] > 0)
      fillOut(flow2_values, len);

    boolean spatial_flow = anyFlow;
    for (int i = 0; i < 3; i++) {
      if (ranges[i] == ranges[i]) {
        if (spatial_units[i] == null) {
          spatial_flow = false;
          break;
        }
        for (int j = 0; j < 3; j++) {
          if (ranges[j] == ranges[j]) {
            if (spatial_units[j] == null) {
              spatial_flow = false;
              break;
            }
            if (!Unit.canConvert(spatial_units[i], spatial_units[j])) {
              spatial_flow = false;
              break;
            }
          }
        }
      }
    }
    // System.out.println("spatial_flow = " + spatial_flow);
    boolean[] spatial_flows = new boolean[] {spatial_flow,spatial_flow};

    if (spatial_flow) {
      for (int i = 0; i < valueArrayLength; i++) {
        if (display_values[i] != null) {
          int displayScalarIndex = valueToScalar[i];
          DisplayRealType real = display.getDisplayScalar(displayScalarIndex);
          DisplayTupleType tuple = real.getTuple();
           if (tuple != null) {
              if (tuple.equals(Display.DisplayFlow1SphericalTuple) ||
                  tuple.equals(Display.DisplayFlow2SphericalTuple)) {
            	  int index =  tuple.equals(Display.DisplayFlow1SphericalTuple) ? 0 : 1;
              	  spatial_flows[index] = false;
                  display_values[i] = null; 
              }
           }
        }
      }

      // adjust flow for spatial setRange scaling
      double max_range = -1.0;
      for (int i = 0; i < 3; i++) {
        if (ranges[i] == ranges[i]) {
          double ar = Math.abs(ranges[i]);
          if (ar > max_range)
            max_range = ar;
        }
      }
      for (int i = 0; i < 3; i++) {
        if (ranges[i] == ranges[i]) {
          ranges[i] = ranges[i] / max_range;
        } else {
          ranges[i] = 1.0;
        }
      }
      for (int k = 0; k < 2; k++) {
        if (!(renderer.getRealVectorTypes(k) instanceof EarthVectorType) && spatial_flows[k]) {
          if (ff_values[k][0] != null || ff_values[k][1] != null
              || ff_values[k][2] != null) {
            for (int j = 0; j < len; j++) {
              float old_speed = 0.0f;
              float new_speed = 0.0f;
              for (int i = 0; i < 3; i++) {
                if (ff_values[k][i] != null) {
                  old_speed += ff_values[k][i][j] * ff_values[k][i][j];
                  ff_values[k][i][j] *= ranges[i];
                  new_speed += ff_values[k][i][j] * ff_values[k][i][j];
                }
              }
              // but don't change vector magnitude ??
              float ratio = (float) Math.sqrt(old_speed / new_speed);
              // BMF 2009-03-04: flow values cannot be NaN
              if (Float.isNaN(ratio)) ratio = 0f;
              for (int i = 0; i < 3; i++) {
                if (ff_values[k][i] != null) {
                  ff_values[k][i][j] *= ratio;
                }
              }
            }
          } // end if (ff_values[k][0] != null || ...)
        } // end if (!(renderer.getRealVectorTypes(k) instanceof
          // EarthVectorType))
      } // end for (int k=0; k<2; k++)

    } // end if (spatial_flow)

    // adjust Flow values for coordinate transform
    if (spatial_tuple.equals(Display.DisplaySpatialCartesianTuple)) {
      // if (anyFlow) { WLH 4 March 2000
      renderer.setEarthSpatialDisplay(null, spatial_tuple, display,
          spatial_value_indices, default_values, ranges);
      // } WLH 4 March 2000
    } else {
      // transform tuple_values to DisplaySpatialCartesianTuple
      CoordinateSystem coord = spatial_tuple.getCoordinateSystem();

      float[][][] vector_ends = new float[2][][];

      // WLH 4 March 2000
      renderer.setEarthSpatialDisplay(coord, spatial_tuple, display,
          spatial_value_indices, default_values, ranges);
      if (spatial_flow) {
        if (anyFlow) {
          // WLH 4 March 2000
          // renderer.setEarthSpatialDisplay(coord, spatial_tuple, display,
          // spatial_value_indices, default_values, ranges);

          // compute and transform 'end points' of flow vectors
          for (int k = 0; k < 2; k++) {
            if (!(renderer.getRealVectorTypes(k) instanceof EarthVectorType) && spatial_flows[k]) {
              if (flen[k] > 0) {
                vector_ends[k] = new float[3][len];
                for (int i = 0; i < 3; i++) {
                  if (ff_values[k][i] != null) {
                    for (int j = 0; j < len; j++) {
                      vector_ends[k][i][j] = spatial_values[i][j]
                          + flowScale[k] * ff_values[k][i][j];
                    }
                  } else { // (ff_values[k][i] == null)
                    for (int j = 0; j < len; j++) {
                      vector_ends[k][i][j] = spatial_values[i][j];
                    }
                  }
                } // end for (int i=0; i<3; i++)
                vector_ends[k] = coord.toReference(vector_ends[k]);
              } // end if (flen[k] > 0)
            } // end if (!(renderer.getRealVectorTypes(k) instanceof
              // EarthVectorType))
          } // end for (int k=0; k<2; k++)
        }

      } // end if (spatial_flow)

      // transform spatial_values
      float[][] new_spatial_values = coord.toReference(spatial_values);
      /*
       * System.out.println("in length = " + spatial_values[0].length +
       * " out length = " + new_spatial_values[0].length); if
       * (spatial_values[0].length == 5329) { // System.out.println(domain_set);
       * // 73 73 for (int i=0; i<spatial_values[0].length; i+=71) {
       * System.out.println("out " + new_spatial_values[0][i] + " " +
       * new_spatial_values[1][i] + " " + new_spatial_values[2][i] + " in " +
       * spatial_values[0][i] + " " + spatial_values[1][i] + " " +
       * spatial_values[2][i] + " (i,j) = " + i/73 + " " + i%73); } }
       */
      for (int i = 0; i < 3; i++)
        spatial_values[i] = new_spatial_values[i];

      if (spatial_flow) {

        if (anyFlow) {
          // subtract transformed spatial_values from transformed flow vectors
          for (int k = 0; k < 2; k++) {
            if (!(renderer.getRealVectorTypes(k) instanceof EarthVectorType) && spatial_flows[k]) {
              if (flen[k] > 0) {
                for (int i = 0; i < 3; i++) {
                  for (int j = 0; j < len; j++) {
                    vector_ends[k][i][j] = (vector_ends[k][i][j] - spatial_values[i][j])
                        / flowScale[k];
                  }
                  ff_values[k][i] = vector_ends[k][i];
                }
              }
            } // end if (!(renderer.getRealVectorTypes(k) instanceof
              // EarthVectorType))
          } // end for (int k=0; k<2; k++)
        }

      } // end if (spatial_flow)

      missing_checked = new boolean[] { false, false, false };

    } // end if (!spatial_tuple.equals(Display.DisplaySpatialCartesianTuple))

    // calculate if need to swap rows and cols in contour line labels
    swap[0] = false;
    if (allSpatial && spatialDimensions[1] == 2 && len > 1) {

      // find the axis most nearly parallel to first grid direction
      // i.e., vector from first sample (0) to second sample (1)
      float simax = 0.0f;
      float max = -1.0f;
      int imax = -1;
      for (int i = 0; i < 3; i++) {
        float sdiff = spatial_values[i][1] - spatial_values[i][0];
        float diff = Math.abs(sdiff);
        if (diff > max) {
          simax = sdiff;
          max = diff;
          imax = i;
        }
      }

      // set ll = number of samples along a side of fastest factor of
      // Gridded2DSet
      // i.e., "stride"
      // WLH 6 April 2001
      // int ll = len;
      int ll = len - 1;
      if (domain_set != null && domain_set instanceof Gridded2DSet) {
        ll = ((Gridded2DSet) domain_set).getLength(0);
        if (ll > (len - 1))
          ll = len - 1; // WLH 6 April 2001
      }

      // find the axis most nearly parallel to second grid direction
      // i.e., vector from first sample (0) to second sample (1)
      float sjmax = 0.0f;
      max = -1.0f;
      int jmax = -1;
      for (int i = 0; i < 3; i++) {
        if (i != imax) {
          // WLH 6 April 2001
          // float sdiff = spatial_values[i][ll-1] - spatial_values[i][0];
          float sdiff = spatial_values[i][ll] - spatial_values[i][0];
          float diff = Math.abs(sdiff);
          if (diff > max) {
            sjmax = sdiff;
            max = diff;
            jmax = i;
          }
        }
      } // end for (int i=0; i<3; i++)
      /*-TDR, debug
      System.out.println("imax: "+imax+" jmax: "+jmax);
      System.out.println("simax: "+simax+" sjmax: "+sjmax);
       */
      if (imax == 0) {
        swap[0] = true;
        swap[1] = (simax < 0.0f);
        swap[2] = (sjmax < 0.0f);
      } else if (imax == 1) {
        /*-TDR, (4-18-01):
           wrong grid dimension swapped

         swap[1] = (sjmax < 0.0f);
         swap[2] = (simax < 0.0f);
         */
        swap[2] = (sjmax < 0.0f);
        swap[1] = (simax < 0.0f);
      } else { // imax == 2
        if (jmax == 1) {
          swap[0] = true;
          swap[1] = (simax < 0.0f);
          swap[2] = (sjmax < 0.0f);
        } else {
          /*-TDR, (4-18-01) Untested:
             should probably be same as change above
           swap[1] = (sjmax < 0.0f);
           swap[2] = (simax < 0.0f);
           */
          swap[2] = (sjmax < 0.0f);
          swap[1] = (simax < 0.0f);
        }
      }
      /*-TDR, debug
      System.out.println("swap[0]: "+swap[0]+" swap[1]: "+swap[1]+
                         " swap[2]: "+swap[2]);
       */
    }

    // assemble SpatialOffsets
    int offset_len = len;
    for (int i = 0; i < valueArrayLength; i++) {
      if (display_values[i] != null) {
        int displayScalarIndex = valueToScalar[i];
        DisplayRealType real = display.getDisplayScalar(displayScalarIndex);
        DisplayTupleType tuple = real.getTuple();
        if (Display.DisplaySpatialOffsetTuple.equals(tuple)) {
          int tuple_index = real.getTupleIndex();
          if (offset_values[tuple_index] == null) {
            offset_values[tuple_index] = display_values[i];
          } else {
            int leno = offset_values[tuple_index].length;
            int lend = display_values[i].length;
            if (leno > lend) {
              // assume lend == 1
              float[] off;
              if (offset_copy[tuple_index]) {
                off = offset_values[tuple_index];
              } else {
                off = new float[leno];
                offset_copy[tuple_index] = true;
              }
              for (int j = 0; j < leno; j++) {
                off[j] = offset_values[tuple_index][j] + display_values[i][0];
              }
              offset_values[tuple_index] = off;
              off = null;
            } else if (leno < lend) {
              // assume leno == 1
              float[] off = new float[lend];
              for (int j = 0; j < lend; j++) {
                off[j] = offset_values[tuple_index][0] + display_values[i][j];
              }
              offset_values[tuple_index] = off;
              off = null;
              offset_copy[tuple_index] = true;
            } else {
              float[] off;
              if (offset_copy[tuple_index]) {
                off = offset_values[tuple_index];
              } else {
                off = new float[leno];
                offset_copy[tuple_index] = true;
              }
              for (int j = 0; j < leno; j++) {
                off[j] = offset_values[tuple_index][j] + display_values[i][j];
              }
              offset_values[tuple_index] = off;
              off = null;
            }
          }
          display_values[i] = null; // MEM_WLH 27 March 99
          offset_len = Math.max(offset_len, offset_values[tuple_index].length);
        } // end if (Display.DisplaySpatialOffsetTuple.equals(tuple))
      } // end if (display_values[i] != null)
    } // end for (int i=0; i<valueArrayLength; i++)

    boolean[] offset_missing_checked = { false, false, false };
    for (int i = 0; i < 3; i++) {
      if (offset_values[i] == null) {
        // WLH 13 June 2003
        DisplayRealType offset = (DisplayRealType) Display.DisplaySpatialOffsetTuple
            .getComponent(i);
        int default_index = display.getDisplayScalarIndex(offset);
        if (0 <= default_index && default_index < default_values.length) {
          float default_value = default_values[default_index];
          if (default_value == default_value) {
            offset_values[i] = new float[] { default_value };
          }
        }
        // end WLH 13 June 2003
        offset_missing_checked[i] = true;
      } else if (offset_values[i].length == 1) {
        offset_missing_checked[i] = true;
        if (offset_values[i][0] != offset_values[i][0]
            || Float.isInfinite(offset_values[i][0])) {
          // missing with length = 1, so nothing to render
          range_select[0] = new boolean[1];
          range_select[0][0] = false;
          return null;
        }
      }
    }

    // spatial offsets longer than spatial, so increase len
    if (offset_len > len) {
      // assume len == 1
      for (int i = 0; i < 3; i++) {
        float[] s = new float[offset_len];
        for (int k = 0; k < offset_len; k++)
          s[k] = spatial_values[i][0];
        spatial_values[i] = s;
        s = null;
      }
      len = offset_len;
    }

    // add any spatial offsets to spatial values
    for (int i = 0; i < 3; i++) {
      if (offset_values[i] != null) {
        int leno = offset_values[i].length;
        if (leno < len) {
          // assume leno == 1
          for (int k = 0; k < offset_len; k++) {
            spatial_values[i][k] += offset_values[i][0];
          }
        } else {
          // assume leno == len
          for (int k = 0; k < offset_len; k++) {
            spatial_values[i][k] += offset_values[i][k];
          }
        }
        offset_values[i] = null;
        missing_checked[i] = missing_checked[i] && offset_missing_checked[i];
      }

      if (!missing_checked[i]) {
        for (int j = 0; j < len; j++) {
          if (spatial_values[i][j] != spatial_values[i][j]
              || Float.isInfinite(spatial_values[i][j])) {
            if (range_select[0] == null) {
              range_select[0] = new boolean[len];
              for (int k = 0; k < len; k++)
                range_select[0][k] = true;
            } else if (range_select[0].length < len) {
              // assume range_select[0].length == 1
              boolean[] r = new boolean[len];
              for (int k = 0; k < len; k++)
                r[k] = range_select[0][0];
              range_select[0] = r;
            }
            range_select[0][j] = false;
            spatial_values[i][j] = Float.NaN;
          }
        }
      }
    } // end for (int i=0; i<3; i++)

    spatial_offset_values = offset_values;

    if (set_needed) {
      try {
        if (spatialDimension == 0) {
          double[] values = new double[3];
          for (int i = 0; i < 3; i++)
            values[i] = spatial_values[i][0];
          RealTuple tuple = new RealTuple(Display.DisplaySpatialCartesianTuple,
              values);
          return new SingletonSet(tuple);
        } else {
          SetType type = new SetType(Display.DisplaySpatialCartesianTuple);
          // MEM
          // WLH 5 April 2000
          // return domain_set.makeSpatial(type, spatial_values);
          return shadow_api.makeSpatialSet(domain_set, type, spatial_values);
        }
      } catch (VisADException e) {
        return null;
      }
    } else {
      return null;
    }
  }

  // WLH 5 April 2000
  public Set makeSpatialSet(Set domain_set, SetType type,
      float[][] spatial_values) throws VisADException {
    return domain_set.makeSpatial(type, spatial_values);
  }

  private static void fillOut(float[][] values, int flen) {
    for (int i = 0; i < values.length; i++) {
      if (values[i] != null) {
        int len = values[i].length;
        if (len < flen) {
          // assume len == 1
          float[] s = new float[flen];
          float v = values[i][0];
          for (int k = 0; k < flen; k++)
            s[k] = v;
          values[i] = s;
        }
      }
    }
  }

  /**
   * assemble Flow components; Flow components are 'single', so no compositing
   * is required
   */
  public void assembleFlow(float[][] flow1_values, float[][] flow2_values,
      float[] flowScale, float[] arrowScale, float[][] display_values, int valueArrayLength,
      int[] valueToScalar, DisplayImpl display, float[] default_values,
      boolean[][] range_select, DataRenderer renderer, ShadowType shadow_api)
      throws VisADException, RemoteException {

    int[] valueToMap = display.getValueToMap();
    Vector MapVector = display.getMapVector();

    int[] flen = { 0, 0 };
    float[][][] ff_values = { flow1_values, flow2_values };
    DisplayTupleType[] flow_tuple = { Display.DisplayFlow1Tuple,
        Display.DisplayFlow2Tuple };
    DisplayTupleType[] actual_tuple = { null, null };

    boolean anyFlow = false;
    /*
     * WLH 15 April 2000 ScalarMap[][] maps = new ScalarMap[2][3];
     */

    for (int i = 0; i < valueArrayLength; i++) {
      if (display_values[i] != null) {
        int displayScalarIndex = valueToScalar[i];
        DisplayRealType real = display.getDisplayScalar(displayScalarIndex);
        DisplayTupleType tuple = real.getTuple();
        for (int k = 0; k < 2; k++) {
          if (tuple != null
              && (tuple.equals(flow_tuple[k]) || (tuple.getCoordinateSystem() != null && tuple
                  .getCoordinateSystem().getReference().equals(flow_tuple[k])))) {
            if (actual_tuple[k] != null && !actual_tuple[k].equals(tuple)) {
              throw new DisplayException("multiple flow " + k
                  + " display tuples: " + "ShadowType.assembleFlow");
            }
            actual_tuple[k] = tuple;
            ScalarMap map = (ScalarMap) MapVector.elementAt(valueToMap[i]);
            FlowControl control = (FlowControl) map.getControl();
            flowScale[k] = control.getFlowScale();
            arrowScale[k] = control.getArrowScale();
            int flow_index = real.getTupleIndex();
            ff_values[k][flow_index] = display_values[i];
            flen[k] = Math.max(flen[k], display_values[i].length);
            // if not DisplayFlow*SphericalTuple, null out values
            if (actual_tuple[k].equals(flow_tuple[k])) {
              display_values[i] = null; // MEM_WLH 27 March 99
            }
            /*
             * WLH 15 April 2000 maps[k][flow_index] = map;
             */

            anyFlow = true;

            if (k == 0) {
              streamline1 = control.streamlinesEnabled();
              streamlineDensity1 = control.getStreamlineDensity();
              arrowScale1 = control.getArrowScale();
              stepFactor1 = control.getStepFactor();
              packingFactor1 = control.getStreamlinePacking();
              float[] pp = control.getStreamlineSmoothing();
              cntrWeight1 = pp[0];
              n_pass1 = (int) pp[1];
              reduction1 = control.getStreamlineReduction();
              trajectory1 = control.trajectoryEnabled();
              if (trajectory1) {
                 ff_values[k][flow_index] = map.inverseScaleValues(ff_values[k][flow_index], true);
                 flowScale[k] = 1f;
                 cacheTrajFlow1 = false; // need to check FlowControl
              }
            }
            if (k == 1) {
              streamline2 = control.streamlinesEnabled();
              streamlineDensity2 = control.getStreamlineDensity();
              arrowScale2 = control.getArrowScale();
              stepFactor2 = control.getStepFactor();
              packingFactor2 = control.getStreamlinePacking();
              float[] pp = control.getStreamlineSmoothing();
              cntrWeight2 = pp[0];
              n_pass2 = (int) pp[1];
              reduction2 = control.getStreamlineReduction();
              trajectory2 = control.trajectoryEnabled();
              if (trajectory2) {
                 ff_values[k][flow_index] = map.inverseScaleValues(ff_values[k][flow_index], true);
                 flowScale[k] = 1f;
                 cacheTrajFlow2 = false; // need to check FlowControl
              }
            }
          }
        }
      }
    }

    /*
     * WLH 15 April 2000 if (anyFlow) renderer.setFlowDisplay(maps, flowScale);
     */

    //
    // TO_DO
    // this should all happen in flow rendering method
    //
    for (int k = 0; k < 2; k++) {
      boolean[] missing_checked = { false, false, false };
      if (flen[k] > 0) {
        for (int i = 0; i < 3; i++) {
          if (ff_values[k][i] == null) {
            // fill any null flow value arrays with default values
            // MEM
            ff_values[k][i] = new float[flen[k]];
            int default_index = display
                .getDisplayScalarIndex(((DisplayRealType) flow_tuple[k]
                    .getComponent(i)));
            float default_value = default_values[default_index];
            for (int j = 0; j < flen[k]; j++) {
              ff_values[k][i][j] = default_value;
            }
            missing_checked[i] = true;
          } else if (ff_values[k][i].length == 1) {
            // check solitary spatial value array for missing
            float v = ff_values[k][i][0];
            missing_checked[i] = true;
            if (v != v) {
              // missing with length = 1, so nothing to render
              range_select[0] = new boolean[1];
              range_select[0][0] = false;
              return;
            }
            if (flen[k] > 1) {
              // expand solitary flow value array
              ff_values[k][i] = new float[flen[k]];
              for (int j = 0; j < flen[k]; j++) {
                ff_values[k][i][j] = v;
              }
            }
          } // end if (ff_values[k][i].length == 1)
        } // end for (int i=0; i<3; i++)
      } // end if (flen[k] > 0)

      if (actual_tuple[k] != null && !actual_tuple[k].equals(flow_tuple[k])) {
        missing_checked = new boolean[] { false, false, false };
        // transform tuple_values to flow_tuple[k]
        CoordinateSystem coord = actual_tuple[k].getCoordinateSystem();
        float[][] new_ff_values = coord.toReference(ff_values[k]);
        for (int i = 0; i < 3; i++)
          ff_values[k][i] = new_ff_values[i];
      }

      if (flen[k] > 0) {
        for (int i = 0; i < 3; i++) {
          if (!missing_checked[i]) {
            for (int j = 0; j < flen[k]; j++) {
              if (ff_values[k][i][j] != ff_values[k][i][j]) {
                if (range_select[0] == null) {
                  range_select[0] = new boolean[flen[k]];
                  for (int m = 0; m < flen[k]; m++)
                    range_select[0][m] = true;
                } else if (range_select[0].length < flen[k]) {
                  // assume range_select[0].length == 1
                  boolean[] r = new boolean[flen[k]];
                  for (int m = 0; m < flen[k]; m++)
                    r[m] = range_select[0][0];
                  range_select[0] = r;
                }
                range_select[0][j] = false;
                ff_values[k][i][j] = 0.0f;
              }
            } // end for (int j=0; j<flen[k]; j++)
          } // end if (!missing_checked[i])
        } // end for (int i=0; i<3; i++)
      } // end if (flen[k] > 0)
    } // end for (int k=0; k<2; k++)
    // end of 'this should all happen in flow rendering method'
  }

  public static final float METERS_PER_DEGREE = 111137.0f;

  public static float[][] adjustFlowToEarth(int which, float[][] flow_values,
      float[][] spatial_values, float flowScale, DataRenderer renderer)
      throws VisADException {
    return adjustFlowToEarth(which, flow_values, spatial_values, flowScale, renderer, false);
  }

  public static float[][] adjustFlowToEarth(int which, float[][] flow_values,
      float[][] spatial_values, float flowScale, DataRenderer renderer, boolean force)
      throws VisADException {
    return adjustFlowToEarth(which, flow_values, spatial_values, flowScale, renderer, force, false, 3600f);
  }


  public static float[][] adjustFlowToEarth(int which, float[][] flow_values,
      float[][] spatial_values, float flowScale, DataRenderer renderer, boolean force, 
          boolean isTraj, float timeStep)
      throws VisADException {
    // System.out.println("adjustFlowToEarth " + renderer.getDisplay().getName()
    // + " " + renderer.getRealVectorTypes(which)); // IDV
	// Move this down into the check for shouldAdjust
    //if (!(renderer.getRealVectorTypes(which) instanceof EarthVectorType) && !force) {
      // only do this for EarthVectorType
    //  return flow_values;
    //}

      
    FlowControl fcontrol = null;
    DisplayImpl display = null;
    boolean shouldAdjust = true;
    DataDisplayLink link = renderer.getLinks()[0];
    if (link != null && !force) {
      display = link.getDisplay();
      if (display != null) {

        if (which == 0) {
          fcontrol = (FlowControl) display.getControl(Flow1Control.class);
        } else if (which == 1) {
          fcontrol = (FlowControl) display.getControl(Flow2Control.class);
        }
        if (fcontrol == null) {
          throw new VisADException(
              "adjustFlowToEarth: Unable to get FlowControl");
        }
        shouldAdjust = fcontrol.getAdjustFlowToEarth();
        // add one more check
        Vector maps = link.getSelectedMapVector();
        boolean haveSpeedDir = false;
        for (int i = 0; i < maps.size(); i++) {
        	ScalarMap map = (ScalarMap) maps.get(i);
        	DisplayRealType displayType = map.getDisplayScalar();
        	if ((which == 0 && (displayType.equals(Display.Flow1Azimuth) ||
        	                    displayType.equals(Display.Flow1Radial)  ||
        	                    displayType.equals(Display.Flow1Elevation)))  ||
        	    (which == 1 && (displayType.equals(Display.Flow2Azimuth)  ||
        	                    displayType.equals(Display.Flow2Radial)  ||
        	                    displayType.equals(Display.Flow2Elevation)))) {
        		haveSpeedDir = true;
        		break;
        	}
        }
        if (!(renderer.getRealVectorTypes(which) instanceof EarthVectorType) && !haveSpeedDir ) {
        	shouldAdjust = false;
        }
      }
    }

    if (!shouldAdjust)
      return flow_values;

    int flen = flow_values[0].length;

    // get flow_values maximum
    float scale = 0.0f;
    for (int j = 0; j < flen; j++) {
      if (Math.abs(flow_values[0][j]) > scale) {
        scale = (float) Math.abs(flow_values[0][j]);
      }
      if (Math.abs(flow_values[1][j]) > scale) {
        scale = (float) Math.abs(flow_values[1][j]);
      }
      if (Math.abs(flow_values[2][j]) > scale) {
        scale = (float) Math.abs(flow_values[2][j]);
      }
    }
    float inv_scale = 1.0f / scale;
    if (inv_scale != inv_scale)
      inv_scale = 1.0f;
    /*
     * System.out.println("spatial_values = " + spatial_values[0][0] + " " +
     * spatial_values[1][0] + " " + spatial_values[2][0]);
     */
    // convert spatial DisplayRealType values to earth coordinates
    float[][] base_spatial_locs = new float[3][]; // WLH 9 Dec 99
    float[][] earth_locs = renderer.spatialToEarth(spatial_values,
        base_spatial_locs);
    if (earth_locs == null)
      return flow_values;
    int elen = earth_locs.length; // 2 or 3
    /*
     * System.out.println("earth_locs = " + earth_locs[0][0] + " " +
     * earth_locs[1][0]);
     */

    // convert earth coordinate Units to (radian, radian, meter)
    boolean other_meters = false;
    Unit[] earth_units = renderer.getEarthUnits();
    if (earth_units != null) {
      if (Unit.canConvert(earth_units[0], CommonUnit.radian)) {
        earth_locs[0] = CommonUnit.radian.toThis(earth_locs[0], earth_units[0]);
      }
      if (Unit.canConvert(earth_units[1], CommonUnit.radian)) {
        earth_locs[1] = CommonUnit.radian.toThis(earth_locs[1], earth_units[1]);
      }
      if (elen == 3 && earth_units.length == 3
          && Unit.canConvert(earth_units[2], CommonUnit.meter)) {
        other_meters = true;
        earth_locs[2] = CommonUnit.meter.toThis(earth_locs[2], earth_units[2]);
      }
    }
    /*
     * System.out.println("radian earth_locs = " + earth_locs[0][0] + " " +
     * earth_locs[1][0]);
     */
    // add scaled flow vector to earth location
    if (elen == 3) {
      // assume meters even if other_meters == false
      float factor_lat = (float) (inv_scale * 1000.0f * Data.DEGREES_TO_RADIANS / METERS_PER_DEGREE);
      float factor_vert = inv_scale * 1000.0f;
      if (!isTraj) {
        for (int j = 0; j < flen; j++) {
          earth_locs[2][j] += factor_vert * flow_values[2][j];
          earth_locs[1][j] += factor_lat * flow_values[0][j]
              / ((float) Math.cos(earth_locs[0][j]));
          earth_locs[0][j] += factor_lat * flow_values[1][j];
        }
      }
      else {
        for (int j = 0; j < flen; j++) {
          earth_locs[2][j] += flow_values[2][j] * timeStep;

          earth_locs[1][j] += ((flow_values[0][j] * timeStep * (1f/METERS_PER_DEGREE)) / ((float)Math.cos(earth_locs[0][j])) ) * Data.DEGREES_TO_RADIANS;

          earth_locs[0][j] += flow_values[1][j] * timeStep * (1f/METERS_PER_DEGREE) * Data.DEGREES_TO_RADIANS;
        }
      }
    } else {
      float factor_lat = 0.00001f * inv_scale
          * (0.5f * renderer.getLatLonRange());
      for (int j = 0; j < flen; j++) {
        earth_locs[1][j] += factor_lat * flow_values[0][j]
            / ((float) Math.cos(earth_locs[0][j]));
        earth_locs[0][j] += factor_lat * flow_values[1][j];
      }
    }
    /*
     * System.out.println("flow earth_locs = " + earth_locs[0][0] + " " +
     * earth_locs[1][0]);
     */
    // convert earth coordinate Units from (radian, radian, meter)
    if (earth_units != null) {
      if (Unit.canConvert(earth_units[0], CommonUnit.radian)) {
        earth_locs[0] = CommonUnit.radian.toThat(earth_locs[0], earth_units[0]);
      }
      if (Unit.canConvert(earth_units[1], CommonUnit.radian)) {
        earth_locs[1] = CommonUnit.radian.toThat(earth_locs[1], earth_units[1]);
      }
      if (elen == 3 && earth_units.length == 3
          && Unit.canConvert(earth_units[2], CommonUnit.meter)) {
        earth_locs[2] = CommonUnit.meter.toThat(earth_locs[2], earth_units[2]);
      }
    }
    /*
     * System.out.println("degree earth_locs = " + earth_locs[0][0] + " " +
     * earth_locs[1][0]);
     */
    // convert earth coordinates to spatial DisplayRealType values
    if (elen == 3) {
      earth_locs = renderer.earthToSpatial(earth_locs, null, base_spatial_locs);
    } else {
      // apply vertical flow in earthToSpatial
      float factor_vert = 0.00001f * inv_scale;
      float[] vert = new float[flen];
      for (int j = 0; j < flen; j++) {
        vert[j] = factor_vert * flow_values[2][j];
      }
      earth_locs = renderer.earthToSpatial(earth_locs, vert, base_spatial_locs);
      for (int i=0; i<earth_locs.length; i++) {
        if (earth_locs[i] == null) {
          earth_locs[i] = new float[flen];
            System.arraycopy(spatial_values[i], 0, earth_locs[i], 0, flen);
        }
      }
    }
    /*
     * System.out.println("spatial earth_locs = " + earth_locs[0][0] + " " +
     * earth_locs[1][0] + " " + earth_locs[2][0]);
     */
    // flow = change in spatial_values
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < flen; j++) {
        earth_locs[i][j] -= spatial_values[i][j];
      }
    }
    /*
     * System.out.println("vector earth_locs = " + earth_locs[0][0] + " " +
     * earth_locs[1][0] + " " + earth_locs[2][0]);
     */
    // combine earth_locs direction with flow_values magnitude
    for (int j = 0; j < flen; j++) {
      float mag = (float) Math.sqrt(flow_values[0][j] * flow_values[0][j]
          + flow_values[1][j] * flow_values[1][j] + flow_values[2][j]
          * flow_values[2][j]);
      float new_mag = (float) Math.sqrt(earth_locs[0][j] * earth_locs[0][j]
          + earth_locs[1][j] * earth_locs[1][j] + earth_locs[2][j]
          * earth_locs[2][j]);
      float ratio = mag / new_mag;
      if (!isTraj) {
        flow_values[0][j] = ratio * earth_locs[0][j];
        flow_values[1][j] = ratio * earth_locs[1][j];
        flow_values[2][j] = ratio * earth_locs[2][j];
      }
      else {
        flow_values[0][j] = earth_locs[0][j];
        flow_values[1][j] = earth_locs[1][j];
        flow_values[2][j] = earth_locs[2][j];
      }
    }
/*
System.out.println("adjusted flow values = " + flow_values[0][0] + " " +
                   flow_values[1][0] + " " + flow_values[2][0]);
*/
    testFlow("adjust", flow_values);
    return flow_values;
  }

  public VisADGeometryArray[] makeStreamline(int which, float[][] flow_values,
      float flowScale, float[][] spatial_values, Set spatial_set,
      int spatialManifoldDimension, byte[][] color_values,
      boolean[][] range_select, int valueArrayLength, int[] valueToMap,
      Vector MapVector) throws VisADException {

    if (flow_values[0] == null)
      return null;
    if (spatial_set == null)
      return null;

    if (which == 0 && !streamline1)
      return null;
    if (which == 1 && !streamline2)
      return null;

    if (!(spatial_set instanceof Gridded3DSet))
      return null;

    // -- TDR, only 2 possibilities: (0,1),(0,2),(1,2) because streamline
    // algorithm, adapted from Vis5D, only works with 2 flow components.
    // 2004-01-14
    FunctionType ftype = (FunctionType) Type;
    RealTupleType rtt = ftype.getFlatRange();
    RealType[] range_reals = rtt.getRealComponents();

    int flow_dim0 = -1;
    int flow_dim1 = -1;
    int cnt_flow1_maps = 0;
    int cnt_flow2_maps = 0;
    
    for (int k = 0; k < range_reals.length; k++) {
      for (int i = 0; i < valueArrayLength; i++) {
        ScalarMap map = (ScalarMap) MapVector.elementAt(valueToMap[i]);
        DisplayRealType dreal = map.getDisplayScalar();
        ScalarType scalar = map.getScalar();
        if (!scalar.equals(range_reals[k]))
          continue;
        if ((dreal.equals(Display.Flow1X)) || dreal.equals(Display.Flow1Elevation)) {
          if (flow_dim0 == -1) {
            flow_dim0 = 0;
          } else {
            flow_dim0 = 0;
            flow_dim1 = 1;
          }
          cnt_flow1_maps++;
        }
        if ((dreal.equals(Display.Flow2X)) || dreal.equals(Display.Flow2Elevation)) {
          if (flow_dim0 == -1) {
            flow_dim0 = 0;
          } else {
            flow_dim0 = 0;
            flow_dim1 = 1;
          }
          cnt_flow2_maps++;
        }
        if (dreal.equals(Display.Flow1Y) || dreal.equals(Display.Flow1Azimuth)) {
          if (flow_dim0 == -1) {
            flow_dim0 = 1;
          } else {
            flow_dim1 = 1;
          }
          cnt_flow1_maps++;
        }
        if (dreal.equals(Display.Flow2Y) ||dreal.equals(Display.Flow2Azimuth)) {
          if (flow_dim0 == -1) {
            flow_dim0 = 1;
          } else {
            flow_dim1 = 1;
          }
          cnt_flow2_maps++;
        }        
        if (dreal.equals(Display.Flow1Z) || dreal.equals(Display.Flow1Radial)) {
          flow_dim1 = 2;
          cnt_flow1_maps++;
        }
        if (dreal.equals(Display.Flow2Z) || dreal.equals(Display.Flow2Radial)) {
          flow_dim1 = 2;
          cnt_flow2_maps++;
        }
      }
    }

    if (cnt_flow1_maps > 2 || cnt_flow2_maps > 2) {
      throw new BadMappingException("Streamlines: Flow tuple dimension must be two");
    }
    
    if (flow_dim0 == -1 || flow_dim1 == -1) {
      throw new BadMappingException("Streamlines: Flow tuple dimension must be two");
    }
    // - 2004-01-14 ------------------------------------------------------

    if (range_select[0] != null) {
      if ((range_select[0].length == 1) && (!range_select[0][0]))
        return null;
      for (int ii = 0; ii < range_select[0].length; ii++) {
        if (!range_select[0][ii]) {
          flow_values[0][ii] = Float.NaN;
          flow_values[1][ii] = Float.NaN;
          // -TDR, 2004-01-15
          flow_values[2][ii] = Float.NaN;
        }
      }
    }

    DataRenderer renderer = getLink().getRenderer();
    flow_values = adjustFlowToEarth(which, flow_values, spatial_values,
        flowScale, renderer);

    /*- start make streamline vertices
    System.out.println("----start streamlines----------"); */

    float density = 1;
    float arrowScale = 1;
    float stepFactor = 2;
    float packingFactor = 1;
    float cntrWeight = 3;
    int n_pass = 0;
    float reduction = 1f;

    int[] numl = new int[1];

    int[][] n_verts = new int[1][];
    float[][][] vr = new float[1][][];
    float[][][] vc = new float[1][][];
    int nr;
    int nc;

    if (which == 0)
      density = streamlineDensity1;
    if (which == 1)
      density = streamlineDensity2;
    if (which == 0)
      arrowScale = arrowScale1;
    if (which == 1)
      arrowScale = arrowScale2;
    if (which == 0)
      stepFactor = stepFactor1;
    if (which == 1)
      stepFactor = stepFactor2;
    if (which == 0)
      packingFactor = packingFactor1;
    if (which == 1)
      packingFactor = packingFactor2;
    if (which == 0)
      cntrWeight = cntrWeight1;
    if (which == 1)
      cntrWeight = cntrWeight2;
    if (which == 0)
      n_pass = n_pass1;
    if (which == 1)
      n_pass = n_pass2;
    if (which == 0)
      reduction = reduction1;
    if (which == 1)
      reduction = reduction2;

    if (spatialManifoldDimension == 2) {
      nc = ((Gridded3DSet) spatial_set).LengthX;
      nr = ((Gridded3DSet) spatial_set).LengthY;

      Gridded2DSet gset = new Gridded2DSet(
          RealTupleType.Generic2D,
          new float[][] { spatial_values[flow_dim0], spatial_values[flow_dim1] },
          nc, nr, null, null, null, false, false);

      Stream2D.stream(flow_values[flow_dim0], flow_values[flow_dim1], nr, nc,
          density, stepFactor, arrowScale, vr, vc, n_verts, numl, gset,
          packingFactor, cntrWeight, n_pass, reduction);
    } else {
      throw new VisADException(
          "only manifoldDimension==2 supported for streamlimes");
    }

    VisADLineArray[] arrays = new VisADLineArray[numl[0]];
    Integer2DSet grid_set = new Integer2DSet(nc, nr);

    for (int kk = 0; kk < arrays.length; kk++) {
      arrays[kk] = new VisADLineArray();
      float[][] grid = new float[2][n_verts[0][kk]];
      System.arraycopy(vr[0][kk], 0, grid[1], 0, n_verts[0][kk]);
      System.arraycopy(vc[0][kk], 0, grid[0], 0, n_verts[0][kk]);

      float[][] spatial_set_vals = ((Gridded3DSet) spatial_set)
          .gridToValue(grid);

      byte[][] intrp_color_values = null;
      if (color_values != null) {
        int len = color_values.length;
        intrp_color_values = new byte[len][n_verts[0][kk]];
        int[] indices = grid_set.valueToIndex(grid);
        for (int cc = 0; cc < n_verts[0][kk]; cc++) {
          if (indices[cc] >= 0) {
            intrp_color_values[0][cc] = color_values[0][indices[cc]];
            intrp_color_values[1][cc] = color_values[1][indices[cc]];
            intrp_color_values[2][cc] = color_values[2][indices[cc]];
            if (len > 3) {
              intrp_color_values[3][cc] = color_values[3][indices[cc]];
            }
          } else {
            intrp_color_values[0][cc] = (byte) 255;
            intrp_color_values[1][cc] = 0;
            intrp_color_values[2][cc] = 0;
            if (len > 3) {
              intrp_color_values[3][cc] = color_values[3][indices[cc]];
            }
          }
        }
      }

      Gridded3DSet.setGeometryArray(arrays[kk], spatial_set_vals, 4,
          intrp_color_values);
      arrays[kk] = (VisADLineArray) arrays[kk].removeMissing();
    }

    return arrays;
  }

  private static void testFlow(String id, float[][] flow_values) {
    int flen = flow_values[0].length;
    for (int i = 0; i < flen; i++) {
      if (flow_values[0][i] != flow_values[0][i]
          || flow_values[1][i] != flow_values[1][i]
          || flow_values[2][i] != flow_values[2][i]
          || Float.isInfinite(flow_values[0][i])
          || Float.isInfinite(flow_values[1][i])
          || Float.isInfinite(flow_values[2][i])) {
        // System.out.println(id + " " + i + " " + flow_values[0][i] + " " +
        // flow_values[1][i] + " " + flow_values[2][i]);
        flow_values[0][i] = 0.0f;
        flow_values[1][i] = 0.0f;
        flow_values[2][i] = 0.0f;
      }
    }
  }

  public void makeTrajFlow(int which, float[][] flow_values, Data data,
      float flowScale, float[][] spatial_values, Set spatial_set, int spatialManifoldDimension, byte[][] color_values,
      boolean[][] range_select, GraphicsModeControl mode, float constant_alpha, float[] constant_color,
      int valueArrayLength, int[] valueToMap,
      Vector MapVector, ArrayList flowInfoList) throws VisADException, RemoteException {
      
    // incoming spatial_set will be null (see assembleSpatial), so create from spatial_values.
    if (spatialManifoldDimension == 3) {
      SetType stype = new SetType(Display.DisplaySpatialCartesianTuple);
      Set domain_set = ((FlatField)data).getDomainSet();
      spatial_set = domain_set.makeSpatial(stype, spatial_values);
    }

    Unit[] flowUnits = new Unit[3];
    Unit[][] dataUnits = ((FlatField)data).getRangeUnits();
    float[][] rangeValues = ((Field)data).getFloats(false);
    
    if (flow_values[0] == null)
      return;
    if (spatial_values[0] == null)
      return;

    VisADLineArray array = new VisADLineArray();

    int len = spatial_values[0].length;
    int flen = flow_values[0].length;
    int rlen = 0; // number of non-missing values
    if (range_select[0] == null) {
      rlen = len;
    } else {
      for (int j = 0; j < range_select[0].length; j++) {
        if (range_select[0][j])
          rlen++;
      }
    }
    if (rlen == 0)
      return;

    DataRenderer renderer = getLink().getRenderer();

    FunctionType ftype = (FunctionType) Type;
    RealTupleType rtt = ftype.getFlatRange();
    RealType[] range_reals = rtt.getRealComponents();

    for (int k = 0; k < range_reals.length; k++) {
      for (int i = 0; i < valueArrayLength; i++) {
        ScalarMap map = (ScalarMap) MapVector.elementAt(valueToMap[i]);
        DisplayRealType dreal = map.getDisplayScalar();
        ScalarType scalar = map.getScalar();
        if (!scalar.equals(range_reals[k]))
          continue;

        if ( dreal.equals(Display.Flow1Elevation) || dreal.equals(Display.Flow2Elevation) ||
             dreal.equals(Display.Flow1Azimuth) || dreal.equals(Display.Flow2Azimuth) ||
             dreal.equals(Display.Flow1Radial) || dreal.equals(Display.Flow2Radial) )  {
           throw new VisADException("Elevation, Azimuth, Radial not supported for Trajectory");
        }

        if (dreal.equals(Display.Flow1X) || dreal.equals(Display.Flow2X)) {
          flowUnits[0] = dataUnits[k][0];
        }
        if (dreal.equals(Display.Flow1Y) || dreal.equals(Display.Flow2Y)) {
          flowUnits[1] = dataUnits[k][0];
        }
        if (dreal.equals(Display.Flow1Z) || dreal.equals(Display.Flow2Z)) {
          flowUnits[2] = dataUnits[k][0];
        }
      }
    }
    
    byte[][] trajColors = makeTrajColor(rangeValues, valueArrayLength,
                                   valueToMap, MapVector);

    FlowInfo flwInfo = new FlowInfo();
    flwInfo.flow_values = flow_values;
    flwInfo.flow_units = flowUnits;
    flwInfo.flowScale = flowScale;
    flwInfo.spatial_values = spatial_values;
    flwInfo.spatial_set = spatial_set;
    flwInfo.spatialManifoldDimension = spatialManifoldDimension;
    flwInfo.color_values = color_values;
    flwInfo.range_select = range_select;
    flwInfo.mode = mode;
    flwInfo.constant_alpha = constant_alpha;
    flwInfo.constant_color = constant_color;
    flwInfo.renderer = renderer;
    flwInfo.which = which;
    flwInfo.trajColors = trajColors;
    flwInfo.useCache = false;
    flowInfoList.add(flwInfo);
    
    if ((which == 0 && cacheTrajFlow1) || (which == 1 && cacheTrajFlow2)){
      flwInfo.useCache = true;
      flwInfo.flowObjId = DataCacheManager.getCacheManager().addToCache(flow_values);
      flwInfo.flow_values = null;
      flwInfo.colorObjId = DataCacheManager.getCacheManager().addToCache(color_values);
      flwInfo.color_values = null;
    }

    return;
  }

  /* TODO: How, or should this be integrated with assembleColor? */
  public byte[][] makeTrajColor(float[][] rangeValues, int valueArrayLength, int[] valueToMap,
      Vector MapVector) throws VisADException, RemoteException {

    FunctionType ftype = (FunctionType) Type;
    RealTupleType rtt = ftype.getFlatRange();
    RealType[] range_reals = rtt.getRealComponents();

    ScalarMap colorMap = null;
    int rngIdxClr = -1;

    int cnt = 0;
    for (int k = 0; k < range_reals.length; k++) {
      for (int i = 0; i < valueArrayLength; i++) {
        ScalarMap map = (ScalarMap) MapVector.elementAt(valueToMap[i]);
        DisplayRealType dreal = map.getDisplayScalar();
        ScalarType scalar = map.getScalar();
        if (!scalar.equals(range_reals[k]))
          continue;

        if (dreal.equals(Display.RGB) || dreal.equals(Display.RGBA)) {
           colorMap = map;
           if (rngIdxClr < 0) {
             rngIdxClr = k;
           }
           cnt++;
        }
      }
    }

    byte[][] color_values = null;
            
    if (colorMap == null) {
      return null;
    }
    if (cnt > 1) {
      System.out.println("Trajectory: more than one range type for color. Can only use the first found");
    }
    float[] dspVals = colorMap.scaleValues(rangeValues[rngIdxClr]);
    float[][] fltClrs = ((BaseColorControl)colorMap.getControl()).lookupValues(dspVals);
    int clrDim = fltClrs.length;
    color_values = new byte[clrDim][fltClrs[0].length];
    for (int d=0; d<clrDim; d++) {
      for (int t=0; t<color_values[0].length; t++) {
        color_values[d][t] = ShadowType.floatToByte(fltClrs[d][t]);
      }
    }
    return color_values;
  }

  private static final float BACK_SCALE = -0.15f;
  private static final float PERP_SCALE = 0.15f;

  /** which = 0 for Flow1 and which = 1 for Flow2 */
  public VisADGeometryArray[] makeFlow(int which, float[][] flow_values,
      float flowScale, float arrowScale, float[][] spatial_values, byte[][] color_values,
      boolean[][] range_select) throws VisADException {
    if (flow_values[0] == null)
      return null;
    if (spatial_values[0] == null)
      return null;

    VisADLineArray array = new VisADLineArray();

    int len = spatial_values[0].length;
    int flen = flow_values[0].length;
    int rlen = 0; // number of non-missing values
    if (range_select[0] == null) {
      rlen = len;
    } else {
      for (int j = 0; j < range_select[0].length; j++) {
        if (range_select[0][j])
          rlen++;
      }
    }
    if (rlen == 0)
      return null;


    DataRenderer renderer = getLink().getRenderer();
    flow_values = adjustFlowToEarth(which, flow_values, spatial_values,
        flowScale, renderer);

    array.vertexCount = 6 * rlen;

    float[] coordinates = new float[18 * rlen];
    int m = 0;
    // flow vector
    float f0 = 0.0f, f1 = 0.0f, f2 = 0.0f;
    // arrow head vector
    float a0 = 0.0f, a1 = 0.0f, a2 = 0.0f;
    float b0 = 0.0f, b1 = 0.0f, b2 = 0.0f;
    for (int j = 0; j < len; j++) {
      if (range_select[0] == null || range_select[0][j]) {
        if (flen == 1) {
          f0 = flowScale * flow_values[0][0];
          f1 = flowScale * flow_values[1][0];
          f2 = flowScale * flow_values[2][0];
        } else {
          f0 = flowScale * flow_values[0][j];
          f1 = flowScale * flow_values[1][j];
          f2 = flowScale * flow_values[2][j];
        }
        int k = m;
        // base point of flow vector
        coordinates[m++] = spatial_values[0][j];
        coordinates[m++] = spatial_values[1][j];
        coordinates[m++] = spatial_values[2][j];
        int n = m;
        // k = orig m
        // m = orig m + 3
        // end point of flow vector
        coordinates[m++] = coordinates[k++] + f0;
        coordinates[m++] = coordinates[k++] + f1;
        coordinates[m++] = coordinates[k++] + f2;
        k = n;
        // n = orig m + 3
        // m = orig m + 6
        // repeat end point of flow vector as
        // first point of first arrow head
        coordinates[m++] = coordinates[n++];
        coordinates[m++] = coordinates[n++];
        coordinates[m++] = coordinates[n++];
        boolean mode2d = display.getDisplayRenderer().getMode2D();
        b0 = a0 = BACK_SCALE * arrowScale * f0;
        b1 = a1 = BACK_SCALE * arrowScale * f1;
        b2 = a2 = BACK_SCALE * arrowScale * f2;

        if (mode2d
            || (Math.abs(f2) <= Math.abs(f0) && Math.abs(f2) <= Math.abs(f1))) {
          a0 += PERP_SCALE * arrowScale * f1;
          a1 -= PERP_SCALE * arrowScale * f0;
          b0 -= PERP_SCALE * arrowScale * f1;
          b1 += PERP_SCALE * arrowScale * f0;
        } else if (Math.abs(f1) <= Math.abs(f0)) {
          a0 += PERP_SCALE * arrowScale * f2;
          a2 -= PERP_SCALE * arrowScale * f0;
          b0 -= PERP_SCALE * arrowScale * f2;
          b2 += PERP_SCALE * arrowScale * f0;
        } else { // f0 is least
          a1 += PERP_SCALE * arrowScale * f2;
          a2 -= PERP_SCALE * arrowScale * f1;
          b1 -= PERP_SCALE * arrowScale * f2;
          b2 += PERP_SCALE * arrowScale * f1;
        }

        k = n;
        // n = orig m + 6
        // m = orig m + 9
        // second point of first arrow head
        coordinates[m++] = coordinates[n++] + a0;
        coordinates[m++] = coordinates[n++] + a1;
        coordinates[m++] = coordinates[n++] + a2;

        n = k;
        // k = orig m + 6
        // first point of second arrow head
        coordinates[m++] = coordinates[k++];
        coordinates[m++] = coordinates[k++];
        coordinates[m++] = coordinates[k++];

        // n = orig m + 6
        // second point of second arrow head
        coordinates[m++] = coordinates[n++] + b0;
        coordinates[m++] = coordinates[n++] + b1;
        coordinates[m++] = coordinates[n++] + b2;
      }
    }
    array.coordinates = coordinates;
    // array.vertexFormat = COORDINATES;

    if (color_values != null) {
      int c_len = color_values.length;
      byte[] colors = new byte[6 * c_len * rlen];
      m = 0;
      float c0 = 0.0f, c1 = 0.0f, c2 = 0.0f;
      for (int j = 0; j < len; j++) {
        if (range_select[0] == null || range_select[0][j]) {
          int k1 = m;
          int k2 = m;
          int k3 = m;
          int k4 = m;
          int k5 = m;
          // repeat color 6 times
          colors[m++] = color_values[0][j];
          colors[m++] = color_values[1][j];
          colors[m++] = color_values[2][j];
          if (c_len == 4)
            colors[m++] = color_values[3][j];
          colors[m++] = colors[k1++];
          colors[m++] = colors[k1++];
          colors[m++] = colors[k1++];
          if (c_len == 4)
            colors[m++] = colors[k1++];
          colors[m++] = colors[k2++];
          colors[m++] = colors[k2++];
          colors[m++] = colors[k2++];
          if (c_len == 4)
            colors[m++] = colors[k2++];
          colors[m++] = colors[k3++];
          colors[m++] = colors[k3++];
          colors[m++] = colors[k3++];
          if (c_len == 4)
            colors[m++] = colors[k3++];
          colors[m++] = colors[k4++];
          colors[m++] = colors[k4++];
          colors[m++] = colors[k4++];
          if (c_len == 4)
            colors[m++] = colors[k4++];
          colors[m++] = colors[k5++];
          colors[m++] = colors[k5++];
          colors[m++] = colors[k5++];
          if (c_len == 4)
            colors[m++] = colors[k5++];
        }
      }
      array.colors = colors;
      // array.vertexFormat |= COLOR_3;
    }

    // WLH 30 May 2002
    if (getAdjustProjectionSeam()) {
      array = (VisADLineArray) array.adjustLongitudeBulk(renderer);
    }

    return new VisADGeometryArray[] { array };
  }

  private static final double FONT_SCALE = 0.07;

  /**
   * abcd - 2 February 2001 Rotate the base and up vectors
   * 
   * Rotation is in degrees clockwise from positive X axis
   */
  static void rotateVectors(double[] base, double[] up, double rotationDegrees) {
    double rotation = Data.DEGREES_TO_RADIANS * rotationDegrees;
    double sinRotation = Math.sin(rotation);
    double cosRotation = Math.cos(rotation);
    double[] newBase = new double[3];
    double[] newUp = new double[3];

    // Check if no rotation is needed
    if (rotationDegrees == 0.0) {
      return;
    }

    // For each axis
    for (int i = 0; i < 3; i++) {
      // Rotate the point
      newBase[i] = cosRotation * base[i] - sinRotation * up[i];
      newUp[i] = sinRotation * base[i] + cosRotation * up[i];
    }

    // Copy data back to arrays
    System.arraycopy(newBase, 0, base, 0, 3);
    System.arraycopy(newUp, 0, up, 0, 3);
  }

  public VisADGeometryArray makeText(String[] text_values,
      TextControl text_control, float[][] spatial_values,
      byte[][] color_values, boolean[][] range_select) throws VisADException {
    if (text_values == null || text_values.length == 0 || text_control == null)
      return null;

    if (spatial_values[0] == null)
      return null;

    byte r = 0;
    byte g = 0;
    byte b = 0;
    byte a = 0;
    int color_length = 0;
    if (color_values != null) {
      color_length = color_values.length;
      r = color_values[0][0];
      g = color_values[1][0];
      b = color_values[2][0];
      if (color_length > 3)
        a = color_values[3][0];
    }

    int n = text_values.length;

    // CTR 22 Jan 2001
    if (n > spatial_values[0].length)
      n = spatial_values[0].length;

    VisADGeometryArray[] as = new VisADGeometryArray[n];
    // abcd 5 February 2001
    // boolean center = text_control.getCenter();
    TextControl.Justification justification = text_control.getJustification();
    // abcd 19 March 2003
    TextControl.Justification verticalJustification = text_control
        .getVerticalJustification();
    double size = text_control.getSize();
    Font font = text_control.getFont();
    HersheyFont hfont = text_control.getHersheyFont();
    // SL 22 June 2003
    double rotation = text_control.getRotation();
    double characterRotation = text_control.getCharacterRotation();
    double scale = text_control.getScale();
    double[] offset = text_control.getOffset();

    // WLH 31 May 2000
    boolean sphere = text_control.getSphere();
    float[][] spatial_sphere = null;
    if (sphere) {
      spatial_sphere = Display.DisplaySphericalCoordSys
          .fromReference(spatial_values);
    }

    double[] start = new double[3];
    double[] base = new double[] { size * FONT_SCALE, 0.0, 0.0 };
    double[] up = new double[] { 0.0, size * FONT_SCALE, 0.0 };

    // abcd 2 February 2001
    // This cannot be moved outside the for loop
    rotateVectors(base, up, text_control.getRotation());

    int k = 0;
    for (int i = 0; i < n; i++) {
      if (range_select[0] == null || range_select[0].length == 1
          || range_select[0][i]) {
        /*
         * System.out.println("makeText, i = " + i + " text = " + text_values[i]
         * + " spatial_values = " + spatial_values[0][i] + " " +
         * spatial_values[1][i] + " " + spatial_values[2][i]);
         */
        if (sphere) {
          double size_in_radians = (size * FONT_SCALE) / spatial_sphere[2][i];
          double size_in_degrees = size_in_radians * Data.RADIANS_TO_DEGREES;
          double lon_size_in_degrees = size_in_degrees
              / Math.cos(Data.DEGREES_TO_RADIANS * spatial_sphere[0][i]);
          start = new double[] { spatial_sphere[0][i], spatial_sphere[1][i],
              spatial_sphere[2][i] };
          base = new double[] { 0.0, lon_size_in_degrees, 0.0 };
          up = new double[] { size_in_degrees, 0.0, 0.0 };

          // abcd 2 February 2001
          // This cannot be moved outside the for loop
          rotateVectors(base, up, text_control.getRotation());

          if (font != null) {
            as[k] = PlotText.render_font(text_values[i], font, start, base, up,
                justification, verticalJustification, characterRotation, scale,
                offset);
          } else if (hfont != null) {
            as[k] = PlotText.render_font(text_values[i], hfont, start, base,
                up, justification, verticalJustification, characterRotation,
                scale, offset);

          } else {
            as[k] = PlotText.render_label(text_values[i], start, base, up,
                justification, verticalJustification, characterRotation, scale,
                offset);
          }
          int len = (as[k] == null) ? 0 : as[k].coordinates.length;
          if (len > 0) {
            float[] coordinates = as[k].coordinates;
            float[][] cs = new float[3][len / 3];
            int m = 0;
            for (int j = 0; j < len / 3; j++) {
              cs[0][j] = coordinates[m++];
              cs[1][j] = coordinates[m++];
              cs[2][j] = coordinates[m++];
            }
            cs = Display.DisplaySphericalCoordSys.toReference(cs);
            m = 0;
            for (int j = 0; j < len / 3; j++) {
              coordinates[m++] = cs[0][j];
              coordinates[m++] = cs[1][j];
              coordinates[m++] = cs[2][j];
            }
            as[k].coordinates = coordinates; // not necessary
            if (font != null) {
              float[] normals = as[k].normals;
              for (int j3 = 0; j3 < len; j3 += 3) {
                float c = (float) Math.sqrt(coordinates[j3 + 0]
                    * coordinates[j3 + 0] + coordinates[j3 + 1]
                    * coordinates[j3 + 1] + coordinates[j3 + 2]
                    * coordinates[j3 + 2]);
                float cinv = (c == 0.0f) ? 1.0f : 1.0f / c;
                normals[j3 + 0] = cinv * coordinates[j3 + 0];
                normals[j3 + 1] = cinv * coordinates[j3 + 1];
                normals[j3 + 2] = cinv * coordinates[j3 + 2];
              }
              as[k].normals = normals; // not necessary
            }
          }
        } else { // !sphere
          start = new double[] { spatial_values[0][i], spatial_values[1][i],
              spatial_values[2][i] };
          if (font != null) {
            as[k] = PlotText.render_font(text_values[i], font, start, base, up,
                justification, verticalJustification, characterRotation, scale,
                offset);

          } else if (hfont != null) {
            as[k] = PlotText.render_font(text_values[i], hfont, start, base,
                up, justification, verticalJustification, characterRotation,
                scale, offset);

          } else {
            as[k] = PlotText.render_label(text_values[i], start, base, up,
                justification, verticalJustification, characterRotation, scale,
                offset);
          }
        }

        int len = (as[k] == null) ? 0 : as[k].coordinates.length;
        int numPts = len / 3;
        if (len > 0 && color_values != null) {
          byte[] colors = new byte[numPts * color_length];
          if (color_values[0].length > 1) {
            r = color_values[0][k];
            g = color_values[1][k];
            b = color_values[2][k];
            if (color_length > 3)
              a = color_values[3][k];
          }
          for (int j = 0; j < colors.length; j += color_length) {
            colors[j] = r;
            colors[j + 1] = g;
            colors[j + 2] = b;
            if (color_length > 3)
              colors[j + 3] = a;
          }
          as[k].colors = colors;
        }
        k++;
      }
    } // end for (int i=0; i<n; i++)
    if (k == 0)
      return null;
    VisADGeometryArray[] arrays = new VisADGeometryArray[k];
    System.arraycopy(as, 0, arrays, 0, k);
    VisADGeometryArray array = null;

    // WLH 30 May 2002
    DataRenderer renderer = getLink().getRenderer();
    for (int i = 0; i < k; i++) {
      if (arrays[i] != null) {
        if (getAdjustProjectionSeam()) {
          arrays[i] = arrays[i].adjustLongitudeBulk(renderer);
        }
        if (array == null)
          array = (VisADGeometryArray) arrays[i].clone();
      }
    }
    if (array != null)
      VisADGeometryArray.merge(arrays, array);
    return array;
  }

  /**
   * composite and transform color and Alpha DisplayRealType values from
   * display_values, and return as (Red, Green, Blue, Alpha)
   */
  public byte[][] assembleColor(float[][] display_values, int valueArrayLength,
      int[] valueToScalar, DisplayImpl display, float[] default_values,
      boolean[][] range_select, boolean[] single_missing, ShadowType shadow_api)
      throws VisADException, RemoteException {
    float[][] rgba_values = new float[4][];
    float[] rgba_value_counts = { 0.0f, 0.0f, 0.0f, 0.0f };
    float[] rgba_singles = new float[4];
    float[] rgba_single_counts = { 0.0f, 0.0f, 0.0f, 0.0f };
    float[][] tuple_values = new float[3][];
    float[] tuple_value_counts = { 0.0f, 0.0f, 0.0f };
    float[] tuple_singles = new float[3];
    float[] tuple_single_counts = { 0.0f, 0.0f, 0.0f };

    // mark array to keep track of which valueIndices have
    // contributed to display color_tuples
    boolean[] mark = new boolean[valueArrayLength];
    for (int i = 0; i < valueArrayLength; i++)
      mark[i] = false;

    // loop to assemble values for each different
    // display color_tuple
    while (true) {
      DisplayTupleType color_tuple = null;
      for (int i = 0; i < valueArrayLength; i++) {
        float[] values = display_values[i];
        if (values != null && !mark[i]) {
          int len = values.length;
          int displayScalarIndex = valueToScalar[i];
          DisplayRealType real = display.getDisplayScalar(displayScalarIndex);
          DisplayTupleType tuple = real.getTuple();
          // check whether this real is part of a display color tuple
          if (tuple != null
              && (tuple.equals(Display.DisplayRGBTuple) || (tuple
                  .getCoordinateSystem() != null && tuple.getCoordinateSystem()
                  .getReference().equals(Display.DisplayRGBTuple)))) {
            if (color_tuple == null || color_tuple.equals(tuple)) {
              if (color_tuple == null) {
                // start a new color_tuple
                color_tuple = tuple;
                for (int j = 0; j < 3; j++) {
                  tuple_singles[j] = 0.0f;
                  tuple_single_counts[j] = 0.0f;
                  tuple_values[j] = null;
                  tuple_value_counts[j] = 0.0f;
                }
              }
              int index = real.getTupleIndex();
              if (len == 1) {
                tuple_singles[index] += values[0];
                tuple_single_counts[index]++;
              } else { // (len != 1)
                singleComposite(index, tuple_values, tuple_value_counts, values);
              }
              // FREE
              display_values[i] = null; // MEM_WLH 27 March 99
              mark[i] = true;
            } // end if (color_tuple == null || color_tuple.equals(tuple))
          } // end if component of a color tuple
        } // end if (values != null && !mark[i])
      } // end for (int i=0; i<valueArrayLength; i++)
      if (color_tuple != null) {
        colorSum(3, tuple_values, tuple_value_counts, tuple_singles,
            tuple_single_counts, display, color_tuple, default_values);
        if (!color_tuple.equals(Display.DisplayRGBTuple)) {
          // equalize all rgba_values[index] to same length
          // and fill with default values
          equalizeAndDefault(tuple_values, display, color_tuple, default_values);
          // transform tuple_values to DisplayRGBTuple
          CoordinateSystem coord = color_tuple.getCoordinateSystem();
          tuple_values = coord.toReference(tuple_values);
        }
        colorComposite(rgba_values, rgba_value_counts, tuple_values);
      } else { // if (color_tuple == null)
        // no new color_tuple found on this loop iteration
        break;
      }
    } // end while (true)

    int[] valueToMap = display.getValueToMap();
    Vector MapVector = display.getMapVector();
    for (int i = 0; i < valueArrayLength; i++) {
      float[] values = display_values[i];
      if (values != null && !mark[i]) {
        int len = values.length;
        int displayScalarIndex = valueToScalar[i];
        DisplayRealType real = display.getDisplayScalar(displayScalarIndex);
        if (real.equals(Display.RGB) || real.equals(Display.HSV)
            || real.equals(Display.CMY)) {
          ColorControl control = (ColorControl) ((ScalarMap) MapVector
              .elementAt(valueToMap[i])).getControl();
          /*
           * ScalarMap map = (ScalarMap) MapVector.elementAt(valueToMap[i]);
           * System.out.println("map = " + map); int nummissing = 0; for (int
           * k=0; k<values.length; k++) { if (values[k] != values[k])
           * nummissing++; } System.out.println("values: nummissing = " +
           * nummissing);
           */

          float[][] color_values = control.lookupValues(values);

          /*
           * nummissing = 0; for (int k=0; k<color_values[0].length; k++) { if
           * (color_values[0][k] != color_values[0][k]) nummissing++; }
           * System.out.println("color_values: nummissing = " + nummissing);
           */
          if (real.equals(Display.HSV)) {
            // transform color_values to DisplayRGBTuple
            color_values = Display.DisplayHSVCoordSys.toReference(color_values);
          } else if (real.equals(Display.CMY)) {
            // transform color_values to DisplayRGBTuple
            color_values = Display.DisplayCMYCoordSys.toReference(color_values);
          } else if (real.equals(Display.RGB)) {
            // do nothing
          } else {
            throw new DisplayException("unrecognized color CoordinateSsystem: "
                + "ShadowType.assembleColor");
          }
          if (len == 1) {
            for (int index = 0; index < 3; index++) {
              rgba_singles[index] += color_values[index][0];
              rgba_single_counts[index]++;
            }
          } else { // (len != 1)
            colorComposite(rgba_values, rgba_value_counts, color_values);
          }
          // FREE
          display_values[i] = null; // MEM_WLH 27 March 99
        } // end if (real.equals(Display.RGB) || HSV || CMY)
        if (real.equals(Display.RGBA)) {
          ColorAlphaControl control = (ColorAlphaControl) ((ScalarMap) MapVector
              .elementAt(valueToMap[i])).getControl();
          float[][] color_values = control.lookupValues(values);
          if (len == 1) {
            for (int index = 0; index < 4; index++) {
              rgba_singles[index] += color_values[index][0];
              rgba_single_counts[index]++;
            }
          } else { // (len != 1)
            colorComposite(rgba_values, rgba_value_counts, color_values);

            for (int index = 0; index < 4; index++) {
              singleComposite(index, rgba_values, rgba_value_counts,
                  color_values[index]);
              // FREE
              color_values[index] = null;
            }
          }
          // FREE
          display_values[i] = null; // MEM_WLH 27 March 99
        } // end if (real.equals(Display.RGBA))
        if (real.equals(Display.Alpha)) {
          if (len == 1) {
            rgba_singles[3] += values[0];
            rgba_single_counts[3]++;
          } else {
            singleComposite(3, rgba_values, rgba_value_counts, values);
          }
          // FREE
          display_values[i] = null; // MEM_WLH 27 March 99
        } // end if (real.equals(Display.Alpha))
        // no need for 'mark[i] = true;' in this loop
      } // end if (values != null && !mark[i])
    } // end for (int i=0; i<valueArrayLength; i++)
    if (rgba_values[0] == null && rgba_values[1] == null
        && rgba_values[2] == null && rgba_values[3] == null) {
      // no long color vectors, so try singles, then defaults
      for (int index = 0; index < 4; index++) {
        rgba_values[index] = new float[1];
        if (rgba_single_counts[index] > 0) {
          rgba_values[index][0] = rgba_singles[index]
              / rgba_single_counts[index];
        } else {
          // nothing mapped to this color component, so use default
          int default_index = getDefaultColorIndex(display, index);
          /*
           * WLH 7 Feb 98 int default_index = index == 0 ?
           * display.getDisplayScalarIndex(Display.Red) : (index == 1 ?
           * display.getDisplayScalarIndex(Display.Green) : (index == 2 ?
           * display.getDisplayScalarIndex(Display.Blue) :
           * display.getDisplayScalarIndex(Display.Alpha) ) );
           */
          rgba_values[index][0] = default_values[default_index];
        }
      }
    } else {
      colorSum(4, rgba_values, rgba_value_counts, rgba_singles,
          rgba_single_counts, display, Display.DisplayRGBTuple, default_values);
      // equalize all rgba_values[index] to same length
      // and fill with default values
      equalizeAndDefault(rgba_values, display, Display.DisplayRGBTuple,
          default_values);
    }

    // test for any missing values
    int big_len = rgba_values[0].length;
    for (int i = 0; i < 4; i++) {
      int len = rgba_values[i].length;
      for (int j = 0; j < len; j++) {
        if (rgba_values[i][j] != rgba_values[i][j]) {
          if (range_select[0] == null) {
            range_select[0] = new boolean[big_len];
            for (int k = 0; k < big_len; k++)
              range_select[0][k] = true;
          }
          if (len > 1) {
            range_select[0][j] = false;
            rgba_values[i][j] = 0.0f;
          } else {
            for (int k = 0; k < big_len; k++)
              range_select[0][k] = false;
            // leave any single color value missing -
            // this will prevent anything from being rendered
            // MEM_WLH
            rgba_values[i][j] = 0.0f;
            single_missing[i] = true;
          }
        }
      } // end for (int j=0; j<len; j++)
    } // end for (int i=0; i<4; i++)

    //
    // TO_DO
    // should colors be clamped to range (0.0f, 1.0f)?
    //

    /*
     * MEM_WLH return rgba_values;
     */
    // MEM_WLH
    // page 291 of Java3D book says byte colors are [0, 255] range
    byte[][] b = new byte[rgba_values.length][];
    for (int i = 0; i < rgba_values.length; i++) {
      if (rgba_values[i] != null) {
        int len = rgba_values[i].length;
        b[i] = new byte[len];
        for (int j = 0; j < len; j++) {
          int k = (int) (rgba_values[i][j] * 255.0);
          k = (k < 0) ? 0 : (k > 255) ? 255 : k;
          b[i][j] = (byte) ((k < 128) ? k : k - 256);
        }
      }
    }
    return b;
  }

  public static final float byteToFloat(byte b) {
    return (b < 0) ? (((float) b) + 256.0f) / 255.0f : ((float) b) / 255.0f;
    //
    // no 255.0f divide:
    // return ((b < 0) ? ((float) b) + 256.0f : ((float) b));
  }

  public static final byte floatToByte(float f) {
    /*
     * int k = (int) (f 255.0); k = (k < 0) ? 0 : (k > 255) ? 255 : k; return
     * (byte) ((k < 128) ? k : k - 256);
     */
    int k = (int) (f * 255.0);
    return (byte) ((k < 0) ? 0 : ((k > 255) ? -1 : ((k < 128) ? k : k - 256)));
    //
    // no 255.0f multiply:
    // return ((byte) ( ((int) f) < 0 ? 0 : ((int) f) > 255 ? -1 :
    // ((int) f) < 128 ? ((byte) f) : ((byte) (f - 256.0f)) ));
  }

  static void colorSum(int nindex, float[][] tuple_values,
      float[] tuple_value_counts, float[] tuple_singles,
      float[] tuple_single_counts, DisplayImpl display, DisplayTupleType tuple,
      float[] default_values) throws VisADException {

    for (int index = nindex - 1; index >= 0; index--) {
      if (tuple_values[index] == null) {
        if (tuple_single_counts[index] > 0) {
          tuple_values[index] = new float[1];
          tuple_values[index][0] = tuple_singles[index];
          tuple_value_counts[index] = tuple_single_counts[index];
        }
      } else { // (tuple_values[index] != null)
        // DRM: 2003-09-19 allow for setting by ConstantMap
        // int cm = display.getGraphicsModeControl().getColorMode();
        int cm = (int) default_values[display
            .getDisplayScalarIndex(Display.ColorMode)];
        /*
         * DRM: 2005-09-25 default_values now has default from GMC int cm =
         * (colorMode >= 0) ? colorMode :
         * display.getGraphicsModeControl().getColorMode();
         */
        float inv_count = cm == GraphicsModeControl.SUM_COLOR_MODE ? 1.0f
            : 1.0f / (tuple_value_counts[index] + tuple_single_counts[index]);
        float[] t_values = tuple_values[index];
        for (int j = 0; j < t_values.length; j++) {
          if (t_values[j] == t_values[j]) {
            t_values[j] = inv_count * (t_values[j] + tuple_singles[index]);
          }
        }
      }
    } // end for (int index=0; index<nindex; index++)
  }

  public static int getDefaultColorIndex(DisplayImpl display, int index) {
    return index == 0 ? display.getDisplayScalarIndex(Display.Red)
        : (index == 1 ? display.getDisplayScalarIndex(Display.Green)
            : (index == 2 ? display.getDisplayScalarIndex(Display.Blue)
                : display.getDisplayScalarIndex(Display.Alpha)));
  }

  /** equalize lengths and fill with default values */
  static void equalizeAndDefault(float[][] tuple_values, DisplayImpl display,
      DisplayTupleType tuple, float[] default_values) throws VisADException {
    int nindex = tuple_values.length;
    // fill any empty tuple_values[index] with default values
    for (int index = 0; index < nindex; index++) {
      if (tuple_values[index] == null) {
        tuple_values[index] = new float[1];
        int default_index = (index < 3) ? display
            .getDisplayScalarIndex(((DisplayRealType) tuple.getComponent(index)))
            : display.getDisplayScalarIndex(Display.Alpha);
        tuple_values[index][0] = default_values[default_index];
        /*
         * System.out.println("default color " + index + " is " +
         * default_values[default_index]);
         */
      }
    }
    // compute maximum length of tuple_values[index]
    int len = 1;
    for (int index = 0; index < nindex; index++) {
      len = Math.max(len, tuple_values[index].length);
    }
    // entend any tuple_values[index], except Alpha, to maximum length
    for (int index = 0; index < 3; index++) {
      int t_len = tuple_values[index].length;
      if (len > t_len) {
        if (t_len != 1) {
          throw new DisplayException(
              "bad length: ShadowType.equalizeAndDefault");
        }
        float[] t = new float[len];
        float v = tuple_values[index][0];
        for (int i = 0; i < len; i++)
          t[i] = v;
        tuple_values[index] = t;
      }
    }
  }

  /**
   * composite tuple_values into rgba_values and rgba_value_counts, for index =
   * 0, 1, 2
   */
  static void colorComposite(float[][] rgba_values, float[] rgba_value_counts,
      float[][] tuple_values) throws VisADException {
    for (int index = 0; index < 3; index++) {
      singleComposite(index, rgba_values, rgba_value_counts,
          tuple_values[index]);
      // FREE
      tuple_values[index] = null;
    }
  }

  /**
   * composite values into rgba_values[index] and rgba_value_counts[index]
   */
  static void singleComposite(int index, float[][] rgba_values,
      float[] rgba_value_counts, float[] values) throws VisADException {
    if (values == null)
      return;
    if (rgba_values[index] == null) {
      rgba_values[index] = values;
      rgba_value_counts[index] = 1.0f;
    } else {
      rgba_value_counts[index]++;
      int rgba_len = rgba_values[index].length;
      int values_len = values.length;
      if (rgba_len == values_len) {
        for (int j = 0; j < rgba_len; j++) {
          rgba_values[index][j] += values[j];
        }
      } else if (values_len == 1) {
        for (int j = 0; j < rgba_len; j++) {
          rgba_values[index][j] += values[0];
        }
      } else if (rgba_len == 1) {
        for (int j = 0; j < rgba_len; j++) {
          values[j] += rgba_values[index][0];
        }
        rgba_values[index] = values;
      } else {
        throw new DisplayException("bad length: ShadowType.singleComposite");
      }
    }
  }

  /**
   * return a composite of SelectRange DisplayRealType values from
   * display_values, as 0.0 for select and Double.Nan for no select (these
   * values can be added to other DisplayRealType values)
   */
  public boolean[][] assembleSelect(float[][] display_values,
      int domain_length, int valueArrayLength, int[] valueToScalar,
      DisplayImpl display, ShadowType shadow_api) throws VisADException {
    int[] valueToMap = display.getValueToMap();
    Vector MapVector = display.getMapVector();
    boolean[][] range_select = new boolean[1][];
    boolean anySelect = false;
    for (int i = 0; i < valueArrayLength; i++) {
      float[] values = display_values[i];
      if (values != null) {
        int displayScalarIndex = valueToScalar[i];
        DisplayRealType real = display.getDisplayScalar(displayScalarIndex);
        if (real.equals(Display.SelectRange)) {
          if (range_select[0] == null) {
            // MEM
            range_select[0] = new boolean[domain_length];
            for (int j = 0; j < domain_length; j++) {
              range_select[0][j] = true;
            }
          }
          RangeControl control = (RangeControl) ((ScalarMap) MapVector
              .elementAt(valueToMap[i])).getControl();
          float[] range = control.getRange();
          // System.out.println("range = " + range[0] + " " + range[1]);

          if (values.length == 1) {
            if (values[0] < range[0] || range[1] < values[0]) {
              for (int j = 0; j < domain_length; j++) {
                range_select[0][j] = false;
              }
              anySelect = true;
            }
          } else {
            for (int j = 0; j < values.length; j++) {
              if (values[j] < range[0] || range[1] < values[j]) {
                range_select[0][j] = false;
                anySelect = true;
                /*
                 * System.out.println("range = " + range[0] + " " + range[1] +
                 * " values[" + j + "] = " + values[j]);
                 */
              }
            }
          }
          // FREE
          display_values[i] = null; // MEM_WLH 27 March 99
        } // end if (real.equals(Display.SelectRange))
      } // end if (values != null)
    } // end for (int i=0; i<valueArrayLength; i++)
    if (range_select[0] != null && !anySelect)
      range_select[0] = null;
    return range_select;
  }

  /**
   * transform data into a (Java3D or Java2D) scene graph; add generated scene
   * graph components as children of group; group is Group (Java3D) or
   * VisADGroup (Java2D); value_array are inherited valueArray values;
   * default_values are defaults for each display.DisplayRealTypeVector; return
   * true if need post-process
   */
  public boolean terminalTupleOrScalar(Object group, float[][] display_values,
      String text_value, TextControl text_control, int valueArrayLength,
      int[] valueToScalar, float[] default_values, int[] inherited_values,
      DataRenderer renderer, ShadowType shadow_api) throws VisADException,
      RemoteException {

    GraphicsModeControl mode = (GraphicsModeControl) display
        .getGraphicsModeControl().clone();
    float pointSize = default_values[display
        .getDisplayScalarIndex(Display.PointSize)];
    mode.setPointSize(pointSize, true);
    float lineWidth = default_values[display
        .getDisplayScalarIndex(Display.LineWidth)];
    mode.setLineWidth(lineWidth, true);
    int lineStyle = (int) default_values[display
        .getDisplayScalarIndex(Display.LineStyle)];
    mode.setLineStyle(lineStyle, true);
    float polygonOffset = default_values[display
        .getDisplayScalarIndex(Display.PolygonOffset)];
    mode.setPolygonOffset(polygonOffset, true);
    float polygonOffsetFactor = default_values[display
        .getDisplayScalarIndex(Display.PolygonOffsetFactor)];
    mode.setPolygonOffsetFactor(polygonOffsetFactor, true);
    float cacheAppearances = default_values[display
        .getDisplayScalarIndex(Display.CacheAppearances)];
    mode.setCacheAppearances(cacheAppearances > 0.5f);
    float mergeArrays = default_values[display
        .getDisplayScalarIndex(Display.MergeGeometries)];
    mode.setMergeGeometries(mergeArrays > 0.5f);

    float[][] flow1_values = new float[3][];
    float[][] flow2_values = new float[3][];
    float[] flowScale = new float[2];
    float[] arrowScale = new float[2];
    boolean[][] range_select = new boolean[1][];
    shadow_api.assembleFlow(flow1_values, flow2_values, flowScale, arrowScale,
        display_values, valueArrayLength, valueToScalar, display,
        default_values, range_select, renderer, shadow_api);

    if (range_select[0] != null && !range_select[0][0]) {
      // data not selected
      return false;
    }

    boolean[] swap = { false, false, false };
    int[] spatialDimensions = new int[2];
    float[][] spatial_values = new float[3][];
    shadow_api.assembleSpatial(spatial_values, display_values,
        valueArrayLength, valueToScalar, display, default_values,
        inherited_values, null, false, false, spatialDimensions, range_select,
        flow1_values, flow2_values, flowScale, swap, renderer, shadow_api);

    if (range_select[0] != null && !range_select[0][0]) {
      // data not selected
      return false;
    }

    boolean[] single_missing = { false, false, false, false };
    byte[][] color_values = shadow_api.assembleColor(display_values,
        valueArrayLength, valueToScalar, display, default_values, range_select,
        single_missing, shadow_api);

    if (range_select[0] != null && !range_select[0][0]) {
      // data not selected
      return false;
    }

    int LevelOfDifficulty = getLevelOfDifficulty();
    if (LevelOfDifficulty == SIMPLE_TUPLE) {
      // only manage Spatial, Color and Alpha here
      // i.e., the 'dots'

      if (single_missing[0] || single_missing[1] || single_missing[2]) {
        // System.out.println("single missing alpha");
        // a single missing color value, so render nothing
        return false;
      }
      // put single color in appearance
      /*
       * ColoringAttributes constant_color = new ColoringAttributes();
       * constant_color.setColor(byteToFloat(color_values[0][0]),
       * byteToFloat(color_values[1][0]), byteToFloat(color_values[2][0]));
       */
      float[] constant_color = new float[] { byteToFloat(color_values[0][0]),
          byteToFloat(color_values[1][0]), byteToFloat(color_values[2][0]) };
      float constant_alpha = Float.NaN;

      VisADGeometryArray array;

      boolean anyShapeCreated = false;
      int[] valueToMap = display.getValueToMap();
      Vector MapVector = display.getMapVector();
      VisADGeometryArray[] arrays = shadow_api.assembleShape(display_values,
          valueArrayLength, valueToMap, MapVector, valueToScalar, display,
          default_values, inherited_values, spatial_values, color_values,
          range_select, -1, shadow_api);
      if (arrays != null) {
        for (int i = 0; i < arrays.length; i++) {
          array = arrays[i];
          if (array != null) {
            shadow_api.addToGroup(group, array, mode, constant_alpha,
                constant_color);
            /*
             * WLH 25 June 2000 if (renderer.getIsDirectManipulation()) {
             * renderer.setSpatialValues(spatial_values); }
             */
          }
        }
        anyShapeCreated = true;
      }

      boolean anyTextCreated = false;
      if (text_value != null && text_control != null) {
        String[] text_values = { text_value };
        array = shadow_api.makeText(text_values, text_control, spatial_values,
            color_values, range_select);
        shadow_api.addTextToGroup(group, array, mode, constant_alpha,
            constant_color);
        anyTextCreated = true;
      }

      boolean anyFlowCreated = false;
      // try Flow1
      arrays = shadow_api.makeFlow(0, flow1_values, flowScale[0], arrowScale[0],
          spatial_values, color_values, range_select);
      if (arrays != null) {
        for (int i = 0; i < arrays.length; i++) {
          if (arrays[i] != null) {
            shadow_api.addToGroup(group, arrays[i], mode, constant_alpha,
                constant_color);
          }
        }
        anyFlowCreated = true;
      }
      // try Flow2
      arrays = shadow_api.makeFlow(1, flow2_values, flowScale[1], arrowScale[1],
          spatial_values, color_values, range_select);
      if (arrays != null) {
        for (int i = 0; i < arrays.length; i++) {
          if (arrays[i] != null) {
            shadow_api.addToGroup(group, arrays[i], mode, constant_alpha,
                constant_color);
          }
        }
        anyFlowCreated = true;
      }

      if (!anyFlowCreated && !anyTextCreated && !anyShapeCreated) {
        array = makePointGeometry(spatial_values, null);
        if (array != null && array.vertexCount > 0) {
          shadow_api.addToGroup(group, array, mode, constant_alpha,
              constant_color);
          /*
           * WLH 25 June 2000 if (renderer.getIsDirectManipulation()) {
           * renderer.setSpatialValues(spatial_values); }
           */
        }
      }

      // WLH 25 June 2000
      if (renderer.getIsDirectManipulation()) {
        renderer.setSpatialValues(spatial_values);
      }

      return false;
    } else { // if (!(LevelOfDifficulty == SIMPLE_TUPLE))
      // must be LevelOfDifficulty == LEGAL
      // add values to value_array according to SelectedMapVector-s
      // of RealType-s in components (including Reference)
      //
      // accumulate Vector of value_array-s at this ShadowTypeJ3D,

      // to be rendered in a post-process to scanning data
      throw new UnimplementedException("terminal LEGAL unimplemented: "
          + "ShadowType.terminalTupleOrReal");
    }
  }

  public boolean makeContour(int valueArrayLength, int[] valueToScalar,
      float[][] display_values, int[] inherited_values, Vector MapVector,
      int[] valueToMap, int domain_length, boolean[][] range_select,
      int spatialManifoldDimension, Set spatial_set, byte[][] color_values,
      boolean indexed, Object group, GraphicsModeControl mode, boolean[] swap,
      float constant_alpha, float[] constant_color, ShadowType shadow_api,
      ShadowRealTupleType Domain, ShadowRealType[] DomainReferenceComponents,
      Set domain_set, Unit[] domain_units, CoordinateSystem dataCoordinateSystem)
      throws VisADException {
    boolean anyContourCreated = false;

    // WLH 4 May 2001
    DataRenderer renderer = getLink().getRenderer();

    double[] matrix = p_cntrl.getMatrix();
    double scale = Double.NaN;
    double[] scale_a = new double[3];
    MouseBehavior mouse = display.getMouseBehavior();
    if (mouse != null) {
      double[] rot_a = new double[3];
      double[] trans_a = new double[3];
      mouse.instance_unmake_matrix(rot_a, scale_a, trans_a, matrix);
      scale = scale_a[0];
    }

    /*
     * try { System.out.println("makeContour " +
     * getLink().getThingReference().getName()); } catch (RemoteException e) { }
     */

    boolean isLinearContour3D = getIsLinearContour3D()
        && spatial_set instanceof Linear3DSet;
    ScalarMap[] spatial_maps = { null, null, null };
    int[] permute = { -1, -1, -1 };
    if (isLinearContour3D) {
      RealType[] reals = ((SetType) spatial_set.getType()).getDomain()
          .getRealComponents();
      for (int i = 0; i < valueArrayLength; i++) {
        ScalarMap map = (ScalarMap) MapVector.elementAt(valueToMap[i]);
        ScalarType sc = map.getScalar();
        RealType real = (sc instanceof RealType) ? (RealType) sc : null;
        DisplayRealType dreal = map.getDisplayScalar();
        DisplayTupleType tuple = dreal.getTuple();

        if (tuple != null && tuple.equals(Display.DisplaySpatialCartesianTuple)) {
          int tuple_index = dreal.getTupleIndex();
          for (int j = 0; j < reals.length; j++) {
            if (real.equals(reals[j])) {
              permute[j] = tuple_index;
              spatial_maps[j] = map;
              break;
            }
          }
        }
      }
    }

    ShadowRealTupleType domain_reference = null;
    CoordinateSystem coord_sys = null;
    boolean sphericalDisplayCS = false;
    if (spatialTuple != null)
      coord_sys = spatialTuple.getCoordinateSystem();
    if (coord_sys != null) {
      sphericalDisplayCS = coord_sys instanceof SphericalCoordinateSystem;
    }
    domain_reference = Domain.getReference();
    boolean singleValueAsTexture = Boolean.parseBoolean(System.getProperty(PROP_CONTOURFILL_SINGLE_VALUE_AS_TEXTURE, "false"));

    for (int i = 0; i < valueArrayLength; i++) {
      int displayScalarIndex = valueToScalar[i];
      DisplayRealType real = display.getDisplayScalar(displayScalarIndex);
      if (real.equals(Display.IsoContour) && display_values[i] != null
          && display_values[i].length == domain_length
          && inherited_values[i] == 0) {
        // non-inherited IsoContour, so generate contours
        VisADGeometryArray array = null;
        ContourControl control = (ContourControl) ((ScalarMap) MapVector
            .elementAt(valueToMap[i])).getControl();
        c_cntrl = control;

        boolean[] bvalues = new boolean[2];
        float[] fvalues = new float[5];
        control.getMainContours(bvalues, fvalues);
        boolean doContour = bvalues[0];
        boolean doLabels = bvalues[1];
        float isoLvl = fvalues[0];

        if (scale != scale)
          scale = ContourControl.getInitScale();
        double label_size = control.getLabelSize();
        if (spatialManifoldDimension == 3 || spatialManifoldDimension == 2) {
          anyContourCreated = true;
        }
        if (doContour) {
          if (range_select[0] != null) {
            int len = range_select[0].length;
            if (len == 1 || display_values[i].length == 1)
              break;

            // WLH 30 July 99
            int dlen = display_values[i].length;
            float[] temp = display_values[i];
            display_values[i] = new float[dlen];
            System.arraycopy(temp, 0, display_values[i], 0, dlen);

            for (int j = 0; j < len; j++) {
              if (!range_select[0][j]) {
                display_values[i][j] = Float.NaN;
              }
            }
          }
          if (spatialManifoldDimension == 3) {
            if (isoLvl == isoLvl) { // not NaN
              if (spatial_set != null) {
                if (isLinearContour3D) {
                  array = ((Linear3DSet) spatial_set).makeLinearIsoSurface(
                      isoLvl, display_values[i], color_values, indexed,
                      spatial_maps, permute);
                } else {
                  array = spatial_set.makeIsoSurface(isoLvl, display_values[i],
                      color_values, indexed);
                }

                // WLH 4 May 2001
                if (array != null && getAdjustProjectionSeam()) {
                  try {
                    array = array.adjustLongitude(renderer);
                    array = array.adjustSeam(renderer);
                  } catch (Exception e) {
                  }
                }

                // add all data to group
                shadow_api.addToGroup(group, array, mode, constant_alpha,
                    constant_color);
                array = null;
              } else if (coord_sys != null) { // missing spatials set as result
                                              // of transform (coord_sys)
                array = ((Gridded3DSet) domain_set)
                    .makeIsoSurfaceMissingSpatial(isoLvl, display_values[i],
                        color_values, indexed, Domain, domain_reference,
                        domain_units, dataCoordinateSystem, coord_sys,
                        DomainReferenceComponents, spatialTuple,
                        spatial_offset_values);
                if (array != null) {
                  array = array.removeMissing();
                }
                shadow_api.addToGroup(group, array, mode, constant_alpha,
                    constant_color);
                array = null;
              }
            }
            // anyContourCreated = true;
          } else if (spatialManifoldDimension == 2) {
            if (spatial_set != null) {

              float[] lowhibase = new float[3];
              boolean[] doStyle = { false };
              float[] levs = control.getLevels(lowhibase, doStyle);

              boolean fill = control.contourFilled();
              ScalarMap[] smap = new ScalarMap[2]; // changed to 2 to pass
                                                   // IsoContour Map to Set
              ScalarType sc = ((ScalarMap) MapVector.elementAt(valueToMap[i]))
                  .getScalar();
              if (fill) {
                for (int kk = 0; kk < MapVector.size(); kk++) {
                  ScalarMap sm = (ScalarMap) MapVector.elementAt(kk);
                  if (sm != null) {
                    if (sm.getScalar().equals(sc)
                        && (sm.getDisplayScalar().equals(Display.RGB) || sm
                            .getDisplayScalar().equals(Display.RGBA))) {
                      smap[0] = sm;
                    }
                  }
                }
                if (smap[0] == null) {
                  throw new DisplayException(
                      "IsoContour color-fill is enabled, so " + sc
                          + " must also be mapped to Display.RGB");
                }
              }

              // BMF 2006-10-04 get the IsoContour ScalarMap
              for (int kk = 0; kk < MapVector.size(); kk++) {
                ScalarMap sm = (ScalarMap) MapVector.elementAt(kk);
                if (sm != null) {
                  if (sm.getScalar().equals(sc)
                      && sm.getDisplayScalar().equals(Display.IsoContour)) {
                    smap[1] = sm;
                  }
                }
              }
              float maxValue = Float.NEGATIVE_INFINITY;
              float minValue = Float.POSITIVE_INFINITY;
              boolean haveSingleValue = false;
              // if the singleValueAsTexture is true, loop over the values
              visad.util.Trace.call1("ShadowType:isSingleValue");
              if (fill && singleValueAsTexture) {
                int dlen = display_values[i].length;
                for (int j = 0; j < dlen; j++) {
                  if (display_values[i][j] > maxValue)
                    maxValue = display_values[i][j];
                  if (display_values[i][j] < minValue)
                    minValue = display_values[i][j];
                }
                haveSingleValue = (maxValue != Float.NEGATIVE_INFINITY &&
                               minValue !=Float.POSITIVE_INFINITY &&
                               maxValue == minValue);
              }
              visad.util.Trace.call2("ShadowType:isSingleValue");
              // if we are filling and we have a single value, don't try
              // to contour and return false;
              if (haveSingleValue && singleValueAsTexture) return false;

              visad.util.Trace.call1("ShadowType:makeIsoLines");
              VisADGeometryArray[][] array_s = spatial_set.makeIsoLines(levs,
                  lowhibase[0], lowhibase[1], lowhibase[2], display_values[i],
                  color_values, swap, doStyle[0], fill, smap, scale_a,
                  label_size, sphericalDisplayCS);
              visad.util.Trace.call2("ShadowType:makeIsoLines");

              // even though no contours were created, we at least tried
              // so have to return true.
              if (array_s == null) return anyContourCreated;
                boolean adjust = getAdjustProjectionSeam();
                // make necessary adjustments
                for (int j = 0; j < array_s.length; j++) {
                   if (array_s[j] != null) {
                      for (int k=0; k< array_s[j].length; k++) {
                        VisADGeometryArray arr = array_s[j][k];
                        if (arr != null) {
                          if (adjust) {
                            arr = arr.adjustLongitude(renderer);
                            arr = arr.adjustSeam(renderer);
                          }
                          arr = arr.removeMissing();
                        }
                        array_s[j][k] = arr;
                      }
                   }
                }

                VisADGeometryArray[] uBasicLines = array_s[0];
                VisADGeometryArray[] fillLines = array_s[1];
                VisADGeometryArray[] labelLines = null;
                VisADGeometryArray[] sBasicLines = null;

                if (array_s != null) {
                  // set'em if you got em
                  switch (array_s.length) {
                  case 4:
                    sBasicLines = array_s[3];
                  case 3:
                    labelLines = array_s[2];
                  }

                //if (array_s.length > 0 && uBasicLines.length > 0) {
                if (array_s.length > 0) {

                  // label mode, forcing labels to have solid J3D line style
                  GraphicsModeControl labelMode 
                    = (GraphicsModeControl) mode.clone();
                  labelMode.setLineStyle(GraphicsModeControl.SOLID_STYLE, false);

                  // mode for dashed lines rendered with J3D line style
                  GraphicsModeControl styledMode 
                    = (GraphicsModeControl) mode.clone();
                  styledMode.setLineStyle(control.getDashedStyle(), false);

                  if (fill) {
                    // BMF set offset to make labels more clear.
                    // FIXME: There may be a better value to use here
                    labelMode.setPolygonOffsetFactor(10f, false);
                    labelMode.setPolygonOffset(1f, false);

                    // make adjustment for basic lines
                    if (uBasicLines != null) {
                      for (VisADGeometryArray arr : uBasicLines) {
                        if (arr == null)
                          continue;
                        shadow_api.adjustZ(arr.coordinates);
                      }
                    }
                    // there may not be unstyled lines
                    if (sBasicLines != null) {
                      for (VisADGeometryArray arr : sBasicLines) {
                        if (arr == null)
                          continue;
                        shadow_api.adjustZ(arr.coordinates);
                      }
                    }
                  }

                  // add unstyled lines
                  if (uBasicLines != null) {
                    for (VisADGeometryArray arr : uBasicLines) {
                      if (arr == null)
                        continue;
                      shadow_api.addToGroup(group, arr, mode, constant_alpha,
                          constant_color);
                    }
                  }
                  array_s[0] = null;
                  uBasicLines = null;

                  // add styled lines
                  if (sBasicLines != null) {
                    for (VisADGeometryArray arr : sBasicLines) {
                      if (arr == null)
                        continue;
                      shadow_api.addToGroup(group, arr, styledMode,
                          constant_alpha, constant_color);
                    }
                    sBasicLines = null;
                    array_s[3] = null;
                  }

                  if (doLabels && labelLines != null) {
                    shadow_api.addLabelsToGroup(group, labelLines, labelMode,
                        control, p_cntrl, cnt, constant_alpha, constant_color);
                    labelLines = null;
                    array_s[2] = null;

                  } else if (!doLabels && fillLines != null) {
                    // fill in contour lines in place of labels
                    shadow_api.addToGroup(group, fillLines[0], mode,
                        constant_alpha, constant_color);
                    if (fillLines.length == 2) {  //- styled lines available
                      shadow_api.addToGroup(group, fillLines[1], styledMode,
                         constant_alpha, constant_color);
                      fillLines[1] = null;
                    }
                    fillLines[0] = null;
                    array_s[1] = null;
                  }
                  array_s = null;
                }
              }
            } // end if (spatial_set != null)
            // anyContourCreated = true;
          } // end if (spatialManifoldDimension == 2)
        } // end if (bvalues[CONTOUR])
      } // end if (real.equals(Display.IsoContour) && not inherited)
    } // end for (int i=0; i<valueArrayLength; i++)

    return anyContourCreated;
  }

  public int textureWidth(int data_width) {
    return data_width;
  }

  public int textureHeight(int data_height) {
    return data_height;
  }

  public int textureDepth(int data_depth) {
    return data_depth;
  }

  public void adjustZ(float[] coordinates) {
  }

  public void setTexCoords(float[] texCoords, float ratiow, float ratioh) {
  }

  public float[] setTex3DCoords(int length, int axis, float ratiow,
      float ratioh, float ratiod) {
    return null;
  }

  public float[] setTexStackCoords(int length, int axis, float ratiow,
      float ratioh, float ratiod) {
    return null;
  }

  public Vector getTextMaps(int i, int[] textIndices) {
    return new Vector();
  }

  public boolean addToGroup(Object group, VisADGeometryArray array,
      GraphicsModeControl mode, float constant_alpha, float[] constant_color)
      throws VisADException {
    return false;
  }
  
  /* Convenience method that returns a Branch node, which may be parented,
     and whose only child can be detached. Acts also as an id since its parent
     may have multiple children */
  public Object addToDetachableGroup(Object group, VisADGeometryArray array,
      GraphicsModeControl mode, float constant_alpha, float[] constant_color)
      throws VisADException {
    return null;     
  }

  //public void addLabelsToGroup(Object group, VisADGeometryArray[][] arrays,
  public void addLabelsToGroup(Object group, VisADGeometryArray[] arrays,
      GraphicsModeControl mode, ContourControl control,
      ProjectionControl p_cntrl, int[] cnt, float constant_alpha,
      float[] contstant_color) throws VisADException {
  }

  public boolean addTextToGroup(Object group, VisADGeometryArray array,
      GraphicsModeControl mode, float constant_alpha, float[] constant_color)
      throws VisADException {
    return addToGroup(group, array, mode, constant_alpha, constant_color);
  }
  
  public Object createImage(int data_width, int data_height, int texture_width,
                     int texture_height, byte[][] color_values) throws VisADException {
     return null;
  }
  
  public Object[] createImages(int axis, int data_width_in,
           int data_height_in, int data_depth_in, int texture_width_in,
           int texture_height_in, int texture_depth_in, byte[][] color_values)
         throws VisADException {
    return null;
  }

  public void textureToGroup(Object group, VisADGeometryArray array,
      Object image, GraphicsModeControl mode, float constant_alpha,
      float[] constant_color, int texture_width, int texture_height)
      throws VisADException {
  }

  public void texture3DToGroup(Object group, VisADGeometryArray arrayX,
      VisADGeometryArray arrayY, VisADGeometryArray arrayZ,
      VisADGeometryArray arrayXrev, VisADGeometryArray arrayYrev,
      VisADGeometryArray arrayZrev, Object[] images,
      GraphicsModeControl mode, float constant_alpha, float[] constant_color,
      int texture_width, int texture_height, int texture_depth,
      DataRenderer renderer) throws VisADException {
  }

  public void textureStackToGroup(Object group, VisADGeometryArray arrayX,
      VisADGeometryArray arrayY, VisADGeometryArray arrayZ,
      VisADGeometryArray arrayXrev, VisADGeometryArray arrayYrev,
      VisADGeometryArray arrayZrev, Object[] imagesX,
      Object[] imagesY, Object[] imagesZ,
      GraphicsModeControl mode, float constant_alpha, float[] constant_color,
      int texture_width, int texture_height, int texture_depth,
      DataRenderer renderer) throws VisADException {
  }

  public Object makeSwitch() {
    return null;
  }

  public Object makeBranch() {
    return null;
  }

  public void addToGroup(Object group, Object branch) throws VisADException {
  }

  public void addToSwitch(Object swit, Object branch) throws VisADException {
  }

  public void addSwitch(Object group, Object swit, Control control,
      Set domain_set, DataRenderer renderer) throws VisADException {
  }

  public boolean recurseRange(Object group, Data data, float[] value_array,
      float[] default_values, DataRenderer renderer) throws VisADException,
      RemoteException {
    return false;
  }

  public boolean recurseComponent(int i, Object group, Data data,
      float[] value_array, float[] default_values, DataRenderer renderer)
      throws VisADException, RemoteException {
    return false;
  }

  public boolean wantIndexed() {
    return false;
  }

  public TextControl getParentTextControl() {
    return null;
  }

  public String getParentText() {
    return null;
  }

  public void setText(String text, TextControl control) {
  }

  public boolean allowCurvedTexture() {
    return true;
  }

  public boolean allowConstantColorSurfaces() {
    return true;
  }

  public boolean allowLinearContour() {
    return true;
  }

  public String toString() {
    return getClass() + " for \n  " + Type.toString();
    // return " LevelOfDifficulty = " + LevelOfDifficulty;
  }

}
