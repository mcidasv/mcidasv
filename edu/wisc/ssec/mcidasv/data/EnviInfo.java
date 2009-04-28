package edu.wisc.ssec.mcidasv.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class EnviInfo extends HeaderInfo {

	/** The url */
	private String dataFile = "";
	private boolean isEnvi = false;

	/**
	 * Ctor for xml encoding
	 */
	public EnviInfo() {}

	/**
	 * CTOR
	 *
	 * @param filename The filename
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
		List bandNames = (List)getParameter(BANDNAMES, new ArrayList());
		if (bandNames == null) return false;
		if (bandNames.contains("Latitude") && bandNames.contains("Longitude")) return true;
		return false;
	}

	/**
	 * Which band/file is latitude?
	 */
	public int getLatBandNum() {
		parseHeader();
		List bandNames = (List)getParameter(BANDNAMES, new ArrayList());
		for (int i=0; i<bandNames.size(); i++) {
			if (bandNames.get(i).equals("Latitude")) return i+1;
		}
		return -1;
	}
	public String getLatBandFile() {
		parseHeader();
		List bandFiles = (List)getParameter(BANDFILES, new ArrayList());
		int bandNum = getLatBandNum();
		if (bandNum < 0) return "";
		return (String)(bandFiles.get(bandNum-1));
	}

	/**
	 * Which band/file is longitude?
	 */
	public int getLonBandNum() {
		parseHeader();
		List bandNames = (List)getParameter(BANDNAMES, new ArrayList());
		for (int i=0; i<bandNames.size(); i++) {
			if (bandNames.get(i).equals("Longitude")) return i+1;
		}
		return -1;
	}
	public String getLonBandFile() {
		parseHeader();
		List bandFiles = (List)getParameter(BANDFILES, new ArrayList());
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

			List bandNames = new ArrayList();
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
				else if (parameter.equals("samples")) {
					setParameter(ELEMENTS, Integer.parseInt(value));
				}
				else if (parameter.equals("lines")) {
					setParameter(LINES, Integer.parseInt(value));
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
					bandNames = new ArrayList();
					bandFiles = new ArrayList();
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
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}

}
