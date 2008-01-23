package edu.wisc.ssec.mcidasv.control;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.DataSelection;

import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.DisplayControl;

import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Displayable;

import visad.*;
import visad.VisADException;
import visad.RemoteVisADException;
import visad.ReferenceException;
import visad.QuickSort;

import edu.wisc.ssec.mcidasv.data.hydra.HydraRGBDisplayable;
//-import edu.wisc.ssec.mcidasv.data.hydra.MyRGBDisplayable;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionDataSource;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset;

import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ViewDescriptor;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.Range;

import java.util.HashMap;


import java.rmi.RemoteException;



public class ProfileAlongTrackControl extends DisplayControlImpl {

  private DataChoice dataChoice;
  
  private DisplayableData imageDisplay;

  private DisplayMaster mainViewMaster;

  private RealType imageRangeType;

  public MultiDimensionSubset subset;


  public ProfileAlongTrackControl() {
    super();
    setAttributeFlags(FLAG_COLORTABLE | FLAG_SELECTRANGE);
  }

  public boolean init(DataChoice dataChoice) throws VisADException, RemoteException {
    this.dataChoice = dataChoice;
    subset = (MultiDimensionSubset) dataChoice.getDataSelection();
    FlatField image = (FlatField) this.dataChoice.getData(null);
    imageRangeType = (RealType) ((FunctionType)image.getType()).getRange();
    imageDisplay = create3DDisplay(image);
    ViewManager vm = getViewManager();
    mainViewMaster = vm.getMaster();
    addDisplayable(imageDisplay, FLAG_COLORTABLE | FLAG_SELECTRANGE);

    return true;
  }

  private DisplayableData create3DDisplay(FlatField image) throws VisADException, RemoteException {
    Gridded3DSet domainSet = (Gridded3DSet) image.getDomainSet();
    int[] lens = domainSet.getLengths();
    float[] range_values = (image.getFloats(false))[0];
    range_values = medianFilter(range_values, lens[0], lens[1], 5, 7);
    image.setSamples(new float[][] {range_values});
    RealType imageRangeType = (RealType) ((FunctionType)image.getType()).getRange();
    //-MyRGBDisplayable imageDsp = new MyRGBDisplayable("image", imageRangeType, null, true);
    HydraRGBDisplayable imageDsp = new HydraRGBDisplayable("image", imageRangeType, null, true, null);
    imageDsp.setData(image);
    return imageDsp;
  }

  private DisplayableData create2DDisplay() throws VisADException, RemoteException {
    return null;
  }

  protected ColorTable getInitialColorTable() {
    return getDisplayConventions().getParamColorTable(imageRangeType.getName());
  }

  protected Range getInitialRange() throws RemoteException, VisADException {
      Range range = getDisplayConventions().getParamRange(imageRangeType.getName(), null);
                          //getDisplayUnit());
        setSelectRange(range);
        return range;
  }

  FlatField filter(FlatField field) throws VisADException, RemoteException {
    return field;
  }

  public static float[] medianFilter(float[] A, int lenx, int leny, int window_lenx, int window_leny)
         throws VisADException {
    float[] result =  new float[A.length];
    float[] window =  new float[window_lenx*window_leny];
    float[] new_window =  new float[window_lenx*window_leny];
    int[] sort_indexes = new int[window_lenx*window_leny];
                                                                                                                                               
    int a_idx;
    int w_idx;
                                                                                                                                               
    int w_lenx = window_lenx/2;
    int w_leny = window_leny/2;
                                                                                                                                               
    int lo;
    int hi;
    int ww_jj;
    int ww_ii;
    int cnt;
                                                                                                                                               
    for (int j=0; j<leny; j++) {
      for (int i=0; i<lenx; i++) {
        a_idx = j*lenx + i;
                                                                                                                                               
        cnt = 0;
        for (int w_j=-w_leny; w_j<w_leny; w_j++) {
          for (int w_i=-w_lenx; w_i<w_lenx; w_i++) {
            ww_jj = w_j + j;
            ww_ii = w_i + i;
            w_idx = (w_j+w_leny)*window_lenx + (w_i+w_lenx);
            if ((ww_jj >= 0) && (ww_ii >=0) && (ww_jj < leny) && (ww_ii < lenx)) {
              window[cnt] = A[ww_jj*lenx+ww_ii];
              cnt++;
            }
          }
        }
        System.arraycopy(window, 0, new_window, 0, cnt);
        //-sort_indexes = QuickSort.sort(new_window, sort_indexes);
        sort_indexes = QuickSort.sort(new_window);
        result[a_idx] = new_window[cnt/2];
      }
    }
    return result;
  }


}
