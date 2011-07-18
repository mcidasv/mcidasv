/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2011
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

package edu.wisc.ssec.mcidasv.data.hydra;

import edu.wisc.ssec.mcidasv.data.HydraDataSource;
import edu.wisc.ssec.mcidasv.data.PreviewSelection;

import java.io.ByteArrayInputStream;
import java.io.File;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;

import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.GeoSelection;

import ucar.unidata.util.Misc;

import visad.Data;
import visad.FlatField;
import visad.VisADException;

import visad.util.Util;

/**
 * A data source for NPOESS Preparatory Project (NPP) data
 * This will probably move, but we are placing it here for now
 * since we are leveraging some existing code used for HYDRA.
 */

public class NPPDataSource extends HydraDataSource {

	private static final Logger logger = LoggerFactory.getLogger(NPPDataSource.class);
	
	/** Sources file */
    protected String filename;

    protected MultiDimensionReader nppAggReader;

    protected MultiDimensionAdapter[] adapters = null;
    
    private ArrayList<MultiSpectralData> multiSpectralData = new ArrayList<MultiSpectralData>();
    private HashMap<String, MultiSpectralData> msdMap = new HashMap<String, MultiSpectralData>();

    private static final String DATA_DESCRIPTION = "NPP Data";
    
    // instrument related variables and flags
    ucar.nc2.Attribute instrumentName = null;
    private String productName = null;
    
    // for now, we are only handling CrIS variables that match this filter and SCAN dimensions
    private String crisFilter = "ES_Real";

    private HashMap defaultSubset;
    public TrackAdapter track_adapter;

    private List categories;
    private boolean hasChannelSelect = false;
    private boolean hasImagePreview = true;
    private boolean isCombinedProduct = false;

    private PreviewSelection previewSelection = null;
    private FlatField previewImage = null;
    
    private static int[] YSCAN_POSSIBILITIES = { 96,  512,  768,  1536, 2304, 2313, 12, 4,   4,   4   };
    private static int[] XSCAN_POSSIBILITIES = { 508, 2133, 3200, 6400, 4064, 4121, 96, 30,  30,  30  }; 
    private static int[] ZSCAN_POSSIBILITIES = { -1,  -1,   -1,   -1,   -1,   -1,   22, 163, 437, 717 };    
    private int inTrackDimensionLength = -1;
    
    // need our own separator char since it's always Unix-style in the NPP files
    private static final String SEPARATOR_CHAR = "/";
    
    // date formatter for converting NPP day/time to something we can use
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss.SSS");

    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    
    public NPPDataSource() {}

