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

import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.HistogramField;
import edu.wisc.ssec.mcidasv.data.hydra.SubsetRubberBandBox;
import edu.wisc.ssec.mcidasv.data.hydra.MyRubberBandBoxRendererJ3D;
import edu.wisc.ssec.mcidasv.data.hydra.CurveDrawer;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset;
import edu.wisc.ssec.mcidasv.data.hydra.HydraContext;
import edu.wisc.ssec.mcidasv.control.LambertAEA;

import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import ucar.unidata.util.Range;
import ucar.unidata.util.ColorTable;
import java.net.URL;

import java.awt.Container;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.ColorTableWidget;
import ucar.unidata.ui.colortable.ColorTableManager;

import ucar.unidata.data.DataChoice;
import ucar.unidata.util.ColorTable;
import visad.VisADException;
import visad.AxisScale;
import visad.FlatField;
import visad.FieldImpl;
import visad.CoordinateSystem;
import visad.Data;
import visad.RealType;
import visad.CellImpl;
import visad.Integer1DSet;
import visad.RealTupleType;
import visad.FunctionType;
import visad.ScalarMap;
import visad.Gridded3DSet;
import visad.Gridded2DSet;
import visad.Linear2DSet;
import visad.SampledSet;
import visad.Set;
import visad.SetType;
import visad.UnionSet;
import visad.BaseColorControl;
import visad.DisplayListener;
import visad.DisplayEvent;
import visad.DelaunayCustom;
import visad.georef.MapProjection;
import visad.data.mcidas.BaseMapAdapter;

import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
                                                                                                                                          
import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.XYDisplay;
import ucar.visad.display.RGBDisplayable;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.RubberBandBox;
import ucar.visad.display.MapLines;

import ucar.unidata.idv.DisplayConventions;


public class ScatterDisplay extends DisplayControlImpl {
    
    private Container container;
    private FlatField X_field;
    private FlatField Y_field;
    private DisplayMaster scatterMaster = null;

    private DisplayMaster dspMasterX;
    private DisplayMaster dspMasterY;

    private HistogramField histoField;

    private FlatField mask_field;
    private float[][] mask_range;
    private float[][] scatterFieldRange;
    private Data X_data;
    private Data Y_data;
    private String X_name;
    private String Y_name;

    private ScatterDisplayable scatterMarkDsp;

    private RGBDisplayable maskX;
    private RGBDisplayable maskY;

    private BoxCurveSwitch boxCurveSwitch;

    public DataChoice dataChoiceX = null;
    public DataChoice dataChoiceY = null;
    public DataSelection dataSelectionX = null;
    public DataSelection dataSelectionY = null;

    JComponent ctwCompX;
    JComponent ctwCompY;
    ColorTableWidget ctw;

    public ScatterDisplay() {
      super();
    }
    

    @Override public boolean init(List choices) throws VisADException, RemoteException {
        if ((dataChoiceX != null) && (dataChoiceY != null)) {
          setupFromUnpersistence();
        }
        else {
          setup();
        }

        mask_field = new FlatField(
             new FunctionType(((FunctionType)X_field.getType()).getDomain(), RealType.Generic),
                  X_field.getDomainSet());

        int len = X_field.getDomainSet().getLength();
        mask_range = new float[1][len];
        for (int t=0; t<len; t++) {
          mask_range[0][t] = Float.NaN;
        }
        mask_range[0][0] = 0; //- field should not be all missing
        mask_field.setSamples(mask_range, false);
                                                                                                                                                  
        try {
          histoField = new HistogramField(X_field, Y_field, mask_field, 100, 10);
        }
        catch (Exception e) {
          e.printStackTrace();
        }

        Range rangeX = getImageRange(X_field);
        Range rangeY = getImageRange(Y_field);
        ColorTable clrTableX = getColorTable(X_field);
        ColorTable clrTableY = getColorTable(Y_field);

        dspMasterX = makeImageDisplay(getDataProjection(X_field), X_field, mask_field, 
                         rangeX, clrTableX);

        dspMasterY = makeImageDisplay(getDataProjection(Y_field), Y_field, mask_field, 
                         rangeY, clrTableY);

        dspMasterX.addDisplayListener(new DisplayListener() {
            public void displayChanged(final DisplayEvent e) {
                double[] xProjection = dspMasterX.getProjectionMatrix();
                double[] yProjection = dspMasterY.getProjectionMatrix();
                if (xProjection.equals(yProjection))
                    return;

                try {
                    dspMasterY.setProjectionMatrix(xProjection);
                } catch (Exception ex) {
                    LogUtil.logException("dspMasterX.displayChanged", ex);
                }
            }
        });

        dspMasterY.addDisplayListener(new DisplayListener() {
            public void displayChanged(final DisplayEvent e) {
                double[] xProjection = dspMasterX.getProjectionMatrix();
                double[] yProjection = dspMasterY.getProjectionMatrix();
                if (yProjection.equals(xProjection))
                    return;

                try {
                    dspMasterX.setProjectionMatrix(yProjection);
                } catch (Exception ex) {
                    LogUtil.logException("dspMasterX.displayChanged", ex);
                }
            }
        });

        X_name = ((((FunctionType)X_field.getType()).getFlatRange().getRealComponents())[0]).getName();
        Y_name = ((((FunctionType)Y_field.getType()).getFlatRange().getRealComponents())[0]).getName();

        Grid2DReadoutProbe probeX = new Grid2DReadoutProbe(X_field, dspMasterX);
        Grid2DReadoutProbe probeY = new Grid2DReadoutProbe(Y_field, dspMasterY);
        probeX.doMakeProbe(Color.red, dspMasterX);
        probeY.doMakeProbe(Color.red, dspMasterY);
	
	ctw = new ColorTableWidget(
                 new ImageControl((HydraRGBDisplayable)dspMasterX.getDisplayables(3), getDisplayConventions()),
                    ColorTableManager.getManager(), clrTableX, rangeX);
	ctwCompX = ctw.getLegendPanel(BOTTOM_LEGEND);

	ctw = new ColorTableWidget(
                 new ImageControl((HydraRGBDisplayable)dspMasterY.getDisplayables(3), getDisplayConventions()),
                    ColorTableManager.getManager(), clrTableY, rangeY);
	ctwCompY = ctw.getLegendPanel(BOTTOM_LEGEND);


        return true;
    }

