package edu.wisc.ssec.mcidasv.startupmanager;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * 
 * @author Jonathan Beavers, SSEC
 */
public class PluginOptions extends OptionPanel {
	// BAD BAD BAD
	protected final String[] VALID_BUNDLE_OPTIONS = {"Load", "Disable"};
	protected final String[] VALID_PLUGIN_OPTIONS = {"Install", "Load", "Disable"};
	
	private JCheckBox disablePlugins;
	
	private StartupManagerTable table;
	
	private StartupManager manager;
	
	public PluginOptions(StartupManager mngr) {
		super("Plugin Management");
	
		manager = mngr;
		
		table = new StartupManagerTable(manager, ListItem.ID_PLUGIN);
		table.add(new ListItem("Plugin 1", "./p1", "Load", ListItem.ID_PLUGIN));
		table.add(new ListItem("Plugin 2", "./p2", "Install", ListItem.ID_PLUGIN));
		table.add(new ListItem("Plugin 3", "./p3", "Disable", ListItem.ID_PLUGIN));
	}
	
	public String getFlags() {
		String flags = new String("");
		
		if (disablePlugins.isSelected() == true)
			flags += "-noplugins ";
		
		return flags;
	}
	
	public String savePanel() {
		return new String("i'm a 21st century digital boy");
	}
	
	public void loadPanel(String data) {
		System.out.println("don't know how to read but I've got a lot of toys");
	}
	
	public PluginOptions createPanel() {
		JPanel hmm = table.createDisplayableTable(); // TODO: better name
		disablePlugins = new JCheckBox("Disable all plugins", false);
		
		
		addNormalOption("", disablePlugins);
		// damn it! setEnabled() doesn't do anything to children!
		addAdvancedOption("", hmm); // TODO: better name
		return this;
	}
}