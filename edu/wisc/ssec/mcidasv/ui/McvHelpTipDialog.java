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


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;


/**
 * Represents a dialog that holds {@literal "help tips"}. This class is based
 * upon {@link ucar.unidata.ui.HelpTipDialog}, but adds new functionality:
 * <ul>
 * <li>tip counter</li>
 * <li>formatting</li>
 * <li>random tips</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class McvHelpTipDialog extends JDialog implements Constants, 
    HyperlinkListener 
{

	/** help tip preference */
	public static final String PREF_HELPTIPSHOW = "help.helptip.Show";

	/** help tip index */
	public static final String PREF_HELPTIPIDX = "help.helptip.Index";

	/** list of tips */
	private List helpTips = new ArrayList();

	/** resources */
	private XmlResourceCollection resources;

	/** index */
	private int idx = 0;

	/** count */
	private JLabel counterText;

	/** label */
	private JLabel titleText;

	/** message */
	private JEditorPane messageText;

	/** checkbox */
	private JCheckBox showCbx;

	/** store */
	private XmlObjectStore store;

	/** action listener */
	private ActionListener actionListener;

	/**
	 * Create the HelpTipDialog
	 *
	 * @param resources    list of XML resources
	 * @param actionListener  listener for changes
	 * @param store           store for persistence
	 * @param origin          calling class
	 * @param showByDefault   true to show by default
	 *
	 */
	public McvHelpTipDialog(XmlResourceCollection resources,
			ActionListener actionListener, XmlObjectStore store,
			Class origin, boolean showByDefault) {

		this.actionListener = actionListener;
		this.resources      = resources;
		if ((resources == null) || (resources.size() == 0)) {
			return;
		}
		this.store = store;

		String title = null;
		String icon  = null;

		for (int i = 0; i < resources.size(); i++) {
			Element helpTipRoot = resources.getRoot(i);
			if (helpTipRoot == null) {
				continue;
			}
			if (title == null) {
				title = XmlUtil.getAttribute(helpTipRoot, "title", (String) null);
			}
			if (icon == null) {
				icon = XmlUtil.getAttribute(helpTipRoot, "icon", (String) null);
			}
			helpTips.addAll(XmlUtil.findChildren(helpTipRoot, "helptip"));
		}

		if (title == null) {
			title = "McIDAS-V Help Tips";
		}
		setTitle(title);
		if (icon == null) {
			icon = "/edu/wisc/ssec/mcidasv/resources/icons/toolbar/dialog-information32.png";
		}
		JLabel imageLabel = GuiUtils.getImageLabel(icon);

		// Build the "Tips" menu
		JMenu     topMenu    = new JMenu("Tips");
		Hashtable menus      = new Hashtable();
		menus.put("top", topMenu);
		for (int i = 0; i < helpTips.size(); i++) {
			Element helpTip = (Element) helpTips.get(i);
			String tipTitle = XmlUtil.getAttribute(helpTip, "title", (String) null);
			if (tipTitle == null) {
				tipTitle = getMessage(helpTip).substring(0, 20);
			}
			if (tipTitle.trim().length() == 0) {
				continue;
			}

			String category = XmlUtil.getAttribute(helpTip, "category", "None");
			JMenu m = (JMenu) menus.get(category);
			if (m == null) {
				m = new JMenu(category);
				menus.put(category, m);
				topMenu.add(m);
			}
			JMenuItem mi = new JMenuItem(tipTitle);
			mi.addActionListener(new ObjectListener(new Integer(i)) {
				public void actionPerformed(ActionEvent ae) {
					idx = ((Integer) theObject).intValue();
					showTip();
				}
			});
			m.add(mi);
		}

		titleText = new JLabel("Title");
		counterText = McVGuiUtils.makeLabelRight("0/0");

		JPanel topPanel = GuiUtils.left(
				GuiUtils.hbox(imageLabel, titleText, GAP_RELATED)
		);

		Dimension helpDimension = new Dimension(400, 200);
		messageText     = new JEditorPane();
		messageText.setMinimumSize(helpDimension);
		messageText.setPreferredSize(helpDimension);
		messageText.setEditable(false);
		messageText.addHyperlinkListener(this);
		messageText.setContentType("text/html");
		Font font = javax.swing.UIManager.getFont("Label.font");
		String rule = "body { font-family:"+font.getFamily()+"; font-size:"+font.getSize()+"pt; }";
		((HTMLDocument)messageText.getDocument()).getStyleSheet().addRule(rule);
		//        messageText.setBackground(new JPanel().getBackground());
		JScrollPane scroller = GuiUtils.makeScrollPane(messageText, 0, 0);
		scroller.setBorder(BorderFactory.createLoweredBevelBorder());
		scroller.setPreferredSize(helpDimension);
		scroller.setMinimumSize(helpDimension);

		showCbx = new JCheckBox("Show tips on startup", showByDefault);
		showCbx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				writeShowNextTime();
			}
		});

		JPanel centerPanel = GuiUtils.center(scroller);

		JButton prevBtn = McVGuiUtils.makeImageButton(ICON_PREVIOUS_SMALL, "Previous");
		prevBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				previous();
			}
		});

		JButton nextBtn = McVGuiUtils.makeImageButton(ICON_NEXT_SMALL, "Next");
		nextBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				next();
			}
		});

		JButton randBtn = McVGuiUtils.makeImageButton(ICON_RANDOM_SMALL, "Random");
		randBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				random();
			}
		});

		JButton closeBtn = McVGuiUtils.makePrettyButton("Close");
		closeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				close();
			}
		});

