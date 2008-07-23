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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Keeper of info relevant to a single entry in RESOLV.SRV
 */
public class AddeEntry {
	private String addeGroup = "";
	private String addeDescriptor = "";
	private String addeRt = "N";
	private String addeType = "";
	private String addeServer = "";
	private String addeStart = "1";
	private String addeEnd = "99999";
	private String addeFileMask = "";
	
	private String[] addeTypes = { "IMAGE" };
	private String[] addeServers = { "AREA", "GVAR", "NCDF", "MSGT", "MD", "GEOT" };
	
	private String cygwinPrefix = "/cygdrive/";
	private int cygwinPrefixLength = cygwinPrefix.length();
	
	/**
	 * Empty constructor
	 */
	public AddeEntry() {
		addeGroup = "";
		addeDescriptor = "";
		addeRt = "N";
		addeType = addeTypes[0];
		addeServer = addeServers[0];
		addeStart = "1";
		addeEnd = "99999";
		addeFileMask = "";
	}
	
	/**
	 * Initialize all the relevant fields
	 * 
	 * @param inGroup
	 * @param inDescriptor
	 * @param inType
	 * @param inServer
	 * @param inFileMask
	 */
	public AddeEntry(String inGroup, String inDescriptor, String inType, String inServer, String inFileMask) {
		addeGroup = inGroup;
		addeDescriptor = inDescriptor;
		addeRt = "N";
		addeType = inType;
		addeServer = inServer;
		addeStart = "1";
		addeEnd = "99999";
		addeFileMask = inFileMask;
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
	    	else if (varval[0].equals("K")) addeServer = varval[1];
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
	    }
	}
	
	/**
	 * Return a JPanel with column headings
	 */
	public JPanel doMakePanelLabel() {
		JPanel labelPanel = new JPanel();
		
		JLabel labelGroup = new JLabel("Group");
		labelGroup.setSize(100, 16);
		JLabel labelDescriptor = new JLabel("Descriptor");
		labelDescriptor.setSize(100, 16);
		JLabel labelType = new JLabel("Type");
		labelType.setSize(75, 16);
		JLabel labelServer = new JLabel("Server");
		labelServer.setSize(75, 16);
		JLabel labelFileMask = new JLabel("File mask");
		
		labelPanel.add(labelGroup);
		labelPanel.add(labelDescriptor);
		labelPanel.add(labelType);
		labelPanel.add(labelServer);
		labelPanel.add(labelFileMask);
				
		return labelPanel;
		
	}
	
	/**
	 * Return a JPanel with editing elements
	 */
	public JPanel doMakePanel() {
		JPanel entryPanel = new JPanel();
		
		final JTextField inputGroup = new JTextField(addeGroup, 8);
		inputGroup.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){}
			public void focusLost(FocusEvent e){
				addeGroup = inputGroup.getText();
			}
		});
		
		final JTextField inputDescriptor = new JTextField(addeDescriptor, 8);
		inputDescriptor.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){}
			public void focusLost(FocusEvent e){
				addeDescriptor = inputDescriptor.getText();
			}
		});
		
		final JComboBox inputType = new JComboBox(addeTypes);
		inputType.setSelectedItem(addeType);
	    inputType.addItemListener(new ItemListener(){
	        public void itemStateChanged(ItemEvent e){
	        	addeType = (String)inputType.getSelectedItem();
	        }
	    });

		final JComboBox inputServer = new JComboBox(addeServers);
		inputServer.setSelectedItem(addeServer);
	    inputServer.addItemListener(new ItemListener(){
	        public void itemStateChanged(ItemEvent e){
	        	addeServer = (String)inputServer.getSelectedItem();
	        }
	    });
	    
		final JLabel inputFileMask = new JLabel(addeFileMask);
		
		final JButton inputFileButton = new JButton("File mask:");
		inputFileButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				addeFileMask = getDataDirectory(addeFileMask);
				inputFileMask.setText(addeFileMask);
			}
		});

		entryPanel.add(inputGroup);
		entryPanel.add(inputDescriptor);
		entryPanel.add(inputType);
		entryPanel.add(inputServer);
		entryPanel.add(inputFileButton);
		entryPanel.add(inputFileMask);
		
		return entryPanel;
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
		if (addeGroup.equals("") ||	addeDescriptor.equals(""))
			return(null);
		String entry = "N1=" + addeGroup.toUpperCase() + ",";
		entry += "N2=" + addeDescriptor.toUpperCase() + ",";
		entry += "TYPE=" + addeType.toUpperCase() + ",";
		entry += "RT=" + addeRt.toUpperCase() + ",";
		entry += "K=" + addeServer.toUpperCase() + ",";
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
		return(entry);
	}
	
	/**
	 * Return just the group
	 */
	public String getGroup() {
		return this.addeGroup;
	}
	
}
