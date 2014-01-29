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

package edu.wisc.ssec.mcidasv.ui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * A {@link javax.swing.JTabbedPane} implementation that allows tabbed heavy-weight 
 * components. When a component is added to a tab it is cached and an associated 
 * light-weight stand-in component and added instead. When a tab is selected the 
 * light-weight stand in is removed and it's heavy-weight counter-part is displayed.  
 * When another tab is selected the reverse happens.
 * <p>
 * This was originally written to facilitate the use of <tt>Canvas3D</tt> objects in 
 * a <tt>JTabbedPane</tt>, but I believe it will work for any heavy-weight component.
 * <p>
 * 
 * @author <a href="http://www.ssec.wisc.edu/cgi-bin/email_form.cgi?name=Flynn,%20Bruce">Bruce Flynn, SSEC</a>
 * @version $Id$
 */
public class HeavyTabbedPane extends JTabbedPane {
	private static final long serialVersionUID = -3903797547171213551L;
	
	/**
	 * Delay in milliseconds for <tt>ChangeEvent</tt>s. This prevents some
	 * re-draw issues that popup with the heavy weight components.
	 */
	protected long heavyWeightDelay = 0;
	
	/**
	 * Components, in tab index order, that will be displayed when a
	 * tab is selected.
	 */
	private List<Component> comps = new ArrayList<Component>();
	/**
	 * Components, in tab index order, that will be displayed when a
	 * tab is not selected. These should never actually be visible to the
	 * user.
	 */
	private List<Component> blanks = new ArrayList<Component>();
	
	/**
	 * Create and return the component to be used when a tab is not visible.
	 * @return Component used for tabs that are not currently selected.
	 */
	protected Component blank() {
		return new JPanel();
	}
	
	/**
	 * Set the delay to wait before firing a state change event.
	 * @param d If >= 0, no delay will be used.
	 */
	protected void setHeavyWeightDeleay(long d) {
		if (d < 0) d = 0;
		heavyWeightDelay = d;
	}
	
	@Override
	public void insertTab(String title, Icon ico, Component comp, String tip, int idx) {
		Component blank = blank();
		blanks.add(idx, blank);
		comps.add(idx, comp);
		super.insertTab(title, ico, blank, tip, idx);
	}

	@Override
	public int indexOfComponent(Component comp) {
		// if the tab count does not equal the size of the component caches
		// this was probably called by something internal. This ensures we
		// don't return an errant value.
		if (getTabCount() == blanks.size() && getTabCount() == comps.size()) {
			if (comps.contains(comp)) {
				return comps.indexOf(comp);
			} else if (blanks.contains(comp)) {
				return blanks.indexOf(comp);
			}
		}
		return -1;
	}
	
	@Override
	public Component getComponentAt(int idx) {
		// return the actual component, not the blank
		return comps.get(idx);
	}
	
	@Override
	public void setComponentAt(int idx, Component comp) {
		// no need to change the blanks
		comps.set(idx, comp);
		super.setComponentAt(idx, comp);
	}
	
	@Override
	public void setSelectedIndex(int idx) {
		int prevIdx = getSelectedIndex();
		super.setSelectedIndex(idx);
		// show the actual component for the selected index and change
		// the other to it's blank
		if (prevIdx != -1 && idx != -1) {
			super.setComponentAt(prevIdx, blanks.get(prevIdx));
			super.setComponentAt(idx, comps.get(idx));
		}
	}
	
	@Override
	public void setSelectedComponent(Component comp) {
		if (comp == null || comps.indexOf(comp) < 0) {
			throw new IllegalArgumentException("Component not found in tabbed pane");
		}
		int idx = comps.indexOf(comp);
		setSelectedIndex(idx);
	}
	
	@Override
	public void removeTabAt(int idx) {
		super.removeTabAt(idx);
		comps.remove(idx);
		blanks.remove(idx);
	}
	
	@Override
	public void remove(int idx) {
		removeTabAt(idx);
	}
	
	@Override
	public void removeAll() {
		super.removeAll();
		comps.clear();
		blanks.clear();
	}
	
	/**
	 * <tt>ChangeEvent</tt> are delayed by the heavy weight delay 
	 * milliseconds to aid in the proper rendering of heavy weight components.
	 * @see javax.swing.JTabbedPane#fireStateChanged()
	 */
	@Override
	protected void fireStateChanged() {
		try {
			Thread.sleep(heavyWeightDelay);
		} catch (InterruptedException e) {}
		super.fireStateChanged();
	}
	
//	public static void main(String[] args) throws Exception {
//		javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());
//		JFrame frame = new JFrame("J3DTabbedPane");
//		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//		final JTabbedPane tabs = new HeavyTabbedPane();
//		frame.setLayout(new BorderLayout());
//		frame.add(tabs, BorderLayout.CENTER);
//		JPanel panel = new JPanel();
//		panel.setLayout(new BorderLayout());
//		panel.setName("BluePanel");
//		panel.add(new JLabel("Actual"), BorderLayout.BEFORE_FIRST_LINE);
//		panel.setBackground(Color.BLUE);
//		DisplayImpl display = new DisplayImplJ3D("Blue");
//		panel.add(display.getComponent(), BorderLayout.CENTER);
//		tabs.add("BluePanel", panel);
//		panel = new JPanel();
//		panel.setLayout(new BorderLayout());
//		display = new DisplayImplJ3D("Red");
//		panel.add(display.getComponent(), BorderLayout.CENTER);
//		panel.setName("RedPanel");
//		panel.add(new JLabel("Actual"), BorderLayout.BEFORE_FIRST_LINE);
//		panel.setBackground(Color.RED);
//		tabs.add("RedPanel", panel);
//		frame.setSize(400, 600);
//		frame.setVisible(true);
//		
//		tabs.addChangeListener(new ChangeListener() {
//			public void stateChanged(ChangeEvent e) {
//				System.err.println();
//				for (int i=0; i<tabs.getTabCount(); i++) {
//					System.err.println("Tab " + i + " " + tabs.getComponentAt(i).getName());
//				}
//			}
//		});
//	}
}
