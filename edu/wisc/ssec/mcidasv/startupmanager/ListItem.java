package edu.wisc.ssec.mcidasv.startupmanager;

import javax.swing.JLabel;

/**
 * A ListItem is the object that occupies a row within a StartupManagerTable.
 * It's an easy-to-interact-with abstraction of either a bundle or a plugin...
 * users can easily alter how McV/IDV will treat the bundle or plugin, specify
 * and edit a human-friendly name for the bundle/plugin, and provide or edit
 * the paths associated with the bundle/plugin.
 * 
 * @author Jonathan Beavers, SSEC
 */
public class ListItem extends JLabel {
	/** */
	protected final String[] VALID_BUNDLE_OPTIONS = {"Load", "Disable"};
	
	/** */
	protected final String[] VALID_PLUGIN_OPTIONS = {"Install", "Load", "Disable"};
	
	// TODO: move these guys out of ListItem and into StartupManager proper?
	
	/** */
	public static final int ID_BUNDLE = 0xBEEFBEEF;
	
	/** */
	public static final int ID_PLUGIN = 0xCAFE;
	
	/** */
	public static final int ID_BADTYPE = 0xDECAF;
	
	/** 
	 * An easy way for the user to refer to this file. Note: say the user 
	 * provides /home/user/.whatever/plugin.ext, label will default to 
	 * "plugin." 
	 */
	protected String label;
	
	/** One of the options supported by bundles or plugins. */
	protected String option;
	
	/** The path of the actual bundle or plugin. */
	protected String path;
	
	/** 
	 * A copy of one of ID_BUNDLE or ID_PLUGIN so that we can figure out what 
	 * this object is!
	 */
	protected int itemType;
	
	/** If this object doesn't result in ID_BADTYPE this will be true. */
	protected boolean isValid = false;
	
	/** Whether or not the user can interact with this item. */
	protected boolean isEnabled = false;
	
	/** 
	 * 
	 * 
	 * @param label
	 * @param path
	 * @param option
	 * @param type
	 */
	public ListItem(String label, String path, String option, int type) {
		this.label = label;
		this.path = path;
		this.option = option;

		if ((type == ID_BUNDLE) && (validateOptions(VALID_BUNDLE_OPTIONS) == true))
			itemType = ID_BUNDLE;
		else if ((type == ID_PLUGIN) && (validateOptions(VALID_PLUGIN_OPTIONS) == true))
			itemType = ID_PLUGIN;
		else
			itemType = ID_BADTYPE;
	}
	
	/**
	 * Attempt to match the option provided in the constructor against the 
	 * known options for the type given in the constructor.
	 * 
	 * @param tempOpts The options to use as a test.
	 * 
	 * @return true if everything checks out, false otherwise. 
	 */
	private boolean validateOptions(String[] tempOpts) {
		for (int i = 0; i < tempOpts.length; i++) {
			if (option.equals(tempOpts[i])) {
				if (tempOpts[i].equals("Load"))
					isEnabled = true;
				
				isValid = true;
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return
	 */
	public String getOption() {
		return option;
	}

	/**
	 * @return
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return
	 */
	public int getItemType() {
		return itemType;
	}

	/**
	 * @return
	 */
	public boolean getIsValid() {
		return isValid;
	}

	/**
	 * @return
	 */
	public boolean getIsEnabled() {
		return isEnabled;
	}

	/**
	 * @param label
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * @param option
	 */
	public void setOption(String option) {
		this.option = option;
	}

	/**
	 * @param path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @param type
	 */
	public void setItemType(int type) {
		itemType = type;
	}

	/**
	 * @param value
	 */
	public void setIsValid(boolean value) {
		isValid = value;
	}

	/**
	 * @param value
	 */
	public void setIsEnabled(boolean value) {
		isEnabled = true;
	}
}