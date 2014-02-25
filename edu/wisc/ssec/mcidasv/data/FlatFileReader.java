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
package edu.wisc.ssec.mcidasv.data;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.*;
import java.rmi.RemoteException;

import ucar.unidata.data.BadDataException;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.util.IOUtil;
import visad.CoordinateSystem;
import visad.Data;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded2DSet;
import visad.Linear2DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.VisADException;
import visad.util.ImageHelper;

import edu.wisc.ssec.mcidasv.data.HeaderInfo;
import edu.wisc.ssec.mcidasv.data.hydra.LongitudeLatitudeCoordinateSystem;
import edu.wisc.ssec.mcidasv.data.hydra.SwathNavigation;

public class FlatFileReader {
	
    /** The url */
	private String url = null;
	
	/** The dimensions */
	private int lines = 0;
	private int elements = 0;
	private int strideLines = 0;
	private int strideElements = 0;
	private int band = 1;
	private int bandCount = 1;
	private String unit = "";
	private int stride = 1;
	
	/** The nav dimensions */
	private int navLines = 0;
	private int navElements = 0;
	
	/** The data parameters */
	private String interleave = HeaderInfo.kInterleaveSequential;
	private boolean bigEndian = false;
	private int offset = 0;
	private String delimiter = "\\s+";
	private int dataScale = 1;
	
	/** The nav parameters */
	private double ulLat = Double.NaN;
	private double ulLon = Double.NaN;
	private double lrLat = Double.NaN;
	private double lrLon = Double.NaN;
	private String latFile = null;
	private String lonFile = null;
	private int latlonScale = 1;
	private boolean eastPositive = false;
	    	    	
	/** which format this object is representing */
	private int myFormat = HeaderInfo.kFormatUnknown;
	private int myNavigation = HeaderInfo.kNavigationUnknown;
	
	/** the actual floats read from the file */
	private float[] floatData = null;
	
	/** cache the nav info when possible */
	Gridded2DSet navigationSet = null;
	CoordinateSystem navigationCoords = null;
	
    /**
     * Ctor for xml encoding
     */
    public FlatFileReader() {
    	this.floatData = null;
    }

    /**
     * CTOR
     *
     * @param filename The filename
     */
    public FlatFileReader(String filename) {
    	this.floatData = null;
        this.url = filename;
    }

    /**
     * CTOR
     *
     * @param filename The filename
     * @param lines The number of lines
     * @param elements The number of elements
     */
    public FlatFileReader(String filename, int lines, int elements) {
    	this.floatData = null;
        this.url = filename;
        this.lines = lines;
        this.elements = elements;
        setStride(1);
    }
    
    /**
     * @param format
     * @param interleave
     * @param bigEndian
     * @param offset
     * @param band
     * @param bandCount
     */
    public void setBinaryInfo(int format, String interleave, boolean bigEndian, int offset, int band, int bandCount) {
        this.myFormat = format;
    	this.interleave = interleave;
    	this.bigEndian = bigEndian;
    	this.offset = offset;
    	this.band = band;
    	this.bandCount = bandCount;
    }

    /**
     * @param delimiter The data value delimiter
     * @param dataScale The data scale factor
     */
    public void setAsciiInfo(String delimiter, int dataScale) {
        this.myFormat = HeaderInfo.kFormatASCII;
    	if (delimiter == null || delimiter.trim().length() == 0) delimiter="\\s+";
    	this.delimiter = delimiter;
    	this.dataScale = dataScale;
    }

    /**
     */
    public void setImageInfo() {
		this.myFormat = HeaderInfo.kFormatImage;
    }

    /**
     * @param ulLat The upper left latitude
     * @param ulLon The upper left longitude
     * @param lrLat The lower right latitude
     * @param lrLon The lower right longitude
     */
    public void setNavBounds(double ulLat, double ulLon, double lrLat, double lrLon) {
    	this.myNavigation = HeaderInfo.kNavigationBounds;
    	this.ulLat = ulLat;
    	this.ulLon = ulLon;
    	this.lrLat = lrLat;
    	this.lrLon = lrLon;
    	this.latlonScale = 1;
    }
    
