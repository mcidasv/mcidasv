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
package edu.wisc.ssec.mcidasv.control;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataDataChoice;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataCategory;

import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.DisplayControl;

import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Displayable;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.RGBDisplayable;

import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.NearCastTrajDataSource;

import visad.*;
import visad.BaseColorControl;
import visad.VisADException;
import visad.RemoteVisADException;
import visad.ScalarMapListener;
import visad.ScalarMapControlEvent;
import visad.ScalarMapEvent;
import visad.ReferenceException;
import visad.georef.MapProjection;
import visad.georef.TrivialMapProjection;


import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ViewDescriptor;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.Range;
import ucar.unidata.util.Misc;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;

import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;


import javax.swing.*;
import javax.swing.event.*;


import java.util.HashMap;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import java.awt.Color;
import java.awt.Font;


import java.rmi.RemoteException;


public class NearCastTrajControl extends DisplayControlImpl {

   /** Displayable for the data */
   LineDrawing trajDisplay;

   private DisplayMaster displayMaster;

   FieldImpl trajField = null;

   RealType rgbRealType = null;
   String paramName = null;

   NearCastTrajDataSource dataSource;

   private DisplayableData rgbDisp;

   float lineWidth = 1.0f;


   public NearCastTrajControl() {
     super();
     setAttributeFlags(FLAG_COLORTABLE | FLAG_LINEWIDTH);
   }

   public boolean init(DataChoice dataChoice) throws VisADException, RemoteException {
     displayMaster = getViewManager().getMaster();
     dataSource = (NearCastTrajDataSource) ((DirectDataChoice) dataChoice).getDataSource();

     DataSelection dataSelection = getDataSelection();
     trajField = (FieldImpl) dataChoice.getData(dataSelection);

     FlatField fltFld = null;
     if (GridUtil.isSequence(trajField)) {
       FieldImpl fld = (FieldImpl) trajField.getSample(0);
       if (GridUtil.isSequence(fld)) {
         fltFld = (FlatField)  fld.getSample(0);
       }
     }
     else {
       fltFld = (FlatField) trajField;
     }
     rgbRealType = (RealType) ((FunctionType)fltFld.getType()).getRange();
     paramName = rgbRealType.getName();

     rgbDisp = new RGBDisplayableImpl("traj", rgbRealType, null, true);
     rgbDisp.setData(trajField);

     addDisplayable(rgbDisp, FLAG_COLORTABLE);

     return true;
   }

   public void initDone() {
   }

   protected void applyLineWidth() throws VisADException, RemoteException {
     float lw = getLineWidth();
     if (lw != lineWidth) {
       rgbDisp.setLineWidth(lw);
       lineWidth = lw;
     }
   }

   protected Range getInitialRange() throws VisADException, RemoteException {
     //Range rng = getDisplayConventions().getParamRange(paramName, null);
     Range rng = null;
     if (rng == null) {
       return dataSource.getParamRange();
     }
     return rng;
   }

   /**
   protected ColorTable getInitialColorTable() {
     return getDisplayConventions().getParamColorTable("image");
   }
   */

   private class RGBDisplayableImpl extends RGBDisplayable {
       RGBDisplayableImpl(String name, RealType rgbRealType, float[][] colorPalette, boolean alphaflag)
           throws VisADException, RemoteException {
         super(name, rgbRealType, colorPalette, alphaflag);
       }
    }

    @Override public MapProjection getDataProjection() {
        MapProjection mp = null;
        Range lonRange = dataSource.getLonRange();
        Range latRange = dataSource.getLatRange();
        float delLon = (float) (lonRange.getMax() - lonRange.getMin());
        float delLat = (float) (latRange.getMax() - latRange.getMin());

        try {
           mp = new TrivialMapProjection(RealTupleType.SpatialEarth2DTuple, 
                   new Rectangle2D.Float((float)lonRange.getMin(), (float)latRange.getMin(), 
                                         delLon, delLat));
        } catch (Exception e) {
            logException("MultiSpectralControl.getDataProjection", e);
        }

        return mp;
    }


}