    public void setup() throws VisADException, RemoteException {
        dataSelectionX = getDataSelection();
        dataChoiceX = getDataChoice();
        X_data = dataChoiceX.getData(dataSelectionX);

        if (X_data instanceof FlatField) {
          X_field = (FlatField) X_data;
        } else if (X_data instanceof FieldImpl) {
          X_field = (FlatField) ((FieldImpl)X_data).getSample(0);
        }

        this.popupDataDialog("select Y Axis field", container, false, null);


        dataSelectionY = getDataSelection();
        dataChoiceY = getDataChoice();

        dataSelectionY.setGeoSelection(dataSelectionX.getGeoSelection());
                                                                                                                                                  
        Y_data = dataChoiceY.getData(dataSelectionY);

        if (Y_data instanceof FlatField) {
          Y_field = (FlatField) Y_data;
        } else if (X_data instanceof FieldImpl) {
          Y_field = (FlatField) ((FieldImpl)Y_data).getSample(0);
        }
    }

    public void setupFromUnpersistence() throws VisADException, RemoteException {
        X_data = dataChoiceX.getData(dataSelectionX);
        if (X_data instanceof FlatField) {
          X_field = (FlatField) X_data;
        } else if (X_data instanceof FieldImpl) {
          X_field = (FlatField) ((FieldImpl)X_data).getSample(0);
        }
                                                                                                                                                  
        Y_data = dataChoiceY.getData(dataSelectionY);
        if (Y_data instanceof FlatField) {
          Y_field = (FlatField) Y_data;
        } else if (X_data instanceof FieldImpl) {
          Y_field = (FlatField) ((FieldImpl)Y_data).getSample(0);
        }
    }

    protected void popupDataDialog(final String dialogMessage,
                                   Component from, boolean multiples,
                                   List categories) {

        List<DataChoice> choices = selectDataChoices(dialogMessage, from,
                                       multiples, categories);
        if ((choices == null) || (choices.size() == 0)) {
            return;
        }
        final List clonedList =
            DataChoice.cloneDataChoices((List) choices.get(0));
        dataSelection = ((DataChoice) clonedList.get(0)).getDataSelection();
        //- don't do this in a separate thread like the IDV does.
        //- We want the dataChoice list updated before return.
        try {
          addNewData(clonedList);
        } catch (Exception exc) {
          logException("Selecting new data", exc);
        }
    }


