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

    private CoordinateSystem dataCS;

    private CoordinateSystem displayCS;

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
    public SubsetRubberBandBox(FlatField data, CoordinateSystem displayCS)
            throws VisADException, RemoteException {
        this(false, data, displayCS, 0);
    }

    public SubsetRubberBandBox(FlatField data, CoordinateSystem displayCS, int mask)
            throws VisADException, RemoteException {
        this(false, data, displayCS, mask);
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
    public SubsetRubberBandBox(boolean isLL, FlatField data, CoordinateSystem displayCS, int mask)
            throws VisADException, RemoteException {
        super("Subset Rubber Band Box");

        this.data = data;
        this.displayCS = displayCS;
        this.isLL = isLL;

        //System.out.println("\n\nSubsetRubberBandBox:");
        //System.out.println("    isLL=" + isLL);
        //System.out.println("    displayCS=" + displayCS.getClass());
        //System.out.println("    mask=" + mask);
        RealTupleType rtype = ((FunctionType)data.getType()).getDomain();
        dataCS = rtype.getCoordinateSystem();
        //System.out.println("    dataCS=" + dataCS.getClass());

        IdentityCoordinateSystem iCS =
             new IdentityCoordinateSystem(
                   new RealTupleType(new RealType[] {RealType.getRealType("ZZtop")}));
        //System.out.println("iCS=" + iCS);

        CoordinateSystem cs =
             new CartesianProductCoordinateSystem(new CoordinateSystem[] {dataCS, iCS});
        //System.out.println("cs=" + cs);

        CoordinateSystem new_cs = new DataToDisplayCoordinateSystem(isLL, cs, displayCS);
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
          /* debug 
            float[] highs = bounds.getHi();
            float[] lows = bounds.getLow();
            if (highs != null && lows != null)
                System.out.println("box: X range = " + lows[0] + " to " +
                                   highs[0] +
                      "; Y range = " + lows[1] + " to " + highs[1] );
           */
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
        //System.out.println("getDataRenderer");
        rubberBandBox = new MyRubberBandBoxRendererJ3D(xType, yType, mask,
                mask);
        rubberBandBox.setKeepLastBoxOn(true);

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
}


class DataToDisplayCoordinateSystem extends CoordinateSystem {
  private CoordinateSystem dataCS;
  private CoordinateSystem displayCS;
  private boolean isLL;


  DataToDisplayCoordinateSystem(boolean isLL, CoordinateSystem dataCS, CoordinateSystem displayCS) throws VisADException {
    super(displayCS.getReference(), null);
    //System.out.println("SubsetRubberBandBox DataToDisplayCoordinateSystem:");
    //System.out.println("    dataCS=" + dataCS.getClass());
    //System.out.println("    isLL=" + isLL);
    //System.out.println("    displayCS=" + displayCS.getClass() + "\n");
    try {
        this.dataCS = dataCS;
        this.displayCS = displayCS;
        this.isLL = isLL;
    } catch (Exception e) {
        System.out.println("e=" + e);
    }
  }

  public float[][] toReference(float[][] values) throws VisADException {
/*
    System.out.println("\nSubsetRubberBandBox toReference float");
    System.out.println("    values[0]: " + values[0][0]);
    System.out.println("    values[1]: " + values[1][0]);
*/
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
    System.out.println("SubsetRubberBandBox fromReference float");
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
    System.out.println("SubsetRubberBandBox toReference double");
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
    System.out.println("\nSubsetRubberBandBox fromReference double");
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
