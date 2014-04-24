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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.geoloc.projection.UtmProjection;

public class EnviInfo extends HeaderInfo {

	/** The url */
	private String dataFile = "";
	private boolean isEnvi = false;
	private boolean hasBounds = false;
	private static final Logger logger = LoggerFactory.getLogger(EnviInfo.class);
	
	// Map Info header field indices 
	// See www.exelisvis.com/docs/ENVIHeaderFiles.html 
	// for a description of this information
	enum MapInfoIndex {
		
		MAP_INFO_IDX_PROJ_NAME(0), 
		MAP_INFO_IDX_X_REF(1), 
		MAP_INFO_IDX_Y_REF(2),
		MAP_INFO_IDX_EASTING(3),
		MAP_INFO_IDX_NORTHING(4),
		MAP_INFO_IDX_X_SIZE(5),
		MAP_INFO_IDX_Y_SIZE(6),
		MAP_INFO_IDX_ZONE(7),
		MAP_INFO_IDX_N_OR_S(8),
		MAP_INFO_IDX_DATUM(9),
		MAP_INFO_IDX_UNITS(10);
		
		private final int index;
		
		private MapInfoIndex(int idx) {
			index = idx;
		}
		
		public int getIndex() {
			return index;
		}
	}

	/**
	 * Ctor for xml encoding
	 */
	public EnviInfo() {}

	/**
	 * CTOR
	 *
	 * @param thisFile File to use.
	 */
	public EnviInfo(File thisFile) {
		this(thisFile.getAbsolutePath());
	}
	
	/**
	 * CTOR
	 *
	 * @param filename The filename
	 */
	public EnviInfo(String filename) {
		super(filename);
		this.dataFile = filename.replace(".hdr", ".img");
	}

	/**
	 * Is the file an ENVI header file?
	 */
	public boolean isEnviHeader() {
		parseHeader();
		return isEnvi;
	}

	/**
	 * Can we find a matching ENVI data file?
	 */
	public boolean hasEnviData() {
		File testFile = new File(dataFile);
		if (testFile.exists()) return true;
		else return false;
	}

	/**
	 * Is this a navigation header file?
	 */
	public boolean isNavHeader() {
		parseHeader();
		List bandNames = (List) getParameter(BANDNAMES, new ArrayList());
		if (bandNames == null) return false;
		if (bandNames.contains("Latitude") && bandNames.contains("Longitude")) return true;
		return false;
	}

	/**
	 * Which band/file is latitude?
	 */
	public int getLatBandNum() {
		parseHeader();
		List bandNames = (List) getParameter(BANDNAMES, new ArrayList());
		for (int i=0; i<bandNames.size(); i++) {
			if (bandNames.get(i).equals("Latitude")) return i+1;
		}
		return -1;
	}
	
	public String getLatBandFile() {
		parseHeader();
		List bandFiles = (List) getParameter(BANDFILES, new ArrayList());
		int bandNum = getLatBandNum();
		if (bandNum < 0) return "";
		return (String)(bandFiles.get(bandNum-1));
	}

	/**
	 * Which band/file is longitude?
	 */
	public int getLonBandNum() {
		parseHeader();
		List bandNames = (List) getParameter(BANDNAMES, new ArrayList());
		for (int i=0; i<bandNames.size(); i++) {
			if (bandNames.get(i).equals("Longitude")) return i+1;
		}
		return -1;
	}
	
	public String getLonBandFile() {
		parseHeader();
		List bandFiles = (List) getParameter(BANDFILES, new ArrayList());
		int bandNum = getLonBandNum();
		if (bandNum < 0) return "";
		return (String)(bandFiles.get(bandNum-1));
	}

	/**
	 * Return a FlatField representing the data
	 */
//	public FlatField getDataField() {
//
//	}

	/**
	 * Return a Gridded2DSet representing navigation
	 */
//	public Gridded2DSet getNavField() {
//
//	}

	/**
	 * @return the hasBounds
	 */
	public boolean isHasBounds() {
		return hasBounds;
	}

	/**
	 * @param hasBounds the hasBounds to set
	 */
	public void setHasBounds(boolean hasBounds) {
		this.hasBounds = hasBounds;
	}

	/**
	 * Parse a potential ENVI header file
	 */
	
