package edu.wisc.ssec.mcidasv.util;

import java.rmi.RemoteException;
import visad.FlatField;
import visad.FunctionType;
import visad.Linear1DSet;
import visad.Linear2DSet;
import visad.MathType;
import visad.RealTupleType;
import visad.VisADException;
import java.lang.Math;
import visad.CommonUnit;
import visad.Data;
import visad.RealType;
import visad.Set;
import visad.SetException;
import visad.SetType;

/**
 * Utilities for efficiently upscaling and downscaling data referenced on the GOES-16
 * 2km, 1km and hkm Fixed Grid Frame (FGF). Adapted from Visad resample, but realizes 
 * efficiencies when target and source domain sets are both rectilinear, especially 
 * avoiding the expansion of the domain samples (getSamples), in this special case.
 * Follows resampling rules outlined in the GOES-16 ABI ATBD for the Clould and Moisture 
 * Imagery Product (CMIP) when incoming grids are at the their full resolution. 
 * Should be thought of as a grid transfer operation, to be used in conjunction with, 
 * not necessarily a replacement for, FlatField.resample.
 */

public class GOESgridUtil {
   
   public static RealTupleType LamdaTheta;
   static {
      try {
         LamdaTheta = new RealTupleType(RealType.getRealType("lamda", CommonUnit.radian), RealType.getRealType("theta", CommonUnit.radian));
      }
      catch (VisADException e) {
      }
   }
   
   private static int uniqueID = 0;
   
   private static double offsetTolerance = 0.01;
   
   private static float accumTolerance = 0.01f;
   
   /**
    * 
    * @param red
    * @param grn
    * @param blu
    * 
    * DomainSets of red, grn, blu must be (lamda, theta): the intermediate view angle coordinates (radians)
    * for the Fixed Grid Frame. See makeGEOSRadiansDomainField below. 
    * TargetSet is taken as that of the first component field (red).
    * 
    * @return
    * @throws VisADException
    * @throws RemoteException 
    */
   public static FlatField combineRGB(FlatField red, FlatField grn, FlatField blu) throws VisADException, RemoteException {
      return combineRGB(red, grn, blu, (Linear2DSet)red.getDomainSet(), false);
   }
   
   /**
    * 
    * @param red
    * @param grn
    * @param blu
    * @param targetSet
    * @return
    * @throws VisADException
    * @throws RemoteException 
    */
   public static FlatField combineRGB(FlatField red, FlatField grn, FlatField blu, Linear2DSet targetSet) throws VisADException, RemoteException {
      return combineRGB(red, grn, blu, targetSet, true);
   }

