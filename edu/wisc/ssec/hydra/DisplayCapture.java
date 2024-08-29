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
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import java.io.File;

import visad.VisADException;
import visad.RealTupleType;
import visad.georef.MapProjection;
import visad.georef.EarthLocation;
import visad.georef.TrivialMapProjection;

import java.rmi.RemoteException;

import ucar.unidata.util.LogUtil;

import ucar.unidata.view.geoloc.MapProjectionDisplay;
import visad.DisplayImpl;
import visad.DisplayRenderer;
import visad.LocalDisplay;


public class DisplayCapture {

    JFileChooser fc = new JFileChooser();
    MapProjectionDisplay mapProjDsp;
    double[] lastProjMatrix;
    MapProjection lastMapProj;
    ImageDisplay imageDisplay;
    JDialog dialog;

    public DisplayCapture(final JFrame parent, final ImageDisplay imageDisplay) throws VisADException, RemoteException {
        this.imageDisplay = imageDisplay;
        this.mapProjDsp = (MapProjectionDisplay) imageDisplay.getDisplayMaster();
        this.imageDisplay.setProbeVisible(false);

        dialog = new JDialog(parent, "KML capture");
        dialog.setLocationRelativeTo(parent);
        dialog.addWindowListener(new WindowAdapter() {
                                     public void windowClosing(WindowEvent e) {
                                         resetAfterKMLcaptureDone();
                                     }
                                 }
        );

        lastMapProj = imageDisplay.getMapProjection();
        imageDisplay.setDisplayLinking(false);
        lastProjMatrix = imageDisplay.getProjectionMatrix();

        Rectangle2D bounds = mapProjDsp.getLatLonBox();
        mapProjDsp.setMapProjection(new TrivialMapProjection(RealTupleType.SpatialEarth2DTuple, bounds));

        JPanel optPanel = new JPanel(new FlowLayout());
        JCheckBox boundaryToggle = new JCheckBox("Map Boundaries", true);
        boundaryToggle.addItemListener(new ItemListener() {
                                           public void itemStateChanged(ItemEvent e) {
                                               if (e.getStateChange() == ItemEvent.DESELECTED) {
                                                   imageDisplay.toggleMapBoundaries(false);
                                               } else {
                                                   imageDisplay.toggleMapBoundaries(true);
                                               }
                                           }
                                       }
        );
        optPanel.add(boundaryToggle);

        JPanel actPanel = new JPanel(new FlowLayout());

        final JDialog fcParent = dialog;
        JButton saveButton = new JButton("Save");
        saveButton.setActionCommand("Save");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("Save")) {
                    int retVal = fc.showSaveDialog(fcParent);
                    if (retVal == JFileChooser.APPROVE_OPTION) {
                        try {
                            saveKML(fc.getSelectedFile());
                        } catch (VisADException exc) {
                        } catch (RemoteException exc) {
                        }
                    }
                }
            }
        });

        JButton closeButton = new JButton("Close");
        closeButton.setActionCommand("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("Close")) {
                    resetAfterKMLcaptureDone();
                }
            }
        });

        actPanel.add(closeButton);
        actPanel.add(saveButton);

        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.add(optPanel);
        panel.add(actPanel);

        dialog.setContentPane(panel);
        dialog.getRootPane().setDefaultButton(saveButton);
        dialog.validate();
        dialog.setVisible(true);
        dialog.setSize(dialog.getPreferredSize());
    }

    private void resetAfterKMLcaptureDone() {
        imageDisplay.setMapProjection(lastMapProj);
        imageDisplay.setProjectionMatrix(lastProjMatrix);
        imageDisplay.setDisplayLinking(true);
        imageDisplay.setProbeVisible(true);
        imageDisplay.toggleMapBoundaries(true);
        dialog.setVisible(false);
    }

    void saveKML(File file) throws VisADException, RemoteException {
        String path = file.getAbsolutePath();
        String kmlPath = path + ".kml";
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        final String imagePath = path;
        java.io.File tmpF = new java.io.File(imagePath);
        String imageName = tmpF.getName();

        Rectangle2D screenBounds = mapProjDsp.getScreenBounds();
        EarthLocation ul = mapProjDsp.screenToEarthLocation(0, 0);
        EarthLocation lr = mapProjDsp.screenToEarthLocation((int) screenBounds.getWidth(), (int) screenBounds.getHeight());
        double north = ul.getLatLonPoint().getLatitude().getValue();
        double west = ul.getLatLonPoint().getLongitude().getValue();
        double south = lr.getLatLonPoint().getLatitude().getValue();
        double east = lr.getLatLonPoint().getLongitude().getValue();
        Hydra.makeKML(south, north, west, east, kmlPath, imageName);

        captureDisplay(imagePath);
    }


    void captureDisplay(final String imagePath) {
        final MapProjectionDisplay dsp = mapProjDsp;
        Runnable captureImage = new Runnable() {
            public void run() {
                dsp.saveCurrentDisplay(new java.io.File(imagePath), true, true);
            }
        };
        Thread t = new Thread(captureImage);
        t.start();
    }

    public void captureJPEG(JFrame frame) {
        int retVal = fc.showSaveDialog(frame);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fc.getSelectedFile();
            String path = file.getAbsolutePath();
            if (!path.endsWith(".jpeg")) {
                path = path + ".jpeg";
            }
            String imagePath = path;

            captureDisplay(imagePath);
        }
    }

    public static void capture(JFrame frame, final MapProjectionDisplay dsp, String suffix) {
        capture(frame, dsp.getDisplay(), suffix);
    }

    public static void capture(JFrame frame, final LocalDisplay dsp, String suffix) {
        JFileChooser fc = new JFileChooser();
        int retVal = fc.showSaveDialog(frame);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fc.getSelectedFile();
            String path = file.getAbsolutePath();
            if (!path.endsWith(suffix)) {
                path = path + suffix;
            }
            String imagePath = path;

            captureDisplay(imagePath, dsp);
        }
    }

    public static void captureDisplay(final String imagePath, final MapProjectionDisplay dsp) {
        Runnable captureImage = new Runnable() {
            public void run() {
                // dsp.saveCurrentDisplay(new java.io.File(imagePath), true, true);
                saveCurrentDisplay(dsp.getDisplay(), new java.io.File(imagePath), true, true, 1.0f);
            }
        };
        Thread t = new Thread(captureImage);
        t.start();
    }

    public static void captureDisplay(final String imagePath, final LocalDisplay display) {
        Runnable captureImage = new Runnable() {
            public void run() {
                saveCurrentDisplay(display, new java.io.File(imagePath), true, true, 1.0f);
            }
        };
        Thread t = new Thread(captureImage);
        t.start();
    }

    /**
     * Capture the display's current image and save it to a file as an image
     * (eg, JPEG, png). If <code>doSync</code> is true, then the calling
     * thread will block until rendering is complete.
     *
     * @param toFile  The file to which to save the current image.
     * @param doSync  Whether or not to wait until the display is stable.
     * @param block   Whether or not to wait until the image is saved.
     * @param quality JPEG quality
     */
    public static void saveCurrentDisplay(final LocalDisplay display, File toFile, final boolean doSync,
                                          boolean block, final float quality) {
        // user has requested saving display as an image
        final File saveFile = toFile;

        try {
            Runnable captureImage = new Runnable() {
                public void run() {
                    DisplayRenderer renderer = display.getDisplayRenderer();
                    BufferedImage image;
                    Thread thread = Thread.currentThread();

                    //A hack to make use of the syncing feature in DisplayImpl
                    if (display instanceof DisplayImpl) {
                        renderer.setWaitMessageVisible(false);
                        image = ((DisplayImpl) display).getImage(doSync);
                        //                        System.err.println (display.getClass().getName ()+": image = " + image.getClass().getName ());
                        renderer.setWaitMessageVisible(true);
                    } else {
                        image = display.getImage();
                    }

                    try {
                        ucar.unidata.ui.ImageUtils.writeImageToFile(image,
                                saveFile.toString(), quality);
                    } catch (Exception err) {
                        LogUtil.logException("Problem saving image", err);
                    }
                }
            };
            Thread t = new Thread(captureImage);

            //For some reason visad does not allow a getImage call
            //from an AWT-EventQueue thread. So we won't block here
            //so the getImage will be called from this new thread
            if (block) {
                t.run();
            } else {
                t.start();
            }
        } catch (Exception exp) {
            LogUtil.logException("Problem saving image", exp);
        }
    }
}
