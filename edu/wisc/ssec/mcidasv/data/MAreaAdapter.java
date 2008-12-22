/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

//
// MAreaAdapter.java
//

/*

The software in this file is Copyright(C) 1998 by Tom Whittaker.
It is designed to be used with the VisAD system for interactive
analysis and visualization of numerical data.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 1, or (at your option)
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License in file NOTICE for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

package edu.wisc.ssec.mcidasv.data;

import java.io.IOException;
import java.rmi.RemoteException;

import visad.CoordinateSystem;
import visad.DateTime;
import visad.FlatField;
import visad.FunctionType;
import visad.Integer1DSet;
import visad.Linear2DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.Set;
import visad.Unit;
import visad.VisADException;
//import visad.data.mcidas.AREACoordinateSystem;
import visad.meteorology.NavigatedImage;
import visad.meteorology.SingleBandedImage;
import visad.meteorology.SingleBandedImageImpl;
import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaFile;
import edu.wisc.ssec.mcidas.AreaFileFactory;
import edu.wisc.ssec.mcidas.Calibrator;
import edu.wisc.ssec.mcidas.McIDASException;

/** this is an adapter for McIDAS AREA images */

public class MAreaAdapter {
  
  private FlatField field = null;
  private McIDASVAREACoordinateSystem cs;
  private AreaDirectory areaDirectory;

  /** Create a VisAD FlatField from a local McIDAS AREA file or a URL.
    * @param imageSource name of local file or a URL to locate file.
    * @exception IOException if there was a problem reading the file.
    * @exception VisADException if an unexpected problem occurs.
    */
  public MAreaAdapter(String imageSource) throws IOException, VisADException {
    this(
        imageSource, 
        0, 0, 
        0, 0, 
        0, 0, 
        Calibrator.CAL_NONE, 0, true
    );
  }

  /** Create a VisAD FlatField from a local McIDAS AREA file or a URL.
   * @param imageSource name of local file or a URL to locate file.
   * @param cal type of calibration to perform on retrieved data; ignored if
   * 'unit' is specified in <code>imageSource</code>.
   * @exception IOException if there was a problem reading the file.
   * @exception VisADException if an unexpected problem occurs.
   */
  public MAreaAdapter(String imageSource, int cal) throws IOException, VisADException {
    this(
        imageSource, 
        0, 0, 
        0, 0, 
        0, 0, 
        cal, 0, true
    );
  }
  
  /** Create a VisAD FlatField from a local McIDAS AREA file or a URL.
    * @param imageSource name of local file or a URL to locate file.
    * @param pack      pack data if possible.  If calibration is BRIT, 
    *                  images are packed into bytes
    * @exception IOException if there was a problem reading the file.
    * @exception VisADException if an unexpected problem occurs.
    */
  public MAreaAdapter(String imageSource, boolean pack) 
      throws IOException, VisADException {
    this(
        imageSource, 
        0, 0, 
        0, 0, 
        0, 0, 
        Calibrator.CAL_NONE, 0, pack
    );
  }

  /** Create a VisAD FlatField from a local McIDAS AREA file using
    * the subsecting information
    * @param imageSource name of local file or a URL to locate file.
    * @param startLine starting line from the file (AREA coordinates)
    * @param startEle starting element from the file (AREA coordinates)
    * @param numLines number of lines to read
    * @param numEles number of elements to read
    * @exception IOException if there was a problem reading the file.
    * @exception VisADException if an unexpected problem occurs.
    */
  public MAreaAdapter(String imageSource, 
                     int startLine, 
                     int startEle, 
                     int numLines, 
                     int numEles ) throws IOException, VisADException {
    this(
        imageSource, 
        startLine, startEle, 
        numLines, numEles, 
        0, 0, 
        Calibrator.CAL_NONE, 0, true);
  }

