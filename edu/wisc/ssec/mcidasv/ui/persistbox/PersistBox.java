package edu.wisc.ssec.mcidasv.ui.persistbox;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

/**
 * <p>A PersistBox (name perhaps not the best) is a subclass of JComboBox that
 * provides three new features: the insertion of separators that appear as 
 * lines within the combo box, the ability to add new entries into the 
 * combo box via the GUI, and the ability to store and load data to/from disk.
 * </p>
 * 
 * <p>
 * The appearance of a PersistBox is essentially that of a typical JComboBox.
 * However, when the user is selecting an option, there are three sections of
 * options. The first of which are the default options, which should always be
 * sensible defaults offered by the programmer. The second section is 
 * comprimised of the values that the user has entered at one time or another.
 * The third section should merely consist of an option that will create a GUI
 * which allows the user to manipulate the contents of the combo box.
 * </p>
 * 
 * <p><pre>
 * TODO: it might be advisable to have the items entered by a user appear
 *       first. They've been explicitly added, so the user must be 
 *       interested in using them.
 * TODO: There should be a default ordering, but then the user should be able
 *       to order things however they like.
 * TODO: choose one of the above! If the latter, the sensible default ordering
 *       is the current ordering. That way when users add values they are added
 *       to the end of the list, which seems like standard behavior.
 * </pre></p>
 * @author Jonathan Beavers, SSEC
 */
public class PersistBox extends JComboBox implements ActionListener {

	// The following booleans are useful when deserializing as they ensure that
	// initComboBox() only initializes a PersistBox upon being given all 
	// available data.
	
	/** Whether or not setDefaultValues() has been called. */
	private boolean setDefault = false;
	
	/** Whether or not setCurrentIndex() has been called. */
	private boolean setIndex = false;
	
	/** Whether or not setUserValues() has been called. */
	private boolean setUser = false;

	/** */
	private ComboBoxModel boxModel;
	
	/** */
	protected int currentIndex = 0; // TODO: need to make good use of this
	
	/** Exception message for null default values. */
	protected static final String EMSG_NULL_DEFAULT = "";
	
	/** Exception message for null user values. */
	protected static final String EMSG_NULL_USER = "";
	
	/** Exception message for providing an index that exceeds amount of data. */
	protected static final String EMSG_IDX_TOO_BIG = "";
	
	/** Exception message for an index less than zero. */
	protected static final String EMSG_IDX_TOO_LOW = "";
	
	/** The strings that fall within the "default" section of a PersistBox. */
	protected String[] defaultValues;
	
	/** The strings that fall within the "user" section of a PersistBox. */
	protected String[] userValues;
	
	/**
	 * Creates a PersistBox instance, but that's it. This constructor is only
	 * used when deserializing from the IDV XmlEncoder output, and so we have
	 * to wait until XmlDecoder fills the appropriate fields with values before
	 * we can prepare to be displayed.
	 */
	public PersistBox() {}
	
	/**
	 * Creates a PersistBox that can go ahead and prepare itself for being
	 * displayed.
	 * 
	 * @param idx The index that will be selected upon initialization.
	 * @param def Default values.
	 * @param user User values.
	 */
	public PersistBox(int idx, String[] def, String[] user) {
		if (idx < 0)
			throw new IllegalArgumentException(EMSG_IDX_TOO_LOW);
		if (def == null)
			throw new NullPointerException(EMSG_NULL_DEFAULT);
		if (user == null)
			throw new NullPointerException(EMSG_NULL_USER);

		// ensure that idx fits within data bounds
		int maxSize = def.length + user.length + 1;
		maxSize += (def.length > 0) ? 1 : 0;
		maxSize += (user.length > 0) ? 1 : 0;

		if (idx >= maxSize)
			throw new IllegalArgumentException(EMSG_IDX_TOO_BIG);

		// whew! passed the tests. init everything and go.
		currentIndex = idx;
		defaultValues = def;
		userValues = user;

		setDefault = true;
		setIndex = true;
		setUser = true;

		initPersistBox();
		addActionListener(this);
	}

