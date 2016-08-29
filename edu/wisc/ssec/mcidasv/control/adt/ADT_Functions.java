package edu.wisc.ssec.mcidasv.control.adt;

import java.io.*;
import java.lang.Math;
import java.lang.String;
import java.util.*;

@SuppressWarnings("unused")

public class ADT_Functions {

   static String[] Months = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
   static double[] PW_TnoValues = {-9999.,-8888.,
      1.0,1.1,1.2,1.3,1.4,1.5,1.6,1.7,1.8,1.9,
      2.0,2.1,2.2,2.3,2.4,2.5,2.6,2.7,2.8,2.9,
      3.0,3.1,3.2,3.3,3.4,3.5,3.6,3.7,3.8,3.9,
      4.0,4.1,4.2,4.3,4.4,4.5,4.6,4.7,4.8,4.9,
      5.0,5.1,5.2,5.3,5.4,5.5,5.6,5.7,5.8,5.9,
      6.0,6.1,6.2,6.3,6.4,6.5,6.6,6.7,6.8,6.9,
      7.0,7.1,7.2,7.3,7.4,7.5,7.6,7.7,7.8,7.9,
      8.0,8.1,8.2,8.3,8.4,8.5,8.6,8.7,8.8,8.9,9.0};
   static double[][] PW_PressureValues = {
        /** Atlantic pressure relationship values */
      {-9999.0,-8888.0,
        1014.0,1013.6,1013.2,1012.8,1012.4,1012.0,1011.4,1010.8,1010.2,1009.6,
        1009.0,1008.2,1007.4,1006.6,1005.8,1005.0,1004.0,1003.0,1002.0,1001.0,
        1000.0,998.8,997.6,996.4,995.2,994.0,992.6,991.2,989.8,988.4,
         987.0,985.4,983.8,982.2,980.6,979.0,977.2,975.4,973.6,971.8,
         970.0,968.0,966.0,964.0,962.0,960.0,957.6,955.2,952.8,950.4,
         948.0,945.4,942.8,940.2,937.6,935.0,932.2,929.4,926.6,923.8,
         921.0,918.0,915.0,912.0,909.0,906.0,902.8,899.6,896.4,893.2,
         890.0,886.6,883.2,879.8,876.4,873.0,869.4,865.8,862.2,858.6,855.0},
        /** Pacific pressure relationship values */
      {-9999.0,-8888.0,
        1005.0,1004.6,1004.2,1003.8,1003.4,1003.0,1002.4,1001.8,1001.2,1000.6,
        1000.0,999.4,998.8,998.2,997.6,997.0,995.8,994.6,993.4,992.2,
         991.0,989.6,988.2,986.8,985.4,984.0,982.4,980.8,979.2,977.6,
         976.0,974.0,972.0,970.0,968.0,966.0,963.6,961.2,958.8,956.4,
         954.0,951.4,948.8,946.2,943.6,941.0,938.2,935.4,932.6,929.8,
         927.0,924.4,921.8,919.2,916.6,914.0,910.8,907.6,904.4,901.2,
         898.0,894.2,890.4,886.6,882.8,879.0,874.8,870.6,866.4,862.2,
         858.0,853.4,848.8,844.2,839.6,835.0,830.0,825.0,820.0,815.0,810.0} };

    /** Atlantic/Pacific pressure relationship values */
   static double[] PW_WindValues = {-9999.0,-8888.0,
          25.0, 25.0, 25.0, 25.0, 25.0, 25.0, 26.0, 27.0, 28.0, 29.0,
          30.0, 31.0, 32.0, 33.0, 34.0, 35.0, 37.0, 39.0, 41.0, 43.0,
          45.0, 47.0, 49.0, 51.0, 53.0, 55.0, 57.0, 59.0, 61.0, 63.0,
          65.0, 67.4, 69.8, 72.2, 74.6, 77.0, 79.6, 82.2, 84.8, 87.4,
          90.0, 92.4, 94.8, 97.2, 99.6,102.0,104.6,107.2,109.8,112.4,
         115.0,117.4,119.8,122.2,124.6,127.0,129.6,132.2,134.8,137.4,
         140.0,143.0,146.0,149.0,152.0,155.0,158.0,161.0,164.0,167.0,
         170.0,173.0,176.0,179.0,182.0,185.0,188.0,191.0,194.0,197.0,200.0};

   private static double RADIANSCONSTANT = 0.017453292;
   private static double ANGLE90DEGRADIANS = 1.570797;
   private static double PI = 3.14159265358979;
   private static double EARTHRADIUSKM = 6371.0;
   private static int[] JulianDateMonthArray = {0,31,59,90,120,151,181,212,243,273,304,334,365};

   private static double deg_to_rad(double deg) {
      return ((deg * PI) / 180.0);
   }

   private static double rad_to_deg(double rad) {
      return ((rad * 180.0) / PI);
   }

   private static double a_sign(double x,double y) {
      return ((x)/(y))*Math.abs(y);
   }

