package edu.wisc.ssec.mcidasv;

import java.awt.Insets;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.wisc.ssec.mcidasv.ui.McvComponentGroup;
import edu.wisc.ssec.mcidasv.ui.McvComponentHolder;
import edu.wisc.ssec.mcidasv.ui.UIManager;
import edu.wisc.ssec.mcidasv.util.CompGroups;

import ucar.unidata.data.DataSource;
import ucar.unidata.idv.ArgsManager;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.IdvManager;
import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.LoadBundleDialog;
import ucar.unidata.idv.ui.WindowInfo;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.xml.XmlUtil;

/**
 * <p>McIDAS-V has 99 problems, and bundles are several of 'em. Since the UI of
 * alpha 10 relies upon component groups and McIDAS-V needs to support IDV and
 * bundles prior to alpha 10, we must add facilities for coping with bundles
 * that may not contain component groups. Here's a list of the issues and how
 * they are resolved:</p> 
 * 
 * <p><ul>
 * <li>Bundles prior to alpha 9 use the <code>TabbedUIManager</code>. Each tab
 * is, internally, an IDV window. This is reflected in the contents of bundles,
 * so the IDV wants to create a new window for each tab upon loading. Alpha 10
 * allows the user to force bundles to only create one window. This work is
 * done in {@link #injectComponentGroups(List)}.</li>
 * 
 * <li>The IDV allows users to save bundles that contain <i>both</i> 
 * {@link ucar.unidata.idv.ViewManager}s with component groups and without! 
 * This is actually only a problem when limiting the windows; 
 * <code>injectComponentGroups</code> has to wrap ViewManagers without 
 * component groups in dynamic skins. These ViewManagers must be removed 
 * from the bundle's internal list of ViewManagers, as they don't exist until
 * the dynamic skin is built. <i>Do not simply clear the list!</i> The 
 * ViewManagers within component groups must appear in it, otherwise the IDV
 * does not add them to the {@link ucar.unidata.idv.VMManager}. If limiting 
 * windows is off, everything will be caught properly by the unpersisting 
 * facilities in {@link edu.wisc.ssec.mcidasv.ui.UIManager}.</li>
 * 
 * @see ucar.unidata.idv.IdvPersistenceManager
 * @see edu.wisc.ssec.mcidasv.ui.UIManager
 */
public class PersistenceManager extends IdvPersistenceManager {

	static ucar.unidata.util.LogUtil.LogCategory log_ =
		ucar.unidata.util.LogUtil.getLogInstance(IdvManager.class.getName());
	
	/**
	 * Java requires this constructor. 
	 */
	public PersistenceManager() {
		super(null);
	}

	/**
	 * @see ucar.unidata.idv.IdvPersistenceManager#PersistenceManager(IntegratedDataViewer)
	 */
	public PersistenceManager(IntegratedDataViewer idv) {
		super(idv);
	}

	/**
	 * <p>Overridden so that McIDAS-V can redirect to the version of this 
	 * method that supports limiting the number of new windows.</p>
	 * 
	 * @see #decodeXml(String, boolean, String, String, boolean, boolean, List, boolean, boolean, boolean)
	 */
	@Override public void decodeXml(String xml, boolean fromCollab, 
									String xmlFile, String label, 
									boolean showDialog, boolean shouldMerge,
									List overrideTimes, boolean removeAll, 
									boolean letUserChangeData) {

		decodeXml(xml, fromCollab, xmlFile, label, showDialog, shouldMerge, 
				  overrideTimes, removeAll, letUserChangeData, false);
	}

