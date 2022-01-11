/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2022
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

package ucar.visad.quantities;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import visad.CommonUnit;

import visad.DerivedUnit;

import visad.SI;

import visad.ScaledUnit;

import visad.Unit;

import visad.VisADException;

import visad.data.units.Parser;


/**
 * Provides support for units common to meteorology.
 *
 * @author Steven R. Emmerson
 * @version $Id: CommonUnits.java,v 1.12 2005/05/13 18:35:38 jeffmc Exp $
 */
public class CommonUnits {
    
    private static final Logger logger =
        LoggerFactory.getLogger(CommonUnits.class);
    
    /**
     * The SI unit of pressure.
     */
    public static final Unit PASCAL;

    /**
     * A common unit of pressure.
     */
    public static final Unit HECTOPASCAL;

    /**
     * A common unit of pressure.
     */
    public static final Unit MILLIBAR;

    /**
     * A common unit of temperature.
     */
    public static final Unit CELSIUS;

    /**
     * A common unit of mixing-ratio.
     */
    public static final Unit GRAMS_PER_KILOGRAM;

    /**
     * The SI unit of speed.
     */
    public static final Unit METERS_PER_SECOND;

    /**
     * A common unit of time.
     */
    public static final Unit HOUR;

    /**
     * A common unit of distance.
     */
    public static final Unit NAUTICAL_MILE;

    /**
     * A common unit of speed.
     */
    public static final Unit KNOT;

    /**
     * A common unit of plane angle.
     */
    public static final Unit DEGREE;

    /**
     * A common unit of international foot
     */
    public static final Unit FOOT;

    /**
     * A common unit of kilometer
     */
    public static final Unit KILOMETER;

    /**
     * A common unit of mile
     */
    public static final Unit MILE;


    /**
     * A common unit of millimeter
     */
    public static final Unit MILLIMETER;

    /**
     * A common unit for percent
     */
    public static final Unit PERCENT;




    static {
        Unit pascal          = null;
        Unit millibar        = null;
        Unit celsius         = null;
        Unit gPerKg          = null;
        Unit metersPerSecond = null;
        Unit nauticalMile    = null;
        Unit knot            = null;
        Unit hour            = null;
        Unit foot            = null;
        Unit kilometer       = null;
        Unit mile            = null;
        Unit millimeter      = null;
        Unit percent         = null;


        try {
            pascal = SI.kilogram.divide(SI.meter).divide(
                SI.second.pow(2)).clone("Pa");
            millibar = new ScaledUnit(100, (DerivedUnit) pascal).clone("hPa");
            celsius         = SI.kelvin.shift(273.15).clone("°C");
            gPerKg          = new ScaledUnit(0.001).clone("g/kg");
            metersPerSecond = SI.meter.divide(SI.second).clone("m/s");
            nauticalMile    = new ScaledUnit(1.852e3, SI.meter).clone("nmi");
            hour            = new ScaledUnit(3600.0, SI.second).clone("h");
            knot            = nauticalMile.divide(hour).clone("kt");
            foot            = SI.meter.scale(.0254 * 12).clone("ft");
            kilometer       = SI.meter.scale(1000).clone("km");
            mile            = foot.scale(5280).clone("mi");
            millimeter      = SI.meter.scale(.001).clone("mm");
            percent         = Parser.parse("%").clone("%");
        } catch (Exception e) {
            logger.error("Couldn't initialize CommonUnits class", e);
        }

        PASCAL             = pascal;
        HECTOPASCAL        = millibar;
        MILLIBAR           = millibar;
        CELSIUS            = celsius;
        GRAMS_PER_KILOGRAM = gPerKg;
        METERS_PER_SECOND  = metersPerSecond;
        HOUR               = hour;
        NAUTICAL_MILE      = nauticalMile;
        KNOT               = knot;
        DEGREE             = CommonUnit.degree;
        FOOT               = foot;
        KILOMETER          = kilometer;
        MILE               = mile;
        MILLIMETER         = millimeter;
        PERCENT            = percent;
    }

    /**
     * Constructs from nothing.
     */
    private CommonUnits() {}
}
