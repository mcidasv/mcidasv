//
// FieldImpl.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2014 Bill Hibbard, Curtis Rueden, Tom
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

import java.rmi.RemoteException;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
   FieldImpl is the VisAD class for finite samplings of functions
   from R^n to a range type, where  n>0.  The DomainSet, DomainUnits
   and DomainCoordinateSystem variables of FieldImpl are immutable.<P>

   A FieldImpl domain type may be either a RealType (for a function with
   domain = R) or a RealTupleType (for a function with domain = R^n
   for n > 0).<P>
*/
public class FieldImpl extends FunctionImpl implements Field {

  private static final long serialVersionUID = 1L;

  /** the sampling of the function domain R^n */
  Set DomainSet;

  /** this is DomainSet.DomainCoordinateSystem */
  CoordinateSystem DomainCoordinateSystem;

  /** this is DomainSet.SetUnits */
  Unit[] DomainUnits;

  /** the number of samples */
  int Length;

  /** the array of function values */
    //jeffm: Change the name of Range to MyRange 
    //so methods that use Range can simply call:
    //Data[]Range = getRange ();
  private Data[] MyRange;  // won't be null though elements might be


  // This is used to synchronize access to the Range (which might be null)
  // use VisADRay since it is Serializable and small
  private VisADRay RangeLock = new VisADRay();

  private boolean MissingFlag;

  /** construct a FieldImpl from type;
      use default Set of FunctionType domain;
      initial values are missing */
  public FieldImpl(FunctionType type) throws VisADException {
    this(type, null);
  }


  /**
   * Constructs from the type of function and a set of domain points.
   *
   * @param type                The type of function.
   * @param set                 The set of domain points.  Defines the units
   *                            and any coordinate system transformation
   *                            for the domain of the funciton.  May be
   *                            <code>null</code>, in which case the default
   *                            domain set ({@link FunctionType#getDomain()}) is
   *                            used.  May <em>not</em> be {@link FloatSet} or
   *                            {@link DoubleSet}.  If non-<code>null</code>, 
   *                            then must be compatible with the domain of the
   *                            FunctionType.
   * @throws CoordinateSystemException
   *                            if the {@link CoordinateSystem} of the
   *                            domain set is <code>null</code> but the
   *                            {@link CoordinateSystem} of the domain of
   *                            the FunctionType} is not; or if both {@link
   *                            CoordinateSystem}s are non-<code>null</code>
   *                            and do not have the same reference {@link
   *                            RealTupleType}.
   * @throws VisADException     if a VisAD failure occurs.
   */
  public FieldImpl(FunctionType type, Set set) throws VisADException {
      //Turn around and call the other ctor - the true says to go ahead 
      //and create the  Range array
      this (type, set, true);
  }

  /**
   * Trusted constructor for subclasses that don't need to have the
   * Range array instantiated (i.e., FlatField).
   *
   * @param type                The type of function.
   * @param set                 The set of domain points.  Defines the units
   *                            and any coordinate system transformation
   *                            for the domain of the funciton.  May be
   *                            <code>null</code>, in which case the default
   *                            domain set ({@link FunctionType#getDomain()}) is
   *                            used.  May <em>not</em> be {@link FloatSet} or
   *                            {@link DoubleSet}.  If non-<code>null</code>, 
   *                            then must be compatible with the domain of the
   *                            FunctionType.
   * @param createRangeArray    If true then the Range array is allocated.
   * @throws CoordinateSystemException
   *                            if the {@link CoordinateSystem} of the
   *                            domain set is <code>null</code> but the
   *                            {@link CoordinateSystem} of the domain of
   *                            the FunctionType} is not; or if both {@link
   *                            CoordinateSystem}s are non-<code>null</code>
   *                            and do not have the same reference {@link
   *                            RealTupleType}.
   * @throws VisADException     if a VisAD failure occurs.
   */
  protected FieldImpl(FunctionType type, Set set, boolean createRangeArray) 
    throws VisADException {

    super(type);

    RealTupleType DomainType = type.getDomain();

    if (set == null) set = DomainType.getDefaultSet();
    if (set == null) {
      throw new SetException("FieldImpl: set cannot be null");
    }

    if (set instanceof DoubleSet || set instanceof FloatSet) {
      throw new SetException("FieldImpl: set may not be DoubleSet " +
                             "or FloatSet");
    }
    if (DomainType.getDimension() != set.getDimension()) {
      throw new SetException("FieldImpl: set dimension " + set.getDimension() +
                             " and type dimension " +
                             DomainType.getDimension() + " don't match");
    }
    // force DomainSet Type to match DomainType
    if (DomainType.equals(((SetType) set.getType()).getDomain())) {
      DomainSet = set;
    }
    else {
      DomainSet = (Set) set.cloneButType(new SetType(DomainType));
    }
    DomainCoordinateSystem = DomainSet.getCoordinateSystem();
    CoordinateSystem domTypeCs = DomainType.getCoordinateSystem();
    if (domTypeCs == null 
          ? (DomainCoordinateSystem != null)
          : (DomainCoordinateSystem != null && !domTypeCs.getReference().equals(
              DomainCoordinateSystem.getReference()))) {
      throw new CoordinateSystemException(domTypeCs, DomainCoordinateSystem);
    }
    DomainUnits = DomainSet.getSetUnits();
    Length = DomainSet.getLength();
    if (createRangeArray) {
        MyRange = new Data[Length];
    }
    MissingFlag = true;
  }

    /**
       This  creates (if it has not been created already)  and returns the MyRange  data member
     **/
    private Data[] getRange () {
        synchronized (RangeLock) {
            if (MyRange == null) {
                MyRange = new Data[getLength()];
            }
        }
        return  MyRange;
    }

  /** 
   * Set the range samples of the function; the order of range samples
   * must be the same as the order of domain indices in the DomainSet;
   * copy range objects if copy is true;
   * @param range The range values 
   * @param copy should the range values be copied
   */
  public void setSamples(Data[] range, boolean copy)
         throws VisADException, RemoteException {
      setSamples(range, copy, true);
  }

  /** 
   * Set the range samples of the function; the order of range samples
   * must be the same as the order of domain indices in the DomainSet;
   * copy range objects if copy is true;
   * should use same MathType object in each Data object in range array 
   * @param range The range values 
   * @param copy should the range values be copied
   * @param checkAllRangeTypes  If true then ensure that the MathType of 
   *                            each element in the range matches the type 
   *                            of this field. If false then only check the 
   *                            first range element.  This is the "trust 
   *                            the calling method" flag. If you pass in false 
   *                            and there are elements in the range whose type 
   *                            does not match the type of this field then 
   *                            this may result in hard-to-track-down bugs.
   */
   public void setSamples(Data[] range, boolean copy, boolean checkAllRangeTypes)
         throws VisADException, RemoteException {

    if (range == null) {
        MyRange = null;
        MissingFlag = true;
        return;
    }

    if (range.length != getLength()) {
      throw new FieldException("FieldImpl.setSamples: bad array length");
    }

    Data[]Range = getRange ();


    synchronized (RangeLock) {
      MissingFlag = false;
      MathType t = ((FunctionType) Type).getRange();
      for (int i=0; i<getLength(); i++) {
        if (range[i] != null) {
          if(i==0 || checkAllRangeTypes) {
              if(!t.equals(range[i].getType())) {
                throw new TypeException("FieldImpl.setSamples: sample#" + i +
                                        " type " + range[i].getType() +
                                       " doesn't match field type " + t);
              }
          }
          if (copy) Range[i] = (Data) range[i].dataClone();
          else Range[i] = range[i];
        }
        else Range[i] = null;
      }

      for (int i=0; i<getLength(); i++) {
        if (Range[i] instanceof DataImpl) {
          ((DataImpl) Range[i]).setParent(this);
        }
      }
    }
    notifyReferences();
  }

  /**
   * <p>Returns the domain-set of this instance.  The actual set is returned:
   * it is not a copy or a clone.</p>
   *
   * <p> Note that it is possible to simultaneously modify the domain-set of
   * both this instance and of a clone by modifying the values in the array
   * returned by invoking <code>getSamples(false)</code> on the domain-set of
   * either this instance or the clone.  Don't do this unless you enjoy 
   * debugging.</p>
   *
   * @return                      The actual domain-set of this instance.
   */
  public Set getDomainSet() {
    return DomainSet;
  }

  /** get number of samples */
  public int getLength() {
    return Length;
  }

  /**
   * Returns the units of the values in the domain set.  The units may differ
   * from the default units of the underlying MathType of the domain, but will
   * be convertible with them.
   * @return                    The units of the values in the domain set.
   */
  public Unit[] getDomainUnits() {
    return DomainUnits;
  }

  public CoordinateSystem getDomainCoordinateSystem() {
    return DomainCoordinateSystem;
  }

  /** get range values for Text components; the return array is dimensioned
      double[number_of_range_components][number_of_range_samples] */
  public String[][] getStringValues()
         throws VisADException, RemoteException {
    TextType[] textComponents = ((FunctionType) Type).getTextComponents();
    if (textComponents == null) return null;
    int[] textIndices = ((FunctionType) Type).getTextIndices();
    int n = textComponents.length;
    int len = getLength();
    String[][] values = new String[n][len];
    if (isMissing()) {
      for (int k=0; k<n; k++) {
        for (int i=0; i<len; i++) values[k][i] = "";
      }
      return values;
    }

    MathType RangeType = ((FunctionType) Type).getRange();

    synchronized (RangeLock) {
      for (int i=0; i<len; i++) {
        Data range = (MyRange==null?null:MyRange[i]);
        if (range == null || range.isMissing()) {
          for (int k=0; k<n; k++) values[k][i] = "";
        }
        else {
          if (RangeType instanceof TextType) {
            values[0][i] = ((Text) range).getValue();
          }
          else if (RangeType instanceof TupleType) {
            for (int k=0; k<n; k++) {
              Text t = (Text) ((TupleIface) range).getComponent(textIndices[k]);
              values[k][i] = t.getValue();
            }
          }
        }
      } // end for (int i=0; i<len; i++)
    }
    return values;
  }

  public float[][] getFloats()
         throws VisADException, RemoteException {
    return getFloats(true);
  }

  /** get range values for 'Flat' components in their default range
      Units (as defined by the range of this FieldImpl's
      FunctionType); the return array is dimensioned
      float[number_of_range_components][number_of_range_samples];
      copy is ignored for FieldImpl */
  public float[][] getFloats(boolean copy)
         throws VisADException, RemoteException {
    return Set.doubleToFloat(getValues(copy));
  }

  public double[][] getValues()
         throws VisADException, RemoteException {
    return getValues(true);
  }

  /** get range values for 'Flat' components in their default range
      Units (as defined by the range of this FieldImpl's
      FunctionType); the return array is dimensioned
      double[number_of_range_components][number_of_range_samples];
      copy is ignored for FieldImpl */
  public double[][] getValues(boolean copy)
         throws VisADException, RemoteException {
    RealType[] realComponents = ((FunctionType) Type).getRealComponents();
    if (realComponents == null) return null;
    int n = realComponents.length;
    Unit[] units = getDefaultRangeUnits();
    int len = getLength();
    double[][] values = new double[n][len];

    if (isMissing()) {
      for (int k=0; k<n; k++) {
        for (int i=0; i<len; i++) values[k][i] = Double.NaN;
      }
      return values;
    }

    MathType RangeType = ((FunctionType) Type).getRange();

    synchronized (RangeLock) {
      for (int i=0; i<len; i++) {
        Data range = (MyRange == null? null: MyRange[i]);
        if (range == null || range.isMissing()) {
          for (int k=0; k<n; k++) values[k][i] = Double.NaN;
        }
        else {
          if (RangeType instanceof RealType) {
            values[0][i] = ((Real) range).getValue(units[0]);
          }
          else if (RangeType instanceof TupleType) {
            int k = 0;
            for (int j=0; j<((TupleType) RangeType).getDimension(); j++) {
              MathType component_type = ((TupleType) RangeType).getComponent(j);
              Data component = ((TupleIface) range).getComponent(j);
              if (component_type instanceof RealType) {
                values[k][i] = ((Real) component).getValue(units[k]);
                k++;
              }
              else if (component_type instanceof RealTupleType) {
                for (int m=0; m<((TupleType) component_type).getDimension(); m++) {
                  Data comp_comp = ((TupleIface) component).getComponent(m);
                  values[k][i] = ((Real) comp_comp).getValue(units[k]);
                  k++;
                }
              }
            }
          }
        }
      } // end for (int i=0; i<len; i++)
    }
    return values;
  }

  /** set range array as range values of this FieldImpl;
      this must have a Flat range; the array is dimensioned
      float[number_of_range_components][number_of_range_samples];
      the order of range values must be the same as the order of domain
      indices in the DomainSet */
  public void setSamples(double[][] range)
         throws VisADException, RemoteException {
    RealType[] realComponents = ((FunctionType) Type).getRealComponents();
    if (!((FunctionType) Type).getFlat()) {
      throw new FieldException("FieldImpl.setSamples: not Flat range");
    }
    if (realComponents == null) {
      throw new FieldException("FieldImpl.setSamples: no Real components");
    }
    int n = realComponents.length;
    int len = getLength();
    if (range == null || range.length != n) {
      throw new FieldException("FieldImpl.setSamples: bad tuple length");
    }
    if (range[0] == null || range[0].length != len) {
      throw new FieldException("FieldImpl.setSamples: bad array length");
    }
    Unit[] units = getDefaultRangeUnits();

    MathType RangeType = ((FunctionType) Type).getRange();

    synchronized (RangeLock) {
      MissingFlag = false;
      Data[]Range = getRange ();
      if (RangeType instanceof RealType) {
        for (int i=0; i<len; i++) {
          Range[i] = new Real((RealType) RangeType, range[0][i], units[0]);
        }
      }
      else if (RangeType instanceof RealTupleType) {
        int ntup = ((RealTupleType) RangeType).getDimension();
        Real[] reals = new Real[ntup];
        for (int i=0; i<len; i++) {
          for (int j=0; j<ntup; j++) {
            RealType type = (RealType)
              ((RealTupleType) RangeType).getComponent(j);
            reals[j] = new Real(type, range[j][i], units[j]);
          }
          Range[i] = new RealTuple(reals);
        }
      }
      else if (RangeType instanceof TupleType) {
        int ntup = ((TupleType) RangeType).getDimension();
        Data[] data = new Real[ntup];
        MathType[] types = new MathType[ntup];
        for (int j=0; j<ntup; j++) {
          types[j] = ((TupleType) RangeType).getComponent(j);
        }
        for (int i=0; i<len; i++) {
          int k = 0;
          for (int j=0; j<ntup; j++) {
            if (types[j] instanceof RealType) {
              data[j] = new Real((RealType) types[j], range[k][i], units[k]);
              k++;
            }
            else { // types[j] instanceof RealTupleType
              int mtup = ((RealTupleType) types[j]).getDimension();
              Real[] reals = new Real[mtup];
              for (int m=0; m<mtup; m++) {
                RealType type = (RealType)
                  ((RealTupleType) types[j]).getComponent(m);
                reals[m] = new Real(type, range[k][i], units[k]);
                k++;
              }
              data[j] = new RealTuple(reals);
            }
          } // end for (int j=0; j<ntup; j++)
          Range[i] = new Tuple(data);
        } // end for (int i=0; i<len; i++)
      }
    }
    return;
  }

  /** set range array as range values of this FieldImpl;
      this must have a Flat range; the array is dimensioned
      float[number_of_range_components][number_of_range_samples];
      the order of range values must be the same as the order of domain
      indices in the DomainSet */
  public void setSamples(float[][] range)
         throws VisADException, RemoteException {
    setSamples(Set.floatToDouble(range));
  }