  /** Create a VisAD FlatField from a local McIDAS AREA subsected
    * according to the parameters
    * @param imageSource name of local file or a URL to locate file.
    * @param startLine starting line from the file (AREA coordinates)
    * @param startEle starting element from the file (AREA coordinates)
    * @param numLines number of lines to read
    * @param numEles number of elements to read
    * @param band band number to get
    * @exception IOException if there was a problem reading the file.
    * @exception VisADException if an unexpected problem occurs.
    */
  public MAreaAdapter(String imageSource, 
                     int startLine, 
                     int startEle, 
                     int numLines, 
                     int numEles, 
                     int band ) throws IOException, VisADException {
    this(
        imageSource, 
        startLine, startEle, 
        numLines, numEles, 
        0, 0, 
        Calibrator.CAL_NONE, band, true
    );
  } 
  
  /** Create a VisAD FlatField from a local McIDAS AREA subsected
   * according to the parameters
   * @param imageSource name of local file or a URL to locate file.
   * @param startLine starting line from the file (AREA coordinates)
   * @param startEle  starting element from the file (AREA coordinates)
   * @param numLines  number of lines to read
   * @param numEles   number of elements to read
   * @param band      band number to get
   * @param pack      pack data if possible.  If calibration is BRIT, 
   *                  images are packed into bytes
   *                      
   * @exception IOException if there was a problem reading the file.
   * @exception VisADException if an unexpected problem occurs.
   */
  public MAreaAdapter(String imageSource, 
      int startLine, 
      int startEle, 
      int numLines, 
      int numEles,
      int band,
      boolean pack) throws IOException, VisADException {
    this(
        imageSource, 
        startLine, startEle, 
        numLines, numEles, 
        0, 0, 
        Calibrator.CAL_NONE, band, pack
    );
  }
 
  /** Create a VisAD FlatField from a local McIDAS AREA subsected
   * according to the parameters
   * @param imageSource name of local file or a URL to locate file.
   * @param startLine starting line from the file (AREA coordinates)
   * @param startEle starting element from the file (AREA coordinates)
   * @param numLines number of lines to read
   * @param numEles number of elements to read
   * @param band band number to get
   * @param lineMag magnification for lines
   * @param eleMag magnification for elements
   * @exception IOException if there was a problem reading the file.
   * @exception VisADException if an unexpected problem occurs.
   */
  public MAreaAdapter(String imageSource, 
      int startLine, 
      int startEle, 
      int numLines, 
      int numEles,
      int lineMag,
      int eleMag,
      int band) throws IOException, VisADException {
    
    this(
        imageSource, 
        startLine, startEle, 
        numLines, numEles, 
        lineMag, eleMag, 
        Calibrator.CAL_NONE, band, true
    );
  }

  /** Create a VisAD FlatField from a local McIDAS AREA subsected
   * according to the parameters
   * @param imageSource name of local file or a URL to locate file.
   * @param startLine starting line from the file (AREA coordinates)
   * @param startEle starting element from the file (AREA coordinates)
   * @param numLines number of lines to read
   * @param numEles number of elements to read
   * @param lineMag magnification for lines
   * @param eleMag magnification for elements
   * @param cal type of calibration to perform on retrieved data; ignored if
   * 'unit' is specified in <code>imageSource</code>.
   * @param band band number to get
   * @exception IOException if there was a problem reading the file.
   * @exception VisADException if an unexpected problem occurs.
   */
  public MAreaAdapter(String imageSource, 
      int startLine, 
      int startEle, 
      int numLines, 
      int numEles,
      int lineMag,
      int eleMag,
      int cal,
      int band) throws IOException, VisADException {
    
    this(
        imageSource, 
        startLine, startEle, 
        numLines, numEles, 
        lineMag, eleMag, 
        cal, band, true
    );
  }
  
  /** Create a VisAD FlatField from a local McIDAS AREA subsected
   * according to the parameters
   * @param imageSource name of local file or a URL to locate file.
   * @param startLine starting line from the file (AREA coordinates)
   * @param startEle  starting element from the file (AREA coordinates)
   * @param numLines  number of lines to read
   * @param numEles   number of elements to read
   * @param band      band number to get
   * @param lineMag magnification for lines
   * @param eleMag magnification for elements
   * @param pack      pack data if possible.  If calibration is BRIT, 
   *                  images are packed into bytes
   *                      
   * @exception IOException if there was a problem reading the file.
   * @exception VisADException if an unexpected problem occurs.
   */
  public MAreaAdapter(String imageSource, 
                     int startLine, 
                     int startEle, 
                     int numLines, 
                     int numEles,
                     int lineMag,
                     int eleMag,
                     int band,
                     boolean pack) throws IOException, VisADException {
    this(
        imageSource, 
        startLine, startEle, 
        numLines, numEles,
        lineMag, eleMag, 
        Calibrator.CAL_NONE, band, pack
    );
  }
  
