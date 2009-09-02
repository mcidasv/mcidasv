/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.util;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.WindowInfo;
import ucar.unidata.ui.ComponentHolder;

import edu.wisc.ssec.mcidasv.ui.McvComponentGroup;
import edu.wisc.ssec.mcidasv.ui.McvComponentHolder;

/**
 * <p>
 * Just some utility methods for dealing with component holders and their
 * groups.
 * </p>
 * 
 * <p>
 * While the methods may return {@link IdvComponentGroup}s or 
 * {@link IdvComponentHolder}s, they mostly operate under the 
 * {@literal "McIDAS-V Model"} of component groups: each IdvWindow, excepting 
 * the dashboard, contains one component group, which only contain component 
 * holders.
 * </p>
 * 
 * <p>
 * The IDV differs in that component groups can contain other component 
 * groups ({@literal "nested"}). This is typically only a concern when 
 * loading a bundle created by the IDV.
 * </p>
 */
// TODO: make these support the IDV model
// TODO: look at using generics more intelligently
public class CompGroups {

    private CompGroups() {}

    public static List<ViewManager> getViewManagers(final WindowInfo info) {
        List<ViewManager> vms = arrList();
        for (IdvComponentHolder holder : getComponentHolders(info)) {
            vms.addAll(holder.getViewManagers());
        }
        return vms;
    }

    public static List<ViewManager> getViewManagers(final IdvWindow window) {
        List<ViewManager> vms = arrList();
        vms.addAll(window.getViewManagers());

        for (IdvComponentHolder holder : getComponentHolders(window)) {
            vms.addAll(holder.getViewManagers());
        }
        return vms;
    }

    /**
     * @return Whether or not {@code h} contains some UI component like
     * the dashboard of field selector. Yes, it can happen!
     */
    public static boolean isUIHolder(final IdvComponentHolder h) {
        if (h.getType().equals(McvComponentHolder.TYPE_DYNAMIC_SKIN))
            return false;
        return h.getViewManagers().isEmpty();
    }

    /**
     * @return Whether or not {@code h} is a dynamic skin.
     */
    public static boolean isDynamicSkin(final IdvComponentHolder h) {
        return (h.getType().equals(McvComponentHolder.TYPE_DYNAMIC_SKIN));
    }

    /**
     * @return Whether or not {@code windows} has at least one dynamic
     * skin.
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
    public static List<IdvComponentHolder> getComponentHolders(
        final WindowInfo windowInfo) {
        List<IdvComponentHolder> holders = arrList();

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
     * @return The component holders within {@code idvWindow}.
     * @see #getComponentHolders(IdvComponentGroup)
     */
    public static List<IdvComponentHolder> getComponentHolders(
        final IdvWindow idvWindow) 
    {
        List<IdvComponentHolder> holders = arrList();
        for (IdvComponentGroup group : (List<IdvComponentGroup>)idvWindow.getComponentGroups())
            holders.addAll(getComponentHolders(group));
        return holders;
    }

