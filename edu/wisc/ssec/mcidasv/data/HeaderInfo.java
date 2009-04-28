package edu.wisc.ssec.mcidasv.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class HeaderInfo {

	// Enumerate some constants
	public static final int kFormatUnknown = -1;
	public static final int kFormatASCII = 0;
	public static final int kFormat1ByteUInt = 1;
	public static final int kFormat2ByteSInt = 2;
	public static final int kFormat4ByteSInt = 3;
	public static final int kFormat4ByteFloat = 4;
	public static final int kFormat8ByteDouble = 5;
	public static final int kFormat2x8Byte = 9;
	public static final int kFormat2ByteUInt = 12;
	public static final int kFormatImage = 8080;
	
	public static final int kNavigationUnknown = -1;
	public static final int kNavigationBounds = 1;
	public static final int kNavigationFiles = 2;
	
	public static final String kInterleaveSequential = "BSQ";
	public static final String kInterleaveByLine = "BIL";
	public static final String kInterleaveByPixel = "BIP";

	public static final String DESCRIPTION = "description";
	public static final String ELEMENTS = "elements";
	public static final String LINES = "lines";
	public static final String OFFSET = "offset";
	public static final String DATATYPE = "dataType";
	public static final String MISSINGVALUE = "missingValue";
	public static final String BANDNAMES = "bandNames";
	public static final String BANDFILES = "bandFiles";
	public static final String INTERLEAVE = "interleave";
	public static final String BYTEORDER = "byteOrder";
	public static final String BIGENDIAN = "bigEndian";
	public static final String NAVBOUNDS = "navBounds";
	public static final String NAVFILES = "navFiles";

	/** The url */
	private String headerFile = "";
	private Hashtable parameters = new Hashtable();

	/**
	 * Ctor for xml encoding
	 */
	public HeaderInfo() {}

	/**
	 * CTOR
	 *
	 * @param filename The filename
	 */
	public HeaderInfo(File thisFile) {
		this(thisFile.getAbsolutePath());
	}
	
	/**
	 * CTOR
	 *
	 * @param filename The filename
	 */
	public HeaderInfo(String filename) {
		setFilename(filename);
	}
	
	/**
	 * Set the filename we are working with
	 */
	public void setFilename(String filename) {
		parameters = new Hashtable();
		headerFile = filename;
	}
	
	/**
	 * Get the filename we are working with
	 */
	public String getFilename() {
		return headerFile;
	}
	
	/**
	 * Get the number of bands this header knows about
	 */
	public int getBandCount() {
		if (!haveParsed()) {
			parseHeader();
		}
		List bandNames = getParameter(BANDNAMES, new ArrayList());
		return bandNames.size();
	}
		
	/**
	 * Return the matching header parameter if available, default value if not available
	 */
	public String getParameter(String parameter, String defaultValue) {
		parseHeader();
		Object hashedValue = parameters.get(parameter);
		if (hashedValue == null || !(hashedValue instanceof String)) return defaultValue;
		return (String)hashedValue;
	}
	public Integer getParameter(String parameter, Integer defaultValue) {
		parseHeader();
		Object hashedValue = parameters.get(parameter);
		if (hashedValue == null || !(hashedValue instanceof Integer)) return defaultValue;
		return (Integer)hashedValue;
	}
	public Float getParameter(String parameter, Float defaultValue) {
		parseHeader();
		Object hashedValue = parameters.get(parameter);
		if (hashedValue == null || !(hashedValue instanceof Float)) return defaultValue;
		return (Float)hashedValue;
	}
	public Boolean getParameter(String parameter, Boolean defaultValue) {
		parseHeader();
		Object hashedValue = parameters.get(parameter);
		if (hashedValue == null || !(hashedValue instanceof Boolean)) return defaultValue;
		return (Boolean)hashedValue;
	}
	public List getParameter(String parameter, List defaultValue) {
		parseHeader();
		Object hashedValue = parameters.get(parameter);
		if (hashedValue == null || !(hashedValue instanceof List)) return defaultValue;
		return (List)hashedValue;
	}
	
	/**
	 * Set a parsed parameter value
	 */
	public void setParameter(String parameter, Object value) {
		parameters.put(parameter, value);
	}
	
	/**
	 * Have we already parsed once?
	 */
	public boolean haveParsed() {
		return parameters.size() > 0;
	}
	
	/**
	 * Does the file we are pointing to even exist?
	 */
	public boolean doesExist() {
		File checkFile = new File(headerFile);
		return checkFile.exists();
	}
	
	/**
	 * Override this when extending for a specific header type
	 */
	protected void parseHeader() {}
	
}
