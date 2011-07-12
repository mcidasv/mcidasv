package edu.wisc.ssec.mcidasv.display.hydra;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import ucar.unidata.util.LogUtil;
import edu.wisc.ssec.mcidasv.data.hydra.GrabLineRendererJ3D;
import visad.util.Util;


import visad.*;


    public class DragLine extends CellImpl {
        private final String selectorId = hashCode() + "_selector";
        private final String lineId = hashCode() + "_line";
        private final String controlId;

        private ConstantMap[] mappings = new ConstantMap[5];

        private DataReference line;

        private DataReference selector;

        private RealType domainType;
        private RealType rangeType;

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
