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

import java.io.IOException;
import java.io.StringReader;
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
	
	Document d = null;
	HashMap<String, String> rangeMin = new HashMap<String, String>();
	HashMap<String, String> rangeMax = new HashMap<String, String>();

	public NPPProductProfile(String fileName) throws ParserConfigurationException, SAXException, IOException {

        logger.trace("NPPProductProfile, file name: " + fileName);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder db = null;
        db = factory.newDocumentBuilder();
        db.setEntityResolver(new EntityResolver()
        {
            public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException
            {
                return new InputSource(new StringReader(""));
            }
        });

		d = db.parse(fileName);
		NodeList nl = d.getElementsByTagName("Datum");
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			NodeList children = n.getChildNodes();
			String name = null;
			String rMin = null;
			String rMax = null;
			for (int j = 0; j < children.getLength(); j++) {
				Node child = children.item(j);
				if (child.getNodeName().equals("RangeMin")) {
					rMin = child.getTextContent();
				}
				if (child.getNodeName().equals("RangeMax")) {
					rMax = child.getTextContent();
				}
				if (child.getNodeName().equals("Name")) {
					name = child.getTextContent();
				}
			}
			if ((name != null) && (rMin != null)) {
				rangeMin.put(name, rMin);
			}
			if ((name != null) && (rMax != null)) {
				rangeMax.put(name, rMax);
			}
		}
	}
	
	public String getRangeMin(String name) {
		return rangeMin.get(name);
	}
	
	public String getRangeMax(String name) {
		return rangeMax.get(name);
	}

}