   public static double calctime(int InputJulianDate, int InputHMSTime) {
   /**
    ** Compute time in ADT xxxxx.yyy units, where xxxxx is the
    ** day and yyy is the percentage of the day.  This routine
    ** will also correct for Y2K problems.
    ** Inputs  : InputJulianDate - Julian date
    **           InputHMSTime    - time in HHMMSS format
    ** Outputs : None
    ** Return  : Date/Time value in xxxxx.yyy units
    */

      double TimeReturnValue;

      /** System.out.printf("calctime : input date=%d\n", InputJulianDate); */
      if(((InputJulianDate%1000)==0)||(InputHMSTime<0)) {
         TimeReturnValue=0.0;
      }
      else {
         int YearValue=InputJulianDate/1000;  /** obtain year */
         /** check for millenium designation in the year.
            if it is not there, add it onto the beginning */
         /** System.out.printf("calctime : year=%d\n", YearValue); */
         if(YearValue<1900) {
            if(YearValue>70) {
               InputJulianDate=1900000+InputJulianDate;
            }
            else {
               InputJulianDate=2000000+InputJulianDate;
            }
         }
         /** System.out.printf("calctime : InputJulianDate=%d\n",InputJulianDate); */
         double SecondsValue=((double)(InputHMSTime%100))/3600.0;
         /** System.out.printf("calctime : secs=%f\n", SecondsValue); */
         double MinutesValue=((double)((InputHMSTime/100)%100))/60.0;
         /** System.out.printf("calctime : mins=%f\n", MinutesValue); */
         double HoursValue=(double)(InputHMSTime/10000);
         /** System.out.printf("calctime : hrs=%f\n", HoursValue); */
         double PartialDateValue=(HoursValue+MinutesValue+SecondsValue)/24.0;
         /** System.out.printf("calctime : pdv=%f\n", PartialDateValue); */
         TimeReturnValue=(double)InputJulianDate+(double)PartialDateValue;
         /** System.out.printf("calctime : retval=%f\n", TimeReturnValue); */
      }

      return TimeReturnValue;
   }

   public static int cmonth2julian(String InputDateCharString) {
      /**
      ** Convert YYYYMMMDD format character string value to julian date Integer.
      ** Inputs  : InputDateCharString - character representation of date in
      **                                 YYYYmonDD format where DD and YYYY are
      **                                 integers and mon is a three character
      **                                 abbreviation for the month (e.g. 2000MAR13)
      ** Outputs : None
      ** Return  : Julian Date
      */

      int JulianDateReturnValue=99999;     /** julian date return value */
      int MonthValue=0;

      String delims = "[A-Z]+";

      String[] tokens = InputDateCharString.split(delims);

      int YearValue = Integer.parseInt(tokens[0]);
      int DayValue = Integer.parseInt(tokens[1]);

      Scanner InputValue = new Scanner(InputDateCharString);
      InputValue.useDelimiter("[^A-Z]+");
      String MonthCharString = InputValue.next();
      InputValue.close();
      
      /** calculate integer month value */
      while((MonthValue < 12) && (!MonthCharString.equals(Months[MonthValue]))) {
         MonthValue++;
      }

      /** determine julian date from day/month/year */
      if(MonthValue<12) {
         JulianDateReturnValue = ADT_Functions.idmyyd(DayValue,MonthValue+1,YearValue);
      }
      else {
         JulianDateReturnValue = 0;   /** Error */
      }

      return JulianDateReturnValue;

   }

   public static int idmyyd(int InputDay, int InputMonth, int InputYear)
   /**
   ** Convert dd/mm/yy to yyyyddd format.
   ** this routine was originally taken from McIDAS
   ** program idmyyd.for.
   ** Inputs  : InputDay   - day
   **           InputMonth - month (integer)
   **           InputYear  - year (YY or YYYY)
   ** Outputs : None
   ** Return  : Julian date or 0 (0=bad input data)
   */
   {
     int JulianDate_Return=-1;             /* julian date return value */
     int DayValue;                         /* local day value */
   
     /** perform a couple quality checks for day/month */
     if(((InputDay<1)||(InputDay>31))||((InputMonth<1)||(InputMonth>12))) {
        JulianDate_Return=0;
     }
     else
     {
        DayValue=InputDay+JulianDateMonthArray[InputMonth-1];
        if(InputYear<1900) {
          if(InputYear>70) {
            InputYear=1900+InputYear;
          }
          else {
            InputYear=2000+InputYear;
          }
        }
        /** Leap year check */
        if(((InputYear%4)==0)&&(InputMonth>2)) {
           DayValue=DayValue+1;
        }
        JulianDate_Return=(InputYear*1000)+DayValue;
     }
   
     return JulianDate_Return;
   
   }

