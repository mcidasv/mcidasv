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

package edu.wisc.ssec.mcidasv.data.hydra;

import edu.wisc.ssec.mcidasv.data.hydra.MyRubberBandBoxRendererJ3D;

import ucar.visad.display.Displayable;
import ucar.visad.display.LineDrawing;

import visad.*;
import visad.bom.*;

import java.rmi.RemoteException;

import java.awt.event.InputEvent;


public class SubsetRubberBandBox extends LineDrawing {

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

    private static int count = 0;

    /**
     * Construct a RubberBandBox using xType as the X coordinate and
     * yType as the Y coordinate of the box.
     *
     * @param data
     * @param displayCS
     *
     * @throws VisADException VisAD error
     * @throws RemoteException Remote error
     */
    public SubsetRubberBandBox(FlatField data, CoordinateSystem displayCS)
            throws VisADException, RemoteException {
        this(false, data, displayCS, 0);
    }

    public SubsetRubberBandBox(FlatField data, CoordinateSystem displayCS, int mask)
            throws VisADException, RemoteException {
        this(false, data, displayCS, mask);
    }

    public SubsetRubberBandBox(boolean isLL, FlatField data, CoordinateSystem displayCS, int mask)
            throws VisADException, RemoteException {
        this(isLL, data, displayCS, mask, true);
    }

    public SubsetRubberBandBox(FlatField data, CoordinateSystem displayCS, int mask, boolean lastBoxOn)
            throws VisADException, RemoteException {
        this(false, data, displayCS, mask, lastBoxOn);
    }



    /**
     * Construct a RubberBandBox using xType as the X coordinate and
     * yType as the Y coordinate of the box.
     *
     * @param isLL
     * @param data
     * @param displayCS
     * @param mask Key mask to use for rubberbanding
     * @param lastBoxOn
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   Remote error
     */
    public SubsetRubberBandBox(boolean isLL, FlatField data, CoordinateSystem displayCS, int mask, boolean lastBoxOn)
            throws VisADException, RemoteException {
        super("Subset Rubber Band Box");

        this.data = data;
        this.displayCS = displayCS;
        this.isLL = isLL;
        this.lastBoxOn =  lastBoxOn;

        RealTupleType rtype = ((FunctionType)data.getType()).getDomain();
        dataCS = rtype.getCoordinateSystem();
        if (dataCS == null) {
          dataCS = new GridCoordinateSystem((GriddedSet)data.getDomainSet());
        }

        IdentityCoordinateSystem iCS =
             new IdentityCoordinateSystem(
                   new RealTupleType(new RealType[] {RealType.getRealType("ZZtop")}));

        CoordinateSystem cs =
             new CartesianProductCoordinateSystem(new CoordinateSystem[] {dataCS, iCS});

        CoordinateSystem new_cs = new DataToDisplayCoordinateSystem(isLL, cs, displayCS);
        

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
        bounds = new Gridded2DSet(new RealTupleType(xType, yType), null, 1);

        ScalarMap elemMap = new ScalarMap(elemType, displayElemType);
        ScalarMap lineMap = new ScalarMap(lineType, displayLineType);

        GriddedSet domainSet = (GriddedSet) data.getDomainSet();
        float[] low = domainSet.getLow();
        float[] hi  = domainSet.getHi();

        elemMap.setRange(low[1], hi[1]);
        lineMap.setRange(low[0], hi[0]);

        addScalarMap(elemMap);
        addScalarMap(lineMap);

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
    protected SubsetRubberBandBox(SubsetRubberBandBox that)
            throws VisADException, RemoteException {

        super(that);

        this.xType  = that.xType;
        this.yType  = that.yType;
        this.bounds = that.bounds;
    }

    /**
     * Invoked when box mouse is released. Subclasses should invoke
     * super.dataChange() to ensure the the bounds are set.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected void dataChange() throws VisADException, RemoteException {

        bounds = (Gridded2DSet) getData();
            float[] highs = bounds.getHi();
            float[] lows = bounds.getLow();
            if (highs != null && lows != null)
        super.dataChange();
    }

    /**
     * Return the bounds of the RubberBandBox.  The Gridded2DSet that
     * is returned contains the opposite (starting and ending) corners
     * of the box.
     *
     * @return  set containing the opposite corners of the box.
     */
    public Gridded2DSet getBounds() {
        return bounds;
    }

    /**
     * Get the DataRenderer used for this displayable.
     *
     * @return  RubberBandBoxRendererJ3D associated with this displayable
     */
    protected DataRenderer getDataRenderer() {
        rubberBandBox = new MyRubberBandBoxRendererJ3D(xType, yType, mask,
                mask);
        rubberBandBox.setKeepLastBoxOn(lastBoxOn);

        return rubberBandBox;
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     *
     * @return                  A semi-deep clone of this instance.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new SubsetRubberBandBox(this);
    }

    public void setBox(SubsetRubberBandBox rbb) {
       rubberBandBox.setLastBox((MyRubberBandBoxRendererJ3D)rbb.getDataRenderer());
    }

    public Gridded3DSet getLastBox() {
      return rubberBandBox.last_box;
    }
}


class DataToDisplayCoordinateSystem extends CoordinateSystem {
  private CoordinateSystem dataCS;
  private CoordinateSystem displayCS;
  private boolean isLL;


  DataToDisplayCoordinateSystem(boolean isLL, CoordinateSystem dataCS, CoordinateSystem displayCS) throws VisADException {
    super(displayCS.getReference(), null);
    try {
        this.dataCS = dataCS;
        this.displayCS = displayCS;
        this.isLL = isLL;
    } catch (Exception e) {
        System.out.println("e=" + e);
    }
  }

  public float[][] toReference(float[][] values) throws VisADException {
    //- if (isLL) values = reverseArrayOrder(values);
    float[][] new_values = dataCS.toReference(values);
    if (isLL) new_values = reverseArrayOrder(new_values);
    new_values = displayCS.toReference(new float[][] {new_values[1], new_values[0], new_values[2]});
    return new_values;
  }

  public float[][] fromReference(float[][] values) throws VisADException {
    //- if (isLL) values = reverseArrayOrder(values);
    float[][] new_values = displayCS.fromReference(values);
    if (isLL) new_values = reverseArrayOrder(new_values);
    new_values = dataCS.fromReference(new float[][] {new_values[1], new_values[0], new_values[2]});

    return new_values;
  }

  public double[][] toReference(double[][] values) throws VisADException {
    //- if (isLL) values = reverseArrayOrder(values);
    double[][] new_values = dataCS.toReference(values);
    if (isLL) new_values = reverseArrayOrder(new_values);
    new_values = displayCS.toReference(new double[][] {new_values[1], new_values[0], new_values[2]});

    return new_values;
  }
                                                                                                                                  
  public double[][] fromReference(double[][] values) throws VisADException {
    //- if (isLL) values = reverseArrayOrder(values);
    double[][] new_values = displayCS.fromReference(values);
    if (isLL) new_values = reverseArrayOrder(new_values);
    new_values = dataCS.fromReference(new double[][] {new_values[1], new_values[0], new_values[2]});
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
