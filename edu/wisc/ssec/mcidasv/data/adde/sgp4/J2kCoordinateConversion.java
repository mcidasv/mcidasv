/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
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

package edu.wisc.ssec.mcidasv.data.adde.sgp4;

//import name.gano.astro.AstroConst;
//import name.gano.astro.MathUtils;

/**
 *
 * @author Shawn
 */
public class J2kCoordinateConversion
{
    public static enum Opt
    {
        e80,
        e96,
        e00a,
        e00b
    }

    public static enum Direction
    {
        to,
        from
    }

/* -----------------------------------------------------------------------------
*
*                           function teme_j2k
*
*  this function transforms a vector between the true equator mean equinox system,
*    (teme) and the mean equator mean equinox (j2000) system.
*
*  author        : david vallado                  719-573-2600   25 jun 2002
*
*  revisions
*    vallado     - add order                                     29 sep 2002
*    vallado     - conversion to c++                             21 feb 2005
*
*  inputs          description                    range / units
*    rteme       - position vector of date
*                    true equator, mean equinox   km
*    vteme       - velocity vector of date
*                    true equator, mean equinox   km/s
*    ateme       - acceleration vector of date
*                    true equator, mean equinox   km/s2
*    direct      - direction of transfer          eFrom, 'TOO '
*    iau80rec    - record containing the iau80 constants rad
*    ttt         - julian centuries of tt         centuries
*    eqeterms    - number of terms for eqe        0, 2
*    opt1        - option for processing          a - complete nutation
*                                                 b - truncated nutation
*                                                 c - truncated transf matrix
*
*  outputs       :
*    rj2k        - position vector j2k            km
*    vj2k        - velocity vector j2k            km/s
*    aj2k        - acceleration vector j2k        km/s2
*
*  locals        :
*    prec        - matrix for mod - j2k
*    nutteme     - matrix for mod - teme - an approximation for nutation
*    tm          - combined matrix for teme
*
*  coupling      :
*   precess      - rotation for precession        j2k - mod
*   truemean     - rotation for truemean          j2k - teme
*
*  references    :
*    vallado       2007, 236
* --------------------------------------------------------------------------- */
//returns the transformation matrix
public static double[][] teme_j2k
     (
       //double rteme[3], double vteme[3], double ateme[3],
       Direction direct,
       //double rj2k[3],  double vj2k[3],  double aj2k[3],
       //iau80data& iau80rec,
       double ttt, int order, int eqeterms, char optteme
     )
      {
        //std::vector< std::vector<double> >  prec, nutteme, temp[3][3], tempmat, nuttemep, precp;
        double[][] tempmat = new double[3][3];

//        double psia, wa, epsa, chia, deltapsi, trueeps, meaneps,
//               omega, thetasa;
//        double[] omegaearth = new double[3];
//        double[] omgxr = new double[3];
//        double[] omgxomgxr = new double[3];
//        double[] omgxv = new double[3];
//        double[] tempvec1 = new double[3];
//        double[] tempvec = new double[3];

        double[][] prec = precess ( ttt, Opt.e80);//,  psia,wa,epsa,chia );
        double[][] nutteme = truemean( ttt,order,eqeterms,optteme );

        if (direct == Direction.to)
          {
            tempmat = matmult( prec, nutteme, 3, 3, 3 );
//            double[] rj2k = matvecmult( tempmat, rteme);
//            double[] vj2k = matvecmult( tempmat, vteme);
//            double[] aj2k = matvecmult( tempmat, ateme);
          }
          else
          {
            double[][] nuttemep = mattrans(nutteme, 3, 3 );
            double[][] precp = mattrans(prec, 3, 3 );

            tempmat = matmult( nuttemep, precp, 3, 3, 3 );

//            double[] rteme = matvecmult(tempmat, rj2k );
//            double[] vteme = matvecmult(tempmat, vj2k);
//            double[] ateme = matvecmult(tempmat, aj2k);
          }

        return tempmat;
      }  // procedure teme_j2k

/* -----------------------------------------------------------------------------
*
*                           function precess
*
*  this function calulates the transformation matrix that accounts for the effects
*    of precession. both the 1980 and 2000 theories are handled. note that the
*    required parameters differ a little.
*
*  author        : david vallado                  719-573-2600   25 jun 2002
*
*  revisions
*    vallado     - conversion to c++                             21 feb 2005
*    vallado     - misc updates, nomenclature, etc               23 nov 2005
*
*  inputs          description                    range / units
*    ttt         - julian centuries of tt
*    opt         - method option                  e00a, e00b, e96, e80
*
*  outputs       :
*    prec        - transformation matrix for mod - j2000 (80 only)
*    psia        - cannonical precession angle    rad    (00 only)
*    wa          - cannonical precession angle    rad    (00 only)
*    epsa        - cannonical precession angle    rad    (00 only)
*    chia        - cannonical precession angle    rad    (00 only)
*    prec        - matrix converting from "mod" to gcrf
*
*  locals        :
*    zeta        - precession angle               rad
*    z           - precession angle               rad
*    theta       - precession angle               rad
*    oblo        - obliquity value at j2000 epoch "//
*
*  coupling      :
*    none        -
*
*  references    :
*    vallado       2007, 217, 228
* --------------------------------------------------------------------------- */
public static double[][] precess
     (
       double ttt,      Opt opt//,
       //double& psia,    double& wa,    double& epsa,  double& chia,
       //std::vector< std::vector<double> > &prec
     )
     {
       double[][] prec = new double[3][3];//.resize(3);  // rows
       //for (std::vector< std::vector<double> >::iterator it=prec.begin(); it != prec.end();++it)
       //   it->resize(3);
       //std::vector< std::vector<double> > p1, p2, p3, p4, tr1, tr2;
       double[][] p1= new double[3][3];
       double[][] p2= new double[3][3];
       double[][] p3= new double[3][3];
       double[][] p4= new double[3][3];
       double[][] tr1= new double[3][3];
       double[][] tr2= new double[3][3];

       // since not returning these for now
       double psia, wa, epsa, chia;

       double convrt, zeta, theta, z, coszeta, sinzeta, costheta, sintheta,
              cosz, sinz, oblo;
       //char iauhelp;
       //sethelp(iauhelp, ' ');

       convrt = Math.PI / (180.0 * 3600.0);

       // ------------------- iau 77 precession angles --------------------
       if ((opt == Opt.e80) | (opt == Opt.e96))
         {
           oblo =  84381.448; // "
           psia =  (( - 0.001147 * ttt - 1.07259 ) * ttt + 5038.7784 ) * ttt; // "
           wa   =  (( - 0.007726 * ttt + 0.05127 ) * ttt )                 + oblo;
           epsa =  ((   0.001813 * ttt - 0.00059 ) * ttt -   46.8150 ) * ttt + oblo;
           chia =  (( - 0.001125 * ttt - 2.38064 ) * ttt +   10.5526 ) * ttt;

           zeta =  ((   0.017998 * ttt + 0.30188 ) * ttt + 2306.2181 ) * ttt; // "
           theta=  (( - 0.041833 * ttt - 0.42665 ) * ttt + 2004.3109 ) * ttt;
           z    =  ((   0.018203 * ttt + 1.09468 ) * ttt + 2306.2181 ) * ttt;
         }
         // ------------------ iau 00 precession angles -------------------
         else
         {
           oblo =  84381.406; // "
           psia =  (((( -0.0000000951 * ttt + 0.000132851 ) * ttt - 0.00114045 ) * ttt - 1.0790069 ) * ttt + 5038.481507 ) * ttt; // "
           wa   =  ((((  0.0000003337 * ttt - 0.000000467 ) * ttt - 0.00772503 ) * ttt + 0.0512623 ) * ttt -    0.025754 ) * ttt + oblo;
           epsa =  (((( -0.0000000434 * ttt - 0.000000576 ) * ttt + 0.00200340 ) * ttt - 0.0001831 ) * ttt -   46.836769 ) * ttt + oblo;
           chia =  (((( - 0.0000000560 * ttt + 0.000170663 ) * ttt - 0.00121197 ) * ttt - 2.3814292 ) * ttt +   10.556403 ) * ttt;

           zeta =  (((( - 0.0000003173 * ttt - 0.000005971 ) * ttt + 0.01801828 ) * ttt + 0.2988499 ) * ttt + 2306.083227 ) * ttt + 2.650545; // "
           theta=  (((( - 0.0000001274 * ttt - 0.000007089 ) * ttt - 0.04182264 ) * ttt - 0.4294934 ) * ttt + 2004.191903 ) * ttt;
           z    =  ((((   0.0000002904 * ttt - 0.000028596 ) * ttt + 0.01826837 ) * ttt + 1.0927348 ) * ttt + 2306.077181 ) * ttt - 2.650545;
         }

       // convert units to rad
       psia = psia  * convrt;
       wa   = wa    * convrt;
       oblo = oblo  * convrt;
       epsa = epsa  * convrt;
       chia = chia  * convrt;

       zeta = zeta  * convrt;
       theta= theta * convrt;
       z    = z     * convrt;

       if ((opt == Opt.e80) | (opt == Opt.e96))
         {
           coszeta  = Math.cos(zeta);
           sinzeta  = Math.sin(zeta);
           costheta = Math.cos(theta);
           sintheta = Math.sin(theta);
           cosz     = Math.cos(z);
           sinz     = Math.sin(z);

           // ----------------- form matrix  mod to gcrf ------------------
           prec[0][0] =  coszeta * costheta * cosz - sinzeta * sinz;
           prec[0][1] =  coszeta * costheta * sinz + sinzeta * cosz;
           prec[0][2] =  coszeta * sintheta;
           prec[1][0] = -sinzeta * costheta * cosz - coszeta * sinz;
           prec[1][1] = -sinzeta * costheta * sinz + coszeta * cosz;
           prec[1][2] = -sinzeta * sintheta;
           prec[2][0] = -sintheta * cosz;
           prec[2][1] = -sintheta * sinz;
           prec[2][2] =  costheta;

           // ----------------- do rotations instead ----------------------
           // p1 = rot3mat( z );
           // p2 = rot2mat( -theta );
           // p3 = rot3mat( zeta );
           // prec = p3 * p2*p1;
         }
         else
         {
           p1 = rot3mat( -chia );
           p2 = rot1mat( wa );
           p3 = rot3mat( psia );
           p4 = rot1mat( -oblo );

           tr1 = matmult( p4 , p3, 3, 3, 3 );
           tr2 = matmult( tr1, p2, 3, 3, 3 );
           prec = matmult( tr2, p1, 3, 3, 3 );

         }

//       if (iauhelp == 'y')
//         {
//           printf("pr %11.7f  %11.7f  %11.7f %11.7fdeg \n",psia * 180/pi,wa * 180/pi,epsa * 180/pi,chia * 180/pi );
//           printf("pr %11.7f  %11.7f  %11.7fdeg \n",zeta * 180/pi,theta * 180/pi,z * 180/pi );
//         }

       return prec;

     }  // procedure precess


/* -----------------------------------------------------------------------------
*
*                           procedure mattrans
*
*  this procedure finds the transpose of a matrix.
*
*  author        : david vallado                  719-573-2600    1 mar 2001
*
*  inputs          description                    range / units
*    mat1        - matrix number 1
*    mat1r       - matrix number 1 rows
*    mat1c       - matrix number 1 columns
*
*  outputs       :
*    mat2        - matrix result of transpose mat2
*
*  locals        :
*    row         - row index
*    col         - column index
*
*  coupling      :
*
* --------------------------------------------------------------------------- */

public static double[][]    mattrans
        (
          double[][] mat1,
          //double[][] mat2,
//          double mat1[3][3],
//          double mat2[3][3],
          int mat1r, int mat1c
        )
   {
     int row,col;

     double[][] mat2 = new double[mat1c][mat1r];
     //mat2.resize(mat1c);  // rows
     //for (std::vector< std::vector<double> >::iterator it=mat2.begin(); it != mat2.end();++it)
     //     it->resize(mat1r);

     for (row = 0; row < mat1r; row++)
       {
         for (col = 0; col < mat1c; col++)
             mat2[col][row] = mat1[row][col];
       }

     return mat2;
   }


/* -----------------------------------------------------------------------------
*
*                           procedure matmult
*
*  this procedure multiplies two matricies up to 10x10 together.
*
*  author        : david vallado                  719-573-2600    7 dec 2007
*
*  inputs          description                    range / units
*    mat1        - matrix number 1
*    mat2        - matrix number 2
*    mat1r       - matrix number 1 rows
*    mat1c       - matrix number 1 columns
*    mat2c       - matrix number 2 columns
*
*  outputs       :
*    mat3        - matrix result of mat1 * mat2 of size mat1r x mat2c
*
*  locals        :
*    row         - row index
*    col         - column index
*    ktr         - index
*
*  coupling      :
*
* --------------------------------------------------------------------------- */

public static double[][] matmult
        (
          double[][] mat1,
          double[][] mat2,
//          std::vector< std::vector<double> > &mat3,
//          double mat1[3][3],
//          double mat2[3][3],
//          double mat3[3][3],
          int mat1r, int mat1c, int mat2c
        )
   {
    double[][] mat3 = new double[mat1r][mat2c];
     int row,col,ktr;
     // specify the actual sizes
    // mat3.resize(mat1r);  // rows
    // for (std::vector< std::vector<double> >::iterator it=mat3.begin(); it != mat3.end();++it)
    //      it->resize(mat2c);


     for (row = 0; row < mat1r; row++)
       {
         for (col = 0; col < mat2c; col++)
           {
             mat3[row][col]= 0.0;
             for (ktr = 0; ktr < mat1c; ktr ++)
                 mat3[row][col]= mat3[row][col] + mat1[row][ktr] * mat2[ktr][col];
           }
       }

     return mat3;
   } //matmult

/* -----------------------------------------------------------------------------
*
*                           procedure matvecmult
*
*  this procedure multiplies a 3x3 matrix and a 3x1 vector together.
*
*  author        : david vallado                  719-573-2600    1 mar 2001
*
*  inputs          description                    range / units
*    mat         - 3 x 3 matrix
*    vec         - vector
*
*  outputs       :
*    vecout      - vector result of mat * vec
*
*  locals        :
*    row         - row index
*    col         - column index
*    ktr         - index
*
*  coupling      :
*
* --------------------------------------------------------------------------- */

public static double[] matvecmult
        (
          double[][] mat,
//          double mat[3][3],
          double[] vec//,
          //double[] vecout
        )
   {
     int row,col,ktr;
     double[] vecout = new double[3];

     for (row = 0; row <= 2; row++)
       {
         vecout[row]= 0.0;
         for (ktr = 0; ktr <= 2; ktr++)
             vecout[row]= vecout[row] + mat[row][ktr] * vec[ktr];
       }

     return vecout;
   }

/* -----------------------------------------------------------------------------
*
*                                  rotimat
*
*  this function sets up a rotation matrix for an input angle about the first
*    axis.
*
*  author        : david vallado                  719-573-2600   10 jan 2003
*
*  revisions
*                -
*
*  inputs          description                    range / units
*    xval        - angle of rotation              rad
*
*  outputs       :
*    outmat      - matrix result
*
*  locals        :
*    c           - cosine of the angle xval
*    s           - sine of the angle xval
*
*  coupling      :
*    none.
*
* --------------------------------------------------------------------------- */
public static double[][] rot1mat
        (
          double xval//,
          //std::vector< std::vector<double> > &outmat
//          double outmat[3][3]
        )
   {
     double[][] outmat = new double[3][3];//outmat.resize(3);  // rows
     //for (std::vector< std::vector<double> >::iterator it=outmat.begin(); it != outmat.end();++it)
     //     it->resize(3);
     double c, s;
     c= Math.cos( xval );
     s= Math.sin( xval );

     outmat[0][0]= 1.0;
     outmat[0][1]= 0.0;
     outmat[0][2]= 0.0;

     outmat[1][0]= 0.0;
     outmat[1][1]= c;
     outmat[1][2]= s;

     outmat[2][0]= 0.0;
     outmat[2][1]= -s;
     outmat[2][2]= c;

     return outmat;
   }

public static double[][] rot3mat
        (
          double xval//,
          //std::vector< std::vector<double> > &outmat
//          double outmat[3][3]
        )
   {
     double[][] outmat = new double[3][3];//.resize(3);  // rows
     //for (std::vector< std::vector<double> >::iterator it=outmat.begin(); it != outmat.end();++it)
     //     it->resize(3);
     double c, s;
     c= Math.cos( xval );
     s= Math.sin( xval );

     outmat[0][0]= c;
     outmat[0][1]= s;
     outmat[0][2]= 0.0;

     outmat[1][0]= -s;
     outmat[1][1]= c;
     outmat[1][2]= 0.0;

     outmat[2][0]= 0.0;
     outmat[2][1]= 0.0;
     outmat[2][2]= 1.0;

     return outmat;
   }

/* -----------------------------------------------------------------------------
*
*                           function truemean
*
*  this function forms the transformation matrix to go between the
*    norad true equator mean equinox of date and the mean equator mean equinox
*    of date (gcrf).  the results approximate the effects of nutation and
*    precession.
*
*  author        : david vallado                  719-573-2600   25 jun 2002
*
*  revisions
*    vallado     - fixes to order                                29 sep 2002
*    vallado     - fixes to all options                           6 may 2003
*    vallado     - conversion to c++                             21 feb 2005
*
*  inputs          description                    range / units
*    ttt         - julian centuries of tt
*    order       - number of terms for nutation   4, 50, 106, ...
*    eqeterms    - number of terms for eqe        0, 2
*    opt1        - option for procesMath.sing     a - complete nutation
*                                                 b - truncated nutation
*                                                 c - truncated transf matrix
*    iau80rec    - record containing the iau80 constants rad
*
*  outputs       :
*    nutteme     - matrix for mod - teme - an approximation for nutation
*
*  locals        :
*    prec        - matrix for mod - j2000
*    tm          - combined matrix for teme
*    l           -                                rad
*    ll          -                                rad
*    f           -                                rad
*    d           -                                rad
*    omega       -                                rad
*    deltapsi    - nutation angle                 rad
*    deltaeps    - change in obliquity            rad
*    eps         - mean obliquity of the ecliptic rad
*    trueeps     - true obliquity of the ecliptic rad
*    meaneps     - mean obliquity of the ecliptic rad
*
*  coupling      :
*
*
*  references    :
*    vallado       2007, 236
* --------------------------------------------------------------------------- */

public static double[][] truemean
     (
       double ttt, int order, int eqeterms, char opt//,
       //iau80data& iau80rec,
       //std::vector< std::vector<double> > &nutteme
     )
     {
       //nutteme.resize(3);  // rows
       //for (std::vector< std::vector<double> >::iterator it=nutteme.begin(); it != nutteme.end();++it)
       //     it->resize(3);
    double[][] nutteme = new double[3][3];

       double deg2rad, l, l1, f, d, omega,
              cospsi, sinpsi, coseps, sineps, costrueeps, sintrueeps, meaneps,
              deltapsi, deltaeps, trueeps;
       int i;
       double[][] nut = new double[3][3];
       double[][] st = new double[3][3];
       double  tempval, jdttt, eqe;

       deg2rad = Math.PI/180.0;

       // ---- determine coefficients for iau 1980 nutation theory ----
       meaneps = ((0.001813  * ttt - 0.00059 ) * ttt -46.8150 ) * ttt + 84381.448;
       meaneps = meaneps/3600.0  % 360.0 ;
       meaneps = meaneps  *  deg2rad;

       l    = ((((  0.064 ) * ttt + 31.310 ) * ttt + 1717915922.6330 ) * ttt )
              / 3600.0 + 134.96298139;
       l1   = ((((- 0.012 ) * ttt -  0.577 ) * ttt +  129596581.2240 ) * ttt )
              / 3600.0 + 357.52772333;
       f    = ((((  0.011 ) * ttt - 13.257 ) * ttt + 1739527263.1370 ) * ttt )
              / 3600.0 +  93.27191028;
       d    = ((((  0.019 ) * ttt -  6.891 ) * ttt + 1602961601.3280 ) * ttt )
              / 3600.0 + 297.85036306;
       omega= ((((  0.008 ) * ttt +  7.455 ) * ttt -    6962890.5390 ) * ttt )
              / 3600.0 + 125.04452222;

       l    = ( l%360.0  )     * deg2rad;
       l1   = ( l1%360.0  )    * deg2rad;
       f    = ( f%360.0  )     * deg2rad;
       d    = ( d%360.0  )     * deg2rad;
       omega= ( omega%360.0  ) * deg2rad;

       deltapsi= 0.0;
       deltaeps= 0.0;
       for (i= 1; i <= order; i++)   // the eqeterms in nut80.dat are already sorted
         {
           tempval= iar80(i,1) * l + iar80(i,2) * l1 + iar80(i,3) * f +
                    iar80(i,4) * d + iar80(i,5) * omega;
           deltapsi= deltapsi + (rar80(i,1)+rar80(i,2) * ttt) * Math.sin( tempval );
           deltaeps= deltaeps + (rar80(i,3)+rar80(i,4) * ttt) * Math.cos( tempval );
         }

       // --------------- find nutation parameters --------------------
       deltapsi = ( deltapsi%360.0  ) * deg2rad;
       deltaeps = ( deltaeps%360.0  ) * deg2rad;
       trueeps  = meaneps + deltaeps;

       cospsi  = Math.cos(deltapsi);
       sinpsi  = Math.sin(deltapsi);
       coseps  = Math.cos(meaneps);
       sineps  = Math.sin(meaneps);
       costrueeps = Math.cos(trueeps);
       sintrueeps = Math.sin(trueeps);

       jdttt = ttt * 36525.0 + 2451545.0;
       // small disconnect with ttt instead of ut1
       if ((jdttt > 2450449.5 ) && (eqeterms > 0))
           eqe= deltapsi *  Math.cos(meaneps)
               + 0.00264 * Math.PI /(3600 * 180) * Math.sin(omega)
               + 0.000063 * Math.PI /(3600 * 180) * Math.sin(2.0  * omega);
         else
           eqe= deltapsi *  Math.cos(meaneps);

       nut[0][0] =  cospsi;
       nut[0][1] =  costrueeps * sinpsi;
       if (opt == 'b')
           nut[0][1] = 0.0;
       nut[0][2] =  sintrueeps * sinpsi;
       nut[1][0] = -coseps * sinpsi;
       if (opt == 'b')
           nut[1][0] = 0.0;
       nut[1][1] =  costrueeps * coseps * cospsi + sintrueeps * sineps;
       nut[1][2] =  sintrueeps * coseps * cospsi - sineps * costrueeps;
       nut[2][0] = -sineps * sinpsi;
       nut[2][1] =  costrueeps * sineps * cospsi - sintrueeps * coseps;
       nut[2][2] =  sintrueeps * sineps * cospsi + costrueeps * coseps;

       st[0][0] =  Math.cos(eqe);
       st[0][1] = -Math.sin(eqe);
       st[0][2] =  0.0;
       st[1][0] =  Math.sin(eqe);
       st[1][1] =  Math.cos(eqe);
       st[1][2] =  0.0;
       st[2][0] =  0.0;
       st[2][1] =  0.0;
       st[2][2] =  1.0;

       //matmult( st, nut, nutteme, 3, 3, 3 );
       nutteme = MathUtils.mult(st, nut);

       if (opt == 'c')
         {
           nutteme[0][0] =  1.0;
           nutteme[0][1] =  0.0;
           nutteme[0][2] =  deltapsi * sineps;
           nutteme[1][0] =  0.0;
           nutteme[1][1] =  1.0;
           nutteme[1][2] =  deltaeps;
           nutteme[2][0] = -deltapsi * sineps;
           nutteme[2][1] = -deltaeps;
           nutteme[2][2] =  1.0;
          }

//       tm = nutteme * prec;
       return nutteme;

      }   // procedure truemean


// read data and apply corrections if needed from iau80rec data
// row 1-106, index 1-4
public static double rar80(int row, int index)
{
    return iau80rec[row-1][index+4] * 0.0001 /3600.0;
}

// row 1-106, index 1-5
public static double iar80(int row, int index)
{
    return iau80rec[row-1][index-1];
}

public static double[][] iau80rec =
{
    {0,0,0,0,1,-171996,-174.2,92025,8.9,1}, // 1
{0,0,2,-2,2,-13187,-1.6,5736,-3.1,9},
{0,0,2,0,2,-2274,-0.2,977,-0.5,31},
{0,0,0,0,2,2062,0.2,-895,0.5,2},
{0,1,0,0,0,1426,-3.4,54,-0.1,10},
{1,0,0,0,0,712,0.1,-7,0,32},
{0,1,2,-2,2,-517,1.2,224,-0.6,11},
{0,0,2,0,1,-386,-0.4,200,0,33},
{1,0,2,0,2,-301,0,129,-0.1,34},
{0,-1,2,-2,2,217,-0.5,-95,0.3,12},
{1,0,0,-2,0,-158,0,-1,0,35},
{0,0,2,-2,1,129,0.1,-70,0,13},
{-1,0,2,0,2,123,0,-53,0,36},
{1,0,0,0,1,63,0.1,-33,0,38},
{0,0,0,2,0,63,0,-2,0,37},
{-1,0,2,2,2,-59,0,26,0,40},
{-1,0,0,0,1,-58,-0.1,32,0,39},
{1,0,2,0,1,-51,0,27,0,41},
{2,0,0,-2,0,48,0,1,0,14},
{-2,0,2,0,1,46,0,-24,0,3},
{0,0,2,2,2,-38,0,16,0,42},
{2,0,2,0,2,-31,0,13,0,45},
{2,0,0,0,0,29,0,-1,0,43},
{1,0,2,-2,2,29,0,-12,0,44},
{0,0,2,0,0,26,0,-1,0,46},
{0,0,2,-2,0,-22,0,0,0,15},
{-1,0,2,0,1,21,0,-10,0,47},
{0,2,0,0,0,17,-0.1,0,0,16},
{0,2,2,-2,2,-16,0.1,7,0,18},
{-1,0,0,2,1,16,0,-8,0,48},
{0,1,0,0,1,-15,0,9,0,17},
{1,0,0,-2,1,-13,0,7,0,49},
{0,-1,0,0,1,-12,0,6,0,19},
{2,0,-2,0,0,11,0,0,0,4},
{-1,0,2,2,1,-10,0,5,0,50},
{1,0,2,2,2,-8,0,3,0,54},
{0,-1,2,0,2,-7,0,3,0,53},
{0,0,2,2,1,-7,0,3,0,58},
{1,1,0,-2,0,-7,0,0,0,51},
{0,1,2,0,2,7,0,-3,0,52},
{-2,0,0,2,1,-6,0,3,0,20},
{0,0,0,2,1,-6,0,3,0,57},
{2,0,2,-2,2,6,0,-3,0,56},
{1,0,0,2,0,6,0,0,0,55},
{1,0,2,-2,1,6,0,-3,0,58},
{0,0,0,-2,1,-5,0,3,0,60},
{0,-1,2,-2,1,-5,0,3,0,21},
{2,0,2,0,1,-5,0,3,0,62},
{1,-1,0,0,0,5,0,0,0,61},
{1,0,0,-1,0,-4,0,0,0,24},
{0,0,0,1,0,-4,0,0,0,65},
{0,1,0,-2,0,-4,0,0,0,63},
{1,0,-2,0,0,4,0,0,0,64},
{2,0,0,-2,1,4,0,-2,0,22},
{0,1,2,-2,1,4,0,-2,0,23},
{1,1,0,0,0,-3,0,0,0,66},
{1,-1,0,-1,0,-3,0,0,0,6},
{-1,-1,2,2,2,-3,0,1,0,69},
{0,-1,2,2,2,-3,0,1,0,72},
{1,-1,2,0,2,-3,0,1,0,68},
{3,0,2,0,2,-3,0,1,0,71},
{-2,0,2,0,2,-3,0,1,0,5},
{1,0,2,0,0,3,0,0,0,67},
{-1,0,2,4,2,-2,0,1,0,82},
{1,0,0,0,2,-2,0,1,0,76},
{-1,0,2,-2,1,-2,0,1,0,74},
{0,-2,2,-2,1,-2,0,1,0,7},
{-2,0,0,0,1,-2,0,1,0,70},
{2,0,0,0,1,2,0,-1,0,75},
{3,0,0,0,0,2,0,0,0,77},
{1,1,2,0,2,2,0,-1,0,73},
{0,0,2,1,2,2,0,-1,0,78},
{1,0,0,2,1,-1,0,0,0,91},
{1,0,2,2,1,-1,0,1,0,85},
{1,1,0,-2,1,-1,0,0,0,102},
{0,1,0,2,0,-1,0,0,0,99},
{0,1,2,-2,0,-1,0,0,0,30},
{0,1,-2,2,0,-1,0,0,0,27},
{1,0,-2,2,0,-1,0,0,0,103},
{1,0,-2,-2,0,-1,0,0,0,100},
{1,0,2,-2,0,-1,0,0,0,94},
{1,0,0,-4,0,-1,0,0,0,80},
{2,0,0,-4,0,-1,0,0,0,83},
{0,0,2,4,2,-1,0,0,0,105},
{0,0,2,-1,2,-1,0,0,0,98},
{-2,0,2,4,2,-1,0,1,0,86},
{2,0,2,2,2,-1,0,0,0,90},
{0,-1,2,0,1,-1,0,0,0,101},
{0,0,-2,0,1,-1,0,0,0,97},
{0,0,4,-2,2,1,0,0,0,92},
{0,1,0,0,2,1,0,0,0,28},
{1,1,2,-2,2,1,0,-1,0,84},
{3,0,2,-2,2,1,0,0,0,93},
{-2,0,2,2,2,1,0,-1,0,81},
{-1,0,0,0,2,1,0,-1,0,79},
{0,0,-2,2,1,1,0,0,0,26},
{0,1,2,0,1,1,0,0,0,95},
{-1,0,4,0,2,1,0,0,0,87},
{2,1,0,-2,0,1,0,0,0,25},
{2,0,0,2,0,1,0,0,0,104},
{2,0,2,-2,1,1,0,-1,0,89},
{2,0,-2,0,1,1,0,0,0,8},
{1,-1,0,-2,0,1,0,0,0,88},
{-1,0,0,1,1,1,0,0,0,29},
{-1,-1,0,2,1,1,0,0,0,96},
{0,1,0,1,0,1,0,0,0,106}
};

}
