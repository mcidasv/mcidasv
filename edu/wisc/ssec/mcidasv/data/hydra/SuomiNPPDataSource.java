/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.mcidasv.data.hydra;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.PersistenceManager;
import edu.wisc.ssec.mcidasv.data.HydraDataSource;
import edu.wisc.ssec.mcidasv.data.PreviewSelection;
import edu.wisc.ssec.mcidasv.data.QualityFlag;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.VariableDS;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.util.Misc;
import visad.Data;
import visad.DateTime;
import visad.DerivedUnit;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.RealType;
import visad.SampledSet;
import visad.Unit;
import visad.VisADException;
import visad.data.units.NoSuchUnitException;
import visad.data.units.ParseException;
import visad.data.units.Parser;
import visad.util.Util;

/**
 * A data source for NPOESS Preparatory Project (Suomi NPP) data
 * and JPSS data (JPSS-1 is officially NOAA-20).
 * 
 * This should probably move, but we are placing it here for now
 * since we are leveraging some existing code used for HYDRA.
 */

public class SuomiNPPDataSource extends HydraDataSource {

	private static final Logger logger = LoggerFactory.getLogger(SuomiNPPDataSource.class);
	
	/** Sources file */
    protected String filename;
    
    // for loading bundles, store granule lists and geo lists here
    protected List<String> oldSources = new ArrayList<>();
    protected List<String> geoSources = new ArrayList<>();
    
    // integrity map for grouping sets/aggregations of selected products
    Map<String, List<String>> filenameMap = null;

    protected MultiDimensionReader nppAggReader;

    protected MultiDimensionAdapter[] adapters = null;
    
    private List<MultiSpectralData> msd_CrIS = new ArrayList<>();
    private List<MultiSpectralData> multiSpectralData = new ArrayList<>();
    private Map<String, MultiSpectralData> msdMap = new HashMap<>();
    private Map<String, QualityFlag> qfMap = new HashMap<>();
    private Map<String, float[]> lutMap = new HashMap<>();

    private static final String DATA_DESCRIPTION = "JPSS Data";
    
    // instrument related variables and flags
    Attribute instrumentName = null;
    private String productName = null;
    
    // product related variables and flags
    String whichEDR = "";
    
    // for now, we are only handling CrIS variables that match this filter and SCAN dimensions
    private String crisFilter = "ES_Real";

    private Map<String, double[]> defaultSubset;
    public TrackAdapter track_adapter;

    private List<DataCategory> categories;
    private boolean isCombinedProduct = false;
    private boolean isDerived = false;
    private boolean nameHasBeenSet = false;

    private boolean isNOAA;
    private boolean isEnterprise;

    // need our own separator char since it's always Unix-style in the Suomi NPP files
    private static final String SEPARATOR_CHAR = "/";
    
    // date formatter for NASA L1B data, ex 2016-02-07T00:06:00.000Z
    SimpleDateFormat sdfNASA = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    SimpleDateFormat sdfEnterprise = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    // LUTs for NASA L1B data
    float[] m12LUT = null;
    float[] m13LUT = null;
    float[] m14LUT = null;
    float[] m15LUT = null;
    float[] m16LUT = null;
    float[] i04LUT = null;
    float[] i05LUT = null;
    
    // Map to match NASA variables to units (XML Product Profiles used for NOAA)
    Map<String, String> unitsNASA = new HashMap<String, String>();
    
    // Map to match Enterprise EDR variables to units
    Map<String, String> unitsEnterprise = new HashMap<String, String>();

    // date formatter for converting Suomi NPP day/time to something we can use
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss.SSS");
    
    // date formatter for how we want to show granule day/time on display
    SimpleDateFormat sdfOut = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    
    // MJH keep track of date to add time dim to FieldImpl
    Date theDate;

    // TJJ set different subset with full res stride for derived products
    Map<String, double[]> subset = null;
    private Map<String, double[]> derivedSubset = null;
    private boolean derivedInit = false;

    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    
    public SuomiNPPDataSource() {
    }
    
    public SuomiNPPDataSource(String fileName) throws VisADException {
    	this(null, Misc.newList(fileName), null);
    	logger.debug("filename only constructor call..");
    }

    /**
     * Construct a new Suomi NPP HDF5 data source.
     * @param  descriptor  descriptor for this {@code DataSource}
     * @param  fileName  name of the hdf file to read
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    
    public SuomiNPPDataSource(DataSourceDescriptor descriptor,
                                 String fileName, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.newList(fileName), properties);
        logger.debug("SuomiNPPDataSource called, single file selected: " + fileName);
    }

    /**
     * Construct a new Suomi NPP HDF5 data source.
     *
     * @param descriptor Descriptor for this {@code DataSource}.
     * @param newSources List of filenames.
     * @param properties Hashtable of properties.
     *
     * @throws VisADException problem creating data
     */
    
    public SuomiNPPDataSource(DataSourceDescriptor descriptor,
                                 List<String> newSources, Hashtable properties)
            throws VisADException {
        super(descriptor, newSources, DATA_DESCRIPTION, properties);
        logger.debug("SuomiNPPDataSource constructor called, file count: " + sources.size());

        filename = (String) sources.get(0);
        setDescription(DATA_DESCRIPTION);
        
        // NASA data is UTC, pre-set time zone
        SimpleTimeZone stz = new SimpleTimeZone(0, "UTC");
        sdfNASA.setTimeZone(stz);
        sdfEnterprise.setTimeZone(stz);
        
        // build the filename map - matches each product to set of files for that product
        filenameMap = new HashMap<>();
        
        // Pass 1, populate the list of products selected
        for (Object o : sources) {
        	String filename = (String) o;
        	// first five characters of any product go together
        	int lastSeparator = filename.lastIndexOf(File.separatorChar);
        	int firstUnderscore = filename.indexOf("_", lastSeparator + 1);
        	String prodStr = filename.substring(lastSeparator + 1, firstUnderscore);
        	if (! filenameMap.containsKey(prodStr)) {
				List<String> l = new ArrayList<String>();
				filenameMap.put(prodStr, l);
        	}
        }
        
        // Pass 2, create a list of files for each product in this data source
        for (Object o : sources) {
        	String filename = (String) o;
        	// first five characters of any product go together
        	int lastSeparator = filename.lastIndexOf(File.separatorChar);
        	int firstUnderscore = filename.indexOf("_", lastSeparator + 1);
        	String prodStr = filename.substring(lastSeparator + 1, firstUnderscore);
        	List<String> l = filenameMap.get(prodStr);
        	l.add(filename);
        	filenameMap.put(prodStr, l);
        }
        
        setup();
        initQfTranslations();
    }
    

