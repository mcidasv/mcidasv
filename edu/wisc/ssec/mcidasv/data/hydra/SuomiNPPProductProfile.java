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

package edu.wisc.ssec.mcidasv.data.hydra;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.wisc.ssec.mcidasv.data.QualityFlag;

public class SuomiNPPProductProfile {
	
	private static final Logger logger = LoggerFactory.getLogger(SuomiNPPProductProfile.class);
	
	DocumentBuilder db = null;
	// if we need to pull product profiles from the McV jar file
	boolean readFromJar = false;
	HashMap<String, String> rangeMin = new HashMap<String, String>();
	HashMap<String, String> rangeMax = new HashMap<String, String>();
	HashMap<String, String> units = new HashMap<String, String>();
	HashMap<String, String> scaleFactorName = new HashMap<String, String>();
	HashMap<String, ArrayList<Float>> fillValues = new HashMap<String, ArrayList<Float>>();
	HashMap<String, ArrayList<QualityFlag>> qualityFlags = new HashMap<String, ArrayList<QualityFlag>>();

	public SuomiNPPProductProfile() throws ParserConfigurationException, SAXException, IOException {

        logger.trace("SuomiNPPProductProfile init...");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        db = factory.newDocumentBuilder();
        db.setEntityResolver(new EntityResolver()
        {
            public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException
            {
                return new InputSource(new StringReader(""));
            }
        });

	}
	
	/**
	 * See if for a given N_Collection_Short_Name attribute, the profile is present
	 * @param pathStr the directory the XML Product Profiles reside
	 * @param attrName The attribute name our file should match
	 * @return the full file name for the XML Product Profile
	 */
	
