package edu.wisc.ssec.mcidasv.ui;

import java.awt.Color;
import java.awt.Container;
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
 * lightweight component, and due to issues mixing the lightweight swing 
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
public class TabbedUIManager extends UIManager {
	
	// FIXME: debug code
	private static Color[] colors = new Color[]{
		Color.BLUE,
		Color.RED,
		Color.GRAY,
		Color.GREEN,
		Color.PINK
	};
	
	/** The only display window. */
	private IdvWindow tabbedWindow;
	/** Main display container. */
	private JTabbedPane tabbedContainer;
	
	/**
	 * Number to assign to the next view added.
	 */
	private int nextViewNumber = 1;
	/**
	 * Mapping of the all view descriptor names to thier <tt>ViewProps</tt>.
	 */
	private Map<String, ViewProps> views = new Hashtable<String, ViewProps>();
	/**
	 * Mapping of tab number to view descriptor name. Provieds constant time access
	 * to a <tt>ViewProps</tt> using a tab index.
	 */
	private Map<Integer, String> tabViews = new Hashtable<Integer, String>();
	
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
	@SuppressWarnings("unchecked")
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
			
			// FIXME: debug code
			viewManager.setColors(Color.WHITE, colors[(nextViewNumber - 1) % 5]);
			
			ViewProps view = new ViewProps(
				nextViewNumber++,
				viewManager
			);
			String descName = viewManager.getViewDescriptor().getName();
			views.put(descName, view);
			addTab(view);
			
			getVMManager().addViewManager(viewManager);
			
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
	
	/*
	 * Overriden to set tab name.
	 * @see ucar.unidata.idv.ui.IdvUIManager#viewManagerChanged(ucar.unidata.idv.ViewManager)
	 */
	public void viewManagerChanged(ViewManager viewManager) {
		super.viewManagerChanged(viewManager);
		
		ViewDescriptor desc = viewManager.getViewDescriptor();
		if (views.containsKey(desc.getName())) {
			ViewProps view = views.get(desc.getName());
			if(view.window != null) {
				view.window.setTitle(getViewTitle(view, true));
			} else {
				int idx = tabbedContainer.indexOfComponent(viewManager.getContents());
				tabbedContainer.setTitleAt(idx, getViewTitle(view));
			}
		}
		
	}

	private void addTab(final ViewProps view) {
		System.err.println("addTab ** " + view);
		String descName = view.vm.getViewDescriptor().getName();
		tabViews.put(tabbedContainer.getTabCount(), descName);
		tabbedContainer.add(getViewTitle(view), makeBlankTab(descName));
	}
	