   /**
    * 
    * @param red
    * @param grn
    * @param blu
    * @param targetSet the target, or outgoing domain
    * @param copy to copy range of resulting RGB FlatField (default=true)
    * 
    * DomainSets of red, grn, blu and targetSet must be (lamda, theta): the intermediate view angle coordinates (radians)
    * for the Fixed Grid Frame. See makeGEOSRadiansDomainField below.
    * 
    * @return
    * @throws VisADException
    * @throws RemoteException 
    */   
   public static FlatField combineRGB(FlatField red, FlatField grn, FlatField blu, Linear2DSet targetSet, boolean copy) throws VisADException, RemoteException {
     
       Linear2DSet setR = (Linear2DSet) red.getDomainSet();
       Linear2DSet setG = (Linear2DSet) grn.getDomainSet();
       Linear2DSet setB = (Linear2DSet) blu.getDomainSet();
       
       float[] redValues;
       float[] grnValues;
       float[] bluValues;
       
       RealTupleType newRangeType = new RealTupleType(new RealType[] 
           {RealType.getRealType("redimage_"+uniqueID), RealType.getRealType("greenimage_"+uniqueID), RealType.getRealType("blueimage_"+uniqueID)});
          
       FlatField rgb = new FlatField(new FunctionType(((SetType)targetSet.getType()).getDomain(), newRangeType), targetSet);
       
       if (targetSet.equals(setR) && targetSet.equals(setB) && targetSet.equals(setG)) {
          redValues = red.getFloats(false)[0];
          grnValues = grn.getFloats(false)[0];
          bluValues = blu.getFloats(false)[0];
       }
       else {
          redValues = goesResample(red, targetSet).getFloats(false)[0];
          grnValues = goesResample(grn, targetSet).getFloats(false)[0];
          bluValues = goesResample(blu, targetSet).getFloats(false)[0];
       }
          
       // For RGB composite, if any NaN -> all NaN
       for (int k=0; k<redValues.length; k++) {
          if (Float.isNaN(redValues[k]) || Float.isNaN(grnValues[k]) || Float.isNaN(bluValues[k])) {
             redValues[k] = Float.NaN;
             grnValues[k] = Float.NaN;
             bluValues[k] = Float.NaN;
          }
       }
          
       rgb.setSamples(new float[][] {redValues, grnValues, bluValues}, copy);                     
          
       uniqueID++;
       return rgb;
   }
   
   
   /**
    * Transforms FlatField with DomainCoordinateSystem (fgf_x, fgf_y) <-> (Lon,Lat) based on the Geostationary projection 
    * from fixed grid coordinates to intermediate coordinates view angle coordinates in radians (lamda, theta).
    *
    * @param fltFld
    *         The incoming FlatField 
    * @param scaleX
    * @param offsetX
    *         To transform to fgf_x -> lamda (radians)
    * @param scaleY
    * @param offsetY
    *         To transform to fgf_y -> theta (radians)
    * 
    * @return FlatField with transformed Domain and original (not copied) Range
    */   
   public static FlatField makeGEOSRadiansDomainField(FlatField fltFld, double scaleX, double offsetX, double scaleY, double offsetY) throws VisADException, RemoteException  {
       Linear2DSet domainSet = (Linear2DSet) fltFld.getDomainSet();
       MathType rangeType = ((FunctionType)fltFld.getType()).getRange();
       float[][] rangeVals = fltFld.getFloats(false);
       Linear1DSet setX = domainSet.getX();
       Linear1DSet setY = domainSet.getY();

       int lenX = setX.getLength();
       int lenY = setY.getLength();

       double firstX = setX.getFirst()*scaleX + offsetX;
       double firstY = setY.getFirst()*scaleY + offsetY;

       double lastX = setX.getLast()*scaleX + offsetX;
       double lastY = setY.getLast()*scaleY + offsetY;   

       Linear2DSet dSetRadians = new Linear2DSet(firstX, lastX, lenX, firstY, lastY, lenY);
       fltFld = new FlatField(new FunctionType(LamdaTheta, rangeType), dSetRadians);
       fltFld.setSamples(rangeVals, false);  
       return fltFld;
   }
   
  /**
   * Efficiently upscales or downscales between ABI fields with domainSets on the Fixed Grid Frame. 
   * Can be seen as grid transfer process, to be used in conjunction with, not a replacement for, VisAD resample.
   * 
   * @param fld
   * @param targetSet
   * 
   * DomainSets must be (lamda, theta): the intermediate view angle coordinates (radians)
   * for the Fixed Grid Frame. See makeGEOSRadiansDomainField below.
   * 
   * @return result field with targetSet domain.
   * @throws VisADException
   * @throws RemoteException 
   */
   
  public static FlatField goesResample(FlatField fld, Linear2DSet targetSet) throws VisADException, RemoteException {
     return goesResample(fld, targetSet, Data.WEIGHTED_AVERAGE); 
  }
  
