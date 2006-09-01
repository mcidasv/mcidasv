package ucar.unidata.idv.chooser.mcidas;


import ucar.unidata.idv.*;

import java.util.Hashtable;

import java.awt.*;

import javax.swing.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.xml.XmlResourceCollection;

import org.w3c.dom.Element;

import ucar.unidata.data.imagery.ImageDataset;

import ucar.unidata.ui.imagery.mcidas.TestAddeDefaultChooser;


/**
 * Test new type of image chooser
 */


public class TestDefaultChooser extends IdvChooser {

/*
    public static final XmlIdvResource RSC_TESTDEFAULTS =
        new XmlIdvResource("application.resource.testdefaults",
                           "ADDE Test Defaults", "testfaults\\.xml$");
*/

    /** This really does the work */
    private TestAddeDefaultChooser imageChooser;

    /**
     * Make a new one
     *
     * @param mgr The manager
     * @param root The xml element that defined this object
     *
     */
    public TestDefaultChooser(IdvChooserManager mgr, Element root) {
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
    protected Component doMakeContents() {
        imageChooser = doMakeImageChooser();
        initChooserPanel(imageChooser);
        imageChooser.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals(
                        TestAddeDefaultChooser.NEW_SELECTION)) {
                    TestDefaultChooser.this.doLoad();
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
    protected TestAddeDefaultChooser doMakeImageChooser() {
        return new TestAddeDefaultChooser(getImageDefaults(),
                                    getPreferenceList(PREF_IMAGEDESCLIST),
                                    getPreferenceList(PREF_ADDESERVERS)) {
            public void doCancel() {
                closeChooser();
            }
        };
    }

    /**
     * Get the xml resource collection that defines the image default xml
     *
     * @return Image defaults resources
     */
    protected XmlResourceCollection getImageDefaults() {
        return getIdv().getResourceManager().getXmlResources(
            IdvResourceManager.RSC_TESTDEFAULTS);
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
        ht.put(imageChooser.DATASET_NAME_KEY, imageChooser.getDatasetName());
        Object bandName = imageChooser.getSelectedBandName();
        if (bandName != null) {
            ht.put(imageChooser.DATA_NAME_KEY, bandName);
        }
        makeDataSource(ids, "ADDE.IMAGE", ht);
    }


}
