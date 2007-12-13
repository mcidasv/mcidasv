package edu.wisc.ssec.mcidasv;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.PluginManager;

public class McIDASVPluginManager extends PluginManager {

	private IntegratedDataViewer idv;
	
	public McIDASVPluginManager(IntegratedDataViewer idv) {
		super(idv);
		
		this.idv = idv;
	}
	
}
