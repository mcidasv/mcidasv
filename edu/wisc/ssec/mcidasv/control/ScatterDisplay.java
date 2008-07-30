package edu.wisc.ssec.mcidasv.control;

import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.HistogramField;
import edu.wisc.ssec.mcidasv.data.hydra.SubsetRubberBandBox;
import edu.wisc.ssec.mcidasv.data.hydra.MyRubberBandBoxRendererJ3D;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset;
import edu.wisc.ssec.mcidasv.control.LambertAEA;

import java.util.List;
import java.awt.Container;
import java.awt.Component;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;

import javax.swing.JTabbedPane;

import ucar.unidata.idv.control.DisplayControlImpl;

import ucar.unidata.data.DataChoice;
import ucar.unidata.util.ColorTable;
import visad.VisADException;
import visad.FlatField;
import visad.FieldImpl;
import visad.Data;
import visad.RealType;
import visad.CellImpl;
import visad.Integer1DSet;
import visad.RealTupleType;
import visad.FunctionType;
import visad.ScalarMap;
import visad.Gridded3DSet;
import visad.Gridded2DSet;
import visad.Linear2DSet;
import visad.SampledSet;
import visad.Set;
import visad.UnionSet;
import visad.BaseColorControl;
import visad.georef.MapProjection;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
                                                                                                                                          
import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.XYDisplay;
import ucar.visad.display.RGBDisplayable;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.RubberBandBox;

import ucar.unidata.idv.DisplayConventions;



public class ScatterDisplay extends DisplayControlImpl {
    
    Container container;
    FlatField X_field;
    FlatField Y_field;
    ViewManager scatterView = null;

    DisplayMaster dspMasterX;
    DisplayMaster dspMasterY;

    HistogramField histoField;

    FlatField mask_field;
    FlatField scatterField;
    FlatField scatterFieldMark;
    Data X_data;
    Data Y_data;

    ScatterDisplayable scatterDspMark;

    RGBDisplayable maskX;
    RGBDisplayable maskY;

    LineDrawing lastBoxX;
    LineDrawing lastBoxY;
    
    public ScatterDisplay() {
      super();
    }
    
    @Override public boolean init(List choices) throws VisADException, RemoteException {
        DataSelection dataSelection = getDataSelection();
        X_data = getDataChoice().getData(dataSelection);
        if (X_data instanceof FlatField) {
          X_field = (FlatField) X_data;
        } else if (X_data instanceof FieldImpl) { 
          X_field = (FlatField) ((FieldImpl)X_data).getSample(0);
        }

        popupDataDialog("select Y Axis field", container, false, null);

        //-- hack for a sync problem, use new code from Unidata 
        try {
          java.lang.Thread.sleep(2000);
        } catch (Exception e) {
        }

        //Y_data = getDataChoice().getData(getDataSelection());
        Y_data = getDataChoice().getData(dataSelection);
        if (Y_data instanceof FlatField) {
          Y_field = (FlatField) Y_data;
        } else if (X_data instanceof FieldImpl) {
          Y_field = (FlatField) ((FieldImpl)Y_data).getSample(0);
        }

        dspMasterX = makeImageDisplay(getDataProjection(X_field), X_field);
        dspMasterY = makeImageDisplay(getDataProjection(Y_field), Y_field);

        mask_field = new FlatField(
             new FunctionType(((FunctionType)X_field.getType()).getDomain(), RealType.Generic), 
                  X_field.getDomainSet());

        try {
          histoField = new HistogramField(X_field, Y_field, mask_field, 100, 10); 
        }
        catch (Exception e) {
          e.printStackTrace();
        }


        maskX = new ScatterDisplayable("mask", RealType.Generic, new float[][] {{0},{1},{0}}, false);
        maskX.setData(mask_field);
        maskY = new ScatterDisplayable("mask", RealType.Generic, new float[][] {{0},{1},{0}}, false);
        maskY.setData(mask_field);

        dspMasterX.addDisplayable(maskX);
        dspMasterY.addDisplayable(maskY);

        return true;
    }

