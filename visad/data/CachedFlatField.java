//
// CachedFlatField
//

/*
 * VisAD system for interactive analysis and visualization of numerical
 * data.  Copyright (C) 1996 - 2014 Bill Hibbard, Curtis Rueden, Tom
 * Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
 * Tommy Jasmin, Jeff McWhirter.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
 * MA 02111-1307, USA
 */


package visad.data;

import visad.*;
import visad.util.DataUtility;
import java.rmi.RemoteException;


/**
 * This is a FloatField that caches to disk its float array.
 */
public class CachedFlatField extends FlatField {


    /** the id for this instance */
    private Object cacheId;

    private boolean inCache = false;

    /** Mutex */
    transient protected Object MUTEX = new Object();


    /** The min/max ranges */
    DataRange[] ranges;

    /** The min/max ranges */
    DataRange[] sampleRanges;

    private  CachedFlatField parent;

    /**
     * Create a new CachedFlatField
     *
     * @param type Function type
     * @param domainSet set for this
     *
     * @throws VisADException On badness
     */
    public CachedFlatField(FunctionType type, Set domainSet)
            throws VisADException {
        this(type, domainSet, (CoordinateSystem) null, (Set[]) null,
             (Unit[]) null, null);
    }


    /**
     * Create a new CachedFlatField
     *
     * @param floats The values
     * @param type Function type
     *
     * @throws VisADException On badness
     */
    public CachedFlatField(FunctionType type, float[][] floats)
            throws VisADException {
        this(type, type.getDomain().getDefaultSet(), (CoordinateSystem) null,
             (Set[]) null, (Unit[]) null, floats);
    }

    /**
     * Create a new CachedFlatField
     *
     * @param floats The values
     * @param type Function type
     * @param domainSet Domain
     *
     * @throws VisADException On badness
     */
    public CachedFlatField(FunctionType type, Set domainSet, float[][] floats)
            throws VisADException {
        this(type, domainSet, (CoordinateSystem) null, (Set[]) null,
             (Unit[]) null, floats);
    }

    /**
     * Create a new CachedFlatField
     *
     * @param type Function type
     * @param domainSet Domain
     * @param rangeCoordSys  range CoordSystem
     * @param rangeSets range sets
     * @param units units
     * @param floats The values
     *
     * @throws VisADException On badness
     */
    public CachedFlatField(FunctionType type, Set domainSet,
                           CoordinateSystem rangeCoordSys, Set[] rangeSets,
                           Unit[] units, float[][] floats)
            throws VisADException {
        this(type, domainSet, rangeCoordSys, null, rangeSets, units, floats);
    }

    /**
     * Create a new CachedFlatField
     *
     * @param type Function type
     * @param domainSet Domain
     * @param rangeCoordSys  range CoordSystem
     * @param rangeCoordSyses  range CoordSystem's
     * @param rangeSets range sets
     * @param units units
     * @param floats The values
     *
     * @throws VisADException On badness
     */
    public CachedFlatField(FunctionType type, Set domainSet,
                           CoordinateSystem rangeCoordSys,
                           CoordinateSystem[] rangeCoordSyses,
                           Set[] rangeSets, Unit[] units, float[][] floats)
            throws VisADException {
        super(type, domainSet, rangeCoordSys, null, rangeSets, units);
        initCache(floats);
    }

    /**
     * Copy constructor
     *
     * @param that What we clone from
     * @param copy copy the values
     * @param type Function type
     * @param domainSet Domain
     * @param rangeCoordSys  range CoordSystem
     * @param rangeCoordSysArray  rangeCoordSysArray
     * @param rangeSets range sets
     * @param units units
     *
     * @throws VisADException On badness
     */
    public CachedFlatField(CachedFlatField that, boolean copy,
                           FunctionType type, Set domainSet,
                           CoordinateSystem rangeCoordSys,
                           CoordinateSystem[] rangeCoordSysArray,
                           Set[] rangeSets, Unit[] units)
            throws VisADException {
        super(type, domainSet, rangeCoordSys, rangeCoordSysArray, rangeSets,
              units);

        this.ranges       = that.ranges;
        this.sampleRanges = that.sampleRanges;
        this.cacheId =  null;
        this.inCache = false;

        //Get the values from the cloned field if they had read their values
        if(that.haveData()) {
            //            msg("CCF - cloned object is in cache");
            // We used to ignore the copy flag - if this causes problems, 
            // change back to true 
            //float[][] values = that.unpackFloats(true);
            float[][] values = that.unpackFloats(copy);
            if(values == null) {
                parent = that;
            }
            initCache(values);
        } else {
            this.parent = that;
            clearMissing();
            //            msg("CCF - cloned object not in cache");
        }

    }




