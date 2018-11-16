/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2018
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

package edu.wisc.ssec.mcidasv.startupmanager.options;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.list;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import edu.wisc.ssec.mcidasv.util.MakeToString;
import edu.wisc.ssec.mcidasv.util.SystemState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LayoutUtil;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVTextField;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.OptionPlatform;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Type;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Visibility;

public class MemoryOption extends AbstractOption implements ActionListener {
    
    /** Logger object. */
    private static final Logger logger = LoggerFactory.getLogger(MemoryOption.class);
    
    private static final long GIGA_BYTES_TO_BYTES = 1024 * 1024 * 1024;
    
    private static final String TOO_BIG_FMT = "Value exceeds your maximum available memory (%s MB)";
    
    private static final String BAD_MEM_FMT = "Badly formatted memory string: %s";
    
    private static final String LTE_ZERO_FMT = "Memory cannot be less than or equal to zero: %s";
    
    private static final String SLIDER_LABEL_FMT = "Using %s";
    
    private static final String SLIDER_LESS_THAN_MIN_LABEL_FMT = "Using < %s";
    
    private static final String SLIDER_GREATER_THAN_MAX_LABEL_FMT = "Using > %s";
    
    private static final String NO_MEM_PREFIX_FMT = "Could not find matching memory prefix for \"%s\" in string: %s";
    
    public enum Prefix {
        PERCENT("P", "percent", 1),
        MEGA("M", "megabytes", 1),
        GIGA("G", "gigabytes", 1024),
        TERA("T", "terabytes", 1024 * 1024);
        
        private final String javaChar;
        private final String name;
        private final long scale;
        
        Prefix(final String javaChar, final String name, final long scale) {
            this.javaChar = javaChar;
            this.name = name;
            this.scale = scale;
        }
        
        public long getScale() { 
            return scale; 
        }
        
        public String getJavaChar() {
            return javaChar.toUpperCase();
        }
        
        public String getName() {
            return name;
        }
        
        public String getJavaFormat(final String value) {
            long longVal = Long.parseLong(value);
            return longVal + javaChar;
        }
        
        @Override public String toString() {
            return name;
        }
    }
    
    private enum State { 
        VALID(Color.BLACK, Color.WHITE),
        WARN(Color.BLACK, new Color(255, 255, 204)),
        ERROR(Color.WHITE, Color.PINK);
        
        private final Color foreground;
        
        private final Color background;
        
        State(final Color foreground, final Color background) {
            this.foreground = foreground;
            this.background = background;
        }
        
        public Color getForeground() { 
            return foreground; 
        }
        
        public Color getBackground() { 
            return background; 
        }
    }
    
    private static final Prefix[] PREFIXES = { Prefix.MEGA, Prefix.GIGA, Prefix.TERA };
    
    private Prefix currentPrefix = Prefix.MEGA;
    
    private boolean sliderActive = false;
    
    private static final Pattern MEMSTRING = 
        Pattern.compile("^(\\d+)(M|G|T|P|MB|GB|TB)$", Pattern.CASE_INSENSITIVE);
    
    private final String defaultPrefValue;
    
    // default to 80% of system memory (in gigabytes)
    private String failsafeValue = 
        String.valueOf((int) Math.ceil(0.8 * (getSystemMemory() / MemoryOption.GIGA_BYTES_TO_BYTES))) + 'G';
    
    private String value = failsafeValue; // bootstrap
    
    private JRadioButton jrbSlider = new JRadioButton();
    
    private JRadioButton jrbNumber = new JRadioButton();
    
    private JPanel sliderPanel = new JPanel();
    
    private JLabel sliderLabel = new JLabel();
    
    private JSlider slider = new JSlider();
    
    private JPanel textPanel = new JPanel();
    private McVTextField text = new McVTextField();
    private String initTextValue = value;
    
    private int minSliderValue = 10;
    private int maxSliderValue = 80;
    private int initSliderValue = minSliderValue;
    
    // max size of current JVM, in *megabytes*
    private long maxmem = getSystemMemory() / (1024 * 1024);
    
    private State currentState = State.VALID;
    
    private boolean doneInit = false;
    
