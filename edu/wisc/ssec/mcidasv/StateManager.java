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
		URL url;
		URLConnection urlc;
		DataInputStream inputStream = null;
		String version="";
		
		try {
			url = new URL(Constants.HOMEPAGE_URL + "/" + Constants.VERSION_URL);
			urlc = url.openConnection();
			urlc.setUseCaches(false);
			InputStream is = urlc.getInputStream();
			inputStream = new DataInputStream(new BufferedInputStream(is));
		} catch (Exception e) {
			System.out.println("Version check failed: " + e);
			try { inputStream.close(); }
			catch (Exception ee) {}
			return version;
		}
			
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
    	try {
    		version = br.readLine();
        } catch (Exception e) {
            System.out.println("Version check failed: " + e);
			try { br.close(); }
			catch (Exception ee) {}
        }
		try {
			inputStream.close();
			br.close();
		}
		catch (Exception ee) {}
        
        return version;
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

		return value;
	}
	
	public void checkForNewerVersion(boolean notifyIfCurrent) {
		String thisVersion = getMcIdasVersion();
		String thatVersion = getMcIdasVersionLatest();
		if (compareVersions(thisVersion, thatVersion) > 0) {
			if ( !GuiUtils.showYesNoDialog(null,
					"Version " + thatVersion + " is available for download.\n\nDo you wish to visit the McIDAS-V web page now?\n\n" + Constants.HOMEPAGE_URL,
					"New Version Available")) {
				System.out.println("You clicked no");
			}
			else {
				System.out.println("You clicked yes");
			}
		}
		else {
			if (notifyIfCurrent) {
				System.out.println("This version (" + thisVersion + ") is up to date");
			}
		}
	}

}
