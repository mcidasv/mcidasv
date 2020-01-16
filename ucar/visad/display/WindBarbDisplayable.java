/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2020
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

package ucar.visad.display;


import ucar.visad.WindBarbRenderer;
import ucar.visad.quantities.CommonUnits;

import visad.CoordinateSystem;
import visad.DataRenderer;
import visad.RealTupleType;
import visad.RealType;
import visad.SphericalCoordinateSystem;
import visad.VisADException;

import visad.bom.BarbRenderer;

import visad.java2d.DisplayRendererJ2D;


import java.rmi.RemoteException;


/**
 * Provides support for a Displayable to show wind with the
 * conventional meteorological "wind barb" symbols.
 *
 * @author IDV Development Team
 */
public class WindBarbDisplayable extends FlowDisplayable {

    /**
     * Constructs from a name for the Displayable and the type of the
     * parameter.
     *
     * @param name           The name for the displayable.
     * @param rTT        The VisAD RealTupleType of the parameter.  May be
     *                          <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindBarbDisplayable(String name, RealTupleType rTT)
            throws VisADException, RemoteException {
        this(name, rTT, false);
    }

    /**
     * Constructs from a name for the Displayable and the type of the
     * parameter.
     *
     * @param name           The name for the displayable.
     * @param rTT        The VisAD RealTupleType of the parameter.  May be
     *                          <code>null</code>.
     * @param useSpeedForColor _more_
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public WindBarbDisplayable(String name, RealTupleType rTT,
                               boolean useSpeedForColor)
            throws VisADException, RemoteException {
        super(name, rTT, 0.1f, useSpeedForColor);

    }

    /**
     * Returns the {@link visad.DataRenderer} associated with this instance.
     *
     * @return             The {@link visad.DataRenderer} associated with this
     *                     instance.
     */
    protected DataRenderer getDataRenderer() {
        BarbRenderer br = (getDisplay().getDisplayRenderer()
                           instanceof DisplayRendererJ2D)
                          ? new visad.bom.BarbRendererJ2D()
                          : new WindBarbRenderer(speedUnit);
        return (DataRenderer) br;
    }

    /**
     * Sets the RealType of the RGB parameter.
     * @param realType          The RealType of the RGB parameter.  May
     *                          not be <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRGBRealType(RealType realType)
            throws RemoteException, VisADException {
        super.setRGBRealType(realType);
        // mjh inq 1911: we actually don't want to force units to KNOT here;
        // the barb icon will always be in knots but not necessarily the color
        // and color scale.
        // TODO: this method probably doesn't need to be overridden if we
        // aren't forcing unit to knots...
        //setDisplayUnit(CommonUnits.KNOT);
    }


    /**
     * Set the range of the flow maps
     *
     * @param min min value
     * @param max max value
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setFlowRange(double min, double max)
            throws VisADException, RemoteException {

        if (isCartesianWind()) {
            flowXMap.setRange(-1.0, 1.0);
            flowYMap.setRange(-1.0, 1.0);
        } else {
            flowXMap.setRange(0.0, 360.0);
            flowYMap.setRange(0.0, 1.0);
        }
    }

    /**
     * Check to see if this is 3D flow
     * @return  false
     */
    public boolean get3DFlow() {
        return false;
    }

}
