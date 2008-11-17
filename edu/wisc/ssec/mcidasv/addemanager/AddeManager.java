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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import ucar.unidata.idv.chooser.adde.AddeServer.Group;
import ucar.unidata.ui.WindowHolder;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Msg;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.McIdasPreferenceManager;

/**
 *  Includes graphical RESOLV.SRV editor...
 */
public class AddeManager extends WindowHolder {
	
	/** Back reference to main McIDASV */
	McIDASV idv;
	
	/** String tried against the <tt>os.name</tt> property. */
	public static final String WINDOWS_ID = "Windows";
	
	/** Is this a Unix-style platform? */
	private boolean isUnixLike = false;

	/** Is this a Windows platform? */
	private boolean isWindows = false;
	
	/** Which port is this particular manager operating on */
    private String LOCAL_PORT = Constants.LOCAL_ADDE_PORT;
	
	/** Populate later after we determine the platform */
	private String addeDirectory;
	private String addeBin;
	private String addeData;
	private String addeMcservl;
	private String addeResolv;
	private String userDirectory;
	
	/** Remember the last used directory for the editor **/
	private String lastMask = "";
	
	/** Thread for the mcservl process */
	private AddeThread thread = null;
	
	/** List of entries read from RESOLV.SRV */
	private List<AddeEntry> addeEntries = new ArrayList<AddeEntry>();
	
	/** Table model for the editor */
	private ResolvTableModel resolvTableModel = new ResolvTableModel(this);
	
	/** Table for the editor */
	private JTable resolvTable = createTable(resolvTableModel);
		
	/**
	 * Thread to read the stderr and stdout of mcservl
	 */
	private class StreamReaderThread extends Thread
	{
	    StringBuffer mOut;
	    InputStreamReader mIn;
	    
	    public StreamReaderThread(InputStream in, StringBuffer out)
	    {
	    mOut=out;
	    mIn=new InputStreamReader(in);
	    }
	    
	    public void run()
	    {
	    int ch;
	    try {
	        while(-1 != (ch=mIn.read()))
	            mOut.append((char)ch);
	        }
	    catch (Exception e)
	        {
	        mOut.append("\nRead error: "+e.getMessage());
	        }
	    }
	    
	}

	/**
	 * Thread that actually execs mcservl
	 */
	private class AddeThread extends Thread {
				
		String[] addeCommands = { addeMcservl, "-p", LOCAL_PORT };
		
		String[] addeEnvUnix = {
				"PATH=" + addeBin,
				"MCPATH=" + userDirectory + ":" + addeData,
				"LD_LIBRARY_PATH=" + addeBin,
				"DYLD_LIBRARY_PATH=" + addeBin,
				"MCNOPREPEND=1",
				"MCJAVAPATH=" + System.getProperty("java.home"),
				"MCBUFRJARPATH=" + addeBin
		};
		
		String[] addeEnvWindows = {
				"PATH=" + addeBin,
				"MCPATH=" + userDirectory + ":" + addeData,
				"MCNOPREPEND=1",
				"MCJAVAPATH=" + System.getProperty("java.home"),
				"MCBUFRJARPATH=" + addeBin,
				"SYSTEMDRIVE=C:",
				"SYSTEMROOT=C:\\Windows",
				"HOMEDRIVE=C:",
				"HOMEPATH=\\Windows"
		};
		
		int result;
		Process proc;
		
		//prepare buffers for process output and error streams
		StringBuffer err=new StringBuffer();
		StringBuffer out=new StringBuffer();

