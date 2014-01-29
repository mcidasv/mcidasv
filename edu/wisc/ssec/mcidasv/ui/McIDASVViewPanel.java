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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import edu.wisc.ssec.mcidasv.Constants;

import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.IdvConstants;
import ucar.unidata.idv.IdvManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.TransectViewManager;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.MapDisplayControl;
import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.ViewPanel;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.ui.DndImageButton;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.StringUtil;

/**
 * <p>This class has largely been copied over wholesale from the IDV code. 
 * Merely extending was proving to be as much as a hassle as just copying it, 
 * though now we still maintain complete control over the ViewPanel, and we have 
 * an obvious point of departure for whenever the JTree is started.</p>
 * 
 * <p>That said, I personally recommend avoiding this class until the JTree 
 * stuff is ready to go.</p>
 */
public class McIDASVViewPanel extends IdvManager implements ViewPanel {

	private static final Image BUTTON_ICON =
		GuiUtils.getImage("/auxdata/ui/icons/Selected.gif");

	private static final ImageIcon CATEGORY_OPEN_ICON = 
		GuiUtils.getImageIcon("/auxdata/ui/icons/CategoryOpen.gif");

	private static final ImageIcon CATEGORY_CLOSED_ICON = 
		GuiUtils.getImageIcon("/auxdata/ui/icons/CategoryClosed.gif");

	private static final Border BUTTON_BORDER = 
		BorderFactory.createEmptyBorder(2, 6, 2, 0);

	private static final Font BUTTON_FONT = new Font("Dialog", Font.PLAIN, 11);
	private static final Color LINE_COLOR = Color.gray;
	private static final Font CAT_FONT = new Font("Dialog", Font.BOLD, 11);
	
