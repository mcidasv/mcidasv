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

package edu.wisc.ssec.mcidasv.ui;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.ui.SideLegend;
import ucar.unidata.idv.ui.SideLegend.CategoryPanel;
//import ucar.unidata.idv.ui.SideLegend.DisplayControlLegendPanel;
import ucar.unidata.util.GuiUtils;

public class McvSideLegend extends SideLegend {

	// since you're gonna have to redo a significant portion of this, you might
	// as well add in something to convey the ViewManager's active layer...
	// may as well make it a tree, too.
	// parent nodes don't get checks unless *all* kids are checked
	// i like the UI from this link:
	// http://rabbit-hole.blogspot.com/2006/11/tree-and-tree-table-with-checkboxes.html
//	@Override protected void fillLegendSafely() {
//        List controls = viewManager.getControlsForLegend();
//        for (int i = 0; i < categoryPanels.size(); i++) {
//            CategoryPanel categoryPanel =
//                (CategoryPanel) categoryPanels.get(i);
//            //            categoryPanel.clear();
//            categoryPanel.reInitialize();
//            categoryPanel.getContents().setVisible(true);
//        }
//
//        Hashtable seen = new Hashtable();
//        for (int i = controls.size() - 1; i >= 0; i--) {
//            final DisplayControl control  = (DisplayControl) controls.get(i);
//            String               category = control.getDisplayCategory();
//            System.err.println("debug: category=" + category);
//            if ((category == null) || (category.length() == 0)) {
//                category = "Displays";
//            }
//            CategoryPanel categoryPanel =
//               (CategoryPanel) categoryToPanel.get(category);
//            if (categoryPanel == null) {
//                categoryPanel = new CategoryPanel(this, category);
//                categoryPanels.add(categoryPanel);
//                categoryToPanel.put(category, categoryPanel);
//                Boolean b = (Boolean) categoryToPanelOpen.get(category);
//                if (b != null) {
//                    categoryPanel.setInnerVisible(b.booleanValue());
//                }
//            }
//            seen.put(categoryPanel, categoryPanel);
//            DisplayControlLegendPanel legendPanel =
//                (DisplayControlLegendPanel) control.getTransientProperty(
//                    "SIDELEGEND");
//            if (legendPanel == null) {
//                JCheckBox    visCbx = control.doMakeVisibilityControl("");
//                ItemListener itemListener = new ItemListener() {
//                    public void itemStateChanged(ItemEvent event) {
//                        displayControlVisibilityChanged(control,
//                                event.getStateChange() == ItemEvent.SELECTED);
//                    }
//                };
//                visCbx.addItemListener(itemListener);
//                visCbx.setBorder(BorderFactory.createEmptyBorder());
//                JComponent sideLegendLabel =
//                    control.getLegendLabel(control.SIDE_LEGEND);
//                sideLegendLabel.setBorder(BorderFactory.createEmptyBorder(0,
//                        5, 0, 3));
//                legendPanel = new DisplayControlLegendPanel(control,
//                        sideLegendLabel, false,
//                        GuiUtils.inset(visCbx, new Insets(0, 0, 0, 2)),
//                        control.getLegendButtons(control.SIDE_LEGEND));
//
//                JComponent controlLegend =
//                    control.getLegendComponent(control.SIDE_LEGEND);
//                legendPanel.add(controlLegend, false);
//                control.putTransientProperty("SIDELEGEND", legendPanel);
//            }
//            categoryPanel.add(control, legendPanel.getContents());
//        }
//
//        List orderedCategoryPanels = new ArrayList();
//        for (int i = 0; i < categoryPanels.size(); i++) {
//            CategoryPanel categoryPanel =
//                (CategoryPanel) categoryPanels.get(i);
//            if (seen.get(categoryPanel) != null) {
//                orderedCategoryPanels.add(categoryPanel.getContents());
//            } else {
//                categoryPanel.clear();
//            }
//        }
//
//
//        JPanel panels = GuiUtils.vbox(orderedCategoryPanels);
//        //        synchronized (legendsPanel.getTreeLock()) {
//        legendsPanel.removeAll();
//        legendsPanel.add(panels, BorderLayout.NORTH);
//        legendsPanel.invalidate();
//        //        }
//    }

	
}