    public void initDone() {
       try {
         DisplayMaster master = makeScatterDisplay();
         ScatterBoxSelector boxSelect = new ScatterBoxSelector(master);
         ScatterCurveSelector curveSelect = new ScatterCurveSelector(master);
         curveSelect.setActive(false);
         curveSelect.setVisible(false);
         master.draw();

         SubsetRubberBandBox X_subsetBox =
            new SubsetRubberBandBox(getIsLatLon(X_field), X_field,
                    ((MapProjectionDisplayJ3D)dspMasterX).getDisplayCoordinateSystem(), 1, false);
         X_subsetBox.setColor(Color.magenta);
         ImageBoxSelector markX = new ImageBoxSelector(X_subsetBox, X_field.getDomainSet(), dspMasterX);
         X_subsetBox.addAction(markX);

         CurveDrawer curveDraw = new CurveDrawer(RealType.Longitude, RealType.Latitude, 1);
         curveDraw.setColor(Color.magenta);
         curveDraw.setLineWidth(2);
         ImageCurveSelector curveX = new ImageCurveSelector(curveDraw, X_field, dspMasterX);
         curveX.setActive(false);
         curveDraw.addAction(curveX);
         curveX.setVisible(false);
         dspMasterX.addDisplayable(curveDraw);


         SubsetRubberBandBox Y_subsetBox =
            new SubsetRubberBandBox(getIsLatLon(Y_field), Y_field,
               ((MapProjectionDisplayJ3D)dspMasterY).getDisplayCoordinateSystem(), 1, false);
         Y_subsetBox.setColor(Color.magenta);
         ImageBoxSelector markY = new ImageBoxSelector(Y_subsetBox, Y_field.getDomainSet(), dspMasterY);
         Y_subsetBox.addAction(markY);

         dspMasterX.addDisplayable(X_subsetBox);
         dspMasterY.addDisplayable(Y_subsetBox);

         markX.setOther(markY);
         markY.setOther(markX);

         curveDraw = new CurveDrawer(RealType.Longitude, RealType.Latitude, 1);
         curveDraw.setColor(Color.magenta);
         curveDraw.setLineWidth(2);
         ImageCurveSelector curveY = new ImageCurveSelector(curveDraw, Y_field, dspMasterY);
         curveY.setActive(false);
         curveDraw.addAction(curveY);
         curveY.setVisible(false);
         dspMasterY.addDisplayable(curveDraw);

         curveX.setOther(curveY);
         curveY.setOther(curveX);

         boxCurveSwitch.boxSelect   = boxSelect;
         boxCurveSwitch.curveSelect = curveSelect;
         boxCurveSwitch.markX = markX;
         boxCurveSwitch.markY = markY;
         boxCurveSwitch.curveX = curveX;
         boxCurveSwitch.curveY = curveY;

       }
       catch (Exception e) {
         e.printStackTrace();
       }
    }
    
    public DisplayMaster makeScatterDisplay() throws VisADException, RemoteException {

       ScatterDisplayable scatterDsp = new ScatterDisplayable("scatter",
                   RealType.getRealType("mask"), new float[][] {{1,1},{1,0},{1,1}}, false);
       float[] valsX = X_field.getFloats(false)[0];
       float[] valsY = Y_field.getFloats(false)[0];
       Integer1DSet set = new Integer1DSet(valsX.length);
       FlatField scatter = new FlatField(
           new FunctionType(RealType.Generic,
               new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), set);
       float[] mask = new float[valsX.length];
       for (int k=0; k<mask.length; k++) mask[k] = 0;
       scatterFieldRange = new float[][] {valsX, valsY, mask};
       scatter.setSamples(scatterFieldRange);
       scatterDsp.setPointSize(2f);
       scatterDsp.setRangeForColor(0,1);
       float[] xRange = minmax(valsX);
       float[] yRange = minmax(valsY);
       
       scatterDsp.setData(scatter);
                                                                                                                                                  
       scatterMarkDsp = new ScatterDisplayable("scatter",
                   RealType.getRealType("mask"), new float[][] {{1,1},{1,0},{1,1}}, false);
       set = new Integer1DSet(2);
       scatter = new FlatField(
           new FunctionType(RealType.Generic,
               new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), set);
       scatterMarkDsp.setData(scatter);
       scatterMarkDsp.setPointSize(2f);
       scatterMarkDsp.setRangeForColor(0,1);

       DisplayMaster master = scatterMaster;
       ((XYDisplay)master).showAxisScales(true);
       AxisScale scaleX = ((XYDisplay)master).getXAxisScale();
       scaleX.setTitle(X_name);
       AxisScale scaleY = ((XYDisplay)master).getYAxisScale();
       scaleY.setTitle(Y_name);

       ((XYDisplay)master).setXRange((double)xRange[0], (double)xRange[1]);
       ((XYDisplay)master).setYRange((double)yRange[0], (double)yRange[1]);
       master.addDisplayable(scatterDsp);
       master.addDisplayable(scatterMarkDsp);

       return master;
    }


