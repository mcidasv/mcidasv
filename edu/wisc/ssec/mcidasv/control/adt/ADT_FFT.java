/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2016
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
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
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.control.adt;

import java.lang.Math;

@SuppressWarnings("unused")

public class ADT_FFT {

   private static int FFTBINS=64;
   private static double PI = 3.14159265358979;

   private static double FFT_Real[] = new double[FFTBINS];
   private static double FFT_Complex[] = new double[FFTBINS];
   private static double FFT_Magnitude[] = new double[FFTBINS];

   public ADT_FFT() {
   }

   private static int dfft() {

      /**
      ** A Duhamel-Hollman split-radix dif fft
      ** Ref: Electronics Letters, Jan. 5, 1984
      ** Complex input and output data in arrays x and y
      ** Length is n.
      ** Inputs  : RealArray_Input  - Input data array to perform FFT analysis
      **           CmplxArray_Input - Empty on input
      **           NumBins          - Number of histogram bins in input array
      ** Outputs : RealArray_Input  - Real part of FFT Analysis
      **           CmplxArray_Input - Complex part of FFT Analysis
      ** Return  : <=0- error; >0 - o.k.
      */
      
      double[] RealArr = new double[FFTBINS+1];                      /** real values array */
      double[] CmplxArr = new double[FFTBINS+1];                     /** complex values array */
      int LocalA;
      int LocalB;
      int LocalC;
      int LocalD;
      int LocalE;
      int LocalX0;
      int LocalX1;
      int LocalX2;
      int LocalX3;
      int LocalY;
      int LocalZ;
      int LocalE1;
      int LocalE2;
      int LocalE4;
      double LocalDblA;
      double LocalDblB;
      double LocalDblA3;
      double LocalDblX1;
      double LocalDblX3;
      double LocalDblY1;
      double LocalDblY3;
      double LocalDblW1;
      double LocalDblW2;
      double LocalDblZ1;
      double LocalDblZ2;
      double LocalDblZ3;
      double LocalDblXt;
      int NumBins=FFTBINS;
      int i;
    
      RealArr[0] = 0.0;
      CmplxArr[0] = 0.0;
      for (i = 1; i <= NumBins; i++ ) {
         RealArr[i] = FFT_Real[i-1];
         CmplxArr[i] = FFT_Complex[i-1];
      }
      LocalA = 2;
      LocalD = 1;
      while (LocalA < NumBins) {
         LocalA = LocalA+LocalA;
         LocalD = LocalD+1;
      }
      LocalE = LocalA;
   
      if (LocalE != NumBins) {
         for (LocalA = NumBins+1; LocalA <= LocalE; LocalA++)  {
            RealArr[LocalA] = 0.0;
            CmplxArr[LocalA] = 0.0; 
         }
      }
    
      LocalE2 = LocalE+LocalE;
      for (LocalC = 1;  LocalC <= LocalD-1; LocalC++ ) {
         LocalE2 = LocalE2 / 2;
         LocalE4 = LocalE2 / 4;
         LocalDblB = 2.0 * PI / LocalE2;
         LocalDblA = 0.0;
         for (LocalB = 1; LocalB<= LocalE4 ; LocalB++) {
            LocalDblA3 = 3.0*LocalDblA;
            LocalDblX1 = Math.cos(LocalDblA);
            LocalDblY1 = Math.sin(LocalDblA);
            LocalDblX3 = Math.cos(LocalDblA3);
            LocalDblY3 = Math.sin(LocalDblA3);
            LocalDblA = ((double)LocalB)*LocalDblB;
            LocalY = LocalB;
            LocalZ = 2*LocalE2;
            while ( LocalY < LocalE ) {
    
               for (LocalX0 = LocalY; LocalX0 <= LocalE-1;
                    LocalX0 = LocalX0 + LocalZ) {
                  LocalX1 = LocalX0 + LocalE4;
                  LocalX2 = LocalX1 + LocalE4;
                  LocalX3 = LocalX2 + LocalE4;
                  LocalDblW1 = RealArr[LocalX0] - RealArr[LocalX2];
   
                  RealArr[LocalX0] = RealArr[LocalX0] + RealArr[LocalX2];
                  LocalDblW2 = RealArr[LocalX1] - RealArr[LocalX3];
                  RealArr[LocalX1] = RealArr[LocalX1] + RealArr[LocalX3];
                  LocalDblZ1 = CmplxArr[LocalX0] - CmplxArr[LocalX2];
                  CmplxArr[LocalX0] = CmplxArr[LocalX0] + CmplxArr[LocalX2];
                  LocalDblZ2 = CmplxArr[LocalX1] - CmplxArr[LocalX3];
                  CmplxArr[LocalX1] = CmplxArr[LocalX1] + CmplxArr[LocalX3];
                  LocalDblZ3 = LocalDblW1 - LocalDblZ2;
                  LocalDblW1 = LocalDblW1 + LocalDblZ2;
                  LocalDblZ2 = LocalDblW2 - LocalDblZ1;
                  LocalDblW2 = LocalDblW2 + LocalDblZ1;
                  RealArr[LocalX2] = LocalDblW1*LocalDblX1 - 
                                       LocalDblZ2*LocalDblY1; 
                  CmplxArr[LocalX2] = -LocalDblZ2*LocalDblX1 - 
                                         LocalDblW1*LocalDblY1;
                  RealArr[LocalX3] = LocalDblZ3*LocalDblX3 + 
                                       LocalDblW2*LocalDblY3;
                  CmplxArr[LocalX3] = LocalDblW2*LocalDblX3 - 
                                        LocalDblZ3*LocalDblY3;
               }
               LocalY = 2*LocalZ - LocalE2 + LocalB;
               LocalZ = 4*LocalZ;
            }
         }
      }
    
       /**
       ---------------------Last stage, length=2 butterfly---------------------
       */
      LocalY = 1;
      LocalZ = 4;
      while ( LocalY < LocalE) {
         for (LocalX0 = LocalY; LocalX0 <= LocalE; LocalX0 = LocalX0 + LocalZ) {
            LocalX1 = LocalX0 + 1; LocalDblW1 = RealArr[LocalX0];
            RealArr[LocalX0] = LocalDblW1 + RealArr[LocalX1];
            RealArr[LocalX1] = LocalDblW1 - RealArr[LocalX1];
            LocalDblW1 = CmplxArr[LocalX0];
            CmplxArr[LocalX0] = LocalDblW1 + CmplxArr[LocalX1];
            CmplxArr[LocalX1] = LocalDblW1 - CmplxArr[LocalX1];
         }
         LocalY = 2*LocalZ - 1;
         LocalZ = 4 * LocalZ;
      }
    
       /**
       c--------------------------Bit reverse counter
       */
      LocalB = 1;
      LocalE1 = LocalE - 1;
      for (LocalA = 1; LocalA <= LocalE1; LocalA++) {
         if (LocalA < LocalB) {
            LocalDblXt = RealArr[LocalB];
            RealArr[LocalB] = RealArr[LocalA];
            RealArr[LocalA] = LocalDblXt;
            LocalDblXt = CmplxArr[LocalB];
            CmplxArr[LocalB] = CmplxArr[LocalA];
            CmplxArr[LocalA] = LocalDblXt;
         }
         LocalC = LocalE / 2;
         while (LocalC < LocalB) {
            LocalB = LocalB - LocalC;
            LocalC = LocalC / 2;
         }
         LocalB = LocalB + LocalC;
      }
    
      /** write Real/CmplxArr back to FFT_Real/Comples arrays */
      for (i = 1; i <= NumBins; i++ ) {
         FFT_Real[i-1] = RealArr[i];
         FFT_Complex[i-1] = CmplxArr[i];
      }

      return LocalE;
    
   }

