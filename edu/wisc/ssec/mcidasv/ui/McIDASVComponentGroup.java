package edu.wisc.ssec.mcidasv.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.border.BevelBorder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.TransectViewManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ResourceCollection.Resource;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

/**
 * Extends the IDV component groups so that we can intercept clicks for Bruce's
 * tab popup menu and handle drag and drop. It also intercepts ViewManager 
 * creation in order to wrap components in McIDASVComponentHolders rather than
 * IdvComponentHolders. Doing this allows us to associate ViewManagers back to
 * their ComponentHolders, and this functionality is taken advantage of to form
 * the hierarchical names seen in the McIDASVViewPanel.
 */
public class McIDASVComponentGroup extends IdvComponentGroup {

	/** Path to the "close tab" icon in the popup menu. */
	protected static final String ICO_CLOSE = 
		"/edu/wisc/ssec/mcidasv/resources/icons/tabmenu/stop-loads16.png";

	/** Path to the "rename" icon in the popup menu. */
	protected static final String ICO_RENAME = 
		"/edu/wisc/ssec/mcidasv/resources/icons/tabmenu/accessories-text-editor16.png";

	/** Path to the eject icon in the popup menu. */
	protected static final String ICO_UNDOCK = 
		"/edu/wisc/ssec/mcidasv/resources/icons/tabmenu/media-eject16.png";

	/** Action command for destroying a display. */
	private static final String CMD_DISPLAY_DESTROY = "DESTROY_DISPLAY_TAB";

	/** Action command for ejecting a display from a tab. */
	private static final String CMD_DISPLAY_EJECT = "EJECT_TAB";

	/** Action command for renaming a display. */
	private static final String CMD_DISPLAY_RENAME = "RENAME_DISPLAY";

	/** Keep a reference to avoid extraneous calls to <tt>getIdv().</tt> */
	private IntegratedDataViewer idv;

	/** The popup menu for the McV tabbed display interface. */
	private JPopupMenu popup;

	/**
	 * Default constructor for serialization.
	 */
	public McIDASVComponentGroup() {}

	/**
	 * A pretty typical constructor.
	 * 
	 * @param idv The main IDV instance.
	 * @param name Presumably the name of this component group?
	 */
	public McIDASVComponentGroup(IntegratedDataViewer idv, String name) {
		super(idv, name);
		this.idv = idv;
	}

	/**
	 * Create and return the GUI contents. Overridden so that McV can implement
	 * the right click tab menu and draggable tabs.
	 *
	 * @return GUI contents
	 */
	@Override
	public JComponent doMakeContents() {
		JComponent comp = super.doMakeContents();

		popup = doMakeTabMenu();

		// get sneaky and replace the default JTabbedPane with the draggable
		// McV tab code.
		tabbedPane = new DraggableTabbedPane(this);
		tabbedPane.addMouseListener(new TabPopupListener());

		return comp;
	}

	/**
	 * <p>Importing a display control entails adding the control to the 
	 * component group and informing the UI that the control is no longer in 
	 * its own window.</p>
	 * 
	 * <p>Overridden in McV so that the display control is wrapped in a
	 * McIDASVComponentHolder rather than a IdvComponentHolder.</p>
	 * 
	 * @param dc The display control to import.
	 */
	@Override
	public void importDisplayControl(DisplayControlImpl dc) {
		if (dc.getComponentHolder() != null)
			dc.getComponentHolder().removeDisplayControl(dc);

		idv.getIdvUIManager().getViewPanel().removeDisplayControl(dc);
		dc.guiImported();
		addComponent(new McIDASVComponentHolder(idv, dc));
	}

	/**
	 * Basically just creates a McVCompHolder for holding a dynamic skin and
	 * sets the name of the component holder.
	 * 
	 * @param root The XML skin that we'll use.
	 */
	public void makeDynamicSkin(Element root) {
		IdvComponentHolder comp = new McIDASVComponentHolder(idv, root);
		comp.setType(McIDASVComponentHolder.TYPE_DYNAMIC_SKIN);
		comp.setName("doh doh doh");
		addComponent(comp);
	}

	/**
	 * Doesn't do anything for the time being...
	 * 
	 * @param doc 
	 * 
	 * @return XML representation of the contents of this component group.
	 */
	@Override
	public Element createXmlNode(Document doc) {
		//System.err.println("caught createXmlNode");
		Element e = super.createXmlNode(doc);
		//System.err.println(XmlUtil.toString(e));
		//System.err.println("exit createXmlNode");
		return e;
	}

