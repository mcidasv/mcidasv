/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package ucar.unidata.view.sounding;



import java.rmi.RemoteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.visad.quantities.CAPE;
import ucar.visad.quantities.MassicEnergy;
import ucar.visad.quantities.Pressure;
import ucar.visad.quantities.AirPressure;
import ucar.visad.Util;
import ucar.visad.VisADMath;

import visad.Field;

import visad.Data;

import visad.DataReference;

import visad.FunctionType;

import visad.TypeException;

import visad.Real;

import visad.RealTupleType;

import visad.VisADException;


/**
 * Computes the Convective Available Potential Energy (CAPE) from a profile
 * of massic energy.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.6 $ $Date: 2005/05/13 18:33:25 $
 */
public final class CapeCell extends EnergyFeatureCell {

    private static final Logger logger = LoggerFactory.getLogger(CapeCell.class);

    /**
     * Constructs from references to the massic energy profile, the Level of
     * Free convection (LFC) and the Level of Neutral Buoyancy (LNB).
     *
     * @param energyProfileRef       The massic energy profile reference.
     * @param lfcRef                 The LFC reference.
     * @param lnbRef                 The LNB reference.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public CapeCell(DataReference energyProfileRef, DataReference lfcRef, DataReference lnbRef)
            throws VisADException, RemoteException {

        super("CapeCell", energyProfileRef, lfcRef, lnbRef);

        enableAllInputRefs();
    }

    /**
     * Computes the output Convective Available Potential Energy (CAPE) from
     * the massic energy profile, level of free convection (LFC), and level of
     * neutral buoyancy (LNB).
     *
     * @param datums                The input data in the same order as
     *                              during construction. <code>datums[0]</code>
     *                              is the massic energy profile;
     *                              <code>datums[1]</code> is the LFC pressure;
     *                              and <code>datums[2]</code> is the LNB
     *                              pressure.
     * @return                      The corresponding CAPE.
     * @throws ClassCastException   if an input data reference has the wrong
     *                              type of data object.
     * @throws TypeException        if a VisAD data object has the wrong type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    protected Data compute(Data[] datums)
            throws TypeException, VisADException, RemoteException {

        Field energyProfile = (Field) datums[0];
        Data  cape          = noData;

        if (energyProfile != null) {
            Util.vetType(MassicEnergy.getRealType(), energyProfile);

            Real lfc = (Real) datums[1];

            if (lfc != null) {
                Util.vetType(AirPressure.getRealType(), lfc);

                Real lnb = (Real) datums[2];

                if (lnb != null) {
                    Util.vetType(AirPressure.getRealType(), lnb);

                    FunctionType funcType =
                        (FunctionType) energyProfile.getType();
                    RealTupleType domainType = funcType.getDomain();

                    if ( !Pressure.getRealType().equalsExceptNameButUnits(
                            domainType)) {
                        throw new TypeException(domainType.toString());
                    }

                    /*
                     * CAPE is the difference in the massic energy profile
                     * between the LNB (natural level buoyancy) and the LFC (level of free convection).
                     */

                    //Data num = VisADMath.subtract(energyProfile.evaluate(lfc), energyProfile.evaluate(lnb));
                    //logger.info("Num: " + num);
                    //Data num = VisADMath.subtract(energyProfile.evaluate(lfc), energyProfile.evaluate(lnb));
                    // logger.info("{} ", num);
                    //Data frac = VisADMath.divide(num, energyProfile.evaluate(lfc));
                    //logger.info("{} ", frac);
                    //Real value = (Real) Util.clone(frac, ucar.visad.quantities.CAPE.getRealType());

                    Real value = (Real) Util.clone(VisADMath.subtract(energyProfile.evaluate(lfc), energyProfile.evaluate(lnb)), ucar.visad.quantities.CAPE.getRealType());

                    logger.info("Val: {}", value);
                    //cape = value;
                    if (value.getValue() >= 0) {
                        cape = value;
                    }
                }
            }
        }

        return cape;
    }
}







