package edu.wisc.ssec.mcidasv.startupmanager;

import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;

public class JamPackedPanel extends OptionPanel {
	
	public JamPackedPanel(StartupManager mngr) {
		super("Panel 1");
	}

	public JamPackedPanel createPanel() {
		JCheckBox mcidasScheduler = new JCheckBox();
		JCheckBox caseInvert = new JCheckBox();
		
		JTextField workingDir = new JTextField();
		JTextField maxHeap = new JTextField();
		JTextField initialHeap = new JTextField();
		
		JSpinner stackSize = new JSpinner();
		JSpinner heapYoungSize = new JSpinner();
		JSpinner allocMemory = new JSpinner();
		
		addNormalOption("Java Maximum Heap Size", maxHeap);
		addNormalOption("Java Initial Heap Size", initialHeap);
		addNormalOption("Java Thread Stack Size", stackSize);
		addNormalOption("Java Young Generation Heap Size", heapYoungSize);
		addNormalOption("Enable McIDAS-X Scheduler", mcidasScheduler);
		addNormalOption("Enable McIDAS-X Case Inversion", caseInvert);
		addNormalOption("McIDAS-X Working Directory", workingDir);
		addNormalOption("Memory allocated to McIDAS-X", allocMemory);
		return this;
	}
	
	public String savePanel() {
		return new String("temp");
	}
	
	public void loadPanel(String data) {
		return;
	}
	
	public String getFlags() {
		return new String("temp");
	}
}