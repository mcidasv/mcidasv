/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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

package edu.wisc.ssec.mcidasv.chooser.adde;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.w3c.dom.Element;

import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.AddeImageInfo;
import ucar.unidata.data.imagery.BandInfo;
import ucar.unidata.data.imagery.ImageDataSource;
import ucar.unidata.data.imagery.ImageDataset;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.ui.ChooserList;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlNodeList;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;
import ucar.visad.UtcDate;
import visad.DateTime;
import visad.Gridded1DSet;
import visad.VisADException;
import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaDirectoryList;
import edu.wisc.ssec.mcidas.McIDASException;
import edu.wisc.ssec.mcidas.adde.AddeSatBands;
import edu.wisc.ssec.mcidas.adde.AddeURL;
import edu.wisc.ssec.mcidas.adde.DataSetInfo;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.servermanager.EntryStore;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;


/**
 * Widget to select images from a remote ADDE server
 * Displays a list of the descriptors (names) of the image datasets
 * available for a particular ADDE group on the remote server.
 *
 * @author Don Murray
 */
public class AddeImageParameterChooser extends AddeImageChooser implements Constants {

    /**
     * Public keys for server, group, dataset, user, project.
     */
    public final static String SIZE_KEY = "size";
    public final static String BAND_KEY = "band";
    public final static String PLACE_KEY = "place";
    public final static String LATLON_KEY = "latlon";
    public final static String LINELE_KEY = "linele";
    public final static String MAG_KEY = "mag";
    public final static String UNIT_KEY = "unit";
    public final static String PREVIEW_KEY = "preview";
    
    /** Property for image default value unit */
    protected static final String PROP_NAV = "NAV";

    /** Property for image default value unit */
    protected static final String PROP_UNIT = "UNIT";

    /** Property for image default value band */
    protected static final String PROP_BAND = "BAND";

    /** Xml attr name for the defaults */
    private static final String ATTR_NAV = "NAV";
    private static final String ATTR_UNIT = "UNIT";
    private static final String ATTR_BAND = "BAND";
    private static final String ATTR_PLACE = "PLACE";
    private static final String ATTR_SIZE = "SIZE";
    private static final String ATTR_MAG = "MAG";
    private static final String ATTR_LATLON = "LATLON";
    private static final String ATTR_LINELE = "LINELE";

    /** string for ALL */
    private static final String ALL = "ALL";

    private static JCheckBox previewBox = null;
    private boolean showPreviewDefault = false;

    /**
     * Construct an Adde image selection widget
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeImageParameterChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        //DAVEP: Hiding parameter set picker for now... revisit after 1.0
//        showParameterButton();
    }
    
    /**
     * Return the parameter type associated with this chooser.  Override!
     */
    @Override
    protected String getParameterSetType() {
    	return "addeimagery";
    }
    
	/**
	 * Return the data source ID.  Used by extending classes.
	 */
    @Override
	protected String getDataSourceId() {
		return "ADDE.IMAGE.V";
	}
    
    /**
     * Restore the selected parameter set using element attributes
     * 
     * @param restoreElement
     * @return
     */
    @Override
    protected boolean restoreParameterSet(Element restoreElement) {
    	boolean okay = super.restoreParameterSet(restoreElement);
    	if (!okay) return okay;
    	
        // Imagery specific restore
    	
    	// Restore nav
        if (restoreElement.hasAttribute(ATTR_NAV)) {
        	String nav = restoreElement.getAttribute(ATTR_NAV);
            TwoFacedObject tfo = new TwoFacedObject("Default", "X");
            navComboBox.setSelectedItem((Object)tfo);
            if (nav.toUpperCase().equals("LALO")) {
                tfo = new TwoFacedObject("Lat/Lon", "LALO");
            }
            navComboBox.setSelectedItem((Object)tfo);
        }
    	return true;
    }
    
    /**
     * Get the list of BandInfos for the current selected images
     * @return list of BandInfos
     */
    public List<BandInfo> getSelectedBandInfos() {
        return super.getBandInfos();
    }

    /**
     * Get the value for the given property. This can either be the value
     * supplied by the end user through the advanced GUI or is the default
     *
     * @param prop The property
     * @param ad The AreaDirectory
     *
     * @return The value of the property to use in the request string
     */
    @Override
    protected String getPropValue(String prop, AreaDirectory ad) {
    	String propValue = super.getPropValue(prop, ad);
        if (prop.equals(PROP_NAV)) {
        	propValue = TwoFacedObject.getIdString(navComboBox.getSelectedItem());
		}
        return propValue;
    }
    