	public void setup() throws VisADException {

		// which format, NASA, NOAA, or NOAA Enterprise?
		isNOAA = false;
		isEnterprise = false;
		nameHasBeenSet = false;
		
    	// store filenames for possible bundle unpersistence
    	for (Object o : sources) {
    		oldSources.add((String) o);
    	}
    	
    	// time zone for product labels
    	SimpleTimeZone stz = new SimpleTimeZone(0, "GMT");
    	sdf.setTimeZone(stz);
    	sdfOut.setTimeZone(stz);
    	
    	// looking to populate 3 things - path to lat, path to lon, path to relevant products
    	String pathToLat = null;
    	String pathToLon = null;
    	Set<String> pathToProducts = new LinkedHashSet<>();
    	Map<String, String> prodToDesc = new HashMap<>();
    	
    	// flag to differentiate VIIRS from one of the other Suomi sensors
    	boolean isVIIRS = true;
    	
    	// check source filenames to see if this is a combined product. everything
    	// from last file separator to first underscore should be product info
    	int lastSeparator = filename.lastIndexOf(File.separatorChar);
    	int firstUnderscore = filename.indexOf("_", lastSeparator + 1);
    	String prodStr = filename.substring(lastSeparator + 1, firstUnderscore);
    	// only do this check for NOAA data
        if (filename.endsWith(".h5")) {
    		isNOAA = true;
	        StringTokenizer st = new StringTokenizer(prodStr, "-");
	        logger.debug("SNPPDS check for embedded GEO, tokenizing: " + prodStr);
	        while (st.hasMoreTokens()) {
	        	String singleProd = st.nextToken();
	        	for (int i = 0; i < JPSSUtilities.geoProductIDs.length; i++) {
	        		if (singleProd.equals(JPSSUtilities.geoProductIDs[i])) {
	        			logger.debug("Setting isCombinedProduct true, Found embedded GEO: " + singleProd);
	        			isCombinedProduct = true;
	        			break;
	        		}
	        	}
	        }
    	}
    	
        lastSeparator = filename.lastIndexOf(File.separatorChar);
        String enterpriseTest = filename.substring(lastSeparator + 1);
        if (enterpriseTest.matches(JPSSUtilities.JPSS_REGEX_ENTERPRISE_EDR)) {
           isEnterprise = true;
        }

    	// various metatdata we'll need to gather on a per-product basis
        Map<String, String> unsignedFlags = new LinkedHashMap<>();
        Map<String, String> unpackFlags = new LinkedHashMap<>();
    	
    	// geo product IDs for each granule
    	Set<String> geoProductIDs = new LinkedHashSet<>();
    	
    	// aggregations will use sets of NetCDFFile readers
    	List<NetCDFFile> ncdfal = new ArrayList<>();
    	
    	// we should be able to find an XML Product Profile for each data/product type
		Map<String, SuomiNPPProductProfile> profiles = new HashMap<>();

    	// and also Profile metadata for geolocation variables
    	boolean haveGeoMetaData = false;
    	
    	// number of source granules which make up the data source
    	int granuleCount = 1;
    	   	
    	try {
    		
    		// for each source file provided, find the appropriate geolocation,
    		// get the nominal time and various other granule-level metadata
    		Iterator keyIterator = filenameMap.keySet().iterator();
    		while (keyIterator.hasNext()) {
    			String keyStr = (String) keyIterator.next();
        		List fileNames = (List) filenameMap.get(keyStr);
        		granuleCount = fileNames.size();
        		setProperty(Constants.PROP_GRANULE_COUNT, granuleCount + " Granule");
    			for (int fileCount = 0; fileCount < granuleCount; fileCount++) {
    				// need to open the main NetCDF file to determine the geolocation product
    				NetcdfFile ncfile = null;
    				String fileAbsPath = null;
    				try {
    					fileAbsPath = (String) fileNames.get(fileCount);
    					logger.debug("Trying to open file: " + fileAbsPath);
    					ncfile = NetcdfFile.open(fileAbsPath);
    					if (! isCombinedProduct) {
    						if (isNOAA) {
	    						Attribute a = ncfile.findGlobalAttribute("N_GEO_Ref");
	    						logger.debug("Value of GEO global attribute: " + a.getStringValue());
	    						String tmpGeoProductID = a.getStringValue();
	    						geoProductIDs.add(tmpGeoProductID);
    						} else {
    							geoProductIDs.add(keyStr.replace("L1B", "GEO"));
    						}
    					}
    					Group rg = ncfile.getRootGroup();

                        // Since no sub-groups for Enterprise EDRs, need to set date and instrument here
                        if (isEnterprise) {
                            if (! nameHasBeenSet) {
                                Attribute coverageStartTime = ncfile.findGlobalAttribute("time_coverage_start");
                                Date d = new Date();
                                if (coverageStartTime != null) {
                                    d = sdfEnterprise.parse(coverageStartTime.getStringValue());
                                } else {
                                    logger.error("Warning: unable to retrieve granule start time");
                                }
                                theDate = d;
                                instrumentName = ncfile.findGlobalAttribute("instrument_name");
                                setName(instrumentName.getStringValue() + " " + sdfOut.format(theDate));
                            }
                            nameHasBeenSet = true;
                        }

    					List<Group> gl = rg.getGroups();
    					if (gl != null) {
    						for (Group g : gl) {
    							logger.trace("Group name: " + g.getFullName());
    							if (isNOAA) {
									// when we find the Data_Products group, go down another group level and pull out 
									// what we will use for nominal day and time (for now anyway).
									// XXX TJJ fileCount check is so we don't count the GEO file in time array!
									if (g.getFullName().contains("Data_Products")
											&& (fileCount != fileNames.size())) {
										List<Group> dpg = g.getGroups();

										// cycle through once looking for XML Product Profiles
										for (Group subG : dpg) {

											String subName = subG.getFullName();
											// use actual product, not geolocation, to id XML Product Profile
											if (!subName.contains("-GEO")) {
												// determine the instrument name (VIIRS, ATMS, CrIS, OMPS)
												instrumentName = subG.findAttribute("Instrument_Short_Name");

												// note any EDR products, will need to check for and remove
												// fill scans later
												Attribute adtt = subG.findAttribute("N_Dataset_Type_Tag");
												if (adtt != null) {
													String baseName = adtt.getStringValue();
													if ((baseName != null) && (baseName.equals("EDR"))) {
														// have to loop through sub groups variables to determine band
														List<Variable> tmpVar = subG.getVariables();
														for (Variable v : tmpVar) {
															// if Imagery EDR attribute for band is specified, save it
															Attribute mBand = v.findAttribute("Band_ID");
															if (mBand != null) {
																whichEDR = mBand.getStringValue();
															}
														}
													}
												}

												// This is also where we find the attribute which tells us which
												// XML Product Profile to use!
												Attribute axpp = subG.findAttribute("N_Collection_Short_Name");
												if (axpp != null) {
													String baseName = axpp.getStringValue();
													productName = baseName;
													
													// TJJ Apr 2018
													// Hack so we can look at CrIS Full Spectrum, until we can
													// track down existence of an official Product Profile for it.
													// http://mcidas.ssec.wisc.edu/inquiry-v/?inquiry=2634
													// The regular SDR profile lets us visualize it.

													SuomiNPPProductProfile profile = new SuomiNPPProductProfile();
													String productProfileFileName = null;
													if (productName.equals("CrIS-FS-SDR")) {
														productProfileFileName = profile.getProfileFileName("CrIS-SDR");
													} else {
														productProfileFileName = profile.getProfileFileName(productName);
													}

													logger.info("Found profile: " + productProfileFileName + " for prod: " + productName);
													profiles.put(productName, profile);
													if (productProfileFileName == null) {
														throw new Exception("XML Product Profile not found in catalog for: " + productName);
													}
													try {
														profile.addMetaDataFromFile(productProfileFileName);
													} catch (Exception nppppe) {
														logger.error("Error parsing XML Product Profile: "
																+ productProfileFileName);
														throw new Exception("XML Product Profile Error", nppppe);
													}
												}
											}
										}

										// 2nd pass through sub-group to extract date/time for aggregation
										for (Group subG : dpg) {
											List<Variable> vl = subG.getVariables();
											for (Variable v : vl) {
												Attribute aDate = v.findAttribute("AggregateBeginningDate");
												Attribute aTime = v.findAttribute("AggregateBeginningTime");
												// did we find the attributes we are looking for?
												if ((aDate != null) && (aTime != null)) {
													// set time for display to day/time of 1st granule examined
												    if (! nameHasBeenSet) {
												        String sDate = aDate.getStringValue();
												        String sTime = aTime.getStringValue();
												        logger.debug("For day/time, using: " + sDate
												                + sTime.substring(0, sTime.indexOf('Z') - 3));
												        Date d = sdf.parse(sDate
												                + sTime.substring(0, sTime.indexOf('Z') - 3));
												        theDate = d;
												        setName(instrumentName.getStringValue() + " "
												                + sdfOut.format(d));
												        nameHasBeenSet = true;
												    }
													break;
												}
											}
										}
										if (! nameHasBeenSet) {
											throw new VisADException(
													"No date time found in Suomi NPP granule");
										}
									} 
                                } else {
									// NASA data - date/time from global attribute
									// set time for display to day/time of 1st granule examined
									Attribute timeStartNASA = ncfile.findGlobalAttribute("time_coverage_start");
									Date d = sdfNASA.parse(timeStartNASA.getStringValue());
									theDate = d;
									if (! nameHasBeenSet) {
										instrumentName = ncfile.findGlobalAttribute("instrument");
										setName(instrumentName.getStringValue() + " " + sdfOut.format(d));
										nameHasBeenSet = true;
									}
								}    	    			
    						}
    					}
    				} catch (Exception e) {
    					logger.warn("Exception during processing of file: " + fileAbsPath);
    					throw (e);
    				} finally {
    					ncfile.close();
    				}
    			}

    		}
    		
    		// build each union aggregation element
    		Iterator<String> iterator = geoProductIDs.iterator();
    		for (int elementNum = 0; elementNum < granuleCount; elementNum++) {
    			
    			String s = null;
    			
    			// build an XML (NCML actually) representation of the union aggregation of these two files
    			Namespace ns = Namespace.getNamespace("http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2");
    			Element root = new Element("netcdf", ns);
    			Document document = new Document(root);

    			Element agg = new Element("aggregation", ns);
    			agg.setAttribute("type", "union");
        		
    			// TJJ - Loop over filename map, could be several products that need to be aggregated
    	        Set set = filenameMap.keySet();
    	        Iterator mapIter = set.iterator();
    	        while (mapIter.hasNext()) {
    	        	String key = (String) mapIter.next();
    	        	List l = (List) filenameMap.get(key);
        			Element fData = new Element("netcdf", ns);
        			fData.setAttribute("location", (String) l.get(elementNum));
        			agg.addContent(fData);
        			s = (String) l.get(elementNum);
    	        }
    			
    			String geoFilename = null;
    			Element fGeo = new Element("netcdf", ns);;
    			
    			if (! isCombinedProduct) {
	
	    			if (isNOAA) {
						geoFilename = s.substring(0, s.lastIndexOf(File.separatorChar) + 1);
						// check if we have the whole file name or just the prefix
						String geoProductID = iterator.next();
						if (geoProductID.endsWith("h5")) {
							geoFilename += geoProductID;
						} else {
							geoFilename += geoProductID;
							geoFilename += s.substring(s
									.lastIndexOf(File.separatorChar) + 6);
						}
						// Be sure file as specified by N_GEO_Ref global attribute really is there.
						File tmpGeo = new File(geoFilename);
						if (!tmpGeo.exists()) {
							// Ok, the expected file defined (supposedly) exactly by a global att is not there...
							// We need to check for similar geo files with different creation dates
							String geoFileRelative = geoFilename
									.substring(geoFilename
											.lastIndexOf(File.separatorChar) + 1);
							// also check for Terrain Corrected version of geo
							String geoTerrainCorrected = geoFileRelative;
							geoTerrainCorrected = geoTerrainCorrected.replace(
									"OD", "TC");
							geoTerrainCorrected = geoTerrainCorrected.replace(
									"MG", "TC");

							// now we make a file filter, and see if a matching geo file is present
							File fList = new File(
									geoFilename.substring(
											0,
											geoFilename
													.lastIndexOf(File.separatorChar) + 1)); // current directory

							FilenameFilter geoFilter = new FilenameFilter() {
								public boolean accept(File dir, String name) {
									if (name.matches(JPSSUtilities.SUOMI_GEO_REGEX_NOAA)) {
										return true;
									} else {
										return false;
									}
								}
							};

							File[] files = fList.listFiles(geoFilter);
							for (File file : files) {
								if (file.isDirectory()) {
									continue;
								}
								// get the file name for convenience
								String fName = file.getName();
								// is it one of the standard Ellipsoid geo types we are looking for?
								if (fName.substring(0, 5).equals(
										geoFileRelative.substring(0, 5))) {
									int geoStartIdx = geoFileRelative
											.indexOf("_d");
									int prdStartIdx = fName.indexOf("_d");
									String s1 = geoFileRelative.substring(
											geoStartIdx, geoStartIdx + JPSSUtilities.NOAA_CREATION_DATE_INDEX);
									String s2 = fName.substring(prdStartIdx,
											prdStartIdx + JPSSUtilities.NOAA_CREATION_DATE_INDEX);
									if (s1.equals(s2)) {
										geoFilename = s.substring(0, s.lastIndexOf(File.separatorChar) + 1) + fName;
										break;
									}
								}
								// same check, but for Terrain Corrected version
								if (fName.substring(0, 5).equals(
										geoTerrainCorrected.substring(0, 5))) {
									int geoStartIdx = geoTerrainCorrected
											.indexOf("_d");
									int prdStartIdx = fName.indexOf("_d");
									String s1 = geoTerrainCorrected.substring(
											geoStartIdx, geoStartIdx + JPSSUtilities.NOAA_CREATION_DATE_INDEX);
									String s2 = fName.substring(prdStartIdx,
											prdStartIdx + JPSSUtilities.NOAA_CREATION_DATE_INDEX);
									if (s1.equals(s2)) {
										geoFilename = s.substring(0, s.lastIndexOf(File.separatorChar) + 1) + fName;
										break;
									}
								}
							}
						} 
					} else if (! isEnterprise) {
						// NASA format
						geoFilename = JPSSUtilities.replaceLast(s, "L1B", "GEO");
						// get list of files in current directory
						File fList = 
							new File(geoFilename.substring(0, geoFilename.lastIndexOf(File.separatorChar) + 1)); 
						// make a NASA style file filter, and see if a matching geo file is present
						FilenameFilter geoFilter = new FilenameFilter() {
							public boolean accept(File dir, String name) {
								if (name.matches(JPSSUtilities.SUOMI_GEO_REGEX_NASA)) {
									return true;
								} else {
									return false;
								}
							}
						};
						File[] files = fList.listFiles(geoFilter);
						for (File file : files) {
							if (file.isDirectory()) {
								continue;
							}
							// get the file name for convenience
							String fName = file.getName();
							String tmpStr = geoFilename.substring(s.lastIndexOf(File.separatorChar) + 1,
									s.lastIndexOf(File.separatorChar) + (JPSSUtilities.NASA_CREATION_DATE_INDEX + 1));
							if (fName.substring(0, JPSSUtilities.NASA_CREATION_DATE_INDEX).equals(tmpStr.substring(0, JPSSUtilities.NASA_CREATION_DATE_INDEX))) {
								geoFilename = s.substring(0, s.lastIndexOf(File.separatorChar) + 1) + fName;
								break;
							}
						}
					}
                    if (! isEnterprise) {
                        logger.debug("Determined GEO file name should be: " + geoFilename);
                        fGeo.setAttribute("location", geoFilename);
                        // add this to list used if we create a zipped bundle
                        geoSources.add(geoFilename);
                        agg.addContent(fGeo);
                    }
    			}

    			root.addContent(agg);    
    		    XMLOutputter xmlOut = new XMLOutputter();
    		    String ncmlStr = xmlOut.outputString(document);
    		    ByteArrayInputStream is = new ByteArrayInputStream(ncmlStr.getBytes());			
    		    MultiDimensionReader netCDFReader = new NetCDFFile(is);
    		    
    	    	// let's try and look through the NetCDF reader and see what we can learn...
    	    	NetcdfFile ncdff = ((NetCDFFile) netCDFReader).getNetCDFFile();
    	    	
    	    	Group rg = ncdff.getRootGroup();
    	    	// this is a list filled with unpacked qflag products, if any
    	    	ArrayList<VariableDS> qfProds = new ArrayList<VariableDS>();
    	    	
    	    	// this is a list filled with pseudo Brightness Temp variables converted from Radiance
    	    	ArrayList<VariableDS> btProds = new ArrayList<VariableDS>();

                // Enterprise EDRs - no groups, just scan root level
                if (isEnterprise) {
                    List<Variable> vl = rg.getVariables();
                    int xDimEnt = -1;
                    int yDimEnt = -1;
                    for (Variable v : vl) {
                        if (v.getShortName().equals("Latitude")) {
                            pathToLat = v.getFullName();
                            pathToProducts.add(v.getFullName());
                            prodToDesc.put(v.getFullName(), v.getDescription());
                            xDimEnt = v.getDimension(0).getLength();
                            yDimEnt = v.getDimension(1).getLength();
                        }
                        if (v.getShortName().equals("Longitude")) {
                            pathToLon = v.getFullName();
                            pathToProducts.add(v.getFullName());
                            prodToDesc.put(v.getFullName(), v.getDescription());
                        }
                        // store units in a map for later
                        Attribute unitAtt = v.findAttribute("units");
                        if (unitAtt != null) {
                            unitsEnterprise.put(v.getShortName(), unitAtt.getStringValue());
                        } else {
                            unitsEnterprise.put(v.getShortName(), "Unknown");
                        }
                    }

                    for (Variable v : vl) {
                        if (v.getShortName().equals("Latitude")) continue;
                        if (v.getShortName().equals("Longitude")) continue;
                        // keep any data which matches geolocation dimensions
                        if (v.getRank() != 2) continue;
                        if (v.getDimension(0).getLength() == xDimEnt &&
                            v.getDimension(1).getLength() == yDimEnt) {
                            pathToProducts.add(v.getFullName());
                            prodToDesc.put(v.getFullName(), v.getDescription());
                        }
                        // store units in a map for later
                        Attribute unitAtt = v.findAttribute("units");
                        if (unitAtt != null) {
                            unitsEnterprise.put(v.getShortName(), unitAtt.getStringValue());
                        } else {
                            unitsEnterprise.put(v.getShortName(), "Unknown");
                        }
                    }
                }

    	    	List<Group> gl = rg.getGroups();
    	    	if (gl != null) {
    				int xDimNASA = -1;
    				int yDimNASA = -1;
    	    		// Make a first pass to determine the shape of the geolocation data
    	    		for (Group g : gl) {
    	    			if (g.getFullName().contains("geolocation_data")) {
    	    				List<Variable> vl = g.getVariables();
    						for (Variable v : vl) {
    							if (v.getShortName().equals("latitude")) {
    								// XXX TJJ Nov 2015
    								// Hack because fill value in attribute does not match
    								// what I am seeing in the data.
    								Attribute fillAtt = new Attribute("_FillValue", -999.0);
    								v.addAttribute(fillAtt);
    								pathToLat = v.getFullName();
    								pathToProducts.add(v.getFullName());
    								prodToDesc.put(v.getFullName(), v.getDescription());
    								xDimNASA = v.getDimension(0).getLength();
    								yDimNASA = v.getDimension(1).getLength();
    							}
    							if (v.getShortName().equals("longitude")) {
    								// XXX TJJ Nov 2015
    								// Hack because fill value in attribute does not match
    								// what I am seeing in the data.
    								Attribute fillAtt = new Attribute("_FillValue", -999.0);
    								v.addAttribute(fillAtt);
    								pathToLon = v.getFullName();
    								pathToProducts.add(v.getFullName());
									prodToDesc.put(v.getFullName(), v.getDescription());
    							}
    						}
    	    			}
    	    		}
    	    		for (Group g : gl) {
    	    			logger.debug("Group name: " + g.getFullName());
    	    			// NASA only - looking through observation_data and geolocation_data
    	    			if (g.getFullName().contains("observation_data")) {
    	    				List<Variable> vl = g.getVariables();
    						for (Variable v : vl) {
    							// keep any data which matches geolocation dimensions
    							if (v.getDimension(0).getLength() == xDimNASA &&
    								v.getDimension(1).getLength() == yDimNASA) {
	    							logger.debug("Adding product: " + v.getFullName());
	    							pathToProducts.add(v.getFullName());
									prodToDesc.put(v.getFullName(), v.getDescription());
	    							Attribute aUnsigned = v.findAttribute("_Unsigned");
	    							if (aUnsigned != null) {
	    								unsignedFlags.put(v.getFullName(), aUnsigned.getStringValue());
	    							} else {
	    								unsignedFlags.put(v.getFullName(), "false");
	    							}
	    							
	    							// store units in a map for later
	    							Attribute unitAtt = v.findAttribute("units");
	    							if (unitAtt != null) {
	    								unitsNASA.put(v.getShortName(), unitAtt.getStringValue());
	    							} else {
	    								unitsNASA.put(v.getShortName(), "Unknown");
	    							}
	    							
	    							// TJJ Nov 2018 - SIPS V2+ mods
	    							// Regridding with bow-tie interpolation wasn't working since there are
	    							// now multiple fill value categories and we need to look specifically
	    							// for the bowtie deletion flag
	    							
	    							Attribute longNameAtt = v.findAttribute("long_name");
	    							String longName = "empty";
	    							if (longNameAtt != null) longName = longNameAtt.getStringValue();
	    							if (longName.contains("reflectance") || longName.contains("radiance")) {
	    							    
	    							    Attribute flagMeanings = v.findAttribute(JPSSUtilities.SIPS_FLAG_MEANINGS_ATTRIBUTE);
	    							    // If this is not null, we must be v2.0.0 or higher
	    							    if (flagMeanings != null) {
	    							        String meanings = flagMeanings.getStringValue();
	    							        // Tokenize meanings string, multiple flags defined there
	    							        StringTokenizer st = new StringTokenizer(meanings);
	    							        int bowtieIdx = -1;
	    							        boolean foundBowTieAttribute = false;
	    							        String tokStr = null;
	    							        while (st.hasMoreTokens()) {
	    							            tokStr = st.nextToken();
	    							            bowtieIdx++;
	    							            if (tokStr.equals(JPSSUtilities.SIPS_BOWTIE_DELETED_FLAG)) {
	    							                foundBowTieAttribute = true;
	    							                break;
	    							            }
	    							        }

	    							        if (foundBowTieAttribute) {
	    							            Attribute flagValues = v.findAttribute(JPSSUtilities.SIPS_FLAG_VALUES_ATTRIBUTE);
	    							            Array flagValsArr = flagValues.getValues();
	    							            int bowTieVal = (int) flagValsArr.getInt(bowtieIdx);
	    							            Attribute a1 = new Attribute("_FillValue", bowTieVal);
	    							            v.addAttribute(a1);
	    							        }
	    							    }

	    							}
	    							
	    							// TJJ Feb 2016 - Create BT variables where applicable
	    							if ((v.getShortName().matches("M12|M13|M14|M15|M16")) ||
	    								(v.getShortName().matches("I04|I05"))) {
	    								
	    								// Get the LUT variable, load into primitive array
	    								Variable lut = g.findVariable(v.getShortName() + "_brightness_temperature_lut");
	    								int [] lutShape = lut.getShape();
	    								logger.debug("Handling NASA LUT Variable, LUT size: " + lutShape[0]);
	    								
	    								// pull out valid min, max - these will be used for our new VariableDS
	    								Attribute aVMin = lut.findAttribute("valid_min");
	    								Attribute aVMax = lut.findAttribute("valid_max");
	    								Attribute fillAtt = lut.findAttribute("_FillValue");
	    								logger.debug("valid_min from LUT: " + aVMin.getNumericValue());
	    								logger.debug("valid_max from LUT: " + aVMax.getNumericValue());
	    								
	    								// A little hacky, but at this point the class is such a mess
	    								// that what's a little more, right? Load M12-M16, I4-I5 LUTS
	    								
	    								if (v.getShortName().matches("M12")) {
		    								m12LUT = new float[lutShape[0]];
		    								ArrayFloat.D1 lutArray = (ArrayFloat.D1) lut.read();
		    								for (int lutIdx = 0; lutIdx < lutShape[0]; lutIdx++) {
		    									m12LUT[lutIdx] = lutArray.get(lutIdx);
		    								}
	    								}
	    								
	    								if (v.getShortName().matches("M13")) {
		    								m13LUT = new float[lutShape[0]];
		    								ArrayFloat.D1 lutArray = (ArrayFloat.D1) lut.read();
		    								for (int lutIdx = 0; lutIdx < lutShape[0]; lutIdx++) {
		    									m13LUT[lutIdx] = lutArray.get(lutIdx);
		    								}
	    								}
	    								
	    								if (v.getShortName().matches("M14")) {
		    								m14LUT = new float[lutShape[0]];
		    								ArrayFloat.D1 lutArray = (ArrayFloat.D1) lut.read();
		    								for (int lutIdx = 0; lutIdx < lutShape[0]; lutIdx++) {
		    									m14LUT[lutIdx] = lutArray.get(lutIdx);
		    								}
	    								}
	    								
	    								if (v.getShortName().matches("M15")) {
		    								m15LUT = new float[lutShape[0]];
		    								ArrayFloat.D1 lutArray = (ArrayFloat.D1) lut.read();
		    								for (int lutIdx = 0; lutIdx < lutShape[0]; lutIdx++) {
		    									m15LUT[lutIdx] = lutArray.get(lutIdx);
		    								}
	    								}
	    								
	    								if (v.getShortName().matches("M16")) {
		    								m16LUT = new float[lutShape[0]];
		    								ArrayFloat.D1 lutArray = (ArrayFloat.D1) lut.read();
		    								for (int lutIdx = 0; lutIdx < lutShape[0]; lutIdx++) {
		    									m16LUT[lutIdx] = lutArray.get(lutIdx);
		    								}
	    								}
	    								
	    								if (v.getShortName().matches("I04")) {
		    								i04LUT = new float[lutShape[0]];
		    								ArrayFloat.D1 lutArray = (ArrayFloat.D1) lut.read();
		    								for (int lutIdx = 0; lutIdx < lutShape[0]; lutIdx++) {
		    									i04LUT[lutIdx] = lutArray.get(lutIdx);
		    								}
	    								}
	    								
	    								if (v.getShortName().matches("I05")) {
		    								i05LUT = new float[lutShape[0]];
		    								ArrayFloat.D1 lutArray = (ArrayFloat.D1) lut.read();
		    								for (int lutIdx = 0; lutIdx < lutShape[0]; lutIdx++) {
		    									i05LUT[lutIdx] = lutArray.get(lutIdx);
		    								}
	    								}
	    								
	    								// Create a pseudo-variable, fill using LUT
    									// make a copy of the source variable
    									// NOTE: by using a VariableDS here, the original
    									// variable is used for the I/O, this matters!
    									VariableDS vBT = new VariableDS(g, v, false);

    									// Name is orig name plus suffix
    									vBT.setShortName(v.getShortName() + "_BT");

	    								vBT.addAttribute(fillAtt);
	    								vBT.addAttribute(aVMin);
	    								vBT.addAttribute(aVMax);

    									if (v.getShortName().matches("M12")) {
    										lutMap.put(vBT.getFullName(), m12LUT);
    									}
    									if (v.getShortName().matches("M13")) {
    										lutMap.put(vBT.getFullName(), m13LUT);
    									}
    									if (v.getShortName().matches("M14")) {
    										lutMap.put(vBT.getFullName(), m14LUT);
    									}
    									if (v.getShortName().matches("M15")) {
    										lutMap.put(vBT.getFullName(), m15LUT);
    									}
    									if (v.getShortName().matches("M16")) {
    										lutMap.put(vBT.getFullName(), m16LUT);
    									}
    									if (v.getShortName().matches("I04")) {
    										lutMap.put(vBT.getFullName(), i04LUT);
    									}
    									if (v.getShortName().matches("I05")) {
    										lutMap.put(vBT.getFullName(), i05LUT);
    									}
    									pathToProducts.add(vBT.getFullName());
    									String newName = vBT.getDescription().replace("radiance", "brightness temperature");
										prodToDesc.put(vBT.getFullName(), newName);
    									btProds.add(vBT);
	    							}
    							}
    						}
    	    			}
    	    			if (g.getFullName().contains("geolocation_data")) {
    	    				List<Variable> vl = g.getVariables();
    						for (Variable v : vl) {
    							// keep any data which matches geolocation dimensions
    							if (v.getDimension(0).getLength() == xDimNASA &&
    								v.getDimension(1).getLength() == yDimNASA) {
    								// except we already found Lat and Lon, skip those 
    								if ((v.getShortName().equals("latitude")) ||
    								    (v.getShortName().equals("latitude"))) continue;
	    							logger.debug("Adding product: " + v.getFullName());
	    							pathToProducts.add(v.getFullName());
									prodToDesc.put(v.getFullName(), v.getDescription());
    							}
    						}
    	    			}
    	    			
    	    			// NOAA only - we are looking through All_Data, finding displayable data
    	    			if (g.getFullName().contains("All_Data")) {
    	    				List<Group> adg = g.getGroups();
    	    				int xDim = -1;
    	    				int yDim = -1;

    	    				// two sub-iterations, first one to find geolocation and product dimensions
    	    				for (Group subG : adg) {
    	    					logger.debug("Sub group name: " + subG.getFullName());
    	    					String subName = subG.getFullName();
    	    					if (subName.contains("-GEO")) {
    	    						// this is the geolocation data
    	    						String geoBaseName = subG.getShortName();
    	    						geoBaseName = geoBaseName.substring(0, geoBaseName.indexOf('_'));
    	    						if (! haveGeoMetaData) {
										SuomiNPPProductProfile profile = new SuomiNPPProductProfile();
                                        String geoProfileFileName = profile.getProfileFileName(geoBaseName);
										logger.info("Found profile: " + geoProfileFileName + " for prod: " + geoBaseName);
										if (geoProfileFileName == null) {
											throw new Exception("XML Product Profile not found in catalog for: " + geoBaseName);
										}
	    								// also add meta data from geolocation profile
										profile.addMetaDataFromFile(geoProfileFileName);
										profiles.put(geoBaseName, profile);
    	    							haveGeoMetaData = true;
    	    						}
    	    						List<Variable> vl = subG.getVariables();
    	    						for (Variable v : vl) {
    	    							if (v.getFullName().endsWith(SEPARATOR_CHAR + "Latitude")) {
    	    								pathToLat = v.getFullName();
    	    								logger.debug("Ellipsoid Lat/Lon Variable: " + v.getFullName());
    	    								// get the dimensions of the lat variable
    	    								Dimension dAlongTrack = v.getDimension(0);
    	    								yDim = dAlongTrack.getLength();
    	    								Dimension dAcrossTrack = v.getDimension(1);
    	    								xDim = dAcrossTrack.getLength();
    	    								logger.debug("Lat across track dim: " + dAcrossTrack.getLength());
    	    							}
    	    							if (v.getFullName().endsWith(SEPARATOR_CHAR + "Longitude")) {
    	    								// we got dimensions from lat, don't need 'em twice, but need path
    	    								pathToLon = v.getFullName();
    	    							}
    	    						} 
    	    						// one more pass in case there is terrain-corrected Lat/Lon
    	    						for (Variable v : vl) {
    	    							if (v.getFullName().endsWith(SEPARATOR_CHAR + "Latitude_TC")) {
    	    								pathToLat = v.getFullName();
    	    								logger.debug("Switched Lat/Lon Variable to TC: " + v.getFullName());
    	    								// get the dimensions of the lat variable
    	    								Dimension dAlongTrack = v.getDimension(0);
    	    								yDim = dAlongTrack.getLength();
    	    								Dimension dAcrossTrack = v.getDimension(1);
    	    								xDim = dAcrossTrack.getLength();
    	    								logger.debug("Lat across track dim: " + dAcrossTrack.getLength());
    	    							}
    	    							if (v.getFullName().endsWith(SEPARATOR_CHAR + "Longitude_TC")) {
    	    								// we got dimensions from lat, don't need 'em twice, but need path
    	    								pathToLon = v.getFullName();
    	    							}
    	    						}
    	    					}
    	    				}

    	    				// second to identify displayable products
    	    				for (Group subG : adg) {
    	    					// this is the product data
    	    					List<Variable> vl = subG.getVariables();
    	    					for (Variable v : vl) {
    	    						boolean useThis = false;
    	    						String vName = v.getFullName();
    	    						logger.trace("Variable: " + vName);
    	    						String varShortName = vName.substring(vName.lastIndexOf(SEPARATOR_CHAR) + 1);

									// Pull out the profile pertaining to this variable
									SuomiNPPProductProfile matchingProfile = null;
									Set<String> keys = profiles.keySet();
									for (String key : keys) {
										if (v.getFullName().contains(key)) {
											matchingProfile = profiles.get(key);
											break;
										}
									}

									if (matchingProfile == null) {
										throw new VisADException("No profile found for " + varShortName);
									}

    	    						// Special code to handle quality flags. We throw out anything
    	    						// that does not match bounds of the geolocation data
    	    						
    	    						if (varShortName.startsWith("QF")) {
    	    							
    	    							logger.trace("Handling Quality Flag: " + varShortName);
    	    							
        	    						// this check is done later for ALL variables, but we need
    	    							// it early here to weed out those quality flags that are 
    	    							// simply a small set of data w/no granule geo nbounds
    	    							boolean xScanOk = false;
        	    						boolean yScanOk = false;
        	    						List<Dimension> dl = v.getDimensions();
        	    						
        	    						// toss out > 2D Quality Flags 
        	    						if (dl.size() > 2) {
        	    							logger.trace("SKIPPING QF, > 2D: " + varShortName);
        	    							continue;
        	    						}
        	    						
        	    						for (Dimension d : dl) {
        	    							// in order to consider this a displayable product, make sure
        	    							// both scan direction dimensions are present and look like a granule
        	    							if (d.getLength() == xDim) {
        	    								xScanOk = true;
        	    							}
        	    							if (d.getLength() == yDim) {
        	    								yScanOk = true;
        	    							}
        	    						}
        	    						
        	    						if (! (xScanOk && yScanOk)) {
        	    							logger.trace("SKIPPING QF, does not match geo bounds: " + varShortName);
        	    							continue;
        	    						}

                                        ArrayList<QualityFlag> qfal = matchingProfile.getQualityFlags(varShortName);
    	    							if (qfal != null) {
    	    								for (QualityFlag qf : qfal) {
    	    									qf.setPackedName(vName);
    	    									// make a copy of the qflag variable
    	    									// NOTE: by using a VariableDS here, the original
    	    									// variable is used for the I/O, this matters!
    	    									VariableDS vqf = new VariableDS(subG, v, false);
    	    									// prefix with QF num to help guarantee uniqueness across groups
    	    									// this will cover most cases, but could still be dupe names
    	    									// within a single QF.  This is handled when fetching XMLPP metadata
    	    									vqf.setShortName(
    	    											varShortName.substring(0, 3) + "_" + qf.getName()
    	    									);
    	    									logger.debug("New QF var full name: " + vqf.getFullName());
    	    	    							qfProds.add(vqf);
    	    	    							qfMap.put(vqf.getFullName(), qf);
    	    								}
    	    							}
    	    						}

    	    						// for CrIS instrument, first find dimensions of var matching
    	    						// CrIS filter, then throw out all variables which don't match 
    	    						// those dimensions
    	    						
    	    						if (instrumentName.getStringValue().equals("CrIS")) {
    	    							if (! vName.contains("GEO")) {
	    	    							if (! varShortName.startsWith(crisFilter)) {
	    	    								logger.trace("Skipping variable: " + varShortName);
	    	    								continue;
	    	    							}
    	    							} else {
    	    								// these variables are all GEO-related
    	    								// if they match lat/lon bounds, keep them
    	    								List<Dimension> dl = v.getDimensions();
        	    							if (dl.size() == 3) {
        	    								boolean isDisplayableCrIS = true;
        	    								for (Dimension d : dl) {
        	    									if ((d.getLength() != xDim) && (d.getLength() != yDim) && (d.getLength() != 9)) {
        	    										isDisplayableCrIS = false;
        	    									}
        	    								}
        	    								if (! isDisplayableCrIS) {
        	    									continue;
        	    								}
        	    							}
    	    							}
    	    						}

    	    						DataType dt = v.getDataType();
    	    						if ((dt.getSize() != 4) && (dt.getSize() != 2) && (dt.getSize() != 1)) {
    	    							continue;
    	    						}

    	    						List<Dimension> dl = v.getDimensions();
    	    						if (dl.size() > 4) {
    	    							continue;
    	    						}

    	    						// for now, skip any 3D VIIRS data
    	    						if (instrumentName.getStringValue().equals("VIIRS")) {
    	    							if (dl.size() == 3) {
    	    								continue;
    	    							}
    	    						}

    	    						boolean xScanOk = false;
    	    						boolean yScanOk = false;
    	    						for (Dimension d : dl) {
    	    							// in order to consider this a displayable product, make sure
    	    							// both scan direction dimensions are present and look like a granule
    	    							if (d.getLength() == xDim) {
    	    								xScanOk = true;
    	    							}
    	    							if (d.getLength() == yDim) {
    	    								yScanOk = true;
    	    							}
    	    						}

    	    						if (xScanOk && yScanOk) {
    	    							useThis = true;
    	    						}

    	    						// For ATMS, only 3-D variable we pass through is BrightnessTemperature
    	    						// Dimensions for BT are (lon, lat, channel)
    	    						if (instrumentName.getStringValue().equals("ATMS")) {
    	    							if (dl.size() == 3) {
    	    								boolean isDisplayableATMS = false;
    	    								for (Dimension d : dl) {
    	    									if (d.getLength() == JPSSUtilities.ATMSChannelCenterFrequencies.length) {
    	    										isDisplayableATMS = true;
    	    										logger.trace("This variable has a dimension matching num ATMS channels");
    	    										break;
    	    									}
    	    								}
    	    								if (! isDisplayableATMS) useThis = false;
    	    							}
    	    						}

    	    						// sensor data with a channel dimension
    	    						if (useThis) {
    	    							if ((instrumentName.getStringValue().equals("CrIS")) ||
    	    									(instrumentName.getStringValue().equals("ATMS")) || 
    	    									(instrumentName.getStringValue().contains("OMPS"))) {
    	    								isVIIRS = false;
    	    								logger.debug("Handling non-VIIRS data source...");
    	    							}
    	    						}

    	    						if (useThis) { 
    	    							// loop through the variable list again, looking for a corresponding "Factors"
    	    							float scaleVal = 1f;
    	    							float offsetVal = 0f;
    	    							boolean unpackFlag = false;

    	    							//   if the granule has an entry for this variable name
    	    							//     get the data, data1 = scale, data2 = offset
    	    							//     create and poke attributes with this data
    	    							//   endif

                                        String factorsVarName = matchingProfile.getScaleFactorName(varShortName);
    	    							if (factorsVarName != null) {
                                            logger.debug("Mapping: " + varShortName + " to: " + factorsVarName);
    	    								for (Variable fV : vl) {
    	    									if (fV.getShortName().equals(factorsVarName)) {
    	    										logger.trace("Pulling scale and offset values from variable: " + fV.getShortName());
    	    										ucar.ma2.Array a = fV.read();
    	    										float[] so = (float[]) a.copyTo1DJavaArray();
    	    										scaleVal = so[0];
    	    										offsetVal = so[1];
    	    										logger.trace("Scale value: " + scaleVal + ", Offset value: " + offsetVal);
    	    										unpackFlag = true;
    	    										break;
    	    									}
    	    								}
    	    							}

    	    							// poke in scale/offset attributes for now

    	    							Attribute a1 = new Attribute("scale_factor", scaleVal);
    	    							v.addAttribute(a1);
    	    							Attribute a2 = new Attribute("add_offset", offsetVal);
                                        v.addAttribute(a2);

    	    							// add valid range and fill value attributes here
    	    							// try to fill in valid range
                                        if (matchingProfile.hasNameAndMetaData(varShortName)) {
                                            String rangeMin = matchingProfile.getRangeMin(varShortName);
                                            String rangeMax = matchingProfile.getRangeMax(varShortName);
                                            logger.trace("range min: " + rangeMin + ", range max: " + rangeMax);
    	    								// only store range attribute if VALID range found
    	    								if ((rangeMin != null) && (rangeMax != null)) {
    	    									int [] shapeArr = new int [] { 2 };
    	    									ArrayFloat af = new ArrayFloat(shapeArr);
    	    									try {
    	    										af.setFloat(0, Float.parseFloat(rangeMin));
    	    									} catch (NumberFormatException nfe) {
    	    										af.setFloat(0, Float.valueOf(Integer.MIN_VALUE));
    	    									}
    	    									try {
    	    										af.setFloat(1, Float.parseFloat(rangeMax));
    	    									} catch (NumberFormatException nfe) {
    	    										af.setFloat(1, Float.valueOf(Integer.MAX_VALUE));
    	    									}
    	    									Attribute rangeAtt = new Attribute("valid_range", af);
    	    									v.addAttribute(rangeAtt);
    	    								}

    	    								// check for and load fill values too...

    	    								// we need to check two places, first, the XML product profile
                                            ArrayList<Float> fval = matchingProfile.getFillValues(varShortName);

    	    								// 2nd, does the variable already have one defined?
    	    								// if there was already a fill value associated with this variable, make
    	    								// sure we bring that along for the ride too...
    	    								Attribute aFill = v.findAttribute("_FillValue");

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
    	    										logger.trace("Adding fill value (from XML): " + fval.get(fillIdx));
    	    									}
    	    								}

    	    								if (aFill != null) {
    	    									Number n = aFill.getNumericValue();
    	    									// is the data unsigned?
    	    									Attribute aUnsigned = v.findAttribute("_Unsigned");
    	    									float fillValAsFloat = Float.NaN;
    	    									if (aUnsigned != null) {
    	    										if (aUnsigned.getStringValue().equals("true")) {
    	    											DataType fvdt = aFill.getDataType();
    	    											logger.trace("Data String: " + aFill.toString());
    	    											logger.trace("DataType primitive type: " + fvdt.getPrimitiveClassType());
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
    	    									logger.trace("Adding fill value (from variable): " + fillValAsFloat);
    	    								}
    	    								Attribute fillAtt = new Attribute("_FillValue", afFill);
    	    								v.addAttribute(fillAtt);
    	    							}

    	    							Attribute aUnsigned = v.findAttribute("_Unsigned");
    	    							if (aUnsigned != null) {
    	    								unsignedFlags.put(v.getFullName(), aUnsigned.getStringValue());
    	    							} else {
    	    								unsignedFlags.put(v.getFullName(), "false");
    	    							}

    	    							if (unpackFlag) {
    	    								unpackFlags.put(v.getFullName(), "true");
    	    							} else {
    	    								unpackFlags.put(v.getFullName(), "false");
    	    							}

    	    							logger.debug("Adding product: " + v.getFullName());
    	    							pathToProducts.add(v.getFullName());
										prodToDesc.put(v.getFullName(), v.getDescription());
    	    						}
    	    					}
    	    				}
    	    			}
    	    		}
    	    	}
    	    	
    	    	// add in any unpacked qflag products
    	    	for (VariableDS qfV: qfProds) {
    	    		// skip the spares - they are reserved for future use
    	    		if (qfV.getFullName().endsWith("Spare")) {
    	    			continue;
    	    		}
    	    		// String.endsWith is case sensitive so gotta check both cases
    	    		if (qfV.getFullName().endsWith("spare")) {
    	    			continue;
    	    		}
    	    		ncdff.addVariable(qfV.getGroup(), qfV);
    	    		logger.trace("Adding QF product: " + qfV.getFullName());
    	    		pathToProducts.add(qfV.getFullName());
					prodToDesc.put(qfV.getFullName(), qfV.getDescription());
    	    		unsignedFlags.put(qfV.getFullName(), "true");
    	    		unpackFlags.put(qfV.getFullName(), "false");
    	    	}
    	    	
    	    	// add in any pseudo BT products from NASA data
    	    	for (Variable vBT: btProds) {
    	    		logger.trace("Adding BT product: " + vBT.getFullName());
					ncdff.addVariable(vBT.getGroup(), vBT);
					unsignedFlags.put(vBT.getFullName(), "true");
					unpackFlags.put(vBT.getFullName(), "false");
    	    	}
    	    	
    		    ncdfal.add((NetCDFFile) netCDFReader);
    		}
    		
    	} catch (Exception e) {
    		logger.error("cannot create NetCDF reader for files selected", e);
    		if (e.getMessage() != null && e.getMessage().equals("XML Product Profile Error")) {
    			throw new VisADException("Unable to extract metadata from required XML Product Profile", e);
    		}
    	}
    	
    	// TJJ Feb 2018 
    	// Doing a reorder of variable names here, as per HP's request from
    	// http://mcidas.ssec.wisc.edu/inquiry-v/?inquiry=2613

    	if (isVIIRS) {
    	    // Copy the variable Set to a sortable List
    	    List<String> sortedList = new ArrayList(pathToProducts);
    	    Collections.sort(sortedList, new VIIRSSort());

    	    // Clear the original data structure which retains insert order
    	    // (it's a LinkedHashSet)
    	    pathToProducts.clear();

    	    // Re-add the variables in corrected order
    	    for (String s : sortedList) {
    	        pathToProducts.add(s);
    	    }
    	}
        
    	// initialize the aggregation reader object
    	try {
    		if (isNOAA) {
    		    nppAggReader = new GranuleAggregation(ncdfal, pathToProducts, "Track", "XTrack", isVIIRS);
    		    ((GranuleAggregation) nppAggReader).setQfMap(qfMap);
            } else if (isEnterprise) {
                nppAggReader = new GranuleAggregation(ncdfal, pathToProducts, "Rows", "Columns", isVIIRS);
                ((GranuleAggregation) nppAggReader).setQfMap(qfMap);
    		} else {
    			nppAggReader = new GranuleAggregation(ncdfal, pathToProducts, "number_of_lines", "number_of_pixels", isVIIRS);
    		    ((GranuleAggregation) nppAggReader).setLUTMap(lutMap);
    		}
    	} catch (Exception e) {
    		throw new VisADException("Unable to initialize aggregation reader", e);
    	}

    	// make sure we found valid data
    	if (pathToProducts.size() == 0) {
    		throw new VisADException("No data found in files selected");
    	}
    	
    	logger.debug("Number of adapters needed: " + pathToProducts.size());
    	adapters = new MultiDimensionAdapter[pathToProducts.size()];
    	Hashtable<String, String[]> properties = new Hashtable<>();
    	
    	Iterator<String> iterator = pathToProducts.iterator();
    	int pIdx = 0;
    	boolean adapterCreated = false;
    	while (iterator.hasNext()) {
    		String pStr = iterator.next();
    		logger.debug("Working on adapter number " + (pIdx + 1) + ": " + pStr);
        	Map<String, Object> swathTable = SwathAdapter.getEmptyMetadataTable();
        	Map<String, Object> spectTable = SpectrumAdapter.getEmptyMetadataTable();
        	swathTable.put("array_name", pStr);
        	swathTable.put("lon_array_name", pathToLon);
        	swathTable.put("lat_array_name", pathToLat);
        	swathTable.put("XTrack", "XTrack");
        	swathTable.put("Track", "Track");
        	swathTable.put("geo_Track", "Track");
        	swathTable.put("geo_XTrack", "XTrack");
        	// TJJ is this even needed?  Is product_name used anywhere?
        	if (productName == null) productName = pStr.substring(pStr.indexOf(SEPARATOR_CHAR) + 1);
        	swathTable.put("product_name", productName);
			swathTable.put("_mapping", prodToDesc);
        	// array_name common to spectrum table
        	spectTable.put("array_name", pStr);
        	spectTable.put("product_name", productName);
			spectTable.put("_mapping", prodToDesc);
        	
        	if (! isVIIRS) {

        		// 3D data is either ATMS, OMPS, or CrIS
        		if ((instrumentName.getShortName() != null) && (instrumentName.getStringValue().equals("ATMS"))) {

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
                		
        			} else if (instrumentName.getStringValue().contains("OMPS")) {
        				
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
                        
            			int numChannels = 200;
            			if (instrumentName.getStringValue().equals("OMPS-TC")) {
            				numChannels = 260;
            			}
            			logger.debug("Setting up OMPS adapter, num channels: " + numChannels);
                		float[] bandArray = new float[numChannels];
                		String[] bandNames = new String[numChannels];
                		for (int bIdx = 0; bIdx < numChannels; bIdx++) {
                			bandArray[bIdx] = bIdx;
                			bandNames[bIdx] = "Channel " + (bIdx + 1);
                		}
                		spectTable.put(SpectrumAdapter.channelValues, bandArray);
                		spectTable.put(SpectrumAdapter.bandNames, bandNames);
                		
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
        	swathTable.put("range_name", pStr.substring(pStr.indexOf(SEPARATOR_CHAR) + 1));
        	spectTable.put("range_name", pStr.substring(pStr.indexOf(SEPARATOR_CHAR) + 1));
        	
        	// set the valid range hash if data is available
			// Pull out the profile pertaining to this variable
			SuomiNPPProductProfile stProfile = null;
			Set<String> keys = profiles.keySet();
			for (String key : keys) {
				if (productName.contains(key)) {
					stProfile = profiles.get(key);
					break;
				}
			}

			if (stProfile == null) {
				throw new VisADException("No profile found for " + productName);
			}

            if (stProfile != null) {
                if (stProfile.getRangeMin(pStr.substring(pStr.lastIndexOf(SEPARATOR_CHAR) + 1)) != null) {
        			swathTable.put("valid_range", "valid_range");
        		}
        	}
        	
        	String unsignedAttributeStr = unsignedFlags.get(pStr);
        	if ((unsignedAttributeStr != null) && (unsignedAttributeStr.equals("true"))) {
        		swathTable.put("unsigned", unsignedAttributeStr);
        	}
        	
        	String unpackFlagStr = unpackFlags.get(pStr);
        	if ((unpackFlagStr != null) && (unpackFlagStr.equals("true"))) {
        		swathTable.put("unpack", "true");
        	}
        	
        	// For Suomi NPP data, do valid range check AFTER applying scale/offset
        	swathTable.put("range_check_after_scaling", "true");
        	
        	// pass in a GranuleAggregation reader...
        	if (! isVIIRS) {
                if (instrumentName.getStringValue().equals("ATMS")) {
            		adapters[pIdx] = new SwathAdapter(nppAggReader, swathTable);
            		adapterCreated = true;
            		SpectrumAdapter sa = new SpectrumAdapter(nppAggReader, spectTable);
                    DataCategory.createCategory("MultiSpectral");
                    categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
                	MultiSpectralData msd = new MultiSpectralData((SwathAdapter) adapters[pIdx], sa, 
                		"BrightnessTemperature", "BrightnessTemperature", "SuomiNPP", "ATMS");
                	msd.setInitialWavenumber(JPSSUtilities.ATMSChannelCenterFrequencies[0]);
                	multiSpectralData.add(msd);
                } 
                if (instrumentName.getStringValue().equals("CrIS")) {
                	if (pStr.contains(crisFilter)) {
	            		adapters[pIdx] = new CrIS_SDR_SwathAdapter(nppAggReader, swathTable);
	            		adapterCreated = true;
	            		CrIS_SDR_Spectrum csa = new CrIS_SDR_Spectrum(nppAggReader, spectTable);
	                    DataCategory.createCategory("MultiSpectral");
	                    categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
	                	MultiSpectralData msd = new CrIS_SDR_MultiSpectralData((CrIS_SDR_SwathAdapter) adapters[pIdx], csa); 
	                    msd.setInitialWavenumber(csa.getInitialWavenumber());
	                    msd_CrIS.add(msd);
                	}
                }
                if (instrumentName.getStringValue().contains("OMPS")) {
            		adapters[pIdx] = new SwathAdapter(nppAggReader, swathTable);
            		adapterCreated = true;
            		SpectrumAdapter sa = new SpectrumAdapter(nppAggReader, spectTable);
                    DataCategory.createCategory("MultiSpectral");
                    categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
                	MultiSpectralData msd = new MultiSpectralData((SwathAdapter) adapters[pIdx], sa, 
                		"RadianceEarth", "RadianceEarth", "SuomiNPP", "OMPS");
			// TJJ Jul 2023 - below numbers are just a first guess to get OMPS from J2 working
                        msd.setInitialWavenumber(85);
			float[] ompsRange = new float[2];
			ompsRange[0] = 0;
			ompsRange[1] = 400;
			msd.setDataRange(ompsRange);
                	multiSpectralData.add(msd);
                } 
                if (pIdx == 0) {
                	// generate default subset for ATMS and OMPS
                	if (! instrumentName.getStringValue().equals("CrIS")) {
                		defaultSubset = multiSpectralData.get(pIdx).getDefaultSubset();
                	}
                }
                
        	} else {
        		// setting NOAA-format units
        		String varName = pStr.substring(pStr.indexOf(SEPARATOR_CHAR) + 1);
        		String varShortName = pStr.substring(pStr.lastIndexOf(SEPARATOR_CHAR) + 1);
                String units = stProfile.getUnits(varShortName);

                // setting NASA-format and Enterprise EDR units
        		if (! isNOAA) {
                    if (isEnterprise) {
                        units = unitsEnterprise.get(varShortName);
                    } else {
                        units = unitsNASA.get(varShortName);
                        // Need to set _BT variables manually, since they are created on the fly
                        if (varShortName.endsWith("_BT")) units = "Kelvin";
                    }
        		}
        		if (units == null) units = "Unknown";
        		Unit u = null;
        		try {
					u = Parser.parse(units);
                    logger.debug("Found units: " + units);
				} catch (NoSuchUnitException e) {
					u = new DerivedUnit(units);
					logger.debug("Unknown units: " + units);
				} catch (ParseException e) {
					u = new DerivedUnit(units);
					logger.debug("Unparseable units: " + units);
				}
        		// associate this variable with these units, if not done already
        		RealType.getRealType(varName, u);
        		adapters[pIdx] = new SwathAdapter(nppAggReader, swathTable);
        		adapterCreated = true;
        		if (pIdx == 0) {
        			defaultSubset = adapters[pIdx].getDefaultSubset();
        		}
        		categories = DataCategory.parseCategories("IMAGE");
        	}
        	// only increment count if we created an adapter, some products are skipped
    		if (adapterCreated) pIdx++;
    		adapterCreated = false;
    	}

    	if (msd_CrIS.size() > 0) {
    		try {
    			MultiSpectralAggr aggr = new MultiSpectralAggr(msd_CrIS.toArray(new MultiSpectralData[msd_CrIS.size()]));
    			aggr.setInitialWavenumber(902.25f);
    			multiSpectralData.add(aggr);
    			defaultSubset = ((MultiSpectralData) msd_CrIS.get(0)).getDefaultSubset();
    		} catch (Exception e) {
    			logger.error("Exception: ", e);
    		}
    	}

    	// Merge with pre-set properties
    	Hashtable tmpHt = getProperties();
    	tmpHt.putAll(properties);
    	setProperties(tmpHt);
    }

    /* (non-Javadoc)
     * @see ucar.unidata.data.DataSourceImpl#initDataChoice(ucar.unidata.data.DataChoice)
     */
    @Override
    public void initDataChoice(DataChoice dataChoice) {
        super.initDataChoice(dataChoice);
        // Note here if we are a derived data choice
        if (dataChoice instanceof DerivedDataChoice) {
            isDerived = true;
        }
    }

    public void initAfterUnpersistence() {
    	try {
            String zidvPath = 
                    McIDASV.getStaticMcv().getStateManager().
                    getProperty(IdvPersistenceManager.PROP_ZIDVPATH, "");
    	    if (getTmpPaths() != null) {
    	        // New code for zipped bundles-
    	        // we want 'sources' to point to wherever the zipped data was unpacked.
    	        sources.clear();
    	        // following PersistenceManager.fixBulkDataSources, get temporary data location
    	        for (Object o : getTmpPaths()) {
    	            String tempPath = (String) o;
    	            // replace macro string with actual path
    	            String expandedPath = tempPath.replace(PersistenceManager.MACRO_ZIDVPATH, zidvPath);
    	            // we don't want to add nav files to this list!:
    	            File f = new File(expandedPath);
    	            if (!f.getName().matches(JPSSUtilities.SUOMI_GEO_REGEX_NOAA)) {
    	                sources.add(expandedPath);
    	            }
    	        }

                // mjh fix absolute paths in filenameMap
                logger.debug("original filenameMap: {}", filenameMap);
                Iterator<String> keyIterator = filenameMap.keySet().iterator();
                while (keyIterator.hasNext()) {
                    String keyStr = (String) keyIterator.next();
                    List<String> fileNames = (List<String>) filenameMap.get(keyStr);
                    for (int i = 0; i < fileNames.size(); i++) {
                        String name = fileNames.get(i);
                        int lastSeparator = name.lastIndexOf(File.separatorChar);
                        String sub = name.substring(0, lastSeparator);
                        name = name.replace(sub, zidvPath);
                        fileNames.set(i, name);
                    }
                }
                logger.debug("filenameMap with zidvPath: {}", filenameMap);
    	    } else {
    	        // leave in original unpersistence code - this will get run for unzipped bundles.
    	        // TODO: do we need to handle the "Save with relative paths" case specially?
        	    if (! oldSources.isEmpty()) {
                    sources.clear();
                    for (Object o : oldSources) {
                        sources.add((String) o);
                    }
                }
    	    }
    	    oldSources.clear();
    		setup();
    	} catch (Exception e) {
    		logger.error("Exception: ", e);
    	}
    }

    /* (non-Javadoc)
	 * @see edu.wisc.ssec.mcidasv.data.HydraDataSource#canSaveDataToLocalDisk()
	 */
	@Override
	public boolean canSaveDataToLocalDisk() {
		// At present, Suomi data is always data granules on disk
		return true;
	}

	/* (non-Javadoc)
	 * @see ucar.unidata.data.DataSourceImpl#saveDataToLocalDisk(java.lang.String, java.lang.Object, boolean)
	 */
	@Override
	protected List saveDataToLocalDisk(String filePrefix, Object loadId,
			boolean changeLinks) throws Exception {
		// need to make a list of all data granule files
		// PLUS all geolocation granule files, but only if accessed separate!
		List<String> fileList = new ArrayList<String>();
		for (Object o : sources) {
			fileList.add((String) o);
		}
		for (String s : geoSources) {
			fileList.add(s);
		}
		return fileList;
	}

	public List<String> getOldSources() {
		return oldSources;
	}

	public void setOldSources(List<String> oldSources) {
		this.oldSources = oldSources;
	}
    
    public Map<String, List<String>> getFilenameMap() {
        return filenameMap;
    }

    public void setFilenameMap(Map<String, List<String>> filenameMap) {
        this.filenameMap = filenameMap;
    }

	/**
     * Make and insert the {@link DataChoice DataChoices} for this
     * {@code DataSource}.
     */
    
    public void doMakeDataChoices() {
    	
    	// special loop for CrIS, ATMS, and OMPS data
    	if (multiSpectralData.size() > 0) {
    		for (int k = 0; k < multiSpectralData.size(); k++) {
    			MultiSpectralData adapter = multiSpectralData.get(k);
    			DataChoice choice = null;
				try {
					choice = doMakeDataChoice(k, adapter);
					choice.setObjectProperty(Constants.PROP_GRANULE_COUNT, 
							getProperty(Constants.PROP_GRANULE_COUNT, "1 Granule"));
	    			msdMap.put(choice.getName(), adapter);
	    			addDataChoice(choice);
				} catch (Exception e) {
					logger.error("Exception: ", e);
				}
    		}
    		return;
    	}
    	    	
    	// all other data (VIIRS and 2D EDRs)
    	if (adapters != null) {
    		for (int idx = 0; idx < adapters.length; idx++) {
    			DataChoice choice = null;
    			try {
    				Map<String, Object> metadata = adapters[idx].getMetadata();
					String description = null;
    				if (metadata.containsKey("_mapping")) {
						String arrayName = metadata.get("array_name").toString();
    					Map<String, String> mapping =
							(Map<String, String>)metadata.get("_mapping");
						description = mapping.get(arrayName);
					}
					choice = doMakeDataChoice(idx, adapters[idx].getArrayName(), description);
    				choice.setObjectProperty(Constants.PROP_GRANULE_COUNT, 
							getProperty(Constants.PROP_GRANULE_COUNT, "1 Granule")); 
    			} 
    			catch (Exception e) {
    				logger.error("doMakeDataChoice failed", e);
    			}

    			if (choice != null) {
    				addDataChoice(choice);
    			}
    		}
    	}
    }

    private DataChoice doMakeDataChoice(int idx, String var, String description) throws Exception {
        String name = var;
        if (description == null) {
            description = name;
        }
        DataSelection dataSel = new MultiDimensionSubset(defaultSubset);
        Hashtable dcSubset = new Hashtable();
        dcSubset.put(new MultiDimensionSubset(), dataSel);
        // TJJ Hack check for uber-odd case of data type varies for same variable
        // If it's M12 - M16, it's a BrightnessTemperature, otherwise Reflectance
        if (name.endsWith("BrightnessTemperatureOrReflectance")) {
        	name = name.substring(0, name.length() - "BrightnessTemperatureOrReflectance".length());
        	if (whichEDR.matches("M12|M13|M14|M15|M16")) {
        		name = name + "BrightnessTemperature";
        	} else {
        		name = name + "Reflectance";
        	}
        }
        DirectDataChoice ddc = new DirectDataChoice(this, idx, name, description, categories, dcSubset);
        return ddc;
    }
    
    private DataChoice doMakeDataChoice(int idx, MultiSpectralData adapter) throws Exception {
        String name = adapter.getName();
        DataSelection dataSel = new MultiDimensionSubset(defaultSubset);
        Hashtable dcSubset = new Hashtable();
        dcSubset.put(MultiDimensionSubset.key, dataSel);
        dcSubset.put(MultiSpectralDataSource.paramKey, adapter.getParameter());
        // TJJ Hack check for uber-odd case of data type varies for same variable
        // If it's M12 - M16, it's a BrightnessTemperature, otherwise Reflectance
        if (name.endsWith("BrightnessTemperatureOrReflectance")) {
        	name = name.substring(0, name.length() - "BrightnessTemperatureOrReflectance".length());
        	if (whichEDR.matches("M12|M13|M14|M15|M16")) {
        		name = name + "BrightnessTemperature";
        	} else {
        		name = name + "Reflectance";
        	}
        }
        DirectDataChoice ddc = new DirectDataChoice(this, Integer.valueOf(idx), name, name, categories, dcSubset);
        ddc.setProperties(dcSubset);
        return ddc;
    }

    /**
     * Check to see if this {@code SuomiNPPDataSource} is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    
    public boolean equals(Object o) {
        if ( !(o instanceof SuomiNPPDataSource)) {
            return false;
        }
        return (this == (SuomiNPPDataSource) o);
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

	/**
	 * @return the qfMap
	 */
	public Map<String, QualityFlag> getQfMap() {
		return qfMap;
	}

	public void setDatasetName(String name) {
      filename = name;
    }

    /**
     * Determine if this data source originated from a
	 * {@literal "NOAA file"}.
     *
     * @return {@code true} if file came from NOAA, {@code false} otherwise.
     */
    public boolean isNOAA() {
        return isNOAA;
    }

    public Map<String, double[]> getSubsetFromLonLatRect(MultiDimensionSubset select, GeoSelection geoSelection) {
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
            if (! isDerived) {
                geoSelection = (dataSelection.getGeoSelection().getBoundingBox() != null) ?
                    dataSelection.getGeoSelection() :
                    dataChoice.getDataSelection().getGeoSelection();
            } else {
                geoSelection = dataSelection.getGeoSelection();
            }
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
        	if (dc.getName().equals(dataChoice.getName())) {
        		aIdx = dcl.indexOf(dc);
        		break;
        	}
        }

        adapter = adapters[aIdx];

        try {

            if (ginfo != null) {
            	subset = adapter.getSubsetFromLonLatRect(ginfo.getMinLat(), ginfo.getMaxLat(),
            			ginfo.getMinLon(), ginfo.getMaxLon(),
            			geoSelection.getXStride(),
            			geoSelection.getYStride(),
            			geoSelection.getZStride());
            }
            else {

              MultiDimensionSubset select = null;
              Hashtable table = null;
              if (dataChoice instanceof DerivedDataChoice) {
                  List<DataChoice> children = ((DerivedDataChoice) dataChoice).getChoices();
                  DataChoice dc = children.get(0);
                  table = dc.getProperties();
              } else {
                  table = dataChoice.getProperties();
              }
              Enumeration keys = table.keys();
              while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                // TJJ - need a better way here to determine when a new, non-derived product has been selected
                if (key.toString().equals("Use_Display_Driver_Times")) {
                    isDerived = false;
                    derivedInit = false;
                }
                if (key instanceof MultiDimensionSubset) {
                  select = (MultiDimensionSubset) table.get(key);
                }
              }  
              subset = select.getSubset();
              if ((dataSelection != null) && (dataSelection.getGeoSelection() != null)) {
                 if (isDerived) {
                    // Only need to set this once
                    if (! derivedInit) {
                       derivedSubset = subset;
                       derivedInit = true;
                    } else {
                       subset = derivedSubset;
                    }
                 }
              }

              if (dataSelection != null) {
                Hashtable props = dataSelection.getProperties();
                if (props != null) {
                  if (props.containsKey(SpectrumAdapter.channelIndex_name)) {
                	  logger.debug("Props contains channel index key...");
                    double[] coords = subset.get(SpectrumAdapter.channelIndex_name);
                    int idx = ((Integer) props.get(SpectrumAdapter.channelIndex_name)).intValue();
                    coords[0] = (double) idx;
                    coords[1] = (double) idx;
                    coords[2] = (double) 1;
                  }
                }
              }
            }

            if (subset != null) {
                // TJJ Feb 2021 - For derived products, we want to use the same subset for all
                // contributing bands
                data = adapter.getData(subset);
                data = applyProperties(data, requestProperties, subset, aIdx);
            }
        } catch (Exception e) {
            logger.error("getData Exception: ", e);
        }
        ////////// inq1429 return FieldImpl with time dim /////////////////
        if (data != null) {
        	List dateTimes = new ArrayList();
        	dateTimes.add(new DateTime(theDate));
        	SampledSet timeSet = (SampledSet) ucar.visad.Util.makeTimeSet(dateTimes);
        	FunctionType ftype = new FunctionType(RealType.Time, data.getType());
        	FieldImpl fi = new FieldImpl(ftype, timeSet);
        	fi.setSample(0, data);
        	data = fi;
        }
        //////////////////////////////////////////////////////////////////
        return data;
    }

