package edu.wisc.ssec.mcidasv.startupmanager;

import java.awt.*;

import java.awt.event.*;

import java.io.*;

import javax.swing.*;

import javax.swing.table.*;

import java.util.*;

import java.util.regex.*;

/**
 * TODO: make sure serialization works
 * 
 * @author Jonathan Beavers, SSEC
 */
public class StartupManagerTable extends JPanel implements ActionListener {
	
	/** The valid options for a McV/IDV bundle. */
	protected final String[] VALID_BUNDLE_OPTIONS = {"Load", "Disable"};
	
	/** The valid options for a McV/IDV plugin. */
	protected final String[] VALID_PLUGIN_OPTIONS = 
		{"Install", "Load", "Disable"};
	
	/** Whether or not the container panel had it's advanced options set. */
	protected boolean advancedEnabled = false;
	
	/** The "Add" button. */
	private AddButton addItem;
		
	/** The "Move Down" button*/
	private MoveButton moveDown;
	
	/** The "Move Up" button. */
	private MoveButton moveUp;
	
	/** The "Remove" button. */
	private RemoveButton removeItem;
	
	/** Handles mouse events that happen in the JTable. */
	private StartupManagerTableMouseHandler mouseHandler;
	
	/** 
	 * Self-reference that comes in handy when binding the different action
	 * buttons to the current table.
	 */
	private StartupManagerTable tableManager = this;
		
	/** 
	 * Shorthand reference that points to one of VALID_BUNDLE_OPTIONS or
	 * VALID_PLUGIN_OPTIONS. Beats having to do tedious if-else stuff. 
	 */
	private String[] optArray;
	
	/** The actual table that the user will poke and prod. */
	protected JTable itemTable;
	
	/** Reference to the model portion of this pseudo-MVC component. */
	protected StartupManagerTableModel tableModel;
	
	/** Contains all of the JButtons that allow table manipulation. */
	private JPanel buttonPanel;
	
	/** A collection of the ListItems popuplating the current table. */
	protected Vector<ListItem> itemVector;
	
	// TODO: This is STUPID.
	protected Dimension viewportSize = new Dimension(500, 100);
	
	/** Handy reference back to the invoking Startup Manager instance. */
	private StartupManager manager;
	
	/** Currently selected row within the table. */
	private int selRow = 0;
	
	/** Currently selected column within the table. */
	private int selCol = 0;
	
	/** 
	 * This is used to tell the ListItems contained in this table what sort of
	 * world they're living in. 
	 */
	private int tableType = 0;
	
	/** 
	 * Create an instance that uses the characteristics of lists or bundles
	 * given a proper type ID. 
	 * 
	 * @param type Either ID_PLUGIN or ID_BUNDLE.
	 */
	public StartupManagerTable(StartupManager mngr, int type) {
		if ((type != ListItem.ID_PLUGIN) && (type != ListItem.ID_BUNDLE))
			throw new IllegalArgumentException();
		
		manager = mngr;
		tableModel = new StartupManagerTableModel();
		mouseHandler = new StartupManagerTableMouseHandler();
		itemVector = new Vector<ListItem>();
		tableType = type;
		
		// point optArray at the proper set of options and we're done.
		if (type == ListItem.ID_PLUGIN)
			optArray = VALID_PLUGIN_OPTIONS;
		else
			optArray = VALID_BUNDLE_OPTIONS;
	}