	public String getProfileFileName(String attrName) {
		
		// sanity check
		if (attrName == null) return null;
		
		// Locate the base app JAR file
		File mcvJar = findMcVJar();
		if (mcvJar == null) return null;

		// we need to pull the XML Product Profiles out of mcidasv.jar
		JarFile jar;
		try {
			jar = new JarFile(mcvJar);
			// gives ALL entries in jar
			Enumeration<JarEntry> entries = jar.entries();
			boolean found = false;
			String name = null;
			while (entries.hasMoreElements()) {
				name = entries.nextElement().getName();
				// filter according to the profiles
				if (name.contains("XML_Product_Profiles")) { 
					logger.trace("looking at line: " + name);
					if (name.contains(attrName + "-PP")) {
						found = true;
						break;
					}
				}
			}
			jar.close();
			if (found == true) {
				logger.trace("Found profile: " + name);
				return name;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return null;
	}

	/**
	 * @param mcvJar
	 * @return the File object which for mcidasv.jar, or null if not found
	 */
	
	private File findMcVJar() {
		File mcvJar = null;
		try {
			mcvJar = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException urise) {
			// just log the exception for analysis
			urise.printStackTrace();
		}
		return mcvJar;
	}
	
	@SuppressWarnings("deprecation")
	public void addMetaDataFromFile(String fileName) throws SAXException, IOException {
		
		File mcvJar = findMcVJar();
		if (mcvJar == null) {
			logger.error("Unable to parse Suomi XML Product Profile");
			return;
		}

		Document d = null;
		InputStream ios = null;
		JarFile jar = new JarFile(mcvJar);
		JarEntry je = jar.getJarEntry(fileName);
		ios = jar.getInputStream(je);
		d = db.parse(ios);
		
		NodeList nl = d.getElementsByTagName("Field");
		for (int i = 0; i < nl.getLength(); i++) {
			ArrayList<Float> fValAL = new ArrayList<Float>();
			ArrayList<QualityFlag> qfAL = new ArrayList<QualityFlag>();
			Node n = nl.item(i);
			NodeList children = n.getChildNodes();
			NodeList datum = null;
			String name = null;
			boolean isQF = false;
			// temporary set used to guarantee unique names within quality flag
			HashSet<String> hs = new HashSet<String>();
			int uniqueCounter = 2;
			
			// cycle through once, finding name and datum node(s)
			// NOTE: may be multiple datum notes, e.g. QF quality flags
			for (int j = 0; j < children.getLength(); j++) {
				
				Node child = children.item(j);
				if (child.getNodeName().equals("Name")) {
					name = child.getTextContent();
					logger.debug("Found Suomi NPP product name: " + name);
					if (name.startsWith("QF")) {
						isQF = true;
						uniqueCounter = 2;
					}
				}
				
				if (child.getNodeName().equals("Datum")) {
					
					datum = child.getChildNodes();
					String rMin = null;
					String rMax = null;	
					String unitStr = null;
					String sFactorName = null;	

					if ((name != null) && (datum != null)) {
						
						// if it's a quality flag, do separate loop 
						// and store relevant info in a bean
						if (isQF) {
							QualityFlag qf = null;
							HashMap<String, String> hm = new HashMap<String, String>();
							int bitOffset = -1;
							int numBits = -1;
							String description = null;
							boolean haveOffs = false;
							boolean haveSize = false;
							boolean haveDesc = false;
							for (int k = 0; k < datum.getLength(); k++) {
								Node datumChild = datum.item(k);
								if (datumChild.getNodeName().equals("DatumOffset")) {
									String s = datumChild.getTextContent();
									bitOffset = Integer.parseInt(s);
									haveOffs = true;
								}
								if (datumChild.getNodeName().equals("DataType")) {
									String s = datumChild.getTextContent();
									// we will only handle the bit fields.
									// others cause an exception so just catch and continue
									try {
										numBits = Integer.parseInt(s.substring(0, 1));
									} catch (NumberFormatException nfe) {
										continue;
									}
									haveSize = true;
								}
								if (datumChild.getNodeName().equals("Description")) {
									String s = datumChild.getTextContent();
									// first, special check for "Test" flags, want to 
									// include the relevant bands on those.  Seem to
									// directly follow test name in parens
									boolean isTest = false;
									if (s.contains("Test (")) {
										isTest = true;
										int idx = s.indexOf(")");
										if (idx > 0) {
											description = s.substring(0, idx + 1);
										}
									} else {
										// for non-Test flags, we DO want to
										// lose any ancillary (in parentheses) info
										int endIdx = s.indexOf("(");
										if (endIdx > 0) {
											description = s.substring(0, endIdx);
										} else {
											description = s;
										}
									}
									// another "ancillary info weedout" check, sometimes
									// there is long misc trailing info after " - "
									if ((description.contains(" - ")) && !isTest) {
										int idx = description.indexOf(" - ");
										description = description.substring(0, idx);
										boolean added = hs.add(name + description);
										// if HashSet add fails, it's a dupe name.
										// tack on incrementing digit to make it unique
										if (! added) {
											description = description + "_" + uniqueCounter;
											hs.add(name + description);
											uniqueCounter++;
										}
									}
									// ensure what's left is a valid NetCDF object name
									description= ucar.nc2.iosp.netcdf3.N3iosp.makeValidNetcdf3ObjectName(description);
									// valid name maker sometimes leaves trailing underscore - remove these
									if (description.endsWith("_")) {
										description = description.substring(0, description.length() - 1);
									}
									logger.debug("Final name: " + description);
									haveDesc = true;
								}
								if (datumChild.getNodeName().equals("LegendEntry")) {
									NodeList legendChildren = datumChild.getChildNodes();
									boolean gotName = false;
									boolean gotValue = false;
									String nameStr = null;
									String valueStr = null;
									for (int legIdx = 0; legIdx < legendChildren.getLength(); legIdx++) { 
										Node legendChild = legendChildren.item(legIdx);
										if (legendChild.getNodeName().equals("Name")) {
											nameStr = legendChild.getTextContent();
											gotName = true;
										}
										if (legendChild.getNodeName().equals("Value")) {
											valueStr = legendChild.getTextContent();
											gotValue = true;
										}
									}
									if (gotName && gotValue) {
										hm.put(valueStr, nameStr);
									}
								}
							}
							if (haveOffs && haveSize && haveDesc) {
								qf = new QualityFlag(bitOffset, numBits, description, hm);
								qfAL.add(qf);
							}
						}
						
						for (int k = 0; k < datum.getLength(); k++) {
							
							Node datumChild = datum.item(k);
							if (datumChild.getNodeName().equals("RangeMin")) {
								rMin = datumChild.getTextContent();
							}
							if (datumChild.getNodeName().equals("RangeMax")) {
								rMax = datumChild.getTextContent();
							}
							if (datumChild.getNodeName().equals("MeasurementUnits")) {
								unitStr = datumChild.getTextContent();
							}
							if (datumChild.getNodeName().equals("ScaleFactorName")) {
								sFactorName = datumChild.getTextContent();
							}
							if (datumChild.getNodeName().equals("FillValue")) {
								// go one level further to datumChild element Value
								NodeList grandChildren = datumChild.getChildNodes();
								for (int l = 0; l < grandChildren.getLength(); l++) {
									Node grandChild = grandChildren.item(l);
									if (grandChild.getNodeName().equals("Value")) {
										String fillValueStr = grandChild.getTextContent();
										fValAL.add(new Float(Float.parseFloat(fillValueStr)));
									}
								}
							}
						}
					}
					
					boolean rangeMinOk = false;
					boolean rangeMaxOk = false;
					
					if ((name != null) && (rMin != null)) {
						// make sure the field parses to a numeric value
						try {
							Float.parseFloat(rMin);
							rangeMinOk = true;
						} catch (NumberFormatException nfe) {
							// do nothing, just won't use ranges for this variable
						}
					}
					
					if ((name != null) && (rMax != null)) {
						// make sure the field parses to a numeric value
						try {
							Float.parseFloat(rMax);
							rangeMaxOk = true;
						} catch (NumberFormatException nfe) {
							// do nothing, just won't use ranges for this variable
						}
					}
					
					// only use range if min and max checked out
					if ((rangeMinOk) && (rangeMaxOk)) {
						logger.debug("Adding range min: " + rMin + " for product: " + name);
						logger.debug("Adding range max: " + rMax + " for product: " + name);
						rangeMin.put(name, rMin);
						rangeMax.put(name, rMax);
					}
					
					if ((name != null) && (unitStr != null)) {
						units.put(name, unitStr);
					} else {
						units.put(name, "Unknown");
					}
					
					if ((name != null) && (sFactorName != null)) {
						logger.debug("Adding scale factor name: " + sFactorName + " for product: " + name);
						scaleFactorName.put(name, sFactorName);
					}
					
					if ((name != null) && (! fValAL.isEmpty())) {
						logger.debug("Adding fill value array for product: " + name);
						fillValues.put(name, fValAL);
					}
					
					if ((name != null) && (! qfAL.isEmpty())) {
						logger.debug("Adding quality flags array for product: " + name);
						qualityFlags.put(name, qfAL);
					}
				}
			}
		}
		if (ios != null) {
			try {
				ios.close();
				jar.close();
			} catch (IOException ioe) {
				// do nothing
			}
		}
	}

	/**
	 * Check if this product profile has a product AND metadata
	 * Checking presence of a Range alone is not sufficient.
	 * @param product name
	 * @return true if both conditions met
	 */
	
	public boolean hasNameAndMetaData(String name) {
		if ((rangeMin.containsKey(name) || (fillValues.containsKey(name)))) {
			return true;
		} else {
			return false;
		}
	}
	
	public String getRangeMin(String name) {
		return rangeMin.get(name);
	}
	
	public String getRangeMax(String name) {
		return rangeMax.get(name);
	}
	
	public String getUnits(String name) {
		return units.get(name);
	}
	
	public String getScaleFactorName(String name) {
		return scaleFactorName.get(name);
	}
	
	public ArrayList<Float> getFillValues(String name) {
		return fillValues.get(name);
	}
	
	public ArrayList<QualityFlag> getQualityFlags(String name) {
		return qualityFlags.get(name);
	}

}
