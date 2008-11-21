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
import java.awt.Font;
import java.awt.Image;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import ucar.unidata.util.GuiUtils;
import edu.wisc.ssec.mcidasv.Constants;


public class McVGuiUtils implements Constants {
    private McVGuiUtils() {}
    
    public enum Width { HALF, SINGLE, ONEHALF, DOUBLE, DOUBLEDOUBLE }
    public enum Position { LEFT, RIGHT, CENTER }
    public enum Prefer { TOP, BOTTOM }
    public enum TextColor { NORMAL, STATUS }
    
    /**
     * Create a standard sized, right-justified label
     * @param title
     * @return
     */
    public static JLabel makeLabelRight(String title) {
    	return makeLabelRight(title, null);
    }
    
    public static JLabel makeLabelRight(String title, Width width) {
    	if (width==null) width=Width.SINGLE;
        JLabel newLabel = new JLabel(title);
        setComponentWidth(newLabel, width);
    	setLabelPosition(newLabel, Position.RIGHT);
        return newLabel;
    }
        
    /**
     * Create a sized, labeled component
     * @param label
     * @param thing
     * @return
     */
    public static JPanel makeLabeledComponent(String label, JComponent thing) {
    	return makeLabeledComponent(makeLabelRight(label), thing);
    }

    public static JPanel makeLabeledComponent(JLabel label, JComponent thing) {
    	JPanel newPanel = new JPanel();

    	org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(newPanel);
    	newPanel.setLayout(layout);
    	layout.setHorizontalGroup(
    			layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
    			.add(layout.createSequentialGroup()
    					.add(label)
    					.add(GAP_RELATED)
    					.add(thing))
    	);
    	layout.setVerticalGroup(
    			layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
    			.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
    					.add(label)
    					.add(thing))
    	);

