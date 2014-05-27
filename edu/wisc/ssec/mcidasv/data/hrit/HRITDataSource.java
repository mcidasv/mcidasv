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

package edu.wisc.ssec.mcidasv.data.hrit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import edu.wisc.ssec.mcidas.Calibrator;

import visad.Data;
import visad.VisADException;
import visad.data.hrit.HRITAdapter;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.util.Misc;
import ucar.unidata.util.WrapperException;

public class HRITDataSource extends DataSourceImpl  {

    /** List of sources files */
    protected List sources;

    public static String request;

    /** List of sources files */
    protected List adapters;
    
    private List categories;

    /** for unpersistence */
    protected String oldSourceFromBundles;
    
    private static final String DATA_DESCRIPTION = "HRIT Data";
    
    private static int counter = 1;

    /** children choices */
    private List myDataChoices = new ArrayList();

    /**
     * Default constructor
     */
    public HRITDataSource() {}

    /**
     * Construct a new HRIT data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  fileName  name of the HRIT segment file to read
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public HRITDataSource(DataSourceDescriptor descriptor,
                                 String fileName, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.newList(fileName), properties);
    }
    
    /**
     * Construct a new HRIT data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  newSources  List of filenames
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public HRITDataSource(DataSourceDescriptor descriptor,
                                 List newSources, Hashtable properties)
            throws VisADException {
    	
        this(descriptor, newSources, DATA_DESCRIPTION, properties);
        boolean looksOk = false;
        String dataCategoryStr = "HRIT Data";
        if ((newSources != null) && (newSources.size() >= 1)) {
        	String fileNameFullPath = (String) newSources.get(0);
        	if ((fileNameFullPath != null) && (fileNameFullPath.length() >= 58)) {
        		if ((fileNameFullPath.contains("MSG2")) && (fileNameFullPath.endsWith("-__"))) {
        			String channelStr = fileNameFullPath.substring(fileNameFullPath.lastIndexOf("MSG2") + 13, fileNameFullPath.lastIndexOf("MSG2") + 19);
        			String timeStr = fileNameFullPath.substring(fileNameFullPath.lastIndexOf("MSG2") + 33, fileNameFullPath.lastIndexOf("MSG2") + 45);
        			dataCategoryStr = "MSG2 " + channelStr + " " + timeStr;
        			looksOk = true;
        		}
        	}
        }
        if (looksOk) {
        	DataCategory.createCategory(dataCategoryStr);
        	categories = DataCategory.parseCategories(dataCategoryStr + ";IMAGE");
        } else {
        	throw new VisADException("Not a decompressed MSG HRIT file");
        }
    }    

	/**
     * Create a HRITDataSource
     *
     * @param descriptor The datasource descriptor
     * @param newSources List of files or urls
     * @param description The long name
     * @param properties properties
     *
     * @throws VisADException  couldn't create the data
     */
    public HRITDataSource(DataSourceDescriptor descriptor, List newSources,
                           String description, Hashtable properties) 
            throws VisADException {

        super(descriptor, "HRIT" + counter, "HRIT" + counter, properties);
        counter++;
        sources = newSources;
    }


    /**
     * Can this data source save its data to local disk
     *
     * @return can save to local disk
     */
    public boolean canSaveDataToLocalDisk() {
        return !isFileBased() && (getProperty(PROP_SERVICE_HTTP) != null);
    }


    /**
     * Are we getting data from a file or from server
     * 
     * @return is the data from files
     */
    protected boolean isFileBased() {
        if (sources.size() == 0) {
            return false;
        }
        return (new File(sources.get(0).toString())).exists();
    }

    /**
     * This is called when the CacheManager detects the need ot clear memory.
     * It is intended to be overwritten by derived classes that are holding cached
     * data that is not in the normal putCache facilities provided by this class
     * since that data is actually managed by the CacheManager
     */
    public void clearCachedData() {
        super.clearCachedData();
    }
    
