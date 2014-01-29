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

package edu.wisc.ssec.mcidasv.startupmanager.options;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ucar.unidata.util.GuiUtils;

import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVTextField;
import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.OptionPlatform;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Type;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Visibility;

public class MemoryOption extends AbstractOption implements ActionListener {
    public enum Prefix {
        MEGA("M", "megabytes"),
        GIGA("G", "gigabytes"),
        TERA("T", "terabytes"),
        PERCENT("P", "percent");
        
        private final String javaChar;
        private final String name;
        
        private Prefix(final String javaChar, final String name) {
            this.javaChar = javaChar;
            this.name = name;
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
        
        private State(final Color foreground, final Color background) {
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
    
    private final static Prefix[] PREFIXES = { Prefix.MEGA, Prefix.GIGA, Prefix.TERA };
    
    private Prefix currentPrefix = Prefix.MEGA;
    
    private static final Pattern MEMSTRING = 
        Pattern.compile("^(\\d+)([M|G|T|P]?)$", Pattern.CASE_INSENSITIVE);
    
    private final String defaultPrefValue;
    
    private String failsafeValue = "512M";
    
    private String value = failsafeValue; // bootstrap
    
    private JRadioButton jrbSlider = new JRadioButton();
    
    private JRadioButton jrbNumber = new JRadioButton();
    
    private ButtonGroup jtbBg = GuiUtils.buttonGroup(jrbSlider, jrbNumber);
    
    private JPanel sliderPanel = new JPanel();
    
    private JLabel sliderLabel = new JLabel();
    
    private JSlider slider = new JSlider();
    
    private JPanel textPanel = new JPanel();
    private McVTextField text = new McVTextField();
    private JComboBox memVals = new JComboBox(PREFIXES);
    private String initTextValue = value;
    private Prefix initPrefixValue = currentPrefix;
    
    private int minSliderValue = 10;
    private int maxSliderValue = 80;
    private int initSliderValue = minSliderValue;
    
    private int maxmem = StartupManager.getMaximumHeapSize();
    
    private State currentState = State.VALID;
    
    private boolean doneInit = false;
    
    public MemoryOption(final String id, final String label, 
        final String defaultValue, final OptionPlatform optionPlatform,
        final Visibility optionVisibility) 
    {
        super(id, label, Type.MEMORY, optionPlatform, optionVisibility);
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
        text.setAllow(new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});
        jrbSlider.setActionCommand("slider");
        jrbSlider.addActionListener(this);
        jrbNumber.setActionCommand("number");
        jrbNumber.addActionListener(this);
        sliderPanel.setEnabled(false);
        textPanel.setEnabled(false);
    }
    
    private String[] getNames(final Prefix[] arr) {
        assert arr != null;
        String[] newArr = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            newArr[i] = arr[i].getName();
        }
        return newArr;
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
        return currentPrefix.equals(Prefix.PERCENT);
    }
    
    public void actionPerformed(ActionEvent e) {
        if ("slider".equals(e.getActionCommand())) {
            GuiUtils.enableTree(sliderPanel, true);
            GuiUtils.enableTree(textPanel, false);
            // Trigger the listener
            int sliderValue = slider.getValue();
            if (sliderValue==minSliderValue) {
                slider.setValue(maxSliderValue);
            } else {
                slider.setValue(minSliderValue);
            }
            slider.setValue(sliderValue);
        } else {
            GuiUtils.enableTree(sliderPanel, false);
            GuiUtils.enableTree(textPanel, true);
            // Trigger the listener
            handleNewValue(text, memVals);
        }
    }
    
    public ChangeListener percentListener = new ChangeListener() {
        public void stateChanged(ChangeEvent evt) {
            if (!sliderPanel.isEnabled()) {
                return;
            }
            int sliderValue = ((JSlider)evt.getSource()).getValue();
            setValue(sliderValue + Prefix.PERCENT.getJavaChar());
            text.setText("" + Math.round(sliderValue / 100.0 * maxmem));
        }
    };
    
    private void handleNewValue(final JTextField field, final JComboBox box) {
        if (!textPanel.isEnabled()) return;
        assert field != null;
        assert box != null;
        
        try {
            String newValue = field.getText();
            String huh = ((Prefix)box.getSelectedItem()).getJavaFormat(newValue);
            
            if (!isValid()) {
                setState(State.VALID);
            }
            setValue(huh);
        } catch (IllegalArgumentException e) {
            setState(State.ERROR);
            text.setToolTipText("This value must be an integer greater than zero.");
        }
    }
    
