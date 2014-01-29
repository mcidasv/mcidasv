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
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.GroupLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;

import org.w3c.dom.Element;

import ucar.unidata.data.DataSource;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.ui.ChooserList;
import ucar.unidata.util.GuiUtils;

import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

/**
 * A chooser for adde front products
 *
 *
 *
 * @author IDV development team
 * @version $Revision$Date: 2011/03/24 16:06:32 $
 */
public class AddeFrontChooser extends AddeChooser {
    /** for gui */
    ChooserList timesList;

    /** for gui */
    JRadioButton forecastBtn;

    /** for gui */
    JRadioButton observedBtn;

    /**
     * Make a new one
     *
     * @param mgr The manager
     * @param root The xml element that defined this object
     *
     */
    public AddeFrontChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        
        addServerComp(loadButton);

    }

    public void readTimes() {
    	if (canAccessServer())
    		setState(STATE_CONNECTED);
    	else
    		setState(STATE_UNCONNECTED);
    }

    
    /**
     * update the buttons
     */
    protected void updateStatus() {
        super.updateStatus();
        setHaveData(true);
    }

    /**
     * Update the widget with the latest data.
     *
     * @throws Exception On badness
     */
    @Override public void handleUpdate() throws Exception {
//        updateServerList();
        readTimes();
        updateStatus();
        showNormalCursor();
        saveServerState();
    }
    
    protected boolean haveDescriptorSelected() {
    	return true;
    }

    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    public JComponent doMakeContents() {
    	JPanel myPanel = new JPanel();

    	JLabel frontLabel = new JLabel();
        observedBtn = new JRadioButton("Analysis Fronts", true);
        forecastBtn = new JRadioButton("Forecast Fronts", false);
                
        frontLabel = McVGuiUtils.makeLabelRight("Latest:");

        GuiUtils.buttonGroup(observedBtn, forecastBtn);

        frontLabel.setEnabled(false);
        observedBtn.setEnabled(false);
        forecastBtn.setEnabled(false);
                
        addServerComp(frontLabel);
        addServerComp(observedBtn);
        addServerComp(forecastBtn);
        
        timesList = new ChooserList();
        timesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Vector items = new Vector();
        for (int i = 0; i < 10; i++) {
            if (i == 0) {
                items.add("Most recent day");
            } else {
                items.add((i + 1) + " most recent days");
            }
        }
        timesList.setListData(items);
        timesList.setSelectedIndex(0);
        timesList.getScroller().setPreferredSize(new Dimension(200, 100));
        //        comps.add(GuiUtils.rLabel("Days:"));
        //        comps.add(GuiUtils.left(timesList.getScroller()));
        
        GroupLayout layout = new GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(frontLabel)
                .addGap(GAP_RELATED)
                .addComponent(observedBtn)
                .addGap(GAP_RELATED)
                .addComponent(forecastBtn)
                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(frontLabel)
                    .addComponent(observedBtn)
                    .addComponent(forecastBtn))
                .addGap(GAP_UNRELATED))
        );
        
        setInnerPanel(myPanel);
        return super.doMakeContents();
    }


    /**
     * User said go, we go. Simply get the list of images
     * from the imageChooser and create the ADDE.IMAGE
     * DataSource
     *
     */
    public void doLoadInThread() {
        List   urls   = new ArrayList();
        int    index  = timesList.getSelectedIndex();
        String server = getAddeServer().getName();
        String type   = (forecastBtn.isSelected()
                         ? "SRP"
                         : "SUS&wmo=ASUS01");
        for (int i = 0; i <= index; i++) {
            String url = "adde://" + server
                         + "/wxtext?group=RTWXTEXT&apro=COD&astn=" + type
                         + "&day=%DAY-" + i + "%";
            urls.add(url);
        }
        Hashtable ht = new Hashtable();
        //TODO: Change the name, maybe add the date
        ht.put(DataSource.PROP_TITLE, (forecastBtn.isSelected()
                                       ? "Forecast Front"
                                       : "Analysis Fronts"));
        makeDataSource(urls, "TEXT.FRONT", ht);
        saveServerState();
    }

    /**
     * get the adde server grup type to use
     *
     * @return group type
     */
    @Override protected String getGroupType() {
        return "text";
    }

    public String getDataType() {
        return "TEXT";
    }
}

