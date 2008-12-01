/*
 * $Id$
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.wisc.ssec.mcidasv.chooser;


import java.awt.Insets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.w3c.dom.Element;

import ucar.unidata.data.DataSource;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.PollingInfo;
import ucar.unidata.xml.XmlUtil;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;




/**
 * A class for choosing files that can be polled.
 *
 * @author IDV development team
 */
public class PollingFileChooser extends FileChooser {

    /** Any initial file system path to start with */
    public static final String ATTR_DIRECTORY = "directory";

    /** Polling interval */
    public static final String ATTR_INTERVAL = "interval";

    /** The title attribute */
    public static final String ATTR_TITLE = "title";

    /** polling info */
    private PollingInfo pollingInfo;

    /**
     * Create the PollingFileChooser, passing in the manager and the xml element
     * from choosers.xml
     *
     * @param mgr The manager
     * @param root The xml root
     *
     */
    public PollingFileChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }

    /**
     * Override the base class method to catch the do load
     */
    public void doLoadInThread() {
       	Element chooserNode = getXmlNode();

        Hashtable properties = new Hashtable();
        if ( !pollingInfo.applyProperties()) {
            return;
        }
        //pollingInfo.setMode(PollingInfo.MODE_COUNT);
        if (pollingInfo.hasName()) {
            properties.put(DataSource.PROP_TITLE, pollingInfo.getName());
        }
        properties.put(DataSource.PROP_POLLINFO, pollingInfo.cloneMe());

        String dataSourceId;
        if (XmlUtil.hasAttribute(chooserNode, ATTR_DATASOURCEID)) {
            dataSourceId = XmlUtil.getAttribute(chooserNode, ATTR_DATASOURCEID);
        } else {
            dataSourceId = getDataSourceId();
        }
        makeDataSource(pollingInfo.getFiles(), dataSourceId, properties);
        idv.getStateManager().writePreference(PREF_POLLINGINFO + "." + getId(), pollingInfo);
    }
    
    /**
     * Get the tooltip for the load button
     *
     * @return The tooltip for the load button
     */
    protected String getLoadToolTip() {
        return "Load the directory";
    }

    /**
     * Override the base class method to catch the do update
     */
    public void doUpdate() {}

    /**
     * Get the top panel for the chooser
     * @return the top panel
     */
    protected JPanel getTopPanel() {
       	Element chooserNode = getXmlNode();

        pollingInfo = (PollingInfo) idv.getPreference(PREF_POLLINGINFO + "." + getId());
        if (pollingInfo == null) {
            pollingInfo = new PollingInfo();
            pollingInfo.setMode(PollingInfo.MODE_COUNT);
            pollingInfo.setName(getAttribute(ATTR_TITLE, ""));
            pollingInfo.setFilePattern(getAttribute(ATTR_FILEPATTERN, ""));
            pollingInfo.setFilePath(getAttribute(ATTR_DIRECTORY, ""));
            pollingInfo.setIsActive(XmlUtil.getAttribute(chooserNode, ATTR_POLLON, true));

            pollingInfo.setInterval((long) (XmlUtil.getAttribute(chooserNode, ATTR_INTERVAL, 5.0) * 60 * 1000));
            int fileCount = 1;
            String s = XmlUtil.getAttribute(chooserNode, ATTR_FILECOUNT, "1");
            s = s.trim();
            if (s.equals("all")) {
                fileCount = Integer.MAX_VALUE;
            } else {
                fileCount = new Integer(s).intValue();
            }
            pollingInfo.setFileCount(fileCount);
        }

        // Pull apart the PollingInfo components and rearrange them
        // Don't want to override PollingInfo because it isn't something the user sees
        // Arranged like: Label, Panel; Label, Panel; Label, Panel; etc...
        List comps = new ArrayList();
        pollingInfo.getPropertyComponents(comps, true, XmlUtil.hasAttribute(chooserNode, ATTR_FILECOUNT));
        for (int i=0; i<comps.size(); i++) {
        	JComponent comp = (JComponent)comps.get(i);
        	if (comp instanceof JLabel) {
        		comps.set(i, McVGuiUtils.makeLabelRight(((JLabel)comp).getText()));
        	}
        }
        
        
        System.out.println("DAVEP:" + comps);
        
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        setHaveData(true);
        return GuiUtils.top(GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY, GuiUtils.WT_N));
    }
    
    /**
     * Get the center panel for the chooser
     * @return the center panel
     */
    protected JPanel getCenterPanel() {
    	return new JPanel();
    }

}