        public void run() {
    		try {
    			//start ADDE binary with "-p PORT" and set environment appropriately
    			if (isUnixLike) {
        		    proc=Runtime.getRuntime().exec(addeCommands, addeEnvUnix);
    			}
    			else {
    				proc=Runtime.getRuntime().exec(addeCommands, addeEnvWindows);
    			}
    		    //create thread for reading inputStream (process' stdout)
    		    StreamReaderThread outThread=new StreamReaderThread(proc.getInputStream(),out);
    		    //create thread for reading errorStream (process' stderr)
    		    StreamReaderThread errThread=new StreamReaderThread(proc.getErrorStream(),err);
    		    //start both threads
    		    outThread.start();
    		    errThread.start();
    		    //wait for process to end
    		    result=proc.waitFor();
    		    //finish reading whatever's left in the buffers
    		    outThread.join();
    		    errThread.join();
    		    
    		    if (result!=0) {
    		    	stopLocalServer();
    		    	String errString = err.toString();
    		    	
    		    	/** If the server couldn't start for a known reason, try again on another port
    		    	 *  Retry up to 10 times 
    		    	 */
    		    	if ((result==35584 || errString.indexOf("Error binding to port") >= 0) &&
    		    			Integer.parseInt(LOCAL_PORT) < Integer.parseInt(Constants.LOCAL_ADDE_PORT) + 10) {
        		    	String oldPort = LOCAL_PORT;
        		    	setLocalPort(nextLocalPort());
//        		        System.out.println(addeMcservl + " couldn't start on port "+ oldPort + ", trying " + LOCAL_PORT);
        		        startLocalServer();
    		    	}
    		    	else {
//    		    		System.out.println(addeMcservl + " returned: " + result);
//    		    		System.out.println("  " + errString);
    		    	}
    		    }
    		    else {
//    		    	System.out.println(addeMcservl + " went away...");
    		    }
    		    
    		}
    		catch (InterruptedException e) {
//    			System.out.println(addeMcservl + " was interrupted");
    		}
    		catch (Exception e) {
//    		    System.out.println("Error executing " + addeMcservl);
    		    e.printStackTrace();
    		}
        }
        
        public void stopProcess() {
        	proc.destroy();
        }
    }
			
	/**
	 * ctor keeps track of where adde stuff should be
	 */
	public AddeManager(McIDASV myself) {
		idv = myself;
		try {
			determinePlatform();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}

		if (isUnixLike == true) {
			addeDirectory = System.getProperty("user.dir") + "/adde";
			addeBin = addeDirectory + "/bin";
			addeData = addeDirectory + "/data";
			addeMcservl = addeBin + "/mcservl";
//			userDirectory = System.getProperty("user.home") + "/" + ".mcidasv";
			userDirectory = idv.getObjectStore().getUserDirectory().toString();
			addeResolv = userDirectory + "/RESOLV.SRV";
		} else {
			addeDirectory = System.getProperty("user.dir") + "\\adde";
			addeBin = addeDirectory + "\\bin";
			addeData = addeDirectory + "\\data";
			addeMcservl = addeBin + "\\mcservl.exe";
//			userDirectory = System.getProperty("user.home") + "\\" + ".mcidasv";
			userDirectory = idv.getObjectStore().getUserDirectory().toString();
			addeResolv = userDirectory + "\\RESOLV.SRV";
		}
		
		try {
			readResolvFile();
		} catch (FileNotFoundException ex) { }
		cleanAddeEntries();

	}
	
	/**
	 * Change the port we are listing on
	 * @param localPort
	 */
	public void setLocalPort(String localPort) {
		LOCAL_PORT = localPort;
	}
	
	/**
	 * Ask for the port we are listening on
	 * @return
	 */
	public String getLocalPort() {
		return LOCAL_PORT;
	}
	
	/**
	 * Get the next port by incrementing current port
	 * @return
	 */
	private String nextLocalPort() {
		return Integer.toString(Integer.parseInt(LOCAL_PORT) + 1);
	}
	
	/**
	 * start addeMcservl if it exists
	 */
	public void startLocalServer() {
	    boolean exists = (new File(addeMcservl)).exists();
	    if (exists) {
	        // Create and start the thread if there isn't already one running
	    	if (!checkLocalServer()) {
	    		thread = new AddeThread();
	    		thread.start();
//		        System.out.println(addeMcservl + " was started on port " + LOCAL_PORT);
//	    	} else {
//	    		System.out.println(addeMcservl + " is already running on port " + LOCAL_PORT);
	    	}
	    } else {
	    	System.err.println(addeMcservl + " does not exist");
	    }
	}
	
