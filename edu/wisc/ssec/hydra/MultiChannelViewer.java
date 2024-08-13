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

import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.Cursor;

import java.util.HashMap;

import edu.wisc.ssec.adapter.MultiSpectralData;

import visad.*;
import visad.georef.LatLonTuple;
import visad.georef.MapProjection;
import java.rmi.RemoteException;

import ucar.unidata.util.ColorTable;
import edu.wisc.ssec.hydra.data.DataChoice;
import edu.wisc.ssec.hydra.data.DataSelection;
import edu.wisc.ssec.hydra.data.DataSource;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.Date;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingWorker;


public class MultiChannelViewer extends HydraDisplay {

    MultiSpectralDisplay multiSpectDsp = null;
    JComponent channelSelectComp = null;
    MultiSpectralData multiSpectData = null;
    ImageDisplay imgDisplay = null;
    DataChoice dataChoice = null;

    Hydra hydra = null;
    DataBrowser dataBrowser = null;
    String sourceDescription = null;
    String dateTimeStamp = null;
    String fldName = null;
    String baseDescription = null;
    int dataSourceId;

    JMenuBar menuBar = null;

    JFrame frame = null;

    float initXmapScale;
    
    boolean initialized = false;
    ReadoutProbe probe = null;

    public MultiChannelViewer(Hydra hydra, DataChoice dataChoice, String sourceDescription, String dateTimeStamp, int windowNumber, int dataSourceId) throws Exception {
        this(hydra, dataChoice, sourceDescription, dateTimeStamp, windowNumber, dataSourceId, Float.NaN, null, null);
    }

    public MultiChannelViewer(Hydra hydra, DataChoice dataChoice, String sourceDescription, String dateTimeStamp, int windowNumber, int dataSourceId,
                              float initWaveNumber, float[] xMapRange, float[] yMapRange) throws Exception {
         this.hydra = hydra;
         this.dataBrowser = hydra.getDataBrowser();
         this.dataChoice = dataChoice;
         this.sourceDescription = sourceDescription;
         this.dateTimeStamp = dateTimeStamp;
         this.dataSourceId = dataSourceId;

         multiSpectDsp = new MultiSpectralDisplay((DataChoice) dataChoice, initWaveNumber, xMapRange, yMapRange);
         initXmapScale = (float) multiSpectDsp.getXmapScale();
         multiSpectDsp.showChannelSelector();
         final MultiSpectralDisplay msd = multiSpectDsp;

         FlatField image = msd.getImageData();
         image = reproject(image);

         MapProjection mapProj = Hydra.getDataProjection(image);

         ColorTable clrTbl = Hydra.invGrayTable;

         HydraRGBDisplayable imageDsp = Hydra.makeImageDisplayable(image, null, clrTbl, fldName);
         
         DataSourceInfo datSrcInfo = new DataSourceInfo(sourceDescription, dateTimeStamp, dataSourceId);
         DatasetInfo dsInfo = new DatasetInfo(fldName, Float.NaN, datSrcInfo);

         baseDescription = sourceDescription+" "+dateTimeStamp;
         String str = baseDescription+" "+Float.toString(multiSpectDsp.getWaveNumber())+" cm-1";

         if (windowNumber > 0) {
            imgDisplay = new ImageDisplay(imageDsp, mapProj, windowNumber, false, dsInfo, false);
         }
         else {
            imgDisplay = new ImageDisplay(imageDsp, mapProj, false, dsInfo, false);
         }
         imgDisplay.onlyOverlayNoReplace = true;
         final ImageDisplay iDisplay = imgDisplay;
         iDisplay.getDepiction().setName(Float.toString(multiSpectDsp.getWaveNumber()));

         multiSpectData = multiSpectDsp.getMultiSpectralData();

         channelSelectComp = doMakeChannelSelectComponent(multiSpectData.hasBandNames());

         final MultiSpectralData data = msd.getMultiSpectralData();

         probe = iDisplay.getReadoutProbe();
         DataReference probeLocationRef = probe.getEarthLocationRef();
         DataReference spectrumRef = msd.getSpectrumRef();
         spectrumRef.setData(data.getSpectrum((LatLonTuple)probeLocationRef.getData()));
         ProbeLocationChange probeChangeListnr = new ProbeLocationChange(probeLocationRef, spectrumRef, data);

         DataReference probeLocationRefB = (iDisplay.addReadoutProbe(Color.cyan, 0.08, 0.08)).getEarthLocationRef();
         DataReference spectrumRefB = new DataReferenceImpl("spectrumB");
         spectrumRefB.setData(msd.getMultiSpectralData().getSpectrum((LatLonTuple)probeLocationRefB.getData()));
         msd.addRef(spectrumRefB, Color.cyan);
         ProbeLocationChange probeChangeListnrB = new ProbeLocationChange(probeLocationRefB, spectrumRefB, data);

         JComponent comp = doMakeComponent();

         menuBar = buildMenuBar();

         String title;
         if (windowNumber > 0) {
            title = "Window "+windowNumber;
         }
         else {
            title = sourceDescription+" "+dateTimeStamp;
         }
         frame = Hydra.createAndShowFrame(title, comp, menuBar,
//                                  new Dimension(480,760), new Point(220, 0));
                                  new Dimension(520,760), new Point(220, 0));
         frame.addWindowListener(this);
         iDisplay.setParentFrame(frame);
    }

