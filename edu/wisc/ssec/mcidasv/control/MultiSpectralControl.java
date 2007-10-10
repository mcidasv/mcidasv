package edu.wisc.ssec.mcidasv.control;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.DataSelection;

import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.DisplayControl;

import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.ColorTable;

import ucar.visad.display.DisplayableData;
import ucar.visad.display.Displayable;
import ucar.visad.display.Grid2DDisplayable;
import ucar.visad.display.RGBDisplayable;
import ucar.visad.display.XYDisplay;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.LineDrawing;

import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ViewDescriptor;

import visad.*;
import visad.VisADException;
import visad.RemoteVisADException;
import visad.ReferenceException;
import visad.bom.RubberBandBoxRendererJ3D;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;

import java.lang.String;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
//-import edu.wisc.ssec.mcidasv.data.hydra.MyRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionDataSource;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.control.HydraImageProbe;



/**
 * Class for controlling the display of images.
 * @author IDV Development Group
 * @version $Revision$
 */
public class MultiSpectralControl extends DisplayControlImpl {


   /** The spectrum view gui */
   private Container viewContents;


   private ViewManager spectrumView;

   /** foreground color */
   private Color foreground = Color.black;

   /** background color */
   private Color background = Color.white;

   /** spectrum display/data info  */
   private RealType spectrumDomainType;

   private RealType spectrumRangeType;

   private RealType imageRangeType;

   private DataReferenceImpl positionRef;

   private DataReferenceImpl positionRef_B;

   private DataChoice dataChoice;

   private DataReferenceImpl spectrumRef;

   private DataReferenceImpl spectrumRef_B;

   private DataReferenceImpl rubberBandRef;

   private MultiSpectralData multiSpecData;

   private LocalDisplay display;

   private ScalarMap xmap;

   private ScalarMap ymap;

   private float[] x_init_range = new float[2];

   private float[] y_init_range = new float[2];

   private DataReferenceImpl channelSelectRef;

   private DisplayableData imageDisplay;

   private Gridded1DSet spectrumDomain;

   private MultiDimensionDataSource dataSource;

   private DisplayMaster mainViewMaster;

   private float init_wavenumber;



    public MultiSpectralControl() {
       super();
       setAttributeFlags(FLAG_COLORTABLE | FLAG_SELECTRANGE | FLAG_ZPOSITION);
    }


