/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.WindowEvent;

import java.util.List;

import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.SwathSoundingData;

import visad.*;
import visad.georef.LatLonTuple;
import visad.georef.MapProjection;

import java.rmi.RemoteException;

import ucar.unidata.util.ColorTable;
import edu.wisc.ssec.hydra.data.DataChoice;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;


public class AtmSoundingViewer extends HydraDisplay {

    AtmSoundingDisplay atmSoundingDsp = null;
    JComponent channelSelectComp = null;
    MultiSpectralData multiSpectData = null;
    SwathSoundingData swathSoundingData = null;
    ImageDisplay imgDisplay = null;
    DataChoice dataChoice = null;
    List dataChoices = null;

    String sourceDescription = null;
    String dateTimeStamp = null;
    String fldName = null;
    String baseDescription = null;
    int dataSourceId;

    JMenuBar menuBar = null;

    JFrame frame = null;

    Hydra hydra = null;
    DataBrowser dataBrowser = null;

    public AtmSoundingViewer(DataChoice dataChoice, String sourceDescription, String dateTimeStamp, int windowNumber, int dataSourceId) throws Exception {
        this.dataChoice = dataChoice;
        this.sourceDescription = sourceDescription;
        this.dateTimeStamp = dateTimeStamp;
        this.dataSourceId = dataSourceId;

        atmSoundingDsp = new AtmSoundingDisplay((DataChoice) dataChoice);
        atmSoundingDsp.showChannelSelector();
        final AtmSoundingDisplay msd = atmSoundingDsp;

        FlatField image = msd.getImageData();
        image = reproject(image);

        MapProjection mapProj = Hydra.getDataProjection(image);

        ColorTable clrTbl = Hydra.invGrayTable;

        HydraRGBDisplayable imageDsp = Hydra.makeImageDisplayable(image, null, clrTbl, fldName);

        String str = null;
        if (windowNumber > 0) {
            imgDisplay = new ImageDisplay(imageDsp, mapProj, windowNumber, false, false);
        } else {
            imgDisplay = new ImageDisplay(imageDsp, mapProj, false, false);
        }

        imgDisplay.onlyOverlayNoReplace = true;
        final ImageDisplay iDisplay = imgDisplay;

        swathSoundingData = atmSoundingDsp.getSwathSoundingData();

        channelSelectComp = doMakeChannelSelectComponent();

        final SwathSoundingData data = swathSoundingData;

        ReadoutProbe probe = iDisplay.getReadoutProbe();
        DataReference probeLocationRef = probe.getEarthLocationRef();
        DataReference spectrumRef = msd.getSpectrumRef();
        spectrumRef.setData(data.getSounding((LatLonTuple) probeLocationRef.getData()));
        ProbeLocationChange probeChangeListnr = new ProbeLocationChange(probeLocationRef, spectrumRef, data);

        DataReference probeLocationRefB = (iDisplay.addReadoutProbe(Color.cyan, 0.08, 0.08)).getEarthLocationRef();
        DataReference spectrumRefB = new DataReferenceImpl("spectrumB");
        spectrumRefB.setData(msd.getSwathSoundingData().getSounding((LatLonTuple) probeLocationRefB.getData()));
        msd.addRef(spectrumRefB, Color.cyan);
        ProbeLocationChange probeChangeListnrB = new ProbeLocationChange(probeLocationRefB, spectrumRefB, data);

        JComponent comp = doMakeComponent();

        menuBar = buildMenuBar();

        String title;
        if (windowNumber > 0) {
            title = "Window " + windowNumber;
        } else {
            title = sourceDescription + " " + dateTimeStamp;
        }
        frame = Hydra.createAndShowFrame(title, comp, menuBar,
                new Dimension(760, 480), new Point(220, 0));
        frame.addWindowListener(this);
        iDisplay.setParentFrame(frame);
    }

    public void setDataChoices(List dataChoices) {
        this.dataChoices = dataChoices;
    }

    public JFrame getFrame() {
        return frame;
    }

    public void windowClosing(WindowEvent e) {
        imgDisplay.windowClosing(e);
    }

    public JComponent doMakeComponent() {
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                imgDisplay.getComponent(), atmSoundingDsp.getDisplayComponent());
        if (SwingUtilities.isEventDispatchThread()) {
            imgDisplay.getControlPanel().add(channelSelectComp);
            splitPane.setDividerLocation(400);
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        imgDisplay.getControlPanel().add(channelSelectComp);
                        splitPane.setDividerLocation(400);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return splitPane;
    }

