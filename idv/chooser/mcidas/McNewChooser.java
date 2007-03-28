package ucar.unidata.idv.chooser.mcidas;

import ucar.unidata.idv.*;

import ucar.unidata.data.imagery.mcidas.McDataset;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;

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

import ucar.unidata.ui.imagery.mcidas.McIDASNewChooser;


public class McNewChooser extends IdvChooser {

    private McIDASNewChooser mcIDASNewChooser;

    /**
     * Create the chooser with the given manager and xml
     *
     * @param mgr The manager
     * @param root The xml
     *
     */
    public McNewChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }


    /**
     * Handle the update event. Just pass it through to the mcIDASNewChooser
     */
    public void doUpdate() {
        mcIDASNewChooser.doUpdate();
    }

    /**
     * Make the GUI
     *
     * @return The GUI
     */
    protected JComponent doMakeContents() {
        mcIDASNewChooser = doMakeMcIDASNewChooser();
        initChooserPanel(mcIDASNewChooser);
        mcIDASNewChooser.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals(
                        McIDASNewChooser.NEW_SELECTION)) {
                    loadMcIDASDataSet(e);
                }

            }
        });
        return mcIDASNewChooser.getContents();
    }

    /**
     * Make the chooser. This is a hook so a derived class
     * can make its own chooser
     *
     * @return The {@link ucar.unidata.ui.imagery.McIDASNewChooser} to pass
     * to the mcIDASNewChooser.
     */
    protected McIDASNewChooser doMakeMcIDASNewChooser() {
        return new McIDASNewChooser(this, getPreferenceList(McIDASIdvChooser.PREF_FRAMEDESCLIST)) {
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
     * from the mcIDASNewChooser and create the MCNEW
     * DataSource
     *
     * @param e The event
     */
    protected void loadMcIDASDataSet(PropertyChangeEvent e) {
        //System.out.println("idv/chooser/mcidas/McNewChooser:  loadMcIDASDataSet");
        McIDASNewChooser mxc = (McIDASNewChooser) e.getSource();
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
        makeDataSource("", "MCNEW", ht);
    }
}