	/**
	 * Preps the current PersistBox instance to be displayed. If the non-empty
	 * constructor is called, initPersistBox will initialize immediately, as
	 * all of the required information is available. If the empty constructor
	 * has been used (likely due to deserializing), initPersistBox will not
	 * do anything until <code>setDefaultValues</code>, 
	 * <code>setCurrentIndex</code>, and <code>setUserValues</code> have all 
	 * been called.
	 */
	private void initPersistBox() {
		if ((setDefault == true) && (setIndex == true) && (setUser == true)) {
			boxModel = createModel();
			
			setModel(boxModel);
			setSelectedIndex(currentIndex);
			setRenderer(createRenderer());
			
			setDefault = false;
			setIndex = false;
			setUser = false;
		}
	}
	
	/**
	 * @return returns a PersistBoxModel.
	 */
	private ComboBoxModel createModel() {
		return new PersistBoxModel(this, defaultValues, userValues);
	}
	
	/**
	 * Create and return a custom renderer, which is mostly custom due to the
	 * existence of PersistBoxSeparator.
	 * 
	 * @return The renderer Java/Swing should use for a PersistBox. 
	 */
	private ListCellRenderer createRenderer() {
		return new DefaultListCellRenderer() {
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				
				if (value.getClass() == PersistBoxSeparator.class)
					return (PersistBoxSeparator)value;
				else if (value instanceof PersistBoxItem)
					value = ((PersistBoxItem)value).getValue();
				
				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		};
	}

	/**
	 * Handle actions that occur within a PersistBox.
	 * 
	 * @param e The event that needs handling.
	 */
	public void actionPerformed(ActionEvent e) {
		PersistBox box = (PersistBox)e.getSource();		
		PersistBoxItem item = (PersistBoxItem)box.getSelectedItem();
		
		if (item.getType() == PersistBoxItem.ID_CUSTOMIZE) {
			PersistBoxCustomize cust = (PersistBoxCustomize)item;
			cust.showEditor();
			System.out.println("action performed on customize object");
		}
		else {
			System.out.println("action performed on normal object");
		}

	}	
	
	/**
	 * This is mainly useful when deserializing from XmlEncoder. It recovers 
	 * the index of a previous PersistBox selection.
	 * 
	 * @param idx The index of the selection.
	 */
	public void setCurrentIndex(int idx) {
		// TODO: sanity checks
		currentIndex = idx;
		setIndex = true;
		
		initPersistBox();
	}

	/**
	 * Populates the default section of a PersistBox with values. Note that 
	 * this method will call <code>initPersistBox</code> in an attempt to
	 * initialize the current instantiation (if we've been given enough data).
	 * 
	 * @param vals The strings to populate the default section with.
	 */
	public void setDefaultValues(String[] vals) {
		defaultValues = vals;
		setDefault = true;
		
		initPersistBox();
	}

	/**
	 * See the description of <code>setDefaultValues</code>.
	 * 
	 * @param vals The strings to populate the user section with.
	 */
	public void setUserValues(String[] vals) {
		userValues = vals;
	}
	
	/**
	 * @return The index of the current selection.
	 */
	public int getCurrentIndex() {
		return currentIndex;
	}
	
	/**
	 * @return The values currently stored within the default section.
	 */
	public String[] getDefaultValues() {
		return defaultValues;
	}

	/**
	 * @return The values currently stored within the user section.
	 */
	public String[] getUserValues() {
		return userValues;
	}
	
	/**
	 * Handy main method for testing out code changes.
	 * 
	 * @param args Ignored.
	 */
	public static void main(String[] args) {
		String[] d = {"DEF 1", "DEF 2"};
		String[] u = {"USR 1", "USR 2"};
		
		// force the metal look and feel.
		try {
			String laf = UIManager.getCrossPlatformLookAndFeelClassName();
			UIManager.setLookAndFeel(laf);
		} catch (Exception e) {
			e.printStackTrace();
		}

		PersistBox box = new PersistBox(1, d, u);

		JFrame f = new JFrame();
		f.getContentPane().setLayout(new FlowLayout());
		f.getContentPane().add(new JLabel("Test View:"));
		f.getContentPane().add(box);
		
		f.pack();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
	}
}

