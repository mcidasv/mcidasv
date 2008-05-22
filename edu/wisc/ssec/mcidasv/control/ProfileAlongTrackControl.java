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

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataDataChoice;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.GeoSelectionPanel;
import ucar.unidata.data.GeoSelection;

import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.DisplayControl;

import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Displayable;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.TextDisplayable;

import visad.*;
import visad.VisADException;
import visad.RemoteVisADException;
import visad.ReferenceException;
import visad.QuickSort;

import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
//-import edu.wisc.ssec.mcidasv.data.hydra.MyRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionDataSource;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset;
import edu.wisc.ssec.mcidasv.data.hydra.SetNDAdapter;

import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ViewDescriptor;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.Range;
import ucar.unidata.util.Misc;
import ucar.unidata.util.GuiUtils;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;
                                                                                                                                             
import javax.swing.*;
import javax.swing.event.*;


import java.util.HashMap;

import java.awt.Color;
import java.awt.Font;


import java.rmi.RemoteException;



public class ProfileAlongTrackControl extends DisplayControlImpl {

  private DataChoice dataChoice;
  
  private DisplayableData imageDisplay;
  private DisplayableData trackDisplay;
  private DisplayableData meshDisplay;
  private DisplayableData textDisplay;

  private DisplayMaster mainViewMaster;

  private RealType imageRangeType;

  public MultiDimensionSubset subset;

  private MultiDimensionDataSource dataSource;

  private FlatField track;

  private GeoSelection geoSelection;

  private GeoSelectionPanel geoSelectionPanel;


  public ProfileAlongTrackControl() {
    super();
    setAttributeFlags(FLAG_COLORTABLE | FLAG_SELECTRANGE);
  }

  public boolean init(DataChoice dataChoice) throws VisADException, RemoteException {
    this.dataChoice = dataChoice;
    dataSource = (MultiDimensionDataSource) ((DirectDataChoice)dataChoice).getDataSource();
    ViewManager vm = getViewManager();
    mainViewMaster = vm.getMaster();
    subset = (MultiDimensionSubset) dataChoice.getDataSelection();
    subset.setGeoSelection(getDataSelection().getGeoSelection());
    FlatField image = (FlatField) dataSource.getData(dataChoice, null, dataSource.getProperties());
    if (image == null) {
      return false;
    }
    imageRangeType = (RealType) ((FunctionType)image.getType()).getRange();
    track = createTrackDisplay();
    imageDisplay = create3DDisplay(image);
    addDisplayable(imageDisplay, FLAG_COLORTABLE | FLAG_SELECTRANGE);
    if (track != null) create3DMesh(track);
    return true;
  }

  private FlatField createTrackDisplay() throws VisADException, RemoteException {
    IntegratedDataViewer idv = getIdv();
    FlatField track = null;

    HashMap map = dataSource.getSubsetFromLonLatRect(subset, getDataSelection().getGeoSelection());
    track = dataSource.track_adapter.getData(map);

    LineDrawing trackDsp = new LineDrawing("track");
    trackDsp.setLineWidth(2f);
    trackDsp.setData(track);
    mainViewMaster.addDisplayable(trackDsp);

    trackDisplay = trackDsp;
    return track;
  }

  private DisplayableData create3DDisplay(FlatField image) throws VisADException, RemoteException {
    RealType imageRangeType = (RealType) ((FunctionType)image.getType()).getRange();
    //-MyRGBDisplayable imageDsp = new MyRGBDisplayable("image", imageRangeType, null, true);
    HydraRGBDisplayable imageDsp = new HydraRGBDisplayable("image", imageRangeType, null, true, null);
    imageDsp.setData(image);
    return imageDsp;
  }

  private void create3DMesh(FlatField track) throws VisADException, RemoteException {
    float del_lat = 2f;
    int n_sets = 3;
    Gridded3DSet set = (Gridded3DSet) track.getDomainSet();

    float[][] samples = set.getSamples();
    SampledSet[] sets = new SampledSet[n_sets];
    Tuple[] labels = new Tuple[n_sets];
    float alt_start = 2000;
    float alt_inc = 5000;
    for (int k=0; k<n_sets; k++) {
      for (int i=0; i<samples[2].length; i++) {
        samples[2][i] = alt_start + k*alt_inc;
      }
      sets[k] = new Gridded3DSet(RealTupleType.SpatialEarth3DTuple, samples, samples[2].length);
      Tuple tup = new Tuple(new TupleType(new MathType[] {RealTupleType.SpatialEarth3DTuple, TextType.Generic}),
            new Data[] {new RealTuple(RealTupleType.SpatialEarth3DTuple, new double[] {samples[0][0], samples[1][0] - del_lat, samples[2][0]}), new Text(TextType.Generic, Float.toString(samples[2][0]))});
      labels[k] = tup;
    }

    UnionSet u_set = new UnionSet(sets);
    LineDrawing meshDsp = new LineDrawing("mesh");
    meshDsp.setLineWidth(2f);
    meshDsp.setData(u_set);
    mainViewMaster.addDisplayable(meshDsp);

    TextDisplayable txtDsp = new TextDisplayable(TextType.Generic);
    txtDsp.setData(new Tuple(labels));
    txtDsp.setLineWidth(2f);
    mainViewMaster.addDisplayable(txtDsp);

    meshDisplay = meshDsp;
    textDisplay = txtDsp;
    
    return;
  }

  private DisplayableData create2DDisplay() throws VisADException, RemoteException {
    return null;
  }

  protected ColorTable getInitialColorTable() {
    return getDisplayConventions().getParamColorTable(imageRangeType.getName());
  }

  protected Range getInitialRange() throws RemoteException, VisADException {
      Range range = getDisplayConventions().getParamRange(imageRangeType.getName(), null);
                          //getDisplayUnit());
        setSelectRange(range);
        return range;
  }

  public void doRemove() throws RemoteException, VisADException{
    mainViewMaster.removeDisplayable(meshDisplay);
    mainViewMaster.removeDisplayable(trackDisplay);
    mainViewMaster.removeDisplayable(textDisplay);
    super.doRemove();
  }

  public Container doMakeContentes() {
    return null;
  }

}
