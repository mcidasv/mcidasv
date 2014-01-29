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

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.XmlHandler;
import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.WmsUtil;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Position;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.TextColor;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;

/**
 * This handles a variety of flavors of xml documents (e.g., thredds
 * query capability, thredds catalogs, idv menus) to create data
 * choosers from. It provides a combobox to enter urls to xml
 * documents. It retrieves the xml and creates a {@link XmlHandler}
 * based on the type of xml. Currently this class handles two
 * types of xml: Thredds catalog and Web Map Server (WMS)
 * capability documents. The XmlHandler does most of the work.
 * <p>
 * This class maintains the different xml docs the user has gone
 * to coupled with the XmlHandler for each doc. It uses this list
 * to support navigating back and forth through the history of
 * documents.
 *
 * @author IDV development team
 * @version $Revision$Date: 2011/03/24 16:06:31 $
 */


public class XmlChooser extends ucar.unidata.idv.chooser.XmlChooser implements Constants {
    
    /** Catalog browser panel */
    JPanel catalogPanel;
    
    /** Catalog browser panel label history */
    int labelHistoryIdx = -1;
    List labelHistory = new ArrayList();
    
    /**
     * Create the <code>XmlChooser</code>
     *
     * @param mgr The <code>IdvChooserManager</code>
     * @param root  The xml root that defines this chooser
     *
     */
    public XmlChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        
        loadButton = McVGuiUtils.makeImageTextButton(ICON_ACCEPT_SMALL, getLoadCommandName());
        loadButton.setActionCommand(getLoadCommandName());
        loadButton.addActionListener(this);

    }
    
    private void repaintCatalog() {
        if (catalogPanel != null) {
            catalogPanel.invalidate();
            catalogPanel.validate();
            catalogPanel.repaint();
        }
        String labelName = (String)labelHistory.get(labelHistoryIdx);
        catalogPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(labelName));
    }
    
    /**
     *  Go back and display  the previous  document.
     */
    public void goBack() {
        super.goBack();
        labelHistoryIdx--;
        repaintCatalog();
    }

    /**
     *  Go forward and display  the next   document in the history list.
     */
    public void goForward() {
        super.goForward();
        labelHistoryIdx++;
        repaintCatalog();
    }
    
    /**
     * Add a labeled border to the catalog browser indicating the data type
     */
    protected void makeUi(Document doc, Element xmlRoot, String path) {
        super.makeUi(doc, xmlRoot, path);

        String tagName = xmlRoot.getTagName();
        String labelName = "Unknown Catalog Type";
        if (tagName.equals(WmsUtil.TAG_WMS1) || tagName.equals(WmsUtil.TAG_WMS2)) labelName = "Web Map Service";
        else if (tagName.equals(CatalogUtil.TAG_CATALOG)) labelName = "THREDDS";
        else if (tagName.equals("menus")) labelName = "Menu";
        
        labelHistoryIdx++;
        labelHistory.add(labelHistoryIdx, labelName);
        repaintCatalog();
    }
        
    private JLabel statusLabel = new JLabel("Status");

    @Override
    public void setStatus(String statusString, String foo) {
        if (statusString == null)
            statusString = "";
        statusLabel.setText(statusString);
    }

    /**
     *  Create and return the Gui contents.
     *
     *  @return The gui contents.
     */
    protected JComponent doMakeContents() {
        JComponent parentContents = super.doMakeContents();
        
        // Pull apart the panels
        // Expected:
        // Top: URL chooser
        // Center: Catalog chooser
        // Bottom: chooser buttons
        // This takes a bit of digging--some of the components are really buried!
        Component[] parentComps = parentContents.getComponents();
        if (parentComps.length != 3 ||
                !(parentComps[0] instanceof JPanel) ||
                !(parentComps[1] instanceof JPanel) ||
                !(parentComps[2] instanceof JPanel)
        ) return parentContents;
        
        // Assign file picker to fileComponent
        JPanel topPanel = (JPanel)parentComps[0];
        Component[] panels = topPanel.getComponents();
        if (panels.length != 2 ||
                !(panels[0] instanceof JPanel) ||
                !(panels[1] instanceof JPanel)
        ) return parentContents;
        // Found the navigation panel
        JPanel navigationPanel = (JPanel)panels[0];
        panels = ((JPanel)panels[1]).getComponents();
        if (panels.length != 3 ||
                !(panels[0] instanceof JLabel) ||
                !(panels[1] instanceof JPanel) ||
                !(panels[2] instanceof JButton)
        ) return parentContents;
        // Found the button
        JButton fileButton = (JButton)panels[2];
        panels = ((JPanel)panels[1]).getComponents();
        if (panels.length != 1 ||
                !(panels[0] instanceof JComboBox)
        ) return parentContents;
        JComboBox fileComponent = (JComboBox)panels[0];
        McVGuiUtils.setButtonImage(fileButton, ICON_OPEN_SMALL);
        McVGuiUtils.setComponentWidth(fileButton, Width.DOUBLE);
        McVGuiUtils.setComponentWidth(fileComponent, Width.DOUBLEDOUBLE);
        McVGuiUtils.setComponentHeight(fileComponent, fileButton);

        // Deal with the navigation buttons
        panels = navigationPanel.getComponents();
        if (panels.length != 2 ||
                !(panels[0] instanceof JButton) ||
                !(panels[1] instanceof JButton)
        ) return parentContents;
        McVGuiUtils.setButtonImage((JButton)panels[0], Constants.ICON_PREVIOUS_SMALL);
        McVGuiUtils.setButtonImage((JButton)panels[1], Constants.ICON_NEXT_SMALL);
        JLabel navigationLabel = McVGuiUtils.makeLabelRight("History:");
        navigationPanel = GuiUtils.hbox(panels[0], panels[1]);
        
        // Rearrange the catalog browser and assign it to innerPanel
        catalogPanel = (JPanel)parentComps[1];
        JPanel innerPanel = McVGuiUtils.makeLabeledComponent("Browse:", catalogPanel);

        // Start building the whole thing here
        JPanel outerPanel = new JPanel();

        JLabel fileLabel = McVGuiUtils.makeLabelRight("Catalog:");

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
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(navigationLabel)
                                                                .addGap(GAP_RELATED)
                                                                .addComponent(navigationPanel))
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
                                                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(navigationLabel)
                    .addComponent(navigationPanel))
                .addPreferredGap(RELATED)

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

