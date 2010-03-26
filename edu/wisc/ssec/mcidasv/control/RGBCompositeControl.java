package edu.wisc.ssec.mcidasv.control;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataDataChoice;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataCategory;

import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.DisplayControl;

import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Displayable;

//import ucar.visad.display.ImageRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.ImageRGBDisplayable;

import visad.*;
import visad.BaseColorControl;
import visad.VisADException;
import visad.RemoteVisADException;
import visad.ScalarMapListener;
import visad.ScalarMapControlEvent;
import visad.ScalarMapEvent;
import visad.ReferenceException;
import visad.georef.MapProjection;


import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ViewDescriptor;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.Range;
import ucar.unidata.util.Misc;
import ucar.unidata.util.GuiUtils;

import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
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

import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;


public class RGBCompositeControl extends DisplayControlImpl {

   /** Displayable for the data */
   ImageRGBDisplayable imageDisplay;

   private DisplayMaster displayMaster;

   ScalarMap redMap = null;
   ScalarMap grnMap = null;
   ScalarMap bluMap = null;

   float[][] redTable = null;
   float[][] grnTable = null;
   float[][] bluTable = null;

   final double[] redRange = new double[] {Double.NaN, Double.NaN};
   final double[] grnRange = new double[] {Double.NaN, Double.NaN};
   final double[] bluRange = new double[] {Double.NaN, Double.NaN};

   FieldImpl imageField = null;


   private double gamma = 1.0;

   private final JTextField gammaTxtFld =
        new JTextField(Float.toString(1f), 12);


   public RGBCompositeControl() {
     super();
   }

   public boolean init(DataChoice dataChoice) throws VisADException, RemoteException {
     displayMaster = getViewManager().getMaster();

     DataSelection dataSelection = getDataSelection();
     imageField = (FieldImpl) dataChoice.getData(dataSelection);

     imageDisplay = new ImageRGBDisplayable("rgb composite", null, false, imageField);

     Iterator iter = imageDisplay.getScalarMapSet().iterator();
     while (iter.hasNext()) {
       ScalarMap map = (ScalarMap) iter.next();
       double[] datRng = map.getRange();
       if (map.getScalarName().startsWith("redimage")) redMap = map;
       if (map.getScalarName().startsWith("greenimage")) grnMap = map;
       if (map.getScalarName().startsWith("blueimage")) bluMap = map;
     }

     redMap.resetAutoScale();
     grnMap.resetAutoScale();
     bluMap.resetAutoScale();

     redMap.addScalarMapListener(new ColorMapListener(redMap));
     grnMap.addScalarMapListener(new ColorMapListener(grnMap));
     bluMap.addScalarMapListener(new ColorMapListener(bluMap));

     addDisplayable(imageDisplay, FLAG_COLORTABLE);
     imageDisplay.loadData(imageField);

     return true;
   }

   public void initDone() {
     redTable = ((BaseColorControl)redMap.getControl()).getTable();
     grnTable = ((BaseColorControl)grnMap.getControl()).getTable();
     bluTable = ((BaseColorControl)bluMap.getControl()).getTable();
   }


   public void setRedRange(float lo, float hi) throws VisADException, RemoteException {
     redMap.setRange(lo, hi);
   }

   public void setGrnRange(float lo, float hi) throws VisADException, RemoteException {
     grnMap.setRange(lo, hi);
   }

   public void setBluRange(float lo, float hi) throws VisADException, RemoteException {
     bluMap.setRange(lo, hi);
   }

   public void setRedGamma(float gamma) {
   }

   public void setGrnGamma(float gamma) {
   }

   public void setBluGamma(float gamma) {
   }

   public void setGamma(double gamma) {
     this.gamma = gamma;
   }

   public double getGamma() {
     return gamma;
   }

   public void updateGamma(double gamma) {
     setGamma(gamma);

     float[][] newRedTbl = getZeroOutArray(redTable);
     float[][] newGrnTbl = getZeroOutArray(grnTable);
     float[][] newBluTbl = getZeroOutArray(bluTable);

     for (int k=0; k<redTable[0].length; k++) {
       newRedTbl[0][k] = (float) Math.pow(redTable[0][k], gamma);
       newGrnTbl[1][k] = (float) Math.pow(grnTable[1][k], gamma);
       newBluTbl[2][k] = (float) Math.pow(bluTable[2][k], gamma);
     }
     try {
       displayMaster.setDisplayInactive();
       ((BaseColorControl)redMap.getControl()).setTable(newRedTbl);
       ((BaseColorControl)grnMap.getControl()).setTable(newGrnTbl);
       ((BaseColorControl)bluMap.getControl()).setTable(newBluTbl);
       displayMaster.setDisplayActive();
     } catch(Exception e) {
       e.printStackTrace();
     }
   }

   public float[][] getZeroOutArray(float[][] array) {
     float[][] newArray = new float[array.length][array[0].length];
     for (int i=0; i<newArray.length; i++) {
       for (int j=0; j<newArray[0].length; j++) {
         newArray[i][j] = 0f;
       }
     }
     return newArray;
   }

   protected ColorTable getInitialColorTable() {
     return getDisplayConventions().getParamColorTable("image");
   }

   public Container doMakeContents() {
     JPanel panel = new JPanel(new FlowLayout());
          final JLabel nameLabel = new JLabel("Gamma: ");

          gammaTxtFld.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  String tmp = gammaTxtFld.getText().trim();
                  updateGamma(Double.valueOf(tmp));
              }
          });

     panel.add(nameLabel);
     panel.add(gammaTxtFld);

     return panel;
   }

  private class ColorMapListener implements ScalarMapListener {

    ScalarMap clrMap;

    double[] range = new double[2];

    ColorMapListener(ScalarMap clrMap) {
      this.clrMap = clrMap;
    }

    public void controlChanged(ScalarMapControlEvent event) throws RemoteException, VisADException {
    }

    public void mapChanged(ScalarMapEvent event) throws RemoteException, VisADException {
      if (event.getId() == event.AUTO_SCALE) {
            double[] rng = clrMap.getRange();
            range[0] = rng[0];
            range[1] = rng[1];
      }
      else if (event.getId() == event.MANUAL) {
            double[] rng = clrMap.getRange();
            range[0] = rng[0];
            range[1] = rng[1];
      }
    }
  }

}
