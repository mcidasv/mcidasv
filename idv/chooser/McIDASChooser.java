package ucar.unidata.idv.chooser;

import ucar.unidata.idv.*;

import ucar.unidata.data.imagery.McIDASDataset;

import java.awt.*;
import java.awt.event.*;

import java.util.List;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import ucar.unidata.util.PreferenceList;

import ucar.unidata.xml.XmlResourceCollection;

import org.w3c.dom.Element;

import ucar.unidata.ui.imagery.McIDASXFrameChooser;
//import ucar.unidata.data.imagery.McIDASDataset;


public class McIDASChooser extends IdvChooser {

    private McIDASXFrameChooser mcidasxChooser;

    /**
     * Create the chooser with the given manager and xml
     *
     * @param mgr The manager
     * @param root The xml
     *
     */
    public McIDASChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    /**
     * Handle the update event. Just pass it through to the imageChooser
     */
    public void doUpdate() {
        mcidasxChooser.doUpdate();
    }

    /**
     * Make the GUI
     *
     * @return The GUI
     */
    protected Component doMakeContents() {
        mcidasxChooser = doMakeMcidasXChooser();
        initChooserPanel(mcidasxChooser);
        mcidasxChooser.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals(
                        McIDASXFrameChooser.NEW_SELECTION)) {
                    loadMcIDASDataSet(e);
                }

            }
        });
        return mcidasxChooser.getContents();
    }

    /**
     * Make the chooser. This is a hook so a derived class
     * can make its own chooser
     *
     * @return The {@link ucar.unidata.ui.imagery.McIDASXFrameChooser} to pass
     * to the mcidasxChooser.
     */
    protected McIDASXFrameChooser doMakeMcidasXChooser() {
        return new McIDASXFrameChooser(getPreferenceList(McIDASIdvChooser.PREF_FRAMEDESCLIST)) {
            public void doCancel() {
                closeChooser();
            }
        };
    }

    /**
     * Get the xml resource collection that defines the frame default xml
     *
     * @return Frame defaults resources
     */
    protected XmlResourceCollection getFrameDefaults() {
        return getIdv().getResourceManager().getXmlResources(
            McIDASIdvResourceManager.RSC_FRAMEDEFAULTS);
    }

    /**
     * User said go, we go. Simply get the list of frames
     * from the mcidasxChooser and create the MCIDAS
     * DataSource
     *
     * @param e The event
     */
    protected void loadMcIDASDataSet(PropertyChangeEvent e) {
        McIDASXFrameChooser mxc = (McIDASXFrameChooser) e.getSource();
        McIDASDataset mds = new McIDASDataset( mxc.getDatasetName(),
                                            (List) e.getNewValue());
        // make properties Hashtable 
        Hashtable ht = new Hashtable();
        ht.put(mxc.FRAME_NUMBERS_KEY, mds.frameNumbers);
        makeDataSource("", "MCIDAS", ht);
    }

}
