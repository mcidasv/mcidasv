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
package edu.wisc.ssec.mcidasv.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class AxformInfo extends HeaderInfo {

	/** The url */
	private String dataFile = "";
	private boolean isAxform = false;

	/**
	 * Ctor for xml encoding
	 */
	public AxformInfo() {}

	/**
	 * CTOR
	 *
	 * @param thisFile File to use.
	 */
	public AxformInfo(File thisFile) {
		this(thisFile.getAbsolutePath());
	}

	/**
	 * CTOR
	 *
	 * @param filename The filename
	 */
	public AxformInfo(String filename) {
		super(filename);
	}

	/**
	 * Is the file an AXFORM header file?
	 */
	public boolean isAxformInfoHeader() {
		parseHeader();
		return isAxform;
	}
	
	/**
	 * Which band/file is latitude?
	 */
	public int getLatBandNum() {
		return 1;
	}
	public String getLatBandFile() {
		parseHeader();
		List bandFiles = (List)getParameter(NAVFILES, new ArrayList());
		if (bandFiles.size()!=2) return "";
		return (String)(bandFiles.get(0));
	}

	/**
	 * Which band/file is longitude?
	 */
	public int getLonBandNum() {
		return 1;
	}
	public String getLonBandFile() {
		parseHeader();
		List bandFiles = (List)getParameter(NAVFILES, new ArrayList());
		if (bandFiles.size()!=2) return "";
		return (String)(bandFiles.get(1));
	}

	/**
	 * Parse a potential AXFORM header file
	 */
	protected void parseHeader() {
		if (haveParsed()) return;
		if (!doesExist()) {
			isAxform = false;
			return;
		}

		try {
			BufferedReader br = new BufferedReader(new FileReader(getFilename()));
			int lineNum = 0;
			String line;
			String description = "";
			boolean gotFiles = false;
			
			List bandNames = new ArrayList();
			List bandFiles = new ArrayList();
			
			String latFile = "";
			String lonFile = "";
			File thisFile = new File(getFilename());
			String parent = thisFile.getParent();
			if (parent==null) parent=".";

			while ((line = br.readLine()) != null) {
				lineNum++;
				if (line.trim().equals("Space Science & Engineering Center")) {
					isAxform = true;
					continue;
				}
				if (!isAxform) break;

				if (line.length() < 80) {
					if (lineNum > 15) gotFiles = true;
					continue;
				}

				// Process the description from lines 5 and 6
				if (lineNum==5) {
					description += line.substring(13, 41).trim();
				}
				else if (lineNum==6) {
					description += " " + line.substring(14, 23).trim() +" " + line.substring(59, 71).trim();
					setParameter(DESCRIPTION, description);
				}

				// Process the file list starting at line 15
				else if (lineNum>=15 && !gotFiles) {
					String parameter = line.substring(0, 13).trim();
					if (parameter.equals("Header")) {
						isAxform = true;
					}
					else if (parameter.equals("Latitude")) {
						latFile = parent + "/" + line.substring(66).trim();
					}
					else if (parameter.equals("Longitude")) {
						lonFile = parent + "/" + line.substring(66).trim();
					}
					else {
						setParameter(LINES, Integer.parseInt(line.substring(24, 31).trim()));
						setParameter(ELEMENTS, Integer.parseInt(line.substring(32, 40).trim()));
						setParameter(UNIT, parameter);
						String band = line.substring(19, 23).trim();
						String format = line.substring(41, 59).trim();
						System.out.println("looking at format line: " + format);
						if (format.indexOf("ASCII") >= 0) {
							setParameter(DATATYPE, kFormatASCII);
						}
						else if (format.indexOf("8 bit") >= 0) {
							setParameter(DATATYPE, kFormat1ByteUInt);
						}
						else if (format.indexOf("16 bit") >= 0) {
							setParameter(DATATYPE, kFormat2ByteSInt);
						}
						else if (format.indexOf("32 bit") >= 0) {
							setParameter(DATATYPE, kFormat4ByteSInt);
						}
						String filename = line.substring(66).trim();
						filename = parent + "/" + filename;
						bandFiles.add(filename);
						bandNames.add("Band " + band);
					}
				}

				// Look for the missing value, bail when you find it
				else if (gotFiles) {
					if (line.indexOf("Navigation files missing data value") >= 0) {
						setParameter(MISSINGVALUE, Float.parseFloat(line.substring(44, 80).trim()));    			
						break;
					}
				}

				setParameter(BANDNAMES, bandNames);
				setParameter(BANDFILES, bandFiles);
				
				List latlonFiles = new ArrayList();
				latlonFiles.add(latFile);
				latlonFiles.add(lonFile);
				setParameter(NAVFILES, latlonFiles);

			}
			br.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}

}
