package edu.wisc.ssec.mcidasv;

import java.rmi.RemoteException;
import java.util.List;

import edu.wisc.ssec.mcidasv.ui.UIManager;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.util.LogUtil;
import visad.VisADException;

public class McIDASV extends IntegratedDataViewer {

	public static final String RSC_SERVERS = "";
	
    /**
     * Create the McIdasV with the given command line arguments.
     * This constructor calls {@link IntegratedDataViewer#init()}
     *
     * @param args Command line arguments
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     *
     */
    public McIDASV(String[] args) throws VisADException, RemoteException {
        super(args);
        init();
    }


    /**
     *  Add in our properties. This is the first part  of the bootstrap
     * initializatio process.  The properties file contains a property
     * that defines any other property files to be loaded in
     * Then the idv looks at the property:<pre>
     * idv.resourcefiles </pre>
     * to find out where the rbi files are located. These  rbi files
     * define where all of the various and sundry resources exist. In this
     * example we use our own rbi file: example.rbi
     *
     * @param files List of property files
     */
    public void initPropertyFiles(List files) {
        //The files list contains the default system 
        //properties (idv.properties)
        //We want to completely clobber it to use our own
        //If we just wanted to add ours on top of the system we just
        //don't clear the list. Note the path here is a java resource, i.e.,
        //it is found from the classpath (either on dsk or in a jar file).
        //In  general whenever we specify some path (e.g., for properties,
        //for resources) the path can either be a java resource, a 
        //file system path or a url



        files.clear();
        files.add("/edu/wisc/ssec/mcidasv/resources/mcidasv.properties");

    }



    /**
     * Factory method to create the
     * {@link IdvUIManager}. Here we create our own ui manager
     * so it can do McV specific things.
     *
     * @return The UI manager
     */
    protected IdvUIManager doMakeIdvUIManager() {
        return new UIManager(getIdv());
    }



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