    /**
     * Construct a new NPP hdf data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  fileName  name of the hdf file to read
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    
    public NPPDataSource(DataSourceDescriptor descriptor,
                                 String fileName, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.newList(fileName), properties);
        logger.debug("NPPDataSource called, single file selected: " + fileName);
    }

    /**
     * Construct a new NPP hdf data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  sources   List of filenames
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    
    public NPPDataSource(DataSourceDescriptor descriptor,
                                 List<String> newSources, Hashtable properties)
            throws VisADException {
        super(descriptor, newSources, DATA_DESCRIPTION, properties);
        logger.debug("NPPDataSource constructor called, file count: " + sources.size());

        this.filename = (String) sources.get(0);
        
        this.setName("NPP");
        this.setDescription("NPP");
        
        for (Object o : sources) {
        	logger.debug("NPP source file: " + (String) o);
        }

        setup();
    }

    public void setup() throws VisADException {

    	// looking to populate 3 things - path to lat, path to lon, path to relevant products
    	String pathToLat = null;
    	String pathToLon = null;
    	TreeSet<String> pathToProducts = new TreeSet<String>();
    	
    	// flag to indicate data is 3-dimensions (X, Y, channel or band)
    	boolean is3D = false;
    	
    	// check source filenames to see if this is a combined product
    	// XXX TJJ - looking for "underscore" is NOT GUARANTEED TO WORK! FIXME 
    	String prodStr = filename.substring(
    			filename.lastIndexOf(File.separatorChar) + 1, 
    			filename.lastIndexOf(File.separatorChar) + 1 + filename.indexOf("_"));
        StringTokenizer st = new StringTokenizer(prodStr, "-");
        logger.debug("check for embedded GEO, tokenizing: " + prodStr);
        while (st.hasMoreTokens()) {
        	String singleProd = st.nextToken();
        	logger.debug("Next token: " + singleProd);
        	for (int i = 0; i < JPSSUtilities.geoProductIDs.length; i++) {
        		if (singleProd.equals(JPSSUtilities.geoProductIDs[i])) {
        			logger.debug("Setting isCombinedProduct true, Found embedded GEO: " + singleProd);
        			isCombinedProduct = true;
        			break;
        		}
        	}
        }
    	
    	// various metatdata we'll need to gather on a per-product basis
    	ArrayList<String> unsignedFlags = new ArrayList<String>();
    	ArrayList<String> unpackFlags = new ArrayList<String>();
    	
    	// time for each product in milliseconds since epoch
    	ArrayList<Long> productTimes = new ArrayList<Long>();
    	
    	// geo product IDs for each granule
    	ArrayList<String> geoProductIDs = new ArrayList<String>();
    	
    	// aggregations will use sets of NetCDFFile readers
    	ArrayList<NetCDFFile> ncdfal = new ArrayList<NetCDFFile>();
    	
    	// we should be able to find an XML Product Profile for each data/product type
    	NPPProductProfile nppPP = null;
    	    	
    	sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    	   	
    	try {
    		
    		nppPP = new NPPProductProfile();
    		
    		// for each source file provided get the nominal time
    		for (int fileCount = 0; fileCount < sources.size(); fileCount++) {
	    		// need to open the main NetCDF file to determine the geolocation product
	    		NetcdfFile ncfile = null;
	    		String fileAbsPath = null;
	    		try {
	    			fileAbsPath = (String) sources.get(fileCount);
		    		logger.debug("Trying to open file: " + fileAbsPath);
		    		ncfile = NetcdfFile.open(fileAbsPath);
		    		if (! isCombinedProduct) {
						ucar.nc2.Attribute a = ncfile
								.findGlobalAttribute("N_GEO_Ref");
						logger.debug("Value of GEO global attribute: "
								+ a.getStringValue());
						String tmpGeoProductID = null;
						if (a.getStringValue().endsWith("h5")) {
							tmpGeoProductID = a.getStringValue();
						} else {
							tmpGeoProductID = JPSSUtilities
									.mapGeoRefToProductID(a.getStringValue());
						}
						logger.debug("Value of corresponding Product ID: "
								+ tmpGeoProductID);
						geoProductIDs.add(tmpGeoProductID);
					}
					Group rg = ncfile.getRootGroup();

	    	    	logger.debug("Root group name: " + rg.getName());
	    	    	List<Group> gl = rg.getGroups();
	    	    	if (gl != null) {
	    	    		for (Group g : gl) {
	    	    			logger.debug("Group name: " + g.getName());
	    	    			// when we find the Data_Products group, go down another group level and pull out 
	    	    			// what we will use for nominal day and time (for now anyway).
	    	    			// XXX TJJ fileCount check is so we don't count the GEO file in time array!
	    	    			if (g.getName().contains("Data_Products") && (fileCount != sources.size())) {
	    	    				boolean foundDateTime = false;
	    	    				List<Group> dpg = g.getGroups();
	    	    				
	    	    				// cycle through once looking for XML Product Profiles
	    	    				for (Group subG : dpg) {
	    	    					
	    	    					// determine the instrument name (VIIRS, ATMS, CrIS)
	    	    					instrumentName = subG.findAttribute("Instrument_Short_Name");
	    	    					
	    	    					// This is also where we find the attribute which tells us which
	    	    					// XML Product Profile to use!
    	    						ucar.nc2.Attribute axpp = subG.findAttribute("N_Collection_Short_Name");
    	    						if (axpp != null) {
    	    							System.err.println("XML Product Profile N_Collection_Short_Name: " + axpp.getStringValue());
    	    							String baseName = axpp.getStringValue();
    	    							productName = baseName;
    	    							String productProfileFileName = nppPP.getProfileFileName(baseName);
    	    							logger.debug("Found profile: " + productProfileFileName);
    	    							if (productProfileFileName == null) {
    	    								throw new Exception("XML Product Profile not found in catalog");
    	    							}
    	    							try {
    	    								nppPP.addMetaDataFromFile(productProfileFileName);
    	    							} catch (Exception nppppe) {
    	    								logger.error("Error parsing XML Product Profile: " + productProfileFileName);
    	    								throw new Exception("XML Product Profile Error");
    	    							}
    	    						}
	    	    				}
	    	    				
	    	    				// 2nd pass through sub-group to extract date/time for aggregation
	    	    				for (Group subG : dpg) {
	    	    					List<Variable> vl = subG.getVariables();
	    	    					for (Variable v : vl) {
	    	    						ucar.nc2.Attribute aDate = v.findAttribute("AggregateBeginningDate");
	    	    						ucar.nc2.Attribute aTime = v.findAttribute("AggregateBeginningTime");
	    	    						// did we find the attributes we are looking for?
	    	    						if ((aDate != null) && (aTime != null)) {
	    	    							String sDate = aDate.getStringValue();
	    	    							String sTime = aTime.getStringValue();
	    	    							logger.debug("For day/time, using: " + sDate + sTime.substring(0, sTime.indexOf('Z') - 3));
	    	    							Date d = sdf.parse(sDate + sTime.substring(0, sTime.indexOf('Z') - 3));
	    	    							productTimes.add(new Long(d.getTime()));
	    	    							logger.debug("ms since epoch: " + d.getTime());
	    	    							foundDateTime = true;
	    	    							break;
	    	    						}
	    	    					}
	    	    					if (foundDateTime) break;
	    	    				}
	    	    				if (! foundDateTime) {
	    	    					throw new VisADException("No date time found in NPP granule");
	    	    				}
	    	    			}	    	    			
	    	    		}
	    	    	}
	    		} catch (Exception e) {
	    			logger.debug("Exception during processing of file: " + fileAbsPath);
	    			throw (e);
	    		} finally {
	    			ncfile.close();
	    		}
    		}
    		
    		for (Long l : productTimes) {
    			logger.debug("Product time: " + l);
    		}
    		
    		// build each union aggregation element
    		for (int elementNum = 0; elementNum < sources.size(); elementNum++) {
    			String s = (String) sources.get(elementNum);
    			
    			// build an XML (NCML actually) representation of the union aggregation of these two files
    			Namespace ns = Namespace.getNamespace("http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2");
    			org.jdom.Element root = new org.jdom.Element("netcdf", ns);
    			org.jdom.Document document = new org.jdom.Document(root);

    			org.jdom.Element agg = new org.jdom.Element("aggregation", ns);
    			agg.setAttribute("type", "union");
        		
    			org.jdom.Element fData = new org.jdom.Element("netcdf", ns);
    			fData.setAttribute("location", s);
    			agg.addContent(fData);
    			
    			if (! isCombinedProduct) {
	    			org.jdom.Element fGeo  = new org.jdom.Element("netcdf", ns);
	
	    			String geoFilename = s.substring(0, s.lastIndexOf(File.separatorChar) + 1);
	    			// check if we have the whole file name or just the prefix
	    			String geoProductID = geoProductIDs.get(elementNum);
	    			if (geoProductID.endsWith("h5")) {
	    				geoFilename += geoProductID;
	    			} else {
	    				geoFilename += geoProductID;
	    				geoFilename += s.substring(s.lastIndexOf(File.separatorChar) + 6);
	    			}
	    			logger.debug("Cobbled together GEO file name: " + geoFilename);
	    			fGeo.setAttribute("location", geoFilename);
	    			agg.addContent(fGeo);
    			}

    			root.addContent(agg);    
    		    XMLOutputter xmlOut = new XMLOutputter();
    		    String ncmlStr = xmlOut.outputString(document);
    		    ByteArrayInputStream is = new ByteArrayInputStream(ncmlStr.getBytes());			
    		    MultiDimensionReader netCDFReader = new NetCDFFile(is);
    		    
    	    	// let's try and look through the NetCDF reader and see what we can learn...
    	    	NetcdfFile ncdff = ((NetCDFFile) netCDFReader).getNetCDFFile();
    	    	
    	    	Group rg = ncdff.getRootGroup();

    	    	List<Group> gl = rg.getGroups();
    	    	if (gl != null) {
    	    		for (Group g : gl) {
    	    			logger.debug("Group name: " + g.getName());
    	    			// XXX just temporary - we are looking through All_Data, finding displayable data
    	    			if (g.getName().contains("All_Data")) {
    	    				List<Group> adg = g.getGroups();
    	    				// again, iterate through
    	    				for (Group subG : adg) {
    	    					logger.debug("Sub group name: " + subG.getName());
    	    					String subName = subG.getName();
    	    					if (subName.contains("-GEO")) {
    	    						// this is the geolocation data
    	    						List<Variable> vl = subG.getVariables();
    	    						for (Variable v : vl) {
    	    							if (v.getName().contains("Latitude")) {
    	    								pathToLat = v.getName();
    	        							logger.debug("Lat/Lon Variable: " + v.getName());
    	    							}
    	    							if (v.getName().contains("Longitude")) {
    	    								pathToLon = v.getName();
    	        							logger.debug("Lat/Lon Variable: " + v.getName());
    	    							}
    	    						} 
    	    					} else {
    	    						// this is the product data
    	    						List<Variable> vl = subG.getVariables();
    	    						for (Variable v : vl) {
    	    							boolean useThis = false;
    	    							String vName = v.getName();
    	    							logger.debug("Variable: " + vName);
    	    							String varShortName = vName.substring(vName.lastIndexOf(SEPARATOR_CHAR) + 1);
    	    							
    	    							// skip Quality Flags for now.
    	    							// XXX TJJ - should we show these?  if so, note they sometimes
    	    							// have different dimensions than the main variables.  For ex,
    	    							// on high res bands QFs are 768 x 3200 while vars are 1536 x 6400
    	    							if (varShortName.startsWith("QF")) {
    	    								continue;
    	    							}
    	    							
    	    							// for CrIS instrument, only taking real calibrated values for now
    	    							logger.debug("INSTRUMENT NAME: " + instrumentName);
    	    							if (instrumentName.getStringValue().equals("CrIS")) {
    	    								if (! varShortName.startsWith(crisFilter)) {
    	    									logger.debug("Skipping variable: " + varShortName);
    	    									continue;
    	    								}
    	    							}
    	    							
    	    							logger.debug("Variable prefix for finding Factors: " + varShortName);
    	    							DataType dt = v.getDataType();
    	    							if ((dt.getSize() != 4) && (dt.getSize() != 2) && (dt.getSize() != 1)) {
    	    								logger.debug("Skipping data of size: " + dt.getSize());
    	    								continue;
    	    							}
    	    							List al = v.getAttributes();

    	    							List<Dimension> dl = v.getDimensions();
    	    							if (dl.size() > 4) {
    	    								logger.debug("Skipping data of dimension: " + dl.size());
    	    								continue;
    	    							}
    	    							
    	    							// for now, skip any 3D VIIRS data
    	    							if (instrumentName.getStringValue().equals("VIIRS")) {
    	    								if (dl.size() == 3) {
    	    									logger.debug("Skipping VIIRS 3D data for now...");
    	    									continue;
    	    								}
    	    							}
    	    							
    	    							boolean xScanOk = false;
    	    							boolean yScanOk = false;
    	    							boolean zScanOk = false;
    	    							for (Dimension d : dl) {
    	    								// in order to consider this a displayable product, make sure
    	    								// both scan direction dimensions are present and look like a granule
    	    								for (int xIdx = 0; xIdx < XSCAN_POSSIBILITIES.length; xIdx++) {
    	    									if (d.getLength() == XSCAN_POSSIBILITIES[xIdx]) {
    	    										xScanOk = true;
    	    										break;
    	    									}
    	    								}
    	    								for (int yIdx = 0; yIdx < YSCAN_POSSIBILITIES.length; yIdx++) {
    	    									if (d.getLength() == YSCAN_POSSIBILITIES[yIdx]) {
    	    										yScanOk = true;
    	    										inTrackDimensionLength = YSCAN_POSSIBILITIES[yIdx];
    	    										break;
    	    									}
    	    								}   
    	    								for (int zIdx = 0; zIdx < ZSCAN_POSSIBILITIES.length; zIdx++) {
    	    									if (d.getLength() == ZSCAN_POSSIBILITIES[zIdx]) {
    	    										zScanOk = true;
    	    										break;
    	    									}
    	    								}
    	    							}
    	    							
    	    							if (xScanOk && yScanOk) {
    	    								useThis = true;
    	    							}
    	    							
    	    							if (zScanOk) {
    	    								is3D = true;
    	    								hasChannelSelect = true;
    	    								logger.info("Handling 3D data source!");
    	    							}
    	    							
    	    							if (useThis) { 
    	    								// loop through the variable list again, looking for a corresponding "Factors"
    	    								float scaleVal = 1f;
    	    								float offsetVal = 0f;
    	    								boolean unpackFlag = false;
    	    								
    	    								// XXX TJJ - this is NOT DETERMINISTIC!  The spec in
    	    								// CDFCB-X, Vol 5, page 8, is too vague, and there is
    	    								// no SURE way to map variable name to scale/offset parameter
    	    								//
    	    								//   if static map has an entry for this variable name
    	    								//     get the data, data1 = scale, data2 = offset
    	    								//     create and poke attributes with this data
    	    								//   endif
    	    								
    	    								String factorsVarName = JPSSUtilities.mapDataVarNameToFactorsName(varShortName);
    	    								logger.info("Mapping: " + varShortName + " to: " + factorsVarName);
    	    								if (factorsVarName != null) {
	    	    								for (Variable fV : vl) {
	    	    									if (fV.getName().endsWith(factorsVarName)) {
	    	    										logger.debug("Pulling scale and offset values from variable: " + fV.getName());
	    	    										ucar.ma2.Array a = fV.read();
	    	    										ucar.ma2.Index i = a.getIndex();
	    	    										scaleVal = a.getFloat(i);
	    	    										logger.debug("Scale value: " + scaleVal);
	    	    										i.incr();
	    	    										offsetVal = a.getFloat(i);
	    	    										logger.debug("Offset value: " + offsetVal);
	    	    										unpackFlag = true;
	    	    										break;
	    	    									}
	    	    								}
    	    								}

    	    								// poke in scale/offset attributes for now

    	    								ucar.nc2.Attribute a1 = new ucar.nc2.Attribute("scale_factor", scaleVal);
    	    								v.addAttribute(a1);
    	    								ucar.nc2.Attribute a2 = new ucar.nc2.Attribute("add_offset", offsetVal);
    	    								v.addAttribute(a2);  
    	    								
    	    								// add valid range and fill value attributes here
    	    					        	// try to fill in valid range
    	    					        	if (nppPP != null) {
    	    					        		String translatedName = JPSSUtilities.mapProdNameToProfileName(vName.substring(vName.lastIndexOf(SEPARATOR_CHAR) + 1));
    	    					        		logger.debug("mapped name: " + translatedName);
    	    					        		if (translatedName != null) {
    	    					        			String rangeMin = nppPP.getRangeMin(translatedName);
    	    					        			String rangeMax = nppPP.getRangeMax(translatedName);
    	    					        			logger.debug("range min: " + rangeMin);
    	    					        			logger.debug("range max: " + rangeMax);
    	    					        			int [] shapeArr = new int [] { 2 };
    	    					        			ArrayFloat af = new ArrayFloat(shapeArr);
    	    					        			try {
        	    					        			af.setFloat(0, Float.parseFloat(rangeMin));
    	    					        			} catch (NumberFormatException nfe) {
    	    					        				af.setFloat(0, new Float(Integer.MIN_VALUE));
    	    					        			}
    	    					        			try {
    	    					        				af.setFloat(1, Float.parseFloat(rangeMax));
    	    					        			} catch (NumberFormatException nfe) {
    	    					        				af.setFloat(1, new Float(Integer.MAX_VALUE));
    	    					        			}
    	    	    								ucar.nc2.Attribute rangeAtt = new ucar.nc2.Attribute("valid_range", af);
    	    	    								v.addAttribute(rangeAtt);
    	    	    								
    	    	    								// check for and load fill values too...
    	    	    								
    	    	    								// we need to check two places, first, the XML product profile
    	    	    								ArrayList<Float> fval = nppPP.getFillValues(translatedName);
    	    	    								
    	    	    								// 2nd, does the variable already have one defined?
	    	    									// if there was already a fill value associated with this variable, make
	    	    									// sure we bring that along for the ride too...
	        	    								ucar.nc2.Attribute aFill = v.findAttribute("_FillValue");
	        	    								
	        	    								// determine size of our fill value array
	        	    								int fvArraySize = 0;
	        	    								if (aFill != null) fvArraySize++;
	        	    								if (! fval.isEmpty()) fvArraySize += fval.size();
	        	    								int [] fillShape = new int [] { fvArraySize };
	        	    								
	        	    								// allocate the array
	        	    								ArrayFloat afFill = new ArrayFloat(fillShape);
	        	    								
    	    	    								// and FINALLY, fill it!
	        	    								if (! fval.isEmpty()) {
    	    	    									for (int fillIdx = 0; fillIdx < fval.size(); fillIdx++) {
    	    	    										afFill.setFloat(fillIdx, fval.get(fillIdx));
    	    	    										logger.info("Adding fill value (from XML): " + fval.get(fillIdx));
    	    	    									}
    	    	    								}
	        	    								
	        	    								if (aFill != null) {
	        	    									Number n = aFill.getNumericValue();
	        	    									// is the data unsigned?
	        	    									ucar.nc2.Attribute aUnsigned = v.findAttribute("_Unsigned");
	        	    									float fillValAsFloat = Float.NaN;
	        	    									if (aUnsigned != null) {
	        	    										if (aUnsigned.getStringValue().equals("true")) {
	        	    											DataType fvdt = aFill.getDataType();
	        	    											logger.info("Data String: " + aFill.toString());
	        	    											logger.info("DataType primitive type: " + fvdt.getPrimitiveClassType());
	        	    											// signed byte that needs conversion?
	        	    											if (fvdt.getPrimitiveClassType() == byte.class) {
	        	    												fillValAsFloat = (float) Util.unsignedByteToInt(n.byteValue());
	        	    											}
	        	    											else if (fvdt.getPrimitiveClassType() == short.class) {
	        	    												fillValAsFloat = (float) Util.unsignedShortToInt(n.shortValue());
	        	    											} else {
	        	    												fillValAsFloat = n.floatValue();
	        	    											}
	        	    										}
	        	    									}
	        	    									afFill.setFloat(fvArraySize - 1, fillValAsFloat);
	        	    									logger.info("Adding fill value (from variable): " + fillValAsFloat);
	        	    								}
	    	    									ucar.nc2.Attribute fillAtt = new ucar.nc2.Attribute("_FillValue", afFill);
	    	    									v.addAttribute(fillAtt);
    	    					        		}
    	    					        	}

    	    								ucar.nc2.Attribute aUnsigned = v.findAttribute("_Unsigned");
    	    								if (aUnsigned != null) {
    	    									logger.debug("_Unsigned attribute value: " + aUnsigned.getStringValue());
    	    									unsignedFlags.add(aUnsigned.getStringValue());
    	    								} else {
    	    									unsignedFlags.add("false");
    	    								}
    	    								
    	    								if (unpackFlag) {
    	    									unpackFlags.add("true");
    	    								} else {
    	    									unpackFlags.add("false");
    	    								}
    	    								
    	    								logger.debug("Adding product: " + v.getName());
    	    								pathToProducts.add(v.getName());
    	    								
    	    							}
    	    						}    						
    	   						
    	    					}
    	    				}
    	    			}
    	    		}
    	    	}
    	    	
    		    ncdfal.add((NetCDFFile) netCDFReader);
    		}
    		
    	} catch (Exception e) {
    		logger.error("cannot create NetCDF reader for files selected");
    		if (e.getMessage() != null && e.getMessage().equals("XML Product Profile Error")) {
    			throw new VisADException("Unable to extract metadata from required XML Product Profile");
    		}
    	}
    	
    	// initialize the aggregation reader object
    	try {
    		nppAggReader = new GranuleAggregation(ncdfal, inTrackDimensionLength, "Track", "XTrack");
    	} catch (Exception e) {
    		throw new VisADException("Unable to initialize aggregation reader");
    	}

    	// make sure we found valid data
    	if (pathToProducts.size() == 0) {
    		throw new VisADException("No data found in files selected");
    	}
    	
    	logger.debug("Number of adapters needed: " + pathToProducts.size());
    	adapters = new MultiDimensionAdapter[pathToProducts.size()];
    	Hashtable<String, String[]> properties = new Hashtable<String, String[]>(); 
    	
    	Iterator<String> iterator = pathToProducts.iterator();
    	int pIdx = 0;
    	while (iterator.hasNext()) {
    		String pStr = (String) iterator.next();
    		logger.debug("Working on adapter number " + (pIdx + 1));
        	HashMap<String, Object> swathTable = SwathAdapter.getEmptyMetadataTable();
        	HashMap<String, Object> spectTable = SpectrumAdapter.getEmptyMetadataTable();
        	swathTable.put("array_name", pStr);
        	swathTable.put("lon_array_name", pathToLon);
        	swathTable.put("lat_array_name", pathToLat);
        	swathTable.put("XTrack", "XTrack");
        	swathTable.put("Track", "Track");
        	swathTable.put("geo_Track", "Track");
        	swathTable.put("geo_XTrack", "XTrack");
        	swathTable.put("product_name", productName);
        	
        	// array_name common to spectrum table
        	spectTable.put("array_name", pStr);
        	spectTable.put("product_name", productName);
        	logger.debug("Product Name: " + productName);
        	
        	if (is3D) {

        		// 3D data is either ATMS or CrIS
        		if ((instrumentName.getName() != null) && (instrumentName.getStringValue().equals("ATMS"))) {
            		//hasChannelSelect = true;
        			spectTable.put(SpectrumAdapter.channelIndex_name, "Channel");
            		swathTable.put(SpectrumAdapter.channelIndex_name, "Channel");
            		
            		swathTable.put("array_dimension_names", new String[] {"Track", "XTrack", "Channel"});
            		swathTable.put("lon_array_dimension_names", new String[] {"Track", "XTrack"});
            		swathTable.put("lat_array_dimension_names", new String[] {"Track", "XTrack"});
            		spectTable.put("array_dimension_names", new String[] {"Track", "XTrack", "Channel"});
            		spectTable.put("lon_array_dimension_names", new String[] {"Track", "XTrack"});
            		spectTable.put("lat_array_dimension_names", new String[] {"Track", "XTrack"});
            		
            		spectTable.put(SpectrumAdapter.channelType, "wavelength");
            		spectTable.put(SpectrumAdapter.channels_name, "Channel");
                    spectTable.put(SpectrumAdapter.x_dim_name, "XTrack");
                    spectTable.put(SpectrumAdapter.y_dim_name, "Track");
                    
        			int numChannels = JPSSUtilities.ATMSChannelCenterFrequencies.length;
            		float[] bandArray = new float[numChannels];
            		String[] bandNames = new String[numChannels];
            		for (int bIdx = 0; bIdx < numChannels; bIdx++) {
            			bandArray[bIdx] = JPSSUtilities.ATMSChannelCenterFrequencies[bIdx];
            			bandNames[bIdx] = "Channel " + (bIdx + 1);
            		}
            		spectTable.put(SpectrumAdapter.channelValues, bandArray);
            		spectTable.put(SpectrumAdapter.bandNames, bandNames);

        		} else {
        			if (instrumentName.getStringValue().equals("CrIS")) {
        				
        				swathTable.put("XTrack", "dim1");
        				swathTable.put("Track", "dim0");
        				swathTable.put("geo_XTrack", "dim1");
        				swathTable.put("geo_Track", "dim0");
        				swathTable.put("product_name", "CrIS_SDR");
        				swathTable.put(SpectrumAdapter.channelIndex_name, "dim3");
        				swathTable.put(SpectrumAdapter.FOVindex_name, "dim2"); 
        				 
        				spectTable.put(SpectrumAdapter.channelIndex_name, "dim3");
        				spectTable.put(SpectrumAdapter.FOVindex_name, "dim2");
                		spectTable.put(SpectrumAdapter.x_dim_name, "dim1");
                		spectTable.put(SpectrumAdapter.y_dim_name, "dim0");
                		
        			} else {
        				// sorry, if we can't id the instrument, we can't display the data!
        				throw new VisADException("Unable to determine instrument name");
        			}
        		}

        	} else {
        		swathTable.put("array_dimension_names", new String[] {"Track", "XTrack"});
        		swathTable.put("lon_array_dimension_names", new String[] {"Track", "XTrack"});
        		swathTable.put("lat_array_dimension_names", new String[] {"Track", "XTrack"});
        	}
        	
        	swathTable.put("scale_name", "scale_factor");
        	swathTable.put("offset_name", "add_offset");
        	swathTable.put("fill_value_name", "_FillValue");
        	logger.info("Setting range_name to: " + pStr.substring(pStr.indexOf(SEPARATOR_CHAR) + 1));
        	swathTable.put("range_name", pStr.substring(pStr.indexOf(SEPARATOR_CHAR) + 1));
        	spectTable.put("range_name", pStr.substring(pStr.indexOf(SEPARATOR_CHAR) + 1));
        	
        	// set the valid range hash if data is available
        	if (nppPP != null) {
        		String translatedName = JPSSUtilities.mapProdNameToProfileName(pStr.substring(pStr.lastIndexOf(SEPARATOR_CHAR) + 1));
        		if (translatedName != null) {
        			swathTable.put("valid_range", "valid_range");
        		}
        	}
        	
        	String unsignedAttributeStr = unsignedFlags.get(pIdx);
        	if (unsignedAttributeStr.equals("true")) {
        		swathTable.put("unsigned", unsignedAttributeStr);
        	}
        	
        	String unpackFlagStr = unpackFlags.get(pIdx);
        	if (unpackFlagStr.equals("true")) {
        		swathTable.put("unpack", "true");
        	}
        	
        	// For NPP data, do valid range check AFTER applying scale/offset
        	swathTable.put("range_check_after_scaling", "true");
        	
        	// pass in a GranuleAggregation reader...
        	if (is3D) {
                if (instrumentName.getStringValue().equals("ATMS")) {
            		adapters[pIdx] = new SwathAdapter(nppAggReader, swathTable);
            		SpectrumAdapter sa = new SpectrumAdapter(nppAggReader, spectTable);
                    DataCategory.createCategory("MultiSpectral");
                    categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
                	MultiSpectralData msd = new MultiSpectralData((SwathAdapter) adapters[pIdx], sa, 
                		"BrightnessTemperature", "BrightnessTemperature", "NPP", "ATMS");
                	msd.setInitialWavenumber(JPSSUtilities.ATMSChannelCenterFrequencies[0]);
                	multiSpectralData.add(msd);
                } else {
            		adapters[pIdx] = new CrIS_SDR_SwathAdapter(nppAggReader, swathTable);
            		CrIS_SDR_Spectrum csa = new CrIS_SDR_Spectrum(nppAggReader, spectTable);
                    DataCategory.createCategory("MultiSpectral");
                    categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
                	MultiSpectralData msd = new MultiSpectralData((CrIS_SDR_SwathAdapter) adapters[pIdx], 
                			csa);
                    msd.setInitialWavenumber(csa.getInitialWavenumber());
                    multiSpectralData.add(msd);
                }
                if (pIdx == 0) {
                	defaultSubset = multiSpectralData.get(pIdx).getDefaultSubset();
                	try {
                		previewImage = multiSpectralData.get(pIdx).getImage(defaultSubset);
                	} catch (Exception e) {
                		e.printStackTrace();
                	}
                }
                
        	} else {
        		adapters[pIdx] = new SwathAdapter(nppAggReader, swathTable);
        		if (pIdx == 0) {
        			defaultSubset = adapters[pIdx].getDefaultSubset();
        		}
        		categories = DataCategory.parseCategories("IMAGE");
        	}
    		pIdx++;
    	}

    	setProperties(properties);
    }

    public void initAfterUnpersistence() {
    	try {
    		setup();
    	} catch (Exception e) {
    	}
    }

    /**
     * Make and insert the <code>DataChoice</code>-s for this
     * <code>DataSource</code>.
     */
    