  /** return array of Units associated with each RealType
      component of range; these may differ from default
      Units of range RealTypes, but must be convertable;
      the second index enumerates samples since Units may
      differ between samples */
  public Unit[][] getRangeUnits()
         throws VisADException, RemoteException {
    RealType[] realComponents = ((FunctionType) Type).getRealComponents();
    if (realComponents == null) return null;
    int n = realComponents.length;
    Unit[][] units = new Unit[n][getLength()];
    Unit[] default_units = getDefaultRangeUnits();

    MathType RangeType = ((FunctionType) Type).getRange();

    for (int i=0; i<getLength(); i++) {
      Data range = (MyRange==null?null:MyRange[i]);
      if (range == null || range.isMissing()) {
        for (int k=0; k<n; k++) units[k][i] = default_units[k];
      }
      else {
        if (RangeType instanceof RealType) {
          units[0][i] = ((Real) range).getUnit();
        }
        else if (RangeType instanceof TupleType) {
          int k = 0;
          for (int j=0; j<((TupleType) RangeType).getDimension(); j++) {
            MathType component_type = ((TupleType) RangeType).getComponent(i);
            Data component = ((TupleIface) range).getComponent(j);
            if (component_type instanceof RealType) {
              units[k][i] = ((Real) component).getUnit();
              k++;
            }
            else if (component_type instanceof RealTupleType) {
              for (int m=0; m<((TupleType) component_type).getDimension(); m++) {
                Data comp_comp = ((TupleIface) component).getComponent(m);
                units[k][i] = ((Real) comp_comp).getUnit();
                k++;
              }
            }
          }
        }
      }
    }
    return units;
  }

  /** 
   * Get range CoordinateSystem for 'RealTuple' range;
   * second index enumerates samples.
   * @return range CoordinateSystem assuming range type is
   * a RealTupleType (throws a TypeException if its not);
   * this may differ from default CoordinateSystem of
   * range RealTupleType, but must be convertable;
   * the index enumerates samples since Units may
   * differ between samples 
   */
  public CoordinateSystem[] getRangeCoordinateSystem()
         throws VisADException, RemoteException {
    MathType RangeType = ((FunctionType) Type).getRange();
    if (!(RangeType instanceof RealTupleType)) {
      throw new TypeException("FieldImpl.getRangeCoordinateSystem: " +
        "Range is not RealTupleType");
    }

    CoordinateSystem[] cs = new CoordinateSystem[getLength()];
    CoordinateSystem default_cs =
      ((RealTupleType) RangeType).getCoordinateSystem();

    for (int i=0; i<getLength(); i++) {
      Data range = (MyRange==null?null:MyRange[i]);
      if (range == null || range.isMissing()) {
        cs[i] = default_cs;
      }
      else {
        cs[i] = ((RealTuple) range).getCoordinateSystem();
      }
    }
    return cs;
  }

  /** get range CoordinateSystem for 'RealTuple' components;
      second index enumerates samples */
  public CoordinateSystem[] getRangeCoordinateSystem(int component)
         throws VisADException, RemoteException {
    MathType RangeType = ((FunctionType) Type).getRange();
    if ( (!(RangeType instanceof TupleType)) ||
         (RangeType instanceof RealTupleType) ) {
      throw new TypeException("FieldImpl.getRangeCoordinateSystem: " +
        "Range must be TupleType but not RealTupleType");
    }

    MathType component_type =
      ((TupleType) RangeType).getComponent(component);

    if (!(component_type instanceof RealTupleType)) {
      throw new TypeException("FieldImpl.getRangeCoordinateSystem: " +
        "selected Range component must be RealTupleType");
    }

    CoordinateSystem[] cs = new CoordinateSystem[getLength()];

    CoordinateSystem default_cs =
      ((RealTupleType) component_type).getCoordinateSystem();

    for (int i=0; i<getLength(); i++) {
      Data range = (MyRange==null?null:MyRange[i]);
      if (range == null || range.isMissing()) {
        cs[i] = default_cs;
      }
      else {
        Data comp = ((TupleIface) range).getComponent(component);
        if (comp == null || comp.isMissing()) {
          cs[i] = default_cs;
        }
        else {
          cs[i] = ((RealTuple) comp).getCoordinateSystem();
        }
      }
    }
    return cs;
  }

  /** get default range Unit-s for 'Flat' components */
  public Unit[] getDefaultRangeUnits() {
    RealType[] realComponents = ((FunctionType) Type).getRealComponents();
    if (realComponents == null) return null;
    int n = realComponents.length;
    Unit[] units = new Unit[n];
    for (int i=0; i<n; i++) {
      units[i] = realComponents[i].getDefaultUnit();
    }
    return units;
  }

  /**
   * <p>Get the range value at the index-th sample.  The actual range value
   * is returned -- not a copy.</p>
   *
   * <p>This implementation uses {@link #getSample(int, boolean)}.</p>
   *
   * @param index index of requested range sample
   * @return range value at sample index
   */
  
  public Data getSample(int index)
         throws VisADException, RemoteException {
    return getSample(index, false);
  }

  /**
   * Get the metadata for the range value at the index-th sample.  If the range
   * value is also requested, then the actual range value is returned -- not a
   * copy.</p>
   *
   * @param index index of requested range sample
   * @param metadataOnly <tt>true</tt> if only the metadata is needed,
   *                     <tt>false</ff> if both metadata and data are
   *                     desired.
   */
  public Data getSample(int index, boolean metadataOnly)
         throws VisADException, RemoteException {
    synchronized (RangeLock) {
      if (MyRange == null || isMissing() || index < 0 || index >= getLength() || MyRange[index] == null) {
        return ((FunctionType) Type).getRange().missingData();
      }
      else return MyRange[index];
    }
  }

  /** set the range value at the sample nearest to domain */
  public void setSample(RealTuple domain, Data range, boolean copy)
         throws VisADException, RemoteException {
    if (getDomainSet() == null) {
      throw new FieldException("FieldImpl.setSample: DomainSet undefined");
    }
    // WLH 9 Dec 99
    // if (!((FunctionType) Type).getDomain().equalsExceptName(domain.getType())) {
    if (!((FunctionType) Type).getDomain().equals(domain.getType())) {
      throw new TypeException("FieldImpl.setSample: bad domain type");
    }

    int dimension = getDomainSet().getDimension();
    double[][] vals = new double[dimension][1];
    for (int j=0; j<dimension; j++) {
      vals[j][0] = ((Real) ((RealTuple) domain).getComponent(j)).getValue();
    }
    // always use simple resampling for set
    int[] indices = getDomainSet().doubleToIndex(vals);
    setSample(indices[0], range, copy);
  }

  public void setSample(RealTuple domain, Data range)
         throws VisADException, RemoteException {
    setSample(domain, range, true);
  }

  /** 
   * Set the range value at the index-th sample; makes a local copy
   * @param  index  index in domain
   * @param  range  sample at that index
   */
  public void setSample(int index, Data range)
         throws VisADException, RemoteException {
    setSample(index, range, true);
  }

  /** 
   * Set the range value at the index-th sample 
   * @param  index  index in domain
   * @param  range  sample at that index
   * @param  copy   true to make a copy
   */
  public void setSample(int index, Data range, boolean copy)
         throws VisADException, RemoteException {
    setSample(index, range, copy, true);
  }

  /** 
   * Set the range value at the index-th sample 
   * @param  index  index in domain
   * @param  range  sample at that index
   * @param  copy   true to make a copy
   * @param  checkRangeType  setting to false will not check to make
   *                         sure that the sample MathType is the same as
   *                         this FieldImpl's range.  This saves on time
   *                         at the expense of accuracy.  Use this only
   *                         if you like shooting yourself in the foot.
   */
  public void setSample(int index, Data range, boolean copy, boolean checkRangeType)
         throws VisADException, RemoteException {
    if (getDomainSet() == null) {
      throw new FieldException("FieldImpl.setSample: DomainSet undefined");
    }
    if (range != null && checkRangeType &&
        !((FunctionType) Type).getRange().equals(range.getType())) {
      throw new TypeException("FieldImpl.setSample: bad range type");
    }
    if (index >= 0 && index < getLength()) {
      Data[]Range = getRange ();
      synchronized (RangeLock) {
        MissingFlag = false;
        if (range != null) {
          if (copy) {
            Range[index] = (Data) range.dataClone();
          }
          else {
            Range[index] = range;
          }
          if (Range[index] instanceof DataImpl) {
            ((DataImpl) Range[index]).setParent(this);
          }
        }
        else {
          Range[index] = null;
        }
      }
    }
    notifyReferences();
  }

  /** test whether Field value is missing */
  public boolean isMissing() {
    synchronized (RangeLock) {
      return MissingFlag;
    }
  }

  /** return new Field with value 'this op data';
      test for various relations between types of this and data */
  /*- TDR  May 1998
  public Data binary(Data data, int op, int sampling_mode, int error_mode)
              throws VisADException, RemoteException {
   */
  public Data binary(Data data, int op, MathType new_type,
                     int sampling_mode, int error_mode)
              throws VisADException, RemoteException {
    boolean field_flag; // true if this and data have same type
    if ( new_type == null ) {
      throw new TypeException("binary: new_type may not be null" );
    }
    if (Type.equalsExceptName(data.getType())) {
      /*-  TDR  May 1998  */
      if ( !Type.equalsExceptName( new_type )) {
        throw new TypeException("binary: new_type doesn't match return type");
      }
      /*- end  */
      // type of this and data match, so normal Field operation
      field_flag = true;
      if (((Field) data).isFlatField()) {
        // force (data instanceof FlatField) to be true
        data = data.local();
        // this and data have same type, but data is Flat and this is not
        data = ((FlatField) data).convertToField();
      }
    }
    else if (data instanceof Real ||
             ((FunctionType) Type).getRange().equalsExceptName(data.getType())) {
      // data is real or matches range type of this
      field_flag = false;
      /*-  TDR May 1998  */
      if ( !Type.equalsExceptName( new_type )) {
        throw new TypeException("binary: new_type doesn't match return type");
      }
      /*-  end  */
    }
    else if (data instanceof Field &&
             ((FunctionType) data.getType()).getRange().equalsExceptName(Type)) {

      /*- TDR  May 1998 */
      if ( !((FunctionType) data.getType()).getRange().equalsExceptName(new_type)) {
        throw new TypeException("binary: new_type doesn't match return type");
      }
      /*- end */
      // this matches range type of data
      // note invertOp to reverse order of operands
      /*- TDR  May 1998
      return data.binary(this, invertOp(op), sampling_mode, error_mode);
       */
      return data.binary(this, invertOp(op), new_type, sampling_mode, error_mode);
    }
    else {
      throw new TypeException("FieldImpl.binary: types don't match");
    }
    // create (initially missing) Field for return
    Field new_field = new FieldImpl((FunctionType) new_type, getDomainSet());
    if (isMissing() || data.isMissing()) return new_field;
    Data[] range = new Data[getLength()];
    /*- TDR  May 1998  */
    MathType m_type = ((FunctionType)new_type).getRange();


    if (field_flag) {
      // resample data if needed
      data = ((Field) data).resample(getDomainSet(), sampling_mode, error_mode);
      // apply operation to each range object
      for (int i=0; i<getLength(); i++) {
        synchronized (RangeLock) {
          range[i] = ((MyRange==null||MyRange[i] == null)) ? null :
                    /*-  TDR May 1998
                     Range[i].binary(((Field) data).getSample(i), op,
                                     sampling_mode, error_mode);
                     */
                     MyRange[i].binary(((Field) data).getSample(i), op, m_type,
                                     sampling_mode, error_mode);

        }
      }
    }
    else { // !field_flag
      for (int i=0; i<getLength(); i++) {
        synchronized (RangeLock) {
          range[i] = ((MyRange==null||MyRange[i] == null)) ? null :
                     /*- TDR  May 1998
                     MyRange[i].binary(data, op, sampling_mode, error_mode);
                      */
                     MyRange[i].binary(data, op, m_type, sampling_mode, error_mode);
        }
      }
    }
    new_field.setSamples(range, false);
    return new_field;
  }


  /** return new Field with value 'op this' */
  /*- TDR  July  1998
  public Data unary(int op, int sampling_mode, int error_mode)
              throws VisADException, RemoteException {
  */
  public Data unary(int op, MathType new_type, int sampling_mode,
                    int error_mode )
              throws VisADException, RemoteException {
    if ( new_type == null ) {
      throw new TypeException("unary: new_type may not be null");
    }
    if ( !Type.equalsExceptName(new_type)) {
      throw new TypeException("unary: new_type doesn't match return type");
    }
    MathType m_type = ((FunctionType)new_type).getRange();
    // create (initially missing) Field for return
/* WLH 17 Jan 2000
    Field new_field = new FieldImpl((FunctionType) Type, getDomainSet());
*/
/* WLH 3 April 2003
    Field new_field = new FieldImpl((FunctionType) new_type, getDomainSet());
*/
    RealTupleType d_type = ((FunctionType)new_type).getDomain();
    Set new_set = null;
    if (!d_type.equals( ((FunctionType) getType()).getDomain() )) {
      new_set = (Set) getDomainSet().cloneButType(d_type);
    }
    else {
      new_set = getDomainSet();
    }
    Field new_field = new FieldImpl((FunctionType) new_type, new_set);

    if (isMissing()) return new_field;
    Data[] range = new Data[getLength()];


    // apply operation to each range object
    for (int i=0; i<getLength(); i++) {
      synchronized (RangeLock) {
        range[i] = ((MyRange==null||MyRange[i] == null)) ? null :
                   MyRange[i].unary(op, m_type, sampling_mode, error_mode);
      }
    }
    new_field.setSamples(range, false);
    return new_field;
  }

  /** 
   * Resample all elements of the fields array to the domain
   * set of fields[0], then return a Field whose range samples
   * are Tuples merging the corresponding range samples from
   * each element of fields; if the range of fields[i] is a
   * Tuple without a RangeCoordinateSystem, then each Tuple
   * component of a range sample of fields[i] becomes a
   * Tuple component of a range sample of the result -
   * otherwise a range sample of fields[i] becomes a Tuple
   * component of a range sample of the result; this assumes
   * all elements of the fields array have the same domain
   * dimension; use default sampling_mode (Data.NEAREST_NEIGHBOR)
   * and default error_mode (Data.NO_ERRORS) 
   *
   * @param fields  fields to combine
   * @return  combined fields
   */
  public static Field combine( Field[] fields )
                throws VisADException, RemoteException
  {
    return combine( fields, Data.NEAREST_NEIGHBOR, Data.NO_ERRORS, true);
  }

  /** 
   * Resample all elements of the fields array to the domain
   * set of fields[0], then return a Field whose range samples
   * are Tuples merging the corresponding range samples from
   * each element of fields.  If <code>flatten</code> is true and 
   * if the range of fields[i] is a
   * Tuple without a RangeCoordinateSystem, then each Tuple
   * component of a range sample of fields[i] becomes a
   * Tuple component of a range sample of the result -
   * otherwise a range sample of fields[i] becomes a Tuple
   * component of a range sample of the result; this assumes
   * all elements of the fields array have the same domain
   * dimension; use default sampling_mode (Data.NEAREST_NEIGHBOR)
   * and default error_mode (Data.NO_ERRORS) 
   *
   * @param fields  fields to combine
   * @param flatten   true to flatten range tuples with no CoordinateSystem
   * @return  combined fields
   */
  public static Field combine( Field[] fields, boolean flatten )
                throws VisADException, RemoteException
  {
    return combine( fields, Data.NEAREST_NEIGHBOR, Data.NO_ERRORS, flatten );
  }