   public static int[] adt_yddmy(int JulianDateInput)
   /**
    ** Convert yyyyddd to dd/mm/yy format.
    ** Inputs  : JulianDateInput - Julian day (yyyyddd)
    ** Outputs : Day_Return      - date
    **           Month_Return    - month
    **           Year_Return     - year (yyyy)
    ** Return  : None
    */
   {
      int YearValue_Local;                  /** local year value */
      int DayValue_Local;                   /** local day value */
      int MonthValue_Local;                 /** local month value */
      int LeapYearFlag=0;                   /** leap year flag value */

      YearValue_Local=JulianDateInput/1000;
      if(YearValue_Local<1900) {
         if(YearValue_Local>70) {
           YearValue_Local=YearValue_Local+1900;
         }
         else {
           YearValue_Local=YearValue_Local+2000;
         }
      }
      DayValue_Local=(JulianDateInput%1000);
      for(MonthValue_Local=0;MonthValue_Local<13;MonthValue_Local++) {
         if(DayValue_Local<=JulianDateMonthArray[MonthValue_Local]) {
            break;
         }
      }
      int Year_Return=YearValue_Local;
      int Month_Return=MonthValue_Local;
      int Day_Return=DayValue_Local-JulianDateMonthArray[MonthValue_Local-1];
      if(((YearValue_Local%4)==0)&&(MonthValue_Local>2)) {
         Day_Return=Day_Return+1;
      }

      return new int[] { Day_Return, Month_Return, Year_Return };
 
   }

   public static String adt_julian2cmonth( int JulianDateInputValue)
   /**
    ** convert julian date to YYYYMMMDD format for output.
    ** Inputs  : JulianDateInputValue - Julian date representation of date
    ** Outputs : ReturnDateCharString - character representation of date
    **                                  in YYYYmonDD format where DD and YYYY
    **                                  are integers and mon is a three
    **           character abbreviation for the month (e.g. 2000MAR13)
    ** Return  : None
    */
   {
      /* calculate date/month/year from julian date */
      int ReturnValues[] = ADT_Functions.adt_yddmy(JulianDateInputValue);
      int DayValue = ReturnValues[0];
      int MonthValue = ReturnValues[1];
      int YearValue = ReturnValues[2];

      /* form character string representation from various components */
      String ReturnDateCharString = String.format("%04d%3s%02d",YearValue,
                    Months[MonthValue-1],DayValue);

      return ReturnDateCharString;
   }

   public static double[] distance_angle(double EndLatitudeInput,double EndLongitudeInput,
                                         double StartLatitudeInput, double StartLongitudeInput,
                                         int UnitFlagID)
   {
      double Distance_Intermediate=0.0;
      double Distance_Final=0.0;
      double Angle_Final=0.0;
      double StartLatitude_Radians=StartLatitudeInput*RADIANSCONSTANT;
      double StartLongitude_Radians=StartLongitudeInput*RADIANSCONSTANT;
      double EndLatitude_Radians=EndLatitudeInput*RADIANSCONSTANT;
      double EndLongitude_Radians=EndLongitudeInput*RADIANSCONSTANT;
      double StartLatitude_Radians_ACOS=Math.cos(StartLatitude_Radians);
      double StartLongitude_Radians_ACOS=Math.cos(StartLongitude_Radians);
      double StartLatitude_Radians_ASIN=Math.sin(StartLatitude_Radians);
      double StartLongitude_Radians_ASIN=Math.sin(StartLongitude_Radians);
      double EndLatitude_Radians_ACOS=Math.cos(EndLatitude_Radians);
      double EndLongitude_Radians_ACOS=Math.cos(EndLongitude_Radians);
      double EndLatitude_Radians_ASIN=Math.sin(EndLatitude_Radians);
      double EndLongitude_Radians_ASIN=Math.sin(EndLongitude_Radians);
      double COSCOS_Difference=(EndLatitude_Radians_ACOS*EndLongitude_Radians_ACOS)-
                               (StartLatitude_Radians_ACOS*StartLongitude_Radians_ACOS);
      double SINCOS_Difference=(EndLatitude_Radians_ACOS*EndLongitude_Radians_ASIN)-
                               (StartLatitude_Radians_ACOS*StartLongitude_Radians_ASIN);
      double LatitudeSIN_Difference=EndLatitude_Radians_ASIN-StartLatitude_Radians_ASIN;
      double AdditiveValue=(COSCOS_Difference*COSCOS_Difference)+
                           (SINCOS_Difference*SINCOS_Difference)+
                           (LatitudeSIN_Difference*LatitudeSIN_Difference);
      Distance_Intermediate=Math.sqrt(AdditiveValue);
    
      /* Distance_Final is distance in kilometers */
      Distance_Final=2.0*Math.asin(Distance_Intermediate/2.0)*EARTHRADIUSKM;
    
      if(UnitFlagID==2) {
         /** Conversion to Miles */
         Distance_Final=((69.0*Distance_Final)+55)/111.0;
      }
      if(UnitFlagID==3) {
         /** Conversion to Nautical Miles */
         Distance_Final=((60.0*Distance_Final)+55)/111.0;
      }
    
      /** Compute Final Angle */
      if(Math.abs(Distance_Final)>0.0001) {
         Angle_Final=(Math.sin(StartLongitude_Radians-EndLongitude_Radians)*
                      Math.sin((PI/2.0)-EndLatitude_Radians))/
                      Math.sin(Distance_Intermediate);
      }
      else {
         Angle_Final=0.0;
      }
      if(Math.abs(Angle_Final)>1.0) {
         Angle_Final=a_sign(1.000,Angle_Final);
      }
      Angle_Final=Math.asin(Angle_Final)/RADIANSCONSTANT;
      if(EndLatitude_Radians<StartLatitude_Radians) {
         Angle_Final=180.0-Angle_Final;
      }
      if(Angle_Final<0.0) {
         Angle_Final=360.0+Angle_Final;
      }

      return new double[] {Distance_Final, Angle_Final};
   }