	/**
	 * Sets up the table so that the contents of the table will be 
	 * displayed properly and capable of handling user interaction. So far
	 * users are able to edit the labels of each plugin or bundle, edit the
	 * way IDV interacts with the plugin or bundle, and provide an entirely
	 * different file for each plugin or bundle.
	 * 
	 * All of this functionality is disabled by default as it could make
	 * the user's plugins and bundles go FUBAR quite easily.
	 * 
	 * @return The newly displayable StartupManagerTable.
	 */
	public StartupManagerTable createDisplayableTable() {

		JComboBox comboBox = new JComboBox(optArray);
		
		JScrollPane pane;

		TableColumn labelColumn;
		TableColumn optionColumn;

		buttonPanel = createButtons();
		itemTable = new JTable(tableModel);

		TableColumnModel colModel = itemTable.getColumnModel();
		
		int labelIdx = tableModel.getColumnIndex(tableModel.ID_LABEL_COL);
		int optIdx = tableModel.getColumnIndex(tableModel.ID_OPTION_COL);
		int pathIdx = tableModel.getColumnIndex(tableModel.ID_PATH_COL);
		
		labelColumn = colModel.getColumn(labelIdx);			
		labelColumn.setCellEditor(new DefaultCellEditor(new JFormattedTextField()));

		optionColumn = colModel.getColumn(optIdx);
		optionColumn.setCellEditor(new DefaultCellEditor(comboBox));
		comboBox.addMouseListener(mouseHandler);
		
		// manipulate the dimension param to adjust stupid box around table.
		itemTable.setPreferredScrollableViewportSize(viewportSize);
		itemTable.addMouseListener(mouseHandler);
		
		pane = new JScrollPane(itemTable);

		setLayout(new BorderLayout());
		add(pane, BorderLayout.NORTH);
		add(buttonPanel, BorderLayout.SOUTH);
		return this;
	}

	public void actionPerformed(ActionEvent e) {
		CmdInterface cmd = (CmdInterface)e.getSource();
		cmd.processEvent();
	}
	
	/**
	 * Toggles whether or not the table is enabled. The JPanel/JComponent
	 * method has to be overridden as it won't enable or disable any child
	 * components (kinda important!). 
	 * 
	 * @param b true if the table should be enabled, false otherwise.
	 */
	public void setEnabled(boolean b) {
		advancedEnabled = b;
		
		// toggle the various editing buttons.
		addItem.setEnabled(b);
		moveDown.setEnabled(b);
		moveUp.setEnabled(b);
		removeItem.setEnabled(b);
		
		// toggle the actual table
		itemTable.setEnabled(b);

		// TODO: update this for other platforms
		// OS X keeps the JComboBox appearance if you disable the advanced
		// options while editing. Using TableCellEditor.stopCellEditing()
		// to explicitly tell Swing to stop editing seems to work.
		if (b == false)
			stopEditing();
	}
	
	
	/**
	 * Some events, like moving the selected row or disabling the advanced
	 * options will result in the JComboBox control sticking around when it
	 * really should not. An easy way to avoid this (buggy) behavior is to
	 * explicitly tell the JTable to stop editing via a call to the option 
	 * column's cell editor, which this function is a wrapper around.
	 */
	// TODO: verify on other platforms
	private void stopEditing() {
		int idx = tableModel.getColumnIndex(tableModel.ID_OPTION_COL);
		TableColumnModel colModel = itemTable.getColumnModel();
		TableColumn optCol = colModel.getColumn(idx);
		TableCellEditor editor = optCol.getCellEditor();
		editor.stopCellEditing();
	}
	
	/**
	 * Returns whether or not the current table is enabled. Tables within
	 * the startup manager are (so far) used only for advanced features,
	 * so they are disabled by default.
	 * 
	 * @return true if the table is enabled, false otherwise.
	 */
	public boolean isEnabled() {
		return advancedEnabled;
	}

	/**
	 * Each table within the startup manager has several buttons that allow
	 * the contents of the table to be manipulated. This method creates the
	 * buttons and preps them to be displayed.
	 * 
	 * @return The panel containing the various editing buttons.
	 */
	private JPanel createButtons() {
		JPanel buttons = new JPanel();
		JPanel ordering = new JPanel();
		JPanel manage = new JPanel();
		
		buttons.setLayout(new BorderLayout());
		
		addItem = new AddButton("Add");
		moveDown = new MoveButton("Move Down", false);
		moveUp = new MoveButton("Move Up", true);
		removeItem = new RemoveButton("Remove");
		
		manage.add(addItem);
		manage.add(removeItem);
		
		ordering.add(moveDown);
		ordering.add(moveUp);
		
		buttons.add(ordering, BorderLayout.EAST);
		buttons.add(manage, BorderLayout.WEST);
		return buttons;
	}
	
	/**
	 * Adds a ListItem object to the current table model instance.
	 * 
	 * @param li The ListItem to add to our table model.
	 */
	public void add(ListItem li) {
		tableModel.addItem(li);
	}
		
