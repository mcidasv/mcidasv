package edu.wisc.ssec.mcidasv.monitors.memory;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.wisc.ssec.mcidasv.monitors.MonitorEvent;
import edu.wisc.ssec.mcidasv.monitors.Monitoring;
import edu.wisc.ssec.mcidasv.monitors.MonitorManager.MonitorType;

@SuppressWarnings("serial")
public class MemoryPanel extends JPanel implements Monitoring {
    private final JLabel memoryLabel = new JLabel("");

    public MemoryPanel() {
        initComponents();
    }

    // runs in the EDT! be cautious!
    public void monitorUpdated(final MonitorEvent event) {
        if (event.getType() != MonitorType.MEMORY)
            return;

        MemoryMonitorEvent memEvent = (MemoryMonitorEvent)event;
        memoryLabel.setText(memEvent.getReadout());
        memoryLabel.setBackground(memEvent.getColor());
        repaint();
    }

    private void initComponents() {
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(memoryLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(memoryLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        memoryLabel.setToolTipText("Used memory/Max used memory/Max memory");
        memoryLabel.setOpaque(true);
        memoryLabel.setBackground(MemoryMonitor.doColorThing(0));
    }
}
