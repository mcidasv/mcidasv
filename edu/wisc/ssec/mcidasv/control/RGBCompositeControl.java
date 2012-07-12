/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2012
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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import visad.BaseColorControl;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.ScalarMap;
import visad.ScalarMapControlEvent;
import visad.ScalarMapEvent;
import visad.ScalarMapListener;
import visad.VisADException;
import visad.Data;
import visad.CoordinateSystem;

import visad.georef.MapProjection;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Range;
import ucar.visad.display.DisplayMaster;

import edu.wisc.ssec.mcidasv.data.hydra.ImageRGBDisplayable;


public class RGBCompositeControl extends DisplayControlImpl {

   /** Displayable for the data */
   private ImageRGBDisplayable imageDisplay;

   private DisplayMaster displayMaster;

   private ScalarMap redMap = null;
   private ScalarMap grnMap = null;
   private ScalarMap bluMap = null;

   float[][] redTable = null;
   float[][] grnTable = null;
   float[][] bluTable = null;

   final private double[] redRange = new double[] {Double.NaN, Double.NaN};
   final private double[] grnRange = new double[] {Double.NaN, Double.NaN};
   final private double[] bluRange = new double[] {Double.NaN, Double.NaN};

   final double[] initRedRange = new double[] {Double.NaN, Double.NaN};
   final double[] initGrnRange = new double[] {Double.NaN, Double.NaN};
   final double[] initBluRange = new double[] {Double.NaN, Double.NaN};

   private FieldImpl imageField = null;
   private MapProjection mapProjection = null;


   private double gamma = 1.0;

   private double redGamma = 1.0;
   private double grnGamma = 1.0;
   private double bluGamma = 1.0;

   private boolean hasRange = false;

   private final JTextField gammaTxtFld =
        new JTextField(Float.toString(1f), 4);
   private final JTextField redGammaTxtFld =
        new JTextField(Float.toString(1f), 4);
   private final JTextField grnGammaTxtFld =
        new JTextField(Float.toString(1f), 4);
   private final JTextField bluGammaTxtFld =
        new JTextField(Float.toString(1f), 4);

