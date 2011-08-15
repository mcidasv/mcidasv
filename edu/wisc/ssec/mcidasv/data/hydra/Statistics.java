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
   MathType statType;

   PearsonsCorrelation pCorrelation = null;


   public Statistics(FlatField fltFld) throws VisADException, RemoteException {
      double[][] rngVals = fltFld.getValues(false);
      this.values_x = rngVals;
      rngTupLen = rngVals.length;
      numPoints = fltFld.getDomainSet().getLength();
      descriptiveStats = new DescriptiveStatistics[rngTupLen];
      for (int k=0; k<rngTupLen; k++) {
        descriptiveStats[k] = new DescriptiveStatistics(rngVals[k]);
      }

      MathType rangeType = ((FunctionType)fltFld.getType()).getRange();

      if (rngTupLen > 1) {
        RealType[] rttypes = ((TupleType)rangeType).getRealComponents();
        statType = new RealTupleType(rttypes); 
      }
      else {
        statType = (RealType) rangeType;
      }

      pCorrelation = new PearsonsCorrelation();
   }

   public int numPoints() {
     return numPoints;
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
       stats[k] = pCorrelation.correlation(values_x[k], values_y[k]);
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

}