    /**
     * @param latFile Path to the latitude file.
     * @param lonFile Path to the longitude file.
     * @param latlonScale The navigation value scaling
     */
    public void setNavFiles(String latFile, String lonFile, int latlonScale) {
    	this.myNavigation = HeaderInfo.kNavigationFiles;
    	this.latFile = latFile;
    	this.lonFile = lonFile;
    	this.latlonScale = latlonScale;
    }
    
    /**
     * @param eastPositive
     */
    public void setEastPositive(boolean eastPositive) {
    	this.eastPositive = eastPositive;
    }

    /**
     * @param stride
     */
    public void setStride(int stride) {
    	if (stride < 1) stride=1;
    	this.stride = stride;
    	this.strideElements = (int)Math.ceil((float)this.elements/(float)stride);
		this.strideLines = (int)Math.ceil((float)this.lines/(float)stride);
    }

    /**
     * @param unit
     */
    public void setUnit(String unit) {
    	if (unit.trim().equals("")) unit="";
    	this.unit = unit;
    }

    /**
     * Read floats from a binary file
     */
    private void readFloatsFromBinary() {
    	System.out.println("FlatFileInfo.readFloatsFromBinary()");
        		
    	int bytesEach = 1;
		switch (this.myFormat) {
		case HeaderInfo.kFormat1ByteUInt:
			bytesEach = 1;
			break;
		case HeaderInfo.kFormat2ByteUInt:
			bytesEach = 2;
			break;
		case HeaderInfo.kFormat2ByteSInt:
			bytesEach = 2;
			break;
		case HeaderInfo.kFormat4ByteSInt:
			bytesEach = 4;
			break;
		case HeaderInfo.kFormat4ByteFloat:
			bytesEach = 4;
			break;
		default:
			System.err.println("FlatFileReader: Unrecognized binary format: " + this.myFormat);
			return;
		}

        int curPixel = 0;
		int curElement = 0;
		int curLine = 0;
		int lastRead = 0;
		int readEach = 8192;
		int startPointer = 0;
		int endPointer = -1;
		int pixelPointer = 0;

		int readPixels = this.strideElements * this.strideLines;
        this.floatData = new float[readPixels];
		byte[] readBytes = new byte[readEach];

    	try {            
    		FileInputStream fis = new FileInputStream(url);
    		
    		// byte boundaries
    		assert(readEach % 64 == 0);
    		
    		// assure we read the first time
    		assert(endPointer < 0);
    		
    		while ((curPixel < readPixels && curLine < lines && lastRead > 0) || curPixel == 0) {
    			
    			pixelPointer = this.offset;
    			if (this.interleave.equals(HeaderInfo.kInterleaveSequential)) {
    				// Skip to the right band
    				pixelPointer += (this.band - 1) * (this.lines * this.elements * bytesEach);
    				// Skip into the band
    				pixelPointer += (curLine * this.elements * bytesEach) + (curElement * bytesEach);
    			}
    			else if (this.interleave.equals(HeaderInfo.kInterleaveByLine)) {
    				// Skip to the right line
    				pixelPointer += curLine * (this.bandCount * this.elements * bytesEach);
    				// Skip into the line
    				pixelPointer += ((this.band - 1) * this.elements * bytesEach) + (curElement * bytesEach);
    			}
    			else if (this.interleave.equals(HeaderInfo.kInterleaveByPixel)) {
    				// Skip to the right line
    				pixelPointer += curLine * (this.bandCount * this.elements * bytesEach);
    				// Skip into the line
    				pixelPointer += (curElement * bandCount * bytesEach) + ((this.band - 1) * bytesEach);
    			}
    			else {
    				System.err.println("FlatFileReader: Unrecognized interleave type: " + this.interleave);
    			}
    			
    			// We need data outside of our buffer
    			if (pixelPointer > endPointer) {
    				
        			// Skip ahead to useful data
    				int skipBytes = pixelPointer - endPointer - 1;
    				if (skipBytes > 0) {
//        				System.out.println(" Skipping " + skipBytes + " bytes");
        				startPointer += lastRead + fis.skip(skipBytes);
        				endPointer = startPointer;
    				}

        			// Read more bytes 
    				lastRead = fis.read(readBytes);
    				if (startPointer != endPointer)
    					startPointer = endPointer + 1;
    				endPointer = startPointer + lastRead - 1;
//    				System.out.println(" Read " + lastRead + " bytes, from " + startPointer);

    			}
    			
    			int readOffset = pixelPointer - startPointer;
    			float readFloat = 0.0f;
    			switch (this.myFormat) {
    			case HeaderInfo.kFormat1ByteUInt:
    				readFloat = (float)bytesTo1ByteUInt(readBytes, readOffset);
    				break;
    			case HeaderInfo.kFormat2ByteUInt:
    				int newInt = bytesTo2ByteUInt(readBytes, readOffset);
    				if (this.bigEndian) {
    					newInt = (((newInt&0xff)<<8)|((newInt&0xff00)>>8));
    				}
    				readFloat = (float)newInt;
    				break;
    			case HeaderInfo.kFormat2ByteSInt:
    				readFloat = (float)bytesTo2ByteSInt(readBytes, readOffset);
    				break;
    			case HeaderInfo.kFormat4ByteSInt:
    				readFloat = (float)bytesTo4ByteSInt(readBytes, readOffset);
    				break;
    			case HeaderInfo.kFormat4ByteFloat:
    				readFloat = (float)bytesTo4ByteFloat(readBytes, readOffset);
    				break;
    			}
    			this.floatData[curPixel++] = readFloat;

    			curElement+=stride;
    			if (curElement >= elements) {
    				curElement = 0;
    				curLine+=stride;
    			}
    		}
    		
            fis.close();
            
            System.out.println("  read " + curPixel + " floats (expected " + readPixels + ")");
            
    	} catch (NumberFormatException exc) {
    		throw new BadDataException("Error parsing binary file");
    	} catch (Exception e) {
    		throw new BadDataException("Error reading binary file: " + url + "\n" + e);
    	}
    }
        
