package edu.wisc.ssec.mcidasv;

import java.rmi.RemoteException;
import java.util.List;

import ucar.unidata.idv.IdvPreferenceManager;
import ucar.unidata.idv.IdvProjectionManager;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.util.LogUtil;
import visad.VisADException;
import edu.wisc.ssec.mcidasv.chooser.McIdasChooserManager;
import edu.wisc.ssec.mcidasv.data.McIDASVProjectionManager;
import edu.wisc.ssec.mcidasv.ui.McIdasColorTableManager;
import edu.wisc.ssec.mcidasv.ui.TabbedUIManager;
import edu.wisc.ssec.mcidasv.ui.UIManager;

@SuppressWarnings("unchecked")
public class McIDASV extends IntegratedDataViewer {

    /** Points to the adde image defaults. */
    public static final IdvResourceManager.XmlIdvResource RSC_FRAMEDEFAULTS =
        new IdvResourceManager.XmlIdvResource("idv.resource.framedefaults",
                           "McIDAS-X Frame Defaults");

    /** Points to the server definitions. */
    public static final IdvResourceManager.XmlIdvResource RSC_SERVERS =
        new IdvResourceManager.XmlIdvResource("idv.resource.servers",
                           "Servers", "servers\\.xml$");

    /** The chooser manager */
    protected McIdasChooserManager chooserManager;
	
    /**
     * Create the McIdasV with the given command line arguments.
     * This constructor calls {@link IntegratedDataViewer#init()}
     *
     * @param args Command line arguments
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     */
    public McIDASV(String[] args) throws VisADException, RemoteException {
        super(args);
        this.init();
    }
    
    /**
     * Load the McV properties. All other property files are disregarded.
     * 
     * @see ucar.unidata.idv.IntegratedDataViewer#initPropertyFiles(java.util.List)
     */
    @Override
	public void initPropertyFiles(List files) {
        files.clear();
        files.add(Constants.PROPERTIES_FILE);
    }


    /**
     * Factory method to create the {@link IdvUIManager}. 
     * Here we create our own ui manager so it can do McV 
     * specific things.
     *
     * @return The UI manager indicated by the startup
     * 		properties.
     */
    @Override
    protected IdvUIManager doMakeIdvUIManager() {
    	
    	if (getIdv().getProperty(Constants.PROP_TABBED_UI, false)) {
			return new TabbedUIManager(getIdv());
		}
    	
        return new UIManager(getIdv());
    }

    /**
     * Make the {@link McIdasPreferenceManager}.
     * @see ucar.unidata.idv.IdvBase#doMakePreferenceManager()
     */
    @Override
    protected IdvPreferenceManager doMakePreferenceManager() {
        return new McIdasPreferenceManager(getIdv());
    }

    /**
     * Make the {@link edu.wisc.ssec.mcidasv.chooser.McIdasChooserManager}.
     * @see ucar.unidata.idv.IdvBase#doMakePreferenceManager()
     */
    protected IdvChooserManager doMakeChooserManager() {
        return new McIdasChooserManager(getIdv());
    }

    /**
     *  Create, if needed,  and return the
     * {@link McIdasChooserManager}
     *
     * @return The Chooser manager
     */
    public McIdasChooserManager getMcIdasChooserManager() {
        if (chooserManager == null) {
            chooserManager = (McIdasChooserManager)doMakeChooserManager();
        }
        return chooserManager;
    }

    /**
     * Get McIDASV. 
     * @see ucar.unidata.idv.IdvBase#getIdv()
     */
    @Override
    public IntegratedDataViewer getIdv() {
    	return this;
    }
    
    /**
     * Make the McIDAS-V {@link StateManager}.
     * @see ucar.unidata.idv.IdvBase#doMakeStateManager()
     */
    @Override
    protected StateManager doMakeStateManager() {
    	return new StateManager(getIdv());
    }
    
    /**
     * Make the McIDAS-V {@link ResourceManager}.
     * @see ucar.unidata.idv.IdvBase#doMakeResourceManager()
     */
    @Override
    protected IdvResourceManager doMakeResourceManager() {
    	return new ResourceManager(getIdv());
    }

    /**
     * Make the {@link edu.wisc.ssec.mcidasv.ui.McIdasColorTableManager}.
     * @see ucar.unidata.idv.IdvBase#doMakeColorTableManager()
     */
    @Override
    protected ColorTableManager doMakeColorTableManager() {
        return new McIdasColorTableManager();
    }
    
//    /**
//     * Make the {@link edu.wisc.ssec.mcidasv.data.McIDASVProjectionManager}.
//     * @see ucar.unidata.idv.IdvBase#doMakeIdvProjectionManager()
//     */
//    @Override
//    protected IdvProjectionManager doMakeIdvProjectionManager() {
//    	return new McIDASVProjectionManager(getIdv());
//    }
    
    /**
     * The main. Configure the logging and create the McIdasV
     *
     * @param args Command line arguments
     *
     * @throws Exception When something untoward happens
     */
    public static void main(String[] args) throws Exception {
        LogUtil.configure();
        new McIDASV(args);
    }
}







