/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
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

package edu.wisc.ssec.mcidasv.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import ucar.unidata.util.GuiUtils;
import edu.wisc.ssec.mcidasv.Constants;


public class McVGuiUtils implements Constants {
    private McVGuiUtils() {}
    
    public enum Width { HALF, SINGLE, ONEHALF, DOUBLE, TRIPLE, QUADRUPLE, DOUBLEDOUBLE }
    public enum Position { LEFT, RIGHT, CENTER }
    public enum Prefer { TOP, BOTTOM, NEITHER }
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
     * Create a standard sized, left-justified label
     * @param title
     * @return
     */
    public static JLabel makeLabelLeft(String title) {
    	return makeLabelLeft(title, null);
    }
    
    public static JLabel makeLabelLeft(String title, Width width) {
    	if (width==null) width=Width.SINGLE;
        JLabel newLabel = new JLabel(title);
        setComponentWidth(newLabel, width);
    	setLabelPosition(newLabel, Position.LEFT);
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
    	return makeLabeledComponent(label, thing, Position.RIGHT);
    }

    public static JPanel makeLabeledComponent(JLabel label, JComponent thing, Position position) {
    	JPanel newPanel = new JPanel();

    	if (position == Position.RIGHT) {
	    	setComponentWidth(label);
	    	setLabelPosition(label, Position.RIGHT);
    	}
    	
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
     * Create a sized, labeled component
     * @param label
     * @param thing
     * @return
     */
    public static JPanel makeComponentLabeled(JComponent thing, String label) {
    	return makeComponentLabeled(thing, new JLabel(label));
    }
    
    public static JPanel makeComponentLabeled(JComponent thing, JLabel label) {
    	return makeComponentLabeled(thing, label, Position.LEFT);
    }

    public static JPanel makeComponentLabeled(JComponent thing, JLabel label, Position position) {
    	JPanel newPanel = new JPanel();

    	if (position == Position.RIGHT) {
	    	setComponentWidth(label);
	    	setLabelPosition(label, Position.RIGHT);
    	}
    	
    	org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(newPanel);
    	newPanel.setLayout(layout);
    	layout.setHorizontalGroup(
    			layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
    			.add(layout.createSequentialGroup()
    					.add(thing)
    					.add(GAP_RELATED)
    					.add(label))
    	);
    	layout.setVerticalGroup(
    			layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
    			.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
    					.add(thing)
    					.add(label))
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

    	case TRIPLE: 
    		setComponentWidth(existingComponent, ELEMENT_DOUBLE_WIDTH + ELEMENT_WIDTH);
    		break;

    	case QUADRUPLE: 
    		setComponentWidth(existingComponent, ELEMENT_DOUBLE_WIDTH + ELEMENT_DOUBLE_WIDTH);
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
    		existingComponent.setForeground(MCV_BLUE_DARK);
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

    	ImageIcon imageIcon = GuiUtils.getImageIcon(iconName);
    	if (imageIcon.getIconWidth() > 22 || imageIcon.getIconHeight() > 22) {
    		Image scaledImage  = imageIcon.getImage().getScaledInstance(22, 22, Image.SCALE_SMOOTH);
    		imageIcon = new ImageIcon(scaledImage);
    	}

    	final JButton btn = GuiUtils.getImageButton(imageIcon);
    	btn.setBackground(null);
    	btn.setSize(new Dimension(24, 24));
    	btn.setPreferredSize(new Dimension(24, 24));
    	btn.setMinimumSize(new Dimension(24, 24));
    	if (addMouseOverBorder) {
    		GuiUtils.makeMouseOverBorder(btn);
    	}
    	btn.setToolTipText(tooltip);
    	return btn;
    }
    
    /**
     * Create a button with text and an icon
     */
    public static JButton makeImageTextButton(String iconName, String label) {
    	JButton newButton = new JButton(label);
    	setButtonImage(newButton, iconName);
    	return newButton;
    }
    
    /**
     * Add an icon to a button... but only if the LookAndFeel supports it
     */
    public static void setButtonImage(JButton existingButton, String iconName) {
    	// TODO: see if this is fixed in some future Apple Java release?
    	// When using Aqua look and feel don't use icons in the buttons
    	// Messes with the button vertical sizing
    	if (existingButton.getBorder().toString().indexOf("Aqua") > 0) return;
    	ImageIcon imageIcon = GuiUtils.getImageIcon(iconName);
    	existingButton.setIcon(imageIcon);
    }

    /**
     * Add an icon to a menu item
     */
    public static void setMenuImage(JMenuItem existingMenuItem, String iconName) {
        ImageIcon imageIcon = GuiUtils.getImageIcon(iconName);
        existingMenuItem.setIcon(imageIcon);
    }

    public static <E> JComboBox makeComboBox(final E[] items, final Object selected) {
        return makeComboBox(CollectionHelpers.list(items), selected);
    }

    public static <E> JComboBox makeComboBox(final E[] items, final Object selected, final Width width) {
        return makeComboBox(CollectionHelpers.list(items), selected, width);
    }

    public static JComboBox makeComboBox(final Collection<?> items, final Object selected) {
        return makeComboBox(items, selected, null);
    }

    public static JComboBox makeComboBox(final Collection<?> items, final Object selected, final Width width) {
        JComboBox newComboBox = getEditableBox(items, selected);
        setComponentWidth(newComboBox, width);
        return newComboBox;
    }

    public static void setListData(final JComboBox box, final Collection<?> items, final Object selected) {
        box.removeAllItems();
        if (items != null) {
            for (Object o : items)
                box.addItem(o);
        }

        if (selected != null && !items.contains(selected))
            box.addItem(selected);
    }

    public static JComboBox getEditableBox(final Collection<?> items, final Object selected) {
        JComboBox fld = new JComboBox();
        fld.setEditable(true);
        setListData(fld, items, selected);
        if (selected != null) {
            fld.setSelectedItem(selected);
        }
        return fld;
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
    
    /**
     * Use GroupLayout for wrapping components to stop vertical resizing
     * @param left
     * @param right
     * @return
     */
    public static JPanel sideBySide(JComponent left, JComponent right) {
    	return sideBySide(left, right, GAP_RELATED);
    }
    
    /**
     * Use GroupLayout for wrapping components to stop vertical resizing
     * @param left
     * @param right
     * @return
     */
    public static JPanel sideBySide(JComponent left, JComponent right, int gap) {
    	JPanel newPanel = new JPanel();

    	org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(newPanel);
    	newPanel.setLayout(layout);
    	layout.setHorizontalGroup(
    			layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
    			.add(layout.createSequentialGroup()
    					.add(left, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
    					.add(gap)
    					.add(right, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    	);
    	layout.setVerticalGroup(
    			layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
    			.add(layout.createSequentialGroup()
    	                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
    	                		.add(left, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
    	                		.add(right, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
    	);

    	return newPanel;
    }
        
    /**
     * Hack apart an IDV button panel and do a few things:
     * - Reorder the buttons based on OS preference
     *   Windows: OK on left
     *   Mac: OK on right
     * - Add icons when we understand the button name
     * 
     * TODO: Revisit this?  Could hamper GUI performance.  But it is niiice...
     * 
     * @param idvButtonPanel
     * @return
     */
    public static JPanel makePrettyButtons(JPanel idvButtonPanel) {    	
    	// These are the buttons we know about
    	JButton buttonOK = null;
    	JButton buttonApply = null;
    	JButton buttonCancel = null;
    	JButton buttonHelp = null;
    	JButton buttonNew = null;
    	JButton buttonReset = null;
    	JButton buttonYes = null;
    	JButton buttonNo = null;
    	
    	// These are the buttons we don't know about
    	List<JButton> buttonList = new ArrayList<JButton>();
    	
    	// First pull apart the panel and see if it looks like we expect
    	Component[] comps = idvButtonPanel.getComponents();
    	for (int i=0; i<comps.length; i++) {
    		if (!(comps[i] instanceof JButton)) continue;
    		JButton button = (JButton)comps[i];
    		if (button.getText().equals("OK")) {
    			buttonOK = makePrettyButton(button);
    		}
    		else if (button.getText().equals("Apply")) {
    			buttonApply = makePrettyButton(button);
    		}
    		else if (button.getText().equals("Cancel")) {
    			buttonCancel = makePrettyButton(button);
    		}
    		else if (button.getText().equals("Help")) {
    			buttonHelp = makePrettyButton(button);
    		}
    		else if (button.getText().equals("New")) {
    			buttonNew = makePrettyButton(button);
    		}
    		else if (button.getText().equals("Reset")) {
    			buttonReset = makePrettyButton(button);
    		}
    		else if (button.getText().equals("Yes")) {
    			buttonYes = makePrettyButton(button);
    		}
    		else if (button.getText().equals("No")) {
    			buttonNo = makePrettyButton(button);
    		}
    		else {
    			buttonList.add(button);
    		}
    	}
    	    	
    	// If we are on a Mac, this is the order (right aligned)
    	// Help, New, Reset, No, Yes, Cancel, Apply, OK
    	if (System.getProperty("os.name").indexOf("Mac OS X") >= 0) {
    		JPanel newButtonPanel = new JPanel();
    		if (buttonHelp!=null) newButtonPanel.add(buttonHelp);
    		if (buttonNew!=null) newButtonPanel.add(buttonNew);
    		if (buttonReset!=null) newButtonPanel.add(buttonReset);
    		if (buttonNo!=null) newButtonPanel.add(buttonNo);
    		if (buttonYes!=null) newButtonPanel.add(buttonYes);
    		if (buttonCancel!=null) newButtonPanel.add(buttonCancel);
    		if (buttonApply!=null) newButtonPanel.add(buttonApply);
    		if (buttonOK!=null) newButtonPanel.add(buttonOK);
    		if (buttonList.size() > 0) 
    			return GuiUtils.right(GuiUtils.hbox(GuiUtils.hbox(buttonList), newButtonPanel));
    		else
    			return(GuiUtils.right(newButtonPanel));
    	}
    	
    	// If we are not on a Mac, this is the order (center aligned)
    	// OK, Apply, Cancel, Yes, No, Reset, New, Help
    	if (System.getProperty("os.name").indexOf("Mac OS X") < 0) {
    		JPanel newButtonPanel = new JPanel();
    		if (buttonOK!=null) newButtonPanel.add(buttonOK);
    		if (buttonApply!=null) newButtonPanel.add(buttonApply);
    		if (buttonCancel!=null) newButtonPanel.add(buttonCancel);
    		if (buttonYes!=null) newButtonPanel.add(buttonYes);
    		if (buttonNo!=null) newButtonPanel.add(buttonNo);
    		if (buttonReset!=null) newButtonPanel.add(buttonReset);
    		if (buttonNew!=null) newButtonPanel.add(buttonNew);
    		if (buttonHelp!=null) newButtonPanel.add(buttonHelp);
    		if (buttonList.size() > 0) 
    			return GuiUtils.center(GuiUtils.hbox(GuiUtils.hbox(buttonList), newButtonPanel));
    		else
    			return(GuiUtils.center(newButtonPanel));
    	}
    	
    	return idvButtonPanel;
    }
    
    /**
     * Take a list of buttons and make them pretty
     * 
     * @param list
     * @return list
     */
    public static List makePrettyButtons(List buttonList) {
    	List newButtons = new ArrayList();
    	for (int i=0; i<buttonList.size(); i++) {
    		if (buttonList.get(i) instanceof JButton)
    			newButtons.add(makePrettyButton((JButton)(buttonList.get(i))));
    		else
    			newButtons.add(buttonList.get(i));
    	}
    	return newButtons;
    }
    
    /**
     * Convenience method to make a button based solely on its name
     */
    public static JButton makePrettyButton(String name) {
    	return makePrettyButton(new JButton(name));
    }
    
    /**
     * - Add icons when we understand the button name
     * 
     * @param button
     * @return button
     */
    public static JButton makePrettyButton(JButton button) {    	    	
    	McVGuiUtils.setComponentWidth(button, Width.ONEHALF);
    	if (button.getText().equals("OK")) {
    		McVGuiUtils.setButtonImage(button, ICON_ACCEPT_SMALL);
    	}
    	else if (button.getText().equals("Apply")) {
    		McVGuiUtils.setButtonImage(button, ICON_APPLY_SMALL);
    	}
    	else if (button.getText().equals("Cancel")) {
    		McVGuiUtils.setButtonImage(button, ICON_CANCEL_SMALL);
    	}
    	else if (button.getText().equals("Help")) {
    		McVGuiUtils.setButtonImage(button, ICON_HELP_SMALL);
    	}
    	else if (button.getText().equals("New")) {
    		McVGuiUtils.setButtonImage(button, ICON_ADD_SMALL);
    	}
    	else if (button.getText().equals("Reset")) {
    		McVGuiUtils.setButtonImage(button, ICON_UNDO_SMALL);
    	}
    	else if (button.getText().equals("Yes")) {
    		McVGuiUtils.setButtonImage(button, ICON_ACCEPT_SMALL);
    	}
    	else if (button.getText().equals("No")) {
    		McVGuiUtils.setButtonImage(button, ICON_CANCEL_SMALL);
    	}
    	else if (button.getText().equals("Close")) {
    		McVGuiUtils.setButtonImage(button, ICON_CANCEL_SMALL);
    	}
    	else if (button.getText().equals("Previous")) {
    		McVGuiUtils.setButtonImage(button, ICON_PREVIOUS_SMALL);
    	}
    	else if (button.getText().equals("Next")) {
    		McVGuiUtils.setButtonImage(button, ICON_NEXT_SMALL);
    	}
    	else if (button.getText().equals("Random")) {
    		McVGuiUtils.setButtonImage(button, ICON_RANDOM_SMALL);
    	}
    	else if (button.getText().equals("Support Form")) {
    		McVGuiUtils.setButtonImage(button, ICON_SUPPORT_SMALL);
    	}
    	return button;
    }
        
    /**
     * Print the hierarchy of components
     */
    public static void printUIComponents(JComponent parent) {
    	printUIComponents(parent, 0, 0);
    }
    public static void printUIComponents(JComponent parent, int index, int depth) {
    	Component[] children = parent.getComponents();
    	int childcount = children.length;
    	
		String indent = "";
		for (int d=0; d<depth; d++) {
			indent += "  ";
		}
		System.out.println(indent + index + ": " + parent);

    	if (childcount > 0) {
    		for (int c=0; c<childcount; c++) {
    			if (children[c] instanceof JComponent) {
        			printUIComponents((JComponent)children[c], c, depth+1);
    			}
    		}
    	}
    }
    
}
