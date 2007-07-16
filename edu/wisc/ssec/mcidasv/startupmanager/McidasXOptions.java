package edu.wisc.ssec.mcidasv.startupmanager;

/**
 * 
 * @author Jonathan Beavers, SSEC
 */
public class McidasXOptions extends OptionPanel {

	private StartupManager manager;
	
	public McidasXOptions(StartupManager mngr) {
		super("McIDAS X Options");
		manager = mngr;
	}
	
	public String getFlags() {
		String flags = new String("");
		
		return flags;
	}
	
	
	public void loadPanel(String data) {
		System.out.println("McIDASX: load data");
	}
	
	public String savePanel() {
		return new String("McIDASX: save data");
	}
}