  /**
   * Efficiently upscales or downscales between ABI fields with domainSets on the Fixed Grid Frame. 
   * Can be seen as grid transfer process, to be used in conjunction with, not a replacement for, VisAD resample.
   * 
   * @param fld
   * @param targetSet
   * @param mode VisAD resample mode
   * 
   * DomainSets must be (lamda, theta): the intermediate view angle coordinates (radians)
   * for the Fixed Grid Frame. See makeGEOSRadiansDomainField below.
   * 
   * @return result field with targetSet domain.
   * @throws VisADException
   * @throws RemoteException 
   */
  public static FlatField goesResample(FlatField fld, Linear2DSet targetSet, int mode) throws VisADException, RemoteException {
          
       double targetStepX = targetSet.getX().getStep();
       double targetStepY = targetSet.getY().getStep();
       FunctionType fncType = (FunctionType) fld.getType();
       RealTupleType setType = ((SetType)targetSet.getType()).getDomain();
       FunctionType targetType = new FunctionType(setType, fncType.getRange());
       FlatField target = new FlatField(targetType, targetSet);

       Linear2DSet setR = null;
       Set set = fld.getDomainSet();
       if (set instanceof Linear2DSet) {
          setR = (Linear2DSet) set;
       }
       else {
          throw new VisADException("FlatField must have Linear2DSet domain, use resample");
       }

       /* Just return the incoming field in this case */
       if (setR.equals(targetSet)) {
          return fld;
       }

       double setStepX = setR.getX().getStep();
       double setStepY = setR.getY().getStep();
       double stepRatioX = targetStepX/setStepX;
       double stepRatioY = targetStepY/setStepY;

       boolean upsample = (stepRatioX > 1) && (stepRatioY > 1);

       Linear1DSet setX = ((Linear2DSet)targetSet).getX();
       Linear1DSet setY = ((Linear2DSet)targetSet).getY();

       int lenX = setX.getLength();
       int lenY = setY.getLength();             
       int lenXR = setR.getX().getLength();
       int lenYR = setR.getY().getLength(); 

       boolean ok = upsample;

       ok = ok && ((Math.abs(setX.getFirst() - setR.getX().getFirst()) % (setR.getX().getStep()/2)) < offsetTolerance &&
                   (Math.abs(setY.getFirst() - setR.getY().getFirst()) % (setR.getY().getStep()/2)) < offsetTolerance);


       ok = ok && (Math.abs(stepRatioX - 4.0)*lenX < accumTolerance && Math.abs(stepRatioY - 4.0)*lenY < accumTolerance) ||
                  (Math.abs(stepRatioX - 2.0)*lenX < accumTolerance && Math.abs(stepRatioY - 2.0)*lenY < accumTolerance);

       float[][] values = fld.getFloats(false);
       float[][] targetValues = new float[values.length][targetSet.getLength()];
       for (int t=0; t< targetValues.length; t++) {
          java.util.Arrays.fill(targetValues[t], Float.NaN);
       }

       float[][] xgrid = valueToGrid(setR.getX(), setX.getSamples(false));
       float[][] ygrid = valueToGrid(setR.getY(), setY.getSamples(false));

       if (!upsample || !ok) {
          interp(ygrid[0], xgrid[0], lenYR, lenXR, values, lenY, lenX, targetValues, mode);             
       }
       else {
          int[] xidxs = new int[xgrid[0].length];
          int[] yidxs = new int[ygrid[0].length];

          for (int k=0; k<xidxs.length; k++) {
             float x = xgrid[0][k];
             xidxs[k] = Float.isNaN(x) ? -1 : (int) Math.floor(x);
          }
          for (int k=0; k<yidxs.length; k++) {
             float y = ygrid[0][k];
             yidxs[k] = Float.isNaN(y) ? -1 : (int) Math.floor(y);                
          }

          goesUpsample(yidxs, xidxs, lenYR, lenXR, values, lenY, lenX, targetValues, mode);
       }

       target.setSamples(targetValues, false);

       return target;
   }
  
   static void goesUpsample(int[] yidxs, int[] xidxs, int lenY, int lenX, float[][] values, int targetLenY, int targetLenX, float[][] targetValues, int mode) {
       int tupleDimLen = values.length;
       for (int j=0; j<targetLenY; j++) {
          int jR = yidxs[j];
          if (jR >= 0 && jR < lenY) {

             for (int i=0; i<targetLenX; i++) {
                int iR = xidxs[i];
                if (iR >= 0 && iR < lenX) {
                   int k = j*targetLenX + i;
                   int kR = jR*lenX + iR;

                   if (mode == Data.NEAREST_NEIGHBOR) {
                      for (int t=0; t<tupleDimLen; t++) {
                         targetValues[t][k] = values[t][kR+lenX]; // Upper right corner (view angle space) see PUG
                      }
                   }
                   else {
                      for (int t=0; t<tupleDimLen; t++) {
                         float val = 0;
                         val += values[t][kR];
                         val += values[t][kR+1];
                         val += values[t][kR+lenX];
                         val += values[t][kR+lenX+1];

                         targetValues[t][k] = val/4;
                      }
                   }
                }
             }
          }
       }       
   }

