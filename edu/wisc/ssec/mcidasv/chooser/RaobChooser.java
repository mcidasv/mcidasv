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
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.xml.XmlUtil;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Position;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.TextColor;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;


/**
 * A chooser class for selecting Raob data.
 * Mostly just a wrapper around a
 *  {@link ucar.unidata.view.sounding.SoundingSelector}
 * that does most of the work
 *
 * @author IDV development team
 * @version $Revision$Date: 2011/03/24 16:06:31 $
 */


public class RaobChooser extends ucar.unidata.idv.chooser.RaobChooser implements Constants {
    
    /**
     * Construct a <code>RaobChooser</code> using the manager
     * and the root XML that defines this object.
     *
     * @param mgr  <code>IdvChooserManager</code> that controls this chooser.
     * @param root root element of the XML that defines this object
     */
    public RaobChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }
        
    /**
     * Make the contents
     *
     * @return  the contents
     */
    protected JPanel doMakeInnerPanel(JPanel fromPanel) {

        // Get the station panel
        Component[] fromComps = fromPanel.getComponents();
        
        if (fromComps.length != 2 ||
                !(fromComps[0] instanceof JPanel) ||
                !(fromComps[1] instanceof JPanel)
        ) return fromPanel;
        JComponent stationPanel = (JPanel)fromComps[1];
        // TODO: Yup, these are magic dimension numbers
        stationPanel.setPreferredSize(new Dimension(300, 252));
        Color bgcolor = stationPanel.getBackground();

        // Get the times panel
        Component[] panels = ((JPanel)fromComps[0]).getComponents();
        if (panels.length < 1 ||
                !(panels[0] instanceof JPanel)
        ) return fromPanel;
        panels = ((JPanel)panels[0]).getComponents();
        if (panels.length != 4 ||
                !(panels[0] instanceof JLabel) ||
                !(panels[1] instanceof JScrollPane) ||
                !(panels[2] instanceof JLabel) ||
                !(panels[3] instanceof JScrollPane)
        ) return fromPanel;
        
        JScrollPane availablePanel = (JScrollPane)panels[1];
        // TODO: Yup, these are magic dimension numbers
        availablePanel.setPreferredSize(new Dimension(180, 50));
        availablePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Available"));
        availablePanel.setBackground(bgcolor);
        JScrollPane selectedPanel = (JScrollPane)panels[3];
        // TODO: Yup, these are magic dimension numbers
        selectedPanel.setPreferredSize(new Dimension(170, 50));
        selectedPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected"));
        selectedPanel.setBackground(bgcolor);
        
        // Make the container panel
        JPanel timesPanel = new JPanel();
        
        GroupLayout timesLayout = new GroupLayout(timesPanel);
        timesPanel.setLayout(timesLayout);
        timesLayout.setHorizontalGroup(
                timesLayout.createParallelGroup(LEADING)
            .addGroup(timesLayout.createSequentialGroup()
                .addComponent(availablePanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(GAP_RELATED)
                .addComponent(selectedPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                )
        );
        timesLayout.setVerticalGroup(
                timesLayout.createParallelGroup(LEADING)
            .addGroup(timesLayout.createSequentialGroup()
                .addGroup(timesLayout.createParallelGroup(TRAILING)
                    .addComponent(selectedPanel, LEADING, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(availablePanel, LEADING, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                    )
        );
        
        // TODO: Yup, these are magic dimension numbers
        JComponent temp = new JPanel();
        temp.setPreferredSize(new Dimension(150, 150));
        temp.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        McVGuiUtils.setComponentHeight(timesPanel, temp);

        JPanel myPanel = new JPanel();
        
        JLabel descriptorLabelStatic = McVGuiUtils.makeLabelRight("Soundings:");
        JLabel descriptorString = new JLabel("Upper air mandatory and significant levels");
        McVGuiUtils.setLabelBold(descriptorString, true);
        
        JLabel stationLabel = McVGuiUtils.makeLabelRight("Stations:");
        
        JLabel timesLabel = McVGuiUtils.makeLabelRight("");
                
        GroupLayout layout = new GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(descriptorLabelStatic)
                        .addGap(GAP_RELATED)
                        .addComponent(descriptorString))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(stationLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(stationPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(timesLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(descriptorLabelStatic)
                    .addComponent(descriptorString))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(stationLabel)
                    .addComponent(stationPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(LEADING)
                    .addComponent(timesLabel)
                    .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(RELATED))
        );
        
        return myPanel;
    }
    
    private JLabel statusLabel = new JLabel("Status");

    @Override
    public void setStatus(String statusString, String foo) {
        if (statusString == null)
            statusString = "";
        statusLabel.setText(statusString);
    }

    /**
     * Make the GUI
     *
     * @return The GUI
     */
    protected JComponent doMakeContents() {
        Element chooserNode = getXmlNode();
        XmlUtil.setAttributes(chooserNode, new String[] { ATTR_SHOWSERVER, "false" });
        JComponent parentContents = super.doMakeContents();

        // Pull apart the panels
        // Expected:
        // Top: file chooser
        // Center: sounding selector
        // Bottom: chooser buttons
        // This takes a bit of digging--some of the components are really buried!
        Component[] parentComps = parentContents.getComponents();
        
        // Dig down through all the GuiUtils parents
        parentComps = ((JComponent)parentComps[0]).getComponents();
        parentComps = ((JComponent)parentComps[0]).getComponents();
        parentComps = ((JComponent)parentComps[0]).getComponents();
        
        if (parentComps.length != 3 ||
                !(parentComps[0] instanceof JPanel) ||
                !(parentComps[1] instanceof JPanel) ||
                !(parentComps[2] instanceof JPanel)
        ) return parentContents;
        
        // Assign sounding selector file picker to typeComponent
        JPanel topPanel = (JPanel)parentComps[0];
        Component[] panels = topPanel.getComponents();
        if (panels.length < 1 ||
                !(panels[0] instanceof JPanel)
        ) return parentContents;
        panels = ((JPanel)panels[0]).getComponents();
        if (panels.length != 2 ||
                !(panels[0] instanceof JPanel) ||
                !(panels[1] instanceof JPanel)
        ) return parentContents;
        panels = ((JPanel)panels[0]).getComponents();
        if (panels.length != 2 ||
                !(panels[0] instanceof JLabel) ||
                !(panels[1] instanceof JPanel)
        ) return parentContents;
        panels = ((JPanel)panels[1]).getComponents();
        if (panels.length != 2 ||
                !(panels[0] instanceof JTextField) ||
                !(panels[1] instanceof JButton)
        ) return parentContents;
        JTextField fileComponent = (JTextField)panels[0];
        JButton fileButton = (JButton)panels[1];
        McVGuiUtils.setButtonImage(fileButton, ICON_OPEN_SMALL);
        McVGuiUtils.setComponentWidth(fileButton, Width.DOUBLE);
        McVGuiUtils.setComponentHeight(fileComponent, fileButton);

        // Rearrange the sounding selector and assign it to innerPanel
        JPanel innerPanel = doMakeInnerPanel((JPanel)parentComps[1]);

        // Assign sounding selector loadButton to the chooser
        JPanel bottomPanel = (JPanel)parentComps[2];
        Component[] buttons = bottomPanel.getComponents();
                
        // Dig down through all the GuiUtils parents
        buttons = ((JPanel)buttons[1]).getComponents();
        buttons = ((JPanel)buttons[1]).getComponents();
        buttons = ((JPanel)buttons[0]).getComponents();
        buttons = ((JPanel)buttons[0]).getComponents();

        for (Component button : buttons) {
            if (button instanceof JButton &&
                    ((JButton)button).getText() == getLoadCommandName()) {
                loadButton = (JButton)button;
                break;
            }
        }
        if (loadButton==null) return parentContents;
        
        statusLabel.setEnabled(false);
        setStatus("Status unavailable");

        // Start building the whole thing here
        JPanel outerPanel = new JPanel();

        JLabel fileLabel = McVGuiUtils.makeLabelRight("File:");

        JLabel statusLabelLabel = McVGuiUtils.makeLabelRight("");

        McVGuiUtils.setLabelPosition(statusLabel, Position.RIGHT);
        McVGuiUtils.setComponentColor(statusLabel, TextColor.STATUS);

        JButton helpButton = McVGuiUtils.makeImageButton(ICON_HELP, "Show help");
        helpButton.setActionCommand(GuiUtils.CMD_HELP);
        helpButton.addActionListener(this);

        JButton refreshButton = McVGuiUtils.makeImageButton(ICON_REFRESH, "Refresh");
        refreshButton.setActionCommand(GuiUtils.CMD_UPDATE);
        refreshButton.addActionListener(this);

        McVGuiUtils.setButtonImage(loadButton, ICON_ACCEPT_SMALL);
        McVGuiUtils.setComponentWidth(loadButton, Width.DOUBLE);

        GroupLayout layout = new GroupLayout(outerPanel);
        outerPanel.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(LEADING)
                .addGroup(TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(TRAILING)
                                .addGroup(layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(helpButton)
                                        .addGap(GAP_RELATED)
                                        .addComponent(refreshButton)
                                        .addPreferredGap(RELATED)
                                        .addComponent(loadButton))
                                        .addGroup(LEADING, layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addGroup(layout.createParallelGroup(LEADING)
                                                        .addComponent(innerPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(fileLabel)
                                                                .addGap(GAP_RELATED)
                                                                .addComponent(fileComponent)
                                                                .addGap(GAP_UNRELATED)
                                                                .addComponent(fileButton))
                                                                .addGroup(layout.createSequentialGroup()
                                                                        .addComponent(statusLabelLabel)
                                                                        .addGap(GAP_RELATED)
                                                                        .addComponent(statusLabel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)))))
                                                                        .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(BASELINE)
                                .addComponent(fileLabel)
                                .addComponent(fileComponent)
                                .addComponent(fileButton))
                                .addPreferredGap(UNRELATED)
                                .addComponent(innerPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(UNRELATED)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(statusLabelLabel)
                                        .addComponent(statusLabel))
                                        .addPreferredGap(UNRELATED)
                                        .addGroup(layout.createParallelGroup(BASELINE)
                                                .addComponent(loadButton)
                                                .addComponent(refreshButton)
                                                .addComponent(helpButton))
                                                .addContainerGap())
        );

        return outerPanel;
        
    }

}

