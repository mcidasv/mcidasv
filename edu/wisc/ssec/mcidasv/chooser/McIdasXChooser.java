/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.chooser;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import edu.wisc.ssec.mcidasv.data.McIdasFrame;
import edu.wisc.ssec.mcidasv.data.McIdasXInfo;

/**
 * Widget to select frames from McIdas-X
 * Displays a list of the descriptors (names) of the frame datasets
 */
public class McIdasXChooser extends FrameChooser {

    /** A widget for the command line text */
    private JTextField hostLine;
    private JTextField portLine;
    private JTextField keyLine;

    private boolean goodToGo = true;

    private McIdasXInfo mcidasxInfo;
    
    /**
     * Construct an Adde image selection widget
     *
     * @param idvChooser Chooser to which this interface applies
     * @param descList Holds the preferences for the image descriptors
     */
    public McIdasXChooser(IdvChooser idvChooser,
                               PreferenceList descList) {
/*
        System.out.println("McIdasXChooser constructor:");
        System.out.println("   idvChooser=" + idvChooser);
        System.out.println("   descList=" + descList);
*/
        mcidasxInfo = new McIdasXInfo();
    }

    /**
     * This allows derived classes to provide their own name for labeling, etc.
     *
     * @return  the dataset name
     */
    public String getDataName() {
        return "McIDAS-X Frame Data";
    }

    /**
     * Get the name of the dataset.
     *
     * @return descriptive name of the dataset.
     */
    public String getDatasetName() {
        String temp = null;
        return temp;
    }

    /**
     * Check if we are ready to read times
     *
     * @return  true if times can be read
     */
    protected boolean canReadFrames() {
        return true;
    }

    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    protected JComponent doMakeContents() {
//    	String s =
//    		"<html>The McIDAS-X Bridge provides a way to load data from an active McIDAS-X session " +
//    		"(version 2007a or later) into McIDAS-V.<p>" +
//    		
//    		"To start the McIDAS-X bridge listener, type <b>MCLISTEN START</b> " +
//    		"in a running McIDAS-X session on your local machine. To connect to the listener from " +
//    		"McIDAS-V, select the \"McIDAS-X Bridge\" data source in the Data Explorer, and click " +
//    		"the \"Add Source\" button to connect to the defaults of localhost listening on port " +
//    		"8080.  If <b>MCLISTEN START</b> was not run on the localhost listening on port <b>8080</b>, an error " +
//    		"box will say that the \"Connection to McIDAS-X Bridge Listener at <b>localhost:8080</b> failed\".<p>" +
//    		
//    		"If the McIDAS-X bridge listener is running, clicking \"Add Source\" will bring up the " +
//    		"Field Selector and list the Frame Sequence as the available Field.  Click the circle to " +
//    		"the left of \"Frame Sequence\" to expand that list and list all available frames in the " +
//    		"connected McIDAS-X session.  Select a frame (or the entire Frame Sequence), and click " +
//    		"the \"Create Display\" button.  This will create a new display in the Layer Controls tab of the " +
//    		"Data Explorer mirroring the McIDAS-X frame(s) selected.  You can enter McIDAS-X commands " +
//    		"in the \"Command Line\" text entry box at the bottom of the Layer Controls tab. This will run " +
//    		"McIDAS-X commands, and the McIDAS-V display in the Layer Controls tab will update to reflect the " +
//    		"results.<p>" +
//    		
//    		"To import these McIDAS-X frames into the current McIDAS-V 3D panel, select the " +
//    		"Settings tab and check the \"Display data in main 3D panel\" option.  This will import " +
//    		"all of the navigated McIDAS-X frames into the 3D panel.</html>";
//
//        JEditorPane descLabel = new JEditorPane();
//        descLabel.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
//        descLabel.setPreferredSize(new Dimension(300, 300));
//        descLabel.setEditable(false);
//        descLabel.setContentType("text/html");
//        descLabel.setText(s);
//
//    	JScrollPane scroller = new JScrollPane(descLabel);
//    	scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//    	scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    	List allComps = new ArrayList();
        getComponents(allComps);
//        JPanel descPanel = GuiUtils.center(scroller);
        JPanel linkPanel = GuiUtils.doLayout(allComps, 1, GuiUtils.WT_N, GuiUtils.WT_N);

        return GuiUtils.topLeft(GuiUtils.centerBottom(linkPanel, getDefaultButtons()));
    }

    private void sendHost() {
        //System.out.println("sendHost");
        mcidasxInfo.setHostString((hostLine.getText()).trim());
        addSource();
    }

    private void sendPort() {
        //System.out.println("sendPort");
    	mcidasxInfo.setPortString((portLine.getText()).trim());
        addSource();
    }