	/**
	 * <p>Hijacks control of the IDV's bundle loading facilities. Due to the 
	 * way versions of McIDAS-V prior to alpha 10 handled tabs, the user will
	 * end up with a new window for each tab in the bundle. McIDAS-V alpha 10
	 * has the ability to only create one new window and have everything else
	 * go into that window's tabs.</p>
	 * 
	 * @see ucar.unidata.idv.IdvPersistenceManager#decodeXmlFile(String, String, boolean, boolean, List)
	 * @see #decodeXml(String, boolean, String, String, boolean, boolean, List, boolean, boolean, boolean)
	 */
	@Override public boolean decodeXmlFile(String xmlFile, String label,
								 boolean checkToRemove,
								 boolean letUserChangeData,
								 List overrideTimes) {

		String name = ((label != null) ? label : IOUtil.getFileTail(xmlFile));

		boolean shouldMerge = getStore().get(PREF_OPEN_MERGE, true);

		boolean removeAll   = false;

		boolean limitNewWindows = false;

		if (checkToRemove) {
			// ok[0] = did the user press cancel 
			// ok[1] = should we remove
			// ok[3] = should we limit the number of new windows?
			boolean[] ok =
				getPreferenceManager().getDoRemoveBeforeOpening(name);

			if (!ok[0])
				return false;

			if (ok[1]) {
				// Remove the displays first because, if we remove the data 
				// some state can get cleared that might be accessed from a 
				// timeChanged on the unremoved displays
				getIdv().removeAllDisplays();
				// Then remove the data
				getIdv().removeAllDataSources();
				removeAll = true;
			}
			shouldMerge = ok[2];

			if (ok.length == 4)
				limitNewWindows = ok[3];
		}

		String bundleContents = null;
		try {
			//Is this a zip file
			//            System.err.println ("file "  + xmlFile);
			if (ArgsManager.isZidvFile(xmlFile)) {
				//                System.err.println (" is zidv");
				boolean ask   = getStore().get(PREF_ZIDV_ASK, true);
				boolean toTmp = getStore().get(PREF_ZIDV_SAVETOTMP, true);
				String  dir   = getStore().get(PREF_ZIDV_DIRECTORY, "");
				if (ask || ((dir.length() == 0) && !toTmp)) {

					JCheckBox askCbx = 
						new JCheckBox("Don't show this again", !ask);

					JRadioButton tmpBtn =
						new JRadioButton("Write to temporary directory", toTmp);

					JRadioButton dirBtn = 
						new JRadioButton("Write to:", !toTmp);

					GuiUtils.buttonGroup(tmpBtn, dirBtn);
					JTextField dirFld = new JTextField(dir, 30);
					JComponent dirComp = GuiUtils.centerRight(
											dirFld,
											GuiUtils.makeFileBrowseButton(
												dirFld, true, null));

					JComponent contents =
						GuiUtils
							.vbox(GuiUtils
								.inset(new JLabel("Where should the data files be written to?"),
										5), tmpBtn,
										GuiUtils.hbox(dirBtn, dirComp),
											GuiUtils
												.inset(askCbx,
													new Insets(5, 0, 0, 0)));

					contents = GuiUtils.inset(contents, 5);
					if ( !GuiUtils.showOkCancelDialog(null, "Zip file data",
							contents, null)) {
						return false;
					}

					ask = !askCbx.isSelected();

					toTmp = tmpBtn.isSelected();

					dir = dirFld.getText().toString().trim();

					getStore().put(PREF_ZIDV_ASK, ask);
					getStore().put(PREF_ZIDV_SAVETOTMP, toTmp);
					getStore().put(PREF_ZIDV_DIRECTORY, dir);
					getStore().save();
				}

				String tmpDir = dir;
				if (toTmp) {
					tmpDir = getIdv().getObjectStore().getUserTmpDirectory();
					tmpDir = IOUtil.joinDir(tmpDir, Misc.getUniqueId());
				}
				IOUtil.makeDir(tmpDir);

				getStateManager().putProperty(PROP_ZIDVPATH, tmpDir);
				ZipInputStream zin =
					new ZipInputStream(IOUtil.getInputStream(xmlFile));
				ZipEntry ze = null;

				while ((ze = zin.getNextEntry()) != null) {
					String entryName = ze.getName();

					if (entryName.toLowerCase().endsWith(SUFFIX_XIDV)) {
						bundleContents = new String(IOUtil.readBytes(zin,
								null, false));
					} else {

						if (IOUtil.writeTo(zin, new FileOutputStream(IOUtil.joinDir(tmpDir, entryName))) < 0) {
							return false;
						}
					}
				}
			} else {
				Trace.call1("Decode.readContents");
				bundleContents = IOUtil.readContents(xmlFile);
				Trace.call2("Decode.readContents");
			}

			// TODO: this can probably go one day. I altered the prefix of the
			// comp group classes. Old: "McIDASV...", new: "Mcv..."
			// just gotta be sure to fix the references in the bundles.
			// only people using the nightly build will be affected.
			bundleContents = StringUtil.substitute(bundleContents, 
												   OLD_COMP_STUFF, 
												   NEW_COMP_STUFF);

			Trace.call1("Decode.decodeXml");
			decodeXml(bundleContents, false, xmlFile, name, true,
					  shouldMerge, overrideTimes, removeAll,
					  letUserChangeData, limitNewWindows);
			Trace.call2("Decode.decodeXml");
			return true;
		} catch (Throwable exc) {
			if (contents == null) {
				logException("Unable to load bundle:" + xmlFile, exc);
			} else {
				logException("Unable to evaluate bundle:" + xmlFile, exc);
			}
			return false;
		}
	}

	// replace "old" references in a bundle's XML to the "new" classes.
	private static final String OLD_COMP_STUFF = "McIDASVComp";
	private static final String NEW_COMP_STUFF = "McvComp";