    public MultiChannelViewer cloneMcV() {
        MultiChannelViewer mcv = null;
        try {
           mcv = new MultiChannelViewer(this.hydra, this.dataChoice, this.sourceDescription, this.dateTimeStamp, 0, this.dataSourceId,
                                        this.multiSpectDsp.getWaveNumber(), 
                                        this.multiSpectDsp.getXmapRange(), this.multiSpectDsp.getYmapRange());
        }
        catch (Exception e) {
           e.printStackTrace();
        }
        return mcv;
    }

    public MultiSpectralDisplay getMultiSpectralDisplay() {
       return multiSpectDsp;
    }

    public JFrame getFrame() {
       return frame;
    }

    public void windowClosing(WindowEvent e) {
       imgDisplay.windowClosing(e);
    }
    
//    public void windowActivated(WindowEvent e) {
//     if (initialized) { //TODO: not necessarily initialized, really just skipping the first event
//        if (probe != null) {
//           //if (Float.isNaN(probe.getFloatValue())) {
//              probe.resetProbePosition();
//           //}
//        }
//     }
//     else {
//        initialized = true;
//     }
//   }

    public JComponent doMakeComponent() {
        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                       multiSpectDsp.getDisplayComponent(), imgDisplay.getComponent());
        if (SwingUtilities.isEventDispatchThread()) {
           imgDisplay.getButtonPanel().add(channelSelectComp);
           splitPane.setDividerLocation(200);
        }
        else {
           try {
              SwingUtilities.invokeAndWait(new Runnable() {
                  public void run() {
                     imgDisplay.getButtonPanel().add(channelSelectComp);
                     splitPane.setDividerLocation(200);
                  }
              });
           } 
           catch (Exception e) {
              e.printStackTrace();
           }
        }

