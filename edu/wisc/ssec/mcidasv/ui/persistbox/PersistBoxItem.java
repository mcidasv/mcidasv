package edu.wisc.ssec.mcidasv.ui.persistbox;

import javax.swing.JComponent;

/** 
 * A PersistBoxItem is an item that appears within a {@link PersistBox}. All of
 * the objects that appear within a PersistBox inherit from this class, as it
 * provides the basic functionality needed.  
 * 
 * @author Jonathan Beavers, SSEC
 */
public class PersistBoxItem extends JComponent {

	/** The index of this item within a PersistBox. */
	protected int index;
	
	/** 
	 * What is shown when this item is displayed to the user. Note that this
	 * is not true for {@link PersistBoxSeparator}. {@link PersistBox}'s 
	 * <code>createRenderer</code> will call {@link PersistBoxSeparator}'s 
	 * rendering code to draw a line.
	 */
	protected Object value;
	
	/** Reference back to the {@link PersistBox} containing this item. */
	protected PersistBox box;
	
	/** Whether or not this item is a {@link PersistBoxCustomize} object. */
	protected boolean isCustomize = false;
	
	/** Whether or not this item is a normal PersistBoxItem object. */
	protected boolean isRegular = false;
	
	/** Whether or not this item is a {@link PersistBoxSeparator} object. */
	protected boolean isSeparator = false;

	/** ID for a {@link PersistBoxCustomize} entry. */
	protected static final int ID_CUSTOMIZE = 0xDEADBEEF;

	/** ID for a regular PersistBoxItem entry. */
	protected static final int ID_REGULAR = 0xCAFEBABE;

	/** ID for a {@link PersistBoxSeparator} entry. */
	protected static final int ID_SEPARATOR = 0xDECAFBAD;
	
	/** ID for entries that shouldn't be in a PersistBox. */
	protected static final int ID_UNKNOWN = 0xBADBEEF;
	
	public PersistBoxItem() {}
	
	/**
	 * Create a normal PersistBoxItem. <code>setRegular</code> is called, 
	 * making this safe to use right away.
	 * 
	 * @param box The PersistBox that contains this object.
	 * @param idx The index of this item within a PersistBox.
	 * @param val The value of this item (typically a String).
	 */
	public PersistBoxItem(PersistBox box, int idx, Object val) {
		index = idx;
		value = val;
		
		setRegular();
	}
	
	/**
	 * @return The index of the current item within a PersistBox.
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * @return The value of the current item. Typically a string.
	 */
	public Object getValue() {
		return value;
	}
	
	/**
	 * Shorthand method for setting the type of a PersistBoxItem. Only one of
	 * the parameters should be true when calling this.
	 * 
	 * @param c true if this item is a PersistBoxCustomize instance.
	 * @param r true if this item is a PersistBoxItem instance.
	 * @param s true if this item is a PersistBoxSeparator instance.
	 */
	private void setType(boolean c, boolean r, boolean s) {
		isCustomize = c;
		isRegular = r;
		isSeparator = s;
	}
	
	/** 
	 * Enable the PersistBoxCustomize flag for this instance. I could've done
	 * some tricks with instanceof and friends, but this way is more Java-like. 
	 */
	public void setCustomize() {
		setType(true, false, false);
	}
	
	/**
	 * Enable the PersistBoxItem flag for this instance.
	 */
	public void setRegular() {
		setType(false, true, false);
	}

	/**
	 * Enable the PersistBoxSeparator flag for this instance.
	 */
	public void setSeparator() {
		setType(false, false, true);
	}
		
	/**
	 * Returns the ID that corresponds with the type of the current instance.
	 * 
	 * @return The ID of this object's type.
	 */
	public int getType() {
		if (isCustomize == true)
			return ID_CUSTOMIZE;
		
		if (isRegular == true)
			return ID_REGULAR;
		
		if (isSeparator == true)
			return ID_SEPARATOR;
		
		return ID_UNKNOWN;
	}
}
