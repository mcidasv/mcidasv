package edu.wisc.ssec.mcidasv.chooser;


import edu.wisc.ssec.mcidasv.*;

import edu.wisc.ssec.mcidasv.data.McDataset;

import edu.wisc.ssec.mcidasv.ui.McIdasXChooser;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.util.List;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;

import org.w3c.dom.Element;

import ucar.unidata.idv.*;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.util.PreferenceList;

import ucar.unidata.xml.XmlResourceCollection;


public class McIdasBridgeChooser extends IdvChooser {

    private McIdasXChooser mcIdasXChooser;

    /**
     * Create the chooser with the given manager and xml
     *
     * @param mgr The manager
     * @param root The xml
     *
     */
    public McIdasBridgeChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    /**
     * Handle the update event. Just pass it through to the mcIdasXChooser
     */
    public void doUpdate() {
        mcIdasXChooser.doUpdate();
    }

    /**
     * Make the GUI
     *
     * @return The GUI
     */
    protected JComponent doMakeContents() {
        mcIdasXChooser = doMakeMcIdasXChooser();
        initChooserPanel(mcIdasXChooser);
        mcIdasXChooser.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals(
                        McIdasXChooser.NEW_SELECTION)) {
                    loadMcIdasDataSet(e);
                }

            }
        });
        return mcIdasXChooser.getContents();
    }

    /**
     * Make the chooser. This is a hook so a derived class
     * can make its own chooser
     *
     * @return The {@link ucar.unidata.ui.imagery.McIdasXChooser} to pass
     * to the mcIdasXChooser.
     */
    protected McIdasXChooser doMakeMcIdasXChooser() {
        return new McIdasXChooser(this, getPreferenceList(McIdasChooser.PREF_FRAMEDESCLIST)) {
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
            McIDASV.RSC_FRAMEDEFAULTS);
    }

    /**
     * User said go, we go. Simply get the list of frames
     * from the mcIdasXChooser and create the McIdas-X
     * DataSource
     *
     * @param e The event
     */
    protected void loadMcIdasDataSet(PropertyChangeEvent e) {
        //System.out.println("edu/wisc/ssec/mcidasv/chooser/McIdasBridgeChooser:  loadMcIdasDataSet");
        McIdasXChooser mxc = (McIdasXChooser) e.getSource();
        McDataset mds = new McDataset( mxc.getDatasetName(),
                                            (List) e.getNewValue());
        //System.out.println("    mds.myRequest = " + mds.getRequest());
        // make properties Hashtable 
        Hashtable ht = new Hashtable();
        ht.put(mxc.FRAME_NUMBERS_KEY, mds.frameNumbers);
        //System.out.println("frameNumbers=" + mds.frameNumbers);
        if (mds.frameNumbers.size() > 1) {
           //System.out.println("  Frame Sequence");
           ht.put(mxc.DATA_NAME_KEY,"Frame Sequence");
        } else {
           //System.out.println("  Frame");
           ht.put(mxc.DATA_NAME_KEY,"Frame");
        }
        //System.out.println("REQUEST_KEY=" + mxc.REQUEST_KEY);
        //System.out.println("getRequest=" + mds.getRequest());
        ht.put(mxc.REQUEST_KEY, mds.getRequest());
        //System.out.println("    ht:  " + ht);
        makeDataSource("", "MCIDASX", ht);
    }
}
