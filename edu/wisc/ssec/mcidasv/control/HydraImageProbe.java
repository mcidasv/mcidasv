/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.control;

import ucar.unidata.idv.control.LineProbeControl;

import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.ControlContext;


import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.ThreeDSize;




import ucar.visad.display.Displayable;
import ucar.visad.display.ProfileLine;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.display.XYDisplay;


import visad.ActionImpl;
import visad.CommonUnit;
import visad.CoordinateSystem;
import visad.Data;
import visad.ErrorEstimate;
import visad.FieldImpl;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded1DSet;
import visad.MathType;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.Set;
import visad.SetType;
import visad.Unit;
import visad.VisADException;
import visad.DataReference;

import visad.data.units.Parser;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import visad.util.DataUtility;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class HydraImageProbe extends LineProbeControl {
    DataReference positionRef = null;

    public HydraImageProbe() {
      super();
    }

    protected void probePositionChanged(RealTuple position) { 
        try {
            double[] values = position.getValues();
            EarthLocationTuple elt =
                (EarthLocationTuple) boxToEarth(new double[] { values[0],
                    values[1], 1.0 });
            LatLonPoint llp = elt.getLatLonPoint();
            if (positionRef != null) {
              positionRef.setData(llp);
            }
        }
        catch (Exception e) {
          System.out.println("HydraImageProbe "+e.getMessage());
        }

    }

    public void loadProfile(RealTuple position) throws VisADException,
            RemoteException {
      System.out.println("HydraImageProbe.loadProfile");
    }

    public void setPositionRef(DataReference positionRef) {
      this.positionRef = positionRef;
    }

}
