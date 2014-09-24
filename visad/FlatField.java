//
// FlatField.java
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
import java.util.Arrays;
import java.util.Random;

/**
   FlatField is the VisAD class for finite samplings of functions whose
   range type and range coordinate systems are simple enough to allow
   efficient representation.  The DomainSet, DomainCoordinateSystem,
   RangeSet, RangeCoordinateSystem and RangeCoordinateSystems variables
   of FlatField are immutable.<P>

   A FlatField range type may be either a RealType (for a function with
   range = R), a RealTupleType (for a function with range = R^n for n > 0),
   or a TupleType of RealType-s and RealTupleType-s..<P>

   VisAD avoids invoking methods once per datum through the use of
   FlatField's.  These are logically Field's of Tuple's of RealType's
   and RealTupleType's.  Internally FlatField's are stored as arrays of
   numerical values, rather than the arrays of data objects stored in
   Field's.  Many of the methods in the FlatField class and in other
   classes (e.g., CoordinateTransform, Set, Unit) process data in the
   form double[Dimension][Length] where Length is the number of samples
   in a Field and Dimension is the number of Tuple elements in the
   Field range values.  Note that the order of the Length and Dimension
   indices are reversed as array indices.  This allows efficient
   processing of long columns of Field value components.  For example,
   if Latitude is one component of Field values, then any computation
   involving Latitude can be applied in a tight loop to all Latitude's
   in the Field.<P>

   FlatField's support range types more general than RealTuple's.  To
   understand the motive, consider a set of observations that include
   Latitude, Longitude, Altitude, Pressure, Temperature, etc.  We can
   organize these as a Field whose range values have the Tuple type:<P>
 <PRE>

     (Latitude, Longitude, Altitude, Pressure, Temperature, ...)

</PRE>
   However, in order to declare that (Latitude, Longitude, Altitude)
   is a coordinate system with coordinate transform functions to other
   spatial coordinate systems, we need to organize:<P>
<PRE>

     (Latitude, Longitude, Altitude)

</PRE>
   as a RealTupleType.  Hence the range type of the Field of observations
   must be:<P>
<PRE>

     ((Latitude, Longitude, Altitude), Pressure, Temperature, ...)

</PRE>
   which is not a RealTupleType (since one of its components is a
   RealTupleType).  In order to process such data efficiently, FlatField's
   must support range types that are Tuple's of RealType's and
   RealTupleType's.<P>
*/
public class FlatField extends FieldImpl implements FlatFieldIface {

  protected int TupleDimension; // dimension of Type.getFlatRange()
  private Set RangeSet[]; // one 1-D Set per range components
  private int RangeMode[]; // DOUBLE, FLOAT, INT, SHORT or BYTE
 // coordinate system of the function range R^n
  protected CoordinateSystem RangeCoordinateSystem; // used if Type.Real
  protected CoordinateSystem[] RangeCoordinateSystems; // used for Flat, not for Real
  private boolean MissingFlag;

  protected Unit[] RangeUnits;

  // RangeErrors, like range values, are not immutable
  private ErrorEstimate[] RangeErrors;

  // for tuple memeber i, only one of these is not null, depending on RangeSet[i]
  private double[][] DoubleRange;
  private float[][] FloatRange;
  private long[][] LongRange; // not currently used because array
                            // indices are int's
  private int[][] IntRange;
  private short[][] ShortRange;
  private byte[][] ByteRange;

  private static final int MISSING1 = Byte.MIN_VALUE;      // least byte
  private static final int MISSING2 = Short.MIN_VALUE;     // least short
  private static final int MISSING4 = Integer.MIN_VALUE;   // least int
  // private static final int MISSING8 = Long.MIN_VALUE;   // least long

  private static final int DOUBLE = 1;
  private static final int FLOAT = 2;
  private static final int LONG = 3; // not currently used because array
                                     // indices are int's
  private static final int INT = 4;
  private static final int SHORT = 5;
  private static final int BYTE = 6;

  /**
   * Constructs a FlatField from a function type.  The domain Set is the
   * default Set of the function domain.
   * @param type                The type of the function.
   * @throws VisADException     Couldn't create necessary VisAD object.
   */
  public FlatField(FunctionType type) throws VisADException {
    this(type, type.getDomain().getDefaultSet(), null, null, null, null);
  }

  /**
   * Constructs a FlatField from a function type and a (non-default) domain
   * Set.
   * @param type                The type of the function.
   * @param domain_set          The sampling set of the domain.  May be <code>
   *                            null</code>, in which case the sampling set is
   *                            the default Set of the function domain.
   * @throws VisADException     Couldn't create necessary VisAD object.
   */
  public FlatField(FunctionType type, Set domain_set) throws VisADException {
    this(type, domain_set, null, null, null, null);
  }

  /**
   * Constructs a FlatField from a function type, a sampling set of the domain,
   * a coordinate system for the range, sampling sets for the range components,
   * and units for the range components.
   * @param type                The type of the function.
   * @param domain_set          The sampling set of the domain.  May be
   *                            <code> null</code>, in which case the
   *                            sampling set is set to the default sampling
   *                            set of the function domain (i.e. <code>
   *                            type.getDomain().getDefaultSet()</code>).
   * @param range_coord_sys     Optional coordinate system for the range.
   *                            May be <code>null</code>.  If non-<code>null
   *                            </code>, then the range of the function shall
   *                            be a RealTuple (i.e. <code>type.getRange()
   *                            instanceof RealTuple</code> shall be
   *                            true) and the reference coordinate system
   *                            of the range coordinate system shall be
   *                            the same as the reference coordiinate
   *                            system of the function range (i.e.
   *                            <code>range_coord_sys().getReference().equals(
   *                            ((RealTuple)type.getRange()).getReference())
   *                            </code> shall be true).
   * @param range_sets          The sampling sets of the (flat) range
   *                            components.  May be <code>null</code>, in which
   *                            case the default sampling sets of the range
   *                            component RealType-s are used (i.e. <code>
   *                            ((RealType)type.getFlatRange().getComponent(i))
   *                            .getDefaultSet()</code> for all <code>i</code>
   *                            in the flat range).  If non-<code>null</code>,
   *                            then the <code> i</code>th flat range
   *                            component values are stored in bytes
   *                            if <code>range_sets[i].getLength() <
   *                            256</code>, stored in shorts if <code>
   *                            range_sets[i].getLength() < 65536</code>, etc.
   * @param units               The units of the (flat) range components.  May
   *                            be <code>null</code>, in which case the default
   *                            units of the flat range RealType-s are used.
   * @throws VisADException     Couldn't create necessary VisAD object.
   */
  public FlatField(FunctionType type, Set domain_set,
                   CoordinateSystem range_coord_sys, Set[] range_sets,
                   Unit[] units) throws VisADException {
    this(type, domain_set, range_coord_sys, null, range_sets, units);
  }

  /**
   * Constructs a FlatField from a function type, a sampling set of the domain,
   * coordinate systems for the range components, sampling sets for the range
   * components, and units for the range components.
   * @param type                The type of the function.
   * @param domain_set          The sampling set of the domain.  May be null, in
   *                            which case the sampling set is set to the
   *                            default sampling set of the function domain
   *                            (i.e. <code>
   *                            type.getDomain().getDefaultSet()</code>).
   * @param range_coord_syses   Optional coordinate systems for the range
   *                            components.  May be <code>null</code>.
   *                            If the <code>i</code>th component
   *                            of the range is a RealTuple, then
   *                            <code>range_coord_syses[i]</code> may be
   *                            non-<code>null</code> and, if so, is the
   *                            coordinate system for that component and shall
   *                            have the same reference coordinate system as
   *                            the corresponding component in the range of the
   *                            function type.
   * @param range_sets          The sampling sets of the (flat) range
   *                            components.  May be <code>null</code>, in which
   *                            case the default sampling sets of the range
   *                            component RealType-s are used (i.e. <code>
   *                            ((RealType)type.getFlatRange().getComponent(i))
   *                            .getDefaultSet()</code> for all <code>i</code>
   *                            in the flat range).  If non-<code>null</code>,
   *                            then the <code> i</code>th flat range
   *                            component values are stored in bytes
   *                            if <code>range_sets[i].getLength() <
   *                            256</code>, stored in shorts if <code>
   *                            range_sets[i].getLength() < 65536</code>, etc.
   * @param units               The units of the (flat) range components.  May
   *                            be <code>null</code>, in which case the default
   *                            units of the flat range RealType-s are used.
   * @throws VisADException     Couldn't create necessary VisAD object.
   */
  public FlatField(FunctionType type, Set domain_set,
                   CoordinateSystem[] range_coord_syses, Set[] range_sets,
                   Unit[] units) throws VisADException {
    this(type, domain_set, null, range_coord_syses, range_sets, units);
  }

  /**
   * Constructs a FlatField from a function type, a sampling set of the domain,
   * a coordinate system for the range, coordinate systems for the range
   * components, sampling sets for the range components, and units for the range
   * components.  This is the most general constructor.
   * @param type                The type of the function.
   * @param domain_set          The sampling set of the domain.  May be null, in
   *                            which case the sampling set is set to the
   *                            default sampling set of the function domain
   *                            (i.e. <code>
   *                            type.getDomain().getDefaultSet()</code>).
   * @param range_coord_sys     Optional coordinate system for the range.
   *                            May be <code>null</code>.  If non-<code>null
   *                            </code>, then the range of the function shall
   *                            be a RealTuple (i.e. <code>type.getRange()
   *                            instanceof RealTuple</code> shall be
   *                            true) and the reference coordinate system
   *                            of the range coordinate system shall be
   *                            the same as the reference coordiinate
   *                            system of the function range (i.e.
   *                            <code>range_coord_sys().getReference().equals(
   *                            ((RealTuple)type.getRange()).getReference())
   *                            </code> shall be true).
   * @param range_coord_syses   Optional coordinate systems for the range
   *                            components.  May be <code>null</code>.
   *                            If the <code>i</code>th component
   *                            of the range is a RealTuple, then
   *                            <code>range_coord_syses[i]</code> may be
   *                            non-<code>null</code> and, if so, is the
   *                            coordinate system for that component and shall
   *                            have the same reference coordinate system as
   *                            the corresponding component in the range of the
   *                            function type.
   * @param range_sets          The sampling sets of the (flat) range
   *                            components.  May be <code>null</code>, in which
   *                            case the default sampling sets of the range
   *                            component RealType-s are used (i.e. <code>
   *                            ((RealType)type.getFlatRange().getComponent(i))
   *                            .getDefaultSet()</code> for all <code>i</code>
   *                            in the flat range).  If non-<code>null</code>,
   *                            then the <code> i</code>th flat range
   *                            component values are stored in bytes
   *                            if <code>range_sets[i].getLength() <
   *                            256</code>, stored in shorts if <code>
   *                            range_sets[i].getLength() < 65536</code>, etc.
   * @param units               The units of the (flat) range components.  May
   *                            be <code>null</code>, in which case the default
   *                            units of the flat range RealType-s are used.
   * @throws SetException       if <code>range_sets</code> is non-<code>null
   *                            </code> and either one of its elements is
   *                            <code>null</code> or an element's dimension is
   *                            not one.
   * @throws VisADException     Couldn't create necessary VisAD object.
   */
  public FlatField(FunctionType type, Set domain_set,
                   CoordinateSystem range_coord_sys,
                   CoordinateSystem[] range_coord_syses,
                   Set[] range_sets, Unit[] units)
          throws VisADException {
   //Call FieldImpl.ctor passing false to tell it not to create the Range array
    super(type, domain_set, false);
    pr ("ctor");

    if (!type.getFlat()) {
      throw new FieldException("FlatField: FunctionType must be Flat");
    }
    MathType RangeType = type.getRange();
    RealTupleType FlatRange = type.getFlatRange();
    TupleDimension = FlatRange.getDimension();
    DoubleRange = new double[TupleDimension][];

    // set RangeSet
    RangeSet = new Set[TupleDimension];
    RangeMode = new int[TupleDimension];
    if (range_sets == null) {
      // set the default range sampling; if no default, use double
      for (int i=0; i<TupleDimension; i++) {
        RangeSet[i] = ((RealType) FlatRange.getComponent(i)).getDefaultSet();
        if (RangeSet[i] == null) {
          RangeSet[i] = new FloatSet(new SetType(FlatRange.getComponent(i)));
          // WLH 1 Feb 98
          // RangeSet[i] = new DoubleSet(new SetType(FlatRange.getComponent(i)));
        }
      }
    }
    else {
      // set explicit range sets
      if (TupleDimension != range_sets.length) {
        throw new SetException("FlatField: range set dimensions don't match");
      }
      for (int i=0; i<TupleDimension; i++) {
        if (range_sets[i] == null || range_sets[i].getDimension() != 1) {
          throw new SetException("FlatField: each range set dimension must be 1");
        }
      }
      // force RangeSet Type-s to match FlatRange
      for (int i=0; i<TupleDimension; i++) {
        if (FlatRange.getComponent(i).equals(
            ((SetType) range_sets[i].getType()).getDomain())) {
          RangeSet[i] = range_sets[i];
        }
        else {
          RangeSet[i] = (Set) range_sets[i].cloneButType(
                        new SetType(FlatRange.getComponent(i)));
        }
      }
    }
    nullRanges();

    // set RangeCoordinateSystem or RangeCoordinateSystems
    // also set RangeUnits
    if (type.getReal()) {
      // only one RangeCoordinateSystem
      Unit[] type_units;
      if (range_coord_syses != null) {
        throw new CoordinateSystemException("FlatField: Real Function" +
               " Type requires single range coordinate system");
      }
      RangeCoordinateSystems = null;
      RangeCoordinateSystem = FlatRange.getCoordinateSystem();
      if (range_coord_sys != null) {
        if (!(RangeType instanceof RealTupleType)) {
          throw new CoordinateSystemException("FlatField: " +
                    "range_coord_sys supplied, but RangeType is not RealTupleType");
        }
        if (RangeCoordinateSystem == null ||
            !RangeCoordinateSystem.getReference().equals(
             range_coord_sys.getReference())) {
          throw new CoordinateSystemException("FlatField: range_coord_sys " +
                                            (range_coord_sys == null ? null :
                                             range_coord_sys.getReference()) +
                                            " must match" +
                                            " default range CoordinateSystem " +
                                            (RangeCoordinateSystem == null ? null :
                                             RangeCoordinateSystem.getReference()));
        }
        RangeCoordinateSystem = range_coord_sys;
      }
      if (units == null) {
        RangeUnits = (RangeCoordinateSystem == null) ?
                     FlatRange.getDefaultUnits() :
                     RangeCoordinateSystem.getCoordinateSystemUnits();
      }
      else {
        if (units.length != TupleDimension) {
          throw new UnitException("FlatField: units dimension does not match");
        }
        RangeUnits = new Unit[TupleDimension];
        for (int i=0; i<TupleDimension; i++) {
          RealType componentType = (RealType)FlatRange.getComponent(i);
          Unit componentUnit = units[i];
          RangeUnits[i] =
            componentUnit == null || !componentType.isInterval()
              ? componentUnit
              : componentUnit.getAbsoluteUnit();
        }
      }
      if (RangeType instanceof RealTupleType) {
        type_units = ((RealTupleType) RangeType).getDefaultUnits();
      }
      else {
        type_units = new Unit[1];
        type_units[0] = ((RealType) RangeType).getDefaultUnit();
      }
      if (RangeCoordinateSystem != null &&
          !Unit.canConvertArray(RangeCoordinateSystem.getCoordinateSystemUnits(),
                                type_units)) {
        throw new UnitException("FlatField: RangeCoordinateSystem Units must be " +
                                "convertable with RangeType default Units");
      }
      if (RangeCoordinateSystem != null &&
          !Unit.canConvertArray(RangeCoordinateSystem.getCoordinateSystemUnits(),
                                RangeUnits)) {
        throw new UnitException("FlatField: RangeUnits must be convertable " +
                                "with RangeCoordinateSystem Units");
      }
      if (!Unit.canConvertArray(type_units, RangeUnits)) {
        throw new UnitException("FlatField: RangeUnits must be convertable " +
                                "with RangeType default Units");
      }
    }
    else { // !type.getReal()
      // multiple RangeCoordinateSystems
      Unit[] sub_range_units;
      Unit[] sub_type_units;
      if (range_coord_sys != null) {
        throw new CoordinateSystemException("FlatField: non-Real Function" +
               " Type requires multiple range coordinate systems");
      }
      RangeCoordinateSystem = null;
      int n = ((TupleType) RangeType).getDimension();
      RangeCoordinateSystems = new CoordinateSystem[n];
      for (int i=0; i<n; i++) {
        MathType component = ((TupleType) RangeType).getComponent(i);
        if (component instanceof RealTupleType) {
          RangeCoordinateSystems[i] =
            ((RealTupleType) component).getCoordinateSystem();
          if (range_coord_syses != null && range_coord_syses[i] != null) {
            if (RangeCoordinateSystems[i] == null ||
                RangeCoordinateSystems[i].getReference() !=
                range_coord_syses[i].getReference()) {
              throw new TypeException("FlatField: range_coord_syses must" +
                                      " match Range DefaultCoordinateSystem");
            }
            RangeCoordinateSystems[i] = range_coord_syses[i];
          }
        }
        else {
          RangeCoordinateSystems[i] = null;
        }
      }
      if (units == null) {
        RangeUnits = FlatRange.getDefaultUnits();
        int j = 0;
        for (int i=0; i<n; i++) {
          if (RangeCoordinateSystems[i] != null) {
            sub_range_units =
              RangeCoordinateSystems[i].getCoordinateSystemUnits();
            for (int k=0; k<sub_range_units.length; k++) {
              RangeUnits[j + k] = sub_range_units[k];
            }
          }
          MathType component = ((TupleType) RangeType).getComponent(i);
          if (component instanceof RealType) {
            j++;
          }
          else if (component instanceof RealTupleType) {
            j += ((RealTupleType) component).getDimension();
          }
/* WLH 117 April 99
          j += n;
*/
        }
      }
      else {
        if (units.length != TupleDimension) {
          throw new UnitException("FlatField: units dimension does not match");
        }
        RangeUnits = new Unit[TupleDimension];
        for (int i=0; i<TupleDimension; i++) {
          RealType componentType = (RealType)FlatRange.getComponent(i);
          Unit componentUnit = units[i];
          RangeUnits[i] =
            componentUnit == null || !componentType.isInterval()
              ? componentUnit
              : componentUnit.getAbsoluteUnit();
        }
      }

      int j = 0;
      for (int i=0; i<n; i++) {
        int m;
        MathType component = ((TupleType) RangeType).getComponent(i);
        if (component instanceof RealTupleType) {
          sub_type_units = ((RealTupleType) component).getDefaultUnits();
          m = ((RealTupleType) component).getDimension();
          sub_range_units = new Unit[m];
          for (int k=0; k<m; k++) {
            sub_range_units[k] = RangeUnits[j + k];
          }
        }
        else {
          sub_type_units = new Unit[1];
          sub_type_units[0] = ((RealType) component).getDefaultUnit();
          m = 1;
          sub_range_units = new Unit[1];
          sub_range_units[0] = RangeUnits[j];
        }

        if (RangeCoordinateSystems[i] != null &&
            !Unit.canConvertArray(sub_type_units,
                  RangeCoordinateSystems[i].getCoordinateSystemUnits())) {
          throw new UnitException("FlatField: RangeCoordinateSystems Units must " +
                                  "be convertable with RangeType default Units");
        }
        if (RangeCoordinateSystems[i] != null &&
            !Unit.canConvertArray(sub_range_units,
                  RangeCoordinateSystems[i].getCoordinateSystemUnits())) {
          throw new UnitException("FlatField: RangeUnits must be convertable " +
                                  "with RangeCoordinateSystems Units");
        }
        if (!Unit.canConvertArray(sub_type_units, sub_range_units)) {
          throw new UnitException("FlatField: RangeUnits must be convertable " +
                                  "with RangeType default Units");
        }
        j += m;
      }
    } // end !type.getReal()

    if (RangeUnits == null) RangeUnits = new Unit[TupleDimension];


    // initialize RangeErrors to all null
    RangeErrors = new ErrorEstimate[TupleDimension];

    // initially all values are missing
    MissingFlag = true;
  }

  /**
   * Returns the range CoordinateSystem assuming that the range type is
   * a RealTupleType (and throws a TypeException if its not).  This may
   * differ from default CoordinateSystem of range RealTupleType, but must be
   * convertable; the index has length = 1 (since all samples have the same
   * Units).
   *
   * @return                    The CoordinateSystem of the RealTuple range.
   *                            Will not be <code>null</code> and will be of
   *                            length 1.
   * @throws TypeException      The type of the range is neither RealType nor
   *                            RealTupleType.
   */
  public CoordinateSystem[] getRangeCoordinateSystem()
         throws TypeException {
    MathType RangeType = ((FunctionType) Type).getRange();
    if (!((FunctionType) Type).getReal()) {
      throw new TypeException("FlatField.getRangeCoordinateSystem: " +
        "Range is not Real, need DefaultCoordinateSystem index");
    }
    CoordinateSystem[] cs = {RangeCoordinateSystem};
    return cs;
  }

  /**
   * Returns the sampling set of each flat component.
   * @return            The sampling set of each component in the flat range.
   */
  public Set[] getRangeSets() {
    Set[] sets = new Set[RangeSet.length];
    System.arraycopy(RangeSet, 0, sets, 0, sets.length);
    return sets;
  }

  /**
   * Returns the CoordinateSystem of a component of the range.  The MathType
   * of the range shall be a TupleType.
   *
   * @param i                   The index of the component.  The value shall be
   *                            greater than or equal to zero and less that the
   *                            number of components in the TupleType of the
   *                            range.
   * @return                    The CoordinateSystem of the <code>i</code>-th
   *                            component.  Won't be <code>null</code> and will
   *                            be of length 1.  The single element might be
   *                            <code>null</code>.
   * @throws TypeException      The type of the range is either RealType or
   *                            RealTupleType.
   */
  public CoordinateSystem[] getRangeCoordinateSystem(int i)
         throws TypeException {
    if (((FunctionType) Type).getReal()) {
      throw new TypeException("FlatField.getRangeCoordinateSystem: " +
        "Range is Real, cannot specify CoordinateSystem index");
    }
    CoordinateSystem[] cs = {RangeCoordinateSystems[i]};
    return cs;
  }

  /** return array of Units associated with each RealType
      component of range; these may differ from default
      Units of range RealTypes, but must be convertable;
      the second index has length = 1 (since all samples
      have the same Units) */
  public Unit[][] getRangeUnits() {
    Unit[][] units = new Unit[RangeUnits.length][1];
    for (int i=0; i<RangeUnits.length; i++) {
      units[i][0] = RangeUnits[i];
    }
    return units;
  }

  /** return array of ErrorEstimates associated with each
      RealType component of range; each ErrorEstimate is a
      mean error for all samples of a range RealType
      component */
  public ErrorEstimate[] getRangeErrors() {
    synchronized (RangeErrors) {
      return ErrorEstimate.copyErrorsArray(RangeErrors);
    }
  }

  /**
   * Sets the ErrorEstimates associated with each RealType component of the
   * range.  <code>errors[i]</code> is the {@link ErrorEstimate} for the 
   * <code>i</code>-th {@link RealType} component.
   *
   * @param errors          The error estimates for the range values.
   * @throws FieldException if <code>errors</code> is non-<code>null</code> and
   *                        <code>errors.length != getRangeDimension()</code>.
   */
  public void setRangeErrors(ErrorEstimate[] errors) throws FieldException {
    synchronized (RangeErrors) {
      if (errors == null) {
        for (int i=0; i<TupleDimension; i++) {
          RangeErrors[i] = null;
        }
      }
      else {
        if (errors.length != TupleDimension) {
          throw new FieldException("FlatField.setRangeErrors: errors " +
                                   "dimension does not match");
        }
        for (int i=0; i<TupleDimension; i++) {
          RangeErrors[i] = errors[i];
        }
      }
    }
  }

  /** set the range values of the function; the order of range values
      must be the same as the order of domain indices in the DomainSet;
      copy argument included for consistency with Field, but ignored */
  public void setSamples(Data[] range, boolean copy)
         throws VisADException, RemoteException {
    if (range == null || range.length != getLength()) {
      throw new FieldException("setSamples: bad Data[] length");
    }
    for (int i=0; i<getLength(); i++) {
      setSample(i, range[i]);
    }
  }

  /** set range array as range values of this FlatField;
      the array is dimensioned
      double[number_of_range_components][number_of_range_samples];
      the order of range values must be the same as the order of domain
      indices in the DomainSet */
  public void setSamples(double[][] range)
         throws VisADException, RemoteException {
    setSamples(range, null, true);
  }

  /** set range array as range values of this FlatField;
      the array is dimensioned
      float[number_of_range_components][number_of_range_samples];
      the order of range values must be the same as the order of domain
      indices in the DomainSet */
  public void setSamples(float[][] range)
         throws VisADException, RemoteException {
    setSamples(range, null, true);
  }

  /** set range array as range values of this FlatField;
      the array is dimensioned
      double[number_of_range_components][number_of_range_samples];
      the order of range values must be the same as the order of domain
      indices in the DomainSet; copy array if copy flag is true */
  public void setSamples(double[][] range, boolean copy)
         throws VisADException, RemoteException {
    setSamples(range, null, copy);
  }

