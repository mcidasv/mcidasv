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
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.BoxLayout;
import javax.swing.border.LineBorder;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Point;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.awt.event.ComponentEvent;

import java.util.Iterator;
import java.util.ArrayList;

import java.lang.Runnable;

import ucar.visad.display.DisplayMaster;
import ucar.visad.display.MapLines;
import ucar.visad.display.DisplayableData;

import ucar.unidata.util.ColorTable;

import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;

import visad.georef.MapProjection;
import visad.georef.EarthLocationTuple;


import visad.*;
import visad.MouseBehavior;
import visad.util.Util;
import visad.java3d.DisplayRendererJ3D;
import visad.java3d.GraphicsModeControlJ3D;

import java.rmi.RemoteException;

import edu.wisc.ssec.hydra.data.DataSource;
import edu.wisc.ssec.adapter.MultiSpectralData;


public class ImageDisplay extends HydraDisplay implements ActionListener, ControlListener {

    JFrame frame;

    MapProjectionDisplayJ3D mapProjDsp;
    DisplayMaster dspMaster;

    MapProjection mapProjection;

    MapProjection lastMapProjection;

    DisplayableData imageDsp = null;

    Depiction imageDepiction = null;

    ReadoutProbe probe = null;

    ReadoutProbe minMarker = null;

    ReadoutProbe maxMarker = null;

    ArrayList<ReadoutProbe> readoutProbeList = new ArrayList<>();

    Transect transect = null;

    ProjectionControl pCntrl = null;

    ColorControl clrCntrl = null;

    DepictionControl imgCntrl = null;

    private double[] currentMatrix = new double[16];

    static ArrayList<ImageDisplay> shareList = new ArrayList<>();

    static int numImageWindowsCreated = 0;

    private boolean share = true;

    private boolean shareSize = true;

    public static final double baseScale = 0.65;

    double initScale;

    private MouseBehavior mouseBehav = null;

    private DisplayRendererJ3D dspRenderer = null;

    private ArrayList<MapLines> mapBoundaryList = null;

    final ImageIcon lnkIc = new ImageIcon(ImageDisplay.class.getResource("/resources/icons/link.png"));
    final ImageIcon ulnkIc = new ImageIcon(ImageDisplay.class.getResource("/resources/icons/link_break.png"));
    final JButton linkButton = new JButton(lnkIc);

    JCheckBoxMenuItem maplines = null;

    JCheckBoxMenuItem minmaxItem = null;

    JCheckBoxMenuItem probeItem = null;

    JCheckBoxMenuItem clrScaleItem = null;

    JPanel cntrlPanel;
    JPanel southPanel;
    JLabel forward;
    JLabel bakward;
    LineBorder blackBorder = new LineBorder(Color.black);
    JPanel cntrlPanelLeft;

    private JPanel outerPanel;
    private JMenuBar menuBar;

    static private ArrayList<ImageDisplay> imageDisplayList = new ArrayList<>();

    public boolean isTarget = false;

    Color dfltBGColor;

    private final ArrayList<Depiction> listOfDepictions = new ArrayList<>();
    private final ArrayList<Depiction> listOfMaskDepictions = new ArrayList<>();

    private int windowNumber = 0;

    private boolean initialized;

    private int whichVisible = 0;

    boolean firstToggle = false;

    JPanel buttonPanel;

    InfoLabel infoLabel = new InfoLabel();

    public boolean onlyOverlayNoReplace = false;

    public boolean spectraToolEnabled = true;

    private MyColorScale clrScale = null;

    public ImageDisplay(MapProjection mapProj) throws VisADException, RemoteException {
        this(null, mapProj);
    }

    public ImageDisplay(DisplayableData imageDsp, MapProjection mapProj)
            throws VisADException, RemoteException {
        this(imageDsp, mapProj, true);
    }

    public ImageDisplay(DisplayableData imageDsp, MapProjection mapProj, int windowNumber, DatasetInfo dsInfo)
            throws VisADException, RemoteException {
        this(imageDsp, mapProj, windowNumber, true, dsInfo);
    }

    public ImageDisplay(DisplayableData imageDsp, MapProjection mapProj, int windowNumber, DatasetInfo dsInfo, boolean spectraToolEnabled)
            throws VisADException, RemoteException {
        this(imageDsp, mapProj, windowNumber, true, dsInfo, spectraToolEnabled);
    }

    public ImageDisplay(DisplayableData imageDsp, MapProjection mapProj, int windowNumber)
            throws VisADException, RemoteException {
        this(imageDsp, mapProj, windowNumber, true);
    }

    public ImageDisplay(DisplayableData imageDsp, MapProjection mapProj, boolean createFrame)
            throws VisADException, RemoteException {
        this(imageDsp, mapProj, 0, createFrame);
    }

    public ImageDisplay(DisplayableData imageDsp, MapProjection mapProj, boolean createFrame, boolean spectraToolEnabled)
            throws VisADException, RemoteException {
        this(imageDsp, mapProj, 0, createFrame, spectraToolEnabled);
    }

    public ImageDisplay(DisplayableData imageDsp, MapProjection mapProj, boolean createFrame, DatasetInfo dsInfo, boolean spectraToolEnabled)
            throws VisADException, RemoteException {
        this(imageDsp, mapProj, 0, createFrame, dsInfo, spectraToolEnabled);
    }

    public ImageDisplay(DisplayableData imageDsp, MapProjection mapProj, int windowNumber, boolean createFrame)
            throws VisADException, RemoteException {
        this(imageDsp, mapProj, windowNumber, createFrame, null);
    }

    public ImageDisplay(DisplayableData imageDsp, MapProjection mapProj, int windowNumber, boolean createFrame, boolean spectraToolEnabled)
            throws VisADException, RemoteException {
        this(imageDsp, mapProj, windowNumber, createFrame, null, spectraToolEnabled);
    }

    public ImageDisplay(DisplayableData imageDsp, MapProjection mapProj, int windowNumber, boolean createFrame, DatasetInfo dsInfo)
            throws VisADException, RemoteException {
        this(imageDsp, mapProj, windowNumber, createFrame, dsInfo, true);
    }

