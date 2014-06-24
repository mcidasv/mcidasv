/*
 * Copyright 1997-2014 Unidata Program Center/University Corporation for
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

package ucar.unidata.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ucar.unidata.gis.maps.LatLonLabelData;
import ucar.unidata.idv.control.MapDisplayControl;
import ucar.unidata.ui.drawing.Glyph;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.TwoFacedObject;

/**
 * A panel to hold the gui for Lat/Lon label adjustments
 */

public class LatLonLabelPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** flag for ignoring events */
    private boolean ignoreEvents = false;

    /** This holds the data that describes the latlon lines */
    private LatLonLabelData latLonLabelData;

    /** The visibility cbx */
    JCheckBox onOffCbx;

    /** The spacing input box */
    JTextField spacingField;

    /** The base input box */
    JTextField baseField;

    /** The spacing input box */
    JTextField labelLinesField;

    /** Shows the color */
    //JButton colorButton;
    GuiUtils.ColorSwatch colorButton;

    /** The line style box */
    JCheckBox fastRenderCbx;

    /** the font selector */
    FontSelector fontSelector;

    /** the alignment selector */
    JComboBox alignSelector;

    /** the alignment selector */
    JComboBox formatSelector;

    /** the alignment point list */
    private List<TwoFacedObject> alignPoints;

    /** the use360 checkbox */
    private JCheckBox use360Cbx;

    /** list of predefined formats */
    private static final String[] LABEL_FORMATS = {
        "DD", "DD.d", "DD:MM", "DD:MM:SS", "DDH", "DD.dH", "DD:MMH",
        "DD:MM:SSH"
    };
    
    /** Longitude offset - toggles to 180 when 0-360 checkbox is clicked */
    public static int LON_OFFSET = 0;
    
    /** String to use in error messages for valid lon range */
    public static String LON_RANGE = "(-180 to 180)";

    /**
     * Create a LatLonLabelPanel
     *
     * @param lld Holds the lat lon data
     *
     */
    public LatLonLabelPanel(LatLonLabelData lld) {

        this.latLonLabelData = lld;
        ignoreEvents         = true;
        onOffCbx             = new JCheckBox("",
                                             latLonLabelData.getVisible());
        onOffCbx.setToolTipText("Turn on/off labels");
        onOffCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !ignoreEvents) {
                    latLonLabelData.setVisible(onOffCbx.isSelected());
                }
            }
        });

        spacingField =
            new JTextField(String.valueOf(latLonLabelData.getInterval()), 6);
        spacingField.setToolTipText(
            "Set the interval (degrees) between labels");
        spacingField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                validateTextFields();
                latLonLabelData.setInterval(
                    new Float(spacingField.getText()).floatValue());
                // this ensures the formatting is consistent for lat/lon text fields
                spacingField.setText("" + latLonLabelData.getInterval());
            }
        });

        baseField =
            new JTextField(String.valueOf(latLonLabelData.getBaseValue()), 6);
        baseField.setToolTipText("Set the base value for the interval");
        baseField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                validateTextFields();
                if (latLonLabelData.getIsLatitude()) {
                	latLonLabelData.setBaseValue(
                			new Float(baseField.getText()).floatValue());
                } else {
                	latLonLabelData.setBaseValue(
                			new Float(baseField.getText()).floatValue());
                }
                // this ensures the formatting is consistent for lat/lon text fields
                baseField.setText("" + latLonLabelData.getBaseValue());
            }
        });

        labelLinesField = new JTextField(
            LatLonLabelData.formatLabelLines(
                latLonLabelData.getLabelLines()), 6);
        labelLinesField.setToolTipText(
            "Set the lines to place labels separated by a semicolon (;)");
        labelLinesField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	//System.err.println("labelLinesField event...");
                if (ignoreEvents) {
                	//System.err.println("ignoreEvents true...");
                    return;
                }
                //System.err.println("validating Text Fields...");
                validateTextFields();
                //System.err.println("calling setLabelsLineString...");
                latLonLabelData.setLabelsLineString(labelLinesField.getText());
            }
        });

        colorButton = new GuiUtils.ColorSwatch(latLonLabelData.getColor(),
                "Set " + (latLonLabelData.getIsLatitude()
                          ? "Latitude"
                          : "Longitude") + " Color");
        colorButton.setToolTipText("Set the label color");
        colorButton.addPropertyChangeListener("background",
                new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (ignoreEvents) {
                    return;
                }
                Color c = ((JPanel) evt.getSource()).getBackground();

                if (c != null) {
                    latLonLabelData.setColor(c);
                }
            }
        });

        fastRenderCbx = new JCheckBox("", latLonLabelData.getFastRendering());
        fastRenderCbx.setToolTipText("Set if labels don't render correctly");
        fastRenderCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !ignoreEvents) {
                    latLonLabelData.setFastRendering(
                        fastRenderCbx.isSelected());
                }
            }
        });

        fontSelector = new FontSelector(FontSelector.COMBOBOX_UI, false,
                                        false);
        fontSelector.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if ( !ignoreEvents) {
                    latLonLabelData.setFont(fontSelector.getFont());
                }
            }
        });
        fontSelector.setFont((Font) latLonLabelData.getFont());

        alignPoints = TwoFacedObject.createList(Glyph.RECTPOINTS,
                Glyph.RECTPOINTNAMES);
        alignSelector = new JComboBox();
        alignSelector.setToolTipText(
            "Set the positioning of the label relative to the location");
        alignSelector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if ( !ignoreEvents) {
                    latLonLabelData.setAlignment(
                        TwoFacedObject.getIdString(
                            alignSelector.getSelectedItem()));
                }
            }
        });
        GuiUtils.setListData(alignSelector, alignPoints);
        alignSelector.setSelectedItem(
            getAlignSelectorItem(latLonLabelData.getAlignment()));

        formatSelector = new JComboBox(LABEL_FORMATS);
        formatSelector.setEditable(true);
        formatSelector.setToolTipText("Set the label format");
        formatSelector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if ( !ignoreEvents) {
                    latLonLabelData.setLabelFormat(
                        formatSelector.getSelectedItem().toString());
                }
            }
        });

        formatSelector.setSelectedItem(latLonLabelData.getLabelFormat());
        ignoreEvents = false;

        use360Cbx    = new JCheckBox("0-360", latLonLabelData.getUse360());
        use360Cbx.setToolTipText(
            "Use 0 to 360 vs. -180 to 180 convention for longitude labels");
        use360Cbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	//System.err.println("checkbox event, checked? : " + use360Cbx.isSelected()); 
                if ( !ignoreEvents) {
                	boolean checked = use360Cbx.isSelected();
                    latLonLabelData.setUse360(checked);
                    // we also need a ref to Label Latitude panel
                    LatLonLabelPanel lllpLat = ((MapDisplayControl.LatLonLabelState) latLonLabelData).getMapDisplayControl().getLatLabelPanel();
                    LatLonLabelPanel lllpLon = ((MapDisplayControl.LatLonLabelState) latLonLabelData).getMapDisplayControl().getLonLabelPanel();
                    LatLonPanel llp = ((MapDisplayControl.LatLonLabelState) latLonLabelData).getMapDisplayControl().getLonPanel();
                    if (checked) {
                    	LON_OFFSET = 180;
                    	LON_RANGE = "(0 to 360)";

                    	// if latitude (MARKED AT LONGITUDES), update the placement points too
                    	LatLonLabelData llldLat = lllpLat.getLatLonLabelData();
                    	float [] val = llldLat.getLabelLines();
                    	for (int i = 0; i < val.length; i++) {
                    		if (val[i] < 0) {
                    			val[i] = val[i] + 360;
                    		}
                    	}
                    	lllpLat.labelLinesField.setText("" + LatLonLabelData.formatLabelLines(val));

                    	// also adjust lat/lon relative text fields
                    	if (! latLonLabelData.getIsLatitude()) {
                    		Float f = new Float(lllpLon.baseField.getText()).floatValue();
                    		if (f < 0) {
                    			lllpLon.baseField.setText("" + (f + 360));
                    		}
                    	}
                    	Float f = new Float(llp.baseField.getText()).floatValue();
                    	if (f < 0) {
                    		llp.baseField.setText("" + (f + 360));
                    	}
                    } else {
                    	LON_OFFSET = 0;
                    	LON_RANGE = "(-180 to 180)";

                    	LatLonLabelData llldLat = lllpLat.getLatLonLabelData();
                    	float [] val = llldLat.getLabelLines();
                    	for (int i = 0; i < val.length; i++) {
                    		if (val[i] > 180) {
                    			val[i] = val[i] - 360;
                    		}
                    		//System.err.println("Updating label lines: " + val[i]);
                    	}
                    	lllpLat.labelLinesField.setText("" + LatLonLabelData.formatLabelLines(val));

                    	// also adjust lat/lon relative text fields
                    	if (! latLonLabelData.getIsLatitude()) {
                    		Float f = new Float(lllpLon.baseField.getText()).floatValue();
                    		if (f > 180) {
                    			lllpLon.baseField.setText("" + (f - 360));
                    		}
                    	}
                    	Float f = new Float(llp.baseField.getText()).floatValue();
                    	if (f > 180) {
                    		llp.baseField.setText("" + (f - 360));
                    	}
                    }
                    applyStateToData();
                    llp.applyStateToData();
                }
            }
        });
    }

    /**
     * Set the information that configures this.
     *
     * @param lld   the latlon data
     */
    public void setLatLonLabelData(LatLonLabelData lld) {
        this.latLonLabelData = lld;
        if (onOffCbx != null) {
            ignoreEvents = true;
            onOffCbx.setSelected(lld.getVisible());
            spacingField.setText("" + lld.getInterval());
            //System.err.println("Yeah, went thru here...");
            baseField.setText("" + lld.getBaseValue());
            labelLinesField.setText(
                "" + LatLonLabelData.formatLabelLines(lld.getLabelLines()));
            colorButton.setBackground(lld.getColor());
            fastRenderCbx.setSelected(lld.getFastRendering());
            alignSelector.setSelectedItem(
                getAlignSelectorItem(lld.getAlignment()));
            if (lld.getFont() != null) {
                fontSelector.setFont((Font) lld.getFont());
            }
            formatSelector.setSelectedItem(lld.getLabelFormat());
            use360Cbx.setSelected(lld.getUse360());
            ignoreEvents = false;
        }

    }


    /**
     * Layout the panels
     *
     * @param latPanel  the lat panel
     * @param lonPanel  the lon panel
     *
     * @return The laid back panels
     */
    public static JPanel layoutPanels(LatLonLabelPanel latPanel,
                                      LatLonLabelPanel lonPanel) {
        Component[] comps = {
            GuiUtils.lLabel("<html><b>Labels</b></html>"), GuiUtils.filler(),
            GuiUtils.cLabel("Interval"), GuiUtils.cLabel("Relative to"),
            GuiUtils.filler(), GuiUtils.filler(), GuiUtils.cLabel("Color"),
            GuiUtils.cLabel("Alignment"), latPanel.onOffCbx,
            GuiUtils.rLabel("Latitude:"), latPanel.spacingField,
            latPanel.baseField, GuiUtils.rLabel("At Longitudes:"),
            latPanel.labelLinesField, latPanel.colorButton,
            latPanel.alignSelector, lonPanel.onOffCbx,
            GuiUtils.rLabel("Longitude:"), lonPanel.spacingField,
            lonPanel.baseField, GuiUtils.rLabel("At Latitudes:"),
            lonPanel.labelLinesField, lonPanel.colorButton,
            lonPanel.alignSelector,
        };
        GuiUtils.tmpInsets = new Insets(2, 4, 2, 4);
        JPanel settings = GuiUtils.doLayout(comps, 8, GuiUtils.WT_N,
                                            GuiUtils.WT_N);
        Component[] extraComps = {
            GuiUtils.rLabel("Font:"), latPanel.fontSelector.getComponent(),
            GuiUtils.rLabel("Format:"), latPanel.formatSelector,
            lonPanel.use360Cbx, GuiUtils.filler()
        };
        GuiUtils.tmpInsets = new Insets(2, 4, 2, 4);
        JPanel extra = GuiUtils.doLayout(extraComps, 5, GuiUtils.WT_N,
                                         GuiUtils.WT_N);
        return GuiUtils.vbox(GuiUtils.left(settings), GuiUtils.left(extra));
    }

    /**
     * Apply any of the state in the gui (e.g., spacing) to the  latLonData
     */
    public void applyStateToData() {
        // need to get the TextField values because people could type in a new value
        // without hitting return.  Other widgets should trigger a change
        latLonLabelData.setInterval(
            new Float(spacingField.getText()).floatValue());
        latLonLabelData.setBaseValue(
            new Float(baseField.getText()).floatValue());
        latLonLabelData.setLabelsLineString(labelLinesField.getText());
    }

	/**
	 * Make sure all the text fields have valid data before applying changes
	 * @throws HeadlessException
	 */
    
	private void validateTextFields() throws HeadlessException {

        // TJJ May 2014, validate input, bad data was causing NPEs
        //System.err.println("Is this Lat?: " + latLonLabelData.getIsLatitude());
        boolean isLat = latLonLabelData.getIsLatitude();
        boolean llLinked = ((MapDisplayControl.LatLonLabelState) latLonLabelData).getMapDisplayControl().getApplyChangesToAllLabels();
        //System.err.println("Lat/Lon panels are linked: " + llLinked);
        
        // First check the interval fields
		float curVal = latLonLabelData.getInterval();
        try {
        	float val = Float.parseFloat(spacingField.getText());
        	if (val <= 0.0f) {
        		JOptionPane.showMessageDialog(null, 
        				"Interval must be greater than zero",
        				"Invalid Latitude or Longitude Interval",
        				JOptionPane.ERROR_MESSAGE);
        		// put text field back to old value
        		spacingField.setText("" + curVal);
        		return;
        	} else {
        		if (isLat) {
        			if (val > 180) {
        				JOptionPane.showMessageDialog(null, 
        						"Value exceeds valid bounds",
        						"Invalid Lat/Lon interval",
        						JOptionPane.ERROR_MESSAGE);
        				// put text field back to old value
        				spacingField.setText("" + curVal);
        				return;
        			}
        		} else {
        			if (llLinked) {
        				if (val > 180) {
        					JOptionPane.showMessageDialog(null, 
        							"Unlink to set Longitude interval > 180",
        							"Invalid Longitude interval while linked",
        							JOptionPane.ERROR_MESSAGE);
        					// put text field back to old value
        					spacingField.setText("" + curVal);
        					return;
        				}
        			} else {
        				if (val > 360) {
        					JOptionPane.showMessageDialog(null, 
        							"Value exceeds valid bounds",
        							"Invalid Lat/Lon interval",
        							JOptionPane.ERROR_MESSAGE);
        					// put text field back to old value
        					spacingField.setText("" + curVal);
        					return;
        				}
        			}
        		}
        	}
        } catch (NumberFormatException nfe) {
        	//System.err.println("NFE: " + spacingField.getText());
        	// Also an error, different message
    		JOptionPane.showMessageDialog(null, 
    				"Value entered is not a number",
    				"Invalid Latitude or Longitude Interval",
    				JOptionPane.ERROR_MESSAGE);
    		// put text field back to old value
    		spacingField.setText("" + curVal);
        }
        
        // Now the base (offset) fields
		float curBase = latLonLabelData.getBaseValue();
        try {
        	float val = Float.parseFloat(baseField.getText());
        	if (latLonLabelData.getIsLatitude()) {
	        	if ((val < -90.0f) || (val > 90.0f)) {
	        		JOptionPane.showMessageDialog(null, 
	        				"Value must be a valid Latitude (-90 to 90)",
	        				"Invalid Relative Latitude",
	        				JOptionPane.ERROR_MESSAGE);
	        		// put text field back to old value
	        		baseField.setText("" + curBase);
	        	}
        	} else {
        		// valid range changes depending on convention checkbox
	        	if ((val < (-180.0f + LON_OFFSET)) || (val > (180.0f + LON_OFFSET))) {
	        		JOptionPane.showMessageDialog(null, 
	        				"Value must be a valid Longitude " + LON_RANGE,
	        				"Invalid Relative Longitude",
	        				JOptionPane.ERROR_MESSAGE);
	        		// put text field back to old value
	        		baseField.setText("" + curBase);
	        	}
        	}
        } catch (NumberFormatException nfe) {
        	//System.err.println("NFE: " + baseField.getText());
        	// Also an error, different message
    		JOptionPane.showMessageDialog(null, 
    				"Value entered is not a number",
    				"Invalid Relative Latitude or Longitude",
    				JOptionPane.ERROR_MESSAGE);
    		// put text field back to old value
    		baseField.setText("" + curBase);
        }
        
        // Finally the label placement fields
		float [] curPlacement = latLonLabelData.getLabelLines();
        try {
        	float [] val = LatLonLabelData.parseLabelLineString(labelLinesField.getText());
        	if (! latLonLabelData.getIsLatitude()) {
        		for (int idx = 0; idx < val.length; idx++) {
        			if ((val[idx] < -90.0f) || (val[idx] > 90.0f)) {
        				JOptionPane.showMessageDialog(null, 
        						"String must be valid Latitudes (-90 to 90) separated by semicolons",
        						"Invalid Label Placement Values",
        						JOptionPane.ERROR_MESSAGE);
        				// put text field back to old value
        				labelLinesField.setText("" + LatLonLabelData.formatLabelLines(curPlacement));
        			}
        		}
        	} else {
        		for (int idx = 0; idx < val.length; idx++) {
        			if ((val[idx] < (-180.0f + LON_OFFSET)) || (val[idx] > (180.0f + LON_OFFSET))) {
        				JOptionPane.showMessageDialog(null, 
        						"String must be valid Longitudes " + LON_RANGE + " separated by semicolons",
        						"Invalid Label Placement Values",
        						JOptionPane.ERROR_MESSAGE);
        				// put text field back to old value
        				labelLinesField.setText("" + LatLonLabelData.formatLabelLines(curPlacement));
        			}
        		}
        	}
        } catch (NumberFormatException nfe) {
        	//System.err.println("NFE: " + labelLinesField.getText());
        	// Also an error, different message
    		JOptionPane.showMessageDialog(null, 
    				"String must be valid Lats/Lons separated by semicolons",
    				"Invalid Label Placement Values",
    				JOptionPane.ERROR_MESSAGE);
    		// put text field back to old value
    		labelLinesField.setText("" + LatLonLabelData.formatLabelLines(curPlacement));
        }
        
	}

    /**
     * Get the latlondata object
     *
     * @return The latlondata object
     */
    public LatLonLabelData getLatLonLabelData() {
        return latLonLabelData;
    }

    /**
     * Get the TwoFacedObject associated with the alignment id
     *
     * @param id  the id
     *
     * @return  the corresponding TFO or null
     */
    private TwoFacedObject getAlignSelectorItem(String id) {
        if (alignPoints == null) {
            return null;
        }
        return TwoFacedObject.findId(id, alignPoints);
    }

}