	protected void parseHeader() {
		if (haveParsed()) return;
		if (!doesExist()) {
			isEnvi = false;
			return;
		}

		try {
			BufferedReader br = new BufferedReader(new FileReader(getFilename()));
			String line;
			String parameter = "";
			String value = "";
			boolean inValue = false;

			List<String> bandNames = new ArrayList<String>();
			List bandFiles = new ArrayList();

			while ((line = br.readLine()) != null) {
				if (line.trim().equals("ENVI")) {
					isEnvi = true;
					continue;
				}
				if (!isEnvi) break;

				int indexOfEquals = line.indexOf("=");
				int indexOfOpen = line.indexOf("{");
				int indexOfClose = line.indexOf("}");
				if (indexOfEquals >= 0) {
					parameter = line.substring(0, indexOfEquals).trim();
					value = "";
					inValue = false;
				}
				if (indexOfOpen >= 0) {
					if (indexOfClose >= 0) {
						value += line.substring(indexOfOpen+1, indexOfClose).trim();
						inValue = false;
					}
					else {
						value += line.substring(indexOfOpen+1).trim();
						inValue = true;
						continue;
					}
				}
				else if (inValue) {
					if (indexOfClose >= 0) {
						value += line.substring(0, indexOfClose).trim();
						inValue = false;
					}
					else {
						value += line.trim();
						continue;
					}
				}
				else {
					value += line.substring(indexOfEquals+1).trim();
				}

				if (parameter.equals("")) continue;

				if (parameter.equals("description")) {
					setParameter(DESCRIPTION, value);
				}
				
				// TJJ Apr 2014
				// NOTE: method signatures in parent class should be modified or extended
				// I had to pass in an Integer object here in order to be able to retrieve
				// anything other than default values later (both "lines" and "samples")
				
				else if (parameter.equals("samples")) {
					setParameter(ELEMENTS, new Integer(value));
				}
				else if (parameter.equals("lines")) {
					setParameter(LINES, new Integer(value));
				}
				
				else if (parameter.equals("header offset")) {
					setParameter(OFFSET, Integer.parseInt(value));
				}
				else if (parameter.equals("data type")) {
					setParameter(DATATYPE, Integer.parseInt(value));
				}
				else if (parameter.equals("data ignore value") ||
						parameter.equals("bad value")) {
					setParameter(MISSINGVALUE, Float.parseFloat(value));    			
				}
				else if (parameter.equals("interleave")) {
					setParameter(INTERLEAVE, value.toUpperCase());
				}
				else if (parameter.equals("map info")) {
					logger.debug("Parsing Map Info, value: " + value);
					
					ArrayList<String> mapInfo = new ArrayList<String>();
					String[] mapInfoSplit = value.split(",");
					for (int i = 0; i < mapInfoSplit.length; i++) {
						mapInfo.add(mapInfoSplit[i].trim());
					}
					
					// See www.exelisvis.com/docs/ENVIHeaderFiles.html 
					// for a description of this information
					// this code handles UTM files
					
					String projName = mapInfo.get(MapInfoIndex.MAP_INFO_IDX_PROJ_NAME.getIndex());
					
					if (projName.equals("UTM")) {
						
						// zone and hemisphere
						int utmZone = Integer.parseInt(mapInfo.get(MapInfoIndex.MAP_INFO_IDX_ZONE.getIndex()));
						boolean utmN = false;
						if (mapInfo.get(MapInfoIndex.MAP_INFO_IDX_N_OR_S.getIndex()).equals("North")) utmN = true;
						UtmProjection utmp = new UtmProjection(utmZone, utmN);
						
						// Java UTM class default units km, adjust if necessary
						float distFactor = 1.0f;
						if (mapInfo.get(MapInfoIndex.MAP_INFO_IDX_UNITS.getIndex()).contains("Meters")) distFactor = 1000.0f;
						
						// figure out Lat/Lon bounding box from Northing/Easting,
						// resolution, and grid size
						float upperLeftX = Float.parseFloat(mapInfo.get(MapInfoIndex.MAP_INFO_IDX_EASTING.getIndex())) / distFactor;
						float upperLeftY = Float.parseFloat(mapInfo.get(MapInfoIndex.MAP_INFO_IDX_NORTHING.getIndex())) / distFactor;
						
						// lines and samples were already seen
						int numLines = getParameter(LINES, 0);
						int numSamples = getParameter(ELEMENTS, 0);
						
						float xMag = Float.parseFloat(mapInfo.get(MapInfoIndex.MAP_INFO_IDX_X_SIZE.getIndex()));
						float yMag = Float.parseFloat(mapInfo.get(MapInfoIndex.MAP_INFO_IDX_Y_SIZE.getIndex()));
						
						float lowerRightX = upperLeftX + ((numSamples * xMag) / distFactor);
						float lowerRightY = upperLeftY + ((numLines * yMag) / distFactor);
						
						float [][] from = new float[2][2];
						from [0][0] = upperLeftX;
						from [1][0] = upperLeftY;
						from [0][1] = lowerRightX;
						from [1][1] = lowerRightY;
						float [][] to = new float[2][2];
						to = utmp.projToLatLon(from, to);
						
						// Need to check and see if we are correct in assuming which one is upper left
						if (to[0][0] > to[0][1]) {
							setParameter("BOUNDS.ULLAT", "" + to[0][0]);
							setParameter("BOUNDS.ULLON", "" + to[1][0]);
							setParameter("BOUNDS.LRLAT", "" + to[0][1]);
							setParameter("BOUNDS.LRLON", "" + to[1][1]);
						} else {
							from [0][0] = upperLeftX;
							from [1][0] = upperLeftY - ((numLines * yMag) / distFactor);
							from [0][1] = lowerRightX;
							from [1][1] = lowerRightY - ((numLines * yMag) / distFactor);
							to = utmp.projToLatLon(from, to);
							setParameter("BOUNDS.ULLAT", "" + to[0][1]);
							setParameter("BOUNDS.ULLON", "" + to[1][0]);
							setParameter("BOUNDS.LRLAT", "" + to[0][0]);
							setParameter("BOUNDS.LRLON", "" + to[1][1]);
						}
						hasBounds = true;
					}
				}
				else if (parameter.equals("byte order")) {
					boolean bigEndian = false;
					if (value.equals("1")) bigEndian = true;
					setParameter(BIGENDIAN, bigEndian);
				}
				else if (parameter.equals("bands")) {
					if (bandNames.size() <= 0 && bandFiles.size() <= 0) {
						int bandCount = Integer.parseInt(value);
						for (int i=0; i<bandCount; i++) {
							bandNames.add("Band " + i+1);
							bandFiles.add(dataFile);
						}
						setParameter(BANDNAMES, bandNames);
						setParameter(BANDFILES, bandFiles);
					}
				}
				else if (parameter.equals("band names")) {
					bandNames = new ArrayList<String>();
					bandFiles = new ArrayList<String>();
					String[] bandNamesSplit = value.split(",");
					for (int i=0; i<bandNamesSplit.length; i++) {
						bandNames.add(bandNamesSplit[i].trim());
						bandFiles.add(dataFile);
					}
					setParameter(BANDNAMES, bandNames);
					setParameter(BANDFILES, bandFiles);
				}

			}
			br.close();
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}

}