   public static double[] distance_angle2(double StartLatitudeInput, double StartLongitudeInput,
                                         double DistanceInput, double AngleInput)
   /*
    ** Calculate a latitude and longitude position from an
    ** initial latitude/longitude and distance/angle values.
    ** Inputs  : StartLatitudeInput  - initial latitude
    **           StartLongitudeInput - initial longitude
    **           DistanceInput       - distance from initial position
    **           AngleInput          - angle from initial position
    ** Outputs : EndLatitude_Return  - derived latitude
    **           EndLongitude_Return - derived longitude
    ** Return  : None
    */
   {
      double StartLatitude_Radians=(90.0-StartLatitudeInput)*RADIANSCONSTANT;
      double StartLatitude_Radians_Flipped=StartLatitude_Radians;
      double StartLongitude_Radians=StartLongitudeInput*RADIANSCONSTANT;
      double ArgumentValue;

      if(StartLatitudeInput<0.0) {
        StartLatitude_Radians_Flipped = -(90.0+StartLatitudeInput)*RADIANSCONSTANT;
        StartLongitude_Radians = (StartLongitudeInput-180.0)*RADIANSCONSTANT;
        AngleInput = 360.0-AngleInput;
      }
      int AngleInput_Integer = (int)AngleInput;
      double Angle_Radians = -1.0*((double)((540-AngleInput_Integer)%360))*RADIANSCONSTANT;
      double Distance_Radians = (DistanceInput/111.1)*RADIANSCONSTANT;
      double Latitude_AACOS = Math.acos((Math.cos(StartLatitude_Radians)*Math.cos(Distance_Radians))+
                                        (Math.sin(StartLatitude_Radians)*
                                         Math.sin(Distance_Radians)*Math.cos(Angle_Radians)));
      double Longitude_AASIN = 0.0;
      double ATANValue = 0.0;
      if(Math.abs(Latitude_AACOS)>=0.0000001) {
         ArgumentValue = (Math.sin(Distance_Radians)*Math.sin(Angle_Radians))/
                          Math.sin(Latitude_AACOS);
         if(Math.abs(ArgumentValue)>1.0) {
            ArgumentValue = a_sign(1.0,ArgumentValue);
         }
         Longitude_AASIN=Math.asin(ArgumentValue);
         ATANValue = (Math.atan(Math.sin(ANGLE90DEGRADIANS-Angle_Radians)))/
                               (Math.tan(ANGLE90DEGRADIANS-Distance_Radians));
         if(ATANValue>StartLatitude_Radians_Flipped) {
            Longitude_AASIN = (2.0*ANGLE90DEGRADIANS)-Longitude_AASIN;
         }
      }
      Longitude_AASIN = StartLongitude_Radians-Longitude_AASIN;
      double EndLatitude = 90.0-(Latitude_AACOS/RADIANSCONSTANT);
      double EndLongitude = (double)((int)(10000*(Longitude_AASIN/
                                           RADIANSCONSTANT))%3600000)/10000.0;
      if(EndLongitude<-180.0) {
         EndLongitude = EndLongitude+360.0;
      }

      return new double[] {EndLatitude, EndLongitude};

   }

   public static int[] adt_oceanbasin(double LatitudeInput, double LongitudeInput)
   /*
    ** determine ocean basin given latitude and longitude position of storm
    ** Inputs  : LatitudeInput - latitude
    **           LongitudeInput - longitude
    ** Outputs : None
    ** Return  : basin type (0=atlantic,1=west pacific,2,east pacific,3=Indian)
    */
   {
      int BasinID_Local;
      int DomainID_Local;

      /* flip longitude for McIDAS-V */
      LongitudeInput = -1.0*LongitudeInput;
      
      if(((LongitudeInput<-180.0)||(LongitudeInput>180.0))||
         ((LatitudeInput<-90.0)||(LatitudeInput>90.0))) {
         BasinID_Local=-99;
         DomainID_Local=-99;
      } else {
         if(LatitudeInput>=0.0) {
            /** northern hemisphere */
            if(LongitudeInput<=-100.0) {
               BasinID_Local=1;      /** West Pacific */
            } else if((LongitudeInput>-100.0)&&(LongitudeInput<=-20.0)) {
               BasinID_Local=3;      /** Indian */
            } else if(LongitudeInput>=100.0) {
               BasinID_Local=2;      /** East Pacific */
            } else {
               /** -20 to ~+100 */
               if(LatitudeInput>20.0) {
                  BasinID_Local=0;      /** Atlantic */
               } else if(LatitudeInput<10.0) {
                  if(LongitudeInput<80.0) {
                     BasinID_Local=0;      /** Atlantic */
                  } else {
                     BasinID_Local=2;      /** East Pacific */
                  }
               } else {
                  /** latitude between 10 and 20 north */
                  /**
                   ** slope of line between (100W,20N) and (80W,10N) is 2
                   ** if slope of new point and (100,20)  > 2, storm is in atlantic
                   ** if slope of new point and (100,20) <= 2, storm is in pacific
                   */
                  double AtlEPacDivision=(100.0-LongitudeInput)/(20.0-LatitudeInput);
                  if(AtlEPacDivision>2.0) {
                     BasinID_Local=0;      /** Atlantic */
                  } else {
                     BasinID_Local=2;      /** East Pacific */
                  }
               }
            }
         } else {
            /** southern hemisphere */
            if(LongitudeInput<=-135.0) {
               BasinID_Local=1;      /** West Pacific */
            } else if((LongitudeInput>-135.0)&&(LongitudeInput<=-20.0)) {
               BasinID_Local=3;      /** Indian */
            } else if((LongitudeInput>-20.0)&&(LongitudeInput<=67.0)) {
               BasinID_Local=0;      /** Atlantic */
            } else {
               BasinID_Local=2;      /** East Pacific */
            }
         }
      }

      int DomainID_Input = ADT_Env.DomainID;
      if(DomainID_Input==-1) {
         /* automatically determined storm basin ID */
         /* if(BasinID_Local==0) { */
         if((BasinID_Local==0)||(BasinID_Local==2)) {
            DomainID_Local=0;    /* atlantic and East Pac only */
         }
         else {
            DomainID_Local=1;    /* west pacific and other regions */
         }
      }
      else {
         /* manually determined storm basin ID */
         DomainID_Local=DomainID_Input;
      }
      
      return new int[] { BasinID_Local, DomainID_Local };

   }

