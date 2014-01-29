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

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ucar.unidata.util.GuiUtils;

import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.OptionPlatform;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Type;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Visibility;

public class SliderOption extends AbstractOption {
    
    private final int minValue = 10;
    
    private final int maxValue = 80;
    
    private final int total;
    
    private int sliderValue = minValue;
    
    public SliderOption(final String id, final String label, 
            final String defaultValue, final OptionPlatform optionPlatform, 
            Visibility optionVisibility) 
    {
        super(id, label, Type.SLIDER, optionPlatform, optionVisibility);
        total = StartupManager.getInstance().getPlatform().getAvailableMemory();
        setValue(defaultValue);
    }
    
    private ChangeListener makeChangeListener(final JLabel sliderLabel) {
        return new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                JSlider src = (JSlider)evt.getSource();
                int value = src.getValue();
                sliderLabel.setText("Use "+value+"% ");
                setValue(Integer.toString(value));
            }
        };
    }
    
    public JPanel getComponent() {
        JLabel sliderLabel = new JLabel("Use "+sliderValue+"% ");
        JLabel postLabel = new JLabel(" of available memory ("+total+"mb)");
        
        ChangeListener listener = makeChangeListener(sliderLabel);
        
        JComponent[] sliderComps = GuiUtils.makeSliderPopup(minValue, maxValue+1, sliderValue, listener);
        JSlider slider = (JSlider)sliderComps[1];
        slider.setMinorTickSpacing(5);
        slider.setMajorTickSpacing(10);
        slider.setSnapToTicks(true);
        slider.setExtent(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        sliderComps[0].setToolTipText("Set maximum memory by percent");
        
        JPanel sliderPanel = GuiUtils.hbox(sliderLabel, sliderComps[0], postLabel);
        return sliderPanel;
    }
    
    public String getValue() {
        return Integer.toString(sliderValue)+"P";
    }
    
    public void setValue(final String newValue) {
        int endIndex = newValue.length();
        if (newValue.endsWith("P")) {
            endIndex = endIndex - 1;
        }
        int test = Integer.parseInt(newValue.substring(0, endIndex));
        if (test >= 0) {
            sliderValue = test;
        }
    }
    
    public String toString() {
        return String.format("[SliderOption@%x: optionId=%s, value=%s]", hashCode(), getOptionId(), getValue());
    }
}
