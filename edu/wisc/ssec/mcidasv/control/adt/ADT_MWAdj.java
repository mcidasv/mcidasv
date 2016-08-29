package edu.wisc.ssec.mcidasv.control.adt;

import java.lang.Math;
import java.lang.String;

@SuppressWarnings("unused")

public class ADT_MWAdj {

   public ADT_MWAdj() {
   }

   public static double[] adt_calcmwadjustment(double MWScoreInput, 
                                               double IntensityEstimateValueInput) {

      int MWScoreApplicationFlag = 0;
      int Rule9Flag = 0;
      int XInc;
      int NumRecsHistory = ADT_History.HistoryNumberOfRecords();
      int RecDate,RecTime,RecLand;
      int RecMWDate,RecMWTime;
      int Rule8Flag = 0;;
      int LastCloudSceneValue;
      int LastRule8Flag = 0;;
      int NonEIRSceneCounter = 0;
      int LastEyeSceneValue = 0;
      double IntensityEstimateValueReturn = IntensityEstimateValueInput;
      double InputIntensityEstStore = IntensityEstimateValueInput;
      double MWScoreThresholdValue1 = 20.0;
      double MWScoreThresholdValue2 = 60.0;
      double MWIntensityTnoValue1 = 4.3;
      double MWIntensityTnoValue1a = 4.8;
      double MWIntensityTnoValue2 = 5.0;
      double PreviousHistoryTnoValue = 0.0;
      double RecMWScore;
      double Traw;
      double PreviousMWScoreValue;
      double HistoryRecTime = 0.0;
      double LastCIValue = 1.0;
      double FirstMWHoldTime = 0.0;
      double LastValidMWCIValue = 0.0;
      double LastMWScoreValue = 0.0;
      double FirstHistoryCIValue = 0.0;
      double FirstMWadjTime = 0.0;
      double FirstMWadjTimePlus12hr = 0.0;
      double LastValidMWadjTime = 0.0;
      double LastValidMWadjTimePlus12 = 0.0;
      double LastValidMWadjTimePlus6 = 0.0;
      double HistoryCIValue = 0.0;
      double HistoryCIValueMinus12hr= 0.0;
      double HistoryRecTimeMinus12hr = 0.0;
      double InterpTimePart,IntensityCIMergeValue,NewIntensityEstScore;
      boolean LandCheckTF = true;
      boolean FirstMWHoldTF = true;
      boolean EXIT_Routine = false;
      boolean MWAdjOFFTF = false;
      boolean FoundHistoryRecPrev12hrTF = false;
      boolean First31Record = true;
      boolean MWAdjCurrentONTF = false;
      boolean NORecordAfterMWTimeTF = true;
      String CommentString = "";

      boolean LandFlagTF = ADT_Env.LandFlagTF;

      int CurDate = ADT_History.IRCurrentRecord.date;
      int CurTime = ADT_History.IRCurrentRecord.time;
      int CurCloudScene = ADT_History.IRCurrentRecord.cloudscene;
      int CurEyeScene = ADT_History.IRCurrentRecord.eyescene;
      int CurLand = ADT_History.IRCurrentRecord.land;
      double CurrentTime = ADT_Functions.calctime(CurDate,CurTime);

      System.out.printf("INPUT current mw env score=%f date=%d time=%d\n",MWScoreInput,ADT_Env.MWJulianDate,ADT_Env.MWHHMMSSTime );
      /** check for valid MW eye score value (not equal to -920) */
      if(MWScoreInput<-900.0) {
         if(ADT_Env.DEBUG==100) {
            System.out.printf("MW SCORE=-920 ... will restore to previous valid record");
         }
         XInc = 0;
         while(XInc<NumRecsHistory) {
            RecDate = ADT_History.HistoryFile[XInc].date;
            RecTime = ADT_History.HistoryFile[XInc].time;
            HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);

            RecLand = ADT_History.HistoryFile[XInc].land;
            Traw = ADT_History.HistoryFile[XInc].Traw;
            LandCheckTF = true;
            if(((LandFlagTF)&&(RecLand==1))||(Traw<1.0)) {
               LandCheckTF = false;
            }
            if(LandCheckTF) {
               RecMWDate = ADT_History.HistoryFile[XInc].mwdate;
               RecMWTime = ADT_History.HistoryFile[XInc].mwtime;
               RecMWScore = ADT_History.HistoryFile[XInc].mwscore;
               MWScoreInput = RecMWScore;
               ADT_History.IRCurrentRecord.mwdate = RecMWDate;
               ADT_History.IRCurrentRecord.mwtime = RecMWTime;
               ADT_History.IRCurrentRecord.mwscore = RecMWScore;
               ADT_Env.MWJulianDate = RecMWDate;
               ADT_Env.MWHHMMSSTime = RecMWTime;
               ADT_Env.MWScore = RecMWScore;
            }
            XInc++;
         }
      }

      /** determine MW score flag and value */
      if(MWScoreInput>=MWScoreThresholdValue1) {
         MWScoreApplicationFlag = 1;
      }
      if((MWScoreInput>=MWScoreThresholdValue1)&&
         (MWScoreInput<=MWScoreThresholdValue2)) {
         IntensityEstimateValueReturn = MWIntensityTnoValue1+0.01;
      }
      if(MWScoreInput>=MWScoreThresholdValue2) {
         IntensityEstimateValueReturn = MWIntensityTnoValue2+0.01;
      }
  
      int MWJulianDateGlobal = ADT_Env.MWJulianDate;
      int MWTimeGlobal = ADT_Env.MWHHMMSSTime;
      if(ADT_Env.DEBUG==100) {
         System.out.printf("MW DATE=%d TIME=%d\n",MWJulianDateGlobal,MWTimeGlobal);
      }

      double CurrentMWTime = ADT_Functions.calctime(MWJulianDateGlobal,MWTimeGlobal);

      double CurrentMWTimeMinus12hr = CurrentMWTime-0.5;
      double CurrentMWTimePlus12hr = CurrentMWTime+0.5;
      double CurrentMWTimePlus6hr = CurrentMWTime+0.25;
    
      if(ADT_Env.DEBUG==100) {
         System.out.printf("MWScoreInput=%f  *IntensityEstimateValue_Return=%f\n",
                            MWScoreInput,IntensityEstimateValueReturn);
         System.out.printf("CurrentTime=%f  CurrentMWTime=%f CurrentMWTimePlus12hr=%f\n",
                             CurrentTime,CurrentMWTime,CurrentMWTimePlus12hr);
      }
    
      /** redo MW score logic */
      if(CurrentTime>CurrentMWTimePlus12hr) {
         MWScoreApplicationFlag = 0;
      }

      /** NEW LOGIC
        - If last valid MW overpass time is greater than 8 hours old, then MW HOLD
        -- If, while in MW HOLD pattern we get three shear scenes, turn MW OFF
        -- If in MW HOLD for greater than or equal to 6 hours, turn MW OFF
        - Will NOT check for three shear scenes if in MW ON
      */

