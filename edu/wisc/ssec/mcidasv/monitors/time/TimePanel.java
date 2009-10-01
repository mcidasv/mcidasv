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
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(timeLabel)
                .addContainerGap()));

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(timeLabel)));
    }
}