   private static double complex_abs(double RealValue, double ImaginaryValue) {

      double ComplexAbs_Return=0.0;
      double StorageValue;

      if(RealValue<0.0) {
         RealValue=-RealValue;
      }
      if(ImaginaryValue<0.0) {
         ImaginaryValue=-ImaginaryValue;
      }
      if(ImaginaryValue>RealValue){
         StorageValue=RealValue;
         RealValue=ImaginaryValue;
         ImaginaryValue=StorageValue;
      }

      if((RealValue+ImaginaryValue)==RealValue) {
         ComplexAbs_Return=RealValue;
      } else {
         StorageValue=ImaginaryValue/RealValue;
         StorageValue=RealValue*Math.sqrt(1.0+(StorageValue*StorageValue));
         ComplexAbs_Return=StorageValue;
      }

      return ComplexAbs_Return;

   }

   public static int CalculateFFT(double[] InputArray) {

      int i;
      int FFTValue=-99;
      int HarmonicCounter=0;
      double FFT_BinM2,FFT_BinM1;
      double FFT_TotalAllBins=0.0;
      double Amplitude=0.0;

      for (i = 0; i < FFTBINS; i++ ) {
         FFT_Real[i] = InputArray[i];
         FFT_Complex[i] = 0.0;
         FFT_Magnitude[i] = 0.0;
      }

      int RetErr = ADT_FFT.dfft();
      if(RetErr<=0) {
         /** throw exception */
      } else {
         for (i = 0; i < FFTBINS; i++ ) {
            FFT_Magnitude[i] = ADT_FFT.complex_abs(FFT_Real[i],FFT_Complex[i]);
            /** System.out.printf("arrayinc=%d  FFT real=%f cmplx=%f magnitude=%f\n",i,FFT_Real[i],FFT_Complex[i],FFT_Magnitude[i]); */
         }
         for (i = 2; i <= 31; i++ ) {
            FFT_BinM2 = FFT_Magnitude[i-2];
            FFT_BinM1 = FFT_Magnitude[i-1];
            FFT_TotalAllBins = FFT_TotalAllBins + (FFT_BinM1+FFT_BinM2)/2.0;
            if((FFT_Magnitude[i-1]>FFT_Magnitude[i-2])&&
               (FFT_Magnitude[i-1]>FFT_Magnitude[i])) {
               ++HarmonicCounter;
               /** System.out.printf("i=%d magnitude=%f  counter=%d\n",i,FFT_Magnitude[i],HarmonicCounter); */
            }
         }
         if(FFT_Magnitude[0]==0) {
            /** throw exception */
         } else {
            Amplitude = FFT_TotalAllBins/FFT_Magnitude[0];
            FFTValue=HarmonicCounter;
         }
      }
      
      /** System.out.printf("Amplitude=%f\n",Amplitude); */
      return FFTValue;
   }

}
