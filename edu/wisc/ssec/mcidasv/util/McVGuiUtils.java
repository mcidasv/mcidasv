/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.util;

import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import ucar.unidata.util.GuiUtils;

import edu.wisc.ssec.mcidasv.Constants;


public class McVGuiUtils implements Constants {
    private McVGuiUtils() {}
    
    public enum Width { HALF, SINGLE, ONEHALF, DOUBLE, DOUBLEDOUBLE }
    public enum Position { LEFT, RIGHT, CENTER }
    public enum TextColor { NORMAL, STATUS }
    
    public static JLabel makeLabelRight(String title) {
    	return makeLabelRight(title, null);
    }
    
    public static JLabel makeLabelRight(String title, Width width) {
        JLabel newLabel = new JLabel(title);
    	setComponentSize(newLabel, width);
    	setLabelPosition(newLabel, Position.RIGHT);
        return newLabel;
    }
    
    public static JTextField makeTextField(String value) {
    	return makeTextField(value, null);
    }
    
    public static JTextField makeTextField(String value, Width width) {
    	JTextField newTextField = new JTextField(value);
    	setComponentSize(newTextField, width);
    	return newTextField;
    }
    
    public static JComboBox makeComboBox(List items, Object selected) {
    	return makeComboBox(items, selected, null);
    }
    
    public static JComboBox makeComboBox(List items, Object selected, Width width) {
    	JComboBox newComboBox = GuiUtils.getEditableBox(items, selected);
    	setComponentSize(newComboBox, width);
    	return newComboBox;
    }
    
    public static void setComponentSize(JComponent existingComponent) {
    	setComponentSize(existingComponent, null);
    }
    
    public static void setComponentSize(JComponent existingComponent, Width width) {
    	if (width == null) width = Width.SINGLE;
    	
    	switch (width) {
    	case HALF:
    		existingComponent.setMinimumSize(new Dimension(ELEMENT_HALF_WIDTH, 24));
    		existingComponent.setMaximumSize(new Dimension(ELEMENT_HALF_WIDTH, 24));
    		existingComponent.setPreferredSize(new Dimension(ELEMENT_HALF_WIDTH, 24));
    		break;

    	case SINGLE: 
    		existingComponent.setMinimumSize(new Dimension(ELEMENT_WIDTH, 24));
    		existingComponent.setMaximumSize(new Dimension(ELEMENT_WIDTH, 24));
    		existingComponent.setPreferredSize(new Dimension(ELEMENT_WIDTH, 24));
    		break;

    	case ONEHALF:    	
    		existingComponent.setMinimumSize(new Dimension(ELEMENT_ONEHALF_WIDTH, 24));
    		existingComponent.setMaximumSize(new Dimension(ELEMENT_ONEHALF_WIDTH, 24));
    		existingComponent.setPreferredSize(new Dimension(ELEMENT_ONEHALF_WIDTH, 24));
    		break;
    		
    	case DOUBLE: 
    		existingComponent.setMinimumSize(new Dimension(ELEMENT_DOUBLE_WIDTH, 24));
    		existingComponent.setMaximumSize(new Dimension(ELEMENT_DOUBLE_WIDTH, 24));
    		existingComponent.setPreferredSize(new Dimension(ELEMENT_DOUBLE_WIDTH, 24));
    		break;
    		
    	case DOUBLEDOUBLE: 
    		existingComponent.setMinimumSize(new Dimension(ELEMENT_DOUBLEDOUBLE_WIDTH, 24));
    		existingComponent.setMaximumSize(new Dimension(ELEMENT_DOUBLEDOUBLE_WIDTH, 24));
    		existingComponent.setPreferredSize(new Dimension(ELEMENT_DOUBLEDOUBLE_WIDTH, 24));
    		break;

    	default:	 	    	
    		existingComponent.setMinimumSize(new Dimension(ELEMENT_WIDTH, 24));
    		existingComponent.setMaximumSize(new Dimension(ELEMENT_WIDTH, 24));
    		existingComponent.setPreferredSize(new Dimension(ELEMENT_WIDTH, 24));
    		break;
    	}

    }

    public static void setLabelPosition(JLabel existingLabel) {
    	setLabelPosition(existingLabel, null);
    }
    
    public static void setLabelPosition(JLabel existingLabel, Position position) {
    	if (position == null) position = Position.LEFT;

    	switch (position) {
    	case LEFT:
    		existingLabel.setHorizontalTextPosition(SwingConstants.LEFT);
    		existingLabel.setHorizontalAlignment(SwingConstants.LEFT);
    		break;

    	case RIGHT: 
    		existingLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
    		existingLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    		break;

    	case CENTER:    	
    		existingLabel.setHorizontalTextPosition(SwingConstants.CENTER);
    		existingLabel.setHorizontalAlignment(SwingConstants.CENTER);
    		break;
    		
    	default:	 	    	
    		existingLabel.setHorizontalTextPosition(SwingConstants.LEFT);
		existingLabel.setHorizontalAlignment(SwingConstants.LEFT);
    		break;
    	}
    }
    
    public static void setComponentColor(JComponent existingComponent) {
    	setComponentColor(existingComponent, null);
    }
    
    public static void setComponentColor(JComponent existingComponent, TextColor color) {
    	if (color == null) color = TextColor.NORMAL;

    	switch (color) {
    	case NORMAL:
    		existingComponent.setForeground(new Color(0, 0, 0));
    		break;

    	case STATUS: 
    		existingComponent.setForeground(new Color(0, 95, 255));
    		break;
    		
    	default:	 	    	
    		existingComponent.setForeground(new Color(0, 0, 0));
    		break;
    	}
    }


}
