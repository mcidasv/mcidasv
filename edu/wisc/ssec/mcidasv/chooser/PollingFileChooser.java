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


import java.awt.Component;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.w3c.dom.Element;

import ucar.unidata.data.DataSource;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
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
     * Process PollingInfo GUI components based on their label and properties
     * Turn it into a nicely-formatted labeled panel
     */
    private JPanel processPollingOption(JLabel label, JPanel panel) {
    	String string = label.getText().trim();
    	
    	// Name
    	if (string.equals("Name:")) {
    		string = "Source Name:";
    	}
    	
    	// File Pattern
    	if (string.equals("File Pattern:")) {
    		Component panel1 = panel.getComponent(0);
    		if (panel1 instanceof JPanel) {
	    		Component[] comps = ((JPanel)panel1).getComponents();
	    		if (comps.length == 2) {
	        		List newComps1 = new ArrayList();
	        		List newComps2 = new ArrayList();
	        		if (comps[0] instanceof JPanel) {
	        			Component[] comps2 = ((JPanel)comps[0]).getComponents();
	        			if (comps2.length==1 &&
	        					comps2[0] instanceof JPanel)
	        				comps2=((JPanel)comps2[0]).getComponents();
	        			System.out.println(comps2.length);
	        			if (comps2.length == 2) {
	        				if (comps2[0] instanceof JTextField) {
	        					McVGuiUtils.setComponentWidth((JTextField) comps2[0], McVGuiUtils.Width.SINGLE);
	        				}
	        				newComps1.add(comps2[0]);
	        				newComps2.add(comps2[1]);
	        			}
	        		}    				
    				newComps1.add(comps[1]);
    	    		panel = GuiUtils.vbox(
    	    				GuiUtils.left(GuiUtils.hbox(newComps1)),
    	    				GuiUtils.left(GuiUtils.hbox(newComps2))
    	    		);
	    		}
    		}
    	}
    	
    	// Files
    	if (string.equals("Files:")) {
    		Component panel1 = panel.getComponent(0);
    		if (panel1 instanceof JPanel) {
	    		Component[] comps = ((JPanel)panel1).getComponents();
	    		if (comps.length == 6) {
	        		List newComps1 = new ArrayList();
	        		List newComps2 = new ArrayList();
	    			if (comps[3] instanceof JRadioButton) {
	    				String text = ((JRadioButton) comps[3]).getText().trim();
	    				if (text.equals("All files in last:")) text="All files in last";
	    				((JRadioButton) comps[3]).setText(text);
	    			}
	    			if (comps[4] instanceof JTextField) {
	    				McVGuiUtils.setComponentWidth((JTextField) comps[4], McVGuiUtils.Width.HALF);
	    			}
	    			if (comps[5] instanceof JLabel) {
	    				String text = ((JLabel) comps[5]).getText().trim();
	    				((JLabel) comps[5]).setText(text);
	    			}
    				newComps1.add(comps[0]);
    				newComps1.add(comps[1]);
    				newComps2.add(comps[3]);
    				newComps2.add(comps[4]);
    				newComps2.add(comps[5]);
    	    		panel = GuiUtils.vbox(
    	    				GuiUtils.left(GuiUtils.hbox(newComps1)),
    	    				GuiUtils.left(GuiUtils.hbox(newComps2))
    	    		);
	    		}
    		}
    	}
    	
    	// Polling
    	if (string.equals("Polling:")) {
    		Component panel1 = panel.getComponent(0);
    		if (panel1 instanceof JPanel) {
	    		Component[] comps = ((JPanel)panel1).getComponents();
	    		if (comps.length == 4) {
	        		List newComps = new ArrayList();
	    			if (comps[0] instanceof JCheckBox) {
	    				((JCheckBox) comps[0]).setText("");
	    			}
	    			if (comps[1] instanceof JLabel) {
	    				String text = ((JLabel) comps[1]).getText().trim();
	    				if (text.equals("Check every:")) text="Poll every";
	    				((JLabel) comps[1]).setText(text);
	    			}
	    			if (comps[2] instanceof JTextField) {
	    				McVGuiUtils.setComponentWidth((JTextField) comps[2], McVGuiUtils.Width.HALF);
	    			}
	    			if (comps[3] instanceof JLabel) {
	    				String text = ((JLabel) comps[3]).getText().trim();
	    				((JLabel) comps[3]).setText(text);
	    			}
    				newComps.add(comps[0]);
    				newComps.add(comps[1]);
    				newComps.add(comps[2]);
    				newComps.add(comps[3]);
    	    		string="";
    	    		panel = GuiUtils.left(GuiUtils.hbox(newComps));
	    		}
    		}
    	}
    	
    	return McVGuiUtils.makeLabeledComponent(string, panel);
    }
    
    /**
     * Turn PollingInfo options into a nicely-formatted panel
     */
    private JPanel processPollingOptions(List comps) {
    	List newComps = new ArrayList();
    	newComps = new ArrayList();
    	if (comps.size() == 5) {
    		newComps.add(comps.get(1));
    		newComps.add(comps.get(0));
    		newComps.add(comps.get(2));
    		newComps.add(comps.get(3));
    		newComps.add(comps.get(4));
    	}
    	else {
    		newComps = comps;
    	}
    	return GuiUtils.top(GuiUtils.vbox(newComps));
    }
    
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
            pollingInfo.setFilePaths(Misc.newList(getAttribute(ATTR_DIRECTORY, "")));
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
        List newComps = new ArrayList();
        pollingInfo.getPropertyComponents(comps, true, XmlUtil.hasAttribute(chooserNode, ATTR_FILECOUNT));
        for (int i=0; i<comps.size()-1; i++) {
        	JComponent compLabel = (JComponent)comps.get(i);
        	if (compLabel instanceof JLabel) {
        		i++;
            	JComponent compPanel = (JComponent)comps.get(i);
            	if (compPanel instanceof JPanel) {
            		newComps.add(processPollingOption((JLabel)compLabel, (JPanel)compPanel));
            	}
        	}
        }
        
        JPanel pollingPanel = processPollingOptions(newComps);
        setHaveData(true);
        return pollingPanel;
    }
    
    /**
     * Get the center panel for the chooser
     * @return the center panel
     */
    protected JPanel getCenterPanel() {
    	return new JPanel();
    }

}