        return splitPane;
    }

    public JMenuBar buildMenuBar() {
       JMenuBar menuBar = imgDisplay.getMenuBar();

       JMenu hlpMenu = new JMenu("Help");
       hlpMenu.getPopupMenu().setLightWeightPopupEnabled(false);

       hlpMenu.add(new JTextArea("Spectrum Display:\n   Zoom (rubber band): SHFT+DRAG \n   Reset: CNTL+CLCK"));

       menuBar.add(hlpMenu);

       // get the tools JMenu
       JMenu tools = menuBar.getMenu(0);
       
       JMenuItem captureSpectrum = new JMenuItem("Capture Spectrum");
       captureSpectrum.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 String cmd = e.getActionCommand();
                 if (cmd.equals("captureSpectrum")) {
                     DisplayCapture.capture(frame, multiSpectDsp.getDisplay(), "jpeg");
                 }
             }
       });
       captureSpectrum.setActionCommand("captureSpectrum");
       tools.add(captureSpectrum);
       
       JMenuItem fourChannelCombine = new JMenuItem("FourChannelCombine");
       fourChannelCombine.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 String cmd = e.getActionCommand();
                 if (cmd.equals("doFourChannelCombine")) {
                     doChannelCombine();
                 }
             }
       });
       fourChannelCombine.setActionCommand("doFourChannelCombine");
       tools.add(fourChannelCombine);
       
       JMenuItem convolve = new JMenuItem("Convolve");
       convolve.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 String cmd = e.getActionCommand();
                 if (cmd.equals("doConvolve")) {
                     doHyperToBroadBand();
                 }
             }
       });
       convolve.setActionCommand("doConvolve");
       tools.add(convolve);
       
       JMenu viewMenu = new JMenu("Window");
       viewMenu.getPopupMenu().setLightWeightPopupEnabled(false);
       JMenuItem newItem = new JMenuItem("New");
       newItem.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 try {
                    cloneMcV();
                 }
                 catch (Exception exc) {
                 exc.printStackTrace();
                 }
             }
       });
       viewMenu.add(newItem);
       menuBar.add(viewMenu);
       
       // get the Settings Menu
       JMenu settingsMenu = menuBar.getMenu(1);
       JMenu spectMenu = new JMenu("Spectrum");
       spectMenu.getPopupMenu().setLightWeightPopupEnabled(false);
       JMenu backGroundClr = new JMenu("Background Color");
       JRadioButtonMenuItem white = new JRadioButtonMenuItem("white", false);
       JRadioButtonMenuItem black = new JRadioButtonMenuItem("black", true);
       ButtonGroup bg = new ButtonGroup();
       bg.add(black);
       bg.add(white);
       white.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               try {
                  multiSpectDsp.setBackground(Color.white);
               } catch (Exception exc) {
                  exc.printStackTrace();
               }
           }
       });
       white.setActionCommand("white");
        
       black.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               try {
                  multiSpectDsp.setBackground(Color.black);
               } catch (Exception exc) {
                  exc.printStackTrace();
               }
           }
       });
       black.setActionCommand("black");
        
       backGroundClr.add(black);
       backGroundClr.add(white);
        
       spectMenu.add(backGroundClr);
       settingsMenu.add(spectMenu);

       
       return menuBar;
    }

    public void doChannelCombine() {
       final String idA = "A";
       final String idB = "B";
       final String idC = "C";
       final String idD = "D";
       
       // Use the datasource wrapper
       DataSource wrapDatSource = new MyDataSource(dataSourceId, dataChoice);
       final MyOperand operandA = new MyOperand(multiSpectDsp, idA, dataSourceId, dateTimeStamp, wrapDatSource);
       final MyOperand operandB = new MyOperand(multiSpectDsp, idB, dataSourceId, dateTimeStamp, wrapDatSource);
       final MyOperand operandC = new MyOperand(multiSpectDsp, idC, dataSourceId, dateTimeStamp, wrapDatSource);
       final MyOperand operandD = new MyOperand(multiSpectDsp, idD, dataSourceId, dateTimeStamp, wrapDatSource);

       Object obj;
       if (sourceDescription.contains("CrIS")) {
          obj = new CrIS_SDR_FourOperandCombine(dataBrowser, new Operand[] {operandA, operandB, operandC, operandD}, multiSpectDsp);
       }
       else {
          obj = new FourOperandCombine(dataBrowser, new Operand[] {operandA, operandB, operandC, operandD}, multiSpectDsp);
       }
       final FourOperandCombine widget = (FourOperandCombine) obj;
       
       JComponent gui = widget.buildGUI();
       gui.add(widget.makeActionComponent());

       operandA.isEmpty = false;
       operandB.isEmpty = false;

       try {
           multiSpectDsp.setInactive();
           //multiSpectDsp.hideChannelSelector(); why was this done???

           Gridded1DSet set = multiSpectDsp.getDomainSet();
           float lo = set.getLow()[0];
           float hi = set.getHi()[0];
           float fac = initXmapScale/multiSpectDsp.getXmapScale();
           float off = 0.04f*(hi - lo)*fac;

           float val = multiSpectDsp.getWaveNumber();
           float valA = val+off;
           valA = (valA>hi) ? (2*hi - valA) : valA;
           operandA.waveNumber = valA;
           multiSpectDsp.createSelector(idA, Color.red, valA);
           widget.updateOperandComp(0, Float.toString(valA)); 
           multiSpectDsp.addSelectorListener(idA, new SelectorListener(widget, operandA, 0));

           float valB = val-off;
           valB = (valB<lo) ? 2*lo-valB : valB;
           operandB.waveNumber = valB;
           multiSpectDsp.createSelector(idB, Color.magenta, valB);
           widget.updateOperandComp(1, Float.toString(valB));
           multiSpectDsp.addSelectorListener(idB, new SelectorListener(widget, operandB, 1));

           float valC = val+2*off;
           valC = (valC>hi) ? (2*hi - valC) : valC;
           multiSpectDsp.createSelector(idC, Color.orange, valC);
           multiSpectDsp.addSelectorListener(idC, new SelectorListener(widget, operandC, 2));
           multiSpectDsp.setSelectorVisible(idC, false);

           float valD = val-2*off;
           valD = (valD<lo) ? 2*lo-valD : valD;
           multiSpectDsp.createSelector(idD, Color.blue, valD);
           multiSpectDsp.addSelectorListener(idD, new SelectorListener(widget, operandD, 3));
           multiSpectDsp.setSelectorVisible(idD, false);

           //multiSpectDsp.showChannelSelector();

           multiSpectDsp.setActive();
       }
       catch (Exception exc) {
          exc.printStackTrace();
       }


       JFrame frame = Hydra.createAndShowFrame("FourChannelCombine", gui);
       frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
               multiSpectDsp.removeSelector(idA);
               multiSpectDsp.removeSelector(idB);
               multiSpectDsp.removeSelector(idC);
               multiSpectDsp.removeSelector(idD);
            }
          }
       );

       Point pt = getFrame().getLocation();
       frame.setLocation(pt.x,pt.y-60);
    }
    
    public void doHyperToBroadBand() {
        final String idA = "A";
        final String idB = "B";
        Object obj;
        if (sourceDescription.contains("CrIS")) {
           obj = new CrIS_SDR_HyperToBroadBand(idB, idA, dataBrowser, hydra.getDataSource(), multiSpectDsp, imgDisplay);       
        }
        else {
           obj = new HyperToBroadBand(idB, idA, dataBrowser, hydra.getDataSource(), multiSpectDsp, imgDisplay);
        }
        final HyperToBroadBand widget = (HyperToBroadBand) obj;
        JComponent gui = widget.buildGUI();
        Gridded1DSet set = multiSpectDsp.getDomainSet();
        float lo = set.getLow()[0];
        float hi = set.getHi()[0];
        float fac = initXmapScale/multiSpectDsp.getXmapScale();
        float off = 0.04f*(hi - lo)*fac;

        try {
           multiSpectDsp.setInactive();
           float val = multiSpectDsp.getWaveNumber();
           float valA = val+off;
           valA = (valA>hi) ? (2*hi - valA) : valA;
           valA = multiSpectDsp.findWaveNumber(valA);
           multiSpectDsp.createSelector(idA, Color.red, valA);
           widget.updateOperandComp(1, new Float(valA));
           multiSpectDsp.addSelectorListener(idA, new PropertyChangeListener() {
              public void propertyChange(PropertyChangeEvent pce) {
                 widget.updateOperandComp(1, pce.getNewValue());
              }
           });

           float valB = val-off;
           valB = (valB<lo) ? 2*lo-valB : valB;
           valB = multiSpectDsp.findWaveNumber(valB);
           multiSpectDsp.createSelector(idB, Color.magenta, valB);
           widget.updateOperandComp(0, new Float(valB));
           multiSpectDsp.addSelectorListener(idB, new PropertyChangeListener() {
              public void propertyChange(PropertyChangeEvent pce) {
                 widget.updateOperandComp(0, pce.getNewValue());
              }
           });
           multiSpectDsp.setActive();
        } 
        catch (Exception e) {
           e.printStackTrace();
        }
       
       gui.add(widget.makeActionComponent());
       
       JButton seenow = new JButton("display");
       class MyListener implements ActionListener {
         Compute compute;

         public MyListener(Compute compute) {
            this.compute = compute;
         }

         public void actionPerformed(ActionEvent e) {
             try {
                ((HyperToBroadBand)compute).getParentFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                
                class Task extends SwingWorker<String, Object> {
                   Data result = null;
                   @Override
                   public String doInBackground() {
                      try {
                         Compute clonedCompute = compute.clone();
                         result = clonedCompute.compute();
                      }
                      catch (Exception exc) {
                         exc.printStackTrace();
                      }
                      return "Done";
                   }
                   
                   @Override
                   protected void done() {
                      try {
                         ((HyperToBroadBand)compute).localCreateDisplay(result);
                      }
                      catch (Exception exc) {
                         exc.printStackTrace();
                      }
                      ((HyperToBroadBand)compute).getParentFrame().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                   }
                }
                (new Task()).execute();

             } catch (Exception exc) {
                exc.printStackTrace();
             }
         }
       };
       seenow.addActionListener(new MyListener(widget));
       gui.add(seenow);
       
       JFrame frame = Hydra.createAndShowFrame("Convolve", gui);
       ((HyperToBroadBand)widget).setParentFrame(frame);
       frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
               multiSpectDsp.removeSelector(idA);
               multiSpectDsp.removeSelector(idB);
               ((HyperToBroadBand)widget).destroy();
            }
          }
       );

       Point pt = getFrame().getLocation();
       frame.setLocation(pt.x,pt.y-60);
    }


    public JComponent doMakeMultiBandSelectComponent() {
         Object[] allBands = multiSpectData.getBandNames().toArray();
         final HashMap emisBandMap = multiSpectData.getBandNameMap();
         final JComboBox bandSelectComboBox = new JComboBox(allBands);
         bandSelectComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                 String bandName = (String)bandSelectComboBox.getSelectedItem();
                 if (bandName == null)
                    return;

                 Float waveNum = (Float) emisBandMap.get(bandName);
                 if (multiSpectDsp.setWaveNumber(waveNum.floatValue())) {
                     FlatField image = multiSpectDsp.getImageData();
                     image = reproject(image);
                     imgDisplay.updateImageData(image);
                     imgDisplay.getDepiction().setName(waveNum.toString());
                 }

            }
         });
         bandSelectComboBox.setSelectedItem(multiSpectData.init_bandName); 

         PropertyChangeListener listener = new PropertyChangeListener () {
            public void propertyChange(PropertyChangeEvent event) {
                float waveNumber = multiSpectDsp.getWaveNumber();
                try {
                  bandSelectComboBox.setSelectedIndex(multiSpectData.getChannelIndexFromWavenumber(waveNumber));
                  imgDisplay.getDepiction().setName(Float.toString(waveNumber));
                } catch (Exception e) {
                  e.printStackTrace();
                }
            }
         };
         multiSpectDsp.setListener(listener);

         return bandSelectComboBox;
    }

    public JComponent doMakeHyperSpectralSelectComponent() {
         final JTextField wavenumbox = new JTextField(Float.toString(multiSpectDsp.getWaveNumber()), 5);
         final ActionListener doWavenumChange = new ActionListener()  {
              public void actionPerformed(ActionEvent e) {
                  String tmp = wavenumbox.getText().trim();
                  if (multiSpectDsp.setWaveNumber(Float.valueOf(tmp))) {
                     String txt = Float.toString(multiSpectDsp.getWaveNumber());
                     wavenumbox.setText(txt);
                     FlatField image = multiSpectDsp.getImageData();
                     image = reproject(image);
                     imgDisplay.updateImageData(image);
                     imgDisplay.getDepiction().setName(txt);
                  }
                  else {
                     wavenumbox.setText(Float.toString(multiSpectDsp.getWaveNumber()));
                  }
              }
         };
         wavenumbox.addActionListener(doWavenumChange);
         wavenumbox.addFocusListener(new FocusListener() {
              public void focusGained(FocusEvent e) {
              }
              public void focusLost(FocusEvent e) {
                 doWavenumChange.actionPerformed(null);
              }       
          });

         PropertyChangeListener listener = new PropertyChangeListener () {
            public void propertyChange(PropertyChangeEvent event) {
                FlatField image = multiSpectDsp.getImageData();
                image = reproject(image);
                float waveNumber = multiSpectDsp.getWaveNumber();
                String txt = Float.toString(waveNumber);
                wavenumbox.setText(txt);
                imgDisplay.updateImageData(image);
                imgDisplay.getDepiction().setName(txt);
            }
         };
         multiSpectDsp.setListener(listener);

         return wavenumbox;
    }

    public JComponent doMakeChannelSelectComponent(boolean comboBoxSelect) {
       if (comboBoxSelect) {
          return doMakeMultiBandSelectComponent();
       }
       else {
          return doMakeHyperSpectralSelectComponent();
       }
    }

    FlatField reproject(FlatField image) {
       try {
          if (sourceDescription.contains("CrIS")) {
             image = edu.wisc.ssec.adapter.CrIS_SDR_Utility.reprojectCrIS_SDR_swath(image);
          }
       }
       catch (Exception e) {
          e.printStackTrace();
       }

       return image;
    }
}

