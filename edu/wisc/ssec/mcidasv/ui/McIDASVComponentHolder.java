package edu.wisc.ssec.mcidasv.ui;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.IdvComponentHolder;

/**
 * This currently does nothing whatsoever, but I can anticipate this being 
 * needed in the future. If you want to flesh this out, be sure to change the
 * references in McIDASVXmlUi to IdvComponentHolder to McIDASVComponentHolder.
 */
public class McIDASVComponentHolder extends IdvComponentHolder {
	public McIDASVComponentHolder(IntegratedDataViewer idv, Object obj) {
		super(idv, obj);
	}
}