	/**
	 * <p>Overridden so that McIDAS-V can redirect to the version of this 
	 * method that supports limiting the number of new windows.</p>
	 * 
	 * @see #decodeXmlInner(String, boolean, String, String, boolean, boolean, List, boolean, boolean, boolean)
	 */
	@Override protected synchronized void decodeXmlInner(String xml,
														 boolean fromCollab, 
														 String xmlFile, 
														 String label, 
														 boolean showDialog, 
														 boolean shouldMerge, 
														 List overrideTimes,
														 boolean didRemoveAll, 
														 boolean changeData) {

		decodeXmlInner(xml, fromCollab, xmlFile, label, showDialog, 
					  shouldMerge, overrideTimes, didRemoveAll, changeData, 
					  false);

	}

	/**
	 * <p>Overridden so that McIDAS-V can redirect to the version of this 
	 * method that supports limiting the number of new windows.</p>
	 * 
	 * @see #instantiateFromBundle(Hashtable, boolean, LoadBundleDialog, boolean, List, boolean, boolean, boolean)
	 */
	@Override protected void instantiateFromBundle(Hashtable ht, 
												   boolean fromCollab,
												   LoadBundleDialog loadDialog,
												   boolean shouldMerge,
												   List overrideTimes,
												   boolean didRemoveAll,
												   boolean letUserChangeData)
			throws Exception {

		instantiateFromBundle(ht, fromCollab, loadDialog, shouldMerge, 
							  overrideTimes, didRemoveAll, letUserChangeData, 
							  false);
	}

	/**
	 * <p>Hijacks the second part of the IDV bundle loading pipeline so that 
	 * McIDAS-V can limit the number of new windows.</p>
	 * 
	 * @see ucar.unidata.idv.IdvPersistenceManager#decodeXml(String, boolean, String, String, boolean, boolean, List, boolean, boolean)
	 * @see #decodeXmlInner(String, boolean, String, String, boolean, boolean, List, boolean, boolean, boolean)
	 */
	public void decodeXml(final String xml, final boolean fromCollab,
						  final String xmlFile, final String label,
						  final boolean showDialog, final boolean shouldMerge,
						  final List overrideTimes, final boolean removeAll,
						  final boolean letUserChangeData, 
						  final boolean limitWindows) {

		if (!getStateManager().getShouldLoadBundlesSynchronously()) {
			Runnable runnable = new Runnable() {
				public void run() {
					decodeXmlInner(xml, fromCollab, xmlFile, label,
								   showDialog, shouldMerge, overrideTimes,
								   removeAll, letUserChangeData, limitWindows);
				}
			};
			Misc.run(runnable);
		} else {
			decodeXmlInner(xml, fromCollab, xmlFile, label, showDialog,
						   shouldMerge, overrideTimes, removeAll,
						   letUserChangeData, limitWindows);
		}
	}

	/**
	 * <p>Hijacks the third part of the bundle loading pipeline.</p>
	 * 
	 * @see ucar.unidata.idv.IdvPersistenceManager#decodeXmlInner(String, boolean, String, String, boolean, boolean, List, boolean, boolean, boolean)
	 * @see #instantiateFromBundle(Hashtable, boolean, LoadBundleDialog, boolean, List, boolean, boolean, boolean)
	 */
	protected synchronized void decodeXmlInner(String xml, boolean fromCollab, 
											   String xmlFile, String label,
											   boolean showDialog, 
											   boolean shouldMerge, 
											   List overrideTimes, 
											   boolean didRemoveAll, 
											   boolean letUserChangeData, 
											   boolean limitNewWindows) {

		LoadBundleDialog loadDialog = new LoadBundleDialog(this, label);

		boolean inError = false;

		if ( !fromCollab) {
			showWaitCursor();
			if (showDialog)
				loadDialog.showDialog();
		}

		if (xmlFile != null) {
			getStateManager().putProperty(PROP_BUNDLEPATH,
										  IOUtil.getFileRoot(xmlFile));
		}

		getStateManager().putProperty(PROP_LOADINGXML, true);
		try {
			xml = applyPropertiesToBundle(xml);
			if (xml == null)
				return;

			Trace.call1("Decode.toObject");
			Object data = getIdv().getEncoderForRead().toObject(xml);
			Trace.call2("Decode.toObject");

			if (data != null) {
				Hashtable properties = new Hashtable();
				if (data instanceof Hashtable) {
					Hashtable ht = (Hashtable) data;

					instantiateFromBundle(ht, fromCollab, loadDialog,
										  shouldMerge, overrideTimes,
										  didRemoveAll, letUserChangeData, 
										  limitNewWindows);

				} else if (data instanceof DisplayControl) {
					((DisplayControl) data).initAfterUnPersistence(getIdv(),
																   properties);
					loadDialog.addDisplayControl((DisplayControl) data);
				} else if (data instanceof DataSource) {
					getIdv().getDataManager().addDataSource((DataSource)data);
				} else if (data instanceof ColorTable) {
					getColorTableManager().doImport(data, true);
				} else {
					LogUtil.userErrorMessage(log_,
											 "Decoding xml. Unknown object type:"
											 + data.getClass().getName());
				}

				if ( !fromCollab && getIdv().haveCollabManager()) {
					getCollabManager().write(getCollabManager().MSG_BUNDLE,
											 xml);
				}
			}
		} catch (Throwable exc) {
			if (xmlFile != null)
				logException("Error loading bundle: " + xmlFile, exc);
			else
				logException("Error loading bundle", exc);

			inError = true;
		}

		if (!fromCollab)
			showNormalCursor();

		getStateManager().putProperty(PROP_BUNDLEPATH, "");
		getStateManager().putProperty(PROP_ZIDVPATH, "");
		getStateManager().putProperty(PROP_LOADINGXML, false);

		if (!inError && getIdv().getInteractiveMode() && xmlFile != null)
			getIdv().addToHistoryList(xmlFile);

		loadDialog.dispose();
		if (loadDialog.getShouldRemoveItems()) {
			List displayControls = loadDialog.getDisplayControls();
			for (int i = 0; i < displayControls.size(); i++) {
				try {
					((DisplayControl) displayControls.get(i)).doRemove();
				} catch (Exception exc) {
					// Ignore the exception
				}
			}
			List dataSources = loadDialog.getDataSources();
			for (int i = 0; i < dataSources.size(); i++) {
				getIdv().removeDataSource((DataSource) dataSources.get(i));
			}
		}

		loadDialog.clear();
	}

