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

package edu.wisc.ssec.mcidasv.addemanager;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Calendar;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.text.*;

import ucar.unidata.idv.chooser.adde.AddeServer.Group;
import ucar.unidata.util.GuiUtils;

/**
 * Keeper of info relevant to a single entry in RESOLV.SRV
 */
public class AddeEntry {
	private String addeGroup;
	private String addeDescriptor;
	private String addeRt;
	private String addeType;
	private String addeFormat;
	private String addeDescription;
	private String addeStart;
	private String addeEnd;
	private String addeFileMask;
	
	/**
	 * The full list of possible ADDE servers
	 */
	/*
	private String[][] addeFormats = {
		{ "AREA", "McIDAS AREA", "McIDAS AREA" },
		{ "FSDX", "EUMETCast LRIT", "EUMETCast LRIT" },
		{ "MODS", "MODIS L1b MOD02", "MODIS Level 1b" },
		{ "MODX", "MODIS L2 MOD06", "MODIS Level 2 (Cloud top properties)" },
		{ "MODX", "MODIS L2 MOD07", "MODIS Level 2 (Atmospheric profile)" },
		{ "MODX", "MODIS L2 MOD35", "MODIS Level 2 (Cloud mask)" },
		{ "MOD4", "MODIS L2 MOD04", "MODIS Level 2 (Aerosol)" },
		{ "MOD8", "MODIS L2 MOD28", "MODIS Level 2 (Sea surface temperature)" },
		{ "MODR", "MODIS L2 MODR", "MODIS Level 2 (Corrected reflectance)" },
		{ "AIRS", "AIRS L1b", "AIRS Level 1b" },
		{ "MSGT", "MSG HRIT", "MSG HRIT" },
		{ "MTST", "MTSAT HRIT", "MTSAT HRIT" },
		{ "LV1B", "NOAA AVHRR L1b", "NOAA AVHRR Level 1b" },
		{ "LV1B", "Metop AVHRR L1b", "Metop AVHRR Level 1b" },
		{ "GINI", "AWIPS GINI", "AWIPS GINI" },
		{ "AWIP", "AWIPS netCDF", "AWIPS netCDF" },
		{ "NEXR", "NEXRAD Radar", "NEXRAD Level 3 Radar" },
		{ "OMTP", "Meteosat OpenMTP", "Meteosat OpenMTP" },
		{ "SMIN", "SSMI", "Terrascan netCDF" },
		{ "TMIN", "TRMM", "Terrascan netCDF" },
		{ "AMSR", "AMSR-E L1b", "AMSR-E Level 1b" },
		{ "AMRR", "AMSR-E Rain Product", "AMSR-E Rain Product" }
	};
	*/
	
	private String[][] addeFormats = {
			{ "AREA", "McIDAS AREA", "McIDAS AREA" },
			{ "FSDX", "EUMETCast LRIT", "EUMETCast LRIT" },
			{ "MODS", "MODIS L1b MOD02", "MODIS Level 1b" },
			{ "MODX", "MODIS L2 MOD35", "MODIS Level 2 (Cloud mask)" },
			{ "MOD4", "MODIS L2 MOD04", "MODIS Level 2 (Aerosol)" },
			{ "MODR", "MODIS L2 MODR", "MODIS Level 2 (Corrected reflectance)" },
			{ "MSGT", "MSG HRIT", "MSG HRIT" },
			{ "LV1B", "NOAA AVHRR L1b", "NOAA AVHRR Level 1b" },
			{ "LV1B", "Metop AVHRR L1b", "Metop AVHRR Level 1b" },
			{ "OMTP", "Meteosat OpenMTP", "Meteosat OpenMTP" },
			{ "SMIN", "SSMI", "Terrascan netCDF" },
			{ "TMIN", "TRMM", "Terrascan netCDF" },
			{ "AMSR", "AMSR-E L1b", "AMSR-E Level 1b" },
			{ "AMRR", "AMSR-E Rain Product", "AMSR-E Rain Product" }
		};
	
	private String cygwinPrefix = "/cygdrive/";
	private int cygwinPrefixLength = cygwinPrefix.length();
	
	/**
	 * Empty constructor
	 */
	public AddeEntry() {
		addeGroup = "";
		addeDescriptor = "";
		addeRt = "N";
		addeType = "IMAGE";
		addeFormat = addeFormats[0][0];
		addeDescription = addeFormats[0][1];
		addeStart = "1";
		addeEnd = "99999";
		addeFileMask = "";
	}
	