	/**
	 * stop the thread if it is running
	 */
	public void stopLocalServer() {
		if (checkLocalServer()) {
			thread.stopProcess();
			thread.interrupt();
			thread = null;
//			System.out.println(addeMcservl + " was stopped on port " + LOCAL_PORT);
//		} else {
//			System.out.println(addeMcservl + " is not running");
		}
	}
	
	/**
	 * check to see if the thread is running
	 */
	public boolean checkLocalServer() {
		if (thread != null && thread.isAlive()) return true;
		else return false;
	}

	/**
	 * Queries the "os.name" property and tries to match against known platform
	 * strings. Currently this method will simply set one of <tt>isWindows</tt>
	 * or <tt>isUnixLike</tt> depending on whether or not Windows was found.
	 */
	private void determinePlatform() {
		String os = System.getProperty("os.name");
		
		if (os == null)
			throw new RuntimeException();
		
		if (os.startsWith(WINDOWS_ID))
			isWindows = true;
		else
			isUnixLike = true;
	}
	
	/**
	 * return a list of known groups
	 */
	public List<Group> getGroups() {
		List<Group> addeGroups = new ArrayList<Group>();
		
		Iterator<AddeEntry> it = addeEntries.iterator();
		while (it.hasNext()) {
			AddeEntry ae = (AddeEntry)it.next();
			Group ag = new Group(ae.getGroup(), ae.getGroup(), ae.getGroup());
			if (!addeGroups.contains(ag))
				addeGroups.add(ag);
		}
		
		return addeGroups;
	}
	
	/**
	 * read RESOLV.SRV into addeEntries
	 * 
	 * @throws FileNotFoundException 
	 */
	public synchronized void readResolvFile() throws FileNotFoundException {
		addeEntries.clear();
		File addeFile = new File(addeResolv);
		if (!addeFile.exists() || !addeFile.isFile() || !addeFile.canRead()) {
			return;
		}
	    try {
	        BufferedReader input =  new BufferedReader(new FileReader(addeFile));
	        try {
	        	String line = null;
	        	while (( line = input.readLine()) != null) {
	        		line=line.trim();
	        		if (line.equals("")) continue;
	        		if (line.substring(0, 1).equals("#")) continue;
	        		AddeEntry addeEntry = new AddeEntry(line);
	        		addeEntries.add(addeEntry);
	        	}
	        }
	        finally {
	        	input.close();
	        }
	    }
	    catch (IOException ex) {
	        ex.printStackTrace();
	    }
	}
	
