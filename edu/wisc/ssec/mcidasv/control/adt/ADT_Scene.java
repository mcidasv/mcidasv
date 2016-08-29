package edu.wisc.ssec.mcidasv.control.adt;

@SuppressWarnings("unused")

public class ADT_Scene {

   private static double[] BDCurve_Points =
   { 30.0,  9.0,-30.0,-42.0,-54.0,
    -64.0,-70.0,-76.0,-80.0,-84.0,-100.0};

   private static double LARGE_EYE_RADIUS = 38.0;
   private static double RING_WIDTH = 4.0;
   private static double MANUAL_EYE_RADIUS = 24.0;
   private static float[][] IRImageLatitudeArrayLocal = new float[200][200];
   private static float[][] IRImageLongitudeArrayLocal = new float[200][200];
   private static float[][] IRImageTemperatureArrayLocal = new float[200][200];
   private static int IRImageXSize;
   private static int IRImageYSize;

   public ADT_Scene() {
      IRImageXSize = -1;
      IRImageYSize = -1;
   }

   public static void DetermineSceneType(boolean RunFullAnalysis)
   {
      /** adt_classify in ADT code */
      int RetErr;
      int XInc,YInc;
      int PreviousHistoryEyeSceneID = -1;
      int PreviousHistoryCloudSceneID = -1;
      double PreviousHistoryTnoValueMinus12hrs = 0.0;
      boolean FoundHistoryRecMinus12hrTF = false;
      double TemperatureValue = -999.0;
      int LogSpiralAmount = 0;
      int LogSpiralAmountMax = 0;
      double LogSpiralLatitude = -999.0;
      double LogSpiralLongitude = -999.0;

      int CloudBDCategory = -99;
      int CloudCWBDCategory = -99;
      int EyeBDCategory = -99;
      int BDCategoryDifference = -99;
      int CloudBDDifference = -99;
      int EyeCloudBDCategoryDifference = -99;
      double TnoInterpValue = -99.9;
      double CloudBDCategoryFloat = -99.9;
      double EyeBDCategoryFloat = -99.9;
      double CloudCWBDCategoryFloat = -99.9;
      double CloudTemperatureDifference = -99.9;
      double EyeCloudTemperatureDifference = -99.9;
      double EyeCloudCWBDCategoryFloatDiff = -99.9;
      double EyeCloudBDCategoryFloatDiff = -99.9;
      double CloudBDCategoryFloatDiff = -99.9;
      double EyeCloudTemperatureDiff2 = -99.9;

      boolean LandFlagTF = ADT_Env.LandFlagTF;
      double InitStrengthValue = ADT_Env.InitRawTValue;
      double RMWSize = ADT_Env.RMWSize;

      int EyeFFTValue = ADT_History.IRCurrentRecord.eyefft;
      int CloudFFTValue = ADT_History.IRCurrentRecord.cloudfft;
      double StormLatitude = ADT_History.IRCurrentRecord.latitude;
      double StormLongitude = ADT_History.IRCurrentRecord.longitude;
      double EyeTemperature = ADT_History.IRCurrentRecord.eyet;
      double EyeStdvValue = ADT_History.IRCurrentRecord.eyestdv;
      double CloudCWTemperature = ADT_History.IRCurrentRecord.cwcloudt;
      double CloudTemperature = ADT_History.IRCurrentRecord.cloudt;
      double CloudSymmetryValue = ADT_History.IRCurrentRecord.cloudsymave;
      double CurvedBandBDMaxLatitude = StormLatitude;
      double CurvedBandBDMaxLongitude = StormLongitude;

      for(XInc=0;XInc<10;XInc++) {
         /** compute cloud category */
         if((CloudTemperature<=BDCurve_Points[XInc])&&
            (CloudTemperature>BDCurve_Points[XInc+1])) {
            CloudBDCategory = XInc;
            TnoInterpValue = (CloudTemperature-BDCurve_Points[CloudBDCategory])/
                           (BDCurve_Points[CloudBDCategory+1]-
                            BDCurve_Points[CloudBDCategory]);
            if(CloudBDCategory==0) {
               TnoInterpValue = 0.0;
            }
            CloudBDCategoryFloat = (double)CloudBDCategory+TnoInterpValue;
         }
         /** compute eye category */
         if((EyeTemperature<=BDCurve_Points[XInc])&&
            (EyeTemperature>BDCurve_Points[XInc+1])) {
            EyeBDCategory = XInc;
            TnoInterpValue = (EyeTemperature-BDCurve_Points[EyeBDCategory])/
                           (BDCurve_Points[EyeBDCategory+1]-
                            BDCurve_Points[EyeBDCategory]);
            if(EyeBDCategory==0) {
               TnoInterpValue = 0.0;
            }
            EyeBDCategoryFloat = (double)EyeBDCategory+TnoInterpValue;
         }
         /** compute C-W eye category */
         if((CloudCWTemperature<=BDCurve_Points[XInc])&&
            (CloudCWTemperature>BDCurve_Points[XInc+1])) {
            CloudCWBDCategory = XInc;
            TnoInterpValue = (CloudCWTemperature-BDCurve_Points[CloudCWBDCategory])/
                           (BDCurve_Points[CloudCWBDCategory+1]-
                            BDCurve_Points[CloudCWBDCategory]);
            if(CloudCWBDCategory==0) {
               TnoInterpValue = 0.0;
            }
            CloudCWBDCategoryFloat = (double)CloudCWBDCategory+TnoInterpValue;
         }
      }
   
      /** 
       ** System.out.printf("EYE = temp=%f cat=%d part=%f \n",EyeTemperature,EyeBDCategory,
       **                                        EyeBDCategoryFloat);
       ** System.out.printf("CLD = temp=%f cat=%d part=%f \n",CloudTemperature,CloudBDCategory,
       **                                        CloudBDCategoryFloat);
       ** System.out.printf("CWT = temp=%f cat=%d part=%f \n",CloudCWTemperature,
       **                                        CloudCWBDCategory,
       **                                        CloudCWBDCategoryFloat);
       */
      BDCategoryDifference = Math.max(0,Math.max(CloudBDCategory,CloudCWBDCategory)-EyeBDCategory);
      CloudTemperatureDifference = CloudTemperature-CloudCWTemperature;
      EyeCloudTemperatureDifference = EyeTemperature-CloudTemperature;
      EyeCloudCWBDCategoryFloatDiff = CloudCWBDCategoryFloat-EyeBDCategoryFloat;
      EyeCloudBDCategoryFloatDiff = CloudBDCategoryFloat-EyeBDCategoryFloat;
      CloudBDCategoryFloatDiff = CloudBDCategoryFloat-CloudCWBDCategoryFloat;
      CloudBDDifference = CloudBDCategory-CloudCWBDCategory;
      EyeCloudBDCategoryDifference = CloudBDCategory-EyeBDCategory;
      EyeCloudTemperatureDiff2 = EyeTemperature-(Math.min(CloudTemperature,CloudCWTemperature));

      /**
       ** System.out.printf("BDCategoryDifference=%d\n",BDCategoryDifference);
       ** System.out.printf("EyeCloudBDCategoryDifference=%d\n",EyeCloudBDCategoryDifference);
       ** System.out.printf("CloudBDDifference=%d\n",CloudBDDifference);
       ** System.out.printf("CloudTemperatureDifference=%f\n",CloudTemperatureDifference);
       ** System.out.printf("EyeCloudTemperatureDifference=%f\n",EyeCloudTemperatureDifference);
       ** System.out.printf("EyeCloudCWBDCategoryFloatDiff=%f\n",EyeCloudCWBDCategoryFloatDiff);
       ** System.out.printf("EyeCloudBDCategoryFloatDiff=%f\n",EyeCloudBDCategoryFloatDiff);
       ** System.out.printf("CloudBDCategoryFloatDiff=%f\n",CloudBDCategoryFloatDiff);
       ** System.out.printf("EyeCloudTemperatureDiff2=%f\n",EyeCloudTemperatureDiff2);
       */
      
      int ImageDate = ADT_History.IRCurrentRecord.date;
      int ImageTime = ADT_History.IRCurrentRecord.time;

      double CurrentTime = ADT_Functions.calctime(ImageDate,ImageTime);
      /** System.out.printf("current time=%f\n",CurrentTime); */

      double CurrentTimeMinus12hr = CurrentTime-0.5;
      boolean FoundEyeSceneTF = false;
      double MaximumRule9Value = -99.0;
      int LastRule9Value = 0;
      double PreviousHistoryTnoValue = MaximumRule9Value;
      double PreviousValidHistoryTnoValue = PreviousHistoryTnoValue;

      int HistoryFileRecords = ADT_History.HistoryNumberOfRecords();

      if((HistoryFileRecords==0)||(!RunFullAnalysis)) {
         FoundHistoryRecMinus12hrTF = true;
         PreviousHistoryEyeSceneID = 3;
         LastRule9Value = 1;
         if((CloudCWBDCategoryFloat<3.5)&&(InitStrengthValue<3.5)) {
            PreviousHistoryCloudSceneID = 3;
            PreviousHistoryTnoValueMinus12hrs = InitStrengthValue;
         } else {
            PreviousHistoryCloudSceneID = 0;
            PreviousHistoryTnoValueMinus12hrs = Math.max(InitStrengthValue,4.0);
         }

      } else {
         FoundHistoryRecMinus12hrTF = false;
         PreviousHistoryCloudSceneID = 3;

         int RecDate,RecTime,RecLand;
         int RecEyeScene,RecCloudScene;
         int RecRule9;
         double LastValidHistoryRecTime=0.0;
         double HistoryRecTime;
         double RecTnoRaw,RecTnoFinal;
         boolean LandCheckTF;
         for(XInc=0;XInc<HistoryFileRecords;XInc++) {
            RecDate = ADT_History.HistoryFile[XInc].date;
            RecTime = ADT_History.HistoryFile[XInc].time;
            RecLand = ADT_History.HistoryFile[XInc].land;
            RecTnoRaw = ADT_History.HistoryFile[XInc].Traw;
            HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
            LandCheckTF = true; 
            if(((LandFlagTF)&&(RecLand==1))||(RecTnoRaw<1.0)) {
               LandCheckTF = false;
            }
            if((HistoryRecTime<CurrentTime)&&(LandCheckTF)) {
               LastValidHistoryRecTime = HistoryRecTime;
               RecTnoFinal = ADT_History.HistoryFile[XInc].Tfinal;
               RecEyeScene = ADT_History.HistoryFile[XInc].eyescene;
               RecCloudScene = ADT_History.HistoryFile[XInc].cloudscene;
               RecRule9 = ADT_History.HistoryFile[XInc].rule9;
               if((HistoryRecTime>=CurrentTimeMinus12hr)&&
                  (!FoundHistoryRecMinus12hrTF)) {
                 PreviousHistoryTnoValueMinus12hrs = RecTnoFinal;
                 FoundHistoryRecMinus12hrTF = true;
               }
               PreviousHistoryTnoValue = RecTnoFinal;
               PreviousHistoryCloudSceneID = RecCloudScene;
               PreviousHistoryEyeSceneID = RecEyeScene;
               if(PreviousHistoryEyeSceneID<=2) {
                  FoundEyeSceneTF = true;
               }
               if((PreviousHistoryCloudSceneID==4)&&
                  (PreviousHistoryEyeSceneID==3)) {
                  FoundEyeSceneTF = false;
               }
               PreviousValidHistoryTnoValue = PreviousHistoryTnoValue;
               LastRule9Value = RecRule9;
               if(PreviousHistoryTnoValue>MaximumRule9Value) {
                  MaximumRule9Value = PreviousHistoryTnoValue;
               }
            } else {
               if(!LandCheckTF) {
                  /** if over land for > 12 hours, turn off FoundEyeSceneTF */
                  if((HistoryRecTime-LastValidHistoryRecTime)>0.5) {
                     FoundEyeSceneTF = false;
                     PreviousHistoryTnoValue = PreviousValidHistoryTnoValue-
                              (1.0*(HistoryRecTime-LastValidHistoryRecTime));
                     /**
                      ** printf("PreviousValidHistoryTnoValue=%f  deltatime=%f
                      **         PreviousHistoryTnoValue=%f\n",
                      **         PreviousValidHistoryTnoValue,
                      **         HistoryRecTime-LastValidHistoryRecTime,
                      **         PreviousHistoryTnoValue);
                      */
                  }
               }
            }
         }
         /** check for large break in history file */
         if(!FoundHistoryRecMinus12hrTF) {
            PreviousHistoryTnoValueMinus12hrs = PreviousHistoryTnoValue;
         }
      }

      /** System.out.printf("FoundHistoryRecMinus12hrTF=%b\n",FoundHistoryRecMinus12hrTF); */
      /** System.out.printf("PreviousHistoryTnoValueMinus12hrs=%f\n",PreviousHistoryTnoValueMinus12hrs); */

      int EyeSceneIDValue;
      double EyeSceneFactorC = 0.0;
      double EyeSceneFactorE = 0.0;
      double EyeSceneFactorA = 1.0-((EyeFFTValue-2)*0.1);
      double EyeSceneFactorB = -(EyeBDCategoryFloat*0.5);

      if(EyeStdvValue>10.0) {
         EyeSceneFactorC = 0.50;
      }
      double EyeSceneFactorD = (EyeCloudBDCategoryFloatDiff*0.25)+
                      (EyeCloudCWBDCategoryFloatDiff*0.50);
      
      /** System.out.printf("EyeSceneFactorD=%f\n",EyeSceneFactorD); */
      /** System.out.printf("MaximumRule9Value=%f EyeSceneFactorE=%f\n",MaximumRule9Value,EyeSceneFactorE); */
      
      if((FoundHistoryRecMinus12hrTF)&&(PreviousHistoryEyeSceneID<3)&&
         (MaximumRule9Value>5.0)) {
         EyeSceneFactorC = EyeSceneFactorC+0.25;    /** changed from EyeSceneFactorE */
      }
      /** System.out.printf("EyeSceneFactorE=%f\n",EyeSceneFactorE); */
      if(PreviousHistoryTnoValueMinus12hrs<=4.5) {
         EyeSceneFactorE = Math.max(-1.0,PreviousHistoryTnoValueMinus12hrs-4.5);
      }
      
      /** System.out.printf("PreviousHistoryTnoValueMinus12hrs=%f  EyeSceneFactorE=%f\n",
                          PreviousHistoryTnoValueMinus12hrs,EyeSceneFactorE); */ 

      if((LastRule9Value>0)&&(PreviousHistoryTnoValue<4.0)) {
         EyeSceneFactorE = EyeSceneFactorE-0.5;
      }
      /** System.out.printf("EyeSceneFactorE=%f\n",EyeSceneFactorE); */
      double EyeFactorTotal = EyeSceneFactorA+EyeSceneFactorB+EyeSceneFactorC+
                              EyeSceneFactorD+EyeSceneFactorE;
      EyeSceneIDValue = 3;                                 /** NO EYE */
      if(EyeFactorTotal>=0.50) {
         EyeSceneIDValue = 0;                              /** EYE */
      }
      
      /** System.out.printf("EyeFactorTotal= %f  EyeSceneIDValue=%d \n",EyeFactorTotal,EyeSceneIDValue); */
      
      double EyeCDOSizeValue = 0.0;
      /** System.out.printf("RMW SIZE=%f\n",RMWSize); */
      if(RMWSize>0.0) {
         /** System.out.printf("Manually Entered RMW Size=%f\n",RMWSize); */
         ADT_History.IRCurrentRecord.rmw = RMWSize;
         EyeCDOSizeValue = RMWSize-1.0; /** manually input eye size */
      } else {
         /** System.out.printf("Calculating RMW Size\n"); */
         double LocalValue[] = ADT_Data.CalcRMW();
         double RadiusMaxWind = LocalValue[0];
         /** System.out.printf("Calculated RMW Size=%f\n",RadiusMaxWind); */ 
         ADT_History.IRCurrentRecord.rmw = RadiusMaxWind;
      }
    
      /* LARGE EYE CHECKS */
      double LargeEyeRadius = LARGE_EYE_RADIUS;
      if((EyeSceneIDValue==0)&&(EyeCDOSizeValue>=LargeEyeRadius)) {
         EyeSceneIDValue = 2;                               /** large eye */
      }

      /** NEW CLOUD SCENE THRESHOLD DETERMINATION */
      boolean ShearSceneTF = false;
      boolean IrregularCDOSceneTF = false;
      boolean CurvedBandSceneTF = true;
      boolean CurvedBandBDGrayShadeTF = true;
      boolean CurvedBandBDBlackWhiteTF = false;
      boolean EmbeddedCenterCheckTF = false;
      boolean EmbeddedCenterSceneTF = false;
    
      double CloudSceneFactorC = 0.0;
      double CloudSceneFactorD = 0.5;
      double CloudSceneFactorE = 0.0;
      double CloudSceneFactorA = CloudCWBDCategoryFloat*0.25;
      double CloudSceneFactorB = CloudBDCategoryFloat*0.25;
      if(CloudFFTValue<=2) {
         CloudSceneFactorC = Math.min(1.50,CloudCWBDCategoryFloat*0.25);
      }
      if(PreviousHistoryCloudSceneID>=3) {
         CloudSceneFactorD = -0.50;
      }
      
      /** System.out.printf("CloudCWBDCategoryFloat=%f PreviousHistoryTnoValueMinus12hrs=%f\n",
               CloudCWBDCategoryFloat,PreviousHistoryTnoValueMinus12hrs); */
      
      if(CloudCWBDCategoryFloat>2.0) {
         if(PreviousHistoryTnoValueMinus12hrs>=2.5) {
            if(EyeSceneIDValue==0) {
               CloudSceneFactorE = Math.min(1.00,PreviousHistoryTnoValueMinus12hrs-2.5);
            }
            if(PreviousHistoryTnoValueMinus12hrs>=3.5) {
               CloudSceneFactorE = CloudSceneFactorE+1.00;
            }
         }
         if((FoundHistoryRecMinus12hrTF)&&
            (FoundEyeSceneTF)) CloudSceneFactorE = CloudSceneFactorE+1.25;
      }
      double CloudFactorTotal = CloudSceneFactorA+CloudSceneFactorB+CloudSceneFactorC+
                                CloudSceneFactorD+CloudSceneFactorE;
      if(CloudFactorTotal<0.0) {
         ShearSceneTF = true;                            /** SHEAR */
      }
      if(CloudFactorTotal>=0.00) {
         CurvedBandSceneTF = true;                       /** CURVED BAND (gray) */
      }
      if(CloudFactorTotal>=1.00) {
         CurvedBandSceneTF = true;                       /** CURVED BAND (gray) */
         /* check for irregular CDO */
         if((EyeCloudTemperatureDiff2<0.0)&&(CloudSymmetryValue>40.0)) {
            IrregularCDOSceneTF = true;                  /** IRREGULAR CDO */
         }
      }
      if((CloudFactorTotal>=2.00)&&(CloudFactorTotal<3.00)) {
         CurvedBandSceneTF = true;                       /** CURVED BAND (gray) */
        /* check for irregular CDO */
         if((EyeCloudTemperatureDiff2<0.0)&&(CloudSymmetryValue>30.0)) {
            IrregularCDOSceneTF = true;                  /** IRREGULAR CDO */
         }
         if(CloudCWBDCategory>=3) {
            /* if xcwt>3.0 try black/white CB check */
            if((CloudBDDifference>0)&&
               (CloudTemperatureDifference<-8.0)) {
               CurvedBandBDGrayShadeTF = false;          /** CURVED BAND (b/w) */
               CurvedBandBDBlackWhiteTF = true;
            }
            /* check for large/ragged eye */
            if((EyeSceneIDValue==0)||
               ((EyeBDCategoryFloat>1.00)&&
                (EyeCloudBDCategoryDifference>=2.00))) {
               CurvedBandSceneTF = false;                /** EYE */
            }
            /* check for CDO */
            if((CloudBDCategoryFloatDiff<=0.0)&&
               (EyeCloudCWBDCategoryFloatDiff<1.00)) {
               CurvedBandSceneTF = false;                /** CDO */
            }
         }
      }
      if(CloudFactorTotal>=3.00) {
         CurvedBandSceneTF=false;                      /** CDO */
         /* check for irregular CDO */
         if((CloudBDDifference<0)&&
            (CloudTemperatureDifference>8.0)&&(CloudSymmetryValue>30.0)) {
            IrregularCDOSceneTF = true;                  /** IRREGULAR CDO */
            CurvedBandSceneTF = true;
         }
      }
      /* EMBEDDED CENTER CHECK */
      if((CloudTemperature<CloudCWTemperature)&&
         (CloudCWTemperature<EyeTemperature)) {
         EmbeddedCenterCheckTF = true;
      }
      if((!CurvedBandSceneTF)&&(EmbeddedCenterCheckTF)) {
         TemperatureValue=BDCurve_Points[CloudCWBDCategory+1]+273.16;
         double ReturnValues[] = ADT_Scene.adt_logspiral(StormLatitude,StormLongitude,TemperatureValue,1);
         LogSpiralAmount = (int)ReturnValues[0];
         LogSpiralLatitude = ReturnValues[1];
         LogSpiralLongitude = ReturnValues[2];
         if((LogSpiralAmount>=8)&&(LogSpiralAmount<20)) {
            EmbeddedCenterSceneTF = true;
         }
        
         /** System.out.printf(" EMBDD : CloudCWBDCategory=%d LogSpiralAmount=%d \n",
                 CloudCWBDCategory,LogSpiralAmount); */
        
      }
      
      /** System.out.printf("CloudFactorTotal= %f  ShearSceneTF=%b CurvedBandSceneTF=%b CurvedBandBDGrayShadeTF=%b IrregularCDOSceneTF=%b \n",
               CloudFactorTotal,ShearSceneTF,CurvedBandSceneTF,CurvedBandBDGrayShadeTF,IrregularCDOSceneTF); */
    
      String ImageDateString = ADT_Functions.adt_julian2cmonth(ImageDate);
       
      /** 
       ** System.out.printf("%9s %6d %4.1f %4.1f %2d %2d %5.1f %5.1f  %5.2f %5.2f %5.2f %5.2f %5.2f %5.2f %5.2f  %2d %2d %4.1f %4.1f  %5.2f %5.2f %5.2f %5.2f %5.2f %5.2f   %6.2f %7.2f %3.1f  \n",ImageDateString,ImageTime,
       ** CloudBDCategoryFloat,CloudCWBDCategoryFloat,CloudFFTValue,
       ** CloudBDDifference,CloudTemperatureDifference,CloudSymmetryValue,
       ** CloudSceneFactorA,CloudSceneFactorB,0.0,CloudSceneFactorC,
       ** CloudSceneFactorD,CloudSceneFactorE,CloudFactorTotal,
       ** EyeFFTValue,EyeCloudBDCategoryDifference,EyeBDCategoryFloat,EyeStdvValue,
       ** EyeSceneFactorA,EyeSceneFactorB,EyeSceneFactorC,EyeSceneFactorD,
       ** EyeSceneFactorE,EyeFactorTotal,StormLatitude,StormLongitude,
       ** PreviousHistoryTnoValue);
       */

      /** CLASSIFY CLOUD REGION */
      int CurvedBandBDCategory = 0;
      int CurvedBandBDAmount = 0;
      int CurvedBandBDMaxAmount = 0;
      int CloudSceneIDValue = -99;
      boolean FoundCurvedBandSceneTF = false;
      double ShearDistance = -99.0;
    
      /** System.out.printf("CurvedbandsceneTF=%b  IrregularCDOTF=%b  ShearSceneTF=%b \n",CurvedBandSceneTF,IrregularCDOSceneTF,ShearSceneTF); */
      if(CurvedBandSceneTF) {
         if(ShearSceneTF) {
            EyeSceneIDValue = 3;                         /** NO EYE */
            CloudSceneIDValue = 4;                       /** SHEAR */
            TemperatureValue = ((BDCurve_Points[2]+BDCurve_Points[3])/2.0)+273.16;
            ShearDistance = adt_cdoshearcalc(StormLatitude,StormLongitude,TemperatureValue,3);
            EyeCDOSizeValue = Math.max(4.0,ShearDistance);
         }
         else if(IrregularCDOSceneTF) {
            EyeSceneIDValue = 3;                         /** NO EYE */
            CloudSceneIDValue = 2;                       /** IRREGULAR CDO */
         }
         else {
            FoundCurvedBandSceneTF = false;
            if(CurvedBandBDGrayShadeTF) {
               /** perform Curved Band analysis */
               XInc = 4;  /** start with LIGHT GRAY */
               while((XInc>=2)&&(!FoundCurvedBandSceneTF)) {
                  TemperatureValue = BDCurve_Points[XInc]+273.16;
                  double ReturnValues[] = ADT_Scene.adt_logspiral(StormLatitude,StormLongitude,TemperatureValue,1);
                  LogSpiralAmount = (int)ReturnValues[0];
                  LogSpiralLatitude = ReturnValues[1];
                  LogSpiralLongitude = ReturnValues[2];
                  if((LogSpiralAmount>=8)||(XInc==2)) {
                     /** 10 = .375% -- 10 ==> 9 arcs of 15 degrees */
                     if(LogSpiralAmount>25) {
                        if(XInc==4) {
                          CurvedBandBDGrayShadeTF = false;
                          CurvedBandBDBlackWhiteTF = true;
                          /** following line to exit out of While statement */
                          FoundCurvedBandSceneTF = true;
                        }
                        else {
                          XInc = 0;
                        }
                     }
                     else {
                      if((XInc==2)&&(LogSpiralAmount<7)) {
                          /** 7 = .25% -- 10 ==> 6 arcs of 15 degrees */
                          /** probably shear */
                          FoundCurvedBandSceneTF = false;
                          CurvedBandBDBlackWhiteTF = false;
                          ShearSceneTF = true;
                          /** gross error check... added 08/15/13 */
                          if((EyeBDCategoryFloat>1.5)||(CloudBDCategoryFloat>2.5)) {
                            ShearSceneTF = false;
                            IrregularCDOSceneTF = true;
                          }
                          /** following line to exit out of While statement */
                          XInc--;
                       }
                       else {
                          FoundCurvedBandSceneTF = true;
                       }
                     }
                  }
                  else {
                     XInc--;
                  }
               }
            }
            if(CurvedBandBDBlackWhiteTF) {
               /** try BLACK and WHITE rings */
               FoundCurvedBandSceneTF = false;
               CurvedBandSceneTF = false;
               XInc = 6;
               while((XInc>4)&&(!FoundCurvedBandSceneTF)) {
                  TemperatureValue = BDCurve_Points[XInc]+273.16;
                  double ReturnValues[] = ADT_Scene.adt_logspiral(StormLatitude,StormLongitude,TemperatureValue,1);
                  LogSpiralAmount = (int)ReturnValues[0];
                  LogSpiralLatitude = ReturnValues[1];
                  LogSpiralLongitude = ReturnValues[2];
                  if((LogSpiralAmount>=9)&&(LogSpiralAmount<=25)) {
                     FoundCurvedBandSceneTF = true;
                     /** EmbeddedCenterSceneTF = true;   needed here? */
                  }
                  else {
                     XInc--;
                  }
               }
            }
            if(FoundCurvedBandSceneTF) {
               /** found curved band scenes */
               CurvedBandBDCategory = XInc;
               CurvedBandBDAmount = LogSpiralAmount;
               EyeSceneIDValue = 3;                      /** NO EYE */
               CloudSceneIDValue = 3;                    /** CURVED BAND */
               /** search for max curved band analysis location w/in 1-degree box */
               TemperatureValue = BDCurve_Points[CurvedBandBDCategory]+273.16;
               boolean CBSearchTF_Global = true;
               if(CBSearchTF_Global) {   /** need global variable here */
                  double ReturnValues[] = ADT_Scene.adt_logspiral(StormLatitude,StormLongitude,TemperatureValue,2);
                  CurvedBandBDMaxAmount = (int)ReturnValues[0];
                  CurvedBandBDMaxLatitude = ReturnValues[1];
                  CurvedBandBDMaxLongitude = ReturnValues[2];
                  /** System.out.printf("max amounts %d %f %f\n",CurvedBandBDMaxAmount,CurvedBandBDMaxLatitude,CurvedBandBDMaxLongitude); */
               }
            }
            else {
              /** did not find curved band scenes, mark as non-eye/eye scene */
              CloudSceneIDValue = 0;
              CurvedBandSceneTF = false;
              EmbeddedCenterSceneTF = false;
            }
         }
      }
      /** System.out.printf("CurvedbandsceneTF=%b  IrregularCDOTF=%b  ShearSceneTF=%b EmbeddedCenterTF=%b \n", 
          CurvedBandSceneTF,IrregularCDOSceneTF,ShearSceneTF,EmbeddedCenterSceneTF); */
      if(!CurvedBandSceneTF) {
         if(ShearSceneTF) {
            /** shear scene */
            EyeSceneIDValue = 3;                         /** NO EYE */
            CloudSceneIDValue = 4;                       /** SHEAR */
            TemperatureValue = ((BDCurve_Points[2]+BDCurve_Points[3])/2.0)+273.16;
            ShearDistance = adt_cdoshearcalc(StormLatitude,StormLongitude,TemperatureValue,3);
            EyeCDOSizeValue = Math.max(4.0,ShearDistance);
         }
         else {
            CloudSceneIDValue = 0;                          /** UNIFORM */
            if(EmbeddedCenterSceneTF) {
               CloudSceneIDValue = 1;                       /** EMBEDDED CENTER */
            }
            /** added 08/15/13 */
            if(IrregularCDOSceneTF) {
               CloudSceneIDValue = 2;                       /** Irregular CDO */
            }
            /** PINHOLE EYE TEST */
             /**
              ** System.out.printf("EyeFactorTotal=%f\n",
              **      "EyeSceneIDValue=%d EyeCloudBDCategoryDifference=%d ",
              **      "EyeFFTValue=%d CloudCWBDCategoryFloat=%f \n",
              **      "CloudSceneIDValue=%d CloudFFTValue=%d ",
              **      "PreviousHistoryTnoValueMinus12hrs=%f\n",EyeFactorTotal,
              **       EyeSceneIDValue,EyeCloudBDCategoryDifference,EyeFFTValue,
              **       CloudCWBDCategoryFloat,CloudSceneIDValue,CloudFFTValue,
              **       PreviousHistoryTnoValueMinus12hrs);
              */
            if((RMWSize>0.0)&&(RMWSize<12.0)) {
               EyeSceneIDValue = 1;                         /** PINHOLE EYE CHECK */
            }
            if((EyeFactorTotal>-0.25)&&(EyeFactorTotal<1.50)&&
               (EyeCloudBDCategoryDifference>=2)&&(EyeFFTValue<=2)&&
               (CloudCWBDCategoryFloat>6.0)&&(CloudSceneIDValue<=1)&&
               (CloudFFTValue<=4)&&(PreviousHistoryTnoValueMinus12hrs>=3.5)) {
               EyeSceneIDValue = 1;                         /** PINHOLE EYE CHECK */
            }
         }
      }
      double CDOSize = -999.0;
      /** System.out.printf("cloudsceneID=%d eyesceneID=%d\n",CloudSceneIDValue,EyeSceneIDValue); */
      if((CloudSceneIDValue<=2)&&(EyeSceneIDValue==3)) {
         /** for CDO TESTS */
         for(XInc=2;XInc<=6;XInc++) {  /** DG,MG,LG,B,W */
            TemperatureValue = BDCurve_Points[XInc]+273.16;
            /** System.out.printf("LatitudeValue=%f LongitudeValue=%f TemperatureValue=%f\n",StormLatitude,StormLongitude,TemperatureValue); */
            CDOSize = adt_cdoshearcalc(StormLatitude,StormLongitude,TemperatureValue,1);
            /** System.out.printf("CDO : XInc=%d  CDOSize=%f  CDOSize/111=%f  \n",XInc,CDOSize,CDOSize/111.0); */
            if(XInc==2) {
               EyeCDOSizeValue = CDOSize;
            }
         }
      }
    
      /** System.out.printf("eyescene=%d cloudscene=%d eyecdosize=%f ringcb=%d ringcbval=%d\n",
                 EyeSceneIDValue,CloudSceneIDValue,EyeCDOSizeValue,CurvedBandBDCategory,CurvedBandBDAmount); */ 
      /** System.out.printf("CBMAX :ringcbval=%d ringcb=%d ringcbval=%d\n",CurvedBandBDMaxAmount,CurvedBandBDCategory,CurvedBandBDAmount); */

      /** System.out.printf("EyeScene=%d CloudScene=%d EyeCDOSize=%f \n",EyeSceneIDValue,CloudSceneIDValue,EyeCDOSizeValue); */
      ADT_History.IRCurrentRecord.eyescene = EyeSceneIDValue;
      ADT_History.IRCurrentRecord.cloudscene = CloudSceneIDValue;
      ADT_History.IRCurrentRecord.eyesceneold = -1;
      ADT_History.IRCurrentRecord.cloudsceneold = -1;
      ADT_History.IRCurrentRecord.eyecdosize = EyeCDOSizeValue;
      ADT_History.IRCurrentRecord.ringcb = CurvedBandBDCategory;
      ADT_History.IRCurrentRecord.ringcbval = CurvedBandBDAmount;
      ADT_History.IRCurrentRecord.ringcbvalmax = CurvedBandBDMaxAmount;
      ADT_History.IRCurrentRecord.ringcbvalmaxlat = CurvedBandBDMaxLatitude;
      ADT_History.IRCurrentRecord.ringcbvalmaxlon = CurvedBandBDMaxLongitude;
      ADT_History.IRCurrentRecord.mwscore = ADT_Env.MWScore;
      ADT_History.IRCurrentRecord.mwdate = ADT_Env.MWJulianDate;
      ADT_History.IRCurrentRecord.mwtime = ADT_Env.MWHHMMSSTime;
      
   }

