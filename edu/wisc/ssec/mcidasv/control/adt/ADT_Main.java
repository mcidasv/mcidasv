package edu.wisc.ssec.mcidasv.control.adt;


import java.io.*;
import java.lang.*;
import java.util.*;
@SuppressWarnings({"static-access", "unused"})

public class ADT_Main {

   public static String HistoryFileName;
   public static String ReturnOutputString;
   
   public ADT_Main( ) { 
      HistoryFileName = null;
      ReturnOutputString = null;
   }

   public int GetInitialPosition() {
       
       ADT_Auto AutoMode = new ADT_Auto();
       
       String ForecastFileName = null;
       double[] AutoMode1Return = null;

       boolean RunAuto = ADT_Env.AutoTF;
       ForecastFileName = ADT_Env.ForecastFileName;
       int ForecastFileType = ADT_Env.ForecastFileType;
       
       System.out.printf("Run AUTO=%b\n",RunAuto);
       if(RunAuto) {
          try {
              AutoMode1Return = AutoMode.AutoMode1(ForecastFileName,ForecastFileType);
              if(((int)AutoMode1Return[0])<0) {
                  System.out.printf("ERROR with interpolation/extrapolation : return code %d\n", (int)AutoMode1Return[0]);
                  return -1;
              }
          }
          catch(IOException exception) {
              System.out.printf("ERROR with reading forecast file\n");
              return -2;
          }
          int ForecastReturnFlag = (int)AutoMode1Return[0];
          double ForecastLatitude = AutoMode1Return[1];
          double ForecastLongitude = AutoMode1Return[2];
          double ForecastIntensity = AutoMode1Return[3];
          double ForecastMethodID = AutoMode1Return[4];
          
          ADT_Env.SelectedLatitude = ForecastLatitude;
          ADT_Env.SelectedLongitude = ForecastLongitude;

          ADT_History.IRCurrentRecord.latitude = ForecastLatitude;
          ADT_History.IRCurrentRecord.longitude = ForecastLongitude;
          ADT_History.IRCurrentRecord.autopos = 1;  // forecast interpolation
          System.out.printf("AutoMode1 output position info : Latitude=%f Longitude=%f Intensity=%f MethodID=%f Flag=%d\n",
                             ForecastLatitude,ForecastLongitude,ForecastIntensity,ForecastMethodID,ForecastReturnFlag);

      } else {
          System.out.printf("Manual Mode : latitude=%f longitude=%f",ADT_Env.SelectedLatitude,ADT_Env.SelectedLongitude);
          ADT_History.IRCurrentRecord.latitude = ADT_Env.SelectedLatitude;
          ADT_History.IRCurrentRecord.longitude = ADT_Env.SelectedLongitude;
          ADT_History.IRCurrentRecord.autopos = 0;  // Manual
      }
       
      return 1;
      
   }
   
   public void GetARCHERPosition() {

      double[] AutoMode2Return = null;
      double InputLatitude = ADT_Env.SelectedLatitude;
      double InputLongitude = ADT_Env.SelectedLongitude;
      boolean OverrideCenter = ADT_Env.OverCenterTF;
      try {
          AutoMode2Return = ADT_Auto.AutoMode2(InputLatitude,InputLongitude);
      }
      catch(IOException exception) {
          System.out.printf("ERROR with Automode2 routine\n");
          return;
      }
      
      double FinalLatitude = AutoMode2Return[0];
      double FinalLongitude = AutoMode2Return[1];
      int FinalPositioningMethod = (int)AutoMode2Return[2];
      ADT_History.IRCurrentRecord.latitude = FinalLatitude;
      ADT_History.IRCurrentRecord.longitude = FinalLongitude;
      ADT_History.IRCurrentRecord.autopos = FinalPositioningMethod;
      
      ADT_Env.SelectedLatitude = ADT_History.IRCurrentRecord.latitude;
      ADT_Env.SelectedLongitude = ADT_History.IRCurrentRecord.longitude;

      return;
      
   }

