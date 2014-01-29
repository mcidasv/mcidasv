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

package edu.wisc.ssec.mcidasv.display.hydra;

import java.rmi.RemoteException;

import ucar.unidata.util.LogUtil;

import visad.CellImpl;
import visad.ConstantMap;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.Display;
import visad.Gridded1DSet;
import visad.Gridded2DSet;
import visad.LocalDisplay;
import visad.Real;
import visad.RealTupleType;
import visad.RealType;
import visad.VisADException;

import edu.wisc.ssec.mcidasv.data.hydra.GrabLineRendererJ3D;

    public class DragLine extends CellImpl {
    	
        private final String selectorId = hashCode() + "_selector";
        private final String lineId = hashCode() + "_line";
        private final String controlId;

        private ConstantMap[] mappings = new ConstantMap[5];

        private DataReference line;

        private DataReference selector;

        private RealType domainType;

        private RealTupleType tupleType;

        private LocalDisplay display;

        private float[] YRANGE;

        protected float lastSelectedValue;

        public DragLine(Gridded1DSet domain, RealType domainType, RealType rangeType,
            final float lastSelectedValue, LocalDisplay display, final String controlId, 
            final ConstantMap[] color, float[] YRANGE) throws Exception 
        {
            if (controlId == null)
                throw new NullPointerException("must provide a non-null control ID");
            if (color == null)
                throw new NullPointerException("must provide a non-null color");

            this.controlId = controlId;
            this.YRANGE = YRANGE;
            this.display = display;
            this.domainType = domainType;
    

            for (int i = 0; i < color.length; i++) {
                mappings[i] = (ConstantMap)color[i].clone();
            }
            mappings[4] = new ConstantMap(-0.5, Display.YAxis);


            tupleType = new RealTupleType(domainType, rangeType);

            selector = new DataReferenceImpl(selectorId);
            line = new DataReferenceImpl(lineId);
            
            display.addReferences(new GrabLineRendererJ3D(domain), new DataReference[] { selector }, new ConstantMap[][] { mappings });
            display.addReference(line, cloneMappedColor(color));

            addReference(selector);
        }

        private static ConstantMap[] cloneMappedColor(final ConstantMap[] color) throws Exception {
            assert color != null && color.length >= 3 : color;
            return new ConstantMap[] { 
                (ConstantMap)color[0].clone(),
                (ConstantMap)color[1].clone(),
                (ConstantMap)color[2].clone(),
            };
        }

        public void annihilate() {
            try {
                display.removeReference(selector);
                display.removeReference(line);
            } catch (Exception e) {
                LogUtil.logException("DragLine.annihilate", e);
            }
        }

        public String getControlId() {
            return controlId;
        }

        /**
         * Handles drag and drop updates.
         */
        public void doAction() throws VisADException, RemoteException {
            setSelectedValue(getSelectedValue());
        }

        public float getSelectedValue() {
            float val = (float)display.getDisplayRenderer().getDirectAxisValue(domainType);
            if (Float.isNaN(val))
                val = lastSelectedValue;
            return val;
        }

        public void setSelectedValue(final float val) throws VisADException,
            RemoteException 
        {
            // don't do work for stupid values
            if ((Float.isNaN(val)) 
                || (selector.getThing() != null && val == lastSelectedValue))
                return;

            line.setData(new Gridded2DSet(tupleType,
                new float[][] { { val, val }, { YRANGE[0], YRANGE[1] } }, 2));

            selector.setData(new Real(domainType, val));
            lastSelectedValue = val;
            this.update();
        }

        //- applications can extend and override
        public void update() {

        }
    }
