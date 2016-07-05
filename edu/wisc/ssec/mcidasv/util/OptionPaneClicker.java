/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2016
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

package edu.wisc.ssec.mcidasv.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionPaneClicker {

    private static final Logger logger =
        LoggerFactory.getLogger(OptionPaneClicker.class);

    private final Thread clickDelayer;

    public OptionPaneClicker(JOptionPane optionPane,
                             String dialogTitle,
                             long clickDelay,
                             String buttonText)
    {
        JDialog frame = optionPane.createDialog(null, dialogTitle);
        frame.setModal(true);
        optionPane.selectInitialValue();
        optionPane.setVisible(true);

        clickDelayer = new Thread() {
            public void run() {
                try {
                    sleep(clickDelay);
                } catch (InterruptedException e) {
                    logger.error("Click delayer interrupted!", e);
                } finally {
                    final JButton btn = getButton(frame, buttonText);
                    if (btn != null) {
                        EventQueue.invokeLater(btn::doClick);
                    }
                }
            }
        };

        simulateClick();
    }

    private void simulateClick() {
        clickDelayer.start();
    }

    public static JButton getButton(Container container, String text) {
        JButton btn = null;
        List<Container> children = new ArrayList<>(25);
        for (Component child : container.getComponents()) {
            if (child instanceof JButton) {
                JButton button = (JButton)child;
                if (text.equals(button.getText())) {
                    btn = button;
                    break;
                }
            } else if (child instanceof Container) {
                children.add((Container)child);
            }
        }
        if (btn == null) {
            for (Container cont : children) {
                JButton button = getButton(cont, text);
                if (button != null) {
                    btn = button;
                    break;
                }
            }
        }
        return btn;
    }
}
