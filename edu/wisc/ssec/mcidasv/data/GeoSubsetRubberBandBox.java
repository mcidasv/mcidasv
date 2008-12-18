/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.data;

import edu.wisc.ssec.mcidasv.data.hydra.MyRubberBandBoxRendererJ3D;
import edu.wisc.ssec.mcidasv.data.hydra.SubsetRubberBandBox;
//import edu.wisc.ssec.mcidasv.data.hydra.DataToDisplayCoordinateSystem;

import ucar.visad.display.Displayable;
import ucar.visad.display.LineDrawing;

import visad.*;
import visad.bom.*;

import java.rmi.RemoteException;

import java.awt.event.InputEvent;


public class GeoSubsetRubberBandBox extends SubsetRubberBandBox {

    /** x type for the box */
    private RealType xType;

    /** y type for the box */
    private RealType yType;

    /** renderer */
    private MyRubberBandBoxRendererJ3D rubberBandBox;

    /** bounds defined by the rubber band box */
    private Gridded2DSet bounds;

    /** mouse event mask */
    private int mask;

    private FlatField data;
    private boolean isLL;
    private boolean lastBoxOn;

    private CoordinateSystem dataCS;

    private CoordinateSystem displayCS;

    private GeoDataToDisplayCoordinateSystem new_cs;

    private static int count = 0;

    /**
     * Construct a RubberBandBox using xType as the X coordinate and
     * yType as the Y coordinate of the box.
     *
     * @param  xType   RealType of the X coordinate of the box
     * @param  yType   RealType of the Y coordinate of the box
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   Remote error
     */
    public GeoSubsetRubberBandBox(FlatField data, CoordinateSystem displayCS)
            throws VisADException, RemoteException {
        this(false, data, displayCS, 0);
    }

    public GeoSubsetRubberBandBox(FlatField data, CoordinateSystem displayCS, int mask)
            throws VisADException, RemoteException {
        this(false, data, displayCS, mask);
    }

    public GeoSubsetRubberBandBox(boolean isLL, FlatField data, CoordinateSystem displayCS, int mask)
            throws VisADException, RemoteException {
        this(isLL, data, displayCS, mask, true);
    }

    public GeoSubsetRubberBandBox(FlatField data, CoordinateSystem displayCS, int mask, boolean lastBoxOn)
            throws VisADException, RemoteException {
        this(false, data, displayCS, mask, lastBoxOn);
    }



