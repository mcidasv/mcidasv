package edu.wisc.ssec.mcidasv.startupmanager;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;

/**
 * <p>A plain, unassuming little panel that <i>should</i> inform the user about
 * the capabilities of the startup manager.</p>
 * 
 * @author Jonathan Beavers, SSEC
 */
public class AboutOptions extends OptionPanel {
	
	/** Path to the HTML file describing the startup manager. */
	private static final String ABOUT_FILE = 
		"/edu/wisc/ssec/mcidasv/startupmanager/resources/data/about.html";

	/** Path to the McIDAS-V logo image. */
	private static final String LOGO = 
		"/edu/wisc/ssec/mcidasv/images/mcidasv_logo.gif";
	
	private static final String LOAD_ERROR = 
		"<b>An error occured while loading description data file</b>";
	
	/** Holds the contents of ABOUT_FILE. */
	private String panelData;
	
	/**
	 * Create the "About" option panel
	 * 
	 * @param mngr Unused
	 */
	public AboutOptions(StartupManager mngr) {
		super("Information");
		
		panelData = loadPanelData();
	}
	
	/**
	 * Do the actual layout for this option panel.
	 * 
	 * @return This option panel.
	 */
	public AboutOptions createPanel() {
		GridBagConstraints c = new GridBagConstraints();
		JEditorPane editor = new JEditorPane();		
		JLabel label = new JLabel(new ImageIcon(getClass().getResource(LOGO)));		

		c.insets = new Insets(2, 2, 2, 2);
		
		editor.setEditable(false);
		editor.setContentType("text/html");
		editor.setText(panelData);
		editor.setBackground(getBackground());
				
		add(label, c);
		c.gridx++;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1.0f;
		c.insets = new Insets(2, 2, 2, 2);
		
		add(editor,c);
		
		return this;
	}
	
	/**
	 * Read in the data from the file given in ABOUT_FILE.
	 * 
	 * @return The contents of ABOUT_FILE.
	 */
	private String loadPanelData() {
		StringBuffer contents = new StringBuffer();
		InputStream stream = getClass().getResourceAsStream(ABOUT_FILE);
		String line;
		
		try {
			BufferedReader br = 
				new BufferedReader(new InputStreamReader(stream));
			while ((line = br.readLine()) != null)
				contents.append(line);			
		} catch (IOException e) {
			contents.append(LOAD_ERROR);
			e.printStackTrace();
		}
		return contents.toString();
	}
	
	/** 
	 * Nothing to save.
	 */
	public String savePanel() {
		return null;
	}
	
	/**
	 * Nothing to load from a previous state.
	 */
	public void loadPanel(String data) {
		return;
	}
	
	/**
	 * No flags.
	 */
	public String getFlags() {
		return null;
	}	
}
