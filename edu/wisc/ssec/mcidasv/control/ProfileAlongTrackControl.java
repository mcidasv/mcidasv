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

import java.awt.Container;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Color;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.text.DecimalFormat;

import visad.Data;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.FlatField;
import visad.Real;
import visad.FunctionType;
import visad.Integer1DSet;
import visad.GriddedSet;
import visad.Gridded1DSet;
import visad.Gridded3DSet;
import visad.MathType;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.Set;
import visad.SampledSet;
import visad.SimpleSet;
import visad.Text;
import visad.TextType;
import visad.Tuple;
import visad.TupleType;
import visad.UnionSet;
import visad.ScalarMap;
import visad.Display;
import visad.LocalDisplay;
import visad.ConstantMap;
import visad.VisADException;
import visad.VisADGeometryArray;
import visad.georef.LatLonTuple;
import visad.georef.EarthLocationTuple;
import visad.util.Util;

import ucar.unidata.idv.control.ControlWidget;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.data.GeoSelectionPanel;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Range;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.TextDisplayable;
import ucar.visad.display.XYDisplay;
import ucar.visad.display.SelectorPoint;
import ucar.visad.ShapeUtility;

import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionDataSource;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset;
import edu.wisc.ssec.mcidasv.data.hydra.GrabLineRendererJ3D;
import edu.wisc.ssec.mcidasv.display.hydra.DragLine;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;



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

  private XYDisplay display2D = null;

  private SelectorPoint locOnTrack;

  private DecimalFormat numFmt = new DecimalFormat();


  public ProfileAlongTrackControl() {
    super();
    setAttributeFlags(FLAG_COLORTABLE | FLAG_SELECTRANGE);
  }

  public boolean init(DataChoice dataChoice) throws VisADException, RemoteException {
    this.dataChoice = dataChoice;
    FlatField data;

    if (dataChoice instanceof DerivedDataChoice) {
      data = (FlatField) dataChoice.getData(getDataSelection());
    }
    else {
      dataSource = (MultiDimensionDataSource) ((DirectDataChoice)dataChoice).getDataSource();
      ViewManager vm = getViewManager();
      mainViewMaster = vm.getMaster();

      Hashtable table = dataChoice.getProperties();
      Enumeration keys = table.keys();
      while (keys.hasMoreElements()) {
        Object key = keys.nextElement();
        if (key instanceof MultiDimensionSubset) {
           subset = (MultiDimensionSubset) table.get(key);
        }
      }
      subset.setGeoSelection(getDataSelection().getGeoSelection());

      data = (FlatField) dataSource.getData(dataChoice, null, getDataSelection(), dataSource.getProperties());
    }

    if (data == null) {
      return false;
    }

    imageRangeType = (RealType) ((FunctionType)data.getType()).getRange();
    track = createTrackDisplay(dataChoice);
    imageDisplay = create3DDisplay(data);
    addDisplayable(imageDisplay, FLAG_COLORTABLE | FLAG_SELECTRANGE);
    if (track != null) create3DMesh(track);

    // 2D Display in Control Window, only line graph type display for now
    if (((SimpleSet)data.getDomainSet()).getManifoldDimension() == 1) {
      display2D = makeDisplay2D(data);
    }

    return true;
  }

  public synchronized void dataChanged() {
    super.dataChanged();
  }

  private FlatField createTrackDisplay(DataChoice dataChoice) throws VisADException, RemoteException {
    IntegratedDataViewer idv = getIdv();
    FlatField track = null;

    dataChoice = dataSource.findDataChoice("Track3D");
    if (dataChoice == null) {
       return null;
    }

    track = (FlatField) dataSource.getData(dataSource.findDataChoice("Track3D"), null, getDataSelection(), dataSource.getProperties());

    LineDrawing trackDsp = new LineDrawing("track");
    trackDsp.setLineWidth(2f);
    trackDsp.setData(track.getDomainSet());
    mainViewMaster.addDisplayable(trackDsp);

    // ??? setConstantPosition(val, display real type) ??
    locOnTrack = new SelectorPoint("marker", new EarthLocationTuple(10, 10, 0));
//    locOnTrack.setMarker(ShapeUtility.makeShape(ShapeUtility.CROSS));
    VisADGeometryArray[] markerShape = ShapeUtility.createShape(ShapeUtility.CROSS);
    locOnTrack.setMarker(markerShape[0]);
    mainViewMaster.addDisplayable(locOnTrack);
    locOnTrack.setScale(0.1f);

    trackDisplay = trackDsp;
    return track;
  }

  private DisplayableData create3DDisplay(FlatField data) throws VisADException, RemoteException {
    RealType imageRangeType = (RealType) ((FunctionType)data.getType()).getRange();
    HydraRGBDisplayable imageDsp = new HydraRGBDisplayable("image", imageRangeType, (RealType) null, true, null);
    imageDsp.setDefaultRenderer();
    imageDsp.setData(data);
    return imageDsp;
  }

  private void create3DMesh(FlatField track) throws VisADException, RemoteException {
    float del_lat = 2f;
    int n_sets = 3;
    GriddedSet set = (GriddedSet) track.getDomainSet();

    float[][] samples = set.getSamples();
    float[][] samples3D = new float[][] {samples[0], samples[1], new float[samples[0].length]};

    SampledSet[] sets = new SampledSet[n_sets];
    Tuple[] labels = new Tuple[n_sets];
    float alt_start = 2000;
    float alt_inc = 5000;
    for (int k=0; k<n_sets; k++) {
      for (int i=0; i<samples3D[2].length; i++) {
        samples3D[2][i] = alt_start + k*alt_inc;
      }
      sets[k] = new Gridded3DSet(RealTupleType.SpatialEarth3DTuple, samples3D, samples3D[2].length);
      Tuple tup = new Tuple(new TupleType(new MathType[] {RealTupleType.SpatialEarth3DTuple, TextType.Generic}),
            new Data[] {new RealTuple(RealTupleType.SpatialEarth3DTuple, 
                  new double[] {samples3D[0][0], samples3D[1][0] - del_lat, samples3D[2][0]}), 
                          new Text(TextType.Generic, Float.toString(samples3D[2][0]))});
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

  private XYDisplay makeDisplay2D(final FlatField data) throws VisADException, RemoteException {
    
    FunctionType fncType = (FunctionType) data.getType();

    RealType domainType = RealType.Generic;
    RealType rangeType = (RealType) fncType.getRange();

    final Set domainSet = data.getDomainSet();
    int len = domainSet.getLength();
    Integer1DSet newDomain = new Integer1DSet(len);
    FlatField newFF = new FlatField(new FunctionType(RealType.Generic, rangeType), newDomain);
    newFF.setSamples(data.getFloats());

    XYDisplay master = new XYDisplay("2D disp", domainType, rangeType);

    master.showAxisScales(true);
    master.setAspect(2.5, 0.75);
    double[] proj = master.getProjectionMatrix();
    proj[0] = 0.35;
    proj[5] = 0.35;
    proj[10] = 0.35;
    master.setProjectionMatrix(proj);

    ScalarMap xmap = new ScalarMap(domainType, Display.XAxis);
    ScalarMap ymap = new ScalarMap(rangeType, Display.YAxis);
    ScalarMap txtMap = new ScalarMap(TextType.Generic, Display.Text);

    LocalDisplay display = master.getDisplay();
    display.addMap(xmap);
    display.addMap(ymap);
    display.addMap(txtMap);

    DataReference dataRef = new DataReferenceImpl("data");
    dataRef.setData(newFF);
    display.addReference(dataRef);

    final DataReference txtRef = new DataReferenceImpl("text");
    display.addReference(txtRef, new ConstantMap[] {new ConstantMap(0.9, Display.YAxis)});


    class MyDragLine extends DragLine {
      public MyDragLine(Gridded1DSet domain, RealType domainType, RealType rangeType,
            final float lastSelectedValue, LocalDisplay display, final String controlId,
            final ConstantMap[] color, float[] YRANGE) throws Exception {
        super(domain, domainType, rangeType, lastSelectedValue, display, controlId, color, YRANGE);
      }

      public void update() {
         int idx = (new Float(this.lastSelectedValue)).intValue();
         try {
           float[][] val = domainSet.indexToValue(new int[] {idx});
           locOnTrack.setPoint(new EarthLocationTuple(val[1][0], val[0][0], 0));
           float rangeVal = (float) ((Real)data.getSample(idx)).getValue();
           Tuple tup = new Tuple(new Data[] {new Real(RealType.Generic, (double) idx), new Text(TextType.Generic, numFmt.format(rangeVal))});
           txtRef.setData(tup);
           
         } catch (Exception e) {
           System.out.println(e);
         }
      }
    }

    try {
        MyDragLine draggable = new MyDragLine(newDomain, domainType, rangeType, 100f, display, 
            "dragLine", MultiSpectralDisplay.makeColorMap(Color.GREEN), new float[] {0, 16});
    } catch (Exception e) {
      e.printStackTrace();
    }

    return master;
  }

  protected ColorTable getInitialColorTable() {
    return getDisplayConventions().getParamColorTable(imageRangeType.getName());
  }

  protected Range getInitialRange() throws RemoteException, VisADException {
      Range range = getDisplayConventions().getParamRange(imageRangeType.getName(), null);
      if (range != null) {
        setSelectRange(range);
        return range;
      }
      else {
        return super.getInitialRange();
      }
  }

  public void doRemove() throws RemoteException, VisADException{
    
    if (meshDisplay != null) mainViewMaster.removeDisplayable(meshDisplay);
    if (textDisplay != null) mainViewMaster.removeDisplayable(textDisplay);
    if (trackDisplay != null) mainViewMaster.removeDisplayable(trackDisplay);
    super.doRemove();
  }

  public void setDisplayVisibility(boolean on) {
    super.setDisplayVisibility(on);
    try {
      if (meshDisplay != null) meshDisplay.setVisible(on);
      if (textDisplay != null) textDisplay.setVisible(on);
      if (trackDisplay != null) trackDisplay.setVisible(on);
    }
    catch( Exception e) {
      e.printStackTrace();
    }
  }

  public Container doMakeContents() {
        try {
            JTabbedPane pane = new JTabbedPane();
            if (display2D != null) {
              pane.add("Display", GuiUtils.inset(display2D.getDisplayComponent(), 5));
            }
            pane.add("Settings",
                     GuiUtils.inset(GuiUtils.top(doMakeWidgetComponent()), 5));
            GuiUtils.handleHeavyWeightComponentsInTabs(pane);
            return pane;
        } catch (Exception e) {
            logException("MultiSpectralControl.doMakeContents", e);
        }
        return null;
  }

  protected JComponent doMakeWidgetComponent() {
        List<Component> widgetComponents;
        try {
            List<ControlWidget> controlWidgets = new ArrayList<ControlWidget>();
            getControlWidgets(controlWidgets);
            widgetComponents = ControlWidget.fillList(controlWidgets);
        } catch (Exception e) {
            LogUtil.logException("Problem building the ProfileAlongTrackControl settings", e);
            widgetComponents = new ArrayList<Component>();
            widgetComponents.add(new JLabel("Error building component..."));
        }

        GuiUtils.tmpInsets = new Insets(4, 8, 4, 8);
        GuiUtils.tmpFill = GridBagConstraints.HORIZONTAL;
        return GuiUtils.doLayout(widgetComponents, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
    }

  private JComponent getDisplayTab() {
     return null;
  }
}