	/**
	 * <p>Builds a list of an incoming bundle's 
	 * {@link ucar.unidata.idv.ViewManager}s that are part of a component 
	 * group.</p>
	 * 
	 * <p>The reason for only being interested in component groups is because
	 * any windows <i>not</i> using component groups will be made into a 
	 * dynamic skin. The associated ViewManagers do not technically exist
	 * until the skin has been &quot;built&quot;, so there's nothing to do.
	 * These ViewManagers must also be removed from the bundle's list of 
	 * ViewManagers.</p>
	 * 
	 * <p>However, any ViewManagers associated with component groups still need
	 * to appear in the bundle's ViewManager list, and that's where this method
	 * comes into play!</p>
	 * 
	 * <p>The use case is admittedly obscure: the new window limit needs to be
	 * enabled, and the bundle must have more than one window.</p>
	 * 
	 * @param windows WindowInfos to be searched.
	 * 
	 * @return List of ViewManagers inside any component groups.
	 */
	protected List<ViewManager> extractCompGroupVMs(
		final List<WindowInfo> windows) {

		List<ViewManager> newList = new ArrayList<ViewManager>();

		for (WindowInfo window : windows) {
			Collection<Object> comps = 
				window.getPersistentComponents().values();

			for (Object comp : comps) {
				if (!(comp instanceof IdvComponentGroup))
					continue;

				IdvComponentGroup group = (IdvComponentGroup)comp;
				List<IdvComponentHolder> holders = 
					group.getDisplayComponents();

				for (IdvComponentHolder holder : holders) {
					if (holder.getViewManagers() != null)
						newList.addAll(holder.getViewManagers());
				}
			}
		}

		return newList;
	}

	/**
	 * <p>Does the work in fixing the collisions described in the
	 * <code>instantiateFromBundle</code> javadoc. Basically just queries the
	 * {@link ucar.unidata.idv.VMManager} for each 
	 * {@link ucar.unidata.idv.ViewManager}. If a match is found, a new ID is
	 * generated and associated with the ViewManager, its 
	 * {@link ucar.unidata.idv.ViewDescriptor}, and any associated 
	 * {@link ucar.unidata.idv.DisplayControl}s.</p>
	 * 
	 * @param vms ViewManagers in the incoming bundle.
	 * @param ctrls DisplayControls in the incoming bundle.
	 * 
	 * @see #instantiateFromBundle(Hashtable, boolean, LoadBundleDialog, boolean, List, boolean, boolean, boolean)
	 */
	protected void reverseCollisions(final List<ViewManager> vms) {
		for (ViewManager vm : vms) {
			ViewDescriptor vd = vm.getViewDescriptor();
			ViewManager current = getVMManager().findViewManager(vd);
			if (current != null) {
				ViewDescriptor oldVd = current.getViewDescriptor();
				String oldId = oldVd.getName();
				String newId = "view_" + Misc.getUniqueId();

				oldVd.setName(newId);
				current.setUniqueId(newId);

				List<DisplayControlImpl> controls = current.getControls();
				for (DisplayControlImpl control : controls) {
					control.resetViewManager(oldId, newId);
				}
			}
		}
	}

