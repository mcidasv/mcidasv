/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
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
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.chooser;


import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.data.McIdasFrame;
import edu.wisc.ssec.mcidasv.data.McIdasXInfo;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Position;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.TextColor;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;


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
        
        loadButton = McVGuiUtils.makeImageTextButton(ICON_ACCEPT_SMALL, getLoadCommandName());
        loadButton.setActionCommand(getLoadCommandName());
        loadButton.addActionListener(this);
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

    private JLabel statusLabel = new JLabel("Status");

    @Override
    public void setStatus(String statusString, String foo) {
        if (statusString == null)
            statusString = "";
        statusLabel.setText(statusString);
    }

    /**
     * Make the GUI.
     *
     * @return The GUI.
     */
    protected JComponent doMakeContents() {
        JPanel myPanel = new JPanel();
        
        
        JLabel hostLabel = McVGuiUtils.makeLabelRight("Host:");
        
        hostLine.setText(mcidasxInfo.getHostString());
        McVGuiUtils.setComponentWidth(hostLine, Width.DOUBLE);
        hostLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) { setHost(); }
        });
        hostLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) { setHost(); }
        });
        
        JLabel portLabel = McVGuiUtils.makeLabelRight("Port:");

        portLine.setText(mcidasxInfo.getPortString());
        McVGuiUtils.setComponentWidth(portLine, Width.DOUBLE);
        portLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) { setPort(); }
        });
        portLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) { setPort(); }
        });
                
        JLabel statusLabelLabel = McVGuiUtils.makeLabelRight("");

        statusLabel.setText("Press \"" + getLoadCommandName() + "\" to connect to the McIDAS-X Bridge Listener");
        McVGuiUtils.setLabelPosition(statusLabel, Position.RIGHT);
        McVGuiUtils.setComponentColor(statusLabel, TextColor.STATUS);
                
        JButton helpButton = McVGuiUtils.makeImageButton(ICON_HELP, "Show help");
        helpButton.setActionCommand(GuiUtils.CMD_HELP);
        helpButton.addActionListener(this);
        
        McVGuiUtils.setComponentWidth(loadButton, Width.DOUBLE);
        
        GroupLayout layout = new GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(hostLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(hostLine))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(portLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(portLine))
                    .addGroup(TRAILING, layout.createSequentialGroup()
                        .addComponent(helpButton)
                        .addPreferredGap(RELATED)
                        .addComponent(loadButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(statusLabelLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(statusLabel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(hostLine)
                    .addComponent(hostLabel))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(portLine)
                    .addComponent(portLabel))
                .addPreferredGap(RELATED, DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(statusLabelLabel)
                    .addComponent(statusLabel))
                .addPreferredGap(UNRELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(loadButton)
                    .addComponent(helpButton))
                .addContainerGap())
        );
        
        return myPanel;
        
    }

}