    	return newPanel;
    }
            
    /**
     * Set the width of an existing component
     * @param existingComponent
     */
    public static void setComponentWidth(JComponent existingComponent) {
    	setComponentWidth(existingComponent, Width.SINGLE);
    }

    public static void setComponentWidth(JComponent existingComponent, Width width) {
        if (width == null)
            width = Width.SINGLE;

    	switch (width) {
    	case HALF:
    		setComponentWidth(existingComponent, ELEMENT_HALF_WIDTH);
    		break;

    	case SINGLE: 
    		setComponentWidth(existingComponent, ELEMENT_WIDTH);
    		break;

    	case ONEHALF:    	
    		setComponentWidth(existingComponent, ELEMENT_ONEHALF_WIDTH);
    		break;
    		
    	case DOUBLE: 
    		setComponentWidth(existingComponent, ELEMENT_DOUBLE_WIDTH);
    		break;
    		
    	case DOUBLEDOUBLE: 
    		setComponentWidth(existingComponent, ELEMENT_DOUBLEDOUBLE_WIDTH);
    		break;

    	default:	 	    	
    		setComponentWidth(existingComponent, ELEMENT_WIDTH);
    		break;
    	}
    }
    
    /**
     * Set the width of an existing component to a given int width
     * @param existingComponent
     * @param width
     */
    public static void setComponentWidth(JComponent existingComponent, int width) {
		existingComponent.setMinimumSize(new Dimension(width, 24));
		existingComponent.setMaximumSize(new Dimension(width, 24));
		existingComponent.setPreferredSize(new Dimension(width, 24));
    }
    
    /**
     * Set the component width to that of another component
     */
    public static void setComponentWidth(JComponent setme, JComponent getme) {
    	setComponentWidth(setme, getme, 0);
    }
    
    public static void setComponentWidth(JComponent setme, JComponent getme, int padding) {
        setme.setPreferredSize(new Dimension(getme.getPreferredSize().width + padding, getme.getPreferredSize().height));
    }
    
    /**
     * Set the component height to that of another component
     */
    public static void setComponentHeight(JComponent setme, JComponent getme) {
    	setComponentHeight(setme, getme, 0);
    }
    
    public static void setComponentHeight(JComponent setme, JComponent getme, int padding) {
        setme.setPreferredSize(new Dimension(getme.getPreferredSize().width, getme.getPreferredSize().height + padding));
    }

    /**
     * Set the label position of an existing label
     * @param existingLabel
     */
    public static void setLabelPosition(JLabel existingLabel) {
    	setLabelPosition(existingLabel, Position.LEFT);
    }
    
    public static void setLabelPosition(JLabel existingLabel, Position position) {
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
    
    /**
     * Set the bold attribute of an existing label
     * @param existingLabel
     * @param bold
     */
    public static void setLabelBold(JLabel existingLabel, boolean bold) {
    	Font f = existingLabel.getFont();
    	if (bold)
    		existingLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
    	else
    		existingLabel.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
    }
    
    /**
     * Set the foreground color of an existing component
     * @param existingComponent
     */
    public static void setComponentColor(JComponent existingComponent) {
    	setComponentColor(existingComponent, TextColor.NORMAL);
    }
    
    public static void setComponentColor(JComponent existingComponent, TextColor color) {
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

    /**
     * Custom makeImageButton to ensure proper sizing and mouseborder are set
     */
    public static JButton makeImageButton(String label, 
    		final Object object,
    		final String methodName,
    		final Object arg,
    		final String tooltip
    ) {

    	final JButton btn = makeImageButton(label, tooltip);
    	return (JButton) GuiUtils.addActionListener(btn, object, methodName, arg);
    }

    /**
     * Custom makeImageButton to ensure proper sizing and mouseborder are set
     */
    public static JButton makeImageButton(String iconName, String tooltip) {
    	boolean addMouseOverBorder = true;

    	ImageIcon imageIcon = GuiUtils.getImageIcon(iconName, GuiUtils.class);
    	if (imageIcon.getIconWidth() > 22 || imageIcon.getIconHeight() > 22) {
    		Image scaledImage  = imageIcon.getImage().getScaledInstance(22, 22, Image.SCALE_SMOOTH);
    		imageIcon = new ImageIcon(scaledImage);
    	}

    	final JButton btn = GuiUtils.getImageButton(imageIcon);
    	btn.setBackground(null);
    	if (addMouseOverBorder) {
    		GuiUtils.makeMouseOverBorder(btn);
    	}
    	btn.setToolTipText(tooltip);
    	return btn;
    }
    
	/**
	 * Create a standard sized combo box
	 * @param items
	 * @param selected
	 * @return
	 */
    public static JComboBox makeComboBox(List items, Object selected) {
    	return makeComboBox(items, selected, null);
    }
    
    public static JComboBox makeComboBox(List items, Object selected, Width width) {
    	JComboBox newComboBox = GuiUtils.getEditableBox(items, selected);
    	setComponentWidth(newComboBox, width);
    	return newComboBox;
    }
    
    /**
     * Create a standard sized text field
     * @param value
     * @return
     */
    public static JTextField makeTextField(String value) {
    	return makeTextField(value, null);
    }
    
    public static JTextField makeTextField(String value, Width width) {
    	JTextField newTextField = new McVTextField(value);
    	setComponentWidth(newTextField, width);
    	return newTextField;
    }
    
    /**
     * Create some custom text entry widgets
     */
    public static McVTextField makeTextFieldLimit(String defaultString, int limit) {
    	return new McVTextField(defaultString, limit);
    }
    
    public static McVTextField makeTextFieldUpper(String defaultString, int limit) {
    	return new McVTextField(defaultString, limit, true);
    }
    
    public static McVTextField makeTextFieldAllow(String defaultString, int limit, boolean upper, String allow) {
    	McVTextField newField = new McVTextField(defaultString, limit, upper);
    	newField.setAllow(allow);
    	return newField;
    }
    
    public static McVTextField makeTextFieldDeny(String defaultString, int limit, boolean upper, String deny) {
    	McVTextField newField = new McVTextField(defaultString, limit, upper);
    	newField.setDeny(deny);
    	return newField;
    }
        
    public static McVTextField makeTextFieldAllow(String defaultString, int limit, boolean upper, char[] allow) {
    	McVTextField newField = new McVTextField(defaultString, limit, upper);
    	newField.setAllow(allow);
    	return newField;
    }
    
    public static McVTextField makeTextFieldDeny(String defaultString, int limit, boolean upper, char[] deny) {
    	McVTextField newField = new McVTextField(defaultString, limit, upper);
    	newField.setDeny(deny);
    	return newField;
    }
    
    public static McVTextField makeTextFieldAllow(String defaultString, int limit, boolean upper, Pattern allow) {
    	McVTextField newField = new McVTextField(defaultString, limit, upper);
    	newField.setAllow(allow);
    	return newField;
    }
    
    public static McVTextField makeTextFieldDeny(String defaultString, int limit, boolean upper, Pattern deny) {
    	McVTextField newField = new McVTextField(defaultString, limit, upper);
    	newField.setDeny(deny);
    	return newField;
    }
    
    /**
     * Use GroupLayout for stacking components vertically
     * Set center to resize vertically
     * @param top
     * @param center
     * @param bottom
     * @return
     */
    public static JPanel topCenterBottom(JComponent top, JComponent center, JComponent bottom) {
    	JPanel newPanel = new JPanel();
    	    	
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(newPanel);
        newPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(top, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(center, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(bottom, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(top, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(center, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(bottom, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

    	return newPanel;
    }

    /**
     * Use GroupLayout for stacking components vertically
     * @param top
     * @param bottom
     * @param which
     * @return
     */
    public static JPanel topBottom(JComponent top, JComponent bottom, Prefer which) {
    	JPanel newPanel = new JPanel();

    	int topSize=org.jdesktop.layout.GroupLayout.PREFERRED_SIZE;
    	int bottomSize=org.jdesktop.layout.GroupLayout.PREFERRED_SIZE;
    	
    	if (which == Prefer.TOP) topSize = Short.MAX_VALUE;
    	else if (which == Prefer.BOTTOM) topSize = Short.MAX_VALUE;
    		
    	org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(newPanel);
    	newPanel.setLayout(layout);
    	layout.setHorizontalGroup(
    			layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
    			.add(top, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
    			.add(bottom, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
    	);
    	layout.setVerticalGroup(
    			layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
    			.add(layout.createSequentialGroup()
    					.add(top, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, topSize)
    					.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
    					.add(bottom, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, bottomSize))
    	);

    	return newPanel;
    }
        
}