class ProbeLocationChange extends CellImpl {
    DataReference probeLocationRef;
    DataReference spectrumRef;
    MultiSpectralData multiSpectData;
    boolean init = false;

    public ProbeLocationChange(DataReference probeLocationRef, DataReference spectrumRef, MultiSpectralData multiSpectData) throws VisADException, RemoteException {
       this.probeLocationRef = probeLocationRef;
       this.spectrumRef = spectrumRef;
       this.multiSpectData = multiSpectData;
       this.addReference(probeLocationRef);
    }

    public synchronized void doAction() throws VisADException, RemoteException {
        if (init) {
           LatLonTuple tup = (LatLonTuple) probeLocationRef.getData();
           try {
              spectrumRef.setData(multiSpectData.getSpectrum(tup));
           } catch (Exception e) {
              e.printStackTrace();
           }
        } else {
            init = true;
        }
    }
}

class MyDataSource extends DataSource {
   int dataSourceId;
   DataSource dataSource;
   DataChoice dataChoice;
   
   MyDataSource(int dataSourceId, DataChoice dataChoice) {
      
      this.dataSourceId = dataSourceId;
      this.dataSource = Hydra.getDataSource(dataSourceId);
      this.dataChoice = dataChoice;
   }
   
   public boolean getDoReproject(DataChoice obj) {
      return false; 
   }
   
