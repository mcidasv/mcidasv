package edu.wisc.ssec.mcidasv.startupmanager;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * 
 * @author Jonathan Beavers, SSEC
 */
public class BundleOptions extends OptionPanel {
	// BAD BAD BAD
	protected final String[] VALID_BUNDLE_OPTIONS = {"Load", "Disable"};
	protected final String[] VALID_PLUGIN_OPTIONS = {"Install", "Load", "Disable"};
	
	protected JCheckBox clearDefaultBundle;
	protected JCheckBox disableDefaultBundle;
			
	private StartupManagerTable table;
			
	private StartupManager manager;
	
	public BundleOptions(StartupManager mngr) {
		super("Bundle Management");
		
		manager = mngr;
		
		table = new StartupManagerTable(manager, ListItem.ID_BUNDLE);
		table.add(new ListItem("Bundle 1", "./b1", "Load", ListItem.ID_BUNDLE));
		table.add(new ListItem("Bundle 2", "./b2", "Load", ListItem.ID_BUNDLE));
		table.add(new ListItem("Bundle 3", "./b3", "Disable", ListItem.ID_BUNDLE));
		table.add(new ListItem("Bundle 4", "./b4", "bah bah", ListItem.ID_BUNDLE));			
	}
	
	public BundleOptions createPanel() {
		JPanel hmm = table.createDisplayableTable(); // TODO: better name
		clearDefaultBundle = new JCheckBox("Clear default bundle", false);
		disableDefaultBundle = new JCheckBox("Disable all plugins", false);
					
		addAdvancedOption("", clearDefaultBundle);
		addAdvancedOption("", disableDefaultBundle);
		addAdvancedOption("", hmm); // TODO: better name
		return this;
	}

	public String getFlags() {
		String flags = new String("");
		if (clearDefaultBundle.isEnabled() == true)
			flags += "-cleardefault ";
		if (disableDefaultBundle.isEnabled() == true)
			flags += "-nodefault ";
		
		// iterate through table...
		return flags;
	}
	
	public String savePanel() {
		return new String("");
	}
	
	public void loadPanel(String data) {
		System.out.println("");
	}
}