	/**
	 * Initialize with user editable info
	 */
	public AddeEntry(String group, String descriptor, String description, String mask) {
		this();
		addeGroup = group;
		addeDescriptor = descriptor;
		addeFormat = getServerNameFromDescription(description);
		addeDescription = description;
		addeFileMask = mask;
	}
	
	/**
	 * Initialize with a line from RESOLV.SRV
	 * 
	 * @param resolvLine
	 */
	public AddeEntry(String resolvLine) {
		String[] assignments = resolvLine.trim().split(",");
		String[] varval;
	    for (int i = 0 ; i < assignments.length ; i++) {
	    	if (assignments[i] == null || assignments[i].equals("")) continue;
	    	varval = assignments[i].split("=");
	    	if (varval.length != 2 ||
	    			varval[0].equals("") || varval[1].equals("")) continue;
	    	if (varval[0].equals("N1")) addeGroup = varval[1];
	    	else if (varval[0].equals("N2")) addeDescriptor = varval[1];
	    	else if (varval[0].equals("TYPE")) addeType = varval[1];
	    	else if (varval[0].equals("K")) addeFormat = varval[1];
	    	else if (varval[0].equals("MASK")) {
	    		String tmpFileMask = varval[1];
	    		tmpFileMask = tmpFileMask.replace("/*", "");
	    		/** Look for "cygwinPrefix" at start of string and munge accordingly */
	    		if (tmpFileMask.length() > cygwinPrefixLength+1 &&
	    				tmpFileMask.substring(0,cygwinPrefixLength).equals(cygwinPrefix)) {
	    			String driveLetter = tmpFileMask.substring(cygwinPrefixLength,cygwinPrefixLength+1).toUpperCase();
	    			tmpFileMask = driveLetter + ":" + tmpFileMask.substring(cygwinPrefixLength+1).replace('/', '\\');
	    		}
	    		addeFileMask = tmpFileMask;
	    	}
	    	else if (varval[0].equals("MCV")) addeDescription = varval[1];
	    }
	}
	
	/**
	 * Return descriptions from addeFormats
	 * @return
	 */
	private String[] getFormatDescriptions() {
		String[] descriptions = new String[addeFormats.length];
		int i;
		for (i=0; i<addeFormats.length; i++) {
			descriptions[i] = addeFormats[i][1];
		}
		return descriptions;
	}
	
	/**
	 * Return tooltip from addeFormats (by index)
	 */
	private String getTooltip(int index) {
		return addeFormats[index][2];
	}
	
	/**
	 * Return server name from a given description
	 */
	private String getServerNameFromDescription(String description) {
		String servername = "ERR_";
		int i;
		for (i=0; i<addeFormats.length; i++) {
			if (addeFormats[i][1].equals(description)) return addeFormats[i][0];
		}
		return servername;
	}
	
