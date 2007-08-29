package edu.wisc.ssec.mcidasv.startupmanager;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JSpinner;

import edu.wisc.ssec.mcidasv.ui.persistbox.PersistBox;

/**
 * 
 * @author Jonathan Beavers, SSEC
 */
public class JavaOptions extends OptionPanel {
	
	private final String[] DEF_HEAP_SIZE = {"64M", "128M", "256M", "512M", "1024M", "2048M"};
	
	// work out default vals
	private XPersistBox heapMaxVals;
	private XPersistBox heapInitVals;

	private JXSpinner stackSize;
	private JXSpinner heapYoungSize;
	
	private StartupManager manager;
	
	public JavaOptions(StartupManager mngr) {
		super("Java VM Options");
		manager = mngr;
	}
	
	/**
	 * @return The panel that holds our Java VM tweaks! Woo.
	 */
	public JavaOptions createPanel() {
		String[] usr1 = {};
		String[] usr2 = {};
		heapMaxVals = new XPersistBox(0, DEF_HEAP_SIZE, usr1);
		heapInitVals = new XPersistBox(0, DEF_HEAP_SIZE, usr2);
		
		stackSize = new JXSpinner();
		heapYoungSize = new JXSpinner();
		
		addNormalOption("Maximum Heap Size:", heapMaxVals); // MB
		addNormalOption("Initial Heap Size:", heapInitVals); // MB
		addAdvancedOption("Thread Stack Size:", stackSize); // KB
		addAdvancedOption("Young Generation Heap Size:", heapYoungSize); // MB
		
		// TODO: work out good spinner models.
		
		return this;
	}
	
	public String getFlags() {
		String flags = new String("");
		
		// code up the polling stuff.
				
		return flags;
	}
	
	public String savePanel() {
		// xml encoder calls that return a big ol string
		return new String("we are devo");
	}
	
	public void loadPanel(String data) {
		System.out.println("are we not men?");
	}
	
	private class XPersistBox extends PersistBox implements CmdInterface {
		public XPersistBox(int idx, String[] defs, String[] user) {
			super(idx, defs, user);
		}
		
		public void processEvent() {
			System.out.println("somethin done happened in a persistbox!");
		}
		
		// the antialias stuff will have to extend itself a bit further...
		public void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D)g;
			g2d.setRenderingHints(StartupManager.getRenderingHints());
			super.paintComponent(g2d);
		}
	}
	
	private class JXSpinner extends JSpinner implements CmdInterface {
		public void processEvent() {
			System.out.println("listener: spinner event");
		}
	}
}