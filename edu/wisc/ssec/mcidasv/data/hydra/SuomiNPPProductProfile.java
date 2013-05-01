/*
 * $Id: SuomiNPPProductProfile.java,v 1.2 2012/02/19 17:35:42 davep Exp $
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2012
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
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
		
		// print top level classloader files
		// this is how we will tell if we need to pull the profiles out of a jar file
		
		readFromJar = false;
        ClassLoader loader = getClass().getClassLoader();
        InputStream in = loader.getResourceAsStream(".");
        BufferedReader rdr = new BufferedReader(new InputStreamReader(in));
        String l;
        try {
			while ((l = rdr.readLine()) != null) {
			    if (l.equals("mcidasv.jar")) {
			    	readFromJar = true;
			    }
			}
			rdr.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		// we need to pull the XML Product Profiles out of mcidasv.jar
		if (readFromJar) {
			JarFile jar;
			try {
				jar = new JarFile(URLDecoder.decode("mcidasv.jar", "UTF-8"));
				Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
				boolean found = false;
				String name = null;
				while (entries.hasMoreElements()) {
					name = entries.nextElement().getName();
					if (name.contains("XML_Product_Profiles")) { // filter according to the profiles
						logger.trace("looking at line: " + name);
						if (name.contains(attrName + "-PP")) {
							found = true;
							break;
						}
					}
				}
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
		} else {
			// read contents of XML Product Profile directory as stream
			InputStream ios = getClass().getResourceAsStream("resources/NPP/XML_Product_Profiles/");
			BufferedReader dirReader = new BufferedReader(new InputStreamReader(ios));
			try {
				String line = dirReader.readLine();
				boolean found = false;
				while (line != null) {
					if (line.contains(attrName + "-PP")) {
						found = true;
						break;
					}
					line = dirReader.readLine();
				}
				ios.close();
				if (found == true) {
					logger.info("Found profile: " + attrName);
					return line;
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return null;
			}
		}

		return null;
	}
	
	public void addMetaDataFromFile(String fileName) throws SAXException, IOException {
		
		logger.debug("Attempting to parse XML Product Profile: " + fileName);
		Document d = null;
		InputStream ios = null;
		if (readFromJar) {
			JarFile jar = new JarFile(URLDecoder.decode("mcidasv.jar", "UTF-8"));
			JarEntry je = jar.getJarEntry(fileName);
			ios = jar.getInputStream(je);
			d = db.parse(ios);
		} else {
			ios = getClass().getResourceAsStream("resources/NPP/XML_Product_Profiles/" + fileName);
			d = db.parse(ios);
		}
		
		NodeList nl = d.getElementsByTagName("Field");
		for (int i = 0; i < nl.getLength(); i++) {
			ArrayList<Float> fValAL = new ArrayList<Float>();
			ArrayList<QualityFlag> qfAL = new ArrayList<QualityFlag>();
			Node n = nl.item(i);
			NodeList children = n.getChildNodes();
			NodeList datum = null;
			String name = null;
			boolean isQF = false;
			
			// cycle through once, finding name and datum node(s)
			// NOTE: may be multiple datum notes, e.g. QF quality flags
			for (int j = 0; j < children.getLength(); j++) {
				
				Node child = children.item(j);
				if (child.getNodeName().equals("Name")) {
					name = child.getTextContent();
					logger.info("Found Suomi NPP product name: " + name);
					if (name.startsWith("QF")) {
						isQF = true;
					}
				}
				
				if (child.getNodeName().equals("Datum")) {
					
					datum = child.getChildNodes();
					String rMin = null;
					String rMax = null;		
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
									if (s.contains("Test (")) {
										int idx = s.indexOf(")");
										if (idx > 0) {
											description = s.substring(0, idx + 1);
										}
									} else {
										// lose any ancillary (in parentheses) info
										int endIdx = s.indexOf("(");
										if (endIdx > 0) {
											description = s.substring(0, endIdx);
										} else {
											description = s;
										}
									}
									// another "ancillary info weedout" check, sometimes
									// there is long trailing info after " - "
									if (description.contains(" - ")) {
										int idx = description.indexOf(" - ");
										description = description.substring(0, idx);
									}
									// Now, crunch out any whitespace
									description = description.replaceAll("\\s+", "");
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
					
					if ((name != null) && (rMin != null)) {
						logger.info("Adding range min: " + rMin + " for product: " + name);
						rangeMin.put(name, rMin);
					}
					
					if ((name != null) && (rMax != null)) {
						logger.info("Adding range max: " + rMax + " for product: " + name);
						rangeMax.put(name, rMax);
					}
					
					if ((name != null) && (sFactorName != null)) {
						logger.info("Adding scale factor name: " + sFactorName + " for product: " + name);
						scaleFactorName.put(name, sFactorName);
					}
					
					if ((name != null) && (! fValAL.isEmpty())) {
						logger.info("Adding fill value array for product: " + name);
						fillValues.put(name, fValAL);
					}
					
					if ((name != null) && (! qfAL.isEmpty())) {
						logger.info("Adding quality flags array for product: " + name);
						qualityFlags.put(name, qfAL);
					}
				}
			}
		}
		if (ios != null) {
			try {
				ios.close();
			} catch (IOException ioe) {
				// do nothing
			}
		}
	}

	/**
	 * Check if this product profile has a product AND metadata
	 * Only need to check one of the possible fields
	 * @param product name
	 * @return true if both conditions met
	 */
	
	public boolean hasNameAndMetaData(String name) {
		if (rangeMin.containsKey(name)) {
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
