/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
 *
 * All Rights Reserved
 *
 * McIDAS-V is built on Unidata's IDV, as well as SSEC's VisAD and HYDRA
 * projects. Parts of McIDAS-V source code are based on IDV, VisAD, and
 * HYDRA source code.
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

package edu.wisc.ssec.hydra;

import visad.python.JPythonMethods;
import visad.Gridded2DSet;
import visad.Linear1DSet;
import visad.Integer1DSet;
import visad.UnionSet;
import visad.RealType;
import visad.Real;
import visad.SetType;
import visad.RealTupleType;
import visad.FunctionType;
import visad.FlatField;
import visad.VisADException;
import visad.ScalarMap;
import visad.DataReferenceImpl;
import visad.Display;
import visad.GraphicsModeControl;
import visad.DisplayImpl;
import visad.ShapeControl;
import visad.ConstantMap;
import visad.AxisScale;
import visad.DisplayListener;
import visad.DisplayEvent;
import visad.CellImpl;
import visad.MouseHelper;
import visad.java2d.DisplayImplJ2D;
import visad.java2d.DirectManipulationRendererJ2D;
import java.awt.Component;
import java.rmi.RemoteException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;



public class Histogram implements DisplayListener {

   DisplayImpl display;
   UnionSet graph;
   String xAxisName;
   DataReferenceImpl graphRef;
   DataReferenceImpl clrLowRef;
   DataReferenceImpl clrHighRef;
   DataReferenceImpl lineLowRef;
   DataReferenceImpl lineHighRef;
   DataReferenceImpl lastLowRef;
   DataReferenceImpl lastHighRef;
   ShapeControl clrScaleCntrl;
   ScalarMap xMap;
   RealType xType;
   RealType yType;
   float[][] clrTable;
   float start_x;
   float width;
   float fillFrom;
   float fillTo;
   double scale;
   double offset;
   int nbins = 60;
   float dataLow;
   float dataHigh;
   float countLow;
   float countHi;

   float cbHght = 0.15f;
   float aspect = 2.5f;

   PropertyChangeListener listener;

   public Histogram() {
   }

