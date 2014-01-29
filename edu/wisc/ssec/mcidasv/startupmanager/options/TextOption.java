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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JTextField;

import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.OptionPlatform;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Type;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Visibility;

public class TextOption extends AbstractOption {
    
    private String value = "";
    
    public TextOption(final String id, final String label, 
        final String defaultValue, final OptionPlatform optionPlatform,
        final Visibility optionVisibility) 
    {
        super(id, label, Type.TEXT, optionPlatform, optionVisibility);
        setValue(defaultValue);
    }
    
    public JTextField getComponent() {
        final JTextField tf = new JTextField(getValue(), 10);
        tf.addKeyListener(new KeyAdapter() {
            public void keyReleased(final KeyEvent e) {
                setValue(tf.getText());
            }
        });
        if (!onValidPlatform()) {
            tf.setEnabled(false);
        }
        return tf;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(final String newValue) {
        value = newValue;
    }
    
    public String toString() {
        return String.format("[TextOption@%x: optionId=%s, value=%s]", 
            hashCode(), getOptionId(), getValue());
    }
}
