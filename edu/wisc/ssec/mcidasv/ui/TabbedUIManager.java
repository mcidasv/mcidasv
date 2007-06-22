package edu.wisc.ssec.mcidasv.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
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


/**
 * A tabbed users interface for McIDAS-V.
 * 
 * <p>There is only a single window to contain all displays. When 
 * <tt>createNewWindow</tt> is called a new tab is added to the
 * display window.
 * </p>
 * 
 * <p>The tabbed interface is a 
 * {@link JTabbedPane}, which is a lightweight component, 
 * as a container for the heavyweight Java3D canvas. The only reason 
 * it works is because the canvases are <tt>directly</tt> on top of 
 * each other. As a consequence of this only displays that contain
 * a single view can be supported.
 * </p>
 * @author Bruce Flynn, SSEC
 * @version $Id$
 */
public class TabbedUIManager extends IdvUIManager {
	
	/**
	 * The only display window.
	 */
	private IdvWindow tabbedWindow;
	/**
	 * Main display container.
	 */
	private JTabbedPane tabbedContent;
	
	/**
	 * The ctor. Just pass along the reference to the idv.
	 * 
	 * @param idv The idv
	 */
	public TabbedUIManager(IntegratedDataViewer idv) {
		super(idv);
	}

	public IdvWindow createNewWindow(List viewManagers, boolean notifyCollab,
			String title, String skinPath, Element skinRoot) {
		try {
			
			// setup window if not already
			if (tabbedWindow == null) {
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
				JComponent contents = GuiUtils.topCenter(toolbar, tabbedContent);
				tabbedWindow.setContents(contents);
			}
			
			if (viewManagers == null) {
				viewManagers = new ArrayList();
			}
			
			MapViewManager viewManager;
			if (viewManagers.size() == 0) {
				viewManager = new MapViewManager(
					getIdv(),
					new ViewDescriptor(), 
					null
				);
				viewManagers.add(viewManager);
			} else {
				viewManager = (MapViewManager) viewManagers.get(0);
			}
			
			getVMManager().addViewManager(viewManager);
			
			// default tab name 
			String tabName = "View " + (tabbedContent.getTabCount() + 1);
			tabbedContent.add(tabName, viewManager.getContents());
		
			// Tell the window what view managers it has.
			tabbedWindow.setTheViewManagers(viewManagers);
			tabbedWindow.pack();
	
			// Show the window if needed
			if (getIdv().okToShowWindows()) {
				tabbedWindow.show();
			}
		
		} catch (Exception e) {
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
		
		// set the tab name to the view name
		if (tabbedWindow != null) {
			int idx = tabbedContent.indexOfComponent(viewManager.getContents());
			tabbedContent.setTitleAt(idx, viewManager.getName());
			
		}
		
	}
	
}