   private final JTextField redLowTxtFld =
        new JTextField(Float.toString(1f), 10);
   private final JTextField redHighTxtFld =
        new JTextField(Float.toString(1f), 10);
   private final JTextField grnLowTxtFld =
        new JTextField(Float.toString(1f), 10);
   private final JTextField grnHighTxtFld =
        new JTextField(Float.toString(1f), 10);
   private final JTextField bluLowTxtFld =
        new JTextField(Float.toString(1f), 10);
   private final JTextField bluHighTxtFld =
        new JTextField(Float.toString(1f), 10);

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
       if (map.getScalarName().startsWith("redimage")) {
		redMap = map;
	}
       if (map.getScalarName().startsWith("greenimage")) {
		grnMap = map;
	}
       if (map.getScalarName().startsWith("blueimage")) {
		bluMap = map;
	}
     }

     if (checkRange()) { //- from unpersistence if true, initialize gui, ScalarMaps
       double[] redRange = getRedRange();
       double[] grnRange = getGrnRange();
       double[] bluRange = getBluRange();

       initRedRange[0] = redRange[0];
       initRedRange[1] = redRange[1];
       initGrnRange[0] = grnRange[0];
       initGrnRange[1] = grnRange[1];
       initBluRange[0] = bluRange[0];
       initBluRange[1] = bluRange[1];

       redLowTxtFld.setText(Float.toString((float)redRange[0]));
       redHighTxtFld.setText(Float.toString((float)redRange[1]));
       grnLowTxtFld.setText(Float.toString((float)grnRange[0]));
       grnHighTxtFld.setText(Float.toString((float)grnRange[1]));
       bluLowTxtFld.setText(Float.toString((float)bluRange[0]));
       bluHighTxtFld.setText(Float.toString((float)bluRange[1]));
   
       gammaTxtFld.setText(Float.toString((float)gamma));
       redGammaTxtFld.setText(Float.toString((float)redGamma));
       grnGammaTxtFld.setText(Float.toString((float)grnGamma));
       bluGammaTxtFld.setText(Float.toString((float)bluGamma));

       redMap.setRange(redRange[0], redRange[1]);
       grnMap.setRange(grnRange[0], grnRange[1]);
       bluMap.setRange(bluRange[0], bluRange[1]);
     } 
     else {
       redMap.resetAutoScale();
       grnMap.resetAutoScale();
       bluMap.resetAutoScale();

       redMap.addScalarMapListener(new ColorMapListener(redMap, initRedRange, redRange, redLowTxtFld, redHighTxtFld));
       grnMap.addScalarMapListener(new ColorMapListener(grnMap, initGrnRange, grnRange, grnLowTxtFld, grnHighTxtFld));
       bluMap.addScalarMapListener(new ColorMapListener(bluMap, initBluRange, bluRange, bluLowTxtFld, bluHighTxtFld));
     }

     setShowInDisplayList(true);

     addDisplayable(imageDisplay, FLAG_COLORTABLE);

     return true;
   }

   public void initDone() {
     while (true) {
         if (null != redMap.getControl()) {
            redTable = ((BaseColorControl) redMap.getControl()).getTable();
            break;
         }
     }
     while (true) {
         if (null != grnMap.getControl()) {
             grnTable = ((BaseColorControl) grnMap.getControl()).getTable();
             break;
         }
     }
     while (true) {
         if (null != bluMap.getControl()) {
             bluTable = ((BaseColorControl) bluMap.getControl()).getTable();
             break;
         }
     } 

     float[][] newRedTbl = getZeroOutArray(redTable);
     float[][] newGrnTbl = getZeroOutArray(grnTable);
     float[][] newBluTbl = getZeroOutArray(bluTable);

     for (int k=0; k<redTable[0].length; k++) {
       newRedTbl[0][k] = (float) Math.pow(redTable[0][k], redGamma);
       newGrnTbl[1][k] = (float) Math.pow(grnTable[1][k], grnGamma);
       newBluTbl[2][k] = (float) Math.pow(bluTable[2][k], bluGamma);
     }

     try {
       displayMaster.setDisplayInactive();
       ((BaseColorControl)redMap.getControl()).setTable(newRedTbl);
       ((BaseColorControl)grnMap.getControl()).setTable(newGrnTbl);
       ((BaseColorControl)bluMap.getControl()).setTable(newBluTbl);
       imageDisplay.loadData(imageField);
       displayMaster.setDisplayActive();
     } catch(Exception ex) {
       LogUtil.logException("setDisplayInactive", ex);
     }
   }

   public MapProjection getDataProjection() {
     CoordinateSystem cs = null;
     try {
       if (imageField instanceof FlatField) {
         cs = ((FunctionType)imageField.getType()).getDomain().getCoordinateSystem();
       } 
       else if (imageField instanceof FieldImpl) {
         Data dat = imageField.getSample(0, false);
         if (dat instanceof FlatField) {
           FlatField img = (FlatField) dat;
           cs = ((FunctionType)img.getType()).getDomain().getCoordinateSystem();
         }
       }
     }
     catch (Exception ex) {
       LogUtil.logException("problem accessing data", ex);
     }

     if (cs instanceof MapProjection) mapProjection = (MapProjection) cs;

     return mapProjection;
   }

   boolean checkRange() {
     if (Double.isNaN(redRange[0]) || Double.isNaN(grnRange[0]) || Double.isNaN(bluRange[0])) {
       return false;
     }
     else {
       return true;
     }
   }

   private void updateRedRange(double lo, double hi) {
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

   public void setRedRange(double[] range) {
     redRange[0] = range[0];
     redRange[1] = range[1];
   }

   public double[] getRedRange() {
     return new double[] {redRange[0], redRange[1]};
   }

   private void updateGrnRange(double lo, double hi) {
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

   public void setGrnRange(double[] range) {
     grnRange[0] = range[0];
     grnRange[1] = range[1];
   }

   public double[] getGrnRange() {
     return new double[] {grnRange[0], grnRange[1]};
   }

   private void updateBluRange(double lo, double hi) {
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

   public void setBluRange(double[] range) {
     bluRange[0] = range[0];
     bluRange[1] = range[1];
   }

   public double[] getBluRange() {
     return new double[] {bluRange[0], bluRange[1]};
   }

   public void setRedGamma(double gamma) {
     redGamma = gamma;
   }

   public double getRedGamma() {
     return redGamma;
   }

   public void setGrnGamma(double gamma) {
     grnGamma = gamma;
   }

   public double getGrnGamma() {
     return grnGamma;
   }

   public void setBluGamma(double gamma) {
     bluGamma = gamma;
   }

   public double getBluGamma() {
     return bluGamma;
   }

   public void setGamma(double gamma) {
     this.gamma = gamma;
   }

   public double getGamma() {
     return gamma;
   }

   private void updateGamma(double gamma) {
     setGamma(gamma);
     setRedGamma(gamma);
     setGrnGamma(gamma);
     setBluGamma(gamma);
     redGammaTxtFld.setText(Float.toString((float)gamma));
     grnGammaTxtFld.setText(Float.toString((float)gamma));
     bluGammaTxtFld.setText(Float.toString((float)gamma));
	
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

   private void updateRedGamma(double gamma) {
     setRedGamma(gamma);

     float[][] newRedTbl = getZeroOutArray(redTable);

     for (int k=0; k<redTable[0].length; k++) {
       newRedTbl[0][k] = (float) Math.pow(redTable[0][k], gamma);
     }

     try {
       displayMaster.setDisplayInactive();
       ((BaseColorControl)redMap.getControl()).setTable(newRedTbl);
       displayMaster.setDisplayActive();
     } catch(Exception ex) {
       LogUtil.logException("setDisplayInactive", ex);
     }
   }

   private void updateGrnGamma(double gamma) {
     setGrnGamma(gamma);

     float[][] newGrnTbl = getZeroOutArray(grnTable);
     for (int k=0; k<grnTable[0].length; k++) {
       newGrnTbl[1][k] = (float) Math.pow(grnTable[1][k], gamma);
     }

     try {
       displayMaster.setDisplayInactive();
       ((BaseColorControl)grnMap.getControl()).setTable(newGrnTbl);
       displayMaster.setDisplayActive();
     } catch(Exception ex) {
       LogUtil.logException("setDisplayInactive", ex);
     }
   }

   private void updateBluGamma(double gamma) {
     setBluGamma(gamma);

     float[][] newBluTbl = getZeroOutArray(bluTable);
     for (int k=0; k<bluTable[0].length; k++) {
       newBluTbl[2][k] = (float) Math.pow(bluTable[2][k], gamma);
     }

     try {
       displayMaster.setDisplayInactive();
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

     redGammaTxtFld.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
             String tmp = redGammaTxtFld.getText().trim();
             updateRedGamma(Double.valueOf(tmp));
         }
     });
     redPanel.add(new JLabel("Gamma:"));
     redPanel.add(redGammaTxtFld);

     JButton button = new JButton("reset");
     redPanel.add(button);
     button.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         updateRedRange(initRedRange[0], initRedRange[1]);
         redRange[0] = initRedRange[0];
         redRange[1] = initRedRange[1];
         redLowTxtFld.setText(Float.toString((float)redRange[0]));
         redHighTxtFld.setText(Float.toString((float)redRange[1]));
       }
     });

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

     grnGammaTxtFld.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
             String tmp = grnGammaTxtFld.getText().trim();
             updateGrnGamma(Double.valueOf(tmp));
         }
     });
     grnPanel.add(new JLabel("Gamma:"));
     grnPanel.add(grnGammaTxtFld);


     button = new JButton("reset");
     grnPanel.add(button);
     button.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         updateGrnRange(initGrnRange[0], initGrnRange[1]);
         grnRange[0] = initGrnRange[0];
         grnRange[1] = initGrnRange[1];
         grnLowTxtFld.setText(Float.toString((float)grnRange[0]));
         grnHighTxtFld.setText(Float.toString((float)grnRange[1]));
       }
     });



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

     bluGammaTxtFld.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
             String tmp = bluGammaTxtFld.getText().trim();
             updateBluGamma(Double.valueOf(tmp));
         }
     });
     bluPanel.add(new JLabel("Gamma:"));
     bluPanel.add(bluGammaTxtFld);

     button = new JButton("reset");
     bluPanel.add(button);
     button.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         updateBluRange(initBluRange[0], initBluRange[1]);
         bluRange[0] = initBluRange[0];
         bluRange[1] = initBluRange[1];
         bluLowTxtFld.setText(Float.toString((float)bluRange[0]));
         bluHighTxtFld.setText(Float.toString((float)bluRange[1]));
       }
     });


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
    double[] initRange = null;

    JTextField lowTxtFld;
    JTextField highTxtFld;

    ColorMapListener(ScalarMap clrMap, double[] initRange, double[] range, JTextField lowTxtFld, JTextField highTxtFld) {
      this.clrMap = clrMap;
      this.lowTxtFld = lowTxtFld;
      this.highTxtFld = highTxtFld;
      this.range = range;
      this.initRange = initRange;
    }


    public void controlChanged(ScalarMapControlEvent event) throws RemoteException, VisADException {
    }

    public void mapChanged(ScalarMapEvent event) throws RemoteException, VisADException {	
      if (event.getId() == event.AUTO_SCALE) {
            double[] rng = clrMap.getRange();
	    boolean shouldRemove = false;
	    //Ghansham: decide whether it is first time. The cleaner way
	    if (!Double.isNaN(rng[0]) && !Double.isNaN(rng[1]) && Double.isNaN(initRange[0]) && Double.isNaN(initRange[1])) {
		shouldRemove = true;
	    }
            range[0] = rng[0];
            range[1] = rng[1];
            initRange[0] = rng[0];
            initRange[1] = rng[1];
            lowTxtFld.setText(Float.toString((float)rng[0]));
            highTxtFld.setText(Float.toString((float)rng[1]));
	    //Ghansham:If its first time remove the scalarmaplistener and setRange manually to disable autscaling of the scalarmap
	    if(shouldRemove) {
		clrMap.removeScalarMapListener(this);
            //-Lock out auto-scaling
		clrMap.disableAutoScale();
	    }
      }
      else if (event.getId() == event.MANUAL) {
            double[] rng = clrMap.getRange();
            range[0] = rng[0];
            range[1] = rng[1];
      }
    }
  }


}
