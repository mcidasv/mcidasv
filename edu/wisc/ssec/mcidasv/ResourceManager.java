package edu.wisc.ssec.mcidasv;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.StringUtil;

/**
 * @author McIdas-V Team
 * @version $Id$
 */
public class ResourceManager extends IdvResourceManager {

	public ResourceManager(IntegratedDataViewer idv) {
		super(idv);
	}

	/**
	 * Adds support for McIdas-V macros.
	 * 
	 * @see ucar.unidata.idv.IdvResourceManager#getResourcePath(java.lang.String)
	 */
	public String getResourcePath(String path) {
		String retPath = path;
		if (path.contains(Constants.MACRO_VERSION)) {
			retPath = StringUtil.replace(
				path, 
				Constants.MACRO_VERSION, 
				((StateManager) getStateManager()).getMcIdasVersion()
			);
		} else {
			retPath = super.getResourcePath(path);
		}
		return retPath;
	}
}