    /**
     * Read floats from an ASCII file
     */
    private void readFloatsFromAscii() {
    	System.out.println("FlatFileInfo.readFloatsFromAscii()");
    	
        int curPixel = 0;
		int curElement = 0;
		int curLine = 0;
		
		int readPixels = this.strideElements * this.strideLines;
		this.floatData = new float[readPixels];
        
    	try {            
    		InputStream is = IOUtil.getInputStream(url, getClass());
    		BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String aLine;
            
            while ((aLine = in.readLine()) != null) {
            	aLine = aLine.trim();
            	String[] words = aLine.split(delimiter);
            	for (int i=0; i<words.length; i++) {
            		
        			if (curLine % stride == 0 && curElement % stride == 0) {
        				this.floatData[curPixel++] = Float.parseFloat(words[i]);
        			}
        			
        			// Keep track of what element/line we are reading so we can stride appropriately
        			curElement++;
        			if (curElement >= elements) {
        				curElement = 0;
        				curLine++;
        			}
        			if (curLine > lines || curPixel > readPixels) {
        	    		throw new BadDataException("Error parsing ASCII file: Bad dimensions");
        			}

            	}    			
            }
            in.close();
                        
            System.out.println("  read " + curPixel + " floats (expected " + readPixels + ")");
           
    	} catch (NumberFormatException exc) {
    		throw new BadDataException("Error parsing ASCII file");
    	} catch (Exception e) {
    		throw new BadDataException("Error reading ASCII file: " + url + "\n" + e);
    	}
    }
    
    /**
     * Make a FlatField from an Image
     */
    private Data getDataFromImage() {
    	System.out.println("FlatFileInfo.getDataFromImage()");
    	try {
    		this.floatData = new float[0];
    		InputStream is = IOUtil.getInputStream(url, getClass());
    		byte[] imageContent = IOUtil.readBytes(is);
    		Image image = Toolkit.getDefaultToolkit().createImage(imageContent);
    		ImageHelper ih = new ImageHelper();
    		image.getWidth(ih);
    		if (ih.badImage) {
    			throw new IllegalStateException("Bad image: " + url);
    		}
    		
            makeCoordinateSystem();

    		FlatField field = (FlatField) ucar.visad.Util.makeField(image,true);
			return GridUtil.setSpatialDomain(field, navigationSet);
			
    	} catch (Exception e) {
    		throw new BadDataException("Error reading image file: " + url + "\n" + e);
    	}
    }

