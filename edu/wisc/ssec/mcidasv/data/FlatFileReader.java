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
	float[] floatData = null;
	
	/** cache the nav info when possible */
	Gridded2DSet navigationSet = null;
	CoordinateSystem navigationCoords = null;
	
    /**
     * Ctor for xml encoding
     */
    public FlatFileReader() {}

    /**
     * CTOR
     *
     * @param filename The filename
     */
    public FlatFileReader(String filename) {
        this.url = filename;
    }

    /**
     * CTOR
     *
     * @param filename The filename
     * @param lines The number of lines
     * @param elements The number of elements
     * @param band The band
     */
    public FlatFileReader(String filename, int lines, int elements, int band) {
        this.url = filename;
        this.lines = lines;
        this.elements = elements;
        this.band = band;
        setStride(1);
    }
    
    /**
     * @param delimiter The data value delimiter
     * @param dataScale The data scale factor
     */
    public void setBinaryInfo(int format, String interleave, boolean bigEndian, int offset, int bandCount) {
        this.myFormat = format;
    	this.interleave = interleave;
    	this.bigEndian = bigEndian;
    	this.offset = offset;
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
     * @param delimiter The data value delimiter
     * @param dataScale The data scale factor
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
     * @param ulLat The latitude file
     * @param ulLon The longitude file
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
    	this.strideElements = (int)Math.ceil(this.elements/stride);
		this.strideLines = (int)Math.ceil(this.lines/stride);
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
		case HeaderInfo.kFormat2ByteSInt:
			bytesEach = 2;
			break;
		case HeaderInfo.kFormat4ByteSInt:
			bytesEach = 4;
			break;
		case HeaderInfo.kFormat4ByteFloat:
			bytesEach = 4;
			break;
		case HeaderInfo.kFormat8ByteDouble:
			bytesEach = 8;
			break;
		case HeaderInfo.kFormat2x8Byte:
			bytesEach = 16;
			break;
		case HeaderInfo.kFormat2ByteUInt:
			bytesEach = 2;
			break;
		}

        int curPixel = 0;
		int curElement = 0;
		int curLine = 0;
		int prevOffset = 0;
		int nextOffset = 0;
		int skipBytes = 0;
		int lastRead = 0;

		int readPixels = this.strideElements * this.strideLines;
        floatData = new float[readPixels];
		byte[] readBytes = new byte[bytesEach];

    	try {            
    		FileInputStream fis = new FileInputStream(url);
    		
    		while (curPixel < readPixels && curLine < lines) {
    			
    	    	nextOffset = this.offset;
    			if (this.interleave.equals(HeaderInfo.kInterleaveSequential)) {
    				// Skip to the right band
    				nextOffset += (this.band - 1) * (this.lines * this.elements * bytesEach);
    				// Skip into the band
    				nextOffset += (curLine * this.elements * bytesEach) + (curElement * bytesEach);
    			}
    			else if (this.interleave.equals(HeaderInfo.kInterleaveByLine)) {
    				// Skip to the right line
    				nextOffset += curLine * (this.bandCount * this.elements * bytesEach);
    				// Skip into the line
    				nextOffset += ((this.band - 1) * this.elements * bytesEach) + (curElement * bytesEach);
    			}
    			else if (this.interleave.equals(HeaderInfo.kInterleaveByPixel)) {
    				// Skip to the right line
    				nextOffset += curLine * (this.bandCount * this.elements * bytesEach);
    				// Skip into the line
    				nextOffset += (curElement * bandCount * bytesEach) + ((this.band - 1) * bytesEach);
    			}
    			else {
    				System.err.println("FlatFileReader: Unrecognized interleave type: " + this.interleave);
    			}
    			
    			skipBytes = nextOffset - prevOffset - lastRead;
    			if (skipBytes>0) {
    				fis.skip(skipBytes);
    			}
    			prevOffset = nextOffset;
    			lastRead = fis.read(readBytes);
    			
    			switch (this.myFormat) {
    			case HeaderInfo.kFormat1ByteUInt:
    				floatData[curPixel++] = (float)bytesToInt(readBytes, false);
    				break;
    			case HeaderInfo.kFormat2ByteSInt:
    				floatData[curPixel++] = (float)bytesToInt(readBytes, true);
    				break;
    			case HeaderInfo.kFormat4ByteSInt:
    				floatData[curPixel++] = (float)bytesToInt(readBytes, true);
    				break;
    			case HeaderInfo.kFormat4ByteFloat:
    				floatData[curPixel++] = (float)bytesToFloat(readBytes);
    				break;
    			case HeaderInfo.kFormat8ByteDouble:
    				System.err.println("FlatFileInfo: Can't handle kFormat8ByteDouble yet");
    				break;
    			case HeaderInfo.kFormat2x8Byte:
    				System.err.println("FlatFileInfo: Can't handle kFormat2x8Byte yet");
    				break;
    			case HeaderInfo.kFormat2ByteUInt:
    				floatData[curPixel++] = (float)bytesToInt(readBytes, false);
    				break;
    			default:
    				System.err.println("FlatFileReader: Unrecognized binary format: " + this.myFormat);
    			}

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
        floatData = new float[readPixels];

    	try {            
    		InputStream is = IOUtil.getInputStream(url, getClass());
    		BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String aLine;
            
            while ((aLine = in.readLine()) != null) {
            	aLine = aLine.trim();
            	String[] words = aLine.split(delimiter);
            	for (int i=0; i<words.length; i++) {
            		
        			if (curLine % stride == 0 && curElement % stride == 0) {
        				
        				floatData[curPixel++] = Float.parseFloat(words[i]);
        			
        			}
        			
        			// Keep track of what element/line we are reading so we can stride appropriately
        			// Take a slight hit in disk efficiency for the sake of code clarity (I hope...)
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
    		floatData = new float[0];
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
        		lonData = new FlatFileReader(lonFile, navLines, navElements, 1);
        		lonData.setAsciiInfo(delimiter, 1);
        		
        		// Latitude band
        		latData = new FlatFileReader(latFile, navLines, navElements, 1);
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
            				enviLon.getParameter(HeaderInfo.ELEMENTS, 0),
            				enviLon.getLonBandNum());
            		lonData.setBinaryInfo(
            				enviLon.getParameter(HeaderInfo.DATATYPE, HeaderInfo.kFormatUnknown),
            				enviLon.getParameter(HeaderInfo.INTERLEAVE, HeaderInfo.kInterleaveSequential),
            				enviLon.getParameter(HeaderInfo.BIGENDIAN, false),
            				enviLon.getParameter(HeaderInfo.OFFSET, 0),
            				enviLon.getBandCount());
            		
            		// Latitude band
            		latData = new FlatFileReader(enviLat.getLatBandFile(), 
            				enviLat.getParameter(HeaderInfo.LINES, 0),
            				enviLat.getParameter(HeaderInfo.ELEMENTS, 0),
            				enviLat.getLatBandNum());
            		latData.setBinaryInfo(
            				enviLat.getParameter(HeaderInfo.DATATYPE, HeaderInfo.kFormatUnknown),
            				enviLat.getParameter(HeaderInfo.INTERLEAVE, HeaderInfo.kInterleaveSequential),
            				enviLat.getParameter(HeaderInfo.BIGENDIAN, false),
            				enviLat.getParameter(HeaderInfo.OFFSET, 0),
            				enviLat.getBandCount());
            		
            	}
            	
            	else {
                	System.out.println("    AXFORM nav file");

            		this.navElements = this.elements;
            		this.navLines = this.lines;
                    lalo = new float[2][navElements * navLines];
            		            		            		
            		// Longitude band
            		lonData = new FlatFileReader(lonFile, navLines, navElements, 1);
            		lonData.setBinaryInfo(HeaderInfo.kFormat2ByteUInt, HeaderInfo.kInterleaveSequential, bigEndian, offset, 1);
            		
            		// Latitude band
            		latData = new FlatFileReader(latFile, navLines, navElements, 1);
            		latData.setBinaryInfo(HeaderInfo.kFormat2ByteUInt, HeaderInfo.kInterleaveSequential, bigEndian, offset, 1);
            		
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
						
			System.out.println("makeCoordinateSystem stats for " + url + ":");
//			System.out.println("  Elements: " + strideElements + ", Lines: " + strideLines);
//			System.out.println("  navElements: " + navElements + ", navLines: " + navLines);
//			System.out.println("  ratioElements: " + ratioElements + ", ratioLines: " + ratioLines);
			System.out.println("  navigationSet: " + navigationSet.getLength(0) + " x " + navigationSet.getLength(1));
//			System.out.println("  geo_start: " + geo_start[0] + ", " + geo_start[1]);
//			System.out.println("  geo_count: " + geo_count[0] + ", " + geo_count[1]);
			System.out.println("  geo_stride: " + geo_stride[0] + ", " + geo_stride[1]);
			System.out.println("  domainSet: " + domainSet.getLength(0) + " x " + domainSet.getLength(1));
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
    			floatData = getFloats();
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
    	
    	if (floatData != null) return floatData;
    	
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
//    	printFloats(strideLines-2);
//    	printFloats(strideLines-1);
//    	File justName = new File(url);
//    	try {
//    		BufferedWriter out = new BufferedWriter(new FileWriter("/tmp/mcv/" + justName.getName()));
//    		for (int i=0; i<floatData.length; i++) {
//    			if (i%strideElements==0) out.write("New line " + (i/strideElements) + " at element " + i + "\n");
//    			out.write(floatData[i] + "\n");
//    		}
//    		out.close();
//    	} 
//    	catch (IOException e) { 
//    		System.out.println("Exception ");
//
//    	}


    	
		
    	
    	return floatData;
    }
    
    /**
     * debug helper: print the floats at the beginning and end of a given line
     */
    private void printFloats(int line) {
    	System.out.println("Floats read from file " + url);
    	int quant = 3;
		int offset = line * strideElements;
		if (offset >= floatData.length) {
			System.err.println(" Line " + line + " is outside bounds");
			return;
		}
		System.out.println(" Line " + line + ": first " + quant + ", last " + quant + ":");
		for (int i=offset; i<offset+quant; i++) {
			System.out.println("  " + floatData[i]);
		}
		System.out.println("  ...");
		offset += strideElements - quant;
		for (int i=offset; i<offset+quant; i++) {
			System.out.println("  " + floatData[i]);
		}
    }
    
    /**
     * float array -> flatfield
     */
    private FlatField getFlatField()
    throws IOException, VisADException {
    	
        makeCoordinateSystem();
        
    	RealType[]    generic 			= new RealType[] { RealType.Generic };
    	RealTupleType genericType       = new RealTupleType(generic);

    	RealType      line              = RealType.getRealType("ImageLine");
        RealType      element           = RealType.getRealType("ImageElement");
        RealType[]    domain_components = { element, line };
        RealTupleType image_domain      = new RealTupleType(domain_components, navigationCoords, null);
        FunctionType  image_type 		= new FunctionType(image_domain, genericType);
        Linear2DSet   domain_set 		= new Linear2DSet(image_domain,
        		0.0, (float) (strideElements - 1.0), strideElements,
        		0.0, (float) (strideLines - 1.0), strideLines);

        FlatField    field      = new FlatField(image_type, domain_set);

        float[][]    samples    = new float[][] { floatData };
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

	private static int bytesToInt (byte[] bytes, boolean signed) {
		if (bytes.length == 1) {
			int firstByte = bytes[0] & 0xff;
			return (int) ( firstByte );
		}
		else if (bytes.length == 2) {
			int firstByte = bytes[0] & 0xff;
			int secondByte = bytes[1] & 0xff;
			if (signed) return (int)( secondByte << 8 | firstByte ) - 32768;
			else return (int)( secondByte << 8 | firstByte );
		}
		else if (bytes.length == 4) {
			int firstByte = bytes[0] & 0xff;
			int secondByte = bytes[1] & 0xff;
			int thirdByte = bytes[3] & 0xff;
			int fourthByte = bytes[4] & 0xff;
			return (int)( fourthByte << 24 | thirdByte << 16 | secondByte << 8 | firstByte );
		}
		else {
			return 0;
		}
	}

	private static float bytesToFloat (byte[] bytes) {
		if (bytes.length != 4) return 0;
		int accum = 0;
		int i = 0;
		for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
			accum |= ( (long)( bytes[i] & 0xff ) ) << shiftBy;
			i++;
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