   public int getDataSourceId() {
      return dataSourceId;
   }

   @Override
   public String getDateTimeStamp() {
      return dataSource.getDateTimeStamp();
   }

   @Override
   public String getDescription() {
      return dataSource.getDescription();
   }

   @Override
   public String getDescription(DataChoice choice) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public boolean getDoFilter(DataChoice choice) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public boolean getOverlayAsMask(DataChoice choice) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public float getNadirResolution(DataChoice choice) throws Exception {
      return dataSource.getNadirResolution(choice);
   }

   @Override
   public Data getData(DataChoice dataChoice, DataSelection dataSelection) throws VisADException, RemoteException {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public boolean canUnderstand(File[] files) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public Date getDateTime() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }
}

class MyOperand extends Operand {
    float waveNumber;
    MultiSpectralDisplay multiSpectDsp;

    MyOperand(MultiSpectralDisplay multiSpectDsp, String id, int dataSourceId, String dateTimeStr, DataSource dataSource) {
       this.multiSpectDsp = multiSpectDsp;
       this.id = id;
       this.dataSourceId = dataSourceId;
       this.dateTimeStr = dateTimeStr;
       this.dataSource = dataSource;
    }

    public Data getData() throws VisADException, RemoteException {
       FlatField data = multiSpectDsp.getImageDataFrom(waveNumber);
       return data; 
    }

