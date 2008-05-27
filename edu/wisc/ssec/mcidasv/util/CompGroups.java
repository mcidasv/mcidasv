package edu.wisc.ssec.mcidasv.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.WindowInfo;
import ucar.unidata.ui.ComponentHolder;
import edu.wisc.ssec.mcidasv.ui.McvComponentHolder;

/**
 * <p>Just some utility methods for dealing with component holders and their 
 * groups.</p>
 * 
 * <p>While the methods may return {@link ucar.unidata.idv.ui.IdvComponentGroup}s
 * or {@link ucar.unidata.idv.ui.IdvComponentHolder}s, they mostly operate under 
 * the &quot;McIDAS-V Model&quot; of component groups: each IdvWindow, excepting
 * the dashboard, contains one component group, which only contain component
 * holders.</p>
 * 
 * <p>The IDV differs in that component groups can contain other component
 * groups (&quot;nested&quot;). This is typically only a concern when loading
 * a bundle created by the IDV.</p>
 */
// TODO: make these support the IDV model
// TODO: look at using generics more intelligently
public class CompGroups {

	/**
	 * @return Whether or not <code>h</code> contains some UI component like 
	 *         the dashboard of field selector. Yes, it can happen!
	 */
	public static boolean isUIHolder(final IdvComponentHolder h) {
		if (h.getType().equals(McvComponentHolder.TYPE_DYNAMIC_SKIN))
			return false;
		return h.getViewManagers().isEmpty();
	}

	/**
	 * @return Whether or not <code>h</code> is a dynamic skin.
	 */
	public static boolean isDynamicSkin(final IdvComponentHolder h) {
		return (h.getType().equals(McvComponentHolder.TYPE_DYNAMIC_SKIN));
	}

	/**
	 * @return Whether or not <code>windows</code> has at least one dynamic skin.
	 */
	public static boolean hasDynamicSkins(final List<WindowInfo> windows) {
		for (WindowInfo window : windows)
			for (IdvComponentHolder holder : getComponentHolders(window))
				if (isDynamicSkin(holder))
					return true;
		return false;
	}

	/**
	 * @return The component holders within <code>windowInfo</code>.
	 * @see #getComponentHolders(IdvComponentGroup)
	 */
	public static List<IdvComponentHolder> getComponentHolders(final WindowInfo windowInfo) {
		List<IdvComponentHolder> holders = 
			new ArrayList<IdvComponentHolder>();

		Collection<Object> comps = 
			windowInfo.getPersistentComponents().values();

		for (Object comp : comps) {
			if (!(comp instanceof IdvComponentGroup))
				continue;

			holders.addAll(getComponentHolders((IdvComponentGroup)comp));
		}
		return holders;
	}

	/**
	 * @return The component holders within <code>idvWindow</code>.
	 * @see #getComponentHolders(IdvComponentGroup)
	 */
	public static List<IdvComponentHolder> getComponentHolders(final IdvWindow idvWindow) {
		List<IdvComponentHolder> holders = new ArrayList<IdvComponentHolder>();
		for (IdvComponentGroup group : (List<IdvComponentGroup>)idvWindow.getComponentGroups())
			holders.addAll(getComponentHolders(group));
		return holders;
	}

	/**
	 * @return <b>Recursively</b> searches <code>group</code> to find any 
	 *         component holders.
	 */
	public static List<IdvComponentHolder> getComponentHolders(final IdvComponentGroup group) {
		List<IdvComponentHolder> holders = new ArrayList<IdvComponentHolder>();
		List<ComponentHolder> comps = group.getDisplayComponents();
		if (comps.isEmpty())
			return holders;

		for (ComponentHolder comp : comps) {
			if (comp instanceof IdvComponentGroup)
				holders.addAll(getComponentHolders((IdvComponentGroup)comp));
			else if (comp instanceof IdvComponentHolder)
				holders.add((IdvComponentHolder)comp);
		}

		return holders;
	}

	/**
	 * @return <b>Recursively</b> searches <code>group</code> for any nested
	 *         component groups.
	 */
	public static List<IdvComponentGroup> getComponentGroups(final IdvComponentGroup group) {
		List<IdvComponentGroup> groups = new ArrayList<IdvComponentGroup>();
		groups.add(group);

		List<ComponentHolder> comps = group.getDisplayComponents();
		if (comps.isEmpty())
			return groups;

		for (ComponentHolder comp : comps)
			if (comp instanceof IdvComponentGroup)
				groups.addAll(getComponentGroups((IdvComponentGroup)comp));
		return groups;
	}

	/**
	 * @return Component groups contained in <code>window</code>.
	 * @see #getComponentGroups(IdvComponentGroup)
	 */
	public static List<IdvComponentGroup> getComponentGroups(final WindowInfo window) {
		Collection<Object> comps = window.getPersistentComponents().values();
		for (Object comp : comps)
			if (comp instanceof IdvComponentGroup)
				return getComponentGroups((IdvComponentGroup)comp);
		return new ArrayList<IdvComponentGroup>();
	}

	/**
	 * @return Component groups contained in <code>windows</code>.
	 * @see #getComponentGroups(IdvComponentGroup)
	 */
	public static List<IdvComponentGroup> getComponentGroups(final List<WindowInfo> windows) {
		List<IdvComponentGroup> groups = new ArrayList<IdvComponentGroup>();
		for (WindowInfo window : windows)
			groups.addAll(getComponentGroups(window));
		return groups;
	}

	/**
	 * @return The component group within <code>window</code>.
	 */
	public static IdvComponentGroup getComponentGroup(final IdvWindow window) {
		List<IdvComponentGroup> groups = window.getComponentGroups();
		if (!groups.isEmpty())
			return groups.get(0);
		return null;
	}

	/**
	 * @return Whether or not <code>group</code> contains any component groups.
	 */
	public static boolean hasNestedGroups(final IdvComponentGroup group) {
		List<ComponentHolder> comps = group.getDisplayComponents();
		for (ComponentHolder comp : comps)
			if (comp instanceof IdvComponentGroup)
				return true;
		return false;
	}

	/**
	 * @return All active component holders in McIDAS-V.
	 */
	// TODO: needs update for nested groups
	public static List<IdvComponentHolder> getAllComponentHolders() {
		List<IdvComponentHolder> holders = new ArrayList<IdvComponentHolder>();
		for (IdvComponentGroup g : getAllComponentGroups())
			holders.addAll(g.getDisplayComponents());
		return holders;
	}

	/**
	 * @return All active component groups in McIDAS-V.
	 */
	// TODO: needs update for nested groups
	public static List<IdvComponentGroup> getAllComponentGroups() {
		List<IdvComponentGroup> groups = new ArrayList<IdvComponentGroup>();
		for (IdvWindow w : getAllDisplayWindows()) 
			groups.addAll(w.getComponentGroups());
		return groups;
	}

	/**
	 * @return All windows that contain at least one component group.
	 */
	public static List<IdvWindow> getAllDisplayWindows() {
		List<IdvWindow> windows = new ArrayList<IdvWindow>();
		for (IdvWindow w : (List<IdvWindow>)IdvWindow.getWindows())
			if (!w.getComponentGroups().isEmpty())
				windows.add(w);
		return windows;
	}
}
