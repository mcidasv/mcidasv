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


import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.data.McIdasFrame;
import edu.wisc.ssec.mcidasv.data.McIdasXInfo;


public class McIdasBridgeChooser extends IdvChooser implements Constants {
	
    /** A widget for the command line text */
    private JTextField hostLine = new JTextField("");
    private JTextField portLine = new JTextField("");
    private JTextField keyLine = new JTextField("");

    private McIdasXInfo mcidasxInfo;

    /**
     * Create the chooser with the given manager and xml
     *
     * @param mgr The manager
     * @param root The xml
     *
     */
    public McIdasBridgeChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        mcidasxInfo = new McIdasXInfo();
    }

    public String getHost() {
    	return this.mcidasxInfo.getHostString();
    }
    
    private void setHost() {
        this.mcidasxInfo.setHostString((hostLine.getText()).trim());
    }

    public String getPort() {
    	return this.mcidasxInfo.getPortString();
    }
    
    private void setPort() {
    	this.mcidasxInfo.setPortString((portLine.getText()).trim());
    }

    public String getKey() {
    	return this.mcidasxInfo.getPortString();
    }
    
    private void setKey() {
    	this.mcidasxInfo.setKeyString((keyLine.getText()).trim());
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
     * Returns a list of the frame numbers to load or null if none have been
     * selected.
     *
     * @return  list  get the list of frame numbers
     */
    public List getFrameNumbers() {
    	return this.mcidasxInfo.getFrameNumbers();
    }
    
    public int getFrameCount() {
    	return getFrameNumbers().size();
    }
    
    /**
     * Load in an ADDE point data set based on the
     * <code>PropertyChangeEvent<code>.
     *
     */
    public void doLoadInThread() {
        showWaitCursor();
        List frames = getFrameList();
        if (frames.size() < 1) {
        	LogUtil.userMessage("Connection to McIDAS-X Bridge Listener at " + getHost() + ":" + getPort() + " failed");
            showNormalCursor();
        	return;
        }

        Hashtable ht = new Hashtable();
        ht.put(edu.wisc.ssec.mcidasv.chooser.FrameChooser.FRAME_NUMBERS_KEY, getFrameNumbers());
        if (getFrameCount() > 1) {
           ht.put(edu.wisc.ssec.mcidasv.chooser.FrameChooser.DATA_NAME_KEY,"Frame Sequence");
        } else {
           ht.put(edu.wisc.ssec.mcidasv.chooser.FrameChooser.DATA_NAME_KEY,"Frame");
        }
        ht.put(edu.wisc.ssec.mcidasv.chooser.FrameChooser.REQUEST_HOST, getHost());
        ht.put(edu.wisc.ssec.mcidasv.chooser.FrameChooser.REQUEST_PORT, getPort());
        ht.put(edu.wisc.ssec.mcidasv.chooser.FrameChooser.REQUEST_KEY, this.mcidasxInfo.getKeyString());
        //System.out.println("    ht:  " + ht);
        makeDataSource("", "MCIDASX", ht);
        showNormalCursor();
    }
    

    /**
     * Make the GUI
     *
     * @return The GUI
     */
    private JLabel statusLabel = new JLabel("Status");
    protected JButton addSourceButton = new JButton("Add Source");
    protected JButton helpButton = new JButton("Help");

    @Override
    public void setStatus(String statusString, String foo) {
    	if (statusString == null)
    		statusString = "";
    	statusLabel.setText(statusString);
    }
    
    protected JComponent doMakeContents() {
    	JPanel myPanel = new JPanel();
    	
    	
        JLabel hostLabel = new JLabel("Host:");
        hostLabel.setMaximumSize(new Dimension(ELEMENT_WIDTH, 24));
        hostLabel.setMinimumSize(new Dimension(ELEMENT_WIDTH, 24));
        hostLabel.setPreferredSize(new Dimension(ELEMENT_WIDTH, 24));
        hostLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        hostLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        hostLine.setText(mcidasxInfo.getHostString());
        hostLine.setMaximumSize(new Dimension(ELEMENT_DOUBLE_WIDTH, 24));
        hostLine.setMinimumSize(new Dimension(ELEMENT_DOUBLE_WIDTH, 24));
        hostLine.setPreferredSize(new Dimension(ELEMENT_DOUBLE_WIDTH, 24));
        hostLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) { setHost(); }
        });
        hostLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) { setHost(); }
        });
        
        JLabel portLabel = new JLabel("Port:");
        portLabel.setMaximumSize(new Dimension(ELEMENT_WIDTH, 24));
        portLabel.setMinimumSize(new Dimension(ELEMENT_WIDTH, 24));
        portLabel.setPreferredSize(new Dimension(ELEMENT_WIDTH, 24));
        portLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        portLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        portLine.setText(mcidasxInfo.getPortString());
        portLine.setMaximumSize(new Dimension(ELEMENT_DOUBLE_WIDTH, 24));
        portLine.setMinimumSize(new Dimension(ELEMENT_DOUBLE_WIDTH, 24));
        portLine.setPreferredSize(new Dimension(ELEMENT_DOUBLE_WIDTH, 24));
        portLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) { setPort(); }
        });
        portLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) { setPort(); }
        });
        
        JLabel statusLabelLabel = new JLabel("");
        statusLabelLabel.setMaximumSize(new Dimension(ELEMENT_WIDTH, 24));
        statusLabelLabel.setMinimumSize(new Dimension(ELEMENT_WIDTH, 24));
        statusLabelLabel.setPreferredSize(new Dimension(ELEMENT_WIDTH, 24));
        statusLabelLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        statusLabelLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        statusLabel.setText("Press \"Add Source\" to connect to the McIDAS-X Bridge Listener");
        statusLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusLabel.setForeground(new Color(0, 95, 255));
            	
        helpButton.setMaximumSize(new Dimension(ELEMENT_WIDTH, 24));
        helpButton.setMinimumSize(new Dimension(ELEMENT_WIDTH, 24));
        helpButton.setPreferredSize(new Dimension(ELEMENT_WIDTH, 24));
        helpButton.setActionCommand(GuiUtils.CMD_HELP);
        helpButton.addActionListener(this);

        addSourceButton.setMaximumSize(new Dimension(ELEMENT_DOUBLE_WIDTH, 24));
        addSourceButton.setMinimumSize(new Dimension(ELEMENT_DOUBLE_WIDTH, 24));
        addSourceButton.setPreferredSize(new Dimension(ELEMENT_DOUBLE_WIDTH, 24));
        addSourceButton.setActionCommand(CMD_LOAD);
        addSourceButton.addActionListener(this);
        
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(hostLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(hostLine))
                    .add(layout.createSequentialGroup()
                        .add(portLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(portLine))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(helpButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(addSourceButton))
                    .add(layout.createSequentialGroup()
                        .add(statusLabelLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(statusLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())         
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(hostLine)
                    .add(hostLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(portLine)
                    .add(portLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(statusLabelLabel)
                    .add(statusLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                	.add(addSourceButton)
                    .add(helpButton))
                .addContainerGap())
        );
        
        return myPanel;
        
    }

}