    public MemoryOption(final String id, final String label, 
        final String defaultValue, final OptionPlatform optionPlatform,
        final Visibility optionVisibility) 
    {
        super(id, label, Type.MEMORY, optionPlatform, optionVisibility);
        
        // Link the slider and numeric entry box as a button group
        GuiUtils.buttonGroup(jrbSlider, jrbNumber);
        if (maxmem == 0) {
            defaultPrefValue = failsafeValue;
        } else {
            defaultPrefValue = defaultValue;
        }
        try {
            setValue(defaultPrefValue);
        } catch (IllegalArgumentException e) {
            setValue(value);
        }
        text.setAllow('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'M', 'G', 'T', 'B');
        text.setUppercase(true);
        text.setToolTipText("A positive integer followed by unit, e.g. M, G, or T (no spaces).");
        jrbSlider.setActionCommand("slider");
        jrbSlider.addActionListener(this);
        jrbNumber.setActionCommand("number");
        jrbNumber.addActionListener(this);
        sliderPanel.setEnabled(false);
        textPanel.setEnabled(false);
    }
    
    private void setState(final State newState) {
        assert newState != null : newState;
        currentState = newState;
        text.setForeground(currentState.getForeground());
        text.setBackground(currentState.getBackground());
    }
    
    private boolean isValid() {
        return currentState == State.VALID;
    }
    
    private boolean isSlider() {
        return sliderActive;
    }
    
    public void actionPerformed(ActionEvent e) {
        if ("slider".equals(e.getActionCommand())) {
            sliderActive = true;
            GuiUtils.enableTree(sliderPanel, true);
            GuiUtils.enableTree(textPanel, false);
            // Trigger the listener
            int sliderValue = slider.getValue();
            if (sliderValue == minSliderValue) {
                slider.setValue(maxSliderValue);
            } else {
                slider.setValue(minSliderValue);
            }
            slider.setValue(sliderValue);
        } else {
            sliderActive = false;
            GuiUtils.enableTree(sliderPanel, false);
            GuiUtils.enableTree(textPanel, true);
            // Trigger the listener
            handleNewValue(text);
        }
    }
    
    private ChangeListener percentListener = evt -> {
        if (sliderPanel.isEnabled()) {
            int sliderValue = ((JSlider) evt.getSource()).getValue();
            setValue(sliderValue + "P");
            text.setText(String.valueOf(Math.round(sliderValue / 100.0 * maxmem)) + "MB");
        }
    };
    
    private void handleNewValue(final McVTextField field) {
        
        if (! textPanel.isEnabled()) {
            return;
        }
        assert field != null;
        
        try {

            String memWithSuffix = field.getText();
            
            if (memWithSuffix.isEmpty()) {
                setState(State.ERROR);
                return;
            }
            
            if (!isValid()) {
                setState(State.VALID);
            }
            
            long newMemVal = -1;
            // need to deal with both "G" and "GB" suffixes
            int suffixLength = 1;
            if (memWithSuffix.endsWith("MB") 
                || memWithSuffix.endsWith("GB") 
                || memWithSuffix.endsWith("TB")) 
            {
                suffixLength = 2;
            }
            String memWithoutSuffix = 
                memWithSuffix.substring(0, memWithSuffix.length() - suffixLength);
            
            try {
                newMemVal = Long.parseLong(memWithoutSuffix);
            } catch (NumberFormatException nfe) {
                // TJJ this should never happen, since validation is done live on keystrokes
                // But if somebody ever changed the UI, better log an exception
                logger.error("Memory value error:", nfe);
            }
            
            if (memWithSuffix.endsWith("G") || memWithSuffix.endsWith("GB")) {
                // megabytes per Gigabyte
                newMemVal = newMemVal * Prefix.GIGA.getScale();
            }
            if (memWithSuffix.endsWith("T") || memWithSuffix.endsWith("TB")) {
                // megabytes per Terabyte
                newMemVal = newMemVal * Prefix.TERA.getScale();
            }

            if (newMemVal > maxmem) {
                long memInGB = maxmem;
                // Temporarily disable the text entry box, since Enter key in the modal
                // dialog would just cycle back through the text field key handler and
                // bring up a new dialog!
                text.setEnabled(false);
                JOptionPane.showMessageDialog(null, String.format(TOO_BIG_FMT, memInGB));
                // Re-enable text field, user dismissed warning dialog
                text.setEnabled(true);
                setState(State.ERROR);
            } else {
                setValue(memWithSuffix);
            }
        } catch (IllegalArgumentException e) {
            setState(State.ERROR);
        }
    }
    
    
    public JPanel getComponent() {
        JPanel topPanel = LayoutUtil.hbox(jrbSlider, getSliderComponent());
        JPanel bottomPanel = LayoutUtil.hbox(jrbNumber, getTextComponent());
        if (isSlider()) {
            GuiUtils.enableTree(sliderPanel, true);
            GuiUtils.enableTree(textPanel, false);
        } else {
            GuiUtils.enableTree(sliderPanel, false);
            GuiUtils.enableTree(textPanel, true);
        }
        if (maxmem == 0) {
            jrbSlider.setEnabled(false);
        }
        doneInit = true;
        return McVGuiUtils.topBottom(topPanel, bottomPanel, null);
    }
    
    public JComponent getSliderComponent() {
        sliderLabel = new JLabel("Using " + initSliderValue + "% ");
        String memoryString = maxmem + " MB";
        if (maxmem == 0) {
            memoryString = "Unknown";
        }
        JLabel postLabel = new JLabel(" of available memory (" + memoryString + ')');
        JComponent[] sliderComps = GuiUtils.makeSliderPopup(minSliderValue, maxSliderValue+1, initSliderValue, percentListener);
        slider = (JSlider) sliderComps[1];
        slider.setMinorTickSpacing(5);
        slider.setMajorTickSpacing(10);
        slider.setSnapToTicks(true);
        slider.setExtent(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        sliderComps[0].setToolTipText("Set maximum memory by percent");
        sliderPanel = LayoutUtil.hbox(sliderLabel, sliderComps[0], postLabel);
        return sliderPanel;
    }
    
    public JComponent getTextComponent() {

        text.addKeyListener(new KeyAdapter() {
            public void keyReleased(final KeyEvent e) {
                handleNewValue(text);
            }
        });
        
        textPanel = LayoutUtil.hbox(new JPanel(), list(text), 0);
        McVGuiUtils.setComponentWidth(text, McVGuiUtils.Width.ONEHALF);
        return textPanel;
    }
    
    public String toString() {
        return MakeToString.fromInstance(this)
                           .add("value", value)
                           .add("currentPrefix", currentPrefix)
                           .add("isSlider", isSlider()).toString();
    }
    
    public String getValue() {
        if (! isValid()) {
            return defaultPrefValue;
        }
        return currentPrefix.getJavaFormat(value);
    }
    
    // overridden so that any illegal vals coming *out of* a runMcV.prefs
    // can be replaced with a legal val.
    @Override public void fromPrefsFormat(final String prefText) {
        try {
            super.fromPrefsFormat(prefText);
        } catch (IllegalArgumentException e) {
            setValue(failsafeValue);
        }
    }

    public void setValue(final String newValue) {
        
        Matcher m = MEMSTRING.matcher(newValue);
        if (! m.matches()) {            
            throw new IllegalArgumentException(String.format(BAD_MEM_FMT, newValue));
        }
        String quantity = m.group(1);
        String prefix = m.group(2);
        
        // Fall back on failsafe value if user wants a percentage of an unknown maxmem
        if ((maxmem == 0) && sliderActive) {
            m = MEMSTRING.matcher(failsafeValue);
            if (!m.matches()) {
                throw new IllegalArgumentException(String.format(BAD_MEM_FMT, failsafeValue));
            }
            quantity = m.group(1);
            prefix = m.group(2);
        }
        
        int intVal = Integer.parseInt(quantity);
        if (intVal <= 0) {
            throw new IllegalArgumentException(String.format(LTE_ZERO_FMT, newValue));
        }
        if (prefix.isEmpty()) {
            prefix = "M";
        }
        value = quantity;
        
        // TJJ Nov 2018 - if unit is P (Percentage), activate slider. 
        // This also lets fresh install initialize correctly
        if (prefix.equals("P")) {
            sliderActive = true;
        } else {
            sliderActive = false;
        }
        
        if (sliderActive) {
            
            // Work around all the default settings going on
            initSliderValue = Integer.parseInt(value);
            initTextValue = String.valueOf((int) Math.round(initSliderValue * maxmem / 100.0));
            
            sliderLabel.setText(String.format(SLIDER_LABEL_FMT, value) + "% ");
            if (maxmem > 0) {
                text.setText(initTextValue + "MB");
            }
            if (! doneInit) {
                jrbSlider.setSelected(true);
            }
            currentPrefix = MemoryOption.Prefix.PERCENT;
            return;
        }
        
        for (Prefix tmp : MemoryOption.PREFIXES) {
            String newPrefix = prefix;
            if (prefix.length() > 1) newPrefix = prefix.substring(0, 1);
            if (newPrefix.toUpperCase().equals(tmp.getJavaChar())) {
                currentPrefix = tmp;
                
                // Work around all the default settings going on
                initSliderValue = minSliderValue;
                initTextValue = value;
                
                if (maxmem > 0) {
                    initSliderValue = (int) Math.round(Integer.parseInt(value) * 100.0 * currentPrefix.getScale() / maxmem);
                    boolean aboveMin = true;
                    boolean aboveMax = false;
                    if (initSliderValue < 10) aboveMin = false;
                    if (initSliderValue > 80) aboveMax = true;
                    initSliderValue = Math.max(Math.min(initSliderValue, maxSliderValue), minSliderValue);
                    slider.setValue(initSliderValue);
                    if (aboveMin) {
                        if (aboveMax) {
                            sliderLabel.setText(String.format(SLIDER_GREATER_THAN_MAX_LABEL_FMT, initSliderValue) + "% ");
                        } else {
                            sliderLabel.setText(String.format(SLIDER_LABEL_FMT, initSliderValue) + "% ");
                        }
                    } else {
                        sliderLabel.setText(String.format(SLIDER_LESS_THAN_MIN_LABEL_FMT, initSliderValue) + "% ");
                    }
                }
                if (! doneInit) {
                    jrbNumber.setSelected(true);
                }
                text.setText(newValue);
                return;
            }
        }
        throw new IllegalArgumentException(String.format(NO_MEM_PREFIX_FMT, prefix, newValue));
    }
    
    private static long getSystemMemory() {
        String val = SystemState.queryOpSysProps().get("opsys.memory.physical.total");
        if (Objects.equals(System.getProperty("os.name"), "Windows XP")) {
            logger.trace("returning 1536 * 1024 * 1024 (for XP)");
            return 1536 * (1024 * 1024);
        }
        return Long.parseLong(val);
    }
}
