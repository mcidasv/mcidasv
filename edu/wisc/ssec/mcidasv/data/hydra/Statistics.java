package edu.wisc.ssec.mcidasv.data.hydra;

import visad.*;
import java.rmi.RemoteException;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math.stat.correlation.Covariance;


public class Statistics {

   DescriptiveStatistics[] descriptiveStats = null;
   double[][] values_x;
   int rngTupLen;
   int numPoints;
   int[] numGoodPoints;
   MathType statType;

   PearsonsCorrelation pCorrelation = null;


   public Statistics(FlatField fltFld) throws VisADException, RemoteException {
      double[][] rngVals = fltFld.getValues(false);
      values_x = rngVals;
      rngTupLen = rngVals.length;
      numPoints = fltFld.getDomainSet().getLength();
      numGoodPoints = new int[rngTupLen];

      for (int k=0; k<rngTupLen; k++) {
        values_x[k] = removeMissing(rngVals[k]);
        numGoodPoints[k] = values_x[k].length;
      }

      descriptiveStats = new DescriptiveStatistics[rngTupLen];
      for (int k=0; k<rngTupLen; k++) {
        descriptiveStats[k] = new DescriptiveStatistics(values_x[k]);
      }

      MathType rangeType = ((FunctionType)fltFld.getType()).getRange();

      if (rangeType instanceof RealTupleType) {
        RealType[] rttypes = ((TupleType)rangeType).getRealComponents();
        if (rngTupLen > 1) {
          statType = new RealTupleType(rttypes);
        }
        else {
          statType = (RealType) rttypes[0];
        }
      }
      else if (rangeType instanceof RealType) {
        statType = (RealType) rangeType;
      }
      else {
         throw new VisADException("incoming type must be RealTupleType or RealType");
      }

      pCorrelation = new PearsonsCorrelation();
   }

   public int numPoints() {
     return numPoints;
   }

   private double[] removeMissing(double[] vals) {
     int num = vals.length;
     int cnt = 0;
     int[] good = new int[num];
     for (int k=0; k<num; k++) {
        if ( !(Double.isNaN(vals[k])) ) {
          good[cnt] = k;
          cnt++;
        }
     }

     if (cnt == num) {
        return vals;
     }

     double[] newVals = new double[cnt];
     for (int k=0; k<cnt; k++) {
       newVals[k] = vals[good[k]];
     }

     return newVals;
   }

   public Data mean() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getMean();
     }
     return makeStat(stats);
   }

   public Data geometricMean() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getGeometricMean();
     }
     return makeStat(stats);
   }


   public Data max() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getMax();
     }
     return makeStat(stats);
   }

   public Data min() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getMin();
     }
     return makeStat(stats);
   }

   public Data median() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getPercentile(50.0);
     }
     return makeStat(stats);
   }

   public Data variance() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getVariance();
     }
     return makeStat(stats);
   }

   public Data kurtosis() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getKurtosis();
     }
     return makeStat(stats);
   }

   public Data standardDeviation() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getStandardDeviation();
     }
     return makeStat(stats);
   }

   public Data skewness() throws VisADException, RemoteException {
     double[] stats = new double[rngTupLen];
     for (int k=0; k<rngTupLen; k++) {
       stats[k] = descriptiveStats[k].getSkewness();
     }
     return makeStat(stats);
   }

   public Data correlation(FlatField fltFld) throws VisADException, RemoteException {
     double[][] values_y = fltFld.getValues(false);
     if ((values_y.length != rngTupLen) || (values_y[0].length != numPoints)) {
       throw new VisADException("both fields must have same numPoints and range tuple length");
     }
     double[] stats = new double[rngTupLen];
     
     for (int k=0; k<rngTupLen; k++) {
       double[][] newVals = removeMissingAND(values_x[k], values_y[k]);
       stats[k] = pCorrelation.correlation(newVals[0], newVals[1]);
     }
     return makeStat(stats);
   }

   private Data makeStat(double[] stats) throws VisADException, RemoteException {
     if (statType instanceof RealType) {
       return new Real((RealType)statType, stats[0]);
     }
     else if (statType instanceof RealTupleType) {
       return new RealTuple((RealTupleType)statType, stats);
     }
     return null;
   }

   private double[][] removeMissingAND(double[] vals_x, double[] vals_y) {
     int cnt = 0;
     int[] good = new int[vals_x.length];
     for (int k=0; k<vals_x.length; k++) {
       if ( !(Double.isNaN(vals_x[k])) && !(Double.isNaN(vals_y[k]))  ) {
         good[cnt] = k;
         cnt++;
       }
     }

     if (cnt == vals_x.length) {
       return new double[][] {vals_x, vals_y};
     }
     else {
       double[] newVals_x = new double[cnt];
       double[] newVals_y = new double[cnt];
       for (int k=0; k<cnt; k++) {
         newVals_x[k] = vals_x[good[k]];
         newVals_y[k] = vals_y[good[k]];
       }
       return new double[][] {newVals_x, newVals_y};
     }
   }

}
