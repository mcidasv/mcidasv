package edu.wisc.ssec.mcidasv.data;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;

import ucar.unidata.data.BadDataException;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.util.IOUtil;
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
	private int band = 1;
	private int bandCount = 1;
	
	
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
	    	    	
	/** which format this object is representing */
	private int myFormat = HeaderInfo.kFormatUnknown;
	private int myNavigation = HeaderInfo.kNavigationUnknown;
	
	/** cache the field info when possible */
	FlatField dataField = null;
	
	/** cache the nav info when possible */
	Gridded2DSet navigationSet = null;
	
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
    	this.dataField = null;
    }

    /**
     * @param delimiter The data value delimiter
     * @param dataScale The data scale factor
     */
    public void setImageInfo() {
		this.myFormat = HeaderInfo.kFormatImage;
    	makeDataFieldFromImage();
    	try {
    		Linear2DSet ffDomain = (Linear2DSet) this.dataField.getDomainSet();
    		this.elements  = ffDomain.getX().getLength();
    		this.lines = ffDomain.getY().getLength();
    		this.band = 1;
    	} catch (Exception e) {
    		throw new BadDataException("Error creating domain:\n" + e);
    	}
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
     * Make a FlatField from an ASCII file
     */
    private void makeDataFieldFromBinary() {
    	System.out.println("FlatFileInfo.makeDataFieldFromBinary()");
        int curPointer = 0;
        
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
		default:
			System.err.println("FlatFileInfo: Unrecognized binary format");
		}

		int offsetBytes = 0;
		int skipBytes = 0;
		int skipEach = 0;
		if (this.interleave.equals(HeaderInfo.kInterleaveSequential)) {
			offsetBytes = (band-1) * this.elements * this.lines;
			skipBytes = 0;
			skipEach = 0;
		}
		else if (this.interleave.equals(HeaderInfo.kInterleaveByLine)) {
			offsetBytes = (band-1) * this.elements * bytesEach;
			skipBytes = (bandCount-1) * this.elements * bytesEach;
			skipEach = this.elements * bytesEach;
		}
		else if (this.interleave.equals(HeaderInfo.kInterleaveByPixel)) {
			offsetBytes = (band-1) * bytesEach;
			skipBytes = (bandCount-1) * bytesEach;
			skipEach = 0;
		}
		else {
			System.err.println("FlatFileInfo: Unrecognized interleave type");
		}
		
		// Skip this.offset bytes into the file to find the data
		// bytesEach:
		//  How many bytes represent each pixel value
		// offsetBytes:
		//	Skip into the data to find the first pixel of the band you want
		// skipBytes + skipEach + skipNow:
		//	Skip skipBytes bytes every skipEach bytes read to get to the next pixel
		//	skipNow is a counter keeping track of reads (init with skipEach)
		// bytesLeft:
		//	How many bytes you think are left until you have read the whole band
		
		System.out.println("bytesEach: " + bytesEach + ", offset: " + this.offset + ", offsetBytes: " + offsetBytes + ", skipBytes: " + skipBytes + ", skipEach: " + skipEach);

		int skipNow = skipEach;
    	try {            
    		FileInputStream fis = new FileInputStream(url);
    		fis.skip(this.offset);
    		fis.skip(offsetBytes);
            float[] valueArray = new float[elements * lines];

            int bytesLeft = elements * lines * bytesEach;
    		byte[] readBytes = new byte[bytesEach];
    		while (fis.read(readBytes) == bytesEach && bytesLeft > 0) {
    			bytesLeft -= bytesEach;
    			skipNow -= bytesEach;
    			switch (this.myFormat) {
    			case HeaderInfo.kFormat1ByteUInt:
            		valueArray[curPointer++] = (float)bytesToInt(readBytes, false);
    				break;
    			case HeaderInfo.kFormat2ByteSInt:
            		valueArray[curPointer++] = (float)bytesToInt(readBytes, true);
    				break;
    			case HeaderInfo.kFormat4ByteSInt:
            		valueArray[curPointer++] = (float)bytesToInt(readBytes, true);
    				break;
    			case HeaderInfo.kFormat4ByteFloat:
            		valueArray[curPointer++] = (float)bytesToFloat(readBytes);
    				break;
    			case HeaderInfo.kFormat8ByteDouble:
    				System.err.println("FlatFileInfo: Can't handle kFormat8ByteDouble yet");
    				break;
    			case HeaderInfo.kFormat2x8Byte:
    				System.err.println("FlatFileInfo: Can't handle kFormat2x8Byte yet");
    				break;
    			case HeaderInfo.kFormat2ByteUInt:
            		valueArray[curPointer++] = (float)bytesToInt(readBytes, false);
    				break;
    			default:
    				System.err.println("FlatFileInfo: Unrecognized binary format");
    			}
    			if (skipNow <= 0) {
    				fis.skip(skipBytes);
    				skipNow = skipEach;
    			}
    		}
    		
    		System.out.println("Read " + curPointer + " values (bytesLeft: " + bytesLeft + " [should be 0], available: " + fis.available() + ")");
    		            
            fis.close();
            
            dataField = makeFlatField(valueArray);
    	} catch (NumberFormatException exc) {
    		throw new BadDataException("Error parsing ASCII file");
    	} catch (Exception e) {
    		throw new BadDataException("Error reading ASCII file: " + url + "\n" + e);
    	}
    }
        
    /**
     * Make a FlatField from an ASCII file
     */
    private void makeDataFieldFromAscii() {
    	System.out.println("FlatFileInfo.makeDataFieldFromAscii()");
        int curPointer = 0;
    	try {            
    		InputStream is = IOUtil.getInputStream(url, getClass());
    		BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String aLine;
            float[] valueArray = new float[elements * lines];
            
            while ((aLine = in.readLine()) != null) {
            	aLine = aLine.trim();
            	String[] words = aLine.split(delimiter);
            	for (int i=0; i<words.length; i++) {
            		valueArray[curPointer++] = Float.parseFloat(words[i]);
            	}
            }
            in.close();
            
            dataField = makeFlatField(valueArray);
    	} catch (NumberFormatException exc) {
    		throw new BadDataException("Error parsing ASCII file");
    	} catch (Exception e) {
    		throw new BadDataException("Error reading ASCII file: " + url + "\n" + e);
    	}
    }
    
    /**
     * Make a FlatField from an Image
     */
    private void makeDataFieldFromImage() {
    	System.out.println("FlatFileInfo.makeDataFieldFromImage()");
    	try {
    		InputStream is = IOUtil.getInputStream(url, getClass());
    		byte[] imageContent = IOUtil.readBytes(is);
    		Image image = Toolkit.getDefaultToolkit().createImage(imageContent);
    		ImageHelper ih = new ImageHelper();
    		image.getWidth(ih);
    		if (ih.badImage) {
    			throw new IllegalStateException("Bad image: " + url);
    		}
    		dataField = (FlatField) ucar.visad.Util.makeField(image,true);
    	} catch (Exception e) {
    		throw new BadDataException("Error reading image file: " + url + "\n" + e);
    	}
    }

    /**
     * Make a Gridded2DSet from bounds
     */
    private void makeNavigationSetFromBounds() {
    	System.out.println("FlatFileInfo.makeNavigationSetFromBounds()");
	    try {
	    	navigationSet = new Linear2DSet(RealTupleType.SpatialEarth2DTuple,
	    			ulLon, lrLon, elements,
	    			ulLat, lrLat, lines);
	    } catch (Exception e) {
			throw new BadDataException("Error setting navigation bounds:\n" + e);
	    }
    }

    /**
     * Make a Gridded2DSet from files
     */
    private void makeNavigationSetFromFiles() {
    	System.out.println("FlatFileInfo.makeNavigationSetFromFiles()");
    	try {
    		int curOffset;
            float[][] lalo = new float[0][0];
            InputStream is;
            BufferedReader in;
            String aLine;
            
            int myElements = 0;
            int myLines = 0;
            
            // ASCII nav files
            if (this.myFormat == HeaderInfo.kFormatASCII) {
            	myElements = elements;
            	myLines = lines;
                lalo = new float[2][myElements * myLines];

	    		is = IOUtil.getInputStream(lonFile, getClass());
	    		in = new BufferedReader(new InputStreamReader(is));
	            curOffset = 0;
	            while ((aLine = in.readLine()) != null) {
	            	aLine = aLine.trim();
	            	String[] words = aLine.split(delimiter);
	            	for (int i=0; i<words.length; i++) {
	            		float laloVal = -1 * Float.parseFloat(words[i]) / latlonScale;
	            		lalo[0][curOffset++] = laloVal;
	            	}
	            }
	            in.close();
	
	    		is = IOUtil.getInputStream(latFile, getClass());
	    		in = new BufferedReader(new InputStreamReader(is));
	            curOffset = 0;
	            while ((aLine = in.readLine()) != null) {
	            	aLine = aLine.trim();
	            	String[] words = aLine.split(delimiter);
	            	for (int i=0; i<words.length; i++) {
	            		float laloVal = Float.parseFloat(words[i]) / latlonScale;
	            		lalo[1][curOffset++] = laloVal;
	            	}
	            }
	            in.close();
            }
            
            // Binary nav files
            else {
            	
            	// ENVI header for nav
            	EnviInfo enviLat = new EnviInfo(latFile);
            	EnviInfo enviLon = new EnviInfo(lonFile);
            	if (enviLat.isNavHeader() && enviLon.isNavHeader()) {
            		myElements = enviLat.getParameter(HeaderInfo.ELEMENTS, 0);
            		myLines = enviLat.getParameter(HeaderInfo.LINES, 0);
                    lalo = new float[2][myElements * myLines];
            		            		            		
            		// Longitude band
        	    	System.err.println("  longitude band " +enviLon.getLonBandNum() + ": " +
        	    			enviLon.getParameter(HeaderInfo.DATATYPE, HeaderInfo.kFormatUnknown) + ", " +
        	    			enviLon.getParameter(HeaderInfo.INTERLEAVE, HeaderInfo.kInterleaveSequential) + ", " +
        	    			enviLon.getParameter(HeaderInfo.OFFSET, 0));
            		FlatFileReader lonData = new FlatFileReader(enviLon.getLonBandFile(), 
            				enviLon.getParameter(HeaderInfo.LINES, 0),
            				enviLon.getParameter(HeaderInfo.ELEMENTS, 0),
            				enviLon.getLonBandNum());
            		lonData.setBinaryInfo(
            				enviLon.getParameter(HeaderInfo.DATATYPE, HeaderInfo.kFormatUnknown),
            				enviLon.getParameter(HeaderInfo.INTERLEAVE, HeaderInfo.kInterleaveSequential),
            				enviLon.getParameter(HeaderInfo.BIGENDIAN, false),
            				enviLon.getParameter(HeaderInfo.OFFSET, 0),
            				enviLon.getBandCount());
            		lalo[0] = lonData.getFloats();   
            		
            		// Latitude band
        	    	System.err.println("  latitude band " +enviLat.getLatBandNum() + ": " +
        	    			enviLat.getParameter(HeaderInfo.DATATYPE, HeaderInfo.kFormatUnknown) + ", " +
        	    			enviLat.getParameter(HeaderInfo.INTERLEAVE, HeaderInfo.kInterleaveSequential) + ", " +
        	    			enviLat.getParameter(HeaderInfo.OFFSET, 0));
            		FlatFileReader latData = new FlatFileReader(enviLat.getLatBandFile(), 
            				enviLat.getParameter(HeaderInfo.LINES, 0),
            				enviLat.getParameter(HeaderInfo.ELEMENTS, 0),
            				enviLat.getLatBandNum());
            		latData.setBinaryInfo(
            				enviLat.getParameter(HeaderInfo.DATATYPE, HeaderInfo.kFormatUnknown),
            				enviLat.getParameter(HeaderInfo.INTERLEAVE, HeaderInfo.kInterleaveSequential),
            				enviLat.getParameter(HeaderInfo.BIGENDIAN, false),
            				enviLat.getParameter(HeaderInfo.OFFSET, 0),
            				enviLat.getBandCount());
            		lalo[1] = latData.getFloats();
            		
            	}
            	
            	else {
            		System.err.println("FlatFileReader.makeNavigationSetFromFiles: non-ENVI binary nav not done yet");
            	}
            	
            }
            
            // Sample any LALO nav files you have into the correct dimensions for the data
            if (myElements != this.elements || myLines != this.lines) {
            	System.out.println("nav dimensions don't match data... sampling");
            	float sampledLalo[][] = new float[2][this.elements * this.lines];
            	sampledLalo[0] = sampleFloats(lalo[0], myElements, myLines, this.elements, this.lines);
            	sampledLalo[1] = sampleFloats(lalo[1], myElements, myLines, this.elements, this.lines);
            	myElements = this.elements;
            	myLines = this.lines;
            	lalo = sampledLalo;
            }
            
            
            // Use Hydra to resample the lalo grid
//            if (myElements != this.elements || myLines != this.lines) {
//            	// myElements, myLines: Nav dimensions
//            	// this.elements, this.lines: Data dimensions
//            	float ratioElements = (float)this.elements / (float)myElements;
//            	float ratioLines = (float)this.lines / (float)myLines;
//            	int[] geo_start = new int[2];
//            	int[] geo_count = new int[2];
//            	int[] geo_stride = new int[2];
//            	Linear2DSet newDomainSet = SwathNavigation.getNavigationDomain(
//            			0, myElements, 1,
//            			0, myLines, 1, 
//            			ratioElements, ratioLines, 0, 0, 
//            			geo_start, geo_count, geo_stride);
//            }
            
            
            
            navigationSet = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
            		lalo, myElements, myLines,
            		null, null, null,
            		false, false);
            
    	} catch (NumberFormatException exc) {
    		throw new BadDataException("Error parsing ASCII file");
    	} catch (Exception e) {
    		throw new BadDataException("Error reading ASCII file: " + url + "\n" + e);
    	}
    }
    
    /**
     * Sample points to get float[] of the appropriate length
     */
    private float[] sampleFloats(float[] dataFloats, int fromElements, int fromLines, int toElements, int toLines) {
    	if (fromElements <= toElements || fromLines <= toLines) return dataFloats;
    	float[] sampledFloats = new float[toElements * toLines];
    	
    	int skipElements = (int)Math.floor(fromElements / toElements);
    	int skipLines = (int)Math.floor(fromLines / toLines);
    	
    	int tl = 0;
    	int te = 0;
    	int dataOffset = 0;
    	int sampleOffset = 0;
    	for (int fl=0; fl<fromLines && tl<toLines; fl+=skipLines) {
    		tl++;
    		te=0;
    		for (int fe=0; fe<fromElements && te<toElements; fe+=skipElements) {
    			te++;
    			dataOffset = (fl * fromElements) + fe;
    			sampledFloats[sampleOffset++] = dataFloats[dataOffset];
    		}
    	}
    	
    	System.out.println("fromElements: " + fromElements + ", toElements: " + toElements + ", skipElements: " + skipElements);
    	System.out.println("fromLines: " + fromLines + ", toLines: " + toLines + ", skipLines: " + skipLines);
    	
    	return sampledFloats;
    }
    
    /**
     * Create dataField if it hasn't been built
     */
    private void makeDataField() {
    	System.out.println("FlatFileInfo.makeDataField()");
    	if (dataField != null) return;
    	switch (this.myFormat) {
		case HeaderInfo.kFormat1ByteUInt:
		case HeaderInfo.kFormat2ByteSInt:
		case HeaderInfo.kFormat4ByteSInt:
		case HeaderInfo.kFormat4ByteFloat:
		case HeaderInfo.kFormat8ByteDouble:
		case HeaderInfo.kFormat2x8Byte:
		case HeaderInfo.kFormat2ByteUInt:
        	makeDataFieldFromBinary();
    		break;
    	case HeaderInfo.kFormatASCII:
        	makeDataFieldFromAscii();
    		break;
    	case HeaderInfo.kFormatImage:
        	makeDataFieldFromImage();
    		break;
   		default:
   			System.err.println("Unknown data format");
    	}
    }
    
    /**
     * Create navigationSet if it hasn't been built
     */
    private void makeNavigationSet() {
    	System.out.println("FlatFileInfo.makeNavigationSet()");
    	if (navigationSet != null) return;
    	switch (this.myNavigation) {
    	case HeaderInfo.kNavigationBounds:
    		makeNavigationSetFromBounds();
    		break;
    	case HeaderInfo.kNavigationFiles:
    		makeNavigationSetFromFiles();
    		break;
   		default:
   			System.err.println("Unknown navigation format");
    	}
    }

    /**
     * Return a valid data object for a DataSource
     */
    public Data getData() {
    	System.out.println("FlatFileInfo.getData()");
    	
    	// Make the dataField if it isn't already there
    	makeDataField();
    	
    	// Make the navigationSet if it isn't already there
    	makeNavigationSet();

    	// Create and return a new Data object
    	Data d = null;
		try {
			d = GridUtil.setSpatialDomain(dataField, navigationSet);
		} catch (VisADException e) {
    		throw new BadDataException("Error creating data object:\n" + e);
		}
		return d;
    }
    
    /**
     * Return a valid data object for a DataSource
     */
    public float[] getFloats() {
    	System.out.println("FlatFileInfo.getFloats()");
    	
    	// Make the dataField if it isn't already there
    	makeDataField();
    	
    	// Create and return a new float[] object
    	float[][] f = new float[0][0];
		try {
			f = dataField.getFloats();
		} catch (VisADException e) {
			e.printStackTrace();
		}
    	return f[0];
    }
        
    /**
     * toString
     *
     * @return toString
     */
    public String toString() {
        return "url: " + url + ", lines: " + lines + ", elements: " + elements;
    }
    
    // float array -> flatfield
    private FlatField makeFlatField(float[] valueArray)
    throws IOException, VisADException {
    	RealType[]    generic 			  = new RealType[] { RealType.Generic };
    	RealTupleType genericType         = new RealTupleType(generic);

    	RealType      line              = RealType.getRealType("ImageLine");
        RealType      element           = RealType.getRealType("ImageElement");
        RealType[]    domain_components = { element, line };
        RealTupleType image_domain      = new RealTupleType(domain_components);
        Linear2DSet domain_set = new Linear2DSet(image_domain, 0.0,
        		(float) (elements - 1.0), elements,
        		(float) (lines - 1.0), 0.0, lines);
        FunctionType image_type = new FunctionType(image_domain, genericType);

        FlatField    field      = new FlatField(image_type, domain_set);

        float[][]    samples    = new float[][] { valueArray };
        try {
            field.setSamples(samples, false);
        } catch (RemoteException e) {
            throw new VisADException("Couldn't finish FlatField initialization");
        }
        
        return field;
    }

    // double array -> flatfield
    private FlatField makeFlatField(double[] valueArray)
    throws IOException, VisADException {
    	RealType[]    generic 			  = new RealType[] { RealType.Generic };
    	RealTupleType genericType         = new RealTupleType(generic);

    	RealType      line              = RealType.getRealType("ImageLine");
        RealType      element           = RealType.getRealType("ImageElement");
        RealType[]    domain_components = { element, line };
        RealTupleType image_domain      = new RealTupleType(domain_components);
        Linear2DSet domain_set = new Linear2DSet(image_domain, 0.0,
        		(float) (elements - 1.0), elements,
        		(float) (lines - 1.0), 0.0, lines);
        FunctionType image_type = new FunctionType(image_domain, genericType);

        FlatField    field      = new FlatField(image_type, domain_set);

        double[][]    samples    = new double[][] { valueArray };
        try {
            field.setSamples(samples, false);
        } catch (RemoteException e) {
            throw new VisADException("Couldn't finish FlatField initialization");
        }
        
        return field;
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