    /**
     * Clone this object
     *
     * @param copy copy the values
     * @param type Function type
     * @param domainSet Domain set
     * @param rangeCoordSys  range CoordSystem
     * @param rangeCoordSysArray  rangeCoordSysArray
     * @param rangeSets range sets
     * @param units units
     *
     * @return New field
     *
     * @throws VisADException On badness
     */
    public CachedFlatField cloneMe(boolean copy, FunctionType type,
                                   Set domainSet,
                                   CoordinateSystem rangeCoordSys,
                                   CoordinateSystem[] rangeCoordSysArray,
                                   Set[] rangeSets, Unit[] units)
            throws VisADException {

        CachedFlatField ccf = new CachedFlatField(this, copy, type,
                                  domainSet, rangeCoordSys,
                                  rangeCoordSysArray, rangeSets, units);
        return ccf;
    }




    public void finalize() throws Throwable {
        super.finalize();
        //        System.err.println("CachedFlatField.finalize");
        if(cacheId!=null) {
            DataCacheManager.getCacheManager().removeFromCache(cacheId);
        }
    }


    /**
     * Set the sample
     *
     * @param values the samples
     * @param errors errors
     * @param copy   tru to copy
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException  Problem in VisAD land
     */
    public void setSamples(float[][] values, ErrorEstimate[] errors,
                           boolean copy)
            throws VisADException, RemoteException {

        float[][]myFloatValues = new float[values.length][];
        for (int i = 0; i < myFloatValues.length; i++) {
            if (copy) {
                myFloatValues[i] = (float[]) values[i].clone();
            } else {
                myFloatValues[i] = values[i];
            }
        }
        setRangeErrors(errors);
        this.getRanges(values);
        if(inCache) {
            DataCacheManager.getCacheManager().updateData(cacheId, myFloatValues);
        } else {
            initCache(myFloatValues);
        }

    }




    static int cnt = 0;
    public final int mycnt = cnt++;

    /**
     * Override method so we clear the caches on the cloned object
     *
     * @return the clone
     */
    public Object clone() {
        try {
            //      msg("CCF.clone");
            CachedFlatField ccf = (CachedFlatField) super.clone();
            ccf.cacheId = null;
            float[][]newValues = ccf.unpackFloats(false);
            ccf.nullRanges();
            ccf.initCache(newValues);
            return ccf;
        } catch(Exception exc) {
            exc.printStackTrace();
            throw new RuntimeException(exc);
        }
    }



    /**
     * init
     *
     * @param data data
     *
     * @throws VisADException initializing field
     */
    protected void initCache(float[][] data) throws VisADException {
        if(data!=null) {
            if(cacheId!=null) {
                DataCacheManager.getCacheManager().updateData(cacheId, data);
            } else {
                cacheId = DataCacheManager.getCacheManager().addToCache(getClass().getSimpleName(), data);
            }
            inCache = true;
        }
        //Read the ranges when we first have data
        if (ranges == null) {
            getRanges(data);
        }
        clearMissing();
    }


    /**
     * Set the sample ranges
     *
     * @param sampleRanges the sample ranges
     */
    public void setSampleRanges(DataRange[] sampleRanges) {
        this.sampleRanges = sampleRanges;
    }


    /**
     * Clear the cached ranges
     */
    public void clearCachedRange() {
        sampleRanges = null;
        ranges       = null;
    }




    /**
     * Get the ranges
     *
     * @return ranges
     * 
    * @throws VisADException  problem getting ranges
     */
    public DataRange[] getRanges() throws VisADException {
        return getRanges(false);
    }

    /**
     * Get the ranges
     *
     *
     * @param force   force a recalc
     * @return ranges  the ranges
     *
     * @throws VisADException  problem getting ranges
     */
    public DataRange[] getRanges(boolean force) throws VisADException {
        if (force) {
            sampleRanges = null;
        }
        if (ranges != null) {
            return ranges;
        }
        if (sampleRanges != null) {
            return sampleRanges;
        }
        //        msg("making ranges");
        return getRanges(unpackFloats(false));
    }