  private AreaFile af;

  public AreaFile getAreaFile() {
       return af;
   }


  /** Create a VisAD FlatField from a local McIDAS AREA subsected
   * according to the parameters
   * @param imageSource name of local file or a URL to locate file.
   * @param startLine starting line from the file (AREA coordinates)
   * @param startEle  starting element from the file (AREA coordinates)
   * @param numLines  number of lines to read
   * @param numEles   number of elements to read
   * @param lineMag magnification for lines
   * @param eleMag magnification for elements
   * @param cal type of calibration to perform on retrieved data; ignored if
   * 'unit' is specified in <code>imageSource</code>.
   * @param band      band number to get
   * @param pack      pack data if possible.  If calibration is BRIT, 
   *                  images are packed into bytes
   *                      
   * @exception IOException if there was a problem reading the file.
   * @exception VisADException if an unexpected problem occurs.
   */
  public MAreaAdapter(String imageSource, 
                     int startLine, 
                     int startEle, 
                     int numLines, 
                     int numEles,
                     int lineMag,
                     int eleMag,
                     int cal,
                     int band,
                     boolean pack) throws IOException, VisADException {    

    try {
        //System.out.println("getAreaFileInstance");
        af = AreaFileFactory.getAreaFileInstance(imageSource);
        //System.out.println("af=" + af);
      // cal type not set in the imageSource URL
      //System.out.println("cal=" + cal + " Calibrator.CAL_NONE=" + Calibrator.CAL_NONE);
      if (af.getCalType() == Calibrator.CAL_NONE) {
        af.setCalType(cal);
      }
      
      AreaDirectory dir = af.getAreaDirectory();
      //System.out.println("dir=" + dir);
      
      // indicates subsetting using the parameters
      boolean paramSubset = numLines != 0 && numEles != 0 && band != 0;
      //System.out.println("paramSubset=" + paramSubset);
      
      // subsetted using the URL in imageSource, ignore params
      //System.out.println("af.isSubsetted=" + af.isSubsetted());
      if (af.isSubsetted()) {
        //System.out.println("Here 1");
        buildFlatField(
            af,
            0,
            0,
            dir.getLines(),
            dir.getElements(),
            dir.getBands()[0],
            pack
        );
      
      // subsetted in parameters, so subset manually
      } else if (!af.isSubsetted() && paramSubset){
        //System.out.println("Here 2");
        af = AreaFileFactory.getAreaFileInstance(
                imageSource, 
                startLine, 
                numLines, 
                lineMag, 
                startEle, 
                numEles, 
                eleMag, 
                band
        );
        // cal type not set in the imageSource URL
        if (af.getCalType() == Calibrator.CAL_NONE) {
          af.setCalType(cal);
        }
        //System.out.println("af.getCalType=" + af.getCalType());
        dir = af.getAreaDirectory(); // be sure to re-get the directory
        //System.out.println("dir=" + dir);
        //System.out.println("    lines=" + dir.getLines());
        //System.out.println("    elements=" + dir.getElements());
        //System.out.println("    band=" + dir.getBands()[0]);
        buildFlatField(
            af, 
            0, 
            0, 
            dir.getLines(), 
            dir.getElements(),
            dir.getBands()[0],
            pack
        );
        
      // getting the entire file including all bands
      } else {
        //System.out.println("dir=" + dir);
        //System.out.println("    lines=" + dir.getLines());
        //System.out.println("    elements=" + dir.getElements());
        buildFlatField(
            af, 
            0, 
            0, 
            dir.getLines(), 
            dir.getElements(),
            0,
            pack
        );
      }
      
    } catch (McIDASException afe) {
         throw new VisADException("Problem with McIDAS AREA file: " + afe);
    }
    //System.out.println("Done");
  }

