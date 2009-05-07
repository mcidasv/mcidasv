package edu.wisc.ssec.mcidasv.chooser;

import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.PreferenceList;

public class TDSGridChooser extends XmlChooser {

    /**
     * Create the <code>XmlChooser</code>
     *
     * @param mgr The <code>IdvChooserManager</code>
     * @param root  The xml root that defines this chooser
     *
     */
    public TDSGridChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }
	
    public PreferenceList getPreferenceList(String listProp) {
    	return super.getPreferenceList("idv.data.grid.list");
    }
    
}