	/** 
	 * Pops up a JFileChooser and tries to get the user to select a file.
	 * If no file is selected, return null. Otherwise returns the path to
	 * the selected file. 
	 * 
	 * @return Either null or the path to the selected file.
	 */
	private String getItemPath() {
		JFileChooser jfc = new JFileChooser();
		if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			return jfc.getSelectedFile().getPath();
		else
			return null;
	}
	
	/**
	 * Handles mouse clicks and presses that occur within the JTable, not
	 * the various buttons associated with the JTable.
	 * 
	 * @author Jonathan Beavers, SSEC
	 */
	private class StartupManagerTableMouseHandler extends MouseAdapter {
		
		/**
		 * Handle the mouse button getting pressed on a component. OS X in
		 * particular doesn't seem to register mouse clicks when the 
		 * DefaultCellEditor is a combo box. <code>mousePressed</code> fires 
		 * twice when a combo box has been selected, but this doesn't really
		 * present any problems (so far).
		 * 
		 * @param e 
		 */
		public void mousePressed(MouseEvent e) {
			
			int col = itemTable.getSelectedColumn();
			int row = itemTable.getSelectedRow();
			
			selCol = col;
			selRow = row;
			
			System.out.println("mousePressed:" + row + " " + col);
		}
		
		/**
		 * Handle any mouse clicks that fall within a StartupTableManager.
		 * 
		 * @param e The data that accompanies a mouse click.
		 */
		public void mouseClicked(MouseEvent e) {
			System.out.println("triggered");
			int col = itemTable.getSelectedColumn();
			int row = itemTable.getSelectedRow();

			selCol = col;
			selRow = row;

			// if the mouse click happened in the "Path" column, 
			// I interpret this click as the user wanting a new file.
			if (tableModel.getColumnName(col) == tableModel.ID_PATH_COL) {
				String path = getItemPath();
				System.out.println(path);
				
				if (path != null)
					itemTable.setValueAt(path, row, col);
			}
		}
	}
	//**************** End StartupManagerTableMouseHandler ****************
	
	/**
	 * A lip-service attempt at an MVC design. The model provides an easy API
	 * around the functionality offered by a StartupManagerTable.
	 * 
	 * @author Jonathan Beavers, SSEC
	 */
	private class StartupManagerTableModel extends AbstractTableModel {
		
		/** Defines the ID for the label column. */
		protected final String ID_LABEL_COL = "Label";
		
		/** Defines the ID for the option column. */
		protected final String ID_OPTION_COL = "Option";
		
		/** Defines the ID for the path column. */
		protected final String ID_PATH_COL = "Path";

		/** 
		 * Handy collection of the previous IDs. Note that the ordering of
		 * the IDs in this array is identical to what will be displayed.
		 */
		private final String[] COLUMN_NAMES = {
			ID_OPTION_COL,
			ID_LABEL_COL,
			ID_PATH_COL,
		};
		
		/** The index of the label column within COLUMN_NAMES */
		private int labelColumnIndex = 1;
		
		/** The index of the option column within COLUMN_NAMES */
		private int optionColumnIndex = 0;
		
		/** The index of the path column within COLUMN_NAMES */
		private int pathColumnIndex = 2;
		
		public StartupManagerTableModel() {
			// nothin yet
		}
		
		/**
		 * Obtain the index of the column given by <code>label</code>.
		 * 
		 * @param label The ID of the column whose index we want.
		 * 
		 * @return The index of the column named <code>label</code> or -1
		 *         if <code>label</code> is not a valid column ID.
		 */
		public int getColumnIndex(String label) {
			if (label.startsWith(ID_LABEL_COL))
				return labelColumnIndex;
			else if (label.startsWith(ID_OPTION_COL))
				return optionColumnIndex;
			else if (label.startsWith(ID_PATH_COL))
				return pathColumnIndex;
			else
				return -1;
		}
		
		/**
		 * Returns the number of columns in the model.
		 * 
		 * @return <b><i><u>THE DEATH STAR</u></i></b> object. Watch out!
		 */
		public int getColumnCount() {
			return (COLUMN_NAMES != null) ? COLUMN_NAMES.length : 0 ;
		}
		
