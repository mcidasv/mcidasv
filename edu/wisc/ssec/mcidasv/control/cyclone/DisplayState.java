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

package edu.wisc.ssec.mcidasv.control.cyclone;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;

import ucar.unidata.util.LogUtil;

/**
 * 
 * @author Unidata Development Team
 * @version $Revision$
 */

public class DisplayState {

	/** the way display state */
	private WayDisplayState wayDisplayState;

	/** for gui */
	private JCheckBox cbx;

	/** is this visible */
	private boolean visible;

	/** the name */
	private String name;

	/**
	 * ctor
	 */
	public DisplayState() {
	}

	/**
	 * ctor
	 * 
	 * 
	 * @param wayDisplayState
	 *            the way display state
	 * @param name
	 *            the name
	 * @param visible
	 *            is this visible
	 */
	public DisplayState(WayDisplayState wayDisplayState, String name,
			boolean visible) {
		this.wayDisplayState = wayDisplayState;
		this.name = name;
		this.visible = visible;
	}

	/**
	 * set background color
	 * 
	 * @param c
	 *            color
	 */
	protected void setBackground(Color c) {
		getCheckBox().setBackground(c);
	}

	/**
	 * make the checkbox
	 * 
	 * 
	 * @return the checkbox
	 */
	public JCheckBox getCheckBox() {
		if (cbx == null) {
			cbx = new JCheckBox("", getVisible());
			cbx.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			cbx.setToolTipText(name);
			cbx.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					try {
						setVisible(cbx.isSelected());
						wayDisplayState.getStormDisplayState()
								.displayStateChanged(DisplayState.this);
					} catch (Exception exc) {
						LogUtil.logException("Toggling way visible", exc);
					}
				}
			});
		}
		return cbx;
	}

	/**
	 * Set the Visible property.
	 * 
	 * @param value
	 *            The new value for Visible
	 */
	public void setVisible(boolean value) {
		visible = value;
	}

	/**
	 * Get the Visible property.
	 * 
	 * @return The Visible
	 */
	public boolean getVisible() {
		return visible;
	}

	/**
	 * Set the Name property.
	 * 
	 * @param value
	 *            The new value for Name
	 */
	public void setName(String value) {
		name = value;
	}

	/**
	 * Get the Name property.
	 * 
	 * @return The Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the WayDisplayState property.
	 * 
	 * @param value
	 *            The new value for WayDisplayState
	 */
	public void setWayDisplayState(WayDisplayState value) {
		wayDisplayState = value;
	}

	/**
	 * Get the WayDisplayState property.
	 * 
	 * @return The WayDisplayState
	 */
	public WayDisplayState getWayDisplayState() {
		return wayDisplayState;
	}

}
