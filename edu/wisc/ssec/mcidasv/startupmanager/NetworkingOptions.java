package edu.wisc.ssec.mcidasv.startupmanager;

/**
 * 
 * @author Jonathan Beavers, SSEC
 */
public class NetworkingOptions extends OptionPanel {
	
	private StartupManager manager;
	
	public NetworkingOptions(StartupManager mngr) {
		super("Networking Management");
		manager = mngr;
	}
	
	public NetworkingOptions createPanel() {
		return this;
	}
	
	public String getFlags() {
		String flags = new String("");
		
		
		
		return flags;
	}
	
	public String savePanel() {
		return new String("");
	}
	
	public void loadPanel(String data) {
		System.out.println("");
	}
}