   public static double[] adt_logspiral(double InputLatitude, double InputLongitude, 
                                    double TemperatureThreshold, int AnalysisTypeIDValue)
   /**
    ** Determine storm location using 10^ Log-spiral analysis.
    ** Algorithm will attempt to match the spiral with the image
    ** pixels at or below the threshold temperature based on
    ** BD-enhancement curve values
    ** Inputs  : InputLatitude        - center latitude of analysis grid
    **           InputLongitude       - center longitude of analysis grid
    **           TemperatureThreshold - temperature threshold value
    **           AnalysisTypeIDValue  - 1=search at single point
    **                                  2=search over 2^box
    ** Outputs : SpiralArcLatitude    - best latitude location from analysis
    **           SpiralArcLongitude   - best longitude location from analysis
    **           SpiralArcDistance    - number of consecutive arcs through
    **                                       which spiral passes
    */
   {

      int XInc,YInc,ZInc;
      int SpiralArcDistance=-99;
      int SearchLatitudeMaxInteger,SearchLatitudeMinInteger;
      int SearchLongitudeMaxInteger,SearchLongitudeMinInteger;
      double SearchLatitudeMaximum,SearchLatitudeMinimum;
      double SearchLongitudeMaximum,SearchLongitudeMinimum;
      double SpiralArcLatitude=-999.99;
      double SpiralArcLongitude=-999.99;
      float ValidPixelLatitudeArray[] = new float[40000];
      float ValidPixelLongitudeArray[] = new float[40000];
      float ValidPixelTemperatureArray[] = new float[40000];

      double ImageResolution = ADT_Data.GetCurrentImageResolution();
      double DistanceDifferenceMaximumKM=ImageResolution+(ImageResolution/2.0);
      int IncAddVal=(ImageResolution>RING_WIDTH) ? 1 : ((int)(RING_WIDTH-ImageResolution+1.0));

      if(AnalysisTypeIDValue==2) {
         /* search over 2.0 degree box */
         SearchLatitudeMaximum = InputLatitude+1.0;
         SearchLatitudeMinimum = InputLatitude-1.0;
         SearchLongitudeMaximum = InputLongitude+1.0;
         SearchLongitudeMinimum = InputLongitude-1.0;
         SearchLatitudeMaxInteger = (int)(SearchLatitudeMaximum*100.0);
         SearchLatitudeMinInteger = (int)(SearchLatitudeMinimum*100.0);
         SearchLongitudeMaxInteger = (int)(SearchLongitudeMaximum*100.0);
         SearchLongitudeMinInteger = (int)(SearchLongitudeMinimum*100.0);
      } else {
         /* search at a single point */
         SearchLatitudeMaxInteger = (int)(InputLatitude*100.0);
         SearchLatitudeMinInteger = (int)(InputLatitude*100.0);
         SearchLongitudeMaxInteger = (int)(InputLongitude*100.0);
         SearchLongitudeMinInteger = (int)(InputLongitude*100.0);
      }
      
      /** allocate memory, if necessary */
      if(IRImageXSize==-1) {
         IRImageXSize = ADT_Data.GetCurrentImageXSize();
         IRImageYSize = ADT_Data.GetCurrentImageYSize();
         IRImageLatitudeArrayLocal = ADT_Data.GetCurrentImageLatitudeArray();
         IRImageLongitudeArrayLocal = ADT_Data.GetCurrentImageLongitudeArray();
         IRImageTemperatureArrayLocal = ADT_Data.GetCurrentImageTemperatureArray();
      }

      /** System.out.printf("temperature threshold=%f\n",TemperatureThreshold); */
      /** initialize arrays */
      int ValidPointCounter = 0;
      for(YInc=0;YInc<IRImageYSize;YInc=YInc+IncAddVal) {
         for(XInc=0;XInc<IRImageXSize;XInc=XInc+IncAddVal) {
            if(IRImageTemperatureArrayLocal[YInc][XInc]<=TemperatureThreshold) {
               ValidPixelLatitudeArray[ValidPointCounter] = IRImageLatitudeArrayLocal[YInc][XInc];
               ValidPixelLongitudeArray[ValidPointCounter] = IRImageLongitudeArrayLocal[YInc][XInc];
               ValidPixelTemperatureArray[ValidPointCounter] = IRImageTemperatureArrayLocal[YInc][XInc];
               ValidPointCounter++;
            }
         }
      }
      /** System.out.printf("Valid Point Counter=%d\n",ValidPointCounter); */

      int ArcSkipCounter;
      int ThresholdCounter;
      int SpiralConsecutiveArcCounter;
      int SpiralConsecutiveArcMaximum;
      double SpiralStartMinimumDistance;
      double SearchIncrementLatitude=0.0;
      double SearchIncrementLongitude=0.0;
      double SpiralArcBestRotationAngleValue=0.0;
      double ArcAngleTheta_Radians;
      double RadialDistanceKM;
      double FinalArcAngleTheta,FinalArcAngleThetaPlus180;
      double RadiansValue=57.29578;          /** degree to radians conversion value*/
      double SpiralConstantAValue=25.0;      /** 10^ log spiral distance constant */
      double SpiralConstantBValue=10.0/RadiansValue; /** 10^ log spiral increase */
      double LatitudeDifference;
      double LongitudeDifference;

      for(XInc=SearchLatitudeMinInteger;XInc<=SearchLatitudeMaxInteger;XInc=XInc+20) {
         SearchIncrementLatitude = (double)XInc/100.0;
         /** loop through y-axis/lines of analysis grid box */
         for(YInc=SearchLongitudeMinInteger;YInc<=SearchLongitudeMaxInteger;YInc=YInc+20) {
            SearchIncrementLongitude = (double)YInc/100.0;
            ArcSkipCounter = 0;
            /** determine distance from each point in box to current location */
            if(AnalysisTypeIDValue==2) {
               SpiralStartMinimumDistance = 12.0;
               for(ZInc=0;ZInc<ValidPointCounter;ZInc++) {
                  double LocalValue[] = ADT_Functions.distance_angle(SearchIncrementLatitude,SearchIncrementLongitude,
                                                                     ValidPixelLatitudeArray[ZInc], 
                                                                     ValidPixelLongitudeArray[ZInc],1);
                  double DistanceValue = LocalValue[0];
                  if(DistanceValue<=SpiralStartMinimumDistance) {
                    /**
                     ** if the lat/lon point is too close to cold cloud tops, do
                     ** not calculate log spiral at this point.  Trying to eliminate
                     ** "false" arc locations by forcing the system to use some
                     ** of the arc points on the spiral away from the start of
                     ** the spiral (were getting "false echos" without this".
                     */
                    ArcSkipCounter = 1;
                    break;
                  }
               }
            }
    
            int SpiralArcMaximumValue = 0;
            int SpiralArcMaximumValueRotationFactor = 0;
            /**
             ** if arc location passes analysis above,
             ** proceed with placement of spiral
             */

            if(ArcSkipCounter==0) {
               /** rotate the arc spiral thru entire revolution at 30^ interval */
               for(int RotationFactor=0;RotationFactor<=330;RotationFactor=RotationFactor+30) {
                  SpiralConsecutiveArcCounter = 0;
                  SpiralConsecutiveArcMaximum = 0;
    
                 /** calculate position of each point on spiral from 0 to 540^ */
                  for(int ArcAngleTheta=0;ArcAngleTheta<=540;ArcAngleTheta=ArcAngleTheta+15) {
                     ArcAngleTheta_Radians = (double)ArcAngleTheta/RadiansValue;
                     RadialDistanceKM = SpiralConstantAValue*Math.exp((SpiralConstantBValue*ArcAngleTheta_Radians));
                     FinalArcAngleTheta = (double)ArcAngleTheta+(double)RotationFactor;
                     if(SearchIncrementLatitude<0.0) {
                        FinalArcAngleTheta = (double)(-1*ArcAngleTheta)+(double)RotationFactor;
                     }
                     FinalArcAngleThetaPlus180 = FinalArcAngleTheta+180.0;
                     double LocalValue2[] = ADT_Functions.distance_angle2(SearchIncrementLatitude,SearchIncrementLongitude,
                                                                          RadialDistanceKM,FinalArcAngleThetaPlus180);
                     double SearchGuessLatitude = LocalValue2[0];
                     double SearchGuessLongitude = LocalValue2[1];
                     ThresholdCounter = 0;
                     for(ZInc=0;ZInc<ValidPointCounter;ZInc++) {
                        LatitudeDifference = Math.abs(SearchGuessLatitude-
                                                 ValidPixelLatitudeArray[ZInc]);
                        LongitudeDifference = Math.abs(SearchGuessLongitude-
                                                  ValidPixelLongitudeArray[ZInc]);
                        /**
                         ** if a point is within 0.1^ latitude/longitude
                         ** determine distance
                         */
                        if((LatitudeDifference<=0.1)&&(LongitudeDifference<=0.1)) {
                            double LocalValue3[] = ADT_Functions.distance_angle(SearchGuessLatitude,SearchGuessLongitude,
                                                                               ValidPixelLatitudeArray[ZInc], 
                                                                               ValidPixelLongitudeArray[ZInc],1);
                            double DistanceValue3 = LocalValue3[0];
                           /**
                            ** if distance from spiral point is within 6km from an
                            ** accepted temperature threshold point, count it
                            */
                           if(DistanceValue3<=DistanceDifferenceMaximumKM) {
                              ThresholdCounter++;
                           }
                        }
                     }
                     /**
                      ** if there are 4 or more threshold points associated
                      ** with each spiral point, count within consecutive
                      ** spiral point counter
                      */
                     if(ThresholdCounter>=4) {
                        SpiralConsecutiveArcCounter++;
                        /**
                         ** save spiral that has maximum consecutive spiral counts
                         ** for each rotation though 360^ at each center location
                         */
                        if(SpiralConsecutiveArcCounter>SpiralConsecutiveArcMaximum) {
                           SpiralConsecutiveArcMaximum = SpiralConsecutiveArcCounter;
                        }
                     } else {
                        SpiralConsecutiveArcCounter = 0;
                     }
                     /**
                      ** if this spiral has the greatest number of consecutive
                      ** spiral points, save the location and number of points
                      */
                     if(SpiralConsecutiveArcMaximum>SpiralArcMaximumValue) {
                        SpiralArcMaximumValue = SpiralConsecutiveArcMaximum;
                        SpiralArcMaximumValueRotationFactor = RotationFactor;
                     }
                  }
               } /** RotationFactor loop */
               if(SpiralArcMaximumValue>SpiralArcDistance) {
                  SpiralArcDistance = SpiralArcMaximumValue;
                  SpiralArcLatitude = SearchIncrementLatitude;
                  SpiralArcLongitude = SearchIncrementLongitude;
                  SpiralArcBestRotationAngleValue = SpiralArcMaximumValueRotationFactor;
               }
            } /** ArcSkipCounter if */
         } /** YInc loop */
      } /** XInc loop */
    
      /** load array for best spiral band */
      int TemporaryCounter=0;
      for(int ArcAngleTheta=0;ArcAngleTheta<=540;ArcAngleTheta=ArcAngleTheta+15) {
         ArcAngleTheta_Radians=(double)ArcAngleTheta/RadiansValue;
         RadialDistanceKM=SpiralConstantAValue*Math.exp((SpiralConstantBValue*ArcAngleTheta_Radians));
         FinalArcAngleTheta=(double)ArcAngleTheta+(double)SpiralArcBestRotationAngleValue;
         if(SearchIncrementLatitude<0.0) {
            FinalArcAngleTheta=(double)(-1*ArcAngleTheta)+(double)SpiralArcBestRotationAngleValue;
         }
         FinalArcAngleThetaPlus180=FinalArcAngleTheta+180.0;
         double LocalValue4[] = ADT_Functions.distance_angle2(SpiralArcLatitude,SpiralArcLongitude,
                                                              RadialDistanceKM,FinalArcAngleThetaPlus180);
         double SearchGuessLatitude = LocalValue4[0];
         double SearchGuessLongitude = LocalValue4[1];
         /** load array for external plotting of spiral band */
         /** SpiralBandPoints_Global[0][TemporaryCounter]=SearchGuessLatitude; */
         /**SpiralBandPoints_Global[1][TemporaryCounter]=SearchGuessLongitude; */
         TemporaryCounter++;
      }

      return new double[] { (double)SpiralArcDistance, SpiralArcLatitude, SpiralArcLongitude };

   }

