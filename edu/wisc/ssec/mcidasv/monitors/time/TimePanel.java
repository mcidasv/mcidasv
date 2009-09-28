package edu.wisc.ssec.mcidasv.monitors.time;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.wisc.ssec.mcidasv.monitors.MonitorEvent;
import edu.wisc.ssec.mcidasv.monitors.Monitoring;
import edu.wisc.ssec.mcidasv.monitors.MonitorManager.MonitorType;

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
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(timeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(timeLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }
}
