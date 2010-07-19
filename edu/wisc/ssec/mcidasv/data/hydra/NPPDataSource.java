/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.data.HydraDataSource;
import edu.wisc.ssec.mcidasv.data.PreviewSelection;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.ByteArrayInputStream;
import java.io.File;

import java.net.URL;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

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

import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.Misc;

import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;

import ucar.visad.ProjectionCoordinateSystem;

import ucar.visad.display.DisplayMaster;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.MapLines;
import ucar.visad.display.RubberBandBox;

import visad.CellImpl;
import visad.Data;
import visad.FlatField;
import visad.Gridded2DSet;
import visad.GriddedSet;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.UnionSet;
import visad.VisADException;

import visad.data.mcidas.BaseMapAdapter;

import visad.georef.MapProjection;

/**
 * A data source for NPOESS Preparatory Project (NPP) data
 * This will probably move, but we are placing it here for now
 * since we are leveraging some existing code used for HYDRA.
 */

public class NPPDataSource extends HydraDataSource {

	private static final Logger logger = LoggerFactory.getLogger(NPPDataSource.class);
	
	/** Sources file */
    protected String filename;

    //protected MultiDimensionReader netCDFReader;
    protected MultiDimensionReader nppAggReader;

    protected MultiDimensionAdapter[] adapters = null;

    protected SpectrumAdapter spectrumAdapter;

    private static final String DATA_DESCRIPTION = "NPP Data";

    private HashMap defaultSubset;
    public TrackAdapter track_adapter;
    private MultiSpectralData multiSpectData;

    private List categories;
    private boolean hasImagePreview = true;
    private boolean hasTrackPreview = false;
    private boolean hasChannelSelect = false;
    