    public boolean init(DataChoice dataChoice) throws VisADException, RemoteException {
      this.dataChoice = dataChoice;

      init_wavenumber = MultiSpectralData.init_wavenumber;

      dataSource = (MultiDimensionDataSource) ((DirectDataChoice)dataChoice).getDataSource();
      multiSpecData = dataSource.getMultiSpectralData();

      positionRef = new DataReferenceImpl("positionRef");
      positionRef_B = new DataReferenceImpl("positionRef_B");

      FlatField spectrum = null;
      try {
        spectrum = multiSpecData.getSpectrum(new int[] {1,1});
      } catch (Exception e) {
        System.out.println("problem initializing control");
      } 
      
      spectrumDomain = (Gridded1DSet) spectrum.getDomainSet();
      float[] lo = spectrumDomain.getLow();
      float[] hi = spectrumDomain.getHi();
      x_init_range[0] = lo[0];
      x_init_range[1] = hi[0];

      //y_init_range[0] = 0f;
      //y_init_range[1] = 100f;
      y_init_range[0] = 180f;
      y_init_range[1] = 320f;

      spectrumDomainType = (((FunctionType)spectrum.getType()).getDomain().getRealComponents())[0];
      spectrumRangeType = (((FunctionType)spectrum.getType()).getFlatRange().getRealComponents())[0];

      spectrumView = new ViewManager(getViewContext(),
                             new XYDisplay("Spectrum", spectrumDomainType, spectrumRangeType),
                             new ViewDescriptor("spectrum"), "showControlLegend=false;");

      DisplayMaster master = spectrumView.getMaster();
      ((XYDisplay)master).showAxisScales(true);
      ((XYDisplay)master).setAspect(2.5, 0.75);
      double[] proj = master.getProjectionMatrix();
      proj[0] = 0.35;
      proj[5] = 0.35;
      proj[10] = 0.35;
      master.setProjectionMatrix(proj);


      //Displayable spectrumDisplay = createSpectrumDisplay(spectrum);
      //master.addDisplayable(spectrumDisplay);
      //addDisplayable(spectrumDisplay, spectrumView);
      addViewManager(spectrumView);



      //- low level, eventually use higher-level idv classes.
      //- Note: some of the high-level classes didn't work.
      display = master.getDisplay();
      spectrumRef = new DataReferenceImpl("spectrum");
      spectrumRef_B = new DataReferenceImpl("spectrumB");
      xmap = new ScalarMap(spectrumDomainType, Display.XAxis);
      ymap = new ScalarMap(spectrumRangeType, Display.YAxis);
      xmap.setRange(x_init_range[0], x_init_range[1]);
      ymap.setRange(y_init_range[0], y_init_range[1]);
      display.addMap(xmap);
      display.addMap(ymap);

      display.addReference(spectrumRef_B,  new ConstantMap[] {new ConstantMap(1.0, Display.Red),
               new ConstantMap(0.0, Display.Green),new ConstantMap(0.0, Display.Blue)});
      display.addReference(spectrumRef);
      rubberBandRef = new DataReferenceImpl("rubber band");
      rubberBandRef.setData(new RealTuple(new RealTupleType(spectrumDomainType, spectrumRangeType),
                                                new double[] {Double.NaN,Double.NaN}));
      display.addReferences(new RubberBandBoxRendererJ3D(spectrumDomainType, spectrumRangeType, 2, 2),
                    new DataReference[] {rubberBandRef}, null);
     
      channelSelectRef = new DataReferenceImpl("line");
      channelSelectRef.setData(new Gridded2DSet(new RealTupleType(spectrumDomainType, spectrumRangeType),
                             new float[][] {{919.47f, 919.47f}, {y_init_range[0], y_init_range[1]}},2));
      display.addReference(channelSelectRef, new ConstantMap[] {new ConstantMap(0.0, Display.Red),
               new ConstantMap(1.0, Display.Green),new ConstantMap(0.0, Display.Blue)});


      FlatField image = null;
      try {
         image = multiSpecData.getImage((float)919.47, dataSource.defaultSubset);
      } catch (Exception e) {
        e.printStackTrace();
      }
      imageRangeType = (((FunctionType)image.getType()).getFlatRange().getRealComponents())[0];
      paramName = imageRangeType.getName();


      imageDisplay = createImageDisplay(image);
      ViewManager vm = getViewManager();
      mainViewMaster = vm.getMaster();
      addDisplayable(imageDisplay, FLAG_COLORTABLE | FLAG_SELECTRANGE | FLAG_ZPOSITION);
      master.addDisplayable(imageDisplay);
 
      new SpectrumUpdater(positionRef, spectrumRef);
      new SpectrumUpdater(positionRef_B, spectrumRef_B);
      new RubberBandListener();
      new ImageUpdater();

      return true;
    }

    public void initDone() {
      try { 
         createImageProbe();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.out.println(e.getMessage());
      }
    }

    private Displayable createSpectrumDisplay(Data spectrum) throws VisADException, RemoteException {
      //DisplayableData dspData = new DisplayableData("spectrum");
      //dspData.setData(spectrum);
      LineDrawing lineDsp = new LineDrawing("spectrum");
      /*
      DataReferenceImpl dataRef = new DataReferenceImpl("spectrum");
      dataRef.setData(spectrum);
      HydraDisplayable dspData = new HydraDisplayable(dataRef, null);
      ScalarMap smap = new ScalarMap(spectrumDomainType, Display.XAxis);
      //dspData.addScalarMap(smap);
      */
      //ScalarMap smap = new ScalarMap(spectrumRangeType, Display.RGB);
      //dspData.addScalarMap(smap);
      lineDsp.setData(spectrum);
      return lineDsp;
   }

   private DisplayableData createImageDisplay(FlatField image) throws VisADException, RemoteException {
     HydraRGBDisplayable imageDsp = new HydraRGBDisplayable("image", imageRangeType, null, true);
     //MyRGBDisplayable imageDsp = new MyRGBDisplayable("image", imageRangeType, null, true);
     imageDsp.setData(image);
     return imageDsp;
   }

