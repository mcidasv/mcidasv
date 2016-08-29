package edu.wisc.ssec.mcidasv.control.adt;

import java.io.IOException;
import java.lang.Math;
import java.lang.String;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import visad.FlatField;

@SuppressWarnings("unused")

public class ADT_ReadIRImage {

    private static final Logger logger = LoggerFactory.getLogger(ADT_ReadIRImage.class);
    
   public ADT_ReadIRImage() {

   }

   public static class ImgCoeffs {
       String sat_id;
       int sat_num;
       int chan;
       int det;
       float scal_m;
       float scal_b;
       float side;
       float conv_n;
       float conv_a;
       float conv_b;
       float conv_g;

       public ImgCoeffs(List<String> toks) {
               sat_id = toks.get(0);
               sat_num = Integer.parseInt(toks.get(1));
               chan = Integer.parseInt(toks.get(2));
               det = Integer.parseInt(toks.get(3));
               scal_m = Float.parseFloat(toks.get(4));
               scal_b = Float.parseFloat(toks.get(5));
               side = Float.parseFloat(toks.get(6));
               conv_n = Float.parseFloat(toks.get(7));
               conv_a = Float.parseFloat(toks.get(8));
               conv_b = Float.parseFloat(toks.get(9));
               conv_g = Float.parseFloat(toks.get(10));

       }
       public String getSat_id() {
           return sat_id;
       }

   }
   
   public static void ReadIRDataFile(FlatField satgrid, float cenlat, float cenlon, 
           int SatelliteID, int satChannel, boolean isTemperature) throws IOException {
       
                /*
                 * Retrieve temperatures from image. This to be done in IDV
                 */

                GridUtil.Grid2D g2d = null;
                float[][] temps = null;
                float[][][] satimage = null;
                float[][] lons = null;
                float[][] lats = null;
                int numx = 200;
                int numy = 200;
                float[][] LocalLatitude = new float[200][200];
                float[][] LocalLongitude = new float[200][200];
                float[][] LocalTemperature = new float[200][200];

                try {
                        g2d = GridUtil.makeGrid2D(satgrid);
                        lons = g2d.getlons();
                        lats = g2d.getlats();

                } catch (Exception re) {
                }

                /* now spatial subset numx by numy */
                GridUtil.Grid2D g2d1 = spatialSubset(g2d, cenlat, cenlon, numx, numy);

                satimage = g2d1.getvalues();
                float[][] temp0 = satimage[0];
                int imsorc = SatelliteID, imtype = satChannel;

                if (isTemperature)
                        temps = temp0;
                else
                        temps = im_gvtota(numx, numy, temp0, imsorc, imtype);

                ADT_Data.IRData_NumberRows = numy;
                ADT_Data.IRData_NumberColumns = numx;
                ADT_Data.IRData_CenterLatitude = cenlat;
                ADT_Data.IRData_CenterLongitude = cenlon;

                LocalTemperature = temps;
                LocalLatitude = g2d1.getlats();
                LocalLongitude = g2d1.getlons();

                for(int XInc=0;XInc<numx;XInc++) {
                    for(int YInc=0;YInc<numy;YInc++) {
                        /* must flip x/y to y/x for ADT automated routines */
                        ADT_Data.IRData_Latitude[YInc][XInc] = LocalLatitude[XInc][YInc]; 
                        ADT_Data.IRData_Longitude[YInc][XInc] = LocalLongitude[XInc][YInc];
                        ADT_Data.IRData_Temperature[YInc][XInc] = LocalTemperature[XInc][YInc];
                    }
                }
                int CenterXPos = ADT_Data.IRData_NumberColumns/2;
                int CenterYPos = ADT_Data.IRData_NumberRows/2;

                double LocalValue[] = ADT_Functions.distance_angle(ADT_Data.IRData_Latitude[CenterYPos][CenterXPos],
                                        ADT_Data.IRData_Longitude[CenterYPos][CenterXPos],
                                        ADT_Data.IRData_Latitude[CenterYPos+1][CenterXPos],
                                        ADT_Data.IRData_Longitude[CenterYPos][CenterXPos],1);
                ADT_Data.IRData_ImageResolution = LocalValue[0];
      
                ADT_History.IRCurrentRecord.date = ADT_Data.IRData_JulianDate;
                ADT_History.IRCurrentRecord.time = ADT_Data.IRData_HHMMSSTime;
                ADT_History.IRCurrentRecord.latitude = ADT_Data.IRData_CenterLatitude;
                ADT_History.IRCurrentRecord.longitude = ADT_Data.IRData_CenterLongitude;
                ADT_History.IRCurrentRecord.sattype = SatelliteID;
                
                int RetVal[] = ADT_Functions.adt_oceanbasin(ADT_Data.IRData_CenterLatitude,ADT_Data.IRData_CenterLongitude);
                int OceanBasinID = RetVal[0];
                ADT_Env.DomainID = RetVal[1];
                /** System.out.printf("lat=%f lon=%f domainID=%d\n",ADT_Data.IRData_CenterLatitude,ADT_Data.IRData_CenterLongitude,ADT_Env.DomainID); */
         }
   