   public static double adt_slopecal( double  SearchTimeInterval,int SlopeInterceptFlag)
   /*
    ** Calculate slope or y-intercept of all points over SearchTimeInterval period
    ** Inputs  : SearchTimeInterval - time period to calculate slope or y-intercept
    **           SlopeInterceptFlag - flag value indicating slope or y-intercept
    **                                calculation and parameter to utilize
    **                            1 = Final T# (will return slope)
    **                            2 = Adjusted Raw T# (will return slope)
    **                            3 = latitude (will return y-intercept)
    **                            4 = longitude (will return y-intercept)
    **           HistoryCurrentPointer_Global contains current analysis information
    ** Outputs : None
    ** Return  : Slope or Y-Intercept value of line over time period desired
    */
   {
      double Slope;
      double SlopeYIntReturnValue = 0.0;
      int CounterMinimum = 4;                  /* counter minimum value */
      int RecDate;
      int RecTime;
      int RecLand;
      int Counter = 0;
      double XAxisValue = 0.0;
      double YAxisValue = 0.0;
      double FirstLonValue = 0.0;
      double HistoryRecTime;
      double XMean;
      double YMean;
      double XVariance;
      double YVariance;
      double CovarianceXY;
      double RValue;
      double YIntercept;
      double SumX = 0.0;                       /* X-axis sum value */
      double SumY = 0.0;                       /* Y-axis sum value */
      double SumSquaresX = 0.0;                /* X-axis sum squares value */
      double SumSquaresY = 0.0;                /* Y-axis sum squares value */
      double SumXY = 0.0;                      /* X*Y sum value */
      boolean FoundValidRecordTF = false;      /* found valid record logical */
      boolean OverWaterTF = false;             /* TC over water logical value */
      boolean FirstLonValueTF = true;          /* 1st lon value for extrap logical */

      int NumRecsHistory = ADT_History.HistoryNumberOfRecords();

      int ImageDate = ADT_History.IRCurrentRecord.date;
      int ImageTime = ADT_History.IRCurrentRecord.time;
      double CurrentTime = ADT_Functions.calctime(ImageDate,ImageTime);
      double TimeThreshold = CurrentTime-(SearchTimeInterval/24.0);

      boolean LandFlagTF = ADT_Env.LandFlagTF;
      double InitStrengthValue = ADT_Env.InitRawTValue;

      int XInc = 0;
      while(XInc<NumRecsHistory) {
         RecDate = ADT_History.HistoryFile[XInc].date;
         RecTime = ADT_History.HistoryFile[XInc].time;
         HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
         RecLand = ADT_History.HistoryFile[XInc].land;


         if((HistoryRecTime<CurrentTime)&&(HistoryRecTime>=TimeThreshold)) {
            OverWaterTF = true;
            if(SlopeInterceptFlag<3) {
               if(((LandFlagTF)&&(RecLand==1))||(InitStrengthValue<1.0)) {
                  OverWaterTF = false;
               }
            }
            if(OverWaterTF) {
              XAxisValue = (double)(CurrentTime-HistoryRecTime);
               if(SlopeInterceptFlag==1) {
                  YAxisValue = ADT_History.HistoryFile[XInc].Tfinal;
               }
               if(SlopeInterceptFlag==2) {
                  YAxisValue = ADT_History.HistoryFile[XInc].Traw;
               }
               if(SlopeInterceptFlag==3) {
                  YAxisValue = ADT_History.HistoryFile[XInc].latitude;
               }
               if(SlopeInterceptFlag==4) {
                  YAxisValue = ADT_History.HistoryFile[XInc].longitude;
                  if(FirstLonValueTF) {
                     FirstLonValue = YAxisValue;
                     FirstLonValueTF = false;
                  } else {
                     if((FirstLonValue>100.0)&&(YAxisValue<-100.0)) {
                        /** dateline cross W to E */
                        YAxisValue = YAxisValue+360.0;
                     }
                     if((FirstLonValue<-100.0)&&(YAxisValue>100.0)) {
                        /** dateline cross E to W */
                        YAxisValue = YAxisValue-360.0;
                     }
                  }
               }
               SumX = SumX+XAxisValue;
               SumY = SumY+YAxisValue;
               SumSquaresX = SumSquaresX+(XAxisValue*XAxisValue);
               SumSquaresY = SumSquaresY+(YAxisValue*YAxisValue);
               SumXY = SumXY+(YAxisValue*XAxisValue);
               Counter++;
               FoundValidRecordTF = true;
            }
         } else {
            if(FoundValidRecordTF) {
               break;
            }
         }
         XInc++;
      }
      /** if calculating slope of Final T# values, add in current value */
      if(SlopeInterceptFlag<=2) {
         CounterMinimum=6;
         /** add current record to slope calculation */
         if(SlopeInterceptFlag==1) {
            YAxisValue = ADT_History.IRCurrentRecord.Tfinal;
         }
         if(SlopeInterceptFlag==2) {
            YAxisValue = ADT_History.IRCurrentRecord.Traw;
         }
         SumY = SumY+YAxisValue;
         SumSquaresY = SumSquaresY+(YAxisValue*YAxisValue);
         Counter++;
      }
    
      /*
       ** compute least squares fit of line for data points
       ** using the equation  Y = Y* + r(varX/varY)(X - X*) = Mx + B
       ** X* = mean of X values (time values)  = XMean
       ** Y* = mean of Y values (T# values)    = YMean
       ** varX = variance of X                 = XVariance
       ** varY = variance of Y                 = YVariance
       ** r = covariance - Y*X*                = RValue
       ** M = slope of line (desired value)    = RValue*(sqrt(YVariance/XVariance))
       ** B = y-intercept                      = YMean-(slopecal*XMean)
       */
      /** must have more than CounterMinimum data values to calculate slope */
      if(Counter<CounterMinimum) {
         if((SlopeInterceptFlag==3)||(SlopeInterceptFlag==4)) {
            Slope = 999.99; 
         } else {
            Slope = 0.0;
         }
         SlopeYIntReturnValue = Slope;
      } else {
         XMean = SumX/(double)Counter;
         YMean = SumY/(double)Counter;
         XVariance = (SumSquaresX/(double)Counter)-(XMean*XMean);
         YVariance = (SumSquaresY/(double)Counter)-(YMean*YMean);
         CovarianceXY = (SumXY/(double)Counter)-(XMean*YMean);
         RValue = CovarianceXY/Math.sqrt(XVariance*YVariance);
         if((Math.abs(XVariance)<=0.0001)||(Math.abs(YVariance)<=0.0001)) {
            Slope = 0.0;
         } else {
            Slope = RValue*(Math.sqrt(YVariance/XVariance));
         }
         Slope = (double)((int)(Slope*10.0))/10.0;
         if((SlopeInterceptFlag==3)||(SlopeInterceptFlag==4)) {
            YIntercept = YMean-(Slope*XMean);
            if(SlopeInterceptFlag==4) {
               if(YIntercept<-180.0) YIntercept = YIntercept+360.0;
               if(YIntercept>180.0) YIntercept = YIntercept-360.0;
            }
            /** y-intercept for latitude/longitude extrapolation */
            SlopeYIntReturnValue = YIntercept;
         } else {
            /** slope for Final T# slope calculation */
            SlopeYIntReturnValue = Slope;
         }
      }

      return SlopeYIntReturnValue;
   }

