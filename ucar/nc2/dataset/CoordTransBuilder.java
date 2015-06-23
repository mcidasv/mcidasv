/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2015
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

package ucar.nc2.dataset;

import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.transform.*;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.unidata.util.Parameter;

import java.util.List;
import java.util.ArrayList;
import java.util.Formatter;

/**
 * Manager for Coordinate Transforms.
 * @author john caron
 */
public class CoordTransBuilder {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordTransBuilder.class);
  static private List<Transform> transformList = new ArrayList<>();
  static private boolean userMode = false;

  static private final boolean loadWarnings = false;

  // search in the order added
  static {
    registerTransform(CF.ALBERS_CONICAL_EQUAL_AREA, AlbersEqualArea.class);
    registerTransform(CF.AZIMUTHAL_EQUIDISTANT, AzimuthalEquidistant.class);
    registerTransform("flat_earth", FlatEarth.class);
    registerTransform(CF.GEOSTATIONARY, Geostationary.class);
    registerTransform(CF.LAMBERT_AZIMUTHAL_EQUAL_AREA, LambertAzimuthal.class);
    registerTransform(CF.LAMBERT_CONFORMAL_CONIC , LambertConformalConic.class);
    registerTransform(CF.LAMBERT_CYLINDRICAL_EQUAL_AREA , LambertCylindricalEqualArea.class);
    registerTransformMaybe("mcidas_area", "ucar.nc2.iosp.mcidas.McIDASAreaTransformBuilder"); // optional - needs visad.jar
    registerTransform(CF.MERCATOR, Mercator.class);
    registerTransform("MSGnavigation", MSGnavigation.class);
    registerTransform(CF.ORTHOGRAPHIC, Orthographic.class);
    registerTransform(CF.POLAR_STEREOGRAPHIC, PolarStereographic.class);
    registerTransform("polyconic", PolyconicProjection.class); // ghansham@sac.isro.gov.in 1/8/2012
    registerTransform(CF.ROTATED_LATITUDE_LONGITUDE, RotatedPole.class);
    registerTransform("rotated_latlon_grib", RotatedLatLon.class);
    registerTransform(CF.SINUSOIDAL, Sinusoidal.class);
    registerTransform(CF.STEREOGRAPHIC, Stereographic.class);
    registerTransform(CF.TRANSVERSE_MERCATOR, TransverseMercator.class);
    registerTransform("UTM", UTM.class);
    registerTransform(CF.VERTICAL_PERSPECTIVE, VerticalPerspective.class);

    // registerTransform("atmosphere_ln_pressure_coordinate", VAtmLnPressure.class); // DO NOT USE: see CF1Convention.makeAtmLnCoordinate()
    registerTransform("atmosphere_hybrid_height_coordinate", VAtmHybridHeight.class);
    registerTransform("atmosphere_hybrid_sigma_pressure_coordinate", VAtmHybridSigmaPressure.class);
    registerTransform("atmosphere_sigma_coordinate", VAtmSigma.class);
    registerTransform("ocean_s_coordinate", VOceanS.class);
    registerTransform("ocean_sigma_coordinate", VOceanSigma.class);
    registerTransform("explicit_field", VExplicitField.class);
    registerTransform("existing3DField", VExplicitField.class); // deprecate

    //-sachin 03/25/09
    registerTransform("ocean_s_coordinate_g1", VOceanSG1.class);
    registerTransform("ocean_s_coordinate_g2", VOceanSG2.class);   

    registerTransform("FGFProjection", FGFProjection.class);
    registerTransform("FGF Projection", FGFProjection.class);
//    registerTransform("geostationary", GEOSProjection.class);

    // further calls to registerTransform are by the user
    userMode = true;
  }

   /**
    * Register a class that implements a Coordinate Transform.
    * @param transformName name of transform. This name is used in the datasets to identify the transform, eg CF names.
    * @param c class that implements CoordTransBuilderIF.
    */
  static public void registerTransform( String transformName, Class c) {
    if (!(CoordTransBuilderIF.class.isAssignableFrom( c)))
      throw new IllegalArgumentException("Class "+c.getName()+" must implement CoordTransBuilderIF");

    // fail fast - check newInstance works
    try {
      c.newInstance();
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("CoordTransBuilderIF Class "+c.getName()+" cannot instantiate, probably need default Constructor");
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("CoordTransBuilderIF Class "+c.getName()+" is not accessible");
    }

    // user stuff gets put at top
    if (userMode)
      transformList.add( 0, new Transform( transformName, c));
    else
      transformList.add( new Transform( transformName, c));

  }

  /**
   * Register a class that implements a Coordinate Transform.
   * @param transformName name of transform. This name is used in the datasets to identify the transform, eg CF names.
   * @param className name of class that implements CoordTransBuilderIF.
   * @throws ClassNotFoundException if Class.forName( className) fails
   */
  static public void registerTransform( String transformName, String className) throws ClassNotFoundException {
    Class c = Class.forName( className);
    registerTransform( transformName, c);
  }

  /**
   * Register a class that implements a Coordinate Transform.
   * @param transformName name of transform. This name is used in the datasets to identify the transform, eg CF names.
   * @param className name of class that implements CoordTransBuilderIF.
   */
  static public void registerTransformMaybe( String transformName, String className) {
    Class c;
    try {
      c = Class.forName( className);
    } catch (ClassNotFoundException e) {
      if (loadWarnings) log.warn("Coordinate Transform Class "+className+" not found.");
      return;
    }
    registerTransform( transformName, c);
  }

  static private class Transform {
    String transName;
    Class transClass;
    Transform(String transName,  Class transClass) {
      this.transName = transName;
      this.transClass = transClass;
    }
  }

  /**
   * Make a CoordinateTransform object from the parameters in a Coordinate Transform Variable, using an intrinsic or
   * registered CoordTransBuilder.
   * @param ds enclosing dataset
   * @param ctv the Coordinate Transform Variable - container for the transform parameters
   * @param parseInfo pass back information about the parsing.
   * @param errInfo pass back error information.
   * @return CoordinateTransform, or null if failure.
   */
  static public CoordinateTransform makeCoordinateTransform (NetcdfDataset ds, Variable ctv, Formatter parseInfo, Formatter errInfo) {
    // standard name
    String transform_name = ds.findAttValueIgnoreCase(ctv, "transform_name", null);
    if (null == transform_name)
      transform_name = ds.findAttValueIgnoreCase(ctv, "Projection_Name", null);

    // these names are from CF - dont want to have to duplicate
    if (null == transform_name)
      transform_name = ds.findAttValueIgnoreCase(ctv, CF.GRID_MAPPING_NAME, null);
    if (null == transform_name)
      transform_name = ds.findAttValueIgnoreCase(ctv, CF.STANDARD_NAME, null);

    if (null == transform_name) {
      parseInfo.format("**Failed to find Coordinate Transform name from Variable= %s%n", ctv);
      return null;
    }

    transform_name = transform_name.trim();

    // do we have a transform registered for this ?
    Class builderClass = null;
    for (Transform transform : transformList) {
      if (transform.transName.equals(transform_name)) {
        builderClass = transform.transClass;
        break;
      }
    }
    if (null == builderClass) {
      parseInfo.format("**Failed to find CoordTransBuilder name= %s from Variable= %s%n", transform_name, ctv);
      return null;
    }

      // get an instance of that class
    CoordTransBuilderIF builder = null;
    try {
      builder = (CoordTransBuilderIF) builderClass.newInstance();
    } catch (InstantiationException e) {
      log.error("Cant instantiate "+builderClass.getName(), e);
    } catch (IllegalAccessException e) {
      log.error("Cant access "+builderClass.getName(), e);
    }
    if (null == builder) { // cant happen - because this was tested in registerTransform()
      parseInfo.format("**Failed to build CoordTransBuilder object from class= %s for Variable= %s%n", builderClass.getName(), ctv);
      return null;
    }

    builder.setErrorBuffer( errInfo);
    CoordinateTransform ct = builder.makeCoordinateTransform(ds, ctv);

    if (ct != null) {
      parseInfo.format(" Made Coordinate transform %s from variable %s: %s%n",transform_name, ctv.getFullName(), builder);
    }

    return ct;
  }


  /**
   * Create a "dummy" Coordinate Transform Variable based on the given CoordinateTransform.
   * This creates a scalar Variable with dummy data, and adds the Parameters of the CoordinateTransform
   * as attributes.
   * @param ds for this dataset
   * @param ct based on the CoordinateTransform
   * @return the Coordinate Transform Variable. You must add it to the dataset.
   */
  static public VariableDS makeDummyTransformVariable(NetcdfDataset ds, CoordinateTransform ct) {
    VariableDS v = new VariableDS( ds, null, null, ct.getName(), DataType.CHAR, "", null, null);
    List<Parameter> params = ct.getParameters();
    for (Parameter p : params) {
      if (p.isString())
        v.addAttribute(new Attribute(p.getName(), p.getStringValue()));
      else {
        double[] data = p.getNumericValues();
        Array dataA = Array.factory(double.class, new int[]{data.length}, data);
        v.addAttribute(new Attribute(p.getName(), dataA));
      }
    }
    v.addAttribute( new Attribute(_Coordinate.TransformType, ct.getTransformType().toString()));

    // fake data
    Array data = Array.factory(DataType.CHAR.getPrimitiveClassType(), new int[] {}, new char[] {' '});
    v.setCachedData(data, true);

    return v;
  }

}