      if((CurrentTime-CurrentMWTime)>.3333) {
         /** MW record is > 8 hours old... start new Shear/HOLD checking logic */
         if(ADT_Env.DEBUG==100) {
            System.out.printf("MW record > 8 hours old... new logic  CurrentTime=%f CurrentMWTime=%f\n",CurrentTime,CurrentMWTime);
         }
    
         /** First... loop through history file records */
         XInc = 0;
         while(XInc<NumRecsHistory) {
            RecDate = ADT_History.HistoryFile[XInc].date;
            RecTime = ADT_History.HistoryFile[XInc].time;
            HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);

            RecLand = ADT_History.HistoryFile[XInc].land;
            Traw = ADT_History.HistoryFile[XInc].Traw;
            LandCheckTF = true;
            if(((LandFlagTF)&&(RecLand==1))||(Traw<1.0)) {
               LandCheckTF = false;
            }

            if(LandCheckTF) {
               if(HistoryRecTime<CurrentTime) {
                  LastCloudSceneValue = ADT_History.HistoryFile[XInc].cloudscene;
                  LastRule8Flag = ADT_History.HistoryFile[XInc].rule8;
                  LastValidMWCIValue = ADT_History.HistoryFile[XInc].CI;
                  NonEIRSceneCounter = (LastCloudSceneValue>=4) ? NonEIRSceneCounter+1 : 0;
                  if(ADT_Env.DEBUG==100) {
                     System.out.printf("time=%f nonEIRscenecounter=%d\n",HistoryRecTime,NonEIRSceneCounter);
                  }
                  if(LastRule8Flag==33) {
                     if(FirstMWHoldTF) {
                        FirstMWHoldTime = HistoryRecTime;
                        FirstMWHoldTF = false;
                     }
                  } else {
                     FirstMWHoldTime = HistoryRecTime;
                     FirstMWHoldTF = true;
                  }
               }
            }
            XInc++;
         }
    