    /**
     * Optionally override any defaults per parameter chooser
     * @param property
     * @return
     */
    @Override
	protected String getDefault(String property, String dflt) {
    	String paramDefault = super.getDefault(property, dflt);
    	if (property.equals(PROP_NAV)) {
    		if (restoreElement != null) {
    			paramDefault = restoreElement.getAttribute(ATTR_NAV);
    		}
    	} else if (property.equals(PROP_UNIT)) {
    		paramDefault = "";
		} else if (property.equals(PROP_BAND)) {
    		paramDefault = ALL;
		} else if (property.equals(PROP_PLACE)) {
			paramDefault = "";
		}
    	return paramDefault;
    }
    
	/**
	 * Get the DataSource properties
	 * 
	 * @param ht
	 *            Hashtable of properties
	 */
    @Override
	protected void getDataSourceProperties(Hashtable ht) {
		super.getDataSourceProperties(ht);
        if (restoreElement != null) {
            if (restoreElement.hasAttribute(ATTR_BAND)) {
                ht.put(BAND_KEY, (Object)(restoreElement.getAttribute(ATTR_BAND)));
            }
            if (restoreElement.hasAttribute(ATTR_LATLON)) {
                ht.put(LATLON_KEY, (Object)(restoreElement.getAttribute(ATTR_LATLON)));
            }
            if (restoreElement.hasAttribute(ATTR_LINELE)) {
                ht.put(LINELE_KEY, (Object)(restoreElement.getAttribute(ATTR_LINELE)));
            }
            if (restoreElement.hasAttribute(ATTR_MAG)) {
                ht.put(MAG_KEY, (Object)(restoreElement.getAttribute(ATTR_MAG)));
            }
            if (restoreElement.hasAttribute(ATTR_PLACE)) {
                ht.put(PLACE_KEY, (Object)(restoreElement.getAttribute(ATTR_PLACE)));
            }
            if (restoreElement.hasAttribute(ATTR_SIZE)) {
                ht.put(SIZE_KEY, (Object)(restoreElement.getAttribute(ATTR_SIZE)));
            }
            if (restoreElement.hasAttribute(ATTR_UNIT)) {
                ht.put(UNIT_KEY, (Object)(restoreElement.getAttribute(ATTR_UNIT)));
            }
        }
        
        ht.put(PREVIEW_KEY, (Object)previewBox.isSelected());
	}
    
	/**
	 * Should we use the user supplied property
	 * 
	 * @param propId
	 *            The property
	 * 
	 * @return Should use the value from the advanced widget
	 */
	protected boolean usePropFromUser(String propId) {
		boolean fromSuper = super.usePropFromUser(propId);
		if (propId.equals(PROP_UNIT)) fromSuper = false;
		else if (propId.equals(PROP_BAND)) fromSuper = false;
		return fromSuper;
	}
	
    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    @Override
    public JComponent doMakeContents() {
        JPanel myPanel = new JPanel();

        JLabel timesLabel = McVGuiUtils.makeLabelRight("Times:");
        addDescComp(timesLabel);

        JPanel timesPanel = makeTimesPanel();
        timesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        addDescComp(timesPanel);

        JLabel navigationLabel = McVGuiUtils.makeLabelRight("Navigation:");
        addDescComp(navigationLabel);

        // Use processPropertyComponents to build combo boxes that we rely on
        processPropertyComponents();
        addDescComp(navComboBox);
        McVGuiUtils.setComponentWidth(navComboBox, McVGuiUtils.Width.DOUBLE);

		// Preview checkbox
		JLabel previewLabel = McVGuiUtils.makeLabelRight("Preview:");
		addDescComp(previewLabel);
        previewBox = new JCheckBox("Create preview image", showPreviewDefault);
        addDescComp(previewBox);

		org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(myPanel);
		myPanel.setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
				.add(layout.createSequentialGroup()
						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
								.add(layout.createSequentialGroup()
										.add(descriptorLabel)
										.add(GAP_RELATED)
										.add(descriptorComboBox))
										.add(layout.createSequentialGroup()
												.add(timesLabel)
												.add(GAP_RELATED)
												.add(timesPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
												.add(layout.createSequentialGroup()
														.add(navigationLabel)
														.add(GAP_RELATED)
														.add(navComboBox))
														.add(layout.createSequentialGroup()
																.add(previewLabel)
																.add(GAP_RELATED)
																.add(previewBox))))
		);
		layout.setVerticalGroup(
				layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
				.add(layout.createSequentialGroup()
						.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
								.add(descriptorLabel)
								.add(descriptorComboBox))
								.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
								.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
										.add(timesLabel)
										.add(timesPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
										.addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
										.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
												.add(navigationLabel)
												.add(navComboBox))
												.add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
														.add(previewLabel)
														.add(previewBox)))
		);
		
        setInnerPanel(myPanel);
        return super.doMakeContents(true);
    }

}
