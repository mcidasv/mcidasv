package edu.wisc.ssec.mcidasv.startupmanager;

import java.util.Hashtable;

// shamelessly stolen from IDV!
public class Store {

	private Hashtable<String, Object> map = new Hashtable<String, Object>();
	
	public Store() {
		
	}
	
	public boolean get(String key, boolean def) {
		Object tmp = map.get(key);
		if (tmp == null)
			return def;
		else
			return (Boolean)tmp;
	}
	
	public String get(String key, String def) {
		Object tmp = map.get(key);
		if (tmp == null)
			return def;
		else
			return (String)tmp;
	}
	
	public int get(String key, int def) {
		Object tmp = map.get(key);
		if (tmp == null)
			return def;
		else
			return (Integer)tmp;
	}
	
	public void put(String key, boolean value) {
		map.put(key, (Boolean)value);
	}
	
	public void put(String key, String value) {
		map.put(key, value);
	}
	
	public void put(String key, int value) {
		map.put(key, (Integer)value);
	}
}
