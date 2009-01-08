package edu.wisc.ssec.mcidasv;

import com.apple.mrj.*;

class MacMenuManager implements MRJQuitHandler, MRJPrefsHandler, MRJAboutHandler {
	McIDASV us;
	public MacMenuManager(McIDASV myself) {
		us = myself;
//		System.setProperty("com.apple.macos.useScreenMenubar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "McIDAS-V");
		MRJApplicationUtils.registerAboutHandler(this);
		MRJApplicationUtils.registerPrefsHandler(this);
		MRJApplicationUtils.registerQuitHandler(this);
	}

	public void handleQuit() throws IllegalStateException {
		us.getIdv().quit();
//		us.quit();
	}
	
	public void handlePrefs() throws IllegalStateException {
		us.getIdv().showPreferenceManager();
//		us.Prefs();
	}
	
	public void handleAbout() {
		us.getIdv().getIdvUIManager().about();
//		us.About();		
	}
	
}