	/**
	 * Initialize the tabbed window. Tab component is initialized, listeners added
	 * and added to window.
	 * @param title Window title to use.
	 */
	private void initTabbedWindow(final String title) {
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
		tabbedContainer = new JTabbedPane();

		// compensate for Java3D/Swing issue
		tabbedContainer.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				int idx = tabbedContainer.getSelectedIndex();
				System.err.println("stateChanged ** selected idx:"+ idx + " count:"+tabbedContainer.getTabCount());
				if (idx != -1 && tabbedContainer.getTabCount() > 0) {
					showTab(idx);
				}
			}
		});
		
		// double click to 'eject' tab from window
		tabbedContainer.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				// don't extract tab if there's only 1
				if (evt.getClickCount() >= 2) {
					int idx = tabbedContainer.getSelectedIndex();
					if (idx >= 1) {
						tabToWindow(idx);
					}
				}
			}
		});
		
		JComponent contents = GuiUtils.topCenter(toolbar, tabbedContainer);
		tabbedWindow.setContents(contents);
	}
	
	private JPanel makeBlankTab(final String descName) {
		JPanel panel = new JPanel();
		panel.setName(descName);
		return panel;
	}
	
	/**
	 * Show a particular tab.  This will set the components of all the tabs to be an
	 * empty <tt>JPanel</tt> except the one to be shown.
	 * @param idx index of tab to show
	 */
	private void showTab(final int idx) {

		for (int i = 0; i < tabbedContainer.getTabCount(); i++) {
			String dn = tabViews.get(i);
			tabbedContainer.setComponentAt(idx, makeBlankTab(dn));
		}
		
		// show the selected one
		String descName = tabViews.get(idx);
		if (descName != null) {
			ViewProps view = views.get(descName);
			if (view != null) {
				tabbedContainer.setComponentAt(idx, view.contents);
			}
		}
		

	}
	
	/**
	 * Tabify a window.
	 * @param name The name of the window corresponding to the key in the 
	 * 		view/window cache.
	 */
	private void windowToTab(final String descName) {
		showWaitCursor();
		ViewProps view = views.get(descName);
		
		addTab(view);
		
		view.window.dispose();
		view.window = null;
		
		tabbedContainer.setSelectedIndex(0);
		showNormalCursor();
	}
	
	/**
	 * Extract a tab to a window.
	 * @param idx Component index of the tab to extract.
	 */
	private void tabToWindow(final int idx) {
		showWaitCursor();

		IdvWindow window = new IdvWindow(null, getIdv(), false);
		
		// unregister as tab and get view
		String descName = tabViews.get(idx);
		ViewProps view = views.get(descName);
		
		// register and setup window
		view.window = window;
		window.getFrame().setName(descName);
		window.setTitle(getViewTitle(view, true));
		
		window.addWindowListener(new WindowAdapter() {
			// put the window back in a tab
			public void windowIconified(WindowEvent evt) {
				JFrame window = (JFrame) evt.getSource();
				String name = window.getName(); // should be the descriptor name
				windowToTab(name);
			}
			// unmanage/remove the view
			public void windowClosing(WindowEvent evt) {
				JFrame window = (JFrame)evt.getSource();
				String descName = window.getName();
				ViewProps view = views.remove(descName);
			}
		});
		
		// must remove tab _before_ adding to window
		int tabCount = tabbedContainer.getTabCount();
		tabbedContainer.removeTabAt(idx);
		for (int i = idx + 1; i < tabCount; i++) {
			String desc = tabViews.remove(i);
			System.err.println("removed tab "+i+" descName:"+desc);
			tabViews.put(i - 1, desc);
		}
		
		window.getContentPane().add(view.contents);
		
		window.pack();
		
		if (getIdv().okToShowWindows()) {
			window.setVisible(true);
		}
		
		tabbedContainer.setSelectedIndex(0);
		showNormalCursor();
		
	}

    /**
     * Get an appropriate title for a <tt>ViewProps</tt>. 
     * @param view 
     * @return If the associated <tt>ViewManager</tt> does not have a 
     * 		name the name will consist of &quot;View&quot; and the view 
     * 		number, otherwise it's just the name returned by 
     * 		<tt>ViewManager.getName()</tt>.
     */
    private String getViewTitle(final ViewProps view) {
    	return getViewTitle(view, false);
    }
    
    /**
     * Get an appropriate title for a <tt>ViewProps</tt>. 
     * @param view 
     * @param isWindow When true prepend the window title returned from 
     * 		the <tt>StateManager</tt>.
     * @return If the associated <tt>ViewManager</tt> does not have a 
     * 		name the name will consist of &quot;View&quot; and the view 
     * 		number, otherwise it's just the name returned by 
     * 		<tt>ViewManager.getName()</tt>.
     */
    private String getViewTitle(final ViewProps view, final boolean isWindow) {
    	String title = "View " + view.number;
    	if (null != view.vm.getName()) {
    		title = view.vm.getName();
    	}
    	if (isWindow) {
    		title = getStateManager().getTitle() + " - " + title;
    	}
		return title;
    }
    
    class ViewProps {
    	final int number; // 1-based view number, monotonic increasing
    	final ViewManager vm;
    	final Container contents;
    	IdvWindow window; // may be null if in a tab
    	ViewProps(final int number, final ViewManager vm) {
    		super();
    		this.number = number;
    		this.vm = vm;
    		this.contents = vm.getContents();
    	}
    	public String toString() {
    		return "number:"+number+" vm:"+ vm + " window:" + (window == null ? false : true);
    	}
    }
}