    public void disable() {
       isEmpty = true;
       multiSpectDsp.setSelectorVisible(id, false);
    }

    public void enable() {
       isEmpty = false;
       try {
          waveNumber = multiSpectDsp.getSelectorValue(id);
          multiSpectDsp.setSelectorValue(id, waveNumber);
       }
       catch (Exception e) {
          e.printStackTrace();
       }
       multiSpectDsp.setSelectorVisible(id, true);
    }

    public String getName() {
       return (dataSourceId+":"+Float.toString(waveNumber)+" ");
    }

    public Operand clone() {
       MyOperand operand = new MyOperand(this.multiSpectDsp, this.id, this.dataSourceId, this.dateTimeStr, this.dataSource);
       operand.waveNumber = this.waveNumber;
       operand.isEmpty = this.isEmpty;
       return operand;
    }
}

class CrIS_SDR_FourOperandCombine extends FourOperandCombine {

   public CrIS_SDR_FourOperandCombine(DataBrowser dataBrowser, Operand[] operands, MultiSpectralDisplay multiSpectDsp) {
      super(dataBrowser, operands, multiSpectDsp);
   }

   public FlatField reproject(FlatField swath) throws Exception {
      FlatField grid = edu.wisc.ssec.adapter.CrIS_SDR_Utility.reprojectCrIS_SDR_swath(swath);
      return grid;
   }

