package ucar.unidata.idv.chooser.mcidas;



import ucar.unidata.idv.*;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;


import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.PreferenceList;

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

    /** This really does the work */
    private TestAddeImageChooser imageChooser;

    /**
     * Make a new one
     *
     * @param mgr The manager
     * @param root The xml element that defined this object
     *
     */
    public TestImageChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        List choozers = mgr.getChooserIds();
        String temp = null;
        for (int i=0; i<choozers.size(); i++) {
            temp = (String)choozers.get(i);
            if (temp.startsWith("chooser.testimages.default")) {
                defaultChoosers.add(choozers.get(i));
            }
        }
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
        return new TestAddeImageChooser(this, getImageDefaults(),
                                    getPreferenceList(PREF_IMAGEDESCLIST),
                                    getPreferenceList(PREF_ADDESERVERS),
                                    defaultChoosers) {
            public void doCancel() {
                closeChooser();
            }
        };
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