    @Override public Container doMakeContents() {
        JPanel pane = new JPanel(new GridLayout(1,3));

        Component[] comps = new Component[] {null, null, null};
        comps[0] = dspMasterX.getComponent();
        comps[1] = dspMasterY.getComponent();
        comps[2] = getScatterTabComponent();

        JPanel panelX = new JPanel(new BorderLayout());
        panelX.setBorder(new EmptyBorder(4,4,4,4));
        panelX.add(comps[0], BorderLayout.CENTER);
        panelX.add(ctwCompX, BorderLayout.SOUTH);

        JPanel panelY = new JPanel(new BorderLayout());
        panelY.setBorder(new EmptyBorder(4,4,4,4));
        panelY.add(comps[1], BorderLayout.CENTER);
        panelY.add(ctwCompY, BorderLayout.SOUTH);

        JPanel panelS = new JPanel(new BorderLayout());
        panelS.setBorder(new EmptyBorder(4,4,4,4));
        panelS.add(comps[2], BorderLayout.CENTER);

        pane.add(panelX);
        pane.add(panelY);
        pane.add(panelS);

        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        JRadioButton boxSelect = new JRadioButton("Box");
        boxSelect.setSelected(true);
        JRadioButton curveSelect = new JRadioButton("Curve");
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(boxSelect);
        buttonGroup.add(curveSelect);
        buttonPanel.add(boxSelect);
        buttonPanel.add(curveSelect);

        boxCurveSwitch = new BoxCurveSwitch();
        boxSelect.addActionListener(boxCurveSwitch);
        curveSelect.addActionListener(boxCurveSwitch);
        

        JPanel new_pane = new JPanel(new BorderLayout());
        new_pane.add(pane, BorderLayout.CENTER);
        new_pane.add(buttonPanel, BorderLayout.SOUTH);

        //-container = pane;
        container = new_pane;
        return container;
    }


//    @Override public void doRemove() throws VisADException, RemoteException {
//        super.doRemove();
//    }


    protected Component getScatterTabComponent() {
       try {
         scatterMaster = new XYDisplay("Scatter", RealType.XAxis, RealType.YAxis);
       } catch (Exception e) {
         e.printStackTrace();
       }
       return scatterMaster.getComponent();
    }

    public DisplayMaster makeImageDisplay(MapProjection mapProj, FlatField image, 
                  FlatField mask_image, Range imageRange, ColorTable colorTable) 
           throws VisADException, RemoteException {
      MapProjectionDisplayJ3D mapProjDsp;
      DisplayMaster dspMaster;

      mapProjDsp = new MapProjectionDisplayJ3D(MapProjectionDisplay.MODE_2Din3D);
      mapProjDsp.enableRubberBanding(false);
      dspMaster = mapProjDsp;
      mapProjDsp.setMapProjection(mapProj);
      RealType imageRangeType =
        (((FunctionType)image.getType()).getFlatRange().getRealComponents())[0];

      HydraRGBDisplayable imageDsp = new HydraRGBDisplayable("image", imageRangeType, null, false, null);

      imageDsp.setData(image);
      addMapDisplayables(mapProjDsp);
      dspMaster.addDisplayable(imageDsp);

      if (mask_image != null) {
        RGBDisplayable maskDsp = 
            new ScatterDisplayable("mask", RealType.Generic, new float[][] {{0},{1},{0}}, false);
        maskDsp.setData(mask_image);
        dspMaster.addDisplayable(maskDsp);
      }

      dspMaster.draw();

      ScalarMap colorMap = imageDsp.getColorMap();
      colorMap.setRange(imageRange.getMin(), imageRange.getMax());
      BaseColorControl clrCntrl = (BaseColorControl) colorMap.getControl();
      clrCntrl.setTable(colorTable.getColorTable());

      return dspMaster;
    }

    public Range getImageRange(FlatField image)
           throws VisADException, RemoteException {
      DisplayConventions dc = getDisplayConventions();
      Range[] range = GridUtil.fieldMinMax(image);
      Range imageRange = range[0];
      RealType imageRangeType =
        (((FunctionType)image.getType()).getFlatRange().getRealComponents())[0];
      String canonicalName = DataAlias.aliasToCanonical(imageRangeType.getName());
      Range dfltRange = dc.getParamRange(canonicalName, null);

      if (dfltRange == null) {
        imageRange = range[0];
      }
      else if ((imageRange.getMax() - imageRange.getMin()) < (dfltRange.getMax() - dfltRange.getMin())) {
      }
      else {
        imageRange = dfltRange;
      }
      return imageRange;
    }

    public ColorTable getColorTable(FlatField image) 
           throws VisADException, RemoteException {
      RealType imageRangeType =
        (((FunctionType)image.getType()).getFlatRange().getRealComponents())[0];
      DisplayConventions dc = getDisplayConventions();
      return dc.getParamColorTable(imageRangeType.getName());
    }


    public MapProjection getDataProjection(FlatField image) 
           throws VisADException, RemoteException {
      MapProjection mp = null;
      //- get MapProjection from incoming image.  If none, use default method
      FunctionType fnc_type = (FunctionType) image.getType();
      RealTupleType rtt = fnc_type.getDomain();
      CoordinateSystem cs = rtt.getCoordinateSystem();
      if (cs instanceof MapProjection) {
        return (MapProjection) cs;
      }
      else if (cs instanceof visad.CachingCoordinateSystem) {
         CoordinateSystem cacheCS = 
              ((visad.CachingCoordinateSystem)cs).getCachedCoordinateSystem();
         if (cacheCS instanceof MapProjection) {
           return (MapProjection) cacheCS;
         }
      }
      
      Rectangle2D rect = MultiSpectralData.getLonLatBoundingBox(image);
      try {
        mp = new LambertAEA(rect);
      } catch (Exception e) {
        System.out.println(" getDataProjection"+e);
      }
      return mp;
    }

