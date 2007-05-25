package ucar.unidata.idv.chooser.mcidas;


import ucar.unidata.idv.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import ucar.unidata.idv.IdvResourceManager.XmlIdvResource;
import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.ResourceCollection;

import ucar.unidata.xml.XmlUtil;
import ucar.unidata.xml.XmlResourceCollection;

import org.w3c.dom.Element;

import ucar.unidata.data.imagery.ImageDataset;

import ucar.unidata.ui.imagery.AddeImageChooser;
import ucar.unidata.ui.imagery.mcidas.TestAddeImageChooser;


/**
 * Test new type of image chooser
 */


public class TestImageChooser extends IdvChooser {

    private List defaultChoosers = new ArrayList();

    /** _more_ */
    IntegratedDataViewer idv;

    /** _more_ */
    public static final String TAG_TYPE = "type";

    /** _more_ */
    public static final String TAG_SERVER = "server";

    /** _more_ */
    public static final String ATTR_NAME = "name";
    public static final String ATTR_GROUP = "group";

    public List typeList = new ArrayList();
    public List serverList = new ArrayList();

    /** This really does the work */
    private TestAddeImageChooser imageChooser;


    /*
     * Make a new one
     *
     * @param mgr The manager
     * @param root The xml element that defined this object
     *
     */
    public TestImageChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }

    /**
     * Handle the update event. Just pass it through to the imageChooser
     */
    public void doUpdate() {
        imageChooser.doUpdate();
    }

    /**
     * Make the GUI
     *
     * @return The GUI
     */
    protected JComponent doMakeContents() {
        imageChooser = doMakeImageChooser();
        initChooserPanel(imageChooser);
        imageChooser.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals(
                        TestAddeImageChooser.NEW_SELECTION)) {
                    TestImageChooser.this.doLoad();
                }

            }
        });
        return imageChooser.getContents();
    }

    /**
     * Make the chooser. This is a hook so a derived class
     * can make its own chooser
     *
     * @return The {@link ucar.unidata.ui.imagery.AddeImageChooser} to pass
     * to the imageChooser.
     */
    protected TestAddeImageChooser doMakeImageChooser() {
        XmlResourceCollection serversXRC = getServers();
        return new TestAddeImageChooser(this, serversXRC, getImageDefaults(),
                                    getServerPreferenceList(serversXRC)) {
            public void doCancel() {
                closeChooser();
            }
        };
    }

    /**
     * _more_
     *
     */
    private PreferenceList getServerPreferenceList(XmlResourceCollection servers) {
        ServerInfo si = new ServerInfo(getIdv(), servers);
        List serverList = new ArrayList();
        List stats = new ArrayList();
        List serverDescriptors = si.getServers("image", false, false);
        for (int i=0; i<serverDescriptors.size(); i++) {
            ServerDescriptor sd = (ServerDescriptor)serverDescriptors.get(i);
            serverList.add(sd.getServerName());
        }
        PreferenceList serverPrefs = new PreferenceList(serverList);
        return serverPrefs;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected String getDefaultDisplayType() {
        return "imagesequence";
    }

    /**
     * Get the xml resource collection that defines the image default xml
     *
     * @return Image defaults resources
     */
    protected XmlResourceCollection getImageDefaults() {
        XmlResourceCollection collection = 
           getIdv().getResourceManager().getXmlResources(
            IdvResourceManager.RSC_IMAGEDEFAULTS);
        return collection;
    }

    /**
     * Get the xml resource collection that defines the servers xml
     *
     * @return server resources
     */
    protected XmlResourceCollection getServers() {
        XmlResourceCollection serverCollection =
           getIdv().getResourceManager().getXmlResources(
            IdvResourceManager.RSC_SERVERS);
        return serverCollection;
    }


    /**
     * User said go, we go. Simply get the list of images
     * from the imageChooser and create the ADDE.IMAGE
     * DataSource
     *
     */
    public void doLoadInThread() {
        ImageDataset ids = new ImageDataset(imageChooser.getDatasetName(),
                                            imageChooser.getImageList());
        // make properties Hashtable to hand the station name
        // to the AddeImageDataSource
        Hashtable ht = new Hashtable();
        getDataSourceProperties(ht);
        Object bandName = imageChooser.getSelectedBandName();
        if (bandName != null) {
            ht.put(imageChooser.DATA_NAME_KEY, bandName);
        }
        makeDataSource(ids, "ADDE.IMAGE", ht);
    }

    /**
     * _more_
     *
     * @param ht _more_
     */
    protected void getDataSourceProperties(Hashtable ht) {
        super.getDataSourceProperties(ht);
        ht.put(imageChooser.DATASET_NAME_KEY, imageChooser.getDatasetName());
    }
}