	/**
	 * <p>Handles creation of the component represented by the XML skin at the 
	 * given index.</p>
	 * 
	 * <p>Overridden so that McV can wrap the component in a 
	 * McIDASVComponentHolder.</p>
	 * 
	 * @param index The index of the skin within the skin resource.
	 */
	@Override
	public void makeSkin(int index) {
		XmlResourceCollection skins = idv.getResourceManager().getXmlResources(IdvResourceManager.RSC_SKIN);
		String id = skins.getProperty("skinid", index);
		if (id == null)
			id = skins.get(index).toString();

		IdvComponentHolder comp = new McIDASVComponentHolder(idv, id);
		comp.setType(IdvComponentHolder.TYPE_SKIN);
		comp.setName(skins.getLabel(index));
		addComponent(comp);
	}

	/**
	 * <p>Create a new component whose type will be determined by the contents 
	 * of <tt>what</tt>.</p>
	 * 
	 * <p>Overridden so that McV can wrap up the components in 
	 * McVComponentHolders, which allow McV to map ViewManagers to 
	 * ComponentHolders.</p>
	 *
	 * @param what String that determines what sort of component we create.
	 */
	@Override
	public void makeNew(String what) {
		try {
			ViewManager vm = null;
			ComponentHolder comp = null;
			String property = "showControlLegend=false";
			ViewDescriptor desc = new ViewDescriptor();
			
			// we're only really interested in map, globe, or transect views.
			if (what.equals(IdvUIManager.COMP_MAPVIEW)) {
				vm = new MapViewManager(idv, desc, property);
			}
			else if (what.equals(IdvUIManager.COMP_TRANSECTVIEW)) {
				vm = new TransectViewManager(idv, desc, property);
			}
			else if (what.equals(IdvUIManager.COMP_GLOBEVIEW)) {
				vm = new MapViewManager(idv, desc, property);
				((MapViewManager)vm).setUseGlobeDisplay(true);
			} 
			else {
				// hand off uninteresting things to the IDV
				super.makeNew(what);
				return;
			}

			// make sure we get the component into a mcv component holder,
			// otherwise we won't be able to easily map ViewManagers to 
			// ComponentHolders for the hierarchical names in the ViewPanel.
			idv.getVMManager().addViewManager(vm);
			comp = new McIDASVComponentHolder(idv, vm);

			if (comp != null) {
				addComponent(comp);
				GuiUtils.showComponentInTabs(comp.getContents());
			}

		} catch (Exception exc) {
			LogUtil.logException("Error making new " + what, exc);
		}
	}

	// weirdo bug, but this method will let me change the selected tab to the
	// most recently created componentholder (on Linux and Windows at least).
	//@Override
	//public void redoLayout() {
	//	super.redoLayout();
	//	if (tabbedPane != null) {
	//		// this is REALLY strange. doing this sort of thing results in 
	//		// "invalid drawable" problems... looks like it only happens on OS X...

	//		tabbedPane.setSelectedIndex(tabbedPane.getComponentCount() - 1);
	//	}
	//}

	/**
	 * Create a window title suitable for an application window.
	 * 
	 * @param title window title
	 * 
	 * @return Application title plus the window title.
	 */
	private String makeWindowTitle(final String title) {
		return UIManager.makeTitle(idv.getStateManager().getTitle(), title);
	}