   public Histogram(FlatField fltFld, float[] dataRange) {
      try {
         graph = build(fltFld, nbins, dataRange);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   public Histogram(FlatField fltFld) {
      try {
         graph = build(fltFld, nbins, null);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   public UnionSet build(FlatField fltFld, int nbins) throws VisADException, RemoteException {
       return build(fltFld, nbins, null);
   }

   public UnionSet build(FlatField fltFld, int nbins, float[] dataRange) throws VisADException, RemoteException {
      FlatField hist = JPythonMethods.hist(fltFld, new int[] {0}, new int[] {nbins});
      FunctionType ftype = (FunctionType) hist.getType();
      RealType domType = (RealType) ((RealTupleType)ftype.getDomain()).getComponent(0);
      RealType rngType = (RealType) ftype.getRange();
     
      xAxisName = domType.getName();

      Linear1DSet set = (Linear1DSet) hist.getDomainSet();
      float start = (float) set.getFirst();
      float step = (float) set.getStep();
      float[][] vals = hist.getFloats();
      if (dataRange == null) {
         dataLow = (set.getLow())[0];
         dataHigh = (set.getHi())[0];
      }
      else {
         dataLow = dataRange[0];
         dataHigh = dataRange[1];
      }

      float[] lohi = Hydra.minmax(vals[0]);
      countLow = lohi[0];
      countHi = lohi[1];

      RealTupleType histType = new RealTupleType(RealType.Generic, rngType);

      Gridded2DSet[] sets = new Gridded2DSet[nbins];

      for (int k=0; k<nbins; k++) {

          float x1 = k*step + (start - step/2);
          float y1 = 0f;
          float x2 = x1;
          float y2 = vals[0][k];
          float x3 = x1 + step;
          float y3 = y2;
          float x4 = x3;
          float y4 = 0f;

          sets[k] = new Gridded2DSet(histType, new float[][] {{x1,x2,x3,x4},{y1,y2,y3,y4}},4);
      }
      
      UnionSet uset = new UnionSet(sets);
      return uset;
   }

   public void setListener(PropertyChangeListener listener) {
      this.listener = listener;
   }

   public void updateDisplay(FlatField fltFld) throws VisADException, RemoteException {
      graph = build(fltFld, nbins);

      display.disableAction();
      xMap.setScalarName(xAxisName);
      graphRef.setData(graph);
      updateRange(dataLow, dataHigh);
      display.enableAction();

      start_x = -aspect;
      width = 2*aspect;
      fillFrom = start_x;
      fillTo = start_x + width;
      scale = width/(dataHigh - dataLow);
      offset = dataLow;

      clrScaleCntrl.setShape(0, MyColorScale.createColorBar(start_x, 0.0f ,width, cbHght, fillFrom, fillTo, clrTable));
   }

   public void updateColorBar(float[][] clrTable) throws VisADException, RemoteException {
      fixTable(clrTable);
      this.clrTable = clrTable;
      clrScaleCntrl.setShape(0, MyColorScale.createColorBar(start_x, 0.0f ,width, cbHght, fillFrom, fillTo, clrTable));
   }

   public void updateRange(double start, double end) throws VisADException, RemoteException {
      clrLowRef.setData(new Real(xType, start));
      clrHighRef.setData(new Real(xType, end));
      lastLowRef.setData(new Real(xType, start));
      lastHighRef.setData(new Real(xType, end));
   }
   
   void computeColorBarDisplayXCoords(double start, double end) {
      double loc_scale = 0;
      double loc_offset= 0;
      if ((start < dataLow) && (end > dataHigh)) {
         start_x = -aspect;
         width = 2*aspect;
      }
      else if ((start >= dataLow) && (end <= dataHigh)) {
         loc_scale = scale;
         loc_offset = offset;
         start_x = (float) (-aspect + loc_scale * (start - dataLow));
         float xb = (float) (-aspect + loc_scale * (end - dataLow));
         width = xb - start_x;
      }
      else if ((start > dataLow) && (end > dataHigh)) {
         loc_scale = 2*aspect/(end - dataLow);
         loc_offset = offset;
         
         start_x = (float) (-aspect + loc_scale * (start - dataLow));
         width = (float) (aspect - start_x);
      }
      else if ((start < dataLow) && (end <= dataHigh)) {
         loc_scale = 2*aspect/(dataHigh - start);
         loc_offset = start;
         start_x = (float) -aspect;
         width = (float) (loc_scale * (end - start));
      }
   }

   public void updateColorBarForRange(double start, double end) throws VisADException, RemoteException {
      updateRange(start, end);
      computeColorBarDisplayXCoords(start, end);
      clrScaleCntrl.setShape(0, MyColorScale.createColorBar(start_x, 0.0f, width, cbHght, fillFrom, fillTo, clrTable));
   }

   void makeColorBar(double start, double end) throws VisADException, RemoteException {
      computeColorBarDisplayXCoords(start, end);
      clrScaleCntrl.setShape(0, MyColorScale.createColorBar(start_x, 0.0f, width, cbHght, fillFrom, fillTo, clrTable));
   }

   public Component makeDisplay(float[][] clrTable) throws VisADException, RemoteException {
      fixTable(clrTable);
      this.clrTable = clrTable;
      display = new DisplayImplJ2D("histogram");
      display.disableAction();
      
      MouseHelper mouseHelp = display.getDisplayRenderer().getMouseBehavior().getMouseHelper();
      int[][][] funcMap =
        {{{MouseHelper.DIRECT, MouseHelper.ZOOM}, {MouseHelper.TRANSLATE, MouseHelper.NONE}},
         {{MouseHelper.CURSOR_TRANSLATE, MouseHelper.CURSOR_ZOOM}, {MouseHelper.CURSOR_ROTATE, MouseHelper.NONE}},
         {{MouseHelper.DIRECT, MouseHelper.DIRECT}, {MouseHelper.DIRECT, MouseHelper.DIRECT}}};

      mouseHelp.setFunctionMap(funcMap);

      display.addDisplayListener(this);
      display.getProjectionControl().setAspectCartesian(new double[] {aspect, 1.0, 1.0});
      GraphicsModeControl mode = display.getGraphicsModeControl();
      mode.setScaleEnable(true);
      display.setAlwaysAutoScale(true);

      start_x = -aspect;
      width = 2*aspect;
      fillFrom = start_x;
      fillTo = start_x + width;
      scale = width/(dataHigh - dataLow);
      offset = dataLow;

      graphRef = new DataReferenceImpl("histoRef");
      graphRef.setData(graph);


      RealTupleType type = (RealTupleType) ((SetType)graph.getType()).getDomain();
      xType = (RealType) (type.getComponents())[0];
      yType = (RealType) (type.getComponents())[1];
      
      xMap = new ScalarMap(xType, Display.XAxis);
      ScalarMap yMap = new ScalarMap(yType, Display.YAxis);

      display.addMap(xMap);
      display.addMap(yMap);
      xMap.setScaleEnable(true);
      yMap.setScaleEnable(false);
      xMap.setScalarName(xAxisName);
 
      AxisScale xAxis = xMap.getAxisScale();
      xAxis.setLabelSize(24);
      xAxis.setLabelAllTicks(true);

      display.addReference(graphRef, new ConstantMap[] { new ConstantMap(cbHght, Display.YAxisOffset)});

      clrLowRef = new DataReferenceImpl("clrLowRef");
      clrHighRef = new DataReferenceImpl("clrHighRef");
      lastLowRef = new DataReferenceImpl("lineLowRef");
      lastHighRef = new DataReferenceImpl("lineHighRef");

      updateRange(dataLow, dataHigh);

      display.addReferences(new DirectManipulationRendererJ2D(), clrLowRef,
             new ConstantMap[] {new ConstantMap(1.0f, Display.YAxis), new ConstantMap(7.0f, Display.PointSize), 
             new ConstantMap(1.0f, Display.Green), new ConstantMap(0.0f, Display.Blue), new ConstantMap(0.0f, Display.Red)});

      display.addReferences(new DirectManipulationRendererJ2D(), clrHighRef,
             new ConstantMap[] {new ConstantMap(1.0f, Display.YAxis), new ConstantMap(7.0f, Display.PointSize), 
             new ConstantMap(1.0f, Display.Green), new ConstantMap(0.0f, Display.Blue), new ConstantMap(0.0f, Display.Red)});

      lineLowRef = new DataReferenceImpl("lineLowRef");
      lineHighRef = new DataReferenceImpl("lineHighRef");
   
      display.addReference(lineLowRef, 
          new ConstantMap[] {new ConstantMap(1.0f, Display.Green), new ConstantMap(0.0f, Display.Blue), new ConstantMap(0.0f, Display.Red)});
      display.addReference(lineHighRef, 
          new ConstantMap[] {new ConstantMap(1.0f, Display.Green), new ConstantMap(0.0f, Display.Blue), new ConstantMap(0.0f, Display.Red)});

      RangeChangeListener listnr = new RangeChangeListener(clrLowRef, lineLowRef);
      listnr.addReference(clrLowRef);

      listnr = new RangeChangeListener(clrHighRef, lineHighRef);
      listnr.addReference(clrHighRef);


      RealType csType = RealType.getRealType("colorScale");
      ScalarMap shapeMap = new ScalarMap(csType, Display.Shape);
      display.addMap(shapeMap);
      DataReferenceImpl csRef = new DataReferenceImpl("colorScale");
      csRef.setData(new Integer1DSet(csType, 2));
      clrScaleCntrl = (ShapeControl)shapeMap.getControl();
      clrScaleCntrl.setShapeSet(new Integer1DSet(csType, 2));
      clrScaleCntrl.setShape(0, MyColorScale.createColorBar(-aspect, 0.0f ,2*aspect, cbHght, clrTable));
      display.addReference(csRef, new ConstantMap[] {new ConstantMap(-1.0f, Display.YAxisOffset)});

      display.enableAction();
      
      return display.getComponent();
   }

    public void displayChanged(final DisplayEvent e) throws VisADException, RemoteException {
        if (e.getId() == DisplayEvent.MOUSE_RELEASED_CENTER) {
        }
        else if (e.getId() == DisplayEvent.MOUSE_PRESSED_LEFT) {
        }
        else if (e.getId() == DisplayEvent.MOUSE_RELEASED) {
           double lo = ((Real)clrLowRef.getData()).getValue();
           double hi = ((Real)clrHighRef.getData()).getValue();
           double lastLow = ((Real)lastLowRef.getData()).getValue();
           double lastHigh = ((Real)lastHighRef.getData()).getValue();
           if ((lo != lastLow) || (hi != lastHigh)) {
              if (listener != null) {
                 PropertyChangeEvent evt = new PropertyChangeEvent(this, "range", null, new double[] {lo, hi});
                 listener.propertyChange(evt);
              }
              makeColorBar(lo,hi);
              lastLowRef.setData(clrLowRef.getData());
              lastHighRef.setData(clrHighRef.getData());
           }
        }
    }

    private void fixTable(float[][] clrTable) {
        for (int k=0; k<clrTable[0].length; k++) {
           if (clrTable[0][k] > 1.0f) clrTable[0][k] = 1.0f;
           if (clrTable[1][k] > 1.0f) clrTable[1][k] = 1.0f;
           if (clrTable[2][k] > 1.0f) clrTable[2][k] = 1.0f;
        }
    }

   Gridded2DSet makeSelectorLine(float xval) throws VisADException, RemoteException {
      float[][] points = new float[][] {{xval, xval}, {countLow, countHi}};
      Gridded2DSet line = new Gridded2DSet(new RealTupleType(xType, yType), points, 2);
      return line;
   }

  public void destroy() {
    try {
      if (display != null) {
         display.removeDisplayListener(this);
         display.destroy();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public DisplayImpl getDisplay() {
     return display;
  }

   private class RangeChangeListener extends CellImpl {
      boolean init = true;
      DataReferenceImpl valueRef;
      DataReferenceImpl lineRef;

      public RangeChangeListener(DataReferenceImpl valueRef, DataReferenceImpl lineRef) {
         this.valueRef = valueRef;
         this.lineRef = lineRef;
      }

      public void doAction() throws VisADException, RemoteException {
         if (init) {
            float val = (float) ((Real)valueRef.getData()).getValue();
            lineRef.setData(makeSelectorLine(val));
         }
         else {
            init = true;
         }
      }
   }

}