   private void createImageProbe() throws VisADException, RemoteException {
     IntegratedDataViewer idv = getIdv();

     DisplayControl control = idv.doMakeControl(Misc.newList(dataChoice),
                   idv.getControlDescriptor("hydra.probe"), (String)null, null, false);
        ((HydraImageProbe)control).doMakeProbe();
        ((HydraImageProbe)control).setColor(Color.red);
        ((HydraImageProbe)control).setPositionRef(positionRef_B);

     control = idv.doMakeControl(Misc.newList(dataChoice),
                   idv.getControlDescriptor("hydra.probe"), (String)null, null, false);
        ((HydraImageProbe)control).doMakeProbe();
        ((HydraImageProbe)control).setColor(Color.cyan);
        ((HydraImageProbe)control).setPositionRef(positionRef);
   }


    /**
     * Get the initial color table for the data
     *
     * @return  intitial color table
     */
    protected ColorTable getInitialColorTable() {
/*
        DisplayConventions dc = getDisplayConventions();
        List colorNames = dc.getColorNameList();
        ColorTable colorTable = super.getInitialColorTable();
        if (colorTable.getName().equalsIgnoreCase("default")) {
            colorTable = dc.getParamColorTable("image");
        }
*/
        //return getDisplayConventions().getParamColorTable("Radiance");
        return getDisplayConventions().getParamColorTable("BrightnessTemp");
    }



