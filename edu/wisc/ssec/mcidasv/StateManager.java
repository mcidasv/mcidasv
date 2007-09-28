package edu.wisc.ssec.mcidasv;

import java.util.Properties;

import ucar.unidata.idv.IntegratedDataViewer;
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

}
