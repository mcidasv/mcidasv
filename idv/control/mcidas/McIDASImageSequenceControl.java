package ucar.unidata.idv.control.mcidas;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.Class;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.event.*;
import javax.swing.JCheckBox;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSourceImpl;
//import ucar.unidata.data.imagery.mcidas.FrameComponentInfo;
import ucar.unidata.data.imagery.mcidas.FrameDirtyInfo;
//import ucar.unidata.data.imagery.mcidas.McIDASConstants;
import ucar.unidata.data.imagery.mcidas.McIDASDataSource;
import ucar.unidata.data.imagery.mcidas.McIDASDataSource.FrameDataInfo;
import ucar.unidata.data.imagery.mcidas.McIDASFrame;
import ucar.unidata.data.imagery.mcidas.McIDASXFrameDescriptor;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.control.ImageSequenceControl;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.GuiUtils;

import visad.*;
import visad.georef.MapProjection;


/**
 * A DisplayControl for handling McIDAS-X image sequences
 */
public class McIDASImageSequenceControl extends ImageSequenceControl {

   private JCheckBox imageCbx;
   private JCheckBox graphicsCbx;
   private JCheckBox colorTableCbx;

    /** Holds frame component information for polling */
    private FrameComponentInfo frameComponentInfo;

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
        props.put(McIDASComponents.IMAGE, new Boolean(frameComponentInfo.getIsImage()));
        props.put(McIDASComponents.GRAPHICS, new Boolean(frameComponentInfo.getIsGraphics()));
        props.put(McIDASComponents.COLORTABLE, new Boolean(frameComponentInfo.getIsColorTable()));
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
/*
        ControlContext controlContext = getControlContext();
        List dss = ((IntegratedDataViewer)controlContext).getDataSources();
        for (int i=0; i<dss.size(); i++) {
          DataSourceImpl ds = (DataSourceImpl)dss.get(i);
          if (ds instanceof McIDASDataSource) {
            frameComponentInfo = ((McIDASDataSource)ds).getFrameComponentInfo();
            break;
          }
        }
*/
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
                resetData();
              } catch (Exception ex) {
                System.out.println(ex);
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
              getRequestProperties();
              try {
                resetData();
              } catch (Exception ex) {
                System.out.println(ex);
                System.out.println("graphics exception");
              }
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
              getRequestProperties();
              try {
                resetData();
              } catch (Exception ex) {
                System.out.println(ex);
                System.out.println("colortable exception");
              }
           }
        });
        return colorTableCbx;
    }

    /**
     * This gets called when the control has received notification of a
     * dataChange event.
     * 
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    protected void resetData() throws VisADException, RemoteException {
        DataChoice dc = getDataChoice();

        FrameDirtyInfo frameDirtyInfo = new FrameDirtyInfo(false,false,false);
        ControlContext controlContext = getControlContext();
        List dss = ((IntegratedDataViewer)controlContext).getDataSources();
        for (int i=0; i<dss.size(); i++) {
          DataSourceImpl ds = (DataSourceImpl)dss.get(i);
          if (ds instanceof McIDASDataSource) {
            frameDirtyInfo = ((McIDASDataSource)ds).getFrameDirtyInfo();
            break;
          }
        }
        MapProjection saveMapProjection;
        if (frameDirtyInfo.dirtyImage) {
          saveMapProjection = null;
        } else {
          saveMapProjection = getMapViewProjection();
        }        
        super.resetData();
        if (saveMapProjection != null) {
          MapViewManager mvm = getMapViewManager();
          mvm.setMapProjection(saveMapProjection, false);
        }
    }


    /**
     * Return the label used for the menues in the IDV. Implements
     * the method in the {@link DisplayControl} interface
     *
     * @return The menu label
     */
    public String getMenuLabel() {
        String label;
        DataChoice dc = getDataChoice();
        List frames = new ArrayList();
        if (dc.getId().getClass() == frames.getClass()) {
           frames = (List)dc.getId();
           Integer frameInt = (Integer)frames.get(0);
           int frameFirst = frameInt.intValue();
           if (frameFirst < 0) {
              label = new String("McIDAS Current Frame");
           } else {
              label = new String("McIDAS Frame Sequence ");
              label = label.concat(frameInt.toString());
              if (frames.size() > 1) {
                 label = label.concat("-");
                 frameInt = (Integer)frames.get(frames.size()-1);
                 label = label.concat(frameInt.toString());
              }
           }
        } else {
           FrameDataInfo fdi = (FrameDataInfo)dc.getId();
           String labelFdi = fdi.toString();
           label = new String("McIDAS Frame ");
           label = label.concat(labelFdi.substring(labelFdi.indexOf(" ")+1, labelFdi.length()));
        }

        return label;
    }

}
