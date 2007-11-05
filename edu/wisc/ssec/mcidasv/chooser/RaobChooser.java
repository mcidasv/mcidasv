package edu.wisc.ssec.mcidasv.chooser;

import edu.wisc.ssec.mcidasv.chooser.SoundingSelector;

import org.w3c.dom.Element;

import ucar.unidata.data.sounding.RaobDataSet;

import ucar.unidata.idv.*;
import ucar.unidata.idv.chooser.*;
import ucar.unidata.idv.chooser.adde.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;

import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;




/**
 * A chooser class for selecting Raob data.
 * Mostly just a wrapper around a
 *  {@link ucar.unidata.view.sounding.SoundingSelector}
 * that does most of the work
 *
 * @author IDV development team
 * @version $Revision$Date: 2007/07/27 13:53:08 $
 */


public class RaobChooser extends IdvChooser {

    /** The data source id (from datasources.xml) that we create */
    public static final String DATASOURCE_RAOB = "RAOB";

    /**
     * An xml attribute to show or not show the server selector
     * in the gui
     */
    public static final String ATTR_SHOWSERVER = "showserver";


    /** Does most of the work */
    private SoundingSelector soundingChooser;

    /** Accounting information */
    protected static String user = "idv";
    protected static String proj = "0";

    protected boolean firstTime = true;
    protected boolean retry = true;

    /** Not connected */
//    protected static final int STATE_UNCONNECTED = 0;

    /** The xml node from choosers.xml that defines this chooser */
    Element chooserNode;

    /**
     * Construct a <code>RaobChooser</code> using the manager
     * and the root XML that defines this object.
     *
     * @param mgr  <code>IdvChooserManager</code> that controls this chooser.
     * @param root root element of the XML that defines this object
     */
    public RaobChooser(IdvChooserManager mgr, Element chooserNode) {
        super(mgr, chooserNode);
    }

    /**
     * Pass this on to the
     * {@link ucar.unidata.view.sounding.SoundingSelector}
     * to reload the current chooser state from the server
     */
    public void doUpdate() {
        soundingChooser.doUpdate();
    }

    /**
     * Make the GUI
     *
     * @return The GUI
     */
    protected JComponent doMakeContents() {
        boolean showServer = XmlUtil.getAttribute(chooserNode,
                                 ATTR_SHOWSERVER, true);
        soundingChooser = new SoundingSelector(this,
                getPreferenceList(PREF_ADDESERVERS), showServer,
                true /*multiple selection*/) {
            public void doLoad() {
                RaobChooser.this.doLoad();
            }

            public void doCancel() {
                //                closeChooser();
            }

        };
        initChooserPanel(soundingChooser);
        return GuiUtils.left(soundingChooser.getContents());
    }



    /**
     * get default display to create
     *
     * @return default display
     */
    protected String getDefaultDisplayType() {
        return "raob_skewt";
    }


    /**
     * Load the data source in a thread
     */
    public void doLoadInThread() {
        List soundings = soundingChooser.getSelectedSoundings();
        if (soundings.size() == 0) {
            userMessage("Please select one or more soundings.");
            return;
        }
        Hashtable ht = new Hashtable();
        getDataSourceProperties(ht);

        makeDataSource(new RaobDataSet(soundingChooser.getSoundingAdapter(),
                                       soundings), DATASOURCE_RAOB, ht);
        soundingChooser.getAddeChooser().saveServerState();
    }

    /**
     * Show the given error to the user. If it was an Adde exception
     * that was a bad server error then print out a nice message.
     *
     * @param excp The exception
     */
    protected void handleConnectionError(Exception excp) {
        String aes = excp.toString();
        if ((aes.indexOf("Accounting data")) >= 0) {
            JTextField projFld   = null;
            JTextField userFld   = null;
            JComponent contents  = null;
            JLabel     label     = null;
            AddeChooser addeChooser = soundingChooser.getAddeChooser();
            if (firstTime == false) {
                retry = false;
            } else {
                if (projFld == null) {
                    projFld            = new JTextField("", 10);
                    userFld            = new JTextField("", 10);
                    GuiUtils.tmpInsets = GuiUtils.INSETS_5;
                    contents = GuiUtils.doLayout(new Component[] {
                        GuiUtils.rLabel("User ID:"),
                        userFld, GuiUtils.rLabel("Project #:"), projFld, }, 2,
                            GuiUtils.WT_N, GuiUtils.WT_N);
                    label    = new JLabel(" ");
                    contents = GuiUtils.topCenter(label, contents);
                    contents = GuiUtils.inset(contents, 5);
                }
                String lbl = (firstTime
                              ? "The server: " + addeChooser.getServer()
                                + " requires a user ID & project number for access"
                              : "Authentication for server: " + addeChooser.getServer()
                                + " failed. Please try again");
                label.setText(lbl);

                if ( !GuiUtils.showOkCancelDialog(null, "ADDE Project/User name",
                        contents, null)) {
                    //setState(STATE_UNCONNECTED);
                    return;
                }
                user = userFld.getText().trim();
                proj  = projFld.getText().trim();
            }
            if (firstTime == true) {
                firstTime = false;
                try {
                    addeChooser.handleUpdate();
                } catch (Exception e) {
                    System.out.println("handleUpdate exception e=" + e);
                }
            }
            return;
        }
        String message = excp.getMessage().toLowerCase();
        if (message.indexOf("with position 0") >= 0) {
            LogUtil.userErrorMessage("Unable to handle archive dataset");
            return;
        }
        //super.handleConnectionError(excp);
    }

}