	/**
	 * Return a JPanel with editing elements
	 */
	public JPanel doMakePanel() {
		JPanel entryPanel = new JPanel();
		
		final JTextField inputGroup = new JTextField(8);
        inputGroup.setDocument(new JTextFieldLimit(8));
        inputGroup.setText(addeGroup);
		inputGroup.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){}
			public void focusLost(FocusEvent e){
				addeGroup = inputGroup.getText();
			}
		});
		
		final JTextField inputDescriptor = new JTextField(12);
        inputDescriptor.setDocument(new JTextFieldLimit(12));
        inputDescriptor.setText(addeDescriptor);
		inputDescriptor.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){}
			public void focusLost(FocusEvent e){
				addeDescriptor = inputDescriptor.getText();
			}
		});
		
		final JComboBox inputFormat = new JComboBox(getFormatDescriptions());
	    inputFormat.setRenderer(new TooltipComboBoxRenderer());
		inputFormat.setSelectedItem(addeDescription);
		inputFormat.addItemListener(new ItemListener(){
	        public void itemStateChanged(ItemEvent e){
	        	addeDescription = (String)inputFormat.getSelectedItem();
	        	addeFormat = getServerNameFromDescription(addeDescription);
	        }
	    });
	    
		String buttonLabel = addeFileMask.equals("") ? "<SELECT>" : getShortString(addeFileMask);
		final JButton inputFileButton = new JButton(buttonLabel);
		inputFileButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				addeFileMask = getDataDirectory(addeFileMask);
				inputFileButton.setText(getShortString(addeFileMask));
			}
		});
		
        entryPanel = GuiUtils.doLayout(new Component[] {
                GuiUtils.rLabel("Dataset (Group): "), GuiUtils.left(inputGroup),
                GuiUtils.rLabel("Image Type (Descriptor): "), GuiUtils.left(inputDescriptor),
                GuiUtils.rLabel("Format: "), GuiUtils.left(inputFormat),
                GuiUtils.rLabel("Directory: "), GuiUtils.left(inputFileButton)
            }, 2, GuiUtils.WT_N, GuiUtils.WT_NNNY);
		
		return entryPanel;
	}
	
	/**
	 * Get a short directory name representation, suitable for a button label
	 */
	private String getShortString(String longString) {
		String shortString = longString;
		if (longString.length() > 19) {
			shortString = longString.subSequence(0, 16) + "...";
		}
		return shortString;
	}

	/**
	 * Ask the user for a data directory from which to create a MASK=
	 */
	private String getDataDirectory(String startDir) {
        JFileChooser fileChooser = new JFileChooser(startDir);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int status = fileChooser.showOpenDialog(null);
        if (status == JFileChooser.APPROVE_OPTION) {
        	File file = fileChooser.getSelectedFile();
        	return file.getAbsolutePath();
        }
        return(startDir);
	}
	
	/**
	 * Return a valid RESOLV.SRV line
	 */
	public String getResolvEntry() {
		String entry = "N1=" + addeGroup.toUpperCase() + ",";
		entry += "N2=" + addeDescriptor.toUpperCase() + ",";
		entry += "TYPE=" + addeType.toUpperCase() + ",";
		entry += "RT=" + addeRt.toUpperCase() + ",";
		entry += "K=" + addeFormat.toUpperCase() + ",";
		entry += "R1=" + addeStart.toUpperCase() + ",";
		entry += "R2=" + addeEnd.toUpperCase() + ",";
		/** Look for "C:" at start of string and munge accordingly */
		if (addeFileMask.length() > 3 && addeFileMask.substring(1,2).equals(":")) {
			String newFileMask = addeFileMask;
			String driveLetter = newFileMask.substring(0,1).toLowerCase();
			newFileMask = newFileMask.substring(3);
			newFileMask = newFileMask.replace('\\', '/');
			entry += "MASK=" + cygwinPrefix + driveLetter + "/" + newFileMask + "/*,";
		}
		else {
			entry += "MASK=" + addeFileMask + "/*,";
		}
		if (addeFormat.toUpperCase().equals("LV1B"))
			entry += "Q=LALO,";
		entry += "MCV=" + addeDescription + ",";
		return(entry);
	}
	
	/**
	 * Return just the group
	 */
	public String getGroup() {
		return addeGroup;
	}
	
	/**
	 * Return just the descriptor
	 */
	public String getDescriptor() {
		return addeDescriptor;
	}
	
	/**
	 * Return just the format
	 */
	public String getFormat() {
		return addeFormat;
	}
	
	/**
	 * Return just the description
	 */
	public String getDescription() {
		return addeDescription;
	}
	
	/**
	 * Return just the mask
	 */
	public String getMask() {
		return addeFileMask;
	}
	
	/**
	 * Set just the file mask
	 */
	public void setMask(String mask) {
		addeFileMask = mask;
	}
	
	/**
	 * See if this is a valid entry
	 */
	public boolean isValid() {
		if (addeGroup.equals("") || addeDescriptor.equals("")) return false;
		else return true;
	}
	
	class TooltipComboBoxRenderer extends BasicComboBoxRenderer {
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
				if (-1 < index) {
					list.setToolTipText(getTooltip(index));
				}
			}
			else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			setFont(list.getFont());
			setText((value == null) ? "" : value.toString());
			return this;
		}
	}
	
	public class JTextFieldLimit extends PlainDocument {
	    private int limit;
	    // optional uppercase conversion
	    private boolean toUppercase = true;
	    
	    JTextFieldLimit(int limit) {
	        super();
	        this.limit = limit;
	    }
	    
	    JTextFieldLimit(int limit, boolean upper) {
	        super();
	        this.limit = limit;
	        toUppercase = upper;
	    }
	    
	    public void insertString
	            (int offset, String str, AttributeSet attr)
	            throws BadLocationException {
	        if (str == null ||
	        		str.indexOf('/')>=0 ||
	        		str.indexOf('.')>=0 ||
	        		str.indexOf(' ')>=0 ||
	        		str.indexOf('[')>=0 ||
	        		str.indexOf(']')>=0 ||
	        		str.indexOf('%')>=0
	        	) return;
	        
	        if ((getLength() + str.length()) <= limit) {
	            if (toUppercase) str = str.toUpperCase();
	            super.insertString(offset, str, attr);
	        }
	    }
	}
	
}