    public void doMakeDataChoices() {
    	
    	// special loop for CrIS and ATMS data
    	if (multiSpectralData.size() > 0) {
    		for (int k=0; k<multiSpectralData.size(); k++) {
    			MultiSpectralData adapter = multiSpectralData.get(k);
    			DataChoice choice = null;
				try {
					choice = doMakeDataChoice(k, adapter);
	    			msdMap.put(choice.getName(), adapter);
	    			addDataChoice(choice);
				} catch (Exception e) {
					e.printStackTrace();
				}
    		}
    		return;
    	}
    	// all other data (VIIRS and 2D EDRs)
    	if (adapters != null) {
    		for (int idx = 0; idx < adapters.length; idx++) {
    			DataChoice choice = null;
    			try {
    				choice = doMakeDataChoice(idx, adapters[idx].getArrayName());
    			} 
    			catch (Exception e) {
    				e.printStackTrace();
    				logger.error("doMakeDataChoice failed");
    			}

    			if (choice != null) {
    				addDataChoice(choice);
    			}
    		}
    	}
    }

    private DataChoice doMakeDataChoice(int idx, String var) throws Exception {
        String name = var;
        DataSelection dataSel = new MultiDimensionSubset(defaultSubset);
        Hashtable subset = new Hashtable();
        subset.put(new MultiDimensionSubset(), dataSel);
        DirectDataChoice ddc = new DirectDataChoice(this, idx, name, name, categories, subset);
        return ddc;
    }
    
