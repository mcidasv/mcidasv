package edu.wisc.ssec.mcidasv.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * A popup window that attaches itself to a parent and can display an 
 * component without preventing user interaction like a <tt>JComboBox</tt>.
 *   
 * @author <a href="http://www.ssec.wisc.edu/cgi-bin/email_form.cgi?name=Flynn,%20Bruce">Bruce Flynn, SSEC</a>
 *
 */
public class ComponentPopup extends JWindow {

	private static final long serialVersionUID = 7394231585407030118L;

	/**
	 * Get the calculated total screen size.
	 * @return
	 */
	protected static Dimension getScreenSize() {
		GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice gdev = genv.getDefaultScreenDevice();
	    DisplayMode dmode = gdev.getDisplayMode();

	    return new Dimension(dmode.getWidth(), dmode.getHeight());
	}
	
	/**
	 * Do we contain the screen relative point.
	 * @param point Screen relative point.
	 * @return
	 */
	public boolean containsPoint(Component comp, Point point) {
		if (!comp.isVisible()) {
			return false;
		}
		Point my = comp.getLocationOnScreen();
		boolean containsX = point.x > my.x && point.x < my.x + getWidth();
		boolean containsY = point.y > my.y && point.y < my.y + getHeight();
		return containsX && containsY;
	}	
	
	private final MouseAdapter ourMouseAdapter;
	private final MouseAdapter parentMouseAdapter;
	private final ComponentAdapter parentComponentAdapter;
	private Component parent;
	
	/**
	 * Create an instance associated with the given parent.
	 * @param parent The component to attach this instance to.
	 */
	public ComponentPopup(Component parent) {
		ourMouseAdapter = new MouseAdapter() {
			public void mouseExited(MouseEvent evt) {
				PointerInfo info = MouseInfo.getPointerInfo();
				boolean onParent = containsPoint(
					ComponentPopup.this.parent,
					info.getLocation()
				);
				
				if (isVisible() && !onParent) {
					setVisible(false);
				}
			}
		};
		parentMouseAdapter = new MouseAdapter() {
			public void mouseExited(MouseEvent evt) {
				PointerInfo info = MouseInfo.getPointerInfo();
				boolean onComponent = containsPoint(
					ComponentPopup.this,
					info.getLocation()
				);
				if (isVisible() && !onComponent) {
					setVisible(false);
				}
			}
		};
		parentComponentAdapter = new ComponentAdapter(){
			public void componentMoved(ComponentEvent e) {
				
			}
		};
		setParent(parent);
	}
	
	public void setParent(Component comp) {
		if (parent != null) {
			parent.removeMouseListener(parentMouseAdapter);
			parent.removeComponentListener(parentComponentAdapter);
		}
		
		parent = comp;
		parent.addComponentListener(parentComponentAdapter);
		parent.addMouseListener(parentMouseAdapter);
	}
	
	/**
	 * Show this popup above the parent. It is not checked if
	 * the component will fit on the screen.
	 */
	public void showAbove() {
		Point loc = parent.getLocationOnScreen();
		int x = loc.x;
		int y = loc.y - getHeight();
		showPopupAt(x, y);
	}
	
	/**
	 * Show this popup below the parent. It is not checked if
	 * the component will fit on the screen.
	 */
	public void showBelow() {
		Point loc = parent.getLocationOnScreen();
		int x = loc.x;
		int y = loc.y + parent.getHeight();
		showPopupAt(x, y);
	}
	
	/**
	 * Do we fit between the top of the parent and the top edge
	 * of the screen.
	 * @return
	 */
	protected boolean fitsAbove() {
		Point loc = parent.getLocationOnScreen();
		int myH = getHeight();
		return loc.y - myH > 0;
	}
	
	/**
	 * Do we fit between the bottom of the parent and the edge
	 * of the screen.
	 * @return
	 */
	protected boolean fitsBelow() {
		Point loc = parent.getLocationOnScreen();
		Dimension scr = getScreenSize();
		int myH = getHeight();
		return loc.y + parent.getHeight() + myH < scr.height;
	}
	
	/**
	 * Show at the specified X and Y.
	 * @param x
	 * @param y
	 */
	public void showPopupAt(int x, int y) {
		setLocation(x, y);
		setVisible(true);
	}
	
	/**
	 * Show this popup deciding whether to show it above
	 * or below the parent component.
	 */
	public void showPopup() {
		if (fitsBelow()) {
			showBelow();
		} else {
			showAbove();
		}
	}

	protected void addImpl(Component comp, Object constraints, int index) {
		super.addImpl(comp, constraints, index);
		comp.addMouseListener(ourMouseAdapter);
	}
	
	private static void createAndShowGui() {
		
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("ROOT");
        DefaultTreeModel model = new DefaultTreeModel(root);
        JTree tree = new JTree(model);
        tree.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        
        root.add(new DefaultMutableTreeNode("Child 1"));
        root.add(new DefaultMutableTreeNode("Child 2"));
        root.add(new DefaultMutableTreeNode("Child 3"));
        
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandPath(tree.getPathForRow(i));
        }
        
        final JButton button = new JButton("Popup");
        final ComponentPopup cp = new ComponentPopup(button);
        cp.add(tree, BorderLayout.CENTER);
        cp.pack();
        
        button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				cp.showPopup();
			}
        });
        
        JFrame frame = new JFrame("ComponentPopup");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout());
        frame.add(button);
        frame.pack();
        frame.setVisible(true);
	}
	
	public static void main(String[] args) {
		try {
			javax.swing.UIManager.setLookAndFeel(
					javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGui();
			}
		});
	}
	
}
