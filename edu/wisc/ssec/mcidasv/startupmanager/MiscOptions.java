package edu.wisc.ssec.mcidasv.startupmanager;

import javax.swing.JCheckBox;

/**
 * Use tasks list to see ALL TODOs/FIXMEs
 * set eclipse cvs to ignore .project, .classpath
 * look at working set eclipse functionality: add resource refs
 * @author Jonathan Beavers, SSEC
 */
public class MiscOptions extends OptionPanel {
	private JCheckBox disableDebug;
	private JCheckBox disableCodeTrace;
	private JCheckBox showDataChoosers;
	
	private StartupManager manager;
	
	public MiscOptions(StartupManager mngr) {
		super("Miscellaneous Options");
		manager = mngr;
	}
	
	public MiscOptions createPanel() {
		disableDebug = new JCheckBox("Disable debugging output", true);
		disableCodeTrace = new JCheckBox("Disable code trace output", true);
		showDataChoosers = new JCheckBox("Show choosers on start", false);
		
		addNormalOption("", disableDebug);
		addNormalOption("", disableCodeTrace);
		addNormalOption("", showDataChoosers);
		return this;
	}
	
	public String getFlags() {
		String flags = new String("");
		
		if (disableDebug.isSelected() == false)
			flags += "-debug ";
		if (disableCodeTrace.isSelected() == false)
			flags += "-trace ";
		if (showDataChoosers.isSelected() == true)
			flags += "-chooser "; // TODO: this doesn't seem to work.
		
		return flags;
	}
	
	public String savePanel() {
		return new String("");
	}
	
	public void loadPanel(String data) {
		System.out.println("");
	}
}