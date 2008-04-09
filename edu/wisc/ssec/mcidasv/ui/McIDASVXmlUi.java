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

package edu.wisc.ssec.mcidasv.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.IdvXmlUi;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.ui.HtmlComponent;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;

@SuppressWarnings("unchecked")
public class McIDASVXmlUi extends IdvXmlUi {

	/** Avoid unneeded getIdv() calls. */
	private IntegratedDataViewer idv;
	
	private IdvWindow window;
	
	public McIDASVXmlUi(IdvWindow window, List viewManagers,
			IntegratedDataViewer idv, Element root) {
		super(window, viewManagers, idv, root);
		this.idv = idv;
		this.window = window;
	}

	/** 
	 * Convert the &amp;gt; and &amp;lt; entities to &gt; and &lt;.
	 * 
	 * @param text The text you'd like to convert.
	 * 
	 * @return The converted text!
	 */
	private static String decodeHtml(String text) {
		String html = text.replace("&gt;", ">");
		html = html.replace("&lt;", "<");
		return html;
	}

	// overridden so that we can use McVComponentGroup rather than IDVCompGroup.
	// also so we can use McVCompHolder rather than the IdvCompHolder.
	@Override
	protected IdvComponentGroup makeComponentGroup(Element node) {
		McIDASVComponentGroup group = new McIDASVComponentGroup(idv, "", window);
		group.initWith(node);

		NodeList elements = XmlUtil.getElements(node);
		for (int i = 0; i < elements.getLength(); i++) {
			Element child = (Element)elements.item(i);

			String tag = child.getTagName();

			if (tag.equals(IdvUIManager.COMP_MAPVIEW)
					|| tag.equals(IdvUIManager.COMP_VIEW)) {
				ViewManager viewManager = getViewManager(child);
				group.addComponent(new McIDASVComponentHolder(idv, viewManager));
			}
			else if (tag.equals(IdvUIManager.COMP_COMPONENT_CHOOSERS)) {
				IdvComponentHolder comp = new McIDASVComponentHolder(idv,"choosers");
				comp.setType(IdvComponentHolder.TYPE_CHOOSERS);
				comp.setName(XmlUtil.getAttribute(child,"name","Choosers"));
				group.addComponent(comp);
			}
			else if (tag.equals(IdvUIManager.COMP_COMPONENT_SKIN)) {
				IdvComponentHolder comp = new McIDASVComponentHolder(idv, XmlUtil.getAttribute(child, "url"));
				comp.setType(IdvComponentHolder.TYPE_SKIN);
				comp.setName(XmlUtil.getAttribute(child, "name", "UI"));
				group.addComponent(comp);
			}
			else if (tag.equals(IdvUIManager.COMP_COMPONENT_HTML)) {
				String text = XmlUtil.getChildText(child);
				text = new String(XmlUtil.decodeBase64(text.trim()));
				ComponentHolder comp = new HtmlComponent("Html Text", text);
				comp.setShowHeader(false);
				comp.setName(XmlUtil.getAttribute(child,"name","HTML"));
				group.addComponent(comp);
				
			}
			else if (tag.equals(IdvUIManager.COMP_DATASELECTOR)) {
				group.addComponent(new McIDASVComponentHolder(idv,
						idv.getIdvUIManager().createDataSelector(false,
								false)));
			} 
			else if (tag.equals(IdvUIManager.COMP_COMPONENT_GROUP)) {
				group.addComponent(makeComponentGroup(child));
			}
			else {
				System.err.println("Unknown component element:" + XmlUtil.toString(child));
			}
		}
		return group;
	}

	// overridden so we can do some HTML tricks 
	@Override
	public Component createComponent(Element node, String id) {
		Component comp = null;
		String tagName = node.getTagName();
		if (tagName.equals(TAG_HTML)) {
			String text = getAttr(node, ATTR_TEXT, NULLSTRING);
			text = decodeHtml(text);
			if (text == null) {
				String url = getAttr(node, ATTR_URL, NULLSTRING);
				if (url != null) {
					text = IOUtil.readContents(url, (String) null);
				}
				if (text == null) {
					text = XmlUtil.getChildText(node);
				}
			}
			HyperlinkListener linkListener = new HyperlinkListener() {
				public void hyperlinkUpdate(HyperlinkEvent e) {
					String url;
					if (e.getURL() == null) {
						url = e.getDescription();
					} else {
						url = e.getURL().toString();
					}
					actionPerformed(new ActionEvent(this, 0, url));
				}
			};
			Component[] comps = GuiUtils.getHtmlComponent(text, linkListener,
									getAttr(node, ATTR_WIDTH, 200),
									getAttr(node, ATTR_HEIGHT, 200));
			comp = comps[1];
		} else {
			comp = super.createComponent(node, id);
		}

		return comp;
	}
}