    private static int[] YSCAN_POSSIBILITIES = { 96,  512,  768,  1536, 2304, 2313 };
    private static int[] XSCAN_POSSIBILITIES = { 508, 2133, 3200, 6400, 4064, 4121 };    
    private int inTrackDimensionLength = -1;
    
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
        logger.trace("NPPDataSource called, single file selected: " + fileName);
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
                                 List newSources, Hashtable properties)
            throws VisADException {
        super(descriptor, newSources, DATA_DESCRIPTION, properties);
        logger.debug("NPPDataSource constructor called, file count: " + sources.size());

        this.filename = (String) sources.get(0);
        
        this.setName("NPP");
        this.setDescription("NPP");
        
        for (Object o : sources) {
        	logger.debug("NPP source file: " + (String) o);
        }

        try {
        	setup();
        } catch (Exception e) {
        	e.printStackTrace();
        	throw new VisADException();
        }
    }

    public void setup() throws Exception {

    	// looking to populate 3 things - path to lat, path to lon, path to relevant products
    	String pathToLat = null;
    	String pathToLon = null;
    	TreeSet<String> pathToProducts = new TreeSet<String>();
    	
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
    		
    		// for each source file provided get the nominal time
    		for (int fileCount = 0; fileCount < sources.size(); fileCount++) {
	    		// need to open the main NetCDF file to determine the geolocation product
	    		NetcdfFile ncfile = null;
	    		String fileAbsPath = null;
	    		try {
	    			fileAbsPath = (String) sources.get(fileCount);
		    		logger.debug("Trying to open file: " + fileAbsPath);
		    		ncfile = NetcdfFile.open(fileAbsPath);
		    		ucar.nc2.Attribute a = ncfile.findGlobalAttribute("N_GEO_Ref");
		    		logger.debug("Value of GEO global attribute: " + a.getStringValue());
		    		String tmpGeoProductID = null;
	    			if (a.getStringValue().endsWith("h5")) {
	    				tmpGeoProductID = a.getStringValue();
	    			} else {
	    				tmpGeoProductID = JPSSUtilities.mapGeoRefToProductID(a.getStringValue());
	    			}
		    		logger.debug("Value of corresponding Product ID: " + tmpGeoProductID);
		    		geoProductIDs.add(tmpGeoProductID);
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
	    	    				for (Group subG : dpg) {
	    	    					
	    	    					// This is also where we find the attribute which tells us which
	    	    					// XML Product Profile to use!
	    	    					if (nppPP == null) {
	    	    						ucar.nc2.Attribute axpp = subG.findAttribute("N_Collection_Short_Name");
	    	    						if (axpp != null) {
	    	    							System.err.println("XML Product Profile: " + axpp.getStringValue());
	    	    							String baseName = axpp.getStringValue();
	    	    							baseName = baseName.replace('-', '_');
	    	    							String pathStr = fileAbsPath.substring(0, fileAbsPath.lastIndexOf(File.separatorChar) + 1);
	    	    							String productProfileFileName = pathStr + baseName + ".xml";
	    	    							try {
	    	    								nppPP = new NPPProductProfile(productProfileFileName);
	    	    							} catch (Exception nppppe) {
	    	    								nppppe.printStackTrace();
	    	    							}
	    	    						}
	    	    					}
	    	    					
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
	    			logger.debug("Exception during open file: " + fileAbsPath);
	    			e.printStackTrace();
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
    			agg.addContent(fData);
    			agg.addContent(fGeo);
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
    	    							String varPrefix = vName.substring(vName.lastIndexOf(File.separatorChar) + 1);
    	    							varPrefix = varPrefix.substring(0, 3);
    	    							logger.debug("Variable prefix for finding Factors: " + varPrefix);
    	    							DataType dt = v.getDataType();
    	    							if ((dt.getSize() != 4) && (dt.getSize() != 2) && (dt.getSize() != 1)) {
    	    								logger.debug("Skipping data of size: " + dt.getSize());
    	    								continue;
    	    							}
    	    							List al = v.getAttributes();

    	    							List<Dimension> dl = v.getDimensions();
    	    							if (dl.size() > 2) {
    	    								logger.debug("Skipping data of dimension: " + dl.size());
    	    								continue;
    	    							}
    	    							boolean xScanOk = false;
    	    							boolean yScanOk = false;
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
    	    							}
    	    							
    	    							if (xScanOk && yScanOk) {
    	    								useThis = true;
    	    							}
    	    							
    	    							if (useThis) { 
    	    								// loop through the variable list again, looking for a corresponding "Factors"
    	    								float scaleVal = 1f;
    	    								float offsetVal = 0f;
    	    								boolean unpackFlag = false;
    	    								
    	    								// XXX TJJ - this is NOT DETERMINISTIC!  The spec in
    	    								// CDFCB-X, Vol 5, page 8, is too vague, and there is
    	    								// no SURE way to map variable name to scale/offset parameter
    	    								// We have found the pseudocode below works for all sample data
    	    								// seen so far.
    	    								//
    	    								// for variable list
    	    								//   if name contains 3-char prefix for this variable, and ends with Factors
    	    								//     get the data, data1 = scale, data2 = offset
    	    								//     create and poke attributes with this data
    	    								//   endif
    	    								// endfor
    	    								
    	    								for (Variable fV : vl) {
    	    									if ((fV.getName().contains(varPrefix)) && (fV.getName().endsWith("Factors"))) {
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

    	    								// poke in scale/offset attributes for now

    	    								ucar.nc2.Attribute a1 = new ucar.nc2.Attribute("scale_factor", scaleVal);
    	    								v.addAttribute(a1);
    	    								ucar.nc2.Attribute a2 = new ucar.nc2.Attribute("add_offset", offsetVal);
    	    								v.addAttribute(a2);  
    	    								
    	    								// add valid range and fill value attributes here
    	    					        	// try to fill in valid range
    	    					        	if (nppPP != null) {
    	    					        		String translatedName = JPSSUtilities.mapProdNameToProfileName(vName.substring(vName.lastIndexOf(File.separatorChar) + 1));
    	    					        		logger.debug("mapped name: " + translatedName);
    	    					        		if (translatedName != null) {
    	    					        			String rangeMin = nppPP.getRangeMin(translatedName);
    	    					        			String rangeMax = nppPP.getRangeMax(translatedName);
    	    					        			logger.debug("range min: " + rangeMin);
    	    					        			logger.debug("range max: " + rangeMax);
    	    					        			int [] shapeArr = new int [] { 2 };
    	    					        			ArrayFloat af = new ArrayFloat(shapeArr);
    	    					        			af.setFloat(0, Float.parseFloat(rangeMin));
    	    					        			af.setFloat(1, Float.parseFloat(rangeMax));
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
	        	    												fillValAsFloat = (float) RangeProcessor.unsignedByteToInt(n.byteValue());
	        	    											}
	        	    											else if (fvdt.getPrimitiveClassType() == short.class) {
	        	    												fillValAsFloat = (float) RangeProcessor.unsignedShortToInt(n.shortValue());
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
    		e.printStackTrace();
    		logger.error("cannot create NetCDF reader for files selected");
    	}
    	
    	// initialize the aggregation reader object
    	nppAggReader = new GranuleAggregation(ncdfal, inTrackDimensionLength, 0);

    	// make sure we found valid data
    	if (pathToProducts.size() == 0) {
    		throw new VisADException("No data found in files selected");
    	}
    	
    	logger.debug("Number of adapters needed: " + pathToProducts.size());
    	adapters = new MultiDimensionAdapter[pathToProducts.size()];
    	Hashtable<String, String[]> properties = new Hashtable<String, String[]>(); 
    	
    	Iterator iterator = pathToProducts.iterator();
    	int pIdx = 0;
    	while (iterator.hasNext()) {
    		String pStr = (String) iterator.next();
    		logger.debug("Working on adapter number " + (pIdx + 1));
        	HashMap table = SwathAdapter.getEmptyMetadataTable();
        	table.put("array_name", pStr);
        	table.put("array_dimension_names", new String[] {"Track", "XTrack"});
        	table.put("lon_array_name", pathToLon);
        	table.put("lat_array_name", pathToLat);
        	table.put("lon_array_dimension_names", new String[] {"Track", "XTrack"});
        	table.put("lat_array_dimension_names", new String[] {"Track", "XTrack"});
        	table.put("XTrack", "XTrack");
        	table.put("Track", "Track");
        	table.put("geo_Track", "Track");
        	table.put("geo_XTrack", "XTrack");
        	table.put("scale_name", "scale_factor");
        	table.put("offset_name", "add_offset");
        	table.put("fill_value_name", "_FillValue");
        	logger.info("Setting range_name to: " + pStr.substring(pStr.lastIndexOf(File.separatorChar) + 1));
        	table.put("range_name", pStr.substring(pStr.lastIndexOf(File.separatorChar) + 1));
        	
        	// set the valid range hash if data is available
        	if (nppPP != null) {
        		String translatedName = JPSSUtilities.mapProdNameToProfileName(pStr.substring(pStr.lastIndexOf(File.separatorChar) + 1));
        		if (translatedName != null) {
        			table.put("valid_range", "valid_range");
        		}
        	}
        	
        	String unsignedAttributeStr = unsignedFlags.get(pIdx);
        	if (unsignedAttributeStr.equals("true")) {
        		table.put("unsigned", unsignedAttributeStr);
        	}
        	
        	String unpackFlagStr = unpackFlags.get(pIdx);
        	if (unpackFlagStr.equals("true")) {
        		table.put("unpack", "true");
        	}
        	
        	// pass in a GranuleAggregation reader...
        	adapters[pIdx] = new SwathAdapter(nppAggReader, table);
    		pIdx++;
    	}

    	categories = DataCategory.parseCategories("2D grid;GRID-2D;");
    	defaultSubset = adapters[0].getDefaultSubset();

    	setProperties(properties);
    }

    public void initAfterUnpersistence() {
    	try {
    		setup();
    	} 
    	catch (Exception e) {
    	}
    }

    /**
     * Make and insert the <code>DataChoice</code>-s for this
     * <code>DataSource</code>.
     */
    
    public void doMakeDataChoices() {
    	DataChoice choice = null;
    	if (adapters != null) {
    		for (int idx = 0; idx < adapters.length; idx++) {
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
      return multiSpectData;
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
                if (key instanceof MultiDimensionSubset) {
                  select = (MultiDimensionSubset) table.get(key);
                }
              }  
              subset = select.getSubset();

              if (dataSelection != null) {
                Hashtable props = dataSelection.getProperties();
                if (props != null) {
                  if (props.containsKey(SpectrumAdapter.channelIndex_name)) {
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

      if (requestProperties.containsKey("medianFilter")) {
        String[] items = (String[]) requestProperties.get("medianFilter");
        double window_lenx = Double.parseDouble(items[0]);
        double window_leny = Double.parseDouble(items[1]);
        GriddedSet domainSet = (GriddedSet) ((FlatField)data).getDomainSet();
        int[] lens = domainSet.getLengths();
        float[] range_values = (((FlatField)data).getFloats())[0];
        range_values =
           ProfileAlongTrack.medianFilter(range_values, lens[0], lens[1],
                               (int)window_lenx, (int)window_leny);
        ((FlatField)new_data).setSamples(new float[][] {range_values});
      }
      if (requestProperties.containsKey("setBelowSfcMissing")) {
        String[] items = (String[]) requestProperties.get("setBelowSfcMissing");
        FlatField track = (FlatField) track_adapter.getData(subset);
        float[] sfcElev = (track.getFloats())[0];
        FlatField field = (FlatField) new_data;
        GriddedSet gset = (GriddedSet) field.getDomainSet();
        float[][] samples = gset.getSamples(false);
        int[] lens = gset.getLengths();
        float[] range_values = (field.getFloats())[0];

        int trkIdx = ((ProfileAlongTrack3D) adapters[adapterIndex]).adapter2D.getTrackTupIdx();
        int vrtIdx = ((ProfileAlongTrack3D) adapters[adapterIndex]).adapter2D.getVertTupIdx();

        int k = 0;
        for (int j=0; j<lens[trkIdx]; j++) {
          float val = sfcElev[j];
          for (int i=0; i<lens[vrtIdx]; i++) {
            if (vrtIdx < trkIdx) k = i + j*lens[0];
            if (trkIdx < vrtIdx) k = j + i*lens[0];
            if (samples[2][k] <= val || samples[2][k] < 0.0) {
              range_values[k] = Float.NaN;
            }
          }
        }
        field.setSamples(new float[][] {range_values});
      }
      return new_data;
    }

    protected void initDataSelectionComponents(
         List<DataSelectionComponent> components,
             final DataChoice dataChoice) {

      if (hasImagePreview) {
        try {
          FlatField image = (FlatField) dataChoice.getData(null);
          components.add(new PreviewSelection(dataChoice, image, null));
        } catch (Exception e) {
          logger.error("Can't make PreviewSelection: "+e);
          e.printStackTrace();
        }
      }
      if (hasTrackPreview) {
        try {
          FlatField track = track_adapter.getData(track_adapter.getDefaultSubset());
          components.add(new NPPTrackSelection(dataChoice, track));
        } catch (Exception e) {
          logger.error("Can't make PreviewSelection: "+e);
          e.printStackTrace();
        }
      }
      if (hasChannelSelect) {
        try {
          components.add(new NPPChannelSelection(dataChoice));
        } 
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
}

class NPPChannelSelection extends DataSelectionComponent {

	private static final Logger logger = LoggerFactory.getLogger(NPPChannelSelection.class);
	DataChoice dataChoice;
	MultiSpectralDisplay display;

	NPPChannelSelection(DataChoice dataChoice) throws Exception {
		super("Channels");
		this.dataChoice = dataChoice;
		display = new MultiSpectralDisplay((DirectDataChoice)dataChoice);
		display.showChannelSelector();
	}

	protected JComponent doMakeContents() {
		try {
			JPanel panel = new JPanel(new BorderLayout());
			panel.add("Center", display.getDisplayComponent());
			return panel;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	public void applyToDataSelection(DataSelection dataSelection) {
		try {
			dataSelection.putProperty(Constants.PROP_CHAN, display.getWaveNumber());
			dataSelection.putProperty(SpectrumAdapter.channelIndex_name, display.getChannelIndex());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}


class NPPTrackSelection extends DataSelectionComponent {
	
	private static final Logger logger = LoggerFactory.getLogger(NPPTrackSelection.class);
      DataChoice dataChoice;
      FlatField track;

      double[] x_coords = new double[2];
      double[] y_coords = new double[2];
      boolean hasSubset = true;
      MapProjectionDisplayJ3D mapProjDsp;
      DisplayMaster dspMaster;

      int trackStride;
      int verticalStride;

      JTextField trkStr;
      JTextField vrtStr;


   NPPTrackSelection(DataChoice dataChoice, FlatField track) throws VisADException, RemoteException {
        super("track");
        this.dataChoice = dataChoice;
        this.track = track;
        mapProjDsp = new MapProjectionDisplayJ3D(MapProjectionDisplay.MODE_2Din3D);
        mapProjDsp.enableRubberBanding(false);
        dspMaster = mapProjDsp;
        mapProjDsp.setMapProjection(getDataProjection());
        LineDrawing trackDsp = new LineDrawing("track");
        trackDsp.setLineWidth(2f);
        trackDsp.setData(track);
        mapProjDsp.addDisplayable(trackDsp);


        MapLines mapLines  = new MapLines("maplines");
        URL      mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPU");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception e) {
            logger.error("Can't open map file " + mapSource);
            e.printStackTrace();
        }
                                                                                                                                                     
        mapLines  = new MapLines("maplines");
        mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLSUPW");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception e) {
            logger.error("Can't open map file " + mapSource);
            e.printStackTrace();
        }
                                                                                                                                                     
        mapLines  = new MapLines("maplines");
        mapSource =
        mapProjDsp.getClass().getResource("/auxdata/maps/OUTLHPOL");
        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.cyan);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception e) {
            logger.error("Can't open map file " + mapSource);
            e.printStackTrace();
        }

        final LineDrawing selectBox = new LineDrawing("select");
        selectBox.setColor(Color.green);

        final RubberBandBox rbb =
            new RubberBandBox(RealType.Longitude, RealType.Latitude, 1);
        rbb.setColor(Color.green);
        rbb.addAction(new CellImpl() {
          public void doAction()
             throws VisADException, RemoteException
           {
              Gridded2DSet set = rbb.getBounds();
              float[] low = set.getLow();
              float[] hi = set.getHi();
              x_coords[0] = low[0];
              x_coords[1] = hi[0];
              y_coords[0] = low[1];
              y_coords[1] = hi[1];
              
              SampledSet[] sets = new SampledSet[4];
              sets[0] = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, new float[][] {{low[0], hi[0]}, {low[1], low[1]}}, 2);
              sets[1] = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, new float[][] {{hi[0], hi[0]}, {low[1], hi[1]}}, 2);
              sets[2] = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, new float[][] {{hi[0], low[0]}, {hi[1], hi[1]}}, 2);
              sets[3] = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, new float[][] {{low[0], low[0]}, {hi[1], low[1]}}, 2);
              UnionSet uset = new UnionSet(sets);
              selectBox.setData(uset);
           }
        });
        dspMaster.addDisplayable(rbb);
        dspMaster.addDisplayable(selectBox);

        dspMaster.draw();
   }

       public MapProjection getDataProjection() {
         MapProjection mp = null;
         try {
           mp = new ProjectionCoordinateSystem(new LatLonProjection());
         } catch (Exception e) {
             logger.error(" getDataProjection"+e);
         }
         return mp;
       }

      protected JComponent doMakeContents() {
        try {
          JPanel panel = new JPanel(new BorderLayout());
          panel.add("Center", dspMaster.getDisplayComponent());

          JPanel stridePanel = new JPanel(new FlowLayout());
          trkStr = new JTextField(Integer.toString(5), 3);
          vrtStr = new JTextField(Integer.toString(2), 3);
          trkStr.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ae) {
                setTrackStride(Integer.valueOf(trkStr.getText().trim()));
              }
          });
          vrtStr.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ae) {
                setVerticalStride(Integer.valueOf(vrtStr.getText().trim()));
              }
          });

          stridePanel.add(new JLabel("track stride: "));
          stridePanel.add(trkStr);
          stridePanel.add(new JLabel("vertical stride: "));
          stridePanel.add(vrtStr);
          panel.add("South", stridePanel);

          return panel;
        }
        catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
                                                                                                                                                     
      public void setTrackStride(int stride) {
        trackStride = stride;
      }

      public void setVerticalStride(int stride) {
        verticalStride = stride;
      }

      public void setTrackStride() {
        trackStride = Integer.valueOf(trkStr.getText().trim());
      }

      public void setVerticalStride() {
        verticalStride = Integer.valueOf(vrtStr.getText().trim());
      }

      public void applyToDataSelection(DataSelection dataSelection) {
         setTrackStride();
         setVerticalStride();
         if (hasSubset) {
           GeoSelection geoSelect = new GeoSelection(
                new GeoLocationInfo(y_coords[1], x_coords[0], y_coords[0], x_coords[1]));
           geoSelect.setXStride(trackStride);
           geoSelect.setYStride(verticalStride);
           dataSelection.setGeoSelection(geoSelect);
         }
      }
}
