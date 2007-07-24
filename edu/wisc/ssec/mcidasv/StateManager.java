package edu.wisc.ssec.mcidasv;

import java.util.Properties;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

public class StateManager extends ucar.unidata.idv.StateManager {
	
	private String version;
	private String versionAbout; 
	
	public StateManager(IntegratedDataViewer idv) {
		super(idv);
	}
	
	public String getMcIdasVersionAbout() {
		
		getMcIdasVersion();
        
        versionAbout = IOUtil.readContents((String) getProperty(Constants.PROP_ABOUTTEXT), "");
        versionAbout = StringUtil.replace(versionAbout, Constants.MACRO_VERSION, version);
        
		return versionAbout;
	}
	
	public String getMcIdasVersion() {
		if (version != null) {
			return version;
		}
		
		Properties props = new Properties();
		props = Misc.readProperties((String) getProperty(Constants.PROP_VERSIONFILE), null, getClass());
		String maj = props.getProperty(Constants.PROP_VERSION_MAJOR, "0");
		String min = props.getProperty(Constants.PROP_VERSION_MINOR, "0");
		String rel = props.getProperty(Constants.PROP_VERSION_RELEASE, "");
		
		version = maj.concat(".").concat(min).concat(rel);
		
		return version;
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
