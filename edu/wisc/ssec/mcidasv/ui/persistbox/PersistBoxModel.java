package edu.wisc.ssec.mcidasv.ui.persistbox;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

/**
 * <p>This class implements the data model for a {@link PersistBox} object. It
 * is largely the same as {@link ComboBoxModel}, with two differences.</p>
 * 
 * <p>The first difference is that data is first ordered by whether or not it
 * is a default or user-entered value and that the last entry should always be
 * a {@link PersistBoxCustomize} object. These different types of objects are 
 * separated by {@link PersistBoxSeparator} objects.</p>
 * 
 * <p>The second difference is that keyboard navigation support has to be
 * updated to understand the concept of {@link PersistBoxSeparator} objects.
 * Users should not be allowed to select them, but Java's {@link JComboBox} 
 * will allow selection when using the up/down arrows to navigate the contents.
 * The overridden <code>setSelectedItem</code> prevents that.
 * </p>
 * 
 * @author Jonathan Beavers, SSEC
 */
public class PersistBoxModel 
	extends AbstractListModel 
	implements ComboBoxModel 
{
	
	/** Scathing message that'll shame any programmer for being sloppy. */
	protected static final String EMSG_NULL_BOX = "";
	
	/** Handy reference to the current PersistBox. */
	// TODO: check to see about removing.
	private PersistBox currentBox;
	
	/** The index of currentItem. */
	// TODO: I need to be better about updating this.
	private int currentIndex = 0;
	
	/** The currently selected object. */
	private PersistBoxItem currentItem;

	/** An array that'll eventually hold the data for this PersistBox. */
	private PersistBoxItem[] values;

	/**
	 * Creates the model for a {@link PersistBox} and fills the appropriate
	 * sections with the appropriate values.
	 * 
	 * @param box The {@link PersistBox} using this model. 
	 * @param d The model's default contents.
	 * @param u The model's user-specified contents
	 */
	public PersistBoxModel(PersistBox box, String[] d, String[] u) {
	
		// make sure the user isn't a jerk
		// TODO: excess exceptions == bad! fix these!
		if (box == null)
			throw new NullPointerException(EMSG_NULL_BOX);
		if (d == null)
			throw new NullPointerException(PersistBox.EMSG_NULL_DEFAULT);
		if (u == null)
			throw new NullPointerException(PersistBox.EMSG_NULL_USER);
		
		// figure out the size of the PersistBox (again!)
		int maxSize = d.length + u.length + 1;
		maxSize += (d.length > 0) ? 1 : 0;
		maxSize += (u.length > 0) ? 1 : 0;
		
		// passed the tests, now time to populate the values array.
		values = new PersistBoxItem[maxSize];
		currentBox = box;
		int index = 0;

		// strings from "d" array go into the first section [default]
		// if the default section has any values, a PersistSeparator is 
		// placed after it
		if (d.length > 0) {
			for (int i = 0; i < d.length; i++) {
				values[index] = new PersistBoxItem(box, index, d[i]);
				index++;
			}
			values[index] = new PersistBoxSeparator(box, index);
			index++;
		}
		
		// strings from "u" array are for the user section, and things
		// are much the same as the above comment.
		if (u.length > 0) {
			for (int i = 0; i < u.length; i++) {
				values[index] = new PersistBoxItem(box, index, u[i]);
				index++;
			}
			values[index] = new PersistBoxSeparator(box, index);
			index++;
		}
		
		// the last entry in a PersistBox should always be the "Customize..."
		// option, so this is where we'd do that work.
		// TODO: the label for the customize entry should not be hardcoded!
		values[index] = new PersistBoxCustomize(box, index, "Customize...");
	}
	
	/**
	 * @param index The index of the item that we want.
	 * @return The object at the given index. 
	 */
	public Object getElementAt(int index) {
		// TODO: sanity check index
		return values[index];
	}
	
	/**
	 * @return The currently selected object.
	 */
	public Object getSelectedItem() {
		// TODO: sanity checks!
		return currentItem;
	}
	
	/**
	 * @return The number of items contained in the current PersistBox. 
	 */
	public int getSize() {
		// TODO: make sure values isn't null!
		return values.length;
	}
	
	/**
	 * Implementation of the ComboBoxModel method. It simply sets the selected
	 * item. It's behavior is a bit different in that if the user is using 
	 * keyboard navigation PersistBoxSeparators are automatically skipped over.
	 * 
	 * An interesting note: when using the OS X L&F, setSelectedItem() doesn't
	 * get called until *after* the user has made a selection, making this 
	 * method quite useless. Good thing we're using Metal.
	 * 
	 * @param obj The item to be selected.
	 */
	public void setSelectedItem(Object obj) {
		// ensure we're working with the right type of Object. if we're not,
		// there isn't a whole lot we can do about it.
		if (obj instanceof PersistBoxItem) {
			PersistBoxItem pbi = (PersistBoxItem)obj;
			
			// if obj is a separator, we need to automatically skip over it
			// during keyboard navigation. if currentItem is null or not a 
			// separator we can be lazy. Otherwise there is some work to do.
			if (pbi.getType() == PersistBoxItem.ID_SEPARATOR) {
				if (currentItem != null) {					
					int oldIdx = currentItem.getIndex();
					int newIdx = pbi.getIndex();
					
					// if obj is "before" currentItem we have to skip to the
					// entry before obj. (remember obj == separator)
					if ((newIdx < oldIdx) && ((newIdx - 1) >= 0))
						currentItem = values[newIdx - 1];
					
					// similarly if obj is after currentItem we need to skip 
					// to the entry after obj.
					else if ((newIdx > oldIdx) && ((newIdx+1) < values.length))
						currentItem = values[newIdx + 1];						
				} 				
			}
			else {
				// no worries! just update currentItem to reflect the 
				// selection change.
				currentItem = pbi;
			}
			
			// tell java something has happened 
			super.fireContentsChanged(this, -1, -1);
		}
	}
}