    public void addMapDisplayables(MapProjectionDisplayJ3D mapProjDsp) 
           throws VisADException, RemoteException {
        MapLines mapLines  = new MapLines("maplines");
        URL      mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPU");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }
                                                                                                                                                  
        mapLines  = new MapLines("maplines");
        mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPW");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }
                                                                                                                                                  
        mapLines  = new MapLines("maplines");
        mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLHPOL");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }
    }


    private class ScatterDisplayable extends RGBDisplayable {
       ScatterDisplayable(String name, RealType rgbRealType, float[][] colorPalette, boolean alphaflag) 
           throws VisADException, RemoteException {
         super(name, rgbRealType, colorPalette, alphaflag);
       }
    }

    private class ImageControl extends DisplayControlImpl {
      HydraRGBDisplayable rgbDisp;
      DisplayConventions dc;

      ImageControl(HydraRGBDisplayable rgbDisp, DisplayConventions dc) {
        super();
        this.rgbDisp = rgbDisp;
        this.dc = dc;
      }

      public void setRange(Range r) throws VisADException, RemoteException {
        rgbDisp.setRangeForColor(r.getMin(), r.getMax());
      }

      public DisplayConventions getDisplayConventions() {
        return dc;
      }
    }

    private class ImageCurveSelector extends CellImpl implements DisplayListener {
      boolean init = false;
      CurveDrawer curveDraw;
      DisplayMaster dspMaster;
      FlatField image;
      Gridded2DSet domainSet;
      CoordinateSystem cs;
      int domainLen_0;
      int domainLen_1;
      ImageCurveSelector other;
      UnionSet last_uSet = null;
      boolean imageLatLon = false;
      boolean active = true;

      ImageCurveSelector(CurveDrawer curveDraw, FlatField image, DisplayMaster master) {
        this.curveDraw = curveDraw;
        dspMaster = master;
        dspMaster.addDisplayListener(this);
        this.image = image;
        domainSet = (Gridded2DSet) image.getDomainSet();
        int[] lens = domainSet.getLengths();
        domainLen_0 = lens[0];
        domainLen_1 = lens[1];
        cs = ((FunctionType)image.getType()).getDomain().getCoordinateSystem();
        RealTupleType reference = null;
        if (cs != null) {
          reference = cs.getReference();
        }
        else {
          reference = ((SetType)domainSet.getType()).getDomain();
        }
        RealType[] rtypes = reference.getRealComponents();
        if (rtypes[0].equals(RealType.Latitude)) imageLatLon = true;
      }

      public void displayChanged(DisplayEvent de)
             throws VisADException, RemoteException {
         if ((de.getId() == DisplayEvent.MOUSE_RELEASED) && (active)) {
           UnionSet uSet = curveDraw.getCurves();
           if (uSet == last_uSet) return;
           SampledSet[] sets = uSet.getSets();
           int s_idx = sets.length-1;
           float[][] crv;

           if (cs != null) {
             crv = sets[s_idx].getSamples();
             if (imageLatLon) {
                float[] tmp = crv[0];
                crv[0] = crv[1];
                crv[1] = tmp;
             }
             crv = cs.fromReference(crv);
             crv = domainSet.valueToGrid(crv);
           }
           else {
             crv = sets[s_idx].getSamples();
             crv = domainSet.valueToGrid(crv);
           }

           float[][] onImage = new float[2][crv[0].length];
           int cnt = 0;
           for (int i=0; i<crv[0].length; i++) {
             if ( ((crv[0][i] >= 0)&&(crv[0][i] <= domainLen_0)) &&
                  ((crv[1][i] >= 0)&&(crv[1][i] <= domainLen_1)) ) {
               onImage[0][cnt] = crv[0][i];
               onImage[1][cnt] = crv[1][i];
               cnt++;
             }
           }
           uSet = new UnionSet(new SampledSet[] {sets[s_idx]});
           last_uSet = uSet;
           curveDraw.setCurves(uSet);
           other.updateCurve(sets[s_idx]);

           if (cnt == 0) {
             return;
           }

           float[][] tmp = new float[2][cnt];
           System.arraycopy(onImage[0], 0, tmp[0], 0, cnt);
           System.arraycopy(onImage[1], 0, tmp[1], 0, cnt);
           onImage = tmp;

           float[] minmaxvals = minmax(onImage[0]);
           int low_0 = Math.round(minmaxvals[0]);
           int hi_0 = Math.round(minmaxvals[1]);
           minmaxvals = minmax(onImage[1]);
           int low_1 = Math.round(minmaxvals[0]);
           int hi_1 = Math.round(minmaxvals[1]);

           int len_0 = (hi_0 - low_0) + 1;
           int len_1 = (hi_1 - low_1) + 1;
           int len = len_0*len_1;

           float[][] markScatter = new float[3][len];
           for (int k=0; k<len; k++) {
             markScatter[0][k] = 0;
             markScatter[1][k] = 0;
             markScatter[2][k] = 0;
           }
                                                                                                                                                  
           for (int j=0; j<len_1; j++) {
             for (int i=0; i<len_0; i++) {
               int idx = (j+low_1)*domainLen_0 + (i+low_0);
               int k = j*len_0 + i;
               float x = (float) (i + low_0);
               float y = (float) (j + low_1);
               if (DelaunayCustom.inside(crv, x, y)) {
                 markScatter[0][k] = scatterFieldRange[0][idx];
                 markScatter[1][k] = scatterFieldRange[1][idx];
                 markScatter[2][k] = 1;
               }
             }
           }
                                                                                                                                                  
           Integer1DSet dset = new Integer1DSet(len);
           FlatField scatterFieldMark = new FlatField(
              new FunctionType(RealType.Generic,
                 new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), dset);
                                                                                                                                                  
           scatterFieldMark.setSamples(markScatter, false);
           scatterMarkDsp.setData(scatterFieldMark);
         }
      }