         /** now check for current shear scene and previous MW ON */
         if(LastRule8Flag==32) {
            EXIT_Routine = true;
            /** previous MW ON... set to HOLD */
            ADT_History.IRCurrentRecord.rule8 = 33;     /** HOLD */
            if(ADT_Env.DEBUG==100) {
               System.out.printf("previous MW on... now MW HOLD: nonEIRscenecounter=%d\n",NonEIRSceneCounter);
            }
            IntensityEstimateValueReturn = LastValidMWCIValue;
            MWScoreApplicationFlag = 1;
            /** check to see if current record is shear scene and had two shears before it...
                if so... turn MW OFF */
            if((NonEIRSceneCounter>=2)&&(CurCloudScene==4)) {
               if(ADT_Env.DEBUG==100) {
                  System.out.printf("Third consecutive SHEAR scene... turning OFF!!!\n");
               }
               /** >= three consecutive non-EIR eye scenes and would have been MW HOLD... turn OFF */
               IntensityEstimateValueReturn = InputIntensityEstStore;
               ADT_History.IRCurrentRecord.rule8 = 34;     /** OFF */
               MWScoreApplicationFlag = 0;
            }
            /** end possible remove section */
         } else if (LastRule8Flag==33) {
            EXIT_Routine = true;
            if(ADT_Env.DEBUG==100) {
               System.out.printf("current MW HOLD... nonEIRscene=%d  cloudscene=%d\n",NonEIRSceneCounter,CurCloudScene);
            }
            /** was in MW HOLD... now checking for shear scenes to turn MW OFF */
            if((NonEIRSceneCounter>=2)&&(CurCloudScene==4)) {
               if(ADT_Env.DEBUG==100) {
                  System.out.printf("current MW HOLD... third consecutive SHEAR scene... turning OFF!!!\n");
               }
               /** >= three consecutive non-EIR eye scenes and was in MW HOLD... turn OFF */
               IntensityEstimateValueReturn = InputIntensityEstStore;
               ADT_History.IRCurrentRecord.rule8 = 34;     /** OFF */
               MWScoreApplicationFlag = 0;
            } else {
               ADT_History.IRCurrentRecord.rule8 = 33;     /** HOLD */
               IntensityEstimateValueReturn = LastValidMWCIValue;
               MWScoreApplicationFlag = 1;
               if(ADT_Env.DEBUG==100) {
                  System.out.printf("current MW HOLD... keep HOLD\n");
               }
               if((CurrentTime-FirstMWHoldTime)>=.25) {
                  /**  in MW HOLD >= 6 hours... turn OFF */
                  IntensityEstimateValueReturn = InputIntensityEstStore;
                  ADT_History.IRCurrentRecord.rule8 = 34;     /** OFF */
                  MWScoreApplicationFlag = 0;

                  CommentString = String.format("MW OFF HOLD >=6hrs old",CommentString);
                  ADT_History.IRCurrentRecord.comment = CommentString;
                  if(ADT_Env.DEBUG==100) {
                     System.out.printf("current MW HOLD... in HOLD >= 6 hours... turning OFF!!!\n");
                  }
               }
            }
         }
         else if (LastRule8Flag==34) {
            MWScoreApplicationFlag = 0;
            IntensityEstimateValueReturn = InputIntensityEstStore;
            EXIT_Routine = true;
            if(ADT_Env.DEBUG==100) {
               System.out.printf("last record MW OFF.... returning\n");
            }
         } else {
            /** do nothing */
            MWScoreApplicationFlag = 0;
            IntensityEstimateValueReturn = InputIntensityEstStore;
            EXIT_Routine = true;
            if(ADT_Env.DEBUG==100) {
               System.out.printf("old MW record.... returning\n");
            }
         }
         /** END NEW LOGIC */
      } else {
         /** check for three consecutive eyes */
         int EyeSceneCounter = 0;
         LastCIValue = 1.0;
         XInc = 0;
         while(XInc<NumRecsHistory) {
            RecDate = ADT_History.HistoryFile[XInc].date;
            RecTime = ADT_History.HistoryFile[XInc].time;
            HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);

            RecLand = ADT_History.HistoryFile[XInc].land;
            Traw = ADT_History.HistoryFile[XInc].Traw;
            LandCheckTF = true;
            if(((LandFlagTF)&&(RecLand==1))||(Traw<1.0)) {
               LandCheckTF = false;
            }

            if(LandCheckTF) {
               if(HistoryRecTime<CurrentTime) {
                  /**
                   ** added time check for LastCIValue to make sure MW eye adjustment doesn't
                   ** reset at a time after the MW time once the CI falls below 4.0 (after the
                   ** MW adjustment had been on but had turned off for whatever reason)... the
                   ** CI# must be below 4.0 when the current MW score is implemented at
                   ** or before the current MW time for this to be reset
                   */
                  if(HistoryRecTime<CurrentMWTime) {
                     LastCIValue = ADT_History.HistoryFile[XInc].CI;
                  }
                  LastEyeSceneValue = ADT_History.HistoryFile[XInc].eyescene;
                  LastRule8Flag = ADT_History.HistoryFile[XInc].rule8;
                  EyeSceneCounter = (LastEyeSceneValue<=2) ? EyeSceneCounter+1 : 0;
                  if(ADT_Env.DEBUG==100) {
                     System.out.printf("time=%f eyescenecounter=%d LastCIValue=%f\n",
                                        HistoryRecTime,EyeSceneCounter,LastCIValue);
                  }
                  if(EyeSceneCounter>=3) MWAdjOFFTF = true;
               }
            }
            /**
             ** turn back on MW adjustment if CI falls below 4.0.
             ** had to modifiy because if MW adjustment goes on after "reset"
             ** and CI#s are modified when a new MW adjustment goes on, the
             ** eye check from a previous period will still tell ADT to turn
             ** off MW adjustment even though it was reset.  Checking for Rule8
             ** value of 31 will indicate that the MW was turned back on correctly
             ** and to start new checking for eyes after that point.
             */
            if((LastCIValue<4.0||LastRule8Flag==31)) {
               if(ADT_Env.DEBUG==100) {
                  System.out.printf("CI below 4.0... resetting MW adjustment!!!  LastCIValue=%f\n",LastCIValue);
               }
               MWAdjOFFTF = false;
            }
            XInc++;
         }
         if(ADT_Env.DEBUG==100) {
            System.out.printf("current eye scene=%d\n",CurEyeScene);
         }
         if(CurEyeScene<=2) {
            EyeSceneCounter++;
            if(ADT_Env.DEBUG==100) {
               System.out.printf("time=%f eyescenecounter=%d\n",CurrentTime,EyeSceneCounter);
            }
            if (EyeSceneCounter>=3) MWAdjOFFTF = true;
         }
         if(MWAdjOFFTF) {
            /** found three consecutive eyes... return immediately */
            if(ADT_Env.DEBUG==100) {
               System.out.printf("FOUND THREE EYES... EXITING!!!  LastCIValue=%f\n",LastCIValue);
            }
            if((LastRule8Flag>=30)&&(LastRule8Flag<=33)) {
               ADT_History.IRCurrentRecord.rule8 = 34;     /** OFF */
            }
            IntensityEstimateValueReturn = InputIntensityEstStore;
            EXIT_Routine = true;
            MWScoreApplicationFlag = 0;
         }
      }
      if(ADT_Env.DEBUG==100) {
         System.out.printf("1 EXIT_Routine=%b\n",EXIT_Routine);
      }

      /** check for THREE hours of land interaction... turn MW off this occurs
         changed from six hours on 7 June 2013 per Velden recommendation */
      LastRule8Flag=0;
      if(ADT_Env.DEBUG==100) {
         System.out.printf("CHECKING LAND INTERACTION\n");
      }
      double LastNonLandTime = 0.0;
      boolean MWLandCheckTF = false;
      XInc = 0;
      while(XInc<NumRecsHistory) {
         RecDate = ADT_History.HistoryFile[XInc].date;
         RecTime = ADT_History.HistoryFile[XInc].time;
         HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);

         RecLand = ADT_History.HistoryFile[XInc].land;
         Traw = ADT_History.HistoryFile[XInc].Traw;
         LandCheckTF = true;
         if(((LandFlagTF)&&(RecLand==1))||(Traw<1.0)) {
            LandCheckTF = false;
         }

         if(!LandCheckTF) { /** TC over land */
            if(ADT_Env.DEBUG==100) {
               System.out.printf("OVER LAND SINCE %f FOR %f DAYS \n",LastNonLandTime,(HistoryRecTime-LastNonLandTime));
            }
            if((HistoryRecTime-LastNonLandTime)>=0.125) {
               MWLandCheckTF = true;
               MWAdjOFFTF = false;
               if(ADT_Env.DEBUG==100) {
                  System.out.printf("RESETTING MWAdjOFFTF Flag\n");
               }
            }
         } else {
            LastRule8Flag = ADT_History.HistoryFile[XInc].rule8;
            LastNonLandTime = HistoryRecTime;
            MWLandCheckTF = false;
         }
         if(ADT_Env.DEBUG==100) {
            System.out.printf("MWLandCheckTF=%b\n",MWLandCheckTF);
         }
         XInc++;
      }
      if(ADT_Env.DEBUG==100) {
         System.out.printf("2 EXIT_Routine=%b\n",EXIT_Routine);
      }
      if(ADT_Env.DEBUG==100) {
         System.out.printf("MWLandCheckTF=%b  LandFlag=%d  LastRule8Flag=%d\n",MWLandCheckTF,CurLand,LastRule8Flag);
      }
      if((MWLandCheckTF)&&(CurLand==2)) {
         if((LastRule8Flag>=30)&&(LastRule8Flag<=33)) {
            if(ADT_Env.DEBUG==100) {
               System.out.printf("OVER LAND FOR MORE THAN 3 HOURS... TURNING OFF ME AND EXITING!!!\n");
            }
            ADT_History.IRCurrentRecord.rule8 = 34;     /** OFF */
            IntensityEstimateValueReturn = InputIntensityEstStore;
            EXIT_Routine = true;
            MWScoreApplicationFlag = 0;
         }
      }
      /** End Land Check */

      if(ADT_Env.DEBUG==100) {
         System.out.printf("3 EXIT_Routine=%b\n",EXIT_Routine);
      }

      if(!EXIT_Routine) {
         /** check previous history record values */
         XInc = 0;
         while(XInc<NumRecsHistory) {
            RecDate = ADT_History.HistoryFile[XInc].date;
            RecTime = ADT_History.HistoryFile[XInc].time;
            HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
   
            if(HistoryRecTime<CurrentMWTime) {

               RecLand = ADT_History.HistoryFile[XInc].land;
               Traw = ADT_History.HistoryFile[XInc].Traw;
               LandCheckTF = true;
               if(((LandFlagTF)&&(RecLand==1))||(Traw<1.0)) {
                  LandCheckTF = false;
               }
      
               if(LandCheckTF) { /** TC over land */
                  /* check to see if current MW adjustment was on previous
                  ** to current MW entry date/time */
                  LastMWScoreValue = ADT_History.HistoryFile[XInc].mwscore;
                  LastValidMWCIValue = ADT_History.HistoryFile[XInc].CI;
                  Rule8Flag = ADT_History.HistoryFile[XInc].rule8;
                  if(ADT_Env.DEBUG==100) {
                     System.out.printf("Time=%f  previous MW record:  lastscore=%f lasteye=%d  rule8=%d\n",
                                        HistoryRecTime,LastMWScoreValue,LastEyeSceneValue,Rule8Flag);
                  }
                  PreviousHistoryTnoValue = ADT_History.HistoryFile[XInc].Tfinal;
                  MWAdjCurrentONTF = false;
                  if((Rule8Flag>=31)&&(Rule8Flag<=33)) {
                     MWAdjCurrentONTF = true;
                     if((Rule8Flag>=31)&&(First31Record)) {
                        if(ADT_Env.DEBUG==100) {
                           System.out.printf("found 31 in previous record search!\n");
                        }
                        FirstHistoryCIValue = ADT_History.HistoryFile[XInc].CI;
                        FirstMWadjTime = HistoryRecTime;
                        FirstMWadjTimePlus12hr = FirstMWadjTime+0.5;
                        First31Record = false;
                     }
                  } else {
                     First31Record = true; /** reset mark for MW score merging */
                  }
                  if((Rule8Flag>=30)&&(Rule8Flag<=32)) {
                     LastValidMWCIValue = ADT_History.HistoryFile[XInc].CI;
                     LastValidMWadjTime = HistoryRecTime;
                     LastValidMWadjTimePlus12 = HistoryRecTime+0.5;
                     LastValidMWadjTimePlus6 = HistoryRecTime+0.25;
                  }
               }
            }
            XInc++;
         }

         LastRule8Flag = Rule8Flag;
         PreviousMWScoreValue = LastMWScoreValue;
    
         if(ADT_Env.DEBUG==100) {
            System.out.printf("FirstHistoryCIValue=%f  First31Record=%b\n",FirstHistoryCIValue,First31Record);
            System.out.printf("FirstMWadjTime=%f FirstMWadjTimePlus12hr=%f\n",FirstMWadjTime,FirstMWadjTimePlus12hr);
         }
         /** BEFORE MW TIME */
         if(ADT_Env.DEBUG==100) {
            System.out.printf("MWAdjCurrentONTF=%b MWScoreApplicationFlag=%d\n",MWAdjCurrentONTF,MWScoreApplicationFlag);
         }
         if((!MWAdjCurrentONTF)&&(MWScoreApplicationFlag==0)) {
            /** MW was OFF and is still OFF */
            if(ADT_Env.DEBUG==100) {
               System.out.printf("OFF - OFF\n");
            }
            /** I still need to put the mw value into all records after the mw input time */
            XInc = 0;
            while(XInc<NumRecsHistory) {
               RecDate = ADT_History.HistoryFile[XInc].date;
               RecTime = ADT_History.HistoryFile[XInc].time;
               HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
               if(HistoryRecTime>=CurrentMWTime) {
                  /** determine if there is a record after MW time */
                  NORecordAfterMWTimeTF = false;
               }
               XInc++;
            }
         } else if((!MWAdjCurrentONTF)&&(MWScoreApplicationFlag==1)) {
            /** MW was OFF but is now ON */
            if(ADT_Env.DEBUG==100) {
               System.out.printf("OFF - now ON\n");
            }
    
            XInc = 0;
            while(XInc<NumRecsHistory) {
               RecDate = ADT_History.HistoryFile[XInc].date;
               RecTime = ADT_History.HistoryFile[XInc].time;
               HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
               if(ADT_Env.DEBUG==100) {
                  System.out.printf("HistoryRecTime=%f CurrentMWTime=%f\n",HistoryRecTime,CurrentMWTime);
               }
               if(HistoryRecTime<CurrentMWTime) {
                  /** merge backwards 12 hours previous to MW time */
                  RecLand = ADT_History.HistoryFile[XInc].land;
                  Traw = ADT_History.HistoryFile[XInc].Traw;
                  LandCheckTF = true;
                  if(((LandFlagTF)&&(RecLand==1))||(Traw<1.0)) {
                     LandCheckTF = false;
                  }
      
                  if(LandCheckTF) {
                     HistoryCIValue = ADT_History.HistoryFile[XInc].CI;
                     Rule8Flag = ADT_History.HistoryFile[XInc].rule8;
                     if(HistoryRecTime>=CurrentMWTimeMinus12hr) {
                        if(!FoundHistoryRecPrev12hrTF) {
                           FoundHistoryRecPrev12hrTF = true;
                           HistoryCIValueMinus12hr = HistoryCIValue;
                           HistoryRecTimeMinus12hr = HistoryRecTime;
                        }
                        /**
                         ** interpolate value between current value and
                         ** 12 hour previous value
                         */
                        if(Rule8Flag!=30) {
                           CommentString = "" ;
                           CommentString = String.format("MWinit1=%3.1f/%3.1f/%3.1f",
                                   ADT_History.HistoryFile[XInc].Traw,
                                   ADT_History.HistoryFile[XInc].Tfinal,
                                   ADT_History.HistoryFile[XInc].CI);
                           InterpTimePart = (HistoryRecTime-HistoryRecTimeMinus12hr)/
                                            (CurrentMWTime-HistoryRecTimeMinus12hr);
                           IntensityCIMergeValue = Math.max(IntensityEstimateValueReturn,HistoryCIValueMinus12hr);
                           NewIntensityEstScore = ((IntensityCIMergeValue-HistoryCIValueMinus12hr)*InterpTimePart)+
                                                  HistoryCIValueMinus12hr;
                           ADT_History.HistoryFile[XInc].Traw = NewIntensityEstScore;
                           ADT_History.HistoryFile[XInc].Tfinal = NewIntensityEstScore;
                           ADT_History.HistoryFile[XInc].CI = NewIntensityEstScore;
                           ADT_History.HistoryFile[XInc].rule8 = 30;
                           ADT_History.HistoryFile[XInc].comment = CommentString;
                           if(ADT_Env.DEBUG==100) {
                              System.out.printf("rule8=30 Time=%f NewIntensityEstScore=%f\n",HistoryRecTime,NewIntensityEstScore);
                           }
                        } else {
                           if(ADT_Env.DEBUG==100) {
                              System.out.printf("already merged backwards... skipping\n");
                           }
                        }
                     }
                  }
                  LastValidMWCIValue = ADT_History.HistoryFile[XInc].CI;
                  LastRule8Flag = 30;
               } else {
                  /** determine if there is a record after MW time */
                  NORecordAfterMWTimeTF = false;
               }
               XInc++;
            }
         } else if((MWAdjCurrentONTF)&&(MWScoreApplicationFlag==1)) {
            /** MW was ON and is still ON */
            if(ADT_Env.DEBUG==100) {
               System.out.printf("ON - still ON\n");
            }

            XInc = 0;
            while(XInc<NumRecsHistory) {
               RecDate = ADT_History.HistoryFile[XInc].date;
               RecTime = ADT_History.HistoryFile[XInc].time;
               HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
               if(ADT_Env.DEBUG==100) {
                  System.out.printf("PreviousMWScoreValue=%f IntensityEstimateValue_Ret=%f\n",
                          PreviousMWScoreValue,IntensityEstimateValueReturn);
               }
               if(HistoryRecTime<CurrentMWTime) {
                  if(ADT_Env.DEBUG==100) {
                     System.out.printf("HistoryRecTime=%f CurrentMWTime=%f : LastMWScoreValue=%f *IntensityEstimateValue_Ret=%f\n",
                             HistoryRecTime,CurrentMWTime,LastMWScoreValue,IntensityEstimateValueReturn);
                  }
                  /** merge backwards from 4.3/4.8 to 5.0, if necessary */
                  if((LastMWScoreValue<MWScoreThresholdValue2)&&(IntensityEstimateValueReturn>=MWIntensityTnoValue2)) {
                     /** merge backwards 12 hours previous to MW time */
                     RecLand = ADT_History.HistoryFile[XInc].land;
                     Traw = ADT_History.HistoryFile[XInc].Traw;
                     LandCheckTF = true;
                     if(((LandFlagTF)&&(RecLand==1))||(Traw<1.0)) {
                        LandCheckTF = false;
                     }
                     if(LandCheckTF) {
                        HistoryCIValue = ADT_History.HistoryFile[XInc].CI;
                        Rule8Flag = ADT_History.HistoryFile[XInc].rule8;
                        CommentString = ADT_History.HistoryFile[XInc].comment;
                        if(ADT_Env.DEBUG==100) {
                           System.out.printf("CommentString=%s**%d\n",CommentString,CommentString.length());
                        }
                        if(HistoryRecTime>=CurrentMWTimeMinus12hr) {
                           if(!FoundHistoryRecPrev12hrTF) {
                              FoundHistoryRecPrev12hrTF = true;
                              HistoryCIValueMinus12hr = HistoryCIValue;
                              HistoryRecTimeMinus12hr = HistoryRecTime;
                              if(ADT_Env.DEBUG==100) {
                                 System.out.printf("historyminus12 CI=%f time=%f\n",HistoryCIValueMinus12hr,HistoryRecTimeMinus12hr);
                              }
                           }
                           if(CommentString.length()==0) {
                              /**
                               ** interpolate value between current value and
                               ** 12 hour previous value
                               */
                              CommentString = "";
                              CommentString = String.format("MWinit2=%3.1f/%3.1f/%3.1f",
                                              ADT_History.HistoryFile[XInc].Traw,
                                              ADT_History.HistoryFile[XInc].Tfinal,
                                              ADT_History.HistoryFile[XInc].CI);
                              ADT_History.HistoryFile[XInc].comment = CommentString;
                           }
                           InterpTimePart = (HistoryRecTime-HistoryRecTimeMinus12hr)/
                                            (CurrentMWTime-HistoryRecTimeMinus12hr);
                           NewIntensityEstScore = ((IntensityEstimateValueReturn-HistoryCIValueMinus12hr)*InterpTimePart)+
                                                    HistoryCIValueMinus12hr;
                           ADT_History.HistoryFile[XInc].Traw = NewIntensityEstScore;
                           ADT_History.HistoryFile[XInc].Tfinal = NewIntensityEstScore;
                           ADT_History.HistoryFile[XInc].CI = NewIntensityEstScore;
                           Rule8Flag = ADT_History.HistoryFile[XInc].rule8;
                           if(Rule8Flag>=32) {
                              ADT_History.HistoryFile[XInc].rule8 = 32;
                           }
                           if(ADT_Env.DEBUG==100) {
                              System.out.printf("rule8=32 Time=%f NewIntensityEstScore=%f\n",HistoryRecTime,NewIntensityEstScore);
                           }
                        }
                     }
                  } else {
                     /** merge backwards from 4.3 to 4.8 AFTER HOLD when MW > threshold, if necessary */
                     if((LastMWScoreValue<MWScoreThresholdValue1)&&
                        (MWScoreInput>=MWScoreThresholdValue1)) {
                        CommentString = ADT_History.HistoryFile[XInc].comment;
                        Rule8Flag = ADT_History.HistoryFile[XInc].rule8;
                        if(ADT_Env.DEBUG==100) {
                           System.out.printf("merge backwards - WAS IN HOLD, now back on\n");
                        }
                        /** merge backwards 12 hours previous to MW time */
                        RecLand = ADT_History.HistoryFile[XInc].land;
                        Traw = ADT_History.HistoryFile[XInc].Traw;
                        LandCheckTF = true;
                        if(((LandFlagTF)&&(RecLand==1))||(Traw<1.0)) {
                           LandCheckTF = false;
                        }
                        if(LandCheckTF) {
                           if(HistoryRecTime>=FirstMWadjTime) {
                              if(Rule8Flag==33) {
                                 /**
                                  ** interpolate value between current value and
                                  ** 12 hour previous value
                                  */
                                 if(CommentString.length()==0) {
                                    CommentString = "";
                                    CommentString = String.format("MWinit3=%3.1f/%3.1f/%3.1f",
                                                    ADT_History.HistoryFile[XInc].Traw,
                                                    ADT_History.HistoryFile[XInc].Tfinal,
                                                    ADT_History.HistoryFile[XInc].CI);
                                    ADT_History.HistoryFile[XInc].comment = CommentString;
                                 }
                                 InterpTimePart = (HistoryRecTime-FirstMWadjTime)/
                                                  (FirstMWadjTimePlus12hr-FirstMWadjTime);
                                 InterpTimePart = Math.min(1.0,InterpTimePart);
                                 NewIntensityEstScore = ((MWIntensityTnoValue1a-FirstHistoryCIValue)*InterpTimePart)+
                                                        FirstHistoryCIValue;
                                 ADT_History.HistoryFile[XInc].Traw = NewIntensityEstScore;
                                 ADT_History.HistoryFile[XInc].Tfinal = NewIntensityEstScore;
                                 ADT_History.HistoryFile[XInc].CI = NewIntensityEstScore;
                                 Rule8Flag = ADT_History.HistoryFile[XInc].rule8;
                                 if(Rule8Flag>=32) {
                                    ADT_History.HistoryFile[XInc].rule8 = 32;
                                 }
                                 if(ADT_Env.DEBUG==100) {
                                    System.out.printf("changing to rule8=32 Time=%f NewIntensityEstScore=%f\n",
                                            HistoryRecTime,NewIntensityEstScore);
                                 }
                              } else {
                                 if(ADT_Env.DEBUG==100) {
                                    System.out.printf("ALREADY INTERPOLATED BACKWARDS... SKIPPING\n");
                                 }
                              }
                           }
                        }
                     }
                  }
                  LastValidMWCIValue = ADT_History.HistoryFile[XInc].CI;
               } else {
                  /** determine if there is a record after MW time */
                  NORecordAfterMWTimeTF = false;
               }
               XInc++;
            }
         } else if((MWAdjCurrentONTF)&&(MWScoreApplicationFlag==0)) {
            /** MW was ON and is now OFF */
            if(ADT_Env.DEBUG==100) {
               System.out.printf("ON - now OFF\n");
            }
            XInc = 0;
            while(XInc<NumRecsHistory) {
               RecDate = ADT_History.HistoryFile[XInc].date;
               RecTime = ADT_History.HistoryFile[XInc].time;
               HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
               if(HistoryRecTime>=CurrentMWTime) {
                  /** determine if there is a record after MW time */
                  NORecordAfterMWTimeTF = false;
               }
               XInc++;
            }
         } else {
            /** nothing */
            if(ADT_Env.DEBUG==100) {
               System.out.printf("nothing\n");
            }
         }
    
         if(ADT_Env.DEBUG==100) {
            System.out.printf("LastValidMWCIValue=%f\n",LastValidMWCIValue);
         }
    
         /** AFTER MW TIME */
         if(ADT_Env.DEBUG==100) {
            System.out.printf("NORecordAfterMWTimeTF=%b\n",NORecordAfterMWTimeTF);
         }
         if(!NORecordAfterMWTimeTF) {
            /** first record after MW time is not current record...
             ** do necessary processing to records in between
             ** MW time and up to current record
             */
            if(ADT_Env.DEBUG==100) {
               System.out.printf("CURRENT RECORD IS NOT LAST\n");
            }
            XInc = 0;
            while(XInc<NumRecsHistory) {
               /** handle all records after input MW time */
               RecDate = ADT_History.HistoryFile[XInc].date;
               RecTime = ADT_History.HistoryFile[XInc].time;
               HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
               if(ADT_Env.DEBUG==100) {
                  System.out.printf("HistoryRecTime=%f CurrentMWTime=%f\n",HistoryRecTime,CurrentMWTime);
               }
               Rule8Flag = ADT_History.HistoryFile[XInc].rule8;
               if(Rule8Flag==34) LastRule8Flag = 34;
               RecLand = ADT_History.HistoryFile[XInc].land;
               Traw = ADT_History.HistoryFile[XInc].Traw;
               LandCheckTF = true;
               if(((LandFlagTF)&&(RecLand==1))||(Traw<1.0)) {
                  LandCheckTF = false;
               }
               if(LandCheckTF) {
                  if(HistoryRecTime>=CurrentMWTime) {
                     if(ADT_Env.DEBUG==100) {
                        System.out.printf("LastRule8Flag=%d\n",LastRule8Flag);
                     }
                     switch (LastRule8Flag) {
                     case 30:
                        if(ADT_Env.DEBUG==100) {
                           System.out.printf("rule8 was 30 current is 31\n");
                        }
                        ADT_History.HistoryFile[XInc].rule8 = 32;
                        /** should set value right after MW time to 4.3/4.8 */
                        LastValidMWCIValue = Math.max(IntensityEstimateValueReturn,LastValidMWCIValue);
                        ADT_History.HistoryFile[XInc].Traw = LastValidMWCIValue;
                        ADT_History.HistoryFile[XInc].Tfinal = LastValidMWCIValue;
                        ADT_History.HistoryFile[XInc].CI = LastValidMWCIValue;
                        LastValidMWadjTime = HistoryRecTime;
                        LastValidMWadjTimePlus12 = LastValidMWadjTime+0.5;
                        LastValidMWadjTimePlus6 = LastValidMWadjTime+0.25;
                        FirstHistoryCIValue = ADT_History.HistoryFile[XInc].CI;
                        FirstMWadjTime = HistoryRecTime;
                        FirstMWadjTimePlus12hr = HistoryRecTime+0.5;
                        break;
                     case 31:
                        ADT_History.HistoryFile[XInc].rule8 = 32;
                        if(ADT_Env.DEBUG==100) {
                           System.out.printf("rule8 was 31 current is 32\n");
                        }
                        if(HistoryRecTime<=FirstMWadjTimePlus12hr) {
                           /** merge forward to 4.8 for first 12 hours*/
                           InterpTimePart = 1.0-((FirstMWadjTimePlus12hr-HistoryRecTime)/
                                                 (FirstMWadjTimePlus12hr-FirstMWadjTime));
                           IntensityCIMergeValue = MWIntensityTnoValue1a;
                           NewIntensityEstScore = ((IntensityCIMergeValue-FirstHistoryCIValue)*InterpTimePart)+FirstHistoryCIValue;
                           if(ADT_Env.DEBUG==100) {
                              System.out.printf("InterpTimePart=%f IntensityCIMergeValue=%f NewIntensityEstScore=%f\n",InterpTimePart,
                                                 IntensityCIMergeValue,NewIntensityEstScore);
                           }
                           ADT_History.HistoryFile[XInc].Traw = NewIntensityEstScore;
                           ADT_History.HistoryFile[XInc].Tfinal = NewIntensityEstScore;
                           ADT_History.HistoryFile[XInc].CI = NewIntensityEstScore;
                        }
                        LastValidMWadjTime = HistoryRecTime;
                        LastValidMWadjTimePlus12 = LastValidMWadjTime+0.5;
                        LastValidMWadjTimePlus6 = LastValidMWadjTime+0.25;
                        break;
                     case 32:
                        ADT_History.HistoryFile[XInc].rule8 = 32;
                        if(ADT_Env.DEBUG==100) {
                           System.out.printf("rule8 was 32 current is 32\n");
                        }
                        if(HistoryRecTime<=FirstMWadjTimePlus12hr) {
                           if(MWScoreInput>=MWScoreThresholdValue1) {
                              if(ADT_Env.DEBUG==100) {
                                 System.out.printf("merge to 4.8\n");
                              }
                              /** merge forward to 4.8, if necessary */  /* check to make sure MW score is not 5.0 */
                              if(MWScoreInput<MWScoreThresholdValue2) {
                                 InterpTimePart = 1.0-((FirstMWadjTimePlus12hr-HistoryRecTime)/
                                                       (FirstMWadjTimePlus12hr-FirstMWadjTime));
                                 IntensityCIMergeValue = MWIntensityTnoValue1a;
                                 NewIntensityEstScore = ((IntensityCIMergeValue-FirstHistoryCIValue)*InterpTimePart)+FirstHistoryCIValue;
                                 if(ADT_Env.DEBUG==100) {
                                    System.out.printf("InterpTimePart=%f IntensityCIMergeValue=%f NewIntensityEstScore=%f\n",
                                            InterpTimePart,IntensityCIMergeValue,NewIntensityEstScore);
                                 }
                              } else {
                                 if(ADT_Env.DEBUG==100) {
                                    System.out.printf("holding at 5.0\n");
                                 }
                                 NewIntensityEstScore = MWIntensityTnoValue2;
                              }
                              ADT_History.HistoryFile[XInc].Traw = NewIntensityEstScore;
                              ADT_History.HistoryFile[XInc].Tfinal = NewIntensityEstScore;
                              ADT_History.HistoryFile[XInc].CI = NewIntensityEstScore;
                              LastValidMWCIValue = ADT_History.HistoryFile[XInc].CI;
                              LastValidMWadjTime = HistoryRecTime;
                              LastValidMWadjTimePlus12 = LastValidMWadjTime+0.5;
                              LastValidMWadjTimePlus6 = LastValidMWadjTime+0.25;
                           } else {
                              if(ADT_Env.DEBUG==100) {
                                 System.out.printf("MW value less than 20... START hold\n");
                              }
                              ADT_History.HistoryFile[XInc].rule8 = 33;
                              IntensityEstimateValueReturn = LastValidMWCIValue;
                              ADT_History.HistoryFile[XInc].Traw = LastValidMWCIValue;
                              ADT_History.HistoryFile[XInc].Tfinal = LastValidMWCIValue;
                              ADT_History.HistoryFile[XInc].CI = LastValidMWCIValue;
                           }
                        } else {
                           if(HistoryRecTime<=CurrentMWTimePlus12hr) {
                              if(MWScoreInput>=MWScoreThresholdValue1) {
                                 if(ADT_Env.DEBUG==100) {
                                    System.out.printf("rule8 was 32 keep as 32 (hold at last value of %f)\n",LastValidMWCIValue);
                                 }
                                 ADT_History.HistoryFile[XInc].Traw = LastValidMWCIValue;
                                 ADT_History.HistoryFile[XInc].Tfinal = LastValidMWCIValue;
                                 ADT_History.HistoryFile[XInc].CI = LastValidMWCIValue;
                                 LastValidMWCIValue = ADT_History.HistoryFile[XInc].CI;
                                 LastValidMWadjTime = HistoryRecTime;
                                 LastValidMWadjTimePlus12 = LastValidMWadjTime+0.5;
                                 LastValidMWadjTimePlus6 = LastValidMWadjTime+0.25;
                              } else {
                                 if(ADT_Env.DEBUG==100) {
                                    System.out.printf("MW value less than 20... hold\n");
                                 }
                                 ADT_History.HistoryFile[XInc].rule8 = 33;
                                 ADT_History.HistoryFile[XInc].Traw = LastValidMWCIValue;
                                 ADT_History.HistoryFile[XInc].Tfinal = LastValidMWCIValue;
                                 ADT_History.HistoryFile[XInc].CI = LastValidMWCIValue;
                                 IntensityEstimateValueReturn = LastValidMWCIValue;
                              }
                           } else {
                              if(ADT_Env.DEBUG==100) {
                                 System.out.printf("rule8 was 32 greater than 12 hours old... turn off!!!\n");
                              }
                              ADT_History.HistoryFile[XInc].rule8 = 34;
                              ADT_History.HistoryFile[XInc].Traw = LastValidMWCIValue;
                              ADT_History.HistoryFile[XInc].Tfinal = LastValidMWCIValue;
                              ADT_History.HistoryFile[XInc].CI = LastValidMWCIValue;
                              MWScoreApplicationFlag = 0;
                           }
                        }
                        break;
                     case 33:
                        if(ADT_Env.DEBUG==100) {
                           System.out.printf("HistoryRecTime=%f LastValidMWadjTimeP6=%f\n",
                                   HistoryRecTime,LastValidMWadjTimePlus6);
                        }
                        if(HistoryRecTime<=LastValidMWadjTimePlus6) {
                           if(MWScoreInput>=MWScoreThresholdValue1) {
                              if(ADT_Env.DEBUG==100) {
                                 System.out.printf("rule8 was 33 mwscore>threshold1... turn back ON\n");
                              }
                              ADT_History.HistoryFile[XInc].rule8 = 32; /** Turn back ON */
                              LastValidMWadjTime = HistoryRecTime;
                              LastValidMWadjTimePlus12 = LastValidMWadjTime+0.5;
                              LastValidMWadjTimePlus6 = LastValidMWadjTime+0.25;
                           } else {
                              if(ADT_Env.DEBUG==100) {
                                 System.out.printf("rule8 was 33 hold at 33\n");
                              }
                              ADT_History.HistoryFile[XInc].rule8 = 33; /** hold */
                           }
                           ADT_History.HistoryFile[XInc].Traw = LastValidMWCIValue;
                           ADT_History.HistoryFile[XInc].Tfinal = LastValidMWCIValue;
                           ADT_History.HistoryFile[XInc].CI = LastValidMWCIValue;
                           LastValidMWCIValue = ADT_History.HistoryFile[XInc].CI;
                           MWScoreApplicationFlag = 1;
                        } else {
                           if(ADT_Env.DEBUG==100) {
                              System.out.printf("rule8 was 33 GREATER THAN 6 hours old!   TURNING OFF\n");
                           }
                           ADT_History.HistoryFile[XInc].rule8 = 34; /** turn off */
                           CommentString = "";
                           CommentString = String.format("MW OFF HOLD >=6hrs old");
                           ADT_History.HistoryFile[XInc].comment = CommentString;
                           MWScoreApplicationFlag = 0;
                        }
                        break;
                     case 34:
                        /** added */
                        if(MWScoreInput>=MWScoreThresholdValue1) {
                           if(ADT_Env.DEBUG==100) {
                              System.out.printf("rule8 was 34 mwscore>threshold1... turn back ON\n");
                           }
                           ADT_History.HistoryFile[XInc].rule8 = 31;
                           LastValidMWadjTime = HistoryRecTime;
                           LastValidMWadjTimePlus12 = LastValidMWadjTime+0.5;
                           LastValidMWadjTimePlus6 = LastValidMWadjTime+0.25;
                           ADT_History.HistoryFile[XInc].Traw = LastValidMWCIValue;
                           ADT_History.HistoryFile[XInc].Tfinal = LastValidMWCIValue;
                           ADT_History.HistoryFile[XInc].CI = LastValidMWCIValue;
                           LastValidMWCIValue = ADT_History.HistoryFile[XInc].CI;
                           MWScoreApplicationFlag = 1;
                           CommentString = "";
                           ADT_History.HistoryFile[XInc].comment = CommentString;
                        } else {
                           if(ADT_Env.DEBUG==100) {
                              System.out.printf("rule8 was 34 adjustment is off!\n");
                           }
                           MWScoreApplicationFlag = 0;
                        }
                        /** end added */
                        break;
                     default:
                        if(ADT_Env.DEBUG==100) {
                           System.out.printf("DEFAULT : MW OFF at time %f\n",HistoryRecTime);
                        }
                     }
                     System.out.printf("resetting MWdate/time for record XINC=%d\n",XInc);
                     ADT_History.HistoryFile[XInc].mwscore = MWScoreInput;
                     ADT_History.HistoryFile[XInc].mwdate = ADT_Env.MWJulianDate;
                     ADT_History.HistoryFile[XInc].mwtime = ADT_Env.MWHHMMSSTime;
                  }
                  LastRule8Flag = ADT_History.HistoryFile[XInc].rule8;
                  PreviousMWScoreValue = ADT_History.HistoryFile[XInc].mwscore;
               }
               XInc++;
            }
            /** deal with last/current record */
         }
    
         /** deal with last/current record */
         if(ADT_Env.DEBUG==100) {
            System.out.printf("CURRENT RECORD IS LAST\n");
         }
         /** first record after MW time is current record...
          ** mark it with appropriate Rule 8 value
          */
         if(ADT_Env.DEBUG==100) {
            System.out.printf("LastRule8Flag=%d\n",LastRule8Flag);
         }
         switch (LastRule8Flag) {
         case 30:
            ADT_History.IRCurrentRecord.rule8 = 31;
            if(ADT_Env.DEBUG==100) {
               System.out.printf("rule8 was 30 current is 31\n");
               System.out.printf("LastValidMWCIValue=%f\n",LastValidMWCIValue);
            }
            IntensityEstimateValueReturn = Math.max(LastValidMWCIValue,MWIntensityTnoValue1);
            MWScoreApplicationFlag = 1;
            break;
         case 31:
            ADT_History.IRCurrentRecord.rule8 = 32;
            if(ADT_Env.DEBUG==100) {
               System.out.printf("rule8 was 31 current is 32\n");
               System.out.printf("LastValidMWCIValue=%f\n",LastValidMWCIValue);
            }
            IntensityEstimateValueReturn = Math.max(LastValidMWCIValue,MWIntensityTnoValue1);
            MWScoreApplicationFlag = 1;
            break;
         case 32:
            ADT_History.IRCurrentRecord.rule8 = 32;
            if(ADT_Env.DEBUG==100) {
               System.out.printf("rule8 was 32 current is 32\n");
            }
            MWScoreApplicationFlag = 1;
            /** check for change in MW value from previous record */
            double MWScoreDiffValue = Math.abs(PreviousMWScoreValue-MWScoreInput);
            if(ADT_Env.DEBUG==100) {
               System.out.printf("previousMWvalue=%f  currentMWvalue=%f  diff=%f\n",
                       PreviousMWScoreValue,MWScoreInput,MWScoreDiffValue);
            }
            if(MWScoreInput>=MWScoreThresholdValue1) {
               if(ADT_Env.DEBUG==100) {
                  System.out.printf("currentTime=%f  MWTimeplus12=%f\n",CurrentTime,FirstMWadjTimePlus12hr);
               }
               if(CurrentTime<=FirstMWadjTimePlus12hr) {
                  if(MWScoreInput<MWScoreThresholdValue2) {
                     if(ADT_Env.DEBUG==100) {
                        System.out.printf("merging to 4.8\n");
                     }
                     InterpTimePart = 1.0-((FirstMWadjTimePlus12hr-CurrentTime)/
                                           (FirstMWadjTimePlus12hr-FirstMWadjTime));
                     IntensityCIMergeValue = MWIntensityTnoValue1a;
                     NewIntensityEstScore = ((IntensityCIMergeValue-FirstHistoryCIValue)*InterpTimePart)+FirstHistoryCIValue;
                     if(ADT_Env.DEBUG==100) {
                        System.out.printf("InterpTimePart=%f IntensityCIMergeValue=%f NewIntensityEstScore=%f\n",
                                           InterpTimePart,IntensityCIMergeValue,NewIntensityEstScore);
                     }
                  } else {
                     if(ADT_Env.DEBUG==100) {
                        System.out.printf("holding at 5.0\n");
                     }
                     NewIntensityEstScore = MWIntensityTnoValue2;
                  }
                  IntensityEstimateValueReturn = NewIntensityEstScore;
               } else {
                  if(ADT_Env.DEBUG==100) {
                     System.out.printf("CurrentTime=%f  CurrentMWTimePlus12hr=%f\n",CurrentTime,CurrentMWTimePlus12hr);
                  }
                  if(CurrentTime<=CurrentMWTimePlus12hr) {
                     if(ADT_Env.DEBUG==100) {
                        System.out.printf("***rule8 was 32 keep as 32 (hold at last value of %f)\n",LastValidMWCIValue);
                     }
                     IntensityEstimateValueReturn = LastValidMWCIValue;
                  } else {
                     CommentString = "";
                     CommentString = String.format("MW OFF  >=12hrs old");
                     ADT_History.IRCurrentRecord.comment = CommentString;
                     if(ADT_Env.DEBUG==100) {
                        System.out.printf("rule8 was 33 GREATER THAN 12 hours old!  TURNING OFF\n");
                     }
                     ADT_History.IRCurrentRecord.rule8 = 34; /** turn off */
                     IntensityEstimateValueReturn = LastValidMWCIValue;
                     MWScoreApplicationFlag = 0;
                  }
               }
            } else {
               if(ADT_Env.DEBUG==100) {
                  System.out.printf("MW value < 20: rule8 was 32 current is 33  STARTING HOLD\n");
               }
               ADT_History.IRCurrentRecord.rule8 = 33; /** start hold for 12 hrs */
               IntensityEstimateValueReturn = LastValidMWCIValue;
            }
            break;
         case 33:
            if(ADT_Env.DEBUG==100) {
               System.out.printf("CurrentTime=%f  CurrentMWTimePlus6hr=%f\n",CurrentTime,CurrentMWTimePlus6hr);
            }
            if(CurrentTime<=CurrentMWTimePlus6hr) {
               if(MWScoreInput>=MWScoreThresholdValue1) {
                  if(ADT_Env.DEBUG==100) {
                     System.out.printf("rule8 was 33 current is 32   score >threshold1\n");
                  }
                  ADT_History.IRCurrentRecord.rule8 = 32; /** turn back ON */
               } else {
                  if(ADT_Env.DEBUG==100) {
                     System.out.printf("rule8 was 33 current is 33   STILL HOLDING\n");
                  }
                  ADT_History.IRCurrentRecord.rule8 = 33; /** start hold for 12 hrs */
               }
               IntensityEstimateValueReturn = LastValidMWCIValue;
               MWScoreApplicationFlag = 1;
            } else {
               CommentString = "";
               CommentString = String.format("MW OFF HOLD >=6hrs old");
               ADT_History.IRCurrentRecord.comment = CommentString;
               if(ADT_Env.DEBUG==100) {
                  System.out.printf("rule8 was 33 GREATER THAN 6 hours old!   TURNING OFF\n");
               }
               ADT_History.IRCurrentRecord.rule8 = 34; /** turn off */
               IntensityEstimateValueReturn = LastValidMWCIValue;
               MWScoreApplicationFlag = 0;
            }
            break;
         /** added */
         case 34:
            if(MWScoreInput>=MWScoreThresholdValue1) {
               if(ADT_Env.DEBUG==100) {
                  System.out.printf("rule8 was 34 current is 31   score >threshold1\n");
               }
               ADT_History.IRCurrentRecord.rule8 = 31; /** turn back ON */
               IntensityEstimateValueReturn = LastValidMWCIValue;
               MWScoreApplicationFlag = 1;
            } else {
               IntensityEstimateValueReturn = InputIntensityEstStore;
               MWScoreApplicationFlag = 0;
            }
            break;
         /** end added */
         default:
            IntensityEstimateValueReturn = InputIntensityEstStore;
            MWScoreApplicationFlag = 0;
            if(ADT_Env.DEBUG==100) {
                   System.out.printf("default!\n");
            }
         }
         if(ADT_Env.DEBUG==100) {
            System.out.printf("Setting current MW score to %f\n",MWScoreInput);
         }
         ADT_History.IRCurrentRecord.mwscore = ADT_Env.MWScore;
         ADT_History.IRCurrentRecord.mwdate = ADT_Env.MWJulianDate;
         ADT_History.IRCurrentRecord.mwtime = ADT_Env.MWHHMMSSTime;
         IntensityEstimateValueReturn = ((double)(int)((IntensityEstimateValueReturn+0.05)*10.0))/10.0;
         /** EXIT_Routine */
      } else {
         /** need to put the intensity score into the correct place in the history file */
         XInc = 0;
         while(XInc<NumRecsHistory) {
            RecDate = ADT_History.HistoryFile[XInc].date;
            RecTime = ADT_History.HistoryFile[XInc].time;
            HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
            if(HistoryRecTime>=CurrentMWTime) {
               /** determine if there is a record after MW time */
               if(ADT_Env.DEBUG==100) {
                  System.out.printf("putting MW value of %f into history file at %f!\n",MWScoreInput,HistoryRecTime);
               }
               ADT_History.HistoryFile[XInc].mwscore = MWScoreInput;
               ADT_History.HistoryFile[XInc].mwdate = ADT_Env.MWJulianDate;
               ADT_History.HistoryFile[XInc].mwtime = ADT_Env.MWHHMMSSTime;
            }
            XInc++;
         }
      }
    
      /** IntensityEstimateValueReturn = IntensityEstimateValueReturn;  compiler says this does nothing */
      if(ADT_Env.DEBUG==100) {
         System.out.printf("exit *IntensityEstimateValue_Return=%f\n",IntensityEstimateValueReturn);
         System.out.printf("MWScoreApplicationFlag=%d\n",MWScoreApplicationFlag);
      }

      return new double[] { (double)MWScoreApplicationFlag, IntensityEstimateValueReturn };
   }

}