  /** Build a FlatField from the image pixels
    * @param af is the AreaFile
    * @exception VisADException if an unexpected problem occurs.
    */
  //private void buildFlatField(AreaFile af) throws VisADException {
  private void buildFlatField(AreaFile af, 
                              int startLine, 
                              int startEle, 
                              int numLines,
                              int numEles, 
                              int band,
                              boolean pack) throws VisADException {
/*
    System.out.println("buildFlatField:");
    System.out.println("    af=" + af);
    System.out.println("    startLine=" + startLine);
    System.out.println("    startEle=" + startEle);
    System.out.println("    numLines=" + numLines);
    System.out.println("    numEles=" + numEles);
    System.out.println("    band=" + band);
    System.out.println("    pack=" + pack);
*/
    int[] nav=null;
    int[] aux = null;

    try {
      areaDirectory = af.getAreaDirectory();
      nav = af.getNav();
      aux = af.getAux();
/*
      System.out.println("\n    areaDirectory=" + areaDirectory);
      System.out.println("    nav=" + nav);
      System.out.println("    aux=" + aux);
*/
    } catch (Exception rmd) {
        throw new VisADException(
            "Problem getting Area file directory or navigation: " + rmd);
    }

    // extract the size of each dimension from the directory
    int nLines = (numLines == 0) ? areaDirectory.getLines() : numLines;
    int nEles = (numEles == 0) ? areaDirectory.getElements(): numEles;
/*
    System.out.println("\n    nLines=" + nLines);
    System.out.println("    nEles=" + nEles);
*/
    // make the VisAD RealTypes for the dimension variables
    RealType line = RealType.getRealType("ImageLine", null, null);
    RealType element = RealType.getRealType("ImageElement", null, null);
/*
    System.out.println("\n    line=" + line);
    System.out.println("    element=" + element);
*/
    // extract the number of bands (sensors) and make the VisAD type
    // NB: always zero now
    int bandNums[] = areaDirectory.getBands();
    int numBands = areaDirectory.getNumberOfBands();  // this might be different
    //System.out.println("    numBands=" + numBands);
    
    // create indicies into the data structure for the bands
    int[] bandIndices = new int[numBands];
    if (band != 0) { // specific bands requested
        bandIndices[0] = -1;
        for (int i = 0; i < numBands; i++) {
           if (band == bandNums[i]) {
              bandIndices[0] = i;
              break;
           }
        }
        if (bandIndices[0] == -1)  // not found
            throw new VisADException("requested band number not in image");
        bandNums = new int[] { band };
        numBands = 1;
    } else {  // all bands
        for (int i = 0; i < numBands; i++) bandIndices[i] = i;
    }
    
    RealType[] bands = new RealType[numBands];
    // If we have calibration units, might as well use them.
    Unit calUnit = null;
    float calScale = 1.0f;
    try {
        calUnit = visad.data.units.Parser.parse(
            visad.jmet.MetUnits.makeSymbol(
                areaDirectory.getCalibrationUnitName()));

        calScale = (1.0f/areaDirectory.getCalibrationScaleFactor());
    } catch (Exception e) {
        calUnit = null;
    }
    String calType = areaDirectory.getCalibrationType();

    // first cut: the bands are named "Band##" where ## is the
    // band number from the AREA file bandmap
    for (int i = 0; i < numBands; i++)
    {
        bands[i] = 
          (calUnit != null)
             ? RealType.getRealType("Band"+bandNums[i]+"_"+calType, calUnit)
             : RealType.getRealType("Band"+bandNums[i]);
        //System.out.println("        band=" + bands[i]);
    }


    // the range of the FunctionType is the band(s)
    RealTupleType radiance = new RealTupleType(bands);

    // the domain is (element,line) since elements (X) vary fastest
    RealType[] domain_components = {element,line};

    // Create the appropriate CoordinateSystem and attach it to
    // the domain of the FlatField.  McIDASVAREACoordinateSystem transforms
    // from (ele,lin) -> (lat,lon)
    try
    {
        // adjust the directory in case a subsection was requested
        int[] dirBlock = (int[]) areaDirectory.getDirectoryBlock().clone();
// BMF: directory modification is now handled in AreaFile
//        dirBlock[5] = dirBlock[5] + (startLine * dirBlock[11]);
//        dirBlock[6] = dirBlock[6] + (startEle  * dirBlock[12]);
//        dirBlock[8] = nLines;
//        dirBlock[9] = nEles;
        cs = new McIDASVAREACoordinateSystem( dirBlock, nav, aux);
        //System.out.println("    cs=" + cs);
    }
    catch (VisADException e)
    {
      System.out.println(e);
      System.out.println("Using null CoordinateSystem");
      cs = null;
    }

    RealTupleType image_domain =
                new RealTupleType(domain_components, cs, null);

    //  Image numbering is usually the first line is at the "top"
    //  whereas in VisAD, it is at the bottom.  So define the
    //  domain set of the FlatField to map the Y axis accordingly

    Linear2DSet domain_set = new Linear2DSet(image_domain,
                                0, (nEles - 1), nEles,
                                (nLines - 1), 0, nLines );
    FunctionType image_type =
                        new FunctionType(image_domain, radiance);

    // If calibrationType is brightnes (BRIT), then we can store
    // the values as shorts.  To do this, we crunch the values down
    // from 0-255 to 0-254 so we can have 255 left over for missing
    // values.
    Set[] rangeSets = null;
    pack = pack && calType.equalsIgnoreCase("BRIT");
    if (pack) {
      rangeSets = new Set[numBands];
      for (int i = 0; i < numBands; i++) {
            rangeSets[i] = new Integer1DSet(bands[i], 255);
      }
    }
    Unit[] rangeUnits = null;
    if (calUnit != null) {
      rangeUnits = new Unit[numBands];
      for (int i = 0; i < numBands; i++) rangeUnits[i] = calUnit;
    }

    // finally, create the field.
    //System.out.println("making new Flat Field:");
    field = new FlatField(image_type,
                          domain_set,
                          (CoordinateSystem[]) null,
                          rangeSets,
                          rangeUnits);
    //System.out.println("made new Flat Field");

    // scine we are returning a SingleBandedImage in the getData 
    // and getImage calls anyway, we might as well create the
    // SingleBandedImage here.  We haven't setSamples so this shouldn't
    // be expensive.
    //System.out.println("radiance.getDimension()=" + radiance.getDimension());
    if (radiance.getDimension() == 1) {
      //System.out.println("cs=" + cs);
      field = 
        (cs == null)
          ?  new SingleBandedImageImpl(field, getNominalTime(), "McIDAS Image")
          :  new NavigatedImage(field, getNominalTime(), "McIDAS Image");
    }
    //System.out.println("Here");


    // get the data, possibly calibrated
    float[][][] flt_samples = null;
    try {
      //System.out.println("    get flt_samples");
      flt_samples = af.getFloatData();
    } catch (Exception samp) {
    //} catch (McIDASException samp) {
      System.out.println("ERROR: samp=" + samp);
      //throw new VisADException("Problem reading AREA file: "+samp);
    }
    //System.out.println("    got flt_samples");

    // for each band, create a sample array for the FlatField

    try {
      //System.out.println("    get samples");
      float[][] samples = new float[numBands][nEles*nLines];
      //System.out.println("    got samples");

      //if (areaDirectory.getCalibrationType().equalsIgnoreCase("BRIT"))
      //System.out.println("    pack=" + pack);
      if (pack) {

        for (int b=0; b<numBands; b++) {
          for (int i=0; i<nLines; i++) {
            for (int j=0; j<nEles; j++) {
  
              float val = flt_samples[bandIndices[b]][startLine+i][startEle+j];
              samples[b][j + (nEles * i) ] =
                (val == 255)
                   ? 254.0f                   // push 255 into 254 for BRIT
                   : (float) val * calScale;
            }
          }
        }
      }
      else {

        //System.out.println("numBands=" + numBands);
        //System.out.println("nLines=" + nLines);
        //System.out.println("nEles=" + nEles);
        for (int b=0; b<numBands; b++) {
          for (int i=0; i<nLines; i++) {
            for (int j=0; j<nEles; j++) {

              samples[b][j + (nEles * i) ] = calScale * 
                flt_samples[bandIndices[b]][startLine+i][startEle+j];

            }
          }
        }
      }
      //System.out.println("setting Samples:");
      field.setSamples(samples, false);
      //System.out.println("Samples set");

    } catch (RemoteException e) {
        throw new VisADException("Couldn't finish image initialization");
    }

  }