   public static double adt_getpwval( int PressureWindIDValue, double CIValue,
                                      double LatitudeInput, double LongitudeInput)
   /*
    ** Obtain pressure or wind value (for Atlantic or
    ** West Pacific storms) given the intensity estimate
    ** value.
    ** Inputs  : PressureWindIDValue  - flag for wind (1) or pressure (0) output
    **           CIValue              - Current Intensity (CI) value
    **           Latitude_Input       - Latitude value of analysis
    **           Longitude_Input      - Longitude value of analysis
    ** Outputs : None
    ** Return  : Pressure or Wind Speed value
    */
   {
      double PWReturnValue = -999.0;
      double GaleRadius34_Climo = 0.0;
      double ROCI_Local = 0.0;
      double R34_Local= 0.0;
      int XInc=2;
      int RetErr;

      /** use traditional MSLP/Wind-T# conversion */
      /** determine correct pressure/wind array bin */
      while(((CIValue-0.001)>PW_TnoValues[XInc])&&(XInc<82)) { 
         XInc++; 
      }
    
      int[] ReturnValues = ADT_Functions.adt_oceanbasin(LatitudeInput,LongitudeInput);
      int BasinID = ReturnValues[0];
      /* int DomainID = ADT_Env.DomainID; */
      int DomainID = ReturnValues[1];

      boolean UseCKZTF = ADT_Env.UseCKZTF;
      double CKZGaleRadius34 = ADT_Env.CKZGaleRadius;
      double CKZPenv = ADT_Env.CKZPenv;

      /** convert CI value to wind/pressure value */
      if(PressureWindIDValue==1) {
         PWReturnValue = PW_WindValues[XInc];                      /* WIND */
      } else {
         PWReturnValue = PW_PressureValues[DomainID][XInc];  /* PRESSURE */
         if(UseCKZTF) {
            /**
             ** use new Knaff/Zehr T#->MSLP conversion.
             ** Need Vmax calculation and two command line input values
             */
            if(BasinID==2) {
               GaleRadius34_Climo = 82.0;
            } else {
               GaleRadius34_Climo = 107.0;
            }
            double StormSpeed = 11.0;  /* 11 knots -- climo value */
            double Vmax_Local = PW_WindValues[XInc];                    /* WIND */

            /** System.out.printf("CKZGaleRadius34=%f\n",CKZGaleRadius34); */
            if(CKZGaleRadius34<0.0) {
               ROCI_Local = Math.abs(CKZGaleRadius34);
               R34_Local = (0.354*ROCI_Local)+13.3;
            } else {
               R34_Local = CKZGaleRadius34;
            }

            /** System.out.printf("R34_local=%f\n",R34_Local); */
            /** System.out.printf("LatitudeInput=%f\n",LatitudeInput); */
            double Vstorm_Local = Vmax_Local-(1.5*Math.pow(StormSpeed,0.63));
            /** System.out.printf("Vstorm_local=%f\n",Vstorm_Local); */
            double EXP_Local = 0.1147+(0.0055*Vstorm_Local)-(0.001*(Math.abs(LatitudeInput)-25.0));
            /** System.out.printf("EXP_local=%f\n",EXP_Local); */
            double Vstorm_500 = (R34_Local/9.0)-3.0;
            /** System.out.printf("Vstorm_500=%f\n",Vstorm_500); */
            double Vstorm_500c = Vstorm_Local*Math.pow(((66.785-(0.09102*Vstorm_Local)+
                                (1.0619*(Math.abs(LatitudeInput)-25.0)))/500.0),EXP_Local);
            /** System.out.printf("Vstorm_500c=%f\n",Vstorm_500c); */
            double S = Math.max((Vstorm_500/Vstorm_500c),0.4);
            /** System.out.printf("S=%f\n",S); */
            PWReturnValue = 23.286-(0.483*Vstorm_Local)-Math.pow((Vstorm_Local/24.254),2)-
                            (12.587*S)-(0.483*Math.abs(LatitudeInput))+CKZPenv;
            if(PWReturnValue>=(CKZPenv-1.0)) {
               PWReturnValue = CKZPenv-2.0;
            }
            /** System.out.printf("PWReturnValue=%f\n",PWReturnValue); */
            ADT_History.IRCurrentRecord.r34 = (int)CKZGaleRadius34;
            ADT_History.IRCurrentRecord.MSLPenv = (int)CKZPenv;
         }
      }

      return PWReturnValue;

   }

