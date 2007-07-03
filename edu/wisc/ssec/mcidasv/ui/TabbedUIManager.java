package edu.wisc.ssec.mcidasv.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.w3c.dom.Element;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.util.GuiUtils;
import visad.VisADException;


/**
 * A tabbed users interface for McIDAS-V.
 * 
 * <p>There is only a single window to contain all displays. When 
 * <tt>createNewWindow</tt> is called a new tab is added to the
 * display window. A tab may be 'ejected' from the main window and 
 * placed in a stand-alone window by double clicking on the tab. To
 * re-dock the view click the minimize window button and click the
 * close window button to close it.
 * </p>
 * 
 * <p>FIXME: The tabbed interface is a {@link JTabbedPane}, which is a 
 * lightweight component, Due to issues mixing the lightweight swing 
 * components with the heavyweight Java3D canvases code was added to 
 * replace all views in the tabs with empty JPanels when they are not being 
 * displayed. This is pretty hackish, but works. Additionally, because 
 * when you add a view it gets added behind the other tabs we initially 
 * add an empty JPanel.
 * </p>
 * 
 * @see <a href="http://java3d.j3d.org/tutorials/quick_fix/swing.html">
 *          Integrating Java 3D and Swing
 *      </a>
 *      
 * @author Bruce Flynn, SSEC
 * @version $Id$
 */
public class TabbedUIManager extends IdvUIManager {
	
	/** The only display window. */
	private IdvWindow tabbedWindow;
	/** Main display container. */
	private JTabbedPane tabbedContent;
	
	/** Mapping of views to tab components. */
	private List<ViewManager> viewToTabs = new ArrayList<ViewManager>();
	private Map<String, ViewManager> viewToWindow = new Hashtable<String, ViewManager>();
	
	/**
	 * The ctor. Just pass along the reference to the idv.
	 * 
	 * @param idv The idv
	 */
	public TabbedUIManager(IntegratedDataViewer idv) {
		super(idv);
	}

	/**
	 * Overridden to force windows to appear as tabs.
	 * 
	 * @see ucar.unidata.idv.ui.IdvUIManager#createNewWindow(java.util.List, boolean, java.lang.String, java.lang.String, org.w3c.dom.Element)
	 */
	public IdvWindow createNewWindow(List viewManagers, boolean notifyCollab,
			String title, String skinPath, Element skinRoot) {
		try {
			
			// setup window if not already
			if (tabbedWindow == null) {
				String idvTitle = getIdv().getStateManager().getTitle();
				if (title == null) {
					initTabbedWindow(idvTitle);
				} else {
					initTabbedWindow(title);
				}
			}
			
			if (viewManagers == null) {
				viewManagers = new ArrayList();
			}
			
			ViewManager viewManager;
			if (viewManagers.size() == 0) {
				viewManager = new MapViewManager(
					getIdv(),
					new ViewDescriptor(), 
					null
				);
				viewManagers.add(viewManager);
			} else {
				viewManager = (ViewManager) viewManagers.get(0);
			}
			
			viewToTabs.add(viewManager);
			
			getVMManager().addViewManager(viewManager);
			addTab();
			
			// Tell the window what view managers it has.
			tabbedWindow.setTheViewManagers(viewManagers);
			tabbedWindow.pack();
	
			// Show the window if needed
			if (getIdv().okToShowWindows()) {
				tabbedWindow.show();
			}
		
		} catch (RemoteException e) {
			logException("Adding Tabbed View Manager", e);
			return null;
		} catch (VisADException e) {
			logException("Adding Tabbed View Manager", e);
			return null;
		}
		
		return tabbedWindow;
		
	}
	

	/**
	 * Add a tab to the tab component. An empty panel is added due
	 * to the reasons outlined in the note above.
	 */
	private void addTab() {
		String tabName = "View " + (tabbedContent.getTabCount() + 1);
		// see note in doc comment
		tabbedContent.add(
			tabName, 
			new JPanel()
		);
	}
	