  /** resample all elements of the fields array to the domain
      set of fields[0], then return a Field whose range samples
      are Tuples merging the corresponding range samples from
      each element of fields; if the range of fields[i] is a
      Tuple without a RangeCoordinateSystem, then each Tuple
      component of a range sample of fields[i] becomes a
      Tuple component of a range sample of the result -
      otherwise a range sample of fields[i] becomes a Tuple
      component of a range sample of the result; this assumes
      all elements of the fields array have the same domain
      dimension */
  public static Field combine( Field[] fields, int sampling_mode, int error_mode )
                throws VisADException, RemoteException
  {
    return combine( fields, sampling_mode, error_mode, true);
  }

  /** 
   * Resample all elements of the fields array to the domain
   * set of fields[0], then return a Field whose range samples
   * are Tuples merging the corresponding range samples from
   * each element of fields.  If <code>flatten</code> is true and 
   * if the range of fields[i] is a
   * Tuple without a RangeCoordinateSystem, then each Tuple
   * component of a range sample of fields[i] becomes a
   * Tuple component of a range sample of the result -
   * otherwise a range sample of fields[i] becomes a Tuple
   * component of a range sample of the result; this assumes
   * all elements of the fields array have the same domain
   * dimension.
   *
   * @param fields  fields to combine
   * @param sampling_mode   sampling mode to use (e.g., Data.NEAREST_NEIGHBOR)
   * @param error_mode   error mode to use (e.g., Data.NO_ERRORS)
   * @param flatten   true to flatten range tuples with no CoordinateSystem
   *
   * @return  combined fields
   */
  public static Field combine( Field[] fields, int sampling_mode, int error_mode , boolean flatten)
                throws VisADException, RemoteException
  {
    return combine(fields, sampling_mode, error_mode, flatten, true);
  }

  /** 
   * Resample all elements of the fields array to the domain
   * set of fields[0], then return a Field whose range samples
   * are Tuples merging the corresponding range samples from
   * each element of fields.  If <code>flatten</code> is true and 
   * if the range of fields[i] is a
   * Tuple without a RangeCoordinateSystem, then each Tuple
   * component of a range sample of fields[i] becomes a
   * Tuple component of a range sample of the result -
   * otherwise a range sample of fields[i] becomes a Tuple
   * component of a range sample of the result; this assumes
   * all elements of the fields array have the same domain
   * dimension.
   *
   * @param fields  fields to combine
   * @param sampling_mode   sampling mode to use (e.g., Data.NEAREST_NEIGHBOR)
   * @param error_mode   error mode to use (e.g., Data.NO_ERRORS)
   * @param flatten   true to flatten range tuples with no CoordinateSystem
   * @param copy   true to copy the values from the original fields
   *
   * @return  combined fields
   */
  public static Field combine( Field[] fields, int sampling_mode, int error_mode , boolean flatten, boolean copy)
                throws VisADException, RemoteException
  {
    visad.util.Trace.call1("combine, copy = " + copy);
    int ii, jj, kk, n_fields;
    int domainDim = 0;                 //- domain dimension of field_0.
    Set domainSet_0;                   //- domain set of field_0.
    RealTupleType domainType_0;        //- domain type of field_0.
    int n_samples_0;                   //- number of samples in field_0.
    Field new_field;                   //- return field.
    boolean allFlat = true;            //- all fields in array are FlatFields.
    Field field_0 = fields[0];         //- first field in array, "this".
    FunctionType new_type;             //- type of result.
    n_fields = fields.length;
    MathType[] fieldRange_s = new MathType[ n_fields ];
    MathType[][] rangeComp_s = new MathType[ n_fields ][];
    FunctionType[] fieldType_s = new FunctionType[ n_fields ];
    MathType new_range, range;
    MathType m_type;
    MathType fieldRange;
    FlatField f_field;
    float[][] valuesF = null;
    float[][] new_valuesF = null;
    double[][] valuesD = null;
    double[][] new_valuesD = null;
    int n_comps;
    int n_dims;
    int length;
    int cnt = 0;

    n_fields = fields.length;
    n_comps = 0;

    for ( ii = 0; ii < n_fields; ii++ )
    {
      if ( ii == 0 )
      {
        domainDim = field_0.getDomainDimension();
      }
      else if ( domainDim != fields[ii].getDomainDimension() )
      {
        throw new VisADException( "FieldImpl.combine: domain dimensions of input"+
                                  "fields must match" );
      }

      if ( !(fields[ii].isFlatField()) )
      {
        allFlat = false;
      }

      fieldType_s[ii] = (FunctionType) fields[ii].getType();
      fieldRange_s[ii] = ((FunctionType)fields[ii].getType()).getRange();
      range = fieldRange_s[ii];

      if ( range instanceof RealType )
      {
        rangeComp_s[ii] = new MathType[ 1 ];
        rangeComp_s[ii][0] = range;
        n_comps++;
      }
      else if ( range instanceof RealTupleType )
      {
        rangeComp_s[ii] = new MathType[ 1];
        rangeComp_s[ii][0] = range;
        n_comps++;
      }
      else if ( range instanceof TupleType )
      {
        n_dims = ((TupleType)range).getDimension();
        rangeComp_s[ii] = new MathType[ n_dims ];
        for ( jj = 0; jj < n_dims; jj++ ) {
          rangeComp_s[ii][jj] = ((TupleType)range).getComponent(jj);
        }
        n_comps += n_dims;
      }
      else
      {
        rangeComp_s[ii] = new MathType[1];
        rangeComp_s[ii][0] = range;
        n_comps++;
      }
    }

    domainSet_0 = field_0.getDomainSet();
    domainType_0 = ((FunctionType)field_0.getType()).getDomain();
    n_samples_0 = domainSet_0.getLength();

    if ( allFlat )
    {
      int n_sets;
      int n_sys;
      CoordinateSystem[] rangeCoordSys_s;
      Field field;

      boolean allReal = true;
      int tupleDim = 0;
      cnt = 0;

      Vector coordsys_s = new Vector();
      Vector m_types = new Vector();
      Vector r_types = new Vector();

      for ( ii = 0; ii < n_fields; ii++ )
      {
        field = fields[ii];
        fieldRange = fieldRange_s[ii];

        if ( fieldRange instanceof RealType )
        {
          m_types.add( fieldRange );
          r_types.add( fieldRange );
          rangeCoordSys_s = field.getRangeCoordinateSystem();
          coordsys_s.add( rangeCoordSys_s[0] );
          tupleDim++;
        }
        else if ( fieldRange instanceof RealTupleType )
        {
          n_dims = ((RealTupleType)fieldRange).getDimension();
          tupleDim += n_dims;
          rangeCoordSys_s = field.getRangeCoordinateSystem();
          if ( rangeCoordSys_s[0] == null && flatten)
          {
            for ( jj = 0; jj < n_dims; jj++ )
            {
              m_type = ((RealTupleType)fieldRange).getComponent(jj);
              m_types.add( m_type );
              r_types.add( m_type );
              coordsys_s.add( null );
            }
          }
          else
          {
            m_types.add( fieldRange );
            coordsys_s.add( rangeCoordSys_s[0] );
            allReal = false;
          }
        }
        else //- must be Flat TupleType   -*
        {
          for ( jj = 0; jj < rangeComp_s[ii].length; jj++ )
          {
            rangeCoordSys_s = field.getRangeCoordinateSystem(jj);
            m_type = rangeComp_s[ii][jj];

            if ( m_type instanceof RealType )
            {
              r_types.add( m_type );
              m_types.add( m_type );
              coordsys_s.add( rangeCoordSys_s[0] );
              tupleDim++;
            }
            else if ( m_type instanceof RealTupleType )
            {
              n_dims = ((RealTupleType)m_type).getDimension();
              tupleDim += n_dims;
              if ( rangeCoordSys_s[0] == null && flatten)
              {
                for ( kk = 0; kk < n_dims; kk++ )
                {
                  m_types.add( ((RealTupleType)m_type).getComponent(kk) );
                  r_types.add( ((RealTupleType)m_type).getComponent(kk) );
                  coordsys_s.add( null );
                }
              }
              else
              {
                m_types.add( m_type );
                coordsys_s.add( rangeCoordSys_s[0] );
                allReal = false;
              }
            }
          }
        }
      }

      if ( allReal )
      {
        length = r_types.size();
        RealType[] r_array =  new RealType[ length ];
        for ( ii = 0; ii < length; ii++ ) {
          r_array[ii] = (RealType) r_types.elementAt(ii);
        }
        new_range = new RealTupleType( r_array );
      }
      else
      {
        length = m_types.size();
        MathType[] m_array =  new MathType[ length ];
        for ( ii = 0; ii < length; ii++ ) {
          m_array[ii] = (MathType) m_types.elementAt(ii);
        }
        new_range = new TupleType( m_array );
      }

      new_type = new FunctionType( domainType_0, new_range );

      length = coordsys_s.size();
      CoordinateSystem[] all_rangeCoordSys_s = new CoordinateSystem[ length ];
      for ( ii = 0; ii < length; ii++ )
      {
        all_rangeCoordSys_s[ii] = (CoordinateSystem) coordsys_s.elementAt(ii);
      }

      Set[] rangeSets;
      Set[] new_rangeSets = new Set[ tupleDim ];
      Unit[][] rangeUnits;
      Unit[] new_rangeUnits = new Unit[ tupleDim ];
      Unit[] sub_units;

      int cnt_a = 0;
      int cnt_b = 0;
      int cnt_c = 0;
      int cnt_u = 0;
      int n_coordsys = 0;
      int n_units;
      boolean allFloat = true;  // default set for FlatField
      FlatField[] resampledFields = new FlatField[n_fields];

      for ( ii = 0; ii < n_fields; ii++ )
      {
        f_field = (FlatField) fields[ii].local();

        if ( ii > 0 ) //- don't resample field to itself even though this would
        {             //- probably be no-op anyway.

          f_field = (FlatField) f_field.resample( domainSet_0, sampling_mode, error_mode );
        }
        resampledFields[ii] = f_field;

        //-  collect rangeSets for each tuple dimension  -*

        rangeSets = f_field.getRangeSets();
        n_sets = rangeSets.length;
        // check for float sets
        if (allFloat) {
          for (int s = 0; s < n_sets; s++) {
            if (!(rangeSets[s] instanceof FloatSet)) {
              allFloat = false;
              break;
            }
          }
        }

        System.arraycopy( rangeSets, 0, new_rangeSets, cnt_a, n_sets );
        cnt_a += n_sets;

        /* Don't need to do this because getSamples/Values uses default units)
        //-  collect rangeUnits for each tuple dimension  -*
        rangeUnits = f_field.getRangeUnits();
        n_units = rangeUnits.length;
        sub_units = new Unit[ n_units ];
        for ( jj = 0; jj < n_units; jj++ ) {
          sub_units[jj] = rangeUnits[jj][0];
        }
        System.arraycopy( sub_units, 0, new_rangeUnits, cnt_u, n_units );
        cnt_u += n_units;
        */
      }
      // metadata collected.  Now get Data

      if (allFloat) {
         new_valuesF = new float[ tupleDim ][];
      } 
      else {
         new_valuesD = new double[ tupleDim ][];
      }

      for (int f = 0; f < n_fields; f++) {
        
        //-  get range values for each field, and combine into one array  -*
        if (allFloat) {
          valuesF = resampledFields[f].getFloats(copy);
          for ( jj = 0; jj < valuesF.length; jj++ )
          {
            new_valuesF[cnt_b++] = valuesF[jj];
          }
        }
        else {
          valuesD = resampledFields[f].getValues(copy);
          for ( jj = 0; jj < valuesD.length; jj++ )
          {
            new_valuesD[cnt_b++] = valuesD[jj];
          }
        }
      }

      //- data and metadata collected.  Make new flatfield  -*

      if ( new_type.getReal() )
      {
        new_field = new FlatField( new_type, domainSet_0, null, null,
                                   new_rangeSets, null );
      }
      else
      {
        new_field = new FlatField( new_type, domainSet_0, all_rangeCoordSys_s,
                                   new_rangeSets, null );
      }

      //- set range values for new flatfield  -*
      if (allFloat) {
        ((FlatField)new_field).setSamples( new_valuesF, false );
      } 
      else {
        ((FlatField)new_field).setSamples( new_valuesD, false );
      }
    }
    else  //- not all FlatField(s)  -*
    {
      Vector sub_types = new Vector();
      Field field;
      Data[] data_s;
      Data data;
      Data rangeData;
      boolean allReal = true;
      RealTuple R_tuple;

      Vector[] v_array = new Vector[ n_samples_0 ];
      for ( ii = 0; ii < n_samples_0; ii++ ) {
         v_array[ii] = new Vector();
      }

      for ( ii = 0; ii < n_fields; ii++ )
      {
        if ( ii == 0 ) //- resample fields to domain of first
        {
          field = (Field) fields[ii].local();
        }
        else
        {
          field = fields[ii].resample( domainSet_0, sampling_mode, error_mode );
        }

        fieldRange = fieldRange_s[ii];
        if ( fieldRange instanceof RealType )
        {
          sub_types.add( fieldRange );
          for ( kk = 0; kk < n_samples_0; kk++ ) {
            v_array[kk].add( field.getSample(kk) );
          }
        }
        else if ( fieldRange instanceof RealTupleType )
        {
          if ( ((RealTupleType)fieldRange).getCoordinateSystem() != null )
          {
            sub_types.add( fieldRange );
            allReal = false;
            for ( kk = 0; kk < n_samples_0; kk++ ) {
              v_array[kk].add( field.getSample(kk) );
            }
          }
          else
          {
            n_dims = ((RealTupleType)fieldRange).getDimension();
            for ( jj = 0; jj < n_dims; jj++ )
            {
              sub_types.add( ((RealTupleType)fieldRange).getComponent(jj) );
            }
            for ( kk = 0; kk < n_samples_0; kk++ ) {
              rangeData = field.getSample(kk);
              for ( jj = 0; jj < n_dims; jj++ ) {
                 v_array[kk].add( ((RealTuple)rangeData).getComponent(jj) );
              }
            }
          }
        }
        else if( (fieldRange instanceof TupleType) &&
                !(fieldRange instanceof RealTupleType) )
        {
          if ( !(((TupleType)fieldRange).getFlat()) )
          {
            sub_types.add( fieldRange );
            for ( kk = 0; kk < n_samples_0; kk++ ) {
              v_array[kk].add( field.getSample(kk) );
            }
            allReal = false;
          }
          else
          {
            n_dims = ((TupleType)fieldRange).getDimension();
            for ( ii = 0; ii < n_dims; ii++ )
            {
              m_type = ((TupleType)fieldRange).getComponent(ii);
              if ( m_type instanceof RealType )
              {
                sub_types.add( m_type );
                for ( kk = 0; kk < n_samples_0; kk++ ) {
                  v_array[kk].add( ((TupleIface)field.getSample(kk)).getComponent(ii));
                }
              }
              else if ( m_type instanceof RealTupleType )
              {
                if ( ((RealTupleType)m_type).getCoordinateSystem() != null )
                {
                  sub_types.add( m_type );
                  allReal = false;
                  for ( kk = 0; kk < n_samples_0; kk++ ) {
                    v_array[kk].add( ((TupleIface)field.getSample(kk)).getComponent(ii));
                  }
                }
                else
                {
                  for ( jj = 0; jj < ((RealTupleType)m_type).getDimension(); jj++ )
                  {
                    sub_types.add( ((RealTupleType)m_type).getComponent(jj) );
                  }
                  for ( kk = 0; kk < n_samples_0; kk++ ) {
                    R_tuple = (RealTuple)((TupleIface)field.getSample(kk)).getComponent(ii);
                    for ( jj = 0; jj < ((RealTupleType)m_type).getDimension(); jj++ ) {
                      v_array[kk].add( R_tuple.getComponent(jj));
                    }
                  }
                }
              }
            }
          }
        }
        else if ( fieldRange instanceof FunctionType )
        {
          sub_types.add( fieldRange );
          for ( kk = 0; kk < n_samples_0; kk++ ) {
            v_array[kk].add( field.getSample(kk) );
          }
          allReal = false;
        }
      } //- all fields loop -*

      int size = sub_types.size();

      if ( allReal )
      {
        RealType[] r_types = new RealType[ size ];
        for ( ii = 0; ii < size; ii++ ) {
          r_types[ii] = (RealType) sub_types.elementAt(ii);
        }
        new_range = new RealTupleType( r_types );
      }
      else
      {
        MathType[] m_types = new MathType[ size ];
        for ( ii = 0; ii < size; ii++ ) {
          m_types[ii] = (MathType) sub_types.elementAt(ii);
        }
        new_range = new TupleType( m_types );
      }

      new_type = new FunctionType( domainType_0, new_range );

      new_field = new FieldImpl( new_type, domainSet_0 );

      for ( ii = 0; ii < n_samples_0; ii++ )
      {
        size = v_array[ii].size();
        data_s = new Data[ size ];
        for ( jj = 0; jj < size; jj++ )
        {
          data_s[jj] = (Data) v_array[ii].elementAt( jj );
        }
        data = new Tuple( data_s );
        new_field.setSample( ii, data, copy );
      }
    }

    visad.util.Trace.call2("combine, copy = " + copy);
    return new_field;
  }