   public String RunADTAnalysis( boolean RunFullAnalysis, String InputHistoryFile ) throws IOException {
       
      String BulletinOutput = null;
      String HistoryListOutput = null;
      int aaa;
      int HistoryFileRecords;
      int RetIntValue;
      int RetIntValue2;
      int RetErr;

      ADT_History CurrentHistory = new ADT_History();
      ADT_Topo Topo = new ADT_Topo();
      ADT_Scene SceneType = new ADT_Scene();
      ADT_Intensity Intensity = new ADT_Intensity();
      ADT_Output Output = new ADT_Output();
      ADT_Forecasts Forecast = new ADT_Forecasts();
      ADT_Functions Functions = new ADT_Functions();

      HistoryFileName = InputHistoryFile;

      boolean OverrideScene = ADT_Env.OverSceneTF;
      int OverrideSceneTypeValue = ADT_Env.OverrideSceneType;

      /** System.out.printf("MW Info : Date=%s JulianDate=%d Time=%d Score=%f\n",MWDate,ADT_Env.MWJulianDate,MWTime,MWScore); */
     
      /** READ HISTORY FILE INFORMATION */
      if(RunFullAnalysis && HistoryFileName != null) {    
         try {
            CurrentHistory.ReadHistoryFile(HistoryFileName);
         }
         catch(IOException exception)
         {
            System.out.printf("History file %s not found\n",HistoryFileName);
         }
      }
      else {
          System.out.printf("Not utilizing a history file\n");
      }
      HistoryFileRecords = CurrentHistory.HistoryNumberOfRecords();
      System.out.printf("Number of records in history file %s is %d\n",HistoryFileName,HistoryFileRecords);
    
      /** read topography file at center position */
      double PositionLatitude = ADT_History.IRCurrentRecord.latitude;
      double PositionLongitude = ADT_History.IRCurrentRecord.longitude;
      String TopoFilePath = System.getenv("ODTTOPO");
      String topoPath = new File(".").getCanonicalPath();
      System.err.println("topoPath: " + topoPath);
      String TopoFileName = topoPath + "/edu/wisc/ssec/mcidasv/data/hydra/resources/digelev_hires_le.map";

      int TopographyFlag = 0;
      System.out.printf("TOPO Info : File=%s Lat=%f Lon=%f\n",TopoFileName,PositionLatitude,PositionLongitude);
      try {
         TopographyFlag = ADT_Topo.ReadTopoFile(TopoFileName,PositionLatitude,PositionLongitude);
      }
      catch(IOException e)
        {
            System.err.printf("ERROR reading topography file\n");
            e.printStackTrace();
            return null;
        }
      /** System.out.printf("after topo read flag=%d\n",TopographyFlag); */
      ADT_History.IRCurrentRecord.land = TopographyFlag;

      /** Calculate Eye and Cloud region temperatures */
      ADT_Data.CalcEyeCloudTemps();
      /** System.out.printf("after calceyecloudtemps\n"); */
      /**
       ** double Eye_Temperature = ADT_History.IRCurrentRecord.eyet;
       ** double CWCloud_Temperature = ADT_History.IRCurrentRecord.cwcloudt;
       ** double Cloud_Temperature = ADT_History.IRCurrentRecord.cloudt;
       ** double Cloud2_Temperature = ADT_History.IRCurrentRecord.cloudt2;
       ** double Cloud_Symmetry = ADT_History.IRCurrentRecord.cloudsymave;
       ** double Eye_STDV = ADT_History.IRCurrentRecord.eyestdv;
       ** int CWRing_Distance = ADT_History.IRCurrentRecord.cwring;
       ** System.out.printf("Eye Temperature=%f\n",Eye_Temperature);
       ** System.out.printf("CWCloud Temperature=%f\n",CWCloud_Temperature);
       ** System.out.printf("CWRing Distance=%d\n",CWRing_Distance);
       ** System.out.printf("Cloud Temperature=%f\n",Cloud_Temperature);
       ** System.out.printf("Cloud2 Temperature=%f\n",Cloud2_Temperature);
       ** System.out.printf("Cloud Symmetry=%f\n",Cloud_Symmetry);
       ** System.out.printf("Eye STDV=%f\n",Eye_STDV);
       */

      /** Calculate Eye and Cloud region Scene Type */

      /** System.out.printf("overridescenetypevalue=%d\n", OverrideSceneTypeValue); */
      if(OverrideSceneTypeValue>=0) {
          /** System.out.printf("setting old scene types\n"); */
          ADT_History.IRCurrentRecord.cloudsceneold = ADT_History.IRCurrentRecord.cloudscene;
          ADT_History.IRCurrentRecord.eyesceneold = ADT_History.IRCurrentRecord.eyescene;
          ADT_History.IRCurrentRecord.cloudscene = Math.max(0,(OverrideSceneTypeValue - 3));
          ADT_History.IRCurrentRecord.eyescene = Math.min(3,OverrideSceneTypeValue);
          ADT_Env.OverrideSceneType = -99;
      } else {
          SceneType.DetermineSceneType(RunFullAnalysis);
          /** System.out.printf("after scene type determination\n"); */
          /** System.out.printf("OverrideScene=%b\n",OverrideScene); */
          if(OverrideScene) {
              /** System.out.printf("overriding scene type : eye=%d cloud=%d\n",ADT_History.IRCurrentRecord.eyescene,ADT_History.IRCurrentRecord.cloudscene); */
              if (ADT_History.IRCurrentRecord.eyescene<3) {
                  ADT_Env.OverrideSceneType = ADT_History.IRCurrentRecord.eyescene;
              } else {
                  ADT_Env.OverrideSceneType = 3 + ADT_History.IRCurrentRecord.cloudscene;
              }
              /** System.out.printf("ADTEnv.overridescenetype=%d\n", ADT_Env.OverrideSceneType); */
              return "override";
          }
      }

      /** Calculate Intensity Estimate Values */

      int RedoIntensityFlag = 0;
      Intensity.CalculateIntensity(RedoIntensityFlag,RunFullAnalysis,HistoryFileName);
      /** System.out.printf("after calcintensity\n"); */
      /** Write Bulletin Output */

      BulletinOutput = Output.TextScreenOutput(HistoryFileName);
      /** System.out.printf("\n *** Bulletin Output ***\n%s\n",BulletinOutput); */
      /** System.out.printf("after textscreenoutput\n"); */
      ReturnOutputString = BulletinOutput;
      
      return ReturnOutputString;

   }
}