    /**
     * Construct a RubberBandBox using xType as the X coordinate and
     * yType as the Y coordinate of the box.
     *
     * @param xType   RealType of the X coordinate of the box
     * @param yType   RealType of the Y coordinate of the box
     * @param mask    key mask to use for rubberbanding
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   Remote error
     */
    public GeoSubsetRubberBandBox(boolean isLL, FlatField data, CoordinateSystem displayCS, int mask, boolean lastBoxOn)
            throws VisADException, RemoteException {
        super(isLL, data, displayCS, mask, lastBoxOn);
/*
        System.out.println("\n\nGeoSubsetRubberBandBox:");
        System.out.println("    isLL=" + isLL);
        System.out.println("    displayCS=" + displayCS.getClass());
        System.out.println("    mask=" + mask);
*/

        this.data = data;
        this.displayCS = displayCS;
        this.isLL = isLL;
        this.lastBoxOn =  lastBoxOn;

        RealTupleType rtype = ((FunctionType)data.getType()).getDomain();
        dataCS = rtype.getCoordinateSystem();
        if (dataCS == null) {
          dataCS = new GridCoordinateSystem((GriddedSet)data.getDomainSet());
        }
        //System.out.println("    dataCS=" + dataCS.getClass());

        IdentityCoordinateSystem iCS =
             new IdentityCoordinateSystem(
                   new RealTupleType(new RealType[] {RealType.getRealType("ZZtop")}));
        //System.out.println("iCS=" + iCS);

        CoordinateSystem cs =
             new CartesianProductCoordinateSystem(new CoordinateSystem[] {dataCS, iCS});
        //System.out.println("cs=" + cs);

        new_cs = new GeoDataToDisplayCoordinateSystem(isLL, cs, displayCS);
        new_cs.resetExtremes();
        //System.out.println("new_cs=" + new_cs);
        

        DisplayRealType displayLineType =
           new DisplayRealType("displayLine_"+count, true, 0.0, 10000.0, 0.0, null);
        DisplayRealType displayElemType =
           new DisplayRealType("displayElem_"+count, true, 0.0, 10000.0, 0.0, null);
        DisplayRealType displayAltType =
           new DisplayRealType("displayAlt_"+count, true, -1.0, 1.0, 0.0, null);
        DisplayTupleType dtt =
           new DisplayTupleType(new DisplayRealType[] {displayLineType, displayElemType, displayAltType}, new_cs);

        RealType elemType = RealType.getRealType("elem_"+count);
        RealType lineType = RealType.getRealType("line_"+count);
        this.xType = lineType;
        this.yType = elemType;
        this.mask  = mask;
        //System.out.println("xType=" + xType + " yType=" + yType);
        bounds = new Gridded2DSet(new RealTupleType(xType, yType), null, 1);
        //System.out.println("bounds=" + bounds);

        ScalarMap elemMap = new ScalarMap(elemType, displayElemType);
        ScalarMap lineMap = new ScalarMap(lineType, displayLineType);

        GriddedSet domainSet = (GriddedSet) data.getDomainSet();
        float[] low = domainSet.getLow();
        //System.out.println("    low: " + low[0] + " " + low[1]);
        float[] hi  = domainSet.getHi();
        //System.out.println("    hi: " + hi[0] + " " + hi[1]);

        elemMap.setRange(low[1], hi[1]);
        lineMap.setRange(low[0], hi[0]);
        //System.out.println("elemMap range: " + low[1] + " - " + hi[1]);
        //System.out.println("lineMap range: " + low[0] + " - " + hi[0]);

        addScalarMap(elemMap);
        addScalarMap(lineMap);

        //System.out.println("setData bounds=" + bounds);
        setData(bounds);
        count += 1;
    }

    /**
     * Constructor for creating a RubberBandBox from another instance
     *
     * @param that  other instance
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   Remote error
     */
    protected GeoSubsetRubberBandBox(GeoSubsetRubberBandBox that)
            throws VisADException, RemoteException {

        super(that);
/*
        this.xType  = that.xType;
        this.yType  = that.yType;
        this.bounds = that.bounds;
*/
    }

    /**
     * Invoked when box mouse is released. Subclasses should invoke
     * super.dataChange() to ensure the the bounds are set.
     *
     * @throws RemoteException
     * @throws VisADException
     */
/*
    protected void dataChange() throws VisADException, RemoteException {
        bounds = (Gridded2DSet) getData();
        super.dataChange();
    }
*/

    public float[] getRanges() {
        float[] extrms = new_cs.getExtremes();
        new_cs.resetExtremes();
        return extrms;
    }
}

class GeoDataToDisplayCoordinateSystem extends CoordinateSystem {
  private CoordinateSystem dataCS;
  private CoordinateSystem displayCS;
  private boolean isLL;

  private float lineLo;
  private float lineHi;
  private float eleLo;
  private float eleHi;

  GeoDataToDisplayCoordinateSystem(boolean isLL, CoordinateSystem dataCS, CoordinateSystem displayCS) throws VisADException {
    super(displayCS.getReference(), null);
/*
    System.out.println("GeoSubsetRubberBandBox GeoDataToDisplayCoordinateSystem:");
    System.out.println("    dataCS=" + dataCS.getClass());
    System.out.println("    isLL=" + isLL);
    System.out.println("    displayCS=" + displayCS.getClass() + "\n");
*/
    try {
        this.dataCS = dataCS;
        this.displayCS = displayCS;
        this.isLL = isLL;
    } catch (Exception e) {
        System.out.println("e=" + e);
    }
  }

  public float[] getExtremes() {
    float[] extremes = new float[4];
    extremes[0] = eleLo;
    extremes[1] = lineLo;
    extremes[2] = eleHi;
    extremes[3] = lineHi;
    return extremes;
  }

  public void resetExtremes() {
    lineLo = (float)99999.0;
    lineHi = (float)0.0;
    eleLo = (float)99999.0;
    eleHi = (float)0.0;
  }