  /** set range array as range values of this FlatField;
      the array is dimensioned
      float[number_of_range_components][number_of_range_samples];
      the order of range values must be the same as the order of domain
      indices in the DomainSet; copy array if copy flag is true */
  public void setSamples(float[][] range, boolean copy)
         throws VisADException, RemoteException {
    setSamples(range, null, copy);
  }

  /** set the range values of the function including ErrorEstimate-s;
      the order of range values must be the same as the order of
      domain indices in the DomainSet */
  public void setSamples(double[][] range, ErrorEstimate[] errors,
              boolean copy) throws VisADException, RemoteException {
    if(range.length != TupleDimension ||
       (errors != null && errors.length != TupleDimension)) {
      throw new FieldException("FlatField.setSamples: bad tuple length");
    }

    for (int i=0; i<TupleDimension; i++) {
      if (range[i].length != getLength()) {
        throw new FieldException("setSamples: bad array length");
      }
    }
    packValues(range, copy);
    setRangeErrors(errors);
    notifyReferences();
  }

    /**
       This returns true if any of the RangeMode values == DOUBLE. We use this
       so in operations (e.g., unary, binary) that deal with the data we try
       to keep it to be floats if we can (for size efficiency).
     **/
    private boolean  shouldBeDouble () {
      for (int i=0; i<TupleDimension; i++) {
          if (RangeMode[i] == DOUBLE) {
              return true;
          }
      }
      return false;
    }


  /** update a subset of a FlatField's range samples, where
      start is the index of the first sample to update and
      range[0].length is the number of samples to update;
      the array is dimensioned
      double[number_of_range_components][number_of_range_samples] */
  public void setSamples(int start, double[][] range)
         throws VisADException, RemoteException
  {
    if(range.length != TupleDimension ) {
      throw new FieldException("FlatField.setSamples: bad tuple length");
    }

    for (int i=0; i<TupleDimension; i++) {
      if (range[i].length + start > getLength()) {
        throw new FieldException("setSamples: bad array length");
      }
    }
    
    boolean copy = false;
    int length = range[0].length;

  //--  packValues starts here:

    // NOTE INVERTED ORDER OF range ARRAY INDICES !!!
    int[] index;
    synchronized (DoubleRange) {
      for (int i=0; i<TupleDimension; i++) {
        double[] rangeI = range[i];
        double[][] range1 = new double[1][];
        range1[0] = rangeI;
        switch (RangeMode[i]) {
          case DOUBLE:
            if (DoubleRange[i] == null) {
              DoubleRange[i] = new double[getLength()];
            }
            double[] DoubleRangeI = DoubleRange[i];
            System.arraycopy(rangeI, 0, DoubleRangeI, start, length);
            break;
          case FLOAT:
            if (FloatRange[i] == null) {
              FloatRange[i] = new float[getLength()];
              for (int j=0; j<getLength(); j++) FloatRange[i][j] = Float.NaN;
            }
            float[] FloatRangeI = FloatRange[i];
            for (int j=0; j<length; j++) FloatRangeI[start + j] = (float) rangeI[j];
            break;
          case BYTE:
            index = RangeSet[i].valueToIndex(Set.doubleToFloat(range1));
            if (ByteRange[i] == null) {
              ByteRange[i] = new byte[getLength()];
              for (int j=0; j<getLength(); j++) ByteRange[i][j] = (byte) MISSING1;
            }
            byte[] ByteRangeI = ByteRange[i];
            for (int j=0; j<length; j++) {
              ByteRangeI[start + j] = (byte) (index[j] + MISSING1 + 1);
            }
            break;
          case SHORT:
            index = RangeSet[i].valueToIndex(Set.doubleToFloat(range1));
            if (ShortRange[i] == null) {
              ShortRange[i] = new short[getLength()];
              for (int j=0; j<getLength(); j++) ShortRange[i][j] = (short) MISSING2;
            }
            short[] ShortRangeI = ShortRange[i];
            for (int j=0; j<length; j++) {
              ShortRangeI[start + j] = (short) (index[j] + MISSING2 + 1);
            }
            break;
          case INT:
            index = RangeSet[i].valueToIndex(Set.doubleToFloat(range1));
            if (IntRange[i] == null) {
              IntRange[i] = new int[getLength()];
              for (int j=0; j<getLength(); j++) IntRange[i][j] = (int) MISSING4;
            }
            int[] IntRangeI = IntRange[i];
            for (int j=0; j<length; j++) {
              IntRangeI[start + j] = index[j] + MISSING4 + 1;
            }
            break;
          default:
            throw new SetException("FlatField.packValues: bad RangeMode");
        }
      }
      clearMissing();
    }
  //-- End packValues

    setRangeErrors(null);
    notifyReferences();
  }

  public void setSamples(int[] indices, double[][] range)
         throws VisADException, RemoteException
  {
      pr ("setSamples");


    int length = indices.length;

    if(range.length != TupleDimension ) {
      throw new FieldException("FlatField.setSamples: bad tuple length");
    }
    if ( length > getLength() ) {
      throw new FieldException("setSamples: indices array too long");
    }
    for (int i=0; i<TupleDimension; i++) {
      if (range[i].length != length) {
        throw new FieldException("setSamples: bad data array length");
      }
    }

    boolean copy = false;

  //--  packValues starts here:

    // NOTE INVERTED ORDER OF range ARRAY INDICES !!!
    int[] index;
    synchronized (DoubleRange) {
      for (int i=0; i<TupleDimension; i++) {
        double[] rangeI = range[i];
        double[][] range1 = new double[1][];
        range1[0] = rangeI;


        switch (RangeMode[i]) {
          case DOUBLE:
            if (DoubleRange[i] == null) {
              DoubleRange[i] = new double[getLength()];
            }
            double[] DoubleRangeI = DoubleRange[i];
            for (int j = 0; j < length; j++) {
              DoubleRangeI[indices[j]] = rangeI[j];
            }
            break;
          case FLOAT:
            if (FloatRange[i] == null) {
              FloatRange[i] = new float[getLength()];
              for (int j=0; j<getLength(); j++) FloatRange[i][j] = Float.NaN;
            }
            float[] FloatRangeI = FloatRange[i];
            for (int j=0; j<length; j++) FloatRangeI[indices[j]] = (float) rangeI[j];
            break;
          case BYTE:
            index = RangeSet[i].valueToIndex(Set.doubleToFloat(range1));
            if (ByteRange[i] == null) {
              ByteRange[i] = new byte[getLength()];
              for (int j=0; j<getLength(); j++) ByteRange[i][j] = (byte) MISSING1;
            }
            byte[] ByteRangeI = ByteRange[i];
            for (int j=0; j<length; j++) {
              ByteRangeI[indices[j]] = (byte) (index[j] + MISSING1 + 1);
            }
            break;
          case SHORT:
            index = RangeSet[i].valueToIndex(Set.doubleToFloat(range1));
            if (ShortRange[i] == null) {
              ShortRange[i] = new short[getLength()];
              for (int j=0; j<getLength(); j++) ShortRange[i][j] = (short) MISSING2;
            }
            short[] ShortRangeI = ShortRange[i];
            for (int j=0; j<length; j++) {
              ShortRangeI[indices[j]] = (short) (index[j] + MISSING2 + 1);
            }
            break;
          case INT:
            index = RangeSet[i].valueToIndex(Set.doubleToFloat(range1));
            if (IntRange[i] == null) {
              IntRange[i] = new int[getLength()];
              for (int j=0; j<getLength(); j++) IntRange[i][j] = (int) MISSING4;
            }
            int[] IntRangeI = IntRange[i];
            for (int j=0; j<length; j++) {
              IntRangeI[indices[j]] = index[j] + MISSING4 + 1;
            }
            break;
          default:
            throw new SetException("FlatField.packValues: bad RangeMode");
        }
      }
      clearMissing();
    }
  //-- End packValues

    setRangeErrors(null);
    notifyReferences();
  }

  /** set the range values of the function including ErrorEstimate-s;
      the order of range values must be the same as the order of
      domain indices in the DomainSet */
  public void setSamples(float[][] range, ErrorEstimate[] errors,
              boolean copy) throws VisADException, RemoteException {
    if(range.length != TupleDimension ||
       (errors != null && errors.length != TupleDimension)) {
      throw new FieldException("FlatField.setSamples: bad tuple length");
    }

    for (int i=0; i<TupleDimension; i++) {
      if (range[i].length != getLength()) {
        throw new FieldException("setSamples: bad array length");
      }
    }
    packValues(range, copy);
    setRangeErrors(errors);
    notifyReferences();
  }

    protected void pr (String message) {
//              System.err.println ( hashCode () + " " + getClass().getName () + "  " + message);
    }

  /** pack an array of doubles into field sample values according to the
      RangeSet-s; copies data */
  private void packValues(double[][] range, boolean copy)
          throws VisADException {
    // NOTE INVERTED ORDER OF range ARRAY INDICES !!!
    int[] index;
    pr ("packValuesD");



    synchronized (DoubleRange) {
      nullRanges();
      for (int i=0; i<TupleDimension; i++) {
        double[] rangeI = range[i];
        double[][] range1 = new double[1][];
        range1[0] = rangeI;
        switch (RangeMode[i]) {
          case DOUBLE:
            if (copy) {
              DoubleRange[i] = new double[getLength()];
              double[] DoubleRangeI = DoubleRange[i];
              System.arraycopy(rangeI, 0, DoubleRangeI, 0, getLength());
              // for (int j=0; j<getLength(); j++) DoubleRangeI[j] = rangeI[j];
            }
            else {
              DoubleRange[i] = rangeI;
            }
            break;
          case FLOAT:
            FloatRange[i] = new float[getLength()];
            float[] FloatRangeI = FloatRange[i];
            for (int j=0; j<getLength(); j++) FloatRangeI[j] = (float) rangeI[j];
            break;
          case BYTE:
            index = RangeSet[i].valueToIndex(Set.doubleToFloat(range1));
            ByteRange[i] = new byte[getLength()];
            byte[] ByteRangeI = ByteRange[i];
            for (int j=0; j<getLength(); j++) {
              ByteRangeI[j] = (byte) (index[j] + MISSING1 + 1);
            }
            break;
          case SHORT:
            index = RangeSet[i].valueToIndex(Set.doubleToFloat(range1));
            ShortRange[i] = new short[getLength()];
            short[] ShortRangeI = ShortRange[i];
            for (int j=0; j<getLength(); j++) {
              ShortRangeI[j] = (short) (index[j] + MISSING2 + 1);
            }
            break;
          case INT:
            index = RangeSet[i].valueToIndex(Set.doubleToFloat(range1));
            IntRange[i] = new int[getLength()];
            int[] IntRangeI = IntRange[i];
            for (int j=0; j<getLength(); j++) {
              IntRangeI[j] = index[j] + MISSING4 + 1;
            }
            break;
          default:
            throw new SetException("FlatField.packValues: bad RangeMode");
        }
      }
      clearMissing();
    }
  }

  /**
   * Pack an array of floats into field sample values according to the
   * RangeSet-s; copies data.
   *
   * @throws VisADException if {@link #nullRanges()} fails.
   */
  private void packValues(float[][] range, boolean copy)
          throws VisADException {

    // NOTE INVERTED ORDER OF range ARRAY INDICES !!!
    int[] index;
    synchronized (DoubleRange) {
      nullRanges();
      for (int i=0; i<TupleDimension; i++) {
        float[] rangeI = range[i];
        float[][] range1 = new float[1][];
        range1[0] = rangeI;
        switch (RangeMode[i]) {
          case DOUBLE:
            DoubleRange[i] = new double[getLength()];
            double[] DoubleRangeI = DoubleRange[i];
            for (int j=0; j<getLength(); j++) DoubleRangeI[j] = rangeI[j];
            break;
          case FLOAT:
            if (copy) {
              FloatRange[i] = new float[getLength()];
              float[] FloatRangeI = FloatRange[i];
              System.arraycopy(rangeI, 0, FloatRangeI, 0, getLength());
              // for (int j=0; j<getLength(); j++) FloatRangeI[j] = (float) rangeI[j];
            }
            else {
              FloatRange[i] = rangeI;
            }
            break;
          case BYTE:
            index = RangeSet[i].valueToIndex(range1);
            ByteRange[i] = new byte[getLength()];
            byte[] ByteRangeI = ByteRange[i];
            int offset = MISSING1+1;
            for (int j=0; j<getLength(); j++) {
              ByteRangeI[j] = (byte) (index[j] + offset);
            }
            break;
          case SHORT:
            index = RangeSet[i].valueToIndex(range1);
            ShortRange[i] = new short[getLength()];
            short[] ShortRangeI = ShortRange[i];
            for (int j=0; j<getLength(); j++) {
              ShortRangeI[j] = (short) (index[j] + MISSING2 + 1);
            }
            break;
          case INT:
            index = RangeSet[i].valueToIndex(range1);
            IntRange[i] = new int[getLength()];
            int[] IntRangeI = IntRange[i];
            for (int j=0; j<getLength(); j++) {
              IntRangeI[j] = index[j] + MISSING4 + 1;
            }
            break;
          default:
            throw new SetException("FlatField.packValues: bad RangeMode");
        }
      }
      clearMissing();
    }

  }

  public byte[][] grabBytes() {
    return ByteRange;
  }

  /** unpack an array of doubles from field sample values according to the
      RangeSet-s; returns a copy */
  public double[][] unpackValues() throws VisADException {
    return unpackValues(true);
  }


  /**
   * Unpacks an array of doubles from field sample values according to the
   * RangeSet-s; returns a copy if copy == true.
   *
   * @param copy            Whether or not to return a copy.
   * @throws SetException   if an element of {@link #RangeMode} contains an
   *                        unknown value.
   * @throws VisADException if {@link Set#indexToValue(int [])} on a range set
   *                        fails.
   */
    protected double[][] unpackValues(boolean copy)
        throws SetException, VisADException {
        double[][] range;
        synchronized (DoubleRange) {
            if (isMissing()) {
                range = new double[TupleDimension][getLength()];
                for (int i=0; i<TupleDimension; i++) {
                    for (int j=0; j<getLength(); j++) {
                        range[i][j] = Double.NaN;
                    }
                }
                return range;
            }

            int[] index;
            range = new double[TupleDimension][];
            double[][] range0;
            double[] rangeI;
            for (int i=0; i<TupleDimension; i++) {
                switch (RangeMode[i]) {
                case DOUBLE:
                    if (copy) {
                        range[i] = new double[getLength()];
                        rangeI = range[i];
                        double[] DoubleRangeI = DoubleRange[i];
                        System.arraycopy(DoubleRangeI, 0, rangeI, 0, getLength());
                        // for (int j=0; j<getLength(); j++) rangeI[j] = DoubleRangeI[j];
                    }
                    else {
                        range[i] = DoubleRange[i];
                    }
                    break;
                case FLOAT:
                    range[i] = new double[getLength()];
                    rangeI = range[i];
                    float[] FloatRangeI = FloatRange[i];
                    for (int j=0; j<getLength(); j++) {
                        rangeI[j] = (double) FloatRangeI[j];
                    }
                    break;
                case BYTE:
                    index = new int[getLength()];
                    byte[] ByteRangeI = ByteRange[i];
                    for (int j=0; j<getLength(); j++) {
                        index[j] = ((int) ByteRangeI[j]) - MISSING1 - 1;
                    }
                    range0 = Set.floatToDouble(RangeSet[i].indexToValue(index));
                    range[i] = range0[0];
                    break;
                case SHORT:
                    index = new int[getLength()];
                    short[] ShortRangeI = ShortRange[i];
                    for (int j=0; j<getLength(); j++) {
                        index[j] = ((int) ShortRangeI[j]) - MISSING2 - 1;
                    }
                    range0 = Set.floatToDouble(RangeSet[i].indexToValue(index));
                    range[i] = range0[0];
                    break;
                case INT:
                    index = new int[getLength()];
                    int[] IntRangeI = IntRange[i];
                    for (int j=0; j<getLength(); j++) {
                        index[j] = ((int) IntRangeI[j]) - MISSING4 - 1;
                    }
                    range0 = Set.floatToDouble(RangeSet[i].indexToValue(index));
                    range[i] = range0[0];
                    break;
                default:
                    throw new SetException("FlatField.unpackValues: bad RangeMode");
                }
            }
        }
        return range;
    }


  /** unpack an array of floats from field sample values according to the
      RangeSet-s; returns a copy */
  public float[][] unpackFloats() throws VisADException {
      return unpackFloats (true);
  }

  /** unpack an array of floats from field sample values according to the
      RangeSet-s; returns a copy if copy == true */
 protected float[][] unpackFloats(boolean copy) throws VisADException {
    float[][] range;
    synchronized (DoubleRange) {
      if (isMissing()) {
        range = new float[TupleDimension][getLength()];
        for (int i=0; i<TupleDimension; i++) {
          for (int j=0; j<getLength(); j++) {
            range[i][j] = Float.NaN;
          }
        }
        return range;
      }
      int[] index;
      range = new float[TupleDimension][];
      float[][] range0;
      float[] rangeI;

      for (int i=0; i<TupleDimension; i++) {
        switch (RangeMode[i]) {
          case DOUBLE:
            range[i] = new float[getLength()];
            rangeI = range[i];
            double[] DoubleRangeI = DoubleRange[i];
            for (int j=0; j<getLength(); j++) {
              rangeI[j] = (float) DoubleRangeI[j];
            }
            break;
          case FLOAT:
            if (copy) {
              range[i] = new float[getLength()];
              System.arraycopy (FloatRange[i], 0, range[i], 0, getLength());
            }  else {
              range[i] = FloatRange[i];
            }
            break;
          case BYTE:
              index = new int[getLength()];
              byte[] ByteRangeI = ByteRange[i];
              for (int j=0; j<getLength(); j++) {
                  index[j] = ((int) ByteRangeI[j]) - MISSING1 - 1;
              }
              range0 = RangeSet[i].indexToValue(index);
              range[i] = range0[0];
              break;
          case SHORT:
            index = new int[getLength()];
            short[] ShortRangeI = ShortRange[i];
            for (int j=0; j<getLength(); j++) {
              index[j] = ((int) ShortRangeI[j]) - MISSING2 - 1;
            }
            range0 = RangeSet[i].indexToValue(index);
            range[i] = range0[0];
            break;
          case INT:
            index = new int[getLength()];
            int[] IntRangeI = IntRange[i];
            for (int j=0; j<getLength(); j++) {
              index[j] = ((int) IntRangeI[j]) - MISSING4 - 1;
            }
            range0 = RangeSet[i].indexToValue(index);
            range[i] = range0[0];
            break;
          default:
            throw new SetException("FlatField.unpackFloats: bad RangeMode");
        }
      }
    }
    return range;
  }

  /**
   * Unpack one range component, makes a copy.
   *
   * @param comp	component index
   *
   * @return  array of the values
   * @throws VisADException  bad range mode
   */
  protected double[] unpackOneRangeComp(int comp) throws VisADException {
    return unpackOneRangeComp(comp, true);
  }

  /**
   * Unpack one range component.
   *
   * @param comp	component index
   * @param copy  true to make a copy
   *
   * @return  array of the values
   * @throws VisADException  bad range mode
   */
  protected double[] unpackOneRangeComp(int comp, boolean copy) throws VisADException {
    double[] range = null;
    synchronized (DoubleRange) {
      if (isMissing()) {
        range = new double[getLength()];
        for (int j=0; j<getLength(); j++) {
          range[j] = Double.NaN;
        }
        return range;
      }
      int[] index;
      double[][] range0;
      for (int i=0; i<TupleDimension; i++) {
        switch (RangeMode[comp]) {
          case DOUBLE:
            if (copy) {
              range = new double[getLength()];
              System.arraycopy (DoubleRange[comp], 0, range, 0, getLength());
            }  else {
              range = DoubleRange[comp];
            }
            break;
          case FLOAT:
            range = new double[getLength()];
            float[] FloatRangeI = FloatRange[comp];
            for (int j=0; j<getLength(); j++) {
              range[j] = (double) FloatRangeI[j];
            }
            break;
          case BYTE:
            index = new int[getLength()];
            byte[] ByteRangeI = ByteRange[comp];
            for (int j=0; j<getLength(); j++) {
              index[j] = ((int) ByteRangeI[j]) - MISSING1 - 1;
            }
            range0 = Set.floatToDouble(RangeSet[comp].indexToValue(index));
            range = range0[0];
            break;
          case SHORT:
            index = new int[getLength()];
            short[] ShortRangeI = ShortRange[comp];
            for (int j=0; j<getLength(); j++) {
              index[j] = ((int) ShortRangeI[j]) - MISSING2 - 1;
            }
            range0 = Set.floatToDouble(RangeSet[comp].indexToValue(index));
            range = range0[0];
            break;
          case INT:
            index = new int[getLength()];
            int[] IntRangeI = IntRange[comp];
            for (int j=0; j<getLength(); j++) {
              index[j] = ((int) IntRangeI[j]) - MISSING4 - 1;
            }
            range0 = Set.floatToDouble(RangeSet[comp].indexToValue(index));
            range = range0[0];
            break;
          default:
            throw new SetException("FlatField.unpackValues: bad RangeMode");
        }
      }
    }
    return range;
  }

  /**
   * Unpack one range component, makes a copy.
   *
   * @param comp	component index
   *
   * @return  array of the values
   * @throws VisADException  bad range mode
   */
  protected float[] unpackOneFloatRangeComp(int comp) throws VisADException {
    return unpackOneFloatRangeComp(comp, true);
  }

  /**
   * Unpack one range component.
   *
   * @param comp	component index
   * @param copy  true to make a copy
   *
   * @return  array of the values
   * @throws VisADException  bad range mode
   */
  protected float[] unpackOneFloatRangeComp(int comp, boolean copy) throws VisADException {
    float[] range = null;
    synchronized (FloatRange) {
      if (isMissing()) {
        range = new float[getLength()];
        for (int j=0; j<getLength(); j++) {
          range[j] = Float.NaN;
        }
        return range;
      }
      int[] index;
      float[][] range0;
      for (int i=0; i<TupleDimension; i++) {
        switch (RangeMode[comp]) {
          case DOUBLE:
            range = new float[getLength()];
            double[] DoubleRangeI = DoubleRange[comp];
            for (int j=0; j<getLength(); j++) {
              range[j] = (float) DoubleRangeI[j];
            }
            break;
          case FLOAT:
            if (copy) {
              range = new float[getLength()];
              System.arraycopy (FloatRange[comp], 0, range, 0, getLength());
            }  else {
              range = FloatRange[comp];
            }
            break;
          case BYTE:
            index = new int[getLength()];
            byte[] ByteRangeI = ByteRange[comp];
            for (int j=0; j<getLength(); j++) {
              index[j] = ((int) ByteRangeI[j]) - MISSING1 - 1;
            }
            range0 = RangeSet[comp].indexToValue(index);
            range = range0[0];
            break;
          case SHORT:
            index = new int[getLength()];
            short[] ShortRangeI = ShortRange[comp];
            for (int j=0; j<getLength(); j++) {
              index[j] = ((int) ShortRangeI[j]) - MISSING2 - 1;
            }
            range0 = RangeSet[comp].indexToValue(index);
            range = range0[0];
            break;
          case INT:
            index = new int[getLength()];
            int[] IntRangeI = IntRange[comp];
            for (int j=0; j<getLength(); j++) {
              index[j] = ((int) IntRangeI[j]) - MISSING4 - 1;
            }
            range0 = RangeSet[comp].indexToValue(index);
            range = range0[0];
            break;
          default:
            throw new SetException("FlatField.unpackValues: bad RangeMode");
        }
      }
    }
    return range;
  }

  /** 
   * Unpack the double value at the sample index.
   * @param s_index  sample index
   * @return array of values at that index
   * @throws VisADException  unable to unpack values
   */
  protected double[] unpackValues( int s_index ) throws VisADException {
    double[] range;
    synchronized (DoubleRange) {
      if (isMissing()) {
        range = new double[TupleDimension];
        for (int i=0; i<TupleDimension; i++) {
          range[i] = Double.NaN;
        }
        return range;
      }
      int[] index;
      range = new double[TupleDimension];
      double[][] range0;
      double[] rangeI;
      for (int i=0; i<TupleDimension; i++) {
        switch (RangeMode[i]) {
          case DOUBLE:
            range[i] = DoubleRange[i][s_index];
            break;
          case FLOAT:
            range[i] = (double) FloatRange[i][s_index];
            break;
          case BYTE:
            index = new int[1];
            byte[] ByteRangeI = ByteRange[i];
            index[0] = ((int) ByteRangeI[s_index]) - MISSING1 - 1;
            range0 = Set.floatToDouble(RangeSet[i].indexToValue(index));
            range[i] = range0[0][0];
            break;
          case SHORT:
            index = new int[1];
            short[] ShortRangeI = ShortRange[i];
            index[0] = ((int) ShortRangeI[s_index]) - MISSING2 - 1;
            range0 = Set.floatToDouble(RangeSet[i].indexToValue(index));
            range[i] = range0[0][0];
            break;
          case INT:
            index = new int[1];
            int[] IntRangeI = IntRange[i];
            index[0] = ((int) IntRangeI[s_index]) - MISSING4 - 1;
            range0 = Set.floatToDouble(RangeSet[i].indexToValue(index));
            range[i] = range0[0][0];
            break;
          default:
            throw new SetException("FlatField.unpackValues: bad RangeMode");
        }
      }
    }
    return range;
  }