//		JPanel navBtns = GuiUtils.hbox(prevBtn, randBtn, nextBtn, GAP_RELATED);
		JPanel navBtns = GuiUtils.hbox(counterText, prevBtn, nextBtn, GAP_RELATED);
		JPanel bottomPanel = GuiUtils.leftRight(showCbx, navBtns);

		JMenuBar bar = new JMenuBar();
		bar.add(topMenu);
		add("North", bar);

		JPanel contents = GuiUtils.topCenterBottom(
				GuiUtils.inset(topPanel, GAP_RELATED),
				GuiUtils.inset(centerPanel, new Insets(0, GAP_RELATED, 0, GAP_RELATED)),
				GuiUtils.inset(bottomPanel, GAP_RELATED)
		);
		contents.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,
				Color.gray, Color.gray));

		JPanel bottom = new JPanel();
		bottom.add(closeBtn);
		add(GuiUtils.centerBottom(contents, bottom));
		pack();

		random();
		Dimension size = getSize();
		Dimension ss   = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(ss.width / 2 - size.width / 2, ss.height / 2 - size.height / 2);

		setMinimumSize(helpDimension);
		setVisible(true);
	}

	/**
	 * Write show next time
	 */
	public void writeShowNextTime() {
		if (getStore().get(PREF_HELPTIPSHOW, true) != showCbx.isSelected()) {
			getStore().put(PREF_HELPTIPSHOW, showCbx.isSelected());
			getStore().save();
		}
	}

	/**
	 * Close the dialog
	 */
	public void close() {
		writeShowNextTime();
		setVisible(false);
	}

	/**
	 * Get the persistence store
	 * @return  the persistence
	 */
	public XmlObjectStore getStore() {
		return store;
	}

	/**
	 * Go to the next tip.
	 */
	private void previous() {
		idx--;
		showTip();
	}

	/**
	 * Go to the next tip.
	 */
	private void next() {
		idx++;
		showTip();
	}

	/**
	 * Go to the next tip.
	 */
	private void random() {
		Random rand = new Random();
		idx = rand.nextInt(helpTips.size());
		showTip();
	}

	/**
	 * Handle a change to a link
	 *
	 * @param e  the link's event
	 */
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			if (e.getURL() == null) {
				click(e.getDescription());
			} else {
				click(e.getURL().toString());
			}
		}
	}

	/**
	 * Handle a click on a link
	 *
	 * @param url  the link definition
	 */
	public void click(String url) {
		actionListener.actionPerformed(new ActionEvent(this, 0, url));
	}

	/**
	 * Get the title for this tip
	 *
	 * @param helpTip  the tip node
	 * @return  the title
	 */
	private String getTitle(Node helpTip) {
		String title = XmlUtil.getAttribute(helpTip, "title", (String) null);
		if (title == null) {
			title = "";
		}
		return "<html><h2>" + title + "</h2></html>";
	}

	/**
	 * Get the message for this tip
	 *
	 * @param helpTip  the tip node
	 * @return  the message
	 */
	private String getMessage(Node helpTip) {
		String message = XmlUtil.getAttribute(helpTip, "message", (String) null);
		if (message == null) {
			message = XmlUtil.getChildText(helpTip);
		}
		return message;
	}

	/**
	 * Show the current tip.
	 */
	private void showTip() {

		// Show the first tip if we have no tip history
		if (getStore().get(PREF_HELPTIPIDX, -1) < 0) idx=0;

		if (helpTips.size() == 0) {
			return;
		}
		if (idx >= helpTips.size()) {
			idx = 0;
			getStore().put(PREF_HELPTIPIDX, idx);
		} else if (idx < 0) {
			idx = helpTips.size() - 1;
		}
		Node   helpTip = (Node) helpTips.get(idx);

		counterText.setText((int)(idx+1) + "/" + helpTips.size());
		titleText.setText(getTitle(helpTip));
		messageText.setText(getMessage(helpTip));

		getStore().put(PREF_HELPTIPIDX, idx);
		getStore().save();
	}

}