  public float[][] toReference(float[][] values) throws VisADException {
/*
    System.out.println("\nGeoSubsetRubberBandBox toReference float");
    System.out.println("    values[0]: " + values[0][0]);
    System.out.println("    values[1]: " + values[1][0]);
*/
    if (values[0][0] < eleLo) eleLo = values[0][0];
    if (values[0][0] > eleHi) eleHi = values[0][0];
    if (values[1][0] < lineLo) lineLo = values[1][0];
    if (values[1][0] > lineHi) lineHi = values[1][0];

    //- if (isLL) values = reverseArrayOrder(values);
    float[][] new_values = dataCS.toReference(values);
    if (isLL) new_values = reverseArrayOrder(new_values);
    new_values = displayCS.toReference(new float[][] {new_values[1], new_values[0], new_values[2]});
/*
    System.out.println("    new_values[0]: " + new_values[0][0]);
    System.out.println("    new_values[1]: " + new_values[1][0]);
*/
    return new_values;
  }

  public float[][] fromReference(float[][] values) throws VisADException {
/*
    System.out.println("GeoSubsetRubberBandBox fromReference float");
    System.out.println("    values[0]: " + values[0][0]);
    System.out.println("    values[1]: " + values[1][0]);
*/
    //- if (isLL) values = reverseArrayOrder(values);
    float[][] new_values = displayCS.fromReference(values);
    if (isLL) new_values = reverseArrayOrder(new_values);
    new_values = dataCS.fromReference(new float[][] {new_values[1], new_values[0], new_values[2]});
/*
    System.out.println("    new_values[0]: " + new_values[0][0]);
    System.out.println("    new_values[1]: " + new_values[1][0]);
*/
    return new_values;
  }

  public double[][] toReference(double[][] values) throws VisADException {
/*
    System.out.println("GeoSubsetRubberBandBox toReference double");
    System.out.println("    values[0]: " + values[0][0]);
    System.out.println("    values[1]: " + values[1][0]);
*/
    //- if (isLL) values = reverseArrayOrder(values);
    double[][] new_values = dataCS.toReference(values);
    if (isLL) new_values = reverseArrayOrder(new_values);
    new_values = displayCS.toReference(new double[][] {new_values[1], new_values[0], new_values[2]});
/*
    System.out.println("    new_values[0]: " + new_values[0][0]);
    System.out.println("    new_values[1]: " + new_values[1][0]);
*/
    return new_values;
  }


  public double[][] fromReference(double[][] values) throws VisADException {
/*
    System.out.println("\nGeoSubsetRubberBandBox fromReference double");
    System.out.println("    values[0]: " + values[0][0]);
    System.out.println("    values[1]: " + values[1][0]);
*/
    //- if (isLL) values = reverseArrayOrder(values);
    double[][] new_values = displayCS.fromReference(values);
    if (isLL) new_values = reverseArrayOrder(new_values);
    new_values = dataCS.fromReference(new double[][] {new_values[1], new_values[0], new_values[2]});
/*
    System.out.println("    new_values[0]: " + new_values[0][0]);
    System.out.println("    new_values[1]: " + new_values[1][0]);
*/
    return new_values;
  }

  public boolean equals(Object obj) {
    return true;
  }

    private double[][] reverseArrayOrder(double[][] in) {
        if (in.length < 2) return in;
        int len1 = 2;
        int len2 = in[0].length;
        double[][] out = new double[in.length][len2];;
        for (int i=0; i<len1; i++) {
            for (int j=0; j<len2; j++) {
                out[len1-i-1][j] = in[i][j];
            }
        }
        if (in.length > 2) {
            for (int i=2; i<in.length; i++) {
                for (int j=0; j<len2; j++) {
                    out[i][j] = in[i][j];
                }
            }
        }
        return out;
    }


    private float[][] reverseArrayOrder(float[][] in) {
        if (in.length < 2) return in;
        int len1 = 2;
        int len2 = in[0].length;
        float[][] out = new float[in.length][len2];;
        for (int i=0; i<len1; i++) {
            for (int j=0; j<len2; j++) {
                out[len1-i-1][j] = in[i][j];
            }
        }
        if (in.length > 2) {
            for (int i=2; i<in.length; i++) {
                for (int j=0; j<len2; j++) {
                    out[i][j] = in[i][j];
                }
            }
        }
        return out;
    }
}