	/**
	 * <p>Builds a single window with a single component group. The group 
	 * contains component holders that correspond to each window or component
	 * holder stored in the incoming bundle.</p>
	 * 
	 * @param windows The bundle's list of 
	 *                {@link ucar.unidata.idv.ui.WindowInfo}s.
	 * 
	 * @return List of WindowInfos that contains only one element/window.
	 * 
	 * @throws Exception Bubble up any exceptions from 
	 *                   <code>makeImpromptuSkin</code>.
	 */
	protected List<WindowInfo> injectComponentGroups(
		final List<WindowInfo> windows) throws Exception {

		McvComponentGroup group = 
			new McvComponentGroup(getIdv(), "Group");

		group.setLayout(McvComponentGroup.LAYOUT_TABS);

		Hashtable<String, McvComponentGroup> persist = 
			new Hashtable<String, McvComponentGroup>();

		for (WindowInfo window : windows) {
			List<IdvComponentHolder> holders = buildHolders(window);
			for (IdvComponentHolder holder : holders)
				group.addComponent(holder);
		}

		persist.put("comp1", group);

		// build a new window that contains our component group.
		WindowInfo limitedWindow = new WindowInfo();
		limitedWindow.setPersistentComponents(persist);
		limitedWindow.setSkinPath(Constants.BLANK_COMP_GROUP);
		limitedWindow.setIsAMainWindow(true);
		limitedWindow.setTitle("Super Test");
		limitedWindow.setViewManagers(new ArrayList<ViewManager>());
		limitedWindow.setBounds(windows.get(0).getBounds());

		// make a new list so that we can populate the list of windows with 
		// our single window.
		List<WindowInfo> newWindow = new ArrayList<WindowInfo>();
		newWindow.add(limitedWindow);
		return newWindow;
	}

	/**
	 * <p>Builds an altered copy of <code>windows</code> that preserves the
	 * number of windows while ensuring all displays are inside component 
	 * holders.</p>
	 * 
	 * @throws Exception Bubble up dynamic skin exceptions.
	 * 
	 * @see #injectComponentGroups(List)
	 */
	// TODO: better name!!
	protected List<WindowInfo> betterInject(final List<WindowInfo> windows) 
		throws Exception {

		List<WindowInfo> newList = new ArrayList<WindowInfo>();

		for (WindowInfo window : windows) {
			McvComponentGroup group = 
				new McvComponentGroup(getIdv(), "Group");

			group.setLayout(McvComponentGroup.LAYOUT_TABS);

			Hashtable<String, McvComponentGroup> persist = 
				new Hashtable<String, McvComponentGroup>();

			List<IdvComponentHolder> holders = buildHolders(window);
			for (IdvComponentHolder holder : holders)
				group.addComponent(holder);

			persist.put("comp1", group);
			WindowInfo newWindow = new WindowInfo();
			newWindow.setPersistentComponents(persist);
			newWindow.setSkinPath(Constants.BLANK_COMP_GROUP);
			newWindow.setIsAMainWindow(window.getIsAMainWindow());
			newWindow.setViewManagers(new ArrayList<ViewManager>());
			newWindow.setBounds(window.getBounds());

			newList.add(newWindow);
		}
		return newList;
	}

	/**
	 * <p>Builds a list of component holders with all of <code>window</code>'s
	 * displays.</p>
	 * 
	 * @throws Exception Bubble up any problems creating a dynamic skin.
	 */
	// TODO: refactor
	protected List<IdvComponentHolder> buildHolders(final WindowInfo window) 
		throws Exception {

		List<IdvComponentHolder> holders = 
			new ArrayList<IdvComponentHolder>();

		if (!window.getPersistentComponents().isEmpty()) {
			Collection<Object> comps = 
				window.getPersistentComponents().values();

			for (Object comp : comps) {
				if (!(comp instanceof IdvComponentGroup))
					continue;

				IdvComponentGroup group = (IdvComponentGroup)comp;
				holders.addAll(CompGroups.getComponentHolders(group));
			}
		} else {
			holders.add(makeDynSkin(window));
		}

		return holders;
	}

	/**
	 * <p>Builds a list of any dynamic skins in the bundle and adds them to the
	 * UIMananger's &quot;cache&quot; of encountered ViewManagers.</p>
	 * 
	 * @param windows The bundle's windows.
	 * 
	 * @return Any dynamic skins in <code>windows</code>.
	 */
	public List<ViewManager> mapDynamicSkins(final List<WindowInfo> windows) {
		List<ViewManager> vms = new ArrayList<ViewManager>();
		for (WindowInfo window : windows) {
			Collection<Object> comps = 
				window.getPersistentComponents().values();

			for (Object comp : comps) {
				if (!(comp instanceof IdvComponentGroup))
					continue;

				List<IdvComponentHolder> holders = 
					new ArrayList<IdvComponentHolder>(
							((IdvComponentGroup)comp).getDisplayComponents());

				for (IdvComponentHolder holder : holders) {
					if (!CompGroups.isDynamicSkin(holder))
						continue;

					List<ViewManager> tmpvms = holder.getViewManagers();
					for (ViewManager vm : tmpvms) {
						vms.add(vm);
						UIManager.savedViewManagers.put(
							vm.getViewDescriptor().getName(), vm);
					}
					holder.setViewManagers(new ArrayList<ViewManager>());
				}
			}
		}
		return vms;
	}