  /** 
   * Unpack the floats at the sample index.
   * @param s_index  sample index
   * @return array of values at that index
   * @throws VisADException  unable to unpack floats
   */
  protected float[] unpackFloats( int s_index ) throws VisADException {
    float[] range;
    synchronized (FloatRange) {
      if (isMissing()) {
        range = new float[TupleDimension];
        for (int i=0; i<TupleDimension; i++) {
          range[i] = Float.NaN;
        }
        return range;
      }
      int[] index;
      range = new float[TupleDimension];
      float[][] range0;
      float[] rangeI;
      for (int i=0; i<TupleDimension; i++) {
        switch (RangeMode[i]) {
          case DOUBLE:
            range[i] = (float) DoubleRange[i][s_index];
            break;
          case FLOAT:
            range[i] = FloatRange[i][s_index];
            break;
          case BYTE:
            index = new int[1];
            byte[] ByteRangeI = ByteRange[i];
            index[0] = ((int) ByteRangeI[s_index]) - MISSING1 - 1;
            range0 = RangeSet[i].indexToValue(index);
            range[i] = range0[0][0];
            break;
          case SHORT:
            index = new int[1];
            short[] ShortRangeI = ShortRange[i];
            index[0] = ((int) ShortRangeI[s_index]) - MISSING2 - 1;
            range0 = RangeSet[i].indexToValue(index);
            range[i] = range0[0][0];
            break;
          case INT:
            index = new int[1];
            int[] IntRangeI = IntRange[i];
            index[0] = ((int) IntRangeI[s_index]) - MISSING4 - 1;
            range0 = RangeSet[i].indexToValue(index);
            range[i] = range0[0][0];
            break;
          default:
            throw new SetException("FlatField.unpackFloats: bad RangeMode");
        }
      }
    }
    return range;
  }

  /**
   * Returns the range values in their default units as floats.
   *
   * @return                    The range values in their default units
   *                            as determined by the {@link MathType} of
   *                            the range.  Element <code>[i][j]</code> is
   *                            the <code>j</code>th sample value of the
   *                            <code>i</code>th component of the range.
   * @throws VisADException     if a VisAD object couldn't be created.
   */
  public float[][] getFloats() throws VisADException {
    return getFloats(true);
  }

  /**
   * Returns the range values in their default units as floats.
   *
   * @param copy                Whether or not the returned array might be the
   *                            actual range array.  If <code>true</code>, then
   *                            the returned array will not be the actual range
   *                            array.
   * @return                    The range values in their default units
   *                            as determined by the {@link MathType} of
   *                            the range.  Element <code>[i][j]</code> is
   *                            the <code>j</code>th sample value of the
   *                            <code>i</code>th component of the range.
   * @throws VisADException     if a VisAD object couldn't be created.
   */
  public float[][] getFloats (boolean copy) throws VisADException {
      pr ("getFloats(" + copy + ")");
      float[][] values = unpackFloats(copy);

      Unit[] units_out =  ((FunctionType) Type).getFlatRange().getDefaultUnits();
      //Only converty the data if the output units not equal to the input units
      if (!Arrays.equals (units_out, RangeUnits)) {
          values =  Unit.convertTuple(values, RangeUnits, units_out);
      }
      return values;
  }

  /**
   * Returns the range values in their default units as doubles.
   *
   * @return                    The range values in their default units
   *                            as determined by the {@link MathType} of
   *                            the range.  Element <code>[i][j]</code> is
   *                            the <code>j</code>th sample value of the
   *                            <code>i</code>th component of the range.
   * @throws VisADException     if a VisAD object couldn't be created.
   */
  public double[][] getValues() throws VisADException {
    return getValues(true);
  }

  /**
   * Returns the range values in their default units as doubles.
   *
   * @param copy                Whether or not the returned array might be the
   *                            actual range array.  If <code>true</code>, then
   *                            the returned array will not be the actual range
   *                            array.
   * @return                    The range values in their default units
   *                            as determined by the {@link MathType} of
   *                            the range.  Element <code>[i][j]</code> is
   *                            the <code>j</code>th sample value of the
   *                            <code>i</code>th component of the range.
   * @throws VisADException     if a VisAD object couldn't be created.
   */
  public double[][] getValues(boolean copy) throws VisADException {
      pr ("getValues(" + copy + ")");
      double[][] values = unpackValues(copy);
      Unit[] units_out =
          ((FunctionType) Type).getFlatRange().getDefaultUnits();
      double[][]result =  Unit.convertTuple(values, RangeUnits, units_out);
      return result;
  }

  /** 
   * Get String values for Text components 
   * @return null (there are none for FlatFields) 
   * @throws VisADException     but doesn't happen
   * @throws RemoteException    but doesn't happen
   */
  public String[][] getStringValues()
         throws VisADException, RemoteException {
    return null;
  }

  /** 
   * Get values for 'Flat' components in default range Unit-s. 
   * @return                    The range values in their default units
   *                            as determined by the {@link MathType} of
   *                            the range.  Element <code>[i]</code> is
   *                            the value of the <code>i</code>th component 
   *                            of the flattened range.
   * @throws VisADException     if a VisAD object couldn't be created.
   */
  /*- TDR June 1998  */
  public double[] getValues( int s_index ) throws VisADException {
    double[] values = new double[TupleDimension];
    values = unpackValues( s_index );
    double[][] n_values = new double[TupleDimension][1];
    for ( int ii = 0; ii < TupleDimension; ii++ ) {
      n_values[ii][0] = values[ii];
    }
    Unit[] units_out =
      ((FunctionType) Type).getFlatRange().getDefaultUnits();
    double[][] r_values = Unit.convertTuple(n_values, RangeUnits, units_out);
    for ( int ii = 0; ii < values.length; ii++ ) {
      values[ii] = r_values[ii][0];
    }
    return values;
  }

  /** 
   * Get default range Unit-s for 'Flat' components.
   * @return array of the default Units for each of the RealTypes 
   *         components in the flattened range. 
   */
  public Unit[] getDefaultRangeUnits() {
    return ((FunctionType) Type).getFlatRange().getDefaultUnits();
  }

  /** 
   * Get the range value at the index-th sample.
   * FlatField does not override evaluate, but the correctness
   * of FlatField.evaluate depends on overriding getSample 
   * @return Data object (Real, RealTuple, or Tuple) corresponding to
   *         the range at the index-th sample.
   * @throws VisADException  problem getting data
   * @throws RemoteException problem getting data from remote object
   */
  public Data getSample(int index)
         throws VisADException, RemoteException {
    int[] inds;
    if (isMissing() || index < 0 || index >= getLength()) {
      return ((FunctionType) Type).getRange().missingData();
    }
    double[][] range = new double[TupleDimension][1];
    double[][] range1;
    synchronized (DoubleRange) {
      for (int i=0; i<TupleDimension; i++) {
        switch (RangeMode[i]) {
          case DOUBLE:
            range[i][0] = DoubleRange[i][index];
            break;
          case FLOAT:
            range[i][0] = (double) FloatRange[i][index];
            break;
          case BYTE:
            inds = new int[1];
            inds[0] = ((int) ByteRange[i][index]) - MISSING1 - 1;
            range1 = Set.floatToDouble(RangeSet[i].indexToValue(inds));
            range[i] = range1[0];
            break;
          case SHORT:
            inds = new int[1];
            inds[0] = ((int) ShortRange[i][index]) - MISSING2 - 1;
            range1 = Set.floatToDouble(RangeSet[i].indexToValue(inds));
            range[i] = range1[0];
            break;
          case INT:
            inds = new int[1];
            inds[0] = ((int) IntRange[i][index]) - MISSING4 - 1;
            range1 = Set.floatToDouble(RangeSet[i].indexToValue(inds));
            range[i] = range1[0];
            break;
          default:
            throw new SetException("FlatField.getSample: bad RangeMode");
        }
      }
    }

    MathType RangeType = ((FunctionType) Type).getRange();
    if (RangeType instanceof RealType) {
      return new Real((RealType) RangeType, range[0][0],
                      RangeUnits[0], RangeErrors[0]);
    }
    else if (RangeType instanceof RealTupleType) {
      Real[] reals = new Real[TupleDimension];
      for (int j=0; j<TupleDimension; j++) {
        MathType type = ((RealTupleType) RangeType).getComponent(j);
        reals[j] = new Real((RealType) type, range[j][0],
                            RangeUnits[j], RangeErrors[j]);
      }
      return new RealTuple((RealTupleType) RangeType, reals,
                           RangeCoordinateSystem);
    }
    else { // RangeType is a Flat TupleType
      int n = ((TupleType) RangeType).getDimension();
      int j = 0;
      Data[] datums = new Data[n];
      for (int i=0; i<n; i++) {
        MathType type = ((TupleType) RangeType).getComponent(i);
        if (type instanceof RealType) {
          datums[i] = new Real((RealType) type, range[j][0],
                               RangeUnits[j], RangeErrors[j]);
          j++;
        }
        else { // type instanceof RealTupleType
          int m = ((RealTupleType) type).getDimension();
          Real[] reals = new Real[m];
          for (int k=0; k<m; k++) {
            RealType ctype = (RealType) ((RealTupleType) type).getComponent(k);
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
   * A stub routine which simply invokes 
   * {@link #getSample(int) getSample} to override 
   * {@link FieldImpl#getSample(int, boolean) FieldImpl.getSample}
   * @param index index of requested range sample
   * @throws VisADException     if a VisAD object couldn't be created.
   * @throws RemoteException    if the Remote object couldn't be created.
   */
  public Data getSample(int index, boolean metadataOnly)
         throws VisADException, RemoteException {
    return getSample(index);
  }

  /** 
   * Set the range value at the index-th sample 
   * @param  index   index to set
   * @param  range   range value to set
   * @param  copy    flag to copy values - meaningless for FlatField
   * @throws VisADException     if range's MathType is incompatible or
   *                            some other error.
   * @throws RemoteException    if the Remote object couldn't be created.
   */
  public void setSample(int index, Data range, boolean copy)
         throws VisADException, RemoteException {
    setSample(index, range); // copy flag meaningless for FlatField
  }

  /** 
   * Set the range value at the index-th sample 
   * @param  index   index to set
   * @param  range   range value to set
   * @throws VisADException     if range's MathType is incompatible or
   *                            some other error.
   * @throws RemoteException    if the Remote object couldn't be created.
   */
  public void setSample(int index, Data range)
         throws VisADException, RemoteException {
      pr ("setSample");


    double[][] values;
    int[] indices;
    if (getDomainSet() == null) {
      throw new FieldException("Field.setSample: DomainSet undefined");
    }
    if (range != null &&
        !((FunctionType) Type).getRange().equals(range.getType())) {
      throw new TypeException("Field.setSample: sample range type " +
                              range.getType() +
                              " does not match expected type " +
                              ((FunctionType) Type).getRange());
    }
    if (index < 0 || index >= getLength()) return;

    // disect range into doubles
    double[] vals = new double[TupleDimension];
    // holder for errors of transformed values;
    ErrorEstimate[] errors_out = new ErrorEstimate[TupleDimension];
    if (range == null) {
      for (int j=0; j<TupleDimension; j++) {
        vals[j] = Double.NaN;
      }
    }
    else if (range instanceof Real) {
      vals[0] = ((Real) range).getValue();
      vals = Unit.transformUnits (
                        RangeUnits[0], errors_out,
                        ((Real) range).getUnit(), ((Real) range).getError(),
                        vals);
    }
    else if (range instanceof RealTuple) {
      double[][] value = new double[TupleDimension][1];
      for (int j=0; j<TupleDimension; j++) {
        value[j][0] = ((Real) ((RealTuple) range).getComponent(j)).getValue();
      }
      value = CoordinateSystem.transformCoordinates(
                        (RealTupleType) ((FunctionType) Type).getRange(),
                        RangeCoordinateSystem, RangeUnits, errors_out,
                        (RealTupleType) range.getType(),
                        ((RealTuple) range).getCoordinateSystem(),
                        ((RealTuple) range).getTupleUnits(),
                        ((RealTuple) range).getErrors(), value);
      for (int j=0; j<TupleDimension; j++) {
        vals[j] = value[j][0];
      }
    }
    else { // range is Flat Tuple
      MathType RangeType = ((FunctionType) Type).getRange();
      int n = ((TupleIface) range).getDimension();
      int j = 0;
      for (int i=0; i<n; i++) {
        Data component = ((TupleIface) range).getComponent(i);
        if (component instanceof Real) {
          double[] value = new double[1];
          value[0] = ((Real) component).getValue();
          ErrorEstimate[] sub_errors_out = new ErrorEstimate[1];
          value = Unit.transformUnits(
                            RangeUnits[0], sub_errors_out,
                            ((Real) component).getUnit(),
                            ((Real) component).getError(),
                            value);
          vals[j] = value[0];
          errors_out[j] = sub_errors_out[0];
          j++;
        }
        else {
          int m = ((RealTuple) component).getDimension();
          double[][] value = new double[m][1];
          Unit[] units_out = new Unit[m];
          ErrorEstimate[] sub_errors_out = ((RealTuple) component).getErrors();
          for (int k=0; k<m; k++) {
            value[k][0] =
              ((Real) ((RealTuple) component).getComponent(k)).getValue();
            units_out[k] = RangeUnits[j + k];
          }
          value = CoordinateSystem.transformCoordinates(
                        (RealTupleType) ((TupleType) RangeType).getComponent(i),
                        RangeCoordinateSystems[i], units_out, sub_errors_out,
                        (RealTupleType) component.getType(),
                        ((RealTuple) component).getCoordinateSystem(),
                        ((RealTuple) component).getTupleUnits(),
                        ((RealTuple) component).getErrors(), value);
          for (int k=0; k<m; k++) {
            vals[j] = value[k][0];
            errors_out[j] = sub_errors_out[k];
            j++;
          }
        }
      }
    }
    // now errors_out contains the transformed errors for the sample
    // in range - these may be mixed with RangeErrors
    // incs is counter for increase / decrease in NumberNotMissing
    int[] incs = new int[TupleDimension];

    synchronized (DoubleRange) {
      for (int i=0; i<TupleDimension; i++) {
        // test for missing
        incs[i] = (vals[i] != vals[i]) ? 0 : 1;
        switch (RangeMode[i]) {
          case DOUBLE:
            if (DoubleRange[i] == null) {
              DoubleRange[i] = new double[getLength()];
              for (int j=0; j<getLength(); j++) DoubleRange[i][j] = Double.NaN;
            }
            // test for missing
            incs[i] -= (DoubleRange[i][index] != DoubleRange[i][index]) ? 0 : 1;
            DoubleRange[i][index] = vals[i];
            break;
          case FLOAT:
            if (FloatRange[i] == null) {
              FloatRange[i] = new float[getLength()];
              for (int j=0; j<getLength(); j++) FloatRange[i][j] = Float.NaN;
            }
            // test for missing
            incs[i] -= (FloatRange[i][index] != FloatRange[i][index]) ? 0 : 1;
            FloatRange[i][index] = (float) vals[i];
            break;
          case BYTE:
            values = new double[1][1];
            values[0][0] = vals[i];
            indices = RangeSet[i].valueToIndex(Set.doubleToFloat(values));
            if (ByteRange[i] == null) {
              ByteRange[i] = new byte[getLength()];
              for (int j=0; j<getLength(); j++) ByteRange[i][j] = (byte) MISSING1;
            }
            incs[i] -= (ByteRange[i][index] == (byte) MISSING1) ? 0 : 1;
            ByteRange[i][index] = (byte) (indices[0] + MISSING1 + 1);
            break;
          case SHORT:
            values = new double[1][1];
            values[0][0] = vals[i];
            indices = RangeSet[i].valueToIndex(Set.doubleToFloat(values));
            if (ShortRange[i] == null) {
              ShortRange[i] = new short[getLength()];
              for (int j=0; j<getLength(); j++) ShortRange[i][j] = (short) MISSING2;
            }
            incs[i] -= (ShortRange[i][index] == (short) MISSING2) ? 0 : 1;
            ShortRange[i][index] = (short) (indices[0] + MISSING2 + 1);
            break;
          case INT:
            values = new double[1][1];
            values[0][0] = vals[i];
            indices = RangeSet[i].valueToIndex(Set.doubleToFloat(values));
            if (IntRange[i] == null) {
              IntRange[i] = new int[getLength()];
              for (int j=0; j<getLength(); j++) IntRange[i][j] = MISSING4;
            }
            incs[i] -= (IntRange[i][index] == (int) MISSING4) ? 0 : 1;
            IntRange[i][index] = indices[0] + MISSING4 + 1;
            break;
          default:
            throw new SetException("FlatField.setSample: bad RangeMode");
        }
      }
      synchronized (RangeErrors) {
        for (int i=0; i<TupleDimension; i++) {
          RangeErrors[i] = new ErrorEstimate(RangeErrors[i],
                               errors_out[i], vals[i], incs[i]);
        }
      }
    }
    clearMissing();
    notifyReferences();
  }

  /**
   * Sets various arrays of range values to missing.
   *
   * @throws VisADException if a {@link Set#getLength()} invocation on a range
   *                        set fails.
   */
  protected void nullRanges() throws VisADException {
    synchronized (DoubleRange) {
      // DoubleRange = new double[TupleDimension][];
      for (int i=0; i<TupleDimension; i++) DoubleRange[i] = null; // WLH 12 Jan 2001


      FloatRange = new float[TupleDimension][];
      LongRange = new long[TupleDimension][];
      IntRange = new int[TupleDimension][];
      ShortRange = new short[TupleDimension][];
      ByteRange = new byte[TupleDimension][];

      for (int i=0; i<TupleDimension; i++) {
        if (RangeSet[i] instanceof DoubleSet) {
            RangeMode[i] = DOUBLE;
        }
        else if (RangeSet[i] instanceof FloatSet) {
          RangeMode[i] = FLOAT;
        }
        else {
          int SetLength = RangeSet[i].getLength();
          if (SetLength < 256) {
            RangeMode[i] = BYTE;
          }
          else if (SetLength < 65536) {
            RangeMode[i] = SHORT;
          }
          else {
            RangeMode[i] = INT;
          }
        }
      }
    }
  }

  /** 
   * Test whether range values are missing 
   * @return  true if the range values have not been set
   */
  public boolean isMissing() {
    synchronized (DoubleRange) {
      return MissingFlag;
    }
  }

  /** 
   * Mark this FlatField as non-missing 
   */
  public void clearMissing() {
    synchronized (DoubleRange) {
      MissingFlag = false;
    }
  }

  /** 
   * Return new Field with value 'this op data'.
   * test for various relations between types of this and data;
   * note return type may not be FlatField,
   * in case data is a Field and this matches its range 
   * @param data  object to operate on
   * @param op  operation to perform (e.g. ADD, SUB, MULT)
   * @param new_type  MathType of new object
   * @param sampling_mode  sampling mode to use 
   *                       (e.g., NEAREST_NEIGHBOR, WEIGHTED_AVERAGE)
   * @param error_mode    error estimate mode (e.g., NO_ERROR, DEPENDENT,
   *                      independent)
   * @return new Field corresponding to the requested operation
   * @throws VisADException   couldn't create new VisAD object
   * @throws RemoteException  couldn't create new Remote object
   */
  /*- TDR May 1998
  public Data binary(Data data, int op, int sampling_mode, int error_mode)
              throws VisADException, RemoteException {
   */
  public Data binary(Data data, int op, MathType new_type,
                     int sampling_mode, int error_mode)
              throws VisADException, RemoteException {
    if ( new_type == null ) {
      throw new TypeException("binary: new_type may not be null");
    }



    if (data instanceof Field) {
      /*- TDR June  1998 */
      FunctionType data_type = (FunctionType) data.getType();
      if ((data_type.getRange()).equalsExceptName(Type)) {
        if ( !data_type.equalsExceptName(new_type)) {
          throw new TypeException("binary: new_type doesn't match return type" );
        }
      /*- end   */
        // this matches range type of data;
        // note invertOp to reverse order of operands
        /*- TDR June  1998
        return data.binary(this, invertOp(op), sampling_mode, error_mode);
        */
        return data.binary(this, invertOp(op), new_type, sampling_mode, error_mode);
      }
      else if (!Type.equalsExceptName(data.getType())) {
        throw new TypeException("FlatField.binary: types don't match");
      }
      /*- TDR June 1998 */
      if ( !Type.equalsExceptName(new_type) ) {
        throw new TypeException();
      }
      /*- end */
      if (((Field) data).isFlatField()) {
        // force (data instanceof FlatField) to be true
        data = data.local();
      }
      else {
        // this and data have same type, but this is Flat and data is not
        /*- TDR June  1998
        return convertToField().binary(data, op, sampling_mode, error_mode);
        */
        return convertToField().binary(data, op, new_type, sampling_mode, error_mode);
      }

      // use DoubleSet rather than RangeSet for intermediate computation results
      if (isMissing() || data.isMissing()) return new_type.missingData();

      // resample data if needed
      data = ((FlatField) data).resample(getDomainSet(), sampling_mode, error_mode);

      // get values from two FlatField's
      double[][] thatValuesD = null;
      double[][] thisValuesD = null;
      float[][]  thatValuesF = null;
      float[][]  thisValuesF = null;

      //Should this operation use doubles or floats?
      boolean binaryDouble = (shouldBeDouble () && ((FlatField)data).shouldBeDouble ());
      if (binaryDouble) {
          thatValuesD = ((FlatField) data).unpackValues ();
          thisValuesD = unpackValues();
      } else {
          thatValuesF = ((FlatField) data).unpackFloats ();
          thisValuesF = unpackFloats ();
      }


      // initialize for Unit and ErrorEstimate calculations
      Unit[][] temp_units = ((FlatField) data).getRangeUnits();
      Unit[] units_in = new Unit[temp_units.length];
      for (int i=0; i<temp_units.length; i++) {
        units_in[i] = temp_units[i][0];
      }
      ErrorEstimate[] errors_in = ((FlatField) data).getRangeErrors();
      // substitute arrays of nulls for null arrays
      if (units_in == null) units_in = new Unit[TupleDimension];
      if (errors_in == null) errors_in = new ErrorEstimate[TupleDimension];
      Unit[] units_out = new Unit[TupleDimension];
      ErrorEstimate[] errors_out = new ErrorEstimate[TupleDimension];

      // apply any range coordinate transformations
      if (((FunctionType) Type).getReal()) {
        // transformCoordinatesFreeUnits does not impose any
        // particular Units on the final value2
          CoordinateSystem[] cs = ((FlatField) data).getRangeCoordinateSystem();
          if (thatValuesD != null) {
              thatValuesD = CoordinateSystem.transformCoordinatesFreeUnits(
                   ((FunctionType) Type).getFlatRange(),
                   RangeCoordinateSystem, units_out, errors_out,
                   ((FunctionType) data.getType()).getFlatRange(),
                   cs[0], units_in, errors_in, thatValuesD);
          } 
          if (thatValuesF != null) {
              thatValuesF = CoordinateSystem.transformCoordinatesFreeUnits(
                   ((FunctionType) Type).getFlatRange(),
                   RangeCoordinateSystem, units_out, errors_out,
                   ((FunctionType) data.getType()).getFlatRange(),
                   cs[0], units_in, errors_in, thatValuesF);
          }

      }      else if (RangeCoordinateSystems != null) {
        TupleType rtype =
          (TupleType) ((FunctionType) Type).getRange();
        TupleType dtype =
          (TupleType) ((FunctionType) data.getType()).getRange();
        int n = rtype.getDimension();
        int j = 0;
        for (int i=0; i<n; i++) {
          MathType crtype = rtype.getComponent(i);
          MathType cdtype = dtype.getComponent(i);
          if (crtype instanceof RealTupleType) {
            int m = ((RealTupleType) crtype).getDimension();


            double[][] tmpValuesD = null;
            float [][] tmpValuesF = null;

            if (thatValuesD != null) {
                tmpValuesD = new double[m][];
            } 
            if (thatValuesF != null) {
                tmpValuesF = new float[m][];
            } 


            Unit[] sub_units_out = new Unit[m];
            Unit[] sub_units_in = new Unit[m];
            ErrorEstimate[] sub_errors_out = new ErrorEstimate[m];
            ErrorEstimate[] sub_errors_in = new ErrorEstimate[m]; 
           for (int k=0; k<m; k++) {
                if (tmpValuesD != null)
                    tmpValuesD[k] = thatValuesD[j + k];
                if (tmpValuesF!= null)
                    tmpValuesF[k] = thatValuesF[j + k];
              sub_units_in[k] = units_in[j + k];
              sub_errors_in[k] = errors_in[j + k];
            }
            CoordinateSystem[] cs = ((FlatField) data).getRangeCoordinateSystem(i);
            if (tmpValuesD != null) {
                tmpValuesD = CoordinateSystem.transformCoordinatesFreeUnits(
                       (RealTupleType) crtype, RangeCoordinateSystems[i],
                       sub_units_out, sub_errors_out,
                       (RealTupleType) cdtype,
                       cs[0], sub_units_in, sub_errors_in, tmpValuesD);
            } 
            if (tmpValuesF != null) {
                tmpValuesF = CoordinateSystem.transformCoordinatesFreeUnits(
                       (RealTupleType) crtype, RangeCoordinateSystems[i],
                       sub_units_out, sub_errors_out,
                       (RealTupleType) cdtype,
                       cs[0], sub_units_in, sub_errors_in, tmpValuesF);

            }
            for (int k=0; k<m; k++) {
                if (thatValuesD !=null)
                    thatValuesD[j + k] = tmpValuesD[k];
                if (thatValuesF !=null)
                    thatValuesF[j + k] = tmpValuesF[k];
              errors_out[j + k] = sub_errors_out[k];
              units_out[j + k] = sub_units_out[k];
            }
            j += m;
          }
          else {
            errors_out[j] = errors_in[j];
            units_out[j] = units_in[j];
            j++;
          }
        }
      }




      /*
       * Some variable renamings for clarity in the following:
       */
      Unit[]          thisUnits = (Unit[])RangeUnits.clone();
      Unit[]          thatUnits = units_out;



      ErrorEstimate[] thisErrs = (ErrorEstimate[])RangeErrors.clone();
      ErrorEstimate[] thatErrs = errors_out;
      /*
       * Default values. The following switch might set "outUnits".
       */
      Unit[]          outUnits = new Unit[TupleDimension];

      /*
       * Set the output values.  NOTE: The input array (thisValues) is also
       * used to store the output numeric values.
       */

      switch (op) {
        case ADD:
        case SUBTRACT:
        case INV_SUBTRACT:
        case MAX:
        case MIN:
          for (int j=0; j<TupleDimension; j++) {
            if (thisUnits[j] == null || thatUnits[j] == null) {
              outUnits[j] = null;
            }
            else if (thisUnits[j] == CommonUnit.promiscuous) {
              outUnits[j] = thatUnits[j].getAbsoluteUnit();
            }
            else if (thatUnits[j] == CommonUnit.promiscuous) {
              outUnits[j] = thisUnits[j].getAbsoluteUnit();
            }
            else {
              try {
                outUnits[j] = thisUnits[j].getAbsoluteUnit();
                boolean convertThis = !outUnits[j].equals(thisUnits[j]);
                boolean convertThat = !outUnits[j].equals(thatUnits[j]);

                if (convertThis) {
                    if (thisValuesD != null)
                        thisValuesD[j] = outUnits[j].toThis (thisValuesD[j], thisUnits[j]);
                    if (thisValuesF != null)
                        thisValuesF[j] = outUnits[j].toThis (thisValuesF[j], thisUnits[j]);
                }

                if (convertThat) {
                    if (thatValuesD!=null)
                        thatValuesD[j] = outUnits[j].toThis (thatValuesD[j], thatUnits[j]);
                    if (thatValuesF!=null)
                        thatValuesF[j] = outUnits[j].toThis (thatValuesF[j], thatUnits[j]);
                }

                if (error_mode != NO_ERRORS &&
                  thisErrs[j] != null && thatErrs[j] != null) {

                  if (convertThis) {
                    Unit        errUnit = thisErrs[j].getUnit();

                    if (errUnit == null)
                      errUnit = thisUnits[j];

                    double err = 0.5 * thisErrs[j].getErrorValue();
                    double mean = thisErrs[j].getMean();
                    double a = outUnits[j].toThis(mean + err, errUnit);
                    double b = outUnits[j].toThis(mean - err, errUnit);
                    mean = (a + b) / 2;
                    err = Math.abs(a - b);
                    thisErrs[j] = new ErrorEstimate(mean, err, outUnits[j]);
                  }

                  if (convertThat) {
                    Unit        errUnit = thatErrs[j].getUnit();

                    if (errUnit == null)
                      errUnit = thatUnits[j];

                    double err = 0.5 * thatErrs[j].getErrorValue();
                    double mean = thatErrs[j].getMean();
                    double a = outUnits[j].toThis(mean + err, errUnit);
                    double b = outUnits[j].toThis(mean - err, errUnit);
                    mean = (a + b) / 2;
                    err = Math.abs(a - b);
                    thatErrs[j] = new ErrorEstimate(mean, err, outUnits[j]);
                  }
                }
              }
              catch (UnitException e) {         // inconvertible units
                outUnits[j] = null;
              }
            }
            double[]thisLineD = null;
            double[]thatLineD = null;
            float[]thisLineF = null;
            float[]thatLineF = null;
            switch (op) {
              case ADD:
                  if (thisValuesD != null) {
                      double[]thisValuesLineD = thisValuesD[j];
                      double[]thatValuesLineD = thatValuesD[j];
                      for (int i=0; i<getLength(); i++) {
                          thisValuesLineD[i] += thatValuesLineD[i];
                      }
                  }
                  if (thisValuesF != null) {
                      float[]thisValuesLineF = thisValuesF[j];
                      float[]thatValuesLineF = thatValuesF[j];
                      for (int i=0; i<getLength(); i++) {
                          thisValuesLineF[i] += thatValuesLineF[i];
                      }
                  }

                break;
              case SUBTRACT:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++) 
                          thisValuesD[j][i] -= thatValuesD[j][i];
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++) 
                          thisValuesF[j][i] -= thatValuesF[j][i];
                break;
              case INV_SUBTRACT:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++) 
                          thisValuesD[j][i] = thatValuesD[j][i] - thisValuesD[j][i];
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++) 
                          thisValuesF[j][i] = thatValuesF[j][i] - thisValuesF[j][i];
                break;
              case MAX:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = Math.max(thisValuesD[j][i], thatValuesD[j][i]);
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = Math.max(thisValuesF[j][i], thatValuesF[j][i]);
                break;
              case MIN:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = Math.min (thisValuesD[j][i], thatValuesD[j][i]);
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = Math.min (thisValuesF[j][i], thatValuesF[j][i]);
                break;
            }
          }
          break;


        case MULTIPLY:
        case DIVIDE:
        case INV_DIVIDE:
          for (int j=0; j<TupleDimension; j++) {
            if (thisUnits[j] != null) {
              Unit absUnit = thisUnits[j].getAbsoluteUnit();
              if (!absUnit.equals(thisUnits[j])) {
                  if (thisValuesD != null)
                      thisValuesD[j] = absUnit.toThis (thisValuesD[j], thisUnits[j]);
                  if (thisValuesF != null)
                      thisValuesF[j] = absUnit.toThis (thisValuesF[j], thisUnits[j]);
                  thisUnits[j] = absUnit;
              }
            }
            if (thatUnits[j] != null) {
              Unit absUnit = thatUnits[j].getAbsoluteUnit();
              if (!absUnit.equals(thatUnits[j])) {
                  if (thatValuesD != null)
                      thatValuesD[j] = absUnit.toThis(thatValuesD[j], thatUnits[j]);
                  if (thatValuesF != null)
                      thatValuesF[j] = absUnit.toThis(thatValuesF[j], thatUnits[j]);
                  thatUnits[j] = absUnit;
              }
            }
            if (thisUnits[j] == null || thatUnits[j] == null) {
              outUnits[j] = null;
            }
            else {
              switch(op) {
                case MULTIPLY:
                  outUnits[j] = 
                    thisUnits[j].equals(CommonUnit.promiscuous)
                      ? thatUnits[j]
                      : thatUnits[j].equals(CommonUnit.promiscuous)
                        ? thisUnits[j]
                        : thisUnits[j].multiply(thatUnits[j]);
                  break;
                case DIVIDE:
                  outUnits[j] =
                    thatUnits[j].equals(CommonUnit.promiscuous)
                      ? thisUnits[j]
                      : thisUnits[j].divide(thatUnits[j]);
                  break;
                case INV_DIVIDE:
                  outUnits[j] = 
                    thisUnits[j].equals(CommonUnit.promiscuous)
                      ? thatUnits[j]
                      : thatUnits[j].divide(thisUnits[j]);
                  break;
              }
            }
            switch(op) {
              case MULTIPLY:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] *= thatValuesD[j][i];
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] *= thatValuesF[j][i];
                break;
              case DIVIDE:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] /= thatValuesD[j][i];
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] /= thatValuesF[j][i];
                break;
              case INV_DIVIDE:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = thatValuesD[j][i] / thisValuesD[j][i];
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = thatValuesF[j][i] / thisValuesF[j][i];
                break;
            }
          }
          break;

        case POW:
          for (int j=0; j<TupleDimension; j++) {
            if (thisUnits[j] != null) {
              Unit absUnit = thisUnits[j].getAbsoluteUnit();
              if (!absUnit.equals(thisUnits[j])) {
                  if (thisValuesD != null)
                      thisValuesD[j] = absUnit.toThis(thisValuesD[j], thisUnits[j]);
                  if (thisValuesF != null)
                      thisValuesF[j] = absUnit.toThis(thisValuesF[j], thisUnits[j]);
                  thisUnits[j] = absUnit;
              }
            }
            if (thatUnits[j] != null && 
                !CommonUnit.promiscuous.equals(thatUnits[j])) {
                Unit absUnit = thatUnits[j].getAbsoluteUnit();
                if (!absUnit.equals(thatUnits[j])) {
                    if (thatValuesD != null)
                        thatValuesD[j] = absUnit.toThis(thatValuesD[j], thatUnits[j]);
                    if (thatValuesF != null)
                        thatValuesF[j] = absUnit.toThis(thatValuesF[j], thatUnits[j]);
                    thatUnits[j] = absUnit;
              }
            }
            if (thisUnits[j] != null && (
                thisUnits[j].equals(CommonUnit.promiscuous) ||
                thisUnits[j].equals(CommonUnit.dimensionless))) {
              outUnits[j] = thisUnits[j];
            }
            else {
              outUnits[j] = null;
            }
            if (thisValuesD != null)
                for (int i=0; i<getLength(); i++)
                    thisValuesD[j][i] = (double)(Math.pow(thisValuesD[j][i], thatValuesD[j][i]));
            if (thisValuesF != null)
                for (int i=0; i<getLength(); i++)
                    thisValuesF[j][i] = (float)(Math.pow(thisValuesF[j][i], thatValuesF[j][i]));
          }
          break;

        case INV_POW:
          for (int j=0; j<TupleDimension; j++) {
            if (thatUnits[j] != null) {
              Unit absUnit = thatUnits[j].getAbsoluteUnit();
              if (!absUnit.equals(thatUnits[j])) {
                  if (thatValuesD != null)
                      thatValuesD[j] = absUnit.toThis(thatValuesD[j], thatUnits[j]);
                  if (thatValuesF != null)
                      thatValuesF[j] = absUnit.toThis(thatValuesF[j], thatUnits[j]);
                  thatUnits[j] = absUnit;
              }
            }
            if (thisUnits[j] != null && 
                !CommonUnit.promiscuous.equals(thisUnits[j])) {
              Unit absUnit = thisUnits[j].getAbsoluteUnit();
              if (!absUnit.equals(thisUnits[j])) {
                  if (thisValuesD != null)
                      thisValuesD[j] = absUnit.toThis(thisValuesD[j], thisUnits[j]);
                  if (thisValuesF != null)
                      thisValuesF[j] = absUnit.toThis(thisValuesF[j], thisUnits[j]);
                  thisUnits[j] = absUnit;
              }
            }
            if (thatUnits[j] != null && (
                thatUnits[j].equals(CommonUnit.promiscuous) ||
                thatUnits[j].equals(CommonUnit.dimensionless))) {
              outUnits[j] = thatUnits[j];
            }
            else {
              outUnits[j] = null;
            }
            if (thisValuesD != null)
                for (int i=0; i<getLength(); i++)
                    thisValuesD[j][i] = (double)(Math.pow(thatValuesD[j][i], thisValuesD[j][i]));
            if (thisValuesF != null)
                for (int i=0; i<getLength(); i++)
                    thisValuesF[j][i] = (float)(Math.pow(thatValuesF[j][i], thisValuesF[j][i]));
          }
          break;

        case ATAN2:
        case ATAN2_DEGREES:
        case INV_ATAN2:
        case INV_ATAN2_DEGREES:
        case REMAINDER:
        case INV_REMAINDER:
          for (int j=0; j<TupleDimension; j++) {
            if (thisUnits[j] != null && thatUnits[j] != null) {
              Unit absUnit = thisUnits[j].getAbsoluteUnit();
              if (!absUnit.equals(thisUnits[j])) {
                  if (thisValuesD != null)
                      thisValuesD[j] = absUnit.toThis(thisValuesD[j], thisUnits[j]);
                  if (thisValuesF != null)
                      thisValuesF[j] = absUnit.toThis(thisValuesF[j], thisUnits[j]);
                  thisUnits[j] = absUnit;
              }
              if (!absUnit.equals(thatUnits[j])) {
                  if (thatValuesD != null)
                      thatValuesD[j] = absUnit.toThis(thatValuesD[j], thatUnits[j]);
                  if (thatValuesF != null)
                      thatValuesF[j] = absUnit.toThis(thatValuesF[j], thatUnits[j]);
                  thatUnits[j] = absUnit;
              }
            }
            switch(op) {
              case ATAN2:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = (double)(Math.atan2(thisValuesD[j][i], thatValuesD[j][i]));
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = (float)(Math.atan2(thisValuesF[j][i], thatValuesF[j][i]));
                  outUnits[j] = CommonUnit.radian;
                  break;
              case ATAN2_DEGREES:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = (double)(Data.RADIANS_TO_DEGREES * Math.atan2(thisValuesD[j][i], thatValuesD[j][i]));
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = (float)(Data.RADIANS_TO_DEGREES * Math.atan2(thisValuesF[j][i], thatValuesF[j][i]));
                outUnits[j] = CommonUnit.degree;
                break;
              case INV_ATAN2:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = (double)(Math.atan2(thatValuesD[j][i], thisValuesD[j][i]));
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = (float)(Math.atan2(thatValuesF[j][i], thisValuesF[j][i]));
                  outUnits[j] = CommonUnit.radian;
                break;
              case INV_ATAN2_DEGREES:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = (double)(Data.RADIANS_TO_DEGREES * Math.atan2(thatValuesD[j][i], thisValuesD[j][i]));
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = (float)(Data.RADIANS_TO_DEGREES * Math.atan2(thatValuesF[j][i], thisValuesF[j][i]));

                  outUnits[j] = CommonUnit.degree;
                  break;
              case REMAINDER:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] %= thatValuesD[j][i];
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] %= thatValuesF[j][i];
                outUnits[j] = thisUnits[j];
                break;
              case INV_REMAINDER:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = thatValuesD[j][i] % thisValuesD[j][i];
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = thatValuesF[j][i] % thisValuesF[j][i];
                outUnits[j] = thatUnits[j];
                break;
            }
          }
          break;