    /**
     * Make and insert the <code>DataChoice</code>-s for this
     * <code>DataSource</code>.
     */
    public void doMakeDataChoices() {
    	DataChoice choice = null;
    	
    	for (int i = 0; i < sources.size(); i++) {
    		String fileNameFullPath = (String) sources.get(i);
        	if (fileNameFullPath.contains("MSG2")) {
        		String channelStr = fileNameFullPath.substring(fileNameFullPath.lastIndexOf("MSG2") + 13, fileNameFullPath.lastIndexOf("MSG2") + 19);
        		String timeStr = fileNameFullPath.substring(fileNameFullPath.lastIndexOf("MSG2") + 33, fileNameFullPath.lastIndexOf("MSG2") + 45);
        		String segStr = fileNameFullPath.substring(fileNameFullPath.lastIndexOf("MSG2") + 27, fileNameFullPath.lastIndexOf("MSG2") + 29);
        		try {
        			choice = doMakeDataChoice(0, "MSG2 " + channelStr + " " + timeStr + " SEGMENT " + segStr);
        		} 
        		catch (Exception e) {
        			e.printStackTrace();
        			System.out.println("doMakeDataChoice failed");
        		}

        		if (choice != null) {
        			addDataChoice(choice);
        		}
        	}
    	}

    }
    
    private DataChoice doMakeDataChoice(int idx, String var) throws Exception {
        String name = var;
        Hashtable ht = null;
        DirectDataChoice ddc = new DirectDataChoice(this, idx, name, name, categories, ht);
        return ddc;
    }
    
    /**
     * Create, if needed, and return the list of adapters.
     * Will return null if there are no valid adapters.
     *
     * @return List of adapters or null
     */
    protected List getAdapters() {
        if ((adapters == null) || (adapters.size() == 0)) {
            try {
                makeAdapters(sources);
            } catch (Exception exc) {
                setInError(true);
                throw new WrapperException(exc);
            }
        }
        if (adapters.size() == 0) {
            adapters = null;
        }
        return adapters;
    }

    /**
     * Make the adapters for the given list of files
     *
     * @param files Data files
     *
     * @throws Exception When bad things happen
     */
    private void makeAdapters(List files) throws Exception {
        adapters = new ArrayList();
    }


    /**
     * Create the list of times associated with this DataSource.
     * @return list of times.
     */
    protected List doMakeDateTimes() {
        List    times      = new ArrayList();
        return times;
    }

