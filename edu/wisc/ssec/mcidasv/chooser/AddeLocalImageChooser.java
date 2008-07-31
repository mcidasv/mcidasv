package edu.wisc.ssec.mcidasv.chooser;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.adde.AddeImageChooser;
import ucar.unidata.util.GuiUtils;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.addemanager.AddeManager;

public class AddeLocalImageChooser extends AddeImageChooser {
	
    /** Command for connecting */
    protected static final String CMD_MANAGER = "cmd.manager";

    /**
     * Construct an Adde image selection widget
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeLocalImageChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }
    
    /**
     * Just return "localhost"
     */
    public String getServer() {
    	return "localhost";
    }
    
    /**
     * Set the group list
     */
    protected void setGroups() {
    	McIDASV idv = (McIDASV)getIdv();
    	AddeManager addeManager = idv.getAddeManager();
        GuiUtils.setListData(groupSelector, addeManager.getGroups());
    }
    
    /**
     * Set the server list
     */
    public void updateServers() {
    	updateGroups();
    }
    
    /**
     * Set the group list
     */
    protected void updateGroups() {
    	setGroups();
    }

    /**
     * Get any extra key=value pairs that are appended to all requests.
     *
     * @param buff The buffer to append onto
     */
    protected void appendMiscKeyValues(StringBuffer buff) {
    	McIDASV idv = (McIDASV)getIdv();
    	AddeManager addeManager = idv.getAddeManager();
        appendKeyValue(buff, PROP_COMPRESS, "none");
        appendKeyValue(buff, PROP_PORT, addeManager.getLocalPort());
        appendKeyValue(buff, PROP_DEBUG, DEFAULT_DEBUG);
        appendKeyValue(buff, PROP_VERSION, DEFAULT_VERSION);
        appendKeyValue(buff, PROP_USER, DEFAULT_USER);
        appendKeyValue(buff, PROP_PROJ, DEFAULT_PROJ);
    }
    
    /**
     * Add to the given comps list all the status line and server
     * components.
     *
     * @param comps List of comps to add to
     * @param extra The components after the server box if non-null.
     */
    protected void addTopComponents(List comps, Component extra) {
        comps.add(GuiUtils.rLabel(LABEL_SERVER));
        if (extra == null) {
            extra = GuiUtils.filler();
        }
        GuiUtils.tmpInsets = GRID_INSETS;
        JPanel right = GuiUtils.doLayout(new Component[] { 
                GuiUtils.lLabel("<LOCAL-DATA>"), extra, getConnectButton(), getManageButton() },
                4, GuiUtils.WT_YN, GuiUtils.WT_N);
        comps.add(GuiUtils.left(right));
    }
    
    /**
     * Create the 'Manage...' button.
     *
     * @return The manage button.
     */
    protected JComponent getManageButton() {
        JButton managerBtn = new JButton("Manage...");
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
     * Go directly to the Server Manager
     */
    protected void doManager() {
        getIdv().getPreferenceManager().showTab(Constants.PREF_LIST_LOCAL_ADDE);
    }

}