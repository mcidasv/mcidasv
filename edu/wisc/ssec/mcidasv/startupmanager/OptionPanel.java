package edu.wisc.ssec.mcidasv.startupmanager;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.LinkedList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * An OptionPanel is the base class that implements a large portion of the 
 * functionality of panels such as BatchOptions, McIDASXOptions and so on. 
 * It is largely responsible for laying out components within the panel and 
 * enabling the ability of users to turn so-called advanced components on or
 * off.
 * 
 * @author Jonathan Beavers, SSEC
 */
public abstract class OptionPanel extends JPanel {
	
	/** The current number of grid bag rows. */
	// TODO: this doesn't appear to get used....
	protected int rows = 0;
	
	/** The current number of grid bag columns. */
	protected int columns = 0;
	
	/** The name of the panel instance. */
	// TODO: investigate whether or not this is needed.
	protected String panelName;
	
	/** The layout manager */
	protected GridBagLayout layout = null;
	
	/** 
	 * Contains the components that the user can enable or disable via the GUI. 
	 */
	// TODO: rename to advancedComponents
	protected LinkedList advancedOptions;
	
	/** Contains the components that are always enabled. */
	// TODO: rename to normalComponents
	protected LinkedList normalOptions;
	
	/**
	 *  Simply instantiate an OptionPanel.
	 * 
	 * @param name The name of the current panel.
	 */
	public OptionPanel(String name) {
		panelName = name;
		
		advancedOptions = new LinkedList();
		normalOptions = new LinkedList();
		
		layout = new GridBagLayout();
		setLayout(layout);
	}

	/**
	 * Loop through each component in the <code>advancedOptions</code> list and
	 * turn them on or off. 
	 */
	public void toggleAdvancedOptions() {
		for (int i = 0; i < advancedOptions.size(); i++) {
			JComponent widget = (JComponent)advancedOptions.get(i);
			widget.setEnabled(!widget.isEnabled());
		}
	}	
	
	// TODO: probably should change to "addAdvancedComponent"
	/**
	 * Add a component to the current 
	 * 
	 * @param label
	 * @param widget
	 */
	public void addAdvancedOption(String label, JComponent widget) {
		addOption(label, widget, true);
	}
	
	// TODO: probably should change to "addNormalComponent" or something like that.
	/**
	 * Add an "option" to the current OptionPanel that will always be enabled.
	 * These options are considered to be normal, and are stored separately 
	 * from the so-called advanced options.
	 * 
	 * @param label The label for the <code>widget</code> parameter. This can
	 *              be an empty string if no label is required.
	 * @param widget The actual component to be added to the current panel.
	 */
	public void addNormalOption(String label, JComponent widget) {
		addOption(label, widget, false);
	}
	
	/**
	 * The generalized version of the public add&lt;blank&gt;Option methods.
	 * 
	 * 
	 * @param label The label for the <code>widget</code> parameter. This can
	 *              be an empty string if no label is required.
	 * @param widget The actual component to be added to the current panel.
	 * @param isAdv Whether or not <code>widget</code> is an option that can be
	 *              enabled or disabled by the user.
	 */
	private void addOption(String label, JComponent widget, boolean isAdv) {
		if (isAdv) {
			widget.setEnabled(false);
			advancedOptions.add(widget);
		} else {
			normalOptions.add(widget);
		}
		
		JLabel labelObj = new JLabel(label);
		
		GridBagConstraints constraints = new GridBagConstraints();
		
		constraints.gridy = columns++;
		constraints.gridheight = 1;
		constraints.gridwidth = 1;
		constraints.weightx = 0.0f;
		constraints.insets = new Insets(2, 2, 2, 2);
		constraints.fill = GridBagConstraints.BOTH;
		
		layout.setConstraints(labelObj, constraints);
		add(labelObj);
		
		labelObj.setLabelFor(widget);
		
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridx = 1;
		constraints.weightx = 1.0f;
		layout.setConstraints(widget, constraints);
		add(widget);
	}
	
	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		
		g2d.setRenderingHints(StartupManager.getRenderingHints());
		
		super.paintComponent(g2d);		
		
	}
	
	/**
	 * This is basically just a stub. Subclasses of OptionPanel should do most
	 * of their GUI creation stuff within this method. 
	 *
	 * @return
	 */
	public OptionPanel createPanel() {
		return this;
	}
	
	/**
	 * Returns the command line option choices that the user has made in each
	 * panel.
	 * 
	 * @return
	 */
	abstract public String getFlags();
	
	/**
	 * Save the current state of this OptionPanel as an XML string that the
	 * IDV serialization code can use.
	 * 
	 * @return 
	 */
	// TODO: if I'm smart about things I may not actually need these.
	abstract public String savePanel();
	
	/**
	 * Create an OptionPanel based on XML data passed in from the IDV 
	 * serialization stuff.
	 * 
	 * @param data The XML that represents the state 
	 */
	// TODO: be smart, try to avoid these guys.
	abstract public void loadPanel(String data);
}