  /** extract Field from this.component using the MathType
  * of one of the range componenets
  */

  public Field extract(MathType type) throws VisADException, RemoteException {
    int index = -1;
    MathType rangeType = ((FunctionType)Type).getRange();
    if (!(rangeType instanceof TupleType)) {
      throw new VisADException("FieldImpl.extract: range must be a TupleType");
    }
    int n_comps = ((TupleType)rangeType).getDimension();
    for (int i=0; i<n_comps; i++) {
      MathType test_comp = ((TupleType)rangeType).getComponent(i);
      if (test_comp.equals(type) ) {
        index = i;
        break;
      }
    }

    if (index != -1) {
      return extract(index);
    } else {
      return null; //?
    }
  }

  /** extract Field from this.component using the name
  * of one of the range componenets
  */

  public Field extract(String name) throws VisADException, RemoteException {
    int index = -1;
    MathType rangeType = ((FunctionType)Type).getRange();
    if (!(rangeType instanceof TupleType)) {
      throw new VisADException("FieldImpl.extract: range must be a TupleType");
    }
    int n_comps = ((TupleType)rangeType).getDimension();
    for (int i=0; i<n_comps; i++) {
      String test_comp = ((TupleType)rangeType).getComponent(i).toString();
      if (test_comp.equals(name) || test_comp.equals("("+name+")")) {
        index = i;
        break;
      }
    }
    if (index != -1) {
      return extract(index);
    } else {
      return null; //?
    }
  }

  /** extract field from this[].component */
  public Field extract(int component)
         throws VisADException, RemoteException
  {
    Set domainSet = getDomainSet();
    int n_samples = domainSet.getLength();
    MathType rangeType = ((FunctionType)Type).getRange();
    MathType domainType = ((FunctionType)Type).getDomain();
    Data rangeData;
    Data new_rangeData;
    Unit[] new_unit;
    Unit unit;
    double[][] values, t_values;
    double value;
    CoordinateSystem coord_sys, coord_in, coord_out;
    MathType m_type;
    RealTupleType rt_type;
    RealType r_type;
    Real real;
    RealTuple r_tuple;
    FieldImpl new_field;
    int ii, jj, kk, t_dim, dim, n_coordsys;

    if (!( rangeType instanceof TupleType) )
    {
      throw new VisADException("extract: range type must be TupleType");
    }
    int n_comps = ((TupleType)rangeType).getDimension();
    if (component == 0 && n_comps == 1) return this;

    if ( (component + 1) > n_comps )
    {
      throw new VisADException("extract: component selection too large");
    }

    MathType new_range = ((TupleType)rangeType).getComponent( component );
    FunctionType new_type = new FunctionType( domainType, new_range);

    if ( new_range instanceof RealType )
    {
      new_unit = new Unit[1];
      new_unit[0] = ((RealType)new_range).getDefaultUnit();
      new_field = new FlatField( new_type, domainSet, null, null, null, new_unit );
      values = new double[1][n_samples];

      for ( ii = 0; ii < n_samples; ii++ )
      {
        real = (Real)
          ((TupleIface) getSample(ii)).getComponent( component );
        value = real.getValue();

        if (new_unit[0] != null) {
          unit = real.getUnit();
          values[0][ii] = new_unit[0].toThis( value, unit );
        }
        else {
          values[0][ii] = value;
        }
      }

      ((FlatField)new_field).setSamples( values, false );
    }
    else if ( new_range instanceof RealTupleType )
    {
      coord_out = ((RealTupleType)new_range).getCoordinateSystem();
      dim = ((RealTupleType)new_range).getDimension();
      Unit[] unit_out = new Unit[dim];
      Unit[] unit_in = new Unit[dim];
      unit_out = ((RealTupleType)new_range).getDefaultUnits();
      new_field = new FlatField( new_type, domainSet, coord_out, null, unit_out );
      values = new double[ dim ][ n_samples ];
      t_values = new double[ dim ][ 1];

      for ( ii = 0; ii < n_samples; ii++ )
      {
        r_tuple = (RealTuple)
          ((TupleIface) getSample(ii)).getComponent( component );
        coord_in = r_tuple.getCoordinateSystem();
        unit_in = r_tuple.getTupleUnits();
        for ( jj = 0; jj < dim; jj++ )
        {
          t_values[jj][0] = ((Real)r_tuple.getComponent(jj)).getValue();
        }

        t_values = CoordinateSystem.transformCoordinates(
                        (RealTupleType)new_range, coord_out, unit_out, null,
                        (RealTupleType)new_range, coord_in, unit_in, null, t_values );

        for ( jj = 0; jj < dim; jj++ )
        {
          values[jj][ii] = t_values[jj][0];
        }
      }

      ((FlatField)new_field).setSamples( values, false );
    }
    else if (( new_range instanceof TupleType) && ( ((TupleType)new_range).getFlat()) )
    {
      new_field = new FlatField( new_type, domainSet );
      dim = ((TupleType)new_range).getDimension();

      t_dim = 0;
      n_coordsys = 0;
      for ( ii = 0; ii < dim; ii++ )
      {
        m_type = ((TupleType)new_range).getComponent(ii);
        if ( m_type instanceof RealType )
        {
          t_dim++;
        }
        else if ( m_type instanceof RealTupleType )
        {
          rt_type = (RealTupleType)m_type;
          t_dim += (rt_type).getDimension();
          if ( rt_type.getCoordinateSystem() != null )
          {
            n_coordsys++;
          }
        }
      }

      values = new double[ t_dim ][ n_samples ];


      t_dim = 0;
      for ( ii = 0; ii < n_samples; ii++ )
      {
        rangeData = getSample(ii);
        m_type = rangeData.getType();

        for ( jj = 0; jj < dim; jj++ )
        {
          if ( m_type instanceof RealType )
          {
            values[t_dim][ii] =
              ((Real)((TupleIface)rangeData).getComponent(jj)).getValue();
            t_dim++;
          }
          else
          {
            r_tuple = (RealTuple) ((TupleIface)rangeData).getComponent(jj);
            for ( kk = 0; kk < ((RealTupleType)m_type).getDimension(); kk++ )
            {
              values[t_dim][ii] = ((Real)r_tuple.getComponent(kk)).getValue();
              t_dim++;
            }
          }
        }
      }
      ((FlatField)new_field).setSamples( values, false );
    }
    else
    {
      new_field = new FieldImpl( new_type, domainSet );

      for ( ii = 0; ii < n_samples; ii++ )
      {
        rangeData = getSample(ii);
        new_rangeData = ((TupleIface)rangeData).getComponent( component );
        new_field.setSample( ii, new_rangeData, false );
      }
    }

    return new_field;
  }

  /**
   * Factors this instance into a (nested) field-of-fields.  The type of the
   * domain of the outer field will be the type specified.  The type of the
   * domains of the inner fields will be the remainder of the original domain
   * after the outer domain is factored out.  Range data is not copied for
   * FieldImpls, but will be for FlatFields.
   * @see #domainFactor(RealType, boolean) for copy information
   *
   * @param factor              The type of the domain for the outer field.
   * @return                    The field-of-fields realization of this
   *                            instance.
   * @throws DomainException    The domain of this instance cannot be factored
   *                            as requested.
   * @throws VisADException     VisAD failure.
   * @throws RemoteException    Java RMI failure.
   */
  public Field domainFactor( RealType factor )
         throws DomainException, VisADException, RemoteException
  {
    return domainFactor(factor, false);
  }

  /**
   * Factors this instance into a (nested) field-of-fields.  The type of the
   * domain of the outer field will be the type specified.  The type of the
   * domains of the inner fields will be the remainder of the original domain
   * after the outer domain is factored out.  Range data is copied if
   *
   * @param factor              The type of the domain for the outer field.
   * @param copy                true to make copies of the data objects
   * @return                    The field-of-fields realization of this
   *                            instance.
   * @throws DomainException    The domain of this instance cannot be factored
   *                            as requested.
   * @throws VisADException     VisAD failure.
   * @throws RemoteException    Java RMI failure.
   */
  public Field domainFactor( RealType factor, boolean copy )
         throws DomainException, VisADException, RemoteException
  {
    int factorIndex;
    Set factor_domain = null;
    int length;
    int[] lengths = null;
    int[] new_lengths = null;
    int[] dim_lengths = null;
    int[] dim_product = null;
    int[] sub_domain = null;
    Set new_domain = null;
    RealTupleType new_domain_type = null;
    FieldImpl factor_field = null;
    FunctionType new_type = null;
    Data range;
    Data[] new_range_data = null;

    int ii, jj, kk, cnt;
    int mm, nn, index;

    RealTupleType domainType = ((FunctionType)Type).getDomain();
    MathType rangeType = ((FunctionType)Type).getRange();
    int domainDim = domainType.getDimension();
    RealType[] r_types = new RealType[domainDim - 1];

    if ((factorIndex = domainType.getIndex((MathType)factor)) < 0 ) {
      throw new DomainException(
        "domainFactor: factor not element of domain");
    }
    cnt = 0;
    for ( ii = 0; ii < domainDim; ii++ ) {
      if ( ii != factorIndex ) {
        r_types[cnt++] = (RealType) domainType.getComponent(ii);
      }
    }
    new_domain_type = new RealTupleType(r_types);
    Set domainSet = getDomainSet();

    if ( domainSet instanceof LinearSet )
    {
      factor_domain = ((LinearSet)domainSet).getLinear1DComponent( factorIndex );
      dim_lengths = ((GriddedSet)domainSet).getLengths();

      Linear1DSet[] L1D_sets = new Linear1DSet[domainDim - 1];
      new_lengths = new int[ domainDim - 1 ];
      sub_domain = new int[domainDim - 1];
      cnt = 0;
      for ( ii = 0; ii < domainDim; ii++ ) {
        if ( ii != factorIndex ) {
          L1D_sets[cnt] = ((LinearSet)domainSet).getLinear1DComponent(ii);
          new_lengths[cnt] = L1D_sets[cnt].LengthX;
          sub_domain[cnt] = ii;
          cnt++;
        }
      }
      new_domain = new LinearNDSet( new_domain_type, L1D_sets );
      new_type = new FunctionType( new_domain_type, rangeType );
    }
    else if ( domainSet instanceof GriddedSet )
    {
      //- check for R^N aligned set?  If aligned then should
      //- be created as a ProductSet for efficiency
      throw new DomainException(
        "domainFactor: DomainSet is GriddedSet, if aligned use ProductSet" );
    }
    else if ( domainSet instanceof ProductSet )
    {
      ProductSet prod_set = (ProductSet)((ProductSet)domainSet).product();
      SampledSet[] sets = prod_set.Sets;
      int n_sets = sets.length;
      SampledSet[] sub_sets = new SampledSet[n_sets - 1];
      SampledSet factor_set = null;
      SampledSet fin_factor_set = null;
      int sub_factor_index = -1;
      int fac_set_idx = -1;
      int fac_set_len = -1;
      int[] sub_set_idx = new int[n_sets - 1];
      int[] sub_set_lengths = new int[n_sets - 1];
      cnt = 0;
      for ( kk = 0; kk < n_sets; kk++ ) {
        SetType s_type = (SetType) sets[kk].getType();
        if ((sub_factor_index = s_type.getDomain().getIndex((MathType)factor)) >= 0 ) {
          factor_set = sets[kk];
          fac_set_idx = kk;
          fac_set_len = factor_set.getLength();
        }
        else {
          sub_sets[cnt] = sets[kk];
          sub_set_idx[cnt] = kk;
          sub_set_lengths[cnt] = sets[kk].getLength();
          cnt++;
        }
      }

      int factor_set_dim = factor_set.getDimension();
      int n_sub_sets = sub_sets.length;

      if ( factor_set_dim == 1 )
      {
        fin_factor_set = factor_set;
        new_lengths = new int[n_sub_sets];
        sub_domain = new int[n_sub_sets];
        dim_lengths = new int[n_sets];
        if ( n_sub_sets > 1 ) {
          new_domain = (Set) new ProductSet(sub_sets);
        }
        else {
          new_domain = (Set) sub_sets[0];
        }
        factor_domain = factor_set;
        System.arraycopy(sub_set_lengths, 0, new_lengths, 0, n_sub_sets);
        System.arraycopy(sub_set_idx, 0, sub_domain, 0, n_sub_sets);
        for ( ii = 0; ii < n_sub_sets; ii++ ) {
          dim_lengths[sub_set_idx[ii]] = sub_set_lengths[ii];
        }
        dim_lengths[fac_set_idx] = fac_set_len;
        new_type =
          new FunctionType(
            ((SetType)new_domain.getType()).getDomain(), rangeType );
      }
      else if (!(factor_set instanceof LinearNDSet ))
      {
        throw new DomainException(
          "cannot factor into "+factor_set.getClass());
      }
      else
      {
        MathType n_type = null;
        new_lengths = new int[n_sub_sets + 1];
        sub_domain = new int[n_sub_sets + 1];
        dim_lengths = new int[n_sets + 1];
        Linear1DSet[] L1D_sets = new Linear1DSet[factor_set_dim - 1];
        cnt = 0;
        for ( ii = 0; ii < factor_set_dim; ii++ ) {
          if ( ii != sub_factor_index ) {
            L1D_sets[cnt] = ((LinearSet)domainSet).getLinear1DComponent(ii);
            cnt++;
          }
          else {
            fin_factor_set = ((LinearSet)domainSet).getLinear1DComponent(ii);
          }
        }
        Set new_set = new LinearNDSet(n_type, L1D_sets);
        cnt = 0;
        System.arraycopy(sub_set_lengths, 0, new_lengths, 0, fac_set_idx);
        System.arraycopy(sub_set_idx, 0, sub_domain, 0, fac_set_idx);
        cnt += fac_set_idx;
        new_lengths[fac_set_idx] = new_set.getLength();
        cnt += 1;
        System.arraycopy(sub_set_lengths, fac_set_idx, new_lengths, cnt,
                         (n_sub_sets - fac_set_idx));
        System.arraycopy(sub_set_idx, fac_set_idx, sub_domain, cnt,
                         (n_sub_sets - fac_set_idx));
        new_type =
          new FunctionType(
            ((SetType)new_set.getType()).getDomain(), rangeType );
        new_domain = new_set;
      }
    }
    else if ( domainSet instanceof UnionSet )
    {
      throw new UnimplementedException(
        "domainFactor: DomainSet is UnionSet" );
    }
    else if ( domainSet instanceof IrregularSet )
    {
      throw new DomainException(
        "domainFactor: DomainSet is IrregularSet, can't factor" );
    }

    length = factor_domain.getLength();
    new_range_data = new Data[ length ];

    dim_product = new int[domainDim];
    for ( kk = 0; kk < domainDim; kk++ ) {
      dim_product[kk] = 1;
      for ( mm = 0; mm < kk; mm++ ) {
        dim_product[kk] *= dim_lengths[mm];
      }
    }

    int s_dims = new_lengths.length;
    int[] indexes = new int[ s_dims ];
    int product = 1;
    for ( ii = 0; ii < s_dims; ii++ ) {
      product *= new_lengths[ii];
    }
    int[] work = new int[product];

    for ( int k = 0; k < product; k++ )
    {
      int k2 = k;
      for ( int j = (s_dims-1); j >= 0; j-- ) {
        int temp = 1;
        for ( int m = 0; m < j; m++  ) {
          temp *= new_lengths[m];
        }
        indexes[j] = k2/temp;
        k2 -= temp*indexes[j];
      }
      for ( int t = 0; t < indexes.length; t++ ) {
        work[k] += dim_product[sub_domain[t]]*indexes[t];
      }
    }

    if ( isFlatField() )
    {
      double[][] old_range_values = getValues(false);
      int tup_dim = old_range_values.length;

      for ( ii = 0; ii < length; ii++ )
      {
        double[][] new_range_values = new double[tup_dim][work.length];
        FlatField new_field = new FlatField( new_type, new_domain );
        for ( jj = 0; jj < work.length; jj++ ) {
          index = 0;
          index = ii*dim_product[factorIndex];
          index += work[jj];
          for ( kk = 0; kk < tup_dim; kk++ ) {
            new_range_values[kk][jj] = old_range_values[kk][ index ];
          }
        }
        ((FlatField)new_field).setSamples( new_range_values, false );
        new_range_data[ii] = new_field;
      }
    }
    else
    {
      for ( ii = 0; ii < length; ii++ )
      {
        FieldImpl new_field = new FieldImpl( new_type, new_domain );
        for ( jj = 0; jj < work.length; jj++ ) {
          index = 0;
          index = ii*dim_product[factorIndex];
          index += work[jj];
          new_range_data[jj] = this.getSample(index);
        }
        new_field.setSamples( new_range_data, false, false );
        new_range_data[ii] = new_field;
      }
    }
    factor_field = new FieldImpl( new FunctionType( factor, new_type),
                                  factor_domain );
    factor_field.setSamples( new_range_data, copy, false );
    return factor_field;
  }

