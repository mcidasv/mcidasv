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
// based off of the "typedef struct elsetrec" in the CSSI's sgp4unit.h file
// conatins all the data needed for a SGP4 propogated satellite
// holds all initialization info, etc.

package edu.wisc.ssec.mcidasv.data.adde.sgp4;

import java.io.Serializable;

/**
 * 19 June 2009
 * converted to Java by:
 * @author Shawn E. Gano, shawn@gano.name
 */

public class SGP4SatData implements Serializable
{

  private static final long serialVersionUID = 1L;
  public int   satnum; // changed to int SEG
  public int    epochyr, epochtynumrev;
  public int    error; // 0 = ok, 1= eccentricity (sgp4),   6 = satellite decay, 7 = tle data
  public char   operationmode;
  public char   init, method;

  public SGP4unit.Gravconsttype gravconsttype; // gravity constants to use - SEG

  /* Near Earth */
  public int    isimp;
  public double aycof  , con41  , cc1    , cc4      , cc5    , d2      , d3   , d4    ,
                delmo  , eta    , argpdot, omgcof   , sinmao , t       , t2cof, t3cof ,
                t4cof  , t5cof  , x1mth2 , x7thm1   , mdot   , nodedot, xlcof , xmcof ,
                nodecf;

  /* Deep Space */

  public int    irez;
  public double d2201  , d2211  , d3210  , d3222    , d4410  , d4422   , d5220 , d5232 ,
                d5421  , d5433  , dedt   , del1     , del2   , del3    , didt  , dmdt  ,
                dnodt  , domdt  , e3     , ee2      , peo    , pgho    , pho   , pinco ,
                plo    , se2    , se3    , sgh2     , sgh3   , sgh4    , sh2   , sh3   ,
                si2    , si3    , sl2    , sl3      , sl4    , gsto    , xfact , xgh2  ,
                xgh3   , xgh4   , xh2    , xh3      , xi2    , xi3     , xl2   , xl3   ,
                xl4    , xlamo  , zmol   , zmos     , atime  , xli     , xni;

  public double a      , altp   , alta   , epochdays, jdsatepoch       , nddot , ndot  ,
                bstar  , rcse   , inclo  , nodeo    , ecco             , argpo , mo    ,
                no;

  // Extra Data added by SEG - from TLE and a name variable (and save the lines for future use)
  public String name="", line1="", line2="";
  public boolean tleDataOk;
  public String classification, intldesg;
  public int nexp, ibexp, numb; // numb is the second number on line 1
  public long elnum,revnum; 

}
