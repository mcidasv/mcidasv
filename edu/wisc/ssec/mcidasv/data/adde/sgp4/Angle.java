/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package edu.wisc.ssec.mcidasv.data.adde.sgp4;

//import gov.nasa.worldwind.util.Logging;

//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

/**
 * Represents a geometric angle. Instances of <code>Angle</code> are immutable. An angle can be obtained through the
 * factory methods {@link #fromDegrees} and {@link #fromRadians}.
 *
 * @author Tom Gaskins
 * @version $Id$
 */
public class Angle 
{

    public final double degrees;
    public final double radians;

    private final static double RADIANS_TO_DEGREES = 180d / Math.PI;

    /**
     * Obtains an angle from a specified number of radians.
     *
     * @param radians the size in radians of the angle to be obtained.
     *
     * @return a new angle, whose size in radians is given by <code>radians</code>.
     */
    public static Angle fromRadians(double radians)
    {
        return new Angle(RADIANS_TO_DEGREES * radians, radians);
    }

    private Angle(double degrees, double radians)
    {
        this.degrees = degrees;
        this.radians = radians;
    }
}