    public void initDone() {

       try {
       ScatterDisplayable scatterDsp = new ScatterDisplayable("scatter",
                   RealType.getRealType("mask"), new float[][] {{1,1},{1,0},{1,1}}, false);
       float[] valsX = X_field.getFloats(false)[0];
       float[] valsY = Y_field.getFloats(false)[0];
       Integer1DSet set = new Integer1DSet(valsX.length);
       FlatField scatter = new FlatField(
           new FunctionType(RealType.Generic,
               new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), set);
       float[] mask = new float[valsX.length];
       for (int k=0; k<mask.length; k++) mask[k] = 0;
       scatter.setSamples(new float[][] {valsX, valsY, mask});
       scatterDsp.setPointSize(2f);
       scatterDsp.setRangeForColor(0,1);
       scatterDsp.setData(scatter);
       scatterField = scatter;

       scatterDspMark = new ScatterDisplayable("scatter",
                   RealType.getRealType("mask"), new float[][] {{1,1},{1,0},{1,1}}, false);
       set = new Integer1DSet(2);
       scatter = new FlatField(
           new FunctionType(RealType.Generic,
               new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), set);
       scatterDspMark.setData(scatter);
       scatterFieldMark = scatter;

       scatterDspMark.setPointSize(2f);
       scatterDspMark.setRangeForColor(0,1);
                                                                                                                                                           
       DisplayMaster master = scatterView.getMaster();
       master.addDisplayable(scatterDsp);
       master.addDisplayable(scatterDspMark);


        final LineDrawing selectBox = new LineDrawing("select");
        selectBox.setColor(Color.green);

        final RubberBandBox rbb =
            new RubberBandBox(RealType.XAxis, RealType.YAxis, 1);
        rbb.setColor(Color.green);
        final double[] x_coords = new double[2];
        final double[] y_coords = new double[2];

        rbb.addAction(new CellImpl() {
          boolean init = false;
          public void doAction()
             throws VisADException, RemoteException
           {
              if (!init) {
                init = true;
                return;
              }
              Gridded2DSet set = rbb.getBounds();
              float[] low = set.getLow();
              float[] hi = set.getHi();
              x_coords[0] = low[0];
              x_coords[1] = hi[0];
              y_coords[0] = low[1];
              y_coords[1] = hi[1];

              SampledSet[] sets = new SampledSet[4];
              sets[0] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][] {{low[0], hi[0]}, {low[1], low[1]}}, 2);
              sets[1] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][] {{hi[0], hi[0]}, {low[1], hi[1]}}, 2);
              sets[2] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][] {{hi[0], low[0]}, {hi[1], hi[1]}}, 2);
              sets[3] = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, new float[][] {{low[0], low[0]}, {hi[1], low[1]}}, 2);
              UnionSet uset = new UnionSet(sets);
              selectBox.setData(uset);
 
              try {
                FlatField updateMask = histoField.markMaskFieldByRange(x_coords, y_coords, 0);
                maskX.setData(updateMask);
              } catch (Exception e) {
              }
           }
        });
        master.addDisplayable(rbb);
        master.addDisplayable(selectBox);
        master.draw();
       } catch (Exception e) {
         e.printStackTrace();
       }

       try {
         SubsetRubberBandBox X_subsetBox =
            new SubsetRubberBandBox(X_field, ((MapProjectionDisplayJ3D)dspMasterX).getDisplayCoordinateSystem(), 1, false);
         X_subsetBox.setColor(Color.magenta);
         MarkScatterPlot markX = new MarkScatterPlot(X_subsetBox, X_field.getDomainSet(), scatterField);
         X_subsetBox.addAction(markX);

         SubsetRubberBandBox Y_subsetBox =
            new SubsetRubberBandBox(Y_field, ((MapProjectionDisplayJ3D)dspMasterY).getDisplayCoordinateSystem(), 1, false);
         Y_subsetBox.setColor(Color.magenta);
         MarkScatterPlot markY = new MarkScatterPlot(Y_subsetBox, Y_field.getDomainSet(), scatterField);
         Y_subsetBox.addAction(markY);

         dspMasterX.addDisplayable(X_subsetBox);
         dspMasterY.addDisplayable(Y_subsetBox);
         lastBoxX = new LineDrawing("last_box");
         lastBoxY = new LineDrawing("last_box");
         lastBoxX.setColor(Color.magenta);
         lastBoxY.setColor(Color.magenta);
         dspMasterX.addDisplayable(lastBoxX);
         dspMasterY.addDisplayable(lastBoxY);
       }
       catch (Exception e) {
         e.printStackTrace();
       }
    } 
    
    @Override public Container doMakeContents() {
        JTabbedPane pane = new JTabbedPane();

        //-pane.add("Scatter", GuiUtils.inset(getScatterTabComponent(),5));
        GuiUtils.handleHeavyWeightComponentsInTabs(pane);

        Component[] comps = new Component[] {null, null, null};
        comps[0] = dspMasterX.getComponent();
        comps[1] = dspMasterY.getComponent();
        comps[2] = getScatterTabComponent();
        JPanel panel = GuiUtils.flowRight(comps);
        pane.add(GuiUtils.inset(panel,5));
 

        container = pane;
        return pane;
    }
    
