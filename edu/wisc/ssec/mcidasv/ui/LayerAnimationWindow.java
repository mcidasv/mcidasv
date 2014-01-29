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

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JToggleButton;

import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import net.miginfocom.swing.MigLayout;
import javax.swing.JTextField;
import javax.swing.JLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.idv.ViewManager;

public class LayerAnimationWindow extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(LayerAnimationWindow.class);
    
    private JPanel contentPane;
    private JTextField fieldCurrentDwell;
    private JToggleButton tglbtnEnableAnimation;
    private JButton btnSlower;
    private JButton btnFaster;
    private JLabel lblDwell; 
    private JLabel statusLabel;

    /**
     * Create the frame.
     */
    public LayerAnimationWindow() {
        setTitle("Animate Visibility");
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 208, 162);
        contentPane = new JPanel();
        setContentPane(contentPane);
        tglbtnEnableAnimation = new JToggleButton("Enable Animation");

        tglbtnEnableAnimation.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                tglbtnEnableAnimationChanged(event);
            }
        });
        btnSlower = new JButton("Slower");
        btnSlower.setEnabled(false);
        btnSlower.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                btnSlowerActionPerformed(event);
            }
        });

        btnFaster = new JButton("Faster");
        btnFaster.setEnabled(false);
        btnFaster.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                btnFasterActionPerformed(event);
            }
        });

        lblDwell = new JLabel("Dwell (s):");
        lblDwell.setFont(new Font("Lucida Grande", Font.PLAIN, 11));
        lblDwell.setEnabled(false);

        fieldCurrentDwell = new JTextField();
        fieldCurrentDwell.setEnabled(false);
        fieldCurrentDwell.setEditable(false);
        fieldCurrentDwell.setText("0.0");
        fieldCurrentDwell.setColumns(6);

        statusLabel = new JLabel("");
        statusLabel.setEnabled(false);

        contentPane.setLayout(new MigLayout("", "[grow][grow][][]", "[][][][]"));
        contentPane.add(tglbtnEnableAnimation, "flowy,cell 0 0 3 1,growx,aligny top");
        contentPane.add(btnSlower, "cell 0 1,alignx right,growy");
        contentPane.add(btnFaster, "cell 2 1,alignx left,growy");
        contentPane.add(lblDwell, "cell 0 2,alignx right,aligny baseline");
        contentPane.add(fieldCurrentDwell, "cell 2 2,alignx left,aligny baseline");
        contentPane.add(statusLabel, "cell 0 3 3 1");
    }

    // dear god! change thes
    private void tglbtnEnableAnimationChanged(final ItemEvent event) {
        logger.trace("toggle: {}", event);
        boolean animationEnabled = (event.getStateChange() == ItemEvent.SELECTED);
        btnSlower.setEnabled(animationEnabled);
        btnFaster.setEnabled(animationEnabled);
        ViewManager viewManager = getActiveViewManager();
        viewManager.setAnimatedVisibilityCheckBox(animationEnabled);
        double currentSpeed = viewManager.getVisibilityAnimationSpeed();
        String dwell = Double.toString(currentSpeed / 1000.0);
        fieldCurrentDwell.setText(dwell);
    }

    private void btnFasterActionPerformed(final ActionEvent event) {
        ViewManager viewManager = getActiveViewManager();
        viewManager.fasterVisibilityAnimation();
        double currentSpeed = viewManager.getVisibilityAnimationSpeed();
        String dwell = Double.toString(currentSpeed / 1000.0);
        fieldCurrentDwell.setText(dwell);
        logger.trace("faster: animationSpeed={}", dwell);
    }

    private void btnSlowerActionPerformed(final ActionEvent event) {
        ViewManager viewManager = getActiveViewManager();
        viewManager.slowerVisibilityAnimation();
        double currentSpeed = viewManager.getVisibilityAnimationSpeed();
        String dwell = Double.toString(currentSpeed / 1000.0);
        fieldCurrentDwell.setText(dwell);
        logger.trace("slower: animationSpeed={}", dwell);
    }

    private ViewManager getActiveViewManager() {
        List<ViewManager> viewManagers = McVGuiUtils.getActiveViewManagers();
        if (viewManagers.size() != 1) {
            statusLabel.setText("no multipanel support yet :(");
            logger.trace("woe betide the person venturing into shared groups");
        }
        ViewManager viewManager = viewManagers.get(0);
        logger.trace("found a ViewManager: name={} isActive={}", viewManager.getName(), viewManager.getIsActive());
        return viewManager;
    }

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    LayerAnimationWindow frame = new LayerAnimationWindow();
                    frame.setVisible(true);
                } catch (Exception e) {
                    logger.error("init window", e);
                }
            }
        });
    }
}