  /** 
   * Combine domains of two outermost nested Fields into a single
   * domain and Field.  If the domains each have <code>
   * CoordinateSystem</code>-s the new domain will have
   * a <code>CartesianProductCoordinateSystem</code> of 
   * Set-s CoordinateSystems
   * 
   * @throws VisADException  unable to collapse domains
   * @throws RemoteException  unable to collapse domains of remote data
   */
  public Field domainMultiply()
         throws VisADException, RemoteException
  {
    return domainMultiply(1, null);
  }

  /** 
   * Combine domains of two outermost nested Fields into a single
   * domain and Field.  The supplied <code>resultCS</code> would be used
   * for the new domain
   * @param resultCS  CoordinateSystem to use for the new domain set
   * @throws VisADException  unable to collapse domains
   * @throws RemoteException  unable to collapse domains of remote data
   */
  public Field domainMultiply(CoordinateSystem resultCS)
         throws VisADException, RemoteException
  {
    return domainMultiply(1, resultCS);
  }

  /** 
   * Combine domains of <code>collapse_depth</code> if possible.
   * @param collapse_depth  depth to collapse to
   * @throws VisADException  unable to collapse domains
   * @throws RemoteException  unable to collapse domains of remote data
   */
  public Field domainMultiply(int collapse_depth)
         throws VisADException, RemoteException
  {
     return domainMultiply(collapse_depth, null);
  }

  /** 
   * Combine domains of <code>collapse_depth</code> if possible.
   * Use <code>resultCS</code> as the CoordinateSystem for the new domain.
   * @param collapse_depth  depth to collapse to
   * @param resultCS  CoordinateSystem to use for the new domain set
   * @throws VisADException  unable to collapse domains
   * @throws RemoteException  unable to collapse domains of remote data
   */
  public Field domainMultiply(int collapse_depth, CoordinateSystem resultCS)
         throws VisADException, RemoteException
  {
    class Helper
    {
      int cnt = 0;
      int n_samples;
      int depth;
      int depth_max;
      boolean flat;
      MathType range_type;
      MathType new_range_type;
      SampledSet[] last_set;
      SampledSet[] fac_sets;
      Data[] collapse_array;

      public Helper(Data data, int col_depth)
             throws VisADException, RemoteException
      {
        MathType m_type = data.getType();

        depth = 0;
        flat = false;
        depth_max = checkType( m_type );

        if ( depth_max == 0 )
        {
          throw new FieldException("MathType "+m_type.prettyString());
        }
        else if ( depth_max >= col_depth )
        {
          depth_max = col_depth;
        }


        flat = false;
        for (int kk = 0; kk < depth_max; kk++) {
          if (m_type instanceof FunctionType) {
            m_type = ((FunctionType)m_type).getRange();
          }
        }
        if (m_type instanceof FunctionType)  {
          flat = ((FunctionType)m_type).getFlat();
          new_range_type = ((FunctionType)m_type).getRange();
        }
         

        last_set = new SampledSet[depth_max + 1];
        depth = 0;
        if ( !(setsEqual((Field)data)) )
        {
          throw new FieldException("sets not equal");
        }

        int length = 1;
        if (flat) {
          for ( int kk = 0; kk < depth_max; kk++ ) {
            length *= last_set[kk].getLength();
          }
        }
        else {
          for ( int kk = 0; kk < depth_max+1; kk++ ) {
            length *= last_set[kk].getLength();
          }
        }
 
        collapse_array = new Data[length];

        depth = 0;
        collapse( data );
      }

      public SampledSet[] getSets() {
        int length = last_set.length;
        fac_sets = new SampledSet[length];
        for ( int ii = 0; ii < length; ii++ ) {
          fac_sets[(length-1) - ii] = last_set[ii];
        }
        return fac_sets;
      }

      public Data[] getRangeArray() {
        return collapse_array;
      }

      public MathType getRangeType() {
        return new_range_type;
      }

      public int checkType(MathType m_type)  //-- analyze Data hierarchy
             throws VisADException, RemoteException
      {
        if ( m_type instanceof FunctionType )
        {
          if (((FunctionType)m_type).getFlat())
          {
            flat = true;
            new_range_type = ((FunctionType)m_type).getRange();
            return depth;
          }
          else
          {
            range_type = ((FunctionType)m_type).getRange();
            depth++;
            return checkType(range_type);
          }
        }
        else
        {
          new_range_type = m_type;
          return depth;
        }
      }

      public void collapse( Data data )
             throws VisADException, RemoteException
      {
        if ( depth == depth_max )
        {
          if (flat)
          {
            collapse_array[cnt++] = ((FieldImpl)data);
          }
          else
          {
            for ( int ii = 0; ii < ((FieldImpl)data).getLength(); ii++) {
              collapse_array[cnt++] = ((FieldImpl)data).getSample(ii);
            }
          }
        }
        else
        {
          int n_samples = (((Field)data).getDomainSet()).getLength();
          for ( int ii = 0; ii < n_samples; ii++ ) {
            depth++;
            collapse(((FieldImpl)data).getSample(ii));
            depth--;
          }
        }
      }

      public boolean setsEqual( Field field )
             throws VisADException, RemoteException
      {
        Set domainSet = field.getDomainSet();
        int n_samples = domainSet.getLength();

        if ( depth == 0 ) {
          last_set[depth] = (SampledSet)domainSet;
        }

        depth++;

        if (last_set[depth] == null ) {
          last_set[depth] = (SampledSet)((Field)(field.getSample(0))).getDomainSet();
        }

        for ( int ii = 0; ii < n_samples; ii++ )
        {
          Field range_data = (Field) field.getSample(ii);
          Set range_set = range_data.getDomainSet();

          if ( !(last_set[depth].equals(range_set)) )
          {
            return false;
          }
          if ( !(depth == (depth_max)) )
          {
            if ( !(setsEqual(range_data)) )
            {
              return false;
            }
            depth--;
          }
        }

        return true;
      }
    } //- end helper class

    int cnt;
    SampledSet new_set = null;

    int n_irregular = 0;
    int n_linear = 0;
    int new_domainDim = 0;
    int new_manifoldDim = 0;

    Helper helper = new Helper(this, collapse_depth);
    SampledSet[] fac_sets = helper.getSets();
    int n_sets = fac_sets.length;
    Data[] new_range = helper.getRangeArray();
    MathType new_range_type = helper.getRangeType();

    SetType[] set_types = new SetType[n_sets];
    int new_length = 1;
    for ( int kk = 0; kk < n_sets; kk++ )
    {
      new_length *= fac_sets[kk].getLength();
      new_domainDim += fac_sets[kk].getDimension();
      new_manifoldDim += fac_sets[kk].getManifoldDimension();
      set_types[kk] = (SetType) fac_sets[kk].getType();

      if ( fac_sets[kk] instanceof IrregularSet ) {
        n_irregular++;
      }
      else if ( fac_sets[kk] instanceof LinearSet ) {
        n_linear++;
      }
    }
    RealType[] r_types = new RealType[new_domainDim];

    cnt = 0;
    boolean any_are_null = false;
    CoordinateSystem[] coord_sys = new CoordinateSystem[n_sets];

    for ( int kk = 0; kk < n_sets; kk++ ) {
      RealTupleType domain = set_types[kk].getDomain();
      //- TDR: May, 2003
      CoordinateSystem cs = domain.getCoordinateSystem();
      if (cs == null) {
        any_are_null = true;
      }
      coord_sys[kk] = cs;

      for ( int j = 0; j < domain.getDimension(); j++ ) {
        r_types[cnt++] = (RealType) domain.getComponent(j);
      }
    }
    //- TDR: May, 2003
    CoordinateSystem new_cs = resultCS;
    if (!any_are_null && new_cs == null) {
      new_cs = coord_sys[0];
      for ( int kk = 0; kk < (coord_sys.length - 1); kk++ ) {
        new_cs = new CartesianProductCoordinateSystem(new_cs, coord_sys[kk+1]);
      }
    }

    RealTupleType new_domain_type = new RealTupleType( r_types, new_cs, null );
    FunctionType new_function_type = new FunctionType( new_domain_type,
                                                       new_range_type );

    if ( n_irregular > 0 ) //- if any irregular sets --> ProductSet
    {
      new_set = new ProductSet(fac_sets);
    }
    else if ( n_linear == n_sets )  //- all Linear sets
    {
      Linear1DSet[] L1D_sets = new Linear1DSet[new_domainDim];
      cnt = 0;
      for ( int kk = 0; kk < n_sets; kk++ ) {
        for ( int jj = 0; jj < fac_sets[kk].getDimension(); jj++ ) {
          L1D_sets[cnt++] = ((LinearSet)fac_sets[kk]).getLinear1DComponent(jj);
        }
      }
      new_set = new LinearNDSet(new_domain_type, L1D_sets);
    }
    else  //- some Linear, some Gridded --> GriddedNDSet
    {
      Set set;
      int sub_manifoldDim;
      int domainDim;
      int[] lengths;
      float[][] samples;
      float[][] new_samples = new float[new_domainDim][new_length];
      float[][] sub_samples = new float[new_domainDim][];
      Unit[] newUnits = new Unit[new_domainDim];
      ErrorEstimate[] newErrors = new ErrorEstimate[new_domainDim];

      int[][] manifoldLengths = new int[new_domainDim][];
      int[][] manifoldIndexes = new int[new_domainDim][];
      int[] new_lengths = new int[new_manifoldDim];
      cnt = 0;
      int cnt_m = 0;
      int manifoldDimension = 0;
      for ( int kk = 0; kk < n_sets; kk++ )
      {
        set = fac_sets[kk];
        samples = ((SampledSet)set).getSamples();
        domainDim = set.getDimension();
        sub_manifoldDim = set.getManifoldDimension();
        lengths = ((GriddedSet)set).getLengths();
        Unit[] units = set.getSetUnits();
        ErrorEstimate[] errors = set.getSetErrors();
        for ( int ii = 0; ii < domainDim; ii++ ) {
          sub_samples[cnt] = samples[ii];
          manifoldLengths[cnt] = lengths;
          manifoldIndexes[cnt] = new int[sub_manifoldDim];
          for ( int jj = 0; jj < sub_manifoldDim; jj++ ) {
            manifoldIndexes[cnt][jj] = jj + manifoldDimension;
          }
          newUnits[cnt] = units[ii];
          newErrors[cnt] = errors[ii];
          cnt++;
        }
        for ( int ii = 0; ii < sub_manifoldDim; ii++ ) {
          new_lengths[cnt_m++] = lengths[ii];
        }
        manifoldDimension += sub_manifoldDim;
      }

      int[] indexes = new int[new_manifoldDim];
      for ( int k = 0; k < new_length; k++ )
      {
        int k2 = k;
        for ( int j = (new_manifoldDim - 1); j >= 0; j-- ) {
          int temp = 1;
          for ( int m = 0; m < j; m++  ) {
            temp *= new_lengths[m];
          }
          indexes[j] = k2/temp;
          k2 -= temp*indexes[j];
        }

        for ( int ii = 0; ii < new_domainDim; ii++ )
        {
          int sub_index = 0;
          for ( int mm = (manifoldIndexes[ii].length - 1); mm >= 0; mm-- ) {
            int product = 1;
            for ( int nn = 0; nn < mm; nn++ ) {
              product *= manifoldLengths[ii][nn];
            }
            product *= indexes[manifoldIndexes[ii][mm]];
            sub_index += product;
          }
          new_samples[ii][k] = sub_samples[ii][sub_index];
        }
      }

      // DRM 02-Feb-2003
      //new_set = new GriddedSet(new_domain_type, new_samples, new_lengths );
      //new_set = GriddedSet.create(new_domain_type, new_samples, new_lengths );
      new_set = GriddedSet.create(new_domain_type, new_samples, new_lengths,
          null,  // CS null, because already defined in new_domain_type
          newUnits, newErrors);
    }
    Field new_field;
    if ( helper.flat )
    {
      new_field = new FlatField( new_function_type, new_set );
      int tup_dim =
        (new_function_type.getFlatRange()).getDimension();
      double[][] new_values =
        new double[tup_dim][new_length];

      cnt = 0;
      double[][] sub_range;
      for ( int ii = 0; ii < new_range.length; ii++ ) {
        sub_range = ((FieldImpl) new_range[ii]).getValues(false);
        int len = sub_range[0].length;
        for ( int jj = 0; jj < tup_dim; jj++ ) {
          System.arraycopy(sub_range[jj], 0, new_values[jj], cnt, len);
        }
        cnt += len;
      }
      // still a problem if a factor Set is doubles (e.g., Time)
      // WLH 15 Jan 2000
      ((FlatField) new_field).setSamples(new_values, false);
    }
    else
    {
      new_field = new FieldImpl( new_function_type, new_set );
      for ( int ii = 0; ii < new_length; ii++ ){
        new_field.setSample(ii, new_range[ii]);
      }
    }

    return new_field;
  }