  /**
    * get the dimensions of the image
    *
    * @return dim[0]=number of bands, dim[1] = number of elements,
    *   dim[2] = number of lines
   */
  public int[] getDimensions() {
    int[] dim = new int[3];
    dim[0] = areaDirectory.getNumberOfBands();
    dim[1] = areaDirectory.getElements();
    dim[2] = areaDirectory.getLines();
    return dim;
  }

  /**
    * get the CoordinateSystem of the image
    *
    * @return the CoordinateSystem object
   */
  public CoordinateSystem getCoordinateSystem() {
    return cs;
  }

  /**
    * get the AreaDirectory of the image
    *
    * @return the AreaDirectory object
   */
  public AreaDirectory getAreaDirectory() {
    return areaDirectory;
  }

  /**
   * Return a FlatField representing the image.  The field will look
   * like the following:<P>
   * <UL>
   * <LI>Domain - Linear2DSet of (ImageLine, ImageElement) with
   *              McIDASVAREACoordinateSystem to Lat/Lon (may be null).
   * <LI>Range - One or more bands.  If calibration type is BRIT,
   *             Integer1DSets are used as the range sets with length 255.
   *             (brightness 255 is same as 254).
   * </UL>
   *
   * @return image as a FlatField
   */

  public FlatField getData() {
    /*
    if (field.getRangeDimension() == 1) {
       try {
           return (FlatField) getImage();
       } catch (VisADException ve) { ve.printStackTrace();}
    } 
    */
    return (FlatField) field;
  }