		/**
		 * Returns the number of rows in the current 
		 * StartupManagerTableModel.
		 * 
		 * Sun's documentation says that this implementation needs to be
		 * as quick as possible due to repeated calls. The current 
		 * implementation is safe; Java's Vector stores the number of 
		 * elements in a Vector as they are added.
		 * 
		 * @return The number of plugins or bundles stored in the table.
		 */
		public int getRowCount() {
			return (itemVector != null) ? itemVector.size() : 0 ;
		}
		
		/**
		 * Returns the name of the given column according to COLUMN_NAMES.
		 * 
		 * @param column The index of the column whose name we'd like.
		 * 
		 * @return The name that corresponds to the column parameter or an
		 *         empty string if the column did not correspond to a row.
		 */
		public String getColumnName(int column) {
			if ((column < 0) || (column >= COLUMN_NAMES.length))
				return new String("");
			
			return COLUMN_NAMES[column];
		}
					
		/**
		 * It seems reasonable that manipulating how IDV/McV handles 
		 * plugins or bundles by playing around on the FS level is an 
		 * advanced thing. Thus users are not allowed to do so unless they 
		 * have specifically turned on the Startup Manager advanced 
		 * options.
		 * 
		 * I ignore rows and columns as users are either allowed to edit
		 * plugins and bundles or they are not. There's no need to 
		 * determine permissions based upon location.
		 * 
		 * @param row Ignored.
		 * @param col Ignored.
		 * 
		 * @return true if the advanced options have been enabled.
		 */
		public boolean isCellEditable(int row, int col) {
			return advancedEnabled;
		}
		
		/** 
		 * Grab the object at a given location.
		 * 
		 * @param row The row component of the object of interest.
		 * @param col The column component of the same.
		 *
		 * @return The object at the given location.
		 */
		public Object getValueAt(int row, int col) {
			ListItem li = itemVector.get(row);
			switch (col) {
				case 0: return li.getOption();
				case 1: return li.getLabel();
				case 2: return li.getPath();
				default: return null;
			}
		}
		
		/**
		 * Given a row and column, replace the object currently stored in
		 * that "location" with obj.
		 * 
		 * @param obj The replacement object.
		 * @param row The row component of the replacement coordinate.
		 * @param col The column component of the same.
		 */
		public void setValueAt(Object obj, int row, int col) {
			String strRepr = (String)obj;
			ListItem li = itemVector.get(row);
			switch (col) {
				case 0:
					// we're replacing a "use" option. see either 
					// VALID_BUNDLE_OPTIONS or VALID_PLUGIN_OPTIONS.
					li.setOption(strRepr);
					break;

				case 1:
					// re-labeling a given item. this is pretty simple.
					li.setLabel(strRepr);
					break;

				case 2:
					// we're updating the path to a given item.
					li.setPath(strRepr);
					break;

				default:
					// if this happens we can assume that we aren't in 
					// Kansas any longer.
					li.isValid = false;
					break;
			}
			
			fireTableCellUpdated(row, col);
		}
		
		/**
		 * Retrieve the ListItem at the given row/index.
		 * 
		 * @param row The row/index of the ListItem that should be retrieved.
		 * 
		 * @return The desired ListItem.
		 */
		public ListItem getItemAt(int row) {
			return itemVector.get(row);
		}
		
		/**
		 * Remove the ListItem at the given row/index.
		 * 
		 * @param row The row/index of the ListItem that should be removed.
		 * 
		 * @return The removed ListItem.
		 */	
		public ListItem removeItemAt(int row) {
			ListItem li = itemVector.remove(row);
			fireTableDataChanged();
			return li;
		}
		
		/**
		 * Adds a ListItem to the end of the current list and table.
		 * 
		 * @param li The ListItem to be added.
		 */
		public void addItem(ListItem li) {
			itemVector.add(li);
			fireTableDataChanged();
		}
		
		/**
		 * Adds a ListItem at the given index. 
		 * 
		 * @param index The index where <code>li</code> should be placed.
		 * @param li The ListItem to be added.
		 */
		public void addItemAt(int index, ListItem li) {
			itemVector.add(index, li);
			fireTableDataChanged();
		}
		