  public Data derivative( RealTuple location, RealType[] d_partial_s,
                          MathType[] derivType_s, int error_mode )
         throws VisADException, RemoteException
  {
    int ii, jj, kk, dd, rr, tt, pp, ss;
    Set domainSet = this.getDomainSet();
    int domainDim = domainSet.getDimension();
    int manifoldDimension = domainSet.getManifoldDimension();
    int n_samples = domainSet.getLength();
    CoordinateSystem d_coordsys = this.getDomainCoordinateSystem();
    RealTupleType d_reference = (d_coordsys == null) ? null : d_coordsys.getReference();
    MathType m_type = null;
    MathType[] m_types = null;
    RealType r_type = null;
    RealType[] r_types = null;
    TupleType t_type = null;
    boolean thisDomainFlag = true;

    if ( manifoldDimension != domainDim )
    {
      throw new SetException("derivative: manifoldDimension must equal "+
                             "domain dimension" );
    }
    error_mode = Data.NO_ERRORS;
    int sampling_mode = Data.WEIGHTED_AVERAGE;

    if ( location != null )
    {
      thisDomainFlag = false;
    }

    RealTupleType domainType = ((FunctionType)Type).getDomain();
    RealType[] r_comps = domainType.getRealComponents();
    RealType[] r_compsRef = (d_reference == null) ? null : d_reference.getRealComponents();

    MathType RangeType = ((FunctionType)Type).getRange();

    int n_partials;  // number of partial derivatives to compute -*

  //- get all components for this function's domain -*
    if ( d_partial_s == null )
    {
      n_partials = domainDim;
      d_partial_s = r_comps;
    }
    else
    {
      n_partials = d_partial_s.length;
      if ( n_partials > domainDim ) {
        throw new VisADException("derivative: too many d_partial components");
      }
    }

    int[] u_index = new int[n_partials];
    double[][] u_vectors = new double[n_partials][domainDim];

  //- verify that input RealType-s match the Function's domain -*
  //- create unit vectors for the d_partial RealTypes -*
    int found = 0;
    int foundRef = 0;
    for ( ii = 0; ii < n_partials; ii++ )
    {
      for ( jj = 0; jj < domainDim; jj++ )
      {
        u_vectors[ii][jj] = 0d;
        if ( r_comps[jj].equals(d_partial_s[ii]) )
        {
          u_index[ii] = jj;
          u_vectors[ii][jj] = 1d;
          found++;
        }
        else if ( d_reference != null )
        {
          if ( r_compsRef[jj].equals(d_partial_s[ii]) )
          {
            u_index[ii] = jj;
            u_vectors[jj][ii] = 1d;
            foundRef++;
          }
        }
      }
    }

    boolean transform;  //- flag indicating coordinate transform is required  --*

    if ( found == 0 )
    {
      if ( foundRef == 0 )
      {
         throw new VisADException("derivative: d_partial_s not in domain or reference");
      }
      else if ( 0 < foundRef && foundRef < n_partials )
      {
        throw new VisADException("derivative: d_partial_s must ALL be in function's "+
                                             "domain or ALL in domain's reference");
      }
      else
      {
        transform = true;
      }
    }
    else if ( 0 < found && found < n_partials )
    {
      throw new VisADException("derivative: d_partial_s must ALL be in function's "+
                                           "domain or ALL in domain's reference");
    }
    else
    {
      transform = false;
    }

    String[][] derivNames = null;
    Unit[] D_units;
    MathType[] new_range = new MathType[ n_partials ];
    MathType[] new_types = new MathType[ n_partials ];

    if ( !transform ) {
      D_units = domainSet.getSetUnits();
    }
    else {
      D_units = d_reference.getDefaultUnits();
    }

    if ( derivType_s == null )
    {
      for ( ii = 0; ii < n_partials; ii++ )
      {
        MathType M_type = Type.cloneDerivative( d_partial_s[ii] );
        if ( thisDomainFlag ) {
          new_types[ii] = M_type;
        }
        else {
          new_types[ii] = ((FunctionType)M_type).getRange();
        }
      }
      derivType_s = new_types;
    }
    else //- check supplied derivType-s for compatibility  -*
    {
      if ( derivType_s.length != n_partials ) {
        throw new VisADException("derivative: must be a single MathType "+
                                 "for each domain RealType");
      }
      for ( ii = 0; ii < n_partials; ii++ )
      {
        if ( thisDomainFlag ) {
          if ( !Type.equalsExceptName(derivType_s[ii]) ) {
            throw new TypeException("derivative: incompatible with function range");
          }
        }
        else {
          if ( !((((FunctionType)Type).getRange()).equalsExceptName(derivType_s[ii])) ) {
            throw new TypeException("derivative: incompatible with function range");
          }
        }
      }
    }

  //- compute derivative-s, return FlatField or Tuple of FlatFields, or Data --*

    int[][] neighbors = null;
    int n_points;
    int n_index;
    int m_index;
    int index;
    float distance;
    float step;
    float f_sum;
    double d_sum;
    Data[] p_derivatives = new Data[ n_partials ];
    ErrorEstimate[] domainErrors = domainSet.getSetErrors();
    Real deltaDomain;
    FieldImpl[] new_fields = new FieldImpl[ n_partials ];
    Data data_0;
    Data data_1;
    Data rangeDiff;
    Data newRange;
    Data[] rangeValues = null;

    for ( pp = 0; pp < n_partials; pp++ ) {
      new_fields[pp] = new FieldImpl( (FunctionType)derivType_s[pp], domainSet );
    }

    if ( isMissing() )   //- Bypass computations and return missing field -*
    {

  //- Handle LinearSet case separately for efficiency   -*
    if(( domainSet instanceof LinearSet )&&( thisDomainFlag ))
    {
      //- each partial derivative   -*
      for ( kk = 0; kk < n_partials; kk++ )
      {
        RangeType = ((FunctionType)derivType_s[kk]).getRange();
        //- get manifoldDimension index for this real axis ( LinearSet only )  -*
        m_index = u_index[kk];

        //- get neigbors and separation along this axis   -*
        neighbors = domainSet.getNeighbors( m_index );
        step = (float) (((LinearSet)domainSet).getLinear1DComponent(kk)).getStep();

        //- compute derivative for each sample and each range component   -*
        for ( ii = 0; ii < n_samples; ii++ )
        {
          if ( neighbors[ii][0] == -1) {
            distance = step;
            n_index = neighbors[ii][1];
            index = ii;
          }
          else if ( neighbors[ii][1] == -1 ) {
            distance = step;
            n_index = ii;
            index = neighbors[ii][0];
          }
          else {
            distance = 2.f*step;
            n_index = neighbors[ii][1];
            index = neighbors[ii][0];
          }

          data_1 = getSample(n_index);
          data_0 = getSample(index);
          deltaDomain = new Real( d_partial_s[kk], distance, D_units[m_index] );

          rangeDiff = data_1.binary( data_0, Data.SUBTRACT, sampling_mode, error_mode);
          newRange = rangeDiff.binary( deltaDomain, Data.DIVIDE, RangeType,
                                       sampling_mode, error_mode );

          new_fields[kk].setSample( ii, newRange );
        }
      }
    }
    else  //- GriddedSet, IrregularSet    --*
    {
      float dotproduct;
      float inv_dotproduct;
      float[][] weights = null;
      float sum_weights;
      float[][] Samples;

      //- compute derivative at this Set's sample locations  --*
      if ( thisDomainFlag )
      {
        neighbors = new int[n_samples][];
        weights = new float[n_samples][];
        domainSet.getNeighbors( neighbors, weights );
        if ( transform )
        {
          Samples = domainSet.getSamples(true);

          Samples =
          CoordinateSystem.transformCoordinates( d_reference, null, null, null,
                           domainType, d_coordsys, null, null, Samples );
        }
        else
        {
          Samples = domainSet.getSamples(false);
        }
      }
      //- compute derivative at selected ( probably interpolated locations )  --*
      else
      {
        Data[] new_rangeValues;
        int[][] new_neighbors;
        n_samples = 1;
        Field field;
        float[][] new_Samples;
        float[][] evalSamples;
        float[][] org_Samples = domainSet.getSamples(false);

        field = resample( new SingletonSet(location, null, null, null ),
                          Data.WEIGHTED_AVERAGE, error_mode );

        evalSamples = (field.getDomainSet()).getSamples(false);
        neighbors = new int[n_samples][];
        weights = new float[n_samples][];

        ((SimpleSet)domainSet).valueToInterp( evalSamples, neighbors, weights );

        n_points = neighbors[0].length;
        new_neighbors = new int[n_samples][ n_points ];

        new_rangeValues = new Data[ n_points + 1 ];
        new_Samples = new float[ domainDim ][ n_points + 1 ];
        for ( ii = 0; ii < domainDim; ii++ )
        {
          new_Samples[ii][0] = evalSamples[ii][0];
        }
        new_rangeValues[0] = field.getSample(0);
        for ( kk = 0; kk < n_points; kk++ )
        {
          new_neighbors[0][kk] = kk + 1;
          new_rangeValues[kk+1] = getSample( neighbors[0][kk] );
          for ( ii = 0; ii < domainDim; ii++ )
          {
            new_Samples[ii][kk+1] = org_Samples[ii][ neighbors[0][kk] ];
          }
        }

        neighbors = new_neighbors;
        rangeValues = new_rangeValues;
        Samples = new_Samples;
        if ( transform )
        {
          Samples =
          CoordinateSystem.transformCoordinates( d_reference, null, null, null,
                           domainType, d_coordsys, null, null, Samples );
        }
      }

      //- compute derivatives for each sample   --*
      for ( ii = 0; ii < n_samples; ii++ )
      {
        n_points = neighbors[ii].length;
        Data[] rangeDiff_s = new Data[ n_points ];
        Data p_derivative = null;
        double[][] uvecPoint = new double[ n_points ][ domainDim ];
        data_0 = (thisDomainFlag) ? getSample( ii ) : rangeValues[ii];
        float factor;

        //- neighbors loop   -*
        for ( kk = 0; kk < n_points; kk++ )
        {
          for ( dd = 0; dd < domainDim; dd++ ) {
            uvecPoint[kk][dd] = Samples[dd][ neighbors[ii][kk] ] - Samples[dd][ii];
          }

            data_1 = (thisDomainFlag) ? getSample( neighbors[ii][kk] ) :
                                        rangeValues[ neighbors[ii][kk] ];

            rangeDiff_s[kk] = data_1.binary( data_0, Data.SUBTRACT, sampling_mode,
                                             error_mode );
        }

        //- Interpolate for each partial derivative  -*
        boolean first = true;
        for ( pp = 0; pp < n_partials; pp++ )
        {
          m_index = u_index[pp];
          RangeType = ((FunctionType)derivType_s[pp]).getRange();
          sum_weights = 0f;
          for ( kk = 0; kk < n_points; kk++ )
          {
            dotproduct = 0;

            for ( dd = 0; dd < domainDim; dd++ )
            {
              dotproduct += uvecPoint[kk][dd]*u_vectors[pp][dd];
            }

            inv_dotproduct = 1f/dotproduct;

            if ( ! Float.isInfinite(inv_dotproduct) )
            {
              sum_weights += weights[ii][kk];
              factor = inv_dotproduct*weights[ii][kk];
              rangeDiff_s[kk] = rangeDiff_s[kk].binary( new Real( factor), Data.MULTIPLY,
                                                        sampling_mode, error_mode );
              if ( first ) {
                p_derivative = rangeDiff_s[kk];
                first = false;
              }
              else {
                p_derivative = p_derivative.binary( rangeDiff_s[kk], Data.ADD, sampling_mode,
                                                    error_mode );
              }
            }
          }
          Real real = new Real( d_partial_s[pp], sum_weights, D_units[ m_index] );
          p_derivative = p_derivative.binary( real, Data.DIVIDE,
                                              RangeType, sampling_mode, error_mode);

          new_fields[pp].setSample( ii, p_derivative );
        }
      }
    }
    } //- missing range branch --*

    if ( n_partials == 1 )
    {
      return new_fields[0];
    }
    else
    {
      return new Tuple( new_fields );
    }
  }

  public Data derivative( int error_mode )
         throws VisADException, RemoteException
  {
    MathType[] derivType_s = null;
    RealType[] d_partial_s = null;
    return this.derivative( null, d_partial_s, derivType_s, error_mode );
  }

  public Data derivative( MathType[] derivType_s, int error_mode )
         throws VisADException, RemoteException
  {
    return this.derivative( null, null, derivType_s, error_mode );
  }

  public Function derivative( RealType d_partial, int error_mode )
         throws VisADException, RemoteException
  {
    MathType[] derivType_s = null;
    RealType[] d_partial_s = new RealType[1];
    d_partial_s[0] = d_partial;

    return (Function) this.derivative( null, d_partial_s, derivType_s, error_mode );
  }

  public Function derivative( RealType d_partial, MathType derivType, int error_mode)
         throws VisADException, RemoteException
  {
    MathType[] derivType_s = new MathType[1];
    RealType[] d_partial_s = new RealType[1];
    derivType_s[0] = derivType;
    d_partial_s[0] = d_partial;

    return (Function) this.derivative( null, d_partial_s, derivType_s, error_mode );
  }