	/**
	 * write new RESOLV.SRV from addeEntries
	 * 
	 * @throws FileNotFoundException 
	 */
	public synchronized void writeResolvFile() throws FileNotFoundException {
		File addeFile = new File(addeResolv);

		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(addeFile));
			try {
				for (int i = 0; i < addeEntries.size(); i++) {
					AddeEntry ae = addeEntries.get(i);
					if (!ae.isValid()) continue;
					ae.setDescriptor("ENTRY" + i);
					output.write(ae.getResolvEntry() + "\n");
				}
			}
			finally {
				output.close();
				McIdasPreferenceManager prefs = 
				    (McIdasPreferenceManager)idv.getPreferenceManager();
				prefs.getServerManager().updateManagedChoosers();
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Clean out invalid entries that can crop up when editing
	 */
	private synchronized void cleanAddeEntries() {
		for (int i=addeEntries.size()-1; i>=0; i--) {
			if (addeEntries.get(i).isValid()) continue;
			else addeEntries.remove(i);
		}
	}
	
    private static String HELP_TOP_DIR = "/auxdata/docs/userguide";
    public void showHelp() {
        ucar.unidata.ui.Help.setTopDir(HELP_TOP_DIR);
        ucar.unidata.ui.Help.getDefaultHelp().gotoTarget(
            "idv.tools.localdata");
    }
    
    public void close() {
    	super.close();
    }

    /**
     * Create a new entry
     */
    public void newEntry() {
        editEntry(null, 0, true);
    }
    
    /**
     * Create a JTable for RESOLV.SRV
     *
     * @param tableModel The table model to use
     * @return The newly created JTable
     */
    private JTable createTable(ResolvTableModel tableModel) {
        final JTable table = new JTable(tableModel);
        tableModel.table = table;
//        resolvTable = table;
        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                handleMouseEvent(e, table);
            }
        });

        table.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == e.VK_DELETE) {
                    resolvTableModel.remove(table.getSelectedRows());
                }
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(40);
        table.getColumnModel().getColumn(2).setPreferredWidth(40);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        return table;
    }
    
    /**
     * Get the title to use for the iwindow
     *
     * @return window title
     */
    public String getWindowTitle() {
        return "Local ADDE Data Manager";
    }
    
    /**
     * Initialize. Load in the resources and create the GUI.
     */
    protected JComponent doMakeContents() {
    	JPanel editPanel = new JPanel(new BorderLayout());
    	
		try {
			readResolvFile();
		} catch (FileNotFoundException ex) { }
		cleanAddeEntries();
    	    	
		Iterator<AddeEntry> it = addeEntries.iterator();
		while (it.hasNext()) {
			final AddeEntry ae = (AddeEntry)it.next();
			resolvTableModel.add(ae.getGroup(), ae.getName(), ae.getDescription(), ae.getMask());
		}

		JScrollPane sp =
                new JScrollPane(
                    resolvTable, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JComponent contents    = sp;

		resolvTable.setToolTipText("<html>" + Msg.msg("Right click to edit or delete")
				+ "<br>" + Msg.msg("Double click to edit") + "</html>");
		String path = Msg.msg("Path: ${param1}", addeResolv);

		String status = new String("Local server is ");
		if (checkLocalServer()) status += "listening on port " + LOCAL_PORT;
		else status += "not running";

//		contents = GuiUtils.topCenterBottom(
//				GuiUtils.inset(new JLabel("<html>" + path + "</html>"), 5),
//				sp,
//				GuiUtils.inset(new JLabel("<html>" + status + "</html>"), 5)
//				);
		
		contents = GuiUtils.topCenter(
				sp,
				GuiUtils.inset(new JLabel("<html>" + status + "</html>"), 5)
				);

		editPanel.add(contents);
		
        JMenuBar menuBar  = new JMenuBar();
        JMenu    fileMenu = new JMenu("File");
        JMenu    helpMenu = new JMenu("Help");
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        fileMenu.add(GuiUtils.makeMenuItem("Add New Dataset", this, "newEntry"));
        fileMenu.addSeparator();
        fileMenu.add(GuiUtils.makeMenuItem("Close", this, "close"));

        helpMenu.add(GuiUtils.makeMenuItem("Show Local Data Help", this, "showHelp"));

        JComponent bottom = GuiUtils.wrap(GuiUtils.makeButton("Close", this, "close"));
//        contents = GuiUtils.topCenterBottom(menuBar, editPanel, bottom);
        
        return GuiUtils.topCenterBottom(menuBar, editPanel, bottom);
        
    }
    
    /**
     * If the given MouseEvent is a right mouse click then
     * popup the menu.
     *
     * @param e The event
     * @param table The JTable clicked on
     */
    private void handleMouseEvent(MouseEvent e, JTable table) {
        final int row = table.rowAtPoint(e.getPoint());
        if ( !SwingUtilities.isRightMouseButton(e)) {
            if (e.getClickCount() > 1) {
            	editEntry(resolvTableModel, row, false);
            }
            return;
        }
        table.getSelectionModel().setSelectionInterval(row, row);
        JPopupMenu popup = new JPopupMenu();
        JMenuItem  mi    = null;
        mi = new JMenuItem("Edit Dataset");
        mi.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent ae) {
        		editEntry(resolvTableModel, row, false);
        	}
        });
        popup.add(mi);
        mi = new JMenuItem("Delete Dataset");
        mi.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent ae) {
        		removeEntry(resolvTableModel, row);
        	}
        });
        popup.add(mi);
        popup.show((Component) e.getSource(), e.getX(), e.getY());
    }
    
    /**
     * Popup the edit dialog for the given resource and alias
     *
     * @param tableModel The table model to edit
     * @param row The row to edit
     * @param deleteOnCancel delete entry if cancel is pressed
     */
    public void editEntry(ResolvTableModel tableModel, int row,
                           boolean deleteOnCancel) {
        boolean newEntry = tableModel == null;
        AddeEntry ae;
        
        String windowTitle;
        if (newEntry) {
        	ae = new AddeEntry();
        	if (!lastMask.equals("")) ae.setMask(lastMask);
        	windowTitle = "Add dataset";
        }
        else {
        	ae = tableModel.getAddeEntry(row);
        	windowTitle = "Edit dataset";
        }
                
        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        JPanel p = ae.doMakePanel();
        if ( !GuiUtils.showOkCancelDialog(null, windowTitle, p, null)) {
            if (deleteOnCancel && !newEntry) {
                removeEntry(tableModel, row);
            }
            return;
        }
        String group  = ae.getGroup();
        String name = ae.getName();
        String description = ae.getDescription();
        String mask = ae.getMask();
        lastMask = mask;
        if (!ae.isValid()) return;
        if (!newEntry) {
            tableModel.set(row, group, name, description, mask);
        } else {
            resolvTableModel.add(group, name, description, mask);
        }
        saveEntries();
    }

    /**
     * remove entry
     *
     * @param from which table
     * @param row kthe row
     */
    public void removeEntry(ResolvTableModel from, int row) {
        from.remove(row);
        saveEntries();
    }
    
    /**
     * Write out the new RESOLV.SRV
     */
    private void saveEntries() {
    	addeEntries = resolvTableModel.getAddeEntries();
        try {
        	writeResolvFile();
        }
        catch (FileNotFoundException ex) {
        	System.err.println("Error writing RESOLV.SRV");
        }
    }

    /**
     * Returns current list of {@link AddeEntry}s
     */
    public List<AddeEntry> getAddeEntries() {
        return addeEntries;
    }
    
    /**
     * Returns current list of {@link AddeEntry}s, given a type
     */
    public List<AddeEntry> getAddeEntriesByType(String type) {
    	List<AddeEntry> typedEntries = new ArrayList<AddeEntry>();
    	
		Iterator<AddeEntry> it = addeEntries.iterator();
		while (it.hasNext()) {
			AddeEntry ae = (AddeEntry)it.next();
			if (ae.getType().equals(type))
			typedEntries.add(ae);
		}

        return typedEntries;
    }

    /**
     * Class ResolvTableModel. This extends AbstractTableModel and
     * manages the data RESOLV.SRV
     */
    private static class ResolvTableModel extends AbstractTableModel {
 
        List groups = new ArrayList();
        List names = new ArrayList();
        List descriptions = new ArrayList();
        List masks = new ArrayList();

        /** Back reference to the main editor */
        AddeManager editor;

        /** the jtable */
        JTable table;

        /**
         * Create the table mode
         */
        public ResolvTableModel(AddeManager editor) {
            this.editor      = editor;
        }

        /**
         * Add the given group, descriptor, description, mask and
         * fire a TableStructureChanged event
         */
        public void add(String group, String name, String description, String mask) {
            this.groups.add(group);
            this.names.add(name);
            this.descriptions.add(description);
            this.masks.add(mask);
            fireTableStructureChanged();
        }


        /**
         * Copy the given information into the lists.
         */
        public void set(int row, String group, String name, String description, String mask) {
            this.groups.set(row, group);
            this.names.set(row, name);
            this.descriptions.set(row, description);
            this.masks.set(row, mask);
            fireTableStructureChanged();
        }

        /**
         * Get the group for the given row
         */
        public String getGroup(int row) {
            return (String) groups.get(row);
        }

        /**
         * Get the description for the given row
         */
        public String getDescription(int row) {
            return (String) descriptions.get(row);
        }
        
        /**
         * Get the mask for the given row
         */
        public String getMask(int row) {
            return (String) masks.get(row);
        }
        
        /**
         * Get the name for the given row
         */
        public String getName(int row) {
            return (String) names.get(row);
        }

        /**
         * Create a new list which is the given list with elements defined by the given
         * the indices array removed
         *
         * @param from The list to remove elements from
         * @param indices The indexes to remove
         * @return The new list
         */
        private List remove(List from, int[] indices) {
            List tmp = new ArrayList();
            for (int i = 0; i < from.size(); i++) {
                boolean isIndexIn = false;
                for (int j = 0; (j < indices.length) && !isIndexIn; j++) {
                    isIndexIn = (indices[j] == i);
                }
                if (isIndexIn) {
                    continue;
                }
                tmp.add(from.get(i));
            }
            return tmp;
        }

        /**
         * Remove from the data lists the indices in the given rows argument
         *
         * @param rows The indices to remove
         */
        public void remove(int[] rows) {
            if (groups.size() == 0) {
                return;
            }
            groups   = remove(groups, rows);
            names = remove(names, rows);
            descriptions = remove(descriptions, rows);
            masks = remove(masks, rows);
            fireTableStructureChanged();
            int min = Integer.MAX_VALUE;
            for (int i = 0; i < rows.length; i++) {
                if (rows[i] < min) {
                    min = rows[i];
                }
            }
            while (min >= groups.size()) {
                min--;
            }
            if (min >= 0) {
                editor.resolvTable.setRowSelectionInterval(min, min);
            }

        }

        /**
         * Remove from the data lists the given row
         *
         * @param row The row to remove
         */
        public void remove(int row) {
            groups.remove(row);
            names.remove(row);
            descriptions.remove(row);
            masks.remove(row);
            fireTableStructureChanged();
        }
        
        /**
         * Get the RESOLV.SRV entry from the given table row
         */
        public AddeEntry getAddeEntry(int row) {
        	AddeEntry ae = new AddeEntry(getGroup(row), getName(row), getDescription(row), getMask(row));
        	return ae;
        }
        
        /**
         * Get all RESOLV.SRV entries
         */
        public List<AddeEntry> getAddeEntries() {
        	List<AddeEntry> addeEntries = new ArrayList<AddeEntry>();
        	for (int i=0; i<getRowCount(); i++) {
        		addeEntries.add(getAddeEntry(i));
        	}
        	return addeEntries;
        }

        /**
         * How many rows
         *
         * @return  How many rows
         */
        public int getRowCount() {
            return groups.size();
        }

        /**
         * How many columns
         *
         * @return How many columns
         */
        public int getColumnCount() {
            return 4;
        }

        /**
         * Insert the given value into the appropriate data list
         *
         * @param aValue The value
         * @param rowIndex The row
         * @param columnIndex The column
         */
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        	switch (columnIndex) {
        	case 0: groups.set(rowIndex, aValue.toString()); break;
        	case 1: names.set(rowIndex, aValue.toString()); break;
        	case 2: descriptions.set(rowIndex, aValue.toString()); break;
        	case 3: masks.set(rowIndex, aValue.toString()); break;
        	}
        }

        /**
         * Return the value at the given row/column
         *
         * @param row The row
         * @param column The column
         *
         * @return The value
         */
        public Object getValueAt(int row, int column) {
        	Object returnObject = "";
        	switch (column) {
        	case 0: returnObject = groups.get(row); break;
        	case 1: returnObject = names.get(row); break;
        	case 2: returnObject = descriptions.get(row); break;
        	case 3: returnObject = masks.get(row); break;
        	}
            return returnObject;
        }

        /**
         * Get the name of the given column
         *
         * @param column The column
         * @return Its name
         */
        public String getColumnName(int column) {
        	String returnString = "";
        	switch (column) {
        	case 0: returnString = "Dataset (e.g. MYDATA)"; break;
        	case 1: returnString = "Image Type (e.g. JAN 07 GOES)"; break;
        	case 2: returnString = "Format"; break;
        	case 3: returnString = "Directory"; break;
        	}
            return returnString;
        }

    }
    
    public McIDASV getIDV() {
    	return idv;
    }
    
}