		/**
		 * Moves a ListItem from one index in the list to another. 
		 * 
		 * @param from The original index/row of the desired ListItem.
		 * @param to The new index/row for the ListItem.
		 */
		public void moveItem(int from, int to) {
			stopEditing();
			ListItem li = itemVector.remove(from);
			itemVector.add(to, li);
			fireTableDataChanged();
			itemTable.setRowSelectionInterval(to, to);
			itemTable.setColumnSelectionInterval(selCol, selCol);
			
			selRow = to;
			
			
		}		
	}
	//******************** End StartupManagerTableModel *******************

	/**
	 * TableActionButton is merely around to satisfy the DRY principle. It 
	 * makes for easy registering of JButtons and action listeners.
	 */
	private class TableActionButton extends JButton {
		public TableActionButton(String label) {
			super(label);
			this.addActionListener(tableManager);
		}
	}
	
	/**
	 * AddButton constructs an empty ListItem and has the user populate it with
	 * values. The new ListItem is then added to the JTable and is ready to be
	 * used by McV/IDV.
	 * 
	 * @author Jonathan Beavers, SSEC
	 */
	private class AddButton extends TableActionButton implements CmdInterface {
		public AddButton(String label) {
			super(label);
		}
		
		/**
		 * Attempts to extract the "name" of a file without any extension. 
		 * For example, say this method is provided /home/test/bigtest.txt.
		 * The string that will be returned is "bigtest". If this method can't
		 * figure out how to extract the desired string, it will merely return
		 * the results from file.<code>getName</code>.
		 * 
		 * @param file The file whose name needs to be extracted.
		 * 
		 * @return See method description.
		 */
		private String extractLabel(File file) {
			if (file == null)
				return new String("nil");
			
			Pattern p = Pattern.compile("(.+)\\..+");
			
			Matcher m = p.matcher(file.getName());
			if (m.matches() == true)
				return m.group(1);
			
			return file.getName();
		}
		
		public void processEvent() {
			String path = getItemPath();
			if (path != null) {
				// extract local name -ext from path
				String label = extractLabel(new File(path));
				
				ListItem li = new ListItem(label, path, "Load", tableType);
				
				tableModel.addItem(li);
			}
				
		}
	}
	
	/**
	 * MoveButton allows the user to manipulate the position of a ListItem 
	 * within a JTable. Depending on how the object was instantiated, a user 
	 * may either move a ListItem up in the list (towards index zero) or 
	 * down (towards tableModel.getRowCount()).
	 * 
	 * @author Jonathan Beavers, SSEC
	 */
	private class MoveButton extends TableActionButton implements CmdInterface {
	
		/** 
		 * If true, this instantiation moves ListItems up, false moves 
		 * ListItems down the JTable. 
		 */
		boolean direction;
		
		/**
		 * Creates a MoveButton that moves ListItems in the given direction.
		 * 
		 * @param label The label text for the JButton.
		 * @param direction If true, this button will move ListItems up in the
		 *                  JTable. If false, the button will move ListItems
		 *                  down within the JTable.
		 */
		public MoveButton(String label, boolean direction) {
			super(label);
			this.direction = direction;
		}
		
		public void processEvent() {
			if (direction == true) {
				if ((selRow - 1) >= 0)
					tableModel.moveItem(selRow, selRow - 1);
			} else {
				if ((selRow + 1) < tableModel.getRowCount())
					tableModel.moveItem(selRow, selRow + 1);
			}
		}		
	}
		
	/**
	 * RemoveButton allows the user to remove the currently selected ListItem
	 * from a JTable. 
	 * 
	 * TODO: should remove delete the plugin or bundle?
	 * 
	 * @author Jonathan Beavers, SSEC
	 */
	class RemoveButton extends TableActionButton implements CmdInterface {
		public RemoveButton(String label) {
			super(label);
		}
		
		public void processEvent() {
			stopEditing();
			tableModel.removeItemAt(selRow);
			// TODO: determine if this should really remove the plugin/bundle
			//       from the FS.
		}
	}
}