    private void sendKey() {
        //System.out.println("sendKey");
    	mcidasxInfo.setKeyString((keyLine.getText()).trim());
        addSource();
    }

    private void addSource() {
        goodToGo = true;
    }
    
    /**
     * Return the host string from the McIdasXInfo object
     * 
     * @return host string
     */
    public String getHost() {
    	return mcidasxInfo.getHostString();
    }
    
    /**
     * Return the port string from the McIdasXInfo object
     * 
     * @return port string
     */
    public String getPort() {
    	return mcidasxInfo.getPortString();
    }
    
    /**
     * Return the key string from the McIdasXInfo object
     * 
     * @return key string
     */
    public String getKey() {
    	return mcidasxInfo.getKeyString();
    }
    
    /**
     * Get the names for the buttons (override).
     *
     * @return array of button names
     */
    protected String[] getButtonLabels() {
        return new String[] { getLoadCommandName(), GuiUtils.CMD_HELP };
    }

    /**
     * Make the components (label/widget) and return them
     *
     *
     * @param comps The list to add components to
     */
    protected void getComponents(List comps) {
        List firstLine = new ArrayList();

        /* Host */
        JLabel hostLabel = GuiUtils.rLabel("Host: ");
        firstLine.add(hostLabel);
        hostLine = new JTextField(mcidasxInfo.getHostString(), 10);
        hostLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) { sendHost(); }
        });
        hostLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                sendHost();
            }
        });
        hostLine.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
            }
        });
        firstLine.add(hostLine);
        firstLine.add(new JLabel("  "));

        /* Port */
        JLabel portLabel = GuiUtils.rLabel("Port: ");
        firstLine.add(portLabel);
        portLine = new JTextField(mcidasxInfo.getPortString(), 6);
        portLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) { sendPort(); }
        });
        portLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) { sendPort(); }
        });
        portLine.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
            }
        });
        firstLine.add(portLine);
        firstLine.add(new JLabel("  "));

        /* Key */
        JLabel keyLabel = GuiUtils.rLabel("Key: ");
//        firstLine.add(keyLabel);
        keyLine = new JTextField(mcidasxInfo.getKeyString(), 32);
        keyLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) {
                 sendKey();
            }
        });
        keyLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                 sendKey();
            }
        });
        keyLine.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
            }
        });
//        firstLine.add(keyLine);
//        firstLine.add(new JLabel("  "));
        double[] nineWt = { 0, 0, 0, 0, 0, 0, 0, 0, 1 };
        JPanel firstPanel = GuiUtils.doLayout(firstLine, 9, nineWt,
                                              GuiUtils.WT_N);
        
        comps.add(new JLabel(" "));
        comps.add(firstPanel);
        comps.add(new JLabel(" "));
    }

    /**
     *  Read the set of image times available for the current server/group/type
     *  This method is a wrapper, setting the wait cursor and wrapping the
     *  call to readFramesInner; in a try/catch block
     */
    protected void readFrames() {
    	clearFramesList();
    	if (!canReadFrames()) {
    		return;
    	}
    	Misc.run(new Runnable() {
    		public void run() {
    			updateStatus();
    			showWaitCursor();
    			try {
    				readFramesInner();
    			} catch (Exception e) {
    			}
    			showNormalCursor();
    			updateStatus();
    		}
    	});
    }

    /**
     * Set the list of dates/times based on the image selection
     *
     */
    protected void readFramesInner() {
    	loadFrames();
    }

    /**
     * Load the frames
     *
     */
    protected void loadFrames() {
    }

    /**
     * Does this selector have all of its state set to load in data
     *
     * @return Has the user chosen everything they need to choose to load data
     */
    protected boolean getGoodToGo() {
    	return goodToGo;
    }

    /**
     * Returns a list of the images to load or null if none have been
     * selected.
     *
     * @return  list  get the list of image descriptors
     */
    public List getFrameList() {
        List frames = new ArrayList();
        List xFrames = this.mcidasxInfo.getFrameNumbers();
        if (xFrames.size() < 1) return frames;
        for (int i = 0; i < xFrames.size(); i++) {
            Integer frmInt = (Integer)xFrames.get(i);
            McIdasFrame frame = new McIdasFrame(frmInt.intValue(), this.mcidasxInfo);
            frames.add(frame);
        }
        return frames;
    }

    /**
     * Method to do the work of loading the data
     */
    public void doLoad() {
        List frames = getFrameList();
        if (frames.size() < 1) {
            LogUtil.userMessage("Connection to McIDAS-X Bridge Listener at " + getHost() + ":" + getPort() + "failed");
            return;
        }
        try {
           firePropertyChange(NEW_SELECTION, null, frames);
        } catch (Exception exc) {
           logException("doLoad", exc);
        }
    }
}
