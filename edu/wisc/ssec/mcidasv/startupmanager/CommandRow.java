package edu.wisc.ssec.mcidasv.startupmanager;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * 
 * @author Jonathan Beavers, SSEC
 */
public class CommandRow implements ActionListener {
	/** */
	private JXCheckBox enableAdvanced;
	
	/** */
	protected boolean advanced;
	
	/** */
	protected CmdRowSaveButton save;
	
	/** */
	protected CmdRowQuitButton quit;
	
	/** */
	private JPanel buttonPanel;
	
	/** */
	private JPanel parent;
	
	private StartupManager manager;
	
	/** 
	 * 
	 */
	public CommandRow(StartupManager mngr) {
		createCommandRow();
		manager = mngr;
		advanced = false;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isAdvanced() {
		return enableAdvanced.isEnabled();
	}
	
	/**
	 * @return The panel that contains the entirety of the command row. 
	 */
	// TODO: candidate for removal?
	public JPanel getPanel() {
		return parent;
	}
	
	/**
	 * @return Whether or not advanced options have been enabled.
	 */
	public boolean getAdvanced() {
		return advanced;
	}
	
	/**
	 * @param val Whether or not to enabled the advanced options.
	 */
	public void setAdvanced(boolean val) {
		advanced = val;
		enableAdvanced.setEnabled(val);
	}

	public void actionPerformed(ActionEvent e) {
		CmdInterface cmd = (CmdInterface)e.getSource();
		cmd.processEvent();
	}
	
	/**
	 * 
	 *
	 */
	private void createCommandRow() {
		buttonPanel = new JPanel();
		enableAdvanced = new JXCheckBox("Enable advanced options", advanced);
		parent = new JPanel();
		
		enableAdvanced.addActionListener(this);
		
		parent.setLayout(new BorderLayout());			
		
		buttonPanel.add(Box.createHorizontalGlue());
		
		save = new CmdRowSaveButton("Save");
		save.addActionListener(this);
		
		quit = new CmdRowQuitButton("Quit");
		quit.addActionListener(this);
		
		buttonPanel.add(save);
		buttonPanel.add(quit);
		
		parent.add(enableAdvanced, BorderLayout.WEST);
		parent.add(buttonPanel, BorderLayout.EAST);
	}

	class CmdRowSaveButton extends JButton implements CmdInterface {
		public CmdRowSaveButton(String label) {
			super(label);			
		}
		
		public void processEvent() {
			System.out.println("listener: save data");
			manager.writeScript(new String());
			manager.saveState();
		}
	}

	class CmdRowQuitButton extends JButton implements CmdInterface {
		public CmdRowQuitButton(String label) {
			super(label);
		}
		
		public void processEvent() {
			System.out.println("listener: quit program");
			System.exit(0);
		}
	}
	
	class JXCheckBox extends JCheckBox implements CmdInterface {
		public JXCheckBox(String label, boolean enabled) {
			super(label, enabled);
		}
		
		public void processEvent() {
			System.out.println("listener: toggled advanced options");
			advanced = !advanced;
			setSelected(advanced);
			manager.toggleAdvancedOptions();
		}
	}
}	

	

	

