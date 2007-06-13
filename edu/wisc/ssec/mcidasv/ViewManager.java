package edu.wisc.ssec.mcidasv;

import java.rmi.RemoteException;

import javax.swing.JMenuBar;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.ViewDescriptor;
import visad.VisADException;

/**
 * Class ExampleViewManager derives from the IDV's MapViewManager
 * to do some special example things. This gets created automagically
 * form the example skin.xml file that has a class=example.ExampleViewManager
 * attribute.
 */
public class ViewManager extends MapViewManager {

	/**
	 * Create this view manager.
	 *
	 * @param idv The idv
	 * @param descriptor The descriptor
	 * @param properties Semi-colon delimited list of name=value properties
	 *
	 * @throws RemoteException When bad things happen
	 * @throws VisADException When bad things happen
	 */
	public ViewManager(IntegratedDataViewer idv, ViewDescriptor descriptor,
			String properties) throws VisADException, RemoteException {
		//Just pass thru the args to the base class ctor
		super(idv, descriptor, properties);
	}

	public void init() throws VisADException, RemoteException {
		super.init();
	}

	protected JMenuBar doMakeMenuBar() {
		return super.doMakeMenuBar();
	}


}