      public void setActive(boolean active) {
        this.active = active;
      }

      public void reset() throws VisADException, RemoteException {
        RealTupleType type = ((SetType)curveDraw.getCurves().getType()).getDomain();
        curveDraw.setCurves(new UnionSet(new Gridded2DSet[]{
            new Gridded2DSet(type, new float[][] {
            { 0.0f }, { 0.0f }}, 1) }));
        Integer1DSet dset = new Integer1DSet(2);
        FlatField scatterFieldMark = new FlatField(
        new FunctionType(RealType.Generic,
              new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), dset);
        float[][] markScatter = new float[3][2]; 
        for (int k=0; k<2; k++) {
          markScatter[0][k] = scatterFieldRange[0][k];
          markScatter[1][k] = scatterFieldRange[1][k];
          markScatter[2][k] = 0;
        }
        scatterFieldMark.setSamples(markScatter, false);
        scatterMarkDsp.setData(scatterFieldMark);
      }

      public void updateCurve(SampledSet set) throws VisADException, RemoteException {
        last_uSet = new UnionSet(new SampledSet[] {set});
        curveDraw.setCurves(last_uSet);
      }

      public void setOther(ImageCurveSelector other) {
        this.other = other;
      }

      public void doAction()
           throws VisADException, RemoteException {
        if (!init) {
          init = true;
          return;
        }
      }

      public void setVisible(boolean visible) throws VisADException, RemoteException {
        curveDraw.setVisible(visible);
      }

    }

    private class ImageBoxSelector extends CellImpl {
        boolean init = false;
        boolean active = true;
        SubsetRubberBandBox subsetBox;
        Set imageDomain;
        int domainLen_0;
        int domainLen_1;
        float[][] scatter;
        LineDrawing lastBox;
        ImageBoxSelector other;

        ImageBoxSelector(SubsetRubberBandBox subsetBox, Set imageDomain, DisplayMaster master) 
            throws VisADException, RemoteException {
          super();
          this.subsetBox = subsetBox;
          this.imageDomain = imageDomain;
          int[] lens = ((Gridded2DSet)imageDomain).getLengths();
          domainLen_0 = lens[0];
          domainLen_1 = lens[1];
          lastBox = new LineDrawing("last_box");
          lastBox.setColor(Color.magenta);
          master.addDisplayable(lastBox);
        }