    /**
     * Get the data for the given DataChoice and selection criteria.
     * @param dataChoice         DataChoice for selection
     * @param category           DataCategory for the DataChoice (not used)
     * @param dataparams         Resolution criteria.
     * @param requestProperties  extra request properties
     * @return  the Data object for the request
     *
     * @throws RemoteException couldn't create a remote data object
     * @throws VisADException  couldn't create the data
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataparams,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {

        // for now, hardcoded array of band center wave numbers, such that'
    	// the array index is the band number
    	String[] bandCWN = { 
    			"N/A", "006", "008", "016", "039", "062", "073",
    			"087", "097", "108", "120", "134", "___"
    	};
        
    	// XXX TJJ need to determine this from data type and wavelength
    	int bandNum = 1;
    	// default to BRIT calibration, will check if user picked something else
    	int calType = Calibrator.CAL_BRIT;
        
    	String newRes = (String) dataparams.getProperty("magnification");
        int magFactor = 1;
        if (newRes != null) {
	        try {
	        	magFactor = Integer.parseInt(newRes);
	        } catch (NumberFormatException nfe) {
	        	nfe.printStackTrace();
	        }
        }

        // pull out source index 
        String idxStr = dataChoice.getName().substring(dataChoice.getName().length() - 2, dataChoice.getName().length());
        
        Data data = null;
        
        String [] files = new String[1];
        // initialize central wave number string
        String cwnStr = "006";
        for (int i = 0; i < sources.size(); i++) {
        	String tmpStr = (String) sources.get(i);
        	cwnStr = tmpStr.substring(tmpStr.lastIndexOf("MSG2") + 16, tmpStr.lastIndexOf("MSG2") + 19);
        	String segStr = tmpStr.substring(tmpStr.lastIndexOf("MSG2") + 27, tmpStr.lastIndexOf("MSG2") + 29);
        	if (segStr.equals(idxStr)) {
        		files[0] = (String) sources.get(i);
        	}
        }
        
        // match up central wave number with band number index
        for (int i = 0; i < bandCWN.length; i++) {
        	if (bandCWN[i].equals(cwnStr)) {
        		bandNum = i;
        		break;
        	}
        }
        
        String newCal = (String) dataparams.getProperty("calibration");
        // do checks to only allow valid calibrations here
        if (newCal != null) {
        	if ((bandNum >= 4) && (bandNum <= 11)) {
        		if (newCal.equals("RAD")) {
        			calType = Calibrator.CAL_RAD;
        		}
        		if (newCal.equals("TEMP")) {
        			calType = Calibrator.CAL_TEMP;
        		}
        		if (newCal.equals("BRIT")) {
        			calType = Calibrator.CAL_BRIT;
        		}
        	} else {
        		if (newCal.equals("RAD")) {
        			calType = Calibrator.CAL_RAD;
        		}
        		if (newCal.equals("ALB")) {
        			calType = Calibrator.CAL_ALB;
        		}
        		if (newCal.equals("BRIT")) {
        			calType = Calibrator.CAL_BRIT;
        		}        		
        	}
        }

    	HRITAdapter ha;
		try {
			ha = new HRITAdapter(files, magFactor, calType, bandNum);
			data = ha.getData();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return data;
    }

    protected void initDataSelectionComponents(
    		List<DataSelectionComponent> components,
    		final DataChoice dataChoice) {

    	try {
    		components.add(new ResolutionSelection(dataChoice));
    	} 
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    }


    class ResolutionSelection extends DataSelectionComponent {

    	DataChoice dataChoice;
    	JPanel display;
    	JComboBox jcbMag = null;
    	JComboBox jcbCal = null;

    	ResolutionSelection(DataChoice dataChoice) throws Exception {
    		super("Magnification and Calibration");
    		this.dataChoice = dataChoice;
    		List names = dataChoice.getCurrentNames();
    		display = new JPanel(new FlowLayout());
    		String[] resStrings = { "1", "2", "4", "8", "16" };
    		jcbMag = new JComboBox(resStrings);
    		display.add(jcbMag);
    		String[] irCalStrings  = { "BRIT", "RAD", "RAW", "TEMP" };
    		String[] visCalStrings = { "BRIT", "RAD", "RAW", "ALB" };
    		// XXX TJJ - we need a standard mechanism to make this determination
    		// this is a temporary cheap hack: grab the last file name added and 
    		// do a hardcoded string match.
    		String sampleFileName = names.get(names.size() - 1).toString();
    		// those below are considered "visible" bands, yes even IR_016!
    		if ((sampleFileName.contains("VIS")) ||
    			(sampleFileName.contains("HRV")) ||
    			(sampleFileName.contains("IR_016"))
    				) {
    			jcbCal = new JComboBox(visCalStrings);
    		} else {
    			jcbCal = new JComboBox(irCalStrings);
    		}
    		display.add(jcbCal);
    	}

    	protected JComponent doMakeContents() {
    		try {
    			JPanel panel = new JPanel(new BorderLayout());
    			panel.add("Center", display);
    			return panel;
    		}
    		catch (Exception e) {
    			System.out.println(e);
    		}
    		return null;
    	}

    	public void applyToDataSelection(DataSelection dataSelection) {
    		try {
    			dataSelection.putProperty("magnification", jcbMag.getSelectedItem());
    			dataSelection.putProperty("calibration", jcbCal.getSelectedItem());
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    }
}
