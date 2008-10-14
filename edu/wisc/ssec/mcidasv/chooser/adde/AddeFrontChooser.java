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

package edu.wisc.ssec.mcidasv.chooser.adde;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import org.jdesktop.layout.GroupLayout.Group;
import org.w3c.dom.Element;

import ucar.unidata.data.DataSource;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.ui.ChooserList;
import ucar.unidata.util.GuiUtils;

/**
 * A chooser for adde front products
 *
 *
 *
 * @author IDV development team
 * @version $Revision$Date: 2008/10/08 19:29:55 $
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
        
        addServerComp(addSourceButton);

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
                
        frontLabel.setText("Latest:");
        frontLabel.setMinimumSize(new Dimension(ELEMENT_WIDTH, 24));
        frontLabel.setMaximumSize(new Dimension(ELEMENT_WIDTH, 24));
        frontLabel.setPreferredSize(new Dimension(ELEMENT_WIDTH, 24));
        frontLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        frontLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        GuiUtils.buttonGroup(observedBtn, forecastBtn);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(frontLabel)
                .add(GAP_RELATED)
                .add(observedBtn)
                .add(GAP_RELATED)
                .add(forecastBtn)
                .addContainerGap(63, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(frontLabel)
                    .add(observedBtn)
                    .add(forecastBtn))
                .add(63, 63, 63))
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
        String server = getAddeServer("AddeFrontChooser.doLoadInThread").getName();
        String type   = (forecastBtn.isSelected()
                         ? "SRP"
                         : "SUS");
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

