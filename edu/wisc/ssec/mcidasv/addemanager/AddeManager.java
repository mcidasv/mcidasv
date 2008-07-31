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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.io.*;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ucar.unidata.util.GuiUtils;

import edu.wisc.ssec.mcidasv.Constants;

/**
 *  Includes graphical RESOLV.SRV editor...
 */
public class AddeManager {
	
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
	
	/** Main panel containing editable RESOLV.SRV entries */
	final JPanel editPanel = new JPanel();
		
	/** Thread for the mcservl process */
	AddeThread thread = null;
	
	/** List of entries read from RESOLV.SRV */
	List<AddeEntry> addeEntries = new ArrayList<AddeEntry>();
	
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
	        mOut.append("\nRead error:"+e.getMessage());
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
				"MCPATH=" + addeData + ":" + userDirectory,
				"LD_LIBRARY_PATH=" + addeBin,
				"MCNOPREPEND=1"
		};
		
		String[] addeEnvWindows = {
				"PATH=" + addeBin,
				"MCPATH=" + addeData + ";" + userDirectory,
				"MCNOPREPEND=1",
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
        		        System.out.println(addeMcservl + " couldn't start on port "+ oldPort + ", trying " + LOCAL_PORT);
        		        startLocalServer();
    		    	}
    		    	else {
    		    		System.out.println(addeMcservl + " returned: " + result);
    		    		System.out.println("  " + errString);
    		    	}
    		    }
    		    else {
    		    	System.out.println(addeMcservl + " went away...");
    		    }
    		    
    		}
    		catch (InterruptedException e) {
    			System.out.println(addeMcservl + " was interrupted");
    		}
    		catch (Exception e) {
    		    System.out.println("Error executing "+addeMcservl);
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
	public AddeManager() {
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
//			addeResolv = addeData + "/RESOLV.SRV";
			userDirectory = System.getProperty("user.home") + "/" + ".mcidasv";
			addeResolv = userDirectory + "/RESOLV.SRV";
		} else {
			addeDirectory = System.getProperty("user.dir") + "\\adde";
			addeBin = addeDirectory + "\\bin";
			addeData = addeDirectory + "\\data";
			addeMcservl = addeBin + "\\mcservl.exe";
//			addeResolv = addeData + "\\RESOLV.SRV";
			userDirectory = System.getProperty("user.home") + "\\" + ".mcidasv";
			addeResolv = userDirectory + "\\RESOLV.SRV";
		}
		
        try {
            readResolvFile();
        } catch (FileNotFoundException e) { }
	
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
		        System.out.println(addeMcservl + " was started on port " + LOCAL_PORT);
	    	} else {
	    		System.out.println(addeMcservl + " is already running on port " + LOCAL_PORT);
	    	}
	    } else {
	    	System.out.println(addeMcservl + " does not exist");
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
			System.out.println(addeMcservl + " was stopped on port " + LOCAL_PORT);
		} else {
			System.out.println(addeMcservl + " is not running");
		}
	}
	
	/**
	 * check to see if the thread is running
	 */
	private boolean checkLocalServer() {
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
	public List<String> getGroups() {
		List<String> addeGroups = new ArrayList<String>();
		
		Iterator<AddeEntry> it = addeEntries.iterator();
		while (it.hasNext()) {
			AddeEntry ae = (AddeEntry)it.next();
			String ag = ae.getGroup();
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
	public void readResolvFile() throws FileNotFoundException {
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
	public void writeResolvFile() throws FileNotFoundException {
		File addeFile = new File(addeResolv);

		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(addeFile));
			try {
				Iterator<AddeEntry> it = addeEntries.iterator();
				while (it.hasNext()) {
					AddeEntry ae = (AddeEntry)it.next();
					String outString=ae.getResolvEntry();
					if (outString == null) continue;
					output.write(outString + "\n");
				}
			}
			finally {
				output.close();
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Create a panel suitable for the preference manager
	 */
	public JPanel doMakePreferencePanel() {				
		List<Component> subPanels = new ArrayList<Component>();
		doRedrawEditPanel();
		AddeEntry tempEntry = new AddeEntry();
		subPanels.add(tempEntry.doMakePanelLabel());
		subPanels.add(editPanel);
				
		final JButton addButton = new JButton("Add new entry");
		addButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
        		AddeEntry addeEntry = new AddeEntry();
        		addeEntries.add(addeEntry);
        		doRedrawEditPanel();
			}
		});
		
		String statusString = new String("Local server is ");
		if (checkLocalServer()) statusString += "listening on port " + LOCAL_PORT;
		else statusString += "not running";
		JLabel statusLabel = new JLabel(statusString);
		
		subPanels.add(addButton);
		subPanels.add(new JPanel());
		subPanels.add(statusLabel);
		JPanel fullPanel = GuiUtils.vbox(subPanels);
		
		return GuiUtils.inset(GuiUtils.topLeft(fullPanel), 5);
	}
	
	private JPanel doMakeEditPanel() {
		List<Component> editLines = new ArrayList<Component>();
		
		Iterator<AddeEntry> it = addeEntries.iterator();
		while (it.hasNext()) {
			final AddeEntry ae = (AddeEntry)it.next();
			
			final JButton removeButton = new JButton("X");
			removeButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					addeEntries.remove(ae);
					doRedrawEditPanel();
				}
			});
			
			JPanel editableLine = new JPanel();
			editableLine = GuiUtils.left(GuiUtils.hbox(removeButton, ae.doMakePanel()));

			editLines.add(editableLine);
		}
		
		return GuiUtils.vbox(editLines);
	}
	
	private void doRedrawEditPanel() {
		editPanel.removeAll();
		editPanel.add(doMakeEditPanel());
		editPanel.revalidate();
	}
	
}