	/** The border for the header panel */
	public static Border headerNormal = 
		BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3,
			0, 0, 0), BorderFactory.createMatteBorder(0, 0, 2, 0,
				Color.black));

	/** highlight border for view infos */
	public static Border headerHighlight = 
		BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3,
			0, 0, 0), BorderFactory.createMatteBorder(0, 0, 2, 0,
				ViewManager.borderHighlightColor));

	private static Color fgColor = Color.black;
	private static Color onColor = null;
	private static boolean showPopup = true;
	private static boolean showCategories = false;
	
	private JComponent contents;
	
	private JPanel leftPanel;
	
	private JPanel viewContainer;
	
	private ButtonGroup buttonGroup = new ButtonGroup();
	
	private GuiUtils.CardLayoutPanel rightPanel;
	
	private IntegratedDataViewer idv;
	
	private enum ViewManagers { DEFAULT, GLOBE, MAP, TRANSECT };
	
	private Hashtable<DisplayControl, ControlInfo> controlToInfo = 
		new Hashtable<DisplayControl, ControlInfo>();
	
	
	private List<VMInfo> vmInfos = new ArrayList<VMInfo>();
	
	public McIDASVViewPanel(IntegratedDataViewer idv) {
		super(idv);
		
		this.idv = idv;

	}

	public void createUI() {
		
		leftPanel = new JPanel(new BorderLayout());
		leftPanel.setBorder(
			BorderFactory.createCompoundBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
				BorderFactory.createEmptyBorder(0, 0, 0, 0)));
		
		leftPanel.add(BorderLayout.NORTH, GuiUtils.filler(150, 1));

		viewContainer = new JPanel();
		viewContainer.setLayout(new BoxLayout(viewContainer, BoxLayout.Y_AXIS));

		JScrollPane viewScroll = new JScrollPane(GuiUtils.top(viewContainer));
		viewScroll.setBorder(null);
		
		leftPanel.add(BorderLayout.CENTER, viewScroll);
		
		rightPanel = new GuiUtils.CardLayoutPanel() {
			public void show(Component comp) {
				super.show(comp);
				for (VMInfo vinfo : vmInfos) {
					for (ControlInfo cinfo : vinfo.controlInfos) {
						if (cinfo.outer == comp) {
							cinfo.button.setSelected(true);
							break;
						}
					}
				}
			}
		};
		
		contents = GuiUtils.leftCenter(leftPanel, rightPanel);
		Msg.translateTree(contents);
	}

	public void selectNext(boolean up) {
		boolean gotit  = false;
		VMInfo  select = null;
		int index  = 0;
		for (int vmIdx = 0; !gotit && (vmIdx < vmInfos.size()); vmIdx++) {
			
			VMInfo vmInfo = vmInfos.get(vmIdx);
			
			List<ControlInfo> ctrlInfos = vmInfo.controlInfos;
			
			for (int i = 0; i < ctrlInfos.size(); i++) {
				ControlInfo ci = ctrlInfos.get(i);
			
				if ( !ci.button.isSelected())
					continue;

				if (up) {
					if (vmInfo.getCatOpen() && (i > 0)) {
						select = vmInfo;
						index  = i - 1;
					} else {
						vmIdx--;
						while (vmIdx >= 0) {
							VMInfo prev = vmInfos.get(vmIdx--);
							if (prev.getCatOpen() && (prev.controlInfos.size() > 0)) {
								select = prev;
								index  = select.controlInfos.size() - 1;
								break;
							}
						}
					}
				} else {
					if (vmInfo.getCatOpen() && (i < ctrlInfos.size() - 1)) {
						select = vmInfo;
						index  = i + 1;
					} else {
						vmIdx++;
						while (vmIdx < vmInfos.size()) {
							VMInfo next = vmInfos.get(vmIdx++);
							if (next.getCatOpen() && (next.controlInfos.size() > 0)) {
								select = next;
								index  = 0;
								break;
							}
						}
					}
				}
				gotit = true;
				break;
			}
		}

		if ((select != null) && (index >= 0) && (index < select.controlInfos.size()))
			select.controlInfos.get(index).button.doClick();
	}
	
	public void addControlTab(DisplayControl control, boolean forceShow) {
		if (!control.canBeDocked() || !control.shouldBeDocked())
			return;

		//Check if there are any groups that have autoimport set
		ViewManager viewManager = control.getViewManager();
		if (viewManager != null) {
			IdvWindow window = viewManager.getDisplayWindow();
			if (window != null) {
				List groups = window.getComponentGroups();
				for (int i = 0; i < groups.size(); i++) {
					Object obj = groups.get(i);
					if (obj instanceof IdvComponentGroup) {
						if (((IdvComponentGroup) obj)
								.tryToImportDisplayControl(
									(DisplayControlImpl)control)) {
							return;
						}
					}
				}
			}
		}

		ControlInfo info = controlToInfo.get(control);
		if (info != null)
			return;

		//For now cheat a little with the cast
		((DisplayControlImpl)control).setMakeWindow(false);

		JButton removeBtn =
			GuiUtils.makeImageButton("/auxdata/ui/icons/Remove16.gif",
				control, "doRemove");
		removeBtn.setToolTipText("Remove Display Control");

		JButton expandBtn =
			GuiUtils.makeImageButton("/auxdata/ui/icons/DownDown.gif", this,
				"expandControl", control);
		expandBtn.setToolTipText("Expand in the tabs");

		JButton exportBtn =
			GuiUtils.makeImageButton("/auxdata/ui/icons/Export16.gif", this,
				"undockControl", control);
		exportBtn.setToolTipText("Undock control window");

		JButton propBtn =
			GuiUtils.makeImageButton("/auxdata/ui/icons/Information16.gif",
				control, "showProperties");
		propBtn.setToolTipText("Show Display Control Properties");

		DndImageButton dnd = new DndImageButton(control, "idv/display");
		dnd.setToolTipText("Drag and drop to a window component");
//		JPanel buttonPanel =
//			GuiUtils.left(GuiUtils.hbox(Misc.newList(expandBtn, exportBtn,
//				propBtn, removeBtn, dnd), 4));
		JPanel buttonPanel =
			GuiUtils.left(GuiUtils.hbox(Misc.newList(exportBtn,
				propBtn, removeBtn, dnd), 4));

		buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0,
			Color.lightGray.darker()));

		JComponent inner =
			(JComponent) ((DisplayControlImpl)control).getOuterContents();
		inner = GuiUtils.centerBottom(inner, buttonPanel);
		JComponent outer = GuiUtils.top(inner);
		outer.setBorder(BorderFactory.createEmptyBorder(2, 1, 0, 0));
		
		info = new ControlInfo(control, expandBtn, outer, inner,
			getVMInfo(control.getDefaultViewManager()));
		
		controlToInfo.put(control, info);
		
		GuiUtils.toggleHeavyWeightComponents(outer, false);
		if (!getStateManager().getProperty(IdvConstants.PROP_LOADINGXML, false)) {

			//A hack for now
			if (!(control instanceof MapDisplayControl)) {
				GuiUtils.toggleHeavyWeightComponents(outer, true);
				GuiUtils.showComponentInTabs(outer);
			}

		}
	}
	
	public void expandControl(DisplayControl control) {
		ControlInfo info = controlToInfo.get(control);
		if (info != null)
			info.expand();
	}
	
	public void dockControl(DisplayControl control) {
		control.setShowInTabs(true);
		((DisplayControlImpl)control).guiImported();
		addControlTab(control, true);
	}
	
	public void undockControl(DisplayControl control) {
		removeControlTab(control);
		control.setShowInTabs(false);
		((DisplayControlImpl)control).setMakeWindow(true);
		((DisplayControlImpl)control).popup(null);
	}
	
	public void controlMoved(DisplayControl control) {
		removeControlTab(control);
		addControlTab(control, true);
	}
	
	public void removeControlTab(DisplayControl control) {
		ControlInfo info = controlToInfo.remove(control);
		if (info != null)
			info.removeDisplayControl();
	}
	
	public JComponent getContents() {
		if (contents == null)
			createUI();
		
		return contents;
	}
	
	public void addDisplayControl(DisplayControl control) {
		addControlTab(control, false);
	}
	
	public void displayControlChanged(DisplayControl control) {
		ControlInfo info = controlToInfo.get(control);
		if (info != null)
			info.displayControlChanged();
	}

	public void removeDisplayControl(DisplayControl control) {
		removeControlTab(control);
	}
	
	public void addViewMenuItems(DisplayControl control, List items) {
		if (!control.canBeDocked())
			return;
		
		items.add(GuiUtils.MENU_SEPARATOR);
		
		if (!control.shouldBeDocked())
			items.add(GuiUtils.makeMenuItem("Dock in Data Explorer", this, "dockControl", control));
		else
			items.add(GuiUtils.makeMenuItem("Undock from Data Explorer", this, "undockControl", control));
		
		List groups = getIdvUIManager().getComponentGroups();
		List<JMenuItem> subItems = new ArrayList<JMenuItem>();
		for (int i = 0; i < groups.size(); i++) {
			IdvComponentGroup group = (IdvComponentGroup)groups.get(i);
			subItems.add(GuiUtils.makeMenuItem(group.getHierachicalName(), group, "importDisplayControl", control));
		}
		
		if (subItems.size() > 0)
			items.add(GuiUtils.makeMenu("Export to component", subItems));
	}
	
	public void viewManagerAdded(ViewManager vm) {
		// this forces the addition of the ViewManager
		getVMInfo(vm);
	}
	
	public void viewManagerDestroyed(ViewManager vm) {
		VMInfo info = findVMInfo(vm);
		if (info != null) {
			vmInfos.remove(info);
			info.viewManagerDestroyed();
//			System.err.println("destroying "+info+" for "+vm);
		}
	}
	
	/**
	 * Triggered upon a change in the given ViewManager. Just used so that our
	 * ControlInfo object can update its internal state.
	 * 
	 * @param vm The ViewManager that's changed.
	 */
	public void viewManagerChanged(ViewManager vm) {
		VMInfo info = findVMInfo(vm);
		if (info != null)
			info.viewManagerChanged();
	}

	/**
	 * Initialize the button state.
	 */
	protected void initButtonState() {
		if (fgColor != null)
			return;

		fgColor = Color.black;

		showPopup = idv.getProperty(Constants.PROP_VP_SHOWPOPUP, false);

		showCategories = idv.getProperty(Constants.PROP_VP_SHOWCATS, false);
	}
	
	public VMInfo getVMInfo(ViewManager vm) {
		VMInfo info = findVMInfo(vm);
		if (info == null) {

			// oh no :(
			if (vm instanceof MapViewManager)
				if (((MapViewManager)vm).getUseGlobeDisplay())
					info = new VMInfo(vm, ViewManagers.GLOBE);
				else
					info = new VMInfo(vm, ViewManagers.MAP);
			else if (vm instanceof TransectViewManager)
				info = new VMInfo(vm, ViewManagers.TRANSECT);
			else
				info = new VMInfo(vm, ViewManagers.DEFAULT);

			vmInfos.add(info);
		}
		return info;
	}

	public VMInfo findVMInfo(ViewManager vm) {
		for (VMInfo info : vmInfos)
			if (info.holds(vm))
				return info;

		return null;
	}

	public class VMInfo implements ImageObserver {
		private ViewManager viewManager;
		
		private JButton popupButton;
		
		private JComponent tabContents = new JPanel(new BorderLayout());
		
		private JPanel headerPanel;
		
		private boolean ignore = false;
		
		// private list of controlinfos?
		private List<ControlInfo> controlInfos = new ArrayList<ControlInfo>();
		private List<JToggleButton> buttons = new ArrayList<JToggleButton>();
		
		//private JComponent buttonPanel;
		
		private JComponent contents;
		
		private JLabel viewLabel;
		
		private JButton catToggle;
		
		private boolean catOpen = true;
		
		private KeyListener listener;
		
		private List<String> categories = new ArrayList<String>();
		
		private ViewManagers myType = ViewManagers.DEFAULT;
		
		public VMInfo(ViewManager vm, ViewManagers type) {

			listener = new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_UP)
						selectNext(true);
					else if (e.getKeyCode() == KeyEvent.VK_DOWN)
						selectNext(false);
				}
			};

			initButtonState();
			BUTTON_ICON.getWidth(this);

			viewManager = vm;

			ImageIcon icon = ICON_DEFAULT;
			if (type == ViewManagers.GLOBE)
				icon = ICON_GLOBE;
			else if (type == ViewManagers.MAP)
				icon = ICON_MAP;
			else if (type == ViewManagers.TRANSECT)
				icon = ICON_TRANSECT;
			
			viewLabel = new JLabel(" " + getLabel());
			viewLabel.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (viewManager == null)
						return;
					
					getVMManager().setLastActiveViewManager(viewManager);
					if (e.getClickCount() == 2)
						viewManager.toFront();
				}
			});

			catToggle = GuiUtils.getImageButton(getCatOpen() ? CATEGORY_OPEN_ICON : CATEGORY_CLOSED_ICON);
			catToggle.addKeyListener(listener);
			catToggle.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setCatOpen(!getCatOpen());
				}
			});

			popupButton = new JButton(icon);
			popupButton.addKeyListener(listener);
			popupButton.setContentAreaFilled(false);
			popupButton.addActionListener(GuiUtils.makeActionListener(VMInfo.this, "showPopupMenu", null));
			popupButton.setToolTipText("Show View Menu");
			popupButton.setBorder(BorderFactory.createEmptyBorder());

			headerPanel = GuiUtils.leftCenter(GuiUtils.hbox(
					GuiUtils.inset(catToggle, 1),
					popupButton), viewLabel);

			if (viewManager != null)
				headerPanel = viewManager.makeDropPanel(headerPanel, true);

			JComponent headerWrapper = GuiUtils.center(headerPanel);
			headerPanel.setBorder(headerNormal);
			contents = GuiUtils.topCenter(headerWrapper, tabContents);
			viewContainer.add(contents);
			popupButton.setHorizontalAlignment(SwingConstants.LEFT);
			buttonsChanged();

			setCatOpen(getCatOpen());

			if (viewManager != null)
				viewManagerChanged();
		}

		public boolean getCatOpen() {
			if (viewManager != null) {
				Boolean b = 
					(Boolean)viewManager.getProperty(Constants.PROP_VP_CATOPEN);
				if (b != null)
					return b;
			}

			return catOpen;
		}

		public void setCatOpen(boolean v) {
			if (viewManager != null)
				viewManager.putProperty(Constants.PROP_VP_CATOPEN, v);

			catOpen = v;
			catToggle.setIcon(v ? CATEGORY_OPEN_ICON : CATEGORY_CLOSED_ICON);
			tabContents.setVisible(v);
		}

		public void showPopupMenu() {
			if (viewManager == null)
				return;

			List items = new ArrayList();
			viewManager.addContextMenuItems(items);
			JPopupMenu popup = GuiUtils.makePopupMenu(items);
			popup.show(popupButton, 0, popupButton.getHeight());
		}

		/**
		 * Determine if this VMInfo contains a given ViewManager.
		 * 
		 * @param vm The ViewManager you wish to test.
		 * 
		 * @return True if this VMInfo contains <tt>vm</tt>, false otherwise.
		 */
		public boolean holds(ViewManager vm) {
			return viewManager == vm;
		}

		public void removeControlInfo(ControlInfo info) {
			int idx = controlInfos.indexOf(info);
			if (idx == -1)
				return;
			
			int btnIdx = buttons.indexOf(info.button);
			controlInfos.remove(info);
			rightPanel.remove(info.outer);

			if (info.button.isSelected() && (buttons.size() > 0)) {
				while ((btnIdx >= buttons.size()) && (btnIdx >= 0))
					btnIdx--;

				if (btnIdx >= 0)
					buttons.get(btnIdx).doClick();
			}

			GuiUtils.toggleHeavyWeightComponents(info.outer, true);

			buttonsChanged();

			// hmm -- this must be for synchronization?
			ignore = true;
			buttonGroup.remove(info.button);
			ignore = false;

			// if there are still control infos left then we'll use those?
			if (controlInfos.size() > 0)
				return;

			// otherwise we need to click the buttons of each remaining viewmanager?
			for (VMInfo vm : vmInfos)
				if (vm.controlInfos.size() > 0)
					vm.controlInfos.get(0).button.doClick();
		}

		public void changeControlInfo(ControlInfo info) {
			if (!Misc.equals(info.lastCategory, info.control.getDisplayCategory()))
				buttonsChanged();
		}

		public void paintButton(Graphics g, ControlInfo info) {
			g.setFont(BUTTON_FONT);
			FontMetrics fm = g.getFontMetrics(g.getFont());

			JToggleButton btn = info.button;
			Rectangle b = btn.getBounds();
			String text = info.getLabel();
			int y = (btn.getHeight() + fm.getHeight()) / 2 - 2;
			int buttonWidth = BUTTON_ICON.getWidth(null);
			int offset = 2 + buttonWidth + 4;
			g.setColor(btn.getBackground());
			g.fillRect(0, 0, b.width, b.height);

			if (btn.isSelected()) {

				if (onColor == null) {
					Color c = btn.getBackground();
					//Just go a little bit darker than the normal background
					onColor = new Color((int) Math.max(0,
							c.getRed() - 20), (int) Math.max(0,
								c.getGreen() - 20), (int) Math.max(0,
									c.getBlue() - 20));
				}

				g.setColor(onColor);
				g.fillRect(offset - 1, 0, b.width, b.height);
			}
			g.setColor(LINE_COLOR);

			g.drawLine(offset - 1, b.height - 1, b.width, b.height - 1);

			g.setColor(fgColor);
			int rightSide = b.width;
			if (btn.isSelected())
				rightSide = b.width - buttonWidth - 2;

			int textPos = offset;
			int textRight = textPos + fm.stringWidth(text);
			if (textRight >= rightSide) {
				while ((text.length() > 5) && (textRight >= rightSide)) {
					text = text.substring(0, text.length() - 2);
					textRight = textPos + fm.stringWidth(text + ".");
				}
				text = text + ".";
			}
			g.drawString(text, offset, y);

			if (!btn.isSelected())
				return;
			
			int height = BUTTON_ICON.getHeight(null);
			g.drawImage(BUTTON_ICON, b.width - 2 - buttonWidth, b.height / 2 - height / 2, null);
		}

		public void addControlInfo(final ControlInfo info) {
			// ugly :(
			// why even have b?
			//JToggleButton b = info.button = new JToggleButton(StringUtil.padRight("", 20), true) {
			info.button = new JToggleButton(StringUtil.padRight("", 20), true) {
				public void paint(Graphics g) {
					paintButton(g, info);
				}
			};
			
			info.button.addKeyListener(listener);
			info.button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			info.button.setToolTipText(info.getLabel());
			info.button.setFont(BUTTON_FONT);
			info.button.setForeground(fgColor);
			info.button.setBorder(BUTTON_BORDER);

			info.button.setHorizontalAlignment(SwingConstants.LEFT);
			info.button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (ignore)
						return;

					GuiUtils.toggleHeavyWeightComponents(info.outer, true);
					rightPanel.show(info.outer);
				}
			});
			buttons.add(info.button);
			controlInfos.add(info);
			rightPanel.addCard(info.outer);
			GuiUtils.toggleHeavyWeightComponents(info.outer, false);
			info.displayControlChanged();

			if (info.control.getExpandedInTabs())
				info.expand();

			buttonGroup.add(info.button);
			buttonsChanged();
			setCatOpen(getCatOpen());
		}

		/**
		 * Redo the buttons
		 */
		private void buttonsChanged() {
			List<JComponent> comps  = new ArrayList<JComponent>();

			Hashtable<String, List<JComponent>> catMap = 
				new Hashtable<String, List<JComponent>>();

			for (ControlInfo info : controlInfos) {
				String cat = info.control.getDisplayCategory();
				if (cat == null)
					cat = "Displays";

				info.lastCategory = cat;

				if (!showCategories) {
					comps.add(info.button);
					continue;
				}

				List<JComponent> catList = catMap.get(cat);
				if (catList == null) {
					if (!categories.contains(cat))
						categories.add(cat);

					catList = new ArrayList<JComponent>();

					catMap.put(cat, catList);

					JLabel catLabel = new JLabel(" " + cat);

					catLabel.setFont(CAT_FONT);

					catList.add(catLabel);
				}
				catList.add(info.button);
			}

			if (showCategories) {
				for (String category : categories) {
					List<JComponent> catList = catMap.get(category);
					if (catList != null)
						comps.addAll(catList);
				}
			}

			if (comps.size() == 0) {
			    if (myType == ViewManagers.TRANSECT) {
			        JLabel noLbl = new JLabel("No Displays");
			        noLbl.setFont(BUTTON_FONT);
			        JPanel inset = GuiUtils.inset(noLbl, new Insets(0, 10, 0, 0));
			        inset.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
			                Color.gray));
			        comps.add(inset);
			    } else {
			        headerPanel.setVisible(false);
			    }
			} else {
			    headerPanel.setVisible(true);
			}

			comps.add(GuiUtils.filler(10, 2));
			JComponent buttonPanel = GuiUtils.vbox(comps);

			tabContents.removeAll();
			tabContents.add(BorderLayout.NORTH, buttonPanel);
			tabContents.repaint();
		}

		/**
		 * Handles ViewManager removal.
		 */
		public void viewManagerDestroyed() {
			viewContainer.remove(contents);
		}

		/**
		 * my viewmanager has changed. Update the gui.
		 */
		public void viewManagerChanged() {
			viewLabel.setText(getLabel());

			if (viewManager.showHighlight()) {

				headerHighlight =
					BorderFactory.createCompoundBorder(
						BorderFactory.createEmptyBorder(3, 0, 0, 0),
						BorderFactory.createMatteBorder(
							0, 0, 2, 0,
							getStore().get(
								ViewManager.PREF_BORDERCOLOR, Color.blue)));

				headerPanel.setBorder(headerHighlight);
			} else {
				headerPanel.setBorder(headerNormal);
			}
			
			if (contents != null)
				contents.repaint();
		}

		/**
		 * Get the ViewManager label. If the ViewManager does not already have
		 * a valid name, a default name is created, based on the number of 
		 * existing ViewManagers.
		 *
		 * @return label The ViewManager's name.
		 */
		public String getLabel() {
			// nothing to query for a name?
			if (viewManager == null)
				return "No Display";

			// do we already have a valid name?
			String name = viewManager.getName();
			if ((name != null) && (name.trim().length() > 0)) {
				UIManager uiManager = (UIManager)idv.getIdvUIManager();
				ComponentHolder holder = uiManager.getViewManagerHolder(viewManager);
				if (holder != null)
					return holder.getName() + ">" + name;
				else
					return name;
			}

			// if our name was invalid, build a default one.
			int idx = vmInfos.indexOf(this);
			return "Default " + Constants.PANEL_NAME + " " + 
				((idx == -1) ? vmInfos.size() : idx);
		}

		public boolean imageUpdate(Image img, int flags, int x, int y, int width, int height) {
			if ((flags & ImageObserver.ALLBITS) == 0)
				return true;

			leftPanel.repaint();
			return false;
		}
	}

	public class ControlInfo {
		DisplayControl control;
		JButton expandButton;
		JComponent outer;
		JComponent inner;
		boolean expanded = false;
		Dimension innerSize = new Dimension();
		VMInfo info;
		JToggleButton button;
		String lastCategory = "";
		String label = null;

		/**
		 * ctor
		 *
		 * @param control control
		 * @param expandButton expand button
		 * @param outer outer comp
		 * @param inner inner comp
		 * @param vmInfo my vminfo
		 */
		public ControlInfo(DisplayControl control, JButton expandButton,
							JComponent outer, JComponent inner,
							VMInfo vmInfo) {
			this.info  = vmInfo;
			this.control = control;
			if (control.getExpandedInTabs())
				expanded = false;

			this.expandButton = expandButton;
			this.outer = outer;
			this.inner = inner;
			inner.getSize(innerSize);
			info.addControlInfo(this);
			this.expand();
		}

		/**
		 * get the label for the display control
		 *
		 * @return display control label
		 */
		public String getLabel() {
			if (label == null)
				label = control.getMenuLabel();

			return label;
		}

		/**
		 * display control changed
		 */
		public void displayControlChanged() {
			String tmp = label;
			label = null;
			getLabel();
			if (!Misc.equals(tmp, label) && (button != null)) {
				button.setToolTipText(label);
				button.repaint();
			}
			info.changeControlInfo(this);
		}

		/**
		 * display control is removed
		 */
		public void removeDisplayControl() {
			info.removeControlInfo(this);
		}

		/**
		 * Expand the contents
		 */
		public void expand() {
			outer.removeAll();
			outer.setLayout(new BorderLayout());

			if (!expanded) {
				outer.add(BorderLayout.CENTER, inner);
				expandButton.setIcon(
					GuiUtils.getImageIcon("/auxdata/ui/icons/UpUp.gif"));
				inner.getSize(innerSize);
//				System.err.println("ControlInfo.expand: innerSize=" + innerSize);
			} else {
				outer.add(BorderLayout.NORTH, inner);
				expandButton.setIcon(
					GuiUtils.getImageIcon("/auxdata/ui/icons/DownDown.gif"));
				inner.setSize(innerSize);
			}

			expanded = !expanded;
			control.setExpandedInTabs(expanded);

			final Container parent = outer.getParent();
			outer.invalidate();
			parent.validate();
			parent.doLayout();
		}
	}
}