        public void doAction()
             throws VisADException, RemoteException
        {
           if (!init) {
             init = true;
             return;
           }
 
           if (!active) {
             return;
           }

           Gridded2DSet set = subsetBox.getBounds();
           float[][] corners = set.getSamples(false);
           float[][] coords = corners;

           if (imageDomain instanceof Linear2DSet) {
             coords = ((Gridded2DSet)imageDomain).valueToGrid(corners);
           }

           float[] coords_0 = coords[0];
           float[] coords_1 = coords[1];

           int low_0 = Math.round(Math.min(coords_0[0], coords_0[1]));
           int low_1 = Math.round(Math.min(coords_1[0], coords_1[1]));
           int hi_0  = Math.round(Math.max(coords_0[0], coords_0[1]));
           int hi_1  = Math.round(Math.max(coords_1[0], coords_1[1]));

           int len_0 = (hi_0 - low_0) + 1;
           int len_1 = (hi_1 - low_1) + 1;

           int len = len_0*len_1;

           float[][] markScatter = new float[3][len];
           for (int k=0; k<len; k++) {
             markScatter[0][k] = 0;
             markScatter[1][k] = 0;
             markScatter[2][k] = 0;
           }

           for (int j=0; j<len_1; j++) {
             for (int i=0; i<len_0; i++) {
               int idx = (j+low_1)*domainLen_0 + (i+low_0);

               int k = j*len_0 + i;
               markScatter[0][k] = scatterFieldRange[0][idx];
               markScatter[1][k] = scatterFieldRange[1][idx];
               markScatter[2][k] = 1;
             }
           }

           Integer1DSet dset = new Integer1DSet(len);
           FlatField scatterFieldMark = new FlatField(
             new FunctionType(RealType.Generic,
                new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), dset);

           scatterFieldMark.setSamples(markScatter, false);
           scatterMarkDsp.setData(scatterFieldMark);
           updateBox();
        }

        public void setActive(boolean active) {
          this.active = active;
        }

        public void reset() throws VisADException, RemoteException {
          Gridded2DSet set2D =
             new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                  new float[][] {{0},{0}}, 1);
          lastBox.setData(set2D);

          Integer1DSet dset = new Integer1DSet(2);
          FlatField scatterFieldMark = new FlatField(
          new FunctionType(RealType.Generic,
                new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), dset);
          float[][] markScatter = new float[3][2];
          for (int k=0; k<2; k++) {
            markScatter[0][k] = scatterFieldRange[0][k];
            markScatter[1][k] = scatterFieldRange[1][k];
            markScatter[2][k] = 0;
          }
          scatterFieldMark.setSamples(markScatter, false);
          scatterMarkDsp.setData(scatterFieldMark);
        }

        public void setOther(ImageBoxSelector other) {
          this.other = other;
        }