  /** 
   * Resample range values of this Field to domain samples in set either
   * byt nearest neighbor or multi-linear interpolation.
   * NOTE: this code is very similar to resample in FlatField.java 
   * @param set            finite sampling values for the function.
   * @param sampling_mode  type of interpolation to perform (e.g., 
   *                       Data.WEIGHTED_AVERAGE, Data.NEAREST_NEIGHBOR)
   * @param error_mode     type of error estimation to perform (e.g., 
   *                       Data.INDEPENDENT, Data.DEPENDENT, Data.NO_ERRORS)
   * @return Data object corresponding to the function value at that domain,
   *         using the sampling_mode and error_modes specified.  NOTE: may
   *         return this (i.e., not a copy).
   * @throws  VisADException   unable to resample function
   * @throws  RemoteException  Java RMI exception
   */
  public Field resample(Set set, int sampling_mode, int error_mode)
         throws VisADException, RemoteException {

    /* NB: resampling is done in this method for a float domain.  If
     * you make changes to this method, make the corresponding changes
     * in resampleDouble if necessary.
     */
    Set domainSet = getDomainSet();

    if (domainSet.equals(set)) {
      // nothing to do
      return this;
    }

    MathType range_type = ((FunctionType) Type).getRange();
    RealTupleType domain_type = ((SetType) set.getType()).getDomain();
    FunctionType func_type = new FunctionType(domain_type, range_type);
    Field field = new FieldImpl(func_type, set);
    // Field field = new FieldImpl((FunctionType) Type, set);
    if (isMissing()) return field;

    int dim = domainSet.getDimension();
    if (dim != set.getDimension()) {
      throw new SetException("FieldImpl.resample: bad Set Dimension");
    }

    // Resampling in this method is done if floats so if we have
    // a double domain, call resampleDouble. 
    if (domainSet instanceof GriddedDoubleSet) {
      return resampleDouble(set, sampling_mode, error_mode);
    }

    CoordinateSystem coord_sys = set.getCoordinateSystem();
    Unit[] units = set.getSetUnits();
    ErrorEstimate[] errors =
      (error_mode == NO_ERRORS) ? new ErrorEstimate[dim] : set.getSetErrors();

    // create an array containing all indices of 'this'
    int length = set.getLength();
    int[] wedge = set.getWedge();

    // array of Data objects to receive resampled Range objects
    Data[] range = new Data[length];

    // get values from wedge and possibly transform coordinates
    float[][] vals = set.indexToValue(wedge);
    // holder for sampling errors of transformed set - these are
    // only useful to help estmate range errors due to resampling
    ErrorEstimate[] errors_out = new ErrorEstimate[dim];
    float[][] oldvals = vals;
    try {  // this is only to throw a more meaningful message
      vals = CoordinateSystem.transformCoordinates(
                      ((FunctionType) Type).getDomain(), 
                      getDomainCoordinateSystem(),
                      getDomainUnits(), errors_out,
                      ((SetType) set.getType()).getDomain(), coord_sys,
                      units, errors, vals);
    } catch (UnitException ue) {
        throw new VisADException("Sampling set is not compatible with domain");
    }

    boolean coord_transform = !(vals == oldvals);

    // check whether we need to do sampling error calculations
    boolean sampling_errors = (error_mode != NO_ERRORS);
    if (sampling_errors) {
      for (int i=0; i<dim; i++) {
        if (errors_out[i] == null) sampling_errors = false;
      }
    }
    Data[] sampling_partials = new Data[dim];
    float[][] error_values;
    double[] means = new double[dim];

    Data[]Range = getRange ();
    if (sampling_mode == WEIGHTED_AVERAGE && domainSet instanceof SimpleSet) {
      // resample by interpolation
      int[][] indices = new int[length][];
      float[][] coefs = new float[length][];
      ((SimpleSet) domainSet).valueToInterp(vals, indices, coefs);
      for (int i=0; i<length; i++) {
        int len;
        len = (indices[i] == null) ? 0 : indices[i].length;
        if (len > 0) {
          Data r = null;
          // WLH
          for (int k=0; k<len; k++) {
            Data RangeIK;
            synchronized (RangeLock) {
              RangeIK = Range[indices[i][k]];
            }
            if (RangeIK != null) {
              r = (r == null) ? RangeIK.multiply(new Real(coefs[i][k])) :
                                r.add(RangeIK.multiply(new Real(coefs[i][k])));
            }
            else {
              r = null;
              break;
            }
          }

          // SRE 2002-02-13
          if (r != null) {
            r = r.changeMathType(((FunctionType)Type).getRange());
          }

          range[wedge[i]] = r;
        }
        else {
          // set range[wedge[i]] to a missing Data object
          range[wedge[i]] = ((FunctionType) Type).getRange().missingData();
        }

        if (sampling_errors && !range[wedge[i]].isMissing()) {
          for (int j=0; j<dim; j++) means[j] = vals[j][i];
          error_values = Set.doubleToFloat(
                           ErrorEstimate.init_error_values(errors_out, means) );
          int[][] error_indices = new int[2 * dim][];
          float[][] error_coefs = new float[2 * dim][];
          coefs = new float[2 * dim][];
          ((SimpleSet) domainSet).valueToInterp(error_values, error_indices,
                                                error_coefs);

          for (int j=0; j<dim; j++) {
            Data a = null;
            Data b = null;
            len = error_indices[2*j].length;
            if (len > 0) {
              for (int k=0; k<len; k++) {
                Data RangeIK;
                synchronized (RangeLock) {
                  RangeIK = Range[error_indices[2*j][k]];
                }
                if (RangeIK != null) {
                  a = (a == null) ?
                      RangeIK.multiply(new Real(error_coefs[2*j][k])) :
                      a.add(RangeIK.multiply(new Real(error_coefs[2*j][k])));
                }
                else {
                  a = null;
                  break;
                }
              }
            }
            len = error_indices[2*j+1].length;
            if (len > 0) {
              for (int k=0; k<len; k++) {
                Data RangeIK;
                synchronized (RangeLock) {
                  RangeIK = Range[error_indices[2*j+1][k]];
                }
                if (RangeIK != null) {
                  b = (b == null) ?
                      RangeIK.multiply(new Real(error_coefs[2*j+1][k])) :
                      b.add(RangeIK.multiply(new Real(error_coefs[2*j+1][k])));
                }
                else {
                  b = null;
                  break;
                }
              }
            }
            if (a == null || b == null) {
              sampling_partials[j] = null;
            }
            else {
              sampling_partials[j] = b.subtract(a).abs();
            }
          }

          Data error = null;
          if (error_mode == Data.INDEPENDENT) {
            for (int j=0; j<dim; j++) {
              Data e = sampling_partials[j].multiply(sampling_partials[j]);
              error = (error == null) ? e : error.add(e);
            }
            error = error.sqrt();
          }
          else { // error_mode == Data.DEPENDENT
            for (int j=0; j<dim; j++) {
              Data e = sampling_partials[j];
              error = (error == null) ? e : error.add(e);
            }
          }
          range[wedge[i]] =
            range[wedge[i]].adjustSamplingError(error, error_mode);
        } // end if (sampling_errors && !range[wedge[i]].isMissing())
      } // end for (int i=0; i<length; i++)
    }
    else { // Mode is NEAREST_NEIGHBOR or set is not GriddedSet
      // simple resampling
      int[] indices = domainSet.valueToIndex(vals);
      for (int i=0; i<length; i++) {
        synchronized (RangeLock) {
          range[wedge[i]] = (indices[i] >= 0 && Range[indices[i]] != null) ?
                            Range[indices[i]] :
                            ((FunctionType) Type).getRange().missingData();
        }

        if (sampling_errors && !range[wedge[i]].isMissing()) {
          for (int j=0; j<dim; j++) means[j] = vals[j][i];
          error_values = Set.doubleToFloat(
                           ErrorEstimate.init_error_values(errors_out, means) );
          int[] error_indices = domainSet.valueToIndex(error_values);
          for (int j=0; j<dim; j++) {
            synchronized (RangeLock) {
              if (error_indices[2*j] < 0 || Range[error_indices[2*j]] == null ||
                  error_indices[2*j+1] < 0 || Range[error_indices[2*j+1]] == null) {
                sampling_partials[j] = null;
              }
              else {
                sampling_partials[j] = Range[error_indices[2*j+1]].
                                         subtract(Range[error_indices[2*j]]).abs();
              }
            }
          }

          Data error = null;
          if (error_mode == Data.INDEPENDENT) {
            for (int j=0; j<dim; j++) {
              Data e = sampling_partials[j].multiply(sampling_partials[j]);
              error = (error == null) ? e : error.add(e);
            }
            error = error.sqrt();
          }
          else { // error_mode == Data.DEPENDENT
            for (int j=0; j<dim; j++) {
              Data e = sampling_partials[j];
              error = (error == null) ? e : error.add(e);
            }
          }
          range[wedge[i]] =
            range[wedge[i]].adjustSamplingError(error, error_mode);
        } // end if (sampling_errors && !range[wedge[i]].isMissing())
      } // end for (int i=0; i<length; i++)
    }

    if (coord_transform) {
      // domain coordinates were transformed, so make corresponding
      // vector transform to any RealVectorType-s in range
      MathType RangeType = ((FunctionType) Type).getRange();
      if (RangeType instanceof RealVectorType) {
        int n = vals.length;
        float[][] inloc = new float[n][1];
        float[][] outloc = new float[n][1];
        for (int i=0; i<length; i++) {
          for (int k=0; k<n; k++) inloc[k][0] = oldvals[k][i];
          for (int k=0; k<n; k++) outloc[k][0] = vals[k][i];
          range[i] = ((RealVectorType) RangeType).transformVectors(
                      ((FunctionType) Type).getDomain(),
                      getDomainCoordinateSystem(), getDomainUnits(), errors_out,
                      ((SetType) set.getType()).getDomain(),
                      coord_sys, units,
                      ((RealTuple) range[i]).getCoordinateSystem(),
                      inloc, outloc, (RealTuple) range[i]);
        }
      }
      else if (RangeType instanceof TupleType &&
               !(RangeType instanceof RealTupleType)) {
        int m = ((TupleType) RangeType).getDimension();
        boolean any_vector = false;
        for (int j=0; j<m; j++) {
          if (((TupleType) RangeType).getComponent(j) instanceof RealVectorType) {
            any_vector = true;
          }
        }
        if (any_vector) {
          int n = vals.length;
          float[][] inloc = new float[n][1];
          float[][] outloc = new float[n][1];
          Data[] datums = new Data[m];
          for (int i=0; i<length; i++) {
            for (int k=0; k<n; k++) inloc[k][0] = oldvals[k][i];
            for (int k=0; k<n; k++) outloc[k][0] = vals[k][i];
            for (int j=0; j<m; j++) {
              MathType comp_type = ((TupleType) RangeType).getComponent(j);
              if (comp_type instanceof RealVectorType) {
                RealTuple component =
                  (RealTuple) ((TupleIface) range[i]).getComponent(j);
                datums[j] = ((RealVectorType) comp_type).transformVectors(
                            ((FunctionType) Type).getDomain(),
                            getDomainCoordinateSystem(), getDomainUnits(), errors_out,
                            ((SetType) set.getType()).getDomain(),
                            coord_sys, units, component.getCoordinateSystem(),
                            inloc, outloc, component);
              }
              else {
                datums[j] = ((TupleIface) range[i]).getComponent(j);
              }
            }
            range[i] = new Tuple(datums);

          }
        }
      }
    } // end if (coord_transform)
    ((FieldImpl)field).setSamples(range, false, false);
    return field;
  }

  /** 
   * Resample range values of this Field to domain samples in set either
   * byt nearest neighbor or multi-linear interpolation.
   * NOTE: this code is very similar to resample in FlatField.java 
   * @param set            finite sampling values for the function.
   * @param sampling_mode  type of interpolation to perform (e.g., 
   *                       Data.WEIGHTED_AVERAGE, Data.NEAREST_NEIGHBOR)
   * @param error_mode     type of error estimation to perform (e.g., 
   *                       Data.INDEPENDENT, Data.DEPENDENT, Data.NO_ERRORS)
   * @return Data object corresponding to the function value at that domain,
   *         using the sampling_mode and error_modes specified.  NOTE: may
   *         return this (i.e., not a copy).
   * @throws  VisADException   unable to resample function
   * @throws  RemoteException  Java RMI exception
   */
  public Field resampleDouble(Set set, int sampling_mode, int error_mode)
         throws VisADException, RemoteException {

    /* NB: resampling is done in this method for a double domain.  If
     * you make changes to this method, make the corresponding changes
     * in resample if necessary.
     */
    Set domainSet = getDomainSet();

    if (domainSet.equals(set)) {
      // nothing to do
      return this;
    }

    MathType range_type = ((FunctionType) Type).getRange();
    RealTupleType domain_type = ((SetType) set.getType()).getDomain();
    FunctionType func_type = new FunctionType(domain_type, range_type);
    Field field = new FieldImpl(func_type, set);
    // Field field = new FieldImpl((FunctionType) Type, set);
    if (isMissing()) return field;

    int dim = domainSet.getDimension();
    if (dim != set.getDimension()) {
      throw new SetException("FieldImpl.resample: bad Set Dimension");
    }

    if (!(domainSet instanceof GriddedDoubleSet)) {
      return resample(set, sampling_mode, error_mode);
    }

    CoordinateSystem coord_sys = set.getCoordinateSystem();
    Unit[] units = set.getSetUnits();
    ErrorEstimate[] errors =
      (error_mode == NO_ERRORS) ? new ErrorEstimate[dim] : set.getSetErrors();

    // create an array containing all indices of 'this'
    int length = set.getLength();
    int[] wedge = set.getWedge();

    // array of Data objects to receive resampled Range objects
    Data[] range = new Data[length];

    // get values from wedge and possibly transform coordinates
    double[][] vals = set.indexToDouble(wedge);
    // holder for sampling errors of transformed set - these are
    // only useful to help estmate range errors due to resampling
    ErrorEstimate[] errors_out = new ErrorEstimate[dim];
    double[][] oldvals = vals;
    try {  // this is only to throw a more meaningful message
      vals = CoordinateSystem.transformCoordinates(
                      ((FunctionType) Type).getDomain(), 
                      getDomainCoordinateSystem(),
                      getDomainUnits(), errors_out,
                      ((SetType) set.getType()).getDomain(), coord_sys,
                      units, errors, vals);
    } catch (UnitException ue) {
        throw new VisADException("Sampling set is not compatible with domain");
    }

    boolean coord_transform = !(vals == oldvals);

    // check whether we need to do sampling error calculations
    boolean sampling_errors = (error_mode != NO_ERRORS);
    if (sampling_errors) {
      for (int i=0; i<dim; i++) {
        if (errors_out[i] == null) sampling_errors = false;
      }
    }
    Data[] sampling_partials = new Data[dim];
    double[][] error_values;
    double[] means = new double[dim];

    Data[]Range = getRange ();
    if (sampling_mode == WEIGHTED_AVERAGE) {
      // resample by interpolation
      int[][] indices = new int[length][];
      double[][] coefs = new double[length][];
      ((GriddedDoubleSet) domainSet).doubleToInterp(vals, indices, coefs);
      for (int i=0; i<length; i++) {
        int len;
        len = (indices[i] == null) ? 0 : indices[i].length;
        if (len > 0) {
          Data r = null;
          // WLH
          for (int k=0; k<len; k++) {
            Data RangeIK;
            synchronized (RangeLock) {
              RangeIK = Range[indices[i][k]];
            }
            if (RangeIK != null) {
              r = (r == null) ? RangeIK.multiply(new Real(coefs[i][k])) :
                                r.add(RangeIK.multiply(new Real(coefs[i][k])));
            }
            else {
              r = null;
              break;
            }
          }

          // SRE 2002-02-13
          if (r != null) {
            r = r.changeMathType(((FunctionType)Type).getRange());
          }

          range[wedge[i]] = r;
        }
        else {
          // set range[wedge[i]] to a missing Data object
          range[wedge[i]] = ((FunctionType) Type).getRange().missingData();
        }

        if (sampling_errors && !range[wedge[i]].isMissing()) {
          for (int j=0; j<dim; j++) means[j] = vals[j][i];
          error_values = ErrorEstimate.init_error_values(errors_out, means);
          int[][] error_indices = new int[2 * dim][];
          double[][] error_coefs = new double[2 * dim][];
          coefs = new double[2 * dim][];
          ((GriddedDoubleSet) domainSet).doubleToInterp(error_values, error_indices,
                                                error_coefs);

          for (int j=0; j<dim; j++) {
            Data a = null;
            Data b = null;
            len = error_indices[2*j].length;
            if (len > 0) {
              for (int k=0; k<len; k++) {
                Data RangeIK;
                synchronized (RangeLock) {
                  RangeIK = Range[error_indices[2*j][k]];
                }
                if (RangeIK != null) {
                  a = (a == null) ?
                      RangeIK.multiply(new Real(error_coefs[2*j][k])) :
                      a.add(RangeIK.multiply(new Real(error_coefs[2*j][k])));
                }
                else {
                  a = null;
                  break;
                }
              }
            }
            len = error_indices[2*j+1].length;
            if (len > 0) {
              for (int k=0; k<len; k++) {
                Data RangeIK;
                synchronized (RangeLock) {
                  RangeIK = Range[error_indices[2*j+1][k]];
                }
                if (RangeIK != null) {
                  b = (b == null) ?
                      RangeIK.multiply(new Real(error_coefs[2*j+1][k])) :
                      b.add(RangeIK.multiply(new Real(error_coefs[2*j+1][k])));
                }
                else {
                  b = null;
                  break;
                }
              }
            }
            if (a == null || b == null) {
              sampling_partials[j] = null;
            }
            else {
              sampling_partials[j] = b.subtract(a).abs();
            }
          }

          Data error = null;
          if (error_mode == Data.INDEPENDENT) {
            for (int j=0; j<dim; j++) {
              Data e = sampling_partials[j].multiply(sampling_partials[j]);
              error = (error == null) ? e : error.add(e);
            }
            error = error.sqrt();
          }
          else { // error_mode == Data.DEPENDENT
            for (int j=0; j<dim; j++) {
              Data e = sampling_partials[j];
              error = (error == null) ? e : error.add(e);
            }
          }
          range[wedge[i]] =
            range[wedge[i]].adjustSamplingError(error, error_mode);
        } // end if (sampling_errors && !range[wedge[i]].isMissing())
      } // end for (int i=0; i<length; i++)
    }
    else { // Mode is NEAREST_NEIGHBOR or set is not GriddedSet
      // simple resampling
      int[] indices = domainSet.doubleToIndex(vals);
      for (int i=0; i<length; i++) {
        synchronized (RangeLock) {
          range[wedge[i]] = (indices[i] >= 0 && Range[indices[i]] != null) ?
                            Range[indices[i]] :
                            ((FunctionType) Type).getRange().missingData();
        }

        if (sampling_errors && !range[wedge[i]].isMissing()) {
          for (int j=0; j<dim; j++) means[j] = vals[j][i];
          error_values = ErrorEstimate.init_error_values(errors_out, means);
          int[] error_indices = domainSet.doubleToIndex(error_values);
          for (int j=0; j<dim; j++) {
            synchronized (RangeLock) {
              if (error_indices[2*j] < 0 || Range[error_indices[2*j]] == null ||
                  error_indices[2*j+1] < 0 || Range[error_indices[2*j+1]] == null) {
                sampling_partials[j] = null;
              }
              else {
                sampling_partials[j] = Range[error_indices[2*j+1]].
                                         subtract(Range[error_indices[2*j]]).abs();
              }
            }
          }

          Data error = null;
          if (error_mode == Data.INDEPENDENT) {
            for (int j=0; j<dim; j++) {
              Data e = sampling_partials[j].multiply(sampling_partials[j]);
              error = (error == null) ? e : error.add(e);
            }
            error = error.sqrt();
          }
          else { // error_mode == Data.DEPENDENT
            for (int j=0; j<dim; j++) {
              Data e = sampling_partials[j];
              error = (error == null) ? e : error.add(e);
            }
          }
          range[wedge[i]] =
            range[wedge[i]].adjustSamplingError(error, error_mode);
        } // end if (sampling_errors && !range[wedge[i]].isMissing())
      } // end for (int i=0; i<length; i++)
    }

    if (coord_transform) {
      // domain coordinates were transformed, so make corresponding
      // vector transform to any RealVectorType-s in range
      MathType RangeType = ((FunctionType) Type).getRange();
      if (RangeType instanceof RealVectorType) {
        int n = vals.length;
        double[][] inloc = new double[n][1];
        double[][] outloc = new double[n][1];
        for (int i=0; i<length; i++) {
          for (int k=0; k<n; k++) inloc[k][0] = oldvals[k][i];
          for (int k=0; k<n; k++) outloc[k][0] = vals[k][i];
          range[i] = ((RealVectorType) RangeType).transformVectors(
                      ((FunctionType) Type).getDomain(),
                      getDomainCoordinateSystem(), getDomainUnits(), errors_out,
                      ((SetType) set.getType()).getDomain(),
                      coord_sys, units,
                      ((RealTuple) range[i]).getCoordinateSystem(),
                      inloc, outloc, (RealTuple) range[i]);
        }
      }
      else if (RangeType instanceof TupleType &&
               !(RangeType instanceof RealTupleType)) {
        int m = ((TupleType) RangeType).getDimension();
        boolean any_vector = false;
        for (int j=0; j<m; j++) {
          if (((TupleType) RangeType).getComponent(j) instanceof RealVectorType) {
            any_vector = true;
          }
        }
        if (any_vector) {
          int n = vals.length;
          double[][] inloc = new double[n][1];
          double[][] outloc = new double[n][1];
          Data[] datums = new Data[m];
          for (int i=0; i<length; i++) {
            for (int k=0; k<n; k++) inloc[k][0] = oldvals[k][i];
            for (int k=0; k<n; k++) outloc[k][0] = vals[k][i];
            for (int j=0; j<m; j++) {
              MathType comp_type = ((TupleType) RangeType).getComponent(j);
              if (comp_type instanceof RealVectorType) {
                RealTuple component =
                  (RealTuple) ((TupleIface) range[i]).getComponent(j);
                datums[j] = ((RealVectorType) comp_type).transformVectors(
                            ((FunctionType) Type).getDomain(),
                            getDomainCoordinateSystem(), getDomainUnits(), errors_out,
                            ((SetType) set.getType()).getDomain(),
                            coord_sys, units, component.getCoordinateSystem(),
                            inloc, outloc, component);
              }
              else {
                datums[j] = ((TupleIface) range[i]).getComponent(j);
              }
            }
            range[i] = new Tuple(datums);

          }
        }
      }
    } // end if (coord_transform)
    ((FieldImpl)field).setSamples(range, false, false);
    return field;
  }