   public CrIS_SDR_FourOperandCombine clone() {
      CrIS_SDR_FourOperandCombine clone = new CrIS_SDR_FourOperandCombine(this.dataBrowser, this.operands, this.multiSpectDsp);
      copy(clone);
      clone.operationAB = this.operationAB;
      clone.operationCD = this.operationCD;
      clone.operationLR = this.operationLR;
      return clone;
   }
}

class CrIS_SDR_HyperToBroadBand extends HyperToBroadBand {
   DataSource dataSource;

   public CrIS_SDR_HyperToBroadBand(String idLeft, String idRght, DataBrowser dataBrowser, DataSource dataSource, MultiSpectralDisplay msd, ImageDisplay imageDisplay) {
      super(idLeft, idRght, dataBrowser, dataSource, msd, imageDisplay);
      this.dataSource = dataSource;
   }

   public FlatField reproject(FlatField swath) throws Exception {
      FlatField grid = edu.wisc.ssec.adapter.CrIS_SDR_Utility.reprojectCrIS_SDR_swath(swath);
      return grid;
   }

   public CrIS_SDR_HyperToBroadBand clone() {
      CrIS_SDR_HyperToBroadBand clone = new CrIS_SDR_HyperToBroadBand(this.selectorID_left, this.selectorID_rght, this.dataBrowser, this.dataSource, this.multiSpectDsp, this.imageDisplay);
      clone.dataBrowser = this.dataBrowser;
      clone.multiSpectDsp = this.multiSpectDsp;
      clone.cntrWavenum = multiSpectDsp.getWaveNumber();
      clone.cntrWavenum = this.cntrWavenum;
      clone.wavenumL = this.wavenumL;
      clone.wavenumR = this.wavenumR;
      clone.imageDisplay = this.imageDisplay;
      clone.operands = new Operand[] {this.operands[0]};
      clone.kernel = this.kernel;
      copy(clone);
      return clone;
   }
}

class SelectorListener implements PropertyChangeListener {
    Compute widget;
    MyOperand operand;
    int index;

    public SelectorListener(Compute widget, MyOperand operand, int index) {
       this.widget = widget;
       this.operand = operand;
       this.index = index;
    }

    public void propertyChange(PropertyChangeEvent event) {
       Float flt = (Float) event.getNewValue();
       String str = flt.toString();
       widget.updateOperandComp(index, str);
       operand.waveNumber = flt.floatValue();
    }
}