         private static GridUtil.Grid2D spatialSubset(GridUtil.Grid2D g2d, float cenlat,
                                                         float cenlon, int numx, int numy) {
             float[][] lats = g2d.getlats();
             float[][] lons = g2d.getlons();
             float[][][] values = g2d.getvalues();
             float[][] slats = new float[numx][numy];
             float[][] slons = new float[numx][numy];
             float[][][] svalues = new float[1][numx][numy];

             int ly = lats[0].length;
             int ly0 = ly / 2;
             int lx = lats.length;
             logger.debug("lenx: " + lx + ", leny: " + ly);
             int lx0 = lx / 2;
             int ii = numx / 2, jj = numy / 2;

             for (int j = 0; j < ly - 1; j++) {
                 if (Float.isNaN(lats[lx0][j]))
                  continue;
                 if ((lats[lx0][j] > cenlat) && (lats[lx0][j + 1] < cenlat)) {
                  jj = j;
                 }
             }
             for (int i = 0; i < lx - 1; i++) {
                 if (Float.isNaN(lons[i][ly0]))
                  continue;
                 if ((lons[i][ly0] < cenlon) && (lons[i + 1][ly0] > cenlon)) {
                  ii = i;
                 }
             }
             int startx = ii - (numx / 2 - 1);
             int starty = jj - (numy / 2 - 1);
             logger.debug("startx: " + startx + ", starty: " + starty);
             logger.debug("numx: " + numx + ", numy: " + numy);
             
             if (startx < 0)
                 startx = 0;
             if (starty < 0)
                 starty = 0;
             for (int i = 0; i < numx; i++) {
                 for (int j = 0; j < numy; j++) {
                     try {
                  slats[i][j] = lats[i + startx][j + starty];
                     } catch (ArrayIndexOutOfBoundsException aioobe) {
                         slats[i][j] = Float.NaN;
                     }
                     try {
                  slons[i][j] = lons[i + startx][j + starty];
                     } catch (ArrayIndexOutOfBoundsException aioobe) {
                         slats[i][j] = Float.NaN;
                     }
                     try {
                  svalues[0][i][j] = values[0][i + startx][j + starty];
                     } catch (ArrayIndexOutOfBoundsException aioobe) {
                         slats[i][j] = Float.NaN;
                     }
                 }
             }

             return new GridUtil.Grid2D(slats, slons, svalues);
         }
   
      private static float[][] im_gvtota(int nx, int ny, float[][] gv, int imsorc, int imtype)
      /**
       * im_gvtota
       *
       * This subroutine converts GVAR counts to actual temperatures based on the
       * current image set in IM_SIMG.
       *
       * im_gvtota ( int *nvals, unsigned int *gv, float *ta, int *iret )
       *
       * Input parameters: *nvals int Number of values to convert *gv int Array of
       * GVAR count values
       *
       * Output parameters: *ta float Array of actual temperatures *iret int
       * Return value = -1 - could not open table = -2 - could not find match
       *
       *
       * Log: D.W.Plummer/NCEP 02/03 D.W.Plummer/NCEP 06/03 Add coeff G for 2nd
       * order poly conv T. Piper/SAIC 07/06 Added tmpdbl to eliminate warning
       */
      {
          double c1 = 1.191066E-5;
          double c2 = 1.438833;

          int ii, ip, chan, found;
          double Rad, Teff, tmpdbl;
          float[][] ta = new float[nx][ny];
          int iret;
          String fp = "/ucar/unidata/data/storm/ImgCoeffs.tbl";

          iret = 0;

          for (ii = 0; ii < nx; ii++) {
                  for (int jj = 0; jj < ny; jj++) {
                          ta[ii][jj] = Float.NaN;
                  }
          }

          /*
           * Read in coefficient table if necessary.
           */
          String s = null;
          try {
                  s = IOUtil.readContents(fp);
          } catch (Exception re) {
          }

          int i = 0;
          ImgCoeffs[] ImageConvInfo = new ImgCoeffs[50];
          for (String line : StringUtil.split(s, "\n", true, true)) {
                  if (line.startsWith("!")) {
                      continue;
              }
              List<String> stoks = StringUtil.split(line, " ", true, true);

              ImageConvInfo[i] = new ImgCoeffs(stoks);
              ;
              i++;
          }
          int nImgRecs = i;
          found = 0;
          ii = 0;
          while ((ii < nImgRecs) && (found == 0)) {

              tmpdbl = (double) (ImageConvInfo[ii].chan - 1)
                              * (ImageConvInfo[ii].chan - 1);
              chan = G_NINT(tmpdbl);

              if ((imsorc == ImageConvInfo[ii].sat_num) && (imtype == chan)) {
                      found = 1;
              } else {
                      ii++;
              }

          }

          if (found == 0) {
              iret = -2;
              return null;
          } else {

              ip = ii;
              for (ii = 0; ii < nx; ii++) {
                  for (int jj = 0; jj < ny; jj++) {

                          /*
                           * Convert GVAR count (gv) to Scene Radiance
                           */
                          Rad = ((double) gv[ii][jj] - ImageConvInfo[ip].scal_b) /
                          /* ------------------------------------- */
                          ImageConvInfo[ip].scal_m;

                          Rad = Math.max(Rad, 0.0);

                          /*
                           * Convert Scene Radiance to Effective Temperature
                           */
                          Teff = (c2 * ImageConvInfo[ip].conv_n)
                                          /
                                          /*
                                           * --------------------------------------------------
                                           * -----
                                           */
                                          (Math.log(1.0
                                                          + (c1 * Math.pow(ImageConvInfo[ip].conv_n,
                                                                          3.0)) / Rad));
                          /*
                           * Convert Effective Temperature to Temperature
                           */
                          ta[ii][jj] = (float) (ImageConvInfo[ip].conv_a
                                          + ImageConvInfo[ip].conv_b * Teff + ImageConvInfo[ip].conv_g
                                          * Teff * Teff);
                  }
              }
          }

          return ta;

      }

      public static int G_NINT(double x) {
          return (((x) < 0.0F) ? ((((x) - (float) ((int) (x))) <= -.5f) ? (int) ((x) - .5f)
                          : (int) (x))
                          : ((((x) - (float) ((int) (x))) >= .5f) ? (int) ((x) + .5f)
                                          : (int) (x)));
      }
}