    public JMenuBar buildMenuBar() {
        JMenuBar menuBar = imgDisplay.getMenuBar();

        JMenu hlpMenu = new JMenu("Help");
        hlpMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        JTextArea jt = new JTextArea(" Spectrum Display:\n   Zoom (rubber band): SHFT+DRAG \n   Reset: CNTL+CLCK");
        jt.setEditable(false);
        jt.setCursor(null);
        jt.setFocusable(false);
        hlpMenu.add(jt);

        menuBar.add(hlpMenu);

        // get the tools JMenu
        JMenu tools = menuBar.getMenu(0);
        JMenuItem fourChannelCombine = new JMenuItem("FourChannelCombine");
        fourChannelCombine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String cmd = e.getActionCommand();
                if (cmd.equals("doFourChannelCombine")) {
                    //doChannelCombine();
                }
            }
        });
        fourChannelCombine.setActionCommand("doFourChannelCombine");

        JMenu paramMenu = new JMenu("Parameter");
        paramMenu.getPopupMenu().setLightWeightPopupEnabled(false);

        JMenuItem tempItem = new JMenuItem("Temperature");
        tempItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    AtmSoundingViewer mcv = new AtmSoundingViewer((DataChoice) dataChoices.get(0), sourceDescription, dateTimeStamp, -1, dataSourceId);
                    // TODO: this sucks
                    mcv.setDataChoices(dataChoices);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
        paramMenu.add(tempItem);

        JMenuItem wvItem = new JMenuItem("WaterVapor");
        wvItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    AtmSoundingViewer mcv = new AtmSoundingViewer((DataChoice) dataChoices.get(1), sourceDescription, dateTimeStamp, -1, dataSourceId);
                    // TODO: this sucks
                    mcv.setDataChoices(dataChoices);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
        paramMenu.add(wvItem);

        JMenuItem o3Item = new JMenuItem("Ozone");
        o3Item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    AtmSoundingViewer mcv = new AtmSoundingViewer((DataChoice) dataChoices.get(2), sourceDescription, dateTimeStamp, -1, dataSourceId);
                    // TODO: this sucks
                    mcv.setDataChoices(dataChoices);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
        paramMenu.add(o3Item);

        menuBar.add(paramMenu);

        // get the Settings Menu
        JMenu settingsMenu = menuBar.getMenu(1);
        JMenu soundMenu = new JMenu("Sounding");
        soundMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        JMenu backGroundClr = new JMenu("Background Color");
        JRadioButtonMenuItem white = new JRadioButtonMenuItem("white", false);
        JRadioButtonMenuItem black = new JRadioButtonMenuItem("black", true);
        ButtonGroup bg = new ButtonGroup();
        bg.add(black);
        bg.add(white);
        white.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    atmSoundingDsp.setBackground(Color.white);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
        white.setActionCommand("white");

        black.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    atmSoundingDsp.setBackground(Color.black);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
        black.setActionCommand("black");

        backGroundClr.add(black);
        backGroundClr.add(white);

        soundMenu.add(backGroundClr);
        settingsMenu.add(soundMenu);


        return menuBar;
    }

    public JComponent doMakeMultiBandSelectComponent() {
        final float[] levels = swathSoundingData.getSoundingLevels();
        String[] levelNames = new String[levels.length];
        for (int k = 0; k < levelNames.length; k++) {
            levelNames[k] = Float.toString(levels[k]);
        }
        final JComboBox comboBox = new JComboBox(levelNames);
        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int idx = comboBox.getSelectedIndex();
                String levelName = (String) comboBox.getSelectedItem();
                if (atmSoundingDsp.setWaveNumber(levels[idx])) {
                    FlatField image = atmSoundingDsp.getImageData();
                    image = reproject(image);
                    imgDisplay.updateImageData(image);
                    imgDisplay.getDepiction().setName(levelName);
                }

            }
        });

        try {
            comboBox.setSelectedIndex(swathSoundingData.getLevelIndexFromLevel(swathSoundingData.init_level));
        } catch (Exception e) {
            e.printStackTrace();
        }

        PropertyChangeListener listener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                float waveNumber = atmSoundingDsp.getWaveNumber();
                try {
                    comboBox.setSelectedIndex(swathSoundingData.getLevelIndexFromLevel(waveNumber));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        atmSoundingDsp.setListener(listener);

        return comboBox;
    }

    public JComponent doMakeChannelSelectComponent() {
        return doMakeMultiBandSelectComponent();
    }

    FlatField reproject(FlatField image) {
        try {
            if (sourceDescription.contains("CrIS")) {
                image = edu.wisc.ssec.adapter.CrIS_SDR_Utility.reprojectCrIS_SDR_swath(image);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return image;
    }


    public static class ProbeLocationChange extends CellImpl {
        DataReference probeLocationRef;
        DataReference spectrumRef;
        SwathSoundingData swathSoundingData;
        boolean init = false;

        public ProbeLocationChange(DataReference probeLocationRef, DataReference spectrumRef, SwathSoundingData swathSoundingData) throws VisADException, RemoteException {
            this.probeLocationRef = probeLocationRef;
            this.spectrumRef = spectrumRef;
            this.swathSoundingData = swathSoundingData;
            this.addReference(probeLocationRef);
        }

        public synchronized void doAction() throws VisADException, RemoteException {
            if (init) {
                LatLonTuple tup = (LatLonTuple) probeLocationRef.getData();
                try {
                    spectrumRef.setData(swathSoundingData.getSounding(tup));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                init = true;
            }
        }
    }

}