    /**
     * Make a Gridded2DSet from bounds
     */
    private Gridded2DSet getNavigationSetFromBounds() {
    	System.out.println("FlatFileInfo.getNavigationSetFromBounds()");
	    try {
	    	this.navElements = this.strideElements;
	    	this.navLines = this.strideLines;
	    	int lonScale = this.latlonScale;
	    	int latScale = this.latlonScale;
	    	if (eastPositive) lonScale *= -1;
	    	return new Linear2DSet(RealTupleType.SpatialEarth2DTuple,
	    			ulLon / lonScale, lrLon / lonScale, navElements,
	    			ulLat / latScale, lrLat / latScale, navLines);
	    } catch (Exception e) {
			throw new BadDataException("Error setting navigation bounds:\n" + e);
	    }
    }

    /**
     * Make a Gridded2DSet from files
     */
    private Gridded2DSet getNavigationSetFromFiles() {
    	System.out.println("FlatFileInfo.getNavigationSetFromFiles()");
    	try {
            float[][] lalo = new float[0][0];
            
            FlatFileReader lonData, latData;
            
            // ASCII nav files
            if (this.myFormat == HeaderInfo.kFormatASCII) {
            	System.out.println("  ASCII nav file");
	            
        		this.navElements = this.elements;
        		this.navLines = this.lines;
                lalo = new float[2][navElements * navLines];
        		            		            		
        		// Longitude band
        		lonData = new FlatFileReader(lonFile, navLines, navElements);
        		lonData.setAsciiInfo(delimiter, 1);
        		
        		// Latitude band
        		latData = new FlatFileReader(latFile, navLines, navElements);
        		latData.setAsciiInfo(delimiter, 1);
        			            
            }
            
            // Binary nav files
            else {
            	
            	System.out.println("  Binary nav file");

            	// ENVI header for nav
            	EnviInfo enviLat = new EnviInfo(latFile);
            	EnviInfo enviLon = new EnviInfo(lonFile);
            	if (enviLat.isNavHeader() && enviLon.isNavHeader()) {
                	System.out.println("    ENVI nav file");

            		this.navElements = enviLat.getParameter(HeaderInfo.ELEMENTS, 0);
            		this.navLines = enviLat.getParameter(HeaderInfo.LINES, 0);
                    lalo = new float[2][navElements * navLines];
            		            		            		
            		// Longitude band
            		lonData = new FlatFileReader(enviLon.getLonBandFile(), 
            				enviLon.getParameter(HeaderInfo.LINES, 0),
            				enviLon.getParameter(HeaderInfo.ELEMENTS, 0));
            		lonData.setBinaryInfo(
            				enviLon.getParameter(HeaderInfo.DATATYPE, HeaderInfo.kFormatUnknown),
            				enviLon.getParameter(HeaderInfo.INTERLEAVE, HeaderInfo.kInterleaveSequential),
            				enviLon.getParameter(HeaderInfo.BIGENDIAN, false),
            				enviLon.getParameter(HeaderInfo.OFFSET, 0),
            				enviLon.getLonBandNum(),
            				enviLon.getBandCount());
            		
            		// Latitude band
            		latData = new FlatFileReader(enviLat.getLatBandFile(), 
            				enviLat.getParameter(HeaderInfo.LINES, 0),
            				enviLat.getParameter(HeaderInfo.ELEMENTS, 0));
            		latData.setBinaryInfo(
            				enviLat.getParameter(HeaderInfo.DATATYPE, HeaderInfo.kFormatUnknown),
            				enviLat.getParameter(HeaderInfo.INTERLEAVE, HeaderInfo.kInterleaveSequential),
            				enviLat.getParameter(HeaderInfo.BIGENDIAN, false),
            				enviLat.getParameter(HeaderInfo.OFFSET, 0),
            				enviLat.getLatBandNum(),
            				enviLat.getBandCount());
            		
            	}
            	
            	else {
                	System.out.println("    AXFORM nav file");

            		this.navElements = this.elements;
            		this.navLines = this.lines;
                    lalo = new float[2][navElements * navLines];
            		            		            		
            		// Longitude band
            		lonData = new FlatFileReader(lonFile, navLines, navElements);
            		lonData.setBinaryInfo(HeaderInfo.kFormat2ByteUInt, HeaderInfo.kInterleaveSequential, bigEndian, offset, 1, 1);
            		
            		// Latitude band
            		latData = new FlatFileReader(latFile, navLines, navElements);
            		latData.setBinaryInfo(HeaderInfo.kFormat2ByteUInt, HeaderInfo.kInterleaveSequential, bigEndian, offset, 1, 1);
            		
            	}
            	            	
            }
                        
    		// Set the stride if the dimensions are the same and read the floats
    		if (this.lines == this.navLines && this.elements == this.navElements && stride != 1) {
    			System.out.println("Setting stride for nav files: " + stride);
    			lonData.setStride(this.stride);
    			latData.setStride(this.stride);
    			this.navElements = this.strideElements;
    			this.navLines = this.strideLines;
                lalo = new float[2][this.navElements * this.navLines];
    		}
    		lalo[0] = lonData.getFloats();
    		lalo[1] = latData.getFloats();
    		
    		// Take into account scaling and east positive
    		int latScale = this.latlonScale;
			int lonScale = this.latlonScale;
			if (eastPositive) lonScale = -1 * lonScale;
    		for (int i=0; i<lalo[0].length; i++) {
    			lalo[0][i] = lalo[0][i] / (float)lonScale;
    		}
    		for (int i=0; i<lalo[1].length; i++) {
    			lalo[1][i] = lalo[1][i] / (float)latScale;
    		}
    		
            return new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
            		lalo, navElements, navLines,
            		null, null, null,
            		false, false);
            
    	} catch (NumberFormatException exc) {
    		throw new BadDataException("Error parsing ASCII navigation file");
    	} catch (Exception e) {
    		throw new BadDataException("Error setting navigation from file: " + url + "\n" + e);
    	}
    }
        
    /**
     * Create navigation info if it hasn't been built
     */
    private void makeCoordinateSystem() {
    	System.out.println("FlatFileInfo.makeCoordinateSystem()");
    	
    	if (navigationSet != null && navigationCoords != null) return;

    	switch (this.myNavigation) {
    	case HeaderInfo.kNavigationBounds:
    		navigationSet = getNavigationSetFromBounds();
    		break;
    	case HeaderInfo.kNavigationFiles:
    		navigationSet = getNavigationSetFromFiles();
    		break;
   		default:
   			System.err.println("Unknown navigation format");
    	}
    	
		// myElements, myLines: Nav dimensions
		// this.elements, this.lines: Data dimensions
		float ratioElements = (float)this.strideElements / (float)this.navElements;
		float ratioLines = (float)this.strideLines / (float)this.navLines;
		int[] geo_start = new int[2];
		int[] geo_count = new int[2];
		int[] geo_stride = new int[2];
		try {
			Linear2DSet domainSet = SwathNavigation.getNavigationDomain(
					0, strideElements-1, 1,
					0, strideLines-1, 1, 
					ratioElements, ratioLines, 0, 0, 
					geo_start, geo_count, geo_stride);
						
//			System.out.println("makeCoordinateSystem stats for " + url + ":");
//			System.out.println("  Elements: " + strideElements + ", Lines: " + strideLines);
//			System.out.println("  navElements: " + navElements + ", navLines: " + navLines);
//			System.out.println("  ratioElements: " + ratioElements + ", ratioLines: " + ratioLines);
//			System.out.println("  navigationSet: " + navigationSet.getLength(0) + " x " + navigationSet.getLength(1));
//			System.out.println("  geo_start: " + geo_start[0] + ", " + geo_start[1]);
//			System.out.println("  geo_count: " + geo_count[0] + ", " + geo_count[1]);
//			System.out.println("  geo_stride: " + geo_stride[0] + ", " + geo_stride[1]);
//			System.out.println("  domainSet: " + domainSet.getLength(0) + " x " + domainSet.getLength(1));
//			System.out.println("  domainSet.toString(): " + domainSet.toString());
			
			navigationCoords = new LongitudeLatitudeCoordinateSystem(domainSet, navigationSet);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * Return a valid data object for a DataSource
     */
    public Data getData() {
    	System.out.println("FlatFileInfo.getData()");

    	Data d = null;
    	FlatField field;

    	try {

    		switch (this.myFormat) {
    		case HeaderInfo.kFormatImage:
    			d = getDataFromImage();
    			break;
    		default:
    			this.floatData = getFloats();
    			field = getFlatField();
//    			d = GridUtil.setSpatialDomain(field, navigationSet);
    			d = field;
    			break;
    		}

    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	} catch (VisADException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
		}

        return d;
    }
    
    /**
     * Return the array of floats making up the data
     */
    public float[] getFloats() {
    	System.out.println("FlatFileInfo.getFloats()");
    	
    	if (this.floatData != null) return this.floatData;
    	
    	switch (this.myFormat) {
    	case HeaderInfo.kFormatImage:
    		break;
    	case HeaderInfo.kFormatASCII:
    		readFloatsFromAscii();
    		break;
   		default:
    		readFloatsFromBinary();
   			break;
    	}
    	
    	
    	
		// DEBUG!
//    	File justName = new File(url);
//    	try {
//    		BufferedWriter out = new BufferedWriter(new FileWriter("/tmp/mcv/" + justName.getName()));
//    		for (int i=0; i<this.floatData.length; i++) {
//    			if (i%strideElements==0) out.write("New line " + (i/strideElements) + " at element " + i + "\n");
//    			out.write(this.floatData[i] + "\n");
//    		}
//    		out.close();
//    	} 
//    	catch (IOException e) { 
//    		System.out.println("Exception ");
//    	}

		
    	
    	return this.floatData;
    }
        
    /**
     * float array -> flatfield
     */
    private FlatField getFlatField()
    throws IOException, VisADException {
    	
        makeCoordinateSystem();
        
//    	RealType[]    unit 			= new RealType[] { RealType.Generic };
//    	RealTupleType unitType       = new RealTupleType(unit);
        
        RealType	  unitType			= RealType.getRealType(unit);

    	RealType      line              = RealType.getRealType("ImageLine");
        RealType      element           = RealType.getRealType("ImageElement");
        RealType[]    domain_components = { element, line };
        RealTupleType image_domain      = new RealTupleType(domain_components, navigationCoords, null);
        FunctionType  image_type 		= new FunctionType(image_domain, unitType);
        Linear2DSet   domain_set 		= new Linear2DSet(image_domain,
        		0.0, (float) (strideElements - 1.0), strideElements,
        		0.0, (float) (strideLines - 1.0), strideLines);

        FlatField    field      = new FlatField(image_type, domain_set);

        float[][]    samples    = new float[][] { this.floatData };
        try {
            field.setSamples(samples, false);
        } catch (RemoteException e) {
            throw new VisADException("Couldn't finish FlatField initialization");
        }
        
        return field;
    }
        
    /**
     * toString
     *
     * @return toString
     */
    public String toString() {
        return "url: " + url + ", lines: " + lines + ", elements: " + elements;
    }
    
    // byte[] conversion functions
    // TODO: are these replicated elsewhere in McV?

	private static int bytesTo1ByteUInt (byte[] bytes, int offset) {
		return (int) ( bytes[offset] & 0xff );
	}
	
	private static int bytesTo2ByteUInt (byte[] bytes, int offset) {
		int accum = 0;
		for ( int shiftBy = 0; shiftBy < 16; shiftBy += 8 ) {
			accum |= ( (long)( bytes[offset] & 0xff ) ) << shiftBy;
			offset++;
		}
		return (int)( accum );
	}
	
	private static int bytesTo2ByteSInt (byte[] bytes, int offset) {
		return (bytesTo2ByteUInt(bytes, offset)) - 32768;
	}
	
	private static int bytesTo4ByteSInt (byte[] bytes, int offset) {
		int accum = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
			accum |= ( (long)( bytes[offset] & 0xff ) ) << shiftBy;
			offset++;
		}
		return (int)( accum );
	}

	private static float bytesTo4ByteFloat (byte[] bytes, int offset) {
		int accum = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
			accum |= ( (long)( bytes[offset] & 0xff ) ) << shiftBy;
			offset++;
		}
		return Float.intBitsToFloat(accum);
	}	
	
	private static long bytesToLong (byte[] bytes) {
		if (bytes.length != 4) return 0;
		long accum = 0;
		int i = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
			accum |= ( (long)( bytes[i] & 0xff ) ) << shiftBy;
			i++;
		}
		return accum;
	}
	
	private static double bytesToDouble (byte[] bytes) {
		if (bytes.length != 8) return 0;
		long accum = 0;
		int i = 0;
		for ( int shiftBy = 0; shiftBy < 64; shiftBy += 8 ) {
			accum |= ( (long)( bytes[i] & 0xff ) ) << shiftBy;
			i++;
		}
		return Double.longBitsToDouble(accum);
	}

}