    /**
     * Get the initial range for the data and color table.
     * @return  initial range
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Range getInitialRange() throws RemoteException, VisADException {
        Range range = getDisplayConventions().getParamRange(paramName, null);
                          //getDisplayUnit());
        //range = getDisplayConventions().getParamRange("Radiance", null);
        range = getDisplayConventions().getParamRange("BrightnessTemp", null);
        //Don't do this for now
        /**
        if (range == null) {
            range = getRangeFromColorTable();
            if ((range != null) && (range.getMin() == range.getMax())) {
                range = null;
            }
        }

        if (range == null) {
            range = getDisplayConventions().getParamRange("image",
                    getDisplayUnit());
        }
        if (range == null) {
            return new Range(0, 255);
        }
        **/
        //range = new Range((double)0.0, (double)0.99);
        return range;
    }

    /**
     * Called by doMakeWindow in DisplayControlImpl, which then calls its
     * doMakeMainButtonPanel(), which makes more buttons.
     *
     * @return container of contents
     */
    public Container doMakeContents() {
        try {
            JTabbedPane tab = new MyTabbedPane();
            tab.add("Display", GuiUtils.inset(getDisplayTabComponent(), 5));
            tab.add("Settings",
                    GuiUtils.inset(GuiUtils.top(doMakeWidgetComponent()), 5));
            //Set this here so we don't get odd crud on the screen
            //When the MyTabbedPane goes to paint itself the first time it
            //will set the tab back to 0
            tab.setSelectedIndex(1);
            GuiUtils.handleHeavyWeightComponentsInTabs(tab);
            return tab;
        } catch (Exception exc) {
            logException("doMakeContents", exc);
        }
        return null;
    }

    /**
     * Create the component that goes into the 'Display' tab
     *
     * @return Display tab component
     */
    protected JComponent getDisplayTabComponent() {
        viewContents = spectrumView.getContents();
        //If foreground is not null  then this implies we have been unpersisted
        //We do this here because the CrossSectionViewManager sets the default black on white
        //colors in its init method which might nor be called until we ask for its contents
        if (foreground != null) {
            spectrumView.setColors(background, foreground);
        }
        spectrumView.setContentsBorder(null);
        List compList = new ArrayList();
        final JLabel nameLabel = GuiUtils.rLabel("Wavenumber: ");
        compList.add(nameLabel);
        final JTextField wavenoBox = new JTextField(20);
        compList.add(wavenoBox);
        wavenoBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String newWaveno = wavenoBox.getText().trim();
                System.out.println("new wavenumber = " + newWaveno);
            }
        });
        JPanel waveno = GuiUtils.center(GuiUtils.doLayout(compList,2,GuiUtils.WT_N, GuiUtils.WT_N));
        return GuiUtils.centerBottom(viewContents, waveno);
        //return GuiUtils.centerBottom(viewContents, null);
                                    //- GuiUtils.left(locationComp));
    }

   private class SpectrumUpdater extends CellImpl {
     DataReferenceImpl positionRef;
     DataReferenceImpl spectrumRef;
     boolean init = false;

     public SpectrumUpdater(DataReferenceImpl positionRef, DataReferenceImpl spectrumRef) throws VisADException, RemoteException {
       this.positionRef = positionRef;
       this.spectrumRef = spectrumRef;
       this.addReference(positionRef);
     }

     public void doAction() throws VisADException, RemoteException {
       if (!init) {
         init = true;
         return;
       }
       RealTuple location = (RealTuple) this.positionRef.getData();
       if (location != null) {
         try {
           FlatField spectrum = multiSpecData.getSpectrum(location);
           this.spectrumRef.setData(spectrum);
         }
         catch (Exception e) {
           e.printStackTrace();
           System.out.println("SpectrumUpdater: "+e.getMessage());
         }
       }
     }
   }

   private class RubberBandListener extends CellImpl {
     boolean init = false;
     public RubberBandListener() throws VisADException, RemoteException {
       this.addReference(rubberBandRef);
     }

     public void doAction() throws VisADException, RemoteException {
       if (init) {
         Gridded2DSet set = (Gridded2DSet) rubberBandRef.getData();
         float[] low = set.getLow();
         float[] hi = set.getHi();
         xmap.setRange(low[0], hi[0]);
         ymap.setRange(low[1], hi[1]);
       }
       init = true;
     }
   }

   private class ImageUpdater implements DisplayListener {
     public ImageUpdater() throws VisADException, RemoteException {
       display.addDisplayListener(this);
     }

     public void displayChanged(DisplayEvent e) throws VisADException, RemoteException {
       if (e.getId() == DisplayEvent.MOUSE_RELEASED_CENTER) {
         float xmap_val = (float) display.getDisplayRenderer().getDirectAxisValue(spectrumDomainType);
         int[] idx = spectrumDomain.valueToIndex(new float[][] {{xmap_val}});
         float[][] val = spectrumDomain.indexToValue(idx);
         float channel = val[0][0];
         channelSelectRef.setData(new Gridded2DSet(new RealTupleType(spectrumDomainType, spectrumRangeType),
                                     new float[][] {{channel, channel}, {y_init_range[0], y_init_range[1]}}, 2));

         try {
           ((HydraRGBDisplayable)imageDisplay).getColorMap().resetAutoScale();
           mainViewMaster.reScale();
           imageDisplay.setData(multiSpecData.getImage((float)channel, dataSource.defaultSubset));
         }
         catch (Exception exc) { 
           System.out.println(exc.getMessage());
         }
       }
       if (e.getId() == DisplayEvent.MOUSE_PRESSED_LEFT) {
         if (e.getInputEvent().isShiftDown()) {
           xmap.setRange(x_init_range[0], x_init_range[1]);
           ymap.setRange(y_init_range[0], y_init_range[1]);
         }
       }
     }
   }


    /**
     * Class MyTabbedPane handles the visad component in a tab
     *
     *
     * @author IDV Development Team
     * @version $Revision$
     */
    private class MyTabbedPane extends JTabbedPane implements ChangeListener {
        /** Have we been painted */
        boolean painted = false;
        /**
         * ctor
         */
        public MyTabbedPane() {
            addChangeListener(this);
        }
        /**
         *
         * Handle when the tab has changed. When we move to tab 1 then hide the heavy
         * component. Show it on change to tab 0.
         *
         * @param e The event
         */
        public void stateChanged(ChangeEvent e) {
            if ( !getActive() || !getHaveInitialized()) {
                return;
            }
            if ((spectrumView == null)
                    || (spectrumView.getContents() == null)) {
                return;
            }
            if (getSelectedIndex() == 0) {
                spectrumView.getContents().setVisible(true);
            } else {
                spectrumView.getContents().setVisible(false);
            }
        }
        /**
         * The first time we paint toggle the selected index. This seems to get rid of
         * screen crud
         *
         * @param g graphics
         */
        public void paint(java.awt.Graphics g) {
            if ( !painted) {
                painted = true;
                setSelectedIndex(1);
                setSelectedIndex(0);
                repaint();
            }
            super.paint(g);
        }
    }


}
