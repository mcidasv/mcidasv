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
package edu.wisc.ssec.mcidasv.monitors.time;

import static javax.swing.GroupLayout.Alignment.LEADING;

import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.wisc.ssec.mcidasv.monitors.MonitorEvent;
import edu.wisc.ssec.mcidasv.monitors.MonitorManager.MonitorType;
import edu.wisc.ssec.mcidasv.monitors.Monitoring;

@SuppressWarnings("serial")
public class TimePanel extends JPanel implements Monitoring {
    private JLabel timeLabel = new JLabel("");

    public TimePanel() {
        initComponents();
    }

    // runs in the EDT! be aware!
    public void monitorUpdated(final MonitorEvent event) {
        if (event.getType() != MonitorType.TIME)
            return;

        TimeMonitorEvent timeEvent = (TimeMonitorEvent)event;
        timeLabel.setText(timeEvent.getOutput());
        repaint();
    }

    private void initComponents() {
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(timeLabel)
                .addContainerGap()));

        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(timeLabel)));
    }
}
