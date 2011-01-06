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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

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

public class NPPProductProfile {
	
	private static final Logger logger = LoggerFactory.getLogger(NPPProductProfile.class);
	
	DocumentBuilder db = null;
	HashMap<String, String> rangeMin = new HashMap<String, String>();
	HashMap<String, String> rangeMax = new HashMap<String, String>();
	HashMap<String, ArrayList<Float>> fillValues = new HashMap<String, ArrayList<Float>>();

	public NPPProductProfile() throws ParserConfigurationException, SAXException, IOException {

        logger.trace("NPPProductProfile init...");
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
	
	public String getProfileFileName(String pathStr, String attrName) {
		// sanity check
		if ((pathStr == null) || (attrName == null)) return null;
		
		File profileDir = new File(pathStr);
		if (profileDir.isDirectory()) {
			String fileList[] = profileDir.list();
			for (int i = 0; i < fileList.length; i++) {
				logger.trace("Looking for XMLPP match, file: " + fileList[i]);
				if (fileList[i].contains(attrName)) {
					return fileList[i];
				}
			}
		}
		return null;
	}
	
	public void addMetaDataFromFile(String fileName) throws SAXException, IOException {
		logger.trace("Attempting to parse XML Product Profile: " + fileName);
		Document d = null;
		d = db.parse(fileName);
		NodeList nl = d.getElementsByTagName("Field");
		for (int i = 0; i < nl.getLength(); i++) {
			ArrayList<Float> fValAL = new ArrayList<Float>();
			Node n = nl.item(i);
			NodeList children = n.getChildNodes();
			NodeList datum = null;
			String name = null;
			
			// cycle through once, finding name and making sure it's a valid NPP Product name
			boolean isValidProduct = false;
			for (int j = 0; j < children.getLength(); j++) {
				Node child = children.item(j);
				logger.trace("looking at node name: " + child.getNodeName());
				if (child.getNodeName().equals("Name")) {
					name = child.getTextContent();
					logger.trace("Found NPP product name: " + name);
					isValidProduct = JPSSUtilities.isValidNPPProduct(name);
				}
				if (child.getNodeName().equals("Datum")) {
					datum = child.getChildNodes();
					logger.trace("Found Datum node");
				}
			}
			
			String rMin = null;
			String rMax = null;		
			
			if (isValidProduct) {
				for (int j = 0; j < datum.getLength(); j++) {
					Node child = datum.item(j);
					if (child.getNodeName().equals("RangeMin")) {
						rMin = child.getTextContent();
					}
					if (child.getNodeName().equals("RangeMax")) {
						rMax = child.getTextContent();
					}
					if (child.getNodeName().equals("FillValue")) {
						// go one level further to child element Value
						NodeList grandChildren = child.getChildNodes();
						for (int k = 0; k < grandChildren.getLength(); k++) {
							Node grandChild = grandChildren.item(k);
							if (grandChild.getNodeName().equals("Value")) {
								String fillValueStr = grandChild
										.getTextContent();
								fValAL.add(new Float(Float
										.parseFloat(fillValueStr)));
							}
						}
					}
				}
			} else {
				name = null;
			}
			
			if ((name != null) && (rMin != null)) {
				logger.trace("Adding range min: " + rMin + " for product: " + name);
				rangeMin.put(name, rMin);
			}
			
			if ((name != null) && (rMax != null)) {
				logger.trace("Adding range max: " + rMax + " for product: " + name);
				rangeMax.put(name, rMax);
			}
			
			if ((name != null) && (! fValAL.isEmpty())) {
				logger.trace("Adding fill value array for product: " + name);
				fillValues.put(name, fValAL);
			}
			
		}
	}

	public String getRangeMin(String name) {
		return rangeMin.get(name);
	}
	
	public String getRangeMax(String name) {
		return rangeMax.get(name);
	}
	
	public ArrayList<Float> getFillValues(String name) {
		return fillValues.get(name);
	}

}