        public void updateBox() throws VisADException, RemoteException {
          Gridded3DSet set3D = subsetBox.getLastBox();
          float[][] samples = set3D.getSamples(false);
          Gridded2DSet set2D = 
             new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, 
                                  new float[][] {samples[0], samples[1]}, samples[0].length);
          lastBox.setData(set2D);
          other.updateBox(set2D);
        }

        public void updateBox(Gridded2DSet set2D) throws VisADException, RemoteException {
          lastBox.setData(set2D);
        }

        public void setVisible(boolean visible) throws VisADException, RemoteException {
          subsetBox.setVisible(visible);
          lastBox.setVisible(visible);
        }

        public SubsetRubberBandBox getSelector() {
          return subsetBox;
        }
    }

    private class ScatterBoxSelector extends CellImpl {
       boolean init = false;
       double[] x_coords = new double[2];
       double[] y_coords = new double[2];
       RubberBandBox rbb;
       LineDrawing selectBox;
       boolean active = true;


       ScatterBoxSelector(DisplayMaster master) throws VisADException, RemoteException {
         selectBox = new LineDrawing("select");
         selectBox.setColor(Color.green);
                                                                                                                                                  
         rbb = new RubberBandBox(RealType.XAxis, RealType.YAxis, 1);
         rbb.setColor(Color.green);
         rbb.addAction(this);

         master.addDisplayable(rbb);
         master.addDisplayable(selectBox);
       }


       public void doAction() throws VisADException, RemoteException {
         if (!init) {
           init = true;
           return;
         }

         if (!active) {
           return;
         }

         Gridded2DSet set = rbb.getBounds();
         float[] low = set.getLow();
         float[] hi = set.getHi();
         x_coords[0] = low[0];
         x_coords[1] = hi[0];
         y_coords[0] = low[1];
         y_coords[1] = hi[1];
                                                                                                                                                  
         SampledSet[] sets = new SampledSet[4];
         sets[0] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][] {{low[0], hi[0]}, {low[1], low[1]}}, 2);
         sets[1] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][] {{hi[0], hi[0]}, {low[1], hi[1]}}, 2);
         sets[2] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][] {{hi[0], low[0]}, {hi[1], hi[1]}}, 2);
         sets[3] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][] {{low[0], low[0]}, {hi[1], low[1]}}, 2);
         UnionSet uset = new UnionSet(sets);
         selectBox.setData(uset);
                                                                                                                                                  
         try {
           FlatField updateMask = histoField.markMaskFieldByRange(x_coords, y_coords, 0);
         } catch (Exception e) {
           e.printStackTrace();
         }
       }

       public void setVisible(boolean visible) throws VisADException, RemoteException {
         rbb.setVisible(visible);
         selectBox.setVisible(visible);
       }

       public void setActive(boolean active) {
         this.active = active;
       }

       public void reset() throws Exception {
         histoField.resetMaskField(0);
       }
   }

   private class ScatterCurveSelector extends CellImpl implements DisplayListener {
     CurveDrawer curveDraw;
     boolean init = false;
     UnionSet last_uSet = null;
     boolean active = true;

     ScatterCurveSelector(DisplayMaster master) throws VisADException, RemoteException {
       curveDraw = new CurveDrawer(RealType.XAxis, RealType.YAxis, 1);
       curveDraw.setColor(Color.green);
       curveDraw.setLineWidth(2);
       curveDraw.setData(new UnionSet(new Gridded2DSet[]{
            new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][] {
            { scatterFieldRange[0][0] }, { scatterFieldRange[1][0]}
        }, 1) }));

       master.addDisplayable(curveDraw);

       curveDraw.addAction(this);
       master.addDisplayListener(this);
     }

     public void displayChanged(DisplayEvent de)
            throws VisADException, RemoteException {
       if ((de.getId() == DisplayEvent.MOUSE_RELEASED) && (active)) {
         UnionSet uSet = curveDraw.getCurves();
         if (uSet == last_uSet) return;
         SampledSet[] sets = uSet.getSets();
         int s_idx = sets.length-1;
         float[][] crv;
                                                                                                                                                  
         crv = sets[s_idx].getSamples();
         last_uSet = new UnionSet(new SampledSet[] {sets[s_idx]});
         curveDraw.setCurves(last_uSet);

         try {
           histoField.clearMaskField(0);
           FlatField updateMask = histoField.markMaskFieldByCurve(crv, 0);
         } catch (Exception e) {
           e.printStackTrace();
         }
       }
     }

     public  void doAction() throws VisADException, RemoteException {
       if (!init) {
         init = true;
         return;
       }
     }

     public void setVisible(boolean visible) throws VisADException, RemoteException {
       curveDraw.setVisible(visible);
     }

     public void setActive(boolean active) {
       this.active = active;
     }

     public void reset() throws Exception {
       curveDraw.setData(new UnionSet(new Gridded2DSet[]{
            new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][] {
            { scatterFieldRange[0][0] }, { scatterFieldRange[1][0]}
        }, 1) }));
       histoField.resetMaskField(0);
     }
   }

   private class BoxCurveSwitch implements ActionListener {
     public ScatterBoxSelector boxSelect;
     public ScatterCurveSelector curveSelect;
     public ImageBoxSelector markX;
     public ImageBoxSelector markY;
     public ImageCurveSelector curveX;
     public ImageCurveSelector curveY;

     public BoxCurveSwitch() {
     }
    
     public void actionPerformed(ActionEvent ae) {
       String cmd = ae.getActionCommand();
       try {
       if (cmd.equals("Box")) {
         curveSelect.setActive(false);
         curveSelect.setVisible(false);
         curveSelect.reset();
         curveX.setActive(false);
         curveY.setActive(false);
         curveX.setVisible(false);
         curveY.setVisible(false);
         curveX.reset();
         curveY.reset();

         boxSelect.setActive(true);
         markX.setActive(true);
         markY.setActive(true);
         boxSelect.setVisible(true);
         markX.setVisible(true);
         markY.setVisible(true);
       } else if (cmd.equals("Curve")) {
         boxSelect.setActive(false);
         markX.setActive(false);
         markY.setActive(false);
         boxSelect.setVisible(false);
         markX.setVisible(false);
         markY.setVisible(false);
         boxSelect.reset();
         markX.reset();
         markY.reset();

         curveSelect.setActive(true);
         curveX.setActive(true);
         curveY.setActive(true);
         curveSelect.setVisible(true);
         curveX.setVisible(true);
         curveY.setVisible(true);
       }
       }
       catch (Exception e) {
         e.printStackTrace();
       }
     }
   }

    public static float[] minmax(float[] values) {
      float min =  Float.MAX_VALUE;
      float max = -Float.MAX_VALUE;
      for (int k = 0; k < values.length; k++) {
        float val = values[k];
        if ((val == val) && (val < Float.POSITIVE_INFINITY) && (val > Float.NEGATIVE_INFINITY)) {
          if (val < min) min = val;
          if (val > max) max = val;
        }
      }
      return new float[] {min, max};
    }

    public boolean getIsLatLon(FlatField field) throws VisADException, RemoteException {
      boolean isLL = false;
      FunctionType fnc_type = (FunctionType) field.getType();
      RealTupleType rtt = fnc_type.getDomain();
      if (rtt.equals(RealTupleType.LatitudeLongitudeTuple)) {
        isLL = true;
      }
      else if (!rtt.equals(RealTupleType.SpatialEarth2DTuple)) {
        rtt = fnc_type.getDomain().getCoordinateSystem().getReference();
        if ( rtt.equals(RealTupleType.LatitudeLongitudeTuple)) {
          isLL = true;
        }
      }
      return isLL;
    }
}