   public static String adt_sattypes( int SatelliteIDInput)
   /*
    ** obtain satellite name given ID number
    ** Inputs  : SatelliteIDInput - Internal ADT satellite ID number
    ** Outputs : csat - Satellite character string ID
    ** Return  : None
    */
   {
     String SatelliteName = "";

     if (SatelliteIDInput<0) {
        SatelliteName = String.format("%s","MSNG");
     } else {
        SatelliteName = String.format("%s"," OTHER");
        if(SatelliteIDInput==21) SatelliteName = String.format("%s"," GOES1");
        if(SatelliteIDInput==23) SatelliteName = String.format("%s"," GOES2");
        if(SatelliteIDInput==25) SatelliteName = String.format("%s"," GOES3");
        if(SatelliteIDInput==27) SatelliteName = String.format("%s"," GOES4");
        if(SatelliteIDInput==29) SatelliteName = String.format("%s"," GOES5");
        if(SatelliteIDInput==31) SatelliteName = String.format("%s"," GOES6");
        if(SatelliteIDInput==33) SatelliteName = String.format("%s"," GOES7");
        if(SatelliteIDInput==34) SatelliteName = String.format("%s","  FY2B");
        if(SatelliteIDInput==35) SatelliteName = String.format("%s","  FY2C");
        if(SatelliteIDInput==36) SatelliteName = String.format("%s","  FY2D");
        if(SatelliteIDInput==37) SatelliteName = String.format("%s","  FY2E");
        if(SatelliteIDInput==38) SatelliteName = String.format("%s","  FY2F");
        if(SatelliteIDInput==39) SatelliteName = String.format("%s","  FY2G");
        if(SatelliteIDInput==40) SatelliteName = String.format("%s","  FY2H");
        if((SatelliteIDInput>=42)&&(SatelliteIDInput<=45)) {
           SatelliteName = String.format("%s","  NOAA");
        }
        if(SatelliteIDInput==51) SatelliteName = String.format("%s","  MSG1");
        if(SatelliteIDInput==52) SatelliteName = String.format("%s","  MSG2");
        if(SatelliteIDInput==53) SatelliteName = String.format("%s","  MSG3");
        if(SatelliteIDInput==54) SatelliteName = String.format("%s","  MET3");
        if(SatelliteIDInput==55) SatelliteName = String.format("%s","  MET4");
        if(SatelliteIDInput==56) SatelliteName = String.format("%s","  MET5");
        if(SatelliteIDInput==57) SatelliteName = String.format("%s","  MET6");
        if(SatelliteIDInput==58) SatelliteName = String.format("%s","  MET7");
        if((SatelliteIDInput>=60)&&(SatelliteIDInput<=69)) {
           SatelliteName = String.format("%s","  NOAA");
        }
        if(SatelliteIDInput==70) SatelliteName = String.format("%s"," GOES8");
        if(SatelliteIDInput==72) SatelliteName = String.format("%s"," GOES9");
        if(SatelliteIDInput==74) SatelliteName = String.format("%s","GOES10");
        if(SatelliteIDInput==76) SatelliteName = String.format("%s","GOES11");
        if(SatelliteIDInput==78) SatelliteName = String.format("%s","GOES12");
        if(SatelliteIDInput==83) SatelliteName = String.format("%s","  GMS4");
        if(SatelliteIDInput==83) SatelliteName = String.format("%s","  GMS5");
        if(SatelliteIDInput==84) SatelliteName = String.format("%s","MTSAT1");
        if(SatelliteIDInput==85) SatelliteName = String.format("%s","MTSAT2");
        if((SatelliteIDInput>=87)&&(SatelliteIDInput<=95)) {
           SatelliteName = String.format("%s","  DMSP");
        }
        if(SatelliteIDInput==95) SatelliteName = String.format("%s","  FY1B");
        if(SatelliteIDInput==96) SatelliteName = String.format("%s","  FY1C");
        if(SatelliteIDInput==97) SatelliteName = String.format("%s","  FY1D");
        if((SatelliteIDInput>=101)&&(SatelliteIDInput<=171)) {
           SatelliteName = String.format("%s"," MODIS");
        }
        if(SatelliteIDInput==180) SatelliteName = String.format("%s","GOES13");
        if(SatelliteIDInput==182) SatelliteName = String.format("%s","GOES14");
        if(SatelliteIDInput==184) SatelliteName = String.format("%s","GOES15");
        if(SatelliteIDInput==186) SatelliteName = String.format("%s","GOES16");
        if((SatelliteIDInput>=195)&&(SatelliteIDInput<=196)) {
           SatelliteName = String.format("%s","  DMSP");
        }
        if(SatelliteIDInput==230) SatelliteName = String.format("%s","KLPNA1");
        if(SatelliteIDInput==240) SatelliteName = String.format("%s","MetOpA");
        if(SatelliteIDInput==241) SatelliteName = String.format("%s","MetOpB");
        if(SatelliteIDInput==242) SatelliteName = String.format("%s","MetOpC");
     }
   
     return SatelliteName;
    
   }

