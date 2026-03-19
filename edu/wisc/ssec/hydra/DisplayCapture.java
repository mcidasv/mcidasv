/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
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

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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

import java.io.*;
import java.io.File;
import java.io.FileWriter;

import java.nio.charset.StandardCharsets;

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

    public DisplayCapture(final JFrame parent, final ImageDisplay imageDisplay)
            throws VisADException, RemoteException {

        this.imageDisplay = imageDisplay;
        this.mapProjDsp = (MapProjectionDisplay) imageDisplay.getDisplayMaster();
        this.imageDisplay.setProbeVisible(false);

        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(
            new javax.swing.filechooser.FileNameExtensionFilter("KMZ files (*.kmz)", "kmz")
        );
        chooser.setFileFilter(chooser.getChoosableFileFilters()[0]);

        int retVal = chooser.showSaveDialog(parent);

        if (retVal == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            String path = selected.getAbsolutePath();

            if (!path.toLowerCase().endsWith(".kmz")) {
                path += ".kmz";
            }

            try {
                saveKMZ(new File(path));
            } catch (Exception exc) {
                exc.printStackTrace();
                JOptionPane.showMessageDialog(parent,
                    "Error saving KMZ:\n" + exc.getMessage(),
                    "KMZ Save Error",
                    JOptionPane.ERROR_MESSAGE);
                resetAfterKMLcaptureDone();
            }

        } else {
            resetAfterKMLcaptureDone();
        }
    }

    private void resetAfterKMLcaptureDone() {
        imageDisplay.setDisplayLinking(true);
        imageDisplay.setProbeVisible(true);
        dialog.setVisible(false);
    }

    void saveKMZ(File file) {

        final String kmzPath = file.getAbsolutePath().toLowerCase().endsWith(".kmz")
                ? file.getAbsolutePath()
                : file.getAbsolutePath() + ".kmz";

        final File kmzFile = new File(kmzPath);
        final File parentDir = kmzFile.getParentFile();
        final String imagePath = kmzPath.substring(0, kmzPath.lastIndexOf(".")) + ".png";

        final MapProjectionDisplay dsp = mapProjDsp;

        captureDisplay(imagePath, dsp, () -> {

            try {

                BufferedImage fullImage = ImageIO.read(new File(imagePath));

                int width = fullImage.getWidth();
                int height = fullImage.getHeight();

                final int TILES = 6;

                int tileW = width / TILES;
                int tileH = height / TILES;

                File kmlTemp = new File(parentDir, "doc.kml");

                try (BufferedWriter fw = new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(kmlTemp),
                                StandardCharsets.UTF_8))) {

                    fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                    fw.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\" ");
                    fw.write("xmlns:gx=\"http://www.google.com/kml/ext/2.2\">\n");
                    fw.write("<Document>\n");

                    for (int row = 0; row < TILES; row++) {
                        for (int col = 0; col < TILES; col++) {

                            int x0 = col * tileW;
                            int y0 = row * tileH;

                            int x1 = (col == TILES - 1) ? width  : x0 + tileW;
                            int y1 = (row == TILES - 1) ? height : y0 + tileH;

                            BufferedImage tile =
                                    fullImage.getSubimage(x0, y0, x1 - x0, y1 - y0);

                            String tileName = "tile_" + row + "_" + col + ".png";

                            File tileFile = new File(parentDir, tileName);

                            ImageIO.write(tile, "png", tileFile);

                            EarthLocation ul = dsp.screenToEarthLocation(x0, y0);
                            EarthLocation ur = dsp.screenToEarthLocation(x1, y0);
                            EarthLocation ll = dsp.screenToEarthLocation(x0, y1);
                            EarthLocation lr = dsp.screenToEarthLocation(x1, y1);

                            double latUL = ul.getLatLonPoint().getLatitude().getValue();
                            double lonUL = normalizeLon(
                                    ul.getLatLonPoint().getLongitude().getValue());

                            double latUR = ur.getLatLonPoint().getLatitude().getValue();
                            double lonUR = normalizeLon(
                                    ur.getLatLonPoint().getLongitude().getValue());

                            double latLL = ll.getLatLonPoint().getLatitude().getValue();
                            double lonLL = normalizeLon(
                                    ll.getLatLonPoint().getLongitude().getValue());

                            double latLR = lr.getLatLonPoint().getLatitude().getValue();
                            double lonLR = normalizeLon(
                                    lr.getLatLonPoint().getLongitude().getValue());

                            fw.write("<GroundOverlay>\n");
                            fw.write("<Icon><href>" + tileName + "</href></Icon>\n");

                            fw.write("<gx:LatLonQuad>\n");
                            fw.write("<coordinates>\n");

                            fw.write(lonLL + "," + latLL + "\n");
                            fw.write(lonLR + "," + latLR + "\n");
                            fw.write(lonUR + "," + latUR + "\n");
                            fw.write(lonUL + "," + latUL + "\n");

                            fw.write("</coordinates>\n");
                            fw.write("</gx:LatLonQuad>\n");
                            fw.write("</GroundOverlay>\n");
                        }
                    }

                    fw.write("</Document>\n");
                    fw.write("</kml>\n");
                }

                try (java.util.zip.ZipOutputStream zos =
                        new java.util.zip.ZipOutputStream(
                                new FileOutputStream(kmzFile))) {

                    addToZip(kmlTemp, "doc.kml", zos);

                    for (int row = 0; row < TILES; row++) {
                        for (int col = 0; col < TILES; col++) {

                            String tileName = "tile_" + row + "_" + col + ".png";

                            File tileFile = new File(parentDir, tileName);

                            addToZip(tileFile, tileName, zos);

                            tileFile.delete();
                        }
                    }
                }

                kmlTemp.delete();
                new File(imagePath).delete();

            } catch (Exception e) {
                e.printStackTrace();
            }

            resetAfterKMLcaptureDone();
        });
    }

    private void addToZip(File file, String entryName,
                          java.util.zip.ZipOutputStream zos) throws IOException {

        try (FileInputStream fis = new FileInputStream(file)) {

            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryName);
            zos.putNextEntry(entry);

            byte[] buffer = new byte[4096];
            int len;

            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            zos.closeEntry();
        }
    }

    private double normalizeLon(double lon) {
        if (lon > 180.0)  lon -= 360.0;
        if (lon < -180.0) lon += 360.0;
        return lon;
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

            captureDisplay(imagePath, mapProjDsp);
        }
    }

    public static void capture(JFrame frame, final MapProjectionDisplay dsp, String suffix) {
        capture(frame, dsp.getDisplay(), suffix);
    }

    public static void capture(JFrame frame, final LocalDisplay dsp, String defaultSuffix) {

        JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);

        javax.swing.filechooser.FileNameExtensionFilter filter =
            new javax.swing.filechooser.FileNameExtensionFilter(
                "Image files (*.jpg, *.png, *.gif)", "jpg", "png", "gif");

        fc.addChoosableFileFilter(filter);
        fc.setFileFilter(filter);

        int retVal = fc.showSaveDialog(frame);

        if (retVal == JFileChooser.APPROVE_OPTION) {

            java.io.File file = fc.getSelectedFile();
            String path = file.getAbsolutePath().toLowerCase();

            String suffix = "jpg"; // default

            if (path.endsWith(".png")) {
                suffix = "png";
            } else if (path.endsWith(".gif")) {
                suffix = "gif";
            } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                suffix = "jpg";
            } else {
                // No extension typed → default to JPG
                path = file.getAbsolutePath() + ".jpg";
            }

            captureDisplay(path, dsp);
        }
    }

    public static void captureDisplay(final String imagePath, final MapProjectionDisplay dsp) {
        saveCurrentDisplay(dsp.getDisplay(), new java.io.File(imagePath), true, true, 1.0f);
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

    void captureDisplay(final String imagePath, final MapProjectionDisplay dsp, Runnable after) {
        new Thread(() -> {
            try {
                // saveCurrentDisplay blocks until the PNG is fully written
                dsp.saveCurrentDisplay(new File(imagePath), true, true);
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.logException("Problem saving PNG for KML", e);
                JOptionPane.showMessageDialog(null,
                        "Error saving PNG for KML:\n" + e.getMessage(),
                        "PNG Save Error",
                        JOptionPane.ERROR_MESSAGE);
                resetAfterKMLcaptureDone();
                return; // stop if PNG failed
            }

            if (after != null) {
                after.run();
            }
        }).start();
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