   static void interp(float[] yidxs, float[] xidxs, int lenY, int lenX, float[][] values, int targetLenY, int targetLenX, float[][] targetValues, int mode) {
       int tupleDimLen = values.length;
       int[] idxs = new int[2];
       float[] flts = new float[4];

       for (int j=0; j<targetLenY; j++) {
          float y = yidxs[j];
          int jR = Float.isNaN(y) ? -1 : (int) Math.floor(y);             
          if (jR >= 0 && jR < lenY) {

             for (int i=0; i<targetLenX; i++) {
                float x = xidxs[i];
                int iR = Float.isNaN(x) ? -1 : (int) Math.floor(x);                                      
                if (iR >= 0 && iR < lenX) {
                   int k = j*targetLenX + i;
                   int kR = jR*lenX + iR;


                   float dx00 = xidxs[i] - iR;
                   float dy00 = yidxs[j] - jR;

                   float dx01 = xidxs[i] - (iR + 1);
                   float dy01 = yidxs[j] - jR;

                   float dx10 = xidxs[i] - iR;
                   float dy10 = yidxs[j] - (jR+1);

                   float dx11 = xidxs[i] - (iR+1);
                   float dy11 = yidxs[j] - (jR+1);                      

                   float dst00 = (float) Math.sqrt(dx00*dx00 + dy00*dy00);
                   float dst01 = (float) Math.sqrt(dx01*dx01 + dy01*dy01);
                   float dst10 = (float) Math.sqrt(dx10*dx10 + dy10*dy10);
                   float dst11 = (float) Math.sqrt(dx11*dx11 + dy11*dy11);

                   float sum = dst00 + dst01 + dst10 + dst11;

                   if (mode == Data.NEAREST_NEIGHBOR) {
                      flts[0] = dst00;
                      flts[1] = dst01;
                      flts[2] = dst10;
                      flts[3] = dst11;
                      minmax(flts, 4, idxs);

                      for (int t=0; t<tupleDimLen; t++) {
                         float val = Float.NaN;
                         if (idxs[0] == 0) {
                            val = values[t][kR];
                         }
                         else if (idxs[0] == 1) {
                            val = values[t][kR+1];
                         }
                         else if (idxs[0] == 2) {
                            val = values[t][kR+lenX];
                         }
                         else if (idxs[0] == 3) {
                            val = values[t][kR+lenX+1];
                         }
                         targetValues[t][k] = val;
                      }
                   }
                   else {
                      float w00 = dst00/sum;
                      float w01 = dst01/sum;
                      float w10 = dst10/sum;
                      float w11 = dst11/sum;

                      for (int t=0; t<tupleDimLen;t++) {
                         float val = 0;
                         val += w00*values[t][kR];
                         val += w01*values[t][kR+1];
                         val += w10*values[t][kR+lenX];
                         val += w11*values[t][kR+lenX+1];

                         targetValues[t][k] = val;
                      }
                   }
                }
             }
          }
       }       
   }
   
   
   public static float[] minmax(float[] values, int length, int[] indexes) {
      float min =  Float.MAX_VALUE;
      float max = -Float.MAX_VALUE;
      int minIdx = 0;
      int maxIdx = 0;
      for (int k = 0; k < length; k++) {
        float val = values[k];
        if ((val == val) && (val < Float.POSITIVE_INFINITY) && (val > Float.NEGATIVE_INFINITY)) {
          if (val < min) {
             min = val;
             minIdx = k;
          }
          if (val > max) {
             max = val;
             maxIdx = k;
          }
        }
      }
      if (indexes != null) {
         indexes[0] = minIdx;
         indexes[1] = maxIdx;
      }
      return new float[] {min, max};
   }  
    
  /**
   * Adapted from VisAD except NaN is returned unless:
   * val >= First and val <= Last, First > Last
   * val <= First and val >= Last, Last > First
   * i.e., clamp to inside the interval inclusive
   * @param set
   * @param value
   * @return
   * @throws VisADException 
   */
  private static float[][] valueToGrid(Linear1DSet set, float[][] value) throws VisADException {
    if (value.length != 1) {
      throw new SetException("Linear1DSet.valueToGrid: value dimension" +
                             " should be 1, not " + value.length);
    }
    double First = set.getFirst();
    double Last = set.getLast();
    double Step = set.getStep();
    double Invstep = 1.0/Step;
    int Length = set.getLength();
    int length = value[0].length;
    float[][] grid = new float[1][length];
    float[] grid0 = grid[0];
    float[] value0 = value[0];
    float l = (float) First;
    float h = (float) Last;
    float v;

    if (h < l) {
      float temp = l;
      l = h;
      h = temp;
    }
    for (int i=0; i<length; i++) {
      v = value0[i];
      grid0[i] = (float) ((l < v && v < h) ? (v - First) * Invstep : Float.NaN);
    }
    return grid;
  }    
   
}