    // TODO: description should be folded into Displayable
    public ImageDisplay(DisplayableData imageDsp, MapProjection mapProj, int windowNumber, boolean createFrame, DatasetInfo dsInfo, boolean spectraToolEnabled)
            throws VisADException, RemoteException {
        super();

        this.initialized = false;
        this.mapProjection = mapProj;
        this.lastMapProjection = mapProj;
        this.imageDsp = imageDsp;
        this.windowNumber = windowNumber;
        this.spectraToolEnabled = spectraToolEnabled;

        if (createFrame) {
            share = true;
            shareList.add(this);
        } else {
            //share = false;
            share = true;
            shareList.add(this);
            shareSize = false;
        }


        mapProjDsp = new MapProjectionDisplayJ3D(MapProjectionDisplay.MODE_2Din3D);
        mapProjDsp.enableRubberBanding(false);
        dspMaster = mapProjDsp;
        LocalDisplay dsp = dspMaster.getDisplay();
        // Turn off depth buffer (no depth testing) for 2Din3D and using RenderOrderPriority
        GraphicsModeControlJ3D mode = (GraphicsModeControlJ3D) dsp.getGraphicsModeControl();
        mode.setDepthBufferEnable(false, false);
        dspRenderer = (DisplayRendererJ3D) dsp.getDisplayRenderer();
        mouseBehav = dspMaster.getMouseBehavior();
        pCntrl = dsp.getProjectionControl();
        double[] tmp = pCntrl.getMatrix();
        System.arraycopy(tmp, 0, currentMatrix, 0, tmp.length);
        double[] rot_a = new double[3];
        double[] scale_a = new double[3];
        double[] trans_a = new double[3];
        mouseBehav.instance_unmake_matrix(rot_a, scale_a, trans_a, currentMatrix);
        double scale = scale_a[0];
        initScale = scale;

        pCntrl.addControlListener(this);

        dspMaster.setMouseFunctions(Hydra.getMouseFunctionMap());

        boolean addProbe = true;
        imgCntrl = null;

        //- make simple color range control.  FIXME: should wait on computeRange so we have to do this before next step.
        if (imageDsp instanceof HydraRGBDisplayable) {
            clrCntrl = new ColorControl((HydraRGBDisplayable) imageDsp);
            imgCntrl = clrCntrl;
        } else if (imageDsp instanceof ImageRGBDisplayable) {
            RGBCompositeControl rgbCntrl = new RGBCompositeControl(dspMaster, (ImageRGBDisplayable) imageDsp);
            imgCntrl = rgbCntrl;
        }

        imageDepiction = new Depiction(dspMaster, imageDsp, imgCntrl, dsInfo, false);
        imageDepiction.setPropertyChangeListener(this);
        listOfDepictions.add(imageDepiction);

        buildUI(createFrame);
        if (dsInfo != null) {
            updateInfoLabel(dsInfo.datSrcInfo.description, dsInfo.datSrcInfo.dateTimeStamp);
        }

        //- once at the beginning to initialize DisplayMaster
        dspMaster.draw();
        dspMaster.setDisplayInactive();

        mapProjDsp.setMapProjection(mapProj);

        if (share && (shareList.size() > 1)) {
            setProjectionMatrix(shareList.get(0).getProjectionMatrix());
            scale = shareList.get(0).dspMaster.getScale();
        }

        dspMaster.addDisplayable(imageDsp);

        mapBoundaryList = Hydra.addBaseMapVHRES(mapProjDsp);

        FlatField image = (FlatField) imageDsp.getData();
        //- add probe
        if (addProbe) {
            probe = new ReadoutProbe(image, dspMaster, imgCntrl.getDataRange());
            probe.setInfoLabel(infoLabel);
            probe.doMakeProbe(Color.red, dspMaster, baseScale, scale);
            readoutProbeList.add(probe);

            EarthLocationTuple[] earthLocs = new EarthLocationTuple[2];
            float[] minmax = Hydra.minmax(image, earthLocs);
            if (imageDsp instanceof HydraRGBDisplayable) {
                ((HydraRGBDisplayable) imageDsp).minLoc = earthLocs[0];
                ((HydraRGBDisplayable) imageDsp).maxLoc = earthLocs[1];
            } else if (imageDsp instanceof ImageRGBDisplayable) {
                ((ImageRGBDisplayable) imageDsp).minLoc = earthLocs[0];
                ((ImageRGBDisplayable) imageDsp).maxLoc = earthLocs[1];
            }

            minMarker = new ReadoutProbe(image, dspMaster, new double[][]{{(double) minmax[0], (double) minmax[1]}}, false);
            minMarker.setPosition(earthLocs[0]);
            minMarker.doMakeProbe(Color.red, dspMaster, baseScale, scale, false, "CROSS", Hydra.getTextSizeFactor() * 0.8f, Hydra.getTextSizeFactor() * 1.4f, Color.green);
            minMarker.setFixed();

            maxMarker = new ReadoutProbe(image, dspMaster, new double[][]{{(double) minmax[0], (double) minmax[1]}}, false);
            maxMarker.setPosition(earthLocs[1]);
            maxMarker.doMakeProbe(Color.red, dspMaster, baseScale, scale, false, "CROSS", Hydra.getTextSizeFactor() * 0.8f, Hydra.getTextSizeFactor() * 1.4f, Color.yellow);
            maxMarker.setFixed();
        }
        dspMaster.setDisplayActive();

        imageDisplayList.add(this);
    }

