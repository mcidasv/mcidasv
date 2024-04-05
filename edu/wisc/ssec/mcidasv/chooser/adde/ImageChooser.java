/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package edu.wisc.ssec.mcidasv.chooser.adde;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.w3c.dom.Element;

import ucar.unidata.data.DataSelection;
import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.BandInfo;
import ucar.unidata.data.imagery.ImageDataset;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlObjectStore;

import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

/**
 * Chooser that allows users to select images from a remote ADDE server.
 *
 * <p>Displays a list of the descriptors (names) of the image datasets
 * available for a particular ADDE group on the remote server.</p>
 */
public class ImageChooser extends AddeImageChooser implements Constants {

    private static final long serialVersionUID = 1L;

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
    public final static String NAVIGATION_KEY = "navigation";

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

    private JCheckBox previewBox = null;


    /**
     * Construct an ADDE image selection widget.
     *
     * @param mgr Chooser manager.
     * @param root Chooser XML node.
     */
    public ImageChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        //DAVEP: Hiding parameter set picker for now... revisit after 1.0
//        showParameterButton();
    }
    
    /**
     * Return the parameter type associated with this chooser.
     */
    @Override protected String getParameterSetType() {
        return "addeimagery";
    }
    
    /**
     * Return the data source ID. Used by extending classes.
     */
    @Override protected String getDataSourceId() {
        return "ADDE.IMAGE";
    }

    /**
     * Restore the selected parameter set using element attributes.
     *
     * @param restoreElement {@code Element} with the desired attributes.
     * {@code null} values are permitted.
     *
     * @return {@code true} if the parameter set was restored, {@code false}
     * otherwise.
     */
    @Override protected boolean restoreParameterSet(Element restoreElement) {
        boolean okay = super.restoreParameterSet(restoreElement);
        if (!okay) {
            return okay;
        }
        
        // Imagery specific restore
        
        // Restore nav
        if (restoreElement.hasAttribute(ATTR_NAV)) {
            String nav = restoreElement.getAttribute(ATTR_NAV);
            TwoFacedObject tfo = new TwoFacedObject("Default", "X");
            navComboBox.setSelectedItem(tfo);
            if (nav.toUpperCase().equals("LALO")) {
                tfo = new TwoFacedObject("Lat/Lon", "LALO");
            }
            navComboBox.setSelectedItem(tfo);
        }
        return true;
    }
    
    /**
     * Get the list of BandInfos for the current selected images.
     *
     * @return List of BandInfos.
     */
    public List<BandInfo> getSelectedBandInfos() {
        return super.getBandInfos();
    }

    /**
     * Get the value for the given property. This can either be the value
     * supplied by the end user through the advanced GUI or is the default.
     *
     * @param prop The property.
     * @param ad The AreaDirectory.
     *
     * @return Value of the property to use in the request string.
     */
    @Override protected String getPropValue(String prop, AreaDirectory ad) {
        String propValue = super.getPropValue(prop, ad);
        if (PROP_NAV.equals(prop)) {
            propValue = TwoFacedObject.getIdString(navComboBox.getSelectedItem());
        }
        return propValue;
    }

    /**
     * Get the default value for a key.
     *
     * @param property Property (key type).
     * @param dflt Default value.
     *
     * @return Value for key or {@code dflt} if not found.
     */
    @Override protected String getDefault(String property, String dflt) {
        String paramDefault = super.getDefault(property, dflt);
        if (PROP_NAV.equals(property)) {
            if (restoreElement != null) {
                paramDefault = restoreElement.getAttribute(ATTR_NAV);
            }
        } else if (PROP_UNIT.equals(property)) {
            paramDefault = "";
        } else if (PROP_BAND.equals(property)) {
            paramDefault = ALL;
        } else if (PROP_PLACE.equals(property)) {
            paramDefault = "";
        }
        return paramDefault;
    }
    
    /**
     * Get the DataSource properties.
     * 
     * @param ht Hashtable of properties.
     */
    @Override protected void getDataSourceProperties(Hashtable ht) {
        super.getDataSourceProperties(ht);
        if (restoreElement != null) {
            if (restoreElement.hasAttribute(ATTR_BAND)) {
                ht.put(BAND_KEY, restoreElement.getAttribute(ATTR_BAND));
            }
            if (restoreElement.hasAttribute(ATTR_LATLON)) {
                ht.put(LATLON_KEY, restoreElement.getAttribute(ATTR_LATLON));
            }
            if (restoreElement.hasAttribute(ATTR_LINELE)) {
                ht.put(LINELE_KEY, restoreElement.getAttribute(ATTR_LINELE));
            }
            if (restoreElement.hasAttribute(ATTR_MAG)) {
                ht.put(MAG_KEY, restoreElement.getAttribute(ATTR_MAG));
            }
            if (restoreElement.hasAttribute(ATTR_PLACE)) {
                ht.put(PLACE_KEY, restoreElement.getAttribute(ATTR_PLACE));
            }
            if (restoreElement.hasAttribute(ATTR_SIZE)) {
                ht.put(SIZE_KEY, restoreElement.getAttribute(ATTR_SIZE));
            }
            if (restoreElement.hasAttribute(ATTR_UNIT)) {
                ht.put(UNIT_KEY, restoreElement.getAttribute(ATTR_UNIT));
            }
        } else {
            ht.put(NAVIGATION_KEY, getPropValue(PROP_NAV, null));
        }
        ht.put(PREVIEW_KEY, previewBox.isSelected());
    }
    
    /**
     * Should we use the user supplied property.
     * 
     * @param propId The property.
     * 
     * @return Should use the value from the advanced widget.
     */
    protected boolean usePropFromUser(String propId) {
        boolean fromSuper = super.usePropFromUser(propId);
        if (PROP_UNIT.equals(propId) || PROP_BAND.equals(propId)) {
            fromSuper = false;
        }
        return fromSuper;
    }
    
    /**
     * Make the UI for this selector.
     *
     * @return The GUI.
     */
    @Override public JComponent doMakeContents() {
        JPanel myPanel = new JPanel();

        JLabel timesLabel = McVGuiUtils.makeLabelRight("Times:");
        addDescComp(timesLabel);

        JPanel timesPanel = makeTimesPanel();
        timesPanel.setBorder(BorderFactory.createEtchedBorder());
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
        XmlObjectStore store = getIdv().getStore();
        previewBox = new JCheckBox("Create preview image", store.get(Constants.PREF_IMAGE_PREVIEW, true));
        previewBox.setToolTipText("Creating preview images takes extra time and network bandwidth");
        previewBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                XmlObjectStore store = getIdv().getStore();
                store.put(Constants.PREF_IMAGE_PREVIEW, previewBox.isSelected());
                store.save();
            }
        });
        addDescComp(previewBox);

        GroupLayout layout = new GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(descriptorLabel)
                            .addGap(GAP_RELATED)
                            .addComponent(descriptorComboBox))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(timesLabel)
                            .addGap(GAP_RELATED)
                            .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(navigationLabel)
                            .addGap(GAP_RELATED)
                            .addComponent(navComboBox))
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(previewLabel)
                            .addGap(GAP_RELATED)
                            .addComponent(previewBox))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(BASELINE)
                        .addComponent(descriptorLabel)
                        .addComponent(descriptorComboBox))
                    .addPreferredGap(RELATED)
                    .addGroup(layout.createParallelGroup(LEADING)
                        .addComponent(timesLabel)
                        .addComponent(timesPanel, PREFERRED_SIZE, DEFAULT_SIZE, Short.MAX_VALUE))
                    .addPreferredGap(RELATED)
                    .addGroup(layout.createParallelGroup(LEADING)
                        .addComponent(navigationLabel)
                        .addComponent(navComboBox))
                    .addGroup(layout.createParallelGroup(LEADING)
                        .addComponent(previewLabel)
                        .addComponent(previewBox)))
        );
        setInnerPanel(myPanel);
        return super.doMakeContents(true);
    }

    /**
     * User said go, we go. Simply get the list of images from the Image Chooser
     * and create the {@code ADDE.IMAGE} data source.
     */
    @Override public void doLoadInThread() {
        if (!checkForValidValues()) {
            return;
        }
        if (!getGoodToGo()) {
            updateStatus();
            return;
        }

        List imageList = getImageList();
        if ((imageList == null) || imageList.isEmpty()) {
            return;
        }

        // Check for size threshold
        final int[] dim = { 0, 0 };
        AddeImageDescriptor aid = (AddeImageDescriptor) imageList.get(0);
        dim[0] = aid.getImageInfo().getElements();
        dim[1] = aid.getImageInfo().getLines();
        // System.err.println("dim:" + dim[0] + " x " + dim[1] + " # images:"
        // + imageList.size());
        int numPixels = dim[0] * dim[1] * imageList.size();
        double megs = (4 * numPixels) / (double) 1000000;

        //DAVEP: take this out--it should be warning in the data source, not the chooser
        boolean doSizeCheck = false;
        if ((megs > AddeImageChooser.SIZE_THRESHOLD) && doSizeCheck) {
            final JCheckBox maintainSize = new JCheckBox(
                "Maintain spatial extent", false);
            final JLabel sizeLbl = new JLabel(StringUtil.padRight("  "
                + ((double) ((int) megs * 100)) / 100.0 + " MB", 14));
            GuiUtils.setFixedWidthFont(sizeLbl);
            final List[] listHolder = { imageList };
            final JSlider slider = new JSlider(2, (int) megs, (int) megs);
            slider.setMajorTickSpacing((int) (megs - 2) / 10);
            slider.setMinorTickSpacing((int) (megs - 2) / 10);
            // slider.setPaintTicks(true);
            slider.setSnapToTicks(true);
            final long timeNow = System.currentTimeMillis();
            ChangeListener sizeListener = new javax.swing.event.ChangeListener() {
                public void stateChanged(ChangeEvent evt) {
                    // A hack so we don't respond to the first event that we get
                    // from the slider when
                    // the dialog is first shown
                    if ((System.currentTimeMillis() - timeNow) < 500) {
                        return;
                    }
                    JSlider slider = (JSlider) evt.getSource();
                    int pixelsPerImage = (1000000 * slider.getValue()) / listHolder[0].size() / 4;
                    double aspect = dim[1] / (double) dim[0];
                    int nx = (int) Math.sqrt(pixelsPerImage / aspect);
                    int ny = (int) (aspect * nx);
                    if (maintainSize.isSelected()) {
                        // doesn't work
                        lineMagSlider.setValue(getLineMagValue() - 1);
                        lineMagSliderChanged(true);
                    } else {
                        numElementsFld.setText("" + nx);
                        numLinesFld.setText("" + ny);
                    }
                    listHolder[0] = getImageList();
                    AddeImageDescriptor aid = (AddeImageDescriptor) listHolder[0]
                        .get(0);
                    dim[0] = aid.getImageInfo().getElements();
                    dim[1] = aid.getImageInfo().getLines();
                    int numPixels = dim[0] * dim[1] * listHolder[0].size();
                    double nmegs = (4 * numPixels) / (double) 1000000;
                    sizeLbl.setText(StringUtil.padRight("  "
                            + ((double) ((int) nmegs * 100)) / 100.0 + " MB",
                        14));
                }
            };
            slider.addChangeListener(sizeListener);

            JComponent msgContents = GuiUtils
                .vbox(
                    new JLabel(
                        "<html>You are about to load "
                            + megs
                            + " MB of imagery.<br>Are you sure you want to do this?<p><hr><p></html>"),
                    GuiUtils.inset(GuiUtils.leftCenterRight(new JLabel(
                            "Change Size: "),
                        GuiUtils.inset(slider, 5), sizeLbl), 5));

            if (!GuiUtils.askOkCancel("Image Size", msgContents)) {
                return;
            }
            imageList = listHolder[0];
        }

        ImageDataset ids = new ImageDataset(getDatasetName(), imageList);
        // make properties Hashtable to hand the station name
        // to the AddeImageDataSource
        Hashtable ht = new Hashtable();
        ht.put(DataSelection.PROP_CHOOSERTIMEMATCHING, getDoTimeDrivers());
        getDataSourceProperties(ht);
        Object bandName = getSelectedBandName();
        if ((bandName != null) && !bandName.equals(ALLBANDS.toString())) {
            ht.put(DATA_NAME_KEY, bandName);
        }
        ht.put("allBands", bandDirs);
        makeDataSource(ids, getDataSourceId(), ht);
        saveServerState();
        // uncheck the check box every time click the add source button
        drivercbx.setSelected(false);
        enableTimeWidgets();
        setDoTimeDrivers(false);
    }

}
