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

package edu.wisc.ssec.mcidasv.data.hrit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import java.io.File;
import java.io.IOException;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;

import ucar.unidata.util.Misc;
import ucar.unidata.util.WrapperException;

import visad.Data;
import visad.FlatField;
import visad.data.hrit.HRITAdapter;
import visad.VisADException;

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
    
    /** My composite */
    private CompositeDataChoice myCompositeDataChoice;

    /** children choices */
    private List myDataChoices = new ArrayList();

    /**
     * Default constructor
     */
    public HRITDataSource() {}

    /**
     * Construct a new HRIT data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  fileName  name of the hdf file to read
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
     * @param  sources   List of filenames
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public HRITDataSource(DataSourceDescriptor descriptor,
                                 List newSources, Hashtable properties)
            throws VisADException {
    	
        this(descriptor, newSources, DATA_DESCRIPTION, properties);
        // System.err.println("HRITDataSource constructor (3 param) in...");
        String dataCategoryStr = "HRIT Data";
        if ((newSources != null) && (newSources.size() >= 1)) {
        	String fileNameFullPath = (String) newSources.get(0);
        	if (fileNameFullPath.contains("MSG2")) {
        		String channelStr = fileNameFullPath.substring(fileNameFullPath.lastIndexOf("MSG2") + 13, fileNameFullPath.lastIndexOf("MSG2") + 19);
        		String timeStr = fileNameFullPath.substring(fileNameFullPath.lastIndexOf("MSG2") + 33, fileNameFullPath.lastIndexOf("MSG2") + 45);
        		dataCategoryStr = "MSG2 " + channelStr + " " + timeStr;
        	}
        }
        DataCategory.createCategory(dataCategoryStr);
        categories = DataCategory.parseCategories(dataCategoryStr + ";IMAGE");
        //categories = DataCategory.parseCategories("IMAGE*-");
        // System.err.println("HRITDataSource constructor (3 param) out...");
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
     * @param resolution         resolution criteria
     * @param requestProperties  extra request properties
     * @return  the Data object for the request
     *
     * @throws RemoteException couldn't create a remote data object
     * @throws VisADException  couldn't create the data
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection resolution,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {

        String newRes = (String) resolution.getProperty("magnification");
        int magFactor = 1;
        try {
        	magFactor = Integer.parseInt(newRes);
        } catch (NumberFormatException nfe) {
        	nfe.printStackTrace();
        }

        // pull out source index 
        String idxStr = dataChoice.getName().substring(dataChoice.getName().length() - 2, dataChoice.getName().length());
        
        Data data = null;
/*    	String[] files = new String[sources.size()];
    	for (int i = 0; i < sources.size(); i++) {
    		files[i] = (String) sources.get(i);
    		System.err.println("Processing file: " + files[i]);
    	}*/
        
        String [] files = new String[1];
        for (int i = 0; i < sources.size(); i++) {
        	String tmpStr = (String) sources.get(i);
        	String segStr = tmpStr.substring(tmpStr.lastIndexOf("MSG2") + 27, tmpStr.lastIndexOf("MSG2") + 29);
        	if (segStr.equals(idxStr)) {
        		files[0] = (String) sources.get(i);
        	}
        }

    	HRITAdapter ha;
		try {
			ha = new HRITAdapter(files, magFactor);
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
    	JComboBox jcb = null;

    	ResolutionSelection(DataChoice dataChoice) throws Exception {
    		super("Magnification");
    		this.dataChoice = dataChoice;
    		display = new JPanel(new FlowLayout());
    		String[] resStrings = { "1", "2", "4", "8", "16" };
    		jcb = new JComboBox(resStrings);
    		display.add(jcb);
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
    			dataSelection.putProperty("magnification", jcb.getSelectedItem());
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    }
}
