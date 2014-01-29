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
package edu.wisc.ssec.mcidasv.chooser.adde;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;

import java.util.Hashtable;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.w3c.dom.Element;

import ucar.unidata.data.imagery.ImageDataset;
import ucar.unidata.idv.chooser.IdvChooserManager;

import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;


/**
 * Widget to select images from a remote ADDE server
 * Displays a list of the descriptors (names) of the image datasets
 * available for a particular ADDE group on the remote server.
 *
 * @author Don Murray
 */
public class AddeBasicImageChooser extends AddeImageChooser {

    /**
     * Construct an Adde image selection widget
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeBasicImageChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }
        
    /**
     *  Get the default value for a key
     *
     *  @param property      property (key type)
     *  @param dflt        default value
     *  @return value for key or dflt if not found
     */
    protected String getDefault(String property, String dflt) {
        if (property.equals(PROP_UNIT)) return "";
        if (property.equals(PROP_BAND)) return "ALL";
        return super.getDefault(property, dflt);
    }
    
    /**
     * Set the available units in the  unit selector
     *
     * @param ad   AreaDirectory for the image
     * @param band band to use for units
     */
    protected void setAvailableUnits(AreaDirectory ad, int band) {
        super.setAvailableUnits(ad, band);
        unitComboBox.setSelectedItem(ALLUNITS);
    }
    
    /**
     * User said go, we go. Simply get the list of images
     * from the imageChooser and create the ADDE.IMAGE.V
     * DataSource
     *
     */
    public void doLoadInThread() {
        if ( !getGoodToGo()) {
            updateStatus();
            return;
        }

        List imageList = getImageList();
        if(imageList==null || imageList.size()==0) return;
        ImageDataset ids = new ImageDataset(getDatasetName(), imageList);

        Hashtable ht = new Hashtable();
        getDataSourceProperties(ht);
        ht.put("preview", true);
        makeDataSource(ids, "ADDE.IMAGE.V", ht);
        saveServerState();
    }

    /**
     * Make the UI for this selector.
     * 
     * @return The gui
     */   
    public JComponent doMakeContents() {
        JPanel myPanel = new JPanel();

        JLabel timesLabel = McVGuiUtils.makeLabelRight("Times:");
        addDescComp(timesLabel);

        JPanel timesPanel = makeTimesPanel();
        timesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        addDescComp(timesPanel);

        JLabel navigationLabel = McVGuiUtils.makeLabelRight("Navigation:");
        addDescComp(navigationLabel);
        
        // Use processPropertyComponents to build combo boxes that we rely on
        processPropertyComponents();
        addDescComp(navComboBox);
        McVGuiUtils.setComponentWidth(navComboBox, McVGuiUtils.Width.DOUBLE);
                        
        GroupLayout layout = new GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(descriptorLabel)
                                        .addGap(GAP_RELATED)
                                        .addComponent(descriptorComboBox))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(timesLabel)
                                                .addGap(GAP_RELATED)
                                                .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addGroup(layout.createSequentialGroup()
                                                        .addComponent(navigationLabel)
                                                        .addGap(GAP_RELATED)
                                                        .addComponent(navComboBox))))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(BASELINE)
                                .addComponent(descriptorLabel)
                                .addComponent(descriptorComboBox))
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(LEADING)
                                        .addComponent(timesLabel)
                                        .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addPreferredGap(RELATED)
                                        .addGroup(layout.createParallelGroup(LEADING)
                                                .addComponent(navigationLabel)
                                                .addComponent(navComboBox)))
        );
        
        setInnerPanel(myPanel);
        return super.doMakeContents(true);
    }

}