	/**
	 * Initialize the tabbed window. Tab component is initialized, listeners added
	 * and added to window.
	 * @param title Window title to use.
	 */
	private void initTabbedWindow(String title) {
		tabbedWindow = new IdvWindow(
			title,
			getIdv(), 
			true
		);
		
		JMenuBar menuBar = doMakeMenuBar();
		if (menuBar != null) {
			tabbedWindow.setJMenuBar(menuBar);
		}
		JComponent toolbar = getToolbarUI();
		tabbedContent = new JTabbedPane();

		// FIXME: compensate for Java3D/Swing issue
		tabbedContent.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				int idx = tabbedContent.getSelectedIndex();
				showTab(idx);
			}
		});
		
		// double click to 'eject' tab from window
		tabbedContent.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				// don't extract tab if there's only 1
				if (evt.getClickCount() >= 2 && viewToTabs.size() > 1) {
					int idx = tabbedContent.getSelectedIndex();
					tabToWindow(idx);
				}
			}
		});
		
		JComponent contents = GuiUtils.topCenter(toolbar, tabbedContent);
		tabbedWindow.setContents(contents);
	}
	
	private void showTab(int idx) {
		for (int i = 0; i < tabbedContent.getTabCount(); i++) {
			tabbedContent.setComponentAt(i, new JPanel());
		}
		ViewManager vm = (ViewManager) getVMManager().getViewManagers().get(idx);
		tabbedContent.setComponentAt(idx, vm.getContents());		
	}
	
	/**
	 * Tabify a window.
	 * @param name The name of the window corresponding to the key in the 
	 * 		view/window cache.
	 */
	private void windowToTab(String name) {
		ViewManager vm = viewToWindow.remove(name);
		if (vm == null) {
			System.err.println("Null view manager: " + name);
			return;
		}
		String tabName = vm.getName();
		if (tabName == null) {
			tabName = "View " + (tabbedContent.getTabCount() + 1);
		}
		if (tabbedContent.getTabCount() == 0) {
			tabbedContent.add(tabName, vm.getContents());
		} else {
			tabbedContent.add(tabName, new JPanel());
		}
		viewToTabs.add(vm);
		showTab(0);
	}
	
	/**
	 * Extract a tab to a window.
	 * @param idx Component index of the tab to extract.
	 */
	private void tabToWindow(int idx) {
//		IdvWindow window = new IdvWindow(
//			null,
//			getIdv(),
//			false
//		);
		
		tabbedContent.remove(idx);
		
		JFrame frame = new JFrame();
		
		ViewManager vm = viewToTabs.remove(idx);
		ViewDescriptor desc = vm.getViewDescriptor();
		viewToWindow.put(desc.getName(), vm);
		
		frame.setName(desc.getName());
		frame.addWindowListener(new WindowAdapter() {
			public void windowIconified(WindowEvent evt) {
				JFrame window = (JFrame)evt.getSource();
				String name = window.getName();
				windowToTab(name);
			}
			public void windowClosing(WindowEvent evt) {
				JFrame window = (JFrame)evt.getSource();
				String name = window.getName();
				viewToWindow.remove(name);
			}
		});
		
		frame.getContentPane().add(vm.getContents());
		frame.pack();
		if (getIdv().okToShowWindows()) {
			frame.setVisible(true);
		}
	}

    /*
     * Overriden to set tab name.
     * @see ucar.unidata.idv.ui.IdvUIManager#viewManagerChanged(ucar.unidata.idv.ViewManager)
     */
    public void viewManagerChanged(ViewManager viewManager) {
		super.viewManagerChanged(viewManager);
		
		// set the tab name to the view name
		if (tabbedWindow != null) {
			int idx = tabbedContent.indexOfComponent(viewManager.getContents());
			tabbedContent.setTitleAt(idx, viewManager.getName());
		}
		
	}
	
}
