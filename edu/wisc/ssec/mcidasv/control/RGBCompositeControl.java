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
   private final JTextField redLowTxtFld =
        new JTextField(Float.toString(1f), 12);
   private final JTextField redHighTxtFld =
        new JTextField(Float.toString(1f), 12);
   private final JTextField grnLowTxtFld =
        new JTextField(Float.toString(1f), 12);
   private final JTextField grnHighTxtFld =
        new JTextField(Float.toString(1f), 12);
   private final JTextField bluLowTxtFld =
        new JTextField(Float.toString(1f), 12);
   private final JTextField bluHighTxtFld =
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

     redMap.addScalarMapListener(new ColorMapListener(redMap, redRange, redLowTxtFld, redHighTxtFld));
     grnMap.addScalarMapListener(new ColorMapListener(grnMap, grnRange, grnLowTxtFld, grnHighTxtFld));
     bluMap.addScalarMapListener(new ColorMapListener(bluMap, bluRange, bluLowTxtFld, bluHighTxtFld));

     addDisplayable(imageDisplay, FLAG_COLORTABLE);
     imageDisplay.loadData(imageField);

     return true;
   }

   public void initDone() {
     redTable = ((BaseColorControl)redMap.getControl()).getTable();
     grnTable = ((BaseColorControl)grnMap.getControl()).getTable();
     bluTable = ((BaseColorControl)bluMap.getControl()).getTable();
   }


   public void updateRedRange(double lo, double hi) {
     redRange[0] = lo;
     redRange[1] = hi;
     try {
       redMap.setRange(lo, hi);
     } catch (VisADException ex) {
       LogUtil.logException("redMap.setRange", ex);
     } catch (RemoteException ex) {
       LogUtil.logException("redMap.setRange", ex);
     }
   }

   public void updateGrnRange(double lo, double hi) {
     grnRange[0] = lo;
     grnRange[1] = hi;
     try {
       grnMap.setRange(lo, hi);
     } catch (VisADException ex) {
       LogUtil.logException("grnMap.setRange", ex);
     } catch (RemoteException ex) {
       LogUtil.logException("grnMap.setRange", ex);
     }
   }

   public void updateBluRange(double lo, double hi) {
     bluRange[0] = lo;
     bluRange[1] = hi;
     try {
       bluMap.setRange(lo, hi);
     } catch (VisADException ex) {
       LogUtil.logException("bluMap.setRange", ex);
     } catch (RemoteException ex) {
       LogUtil.logException("bluMap.setRange", ex);
     }
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
     } catch(Exception ex) {
       LogUtil.logException("setDisplayInactive", ex);
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

     JPanel bigPanel = new JPanel(new BorderLayout());
     JPanel subPanel = new JPanel(new GridLayout(4,1));

     JPanel gammaPanel = new JPanel(new FlowLayout());
          final JLabel nameLabel = new JLabel("Gamma: ");

          gammaTxtFld.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  String tmp = gammaTxtFld.getText().trim();
                  updateGamma(Double.valueOf(tmp));
              }
          });

     gammaPanel.add(nameLabel);
     gammaPanel.add(gammaTxtFld);

     JPanel redPanel = new JPanel(new FlowLayout());
     redPanel.add(new JLabel("Red range: "));
   
     redLowTxtFld.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            String tmp = redLowTxtFld.getText().trim();
            updateRedRange(Double.valueOf(tmp), redRange[1]);
         }
     });
     redPanel.add(redLowTxtFld);
     redHighTxtFld.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            String tmp = redHighTxtFld.getText().trim();
            updateRedRange(redRange[0], Double.valueOf(tmp));
         }
     });
     redPanel.add(redHighTxtFld);

     JPanel grnPanel = new JPanel(new FlowLayout());
     grnPanel.add(new JLabel("Green range: "));
   
     grnLowTxtFld.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            String tmp = grnLowTxtFld.getText().trim();
            updateGrnRange(Double.valueOf(tmp), grnRange[1]);
         }
     });
     grnPanel.add(grnLowTxtFld);
     grnHighTxtFld.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            String tmp = grnHighTxtFld.getText().trim();
            updateGrnRange(grnRange[0], Double.valueOf(tmp));
         }
     });
     grnPanel.add(grnHighTxtFld);


     JPanel bluPanel = new JPanel(new FlowLayout());
     bluPanel.add(new JLabel("Blue range: "));
   
     bluLowTxtFld.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            String tmp = bluLowTxtFld.getText().trim();
            updateBluRange(Double.valueOf(tmp), bluRange[1]);
         }
     });
     bluPanel.add(bluLowTxtFld);
     bluHighTxtFld.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            String tmp = bluHighTxtFld.getText().trim();
            updateBluRange(bluRange[0], Double.valueOf(tmp));
         }
     });
     bluPanel.add(bluHighTxtFld);

     subPanel.add(redPanel);
     subPanel.add(grnPanel);
     subPanel.add(bluPanel);
     subPanel.add(gammaPanel);

     bigPanel.add(subPanel, BorderLayout.NORTH);

     return bigPanel;
   }

  private class ColorMapListener implements ScalarMapListener
  {
    ScalarMap clrMap;

    double[] range = null;

    JTextField lowTxtFld;
    JTextField highTxtFld;

    ColorMapListener(ScalarMap clrMap, double[] range, JTextField lowTxtFld, JTextField highTxtFld) {
      this.clrMap = clrMap;
      this.lowTxtFld = lowTxtFld;
      this.highTxtFld = highTxtFld;
      this.range = range;
    }

    public void controlChanged(ScalarMapControlEvent event) throws RemoteException, VisADException {
    }

    public void mapChanged(ScalarMapEvent event) throws RemoteException, VisADException {
      if (event.getId() == event.AUTO_SCALE) {
            double[] rng = clrMap.getRange();
            range[0] = rng[0];
            range[1] = rng[1];
            lowTxtFld.setText(Float.toString((float)rng[0]));
            highTxtFld.setText(Float.toString((float)rng[1]));
      }
      else if (event.getId() == event.MANUAL) {
            double[] rng = clrMap.getRange();
            range[0] = rng[0];
            range[1] = rng[1];
      }
    }
  }


}
