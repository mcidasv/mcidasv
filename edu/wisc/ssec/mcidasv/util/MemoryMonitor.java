/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.mcidasv.util;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.Alignment.LEADING;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.idv.StateManager;
import ucar.unidata.util.CacheManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Msg;

public class MemoryMonitor extends JPanel implements Runnable {
    
    private static final Logger logger =
        LoggerFactory.getLogger(MemoryMonitor.class);
    
    private static final long serialVersionUID = 1L;

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
    
    /** percent cancel */
    private final int percentCancel;
    
    /** have we tried to cancel the load yet */
    private boolean triedToCancel = false;

    /** format */
    private static DecimalFormat fmt = new DecimalFormat("#0");

    /** the label */
    private JLabel label = new JLabel("");
    
    /** Keep track of the last time we ran the gc and cleared the cache */
    private static long lastTimeRanGC = -1;
    
//    /** Keep track of the IDV so we can try to cancel loads if mem usage gets high */
//    private IntegratedDataViewer idv;
    private StateManager stateManager;

    private String memoryString;

    private String mbString;

    private boolean showClock = false;

    private static SimpleDateFormat clockFormat =
        new SimpleDateFormat("HH:mm:ss z");
    
    private static double MEGABYTE = 1024 * 1024;

    /**
     * Default constructor.
     * 
     * @param stateManager Reference back to application session's 
     *                     {@code StateManager}. Cannot be {@code null}.
     */
    public MemoryMonitor(StateManager stateManager) {
        this(stateManager, 75, 95, false);
    }

    /**
     * Create a new MemoryMonitor.
     *
     * @param stateManager Reference back to application session's 
     *                     {@code StateManager}. Cannot be {@code null}.
     * @param percentThreshold Percentage of use memory before garbage
     *                         collection is run.
     * @param percentCancel Not currently in use.
     * @param showClock Whether or not the clock should be shown instead of 
     *                  the memory monitor widget.
     * 
     */
    public MemoryMonitor(StateManager stateManager,
                         final int percentThreshold,
                         final int percentCancel,
                         boolean showClock)
    {
        super(new BorderLayout());
        this.stateManager = stateManager;
        this.showClock = showClock;
        Font f = label.getFont();
        label.setToolTipText("Used memory/Max used memory/Max memory");
        label.setFont(f);
        this.percentThreshold = percentThreshold;
        this.percentCancel = percentCancel;

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addComponent(label, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addComponent(label, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
        );

//        MouseListener ml = new MouseAdapter() {
//            @Override public void mouseClicked(MouseEvent e) {
//                if (SwingUtilities.isRightMouseButton(e))
//                    popupMenu(e);
//            }
//        };
        MouseListener ml = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isRightMouseButton(e)) {
                    toggleClock();
                    try {
                        showStats();
                    } catch (Exception ex) {
                        
                    }
                }
                handleMouseEvent(e);
            }
        };

        label.addMouseListener(ml);
        label.setOpaque(true);
        label.setBackground(doColorThing(0));
        memoryString = Msg.msg("Memory:");
        mbString = Msg.msg("MB");
        start();
    }

    /**
     * Handle a mouse event
     *
     * @param event the event
     */
    private void handleMouseEvent(MouseEvent event) {
        if (SwingUtilities.isRightMouseButton(event)) {
            popupMenu(event);
        }
    }

    /**
     * 
     */
    private void toggleClock() {
        this.showClock = !this.showClock;
        stateManager.putPreference("idv.monitor.showclock", this.showClock);
    }

    /**
     * Returns a description of either the clock or memory monitor GUI.
     * 
     * @return Description of either the clock or memory monitor GUI.
     */
    private String getToolTip() {
        if (showClock) {
            return "Current time";
        } else {
            return "Used memory/Max used memory/Max memory";
        }
    }

    /**
     * Popup a menu on an event
     * 
     * @param event the event
     */
    private void popupMenu(final MouseEvent event) {
        JPopupMenu popup = new JPopupMenu();
//        if (running) {
//            popup.add(GuiUtils.makeMenuItem("Stop Running",
//                MemoryMonitor.this, "toggleRunning"));
//        } else {
//            popup.add(GuiUtils.makeMenuItem("Resume Running",
//                MemoryMonitor.this, "toggleRunning"));
//        }

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
        if (!running) {
            label.setEnabled(true);
            running = true;
            triedToCancel = false;
            thread = new Thread(this, "Memory monitor");
            thread.start();
        }
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
        label.setToolTipText(getToolTip());
        if (showClock) {
            Date d = new Date();
            clockFormat.setTimeZone(GuiUtils.getTimeZone());
            label.setText("  " + clockFormat.format(d));
            repaint();
            return;
        }

        double totalMemory = Runtime.getRuntime().maxMemory();
        double highWaterMark = Runtime.getRuntime().totalMemory();
        double freeMemory = Runtime.getRuntime().freeMemory();
        double usedMemory = (highWaterMark - freeMemory);

        totalMemory = totalMemory / MEGABYTE;
        usedMemory = usedMemory / MEGABYTE;
        highWaterMark = highWaterMark / MEGABYTE;

        long now = System.currentTimeMillis();
        if (lastTimeRanGC < 0) {
            lastTimeRanGC = now;
        }

        // For the threshold use the physical memory
        int percent = (int)(100.0f * (usedMemory / totalMemory));
        if (percent > percentThreshold) {
            timesAboveThreshold++;
            if (timesAboveThreshold > 5) {
                // Only run every 5 seconds
                if (now - lastTimeRanGC > 5000) {
                    // For now just clear the cache. Don't run the gc
                    CacheManager.clearCache();
                    // runGC();
                    lastTimeRanGC = now;
                }
            }
            int stretchedPercent = Math.round(((float)percent - (float)percentThreshold) * (100.0f / (100.0f - (float)percentThreshold)));
            label.setBackground(doColorThing(stretchedPercent));
        } else {
            timesAboveThreshold = 0;
            lastTimeRanGC = now;
            label.setBackground(doColorThing(0));
        }
        
        // TODO: evaluate this method--should we really cancel stuff for the user?
        // Decided that no, we shouldn't.  At least not until we get a more bulletproof way of doing it.
        // action:idv.stopload is unreliable and doesnt seem to stop object creation, just data loading.
        if (percent > this.percentCancel) {
            if (!triedToCancel) {
//              System.err.println("Canceled the load... not much memory available");
//              idv.handleAction("action:idv.stopload");
                triedToCancel = true;
            }
        } else {
            triedToCancel = false;
        }

        label.setText(' '
            + memoryString + ' '
            + fmt.format(usedMemory) + '/'
            + fmt.format(highWaterMark) + '/'
            + fmt.format(totalMemory) + ' ' + mbString
            + ' ');

        repaint();
    }

    private Color doColorThing(final int percent) {
        Float alpha = new Float(percent).floatValue() / 100;
        return new Color(1.0f, 0.0f, 0.0f, alpha);
    }
    
    /**
     * Run this monitor
     */
    public void run() {
        while (running) {
            try {
                SwingUtilities.invokeLater(this::showStats);
                Thread.sleep(sleepInterval);
            } catch (Exception exc) {
                logger.warn("Caught exception!", exc);
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
        MemoryMonitor mm = new MemoryMonitor(null);
        f.getContentPane().add(mm);
        f.pack();
        f.setVisible(true);
    }

}