    public JPanel getComponent() {
        JPanel topPanel = GuiUtils.hbox(jrbSlider, getSliderComponent());
        JPanel bottomPanel = GuiUtils.hbox(jrbNumber, getTextComponent());
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
        sliderLabel = new JLabel("Use " + initSliderValue + "% ");
        String memoryString = maxmem + "mb";
        if (maxmem == 0) {
            memoryString="Unknown";
        }
        JLabel postLabel = new JLabel(" of available memory (" + memoryString + ")");
        JComponent[] sliderComps = GuiUtils.makeSliderPopup(minSliderValue, maxSliderValue+1, initSliderValue, percentListener);
        slider = (JSlider) sliderComps[1];
        slider.setMinorTickSpacing(5);
        slider.setMajorTickSpacing(10);
        slider.setSnapToTicks(true);
        slider.setExtent(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        sliderComps[0].setToolTipText("Set maximum memory by percent");
        sliderPanel = GuiUtils.hbox(sliderLabel, sliderComps[0], postLabel);
        return sliderPanel;
    }
    
    public JComponent getTextComponent() {
        text.setText(initTextValue);
        text.addKeyListener(new KeyAdapter() {
            public void keyReleased(final KeyEvent e) {
                handleNewValue(text, memVals);
            }
        });
        memVals.setSelectedItem(initPrefixValue);
        memVals.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                handleNewValue(text, memVals);
            }
        });
        McVGuiUtils.setComponentWidth(text, McVGuiUtils.Width.ONEHALF);
        McVGuiUtils.setComponentWidth(memVals, McVGuiUtils.Width.ONEHALF);
        textPanel = GuiUtils.hbox(text, memVals);
        return textPanel;
    }
    
    public String toString() {
        return String.format(
            "[MemoryOption@%x: value=%s, currentPrefix=%s, isSlider=%s]", 
            hashCode(), value, currentPrefix, isSlider());
    }
    
    public String getValue() {
        if (!isValid()) {
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
        if (!m.matches()) {
            throw new IllegalArgumentException("Badly formatted memory string: "+newValue);
        }
        String quantity = m.group(1);
        String prefix = m.group(2);
        
        // Fall back on failsafe value if user wants a percentage of an unknown maxmem
        if (maxmem==0 && prefix.toUpperCase().equals(Prefix.PERCENT.getJavaChar())) {
            m = MEMSTRING.matcher(failsafeValue);
            if (!m.matches()) {
                throw new IllegalArgumentException("Badly formatted memory string: "+failsafeValue);
            }
            quantity = m.group(1);
            prefix = m.group(2);
        }
        
        int intVal = Integer.parseInt(quantity);
        if (intVal <= 0) {
            throw new IllegalArgumentException("Memory cannot be less than or equal to zero: "+newValue);
        }
        if (prefix.isEmpty()) {
            prefix = "M";
        }
        value = quantity;
        
        if (prefix.toUpperCase().equals(Prefix.PERCENT.getJavaChar())) {
            currentPrefix = Prefix.PERCENT;
            
            // Work around all the default settings going on
            initSliderValue = Integer.parseInt(value);
            initPrefixValue = Prefix.MEGA;
            initTextValue = "" + (int)Math.round(initSliderValue * maxmem / 100.0);
            
            sliderLabel.setText("Use " + value + "% ");
            if (maxmem > 0) {
                memVals.setSelectedItem(initPrefixValue);
                text.setText(initTextValue);
            }
            if (!doneInit) {
                jrbSlider.setSelected(true);
            }
            return;
        }
        
        for (Prefix tmp : PREFIXES) {
            if (prefix.toUpperCase().equals(tmp.getJavaChar())) {
                currentPrefix = tmp;
                
                // Work around all the default settings going on
                initSliderValue = minSliderValue;
                initPrefixValue = currentPrefix;
                initTextValue = value;
                
                if (maxmem>0) {
                    int multiplier = 1;
                    if (currentPrefix.equals(Prefix.GIGA)) multiplier=1024;
                    else if (currentPrefix.equals(Prefix.TERA)) multiplier=1024 * 1024;
                    initSliderValue = (int)Math.round(Integer.parseInt(value) * 100.0 * multiplier / maxmem);
                    initSliderValue = Math.max(Math.min(initSliderValue, maxSliderValue), minSliderValue);
                    slider.setValue(initSliderValue);
                    sliderLabel.setText("Use "+initSliderValue+"% ");
                }
                if (!doneInit) jrbNumber.setSelected(true);
                return;
            }
        }
        
        throw new IllegalArgumentException("Could not find matching memory prefix for \""+prefix+"\" in string: "+newValue);
    }
}