    public void buildUI(boolean createFrame) {
        if (SwingUtilities.isEventDispatchThread()) {
            outerPanel = (JPanel) buildComponent();
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        outerPanel = (JPanel) buildComponent();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        menuBar = buildMenuBar();
        dfltBGColor = menuBar.getBackground();

        Point loc = DataBrowser.getInstance().getLocation();
        loc = getDefaultLocation(loc.x, loc.y);

        if (createFrame) {
            String title = "Window " + windowNumber;
            frame = Hydra.createAndShowFrame(title, outerPanel, menuBar, HydraDisplay.sharedWindowSize, loc);
            frame.addWindowListener(this);
            frame.addComponentListener(this);
            numImageWindowsCreated++;
            imgCntrl.setFrame(frame);
        }
    }

    public JComponent buildComponent() {
        ImageIcon imgIc = new ImageIcon(getClass().getResource("/resources/icons/house.png"));

        JButton resetButton = new JButton(imgIc);
        resetButton.setToolTipText("Reset Display scale/translation");
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    mapProjDsp.resetScaleTranslate();
                } catch (VisADException ve) {
                    ve.printStackTrace();
                } catch (RemoteException re) {
                    re.printStackTrace();
                }
            }
        });

        linkButton.setToolTipText("add to set of displays sharing zoom/translate");
        if (share) { // if initialize to false, don't allow
            linkButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (share) {
                        setDisplayLinking(false);
                    } else {
                        setDisplayLinking(true);
                    }
                }
            });
        }

        southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));

        JPanel panelA = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = infoLabel.getJLabel();
        label.setBackground(Color.black);
        label.setForeground(Color.white);
        panelA.setBackground(Color.black);
        panelA.add(label);

        JPanel panelB = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(resetButton);
        buttonPanel.add(linkButton);

        cntrlPanel = new JPanel(new FlowLayout());
        cntrlPanel.setComponentOrientation(java.awt.ComponentOrientation.RIGHT_TO_LEFT);

        cntrlPanelLeft = new JPanel(new FlowLayout());
        cntrlPanelLeft.setComponentOrientation(java.awt.ComponentOrientation.RIGHT_TO_LEFT);

        forward = new JLabel();
        forward.setIcon(new ImageIcon(getClass().getResource("/resources/icons/left.png")));
        forward.setBorder(blackBorder);
        forward.addMouseListener(new java.awt.event.MouseAdapter() {
                                     public void mouseClicked(java.awt.event.MouseEvent e) {
                                         int numDepicts = listOfDepictions.size();

                                         if (whichVisible == -1) return;
                                         if (numDepicts == 1) return;

                                         whichVisible -= 1;
                                         if (whichVisible < 0) whichVisible = numDepicts + (whichVisible % numDepicts);

                                         while (!(listOfDepictions.get(whichVisible).getVisibleOK())) {
                                             whichVisible -= 1;
                                             if (whichVisible < 0) whichVisible = numDepicts + (whichVisible % numDepicts);
                                         }

                                         updateDepictionVisibility();
                                         updateForVisibilityChange(whichVisible);
                                     }
                                 }
        );

        bakward = new JLabel();
        bakward.setIcon(new ImageIcon(getClass().getResource("/resources/icons/right.png")));
        bakward.setBorder(blackBorder);
        bakward.addMouseListener(new java.awt.event.MouseAdapter() {
                                     public void mouseClicked(java.awt.event.MouseEvent e) {
                                         if (whichVisible == -1) return;
                                         int numDepicts = listOfDepictions.size();

                                         if (whichVisible == -1) return;
                                         if (numDepicts == 1) return;

                                         whichVisible += 1;
                                         if (whichVisible > (numDepicts - 1)) whichVisible = (whichVisible % numDepicts);

                                         while (!(listOfDepictions.get(whichVisible).getVisibleOK())) {
                                             whichVisible += 1;
                                             if (whichVisible > (numDepicts - 1)) whichVisible = (whichVisible % numDepicts);
                                         }

                                         updateDepictionVisibility();
                                         updateForVisibilityChange(whichVisible);
                                     }
                                 }
        );

        cntrlPanel.add(imageDepiction.doMakeComponent());
        imageDepiction.setParentComponent(cntrlPanel);

        panelB.add(buttonPanel);
        panelB.add(javax.swing.Box.createHorizontalGlue());
        panelB.add(cntrlPanelLeft);
        panelB.add(cntrlPanel);

        if (panelA != null) {
            southPanel.add(panelA);
        }
        southPanel.add(panelB);

        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BorderLayout());
        outerPanel.add(dspMaster.getDisplayComponent(), BorderLayout.CENTER);
        outerPanel.add(southPanel, BorderLayout.SOUTH);

        return outerPanel;
    }

    public JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        menuBar.add(toolsMenu);

        JMenuItem transectMenu = new JMenuItem("Transect");
        transectMenu.setActionCommand("doTransect");
        transectMenu.addActionListener(this);
        toolsMenu.add(transectMenu);

        JMenuItem scatterMenu = new JMenuItem("Scatter");
        scatterMenu.setActionCommand("doScatter");
        scatterMenu.addActionListener(this);
        scatterMenu.setToolTipText("this to X-Axis, next to Y-Axis");
        toolsMenu.add(scatterMenu);

        if (spectraToolEnabled) {
            JMenuItem profileMenu = new JMenuItem("Spectra");
            profileMenu.setActionCommand("doSpectra");
            profileMenu.addActionListener(this);
            toolsMenu.add(profileMenu);
        }

        JMenu captureMenu = new JMenu("Capture");
        captureMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        JMenuItem jpegItem = new JMenuItem("JPEG");
        jpegItem.addActionListener(this);
        jpegItem.setActionCommand("captureToJPEG");
        captureMenu.add(jpegItem);

        JMenuItem pngItem = new JMenuItem("PNG");
        jpegItem.addActionListener(this);
        jpegItem.setActionCommand("captureToPNG");
        captureMenu.add(pngItem);

        JMenuItem kmlItem = new JMenuItem("KML");
        kmlItem.addActionListener(this);
        kmlItem.setActionCommand("captureToKML");
        captureMenu.add(kmlItem);

        JMenuItem ncdfItem = new JMenuItem("NCDF");
        ncdfItem.addActionListener(this);
        ncdfItem.setActionCommand("saveToNCDF");
        captureMenu.add(ncdfItem);

        toolsMenu.add(captureMenu);

        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.getPopupMenu().setLightWeightPopupEnabled(false);
        maplines = new JCheckBoxMenuItem("coastlines", true);
        maplines.addActionListener(this);
        maplines.setActionCommand("coastlines");
        settingsMenu.add(maplines);

        minmaxItem = new JCheckBoxMenuItem("min/max", false);
        minmaxItem.addActionListener(this);
        minmaxItem.setActionCommand("minmax");
        settingsMenu.add(minmaxItem);

        probeItem = new JCheckBoxMenuItem("probe readout", true);
        probeItem.addActionListener(this);
        probeItem.setActionCommand("probe");
        settingsMenu.add(probeItem);

        clrScaleItem = new JCheckBoxMenuItem("color scale", false);
        clrScaleItem.addActionListener(this);
        clrScaleItem.setActionCommand("colorScale");
        settingsMenu.add(clrScaleItem);

        menuBar.add(settingsMenu);

        return menuBar;
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }

    public JPanel getControlPanel() {
        return cntrlPanel;
    }

    public JPanel getButtonPanel() {
        return buttonPanel;
    }

    public JComponent getComponent() {
        return outerPanel;
    }

    public void setParentFrame(JFrame frame) {
        this.frame = frame;
    }

    public void addOverlayImage(DisplayableData imageDsp, DatasetInfo dsInfo) throws VisADException, RemoteException {
        addOverlayImage(imageDsp, dsInfo, false);
    }

    public void addOverlayImage(DisplayableData imageDsp, DatasetInfo dsInfo, boolean asMask) throws VisADException, RemoteException {
        addOverlayImage(imageDsp, null, dsInfo, true, asMask);
    }

    public void addOverlayImage(DisplayableData imageDsp, DatasetInfo dsInfo, boolean isRemovable, boolean asMask) throws VisADException, RemoteException {
        addOverlayImage(imageDsp, null, dsInfo, isRemovable, asMask);
    }

    public void addOverlayImage(DisplayableData imageDsp, Depiction imgDepiction, boolean asMask) throws VisADException, RemoteException {
        addOverlayImage(imageDsp, imgDepiction, null, true, asMask);
    }

    public void addOverlayImage(DisplayableData imageDsp, Depiction imgDepiction, DatasetInfo dsInfo, boolean isRemovable, boolean asMask) throws VisADException, RemoteException {

        ((DisplayImpl) dspMaster.getDisplay()).setOnlyTransformForThis(true);
        dspMaster.addDisplayable(imageDsp);

        if (imgDepiction == null) {
            DepictionControl imgCntrl = null;
            if (imageDsp instanceof HydraRGBDisplayable) {
                imgCntrl = new ColorControl((HydraRGBDisplayable) imageDsp);
                if (clrScale != null) {
                    ((ColorControl) imgCntrl).addPropertyChangeListener(clrScale);
                }
            } else if (imageDsp instanceof ImageRGBDisplayable) {
                imgCntrl = new RGBCompositeControl(dspMaster, (ImageRGBDisplayable) imageDsp);
            }
            imgDepiction = new Depiction(dspMaster, imageDsp, imgCntrl, dsInfo, isRemovable, asMask);
        }
        imgDepiction.setPropertyChangeListener(this);

        if (!asMask) {
            imgDepiction.setParentComponent(cntrlPanel);
        } else {
            imgDepiction.setParentComponent(cntrlPanelLeft);
        }

        if (!imgDepiction.isMask) {
            listOfDepictions.add(imgDepiction);
            whichVisible = listOfDepictions.size() - 1;
            for (int k = 0; k < listOfDepictions.size() - 1; k++) {
                (listOfDepictions.get(k)).setVisible(false);
            }
            setMinMaxMarkers(imageDsp);
            updateForVisibilityChange(whichVisible);
        } else {
            listOfMaskDepictions.add(imgDepiction);
        }

        final Depiction obj = imgDepiction;
        final int nDepicts = listOfDepictions.size();
        if (SwingUtilities.isEventDispatchThread()) {
            updateControlPanel(obj, nDepicts);
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        updateControlPanel(obj, nDepicts);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateControlPanel(Depiction obj, int nDepicts) {
        Dimension size = southPanel.getPreferredSize();
        if (!obj.isMask) {
            cntrlPanel.add(obj.doMakeComponent());
        } else {
            cntrlPanelLeft.add(obj.doMakeComponent());
        }
        if (nDepicts == 2) {
            cntrlPanel.add(forward, 0);
            cntrlPanel.add(bakward, 1);
        }
        if (frame != null) { // TODO: may not have a parent frame
            Dimension newSize = southPanel.getPreferredSize();
            Dimension frmSize = frame.getSize();
            if (newSize.getWidth() > size.getWidth()) {
                Insets insets = frame.getInsets();
                frame.setSize(new Dimension(frmSize.width + (newSize.width - size.width) +
                        insets.left + insets.right + 2, frmSize.height));
                HydraDisplay.sharedWindowSize = frame.getSize();
            }
            frame.validate();
        }
    }

    public Depiction getDepiction() {
        return imageDepiction;
    }

    public int getWindowNumber() {
        return windowNumber;
    }

    public void removeDisplayable(HydraRGBDisplayable imageDisplayable) {
        Depiction depictToRemove = null;
        Iterator<Depiction> iter = listOfDepictions.iterator();
        while (iter.hasNext()) {
            Depiction depiction = iter.next();
            if (depiction.getDisplayableData() == imageDisplayable) {
                depictToRemove = depiction;
                break;
            }
        }
        if (depictToRemove != null) {
            depictToRemove.remove();
        }
    }

    public void removeDepiction(Depiction depiction) {
        if (depiction.isMask) {
            listOfMaskDepictions.remove(depiction);
            return;
        }

        listOfDepictions.remove(depiction);
        int numDepicts = listOfDepictions.size();
        if (numDepicts == 1) {
            cntrlPanel.remove(forward);
            cntrlPanel.remove(bakward);
            frame.validate();
        }

        boolean anyVisibleOK = false;
        for (int k = 0; k < numDepicts; k++) {
            Depiction depict = listOfDepictions.get(k);
            if (depict.getVisibleOK()) anyVisibleOK = true;
        }
        if (!anyVisibleOK) return;

        whichVisible = getWhichVisibleOK(whichVisible);
        updateForVisibilityChange(whichVisible);
        updateDepictionVisibility();
    }

    public void setDisplayLinking(boolean yesno) {
        if (!yesno) {
            share = false;
            linkButton.setIcon(ulnkIc);
        } else {
            share = true;
            if (shareSize) {
                setSize(sharedWindowSize);
            }
            linkButton.setIcon(lnkIc);
        }
    }

    public void setMapProjection(MapProjection mapProj, boolean reset) {
        this.lastMapProjection = this.mapProjection;
        this.mapProjection = mapProj;
        try {
            if (reset) {
                this.mapProjDsp.setMapProjection(mapProj);
            } else {
                this.mapProjDsp.setMapProjection(mapProj, reset);
            }
            Hydra.updateBaseMapVHRES(mapBoundaryList, mapProj);
        } catch (VisADException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setMapProjection(MapProjection mapProj) {
        setMapProjection(mapProj, true);
    }

    public void setMapProjection() {
        this.lastMapProjection = this.mapProjection;
    }

    public void resetMapProjection() {
        if (!mapProjection.equals(lastMapProjection)) {
            try {
                this.mapProjDsp.setMapProjection(lastMapProjection);
            } catch (VisADException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mapProjection = lastMapProjection;
        }
    }

    public MapProjection getMapProjection() {
        return mapProjection;
    }

    public HydraRGBDisplayable getImageDisplayable() {
        return (HydraRGBDisplayable) listOfDepictions.get(whichVisible).getDisplayableData();
    }

    public DisplayableData getReplaceableImageDisplayable() {
        return imageDsp;
    }

    public String getName() {
        return listOfDepictions.get(whichVisible).getName();
    }

    public ReadoutProbe getReadoutProbe() {
        return probe;
    }

    public ReadoutProbe addReadoutProbe(Color color, double x, double y) throws VisADException, RemoteException, Exception {
        FlatField image = (FlatField) imageDsp.getData();
        ReadoutProbe probe = new ReadoutProbe(image, dspMaster, clrCntrl.getDataRange(), false);
        probe.setInitialPosition(x, y);
        double scale = shareList.get(0).dspMaster.getScale();
        probe.doMakeProbe(color, dspMaster, baseScale, scale);
        readoutProbeList.add(probe);
        return probe;
    }

    public void updateImageData(FlatField image) {
        try {
            getImageDisplayable().getColorMap().resetAutoScale();
            dspMaster.reScale();

            imageDsp.setData(image);
            if (getWhichVisible() == 0) {
                for (int k = 0; k < readoutProbeList.size(); k++) {
                    readoutProbeList.get(k).updateData(image);
                }
                setMinMaxMarkers(imageDsp);
                updateMinMaxMarkers(imageDsp);
            }

            if (transect != null) {
                transect.updateData(image);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateImageRGBCompositeData(FlatField image, MapProjection mapProj, String imgName, DatasetInfo dsInfo) {
        try {
            ((ImageRGBDisplayable) imageDsp).setColorTupleType(
                    (RealTupleType) ((FunctionType) image.getType()).getRange());

            if (!getMapProjection().equals(mapProj)) {
                setMapProjection(mapProj);
            }
            imgCntrl.reset();
            imageDsp.setData(image);

            if (getWhichVisible() == 0) {
                updateInfoLabel(dsInfo);
                for (int k = 0; k < readoutProbeList.size(); k++) {
                    readoutProbeList.get(k).updateData(image);
                }
                setMinMaxMarkers(imageDsp);
                updateMinMaxMarkers(imageDsp);
            }

            imageDepiction.setName(dsInfo.name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateImageData(FlatField image, ColorTable clrTbl, MapProjection mapProj, DatasetInfo dsInfo) {
        try {

            if (!getMapProjection().equals(mapProj)) {
                setMapProjection(mapProj);
                for (int k = 0; k < readoutProbeList.size(); k++) {
                    readoutProbeList.get(k).projectionChanged();
                }
            }

            RealType newType = (RealType) ((FunctionType) image.getType()).getRange();
            if (!getImageDisplayable().getRGBRealType().equals(newType)) {
                image = Hydra.makeFlatFieldWithUniqueRange(image);
                RealType rtype = (RealType) ((FunctionType) image.getType()).getRange();

                ((HydraRGBDisplayable) imageDsp).setRGBRealType(rtype, clrTbl);
                clrCntrl.setColorTableNoChange(clrTbl);
                clrCntrl.reset();
            } else {
                ((HydraRGBDisplayable) imageDsp).getColorMap().resetAutoScale();
                dspMaster.reScale();
            }

            imageDsp.setData(image);

            if (whichVisible == 0) {
                updateInfoLabel(dsInfo);
                for (int k = 0; k < readoutProbeList.size(); k++) {
                    readoutProbeList.get(k).updateData(image);
                }
                setMinMaxMarkers(imageDsp);
                updateMinMaxMarkers(imageDsp);
            }

            if (transect != null) {
                transect.updateData(image);
            }

            imageDepiction.setName(dsInfo.name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateImageData(HydraRGBDisplayable imgDsp, FlatField image, ColorTable clrTbl, MapProjection mapProj, DatasetInfo dsInfo) {
        try {
            Depiction depiction = null;
            Iterator<Depiction> iter = listOfDepictions.iterator();
            while (iter.hasNext()) {
                depiction = iter.next();
                if (depiction.getDisplayableData() == imgDsp) {
                    break;
                }
            }
            if (depiction == null) {
                return;
            }

            if (!getMapProjection().equals(mapProj)) {
                setMapProjection(mapProj);
                for (int k = 0; k < readoutProbeList.size(); k++) {
                    readoutProbeList.get(k).projectionChanged();
                }
            }

            RealType newType = (RealType) ((FunctionType) image.getType()).getRange();
            if (!getImageDisplayable().getRGBRealType().equals(newType)) {
                image = Hydra.makeFlatFieldWithUniqueRange(image);
                RealType rtype = (RealType) ((FunctionType) image.getType()).getRange();

                imgDsp.setRGBRealType(rtype, clrTbl);
                clrCntrl.setColorTableNoChange(clrTbl);
                clrCntrl.reset();
            } else {
                imgDsp.getColorMap().resetAutoScale();
                dspMaster.reScale();
            }

            imgDsp.setData(image);

            if (depiction.isVisible) {
                updateInfoLabel(dsInfo);
                for (int k = 0; k < readoutProbeList.size(); k++) {
                    readoutProbeList.get(k).updateData(image);
                }
                setMinMaxMarkers(imgDsp);
                updateMinMaxMarkers(imgDsp);
            }

            if (transect != null) {
                transect.updateData(image);
            }


            depiction.setName(dsInfo.name);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toggleMapBoundaries(boolean on) {
        for (int k = 0; k < mapBoundaryList.size(); k++) {
            MapLines boundary = mapBoundaryList.get(k);
            if (!(on && boundary.getVisible())) {
                try {
                    boundary.setVisible(on);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (VisADException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void toggleMinMax(boolean on) {
        if (!(on && minMarker.getVisible())) {
            // TODO: Fix this hack!
            if (on && !firstToggle) {
                double[] s_a = getScale(dspMaster);
                try {
                    minMarker.resize(baseScale, s_a[0]);
                    maxMarker.resize(baseScale, s_a[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                firstToggle = true;
            }
            //-----
            minMarker.setVisible(on);
            maxMarker.setVisible(on);
        }
        if (on) {
            Depiction depiction = listOfDepictions.get(getWhichVisible());
            DisplayableData imageDsp = depiction.getDisplayableData();
            try {
                updateMinMaxMarkers(imageDsp);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void setMinMaxMarkers(DisplayableData imageDsp) throws VisADException, RemoteException {
        FlatField image = (FlatField) imageDsp.getData();
        EarthLocationTuple[] locs = getMinMaxLocation(image);

        if (imageDsp instanceof HydraRGBDisplayable) {
            ((HydraRGBDisplayable) imageDsp).minLoc = locs[0];
            ((HydraRGBDisplayable) imageDsp).maxLoc = locs[1];
        } else if (imageDsp instanceof ImageRGBDisplayable) {
            ((ImageRGBDisplayable) imageDsp).minLoc = locs[0];
            ((ImageRGBDisplayable) imageDsp).maxLoc = locs[1];
        }
    }

    public void updateMinMaxMarkers(DisplayableData imageDsp) throws Exception {
        if (!minMarker.getVisible()) {
            return;
        }
        EarthLocationTuple minLoc = null;
        EarthLocationTuple maxLoc = null;
        if (imageDsp instanceof HydraRGBDisplayable) {
            minLoc = ((HydraRGBDisplayable) imageDsp).minLoc;
            maxLoc = ((HydraRGBDisplayable) imageDsp).maxLoc;
        } else if (imageDsp instanceof ImageRGBDisplayable) {
            minLoc = ((ImageRGBDisplayable) imageDsp).minLoc;
            maxLoc = ((ImageRGBDisplayable) imageDsp).maxLoc;
        }

        FlatField image = (FlatField) imageDsp.getData();
        minMarker.updateData(image, minLoc);
        maxMarker.updateData(image, maxLoc);
    }

    public void updateInfoLabel(String sourceDescription, String dateTimeStr) {
        infoLabel.setDescAndDateTime(sourceDescription, dateTimeStr);
    }

    public void updateInfoLabel(DatasetInfo info) {
        DataSourceInfo datSrcInfo = info.datSrcInfo;
        infoLabel.setDescAndDateTime(datSrcInfo.description, datSrcInfo.dateTimeStamp);
    }

    public EarthLocationTuple[] getMinMaxLocation(FlatField image) throws VisADException, RemoteException {
        EarthLocationTuple[] locs = new EarthLocationTuple[2];
        float[] minmax = Hydra.minmax(image, locs);
        return locs;
    }

    public void toggleProbeReadout(boolean on) {
        if (!(on && probe.getReadoutVisible())) {
            for (int k = 0; k < readoutProbeList.size(); k++) {
                readoutProbeList.get(k).setReadoutVisible(on);
            }
        }
        if (on) { // was off, so update for the current visible
            Depiction depiction = listOfDepictions.get(getWhichVisible());
            DisplayableData imageDsp = depiction.getDisplayableData();
            try {
                FlatField ff = (FlatField) depiction.getDisplayableData().getData();
                probe.updateData(ff);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void removeMapBoundaries() {
        for (int k = 0; k < mapBoundaryList.size(); k++) {
            MapLines boundary = mapBoundaryList.get(k);
            try {
                dspMaster.removeDisplayable(boundary);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (VisADException e) {
                e.printStackTrace();
            }
        }
    }

    public void addMapBoundaries() {
        for (int k = 0; k < mapBoundaryList.size(); k++) {
            MapLines boundary = mapBoundaryList.get(k);
            try {
                dspMaster.addDisplayable(boundary);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (VisADException e) {
                e.printStackTrace();
            }
        }
    }


    public void setProbeVisible(boolean yesno) {
        probe.setVisible(yesno);
    }

    public CoordinateSystem getDisplayCoordinateSystem() {
        return mapProjDsp.getDisplayCoordinateSystem();
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("doTransect")) {
            try {
                if (transect == null) { // only one transect per image display
                    transect = new Transect(this, listOfDepictions.get(getWhichVisible()).getDisplayableData());
                }
            } catch (VisADException exc) {
                exc.printStackTrace();
            } catch (RemoteException exc) {
                exc.printStackTrace();
            }
        }
        if (cmd.equals("doScatter")) {
            Scatter.makeScatterDisplay(this);
        }
        if (cmd.equals("doSpectra")) {
            int datSrcId = listOfDepictions.get(getWhichVisible()).getDatasetInfo().datSrcInfo.dataSourceId;
            String fldName = listOfDepictions.get(getWhichVisible()).getDatasetInfo().name;
            DataSource datSrc = Hydra.getDataSource(datSrcId);
            MultiSpectralData[] msd = datSrc.getMultiSpectralData();
            try {
                new SpectraDisplay(probe.getEarthLocationRef(), msd, getLocationOnScreen(), getImageDisplayable(), fldName);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
        // TODO: combine jpeg,kml, others into a common widget
        if (cmd.equals("captureToJPEG")) {
            DisplayCapture.capture(frame, mapProjDsp, "jpeg");
        }
        if (cmd.equals("captureToPNG")) {
            DisplayCapture.capture(frame, mapProjDsp, "png");
        }
        if (cmd.equals("captureToKML")) {
            final MapProjection lastMapProj = getMapProjection();
            try {
                new DisplayCapture(frame, this);
            } catch (VisADException exc) {
                exc.printStackTrace();
            } catch (RemoteException exc) {
                exc.printStackTrace();
            }
        }
        if (cmd.equals("saveToNCDF")) {
            try {
                Hydra.writeImage((FlatField) imageDsp.getData(), imageDepiction.getName(), imageDepiction.getDatasetInfo().datSrcInfo.dateTimeStamp);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
        if (cmd.equals("coastlines")) {
            if (maplines.getState()) {
                toggleMapBoundaries(true);
            } else {
                toggleMapBoundaries(false);
            }
        }
        if (cmd.equals("minmax")) {
            if (minmaxItem.getState()) {
                toggleMinMax(true);
            } else {
                toggleMinMax(false);
            }
        }
        if (cmd.equals("probe")) {
            if (probeItem.getState()) {
                toggleProbeReadout(true);
            } else {
                toggleProbeReadout(false);
            }
        }
        if (cmd.equals("colorScale")) {
            if (clrScaleItem.getState()) {
                addColorScale();
            } else {
                removeColorScale();
            }
        }
    }

    public void controlChanged(ControlEvent e) {
        if (e.getControl() instanceof ProjectionControl) {
            double[] matrix = pCntrl.getMatrix();

            if (isMatrixEqual(matrix, currentMatrix)) {
                return;
            } else {
                System.arraycopy(matrix, 0, currentMatrix, 0, currentMatrix.length);

                for (int k = 0; k < shareList.size(); k++) {
                    ImageDisplay dsp = shareList.get(k);

                    if ((this.share) && (dsp.share) && (dsp != this)) { // if others want to share. Don't share with self
                        if (!isMatrixEqual(currentMatrix, dsp.getCurrentMatrix())) {
                            dsp.setProjectionMatrix(currentMatrix);
                        }
                    }
                }
            }
        }
    }

    public void setProjectionMatrix(double[] matrix) {
        try {
            System.arraycopy(matrix, 0, currentMatrix, 0, currentMatrix.length);
            pCntrl.setMatrix(currentMatrix);
        } catch (VisADException e) {
            // TODO handle properly
        } catch (RemoteException e) {
            // TODO handle properly
        }
    }

    public boolean isMatrixEqual(double[] matrixA, double[] matrixB) {
        for (int i = 0; i < matrixA.length; i++) {
            if (!Util.isApproximatelyEqual(matrixA[i], matrixB[i])) {
                return false;
            }
        }
        return true;
    }

    public DisplayMaster getDisplayMaster() {
        return dspMaster;
    }

    public double[] getProjectionMatrix() {
        return currentMatrix;
    }

    public void setCurrentMatrix(double[] matrix) {
        System.arraycopy(matrix, 0, currentMatrix, 0, matrix.length);
    }

    public double[] getCurrentMatrix() {
        return currentMatrix;
    }

    public static double[] getTranslation(DisplayMaster dspMaster) {
        double[] rot_a = new double[3];
        double[] scale_a = new double[3];
        double[] trans_a = new double[3];
        dspMaster.getMouseBehavior().instance_unmake_matrix(rot_a, scale_a, trans_a, dspMaster.getProjectionMatrix());
        return trans_a;
    }

    public static double[] getScale(DisplayMaster dspMaster) {
        double[] rot_a = new double[3];
        double[] scale_a = new double[3];
        double[] trans_a = new double[3];
        dspMaster.getMouseBehavior().instance_unmake_matrix(rot_a, scale_a, trans_a, dspMaster.getProjectionMatrix());
        return scale_a;
    }

    public void windowClosing(WindowEvent e) {
        if (transect != null) {
            transect.removeGraph(); //TODO: don't like this, formalize/consolidate 'remove'
        }
        shareList.remove(this);

        if (probe != null) {
            probe.destroy();
            minMarker.destroy();
            maxMarker.destroy();
        }
        readoutProbeList.remove(probe);
        readoutProbeList.clear();

        for (int k = 0; k < listOfDepictions.size(); k++) {
            listOfDepictions.get(k).setDialogVisible(false);
        }
        for (int k = 0; k < listOfMaskDepictions.size(); k++) {
            listOfMaskDepictions.get(k).setDialogVisible(false);
        }

        imageDisplayList.remove(this);
        DataBrowser.getInstance().windowRemoved(windowNumber);

        for (int k = 0; k < listOfDepictions.size(); k++) {
            listOfDepictions.get(k).destroy();
        }
        for (int k = 0; k < listOfMaskDepictions.size(); k++) {
            listOfMaskDepictions.get(k).destroy();
        }

        imgCntrl = null;
        clrCntrl = null;
        pCntrl.removeControlListener(this);

        listOfDepictions.clear();
        listOfMaskDepictions.clear();
        menuBar.removeAll();
        outerPanel.removeAll();

     /* Can cause a non-fatal Java3D Exception ?
     dspMaster.destroy();
     */
        dspRenderer.destroy(); // This too?

        dspMaster = null;
        imageDsp = null;
        probe = null;
        imageDepiction = null;
        mapBoundaryList = null;
        cntrlPanel = null;
        cntrlPanelLeft = null;


        frame.removeComponentListener(this);
        frame.removeWindowListener(this);
        frame.dispose();
        frame = null;
    }

    public Dimension getSize() {
        return frame.getSize();
    }

    public void setSize(Dimension size) {
        frame.setSize(size);
        frame.validate();
    }

    public void setLocation(int x, int y) {
        frame.setLocation(x, y);
    }

    public Point getDefaultLocation(int x, int y) {
        int off = (numImageWindowsCreated % 3) * 10;
        Point loc = new Point((x + 600) + off, y + off);
        return loc;
    }

    public Point getLocationOnScreen() {
        if (frame == null) {
            return null;
        } else {
            return frame.getLocationOnScreen();
        }
    }

    public void toFront() {
        frame.toFront();
    }

    public void setTitle(String title) {
        frame.setTitle(title);
    }

    public String getTitle() {
        return frame.getTitle();
    }

    public void setIsTarget(boolean isTarget) {
        this.isTarget = isTarget;
    }

    public void componentResized(ComponentEvent e) {
        super.componentResized(e);

        // Don't let user make window smaller than control panel size.
        Dimension size = southPanel.getPreferredSize();
        Dimension frmSize = frame.getSize();
        if (size.getWidth() > frmSize.getWidth()) {
            frame.setSize(new Dimension(size.width, frmSize.height));
        }

        for (int k = 0; k < shareList.size(); k++) {
            ImageDisplay dsp = shareList.get(k);
            if ((this.share) && (dsp.share) && (dsp != this) && (this.shareSize) && (dsp.shareSize) && gotFirstResizeEvent) {
                if (!dsp.getSize().equals(this.getSize())) {
                    dsp.setSize(this.getSize());
                }
            }
        }
        sharedWindowSize = this.getSize();
    }

    public void windowActivated(WindowEvent e) {
        if (initialized) { //TODO: not necessarily initialized, really just skipping the first event
            if (probe != null) {
                if (Float.isNaN(probe.getFloatValue())) {
                    probe.resetProbePosition();
                }
            }
        } else {
            initialized = true;
        }
    }

    public void depictionVisibilityChanged(boolean visibleOK, Depiction depiction) {
        if (depiction.isMask) {
            depiction.setVisible(visibleOK);
            return;
        }

        if (!visibleOK && depiction.getVisible()) {
            depiction.setVisible(false);
            findAndSetNextVisible(depiction);
        }
        if (visibleOK && !depiction.getVisible()) {
            if (listOfDepictions.size() == 1) {
                whichVisible = 0;
                updateDepictionVisibility();
                updateForVisibilityChange(whichVisible);
            }
        }
    }

    public int getWhichVisible() {
        int latest = -1;
        for (int k = 0; k < listOfDepictions.size(); k++) {
            Depiction depiction = listOfDepictions.get(k);
            if (depiction.getVisible()) latest = k;
        }
        return latest;
    }

    public int getWhichVisibleOK(int last) {
        int idx = 0;
        last = (last == 0) ? last : last - 1;
        for (int k = 0; k < listOfDepictions.size(); k++) {
            Depiction depiction = listOfDepictions.get(k);
            if (depiction.getVisibleOK() && (k <= last)) {
                idx = k;
            }
        }
        return idx;
    }

    public void updateForVisibilityChange() {
        whichVisible = getWhichVisible();
        updateForVisibilityChange(whichVisible);
    }

    public void updateForVisibilityChange(int whichVisible) {
        Depiction depiction = listOfDepictions.get(whichVisible);
        try {
            DisplayableData imageDsp = depiction.getDisplayableData();
            FlatField ff = (FlatField) depiction.getDisplayableData().getData();
            if (probe != null) {
                if (probe.getReadoutVisible()) {
                    for (int k = 0; k < readoutProbeList.size(); k++) {
                        readoutProbeList.get(k).updateData(ff);
                    }
                }
                updateMinMaxMarkers(imageDsp);
                updateInfoLabel(depiction.getDatasetInfo());
            }
            if (transect != null) {
                transect.updateData(ff);
            }
            DepictionControl cntrl = depiction.getDepictionControl();
            if (cntrl != null && cntrl instanceof ColorControl) {
                double[] rng = ((ColorControl) cntrl).getClrRange();
                float[][] tbl = ((ColorControl) cntrl).getColorTable();
                if (clrScale != null) {
                    clrScale.update(tbl);
                    clrScale.update(rng);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findAndSetNextVisible(Depiction depiction) {
        int numDepicts = listOfDepictions.size();
        boolean anyVisibleOK = false;
        for (int k = 0; k < numDepicts; k++) {
            Depiction depict = listOfDepictions.get(k);
            if (depict.getVisibleOK()) anyVisibleOK = true;
        }
        if (!anyVisibleOK) {
            depiction.setVisible(false);
            whichVisible = -1;
            return;
        }

        whichVisible -= 1;
        if (whichVisible < 0) whichVisible = numDepicts + (whichVisible % numDepicts);

        while (!(listOfDepictions.get(whichVisible).getVisibleOK())) {
            whichVisible -= 1;
            if (whichVisible < 0) whichVisible = numDepicts + (whichVisible % numDepicts);
        }

        updateDepictionVisibility();
        updateForVisibilityChange(whichVisible);
    }

    public void updateDepictionVisibility() {
        Depiction depict = listOfDepictions.get(whichVisible);
        if (depict.getVisibleOK()) depict.setVisible(true);
        for (int k = 0; k < listOfDepictions.size(); k++) {
            if (k != whichVisible) {
                depict = listOfDepictions.get(k);
                if (depict.getVisible()) {
                    depict.setVisible(false);
                }
            }
        }
    }

    public void setWhichVisible(Depiction depiction) {
        for (int k = 0; k < listOfDepictions.size(); k++) {
            if (listOfDepictions.get(k) == depiction) {
                whichVisible = k;
                break;
            }
        }
        updateDepictionVisibility();
        updateForVisibilityChange(whichVisible);
    }

    public static ImageDisplay getTarget() {
        for (int k = 0; k < imageDisplayList.size(); k++) {
            ImageDisplay iDsp = imageDisplayList.get(k);
            if (iDsp.isTarget) {
                return iDsp;
            }
        }
        return null;
    }

    void addColorScale() {
        Depiction depict = listOfDepictions.get(whichVisible);
        DepictionControl cntrl = depict.getDepictionControl();
        double[] clrRange = null;
        float[][] clrTable = null;
        if (cntrl instanceof ColorControl) {
            clrRange = ((ColorControl) cntrl).getClrRange();
            clrTable = ((ColorControl) cntrl).getColorTable();
        } else {
            return;
        }
        float low = (float) clrRange[0];
        float high = (float) clrRange[1];
        int num = (int) ((high - low) / 10);
        float incr = (float) num;

        try {
            clrScale = new MyColorScale(dspMaster, clrTable, low, high, incr);
            Iterator<Depiction> iter = listOfDepictions.iterator();
            while (iter.hasNext()) {
                cntrl = iter.next().getDepictionControl();
                if (cntrl instanceof ColorControl) {
                    ((ColorControl) cntrl).addPropertyChangeListener(clrScale);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
     
      
      /*
      ScreenAnnotatorJ3D screenAnnotator = new ScreenAnnotatorJ3D((DisplayImpl)dspMaster.getDisplay());
      screenAnnotator.clear();
      screenAnnotator.add(new TriangleJ3D(TriangleJ3D.FILL,
           0,0,60,0,0,60, new float[] {1f, 0f, 1f}, 1.0, 1.0));
      screenAnnotator.add(new LabelJ3D("HYDRA"));
      screenAnnotator.draw();
      screenAnnotator.makeVisible(true);
      */
    }

    void removeColorScale() {
        clrScale.remove();
        clrScale = null;
    }

    public static ArrayList<ImageDisplay> getImageDisplayList() {
        return imageDisplayList;
    }
}
