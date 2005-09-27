package ucar.unidata.idv.control.mcidas;



import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.event.*;

import ucar.unidata.data.DataCancelException;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.data.imagery.mcidas.FrameComponentInfo;
import ucar.unidata.data.imagery.mcidas.McIDASFrame;
import ucar.unidata.data.imagery.mcidas.McIDASConstants;

import ucar.unidata.idv.control.ImageSequenceControl;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Range;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;

import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.MapViewManager;


import ucar.unidata.util.Resource;
import ucar.unidata.util.ColorTable;

import ucar.visad.Util;
import ucar.visad.display.ImageSequenceDisplayable;
import ucar.visad.display.ColorScale;

import visad.*;
import visad.util.ColorPreview;
import visad.util.BaseRGBMap;

import visad.meteorology.ImageSequenceManager;
import visad.meteorology.ImageSequence;
import visad.meteorology.SingleBandedImageImpl;
import visad.RealTupleType;
import visad.RealType;
import visad.FunctionType;


/**
 * A DisplayControl for handling McIDAS-X image sequences
 */
public class McIDASImageSequenceControl extends ImageSequenceControl {

   private JCheckBox imageCbx;
   private JCheckBox graphicsCbx;
   private JCheckBox colorTableCbx;

    /** Holds frame component information for polling */
    private static FrameComponentInfo frameComponentInfo;

    /**
     * Default ctor; sets the attribute flags
     */
    public McIDASImageSequenceControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DISPLAYUNIT);
        frameComponentInfo = initFrameComponentInfo();
    }

    /**
     * Override the base class method that creates request properties
     * and add in the appropriate frame component request parameters.
     * @return  table of properties
     */
    protected Hashtable getRequestProperties() {
        Hashtable props = super.getRequestProperties();
        System.out.println("request properties = " + props);
        props.put(McIDASConstants.IMAGE, new Boolean(frameComponentInfo.getIsImage()));
        props.put(McIDASConstants.GRAPHICS, new Boolean(frameComponentInfo.getIsGraphics()));
        props.put(McIDASConstants.COLORTABLE, new Boolean(frameComponentInfo.getIsColorTable()));
        return props;
    }

    /**
     * Get control widgets specific to this control.
     *
     * @param controlWidgets   list of control widgets from other places
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void getControlWidgets(List controlWidgets)
        throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);
        controlWidgets.add(
            new McIDASWrapperWidget(
                this, GuiUtils.rLabel("Frame components:"),
                doMakeImageBox(),doMakeGraphicsBox(),doMakeColorTableBox()));
    }

    /**
     * Creates, if needed, and returns the frameComponentInfo member.
     *
     * @return The frameComponentInfo
     */
    private FrameComponentInfo initFrameComponentInfo() {
        if (frameComponentInfo == null) {
            frameComponentInfo = new FrameComponentInfo(true, true, true);
        }
        return frameComponentInfo;
    }

    /**
     * Make the frame component check boxes.
     * @return Check box for Graphics
     */
    protected Component doMakeImageBox()
        throws VisADException, RemoteException {

        imageCbx = new JCheckBox("Image",frameComponentInfo.getIsImage());

        final boolean isImage = imageCbx.isSelected();
        imageCbx.setToolTipText("Set to import image data");
        imageCbx.addItemListener(new ItemListener() {
           public void itemStateChanged(ItemEvent e) {
              if (frameComponentInfo.getIsImage() != isImage) {
                 frameComponentInfo.setIsImage(isImage);
              } else {
                 frameComponentInfo.setIsImage(!isImage);
              }
              getRequestProperties();
              try {
                System.out.println("resetting data");
                resetData();
              } catch (Exception ex) {
                System.out.println("image exception");
              }
           }
        });
        return imageCbx;
    }

    /**
     * Make the frame component check boxes.
     * @return Check box for Graphics
     */
    protected Component doMakeGraphicsBox() {
        graphicsCbx = new JCheckBox("Graphics", frameComponentInfo.getIsGraphics());

        final boolean isGraphics = graphicsCbx.isSelected();
        graphicsCbx.setToolTipText("Set to import graphics data");
        graphicsCbx.addItemListener(new ItemListener() {
           public void itemStateChanged(ItemEvent e) {
              if (frameComponentInfo.getIsGraphics() != isGraphics) {
                 frameComponentInfo.setIsGraphics(isGraphics);
              } else {
                 frameComponentInfo.setIsGraphics(!isGraphics);
              }
              try {
                resetData();
              } catch (Exception ex) {}
           }
        });
        return graphicsCbx;
    }

    /**
     * Make the frame component check boxes.
     * @return Check box for Graphics
     */
    protected Component doMakeColorTableBox() {
        colorTableCbx = new JCheckBox("ColorTable", frameComponentInfo.getIsColorTable());
        final boolean isColorTable = colorTableCbx.isSelected();
        colorTableCbx.setToolTipText("Set to import color table data");
        colorTableCbx.addItemListener(new ItemListener() {
           public void itemStateChanged(ItemEvent e) {
              if (frameComponentInfo.getIsColorTable() != isColorTable) {
                 frameComponentInfo.setIsColorTable(isColorTable);
              } else {
                 frameComponentInfo.setIsColorTable(!isColorTable);
              }
              try {
                resetData();
              } catch (Exception ex) {}
           }
        });
        return colorTableCbx;
    }

}