  public DataShadow computeRanges(ShadowType type, DataShadow shadow)
         throws VisADException, RemoteException {
    if (isMissing()) return shadow;

    ShadowRealTupleType domain_type = ((ShadowFunctionType) type).getDomain();
    int n = domain_type.getDimension();
    double[][] ranges = new double[2][n];
    // DomainSet.computeRanges handles Reference
    shadow = getDomainSet().computeRanges(domain_type, shadow, ranges, true);
    ShadowType rtype = ((ShadowFunctionType) type).getRange();


    if (MyRange != null) {
        for (int i=0; i<MyRange.length; i++) {
            synchronized (RangeLock) {
                if (MyRange[i] != null) shadow = MyRange[i].computeRanges(rtype, shadow);
            }
        }
    }
    return shadow;
  }


  /** return a Field that clones this, except its ErrorEstimate-s
      are adjusted for sampling errors in error */
  public Data adjustSamplingError(Data error, int error_mode)
         throws VisADException, RemoteException {
    if (isMissing() || error == null || error.isMissing()) return this;
    Field field = new FieldImpl((FunctionType) Type, getDomainSet());
    if (isMissing()) return field;
    Field new_error =
      ((Field) error).resample(getDomainSet(), NEAREST_NEIGHBOR, NO_ERRORS);



    //Only do this if we have a MyRange
    Data[] range = new Data[getLength()];
    for (int i=0; i<getLength(); i++) {
        synchronized (RangeLock) {
            if (MyRange != null && MyRange[i] != null) {
                range[i] = MyRange[i].adjustSamplingError(new_error.getSample(i), error_mode);
            } else {
                range[i]=null;
            }
        }
    }
    field.setSamples(range, true);
    return field;
  }

  public boolean isFlatField() {
    return false;
  }

  /**
   * A wrapper around {@link #getLength() getLength} for JPython.
   */
  public int __len__() throws VisADException, RemoteException {
    return getLength();
  }

  /**
   * A wrapper around {@link #getSample(int) getSample} for JPython.
   */
  public Data __getitem__(int index) throws VisADException, RemoteException {
    return getSample(index);
  }

  /**
   * A wrapper around {@link #setSample(int, Data) setSample} for JPython.
   */
  public void __setitem__(int index, Data data)
         throws VisADException, RemoteException {
    if (data instanceof Real && ( (Real)data).getUnit() == null) {
      __setitem__(index, ( (Real)data).getValue());
    } else {
      setSample(index, data);
    }
  }

  /**
   * A wrapper around {@link #setSample(int, Data) setSample} for JPython.
   */
  public void __setitem__(int index, double data)
         throws VisADException, RemoteException {
    RealType real = null;
    boolean tuple = false;
    MathType range = ((FunctionType) getType()).getRange();
    if (range instanceof RealType) {
      real = (RealType) range;
    }
    else if (range instanceof RealTupleType) {
      if (((RealTupleType) range).getDimension() == 1) {
        real = (RealType) ((RealTupleType) range).getComponent(0);
        tuple = true;
      }
    }
    if (real != null) {
      Real r = new Real(real, data);
      if (tuple) {
        setSample(index, new RealTuple(new Real[] {r}));
      }
      else {
        setSample(index, r);
      }
    }
    else {
      System.out.println("FieldImpl.__setitem__ bad type");
    }
  }

  /**
   * <p>Clones this instance.  The {@link MathType}, domain {@link Set}, and
   * {@link CoordinateSystem} are shallow copied.  Each range value, however,
   * has its <code>clone()</code> method invoked.</p>
   *
   * <p> Note that it is possible to simultaneously modify the domain-set of
   * both this instance and the clone by modifying the values in the array
   * returned by invoking <code>getSamples(false)</code> on the domain-set of
   * either this instance or the clone.  Don't do this unless you enjoy 
   * debugging.</p>
   *
   * @return                            A clone of this instance.
   * @throws CloneNotSupportedException if cloning isn't supported by a 
   *                                    range-value.
   */
  public Object clone() throws CloneNotSupportedException {
    FieldImpl clone = (FieldImpl)super.clone();

    synchronized(RangeLock) {
        if (MyRange != null) {
            clone.MyRange = new Data[MyRange.length];
            for (int i = 0; i < MyRange.length; i++) {
                if (MyRange[i] != null) {
                    try {
                        /*
                         * Data.dataClone() is invoked because 
                         * Data.clone() doesn't and can't exist.
                         */
                        clone.MyRange[i] = (Data)MyRange[i].dataClone();
                    }
                    catch (RemoteException ex) {
                        throw new RuntimeException(ex.toString());
                    }
                }
            }
        }
    }

    return clone;
  }

  public String longString(String pre)
         throws VisADException, RemoteException {
    StringBuffer s = new StringBuffer(pre + "FieldImpl\n" + pre + "  Type: " +
                                      Type.toString() + "\n");
    Set domainSet = getDomainSet();
    if (domainSet != null) {
      s.append(pre + "  DomainSet:\n" + domainSet.longString(pre + "    "));
    }
    else {
      s.append(pre + "  DomainSet: undefined\n");
    }
    if (isMissing()) {
      s.append("  missing\n");
      return s.toString();
    }

    if (MyRange !=null) {
      for (int i=0; i<getLength(); i++) {
        s.append(pre + "  Range value " + i + ":\n" + ((MyRange[i] == null) ?
                 (pre + "missing\n") : MyRange[i].longString(pre + "    ")));
      }
    }
    return s.toString();
  }

  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof FieldImpl)) {
      return false;
    }

    FieldImpl fi = (FieldImpl )obj;

    if (!getType().equals(fi.getType())) {
      return false;
    }

    if (getLength() != fi.getLength()) {
      return false;
    }

    if (MissingFlag != fi.MissingFlag) {
      return false;
    }
    Set domainSet = getDomainSet();

    if (domainSet == null || fi.getDomainSet() == null) {
      if (domainSet != null || fi.getDomainSet() != null) {
        return false;
      }
    } else {
      if (!domainSet.equals(fi.getDomainSet())) {
        return false;
      }
    }

    return true;
  }



/**
<PRE>
   Here's how to use this:

   for (Enumeration e = field.domainEnumeration() ; e.hasMoreElements(); ) {
     RealTuple domain_sample = (RealTuple) e.nextElement();
     Data range = field.evaluate(domain_sample);
   }
</PRE>
*/
  public Enumeration domainEnumeration()
         throws VisADException, RemoteException {
    return new FieldEnumerator(this);
  }

}

class FieldEnumerator implements Enumeration {
  Field field;
  int[] index;
  int dimension;
  RealTupleType type;
  CoordinateSystem coordinateSystem;
  RealType[] types;
  Unit[] units;

  FieldEnumerator(Field f) throws VisADException, RemoteException {
    field = f;
    if (field.getDomainSet() == null) {
      throw new FieldException("FieldImplEnumerator: DomainSet undefined");
    }
    index = new int[1];
    index[0] = 0;
    dimension = field.getDomainSet().getDimension();
    type = ((FunctionType) field.getType()).getDomain();
    /*
     * The setting of the CoordinateSytem is non-trivial because a the default
     * CoordinateSystem of the domain of a FieldImpl can be null while the
     * CoordinateSystem of the domain set of the FieldImpl can be non-null --
     * giving rise to the possibility of returning a domain RealTuple with a
     * non-null CoordinateSystem even though the corresponding RealTupleType
     * doesn't have one. (see nextElement() below.)
     */
    coordinateSystem = type.getCoordinateSystem();
    if (coordinateSystem != null)
      coordinateSystem = field.getDomainCoordinateSystem();
    types = new RealType[dimension];
    units = field.getDomainUnits();
    for (int j=0; j<dimension; j++) {
      types[j] = (RealType) type.getComponent(j);
    }
  }

  public boolean hasMoreElements() {
    try {
      return index[0] < field.getLength();
    }
    catch (RemoteException e) {
      return false;
    }
    catch (VisADException e) {
      return false;
    }
  }

  /*
   * According to the semantics for java.util.Enumeration, this method can only
   * throw a NoSuchElementException; consequently, all other exceptions must be
   * trapped and converted.
   */
  public Object nextElement() throws NoSuchElementException {
    try {
      if (index[0] < field.getLength()) {
        float[][] vals = field.getDomainSet().indexToValue(index);
        index[0]++;
        Real[] reals = new Real[dimension];
        for (int j=0; j<dimension; j++) {
          reals[j] = new Real(types[j], (double) vals[j][0], units[j]);
        }
        /*
         * The actual CoordinateSystem of the domain set is used only if the
         * domain's RealTupleType has a non-null CoordinateSystem; otherwise,
         * an exception would be thrown as of version 1.34.
         */
        return new RealTuple(type, reals, coordinateSystem);
      }
      else {
        throw new NoSuchElementException(
          "FieldImplEnumerator.nextElement: no more elements");
      }
    }
    catch (VisADException e) {
      throw new NoSuchElementException("FieldImplEnumerator.nextElement: " + e);
    }
    catch (RemoteException e) {
      throw new NoSuchElementException("FieldImplEnumerator.nextElement: " + e);
    }
  }
}

/**
  class NDhelper
  {
    int n_dims;
    int d = 0;
    int cnt = 0;
    int[] lengths;
    int[] dim_product;
    int[] sub_domain;
    int[] indexes;
    int[][] all;
    int[] work;
    int product;

    NDhelper( int[] lengths, int[] dim_product, int[] sub_domain )
    {
      this.lengths = lengths;
      this.n_dims = lengths.length;
      this.indexes = new int[ n_dims ];
      this.dim_product = dim_product;
      this.sub_domain = sub_domain;
      product = 1;
      for ( int i = 0; i < n_dims; i++ ) {
        product *= lengths[i];
      }
      work = new int[product];

      permute();
    }
    void permute()
    {
      if ( d == indexes.length )
      {
        work[cnt] = 0;
        for ( int k = 0; k < indexes.length; k++ ) {
          work[cnt] += dim_product[sub_domain[k]]*indexes[(n_dims-1) - k];
        }
        cnt++;
      }
      else
      {
        for ( int i = 0; i < lengths[(n_dims-1) - d]; i++ )
        {
          indexes[d] = i;
          d++;
          permute();
          d--;
        }
      }
    }
    public int[] getPermutations()
    {
      return work;
    }

    void permute()
    {
      for ( int k = 0; k < product; k++ )
      {
        int k2 = k;
        for ( int j = (n_dims-1); j == 0; j-- ) {
          int temp = 1;
          for ( int m = 0; m < j; m++  ) {
            temp *= lengths[m];
          }
          indexes[j] = k2/temp;
          k2 -= temp*indexes[j];
        }
        for ( int t = 0; t < indexes.length; t++ ) {
          work[k] += dim_product[sub_domain[t]]*indexes[(n_dims-1) - t];
        }
      }
    }
  }
 **/
