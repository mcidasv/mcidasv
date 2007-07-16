package edu.wisc.ssec.mcidasv.ui.persistbox;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;

/**
 * <p>A PersistBoxCustomize object is much like a PersistBoxItem or 
 * PersistBoxSeparator object in that it is designed to appear as an entry 
 * within a PersistBox.</p> 
 * 
 * <p>When the user selects a PersistBoxCustomize object, an editor containing 
 * the values of the associated PersistBox appears and allows users to 
 * add/edit/remove entries from the PersistBox.</p>
 * 
 * @author Jonathan Beavers, SSEC
 */
public class PersistBoxCustomize extends PersistBoxItem {

	/** The text that appears as a header within the editor. */
	private String dialogText;
	private JFrame frame;
	private PersistBoxEditor editor;
	
	/**
	 * Creates a PersistBoxItem that will, when selected, pop up a dialog 
	 * asking the user to edit the contents of the PersistBox. 
	 * {@link PersistBoxItem}'s <code>setCustomize</code> method is called so 
	 * that things are good to go upon calling the constructor.
	 * 
	 * @param box The {@link PersistBox} associated with this object.
	 * @param idx The index of this instance within the PersistBox.
	 * @param text The text that will appear inside the PersistBox.
	 */
	public PersistBoxCustomize(PersistBox box, int idx, String text) {
		index = idx;
		value = text;
		this.box = box;
		
		setCustomize();
	}
	
	/**
	 * @return The text that will appear inside the Customize dialog box.
	 */
	public String getDialogText() {
		return dialogText;
	}
	
	/**
	 * @param val The text that appears inside the Customize dialog box!
	 */
	public void setDialogText(String val) {
		dialogText = val;
	}

	/**
	 * Easy call to display the combo box editor.
	 */
	public void showEditor() {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				frame = new JFrame("test");
				
				editor = new PersistBoxEditor(frame);
				editor.setOpaque(true);				
				
				frame.setContentPane(editor);
				frame.pack();
				frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
				frame.setVisible(true);
			}
		});
	}
	
	public void closeEditor() {
		System.out.println("whiskey tango foxtrot");
		frame.dispose();
	}

	private class PersistBoxEditor extends JPanel {
		protected JFrame frame;
		protected PersistBoxDialog dialog;
		
		protected JScrollPane scrollPane;
		protected String[] colNames = {"Type", "Value"};
		
		protected String LABEL_TYPE_DEFAULT = "System";
		protected String LABEL_TYPE_USER = "User";
		protected String LABEL_BTN_ADD = "Add";
		protected String LABEL_BTN_REMOVE = "Remove";
		protected String LABEL_BTN_UP = "Move Up";
		protected String LABEL_BTN_DOWN = "Move Down";				
		
		public PersistBoxEditor(JFrame frame) {
			super(new BorderLayout());
			this.frame = frame;
			dialog = new PersistBoxDialog(frame, this, loadData());
			dialog.pack();
		}
		
		private String[][] loadData() {
			String[] defVals = box.getDefaultValues();
			String[] userVals = box.getUserValues();
			
			int itemCount = defVals.length + userVals.length;
			String[][] data = new String[itemCount][2];
			int index = 0;
			
			for (int i = 0; i < defVals.length; i++) {
				data[index][0] = LABEL_TYPE_DEFAULT;
				data[index][1] = defVals[i];
				index++;
			}
			
			for (int i = 0; i < userVals.length; i++) {
				data[index][0] = LABEL_TYPE_USER;
				data[index][1] = userVals[i];
				index++;
			}
			
			return data;
		}
	}
	
	private class PersistBoxDialog extends JDialog implements ActionListener {
		protected PersistBoxEditor editor;
		protected JScrollPane scrollPane;
		protected String[] colNames = {"Type", "Value"};
		
		
		public PersistBoxDialog(Frame frame, PersistBoxEditor editor, Object[][] data) {
			super(frame, true);
			this.editor = editor;
			setTitle("SUPER TITLE");
			
			setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					//setVisible(false);
					//editor.setVisible(false);
					cancel();
					closeEditor();
				}
			});
			
			JTable table = new JTable(data, colNames);
			scrollPane = new JScrollPane(table);
			table.setPreferredScrollableViewportSize(new Dimension(500, 70));
			setContentPane(scrollPane);
			setVisible(true);

		}
		
		private void cancel() {
			scrollPane.setVisible(false);
			dispose();
		}
		
		public void actionPerformed(ActionEvent e) {
			System.out.println("caught action event");
		}
	}
	
	
	/**
	 * <p>A PersistBoxEditor allows users to edit the contents of a 
	 * {@link PersistBox} that have been associated with a 
	 * {@link PersistBoxCustomize} object.</p>
	 * 
	 * <p><pre>
	 * TODO: determine how much control users should have over the default 
	 *       options.
	 * </pre></p>
	 * 
	 * @author Jonathan Beavers, SSEC
	 */
	
	private class PersistBoxEditor2 extends JDialog {

		public PersistBoxEditor2(Dialog frame, String title) {
			super(frame, title, true);
			createEditor();
		}
		
		public void closeDialog() {
			dispose();
			super.dispose();
			//frame.setVisible(false);
			frame.dispose();
		}
		
		private void createEditor() {
			setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					System.out.println("do something!");
					closeDialog();
					
				}
			});
			
			String[][] data = loadData();
			
			JPanel panel = new JPanel(new BorderLayout());
			JTable table = new JTable(data, colNames);
			JScrollPane pane = new JScrollPane(table);
			table.setPreferredScrollableViewportSize(new Dimension(500, 70));
			
			panel.add(pane, BorderLayout.NORTH);
			panel.add(createButtons(), BorderLayout.SOUTH);
			setContentPane(panel);
			setVisible(true);
		}
		
		private JPanel createButtons() {
			JPanel buttons = new JPanel();
			JPanel ordering = new JPanel();
			JPanel manage = new JPanel();
			
			buttons.setLayout(new BorderLayout());
			
			JButton addButton = new JButton(LABEL_BTN_ADD);
			JButton removeButton = new JButton(LABEL_BTN_REMOVE);
			JButton upButton = new JButton(LABEL_BTN_UP);
			JButton downButton = new JButton(LABEL_BTN_DOWN);
			
			// TODO: totally forget about reordering.
			ordering.add(upButton);
			ordering.add(downButton);
			
			manage.add(addButton);
			manage.add(removeButton);
			
			buttons.add(ordering, BorderLayout.EAST);
			buttons.add(manage, BorderLayout.WEST);
			
			return buttons;
		}
		
		private String[][] loadData() {
			String[] defVals = box.getDefaultValues();
			String[] userVals = box.getUserValues();
			
			int itemCount = defVals.length + userVals.length;
			String[][] data = new String[itemCount][2];
			int index = 0;
			
			for (int i = 0; i < defVals.length; i++) {
				data[index][0] = LABEL_TYPE_DEFAULT;
				data[index][1] = defVals[i];
				index++;
			}
			
			for (int i = 0; i < userVals.length; i++) {
				data[index][0] = LABEL_TYPE_USER;
				data[index][1] = userVals[i];
				index++;
			}
			
			return data;
		}
		
		protected JScrollPane scrollPane;
		protected String[] colNames = {"Type", "Value"};
		
		protected String LABEL_TYPE_DEFAULT = "System";
		protected String LABEL_TYPE_USER = "User";
		protected String LABEL_BTN_ADD = "Add";
		protected String LABEL_BTN_REMOVE = "Remove";
		protected String LABEL_BTN_UP = "Move Up";
		protected String LABEL_BTN_DOWN = "Move Down";		
	}
}