  /**
   * Retrieves the "nominal" time of the image as a VisAD DateTime.  This
   * may or may not be the start of the image scan.  Values are derived from
   * the 4th and 5th words in the AREA file directory.
   * @see <a href="http://www.ssec.wisc.edu/mug/prog_man/prog_man.html">
   * McIDAS Programmer's Manual</a>
   * @see #getImageStartTime()
   *
   * @ returns  nominal image time
   */
  public DateTime getNominalTime()
      throws VisADException
  {
      return new DateTime(areaDirectory.getNominalTime());
  }

  /**
   * Retrieves the time of the start of the image scan as a VisAD DateTime.
   * Values are derived from the 46th and 47th words in the AREA file directory.
   * @see <a href="http://www.ssec.wisc.edu/mug/prog_man/prog_man.html">
   * McIDAS Programmer's Manual</a>
   * @see #getNominalTime()
   *
   * @ returns  time of the start of the image scan
   */
  public DateTime getImageStartTime()
      throws VisADException
  {
      return new DateTime(areaDirectory.getStartTime());
  }

// WLH 24 August 2000
  /**
   * Retrieves the first (and/or only) band in an image as a SingleBandedImage
   *
   * @return  SingleBandedImage representation of the FlatField from getData().
   *          If there is navigation associated with the image, the returned
   *          image is a NavigatedImage.
   */
  public SingleBandedImage getImage() throws VisADException {

    SingleBandedImage firstBand;
    if (field.getRangeDimension() > 1) {
      try {
        firstBand = 
          (cs == null)
            ? (SingleBandedImage)
              new SingleBandedImageImpl(((FlatField) field.extract(0)),
                                         getNominalTime(),
                                         "McIDAS Image", false)
            : (SingleBandedImage)
              new NavigatedImage(((FlatField) field.extract(0)),
                                  getNominalTime(),
                                  "McIDAS Image", false);
      } catch (RemoteException excp) {
        throw new VisADException("MAreaAdapter.getImage(): RemoteException");
      }

    } else {
      firstBand = (SingleBandedImage) field;
    }
    return firstBand;

  }

}