   public static double adt_cdoshearcalc(double InputLatitude, double InputLongitude, 
                                          double TemperatureThreshold, int AnalysisTypeIDValue)
   /**
    ** Determine eye size or shear distance for a given scene.
    ** Inputs  : LatitudeValue       - center latitude of analysis grid
    **           LongitudeValue      - center longitude of analysis grid
    **           TemperatureValue    - temperature threshold value to be used
    **           AnalysisTypeIDValue - analysis type
    **                                 (1-cdo size,2-eye size,3-shear distance)
    ** Outputs : RadiusOrShear       - eye/cdo radius or shear distance
    */
   {
      double ANGLEMAXDIFFERENCE=15.0;        /** maximum distance constant value */
      double RadiusOrShear=-99.0;
      double Value3=MANUAL_EYE_RADIUS+RING_WIDTH;
      double PixelLatitudeArray[] = new double[40000];
      double PixelLongitudeArray[] = new double[40000];
      double PixelTemperatureArray[] = new double[40000];

      int XInc,YInc,ZInc;
      int PointCounter=0;
      int ValidRadiiCounter=4;
      double DistanceValue,AngleValue;
      double SymmValue;
      
      double RadiiLength1=300.0;
      double RadiiLength2=300.0;
      double RadiiLength3=300.0;
      double RadiiLength4=300.0;
      double MaxDistanceValue=0.0;

      /** allocate memory, if necessary */
      if(IRImageXSize==-1) {
         IRImageXSize = ADT_Data.GetCurrentImageXSize();
         IRImageYSize = ADT_Data.GetCurrentImageYSize();
         IRImageLatitudeArrayLocal = ADT_Data.GetCurrentImageLatitudeArray();
         IRImageLongitudeArrayLocal = ADT_Data.GetCurrentImageLongitudeArray();
         IRImageTemperatureArrayLocal = ADT_Data.GetCurrentImageTemperatureArray();
      }

      if(AnalysisTypeIDValue==1) {
         /** CDO size determination - RETURNS RADIUS */
         for(YInc=0;YInc<IRImageYSize;YInc++) {
            for(XInc=0;XInc<IRImageXSize;XInc++) {
               if(IRImageTemperatureArrayLocal[YInc][XInc]>TemperatureThreshold) {
                  PixelLatitudeArray[PointCounter] = IRImageLatitudeArrayLocal[YInc][XInc];
                  PixelLongitudeArray[PointCounter] = IRImageLongitudeArrayLocal[YInc][XInc];
                  PixelTemperatureArray[PointCounter] = IRImageTemperatureArrayLocal[YInc][XInc];
                  PointCounter++;
               }
            }
         }
        
         /** System.out.printf("PointCounter=%d  numx*numy=%d\n",PointCounter,IRImageYSize*IRImageXSize); */
         if(PointCounter<(IRImageYSize*IRImageXSize)) {
            for(ZInc=0;ZInc<PointCounter;ZInc++) {
               double LocalValue[] = ADT_Functions.distance_angle(InputLatitude,InputLongitude,
                                                                  PixelLatitudeArray[ZInc],PixelLongitudeArray[ZInc],1);
               DistanceValue = LocalValue[0];
               AngleValue = LocalValue[1];
               if(DistanceValue>MaxDistanceValue) MaxDistanceValue=DistanceValue;
               /** determine size of CDO */
               if(DistanceValue>MANUAL_EYE_RADIUS) {
                  if((Math.abs(AngleValue-45.0)<=ANGLEMAXDIFFERENCE)&&
                     (DistanceValue<RadiiLength1)) {
                     RadiiLength1=DistanceValue;
                  }
                  if((Math.abs(AngleValue-135.0)<=ANGLEMAXDIFFERENCE)&&
                     (DistanceValue<RadiiLength2)) {
                     RadiiLength2=DistanceValue;
                  }
                  if((Math.abs(AngleValue-225.0)<=ANGLEMAXDIFFERENCE)&&
                     (DistanceValue<RadiiLength3)) {
                     RadiiLength3=DistanceValue;
                  }
                  if((Math.abs(AngleValue-315.0)<=ANGLEMAXDIFFERENCE)&&
                     (DistanceValue<RadiiLength4)) {
                     RadiiLength4=DistanceValue;
                  }
               }
            }
            
            /** System.out.printf("RadiiLength1=%f RadiiLength2=%fRadiiLength3=%f RadiiLength4=%f\n",
                     RadiiLength1,RadiiLength2,RadiiLength3,RadiiLength4); */
            
            if(RadiiLength1<Value3) {
               ValidRadiiCounter--;
            }
            if(RadiiLength2<Value3) {
               ValidRadiiCounter--;
            }
            if(RadiiLength3<Value3) {
               ValidRadiiCounter--;
            }
            if(RadiiLength4<Value3) {
               ValidRadiiCounter--;
            }
         }
         else {
            RadiiLength1=0.0;
            RadiiLength2=0.0;
            RadiiLength3=0.0;
            RadiiLength4=0.0;
         }
         if(ValidRadiiCounter<3) {
            RadiusOrShear=0.0;
         }
         else {
            RadiiLength1=Math.min(RadiiLength1,MaxDistanceValue);
            RadiiLength2=Math.min(RadiiLength2,MaxDistanceValue);
            RadiiLength3=Math.min(RadiiLength3,MaxDistanceValue);
            RadiiLength4=Math.min(RadiiLength4,MaxDistanceValue);
            RadiusOrShear=(RadiiLength1+RadiiLength2+RadiiLength3+RadiiLength4)/4.0;
            double Value1=RadiiLength1+RadiiLength3;
            double Value2=RadiiLength2+RadiiLength4;
            SymmValue=Value1/Value2;
            SymmValue=Math.max(SymmValue,1.0/SymmValue);
         }
         /** System.out.printf("\nPointCounter=%5d RadiiLength1=%5.1f RadiiLength2=%5.1f RadiiLength3=%5.1f RadiiLength4=%5.1f\n",
                   PointCounter, RadiiLength1,RadiiLength2,RadiiLength3,RadiiLength4); */
      }
    
      if(AnalysisTypeIDValue==3) {
         double ShearDistanceValue=-99.0;
         /**  need to implement entire odtauto.c library including remapping 
         RetErr=adt_shearbw(TemperatureValue,LatitudeValue,LongitudeValue,
                             &ShearDistanceValue);
         */
         RadiusOrShear=ShearDistanceValue;
      }

      return RadiusOrShear;
   }
}
