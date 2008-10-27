package edu.wisc.ssec.mcidasv.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import ucar.unidata.util.CacheManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;

// initial version taken verbatim from Unidata :(
public class MemoryMonitor extends JPanel implements Runnable {

    /** flag for running */
    private boolean running = false;

    /** sleep interval */
    private final long sleepInterval = 2000;

    /** a thread */
    private Thread thread;

    /** percent threshold */
    private final int percentThreshold;

    /** number of times above the threshold */
    private int timesAboveThreshold = 0;

    /** format */
    private static DecimalFormat fmt = new DecimalFormat("#0");

    /** the label */
    private JLabel label = new JLabel("");

    /** Keep track of the last time we ran the gc and cleared the cache */
    private static long lastTimeRanGC = -1;

    /**
     * Default constructor
     */
    public MemoryMonitor() {
        this(80);
    }

    /**
     * Create a new MemoryMonitor
     * 
     * @param percentThreshold the percentage of use memory before garbage
     *        collection is run
     * 
     */
    public MemoryMonitor(final int percentThreshold) {
        super(new BorderLayout());
        Font f = label.getFont();
        label.setToolTipText("Used memory/Max used memory/Max memory");
        label.setFont(f);
        this.percentThreshold = percentThreshold;
        this.add(BorderLayout.CENTER, new Msg.SkipPanel(
            GuiUtils.hbox(Misc.newList(label))));

        MouseListener ml = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e))
                    popupMenu(e);
            }
        };

        label.addMouseListener(ml);
        label.setOpaque(true);
        label.setBackground(doColorThing(0.0));
        start();
    }

    /**
     * Popup a menu on an event
     * 
     * @param event the event
     */
    private void popupMenu(final MouseEvent event) {
        JPopupMenu popup = new JPopupMenu();
        if (running) {
            popup.add(GuiUtils.makeMenuItem("Stop Running",
                MemoryMonitor.this, "toggleRunning"));
        } else {
            popup.add(GuiUtils.makeMenuItem("Resume Running",
                MemoryMonitor.this, "toggleRunning"));
        }

        popup.add(GuiUtils.makeMenuItem("Clear Memory & Cache",
            MemoryMonitor.this, "runGC"));
        popup.show(this, event.getX(), event.getY());
    }

    /**
     * Toggle running
     */
    public void toggleRunning() {
        if (running) {
            stop();
        } else {
            start();
        }
    }

    /**
     * Set the label font
     * 
     * @param f the font
     */
    public void setLabelFont(final Font f) {
        label.setFont(f);
    }

    /**
     * Stop running
     */
    public synchronized void stop() {
        running = false;
        label.setEnabled(false);
    }

    /**
     * Start running
     */
    private synchronized void start() {
        if (running)
            return;

        label.setEnabled(true);
        running = true;
        thread = new Thread(this, "Memory monitor");
        thread.start();
    }

    /**
     * Run the GC and clear the cache
     */
    public void runGC() {
        CacheManager.clearCache();
        Runtime.getRuntime().gc();
        lastTimeRanGC = System.currentTimeMillis();
    }

    /**
     * Show the statistics.
     */
    private void showStats() throws IllegalStateException {
        double totalMemory = Runtime.getRuntime().maxMemory();
        double highWaterMark = Runtime.getRuntime().totalMemory();
        double freeMemory = Runtime.getRuntime().freeMemory();
        double usedMemory = (highWaterMark - freeMemory);


        totalMemory = totalMemory / 1000000.0;
        usedMemory = usedMemory / 1000000.0;
        highWaterMark = highWaterMark / 1000000.0;

        long now = System.currentTimeMillis();
        if (lastTimeRanGC < 0)
            lastTimeRanGC = now;

        // For the threshold use the physical memory
        int percent = (int)(100.0 * (usedMemory / totalMemory));
        if (percent > percentThreshold) {
            timesAboveThreshold++;
            if (timesAboveThreshold > 5) {
                // Only run the GC every 5 seconds
                if (now - lastTimeRanGC > 5000) {
                    // For now just clear the cache. Don't run the gc
                    CacheManager.clearCache();
                    // runGC();
                    lastTimeRanGC = now;
                }
            }
        } else {
            timesAboveThreshold = 0;
            lastTimeRanGC = now;
        }

        label.setText(" " + Msg.msg("Memory:") + " "
            + fmt.format(usedMemory) + "/"
            + fmt.format(highWaterMark) + "/"
            + fmt.format(totalMemory) + " " + Msg.msg("MB"));

        if (percent >= 50)
            label.setBackground(doColorThing((usedMemory/totalMemory)-0.25));
        else
            label.setBackground(doColorThing(0.0));

        repaint();
    }

    private Color doColorThing(final double percent) {
        return new Color(1.0f, 0.0f, 0.0f, new Float(percent).floatValue());
    }

    /**
     * Run this monitor
     */
    public void run() {
        while (running) {
            try {
                showStats();
                Thread.sleep(sleepInterval);
            } catch (Exception exc) {
            }
        }
    }

    /**
     * Set whether we are running
     * 
     * @param r true if we are running
     */
    public void setRunning(final boolean r) {
        running = r;
    }

    /**
     * Get whether we are running
     * 
     * @return true if we are
     */
    public boolean getRunning() {
        return running;
    }

    /**
     * Test routine
     * 
     * @param args not used
     */
    public static void main(final String[] args) {
        JFrame f = new JFrame();
        MemoryMonitor mm = new MemoryMonitor();
        f.getContentPane().add(mm);
        f.pack();
        f.show();
    }

}