	/**
	 * <p>Overridden so that McIDAS-V can preempt the IDV's bundle loading. 
	 * There will be problems if any of the incoming 
	 * {@link ucar.unidata.idv.ViewManager}s share an ID with an existing 
	 * ViewManager. While this case may seem unlikely, it can be triggered 
	 * when loading a bundle and then reloading. The problem is that the 
	 * ViewManagers are the same, and if the previous ViewManagers were not 
	 * removed, the IDV doesn't know what to do.</p>
	 * 
	 * <p>Assigning the incoming ViewManagers a new ID, <i>and associating its
	 * {@link ucar.unidata.idv.ViewDescriptor}s and 
	 * {@link ucar.unidata.idv.DisplayControl}s</i> with the new ID fixes this
	 * problem.</p>
	 * 
	 * <p>McIDAS-V also allows the user to limit the number of new windows the
	 * bundle may create. If enabled, one new window will be created, and any
	 * additional windows will become tabs (component holders) inside the new
	 * window.</p>
	 * 
	 * <p>McIDAS-V also prefers the bundles being loaded to be in a 
	 * semi-regular regular state. For example, say you have bundle containing
	 * only data. The bundle will probably not contain lists of WindowInfos or
	 * ViewManagers. Perhaps the bundle contains nested component groups as 
	 * well! McIDAS-V will alter the unpersisted bundle state (<i>not the 
	 * actual bundle file</i>) to make it fit into the expected idiom. Mostly
	 * this just entails wrapping things in component groups and holders while
	 * &quot;flattening&quot; any nested component groups.</p>
	 * 
	 * @param ht Holds unpersisted objects.
	 * 
	 * @param fromCollab Did the bundle come from the collab stuff?
	 * 
	 * @param loadDialog Show the bundle loading dialog?
	 * 
	 * @param shouldMerge Merge bundle contents into an existing window?
	 * 
	 * @param overrideTimes If non-null, use the set of time indices for data 
	 *                      sources.
	 * 
	 * @param didRemoveAll Remove all data and displays?
	 * 
	 * @param letUserChangeData Allow changes to the data path?
	 * 
	 * @param limitNewWindows Only create one new window?
	 * 
	 * @see ucar.unidata.idv.IdvPersistenceManager#instantiateFromBundle(Hashtable, boolean, LoadBundleDialog, boolean, List, boolean, boolean)
	 */
	protected void instantiateFromBundle(Hashtable ht, 
										 boolean fromCollab,
										 LoadBundleDialog loadDialog,
										 boolean shouldMerge,
										 List overrideTimes,
										 boolean didRemoveAll,
										 boolean letUserChangeData,
										 boolean limitNewWindows) 
			throws Exception {

		// every bundle should have lists corresponding to these ids
		final String[] important = { 
			ID_VIEWMANAGERS, ID_DISPLAYCONTROLS, ID_WINDOWS,
		};
		populateEssentialLists(important, ht);

		List<ViewManager> vms = (List)ht.get(ID_VIEWMANAGERS);
		List<DisplayControlImpl> controls = (List)ht.get(ID_DISPLAYCONTROLS);
		List<WindowInfo> windows = (List)ht.get(ID_WINDOWS);

		// make sure that the list of windows contains no nested comp groups
		flattenWindows(windows);

		// remove any component holders that don't contain displays
		windows = removeUIHolders(windows);

		// generate new IDs for any collisions--typically happens if the same
		// bundle is loaded without removing the previously loaded VMs.
		reverseCollisions(vms);

		// if the incoming bundle has dynamic skins, we've gotta be sure to
		// remove their ViewManagers from the bundle's list of ViewManagers!
		// remember, because they are dynamic skins, the ViewManagers should
		// not exist until the skin is built.
		if (CompGroups.hasDynamicSkins(windows))
			mapDynamicSkins(windows);

		List<WindowInfo> newWindows;
		if (limitNewWindows && windows.size() > 1)
			newWindows = injectComponentGroups(windows);
		else
			newWindows = betterInject(windows);

//			if (limitNewWindows && windows.size() > 1) {
//				// make a single new window with a single component group. 
//				// the group's holders will correspond to each window in the 
//				// bundle.
//				List<WindowInfo> newWindows = injectComponentGroups(windows);
//				ht.put(ID_WINDOWS, newWindows);
//
//				// if there are any component groups in the bundle, we must 
//				// take care that their VMs appear in this list. VMs wrapped 
//				// in dynamic skins don't "exist" at this point, so they do 
//				// not need to be in this list.
//				ht.put(ID_VIEWMANAGERS, extractCompGroupVMs(newWindows));
//			}

		ht.put(ID_WINDOWS, newWindows);

		ht.put(ID_VIEWMANAGERS, extractCompGroupVMs(newWindows));

		// hand our modified bundle information off to the IDV
		super.instantiateFromBundle(ht, fromCollab, loadDialog, shouldMerge, 
									overrideTimes, didRemoveAll, 
									letUserChangeData);

		// no longer needed; the bundle is done loading.
		UIManager.savedViewManagers.clear();
	}

