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
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(memoryLabel)
                .addContainerGap()));

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(memoryLabel)));

        memoryLabel.setToolTipText("Used memory/Max used memory/Max memory");
        memoryLabel.setOpaque(true);
        memoryLabel.setBackground(MemoryMonitor.doColorThing(0));
    }
}
