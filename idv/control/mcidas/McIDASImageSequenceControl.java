package ucar.unidata.idv.control.mcidas;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.Class;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.event.*;
import javax.swing.*;
import javax.swing.JCheckBox;

import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.imagery.mcidas.FrameDirtyInfo;
import ucar.unidata.data.imagery.mcidas.McIDASDataSource;
import ucar.unidata.data.imagery.mcidas.McIDASDataSource.FrameDataInfo;
import ucar.unidata.data.imagery.mcidas.McIDASFrame;
import ucar.unidata.data.imagery.mcidas.McIDASXFrameDescriptor;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.control.ImageSequenceControl;
import ucar.unidata.idv.control.WrapperWidget;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PollingInfo;

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
    private PollingInfo pollingInfo;

    /** Label for polling */
    protected static final String LABEL_POLL = "Polling: ";

    /** Label for polling interval */
    protected static final String LABEL_INTERVAL = "  Interval: ";

   /** Panel for polling dialog */
    JPanel refreshPanel = null;
    private JCheckBox activeWidget = null;
    private JTextField intervalWidget = null;

    /** Should we be polling */
    private boolean isActive = true;
    private long interval = 500;

    /**
     * Default ctor; sets the attribute flags
     */
    public McIDASImageSequenceControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DISPLAYUNIT);
        frameComponentInfo = initFrameComponentInfo();
        pollingInfo = initPollingInfo();
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

        final JTextField labelField = new JTextField("" , 20);

        ActionListener labelListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
              setNameFromUser(labelField.getText()); 
              updateLegendLabel();
            }
        };

        labelField.addActionListener(labelListener);
        JButton labelBtn = new JButton("Apply");
        labelBtn.addActionListener(labelListener);

        JPanel labelPanel =
            GuiUtils.hflow(Misc.newList(labelField, labelBtn),
                                         2, 0);

        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Label:"), labelPanel));

        List frameNumbers = new ArrayList();
        Integer frmI = new Integer(0);
        ControlContext controlContext = getControlContext();
        List dss = ((IntegratedDataViewer)controlContext).getDataSources();
        McIDASDataSource mds = null;
        for (int i=0; i<dss.size(); i++) {
          DataSourceImpl ds = (DataSourceImpl)dss.get(i);
          if (ds instanceof McIDASDataSource) {
             mds = (McIDASDataSource)ds;
             frameNumbers = mds.getFrameNumbers();
             ArrayList frmAL = (ArrayList)(frameNumbers.get(0));
             frmI = (Integer)(frmAL.get(0));
            break;
          }
        }

        if (frmI.intValue() < 0) {
            isActive = true;
            pollingInfo.setIsActive(isActive);
            JPanel pollingActiveBox = GuiUtils.hbox(GuiUtils.rLabel(LABEL_POLL),
                                      getActiveWidget(mds),GuiUtils.rLabel(LABEL_INTERVAL),
                                      getIntervalWidget(mds));
            refreshPanel = GuiUtils.top(GuiUtils.left(pollingActiveBox));

            controlWidgets.add(
                new WrapperWidget(
                this, GuiUtils.rLabel(""), refreshPanel));
        } else {
            isActive = false;
            pollingInfo.setIsActive(isActive);
        }
    }

    public JCheckBox getActiveWidget(McIDASDataSource mds) {
        final McIDASDataSource mdss = mds;
        activeWidget = new JCheckBox("Active", isActive);
        activeWidget.addItemListener(new ItemListener() {
           public void itemStateChanged(ItemEvent e) {
              if (pollingInfo.getIsActive() != isActive) {
                 pollingInfo.setIsActive(isActive);
              } else {
                 pollingInfo.setIsActive(!isActive);
              }
              if (pollingInfo.getIsActive()) {
                  mdss.startPolling();
              } else {
                  mdss.stopPolling();
              }
              getRequestProperties();
              try {
                resetData();
              } catch (Exception ex) {
                System.out.println(ex);
                System.out.println("polling active exception");
              }
           }
        });

        return activeWidget;
    }

    public JTextField getIntervalWidget(McIDASDataSource mds) {
        interval = pollingInfo.getInterval();
        intervalWidget = new JTextField("" + (interval / (double) 3600000.0), 5);
        ActionListener intervalListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
               String intervalString = (intervalWidget.getText()).trim();
               Integer intervalInt = new Integer(intervalString);
               interval = intervalInt.intValue();
               pollingInfo.setInterval(interval);
               getRequestProperties();
               try {
                 resetData();
               } catch (Exception ex) {
                 System.out.println(ex);
                 System.out.println("polling interval exception");
               }
            }
        };

        intervalWidget.enable(isActive);
        return intervalWidget;
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
     * Creates, if needed, and returns the pollingInfo member.
     *
     * @return The pollingInfo
     */
    private PollingInfo initPollingInfo() {
        if (pollingInfo == null) {
            pollingInfo = new PollingInfo(interval,isActive);
        }
        return pollingInfo;
    }


    /**
     * Retrieve polling information
     *
     * @return The pollingInfo
     */
    public PollingInfo getPollingInfo() {
        return pollingInfo;
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

        FrameDirtyInfo frameDirtyInfo = new FrameDirtyInfo(false,false,false);
        ControlContext controlContext = getControlContext();
        List dss = ((IntegratedDataViewer)controlContext).getDataSources();
        DataSourceImpl ds = null;
        for (int i=0; i<dss.size(); i++) {
          ds = (DataSourceImpl)dss.get(i);
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
}