	/**
	 * <p>Alters <code>windows</code> so that no windows in the bundle contain
	 * nested component groups.</p>
	 */
	protected void flattenWindows(final List<WindowInfo> windows) {
		for (WindowInfo window : windows) {
			Hashtable<String, Object> persist = window.getPersistentComponents();
			Set<Map.Entry<String, Object>> blah = persist.entrySet();
			for (Map.Entry<String, Object> entry : blah) {
				if (!(entry.getValue() instanceof IdvComponentGroup))
					continue;

				IdvComponentGroup group = (IdvComponentGroup)entry.getValue();
				if (CompGroups.hasNestedGroups(group))
					entry.setValue(flattenGroup(group));
			}
		}
	}

	/**
	 * @return An altered version of <code>nested</code> that contains no 
	 *         nested component groups.
	 */
	protected IdvComponentGroup flattenGroup(final IdvComponentGroup nested) {
		IdvComponentGroup flat = 
			new IdvComponentGroup(getIdv(), nested.getName());

		flat.setLayout(nested.getLayout());
		flat.setShowHeader(nested.getShowHeader());
		flat.setUniqueId(nested.getUniqueId());

		List<IdvComponentHolder> holders = 
			CompGroups.getComponentHolders(nested);

		for (IdvComponentHolder holder : holders) {
			flat.addComponent(holder);
			holder.setParent(flat);
		}

		return flat;
	}

	/**
	 * @return An altered <code>group</code> containing only component holders
	 *         with displays.
	 */
	protected static List<IdvComponentHolder> removeUIHolders(final IdvComponentGroup group) {
		List<IdvComponentHolder> newHolders = 
			new ArrayList<IdvComponentHolder>(group.getDisplayComponents());

		for (IdvComponentHolder holder : newHolders)
			if (CompGroups.isUIHolder(holder))
				newHolders.remove(holder);

		return newHolders;
	}

	/**
	 * <p>Ensures that the lists corresponding to the ids in <code>ids</code>
	 * actually exist in <code>table</code>, even if they are empty.</p>
	 */
	// TODO: not a fan of this method.
	protected static void populateEssentialLists(final String[] ids, final Hashtable<String, Object> table) {
		for (String id : ids)
			if (table.get(id) == null)
				table.put(id, new ArrayList<Object>());
	}

	/**
	 * <p>Returns an altered copy of <code>windows</code> containing only 
	 * component holders that have displays.</p>
	 * 
	 * <p>The IDV allows users to embed HTML controls or things like the 
	 * dashboard into component holders. This ability, while powerful, could
	 * make for a confusing UI.</p>
	 */
	protected static List<WindowInfo> removeUIHolders(
		final List<WindowInfo> windows) {

		List<WindowInfo> newList = new ArrayList<WindowInfo>();
		for (WindowInfo window : windows) {
			// TODO: ought to write a WindowInfo cloning method
			WindowInfo newWin = new WindowInfo();
			newWin.setViewManagers(window.getViewManagers());
			newWin.setSkinPath(window.getSkinPath());
			newWin.setIsAMainWindow(window.getIsAMainWindow());
			newWin.setBounds(window.getBounds());
			newWin.setTitle(window.getTitle());

			Hashtable<String, IdvComponentGroup> persist = 
				new Hashtable<String, IdvComponentGroup>(
					window.getPersistentComponents()); 

			for (Map.Entry<String, IdvComponentGroup> e : persist.entrySet()) {

				IdvComponentGroup g = e.getValue();

				List<IdvComponentHolder> holders = g.getDisplayComponents();
				if (holders == null || holders.isEmpty())
					continue;

				List<IdvComponentHolder> newHolders = 
					new ArrayList<IdvComponentHolder>();

				// filter out any holders that don't contain view managers
				for (IdvComponentHolder holder : holders)
					if (!CompGroups.isUIHolder(holder))
						newHolders.add(holder);

				g.setDisplayComponents(newHolders);
			}

			newWin.setPersistentComponents(persist);
			newList.add(newWin);
		}
		return newList;
	}

