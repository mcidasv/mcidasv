/*
 * $Id$
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

public class SuomiNPPProductProfile {
	
	private static final Logger logger = LoggerFactory.getLogger(SuomiNPPProductProfile.class);
	
	DocumentBuilder db = null;
	// if we need to pull product profiles from the McV jar file
	boolean readFromJar = false;
	HashMap<String, String> rangeMin = new HashMap<String, String>();
	HashMap<String, String> rangeMax = new HashMap<String, String>();
	HashMap<String, String> scaleFactorName = new HashMap<String, String>();
	HashMap<String, ArrayList<Float>> fillValues = new HashMap<String, ArrayList<Float>>();

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
					logger.trace("looking at line: " + line);
					if (line.contains(attrName + "-PP")) {
						found = true;
						break;
					}
					line = dirReader.readLine();
				}
				ios.close();
				if (found == true) {
					logger.trace("Found profile: " + attrName);
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
		logger.trace("Attempting to parse XML Product Profile: " + fileName);
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
			Node n = nl.item(i);
			NodeList children = n.getChildNodes();
			NodeList datum = null;
			String name = null;
			
			// cycle through once, finding name and datum node
			for (int j = 0; j < children.getLength(); j++) {
				Node child = children.item(j);
				logger.trace("looking at node name: " + child.getNodeName());
				if (child.getNodeName().equals("Name")) {
					name = child.getTextContent();
					logger.trace("Found Suomi NPP product name: " + name);
				}
				if (child.getNodeName().equals("Datum")) {
					datum = child.getChildNodes();
					logger.trace("Found Datum node");
				}
			}
			
			String rMin = null;
			String rMax = null;		
			String sFactorName = null;	

			if ((name != null) && (datum != null)) {
				for (int j = 0; j < datum.getLength(); j++) {
					Node child = datum.item(j);
					if (child.getNodeName().equals("RangeMin")) {
						rMin = child.getTextContent();
					}
					if (child.getNodeName().equals("RangeMax")) {
						rMax = child.getTextContent();
					}
					if (child.getNodeName().equals("ScaleFactorName")) {
						sFactorName = child.getTextContent();
					}
					if (child.getNodeName().equals("FillValue")) {
						// go one level further to child element Value
						NodeList grandChildren = child.getChildNodes();
						for (int k = 0; k < grandChildren.getLength(); k++) {
							Node grandChild = grandChildren.item(k);
							if (grandChild.getNodeName().equals("Value")) {
								String fillValueStr = grandChild.getTextContent();
								fValAL.add(new Float(Float.parseFloat(fillValueStr)));
							}
						}
					}
				}
			}
			
			if ((name != null) && (rMin != null)) {
				logger.trace("Adding range min: " + rMin + " for product: " + name);
				rangeMin.put(name, rMin);
			}
			
			if ((name != null) && (rMax != null)) {
				logger.trace("Adding range max: " + rMax + " for product: " + name);
				rangeMax.put(name, rMax);
			}
			
			if ((name != null) && (sFactorName != null)) {
				logger.trace("Adding scale factor name: " + sFactorName + " for product: " + name);
				scaleFactorName.put(name, sFactorName);
			}
			
			if ((name != null) && (! fValAL.isEmpty())) {
				logger.trace("Adding fill value array for product: " + name);
				fillValues.put(name, fValAL);
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

}