//    @Override public void doRemove() throws VisADException, RemoteException {
//        super.doRemove();
//    }


    protected JComponent getScatterTabComponent() {
       try {
         scatterView = new ViewManager(getViewContext(),
                             new XYDisplay("Scatter", RealType.XAxis, RealType.YAxis),
                             new ViewDescriptor("scatter"), "showControlLegend=false;");
       } catch (Exception e) {
         e.printStackTrace();
       }
                                                                                                                                          
       //-return GuiUtils.centerBottom(scatterView.getContents(), null);
       return scatterView.getComponent();
    }

    public DisplayMaster makeImageDisplay(MapProjection mapProj, FlatField image) 
           throws VisADException, RemoteException {
      MapProjectionDisplayJ3D mapProjDsp;
      DisplayMaster dspMaster;

      mapProjDsp = new MapProjectionDisplayJ3D(MapProjectionDisplay.MODE_2Din3D);
      mapProjDsp.enableRubberBanding(false);
      dspMaster = mapProjDsp;
      mapProjDsp.setMapProjection(mapProj);
      RealType imageRangeType =
        (((FunctionType)image.getType()).getFlatRange().getRealComponents())[0];

      HydraRGBDisplayable imageDsp = new HydraRGBDisplayable("image", imageRangeType, null, false, null);

      imageDsp.setData(image);
      dspMaster.addDisplayable(imageDsp);
      dspMaster.draw();


      DisplayConventions dc = getDisplayConventions();
      Range[] range = GridUtil.fieldMinMax(image);
      Range imageRange = range[0];
      String canonicalName = DataAlias.aliasToCanonical(imageRangeType.getName());
      Range dfltRange = dc.getParamRange(canonicalName, null); 

      if (dfltRange == null) {
        imageRange = range[0];
      }
      else if ((imageRange.getMax() - imageRange.getMin()) < (dfltRange.getMax() - dfltRange.getMin())) {
      
      }
      else {
        imageRange = dfltRange;
      }

      ScalarMap colorMap = imageDsp.getColorMap();
      colorMap.setRange(imageRange.getMin(), imageRange.getMax());
      BaseColorControl clrCntrl = (BaseColorControl) colorMap.getControl();

      clrCntrl.setTable(
           dc.getParamColorTable(
                 imageRangeType.getName()).getColorTable());

      return dspMaster;
    }

    public MapProjection getDataProjection(FlatField image) 
           throws VisADException, RemoteException {
      MapProjection mp = null;
      Rectangle2D rect = MultiSpectralData.getLonLatBoundingBox(image);
      try {
        mp = new LambertAEA(rect);
      } catch (Exception e) {
        System.out.println(" getDataProjection"+e);
      }
      return mp;
    }

    private class ScatterDisplayable extends RGBDisplayable {
                                                                                                                                          
       ScatterDisplayable(String name, RealType rgbRealType, float[][] colorPalette, boolean alphaflag) 
           throws VisADException, RemoteException {
         super(name, rgbRealType, colorPalette, alphaflag);
       }
                                                                                                                                          
    }


    private class MarkScatterPlot extends CellImpl {
        boolean init = false;
        SubsetRubberBandBox subsetBox;
        FlatField scatterField;
        Set imageDomain;
        int domainLen_0;
        int domainLen_1;
        float[][] scatter;
        MarkScatterPlot other;

        MarkScatterPlot(SubsetRubberBandBox subsetBox, Set imageDomain, FlatField scatterField) {
          super();
          this.subsetBox = subsetBox;
          this.scatterField = scatterField;
          this.imageDomain = imageDomain;
          int[] lens = ((Gridded2DSet)imageDomain).getLengths();
          domainLen_0 = lens[0];
          domainLen_1 = lens[1];
        }

        public void doAction()
             throws VisADException, RemoteException
        {
           if (!init) {
             init = true;
             scatter = scatterField.getFloats(true);
             return;
           }

           Gridded2DSet set = subsetBox.getBounds();
           float[][] corners = set.getSamples(false);
           float[][] coords = corners;
           if (imageDomain instanceof Linear2DSet) {
             coords = ((Gridded2DSet)imageDomain).valueToGrid(corners);
           }

           float[] coords_0 = coords[0];
           float[] coords_1 = coords[1];

           int low_0 = Math.round(Math.min(coords_0[0], coords_0[1]));
           int low_1 = Math.round(Math.min(coords_1[0], coords_1[1]));
           int hi_0  = Math.round(Math.max(coords_0[0], coords_0[1]));
           int hi_1  = Math.round(Math.max(coords_1[0], coords_1[1]));

           int len_0 = (hi_0 - low_0) + 1;
           int len_1 = (hi_1 - low_1) + 1;

           int len = len_0*len_1;

           float[][] markScatter = new float[3][len];
           for (int k=0; k<len; k++) {
             markScatter[0][k] = 0;
             markScatter[1][k] = 0;
             markScatter[2][k] = 0;
           }

           for (int j=0; j<len_1; j++) {
             for (int i=0; i<len_0; i++) {
               int idx = (j+low_1)*domainLen_0 + (i+low_0);

               int k = j*len_0 + i;
               markScatter[0][k] = scatter[0][idx];
               markScatter[1][k] = scatter[1][idx];
               markScatter[2][k] = 1;
             }
           }

           Integer1DSet dset = new Integer1DSet(len);
           FlatField scatterFieldMark = new FlatField(
           new FunctionType(RealType.Generic,
               new RealTupleType(RealType.XAxis, RealType.YAxis, RealType.getRealType("mask"))), dset);

           scatterFieldMark.setSamples(markScatter, false);
           scatterDspMark.setData(scatterFieldMark);
           updateBox();
        }

        public void setOther(MarkScatterPlot other) {
          this.other = other;
        }

        public void updateBox() throws VisADException, RemoteException {
           Gridded3DSet set3D = subsetBox.getLastBox();
           float[][] samples = set3D.getSamples(false);
           Gridded2DSet set2D = 
              new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, 
                                  new float[][] {samples[0], samples[1]}, samples[0].length);
           lastBoxX.setData(set2D);
           lastBoxY.setData(set2D);
        }

        public SubsetRubberBandBox getRubberBandBox() {
          return subsetBox;
        }
    }
    
}