        default:
          throw new ArithmeticException("FlatField.binary: illegal operation");
      }

      /*
       * Compute ErrorEstimate-s for the result.
       */
      ErrorEstimate[] outErrs = new ErrorEstimate[TupleDimension];
      for (int j=0; j<TupleDimension; j++) {
        if (error_mode == NO_ERRORS ||
            thisErrs[j] == null || thatErrs[j] == null) {
          outErrs[j] = null;
        }
        else {
            if (thisValuesD != null)
                outErrs[j] =
                    new ErrorEstimate(thisValuesD[j], outUnits[j], op, thisErrs[j],
                                      thatErrs[j], error_mode);
            if (thisValuesF != null)
                outErrs[j] =
                    new ErrorEstimate(thisValuesF[j], outUnits[j], op, thisErrs[j],
                                      thatErrs[j], error_mode);
        }
      }

      // create a FlatField for return
      /*- TDR June  1998
      FlatField new_field = cloneDouble(units_out, errors_out);
      */

      FlatField new_field=null;
      if (thisValuesD!=null)
          new_field = cloneDouble (new_type, outUnits, outErrs, thisValuesD);
      if (thisValuesF!=null)
          new_field = cloneFloat (new_type, outUnits, outErrs, thisValuesF);
      new_field.clearMissing();
      return new_field;
    }
    else if (data instanceof Real || data instanceof RealTuple ||
             (data instanceof TupleIface &&
              ((TupleType) data.getType()).getFlat())) {
      MathType RangeType = ((FunctionType) Type).getRange();
      /*- TDR July 1998
      if (!RangeType.equalsExceptName(data.getType())) {
        throw new TypeException("FlatField.binary: types don't match");
      }
      */
      /*- TDR June 1998 */
      if ( !Type.equalsExceptName(new_type)) {
        throw new TypeException("binary: new_type doesn't match return type");
      }
      /*- end */

      // use DoubleSet rather than RangeSet for intermediate computation results
      if (isMissing() || data.isMissing()) return new_type.missingData();


      // get data values and possibly apply coordinate transform
      double[][] vals = new double[TupleDimension][1];

      Unit[] units_out = new Unit[TupleDimension];
      ErrorEstimate[] errors_out = new ErrorEstimate[TupleDimension];


      if (data instanceof Real) {
        // no need for Unit conversion - just pass Unit into binary ops
        for (int j=0; j<TupleDimension; j++) {
          vals[j][0] = ((Real) data).getValue();
          units_out[j] = ((Real) data).getUnit();
          errors_out[j] = ((Real) data).getError();
        }
      }
      else if (data instanceof RealTuple) {
        for (int j=0; j<TupleDimension; j++) {
          vals[j][0] = ((Real) ((RealTuple) data).getComponent(j)).getValue();
        }
        vals = CoordinateSystem.transformCoordinatesFreeUnits(
                        ((FunctionType) Type).getFlatRange(),
                        RangeCoordinateSystem, units_out, errors_out,
                        (RealTupleType) data.getType(),
                        ((RealTuple) data).getCoordinateSystem(),
                        ((RealTuple) data).getTupleUnits(),
                        ((RealTuple) data).getErrors(), vals);
      }
      else { // (data instanceof TupleIface && !(data instanceof RealTuple))
        int n = ((TupleIface) data).getDimension();
        int j = 0;
        for (int i=0; i<n; i++) {
          Data component = ((TupleIface) data).getComponent(i);
          if (component instanceof Real) {
            // no need for Unit conversion - just pass Unit into binary ops
            vals[j][0] = ((Real) component).getValue();
            units_out[j] = ((Real) component).getUnit();
            errors_out[j] = ((Real) component).getError();
            j++;
          }
          else { // (component instanceof RealTuple)
            int m = ((TupleIface) component).getDimension();
            double[][] tvals = new double[m][1];
            Unit[] sub_units_out = new Unit[m];
            ErrorEstimate[] sub_errors_out = new ErrorEstimate[m];
            for (int k=0; k<m; k++) {
              tvals[k][0] =
                ((Real) ((TupleIface) component).getComponent(k)).getValue();
            }
            tvals = CoordinateSystem.transformCoordinatesFreeUnits(
                        (RealTupleType) ((TupleType) RangeType).getComponent(i),
                        RangeCoordinateSystems[i], sub_units_out, sub_errors_out,
                        (RealTupleType) component.getType(),
                        ((RealTuple) component).getCoordinateSystem(),
                        ((RealTuple) component).getTupleUnits(),
                        ((RealTuple) component).getErrors(), tvals);
            for (int k=0; k<m; k++) {
              vals[j + k][0] = tvals[k][0];
              units_out[j + k] = sub_units_out[k];
              errors_out[j + k] = sub_errors_out[k];
            }
            j += m;
          }
        }
      }

      /*
       * Some variable renamings for clarity in the following:
       */
      Unit[]          thisUnits = (Unit[])RangeUnits.clone();
      Unit[]          thatUnits = units_out;


      boolean binaryDouble = shouldBeDouble ();
      double[][]      thisValuesD = null;
      float [][]      thisValuesF = null;
      if (binaryDouble) {
          thisValuesD = unpackValues ();
      } else {
          thisValuesF = unpackFloats ();
      }


      double[]        thatValues = new double[vals.length];
      ErrorEstimate[] thisErrs = (ErrorEstimate[])RangeErrors.clone();
      ErrorEstimate[] thatErrs = errors_out;
      /*
       * Default (null) values. The following switch might set "outUnits".
       */
      Unit[]          outUnits = new Unit[TupleDimension];

      for (int j = 0; j < vals.length; j++)
          thatValues[j] = vals[j][0];

      /*
       * Set the output values.  NOTE: The input array (thisValuesD) is also
       * used to store the output numeric values.
       */
      switch (op) {
      case ADD:
      case SUBTRACT:
      case INV_SUBTRACT:
      case MAX:
      case MIN:
          for (int j=0; j<TupleDimension; j++) {
              if (thisUnits[j] == null || thatUnits[j] == null) {
                  outUnits[j] = null;
              }
              else if (thisUnits[j] == CommonUnit.promiscuous) {
                  outUnits[j] = thatUnits[j].getAbsoluteUnit();
              }
              else if (thatUnits[j] == CommonUnit.promiscuous) {
                  outUnits[j] = thisUnits[j].getAbsoluteUnit();
              }
              else {
                  try {
                      outUnits[j] = thisUnits[j].getAbsoluteUnit();
                      boolean convertThis = !outUnits[j].equals(thisUnits[j]);
                      boolean convertThat = !outUnits[j].equals(thatUnits[j]);

                      if (convertThis)
                          if (thisValuesD != null)
                              thisValuesD[j] = outUnits[j].toThis(thisValuesD[j], thisUnits[j]);
                          if (thisValuesF != null)
                              thisValuesF[j] = outUnits[j].toThis(thisValuesF[j], thisUnits[j]);

                      if (convertThat)
                          thatValues[j] =
                              outUnits[j].toThis(thatValues[j], thatUnits[j]);

                      if (error_mode != NO_ERRORS &&
                          thisErrs[j] != null && thatErrs[j] != null) {

                          if (convertThis) {
                              Unit      errUnit = thisErrs[j].getUnit();

                              if (errUnit == null)
                                  errUnit = thisUnits[j];

                              double err = 0.5 * thisErrs[j].getErrorValue();
                              double mean = thisErrs[j].getMean();
                              double a = outUnits[j].toThis(mean + err, errUnit);
                              double b = outUnits[j].toThis(mean - err, errUnit);
                              mean = (a + b) / 2;
                              err = Math.abs(a - b);
                              thisErrs[j] = new ErrorEstimate(mean, err, outUnits[j]);
                          }

                          if (convertThat) {
                              Unit      errUnit = thatErrs[j].getUnit();

                              if (errUnit == null)
                                  errUnit = thatUnits[j];

                              double err = 0.5 * thatErrs[j].getErrorValue();
                              double mean = thatErrs[j].getMean();
                              double a = outUnits[j].toThis(mean + err, errUnit);
                              double b = outUnits[j].toThis(mean - err, errUnit);
                              mean = (a + b) / 2;
                              err = Math.abs(a - b);
                              thatErrs[j] = new ErrorEstimate(mean, err, outUnits[j]);
                          }
                      }
                  }
                  catch (UnitException e) {             // inconvertible units
                      outUnits[j] = null;
                  }
              }
              switch (op) {
              case ADD:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] += thatValues[j];
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] += thatValues[j];
                  break;
              case SUBTRACT:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] -= thatValues[j];
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] -= thatValues[j];
                  break;
              case INV_SUBTRACT:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = (double)(thatValues[j] - thisValuesD[j][i]);
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = (float)(thatValues[j] - thisValuesF[j][i]);
                  break;
              case MAX:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = (double)(Math.max(thisValuesD[j][i], thatValues[j]));
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = (float)(Math.max(thisValuesF[j][i], thatValues[j]));
                  break;
              case MIN:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = (double)(Math.min(thisValuesD[j][i], thatValues[j]));
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = (float)(Math.min(thisValuesF[j][i], thatValues[j]));
                  break;
              }
          }
          break;

      case MULTIPLY:
      case DIVIDE:
      case INV_DIVIDE:
          for (int j=0; j<TupleDimension; j++) {
              if (thisUnits[j] != null) {
                  Unit absUnit = thisUnits[j].getAbsoluteUnit();
                  if (!absUnit.equals(thisUnits[j])) {
                      if (thisValuesD != null) thisValuesD[j] = absUnit.toThis(thisValuesD[j], thisUnits[j]);
                      if (thisValuesF != null) thisValuesF[j] = absUnit.toThis(thisValuesF[j], thisUnits[j]);
                      thisUnits[j] = absUnit;
                  }
              }
              if (thatUnits[j] != null) {
                  Unit absUnit = thatUnits[j].getAbsoluteUnit();
                  if (!absUnit.equals(thatUnits[j])) {
                      thatValues[j] = absUnit.toThis(thatValues[j], thatUnits[j]);
                      thatUnits[j] = absUnit;
                  }
              }
              if (thisUnits[j] == null || thatUnits[j] == null) {
                  outUnits[j] = null;
              }
              else {
                  switch(op) {
                  case MULTIPLY:
                      outUnits[j] =
                          thisUnits[j].equals(CommonUnit.promiscuous)
                          ? thatUnits[j]
                          : thatUnits[j].equals(CommonUnit.promiscuous)
                          ? thisUnits[j]
                          : thisUnits[j].multiply(thatUnits[j]);
                      break;
                  case DIVIDE:
                      outUnits[j] =
                          thatUnits[j].equals(CommonUnit.promiscuous)
                          ? thisUnits[j]
                          : thisUnits[j].divide(thatUnits[j]);
                      break;
                  case INV_DIVIDE:
                      outUnits[j] =
                          thisUnits[j].equals(CommonUnit.promiscuous)
                          ? thatUnits[j]
                          : thatUnits[j].divide(thisUnits[j]);
                      break;
                  }
              }
              switch(op) {
              case MULTIPLY:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] *= thatValues[j];
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] *= thatValues[j];
                  break;
              case DIVIDE:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] /= thatValues[j];
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] /= thatValues[j];
                  break;
              case INV_DIVIDE:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = (double)(thatValues[j] / thisValuesD[j][i]);
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = (float)(thatValues[j] / thisValuesF[j][i]);
                  break;
              }
          }
          break;

      case POW:
          for (int j=0; j<TupleDimension; j++) {
              if (thisUnits[j] != null) {
                  Unit absUnit = thisUnits[j].getAbsoluteUnit();
                  if (!absUnit.equals(thisUnits[j])) {
                      if (thisValuesD != null) thisValuesD[j] = absUnit.toThis(thisValuesD[j], thisUnits[j]);
                      if (thisValuesF != null) thisValuesF[j] = absUnit.toThis(thisValuesF[j], thisUnits[j]);
                      thisUnits[j] = absUnit;
                  }
              }
              if (thatUnits[j] != null && 
                  !CommonUnit.promiscuous.equals(thatUnits[j])) {
                  Unit absUnit = thatUnits[j].getAbsoluteUnit();
                  if (!absUnit.equals(thatUnits[j])) {
                      thatValues[j] = absUnit.toThis(thatValues[j], thatUnits[j]);
                      thatUnits[j] = absUnit;
                  }
              }
              if (thisUnits[j] != null && (
                                           thisUnits[j].equals(CommonUnit.promiscuous) ||
                                           thisUnits[j].equals(CommonUnit.dimensionless))) {
                  outUnits[j] = thisUnits[j];
              }
              else {
                  outUnits[j] = null;
              }
              if (thisValuesD != null)
                  for (int i=0; i<getLength(); i++)
                      thisValuesD[j][i] = (double)(Math.pow(thisValuesD[j][i], thatValues[j]));
              if (thisValuesF != null)
                  for (int i=0; i<getLength(); i++)
                      thisValuesF[j][i] = (float)(Math.pow(thisValuesF[j][i], thatValues[j]));
          }
          break;

      case INV_POW:
          for (int j=0; j<TupleDimension; j++) {
              if (thatUnits[j] != null) {
                  Unit absUnit = thatUnits[j].getAbsoluteUnit();
                  if (!absUnit.equals(thatUnits[j])) {
                      thatValues[j] = absUnit.toThis(thatValues[j], thatUnits[j]);
                      thatUnits[j] = absUnit;
                  }
              }
              if (thisUnits[j] != null && 
                  !CommonUnit.promiscuous.equals(thisUnits[j])) {
                  Unit absUnit = thisUnits[j].getAbsoluteUnit();
                  if (!absUnit.equals(thisUnits[j])) {
                      if (thisValuesD != null)  thisValuesD[j] = absUnit.toThis(thisValuesD[j], thisUnits[j]);
                      if (thisValuesF != null)  thisValuesF[j] = absUnit.toThis(thisValuesF[j], thisUnits[j]);
                      thisUnits[j] = absUnit;
                  }
              }
              if (thatUnits[j] != null && (
                                           thatUnits[j].equals(CommonUnit.promiscuous) ||
                                           thatUnits[j].equals(CommonUnit.dimensionless))) {
                  outUnits[j] = thatUnits[j];
              }
              else {
                  outUnits[j] = null;
              }
              if (thisValuesD != null)
                  for (int i=0; i<getLength(); i++)
                      thisValuesD[j][i] = (double)(Math.pow(thatValues[j], thisValuesD[j][i]));
              if (thisValuesF != null)
                  for (int i=0; i<getLength(); i++)
                      thisValuesF[j][i] = (float)(Math.pow(thatValues[j], thisValuesF[j][i]));
          }
          break;

      case ATAN2:
      case ATAN2_DEGREES:
      case INV_ATAN2:
      case INV_ATAN2_DEGREES:
      case REMAINDER:
      case INV_REMAINDER:
          for (int j=0; j<TupleDimension; j++) {
              if (thisUnits[j] != null && thatUnits[j] != null) {
                  Unit absUnit = thisUnits[j].getAbsoluteUnit();
                  if (!absUnit.equals(thisUnits[j])) {
                      if (thisValuesD != null)
                          thisValuesD[j] = absUnit.toThis(thisValuesD[j], thisUnits[j]);
                      if (thisValuesF != null)
                          thisValuesF[j] = absUnit.toThis(thisValuesF[j], thisUnits[j]);
                      thisUnits[j] = absUnit;
                  }
                  if (!absUnit.equals(thatUnits[j])) {
                      thatValues[j] = absUnit.toThis(thatValues[j], thatUnits[j]);
                      thatUnits[j] = absUnit;
                  }
              }
              switch(op) {
              case ATAN2:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = (double)(Math.atan2(thisValuesD[j][i], thatValues[j]));
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = (float)(Math.atan2(thisValuesF[j][i], thatValues[j]));
                  outUnits[j] = CommonUnit.radian;
                  break;
              case ATAN2_DEGREES:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = (double)(Data.RADIANS_TO_DEGREES * 
                                                       Math.atan2(thisValuesD[j][i], thatValues[j]));
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = (float)(Data.RADIANS_TO_DEGREES * 
                                                       Math.atan2(thisValuesF[j][i], thatValues[j]));
                  outUnits[j] = CommonUnit.degree;
                  break;
              case INV_ATAN2:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = (double)(Math.atan2(thatValues[j], thisValuesD[j][i]));
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = (float)(Math.atan2(thatValues[j], thisValuesF[j][i]));
                  outUnits[j] = CommonUnit.radian;
                  break;
              case INV_ATAN2_DEGREES:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = (double)(Data.RADIANS_TO_DEGREES * 
                                                       Math.atan2(thatValues[j], thisValuesD[j][i]));
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = (float)(Data.RADIANS_TO_DEGREES * 
                                                       Math.atan2(thatValues[j], thisValuesF[j][i]));
                  outUnits[j] = CommonUnit.degree;
                  break;
              case REMAINDER:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] %= thatValues[j];
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] %= thatValues[j];
                  outUnits[j] = thisUnits[j];
                  break;
              case INV_REMAINDER:
                  if (thisValuesD != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesD[j][i] = (double)(thatValues[j] % thisValuesD[j][i]);
                  if (thisValuesF != null)
                      for (int i=0; i<getLength(); i++)
                          thisValuesF[j][i] = (float)(thatValues[j] % thisValuesF[j][i]);
                  outUnits[j] = thatUnits[j];
                  break;
              }
          }
          break;

      default:
          throw new ArithmeticException("FlatField.binary: illegal operation");
      }

      /*
       * Compute ErrorEstimate-s for the result.
       */
      ErrorEstimate[] outErrs = new ErrorEstimate[TupleDimension];
      for (int j=0; j<TupleDimension; j++) {
          if (error_mode == NO_ERRORS ||
              thisErrs[j] == null || thatErrs[j] == null) {
              outErrs[j] = null;
          }
          else {
              if (thisValuesD != null)
                  outErrs[j] = new ErrorEstimate(thisValuesD[j], outUnits[j], op, thisErrs[j],
                                                 thatErrs[j], error_mode);
              if (thisValuesF != null)
                  outErrs[j] = new ErrorEstimate(thisValuesF[j], outUnits[j], op, thisErrs[j],
                                                 thatErrs[j], error_mode);
          }
      }

      // create a FlatField for return
      /*- TDR June  1998
        FlatField new_field = cloneDouble(units_out, errors_out);
      */
      FlatField new_field = null;
      if (thisValuesD != null)
          new_field = cloneDouble( new_type, outUnits, outErrs, thisValuesD);
      if (thisValuesF != null)
          new_field = cloneFloat( new_type, outUnits, outErrs, thisValuesF);

      new_field.clearMissing();
      return new_field;
    }
    else {
        throw new TypeException("Field.binary");
    }
  }


    /** 
     * Return new FlatField with value 'this op'.
     * @param op  operation to perform (e.g., NOP, ABS, COS)
     * @param new_type  MathType of new object
     * @param sampling_mode  sampling mode to use 
     *                       (e.g., NEAREST_NEIGHBOR, WEIGHTED_AVERAGE)
     * @param error_mode    error estimate mode (e.g., NO_ERROR, DEPENDENT,
     *                      independent)
     * @return new FlatField corresponding to the requested operation
     * @throws VisADException   couldn't create new VisAD object
     * @throws RemoteException  couldn't create new Remote object
     */
    public Data unary (int op, MathType new_type, int sampling_mode, int error_mode)
        throws VisADException {
        // use DoubleSet rather than RangeSet for intermediate computation results
        //      if (isMissing()) return cloneDouble();
        if (isMissing()) {
            if (shouldBeDouble ()) {
                return cloneDouble(new_type, RangeUnits, RangeErrors);
            } else {
                return cloneFloat(new_type, RangeUnits, RangeErrors);
            }
        }


        /*- TDR July 1998  */
        if ( new_type == null ) {
            throw new TypeException("unary: new_type may not be null");
        }

        Unit[] units_out = new Unit[TupleDimension];

        /*
         * Ensure that the numeric values and units are in rational form, i.e. one
         * in which ratios of data values make sense (e.g. Temperature values in
         * kelvin rather than celsius).
         */
        Unit[]          units_in = (Unit[])RangeUnits.clone();
        ErrorEstimate[] errors_in = (ErrorEstimate[])RangeErrors.clone();;


        double[][] valuesD = null;
        float [][] valuesF = null;

        if (shouldBeDouble ()) {
            valuesD = unpackValues ();
            makeRational (valuesD, units_in, errors_in);
        } else {
            valuesF = unpackFloats ();
            makeRational (valuesF, units_in, errors_in);
        }


        int i, j; // loop indices
        double[] valuesDJ;
        float[] valuesFJ;

        switch (op) {
        case ABS:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = Math.abs(valuesDJ[i]);
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = Math.abs(valuesFJ[i]);
                    }
                }
                units_out[j] = units_in[j];
            }
            break;
        case ACOS:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = (double)(Math.acos(valuesDJ[i]));
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = (float)(Math.acos(valuesFJ[i]));
                    }
                }

                units_out[j] = CommonUnit.radian;
            }
            break;
        case ACOS_DEGREES:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = (double)(Data.RADIANS_TO_DEGREES * Math.acos(valuesDJ[i]));
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = (float)(Data.RADIANS_TO_DEGREES * Math.acos(valuesFJ[i]));
                    }
                }

                units_out[j] = CommonUnit.degree;
            }
            break;
        case ASIN:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = (double)(Math.asin(valuesDJ[i]));
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = (float)(Math.asin(valuesFJ[i]));
                    }
                }

                units_out[j] = CommonUnit.radian;
            }
            break;
        case ASIN_DEGREES:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = (double)(Data.RADIANS_TO_DEGREES * Math.asin(valuesDJ[i]));
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = (float)(Data.RADIANS_TO_DEGREES * Math.asin(valuesFJ[i]));
                    }
                }

                units_out[j] = CommonUnit.degree;
            }
            break;
        case ATAN:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = (double)(Math.atan(valuesDJ[i]));
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = (float)(Math.atan(valuesFJ[i]));
                    }
                }

                units_out[j] = CommonUnit.radian;
            }
            break;
        case ATAN_DEGREES:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = (double)(Data.RADIANS_TO_DEGREES * Math.atan(valuesDJ[i]));
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = (float)(Data.RADIANS_TO_DEGREES * Math.atan(valuesFJ[i]));
                    }
                }
                units_out[j] = CommonUnit.degree;
            }
            break;
        case CEIL:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = (double)(Math.ceil(valuesDJ[i]));
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = (float)(Math.ceil(valuesFJ[i]));
                    }
                }

                units_out[j] = units_in[j];
            }
            break;
        case COS:
            // do cos in degrees, unless unit is radians
            for (j=0; j<TupleDimension; j++) {
                if (CommonUnit.degree.equals(units_in[j])) {
                    if (valuesD != null) {
                        valuesDJ = valuesD[j];
                        for (i=0; i<getLength(); i++) {
                            valuesDJ[i] = (double)(Math.cos(Data.DEGREES_TO_RADIANS * valuesDJ[i]));
                        }
                    }
                    if (valuesF != null) {
                        valuesFJ = valuesF[j];
                        for (i=0; i<getLength(); i++) {
                            valuesFJ[i] = (float)(Math.cos(Data.DEGREES_TO_RADIANS * valuesFJ[i]));
                        }
                    }

                }
                else {
                    if (valuesD != null) {
                        valuesDJ = valuesD[j];
                        for (i=0; i<getLength(); i++) {
                            valuesDJ[i] = (double)(Math.cos(valuesDJ[i]));
                        }
                    }
                    if (valuesF != null) {
                        valuesFJ = valuesF[j];
                        for (i=0; i<getLength(); i++) {
                            valuesFJ[i] = (float)(Math.cos(valuesFJ[i]));
                        }
                    }

                }
                units_out[j] = CommonUnit.dimensionless.equals(units_in[j]) ? units_in[j] : null;
            }
            break;
        case COS_DEGREES:
            for (j=0; j<TupleDimension; j++) {


                if (CommonUnit.radian.equals(units_in[j])) {
                    if (valuesD != null) {
                        valuesDJ = valuesD[j];
                        for (i=0; i<getLength(); i++) {
                            valuesDJ[i] = (double)(Math.cos(valuesDJ[i]));
                        }
                    }
                    if (valuesF != null) {
                        valuesFJ = valuesF[j];
                        for (i=0; i<getLength(); i++) {
                            valuesFJ[i] = (float)(Math.cos(valuesFJ[i]));
                        }
                    }

                }
                else {
                    if (valuesD != null) {
                        valuesDJ = valuesD[j];
                        for (i=0; i<getLength(); i++) {
                            valuesDJ[i] = (double)(Math.cos(Data.DEGREES_TO_RADIANS * valuesDJ[i]));
                        }
                    }
                    if (valuesF != null) {
                        valuesFJ = valuesF[j];
                        for (i=0; i<getLength(); i++) {
                            valuesFJ[i] = (float)(Math.cos(Data.DEGREES_TO_RADIANS * valuesFJ[i]));
                        }
                    }

                }
                units_out[j] =
                    CommonUnit.dimensionless.equals(units_in[j]) ? units_in[j] : null;
            }
            break;
        case EXP:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = (double)(Math.exp(valuesDJ[i]));
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = (float)(Math.exp(valuesFJ[i]));
                    }
                }

                units_out[j] =
                    CommonUnit.dimensionless.equals(units_in[j]) ? units_in[j] : null;
            }
            break;
        case FLOOR:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = (double)(Math.floor(valuesDJ[i]));
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = (float)(Math.floor(valuesFJ[i]));
                    }
                }

                units_out[j] = units_in[j];
            }
            break;
        case LOG:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = (double)(Math.log(valuesDJ[i]));
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = (float)(Math.log(valuesFJ[i]));
                    }
                }

                units_out[j] =
                    CommonUnit.dimensionless.equals(units_in[j]) ? units_in[j] : null;
            }
            break;
        case RINT:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = (double)(Math.rint(valuesDJ[i]));
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = (float)(Math.rint(valuesFJ[i]));
                    }
                }

                units_out[j] = units_in[j];
            }
            break;
        case ROUND:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = Math.round(valuesDJ[i]);
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = Math.round(valuesFJ[i]);
                    }
                }

                units_out[j] = units_in[j];
            }
            break;
        case SIN:
            for (j=0; j<TupleDimension; j++) {
                if (CommonUnit.degree.equals(units_in[j])) {
                    if (valuesD != null) {
                        valuesDJ = valuesD[j];
                        for (i=0; i<getLength(); i++) {
                            valuesDJ[i] = (double)(Math.sin(Data.DEGREES_TO_RADIANS * valuesDJ[i]));
                        }
                    }
                    if (valuesF != null) {
                        valuesFJ = valuesF[j];
                        for (i=0; i<getLength(); i++) {
                            valuesFJ[i] = (float)(Math.sin(Data.DEGREES_TO_RADIANS * valuesFJ[i]));
                        }
                    }

                }
                else {
                    if (valuesD != null) {
                        valuesDJ = valuesD[j];
                        for (i=0; i<getLength(); i++) {
                            valuesDJ[i] = (double)(Math.sin(valuesDJ[i]));
                        }
                    }
                    if (valuesF != null) {
                        valuesFJ = valuesF[j];
                        for (i=0; i<getLength(); i++) {
                            valuesFJ[i] = (float)(Math.sin(valuesFJ[i]));
                        }
                    }

                }
                units_out[j] =
                    CommonUnit.dimensionless.equals(units_in[j]) ? units_in[j] : null;
            }
            break;
        case SIN_DEGREES:
            for (j=0; j<TupleDimension; j++) {

                if (CommonUnit.radian.equals(units_in[j])) {
                    if (valuesD != null) {
                        valuesDJ = valuesD[j];
                        for (i=0; i<getLength(); i++) {
                            valuesDJ[i] = (double)(Math.sin(valuesDJ[i]));
                        }
                    }
                    if (valuesF != null) {
                        valuesFJ = valuesF[j];
                        for (i=0; i<getLength(); i++) {
                            valuesFJ[i] = (float)(Math.sin(valuesFJ[i]));
                        }
                    }

                }
                else {
                    if (valuesD != null) {
                        valuesDJ = valuesD[j];
                        for (i=0; i<getLength(); i++) {
                            valuesDJ[i] = (double)(Math.sin(Data.DEGREES_TO_RADIANS * valuesDJ[i]));
                        }
                    }
                    if (valuesF != null) {
                        valuesFJ = valuesF[j];
                        for (i=0; i<getLength(); i++) {
                            valuesFJ[i] = (float)(Math.sin(Data.DEGREES_TO_RADIANS * valuesFJ[i]));
                        }
                    }

                }
                units_out[j] =
                    CommonUnit.dimensionless.equals(units_in[j]) ? units_in[j] : null;
            }
            break;
        case SQRT:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = (double)(Math.sqrt(valuesDJ[i]));
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = (float)(Math.sqrt(valuesFJ[i]));
                    }
                }

                // WLH 26 Nov 2001
                // units_out[j] =
                //   CommonUnit.dimensionless.equals(units_in[j]) ? units_in[j] : null;
                if (units_in[j] == null) {
                    units_out[j] = null;
                }
                else {
                    try {
                        units_out[j] = units_in[j].sqrt();
                    }
                    catch (IllegalArgumentException e) {
                        units_out[j] = null;
                    }
                    catch (UnitException e) {
                        units_out[j] = null;
                    }
                }
            }
            break;
        case TAN:
            for (j=0; j<TupleDimension; j++) {
                if (CommonUnit.degree.equals(units_in[j])) {
                    if (valuesD != null) {
                        valuesDJ = valuesD[j];
                        for (i=0; i<getLength(); i++) {
                            valuesDJ[i] = (double)(Math.tan(Data.DEGREES_TO_RADIANS * valuesDJ[i]));
                        }
                    }
                    if (valuesF != null) {
                        valuesFJ = valuesF[j];
                        for (i=0; i<getLength(); i++) {
                            valuesFJ[i] = (float)(Math.tan(Data.DEGREES_TO_RADIANS * valuesFJ[i]));
                        }
                    }

                }
                else {
                    if (valuesD != null) {
                        valuesDJ = valuesD[j];
                        for (i=0; i<getLength(); i++) {
                            valuesDJ[i] = (double)(Math.tan(valuesDJ[i]));
                        }
                    }
                    if (valuesF != null) {
                        valuesFJ = valuesF[j];
                        for (i=0; i<getLength(); i++) {
                            valuesFJ[i] = (float)(Math.tan(valuesFJ[i]));
                        }
                    }

                }
                units_out[j] =
                    CommonUnit.dimensionless.equals(units_in[j]) ? units_in[j] : null;
            }
            break;
        case TAN_DEGREES:
            for (j=0; j<TupleDimension; j++) {
                if (CommonUnit.radian.equals(units_in[j])) {
                    if (valuesD != null) {
                        valuesDJ = valuesD[j];
                        for (i=0; i<getLength(); i++) {
                            valuesDJ[i] = (double)(Math.tan(valuesDJ[i]));
                        }
                    }
                    if (valuesF != null) {
                        valuesFJ = valuesF[j];
                        for (i=0; i<getLength(); i++) {
                            valuesFJ[i] = (float)(Math.tan(valuesFJ[i]));
                        }
                    }

                }
                else {
                    if (valuesD != null) {
                        valuesDJ = valuesD[j];
                        for (i=0; i<getLength(); i++) {
                            valuesDJ[i] = (double)(Math.tan(Data.DEGREES_TO_RADIANS * valuesDJ[i]));
                        }
                    }
                    if (valuesF != null) {
                        valuesFJ = valuesF[j];
                        for (i=0; i<getLength(); i++) {
                            valuesFJ[i] = (float)(Math.tan(Data.DEGREES_TO_RADIANS * valuesFJ[i]));
                        }
                    }

                }
                units_out[j] =
                    CommonUnit.dimensionless.equals(units_in[j]) ? units_in[j] : null;
            }
            break;
        case NEGATE:
            for (j=0; j<TupleDimension; j++) {
                if (valuesD != null) {
                    valuesDJ = valuesD[j];
                    for (i=0; i<getLength(); i++) {
                        valuesDJ[i] = -valuesDJ[i];
                    }
                }
                if (valuesF != null) {
                    valuesFJ = valuesF[j];
                    for (i=0; i<getLength(); i++) {
                        valuesFJ[i] = -valuesFJ[i];
                    }
                }

                units_out[j] = units_in[j];
            }
            break;
        case NOP:
            for (j=0; j<TupleDimension; j++) {
                units_out[j] = units_in[j];
            }
            break;
        }

        // compute ErrorEstimates for result
        ErrorEstimate[] errors_out = new ErrorEstimate[TupleDimension];
        for (j=0; j<TupleDimension; j++) {
            if (error_mode == NO_ERRORS || errors_in[j] == null) {
                errors_out[j] = null;
            }
            else {
                if (valuesD != null)
                    errors_out[j] = new ErrorEstimate(valuesD[j], units_out[j], op,
                                                      errors_in[j], error_mode);
                if (valuesF != null)
                    errors_out[j] = new ErrorEstimate(valuesF[j], units_out[j], op,
                                                      errors_in[j], error_mode);
            }
        }

        // create a FlatField for return
        /*- TDR July 1998
          FlatField new_field = cloneDouble(units_out, errors_out);
        */

        FlatField newField=null;
        if (valuesD!=null) {
            newField = cloneDouble (new_type, units_out, errors_out, valuesD);
        }
        if (valuesF!=null) {
            newField = cloneFloat (new_type, units_out, errors_out, valuesF);
        }



        // new_field.DoubleRange = values;
        newField.clearMissing();
        return newField;
    }







    /**
   * Ensure that numeric values and units are in rational form, i.e. one in
   * which ratios of data values make sense (e.g. Temperature values in Kelvin
   * rather than Celsius).  Additionally, if an input unit is a non-unity
   * dimensionless unit, then the associated values will be converted to the
   * dimensionless unity unit (this conditions the values for operations like
   * exp() and pow()).  All conversions are done in-place.
   *
   * @param values              The numeric values.  On output,
   *                            <code>values[i]</code> will have been replaced
   *                            if necessary.
   * @param units               The units for the values and error estimates.
   *                            It's length shall be <code>values.length</code>.
   *                            On output, <code>units[i]</code> will have been
   *                            replaced with the absolute form of the input
   *                            unit if necessary.
   * @param errors              The error estimates.  It's length shall be
   *                            <code>values.length</code>.  On output,
   *                            <code>errors[i]</code> will have been replaced
   *                            if necessary.
   * @throws UnitException      Unit conversion error.
   */
    protected static void makeRational(double[][] values, Unit[] units,
                                       ErrorEstimate[] errors) throws UnitException
    {

        for (int j=0; j<values.length; j++) {
            Unit   inputUnit = units[j];
            if (inputUnit != null && !(inputUnit instanceof PromiscuousUnit)) {
                Unit    outputUnit = inputUnit.getAbsoluteUnit();
                if (Unit.canConvert (outputUnit, CommonUnit.dimensionless)) {
                    outputUnit = CommonUnit.dimensionless;
                }
                if (!outputUnit.equals(inputUnit)) {
                    values[j] = outputUnit.toThis (values[j], inputUnit);
                    if (errors[j] != null) {
                        errors[j] =
                            new ErrorEstimate(
                                              outputUnit.toThis(errors[j].getMean(), inputUnit),
                                              errors[j].getErrorValue(),
                                              outputUnit);
                    }
                    units[j] = outputUnit;
                }
            }
        }
    }


    protected static void makeRational(float[][] values, Unit[] units,
                                       ErrorEstimate[] errors) throws UnitException
    {

        for (int j=0; j<values.length; j++) {
            Unit        inputUnit = units[j];
            if (inputUnit != null && !(inputUnit instanceof PromiscuousUnit)) {
                Unit    outputUnit = inputUnit.getAbsoluteUnit();

                if (Unit.canConvert (outputUnit, CommonUnit.dimensionless)) {
                    outputUnit = CommonUnit.dimensionless;
                }
                if (!outputUnit.equals(inputUnit)) {
                    values[j] = outputUnit.toThis (values[j], inputUnit);
                    if (errors[j] != null) {
                        errors[j] =
                            new ErrorEstimate(
                                              outputUnit.toThis(errors[j].getMean(), inputUnit),
                                              errors[j].getErrorValue(),
                                              outputUnit);
                    }
                    units[j] = outputUnit;
                }
            }
        }
    }

  /** extract field from this[].component;
      this is OK, when we get around to it */
  public Field extract(int component)
         throws VisADException, RemoteException
  {
    return extract(component, true);
  }

  /** extract field from this[].component;
      this is OK, when we get around to it */
  public Field extract(int component, boolean copy)
         throws VisADException, RemoteException
  {
    Set domainSet = getDomainSet();
    int n_samples = domainSet.getLength();
    MathType rangeType = ((FunctionType)Type).getRange();
    MathType domainType = ((FunctionType)Type).getDomain();
    int n_comps;
    CoordinateSystem coord_sys;
    MathType m_type;
    int ii, jj, compSize;

    int[] flat_indices;

    if ( rangeType instanceof RealType )
    {
      if ( component != 0 ) {
        throw new VisADException("extract: component index must be zero");
      }
      else {
        return this;
      }
    }
    else
    {
      n_comps = ((TupleType)rangeType).getDimension();
      if ( (component+1) > n_comps ) {
        throw new VisADException("extract: component index too large");
      }
    }

    MathType new_range = ((TupleType)rangeType).getComponent( component );
    FunctionType new_type = new FunctionType( domainType, new_range);

    int cnt = 0;
    int t_cnt = 0;
    for ( ii = 0; ii < component; ii++ )
    {
      m_type = ((TupleType)rangeType).getComponent(ii);
      if ( m_type instanceof RealType )
      {
        cnt++;
      }
      else
      {
        cnt += ((RealTupleType)m_type).getDimension();
        t_cnt++;
      }
    }

    if ( new_range instanceof RealType )
    {
      compSize = 1;
      flat_indices = new int[compSize];
      flat_indices[0] = cnt;
      coord_sys = null;
    }
    else
    {
      compSize = ((RealTupleType)new_range).getDimension();
      flat_indices = new int[ compSize ];
      for ( jj = 0; jj < compSize; jj++ )
      {
        flat_indices[jj] = cnt++;
      }
      coord_sys = RangeCoordinateSystems[t_cnt];
    }

    ErrorEstimate[] errors_out = new ErrorEstimate[ compSize ];
    Unit[] units_out = new Unit[ compSize ];
    Set[] rangeSet_out = new Set[ compSize ];

    boolean needDoubles = false;
    for ( ii = 0; ii < compSize; ii++ )
    {
      units_out[ii] = RangeUnits[ flat_indices[ii] ];
      errors_out[ii] = RangeErrors[ flat_indices[ii] ];
      rangeSet_out[ii] = RangeSet[ flat_indices[ii] ];
      if (rangeSet_out[ii] instanceof DoubleSet) needDoubles = true;
    }

    FlatField new_field = new FlatField( new_type, domainSet, coord_sys, null,
                                         rangeSet_out, units_out );
    new_field.setRangeErrors( errors_out );

    if (needDoubles) {
   
        double[][] new_values = new double[ compSize ][ n_samples ];
    
        for ( ii = 0; ii < compSize; ii++ )
        {
          new_values[ii] = unpackOneRangeComp( flat_indices[ii], copy);
        }
        new_field.setSamples( new_values, false );
    } else {
        float[][] new_floats = new float[ compSize ][ n_samples ];
    
        for ( ii = 0; ii < compSize; ii++ )
        {
          new_floats[ii] = unpackOneFloatRangeComp( flat_indices[ii], copy);
        }
        new_field.setSamples( new_floats, false );
    }

    return new_field;
  }

  public Data derivative( RealTuple location, RealType[] d_partial_s,
                          MathType[] derivType_s, int error_mode )
         throws VisADException, RemoteException
  {
    int ii, jj, kk, dd, rr, tt, pp, ss;
    Set domainSet = getDomainSet();
    int domainDim = domainSet.getDimension();
    int manifoldDimension = domainSet.getManifoldDimension();
    int n_samples = domainSet.getLength();
    CoordinateSystem d_coordsys = getDomainCoordinateSystem();
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
    if ( location != null )
    {
      thisDomainFlag = false;
    }

    RealTupleType domainType = ((FunctionType)Type).getDomain();
    RealType[] r_comps = domainType.getRealComponents();
    RealType[] r_compsRange = (((FunctionType)Type).getFlatRange()).getRealComponents();
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
            u_vectors[ii][jj] = 1d;
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

    Unit[] D_units = null;
    Unit[][] R_units = getRangeUnits();
    String[][] derivNames = null;
    Unit[][] derivUnits = new Unit[ n_partials ][ TupleDimension ];
    MathType[] new_range = new MathType[ n_partials ];
    MathType[] new_types = new MathType[ n_partials ];

    if ( !transform ) {
      D_units = domainSet.getSetUnits();
    }
    else {
      D_units = d_reference.getDefaultUnits();
    }

  //- Create derivative Units array   -*
    for ( ii = 0; ii < n_partials; ii++ ) {
      for ( jj = 0; jj < TupleDimension; jj++ ) {
        if (( R_units == null)||( D_units == null ))
        {
          derivUnits[ii][jj] = null;
        }
        else if (( R_units[jj][0] == null )||( D_units[u_index[ii]] == null ))
        {
          derivUnits[ii][jj] = null;
        }
        else
        {
          derivUnits[ii][jj] = R_units[jj][0].divide( D_units[ u_index[ii] ] );
        }

      }
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

    double[][][] p_derivatives = null;
    DataImpl[] datums = new DataImpl[ n_partials ];
    ErrorEstimate[][] rangeErrors_out = new ErrorEstimate[ n_partials ][ TupleDimension ];
    if ( thisDomainFlag )
    {
      p_derivatives = new double[ n_partials ][ TupleDimension ][ n_samples ];
      for ( ii = 0; ii < n_partials; ii++ ) {
        datums[ii] = (DataImpl) cloneDouble( derivType_s[ii], derivUnits[ii], null );
      }
      if ( isMissing() ) {
        if ( n_partials == 1 )
        {
          return datums[0];
        }
        else
        {
          return new Tuple( datums );
        }
      }
    }
    else
    {
      p_derivatives = new double[ n_partials ][ TupleDimension ][ 1 ];
      if ( isMissing() ) {
        for ( ii = 0; ii < n_partials; ii++ ) {
          for ( jj = 0; jj < TupleDimension; jj++ ) {
            p_derivatives[ii][jj][0] = Double.NaN;
            rangeErrors_out[ii][jj] = null;
          }
        }
      }
    }

    if ( !isMissing() )  //- skip computations. if thisDomainFlag is also set, then
                         //- method would have already returned.
    {

  //- compute derivative-s, return FlatField or Tuple of FlatFields, or Data --*

    double[][] rangeValues = null;
    int[][] neighbors = null;
    int n_points;
    int n_index;
    int m_index;
    int index;
    float distance;
    float step;
    float f_sum;
    double d_sum;
    ErrorEstimate[] domainErrors = domainSet.getSetErrors();


  //- Handle LinearSet case separately for efficiency   -*
    if(( domainSet instanceof LinearSet )&&( thisDomainFlag ))
    {
      rangeValues = getValues();

      //- each partial derivative   -*
      for ( kk = 0; kk < n_partials; kk++ )
      {
        //- get manifoldDimension index for this real axis ( LinearSet only )  -*
        m_index = u_index[kk];

        //- get neigbors and separation along this axis   -*
        neighbors = domainSet.getNeighbors( m_index );
        step = (float) (((LinearSet)domainSet).getLinear1DComponent(m_index)).getStep();

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

          for ( rr = 0; rr < TupleDimension; rr++ )
          {
            p_derivatives[kk][rr][ii] = ( rangeValues[rr][ n_index ] -
                                          rangeValues[rr][ index ] )/distance;
          }
        }

        if ( error_mode != Data.NO_ERRORS )
        {
          for ( rr = 0; rr < TupleDimension; rr++ ) {
            double[] d_values = p_derivatives[kk][rr];
            rangeErrors_out[kk][rr] = new ErrorEstimate( d_values, derivUnits[kk][rr],
                                                     Data.DIVIDE, RangeErrors[rr],
                                                     domainErrors[m_index],
                                                     error_mode );
          }
        }
      }

      neighbors = null;
      rangeValues = null;
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
        rangeValues = getValues();
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
        double[][] new_rangeValues;
        double[] d_array;
        int[][] new_neighbors;
        n_samples = 1;
        FlatField new_field;
        float[][] new_Samples;
        float[][] evalSamples;
        float[][] org_Samples = domainSet.getSamples(false);

        new_field = (FlatField) resample( new SingletonSet(location, null, null, null ),
                                Data.WEIGHTED_AVERAGE, error_mode );

        evalSamples = (new_field.getDomainSet()).getSamples(false);
        neighbors = new int[n_samples][];
        weights = new float[n_samples][];

        ((SimpleSet)domainSet).valueToInterp( evalSamples, neighbors, weights );

        n_points = neighbors[0].length;
        new_neighbors = new int[n_samples][ n_points ];

        new_rangeValues = new double[ TupleDimension ][ n_points + 1 ];
        new_Samples = new float[ domainDim ][ n_points + 1 ];
        for ( ii = 0; ii < domainDim; ii++ )
        {
          new_Samples[ii][0] = evalSamples[ii][0];
        }
        d_array = new_field.unpackValues(0);
        for ( ii = 0; ii < TupleDimension; ii++ )
        {
          new_rangeValues[ii][0] = d_array[ii];
        }
        for ( kk = 0; kk < n_points; kk++ )
        {
          d_array = unpackValues( neighbors[0][kk] );
          new_neighbors[0][kk] = kk + 1;
          for ( ii = 0; ii < TupleDimension; ii++ )
          {
            new_rangeValues[ii][kk+1] = d_array[ii];
          }
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
        double[] distances = new double[ n_points ];
        double[][] derivatives = new double[ n_points ][ TupleDimension ];
        double[][] uvecPoint = new double[ n_points ][ domainDim ];

        //- neighbors loop   -*
        for ( kk = 0; kk < n_points; kk++ )
        {
          f_sum = 0;
          for ( dd = 0; dd < domainDim; dd++ ) {
            f_sum += (Samples[dd][ neighbors[ii][kk] ] - Samples[dd][ii])*
                     (Samples[dd][ neighbors[ii][kk] ] - Samples[dd][ii]);
            uvecPoint[kk][dd] = Samples[dd][ neighbors[ii][kk] ] - Samples[dd][ii];
          }

          distances[kk] = Math.sqrt((double)f_sum);

          for ( rr = 0; rr < TupleDimension; rr++ )
          {
            derivatives[kk][rr] = rangeValues[rr][ neighbors[ii][kk] ] -
                                  rangeValues[rr][ii];
          }
        }

        //- Interpolate for each partial derivative  -*
        for ( pp = 0; pp < n_partials; pp++ )
        {
          sum_weights = 0f;
          for ( kk = 0; kk < n_points; kk++ )
          {
            dotproduct = 0;

            for ( dd = 0; dd < domainDim; dd++ ) {
              dotproduct += uvecPoint[kk][dd]*u_vectors[pp][dd];
            }

            inv_dotproduct = 1f/dotproduct;

            if ( ! Float.isInfinite(inv_dotproduct) ) {
              sum_weights += weights[ii][kk];
              for ( rr = 0; rr < TupleDimension; rr++ ) {
                p_derivatives[pp][rr][ii] += derivatives[kk][rr]*inv_dotproduct*weights[ii][kk];
              }
            }
          }
          for ( rr = 0; rr < TupleDimension; rr++ ) {
            p_derivatives[pp][rr][ii] /= sum_weights;
          }
        }
      }

    }

    } //-not missing branch  -*


    double[][] s_samples = null;
    for ( pp = 0; pp < n_partials; pp++ )
    {
      s_samples = p_derivatives[pp];
      if ( thisDomainFlag )
      {
        ((FlatField)datums[pp]).setSamples( s_samples );
        ((FlatField)datums[pp]).setRangeErrors( rangeErrors_out[pp] );
      }
      else
      {
        MathType M_type = derivType_s[pp];

        if ( M_type instanceof RealType )
        {
          datums[pp] = new Real( (RealType) M_type, s_samples[0][0], derivUnits[pp][0],
                                 rangeErrors_out[pp][0]);
        }
        else if ( M_type instanceof RealTupleType )
        {
          Real[] reals = new Real[ TupleDimension ];
          for ( ii = 0; ii < TupleDimension; ii++ )
          {
            reals[ii] = new Real( (RealType)((TupleType)M_type).getComponent(ii),
                                  s_samples[ii][0], derivUnits[pp][ii], rangeErrors_out[pp][0] );
          }

          datums[pp] = new RealTuple( (RealTupleType) M_type, reals, RangeCoordinateSystem );
        }
        else if ( M_type instanceof TupleType )
        {
          ss = 0;
          tt = 0;
          int n_comps = ((TupleType)M_type).getDimension();
          Data[] s_datums = new Data[ n_comps ];
          for ( ii = 0; ii < n_comps; ii++ )
          {
            m_type = ((TupleType)M_type).getComponent(ii);
            if ( m_type instanceof RealType )
            {
              s_datums[ii] = new Real( (RealType)m_type, s_samples[tt][0], derivUnits[pp][ii],
                                        rangeErrors_out[pp][ii] );
              tt++;
            }
            else if ( m_type instanceof RealTupleType )
            {
              int n_compsI = ((TupleType)m_type).getDimension();
              Real[] reals = new Real[ n_compsI ];
              for ( jj = 0; jj < n_compsI; jj++ )
              {
                reals[jj] = new Real( (RealType)((TupleType)m_type).getComponent(jj),
                                s_samples[tt][0], derivUnits[pp][jj], rangeErrors_out[pp][jj] );
                tt++;
              }
              s_datums[ii] = new RealTuple( (RealTupleType) m_type, reals,
                                             RangeCoordinateSystems[ss] );
              ss++;
            }
          }

          datums[pp] = new Tuple( (TupleType)M_type, s_datums );
        }
      }
    }

    if ( n_partials == 1 )
    {
      return datums[0];
    }
    else
    {
      return new Tuple( datums );
    }
  }

  public Data derivative( int error_mode )
         throws VisADException, RemoteException
  {
    MathType[] derivType_s = null;
    RealType[] d_partial_s = null;
    return derivative( null, d_partial_s, derivType_s, error_mode );
  }

  public Data derivative( MathType[] derivType_s, int error_mode )
         throws VisADException, RemoteException
  {
    return derivative( null, null, derivType_s, error_mode );
  }

  public Function derivative( RealType d_partial, int error_mode )
         throws VisADException, RemoteException
  {
    MathType[] derivType_s = null;
    RealType[] d_partial_s = new RealType[1];
    d_partial_s[0] = d_partial;

    return (Function) derivative( null, d_partial_s, derivType_s, error_mode );
  }

  public Function derivative( RealType d_partial, MathType derivType, int error_mode )
         throws VisADException, RemoteException
  {
    MathType[] derivType_s = new MathType[1];
    RealType[] d_partial_s = new RealType[1];
    derivType_s[0] = derivType;
    d_partial_s[0] = d_partial;

    return (Function) derivative( null, d_partial_s, derivType_s, error_mode );
  }

  /**
   * Resamples the range to domain samples of a given set.  Resampling is either
   * by nearest neighbor or mulit-linear interpolation.  NOTE: This code is very
   * similar to FieldImpl.resample(Set,int,int).
   * @param set                 The set of points at which to resample this
   *                            field.
   * @param sampling_mode       Resampling mode: Data.NEAREST_NEIGHBOR or
   *                            Data.WEIGHTED_AVERAGE
   * @param error_mode          Error estimation mode: Data.DEPENDENT,
   *                            Data.INDEPENDENT, or Data.NO_ERRORS.
   * @return                    Field of resampled data.  RangeSet objects
   *                            in result are set to DoubleSet.  NOTE: May
   *                            return this (i.e., not a copy).
   */
  public Field resample (Set set, int sampling_mode, int error_mode)
         throws VisADException, RemoteException {

    /* NB: resampling is done in this method for a float domain.  If
     * you make changes to this method, make the corresponding changes
     * in resampleDouble.
     */
    Set domainSet = getDomainSet();

    visad.util.Trace.call1("FlatField.resample");
    if (domainSet.equals(set)) {
      // nothing to do
      visad.util.Trace.call2("FlatField.resample", "sampling set==domain set");
      return this;
    }

    int dim = domainSet.getDimension();
    if (dim != set.getDimension()) {
      throw new SetException("FlatField.resample: bad Set Dimension");
    }

    if (domainSet instanceof GriddedDoubleSet) {
      return resampleDouble(set, sampling_mode, error_mode);
    }

    CoordinateSystem coord_sys = set.getCoordinateSystem();
    Unit[] units = set.getSetUnits();
    ErrorEstimate[] errors =
      (error_mode == NO_ERRORS) ? new ErrorEstimate[dim] : set.getSetErrors();

    // create (initially missing) FlatField for return
    Set[] sets = new Set[TupleDimension];
    for (int i=0; i<TupleDimension; i++) {
      SetType set_type =
        new SetType(((FunctionType) Type).getFlatRange().getComponent(i));
      // WLH 26 Nov 2001
      // sets[i] = new DoubleSet(set_type);
      if (sampling_mode == Data.NEAREST_NEIGHBOR) {
        sets[i] = RangeSet[i];
      }
      else {
        sets[i] = new FloatSet(set_type);
      }
    }

    MathType range_type = ((FunctionType) Type).getRange();
    RealTupleType domain_type = ((SetType) set.getType()).getDomain();
    FunctionType func_type = new FunctionType(domain_type, range_type);
    FlatField new_field =
      new FlatField(func_type, set, RangeCoordinateSystem,
                    RangeCoordinateSystems, sets, RangeUnits);

    if (isMissing()) return new_field;

    ErrorEstimate[] range_errors_in =
      (error_mode == NO_ERRORS) ? new ErrorEstimate[TupleDimension] :
                                  RangeErrors;
    ErrorEstimate[] range_errors_out = range_errors_in;

    int i, j, k; // loop indices

    // create an array containing all indices of 'this'
    int length = set.getLength();
    int[] wedge = set.getWedge();

    // get values from wedge and possibly transform coordinates
    float[][] vals = set.indexToValue(wedge);
    // holder for sampling errors of transformed set; these are
    // only useful to help estmate range errors due to resampling
    ErrorEstimate[] errors_out = new ErrorEstimate[dim];
    float[][] oldvals = vals;
    visad.util.Trace.call1("FlatField.resample:transformCoords");
    try {  // this is only to throw a more meaningful message
      vals = CoordinateSystem.transformCoordinates(
                      ((FunctionType) Type).getDomain(), 
                      getDomainCoordinateSystem(),
                      getDomainUnits(), errors_out,
                      ((SetType) set.getType()).getDomain(), coord_sys,
                      units, errors, vals, false);
    } catch (UnitException ue) {
        throw new VisADException("Sampling set is not compatible with domain");
    }
    visad.util.Trace.call2("FlatField.resample:transformCoords");
    boolean coord_transform = !(vals == oldvals);

    // check whether we need to do sampling error calculations
    boolean sampling_errors = (error_mode != NO_ERRORS);
    if (sampling_errors) {
      for (i=0; i<dim; i++) {
        if (errors_out[i] == null) sampling_errors = false;
      }
      boolean any_range_error = false;
      for (i=0; i<TupleDimension; i++) {
        if (range_errors_in[i] != null) any_range_error = true;
      }
      if (!any_range_error) sampling_errors = false;
    }
    float[][] sampling_partials = new float[TupleDimension][dim];
    float[][] error_values = new float[1][1];
    if (sampling_errors) {
      error_values = Set.doubleToFloat(
                     ErrorEstimate.init_error_values(errors_out) );
    }

    // WLH 20 July 2000
    float[][] values = null;
    if (sampling_errors || (10 * length > getLength()) || 
        !shouldBeDouble() || sampling_mode == WEIGHTED_AVERAGE) {
      values = unpackFloats(false);
      // values = Set.doubleToFloat(unpackValues());
    }

    float[][] new_values = new float[TupleDimension][length];
    float[] new_valuesJ;
    float[] valuesJ;

    if (sampling_mode == WEIGHTED_AVERAGE && domainSet instanceof SimpleSet) {
      // resample by interpolation
      int[][] indices = new int[length][];
      float[][] coefs = new float[length][];
      ((SimpleSet) domainSet).valueToInterp(vals, indices, coefs);

/* DEBUG
// System.out.println("DomainSet = " + domainSet);
// System.out.println("set = " + set);

// for (i=0; i<length; i++) {
boolean pr = false;
int ii = length;
if (ii > 0) ii = 1;
if (indices == null) ii = 0;
for (i=0; i<ii; i++) {
  if (indices[i] != null && coefs[i] != null) {
    pr = true;
    if (i == 0) {
      System.out.println("DomainSet = " + domainSet);
      System.out.println("set = " + set);
    }
    System.out.println("vals[0][" + i + "] = " + vals[0][i] +
                      " vals[1][" + i + "] = " + vals[1][i]);
    String s = "indices[" + i + "] = ";
    for (j=0; j<indices[i].length; j++) s = s + indices[i][j] + " ";
    System.out.println(s);
    s = "coefs[" + i + "] = ";
    for (j=0; j<coefs[i].length; j++) s = s + coefs[i][j] + " ";
    System.out.println(s);
  }
}
*/
      // WLH 20 July 2000
      if (values != null) {
        for (j=0; j<TupleDimension; j++) {
          valuesJ = values[j];
          new_valuesJ = new_values[j];
          for (i=0; i<length; i++) {
            float v = Float.NaN;
            int len = indices[i] == null ? 0 : indices[i].length;
            if (len > 0) {
              v = valuesJ[indices[i][0]] * coefs[i][0];
              for (k=1; k<len; k++) {
                v += valuesJ[indices[i][k]] * coefs[i][k];
              }
              new_valuesJ[wedge[i]] = v;
            }
            else { // values outside grid
              new_valuesJ[wedge[i]] = Float.NaN;
            }
          }
        }
      }
      else {
        for (i=0; i<length; i++) {
          int len = indices[i] == null ? 0 : indices[i].length;
          if (len > 0) {
            float[][] xvals = new float[len][];
            for (k = 0; k<len; k++) {
              xvals[k] = unpackFloats(indices[i][k]);
            }
            for (j=0; j<TupleDimension; j++) {
              float v = xvals[0][j] * coefs[i][0];
              for (k=1; k<len; k++) v += xvals[k][j] * coefs[i][k];
              new_values[j][wedge[i]] = v;
            }
          }         
          else { // values outside grid
            for (j=0; j<TupleDimension; j++) {
              new_values[j][wedge[i]] = Float.NaN;
            }
          }
        }
      }
/* DEBUG
if (pr) System.out.println("value = " + new_values[0][0]);
*/

      if (sampling_errors) {
        int[][] error_indices = new int[2 * dim][];
        float[][] error_coefs = new float[2 * dim][];
        ((SimpleSet) domainSet).valueToInterp(error_values, error_indices,
                                              error_coefs);

        for (j=0; j<TupleDimension; j++) {
          for (i=0; i<dim; i++) {
            float a = Float.NaN;
            float b = Float.NaN;;
            int len = error_indices[2*i].length;
            if (len > 0) {
              a = values[j][error_indices[2*i][0]] * error_coefs[2*i][0];
              for (k=1; k<len; k++) {
                a += values[j][error_indices[2*i][k]] * error_coefs[2*i][k];
              }
            }
            len = error_indices[2*i+1].length;
            if (len > 0) {
              b = values[j][error_indices[2*i+1][0]] * error_coefs[2*i+1][0];
              for (k=1; k<len; k++) {
                b += values[j][error_indices[2*i+1][k]] * error_coefs[2*i+1][k];
              }
            }
            sampling_partials[j][i] = Math.abs(b - a);
          }
        }
      }

    }
    else { // NEAREST_NEIGHBOR or set is not SimpleSet
      // simple resampling
      int[] indices = domainSet.valueToIndex(vals);
/* DEBUG
// System.out.println("DomainSet = " + domainSet);
// System.out.println("set = " + set);

// for (i=0; i<length; i++) {
boolean pr = false;
int ii = length;
if (ii > 0) ii = 1;
if (indices == null) ii = 0;
for (i=0; i<ii; i++) {
  if (indices[i] >= 0) {
    pr = true;
    if (i == 0) {
      System.out.println("DomainSet = " + domainSet);
      System.out.println("set = " + set);
    }
    System.out.println("NEAREST_NEIGHBOR indices[" + i + "] = " + indices[i]);
  }
}
*/
      // WLH 20 July 2000
      if (values != null) {
        for (j=0; j<TupleDimension; j++) {
          valuesJ = values[j];
          new_valuesJ = new_values[j];
          for (i=0; i<length; i++) {
            new_valuesJ[wedge[i]] =
              ((indices[i] >= 0) ? valuesJ[indices[i]]: Float.NaN);
          }
        }
      }
      else {
        for (i=0; i<length; i++) {
          if (indices[i] >= 0) {
            float[] xvals = unpackFloats(indices[i]);
            for (j=0; j<TupleDimension; j++) {
              new_values[j][wedge[i]] = xvals[j];
            }
          }
          else { // values outside grid
            for (j=0; j<TupleDimension; j++) {
              new_values[j][wedge[i]] = Float.NaN;
            }
          }
        }
      }
/* DEBUG
if (pr) System.out.println("value = " + new_values[0][0]);
*/

      if (sampling_errors) {
        int[] error_indices = domainSet.valueToIndex(error_values);
        for (j=0; j<TupleDimension; j++) {
          for (i=0; i<dim; i++) {
            float a = (float) ((error_indices[2*i] >= 0) ?
                       values[j][error_indices[2*i]]: Double.NaN);
            float b = (float) ((error_indices[2*i+1] >= 0) ?
                       values[j][error_indices[2*i+1]]: Double.NaN);
            sampling_partials[j][i] = Math.abs(b - a);
          }
        }
      }

    }

    if (sampling_errors) {
      for (j=0; j<TupleDimension; j++) {
        if (range_errors_in[j] != null) {
          float error = (float) range_errors_in[j].getErrorValue();
          if (error_mode == Data.INDEPENDENT) {
            error = error * error;
            for (i=0; i<dim; i++) {
              error += sampling_partials[j][i] * sampling_partials[j][i];
            }
            error = (float) Math.sqrt(error);
          }
          else { // error_mode == Data.DEPENDENT
            for (i=0; i<dim; i++) {
              error += sampling_partials[j][i];
            }
          }
          range_errors_out[j] =
            new ErrorEstimate(new_values[j], error, RangeUnits[j]);
        }
      }
    }
    else if (error_mode != NO_ERRORS) {
      for (j=0; j<TupleDimension; j++) {
        if (range_errors_in[j] != null) {
          range_errors_out[j] =
            new ErrorEstimate(new_values[j], range_errors_in[j].getErrorValue(),
                              RangeUnits[j]);
        }
      }
    }

    if (coord_transform) {
      range_errors_in = range_errors_out;
      MathType Range = ((FunctionType) Type).getRange();
      if (Range instanceof RealVectorType) {
        new_values = ((RealVectorType) Range).transformVectors(
                      ((FunctionType) Type).getDomain(),
                      getDomainCoordinateSystem(), getDomainUnits(), errors_out,
                      ((SetType) set.getType()).getDomain(),
                      coord_sys, units, RangeCoordinateSystem,
                      range_errors_in, range_errors_out,
                      oldvals, vals, new_values);
      }
      else if (Range instanceof TupleType && !(Range instanceof RealTupleType)) {
        int offset = 0;
        int m = ((TupleType) Range).getDimension();
        for (j=0; j<m; j++) {
          MathType comp_type = ((TupleType) Range).getComponent(j);
          if (comp_type instanceof RealVectorType) {
            int mm = ((RealVectorType) comp_type).getDimension();
            float[][] comp_vals = new float[mm][];
            for (int jj=0; jj<mm; jj++) {
              comp_vals[jj] = new_values[offset + jj];
            }
            ErrorEstimate[] comp_errors_in = new ErrorEstimate[mm];
            for (int jj=0; jj<mm; jj++) {
              comp_errors_in[jj] = range_errors_in[offset + jj];
            }
            ErrorEstimate[] comp_errors_out = comp_errors_in;
            comp_vals = ((RealVectorType) comp_type).transformVectors(
                        ((FunctionType) Type).getDomain(),
                        getDomainCoordinateSystem(), getDomainUnits(), errors_out,
                        ((SetType) set.getType()).getDomain(), coord_sys, units,
                        RangeCoordinateSystems[j],
                        comp_errors_in, comp_errors_out,
                        oldvals, vals, comp_vals);
            for (int jj=0; jj<mm; jj++) {
              new_values[offset + jj] = comp_vals[jj];
            }
            for (int jj=0; jj<mm; jj++) {
              range_errors_out[offset + jj] = comp_errors_out[jj];
            }
          }
          if (comp_type instanceof RealType) {
            offset++;
          }
          else {
            offset += ((RealTupleType) comp_type).getDimension();
          }
        }
      }
    } // end if (coord_transform)
    new_field.packValues(new_values, false);
    // new_field.DoubleRange = new_values;
    new_field.setRangeErrors(range_errors_out);
    new_field.clearMissing();
    visad.util.Trace.call2("FlatField.resample");


    return new_field;
  }

  /**
   * Resamples the range to domain samples of a given double set.  Resampling is either
   * by nearest neighbor or mulit-linear interpolation.  NOTE: This code is very
   * similar to FieldImpl.resample(Set,int,int).
   * @param set                 The set of points at which to resample this
   *                            field.
   * @param sampling_mode       Resampling mode: Data.NEAREST_NEIGHBOR or
   *                            Data.WEIGHTED_AVERAGE
   * @param error_mode          Error estimation mode: Data.DEPENDENT,
   *                            Data.INDEPENDENT, or Data.NO_ERRORS.
   * @return                    Field of resampled data.  RangeSet objects
   *                            in result are set to DoubleSet.  NOTE: May
   *                            return this (i.e., not a copy).
   */
  public Field resampleDouble(Set set, int sampling_mode, int error_mode)
         throws VisADException, RemoteException {

    /* NB: resampling is done in this method for a double domain.  If
     * you make changes to this method, make the corresponding changes
     * in resample if necessary.
     */
    Set domainSet = getDomainSet();
    visad.util.Trace.call1("FlatField.resample");
    if (domainSet.equals(set)) {
      // nothing to do
      visad.util.Trace.call2("FlatField.resample", "sampling set==domain set");
      return this;
    }

    int dim = domainSet.getDimension();
    if (dim != set.getDimension()) {
      throw new SetException("FlatField.resample: bad Set Dimension");
    }

    if (!(domainSet instanceof GriddedDoubleSet)) {
      return resample(set, sampling_mode, error_mode);
    }

    CoordinateSystem coord_sys = set.getCoordinateSystem();
    Unit[] units = set.getSetUnits();
    ErrorEstimate[] errors =
      (error_mode == NO_ERRORS) ? new ErrorEstimate[dim] : set.getSetErrors();

    // create (initially missing) FlatField for return
    Set[] sets = new Set[TupleDimension];
    for (int i=0; i<TupleDimension; i++) {
      SetType set_type =
        new SetType(((FunctionType) Type).getFlatRange().getComponent(i));
      // WLH 26 Nov 2001
      // sets[i] = new DoubleSet(set_type);
      if (sampling_mode == Data.NEAREST_NEIGHBOR) {
        sets[i] = RangeSet[i];
      }
      else {
        sets[i] = new FloatSet(set_type);
      }
    }

    MathType range_type = ((FunctionType) Type).getRange();
    RealTupleType domain_type = ((SetType) set.getType()).getDomain();
    FunctionType func_type = new FunctionType(domain_type, range_type);
    FlatField new_field =
      new FlatField(func_type, set, RangeCoordinateSystem,
                    RangeCoordinateSystems, sets, RangeUnits);

    if (isMissing()) return new_field;

    ErrorEstimate[] range_errors_in =
      (error_mode == NO_ERRORS) ? new ErrorEstimate[TupleDimension] :
                                  RangeErrors;
    ErrorEstimate[] range_errors_out = range_errors_in;

    int i, j, k; // loop indices

    // create an array containing all indices of 'this'
    int length = set.getLength();
    int[] wedge = set.getWedge();

    // get values from wedge and possibly transform coordinates
    double[][] vals = set.indexToDouble(wedge);
    // holder for sampling errors of transformed set; these are
    // only useful to help estmate range errors due to resampling
    ErrorEstimate[] errors_out = new ErrorEstimate[dim];
    double[][] oldvals = vals;
    visad.util.Trace.call1("FlatField.resample:transformCoords");
    try {  // this is only to throw a more meaningful message
      vals = CoordinateSystem.transformCoordinates(
                      ((FunctionType) Type).getDomain(), 
                      getDomainCoordinateSystem(),
                      getDomainUnits(), errors_out,
                      ((SetType) set.getType()).getDomain(), coord_sys,
                      units, errors, vals, false);
    } catch (UnitException ue) {
        throw new VisADException("Sampling set is not compatible with domain");
    }
    visad.util.Trace.call2("FlatField.resample:transformCoords");
    boolean coord_transform = !(vals == oldvals);

    // check whether we need to do sampling error calculations
    boolean sampling_errors = (error_mode != NO_ERRORS);
    if (sampling_errors) {
      for (i=0; i<dim; i++) {
        if (errors_out[i] == null) sampling_errors = false;
      }
      boolean any_range_error = false;
      for (i=0; i<TupleDimension; i++) {
        if (range_errors_in[i] != null) any_range_error = true;
      }
      if (!any_range_error) sampling_errors = false;
    }
    double[][] sampling_partials = new double[TupleDimension][dim];
    double[][] error_values = new double[1][1];
    if (sampling_errors) {
      error_values = ErrorEstimate.init_error_values(errors_out);
    }

    // WLH 20 July 2000
    float[][] values = null;
    if (sampling_errors || (10 * length > getLength()) || 
        !shouldBeDouble() || sampling_mode == WEIGHTED_AVERAGE) {
      values = unpackFloats(false);
      // values = Set.doubleToFloat(unpackValues());
    }

    double[][] new_values = new double[TupleDimension][length];
    double[] new_valuesJ;
    float[] valuesJ;

    if (sampling_mode == WEIGHTED_AVERAGE) {
      // resample by interpolation
      int[][] indices = new int[length][];
      double[][] coefs = new double[length][];
      ((GriddedDoubleSet) domainSet).doubleToInterp(vals, indices, coefs);

/* DEBUG
// System.out.println("DomainSet = " + domainSet);
// System.out.println("set = " + set);

// for (i=0; i<length; i++) {
boolean pr = false;
int ii = length;
if (ii > 0) ii = 1;
if (indices == null) ii = 0;
for (i=0; i<ii; i++) {
  if (indices[i] != null && coefs[i] != null) {
    pr = true;
    if (i == 0) {
      System.out.println("DomainSet = " + domainSet);
      System.out.println("set = " + set);
    }
    System.out.println("vals[0][" + i + "] = " + vals[0][i] +
                      " vals[1][" + i + "] = " + vals[1][i]);
    String s = "indices[" + i + "] = ";
    for (j=0; j<indices[i].length; j++) s = s + indices[i][j] + " ";
    System.out.println(s);
    s = "coefs[" + i + "] = ";
    for (j=0; j<coefs[i].length; j++) s = s + coefs[i][j] + " ";
    System.out.println(s);
  }
}
*/
      // WLH 20 July 2000
      if (values != null) {
        for (j=0; j<TupleDimension; j++) {
          valuesJ = values[j];
          new_valuesJ = new_values[j];
          for (i=0; i<length; i++) {
            double v = Double.NaN;
            int len = indices[i] == null ? 0 : indices[i].length;
            if (len > 0) {
              v = valuesJ[indices[i][0]] * coefs[i][0];
              for (k=1; k<len; k++) {
                v += valuesJ[indices[i][k]] * coefs[i][k];
              }
              new_valuesJ[wedge[i]] = v;
            }
            else { // values outside grid
              new_valuesJ[wedge[i]] = Float.NaN;
            }
          }
        }
      }
      else {
        for (i=0; i<length; i++) {
          int len = indices[i] == null ? 0 : indices[i].length;
          if (len > 0) {
            float[][] xvals = new float[len][];
            for (k = 0; k<len; k++) {
              xvals[k] = unpackFloats(indices[i][k]);
            }
            for (j=0; j<TupleDimension; j++) {
              double v = xvals[0][j] * coefs[i][0];
              for (k=1; k<len; k++) v += xvals[k][j] * coefs[i][k];
              new_values[j][wedge[i]] = v;
            }
          }         
          else { // values outside grid
            for (j=0; j<TupleDimension; j++) {
              new_values[j][wedge[i]] = Float.NaN;
            }
          }
        }
      }
/* DEBUG
if (pr) System.out.println("value = " + new_values[0][0]);
*/

      if (sampling_errors) {
        int[][] error_indices = new int[2 * dim][];
        double[][] error_coefs = new double[2 * dim][];
        ((GriddedDoubleSet) domainSet).doubleToInterp(error_values, error_indices,
                                              error_coefs);

        for (j=0; j<TupleDimension; j++) {
          for (i=0; i<dim; i++) {
            double a = Double.NaN;
            double b = Double.NaN;;
            int len = error_indices[2*i].length;
            if (len > 0) {
              a = values[j][error_indices[2*i][0]] * error_coefs[2*i][0];
              for (k=1; k<len; k++) {
                a += values[j][error_indices[2*i][k]] * error_coefs[2*i][k];
              }
            }
            len = error_indices[2*i+1].length;
            if (len > 0) {
              b = values[j][error_indices[2*i+1][0]] * error_coefs[2*i+1][0];
              for (k=1; k<len; k++) {
                b += values[j][error_indices[2*i+1][k]] * error_coefs[2*i+1][k];
              }
            }
            sampling_partials[j][i] = Math.abs(b - a);
          }
        }
      }

    }
    else { // NEAREST_NEIGHBOR or set is not SimpleSet
      // simple resampling
      int[] indices = domainSet.doubleToIndex(vals);
/* DEBUG
// System.out.println("DomainSet = " + domainSet);
// System.out.println("set = " + set);

// for (i=0; i<length; i++) {
boolean pr = false;
int ii = length;
if (ii > 0) ii = 1;
if (indices == null) ii = 0;
for (i=0; i<ii; i++) {
  if (indices[i] >= 0) {
    pr = true;
    if (i == 0) {
      System.out.println("DomainSet = " + domainSet);
      System.out.println("set = " + set);
    }
    System.out.println("NEAREST_NEIGHBOR indices[" + i + "] = " + indices[i]);
  }
}
*/
      // WLH 20 July 2000
      if (values != null) {
        for (j=0; j<TupleDimension; j++) {
          valuesJ = values[j];
          new_valuesJ = new_values[j];
          for (i=0; i<length; i++) {
            new_valuesJ[wedge[i]] =
              ((indices[i] >= 0) ? valuesJ[indices[i]]: Float.NaN);
          }
        }
      }
      else {
        for (i=0; i<length; i++) {
          if (indices[i] >= 0) {
            float[] xvals = unpackFloats(indices[i]);
            for (j=0; j<TupleDimension; j++) {
              new_values[j][wedge[i]] = xvals[j];
            }
          }
          else { // values outside grid
            for (j=0; j<TupleDimension; j++) {
              new_values[j][wedge[i]] = Float.NaN;
            }
          }
        }
      }
/* DEBUG
if (pr) System.out.println("value = " + new_values[0][0]);
*/

      if (sampling_errors) {
        int[] error_indices = domainSet.doubleToIndex(error_values);
        for (j=0; j<TupleDimension; j++) {
          for (i=0; i<dim; i++) {
            float a = (float) ((error_indices[2*i] >= 0) ?
                       values[j][error_indices[2*i]]: Double.NaN);
            float b = (float) ((error_indices[2*i+1] >= 0) ?
                       values[j][error_indices[2*i+1]]: Double.NaN);
            sampling_partials[j][i] = Math.abs(b - a);
          }
        }
      }

    }

    if (sampling_errors) {
      for (j=0; j<TupleDimension; j++) {
        if (range_errors_in[j] != null) {
          float error = (float) range_errors_in[j].getErrorValue();
          if (error_mode == Data.INDEPENDENT) {
            error = error * error;
            for (i=0; i<dim; i++) {
              error += sampling_partials[j][i] * sampling_partials[j][i];
            }
            error = (float) Math.sqrt(error);
          }
          else { // error_mode == Data.DEPENDENT
            for (i=0; i<dim; i++) {
              error += sampling_partials[j][i];
            }
          }
          range_errors_out[j] =
            new ErrorEstimate(new_values[j], error, RangeUnits[j]);
        }
      }
    }
    else if (error_mode != NO_ERRORS) {
      for (j=0; j<TupleDimension; j++) {
        if (range_errors_in[j] != null) {
          range_errors_out[j] =
            new ErrorEstimate(new_values[j], range_errors_in[j].getErrorValue(),
                              RangeUnits[j]);
        }
      }
    }

    if (coord_transform) {
      range_errors_in = range_errors_out;
      MathType Range = ((FunctionType) Type).getRange();
      if (Range instanceof RealVectorType) {
        new_values = ((RealVectorType) Range).transformVectors(
                      ((FunctionType) Type).getDomain(),
                      getDomainCoordinateSystem(), getDomainUnits(), errors_out,
                      ((SetType) set.getType()).getDomain(),
                      coord_sys, units, RangeCoordinateSystem,
                      range_errors_in, range_errors_out,
                      oldvals, vals, new_values);
      }
      else if (Range instanceof TupleType && !(Range instanceof RealTupleType)) {
        int offset = 0;
        int m = ((TupleType) Range).getDimension();
        for (j=0; j<m; j++) {
          MathType comp_type = ((TupleType) Range).getComponent(j);
          if (comp_type instanceof RealVectorType) {
            int mm = ((RealVectorType) comp_type).getDimension();
            double[][] comp_vals = new double[mm][];
            for (int jj=0; jj<mm; jj++) {
              comp_vals[jj] = new_values[offset + jj];
            }
            ErrorEstimate[] comp_errors_in = new ErrorEstimate[mm];
            for (int jj=0; jj<mm; jj++) {
              comp_errors_in[jj] = range_errors_in[offset + jj];
            }
            ErrorEstimate[] comp_errors_out = comp_errors_in;
            comp_vals = ((RealVectorType) comp_type).transformVectors(
                        ((FunctionType) Type).getDomain(),
                        getDomainCoordinateSystem(), getDomainUnits(), errors_out,
                        ((SetType) set.getType()).getDomain(), coord_sys, units,
                        RangeCoordinateSystems[j],
                        comp_errors_in, comp_errors_out,
                        oldvals, vals, comp_vals);
            for (int jj=0; jj<mm; jj++) {
              new_values[offset + jj] = comp_vals[jj];
            }
            for (int jj=0; jj<mm; jj++) {
              range_errors_out[offset + jj] = comp_errors_out[jj];
            }
          }
          if (comp_type instanceof RealType) {
            offset++;
          }
          else {
            offset += ((RealTupleType) comp_type).getDimension();
          }
        }
      }
    } // end if (coord_transform)
    new_field.packValues(new_values, false);
    // new_field.DoubleRange = new_values;
    new_field.setRangeErrors(range_errors_out);
    new_field.clearMissing();
    visad.util.Trace.call2("FlatField.resample");


    return new_field;
  }

  /** convert this FlatField to a (non-Flat) FieldImpl */
  public Field convertToField() throws VisADException, RemoteException {
    Field new_field = new FieldImpl((FunctionType) Type, getDomainSet());
    if (isMissing()) return new_field;
    for (int i=0; i<getLength(); i++) {
      new_field.setSample(i, getSample(i));
    }
    return new_field;
  }

  public DataShadow computeRanges(ShadowType type, DataShadow shadow)
         throws VisADException {
    if (isMissing()) return shadow;

    ShadowRealTupleType domain_type = ((ShadowFunctionType) type).getDomain();
    int n = domain_type.getDimension();
    double[][] ranges = new double[2][n];
    // DomainSet.computeRanges handles Reference
    shadow = getDomainSet().computeRanges(domain_type, shadow, ranges, true);
    ShadowRealTupleType shad_ref;
    // skip range if no range components are mapped
    int[] indices = ((ShadowFunctionType) type).getRangeDisplayIndices();
    boolean any_mapped = false;
    for (int i=0; i<TupleDimension; i++) {
      if (indices[i] >= 0) any_mapped = true;
    }
    if (!any_mapped) return shadow;

    // check for any range coordinate systems
    boolean anyRangeRef = (RangeCoordinateSystem != null);
    if (RangeCoordinateSystems != null) {
      for (int i=0; i<RangeCoordinateSystems.length; i++) {
        anyRangeRef |= (RangeCoordinateSystems[i] != null);
      }
    }
    ranges = anyRangeRef ? new double[2][TupleDimension] : null;

    // get range values



    double[][] valuesD = null;
    float[][] valuesF = null;
    if (shouldBeDouble ()) {
        valuesD = unpackValues (false);
    } else {
        valuesF = unpackFloats (false);
    }


    for (int i=0; i<TupleDimension; i++) {
        double[] valuesDI = null;
        float [] valuesFI = null;
        if (valuesD !=null)
            valuesDI = valuesD[i];
        if (valuesF !=null)
            valuesFI = valuesF[i];
      int k = indices[i];
      if (k >= 0 || anyRangeRef) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        if (valuesDI!=null) {
            for (int j=0; j<getLength(); j++) {
                if (valuesDI[j] == valuesDI[j]) {
                    min = Math.min(min, valuesDI[j]);
                    max = Math.max(max, valuesDI[j]);
                }
            }
        }

        if (valuesFI!=null) {
            for (int j=0; j<getLength(); j++) {
                if (valuesFI[j] == valuesFI[j]) {
                    min = Math.min(min, valuesFI[j]);
                    max = Math.max(max, valuesFI[j]);
                }
            }
        }


        Unit dunit = ((RealType)
          ((FunctionType) Type).getFlatRange().getComponent(i)).getDefaultUnit();
        if (dunit != null && !dunit.equals(RangeUnits[i])) {
          min = dunit.toThis(min, RangeUnits[i]);
          max = dunit.toThis(max, RangeUnits[i]);
        }
        if (anyRangeRef) {
          ranges[0][i] = Math.min(ranges[0][i], min);
          ranges[1][i] = Math.max(ranges[1][i], max);
        }
        if (k >= 0 && k < shadow.ranges[0].length) {
          shadow.ranges[0][k] = Math.min(shadow.ranges[0][k], min);
          shadow.ranges[1][k] = Math.max(shadow.ranges[1][k], max);
        }
      }
    }
    if (RangeCoordinateSystem != null) {
      // computeRanges for Reference (relative to range) RealTypes
      ShadowRealTupleType range_type =
        (ShadowRealTupleType) ((ShadowFunctionType) type).getRange();
      shad_ref = range_type.getReference();
      shadow = computeReferenceRanges(range_type, RangeCoordinateSystem,
                                      RangeUnits, shadow, shad_ref, ranges);
    }
    else if (RangeCoordinateSystems != null) {
      TupleType RangeType = (TupleType) ((FunctionType) Type).getRange();
      int j = 0;
      for (int i=0; i<RangeCoordinateSystems.length; i++) {
        MathType component = RangeType.getComponent(i);
        if (component instanceof RealType) {
          j++;
        }
        else { // (component instanceof RealTupleType)
          int m = ((RealTupleType) component).getDimension();
          if (RangeCoordinateSystems[i] != null) {
            // computeRanges for Reference (relative to range
            // component) RealTypes
            double[][] sub_ranges = new double[2][m];
            Unit[] sub_units = new Unit[m];
            for (int k=0; k<m; k++) {
              sub_ranges[0][k] = ranges[0][j];
              sub_ranges[1][k] = ranges[1][j];
              sub_units[k] = RangeUnits[j];
              j++;
            }
            ShadowRealTupleType range_type = (ShadowRealTupleType)
              ((ShadowTupleType) ((ShadowFunctionType) type).getRange()).
                                                             getComponent(i);
            shad_ref = range_type.getReference();
            shadow = computeReferenceRanges(range_type, RangeCoordinateSystems[i],
                                       sub_units, shadow, shad_ref, sub_ranges);
          }
          else { // (RangeCoordinateSystems[i] == null)
            j += m;
          }
        } // end if (component instanceof RealTupleType)
      } // end for (int i=0; i<RangeCoordinateSystems.length; i++)
    } // end if (RangeCoordinateSystems != null)
    return shadow;
  }

  /** return a FlatField that clones this, except its ErrorEstimate-s
      are adjusted for sampling errors in error */
  public Data adjustSamplingError(Data error, int error_mode)
         throws VisADException, RemoteException {
    if (isMissing() || error == null || error.isMissing()) return this;
    FlatField field =
      new FlatField((FunctionType) Type, getDomainSet(), RangeCoordinateSystem,
                    RangeCoordinateSystems, RangeSet, RangeUnits);
    if (isMissing()) return field;
    FlatField new_error = (FlatField)
      ((FlatField) error).resample(getDomainSet(), NEAREST_NEIGHBOR, NO_ERRORS);

    //TODO: check for float vs. double
    double[][] values = unpackValues();
    field.packValues(values, false);

    ErrorEstimate[] errors = new ErrorEstimate[TupleDimension];
    double[][] error_values = new_error.unpackValues();
    for (int i=0; i<TupleDimension; i++) {
      double a = 0.0;
      for (int k=0; k<error_values[i].length; k++) {
        a += error_values[i][k];
      }
      a = a / error_values.length;
      double b = RangeErrors[i].getErrorValue();
      double e = (error_mode == INDEPENDENT) ? Math.sqrt(a * a + b * b) :
                                               Math.abs(a) + Math.abs(b);
      errors[i] = new ErrorEstimate(values[i], e, RangeUnits[i]);
    }
    field.setRangeErrors(errors);
    return field;
  }

  public boolean isFlatField() {
    return true;
  }

  /** clone this FlatField, except substitute a DoubleSet for each
      component of RangeSet */
  private FlatField cloneDouble() throws VisADException {
    return cloneDouble(RangeUnits, RangeErrors);
  }

  /** clone this FlatField, except substitute a DoubleSet for each
      component of RangeSet, and substitute units and errors */
  private FlatField cloneDouble(Unit[] units, ErrorEstimate[] errors)
          throws VisADException {
    return cloneDouble( null, units, errors );
    /*- TDR June 1998
    // create (initially missing) FlatField for return
    // use DoubleSet rather than RangeSet for intermediate computation results
    Set[] sets = new Set[TupleDimension];
    for (int i=0; i<TupleDimension; i++) {
      SetType set_type =
        new SetType(((FunctionType) Type).getFlatRange().getComponent(i));
      sets[i] = new DoubleSet(set_type);
    }
    FlatField field =
      new FlatField((FunctionType) Type, getDomainSet(), RangeCoordinateSystem,
                    RangeCoordinateSystems, sets, units);
    double[][] values = unpackValues();
    field.packValues(values, false);
    // field.DoubleRange = values;
    field.setRangeErrors(errors);
    field.clearMissing();
    return field;
    */
  }

  /*- TDR June 1998  */
  protected FlatField cloneDouble ( MathType f_type, Unit[] units,
                                 ErrorEstimate[] errors )
          throws VisADException
  {
      return cloneDouble ( f_type, units, errors, null);
  }


  /*- TDR June 1998  */
  protected FlatField cloneDouble (MathType f_type, 
                                 Unit[] units,
                                 ErrorEstimate[] errors, 
                                 double[][]newValues )
      throws VisADException
    {
    MathType N_type = ((f_type == null) ? Type : f_type );

    // create (initially missing) FlatField for return
    // use DoubleSet rather than RangeSet for intermediate computation results
    Set[] sets = new Set[TupleDimension];
    for (int i=0; i<TupleDimension; i++) {
      SetType set_type =
        new SetType(((FunctionType) N_type).getFlatRange().getComponent(i));
      sets[i] = new DoubleSet(set_type);
    }
/* WLH 3 April 2003
    FlatField field =
      new FlatField((FunctionType) N_type, 
                    getDomainSet(), 
                    RangeCoordinateSystem,
                    RangeCoordinateSystems, 
                    sets, 
                    units);
*/
    RealTupleType d_type = ((FunctionType)N_type).getDomain();
    Set new_set = null;
    if (!d_type.equals( ((FunctionType) getType()).getDomain() )) {
      new_set = (Set) getDomainSet().cloneButType(d_type);
    }
    else {
      new_set = getDomainSet();
    }
    FlatField field =
      new FlatField((FunctionType) N_type,
                    new_set,
                    RangeCoordinateSystem,
                    RangeCoordinateSystems,
                    sets,
                    units);

    if (newValues == null)
        newValues = unpackValues ();
    field.packValues(newValues, false);

    // field.DoubleRange = values;
    field.setRangeErrors(errors);
    field.clearMissing();
    return field;
  }



  protected FlatField cloneFloat (MathType f_type, 
                                Unit[] units,
                                ErrorEstimate[] errors)
          throws VisADException
  {
      //Pass in null values array
      return cloneFloat (f_type, units, errors, null);
  }

  /*- TDR June 1998  */
  protected FlatField cloneFloat (MathType f_type, 
                                Unit[] units,
                                ErrorEstimate[] errors, 
                                float[][]newValues )
          throws VisADException
  {
      MathType N_type = ((f_type == null) ? Type : f_type );

    // create (initially missing) FlatField for return
    // use FloatSet rather than RangeSet for intermediate computation results
    Set[] sets = new Set[TupleDimension];
    for (int i=0; i<TupleDimension; i++) {
      SetType set_type =
        new SetType(((FunctionType) N_type).getFlatRange().getComponent(i));
      sets[i] = new FloatSet(set_type);
    }
/* WLH 3 April 2003
    FlatField field =
      new FlatField((FunctionType) N_type, getDomainSet(), RangeCoordinateSystem,
                    RangeCoordinateSystems, sets, units);
*/
    RealTupleType d_type = ((FunctionType)N_type).getDomain();
    Set new_set = null;
    if (!d_type.equals( ((FunctionType) getType()).getDomain() )) {
      new_set = (Set) getDomainSet().cloneButType(d_type);
    }
    else {
      new_set = getDomainSet();
    }
    FlatField field = 
      new FlatField((FunctionType) N_type, new_set, RangeCoordinateSystem,
                    RangeCoordinateSystems, sets, units);

    if (newValues == null) {
        //If we don't have the values array then copy this one.
        newValues = unpackFloats (true);
    }
    field.packValues(newValues, false);

    // field.DoubleRange = values;
    field.setRangeErrors(errors);
    field.clearMissing();
    return field;
  }


  /** clone metadata but return missing values */
  private FlatField cloneMissing() throws VisADException {
    return new FlatField((FunctionType) Type, getDomainSet(), RangeCoordinateSystem,
                         RangeCoordinateSystems, RangeSet, RangeUnits);
  }

  /**
   * Clones this instance.  Immutable fields are shallow copied.  Range
   * values, however, are deep copied.
   *
   * <p> Note that it is possible to simultaneously modify the domain-set of
   * both this instance and the clone by modifying the values in the array
   * returned by invoking <code>getSamples(false)</code> on the domain-set of
   * either this instance or the clone.  Don't do this unless you enjoy 
   * debugging.</p>
   *
   * @return                  A clone of this instance.
   * @throws RuntimeException if a {@link VisADException} occurs.
   */
  public Object clone() {
    FlatField clone;
    
    try {
      clone = (FlatField)super.clone();
    }
    catch (CloneNotSupportedException ex) {
      throw new Error("Assertion failure");  // can't happen
    }

    synchronized(DoubleRange) {
      if (!MissingFlag) {
        try {
          // DRM 3 Oct 2006 - we need to recreate this otherwise 
          // it's shared and it's mutable!
          clone.DoubleRange = new double[TupleDimension][];
          double[][] values  = unpackValues(true);
          clone.packValues(values, false);
        }
        catch (VisADException ex) {
          throw new RuntimeException(ex.toString());
        }
        try {
          clone.setRangeErrors(RangeErrors);
        }
        catch (FieldException ex) {
          throw new Error("Assertion failure");  // can't happen
        }
      }
    }

    return clone;
  }

  String valuesString() throws VisADException {
    int rowlength;
    StringBuffer s = new StringBuffer("");
    int ncolumns = 8 / TupleDimension;
    if (ncolumns < 1) ncolumns = 1;
    if (getDomainSet() instanceof GriddedSet) {
        // && ((GriddedSet) getDomainSet()).ManifoldDimension == 2) {
      rowlength = ((GriddedSet) getDomainSet()).getLength(0);
    }
    else {
      rowlength = getLength();
    }
    RealTupleType range = ((FunctionType) Type).getFlatRange();
    RealType[] types = range.getRealComponents();
    double[][] values = unpackValues();
    int rl = rowlength;
    int i = 0;
    while (i<getLength()) {
      int nc = Math.min(rl, Math.min(ncolumns, getLength()-i));
      int ip = i + nc;
      for (int k=i; k<ip; k++) {
        if (k > i) s.append(", ");
        if (TupleDimension == 1) {
          s.append(new Real(types[0], values[0][k], RangeUnits[0]).toString());
        }
        else if (((FunctionType) Type).getReal()) {
          String t = "(" + new Real(types[0], values[0][k], RangeUnits[0]);
          for (int j=1; j<TupleDimension; j++) {
            t = t + ", " + new Real(types[j], values[j][k], RangeUnits[j]);
          }
          t = t + ")";
          s.append(t);
        }
        else { // Flat Tuple
          TupleType RangeType = (TupleType) ((FunctionType) Type).getRange();
          String t = "(";
          int j = 0;
          for (int l=0; l<RangeType.getDimension(); l++) {
            if (j > 0) t = t + ", ";
            MathType type = RangeType.getComponent(l);
            if (type instanceof RealType) {
              t = t + new Real(types[j], values[j][k], RangeUnits[j]);
              j++;
            }
            else {
              int mm = ((TupleType) type).getDimension();
              t = t + "(" + new Real(types[j], values[j][k], RangeUnits[j]);
              j++;
              for (int kk=1; kk<mm; kk++) {
                t = t + ", " + new Real(types[j], values[j][k], RangeUnits[j]);
                j++;
              }
              t = t + ")";
            }
          }
          t = t + ")";
          s.append(t);
        }
      } // end for (int k=i; k<ip; k++)
      s.append("\n");
      i = ip;
      rl -= nc;
      if (rl <= 0) {
        rl = rowlength;
        s.append("\n");
      }
    } // end while (i<getLength())
    return s.toString();
  }

  public String toString() {
    try {
      if (isMissing()) {
        return "FlatField  missing\n";
      }
      else {
        return "FlatField\n    " + Type + "\n" + valuesString();
      }
    }
    catch (VisADException e) {
      return e.toString();
    }
  }

  public String longString(String pre) throws VisADException {
    String t = pre + "FlatField\n" + pre + "  Type: " +
               Type.toString() + "\n";
    if (getDomainSet() != null) {
      t = t + pre + "  DomainSet:\n" + getDomainSet().longString(pre + "    ");
    }
    else {
      t = t + pre + "  DomainSet: undefined\n";
    }
    for (int i=0; i<TupleDimension; i++) {
      if (RangeSet[i] != null) {
        t = t + pre + "  RangeSet[" + i + "]:\n" + RangeSet[i].longString(pre + "    ");
      }
      else {
        t = t + pre + "  RangeSet[" + i + "]: undefined\n";
      }
    }
    if (isMissing()) {
      return t + "  missing\n";
    }
    else {
      return t + valuesString();
    }
  }

  /**
   * Gets the number of components in the "flat" range.
   *
   * @return                    The number of components in the "flat" range.
   */
  public int getRangeDimension() {
     return TupleDimension;
  }

  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof FlatField)) {
      return false;
    }

    FlatField fld = (FlatField )obj;

    if (RangeMode == null || fld.RangeMode == null) {
      if (RangeMode != null || fld.RangeMode != null) {
        return false;
      }
    } else if (RangeMode.length != fld.RangeMode.length) {
      return false;
    } else {
      for (int i = 0; i < RangeMode.length; i++) {
        if (RangeMode[i] != fld.RangeMode[i]) {
          return false;
        }
      }
    }

    if (RangeSet == null || fld.RangeSet == null) {
      if (RangeSet != null || fld.RangeSet != null) {
        return false;
      }
    } else if (RangeSet.length != fld.RangeSet.length) {
      return false;
    } else {
      for (int i = 0; i < RangeSet.length; i++) {
        if (!RangeSet[i].equals(fld.RangeSet[i])) {
          return false;
        }
      }
    }

    for (int i=0; i<TupleDimension; i++) {
      switch (RangeMode[i]) {
        case DOUBLE:
          if (!Arrays.equals(DoubleRange[i], fld.DoubleRange[i])) {
            return false;
          }
          break;
        case FLOAT:
          if (!Arrays.equals(FloatRange[i], fld.FloatRange[i])) {
            return false;
          }
          break;
        case BYTE:
          if (!Arrays.equals(ByteRange[i], fld.ByteRange[i])) {
            return false;
          }
          break;
        case SHORT:
          if (!Arrays.equals(ShortRange[i], fld.ShortRange[i])) {
            return false;
          }
          break;
        case INT:
          if (!Arrays.equals(IntRange[i], fld.IntRange[i])) {
            return false;
          }
          break;
        default:
          return false;
      }
    }

    return super.equals(obj);
  }

  /** construct a FlatField of given type; used for testing */
  public static FlatField makeField(FunctionType type, int length, boolean irregular)
         throws VisADException, RemoteException {
    double first = 0.0;
    double last = length - 1.0;
    double step = 1.0;
    double half = 0.5 * last;
    RealTupleType dtype = type.getDomain();
    RealTupleType rtype = type.getFlatRange();
    int domain_dim = dtype.getDimension();
    int range_dim = rtype.getDimension();
    SampledSet domain_set = null;
    int dsize = 0;
    Random random = new Random();
    if (irregular) {
      if (domain_dim == 1) {
        dsize = length;
        float[][] samples = new float[1][dsize];
        for (int i=0; i<dsize; i++) {
          samples[0][i] = (float) (last * random.nextFloat());
        }
        domain_set = new Irregular1DSet(dtype, samples);
      }
      else if (domain_dim == 2) {
        dsize = length * length;
        float[][] samples = new float[2][dsize];
        for (int i=0; i<dsize; i++) {
          samples[0][i] = (float) (last * random.nextFloat());
          samples[1][i] = (float) (last * random.nextFloat());
        }
        domain_set = new Irregular2DSet(dtype, samples);
      }
      else if (domain_dim == 3) {
        dsize = length * length * length;
        float[][] samples = new float[3][dsize];

        // random Irregular3DSet
        for (int i=0; i<dsize; i++) {
          samples[0][i] = (float) (last * random.nextFloat());
          samples[1][i] = (float) (last * random.nextFloat());
          samples[2][i] = (float) (last * random.nextFloat());
        }
/*
        // jittered linear Irregular3DSet
        Linear3DSet square_set = new Linear3DSet(dtype, first, last, length,
                                                 first, last, length,
                                                 first, last, length);
        samples = square_set.getSamples();
        for (int i=0; i<dsize; i++) {
          samples[0][i] += 0.05 * random.nextFloat();
          samples[1][i] += 0.05 * random.nextFloat();
          samples[2][i] += 0.05 * random.nextFloat();
        }
*/

        domain_set = new Irregular3DSet(dtype, samples);
      }
      else {
        throw new FieldException("FlatField.makeField: bad domain dimension");
      }
    }
    else { // if (!irregular)
      if (domain_dim == 1) {
        domain_set = new Linear1DSet(dtype, first, last, length);
        dsize = length;
      }
      else if (domain_dim == 2) {
        domain_set = new Linear2DSet(dtype, first, last, length,
                                            first, last, length);
        dsize = length * length;
      }
      else if (domain_dim == 3) {
        domain_set = new Linear3DSet(dtype, first, last, length,
                                            first, last, length,
                                            first, last, length);
        dsize = length * length * length;
      }
      else {
        throw new FieldException("FlatField.makeField: bad domain dimension");
      }
    }
    FlatField image = new FlatField(type, domain_set);
    fillField(image, step, half);
    return image;
  }

  public static void fillField(FlatField image, double step, double half)
         throws VisADException, RemoteException {
    Random random = new Random();
    FunctionType type = (FunctionType) image.getType();
    RealTupleType dtype = type.getDomain();
    RealTupleType rtype = type.getFlatRange();
    int domain_dim = dtype.getDimension();
    int range_dim = rtype.getDimension();
    SampledSet domain_set = (SampledSet) image.getDomainSet();
    int dsize = domain_set.getLength();

    double[][] data = new double[range_dim][dsize];
    float[][] samples = domain_set.getSamples();
    for (int k=0; k<range_dim; k++) {
      if (domain_dim == 1) {
        for (int i=0; i<dsize; i++) {
          float x = samples[0][i];
          if (k == 0) {
            data[k][i] = (float) Math.abs(step * (x - half));
          }
          else if (k == 1) {
            data[k][i] = x;
          }
          else {
            data[k][i] = random.nextDouble();
          }
        }
      }
      else if (domain_dim == 2) {
        for (int i=0; i<dsize; i++) {
          float x = samples[0][i];
          float y = samples[1][i];
          if (k == 0) {
            data[k][i] = (float) (step * Math.sqrt(
              (x - half) * (x - half) +
              (y - half) * (y - half)));
          }
          else if (k == 1) {
            data[k][i] = x;
          }
          else if (k == 2) {
            data[k][i] = y;
          }
          else {
            data[k][i] = random.nextDouble();
          }
        }
      }
      else if (domain_dim == 3) {
        for (int i=0; i<dsize; i++) {
          float x = samples[0][i];
          float y = samples[1][i];
          float z = samples[2][i];
          if (k == 0) {
            data[k][i] = (float) (step * Math.sqrt(
              (x - half) * (x - half) +
              (y - half) * (y - half) +
              (z - half) * (z - half)));
          }
          else if (k == 1) {
            data[k][i] = x;
          }
          else if (k == 2) {
            data[k][i] = y;
          }
          else if (k == 3) {
            data[k][i] = z;
          }
          else {
            data[k][i] = random.nextDouble();
          }
        }
      }
    }
    image.setSamples(data);
  }


  /** construct a FlatField with a 2-D domain and a 1-D range;
      used for testing */
  public static FlatField makeField1(FunctionType type,
                              double first1, double last1, int length1,
                              double first2, double last2, int length2)
          throws VisADException, RemoteException {

    double step1 = (last1 - first1) / (length1 - 1);
    double step2 = (last2 - first2) / (length2 - 1);

    Linear2DSet imageset =
      new Linear2DSet(type.getDomain(), first1, last1, length1,
                                        first2, last2, length2);

    FlatField image = new FlatField(type, imageset);

    double[][] data = new double[1][length1 * length2];
    for (int i=0; i<length1; i++) {
      for (int j=0; j<length2; j++) {
        data[0][i + length1 * j] =
          (first1 + step1 * i) + (first2 + step2 * j);
      }
    }
    image.setSamples(data);
    return image;
  }

  /** construct a FlatField with a 2-D domain and a 2-D range;
      used for testing */
  public static FlatField makeField2(FunctionType type,
                              double first1, double last1, int length1,
                              double first2, double last2, int length2)
          throws VisADException, RemoteException {

    double step1 = (last1 - first1) / (length1 - 1);
    double step2 = (last2 - first2) / (length2 - 1);

    Linear2DSet imageset =
      new Linear2DSet(type.getDomain(), first1, last1, length1,
                                        first2, last2, length2);

    FlatField image = new FlatField(type, imageset);

    double[][] data = new double[2][length1 * length2];
    for (int i=0; i<length1; i++) {
      for (int j=0; j<length2; j++) {
        data[0][i + length1 * j] = first1 + step1 * i;
        data[1][i + length1 * j] = first2 + step2 * j;
      }
    }
    image.setSamples(data);
    return image;
  }

  /** construct a FlatField with a 2-D domain and a 2-D range
      and random values; used for testing */
  static FlatField makeRandomField2(FunctionType type,
                                    double first1, double last1, int length1,
                                    double first2, double last2, int length2)
          throws VisADException, RemoteException {

    double step1 = (last1 - first1) / (length1 - 1);
    double step2 = (last2 - first2) / (length2 - 1);

    Linear2DSet imageset =
      new Linear2DSet(type.getDomain(), first1, last1, length1,
                                        first2, last2, length2);

    FlatField image = new FlatField(type, imageset);

    Random random = new Random();
    double[][] data = new double[2][length1 * length2];
    for (int i=0; i<length1; i++) {
      for (int j=0; j<length2; j++) {
        data[0][i + length1 * j] = random.nextDouble();
        data[1][i + length1 * j] = random.nextDouble();
      }
    }
    image.setSamples(data);
    return image;
  }

  /** run 'java visad.FlatField' to test the FlatField class */
  public static void main(String args[])
         throws VisADException, RemoteException {

    byte b = 10;
    Real w = new Real(b);

    RealType X = RealType.getRealType("X");
    RealType Y = RealType.getRealType("Y");
    RealType Z = RealType.getRealType("Z");

    RealType A = RealType.getRealType("A");
    RealType B = RealType.getRealType("B");

    RealType[] domain2d = {X, Y};
    RealTupleType Domain2d = new RealTupleType(domain2d, null, null);
    Integer2DSet Domain2dSet = new Integer2DSet(Domain2d, 4, 4);
    Domain2d.setDefaultSet(Domain2dSet);

    RealType[] range2d = {A, B};
    RealTupleType Range2d = new RealTupleType(range2d);

    FunctionType Field2d1 = new FunctionType(Domain2d, A);
    FunctionType Field2d2 = new FunctionType(Domain2d, Range2d);

    double first11 = 0.0;
    double last11 = 3.0;
    int length11 = 4;
    double first12 = 0.0;
    double last12 = 3.0;
    int length12 = 4;
    FlatField image1 = makeField1(Field2d1, first11, last11, length11,
                                            first12, last12, length12);
    FlatField image3 = makeField2(Field2d2, first11, last11, length11,
                                            first12, last12, length12);
    Real[] reals = {new Real(X ,1.5), new Real(Y, 2.5)};
    RealTuple val = new RealTuple(reals);

    double first21 = 0.0;
    double last21 = 3.0;
    int length21 = 7;
    double first22 = 0.0;
    double last22 = 3.0;
    int length22 = 7;
    FlatField image2 = makeField1(Field2d1, first21, last21, length21,
                                            first22, last22, length22);
    FlatField image4 = makeField2(Field2d2, first21, last21, length21,
                                            first22, last22, length22);

    System.out.println("image1 = " + image1);
    System.out.println("image2 = " + image2);
    System.out.println("image3 = " + image3);
    System.out.println("image4 = " + image4);

    // do some computations in NEAREST_NEIGHBOR sampling mode
    System.out.println("sampling mode is NEAREST_NEIGHBOR");
    System.out.println("image3 + image4 = " + image3.add(image4));
    System.out.println("image4 - image3 = " + image4.subtract(image3));
    System.out.println("image3 * image4 = " + image3.multiply(image4));
    System.out.println("image4 / image3 = " + image4.divide(image3));
    System.out.println("sqrt(image3) = " + image3.sqrt());
    System.out.println("val = " + val + " image1(val) = " +
                       image1.evaluate(val));
    System.out.println("val = " + val + " image3(val) = " +
                       image3.evaluate(val) + "\n");
    System.out.println("image3 + val = " + image3.add(val));
    System.out.println("val - image3 = " + val.subtract(image3));
    System.out.println("image3 * val = " + image3.multiply(val));
    System.out.println("val / image3 = " + val.divide(image3));

    // now do some computations in WEIGHTED_AVERAGE sampling mode
    System.out.println("Field.Mode is WEIGHTED_AVERAGE");
    System.out.println("image3 + image4 = " +
                       image3.add(image4, WEIGHTED_AVERAGE, INDEPENDENT));
    System.out.println("image4 - image3 = " +
                       image4.subtract(image3, WEIGHTED_AVERAGE, INDEPENDENT));
    System.out.println("image3 * image4 = " +
                       image3.multiply(image4, WEIGHTED_AVERAGE, INDEPENDENT));
    System.out.println("image4 / image3 = " +
                       image4.divide(image3, WEIGHTED_AVERAGE, INDEPENDENT));
    System.out.println("val = " + val + " image1(val) = " +
                       image1.evaluate(val, WEIGHTED_AVERAGE, INDEPENDENT));
    System.out.println("val = " + val + " image3(val) = " +
                       image3.evaluate(val, WEIGHTED_AVERAGE, INDEPENDENT) + "\n");
    System.out.println("image3 + val = " +
                       image3.add(val, WEIGHTED_AVERAGE, INDEPENDENT));
    System.out.println("val - image3 = " +
                       val.subtract(image3, WEIGHTED_AVERAGE, INDEPENDENT));
    System.out.println("image3 * val = " +
                       image3.multiply(val, WEIGHTED_AVERAGE, INDEPENDENT));
    System.out.println("val / image3 = " +
                       val.divide(image3, WEIGHTED_AVERAGE, INDEPENDENT));

    // do some more computations in NEAREST_NEIGHBOR sampling mode
    System.out.println("sampling mode is NEAREST_NEIGHBOR");

    System.out.println("image1 + w = " + image1.add(w));
    System.out.println("image1 - w = " + image1.subtract(w));
    System.out.println("image1 * w = " + image1.multiply(w));
    System.out.println("image1 / w = " + image1.divide(w));

    System.out.println("w + image2 = " + w.add(image2));
    System.out.println("w - image2 = " + w.subtract(image2));
    System.out.println("w * image2 = " + w.multiply(image2));
    System.out.println("w / image2 = " + w.divide(image2));

    // test DateTime printing
    RealType[] range2t = {A, RealType.Time};
    RealTupleType Range2t = new RealTupleType(range2t);
    FunctionType Field2t2 = new FunctionType(Domain2d, Range2t);
    FlatField imaget = makeField2(Field2t2, first11, last11, length11,
                                            first12, last12, length12);
    System.out.println("imaget = " + imaget);

  }