	/**
	 * Create the <tt>JPopupMenu</tt> that will be displayed for a tab.
	 * 
	 * @return Menu initialized with tab options
	 */
	protected JPopupMenu doMakeTabMenu() {
		ActionListener menuListener = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				final String cmd = evt.getActionCommand();

				if (cmd.equals(CMD_DISPLAY_EJECT))
					ejectDisplay(tabbedPane.getSelectedIndex());

				else if (cmd.equals(CMD_DISPLAY_RENAME))
					renameDisplay(tabbedPane.getSelectedIndex());

				else if (cmd.equals(CMD_DISPLAY_DESTROY))
					destroyDisplay(tabbedPane.getSelectedIndex());
			}
		};

		final JPopupMenu popup = new JPopupMenu();
		JMenuItem item;

		URL img = getClass().getResource(ICO_UNDOCK);
		item = new JMenuItem("Undock", new ImageIcon(img));
		item.setActionCommand(CMD_DISPLAY_EJECT);
		item.addActionListener(menuListener);
		popup.add(item);

		img = getClass().getResource(ICO_RENAME);
		item = new JMenuItem("Rename", new ImageIcon(img));
		item.setActionCommand(CMD_DISPLAY_RENAME);
		item.addActionListener(menuListener);
		popup.add(item);

		popup.addSeparator();

		img = getClass().getResource(ICO_CLOSE);
		item = new JMenuItem("Close", new ImageIcon(img));
		item.setActionCommand(CMD_DISPLAY_DESTROY);
		item.addActionListener(menuListener);
		popup.add(item);

		popup.setBorder(new BevelBorder(BevelBorder.RAISED));

		Msg.translateTree(popup);
		return popup;
	}

	/**
	 * Remove the component holder at index <tt>idx</tt>. This method does not
	 * destroy the component holder.
	 * 
	 * @param idx The index of the ejected component holder.
	 * 
	 * @return The component holder that was ejected.
	 */
	private ComponentHolder ejectDisplay(final int idx) {
		return null;
	}

	/**
	 * Prompt the user to change the name of the component holder at index 
	 * <tt>idx</tt>. Nothing happens if the user doesn't enter anything.
	 * 
	 * @param idx Index of the component holder.
	 */
	private void renameDisplay(final int idx) {
		final String title = JOptionPane.showInputDialog(
			IdvWindow.getActiveWindow().getFrame(),
			"Enter new name",
			makeWindowTitle("Rename Tab"),
			JOptionPane.PLAIN_MESSAGE
		);

		if (title == null)
			return;

		final List<ComponentHolder> comps = getDisplayComponents();
		comps.get(idx).setName(title);
		redoLayout();
	}

	/**
	 * Prompts the user to confirm removal of the component holder at index
	 * <tt>idx</tt>. Nothing happens if the user declines.
	 * 
	 * @param idx Index of the component holder.
	 */
	private void destroyDisplay(final int idx) {
		final List<IdvComponentHolder> comps = getDisplayComponents();
		IdvComponentHolder comp = comps.get(idx);
		comp.removeDisplayComponent();
	}

	/**
	 * Remove the component at <tt>index</tt> without forcing the IDV-land
	 * component group to redraw.
	 * 
	 * @param index The index of the component to be removed.
	 * 
	 * @return The removed component.
	 */
	public ComponentHolder quietRemoveComponentAt(final int index) {
		List<ComponentHolder> comps = getDisplayComponents();
		ComponentHolder removed = comps.remove(index);
		removed.setParent(null);
		return removed;
	}

	/**
	 * Adds a component to the end of the list of display components without
	 * forcing the IDV-land code to redraw.
	 * 
	 * @param component The component to add.
	 */
	public int quietAddComponent(ComponentHolder component) {
		List<ComponentHolder> comps = getDisplayComponents();
		if (comps.contains(component))
			comps.remove(component);

		comps.add(component);
		component.setParent(this);
		return comps.indexOf(component);
	}

	/**
	 * Handle pop-up events for tabs.
	 */
	private class TabPopupListener extends MouseAdapter {
		@Override
		public void mouseClicked(final MouseEvent evt) {
			checkPopup(evt);
		}

		@Override
		public void mousePressed(final MouseEvent evt) {
			checkPopup(evt);
		}

		@Override
		public void mouseReleased(final MouseEvent evt) {
			checkPopup(evt);
		}

		private void checkPopup(final MouseEvent evt) {
			if (evt.isPopupTrigger()) {
				// can't close or eject last tab
				// TODO: re-evaluate this
				Component[] comps = popup.getComponents();
				for (Component comp : comps) {
					if (comp instanceof JMenuItem) {
						String cmd = ((JMenuItem) comp).getActionCommand();
						if ((CMD_DISPLAY_DESTROY.equals(cmd) 
								|| CMD_DISPLAY_EJECT.equals(cmd))
								&& tabbedPane.getTabCount() == 1) {
							comp.setEnabled(false);
						} else {
							comp.setEnabled(true);
						}
					}
				}
				popup.show(tabbedPane, evt.getX(), evt.getY());
			}
		}
	}
}