    /**
     * @return <b>Recursively</b> searches {@code group} to find any 
     * component holders.
     */
    public static List<IdvComponentHolder> getComponentHolders(
        final IdvComponentGroup group) 
    {
        List<IdvComponentHolder> holders = arrList();
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
     * @return <b>Recursively</b> searches {@code group} for any nested
     * component groups.
     */
    public static List<IdvComponentGroup> getComponentGroups(
        final IdvComponentGroup group) 
    {
        List<IdvComponentGroup> groups = arrList();
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
     * @return Component groups contained in {@code window}.
     * @see #getComponentGroups(IdvComponentGroup)
     */
    public static List<IdvComponentGroup> getComponentGroups(
        final WindowInfo window) 
    {
        Collection<Object> comps = window.getPersistentComponents().values();
        for (Object comp : comps)
            if (comp instanceof IdvComponentGroup)
                return getComponentGroups((IdvComponentGroup)comp);
        return arrList();
    }

    /**
     * @return Component groups contained in {@code windows}.
     * @see #getComponentGroups(IdvComponentGroup)
     */
    public static List<IdvComponentGroup> getComponentGroups(
        final List<WindowInfo> windows) 
    {
        List<IdvComponentGroup> groups = arrList();
        for (WindowInfo window : windows)
            groups.addAll(getComponentGroups(window));
        return groups;
    }

    /**
     * @return The component group within {@code window}.
     */
    public static IdvComponentGroup getComponentGroup(final IdvWindow window) {
        List<IdvComponentGroup> groups = window.getComponentGroups();
        if (!groups.isEmpty())
            return groups.get(0);
        return null;
    }

    /**
     * @return Whether or not {@code group} contains any component
     *         groups.
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
        List<IdvComponentHolder> holders = arrList();
        for (IdvComponentGroup g : getAllComponentGroups())
            holders.addAll(g.getDisplayComponents());
        return holders;
    }

    /**
     * @return All active component groups in McIDAS-V.
     */
    // TODO: needs update for nested groups
    public static List<IdvComponentGroup> getAllComponentGroups() {
        List<IdvComponentGroup> groups = arrList();
        for (IdvWindow w : getAllDisplayWindows())
            groups.addAll(w.getComponentGroups());
        return groups;
    }

    /**
     * @return All windows that contain at least one component group.
     */
    public static List<IdvWindow> getAllDisplayWindows() {
        List<IdvWindow> windows = arrList();
        for (IdvWindow w : (List<IdvWindow>)IdvWindow.getWindows())
            if (!w.getComponentGroups().isEmpty())
                windows.add(w);
        return windows;
    }

    /**
     * @return The component holder positioned after the active component holder.
     */
    public static IdvComponentHolder getAfterActiveHolder() {
        return getAfterHolder(getActiveComponentHolder());
    }

    /**
     * @return The component holder positioned before the active component holder.
     */
    public static IdvComponentHolder getBeforeActiveHolder() {
        return getBeforeHolder(getActiveComponentHolder());
    }

    /**
     * @return The active component holder in the active window.
     */
    public static IdvComponentHolder getActiveComponentHolder() {
        IdvWindow window = IdvWindow.getActiveWindow();
        McvComponentGroup group = (McvComponentGroup)getComponentGroup(window);
        return (IdvComponentHolder)group.getActiveComponentHolder();
    }

    /**
     * @return The component holder positioned after {@code current}.
     */
    public static IdvComponentHolder getAfterHolder(
        final IdvComponentHolder current) 
    {
        List<IdvComponentHolder> holders = getAllComponentHolders();
        int currentIndex = holders.indexOf(current);
        return holders.get( (currentIndex + 1) % holders.size());
    }

    /**
     * @return The component holder positioned before {@code current}.
     */
    public static IdvComponentHolder getBeforeHolder(
        final IdvComponentHolder current) 
    {
        List<IdvComponentHolder> holders = getAllComponentHolders();
        int currentIndex = holders.indexOf(current);

        int newidx = (currentIndex - 1) % holders.size();
        if (newidx == -1)
            newidx = holders.size() - 1;

        return holders.get(newidx);
    }

    /**
     * @param w {@link IdvWindow} whose component groups you want (as 
     * {@link McvComponentGroup}s).
     * @return A {@link List} of {@code McvComponentGroup}s or an empty list. 
     * If there were no {@code McvComponentGroup}s in {@code w}, 
     * <b>or</b> if {@code w} is {@code null}, an empty {@code List} is returned.
     */
    public static List<McvComponentGroup> idvGroupsToMcv(final IdvWindow w) {
        if (w == null)
            return Collections.emptyList();
        final List<McvComponentGroup> groups = arrList();
        for (IdvComponentGroup group : w.getComponentGroups())
            groups.add((McvComponentGroup)group);
        return groups;
    }
}