   public static String adt_atcffilename( String StormNameInput, String StormSiteIDInput )
   /*
    ** Derived ATCF file name using various input parameters
    ** Inputs  : StormName_Input - Storm ID character string value (e.g. 12L)
    ** Outputs : ATCFFileName - Complete file name and path for ATCF file
    ** Return  : 0
    */
   {
   
      String ATCFFileName = "";

      int CurDate = ADT_History.IRCurrentRecord.date;
      int CurTime = ADT_History.IRCurrentRecord.time;
      int ReturnValues[] = ADT_Functions.adt_yddmy(CurDate);
      int DayValue = ReturnValues[0];
      int MonthValue = ReturnValues[1];
      int YearValue = ReturnValues[2];

      int HHMMTime = CurTime/100;
      ATCFFileName += String.format("%s%5s_%4d%02d%02d%04d_%3s_%3s",
             StormSiteIDInput,"_DVTO",YearValue,MonthValue,DayValue,HHMMTime,StormNameInput,"FIX");
 
      return ATCFFileName;
   }

   public static double adt_XLZA(double rlat, double rlon, double plat, double plon)
   /*   approximate local zenith angle (angle from vertical to the satellite)
    **  Inputs  : rlat - latitude of target position on earth
    **            rlon - longitude of target position on earth
    **            plat - latitude of sub-satellite point
    **            plon - longitude of sub-satellite point
    **  Outputs : ang  - local zenith angle
    */
   {
   
      double r = 6371.229;           /**  mean earth radius   */
      double sd = r + 35790.0;       /**  mean distance from satellite to earth center  */
    
      rlat = Math.abs(rlat);
       /**  For the longitude in East of Greenwich  */
      if (rlon < 0) rlon = rlon + 360.0;
       /** rlon = Math.abs(rlon);  is this even needed with above + 360.0? */
      double dif = Math.abs(plon - rlon);
    
      double cosal = Math.cos(deg_to_rad(rlat)) * Math.cos(deg_to_rad(dif));
      double dp = Math.sqrt(Math.pow(r, 2) + Math.pow(sd, 2) - (2.0 * r * sd * cosal));
      double yy1 = Math.sqrt(1-Math.pow(cosal, 2)) / cosal;
      double alpha = Math.atan(yy1);
      double zz = sd * Math.sin(alpha) / dp;
      double yy2 = zz / (Math.sqrt(1.0 - Math.pow(zz, 2)));
      double zaz = rad_to_deg(Math.atan(yy2));

      return zaz;
   
   }

}
