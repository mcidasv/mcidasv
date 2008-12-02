/*
 * $Id$
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.wisc.ssec.mcidasv.chooser;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;




import ucar.unidata.idv.*;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.ui.DataSelector;
import ucar.unidata.ui.DatasetUI;
import ucar.unidata.ui.XmlTree;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.CatalogUtil;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.PreferenceList;

import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;




import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;



/**
 * This handles a variety of flavors of xml documents (e.g., thredds
 * query capability, thredds catalogs, idv menus) to create data
 * choosers from. It provides a combobox to enter urls to xml
 * documents. It retrieves the xml and creates a {@link XmlHandler}
 * based on the type of xml. Currently this class handles two
 * types of xml: Thredds catalog and Web Map Server (WMS)
 * capability documents. The XmlHandler does most of the work.
 * <p>
 * This class maintains the different xml docs the user has gone
 * to coupled with the XmlHandler for each doc. It uses this list
 * to support navigating back and forth through the history of
 * documents.
 *
 * @author IDV development team
 * @version $Revision$Date: 2007/07/09 22:59:58 $
 */


public class XmlChooser extends ucar.unidata.idv.chooser.XmlChooser implements Constants {

    /**
     * Create the <code>XmlChooser</code>
     *
     * @param mgr The <code>IdvChooserManager</code>
     * @param root  The xml root that defines this chooser
     *
     */
    public XmlChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        
        loadButton = McVGuiUtils.makeImageTextButton(ICON_ACCEPT_SMALL, getLoadCommandName());
        loadButton.setActionCommand(getLoadCommandName());
        loadButton.addActionListener(this);

    }

    /**
     *  Create and return the Gui contents.
     *
     *  @return The gui contents.
     */
    protected JComponent doMakeContents() {
       	Element chooserNode = getXmlNode();
    	JComponent parentContents = super.doMakeContents();
        return parentContents;
    }

}

