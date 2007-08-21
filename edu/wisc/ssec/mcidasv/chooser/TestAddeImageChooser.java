package edu.wisc.ssec.mcidasv.chooser;

import edu.wisc.ssec.mcidas.*;
import edu.wisc.ssec.mcidas.adde.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.idv.IdvResourceManager;

import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.idv.chooser.adde.AddeChooser;
import ucar.unidata.idv.chooser.adde.AddeImageChooser;
import ucar.unidata.idv.chooser.adde.AddeServer;

import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.AddeImageInfo;
import ucar.unidata.data.imagery.ImageDataset;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;


public class TestAddeImageChooser extends AddeImageChooser {

    /** Command for connecting */
    protected static final String CMD_MANAGER = "cmd.manager";

    /** Default value for the user property */
    protected static String DEFAULT_USER = "idv";

    /** Default value for the proj property */
    protected static String DEFAULT_PROJ = "0";

    /** Holds the subsetting defaults */
    private XmlResourceCollection addeDefaults;
    private XmlResourceCollection addeServerRSC;
    static private Element addeServersRoot;
    static private Document addeServersDocument;

    private ServerInfo serverInfo = null;

    private JComboBox serverSelector;

    private List addeServers;

    private static String user;
    private static String proj;
    private static AddeServer addeServer = null;


    public TestAddeImageChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        this.addeServerRSC = getAddeServers();
        serverInfo = getServerInfo();
        this.user = serverInfo.getUser();
        this.proj = serverInfo.getProj();
        serverSelector = getServerSelector();
        DEFAULT_USER = this.user;
        DEFAULT_PROJ = this.proj;
    }

    /**
     * Get the server selector
     * @return The server selector
     */
    public JComboBox getServerSelector() {
        serverSelector = super.getServerSelector();
        ItemListener[] ell = serverSelector.getItemListeners();
        serverSelector.removeItemListener((ItemListener)ell[0]);
        updateServers();
        serverSelector.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateGroups();
            }
        });
        return serverSelector;
    }

    /**
     * Add to the given comps list all the status line and server
     * components.
     *
     * @param comps List of comps to add to
     * @param extra The components after the server box if non-null.
     */
    protected void addTopComponents(List comps, Component extra) {
        comps.add(GuiUtils.rLabel(""));
        comps.add(getStatusComponent());
        comps.add(GuiUtils.rLabel(LABEL_SERVER));
        if (extra == null) {
            extra = GuiUtils.filler();
        }
        GuiUtils.tmpInsets = GRID_INSETS;
        JPanel right = GuiUtils.doLayout(new Component[] { serverSelector,
                extra, getConnectButton(), getManagerButton() },4, GuiUtils.WT_YN,
                                             GuiUtils.WT_N);
        comps.add(GuiUtils.left(right));
    }

    /**
     * Create the 'Manager' button.
     *
     * @return The manager button.
     */
    protected JComponent getManagerButton() {
        JButton managerBtn = new JButton("Manager");
        managerBtn.setActionCommand(CMD_MANAGER);
        managerBtn.addActionListener(this);
        return registerStatusComp("manager", managerBtn);
        //         return managerBtn;
    }

    /**
     * Handle the event
     * 
     * @param ae The event
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(CMD_MANAGER)) {
            doManager();
        } else {
            super.actionPerformed(ae);
        }
    }

    /**
     * Connect to the server. Call handleConnect in a thread
     */
    protected final void doManager() {
        getIdv().getPreferenceManager().showTab("ADDE Servers");
        //ServerPreferenceManager mspm = new ServerPreferenceManger( getIdv());
    }

    /**
     * Get any extra key=value pairs that are appended to all requests.
     *
     * @param buff The buffer to append onto
     */
    protected void appendMiscKeyValues(StringBuffer buff) {
        appendKeyValue(buff, PROP_COMPRESS, DEFAULT_COMPRESS);
        appendKeyValue(buff, PROP_PORT, DEFAULT_PORT);
        appendKeyValue(buff, PROP_DEBUG, DEFAULT_DEBUG);
        appendKeyValue(buff, PROP_VERSION, DEFAULT_VERSION);
        appendKeyValue(buff, PROP_USER, DEFAULT_USER);
        appendKeyValue(buff, PROP_PROJ, DEFAULT_PROJ);
    }


    /**
     * Reload the list of servers if they have changed
     */
    public void updateServers() {
        if (serverInfo == null) serverInfo = getServerInfo();
        List servers =
            getIdv().getIdvChooserManager().getAddeServers(getGroupType());
        this.addeServers = AddeServer.getServersWithType(getGroupType(), servers);
        GuiUtils.setListData(serverSelector, addeServers);
        serverSelector.setSelectedIndex(0);
        updateGroups();
    }

    /**
     * get the adde server grup type to use
     *
     * @return group type
     */
    protected String getGroupType() {
        return AddeServer.TYPE_IMAGE;
    }

    /**
     * Set the group list
     */
    protected void updateGroups() {
        if (groupSelector != null) {
            AddeServer selectedServer = (AddeServer)serverSelector.getSelectedItem();
            if (selectedServer != null) {
                List groups   = selectedServer.getGroupsWithType(getGroupType(), true);
                if (groups != null) {
                    GuiUtils.setListData(groupSelector, groups);
                    if (groups.size() > 0) groupSelector.setSelectedIndex(0);
                }
            }
        }
    }


    /**
     * Get the xml resource collection that defines the image default xml
     *
     * @return Image defaults resources
     */
    protected XmlResourceCollection getAddeServers() {
        return getIdv().getResourceManager().getXmlResources(
            IdvResourceManager.RSC_ADDESERVER);
    }


    /**
     * Get the xml resource collection that defines the image default xml
     *
     * @return Image defaults resources
     */
    protected XmlResourceCollection getImageDefaults() {
        return getIdv().getResourceManager().getXmlResources(
            IdvResourceManager.RSC_IMAGEDEFAULTS);
    }

    private ServerInfo getServerInfo() {
        if (addeServerRSC == null) addeServerRSC = getAddeServers();
        return new ServerInfo(getIdv(), addeServerRSC);
    }


    /**
     * Handle when the user presses the update button
     *
     * @throws Exception On badness
     */
    public void handleUpdate() throws Exception {
        serverInfo = null;
        updateServers();
    }


    private AddeServer getAddeServer() {
        AddeServer curServer = null;
        if (serverSelector == null) {
            serverSelector = getServerSelector();
        }
        Object selected = serverSelector.getSelectedItem();
        if (selected != null) {
            if (selected instanceof AddeServer) 
                curServer = (AddeServer) selected;
        }
        return curServer;
    }

    /**
     * Set the properties on the AddeImageInfo from the list of properties
     *
     * @param aii  The AddeImageInfo
     * @param props  list of props to set
     * @param ad The AreaDirectory
     */
    protected void setImageInfoProps(AddeImageInfo aii, String[] props,
                                     AreaDirectory ad) {
        super.setImageInfoProps(aii, props, ad);
        for (int i = 0; i < props.length; i++) {
            String prop  = props[i];
            String value = getPropValue(prop, ad);
            if (prop.equals(PROP_USER)) {
                value = this.user;
                aii.setUser(value);
            } else if (prop.equals(PROP_PROJ)) {
                value = this.proj;
                aii.setProject(Integer.parseInt(value));
            }
        }
    }
}
