package edu.wisc.ssec.mcidasv.startupmanager;

import javax.swing.JCheckBox;

/**
 * The panel that contains the various IDV options related to batch processing.
 * If I can't get more than the disableGUI thing in here I should just move it
 * into the MiscOption panel.
 * 
 * @author Jonathan Beavers, SSEC
 */
public class BatchOptions extends OptionPanel {
	
	/** Whether or not the user wants to see the McV/IDV GUI. */
	private JCheckBox disableGUI;
	
	private StartupManager manager;
	
	/**
	 * 
	 * 
	 * @param mngr
	 */
	public BatchOptions(StartupManager mngr) {
		super("Batch Processing Management");
		manager = mngr;
	}
		
	/**
	 * 
	 * @return
	 */
	public BatchOptions createPanel() {
		disableGUI = new JCheckBox("Disable GUI", false);
		
		addNormalOption("", disableGUI);
		return this;
	}
	
	/**
	 * 
	 * @return
	 */
	public String savePanel() {
		return new String("Johnny's always running around");
	}
	
	/**
	 * 
	 * @param data
	 */
	public void loadPanel(String data) {
		System.out.println("trying to find certainty");
	}
	
	/**
	 * 
	 * @return
	 */
	public String getFlags() {
		String flags = new String("");
		if (disableGUI.isSelected() == true)
			flags += "-nogui ";
		
		return flags;
	}
}