    /**
     * Get the ranges for the values
     *
     * @param values the values
     *
     * @return the ranges
     *
     * @throws VisADException  Problem in VisAD land
     */
    public DataRange[] getRanges(float[][] values) throws VisADException {
        sampleRanges = null;
        if (values == null) {
            return null;
        }
        ranges = new DataRange[values.length];
        for (int rangeIdx = 0; rangeIdx < values.length; rangeIdx++) {
            float   pMin         = Float.POSITIVE_INFINITY;
            float   pMax         = Float.NEGATIVE_INFINITY;
            float[] values_range = values[rangeIdx];
            int     length       = values_range.length;
            for (int i = 0; i < length; i++) {
                float value = values_range[i];
                if (pMax < value) {
                    pMax = value;
                }
                if (pMin > value) {
                    pMin = value;
                }
            }
            ranges[rangeIdx] = new DataRange(pMin, pMax);
        }
        //        msg("done making ranges");
        return ranges;
    }







    /**
     * Used to provide a hook to derived classes to dynamically read in the data
     *
     * @return data
     */
    public float[][] readData() {
        //        msg(" CachedFlatField.readData");
        return null;
    }

    /**
     * Debug statment
     *
     * @param s message to print
     */
    public void msg(String s) {
        //        System.err.println("ccf:"+ mycnt + ": " + s);
    }


    /**
     * Read data from cache
     *
     * @return the values from the cache
     *
     * @throws VisADException   problem reading data
     */
    private float[][] getMyValues() throws VisADException {
        //        msg("CCF - getMyValues " + inCache);
        if(inCache) {
            if(cacheId == null) {
                //                msg("CCF - WHoa, inCache=true but no cacheId");
                return null;
            }
            return DataCacheManager.getCacheManager().getFloatArray2D(cacheId);
        }

        float[][] values = null;

        //If we don't have the values and we have a ccf that we were cloned from 
        //then read the data from it and clear it out
        if(parent!=null) {
            values = parent.unpackFloats(true);
            readValuesFromParent(parent);
            parent = null;
        }


        if (values == null) {
            values = readData();
        }


        if (values == null) {
            //            msg("Floats still null after readData");
            return null;
        }
        initCache(values);
        return values;
    }


    public boolean haveData() {
        return inCache;
    }



    /**
     * This gets called to notify derived classes that we jus got the data from the parent ccf
     *
     * @param parent The parent CCF we read data from
     */
    protected void readValuesFromParent(CachedFlatField parent) throws VisADException {
    }



    /**
     * Get the range value at the index-th sample.
     *
     * @param index  index of the sample
     * @return Data object (Real, RealTuple, or Tuple) corresponding to
     *         the range at the index-th sample.
     * @throws VisADException  problem getting data
     * @throws RemoteException problem getting data from remote object
     */
    public Data getSample(int index) throws VisADException, RemoteException {
        //        msg("getSample");
        float[][] values = getMyValues();

        if (values == null) {
            //            msg("Floats still null");
            return null;
        }
        MathType        Type        = getType();
        ErrorEstimate[] RangeErrors = getRangeErrors();

        if (isMissing() || (index < 0) || (index >= getLength())) {
            //            msg("is missing");
            return ((FunctionType) Type).getRange().missingData();
        }
        double[][] range = new double[TupleDimension][1];
        for (int i = 0; i < TupleDimension; i++) {
            range[i][0] = (double) values[i][index];
        }

        MathType RangeType = ((FunctionType) Type).getRange();
        if (RangeType instanceof RealType) {
            return new Real((RealType) RangeType, range[0][0], RangeUnits[0],
                            RangeErrors[0]);
        } else if (RangeType instanceof RealTupleType) {
            Real[] reals = new Real[TupleDimension];
            for (int j = 0; j < TupleDimension; j++) {
                MathType type = ((RealTupleType) RangeType).getComponent(j);
                reals[j] = new Real((RealType) type, range[j][0],
                                    RangeUnits[j], RangeErrors[j]);
            }
            return new RealTuple((RealTupleType) RangeType, reals,
                                 RangeCoordinateSystem);
        } else {  // RangeType is a Flat TupleType
            int    n      = ((TupleType) RangeType).getDimension();
            int    j      = 0;
            Data[] datums = new Data[n];
            for (int i = 0; i < n; i++) {
                MathType type = ((TupleType) RangeType).getComponent(i);
                if (type instanceof RealType) {
                    datums[i] = new Real((RealType) type, range[j][0],
                                         RangeUnits[j], RangeErrors[j]);
                    j++;
                } else {  // type instanceof RealTupleType
                    int    m     = ((RealTupleType) type).getDimension();
                    Real[] reals = new Real[m];
                    for (int k = 0; k < m; k++) {
                        RealType ctype =
                            (RealType) ((RealTupleType) type).getComponent(k);
                        reals[k] = new Real(ctype, range[j][0],
                                            RangeUnits[j], RangeErrors[j]);
                        j++;
                    }
                    datums[i] = new RealTuple((RealTupleType) type, reals,
                            RangeCoordinateSystems[i]);
                }
            }
            return new Tuple(datums, false);
        }
    }