	/**
	 * <p>Uses the {@link ucar.unidata.idv.ViewManager}s in <code>info</code> 
	 * to build a dynamic skin.</p>
	 * 
	 * @param info Window that needs to become a dynamic skin.
	 * 
	 * @return A {@link edu.wisc.ssec.ui.McvComponentHolder} containing the 
	 *         ViewManagers inside <code>info</code>.
	 * 
	 * @throws Exception Bubble up any XML problems.
	 */
	public McvComponentHolder makeDynSkin(final WindowInfo info) throws Exception {
		Document doc = XmlUtil.getDocument(SIMPLE_SKIN_TEMPLATE);
		Element root = doc.getDocumentElement();

		Element panel = XmlUtil.findElement(root, DYNSKIN_TAG_PANEL,
											DYNSKIN_ATTR_ID, DYNSKIN_ID_VALUE);

		List<ViewManager> vms = info.getViewManagers();

		panel.setAttribute(DYNSKIN_ATTR_COLS, Integer.toString(vms.size()));

		for (ViewManager vm : vms) {

			Element view = doc.createElement(DYNSKIN_TAG_VIEW);

			view.setAttribute(DYNSKIN_ATTR_CLASS, vm.getClass().getName());
			view.setAttribute(DYNSKIN_ATTR_VIEWID, vm.getUniqueId());

			StringBuffer props = new StringBuffer(DYNSKIN_PROPS_GENERAL);

			if (vm instanceof MapViewManager)
				if (((MapViewManager)vm).getUseGlobeDisplay())
					props.append(DYNSKIN_PROPS_GLOBE);

			view.setAttribute(DYNSKIN_ATTR_PROPS, props.toString());

			panel.appendChild(view);

			UIManager.savedViewManagers.put(vm.getViewDescriptor().getName(), vm);
		}

		McvComponentHolder holder = 
			new McvComponentHolder(getIdv(), XmlUtil.toString(root));

		holder.setType(McvComponentHolder.TYPE_DYNAMIC_SKIN);
		holder.setName(DYNSKIN_TMPNAME);
		holder.doMakeContents();
		return holder;
	}
	
	private static final String DYNSKIN_TMPNAME = "Dynamic Skin Test";
	private static final String DYNSKIN_TAG_PANEL = "panel";
	private static final String DYNSKIN_TAG_VIEW = "idv.view";
	private static final String DYNSKIN_ATTR_ID = "id";
	private static final String DYNSKIN_ATTR_COLS = "cols";
	private static final String DYNSKIN_ATTR_PROPS = "properties";
	private static final String DYNSKIN_ATTR_CLASS = "class";
	private static final String DYNSKIN_ATTR_VIEWID = "viewid";
	private static final String DYNSKIN_PROPS_GLOBE = "useGlobeDisplay=true;initialMapResources=/auxdata/maps/globemaps.xml;";
	private static final String DYNSKIN_PROPS_GENERAL = "clickToFocus=true;showToolBars=true;shareViews=true;showControlLegend=true;initialSplitPaneLocation=0.2;legendOnLeft=false;size=300:400;shareGroup=view%versionuid%;";
	private static final String DYNSKIN_ID_VALUE = "mcv.content";

	/** XML template for generating dynamic skins. */
	private static final String SIMPLE_SKIN_TEMPLATE = 
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
		"<skin embedded=\"true\">\n" +
		"  <ui>\n" +
		"    <panel layout=\"border\" bgcolor=\"red\">\n" +
		"      <idv.menubar place=\"North\"/>\n" +
		"      <panel layout=\"border\" place=\"Center\">\n" +
		"        <panel layout=\"flow\" place=\"North\">\n" +
		"          <idv.toolbar id=\"idv.toolbar\" place=\"West\"/>\n" +
		"          <panel id=\"idv.favoritesbar\" place=\"North\"/>\n" +
		"        </panel>\n" +
		"        <panel embeddednode=\"true\" id=\"mcv.content\" layout=\"grid\" place=\"Center\">\n" +
		"        </panel>" +
		"      </panel>\n" +
		"      <component idref=\"bottom_bar\"/>\n" +
		"    </panel>\n" +
		"  </ui>\n" +
		"  <styles>\n" +
		"    <style class=\"iconbtn\" space=\"2\" mouse_enter=\"ui.setText(idv.messagelabel,prop:tooltip);ui.setBorder(this,etched);\" mouse_exit=\"ui.setText(idv.messagelabel,);ui.setBorder(this,button);\"/>\n" +
		"    <style class=\"textbtn\" space=\"2\" mouse_enter=\"ui.setText(idv.messagelabel,prop:tooltip)\" mouse_exit=\"ui.setText(idv.messagelabel,)\"/>\n" +
		"  </styles>\n" +
		"  <components>\n" +
		"    <idv.statusbar place=\"South\" id=\"bottom_bar\"/>\n" +
		"  </components>\n" +
		"  <properties>\n" +
		"    <property name=\"icon.wait.wait\" value=\"/ucar/unidata/idv/images/wait.gif\"/>\n" +
		"  </properties>\n" +
		"</skin>\n";
}