    private DataChoice doMakeDataChoice(int idx, MultiSpectralData adapter) throws Exception {
        String name = adapter.getName();
        DataSelection dataSel = new MultiDimensionSubset(defaultSubset);
        Hashtable subset = new Hashtable();
        subset.put(MultiDimensionSubset.key, dataSel);
        subset.put(MultiSpectralDataSource.paramKey, adapter.getParameter());
        DirectDataChoice ddc = new DirectDataChoice(this, new Integer(idx), name, name, categories, subset);
        ddc.setProperties(subset);
        return ddc;
    }

    /**
     * Check to see if this <code>NPPDataSource</code> is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    
    public boolean equals(Object o) {
        if ( !(o instanceof NPPDataSource)) {
            return false;
        }
        return (this == (NPPDataSource) o);
    }

    public MultiSpectralData getMultiSpectralData() {
    	return multiSpectralData.get(0);
    }
    
    public MultiSpectralData getMultiSpectralData(DataChoice choice) {
    	return msdMap.get(choice.getName());
    }

    public String getDatasetName() {
      return filename;
    }
    
    public void setDatasetName(String name) {
      filename = name;
    }

    public HashMap getSubsetFromLonLatRect(MultiDimensionSubset select, GeoSelection geoSelection) {
      GeoLocationInfo ginfo = geoSelection.getBoundingBox();
      return adapters[0].getSubsetFromLonLatRect(select.getSubset(), ginfo.getMinLat(), ginfo.getMaxLat(),
                                        ginfo.getMinLon(), ginfo.getMaxLon());
    }

    public synchronized Data getData(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {
       return this.getDataInner(dataChoice, category, dataSelection, requestProperties);
    }


    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {

        //- this hack keeps the HydraImageProbe from doing a getData()
        //- TODO: need to use categories?
        if (requestProperties != null) {
          if ((requestProperties.toString()).equals("{prop.requester=MultiSpectral}")) {
            return null;
          }
        }

        GeoLocationInfo ginfo = null;
        GeoSelection geoSelection = null;
        
        if ((dataSelection != null) && (dataSelection.getGeoSelection() != null)) {
          geoSelection = (dataSelection.getGeoSelection().getBoundingBox() != null) ? dataSelection.getGeoSelection() :
                                    dataChoice.getDataSelection().getGeoSelection();
        }

        if (geoSelection != null) {
          ginfo = geoSelection.getBoundingBox();
        }

        Data data = null;
        if (adapters == null) {
          return data;
        }

        MultiDimensionAdapter adapter = null;
        
        // pick the adapter with the same index as the current data choice
        int aIdx = 0;
        List<DataChoice> dcl = getDataChoices();
        for (DataChoice dc : dcl) {
        	if (dc.equals(dataChoice)) {
        		aIdx = dcl.indexOf(dc);
        		break;
        	}
        }
        
        logger.debug("Found dataChoice index: " + aIdx);
        adapter = adapters[aIdx];

        try {
            HashMap subset = null;
            if (ginfo != null) {
            	logger.debug("getting subset from lat-lon rect...");
              subset = adapter.getSubsetFromLonLatRect(ginfo.getMinLat(), ginfo.getMaxLat(),
                                                       ginfo.getMinLon(), ginfo.getMaxLon(),
                                                       geoSelection.getXStride(),
                                                       geoSelection.getYStride(),
                                                       geoSelection.getZStride());
            }
            else {

              MultiDimensionSubset select = null;
              Hashtable table = dataChoice.getProperties();
              Enumeration keys = table.keys();
              while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                logger.debug("Key: " + key.toString());
                if (key instanceof MultiDimensionSubset) {
                  select = (MultiDimensionSubset) table.get(key);
                }
              }  
              subset = select.getSubset();
              logger.debug("Subset size: " + subset.size());

              if (dataSelection != null) {
                Hashtable props = dataSelection.getProperties();
                if (props != null) {
                  if (props.containsKey(SpectrumAdapter.channelIndex_name)) {
                	  logger.debug("Props contains channel index key...");
                    double[] coords = (double[]) subset.get(SpectrumAdapter.channelIndex_name);
                    int idx = ((Integer) props.get(SpectrumAdapter.channelIndex_name)).intValue();
                    coords[0] = (double)idx;
                    coords[1] = (double)idx;
                    coords[2] = (double)1;
                  }
                }
              }
            }

            if (subset != null) {
              data = adapter.getData(subset);
              data = applyProperties(data, requestProperties, subset, aIdx);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("getData exception e=" + e);
        }
        return data;
    }

    protected Data applyProperties(Data data, Hashtable requestProperties, HashMap subset, int adapterIndex) 
          throws VisADException, RemoteException {
      Data new_data = data;

      if (requestProperties == null) {
        new_data = data;
        return new_data;
      }

      return new_data;
    }

    protected void initDataSelectionComponents(
         List<DataSelectionComponent> components,
             final DataChoice dataChoice) {
      
      if (System.getProperty("os.name").equals("Mac OS X") && hasImagePreview && hasChannelSelect) {
          try {
            components.add(new ImageChannelSelection(new PreviewSelection(dataChoice, previewImage, null), new ChannelSelection(dataChoice)));
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      else {
    	  if (hasImagePreview) {
    		  try {
    			  FlatField image = (FlatField) dataChoice.getData(null);
    			  components.add(new PreviewSelection(dataChoice, image, null));
    		  } catch (Exception e) {
    			  logger.error("Can't make PreviewSelection: "+e);
    			  e.printStackTrace();
    		  }
    	  }
    	  if (hasChannelSelect) {
    		  try {
    			  components.add(new ChannelSelection(dataChoice));
    		  } 
    		  catch (Exception e) {
    			  e.printStackTrace();
    		  }
    	  }
      }
      
    }
    
}