    protected Data applyProperties(Data data, Hashtable requestProperties, Map<String, double[]> subset, int adapterIndex)
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
      
		  try {
			  // inq1429: need to handle FieldImpl here
              FieldImpl thing = null;
              if (dataChoice instanceof DerivedDataChoice) {
                  List<DataChoice> children = ((DerivedDataChoice) dataChoice).getChoices();
                  DataChoice dc = children.get(0);
                  thing = (FieldImpl) dc.getData(null);
              } else {
                  thing = (FieldImpl) dataChoice.getData(null);
              }
			  FlatField image;
			  if (GridUtil.isTimeSequence(thing)) {
				  image = (FlatField) thing.getSample(0);
			  } else {
				  image = (FlatField) thing;
			  }
			  if (image != null) {
			      // For derived data choices, pull out 1st in list for preview
			      PreviewSelection ps = null;
			      if (dataChoice instanceof DerivedDataChoice) {
			          isDerived = true;
			          List<DataChoice> children = ((DerivedDataChoice) dataChoice).getChoices();
			          DataChoice dc = children.get(0);
			          ps = new PreviewSelection(dc, image, null);
			      } else {
			          ps = new PreviewSelection(dataChoice, image, null);
			      }
				  // Region subsetting not yet implemented for CrIS data
				  if (instrumentName.getStringValue().equals("CrIS")) {
					  ps.enableSubsetting(false);
				  }
				  components.add(ps);
			  }
		  } catch (Exception e) {
			  logger.error("Can't make PreviewSelection: ", e);
		  }
      
    }
    
    /**
     * Add {@code Integer->String} translations to IDV's
     * {@literal "translations"} resource, so they will be made available to
     * the data probe of Image Display's.
     */
    public void initQfTranslations() {
    	
        Map<String, Map<Integer, String>> translations =
                getIdv().getResourceManager().
                getTranslationsHashtable();
        
        for (String qfKey : qfMap.keySet()) {
        	// This string needs to match up with the data choice name:
        	String qfKeySubstr = qfKey.replace("All_Data/", "");
        	// check if we've already added map for this QF
        	if (!translations.containsKey(qfKeySubstr)) {
	        	Map<String, String> hm = qfMap.get(qfKey).getHm();
	        	Map<Integer, String> newMap = 
	        			new HashMap<Integer, String>(hm.size());
	        	for (String dataValueKey : hm.keySet()) {
	        		// convert Map<String, String> to Map<Integer, String>
	        		Integer intKey = Integer.parseInt(dataValueKey);
	        		newMap.put(intKey, hm.get(dataValueKey));
	        	}
	        	translations.put(qfKeySubstr, newMap);
        	}
        }
    }
    
}
