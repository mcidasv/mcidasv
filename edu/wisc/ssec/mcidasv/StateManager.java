/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

public class StateManager extends ucar.unidata.idv.StateManager implements Constants {
	
	private String version;
	private String versionAbout;
	
	
	public StateManager(IntegratedDataViewer idv) {
		super(idv);
	}
	
	public String getMcIdasVersionAbout() {
		
		getMcIdasVersion();
        
        versionAbout = IOUtil.readContents((String) getProperty(Constants.PROP_ABOUTTEXT), "");
        versionAbout = StringUtil.replace(versionAbout, MACRO_VERSION, version);
        Properties props = Misc.readProperties(
        	(String) getProperty(Constants.PROP_VERSIONFILE), 
        	null, 
        	getClass()
        );
        
        String value = getIdvVersion();
        versionAbout = StringUtil.replace(versionAbout, Constants.MACRO_IDV_VERSION, value);
        value = props.getProperty(PROP_COPYRIGHT_YEAR, "");
        versionAbout = StringUtil.replace(versionAbout, Constants.MACRO_COPYRIGHT_YEAR, value);
        value = props.getProperty(PROP_BUILD_DATE, "Unknown");
        versionAbout = StringUtil.replace(versionAbout, Constants.MACRO_BUILDDATE, value);
       
		return versionAbout;
	}
	
	public String getMcIdasVersion() {
		if (version != null) {
			return version;
		}
		
		Properties props = new Properties();
		props = Misc.readProperties((String) getProperty(Constants.PROP_VERSIONFILE), null, getClass());
		String maj = props.getProperty(PROP_VERSION_MAJOR, "0");
		String min = props.getProperty(PROP_VERSION_MINOR, "0");
		String rel = props.getProperty(PROP_VERSION_RELEASE, "");
		
		version = maj.concat(".").concat(min).concat(rel);
		
		return version;
	}
	
	public String getIdvVersion() {
		return getVersion();
	}
	
	/**
	 * Overridden to get dir of the unnecessary second level directory.
	 * 
	 * @see ucar.unidata.idv.StateManager#getStoreName()
	 */
	public String getStoreName() {
		return "";
	}
	
	/**
	 * Connect to McIDAS website and look for latest version
	 */
	public String getMcIdasVersionLatest() {
		String version = "";
		try {
			version = IOUtil.readContents(Constants.HOMEPAGE_URL + "/" + Constants.VERSION_URL + "?requesting=" + getMcIdasVersion(), "");
		} catch (Exception e) {}
		return version.trim();
	}
	
	/**
	 * Compare version strings
	 *  0: equal
	 * <0: this version is greater
	 * >0: that version is greater
	 */
	private int compareVersions(String thisVersion, String thatVersion) {
		int thisInt = versionToInteger(thisVersion);
		int thatInt = versionToInteger(thatVersion);
		return (thatInt - thisInt);
	}
	
	/**
	 * Turn version strings of the form #.#(a#)
	 *  where # is one or two digits, a is one of alpha or beta, and () is optional
	 * Into an integer... (empty) > beta > alpha
	 */
	private int versionToInteger(String version) {
		int value = 0;
		int p;
		String part;
		Character one = null;
		
		try {
			
			// Major version
			p = version.indexOf('.');
			if (p > 0) {
				part = version.substring(0,p);
				value += Integer.parseInt(part) * 1000000;
				version = version.substring(p+1);
			}
			
			// Minor version
			int i=0;
			for (i=0; i<2 && i<version.length(); i++) {
				one = version.charAt(i);
				if (Character.isDigit(one)) {
					if (i==0) value += Character.digit(one, 10) * 100000;
					else value += Character.digit(one, 10) * 10000;
				}
				else {
					break;
				}
			}
			if (one!=null) version = version.substring(i);
	
			// Alpha/beta status
			if (version.length() == 0) value += 300;
			else if (version.charAt(0) == 'b') value += 200;
			else if (version.charAt(0) == 'a') value += 100;
			for (i=0; i<version.length(); i++) {
				one = version.charAt(i);
				if (Character.isDigit(one)) break;
			}
			if (one!=null) version = version.substring(i);
	
			// Alpha/beta version
			if (version.length() > 0)
				value += Integer.parseInt(version);
			
		} catch (Exception e) {}

		return value;
	}
	
	public void checkForNewerVersion(boolean notifyDialog) {
		
		/** Shortcut this whole process if we are processing offscreen */
		if (super.getIdv().getArgsManager().getIsOffScreen())
			return;

		String thisVersion = getMcIdasVersion();
		String thatVersion = getMcIdasVersionLatest();
		String titleText = "Version Check";
		
		if (thisVersion.equals("") || thatVersion.equals("")) {
			if (notifyDialog) {
				JOptionPane.showMessageDialog(null, "Version check failed", titleText, 
						JOptionPane.WARNING_MESSAGE);
			}
		}
		else if (compareVersions(thisVersion, thatVersion) > 0) {
			String labelText = "<html>Version <b>" + thatVersion + "</b> is available<br><br>";
			labelText += "Visit <a href=\"" + Constants.HOMEPAGE_URL + "\">";
			labelText += Constants.HOMEPAGE_URL + "</a> to download</html>";
			JLabel message = new JLabel(labelText, JLabel.CENTER);
			JOptionPane.showMessageDialog(null, message, titleText, 
					JOptionPane.INFORMATION_MESSAGE);
		}
		else {
			if (notifyDialog) {
				String labelText = "<html>This version (<b>" + thisVersion + "</b>) is up to date</html>";
				JLabel message = new JLabel(labelText, JLabel.CENTER);
				JOptionPane.showMessageDialog(null, message, titleText, 
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
		
	}

}