    /**
     * get the float values as doubles
     *
     * @param copy copy the values
     *
     * @return The values
     *
     * @throws VisADException On badness
     */
    protected double[][] unpackValues(boolean copy) throws VisADException {
        float[][] values = unpackFloats(false);
        if (values == null) {
            msg ("unpackValues: ccf: values are null ");
            return null;
        }
        double[][] doubles = new double[values.length][];
        for (int i = 0; i < values.length; i++) {
            float[]  values_i  = values[i];
            double[] doubles_i = new double[values_i.length];
            doubles[i] = doubles_i;
            for (int j = 0; j < values_i.length; j++) {
                doubles_i[j] = values_i[j];
            }
        }
        return doubles;
    }


    /**
     * get the float values
     *
     * @param copy copy the values
     *
     * @return The values
     *
     * @throws VisADException On badness
     */
    public float[][] unpackFloats(boolean copy) throws VisADException {
        //        msg("unpackFloats copy=" + copy);
        float[][] values = getMyValues();
        if (values == null) {
            //            msg("unpackFloats gives null");
            //      System.err.println(mycnt+" CCF.unpackFloats - values are still null");
            return super.unpackFloats(copy);
        }
        float[][] result = null;
        result = new float[values.length][];
        for (int i = 0; i < result.length; i++) {
            if (copy) {
                result[i] = (float[]) values[i].clone();
            } else {
                result[i] = values[i];
            }
        }
        return result;
    }


    /**
     * Unpack floats
     *
     * @param s_index the sample index
     *
     * @return  the floats for that index
     *
     * @throws VisADException  Problem in VisAD land
     */
    protected float[] unpackFloats(int s_index) throws VisADException {
        float[][] values = getMyValues();
        if (values == null) {
            return null;
        }
        float[] range = new float[values.length];
        for (int i = 0; i < TupleDimension; i++) {
            range[i] = values[i][s_index];
        }
        return range;
    }





    /**
     * Make a clone of this using the new type, units and errors.  Called
     * from unary and binar
     *
     * @param f_type  the new FunctionType
     * @param units   the new Units
     * @param errors  the new Errors
     * @param newValues  the new values
     *
     * @return a new FlatField
     *
     * @throws VisADException  Problem in VisAD land
    protected FlatField cloneFloat(MathType f_type, Unit[] units,
                                   ErrorEstimate[] errors,
                                   float[][] newValues)
            throws VisADException {
        MathType N_type = ((f_type == null)
                           ? getType()
                           : f_type);

        // create (initially missing) FlatField for return
        // use FloatSet rather than RangeSet for intermediate computation results
        Set[] sets = new Set[TupleDimension];
        for (int i = 0; i < TupleDimension; i++) {
            SetType set_type =
                new SetType(
                    ((FunctionType) N_type).getFlatRange().getComponent(i));
            sets[i] = new FloatSet(set_type);
        }
        RealTupleType d_type  = ((FunctionType) N_type).getDomain();
        Set           new_set = null;
        if ( !d_type.equals(((FunctionType) getType()).getDomain())) {
            new_set = (Set) getDomainSet().cloneButType(d_type);
        } else {
            new_set = getDomainSet();
        }
        if (newValues == null) {
            //If we don't have the values array then copy this one.
            newValues = unpackFloats(true);
        }
        CachedFlatField field = new CachedFlatField((FunctionType) N_type,
                                    new_set, RangeCoordinateSystem,
                                    RangeCoordinateSystems, sets, units,
                                    newValues);
        return field;
    }
     */

}

