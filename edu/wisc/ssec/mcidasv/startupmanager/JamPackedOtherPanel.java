package edu.wisc.ssec.mcidasv.startupmanager;

import javax.swing.JCheckBox;
import javax.swing.JTextField;

public class JamPackedOtherPanel extends OptionPanel {

		
	public JamPackedOtherPanel(StartupManager mngr) {
		super("Panel 2");
	}
	
	public JamPackedOtherPanel createPanel() {
		
		JCheckBox disablePlugins = new JCheckBox();
		JCheckBox clearDefBundle = new JCheckBox();
		JCheckBox noDefBundle = new JCheckBox();
		JCheckBox noUserPrefs = new JCheckBox();
		JCheckBox noGui = new JCheckBox();
		JCheckBox server = new JCheckBox();
		JCheckBox showChooser = new JCheckBox();
		JCheckBox enableDebug = new JCheckBox();
		JCheckBox enableDebugMsg = new JCheckBox();
		JCheckBox enableTrace = new JCheckBox();		
		
		JTextField instancePort = new JTextField();
		JTextField defaultXIdv = new JTextField();
		JTextField idvPropFile = new JTextField();
		JTextField userPath = new JTextField();
		JTextField sitePath = new JTextField();
		JTextField imageFile = new JTextField();
		JTextField movieFile = new JTextField();
		JTextField catalogUrl = new JTextField();
		JTextField collabHost = new JTextField();
		JTextField collabPort = new JTextField();
		
		addNormalOption("Disable Plugins", disablePlugins);
		addNormalOption("Clear Default Bundle", clearDefBundle);
		addNormalOption("Disable Default Bundle", noDefBundle);
		addNormalOption("Don't use user preferences", noUserPrefs);
		addNormalOption("Disable GUI", noGui);
		addNormalOption("Enable Collaboration Mode", server);
		addNormalOption("Show Chooser on start", showChooser);
		addNormalOption("Enable Debugging", enableDebug);
		addNormalOption("Enable Debug Messages", enableDebugMsg);
		addNormalOption("Enable Tracing", enableTrace);
		addNormalOption("Check for McV on port", instancePort);
		addNormalOption("Specify .xidv", defaultXIdv);
		addNormalOption("Specify McV property file", idvPropFile);
		addNormalOption("User Path", userPath);
		addNormalOption("Site Path URL", sitePath);
		addNormalOption("Output to JPEG", imageFile);
		addNormalOption("Output to Quicktime", movieFile);
		addNormalOption("Chooser Catalog URL", catalogUrl);
		addNormalOption("Collaboration Host", collabHost);
		addNormalOption("Collaboration Port", collabPort);
		
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