/* Here's the output:

iris 251% java visad.FlatField
image1 = FlatField
    FunctionType (Real): (X, Y) -> A
0, 1, 2, 3

1, 2, 3, 4

2, 3, 4, 5

3, 4, 5, 6


image2 = FlatField
    FunctionType (Real): (X, Y) -> A
0, 0.5, 1, 1.5, 2, 2.5, 3

0.5, 1, 1.5, 2, 2.5, 3, 3.5

1, 1.5, 2, 2.5, 3, 3.5, 4

. . .

imaget = FlatField
    FunctionType (Real): (X, Y) -> (A, Time)
(0.0, 1970-01-01 00:00:00.000Z), (1.0, 1970-01-01 00:00:00.000Z), (2.0, 1970-01-01 00:00:00.000Z), (3.0, 1970-01-01 00:00:00.000Z)

(0.0, 1970-01-01 00:00:01.000Z), (1.0, 1970-01-01 00:00:01.000Z), (2.0, 1970-01-01 00:00:01.000Z), (3.0, 1970-01-01 00:00:01.000Z)

(0.0, 1970-01-01 00:00:02.000Z), (1.0, 1970-01-01 00:00:02.000Z), (2.0, 1970-01-01 00:00:02.000Z), (3.0, 1970-01-01 00:00:02.000Z)

(0.0, 1970-01-01 00:00:03.000Z), (1.0, 1970-01-01 00:00:03.000Z), (2.0, 1970-01-01 00:00:03.000Z), (3.0, 1970-01-01 00:00:03.000Z)

*/



}

